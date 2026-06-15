# API — Módulo 04: Expediente Clínico
## Sistema de Gestión de Consultorios Médicos · Fase 1 MVP

---

| Campo | Detalle |
|---|---|
| Versión del documento | 2.1 |
| Fecha | 2026-06-14 |
| Estado | Revisado · Listo para desarrollo — Alineado con Arquitectura v2.1 y Modelo de Datos v2.1 |
| Audiencia | Equipo Backend · Equipo Frontend |
| Módulo | `clinical` |
| Prefijo de rutas | `/v1/medical-records` · `/v1/clinical-documents` · `/v1/clinical-templates` |
| Tablas involucradas | `clinical.medical_records` · `clinical.clinical_documents` · `clinical.document_signatures` · `clinical.clinical_histories` · `clinical.doc_templates` · `clinical.clinical_photos` · `core.doctor_profiles` |
| Referencias | Arquitectura v2.1 §2.7 §4.3 · Modelo de Datos v2.1 (schema `clinical`) · NOM-004-SSA3-2012 · ADR-005 |
| Cambios vs v2.0 | Agrega validación de `credential_status` pre-firma; snapshot de cédula (`cedula_snapshot`) en `document_signatures` por cumplimiento NOM-004-SSA3-2012; nuevos errores `DOCTOR_CREDENTIAL_SUSPENDED` y `DOCTOR_CREDENTIAL_REQUIRED`. |

---

## Tabla de contenido

