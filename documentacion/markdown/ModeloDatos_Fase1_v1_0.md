# Modelo de Datos — Fase 1 MVP
## Sistema de Gestión de Consultorios Médicos
### Especificación técnica de tablas, columnas, tipos de dato, constraints, índices y relaciones · Versión 2.1 · 2026-06-14 · Confidencial · PostgreSQL 16

---

| Campo | Detalle |
|---|---|
| Versión | 2.1 |
| Fecha | 2026-06-14 |
| Estado | Revisado — alineado con Especificación de Arquitectura v2.1 |
| Motor de base de datos | PostgreSQL 16 |
| Cambios vs v2.0 | Agrega `core.doctor_profiles` con credenciales NOM-004 completas; snapshot de cédula en `document_signatures`; elimina campos de credencial de `clinic_staff`; nuevo ENUM `doctor_credential_status`. |

> **Nota de versión.** Esta versión 2.1 incorpora mejoras en la gestión de credenciales profesionales médicas. Toda columna o tabla nueva está marcada en el texto con la anotación *(v2.1)* para facilitar la revisión del diff respecto a la versión anterior.

---

## Tabla de Contenido

1. [Introducción y Convenciones](#1-introducción-y-convenciones)
2. [Schema: core — Usuarios, Clínicas, Personal y Alertas](#2-schema-core--usuarios-clínicas-personal-y-alertas)
3. [Schema: patients — Catálogo de Pacientes](#3-schema-patients--catálogo-de-pacientes)
4. [Schema: clinical — Expediente, Firmas y Conciliación](#4-schema-clinical--expediente-firmas-y-conciliación)
5. [Schema: scheduling — Agenda y Citas](#5-schema-scheduling--agenda-y-citas)
6. [Schema: treatments — Tratamientos, Procedimientos e Insumos](#6-schema-treatments--tratamientos-procedimientos-e-insumos)
7. [Schema: inventory — Inventario y Kardex](#7-schema-inventory--inventario-y-kardex)
8. [Schema: cash — Caja y Cobros](#8-schema-cash--caja-y-cobros)
9. [Schema: accounting — Motor Contable (ARE) y Libro Mayor](#9-schema-accounting--motor-contable-are-y-libro-mayor)
10. [Schema: audit — Log de Auditoría](#10-schema-audit--log-de-auditoría)
11. [Mapa de Relaciones entre Schemas](#11-mapa-de-relaciones-entre-schemas)
12. [Reglas de Integridad y Negocio](#12-reglas-de-integridad-y-negocio)
13. [Índices Adicionales Recomendados](#13-índices-adicionales-recomendados)
14. [Resumen del Modelo](#14-resumen-del-modelo)

---

## 1. Introducción y Convenciones

Este documento especifica el modelo de datos completo para la Fase 1 MVP del sistema. Todas las tablas utilizan PostgreSQL 16 como motor de base de datos. El esquema se organiza en los siguientes namespaces:

| Schema | Dominio | Tablas principales |
|---|---|---|
| `core` | Usuarios, clínicas, personal, autenticación y alertas | `users`, `organizations`, `clinics`, `clinic_staff`, `doctor_profiles` *(v2.1)*, `attendance`, `system_alerts`, `refresh_tokens` *(v2.0)*, `email_verification_tokens` *(v2.0)*, `password_reset_tokens` *(v2.0)*, `password_history` *(v2.0)*, `staff_invitations` *(v2.0)*, `user_sessions` *(v2.0)*, `sequence_counters` *(v2.0)* |
| `patients` | Pacientes y derechos ARCO | `patients`, `arco_requests` *(v2.0)* |
| `clinical` | Expediente, firmas y conciliación | `medical_records`, `clinical_documents`, `document_signatures`, `clinical_histories`, `doc_templates`, `clinical_photos`, `conciliation_items` |
| `scheduling` | Agenda y citas | `appointments`, `appointment_procedures` |
| `treatments` | Tratamientos e insumos | `treatment_plans`, `treatment_plan_items`, `procedures`, `procedure_supplies` |
| `inventory` | Inventario | `products`, `stock_movements` |
| `cash` | Caja y cobros | `cash_registers`, `cash_sessions`, `payments` |
| `accounting` | Contabilidad (ARE) | `chart_of_accounts`, `journal_entries`, `journal_lines`, `domain_events` |
| `audit` | Auditoría | `audit_log` |

### 1.1 Convenciones de columnas estándar

Todas las tablas incluyen las siguientes columnas de control, salvo que se indique explícitamente:

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | PK — generado con `gen_random_uuid()` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` — fecha de creación en UTC |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` — actualizado automáticamente por trigger |
| `deleted_at` | `TIMESTAMPTZ` | `NULL` = registro activo; valor = soft delete |

### 1.2 Leyenda

| Símbolo | Significado |
|---|---|
| 🔑 | PK — Clave primaria |
| 🔗 | FK — Clave foránea |
| 🔍 | IX — Índice |
| ❗ | NOT NULL obligatorio |
| 🔒 *(v2.0)* | Columna encriptada con AES-256-GCM a nivel de aplicación (ADR-005) |

### 1.3 Tipos ENUM personalizados

| ENUM | Valores |
|---|---|
| `staff_role` | `admin` \| `doctor` \| `receptionist` \| `assistant` \| `accountant` \| `cleaning` \| `clinic_admin` |
| `sex_type` | `male` \| `female` \| `other` |
| `doc_type` | `clinical_history` \| `medical_note` \| `lab_study` \| `imaging` \| `informed_consent` \| `prescription` \| `interconsult` \| `referral` \| `counter_referral` \| `nursing_sheet` \| `budget` \| `clinical_photo` \| `administrative` |
| `appt_status` | `scheduled` \| `confirmed` \| `in_progress` \| `completed` \| `cancelled` \| `no_show` |
| `payment_type` | `full` \| `partial` \| `advance` \| `refund` |
| `pay_method` | `cash` \| `card` \| `transfer` \| `check` \| `other` |
| `stock_mvt_type` | `entry` \| `exit` \| `reserve` \| `unreserve` \| `adjustment_up` \| `adjustment_down` \| `expiry_loss` |
| `acct_type` | `asset` \| `liability` \| `income` \| `expense` \| `cost` |
| `entry_type` | `income` \| `expense` \| `cost` \| `adjustment` \| `reversal` |
| `arco_request_type` *(v2.0)* | `access` \| `rectification` \| `cancellation` \| `opposition` |
| `arco_status` *(v2.0)* | `received` \| `in_progress` \| `completed` \| `rejected` |
| `invitation_status` *(v2.0)* | `pending` \| `accepted` \| `expired` \| `revoked` |
| `sequence_scope` *(v2.0)* | `journal_entry` \| `payment_ticket` \| `patient_record` |
| `doctor_credential_status` *(v2.1)* | `activo` \| `en_tramite` \| `suspendido` |

> **Estados de credenciales médicas.** El estado `activo` indica que la cédula profesional ha sido verificada y se encuentra vigente. El estado `en_tramite` indica que la cédula está en proceso de emisión y permite al médico operar bajo supervisión contando con un documento de respaldo. El estado `suspendido` representa una credencial revocada, suspendida o en medio de un proceso legal abierto, lo cual bloquea la firma de cualquier documento clínico.

> **Sobre los datos monetarios.** El modelo de datos almacena los montos en columnas `NUMERIC` con escala fija (por ejemplo `NUMERIC(12,2)` para importes en pesos y `NUMERIC(14,2)` para líneas de asiento). La API REST, en cambio, expone y recibe esos montos como **enteros en centavos** para evitar pérdidas de precisión de punto flotante en el cliente. La conversión entre ambas representaciones ocurre exclusivamente en la capa de mapeo de la API (MapStruct): al serializar se multiplica por 100 y se redondea al entero más cercano; al deserializar se divide entre 100 hacia un `BigDecimal` con escala 2. Esta frontera de conversión es única y está documentada en la Especificación de API v2.0 §1.1.

---

## 2. Schema: `core` — Usuarios, Clínicas, Personal y Alertas

### `core.users` — Usuarios del sistema (todos los roles)

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `email` | `CITEXT` | NOT NULL, UNIQUE | | 🔍 UQ | Correo electrónico — usado como login |
| `phone` | `VARCHAR(20)` | NULL | | | Teléfono de contacto |
| `full_name` | `VARCHAR(200)` | NOT NULL | | 🔍 | Nombre completo |
| `password_hash` | `TEXT` | NOT NULL | | | Bcrypt hash de la contraseña |
| `avatar_url` | `TEXT` | NULL | | | URL del avatar en object storage |
| `email_verified` | `BOOLEAN` | NOT NULL DEFAULT FALSE | | | Correo verificado |
| `theme_preference` *(v2.0)* | `VARCHAR(10)` | NOT NULL DEFAULT `'light'` | | | Preferencia de tema de la interfaz: `light` \| `dark` |
| `failed_login_attempts` *(v2.0)* | `SMALLINT` | NOT NULL DEFAULT 0 | | | Contador de intentos fallidos consecutivos — se reinicia en login exitoso |
| `locked_until` *(v2.0)* | `TIMESTAMPTZ` | NULL | | 🔍 | Bloqueo temporal por intentos fallidos (15 min tras 5 fallos). NULL = sin bloqueo |
| `last_login_at` | `TIMESTAMPTZ` | NULL | | | Último inicio de sesión |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| `is_active` | `BOOLEAN` | NOT NULL | | 🔍 | `TRUE` = activo, `FALSE` = dado de baja |

> **Política de contraseñas (Arquitectura v2.0 §4.1).** El hash se calcula con `BCryptPasswordEncoder` de Spring Security, *strength* 12. El historial de las últimas 5 contraseñas se conserva en `core.password_history` para impedir su reutilización.

---

### `core.organizations` — Corporaciones / grupos médicos (capa opcional sobre clínicas)

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `name` | `VARCHAR(200)` | NOT NULL | | 🔍 | Razón social o nombre comercial |
| `rfc` | `VARCHAR(13)` | NULL, UNIQUE | | 🔍 UQ | RFC de la corporación (persona moral) |
| `owner_user_id` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario dueño de la corporación |
| `logo_url` | `TEXT` | NULL | | | Logotipo en object storage |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| `is_active` | `BOOLEAN` | NOT NULL | | 🔍 | `TRUE` = activo, `FALSE` = dado de baja |

---

### `core.clinics` — Clínicas / consultorios — unidad operativa base

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `organization_id` | `UUID` | NULL | `organizations.id` | 🔍 | FK corporación — NULL si es clínica independiente |
| `owner_user_id` | `UUID` | NOT NULL | `users.id` | 🔍 | Dueño / admin de la clínica |
| `name` | `VARCHAR(200)` | NOT NULL | | 🔍 | Nombre comercial del consultorio |
| `legal_name` | `VARCHAR(300)` | NULL | | | Razón social para CFDI (Fase 3) |
| `rfc` | `VARCHAR(13)` | NULL | | | RFC (requerido en Fase 3) |
| `tax_regime_code` | `VARCHAR(10)` | NULL | | | Código régimen fiscal SAT (Fase 3) |
| `address_street` | `VARCHAR(300)` | NOT NULL | | | Calle y número |
| `address_city` | `VARCHAR(100)` | NOT NULL | | | Ciudad |
| `address_state` | `VARCHAR(100)` | NOT NULL | | | Estado |
| `address_zip` | `VARCHAR(10)` | NOT NULL | | | Código postal |
| `phone` | `VARCHAR(20)` | NULL | | | Teléfono de la clínica |
| `specialties` | `TEXT[]` | NOT NULL DEFAULT `'{}'` | | | Array de especialidades |
| `logo_url` | `TEXT` | NULL | | | Logotipo |
| `timezone` | `VARCHAR(60)` | NOT NULL DEFAULT `'America/Mexico_City'` | | | Zona horaria operativa |
| `privacy_notice_url` *(v2.0)* | `TEXT` | NULL | | | URL del aviso de privacidad de la clínica (LFPDPPP). Se muestra al paciente al registrar su consentimiento |
| `data_processor_agreed_at` *(v2.0)* | `TIMESTAMPTZ` | NULL | | | Fecha de aceptación del Acuerdo de Encargado de Datos por el administrador de la clínica (LFPDPPP) |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| `is_active` | `BOOLEAN` | NOT NULL | | 🔍 | `TRUE` = activo, `FALSE` = dado de baja |

> **Cumplimiento LFPDPPP (Arquitectura v2.0 §2.7).** La clínica actúa como *Responsable* del tratamiento y JClinical como *Encargado*. Las columnas `privacy_notice_url` y `data_processor_agreed_at` materializan el aviso de privacidad y el Acuerdo de Encargado respectivamente. El consentimiento del titular (paciente) se registra en `patients.patients`.

---

### `core.clinic_staff` — Relación usuarios ↔ clínicas con rol asignado

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica a la que pertenece el empleado |
| `user_id` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario del empleado |
| `role` | `staff_role` | NOT NULL | | 🔍 | ENUM de roles |
| `employee_code` | `VARCHAR(50)` | NULL | | | Código interno del empleado |
| `salary` | `NUMERIC(12,2)` | NULL | | | Salario base (para nómina — Fase 2) |
| `salary_period` | `VARCHAR(20)` | NULL | | | `weekly` \| `biweekly` \| `monthly` |
| `hire_date` | `DATE` | NULL | | | Fecha de contratación |
| `end_date` | `DATE` | NULL | | | Fecha de baja — NULL = activo |
| `notes` | `TEXT` | NULL | | | Notas administrativas |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| UNIQUE | — | `(clinic_id, user_id)` | | 🔍 UQ | Un usuario tiene un solo rol activo por clínica |

> A partir de v2.1, las credenciales profesionales de los médicos se gestionan en `core.doctor_profiles`. Los campos `professional_id` y `specialty` fueron eliminados de esta tabla.

---

### `core.doctor_profiles` *(v2.1)* — Perfiles de credenciales profesionales de los médicos

Es una relación 1:1 con `clinic_staff` restringida a registros donde `role = DOCTOR`. Almacena las credenciales profesionales exigidas por NOM-004-SSA3-2012. Su `clinic_id` existe para habilitar RLS.

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único |
| `clinic_staff_id` | `UUID` | NOT NULL, UNIQUE | `clinic_staff.id` | 🔍 | Staff al que pertenece el perfil |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica (RLS) |
| `cedula_profesional` | `VARCHAR(8)` | NULL | | 🔍 | Número de cédula SSP; NULL solo si `credential_status = en_tramite` |
| `cedula_especialidad` | `VARCHAR(8)` | NULL | | | Cédula de especialidad médica; NULL si médico general |
| `especialidad` | `VARCHAR(100)` | NULL | | | Especialidad médica (ej. Odontología, Medicina General) |
| `sub_especialidad` | `VARCHAR(100)` | NULL | | | Subespecialidad; NULL si no aplica |
| `universidad_egreso` | `VARCHAR(200)` | NOT NULL | | | Institución donde obtuvo el título |
| `anio_egreso` | `SMALLINT` | NOT NULL | | | Año de egreso / titulación |
| `institucion_especialidad` | `VARCHAR(200)` | NULL | | | Institución que otorgó la especialidad; NULL si no aplica |
| `documento_tramite_url` | `TEXT` | NULL 🔒 | | | URL del documento que acredita cédula en trámite, encriptado AES-256-GCM (ADR-005); NOT NULL cuando `credential_status = en_tramite` |
| `credential_status` | `doctor_credential_status` | NOT NULL, DEFAULT `en_tramite` | | 🔍 | Estado de la credencial |
| `verified_at` | `TIMESTAMPTZ` | NULL | | | Cuándo el admin de clínica verificó la credencial |
| `verified_by_user_id` | `UUID` | NULL | `users.id` | | Quién verificó |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | | | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | | | Última modificación (auto-update trigger) |

> **Restricciones de verificación (CHECK).** Se definen reglas de integridad a nivel de base de datos para validar las credenciales. La cédula profesional es de carácter obligatorio cuando el estado de la credencial es activo (`credential_status <> 'activo' OR cedula_profesional IS NOT NULL`). De igual forma, el documento que acredita el trámite de la cédula es obligatorio si el estado de la credencial está registrado en trámite (`credential_status <> 'en_tramite' OR documento_tramite_url IS NOT NULL`).

---

### `core.attendance` — Registro de entradas y salidas del personal

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica |
| `staff_id` | `UUID` | NOT NULL | `clinic_staff.id` | 🔍 | Empleado |
| `check_in_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Hora de entrada |
| `check_out_at` | `TIMESTAMPTZ` | NULL | | | Hora de salida — NULL si aún está activo el turno |
| `notes` | `TEXT` | NULL | | | Observaciones del turno |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

### `core.system_alerts` — Alertas operativas y de inventario

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica origen de la alerta |
| `alert_type` | `VARCHAR(60)` | NOT NULL | | 🔍 | `stock_below_min` \| `stock_above_max` \| `expiry_warning` \| `expiry_expired` \| `cash_difference` |
| `severity` | `VARCHAR(20)` | NOT NULL DEFAULT `'warning'` | | | `info` \| `warning` \| `critical` |
| `entity_type` | `VARCHAR(60)` | NOT NULL | | 🔍 | `product` \| `cash_session` \| `appointment` \| etc. |
| `entity_id` | `UUID` | NOT NULL | | 🔍 | ID de la entidad afectada |
| `title` | `TEXT` | NOT NULL | | | Texto en lenguaje natural de la alerta |
| `metadata` | `JSONB` | NULL | | 🔍 GIN | Datos adicionales de contexto para la alerta |
| `status` | `VARCHAR(20)` | NOT NULL DEFAULT `'pending'` | | 🔍 | `pending` \| `read` \| `resolved` \| `snoozed` |
| `read_by` | `UUID` | NULL | `users.id` | 🔍 | Usuario que marcó como leída |
| `read_at` | `TIMESTAMPTZ` | NULL | | | Fecha de lectura |
| `resolved_by` | `UUID` | NULL | `users.id` | 🔍 | Usuario que resolvió la alerta |
| `resolved_at` | `TIMESTAMPTZ` | NULL | | | Fecha de resolución |
| `snoozed_until` | `TIMESTAMPTZ` | NULL | | | Fecha hasta la que la alerta queda pospuesta |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

### `core.refresh_tokens` *(v2.0)* — Tokens de refresco opacos por usuario y dispositivo

Soporta el esquema OAuth 2.0 descrito en Arquitectura v2.0 §4.1. El `access_token` es un JWT firmado con RS256 y vive solo en memoria del cliente; el `refresh_token` es **opaco** (una referencia aleatoria sin contenido), viaja en una cookie `HttpOnly; SameSite=Strict; Secure` y se persiste aquí para permitir su revocación inmediata. Solo se almacena el hash SHA-256 del token, nunca el valor en claro.

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `user_id` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario dueño del token |
| `token_hash` | `TEXT` | NOT NULL, UNIQUE | | 🔍 UQ | Hash SHA-256 del token opaco — nunca se guarda el valor en claro |
| `device_label` | `VARCHAR(200)` | NULL | | | Etiqueta legible del dispositivo (derivada del user-agent) |
| `ip_address` | `INET` | NULL | | | IP desde la que se emitió el token |
| `user_agent` | `TEXT` | NULL | | | User-agent del cliente al emitir el token |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Vencimiento (30 días desde la emisión) |
| `last_used_at` | `TIMESTAMPTZ` | NULL | | | Última vez que se usó para refrescar (rotación) |
| `revoked_at` | `TIMESTAMPTZ` | NULL | | 🔍 | Fecha de revocación — NULL = activo. Se llena en `logout` y `logout-all` |
| `replaced_by_id` | `UUID` | NULL | `refresh_tokens.id` | 🔍 | Token que sustituyó a este por rotación — habilita detección de reuso |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

> **Rotación y detección de reuso.** Cada vez que se usa un `refresh_token`, se emite uno nuevo y el anterior se marca como revocado con `replaced_by_id` apuntando al sustituto. Si un token ya revocado se vuelve a presentar, se interpreta como robo de token y se revoca toda la cadena del usuario.

---

### `core.email_verification_tokens` *(v2.0)* — Tokens de verificación de correo

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `user_id` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario a verificar |
| `token_hash` | `TEXT` | NOT NULL, UNIQUE | | 🔍 UQ | Hash SHA-256 del token de uso único |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Vencimiento (24 horas desde la emisión) |
| `used_at` | `TIMESTAMPTZ` | NULL | | | Fecha de uso — una vez usado, el token se invalida |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

### `core.password_reset_tokens` *(v2.0)* — Tokens de restablecimiento de contraseña

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `user_id` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario que solicita el restablecimiento |
| `token_hash` | `TEXT` | NOT NULL, UNIQUE | | 🔍 UQ | Hash SHA-256 del token de uso único |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Vencimiento (1 hora desde la emisión) |
| `used_at` | `TIMESTAMPTZ` | NULL | | | Fecha de uso — una vez usado, el token se invalida |
| `requested_ip` | `INET` | NULL | | | IP desde la que se solicitó el restablecimiento |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

### `core.password_history` *(v2.0)* — Historial de hashes de contraseña

Impide la reutilización de las últimas 5 contraseñas conforme a la política de Arquitectura v2.0 §4.1.

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `user_id` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario |
| `password_hash` | `TEXT` | NOT NULL | | | Hash BCrypt de una contraseña anterior |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha en que esta contraseña dejó de estar vigente |

> Al cambiar de contraseña, el sistema compara el nuevo valor contra los 5 registros más recientes de este usuario. Los registros más antiguos pueden purgarse periódicamente.

---

### `core.staff_invitations` *(v2.0)* — Invitaciones por correo para alta de personal

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica que envía la invitación |
| `email` | `CITEXT` | NOT NULL | | 🔍 | Correo del invitado |
| `role` | `staff_role` | NOT NULL | | | Rol con el que se incorporará el invitado |
| `invited_by` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario que generó la invitación |
| `token_hash` | `TEXT` | NOT NULL, UNIQUE | | 🔍 UQ | Hash SHA-256 del token de aceptación (uso único) |
| `status` | `invitation_status` | NOT NULL DEFAULT `'pending'` | | 🔍 | ENUM: `pending` \| `accepted` \| `expired` \| `revoked` |
| `expires_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Vencimiento de la invitación (72 horas) |
| `accepted_at` | `TIMESTAMPTZ` | NULL | | | Fecha de aceptación |
| `accepted_user_id` | `UUID` | NULL | `users.id` | 🔍 | Usuario creado o vinculado al aceptar |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

### `core.user_sessions` *(v2.0)* — Historial de sesiones para auditoría y `logout-all`

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `user_id` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario de la sesión |
| `refresh_token_id` | `UUID` | NULL | `refresh_tokens.id` | 🔍 | Token de refresco asociado a la sesión |
| `ip_address` | `INET` | NULL | | | IP de origen de la sesión |
| `user_agent` | `TEXT` | NULL | | | User-agent del cliente |
| `started_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | | 🔍 | Inicio de la sesión |
| `last_seen_at` | `TIMESTAMPTZ` | NULL | | | Última actividad observada |
| `ended_at` | `TIMESTAMPTZ` | NULL | | 🔍 | Fin de la sesión (logout o expiración) |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

### `core.sequence_counters` *(v2.0)* — Contadores de folios secuenciales sin huecos por clínica

Las secuencias nativas de PostgreSQL (`SEQUENCE`) dejan huecos cuando una transacción hace `ROLLBACK`, porque el avance del contador no es transaccional. Para los folios donde la continuidad importa —en particular los números de póliza y de ticket, que en Fase 3 alimentan la facturación CFDI— se usa esta tabla con un bloqueo por fila (`SELECT ... FOR UPDATE`) que garantiza numeración consecutiva por clínica y ámbito.

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica dueña del contador |
| `scope` | `sequence_scope` | NOT NULL | | 🔍 | Ámbito del contador: `journal_entry` \| `payment_ticket` \| `patient_record` |
| `period_year` | `SMALLINT` | NOT NULL | | 🔍 | Año fiscal del contador — el folio se reinicia cada año |
| `current_value` | `BIGINT` | NOT NULL DEFAULT 0 | | | Último valor asignado |
| `prefix` | `VARCHAR(20)` | NULL | | | Prefijo del folio (ej. `POL`, `TKT`, `EXP`) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| UNIQUE | — | `(clinic_id, scope, period_year)` | | 🔍 UQ | Un contador por clínica, ámbito y año |

> **Garantía de continuidad.** El siguiente folio se obtiene en la misma transacción que crea la entidad: `UPDATE core.sequence_counters SET current_value = current_value + 1 WHERE ... RETURNING current_value`. Si la transacción de negocio hace `ROLLBACK`, el incremento del folio también se revierte, evitando huecos. El folio resultante se compone como `prefix-period_year-current_value` con relleno de ceros (ej. `POL-2025-00041`).

---

## 3. Schema: `patients` — Catálogo de Pacientes

### `patients.patients` — Catálogo de pacientes del consultorio

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica a la que pertenece el paciente |
| `record_number` | `VARCHAR(50)` | NOT NULL | | 🔍 UQ | Número de expediente único por clínica (auto-generado) |
| `first_name` | `VARCHAR(100)` | NOT NULL | | 🔍 | Nombre(s) |
| `last_name_paternal` | `VARCHAR(100)` | NOT NULL | | 🔍 | Apellido paterno |
| `last_name_maternal` | `VARCHAR(100)` | NULL | | 🔍 | Apellido materno |
| `curp` | `TEXT` | NULL 🔒 | | | CURP — **encriptada** con AES-256-GCM a nivel de aplicación (ADR-005). El texto cifrado no es indexable ni buscable directamente; ver `curp_hash` |
| `curp_hash` *(v2.0)* | `TEXT` | NULL | | 🔍 UQ | HMAC-SHA-256 determinístico de la CURP normalizada, usando una clave de servicio. Permite búsqueda exacta y la restricción de unicidad por clínica sin exponer el valor en claro |
| `birth_date` | `DATE` | NOT NULL | | 🔍 | Fecha de nacimiento |
| `sex` | `sex_type` | NOT NULL | | | ENUM: `male` \| `female` \| `other` |
| `blood_type` | `VARCHAR(5)` | NULL | | | Tipo de sangre (A+, O-, etc.) |
| `nationality` | `VARCHAR(80)` | NOT NULL DEFAULT `'Mexicana'` | | | Nacionalidad |
| `occupation` | `VARCHAR(100)` | NULL | | | Ocupación / profesión |
| `religion` | `VARCHAR(80)` | NULL | | | Religión (relevante para consentimientos) |
| `marital_status` | `VARCHAR(30)` | NULL | | | Estado civil |
| `education_level` | `VARCHAR(60)` | NULL | | | Escolaridad |
| `phone_mobile` | `VARCHAR(20)` | NULL | | | Teléfono celular |
| `phone_home` | `VARCHAR(20)` | NULL | | | Teléfono fijo |
| `email` | `CITEXT` | NULL | | | Correo electrónico del paciente |
| `address_street` | `VARCHAR(300)` | NULL | | | Domicilio — calle y número |
| `address_city` | `VARCHAR(100)` | NULL | | | Ciudad |
| `address_state` | `VARCHAR(100)` | NULL | | | Estado |
| `address_zip` | `VARCHAR(10)` | NULL | | | Código postal |
| `emergency_contact_name` | `VARCHAR(200)` | NULL | | | Nombre del contacto de emergencia |
| `emergency_contact_phone` | `VARCHAR(20)` | NULL | | | Teléfono del contacto de emergencia |
| `emergency_contact_rel` | `VARCHAR(60)` | NULL | | | Parentesco con el contacto de emergencia |
| `referring_doctor` | `VARCHAR(200)` | NULL | | | Médico que refirió al paciente |
| `allergies_summary` *(v2.0)* | `TEXT` | NULL | | | Resumen de alergias visible en la ficha rápida del paciente (capturado en el alta; el detalle clínico vive en la historia clínica) |
| `photo_url` | `TEXT` | NULL | | | Fotografía del paciente en object storage |
| `notes` | `TEXT` | NULL | | | Notas generales del paciente |
| `privacy_consent_at` *(v2.0)* | `TIMESTAMPTZ` | NULL | | | Fecha y hora en que el paciente aceptó el aviso de privacidad (LFPDPPP). Obligatorio para crear el expediente |
| `privacy_consent_ip` *(v2.0)* | `INET` | NULL | | | IP desde la que se registró el consentimiento |
| `data_retention_until` *(v2.0)* | `DATE` | NULL | | 🔍 | Fecha hasta la que el expediente debe conservarse (mínimo 5 años desde la última consulta, NOM-004). Se recalcula con cada cita completada |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| `deleted_at` | `TIMESTAMPTZ` | NULL | | 🔍 | Soft delete — NULL = activo |
| UNIQUE | — | `(clinic_id, record_number)` | | 🔍 UQ | Número de expediente único por clínica |
| UNIQUE | — | `(clinic_id, curp_hash)` *(v2.0)* | | 🔍 UQ | CURP única por clínica cuando se captura — se aplica sobre el hash ciego, no sobre el valor cifrado |

> **Consentimiento y retención (LFPDPPP + NOM-004).** El flujo de alta de paciente exige registrar `privacy_consent_at` y `privacy_consent_ip` antes de persistir el expediente. La columna `data_retention_until` se inicializa al crear el paciente y se actualiza automáticamente con cada cita completada, garantizando el periodo mínimo legal de conservación. El ejercicio de derechos ARCO se gestiona en `patients.arco_requests`.

> **Convención de marcado.** El símbolo 🔒 indica que la columna se encripta con AES-256-GCM en la capa de aplicación antes de persistirse (ADR-005). La clave maestra proviene de un KMS externo y nunca reside en código ni en variables de entorno del repositorio.

---

### `patients.arco_requests` *(v2.0)* — Solicitudes de derechos ARCO (Acceso, Rectificación, Cancelación, Oposición)

Materializa la obligación de la LFPDPPP de atender los derechos del titular de los datos. Cada solicitud genera una alerta para la clínica con el plazo legal de respuesta (20 días hábiles).

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica responsable del tratamiento |
| `patient_id` | `UUID` | NOT NULL | `patients.id` | 🔍 | Paciente titular de los datos |
| `request_type` | `arco_request_type` | NOT NULL | | 🔍 | ENUM: `access` \| `rectification` \| `cancellation` \| `opposition` |
| `status` | `arco_status` | NOT NULL DEFAULT `'received'` | | 🔍 | ENUM: `received` \| `in_progress` \| `completed` \| `rejected` |
| `description` | `TEXT` | NULL | | | Detalle de la solicitud del titular |
| `legal_due_date` | `DATE` | NOT NULL | | 🔍 | Fecha límite legal de respuesta (20 días hábiles desde la recepción) |
| `resolution_notes` | `TEXT` | NULL | | | Notas de la resolución |
| `requested_by` | `UUID` | NULL | `users.id` | 🔍 | Usuario que capturó la solicitud (recepción/admin) |
| `resolved_by` | `UUID` | NULL | `users.id` | 🔍 | Usuario que resolvió la solicitud |
| `resolved_at` | `TIMESTAMPTZ` | NULL | | | Fecha de resolución |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |

---

## 4. Schema: `clinical` — Expediente, Firmas y Conciliación

### `clinical.medical_records` — Expediente clínico — contenedor raíz por paciente

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `patient_id` | `UUID` | NOT NULL, UNIQUE | `patients.id` | 🔍 UQ | Un expediente por paciente (1:1) |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica donde se abrió el expediente |
| `opened_at` | `DATE` | NOT NULL | | | Fecha de apertura del expediente |
| `opened_by` | `UUID` | NOT NULL | `users.id` | 🔍 | Médico que creó el expediente |
| `status` | `VARCHAR(20)` | NOT NULL DEFAULT `'active'` | | 🔍 | `active` \| `archived` \| `transferred` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |

---

### `clinical.clinical_documents` — Documentos individuales dentro del expediente

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `medical_record_id` | `UUID` | NOT NULL | `medical_records.id` | 🔍 | Expediente al que pertenece |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica (desnormalizado para RLS) |
| `doc_type` | `doc_type` | NOT NULL | | 🔍 | ENUM de tipo de documento |
| `title` | `VARCHAR(300)` | NOT NULL | | 🔍 | Título del documento — campo **no sensible**, indexable y buscable por texto |
| `content_json` | `BYTEA` | NULL 🔒 | | | Contenido estructurado del canvas (bloques y valores), **encriptado** con AES-256-GCM a nivel de aplicación (ADR-005). Se almacena como `BYTEA` (texto cifrado); el JSON en claro solo existe en memoria durante el request. Por estar cifrado, **no admite índice GIN ni búsqueda por contenido** |
| `content_html` | `BYTEA` | NULL 🔒 | | | Renderizado HTML para vista previa e impresión, **encriptado** con AES-256-GCM (ADR-005) |
| `template_id` | `UUID` | NULL | `doc_templates.id` | 🔍 | Plantilla de origen — NULL si es documento libre |
| `status` | `VARCHAR(20)` | NOT NULL DEFAULT `'draft'` | | 🔍 | `draft` \| `final` \| `signed` \| `cancelled` |
| `created_by` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario que creó el documento |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| `deleted_at` | `TIMESTAMPTZ` | NULL | | 🔍 | Soft delete — NULL = activo |

---

### `clinical.document_signatures` — Firmas digitales de documentos clínicos

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinical_document_id` | `UUID` | NOT NULL | `clinical_documents.id` | 🔍 | Documento firmado |
| `signer_user_id` | `UUID` | NULL | `users.id` | 🔍 | Firmante staff; NULL si firma el paciente |
| `signer_patient_id` | `UUID` | NULL | `patients.id` | 🔍 | Firmante paciente; NULL si firma staff |
| `signer_role` | `VARCHAR(40)` | NOT NULL | | 🔍 | `doctor` \| `patient` \| `witness` |
| `cedula_snapshot` *(v2.1)* | `VARCHAR(8)` | NULL | | | Número de cédula profesional al momento de la firma. Snapshot inmutable exigido por NOM-004-SSA3-2012; NULL si el firmante es paciente o testigo |
| `signature_url` | `TEXT` | NOT NULL 🔒 | | | URL de la imagen de firma en object storage, **encriptada** con AES-256-GCM a nivel de aplicación (ADR-005) |
| `signature_method` | `VARCHAR(30)` | NOT NULL | | | `drawn` \| `typed` \| `biometric` |
| `ip_address` | `INET` | NULL | | | IP del dispositivo desde el que se firmó |
| `signed_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | | | Fecha y hora de la firma |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

> **Trazabilidad histórica de la cédula.** Conforme a la norma NOM-004-SSA3-2012, el número de cédula profesional debe capturarse de forma inmutable como un snapshot en el instante de la firma, en lugar de realizar una consulta dinámica a través de una clave foránea activa. Esto garantiza que cualquier suspensión o cambio posterior de la credencial profesional no altere el registro histórico del firmante al momento de avalar el documento clínico.

---

### `clinical.clinical_histories` — Historia clínica estructurada (NOM-004-SSA3-2012)

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinical_document_id` | `UUID` | NOT NULL, UNIQUE | `clinical_documents.id` | 🔍 UQ | Documento base (1:1) |
| `patient_id` | `UUID` | NOT NULL | `patients.id` | 🔍 | Paciente |
| `chief_complaint` | `TEXT` | NOT NULL | | | Motivo de consulta |
| `family_hx` | `JSONB` | NULL | | 🔍 GIN | Antecedentes heredofamiliares: `{diabetes, hta, cancer, cardiac, other}` |
| `nonpath_personal_hx` | `JSONB` | NULL | | 🔍 GIN | No patológicos: `{diet, exercise, tobacco, alcohol, drugs, housing, occupation}` |
| `path_personal_hx` | `JSONB` | NULL | | 🔍 GIN | Patológicos: `{chronic_diseases, surgeries, hospitalizations, transfusions, allergies, traumatisms}` |
| `gyneco_obs_hx` | `JSONB` | NULL | | | Gineco-obstétricos (cuando aplica): `{menarche, cycles, pregnancies, births, abortions, last_menstrual_period, contraception}` |
| `physical_exam` | `JSONB` | NULL | | | Exploración física: `{weight_kg, height_cm, bmi, bp_systolic, bp_diastolic, heart_rate, temp_c, spo2, general_appearance, systems_review}` |
| `diagnosis_presumptive` | `TEXT` | NULL | | | Diagnóstico presuntivo en texto libre |
| `diagnosis_definitive` | `TEXT` | NULL | | | Diagnóstico definitivo |
| `diagnosis_cie10_codes` | `TEXT[]` | NULL | | 🔍 GIN | Array de códigos CIE-10 |
| `prognosis` | `TEXT` | NULL | | | Pronóstico |
| `treatment_plan` | `TEXT` | NULL | | | Plan de tratamiento en texto libre |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |

---

### `clinical.doc_templates` — Plantillas de documentos clínicos creadas en el canvas

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NULL | `clinics.id` | 🔍 | NULL = plantilla global del sistema; UUID = plantilla de la clínica |
| `doc_type` | `doc_type` | NOT NULL | | 🔍 | Tipo de documento al que aplica la plantilla |
| `name` | `VARCHAR(200)` | NOT NULL | | | Nombre de la plantilla |
| `description` | `TEXT` | NULL | | | Descripción de uso |
| `canvas_json` | `JSONB` | NOT NULL | | | Definición del canvas: bloques, posiciones, campos obligatorios y tipos |
| `is_system` | `BOOLEAN` | NOT NULL DEFAULT FALSE | | 🔍 | `TRUE` = plantilla del sistema (no editable por tenant) |
| `created_by` | `UUID` | NOT NULL | `users.id` | | Usuario que creó la plantilla |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| `is_active` | `BOOLEAN` | NOT NULL | | 🔍 | `TRUE` = activo, `FALSE` = dado de baja |

---

### `clinical.clinical_photos` — Fotografías clínicas asociadas al expediente

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `medical_record_id` | `UUID` | NOT NULL | `medical_records.id` | 🔍 | Expediente |
| `clinical_document_id` | `UUID` | NULL | `clinical_documents.id` | 🔍 | Documento al que está asociada (NULL = foto suelta) |
| `file_url` | `TEXT` | NOT NULL | | | URL en object storage (S3/R2) |
| `file_name` | `VARCHAR(300)` | NOT NULL | | | Nombre original del archivo |
| `mime_type` | `VARCHAR(80)` | NOT NULL | | | `image/jpeg` \| `image/png` \| `application/dicom` \| etc. |
| `file_size_bytes` | `BIGINT` | NOT NULL | | | Tamaño del archivo en bytes |
| `capture_date` | `DATE` | NULL | | 🔍 | Fecha de captura de la imagen |
| `category` | `VARCHAR(80)` | NULL | | 🔍 | `preop` \| `postop` \| `xray` \| `ct` \| `lab` \| `clinical` \| `other` |
| `description` | `TEXT` | NULL | | | Descripción clínica de la imagen |
| `uploaded_by` | `UUID` | NOT NULL | `users.id` | | Usuario que subió el archivo |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `deleted_at` | `TIMESTAMPTZ` | NULL | | 🔍 | Soft delete — NULL = activo |

---

### `clinical.conciliation_items` — Cantidades reales validadas en la ventana de conciliación post-cita

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `appointment_id` | `UUID` | NOT NULL | `appointments.id` | 🔍 | Cita |
| `appointment_procedure_id` | `UUID` | NOT NULL | `appointment_procedures.id` | 🔍 | Procedimiento ejecutado en la cita |
| `product_id` | `UUID` | NOT NULL | `products.id` | 🔍 | Insumo consumido |
| `quantity_budgeted` | `NUMERIC(10,4)` | NOT NULL | | | Snapshot de la cantidad presupuestada al agendar |
| `quantity_actual` | `NUMERIC(10,4)` | NOT NULL | | | Cantidad real validada por el médico o asistente |
| `unit` | `VARCHAR(20)` | NOT NULL | | | Unidad de medida del insumo |
| `unit_cost_snapshot` | `NUMERIC(12,4)` | NOT NULL | | | Costo unitario al momento de la conciliación |
| `total_cost` | `NUMERIC(14,4)` | GENERATED ALWAYS AS (`quantity_actual * unit_cost_snapshot`) STORED | | | Costo total calculado de forma inmutable |
| `notes` | `TEXT` | NULL | | | Observaciones de la conciliación |
| `reconciled_by` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario que validó la conciliación |
| `reconciled_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT `now()` | | | Fecha y hora de la validación |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

## 5. Schema: `scheduling` — Agenda y Citas

### `scheduling.appointments` — Citas agendadas

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica |
| `patient_id` | `UUID` | NOT NULL | `patients.id` | 🔍 | Paciente |
| `doctor_id` | `UUID` | NOT NULL | `users.id` | 🔍 | Médico asignado |
| `treatment_plan_id` | `UUID` | NULL | `treatment_plans.id` | 🔍 | Plan de tratamiento al que pertenece (NULL = consulta nueva) |
| `reason` *(v2.0)* | `VARCHAR(300)` | NULL | | | Motivo de la consulta capturado al agendar (se expone como `reason` en la API) |
| `color_tag` *(v2.0)* | `VARCHAR(9)` | NULL | | | Color de la cita en la agenda visual (formato `#RRGGBB`) |
| `scheduled_start` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de inicio programada |
| `scheduled_end` | `TIMESTAMPTZ` | NOT NULL | | | Fecha y hora de fin programada |
| `actual_start` | `TIMESTAMPTZ` | NULL | | | Hora real de inicio |
| `actual_end` | `TIMESTAMPTZ` | NULL | | | Hora real de fin |
| `status` | `appt_status` | NOT NULL DEFAULT `'scheduled'` | | 🔍 | ENUM de estado |
| `cancellation_reason` | `TEXT` | NULL | | | Motivo de cancelación |
| `cancelled_by` | `UUID` | NULL | `users.id` | | Usuario que canceló |
| `cancelled_at` | `TIMESTAMPTZ` | NULL | | | Timestamp de cancelación |
| `room` | `VARCHAR(60)` | NULL | | | Consultorio o sala asignada |
| `notes` | `TEXT` | NULL | | | Notas para la cita |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |

---

### `scheduling.appointment_procedures` — Procedimientos asignados a una cita específica

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `appointment_id` | `UUID` | NOT NULL | `appointments.id` | 🔍 | Cita |
| `treatment_item_id` | `UUID` | NOT NULL | `treatment_plan_items.id` | 🔍 | Ítem del plan de tratamiento |
| `status` | `VARCHAR(30)` | NOT NULL DEFAULT `'pending'` | | 🔍 | `pending` \| `completed` \| `skipped` |
| `notes` | `TEXT` | NULL | | | Notas del procedimiento en esta cita |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |

---

## 6. Schema: `treatments` — Tratamientos, Procedimientos e Insumos

### `treatments.treatment_plans` — Plan de tratamiento y presupuesto por paciente

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `patient_id` | `UUID` | NOT NULL | `patients.id` | 🔍 | Paciente |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica |
| `created_by` | `UUID` | NOT NULL | `users.id` | 🔍 | Médico que creó el plan |
| `title` | `VARCHAR(300)` | NOT NULL | | | Nombre del plan (ej: 'Rehabilitación oral completa') |
| `clinical_notes` | `TEXT` | NULL | | | Notas clínicas del plan de tratamiento |
| `status` | `VARCHAR(30)` | NOT NULL DEFAULT `'quoted'` | | 🔍 | `quoted` \| `accepted` \| `in_progress` \| `completed` \| `cancelled` |
| `total_quoted` | `NUMERIC(12,2)` | NOT NULL DEFAULT 0 | | | Total cotizado (suma de ítems) |
| `total_paid` | `NUMERIC(12,2)` | NOT NULL DEFAULT 0 | | | Total cobrado hasta el momento |
| `accepted_at` | `TIMESTAMPTZ` | NULL | | | Fecha en que el paciente aceptó el plan |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| `deleted_at` | `TIMESTAMPTZ` | NULL | | 🔍 | Soft delete — NULL = activo |

---

### `treatments.treatment_plan_items` — Ítems (procedimientos) dentro de un plan

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `treatment_plan_id` | `UUID` | NOT NULL | `treatment_plans.id` | 🔍 | Plan de tratamiento |
| `procedure_id` | `UUID` | NOT NULL | `procedures.id` | 🔍 | Procedimiento del catálogo |
| `description` | `TEXT` | NULL | | | Descripción libre del procedimiento en este plan |
| `tooth_number` | `VARCHAR(10)` | NULL | | | Órgano dental (nomenclatura FDI) — aplica para odontología |
| `quantity` | `NUMERIC(8,2)` | NOT NULL DEFAULT 1 | | | Cantidad de veces que se realizará |
| `unit_price` | `NUMERIC(12,2)` | NOT NULL | | | Precio unitario cotizado al paciente |
| `discount_pct` | `NUMERIC(5,2)` | NOT NULL DEFAULT 0 | | | Descuento en porcentaje |
| `subtotal` | `NUMERIC(12,2)` | NOT NULL | | | Subtotal = `(unit_price × qty) × (1 − discount_pct/100)` |
| `status` | `VARCHAR(30)` | NOT NULL DEFAULT `'pending'` | | 🔍 | `pending` \| `reserved` \| `in_progress` \| `completed` \| `cancelled` |
| `sort_order` | `INT` | NOT NULL DEFAULT 0 | | | Orden de visualización dentro del plan |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |

---

### `treatments.procedures` — Catálogo de procedimientos médicos de la clínica

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica dueña del catálogo |
| `code` | `VARCHAR(50)` | NULL | | 🔍 | Código interno o código CIE-P (procedimientos) |
| `name` | `VARCHAR(300)` | NOT NULL | | 🔍 | Nombre del procedimiento |
| `description` | `TEXT` | NULL | | | Descripción detallada |
| `category` | `VARCHAR(100)` | NULL | | 🔍 | Categoría (Extracción, Ortodoncia, Cirugía, etc.) |
| `default_price` | `NUMERIC(12,2)` | NOT NULL DEFAULT 0 | | | Precio sugerido de lista |
| `duration_minutes` | `INT` | NULL | | | Duración estimada en minutos |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| `is_active` | `BOOLEAN` | NOT NULL | | 🔍 | `TRUE` = activo, `FALSE` = dado de baja |

---

### `treatments.procedure_supplies` — Insumos necesarios por procedimiento (lista de materiales)

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `procedure_id` | `UUID` | NOT NULL | `procedures.id` | 🔍 | Procedimiento |
| `product_id` | `UUID` | NOT NULL | `products.id` | 🔍 | Producto / insumo del catálogo de inventario |
| `quantity_required` | `NUMERIC(10,4)` | NOT NULL | | | Cantidad requerida por ejecución del procedimiento (soporta decimales para ml/g) |
| `unit` | `VARCHAR(20)` | NOT NULL | | | Unidad de medida: `pz` \| `ml` \| `g` \| `cm` \| etc. |
| `notes` | `TEXT` | NULL | | | Notas de uso del insumo |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |

---

## 7. Schema: `inventory` — Inventario y Kardex

### `inventory.products` — Catálogo de productos e insumos

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica dueña del catálogo |
| `sku` | `VARCHAR(100)` | NULL | | 🔍 | SKU interno o código de barras |
| `name` | `VARCHAR(300)` | NOT NULL | | 🔍 | Nombre del producto |
| `description` | `TEXT` | NULL | | | Descripción |
| `category` | `VARCHAR(100)` | NULL | | 🔍 | `material_dental` \| `medicamento` \| `papelería` \| `equipo` \| `otro` |
| `unit` | `VARCHAR(20)` | NOT NULL | | | Unidad base: `pz` \| `caja` \| `frasco` \| `ml` \| `g` |
| `unit_cost` | `NUMERIC(12,4)` | NOT NULL DEFAULT 0 | | | Costo unitario actual (se actualiza con cada entrada) |
| `stock_current` | `NUMERIC(12,4)` | NOT NULL DEFAULT 0 | | | Stock disponible actual |
| `stock_reserved` | `NUMERIC(12,4)` | NOT NULL DEFAULT 0 | | | Stock apartado por citas confirmadas |
| `version` *(v2.0)* | `BIGINT` | NOT NULL DEFAULT 0 | | | Versión para bloqueo optimista (`@Version` de JPA). Previene condiciones de carrera al reservar o liberar stock desde citas concurrentes. Ver §12.5 |
| `stock_min` | `NUMERIC(12,4)` | NOT NULL DEFAULT 0 | | | Stock mínimo — dispara alerta de reposición |
| `stock_max` | `NUMERIC(12,4)` | NULL | | | Stock máximo — dispara alerta de exceso |
| `has_expiry` | `BOOLEAN` | NOT NULL DEFAULT FALSE | | | `TRUE` si el producto tiene fecha de caducidad |
| `expiry_alert_days` | `INT` | NULL | | | Días antes de caducidad para disparar alerta |
| `is_controlled` | `BOOLEAN` | NOT NULL DEFAULT FALSE | | | `TRUE` si es medicamento controlado (requiere receta especial) |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| `is_active` | `BOOLEAN` | NOT NULL | | 🔍 | `TRUE` = activo, `FALSE` = dado de baja |

---

### `inventory.stock_movements` — Kardex — registro de cada movimiento de inventario

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica |
| `product_id` | `UUID` | NOT NULL | `products.id` | 🔍 | Producto afectado |
| `movement_type` | `stock_mvt_type` | NOT NULL | | 🔍 | ENUM de tipo de movimiento |
| `quantity` | `NUMERIC(12,4)` | NOT NULL | | | Cantidad del movimiento (siempre positivo; el tipo indica dirección) |
| `unit_cost` | `NUMERIC(12,4)` | NULL | | | Costo unitario en el momento del movimiento |
| `total_cost` | `NUMERIC(14,4)` | NULL | | | `quantity × unit_cost` |
| `stock_after` | `NUMERIC(12,4)` | NOT NULL | | | Stock disponible después del movimiento (snapshot) |
| `reference_type` | `VARCHAR(60)` | NULL | | 🔍 | `appointment` \| `treatment_item` \| `cash_register` \| `manual_adjustment` |
| `reference_id` | `UUID` | NULL | | 🔍 | ID de la entidad de origen del movimiento |
| `lot_number` | `VARCHAR(100)` | NULL | | 🔍 | Número de lote del proveedor (Fase 2: trazabilidad completa) |
| `expiry_date` | `DATE` | NULL | | 🔍 | Fecha de caducidad del lote |
| `notes` | `TEXT` | NULL | | | Notas del movimiento |
| `created_by` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario que registró el movimiento |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

## 8. Schema: `cash` — Caja y Cobros

### `cash.cash_registers` — Cajas registradoras / puntos de venta

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica |
| `name` | `VARCHAR(100)` | NOT NULL | | | Nombre de la caja (ej: 'Caja Principal') |
| `is_active` | `BOOLEAN` | NOT NULL DEFAULT TRUE | | | `TRUE` = habilitada |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |

---

### `cash.cash_sessions` — Sesiones de caja (apertura y cierre de turno)

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `cash_register_id` | `UUID` | NOT NULL | `cash_registers.id` | 🔍 | Caja |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica |
| `opened_by` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario que abrió la caja |
| `closed_by` | `UUID` | NULL | `users.id` | | Usuario que cerró la caja |
| `opened_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Hora de apertura |
| `closed_at` | `TIMESTAMPTZ` | NULL | | 🔍 | Hora de cierre — NULL = sesión activa |
| `opening_amount` | `NUMERIC(12,2)` | NOT NULL DEFAULT 0 | | | Fondo inicial en efectivo |
| `closing_amount_expected` | `NUMERIC(12,2)` | NULL | | | Total esperado al cierre (calculado por sistema) |
| `closing_amount_actual` | `NUMERIC(12,2)` | NULL | | | Efectivo contado físicamente al cierre |
| `difference` | `NUMERIC(12,2)` | NULL | | | `closing_actual − closing_expected` (positivo = sobrante) |
| `notes` | `TEXT` | NULL | | | Notas del cierre de caja |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |

---

### `cash.payments` — Cobros realizados a pacientes y ticket de venta

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica |
| `cash_session_id` | `UUID` | NOT NULL | `cash_sessions.id` | 🔍 | Sesión de caja activa al momento del cobro |
| `patient_id` | `UUID` | NOT NULL | `patients.id` | 🔍 | Paciente |
| `appointment_id` | `UUID` | NULL | `appointments.id` | 🔍 | Cita asociada — NULL si es pago libre (abono a tratamiento) |
| `treatment_plan_id` | `UUID` | NULL | `treatment_plans.id` | 🔍 | Plan de tratamiento al que se abona |
| `payment_type` | `payment_type` | NOT NULL | | 🔍 | ENUM: `full` \| `partial` \| `advance` \| `refund` |
| `amount` | `NUMERIC(12,2)` | NOT NULL | | | Monto del cobro |
| `payment_method` | `pay_method` | NOT NULL | | 🔍 | ENUM: `cash` \| `card` \| `transfer` \| `check` \| `other` |
| `reference_number` | `VARCHAR(100)` | NULL | | | Número de autorización / referencia bancaria |
| `ticket_number` | `VARCHAR(50)` | NOT NULL | | 🔍 UQ | Número de ticket único por clínica |
| `ticket_lines` | `JSONB` | NOT NULL DEFAULT `'[]'` | | 🔍 GIN | Detalle del ticket: líneas, subtotales, total y desglose de pago |
| `notes` | `TEXT` | NULL | | | Notas del cobro |
| `received_by` | `UUID` | NOT NULL | `users.id` | 🔍 | Usuario que recibió el pago |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

## 9. Schema: `accounting` — Motor Contable (ARE) y Libro Mayor

### `accounting.chart_of_accounts` — Catálogo de cuentas contables por clínica

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica — cada tenant tiene su CoA |
| `code` | `VARCHAR(20)` | NOT NULL | | 🔍 | Código de cuenta (ej: `11100`) |
| `name` | `VARCHAR(200)` | NOT NULL | | | Nombre de la cuenta |
| `account_type` | `acct_type` | NOT NULL | | 🔍 | ENUM: `asset` \| `liability` \| `income` \| `expense` \| `cost` |
| `parent_code` | `VARCHAR(20)` | NULL | | 🔍 | Código de la cuenta padre — NULL = cuenta raíz |
| `is_system` | `BOOLEAN` | NOT NULL DEFAULT FALSE | | | `TRUE` = cuenta base del sistema (no modificable) |
| `allows_movements` | `BOOLEAN` | NOT NULL DEFAULT TRUE | | | `FALSE` = cuenta de agrupación, no recibe movimientos directos |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | | | Última modificación (auto-update trigger) |
| `is_active` | `BOOLEAN` | NOT NULL | | 🔍 | `TRUE` = activo, `FALSE` = dado de baja |
| UNIQUE | — | `(clinic_id, code)` | | 🔍 UQ | Código único por clínica |

---

### `accounting.journal_entries` — Libro Mayor — cabecera de cada póliza contable

> ⚠️ Esta tabla es **append-only**. No tiene `updated_at` ni `deleted_at`. Un trigger impide `UPDATE` y `DELETE`.

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica |
| `entry_number` | `VARCHAR(20)` | NOT NULL | | 🔍 UQ | Número de póliza único por clínica (secuencial + año) |
| `entry_type` | `entry_type` | NOT NULL | | 🔍 | ENUM: `income` \| `expense` \| `cost` \| `adjustment` \| `reversal` |
| `entry_date` | `DATE` | NOT NULL | | 🔍 | Fecha contable de la póliza |
| `description` | `TEXT` | NOT NULL | | | Descripción en lenguaje natural |
| `domain_event_type` | `VARCHAR(100)` | NOT NULL | | 🔍 | Evento de dominio origen. `JournalBalanceError` nunca produce una `journal_entry`; el intento fallido queda solo en `domain_events` con `processed_at NULL`. |
| `domain_event_id` | `UUID` | NOT NULL | | 🔍 | ID del evento de dominio que originó esta póliza |
| `reversed_by_id` | `UUID` | NULL | `journal_entries.id` | 🔍 | Póliza de reverso — NULL si no ha sido revertida |
| `reversal_of_id` | `UUID` | NULL | `journal_entries.id` | 🔍 | Póliza original que esta revierte — NULL si no es reverso |
| `is_balanced` | `BOOLEAN` | NOT NULL | | 🔍 | `TRUE` si Σ débitos = Σ créditos; validación final corre en trigger DEFERRABLE |
| `checksum` | `TEXT` | NOT NULL | | | SHA-256 del contenido de las líneas — para auditoría de integridad |
| `created_by_are` | `BOOLEAN` | NOT NULL DEFAULT TRUE | | | `TRUE` = generada automáticamente por el ARE; `FALSE` = entrada manual del contador |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

### `accounting.journal_lines` — Líneas de asiento (débito / crédito) de cada póliza

> ⚠️ Esta tabla es **append-only**. No tiene `updated_at` ni `deleted_at`.

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `journal_entry_id` | `UUID` | NOT NULL | `journal_entries.id` | 🔍 | Póliza a la que pertenece la línea |
| `account_code` | `VARCHAR(20)` | NOT NULL | `chart_of_accounts.code` | 🔍 | Código de cuenta del CoA |
| `debit` | `NUMERIC(14,2)` | NOT NULL DEFAULT 0 | | | Monto débito — 0 si es crédito |
| `credit` | `NUMERIC(14,2)` | NOT NULL DEFAULT 0 | | | Monto crédito — 0 si es débito |
| `description` | `TEXT` | NULL | | | Descripción de la línea |
| `reference_type` | `VARCHAR(60)` | NULL | | | `payment` \| `stock_movement` \| `payroll_item` |
| `reference_id` | `UUID` | NULL | | 🔍 | ID de la entidad de referencia |
| `sort_order` | `INT` | NOT NULL DEFAULT 0 | | | Orden de las líneas dentro de la póliza |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

### `accounting.domain_events` — Log inmutable de todos los eventos de dominio publicados

Esta tabla cumple una doble función: es el **log de auditoría** inmutable de todos los eventos de dominio y, a la vez, la **bandeja de salida transaccional (*outbox*)** del ARE. El módulo operativo inserta el evento aquí dentro de la misma transacción que su operación de negocio; un proceso *relay* lee los registros con `processed_at IS NULL` en orden de `sequence_no` y los publica en RabbitMQ. Esto elimina la ventana de inconsistencia entre el `COMMIT` de la base de datos y la publicación del mensaje (ver Arquitectura v2.0 §3.1 y ADR-001).

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `sequence_no` *(v2.0)* | `BIGINT` | NOT NULL, GENERATED ALWAYS AS IDENTITY | | 🔍 UQ | Secuencia monotónica global. Define el **orden total de publicación** que el relay respeta al leer la outbox |
| `clinic_id` | `UUID` | NOT NULL | `clinics.id` | 🔍 | Clínica origen del evento |
| `event_type` | `VARCHAR(100)` | NOT NULL | | 🔍 | `CitaConfirmada` \| `PagoRegistrado` \| `ConsumoConciliado` \| `CitaCancelada` \| `InsumoAgregado` \| `MermaCaducidad` \| `JournalBalanceError` |
| `aggregate_type` | `VARCHAR(80)` | NOT NULL | | 🔍 | Tipo de entidad origen: `Appointment` \| `Payment` \| `StockMovement` \| `JournalEntry` |
| `aggregate_id` | `UUID` | NOT NULL | | 🔍 | ID de la entidad que generó el evento |
| `partition_key` *(v2.0)* | `UUID` | NOT NULL | | 🔍 | Clave de partición para garantizar el **orden por agregado de negocio** en RabbitMQ. Para los eventos relacionados con una cita y su cobro (`PagoRegistrado`, `CitaConfirmada`, `CitaCancelada`, `ConsumoConciliado`) toma el valor del `appointment_id`; para movimientos de inventario sin cita, el `product_id`. Garantiza que eventos del mismo agregado se procesen en orden. Ver §12.7 |
| `payload` | `JSONB` | NOT NULL | | 🔍 GIN | Snapshot completo del estado al momento del evento |
| `processed_at` | `TIMESTAMPTZ` | NULL | | 🔍 | Cuando el ARE procesó el evento — NULL = pendiente |
| `published_at` *(v2.0)* | `TIMESTAMPTZ` | NULL | | 🔍 | Cuando el relay publicó el evento en RabbitMQ — NULL = aún en la outbox |
| `retry_count` *(v2.0)* | `SMALLINT` | NOT NULL DEFAULT 0 | | | Número de reintentos de procesamiento del ARE antes de ir a la Dead Letter Queue |
| `processing_error` | `TEXT` | NULL | | | Error del ARE si el procesamiento falló |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

> **Patrón Outbox + clave de partición (v2.0).** La combinación de `sequence_no` (orden total de publicación) y `partition_key` (orden por agregado en el consumo) cierra los dos riesgos de ordenamiento identificados en la revisión de arquitectura: mensajes huérfanos por caída entre `COMMIT` y *publish*, y reversos contables procesados antes que su cargo original. La idempotencia del ARE se mantiene verificando que no exista ya una `journal_entry` con el mismo `domain_event_id`.

---

## 10. Schema: `audit` — Log de Auditoría

### `audit.audit_log` — Log de auditoría de acciones del sistema

> ⚠️ Esta tabla es **completamente inmutable** — append-only por diseño.

| Columna | Tipo | Constraints | FK | Índice | Descripción |
|---|---|---|---|---|---|
| `id` | `UUID` | PK, NOT NULL | | 🔑 | Identificador único universal |
| `clinic_id` | `UUID` | NULL | `clinics.id` | 🔍 | Clínica afectada — NULL si es acción global |
| `user_id` | `UUID` | NULL | `users.id` | 🔍 | Usuario que realizó la acción — NULL si es sistema |
| `action` | `VARCHAR(100)` | NOT NULL | | 🔍 | `CREATE` \| `UPDATE` \| `DELETE` \| `LOGIN` \| `LOGOUT` \| `EXPORT` \| `SIGN_DOCUMENT` \| etc. |
| `entity_type` | `VARCHAR(80)` | NOT NULL | | 🔍 | Entidad afectada: `Patient` \| `Appointment` \| `Payment` \| `JournalEntry` \| etc. |
| `entity_id` | `UUID` | NULL | | 🔍 | ID de la entidad afectada |
| `old_values` | `JSONB` | NULL | | | Estado anterior (para UPDATE y DELETE) |
| `new_values` | `JSONB` | NULL | | | Estado nuevo (para CREATE y UPDATE) |
| `ip_address` | `INET` | NULL | | | IP de origen de la acción |
| `user_agent` | `TEXT` | NULL | | | User-agent del cliente |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | | 🔍 | Fecha y hora de creación (UTC) |

---

## 11. Mapa de Relaciones entre Schemas

Las relaciones entre schemas siguen el principio de **dependencia descendente**: ningún schema operativo importa desde `accounting` ni `audit`. El flujo de datos siempre va hacia abajo: operación clínica → eventos de dominio → ARE → Libro Mayor.

| Tabla origen | Cardinalidad | Tabla destino | Descripción |
|---|---|---|---|
| `users` | 1 : N | `clinic_staff` | Un usuario puede ser staff en varias clínicas |
| `users` | 1 : N | `refresh_tokens` *(v2.0)* | Un usuario mantiene varios tokens de sesión activos (multi-dispositivo) |
| `users` | 1 : N | `user_sessions` *(v2.0)* | Historial de sesiones por usuario para auditoría y `logout-all` |
| `clinics` | 1 : N | `staff_invitations` *(v2.0)* | Una clínica emite múltiples invitaciones de personal |
| `clinics` | 1 : N | `sequence_counters` *(v2.0)* | Una clínica mantiene un contador por ámbito y año |
| `patients` | 1 : N | `arco_requests` *(v2.0)* | Un paciente puede ejercer múltiples derechos ARCO |
| `clinics` | 1 : N | `clinic_staff` | Una clínica tiene múltiples empleados |
| `clinic_staff` | 1 : 0..1 | `doctor_profiles` *(v2.1)* | Un staff con `role = DOCTOR` tiene exactamente un perfil de credenciales |
| `clinics` | 1 : N | `patients` | Una clínica gestiona muchos pacientes |
| `patients` | 1 : 1 | `medical_records` | Un expediente por paciente |
| `medical_records` | 1 : N | `clinical_documents` | Un expediente contiene múltiples documentos |
| `clinical_documents` | 1 : 1 | `clinical_histories` | Una historia clínica por documento |
| `clinical_documents` | 1 : N | `document_signatures` | Un documento puede tener múltiples firmas |
| `patients` | 1 : N | `appointments` | Un paciente puede tener múltiples citas |
| `patients` | 1 : N | `treatment_plans` | Un paciente puede tener múltiples planes |
| `treatment_plans` | 1 : N | `treatment_plan_items` | Un plan tiene múltiples procedimientos |
| `appointments` | 1 : N | `appt_procedures` | Una cita ejecuta varios ítems del plan |
| `appointments` | 1 : N | `conciliation_items` | Una cita puede registrar múltiples conciliaciones de insumos |
| `appointment_procedures` | 1 : N | `conciliation_items` | Cada procedimiento ejecutado puede detallar sus insumos reales |
| `procedures` | 1 : N | `procedure_supplies` | Un procedimiento requiere N insumos |
| `products` | 1 : N | `stock_movements` | Un producto tiene historial completo de movimientos |
| `products` | 1 : N | `conciliation_items` | Un producto puede aparecer en múltiples conciliaciones |
| `cash_sessions` | 1 : N | `payments` | Una sesión de caja registra múltiples cobros |
| `clinics` | 1 : N | `system_alerts` | Cada clínica puede generar y resolver múltiples alertas |
| `payments` → ARE | 1 : 1 | `journal_entries` | Cada pago genera una póliza de ingreso |
| `stock_movements` → ARE | 1 : 1 | `journal_entries` | Cada consumo/merma genera póliza de costo |
| `journal_entries` | 1 : N | `journal_lines` | Una póliza tiene 2 o más líneas de asiento |

---

## 12. Reglas de Integridad y Negocio

### 12.1 Multi-tenancy y Row Level Security (RLS)

Todas las tablas operativas incluyen `clinic_id`. Se implementa RLS en PostgreSQL: cada sesión de base de datos inyecta el `clinic_id` del tenant activo en el contexto, y las políticas de RLS filtran automáticamente todas las queries. Un usuario no puede acceder a datos de otra clínica aunque conozca el ID.

**Endurecimiento de RLS (v2.0).** Para que la garantía sea real bajo el modelo de despliegue (Spring Boot + HikariCP con *pool* de conexiones), se aplican tres reglas obligatorias:

1. **Inyección por transacción, no por sesión.** El `clinic_id` se establece con `SET LOCAL app.clinic_id = '<uuid>'` al inicio de cada transacción, no con `SET` de sesión. Como las conexiones del *pool* se reutilizan entre peticiones, un `SET` de sesión filtraría el contexto de un tenant a la petición siguiente. `SET LOCAL` queda acotado a la transacción y se descarta en el `COMMIT`/`ROLLBACK`.
2. **Rol de aplicación sin privilegios de *bypass*.** El backend se conecta con un rol dedicado que **no** es `SUPERUSER`, **no** es dueño de las tablas y **no** tiene el atributo `BYPASSRLS`. Cualquiera de esas condiciones anularía silenciosamente las políticas RLS.
3. **`FORCE ROW LEVEL SECURITY`.** Cada tabla con `clinic_id` se declara con `ALTER TABLE ... FORCE ROW LEVEL SECURITY` para que las políticas apliquen incluso al dueño de la tabla, cerrando el último resquicio de fuga entre tenants.

Las políticas de RLS se definen como `USING (clinic_id = current_setting('app.clinic_id')::uuid)` tanto para `SELECT` como para `INSERT`/`UPDATE`/`DELETE`.

### 12.2 Soft delete

Las entidades principales (`patients`, `clinical_documents`, `treatment_plans`, `products`) utilizan soft delete mediante `deleted_at`. Los registros con `deleted_at IS NOT NULL` no aparecen en las vistas normales pero se conservan para auditoría y trazabilidad contable. Las pólizas contables (`journal_entries`, `journal_lines`) y el `audit_log` son completamente inmutables: no tienen `deleted_at` ni `updated_at`.

### 12.3 Inmutabilidad del Libro Mayor

Las tablas `journal_entries` y `journal_lines` NO tienen columnas `updated_at` ni `deleted_at`. Son **append-only** por diseño. Cualquier corrección genera una nueva póliza de reverso referenciada a la original. Se implementa una restricción a nivel de trigger que impide `UPDATE` y `DELETE` sobre estas tablas.

### 12.4 Validación de partida doble

Antes de insertar cualquier conjunto de `journal_lines`, un trigger `CONSTRAINT DEFERRABLE INITIALLY DEFERRED` valida en PostgreSQL que `SUM(debit) = SUM(credit)` al momento del `COMMIT`. Si la condición falla, la transacción completa hace `ROLLBACK` y el ARE registra el intento fallido en `domain_events` con `event_type JournalBalanceError`, `processed_at NULL` y `processing_error`.

### 12.5 Atomicidad y concurrencia en reserva y liberación de stock

La transición `stock_current → stock_reserved` (al confirmar una cita) y su reverso (al cancelar) se realizan dentro de una sola transacción de base de datos junto con la inserción del `stock_movement` correspondiente y el registro del evento de dominio en la *outbox*. No existe estado intermedio visible.

**Control de concurrencia optimista (v2.0).** Cuando dos citas confirman simultáneamente un mismo insumo, una actualización ciega de `stock_reserved` podría perder una de las dos reservas (condición de carrera *lost update*). Para evitarlo, `inventory.products` incorpora la columna `version` (`@Version` de JPA): cada actualización incrementa la versión y verifica que no haya cambiado desde la lectura. Si otra transacción modificó la fila primero, la actualización falla con `OptimisticLockException` y la operación se reintenta sobre el estado fresco. Solo entonces se evalúa si hay stock suficiente, garantizando que `STOCK_INSUFFICIENT` se dispare de forma confiable incluso bajo alta concurrencia.

### 12.6 Índices GIN en JSONB

Las columnas `ticket_lines` (`cash.payments`), `metadata` (`core.system_alerts`), `payload` (`accounting.domain_events`), `family_hx`, `nonpath_personal_hx` y `path_personal_hx` (`clinical.clinical_histories`) tienen índices GIN para permitir búsquedas eficientes dentro del contenido JSON sin descomponer la estructura flexible.

> **Nota de cambio (v2.0).** Las columnas `content_json` y `content_html` de `clinical.clinical_documents` **ya no llevan índice GIN**: al encriptarse a nivel de aplicación (ADR-005) se almacenan como `BYTEA` y su contenido no es legible por la base de datos. La búsqueda de documentos clínicos se realiza exclusivamente sobre campos no sensibles —`title`, `doc_type`, `created_at` y el número de expediente del paciente—. La historia clínica conserva índices GIN porque sus antecedentes (`family_hx`, etc.) no están clasificados como columnas de cifrado en el ADR-005.

### 12.7 Ordenamiento y entrega de eventos de dominio *(v2.0)*

El procesamiento contable depende del orden correcto de los eventos. Se aplican dos mecanismos complementarios:

- **Orden total de publicación (*outbox*).** El módulo operativo solo inserta el evento en `accounting.domain_events` dentro de su transacción de negocio. Un proceso *relay* lee los registros pendientes (`published_at IS NULL`) ordenados por `sequence_no` y los publica en RabbitMQ. Así, ningún mensaje se pierde si el proceso cae entre el `COMMIT` y la publicación, y la latencia ya no depende de un *job* de reconciliación periódico.
- **Orden por agregado (*consistent hashing*).** El *exchange* de RabbitMQ enruta por `partition_key`, de modo que todos los eventos de una misma cita y su cobro caen en la misma cola y se consumen en orden. Esto impide que un reverso contable (`CitaCancelada`) se procese antes que el cargo que revierte (`PagoRegistrado`).

La idempotencia del ARE se preserva: antes de crear una `journal_entry`, verifica que no exista otra con el mismo `domain_event_id`.

### 12.8 Encriptación de columnas sensibles *(v2.0)*

Conforme al ADR-005 y a la LFPDPPP, las columnas marcadas con 🔒 (`patients.curp`, `clinical_documents.content_json`, `clinical_documents.content_html`, `document_signatures.signature_url`) se encriptan con AES-256-GCM en la capa de aplicación antes de persistirse. Implicaciones de diseño:

- El texto cifrado se guarda como `BYTEA` (o `TEXT` en base64) y **no es indexable ni buscable** por contenido.
- La búsqueda exacta de CURP se resuelve con la columna `curp_hash` (HMAC-SHA-256 determinístico), que también soporta la restricción de unicidad por clínica sin exponer el valor en claro.
- La clave maestra proviene de un KMS externo y se carga en memoria al arrancar el proceso; nunca se persiste en código ni en variables de entorno del repositorio.

---

## 13. Índices Adicionales Recomendados

| Tabla | Índice | Justificación |
|---|---|---|
| `patients` | `(clinic_id, last_name_paternal, first_name)` | Búsqueda por nombre en el catálogo de pacientes |
| `appointments` | `(clinic_id, doctor_id, scheduled_start)` | Vista de agenda por médico y día |
| `appointments` | `(clinic_id, status, scheduled_start)` | Filtro de citas activas del día |
| `stock_movements` | `(clinic_id, product_id, created_at DESC)` | Kardex cronológico por producto |
| `journal_entries` | `(clinic_id, entry_date, entry_type)` | Reportes contables por periodo |
| `journal_lines` | `(account_code, journal_entry_id)` | Auxiliar de cuenta contable |
| `domain_events` | `(clinic_id, event_type, processed_at)` | Cola del ARE — eventos pendientes de procesar |
| `clinical_documents` | `(medical_record_id, doc_type, created_at DESC)` | Listado de documentos por tipo en el expediente |
| `document_signatures` | `(clinical_document_id, signed_at DESC)` | Trazabilidad de firmas por documento |
| `conciliation_items` | `(appointment_id, appointment_procedure_id)` | Conciliación rápida por cita y procedimiento |
| `payments` | `GIN(ticket_lines)` | Búsqueda por conceptos y reimpresión del ticket |
| `products` | `(clinic_id, stock_current) WHERE stock_current <= stock_min` | Partial index para alertas de stock bajo |
| `system_alerts` | `(clinic_id, status, created_at DESC)` | Bandeja operativa de alertas pendientes |
| `system_alerts` | `(clinic_id, alert_type, entity_id)` | Evita alertas duplicadas por entidad |
| `system_alerts` | `(entity_id)` | Consulta directa por entidad afectada |
| `refresh_tokens` *(v2.0)* | `(user_id) WHERE revoked_at IS NULL` | Sesiones activas del usuario para `logout-all` |
| `refresh_tokens` *(v2.0)* | `(expires_at) WHERE revoked_at IS NULL` | Barrido de tokens vencidos |
| `staff_invitations` *(v2.0)* | `(clinic_id, status, expires_at)` | Invitaciones pendientes por clínica |
| `domain_events` *(v2.0)* | `(published_at, sequence_no) WHERE published_at IS NULL` | Cola de la *outbox*: eventos pendientes de publicar en orden |
| `domain_events` *(v2.0)* | `(partition_key, sequence_no)` | Reconstrucción del orden por agregado de negocio |
| `arco_requests` *(v2.0)* | `(clinic_id, status, legal_due_date)` | Seguimiento de solicitudes ARCO por plazo legal |
| `patients` *(v2.0)* | `(clinic_id, curp_hash)` | Búsqueda exacta y unicidad de CURP sin descifrar |
| `sequence_counters` *(v2.0)* | `(clinic_id, scope, period_year)` | Acceso directo al contador para `SELECT ... FOR UPDATE` |
| `doctor_profiles` *(v2.1)* | `(clinic_id, credential_status)` | Filtrar médicos activos por clínica |
| `doctor_profiles` *(v2.1)* | `(cedula_profesional)` | Búsqueda y unicidad de cédula |

---

## 14. Resumen del Modelo

| Schema | Tablas | Aprox. columnas | Nota clave |
|---|---|---|---|
| `core` | 14 | ~165 | Multi-tenancy, RBAC, asistencia, alertas, autenticación OAuth 2.0, folios y perfiles médicos |
| `patients` | 2 | ~38 | Ficha completa NOM-004, consentimiento LFPDPPP, soft delete, derechos ARCO |
| `clinical` | 7 | ~82 | Canvas JSON encriptado, firma digital, conciliación, fotos |
| `scheduling` | 2 | ~27 | Citas con estado, motivo, color y procedimientos asignados |
| `treatments` | 4 | ~35 | Planes, ítems, catálogo y lista de materiales |
| `inventory` | 2 | ~31 | Kardex con snapshot de stock, bloqueo optimista, soporte lotes |
| `cash` | 3 | ~31 | Sesiones de caja, cobros, tickets JSONB |
| `accounting` | 4 | ~44 | Libro Mayor inmutable, ARE, *outbox* de eventos de dominio |
| `audit` | 1 | ~12 | Log append-only de todas las acciones |
| **TOTAL** | **39** | **~465** | **Fase 1 MVP completo — alineado con Arquitectura v2.1** |

> Las tablas marcadas para Fase 2+ (valuación por lote, nómina, CxP, órdenes de compra) se integran a este modelo extendiendo los schemas existentes sin romper la estructura base definida aquí.

### Resumen de cambios v1.0 → v2.0

| Área | Cambio |
|---|---|
| Autenticación | Nuevas tablas `refresh_tokens`, `email_verification_tokens`, `password_reset_tokens`, `password_history`, `staff_invitations`, `user_sessions`; columnas `theme_preference`, `failed_login_attempts`, `locked_until` en `users` |
| LFPDPPP | Columnas de consentimiento y retención en `patients`; aviso de privacidad y Acuerdo de Encargado en `clinics`; nueva tabla `arco_requests` |
| Seguridad de datos | Marcado 🔒 de columnas encriptadas (ADR-005); columna de búsqueda ciega `curp_hash`; endurecimiento de RLS (§12.1) |
| Integridad contable | Patrón *outbox* y clave de partición en `domain_events` (`sequence_no`, `partition_key`, `published_at`, `retry_count`); reglas de ordenamiento (§12.7) |
| Concurrencia | Columna `version` para bloqueo optimista en `products` (§12.5) |
| Folios | Tabla `sequence_counters` para numeración sin huecos de pólizas, tickets y expedientes |
| Coherencia con API | Columnas `reason` y `color_tag` en `appointments`, `allergies_summary` en `patients`; nota de frontera de conversión monetaria pesos↔centavos |

---

*Modelo de Datos — Fase 1 MVP · Sistema de Gestión de Consultorios Médicos · Versión 2.1 · Confidencial · Alineado con Especificación de Arquitectura v2.1*
