package com.jclinical.records.infra.config;

import com.jclinical.records.domain.ports.out.ClinicalNoteRepositoryPort;
import com.jclinical.records.domain.ports.out.MedicalHistoryRepositoryPort;
import com.jclinical.records.domain.ports.out.MedicalHistoryTemplateRepositoryPort;
import com.jclinical.records.domain.ports.out.PatientValidatorPort;
import com.jclinical.records.domain.service.ClinicalNoteService;
import com.jclinical.records.domain.service.HistoryTemplateService;
import com.jclinical.records.domain.service.MedicalHistoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecordsDomainConfig {

    @Bean
    public HistoryTemplateService historyTemplateService(
            MedicalHistoryTemplateRepositoryPort templateRepository) {
        return new HistoryTemplateService(templateRepository);
    }

    @Bean
    public MedicalHistoryService medicalHistoryService(
            MedicalHistoryRepositoryPort historyRepository,
            MedicalHistoryTemplateRepositoryPort templateRepository,
            PatientValidatorPort patientValidator) {
        return new MedicalHistoryService(historyRepository, templateRepository, patientValidator);
    }

    @Bean
    public ClinicalNoteService clinicalNoteService(
            ClinicalNoteRepositoryPort noteRepository,
            PatientValidatorPort patientValidator) {
        return new ClinicalNoteService(noteRepository, patientValidator);
    }
}
