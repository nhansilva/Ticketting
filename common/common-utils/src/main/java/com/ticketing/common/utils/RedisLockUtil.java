package com.ticketing.common.utils;

import com.ticketing.common.dto.constants.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Utility class for distributed locking using Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String LOCK_VALUE_PREFIX = "lock:value:";
    private static final int DEFAULT_LOCK_TIMEOUT_SECONDS = 30;

    /**
     * Acquire a distributed lock
     *
     * @param lockKey the lock key
     * @param timeoutSeconds lock timeout in seconds (auto-release)
     * @return lock identifier if acquired, empty if failed
     */
    public Mono<String> acquireLock(String lockKey, int timeoutSeconds) {
        String lockValue = LOCK_VALUE_PREFIX + UUID.randomUUID().toString();
        String fullLockKey = Constants.RedisKeys.TICKET_LOCK_PREFIX + lockKey;

        return redisTemplate.opsForValue()
                .setIfAbsent(fullLockKey, lockValue, Duration.ofSeconds(timeoutSeconds))
                .flatMap(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        log.debug("Lock acquired: {}", fullLockKey);
                        return Mono.just(lockValue);
                    } else {
                        log.debug("Lock not acquired: {}", fullLockKey);
                        return Mono.empty();
                    }
                });
    }

    /**
     * Acquire a distributed lock with default timeout
     */
    public Mono<String> acquireLock(String lockKey) {
        return acquireLock(lockKey, DEFAULT_LOCK_TIMEOUT_SECONDS);
    }

    /**
     * Release a distributed lock
     *
     * @param lockKey the lock key
     * @param lockValue the lock identifier returned from acquireLock
     * @return true if released successfully
     */
    public Mono<Boolean> releaseLock(String lockKey, String lockValue) {
        String fullLockKey = Constants.RedisKeys.TICKET_LOCK_PREFIX + lockKey;

        return redisTemplate.opsForValue()
                .get(fullLockKey)
                .flatMap(currentValue -> {
                    if (lockValue.equals(currentValue)) {
                        return redisTemplate.delete(fullLockKey)
                                .map(count -> count > 0);
                    } else {
                        log.warn("Lock value mismatch for key: {}", fullLockKey);
                        return Mono.just(false);
                    }
                })
                .defaultIfEmpty(false)
                .doOnNext(released -> {
                    if (released) {
                        log.debug("Lock released: {}", fullLockKey);
                    }
                });
    }

    /**
     * Execute a task with distributed lock
     *
     * @param lockKey the lock key
     * @param timeoutSeconds lock timeout
     * @param task the task to execute
     * @return result of the task
     */
    public <T> Mono<T> executeWithLock(String lockKey, int timeoutSeconds, Mono<T> task) {
        return acquireLock(lockKey, timeoutSeconds)
                .flatMap(lockValue -> task
                        .doFinally(signalType -> releaseLock(lockKey, lockValue).subscribe())
                        .switchIfEmpty(Mono.error(new RuntimeException("Lock not acquired: " + lockKey)))
                )
                .switchIfEmpty(Mono.error(new RuntimeException("Failed to acquire lock: " + lockKey)));
    }

    /**
     * Execute a task with distributed lock (default timeout)
     */
    public <T> Mono<T> executeWithLock(String lockKey, Mono<T> task) {
        return executeWithLock(lockKey, DEFAULT_LOCK_TIMEOUT_SECONDS, task);
    }
}

