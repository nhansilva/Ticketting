package com.ticketing.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Database configuration for R2DBC
 * Connection factory is auto-configured by Spring Boot based on application.yml
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.ticketing.user.repository")
public class DatabaseConfig {
    // R2DBC connection factory is auto-configured by Spring Boot
    // based on spring.r2dbc.* properties in application.yml
}

