package com.ticketing.gateway.exception;

import lombok.Getter;

@Getter
public class GatewayAuthException extends RuntimeException {

    private final String errorCode;

    public GatewayAuthException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
