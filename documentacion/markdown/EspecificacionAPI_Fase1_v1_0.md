# ESPECIFICACIÓN DE API REST
## Sistema de Gestión de Consultorios Médicos
### Fase 1 · MVP Entregable — Versión 2.0 · Junio 2025 · Confidencial

---

| Campo | Detalle |
|---|---|
| Versión | 2.0 |
| Fecha | Junio 2025 |
| Estado | Revisado — alineado con Arquitectura v2.0 y Modelo de Datos v2.0 |
| Audiencia | Desarrolladores Backend y Frontend |
| Referencias | Arquitectura v2.0 · Modelo de Datos v2.0 · UI/UX v2.0 |
| Base URL (producción) | `https://api.medicloud.mx/v1` |
| Base URL (staging) | `https://api-staging.medicloud.mx/v1` |
| Protocolo | HTTPS obligatorio · TLS 1.3 mínimo |
| Cambios vs v1.0 | §2 reescrito para **OAuth 2.0** (Spring Authorization Server): emisión de tokens vía `POST /oauth2/token`, `access_token` JWT firmado con **RS256**, `refresh_token` opaco en cookie `HttpOnly`, rotación y `logout-all`. Nuevos endpoints de verificación de correo, invitaciones de personal, derechos ARCO (LFPDPPP) y stream de eventos en tiempo real (SSE). Alineación de campos de paciente y cita con el Modelo de Datos v2.0. |

> **Nota de versión.** Esta versión 2.0 cierra la brecha de coherencia con la Arquitectura v2.0 y el Modelo de Datos v2.0. El esquema de autenticación de la v1.0 (login con JWT simétrico y refresh en el cuerpo de la petición) queda **derogado** y sustituido por el flujo OAuth 2.0 descrito en §2.

---

## Tabla de Contenido

