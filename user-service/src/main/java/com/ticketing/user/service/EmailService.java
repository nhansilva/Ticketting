package com.ticketing.user.service;

/**
 * Email service interface
 */
public interface EmailService {

    /**
     * Send verification email
     */
    void sendVerificationEmail(String email, String token);

    /**
     * Send password reset email
     */
    void sendPasswordResetEmail(String email, String token);
}

