import { useEffect, useState } from "react";
import { IconArrowLeft } from "@tabler/icons-react";
import { getFriendlyError, historyTemplatesApi, medicalHistoryApi } from "../lib/api";
import type { HistoryTemplateResponse, MedicalHistoryResponse } from "../types/clinicalRecords";
import type { PatientResponse } from "../types/patient";
import { HistoryFormModal } from "./HistoryFormModal";

export function PatientHistoryPanel({
  clinicId,
  patient,
  onChangePatient
}: {
  clinicId: string;
  patient: PatientResponse;
  onChangePatient: () => void;
}) {
  const [templates, setTemplates] = useState<HistoryTemplateResponse[]>([]);
  const [histories, setHistories] = useState<MedicalHistoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [activeTemplate, setActiveTemplate] = useState<HistoryTemplateResponse | null>(null);

  const load = () => {
    setLoading(true);
    setError("");
    Promise.all([historyTemplatesApi.list(clinicId), medicalHistoryApi.listByPatient(patient.id, clinicId)])
      .then(([templateList, historyList]) => {
        setTemplates(templateList.filter((template) => template.active));
        setHistories(historyList);
      })
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clinicId, patient.id]);

  return (
    <article className="panel full">
      <div className="panel-heading">
        <div>
          <h2>
            {patient.firstName} {patient.lastNamePaterno} {patient.lastNameMaterno}
          </h2>
          <span>Historia clínica y formularios</span>
        </div>
        <button className="btn secondary" type="button" onClick={onChangePatient}>
          <IconArrowLeft size={16} aria-hidden="true" />
          Cambiar paciente
        </button>
      </div>

      {loading && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>Cargando plantillas...</strong>
          </div>
        </div>
      )}

      {!loading && templates.length === 0 && (
        <div className="clinic-list">
          <div className="clinic-row">
            <strong>No hay plantillas activas</strong>
            <span>Crea una plantilla en la pestaña "Plantillas" para poder llenar la historia clínica</span>
          </div>
        </div>
      )}

      {!loading && templates.length > 0 && (
        <div className="clinic-list">
          {templates.map((template) => {
            const history = histories.find((item) => item.templateId === template.id);
            return (
              <div className="clinic-row clinic-row-actionable" key={template.id} onClick={() => setActiveTemplate(template)}>
                <div>
                  <strong>{template.name}</strong>
                  <span>{history ? `Última actualización: ${new Date(history.updatedAt).toLocaleString()}` : "Sin iniciar"}</span>
                </div>
                <div className="clinic-row-actions">
                  <span className={`badge ${history ? "success" : "neutral"}`}>{history ? "Completada" : "Sin iniciar"}</span>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {error && <p className="alert error">{error}</p>}

      {activeTemplate && (
        <HistoryFormModal
          patient={patient}
          clinicId={clinicId}
          template={activeTemplate}
          onClose={() => setActiveTemplate(null)}
          onSaved={() => {
            setActiveTemplate(null);
            load();
          }}
        />
      )}
    </article>
  );
}
