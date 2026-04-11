package com.ticketing.common.exception.domain;

import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.exception.TicketingException;
import org.springframework.http.HttpStatus;

/** 401 Unauthorized — invalid token, wrong credentials. */
public class UnauthorizedException extends TicketingException {

    public UnauthorizedException(String message) {
        super(message, Constants.ErrorCodes.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED);
    }
}
