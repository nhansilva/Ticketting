package com.ticketing.common.ratelimit.redis;

import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.exception.TicketingException;
import com.ticketing.common.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Sliding Window Log rate limiter — Redis Sorted Set.
 * Score = timestamp ms → đếm requests trong [now - window, now].
 */
@Slf4j
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = Constants.RedisKeys.RATE_LIMIT;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Boolean> isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = KEY_PREFIX + key;
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        return redisTemplate.opsForZSet()
                .count(redisKey, Range.of(
                        Range.Bound.inclusive((double) windowStart),
                        Range.Bound.inclusive((double) now)))
                .flatMap(count -> {
                    if (count >= maxRequests) {
                        return Mono.just(false);
                    }
                    String member = now + ":" + Thread.currentThread().threadId();
                    return redisTemplate.opsForZSet().add(redisKey, member, (double) now)
                            .then(redisTemplate.opsForZSet().removeRangeByScore(
                                    redisKey,
                                    Range.of(Range.Bound.unbounded(), Range.Bound.exclusive((double) windowStart))))
                            .then(redisTemplate.expire(redisKey, window.plusSeconds(5)))
                            .thenReturn(true);
                })
                .onErrorReturn(true); // fail open
    }

    @Override
    public Mono<Void> checkOrThrow(String key, int maxRequests, Duration window) {
        return isAllowed(key, maxRequests, window)
                .flatMap(allowed -> {
                    if (!allowed) {
                        log.warn("Rate limit exceeded: key={}, max={}/{}", key, maxRequests, window);
                        return Mono.error(new TicketingException(
                                "Rate limit exceeded. Try again later.",
                                Constants.ErrorCodes.RATE_LIMIT_EXCEEDED,
                                HttpStatus.TOO_MANY_REQUESTS));
                    }
                    return Mono.empty();
                });
    }
}
