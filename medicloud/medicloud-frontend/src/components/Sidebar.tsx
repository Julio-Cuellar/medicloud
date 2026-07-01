import { IconAdjustments, IconChevronLeft, IconChevronRight } from "@tabler/icons-react";
import { modules, type ModuleKey } from "../constants/modules";
import type { UserProfile } from "../types/auth";

export function Sidebar({
  active,
  setActive,
  collapsed,
  setCollapsed,
  user,
  initials,
  activeClinic
}: {
  active: ModuleKey;
  setActive: (key: ModuleKey) => void;
  collapsed: boolean;
  setCollapsed: (next: boolean) => void;
  user: UserProfile;
  initials: string;
  activeClinic?: string;
}) {
  return (
    <aside className="sidebar">
      <div className="brand-row sidebar-brand">
        <span className="brand-dot" />
        {!collapsed && <span className="brand-word">MediCloud</span>}
      </div>
      {!collapsed && activeClinic && <button className="clinic-selector" type="button">{activeClinic}</button>}
      {["Clínica", "Administración"].map((section) => (
        <nav key={section} aria-label={section}>
          {!collapsed && <p className="nav-section">{section}</p>}
          {modules.filter((item) => item.section === section).map((item) => {
            const Icon = item.icon;
            return (
              <button
                className={`nav-item ${active === item.key ? "active" : ""}`}
                key={item.key}
                title={collapsed ? item.label : undefined}
                type="button"
                onClick={() => setActive(item.key)}
              >
                <Icon size={18} aria-hidden="true" />
                {!collapsed && <span>{item.label}</span>}
              </button>
            );
          })}
        </nav>
      ))}
      <div className="sidebar-bottom">
        <button className="nav-item" type="button">
          <IconAdjustments size={18} aria-hidden="true" />
          {!collapsed && <span>Configuración</span>}
        </button>
        <div className="user-chip" title={user.fullName}>
          <span>{initials}</span>
          {!collapsed && <small>{user.fullName}</small>}
        </div>
        <button className="collapse-btn" aria-label={collapsed ? "Expandir sidebar" : "Colapsar sidebar"} type="button" onClick={() => setCollapsed(!collapsed)}>
          {collapsed ? <IconChevronRight size={18} /> : <IconChevronLeft size={18} />}
        </button>
      </div>
    </aside>
  );
}
