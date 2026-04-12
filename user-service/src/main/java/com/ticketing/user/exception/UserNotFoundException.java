package com.ticketing.user.exception;

import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.exception.TicketingException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends TicketingException {

    public UserNotFoundException(String message) {
        super(message, Constants.ErrorCodes.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
}
