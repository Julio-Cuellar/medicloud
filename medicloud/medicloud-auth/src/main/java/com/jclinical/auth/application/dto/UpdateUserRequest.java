package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @JsonProperty("full_name")
    private String fullName;
    
    private String phone;
    
    @JsonProperty("theme_preference")
    private String themePreference;
}
