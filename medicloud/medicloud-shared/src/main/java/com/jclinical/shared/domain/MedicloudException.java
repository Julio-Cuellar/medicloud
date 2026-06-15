package com.jclinical.shared.domain;

import lombok.Getter;

@Getter
public class MedicloudException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public MedicloudException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
