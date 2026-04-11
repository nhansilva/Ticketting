package com.ticketing.gateway.filter;

import com.ticketing.gateway.exception.GatewayAuthException;
import com.ticketing.gateway.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * GatewayFilter — validate JWT, inject X-User-Id và X-User-Role vào request header.
 * Public routes được bỏ qua, không yêu cầu token.
 */
@Slf4j
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final JwtProperties jwtProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // (method=null) nghĩa là bất kỳ HTTP method nào
    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute(HttpMethod.POST, "/api/v1/users/register"),
            new PublicRoute(HttpMethod.POST, "/api/v1/users/login"),
            new PublicRoute(HttpMethod.POST, "/api/v1/users/refresh-token"),
            new PublicRoute(HttpMethod.POST, "/api/v1/users/password/forgot"),
            new PublicRoute(HttpMethod.POST, "/api/v1/users/password/reset"),
            new PublicRoute(HttpMethod.POST, "/api/v1/users/verification/send"),
            new PublicRoute(HttpMethod.GET,  "/api/v1/users/verification/**"),
            new PublicRoute(HttpMethod.GET,  "/api/v1/users/check-email"),
            new PublicRoute(HttpMethod.POST, "/api/v1/users/account/reactivate"),
            new PublicRoute(HttpMethod.GET,  "/api/v1/events/**"),  // Xem event không cần login
            new PublicRoute(HttpMethod.GET,  "/api/v1/search/**"), // Search public
            new PublicRoute(null,            "/actuator/health"),
            new PublicRoute(null,            "/actuator/info"),
            // Swagger UI — gateway hosts aggregated docs
            new PublicRoute(HttpMethod.GET,  "/swagger-ui/**"),
            new PublicRoute(HttpMethod.GET,  "/swagger-ui.html"),
            new PublicRoute(HttpMethod.GET,  "/v3/api-docs/**"),
            new PublicRoute(HttpMethod.GET,  "/v3/api-docs"),
            new PublicRoute(HttpMethod.GET,  "/webjars/**")
    );

    public JwtAuthFilter(JwtProperties jwtProperties) {
        super(Config.class);
        this.jwtProperties = jwtProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            HttpMethod method = request.getMethod();

            if (isPublicRoute(path, method)) {
                return chain.filter(exchange);
            }

            String token = extractToken(request);
            if (token == null) {
                return Mono.error(new GatewayAuthException(
                        "Authorization header missing or invalid", "AUTH_MISSING_TOKEN"));
            }

            try {
                Claims claims = parseClaims(token);
                String userId = claims.get("userId", String.class);
                String role   = claims.get("role",   String.class);

                if (userId == null || role == null) {
                    return Mono.error(new GatewayAuthException(
                            "Invalid token claims", "AUTH_INVALID_CLAIMS"));
                }

                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id",   userId)
                        .header("X-User-Role", role)
                        .build();

                log.debug("JWT validated: userId={}, role={}, path={}", userId, role, path);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (ExpiredJwtException e) {
                log.warn("Expired JWT: path={}", path);
                return Mono.error(new GatewayAuthException(
                        "Token has expired", "AUTH_TOKEN_EXPIRED"));
            } catch (MalformedJwtException | SignatureException e) {
                log.warn("Invalid JWT: path={}", path);
                return Mono.error(new GatewayAuthException(
                        "Invalid token", "AUTH_INVALID_TOKEN"));
            } catch (Exception e) {
                log.error("JWT processing error: path={}, error={}", path, e.getMessage());
                return Mono.error(new GatewayAuthException(
                        "Authentication failed", "AUTH_ERROR"));
            }
        };
    }

    private boolean isPublicRoute(String path, HttpMethod method) {
        return PUBLIC_ROUTES.stream().anyMatch(route ->
                pathMatcher.match(route.pattern(), path) &&
                (route.method() == null || route.method().equals(method))
        );
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // JWT parsing là synchronous — không có I/O, không cần wrap Mono
    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private record PublicRoute(HttpMethod method, String pattern) {}

    public static class Config {
        // Có thể mở rộng: requiredRole, skipRoutes...
    }
}
