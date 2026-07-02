import {
  IconCalendar,
  IconCash,
  IconClipboardText,
  IconFileInvoice,
  IconPackage,
  IconStethoscope,
  IconUsers
} from "@tabler/icons-react";

export type ModuleKey = "agenda" | "pacientes" | "expediente" | "caja" | "inventario" | "contabilidad" | "personal";

export const modules: Array<{ key: ModuleKey; label: string; section: string; icon: typeof IconCalendar }> = [
  { key: "agenda", label: "Agenda", section: "Clínica", icon: IconCalendar },
  { key: "pacientes", label: "Pacientes", section: "Clínica", icon: IconUsers },
  { key: "expediente", label: "Expediente", section: "Clínica", icon: IconClipboardText },
  { key: "caja", label: "Caja", section: "Administración", icon: IconCash },
  { key: "inventario", label: "Inventario", section: "Administración", icon: IconPackage },
  { key: "contabilidad", label: "Contabilidad", section: "Administración", icon: IconFileInvoice },
  { key: "personal", label: "Personal", section: "Administración", icon: IconStethoscope }
];

export const moduleCopy: Record<ModuleKey, { subtitle: string; primary?: string; secondary?: string }> = {
  agenda: { subtitle: "Lunes 15 de junio, 2026", primary: "+ Nueva cita", secondary: "Vista semana" },
  pacientes: { subtitle: "Catálogo activo de pacientes", primary: "+ Nuevo paciente", secondary: "Exportar listado" },
  expediente: { subtitle: "Plantillas e historia clínica de pacientes" },
  caja: { subtitle: "Cobros, sesiones y cortes", primary: "Registrar cobro", secondary: "Abrir sesión" },
  inventario: { subtitle: "Insumos y existencias básicas", primary: "+ Agregar producto", secondary: "Ajuste" },
  contabilidad: { subtitle: "ARE y libro mayor interno", secondary: "Exportar periodo" },
  personal: { subtitle: "Staff, roles y asistencia", primary: "+ Agregar empleado" }
};

export const mobileRestricted = new Set<ModuleKey>(["agenda", "expediente", "caja", "contabilidad"]);
