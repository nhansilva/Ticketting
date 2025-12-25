package com.ticketing.user.exception;

import com.ticketing.common.exception.TicketingException;

/**
 * Exception thrown when email already exists
 */
public class EmailAlreadyExistsException extends TicketingException {
    public EmailAlreadyExistsException(String message) {
        super(message, "EMAIL_ALREADY_EXISTS");
    }

    public EmailAlreadyExistsException(String message, Throwable cause) {
        super(message, "EMAIL_ALREADY_EXISTS", cause);
    }
}

