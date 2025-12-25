package com.ticketing.user.exception;

import com.ticketing.common.exception.TicketingException;

/**
 * Exception thrown when token is invalid or expired
 */
public class InvalidTokenException extends TicketingException {
    public InvalidTokenException(String message) {
        super(message, "INVALID_TOKEN");
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, "INVALID_TOKEN", cause);
    }
}

