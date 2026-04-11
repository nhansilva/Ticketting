package com.ticketing.common.exception.domain;

import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.exception.TicketingException;
import org.springframework.http.HttpStatus;

public class BookingExpiredException extends TicketingException {

    public BookingExpiredException(String bookingId) {
        super("Booking đã hết hạn: " + bookingId,
              Constants.ErrorCodes.BOOKING_EXPIRED,
              HttpStatus.CONFLICT);
    }
}
