package com.jclinical.records.domain.service;

import com.jclinical.records.domain.model.MedicalHistory;
import com.jclinical.records.domain.ports.in.ManageMedicalHistoryUseCase;
import com.jclinical.records.domain.ports.out.MedicalHistoryRepositoryPort;
import com.jclinical.records.domain.ports.out.MedicalHistoryTemplateRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MedicalHistoryService implements ManageMedicalHistoryUseCase {

    private final MedicalHistoryRepositoryPort historyRepository;
    private final MedicalHistoryTemplateRepositoryPort templateRepository;
    private final PatientValidatorPort patientValidator;

    public MedicalHistoryService(MedicalHistoryRepositoryPort historyRepository,
                                 MedicalHistoryTemplateRepositoryPort templateRepository,
                                 PatientValidatorPort patientValidator) {
        this.historyRepository = historyRepository;
        this.templateRepository = templateRepository;
        this.patientValidator = patientValidator;
    }

    @Override
    public Optional<MedicalHistory> getMedicalHistoryByTemplate(UUID patientId, UUID templateId, UUID clinicId) {
        validatePatient(patientId, clinicId);
        return historyRepository.findByPatientIdAndTemplateIdAndClinicId(patientId, templateId, clinicId);
    }

    @Override
    public List<MedicalHistory> getMedicalHistories(UUID patientId, UUID clinicId) {
        validatePatient(patientId, clinicId);
        return historyRepository.findByPatientIdAndClinicId(patientId, clinicId);
    }

    @Override
    public MedicalHistory saveMedicalHistory(UUID patientId, UUID clinicId, SaveHistoryCommand command) {
        validatePatient(patientId, clinicId);

        // Validar que la plantilla existe y pertenece a esta clínica
        if (!templateRepository.existsByIdAndClinicId(command.templateId(), clinicId)) {
            throw new IllegalArgumentException("La plantilla de historia clínica no existe para esta clínica.");
        }

        Optional<MedicalHistory> existingOpt = historyRepository.findByPatientIdAndTemplateIdAndClinicId(
                patientId, command.templateId(), clinicId);

        MedicalHistory history;
        if (existingOpt.isPresent()) {
            history = existingOpt.get();
            history.setAnswersJson(command.answersJson());
            history.setUpdatedAt(LocalDateTime.now());
        } else {
            history = MedicalHistory.builder()
                    .id(UUID.randomUUID())
                    .patientId(patientId)
                    .clinicId(clinicId)
                    .templateId(command.templateId())
                    .answersJson(command.answersJson())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        return historyRepository.save(history);
    }

    private void validatePatient(UUID patientId, UUID clinicId) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clínica.");
        }
    }
}
