package com.ticketing.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Chuẩn hóa pagination query params — record (Java 25).
 * Controller nhận qua @ModelAttribute hoặc @RequestParam riêng lẻ.
 *
 * Ví dụ URL: GET /api/v1/events?page=0&size=20&sortBy=startTime&sortDir=asc
 */
@Schema(description = "Pagination request parameters")
public record PageRequest(

        @Min(0)
        @Schema(description = "Trang hiện tại (0-based)", defaultValue = "0", example = "0")
        int page,

        @Min(1) @Max(100)
        @Schema(description = "Số phần tử mỗi trang (max 100)", defaultValue = "20", example = "20")
        int size,

        @Schema(description = "Field để sort", example = "createdAt")
        String sortBy,

        @Schema(description = "Chiều sort: asc | desc", defaultValue = "desc", example = "desc")
        String sortDir
) {
    /** Default constructor — page=0, size=20, sort by createdAt desc */
    public PageRequest() {
        this(0, 20, "createdAt", "desc");
    }

    public boolean isAscending() {
        return "asc".equalsIgnoreCase(sortDir);
    }

    public int offset() {
        return page * size;
    }
}
