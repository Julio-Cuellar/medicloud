package com.jclinical.clinics.infra.adapters.in.events;

import com.jclinical.clinics.domain.ports.in.OnboardClinicUseCase;
import com.jclinical.users.domain.model.events.UserEvents.UserEmailVerifiedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventsListener {

    private final OnboardClinicUseCase onboardClinicUseCase;

    @EventListener
    public void handleUserEmailVerified(UserEmailVerifiedEvent event) {
        log.info(">>>> [CLINICS] Recibido UserEmailVerifiedEvent. Creando y activando clínica de forma atómica para '{}' (owner '{}')", event.clinicName(), event.userId());
        onboardClinicUseCase.createPendingClinic(
                event.userId(),
                event.email(),
                event.fullName(),
                event.clinicName()
        );
        onboardClinicUseCase.activateClinic(event.userId());
    }
}
