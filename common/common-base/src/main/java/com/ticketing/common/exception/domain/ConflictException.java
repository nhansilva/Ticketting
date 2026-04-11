package com.ticketing.common.exception.domain;

import com.ticketing.common.exception.TicketingException;
import org.springframework.http.HttpStatus;

/** 409 Conflict — duplicate resource, already reserved seat, etc. */
public class ConflictException extends TicketingException {

    public ConflictException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.CONFLICT);
    }
}
