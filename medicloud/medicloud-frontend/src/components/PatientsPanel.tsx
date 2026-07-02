import { useEffect, useMemo, useState, type MouseEvent } from "react";
import { IconFileText, IconSearch } from "@tabler/icons-react";
import { genderLabel } from "../constants/patientOptions";
import { ApiClientError, getFriendlyError, historyTemplatesApi, medicalHistoryApi, patientsApi } from "../lib/api";
import { parseSchema, type HistoryTemplateResponse } from "../types/clinicalRecords";
import type { PatientResponse } from "../types/patient";
import { getAge } from "../utils/getAge";
import { HistoryFormModal } from "./HistoryFormModal";
import { PatientDetailModal } from "./PatientDetailModal";
import { PatientModal } from "./PatientModal";

function templateIsHistoriaClinica(template: HistoryTemplateResponse): boolean {
  return parseSchema(template.schemaJson).kind === "historia_clinica";
}

function PatientHistoryButton({
  clinicId,
  patient,
  template,
  onOpen
}: {
  clinicId: string;
  patient: PatientResponse;
  template: HistoryTemplateResponse;
  onOpen: () => void;
}) {
  const [started, setStarted] = useState<boolean | null>(null);

  useEffect(() => {
    let cancelled = false;
    medicalHistoryApi
      .getByTemplate(patient.id, template.id, clinicId)
      .then(() => {
        if (!cancelled) setStarted(true);
      })
      .catch((caught) => {
        if (cancelled) return;
        if (caught instanceof ApiClientError && caught.status === 404) {
          setStarted(false);
          return;
        }
        setStarted(false);
      });
    return () => {
      cancelled = true;
    };
  }, [clinicId, patient.id, template.id]);

  return (
    <button
      className="btn ghost"
      type="button"
      disabled={started === null}
      onClick={(event) => {
        event.stopPropagation();
        onOpen();
      }}
    >
      <IconFileText size={14} aria-hidden="true" />
      {started === null ? "..." : started ? "Observar historia clínica" : "Iniciar historia clínica"}
    </button>
  );
}

