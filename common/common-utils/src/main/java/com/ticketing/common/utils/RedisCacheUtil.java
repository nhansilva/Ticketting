package com.ticketing.common.utils;

import com.ticketing.common.dto.constants.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Utility class for Redis caching
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheUtil {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * Get value from cache
     *
     * @param key cache key
     * @return cached value or empty if not found
     */
    public Mono<String> get(String key) {
        String fullKey = Constants.RedisKeys.CACHE_PREFIX + key;
        return redisTemplate.opsForValue()
                .get(fullKey)
                .doOnNext(value -> log.debug("Cache hit: {}", fullKey))
                .doOnError(error -> log.debug("Cache miss: {}", fullKey));
    }

    /**
     * Set value in cache with TTL
     *
     * @param key cache key
     * @param value value to cache
     * @param ttlSeconds time to live in seconds
     * @return true if set successfully
     */
    public Mono<Boolean> set(String key, String value, int ttlSeconds) {
        String fullKey = Constants.RedisKeys.CACHE_PREFIX + key;
        return redisTemplate.opsForValue()
                .set(fullKey, value, Duration.ofSeconds(ttlSeconds))
                .doOnNext(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        log.debug("Cache set: {} with TTL: {}s", fullKey, ttlSeconds);
                    }
                });
    }

    /**
     * Delete value from cache
     *
     * @param key cache key
     * @return true if deleted successfully
     */
    public Mono<Boolean> delete(String key) {
        String fullKey = Constants.RedisKeys.CACHE_PREFIX + key;
        return redisTemplate.delete(fullKey)
                .map(count -> count > 0)
                .doOnNext(deleted -> {
                    if (Boolean.TRUE.equals(deleted)) {
                        log.debug("Cache deleted: {}", fullKey);
                    }
                });
    }

    /**
     * Get or compute: get from cache, if not found compute and cache
     *
     * @param key cache key
     * @param ttlSeconds time to live in seconds
     * @param computeFunction function to compute value if not in cache
     * @return cached or computed value
     */
    public Mono<String> getOrCompute(String key, int ttlSeconds, Mono<String> computeFunction) {
        return get(key)
                .switchIfEmpty(
                        computeFunction
                                .flatMap(value -> set(key, value, ttlSeconds)
                                        .thenReturn(value))
                );
    }
}

