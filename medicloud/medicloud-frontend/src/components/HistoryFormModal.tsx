import { useEffect, useRef, useState } from "react";
import { IconDeviceFloppy, IconFileTypePdf, IconX } from "@tabler/icons-react";
import { CANVAS_WIDTH, NOM_SECTIONS } from "../constants/nomHistoryTemplate";
import { exportPagesToPdf, type PdfExportRange } from "../lib/pdfExport";
import { ApiClientError, clinicsApi, getFriendlyError, medicalHistoryApi } from "../lib/api";
import { bloodTypeLabel, genderLabel, maritalStatusLabel } from "../constants/patientOptions";
import { getAge } from "../utils/getAge";
import type { Address, PatientResponse } from "../types/patient";
import type { ClinicResponse } from "../types/clinic";
import { ClinicHeaderPreview } from "./ClinicHeaderPreview";
import { SignaturePad } from "./SignaturePad";
import {
  DEFAULT_FONT_FAMILY,
  DEFAULT_FONT_SIZE,
  DEFAULT_TEXT_ALIGN,
  fontCssStack,
  parseAnswers,
  parseSchema,
  serializeAnswers,
  type HistoryTemplateResponse,
  type TemplateElement
} from "../types/clinicalRecords";
import { ConfirmDiscardModal } from "./ConfirmDiscardModal";
import { ExportPdfModal } from "./ExportPdfModal";
import { FileFieldEditor } from "./FileFieldEditor";
import { OdontogramField } from "./OdontogramField";
import { TableFieldEditor } from "./TableFieldEditor";
import { formatClinicAddress } from "./ClinicHeaderPreview";

function groupElements(elements: TemplateElement[], enforceBands: boolean) {
  const byPosition = (a: TemplateElement, b: TemplateElement) => a.y - b.y || a.x - b.x;

  if (!enforceBands) {
    return [{ sectionId: undefined, title: undefined, elements: [...elements].sort(byPosition) }];
  }

  return [...NOM_SECTIONS]
    .sort((a, b) => a.order - b.order)
    .map((section) => ({
      sectionId: section.id,
      title: section.title,
      elements: elements.filter((element) => element.sectionId === section.id).sort(byPosition)
    }))
    .filter((group) => group.elements.length > 0);
}

function formatAddress(address?: Address): string {
  if (!address) return "";
  const streetLine = [address.street, address.outdoorNumber].filter(Boolean).join(" ");
  const parts = [
    streetLine || undefined,
    address.indoorNumber ? `Int. ${address.indoorNumber}` : undefined,
    address.colonia,
    address.municipality,
    address.state,
    address.zipCode ? `C.P. ${address.zipCode}` : undefined
  ].filter(Boolean);
  return parts.join(", ");
}

function buildPatientAutofill(patient: PatientResponse): Record<string, string> {
  const autofill: Record<string, string> = {};

  const fullName = [patient.firstName, patient.lastNamePaterno, patient.lastNameMaterno].filter(Boolean).join(" ");
  if (fullName) autofill.nombre = fullName;

  const gender = genderLabel(patient.gender);
  if (gender === "Masculino" || gender === "Femenino") autofill.sexo = gender;

  const age = getAge(patient.dateOfBirth);
  if (age !== undefined) autofill.edad = String(age);

  const address = formatAddress(patient.address);
  if (address) autofill.domicilio = address;

  if (patient.dateOfBirth) autofill.fechaNacimiento = patient.dateOfBirth.slice(0, 10);
  if (patient.phone) autofill.telefono = patient.phone;

  const maritalStatus = maritalStatusLabel(patient.maritalStatus);
  if (maritalStatus) autofill.estadoCivil = maritalStatus;

  if (patient.occupation) autofill.ocupacion = patient.occupation;

  const bloodType = bloodTypeLabel(patient.bloodType);
  if (bloodType) autofill.tipoSangreRh = bloodType;

  return autofill;
}

