package com.ticketing.user.config;

import com.ticketing.user.infrastructure.oauth2.OAuth2FailureHandler;
import com.ticketing.user.infrastructure.oauth2.OAuth2SuccessHandler;
import com.ticketing.user.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // OAuth2 endpoints
                        .pathMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
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
                        .pathMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .pathMatchers("/v3/api-docs/**", "/v3/api-docs").permitAll()
                        .pathMatchers("/webjars/**").permitAll()
                        .pathMatchers("/api/v1/users/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authenticationSuccessHandler(oAuth2SuccessHandler)
                        .authenticationFailureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
