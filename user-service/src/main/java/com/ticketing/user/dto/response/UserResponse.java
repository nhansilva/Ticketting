package com.ticketing.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String profileImageUrl;
    private Boolean emailVerified;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