1. [Convenciones Generales](#1-convenciones-generales)
2. [Autenticación y Sesión](#2-autenticación-y-sesión)
3. [Módulo: Clínicas y Organizaciones](#3-módulo-clínicas-y-organizaciones)
4. [Módulo: Pacientes](#4-módulo-pacientes)
5. [Módulo: Expediente Clínico](#5-módulo-expediente-clínico)
6. [Módulo: Agenda y Citas](#6-módulo-agenda-y-citas)
7. [Módulo: Tratamientos y Presupuestos](#7-módulo-tratamientos-y-presupuestos)
8. [Módulo: Inventario](#8-módulo-inventario)
9. [Módulo: Caja y Cobros](#9-módulo-caja-y-cobros)
10. [Módulo: Contabilidad (ARE + Libro Mayor)](#10-módulo-contabilidad-are--libro-mayor)
11. [Módulo: Personal y Asistencia](#11-módulo-personal-y-asistencia)
12. [Módulo: Alertas del Sistema](#12-módulo-alertas-del-sistema)
13. [Módulo: Eventos en Tiempo Real (SSE)](#13-módulo-eventos-en-tiempo-real-sse)
14. [Módulo: Privacidad y Derechos ARCO (LFPDPPP)](#14-módulo-privacidad-y-derechos-arco-lfpdppp)
15. [Catálogo de Errores](#15-catálogo-de-errores)
16. [Paginación y Filtros Comunes](#16-paginación-y-filtros-comunes)

---

## 1. Convenciones Generales

### 1.1 Formato de solicitudes y respuestas

- Todas las solicitudes y respuestas usan `Content-Type: application/json`.
- Las fechas se expresan en formato **ISO 8601**: `YYYY-MM-DDTHH:mm:ssZ` (UTC).
- Los montos monetarios se expresan en **centavos enteros** (`integer`) para evitar errores de punto flotante. Ejemplo: $2,800.00 MXN → `280000`. Esta es la **única** representación monetaria del contrato de API. Internamente, el Modelo de Datos v2.0 almacena los importes como `NUMERIC` con escala 2; la conversión centavos↔`NUMERIC` ocurre exclusivamente en la capa de mapeo del backend (MapStruct). Ningún endpoint acepta ni devuelve montos en formato decimal.
- Los identificadores son **UUID v4** en formato string.
- Los campos opcionales ausentes se omiten del JSON de respuesta (no se envía `null` explícito salvo que sea semánticamente relevante).

### 1.2 Cabeceras obligatorias en cada solicitud

| Cabecera | Tipo | Descripción |
|---|---|---|
| `Authorization` | `string` | `Bearer <access_token>` — requerido en todos los endpoints excepto `/oauth2/token` y los `/auth/*` públicos (login, refresh, reset, verify, accept-invitation) |
| `X-Clinic-ID` | `UUID` | ID de la clínica activa del tenant. Requerido en todos los endpoints operativos. |
| `Content-Type` | `string` | `application/json` |
| `Accept-Language` | `string` | `es-MX` (predeterminado) |

### 1.3 Estructura de respuesta exitosa

```json
{
  "data": { },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-12T14:30:00Z"
  }
}
```

Para listados:

```json
{
  "data": [ ],
  "meta": {
    "request_id": "...",
    "timestamp": "...",
    "pagination": {
      "page": 1,
      "per_page": 25,
      "total_items": 134,
      "total_pages": 6
    }
  }
}
```

### 1.4 Estructura de respuesta de error

```json
{
  "error": {
    "code": "PATIENT_NOT_FOUND",
    "message": "No se encontró el paciente con el ID proporcionado.",
    "field": null,
    "request_id": "uuid-de-la-solicitud"
  }
}
```

Para errores de validación (`422`), el campo `field` indica el campo problemático y `details` contiene el arreglo de errores:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "La solicitud contiene campos inválidos.",
    "details": [
      { "field": "birth_date", "message": "Fecha inválida. Use formato YYYY-MM-DD." },
      { "field": "curp", "message": "CURP no tiene el formato correcto." }
    ],
    "request_id": "uuid-de-la-solicitud"
  }
}
```

### 1.5 Códigos de estado HTTP utilizados

| Código | Significado |
|---|---|
| `200 OK` | Solicitud exitosa con cuerpo de respuesta |
| `201 Created` | Recurso creado exitosamente |
| `204 No Content` | Solicitud exitosa sin cuerpo de respuesta (ej. baja lógica) |
| `400 Bad Request` | Solicitud malformada (JSON inválido, tipo incorrecto) |
| `401 Unauthorized` | Token ausente, inválido o expirado |
| `403 Forbidden` | Token válido pero sin permiso para el recurso/acción |
| `404 Not Found` | Recurso no encontrado |
| `409 Conflict` | Conflicto de estado (ej. cita en horario ya ocupado) |
| `422 Unprocessable Entity` | Errores de validación de negocio |
| `429 Too Many Requests` | Rate limit excedido |
| `500 Internal Server Error` | Error interno no controlado |

### 1.6 Rate limiting

- **Límite general**: 300 solicitudes por minuto por `access_token`.
- **Endpoints de auth**: 10 solicitudes por minuto por IP.
- Las respuestas incluyen cabeceras: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`.

---

## 2. Autenticación y Sesión

El sistema implementa **OAuth 2.0** con **Spring Authorization Server** como servidor de autorización self-hosted (Arquitectura v2.0 §4.1, ADR-002). El flujo para la SPA de primera parte es *Resource Owner Password*.

| Token | Formato | Vigencia | Almacenamiento en el cliente |
|---|---|---|---|
| `access_token` | JWT firmado con **RS256** | 1 hora | Memoria del proceso JS (nunca `localStorage`) |
| `refresh_token` | Opaco (referencia en BD) | 30 días | Cookie `HttpOnly; SameSite=Strict; Secure` |

El `access_token` se valida en cada petición por el Resource Server contra la clave pública del Authorization Server. El `refresh_token` es opaco y se persiste en `core.refresh_tokens`, lo que permite revocación inmediata, rotación y cierre de todas las sesiones. El `clinic_id` activo **no** viaja en el token: se envía en la cabecera `X-Clinic-ID` en cada petición y se valida contra el claim `clinics` del JWT; además se inyecta en el contexto de base de datos (`SET LOCAL app.clinic_id`) para aplicar Row Level Security.

### Estructura del `access_token` (JWT)

```json
{
  "sub": "uuid-usuario",
  "iss": "https://api.medicloud.mx",
  "iat": 1719000000,
  "exp": 1719003600,
  "email": "dr.garcia@clinica.com",
  "roles": ["doctor"],
  "clinics": ["uuid-clinica-1", "uuid-clinica-2"]
}
```

> La cabecera del JWT declara `"alg": "RS256"`. El cliente no necesita verificar la firma; lo hace el Resource Server. El frontend solo decodifica el *payload* para leer `roles` y `clinics`.

---

### `POST /oauth2/token` — grant_type `password`

Autentica al usuario y emite el par de tokens. El `refresh_token` se devuelve **como cookie** `HttpOnly`, no en el cuerpo de la respuesta.

**Content-Type:** `application/x-www-form-urlencoded`

**Body (form):**
```
grant_type=password
username=dr.garcia@clinica.com
password=contraseña-segura
scope=openid profile
```

**Respuesta `200`:**
```json
{
  "data": {
    "access_token": "eyJhbGciOiJSUzI1NiIs...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "user": {
      "id": "uuid",
      "full_name": "Dr. Roberto García",
      "email": "dr.garcia@clinica.com",
      "avatar_url": "https://storage.medicloud.mx/avatars/uuid.jpg",
      "clinics": [
        { "id": "uuid-clinica-1", "name": "Clínica Reforma", "role": "doctor" }
      ]
    }
  }
}
```

**Cabecera de respuesta:**
```
Set-Cookie: refresh_token=<opaco>; HttpOnly; Secure; SameSite=Strict; Path=/oauth2; Max-Age=2592000
```

**Errores posibles:** `401 INVALID_CREDENTIALS`, `403 EMAIL_NOT_VERIFIED`, `403 ACCOUNT_LOCKED`, `422 VALIDATION_ERROR`, `429 RATE_LIMIT_EXCEEDED`

> **Bloqueo por intentos fallidos.** Tras 5 intentos fallidos consecutivos la cuenta se bloquea 15 minutos (`core.users.locked_until`); durante ese lapso el endpoint responde `403 ACCOUNT_LOCKED`. El endpoint está limitado a 10 solicitudes por minuto por IP.

---

### `POST /oauth2/token` — grant_type `refresh_token`

Obtiene un nuevo `access_token` usando el `refresh_token` que viaja automáticamente en la cookie. **No** se envía en el cuerpo. Implementa rotación: el token anterior se invalida y se emite uno nuevo en la cookie.

**Content-Type:** `application/x-www-form-urlencoded`

**Body (form):**
```
grant_type=refresh_token
```

**Respuesta `200`:** mismo esquema que el *grant* `password` sin el objeto `user`. Se emite una nueva cookie `refresh_token`.

**Errores posibles:** `401 REFRESH_TOKEN_INVALID`, `401 REFRESH_TOKEN_EXPIRED`, `401 REFRESH_TOKEN_REUSED`

> **Detección de reuso.** Si se presenta un `refresh_token` ya rotado (posible robo), el Authorization Server revoca toda la cadena de tokens del usuario y responde `401 REFRESH_TOKEN_REUSED`, forzando un nuevo inicio de sesión.

> **Re-hidratación al recargar la SPA.** Como el `access_token` vive solo en memoria, al recargar la página el frontend hace una petición silenciosa a este endpoint (la cookie se envía sola) antes de renderizar la interfaz (ADR-002).

---

### `POST /auth/logout`

Revoca el `refresh_token` activo de la sesión actual y limpia la cookie.

**Cabeceras:** `Authorization: Bearer <access_token>` + cookie `refresh_token`.

**Body:** vacío.

**Respuesta:** `204 No Content` con `Set-Cookie: refresh_token=; Max-Age=0`.

---

### `POST /auth/logout-all`

Revoca **todas** las sesiones activas del usuario en todos sus dispositivos (borra todos sus `refresh_tokens`).

**Cabeceras:** `Authorization: Bearer <access_token>`

**Respuesta:** `204 No Content`

---

### `GET /auth/sessions`

Lista las sesiones activas del usuario para auditoría de dispositivos (IP, user-agent, último uso). Datos tomados de `core.user_sessions`.

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid-sesion",
      "device_label": "Chrome en Windows",
      "ip_address": "189.203.x.x",
      "started_at": "2025-06-12T08:00:00Z",
      "last_seen_at": "2025-06-12T13:40:00Z",
      "is_current": true
    }
  ]
}
```

---

### `POST /auth/request-password-reset`

Envía correo de restablecimiento de contraseña.

**Body:**
```json
{ "email": "dr.garcia@clinica.com" }
```

**Respuesta `200`:** Siempre devuelve éxito (sin revelar si el email existe). El token tiene vigencia de 1 hora y es de uso único (`core.password_reset_tokens`).

---

### `POST /auth/reset-password`

Establece una nueva contraseña usando el token recibido por correo. La nueva contraseña no puede coincidir con las últimas 5 (`core.password_history`).

**Body:**
```json
{
  "token": "token-del-correo",
  "new_password": "nueva-contraseña-segura"
}
```

**Respuesta `200`:** `{ "data": { "message": "Contraseña actualizada correctamente." } }`

**Errores posibles:** `401 RESET_TOKEN_INVALID`, `401 RESET_TOKEN_EXPIRED`, `422 PASSWORD_TOO_WEAK`, `422 PASSWORD_REUSED`

---

### `POST /auth/verify-email`

Verifica el correo del usuario con el token enviado al registrarse (`core.email_verification_tokens`, vigencia 24 h, uso único).

**Body:**
```json
{ "token": "token-del-correo" }
```

**Respuesta `200`:** `{ "data": { "email_verified": true } }`

**Errores posibles:** `401 VERIFICATION_TOKEN_INVALID`, `401 VERIFICATION_TOKEN_EXPIRED`

---

### `POST /auth/accept-invitation`

Activa la cuenta de un miembro del personal invitado por correo (`core.staff_invitations`). Si el correo ya existe en otro tenant, vincula al usuario existente; si es nuevo, crea el usuario y establece su contraseña.

**Body:**
```json
{
  "token": "token-de-invitacion",
  "full_name": "Dra. Ana Martínez",
  "password": "contraseña-segura"
}
```

**Respuesta `200`:** mismo esquema de tokens que el *grant* `password` (el usuario queda autenticado).

**Errores posibles:** `401 INVITATION_INVALID`, `401 INVITATION_EXPIRED`, `409 INVITATION_ALREADY_ACCEPTED`, `422 PASSWORD_TOO_WEAK`

---

### `GET /auth/me`

Devuelve el perfil del usuario autenticado y sus clínicas asignadas.

**Respuesta `200`:**
```json
{
  "data": {
    "id": "uuid",
    "full_name": "Dr. Roberto García",
    "email": "dr.garcia@clinica.com",
    "phone": "+52 55 1234 5678",
    "avatar_url": "https://...",
    "email_verified": true,
    "theme_preference": "light",
    "clinics": [
      {
        "id": "uuid-clinica-1",
        "name": "Clínica Reforma",
        "role": "doctor",
        "role_label": "Médico"
      }
    ]
  }
}
```

---

### `PATCH /auth/me`

Actualiza preferencias del usuario (nombre, teléfono, tema).

**Body (todos los campos son opcionales):**
```json
{
  "full_name": "Dr. Roberto García López",
  "phone": "+52 55 9876 5432",
  "theme_preference": "dark"
}
```

**Respuesta `200`:** objeto `data` con el perfil actualizado.

---

## 3. Módulo: Clínicas y Organizaciones

### `GET /clinics/{clinic_id}`

Devuelve la configuración completa de la clínica activa.

**Respuesta `200`:**
```json
{
  "data": {
    "id": "uuid",
    "name": "Clínica Reforma",
    "rfc": "CRE200101ABC",
    "address": "Av. Reforma 500, CDMX",
    "phone": "+52 55 5555 1234",
    "email": "contacto@clinicareforma.com",
    "logo_url": "https://storage.medicloud.mx/logos/uuid.png",
    "specialties": ["Medicina General", "Cardiología"],
    "organization_id": "uuid-org",
    "is_active": true,
    "created_at": "2025-01-15T10:00:00Z"
  }
}
```

---

### `PATCH /clinics/{clinic_id}`

Actualiza la configuración de la clínica. Solo accesible para roles `admin` y `clinic_admin`.

**Body (todos los campos son opcionales):**
```json
{
  "name": "Clínica Reforma Norte",
  "phone": "+52 55 5555 9999",
  "specialties": ["Medicina General", "Cardiología", "Dermatología"]
}
```

**Respuesta `200`:** objeto `data` con la clínica actualizada.

---

### `GET /organizations`

Lista las organizaciones a las que pertenece el usuario autenticado. Solo visible para roles `admin`.

**Respuesta `200`:** arreglo de organizaciones con sus clínicas anidadas.

---

## 4. Módulo: Pacientes

### `GET /patients`

Lista los pacientes de la clínica activa con soporte de búsqueda y filtros.

**Query params:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `q` | `string` | Búsqueda por nombre, apellido, CURP o número de expediente |
| `status` | `active` \| `inactive` | Filtro por estado. Default: `active` |
| `page` | `integer` | Número de página. Default: `1` |
| `per_page` | `integer` | Resultados por página. Default: `25`, máximo: `100` |

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid",
      "medical_record_number": "EXP-2025-00041",
      "full_name": "María López Martínez",
      "birth_date": "1985-03-22",
      "sex": "female",
      "phone": "+52 55 1234 5678",
      "email": "maria.lopez@email.com",
      "last_appointment_at": "2025-05-10T09:00:00Z",
      "is_active": true
    }
  ],
  "meta": { "pagination": { "page": 1, "per_page": 25, "total_items": 87, "total_pages": 4 } }
}
```

---

### `POST /patients`

Crea un nuevo paciente. Los campos marcados como obligatorios por NOM-004-SSA3-2012 son requeridos.

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
  "email": "maria.lopez@email.com",
  "address_street": "Calle Morelos 45, Col. Centro",
  "address_city": "Ciudad de México",
  "address_state": "CDMX",
  "address_zip": "06000",
  "occupation": "Contadora",
  "emergency_contact_name": "Juan López",
  "emergency_contact_phone": "+52 55 9876 5432",
  "emergency_contact_rel": "Esposo",
  "blood_type": "O+",
  "allergies_summary": "Penicilina",
  "privacy_consent_accepted": true
}
```

**Campos requeridos:** `first_name`, `last_name_paternal`, `birth_date`, `sex`, `privacy_consent_accepted`.

> **Consentimiento LFPDPPP (Arquitectura v2.0 §2.7).** El campo `privacy_consent_accepted` debe enviarse en `true`: representa la aceptación del aviso de privacidad de la clínica por parte del paciente. El backend registra automáticamente `privacy_consent_at` (timestamp del servidor) y `privacy_consent_ip` (IP de origen de la petición). Si se omite o es `false`, el alta se rechaza con `422 PRIVACY_CONSENT_REQUIRED`. La columna `data_retention_until` se inicializa en este alta y se recalcula con cada cita completada.

> **CURP encriptada (ADR-005).** La CURP se almacena cifrada con AES-256-GCM y se indexa por un *hash* ciego (`curp_hash`). La unicidad por clínica y la búsqueda exacta en `GET /patients?q=` operan sobre ese hash, no sobre el valor en claro.

**Respuesta `201`:**
```json
{
  "data": {
    "id": "uuid-nuevo-paciente",
    "medical_record_number": "EXP-2025-00042",
    "full_name": "María López Martínez",
    "privacy_consent_at": "2025-06-12T14:30:00Z",
    "created_at": "2025-06-12T14:30:00Z"
  }
}
```

**Errores posibles:** `422 VALIDATION_ERROR`, `422 PRIVACY_CONSENT_REQUIRED`, `409 CURP_ALREADY_EXISTS`

---

### `GET /patients/{patient_id}`

Devuelve el perfil completo del paciente incluyendo accesos rápidos a sus módulos.

**Respuesta `200`:**
```json
{
  "data": {
    "id": "uuid",
    "medical_record_number": "EXP-2025-00042",
    "first_name": "María",
    "last_name_paternal": "López",
    "last_name_maternal": "Martínez",
    "full_name": "María López Martínez",
    "birth_date": "1985-03-22",
    "age": 40,
    "sex": "female",
    "curp": "LOMM850322MDFRRS09",
    "phone_mobile": "+52 55 1234 5678",
    "email": "maria.lopez@email.com",
    "address_street": "Calle Morelos 45, Col. Centro",
    "address_city": "Ciudad de México",
    "address_state": "CDMX",
    "address_zip": "06000",
    "occupation": "Contadora",
    "blood_type": "O+",
    "allergies_summary": "Penicilina",
    "emergency_contact_name": "Juan López",
    "emergency_contact_phone": "+52 55 9876 5432",
    "emergency_contact_rel": "Esposo",
    "privacy_consent_at": "2025-01-10T09:00:00Z",
    "data_retention_until": "2030-05-10",
    "is_active": true,
    "created_at": "2025-01-10T09:00:00Z",
    "summary": {
      "total_appointments": 12,
      "last_appointment_at": "2025-05-10T09:00:00Z",
      "active_treatment_plan": true,
      "pending_balance_cents": 150000
    }
  }
}
```

---

### `PATCH /patients/{patient_id}`

Actualiza los datos del paciente. Solo se actualizan los campos enviados.

**Body:** cualquier subconjunto de campos del `POST /patients`.

**Respuesta `200`:** objeto `data` con el paciente actualizado.

---

### `DELETE /patients/{patient_id}`

Realiza la baja lógica del paciente (`soft delete`). El expediente se conserva para auditoría.

**Respuesta:** `204 No Content`

**Errores posibles:** `409 PATIENT_HAS_PENDING_BALANCE` (no se puede dar de baja con saldo pendiente)

---

## 5. Módulo: Expediente Clínico

### `GET /patients/{patient_id}/medical-record`

Devuelve el expediente clínico del paciente con el listado de documentos agrupados por tipo.

**Respuesta `200`:**
```json
{
  "data": {
    "id": "uuid-expediente",
    "patient_id": "uuid-paciente",
    "medical_record_number": "EXP-2025-00042",
    "documents": {
      "clinical_history": { "id": "uuid", "created_at": "2025-01-10T09:00:00Z", "has_signed": true },
      "medical_notes": [ { "id": "uuid", "title": "Nota 12-Jun-2025", "created_at": "..." } ],
      "prescriptions": [ { "id": "uuid", "title": "Receta 10-May-2025", "created_at": "..." } ],
      "lab_studies": [],
      "imaging": [],
      "informed_consents": [ { "id": "uuid", "title": "Consentimiento Cirugía", "signed_at": "..." } ],
      "treatment_plans": [],
      "clinical_photos": [ { "id": "uuid", "thumbnail_url": "https://...", "created_at": "..." } ],
      "interconsults": [],
      "referrals": [],
      "nursing_sheets": [],
      "administrative": []
    }
  }
}
```

---

### `GET /medical-records/{record_id}/documents`

Lista todos los documentos del expediente con filtros.

**Query params:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `doc_type` | `string` | Filtro por tipo de documento (ver ENUM `doc_type` en el Modelo de Datos) |
| `page` | `integer` | Número de página |
| `per_page` | `integer` | Default: `20` |

---

### `POST /medical-records/{record_id}/documents`

Crea un nuevo documento clínico dentro del expediente.

**Body:**
```json
{
  "doc_type": "medical_note",
  "title": "Nota de evolución — 12 Jun 2025",
  "content_json": {
    "blocks": [
      { "type": "text", "content": "Paciente refiere mejoría del dolor..." },
      { "type": "vitals_table", "data": { "bp": "120/80", "hr": 72, "temp": 36.5, "weight_kg": 65 } }
    ]
  },
  "appointment_id": "uuid-cita-opcional"
}
```

**Nota sobre `content_json`:** El esquema de bloques del canvas se valida contra el catálogo de tipos permitidos. Los bloques marcados como NOM-obligatorios no pueden omitirse en los tipos de documento que los requieran.

**Respuesta `201`:**
```json
{
  "data": {
    "id": "uuid-nuevo-documento",
    "doc_type": "medical_note",
    "title": "Nota de evolución — 12 Jun 2025",
    "is_signed": false,
    "created_at": "2025-06-12T14:30:00Z"
  }
}
```

---

### `GET /clinical-documents/{document_id}`

Devuelve el documento completo incluyendo su `content_json` y el estado de firmas.

**Respuesta `200`:**
```json
{
  "data": {
    "id": "uuid",
    "doc_type": "medical_note",
    "title": "Nota de evolución — 12 Jun 2025",
    "content_json": { "blocks": [] },
    "is_signed": true,
    "is_nom_compliant": true,
    "signatures": [
      {
        "id": "uuid-firma",
        "signer_role": "doctor",
        "signer_name": "Dr. Roberto García",
        "signed_at": "2025-06-12T15:00:00Z",
        "signature_hash": "sha256:abc123..."
      }
    ],
    "created_at": "2025-06-12T14:30:00Z",
    "updated_at": "2025-06-12T14:55:00Z"
  }
}
```

---

### `PATCH /clinical-documents/{document_id}`

Actualiza el contenido de un documento **no firmado**. Una vez firmado, el documento es inmutable.

**Body:**
```json
{
  "title": "Nota de evolución — 12 Jun 2025 (corregida)",
  "content_json": { "blocks": [] }
}
```

**Errores posibles:** `409 DOCUMENT_ALREADY_SIGNED`

---

### `POST /clinical-documents/{document_id}/sign`

Firma digitalmente el documento. Genera un hash de integridad del contenido en el momento de la firma.

**Body:**
```json
{
  "signer_role": "doctor",
  "signature_data": "base64-de-la-firma-dibujada-o-pin-de-confirmacion"
}
```

**Respuesta `200`:**
```json
{
  "data": {
    "signature_id": "uuid-firma",
    "signed_at": "2025-06-12T15:00:00Z",
    "signature_hash": "sha256:abc123..."
  }
}
```

**Errores posibles:** `403 INSUFFICIENT_ROLE`, `409 DOCUMENT_ALREADY_SIGNED`

---

### `POST /clinical-documents/{document_id}/photos`

Adjunta una fotografía clínica al expediente. El archivo se sube como `multipart/form-data`.

**Content-Type:** `multipart/form-data`

**Form fields:**
- `file`: archivo de imagen (JPG, PNG, HEIC — máximo 10 MB)
- `caption`: descripción opcional de la fotografía

**Respuesta `201`:**
```json
{
  "data": {
    "id": "uuid-foto",
    "url": "https://storage.medicloud.mx/clinical-photos/uuid.jpg",
    "thumbnail_url": "https://storage.medicloud.mx/clinical-photos/uuid_thumb.jpg",
    "caption": "Fotografía pre-operatoria",
    "created_at": "2025-06-12T14:35:00Z"
  }
}
```

---

## 6. Módulo: Agenda y Citas

### `GET /appointments`

Lista las citas de la clínica activa con filtros de fecha y médico.

**Query params:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `date` | `YYYY-MM-DD` | Citas de un día específico |
| `date_from` | `YYYY-MM-DD` | Inicio del rango de fechas |
| `date_to` | `YYYY-MM-DD` | Fin del rango de fechas |
| `doctor_id` | `UUID` | Filtrar por médico |
| `status` | `string` | Ver ENUM `appt_status` |
| `patient_id` | `UUID` | Citas de un paciente específico |
| `view` | `day` \| `week` \| `month` | Modo de vista de la agenda |
| `page` | `integer` | Default: `1` |
| `per_page` | `integer` | Default: `50` |

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid",
      "patient_id": "uuid",
      "patient_name": "María López Martínez",
      "doctor_id": "uuid",
      "doctor_name": "Dr. Roberto García",
      "scheduled_start": "2025-06-12T09:00:00Z",
      "scheduled_end": "2025-06-12T09:30:00Z",
      "status": "confirmed",
      "status_label": "Confirmada",
      "reason": "Revisión de seguimiento",
      "procedures": [
        { "procedure_id": "uuid", "name": "Consulta General", "price_cents": 80000 }
      ],
      "color_tag": "#376D6D"
    }
  ]
}
```

---

### `POST /appointments`

Crea una nueva cita. Valida disponibilidad del médico antes de confirmar.

**Body:**
```json
{
  "patient_id": "uuid-paciente",
  "doctor_id": "uuid-medico",
  "scheduled_start": "2025-06-20T10:00:00Z",
  "scheduled_end": "2025-06-20T10:30:00Z",
  "reason": "Consulta de primera vez",
  "procedures": [
    { "procedure_id": "uuid-procedimiento" }
  ],
  "color_tag": "#376D6D",
  "notes": "Paciente requiere silla de ruedas"
}
```

**Respuesta `201`:** objeto `data` con la cita creada.

**Errores posibles:** `409 DOCTOR_NOT_AVAILABLE` (horario ocupado), `409 STOCK_INSUFFICIENT` (si los procedimientos requieren insumos sin stock)

---

### `PATCH /appointments/{appointment_id}`

Actualiza datos de la cita: reprogramación, cambio de médico o notas. Si la cita ya está confirmada y se cambia de horario, se liberan y re-reservan los insumos automáticamente.

**Body (todos los campos son opcionales):**
```json
{
  "scheduled_start": "2025-06-21T11:00:00Z",
  "scheduled_end": "2025-06-21T11:30:00Z",
  "reason": "Consulta de seguimiento",
  "notes": "Se reprogramó por solicitud del paciente"
}
```

**Errores posibles:** `409 APPOINTMENT_NOT_EDITABLE` (citas completadas o canceladas no son editables)

---

### `POST /appointments/{appointment_id}/confirm`

Cambia el estado de la cita a `confirmed` y reserva el stock de insumos requeridos de forma atómica. Publica el evento de dominio `CitaConfirmada`.

**Body:** vacío.

**Respuesta `200`:**
```json
{
  "data": {
    "appointment_id": "uuid",
    "status": "confirmed",
    "stock_reserved": [
      { "product_id": "uuid", "product_name": "Anestesia local", "quantity_reserved": 2.5, "unit": "ml" }
    ]
  }
}
```

**Errores posibles:** `409 STOCK_INSUFFICIENT`

---

### `POST /appointments/{appointment_id}/start`

Marca la cita como `in_progress`. Solo disponible para roles `doctor` y `assistant`.

**Respuesta `200`:** `{ "data": { "status": "in_progress", "started_at": "..." } }`

---

### `POST /appointments/{appointment_id}/complete`

Marca la cita como `completed` y abre la ventana de conciliación de insumos.

**Body:**
```json
{
  "conciliation_items": [
    {
      "product_id": "uuid-producto",
      "appointment_procedure_id": "uuid-procedimiento-cita",
      "quantity_used": 2.0,
      "quantity_wasted": 0.5,
      "unit": "ml"
    }
  ]
}
```

**Comportamiento:** por cada ítem conciliado, se genera un `stock_movement` de tipo `exit` y se publica el evento `ConsumoConciliado` para que el ARE genere el asiento del Momento 2. Las cantidades desperdiciadas generan el evento `MermaCaducidad`.

**Respuesta `200`:** `{ "data": { "status": "completed", "domain_events_published": 3 } }`

---

### `POST /appointments/{appointment_id}/cancel`

Cancela la cita, libera el stock reservado y, si hubo cobro previo, publica el evento para el reverso contable.

**Body:**
```json
{
  "reason": "Solicitud del paciente",
  "generate_reversal": true
}
```

**Respuesta `200`:** `{ "data": { "status": "cancelled", "stock_released": true, "reversal_generated": true } }`

---

### `POST /appointments/{appointment_id}/no-show`

Marca la cita como `no_show`. No libera insumos reservados hasta confirmación manual.

**Respuesta `200`:** `{ "data": { "status": "no_show" } }`

---

## 7. Módulo: Tratamientos y Presupuestos

### `GET /patients/{patient_id}/treatment-plans`

Lista los planes de tratamiento del paciente.

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid",
      "status": "active",
      "total_price_cents": 1500000,
      "paid_cents": 500000,
      "pending_cents": 1000000,
      "created_at": "2025-05-01T10:00:00Z",
      "items_count": 5,
      "items_completed": 2
    }
  ]
}
```

---

### `POST /patients/{patient_id}/treatment-plans`

Crea un nuevo plan de tratamiento.

**Body:**
```json
{
  "notes": "Plan de ortodoncia fase 1",
  "items": [
    {
      "procedure_id": "uuid-procedimiento",
      "quantity": 1,
      "unit_price_cents": 500000,
      "discount_percent": 0,
      "notes": "Incluye brackets metálicos"
    }
  ]
}
```

**Respuesta `201`:** objeto `data` con el plan creado e `id` de cada ítem.

---

### `GET /treatment-plans/{plan_id}`

Devuelve el plan completo con cada ítem, su estado de ejecución y el resumen financiero.

---

### `PATCH /treatment-plans/{plan_id}/items/{item_id}`

Actualiza un ítem del plan (precio, descuento, notas). Solo modificable mientras el plan está en estado `draft` o `active`.

---

### `GET /procedures`

Catálogo de procedimientos de la clínica.

**Query params:** `q` (búsqueda por nombre), `page`, `per_page`.

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid",
      "name": "Consulta General",
      "description": "Consulta médica de primera vez o seguimiento",
      "default_price_cents": 80000,
      "duration_minutes": 30,
      "supplies": [
        { "product_id": "uuid", "product_name": "Guantes nitrilo", "quantity_required": 2, "unit": "pz" }
      ]
    }
  ]
}
```

---

### `POST /procedures`

Crea un nuevo procedimiento en el catálogo. Solo para roles `admin` y `clinic_admin`.

---

### `PATCH /procedures/{procedure_id}`

Actualiza un procedimiento existente.

---

## 8. Módulo: Inventario

### `GET /products`

Lista los productos del inventario de la clínica.

**Query params:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `q` | `string` | Búsqueda por nombre o código |
| `low_stock` | `boolean` | Si `true`, devuelve solo productos bajo el mínimo |
| `expiring_before` | `YYYY-MM-DD` | Productos que caducan antes de esta fecha |
| `page` | `integer` | |
| `per_page` | `integer` | Default: `25` |

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid",
      "name": "Anestesia lidocaína 2%",
      "sku": "ANE-LID-2",
      "unit": "ml",
      "stock_current": 150.0,
      "stock_reserved": 10.0,
      "stock_available": 140.0,
      "stock_min": 50.0,
      "stock_max": 500.0,
      "unit_cost_cents": 250,
      "expiry_date": "2026-03-31",
      "is_low_stock": false,
      "is_near_expiry": false
    }
  ]
}
```

---

### `POST /products`

Da de alta un nuevo producto en el inventario.

**Body:**
```json
{
  "name": "Anestesia lidocaína 2%",
  "sku": "ANE-LID-2",
  "unit": "ml",
  "unit_cost_cents": 250,
  "stock_initial": 200.0,
  "stock_min": 50.0,
  "stock_max": 500.0,
  "expiry_date": "2026-03-31",
  "expiry_alert_days": 30
}
```

**Respuesta `201`:** objeto `data` con el producto creado y el primer `stock_movement` de tipo `entry`.

---

### `PATCH /products/{product_id}`

Actualiza los datos del producto (no modifica el stock directamente — eso se hace vía movimientos).

---

### `POST /products/{product_id}/stock-movements`

Registra un movimiento de inventario (entrada, ajuste, merma o caducidad).

**Body:**
```json
{
  "movement_type": "entry",
  "quantity": 100.0,
  "unit_cost_cents": 250,
  "reference": "Factura proveedor #F-2025-001",
  "notes": "Reabastecimiento mensual"
}
```

**Tipos de movimiento permitidos por este endpoint:** `entry`, `adjustment_up`, `adjustment_down`, `expiry_loss`.

Los movimientos `exit`, `reserve` y `unreserve` son generados internamente por el sistema al completar o cancelar citas.

**Respuesta `201`:**
```json
{
  "data": {
    "movement_id": "uuid",
    "movement_type": "entry",
    "quantity": 100.0,
    "stock_before": 50.0,
    "stock_after": 150.0,
    "created_at": "2025-06-12T14:00:00Z"
  }
}
```

**Eventos publicados:** `InsumoAgregado` (en `entry`) o `MermaCaducidad` (en `expiry_loss`) → procesados por el ARE.

---

### `GET /products/{product_id}/stock-movements`

Devuelve el Kardex cronológico del producto.

**Query params:** `date_from`, `date_to`, `movement_type`, `page`, `per_page`.

---

## 9. Módulo: Caja y Cobros

### `GET /cash-sessions`

Lista las sesiones de caja de la clínica.

**Query params:** `status` (`open` | `closed`), `date_from`, `date_to`, `page`, `per_page`.

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid",
      "opened_by_name": "Recepcionista Laura Gómez",
      "opened_at": "2025-06-12T08:00:00Z",
      "closed_at": null,
      "opening_balance_cents": 200000,
      "current_balance_cents": 785000,
      "status": "open",
      "payments_count": 7
    }
  ]
}
```

---

### `POST /cash-sessions`

Abre una nueva sesión de caja. Solo puede existir **una sesión abierta por clínica** a la vez.

**Body:**
```json
{
  "opening_balance_cents": 200000,
  "notes": "Apertura turno matutino"
}
```

**Respuesta `201`:** objeto `data` con la sesión creada.

**Errores posibles:** `409 CASH_SESSION_ALREADY_OPEN`

---

### `POST /cash-sessions/{session_id}/close`

Cierra la sesión de caja actual con el cuadre de turno.

**Body:**
```json
{
  "closing_balance_cents": 785000,
  "notes": "Cierre turno matutino. Diferencia de $20 en efectivo — moneda."
}
```

**Respuesta `200`:**
```json
{
  "data": {
    "session_id": "uuid",
    "status": "closed",
    "closing_balance_cents": 785000,
    "expected_balance_cents": 787000,
    "difference_cents": -2000,
    "closed_at": "2025-06-12T14:00:00Z"
  }
}
```

---

### `GET /cash-sessions/{session_id}/payments`

Lista los cobros registrados en una sesión de caja.

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid",
      "patient_id": "uuid",
      "patient_name": "María López Martínez",
      "appointment_id": "uuid",
      "payment_type": "full",
      "payment_type_label": "Pago completo",
      "total_cents": 80000,
      "pay_method": "card",
      "pay_method_label": "Tarjeta",
      "ticket_number": "TKT-2025-00041",
      "created_at": "2025-06-12T09:35:00Z"
    }
  ]
}
```

---

### `POST /cash-sessions/{session_id}/payments`

Registra un cobro. Publica el evento `PagoRegistrado` → el ARE genera el asiento del Momento 1.

**Body:**
```json
{
  "patient_id": "uuid-paciente",
  "appointment_id": "uuid-cita",
  "treatment_plan_id": "uuid-plan-opcional",
  "payment_type": "full",
  "total_cents": 80000,
  "pay_method": "cash",
  "ticket_lines": [
    {
      "concept": "Consulta General",
      "quantity": 1,
      "unit_price_cents": 80000,
      "discount_cents": 0,
      "subtotal_cents": 80000
    }
  ],
  "notes": ""
}
```

**Tipos de pago:** `full` (pago completo), `partial` (abono parcial), `advance` (anticipo), `refund` (devolución).

**Métodos de pago:** `cash`, `card`, `transfer`, `check`, `other`.

**Respuesta `201`:**
```json
{
  "data": {
    "payment_id": "uuid",
    "ticket_number": "TKT-2025-00042",
    "total_cents": 80000,
    "journal_entry_id": "uuid-poliza",
    "created_at": "2025-06-12T09:35:00Z"
  }
}
```

---

### `GET /payments/{payment_id}`

Devuelve el detalle completo de un cobro incluyendo el ticket y la póliza contable asociada.

---

### `POST /payments/{payment_id}/refund`

Genera un cobro de tipo `refund` referenciado al cobro original. Publica el evento para el reverso contable.

**Body:**
```json
{
  "reason": "Error en cobro — se duplicó el cargo",
  "refund_cents": 80000
}
```

**Errores posibles:** `409 PAYMENT_ALREADY_REFUNDED`, `422 REFUND_EXCEEDS_PAYMENT`

---

## 10. Módulo: Contabilidad (ARE + Libro Mayor)

**Nota de acceso:** los endpoints de este módulo están restringidos a los roles `admin`, `clinic_admin` y `accountant`. El rol `doctor` y `receptionist` no tienen acceso al Libro Mayor.

### `GET /accounting/chart-of-accounts`

Devuelve el Catálogo de Cuentas (CoA) de la clínica con la estructura jerárquica.

**Respuesta `200`:**
```json
{
  "data": [
    {
      "code": "10000",
      "name": "Activos",
      "account_type": "asset",
      "level": 1,
      "is_base": true,
      "is_editable": false,
      "children": [
        {
          "code": "11100",
          "name": "Caja Operativa",
          "account_type": "asset",
          "level": 2,
          "is_base": true,
          "is_editable": false,
          "balance_cents": 785000
        }
      ]
    }
  ]
}
```

---

### `POST /accounting/chart-of-accounts`

Crea una subcuenta personalizada dentro de una cuenta base existente. Las cuentas raíz base no son modificables.

**Body:**
```json
{
  "parent_code": "41000",
  "code": "41100",
  "name": "Ingresos por Ortodoncia",
  "account_type": "income"
}
```

**Errores posibles:** `409 ACCOUNT_CODE_EXISTS`, `403 BASE_ACCOUNT_NOT_EDITABLE`

---

### `GET /accounting/journal-entries`

Lista las pólizas del Libro Mayor con filtros.

**Query params:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `date_from` | `YYYY-MM-DD` | Inicio del periodo |
| `date_to` | `YYYY-MM-DD` | Fin del periodo |
| `entry_type` | `string` | Ver ENUM `entry_type` |
| `account_code` | `string` | Filtrar por cuenta contable |
| `page` | `integer` | |
| `per_page` | `integer` | Default: `25` |

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid",
      "entry_number": "POL-2025-00041",
      "entry_date": "2025-06-12",
      "entry_type": "income",
      "description": "Cobro consulta — María López Martínez",
      "source_event": "PagoRegistrado",
      "source_event_id": "uuid-payment",
      "total_debit_cents": 80000,
      "total_credit_cents": 80000,
      "is_balanced": true,
      "lines_count": 2
    }
  ]
}
```

---

### `GET /accounting/journal-entries/{entry_id}`

Devuelve el detalle completo de una póliza con sus líneas de asiento.

**Respuesta `200`:**
```json
{
  "data": {
    "id": "uuid",
    "entry_number": "POL-2025-00041",
    "entry_date": "2025-06-12",
    "entry_type": "income",
    "description": "Cobro consulta — María López Martínez",
    "source_event": "PagoRegistrado",
    "source_event_id": "uuid-payment",
    "reversal_of_entry_id": null,
    "lines": [
      {
        "id": "uuid-linea",
        "account_code": "11100",
        "account_name": "Caja Operativa",
        "debit_cents": 80000,
        "credit_cents": 0,
        "description": "Ingreso por consulta"
      },
      {
        "id": "uuid-linea-2",
        "account_code": "41000",
        "account_name": "Ingresos por Servicios Médicos",
        "debit_cents": 0,
        "credit_cents": 80000,
        "description": "Ingreso por consulta"
      }
    ],
    "total_debit_cents": 80000,
    "total_credit_cents": 80000,
    "is_balanced": true
  }
}
```

---

### `GET /accounting/domain-events`

Lista los eventos de dominio procesados y pendientes por el ARE. Útil para diagnóstico y auditoría.

**Query params:** `event_type`, `processed` (`true` | `false`), `date_from`, `date_to`, `page`, `per_page`.

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid",
      "event_type": "PagoRegistrado",
      "source_table": "cash.payments",
      "source_id": "uuid-payment",
      "payload": { "amount_cents": 80000, "pay_method": "cash" },
      "processed_at": "2025-06-12T09:35:02Z",
      "processing_error": null
    }
  ]
}
```

---

### `GET /accounting/reports/trial-balance`

Devuelve la Balanza de Comprobación para un periodo.

**Query params:** `date_from` (requerido), `date_to` (requerido).

**Respuesta `200`:**
```json
{
  "data": {
    "period": { "from": "2025-06-01", "to": "2025-06-30" },
    "accounts": [
      {
        "code": "11100",
        "name": "Caja Operativa",
        "account_type": "asset",
        "opening_balance_cents": 200000,
        "total_debit_cents": 785000,
        "total_credit_cents": 0,
        "closing_balance_cents": 985000
      }
    ],
    "totals": {
      "total_debit_cents": 1250000,
      "total_credit_cents": 1250000,
      "is_balanced": true
    }
  }
}
```

---

### `GET /accounting/reports/account-ledger`

Devuelve el auxiliar de una cuenta contable (todos sus movimientos en un periodo).

**Query params:** `account_code` (requerido), `date_from` (requerido), `date_to` (requerido).

---

## 11. Módulo: Personal y Asistencia

### `GET /staff`

Lista el personal de la clínica activa.

**Query params:** `role`, `is_active` (`true` | `false`), `page`, `per_page`.

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid",
      "user_id": "uuid-usuario",
      "full_name": "Dr. Roberto García",
      "role": "doctor",
      "role_label": "Médico",
      "professional_id": "Cédula 8765432",
      "specialty": "Medicina General",
      "is_active": true,
      "hire_date": "2023-01-15"
    }
  ]
}
```

