package iuh.fit.authservice.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import iuh.fit.shared.api.ApiError;
import iuh.fit.shared.api.ApiResponse;
import iuh.fit.shared.trace.TraceIdConstants;
import iuh.fit.shared.trace.TraceIdContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 60)
public class LoginRegisterRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String REGISTER_PATH = "/api/v1/auth/register";

    private final ProxyManager<String> proxyManager;
    private final BucketConfiguration bucketConfiguration;
    private final AuthLoginRegisterRateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public LoginRegisterRateLimitFilter(
            ProxyManager<String> proxyManager,
            BucketConfiguration bucketConfiguration,
            AuthLoginRegisterRateLimitProperties properties,
            ObjectMapper objectMapper
    ) {
        this.proxyManager = proxyManager;
        this.bucketConfiguration = bucketConfiguration;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!properties.isEnabled() || HttpMethod.OPTIONS.matches(request.getMethod()) || !isProtectedEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String key = properties.getKeyPrefix() + clientIp;

        Bucket bucket = proxyManager.getProxy(key, () -> bucketConfiguration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = nanosToSeconds(probe.getNanosToWaitForRefill());
        String traceId = resolveTraceId(request, response);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        ApiError error = new ApiError(
                "RATE_LIMIT_EXCEEDED",
                "Maximum %d requests every %d second(s) exceeded for this IP".formatted(
                        properties.getCapacity(),
                        properties.getRefillDuration().toSeconds()
                ),
                Map.of(
                        "retryAfterSeconds", retryAfterSeconds,
                        "capacity", properties.getCapacity(),
                        "refillTokens", properties.getRefillTokens(),
                        "refillDurationSeconds", properties.getRefillDuration().toSeconds()
                ),
                null
        );

        ApiResponse<Void> payload = ApiResponse.failure(
                "Too many requests. Please retry later.",
                error,
                traceId
        );

        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }

    private static long nanosToSeconds(long nanosToWait) {
        if (nanosToWait <= 0) {
            return 1;
        }
        long seconds = nanosToWait / 1_000_000_000L;
        return (nanosToWait % 1_000_000_000L == 0) ? Math.max(1, seconds) : seconds + 1;
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String firstIp = forwardedFor.split(",")[0].trim();
            if (!firstIp.isBlank()) {
                return firstIp;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "unknown" : remoteAddr;
    }

    private static String resolveTraceId(HttpServletRequest request, HttpServletResponse response) {
        Object attr = request.getAttribute(TraceIdConstants.REQUEST_ATTRIBUTE);
        if (attr instanceof String traceId && !traceId.isBlank()) {
            response.setHeader(TraceIdConstants.HEADER_NAME, traceId);
            return traceId;
        }

        String headerTraceId = request.getHeader(TraceIdConstants.HEADER_NAME);
        if (headerTraceId != null && !headerTraceId.isBlank()) {
            response.setHeader(TraceIdConstants.HEADER_NAME, headerTraceId);
            return headerTraceId;
        }

        String contextTraceId = TraceIdContext.get();
        if (contextTraceId != null && !contextTraceId.isBlank()) {
            response.setHeader(TraceIdConstants.HEADER_NAME, contextTraceId);
            return contextTraceId;
        }

        String generatedTraceId = UUID.randomUUID().toString();
        response.setHeader(TraceIdConstants.HEADER_NAME, generatedTraceId);
        return generatedTraceId;
    }

    private static boolean isProtectedEndpoint(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }

        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }

        return LOGIN_PATH.equals(requestPath) || REGISTER_PATH.equals(requestPath);
    }
}
