package com.ticketing.user.exception;

import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.exception.TicketingException;
import org.springframework.http.HttpStatus;

public class InvalidTokenException extends TicketingException {

    public InvalidTokenException(String message) {
        super(message, Constants.ErrorCodes.INVALID_TOKEN, HttpStatus.BAD_REQUEST);
    }
}
