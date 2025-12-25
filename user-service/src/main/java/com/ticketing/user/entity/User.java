package com.ticketing.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

    @Id
    @Column("id")
    private UUID id;

    @Column("email")
    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("phone_number")
    private String phoneNumber;

    @Column("date_of_birth")
    private LocalDate dateOfBirth;

    @Column("profile_image_url")
    private String profileImageUrl;

    @Column("email_verified")
    private Boolean emailVerified;

    @Column("status")
    private UserStatus status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("last_login_at")
    private LocalDateTime lastLoginAt;

    @Column("deleted_at")
    private LocalDateTime deletedAt;

    /**
     * User status enumeration
     */
    public enum UserStatus {
        PENDING_VERIFICATION,
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        DELETED
    }
}

