package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.MedicalHistory;
import com.jclinical.records.domain.ports.in.ManageMedicalHistoryUseCase;
import com.jclinical.records.domain.ports.in.ManageMedicalHistoryUseCase.SaveHistoryCommand;
import com.jclinical.records.infra.adapters.in.web.dto.MedicalHistoryResponse;
import com.jclinical.records.infra.adapters.in.web.dto.SaveMedicalHistoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/medical-history")
@RequiredArgsConstructor
public class MedicalHistoryController {

    private final ManageMedicalHistoryUseCase historyUseCase;

    @GetMapping
    public ResponseEntity<List<MedicalHistoryResponse>> getMedicalHistories(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId) {
        List<MedicalHistoryResponse> responses = historyUseCase.getMedicalHistories(patientId, clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-template/{templateId}")
    public ResponseEntity<MedicalHistoryResponse> getMedicalHistoryByTemplate(
            @PathVariable UUID patientId,
            @PathVariable UUID templateId,
            @RequestParam UUID clinicId) {
        return historyUseCase.getMedicalHistoryByTemplate(patientId, templateId, clinicId)
                .map(history -> ResponseEntity.ok(toResponse(history)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<MedicalHistoryResponse> saveMedicalHistory(
            @PathVariable UUID patientId,
            @RequestBody SaveMedicalHistoryRequest request) {
        SaveHistoryCommand command = new SaveHistoryCommand(
                request.templateId(),
                request.answersJson()
        );

        MedicalHistory history = historyUseCase.saveMedicalHistory(patientId, request.clinicId(), command);
        return ResponseEntity.ok(toResponse(history));
    }

    private MedicalHistoryResponse toResponse(MedicalHistory history) {
        return new MedicalHistoryResponse(
                history.getId(),
                history.getPatientId(),
                history.getClinicId(),
                history.getTemplateId(),
                history.getAnswersJson(),
                history.getCreatedAt(),
                history.getUpdatedAt()
        );
    }
}
