package com.jclinical.records.infra.adapters.in.web;

import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase.CreateNoteCommand;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase.UpdateNoteCommand;
import com.jclinical.records.infra.adapters.in.web.dto.ClinicalNoteResponse;
import com.jclinical.records.infra.adapters.in.web.dto.CreateClinicalNoteRequest;
import com.jclinical.records.infra.adapters.in.web.dto.UpdateClinicalNoteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/clinical-notes")
@RequiredArgsConstructor
public class ClinicalNoteController {

    private final ManageClinicalNoteUseCase noteUseCase;

    @PostMapping
    public ResponseEntity<ClinicalNoteResponse> createNote(
            @PathVariable UUID patientId,
            @RequestBody CreateClinicalNoteRequest request) {
        CreateNoteCommand command = new CreateNoteCommand(
                request.subjective(),
                request.objective(),
                request.temperature(),
                request.bloodPressure(),
                request.heartRate(),
                request.respiratoryRate(),
                request.weight(),
                request.height(),
                request.oxygenSaturation(),
                request.assessment(),
                request.plan(),
                request.status()
        );

        ClinicalNote note = noteUseCase.createClinicalNote(patientId, request.clinicId(), request.doctorId(), command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(note));
    }

    @GetMapping
    public ResponseEntity<List<ClinicalNoteResponse>> getNotes(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId) {
        List<ClinicalNoteResponse> responses = noteUseCase.getClinicalNotesByPatient(patientId, clinicId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{noteId}")
    public ResponseEntity<ClinicalNoteResponse> getNote(
            @PathVariable UUID patientId,
            @PathVariable UUID noteId,
            @RequestParam UUID clinicId) {
        return noteUseCase.getClinicalNote(noteId, patientId, clinicId)
                .map(note -> ResponseEntity.ok(toResponse(note)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<ClinicalNoteResponse> updateNote(
            @PathVariable UUID patientId,
            @PathVariable UUID noteId,
            @RequestBody UpdateClinicalNoteRequest request) {
        UpdateNoteCommand command = new UpdateNoteCommand(
                request.subjective(),
                request.objective(),
                request.temperature(),
                request.bloodPressure(),
                request.heartRate(),
                request.respiratoryRate(),
                request.weight(),
                request.height(),
                request.oxygenSaturation(),
                request.assessment(),
                request.plan()
        );

        ClinicalNote note = noteUseCase.updateClinicalNote(noteId, patientId, request.clinicId(), command);
        return ResponseEntity.ok(toResponse(note));
    }

    @PatchMapping("/{noteId}/sign")
    public ResponseEntity<ClinicalNoteResponse> signNote(
            @PathVariable UUID patientId,
            @PathVariable UUID noteId,
            @RequestParam UUID clinicId) {
        ClinicalNote note = noteUseCase.signClinicalNote(noteId, patientId, clinicId);
        return ResponseEntity.ok(toResponse(note));
    }

    private ClinicalNoteResponse toResponse(ClinicalNote note) {
        return new ClinicalNoteResponse(
                note.getId(),
                note.getPatientId(),
                note.getClinicId(),
                note.getDoctorId(),
                note.getSubjective(),
                note.getObjective(),
                note.getVitalSigns(),
                note.getAssessment(),
                note.getPlan(),
                note.getStatus(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
