package com.ticketing.common.utils;

import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Utility class for rate limiting using Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterUtil {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * Check if rate limit is exceeded
     *
     * @param key rate limit key (e.g., userId:endpoint)
     * @param maxRequests maximum requests allowed
     * @param windowSeconds time window in seconds
     * @return true if allowed, throws exception if exceeded
     */
    public Mono<Boolean> checkRateLimit(String key, int maxRequests, int windowSeconds) {
        String fullKey = Constants.RedisKeys.RATE_LIMIT_PREFIX + key;
        double currentTime = System.currentTimeMillis();
        double windowStart = currentTime - (windowSeconds * 1000.0);

        // Use sliding window log algorithm
        return redisTemplate.opsForZSet()
                .count(fullKey, org.springframework.data.domain.Range.of(
                        org.springframework.data.domain.Range.Bound.inclusive(windowStart),
                        org.springframework.data.domain.Range.Bound.inclusive(currentTime)))
                .flatMap(count -> {
                    if (count >= maxRequests) {
                        log.warn("Rate limit exceeded for key: {}", key);
                        return Mono.error(new RateLimitExceededException(
                                "Rate limit exceeded. Maximum " + maxRequests + 
                                " requests per " + windowSeconds + " seconds"));
                    }

                    // Add current request to the window
                    return redisTemplate.opsForZSet()
                            .add(fullKey, String.valueOf(currentTime), currentTime)
                            .then(redisTemplate.expire(fullKey, Duration.ofSeconds(windowSeconds)))
                            .thenReturn(true);
                });
    }

    /**
     * Simple rate limiting using counter (fixed window)
     *
     * @param key rate limit key
     * @param maxRequests maximum requests allowed
     * @param windowSeconds time window in seconds
     * @return true if allowed
     */
    public Mono<Boolean> checkRateLimitSimple(String key, int maxRequests, int windowSeconds) {
        String fullKey = Constants.RedisKeys.RATE_LIMIT_PREFIX + key;

        return redisTemplate.opsForValue()
                .increment(fullKey)
                .flatMap(count -> {
                    if (count == 1) {
                        // First request, set expiration
                        return redisTemplate.expire(fullKey, Duration.ofSeconds(windowSeconds))
                                .thenReturn(true);
                    }

                    if (count > maxRequests) {
                        log.warn("Rate limit exceeded for key: {}", key);
                        return Mono.error(new RateLimitExceededException(
                                "Rate limit exceeded. Maximum " + maxRequests + 
                                " requests per " + windowSeconds + " seconds"));
                    }

                    return Mono.just(true);
                });
    }
}

