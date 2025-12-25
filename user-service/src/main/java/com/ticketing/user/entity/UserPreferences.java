package com.ticketing.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/**
 * User preferences entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_preferences")
public class UserPreferences {

    @Id
    @Column("id")
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("email_notifications")
    private Boolean emailNotifications;

    @Column("sms_notifications")
    private Boolean smsNotifications;

    @Column("push_notifications")
    private Boolean pushNotifications;

    @Column("preferred_language")
    private String preferredLanguage;

    @Column("timezone")
    private String timezone;

    @Column("currency")
    private String currency;

    @Column("marketing_emails")
    private Boolean marketingEmails;
}

