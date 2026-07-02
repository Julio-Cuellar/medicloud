import { useEffect, useMemo, useState } from "react";
import { IconLogout, IconMenu2, IconMoon, IconPlus, IconShieldLock, IconSun } from "@tabler/icons-react";
import { ClinicModal } from "../components/ClinicModal";
import { PatientModal } from "../components/PatientModal";
import { PatientsPanel } from "../components/PatientsPanel";
import { Sidebar } from "../components/Sidebar";
import { ExpedienteScreen } from "./ExpedienteScreen";
import { mobileRestricted, moduleCopy, modules, type ModuleKey } from "../constants/modules";
import { authApi, clinicsApi, getFriendlyError, patientsApi, sessionStore } from "../lib/api";
import type { UserProfile } from "../types/auth";
import type { ClinicResponse } from "../types/clinic";
import type { PatientResponse } from "../types/patient";
import type { Theme } from "../types/theme";
import { getInitials } from "../utils/getInitials";

export function AppShell({
  user,
  setUser,
  theme,
  setTheme
}: {
  user: UserProfile;
  setUser: (user: UserProfile | null) => void;
  theme: Theme;
  setTheme: (theme: Theme) => void;
}) {
  const [active, setActive] = useState<ModuleKey>("agenda");
  const [collapsed, setCollapsed] = useState(() => localStorage.getItem("medicloud.sidebar") === "collapsed");
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [clinics, setClinics] = useState<ClinicResponse[]>([]);
  const [clinicModal, setClinicModal] = useState<{ mode: "create" | "edit"; clinic?: ClinicResponse } | null>(null);
  const [patients, setPatients] = useState<PatientResponse[]>([]);
  const [patientsLoading, setPatientsLoading] = useState(false);
  const [patientModalOpen, setPatientModalOpen] = useState(false);
  const copy = moduleCopy[active];
  const initials = useMemo(() => getInitials(user.fullName), [user.fullName]);
  const activeClinicId = clinics[0]?.id ?? user.clinics?.[0]?.id;
  const activeClinic = clinics[0]?.name ?? user.clinics?.[0]?.name;

  const loadClinics = () => {
    clinicsApi
      .list()
      .then(setClinics)
      .catch((caught) => setError(getFriendlyError(caught)));
  };

  useEffect(() => {
    loadClinics();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if ((active !== "pacientes" && active !== "expediente") || !activeClinicId) return;
    setPatientsLoading(true);
    patientsApi
      .listByClinic(activeClinicId)
      .then(setPatients)
      .catch((caught) => setError(getFriendlyError(caught)))
      .finally(() => setPatientsLoading(false));
  }, [active, activeClinicId]);

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      sessionStore.clear();
      setUser(null);
    }
  };

  const handleClinicSaved = (saved: ClinicResponse) => {
    setClinics((prev) => {
      const exists = prev.some((clinic) => clinic.id === saved.id);
      return exists ? prev.map((clinic) => (clinic.id === saved.id ? saved : clinic)) : [...prev, saved];
    });
    const updatedUser: UserProfile = {
      ...user,
      clinics: [
        ...user.clinics.filter((clinic) => clinic.id !== saved.id),
        { id: saved.id, name: saved.name }
      ]
    };
    sessionStore.setUser(updatedUser);
    setUser(updatedUser);
    setClinicModal(null);
    setStatus(clinicModal?.mode === "create" ? "Clínica creada correctamente." : "Datos de la clínica actualizados.");
  };

  const nav = (
    <Sidebar
      active={active}
      setActive={(key) => {
        setActive(key);
        setDrawerOpen(false);
      }}
      collapsed={collapsed}
      setCollapsed={(next) => {
        setCollapsed(next);
        localStorage.setItem("medicloud.sidebar", next ? "collapsed" : "expanded");
      }}
      user={user}
      initials={initials}
      activeClinic={activeClinic}
    />
  );

  return (
    <div className={`app-shell ${collapsed ? "is-collapsed" : ""}`}>
      <div className="desktop-sidebar">{nav}</div>
      {drawerOpen && (
        <div className="sidebar-overlay" onClick={() => setDrawerOpen(false)}>
          {nav}
        </div>
      )}

      <header className="topbar">
        <button className="icon-btn mobile-only" type="button" aria-label="Abrir navegación" onClick={() => setDrawerOpen(true)}>
          <IconMenu2 size={20} />
        </button>
        <div>
          <h1>{modules.find((item) => item.key === active)?.label}</h1>
          <p>{copy.subtitle}</p>
        </div>
        <div className="topbar-actions">
          <button className="icon-btn" type="button" aria-label="Cambiar tema" onClick={() => setTheme(theme === "light" ? "dark" : "light")}>
            {theme === "light" ? <IconMoon size={18} /> : <IconSun size={18} />}
          </button>
          {copy.secondary && <button className="btn secondary" type="button">{copy.secondary}</button>}
          {copy.primary && (
            <button
              className="btn primary"
              type="button"
              disabled={active === "pacientes" && !activeClinicId}
              onClick={active === "pacientes" ? () => setPatientModalOpen(true) : undefined}
            >
              {copy.primary}
            </button>
          )}
        </div>
      </header>

      <main className="content-area">
        {mobileRestricted.has(active) && (
          <div className="mobile-restriction">
            Esta función está optimizada para pantallas más grandes. Por favor, usa una tablet o computadora.
          </div>
        )}
        {active === "pacientes" ? (
          <PatientsPanel
            clinicId={activeClinicId}
            patients={patients}
            loading={patientsLoading}
            hasClinic={Boolean(activeClinicId)}
            onPatientUpdated={(updated) =>
              setPatients((prev) => prev.map((patient) => (patient.id === updated.id ? updated : patient)))
            }
            onPatientDeleted={(patientId) =>
              setPatients((prev) => prev.filter((patient) => patient.id !== patientId))
            }
          />
        ) : active === "expediente" ? (
          <ExpedienteScreen clinicId={activeClinicId} hasClinic={Boolean(activeClinicId)} patients={patients} />
        ) : (
        <section className="dashboard-grid">
          <article className="panel">
            <div className="panel-heading">
              <h2>Perfil y sesión</h2>
              <span className="badge success">{user.active ? "Activo" : "Inactivo"}</span>
            </div>
            <div className="clinic-list">
              <div className="clinic-row">
                <strong>{user.fullName}</strong>
                <span>{user.email}</span>
              </div>
              <div className="clinic-row">
                <strong>{user.phone || "Sin teléfono registrado"}</strong>
                <span>{user.emailVerified ? "Correo verificado" : "Correo sin verificar"}</span>
              </div>
            </div>
          </article>

          <article className="panel wide">
            <div className="panel-heading">
              <h2>Clínicas</h2>
              <div className="topbar-actions">
                <span className="badge neutral">{clinics.length}</span>
                <button className="btn secondary" type="button" onClick={() => setClinicModal({ mode: "create" })}>
                  <IconPlus size={16} />
                  Nueva clínica
                </button>
              </div>
            </div>
            <div className="clinic-list">
              {clinics.length === 0 && (
                <div className="clinic-row">
                  <strong>Sin clínicas registradas</strong>
                  <span>Crea tu primera clínica para comenzar</span>
                </div>
              )}
              {clinics.map((clinic) => {
                const complete = Boolean(clinic.legalName && clinic.rfc && clinic.addressStreet);
                return (
                  <div className="clinic-row clinic-row-actionable" key={clinic.id}>
                    <div>
                      <strong>{clinic.name}</strong>
                      <span>{clinic.email || "Sin correo de contacto"}</span>
                    </div>
                    <div className="clinic-row-actions">
                      <span className={`badge ${complete ? "success" : "warning"}`}>{complete ? "Completa" : "Incompleta"}</span>
                      <button className="btn ghost" type="button" onClick={() => setClinicModal({ mode: "edit", clinic })}>
                        Completar datos
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </article>

          <article className="panel">
            <div className="panel-heading">
              <h2>Sesión</h2>
              <IconShieldLock size={20} aria-hidden="true" />
            </div>
            <div className="clinic-list">
              <button className="btn destructive" type="button" onClick={logout}>
                <IconLogout size={18} />
                Cerrar sesión
              </button>
            </div>
          </article>
        </section>
        )}

        {status && <p className="alert success">{status}</p>}
        {error && <p className="alert error">{error}</p>}
      </main>

      {clinicModal && (
        <ClinicModal
          mode={clinicModal.mode}
          clinic={clinicModal.clinic}
          onClose={() => setClinicModal(null)}
          onSaved={handleClinicSaved}
        />
      )}

      {patientModalOpen && activeClinicId && (
        <PatientModal
          mode="create"
          clinicId={activeClinicId}
          onClose={() => setPatientModalOpen(false)}
          onSaved={(patient) => {
            setPatients((prev) => [...prev, patient]);
            setPatientModalOpen(false);
            setStatus("Paciente registrado correctamente.");
          }}
        />
      )}
    </div>
  );
}
