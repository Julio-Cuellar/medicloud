Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
ESPECIFICACIÓN DE ARQUITECTURA
Sistema de Gestión de Consultorios
Médicos
Documento de Arquitectura y Planificación por Fases
Versión 1.0 · Junio 2025 · Confidencial
Página 1

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
Tabla de Contenido
Página 2

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
1. Contexto y Objetivo
Este documento define la arquitectura, alcance funcional y plan de implementación por fases del sistema de
gestión de consultorios médicos. El sistema es una plataforma SaaS multi-tenant diseñada para cubrir la
operación integral de consultorios médicos bajo la normativa mexicana vigente (NOM-004-SSA3-2012 y
disposiciones complementarias).
El objetivo central es construir una solución donde la operación clínica diaria y la contabilidad sean la misma
cosa: cada acto clínico produce automáticamente su reflejo financiero, sin intervención manual y sin que el
médico necesite conocimientos contables. Simultáneamente, el contador dispone de un Libro Mayor técnico con
toda la trazabilidad necesaria para auditorías y declaraciones fiscales.
1.1 Jerarquía de Entidades
La unidad base del sistema es la clínica. La arquitectura anticipa la agregación de entidades en la siguiente
jerarquía:
• Usuario Administrador (dueño)
◦ Corporación Médica
Clínica / Consultorio
Sucursal (expansión futura)
Hospital (expansión futura)
Página 3

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
2. Principios Arquitectónicos Rectores
2.1 Separación entre operación clínica y contabilidad
Los módulos operativos (agenda, inventario, caja) nunca escriben asientos contables directamente. Publican
eventos de dominio que un motor de reglas contables centralizado (Accounting Rules Engine, ARE) consume de
forma asíncrona para generar las pólizas correspondientes en el Libro Mayor. Toda operación clínica tiene su
reflejo financiero automático e inmutable sin intervención manual.
2.2 Multi-tenancy jerárquico
Cada clínica tiene su propia configuración operativa (catálogo de cuentas, personal, inventario, pacientes y caja).
El Catálogo de Cuentas (CoA) es configurable por tenant pero hereda una estructura base no modificable,
garantizando consistencia en reportes corporativos.
2.3 Norma Oficial Mexicana como restricción de diseño
El sistema no valida contra la norma a posteriori. La norma está embebida en las plantillas, formularios y flujos de
trabajo. Es estructuralmente imposible que un médico genere un expediente o historia clínica que se salga de la
NOM vigente.
2.4 Doble audiencia en toda interfaz Financiera
Audiencia Representación Ejemplo
Médico / Recepcionista Lenguaje natural operativo "Se registró el cobro de $2,800"
Contador / Auditor Vista técnica del Libro Mayor Póliza #0041 — Déb. 11100 $2,800 / Créd.
41000 $2,800
2.5 Inmutabilidad de pólizas contables
Ninguna póliza puede modificarse una vez registrada. Toda corrección produce una nueva póliza referenciada a
la original. Las cancelaciones generan pólizas de ajuste con saldo negativo (reverso contable transparente).
2.6 Validación de integridad en partida doble
Antes de persistir cualquier conjunto de líneas de asiento, el sistema valida que Σ Débitos − Σ
Créditos = 0. Si esta condición no se cumple, la transacción completa se rechaza y se registra en el
log de auditoría.

