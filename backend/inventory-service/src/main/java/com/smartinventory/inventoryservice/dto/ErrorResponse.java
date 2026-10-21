package com.smartinventory.inventoryservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Standard uniform JSON response envelope for API errors.
 * Strictly adheres to Section 2.2 of API_SPECIFICATION.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private boolean success = false;

    private ErrorBody error;

    @Builder.Default
    private Instant timestamp = Instant.now();

    private String path;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorBody {

        private String code;

        private String message;

        @Builder.Default
        private List<ErrorDetail> details = Collections.emptyList();
    }

    public static ErrorResponse of(String code, String message, String path) {
        return ErrorResponse.builder()
                .success(false)
                .error(ErrorBody.builder()
                        .code(code)
                        .message(message)
                        .details(Collections.emptyList())
                        .build())
                .timestamp(Instant.now())
                .path(path)
                .build();
    }

    public static ErrorResponse of(String code, String message, List<ErrorDetail> details, String path) {
        return ErrorResponse.builder()
                .success(false)
                .error(ErrorBody.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .build())
                .timestamp(Instant.now())
                .path(path)
                .build();
    }
}
