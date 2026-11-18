package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.config.PasswordResetProperties;
import com.ecommerce.identity.service.PasswordResetEmailSender;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpPasswordResetEmailSender implements PasswordResetEmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendPasswordResetEmail(String email, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Reset your password - E-Commerce Platform");

        message.setText("""
                We received a request to reset your password.

                Use the following link to reset it:

                %s

                This link expires in 15 minutes.

                If you did not request a password reset, you can ignore this email.
                """.formatted(resetLink));

        mailSender.send(message);
    }
}
