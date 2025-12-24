package com.ticketing.common.exception;

import com.ticketing.common.dto.constants.Constants;

/**
 * Exception thrown when there is insufficient inventory
 */
public class InsufficientInventoryException extends TicketingException {
    public InsufficientInventoryException(String message) {
        super(message, Constants.ErrorCodes.INSUFFICIENT_INVENTORY);
    }

    public InsufficientInventoryException(String message, Throwable cause) {
        super(message, Constants.ErrorCodes.INSUFFICIENT_INVENTORY, cause);
    }
}

