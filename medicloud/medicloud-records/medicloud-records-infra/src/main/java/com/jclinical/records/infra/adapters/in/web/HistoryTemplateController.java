package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.MedicalHistoryTemplate;
import com.jclinical.records.domain.ports.in.ManageHistoryTemplateUseCase;
import com.jclinical.records.domain.ports.in.ManageHistoryTemplateUseCase.CreateTemplateCommand;
import com.jclinical.records.domain.ports.in.ManageHistoryTemplateUseCase.UpdateTemplateCommand;
import com.jclinical.records.infra.adapters.in.web.dto.CreateHistoryTemplateRequest;
import com.jclinical.records.infra.adapters.in.web.dto.HistoryTemplateResponse;
import com.jclinical.records.infra.adapters.in.web.dto.UpdateHistoryTemplateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics/{clinicId}/history-templates")
@RequiredArgsConstructor
public class HistoryTemplateController {

    private final ManageHistoryTemplateUseCase templateUseCase;

    @PostMapping
    public ResponseEntity<HistoryTemplateResponse> createTemplate(
            @PathVariable UUID clinicId,
            @RequestBody CreateHistoryTemplateRequest request) {
        CreateTemplateCommand command = new CreateTemplateCommand(
                request.name(),
                request.description(),
                request.schemaJson()
        );
        MedicalHistoryTemplate template = templateUseCase.createTemplate(clinicId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(template));
    }

    @GetMapping
    public ResponseEntity<List<HistoryTemplateResponse>> getTemplates(@PathVariable UUID clinicId) {
        List<HistoryTemplateResponse> responses = templateUseCase.getTemplatesByClinic(clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<HistoryTemplateResponse> getTemplate(
            @PathVariable UUID clinicId,
            @PathVariable UUID templateId) {
        return templateUseCase.getTemplate(templateId, clinicId)
                .map(template -> ResponseEntity.ok(toResponse(template)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<HistoryTemplateResponse> updateTemplate(
            @PathVariable UUID clinicId,
            @PathVariable UUID templateId,
            @RequestBody UpdateHistoryTemplateRequest request) {
        UpdateTemplateCommand command = new UpdateTemplateCommand(
                request.name(),
                request.description(),
                request.schemaJson(),
                request.active()
        );
        MedicalHistoryTemplate template = templateUseCase.updateTemplate(templateId, clinicId, command);
        return ResponseEntity.ok(toResponse(template));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID clinicId,
            @PathVariable UUID templateId) {
        templateUseCase.deleteTemplate(templateId, clinicId);
        return ResponseEntity.noContent().build();
    }

    private HistoryTemplateResponse toResponse(MedicalHistoryTemplate template) {
        return new HistoryTemplateResponse(
                template.getId(),
                template.getClinicId(),
                template.getName(),
                template.getDescription(),
                template.getSchemaJson(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
