import { FREE_ELEMENT_TYPES, NOM_SECTIONS, type NomFieldSuggestion } from "../../constants/nomHistoryTemplate";
import type { TemplateElement, TemplateFieldType } from "../../types/clinicalRecords";

export interface DragBlueprint {
  label: string;
  type: TemplateFieldType;
  options?: string[];
  columns?: string[];
  sectionId?: string;
  sourceFieldId?: string;
}

export const DRAG_MIME = "application/x-medicloud-template-field";

export function TemplatePalette({
  enforceBands,
  existingElements
}: {
  enforceBands: boolean;
  existingElements: TemplateElement[];
}) {
  const handleDragStart = (event: React.DragEvent, blueprint: DragBlueprint) => {
    event.dataTransfer.setData(DRAG_MIME, JSON.stringify(blueprint));
    event.dataTransfer.effectAllowed = "copy";
  };

  const isSuggestedFieldUsed = (field: NomFieldSuggestion) => existingElements.some((element) => element.id === field.id);

  return (
    <aside className="canvas-palette">
      <p className="canvas-palette-hint">
        {enforceBands
          ? "Arrastra los campos sugeridos por sección hacia el lienzo."
          : "Arrastra cualquier herramienta del expediente NOM-004 hacia tu formulario."}
      </p>
      {[...NOM_SECTIONS]
        .sort((a, b) => a.order - b.order)
        .map((section) => (
          <div className="canvas-palette-group" key={section.id}>
            <h4>{section.title}</h4>
            <div className="canvas-palette-chips">
              {section.suggestedFields.map((field) => {
                const used = isSuggestedFieldUsed(field);
                return (
                  <div
                    key={field.id}
                    className={`canvas-palette-chip ${used ? "used" : ""}`}
                    draggable={!used}
                    onDragStart={(event) =>
                      handleDragStart(event, {
                        label: field.label,
                        type: field.type,
                        options: field.options,
                        columns: field.columns,
                        sectionId: enforceBands ? section.id : undefined,
                        sourceFieldId: field.id
                      })
                    }
                  >
                    {field.label}
                  </div>
                );
              })}
            </div>
          </div>
        ))}

      <div className="canvas-palette-group">
        <h4>Elementos libres</h4>
        <div className="canvas-palette-chips">
          {FREE_ELEMENT_TYPES.map((item) => (
            <div
              key={item.type}
              className="canvas-palette-chip"
              draggable
              onDragStart={(event) => handleDragStart(event, { label: item.label, type: item.type, columns: item.columns })}
            >
              {item.label}
            </div>
          ))}
        </div>
      </div>
    </aside>
  );
}
