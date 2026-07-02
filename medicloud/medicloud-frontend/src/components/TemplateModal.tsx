import { useState } from "react";
import { IconDeviceFloppy, IconFileText, IconFileTypePdf, IconPlus, IconTrash, IconX } from "@tabler/icons-react";
import { buildNomStarterPages, checkCompliance, CANVAS_PAGE_HEIGHT, CANVAS_WIDTH } from "../constants/nomHistoryTemplate";
import { exportPagesToPdf, type PdfExportRange } from "../lib/pdfExport";
import { getFriendlyError, historyTemplatesApi } from "../lib/api";
import {
  genPageId,
  parseSchema,
  serializeSchema,
  type HistoryTemplateResponse,
  type TemplateElement,
  type TemplateKind,
  type TemplatePage,
  type TemplateSchema
} from "../types/clinicalRecords";
import { ComplianceOverrideModal } from "./ComplianceOverrideModal";
import { ExportPdfModal } from "./ExportPdfModal";
import { TemplateCanvas } from "./canvas/TemplateCanvas";
import { TemplatePalette } from "./canvas/TemplatePalette";
import { TypographyToolbar } from "./canvas/TypographyToolbar";

export function TemplateModal({
  clinicId,
  template,
  onClose,
  onSaved
}: {
  clinicId: string;
  template?: HistoryTemplateResponse;
  onClose: () => void;
  onSaved: (template: HistoryTemplateResponse) => void;
}) {
  const initialSchema = template ? parseSchema(template.schemaJson) : null;
  const [kind, setKind] = useState<TemplateKind | null>(initialSchema?.kind ?? null);
  const [name, setName] = useState(template?.name ?? "");
  const [description, setDescription] = useState(template?.description ?? "");
  const [pages, setPages] = useState<TemplatePage[]>(initialSchema?.pages ?? []);
  const [pageIndex, setPageIndex] = useState(0);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [compliance, setCompliance] = useState(initialSchema?.compliance);
  const [showOverride, setShowOverride] = useState<string[] | null>(null);
  const [showExport, setShowExport] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const currentPage = pages[pageIndex];

  const setCurrentPageElements = (elements: TemplateElement[]) => {
    setPages((prev) => prev.map((page, index) => (index === pageIndex ? { ...page, elements } : page)));
  };

  const changePage = (index: number) => {
    setPageIndex(index);
    setSelectedId(null);
  };

  const addPage = () => {
    const created: TemplatePage = { id: genPageId(), elements: [], canvasHeight: CANVAS_PAGE_HEIGHT };
    setPages((prev) => [...prev, created]);
    changePage(pages.length);
  };

  const removePage = (index: number) => {
    if (pages.length <= 1) return;
    setPages((prev) => prev.filter((_, i) => i !== index));
    setSelectedId(null);
    setPageIndex((prev) => Math.max(0, prev >= index ? prev - 1 : prev));
  };

  const updateSelectedElement = (next: TemplateElement) => {
    setCurrentPageElements((currentPage?.elements ?? []).map((element) => (element.id === next.id ? next : element)));
  };

  const selectedElement = currentPage?.elements.find((element) => element.id === selectedId) ?? null;

  const persist = async (schemaOverride?: Partial<TemplateSchema>) => {
    if (!kind) return;
    setLoading(true);
    setError("");
    try {
      const schema: TemplateSchema = { kind, pages, compliance, ...schemaOverride };
      const schemaJson = serializeSchema(schema);

      const saved = template
        ? await historyTemplatesApi.update(clinicId, template.id, {
            name,
            description: description || undefined,
            schemaJson,
            active: template.active
          })
        : await historyTemplatesApi.create(clinicId, { name, description: description || undefined, schemaJson });

      onSaved(saved);
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  const handleSave = () => {
    if (!name.trim()) {
      setError("El nombre de la plantilla es obligatorio.");
      return;
    }
    if (pages.every((page) => page.elements.length === 0)) {
      setError("Agrega al menos un elemento al lienzo.");
      return;
    }

    if (kind === "historia_clinica" && !compliance?.overridden) {
      const result = checkCompliance(pages.flatMap((page) => page.elements));
      if (!result.compliant) {
        setShowOverride(result.missingSections);
        return;
      }
    }

    persist();
  };

  const confirmOverride = () => {
    if (!showOverride) return;
    const nextCompliance = {
      overridden: true,
      overriddenAt: new Date().toISOString(),
      missingSections: showOverride
    };
    setCompliance(nextCompliance);
    setShowOverride(null);
    persist({ compliance: nextCompliance });
  };

  const handleExport = (range: PdfExportRange) => {
    setShowExport(false);
    exportPagesToPdf({ title: name || "Plantilla", pages, canvasWidthPx: CANVAS_WIDTH, range });
  };

  if (!kind) {
    return (
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-card" onClick={(event) => event.stopPropagation()}>
          <div className="panel-heading">
            <h2>Nueva plantilla</h2>
            <button className="icon-btn" type="button" aria-label="Cerrar" onClick={onClose}>
              <IconX size={18} />
            </button>
          </div>
          <p className="confirm-summary-subtitle">Elige el tipo de plantilla que quieres crear:</p>
          <div className="template-kind-options">
            <button
              className="template-kind-card"
              type="button"
              onClick={() => {
                setKind("historia_clinica");
                setPages(buildNomStarterPages());
              }}
            >
              <IconFileText size={28} aria-hidden="true" />
              <strong>Historia clínica (NOM-004)</strong>
              <span>Incluye las secciones y campos sugeridos por la norma, listos para editar.</span>
            </button>
            <button
              className="template-kind-card"
              type="button"
              onClick={() => {
                setKind("complementario");
                setPages([{ id: genPageId(), elements: [], canvasHeight: CANVAS_PAGE_HEIGHT }]);
              }}
            >
              <IconFileText size={28} aria-hidden="true" />
              <strong>Formulario complementario</strong>
              <span>Lienzo libre para documentos adicionales (odontograma, nutrición, etc.).</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  const enforceBands = kind === "historia_clinica" && pageIndex === 0;

  return (
    <div className="canvas-editor-overlay">
      <header className="canvas-editor-header">
        <div className="canvas-editor-header-fields">
          <input
            className="canvas-editor-name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Nombre de la plantilla"
          />
          <input
            className="canvas-editor-description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Descripción (opcional)"
          />
        </div>
        <div className="canvas-editor-header-actions">
          {error && <span className="alert error">{error}</span>}
          <button className="btn ghost" type="button" onClick={() => setShowExport(true)} disabled={loading}>
            <IconFileTypePdf size={18} aria-hidden="true" />
            Exportar PDF
          </button>
          <button className="btn secondary" type="button" onClick={onClose} disabled={loading}>
            Cancelar
          </button>
          <button className="btn primary" type="button" onClick={handleSave} disabled={loading}>
            <IconDeviceFloppy size={18} aria-hidden="true" />
            {loading ? "Guardando" : "Guardar"}
          </button>
        </div>
      </header>

      <div className="canvas-page-tabs">
        {pages.map((page, index) => (
          <div key={page.id} className={`canvas-page-tab ${index === pageIndex ? "active" : ""}`}>
            <button type="button" onClick={() => changePage(index)}>
              Página {index + 1}
            </button>
            {pages.length > 1 && (
              <button
                type="button"
                className="canvas-page-tab-remove"
                aria-label={`Eliminar página ${index + 1}`}
                onClick={() => removePage(index)}
              >
                <IconTrash size={12} />
              </button>
            )}
          </div>
        ))}
        <button type="button" className="canvas-page-tab-add" onClick={addPage}>
          <IconPlus size={14} aria-hidden="true" />
          Página
        </button>
      </div>

      {selectedElement && <TypographyToolbar element={selectedElement} onChange={updateSelectedElement} />}

      <div className="canvas-editor-body">
        <TemplatePalette enforceBands={enforceBands} existingElements={currentPage?.elements ?? []} />
        <TemplateCanvas
          elements={currentPage?.elements ?? []}
          canvasHeight={currentPage?.canvasHeight ?? 400}
          enforceBands={enforceBands}
          selectedId={selectedId}
          onSelectElement={setSelectedId}
          onElementsChange={setCurrentPageElements}
        />
      </div>

      {showOverride && (
        <ComplianceOverrideModal
          missingSections={showOverride}
          onCancel={() => setShowOverride(null)}
          onConfirm={confirmOverride}
        />
      )}

      {showExport && (
        <ExportPdfModal
          totalPages={pages.length}
          currentPage={pageIndex + 1}
          onCancel={() => setShowExport(false)}
          onConfirm={handleExport}
        />
      )}
    </div>
  );
}
