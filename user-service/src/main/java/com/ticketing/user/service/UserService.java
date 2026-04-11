package com.ticketing.user.service;

import com.ticketing.user.dto.request.*;
import com.ticketing.user.dto.response.LoginResponse;
import com.ticketing.user.dto.response.UserPreferencesResponse;
import com.ticketing.user.dto.response.UserResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * User service interface
 */
public interface UserService {

    /**
     * Register a new user
     */
    Mono<UserResponse> register(RegisterRequest request);

    /**
     * Login user
     */
    Mono<LoginResponse> login(LoginRequest request);

    /**
     * Refresh access token
     */
    Mono<LoginResponse> refreshToken(String refreshToken);

    /**
     * Get user profile by id
     */
    Mono<UserResponse> getUserById(UUID userId);

    /**
     * Get current user profile
     */
    Mono<UserResponse> getCurrentUser(UUID userId);

    /**
     * Update user profile
     */
    Mono<UserResponse> updateProfile(UUID userId, UpdateProfileRequest request);

    /**
     * Change password
     */
    Mono<Void> changePassword(UUID userId, ChangePasswordRequest request);

    /**
     * Verify email
     */
    Mono<UserResponse> verifyEmail(String token);

    /**
     * Send verification email
     */
    Mono<Void> sendVerificationEmail(String email);

    /**
     * Forgot password
     */
    Mono<Void> forgotPassword(String email);

    /**
     * Reset password
     */
    Mono<Void> resetPassword(ResetPasswordRequest request);

    /**
     * Get user preferences
     */
    Mono<UserPreferencesResponse> getPreferences(UUID userId);

    /**
     * Update user preferences
     */
    Mono<UserPreferencesResponse> updatePreferences(UUID userId, UpdatePreferencesRequest request);

    /**
     * Check email availability
     */
    Mono<Boolean> checkEmailAvailability(String email);

    /**
     * Deactivate account
     */
    Mono<Void> deactivateAccount(UUID userId, String password);

    /**
     * Reactivate account
     */
    Mono<Void> reactivateAccount(String email, String password);

    /**
     * Delete account
     */
    Mono<Void> deleteAccount(UUID userId, String password);

    /**
     * Create admin user — only callable by existing ADMIN
     */
    Mono<UserResponse> createAdminUser(RegisterRequest request);
}

