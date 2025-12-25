package com.ticketing.user.exception;

import com.ticketing.common.exception.TicketingException;

/**
 * Exception thrown when credentials are invalid
 */
public class InvalidCredentialsException extends TicketingException {
    public InvalidCredentialsException(String message) {
        super(message, "INVALID_CREDENTIALS");
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, "INVALID_CREDENTIALS", cause);
    }
}

