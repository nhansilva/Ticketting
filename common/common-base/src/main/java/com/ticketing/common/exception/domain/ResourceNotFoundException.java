package com.ticketing.common.exception.domain;

import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.exception.TicketingException;
import org.springframework.http.HttpStatus;

/**
 * Generic 404 — thay thế cho các *NotFoundException cụ thể.
 *
 * Dùng:
 *   throw new ResourceNotFoundException("User", userId);
 *   → "User not found: {id}"
 */
public class ResourceNotFoundException extends TicketingException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super(resourceName + " not found: " + id, Constants.ErrorCodes.NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, Constants.ErrorCodes.NOT_FOUND, HttpStatus.NOT_FOUND);
    }
}
