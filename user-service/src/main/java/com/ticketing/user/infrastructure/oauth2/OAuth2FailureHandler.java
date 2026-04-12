package com.ticketing.user.infrastructure.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Component
public class OAuth2FailureHandler implements ServerAuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-uri:http://localhost:5173/auth/callback}")
    private String redirectUri;

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange exchange, AuthenticationException exception) {
        log.error("OAuth2 login failed: {}", exception.getMessage());

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", "oauth2_failed")
                .build().toUriString();

        exchange.getExchange().getResponse().getHeaders().setLocation(URI.create(targetUrl));
        exchange.getExchange().getResponse().setStatusCode(HttpStatus.FOUND);
        return exchange.getExchange().getResponse().setComplete();
    }
}
