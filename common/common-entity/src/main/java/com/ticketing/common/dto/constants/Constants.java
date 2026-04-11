package com.ticketing.common.dto.constants;

/**
 * Centralized constants — toàn bộ services dùng chung.
 * Nhóm theo nested static class để tránh pollution.
 */
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Seat lock TTL: 10 phút (theo business rule) */
    public static final int SEAT_LOCK_TTL_SECONDS = 600;

    /** Payment timeout: 15 phút từ khi tạo Booking */
    public static final int PAYMENT_TIMEOUT_SECONDS = 900;

    /** Max vé mỗi user mỗi event */
    public static final int MAX_TICKETS_PER_USER = 4;

    // ── Redis key prefixes ───────────────────────────────────────────────

    public static final class RedisKeys {
        /** seat lock: booking:seat:{seatId} → userId */
        public static final String SEAT_LOCK        = "booking:seat:";
        public static final String EVENT_CACHE      = "cache:event:";
        public static final String INVENTORY        = "inventory:";
        public static final String TICKET_LOCK      = "lock:ticket:";
        public static final String RATE_LIMIT       = "rate:";
        public static final String CACHE            = "cache:";
        public static final String DISTRIBUTED_LOCK = "lock:";

        private RedisKeys() {}
    }

    // ── Kafka topics ─────────────────────────────────────────────────────

    public static final class KafkaTopics {
        // Booking
        public static final String BOOKING_CREATED   = "booking.created";
        public static final String BOOKING_EXPIRED   = "booking.expired";
        // Payment
        public static final String PAYMENT_COMPLETED = "payment.completed";
        public static final String PAYMENT_FAILED    = "payment.failed";
        // Ticket
        public static final String TICKET_ISSUED     = "ticket.issued";
        public static final String TICKET_RESERVED   = "ticket.reserved";
        public static final String TICKET_RELEASED   = "ticket.released";
        // Event catalog
        public static final String EVENT_PUBLISHED   = "event.published";
        public static final String EVENT_UPDATED     = "event.updated";
        // Media
        public static final String MEDIA_UPLOADED    = "media.uploaded";
        // Notification
        public static final String NOTIFICATION_SEND = "notification.send";

        private KafkaTopics() {}
    }

    // ── HTTP Headers ─────────────────────────────────────────────────────

    public static final class Headers {
        public static final String USER_ID        = "X-User-Id";
        public static final String USER_ROLE      = "X-User-Role";
        public static final String CORRELATION_ID = "X-Correlation-Id";
        public static final String REQUEST_ID     = "X-Request-Id";

        private Headers() {}
    }

    // ── Error codes ──────────────────────────────────────────────────────

    public static final class ErrorCodes {
        // Auth
        public static final String EMAIL_ALREADY_EXISTS  = "EMAIL_ALREADY_EXISTS";
        public static final String INVALID_CREDENTIALS   = "INVALID_CREDENTIALS";
        public static final String USER_NOT_FOUND        = "USER_NOT_FOUND";
        public static final String INVALID_TOKEN         = "INVALID_TOKEN";
        public static final String TOKEN_EXPIRED         = "TOKEN_EXPIRED";
        public static final String UNAUTHORIZED          = "UNAUTHORIZED";
        public static final String FORBIDDEN             = "FORBIDDEN";

        // Booking / Seat
        public static final String SEAT_NOT_AVAILABLE    = "SEAT_NOT_AVAILABLE";
        public static final String SEAT_ALREADY_LOCKED   = "SEAT_ALREADY_LOCKED";
        public static final String TICKET_NOT_AVAILABLE  = "TICKET_NOT_AVAILABLE";
        public static final String TICKET_ALREADY_RESERVED = "TICKET_ALREADY_RESERVED";
        public static final String BOOKING_NOT_FOUND     = "BOOKING_NOT_FOUND";
        public static final String BOOKING_EXPIRED       = "BOOKING_EXPIRED";
        public static final String MAX_TICKETS_EXCEEDED  = "MAX_TICKETS_EXCEEDED";

        // Payment
        public static final String PAYMENT_FAILED        = "PAYMENT_FAILED";
        public static final String PAYMENT_NOT_FOUND     = "PAYMENT_NOT_FOUND";
        public static final String INVALID_SIGNATURE     = "INVALID_SIGNATURE";

        // Resource
        public static final String NOT_FOUND             = "NOT_FOUND";
        public static final String INSUFFICIENT_INVENTORY = "INSUFFICIENT_INVENTORY";

        // General
        public static final String VALIDATION_ERROR      = "VALIDATION_ERROR";
        public static final String RATE_LIMIT_EXCEEDED   = "RATE_LIMIT_EXCEEDED";
        public static final String INTERNAL_ERROR        = "INTERNAL_ERROR";
        public static final String SERVICE_UNAVAILABLE   = "SERVICE_UNAVAILABLE";

        private ErrorCodes() {}
    }
}
