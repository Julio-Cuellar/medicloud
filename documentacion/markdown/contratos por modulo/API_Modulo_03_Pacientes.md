# API — Módulo 03: Catálogo de Pacientes
## Sistema de Gestión de Consultorios Médicos · Fase 1 MVP

---

| Campo | Detalle |
|---|---|
| Versión del documento | 1.0 |
| Fecha | Junio 2025 |
| Estado | Revisado · Listo para desarrollo |
| Audiencia | Equipo Backend · Equipo Frontend |
| Módulo | `patients` |
| Prefijo de rutas | `/v1/patients` |
| Tablas involucradas | `patients.patients` · `patients.arco_requests` · `scheduling.appointments` |
| Referencias | Arquitectura v2.0 §2.7 §4.2 · Modelo de Datos v2.0 (schema `patients`) · NOM-004-SSA3-2012 · LFPDPPP · ADR-005 |
| Cambios vs v1.0 | Consentimiento obligatorio del aviso de privacidad en el alta (`privacy_consent_accepted`); registro de `privacy_consent_at`, `privacy_consent_ip` y `data_retention_until`; CURP encriptada (AES-256-GCM) con búsqueda por *hash* ciego `curp_hash`; nuevos endpoints de derechos ARCO. |

---

## Tabla de contenido