---

### `POST /staff`

Agrega un empleado a la clínica. Si el `user_id` ya existe en el sistema (otro tenant), se vincula. Si es nuevo, se crea el usuario y se le envía invitación por correo.

**Body:**
```json
{
  "email": "nuevo.medico@clinica.com",
  "full_name": "Dra. Ana Martínez",
  "role": "doctor",
  "professional_id": "Cédula 1234567",
  "specialty": "Cardiología",
  "hire_date": "2025-07-01"
}
```

**Respuesta `201`:** objeto `data` con el registro de staff creado y `{ "invitation_sent": true }`.

---

### `PATCH /staff/{staff_id}`

Actualiza el rol, especialidad o estado activo del empleado.

---

### `DELETE /staff/{staff_id}`

Da de baja lógica al empleado de la clínica. No elimina su usuario del sistema.

**Respuesta:** `204 No Content`

---

### `POST /staff/{staff_id}/attendance`

Registra la entrada o salida del empleado.

**Body:**
```json
{
  "event_type": "check_in",
  "timestamp": "2025-06-12T08:05:00Z",
  "notes": ""
}
```

**Tipos de evento:** `check_in`, `check_out`.

**Respuesta `201`:**
```json
{
  "data": {
    "attendance_id": "uuid",
    "event_type": "check_in",
    "timestamp": "2025-06-12T08:05:00Z"
  }
}
```

