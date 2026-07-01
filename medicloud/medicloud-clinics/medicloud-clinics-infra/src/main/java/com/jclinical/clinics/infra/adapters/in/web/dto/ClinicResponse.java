package com.jclinical.clinics.infra.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClinicResponse(
    UUID id,
    UUID organizationId,
    UUID ownerUserId,
    String name,
    String legalName,
    String rfc,
    String taxRegimeCode,
    String addressStreet,
    String addressColonia,
    String addressMunicipality,
    String addressState,
    String addressZip,
    String phone,
    String email,
    String logoUrl,
    String timezone,
    String privacyNoticeUrl,
    LocalDateTime dataProcessorAgreedAt,
    UUID legalRepresentativeStaffId,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
