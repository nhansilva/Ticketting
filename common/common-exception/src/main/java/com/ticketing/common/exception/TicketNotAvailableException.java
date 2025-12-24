package com.ticketing.common.exception;

import com.ticketing.common.dto.constants.Constants;

/**
 * Exception thrown when ticket is not available
 */
public class TicketNotAvailableException extends TicketingException {
    public TicketNotAvailableException(String message) {
        super(message, Constants.ErrorCodes.TICKET_NOT_AVAILABLE);
    }

    public TicketNotAvailableException(String message, Throwable cause) {
        super(message, Constants.ErrorCodes.TICKET_NOT_AVAILABLE, cause);
    }
}