---

### `GET /staff/{staff_id}/attendance`

Devuelve el historial de asistencia del empleado.

**Query params:** `date_from`, `date_to`, `page`, `per_page`.

---

## 12. Módulo: Alertas del Sistema

### `GET /alerts`

Lista las alertas activas de la clínica. Las alertas se generan automáticamente por el sistema (stock bajo, producto próximo a caducar, etc.).

**Query params:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `status` | `pending` \| `resolved` \| `dismissed` | Filtro por estado. Default: `pending` |
| `alert_type` | `string` | Tipo de alerta (ej. `low_stock`, `near_expiry`, `balance_error`) |
| `page` | `integer` | |
| `per_page` | `integer` | Default: `25` |

**Respuesta `200`:**
```json
{
  "data": [
    {
      "id": "uuid",
      "alert_type": "low_stock",
      "severity": "warning",
      "title": "Stock bajo: Anestesia lidocaína 2%",
      "message": "El stock actual (30 ml) está por debajo del mínimo configurado (50 ml).",
      "entity_type": "product",
      "entity_id": "uuid-producto",
      "status": "pending",
      "created_at": "2025-06-12T07:00:00Z",
      "metadata": { "stock_current": 30.0, "stock_min": 50.0, "unit": "ml" }
    }
  ]
}
```

