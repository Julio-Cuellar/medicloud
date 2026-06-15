package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @JsonProperty("current_password")
    private String currentPassword;
    
    @JsonProperty("new_password")
    private String newPassword;
}
