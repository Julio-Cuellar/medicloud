package com.jclinical.shared.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private T data;
    private ApiMeta meta;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(ApiMeta.builder()
                        .requestId(UUID.randomUUID().toString())
                        .timestamp(Instant.now())
                        .build())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(ApiMeta.builder()
                        .requestId(requestId)
                        .timestamp(Instant.now())
                        .build())
                .build();
    }
}
