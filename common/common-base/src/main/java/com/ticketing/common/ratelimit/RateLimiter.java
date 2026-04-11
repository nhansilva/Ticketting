package com.ticketing.common.ratelimit;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Rate limiting abstraction — Strategy pattern.
 * Implementation: Redis Sliding Window (RedisRateLimiter).
 *
 * Cách dùng trong service:
 * <pre>
 *   return rateLimiter.checkOrThrow("booking:" + userId, 5, Duration.ofMinutes(1))
 *       .then(bookingService.createBooking(...));
 * </pre>
 */
public interface RateLimiter {

    /**
     * Kiểm tra xem request có được phép không.
     *
     * @param key         Rate limit key (userId, IP, apiKey...)
     * @param maxRequests Số request tối đa trong window
     * @param window      Thời gian window
     * @return true nếu được phép, false nếu vượt limit
     */
    Mono<Boolean> isAllowed(String key, int maxRequests, Duration window);

    /**
     * Kiểm tra và throw exception nếu vượt limit.
     * Dùng trong reactive chain với .then() hoặc .flatMap().
     */
    Mono<Void> checkOrThrow(String key, int maxRequests, Duration window);
}
