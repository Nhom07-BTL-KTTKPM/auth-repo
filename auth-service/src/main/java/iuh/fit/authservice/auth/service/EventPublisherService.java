package iuh.fit.authservice.auth.service;

import iuh.fit.authservice.event.EmailVerifyEvent;
import iuh.fit.authservice.event.OtpEmailEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);

    private final RabbitTemplate rabbitTemplate;
    private final String criticalExchange;
    private final String emailVerifyRoutingKey;
    private final String emailOtpRoutingKey;

    public EventPublisherService(
            RabbitTemplate rabbitTemplate,
            @Value("${auth.rabbitmq.critical-exchange}") String criticalExchange,
            @Value("${auth.rabbitmq.routing.email-verify}") String emailVerifyRoutingKey,
            @Value("${auth.rabbitmq.routing.email-otp}") String emailOtpRoutingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.criticalExchange = criticalExchange;
        this.emailVerifyRoutingKey = emailVerifyRoutingKey;
        this.emailOtpRoutingKey = emailOtpRoutingKey;
    }

    public void publishVerifyEmail(String email, String fullName, String verificationToken) {
        try {
            var event = new EmailVerifyEvent(email, fullName, verificationToken);
            rabbitTemplate.convertAndSend(criticalExchange, emailVerifyRoutingKey, event);
            log.info("[EventPublisher] Published VERIFY_EMAIL event for email={}", email);
        } catch (Exception e) {
            log.error("[EventPublisher] Failed to publish VERIFY_EMAIL for email={}: {}", email, e.getMessage(), e);
        }
    }

    public void publishOtpEmail(String email, String fullName, String otpCode, String purpose) {
        try {
            var event = new OtpEmailEvent(email, fullName, otpCode, purpose);
            rabbitTemplate.convertAndSend(criticalExchange, emailOtpRoutingKey, event);
            log.info("[EventPublisher] Published OTP_EMAIL event purpose={} for email={}", purpose, email);
        } catch (Exception e) {
            log.error("[EventPublisher] Failed to publish OTP_EMAIL for email={}: {}", email, e.getMessage(), e);
        }
    }
}
