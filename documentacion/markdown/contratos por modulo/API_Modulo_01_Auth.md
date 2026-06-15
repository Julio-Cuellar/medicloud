# API — Módulo 01: Autenticación y Sesión
## Sistema de Gestión de Consultorios Médicos · Fase 1 MVP

---

| Campo | Detalle |
|---|---|
| Versión del documento | 2.1 |
| Fecha | 2026-06-14 |
| Estado | Revisado · Alineado con Arquitectura v2.1 y Modelo de Datos v2.1 |
| Audiencia | Equipo Backend · Equipo Frontend |
| Módulo | `auth` · `staff` |
| Prefijo de rutas | `/v1/auth` · `/oauth2` · `/v1/staff` |
| Referencias | Arquitectura v2.1 §4.1 · Modelo de Datos v2.1 (`core.users`, `core.refresh_tokens`, `core.user_sessions`, `core.staff_invitations`, `core.doctor_profiles`, `core.password_history`) · UI/UX v2.0 · ADR-002 |
| Cambios vs v2.0 | Agrega soporte de credenciales NOM-004 en flujo de invitación de doctores; nuevo endpoint de verificación de credencial; nuevo error `DOCTOR_CREDENTIAL_REQUIRED`. |

---

## Tabla de contenido

1. [Descripción general](#1-descripción-general)
2. [Convenciones del módulo](#2-convenciones-del-módulo)
3. [Flujos cubiertos](#3-flujos-cubiertos)
4. [Política de contraseñas](#4-política-de-contraseñas)
5. [Política de rate limiting](#5-política-de-rate-limiting)
6. [Endpoints](#6-endpoints)
   - [POST /auth/register](#post-authregister)
   - [POST /auth/verify-email](#post-authverify-email)
   - [POST /auth/resend-verification](#post-authresend-verification)
   - [POST /oauth2/token — grant_type password](#post-oauth2token--grant_type-password)
   - [POST /oauth2/token — grant_type refresh_token](#post-oauth2token--grant_type-refresh_token)
   - [POST /auth/logout](#post-authlogout)
   - [POST /auth/logout-all](#post-authlogout-all)
   - [POST /auth/request-password-reset](#post-authrequest-password-reset)
   - [POST /auth/reset-password](#post-authreset-password)
   - [POST /auth/change-password](#post-authchange-password)
   - [GET /auth/me](#get-authme)
   - [PATCH /auth/me](#patch-authme)
   - [POST /auth/me/avatar](#post-authavatar)
   - [POST /auth/accept-invitation](#post-authaccept-invitation)
   - [POST /staff/invite](#post-staffinvite)
   - [GET /staff/{id}](#get-staffid)
   - [PATCH /staff/{id}/credentials/verify](#patch-staffidcredentialsverify)
7. [Catálogo de errores del módulo](#7-catálogo-de-errores-del-módulo)
8. [Notas de seguridad](#8-notas-de-seguridad)

---

## 1. Descripción general

Este módulo gestiona el ciclo de vida completo de la identidad del usuario: registro, verificación de correo, autenticación, mantenimiento de sesión y modificación de credenciales.

El sistema implementa **OAuth 2.0** con **Spring Authorization Server** (self-hosted) bajo el flujo *Resource Owner Password* para la SPA de primera parte. El esquema es de doble token (ADR-002):

| Token | Formato | Vigencia | Almacenamiento en el cliente |
|---|---|---|---|
| `access_token` | JWT firmado con **RS256** | 1 hora | Memoria del proceso JS (nunca `localStorage`) |
| `refresh_token` | **Opaco** (referencia en `core.refresh_tokens`) | 30 días | Cookie `HttpOnly; SameSite=Strict; Secure` |

El `access_token` lo valida en cada petición el Resource Server contra la clave pública del Authorization Server. El `refresh_token` opaco se persiste en base de datos para permitir revocación inmediata, rotación y cierre de todas las sesiones (`logout-all`).

El `clinic_id` de la clínica activa **no** viaja en el token: se envía en la cabecera `X-Clinic-ID` en cada solicitud operativa, se valida contra el claim `clinics` del JWT y se inyecta en el contexto de base de datos (`SET LOCAL app.clinic_id`) para aplicar **RLS (Row Level Security)**. Los endpoints de este módulo (`/oauth2/token` y los `/auth/*` públicos) **no requieren** `X-Clinic-ID`.

### Estructura del `access_token` (JWT)

La cabecera declara `"alg": "RS256"`. El *payload* contiene:

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

---

## 2. Convenciones del módulo

- Todas las solicitudes y respuestas usan `Content-Type: application/json`, salvo `POST /auth/me/avatar` que usa `multipart/form-data`.
- Los endpoints marcados como **públicos** no requieren cabecera `Authorization` ni `X-Clinic-ID`.
- Los endpoints marcados como **privados** requieren `Authorization: Bearer <access_token>`.
- Todas las respuestas de error incluyen `request_id` para correlación en logs y auditoría.

### Estructura de respuesta exitosa

```json
{
  "data": { },
  "meta": {
    "request_id": "uuid-de-la-solicitud",
    "timestamp": "2025-06-12T14:30:00Z"
  }
}
```

### Estructura de respuesta de error

```json
{
  "error": {
    "code": "CODIGO_DE_ERROR",
    "message": "Descripción legible del error.",
    "request_id": "uuid-de-la-solicitud"
  }
}
```

Para errores de validación (`422`) con múltiples campos:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "La solicitud contiene campos inválidos.",
    "details": [
      { "field": "email", "message": "Formato de correo inválido." },
      { "field": "password", "message": "La contraseña es requerida." }
    ],
    "request_id": "uuid-de-la-solicitud"
  }
}
```

---

## 3. Flujos cubiertos

| # | Flujo | Endpoints |
|---|---|---|
| 1 | Registro de nuevo usuario (dueño de clínica) | `POST /auth/register` |
| 2 | Verificación de correo electrónico | `POST /auth/verify-email` · `POST /auth/resend-verification` |
| 3 | Inicio de sesión | `POST /oauth2/token` (grant_type=password) |
| 4 | Renovación de token | `POST /oauth2/token` (grant_type=refresh_token) |
| 5 | Cierre de sesión (sesión actual) | `POST /auth/logout` |
| 6 | Cierre de todas las sesiones | `POST /auth/logout-all` |
| 7 | Restablecimiento de contraseña (sin sesión) | `POST /auth/request-password-reset` · `POST /auth/reset-password` |
| 8 | Cambio de contraseña (con sesión activa) | `POST /auth/change-password` |
| 9 | Consulta y edición de perfil | `GET /auth/me` · `PATCH /auth/me` |
| 10 | Subida de avatar | `POST /auth/me/avatar` |
| 11 | Aceptación de invitación de staff | `POST /auth/accept-invitation` |

---

## 4. Política de contraseñas

Aplica a todos los endpoints que crean o modifican contraseñas (`/register`, `/reset-password`, `/change-password`, `/accept-invitation`).

| Regla | Valor |
|---|---|
| Longitud mínima | 10 caracteres |
| Mayúsculas | Al menos 1 |
| Minúsculas | Al menos 1 |
| Dígitos | Al menos 1 |
| Reutilización | No puede ser igual a ninguna de las últimas 5 contraseñas (historial en `core.password_history`) |
| Hash | `BCryptPasswordEncoder`, *strength* 12 |
| Código de error | `PASSWORD_TOO_WEAK` / `PASSWORD_RECENTLY_USED` |

---

## 5. Política de rate limiting

| Endpoint | Límite | Ventana | Bloqueo |
|---|---|---|---|
| `POST /auth/register` | 5 solicitudes | Por hora · por IP | — |
| `POST /oauth2/token` (password) | 10 solicitudes | 15 minutos · por IP | Bloqueo de 15 min tras 5 intentos fallidos consecutivos (`core.users.locked_until`) |
| `POST /auth/resend-verification` | 3 solicitudes | Por hora · por IP | — |
| `POST /auth/request-password-reset` | 3 solicitudes | Por hora · por IP | — |
| `POST /oauth2/token` (refresh_token) | 60 solicitudes | Por hora · por `refresh_token` | — |
| Resto de endpoints privados | 300 solicitudes | Por minuto · por `access_token` | — |

Todas las respuestas incluyen las cabeceras:
- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `X-RateLimit-Reset` (timestamp Unix de cuándo se restablece el contador)

Al exceder el límite se devuelve `429 Too Many Requests` con el código `RATE_LIMIT_EXCEEDED`.

---

## 6. Endpoints

---

### `POST /auth/register`

**Acceso:** Público  
**Descripción:** Registra un nuevo usuario como dueño de una cuenta nueva en el sistema. Para evitar la enumeración de usuarios, este endpoint responde con éxito de forma silenciosa independientemente de si el correo ya está registrado.
- Si el correo **no existe**, se crea el registro en `core.users` con `email_verified = false`, se genera un token de verificación de correo (vigencia 24 h) y se envía el correo correspondiente.
- Si el correo **ya existe**, se dispara un correo notificando al usuario que ya tiene una cuenta y ofreciendo un enlace para iniciar sesión o restablecer su contraseña.

El usuario **no puede iniciar sesión** hasta verificar su correo. La clínica se crea en un paso posterior (módulo Clínicas).

**Body:**

```json
{
  "full_name": "Dr. Roberto García",
  "email": "dr.garcia@clinica.com",
  "phone": "+52 55 1234 5678",
  "password": "ContraseñaSegura1"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `full_name` | `string` | ✅ | Nombre completo del usuario. Máximo 200 caracteres. |
| `email` | `string` | ✅ | Correo electrónico. Debe ser único en el sistema. Se normaliza a minúsculas. |
| `phone` | `string` | ✗ | Teléfono de contacto. Máximo 20 caracteres. |
| `password` | `string` | ✅ | Contraseña en texto plano. Se almacena como bcrypt hash (cost factor 12). Se aplica política de contraseñas. |

**Respuesta `201`:**

```json
{
  "data": {
    "message": "Si el correo ingresado no está registrado previamente, recibirás un enlace de verificación en breve."
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T14:30:00Z"
  }
}
```

> ⚠️ **Seguridad:** la respuesta es genérica y **no devuelve** el ID del usuario ni tokens de acceso para evitar filtraciones sobre la existencia de cuentas.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `PASSWORD_TOO_WEAK` | 422 | La contraseña no cumple la política. |
| `VALIDATION_ERROR` | 422 | Campos faltantes o con formato inválido. |
| `RATE_LIMIT_EXCEEDED` | 429 | Más de 5 registros en la última hora desde esta IP. |

---

### `POST /auth/verify-email`

**Acceso:** Público  
**Descripción:** Verifica el correo electrónico del usuario usando el token de un solo uso enviado al momento del registro. Tras la verificación exitosa, el token queda invalidado y `email_verified` se establece en `true`. El usuario ya puede iniciar sesión.

**Body:**

```json
{
  "token": "token-de-verificacion-del-correo"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `token` | `string` | ✅ | Token enviado por correo. Vigencia: 24 horas. De un solo uso. |

**Respuesta `200`:**

```json
{
  "data": {
    "message": "Correo verificado correctamente. Ya puedes iniciar sesión.",
    "email": "dr.garcia@clinica.com"
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T14:31:00Z"
  }
}
```

> ⚠️ **Seguridad:** la respuesta **no devuelve** tokens de acceso. El usuario debe autenticarse de forma explícita con `POST /oauth2/token` (grant_type=password) tras verificar.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `VERIFICATION_TOKEN_INVALID` | 401 | El token no existe o ya fue utilizado. |
| `VERIFICATION_TOKEN_EXPIRED` | 401 | El token venció (más de 24 horas). Usar `resend-verification`. |

---

### `POST /auth/resend-verification`

**Acceso:** Público  
**Descripción:** Reenvía el correo de verificación para usuarios con `email_verified = false`. Genera un nuevo token (invalidando el anterior) y envía el correo. Siempre responde con éxito para no revelar si el email existe o su estado.

**Body:**

```json
{
  "email": "dr.garcia@clinica.com"
}
```

**Respuesta `200`:**

```json
{
  "data": {
    "message": "Si el correo está registrado y pendiente de verificación, recibirás un nuevo enlace en breve."
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T14:32:00Z"
  }
}
```

> ⚠️ **Seguridad:** respuesta idéntica independientemente de si el email existe, no existe o ya está verificado. No filtrar información de estado de cuentas.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `RATE_LIMIT_EXCEEDED` | 429 | Más de 3 solicitudes en la última hora desde esta IP. |

---

### `POST /oauth2/token` — grant_type `password`

**Acceso:** Público  
**Descripción:** Autentica al usuario con email y contraseña y emite los tokens. Solo usuarios con `email_verified = true` e `is_active = true` pueden obtener tokens. Tras un login exitoso actualiza `last_login_at` en `core.users` y reinicia `failed_login_attempts`. El `refresh_token` se devuelve **como cookie** `HttpOnly`, no en el cuerpo de la respuesta.

**Content-Type:** `application/x-www-form-urlencoded`

**Body (form):**

```
grant_type=password
username=dr.garcia@clinica.com
password=ContraseñaSegura1
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
      "phone": "+52 55 1234 5678",
      "avatar_url": "https://storage.medicloud.mx/avatars/uuid.jpg",
      "email_verified": true,
      "theme_preference": "light",
      "clinics": [
        {
          "id": "uuid-clinica-1",
          "name": "Clínica Reforma",
          "role": "admin",
          "role_label": "Administrador"
        },
        {
          "id": "uuid-clinica-2",
          "name": "Sucursal Norte",
          "role": "doctor",
          "role_label": "Médico"
        }
      ]
    }
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T08:00:00Z"
  }
}
```

**Cabecera de respuesta:**

```
Set-Cookie: refresh_token=<opaco>; HttpOnly; Secure; SameSite=Strict; Path=/oauth2; Max-Age=2592000
```

> **Nota de negocio:** si `clinics` devuelve `[]`, el usuario no tiene ninguna clínica asignada aún. El frontend debe redirigirlo al flujo de creación de primera clínica (módulo Clínicas).

> ⚠️ **Seguridad:** los errores `INVALID_CREDENTIALS`, `EMAIL_NOT_VERIFIED`, `USER_INACTIVE` y `ACCOUNT_LOCKED` devuelven **el mismo mensaje genérico** al usuario en la interfaz: *"Credenciales inválidas o cuenta no habilitada"*. Los códigos de error específicos son para uso interno del frontend (logging, analítica), nunca deben mostrarse literalmente al usuario. Tras 5 intentos fallidos consecutivos la cuenta se bloquea 15 minutos (`core.users.locked_until`).

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Email no existe o contraseña incorrecta. |
| `EMAIL_NOT_VERIFIED` | 403 | El usuario existe pero no ha verificado su correo. |
| `USER_INACTIVE` | 403 | La cuenta fue desactivada por un administrador. |
| `ACCOUNT_LOCKED` | 403 | Cuenta bloqueada temporalmente tras 5 intentos fallidos (15 min). |
| `VALIDATION_ERROR` | 422 | Campos faltantes o con formato inválido. |
| `RATE_LIMIT_EXCEEDED` | 429 | Más de 10 intentos en los últimos 15 minutos desde esta IP. |

---

### `POST /oauth2/token` — grant_type `refresh_token`

**Acceso:** Público (la cookie `refresh_token` se envía automáticamente)  
**Descripción:** Obtiene un nuevo `access_token`. El `refresh_token` viaja en la cookie `HttpOnly` —**no** en el cuerpo— y queda **invalidado inmediatamente**, emitiéndose uno nuevo en la cookie (rotación). Este mecanismo previene el uso de tokens robados.

**Content-Type:** `application/x-www-form-urlencoded`

**Body (form):**

```
grant_type=refresh_token
```

**Respuesta `200`:**

```json
{
  "data": {
    "access_token": "eyJhbGciOiJSUzI1NiIs...",
    "token_type": "Bearer",
    "expires_in": 3600
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T09:00:00Z"
  }
}
```

Se emite una nueva cookie `Set-Cookie: refresh_token=<nuevo-opaco>; HttpOnly; ...`.

> **Re-hidratación al recargar la SPA:** como el `access_token` vive solo en memoria, al recargar la página el frontend hace una petición silenciosa a este endpoint (la cookie se envía sola) antes de renderizar la interfaz (ADR-002).

> ⚠️ **Seguridad — Detección de reutilización maliciosa:** si se intenta usar un `refresh_token` que ya fue rotado, el sistema interpreta esto como posible robo de token y ejecuta de forma automática:
> 1. Invalida **toda la familia de tokens** del usuario (logout forzado en todos los dispositivos).
> 2. Registra el evento en `audit.audit_log` con IP, user-agent y timestamp.
> 3. Envía correo de alerta de seguridad al usuario.
> 4. Devuelve `401 REFRESH_TOKEN_REUSED`.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `REFRESH_TOKEN_INVALID` | 401 | El token no existe o ya fue rotado. |
| `REFRESH_TOKEN_EXPIRED` | 401 | El token tiene más de 30 días. |
| `REFRESH_TOKEN_REUSED` | 401 | Reuso de un token ya rotado; se revocó toda la familia. |
| `USER_INACTIVE` | 403 | La cuenta fue desactivada. |

---

### `POST /auth/logout`

**Acceso:** Privado  
**Descripción:** Revoca el `refresh_token` de la sesión actual (lo borra de `core.refresh_tokens`) y limpia la cookie. El `access_token` sigue siendo técnicamente válido hasta su expiración natural (máximo 1 hora), pero no puede renovarse.

**Cabeceras:** `Authorization: Bearer <access_token>` + cookie `refresh_token`.

**Body:** vacío.

**Respuesta:** `204 No Content` con `Set-Cookie: refresh_token=; Max-Age=0`.

> ⚠️ **Seguridad:** el `refresh_token` se toma de la cookie `HttpOnly`, no del body. El backend lo revoca en base de datos para que no pueda reutilizarse.

---

### `POST /auth/logout-all`

**Acceso:** Privado  
**Descripción:** Invalida **todos** los `refresh_token` activos del usuario en todos sus dispositivos. Útil ante sospecha de compromiso de cuenta. El `access_token` actual sigue válido hasta su expiración.

**Cabeceras:** `Authorization: Bearer <access_token>`

**Body:** vacío.

**Respuesta:** `204 No Content`

---

### `POST /auth/request-password-reset`

**Acceso:** Público  
**Descripción:** Envía un correo con enlace para restablecer la contraseña. Siempre responde con éxito para no revelar si el email existe. Si el email existe y la cuenta está activa, genera un token de restablecimiento (vigencia 1 hora, un solo uso) y envía el correo.

**Body:**

```json
{
  "email": "dr.garcia@clinica.com"
}
```

**Respuesta `200`:**

```json
{
  "data": {
    "message": "Si el correo está registrado, recibirás las instrucciones para restablecer tu contraseña."
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T10:00:00Z"
  }
}
```

> ⚠️ **Seguridad:** respuesta idéntica en todos los casos (email existe, no existe, inactivo, no verificado). No filtrar información de estado de cuentas.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `RATE_LIMIT_EXCEEDED` | 429 | Más de 3 solicitudes en la última hora desde esta IP. |

---

### `POST /auth/reset-password`

**Acceso:** Público  
**Descripción:** Establece una nueva contraseña usando el token del correo. Tras el cambio exitoso: el token queda invalidado y **todos los `refresh_token` activos del usuario son revocados** (logout forzado global). El usuario debe iniciar sesión con su nueva contraseña.

**Body:**

```json
{
  "token": "token-del-correo-de-restablecimiento",
  "new_password": "NuevaContraseñaSegura1"
}
```

**Respuesta `200`:**

```json
{
  "data": {
    "message": "Contraseña actualizada correctamente. Inicia sesión con tu nueva contraseña."
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T10:05:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `RESET_TOKEN_INVALID` | 401 | El token no existe o ya fue utilizado. |
| `RESET_TOKEN_EXPIRED` | 401 | El token tiene más de 1 hora. |
| `PASSWORD_TOO_WEAK` | 422 | La contraseña no cumple la política. |
| `PASSWORD_RECENTLY_USED` | 422 | La contraseña es igual a una de las últimas 3 usadas. |

---

### `POST /auth/change-password`

**Acceso:** Privado  
**Descripción:** Cambia la contraseña del usuario autenticado. Requiere la contraseña actual como confirmación de identidad. Tras el cambio exitoso: **todos los `refresh_token` activos quedan revocados excepto el de la sesión actual**, forzando cierre de sesión en otros dispositivos.

**Cabeceras:** `Authorization: Bearer <access_token>`

**Body:**

```json
{
  "current_password": "ContraseñaAnterior1",
  "new_password": "NuevaContraseñaSegura1"
}
```

**Respuesta `200`:**

```json
{
  "data": {
    "message": "Contraseña actualizada. Las otras sesiones activas han sido cerradas."
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T10:10:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `INVALID_CREDENTIALS` | 401 | La contraseña actual es incorrecta. |
| `PASSWORD_TOO_WEAK` | 422 | La nueva contraseña no cumple la política. |
| `PASSWORD_RECENTLY_USED` | 422 | La nueva contraseña es igual a una de las últimas 3 usadas. |

---

### `GET /auth/me`

**Acceso:** Privado  
**Descripción:** Devuelve el perfil completo del usuario autenticado con todas sus clínicas y roles asignados.

**Cabeceras:** `Authorization: Bearer <access_token>`

**Respuesta `200`:**

```json
{
  "data": {
    "id": "uuid",
    "full_name": "Dr. Roberto García",
    "email": "dr.garcia@clinica.com",
    "phone": "+52 55 1234 5678",
    "avatar_url": "https://storage.medicloud.mx/avatars/uuid.jpg",
    "email_verified": true,
    "theme_preference": "light",
    "is_active": true,
    "last_login_at": "2025-06-12T08:00:00Z",
    "created_at": "2025-01-15T10:00:00Z",
    "clinics": [
      {
        "id": "uuid-clinica-1",
        "name": "Clínica Reforma",
        "role": "admin",
        "role_label": "Administrador",
        "is_active": true
      }
    ]
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T08:05:00Z"
  }
}
```

> ⚠️ **Seguridad:** este endpoint **nunca devuelve** `password_hash` ni ningún token activo. Solo expone los campos definidos en el esquema anterior.

---

### `PATCH /auth/me`

**Acceso:** Privado  
**Descripción:** Actualiza los datos editables del perfil. El `email` no es modificable por este endpoint (requiere flujo de cambio de email — fuera del alcance de Fase 1). La contraseña tampoco — usar `POST /auth/change-password`.

**Cabeceras:** `Authorization: Bearer <access_token>`

**Body (todos los campos son opcionales):**

```json
{
  "full_name": "Dr. Roberto García López",
  "phone": "+52 55 9876 5432",
  "theme_preference": "dark"
}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `full_name` | `string` | Nombre completo. Máximo 200 caracteres. |
| `phone` | `string` | Teléfono de contacto. Máximo 20 caracteres. |
| `theme_preference` | `light` \| `dark` \| `system` | Preferencia de tema de la interfaz. |

**Respuesta `200`:** objeto `data` con el perfil actualizado completo (mismo esquema que `GET /auth/me`).

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Campos con formato o longitud inválidos. |

---

### `POST /auth/me/avatar`

**Acceso:** Privado  
**Descripción:** Sube o reemplaza el avatar del usuario. El archivo anterior se elimina del object storage. El sistema genera automáticamente dos variantes: `256×256 px` y `64×64 px`.

**Cabeceras:** `Authorization: Bearer <access_token>`

**Content-Type:** `multipart/form-data`

**Form fields:**

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `file` | `file` | ✅ | Imagen del avatar. Tamaño máximo: **2 MB**. |

**Formatos aceptados:** JPG, PNG, WebP.

> ⚠️ **Seguridad:** la validación del tipo de archivo se realiza sobre el **tipo MIME real** del contenido binario (magic bytes), **no** únicamente sobre la extensión del nombre de archivo. Archivos con extensión `.jpg` pero contenido no-imagen deben rechazarse.

**Respuesta `200`:**

```json
{
  "data": {
    "avatar_url": "https://storage.medicloud.mx/avatars/uuid_256.jpg",
    "avatar_url_small": "https://storage.medicloud.mx/avatars/uuid_64.jpg"
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
| `FILE_TOO_LARGE` | 422 | El archivo supera los 2 MB. |
| `INVALID_FILE_TYPE` | 422 | El tipo MIME real no es `image/jpeg`, `image/png` ni `image/webp`. |

---

### `POST /auth/accept-invitation`

**Acceso:** Público  
**Descripción:** Acepta una invitación para unirse a una clínica como miembro del staff. Este endpoint es el destino del enlace enviado por correo cuando un administrador registra a un nuevo empleado desde el módulo de Personal.

Existen dos escenarios según si el email invitado ya tiene cuenta:

| Escenario | Condición | Acción |
|---|---|---|
| **A — Usuario nuevo** | El email invitado no existe en `core.users` | Se crea el usuario y se establece la contraseña en este mismo paso |
| **B — Usuario existente** | El email invitado ya tiene cuenta activa | Solo se vincula la clínica; no se crea usuario ni se solicita contraseña |

> ⚠️ **Seguridad:** el sistema valida que el `invitation_token` corresponda exactamente a la clínica que lo emitió. No es posible usar el token de una invitación de la Clínica A para acceder a la Clínica B. Adicionalmente, el backend valida estrictamente que el email asociado a la invitación coincida con el email del usuario que está aceptándola (en el Escenario B) para evitar ataques de secuestro de invitación.

**Body:**

```json
{
  "invitation_token": "token-de-la-invitacion-del-correo",
  "password": "ContraseñaSegura1"
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `invitation_token` | `string` | ✅ | Token del correo de invitación. Vigencia: **72 horas**. De un solo uso. |
| `password` | `string` | Solo en escenario A | Contraseña nueva. Solo requerida si el email no tenía cuenta previa. Aplica política de contraseñas. |

**Respuesta `200`:** el usuario queda autenticado. El `refresh_token` se emite como cookie `HttpOnly` (igual que en `POST /oauth2/token`); no se incluye en el cuerpo.

```json
{
  "data": {
    "access_token": "eyJhbGciOiJSUzI1NiIs...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "user": {
      "id": "uuid",
      "full_name": "Lic. Laura Gómez",
      "email": "laura.gomez@clinica.com",
      "clinics": [
        {
          "id": "uuid-clinica-1",
          "name": "Clínica Reforma",
          "role": "receptionist",
          "role_label": "Recepcionista"
        }
      ]
    },
    "joined_clinic": {
      "id": "uuid-clinica-1",
      "name": "Clínica Reforma"
    }
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2025-06-12T08:15:00Z"
  }
}
```

**Cabecera de respuesta:** `Set-Cookie: refresh_token=<opaco>; HttpOnly; Secure; SameSite=Strict; Path=/oauth2; Max-Age=2592000`

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `INVITATION_TOKEN_INVALID` | 401 | El token no existe, ya fue usado o no corresponde a ninguna clínica válida. |
| `INVITATION_TOKEN_EXPIRED` | 401 | El token tiene más de 72 horas. |
| `PASSWORD_REQUIRED` | 422 | Se omitió `password` en escenario A (usuario nuevo). |
| `PASSWORD_TOO_WEAK` | 422 | La contraseña no cumple la política. Solo aplica en escenario A. |

---

### `POST /staff/invite`

**Acceso:** Privado  
**Descripción:** Envía una invitación por correo electrónico para registrar un nuevo empleado en la clínica activa. Si el rol asignado es `doctor`, se requiere registrar las credenciales profesionales asociadas según lo exigido por la NOM-004-SSA3-2012.

**Body:**

```json
{
  "email": "nuevo.medico@clinica.com",
  "full_name": "Dra. Ana Martínez",
  "role": "doctor",
  "doctor_credentials": {
    "universidad_egreso": "Universidad Nacional Autónoma de México",
    "anio_egreso": 2015,
    "especialidad": "Odontología",
    "sub_especialidad": null,
    "cedula_profesional": "12345678",
    "cedula_especialidad": null,
    "institucion_especialidad": null,
    "documento_tramite_url": null
  }
}
```

| Campo | Tipo | Requerido | Descripción |
|---|---|---|---|
| `email` | `string` | ✅ | Correo electrónico del invitado. |
| `full_name` | `string` | ✅ | Nombre completo del miembro del staff. |
| `role` | `string` | ✅ | Rol del personal (ej. `doctor`, `receptionist`, etc.). |
| `doctor_credentials` | `object` | Opcional | Datos de credenciales profesionales. Requerido obligatoriamente cuando `role` sea `doctor`. |
| `doctor_credentials.universidad_egreso` | `string` | Requerido si es doctor | Institución donde obtuvo el título profesional. |
| `doctor_credentials.anio_egreso` | `integer` | Requerido si es doctor | Año de egreso o titulación. |
| `doctor_credentials.especialidad` | `string` | ✗ | Especialidad médica si aplica; de lo contrario `null`. |
| `doctor_credentials.sub_especialidad` | `string` | ✗ | Subespecialidad médica si aplica; de lo contrario `null`. |
| `doctor_credentials.cedula_profesional` | `string` | ✗ | Cédula profesional emitida por la SEP. Puede ser `null` solo si `documento_tramite_url` está presente. |
| `doctor_credentials.cedula_especialidad` | `string` | ✗ | Cédula de especialidad médica si aplica; de lo contrario `null`. |
| `doctor_credentials.institucion_especialidad` | `string` | ✗ | Institución que otorgó la especialidad si aplica; de lo contrario `null`. |
| `doctor_credentials.documento_tramite_url` | `string` | ✗ | URL del documento que acredita cédula en trámite. Requerido si `cedula_profesional` es `null`. |

**Reglas de validación y negocio:**
- Si el `role` es `doctor` y el objeto `doctor_credentials` está ausente, se retorna el error `DOCTOR_CREDENTIAL_REQUIRED`.
- Si `cedula_profesional` es `null`, la URL `documento_tramite_url` debe estar presente; en caso contrario se retorna el error `DOCTOR_CREDENTIAL_REQUIRED`.
- Si la `cedula_profesional` está presente, el estado de la credencial (`credential_status`) se establece automáticamente como `activo`.
- Si solo la URL `documento_tramite_url` está presente (con `cedula_profesional` nulo), el estado se establece como `en_tramite`.
- Los campos `universidad_egreso` y `anio_egreso` son siempre requeridos dentro de `doctor_credentials` cuando el `role` es `doctor`.

**Respuesta `201`:**

```json
{
  "data": {
    "invitation_sent": true,
    "email": "nuevo.medico@clinica.com",
    "role": "doctor",
    "doctor_credentials": {
      "universidad_egreso": "Universidad Nacional Autónoma de México",
      "anio_egreso": 2015,
      "especialidad": "Odontología",
      "sub_especialidad": null,
      "cedula_profesional": "12345678",
      "cedula_especialidad": null,
      "institucion_especialidad": null,
      "documento_tramite_url": null,
      "credential_status": "activo"
    }
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2026-06-14T10:00:00Z"
  }
}
```

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `FORBIDDEN` | 403 | El usuario no tiene rol administrativo para invitar personal. |
| `DOCTOR_CREDENTIAL_REQUIRED` | 422 | Se intentó registrar un doctor sin cédula profesional ni documento de trámite, o faltan `universidad_egreso` / `anio_egreso`. |

---

### `GET /staff/{id}`

**Acceso:** Privado  
**Descripción:** Obtiene los datos detallados de un miembro del staff de la clínica activa por su identificador.

**Respuesta `200`:**

```json
{
  "data": {
    "id": "uuid",
    "full_name": "Dra. Ana Martínez",
    "email": "nuevo.medico@clinica.com",
    "role": "doctor",
    "role_label": "Médico",
    "is_active": true,
    "doctor_credentials": {
      "cedula_profesional": "12345678",
      "cedula_especialidad": null,
      "especialidad": "Odontología",
      "sub_especialidad": null,
      "universidad_egreso": "UNAM",
      "anio_egreso": 2015,
      "institucion_especialidad": null,
      "credential_status": "activo",
      "verified_at": "2026-06-14T10:00:00Z",
      "verified_by_user_id": "uuid-del-admin"
    }
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2026-06-14T10:00:00Z"
  }
}
```

**Reglas de respuesta:**
- Si el miembro del staff consultado tiene el rol de `doctor`, se incluye el objeto `doctor_credentials` con la información detallada de sus credenciales profesionales.
- Para roles distintos de `doctor`, el campo `doctor_credentials` se omite de la respuesta o se retorna con valor `null`.

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `STAFF_NOT_FOUND` | 404 | El identificador de staff provisto no existe en la clínica activa. |

---

### `PATCH /staff/{id}/credentials/verify`

**Acceso:** Privado · Solo `clinic_admin`  
**Descripción:** Permite a un administrador de la clínica verificar y marcar como válidas las credenciales profesionales presentadas por un médico.

**Body:** vacío `{}`

**Respuesta `200`:**

```json
{
  "data": {
    "staff_id": "uuid",
    "credential_status": "activo",
    "verified_at": "2026-06-14T10:00:00Z"
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2026-06-14T10:00:00Z"
  }
}
```

**Comportamiento de verificación:**
- Cambia el estado de la credencial (`credential_status`) a `activo` si la cédula profesional (`cedula_profesional`) está presente.
- Mantiene el estado en `en_tramite` si solo existe el documento comprobatorio (`documento_tramite_url`).

**Errores:**

| Código | HTTP | Condición |
|---|---|---|
| `STAFF_NOT_FOUND` | 404 | El miembro del staff no existe en la clínica activa. |
| `STAFF_NOT_A_DOCTOR` | 422 | El miembro del staff existe pero no posee el rol de médico. |

---

## 7. Catálogo de errores del módulo

| Código | HTTP | Descripción |
|---|---|---|
| `VALIDATION_ERROR` | 422 | Uno o más campos no pasaron validación de formato. |
| `EMAIL_ALREADY_EXISTS` | 409 | El email ya está registrado en el sistema. |
| `INVALID_CREDENTIALS` | 401 | Email o contraseña incorrectos. |
| `EMAIL_NOT_VERIFIED` | 403 | El usuario no ha verificado su correo electrónico. |
| `USER_INACTIVE` | 403 | La cuenta fue desactivada por un administrador. |
| `VERIFICATION_TOKEN_INVALID` | 401 | Token de verificación de correo inválido o ya utilizado. |
| `VERIFICATION_TOKEN_EXPIRED` | 401 | Token de verificación de correo vencido (más de 24 h). |
| `REFRESH_TOKEN_INVALID` | 401 | El refresh token no existe o ya fue rotado. |
| `REFRESH_TOKEN_EXPIRED` | 401 | El refresh token tiene más de 30 días. |
| `RESET_TOKEN_INVALID` | 401 | Token de restablecimiento de contraseña inválido o ya utilizado. |
| `RESET_TOKEN_EXPIRED` | 401 | Token de restablecimiento de contraseña vencido (más de 1 h). |
| `INVITATION_TOKEN_INVALID` | 401 | Token de invitación inválido, ya utilizado o sin clínica válida. |
| `INVITATION_TOKEN_EXPIRED` | 401 | Token de invitación vencido (más de 72 h). |
| `PASSWORD_TOO_WEAK` | 422 | La contraseña no cumple la política del sistema. |
| `PASSWORD_RECENTLY_USED` | 422 | La contraseña coincide con una de las últimas 3 usadas. |
| `PASSWORD_REQUIRED` | 422 | El campo `password` es obligatorio en este contexto. |
| `DOCTOR_CREDENTIAL_REQUIRED` | 422 | Se intentó registrar un doctor sin cédula profesional ni documento de trámite, o faltan `universidad_egreso` / `anio_egreso`. |
| `STAFF_NOT_FOUND` | 404 | El miembro del staff no fue encontrado en la clínica activa. |
| `STAFF_NOT_A_DOCTOR` | 422 | El miembro del staff no tiene el rol de médico (doctor). |
| `FILE_TOO_LARGE` | 422 | El archivo supera el tamaño máximo permitido (2 MB). |
| `INVALID_FILE_TYPE` | 422 | El tipo MIME real del archivo no es aceptado. |
| `RATE_LIMIT_EXCEEDED` | 429 | Se superó el límite de solicitudes permitidas. |

---

## 8. Notas de seguridad

Resumen consolidado de las decisiones de seguridad aplicadas en este módulo. Debe revisarse con el equipo de backend antes de iniciar desarrollo.

| # | Decisión | Justificación |
|---|---|---|
| 1 | Registro no emite tokens — requiere verificación de correo primero | Previene creación masiva de cuentas funcionales con emails falsos |
| 2 | `register`, `verify-email`, `resend-verification` y `request-password-reset` siempre responden con éxito silencioso | Previene enumeración de cuentas (user enumeration attack) |
| 3 | Los códigos de error `INVALID_CREDENTIALS`, `EMAIL_NOT_VERIFIED` y `USER_INACTIVE` nunca se muestran literalmente al usuario en la UI | Previene que un atacante distinga si una cuenta existe o está verificada |
| 4 | Rotación de `refresh_token` en cada `POST /oauth2/token` (grant refresh) | Limita la ventana de uso de tokens robados |
| 5 | Reutilización de `refresh_token` ya rotado → invalidación total de la familia + alerta al usuario | Detecta y contiene ataques de token replay |
| 6 | `reset-password` y `change-password` revocan todos los `refresh_token` activos | Expulsa sesiones no autorizadas tras cambio de credenciales |
| 7 | Validación de tipo MIME real en uploads de avatar (magic bytes, no extensión) | Previene subida de archivos maliciosos camuflados con extensión de imagen |
| 8 | `invitation_token` valida correspondencia de email y clínica emisora | Previene uso de tokens ajenos o de otras clínicas para registrarse |
| 9 | Rate limiting diferenciado por endpoint con bloqueo temporal en `/login` | Mitiga fuerza bruta de contraseñas |
| 10 | bcrypt con cost factor 12 para almacenamiento de contraseñas | Balance entre seguridad y rendimiento en servidores de producción |
| 11 | `access_token` JWT RS256 (asimétrico), `refresh_token` opaco en cookie `HttpOnly` | RS256 permite validar el token sin compartir secreto entre módulos (preparado para microservicios); la cookie `HttpOnly` no es accesible por JavaScript, mitigando XSS |

---

*API — Módulo 01: Autenticación y Sesión · Sistema de Gestión de Consultorios Médicos · Fase 1 MVP · v2.1 · Confidencial · Alineado con Arquitectura v2.1 y Modelo de Datos v2.1*
