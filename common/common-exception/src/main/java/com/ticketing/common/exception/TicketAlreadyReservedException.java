package com.ticketing.common.exception;

import com.ticketing.common.dto.constants.Constants;

/**
 * Exception thrown when ticket is already reserved
 */
public class TicketAlreadyReservedException extends TicketingException {
    public TicketAlreadyReservedException(String message) {
        super(message, Constants.ErrorCodes.TICKET_ALREADY_RESERVED);
    }

    public TicketAlreadyReservedException(String message, Throwable cause) {
        super(message, Constants.ErrorCodes.TICKET_ALREADY_RESERVED, cause);
    }
}

