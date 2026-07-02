import { useRef, useState } from "react";
import { IconDental, IconPaperclip, IconPhoto, IconSignature, IconX } from "@tabler/icons-react";
import {
  DEFAULT_FONT_FAMILY,
  DEFAULT_FONT_SIZE,
  DEFAULT_TEXT_ALIGN,
  fontCssStack,
  type TemplateElement
} from "../../types/clinicalRecords";

const MIN_WIDTH = 140;
const MIN_HEIGHT = 40;

export function CanvasElementCard({
  element,
  onChange,
  onRemove,
  onSelect,
  selected,
  canvasWidth,
  canvasHeight
}: {
  element: TemplateElement;
  onChange: (next: TemplateElement) => void;
  onRemove: () => void;
  onSelect: () => void;
  selected: boolean;
  canvasWidth: number;
  canvasHeight: number;
}) {
  const [editingOptions, setEditingOptions] = useState(false);
  const labelInputRef = useRef<HTMLInputElement>(null);
  const dragState = useRef<{ startX: number; startY: number; originX: number; originY: number } | null>(null);
  const resizeState = useRef<{ startX: number; startY: number; originWidth: number; originHeight: number } | null>(null);

  const isInteractiveTarget = (target: EventTarget | null) => {
    if (target instanceof HTMLInputElement || target instanceof HTMLButtonElement || target instanceof HTMLTextAreaElement) return true;
    return target instanceof HTMLElement && Boolean(target.closest(".canvas-element-interactive"));
  };

  const handleWrapperPointerDown = (event: React.PointerEvent) => {
    onSelect();
    if (isInteractiveTarget(event.target)) return;
    event.preventDefault();
    try {
      (event.target as HTMLElement).setPointerCapture(event.pointerId);
    } catch {
      // el navegador puede rechazar la captura para eventos sintéticos; el arrastre sigue funcionando por delegación normal
    }
    dragState.current = { startX: event.clientX, startY: event.clientY, originX: element.x, originY: element.y };
  };

  const handleWrapperPointerMove = (event: React.PointerEvent) => {
    if (!dragState.current) return;
    const dx = event.clientX - dragState.current.startX;
    const dy = event.clientY - dragState.current.startY;
    const nextX = Math.max(0, Math.min(canvasWidth - element.width, dragState.current.originX + dx));
    const nextY = Math.max(0, Math.min(canvasHeight - element.height, dragState.current.originY + dy));
    onChange({ ...element, x: nextX, y: nextY });
  };

  const handleWrapperPointerUp = () => {
    dragState.current = null;
  };

  const handleResizePointerDown = (event: React.PointerEvent) => {
    event.preventDefault();
    event.stopPropagation();
    try {
      (event.target as HTMLElement).setPointerCapture(event.pointerId);
    } catch {
      // ver nota en handleWrapperPointerDown
    }
    resizeState.current = { startX: event.clientX, startY: event.clientY, originWidth: element.width, originHeight: element.height };
  };

  const handleResizePointerMove = (event: React.PointerEvent) => {
    if (!resizeState.current) return;
    const dx = event.clientX - resizeState.current.startX;
    const dy = event.clientY - resizeState.current.startY;
    const nextWidth = Math.max(MIN_WIDTH, Math.min(canvasWidth - element.x, resizeState.current.originWidth + dx));
    const nextHeight = Math.max(MIN_HEIGHT, Math.min(canvasHeight - element.y, resizeState.current.originHeight + dy));
    onChange({ ...element, width: nextWidth, height: nextHeight });
  };

  const handleResizePointerUp = () => {
    resizeState.current = null;
  };

  const textStyle: React.CSSProperties = {
    fontFamily: fontCssStack(element.fontFamily ?? DEFAULT_FONT_FAMILY),
    fontSize: element.fontSize ?? DEFAULT_FONT_SIZE,
    fontWeight: element.bold ? 700 : 500,
    textAlign: element.align ?? DEFAULT_TEXT_ALIGN,
    color: element.color || undefined
  };

  return (
    <div
      className={`canvas-element-card ${selected ? "selected" : ""}`}
      style={{
        left: element.x,
        top: element.y,
        width: element.width,
        height: element.height,
        backgroundColor: element.backgroundColor || "transparent"
      }}
      onPointerDown={handleWrapperPointerDown}
      onPointerMove={handleWrapperPointerMove}
      onPointerUp={handleWrapperPointerUp}
    >
      <button className="canvas-element-remove" type="button" aria-label="Eliminar elemento" onClick={onRemove}>
        <IconX size={12} />
      </button>
      <input
        ref={labelInputRef}
        className="canvas-element-label"
        style={textStyle}
        value={element.label}
        onChange={(event) => onChange({ ...element, label: event.target.value })}
        placeholder="Etiqueta del campo"
      />
      {element.type === "select" &&
        (editingOptions ? (
          <input
            className="canvas-element-options"
            autoFocus
            defaultValue={(element.options ?? []).join(", ")}
            placeholder="Opción 1, Opción 2, Opción 3"
            onBlur={(event) => {
              const options = event.target.value
                .split(",")
                .map((option) => option.trim())
                .filter(Boolean);
              onChange({ ...element, options });
              setEditingOptions(false);
            }}
          />
        ) : (
          <button className="canvas-element-options-preview" type="button" onClick={() => setEditingOptions(true)}>
            {element.options && element.options.length > 0 ? element.options.join(", ") : "Definir opciones..."}
          </button>
        ))}
      {element.type === "table" &&
        (editingOptions ? (
          <input
            className="canvas-element-options"
            autoFocus
            defaultValue={(element.columns ?? []).join(", ")}
            placeholder="Columna 1, Columna 2, Columna 3"
            onBlur={(event) => {
              const columns = event.target.value
                .split(",")
                .map((column) => column.trim())
                .filter(Boolean);
              onChange({ ...element, columns });
              setEditingOptions(false);
            }}
          />
        ) : (
          <div
            className="canvas-element-table-preview canvas-element-interactive"
            role="button"
            tabIndex={0}
            title="Editar columnas"
            onClick={() => setEditingOptions(true)}
          >
            <table>
              <thead>
                <tr>
                  {(element.columns && element.columns.length > 0 ? element.columns : ["Definir columnas..."]).map((column, index) => (
                    <th key={index}>{column}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                <tr>
                  {(element.columns && element.columns.length > 0 ? element.columns : [""]).map((_, index) => (
                    <td key={index} />
                  ))}
                </tr>
              </tbody>
            </table>
          </div>
        ))}
      {element.type === "file" && (
        <div className="canvas-element-file-preview">
          <IconPaperclip size={14} aria-hidden="true" />
          Adjuntar archivo (PDF, JPG, PNG)
        </div>
      )}
      {element.type === "odontogram" && (
        <div className="canvas-element-file-preview">
          <IconDental size={14} aria-hidden="true" />
          Odontograma (32 piezas, notación FDI)
        </div>
      )}
      {element.type === "clinic_header" && (
        <div className="canvas-element-file-preview">
          <IconPhoto size={14} aria-hidden="true" />
          Logotipo y datos de la clínica (automático)
        </div>
      )}
      {(element.type === "signature_patient" || element.type === "signature_doctor") && (
        <div className="canvas-element-file-preview">
          <IconSignature size={14} aria-hidden="true" />
          {element.type === "signature_patient" ? "Firma del paciente" : "Firma del médico"}
        </div>
      )}
      <span
        className="canvas-element-resize-handle"
        onPointerDown={handleResizePointerDown}
        onPointerMove={handleResizePointerMove}
        onPointerUp={handleResizePointerUp}
      />
    </div>
  );
}