1. [Descripción general](#1-descripción-general)
2. [Estructura del Canvas y Bloques Dinámicos](#2-estructura-del-canvas-y-bloques-dinámicos)
3. [Flujos cubiertos](#3-flujos-cubiertos)
4. [Reglas de acceso por rol (RBAC)](#4-reglas-de-acceso-por-rol-rbac)
5. [Endpoints](#5-endpoints)
   - [GET /patients/{patient_id}/medical-record](#get-patientspatient_idmedical-record)
   - [GET /medical-records/{record_id}/documents](#get-medical-recordsrecord_iddocuments)
   - [POST /medical-records/{record_id}/documents](#post-medical-recordsrecord_iddocuments)
   - [GET /clinical-documents/{document_id}](#get-clinical-documentsdocument_id)
   - [PATCH /clinical-documents/{document_id}](#patch-clinical-documentsdocument_id)
   - [POST /clinical-documents/{document_id}/sign](#post-clinical-documentsdocument_idsign)
   - [POST /clinical-documents/{document_id}/photos](#post-clinical-documentsdocument_idphotos)
   - [GET /clinical-templates](#get-clinical-templates)
   - [POST /clinical-templates](#post-clinical-templates)
6. [Catálogo de errores del módulo](#6-catálogo-de-errores-del-módulo)
7. [Notas de seguridad](#7-notas-de-seguridad)

---

## 1. Descripción general

Este módulo gobierna el expediente clínico y todos los documentos relacionados del paciente, asegurando la inmutabilidad y confidencialidad requerida por la norma **NOM-004-SSA3-2012**.

El expediente clínico es el contenedor raíz (`clinical.medical_records`) y tiene una relación `1:1` con cada paciente. Dentro de él se agrupan múltiples documentos (`clinical.clinical_documents`) de diversos tipos. 

Un documento se crea inicialmente en estado Borrador (`status = draft`). Mientras permanezca en este estado, el personal autorizado puede realizar modificaciones parciales. Una vez que se registra al menos una firma digital (`POST /sign`), el documento cambia a estado Firmado (`status = signed`) y su `content_json` es bloqueado de forma inmutable mediante un **Hash criptográfico (SHA-256)**.

> **Encriptación en reposo (ADR-005).** Las columnas `content_json`, `content_html` (contenido del Canvas) y `signature_url` (firmas) se almacenan **encriptadas con AES-256-GCM** a nivel de aplicación; el JSON en claro solo existe en memoria durante el procesamiento del request. En la API, el cliente sigue enviando y recibiendo `content_json` como objeto JSON normal — el cifrado/descifrado es transparente y ocurre en el backend. Como consecuencia, **el contenido clínico no es buscable ni indexable** en base de datos: las búsquedas de documentos operan solo sobre campos no sensibles (`title`, `doc_type`, `created_at`, número de expediente).

---

## 2. Estructura del Canvas y Bloques Dinámicos

La creación y modificación de documentos clínicos utiliza una interfaz de Canvas basada en bloques. En la API, esto se representa a través de la columna `content_json` con el siguiente esquema estructurado de bloques:

```json
{
  "blocks": [
    {
      "id": "uuid-bloque-1",
      "type": "text",
      "label": "Motivo de consulta",
      "value": "Paciente refiere dolor severo en el molar inferior derecho..."
    },
    {
      "id": "uuid-bloque-2",
      "type": "vitals_table",
      "label": "Signos Vitales",
      "value": {
        "bp_systolic": 120,
        "bp_diastolic": 80,
        "heart_rate": 72,
        "temp_c": 36.5,
        "weight_kg": 72.0,
        "height_cm": 170.0,
        "bmi": 24.9
      }
    },
    {
      "id": "uuid-bloque-3",
      "type": "checkbox_group",
      "label": "Antecedentes Heredofamiliares",
      "value": [
        { "name": "diabetes", "checked": true, "notes": "Abuela materna" },
        { "name": "hta", "checked": false }
      ]
    }
  ]
}
```

### Tipos de Bloque Soportados:
1. `text`: Cuadro de texto libre (cadenas string largas).
2. `vitals_table`: Tabla preestructurada con campos de validación numérica de signos vitales (presión, frecuencia, peso, altura, temperatura).
3. `checkbox_group`: Grupo de casillas para interrogatorio de antecedentes heredofamiliares o patológicos.
4. `image_block`: Contenedor para fotos clínicas o estudios referenciados de `clinical.clinical_photos`.
5. `signature_block`: Campo que define el requerimiento de firma del médico, paciente o testigo.

---

## 3. Flujos cubiertos

| # | Flujo | Endpoints |
|---|---|---|
| 1 | Consultar expediente estructurado por tipo | `GET /patients/{patient_id}/medical-record` |
| 2 | Listar documentos clínicos filtrados | `GET /medical-records/{record_id}/documents` |
| 3 | Crear nuevo documento clínico (Canvas) | `POST /medical-records/{record_id}/documents` |
| 4 | Consultar contenido y firmas de documento | `GET /clinical-documents/{document_id}` |
| 5 | Modificar borrador no firmado | `PATCH /clinical-documents/{document_id}` |
| 6 | Firmar digitalmente y bloquear documento | `POST /clinical-documents/{document_id}/sign` |
| 7 | Subir fotografía o estudio al documento | `POST /clinical-documents/{document_id}/photos` |
| 8 | Listar plantillas de Canvas | `GET /clinical-templates` |
| 9 | Crear plantilla personalizada | `POST /clinical-templates` |

---

## 4. Reglas de acceso por rol (RBAC)

El acceso al expediente clínico es sumamente restrictivo para proteger la privacidad de los pacientes:

- **Recepcionistas y Administradores de Clínica** tienen denegado cualquier acceso (`403 Forbidden`).
- **Contadores** tienen acceso a los listados (`GET`) de manera que puedan validar las pólizas y procedimientos facturados, pero los detalles médicos (`content_json` del diagnóstico) se omiten en sus respuestas.
- **Asistentes** pueden ver y editar borradores, pero no están autorizados a realizar firmas médicas.

| Endpoint / Acción | `admin` | `clinic_admin` | `doctor` | `receptionist` | `assistant` | `accountant` | `cleaning` |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Ver carpeta expediente | ✅ | — | ✅ | — | ✅ | ✅ (parcial) | — |
| Listar documentos | ✅ | — | ✅ | — | ✅ | ✅ (parcial) | — |
| Crear documento | ✅ | — | ✅ | — | ✅ | — | — |
| Ver documento completo | ✅ | — | ✅ | — | ✅ | — | — |
| Editar borrador | ✅ | — | ✅ | — | ✅ | — | — |
| Firmar documento | ✅ | — | ✅ | — | — | — | — |
| Subir fotos | ✅ | — | ✅ | — | ✅ | — | — |
| Gestionar plantillas | ✅ | — | ✅ | — | ✅ (lectura) | — | — |

---

## 5. Endpoints

---

### `GET /patients/{patient_id}/medical-record`

**Acceso:** Privado · Requiere `X-Clinic-ID`  
**Descripción:** Devuelve el expediente raíz del paciente (`medical_records.id`) junto con el resumen clasificado de todos sus documentos para alimentar el árbol del expediente en la UI.

**Respuesta `200` (Para Médicos / Admins / Asistentes):**

```json
{
  "data": {
    "id": "b3c2a1e0-d9c8-4b7a-6a5f-4e3d2c1b0a9f",
    "patient_id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
    "record_number": "EXP-2025-00042",
    "status": "active",
    "opened_at": "2025-06-12",
    "opened_by_name": "Dr. Roberto García",
    "documents": {
      "clinical_history": [
        { "id": "uuid-doc-1", "title": "Historia Clínica Inicial", "status": "signed", "created_at": "2025-06-12T14:30:00Z" }
      ],
      "medical_notes": [
        { "id": "uuid-doc-2", "title": "Nota de evolución - 13 Jun", "status": "draft", "created_at": "2025-06-13T10:00:00Z" }
      ],
      "informed_consents": [],
      "prescriptions": [],
      "clinical_photos": []
    }
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-13T10:05:00Z"
  }
}
```

**Respuesta `200` (Para Contadores - Sin contenido clínico):**

```json
{
  "data": {
    "id": "b3c2a1e0-d9c8-4b7a-6a5f-4e3d2c1b0a9f",
    "patient_id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
    "record_number": "EXP-2025-00042",
    "status": "active",
    "opened_at": "2025-06-12",
    "opened_by_name": "Dr. Roberto García"
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-13T10:05:00Z"
  }
}
```

---

### `GET /medical-records/{record_id}/documents`

**Acceso:** Privado · Requiere `X-Clinic-ID`  
**Descripción:** Retorna la lista de documentos clínicos dentro de una carpeta de expediente.

**Query Parameters:**

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `doc_type` | `string` | ✗ | Filtrar por tipo (ej. `clinical_history`, `medical_note`, `prescription`, `informed_consent`). |
| `status` | `string` | ✗ | `draft` \| `signed` \| `cancelled`. |
| `page` | `integer` | ✗ | Número de página (Default: 1). |
| `per_page` | `integer` | ✗ | Resultados por página (Default: 20). |

**Respuesta `200`:**

```json
{
  "data": [
    {
      "id": "uuid-doc-1",
      "doc_type": "medical_note",
      "title": "Nota de evolución - 13 Jun",
      "status": "draft",
      "created_by_name": "Dr. Roberto García",
      "created_at": "2025-06-13T10:00:00Z",
      "is_signed": false
    }
  ],
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "pagination": { "page": 1, "per_page": 20, "total_items": 1, "total_pages": 1 }
  }
}
```

---

### `POST /medical-records/{record_id}/documents`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `doctor`, `assistant`  
**Descripción:** Crea un nuevo documento clínico. El backend valida la estructura obligatoria de la NOM según el tipo de documento.

**Body:**

```json
{
  "doc_type": "medical_note",
  "title": "Nota de evolución — 12 Jun 2025",
  "content_json": {
    "blocks": [
      {
        "id": "59cb3a70-e593-4a0b-bc67-0c7f8c148281",
        "type": "text",
        "label": "Evolución y actualización del cuadro",
        "value": "El paciente muestra disminución de inflamación."
      },
      {
        "id": "c1a012ab-f32a-42cd-bc39-a9a38c928421",
        "type": "vitals_table",
        "label": "Signos Vitales",
        "value": {
          "bp_systolic": 118,
          "bp_diastolic": 79,
          "heart_rate": 70,
          "temp_c": 36.4,
          "weight_kg": 71.5,
          "height_cm": 170.0,
          "bmi": 24.7
        }
      }
    ]
  },
  "appointment_id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"
}
```

**Respuesta `201`:**

```json
{
  "data": {
    "id": "d1c2b3a4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "doc_type": "medical_note",
    "title": "Nota de evolución — 12 Jun 2025",
    "status": "draft",
    "created_at": "2025-06-13T10:15:00Z"
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-13T10:15:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Estructura de bloques inválida o falta algún campo normativo requerido de la NOM. |

---

### `GET /clinical-documents/{document_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `doctor`, `assistant`  
**Descripción:** Devuelve el contenido completo y estructurado del documento y las firmas asociadas.

**Respuesta `200`:**

```json
{
  "data": {
    "id": "d1c2b3a4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "medical_record_id": "b3c2a1e0-d9c8-4b7a-6a5f-4e3d2c1b0a9f",
    "doc_type": "medical_note",
    "title": "Nota de evolución — 12 Jun 2025",
    "content_json": { "blocks": [] },
    "status": "signed",
    "is_nom_compliant": true,
    "created_by": "uuid-doctor-creador",
    "created_by_name": "Dr. Roberto García",
    "created_at": "2025-06-13T10:15:00Z",
    "updated_at": "2025-06-13T10:20:00Z",
    "signatures": [
      {
        "id": "uuid-firma",
        "signer_role": "doctor",
        "signer_user_id": "uuid-usuario",
        "signer_full_name": "Dr. Roberto García",
        "cedula_snapshot": "12345678",
        "signed_at": "2026-06-14T10:30:00Z",
        "signature_hash": "sha256:7f83b162... (SHA-256 del contenido)",
        "signature_url": "https://storage.medicloud-assets.mx/signatures/uuid-img.png"
      }
    ]
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-13T10:25:00Z"
  }
}
```

---

### `PATCH /clinical-documents/{document_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `doctor`, `assistant`  
**Descripción:** Modifica el título o el contenido estructurado del documento. Solo se permite si el documento está en estado `draft`.

**Body:**

```json
{
  "title": "Nota de evolución — 12 Jun 2025 (Corregida)",
  "content_json": { "blocks": [] }
}
```

**Respuesta `200`:** objeto `data` con el documento modificado.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `DOCUMENT_ALREADY_SIGNED` | 409 | El documento ya cuenta con al menos una firma. Sus bloques son inmutables. |
| `VALIDATION_ERROR` | 422 | Formato de bloques JSON erróneo. |

---

### `POST /clinical-documents/{document_id}/sign`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `doctor`  
**Descripción:** Registra una firma digital del médico, del paciente o de un testigo. El backend calcula de manera interna el SHA-256 de los bloques del Canvas (`content_json`) y los asocia a la firma de forma inalterable. El estado del documento cambia de forma definitiva a `signed`.

**Validaciones pre-firma para firmantes con `signer_role = "doctor"`:**
1. Se consulta el perfil en `core.doctor_profiles` vinculado al `clinic_staff_id` del firmante.
2. Si el estado de la credencial es suspendido (`credential_status = suspendido`), la firma se rechaza retornando el error `DOCTOR_CREDENTIAL_SUSPENDED` (HTTP 403).
3. Si no existe un registro en `doctor_profiles` asociado a dicho miembro del personal, se retorna el error `DOCTOR_CREDENTIAL_REQUIRED` (HTTP 422).
4. Si las validaciones son exitosas, se captura el valor actual de `cedula_profesional` (que puede ser nulo si el estado es `en_tramite`) y se persiste de manera inmutable como `cedula_snapshot` en el registro de la firma (`clinical.document_signatures`).

> **Cumplimiento NOM-004-SSA3-2012.** La norma exige que la cédula profesional del médico quede registrada de manera inmutable en el acto de la firma como dato histórico. Se utiliza un snapshot de la cédula en lugar de una relación dinámica por clave foránea (FK) debido a que la cédula profesional del médico podría suspenderse o revocarse en el futuro sin que el documento firmado pierda su validez legal histórica.

**Body:**

```json
{
  "signer_role": "doctor",
  "signer_name": "Dr. Roberto García",
  "signature_data": "data:image/png;base64,iVBORw0KGgoAAA...",
  "signature_method": "drawn"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `signer_role` | `string` | ✅ | Debe ser uno de: `doctor`, `patient`, `witness`. |
| `signer_name` | `string` | ✅ | Nombre del firmante. |
| `signature_data` | `string` | ✅ | Base64 de la imagen de la firma dibujada. |
| `signature_method` | `string` | ✅ | `drawn` (dibujado) o `biometric` (firma avanzada en tablet). |

**Respuesta `200`:**

```json
{
  "data": {
    "signature_id": "uuid-nueva-firma",
    "signed_at": "2026-06-14T10:30:00Z",
    "signer_role": "doctor",
    "cedula_snapshot": "12345678",
    "signature_method": "drawn",
    "signature_hash": "sha256:7f83b162bf53c4897f2c8d2019484b9012f45812b19283bc9e8c718a209bc1d7"
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2026-06-14T10:30:00Z"
  }
}
```

Para firmantes paciente o testigo, el campo `cedula_snapshot` se omite de la respuesta o se retorna con valor `null`.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `FORBIDDEN` | 403 | El rol del usuario no tiene autorización para firmar como médico (por ejemplo, asistentes). |
| `DOCTOR_CREDENTIAL_SUSPENDED` | 403 | El médico firmante tiene `credential_status = suspendido`; no puede firmar documentos clínicos. |
| `DOCTOR_CREDENTIAL_REQUIRED` | 422 | El usuario con `role = doctor` no tiene perfil de credenciales registrado en `doctor_profiles`. |
| `DOCUMENT_ALREADY_SIGNED` | 409 | El documento ya se encuentra cerrado con firmas de su categoría. |

---

### `POST /clinical-documents/{document_id}/photos`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `doctor`, `assistant`  
**Content-Type:** `multipart/form-data`  
**Descripción:** Adjunta una fotografía clínica o archivo digitalizado (estudio de laboratorio o imagen) al documento. Se limita a formatos estándar y un peso máximo.

**Form Fields:**

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `file` | `file` | ✅ | Archivo binario. Formatos: JPG, PNG, HEIC, PDF. Peso máximo: **10 MB**. |
| `category` | `string` | ✅ | Categoría del archivo: `preop`, `postop`, `xray`, `ct`, `lab`, `clinical`, `other`. |
| `description` | `string` | ✗ | Notas clínicas del archivo cargado. |

**Respuesta `201`:**

```json
{
  "data": {
    "id": "uuid-archivo-cargado",
    "file_url": "https://storage.medicloud-assets.mx/clinical-photos/uuid-file.jpg",
    "thumbnail_url": "https://storage.medicloud-assets.mx/clinical-photos/uuid-file_thumb.jpg",
    "category": "clinical",
    "description": "Detalle de encías en zona 12-14",
    "uploaded_by_name": "Dra. Ana Martínez",
    "created_at": "2025-06-13T10:40:00Z"
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-13T10:40:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `FILE_TOO_LARGE` | 422 | El archivo excede los 10 MB. |
| `INVALID_FILE_TYPE` | 422 | El tipo MIME binario (magic bytes) no coincide con imágenes o archivos PDF permitidos. |

---

### `GET /clinical-templates`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `doctor`, `assistant`  
**Descripción:** Lista las plantillas preconstruidas para el Canvas de documentos de la clínica.

**Respuesta `200`:**

```json
{
  "data": [
    {
      "id": "uuid-plantilla-1",
      "doc_type": "medical_note",
      "name": "Nota Pediátrica Estándar",
      "description": "Plantilla con percentiles y vacunas básicas",
      "canvas_json": { "blocks": [] },
      "is_system": false
    }
  ],
  "meta": { "request_id": "uuid" }
}
```

---

### `POST /clinical-templates`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `doctor`  
**Descripción:** Crea una nueva plantilla de Canvas reutilizable para los expedientes de la clínica.

**Body:**

```json
{
  "doc_type": "informed_consent",
  "name": "Consentimiento Informado - Cirugía Oral",
  "description": "Formato para cirugías menores y extracciones",
  "canvas_json": {
    "blocks": [
      { "id": "b1", "type": "text", "label": "Riesgos del procedimiento", "value": "" }
    ]
  }
}
```

**Respuesta `201`:** objeto `data` con la plantilla creada.

---

## 6. Catálogo de errores del módulo

| Código | HTTP | Descripción |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Parámetros incorrectos o bloques de Canvas que omiten validaciones obligatorias de la NOM. |
| `DOCUMENT_ALREADY_SIGNED` | 409 | Operación rechazada debido a que el documento clínico está firmado y cerrado (inmutable). |
| `PATIENT_NOT_FOUND` | 404 | El paciente del expediente no existe o es ajeno a la clínica. |
| `DOCUMENT_NOT_FOUND` | 404 | El documento clínico especificado no existe o pertenece a otro expediente. |
| `TEMPLATE_NOT_FOUND` | 404 | La plantilla de Canvas no existe o no pertenece a la clínica activa. |
| `DOCTOR_CREDENTIAL_SUSPENDED` | 403 | El médico firmante tiene `credential_status = suspendido`; no puede firmar documentos clínicos |
| `DOCTOR_CREDENTIAL_REQUIRED` | 422 | El usuario con `role = doctor` no tiene perfil de credenciales registrado en `doctor_profiles` |
| `FILE_TOO_LARGE` | 422 | El archivo excede el tamaño máximo establecido (10 MB). |
| `INVALID_FILE_TYPE` | 422 | El archivo binario cargado no contiene una firma o tipo MIME de imagen/PDF aceptado. |

---

## 7. Notas de seguridad

| # | Control de Seguridad | Propósito y Justificación |
|---|---|---|
| 1 | **Bloqueo Total de RLS** | Todos los endpoints operativos validan que el `X-Clinic-ID` corresponda a una membresía activa en `core.clinic_staff` del usuario autenticado. Las tablas `clinical_documents` e `history` aplican el aislamiento estricto por clínica. |
| 2 | **Inmutabilidad de Diagnósticos** | El backend debe rechazar peticiones `PATCH` o `DELETE` sobre cualquier documento clínico que tenga `status = 'signed'`. Una vez que el médico registra su firma, el documento no se puede alterar para proteger la integridad legal del expediente. |
| 3 | **Cálculo de Hash Criptográfico (SHA-256)** | Al firmar, el backend genera un SHA-256 del contenido (`content_json` y título) y lo persiste en la firma. Esto permite verificar mediante auditoría externa si el registro médico ha sufrido alguna alteración manual en base de datos. |
| 4 | **Restricción de Roles Administrativos** | Los roles de `receptionist` y `clinic_admin` no tienen acceso bajo ninguna circunstancia a los endpoints del expediente clínico (`403 Forbidden`). Esto garantiza el principio de privilegio mínimo de los expedientes de pacientes. |
| 5 | **Sanitización y Ofuscación en Respuestas Financieras** | El rol `accountant` tiene acceso a listados y metadatos operativos básicos para auditoría de servicios, pero el contenido detallado de los campos médicos (`content_json`) es ofuscado o devuelto como `null` en sus solicitudes. |
| 6 | **Magic Bytes y Aislamiento en Uploads de Fotos** | Las imágenes de estudios o fotos clínicas cargadas se analizan mediante magic bytes en el backend, se almacenan en un bucket de storage aislado y se sirven con cabeceras `Content-Security-Policy: default-src 'none'` y renderizadas exclusivamente en etiquetas `<img>`. |
| 7 | **Encriptación a Nivel de Aplicación del Contenido Clínico (ADR-005)** | `content_json`, `content_html` y `signature_url` se cifran con AES-256-GCM antes de persistirse, con clave gestionada en un KMS externo. Ni un acceso SQL directo a la base de datos expone el contenido del expediente. El contenido cifrado no es indexable; la búsqueda se limita a metadatos no sensibles. |

---
*API — Módulo 04: Expediente Clínico · Sistema de Gestión de Consultorios Médicos · Fase 1 MVP · v2.1 · Confidencial · Alineado con Arquitectura v2.1 y Modelo de Datos v2.1*
