package com.ticketing.gateway.exception;

public class GatewayRateLimitException extends RuntimeException {

    public GatewayRateLimitException(String message) {
        super(message);
    }
}
