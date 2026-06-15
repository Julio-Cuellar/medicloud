package com.jclinical.shared.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiMeta {
    @JsonProperty("request_id")
    private String requestId;
    private Instant timestamp;
}
