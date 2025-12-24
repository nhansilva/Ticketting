package com.ticketing.common.exception;

import com.ticketing.common.dto.constants.Constants;

/**
 * Exception thrown when rate limit is exceeded
 */
public class RateLimitExceededException extends TicketingException {
    public RateLimitExceededException(String message) {
        super(message, Constants.ErrorCodes.RATE_LIMIT_EXCEEDED);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(message, Constants.ErrorCodes.RATE_LIMIT_EXCEEDED, cause);
    }
}

