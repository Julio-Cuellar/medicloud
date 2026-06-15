const {
    Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
    Header, Footer, AlignmentType, HeadingLevel, BorderStyle, WidthType,
    ShadingType, PageNumber, LevelFormat, TableOfContents, PageBreak, PageOrientation
} = require('docx');
const fs = require('fs');

// ── THEME ──────────────────────────────────────────────────────────────────
const C = {
    brand: "1A3A5C",
    brandMid: "2E75B6",
    brandLight: "D6E4F0",
    accent: "1A6B3A",   // green for PK
    pkFill: "E6F4EA",
    fkFill: "FFF8E1",
    ixFill: "F3E5F5",
    nullFill: "FAFAFA",
    headerFill: "1A3A5C",
    rowAlt: "F5F8FC",
    white: "FFFFFF",
    gray: "666666",
    darkText: "1A1A1A",
    red: "C0392B",
    orange: "D35400",
    purple: "6C3483",
    teal: "0E6655",
};

const border1 = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
const borders = { top: border1, bottom: border1, left: border1, right: border1 };
const noBorder = { style: BorderStyle.NONE, size: 0, color: "FFFFFF" };
const noBorders = { top: noBorder, bottom: noBorder, left: noBorder, right: noBorder };

// ── HELPERS ────────────────────────────────────────────────────────────────
const sp = (n = 120) => new Paragraph({ spacing: { before: n, after: 0 }, children: [new TextRun("")] });

function h1(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_1,
        spacing: { before: 360, after: 180 },
        border: { bottom: { style: BorderStyle.SINGLE, size: 8, color: C.brandMid, space: 6 } },
        children: [new TextRun({ text, font: "Arial", size: 32, bold: true, color: C.brand })]
    });
}
function h2(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_2,
        spacing: { before: 280, after: 140 },
        children: [new TextRun({ text, font: "Arial", size: 26, bold: true, color: C.brandMid })]
    });
}
function h3(text) {
    return new Paragraph({
        heading: HeadingLevel.HEADING_3,
        spacing: { before: 200, after: 100 },
        children: [new TextRun({ text, font: "Arial", size: 22, bold: true, color: "2C3E50" })]
    });
}
function p(text, opts = {}) {
    return new Paragraph({
        spacing: { before: 60, after: 100 },
        children: [new TextRun({ text, font: "Arial", size: 19, color: C.darkText, ...opts })]
    });
}
function pb() { return new Paragraph({ children: [new PageBreak()] }); }

function note(text, fill = C.brandLight) {
    return new Table({
        width: { size: 9360, type: WidthType.DXA }, columnWidths: [9360],
        rows: [new TableRow({
            children: [new TableCell({
                borders: noBorders,
                shading: { fill, type: ShadingType.CLEAR },
                margins: { top: 140, bottom: 140, left: 200, right: 200 },
                width: { size: 9360, type: WidthType.DXA },
                children: [new Paragraph({
                    spacing: { before: 0, after: 0 },
                    children: [new TextRun({ text, font: "Arial", size: 18, color: C.darkText, italics: true })]
                })]
            })]
        })],
    });
}

function sectionBanner(text, fill = C.brand) {
    return new Table({
        width: { size: 9360, type: WidthType.DXA }, columnWidths: [9360],
        rows: [new TableRow({
            children: [new TableCell({
                borders: noBorders,
                shading: { fill, type: ShadingType.CLEAR },
                margins: { top: 160, bottom: 160, left: 240, right: 240 },
                width: { size: 9360, type: WidthType.DXA },
                children: [new Paragraph({
                    spacing: { before: 0, after: 0 },
                    children: [new TextRun({ text, font: "Arial", size: 26, bold: true, color: C.white })]
                })]
            })]
        })],
    });
}

function legend() {
    return new Table({
        width: { size: 9360, type: WidthType.DXA }, columnWidths: [2340, 2340, 2340, 2340],
        rows: [new TableRow({
            children: [
                cell("🔑  PK — Clave primaria", C.pkFill, 2340, true),
                cell("🔗  FK — Clave foránea", C.fkFill, 2340, true),
                cell("🔍  IX — Índice", C.ixFill, 2340, true),
                cell("❗  NOT NULL obligatorio", "FEE2E2", 2340, true),
            ]
        })],
    });
}

function cell(text, fill, width, bold = false, color = C.darkText, size = 17) {
    return new TableCell({
        borders,
        shading: { fill, type: ShadingType.CLEAR },
        margins: { top: 70, bottom: 70, left: 110, right: 110 },
        width: { size: width, type: WidthType.DXA },
        children: [new Paragraph({
            spacing: { before: 0, after: 0 },
            children: [new TextRun({ text: String(text), font: "Arial", size, bold, color })]
        })]
    });
}

// Main table renderer
// cols: [{label, width}]
// rows: [{col, type, constraints, fk, index, description}]
function tableBlock(tableName, schemaName, description, cols, rows) {
    const totalW = 9360;
    // Header row
    const hdrRow = new TableRow({
        children: cols.map(c => new TableCell({
            borders,
            shading: { fill: C.headerFill, type: ShadingType.CLEAR },
            margins: { top: 80, bottom: 80, left: 110, right: 110 },
            width: { size: c.width, type: WidthType.DXA },
            children: [new Paragraph({
                spacing: { before: 0, after: 0 },
                children: [new TextRun({ text: c.label, font: "Arial", size: 18, bold: true, color: C.white })]
            })]
        }))
    });

    const dataRows = rows.map((r, i) => {
        // pick row fill based on constraint
        let fill = i % 2 === 0 ? C.white : C.rowAlt;
        if (r.constraints && r.constraints.includes("PK")) fill = C.pkFill;
        else if (r.constraints && r.constraints.includes("FK")) fill = C.fkFill;

        return new TableRow({
            children: [
                cell(r.col, fill, cols[0].width, r.constraints && r.constraints.includes("PK"), r.constraints && r.constraints.includes("PK") ? C.accent : C.darkText, 17),
                cell(r.type, fill, cols[1].width, false, C.purple, 17),
                cell(r.constraints || "", fill, cols[2].width, false, C.red, 17),
                cell(r.fk || "", fill, cols[3].width, false, C.orange, 17),
                cell(r.index || "", fill, cols[4].width, false, C.teal, 17),
                cell(r.description, fill, cols[5].width, false, C.darkText, 17),
            ]
        });
    });

    const titlePara = new Paragraph({
        spacing: { before: 240, after: 80 },
        children: [
            new TextRun({ text: `${schemaName}.`, font: "Arial", size: 22, color: C.gray }),
            new TextRun({ text: tableName, font: "Arial", size: 22, bold: true, color: C.brand }),
            new TextRun({ text: `  —  ${description}`, font: "Arial", size: 19, color: C.gray, italics: true }),
        ]
    });

    return [titlePara, new Table({
        width: { size: totalW, type: WidthType.DXA },
        columnWidths: cols.map(c => c.width),
        rows: [hdrRow, ...dataRows],
    })];
}

const COLS = [
    { label: "Columna", width: 1700 },
    { label: "Tipo", width: 1400 },
    { label: "Constraints", width: 1000 },
    { label: "FK → Tabla", width: 1600 },
    { label: "Índice", width: 900 },
    { label: "Descripción", width: 2760 },
];

// ═══════════════════════════════════════════════════════════════════════════
// TABLE DEFINITIONS
// ═══════════════════════════════════════════════════════════════════════════

// Helper for UUID PK row
const pkRow = (desc = "Identificador único universal") => ({
    col: "id", type: "UUID", constraints: "PK, NOT NULL", fk: "", index: "PK", description: desc
});
const tsCreated = { col: "created_at", type: "TIMESTAMPTZ", constraints: "NOT NULL", fk: "", index: "IX", description: "Fecha y hora de creación (UTC)" };
const tsUpdated = { col: "updated_at", type: "TIMESTAMPTZ", constraints: "NOT NULL", fk: "", index: "", description: "Última modificación (auto-update trigger)" };
const tsDeleted = { col: "deleted_at", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "IX", description: "Soft delete — NULL = activo" };
const isActive = { col: "is_active", type: "BOOLEAN", constraints: "NOT NULL", fk: "", index: "IX", description: "TRUE = activo, FALSE = dado de baja" };

// ── SCHEMA: core ──────────────────────────────────────────────────────────

const tUsers = tableBlock("users", "core", "Usuarios del sistema (todos los roles)", COLS, [
    pkRow(),
    { col: "email", type: "CITEXT", constraints: "NOT NULL, UNIQUE", fk: "", index: "UQ", description: "Correo electrónico — usado como login" },
    { col: "phone", type: "VARCHAR(20)", constraints: "NULL", fk: "", index: "", description: "Teléfono de contacto" },
    { col: "full_name", type: "VARCHAR(200)", constraints: "NOT NULL", fk: "", index: "IX", description: "Nombre completo" },
    { col: "password_hash", type: "TEXT", constraints: "NOT NULL", fk: "", index: "", description: "Bcrypt hash de la contraseña" },
    { col: "avatar_url", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "URL del avatar en object storage" },
    { col: "email_verified", type: "BOOLEAN", constraints: "NOT NULL DEFAULT FALSE", fk: "", index: "", description: "Correo verificado" },
    { col: "last_login_at", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "", description: "Último inicio de sesión" },
    tsCreated, tsUpdated, isActive,
]);

