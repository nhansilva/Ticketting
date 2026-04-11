package com.ticketing.common.lock.redis;

import com.ticketing.common.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Redis implementation của DistributedLock.
 * Dùng SETNX (SET NX EX) — atomic acquire.
 * Release dùng Lua script để atomic check-and-delete.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisDistributedLock implements DistributedLock {

    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<String> acquire(String lockKey, Duration timeout) {
        String lockToken = UUID.randomUUID().toString();
        return redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, timeout)
                .flatMap(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        log.debug("Lock acquired: key={} token={}", lockKey, lockToken);
                        return Mono.just(lockToken);
                    }
                    return Mono.error(new IllegalStateException("Lock unavailable: " + lockKey));
                });
    }

    @Override
    public Mono<Boolean> release(String lockKey, String lockToken) {
        RedisScript<Long> script = RedisScript.of(RELEASE_SCRIPT, Long.class);
        return redisTemplate.execute(script, List.of(lockKey), List.of(lockToken))
                .next()
                .map(result -> result != null && result == 1L)
                .doOnNext(released -> {
                    if (released) log.debug("Lock released: key={}", lockKey);
                    else log.warn("Lock not released (expired or token mismatch): key={}", lockKey);
                })
                .onErrorReturn(false);
    }

    @Override
    public <T> Mono<T> executeWithLock(String lockKey, Duration timeout, Mono<T> task) {
        return acquire(lockKey, timeout)
                .flatMap(token -> task
                        .doFinally(signal -> release(lockKey, token)
                                .subscribe(r -> {}, e -> log.error("Release error: {}", e.getMessage()))));
    }
}
