package com.ticketing.user.controller;

import com.ticketing.common.dto.response.ApiResponse;
import com.ticketing.user.dto.request.*;
import com.ticketing.user.dto.response.ChangePasswordResponse;
import com.ticketing.user.dto.response.LoginResponse;
import com.ticketing.user.dto.response.UserPreferencesResponse;
import com.ticketing.user.dto.response.UserResponse;
import com.ticketing.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Users", description = "User registration, authentication, and profile management")
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Register new user", description = "Create account and send verification email")
    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> register(
            @Valid @RequestBody RegisterRequest request) {
        return userService.register(request)
                .map(user -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(ApiResponse.success("User registered successfully. Please verify your email.", user)))
                .doOnError(error -> log.error("Registration error: {}", error.getMessage()));
    }

    @Operation(summary = "Login", description = "Authenticate and receive access + refresh tokens")
    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> login(
            @Valid @RequestBody LoginRequest request) {
        return userService.login(request)
                .map(loginResponse -> ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse)))
                .doOnError(error -> log.error("Login error: {}", error.getMessage()));
    }

    @Operation(summary = "Refresh token", description = "Exchange refresh token for new access + refresh token pair")
    @PostMapping("/refresh-token")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> refreshToken(
            @RequestBody RefreshTokenRequest request) {
        return userService.refreshToken(request.getRefreshToken())
                .map(loginResponse -> ResponseEntity.ok(ApiResponse.success(loginResponse)))
                .doOnError(error -> log.error("Refresh token error: {}", error.getMessage()));
    }

    @Operation(summary = "Get my profile", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/profile")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> getCurrentUser(
            @AuthenticationPrincipal UUID userId) {
        return userService.getCurrentUser(userId)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .doOnError(error -> log.error("Get profile error: {}", error.getMessage()));
    }

    @Operation(summary = "Get user by ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{userId}")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> getUserById(
            @PathVariable UUID userId) {
        return userService.getUserById(userId)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .doOnError(error -> log.error("Get user error: {}", error.getMessage()));
    }

    @Operation(summary = "Update profile", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/profile")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId, request)
                .map(user -> ResponseEntity.ok(ApiResponse.success("Profile updated successfully", user)))
                .doOnError(error -> log.error("Update profile error: {}", error.getMessage()));
    }

    @Operation(summary = "Change password", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/password/change")
    public Mono<ResponseEntity<ApiResponse<ChangePasswordResponse>>> changePassword(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(userId, request)
                .then(Mono.fromSupplier(() -> {
                    ChangePasswordResponse response = ChangePasswordResponse.builder()
                            .changed(true)
                            .changedAt(LocalDateTime.now())
                            .build();
                    return ResponseEntity.ok(ApiResponse.success("Password changed successfully", response));
                }))
                .doOnError(error -> log.error("Change password error: {}", error.getMessage()));
    }

    @Operation(summary = "Verify email", description = "Token sent via email link")
    @GetMapping("/verification/verify")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> verifyEmail(
            @RequestParam String token) {
        return userService.verifyEmail(token)
                .map(user -> ResponseEntity.ok(ApiResponse.success("Email verified successfully", user)))
                .doOnError(error -> log.error("Verify email error: {}", error.getMessage()));
    }

    @Operation(summary = "Send verification email")
    @PostMapping("/verification/send")
    public Mono<ResponseEntity<ApiResponse<Void>>> sendVerificationEmail(
            @RequestBody SendVerificationEmailRequest request) {
        return userService.sendVerificationEmail(request.getEmail())
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("Verification email sent", null))))
                .doOnError(error -> log.error("Send verification email error: {}", error.getMessage()));
    }

    @Operation(summary = "Resend verification email", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/verification/resend")
    public Mono<ResponseEntity<ApiResponse<Void>>> resendVerificationEmail(
            @AuthenticationPrincipal UUID userId) {
        return userService.getUserById(userId)
                .flatMap(user -> userService.sendVerificationEmail(user.getEmail()))
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("Verification email resent", null))))
                .doOnError(error -> log.error("Resend verification email error: {}", error.getMessage()));
    }

    @Operation(summary = "Forgot password", description = "Send password reset link to email")
    @PostMapping("/password/forgot")
    public Mono<ResponseEntity<ApiResponse<Void>>> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {
        return userService.forgotPassword(request.getEmail())
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("Password reset link sent to your email", null))))
                .doOnError(error -> log.error("Forgot password error: {}", error.getMessage()));
    }

    @Operation(summary = "Reset password", description = "Use token from email to set new password")
    @PostMapping("/password/reset")
    public Mono<ResponseEntity<ApiResponse<Void>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(request)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("Password reset successfully", null))))
                .doOnError(error -> log.error("Reset password error: {}", error.getMessage()));
    }

    @Operation(summary = "Get preferences", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/preferences")
    public Mono<ResponseEntity<ApiResponse<UserPreferencesResponse>>> getPreferences(
            @AuthenticationPrincipal UUID userId) {
        return userService.getPreferences(userId)
                .map(preferences -> ResponseEntity.ok(ApiResponse.success(preferences)))
                .doOnError(error -> log.error("Get preferences error: {}", error.getMessage()));
    }

    @Operation(summary = "Update preferences", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/preferences")
    public Mono<ResponseEntity<ApiResponse<UserPreferencesResponse>>> updatePreferences(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return userService.updatePreferences(userId, request)
                .map(preferences -> ResponseEntity.ok(ApiResponse.success("Preferences updated successfully", preferences)))
                .doOnError(error -> log.error("Update preferences error: {}", error.getMessage()));
    }

    @Operation(summary = "Check email availability")
    @GetMapping("/check-email")
    public Mono<ResponseEntity<ApiResponse<EmailAvailabilityResponse>>> checkEmailAvailability(
            @RequestParam String email) {
        return userService.checkEmailAvailability(email)
                .map(available -> {
                    EmailAvailabilityResponse response = EmailAvailabilityResponse.builder()
                            .email(email)
                            .available(available)
                            .build();
                    return ResponseEntity.ok(ApiResponse.success(response));
                })
                .doOnError(error -> log.error("Check email availability error: {}", error.getMessage()));
    }

    @Operation(summary = "Deactivate account", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/account/deactivate")
    public Mono<ResponseEntity<ApiResponse<Void>>> deactivateAccount(
            @AuthenticationPrincipal UUID userId,
            @RequestBody DeactivateAccountRequest request) {
        return userService.deactivateAccount(userId, request.getPassword())
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("Account deactivated successfully", null))))
                .doOnError(error -> log.error("Deactivate account error: {}", error.getMessage()));
    }

    @Operation(summary = "Reactivate account")
    @PostMapping("/account/reactivate")
    public Mono<ResponseEntity<ApiResponse<Void>>> reactivateAccount(
            @RequestBody ReactivateAccountRequest request) {
        return userService.reactivateAccount(request.getEmail(), request.getPassword())
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("Account reactivated successfully", null))))
                .doOnError(error -> log.error("Reactivate account error: {}", error.getMessage()));
    }

    @Operation(summary = "Delete account (soft delete)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/account")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteAccount(
            @AuthenticationPrincipal UUID userId,
            @RequestBody DeleteAccountRequest request) {
        return userService.deleteAccount(userId, request.getPassword())
                .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success("Account deleted successfully", null))))
                .doOnError(error -> log.error("Delete account error: {}", error.getMessage()));
    }

    @Operation(summary = "Create admin user", description = "ADMIN only",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/admin/create")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> createAdminUser(
            @Valid @RequestBody RegisterRequest request) {
        return userService.createAdminUser(request)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success("Admin user created successfully", user)))
                .doOnError(error -> log.error("Create admin error: {}", error.getMessage()));
    }

    @Operation(summary = "Health check", description = "Service liveness probe")
    @GetMapping("/health")
    public Mono<ResponseEntity<ApiResponse<HealthResponse>>> health() {
        HealthResponse health = HealthResponse.builder()
                .status("UP")
                .build();
        return Mono.just(ResponseEntity.ok(ApiResponse.success(health)));
    }

    // Inner classes for request/response DTOs
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RefreshTokenRequest {
        private String refreshToken;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SendVerificationEmailRequest {
        private String email;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ForgotPasswordRequest {
        private String email;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EmailAvailabilityResponse {
        private String email;
        private Boolean available;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DeactivateAccountRequest {
        private String password;
        private String reason;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReactivateAccountRequest {
        private String email;
        private String password;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DeleteAccountRequest {
        private String password;
        private String confirmation;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HealthResponse {
        private String status;
    }
}

