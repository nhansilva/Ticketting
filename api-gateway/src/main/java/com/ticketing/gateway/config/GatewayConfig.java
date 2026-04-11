package com.ticketing.gateway.config;

import com.ticketing.gateway.filter.JwtAuthFilter;
import com.ticketing.gateway.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Route definitions — functional DSL.
 * Mỗi route áp dụng:
 *   1. JwtAuthFilter  — validate JWT, inject X-User-Id + X-User-Role
 *   2. RateLimitFilter — sliding window per user/IP
 *   3. removeRequestHeader("Cookie") — security: không chuyển cookies xuống service
 */
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtAuthFilter    jwtAuthFilter;
    private final RateLimitFilter  rateLimitFilter;

    @Value("${services.user-service}")   private String userServiceUrl;
    @Value("${services.event-catalog}")  private String eventCatalogUrl;
    @Value("${services.booking}")        private String bookingUrl;
    @Value("${services.payment}")        private String paymentUrl;
    @Value("${services.search}")         private String searchUrl;
    @Value("${services.cms}")            private String cmsUrl;
    @Value("${services.media}")          private String mediaUrl;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        JwtAuthFilter.Config   jwtConfig = new JwtAuthFilter.Config();

        return builder.routes()

                // ── SWAGGER API DOCS PROXY ────────────────────────────
                // Gateway rewrite /v3/api-docs/{service} → /v3/api-docs trên service đó
                // Dùng cho Swagger UI aggregate tại :8080/swagger-ui.html
                .route("api-docs-user-service", r -> r
                        .path("/v3/api-docs/user-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/user-service", "/v3/api-docs"))
                        .uri(userServiceUrl))

                .route("api-docs-event-catalog", r -> r
                        .path("/v3/api-docs/event-catalog-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/event-catalog-service", "/v3/api-docs"))
                        .uri(eventCatalogUrl))

                .route("api-docs-booking", r -> r
                        .path("/v3/api-docs/booking-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/booking-service", "/v3/api-docs"))
                        .uri(bookingUrl))

                .route("api-docs-payment", r -> r
                        .path("/v3/api-docs/payment-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/payment-service", "/v3/api-docs"))
                        .uri(paymentUrl))

                .route("api-docs-search", r -> r
                        .path("/v3/api-docs/search-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/search-service", "/v3/api-docs"))
                        .uri(searchUrl))

                .route("api-docs-cms", r -> r
                        .path("/v3/api-docs/cms-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/cms-service", "/v3/api-docs"))
                        .uri(cmsUrl))

                .route("api-docs-media", r -> r
                        .path("/v3/api-docs/media-service")
                        .filters(f -> f.rewritePath("/v3/api-docs/media-service", "/v3/api-docs"))
                        .uri(mediaUrl))

                // ── USER SERVICE ───────────────────────────────────────
                // Public: POST /register, /login, /refresh-token, /password/**, /verification/**
                // Protected: GET /profile, PUT /profile, PUT /password/change, ...
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(jwtConfig))
                                .filter(rateLimitFilter.apply(
                                        RateLimitFilter.Config.perUser(100, 60)))
                                .removeRequestHeader("Cookie"))
                        .uri(userServiceUrl))

                // ── EVENT CATALOG SERVICE ──────────────────────────────
                // Public: GET /events/** (xem event không cần login)
                // Protected: POST/PUT/DELETE /events/** (ADMIN — enforced by service)
                .route("event-catalog-service", r -> r
                        .path("/api/v1/events/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(jwtConfig))
                                .filter(rateLimitFilter.apply(
                                        RateLimitFilter.Config.perIp(200, 60)))
                                .removeRequestHeader("Cookie"))
                        .uri(eventCatalogUrl))

                // ── BOOKING SERVICE ────────────────────────────────────
                // Tất cả đều protected — phải login mới đặt vé
                // Rate limit chặt hơn để tránh seat-lock abuse
                .route("booking-service", r -> r
                        .path("/api/v1/bookings/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(jwtConfig))
                                .filter(rateLimitFilter.apply(
                                        RateLimitFilter.Config.perUser(30, 60)))
                                .removeRequestHeader("Cookie"))
                        .uri(bookingUrl))

                // ── PAYMENT SERVICE ────────────────────────────────────
                // Protected — rate limit rất chặt (tránh duplicate payment)
                // Webhook /payments/webhook/** được bỏ qua JWT (payment gateway gọi trực tiếp)
                .route("payment-webhook", r -> r
                        .path("/api/v1/payments/webhook/**")
                        .filters(f -> f
                                .removeRequestHeader("Cookie"))
                        .uri(paymentUrl))

                .route("payment-service", r -> r
                        .path("/api/v1/payments/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(jwtConfig))
                                .filter(rateLimitFilter.apply(
                                        RateLimitFilter.Config.perUser(10, 60)))
                                .removeRequestHeader("Cookie"))
                        .uri(paymentUrl))

                // ── SEARCH SERVICE ─────────────────────────────────────
                // Public — không cần login để search
                .route("search-service", r -> r
                        .path("/api/v1/search/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(jwtConfig))
                                .filter(rateLimitFilter.apply(
                                        RateLimitFilter.Config.perIp(300, 60)))
                                .removeRequestHeader("Cookie"))
                        .uri(searchUrl))

                // ── CMS SERVICE ────────────────────────────────────────
                // GET /cms/** public (homepage config), POST/PUT/DELETE chỉ ADMIN
                .route("cms-service", r -> r
                        .path("/api/v1/cms/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(jwtConfig))
                                .filter(rateLimitFilter.apply(
                                        RateLimitFilter.Config.perUser(50, 60)))
                                .removeRequestHeader("Cookie"))
                        .uri(cmsUrl))

                // ── MEDIA SERVICE ──────────────────────────────────────
                // Upload chỉ ADMIN — rate limit để tránh abuse storage
                .route("media-service", r -> r
                        .path("/api/v1/media/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(jwtConfig))
                                .filter(rateLimitFilter.apply(
                                        RateLimitFilter.Config.perUser(20, 60)))
                                .removeRequestHeader("Cookie"))
                        .uri(mediaUrl))

                .build();
    }
}
