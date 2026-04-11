package com.ticketing.common.cache.redis;

import com.ticketing.common.cache.CacheStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Redis implementation của CacheStrategy.
 * Value serialize thành JSON string để human-readable trên Redis CLI.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisCacheStrategy<V> implements CacheStrategy<String, V> {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Class<V> valueType;

    @Override
    public Mono<Optional<V>> get(String key) {
        return redisTemplate.opsForValue().get(key)
                .map(json -> {
                    try {
                        return Optional.of(objectMapper.readValue(json, valueType));
                    } catch (Exception e) {
                        log.warn("Cache deserialize failed for key={}: {}", key, e.getMessage());
                        return Optional.<V>empty();
                    }
                })
                .defaultIfEmpty(Optional.empty())
                .onErrorReturn(Optional.empty());
    }

    @Override
    public Mono<Void> put(String key, V value, Duration ttl) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(value))
                .flatMap(json -> redisTemplate.opsForValue().set(key, json, ttl))
                .doOnError(e -> log.error("Cache put failed for key={}: {}", key, e.getMessage()))
                .then();
    }

    @Override
    public Mono<Void> evict(String key) {
        return redisTemplate.delete(key).then();
    }

    @Override
    public Mono<V> getOrLoad(String key, Duration ttl, Supplier<Mono<V>> loader) {
        return get(key)
                .flatMap(opt -> opt.map(Mono::just)
                        .orElseGet(() -> loader.get()
                                .flatMap(value -> put(key, value, ttl).thenReturn(value))));
    }
}