Página 4

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
3. Roadmap de Implementación por Fases
Fase Nombre Objetivo principal Estado
Fase 1 MVP Entregable Operación clínica funcional y contabilidad interna Alcance inicial
básica
Fase 2 Madurez Operativa Inventario avanzado, nómina, presupuestación Post-MVP
Fase 3 Cumplimiento Fiscal CFDI 4.0, contabilidad electrónica SAT, Mediano plazo
conciliación bancaria
Fase 4 Inteligencia de Negocio BI, proyecciones, benchmarking entre clínicas Largo plazo
Fase 5 Ecosistema Extendido Portal paciente, aseguradoras, escala Visión futura
hospitalaria
FASE 1 — MVP ENTREGABLE
Esta fase constituye el primer producto entregable. El objetivo es que el médico pueda operar su
clínica de forma completa: gestionar pacientes, expedientes, citas, inventario básico y caja, con un
reflejo contable interno automático. La facturación electrónica (CFDI) NO está incluida en esta fase.
4. Fase 1 — MVP Entregable
4.1 Gestión Multi-Clínica
• Alta y administración de 1 a n clínicas por usuario administrador.
• Cada clínica con configuración independiente: nombre, dirección, RFC, logotipo, especialidades.
• Cambio de contexto entre clínicas desde la barra de navegación principal.
• Estructura de datos preparada para jerarquía corporativa (sin UI corporativa en esta fase).
4.2 Gestión de Pacientes
• Catálogo de pacientes con búsqueda por nombre, CURP, fecha de nacimiento y número de expediente.
• Alta y baja lógica de pacientes.
• Desde el perfil del paciente, acceso con un clic a: Historia Clínica, Expediente, Citas y Tratamientos y
Presupuestos.
• Los datos del paciente siguen la estructura mínima exigida por NOM-004-SSA3-2012: ficha de identificación
completa.
4.3 Expediente Clínico (NOM-004-SSA3-2012)
El expediente agrupa los siguientes tipos de documentos, todos creables desde la herramienta Canvas:
Página 5

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos  |  Confidencial
| Documento                         | Normativa de referencia  | Incluido en Fase 1  |
| --------------------------------- | ------------------------ | ------------------- |
| Historia Clínica                  | NOM-004-SSA3-2012        | ✅                   |
| Notas Médicas                     | NOM-004-SSA3-2012        | ✅                   |
| Consentimientos Informados        | NOM-004-SSA3-2012        | ✅                   |
| Recetas                           | NOM-072-SSA1-2012        | ✅                   |
| Presupuestos y Tratamientos       | Operativo                | ✅                   |
| Fotografías Clínicas              | Operativo                | ✅                   |
| Estudios de Laboratorio           | NOM-004-SSA3-2012        | ✅                   |
| Estudios de Imagen                | NOM-004-SSA3-2012        | ✅                   |
| Interconsultas                    | NOM-004-SSA3-2012        | ✅                   |
| Referencias y Contrarreferencias  | NOM-004-SSA3-2012        | ✅                   |
| Hojas de Enfermería               | NOM-004-SSA3-2012        | ✅                   |
| Documentos Administrativos        | Operativo                | ✅                   |

4.3.1 Herramienta Canvas para documentos clínicos
Barra de herramientas con los siguientes componentes arrastrables:
•  Cajas de texto libres.
•  Tablas preconstruidas: signos vitales, antecedentes heredofamiliares, enfermedades de transmisión sexual,
alergias, medicamentos actuales, revisión por aparatos y sistemas.
•  Tablas vacías configurables (columnas y filas definidas por el médico).
•  Bloques de imagen (fotografías clínicas, estudios).
•  Campos de firma digital (médico y paciente).
•  Bloques de fecha/hora automáticos.

Restricción de diseño: todos los bloques respetan la estructura mínima exigida por la NOM
correspondiente. El médico puede extender los documentos pero no puede omitir campos
normativos obligatorios.

