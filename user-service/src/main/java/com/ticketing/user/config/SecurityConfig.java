package com.ticketing.user.config;

import com.ticketing.user.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers("/api/v1/users/register").permitAll()
                        .pathMatchers("/api/v1/users/login").permitAll()
                        .pathMatchers("/api/v1/users/refresh-token").permitAll()
                        .pathMatchers("/api/v1/users/verification/**").permitAll()
                        .pathMatchers("/api/v1/users/password/forgot").permitAll()
                        .pathMatchers("/api/v1/users/password/reset").permitAll()
                        .pathMatchers("/api/v1/users/check-email").permitAll()
                        .pathMatchers("/api/v1/users/account/reactivate").permitAll()
                        .pathMatchers("/api/v1/users/health").permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        // Swagger UI
                        .pathMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .pathMatchers("/v3/api-docs/**", "/v3/api-docs").permitAll()
                        .pathMatchers("/webjars/**").permitAll()
                        // Admin-only endpoints
                        .pathMatchers("/api/v1/users/admin/**").hasRole("ADMIN")
                        // Protected endpoints
                        .anyExchange().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

