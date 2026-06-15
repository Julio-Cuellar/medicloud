package com.jclinical.auth.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Estado en el que se encuentra una invitación de personal médico o administrativo.
 */
public enum InvitationStatus {
    /** La invitación ha sido enviada pero aún no ha sido respondida. */
    PENDING,
    /** La invitación fue aceptada con éxito por el destinatario. */
    ACCEPTED,
    /** El plazo de validez de la invitación ha vencido. */
    EXPIRED,
    /** La invitación fue cancelada o revocada por un administrador antes de ser aceptada. */
    REVOKED;

    /**
     * Obtiene el valor textual en minúsculas del estado para persistencia o API.
     *
     * @return El nombre del enum en minúsculas.
     */
    public String getValue() {
        return name().toLowerCase();
    }

    /**
     * Mapea un valor textual de estado a su correspondiente constante enum.
     *
     * @param value El valor textual a mapear.
     * @return La constante {@link InvitationStatus} equivalente, o {@code null} si el valor provisto es nulo.
     */
    public static InvitationStatus fromValue(String value) {
        if (value == null) return null;
        return InvitationStatus.valueOf(value.toUpperCase());
    }

    /**
     * Conversor JPA automático para mapear el enum {@link InvitationStatus} a su representación de base de datos.
     */
    @Converter(autoApply = true)
    public static class InvitationStatusConverter implements AttributeConverter<InvitationStatus, String> {
        
        /**
         * Convierte el enum del estado de invitación a la cadena de base de datos correspondiente.
         */
        @Override
        public String convertToDatabaseColumn(InvitationStatus attribute) {
            return attribute == null ? null : attribute.getValue();
        }

        /**
         * Mapea la cadena de base de datos al correspondiente enum.
         */
        @Override
        public InvitationStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : InvitationStatus.fromValue(dbData);
        }
    }
}
