package com.ticketing.user.exception;

import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.exception.TicketingException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends TicketingException {

    public InvalidCredentialsException(String message) {
        super(message, Constants.ErrorCodes.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
    }
}
