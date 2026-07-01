package com.jclinical.users.infra.config;


import com.jclinical.users.domain.ports.out.EventPublisherPort;
import com.jclinical.users.domain.ports.out.PasswordHasherPort;
import com.jclinical.users.domain.ports.out.UserPreRegistrationRepositoryPort;
import com.jclinical.users.domain.ports.out.UserRepositoryPort;

import com.jclinical.users.domain.service.RegisterUserService;
import com.jclinical.users.domain.service.VerifyUserEmailService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDomainConfig {

    @Bean
    public RegisterUserService registerUserService(
            UserRepositoryPort userRepository,
            UserPreRegistrationRepositoryPort preRegistrationRepository,
            PasswordHasherPort passwordHasher) {
        return new RegisterUserService(userRepository, preRegistrationRepository, passwordHasher);
    }

    @Bean
    public VerifyUserEmailService verifyUserEmailService(
            UserRepositoryPort userRepository,
            UserPreRegistrationRepositoryPort preRegistrationRepository,
            EventPublisherPort eventPublisher) {
        return new VerifyUserEmailService(userRepository, preRegistrationRepository, eventPublisher);
    }
}
