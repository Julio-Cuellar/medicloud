import { useRef } from "react";
import { CANVAS_WIDTH, bandBoundaries, defaultSizeForType, sectionIdForY } from "../../constants/nomHistoryTemplate";
import type { TemplateElement } from "../../types/clinicalRecords";
import { CanvasElementCard } from "./CanvasElementCard";
import { DRAG_MIME, type DragBlueprint } from "./TemplatePalette";

export function TemplateCanvas({
  elements,
  canvasHeight,
  enforceBands,
  selectedId,
  onSelectElement,
  onElementsChange
}: {
  elements: TemplateElement[];
  canvasHeight: number;
  enforceBands: boolean;
  selectedId: string | null;
  onSelectElement: (id: string | null) => void;
  onElementsChange: (elements: TemplateElement[]) => void;
}) {
  const canvasRef = useRef<HTMLDivElement>(null);
  const bands = enforceBands
    ? bandBoundaries()
        .filter((band) => band.top < canvasHeight)
        .map((band) => ({ ...band, bottom: Math.min(band.bottom, canvasHeight) }))
    : [];

  const handleDrop = (event: React.DragEvent) => {
    event.preventDefault();
    const raw = event.dataTransfer.getData(DRAG_MIME);
    if (!raw || !canvasRef.current) return;

    const blueprint = JSON.parse(raw) as DragBlueprint;
    const rect = canvasRef.current.getBoundingClientRect();
    const size = defaultSizeForType(blueprint.type);
    const x = Math.max(0, Math.min(CANVAS_WIDTH - size.width, event.clientX - rect.left));
    const y = Math.max(0, Math.min(canvasHeight - size.height, event.clientY - rect.top));

    const id = blueprint.sourceFieldId ?? `field_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    if (elements.some((element) => element.id === id)) return;

    const newElement: TemplateElement = {
      id,
      sectionId: enforceBands ? blueprint.sectionId ?? sectionIdForY(y) : undefined,
      label: blueprint.label,
      type: blueprint.type,
      options: blueprint.options,
      columns: blueprint.columns,
      x,
      y,
      width: size.width,
      height: size.height
    };

    onElementsChange([...elements, newElement]);
  };

  const updateElement = (next: TemplateElement) => {
    const withRecalculatedSection: TemplateElement = enforceBands ? { ...next, sectionId: sectionIdForY(next.y) } : next;
    onElementsChange(elements.map((element) => (element.id === next.id ? withRecalculatedSection : element)));
  };

  const removeElement = (id: string) => {
    onElementsChange(elements.filter((element) => element.id !== id));
  };

  const sectionCounts = (sectionId: string) => elements.filter((element) => element.sectionId === sectionId).length;

  return (
    <div className="canvas-scroll">
      <div
        className="canvas-page"
        ref={canvasRef}
        style={{ width: CANVAS_WIDTH, height: canvasHeight }}
        onDragOver={(event) => event.preventDefault()}
        onDrop={handleDrop}
        onMouseDown={(event) => {
          if (event.target === canvasRef.current) onSelectElement(null);
        }}
      >
        {bands.map((band) => (
          <div
            key={band.sectionId}
            className={`canvas-band ${sectionCounts(band.sectionId) < band.minElements ? "under-minimum" : ""}`}
            style={{ top: band.top, height: band.bottom - band.top }}
          >
            <div className="canvas-band-header">
              <span>{band.title}</span>
              <span className="canvas-band-count">
                {sectionCounts(band.sectionId)}/{band.minElements} sugeridos
              </span>
            </div>
          </div>
        ))}

        {elements.map((element) => (
          <CanvasElementCard
            key={element.id}
            element={element}
            canvasWidth={CANVAS_WIDTH}
            canvasHeight={canvasHeight}
            selected={selectedId === element.id}
            onSelect={() => onSelectElement(element.id)}
            onChange={updateElement}
            onRemove={() => {
              removeElement(element.id);
              if (selectedId === element.id) onSelectElement(null);
            }}
          />
        ))}
      </div>
    </div>
  );
}