const tOrganizations = tableBlock("organizations", "core", "Corporaciones / grupos médicos (capa opcional sobre clínicas)", COLS, [
    pkRow(),
    { col: "name", type: "VARCHAR(200)", constraints: "NOT NULL", fk: "", index: "IX", description: "Razón social o nombre comercial" },
    { col: "rfc", type: "VARCHAR(13)", constraints: "NULL, UNIQUE", fk: "", index: "UQ", description: "RFC de la corporación (persona moral)" },
    { col: "owner_user_id", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "IX", description: "Usuario dueño de la corporación" },
    { col: "logo_url", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Logotipo en object storage" },
    tsCreated, tsUpdated, isActive,
]);

const tClinics = tableBlock("clinics", "core", "Clínicas / consultorios — unidad operativa base", COLS, [
    pkRow(),
    { col: "organization_id", type: "UUID", constraints: "NULL", fk: "organizations.id", index: "IX", description: "FK corporación — NULL si es clínica independiente" },
    { col: "owner_user_id", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "IX", description: "Dueño / admin de la clínica" },
    { col: "name", type: "VARCHAR(200)", constraints: "NOT NULL", fk: "", index: "IX", description: "Nombre comercial del consultorio" },
    { col: "legal_name", type: "VARCHAR(300)", constraints: "NULL", fk: "", index: "", description: "Razón social para CFDI (Fase 3)" },
    { col: "rfc", type: "VARCHAR(13)", constraints: "NULL", fk: "", index: "", description: "RFC (requerido en Fase 3)" },
    { col: "tax_regime_code", type: "VARCHAR(10)", constraints: "NULL", fk: "", index: "", description: "Código régimen fiscal SAT (Fase 3)" },
    { col: "address_street", type: "VARCHAR(300)", constraints: "NOT NULL", fk: "", index: "", description: "Calle y número" },
    { col: "address_city", type: "VARCHAR(100)", constraints: "NOT NULL", fk: "", index: "", description: "Ciudad" },
    { col: "address_state", type: "VARCHAR(100)", constraints: "NOT NULL", fk: "", index: "", description: "Estado" },
    { col: "address_zip", type: "VARCHAR(10)", constraints: "NOT NULL", fk: "", index: "", description: "Código postal" },
    { col: "phone", type: "VARCHAR(20)", constraints: "NULL", fk: "", index: "", description: "Teléfono de la clínica" },
    { col: "specialties", type: "TEXT[]", constraints: "NOT NULL DEFAULT '{}'", fk: "", index: "", description: "Array de especialidades (ej: Odontología, Medicina General)" },
    { col: "logo_url", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Logotipo" },
    { col: "timezone", type: "VARCHAR(60)", constraints: "NOT NULL DEFAULT 'America/Mexico_City'", fk: "", index: "", description: "Zona horaria operativa" },
    tsCreated, tsUpdated, isActive,
]);

const tClinicStaff = tableBlock("clinic_staff", "core", "Relación usuarios ↔ clínicas con rol asignado", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica a la que pertenece el empleado" },
    { col: "user_id", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "IX", description: "Usuario del empleado" },
    { col: "role", type: "staff_role", constraints: "NOT NULL", fk: "", index: "IX", description: "ENUM: admin | doctor | receptionist | assistant | accountant | cleaning | clinic_admin" },
    { col: "employee_code", type: "VARCHAR(50)", constraints: "NULL", fk: "", index: "", description: "Código interno del empleado" },
    { col: "salary", type: "NUMERIC(12,2)", constraints: "NULL", fk: "", index: "", description: "Salario base (para nómina — Fase 2)" },
    { col: "salary_period", type: "VARCHAR(20)", constraints: "NULL", fk: "", index: "", description: "weekly | biweekly | monthly" },
    { col: "hire_date", type: "DATE", constraints: "NULL", fk: "", index: "", description: "Fecha de contratación" },
    { col: "end_date", type: "DATE", constraints: "NULL", fk: "", index: "", description: "Fecha de baja — NULL = activo" },
    { col: "notes", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Notas administrativas" },
    tsCreated, tsUpdated,
    { col: "UNIQUE", type: "—", constraints: "(clinic_id, user_id)", fk: "", index: "UQ", description: "Un usuario tiene un solo rol activo por clínica" },
]);

const tAttendance = tableBlock("attendance", "core", "Registro de entradas y salidas del personal", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica" },
    { col: "staff_id", type: "UUID", constraints: "NOT NULL", fk: "clinic_staff.id", index: "IX", description: "Empleado" },
    { col: "check_in_at", type: "TIMESTAMPTZ", constraints: "NOT NULL", fk: "", index: "IX", description: "Hora de entrada" },
    { col: "check_out_at", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "", description: "Hora de salida — NULL si aún está activo el turno" },
    { col: "notes", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Observaciones del turno" },
    tsCreated,
]);

// ── SCHEMA: patients ──────────────────────────────────────────────────────

const tSystemAlerts = tableBlock("system_alerts", "core", "Alertas operativas y de inventario", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica origen de la alerta" },
    { col: "alert_type", type: "VARCHAR(60)", constraints: "NOT NULL", fk: "", index: "IX", description: "stock_below_min | stock_above_max | expiry_warning | expiry_expired | cash_difference" },
    { col: "severity", type: "VARCHAR(20)", constraints: "NOT NULL DEFAULT 'warning'", fk: "", index: "", description: "info | warning | critical" },
    { col: "entity_type", type: "VARCHAR(60)", constraints: "NOT NULL", fk: "", index: "IX", description: "product | cash_session | appointment | etc." },
    { col: "entity_id", type: "UUID", constraints: "NOT NULL", fk: "", index: "IX", description: "ID de la entidad afectada" },
    { col: "title", type: "TEXT", constraints: "NOT NULL", fk: "", index: "", description: "Texto en lenguaje natural de la alerta" },
    { col: "metadata", type: "JSONB", constraints: "NULL", fk: "", index: "GIN", description: "Datos adicionales de contexto para la alerta" },
    { col: "status", type: "VARCHAR(20)", constraints: "NOT NULL DEFAULT 'pending'", fk: "", index: "IX", description: "pending | read | resolved | snoozed" },
    { col: "read_by", type: "UUID", constraints: "NULL", fk: "users.id", index: "IX", description: "Usuario que marcó como leída" },
    { col: "read_at", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "", description: "Fecha de lectura" },
    { col: "resolved_by", type: "UUID", constraints: "NULL", fk: "users.id", index: "IX", description: "Usuario que resolvió la alerta" },
    { col: "resolved_at", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "", description: "Fecha de resolución" },
    { col: "snoozed_until", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "", description: "Fecha hasta la que la alerta queda pospuesta" },
    tsCreated,
]);

const tPatients = tableBlock("patients", "patients", "Catálogo de pacientes del consultorio", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica a la que pertenece el paciente" },
    { col: "record_number", type: "VARCHAR(50)", constraints: "NOT NULL", fk: "", index: "UQ", description: "Número de expediente único por clínica (auto-generado)" },
    { col: "first_name", type: "VARCHAR(100)", constraints: "NOT NULL", fk: "", index: "IX", description: "Nombre(s)" },
    { col: "last_name_paternal", type: "VARCHAR(100)", constraints: "NOT NULL", fk: "", index: "IX", description: "Apellido paterno" },
    { col: "last_name_maternal", type: "VARCHAR(100)", constraints: "NULL", fk: "", index: "IX", description: "Apellido materno" },
    { col: "curp", type: "VARCHAR(18)", constraints: "NULL", fk: "", index: "IX", description: "CURP — único en la clínica si se captura" },
    { col: "birth_date", type: "DATE", constraints: "NOT NULL", fk: "", index: "IX", description: "Fecha de nacimiento" },
    { col: "sex", type: "sex_type", constraints: "NOT NULL", fk: "", index: "", description: "ENUM: male | female | other" },
    { col: "blood_type", type: "VARCHAR(5)", constraints: "NULL", fk: "", index: "", description: "Tipo de sangre (A+, O-, etc.)" },
    { col: "nationality", type: "VARCHAR(80)", constraints: "NOT NULL DEFAULT 'Mexicana'", fk: "", index: "", description: "Nacionalidad" },
    { col: "occupation", type: "VARCHAR(100)", constraints: "NULL", fk: "", index: "", description: "Ocupación / profesión" },
    { col: "religion", type: "VARCHAR(80)", constraints: "NULL", fk: "", index: "", description: "Religión (relevante para consentimientos)" },
    { col: "marital_status", type: "VARCHAR(30)", constraints: "NULL", fk: "", index: "", description: "Estado civil" },
    { col: "education_level", type: "VARCHAR(60)", constraints: "NULL", fk: "", index: "", description: "Escolaridad" },
    { col: "phone_mobile", type: "VARCHAR(20)", constraints: "NULL", fk: "", index: "", description: "Teléfono celular" },
    { col: "phone_home", type: "VARCHAR(20)", constraints: "NULL", fk: "", index: "", description: "Teléfono fijo" },
    { col: "email", type: "CITEXT", constraints: "NULL", fk: "", index: "", description: "Correo electrónico del paciente" },
    { col: "address_street", type: "VARCHAR(300)", constraints: "NULL", fk: "", index: "", description: "Domicilio — calle y número" },
    { col: "address_city", type: "VARCHAR(100)", constraints: "NULL", fk: "", index: "", description: "Ciudad" },
    { col: "address_state", type: "VARCHAR(100)", constraints: "NULL", fk: "", index: "", description: "Estado" },
    { col: "address_zip", type: "VARCHAR(10)", constraints: "NULL", fk: "", index: "", description: "Código postal" },
    { col: "emergency_contact_name", type: "VARCHAR(200)", constraints: "NULL", fk: "", index: "", description: "Nombre del contacto de emergencia" },
    { col: "emergency_contact_phone", type: "VARCHAR(20)", constraints: "NULL", fk: "", index: "", description: "Teléfono del contacto de emergencia" },
    { col: "emergency_contact_rel", type: "VARCHAR(60)", constraints: "NULL", fk: "", index: "", description: "Parentesco con el contacto de emergencia" },
    { col: "referring_doctor", type: "VARCHAR(200)", constraints: "NULL", fk: "", index: "", description: "Médico que refirió al paciente" },
    { col: "photo_url", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Fotografía del paciente en object storage" },
    { col: "notes", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Notas generales del paciente" },
    tsCreated, tsUpdated, tsDeleted,
    { col: "UNIQUE", type: "—", constraints: "(clinic_id, record_number)", fk: "", index: "UQ", description: "Número de expediente único por clínica" },
]);

// ── SCHEMA: clinical_records ───────────────────────────────────────────────

const tMedicalRecords = tableBlock("medical_records", "clinical", "Expediente clínico — contenedor raíz por paciente", COLS, [
    pkRow(),
    { col: "patient_id", type: "UUID", constraints: "NOT NULL, UNIQUE", fk: "patients.id", index: "UQ", description: "Un expediente por paciente (1:1)" },
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica donde se abrió el expediente" },
    { col: "opened_at", type: "DATE", constraints: "NOT NULL", fk: "", index: "", description: "Fecha de apertura del expediente" },
    { col: "opened_by", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "IX", description: "Médico que creó el expediente" },
    { col: "status", type: "VARCHAR(20)", constraints: "NOT NULL DEFAULT 'active'", fk: "", index: "IX", description: "active | archived | transferred" },
    tsCreated, tsUpdated,
]);

const tClinicalDocs = tableBlock("clinical_documents", "clinical", "Documentos individuales dentro del expediente (firmas normalizadas en tabla separada)", COLS, [
    pkRow(),
    { col: "medical_record_id", type: "UUID", constraints: "NOT NULL", fk: "medical_records.id", index: "IX", description: "Expediente al que pertenece" },
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica (desnormalizado para RLS)" },
    { col: "doc_type", type: "doc_type", constraints: "NOT NULL", fk: "", index: "IX", description: "ENUM: clinical_history | medical_note | lab_study | imaging | informed_consent | prescription | interconsult | referral | counter_referral | nursing_sheet | budget | clinical_photo | administrative" },
    { col: "title", type: "VARCHAR(300)", constraints: "NOT NULL", fk: "", index: "", description: "Título del documento" },
    { col: "content_json", type: "JSONB", constraints: "NULL", fk: "", index: "GIN", description: "Contenido estructurado del canvas (bloques y valores)" },
    { col: "content_html", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Renderizado HTML para vista previa e impresión" },
    { col: "template_id", type: "UUID", constraints: "NULL", fk: "doc_templates.id", index: "IX", description: "Plantilla de origen — NULL si es documento libre" },
    { col: "status", type: "VARCHAR(20)", constraints: "NOT NULL DEFAULT 'draft'", fk: "", index: "IX", description: "draft | final | signed | cancelled" },
    { col: "created_by", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "IX", description: "Usuario que creó el documento" },
    tsCreated, tsUpdated, tsDeleted,
]);

const tDocumentSignatures = tableBlock("document_signatures", "clinical", "Firmas digitales de documentos clínicos", COLS, [
    pkRow(),
    { col: "clinical_document_id", type: "UUID", constraints: "NOT NULL", fk: "clinical_documents.id", index: "IX", description: "Documento firmado" },
    { col: "signer_user_id", type: "UUID", constraints: "NULL", fk: "users.id", index: "IX", description: "Firmante staff; NULL si firma el paciente" },
    { col: "signer_patient_id", type: "UUID", constraints: "NULL", fk: "patients.id", index: "IX", description: "Firmante paciente; NULL si firma staff" },
    { col: "signer_role", type: "VARCHAR(40)", constraints: "NOT NULL", fk: "", index: "IX", description: "doctor | patient | witness" },
    { col: "signature_url", type: "TEXT", constraints: "NOT NULL", fk: "", index: "", description: "URL de la imagen de firma en object storage" },
    { col: "signature_method", type: "VARCHAR(30)", constraints: "NOT NULL", fk: "", index: "", description: "drawn | typed | biometric" },
    { col: "ip_address", type: "INET", constraints: "NULL", fk: "", index: "", description: "IP del dispositivo desde el que se firmó" },
    { col: "signed_at", type: "TIMESTAMPTZ", constraints: "NOT NULL DEFAULT now()", fk: "", index: "", description: "Fecha y hora de la firma" },
    tsCreated,
]);

const tClinicalHistory = tableBlock("clinical_histories", "clinical", "Historia clínica estructurada (NOM-004-SSA3-2012)", COLS, [
    pkRow(),
    { col: "clinical_document_id", type: "UUID", constraints: "NOT NULL, UNIQUE", fk: "clinical_documents.id", index: "UQ", description: "Documento base (1:1)" },
    { col: "patient_id", type: "UUID", constraints: "NOT NULL", fk: "patients.id", index: "IX", description: "Paciente" },
    // Motivo
    { col: "chief_complaint", type: "TEXT", constraints: "NOT NULL", fk: "", index: "", description: "Motivo de consulta" },
    // Antecedentes heredofamiliares
    { col: "family_hx", type: "JSONB", constraints: "NULL", fk: "", index: "", description: "Antecedentes heredofamiliares: {diabetes, hta, cancer, cardiac, other}" },
    // Antecedentes personales no patológicos
    { col: "nonpath_personal_hx", type: "JSONB", constraints: "NULL", fk: "", index: "", description: "No patológicos: {diet, exercise, tobacco, alcohol, drugs, housing, occupation}" },
    // Antecedentes personales patológicos
    { col: "path_personal_hx", type: "JSONB", constraints: "NULL", fk: "", index: "", description: "Patológicos: {chronic_diseases, surgeries, hospitalizations, transfusions, allergies, traumatisms}" },
    // Antecedentes gineco-obstétricos
    { col: "gyneco_obs_hx", type: "JSONB", constraints: "NULL", fk: "", index: "", description: "Gineco-obstétricos (cuando aplica): {menarche, cycles, pregnancies, births, abortions, last_menstrual_period, contraception}" },
    // Exploración física
    { col: "physical_exam", type: "JSONB", constraints: "NULL", fk: "", index: "", description: "Exploración física: {weight_kg, height_cm, bmi, bp_systolic, bp_diastolic, heart_rate, temp_c, spo2, general_appearance, systems_review}" },
    // Diagnóstico
    { col: "diagnosis_presumptive", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Diagnóstico presuntivo en texto libre" },
    { col: "diagnosis_definitive", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Diagnóstico definitivo" },
    { col: "diagnosis_cie10_codes", type: "TEXT[]", constraints: "NULL", fk: "", index: "GIN", description: "Array de códigos CIE-10" },
    // Pronóstico
    { col: "prognosis", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Pronóstico" },
    // Plan de tratamiento
    { col: "treatment_plan", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Plan de tratamiento en texto libre" },
    tsCreated, tsUpdated,
]);

const tDocTemplates = tableBlock("doc_templates", "clinical", "Plantillas de documentos clínicos creadas en el canvas", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NULL", fk: "clinics.id", index: "IX", description: "NULL = plantilla global del sistema; UUID = plantilla de la clínica" },
    { col: "doc_type", type: "doc_type", constraints: "NOT NULL", fk: "", index: "IX", description: "Tipo de documento al que aplica la plantilla" },
    { col: "name", type: "VARCHAR(200)", constraints: "NOT NULL", fk: "", index: "", description: "Nombre de la plantilla" },
    { col: "description", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Descripción de uso" },
    { col: "canvas_json", type: "JSONB", constraints: "NOT NULL", fk: "", index: "", description: "Definición del canvas: bloques, posiciones, campos obligatorios y tipos" },
    { col: "is_system", type: "BOOLEAN", constraints: "NOT NULL DEFAULT FALSE", fk: "", index: "IX", description: "TRUE = plantilla del sistema (no editable por tenant)" },
    { col: "created_by", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "", description: "Usuario que creó la plantilla" },
    tsCreated, tsUpdated, isActive,
]);

const tClinicalPhotos = tableBlock("clinical_photos", "clinical", "Fotografías clínicas asociadas al expediente", COLS, [
    pkRow(),
    { col: "medical_record_id", type: "UUID", constraints: "NOT NULL", fk: "medical_records.id", index: "IX", description: "Expediente" },
    { col: "clinical_document_id", type: "UUID", constraints: "NULL", fk: "clinical_documents.id", index: "IX", description: "Documento al que está asociada (NULL = foto suelta)" },
    { col: "file_url", type: "TEXT", constraints: "NOT NULL", fk: "", index: "", description: "URL en object storage (S3/R2)" },
    { col: "file_name", type: "VARCHAR(300)", constraints: "NOT NULL", fk: "", index: "", description: "Nombre original del archivo" },
    { col: "mime_type", type: "VARCHAR(80)", constraints: "NOT NULL", fk: "", index: "", description: "image/jpeg | image/png | application/dicom | etc." },
    { col: "file_size_bytes", type: "BIGINT", constraints: "NOT NULL", fk: "", index: "", description: "Tamaño del archivo en bytes" },
    { col: "capture_date", type: "DATE", constraints: "NULL", fk: "", index: "IX", description: "Fecha de captura de la imagen" },
    { col: "category", type: "VARCHAR(80)", constraints: "NULL", fk: "", index: "IX", description: "preop | postop | xray | ct | lab | clinical | other" },
    { col: "description", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Descripción clínica de la imagen" },
    { col: "uploaded_by", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "", description: "Usuario que subió el archivo" },
    tsCreated, tsDeleted,
]);

const tConciliationItems = tableBlock("conciliation_items", "clinical", "Cantidades reales validadas en la ventana de conciliación post-cita", COLS, [
    pkRow(),
    { col: "appointment_id", type: "UUID", constraints: "NOT NULL", fk: "appointments.id", index: "IX", description: "Cita" },
    { col: "appointment_procedure_id", type: "UUID", constraints: "NOT NULL", fk: "appointment_procedures.id", index: "IX", description: "Procedimiento ejecutado en la cita" },
    { col: "product_id", type: "UUID", constraints: "NOT NULL", fk: "products.id", index: "IX", description: "Insumo consumido" },
    { col: "quantity_budgeted", type: "NUMERIC(10,4)", constraints: "NOT NULL", fk: "", index: "", description: "Snapshot de la cantidad presupuestada al agendar" },
    { col: "quantity_actual", type: "NUMERIC(10,4)", constraints: "NOT NULL", fk: "", index: "", description: "Cantidad real validada por el médico o asistente" },
    { col: "unit", type: "VARCHAR(20)", constraints: "NOT NULL", fk: "", index: "", description: "Unidad de medida del insumo" },
    { col: "unit_cost_snapshot", type: "NUMERIC(12,4)", constraints: "NOT NULL", fk: "", index: "", description: "Costo unitario al momento de la conciliación" },
    { col: "total_cost", type: "NUMERIC(14,4)", constraints: "GENERATED ALWAYS AS (quantity_actual * unit_cost_snapshot) STORED", fk: "", index: "", description: "Costo total calculado de forma inmutable" },
    { col: "notes", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Observaciones de la conciliación" },
    { col: "reconciled_by", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "IX", description: "Usuario que validó la conciliación" },
    { col: "reconciled_at", type: "TIMESTAMPTZ", constraints: "NOT NULL DEFAULT now()", fk: "", index: "", description: "Fecha y hora de la validación" },
    tsCreated,
]);

// ── SCHEMA: scheduling ────────────────────────────────────────────────────

const tAppointments = tableBlock("appointments", "scheduling", "Citas agendadas", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica" },
    { col: "patient_id", type: "UUID", constraints: "NOT NULL", fk: "patients.id", index: "IX", description: "Paciente" },
    { col: "doctor_id", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "IX", description: "Médico asignado" },
    { col: "treatment_plan_id", type: "UUID", constraints: "NULL", fk: "treatment_plans.id", index: "IX", description: "Plan de tratamiento al que pertenece (NULL = consulta nueva)" },
    { col: "scheduled_start", type: "TIMESTAMPTZ", constraints: "NOT NULL", fk: "", index: "IX", description: "Fecha y hora de inicio programada" },
    { col: "scheduled_end", type: "TIMESTAMPTZ", constraints: "NOT NULL", fk: "", index: "", description: "Fecha y hora de fin programada" },
    { col: "actual_start", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "", description: "Hora real de inicio" },
    { col: "actual_end", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "", description: "Hora real de fin" },
    { col: "status", type: "appt_status", constraints: "NOT NULL DEFAULT 'scheduled'", fk: "", index: "IX", description: "ENUM: scheduled | confirmed | in_progress | completed | cancelled | no_show" },
    { col: "cancellation_reason", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Motivo de cancelación" },
    { col: "cancelled_by", type: "UUID", constraints: "NULL", fk: "users.id", index: "", description: "Usuario que canceló" },
    { col: "cancelled_at", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "", description: "Timestamp de cancelación" },
    { col: "room", type: "VARCHAR(60)", constraints: "NULL", fk: "", index: "", description: "Consultorio o sala asignada" },
    { col: "notes", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Notas para la cita" },
    tsCreated, tsUpdated,
]);

const tApptProcedures = tableBlock("appointment_procedures", "scheduling", "Procedimientos asignados a una cita específica", COLS, [
    pkRow(),
    { col: "appointment_id", type: "UUID", constraints: "NOT NULL", fk: "appointments.id", index: "IX", description: "Cita" },
    { col: "treatment_item_id", type: "UUID", constraints: "NOT NULL", fk: "treatment_plan_items.id", index: "IX", description: "Ítem del plan de tratamiento" },
    { col: "status", type: "VARCHAR(30)", constraints: "NOT NULL DEFAULT 'pending'", fk: "", index: "IX", description: "pending | completed | skipped" },
    { col: "notes", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Notas del procedimiento en esta cita" },
    tsCreated, tsUpdated,
]);

// ── SCHEMA: treatments ────────────────────────────────────────────────────

const tTreatmentPlans = tableBlock("treatment_plans", "treatments", "Plan de tratamiento y presupuesto por paciente", COLS, [
    pkRow(),
    { col: "patient_id", type: "UUID", constraints: "NOT NULL", fk: "patients.id", index: "IX", description: "Paciente" },
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica" },
    { col: "created_by", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "IX", description: "Médico que creó el plan" },
    { col: "title", type: "VARCHAR(300)", constraints: "NOT NULL", fk: "", index: "", description: "Nombre del plan (ej: 'Rehabilitación oral completa')" },
    { col: "clinical_notes", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Notas clínicas del plan de tratamiento" },
    { col: "status", type: "VARCHAR(30)", constraints: "NOT NULL DEFAULT 'quoted'", fk: "", index: "IX", description: "quoted | accepted | in_progress | completed | cancelled" },
    { col: "total_quoted", type: "NUMERIC(12,2)", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Total cotizado (suma de ítems)" },
    { col: "total_paid", type: "NUMERIC(12,2)", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Total cobrado hasta el momento" },
    { col: "accepted_at", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "", description: "Fecha en que el paciente aceptó el plan" },
    tsCreated, tsUpdated, tsDeleted,
]);

const tTreatmentItems = tableBlock("treatment_plan_items", "treatments", "Ítems (procedimientos) dentro de un plan", COLS, [
    pkRow(),
    { col: "treatment_plan_id", type: "UUID", constraints: "NOT NULL", fk: "treatment_plans.id", index: "IX", description: "Plan de tratamiento" },
    { col: "procedure_id", type: "UUID", constraints: "NOT NULL", fk: "procedures.id", index: "IX", description: "Procedimiento del catálogo" },
    { col: "description", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Descripción libre del procedimiento en este plan" },
    { col: "tooth_number", type: "VARCHAR(10)", constraints: "NULL", fk: "", index: "", description: "Órgano dental (nomenclatura FDI) — aplica para odontología" },
    { col: "quantity", type: "NUMERIC(8,2)", constraints: "NOT NULL DEFAULT 1", fk: "", index: "", description: "Cantidad de veces que se realizará" },
    { col: "unit_price", type: "NUMERIC(12,2)", constraints: "NOT NULL", fk: "", index: "", description: "Precio unitario cotizado al paciente" },
    { col: "discount_pct", type: "NUMERIC(5,2)", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Descuento en porcentaje" },
    { col: "subtotal", type: "NUMERIC(12,2)", constraints: "NOT NULL", fk: "", index: "", description: "Subtotal = (unit_price × qty) × (1 − discount_pct/100)" },
    { col: "status", type: "VARCHAR(30)", constraints: "NOT NULL DEFAULT 'pending'", fk: "", index: "IX", description: "pending | reserved | in_progress | completed | cancelled" },
    { col: "sort_order", type: "INT", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Orden de visualización dentro del plan" },
    tsCreated, tsUpdated,
]);

const tProcedures = tableBlock("procedures", "treatments", "Catálogo de procedimientos médicos de la clínica", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica dueña del catálogo" },
    { col: "code", type: "VARCHAR(50)", constraints: "NULL", fk: "", index: "IX", description: "Código interno o código CIE-P (procedimientos)" },
    { col: "name", type: "VARCHAR(300)", constraints: "NOT NULL", fk: "", index: "IX", description: "Nombre del procedimiento" },
    { col: "description", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Descripción detallada" },
    { col: "category", type: "VARCHAR(100)", constraints: "NULL", fk: "", index: "IX", description: "Categoría (Extracción, Ortodoncia, Cirugía, etc.)" },
    { col: "default_price", type: "NUMERIC(12,2)", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Precio sugerido de lista" },
    { col: "duration_minutes", type: "INT", constraints: "NULL", fk: "", index: "", description: "Duración estimada en minutos" },
    tsCreated, tsUpdated, isActive,
]);

const tProcedureSupplies = tableBlock("procedure_supplies", "treatments", "Insumos necesarios por procedimiento (lista de materiales)", COLS, [
    pkRow(),
    { col: "procedure_id", type: "UUID", constraints: "NOT NULL", fk: "procedures.id", index: "IX", description: "Procedimiento" },
    { col: "product_id", type: "UUID", constraints: "NOT NULL", fk: "products.id", index: "IX", description: "Producto / insumo del catálogo de inventario" },
    { col: "quantity_required", type: "NUMERIC(10,4)", constraints: "NOT NULL", fk: "", index: "", description: "Cantidad requerida por ejecución del procedimiento (soporta decimales para ml/g)" },
    { col: "unit", type: "VARCHAR(20)", constraints: "NOT NULL", fk: "", index: "", description: "Unidad de medida: pz | ml | g | cm | etc." },
    { col: "notes", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Notas de uso del insumo" },
    tsCreated, tsUpdated,
]);

// ── SCHEMA: inventory ─────────────────────────────────────────────────────

const tProducts = tableBlock("products", "inventory", "Catálogo de productos e insumos", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica dueña del catálogo" },
    { col: "sku", type: "VARCHAR(100)", constraints: "NULL", fk: "", index: "IX", description: "SKU interno o código de barras" },
    { col: "name", type: "VARCHAR(300)", constraints: "NOT NULL", fk: "", index: "IX", description: "Nombre del producto" },
    { col: "description", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Descripción" },
    { col: "category", type: "VARCHAR(100)", constraints: "NULL", fk: "", index: "IX", description: "Categoría: material_dental | medicamento | papelería | equipo | otro" },
    { col: "unit", type: "VARCHAR(20)", constraints: "NOT NULL", fk: "", index: "", description: "Unidad base: pz | caja | frasco | ml | g" },
    { col: "unit_cost", type: "NUMERIC(12,4)", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Costo unitario actual (se actualiza con cada entrada)" },
    { col: "stock_current", type: "NUMERIC(12,4)", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Stock disponible actual" },
    { col: "stock_reserved", type: "NUMERIC(12,4)", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Stock apartado por citas confirmadas" },
    { col: "stock_min", type: "NUMERIC(12,4)", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Stock mínimo — dispara alerta de reposición" },
    { col: "stock_max", type: "NUMERIC(12,4)", constraints: "NULL", fk: "", index: "", description: "Stock máximo — dispara alerta de exceso" },
    { col: "has_expiry", type: "BOOLEAN", constraints: "NOT NULL DEFAULT FALSE", fk: "", index: "", description: "TRUE si el producto tiene fecha de caducidad" },
    { col: "expiry_alert_days", type: "INT", constraints: "NULL", fk: "", index: "", description: "Días antes de caducidad para disparar alerta" },
    { col: "is_controlled", type: "BOOLEAN", constraints: "NOT NULL DEFAULT FALSE", fk: "", index: "", description: "TRUE si es medicamento controlado (requiere receta especial)" },
    tsCreated, tsUpdated, isActive,
]);

const tStockMovements = tableBlock("stock_movements", "inventory", "Kardex — registro de cada movimiento de inventario", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica" },
    { col: "product_id", type: "UUID", constraints: "NOT NULL", fk: "products.id", index: "IX", description: "Producto afectado" },
    { col: "movement_type", type: "stock_mvt_type", constraints: "NOT NULL", fk: "", index: "IX", description: "ENUM: entry | exit | reserve | unreserve | adjustment_up | adjustment_down | expiry_loss" },
    { col: "quantity", type: "NUMERIC(12,4)", constraints: "NOT NULL", fk: "", index: "", description: "Cantidad del movimiento (siempre positivo; el tipo indica dirección)" },
    { col: "unit_cost", type: "NUMERIC(12,4)", constraints: "NULL", fk: "", index: "", description: "Costo unitario en el momento del movimiento" },
    { col: "total_cost", type: "NUMERIC(14,4)", constraints: "NULL", fk: "", index: "", description: "quantity × unit_cost" },
    { col: "stock_after", type: "NUMERIC(12,4)", constraints: "NOT NULL", fk: "", index: "", description: "Stock disponible después del movimiento (snapshot)" },
    { col: "reference_type", type: "VARCHAR(60)", constraints: "NULL", fk: "", index: "IX", description: "appointment | treatment_item | cash_register | manual_adjustment" },
    { col: "reference_id", type: "UUID", constraints: "NULL", fk: "", index: "IX", description: "ID de la entidad de origen del movimiento" },
    { col: "lot_number", type: "VARCHAR(100)", constraints: "NULL", fk: "", index: "IX", description: "Número de lote del proveedor (Fase 2: trazabilidad completa)" },
    { col: "expiry_date", type: "DATE", constraints: "NULL", fk: "", index: "IX", description: "Fecha de caducidad del lote" },
    { col: "notes", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Notas del movimiento" },
    { col: "created_by", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "IX", description: "Usuario que registró el movimiento" },
    tsCreated,
]);

// ── SCHEMA: cash ──────────────────────────────────────────────────────────

const tCashRegisters = tableBlock("cash_registers", "cash", "Cajas registradoras / puntos de venta", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica" },
    { col: "name", type: "VARCHAR(100)", constraints: "NOT NULL", fk: "", index: "", description: "Nombre de la caja (ej: 'Caja Principal')" },
    { col: "is_active", type: "BOOLEAN", constraints: "NOT NULL DEFAULT TRUE", fk: "", index: "", description: "TRUE = habilitada" },
    tsCreated, tsUpdated,
]);

const tCashSessions = tableBlock("cash_sessions", "cash", "Sesiones de caja (apertura y cierre de turno)", COLS, [
    pkRow(),
    { col: "cash_register_id", type: "UUID", constraints: "NOT NULL", fk: "cash_registers.id", index: "IX", description: "Caja" },
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica" },
    { col: "opened_by", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "IX", description: "Usuario que abrió la caja" },
    { col: "closed_by", type: "UUID", constraints: "NULL", fk: "users.id", index: "", description: "Usuario que cerró la caja" },
    { col: "opened_at", type: "TIMESTAMPTZ", constraints: "NOT NULL", fk: "", index: "IX", description: "Hora de apertura" },
    { col: "closed_at", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "IX", description: "Hora de cierre — NULL = sesión activa" },
    { col: "opening_amount", type: "NUMERIC(12,2)", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Fondo inicial en efectivo" },
    { col: "closing_amount_expected", type: "NUMERIC(12,2)", constraints: "NULL", fk: "", index: "", description: "Total esperado al cierre (calculado por sistema)" },
    { col: "closing_amount_actual", type: "NUMERIC(12,2)", constraints: "NULL", fk: "", index: "", description: "Efectivo contado físicamente al cierre" },
    { col: "difference", type: "NUMERIC(12,2)", constraints: "NULL", fk: "", index: "", description: "closing_actual − closing_expected (positivo = sobrante)" },
    { col: "notes", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Notas del cierre de caja" },
    tsCreated, tsUpdated,
]);

const tPayments = tableBlock("payments", "cash", "Cobros realizados a pacientes y ticket de venta", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica" },
    { col: "cash_session_id", type: "UUID", constraints: "NOT NULL", fk: "cash_sessions.id", index: "IX", description: "Sesión de caja activa al momento del cobro" },
    { col: "patient_id", type: "UUID", constraints: "NOT NULL", fk: "patients.id", index: "IX", description: "Paciente" },
    { col: "appointment_id", type: "UUID", constraints: "NULL", fk: "appointments.id", index: "IX", description: "Cita asociada — NULL si es pago libre (abono a tratamiento)" },
    { col: "treatment_plan_id", type: "UUID", constraints: "NULL", fk: "treatment_plans.id", index: "IX", description: "Plan de tratamiento al que se abona" },
    { col: "payment_type", type: "payment_type", constraints: "NOT NULL", fk: "", index: "IX", description: "ENUM: full | partial | advance | refund" },
    { col: "amount", type: "NUMERIC(12,2)", constraints: "NOT NULL", fk: "", index: "", description: "Monto del cobro" },
    { col: "payment_method", type: "pay_method", constraints: "NOT NULL", fk: "", index: "IX", description: "ENUM: cash | card | transfer | check | other" },
    { col: "reference_number", type: "VARCHAR(100)", constraints: "NULL", fk: "", index: "", description: "Número de autorización / referencia bancaria" },
    { col: "ticket_number", type: "VARCHAR(50)", constraints: "NOT NULL", fk: "", index: "UQ", description: "Número de ticket único por clínica; el detalle vive en ticket_lines" },
    { col: "ticket_lines", type: "JSONB", constraints: "NOT NULL DEFAULT '[]'", fk: "", index: "GIN", description: "Detalle del ticket: líneas, subtotales, total y desglose de pago" },
    { col: "notes", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Notas del cobro" },
    { col: "received_by", type: "UUID", constraints: "NOT NULL", fk: "users.id", index: "IX", description: "Usuario que recibió el pago" },
    tsCreated,
]);

// ── SCHEMA: accounting ────────────────────────────────────────────────────

const tChartOfAccounts = tableBlock("chart_of_accounts", "accounting", "Catálogo de cuentas contables por clínica", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica — cada tenant tiene su CoA" },
    { col: "code", type: "VARCHAR(20)", constraints: "NOT NULL", fk: "", index: "IX", description: "Código de cuenta (ej: 11100)" },
    { col: "name", type: "VARCHAR(200)", constraints: "NOT NULL", fk: "", index: "", description: "Nombre de la cuenta" },
    { col: "account_type", type: "acct_type", constraints: "NOT NULL", fk: "", index: "IX", description: "ENUM: asset | liability | income | expense | cost" },
    { col: "parent_code", type: "VARCHAR(20)", constraints: "NULL", fk: "", index: "IX", description: "Código de la cuenta padre — NULL = cuenta raíz" },
    { col: "is_system", type: "BOOLEAN", constraints: "NOT NULL DEFAULT FALSE", fk: "", index: "", description: "TRUE = cuenta base del sistema (no modificable)" },
    { col: "allows_movements", type: "BOOLEAN", constraints: "NOT NULL DEFAULT TRUE", fk: "", index: "", description: "FALSE = cuenta de agrupación, no recibe movimientos directos" },
    tsCreated, tsUpdated, isActive,
    { col: "UNIQUE", type: "—", constraints: "(clinic_id, code)", fk: "", index: "UQ", description: "Código único por clínica" },
]);

const tJournalEntries = tableBlock("journal_entries", "accounting", "Libro Mayor — cabecera de cada póliza contable", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica" },
    { col: "entry_number", type: "VARCHAR(20)", constraints: "NOT NULL", fk: "", index: "UQ", description: "Número de póliza único por clínica (secuencial + año)" },
    { col: "entry_type", type: "entry_type", constraints: "NOT NULL", fk: "", index: "IX", description: "ENUM: income | expense | cost | adjustment | reversal" },
    { col: "entry_date", type: "DATE", constraints: "NOT NULL", fk: "", index: "IX", description: "Fecha contable de la póliza" },
    { col: "description", type: "TEXT", constraints: "NOT NULL", fk: "", index: "", description: "Descripción en lenguaje natural" },
    { col: "domain_event_type", type: "VARCHAR(100)", constraints: "NOT NULL", fk: "", index: "IX", description: "Evento de dominio origen: PaymentRegistered | ConsumptionConciliated | AppointmentCancelled | StockLoss. JournalBalanceError nunca produce una journal_entry; el intento fallido queda solo en domain_events con processed_at NULL." },
    { col: "domain_event_id", type: "UUID", constraints: "NOT NULL", fk: "", index: "IX", description: "ID del evento de dominio que originó esta póliza" },
    { col: "reversed_by_id", type: "UUID", constraints: "NULL", fk: "journal_entries.id", index: "IX", description: "Póliza de reverso — NULL si no ha sido revertida" },
    { col: "reversal_of_id", type: "UUID", constraints: "NULL", fk: "journal_entries.id", index: "IX", description: "Póliza original que esta revierte — NULL si no es reverso" },
    { col: "is_balanced", type: "BOOLEAN", constraints: "NOT NULL", fk: "", index: "IX", description: "TRUE si Σ débitos = Σ créditos; la validación final corre en trigger DEFERRABLE" },
    { col: "checksum", type: "TEXT", constraints: "NOT NULL", fk: "", index: "", description: "SHA-256 del contenido de las líneas — para auditoría de integridad" },
    { col: "created_by_are", type: "BOOLEAN", constraints: "NOT NULL DEFAULT TRUE", fk: "", index: "", description: "TRUE = generada automáticamente por el ARE; FALSE = entrada manual del contador" },
    tsCreated,
]);

const tJournalLines = tableBlock("journal_lines", "accounting", "Líneas de asiento (débito / crédito) de cada póliza", COLS, [
    pkRow(),
    { col: "journal_entry_id", type: "UUID", constraints: "NOT NULL", fk: "journal_entries.id", index: "IX", description: "Póliza a la que pertenece la línea" },
    { col: "account_code", type: "VARCHAR(20)", constraints: "NOT NULL", fk: "chart_of_accounts.code", index: "IX", description: "Código de cuenta del CoA" },
    { col: "debit", type: "NUMERIC(14,2)", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Monto débito — 0 si es crédito" },
    { col: "credit", type: "NUMERIC(14,2)", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Monto crédito — 0 si es débito" },
    { col: "description", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Descripción de la línea" },
    { col: "reference_type", type: "VARCHAR(60)", constraints: "NULL", fk: "", index: "", description: "payment | stock_movement | payroll_item" },
    { col: "reference_id", type: "UUID", constraints: "NULL", fk: "", index: "IX", description: "ID de la entidad de referencia" },
    { col: "sort_order", type: "INT", constraints: "NOT NULL DEFAULT 0", fk: "", index: "", description: "Orden de las líneas dentro de la póliza" },
    tsCreated,
]);

const tDomainEvents = tableBlock("domain_events", "accounting", "Log inmutable de todos los eventos de dominio publicados", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NOT NULL", fk: "clinics.id", index: "IX", description: "Clínica origen del evento" },
    { col: "event_type", type: "VARCHAR(100)", constraints: "NOT NULL", fk: "", index: "IX", description: "CitaConfirmada | PagoRegistrado | ConsumoConciliado | CitaCancelada | InsumoAgregado | MermaCaducidad | JournalBalanceError" },
    { col: "aggregate_type", type: "VARCHAR(80)", constraints: "NOT NULL", fk: "", index: "IX", description: "Tipo de entidad origen: Appointment | Payment | StockMovement | JournalEntry" },
    { col: "aggregate_id", type: "UUID", constraints: "NOT NULL", fk: "", index: "IX", description: "ID de la entidad que generó el evento" },
    { col: "payload", type: "JSONB", constraints: "NOT NULL", fk: "", index: "GIN", description: "Snapshot completo del estado al momento del evento" },
    { col: "processed_at", type: "TIMESTAMPTZ", constraints: "NULL", fk: "", index: "IX", description: "Cuando el ARE procesó el evento — NULL = pendiente" },
    { col: "processing_error", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "Error del ARE si el procesamiento falló" },
    tsCreated,
]);

// ── SCHEMA: audit ─────────────────────────────────────────────────────────

const tAuditLog = tableBlock("audit_log", "audit", "Log de auditoría de acciones del sistema", COLS, [
    pkRow(),
    { col: "clinic_id", type: "UUID", constraints: "NULL", fk: "clinics.id", index: "IX", description: "Clínica afectada — NULL si es acción global" },
    { col: "user_id", type: "UUID", constraints: "NULL", fk: "users.id", index: "IX", description: "Usuario que realizó la acción — NULL si es sistema" },
    { col: "action", type: "VARCHAR(100)", constraints: "NOT NULL", fk: "", index: "IX", description: "Acción: CREATE | UPDATE | DELETE | LOGIN | LOGOUT | EXPORT | SIGN_DOCUMENT | etc." },
    { col: "entity_type", type: "VARCHAR(80)", constraints: "NOT NULL", fk: "", index: "IX", description: "Entidad afectada: Patient | Appointment | Payment | JournalEntry | etc." },
    { col: "entity_id", type: "UUID", constraints: "NULL", fk: "", index: "IX", description: "ID de la entidad afectada" },
    { col: "old_values", type: "JSONB", constraints: "NULL", fk: "", index: "", description: "Estado anterior (para UPDATE y DELETE)" },
    { col: "new_values", type: "JSONB", constraints: "NULL", fk: "", index: "", description: "Estado nuevo (para CREATE y UPDATE)" },
    { col: "ip_address", type: "INET", constraints: "NULL", fk: "", index: "", description: "IP de origen de la acción" },
    { col: "user_agent", type: "TEXT", constraints: "NULL", fk: "", index: "", description: "User-agent del cliente" },
    tsCreated,
]);

// ═══════════════════════════════════════════════════════════════════════════
// DOCUMENT ASSEMBLY
// ═══════════════════════════════════════════════════════════════════════════

const children = [
    // PORTADA
    sp(600),
    new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { before: 0, after: 80 },
        children: [new TextRun({ text: "MODELO DE DATOS — FASE 1 MVP", font: "Arial", size: 40, bold: true, color: C.brand })]
    }),
    new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { before: 0, after: 80 },
        children: [new TextRun({ text: "Sistema de Gestión de Consultorios Médicos", font: "Arial", size: 52, bold: true, color: C.brand })]
    }),
    new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { before: 80, after: 80 },
        children: [new TextRun({ text: "Especificación técnica de tablas, columnas, tipos de dato, constraints, índices y relaciones", font: "Arial", size: 24, italics: true, color: C.gray })]
    }),
    sp(80),
    new Paragraph({
        alignment: AlignmentType.CENTER, spacing: { before: 0, after: 0 },
        children: [new TextRun({ text: "Versión 1.0  ·  Junio 2025  ·  Confidencial  ·  Base de datos: PostgreSQL 16", font: "Arial", size: 20, color: C.gray })]
    }),
    sp(500),
    pb(),

    // TOC
    h1("Tabla de Contenido"),
    new TableOfContents("Tabla de Contenido", { hyperlink: true, headingStyleRange: "1-3" }),
    pb(),

    // 1. INTRO
    h1("1. Introducción y Convenciones"),
    p("Este documento especifica el modelo de datos completo para la Fase 1 MVP del sistema. Todas las tablas utilizan PostgreSQL 16 como motor de base de datos. El esquema se organiza en los siguientes namespaces:"),
    sp(60),
    new Table({
        width: { size: 9360, type: WidthType.DXA }, columnWidths: [1800, 2400, 5160],
        rows: [
            new TableRow({
                children: [
                    cell("Schema", C.headerFill, 1800, true, C.white, 18),
                    cell("Dominio", C.headerFill, 2400, true, C.white, 18),
                    cell("Tablas principales", C.headerFill, 5160, true, C.white, 18),
                ]
            }),
            new TableRow({ children: [cell("core", C.pkFill, 1800), cell("Usuarios, clínicas, personal y alertas", C.pkFill, 2400), cell("users, organizations, clinics, clinic_staff, attendance, system_alerts", C.pkFill, 5160)] }),
            new TableRow({ children: [cell("patients", C.white, 1800), cell("Pacientes", C.white, 2400), cell("patients", C.white, 5160)] }),
            new TableRow({ children: [cell("clinical", C.rowAlt, 1800), cell("Expediente, firmas y conciliación", C.rowAlt, 2400), cell("medical_records, clinical_documents, document_signatures, clinical_histories, doc_templates, clinical_photos, conciliation_items", C.rowAlt, 5160)] }),
            new TableRow({ children: [cell("scheduling", C.white, 1800), cell("Agenda y citas", C.white, 2400), cell("appointments, appointment_procedures", C.white, 5160)] }),
            new TableRow({ children: [cell("treatments", C.rowAlt, 1800), cell("Tratamientos e insumos", C.rowAlt, 2400), cell("treatment_plans, treatment_plan_items, procedures, procedure_supplies", C.rowAlt, 5160)] }),
            new TableRow({ children: [cell("inventory", C.white, 1800), cell("Inventario", C.white, 2400), cell("products, stock_movements", C.white, 5160)] }),
            new TableRow({ children: [cell("cash", C.rowAlt, 1800), cell("Caja y cobros", C.rowAlt, 2400), cell("cash_registers, cash_sessions, payments", C.rowAlt, 5160)] }),
            new TableRow({ children: [cell("accounting", C.white, 1800), cell("Contabilidad (ARE)", C.white, 2400), cell("chart_of_accounts, journal_entries, journal_lines, domain_events", C.white, 5160)] }),
            new TableRow({ children: [cell("audit", C.rowAlt, 1800), cell("Auditoría", C.rowAlt, 2400), cell("audit_log", C.rowAlt, 5160)] }),
        ]
    }),
    sp(160),

    h2("1.1 Convenciones de columnas estándar"),
    p("Todas las tablas incluyen las siguientes columnas de control, salvo que se indique explícitamente:"),
    sp(60),
    new Table({
        width: { size: 9360, type: WidthType.DXA }, columnWidths: [2000, 2000, 5360],
        rows: [
            new TableRow({ children: [cell("Columna", C.headerFill, 2000, true, C.white, 18), cell("Tipo", C.headerFill, 2000, true, C.white, 18), cell("Descripción", C.headerFill, 5360, true, C.white, 18)] }),
            new TableRow({ children: [cell("id", C.pkFill, 2000, true, C.accent), cell("UUID", C.pkFill, 2000), cell("PK — generado con gen_random_uuid()", C.pkFill, 5360)] }),
            new TableRow({ children: [cell("created_at", C.white, 2000), cell("TIMESTAMPTZ", C.white, 2000), cell("NOT NULL DEFAULT now() — fecha de creación en UTC", C.white, 5360)] }),
            new TableRow({ children: [cell("updated_at", C.rowAlt, 2000), cell("TIMESTAMPTZ", C.rowAlt, 2000), cell("NOT NULL — actualizado automáticamente por trigger", C.rowAlt, 5360)] }),
            new TableRow({ children: [cell("deleted_at", C.white, 2000), cell("TIMESTAMPTZ", C.white, 2000), cell("NULL = registro activo; valor = soft delete", C.white, 5360)] }),
        ]
    }),
    sp(160),

    h2("1.2 Leyenda de colores"),
    legend(),
    sp(160),

    h2("1.3 Tipos ENUM personalizados"),
    p("Los siguientes tipos ENUM se crean a nivel de base de datos:"),
    sp(60),
    new Table({
        width: { size: 9360, type: WidthType.DXA }, columnWidths: [2200, 7160],
        rows: [
            new TableRow({ children: [cell("ENUM", C.headerFill, 2200, true, C.white, 18), cell("Valores", C.headerFill, 7160, true, C.white, 18)] }),
            new TableRow({ children: [cell("staff_role", C.white, 2200), cell("admin | doctor | receptionist | assistant | accountant | cleaning | clinic_admin", C.white, 7160)] }),
            new TableRow({ children: [cell("sex_type", C.rowAlt, 2200), cell("male | female | other", C.rowAlt, 7160)] }),
            new TableRow({ children: [cell("doc_type", C.white, 2200), cell("clinical_history | medical_note | lab_study | imaging | informed_consent | prescription | interconsult | referral | counter_referral | nursing_sheet | budget | clinical_photo | administrative", C.white, 7160)] }),
            new TableRow({ children: [cell("appt_status", C.rowAlt, 2200), cell("scheduled | confirmed | in_progress | completed | cancelled | no_show", C.rowAlt, 7160)] }),
            new TableRow({ children: [cell("payment_type", C.white, 2200), cell("full | partial | advance | refund", C.white, 7160)] }),
            new TableRow({ children: [cell("pay_method", C.rowAlt, 2200), cell("cash | card | transfer | check | other", C.rowAlt, 7160)] }),
            new TableRow({ children: [cell("stock_mvt_type", C.white, 2200), cell("entry | exit | reserve | unreserve | adjustment_up | adjustment_down | expiry_loss", C.white, 7160)] }),
            new TableRow({ children: [cell("acct_type", C.rowAlt, 2200), cell("asset | liability | income | expense | cost", C.rowAlt, 7160)] }),
            new TableRow({ children: [cell("entry_type", C.white, 2200), cell("income | expense | cost | adjustment | reversal", C.white, 7160)] }),
        ]
    }),
    sp(200),
    pb(),

    // SCHEMA CORE
    sectionBanner("SCHEMA: core — Usuarios, Clínicas, Personal y Alertas", C.brand),
    sp(80),
    ...tUsers, sp(160),
    ...tOrganizations, sp(160),
    ...tClinics, sp(160),
    ...tClinicStaff, sp(160),
    ...tAttendance, sp(160),
    ...tSystemAlerts, sp(160),
    pb(),

    // SCHEMA PATIENTS
    sectionBanner("SCHEMA: patients — Catálogo de Pacientes", "1A5276"),
    sp(80),
    ...tPatients, sp(160),
    pb(),

    // SCHEMA CLINICAL
    sectionBanner("SCHEMA: clinical — Expediente, Firmas y Conciliación Clínica", "1A6B3A"),
    sp(80),
    ...tMedicalRecords, sp(160),
    ...tClinicalDocs, sp(160),
    ...tDocumentSignatures, sp(160),
    ...tClinicalHistory, sp(160),
    ...tDocTemplates, sp(160),
    ...tClinicalPhotos, sp(160),
    ...tConciliationItems, sp(160),
    pb(),

    // SCHEMA SCHEDULING
    sectionBanner("SCHEMA: scheduling — Agenda y Citas", "6C3483"),
    sp(80),
    ...tAppointments, sp(160),
    ...tApptProcedures, sp(160),
    pb(),

    // SCHEMA TREATMENTS
    sectionBanner("SCHEMA: treatments — Tratamientos, Procedimientos e Insumos", "0E6655"),
    sp(80),
    ...tTreatmentPlans, sp(160),
    ...tTreatmentItems, sp(160),
    ...tProcedures, sp(160),
    ...tProcedureSupplies, sp(160),
    pb(),

    // SCHEMA INVENTORY
    sectionBanner("SCHEMA: inventory — Inventario y Kardex", "784212"),
    sp(80),
    ...tProducts, sp(160),
    ...tStockMovements, sp(160),
    pb(),

    // SCHEMA CASH
    sectionBanner("SCHEMA: cash — Caja y Cobros", "512E5F"),
    sp(80),
    ...tCashRegisters, sp(160),
    ...tCashSessions, sp(160),
    ...tPayments, sp(160),
    pb(),

    // SCHEMA ACCOUNTING
    sectionBanner("SCHEMA: accounting — Motor Contable (ARE) y Libro Mayor", "1A3A5C"),
    sp(80),
    ...tChartOfAccounts, sp(160),
    ...tJournalEntries, sp(160),
    ...tJournalLines, sp(160),
    ...tDomainEvents, sp(160),
    pb(),

    // SCHEMA AUDIT
    sectionBanner("SCHEMA: audit — Log de Auditoría", "424242"),
    sp(80),
    ...tAuditLog, sp(160),
    pb(),

    // RELACIONES
    h1("3. Mapa de Relaciones entre Schemas"),
    sp(80),
    note("Las relaciones entre schemas siguen el principio de dependencia descendente: ningún schema operativo importa desde accounting ni audit. El flujo de datos siempre va hacia abajo: operación clínica → eventos de dominio → ARE → Libro Mayor.", C.brandLight),
    sp(160),
    new Table({
        width: { size: 9360, type: WidthType.DXA }, columnWidths: [2400, 1400, 2000, 3560],
        rows: [
            new TableRow({
                children: [
                    cell("Tabla origen", C.headerFill, 2400, true, C.white, 18),
                    cell("Cardinalidad", C.headerFill, 1400, true, C.white, 18),
                    cell("Tabla destino", C.headerFill, 2000, true, C.white, 18),
                    cell("Descripción", C.headerFill, 3560, true, C.white, 18),
                ]
            }),
            new TableRow({ children: [cell("users", C.pkFill, 2400, true, C.accent), cell("1 : N", C.pkFill, 1400), cell("clinic_staff", C.pkFill, 2000), cell("Un usuario puede ser staff en varias clínicas", C.pkFill, 3560)] }),
            new TableRow({ children: [cell("clinics", C.white, 2400, true, C.accent), cell("1 : N", C.white, 1400), cell("clinic_staff", C.white, 2000), cell("Una clínica tiene múltiples empleados", C.white, 3560)] }),
            new TableRow({ children: [cell("clinics", C.rowAlt, 2400, true, C.accent), cell("1 : N", C.rowAlt, 1400), cell("patients", C.rowAlt, 2000), cell("Una clínica gestiona muchos pacientes", C.rowAlt, 3560)] }),
            new TableRow({ children: [cell("patients", C.white, 2400, true, C.accent), cell("1 : 1", C.white, 1400), cell("medical_records", C.white, 2000), cell("Un expediente por paciente", C.white, 3560)] }),
            new TableRow({ children: [cell("medical_records", C.rowAlt, 2400, true, C.accent), cell("1 : N", C.rowAlt, 1400), cell("clinical_documents", C.rowAlt, 2000), cell("Un expediente contiene múltiples documentos", C.rowAlt, 3560)] }),
            new TableRow({ children: [cell("clinical_documents", C.white, 2400, true, C.accent), cell("1 : 1", C.white, 1400), cell("clinical_histories", C.white, 2000), cell("Una historia clínica por documento", C.white, 3560)] }),
            new TableRow({ children: [cell("clinical_documents", C.rowAlt, 2400, true, C.accent), cell("1 : N", C.rowAlt, 1400), cell("document_signatures", C.rowAlt, 2000), cell("Un documento puede tener múltiples firmas", C.rowAlt, 3560)] }),
            new TableRow({ children: [cell("patients", C.rowAlt, 2400, true, C.accent), cell("1 : N", C.rowAlt, 1400), cell("appointments", C.rowAlt, 2000), cell("Un paciente puede tener múltiples citas", C.rowAlt, 3560)] }),
            new TableRow({ children: [cell("patients", C.white, 2400, true, C.accent), cell("1 : N", C.white, 1400), cell("treatment_plans", C.white, 2000), cell("Un paciente puede tener múltiples planes", C.white, 3560)] }),
            new TableRow({ children: [cell("treatment_plans", C.rowAlt, 2400, true, C.accent), cell("1 : N", C.rowAlt, 1400), cell("treatment_plan_items", C.rowAlt, 2000), cell("Un plan tiene múltiples procedimientos", C.rowAlt, 3560)] }),
            new TableRow({ children: [cell("appointments", C.white, 2400, true, C.accent), cell("1 : N", C.white, 1400), cell("appt_procedures", C.white, 2000), cell("Una cita ejecuta varios ítems del plan", C.white, 3560)] }),
            new TableRow({ children: [cell("appointments", C.rowAlt, 2400, true, C.accent), cell("1 : N", C.rowAlt, 1400), cell("conciliation_items", C.rowAlt, 2000), cell("Una cita puede registrar múltiples conciliaciones de insumos", C.rowAlt, 3560)] }),
            new TableRow({ children: [cell("appointment_procedures", C.white, 2400, true, C.accent), cell("1 : N", C.white, 1400), cell("conciliation_items", C.white, 2000), cell("Cada procedimiento ejecutado puede detallar sus insumos reales", C.white, 3560)] }),
            new TableRow({ children: [cell("procedures", C.rowAlt, 2400, true, C.accent), cell("1 : N", C.rowAlt, 1400), cell("procedure_supplies", C.rowAlt, 2000), cell("Un procedimiento requiere N insumos", C.rowAlt, 3560)] }),
            new TableRow({ children: [cell("products", C.white, 2400, true, C.accent), cell("1 : N", C.white, 1400), cell("stock_movements", C.white, 2000), cell("Un producto tiene historial completo de movimientos", C.white, 3560)] }),
            new TableRow({ children: [cell("products", C.rowAlt, 2400, true, C.accent), cell("1 : N", C.rowAlt, 1400), cell("conciliation_items", C.rowAlt, 2000), cell("Un producto puede aparecer en múltiples conciliaciones", C.rowAlt, 3560)] }),
            new TableRow({ children: [cell("cash_sessions", C.rowAlt, 2400, true, C.accent), cell("1 : N", C.rowAlt, 1400), cell("payments", C.rowAlt, 2000), cell("Una sesión de caja registra múltiples cobros", C.rowAlt, 3560)] }),
            new TableRow({ children: [cell("clinics", C.white, 2400, true, C.accent), cell("1 : N", C.white, 1400), cell("system_alerts", C.white, 2000), cell("Cada clínica puede generar y resolver múltiples alertas", C.white, 3560)] }),
            new TableRow({ children: [cell("payments → ARE", C.fkFill, 2400, false, C.orange), cell("1 : 1", C.fkFill, 1400), cell("journal_entries", C.fkFill, 2000), cell("Cada pago genera una póliza de ingreso", C.fkFill, 3560)] }),
            new TableRow({ children: [cell("stock_movements → ARE", C.fkFill, 2400, false, C.orange), cell("1 : 1", C.fkFill, 1400), cell("journal_entries", C.fkFill, 2000), cell("Cada consumo/merma genera póliza de costo", C.fkFill, 3560)] }),
            new TableRow({ children: [cell("journal_entries", C.white, 2400, true, C.accent), cell("1 : N", C.white, 1400), cell("journal_lines", C.white, 2000), cell("Una póliza tiene 2 o más líneas de asiento", C.white, 3560)] }),
        ]
    }),
    sp(200),

    // REGLAS DE NEGOCIO
    h1("4. Reglas de Integridad y Negocio"),
    sp(80),
    h2("4.1 Multi-tenancy y Row Level Security (RLS)"),
    p("Todas las tablas operativas incluyen clinic_id. Se implementa RLS en PostgreSQL: cada sesión de base de datos inyecta el clinic_id del tenant activo en el contexto, y las políticas de RLS filtran automáticamente todas las queries. Un usuario no puede acceder a datos de otra clínica aunque conozca el ID."),
    sp(120),

    h2("4.2 Soft delete"),
    p("Las entidades principales (patients, clinical_documents, treatment_plans, products) utilizan soft delete mediante deleted_at. Los registros con deleted_at IS NOT NULL no aparecen en las vistas normales pero se conservan para auditoría y trazabilidad contable. Las pólizas contables (journal_entries, journal_lines) y el audit_log son completamente inmutables: no tienen deleted_at ni updated_at."),
    sp(120),

    h2("4.3 Inmutabilidad del Libro Mayor"),
    note("Las tablas journal_entries y journal_lines NO tienen columnas updated_at ni deleted_at. Son append-only por diseño. Cualquier corrección genera una nueva póliza de reverso referenciada a la original. Se implementa una restricción a nivel de trigger que impide UPDATE y DELETE sobre estas tablas.", "FEE2E2"),
    sp(120),

    h2("4.4 Validación de partida doble"),
    p("Antes de insertar cualquier conjunto de journal_lines, un trigger CONSTRAINT DEFERRABLE INITIALLY DEFERRED valida en PostgreSQL que SUM(debit) = SUM(credit) al momento del COMMIT. Si la condición falla, la transacción completa hace ROLLBACK y el ARE registra el intento fallido en domain_events con event_type JournalBalanceError, processed_at NULL y processing_error."),
    sp(120),

    h2("4.5 Atomicidad en reserva y liberación de stock"),
    p("La transición de stock_current → stock_reserved (al confirmar una cita) y su reverso (al cancelar) se realizan dentro de una sola transacción de base de datos junto con la inserción del stock_movement correspondiente y la publicación del evento de dominio. No existe estado intermedio visible."),
    sp(120),

    h2("4.6 Índices GIN en JSONB"),
    p("Las columnas content_json (clinical_documents), ticket_lines (cash.payments), metadata (core.system_alerts), payload (domain_events), family_hx, nonpath_personal_hx y path_personal_hx (clinical_histories) tienen índices GIN para permitir búsquedas eficientes dentro del contenido JSON sin descomponer la estructura flexible."),
    sp(160),

    // INDICES
    h1("5. Índices Adicionales Recomendados"),
    sp(80),
    new Table({
        width: { size: 9360, type: WidthType.DXA }, columnWidths: [2800, 3200, 3360],
        rows: [
            new TableRow({ children: [cell("Tabla", C.headerFill, 2800, true, C.white, 18), cell("Índice", C.headerFill, 3200, true, C.white, 18), cell("Justificación", C.headerFill, 3360, true, C.white, 18)] }),
            new TableRow({ children: [cell("patients", C.white, 2800), cell("(clinic_id, last_name_paternal, first_name)", C.white, 3200), cell("Búsqueda por nombre en el catálogo de pacientes", C.white, 3360)] }),
            new TableRow({ children: [cell("appointments", C.rowAlt, 2800), cell("(clinic_id, doctor_id, scheduled_start)", C.rowAlt, 3200), cell("Vista de agenda por médico y día", C.rowAlt, 3360)] }),
            new TableRow({ children: [cell("appointments", C.white, 2800), cell("(clinic_id, status, scheduled_start)", C.white, 3200), cell("Filtro de citas activas del día", C.white, 3360)] }),
            new TableRow({ children: [cell("stock_movements", C.rowAlt, 2800), cell("(clinic_id, product_id, created_at DESC)", C.rowAlt, 3200), cell("Kardex cronológico por producto", C.rowAlt, 3360)] }),
            new TableRow({ children: [cell("journal_entries", C.white, 2800), cell("(clinic_id, entry_date, entry_type)", C.white, 3200), cell("Reportes contables por periodo", C.white, 3360)] }),
            new TableRow({ children: [cell("journal_lines", C.rowAlt, 2800), cell("(account_code, journal_entry_id)", C.rowAlt, 3200), cell("Auxiliar de cuenta contable", C.rowAlt, 3360)] }),
            new TableRow({ children: [cell("domain_events", C.white, 2800), cell("(clinic_id, event_type, processed_at)", C.white, 3200), cell("Cola del ARE — eventos pendientes de procesar", C.white, 3360)] }),
            new TableRow({ children: [cell("clinical_documents", C.rowAlt, 2800), cell("(medical_record_id, doc_type, created_at DESC)", C.rowAlt, 3200), cell("Listado de documentos por tipo en el expediente", C.rowAlt, 3360)] }),
            new TableRow({ children: [cell("document_signatures", C.white, 2800), cell("(clinical_document_id, signed_at DESC)", C.white, 3200), cell("Trazabilidad de firmas por documento", C.white, 3360)] }),
            new TableRow({ children: [cell("conciliation_items", C.rowAlt, 2800), cell("(appointment_id, appointment_procedure_id)", C.rowAlt, 3200), cell("Conciliación rápida por cita y procedimiento", C.rowAlt, 3360)] }),
            new TableRow({ children: [cell("payments", C.white, 2800), cell("GIN(ticket_lines)", C.white, 3200), cell("Búsqueda por conceptos y reimpresión del ticket", C.white, 3360)] }),
            new TableRow({ children: [cell("products", C.white, 2800), cell("(clinic_id, stock_current) WHERE stock_current <= stock_min", C.white, 3200), cell("Partial index para alertas de stock bajo", C.white, 3360)] }),
            new TableRow({ children: [cell("system_alerts", C.rowAlt, 2800), cell("(clinic_id, status, created_at DESC)", C.rowAlt, 3200), cell("Bandeja operativa de alertas pendientes", C.rowAlt, 3360)] }),
            new TableRow({ children: [cell("system_alerts", C.white, 2800), cell("(clinic_id, alert_type, entity_id)", C.white, 3200), cell("Evita alertas duplicadas por entidad", C.white, 3360)] }),
            new TableRow({ children: [cell("system_alerts", C.rowAlt, 2800), cell("(entity_id)", C.rowAlt, 3200), cell("Consulta directa por entidad afectada", C.rowAlt, 3360)] }),
        ]
    }),
    sp(200),

    // RESUMEN
    h1("6. Resumen del Modelo"),
    sp(80),
    new Table({
        width: { size: 9360, type: WidthType.DXA }, columnWidths: [2200, 1400, 1400, 4360],
        rows: [
            new TableRow({ children: [cell("Schema", C.headerFill, 2200, true, C.white, 18), cell("Tablas", C.headerFill, 1400, true, C.white, 18), cell("Aprox. cols", C.headerFill, 1400, true, C.white, 18), cell("Nota clave", C.headerFill, 4360, true, C.white, 18)] }),
            new TableRow({ children: [cell("core", C.pkFill, 2200), cell("6", C.pkFill, 1400), cell("~77", C.pkFill, 1400), cell("Multi-tenancy, RBAC, asistencia, alertas", C.pkFill, 4360)] }),
            new TableRow({ children: [cell("patients", C.white, 2200), cell("1", C.white, 1400), cell("~28", C.white, 1400), cell("Ficha completa NOM-004, soft delete", C.white, 4360)] }),
            new TableRow({ children: [cell("clinical", C.rowAlt, 2200), cell("7", C.rowAlt, 1400), cell("~82", C.rowAlt, 1400), cell("Canvas JSON, firma digital, conciliación, fotos", C.rowAlt, 4360)] }),
            new TableRow({ children: [cell("scheduling", C.white, 2200), cell("2", C.white, 1400), cell("~25", C.white, 1400), cell("Citas con estado y procedimientos asignados", C.white, 4360)] }),
            new TableRow({ children: [cell("treatments", C.rowAlt, 2200), cell("4", C.rowAlt, 1400), cell("~35", C.rowAlt, 1400), cell("Planes, ítems, catálogo y lista de materiales", C.rowAlt, 4360)] }),
            new TableRow({ children: [cell("inventory", C.white, 2200), cell("2", C.white, 1400), cell("~30", C.white, 1400), cell("Kardex con snapshot de stock, soporte lotes", C.white, 4360)] }),
            new TableRow({ children: [cell("cash", C.rowAlt, 2200), cell("3", C.rowAlt, 1400), cell("~31", C.rowAlt, 1400), cell("Sesiones de caja, cobros, tickets JSONB", C.rowAlt, 4360)] }),
            new TableRow({ children: [cell("accounting", C.fkFill, 2200), cell("4", C.fkFill, 1400), cell("~40", C.fkFill, 1400), cell("Libro Mayor inmutable, ARE, eventos de dominio", C.fkFill, 4360)] }),
            new TableRow({ children: [cell("audit", C.ixFill, 2200), cell("1", C.ixFill, 1400), cell("~12", C.ixFill, 1400), cell("Log append-only de todas las acciones", C.ixFill, 4360)] }),
            new TableRow({
                children: [
                    cell("TOTAL", C.headerFill, 2200, true, C.white, 18),
                    cell("30", C.headerFill, 1400, true, C.white, 18),
                    cell("~360", C.headerFill, 1400, true, C.white, 18),
                    cell("Fase 1 MVP completo", C.headerFill, 4360, true, C.white, 18),
                ]
            }),
        ]
    }),
    sp(200),
    note("Las tablas marcadas para Fase 2+ (valuación por lote, nómina, CxP, órdenes de compra) se integran a este modelo extendiendo los schemas existentes sin romper la estructura base definida aquí.", C.brandLight),
];

// ── BUILD ──────────────────────────────────────────────────────────────────

const doc = new Document({
    numbering: {
        config: [
            {
                reference: "bullets", levels: [
                    {
                        level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT,
                        style: { paragraph: { indent: { left: 480, hanging: 240 } } }
                    },
                ]
            },
        ]
    },
    styles: {
        default: { document: { run: { font: "Arial", size: 19 } } },
        paragraphStyles: [
            {
                id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
                run: { size: 32, bold: true, font: "Arial", color: C.brand },
                paragraph: { spacing: { before: 360, after: 180 }, outlineLevel: 0 }
            },
            {
                id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
                run: { size: 26, bold: true, font: "Arial", color: C.brandMid },
                paragraph: { spacing: { before: 280, after: 140 }, outlineLevel: 1 }
            },
            {
                id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
                run: { size: 22, bold: true, font: "Arial", color: "2C3E50" },
                paragraph: { spacing: { before: 200, after: 100 }, outlineLevel: 2 }
            },
        ]
    },
    sections: [{
        properties: {
            page: {
                size: { width: 12240, height: 15840 },
                margin: { top: 900, right: 900, bottom: 900, left: 900 }
            }
        },
        headers: {
            default: new Header({
                children: [
                    new Paragraph({
                        border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: C.brandMid, space: 4 } },
                        spacing: { before: 0, after: 100 },
                        children: [
                            new TextRun({ text: "Modelo de Datos — Fase 1 MVP  ·  Sistema de Gestión de Consultorios Médicos", font: "Arial", size: 16, color: C.gray }),
                        ]
                    })
                ]
            })
        },
        footers: {
            default: new Footer({
                children: [
                    new Paragraph({
                        border: { top: { style: BorderStyle.SINGLE, size: 4, color: C.brandMid, space: 4 } },
                        spacing: { before: 80, after: 0 },
                        alignment: AlignmentType.RIGHT,
                        children: [
                            new TextRun({ text: "Confidencial  ·  Pág. ", font: "Arial", size: 16, color: C.gray }),
                            new TextRun({ children: [PageNumber.CURRENT], font: "Arial", size: 16, color: C.gray }),
                        ]
                    })
                ]
            })
        },
        children
    }]
});

Packer.toBuffer(doc).then(buf => {
    fs.writeFileSync("ModeloDatos_Fase1_MVP_modificado.docx", buf);
    console.log("Done");
});