---

### `POST /alerts/{alert_id}/resolve`

Marca la alerta como resuelta.

**Body:**
```json
{
  "notes": "Se realizó compra de reabastecimiento."
}
```

**Respuesta `200`:** `{ "data": { "status": "resolved", "resolved_at": "..." } }`

---

### `POST /alerts/{alert_id}/dismiss`

Descarta la alerta sin resolverla.

**Respuesta `200`:** `{ "data": { "status": "dismissed" } }`

---

## 13. Módulo: Eventos en Tiempo Real (SSE)

La agenda, las alertas y el estado de las citas se actualizan en la interfaz sin recarga mediante **Server-Sent Events (SSE)** — comunicación unidireccional servidor → cliente sobre HTTP/1.1 (Arquitectura v2.0 §3.2, ADR-003).

### `GET /events/stream`

Abre una conexión SSE persistente. El servidor emite eventos cuando ocurren cambios en la clínica activa. El cliente usa el objeto `EventSource` nativo del navegador, que reconecta automáticamente.

**Cabeceras requeridas:**
```
Authorization: Bearer <access_token>
X-Clinic-ID:   <uuid>
```

**Respuesta:**
```
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

**Formato de cada evento:**
```
event: appointment.status_changed
data: { "appointment_id": "uuid", "status": "confirmed", "patient_name": "María López Martínez" }

