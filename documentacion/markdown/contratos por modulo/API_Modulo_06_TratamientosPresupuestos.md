# API — Módulo 06: Tratamientos y Presupuestos
## Sistema de Gestión de Consultorios Médicos · Fase 1 MVP

---

| Campo | Detalle |
|---|---|
| Versión del documento | 1.0 |
| Fecha | Junio 2025 |
| Estado | Revisado · Listo para desarrollo |
| Audiencia | Equipo Backend · Equipo Frontend |
| Módulo | `treatments` |
| Prefijo de rutas | `/v1/treatment-plans` · `/v1/procedures` |
| Tablas involucradas | `treatments.treatment_plans` · `treatments.treatment_plan_items` · `treatments.procedures` · `treatments.procedure_supplies` · `inventory.products` |
| Referencias | Arquitectura v2.0 §4.4.1 · Modelo de Datos v2.0 (schema `treatments`) |
| Cambios vs v1.0 | Alineación de referencias a v2.0. Sin cambios de contrato: el esquema de planes, ítems, procedimientos y lista de materiales permanece estable respecto a v1.0. |

---

## Tabla de contenido

1. [Descripción general](#1-descripción-general)
2. [Convenciones del módulo](#2-convenciones-del-módulo)
3. [Flujos cubiertos](#3-flujos-cubiertos)
4. [Reglas de acceso por rol (RBAC)](#4-reglas-de-acceso-por-rol-rbac)
5. [Endpoints — Planes de Tratamiento](#5-endpoints--planes-de-tratamiento)
   - [GET /patients/{patient_id}/treatment-plans](#get-patientspatient_idtreatment-plans)
   - [POST /patients/{patient_id}/treatment-plans](#post-patientspatient_idtreatment-plans)
   - [GET /treatment-plans/{plan_id}](#get-treatment-plansplan_id)
   - [PATCH /treatment-plans/{plan_id}/items/{item_id}](#patch-treatment-plansplan_iditemsitem_id)
   - [DELETE /treatment-plans/{plan_id}](#delete-treatment-plansplan_id)
6. [Endpoints — Catálogo de Procedimientos](#6-endpoints--catálogo-de-procedimientos)
   - [GET /procedures](#get-procedures)
   - [POST /procedures](#post-procedures)
   - [PATCH /procedures/{procedure_id}](#patch-proceduresprocedure_id)
   - [DELETE /procedures/{procedure_id}](#delete-proceduresprocedure_id)
7. [Catálogo de errores del módulo](#7-catálogo-de-errores-del-módulo)
8. [Notas de seguridad](#8-notas-de-seguridad)

---

## 1. Descripción general

Este módulo gestiona la creación de planes de tratamiento y presupuestos para los pacientes, y el catálogo maestro de procedimientos de la clínica con sus respectivos insumos de inventario configurados.

La API permite a los médicos cotizar planes de tratamiento integrando precios vigentes del catálogo y agregando soporte para dientes bajo la nomenclatura FDI (para uso odontológico). Las cotizaciones de tratamientos se mantienen en estado Borrador (`status = quoted`) hasta que el paciente acepte formalmente el tratamiento (`status = accepted`), punto en el cual se congela su precio unitario snapshot.

El catálogo de procedimientos es compartido entre el personal clínico, pero sus modificaciones de insumos y precios base están estrictamente restringidos al personal administrativo.

---

## 2. Convenciones del módulo

- Todas las solicitudes y respuestas usan `Content-Type: application/json`.
- Todos los endpoints son **privados** y requieren `Authorization: Bearer <access_token>` y `X-Clinic-ID`.
- Los montos financieros se expresan en centavos enteros (`integer`).
- Las cantidades de insumos configurados en las listas de materiales de los procedimientos (`procedure_supplies`) se representan con números decimales (`decimal`) con precisión de hasta 4 decimales para soportar mililitros, gramos, o fracciones de piezas.

---

## 3. Flujos cubiertos

| # | Flujo | Endpoints |
|---|---|---|
| 1 | Listar planes de tratamiento del paciente | `GET /patients/{patient_id}/treatment-plans` |
| 2 | Crear un plan de tratamiento/presupuesto | `POST /patients/{patient_id}/treatment-plans` |
| 3 | Consultar desglose de plan y balances financieros | `GET /treatment-plans/{plan_id}` |
| 4 | Modificar precios o descuentos de un ítem cotizado | `PATCH /treatment-plans/{plan_id}/items/{item_id}` |
| 5 | Cancelar o eliminar plan de tratamiento | `DELETE /treatment-plans/{plan_id}` |
| 6 | Consultar catálogo de procedimientos de la clínica | `GET /procedures` |
| 7 | Dar de alta un procedimiento con insumos | `POST /procedures` |
| 8 | Modificar precios base o insumos del catálogo | `PATCH /procedures/{procedure_id}` |
| 9 | Dar de baja un procedimiento | `DELETE /procedures/{procedure_id}` |

---

## 4. Reglas de acceso por rol (RBAC)

| Acción / Endpoint | `admin` | `clinic_admin` | `doctor` | `receptionist` | `assistant` | `accountant` | `cleaning` |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Ver Planes de Tratamiento | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ (solo lectura) | — |
| Crear / Editar Planes | ✅ | ✅ | ✅ | — | ✅ | — | — |
| Eliminar Plan (`quoted`) | ✅ | ✅ | ✅ | — | — | — | — |
| Ver Catálogo de Proced. | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| Crear Procedimiento Cat. | ✅ | ✅ | — | — | — | — | — |
| Modificar Catálogo | ✅ | ✅ | — | — | — | — | — |
| Dar de baja en Catálogo | ✅ | ✅ | — | — | — | — | — |

---

## 5. Endpoints — Planes de Tratamiento

---

### `GET /patients/{patient_id}/treatment-plans`

**Acceso:** Privado · Requiere `X-Clinic-ID`  
**Descripción:** Devuelve los planes de tratamiento registrados del paciente.

**Respuesta `200`:**

```json
{
  "data": [
    {
      "id": "e4d3c2b1-a09f-4b7a-6a5f-4e3d2c1b0a9f",
      "title": "Tratamiento Ortodoncia Inicial",
      "status": "in_progress",
      "status_label": "En Progreso",
      "total_quoted_cents": 1500000,
      "total_paid_cents": 500000,
      "pending_balance_cents": 1000000,
      "created_by_name": "Dr. Roberto García",
      "created_at": "2025-05-10T12:00:00Z"
    }
  ],
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-13T17:20:00Z"
  }
}
```

---

### `POST /patients/{patient_id}/treatment-plans`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `assistant`  
**Descripción:** Crea una nueva propuesta de plan de tratamiento. El backend toma automáticamente los precios vigentes sugeridos del catálogo maestro (`procedures.default_price`) e inicializa el subtotal de cada ítem cotizado.

**Body:**

```json
{
  "title": "Rehabilitación Oral Completa",
  "clinical_notes": "Paciente requiere extracción de molares 18 y 28 antes de iniciar resinas.",
  "items": [
    {
      "procedure_id": "uuid-procedimiento-1",
      "description": "Extracción quirúrgica de molar",
      "tooth_number": "18",
      "quantity": 1.0,
      "discount_pct": 5.00
    },
    {
      "procedure_id": "uuid-procedimiento-1",
      "description": "Extracción quirúrgica de molar",
      "tooth_number": "28",
      "quantity": 1.0,
      "discount_pct": 5.00
    }
  ]
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `title` | `string` | ✅ | Título del plan de tratamiento. Máx 300 caracteres. |
| `clinical_notes` | `string` | ✗ | Notas clínicas del plan. |
| `items` | `array` | ✅ | Lista de procedimientos a presupuestar. Mínimo 1. |
| `items.procedure_id` | `UUID` | ✅ | ID del procedimiento en el catálogo maestro. |
| `items.tooth_number` | `string` | ✗ | Número de pieza dental bajo nomenclatura FDI (ej. `18`, `28`, `46`). |
| `items.quantity` | `decimal` | ✅ | Cantidad de veces a realizar. Por defecto: 1.00. |
| `items.discount_pct` | `decimal` | ✗ | Descuento propuesto. Rango: `0.00` a `100.00`. Por defecto: 0.00. |

**Respuesta `201`:**

```json
{
  "data": {
    "id": "e4d3c2b1-a09f-4b7a-6a5f-4e3d2c1b0a9f",
    "status": "quoted",
    "total_quoted_cents": 190000,
    "created_at": "2025-06-13T17:25:00Z",
    "items": [
      {
        "id": "uuid-item-1",
        "procedure_id": "uuid-procedimiento-1",
        "unit_price_cents": 100000,
        "discount_pct": 5.00,
        "subtotal_cents": 95000
      },
      {
        "id": "uuid-item-2",
        "procedure_id": "uuid-procedimiento-1",
        "unit_price_cents": 100000,
        "discount_pct": 5.00,
        "subtotal_cents": 95000
      }
    ]
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `PROCEDURE_NOT_FOUND` | 404 | Uno de los `procedure_id` especificados no existe en la clínica. |
| `VALIDATION_ERROR` | 422 | Descuentos inválidos o campos incompletos. |

---

### `GET /treatment-plans/{plan_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID`  
**Descripción:** Devuelve la cotización o plan completo con desglose de ítems, su estado de ejecución y su balance financiero.

**Respuesta `200`:**

```json
{
  "data": {
    "id": "e4d3c2b1-a09f-4b7a-6a5f-4e3d2c1b0a9f",
    "patient_id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
    "title": "Rehabilitación Oral Completa",
    "status": "accepted",
    "total_quoted_cents": 190000,
    "total_paid_cents": 95000,
    "pending_balance_cents": 95000,
    "created_by": "uuid-medico",
    "created_at": "2025-06-13T17:25:00Z",
    "accepted_at": "2025-06-13T17:30:00Z",
    "items": [
      {
        "id": "uuid-item-1",
        "procedure_name": "Extracción quirúrgica de molar",
        "tooth_number": "18",
        "quantity": 1.0,
        "unit_price_cents": 100000,
        "discount_pct": 5.00,
        "subtotal_cents": 95000,
        "status": "completed",
        "status_label": "Completado"
      },
      {
        "id": "uuid-item-2",
        "procedure_name": "Extracción quirúrgica de molar",
        "tooth_number": "28",
        "quantity": 1.0,
        "unit_price_cents": 100000,
        "discount_pct": 5.00,
        "subtotal_cents": 95000,
        "status": "pending",
        "status_label": "Pendiente"
      }
    ]
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud"
  }
}
```

---

### `PATCH /treatment-plans/{plan_id}/items/{item_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `assistant`  
**Descripción:** Modifica las características de cotización de un ítem específico dentro del plan (cantidad, precio cotizado o descuento).
**Nota de negocio:** Solo se puede modificar mientras el plan esté en estado cotizado (`quoted`) o activo (`accepted` / `in_progress`). Una vez completado (`completed`) o cancelado (`cancelled`), este endpoint queda deshabilitado para el plan.

**Body:**

```json
{
  "discount_pct": 10.00,
  "description": "Se aplica 10% por paquete familiar."
}
```

**Respuesta `200`:** objeto `data` con el plan consolidado recalculado.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `PLAN_NOT_EDITABLE` | 409 | El plan ya está cerrado (completed o cancelled). |
| `ITEM_NOT_IN_PLAN` | 404 | El `item_id` no pertenece al plan de tratamiento. |

---

### `DELETE /treatment-plans/{plan_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`  
**Descripción:** Realiza la baja lógica (`deleted_at = now()`) de un plan de tratamiento. Solo está permitido si el plan de tratamiento está en estado cotizado (`quoted`). Si ya tiene abonos/pagos recibidos (`total_paid > 0`) o procedimientos completados, la baja lógica es rechazada.

**Respuesta:** `204 No Content`

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `PLAN_ALREADY_ACCEPTED` | 409 | El plan ya fue aceptado o cuenta con pagos registrados. Debe cancelarse mediante flujos de caja/reverso. |

---

## 6. Endpoints — Catálogo de Procedimientos

---

### `GET /procedures`

**Acceso:** Privado · Requiere `X-Clinic-ID`  
**Descripción:** Lista el catálogo maestro de procedimientos de la clínica e incluye la receta/lista de materiales necesarios para su ejecución.

**Query Parameters:**

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `q` | `string` | ✗ | Búsqueda por nombre de procedimiento o código. |
| `category` | `string` | ✗ | Filtrar por categoría clínica. |

**Respuesta `200`:**

```json
{
  "data": [
    {
      "id": "uuid-procedimiento-1",
      "code": "PROC-EXT-01",
      "name": "Extracción quirúrgica de molar",
      "description": "Procedimiento de remoción de piezas molares impactadas.",
      "category": "Cirugía Oral",
      "default_price_cents": 100000,
      "duration_minutes": 45,
      "is_active": true,
      "supplies": [
        {
          "product_id": "uuid-producto-1",
          "product_name": "Lidocaína 2%",
          "quantity_required": 2.5000,
          "unit": "ml"
        },
        {
          "product_id": "uuid-producto-2",
          "product_name": "Guantes Nitrilo",
          "quantity_required": 2.0000,
          "unit": "pz"
        }
      ]
    }
  ],
  "meta": { "request_id": "uuid" }
}
```

---

### `POST /procedures`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`  
**Descripción:** Da de alta un nuevo procedimiento clínico asociándole su costo y los materiales requeridos por sesión.

**Body:**

```json
{
  "code": "PROC-EXT-01",
  "name": "Extracción quirúrgica de molar",
  "description": "Procedimiento de remoción de piezas molares impactadas.",
  "category": "Cirugía Oral",
  "default_price_cents": 100000,
  "duration_minutes": 45,
  "supplies": [
    {
      "product_id": "uuid-producto-1",
      "quantity_required": 2.5000,
      "unit": "ml"
    }
  ]
}
```

**Respuesta `201`:** objeto `data` con el procedimiento creado.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `PRODUCT_NOT_FOUND` | 404 | Uno de los insumos en `supplies` no existe en la clínica. |
| `VALIDATION_ERROR` | 422 | Datos obligatorios faltantes. |

---

### `PATCH /procedures/{procedure_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`  
**Descripción:** Actualiza los precios, duraciones o la receta de materiales del procedimiento.
**Implicación de negocio:** Los cambios a la receta de materiales de este catálogo **no alteran** de forma retroactiva las citas que ya se encuentren confirmadas o los planes cotizados previamente, preservando la inmutabilidad de presupuestos antiguos.

**Body:**

```json
{
  "default_price_cents": 120000,
  "supplies": [
    {
      "product_id": "uuid-producto-1",
      "quantity_required": 3.0000,
      "unit": "ml"
    }
  ]
}
```

**Respuesta `200`:** objeto `data` con el procedimiento actualizado.

---

### `DELETE /procedures/{procedure_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`  
**Descripción:** Realiza la baja lógica (`is_active = false`) del procedimiento. Ya no se podrá usar para crear nuevos planes de tratamiento, pero se conserva en el catálogo histórico de la base de datos para consistencia de auditoría de tratamientos antiguos.

**Respuesta:** `204 No Content`

---

## 7. Catálogo de errores del módulo

| Código | HTTP | Descripción |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Descuentos fuera de límites (menor a 0 o mayor a 100) o datos faltantes. |
| `PROCEDURE_NOT_FOUND` | 404 | El procedimiento maestro no existe en esta clínica. |
| `ITEM_NOT_IN_PLAN` | 404 | El ítem del tratamiento no pertenece al identificador del plan provisto. |
| `PLAN_NOT_EDITABLE` | 409 | No se puede editar el ítem debido a que el plan está completado o cancelado. |
| `PLAN_ALREADY_ACCEPTED` | 409 | No se puede eliminar el plan porque ya fue aceptado o tiene saldos pagados registrados. |
| `PRODUCT_NOT_FOUND` | 404 | Uno de los insumos indicados en la lista de materiales no existe en el catálogo de inventario. |

---

## 8. Notas de seguridad

| # | Control de Seguridad | Propósito y Justificación |
|---|---|---|
| 1 | **Congelamiento Financiero de Precios** | Al pasar el plan a `accepted`, los precios unitarios se guardan de forma permanente como un snapshot en `treatment_plan_items.unit_price`. Los cambios posteriores en el precio base del procedimiento en el catálogo maestro no afectarán los planes ya autorizados. |
| 2 | **Filtros Multi-Tenant RLS** | Todas las consultas e inserciones a `treatment_plans` y `procedures` se filtran de forma nativa mediante `clinic_id = current_setting('app.current_clinic_id')` para evitar accesos cruzados entre clínicas. |
| 3 | **Restricción Estricta de Modificación del Catálogo** | Las operaciones de escritura (`POST`, `PATCH`, `DELETE`) en el catálogo maestro `/procedures` están estrictamente restringidas a los roles `admin` y `clinic_admin`. El personal clínico (`doctor`, `assistant`) solo puede leer. |
| 4 | **Inmutabilidad Histórica de Procedimientos** | Para evitar errores de integridad referencial, los procedimientos del catálogo maestro nunca se eliminan físicamente (`DELETE` en SQL); se desactivan lógicamente (`is_active = false`), protegiendo la consistencia de expedientes contables y clínicos. |
| 5 | **Sanitización de Notas Clínicas** | Los campos `clinical_notes` y `description` son sanitizados en el backend para prevenir XSS persistente en las interfaces de consulta de los doctores y administradores. |

---
*API — Módulo 06: Tratamientos y Presupuestos · Sistema de Gestión de Consultorios Médicos · Fase 1 MVP · v2.0 · Confidencial · Alineado con Arquitectura v2.0 y Modelo de Datos v2.0*
