import { genPageId, type TemplateElement, type TemplatePage } from "../types/clinicalRecords";
import { buildNomStarterPages, defaultSizeForType, CANVAS_WIDTH, CANVAS_PAGE_HEIGHT, ELEMENT_MARGIN } from "./nomHistoryTemplate";

// Encabezado con logotipo y datos de la clínica/odontólogo: ocupa la banda superior de la
// ficha de identificación (única hoja donde la norma exige mostrar estos datos) y se reserva
// espacio equivalente en el pie de página para su contraparte del lado derecho.
const HEADER_HEIGHT = 90;
const HEADER_GAP = 18;
const HEADER_OFFSET = HEADER_HEIGHT + HEADER_GAP;

const GUTTER = 20;
const ROW_GAP = 14;
const FULL_WIDTH = CANVAS_WIDTH - ELEMENT_MARGIN * 2;

interface DentalField {
  id: string;
  label: string;
  type: TemplateElement["type"];
}

const DENTAL_FIELDS: DentalField[] = [
  { id: "antecedentesOdontologicos", label: "Antecedentes odontológicos (última visita, motivo, hábitos de higiene bucal)", type: "textarea" },
  { id: "exploracionTejidosBlandos", label: "Exploración de tejidos blandos (mucosa, lengua, paladar, encías)", type: "textarea" },
  { id: "diagnosticoDental", label: "Diagnóstico", type: "textarea" },
  { id: "planTratamientoDental", label: "Plan de tratamiento", type: "textarea" },
  { id: "pronosticoDental", label: "Pronóstico", type: "textarea" }
];

function withHeaderSpace(page: TemplatePage): TemplatePage {
  return {
    ...page,
    elements: [
      ...page.elements,
      {
        id: "clinicHeaderTop",
        label: "Logotipo y datos de la clínica",
        type: "clinic_header" as const,
        x: ELEMENT_MARGIN,
        y: ELEMENT_MARGIN,
        width: 320,
        height: HEADER_HEIGHT
      },
      {
        id: "clinicHeaderFooter",
        label: "Logotipo y datos de la clínica",
        type: "clinic_header" as const,
        x: CANVAS_WIDTH - ELEMENT_MARGIN - 320,
        y: page.canvasHeight - ELEMENT_MARGIN - HEADER_HEIGHT,
        width: 320,
        height: HEADER_HEIGHT
      }
    ]
  };
}

function buildDentalPage(): TemplatePage {
  const elements: TemplateElement[] = [];
  let cursorY = ELEMENT_MARGIN;

  DENTAL_FIELDS.forEach((field) => {
    const height = 90;
    elements.push({
      id: field.id,
      label: field.label,
      type: field.type,
      x: ELEMENT_MARGIN,
      y: cursorY,
      width: FULL_WIDTH,
      height
    });
    cursorY += height + ROW_GAP;
  });

  const odontogramHeight = 320;
  elements.push({
    id: "odontogramaDental",
    label: "Odontograma",
    type: "odontogram",
    x: ELEMENT_MARGIN,
    y: cursorY,
    width: 780,
    height: odontogramHeight
  });
  cursorY += odontogramHeight + ROW_GAP + 10;

  const signatureSize = defaultSizeForType("signature_patient");
  const signatureGap = GUTTER;
  elements.push({
    id: "firmaPaciente",
    label: "Firma del paciente",
    type: "signature_patient",
    x: ELEMENT_MARGIN,
    y: cursorY,
    width: signatureSize.width,
    height: signatureSize.height
  });
  elements.push({
    id: "firmaMedico",
    label: "Firma del médico",
    type: "signature_doctor",
    x: ELEMENT_MARGIN + signatureSize.width + signatureGap,
    y: cursorY,
    width: signatureSize.width,
    height: signatureSize.height
  });

  return { id: genPageId(), elements, canvasHeight: CANVAS_PAGE_HEIGHT };
}

/**
 * Plantilla de historia clínica odontológica: parte de la misma base NOM-004 (ficha de
 * identificación, antecedentes, exploración física) y agrega el odontograma, la exploración
 * de tejidos blandos, diagnóstico/plan de tratamiento dental y las firmas, conforme a la
 * NOM-013-SSA2-2015. El logotipo y los datos de la clínica solo se colocan en la primera hoja.
 */
export function buildNomOdontologyStarterPages(): TemplatePage[] {
  const basePages = buildNomStarterPages({ firstPageTopOffset: HEADER_OFFSET, firstPageBottomReserve: HEADER_OFFSET });
  const [firstPage, ...restPages] = basePages;
  const pages = firstPage ? [withHeaderSpace(firstPage), ...restPages] : basePages;
  return [...pages, buildDentalPage()];
}