```

**Catálogo de eventos emitidos:**

| Evento | Disparador | Payload mínimo |
|---|---|---|
| `appointment.status_changed` | Cambio de estado de una cita | `{ appointment_id, status, patient_name }` |
| `appointment.created` | Nueva cita agendada | `{ appointment_id, scheduled_start, doctor_id }` |
| `alert.created` | Nueva alerta del sistema | `{ alert_id, alert_type, severity, title }` |
| `alert.resolved` | Alerta resuelta o descartada | `{ alert_id }` |
| `stock.low_warning` | Stock bajo el mínimo | `{ product_id, product_name, stock_current, stock_min }` |
| `payment.registered` | Cobro registrado en caja | `{ payment_id, patient_name, amount_cents }` |
| `are.processing_error` | El ARE falló al procesar un evento | `{ domain_event_id, event_type, error }` |

> **Comportamiento del cliente.** El payload del SSE es solo una **señal de invalidación**: al recibirlo, el frontend invalida la query correspondiente en TanStack Query y vuelve a consultar el REST API, que es la fuente de verdad. El servidor emite un comentario de *keep-alive* cada 30 segundos para mantener viva la conexión a través de proxies.

---

## 14. Módulo: Privacidad y Derechos ARCO (LFPDPPP)

Materializa las obligaciones de la Ley Federal de Protección de Datos Personales en Posesión de los Particulares respecto a los derechos de Acceso, Rectificación, Cancelación y Oposición del titular de los datos (Arquitectura v2.0 §2.7).

### `POST /patients/{patient_id}/arco-request`

Registra una solicitud ARCO del paciente. Genera una alerta para la clínica con el plazo legal de respuesta (20 días hábiles) y crea el registro en `patients.arco_requests`.

**Body:**
```json
{
  "request_type": "access",
  "description": "El paciente solicita copia íntegra de su expediente clínico."
}
```

**Tipos de solicitud:** `access` (Acceso), `rectification` (Rectificación), `cancellation` (Cancelación), `opposition` (Oposición).

**Respuesta `201`:**
```json
{
  "data": {
    "id": "uuid-solicitud",
    "request_type": "access",
    "status": "received",
    "legal_due_date": "2025-07-10",
    "created_at": "2025-06-12T14:30:00Z"
  }
}
```

---

### `GET /arco-requests`

Lista las solicitudes ARCO de la clínica activa. Accesible para roles `admin` y `clinic_admin`.

**Query params:** `status` (`received` | `in_progress` | `completed` | `rejected`), `request_type`, `patient_id`, `page`, `per_page`.

**Respuesta `200`:** arreglo de solicitudes con su plazo legal y estado.

---

### `PATCH /arco-requests/{request_id}`

Actualiza el estado de una solicitud ARCO y registra su resolución.

**Body:**
```json
{
  "status": "completed",
  "resolution_notes": "Se entregó copia digital del expediente al titular el 2025-06-20."
}
```

**Respuesta `200`:** objeto `data` con la solicitud actualizada, incluyendo `resolved_by` y `resolved_at`.

---

## 15. Catálogo de Errores

| Código de error | HTTP | Descripción |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Uno o más campos no pasaron validación |
| `INVALID_CREDENTIALS` | 401 | Email o contraseña incorrectos |
| `EMAIL_NOT_VERIFIED` | 403 | El usuario no ha verificado su correo |
| `ACCOUNT_LOCKED` | 403 | Cuenta bloqueada temporalmente por intentos fallidos (15 min) |
| `REFRESH_TOKEN_INVALID` | 401 | El refresh token es inválido o ha sido revocado |
| `REFRESH_TOKEN_EXPIRED` | 401 | El refresh token ha expirado |
| `REFRESH_TOKEN_REUSED` | 401 | Se detectó reuso de un refresh token rotado; se revocaron todas las sesiones |
| `RESET_TOKEN_INVALID` | 401 | El token de restablecimiento no es válido |
| `RESET_TOKEN_EXPIRED` | 401 | El token de restablecimiento ha expirado |
| `VERIFICATION_TOKEN_INVALID` | 401 | El token de verificación de correo no es válido |
| `VERIFICATION_TOKEN_EXPIRED` | 401 | El token de verificación de correo ha expirado |
| `INVITATION_INVALID` | 401 | El token de invitación no es válido |
| `INVITATION_EXPIRED` | 401 | La invitación de personal ha expirado |
| `INVITATION_ALREADY_ACCEPTED` | 409 | La invitación ya fue aceptada previamente |
| `PASSWORD_TOO_WEAK` | 422 | La contraseña no cumple los requisitos mínimos |
| `PASSWORD_REUSED` | 422 | La contraseña coincide con una de las últimas 5 utilizadas |
| `PRIVACY_CONSENT_REQUIRED` | 422 | El alta de paciente requiere la aceptación del aviso de privacidad (LFPDPPP) |
| `UNAUTHORIZED` | 401 | Token de acceso ausente o inválido |
| `FORBIDDEN` | 403 | El rol del usuario no tiene permiso para esta acción |
| `CLINIC_ACCESS_DENIED` | 403 | El usuario no tiene acceso a la clínica indicada en `X-Clinic-ID` |
| `RESOURCE_NOT_FOUND` | 404 | El recurso solicitado no existe |
| `PATIENT_NOT_FOUND` | 404 | Paciente no encontrado |
| `CURP_ALREADY_EXISTS` | 409 | Ya existe un paciente con esa CURP en la clínica |
| `PATIENT_HAS_PENDING_BALANCE` | 409 | No se puede dar de baja al paciente con saldo pendiente |
| `DOCUMENT_ALREADY_SIGNED` | 409 | El documento ya está firmado y es inmutable |
| `DOCTOR_NOT_AVAILABLE` | 409 | El médico no tiene disponibilidad en el horario solicitado |
| `APPOINTMENT_NOT_EDITABLE` | 409 | La cita está en un estado que no permite modificaciones |
| `STOCK_INSUFFICIENT` | 409 | No hay suficiente stock disponible para el procedimiento |
| `CASH_SESSION_ALREADY_OPEN` | 409 | Ya existe una sesión de caja abierta para la clínica |
| `ACCOUNT_CODE_EXISTS` | 409 | Ya existe una cuenta contable con ese código |
| `BASE_ACCOUNT_NOT_EDITABLE` | 403 | Las cuentas base del sistema no son modificables |
| `PAYMENT_ALREADY_REFUNDED` | 409 | El cobro ya fue devuelto previamente |
| `REFUND_EXCEEDS_PAYMENT` | 422 | El monto de devolución supera el cobro original |
| `JOURNAL_BALANCE_ERROR` | 422 | La póliza no cumple la validación de partida doble (Σ Débitos ≠ Σ Créditos) |
| `RATE_LIMIT_EXCEEDED` | 429 | Se superó el límite de solicitudes permitidas |
| `INTERNAL_ERROR` | 500 | Error interno no controlado |

---

## 16. Paginación y Filtros Comunes

Todos los endpoints de listado soportan los siguientes parámetros de paginación:

| Parámetro | Tipo | Default | Descripción |
|---|---|---|---|
| `page` | `integer` | `1` | Número de página (base 1) |
| `per_page` | `integer` | `25` | Resultados por página. Máximo: `100` |
| `sort_by` | `string` | Depende del endpoint | Campo de ordenamiento |
| `sort_dir` | `asc` \| `desc` | `desc` | Dirección del ordenamiento |

La respuesta siempre incluye el objeto `pagination` dentro de `meta`:

```json
"pagination": {
  "page": 2,
  "per_page": 25,
  "total_items": 87,
  "total_pages": 4,
  "has_next_page": true,
  "has_prev_page": true
}
```

### Filtros de fecha comunes

Todos los endpoints de listado que manejan registros temporales aceptan:

| Parámetro | Formato | Descripción |
|---|---|---|
| `date_from` | `YYYY-MM-DD` | Fecha de inicio (inclusive) |
| `date_to` | `YYYY-MM-DD` | Fecha de fin (inclusive) |

Si solo se envía `date_from`, se devuelven registros desde esa fecha hasta hoy. Si solo se envía `date_to`, se devuelven todos los registros hasta esa fecha.

### Búsqueda de texto libre

Los endpoints que aceptan el parámetro `q` realizan búsqueda insensible a mayúsculas y diacríticos sobre los campos principales del recurso (nombre, código, etc.). La búsqueda mínima es de **2 caracteres**; con menos de 2 caracteres el parámetro se ignora.

---

*Especificación de API REST — Sistema de Gestión de Consultorios Médicos · Fase 1 MVP · Versión 2.0 · Confidencial · Alineada con Arquitectura v2.0 y Modelo de Datos v2.0*
