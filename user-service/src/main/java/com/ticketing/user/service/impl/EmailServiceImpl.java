package com.ticketing.user.service.impl;

import com.ticketing.user.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Email service implementation
 * TODO: Integrate with actual email service (e.g., SendGrid, AWS SES, etc.)
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendVerificationEmail(String email, String token) {
        // TODO: Implement actual email sending
        String verificationLink = "http://localhost:8080/api/v1/users/verification/verify?token=" + token;
        log.info("Sending verification email to: {} with link: {}", email, verificationLink);
        // In production, integrate with email service
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        // TODO: Implement actual email sending
        String resetLink = "http://localhost:8080/reset-password?token=" + token;
        log.info("Sending password reset email to: {} with link: {}", email, resetLink);
        // In production, integrate with email service
    }
}

