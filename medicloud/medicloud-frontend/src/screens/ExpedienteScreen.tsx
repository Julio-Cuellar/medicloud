import { useMemo, useState } from "react";
import { IconSearch } from "@tabler/icons-react";
import { PatientHistoryPanel } from "../components/PatientHistoryPanel";
import { TemplatesPanel } from "../components/TemplatesPanel";
import type { PatientResponse } from "../types/patient";

type Tab = "plantillas" | "historias";

export function ExpedienteScreen({
  clinicId,
  hasClinic,
  patients
}: {
  clinicId?: string;
  hasClinic: boolean;
  patients: PatientResponse[];
}) {
  const [tab, setTab] = useState<Tab>("plantillas");
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [search, setSearch] = useState("");

  const filteredPatients = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return patients;
    return patients.filter((patient) =>
      [patient.firstName, patient.lastNamePaterno, patient.lastNameMaterno, patient.curp]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
        .includes(term)
    );
  }, [patients, search]);

  return (
    <section className="dashboard-grid">
      <div className="tab-switch">
        <button className={tab === "plantillas" ? "active" : ""} type="button" onClick={() => setTab("plantillas")}>
          Plantillas
        </button>
        <button className={tab === "historias" ? "active" : ""} type="button" onClick={() => setTab("historias")}>
          Historias clínicas
        </button>
      </div>

      {tab === "plantillas" && <TemplatesPanel clinicId={clinicId} hasClinic={hasClinic} />}

      {tab === "historias" &&
        (selectedPatient && clinicId ? (
          <PatientHistoryPanel clinicId={clinicId} patient={selectedPatient} onChangePatient={() => setSelectedPatient(null)} />
        ) : (
          <article className="panel full">
            <div className="panel-heading">
              <h2>Selecciona un paciente</h2>
              <span className="badge neutral">{patients.length}</span>
            </div>

            {!hasClinic && (
              <div className="clinic-list">
                <div className="clinic-row">
                  <strong>Completa los datos de tu clínica</strong>
                  <span>Necesitas una clínica activa para ver historias clínicas</span>
                </div>
              </div>
            )}

            {hasClinic && patients.length > 0 && (
              <div className="table-toolbar">
                <label className="search-field">
                  <IconSearch size={16} aria-hidden="true" />
                  <input
                    type="search"
                    placeholder="Buscar paciente por nombre o CURP"
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                  />
                </label>
              </div>
            )}

            {hasClinic && patients.length === 0 && (
              <div className="clinic-list">
                <div className="clinic-row">
                  <strong>Sin pacientes registrados</strong>
                  <span>Registra pacientes en el módulo Pacientes para ver su historia clínica</span>
                </div>
              </div>
            )}

            {hasClinic && patients.length > 0 && (
              <div className="table-wrapper">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Paciente</th>
                      <th>CURP</th>
                      <th>Contacto</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredPatients.map((patient) => (
                      <tr key={patient.id} onClick={() => setSelectedPatient(patient)}>
                        <td>
                          <strong>
                            {patient.firstName} {patient.lastNamePaterno} {patient.lastNameMaterno}
                          </strong>
                        </td>
                        <td>{patient.curp || "Sin registrar"}</td>
                        <td>{patient.phone || patient.email || "Sin contacto"}</td>
                      </tr>
                    ))}
                    {filteredPatients.length === 0 && (
                      <tr>
                        <td colSpan={3}>Sin resultados para "{search}"</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </article>
        ))}
    </section>
  );
}
