package com.jclinical.clinics.infra.config;

import com.jclinical.clinics.domain.ports.in.OnboardClinicUseCase;
import com.jclinical.clinics.domain.ports.out.ClinicRepositoryPort;
import com.jclinical.clinics.domain.ports.out.ClinicStaffRepositoryPort;
import com.jclinical.clinics.domain.ports.out.DoctorProfileRepositoryPort;
import com.jclinical.clinics.domain.service.OnboardClinicService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClinicDomainConfig {

    @Bean
    public OnboardClinicUseCase onboardClinicUseCase(
            ClinicRepositoryPort clinicRepository,
            ClinicStaffRepositoryPort clinicStaffRepository,
            DoctorProfileRepositoryPort doctorProfileRepository) {
        return new OnboardClinicService(clinicRepository, clinicStaffRepository, doctorProfileRepository);
    }

    @Bean
    public com.jclinical.clinics.domain.ports.in.ManageClinicUseCase manageClinicUseCase(
            ClinicRepositoryPort clinicRepository,
            ClinicStaffRepositoryPort clinicStaffRepository,
            DoctorProfileRepositoryPort doctorProfileRepository) {
        return new com.jclinical.clinics.domain.service.ManageClinicService(clinicRepository, clinicStaffRepository, doctorProfileRepository);
    }
}
