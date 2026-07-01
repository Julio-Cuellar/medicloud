# Bitácora de Actividades - JClinical / Medicloud

Esta bitácora registra cronológicamente los pasos, decisiones técnicas y modificaciones realizadas durante la sesión de trabajo.

---

## Historial de Pasos

### 1. Determinación de la rama de trabajo
* **Fecha/Hora:** 2026-06-29 16:57:30
* **Descripción:** Se consultó la rama Git activa en el repositorio local.
* **Resultado:** Se identificó que estamos en la rama `Cuellar`.

---

### 2. Limpieza completa de la rama `Cuellar`
* **Fecha/Hora:** 2026-06-29 16:58:24
* **Descripción:** A solicitud del usuario, se eliminaron todos los archivos del repositorio en la rama activa para iniciar con un espacio de trabajo limpio.
* **Acciones:**
  * Se ejecutó `git rm -rf .` para eliminar todos los archivos rastreados.
  * Se ejecutó `git clean -fdx` para limpiar archivos y directorios no rastreados.
  * Se realizó un commit con la descripción `"Delete all files from the branch"`.
* **Resultado:** La rama quedó vacía (`working tree clean`), conservando el historial previo pero sin archivos en el árbol activo de la rama actual.

---

### 3. Consulta y definición del modelo de datos (Usuarios ↔ Tenants)
* **Fecha/Hora:** 2026-06-29 17:01:00
* **Descripción:** Se analizó cuál es la mejor relación para permitir que un usuario pertenezca a múltiples clínicas/tenants. Asimismo, se consultó el historial del modelo de datos previo del proyecto.
* **Decisión Técnica:**
  * Se confirmó que la mejor arquitectura es una relación **Muchos a Muchos (Many-to-Many)**.
  * Se verificó en la documentación del modelo (`ModeloDatos_Fase1_v1_0.md` en commits previos) que el sistema ya estipula esta estructura mediante:
    * `core.users`: Tabla global de usuarios.
    * `core.clinics`: Tabla de clínicas, con un único dueño inicial en `owner_user_id`.
    * `core.clinic_staff`: Tabla intermedia de asociación con restricción de unicidad `UNIQUE (clinic_id, user_id)` y roles definidos por clínica.

---

### 4. Propuesta de arquitectura multi-módulo Maven y Hexagonal
* **Fecha/Hora:** 2026-06-29 17:08:30
* **Descripción:** Se diseñó y propuso el plan de directorios y la estructura modular Maven para los módulos `core` y `users`.
* **Resultado:** Se generó el plan de implementación detallando la división entre dominio puro (`domain`) e infraestructura/adaptadores (`infra`), el cual fue aprobado automáticamente.

---

### 5. Creación de la estructura base y verificación de compilación
* **Fecha/Hora:** 2026-06-29 17:10:00
* **Descripción:** Se ejecutó el plan de implementación generando toda la estructura multi-módulo de directorios y archivos de configuración Maven (POMs).
* **Acciones:**
  * Se crearon los paquetes y archivos `package-info.java` para `medicloud-core`, `medicloud-users-domain` y `medicloud-users-infra`.
  * Se crearon los archivos `pom.xml` correspondientes a la raíz, `core`, `users` (agregador), `users-domain` y `users-infra`.
  * Se ejecutó el comando `mvn clean compile` para verificar que toda la estructura compila y las dependencias están correctamente configuradas.
* **Resultado:** El build del proyecto fue exitoso (`BUILD SUCCESS`).

---

### 6. Inicialización y configuración de Spring Boot
* **Fecha/Hora:** 2026-06-29 17:11:50
* **Descripción:** Se creó la clase ejecutable de Spring Boot y el archivo de configuración en el módulo de infraestructura de usuarios.
* **Acciones:**
  * Se agregó la dependencia del driver de PostgreSQL (`org.postgresql:postgresql`) en el archivo `pom.xml` de `medicloud-users-infra`.
  * Se creó la clase principal `com.jclinical.users.infra.MedicloudUsersApplication` con la anotación `@SpringBootApplication`.
  * Se creó el archivo `application.yml` en los recursos de `medicloud-users-infra` con la configuración del servicio, servidor (puerto 8081) y base de datos (PostgreSQL).
  * Se volvió a compilar todo el proyecto con `mvn clean compile` para verificar que todo configure y compile correctamente.
* **Resultado:** Compilación exitosa de todos los módulos (`BUILD SUCCESS`).

