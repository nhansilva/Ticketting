package com.ticketing.common.exception;

import com.ticketing.common.dto.constants.Constants;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception — mọi domain exception đều extend class này.
 *
 * Thiết kế:
 * - errorCode: định danh loại lỗi (machine-readable) → dùng Constants.ErrorCodes
 * - httpStatus: HTTP status tương ứng → GlobalExceptionHandler đọc trực tiếp
 * - Không còn cần switch-case trong GlobalExceptionHandler để map status
 */
@Getter
public class TicketingException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public TicketingException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public TicketingException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /** Shorthand cho internal error (500) */
    public static TicketingException internal(String message) {
        return new TicketingException(message, Constants.ErrorCodes.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /** Shorthand cho not found (404) */
    public static TicketingException notFound(String message) {
        return new TicketingException(message, Constants.ErrorCodes.NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    /** Shorthand cho unauthorized (401) */
    public static TicketingException unauthorized(String message) {
        return new TicketingException(message, Constants.ErrorCodes.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
    }

    /** Shorthand cho forbidden (403) */
    public static TicketingException forbidden(String message) {
        return new TicketingException(message, Constants.ErrorCodes.FORBIDDEN, HttpStatus.FORBIDDEN);
    }

    /** Shorthand cho conflict (409) */
    public static TicketingException conflict(String message, String errorCode) {
        return new TicketingException(message, errorCode, HttpStatus.CONFLICT);
    }
}
