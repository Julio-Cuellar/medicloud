# Especificación de Arquitectura
## Sistema de Gestión de Consultorios Médicos
### Documento de Arquitectura y Planificación por Fases · Versión 2.0 · Junio 2025 · Confidencial

---

| Campo | Detalle |
|---|---|
| Versión | 2.0 |
| Fecha | Junio 2025 |
| Estado | Revisado — incorpora stack tecnológico, monolito modular, LFPDPPP, RabbitMQ, OAuth 2.0 y estrategia móvil |
| Cambios vs v1.0 | §1.2 Stack tecnológico · §1.3 Monolito modular · §2.7 LFPDPPP · §3 Eventos y tiempo real (RabbitMQ) · §4 Seguridad (Spring Security + OAuth 2.0) · §5 Estrategia móvil · §7.6.1 CFDI puente · §14 ADRs |
| Audiencia | Equipo técnico, Product Owner, Dirección |

---

## Tabla de Contenido

1. [Contexto y Objetivo](#1-contexto-y-objetivo)
   - [1.2 Stack Tecnológico](#12-stack-tecnológico)
   - [1.3 Arquitectura de Monolito Modular](#13-arquitectura-de-monolito-modular)
2. [Principios Arquitectónicos Rectores](#2-principios-arquitectónicos-rectores)
3. [Arquitectura de Eventos y Tiempo Real](#3-arquitectura-de-eventos-y-tiempo-real)
4. [Seguridad y Cumplimiento Legal](#4-seguridad-y-cumplimiento-legal)
5. [Estrategia Móvil](#5-estrategia-móvil)
6. [Roadmap de Implementación por Fases](#6-roadmap-de-implementación-por-fases)
7. [Fase 1 — MVP Entregable](#7-fase-1--mvp-entregable)
8. [Fase 2 — Madurez Operativa](#8-fase-2--madurez-operativa)
9. [Fase 3 — Cumplimiento Fiscal](#9-fase-3--cumplimiento-fiscal)
10. [Fase 4 — Inteligencia de Negocio](#10-fase-4--inteligencia-de-negocio)
11. [Fase 5 — Ecosistema Extendido](#11-fase-5--ecosistema-extendido)
12. [Resumen de Capacidades por Fase](#12-resumen-de-capacidades-por-fase)
13. [Apéndice A — Normativa Mexicana de Referencia](#13-apéndice-a--normativa-mexicana-de-referencia)
14. [Apéndice B — Registro de Decisiones de Arquitectura (ADR)](#14-apéndice-b--registro-de-decisiones-de-arquitectura-adr)

---

## 1. Contexto y Objetivo

Este documento define la arquitectura, alcance funcional y plan de implementación por fases del sistema de gestión de consultorios médicos. El sistema es una plataforma SaaS multi-tenant diseñada para cubrir la operación integral de consultorios médicos bajo la normativa mexicana vigente (NOM-004-SSA3-2012 y disposiciones complementarias).

El objetivo central es construir una solución donde la operación clínica diaria y la contabilidad sean la misma cosa: cada acto clínico produce automáticamente su reflejo financiero, sin intervención manual y sin que el médico necesite conocimientos contables. Simultáneamente, el contador dispone de un Libro Mayor técnico con toda la trazabilidad necesaria para auditorías y declaraciones fiscales.

### 1.1 Jerarquía de Entidades

La unidad base del sistema es la clínica. La arquitectura anticipa la agregación de entidades en la siguiente jerarquía:

- Usuario Administrador (dueño)
  - Corporación Médica
    - Clínica / Consultorio
      - Sucursal (expansión futura)
      - Hospital (expansión futura)

### 1.2 Stack Tecnológico

Esta sección documenta las decisiones tecnológicas adoptadas para la Fase 1 MVP. Las decisiones de stack son vinculantes para el equipo de desarrollo — cualquier cambio requiere actualizar este documento y registrar el razonamiento en el Apéndice B (ADR).

#### 1.2.1 Backend

| Componente | Tecnología | Justificación |
|---|---|---|
| Lenguaje | Java 21 LTS | Virtual threads (Project Loom) para alta concurrencia sin código reactivo; records y sealed classes reducen boilerplate del dominio contable; soporte empresarial de largo plazo |
| Framework | Spring Boot 3.3.x | Ecosistema maduro con Spring Security, Spring AMQP y Spring Data integrados; autoconfiguración reduce setup; amplia disponibilidad de talento |
| Arquitectura | Monolito modular | Ver §1.3 — módulos con fronteras bien definidas, preparado para extracción a microservicios en F3/F4 |
| Autenticación | Spring Security 6.x + OAuth 2.0 + JWT | Spring Authorization Server para emisión de tokens; Resource Server para validación; estándar de la industria con soporte nativo en Spring Boot 3.x. Ver §4.1 |
| Persistencia | Spring Data JPA + Hibernate 6.x | Soporte completo de PostgreSQL 16; tipos ENUM nativos vía `@Enumerated`; `@Embeddable` para value objects del dominio contable |
| Migraciones BD | Flyway | Versionado de esquema en código; integrado con Spring Boot; garantiza reproducibilidad entre ambientes |
| Broker de mensajes | RabbitMQ 3.13 + Spring AMQP | Comunicación asíncrona del ARE; durable queues + Dead Letter Exchange para garantías de entrega; preparado para separación en microservicios. Ver §3.1 |
| Tiempo real (SSE) | Spring MVC `SseEmitter` | Emisión de eventos server-to-client sobre HTTP/1.1; sin dependencias adicionales al stack Spring |
| Mapping DTO | MapStruct 1.6 | Generación de código en compile-time; cero reflection en runtime; reduce boilerplate de conversión entre capas |
| Reducción boilerplate | Lombok | `@Builder`, `@Value`, `@Slf4j` en capas de infraestructura; no se usa en clases de dominio para mantener explícito el modelo |
| Testing | JUnit 5 + Testcontainers + Mockito | Testcontainers levanta PostgreSQL y RabbitMQ reales en contenedores para tests de integración; sin mocks de base de datos |

#### 1.2.2 Frontend

| Componente | Tecnología | Justificación |
|---|---|---|
| Framework | React 18 + TypeScript | Mayor disponibilidad de talento; concurrent features para UX fluida en el Canvas |
| Build tool | Vite 5.x | Arranque en desarrollo < 500 ms; HMR instantáneo; soporte nativo de PWA con `vite-plugin-pwa` |
| Estado global | Zustand | Minimal, sin boilerplate; suficiente para el scope de F1 |
| Queries / caché | TanStack Query v5 | Gestión de estado del servidor, revalidación automática, optimistic updates para el Canvas |
| Routing | React Router v6 | Estándar del ecosistema; data loaders para pre-fetch |
| UI base | Sistema de diseño propio (ver Especificación UI/UX v2.0) | Componentes construidos sobre Radix UI primitives — accesibilidad WCAG 2.1 AA sin costo adicional |
| Iconos | Tabler Icons (outline) | Definido en Especificación UI/UX v2.0 |
| Tiempo real | `EventSource` nativo del browser (SSE) | Ver §3.2 |

#### 1.2.3 Infraestructura

| Componente | Tecnología | Justificación |
|---|---|---|
| Base de datos | PostgreSQL 16 | Definido en Modelo de Datos v2.0; RLS nativo para multi-tenancy |
| Broker de mensajes | RabbitMQ 3.13 | Comunicación asíncrona entre módulos del ARE; durable queues con Dead Letter Exchange; panel de administración incluido |
| Object storage | Cloudflare R2 | Sin costos de egress — crítico para fotos clínicas de alta resolución; compatible con API S3; CDN incluido |
| Hosting backend | Railway (F1) → AWS ECS Fargate (F2+) | Railway permite desplegar el JAR de Spring Boot y el servicio de RabbitMQ con mínimo setup; migración a ECS cuando el volumen lo justifique |
| Hosting frontend | Cloudflare Pages | Deploy automático desde Git; CDN global; soporte nativo de PWA |
| Región primaria | us-central1 / Dallas TX | Latencia < 30 ms desde CDMX, Guadalajara y Monterrey |
| Email transaccional | Resend | API simple, alta entregabilidad en México; se integra desde Spring con JavaMailSender o cliente HTTP directo |
| Monitoreo | Spring Boot Actuator + Grafana Cloud + Sentry | Actuator expone métricas de RabbitMQ, JVM y ARE; Grafana las visualiza; Sentry captura excepciones no controladas |
| CI/CD | GitHub Actions | Workflow: compile → test (Testcontainers) → build JAR → Docker image → deploy. Secrets gestionados en GitHub |

#### 1.2.4 Seguridad de Infraestructura

- Todas las comunicaciones vía HTTPS / TLS 1.3 mínimo.
- Variables de entorno gestionadas como secrets en Railway y GitHub Actions — nunca en el repositorio de código.
- La base de datos no está expuesta a internet público — acceso solo desde la VPC del backend.
- Backups automáticos diarios de PostgreSQL con retención de 30 días.
- Credenciales de R2 con permisos de mínimo privilegio separadas por bucket (fotos clínicas / avatares / logos).

---

## 1.3 Arquitectura de Monolito Modular

El sistema se construye como un **monolito modular** — una única unidad de despliegue con fronteras de módulo bien definidas internamente. Esta decisión equilibra la simplicidad operativa de F1 con la capacidad de migrar a microservicios en fases futuras sin reescritura estructural.

### Por qué no microservicios desde el inicio

Los microservicios resuelven problemas de escala y equipos independientes que no existen en F1. Introducirlos prematuramente añade complejidad operativa (service discovery, tracing distribuido, eventual consistency cross-service) que ralentizaría el desarrollo sin aportar valor. El monolito modular ofrece la misma disciplina de separación de responsabilidades con cero sobrecarga operativa adicional.

### Estructura de paquetes

Cada módulo de dominio tiene su propio paquete raíz y expone solo su API pública. El acceso cruzado entre módulos está prohibido a nivel de paquete privado y verificado en tiempo de compilación mediante **ArchUnit**.

```
com.jclinical/
├── auth/               # Autenticación, sesiones, invitaciones
├── core/               # Clínicas, organizaciones, personal, alertas
├── patients/           # Catálogo de pacientes
├── clinical/           # Expediente, documentos, canvas, firmas
├── scheduling/         # Agenda y citas
├── treatments/         # Planes de tratamiento y catálogo de procedimientos
├── inventory/          # Productos y kardex
├── cash/               # Caja, sesiones y cobros
├── accounting/         # ARE, Libro Mayor, catálogo de cuentas
└── shared/             # Value objects, excepciones base, utilidades cross-cutting
```

Cada módulo sigue internamente una estructura de tres capas:

```
{modulo}/
├── application/        # Casos de uso, DTOs de entrada/salida, facades públicos
├── domain/             # Entidades, value objects, eventos de dominio, repositorios (interfaces)
└── infrastructure/     # Repositorios JPA, publishers RabbitMQ, adapters externos
```

### Reglas de comunicación entre módulos

| Tipo de comunicación | Mecanismo | Cuándo usarlo |
|---|---|---|
| Síncrona (intra-proceso) | Llamada directa al facade público del módulo destino | Cuando el resultado se necesita en la misma transacción |
| Asíncrona (intra-proceso) | `ApplicationEventPublisher` de Spring | Notificaciones de bajo acoplamiento sin garantía de orden |
| Asíncrona (para el ARE) | RabbitMQ vía Spring AMQP | Eventos de dominio que generan pólizas contables — requieren garantía de entrega |

**Regla invariable:** el módulo `accounting` nunca recibe llamadas síncronas de módulos operativos. Toda comunicación hacia el ARE es exclusivamente a través de RabbitMQ.

### Preparación para microservicios (F3/F4)

El monolito modular facilita la extracción futura porque:

1. **RabbitMQ ya es el canal de comunicación asíncrona** — al extraer un módulo, solo cambia quién publica/consume en el exchange, no el contrato del mensaje.
2. **Cada módulo tiene su propio schema de PostgreSQL** — la extracción implica apuntar el módulo a su propia base de datos sin refactorizar queries entre schemas.
3. **Los facades son la API pública** — se convierten en endpoints REST o gRPC del microservicio sin cambiar la firma.
4. **ArchUnit garantiza** que no existen dependencias circulares ni accesos directos a infraestructura de otro módulo que impidan la separación.

El orden probable de extracción si se llega a ese punto: `accounting` primero (alta carga de procesamiento del ARE), luego `clinical` (datos sensibles que podrían requerir compliance independiente), luego `scheduling`.

---

## 2. Principios Arquitectónicos Rectores

### 2.1 Separación entre Operación Clínica y Contabilidad

Los módulos operativos (agenda, inventario, caja) nunca escriben asientos contables directamente. Publican eventos de dominio que el Motor de Reglas Contables (ARE) consume de forma asíncrona para generar las pólizas correspondientes en el Libro Mayor. Toda operación clínica tiene su reflejo financiero automático e inmutable sin intervención manual. Ver §3.1 para la implementación técnica del ARE.

### 2.2 Multi-tenancy Jerárquico

Cada clínica tiene su propia configuración operativa: catálogo de cuentas, personal, inventario, pacientes y caja. El Catálogo de Cuentas (CoA) es configurable por tenant pero hereda una estructura base no modificable, garantizando consistencia en reportes corporativos. La separación entre tenants se aplica en dos capas independientes:

- **Capa de aplicación:** la cabecera `X-Clinic-ID` se valida en cada request y se inyecta en el contexto de la sesión de base de datos.
- **Capa de base de datos:** Row Level Security (RLS) en PostgreSQL filtra automáticamente todas las queries. Un bug en la capa de aplicación no puede exponer datos de otro tenant.

### 2.3 Norma Oficial Mexicana como Restricción de Diseño

El sistema no valida contra la norma a posteriori. La norma está embebida en las plantillas, formularios y flujos de trabajo. Es estructuralmente imposible que un médico genere un expediente o historia clínica que incumpla la NOM vigente. Las restricciones normativas se marcan visualmente en la UI con el badge `NOM-004` y los bloques correspondientes no pueden eliminarse del Canvas.

### 2.4 Doble Audiencia en toda Interfaz Financiera

| Audiencia | Representación | Ejemplo |
|---|---|---|
| Médico / Recepcionista | Lenguaje natural operativo | "Se registró el cobro de $2,800" |
| Contador / Auditor | Vista técnica del Libro Mayor | Póliza #0041 — Déb. 11100 $2,800 / Créd. 41000 $2,800 |

El mismo módulo financiero soporta ambas representaciones sin duplicar interfaces. El toggle entre vistas está disponible para roles con acceso contable.

### 2.5 Inmutabilidad de Pólizas Contables

Ninguna póliza puede modificarse una vez registrada. Toda corrección produce una nueva póliza de reverso referenciada a la original. Las cancelaciones generan pólizas de ajuste con saldo negativo. Esta garantía se implementa a nivel de **trigger en PostgreSQL** — no es una convención de código que pueda omitirse por error.

### 2.6 Validación de Integridad en Partida Doble

Antes de persistir cualquier conjunto de líneas de asiento, un trigger `CONSTRAINT DEFERRABLE INITIALLY DEFERRED` valida que `Σ Débitos = Σ Créditos` al momento del `COMMIT`. Si la condición falla, la transacción completa hace `ROLLBACK` y el ARE registra el intento fallido en `accounting.domain_events` con `event_type = 'JournalBalanceError'`, `processed_at = NULL` y el detalle en `processing_error`.

### 2.7 Privacidad y Protección de Datos (LFPDPPP)

Los expedientes clínicos contienen datos personales sensibles en términos de la **Ley Federal de Protección de Datos Personales en Posesión de los Particulares (LFPDPPP)** y su Reglamento. El sistema opera bajo el esquema responsable–encargado:

- **Responsable del tratamiento:** la clínica (persona física o moral propietaria del consultorio). Es quien determina el propósito y medios del tratamiento de los datos del paciente.
- **Encargado del tratamiento:** JClinical como proveedor de la plataforma SaaS. Trata los datos únicamente bajo instrucción del Responsable.

Esta distinción tiene consecuencias directas en el diseño: el sistema no puede usar datos de pacientes de una clínica para ningún fin ajeno a esa clínica — ni analytics agregados sin anonimización, ni entrenamiento de modelos, ni transferencia a terceros.

#### Obligaciones implementadas en el sistema

| Obligación LFPDPPP | Implementación técnica |
|---|---|
| Aviso de privacidad | Cada clínica configura la URL de su aviso en `core.clinics.privacy_notice_url`. El sistema muestra y registra el consentimiento del paciente al crear su expediente |
| Consentimiento explícito | El flujo de registro de paciente incluye aceptación obligatoria del aviso de privacidad. Se registran `patients.privacy_consent_at` y `patients.privacy_consent_ip` |
| Retención mínima (NOM-004) | Los expedientes se conservan mínimo 5 años desde la última consulta. `patients.data_retention_until` se calcula y actualiza automáticamente con cada cita completada |
| Derecho ARCO | `POST /patients/{id}/arco-request` registra solicitudes de Acceso, Rectificación, Cancelación u Oposición. Se genera una alerta para la clínica con el plazo legal (20 días hábiles) |
| Encriptación en reposo | Las columnas `curp`, `content_json` (documentos clínicos) y `signature_url` se encriptan con AES-256-GCM a nivel de aplicación. La clave se gestiona en un KMS externo (ver ADR-005) |
| Transferencias a terceros | JClinical no transfiere datos a terceros sin consentimiento explícito. El Acuerdo de Encargado con cada clínica documenta las obligaciones de confidencialidad |
| Violaciones de seguridad | Ante una brecha, el protocolo de notificación al INAI y a los titulares se activa en menos de 72 horas. El procedimiento se documenta en el runbook de incidentes |

#### Acuerdo de Encargado

Al crear una cuenta de clínica, el administrador acepta los Términos de Servicio que incluyen el Acuerdo de Encargado de Datos. La fecha de aceptación queda registrada en `core.clinics.data_processor_agreed_at`.

---

## 3. Arquitectura de Eventos y Tiempo Real

### 3.1 Motor de Reglas Contables (ARE)

El ARE es un `@RabbitListener` de Spring AMQP que corre dentro del mismo proceso Spring Boot pero en su propio hilo virtual (Java 21 Project Loom). Consume mensajes del exchange `jclinical.domain.events` y genera pólizas contables en el Libro Mayor.

#### Topología de RabbitMQ

```
Exchange: jclinical.domain.events  (tipo: topic, durable)
  │
  ├── routing key: appointment.confirmed   →  queue: are.appointment.confirmed
  ├── routing key: payment.registered      →  queue: are.payment.registered
  ├── routing key: consumption.reconciled  →  queue: are.consumption.reconciled
  ├── routing key: appointment.cancelled   →  queue: are.appointment.cancelled
  ├── routing key: supply.added            →  queue: are.supply.added
  └── routing key: supply.wasted           →  queue: are.supply.wasted

Dead Letter Exchange: jclinical.dlx  (tipo: direct, durable)
  └── Dead Letter Queue: are.dlq          (mensajes fallidos tras 3 reintentos)
```

Todas las queues son **durable** y los mensajes se publican con `deliveryMode = PERSISTENT`. Si RabbitMQ se reinicia, los mensajes no procesados se conservan en disco.

#### Flujo de un evento de dominio

```
Módulo operativo (ej. Caja — cash module)
  │
  ├── Abre transacción de BD
  ├── Inserta cash.payments
  ├── Inserta accounting.domain_events (processed_at = NULL)
  ├── Hace COMMIT de la transacción
  └── Publica mensaje en RabbitMQ vía RabbitTemplate
        routing key: "payment.registered"
        payload: { eventId, clinicId, eventType, aggregateId, occurredAt }
              │
              └──► ARE @RabbitListener (hilo virtual Java 21)
                     │
                     ├── Extrae el eventId del mensaje
                     ├── Lee el domain_event completo de la BD (con el payload JSONB)
                     ├── Aplica la regla contable correspondiente
                     ├── Abre transacción de BD
                     ├── Inserta journal_entry + journal_lines
                     ├── Trigger DEFERRABLE valida Σ Déb = Σ Créd al COMMIT
                     ├── Actualiza domain_events.processed_at = now()
                     ├── Hace COMMIT → ACK al broker
                     └── Si falla: NACK → RabbitMQ reintenta (máx. 3 veces)
                                  Tras 3 fallos → mensaje va al are.dlq
                                  domain_events.processing_error = detalle
```

**Nota sobre la secuencia COMMIT → publish:** la publicación a RabbitMQ ocurre **después** del COMMIT de la transacción de BD. Esto elimina el caso en que el mensaje llega al ARE antes de que el registro exista en BD. Si el proceso cae entre el COMMIT y el publish, el `domain_events.processed_at` queda NULL — un job programado de reconciliación escanea estos registros cada 5 minutos y republica los mensajes huérfanos.

#### Política de reintentos

| Intento | Delay antes de reintentar |
|---|---|
| 1° reintento | 5 segundos |
| 2° reintento | 30 segundos |
| 3° reintento | 2 minutos |
| Tras 3 fallos | Mensaje → `are.dlq` + `system_alert` de tipo `are_processing_error` |

#### Garantías del ARE

| Garantía | Implementación |
|---|---|
| Al-menos-una-vez | RabbitMQ reencola el mensaje si el consumer no hace ACK. Si el pod cae, el mensaje se reencola automáticamente |
| Idempotencia | El ARE verifica que no exista una `journal_entry` con el mismo `domain_event_id` antes de crear una nueva. Si ya existe, hace ACK sin crear duplicado |
| Latencia esperada | < 300 ms desde el publish hasta la póliza creada en condiciones normales |
| Monitoreo | Spring Boot Actuator expone métricas de profundidad de cola; Grafana alerta si `are.*.queue` supera 50 mensajes acumulados |

#### Ventaja para futura migración a microservicios

Con RabbitMQ ya como broker, extraer el módulo `accounting` a un microservicio independiente solo requiere apuntar su consumer a la misma queue existente. El contrato del mensaje y la topología del exchange no cambian. Ver ADR-001.

### 3.2 Actualizaciones en Tiempo Real (SSE)

La agenda, las alertas y el estado de citas deben actualizarse en la UI sin que el usuario recargue la página. El sistema usa **Server-Sent Events (SSE)** — comunicación unidireccional servidor → cliente, sobre HTTP/1.1, sin la complejidad operativa de WebSockets. Ver ADR-003.

#### Endpoint de suscripción

```
GET /v1/events/stream
Headers requeridos:
  Authorization: Bearer <access_token>
  X-Clinic-ID:   <uuid>
Respuesta:
  Content-Type: text/event-stream
  Cache-Control: no-cache
  Connection: keep-alive
```

El servidor mantiene la conexión abierta y emite eventos cuando ocurren. El cliente usa `EventSource` nativo del browser, que reconecta automáticamente si la conexión se pierde.

#### Catálogo de eventos SSE

| Evento | Disparador | Payload mínimo |
|---|---|---|
| `appointment.status_changed` | Cambio de estado de cualquier cita | `{ appointment_id, status, patient_name }` |
| `appointment.created` | Nueva cita agendada | `{ appointment_id, scheduled_start, doctor_id }` |
| `alert.created` | Nueva alerta del sistema | `{ alert_id, alert_type, severity, title }` |
| `alert.resolved` | Alerta resuelta o descartada | `{ alert_id }` |
| `stock.low_warning` | Stock cayó bajo el mínimo configurado | `{ product_id, product_name, stock_current, stock_min }` |
| `payment.registered` | Cobro registrado en caja | `{ payment_id, patient_name, amount_cents }` |
| `are.processing_error` | ARE falló al procesar un evento | `{ domain_event_id, event_type, error }` |

#### Comportamiento del cliente

- Al recibir un evento SSE, el cliente invalida la query correspondiente en TanStack Query, que la re-fetches automáticamente. El payload del SSE es solo una señal de invalidación — la fuente de verdad siempre es el REST API.
- En tablet y móvil, la conexión SSE se pausa cuando la pestaña pierde el foco (`visibilitychange`) y se reanuda al recuperarlo, para no consumir batería en segundo plano.
- Si la reconexión falla más de 3 veces seguidas, se muestra un indicador visual de "sin conexión" en el topbar y el usuario puede forzar recarga manual.

---

## 4. Seguridad y Cumplimiento

### 4.1 Autenticación y Gestión de Sesiones — Spring Security + OAuth 2.0 + JWT

El sistema implementa OAuth 2.0 con el flujo **Resource Owner Password** para aplicaciones de primera parte (la propia SPA), usando **Spring Authorization Server** como servidor de autorización self-hosted y **Spring Security Resource Server** para validar tokens en cada request. Ver ADR-002.

#### Flujo de autenticación

```
Frontend (SPA)
  │
  └── POST /oauth2/token  (grant_type=password, username, password, scope)
            │
            └──► Spring Authorization Server
                   ├── Valida credenciales contra core.users (BCrypt)
                   ├── Genera access_token (JWT firmado, 1 hora)
                   ├── Genera refresh_token (opaco, almacenado en BD, 30 días)
                   └── Responde con { access_token, refresh_token, expires_in, token_type }

Requests subsiguientes:
  Authorization: Bearer <access_token>
  X-Clinic-ID:   <uuid>
        │
        └──► Spring Security Resource Server
               ├── Valida firma JWT con clave pública del Authorization Server
               ├── Verifica expiración y claims
               └── Inyecta SecurityContext con usuario y permisos
```

#### Estructura del JWT (access_token)

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

El `clinic_id` activo **no** se incluye en el JWT — se lee de la cabecera `X-Clinic-ID` en cada request y se valida contra el array `clinics` del token. Esto evita emitir un token por clínica y permite cambio de contexto sin re-autenticación.

#### Tokens y almacenamiento

| Token | Duración | Almacenamiento en cliente | Tipo |
|---|---|---|---|
| `access_token` | 1 hora | Memoria del proceso JS (no `localStorage`) | JWT firmado (RS256) |
| `refresh_token` | 30 días | Cookie `HttpOnly; SameSite=Strict; Secure` | Opaco — referencia a registro en BD |

Los `refresh_tokens` opacos se persisten en `core.refresh_tokens`, lo que permite:
- Invalidación inmediata al hacer logout (el token opaco se borra de BD).
- `POST /auth/logout-all` para revocar todas las sesiones activas del usuario.
- Auditoría de dispositivos con sesión activa (IP, user-agent, último uso).

#### Configuración Spring Security (referencia)

```java
// Resource Server — valida JWTs en cada request
@Bean
SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
    return http
        .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/v1/auth/**").permitAll()
            .anyRequest().authenticated())
        .build();
}

// Extractor de clinic_id desde cabecera X-Clinic-ID
@Component
public class ClinicContextFilter extends OncePerRequestFilter {
    // Valida X-Clinic-ID contra los clinics del JWT y setea en ClinicContext (ThreadLocal)
}
```

#### Tablas de soporte de autenticación (Modelo de Datos v2.0)

| Tabla | Propósito |
|---|---|
| `core.refresh_tokens` | Tokens opacos activos por usuario y dispositivo (referenciados desde la cookie) |
| `core.email_verification_tokens` | Tokens de verificación de correo (uso único, expiran en 24h) |
| `core.password_reset_tokens` | Tokens de restablecimiento de contraseña (uso único, expiran en 1h) |
| `core.staff_invitations` | Invitaciones por correo al agregar nuevo personal a una clínica |
| `core.user_sessions` | Historial de sesiones para auditoría y `logout-all` |

#### Política de contraseñas

- Mínimo 10 caracteres.
- Al menos 1 mayúscula, 1 minúscula, 1 número.
- Sin repetición de las últimas 5 contraseñas (historial almacenado como hash).
- Hash con `BCryptPasswordEncoder`, strength 12.

#### Protecciones adicionales

- Rate limiting en `/oauth2/token` y `/v1/auth/*`: 10 solicitudes/minuto por IP (filtro Spring Security).
- Bloqueo temporal de 15 minutos tras 5 intentos fallidos consecutivos (campo `failed_login_attempts` en `core.users`).
- Los tokens de reset y verificación son de uso único — se invalidan inmediatamente al usarse.
- Los refresh tokens rotativos: al usar un refresh token, se emite uno nuevo y el anterior se invalida.

### 4.2 Control de Acceso (RBAC)

Los permisos se evalúan en dos capas independientes:

1. **Middleware de API:** cada endpoint verifica que el rol del usuario en la clínica activa tenga el permiso requerido antes de ejecutar cualquier lógica.
2. **RLS en PostgreSQL:** las políticas de Row Level Security aplican `clinic_id` de forma independiente a la capa de aplicación. Un error en el middleware no puede exponer datos de otro tenant.

| Rol | Capacidades principales | Restricciones clave |
|---|---|---|
| `admin` | Control total de la plataforma | Ninguna |
| `clinic_admin` | Administración completa de su clínica | Sin acceso a otras clínicas |
| `doctor` | Expedientes, citas, tratamientos, conciliación de insumos | Sin acceso a nómina ni Libro Mayor |
| `receptionist` | Agenda, caja, catálogo de pacientes | Sin acceso al expediente clínico completo |
| `assistant` | Apoyo clínico, conciliación de insumos, agenda | Sin acceso a caja ni Libro Mayor |
| `accountant` | Libro Mayor, CoA, reportes financieros | Solo lectura en módulos clínicos |
| `cleaning` | Sin acceso a módulos operativos | Solo registro de asistencia |

### 4.3 Encriptación

| Dato | En tránsito | En reposo |
|---|---|---|
| Toda comunicación API | TLS 1.3 obligatorio | — |
| Fotos clínicas en R2 | TLS en upload/download | AES-256 nativo de R2 |
| `curp` en base de datos | TLS en conexión PostgreSQL | AES-256-GCM nivel aplicación |
| `content_json` (documentos clínicos) | TLS | AES-256-GCM nivel aplicación |
| `signature_url` (firmas digitales) | TLS | AES-256-GCM nivel aplicación |
| `password_hash` | — | bcrypt cost factor 12 |
| Tokens de sesión y reset | TLS | SHA-256 hash antes de persistir |

La clave de encriptación a nivel aplicación se obtiene de un KMS externo al arrancar el proceso. Nunca se persiste en código ni en variables de entorno del repositorio. Ver ADR-005.

### 4.4 Auditoría

`audit.audit_log` es append-only e inmutable. Registra toda acción relevante: accesos, creaciones, modificaciones, exportaciones, firmas digitales y cambios de rol. Los campos `old_values` y `new_values` en JSONB permiten reconstruir el estado de cualquier entidad en cualquier punto del tiempo.

Los registros de auditoría se insertan dentro de la **misma transacción** que la operación que los origina — nunca de forma asíncrona. Si la operación hace rollback, el registro de auditoría también se revierte. Esto garantiza coherencia total entre las acciones y su traza de auditoría.

---

## 5. Estrategia Móvil

### 5.1 Fase 1 — PWA (Progressive Web App)

En F1 el sistema se despliega como PWA. Esto no requiere desarrollo de una app nativa separada: la configuración de `manifest.json` y un service worker básico, gestionados por `vite-plugin-pwa`, son suficientes.

| Capacidad | Disponible en F1 (PWA) |
|---|---|
| Instalable en homescreen (iOS y Android) | ✅ |
| Funciona con conexión lenta o inestable | ✅ (caché de lectura) |
| Lectura de expediente sin conexión | ✅ (caché service worker) |
| Firma digital del paciente en tablet | ✅ |
| Notificaciones push de confirmación de citas | ✅ (Web Push API) |
| Agenda (lectura) | ✅ en todas las pantallas |
| Agenda (edición), Canvas, Caja, Libro Mayor | Solo en pantallas ≥ 768 px |

La restricción de edición en pantallas pequeñas se debe al área mínima requerida para operar los módulos de forma eficiente, no a una limitación técnica de la PWA.

### 5.2 Fase 2 — App Nativa (Decisión Diferida)

Si el análisis de uso post-lanzamiento muestra que los médicos necesitan funcionalidad de edición en móvil (no solo lectura), se evaluará una app **React Native** que reutilice los tipos TypeScript y la lógica de dominio del frontend web. La decisión se toma en F2 con datos reales de uso — no antes. Ver ADR-004.

### 5.3 Matriz de Soporte por Plataforma

| Plataforma | F1 PWA | F2+ (App nativa, si aplica) |
|---|---|---|
| iPadOS — Safari 17+ (≥ 768 px) | Funcionalidad completa | ✅ |
| Android — Chrome 120+ (≥ 768 px) | Funcionalidad completa | ✅ |
| iOS Safari 17+ — iPhone (< 768 px) | Solo lectura + firma digital | App nativa React Native |
| Android Chrome 120+ — teléfono (< 768 px) | Solo lectura + firma digital | App nativa React Native |
| Desktop — Chrome / Edge / Safari 120+ | Funcionalidad completa | — |

---

## 6. Roadmap de Implementación por Fases

| Fase | Nombre | Objetivo principal | Horizonte |
|---|---|---|---|
| Fase 1 | MVP Entregable | Operación clínica funcional, contabilidad interna básica y CFDI puente | Alcance inicial |
| Fase 2 | Madurez Operativa | Inventario avanzado, nómina, CxP/CxC, presupuestación | Post-MVP |
| Fase 3 | Cumplimiento Fiscal | CFDI 4.0 completo, contabilidad electrónica SAT, conciliación bancaria | Mediano plazo |
| Fase 4 | Inteligencia de Negocio | BI, KPIs, proyecciones, benchmarking entre clínicas | Largo plazo |
| Fase 5 | Ecosistema Extendido | Portal del paciente, aseguradoras, escala hospitalaria | Visión futura |

---

## 7. Fase 1 — MVP Entregable

Esta fase constituye el primer producto entregable. El objetivo es que el médico pueda operar su clínica de forma completa: gestionar pacientes, expedientes, citas, inventario básico y caja, con un reflejo contable interno automático y capacidad básica de facturación electrónica.

### 7.1 Gestión Multi-Clínica

- Alta y administración de 1 a n clínicas por usuario administrador.
- Cada clínica con configuración independiente: nombre, dirección, RFC, logotipo, especialidades, zona horaria.
- Cambio de contexto entre clínicas desde la barra de navegación principal.
- Estructura de datos preparada para jerarquía corporativa; la UI corporativa se implementa en Fase 5.

### 7.2 Gestión de Pacientes

- Catálogo de pacientes con búsqueda por nombre, CURP, fecha de nacimiento y número de expediente.
- Alta y baja lógica de pacientes (`soft delete`).
- Registro y almacenamiento del consentimiento del aviso de privacidad (LFPDPPP §2.7) al crear el expediente.
- Desde el perfil del paciente, acceso con un clic a: Historia Clínica, Expediente, Citas y Tratamientos y Presupuestos.
- Ficha de identificación conforme a los requisitos mínimos de NOM-004-SSA3-2012.

### 7.3 Expediente Clínico (NOM-004-SSA3-2012)

El expediente agrupa los siguientes tipos de documentos, todos creables desde la herramienta Canvas:

| Documento | Normativa de referencia | Incluido en Fase 1 |
|---|---|---|
| Historia Clínica | NOM-004-SSA3-2012 | ✅ |
| Notas Médicas | NOM-004-SSA3-2012 | ✅ |
| Consentimientos Informados | NOM-004-SSA3-2012 | ✅ |
| Recetas | NOM-072-SSA1-2012 | ✅ |
| Presupuestos y Tratamientos | Operativo | ✅ |
| Fotografías Clínicas | Operativo | ✅ |
| Estudios de Laboratorio | NOM-004-SSA3-2012 | ✅ |
| Estudios de Imagen | NOM-004-SSA3-2012 | ✅ |
| Interconsultas | NOM-004-SSA3-2012 | ✅ |
| Referencias y Contrarreferencias | NOM-004-SSA3-2012 | ✅ |
| Hojas de Enfermería | NOM-004-SSA3-2012 | ✅ |
| Documentos Administrativos | Operativo | ✅ |

#### 7.3.1 Herramienta Canvas para Documentos Clínicos

Barra de herramientas con los siguientes componentes arrastrables:

- Cajas de texto libres.
- Tablas preconstruidas: signos vitales, antecedentes heredofamiliares, alergias, medicamentos actuales, revisión por aparatos y sistemas.
- Tablas vacías configurables (columnas y filas definidas por el médico).
- Bloques de imagen (fotografías clínicas, estudios).
- Campos de firma digital (médico y paciente).
- Bloques de fecha/hora automáticos.

**Restricción de diseño:** los bloques de cumplimiento normativo están marcados con el badge `NOM-004` y bloqueados para eliminación. El médico puede extender los documentos pero no puede omitir campos obligatorios.

#### 7.3.2 Historia Clínica

Estructura obligatoria conforme a NOM-004-SSA3-2012:

1. Ficha de identificación completa
2. Motivo de consulta
3. Interrogatorio y antecedentes (hereditarios, patológicos, no patológicos, gineco-obstétricos cuando aplique)
4. Exploración física con signos vitales
5. Diagnóstico presuntivo y definitivo con códigos CIE-10
6. Pronóstico
7. Plan de tratamiento
8. Evolución médica

### 7.4 Tratamientos, Citas e Inventario Básico

#### 7.4.1 Plan de Tratamiento y Cotización

- Sección "Tratamientos y Presupuestos" en el perfil de cada paciente.
- El médico construye el presupuesto a partir del catálogo de procedimientos de la clínica.
- Cada procedimiento tiene asociada su lista de insumos con precios tomados del inventario en tiempo real.

#### 7.4.2 Ciclo de Vida de Insumos en Tratamiento

| Estado | Disparador | Efecto en Inventario | Efecto Contable |
|---|---|---|---|
| Cotizado | Médico crea el presupuesto | Sin movimiento | Sin asiento |
| Reservado | Médico confirma la cita | Disponible → Reservado (atómico) | Sin asiento aún |
| Consumido | Conciliación post-cita | Reservado → Baja definitiva | ARE asienta Momento 2 |

Las cantidades se registran en `NUMERIC(10,4)` para soportar mililitros, gramos y piezas fraccionadas. La transición entre estados es atómica — no existe estado intermedio visible en la base de datos.

#### 7.4.3 Gestión de Citas

- Agenda visual con vistas diaria, semanal y mensual.
- Una cita puede contener de 0 a n procedimientos del plan de tratamiento activo.
- Al confirmar la cita: el stock de insumos requeridos pasa a `reservado` de forma atómica junto con la publicación del evento `CitaConfirmada`.
- Al cancelar: el stock reservado regresa a disponible de forma atómica. Si hubo cobro, el ARE genera el reverso contable correspondiente.

### 7.5 Inventario Básico

- Alta, baja y modificación de productos e insumos.
- Entradas de inventario con costo unitario — actualizan el precio promedio del producto.
- Alertas configurables de stock mínimo y stock máximo por producto.
- Alertas de caducidad para productos perecederos (días de anticipación configurables por producto).
- Kardex básico: registro cronológico de entradas, salidas y reservas con snapshot de stock en cada movimiento.
- Ajustes de inventario por merma o caducidad — generan el evento de dominio `MermaCaducidad` para el ARE.

El soporte para valuación PEPS/UEPS/Costo Promedio Ponderado, trazabilidad por lote y órdenes de compra se implementa en Fase 2.

### 7.6 Gestión de Flujos de Caja

- Apertura y cierre de caja con cuadre de turno.
- Registro de pagos: contado, anticipos, abonos parciales a tratamientos.
- Generación de tickets de venta con número secuencial único por clínica.
- Control de múltiples formas de pago: efectivo, transferencia, tarjeta, cheque.
- Todos los movimientos de caja publican eventos que el ARE procesa para el Libro Mayor interno.

#### 7.6.1 Estrategia Puente — CFDI Básico en Fase 1

La facturación electrónica completa (CFDI 4.0 con ciclo de vida completo, notas de crédito y Anexo 24 SAT) forma parte de la Fase 3. Sin embargo, para que los consultorios no requieran un sistema externo para emitir facturas desde el primer día, la Fase 1 incluye una **integración básica con PAC**:

**Alcance de la integración básica:**
- Integración con **Finkok** o **SW Sapien** vía API REST (ambos ofrecen sandbox gratuito para desarrollo).
- El sistema genera el XML del CFDI 4.0 a partir de los datos del ticket de venta y los datos fiscales de la clínica.
- El XML se envía al PAC para timbrado; se almacenan el UUID fiscal, la cadena original y el PDF resultante en object storage.
- La UI muestra el estado del CFDI (`pendiente` / `timbrado` / `error` / `cancelado`) y permite descarga del PDF.
- Cancelación básica disponible (sin solicitud de aceptación del receptor ni flujo de sustitución — esos son Fase 3).
- El número de serie y folio se configuran por clínica en el módulo de Configuración.

**Lo que no cubre esta integración (queda para Fase 3):**
- Complemento de pago (REP) para pagos diferidos.
- Notas de crédito y notas de cargo.
- Sustitución de CFDIs cancelados.
- Contabilidad electrónica SAT (Anexo 24).
- Soporte para múltiples regímenes fiscales avanzados.

Esta integración cubre el 80% de los casos de uso de facturación de un consultorio pequeño y elimina la necesidad de un sistema externo desde el inicio.

### 7.7 Motor de Reglas Contables (ARE) — Fase 1

#### 7.7.1 Catálogo de Cuentas Base

La estructura raíz es no modificable; el tenant puede agregar subcuentas dentro de cada rama:

| Código | Nombre de cuenta | Tipo |
|---|---|---|
| 10000 | Activos | Raíz |
| 11100 | Caja Operativa | Detalle |
| 11200 | Bancos | Detalle |
| 11300 | Cuentas por Cobrar | Detalle |
| 12100 | Almacén de Insumos Clínicos | Detalle |
| 20000 | Pasivos | Raíz |
| 21100 | Anticipos de Pacientes | Detalle |
| 40000 | Ingresos | Raíz |
| 41000 | Ingresos por Servicios Médicos | Detalle |
| 51000 | Costo Directo del Servicio | Detalle |
| 52000 | Gastos Operativos | Raíz |
| 52100 | Merma y Caducidad | Detalle |

#### 7.7.2 Momentos Contables Obligatorios

**Momento 1 — Cobro al paciente:**
- Débito: Caja Operativa (11100) o Bancos (11200)
- Crédito: Ingresos por Servicios Médicos (41000) — si el pago es total
- Crédito: Anticipos de Pacientes (21100) — si el pago es parcial o diferido

**Momento 2 — Consumo real de insumos (post-conciliación):**
- Débito: Costo Directo del Servicio (51000)
- Crédito: Almacén de Insumos Clínicos (12100)
- Usando exactamente las cantidades validadas en la ventana de conciliación post-cita

**Momento 3 — Ajuste por merma o caducidad:**
- Débito: Merma y Caducidad (52100)
- Crédito: Almacén de Insumos Clínicos (12100)

#### 7.7.3 Eventos de Dominio — Contrato entre Módulos

| Publicado por | Nombre del evento | Acción en ARE | Acción en Inventario |
|---|---|---|---|
| Agenda | `CitaConfirmada` | Sin asiento | Reserva stock (atómico) |
| Caja | `PagoRegistrado` | Asiento Momento 1 | — |
| Clínica | `ConsumoConciliado` | Asiento Momento 2 | Baja definitiva |
| Agenda | `CitaCancelada` | Reverso si hubo pago | Libera reserva (atómico) |
| Inventario | `InsumoAgregado` | — | Entrada de stock |
| Inventario | `MermaCaducidad` | Asiento Momento 3 | Baja por ajuste |
| ARE (interno) | `JournalBalanceError` | Registro del fallo | — |

### 7.8 Personal del Consultorio

#### 7.8.1 Roles y Control de Acceso

Ver tabla completa en §4.2.

- Control de asistencia: registro de entrada y salida por empleado con timestamp.
- Perfil de empleado: datos personales, rol en la clínica, cédula profesional, especialidad.
- Alta de personal por invitación por correo — el nuevo usuario activa su cuenta desde el enlace enviado.
- El campo `salary` existe en el modelo de datos para uso en Fase 2 (nómina); no se expone en la UI de F1.

---

## 8. Fase 2 — Madurez Operativa

Esta fase profundiza las capacidades operativas: inventario de nivel profesional, nómina integrada y presupuestación. No requiere integración fiscal externa adicional.

### 8.1 Inventario Avanzado

**Valuación de inventario:** soporte para PEPS (FIFO), UEPS (LIFO) y Costo Promedio Ponderado bajo NIF mexicanas. El método se selecciona por clínica; un cambio de método requiere un proceso de revaluación auditado.

**Trazabilidad por lote:** cada entrada de inventario se asocia a un lote del proveedor. Trazabilidad completa: lote → factura de compra → paciente donde se consumió. Alerta de recall si un lote está comprometido.

**Órdenes de compra:** generación con flujo de aprobación configurable, comparativa de precios entre proveedores, recepción parcial, devoluciones y matching de 3 vías (OC → recepción → factura).

**Inventario físico periódico:** el usuario captura conteos físicos y el sistema genera diferencias automáticamente. Los ajustes generan el evento `MermaCaducidad` para el ARE.

### 8.2 Cuentas por Pagar (CxP)

- Alta de proveedores con RFC, datos fiscales y cuenta bancaria.
- Registro de facturas de proveedores: insumos, renta, servicios.
- Programación de pagos con calendarización y alertas de vencimiento.
- Estados de cuenta por proveedor.

### 8.3 Cuentas por Cobrar (CxC) — Pacientes

- Gestión de crédito: saldo deudor, límite de crédito y días de crédito por paciente.
- Estados de cuenta exportables y enviables por correo o WhatsApp.
- Recordatorios automáticos de adeudo a los 3, 7 y 30 días.

### 8.4 Nómina Integrada

- Cálculo de nómina semanal, quincenal o mensual con tabla ISR vigente del SAT.
- Cálculo de cuotas IMSS (obrero-patronal) e INFONAVIT.
- Prestaciones de ley: aguinaldo, vacaciones, prima vacacional, PTU.
- Control de incidencias: faltas, horas extra, permisos, incapacidades IMSS.
- Integración con el módulo de asistencia para cálculo automático de incidencias.
- Generación del reporte SUA para pago bimestral al IMSS.
- Dispersión de nómina: archivo SPEI en lote para el banco.

El timbrado de CFDI de nómina (complemento `nomina12`) se habilita en Fase 3 al conectar el PAC.

### 8.5 Presupuestación y Control de Costos

- Presupuesto anual por centro de costo: metas de ingreso y techo de gasto por mes y área.
- Dashboard de variación presupuestal: real vs. presupuestado en tiempo real con semáforo visual.
- Punto de equilibrio por clínica: cálculo automático de consultas o tratamientos necesarios para cubrir costos fijos.
- Centros de costo por especialidad o área (Consulta General, Ortodoncia, Cirugía, etc.).

### 8.6 Reportes Financieros Internos

- Estado de Resultados: mensual, acumulado y comparativo entre periodos.
- Balance General.
- Flujo de Efectivo (método directo e indirecto).
- Auxiliar de cuentas: detalle de movimientos por cuenta en un rango de fechas.
- Antigüedad de saldos (CxC y CxP).
- Exportación a PDF y Excel.

---

## 9. Fase 3 — Cumplimiento Fiscal

Esta fase integra el cumplimiento fiscal completo ante el SAT: CFDI 4.0, contabilidad electrónica (Anexo 24) y conciliación bancaria. Requiere integración con un PAC certificado.

### 9.1 CFDI 4.0 Completo

- Emisión de facturas, notas de crédito, notas de cargo y complementos de pago (REP).
- Integración directa con PAC certificado (Finkok, SW Sapien o Diverza) para timbrado, cancelación y consulta de estado ante el SAT.
- Ciclo de vida completo del CFDI: emitido → timbrado → cancelado → sustituido.
- Control de folios y series por clínica.
- Cancelación con y sin aceptación del receptor, conforme a reglas SAT vigentes.
- Soporte para régimen fiscal por clínica: RESICO, Persona Moral, Régimen General.
- Migración automática del historial de CFDIs emitidos con la integración básica de F1 al módulo completo.

### 9.2 CFDI de Nómina (complemento nomina12)

- Timbrado de recibos de nómina al conectar el PAC.
- Generación y envío automático del recibo por correo electrónico al empleado.

### 9.3 Impuestos

- Cálculo automático de IVA (16%, tasa 0% y exento). Los servicios médicos son exentos bajo condiciones que el sistema identifica automáticamente.
- Retención de ISR e IVA para honorarios a médicos independientes.
- Soporte para declaraciones complementarias referenciadas a la declaración original.

### 9.4 Contabilidad Electrónica (Anexo 24 — SAT)

- Exportación del Catálogo de Cuentas en formato XML SAT (CatCtg) mensual.
- Exportación de la Balanza de Comprobación en formato XML SAT (BalCtg) mensual.
- Exportación de Pólizas en formato XML SAT (PolCtg) bajo requerimiento de auditoría.
- Números de cuenta alineados al estándar SAT para exportación directa sin mapeo manual.
- Control de periodos contables: apertura, cierre provisional y cierre definitivo. Un periodo cerrado definitivamente no puede recibir pólizas.

### 9.5 Conciliación Bancaria

- Importación de estados de cuenta bancarios (HSBC, Banamex, BBVA, Banorte) en formato CAMT.053 o CSV.
- Motor de conciliación automática por monto, fecha y referencia.
- Gestión de partidas en tránsito y diferencias no conciliadas.
- Integración con Open Banking conforme a Ley Fintech mexicana cuando los bancos lo soporten.

---

## 10. Fase 4 — Inteligencia de Negocio

Esta fase transforma los datos operativos y financieros acumulados en inteligencia accionable para el médico, el administrador y el contador.

### 10.1 Dashboard KPI Clínico-Financiero

- Ingreso por médico, por tipo de tratamiento y por periodo.
- Costo promedio de consulta y margen por procedimiento.
- Tasa de ocupación de agenda.
- Tasa de conversión: pacientes en consulta que aceptan plan de tratamiento.
- Comparativo entre clínicas de la misma corporación.

### 10.2 Proyección de Flujo de Caja

- Basada en citas agendadas (ingresos esperados) más CxP programadas (egresos comprometidos).
- Vista de liquidez proyectada a 30, 60 y 90 días.
- Alertas cuando la proyección indica riesgo de liquidez negativa.

### 10.3 Alertas de Negocio Configurables

Reglas configurables por el administrador. Ejemplos:
- "Si el ingreso semanal cae más del 20% respecto a la semana anterior, notificar al admin."
- "Si el costo de insumos supera el X% del ingreso del mes, notificar al contador."

### 10.4 Exportación a BI Externo

- Exportación a Google Looker Studio y Microsoft Power BI vía API REST o conector nativo.
- Reportes personalizados con filtros por clínica, médico, periodo y centro de costo.

---

## 11. Fase 5 — Ecosistema Extendido (Visión Futura)

Esta fase expande el sistema más allá de los límites del consultorio. Su implementación depende de la madurez del producto y del ecosistema regulatorio mexicano.

### 11.1 Portal del Paciente

- Acceso del paciente a su propio expediente clínico desde app móvil o web.
- Consentimiento digital para compartir expediente con un nuevo médico.
- El médico receptor solicita acceso; el paciente autoriza mediante flujo seguro.
- El nuevo médico obtiene contexto clínico completo: diagnósticos previos, tratamientos, alergias, medicamentos.

### 11.2 Integración con Aseguradoras (Largo Plazo)

Módulo condicionado a la disponibilidad de APIs por parte de las aseguradoras mexicanas y al marco regulatorio aplicable.

- Gestión de deducibles, coaseguros y reembolsos con AXA, GNP, Metlife y otras.
- Estados de cuenta por aseguradora.
- Conciliación automática entre cobros a aseguradora y pagos recibidos.

### 11.3 Escala Hospitalaria y Corporativa

- Incorporación de la entidad Hospital por encima de la jerarquía de clínicas.
- UI corporativa: reportes consolidados con desagregación por clínica.
- Gestión centralizada de proveedores y contratos a nivel corporación.
- Políticas de control de acceso heredables en cascada: corporación → clínica → empleado.

---

## 12. Resumen de Capacidades por Fase

| Capacidad | F1 MVP | F2 Madurez | F3 Fiscal | F4 BI | F5 Ecosist. |
|---|---|---|---|---|---|
| Gestión multi-clínica | ✅ | — | — | — | — |
| Catálogo de pacientes y expediente | ✅ | — | — | — | — |
| Historia clínica NOM-004 | ✅ | — | — | — | — |
| Herramienta Canvas para documentos | ✅ | — | — | — | — |
| Agenda y gestión de citas | ✅ | — | — | — | — |
| Tratamientos y presupuestos | ✅ | — | — | — | — |
| Inventario básico con alertas | ✅ | — | — | — | — |
| Gestión de caja y pagos | ✅ | — | — | — | — |
| Motor ARE + Libro Mayor interno | ✅ | — | — | — | — |
| RBAC y control de personal | ✅ | — | — | — | — |
| CFDI básico (integración PAC puente) | ✅ | — | — | — | — |
| PWA — app instalable en móvil | ✅ | — | — | — | — |
| Cumplimiento LFPDPPP | ✅ | — | — | — | — |
| Actualizaciones en tiempo real (SSE) | ✅ | — | — | — | — |
| Inventario avanzado (lotes/OC) | — | ✅ | — | — | — |
| Nómina (sin timbrado) | — | ✅ | — | — | — |
| CxP / CxC pacientes | — | ✅ | — | — | — |
| Presupuestación y punto de equilibrio | — | ✅ | — | — | — |
| Reportes financieros estándar | — | ✅ | — | — | — |
| App nativa móvil (si datos lo justifican) | — | ✅ | — | — | — |
| CFDI 4.0 completo + PAC | — | — | ✅ | — | — |
| CFDI nómina (nomina12) | — | — | ✅ | — | — |
| Contabilidad electrónica SAT (Anexo 24) | — | — | ✅ | — | — |
| Conciliación bancaria | — | — | ✅ | — | — |
| Dashboard KPI y proyección de caja | — | — | — | ✅ | — |
| Exportación a BI externo | — | — | — | ✅ | — |
| Portal del paciente | — | — | — | — | ✅ |
| Integración aseguradoras | — | — | — | — | ✅ |
| Escala hospitalaria / corporativa | — | — | — | — | ✅ |

> La ventaja competitiva real frente a la competencia no es igualar la profundidad de los sistemas contables existentes. Es ser la única solución donde la operación clínica y la contabilidad son la misma cosa: cada acto clínico genera automáticamente su reflejo financiero, en lenguaje que cualquier médico entiende y con la trazabilidad que cualquier contador necesita.

---

## 13. Apéndice A — Normativa Mexicana de Referencia

| Norma | Descripción | Módulo afectado |
|---|---|---|
| NOM-004-SSA3-2012 | Del expediente clínico | Expediente, Historia Clínica, Canvas |
| NOM-072-SSA1-2012 | Etiquetado de medicamentos | Recetas, Inventario |
| NOM-024-SSA3-2010 | Sistemas de información en salud | Expediente electrónico |
| LFPDPPP + Reglamento | Protección de datos personales sensibles | Todos los módulos con datos de pacientes |
| Anexo 24 SAT | Contabilidad electrónica | Módulo contable (Fase 3) |
| CFDI 4.0 SAT | Comprobantes fiscales digitales | Facturación (básico en F1, completo en F3) |
| Ley Federal del Trabajo | Derechos laborales y nómina | Nómina (Fase 2) |
| Ley del IMSS / INFONAVIT | Seguridad social | Nómina (Fase 2) |
| Ley Fintech | Open Banking | Conciliación bancaria (Fase 3) |

---

## 14. Apéndice B — Registro de Decisiones de Arquitectura (ADR)

Los ADRs documentan decisiones técnicas significativas, su contexto y las alternativas descartadas. Una vez aprobados, no se modifican — las revisiones generan un nuevo ADR que referencia y reemplaza al anterior.

---

### ADR-001 — ARE implementado con RabbitMQ + Spring AMQP

**Fecha:** Junio 2025 · **Estado:** Aprobado

**Decisión:** El Motor de Reglas Contables usa RabbitMQ (vía Spring AMQP) como broker de mensajes para el procesamiento asíncrono de eventos de dominio.

**Contexto:** La arquitectura requiere que los eventos de dominio generen pólizas contables de forma confiable, con garantía de entrega y capacidad de reintentos. El stack es Java 21 + Spring Boot, donde Spring AMQP es la integración estándar con RabbitMQ. Adicionalmente, la arquitectura de monolito modular está diseñada para una eventual migración a microservicios — RabbitMQ como broker externo facilita esa separación sin cambiar el contrato de mensajes.

**Alternativas descartadas:**
- PostgreSQL LISTEN/NOTIFY: no ofrece reintentos nativos, Dead Letter Queue ni métricas de cola. Requiere implementar manualmente la lógica de reintento y reconciliación. Adecuado para proyectos sin plan de microservicios; no es el caso de este proyecto.
- Polling sobre `domain_events WHERE processed_at IS NULL`: latencia variable según el intervalo, carga constante en BD, sin backpressure natural.
- Apache Kafka: sobredimensionado para el volumen de F1; mayor complejidad operativa; la semántica de consumer groups es innecesaria cuando hay un único consumer del ARE.

**Consecuencias:** RabbitMQ es una dependencia de infraestructura adicional que requiere hosting y monitoreo. En Railway (F1), se despliega como servicio adicional en el mismo proyecto. La tabla `domain_events` sigue siendo la fuente de verdad de auditoría — RabbitMQ es solo el mecanismo de entrega.

---

### ADR-002 — Spring Security + OAuth 2.0 con JWT (RS256) y refresh token opaco

**Fecha:** Junio 2025 · **Estado:** Aprobado

**Decisión:** Se usa Spring Authorization Server como servidor de autorización self-hosted. Los `access_token` son JWT firmados con RS256. Los `refresh_token` son opacos y se almacenan en BD para permitir revocación. El `refresh_token` viaja en cookie `HttpOnly`; el `access_token` se mantiene en memoria del proceso JS del frontend.

**Contexto:** El stack es Java 21 + Spring Boot 3.x, donde Spring Authorization Server es la implementación OAuth 2.0 mantenida por el equipo de Spring. Ofrece integración nativa con Spring Security Resource Server sin configuración adicional de validación de tokens.

**Alternativas descartadas:**
- Auth0 / Keycloak externo: introduce una dependencia de terceros para la identidad. Para F1 agrega complejidad de configuración sin beneficio neto; puede considerarse en F5 si el portal del paciente requiere federación de identidades.
- JWT simétrico (HS256): requiere compartir la clave secreta entre el Authorization Server y todos los Resource Servers. Con la arquitectura de monolito modular esto no es un problema hoy, pero sería un bloqueador al extraer módulos a microservicios.
- `localStorage` para el `access_token`: vulnerable a XSS. Una cookie `HttpOnly` no es accesible por JavaScript.

**Consecuencias:** Al recargar la SPA, el `access_token` en memoria se pierde. El frontend ejecuta un request silencioso a `/oauth2/token` con `grant_type=refresh_token` (la cookie se envía automáticamente) antes de mostrar la interfaz. Este comportamiento debe documentarse en el contrato del módulo Auth.

---

### ADR-003 — SSE sobre WebSockets para actualizaciones en tiempo real

**Fecha:** Junio 2025 · **Estado:** Aprobado

**Decisión:** Se usa Server-Sent Events (SSE) en lugar de WebSockets para las actualizaciones en tiempo real de la UI.

**Contexto:** Las actualizaciones requeridas son unidireccionales: el servidor notifica al cliente de cambios de estado. El cliente responde a esas notificaciones mediante llamadas REST normales — no necesita enviar mensajes al servidor por el canal de tiempo real.

**Alternativas descartadas:**
- WebSockets: permite comunicación bidireccional, pero esa complejidad adicional no aporta valor en este dominio. Requiere gestión de reconexión personalizada, protocolo de ping/pong y consideraciones de escalado horizontal (sticky sessions o broker compartido).
- Polling cada N segundos: introduce latencia de hasta N segundos en los cambios de estado, genera carga constante en el servidor incluso cuando no hay novedades, y complica el mantenimiento del presupuesto de requests.

**Consecuencias:** SSE no es compatible con Internet Explorer 11, lo cual se acepta dado que los browsers objetivo son Chrome/Edge/Safari 120+. En entornos con proxies que cierran conexiones idle, el servidor emite un comentario de keep-alive cada 30 segundos.

---

### ADR-004 — PWA en Fase 1; app nativa diferida a Fase 2

**Fecha:** Junio 2025 · **Estado:** Aprobado

**Decisión:** La estrategia móvil de F1 es PWA. La decisión sobre app nativa se pospone a F2 con datos reales de uso.

**Contexto:** Construir una app nativa simultáneamente al MVP duplica el esfuerzo de frontend sin haber validado primero cuántos médicos necesitan funcionalidad de edición en móvil (en contraposición a solo lectura y firma digital, que la PWA cubre completamente).

**Alternativas descartadas:**
- App nativa desde F1: requiere un equipo de móvil dedicado o tiempo adicional significativo de desarrollo. Los módulos de edición compleja (Canvas, Caja, Agenda) requieren rediseño para pantallas pequeñas. Sin datos de uso reales, es probable que el esfuerzo no esté justificado.
- Solo web sin optimización móvil: los médicos usan celular para revisar su agenda y los pacientes firman documentos en tablet. No tener soporte móvil razonable sería un bloqueador de adopción.

**Consecuencias:** La funcionalidad de edición en móvil (< 768 px) es limitada en F1. Si el análisis post-lanzamiento muestra demanda significativa, se construye una app React Native en F2 que reutiliza tipos TypeScript y lógica de dominio del frontend web.

---

### ADR-005 — Encriptación AES-256-GCM a nivel de aplicación para columnas sensibles

**Fecha:** Junio 2025 · **Estado:** Aprobado

**Decisión:** Las columnas `curp`, `content_json` (documentos clínicos) y `signature_url` se encriptan con AES-256-GCM en la capa de aplicación antes de persistirse en PostgreSQL. La clave maestra se gestiona en un KMS externo.

**Contexto:** La encriptación en reposo a nivel de disco del proveedor de hosting protege contra robo físico de almacenamiento, pero no protege ante un acceso SQL directo a la base de datos (por ejemplo, en una brecha de credenciales de base de datos). La encriptación a nivel columna garantiza que incluso con acceso SQL directo, los datos sensibles no son legibles sin la clave del KMS.

**Alternativas descartadas:**
- Solo encriptación de disco del hosting: insuficiente para datos personales sensibles bajo LFPDPPP; un acceso SQL directo expone los datos en claro.
- Transparent Data Encryption (TDE) de PostgreSQL: no disponible de forma estándar en todos los proveedores de PostgreSQL gestionado; requiere extensiones o versiones Enterprise.
- Sin encriptación de columnas específicas: no cumple el espíritu del principio de minimización y las medidas de seguridad exigidas por LFPDPPP para datos sensibles.

**Consecuencias:** Las columnas encriptadas no pueden indexarse ni buscarse por contenido directamente en la base de datos. Las búsquedas de texto libre en expedientes se implementan sobre campos no encriptados (título del documento, tipo de documento, número de expediente) o sobre un índice de búsqueda separado que contiene solo fragmentos no sensibles. El impacto en rendimiento de la encriptación/desencriptación a nivel aplicación se mitiga con caché de objetos desencriptados en memoria durante la sesión de request.

---

### ADR-006 — Monolito modular sobre microservicios para Fase 1

**Fecha:** Junio 2025 · **Estado:** Aprobado

**Decisión:** El sistema se construye como un monolito modular (único JAR desplegable, múltiples módulos con fronteras explícitas) en lugar de microservicios desde el inicio.

**Contexto:** El equipo está en fase de MVP. Los requisitos funcionales aún están siendo validados con usuarios reales. Los microservicios añaden sobrecarga operativa (service discovery, tracing distribuido, consistencia eventual cross-service, múltiples deployments) que ralentizaría significativamente la velocidad de iteración en F1.

**Alternativas descartadas:**
- Microservicios desde F1: las ventajas (escalado independiente, deploys aislados, fallos contenidos) no compensan la sobrecarga en un equipo pequeño con un dominio aún en exploración. La complejidad operativa del stack distribuido (Kubernetes, service mesh, distributed tracing) requiere infraestructura y expertise que no aportan valor hasta tener carga real.
- Monolito sin estructura modular (big ball of mud): facilita el desarrollo inicial pero hace imposible la migración futura a microservicios sin una reescritura. Se descarta explícitamente.

**Mecanismo de enforcement de fronteras:** ArchUnit verifica en el pipeline de CI que ningún módulo acceda directamente a la capa de infraestructura de otro módulo. Las violaciones de frontera son errores de compilación, no convenciones de código.

**Consecuencias:** La migración a microservicios en F3/F4 será incremental y por módulo, no una reescritura. El orden probable de extracción: `accounting` (mayor carga de procesamiento), `clinical` (posible compliance independiente), `scheduling` (picos de carga en apertura de agendas). Los contratos entre módulos — RabbitMQ para eventos, facades para llamadas síncronas — son la interfaz que se convierte en API del microservicio al momento de la extracción.

---

*Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos · Versión 2.0 · Confidencial*