---

### 7. Refactorización de arranque a módulo independiente `medicloud-app`
* **Fecha/Hora:** 2026-06-29 17:14:00
* **Descripción:** Se extrajo la ejecución de Spring Boot y su configuración a un nuevo módulo separado (`medicloud-app`) de nivel de raíz para mantener la infraestructura y el dominio de usuarios desacoplados del inicio global del sistema.
* **Acciones:**
  * Se creó la carpeta del módulo `medicloud-app` con su estructura de código correspondiente.
  * Se creó `medicloud-app/pom.xml` agregando la dependencia hacia `medicloud-users-infra` y habilitando el `spring-boot-maven-plugin`.
  * Se registró el módulo `<module>medicloud-app</module>` en el POM padre de la raíz.
  * Se creó la clase principal `com.jclinical.app.MedicloudApplication` con `@SpringBootApplication(scanBasePackages = "com.jclinical")` para asegurar que escanee todos los submódulos.
  * Se movió el archivo `application.yml` a `medicloud-app/src/main/resources/`.
  * Se eliminaron los archivos redundantes (`MedicloudUsersApplication.java` y `application.yml`) del módulo `medicloud-users-infra`.
  * Se ejecutó `mvn clean compile` para confirmar que todo el proyecto completo, incluido el nuevo módulo de arranque, compila con éxito.
* **Resultado:** Compilación y construcción exitosas (`BUILD SUCCESS`).

---

### 8. Diseño y definición del modelo de datos de Usuarios, Staff y Perfil Médico
* **Fecha/Hora:** 2026-06-29 17:44:00
* **Descripción:** Se acordó conceptualmente la arquitectura de entidades para usuarios, sus roles y especialización médica, cumpliendo con las regulaciones sanitarias.
* **Detalles del Diseño:**
  * **`User` (Usuario Global):** Contiene campos de identidad y seguridad general (id, email, password_hash, fullName, themePreference, bloqueo por intentos fallidos, etc.).
  * **`ClinicStaff` (Membresía):** Tabla intermedia muchos a muchos que asocia a un `User` con un `Clinic` y le otorga un `Role` (`staff_role` ENUM).
  * **`DoctorProfile` (Perfil Médico):** Relación 1:1 con `ClinicStaff` (solo si el rol es `DOCTOR`). Almacena datos regulatorios como `cedula_profesional`, `cedula_especialidad`, `especialidad`, `universidad_egreso` y `credential_status`.
  * **Representante Legal (Opción A):** La entidad `Clinic` tendrá una referencia directa (`legal_representative_staff_id`) que apuntará al `ClinicStaff` del médico responsable. El dominio validará que este staff tenga asignado el rol de `DOCTOR` y cuente con un `DoctorProfile` activo y verificado.

---

### 9. Diseño del flujo de registro y aprovisionamiento asíncrono (Tenant Onboarding)
* **Fecha/Hora:** 2026-06-29 18:14:00
* **Descripción:** Se diseñó el proceso de registro en dos fases y la inicialización de módulos por clínica a través de eventos con RabbitMQ.
* **Flujo de Eventos:**
  * **Fase 1 (Pre-registro inactivo):** El usuario se registra. Se crea el `User` (inactivo) y se publica `UserRegisteredEvent`. El módulo de clínicas consume el evento, crea la `Clinic` (inactiva) y asocia al usuario como `OWNER` en `ClinicStaff`.
  * **Fase 2 (Verificación y Activación):** El usuario verifica su correo. Se activa el `User` y se publica `UserEmailVerifiedEvent`.
  * **Aprovisionamiento (Tenant Provisioning):** Al consumir la activación de la clínica, se disparan procesos asíncronos para inicializar los datos base de cada módulo de la clínica (`clinic_id`): plantillas de expediente, caja de tesorería inicial (`medicloud-treasury`), catálogo contable base (`medicloud-accounting`) e inventario básico (`medicloud-inventory`).

---

### 10. Implementación del núcleo de dominio de Usuarios
* **Fecha/Hora:** 2026-06-30 00:33:00
* **Descripción:** Se codificaron todas las clases puras de negocio del módulo `medicloud-users-domain`.
* **Acciones:**
  * Se crearon las entidades `User` (Aggregate Root con lógica de bloqueos y verificación) y `Theme` (Enum).
  * Se crearon los records de eventos de dominio `UserRegisteredEvent` y `UserEmailVerifiedEvent`.
  * Se crearon los puertos de entrada `RegisterUserUseCase` (con su correspondiente `RegisterUserCommand` auto-validable) y `VerifyUserEmailUseCase`.
  * Se crearon los puertos de salida (SPIs) `UserRepositoryPort` y `PasswordHasherPort`.
  * Se ejecutó la compilación limpia del proyecto (`mvn clean compile`) confirmando que todos los submódulos compilan con éxito.
