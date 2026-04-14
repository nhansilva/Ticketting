package com.ticketing.catalog.common.exception;

import com.ticketing.common.exception.domain.ResourceNotFoundException;

public class EventNotFoundException extends ResourceNotFoundException {
    public EventNotFoundException(String id) {
        super("Event", id);
    }
}
