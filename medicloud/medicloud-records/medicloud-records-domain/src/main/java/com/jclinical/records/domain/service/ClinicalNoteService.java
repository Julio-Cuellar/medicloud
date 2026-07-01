package com.jclinical.records.domain.service;

import com.jclinical.records.domain.model.ClinicalNote;
import com.jclinical.records.domain.model.NoteStatus;
import com.jclinical.records.domain.model.VitalSigns;
import com.jclinical.records.domain.ports.in.ManageClinicalNoteUseCase;
import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ClinicalNoteService implements ManageClinicalNoteUseCase {

    private final ClinicalNoteRepositoryPort noteRepository;
    private final PatientValidatorPort patientValidator;

    public ClinicalNoteService(ClinicalNoteRepositoryPort noteRepository, PatientValidatorPort patientValidator) {
        this.noteRepository = noteRepository;
        this.patientValidator = patientValidator;
    }

    @Override
    public ClinicalNote createClinicalNote(UUID patientId, UUID clinicId, UUID doctorId, CreateNoteCommand command) {
        validatePatient(patientId, clinicId);

        VitalSigns vitalSigns = VitalSigns.create(
                command.temperature(),
                command.bloodPressure(),
                command.heartRate(),
                command.respiratoryRate(),
                command.weight(),
                command.height(),
                command.oxygenSaturation()
        );

        ClinicalNote note = ClinicalNote.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .clinicId(clinicId)
                .doctorId(doctorId)
                .subjective(command.subjective())
                .objective(command.objective())
                .vitalSigns(vitalSigns)
                .assessment(command.assessment())
                .plan(command.plan())
                .status(command.status() != null ? command.status() : NoteStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return noteRepository.save(note);
    }

    @Override
    public ClinicalNote updateClinicalNote(UUID noteId, UUID patientId, UUID clinicId, UpdateNoteCommand command) {
        validatePatient(patientId, clinicId);

        ClinicalNote note = noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La nota clínica no existe para este paciente en esta clínica."));

        VitalSigns vitalSigns = VitalSigns.create(
                command.temperature(),
                command.bloodPressure(),
                command.heartRate(),
                command.respiratoryRate(),
                command.weight(),
                command.height(),
                command.oxygenSaturation()
        );

        note.update(
                command.subjective(),
                command.objective(),
                vitalSigns,
                command.assessment(),
                command.plan()
        );

        return noteRepository.save(note);
    }

    @Override
    public ClinicalNote signClinicalNote(UUID noteId, UUID patientId, UUID clinicId) {
        validatePatient(patientId, clinicId);

        ClinicalNote note = noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("La nota clínica no existe para este paciente en esta clínica."));

        note.sign();

        return noteRepository.save(note);
    }

    @Override
    public Optional<ClinicalNote> getClinicalNote(UUID noteId, UUID patientId, UUID clinicId) {
        validatePatient(patientId, clinicId);
        return noteRepository.findByIdAndPatientIdAndClinicId(noteId, patientId, clinicId);
    }

    @Override
    public List<ClinicalNote> getClinicalNotesByPatient(UUID patientId, UUID clinicId) {
        validatePatient(patientId, clinicId);
        return noteRepository.findByPatientIdAndClinicIdOrderByCreatedAtDesc(patientId, clinicId);
    }

    private void validatePatient(UUID patientId, UUID clinicId) {
        if (!patientValidator.existsByIdAndClinicId(patientId, clinicId)) {
            throw new IllegalArgumentException("El paciente no existe en esta clínica.");
        }
    }
}
