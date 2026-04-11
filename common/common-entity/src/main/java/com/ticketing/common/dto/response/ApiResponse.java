package com.ticketing.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard API response wrapper — Builder pattern.
 *
 * Success (single object):
 *   { "success": true, "data": {...}, "meta": { "timestamp": "..." } }
 *
 * Success (paginated):
 *   { "success": true, "data": [...], "meta": { "page": 1, "limit": 20, "total": 150 } }
 *
 * Error:
 *   { "success": false, "message": "...", "errorCode": "SEAT_ALREADY_LOCKED", "meta": {...} }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "true nếu request thành công")
    private boolean success;

    @Schema(description = "Human-readable message")
    private String message;

    @Schema(description = "Response payload")
    private T data;

    @Schema(description = "Error code khi success=false", example = "USER_NOT_FOUND")
    private String errorCode;

    @Schema(description = "Metadata: timestamp, pagination info")
    private Meta meta;

    // ── Factory methods ────────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .meta(Meta.now())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .meta(Meta.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .meta(Meta.now())
                .build();
    }

    // ── Meta inner record ──────────────────────────────────────────────────

    /**
     * Metadata đính kèm mỗi response.
     * Dùng record (Java 25) — immutable, tự có equals/hashCode/toString.
     */
    @Schema(description = "Response metadata")
    public record Meta(
            @Schema(description = "Thời điểm server xử lý request") Instant timestamp,
            @Schema(description = "Tổng số phần tử (paginated)") Long total,
            @Schema(description = "Trang hiện tại (0-based)") Integer page,
            @Schema(description = "Kích thước trang") Integer size
    ) {
        /** Tạo Meta cơ bản — chỉ có timestamp */
        public static Meta now() {
            return new Meta(Instant.now(), null, null, null);
        }

        /** Tạo Meta cho paginated response */
        public static Meta paged(long total, int page, int size) {
            return new Meta(Instant.now(), total, page, size);
        }
    }
}
