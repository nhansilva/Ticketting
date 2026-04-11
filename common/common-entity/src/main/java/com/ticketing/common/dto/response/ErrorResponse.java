package com.ticketing.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Error response — trả về khi request thất bại ở tầng exception handler.
 * Tuân theo format: { "error": { "code": "...", "message": "...", "traceId": "..." } }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error response format")
public class ErrorResponse {

    @Schema(description = "Error code định danh loại lỗi", example = "USER_NOT_FOUND")
    private String errorCode;

    @Schema(description = "Mô tả lỗi cho developer")
    private String message;

    @Schema(description = "Thời điểm xảy ra lỗi")
    private Instant timestamp;

    @Schema(description = "Request path gây ra lỗi")
    private String path;

    @Schema(description = "Trace ID để correlate logs")
    private String traceId;

    @Schema(description = "Chi tiết lỗi validation từng field")
    private List<FieldError> fieldErrors;

    /**
     * Validation error per field — dùng record.
     */
    @Schema(description = "Validation error trên một field cụ thể")
    public record FieldError(
            @Schema(description = "Tên field lỗi") String field,
            @Schema(description = "Mô tả lỗi") String message,
            @Schema(description = "Giá trị bị reject") Object rejectedValue
    ) {}
}