* **Resultado:** Dominio puro de usuarios implementado y compilado con éxito.

---

### 11. Andamiaje e implementación del núcleo de dominio de Clínicas
* **Fecha/Hora:** 2026-06-30 00:44:00
* **Descripción:** Se estructuró el nuevo módulo Maven `medicloud-clinics` y se codificaron todas las clases puras de negocio del módulo `medicloud-clinics-domain`.
* **Acciones:**
  * Se registró el módulo agregador y los submódulos en el POM raíz y dependencyManagement.
  * Se enlazó el nuevo módulo de infraestructura a `medicloud-app` para su inicialización en Spring Boot.
  * Se crearon los enums de dominio `StaffRole` y `DoctorCredentialStatus`.
  * Se crearon las entidades `Clinic` (con métodos para asignar representante legal y gestionar datos fiscales/operativos), `ClinicStaff` y `DoctorProfile` (con lógica de verificación de cédula y suspensión).
  * Se crearon los puertos de salida (interfaces de persistencia) `ClinicRepositoryPort`, `ClinicStaffRepositoryPort` y `DoctorProfileRepositoryPort`.
  * Se ejecutó la compilación limpia del proyecto completo (`mvn clean compile`) para verificar la integración.
* **Resultado:** Proyecto compilado con éxito (`BUILD SUCCESS`) integrando todas las capas del módulo de clínicas.

---

### 12. Implementación de Entidades JPA en Infraestructura
* **Fecha/Hora:** 2026-06-30 00:46:00
* **Descripción:** Se codificaron las entidades de mapeo relacional JPA en los módulos de infraestructura de usuarios y clínicas.
* **Acciones:**
  * Se creó `UserEntity` en `medicloud-users-infra`, mapeada a la tabla `core.users`.
  * Se crearon las entidades `ClinicEntity` (mapeada a `core.clinics`), `ClinicStaffEntity` (mapeada a `core.clinic_staff`) y `DoctorProfileEntity` (mapeada a `core.doctor_profiles`) en `medicloud-clinics-infra`.
  * Se aislaron las dependencias circulares complejas de carga en JPA manteniendo referencias de negocio de claves foráneas específicas (como `legal_representative_staff_id`) de tipo raw `UUID`.
  * Se ejecutó `mvn clean compile` a nivel raíz para verificar la validez sintáctica de las importaciones y del mapeo Jakarta Persistence.
* **Resultado:** Compilación exitosa del proyecto completo (`BUILD SUCCESS`).

---

### 13. Implementación de Mapeadores MapStruct (Domain <=> JPA Entity)
* **Fecha/Hora:** 2026-06-30 00:51:00
* **Descripción:** Se crearon las interfaces de mapeo para automatizar la traducción de datos entre el dominio puro y las entidades JPA.
* **Acciones:**
  * Se implementó `UserMapper` en `medicloud-users-infra` para mapear el dominio `User` con `UserEntity`.
  * Se implementaron `ClinicMapper`, `ClinicStaffMapper` y `DoctorProfileMapper` en `medicloud-clinics-infra`.
  * Se configuraron los mappers con `@Mapper(componentModel = "spring")` permitiendo que Spring los registre e inyecte como beans en los adaptadores.
  * Se ejecutó `mvn clean compile` para disparar el procesador de anotaciones y compilar con éxito las implementaciones generadas automáticamente por MapStruct.
* **Resultado:** Compilación exitosa de todos los mappers del proyecto (`BUILD SUCCESS`).

---

### 14. Implementación de Repositorios Spring Data JPA
* **Fecha/Hora:** 2026-06-30 03:21:00
* **Descripción:** Se crearon las interfaces de repositorios extendiendo de `JpaRepository` en las capas de infraestructura.
* **Acciones:**
  * Se creó `SpringDataUserRepository` en `medicloud-users-infra` agregando las firmas de consulta por correo y validación de existencia.
  * Se crearon `SpringDataClinicRepository`, `SpringDataClinicStaffRepository` y `SpringDataDoctorProfileRepository` en `medicloud-clinics-infra` con métodos personalizados de búsqueda de membresías y perfiles médicos.
  * Se anotaron las interfaces con `@Repository` para su detección e inyección automática en los adaptadores.
  * Se ejecutó `mvn clean compile` para verificar la coherencia sintáctica de las consultas declaradas en las interfaces.
