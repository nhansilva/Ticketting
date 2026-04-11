package com.ticketing.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Paginated response wrapper — record pattern (Java 25).
 *
 * Dùng với ApiResponse:
 * <pre>
 *   return ApiResponse.success(PageResponse.of(content, page, size, total));
 * </pre>
 */
@Schema(description = "Paginated list response")
public record PageResponse<T>(

        @Schema(description = "Danh sách phần tử trang hiện tại")
        List<T> content,

        @Schema(description = "Trang hiện tại (0-based)", example = "0")
        int page,

        @Schema(description = "Số phần tử mỗi trang", example = "20")
        int size,

        @Schema(description = "Tổng số phần tử", example = "150")
        long totalElements,

        @Schema(description = "Tổng số trang", example = "8")
        int totalPages,

        @Schema(description = "Có phải trang cuối không")
        boolean last
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;
        return new PageResponse<>(content, page, size, totalElements, totalPages, last);
    }

    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0, 0, true);
    }
}
