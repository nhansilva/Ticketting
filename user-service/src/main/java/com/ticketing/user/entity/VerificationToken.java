package com.ticketing.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("verification_tokens")
public class VerificationToken implements Persistable<UUID> {

    @Transient
    private boolean isNew = false;

    @Id
    @Column("id")
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("token")
    private String token;

    @Column("token_type")
    private TokenType tokenType;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Column("used_at")
    private LocalDateTime usedAt;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Override
    public boolean isNew() {
        return isNew;
    }

    public enum TokenType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }
}
