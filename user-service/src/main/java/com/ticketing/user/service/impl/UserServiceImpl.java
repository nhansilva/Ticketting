package com.ticketing.user.service.impl;

import com.ticketing.user.dto.request.*;
import com.ticketing.user.dto.response.LoginResponse;
import com.ticketing.user.dto.response.UserPreferencesResponse;
import com.ticketing.user.dto.response.UserResponse;
import com.ticketing.user.entity.User;
import com.ticketing.user.entity.UserPreferences;
import com.ticketing.user.entity.VerificationToken;
import com.ticketing.user.exception.EmailAlreadyExistsException;
import com.ticketing.user.exception.InvalidCredentialsException;
import com.ticketing.user.exception.InvalidTokenException;
import com.ticketing.user.exception.UserNotFoundException;
import com.ticketing.user.mapper.UserMapper;
import com.ticketing.user.repository.UserPreferencesRepository;
import com.ticketing.user.repository.UserRepository;
import com.ticketing.user.repository.VerificationTokenRepository;
import com.ticketing.user.service.EmailService;
import com.ticketing.user.service.JwtService;
import com.ticketing.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final EmailService emailService;

    // Note: @Transactional doesn't work with reactive R2DBC
    // Transaction management should be handled differently for reactive code

    @Override
    public Mono<UserResponse> register(RegisterRequest request) {
        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new EmailAlreadyExistsException("Email already registered"));
                    }
                    return createUser(request);
                });
    }

    private Mono<UserResponse> createUser(RegisterRequest request) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .emailVerified(false)
                .role(User.UserRole.CUSTOMER)
                .status(User.UserStatus.PENDING_VERIFICATION)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user)
                .flatMap(savedUser -> {
                    // Create default preferences
                    UserPreferences preferences = UserPreferences.builder()
                            .id(UUID.randomUUID())
                            .userId(savedUser.getId())
                            .emailNotifications(true)
                            .smsNotifications(false)
                            .pushNotifications(true)
                            .preferredLanguage("en")
                            .timezone("UTC")
                            .currency("USD")
                            .marketingEmails(false)
                            .build();

                    return preferencesRepository.save(preferences)
                            .then(sendVerificationEmail(savedUser.getEmail()))
                            .thenReturn(userMapper.toResponse(savedUser));
                });
    }

    @Override
    public Mono<LoginResponse> login(LoginRequest request) {
        return userRepository.findActiveUserByEmail(request.getEmail())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid email or password")))
                .flatMap(user -> {
                    // Update last login
                    user.setLastLoginAt(LocalDateTime.now());
                    return userRepository.save(user)
                            .flatMap(updatedUser -> {
                                String token = jwtService.generateToken(updatedUser);
                                String refreshToken = jwtService.generateRefreshToken(updatedUser);
                                UserResponse userResponse = userMapper.toResponse(updatedUser);
                                
                                LoginResponse loginResponse = LoginResponse.builder()
                                        .token(token)
                                        .refreshToken(refreshToken)
                                        .expiresIn(3600L)
                                        .user(userResponse)
                                        .build();
                                
                                return Mono.just(loginResponse);
                            });
                });
    }

    @Override
    public Mono<LoginResponse> refreshToken(String refreshToken) {
        return Mono.fromCallable(() -> {
                    String tokenType = jwtService.extractClaim(refreshToken,
                            claims -> claims.get("type", String.class));
                    if (!"refresh".equals(tokenType)) {
                        throw new InvalidTokenException("Not a refresh token");
                    }
                    return jwtService.getUserIdFromToken(refreshToken);
                })
                .onErrorMap(InvalidTokenException.class, e -> e)
                .onErrorMap(e -> !(e instanceof InvalidTokenException),
                        e -> new InvalidTokenException("Invalid or expired refresh token"))
                .flatMap(userId -> userRepository.findById(userId))
                .filter(user -> user.getStatus() == User.UserStatus.ACTIVE && user.getDeletedAt() == null)
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("User not found or inactive")))
                .map(user -> LoginResponse.builder()
                        .token(jwtService.generateToken(user))
                        .refreshToken(jwtService.generateRefreshToken(user))
                        .expiresIn(3600L)
                        .user(userMapper.toResponse(user))
                        .build());
    }

    @Override
    public Mono<UserResponse> getUserById(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
                .map(userMapper::toResponse);
    }

    @Override
    public Mono<UserResponse> getCurrentUser(UUID userId) {
        return getUserById(userId);
    }

    @Override
    public Mono<UserResponse> updateProfile(UUID userId, UpdateProfileRequest request) {
        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
                .flatMap(user -> {
                    if (request.getFirstName() != null) {
                        user.setFirstName(request.getFirstName());
                    }
                    if (request.getLastName() != null) {
                        user.setLastName(request.getLastName());
                    }
                    if (request.getPhoneNumber() != null) {
                        user.setPhoneNumber(request.getPhoneNumber());
                    }
                    if (request.getDateOfBirth() != null) {
                        user.setDateOfBirth(request.getDateOfBirth());
                    }
                    if (request.getProfileImageUrl() != null) {
                        user.setProfileImageUrl(request.getProfileImageUrl());
                    }
                    user.setUpdatedAt(LocalDateTime.now());
                    
                    return userRepository.save(user)
                            .map(userMapper::toResponse);
                });
    }

    @Override
    public Mono<Void> changePassword(UUID userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Mono.error(new IllegalArgumentException("Passwords do not match"));
        }

        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
                .filter(user -> passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash()))
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid current password")))
                .flatMap(user -> {
                    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user)
                            .then();
                });
    }

    @Override
    public Mono<UserResponse> verifyEmail(String token) {
        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.findValidToken(token, VerificationToken.TokenType.EMAIL_VERIFICATION, now)
                .switchIfEmpty(Mono.error(new InvalidTokenException("Invalid or expired verification token")))
                .flatMap(verificationToken -> {
                    verificationToken.setUsedAt(now);
                    return tokenRepository.save(verificationToken)
                            .flatMap(vt -> userRepository.findById(vt.getUserId()))
                            .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
                            .flatMap(user -> {
                                user.setEmailVerified(true);
                                user.setStatus(User.UserStatus.ACTIVE);
                                user.setUpdatedAt(now);
                                return userRepository.save(user)
                                        .map(userMapper::toResponse);
                            });
                });
    }

    @Override
    public Mono<Void> sendVerificationEmail(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
                .flatMap(user -> {
                    String token = UUID.randomUUID().toString();
                    VerificationToken verificationToken = VerificationToken.builder()
                            .id(UUID.randomUUID())
                            .userId(user.getId())
                            .token(token)
                            .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                            .expiresAt(LocalDateTime.now().plusHours(24))
                            .createdAt(LocalDateTime.now())
                            .build();

                    return tokenRepository.save(verificationToken)
                            .then(Mono.fromRunnable(() -> {
                                // Send email asynchronously
                                emailService.sendVerificationEmail(user.getEmail(), token);
                            }));
                });
    }

    @Override
    public Mono<Void> forgotPassword(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
                .flatMap(user -> {
                    String token = UUID.randomUUID().toString();
                    VerificationToken resetToken = VerificationToken.builder()
                            .id(UUID.randomUUID())
                            .userId(user.getId())
                            .token(token)
                            .tokenType(VerificationToken.TokenType.PASSWORD_RESET)
                            .expiresAt(LocalDateTime.now().plusHours(1))
                            .createdAt(LocalDateTime.now())
                            .build();

                    return tokenRepository.save(resetToken)
                            .then(Mono.fromRunnable(() -> {
                                emailService.sendPasswordResetEmail(user.getEmail(), token);
                            }));
                });
    }

    @Override
    public Mono<Void> resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return Mono.error(new IllegalArgumentException("Passwords do not match"));
        }

        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.findValidToken(request.getToken(), VerificationToken.TokenType.PASSWORD_RESET, now)
                .switchIfEmpty(Mono.error(new InvalidTokenException("Invalid or expired reset token")))
                .flatMap(resetToken -> {
                    resetToken.setUsedAt(now);
                    return tokenRepository.save(resetToken)
                            .flatMap(rt -> userRepository.findById(rt.getUserId()))
                            .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
                            .flatMap(user -> {
                                user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
                                user.setUpdatedAt(now);
                                return userRepository.save(user).then();
                            });
                });
    }

    @Override
    public Mono<UserPreferencesResponse> getPreferences(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User preferences not found")))
                .map(userMapper::toPreferencesResponse);
    }

    @Override
    public Mono<UserPreferencesResponse> updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        return preferencesRepository.findByUserId(userId)
                .switchIfEmpty(createDefaultPreferences(userId))
                .flatMap(preferences -> {
                    if (request.getEmailNotifications() != null) {
                        preferences.setEmailNotifications(request.getEmailNotifications());
                    }
                    if (request.getSmsNotifications() != null) {
                        preferences.setSmsNotifications(request.getSmsNotifications());
                    }
                    if (request.getPushNotifications() != null) {
                        preferences.setPushNotifications(request.getPushNotifications());
                    }
                    if (request.getPreferredLanguage() != null) {
                        preferences.setPreferredLanguage(request.getPreferredLanguage());
                    }
                    if (request.getTimezone() != null) {
                        preferences.setTimezone(request.getTimezone());
                    }
                    if (request.getCurrency() != null) {
                        preferences.setCurrency(request.getCurrency());
                    }
                    if (request.getMarketingEmails() != null) {
                        preferences.setMarketingEmails(request.getMarketingEmails());
                    }

                    return preferencesRepository.save(preferences)
                            .map(userMapper::toPreferencesResponse);
                });
    }

    private Mono<UserPreferences> createDefaultPreferences(UUID userId) {
        UserPreferences preferences = UserPreferences.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .emailNotifications(true)
                .smsNotifications(false)
                .pushNotifications(true)
                .preferredLanguage("en")
                .timezone("UTC")
                .currency("USD")
                .marketingEmails(false)
                .build();
        return preferencesRepository.save(preferences);
    }

    @Override
    public Mono<Boolean> checkEmailAvailability(String email) {
        return userRepository.existsByEmail(email)
                .map(exists -> !exists);
    }

    @Override
    public Mono<Void> deactivateAccount(UUID userId, String password) {
        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid password")))
                .flatMap(user -> {
                    user.setStatus(User.UserStatus.INACTIVE);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user)
                            .then();
                });
    }

    @Override
    public Mono<Void> reactivateAccount(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getStatus() == User.UserStatus.INACTIVE)
                .switchIfEmpty(Mono.error(new UserNotFoundException("Account cannot be reactivated")))
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid credentials")))
                .flatMap(user -> {
                    user.setStatus(User.UserStatus.ACTIVE);
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user)
                            .then();
                });
    }

    @Override
    public Mono<UserResponse> createAdminUser(RegisterRequest request) {
        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new EmailAlreadyExistsException("Email already registered"));
                    }
                    User admin = User.builder()
                            .id(UUID.randomUUID())
                            .email(request.getEmail().toLowerCase())
                            .passwordHash(passwordEncoder.encode(request.getPassword()))
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .phoneNumber(request.getPhoneNumber())
                            .dateOfBirth(request.getDateOfBirth())
                            .emailVerified(true)
                            .role(User.UserRole.ADMIN)
                            .status(User.UserStatus.ACTIVE)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(admin);
                })
                .flatMap(savedAdmin -> {
                    UserPreferences preferences = UserPreferences.builder()
                            .id(UUID.randomUUID())
                            .userId(savedAdmin.getId())
                            .emailNotifications(true)
                            .smsNotifications(false)
                            .pushNotifications(true)
                            .preferredLanguage("en")
                            .timezone("Asia/Ho_Chi_Minh")
                            .currency("VND")
                            .marketingEmails(false)
                            .build();
                    return preferencesRepository.save(preferences)
                            .thenReturn(userMapper.toResponse(savedAdmin));
                });
    }

    @Override
    public Mono<Void> deleteAccount(UUID userId, String password) {
        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found")))
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Invalid password")))
                .flatMap(user -> {
                    user.setStatus(User.UserStatus.DELETED);
                    user.setDeletedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user)
                            .then();
                });
    }
}