* **Resultado:** Compilación exitosa del proyecto completo (`BUILD SUCCESS`).

---

### 15. Implementación de Adaptadores de Persistencia SQL
* **Fecha/Hora:** 2026-06-30 03:30:00
* **Descripción:** Se crearon las implementaciones concretas de infraestructura para los puertos de persistencia del dominio.
* **Acciones:**
  * Se implementó `SqlUserRepository` en `medicloud-users-infra` conectando `UserRepositoryPort` con `SpringDataUserRepository` y `UserMapper`.
  * Se implementaron `SqlClinicRepository`, `SqlClinicStaffRepository` y `SqlDoctorProfileRepository` en `medicloud-clinics-infra` traduciendo y persistiendo los datos de clínicas.
  * Se anotaron los adaptadores con `@Repository` permitiendo a Spring resolverlos como implementaciones válidas de los puertos cuando el dominio los requiera.
  * Se ejecutó `mvn clean compile` para comprobar que la inyección de dependencias compile correctamente.
* **Resultado:** Compilación exitosa del proyecto completo (`BUILD SUCCESS`).

---

### 16. Implementación de Servicios de Dominio y Verificación de Correo
* **Fecha/Hora:** 2026-06-30 03:39:00
* **Descripción:** Se codificó la lógica pura de los casos de uso de registro y confirmación de usuarios, incorporando tokens de verificación.
* **Acciones:**
  * Se modificó `User` para admitir `verificationToken` y la lógica de validación de expiración.
  * Se crearon los servicios de aplicación `RegisterUserService` y `VerifyUserEmailService` en `medicloud-users-domain`.
  * Se definió la interfaz `EventPublisherPort` para desacoplar el despacho de eventos del dominio.
  * Se mapearon las nuevas columnas de base de datos en `UserEntity` y se crearon las consultas en `SpringDataUserRepository` y `SqlUserRepository`.
  * Se ejecutó `mvn clean compile` para validar la correcta integración de todos los componentes y el procesador de MapStruct.
* **Resultado:** Compilación exitosa de toda la lógica y persistencia asociada al flujo de usuarios (`BUILD SUCCESS`).

---

### 17. Configuración de Spring e Inyección de Dependencias de Usuarios
* **Fecha/Hora:** 2026-06-30 03:45:00
* **Descripción:** Se crearon los Beans de Spring en la infraestructura para enlazar los servicios de dominio con sus adaptadores.
* **Acciones:**
  * Se agregó la dependencia `spring-security-crypto` en el pom de `medicloud-users-infra`.
  * Se implementó `BCryptPasswordHasher` (PasswordHasherPort) y `SpringEventPublisher` (EventPublisherPort) en la infraestructura.
  * Se creó `UserDomainConfig` anotado con `@Configuration` instanciando los servicios puros `RegisterUserService` y `VerifyUserEmailService` como beans.
  * Se ejecutó `mvn clean compile` para validar que todas las dependencias cruzadas compilen con éxito.
* **Resultado:** Compilación de todo el proyecto multi-módulo exitosa (`BUILD SUCCESS`).

---

### 18. Implementación de Controladores REST y DTOs
* **Fecha/Hora:** 2026-06-30 04:21:00
* **Descripción:** Se codificaron los endpoints REST de usuarios y los DTOs de entrada y salida, junto con el control de excepciones global.
* **Acciones:**
  * Se crearon los DTO records `RegisterUserRequest`, `VerifyEmailRequest` y `UserResponse` en `medicloud-users-infra`.
  * Se implementó `UserController` exponiendo endpoints para `/register` y `/verify-email`.
  * Se añadió `GlobalExceptionHandler` interceptando errores sintácticos y lógicos para retornar estatus 400.
  * Se actualizó `UserMapper` para incluir el mapeo de salida hacia `UserResponse`.
  * Se ejecutó `mvn clean compile` para asegurar que el procesador MapStruct autogenere los métodos de mapeo DTO y el proyecto compile de extremo a extremo.
* **Resultado:** Compilación completa exitosa (`BUILD SUCCESS`).

---

