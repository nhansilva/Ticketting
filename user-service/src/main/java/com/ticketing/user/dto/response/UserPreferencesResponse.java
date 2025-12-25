package com.ticketing.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * User preferences response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesResponse {

    private UUID userId;
    private Boolean emailNotifications;
    private Boolean smsNotifications;
    private Boolean pushNotifications;
    private String preferredLanguage;
    private String timezone;
    private String currency;
    private Boolean marketingEmails;
}

