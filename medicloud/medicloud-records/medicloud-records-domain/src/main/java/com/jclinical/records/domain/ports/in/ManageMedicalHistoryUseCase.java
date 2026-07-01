package com.jclinical.records.domain.ports.in;

import com.jclinical.records.domain.model.MedicalHistory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageMedicalHistoryUseCase {

    Optional<MedicalHistory> getMedicalHistoryByTemplate(UUID patientId, UUID templateId, UUID clinicId);

    List<MedicalHistory> getMedicalHistories(UUID patientId, UUID clinicId);

    MedicalHistory saveMedicalHistory(UUID patientId, UUID clinicId, SaveHistoryCommand command);

    record SaveHistoryCommand(
        UUID templateId,
        String answersJson
    ) {}
}
