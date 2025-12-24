package com.ticketing.common.exception;

import com.ticketing.common.dto.constants.Constants;

/**
 * Exception thrown when booking is not found
 */
public class BookingNotFoundException extends TicketingException {
    public BookingNotFoundException(String message) {
        super(message, Constants.ErrorCodes.BOOKING_NOT_FOUND);
    }

    public BookingNotFoundException(String message, Throwable cause) {
        super(message, Constants.ErrorCodes.BOOKING_NOT_FOUND, cause);
    }
}

