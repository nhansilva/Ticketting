package com.ticketing.common.dto.constants;

/**
 * Application constants
 */
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Reservation timeout in seconds (30 minutes)
     */
    public static final int RESERVATION_TIMEOUT_SECONDS = 1800;

    /**
     * Redis key prefixes
     */
    public static final class RedisKeys {
        public static final String EVENT_PREFIX = "event:";
        public static final String INVENTORY_PREFIX = "inventory:";
        public static final String TICKET_LOCK_PREFIX = "lock:ticket:";
        public static final String RATE_LIMIT_PREFIX = "rate:";
        public static final String CACHE_PREFIX = "cache:";

        private RedisKeys() {
        }
    }

    /**
     * Kafka topics
     */
    public static final class KafkaTopics {
        public static final String TICKET_RESERVED = "ticket.reserved";
        public static final String TICKET_CONFIRMED = "ticket.confirmed";
        public static final String TICKET_RELEASED = "ticket.released";
        public static final String PAYMENT_PROCESSED = "payment.processed";
        public static final String BOOKING_CREATED = "booking.created";
        public static final String BOOKING_CONFIRMED = "booking.confirmed";
        public static final String BOOKING_CANCELLED = "booking.cancelled";
        public static final String NOTIFICATION_SEND = "notification.send";

        private KafkaTopics() {
        }
    }

    /**
     * HTTP Headers
     */
    public static final class Headers {
        public static final String USER_ID = "X-User-Id";
        public static final String CORRELATION_ID = "X-Correlation-Id";
        public static final String REQUEST_ID = "X-Request-Id";

        private Headers() {
        }
    }

    /**
     * Error codes
     */
    public static final class ErrorCodes {
        public static final String TICKET_NOT_AVAILABLE = "TICKET_NOT_AVAILABLE";
        public static final String TICKET_ALREADY_RESERVED = "TICKET_ALREADY_RESERVED";
        public static final String BOOKING_NOT_FOUND = "BOOKING_NOT_FOUND";
        public static final String BOOKING_EXPIRED = "BOOKING_EXPIRED";
        public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
        public static final String INSUFFICIENT_INVENTORY = "INSUFFICIENT_INVENTORY";
        public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
        public static final String VALIDATION_ERROR = "VALIDATION_ERROR";

        private ErrorCodes() {
        }
    }
}

