package com.ticketing.user.repository;

import com.ticketing.user.entity.VerificationToken;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Verification token repository
 */
@Repository
public interface VerificationTokenRepository extends ReactiveCrudRepository<VerificationToken, UUID> {

    /**
     * Find token by token string
     */
    Mono<VerificationToken> findByToken(String token);

    /**
     * Find valid token by token string and type
     */
    @Query("SELECT * FROM verification_tokens WHERE token = :token AND token_type = :tokenType AND expires_at > :now AND used_at IS NULL")
    Mono<VerificationToken> findValidToken(String token, VerificationToken.TokenType tokenType, LocalDateTime now);

    /**
     * Delete expired tokens
     */
    @Query("DELETE FROM verification_tokens WHERE expires_at < :now")
    Mono<Void> deleteExpiredTokens(LocalDateTime now);

    /**
     * Delete tokens by user id
     */
    Mono<Void> deleteByUserId(UUID userId);
}

