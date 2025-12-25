package com.ticketing.user.exception;

import com.ticketing.common.exception.TicketingException;
import com.ticketing.common.dto.constants.Constants;

/**
 * Exception thrown when user is not found
 */
public class UserNotFoundException extends TicketingException {
    public UserNotFoundException(String message) {
        super(message, "USER_NOT_FOUND");
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, "USER_NOT_FOUND", cause);
    }
}