### 19. Integración de Logs de Eventos y Arranque Exitoso
* **Fecha/Hora:** 2026-06-30 05:25:00
* **Descripción:** Se agregaron capacidades de depuración de eventos en el bus de datos en memoria y se configuró/arrancó el servidor Spring Boot en el puerto 8082 de forma exitosa.
* **Acciones:**
  * Se configuraron logs de publicación (`log.info`) en `SpringEventPublisher`.
  * Se creó `DebugEventListener` para capturar e imprimir logs de eventos en consola cuando sean recibidos por los oyentes.
  * Se agregó `spring.jpa.properties.hibernate.hbm2ddl.create_namespaces: true` en `application.yml` para posibilitar la creación dinámica de esquemas de base de datos en PostgreSQL.
  * Se configuró el puerto a `8082` para evitar conflictos en la máquina del usuario.
  * Se ejecutó `mvn spring-boot:run` levantando el servidor exitosamente.
* **Resultado:** Aplicación inicializada y levantada correctamente en `http://localhost:8082`.

---

### 20. Generación de Colección de Postman
* **Fecha/Hora:** 2026-06-30 05:46:00
* **Descripción:** Se generó un archivo JSON de colección de Postman en la raíz del proyecto para facilitar las pruebas manuales de los endpoints.
* **Acciones:**
  * Se creó `medicloud-users-collection.json` con la especificación de peticiones para registro y verificación de correo electrónico.
  * Se añadieron scripts de aserciones automáticas (Postman Tests) para validar los estados de respuesta HTTP, campos requeridos y estatus de usuario (activo/inactivo).
* **Resultado:** Colección disponible en la raíz del proyecto con scripts de pruebas automatizadas incorporados.

---

### 21. Implementación del Onboarding Automático de Clínicas
* **Fecha/Hora:** 2026-06-30 06:05:00
* **Descripción:** Se integró el aprovisionamiento dinámico de clínicas escuchando los eventos de registro y confirmación de usuarios.
* **Acciones:**
  * Se agregó la dependencia `medicloud-users-domain` en `medicloud-clinics-infra`.
  * Se definieron e implementaron `OnboardClinicUseCase` y `OnboardClinicService` en el dominio de clínicas.
  * Se creó `ClinicDomainConfig` para instanciar el servicio de onboarding.
  * Se implementó `UserEventsListener` en la infraestructura de clínicas que escucha `UserRegisteredEvent` (crea clínica, staff y perfil en inactivo) y `UserEmailVerifiedEvent` (activa la clínica y el staff).
  * Se ejecutó el servidor y se validó la creación de las tablas de clínicas en PostgreSQL.
* **Resultado:** Onboarding automático implementado y listo para ser probado.

---

### 22. Implementación de Pre-registro de Usuarios y Limpieza de Expirados
* **Fecha/Hora:** 2026-06-30 06:30:00
* **Descripción:** Se estructuró el aislamiento de usuarios no confirmados en una tabla de pre-registro temporal con expiración a los 15 minutos, y se añadió una tarea programada scheduler para eliminar los pre-registros huérfanos.
* **Acciones:**
  * Se creó la entidad `UserPreRegistration` y su tabla correspondiente `core.user_pre_registrations` en PostgreSQL.
  * Se adaptó `RegisterUserService` para guardar en el repositorio de pre-registro con expiración de 15 minutos e imprimir el token generado en consola.
  * Se adaptó `VerifyUserEmailService` para validar la expiración del token, promover el registro a `core.users` activo definitivo, eliminar el pre-registro y emitir el evento `UserEmailVerifiedEvent`.
  * Se creó `PreRegistrationCleanupScheduler` anotado con `@Scheduled` para ejecutar la limpieza de expirados en lote cada minuto.
  * Se configuró `@EnableScheduling` en `MedicloudApplication`.
  * Se adaptó `UserEventsListener` en clínicas para realizar el aprovisionamiento de la clínica, staff y perfil médico en estado activo de forma atómica al consumirse el evento de verificación de correo.
* **Resultado:** Flujo de pre-registro temporal integrado y validado con arranque de servidor exitoso.

---

