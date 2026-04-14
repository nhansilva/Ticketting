package com.ticketing.catalog.common.exception;

import com.ticketing.common.exception.domain.ResourceNotFoundException;

public class VenueNotFoundException extends ResourceNotFoundException {
    public VenueNotFoundException(String id) {
        super("Venue", id);
    }
}
