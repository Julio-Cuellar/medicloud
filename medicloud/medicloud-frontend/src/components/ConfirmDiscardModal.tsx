export function ConfirmDiscardModal({
  saving,
  onSaveAndClose,
  onDiscard,
  onCancel
}: {
  saving: boolean;
  onSaveAndClose: () => void;
  onDiscard: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal-card" onClick={(event) => event.stopPropagation()}>
        <div className="panel-heading">
          <h2>Cambios sin guardar</h2>
        </div>
        <p className="confirm-summary-subtitle">Tienes cambios sin guardar en esta historia clínica. ¿Qué deseas hacer?</p>
        <div className="form-actions">
          <button className="btn ghost" type="button" onClick={onCancel}>
            Seguir editando
          </button>
          <button className="btn secondary" type="button" onClick={onDiscard}>
            Descartar cambios
          </button>
          <button className="btn primary" type="button" disabled={saving} onClick={onSaveAndClose}>
            {saving ? "Guardando..." : "Guardar y salir"}
          </button>
        </div>
      </div>
    </div>
  );
}