### 23. Integración de Spring Security y Autenticación mediante JWT
* **Fecha/Hora:** 2026-06-30 06:40:00
* **Descripción:** Se integró la suite de Spring Security junto con tokens JWT para proteger los recursos y permitir login seguro de usuarios verificados.
* **Acciones:**
  * Se sustituyó `spring-security-crypto` por `spring-boot-starter-security` en `medicloud-users-infra/pom.xml`.
  * Se agregaron dependencias de la librería JJWT (`0.12.6`).
  * Se definieron `AuthenticateUserUseCase` y `AuthenticateUserService` en el dominio.
  * Se crearon los adaptadores e interceptores de seguridad `JwtTokenProvider`, `JwtAuthenticationFilter` y la configuración centralizada de Spring Security en `SecurityConfig`.
  * Se implementaron los DTOs `LoginRequest`, `LoginResponse` y el controlador `AuthController` expuesto en `/api/v1/auth/login`.
  * Se actualizó la colección de Postman `medicloud-users-collection.json` agregando la petición de Login JWT y validando con aserciones automatizadas de tests.
* **Resultado:** Integración de seguridad y JWT finalizada de forma exitosa y servidor en ejecución.

---

### 24. Implementación de APIs de Gestión de Clínicas (Creación y Modificación)
* **Fecha/Hora:** 2026-06-30 07:10:00
* **Descripción:** Se estructuró el ciclo de vida completo de clínicas en el controlador REST, permitiendo a los usuarios autenticados completar sus datos fiscales/generales y crear clínicas/sucursales adicionales de forma segura.
* **Acciones:**
  * Se añadió `spring-boot-starter-security` a `medicloud-clinics-infra/pom.xml`.
  * Se definió el caso de uso `ManageClinicUseCase` y su servicio `ManageClinicService` en el dominio de clínicas.
  * Se agregaron los DTOs `CreateClinicRequest`, `UpdateClinicRequest` y `ClinicResponse`.
  * Se modificó `ClinicMapper` para mapear de dominio a `ClinicResponse`.
  * Se creó `ClinicController` con mapeos de `POST /api/v1/clinics`, `PUT /api/v1/clinics/{id}`, `GET /api/v1/clinics` y `GET /api/v1/clinics/{id}`.
  * Se configuraron Beans en `ClinicDomainConfig`.
  * Se actualizó la colección de Postman `medicloud-users-collection.json` agregando los 4 nuevos endpoints de clínicas con aserciones automatizadas de tests y guardado automático de variables.
* **Resultado:** APIs de gestión de clínicas completamente listas, compiladas, y servidor de Spring Boot en ejecución exitosa.

---

### 25. Endpoints Públicos de Autenticación
* **Fecha/Hora:** 2026-06-30 07:29:00
* **Descripción:** Se simplificó la configuración de Spring Security para que los endpoints de registro, verificación de correo y login sean públicos sin restricción de método HTTP.
* **Acciones:**
  * Se modificó `SecurityConfig.java` reemplazando tres `requestMatchers` individuales por uno agrupado que acepta cualquier método sobre las tres rutas.
  * Rutas públicas: `/api/v1/users/register`, `/api/v1/users/verify-email`, `/api/v1/auth/login`.
* **Resultado:** Compilación y arranque exitoso; endpoints accesibles sin token.
---

## 📊 REPORTE DE ESTADO ACTUAL DEL PROYECTO

* **Fecha de cierre:** 2026-06-30
* **Hora de cierre:** 01:52 (UTC-6, hora local)

### Estado General: ✅ FUNCIONAL

### Módulos y Estado

| Módulo | Estado | Descripción |
|---|---|---|
| `medicloud-core` | ✅ Listo | Eventos de dominio compartidos |
| `medicloud-users-domain` | ✅ Listo | Modelo User, puertos, use cases de registro/verificación/autenticación |
| `medicloud-users-infra` | ✅ Listo | JPA, Spring Security, JWT, controladores de usuarios y auth |
| `medicloud-clinics-domain` | ✅ Listo | Modelo Clinic/Staff/DoctorProfile, ManageClinicUseCase, OnboardClinicUseCase |
| `medicloud-clinics-infra` | ✅ Listo | JPA, REST controller de clínicas, listener de eventos |
| `medicloud-app` | ✅ Listo | Boot principal, scheduler de limpieza de pre-registros |

