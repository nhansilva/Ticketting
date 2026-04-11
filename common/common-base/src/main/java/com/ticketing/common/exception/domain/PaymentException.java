package com.ticketing.common.exception.domain;

import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.exception.TicketingException;
import org.springframework.http.HttpStatus;

/** 402 Payment Required — payment failed hoặc timeout. */
public class PaymentException extends TicketingException {

    public PaymentException(String message) {
        super(message, Constants.ErrorCodes.PAYMENT_FAILED, HttpStatus.PAYMENT_REQUIRED);
    }

    public PaymentException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.PAYMENT_REQUIRED);
    }
}