export function HistoryFormModal({
  patient,
  clinicId,
  template,
  onClose,
  onSaved
}: {
  patient: PatientResponse;
  clinicId: string;
  template: HistoryTemplateResponse;
  onClose: () => void;
  onSaved: () => void;
}) {
  const schema = parseSchema(template.schemaJson);
  const isHistoriaClinica = schema.kind === "historia_clinica";
  const [pageIndex, setPageIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showExport, setShowExport] = useState(false);
  const [showUnsavedPrompt, setShowUnsavedPrompt] = useState(false);
  const [error, setError] = useState("");
  const [clinic, setClinic] = useState<ClinicResponse | null>(null);
  const [clinicLoading, setClinicLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const initialAnswersRef = useRef<string>(serializeAnswers({}));

  useEffect(() => {
    let cancelled = false;
    clinicsApi
      .get(clinicId)
      .then((response) => {
        if (!cancelled) setClinic(response);
      })
      .catch(() => {
        // si no se puede cargar la clínica, el encabezado simplemente se omite en la vista previa y el PDF
      })
      .finally(() => {
        if (!cancelled) setClinicLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [clinicId]);

  useEffect(() => {
    let cancelled = false;
    const autofill = buildPatientAutofill(patient);
    medicalHistoryApi
      .getByTemplate(patient.id, template.id, clinicId)
      .then((history) => {
        if (cancelled) return;
        const merged = { ...autofill, ...parseAnswers(history.answersJson) };
        setAnswers(merged);
        initialAnswersRef.current = serializeAnswers(merged);
      })
      .catch((caught) => {
        if (cancelled) return;
        if (caught instanceof ApiClientError && caught.status === 404) {
          setAnswers(autofill);
          initialAnswersRef.current = serializeAnswers(autofill);
          return;
        }
        setError(getFriendlyError(caught));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [patient.id, template.id, clinicId]);

  const isDirty = () => serializeAnswers(answers) !== initialAnswersRef.current;

  const handleSave = async () => {
    setSaving(true);
    setError("");
    try {
      await medicalHistoryApi.save(patient.id, {
        clinicId,
        templateId: template.id,
        answersJson: serializeAnswers(answers)
      });
      onSaved();
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setSaving(false);
    }
  };

  const handleRequestClose = () => {
    if (isDirty()) {
      setShowUnsavedPrompt(true);
      return;
    }
    onClose();
  };

  const handleExport = async (range: PdfExportRange) => {
    setShowExport(false);
    setExporting(true);
    try {
      await exportPagesToPdf({
        title: template.name,
        pages: schema.pages,
        canvasWidthPx: CANVAS_WIDTH,
        answers,
        range,
        clinicInfo: clinic
          ? {
              name: clinic.name,
              legalName: clinic.legalName,
              addressLine: formatClinicAddress(clinic),
              phone: clinic.phone,
              email: clinic.email,
              logoUrl: clinic.logoUrl
            }
          : undefined
      });
    } finally {
      setExporting(false);
    }
  };

  const currentPage = schema.pages[pageIndex];
  const groups = groupElements(currentPage?.elements ?? [], isHistoriaClinica && pageIndex === 0);

  return (
    <div className="modal-overlay" onClick={handleRequestClose}>
      <div className="modal-card modal-card-wide" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>{template.name}</h2>
          <div className="topbar-actions">
            <button className="btn ghost" type="button" disabled={exporting} onClick={() => setShowExport(true)}>
              <IconFileTypePdf size={16} aria-hidden="true" />
              {exporting ? "Generando PDF..." : "Exportar PDF"}
            </button>
            <button className="icon-btn" type="button" aria-label="Cerrar" onClick={handleRequestClose}>
              <IconX size={18} />
            </button>
          </div>
        </div>

        {schema.pages.length > 1 && (
          <div className="canvas-page-tabs">
            {schema.pages.map((page, index) => (
              <div key={page.id} className={`canvas-page-tab ${index === pageIndex ? "active" : ""}`}>
                <button type="button" onClick={() => setPageIndex(index)}>
                  Página {index + 1}
                </button>
              </div>
            ))}
          </div>
        )}

        {loading ? (
          <p>Cargando...</p>
        ) : (
          <div className="history-form">
            {groups.map((group) => (
              <div className="history-form-group" key={group.sectionId ?? "root"}>
                {group.title && <h4>{group.title}</h4>}
                {group.elements.map((element) => {
                  const labelStyle: React.CSSProperties = {
                    fontFamily: fontCssStack(element.fontFamily ?? DEFAULT_FONT_FAMILY),
                    fontSize: element.fontSize ?? DEFAULT_FONT_SIZE,
                    fontWeight: element.bold ? 700 : 500,
                    textAlign: element.align ?? DEFAULT_TEXT_ALIGN,
                    color: element.color || undefined,
                    backgroundColor: element.backgroundColor || "transparent"
                  };
                  return (
                  <label className="field" key={element.id}>
                    <span style={labelStyle}>{element.label}</span>
                    {element.type === "textarea" ? (
                      <textarea
                        value={answers[element.id] ?? ""}
                        onChange={(event) => setAnswers((prev) => ({ ...prev, [element.id]: event.target.value }))}
                      />
                    ) : element.type === "table" ? (
                      <TableFieldEditor
                        columns={element.columns ?? []}
                        value={answers[element.id]}
                        onChange={(next) => setAnswers((prev) => ({ ...prev, [element.id]: next }))}
                      />
                    ) : element.type === "file" ? (
                      <FileFieldEditor
                        patientId={patient.id}
                        clinicId={clinicId}
                        elementId={element.id}
                        value={answers[element.id]}
                        onChange={(next) => setAnswers((prev) => ({ ...prev, [element.id]: next }))}
                      />
                    ) : element.type === "odontogram" ? (
                      <OdontogramField
                        value={answers[element.id]}
                        onChange={(next) => setAnswers((prev) => ({ ...prev, [element.id]: next }))}
                      />
                    ) : element.type === "clinic_header" ? (
                      <ClinicHeaderPreview clinic={clinic} loading={clinicLoading} />
                    ) : element.type === "signature_patient" ? (
                      <SignaturePad
                        label="Firma del paciente"
                        signerName={answers.nombre}
                        value={answers[element.id]}
                        onChange={(next) => setAnswers((prev) => ({ ...prev, [element.id]: next }))}
                      />
                    ) : element.type === "signature_doctor" ? (
                      <SignaturePad
                        label="Firma del médico"
                        value={answers[element.id]}
                        onChange={(next) => setAnswers((prev) => ({ ...prev, [element.id]: next }))}
                      />
                    ) : element.type === "select" ? (
                      <select
                        value={answers[element.id] ?? ""}
                        onChange={(event) => setAnswers((prev) => ({ ...prev, [element.id]: event.target.value }))}
                      >
                        <option value="" disabled>
                          Selecciona una opción
                        </option>
                        {(element.options ?? []).map((option) => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ))}
                      </select>
                    ) : (
                      <input
                        type={element.type === "number" ? "number" : element.type === "date" ? "date" : "text"}
                        value={answers[element.id] ?? ""}
                        onChange={(event) => setAnswers((prev) => ({ ...prev, [element.id]: event.target.value }))}
                      />
                    )}
                  </label>
                  );
                })}
              </div>
            ))}
          </div>
        )}

        {error && <p className="alert error">{error}</p>}

        <div className="form-actions">
          <button className="btn primary" type="button" disabled={loading || saving} onClick={handleSave}>
            <IconDeviceFloppy size={18} aria-hidden="true" />
            {saving ? "Guardando" : "Guardar"}
          </button>
        </div>
      </div>

      {showExport && (
        <ExportPdfModal
          totalPages={schema.pages.length}
          currentPage={pageIndex + 1}
          onCancel={() => setShowExport(false)}
          onConfirm={handleExport}
        />
      )}

      {showUnsavedPrompt && (
        <ConfirmDiscardModal
          saving={saving}
          onCancel={() => setShowUnsavedPrompt(false)}
          onDiscard={onClose}
          onSaveAndClose={async () => {
            await handleSave();
            setShowUnsavedPrompt(false);
          }}
          // onSaved (invoked inside handleSave on success) is expected to unmount this modal from the parent;
          // the setShowUnsavedPrompt(false) above only matters when the save fails and the modal stays open.
        />
      )}
    </div>
  );
}
