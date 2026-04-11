package com.ticketing.common.cache;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache abstraction — Strategy pattern.
 * Service code phụ thuộc interface này, không phụ thuộc Redis trực tiếp.
 * Dễ dàng swap implementation (Redis → in-memory) cho testing.
 *
 * @param <K> Key type (thường là String)
 * @param <V> Value type
 */
public interface CacheStrategy<K, V> {

    /**
     * Lấy giá trị từ cache.
     *
     * @return Mono<Optional<V>> — empty Optional nếu cache miss, có value nếu hit
     */
    Mono<Optional<V>> get(K key);

    /**
     * Lưu giá trị vào cache với TTL.
     */
    Mono<Void> put(K key, V value, Duration ttl);

    /**
     * Xóa entry khỏi cache.
     */
    Mono<Void> evict(K key);

    /**
     * Cache-aside pattern: lấy từ cache, nếu miss thì gọi loader và cache kết quả.
     *
     * Ví dụ:
     * <pre>
     *   return cache.getOrLoad("event:" + id, Duration.ofMinutes(30),
     *       () -> eventRepository.findById(id).map(mapper::toDto));
     * </pre>
     */
    Mono<V> getOrLoad(K key, Duration ttl, java.util.function.Supplier<Mono<V>> loader);
}
