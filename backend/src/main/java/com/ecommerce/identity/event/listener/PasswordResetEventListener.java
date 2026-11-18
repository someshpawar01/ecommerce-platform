package com.ecommerce.identity.event.listener;

import com.ecommerce.identity.config.PasswordResetProperties;
import com.ecommerce.identity.event.PasswordResetRequestedEvent;
import com.ecommerce.identity.service.PasswordResetEmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetEventListener {

    private final PasswordResetEmailSender emailSender;

    private final PasswordResetProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PasswordResetRequestedEvent event) {
        String encodedToken = URLEncoder.encode(event.rawToken(), StandardCharsets.UTF_8);
        String separator = properties.baseUrl().contains("?") ? "&" : "?";
        String resetLink = properties.baseUrl() + separator + "token=" + encodedToken;
        try {
            emailSender.sendPasswordResetEmail(event.email(), resetLink);
        } catch (Exception ex) {
            log.error("Failed to dispatch password reset email", ex);
        }
    }
}
