package com.jclinical.auth.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED;

    public String getValue() {
        return name().toLowerCase();
    }

    public static InvitationStatus fromValue(String value) {
        if (value == null) return null;
        return InvitationStatus.valueOf(value.toUpperCase());
    }

    @Converter(autoApply = true)
    public static class InvitationStatusConverter implements AttributeConverter<InvitationStatus, String> {
        @Override
        public String convertToDatabaseColumn(InvitationStatus attribute) {
            return attribute == null ? null : attribute.getValue();
        }

        @Override
        public InvitationStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : InvitationStatus.fromValue(dbData);
        }
    }
}
