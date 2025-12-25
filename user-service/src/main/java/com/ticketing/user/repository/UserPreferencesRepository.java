package com.ticketing.user.repository;

import com.ticketing.user.entity.UserPreferences;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * User preferences repository
 */
@Repository
public interface UserPreferencesRepository extends ReactiveCrudRepository<UserPreferences, UUID> {

    /**
     * Find preferences by user id
     */
    Mono<UserPreferences> findByUserId(UUID userId);

    /**
     * Delete preferences by user id
     */
    Mono<Void> deleteByUserId(UUID userId);
}