4.3.2 Historia Clínica
Estructura obligatoria conforme a NOM-004-SSA3-2012:
1. Ficha de identificación
2. Motivo de consulta
3. Interrogatorio y antecedentes (hereditarios, patológicos, no patológicos, gineco-obstétricos cuando aplique)
4. Exploración física
5. Diagnóstico (presuntivo y definitivo)
6. Pronóstico
7. Plan de tratamiento
8. Evolución médica
Página 6

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
4.4 Tratamientos, Citas e Inventario Básico
4.4.1 Plan de tratamiento y cotización
• Sección 'Tratamientos y Presupuestos' en el perfil de cada paciente.
• Botón 'Agregar' cuando no existe un plan activo.
• El médico construye el presupuesto con apoyo de estudios de imagen, fotografías y el catálogo de
procedimientos.
• Cada procedimiento tiene asociado su lista de insumos necesarios con precios tomados del inventario en
tiempo real.
4.4.2 Ciclo de vida de insumos en tratamiento
Estado Disparador Efecto en Inventario Efecto Contable
Cotizado Médico crea el presupuesto Sin movimiento Sin asiento
Reservado Médico confirma la cita Disponible → Reservado Sin asiento aún
Consumido Médico confirma en Reservado → Baja ARE asienta Momento 2
ventana de conciliación definitiva
Las cantidades consumidas se registran en unidades decimales (NUMERIC) para soportar mililitros, gramos y
piezas fraccionadas. El médico o asistente valida las cantidades reales en la ventana de conciliación post-cita.
4.4.3 Gestión de citas
• Agenda visual con vistas diaria, semanal y mensual.
• Una cita puede contener de 0 a n procedimientos del plan de tratamiento.
• Al confirmar la cita: el stock de insumos requeridos pasa a estado 'reservado'.
• Al cancelar la cita: el stock reservado regresa a disponible de forma atómica. Si hubo cobro, el ARE genera
el reverso contable correspondiente.
4.5 Inventario Básico
• Alta, baja y modificación de productos e insumos.
• Entradas de inventario con costo unitario.
• Alertas configurables de stock mínimo y stock máximo.
• Alertas de caducidad para productos perecederos (configurable por producto).
• Kardex básico: registro cronológico de entradas, salidas y reservas.
• Ajustes de inventario por merma o caducidad (generan evento MermaCaducidad para el ARE).
El soporte para valuación por PEPS/UEPS/Costo Promedio, trazabilidad por lote y órdenes de
compra se implementará en Fase 2.
4.6 Gestión de Flujos de Caja
Página 7

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos  |  Confidencial
•  Apertura y cierre de caja con cuadre de turno.
•  Registro de pagos: contado, anticipos, abonos parciales a tratamientos.
•  Generación de tickets de venta (sin timbrado fiscal en esta fase).
•  Control de múltiples formas de pago: efectivo, transferencia, tarjeta.
•  Todos los movimientos de caja publican eventos que el ARE procesa para el Libro Mayor interno.

4.7 Motor de Reglas Contables (ARE) — Fase 1
4.7.1 Catálogo de Cuentas base
Estructura jerárquica estandarizada. La estructura raíz es no modificable; el tenant puede agregar subcuentas
dentro de cada rama:

| Código  | Nombre de cuenta                | Tipo     |
| ------- | ------------------------------- | -------- |
| 10000   | Activos                         | Raíz     |
| 11100   | Caja Operativa                  | Detalle  |
| 11200   | Bancos                          | Detalle  |
| 11300   | Cuentas por Cobrar              | Detalle  |
| 12100   | Almacén de Insumos Clínicos     | Detalle  |
| 20000   | Pasivos                         | Raíz     |
| 21100   | Anticipos de Pacientes          | Detalle  |
| 40000   | Ingresos                        | Raíz     |
| 41000   | Ingresos por Servicios Médicos  | Detalle  |
| 51000   | Costo Directo del Servicio      | Detalle  |
| 52000   | Gastos Operativos               | Raíz     |
| 52100   | Merma y Caducidad               | Detalle  |

4.7.2 Momentos contables obligatorios
Momento 1 — Cobro al paciente:
•  Débito: Caja Operativa (11100) o Bancos (11200)
•  Crédito: Ingresos por Servicios Médicos (41000) — si el pago es total
•  Crédito: Anticipos de Pacientes (21100) — si el pago es parcial o diferido

