package com.ticketing.user.exception;

import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.exception.TicketingException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends TicketingException {

    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email, Constants.ErrorCodes.EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT);
    }
}
