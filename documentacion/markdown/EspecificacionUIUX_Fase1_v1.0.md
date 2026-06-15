Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
E S P E C I F I C A C I Ó N U I / U X
Sistema de Gestión de Consultorios Médicos
Fase 1 · MVP Entregable
Versión 1.0
Junio 2025 · Confidencial
Campo Detalle
Versión 1.0
Fecha Junio 2025
Estado Borrador para revisión
Audiencia Diseñadores UI/UX y Desarrolladores Frontend
Referencias Arquitectura v1.0 · Modelo de Datos v1.0
Plataforma objetivo Aplicación web SaaS — escritorio primario, tablet secundario
Compatibilidad Chrome 120+, Edge 120+, Safari 17+, Firefox 120+
Versión 1.0 · Junio 20251

Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
1. Introducción
1.1 Propósito del documento
Este documento define la especificación completa de interfaz de usuario y experiencia de usuario
(UI/UX) para la Fase 1 MVP del Sistema de Gestión de Consultorios Médicos. Su objetivo es servir
como fuente única de verdad para el equipo de diseño y el equipo de desarrollo frontend, garantizando
que ambos trabajen con los mismos criterios de interacción, jerarquía visual y comportamiento de
componentes.
El documento abarca desde los fundamentos del sistema de diseño (colores, tipografía, componentes
base) hasta la especificación detallada de cada módulo funcional, incluyendo wireframes de referencia,
flujos de navegación y reglas de comportamiento por rol de usuario.
1.2 Audiencia
Perfil Uso del documento
Diseñador UI/UX Referencia para crear prototipos de alta fidelidad y validar con
usuarios
Desarrollador frontend Especificación de componentes, estados, validaciones y flujos a
implementar
Product Owner Verificación de cobertura funcional contra la especificación de
arquitectura
QA Criterios de aceptación para flujos de usuario y comportamientos de
interfaz
1.3 Alcance — Fase 1 MVP
Este documento cubre exclusivamente los módulos incluidos en la Fase 1 MVP. Los módulos de fases
posteriores (CFDI, nómina, BI, portal del paciente) están fuera del alcance de esta especificación.
Módulo Incluido en Fase 1
Agenda y gestión de citas Sí
Catálogo de pacientes Sí
Expediente clínico y Canvas Sí
Tratamientos y presupuestos Sí
Caja y cobros Sí
Inventario básico Sí
Contabilidad interna (ARE + Libro Mayor) Sí
Personal y asistencia Sí
Facturación electrónica CFDI No — Fase 3
Nómina con timbrado No — Fase 2/3
Portal del paciente No — Fase 5
Versión 1.0 · Junio 20252

Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
1.4 Convenciones del documento
• Los wireframes incluidos son de referencia estructural (baja-media fidelidad). El equipo de diseño
los usa como base para prototipos de alta fidelidad.
• Las especificaciones de componente incluyen: nombre, variantes, estados, comportamiento y
correspondencia con la tabla del modelo de datos.
• Las notas en azul indican decisiones de diseño con justificación técnica o de negocio.
• Las restricciones en amarillo indican limitaciones derivadas de la normativa NOM o reglas del
negocio que no pueden modificarse.
1.5 Documentos de referencia
Documento Versión Descripción
Especificación de Arquitectura v1.0 Jun 2025 Principios arquitectónicos, fases y
módulos funcionales
Modelo de Datos Fase 1 MVP v1.0 Jun 2025 Esquema completo de base de datos
PostgreSQL 16
Versión 1.0 · Junio 20253

Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
2. Sistema de Diseño
2.1 Principios de diseño
El sistema de diseño está construido sobre cuatro principios que reflejan la naturaleza del producto: un
sistema de uso intensivo diario con dos audiencias distintas (personal clínico y personal administrativo-
contable).
Claridad operativa
Cada pantalla comunica el estado del sistema sin ambigüedad. El médico o la recepcionista deben
poder leer el estado de una cita, un cobro o un insumo en menos de dos segundos, sin necesidad de
abrir detalles. La jerarquía visual prioriza la información de acción inmediata.
Doble audiencia, un solo sistema
El mismo módulo financiero se presenta en lenguaje natural para el médico ("Se registró el cobro de
$2,800") y en vista técnica para el contador (Póliza #0041 — Déb. 11100 / Créd. 41000). El sistema de
diseño define los componentes que soportan ambas representaciones sin duplicar interfaces.
Seriedad sin frialdad
La paleta azul-aqua con acento verde transmite confianza y profesionalismo médico. Los bordes
definidos (border-radius mínimo en elementos interactivos) refuerzan la seriedad. La tipografía Inter en
pesos 400 y 500 garantiza legibilidad en tablas densas sin fatiga visual.
Modos de uso prolongado
El sistema ofrece modo claro y modo oscuro. El modo claro es el predeterminado para uso diurno en
consultorio. El modo oscuro reduce la fatiga visual en consultorios con poca iluminación o turnos
nocturnos. La preferencia del usuario persiste en su perfil.
2.2 Paleta de colores
2.2.1 Modo claro
Muestra Token Hex Uso Modo
--color-sidebar #274E4E Fondo de la barra lateral de Claro
navegación
--color-primary #376D6D Botones primarios, links, ítem activo, Claro
acentos
--color-accent #B8DBDB Borde activo en nav, punto del Claro
logotipo, confirmación
--color-surface #EEF3F3 Fondo de página y áreas de Claro
contenido
--color-card #FFFFFF Fondo de tarjetas, tablas y paneles Claro
--color-border #D0DEDE Bordes de inputs, tablas y Claro
separadores
--color-text-1 #1A2A2A Texto principal — títulos y datos Claro
--color-text-2 #4A6A6A Texto secundario — etiquetas y Claro
subtítulos
Versión 1.0 · Junio 20254

Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
| Muestra  | Token           | Hex      | Uso                       | Modo   |
| -------- | --------------- | -------- | ------------------------- | ------ |
|          | --color-text-3  | #829a9a  |                           | Claro  |
|          |                 |          | Texto muted — metadatos,  |        |
timestamps, placeholders
|     | --color-success  | #1A5C38  |                                 | Ambos  |
| --- | ---------------- | -------- | ------------------------------- | ------ |
|     |                  |          | Estados positivos: confirmado,  |        |
pagado, activo
|     |                  | #DAF2E8  | Fondo de badges y alertas de éxito  | Claro  |
| --- | ---------------- | -------- | ----------------------------------- | ------ |
|     | --color-success- |          |                                     |        |
bg
  --color-warning  #92530A  Estados de advertencia: pendiente,  Ambos
por vencer
  --color-warning- #FEF3E2  Fondo de badges y alertas de  Claro
|     | bg  |     | advertencia  |     |
| --- | --- | --- | ------------ | --- |
  --color-error  #932020  Estados de error: cancelado,  Ambos
vencido, fallo
  --color-error-bg  #FDEAEA  Fondo de badges y alertas de error  Claro

2.2.2 Modo oscuro
| Muestra  | Token  | Hex  | Uso  | Modo  |
| -------- | ------ | ---- | ---- | ----- |
  --color-sidebar  #152727  Fondo de la barra lateral de  Oscuro
navegación
  --color-primary  #5AA2A2  Botones primarios, links, ítem activo  Oscuro
--color-accent  #B8DBDB  Borde activo en nav, punto del  Oscuro

logotipo (igual)
|     | --color-surface  | #0B1212  | Fondo de página  | Oscuro  |
| --- | ---------------- | -------- | ---------------- | ------- |

|     | --color-card  | #162525  | Fondo de tarjetas y paneles  | Oscuro  |
| --- | ------------- | -------- | ---------------------------- | ------- |

  --color-border  #243A3A  Bordes de inputs, tablas y  Oscuro
separadores
|     | --color-text-1  | #E6F0F0  | Texto principal   | Oscuro  |
| --- | --------------- | -------- | ----------------- | ------- |
|     | --color-text-2  | #A2B8B8  | Texto secundario  | Oscuro  |
|     | --color-text-3  | #4A6A6A  | Texto muted       | Oscuro  |
  --color-success- #123821  Fondo de badges de éxito en modo  Oscuro
|     | bg  |     | oscuro  |     |
| --- | --- | --- | ------- | --- |
--color-warning- #3B2609  Fondo de badges de advertencia en  Oscuro

modo oscuro
bg
|     |                   | #3D1414  |                                   | Oscuro  |
| --- | ----------------- | -------- | --------------------------------- | ------- |
|     | --color-error-bg  |          | Fondo de badges de error en modo  |         |
oscuro

  Nota: Los colores semánticos de texto (--color-success, --color-warning, --color-error) se aclaran
automáticamente en modo oscuro para mantener contraste WCAG AA mínimo 4.5:1 sobre los fondos de
badge correspondientes.

Versión 1.0 · Junio 20255

Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
2.3 Tipografía
La fuente del sistema es Inter, disponible vía Google Fonts. Es la elección estándar para sistemas
SaaS de gestión en 2025: optimizada para pantallas de alta densidad, legible en tamaños pequeños y
neutra sin verse genérica.
La carga se realiza con font-display: swap para evitar FOIT. Se precargan únicamente los pesos 400 y
500.
Uso Tamaño Peso Color Ejemplo
Título de página 22px / 1.375rem 600 --color-text-1 Agenda
Título de sección / H2 16px / 1rem 600 --color-text-1 Citas de hoy
Subtítulo / H3 14px / 0.875rem 500 --color-text-2 Lunes 12 de junio
Cuerpo de texto 14px / 0.875rem 400 --color-text-1 El paciente refiere...
Etiqueta de campo 12px / 0.75rem 500 --color-text-2 Fecha de nacimiento
Metadato / timestamp 12px / 0.75rem 400 --color-text-3 09:14 hrs
Encabezado de columna 11px / 0.6875rem 600 --color-text-2 ESTADO
(uppercase)
Código / monoespaciado 13px / 0.8125rem 400 --color-text-2 #376D6D (tokens)
Restricción: No usar pesos 300 (thin) ni 700 (bold) en ningún elemento de la interfaz. El sistema usa
únicamente 400 (regular) y 500–600 (medium/semibold). Pesos más gruesos generan fatiga visual en uso
prolongado.
2.4 Iconografía
El sistema usa exclusivamente Tabler Icons en su variante outline, cargado vía webfont. No se usan
iconos filled ni iconos de otras librerías en la misma interfaz para mantener coherencia visual.
Tamaño Uso Ejemplo de contexto
16px Iconos inline en texto, etiquetas y badges Ícono junto a "Confirmado"
18px Ítems de navegación lateral Agenda, Pacientes, Caja
20px Acciones en topbar y botones Botón "+ Nueva cita"
24px Ilustraciones de estado vacío Pantalla "Sin citas hoy"
Nota: Los iconos decorativos llevan aria-hidden="true". Los iconos que actúan como único contenido de un
botón requieren aria-label descriptivo en el elemento padre.
2.5 Espaciado y grid
El sistema usa una escala de espaciado base 4px. Todos los valores de padding, margin y gap son
múltiplos de 4.
Versión 1.0 · Junio 20256

Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
| Token      | Valor  | Uso típico                                 |     |
| ---------- | ------ | ------------------------------------------ | --- |
| --space-1  | 4px    | Separación mínima entre elementos inline   |     |
| --space-2  | 8px    | Gap interno en componentes (icon + label)  |     |
| --space-3  | 12px   | Padding interno de badges y chips          |     |
| --space-4  | 16px   | Padding de tarjetas y celdas de tabla      |     |
| --space-5  | 20px   | Padding de paneles y secciones de página   |     |
| --space-6  | 24px   | Separación entre secciones de formulario   |     |
| --space-8  | 32px   | Margen entre módulos en una página         |     |

El layout de página usa un grid de 12 columnas con gutter de 16px. El área de contenido disponible
(descontando sidebar de 52px en modo ícono o 200px expandido) es de 1028–1228px en resolución
1280px.

2.6 Bordes, radios y elevación
| Elemento  | border- | Borde  | Justificación  |
| --------- | ------- | ------ | -------------- |
radius
Botones  5px  1px solid --color- Apariencia definida, sería sin ser
|                   |      | border             | agresivo                           |
| ----------------- | ---- | ------------------ | ---------------------------------- |
| Inputs y selects  | 3px  |                    |                                    |
|                   |      | 1px solid --color- | Consistencia con botones, aspecto  |
|                   |      | border             | de formulario técnico              |
Tarjetas / cards  6px  1px solid --color- Ligera suavidad para diferenciar
|     |     | border  | del fondo  |
| --- | --- | ------- | ---------- |
Badges de estado  20px  ninguno  Píldora — diferencia visual
respecto a botones
| Pills NOM  | 3px  | ninguno  | Rectangular para transmitir  |
| ---------- | ---- | -------- | ---------------------------- |
carácter normativo
Modal / drawer  8px  1px solid --color- Contenedor principal de acciones
|                        |      | border   | complejas  |
| ---------------------- | ---- | -------- | ---------- |
| Sidebar de navegación  | 0px  | ninguno  |            |
Ocupa el borde de pantalla, sin
radio
| Topbar  | 0px  | border-bottom  | Borde de plataforma, sin radio  |
| ------- | ---- | -------------- | ------------------------------- |
solamente

  Nota: El sistema no usa box-shadow para elevación. La jerarquía visual se logra exclusivamente con color
de fondo (--color-surface vs --color-card) y bordes. Esto garantiza que el sistema se vea limpio en ambos
modos sin artefactos de sombra en modo oscuro.

2.7 Componentes base
Botones
Versión 1.0 · Junio 20257

Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
Variante Uso Estilos clave
Primario Acción principal de la pantalla (guardar, bg: --color-primary / text: white / border:
confirmar, nueva cita) none
Secundario Acción alternativa (editar, exportar, bg: transparent / text: --color-primary /
vista previa) border: 1px --color-primary
Ghost Acción de baja prioridad (cancelar, bg: transparent / text: --color-text-2 /
cerrar) border: 1px --color-border
Destructivo Eliminar, cancelar cita, dar de baja bg: transparent / text: --color-error /
border: 1px --color-error
Icono solamente Acciones compactas en tablas y width: 32px / height: 32px / border-
toolbars radius: 5px
Restricción: Solo puede existir un botón Primario por vista o sección. Si una pantalla requiere dos
acciones primarias, una de ellas debe degradarse a Secundario.
Badges de estado
Estado Color texto Color fondo Uso en el sistema
Confirmado --color-success --color-success-bg Citas, pagos, expedientes activos
Pendiente --color-warning --color-warning-bg Citas sin confirmar, cobros parciales
En progreso 0A5F82 E0F0FA Cita en curso, documento en edición
Cancelado --color-error --color-error-bg Citas canceladas, documentos
anulados
Archivado --color-text-3 --color-surface Pacientes inactivos, planes cerrados
NOM-004 1A5C38 DAF2E8 Bloques de canvas protegidos por
norma
Inputs y selects
Todos los campos de formulario comparten la misma altura de 36px y el mismo estilo de borde. Los
estados son:
• Default: border 1px --color-border, fondo --color-card
• Focus: border 1px --color-primary, sin box-shadow adicional
• Error: border 1px --color-error, mensaje de error debajo en 12px --color-error
• Disabled: fondo --color-surface, texto --color-text-3, cursor not-allowed
• Read-only: igual que disabled pero con fondo ligeramente distinto para indicar que el valor existe
Tablas de datos
Las tablas son el componente más frecuente del sistema. Reglas invariables:
• Encabezados en 11px uppercase weight 600, fondo --color-surface
• Filas alternas: fondo --color-card y --color-surface para facilitar la lectura horizontal
• La primera columna siempre contiene el identificador principal (nombre del paciente, número de
póliza, etc.)
Versión 1.0 · Junio 20258

Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
• Acciones por fila (editar, ver, eliminar) aparecen solo en hover para reducir el ruido visual
• Las tablas con más de 6 columnas son candidatas a rediseño con vista de detalle en drawer
Modales y drawers
Componente Cuándo usarlo Ancho
Modal centrado Confirmaciones destructivas, alertas críticas, 480px máximo
acciones de 1-2 pasos
Drawer lateral Formularios complejos, detalle de registro, 480–560px
creación de cita o pago
Panel expandido Expediente clínico, canvas de documentos — 100% del área de contenido
requiere toda la pantalla
Nota: Los drawers se abren desde la derecha. El fondo detrás del drawer recibe un overlay
rgba(0,0,0,0.32). Hacer clic fuera del drawer cierra sin guardar, excepto cuando hay cambios no guardados
— en ese caso aparece un modal de confirmación.
2.8 Breakpoints y comportamiento responsive
Breakpoint Ancho Comportamiento del sidebar Módulos disponibles
Desktop 1280px+ Expandido (200px) o colapsado Todos
a íconos (52px) — toggle por
usuario
Laptop 1024–1279px Colapsado a íconos (52px) por Todos
defecto
Tablet 768–1023px Sidebar como drawer — Todos en modo
hamburger en topbar simplificado
Móvil < 768px Sidebar oculto — solo accesible Solo lectura de
via menú expediente y firma
digital
Restricción: En resoluciones móviles menores a 768px, los módulos de Caja, Agenda (edición), Canvas y
Libro Mayor muestran un mensaje: "Esta función está optimizada para pantallas más grandes. Por favor, usa
una tablet o computadora." El módulo de lectura de expediente y firma digital sí están disponibles en móvil.
Versión 1.0 · Junio 20259

Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
3. Shell de la Aplicación
El shell es la estructura visual persistente que rodea todos los módulos. Está compuesto por tres
elementos invariables: sidebar de navegación, topbar contextual y área de contenido. El shell nunca se
recarga entre navegaciones — la aplicación es un SPA (Single Page Application).
3.1 Layout general
Zona Dimensiones Posición Descripción
Sidebar de navegación 52px colapsado / 200px Fija, lado izquierdo, Navegación
expandido altura completa principal entre
módulos y
perfil de
usuario
Topbar contextual Altura 52px, ancho = 100% - Fija, parte superior del Título del
sidebar área de contenido módulo activo,
subtítulo y
acciones
principales
Área de contenido Resto del viewport Scrolleable Contenido del
verticalmente módulo activo
— varía por
pantalla
Nota: El área de contenido tiene padding interno de 20px en todos sus lados. Las tablas y el canvas
expanden hasta el 100% del área disponible descontando este padding.
3.2 Sidebar de navegación
Estructura
• Logo / marca: punto verde (#B8DBDB) + wordmark "MediCloud" — al colapsar solo queda el
punto
• Sección "Clínica": Agenda, Pacientes, Expediente
• Sección "Administración": Caja, Inventario, Contabilidad, Personal
• Zona inferior: Configuración (ícono engrane) + Avatar del usuario con iniciales
Estados de ítem de navegación
Estado Apariencia
Default Color: rgba(255,255,255,0.38) — ícono y texto en gris tenue
Hover Color: rgba(255,255,255,0.70) — fondo rgba(255,255,255,0.06)
Activo Color: blanco — fondo rgba(255,255,255,0.08) — border-left 2px #B8DBDB
Colapsado Solo ícono visible — tooltip con nombre del módulo al hacer hover
Versión 1.0 · Junio 202510

Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
Toggle de colapso
Un botón de flecha al borde inferior del sidebar (encima de la sección de usuario) permite al usuario
expandir o colapsar el sidebar. El estado persiste en localStorage. En tablet el sidebar arranca
colapsado. La transición de expansión/colapso es de 200ms con ease-out.

3.3 Topbar contextual
El topbar muestra el título del módulo activo y las acciones primarias de esa pantalla. Su contenido
cambia completamente al navegar entre módulos.
Composición
•  Izquierda: Título del módulo (22px / 600) + subtítulo contextual (12px / 400, --color-text-3).
Ejemplo: "Agenda" / "Lunes 12 de junio, 2026"
•  Centro: Vacío en la mayoría de módulos. En Agenda puede contener los controles de vista (Día /
Semana / Mes)
•  Derecha: Hasta 2 botones de acción (máximo 1 Primario + 1 Secundario o Ghost). En pantallas
de detalle puede incluir un breadcrumb de navegación

Acciones por módulo — referencia rápida
| Módulo        | Acción primaria     | Acción secundaria        |
| ------------- | ------------------- | ------------------------ |
| Agenda        | + Nueva cita        | Vista semana / mes       |
| Pacientes     | + Nuevo paciente    | Exportar listado         |
| Expediente    | + Nuevo documento   | Vista previa / imprimir  |
| Tratamientos  | + Nuevo plan        | —                        |
| Caja          | Registrar cobro     | Abrir / cerrar sesión    |
| Inventario    | + Agregar producto  | Ajuste de inventario     |
| Contabilidad  | —                   | Exportar periodo         |
| Personal      | + Agregar empleado  | —                        |

3.4 Cambio de clínica (multi-tenant)
Los usuarios con acceso a más de una clínica ven un selector de clínica en la parte superior del
sidebar, encima de las secciones de navegación. El selector muestra el nombre de la clínica activa con
un ícono de chevron. Al hacer clic despliega un dropdown con las clínicas disponibles.
•  Al cambiar de clínica: la aplicación recarga el área de contenido con los datos de la nueva clínica
•  El sidebar y el topbar muestran el nombre de la clínica activa en todo momento
•  Los datos nunca se mezclan entre clínicas — el RLS de PostgreSQL lo garantiza a nivel de base
de datos
  Nota: El selector de clínica solo es visible para usuarios con rol Admin o clinic_admin asignados en más de
una clínica. El médico o la recepcionista que trabajan en una sola clínica no ven este elemento.

Versión 1.0 · Junio 202511

Especificación UI/UX — Sistema de Gestión de Consultorios MédicosConfidencial · Fase 1 MVP
3.5 Perfil de usuario y sesión
El avatar en la parte inferior del sidebar (círculo con las iniciales del usuario, fondo --color-primary)
abre un dropdown con:
• Nombre completo y rol del usuario
• Clínica activa
• Enlace a preferencias de cuenta
• Toggle modo claro / modo oscuro
• Cerrar sesión
3.6 Modo claro / modo oscuro
El toggle de modo se encuentra en el dropdown de perfil de usuario. Al activarlo, el cambio de tema
aplica instantáneamente vía CSS custom properties sin recargar la página. La preferencia se guarda
en el perfil del usuario en base de datos (no solo en localStorage) para que persista entre dispositivos
y sesiones.
Aspecto Comportamiento
Valor predeterminado Modo claro para todos los usuarios nuevos
Persistencia Base de datos + localStorage como caché local
Transición CSS transition de 150ms en background-color y color para todos los
elementos
Imágenes y logos Las fotografías clínicas y logos de clínica no se alteran por el modo
Gráficas y charts Usan paleta adaptada definida en §10 (Contabilidad)
Versión 1.0 · Junio 202512