Momento 2 — Consumo real de insumos (post-conciliación):
•  Débito: Costo Directo del Servicio (51000)
•  Crédito: Almacén de Insumos Clínicos (12100)
•  Usando exactamente las cantidades validadas en la ventana de conciliación

Página 8

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
Momento 3 — Ajuste por merma o caducidad:
• Débito: Merma y Caducidad (52100)
• Crédito: Almacén de Insumos Clínicos (12100)
4.7.3 Eventos de dominio — contrato entre módulos
Evento publicado Nombre del evento Acción en ARE Acción en Inventario
por
Agenda CitaConfirmada Sin asiento Reserva stock
Caja PagoRegistrado Asiento Momento 1 —
Clínica ConsumoConciliado Asiento Momento 2 Baja definitiva
Agenda CitaCancelada Reverso si hubo pago Libera reserva
Inventario InsumoAgregado — Entrada de stock
Inventario MermaCaducidad Asiento Momento 3 Baja por ajuste
4.8 Personal del Consultorio
4.8.1 Roles y control de acceso (RBAC)
Rol Capacidades principales Restricciones clave
Admin (dueño) Control total de la plataforma Ninguna
Médico Expedientes, citas, tratamientos, Sin acceso a nómina ni Libro Mayor
recetas, conciliación de insumos
Recepcionista Agenda, caja, catálogo de pacientes Sin acceso a expediente clínico
Asistente Apoyo clínico, conciliación de insumos, Sin acceso a caja ni Libro Mayor
agenda
Administrador de Inventario, reportes básicos, personal Sin acceso a expediente clínico
clínica
Contador Libro Mayor, CoA, reportes financieros Solo lectura en módulos clínicos
Limpieza Sin acceso a módulos operativos Solo registro de asistencia
• Control de asistencia: registro de entrada y salida por empleado.
• Perfil de empleado: datos personales, rol, clínica asignada, salario (para Fase 2).
Página 9

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
FASE 2 — MADUREZ OPERATIVA
Esta fase profundiza las capacidades operativas: inventario de nivel profesional, nómina integrada y
presupuestación. No requiere integración fiscal externa.
5. Fase 2 — Madurez Operativa
5.1 Inventario Avanzado
5.1.1 Valuación de inventario
• Soporte para los tres métodos bajo NIF mexicanas: PEPS (FIFO), UEPS (LIFO) y Costo Promedio
Ponderado.
• El método se selecciona por clínica. Un cambio de método requiere un proceso de revaluación auditado.
• Costeo de cada lote al momento de la entrada, no al momento del consumo.
5.1.2 Trazabilidad por lote
• Cada entrada de inventario se asocia a un lote del proveedor.
• Trazabilidad completa: lote → factura de compra → paciente donde se consumió.
• Alerta de recall: si un lote está comprometido, el sistema identifica en qué pacientes se usó.
5.1.3 Órdenes de compra
• Generación de órdenes de compra con flujo de aprobación configurable.
• Comparativa de precios entre proveedores para el mismo insumo.
• Recepción parcial de OC con ajuste automático del saldo pendiente.
• Devoluciones a proveedor con nota de crédito vinculada.
• Matching de 3 vías: orden de compra → recepción de insumos → factura del proveedor.
5.1.4 Inventario físico periódico
• El usuario captura conteos físicos y el sistema genera diferencias automáticamente.
• Los ajustes generan el evento MermaCaducidad que el ARE contabiliza.
5.2 Cuentas por Pagar (CxP)
• Alta de proveedores con RFC, datos fiscales y cuenta bancaria.
• Registro de facturas de proveedores: insumos, renta, servicios.
Página 10

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
• Programación de pagos con calendarización y alertas de vencimiento.
• Estados de cuenta por proveedor.
5.3 Cuentas por Cobrar (CxC) — Pacientes
• Gestión de crédito a pacientes: saldo deudor, límite de crédito, días de crédito.
• Estados de cuenta por paciente exportables y enviables por correo o WhatsApp.
• Recordatorios automáticos de adeudo configurables (3, 7 y 30 días).
5.4 Nómina Integrada
• Cálculo de nómina semanal, quincenal o mensual con tabla ISR vigente del SAT.
• Cálculo de cuotas IMSS (obrero-patronal) e INFONAVIT.
• Cálculo de prestaciones de ley: aguinaldo, vacaciones, prima vacacional, PTU.
• Control de incidencias: faltas, horas extra, permisos, incapacidades IMSS.
• Integración con el módulo de asistencia para cálculo automático de incidencias.
• Generación del reporte SUA (Sistema Único de Autodeterminación) para pago bimestral al IMSS.
• Dispersión de nómina: generación de archivo SPEI en lote para el banco.
El timbrado de CFDI de nómina (complemento nomina12) se habilitará en Fase 3, cuando se integre
el PAC.
5.5 Presupuestación y Control de Costos
• Presupuesto anual por centro de costo: el administrador define metas de ingreso y techo de gasto por mes
y área.
• Dashboard de variación presupuestal: real vs. presupuestado en tiempo real con semáforo visual.
• Punto de equilibrio por clínica: cálculo automático de cuántas consultas o tratamientos se necesitan para
cubrir costos fijos.
• Centros de costo adicionales: cada clínica puede subdividirse por especialidad o área (Consulta General,
Ortodoncia, Cirugía, etc.).
5.6 Reportes Financieros Internos
• Estado de Resultados: mensual, acumulado y comparativo entre periodos.
• Balance General.
• Flujo de Efectivo (método directo e indirecto).
• Auxiliar de cuentas: detalle de movimientos por cuenta en un rango de fechas.
• Antigüedad de saldos (CxC y CxP).
• Todos exportables a PDF y Excel.
Página 11

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
Página 12

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
FASE 3 — CUMPLIMIENTO FISCAL
Esta fase integra el cumplimiento fiscal ante el SAT: CFDI 4.0, contabilidad electrónica (Anexo 24) y
conciliación bancaria. Requiere integración con un PAC certificado.
6. Fase 3 — Cumplimiento Fiscal
6.1 CFDI 4.0
• Emisión de facturas, notas de crédito, notas de cargo y complementos de pago (REP).
• Integración directa con PAC certificado (Finkok, SW Sapien o Diverza) para timbrado, cancelación y
consulta de estado ante el SAT.
• Gestión del ciclo de vida del CFDI: emitido → timbrado → cancelado → sustituido.
• Control de folios y series por clínica.
• Cancelación con y sin aceptación del receptor, conforme a reglas SAT vigentes.
• Soporte para régimen fiscal por clínica: RESICO, Persona Moral, Régimen General.
6.2 CFDI de Nómina (complemento nomina12)
• Timbrado de recibos de nómina habilitado al conectar el PAC.
• Generación y envío automático de recibo por correo al empleado.
6.3 Impuestos
• Cálculo automático de IVA (16%, tasa 0% y exento). Los servicios médicos son exentos de IVA bajo
condiciones específicas que el sistema identifica automáticamente.
• Retención de ISR e IVA para honorarios a médicos independientes.
• Soporte para declaraciones complementarias referenciadas a la declaración original.
6.4 Contabilidad Electrónica (Anexo 24 — SAT)
• Exportación del Catálogo de Cuentas en formato XML SAT (CatCtg) mensual.
• Exportación de la Balanza de Comprobación en formato XML SAT (BalCtg) mensual.
• Exportación de Pólizas en formato XML SAT (PolCtg) bajo requerimiento de auditoría.
• Números de cuenta alineados al estándar SAT para exportación directa sin mapeo manual.
• Control de periodos contables: apertura, cierre provisional y cierre definitivo. Un periodo cerrado
definitivamente no puede recibir pólizas.
6.5 Conciliación Bancaria
Página 13

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
• Importación de estados de cuenta bancarios (HSBC, Banamex, BBVA, Banorte) en formato CAMT.053 o
CSV.
• Motor de conciliación automática por monto, fecha y referencia.
• Gestión de partidas en tránsito y diferencias no conciliadas.
• Integración con Open Banking conforme a Ley Fintech mexicana cuando los bancos lo soporten.
Página 14

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
FASE 4 — INTELIGENCIA DE NEGOCIO
Esta fase transforma los datos operativos y financieros acumulados en inteligencia accionable para
el médico, el administrador y el contador.
7. Fase 4 — Inteligencia de Negocio
7.1 Dashboard KPI Clínico-Financiero
• Ingreso por médico, por tipo de tratamiento y por periodo.
• Costo promedio de consulta y margen por procedimiento.
• Tasa de ocupación de agenda.
• Tasa de conversión: pacientes en consulta que aceptan plan de tratamiento.
• Comparativo entre clínicas de la misma corporación.
7.2 Proyección de Flujo de Caja
• Basada en citas agendadas (ingresos esperados) más CxP programadas (egresos comprometidos).
• Vista de liquidez proyectada a 30, 60 y 90 días.
• Alertas cuando la proyección indica riesgo de liquidez negativa.
7.3 Alertas de Negocio Configurables
• 'Si el ingreso semanal cae más del 20% respecto a la semana anterior, notificar al admin.'
• 'Si el costo de insumos supera el X% del ingreso del mes, notificar al contador.'
• Alertas completamente configurables por el administrador.
7.4 Exportación a BI Externo
• Exportación de datos a Google Looker Studio y Microsoft Power BI vía API REST o conector nativo.
• Reportes personalizados con filtros por clínica, médico, periodo y centro de costo.
Página 15

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos | Confidencial
FASE 5 — ECOSISTEMA EXTENDIDO (VISIÓN FUTURA)
Esta fase expande el sistema más allá de los límites del consultorio. Su implementación depende de
la madurez del producto y del ecosistema regulatorio mexicano.
8. Fase 5 — Ecosistema Extendido
8.1 Portal del Paciente
• Acceso del paciente a su propio expediente clínico desde una app móvil o web.
• Consentimiento digital para compartir expediente con un nuevo médico.
• El médico receptor solicita acceso; el paciente autoriza mediante flujo seguro.
• El nuevo médico obtiene contexto clínico completo: diagnósticos previos, tratamientos, alergias,
medicamentos.
8.2 Integración con Aseguradoras (Largo Plazo)
Este módulo se contempla como una posibilidad de implementación futura lejana, condicionada a la
disponibilidad de APIs por parte de las aseguradoras mexicanas y al marco regulatorio aplicable.
• Gestión de deducibles, coaseguros y reembolsos con AXA, GNP, Metlife y otras.
• Estados de cuenta por aseguradora.
• Conciliación automática entre cobros a aseguradora y pagos recibidos.
8.3 Escala Hospitalaria y Corporativa
• Incorporación de la entidad Hospital por encima de la jerarquía de clínicas.
• UI corporativa: reportes consolidados a nivel corporación con desagregación por clínica.
• Gestión centralizada de proveedores y contratos a nivel corporación.
• Políticas de control de acceso heredables en cascada: corporación → clínica → empleado.
Página 16

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos  |  Confidencial
9. Resumen de Capacidades por Fase

