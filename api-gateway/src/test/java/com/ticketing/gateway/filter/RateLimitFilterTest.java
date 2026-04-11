package com.ticketing.gateway.filter;

import com.ticketing.gateway.exception.GatewayRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter Tests")
class RateLimitFilterTest {

    @Mock private ReactiveRedisOperations<String, String> redisTemplate;
    @Mock private ReactiveZSetOperations<String, String> zSetOps;
    @Mock private GatewayFilterChain chain;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(redisTemplate);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        // lenient: chain.filter chỉ được gọi khi request được allow, không gọi khi rate limited
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("count < maxRequests → request được phép qua")
    void shouldAllowWhenUnderLimit() {
        when(zSetOps.count(anyString(), any(Range.class))).thenReturn(Mono.just(5L));
        when(zSetOps.add(anyString(), anyString(), anyDouble())).thenReturn(Mono.just(true));
        when(zSetOps.removeRangeByScore(anyString(), any(Range.class))).thenReturn(Mono.just(0L));
        when(redisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/bookings/my")
                        .header("X-User-Id", "user-123")
                        .build());

        StepVerifier.create(filter.apply(
                        RateLimitFilter.Config.perUser(10, 60)).filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("count >= maxRequests → lỗi GatewayRateLimitException")
    void shouldBlockWhenOverLimit() {
        when(zSetOps.count(anyString(), any(Range.class))).thenReturn(Mono.just(10L));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/bookings/my")
                        .header("X-User-Id", "user-123")
                        .build());

        StepVerifier.create(filter.apply(
                        RateLimitFilter.Config.perUser(10, 60)).filter(exchange, chain))
                .expectError(GatewayRateLimitException.class)
                .verify();
    }

    @Test
    @DisplayName("Redis unavailable → fail open (request vẫn được phép qua)")
    void shouldFailOpenWhenRedisDown() {
        when(zSetOps.count(anyString(), any(Range.class)))
                .thenReturn(Mono.error(new RuntimeException("Redis connection refused")));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/bookings/my")
                        .header("X-User-Id", "user-123")
                        .build());

        StepVerifier.create(filter.apply(
                        RateLimitFilter.Config.perUser(10, 60)).filter(exchange, chain))
                .verifyComplete(); // không throw, vẫn pass
    }

    @Test
    @DisplayName("PER_IP mode — key dựa trên X-Forwarded-For")
    void shouldUseIpKeyWhenPerIpMode() {
        when(zSetOps.count(anyString(), any(Range.class))).thenReturn(Mono.just(0L));
        when(zSetOps.add(anyString(), anyString(), anyDouble())).thenReturn(Mono.just(true));
        when(zSetOps.removeRangeByScore(anyString(), any(Range.class))).thenReturn(Mono.just(0L));
        when(redisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/events/1")
                        .header("X-Forwarded-For", "203.0.113.5, 10.0.0.1")
                        .build());

        StepVerifier.create(filter.apply(
                        RateLimitFilter.Config.perIp(200, 60)).filter(exchange, chain))
                .verifyComplete();
    }

    @Test
    @DisplayName("PER_USER mode không có X-User-Id → fallback về IP key")
    void shouldFallbackToIpWhenNoUserId() {
        when(zSetOps.count(anyString(), any(Range.class))).thenReturn(Mono.just(1L));
        when(zSetOps.add(anyString(), anyString(), anyDouble())).thenReturn(Mono.just(true));
        when(zSetOps.removeRangeByScore(anyString(), any(Range.class))).thenReturn(Mono.just(0L));
        when(redisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));

        // Không có X-User-Id header → public route
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/check-email").build());

        StepVerifier.create(filter.apply(
                        RateLimitFilter.Config.perUser(100, 60)).filter(exchange, chain))
                .verifyComplete();
    }
}
