package com.ticketing.catalog.common.exception;

import com.ticketing.common.dto.enums.EventStatus;
import com.ticketing.common.exception.domain.ConflictException;

public class InvalidStatusTransitionException extends ConflictException {
    public InvalidStatusTransitionException(EventStatus from, EventStatus to) {
        super("Invalid status transition: " + from + " → " + to, "INVALID_STATUS_TRANSITION");
    }
}