1. [Descripción general](#1-descripción-general)
2. [Convenciones del módulo](#2-convenciones-del-módulo)
3. [Flujos cubiertos](#3-flujos-cubiertos)
4. [Reglas de acceso por rol (RBAC)](#4-reglas-de-acceso-por-rol-rbac)
5. [Endpoints](#5-endpoints)
   - [GET /patients](#get-patients)
   - [POST /patients](#post-patients)
   - [GET /patients/{patient_id}](#get-patientspatient_id)
   - [PATCH /patients/{patient_id}](#patch-patientspatient_id)
   - [DELETE /patients/{patient_id}](#delete-patientspatient_id)
   - [POST /patients/{patient_id}/photo](#post-patientspatient_idphoto)
   - [DELETE /patients/{patient_id}/photo](#delete-patientspatient_idphoto)
   - [POST /patients/{patient_id}/arco-request](#post-patientspatient_idarco-request)
   - [GET /arco-requests](#get-arco-requests)
   - [PATCH /arco-requests/{request_id}](#patch-arco-requestsrequest_id)
6. [Privacidad y protección de datos (LFPDPPP)](#6-privacidad-y-protección-de-datos-lfpdppp)
7. [Catálogo de errores del módulo](#7-catálogo-de-errores-del-módulo)
8. [Notas de seguridad](#8-notas-de-seguridad)

---

## 1. Descripción general

Este módulo gestiona la información administrativa y demográfica de los pacientes de la clínica. Los datos recopilados en el alta cumplen estrictamente con la estructura mínima exigida por la **Normativa Oficial Mexicana NOM-004-SSA3-2012** para la ficha de identificación del expediente clínico.

El módulo opera bajo el contexto de multi-tenancy aislado. El backend **requiere obligatoriamente** la cabecera `X-Clinic-ID` en cada solicitud operativa y aplica filtros de seguridad a nivel de base de datos (RLS) para evitar que un usuario de la Clínica A acceda a pacientes de la Clínica B.

Adicionalmente, este módulo materializa las obligaciones de la **Ley Federal de Protección de Datos Personales en Posesión de los Particulares (LFPDPPP)** (Arquitectura v2.0 §2.7): el alta de paciente exige el consentimiento explícito del aviso de privacidad, la CURP se almacena encriptada (AES-256-GCM, ADR-005) y los titulares pueden ejercer sus derechos ARCO a través de los endpoints de la sección 6.

---

## 2. Convenciones del módulo

- Todas las solicitudes y respuestas usan `Content-Type: application/json`, excepto `POST /patients/{patient_id}/photo` que utiliza `multipart/form-data`.
- Todos los endpoints son **privados** y requieren la cabecera `Authorization: Bearer <access_token>` y `X-Clinic-ID`.
- Los montos financieros se expresan en centavos enteros (`integer`). Las fechas en formato ISO 8601 UTC.
- En listados, los pacientes dados de baja lógica (`deleted_at IS NOT NULL`) son **excluidos por defecto** a menos que se solicite explícitamente lo contrario mediante filtros de administración.

---

## 3. Flujos cubiertos

| # | Flujo | Endpoints |
|---|---|---|
| 1 | Listar/Buscar pacientes con paginación | `GET /patients` |
| 2 | Registrar nuevo paciente (NOM-004) | `POST /patients` |
| 3 | Consultar expediente/perfil de paciente | `GET /patients/{patient_id}` |
| 4 | Modificar datos del paciente | `PATCH /patients/{patient_id}` |
| 5 | Dar de baja lógica a un paciente | `DELETE /patients/{patient_id}` |
| 6 | Subir fotografía clínica identificativa | `POST /patients/{patient_id}/photo` |
| 7 | Eliminar fotografía clínica | `DELETE /patients/{patient_id}/photo` |

---

## 4. Reglas de acceso por rol (RBAC)

| Acción | `admin` | `clinic_admin` | `doctor` | `receptionist` | `assistant` | `accountant` | `cleaning` |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Listar / Buscar | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| Ver Detalle | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| Crear Paciente | ✅ | ✅ | ✅ | ✅ | ✅ | — | — |
| Editar Paciente | ✅ | ✅ | ✅ | ✅ | ✅ | — | — |
| Cargar Foto | ✅ | ✅ | ✅ | ✅ | ✅ | — | — |
| Eliminar Foto | ✅ | ✅ | ✅ | ✅ | — | — | — |
| Dar de Baja | ✅ | ✅ | — | — | — | — | — |

---

## 5. Endpoints

---

### `GET /patients`

**Acceso:** Privado · Requiere `X-Clinic-ID`  
**Descripción:** Devuelve un listado paginado de los pacientes pertenecientes a la clínica activa. Permite realizar búsquedas rápidas por nombre, número de expediente y CURP.

**Query Parameters:**

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `q` | `string` | ✗ | Búsqueda por coincidencia parcial en `first_name`, `last_name_paternal`, `last_name_maternal` y `record_number`. La búsqueda por **CURP es exacta**, no parcial: al estar la columna encriptada (ADR-005), se resuelve contra el *hash* ciego `curp_hash` (HMAC-SHA-256), por lo que requiere la CURP completa. |
| `status` | `string` | ✗ | Filtro de estado: `active` (por defecto) o `deleted` (incluye bajas lógicas). |
| `page` | `integer` | ✗ | Número de página. Por defecto: `1`. |
| `per_page` | `integer` | ✗ | Registros por página. Mínimo `1`, máximo `100`. Por defecto: `25`. |

**Respuesta `200`:**

```json
{
  "data": [
    {
      "id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
      "record_number": "EXP-2025-00041",
      "first_name": "María",
      "last_name_paternal": "López",
      "last_name_maternal": "Martínez",
      "curp": "LOMM850322MDFRRS09",
      "birth_date": "1985-03-22",
      "sex": "female",
      "phone_mobile": "+52 55 1234 5678",
      "email": "maria.lopez@email.com",
      "is_active": true,
      "created_at": "2025-06-12T14:30:00Z"
    }
  ],
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-12T14:40:00Z",
    "pagination": {
      "page": 1,
      "per_page": 25,
      "total_items": 87,
      "total_pages": 4
    }
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `CLINIC_ACCESS_DENIED` | 403 | El usuario no tiene rol activo en la clínica especificada en `X-Clinic-ID`. |
| `VALIDATION_ERROR` | 422 | Parámetros de paginación inválidos. |

---

### `POST /patients`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `receptionist`, `assistant`  
**Descripción:** Registra un nuevo paciente en la clínica. El backend valida los campos obligatorios bajo la NOM-004-SSA3-2012 y genera de manera incremental el número de expediente (`record_number`) de forma atómica y aislada por clínica.

**Body:**

```json
{
  "first_name": "María",
  "last_name_paternal": "López",
  "last_name_maternal": "Martínez",
  "birth_date": "1985-03-22",
  "sex": "female",
  "curp": "LOMM850322MDFRRS09",
  "phone_mobile": "+52 55 1234 5678",
  "phone_home": "+52 55 9876 5432",
  "email": "maria.lopez@email.com",
  "blood_type": "O+",
  "nationality": "Mexicana",
  "occupation": "Contadora",
  "religion": "Católica",
  "marital_status": "Casada",
  "education_level": "Licenciatura",
  "address_street": "Calle Morelos 45, Col. Centro",
  "address_city": "Ciudad de México",
  "address_state": "CDMX",
  "address_zip": "06000",
  "emergency_contact_name": "Juan López",
  "emergency_contact_phone": "+52 55 4321 8765",
  "emergency_contact_rel": "Esposo",
  "referring_doctor": "Dr. Carlos Altamirano",
  "allergies_summary": "Penicilina",
  "notes": "Paciente alérgica a la penicilina.",
  "privacy_consent_accepted": true
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `first_name` | `string` | ✅ | Nombre(s) del paciente. Máx 100 caracteres. |
| `last_name_paternal` | `string` | ✅ | Apellido paterno. Máx 100 caracteres. |
| `last_name_maternal` | `string` | ✗ | Apellido materno. Máx 100 caracteres. |
| `birth_date` | `string (YYYY-MM-DD)` | ✅ | Fecha de nacimiento. Debe ser una fecha pasada válida. |
| `sex` | `string` | ✅ | Debe ser uno de: `male`, `female`, `other`. |
| `curp` | `string` | ✗ | CURP del paciente (18 caracteres). Única por clínica. Se **encripta** con AES-256-GCM antes de persistirse (ADR-005); la unicidad y la búsqueda exacta operan sobre `curp_hash`. |
| `phone_mobile` | `string` | ✗ | Teléfono móvil. Máx 20 caracteres. |
| `email` | `string` | ✗ | Correo electrónico con formato válido. |
| `address_zip` | `string` | ✗ | Código postal. Debe tener formato de 5 dígitos (México). |
| `allergies_summary` | `string` | ✗ | Resumen de alergias para la ficha rápida. |
| `privacy_consent_accepted` | `boolean` | ✅ | Aceptación del aviso de privacidad por el paciente (LFPDPPP). Debe ser `true`. |

> **Consentimiento LFPDPPP (Arquitectura v2.0 §2.7).** `privacy_consent_accepted` es obligatorio y debe enviarse en `true`. El backend registra automáticamente `privacy_consent_at` (timestamp del servidor) y `privacy_consent_ip` (IP de origen). Si se omite o es `false`, el alta se rechaza con `422 PRIVACY_CONSENT_REQUIRED`. La columna `data_retention_until` se inicializa en este alta (mínimo 5 años, NOM-004) y se recalcula con cada cita completada.

**Respuesta `201`:**

```json
{
  "data": {
    "id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
    "record_number": "EXP-2025-00042",
    "first_name": "María",
    "last_name_paternal": "López",
    "last_name_maternal": "Martínez",
    "created_at": "2025-06-12T14:30:00Z"
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-12T14:30:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `CURP_ALREADY_EXISTS` | 409 | El CURP provisto ya está registrado en otro paciente activo de la misma clínica. |
| `PRIVACY_CONSENT_REQUIRED` | 422 | No se envió la aceptación del aviso de privacidad (`privacy_consent_accepted`). |
| `VALIDATION_ERROR` | 422 | Faltan campos requeridos o tienen formato inválido (ej. CURP mal estructurado, fecha futura). |

---

### `GET /patients/{patient_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID`  
**Descripción:** Devuelve la información de perfil completa de un paciente junto con un resumen operativo y financiero (citas, deudas, tratamientos activos).

**Respuesta `200`:**

```json
{
  "data": {
    "id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
    "record_number": "EXP-2025-00042",
    "first_name": "María",
    "last_name_paternal": "López",
    "last_name_maternal": "Martínez",
    "curp": "LOMM850322MDFRRS09",
    "birth_date": "1985-03-22",
    "sex": "female",
    "blood_type": "O+",
    "nationality": "Mexicana",
    "occupation": "Contadora",
    "religion": "Católica",
    "marital_status": "Casada",
    "education_level": "Licenciatura",
    "phone_mobile": "+52 55 1234 5678",
    "phone_home": "+52 55 9876 5432",
    "email": "maria.lopez@email.com",
    "address_street": "Calle Morelos 45, Col. Centro",
    "address_city": "Ciudad de México",
    "address_state": "CDMX",
    "address_zip": "06000",
    "emergency_contact_name": "Juan López",
    "emergency_contact_phone": "+52 55 4321 8765",
    "emergency_contact_rel": "Esposo",
    "referring_doctor": "Dr. Carlos Altamirano",
    "allergies_summary": "Penicilina",
    "photo_url": "https://storage.medicloud-assets.mx/photos/a9b8c7d6_256.jpg",
    "notes": "Paciente alérgica a la penicilina.",
    "privacy_consent_at": "2025-06-12T14:30:00Z",
    "data_retention_until": "2030-05-10",
    "is_active": true,
    "created_at": "2025-06-12T14:30:00Z",
    "summary": {
      "total_appointments": 12,
      "last_appointment_at": "2025-05-10T09:00:00Z",
      "active_treatment_plan": true,
      "pending_balance_cents": 150000
    }
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-12T14:45:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `PATIENT_NOT_FOUND` | 404 | El paciente no existe o pertenece a otra clínica. |

---

### `PATCH /patients/{patient_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `receptionist`, `assistant`  
**Descripción:** Modificación parcial de los datos demográficos o de contacto del paciente. El `record_number` es inmutable y no se puede modificar por este endpoint.

**Body (todos los campos son opcionales):**

```json
{
  "phone_mobile": "+52 55 9999 8888",
  "email": "maria.lopez.new@email.com",
  "address_street": "Paseo de la Reforma 222, Apto 5B"
}
```

**Respuesta `200`:** Devuelve el recurso del paciente actualizado completo (mismo esquema que `GET /patients/{patient_id}`).

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `PATIENT_NOT_FOUND` | 404 | El paciente no existe en esta clínica. |
| `CURP_ALREADY_EXISTS` | 409 | Se intentó modificar el CURP a uno que ya está asignado a otro paciente. |
| `VALIDATION_ERROR` | 422 | Formato de campos inválido. |

---

### `DELETE /patients/{patient_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`  
**Descripción:** Realiza la baja lógica (`soft delete`) del paciente, registrando `deleted_at = now()`. El expediente clínico e historial médico se conservan inmutables por ley de auditoría (NOM-004).

**Reglas de negocio e implicaciones de la baja:**
1. **Validación de deudas**: Si el paciente cuenta con saldo deudor en caja (`pending_balance_cents > 0`), la operación se rechaza con código `409 PATIENT_HAS_PENDING_BALANCE`.
2. **Cancelación de citas futuras en cascada**: Si el paciente tiene citas futuras programadas o confirmadas (`scheduling.appointments.status` en `scheduled` o `confirmed`), el sistema las cancelará automáticamente de forma atómica:
   - Modifica el estado de las citas a `cancelled`.
   - Libera cualquier inventario e insumo reservado para dichas citas (`inventory.products.stock_reserved`).
   - Dispara y registra los eventos de dominio `CitaCancelada` para cada una de ellas para fines de auditoría del motor contable (ARE).

**Respuesta `200`:**

```json
{
  "data": {
    "message": "Paciente dado de baja correctamente.",
    "cancelled_appointments_count": 3,
    "released_inventory_items_count": 5
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-12T15:00:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `PATIENT_NOT_FOUND` | 404 | El paciente no existe o ya fue dado de baja lógica. |
| `PATIENT_HAS_PENDING_BALANCE` | 409 | El paciente tiene un saldo pendiente mayor a 0 y no puede ser dado de baja hasta saldarlo. |

---

### `POST /patients/{patient_id}/photo`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `receptionist`, `assistant`  
**Content-Type:** `multipart/form-data`  
**Descripción:** Carga o actualiza la fotografía de identificación del paciente. La imagen anterior se elimina del storage. El sistema redimensiona la imagen y almacena las versiones necesarias para la UI.

**Form Fields:**

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `file` | `file` | ✅ | Archivo de imagen. Formatos: JPG, PNG, WebP. Tamaño máximo: **2 MB**. |

**Respuesta `200`:**

```json
{
  "data": {
    "photo_url": "https://storage.medicloud-assets.mx/photos/a9b8c7d6_256.jpg",
    "photo_url_small": "https://storage.medicloud-assets.mx/photos/a9b8c7d6_64.jpg"
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-12T15:10:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `PATIENT_NOT_FOUND` | 404 | El paciente no existe en esta clínica. |
| `FILE_TOO_LARGE` | 422 | El archivo excede el tamaño máximo permitido (2 MB). |
| `INVALID_FILE_TYPE` | 422 | El tipo MIME real (verificado por bytes mágicos) no es una imagen válida. |

---

### `DELETE /patients/{patient_id}/photo`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `receptionist`  
**Descripción:** Elimina la fotografía del paciente de los buckets de almacenamiento y establece `photo_url = NULL` en la base de datos.

**Respuesta:** `204 No Content`

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `PATIENT_NOT_FOUND` | 404 | El paciente no existe en esta clínica. |
| `PATIENT_NO_PHOTO` | 404 | El paciente no cuenta con una fotografía previa cargada. |

---

### `POST /patients/{patient_id}/arco-request`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `receptionist`  
**Descripción:** Registra una solicitud de ejercicio de derechos ARCO del paciente (LFPDPPP). Crea el registro en `patients.arco_requests`, calcula la fecha límite legal de respuesta (20 días hábiles) y genera una alerta para la clínica.

**Body:**

```json
{
  "request_type": "access",
  "description": "El paciente solicita copia íntegra de su expediente clínico."
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `request_type` | `string` | ✅ | Uno de: `access`, `rectification`, `cancellation`, `opposition`. |
| `description` | `string` | ✗ | Detalle de la solicitud del titular. |

**Respuesta `201`:**

```json
{
  "data": {
    "id": "uuid-solicitud",
    "patient_id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
    "request_type": "access",
    "status": "received",
    "legal_due_date": "2025-07-10",
    "created_at": "2025-06-12T14:30:00Z"
  },
  "meta": { "request_id": "uuid" }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `PATIENT_NOT_FOUND` | 404 | El paciente no existe en esta clínica. |
| `VALIDATION_ERROR` | 422 | `request_type` inválido. |

---

### `GET /arco-requests`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`  
**Descripción:** Lista las solicitudes ARCO de la clínica activa con su plazo legal y estado de atención.

**Query Parameters:** `status` (`received` \| `in_progress` \| `completed` \| `rejected`), `request_type`, `patient_id`, `page`, `per_page`.

**Respuesta `200`:** arreglo de solicitudes con `legal_due_date` y `status`.

---

### `PATCH /arco-requests/{request_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`  
**Descripción:** Actualiza el estado de una solicitud ARCO y registra su resolución (`resolved_by`, `resolved_at`).

**Body:**

```json
{
  "status": "completed",
  "resolution_notes": "Se entregó copia digital del expediente al titular el 2025-06-20."
}
```

**Respuesta `200`:** objeto `data` con la solicitud actualizada.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `ARCO_REQUEST_NOT_FOUND` | 404 | La solicitud no existe en esta clínica. |
| `VALIDATION_ERROR` | 422 | `status` inválido. |

---

## 6. Privacidad y protección de datos (LFPDPPP)

| Obligación | Implementación en este módulo |
|---|---|
| Aviso de privacidad | La clínica configura `privacy_notice_url` (módulo Clínicas). El alta de paciente exige aceptación explícita (`privacy_consent_accepted`). |
| Consentimiento | Se registran `privacy_consent_at` y `privacy_consent_ip` en el alta. |
| Retención mínima (NOM-004) | `data_retention_until` se calcula al crear el paciente (mínimo 5 años) y se recalcula con cada cita completada. |
| Derechos ARCO | `POST /patients/{id}/arco-request` + gestión vía `GET`/`PATCH /arco-requests`. Plazo legal: 20 días hábiles. |
| Encriptación en reposo | La CURP se cifra con AES-256-GCM (ADR-005); la búsqueda exacta usa el *hash* ciego `curp_hash`. |

---

## 7. Catálogo de errores del módulo

| Código | HTTP | Descripción |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Uno o más campos de la solicitud no pasaron la validación (CURP inválido, formato de teléfono o fecha). |
| `PATIENT_NOT_FOUND` | 404 | No se encontró ningún paciente activo con el UUID proporcionado en el contexto de la clínica activa. |
| `CURP_ALREADY_EXISTS` | 409 | El CURP enviado ya está registrado para otro paciente activo dentro del mismo tenant. |
| `PRIVACY_CONSENT_REQUIRED` | 422 | El alta de paciente requiere la aceptación del aviso de privacidad (`privacy_consent_accepted`). |
| `PATIENT_HAS_PENDING_BALANCE` | 409 | No se puede realizar la baja del paciente debido a que cuenta con un saldo pendiente en su estado de cuenta. |
| `ARCO_REQUEST_NOT_FOUND` | 404 | La solicitud ARCO no existe en la clínica activa. |
| `FILE_TOO_LARGE` | 422 | El tamaño del archivo de la foto del paciente excede el límite máximo de 2 MB. |
| `INVALID_FILE_TYPE` | 422 | El archivo cargado no contiene una firma binaria de imagen válida (MIME no aceptado). |
| `PATIENT_NO_PHOTO` | 404 | El paciente seleccionado no cuenta con una fotografía previa para eliminar. |

---

## 8. Notas de seguridad

| # | Control de Seguridad | Propósito y Justificación |
|---|---|---|
| 1 | **Aislamiento Multi-Tenant (RLS)** | El backend debe asegurar que todas las consultas a `patients.patients` apliquen un filtro estricto de `clinic_id = current_setting('app.current_clinic_id')`. El UUID del paciente nunca se debe consultar globalmente sin validar la clínica activa. |
| 2 | **Validación del Header `X-Clinic-ID`** | El middleware de autorización debe cruzar el `X-Clinic-ID` recibido contra la tabla `core.clinic_staff` para verificar que el usuario tenga un rol activo en esa clínica antes de procesar la solicitud. |
| 3 | **Sanitización de Datos de Identificación** | Todos los campos de texto (`first_name`, `last_name_paternal`, `address_street`, `notes`) deben pasar por un proceso de sanitización para remover caracteres que puedan propiciar ataques de XSS o SQL Injection. |
| 4 | **Validación Binaria de Fotografías** | Al subir una foto, no se debe confiar en la extensión del archivo (`.jpg`, `.png`). El backend debe analizar la cabecera binaria del archivo (magic bytes) para validar el tipo MIME real e impedir la ejecución de archivos maliciosos en el storage. |
| 5 | **Baja Lógica y Cumplimiento NOM-004** | El borrado físico (`DELETE`) de pacientes está estrictamente prohibido. Toda eliminación realiza un `soft delete` conservando la integridad de los datos históricos y del expediente para auditorías gubernamentales o médicas futuras. |
| 6 | **Encriptación de CURP a nivel de aplicación (ADR-005)** | La CURP se cifra con AES-256-GCM antes de persistirse; ni un acceso SQL directo expone el valor en claro. La unicidad y la búsqueda exacta operan sobre el *hash* ciego `curp_hash` (HMAC-SHA-256). |
| 7 | **Consentimiento obligatorio y derechos ARCO (LFPDPPP)** | El alta exige consentimiento explícito del aviso de privacidad; el titular puede ejercer derechos ARCO con plazo legal de 20 días hábiles registrado y monitoreado por el sistema. |

---
*API — Módulo 03: Catálogo de Pacientes · Sistema de Gestión de Consultorios Médicos · Fase 1 MVP · v2.0 · Confidencial · Alineado con Arquitectura v2.0 y Modelo de Datos v2.0*