export function PatientsPanel({
  clinicId,
  patients,
  loading,
  hasClinic,
  onPatientUpdated,
  onPatientDeleted
}: {
  clinicId?: string;
  patients: PatientResponse[];
  loading: boolean;
  hasClinic: boolean;
  onPatientUpdated: (patient: PatientResponse) => void;
  onPatientDeleted: (patientId: string) => void;
}) {
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState<PatientResponse | null>(null);
  const [editing, setEditing] = useState<PatientResponse | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [historiaTemplate, setHistoriaTemplate] = useState<HistoryTemplateResponse | null>(null);
  const [historyPatient, setHistoryPatient] = useState<PatientResponse | null>(null);
  const [historyRefreshKey, setHistoryRefreshKey] = useState(0);

  useEffect(() => {
    if (!clinicId) {
      setHistoriaTemplate(null);
      return;
    }
    historyTemplatesApi
      .list(clinicId)
      .then((templates) => {
        setHistoriaTemplate(templates.find((template) => template.active && templateIsHistoriaClinica(template)) ?? null);
      })
      .catch(() => setHistoriaTemplate(null));
  }, [clinicId]);

  const filteredPatients = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return patients;
    return patients.filter((patient) => {
      const haystack = [patient.firstName, patient.lastNamePaterno, patient.lastNameMaterno, patient.curp, patient.phone, patient.email]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return haystack.includes(term);
    });
  }, [patients, search]);

  const handleDelete = async (event: MouseEvent, patient: PatientResponse) => {
    event.stopPropagation();
    const fullName = `${patient.firstName} ${patient.lastNamePaterno}`.trim();
    if (!window.confirm(`¿Eliminar a ${fullName}? Esta acción no se puede deshacer.`)) return;

    setDeletingId(patient.id);
    setError("");
    try {
      await patientsApi.remove(patient.id);
      onPatientDeleted(patient.id);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <section className="dashboard-grid">
      <article className="panel full">
        <div className="panel-heading">
          <h2>Pacientes</h2>
          <span className="badge neutral">{patients.length}</span>
        </div>

        {hasClinic && patients.length > 0 && (
          <div className="table-toolbar">
            <label className="search-field">
              <IconSearch size={16} aria-hidden="true" />
              <input
                type="search"
                placeholder="Buscar por nombre, CURP, teléfono o correo"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </label>
          </div>
        )}

        {!hasClinic && (
          <div className="clinic-list">
            <div className="clinic-row">
              <strong>Completa los datos de tu clínica</strong>
              <span>Necesitas una clínica activa para registrar pacientes</span>
            </div>
          </div>
        )}
        {hasClinic && loading && (
          <div className="clinic-list">
            <div className="clinic-row">
              <strong>Cargando pacientes...</strong>
            </div>
          </div>
        )}
        {hasClinic && !loading && patients.length === 0 && (
          <div className="clinic-list">
            <div className="clinic-row">
              <strong>Sin pacientes registrados</strong>
              <span>Usa "+ Nuevo paciente" para registrar el primero</span>
            </div>
          </div>
        )}
        {hasClinic && !loading && patients.length > 0 && (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Paciente</th>
                  <th>Edad</th>
                  <th>Género</th>
                  <th>Contacto</th>
                  <th>Ocupación</th>
                  <th aria-label="Acciones" />
                </tr>
              </thead>
              <tbody>
                {filteredPatients.map((patient) => {
                  const age = getAge(patient.dateOfBirth);
                  return (
                    <tr key={patient.id} onClick={() => setSelected(patient)}>
                      <td>
                        <strong>
                          {patient.firstName} {patient.lastNamePaterno} {patient.lastNameMaterno}
                        </strong>
                      </td>
                      <td>{age !== undefined ? `${age} años` : "Sin registrar"}</td>
                      <td>{genderLabel(patient.gender) ?? "Sin registrar"}</td>
                      <td>{patient.phone || patient.email || "Sin contacto"}</td>
                      <td>{patient.occupation || "Sin registrar"}</td>
                      <td className="table-actions">
                        {historiaTemplate && clinicId && (
                          <PatientHistoryButton
                            key={`${patient.id}-${historyRefreshKey}`}
                            clinicId={clinicId}
                            patient={patient}
                            template={historiaTemplate}
                            onOpen={() => setHistoryPatient(patient)}
                          />
                        )}
                        <button
                          className="btn ghost"
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            setEditing(patient);
                          }}
                        >
                          Editar
                        </button>
                        <button
                          className="btn destructive"
                          type="button"
                          disabled={deletingId === patient.id}
                          onClick={(event) => handleDelete(event, patient)}
                        >
                          {deletingId === patient.id ? "Eliminando" : "Eliminar"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
                {filteredPatients.length === 0 && (
                  <tr>
                    <td colSpan={6}>Sin resultados para "{search}"</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {error && <p className="alert error">{error}</p>}
      </article>

      {selected && <PatientDetailModal patient={selected} onClose={() => setSelected(null)} />}

      {editing && clinicId && (
        <PatientModal
          mode="edit"
          clinicId={clinicId}
          patient={editing}
          onClose={() => setEditing(null)}
          onSaved={(updated) => {
            onPatientUpdated(updated);
            setEditing(null);
          }}
        />
      )}

      {historyPatient && historiaTemplate && clinicId && (
        <HistoryFormModal
          patient={historyPatient}
          clinicId={clinicId}
          template={historiaTemplate}
          onClose={() => setHistoryPatient(null)}
          onSaved={() => {
            setHistoryPatient(null);
            setHistoryRefreshKey((key) => key + 1);
          }}
        />
      )}
    </section>
  );
}
