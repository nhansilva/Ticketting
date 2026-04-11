package com.ticketing.gateway.filter;

import com.ticketing.gateway.exception.GatewayRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * Rate limiting filter — Sliding Window Log algorithm với Redis Sorted Set.
 *
 * Hai mode:
 * - PER_USER: key dựa trên X-User-Id (đã inject bởi JwtAuthFilter) — dùng cho authenticated routes
 * - PER_IP:   key dựa trên client IP — dùng cho public routes
 *
 * Fail open: nếu Redis không available, request vẫn được forward (không block hệ thống).
 */
@Slf4j
@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate:gw:";
    private final ReactiveRedisOperations<String, String> redisTemplate;

    public RateLimitFilter(ReactiveRedisOperations<String, String> redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String key = buildKey(exchange.getRequest(), config);

            return checkSlidingWindow(key, config.maxRequests(), config.windowSeconds())
                    .flatMap(allowed -> {
                        if (!allowed) {
                            return Mono.error(new GatewayRateLimitException(
                                    "Rate limit exceeded: max " + config.maxRequests() +
                                    " requests per " + config.windowSeconds() + "s"));
                        }
                        return chain.filter(exchange);
                    })
                    .onErrorResume(e -> {
                        // GatewayRateLimitException — propagate as-is (không catch)
                        if (e instanceof GatewayRateLimitException) {
                            return Mono.error(e);
                        }
                        // Redis không available → fail open, không block request
                        log.warn("Rate limit Redis unavailable, failing open: {}", e.getMessage());
                        return chain.filter(exchange);
                    });
        };
    }

    private String buildKey(ServerHttpRequest request, Config config) {
        // Lấy tên service từ path: /api/v1/{service}/...
        String[] segments = request.getPath().value().split("/");
        String serviceId = segments.length > 3 ? segments[3] : "unknown";

        if (config.mode() == Mode.PER_USER) {
            // X-User-Id đã được JwtAuthFilter inject trước khi RateLimitFilter chạy
            String userId = request.getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return RATE_LIMIT_KEY_PREFIX + "user:" + userId + ":" + serviceId;
            }
            // Fallback về IP nếu public route (không có userId)
        }

        String ip = extractClientIp(request);
        return RATE_LIMIT_KEY_PREFIX + "ip:" + ip + ":" + serviceId;
    }

    private String extractClientIp(ServerHttpRequest request) {
        // X-Forwarded-For từ load balancer (lấy IP đầu tiên = client thực)
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    /**
     * Sliding Window Log — Redis Sorted Set
     * Score = timestamp ms, member = timestamp string (unique per request)
     * Window = [now - windowMs, now]
     */
    private Mono<Boolean> checkSlidingWindow(String key, int maxRequests, int windowSeconds) {
        long now         = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        return redisTemplate.opsForZSet()
                .count(key, Range.of(
                        Range.Bound.inclusive((double) windowStart),
                        Range.Bound.inclusive((double) now)))
                .flatMap(count -> {
                    if (count >= maxRequests) {
                        log.debug("Rate limit hit: key={}, count={}/{}", key, count, maxRequests);
                        return Mono.just(false);
                    }
                    // Thêm request hiện tại — member = timestamp:random để tránh trùng
                    String member = now + ":" + Thread.currentThread().threadId();
                    return redisTemplate.opsForZSet().add(key, member, (double) now)
                            // Dọn entries cũ hơn window (tránh set phình to)
                            .then(redisTemplate.opsForZSet().removeRangeByScore(
                                    key,
                                    Range.of(Range.Bound.unbounded(),
                                             Range.Bound.exclusive((double) windowStart))))
                            // TTL tự dọn key sau window
                            .then(redisTemplate.expire(key, Duration.ofSeconds(windowSeconds + 5)))
                            .thenReturn(true);
                });
    }

    public enum Mode { PER_USER, PER_IP }

    public record Config(int maxRequests, int windowSeconds, Mode mode) {

        public static Config perUser(int maxRequests, int windowSeconds) {
            return new Config(maxRequests, windowSeconds, Mode.PER_USER);
        }

        public static Config perIp(int maxRequests, int windowSeconds) {
            return new Config(maxRequests, windowSeconds, Mode.PER_IP);
        }
    }
}
