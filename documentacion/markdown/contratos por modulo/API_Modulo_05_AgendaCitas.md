# API — Módulo 05: Agenda y Citas
## Sistema de Gestión de Consultorios Médicos · Fase 1 MVP

---

| Campo | Detalle |
|---|---|
| Versión del documento | 1.0 |
| Fecha | Junio 2025 |
| Estado | Revisado · Listo para desarrollo |
| Audiencia | Equipo Backend · Equipo Frontend |
| Módulo | `scheduling` |
| Prefijo de rutas | `/v1/appointments` |
| Tablas involucradas | `scheduling.appointments` · `scheduling.appointment_procedures` · `clinical.conciliation_items` · `inventory.products` · `inventory.stock_movements` |
| Referencias | Arquitectura v2.0 §3 §4.4 · Modelo de Datos v2.0 (schema `scheduling` y `clinical.conciliation_items`) |
| Cambios vs v1.0 | Alineación de referencias a v2.0. Campo `reason` (motivo de consulta) en la cita. Control de concurrencia de inventario por **bloqueo optimista** (`inventory.products.version`) en lugar de transacciones serializables. Publicación de eventos de dominio mediante el patrón **outbox** (`accounting.domain_events`). |

---

## Tabla de contenido

1. [Descripción general](#1-descripción-general)
2. [Ciclo de vida y efectos colaterales](#2-ciclo-de-vida-y-efectos-colaterales)
3. [Flujos cubiertos](#3-flujos-cubiertos)
4. [Reglas de acceso por rol (RBAC)](#4-reglas-de-acceso-por-rol-rbac)
5. [Endpoints](#5-endpoints)
   - [GET /appointments](#get-appointments)
   - [POST /appointments](#post-appointments)
   - [PATCH /appointments/{appointment_id}](#patch-appointmentsappointment_id)
   - [POST /appointments/{appointment_id}/confirm](#post-appointmentsappointment_idconfirm)
   - [POST /appointments/{appointment_id}/start](#post-appointmentsappointment_idstart)
   - [POST /appointments/{appointment_id}/complete](#post-appointmentsappointment_idcomplete)
   - [POST /appointments/{appointment_id}/cancel](#post-appointmentsappointment_idcancel)
   - [POST /appointments/{appointment_id}/no-show](#post-appointmentsappointment_idno-show)
6. [Catálogo de errores del módulo](#6-catálogo-de-errores-del-módulo)
7. [Notas de seguridad](#7-notas-de-seguridad)

---

## 1. Descripción general

Este módulo gestiona la agenda del consultorio y el ciclo de vida de las citas médicas. La API coordina la disponibilidad horaria del personal médico y, de manera atómica, gestiona la reserva y consumo físico de insumos del inventario, publicando los eventos de dominio correspondientes para la contabilidad automatizada.

El contexto de multi-tenancy se aplica mediante la cabecera `X-Clinic-ID`. El backend realiza comprobaciones automáticas de zona horaria según la configuración de la clínica para calcular los rangos de las agendas de forma consistente.

---

## 2. Ciclo de vida y efectos colaterales

El estado de la cita (`status`) progresa conforme al siguiente flujo:

```
[scheduled] ──> [confirmed] ──> [in_progress] ──> [completed]
     │               │
     └──────┬────────┘
            v
       [cancelled] o [no_show]
```

### Efectos automáticos por cambio de estado:
* **`confirmed`**: El backend bloquea de forma atómica la cantidad teórica de insumos configurados en los procedimientos asignados (`inventory.products.stock_reserved` aumenta, y el stock disponible disminuye). Genera el evento `CitaConfirmada`. Si no hay suficiente existencia, se rechaza la confirmación.
* **`in_progress`**: Registra la hora de inicio real (`actual_start`).
* **`completed`**: Requiere enviar la conciliación definitiva de materiales. Libera el stock reservado y da de baja definitiva del almacén el stock real consumido, registrando movimientos de salida (`exit`) y mermas si las hubiera. Publica `ConsumoConciliado` y `MermaCaducidad` para el motor contable (ARE).
* **`cancelled`**: Libera de forma atómica el stock en reserva (lo devuelve a disponible). Si la cita ya tiene cobros registrados, publica el evento de reverso contable correspondiente.

---

## 3. Flujos cubiertos

| # | Flujo | Endpoints |
|---|---|---|
| 1 | Consultar la agenda (vistas de calendario) | `GET /appointments` |
| 2 | Agendar una nueva cita | `POST /appointments` |
| 3 | Reprogramar o modificar notas de cita | `PATCH /appointments/{appointment_id}` |
| 4 | Confirmar cita y reservar insumos en inventario | `POST /appointments/{appointment_id}/confirm` |
| 5 | Iniciar la consulta médica | `POST /appointments/{appointment_id}/start` |
| 6 | Conciliar insumos y completar consulta | `POST /appointments/{appointment_id}/complete` |
| 7 | Cancelar cita y liberar reservas/revertir cobros | `POST /appointments/{appointment_id}/cancel` |
| 8 | Registrar inasistencia del paciente | `POST /appointments/{appointment_id}/no-show` |

---

## 4. Reglas de acceso por rol (RBAC)

| Endpoint / Acción | `admin` | `clinic_admin` | `doctor` | `receptionist` | `assistant` | `accountant` | `cleaning` |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Ver Agenda / Listar | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ (solo lectura)| — |
| Crear Cita | ✅ | ✅ | ✅ | ✅ | ✅ | — | — |
| Modificar / Reprogramar| ✅ | ✅ | ✅ | ✅ | ✅ | — | — |
| Confirmar Cita | ✅ | ✅ | ✅ | ✅ | ✅ | — | — |
| Iniciar Cita (`/start`)| ✅ | — | ✅ | — | ✅ | — | — |
| Completar Cita (`/complete`)| ✅ | — | ✅ | — | ✅ | — | — |
| Cancelar Cita | ✅ | ✅ | ✅ | ✅ | ✅ | — | — |
| Registrar No-Show | ✅ | ✅ | ✅ | ✅ | ✅ | — | — |

---

## 5. Endpoints

---

### `GET /appointments`

**Acceso:** Privado · Requiere `X-Clinic-ID`  
**Descripción:** Devuelve un listado de citas dentro de la clínica filtradas por rangos de fechas, médicos o pacientes, optimizado para alimentar vistas de agenda diaria, semanal o mensual.

**Query Parameters:**

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `date_from` | `string (ISO 8601)` | ✅ | Inicio del periodo de consulta en UTC. |
| `date_to` | `string (ISO 8601)` | ✅ | Fin del periodo de consulta en UTC. |
| `doctor_id` | `UUID` | ✗ | Filtrar citas asignadas a un médico específico. |
| `patient_id` | `UUID` | ✗ | Filtrar citas de un paciente. |
| `status` | `string` | ✗ | Filtrar por estado. |
| `page` | `integer` | ✗ | Página de resultados (por defecto: 1). |
| `per_page` | `integer` | ✗ | Resultados por página (por defecto: 50, máx: 100). |

**Respuesta `200`:**

```json
{
  "data": [
    {
      "id": "c1b2a3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "patient_id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
      "patient_name": "María López Martínez",
      "doctor_id": "d8c7b6a5-e4f3-2a1b-0c9d-8e7f6a5b4c3d",
      "doctor_name": "Dr. Roberto García",
      "treatment_plan_id": "uuid-plan-tratamiento-o-null",
      "scheduled_start": "2025-06-20T10:00:00Z",
      "scheduled_end": "2025-06-20T10:30:00Z",
      "actual_start": null,
      "actual_end": null,
      "status": "scheduled",
      "status_label": "Agendada",
      "room": "Consultorio 102",
      "color_tag": "#376D6D",
      "notes": "Requiere expediente físico a la mano.",
      "procedures": [
        {
          "procedure_id": "uuid-procedimiento-1",
          "name": "Consulta Dental Preventiva",
          "default_price_cents": 50000
        }
      ]
    }
  ],
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-13T16:30:00Z",
    "pagination": { "page": 1, "per_page": 50, "total_items": 1, "total_pages": 1 }
  }
}
```

---

### `POST /appointments`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `receptionist`, `assistant`  
**Descripción:** Agenda una nueva cita. El backend valida en primer lugar la disponibilidad del médico (previniendo sobreposición de horarios o double booking).

**Body:**

```json
{
  "patient_id": "a9b8c7d6-e5f4-3a2b-1c0d-9e8f7a6b5c4d",
  "doctor_id": "d8c7b6a5-e4f3-2a1b-0c9d-8e7f6a5b4c3d",
  "treatment_plan_id": null,
  "scheduled_start": "2025-06-20T10:00:00Z",
  "scheduled_end": "2025-06-20T10:30:00Z",
  "room": "Consultorio 102",
  "color_tag": "#376D6D",
  "reason": "Dolor en primer molar.",
  "notes": "Paciente requiere silla de ruedas.",
  "procedures": [
    { "procedure_id": "uuid-procedimiento-1" }
  ]
}
```

> **Nota de campos (v2.0).** `reason` es el motivo de la consulta (se persiste en `scheduling.appointments.reason`); `notes` queda para observaciones logísticas. `color_tag` admite formato `#RRGGBB`.

**Respuesta `201`:**

```json
{
  "data": {
    "id": "c1b2a3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "status": "scheduled",
    "scheduled_start": "2025-06-20T10:00:00Z",
    "created_at": "2025-06-13T16:35:00Z"
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-13T16:35:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `DOCTOR_NOT_AVAILABLE` | 409 | El médico ya tiene una cita activa o confirmada asignada en la franja horaria solicitada (tolerancia de colisión de 0 minutos). |
| `VALIDATION_ERROR` | 422 | Faltan campos, formato de fecha inválido o la hora de inicio es posterior a la de fin. |

---

### `PATCH /appointments/{appointment_id}`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `receptionist`, `assistant`  
**Descripción:** Modifica o reprograma una cita médica. Si la cita ya está confirmada (`confirmed`) y se modifica su horario o médico, el backend libera automáticamente la reserva previa de insumos y calcula la disponibilidad del nuevo bloque.

**Body (todos los campos son opcionales):**

```json
{
  "scheduled_start": "2025-06-21T11:00:00Z",
  "scheduled_end": "2025-06-21T11:30:00Z",
  "notes": "Reprogramada por llamada del paciente."
}
```

**Respuesta `200`:** objeto `data` con la información de la cita actualizada.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `APPOINTMENT_NOT_EDITABLE` | 409 | Citas en estado `completed`, `cancelled` o `no_show` no pueden modificarse. |
| `DOCTOR_NOT_AVAILABLE` | 409 | Conflicto de horario al reprogramar. |

---

### `POST /appointments/{appointment_id}/confirm`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `receptionist`, `assistant`  
**Descripción:** Cambia el estado a `confirmed`. El backend calcula de manera atómica las cantidades teóricas de materiales requeridas en `treatments.procedure_supplies` para los procedimientos asociados y las coloca en estado Reservado en el Kardex.

**Respuesta `200`:**

```json
{
  "data": {
    "appointment_id": "c1b2a3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "status": "confirmed",
    "stock_reserved": [
      {
        "product_id": "uuid-producto-1",
        "product_name": "Lidocaína 2%",
        "quantity_reserved": 2.5000,
        "unit": "ml"
      }
    ]
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-13T16:40:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `STOCK_INSUFFICIENT` | 409 | No hay suficiente stock disponible de uno o más insumos asignados. Devuelve listado de insumos con deficiencias de stock en el campo `details` de la respuesta de error. |

---

### `POST /appointments/{appointment_id}/start`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `doctor`, `assistant`  
**Descripción:** Cambia el estado de la cita a `in_progress` y registra la marca de tiempo `actual_start = now()`.

**Respuesta `200`:**

```json
{
  "data": {
    "appointment_id": "c1b2a3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "status": "in_progress",
    "actual_start": "2025-06-20T10:05:22Z"
  },
  "meta": {
    "request_id": "uuid"
  }
}
```

---

### `POST /appointments/{appointment_id}/complete`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `doctor`, `assistant`  
**Descripción:** Completa la cita médica. Este endpoint recopila las cantidades decimales reales de insumos consumidos y mermados (`conciliation_items`). 
El sistema:
1. Libera las reservas previas asociadas a la cita.
2. Registra los movimientos de salida (`exit`) definitivos y mermas en el Kardex.
3. Publica los eventos `ConsumoConciliado` y `MermaCaducidad` para registrar los costos contables en el Libro Mayor.
4. Cambia el estado a `completed` y setea `actual_end = now()`.

**Body:**

```json
{
  "conciliation_items": [
    {
      "appointment_procedure_id": "uuid-proc-cita-1",
      "product_id": "uuid-producto-1",
      "quantity_budgeted": 2.5000,
      "quantity_actual": 2.0000,
      "quantity_wasted": 0.5000,
      "unit": "ml"
    }
  ],
  "notes": "Se usó menos anestesia de la presupuestada, pero se desperdició media ampolleta rota."
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `conciliation_items` | `array` | ✅ | Lista de materiales validados. |
| `conciliation_items.product_id` | `UUID` | ✅ | Identificador del insumo. |
| `conciliation_items.quantity_actual` | `decimal (NUMERIC)` | ✅ | Cantidad real consumida (soporta decimales hasta 4 posiciones). |
| `conciliation_items.quantity_wasted` | `decimal (NUMERIC)` | ✅ | Cantidad de merma o desperdicio durante la consulta. |
| `conciliation_items.unit` | `string` | ✅ | Unidad de medida (ej. `ml`, `pz`, `g`). |

**Respuesta `200`:**

```json
{
  "data": {
    "appointment_id": "c1b2a3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "status": "completed",
    "actual_end": "2025-06-20T10:45:10Z",
    "stock_movements_registered": 2,
    "domain_events_published": [
      "ConsumoConciliado",
      "MermaCaducidad"
    ]
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-20T10:45:12Z"
  }
}
```

---

### `POST /appointments/{appointment_id}/cancel`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `receptionist`, `assistant`  
**Descripción:** Cancela la cita médica. De forma atómica, devuelve las cantidades de insumos reservadas al stock disponible. Si la cita contaba con un cobro previo, publica el evento de cancelación para que el ARE genere la póliza de ajuste/reverso contable.

**Body:**

```json
{
  "reason": "El paciente canceló vía telefónica por viaje.",
  "generate_reversal": true
}
```

**Respuesta `200`:**

```json
{
  "data": {
    "appointment_id": "c1b2a3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "status": "cancelled",
    "stock_released": true,
    "reversal_generated": true
  },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-13T16:50:00Z"
  }
}
```

---

### `POST /appointments/{appointment_id}/no-show`

**Acceso:** Privado · Requiere `X-Clinic-ID` · Roles: `admin`, `clinic_admin`, `doctor`, `receptionist`, `assistant`  
**Descripción:** Registra la inasistencia del paciente (`no_show`). **Nota:** Este cambio no libera de forma automática el stock reservado del inventario, permitiendo que la clínica evalúe la penalización o reprogramación en el transcurso del día de forma manual.

**Respuesta `200`:**

```json
{
  "data": {
    "appointment_id": "c1b2a3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "status": "no_show"
  },
  "meta": {
    "request_id": "uuid"
  }
}
```

---

## 6. Catálogo de errores del módulo

| Código | HTTP | Descripción |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Fechas en formato inválido, hora de inicio posterior a la final, o datos faltantes. |
| `DOCTOR_NOT_AVAILABLE` | 409 | El médico tiene conflicto de horario con otra cita en la misma franja de tiempo. |
| `STOCK_INSUFFICIENT` | 409 | Uno o más insumos asignados a los procedimientos no tienen existencias suficientes disponibles en inventario. |
| `APPOINTMENT_NOT_EDITABLE` | 409 | La cita seleccionada está en un estado que impide reprogramación o edición (completed, cancelled, no_show). |
| `APPOINTMENT_NOT_FOUND` | 404 | No se encontró ninguna cita con el UUID provisto dentro de la clínica activa. |

---

## 7. Notas de seguridad

| # | Control de Seguridad | Propósito y Justificación |
|---|---|---|
| 1 | **Bloqueo de Overbooking / Doble Reserva** | El backend debe garantizar, mediante bloqueos optimistas o semáforos a nivel de base de datos (`SELECT ... FOR UPDATE`), que no sea posible sobreponer citas del mismo médico a la misma hora, mitigando conflictos de agenda bajo alta concurrencia. |
| 2 | **Middleware de Membresía Multi-tenant** | Cada endpoint valida que la clínica activa en `X-Clinic-ID` sea una sucursal en la cual el usuario tiene permisos operativos vigentes en `core.clinic_staff`. |
| 3 | **Control de Concurrencia de Inventario (bloqueo optimista)** | La reserva de stock durante `/confirm` y la deducción en `/complete` usan bloqueo optimista sobre `inventory.products.version` (`@Version` de JPA). Si dos citas confirman el mismo insumo en paralelo, una falla con `OptimisticLockException` y se reintenta sobre el estado fresco, evitando *lost updates* y garantizando que `STOCK_INSUFFICIENT` se dispare de forma confiable. Ver Modelo de Datos v2.0 §12.5. |
| 4 | **Inmutabilidad Contable y patrón Outbox** | Las cancelaciones de citas pagadas jamás eliminan los registros de pagos o facturación. Los eventos contables (`ConsumoConciliado`, `MermaCaducidad`, `CitaCancelada`) se insertan en `accounting.domain_events` dentro de la misma transacción de negocio (patrón *outbox*) y un *relay* los publica en RabbitMQ en orden, garantizando pólizas de reverso/ajuste transparentes y auditables. Ver Arquitectura v2.0 §3.1. |
| 5 | **Sanitización de Notas y Diagnósticos en Agenda** | Las descripciones libres y notas de citas son sanitizadas contra inyección de HTML o scripts para evitar vulnerabilidades XSS en la visualización del panel de agenda por parte de los recepcionistas. |

---
*API — Módulo 05: Agenda y Citas · Sistema de Gestión de Consultorios Médicos · Fase 1 MVP · v2.0 · Confidencial · Alineado con Arquitectura v2.0 y Modelo de Datos v2.0*
