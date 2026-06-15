package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserClinicDto {
    private UUID id;
    private String name;
    private String role;
    
    @JsonProperty("role_label")
    private String roleLabel;
    
    @JsonProperty("is_active")
    private boolean isActive;
}
