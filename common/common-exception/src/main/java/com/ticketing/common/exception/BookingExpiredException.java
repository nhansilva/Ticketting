package com.ticketing.common.exception;

import com.ticketing.common.dto.constants.Constants;

/**
 * Exception thrown when booking reservation has expired
 */
public class BookingExpiredException extends TicketingException {
    public BookingExpiredException(String message) {
        super(message, Constants.ErrorCodes.BOOKING_EXPIRED);
    }

    public BookingExpiredException(String message, Throwable cause) {
        super(message, Constants.ErrorCodes.BOOKING_EXPIRED, cause);
    }
}

