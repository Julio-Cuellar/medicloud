package com.jclinical.shared.domain;

public class ResourceNotFoundException extends MedicloudException {
    public ResourceNotFoundException(String message) {
        super(message, ErrorCodes.RESOURCE_NOT_FOUND, 404);
    }
}
