# Diagramas de Secuencia - Medicloud

Este documento visualiza mediante diagramas de secuencia (sintaxis Mermaid) los flujos lógicos clave implementados en el sistema hasta el momento.

---

## 1. Pre-registro, Verificación de Email y Onboarding de Clínica

Este diagrama describe cómo un usuario realiza su registro inicial en una tabla temporal, verifica su correo a los 15 minutos, y cómo el sistema aprovisiona automáticamente su clínica y perfil médico de forma reactiva a través de eventos.

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Cliente (Postman/Web)
    participant AuthController as AuthController / UserController
    participant RegisterService as RegisterUserService
    participant VerifyService as VerifyUserEmailService
    participant PreRepo as UserPreRegistrationRepository
    participant UserRepo as UserRepository
    participant EventPublisher as EventPublisherPort (SpringEventPublisher)
    participant ClinicListener as UserEventsListener (Clinics)
    participant OnboardService as OnboardClinicService
    participant ClinicRepo as ClinicRepository

    %% Fase 1: Pre-registro
    rect rgb(240, 248, 255)
        note right of Cliente: Fase 1: Pre-registro Temporal
        Cliente->>AuthController: POST /api/v1/users/register (datos + password)
        AuthController->>RegisterService: registerUser(command)
        RegisterService->>PreRepo: existsByEmail()
        RegisterService->>PreRepo: save(PreRegistrationEntity)
        RegisterService-->>AuthController: PreRegistration (Token impreso en consola)
        AuthController-->>Cliente: Response 201 Created (inactivo)
    end

    %% Fase 2: Verificación de Email y Onboarding
    rect rgb(255, 250, 240)
        note right of Cliente: Fase 2: Verificación y Aprovisionamiento
        Cliente->>AuthController: POST /api/v1/users/verify-email (token)
        AuthController->>VerifyService: verifyUserEmail(token)
        VerifyService->>PreRepo: findByToken(token)
        VerifyService->>VerifyService: Validar expiración (15 mins)
        VerifyService->>UserRepo: save(UserEntity activo)
        VerifyService->>PreRepo: delete(PreRegistrationEntity)
        VerifyService->>EventPublisher: publish(UserEmailVerifiedEvent)
        
        %% Evento de Verificación
        par Evento de Verificación
            EventPublisher-->>ClinicListener: Consumir UserEmailVerifiedEvent
            ClinicListener->>OnboardService: onboardClinic(ownerId, clinicName)
            OnboardService->>ClinicRepo: Crear Clinic, ClinicStaff (OWNER) y DoctorProfile (inactivo)
            OnboardService-->>ClinicListener: Onboarding Completo
        end

        VerifyService-->>AuthController: Éxito
        AuthController-->>Cliente: Response 200 OK
    end
```

---

## 2. Creación de Pacientes con Aislamiento Multi-Tenant

Representa cómo se registra un paciente y cómo se restringe la existencia de CURPs duplicados únicamente dentro de la misma clínica/consultorio para mantener el aislamiento multi-tenant.

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Cliente Autenticado (JWT)
    participant PatientController as PatientController
    participant PatientService as PatientService
    participant PatientRepo as PatientRepositoryPort

    Cliente->>PatientController: POST /api/v1/patients (body + clinicId)
    PatientController->>PatientService: registerPatient(command)
    PatientService->>PatientRepo: existsByCurpAndClinicId(curp, clinicId)
    alt CURP ya registrado en la clínica
        PatientService-->>PatientController: Throw IllegalArgumentException
        PatientController-->>Cliente: Response 400 Bad Request
    else CURP libre o nuevo
        PatientService->>PatientRepo: save(Patient)
        PatientRepo-->>PatientService: Patient guardado
        PatientService-->>PatientController: Patient
        PatientController-->>Cliente: Response 201 Created
    end
```

---

## 3. Plantillas e Historias Clínicas Dinámicas (Arrastrar y Soltar)

Describe el flujo en el que la clínica define plantillas personalizadas (cuyo esquema en formato JSON es compatible con el editor visual tipo Canva) y cómo estas plantillas se consumen para guardar y consultar las respuestas del paciente.

