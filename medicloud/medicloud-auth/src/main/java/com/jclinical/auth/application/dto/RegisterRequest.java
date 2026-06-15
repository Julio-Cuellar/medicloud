package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RegisterRequest {
    @JsonProperty("full_name")
    private String fullName;
    
    private String email;
    private String phone;
    private String password;
}
