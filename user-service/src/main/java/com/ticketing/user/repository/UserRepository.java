package com.ticketing.user.repository;

import com.ticketing.user.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * User repository
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {

    /**
     * Find user by email
     */
    Mono<User> findByEmail(String email);

    /**
     * Find user by email ignoring case
     */
    Mono<User> findByEmailIgnoreCase(String email);

    /**
     * Check if user exists by email
     */
    Mono<Boolean> existsByEmail(String email);

    /**
     * Find active user by email
     */
    @Query("SELECT * FROM users WHERE email = :email AND status = 'ACTIVE' AND deleted_at IS NULL")
    Mono<User> findActiveUserByEmail(String email);

    /**
     * Find user by id and status
     */
    @Query("SELECT * FROM users WHERE id = :id AND status = :status AND deleted_at IS NULL")
    Mono<User> findByIdAndStatus(UUID id, User.UserStatus status);
}