| Capacidad                           | F1 MVP  | F2      | F3      | F4 BI  F5 Ecosist.  |
| ----------------------------------- | ------- | ------- | ------- | ------------------- |
|                                     |         | Madur.  | Fiscal  |                     |
| Gestión multi-clínica               | ✅       | —       | —       | —  —                |
| Catálogo de pacientes y expediente  | ✅       | —       | —       | —  —                |
| Historia clínica NOM-004            | ✅       | —       | —       | —  —                |
| Herramienta Canvas para documentos  | ✅       | —       | —       | —  —                |
| Agenda y gestión de citas           |         | —       | —       | —  —                |
✅
| Tratamientos y presupuestos  |     | —   | —   | —  —  |
| ---------------------------- | --- | --- | --- | ----- |
✅
| Inventario básico con alertas  |     | —   | —   | —  —  |
| ------------------------------ | --- | --- | --- | ----- |
✅
| Gestión de caja y pagos  |     | —   | —   | —  —  |
| ------------------------ | --- | --- | --- | ----- |
✅
| Motor ARE + Libro Mayor interno  |     | —   | —   | —  —  |
| -------------------------------- | --- | --- | --- | ----- |
✅
| RBAC y control de personal  |     | —   | —   | —  —  |
| --------------------------- | --- | --- | --- | ----- |
✅
| Inventario avanzado (lotes/OC)  | —   |     | —   | —  —  |
| ------------------------------- | --- | --- | --- | ----- |
✅
| Nómina (sin timbrado)  | —   |     | —   | —  —  |
| ---------------------- | --- | --- | --- | ----- |
✅
| CxP / CxC pacientes  | —   |     | —   | —  —  |
| -------------------- | --- | --- | --- | ----- |
✅
| Presupuestación y punto de equilibrio  | —   |     | —   | —  —  |
| -------------------------------------- | --- | --- | --- | ----- |
✅
| Reportes financieros estándar  | —   |     | —   | —  —  |
| ------------------------------ | --- | --- | --- | ----- |
✅
| CFDI 4.0 + PAC  | —   | —   |     | —  —  |
| --------------- | --- | --- | --- | ----- |
✅
| CFDI nómina (nomina12)  | —   | —   |     | —  —  |
| ----------------------- | --- | --- | --- | ----- |
✅
| Contabilidad electrónica SAT (Anexo 24)  | —   | —   |     | —  —  |
| ---------------------------------------- | --- | --- | --- | ----- |
✅
| Conciliación bancaria               | —   | —   | ✅   | —  —  |
| ----------------------------------- | --- | --- | --- | ----- |
| Dashboard KPI y proyección de caja  | —   | —   | —   | ✅  —  |
| Exportación a BI externo            | —   | —   | —   | ✅  —  |
| Portal del paciente                 | —   | —   | —   | —  ✅  |
| Integración aseguradoras            | —   | —   | —   | —  ✅  |
| Escala hospitalaria / corporativa   | —   | —   | —   | —  ✅  |

