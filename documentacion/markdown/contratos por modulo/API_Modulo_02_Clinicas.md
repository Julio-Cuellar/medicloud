# API — Módulo 02: Clínicas y Organizaciones
## Sistema de Gestión de Consultorios Médicos · Fase 1 MVP

---

| Campo | Detalle |
|---|---|
| Versión del documento | 1.0 |
| Fecha | Junio 2025 |
| Estado | Revisado · Listo para desarrollo |
| Audiencia | Equipo Backend · Equipo Frontend |
| Módulo | `clinics` · `organizations` |
| Prefijo de rutas | `/v1/clinics` · `/v1/organizations` |
| Tablas involucradas | `core.clinics` · `core.organizations` · `core.clinic_staff` |
| Referencias | Arquitectura v2.0 §4.1 · Modelo de Datos v2.0 (schema `core`) · UI/UX v2.0 §3.4 |
| Cambios vs v1.0 | Alineación de referencias a v2.0. Incorporación de los campos de cumplimiento LFPDPPP de la clínica (`privacy_notice_url`, `data_processor_agreed_at`) en la configuración. Flujo de onboarding actualizado al endpoint OAuth 2.0 `POST /oauth2/token`. |

---

## Tabla de contenido

1. [Descripción general](#1-descripción-general)
2. [Convenciones del módulo](#2-convenciones-del-módulo)
3. [Jerarquía de entidades](#3-jerarquía-de-entidades)
4. [Flujos cubiertos](#4-flujos-cubiertos)
5. [Reglas de acceso por rol](#5-reglas-de-acceso-por-rol)
6. [Endpoints — Clínicas](#6-endpoints--clínicas)
   - [GET /clinics](#get-clinics)
   - [POST /clinics](#post-clinics)
   - [GET /clinics/{clinic_id}](#get-clinicsclinic_id)
   - [PATCH /clinics/{clinic_id}](#patch-clinicsclinic_id)
   - [DELETE /clinics/{clinic_id}](#delete-clinicsclinic_id)
   - [POST /clinics/{clinic_id}/logo](#post-clinicsclinic_idlogo)
   - [DELETE /clinics/{clinic_id}/logo](#delete-clinicsclinic_idlogo)
   - [GET /clinics/{clinic_id}/stats](#get-clinicsclinic_idstats)
7. [Endpoints — Organizaciones](#7-endpoints--organizaciones)
   - [GET /organizations](#get-organizations)
   - [POST /organizations](#post-organizations)
   - [GET /organizations/{org_id}](#get-organizationsorg_id)
   - [PATCH /organizations/{org_id}](#patch-organizationsorg_id)
   - [POST /organizations/{org_id}/clinics/{clinic_id}](#post-organizationsorg_idclinicsclinic_id)
   - [DELETE /organizations/{org_id}/clinics/{clinic_id}](#delete-organizationsorg_idclinicsclinic_id)
8. [Catálogo de errores del módulo](#8-catálogo-de-errores-del-módulo)
9. [Notas de seguridad](#9-notas-de-seguridad)

---

## 1. Descripción general

Este módulo gestiona las dos entidades organizacionales del sistema: **clínicas** y **organizaciones**.

La **clínica** es la unidad operativa base del sistema. Todo registro — pacientes, citas, cobros, pólizas — pertenece a exactamente una clínica. El aislamiento entre clínicas es absoluto y lo garantiza el RLS de PostgreSQL a nivel de base de datos.

La **organización** es una capa opcional que agrupa varias clínicas bajo una persona moral o grupo médico. En Fase 1 la estructura existe en base de datos y la API la soporta, pero la UI corporativa (reportes consolidados, configuración cross-clínica) se implementa en Fase 2.

### Flujo de onboarding del usuario nuevo

Un usuario recién registrado que completó la verificación de correo llega sin clínicas. El flujo esperado es:

```
POST /auth/register
  → POST /auth/verify-email
    → POST /oauth2/token       (grant_type=password → clinics: [])
      → POST /clinics          (primera clínica)
        → Operación normal del sistema
```

---

## 2. Convenciones del módulo

- Todos los endpoints de este módulo requieren `Authorization: Bearer <access_token>`.
- Los endpoints de clínica específica (`/clinics/{clinic_id}/*`) requieren además `X-Clinic-ID` con el mismo `clinic_id` de la ruta. El backend valida que coincidan y que el usuario autenticado tenga un rol activo en esa clínica.
  - **Middleware BOLA/IDOR Protection**: El backend verificará en cada solicitud operativa que el `X-Clinic-ID` provisto corresponda a una membresía activa del usuario en `core.clinic_staff`. En caso negativo, denegará acceso inmediatamente devoliendo `403 CLINIC_ACCESS_DENIED`.
- La única excepción es `POST /clinics` y `GET /clinics`, que operan a nivel de usuario y no requieren `X-Clinic-ID`.
- El `owner_user_id` de una clínica u organización **nunca se acepta en el body** de ningún endpoint — siempre se extrae del `access_token` para evitar suplantación.
- Los montos se expresan en centavos enteros (`integer`). Las fechas en ISO 8601 UTC.

---

## 3. Jerarquía de entidades

```
Usuario (core.users)
  └── Organización (core.organizations)  ← opcional
        └── Clínica (core.clinics)
              └── Staff (core.clinic_staff)  ← rol del usuario en esta clínica
```

Una clínica puede existir sin organización (`organization_id = NULL`). Esto cubre el caso del médico independiente con uno o varios consultorios sin estructura corporativa.

Un usuario puede tener roles distintos en clínicas distintas. Ejemplo: `admin` en su clínica principal y `doctor` en una clínica asociada.

---

## 4. Flujos cubiertos

| # | Flujo | Endpoints |
|---|---|---|
| 1 | Crear primera clínica (onboarding) | `POST /clinics` |
| 2 | Listar todas las clínicas del usuario | `GET /clinics` |
| 3 | Consultar detalle de una clínica | `GET /clinics/{clinic_id}` |
| 4 | Actualizar configuración de la clínica | `PATCH /clinics/{clinic_id}` |
| 5 | Dar de baja lógica una clínica | `DELETE /clinics/{clinic_id}` |
| 6 | Subir o reemplazar logo de la clínica | `POST /clinics/{clinic_id}/logo` |
| 7 | Eliminar logo de la clínica | `DELETE /clinics/{clinic_id}/logo` |
| 8 | Consultar estadísticas rápidas de la clínica | `GET /clinics/{clinic_id}/stats` |
| 9 | Crear una organización (grupo médico) | `POST /organizations` |
| 10 | Listar organizaciones del usuario | `GET /organizations` |
| 11 | Consultar detalle de una organización | `GET /organizations/{org_id}` |
| 12 | Actualizar una organización | `PATCH /organizations/{org_id}` |
| 13 | Asociar una clínica a una organización | `POST /organizations/{org_id}/clinics/{clinic_id}` |
| 14 | Desasociar una clínica de una organización | `DELETE /organizations/{org_id}/clinics/{clinic_id}` |

---

## 5. Reglas de acceso por rol

| Acción | `admin` | `clinic_admin` | `doctor` | `receptionist` | `assistant` | `accountant` | `cleaning` |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Ver detalle de clínica | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Crear clínica | ✅ (dueño) | — | — | — | — | — | — |
| Editar clínica | ✅ | ✅ | — | — | — | — | — |
| Subir / eliminar logo | ✅ | ✅ | — | — | — | — | — |
| Dar de baja clínica | ✅ (dueño) | — | — | — | — | — | — |
| Ver estadísticas de clínica | ✅ | ✅ | ✅ | ✅ | — | ✅ | — |
| Gestionar organizaciones | ✅ (dueño) | — | — | — | — | — | — |

> La columna `admin (dueño)` indica que, además del rol, el usuario debe ser el `owner_user_id` del recurso para acciones destructivas.

---

## 6. Endpoints — Clínicas

---

### `GET /clinics`

**Acceso:** Privado · Sin `X-Clinic-ID`  
**Descripción:** Lista todas las clínicas activas a las que el usuario autenticado pertenece (tiene un registro activo en `core.clinic_staff`). Este endpoint alimenta el selector de clínica del sidebar y el objeto `clinics` que devuelve `GET /auth/me`.

> ⚠️ **Seguridad:** el backend filtra estrictamente por `user_id` extraído del token. No es posible ver clínicas de otros usuarios aunque se conozca su UUID.

**Respuesta `200`:**

```json
{
  "data": [
    {
      "id": "uuid-clinica-1",
      "name": "Clínica Reforma",
      "address_city": "Ciudad de México",
      "address_state": "CDMX",
      "phone": "+52 55 5555 1234",
      "specialties": ["Medicina General", "Cardiología"],
      "logo_url": "https://storage.medicloud.mx/logos/uuid.png",
      "timezone": "America/Mexico_City",
      "is_active": true,
      "role": "admin",
      "role_label": "Administrador",
      "organization": {
        "id": "uuid-org",
        "name": "Grupo Médico Reforma"
      }
    },
    {
      "id": "uuid-clinica-2",
      "name": "Sucursal Norte",
      "address_city": "Monterrey",
      "address_state": "Nuevo León",
      "phone": null,
      "specialties": ["Odontología"],
      "logo_url": null,
      "timezone": "America/Monterrey",
      "is_active": true,
      "role": "doctor",
      "role_label": "Médico",
      "organization": null
    }
  ],
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T08:00:00Z"
  }
}
```

> **Nota de negocio:** si el arreglo es vacío (`[]`), el frontend muestra el flujo de onboarding de primera clínica. No se devuelve `404` — un usuario sin clínicas es un estado válido del sistema.

---

### `POST /clinics`

**Acceso:** Privado · Sin `X-Clinic-ID`  
**Descripción:** Crea una nueva clínica. El usuario autenticado queda automáticamente registrado como `admin` (dueño) en `core.clinic_staff`. No es necesario agregar un paso posterior para vincularse. Si se provee `organization_id`, se valida que el usuario autenticado sea dueño de esa organización.

> ⚠️ **Seguridad:** el campo `owner_user_id` se extrae del `access_token`, nunca del body. Cualquier `owner_user_id` enviado en el body es ignorado.

**Body:**

```json
{
  "name": "Clínica Reforma",
  "address_street": "Av. Reforma 500, Col. Juárez",
  "address_city": "Ciudad de México",
  "address_state": "CDMX",
  "address_zip": "06600",
  "phone": "+52 55 5555 1234",
  "specialties": ["Medicina General", "Cardiología"],
  "timezone": "America/Mexico_City",
  "organization_id": "uuid-org-opcional"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `name` | `string` | ✅ | Nombre comercial del consultorio. Máximo 200 caracteres. |
| `address_street` | `string` | ✅ | Calle y número. Máximo 300 caracteres. |
| `address_city` | `string` | ✅ | Ciudad. Máximo 100 caracteres. |
| `address_state` | `string` | ✅ | Estado. Máximo 100 caracteres. |
| `address_zip` | `string` | ✅ | Código postal. Máximo 10 caracteres. |
| `phone` | `string` | ✗ | Teléfono de la clínica. Máximo 20 caracteres. |
| `specialties` | `string[]` | ✗ | Arreglo de especialidades. Máximo 20 elementos. Default: `[]`. |
| `timezone` | `string` | ✗ | Zona horaria IANA. Default: `America/Mexico_City`. |
| `organization_id` | `UUID` | ✗ | ID de la organización a la que pertenece. El usuario autenticado debe ser dueño de esa organización. |

**Comportamiento del sistema tras crear la clínica:**
1. Se crea el registro en `core.clinics` con `owner_user_id` del token.
2. Se crea el registro en `core.clinic_staff` con `role = admin` para el usuario autenticado.
3. Se inicializa el Catálogo de Cuentas base (CoA) del ARE para la nueva clínica, copiando la estructura estándar no modificable.

**Respuesta `201`:**

```json
{
  "data": {
    "id": "uuid-nueva-clinica",
    "name": "Clínica Reforma",
    "address_street": "Av. Reforma 500, Col. Juárez",
    "address_city": "Ciudad de México",
    "address_state": "CDMX",
    "address_zip": "06600",
    "phone": "+52 55 5555 1234",
    "specialties": ["Medicina General", "Cardiología"],
    "logo_url": null,
    "timezone": "America/Mexico_City",
    "is_active": true,
    "owner_user_id": "uuid-usuario",
    "organization_id": null,
    "created_at": "2025-06-12T10:00:00Z"
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T10:00:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Campos requeridos faltantes o con formato inválido. |
| `INVALID_TIMEZONE` | 422 | El valor de `timezone` no es un identificador IANA válido. |
| `ORGANIZATION_NOT_FOUND` | 404 | El `organization_id` no existe. |
| `ORGANIZATION_ACCESS_DENIED` | 403 | El usuario no es dueño de la organización indicada. |

---

### `GET /clinics/{clinic_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID`  
**Descripción:** Devuelve la configuración completa de una clínica. Accesible para todos los roles con membresía activa en la clínica.

> ⚠️ **Seguridad:** el backend valida que el `clinic_id` de la ruta coincida con el `X-Clinic-ID` de la cabecera y que el usuario tenga un rol activo en esa clínica. Un UUID de clínica válido pero no perteneciente al usuario devuelve `403 CLINIC_ACCESS_DENIED`, no `404`, para no revelar la existencia del recurso.

**Respuesta `200`:**

```json
{
  "data": {
    "id": "uuid-clinica",
    "name": "Clínica Reforma",
    "legal_name": null,
    "rfc": null,
    "tax_regime_code": null,
    "address_street": "Av. Reforma 500, Col. Juárez",
    "address_city": "Ciudad de México",
    "address_state": "CDMX",
    "address_zip": "06600",
    "phone": "+52 55 5555 1234",
    "email": "contacto@clinicareforma.com",
    "specialties": ["Medicina General", "Cardiología"],
    "logo_url": "https://storage.medicloud.mx/logos/uuid.png",
    "timezone": "America/Mexico_City",
    "privacy_notice_url": "https://clinicareforma.com/aviso-privacidad",
    "data_processor_agreed_at": "2025-01-15T10:05:00Z",
    "is_active": true,
    "owner_user_id": "uuid-usuario",
    "organization": {
      "id": "uuid-org",
      "name": "Grupo Médico Reforma"
    },
    "viewer_role": "admin",
    "viewer_role_label": "Administrador",
    "created_at": "2025-01-15T10:00:00Z",
    "updated_at": "2025-06-01T09:00:00Z"
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T08:05:00Z"
  }
}
```

> **Nota:** `legal_name`, `rfc` y `tax_regime_code` se reservan para Fase 3 (CFDI). En Fase 1 siempre devuelven `null`.  
> `viewer_role` indica el rol del usuario autenticado en esta clínica, útil para que el frontend decida qué controles mostrar.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `CLINIC_ACCESS_DENIED` | 403 | El usuario no tiene rol activo en esta clínica o el UUID no existe. |

---

### `PATCH /clinics/{clinic_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`  
**Descripción:** Actualiza la configuración operativa de la clínica. Solo se modifican los campos enviados (PATCH parcial). Los campos reservados para Fase 3 (`legal_name`, `rfc`, `tax_regime_code`) se aceptan en el body pero se almacenan sin efecto operativo hasta esa fase.

**Body (todos los campos son opcionales):**

```json
{
  "name": "Clínica Reforma Norte",
  "address_street": "Av. Insurgentes Norte 100",
  "address_city": "Ciudad de México",
  "address_state": "CDMX",
  "address_zip": "07300",
  "phone": "+52 55 5555 9999",
  "email": "norte@clinicareforma.com",
  "specialties": ["Medicina General", "Dermatología"],
  "timezone": "America/Mexico_City",
  "legal_name": "Servicios Médicos Reforma SA de CV",
  "rfc": "SMR200101ABC"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `name` | `string` | Nombre comercial. Máximo 200 caracteres. |
| `address_street` | `string` | Calle y número. Máximo 300 caracteres. |
| `address_city` | `string` | Ciudad. Máximo 100 caracteres. |
| `address_state` | `string` | Estado. Máximo 100 caracteres. |
| `address_zip` | `string` | Código postal. Máximo 10 caracteres. |
| `phone` | `string` | Teléfono. Máximo 20 caracteres. |
| `email` | `string` | Correo de contacto de la clínica. |
| `specialties` | `string[]` | Arreglo completo de especialidades (reemplaza el anterior). Máximo 20 elementos. |
| `timezone` | `string` | Zona horaria IANA. |
| `legal_name` | `string` | Razón social (Fase 3). Máximo 300 caracteres. |
| `rfc` | `string` | RFC de la clínica (Fase 3). Máximo 13 caracteres. |
| `privacy_notice_url` | `string` | URL del aviso de privacidad de la clínica (LFPDPPP). Se muestra al paciente al registrar su consentimiento. |

> **Cumplimiento LFPDPPP (Arquitectura v2.0 §2.7).** La clínica configura aquí la URL de su aviso de privacidad (`privacy_notice_url`). La aceptación del Acuerdo de Encargado de Datos (`data_processor_agreed_at`) se registra automáticamente al aceptar los Términos de Servicio durante el alta de la clínica y **no** es editable por este endpoint.

**Respuesta `200`:** objeto `data` con la clínica actualizada completa (mismo esquema que `GET /clinics/{clinic_id}`).

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `CLINIC_ACCESS_DENIED` | 403 | El usuario no tiene rol activo en esta clínica. |
| `FORBIDDEN` | 403 | El rol del usuario no permite editar la clínica (`doctor`, `receptionist`, etc.). |
| `VALIDATION_ERROR` | 422 | Campos con formato o longitud inválidos. |
| `INVALID_TIMEZONE` | 422 | Zona horaria no reconocida como identificador IANA. |

---

### `DELETE /clinics/{clinic_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Solo `owner_user_id` del recurso  
**Descripción:** Realiza la baja lógica de la clínica (`is_active = false`). El registro y todos sus datos se conservan para auditoría. Esta acción es irreversible desde la UI — solo puede revertirse por soporte técnico.

> ⚠️ **Seguridad:** solo el `owner_user_id` de la clínica puede darla de baja. El rol `admin` en `clinic_staff` no es suficiente si no es el dueño original.

**Precondiciones — el sistema rechaza la baja si:**

| Condición | Código de error |
|---|---|
| Hay una sesión de caja abierta | `CLINIC_HAS_OPEN_CASH_SESSION` |
| Hay citas en estado `confirmed` o `in_progress` hoy o en el futuro | `CLINIC_HAS_ACTIVE_APPOINTMENTS` |
| Hay pacientes con saldo pendiente (`pending_balance_cents > 0`) | `CLINIC_HAS_PENDING_BALANCES` |
| Hay pólizas en el ARE con errores pendientes de resolución | `CLINIC_HAS_UNRESOLVED_ACCOUNTING_ERRORS` |

**Body:**

```json
{
  "reason": "Cierre definitivo del consultorio."
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `reason` | `string` | ✅ | Motivo de la baja. Se registra en `audit.audit_log`. Mínimo 10 caracteres. |

**Respuesta:** `204 No Content`

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `FORBIDDEN` | 403 | El usuario autenticado no es el `owner_user_id` de la clínica. |
| `CLINIC_HAS_OPEN_CASH_SESSION` | 409 | Hay una sesión de caja abierta. |
| `CLINIC_HAS_ACTIVE_APPOINTMENTS` | 409 | Hay citas activas futuras. |
| `CLINIC_HAS_PENDING_BALANCES` | 409 | Hay pacientes con saldo pendiente. |
| `CLINIC_HAS_UNRESOLVED_ACCOUNTING_ERRORS` | 409 | Hay errores contables pendientes en el ARE. |

---

### `POST /clinics/{clinic_id}/logo`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`  
**Descripción:** Sube o reemplaza el logo de la clínica. El archivo anterior se elimina del object storage. El sistema genera una versión redimensionada a **400×400 px** para uso en la interfaz y una versión original para documentos PDF futuros.

**Content-Type:** `multipart/form-data`

**Form fields:**

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `file` | `file` | ✅ | Imagen del logo. Tamaño máximo: **2 MB**. |

**Formatos aceptados:** JPG, PNG, WebP, SVG.

> ⚠️ **Seguridad:** la validación del tipo de archivo se realiza sobre el **tipo MIME real** del contenido binario (magic bytes), no solo sobre la extensión del nombre. Para SVG se realiza adicionalmente sanitización del contenido para remover scripts embebidos (`<script>`, `on*` handlers, `javascript:` en `href`). Para mitigar ataques XSS, se almacenan en un bucket de assets aislado del dominio principal y se renderizan en el frontend **únicamente** a través de la etiqueta `<img>` (`<img src="..." />`), deshabilitando la ejecución de scripts del navegador.

**Respuesta `200`:**

```json
{
  "data": {
    "logo_url": "https://storage.medicloud.mx/logos/uuid_400.png",
    "logo_url_original": "https://storage.medicloud.mx/logos/uuid_original.png"
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T10:15:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `FORBIDDEN` | 403 | El rol del usuario no permite subir logo. |
| `FILE_TOO_LARGE` | 422 | El archivo supera los 2 MB. |
| `INVALID_FILE_TYPE` | 422 | El tipo MIME real no es aceptado. |

---

### `DELETE /clinics/{clinic_id}/logo`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`  
**Descripción:** Elimina el logo de la clínica del object storage y establece `logo_url = NULL` en `core.clinics`. La interfaz mostrará las iniciales del nombre de la clínica como fallback.

**Respuesta:** `204 No Content`

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `FORBIDDEN` | 403 | El rol del usuario no permite eliminar logo. |
| `CLINIC_NO_LOGO` | 404 | La clínica no tiene logo configurado. |

---

### `GET /clinics/{clinic_id}/stats`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `receptionist`, `accountant`  
**Descripción:** Devuelve estadísticas operativas rápidas de la clínica para el dashboard de inicio. Los valores son aproximados y se calculan en tiempo real sobre los registros activos.

**Respuesta `200`:**

```json
{
  "data": {
    "clinic_id": "uuid-clinica",
    "today": {
      "appointments_total": 12,
      "appointments_confirmed": 8,
      "appointments_completed": 3,
      "appointments_cancelled": 1,
      "revenue_cents": 450000
    },
    "active_totals": {
      "patients": 347,
      "staff_members": 6,
      "products_low_stock": 3,
      "products_near_expiry": 1,
      "pending_alerts": 4
    },
    "cash_session": {
      "is_open": true,
      "opened_at": "2025-06-12T08:00:00Z",
      "current_balance_cents": 785000
    }
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T14:00:00Z"
  }
}
```

> **Nota:** `cash_session` es `null` si no hay sesión de caja abierta. Los campos de `today` usan la zona horaria configurada en `core.clinics.timezone`.
>
> ⚠️ **Control de Acceso de Roles (Fuga de Información):** Para los roles `doctor` y `receptionist`, los campos `revenue_cents` (dentro de `today`) y el objeto `cash_session` completo **son excluidos de la respuesta (devuelven `null` o son omitidos)** para evitar la exposición de datos financieros consolidados al personal no administrativo.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `CLINIC_ACCESS_DENIED` | 403 | El usuario no tiene rol activo en esta clínica. |
| `FORBIDDEN` | 403 | El rol del usuario no tiene acceso a estadísticas (`assistant`, `cleaning`). |

---

## 7. Endpoints — Organizaciones

---

### `GET /organizations`

**Acceso:** Privado · Sin `X-Clinic-ID`  
**Descripción:** Lista las organizaciones de las que el usuario autenticado es `owner_user_id`. En Fase 1 un usuario solo puede ser dueño de una organización, pero la API soporta múltiples para no limitar el escalamiento.

**Respuesta `200`:**

```json
{
  "data": [
    {
      "id": "uuid-org",
      "name": "Grupo Médico Reforma",
      "rfc": "GMR200101XYZ",
      "logo_url": "https://storage.medicloud.mx/org-logos/uuid.png",
      "is_active": true,
      "clinics_count": 3,
      "created_at": "2025-01-10T10:00:00Z"
    }
  ],
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T08:00:00Z"
  }
}
```

---

### `POST /organizations`

**Acceso:** Privado · Sin `X-Clinic-ID`  
**Descripción:** Crea una nueva organización (grupo médico o persona moral). El usuario autenticado queda como `owner_user_id`. No vincula clínicas en este paso — la vinculación se hace con `POST /organizations/{org_id}/clinics/{clinic_id}`.

> ⚠️ **Seguridad:** igual que en clínicas, el `owner_user_id` se extrae del token y nunca se acepta en el body.

**Body:**

```json
{
  "name": "Grupo Médico Reforma",
  "rfc": "GMR200101XYZ"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `name` | `string` | ✅ | Razón social o nombre comercial del grupo. Máximo 200 caracteres. |
| `rfc` | `string` | ✗ | RFC de la persona moral. Máximo 13 caracteres. Debe ser único si se provee. |

**Respuesta `201`:**

```json
{
  "data": {
    "id": "uuid-nueva-org",
    "name": "Grupo Médico Reforma",
    "rfc": "GMR200101XYZ",
    "logo_url": null,
    "is_active": true,
    "owner_user_id": "uuid-usuario",
    "created_at": "2025-06-12T10:00:00Z"
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T10:00:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Campos con formato o longitud inválidos. |
| `RFC_ALREADY_EXISTS` | 409 | El RFC ya está registrado en otra organización. |

---

### `GET /organizations/{org_id}`

**Acceso:** Privado · Sin `X-Clinic-ID` · Solo `owner_user_id`  
**Descripción:** Devuelve el detalle completo de la organización con la lista de sus clínicas asociadas.

> ⚠️ **Seguridad:** solo el `owner_user_id` puede ver el detalle de una organización. Un `org_id` válido pero no propiedad del usuario devuelve `403 FORBIDDEN`, no `404`.

**Respuesta `200`:**

```json
{
  "data": {
    "id": "uuid-org",
    "name": "Grupo Médico Reforma",
    "rfc": "GMR200101XYZ",
    "logo_url": "https://storage.medicloud.mx/org-logos/uuid.png",
    "is_active": true,
    "owner_user_id": "uuid-usuario",
    "created_at": "2025-01-10T10:00:00Z",
    "clinics": [
      {
        "id": "uuid-clinica-1",
        "name": "Clínica Reforma",
        "address_city": "Ciudad de México",
        "is_active": true
      },
      {
        "id": "uuid-clinica-2",
        "name": "Sucursal Norte",
        "address_city": "Monterrey",
        "is_active": true
      }
    ]
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T08:10:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `FORBIDDEN` | 403 | El usuario no es dueño de esta organización o no existe. |

---

### `PATCH /organizations/{org_id}`

**Acceso:** Privado · Sin `X-Clinic-ID` · Solo `owner_user_id`  
**Descripción:** Actualiza los datos de la organización. Solo el dueño puede modificarla.

**Body (todos los campos son opcionales):**

```json
{
  "name": "Grupo Médico Reforma SA de CV",
  "rfc": "GMR200101XYZ"
}
```

**Respuesta `200`:** objeto `data` con la organización actualizada completa (mismo esquema que `GET /organizations/{org_id}`).

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `FORBIDDEN` | 403 | El usuario no es dueño de esta organización. |
| `RFC_ALREADY_EXISTS` | 409 | El nuevo RFC ya pertenece a otra organización. |
| `VALIDATION_ERROR` | 422 | Campos con formato o longitud inválidos. |

---

### `POST /organizations/{org_id}/clinics/{clinic_id}`

**Acceso:** Privado · Sin `X-Clinic-ID` · Solo `owner_user_id` de ambos recursos  
**Descripción:** Asocia una clínica existente a una organización. La clínica actualiza su `organization_id`. El usuario autenticado debe ser dueño tanto de la organización como de la clínica.

**Body:** vacío.

**Respuesta `200`:**

```json
{
  "data": {
    "organization_id": "uuid-org",
    "clinic_id": "uuid-clinica",
    "clinic_name": "Clínica Reforma",
    "associated_at": "2025-06-12T10:20:00Z"
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T10:20:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `FORBIDDEN` | 403 | El usuario no es dueño de la organización o de la clínica. |
| `CLINIC_ALREADY_IN_ORGANIZATION` | 409 | La clínica ya está asociada a esta u otra organización. |

---

### `DELETE /organizations/{org_id}/clinics/{clinic_id}`

**Acceso:** Privado · Sin `X-Clinic-ID` · Solo `owner_user_id` de ambos recursos  
**Descripción:** Desasocia una clínica de la organización. La clínica queda independiente (`organization_id = NULL`). Los datos operativos de la clínica no se modifican.

**Respuesta:** `204 No Content`

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `FORBIDDEN` | 403 | El usuario no es dueño de la organización o de la clínica. |
| `CLINIC_NOT_IN_ORGANIZATION` | 404 | La clínica no está asociada a esta organización. |

---

## 8. Catálogo de errores del módulo

| Código | HTTP | Descripción |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Uno o más campos no pasaron validación de formato. |
| `INVALID_TIMEZONE` | 422 | El valor de `timezone` no es un identificador IANA válido. |
| `CLINIC_ACCESS_DENIED` | 403 | El usuario no tiene rol activo en la clínica o el UUID no existe. |
| `FORBIDDEN` | 403 | El rol del usuario no permite esta acción o no es dueño del recurso. |
| `ORGANIZATION_NOT_FOUND` | 404 | El `organization_id` no existe en el sistema. |
| `ORGANIZATION_ACCESS_DENIED` | 403 | El usuario no es dueño de la organización indicada. |
| `RFC_ALREADY_EXISTS` | 409 | El RFC ya está registrado en otra organización. |
| `CLINIC_ALREADY_IN_ORGANIZATION` | 409 | La clínica ya pertenece a una organización. |
| `CLINIC_NOT_IN_ORGANIZATION` | 404 | La clínica no está asociada a esta organización. |
| `CLINIC_HAS_OPEN_CASH_SESSION` | 409 | No se puede dar de baja: hay una sesión de caja abierta. |
| `CLINIC_HAS_ACTIVE_APPOINTMENTS` | 409 | No se puede dar de baja: hay citas activas o futuras. |
| `CLINIC_HAS_PENDING_BALANCES` | 409 | No se puede dar de baja: hay pacientes con saldo pendiente. |
| `CLINIC_HAS_UNRESOLVED_ACCOUNTING_ERRORS` | 409 | No se puede dar de baja: hay errores contables pendientes. |
| `CLINIC_NO_LOGO` | 404 | La clínica no tiene logo configurado. |
| `FILE_TOO_LARGE` | 422 | El archivo supera el tamaño máximo permitido (2 MB). |
| `INVALID_FILE_TYPE` | 422 | El tipo MIME real del archivo no es aceptado. |

---

## 9. Notas de seguridad

| # | Decisión | Justificación |
|---|---|---|
| 1 | `owner_user_id` siempre se extrae del token, nunca del body | Previene que un usuario cree o modifique recursos a nombre de otro usuario. |
| 2 | `GET /clinics` filtra estrictamente por membresía activa en `clinic_staff` | Un usuario no puede ver clínicas a las que no pertenece, aunque conozca el UUID. |
| 3 | UUID de clínica no accesible devuelve `403`, no `404` | Previene enumeración de recursos — un atacante no puede distinguir si una clínica existe o si simplemente no tiene acceso. |
| 4 | `clinic_id` de la ruta debe coincidir con `X-Clinic-ID` del header y es validado contra memberships en DB | Previene que se use una cabecera de una clínica ajena (BOLA / IDOR) o que se crucen tokens entre clínicas. |
| 5 | Baja de clínica con precondiciones estrictas (caja, citas, saldos, errores ARE) | Previene dejar datos en estado inconsistente que no pueda auditarse correctamente. |
| 6 | Baja de clínica solo permitida al `owner_user_id`, no a cualquier `admin` | Un `clinic_admin` delegado no puede destruir el consultorio de otro — solo el dueño original. |
| 7 | SVG sanitizado antes de almacenar, servido desde dominio aislado y renderizado vía `<img>` | Evita ataques de Cross-Site Scripting (XSS) almacenado al impedir la ejecución de scripts en el navegador del cliente. |
| 8 | Validación de MIME real (magic bytes) en uploads de logo | Igual que en avatar — previene subida de archivos maliciosos camuflados con extensión de imagen. |
| 9 | `organization_id` en `POST /clinics` valida que el usuario sea dueño de la org | Previene que un usuario vincule una clínica a una organización ajena. |
| 10 | `viewer_role` en `GET /clinics/{clinic_id}` expone el rol del solicitante | El frontend puede ocultar o deshabilitar controles según el rol sin hacer una segunda solicitud. |
| 11 | Restricción de campos financieros en `/stats` para personal operativo | Evita fugas de información financiera a empleados no autorizados (`doctor`, `receptionist`). |

---

*API — Módulo 02: Clínicas y Organizaciones · Sistema de Gestión de Consultorios Médicos · Fase 1 MVP · v2.0 · Confidencial · Alineado con Arquitectura v2.0 y Modelo de Datos v2.0*
