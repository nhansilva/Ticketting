package com.ticketing.user.service;

import com.ticketing.user.dto.request.ChangePasswordRequest;
import com.ticketing.user.dto.request.LoginRequest;
import com.ticketing.user.dto.request.RegisterRequest;
import com.ticketing.user.dto.response.LoginResponse;
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
import com.ticketing.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserPreferencesRepository preferencesRepository;
    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;
    @Mock private EmailService emailService;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User activeUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .firstName("Nguyen")
                .lastName("Van A")
                .emailVerified(true)
                .role(User.UserRole.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userResponse = UserResponse.builder()
                .userId(userId)
                .email("user@example.com")
                .firstName("Nguyen")
                .lastName("Van A")
                .emailVerified(true)
                .role("CUSTOMER")
                .status("ACTIVE")
                .build();
    }

    // ─────────────────────────────────────────────
    // register
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("register()")
    class Register {

        private RegisterRequest request;

        @BeforeEach
        void setUp() {
            request = RegisterRequest.builder()
                    .email("new@example.com")
                    .password("Password@1")
                    .firstName("Tran")
                    .lastName("Thi B")
                    .build();
        }

        @Test
        @DisplayName("register thành công → trả về UserResponse với status PENDING_VERIFICATION")
        void shouldRegisterSuccessfully() {
            User savedUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("new@example.com")
                    .status(User.UserStatus.PENDING_VERIFICATION)
                    .role(User.UserRole.CUSTOMER)
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            UserResponse savedResponse = UserResponse.builder()
                    .email("new@example.com")
                    .status("PENDING_VERIFICATION")
                    .build();

            when(userRepository.existsByEmail("new@example.com")).thenReturn(Mono.just(false));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
            when(preferencesRepository.save(any(UserPreferences.class)))
                    .thenReturn(Mono.just(UserPreferences.builder().build()));
            // sendVerificationEmail() gọi findByEmail để lấy user trước khi tạo token
            when(userRepository.findByEmail("new@example.com")).thenReturn(Mono.just(savedUser));
            when(tokenRepository.save(any(VerificationToken.class)))
                    .thenReturn(Mono.just(VerificationToken.builder().build()));
            // emailService.sendVerificationEmail là void — Mockito mặc định doNothing(), không cần stub
            when(userMapper.toResponse(any(User.class))).thenReturn(savedResponse);

            StepVerifier.create(userService.register(request))
                    .assertNext(response -> {
                        assertThat(response.getStatus()).isEqualTo("PENDING_VERIFICATION");
                        assertThat(response.getEmail()).isEqualTo("new@example.com");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("email đã tồn tại → lỗi EmailAlreadyExistsException")
        void shouldFailWhenEmailAlreadyExists() {
            when(userRepository.existsByEmail("new@example.com")).thenReturn(Mono.just(true));

            StepVerifier.create(userService.register(request))
                    .expectError(EmailAlreadyExistsException.class)
                    .verify();
        }
    }

    // ─────────────────────────────────────────────
    // login
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("login()")
    class Login {

        private LoginRequest request;

        @BeforeEach
        void setUp() {
            request = LoginRequest.builder()
                    .email("user@example.com")
                    .password("Password@1")
                    .build();
        }

        @Test
        @DisplayName("login thành công → trả về token + user")
        void shouldLoginSuccessfully() {
            when(userRepository.findActiveUserByEmail("user@example.com"))
                    .thenReturn(Mono.just(activeUser));
            when(passwordEncoder.matches("Password@1", activeUser.getPasswordHash())).thenReturn(true);
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(activeUser));
            when(jwtService.generateToken(any(User.class))).thenReturn("access.token.jwt");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh.token.jwt");
            when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

            StepVerifier.create(userService.login(request))
                    .assertNext(response -> {
                        assertThat(response.getToken()).isEqualTo("access.token.jwt");
                        assertThat(response.getRefreshToken()).isEqualTo("refresh.token.jwt");
                        assertThat(response.getExpiresIn()).isEqualTo(3600L);
                        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("sai password → lỗi InvalidCredentialsException")
        void shouldFailWhenPasswordWrong() {
            when(userRepository.findActiveUserByEmail("user@example.com"))
                    .thenReturn(Mono.just(activeUser));
            when(passwordEncoder.matches("Password@1", activeUser.getPasswordHash())).thenReturn(false);

            StepVerifier.create(userService.login(request))
                    .expectError(InvalidCredentialsException.class)
                    .verify();
        }

        @Test
        @DisplayName("user không tồn tại → lỗi InvalidCredentialsException")
        void shouldFailWhenUserNotFound() {
            when(userRepository.findActiveUserByEmail("user@example.com"))
                    .thenReturn(Mono.empty());

            StepVerifier.create(userService.login(request))
                    .expectError(InvalidCredentialsException.class)
                    .verify();
        }
    }

    // ─────────────────────────────────────────────
    // refreshToken
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("refreshToken()")
    class RefreshToken {

        @Test
        @DisplayName("refresh token hợp lệ → trả về LoginResponse mới")
        void shouldRefreshTokenSuccessfully() {
            String token = "valid.refresh.token";

            when(jwtService.extractClaim(anyString(), any())).thenReturn("refresh");
            when(jwtService.getUserIdFromToken(token)).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Mono.just(activeUser));
            when(jwtService.generateToken(activeUser)).thenReturn("new.access.token");
            when(jwtService.generateRefreshToken(activeUser)).thenReturn("new.refresh.token");
            when(userMapper.toResponse(activeUser)).thenReturn(userResponse);

            StepVerifier.create(userService.refreshToken(token))
                    .assertNext(response -> {
                        assertThat(response.getToken()).isEqualTo("new.access.token");
                        assertThat(response.getRefreshToken()).isEqualTo("new.refresh.token");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("dùng access token thay vì refresh token → lỗi InvalidTokenException")
        void shouldFailWhenNotRefreshToken() {
            when(jwtService.extractClaim(anyString(), any())).thenReturn("access");

            StepVerifier.create(userService.refreshToken("access.token"))
                    .expectError(InvalidTokenException.class)
                    .verify();
        }

        @Test
        @DisplayName("user bị suspend → lỗi InvalidCredentialsException")
        void shouldFailWhenUserInactive() {
            User suspendedUser = User.builder()
                    .id(userId)
                    .status(User.UserStatus.SUSPENDED)
                    .deletedAt(null)
                    .build();

            when(jwtService.extractClaim(anyString(), any())).thenReturn("refresh");
            when(jwtService.getUserIdFromToken(anyString())).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Mono.just(suspendedUser));

            StepVerifier.create(userService.refreshToken("refresh.token"))
                    .expectError(InvalidCredentialsException.class)
                    .verify();
        }
    }

    // ─────────────────────────────────────────────
    // verifyEmail
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmail {

        @Test
        @DisplayName("token hợp lệ → user trở thành ACTIVE")
        void shouldVerifyEmailSuccessfully() {
            VerificationToken verificationToken = VerificationToken.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .token("valid-token")
                    .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            User pendingUser = User.builder()
                    .id(userId)
                    .status(User.UserStatus.PENDING_VERIFICATION)
                    .emailVerified(false)
                    .updatedAt(LocalDateTime.now())
                    .build();

            UserResponse activeResponse = UserResponse.builder()
                    .status("ACTIVE")
                    .emailVerified(true)
                    .build();

            when(tokenRepository.findValidToken(anyString(), any(), any()))
                    .thenReturn(Mono.just(verificationToken));
            when(tokenRepository.save(any())).thenReturn(Mono.just(verificationToken));
            when(userRepository.findById(userId)).thenReturn(Mono.just(pendingUser));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(pendingUser));
            when(userMapper.toResponse(any(User.class))).thenReturn(activeResponse);

            StepVerifier.create(userService.verifyEmail("valid-token"))
                    .assertNext(response -> {
                        assertThat(response.getStatus()).isEqualTo("ACTIVE");
                        assertThat(response.getEmailVerified()).isTrue();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("token hết hạn hoặc không tồn tại → lỗi InvalidTokenException")
        void shouldFailWhenTokenExpired() {
            when(tokenRepository.findValidToken(anyString(), any(), any()))
                    .thenReturn(Mono.empty());

            StepVerifier.create(userService.verifyEmail("expired-token"))
                    .expectError(InvalidTokenException.class)
                    .verify();
        }
    }

    // ─────────────────────────────────────────────
    // changePassword
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("đổi password thành công")
        void shouldChangePasswordSuccessfully() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("OldPass@1")
                    .newPassword("NewPass@1")
                    .confirmPassword("NewPass@1")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Mono.just(activeUser));
            when(passwordEncoder.matches("OldPass@1", activeUser.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("NewPass@1")).thenReturn("$2a$10$newHash");
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(activeUser));

            StepVerifier.create(userService.changePassword(userId, request))
                    .verifyComplete();
        }

        @Test
        @DisplayName("newPassword != confirmPassword → lỗi IllegalArgumentException")
        void shouldFailWhenPasswordMismatch() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("OldPass@1")
                    .newPassword("NewPass@1")
                    .confirmPassword("Different@1")
                    .build();

            StepVerifier.create(userService.changePassword(userId, request))
                    .expectError(IllegalArgumentException.class)
                    .verify();
        }

        @Test
        @DisplayName("current password sai → lỗi InvalidCredentialsException")
        void shouldFailWhenCurrentPasswordWrong() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("WrongPass@1")
                    .newPassword("NewPass@1")
                    .confirmPassword("NewPass@1")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Mono.just(activeUser));
            when(passwordEncoder.matches("WrongPass@1", activeUser.getPasswordHash())).thenReturn(false);

            StepVerifier.create(userService.changePassword(userId, request))
                    .expectError(InvalidCredentialsException.class)
                    .verify();
        }
    }

    // ─────────────────────────────────────────────
    // getUserById
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("user tồn tại → trả về UserResponse")
        void shouldReturnUserWhenExists() {
            when(userRepository.findById(userId)).thenReturn(Mono.just(activeUser));
            when(userMapper.toResponse(activeUser)).thenReturn(userResponse);

            StepVerifier.create(userService.getUserById(userId))
                    .assertNext(response -> assertThat(response.getEmail()).isEqualTo("user@example.com"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("user đã bị xóa mềm → lỗi UserNotFoundException")
        void shouldFailWhenUserSoftDeleted() {
            User deletedUser = User.builder()
                    .id(userId)
                    .deletedAt(LocalDateTime.now().minusDays(1))
                    .build();

            when(userRepository.findById(userId)).thenReturn(Mono.just(deletedUser));

            StepVerifier.create(userService.getUserById(userId))
                    .expectError(UserNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("user không tồn tại → lỗi UserNotFoundException")
        void shouldFailWhenUserNotFound() {
            when(userRepository.findById(userId)).thenReturn(Mono.empty());

            StepVerifier.create(userService.getUserById(userId))
                    .expectError(UserNotFoundException.class)
                    .verify();
        }
    }
}