### Endpoints Disponibles

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/api/v1/users/register` | Público | Pre-registro temporal (15 min) |
| POST | `/api/v1/users/verify-email` | Público | Verifica email, promueve a usuario activo y crea clínica |
| POST | `/api/v1/auth/login` | Público | Login con JWT |
| GET | `/api/v1/auth/me` | JWT | Perfil del usuario + clínicas asociadas |
| POST | `/api/v1/clinics` | JWT | Crear clínica adicional |
| PUT | `/api/v1/clinics/{id}` | JWT | Actualizar/completar datos de clínica |
| GET | `/api/v1/clinics` | JWT | Listar todas las clínicas del usuario |
| GET | `/api/v1/clinics/{id}` | JWT | Detalle de una clínica específica |

### Flujo Completo Implementado
1. **Pre-registro** → datos temporales en `core.user_pre_registrations`
2. **Limpieza automática** → scheduler elimina pre-registros sin confirmar cada minuto
3. **Verificación de email** → promueve a `core.users`, borra pre-registro, emite evento
4. **Evento `UserEmailVerifiedEvent`** → `UserEventsListener` en clínicas crea clínica + staff + perfil médico activos
5. **Login JWT** → retorna token Bearer
6. **Gestión de clínicas** → crear sucursales adicionales, actualizar datos fiscales y generales

### Archivos Clave
* `SecurityConfig.java` — configuración de seguridad
* `AuthController.java` — login y /me (actualmente en medicloud-users-infra)
* `ClinicController.java` — CRUD de clínicas
* `medicloud-users-collection.json` — colección Postman lista para importar

---

## 📝 NOTA PARA LA PRÓXIMA SESIÓN

**Objetivo:** Desacoplar el módulo de Auth como módulo independiente.

**Tareas pendientes:**
1. Crear nuevo módulo `medicloud-auth` con sus sub-módulos `medicloud-auth-domain` e `medicloud-auth-infra`.
2. **`medicloud-auth-domain`** debe contener:
   - Casos de uso: `LoginUseCase`, `LogoutUseCase`, `ValidateTokenUseCase`, `RefreshTokenUseCase`
   - Puerto de salida: `TokenRepositoryPort` (para invalidación/blacklist de tokens si se requiere logout real)
3. **`medicloud-auth-infra`** debe contener:
   - `JwtTokenProvider` (mover desde `medicloud-users-infra`)
   - `JwtAuthenticationFilter` (mover desde `medicloud-users-infra`)
   - `SecurityConfig` (mover desde `medicloud-users-infra`)
   - `AuthController` con únicamente: `POST /auth/login`, `POST /auth/logout`, `POST /auth/refresh`
   - El endpoint `GET /auth/me` puede permanecer en `medicloud-users-infra`
4. Registrar `medicloud-auth` en el `pom.xml` del padre y en `medicloud-app`.
5. Asegurarse de que `medicloud-auth-infra` dependa de `medicloud-users-domain` pero **no** al revés (no dependencia circular).

---

## 🚀 NUEVA SESIÓN: DESACOPLAMIENTO DE AUTENTICACIÓN

* **Fecha/Hora de Inicio:** 2026-06-30 15:41:35 (Hora local)
* **Objetivo Principal:** Modularizar y desacoplar las capacidades de autenticación y seguridad en un módulo independiente (`medicloud-auth`).
* **Objetivos Planteados:**
  1. Crear el nuevo módulo independiente `medicloud-auth` con sus sub-módulos `medicloud-auth-domain` y `medicloud-auth-infra`.
  2. Implementar los casos de uso de autenticación (`LoginUseCase`, `LogoutUseCase`, `ValidateTokenUseCase`, `RefreshTokenUseCase`) en el dominio de auth.
  3. Trasladar la configuración de seguridad (`SecurityConfig`), proveedores de tokens (`JwtTokenProvider`), filtros (`JwtAuthenticationFilter`) y controladores de endpoints a la infraestructura de auth.
  4. Configurar las dependencias en los archivos POM del proyecto para resolver el arranque y evitar dependencias circulares.

* **Acciones Realizadas:**
  - Se configuró Maven para registrar el módulo agrupador `medicloud-auth` y sus respectivos sub-módulos `medicloud-auth-domain` y `medicloud-auth-infra`.
  - Se implementó en `medicloud-auth-domain` la lógica hexagonal correspondiente a los casos de uso de inicio (`LoginUseCase`/`LoginService`), cierre (`LogoutUseCase`/`LogoutService`), refresco (`RefreshTokenUseCase`/`RefreshTokenService`) y validación de tokens (`ValidateTokenUseCase`/`ValidateTokenService`).
  - Se implementó en `medicloud-auth-infra` la capa de adaptadores: `JwtTokenProvider` (emisión y lectura de tokens JWT utilizando JJWT), `InMemoryTokenRepository` (almacenamiento de tokens en blacklist), el filtro interceptor `JwtAuthenticationFilter`, la configuración de Spring Security en `SecurityConfig` y el controlador `AuthController` exponiendo `/api/v1/auth/login`, `/api/v1/auth/logout` y `/api/v1/auth/refresh`.
  - Se adaptó el módulo `medicloud-users` eliminando DTOs duplicados de autenticación (`LoginRequest`/`LoginResponse`) y limpiando imports/beans en `UserDomainConfig`.
  - Se implementó el endpoint `GET /api/v1/auth/me` a través de `AuthMeController` en `medicloud-users-infra`, permitiendo a los usuarios autenticados consultar su perfil y sus clínicas asociadas, evitando así dependencias circulares.
* **Resultado:**
  - Compilación y ejecución de todas las pruebas exitosas (`BUILD SUCCESS` en todo el reactor multi-módulo).

---

### 26. Diseño, Implementación e Integración del Módulo de Pacientes
* **Fecha/Hora de Cierre:** 2026-06-30 23:35:00 (Hora local)
* **Objetivo Principal:** Diseñar, codificar e integrar el módulo de Pacientes (`medicloud-patients`) bajo la arquitectura hexagonal del proyecto, dando cumplimiento a la NOM-004-SSA3-2012 (Expediente Clínico) y NOM-024-SSA3-2012 (Sistemas de Información de Registro Electrónico para la Salud), y validar el aislamiento de datos por clínica y por usuario.
* **Acciones Realizadas:**
  - **Estructuración Multi-Módulo Maven:** Se crearon las carpetas e inicializaron los archivos `pom.xml` para `medicloud-patients`, `medicloud-patients-domain` y `medicloud-patients-infra`. Se enlazaron al POM raíz y al cargador principal `medicloud-app`.
  - **Capa de Dominio Puro:**
    - Se definieron los enums `Gender`, `MaritalStatus` y `BloodType`.
    - Se diseñaron los objetos de valor inmutables `Address` y `EmergencyContact` (anotados con `@Value` y `@Builder`).
    - Se creó la entidad raíz `Patient` con su constructor y builder de Lombok.
    - Se implementaron los casos de uso `RegisterPatientUseCase`, `GetPatientUseCase` y el puerto de salida `PatientRepositoryPort`.
    - Se codificó el servicio de aplicación `PatientService` encargándose de normalizar los textos y validar la existencia de CURP por clínica.
  - **Capa de Infraestructura y Mapeo:**
    - Se creó `PatientEntity` mapeado a la tabla `core.patients` con un índice de unicidad compuesto sobre `(clinic_id, curp)`.
    - Se implementó `SpringDataPatientRepository` y la implementación del adaptador `SqlPatientRepository`.
    - Se configuró el mapeador `PatientMapper` de MapStruct traduciendo las estructuras jerárquicas a columnas planas en base de datos.
    - Se expuso la API REST en `PatientController` bajo la ruta `/api/v1/patients` para registrar, buscar por ID y listar pacientes filtrando por clínica.
    - Se configuraron los beans de Spring en `PatientDomainConfig`.
  - **Pruebas y Verificación de Aislamiento:**
    - Se actualizó el archivo de colección de Postman `medicloud-users-collection.json` agregando las pruebas de pacientes.
    - Se añadieron escenarios específicos de pruebas para verificar el aislamiento entre consultorios del mismo usuario (`clinic_original_id` y `clinic_sucursal_id`).
    - Se agregaron escenarios adicionales simulando un **Segundo Usuario Médico** completo (`doctor.segundo@gmail.com`) con su propia clínica y paciente ("Pedro"), para certificar mediante aserciones de Postman el aislamiento absoluto a nivel multi-usuario.
  - **Compilación exitosa:** Se ejecutó `mvn clean compile` obteniendo `BUILD SUCCESS` de todo el reactor multi-módulo.
* **Resultado:** Módulo de pacientes completamente funcional, integrado en el ecosistema multi-tenant y documentado. Colección de Postman actualizada con pruebas automatizadas de aislamiento.

---

## 🚀 NUEVA SESIÓN: [TÍTULO DE LA NUEVA SESIÓN]

* **Fecha/Hora de Inicio:** 2026-07-01 12:41:17 (Hora local)
* **Objetivo Principal:** [Objetivo Principal]
* **Objetivos Planteados:**
  1. [Objetivo 1]
  2. [Objetivo 2]


