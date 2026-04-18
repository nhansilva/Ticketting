package com.ticketing.user.controller;

import com.ticketing.user.dto.request.LoginRequest;
import com.ticketing.user.dto.request.RegisterRequest;
import com.ticketing.user.dto.response.LoginResponse;
import com.ticketing.user.dto.response.UserResponse;
import com.ticketing.user.exception.EmailAlreadyExistsException;
import com.ticketing.user.exception.InvalidCredentialsException;
import com.ticketing.common.exception.handler.GlobalExceptionHandler;
import com.ticketing.user.config.SecurityConfig;
import com.ticketing.user.security.JwtAuthenticationFilter;
import com.ticketing.user.service.JwtService;
import com.ticketing.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserService userService;

    // JwtAuthenticationFilter (trong SecurityConfig) cần JwtService — phải mock để WebFluxTest load được context
    @MockitoBean
    private JwtService jwtService;

    private UserResponse userResponse;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userResponse = UserResponse.builder()
                .userId(userId)
                .email("user@example.com")
                .firstName("Nguyen")
                .lastName("Van A")
                .emailVerified(false)
                .role("CUSTOMER")
                .status("PENDING_VERIFICATION")
                .build();
    }

    // ─────────────────────────────────────────────
    // POST /register
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/users/register")
    class Register {

        @Test
        @DisplayName("request hợp lệ → 201 Created")
        void shouldReturn201WhenRegistered() {
            when(userService.register(any(RegisterRequest.class))).thenReturn(Mono.just(userResponse));

            webTestClient.post().uri("/api/v1/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(RegisterRequest.builder()
                            .email("user@example.com")
                            .password("Password@1")
                            .firstName("Nguyen")
                            .lastName("Van A")
                            .build())
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.data.email").isEqualTo("user@example.com")
                    .jsonPath("$.data.status").isEqualTo("PENDING_VERIFICATION");
        }

        @Test
        @DisplayName("email đã tồn tại → 409 Conflict")
        void shouldReturn409WhenEmailExists() {
            when(userService.register(any(RegisterRequest.class)))
                    .thenReturn(Mono.error(new EmailAlreadyExistsException("Email already registered")));

            webTestClient.post().uri("/api/v1/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(RegisterRequest.builder()
                            .email("user@example.com")
                            .password("Password@1")
                            .firstName("Nguyen")
                            .lastName("Van A")
                            .build())
                    .exchange()
                    .expectStatus().is4xxClientError();
        }

        @Test
        @DisplayName("request thiếu email → 400 Bad Request")
        void shouldReturn400WhenEmailMissing() {
            webTestClient.post().uri("/api/v1/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(RegisterRequest.builder()
                            .password("Password@1")
                            .firstName("Nguyen")
                            .lastName("Van A")
                            .build())
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    // ─────────────────────────────────────────────
    // POST /login
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/users/login")
    class Login {

        @Test
        @DisplayName("credentials hợp lệ → 200 OK + token")
        void shouldReturn200WithToken() {
            LoginResponse loginResponse = LoginResponse.builder()
                    .token("access.token.jwt")
                    .refreshToken("refresh.token.jwt")
                    .expiresIn(3600L)
                    .user(userResponse)
                    .build();

            when(userService.login(any(LoginRequest.class))).thenReturn(Mono.just(loginResponse));

            webTestClient.post().uri("/api/v1/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(LoginRequest.builder()
                            .email("user@example.com")
                            .password("Password@1")
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.token").isEqualTo("access.token.jwt")
                    .jsonPath("$.data.expiresIn").isEqualTo(3600);
        }

        @Test
        @DisplayName("sai password → 401 Unauthorized")
        void shouldReturn401WhenPasswordWrong() {
            when(userService.login(any(LoginRequest.class)))
                    .thenReturn(Mono.error(new InvalidCredentialsException("Invalid email or password")));

            webTestClient.post().uri("/api/v1/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(LoginRequest.builder()
                            .email("user@example.com")
                            .password("WrongPass@1")
                            .build())
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    // ─────────────────────────────────────────────
    // GET /profile
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/v1/users/profile")
    class GetProfile {

        @Test
        @WithMockUser
        @DisplayName("authenticated → 200 OK + user data")
        void shouldReturn200WithUserData() {
            when(userService.getCurrentUser(any())).thenReturn(Mono.just(userResponse));

            webTestClient.get().uri("/api/v1/users/profile")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data.email").isEqualTo("user@example.com")
                    .jsonPath("$.data.role").isEqualTo("CUSTOMER");
        }

        @Test
        @DisplayName("chưa đăng nhập → 401 Unauthorized")
        void shouldReturn401WhenUnauthenticated() {
            webTestClient.get().uri("/api/v1/users/profile")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    // ─────────────────────────────────────────────
    // POST /admin/create
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/v1/users/admin/create")
    class CreateAdmin {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN tạo admin mới → 201 Created")
        void shouldReturn201WhenAdminCreated() {
            UserResponse adminResponse = UserResponse.builder()
                    .userId(UUID.randomUUID())
                    .email("newadmin@example.com")
                    .role("ADMIN")
                    .status("ACTIVE")
                    .build();

            when(userService.createAdminUser(any(RegisterRequest.class)))
                    .thenReturn(Mono.just(adminResponse));

            webTestClient.post().uri("/api/v1/users/admin/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(RegisterRequest.builder()
                            .email("newadmin@example.com")
                            .password("Admin@123")
                            .firstName("Admin")
                            .lastName("Two")
                            .build())
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.data.role").isEqualTo("ADMIN")
                    .jsonPath("$.data.status").isEqualTo("ACTIVE");
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        @DisplayName("CUSTOMER gọi admin endpoint → 403 Forbidden")
        void shouldReturn403WhenNotAdmin() {
            webTestClient.post().uri("/api/v1/users/admin/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(RegisterRequest.builder()
                            .email("hack@example.com")
                            .password("Admin@123")
                            .firstName("Hack")
                            .lastName("Er")
                            .build())
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }
}
