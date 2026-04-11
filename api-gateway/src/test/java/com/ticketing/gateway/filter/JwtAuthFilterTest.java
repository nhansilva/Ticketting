package com.ticketing.gateway.filter;

import com.ticketing.gateway.exception.GatewayAuthException;
import com.ticketing.gateway.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JwtAuthFilter Tests")
class JwtAuthFilterTest {

    private static final String SECRET =
            "your-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm-please-change-in-production";

    private JwtAuthFilter filter;
    private JwtProperties jwtProperties;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        filter = new JwtAuthFilter(jwtProperties);
        chain  = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    // ─────────────────────────────────────────────
    // Public routes — không cần token
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Public routes")
    class PublicRoutes {

        @Test
        @DisplayName("POST /register không cần token")
        void shouldAllowRegister() {
            var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/api/v1/users/register").build());

            StepVerifier.create(filter.apply(new JwtAuthFilter.Config()).filter(exchange, chain))
                    .verifyComplete();
        }

        @Test
        @DisplayName("POST /login không cần token")
        void shouldAllowLogin() {
            var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/api/v1/users/login").build());

            StepVerifier.create(filter.apply(new JwtAuthFilter.Config()).filter(exchange, chain))
                    .verifyComplete();
        }

        @Test
        @DisplayName("GET /events/123 không cần token")
        void shouldAllowPublicGetEvent() {
            var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/v1/events/123").build());

            StepVerifier.create(filter.apply(new JwtAuthFilter.Config()).filter(exchange, chain))
                    .verifyComplete();
        }

        @Test
        @DisplayName("GET /search?q=coldplay không cần token")
        void shouldAllowPublicSearch() {
            var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/v1/search/events?q=coldplay").build());

            StepVerifier.create(filter.apply(new JwtAuthFilter.Config()).filter(exchange, chain))
                    .verifyComplete();
        }
    }

    // ─────────────────────────────────────────────
    // Protected routes — cần token hợp lệ
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Protected routes")
    class ProtectedRoutes {

        @Test
        @DisplayName("token hợp lệ → inject X-User-Id và X-User-Role vào request")
        void shouldInjectHeadersWhenTokenValid() {
            String userId = UUID.randomUUID().toString();
            String token  = buildToken(userId, "CUSTOMER", 3600_000L);

            var request = MockServerHttpRequest.get("/api/v1/bookings/my")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);

            // Capture mutated exchange trong chain
            final String[] capturedUserId = {null};
            final String[] capturedRole   = {null};
            when(chain.filter(any())).thenAnswer(inv -> {
                var mutated = (org.springframework.web.server.ServerWebExchange) inv.getArgument(0);
                capturedUserId[0] = mutated.getRequest().getHeaders().getFirst("X-User-Id");
                capturedRole[0]   = mutated.getRequest().getHeaders().getFirst("X-User-Role");
                return Mono.empty();
            });

            StepVerifier.create(filter.apply(new JwtAuthFilter.Config()).filter(exchange, chain))
                    .verifyComplete();

            assertThat(capturedUserId[0]).isEqualTo(userId);
            assertThat(capturedRole[0]).isEqualTo("CUSTOMER");
        }

        @Test
        @DisplayName("không có Authorization header → lỗi GatewayAuthException")
        void shouldFailWhenMissingToken() {
            var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/v1/bookings/my").build());

            StepVerifier.create(filter.apply(new JwtAuthFilter.Config()).filter(exchange, chain))
                    .expectErrorMatches(ex ->
                            ex instanceof GatewayAuthException &&
                            ((GatewayAuthException) ex).getErrorCode().equals("AUTH_MISSING_TOKEN"))
                    .verify();
        }

        @Test
        @DisplayName("token hết hạn → lỗi AUTH_TOKEN_EXPIRED")
        void shouldFailWhenTokenExpired() {
            String token = buildToken(UUID.randomUUID().toString(), "CUSTOMER", -1000L); // đã hết hạn

            var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/v1/bookings/my")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .build());

            StepVerifier.create(filter.apply(new JwtAuthFilter.Config()).filter(exchange, chain))
                    .expectErrorMatches(ex ->
                            ex instanceof GatewayAuthException &&
                            ((GatewayAuthException) ex).getErrorCode().equals("AUTH_TOKEN_EXPIRED"))
                    .verify();
        }

        @Test
        @DisplayName("token bị sửa (chữ ký sai) → lỗi AUTH_INVALID_TOKEN")
        void shouldFailWhenTokenTampered() {
            var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/api/v1/bookings/my")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                            .build());

            StepVerifier.create(filter.apply(new JwtAuthFilter.Config()).filter(exchange, chain))
                    .expectErrorMatches(ex ->
                            ex instanceof GatewayAuthException &&
                            ((GatewayAuthException) ex).getErrorCode().equals("AUTH_INVALID_TOKEN"))
                    .verify();
        }

        @Test
        @DisplayName("ADMIN token → X-User-Role: ADMIN")
        void shouldInjectAdminRole() {
            String userId = UUID.randomUUID().toString();
            String token  = buildToken(userId, "ADMIN", 3600_000L);

            final String[] capturedRole = {null};
            when(chain.filter(any())).thenAnswer(inv -> {
                var mutated = (org.springframework.web.server.ServerWebExchange) inv.getArgument(0);
                capturedRole[0] = mutated.getRequest().getHeaders().getFirst("X-User-Role");
                return Mono.empty();
            });

            var exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.post("/api/v1/events")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .build());

            StepVerifier.create(filter.apply(new JwtAuthFilter.Config()).filter(exchange, chain))
                    .verifyComplete();

            assertThat(capturedRole[0]).isEqualTo("ADMIN");
        }
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────
    private String buildToken(String userId, String role, long expirationMs) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now      = new Date();
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role",   role)
                .subject(userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }
}
