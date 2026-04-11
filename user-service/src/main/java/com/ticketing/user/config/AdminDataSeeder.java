package com.ticketing.user.config;

import com.ticketing.user.entity.User;
import com.ticketing.user.entity.UserPreferences;
import com.ticketing.user.repository.UserPreferencesRepository;
import com.ticketing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataSeeder {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.email:admin@ticketing.com}")
    private String adminEmail;

    @Value("${admin.seed.password:Admin@123}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void seedAdmin() {
        userRepository.existsByEmail(adminEmail)
                .filter(exists -> !exists)
                .flatMap(v -> {
                    User admin = User.builder()
                            .id(UUID.randomUUID())
                            .email(adminEmail)
                            .passwordHash(passwordEncoder.encode(adminPassword))
                            .firstName("System")
                            .lastName("Admin")
                            .emailVerified(true)
                            .role(User.UserRole.ADMIN)
                            .status(User.UserStatus.ACTIVE)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(admin);
                })
                .flatMap(admin -> {
                    UserPreferences prefs = UserPreferences.builder()
                            .id(UUID.randomUUID())
                            .userId(admin.getId())
                            .emailNotifications(true)
                            .smsNotifications(false)
                            .pushNotifications(true)
                            .preferredLanguage("en")
                            .timezone("Asia/Ho_Chi_Minh")
                            .currency("VND")
                            .marketingEmails(false)
                            .build();
                    return preferencesRepository.save(prefs);
                })
                .subscribe(
                        prefs -> log.info("Admin user seeded: {}", adminEmail),
                        err -> log.error("Failed to seed admin user: {}", err.getMessage())
                );
    }
}
