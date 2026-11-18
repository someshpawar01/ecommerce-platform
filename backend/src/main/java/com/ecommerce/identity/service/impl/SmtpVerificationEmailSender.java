package com.ecommerce.identity.service.impl;

import com.ecommerce.identity.service.VerificationEmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmtpVerificationEmailSender implements VerificationEmailSender {

  private final JavaMailSender mailSender;

  @Value("${app.mail.from}")
  private String fromAddress;

  @Override
  public void sendVerificationEmail(String email, String verificationLink) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(email);
    message.setSubject("Verify your email address");
    message.setText(
        """
                Welcome to our e-commerce platform!

                Please verify your email address using the link below:

                %s

                If you did not create this account, you can ignore this email.

                This is an automated message.
                """
            .formatted(verificationLink));

    mailSender.send(message);
  }
}
