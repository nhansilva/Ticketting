package com.ticketing.common.lock;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Distributed lock abstraction — Strategy pattern.
 * Implementation: Redis SETNX (RedisDistributedLock).
 * Dễ swap cho testing (in-memory lock).
 *
 * Cách dùng chuẩn — luôn dùng executeWithLock:
 * <pre>
 *   return lock.executeWithLock("booking:seat:" + seatId,
 *       bookingService.createBooking(userId, seatId));
 * </pre>
 */
public interface DistributedLock {

    /**
     * Acquire lock, trả về lock token (UUID) để release sau.
     *
     * @param lockKey   Redis key
     * @param timeout   TTL của lock
     * @return lock token nếu acquire thành công, error nếu lock đang bị giữ
     */
    Mono<String> acquire(String lockKey, Duration timeout);

    /**
     * Release lock — validate token trước khi xóa (tránh release của process khác).
     *
     * @param lockKey   Redis key
     * @param lockToken Token nhận được từ acquire()
     * @return true nếu release thành công
     */
    Mono<Boolean> release(String lockKey, String lockToken);

    /**
     * Execute task trong lock — tự acquire và release, kể cả khi task lỗi.
     * Đây là cách dùng khuyến khích — tránh quên release.
     *
     * @param lockKey  Redis key
     * @param timeout  Lock TTL
     * @param task     Task cần execute
     */
    <T> Mono<T> executeWithLock(String lockKey, Duration timeout, Mono<T> task);
}