La ventaja competitiva real frente a la competencia de sistemas contables no es igualar su
profundidad en contabilidad general. Es ser la única solución donde la operación clínica y la
contabilidad son la misma cosa: cada acto clínico genera automáticamente su reflejo financiero, en
lenguaje que cualquier médico entiende y con la trazabilidad que cualquier contador necesita.

Página 17

Especificación de Arquitectura — Sistema de Gestión de Consultorios Médicos  |  Confidencial
Apéndice A — Normativa Mexicana de Referencia

| Norma  | Descripción  | Módulo afectado  |
| ------ | ------------ | ---------------- |
NOM-004-SSA3-2012  Del expediente clínico  Expediente, Historia Clínica
NOM-072-SSA1-2012  Etiquetado de medicamentos  Recetas, Inventario
NOM-024-SSA3-2010  Sistemas de información en salud  Expediente electrónico
Anexo 24 SAT  Contabilidad electrónica  Módulo contable (Fase 3)
CFDI 4.0 SAT  Comprobantes fiscales digitales  Facturación (Fase 3)
Ley Federal del Trabajo  Derechos laborales y nómina  Nómina (Fase 2)
| Ley del IMSS / INFONAVIT  | Seguridad social  | Nómina (Fase 2)                 |
| ------------------------- | ----------------- | ------------------------------- |
| Ley Fintech               | Open Banking      | Conciliación bancaria (Fase 3)  |

Página 18