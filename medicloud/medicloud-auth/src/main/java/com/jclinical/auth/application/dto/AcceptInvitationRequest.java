package com.jclinical.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AcceptInvitationRequest {
    @JsonProperty("invitation_token")
    private String invitationToken;
    
    private String password;
}
