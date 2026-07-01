package com.jclinical.patients.infra.config;

import com.jclinical.patients.domain.ports.out.PatientRepositoryPort;
import com.jclinical.patients.domain.service.PatientService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PatientDomainConfig {

    @Bean
    public PatientService patientService(PatientRepositoryPort patientRepository) {
        return new PatientService(patientRepository);
    }
}