```mermaid
sequenceDiagram
    autonumber
    actor Clinica as Clínica / Médico
    participant TemplateController as HistoryTemplateController
    participant HistoryController as MedicalHistoryController
    participant HistoryService as MedicalHistoryService
    participant TemplateRepo as MedicalHistoryTemplateRepositoryPort
    participant HistoryRepo as MedicalHistoryRepositoryPort

    %% Escenario: Definir Plantilla
    rect rgb(240, 255, 255)
        note right of Clinica: Creación de Plantilla (Canva Builder)
        Clinica->>TemplateController: POST /api/v1/clinics/{id}/history-templates (name, schemaJson)
        TemplateController->>TemplateRepo: save(MedicalHistoryTemplate)
        TemplateRepo-->>TemplateController: Plantilla guardada con ID
        TemplateController-->>Clinica: Response 201 Created (ID retornado)
    end

    %% Escenario: Registrar respuestas del paciente
    rect rgb(245, 245, 245)
        note right of Clinica: Registrar Respuestas de Paciente
        Clinica->>HistoryController: PUT /api/v1/patients/{id}/medical-history (templateId, answersJson)
        HistoryController->>HistoryService: saveMedicalHistory(patientId, clinicId, command)
        HistoryService->>TemplateRepo: existsByIdAndClinicId(templateId, clinicId)
        TemplateRepo-->>HistoryService: true
        HistoryService->>HistoryRepo: save(MedicalHistory)
        HistoryRepo-->>HistoryService: Respuestas guardadas (JSON)
        HistoryService-->>HistoryController: MedicalHistory
        HistoryController-->>Clinica: Response 200 OK
    end

    %% Escenario: Obtener respuestas por plantilla
    rect rgb(255, 250, 250)
        note right of Clinica: Consultar Respuestas por Plantilla
        Clinica->>HistoryController: GET /api/v1/patients/{id}/medical-history/by-template/{templateId}
        HistoryController->>HistoryService: getMedicalHistoryByTemplate(patientId, templateId, clinicId)
        HistoryService->>HistoryRepo: findByPatientIdAndTemplateIdAndClinicId(...)
        HistoryRepo-->>HistoryService: MedicalHistory (contiene answersJson)
        HistoryService-->>HistoryController: MedicalHistory
        HistoryController-->>Clinica: Response 200 OK
    end
```

---

## 4. Ciclo de Vida de Notas Clínicas (Formato SOAP)

Describe la creación de una nota clínica en estado Borrador (`DRAFT`), su edición, la firma electrónica (`SIGNED`) que la vuelve inmutable, y el rechazo automático del sistema ante cualquier intento de alteración posterior para dar cumplimiento a normativas sanitarias.

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Médico Autenticado
    participant NoteController as ClinicalNoteController
    participant NoteService as ClinicalNoteService
    participant PatientValidator as PatientValidatorPort
    participant NoteRepo as ClinicalNoteRepositoryPort

    %% Escenario: Crear Nota en Borrador (DRAFT)
    rect rgb(245, 255, 250)
        note right of Cliente: Creación de Nota (DRAFT)
        Cliente->>NoteController: POST /api/v1/patients/{id}/clinical-notes (status: DRAFT)
        NoteController->>NoteService: createClinicalNote(patientId, clinicId, doctorId, cmd)
        NoteService->>PatientValidator: existsByIdAndClinicId(patientId, clinicId)
        PatientValidator-->>NoteService: true
        NoteService->>NoteService: Calcular IMC en VitalSigns
        NoteService->>NoteRepo: save(ClinicalNote)
        NoteRepo-->>NoteService: Nota guardada (DRAFT)
        NoteService-->>NoteController: ClinicalNote
        NoteController-->>Cliente: Response 201 Created
    end

    %% Escenario: Intento de Edición de Borrador
    rect rgb(255, 240, 245)
        note right of Cliente: Edición de Nota (DRAFT)
        Cliente->>NoteController: PUT /api/v1/patients/{id}/clinical-notes/{noteId}
        NoteController->>NoteService: updateClinicalNote(noteId, patientId, clinicId, cmd)
        NoteService->>NoteRepo: findByIdAndPatientIdAndClinicId(...)
        NoteRepo-->>NoteService: ClinicalNote
        NoteService->>NoteService: note.update(...) -> Valida status != SIGNED
        NoteService->>NoteRepo: save(ClinicalNote)
        NoteService-->>NoteController: Nota modificada
        NoteController-->>Cliente: Response 200 OK
    end

    %% Escenario: Firma de la Nota (SIGNED)
    rect rgb(240, 255, 240)
        note right of Cliente: Firma de Nota (SIGNED)
        Cliente->>NoteController: PATCH /api/v1/patients/{id}/clinical-notes/{noteId}/sign
        NoteController->>NoteService: signClinicalNote(noteId, patientId, clinicId)
        NoteService->>NoteRepo: findByIdAndPatientIdAndClinicId(...)
        NoteRepo-->>NoteService: ClinicalNote
        NoteService->>NoteService: note.sign() -> Cambia status a SIGNED (Inmutable)
        NoteService->>NoteRepo: save(ClinicalNote)
        NoteService-->>NoteController: Nota firmada
        NoteController-->>Cliente: Response 200 OK
    end

    %% Escenario: Intento de Modificación de Nota Firmada (Falla)
    rect rgb(255, 228, 225)
        note right of Cliente: Intento de Modificar Nota Firmada (Falla)
        Cliente->>NoteController: PUT /api/v1/patients/{id}/clinical-notes/{noteId}
        NoteController->>NoteService: updateClinicalNote(noteId, patientId, clinicId, cmd)
        NoteService->>NoteRepo: findByIdAndPatientIdAndClinicId(...)
        NoteRepo-->>NoteService: ClinicalNote
        NoteService->>NoteService: note.update(...) -> Lanza IllegalStateException
        NoteService-->>NoteController: Error (IllegalStateException)
        NoteController-->>Cliente: Response 400 Bad Request (vía ExceptionHandler)
    end
```
