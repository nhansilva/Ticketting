package com.ticketing.common.exception;

import com.ticketing.common.dto.constants.Constants;
import lombok.Getter;

/**
 * Base exception for ticketing system
 */
@Getter
public class TicketingException extends RuntimeException {
    private final String errorCode;

    public TicketingException(String message) {
        super(message);
        this.errorCode = Constants.ErrorCodes.INTERNAL_ERROR;
    }

    public TicketingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TicketingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Constants.ErrorCodes.INTERNAL_ERROR;
    }

    public TicketingException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}

