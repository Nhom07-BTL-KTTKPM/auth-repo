package iuh.fit.authservice.client;

import feign.FeignException;
import feign.RetryableException;
import iuh.fit.shared.error.BusinessException;
import iuh.fit.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;

@Component
public class UserServiceErrorMapper {

    public BusinessException mapClientException(String operation, String email, FeignException.FeignClientException exception) {
        int status = exception.status();

        if ("get-user-by-email".equals(operation)) {
            return new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }

        ErrorCode code = switch (status) {
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            case 409 -> ErrorCode.CONFLICT;
            default -> ErrorCode.BAD_REQUEST;
        };

        String message = switch (status) {
            case 409 -> "User already exists";
            case 404 -> "User resource not found";
            default -> "Invalid request to user-service";
        };

        return new BusinessException(
                code,
                message,
                Map.of("operation", operation, "httpStatus", status, "email", email)
        );
    }

    public BusinessException mapRetryExhausted(String operation, String email, Throwable throwable) {
        Throwable root = rootCause(throwable);

        if (root instanceof BusinessException businessException) {
            return businessException;
        }

        if (root instanceof FeignException.FeignClientException clientException) {
            return mapClientException(operation, email, clientException);
        }

        String reason = "unknown";
        if (root instanceof RetryableException || root instanceof SocketTimeoutException) {
            reason = "timeout";
        } else if (root instanceof ConnectException) {
            reason = "connection";
        } else if (root instanceof FeignException feignException && feignException.status() >= 500) {
            reason = "downstream-5xx";
        }

        return new BusinessException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "User service is temporarily unavailable. Please try again later.",
                Map.of(
                        "operation", operation,
                        "email", email,
                        "reason", reason
                )
        );
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
    }
}
