import { jsPDF } from "jspdf";
import {
  LOWER_ARCH,
  SURFACE_CONDITION_COLORS,
  UPPER_ARCH,
  WHOLE_CONDITION_BADGES,
  emptyTooth,
  parseOdontogram
} from "../constants/odontogram";
import {
  DEFAULT_FONT_FAMILY,
  DEFAULT_FONT_SIZE,
  DEFAULT_TEXT_ALIGN,
  DEFAULT_TEXT_COLOR,
  fontPdfFamily,
  parseAttachments,
  parseTableRows,
  type TemplateElement,
  type TemplatePage
} from "../types/clinicalRecords";

const PAGE_WIDTH_MM = 216;
const PAGE_HEIGHT_MM = 279;
const MARGIN_MM = 14;
const CONTENT_TOP_MM = 22;

export interface ClinicHeaderInfo {
  name: string;
  legalName?: string;
  addressLine?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
}

interface ResolvedClinicHeaderInfo extends ClinicHeaderInfo {
  logoDataUrl?: string;
}

function loadImageAsDataUrl(url: string): Promise<string | null> {
  if (url.startsWith("data:")) return Promise.resolve(url);
  return new Promise((resolve) => {
    const image = new Image();
    image.crossOrigin = "anonymous";
    image.onload = () => {
      try {
        const canvas = document.createElement("canvas");
        canvas.width = image.naturalWidth;
        canvas.height = image.naturalHeight;
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          resolve(null);
          return;
        }
        ctx.drawImage(image, 0, 0);
        resolve(canvas.toDataURL("image/png"));
      } catch {
        resolve(null);
      }
    };
    image.onerror = () => resolve(null);
    image.src = url;
  });
}

function pxToMm(px: number, canvasWidthPx: number) {
  const usableWidth = PAGE_WIDTH_MM - MARGIN_MM * 2;
  return (px / canvasWidthPx) * usableWidth;
}

function hexToRgb(hex: string): [number, number, number] {
  const match = /^#?([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$/i.exec(hex);
  if (!match) return [26, 26, 26];
  return [parseInt(match[1], 16), parseInt(match[2], 16), parseInt(match[3], 16)];
}

function drawElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, value: string | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(20, pxToMm(element.width, canvasWidthPx));
  const heightMm = Math.max(8, pxToMm(element.height, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  const fontFamily = fontPdfFamily(element.fontFamily ?? DEFAULT_FONT_FAMILY);
  const fontSize = element.fontSize ?? DEFAULT_FONT_SIZE;
  const align = element.align ?? DEFAULT_TEXT_ALIGN;
  const [r, g, b] = hexToRgb(element.color ?? DEFAULT_TEXT_COLOR);
  const alignX = align === "left" ? x : align === "center" ? x + widthMm / 2 : x + widthMm;

  if (element.backgroundColor) {
    const [bgR, bgG, bgB] = hexToRgb(element.backgroundColor);
    doc.setFillColor(bgR, bgG, bgB);
    doc.rect(x, top, widthMm, heightMm, "F");
  }

  doc.setTextColor(r, g, b);
  doc.setFont(fontFamily, "bold");
  doc.setFontSize(Math.max(7, fontSize * 0.7));
  doc.text(element.label, alignX, top + 4, { align });

  doc.setFont(fontFamily, element.bold ? "bold" : "normal");
  doc.setFontSize(fontSize);
  const text = value && value.trim() ? value : "—";
  const lines = doc.splitTextToSize(text, widthMm);
  doc.text(lines, alignX, top + 8.5, { align });
}

function drawTableElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, value: string | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(20, pxToMm(element.width, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  const columns = element.columns ?? [];
  if (columns.length === 0) return;

  const fontFamily = fontPdfFamily(element.fontFamily ?? DEFAULT_FONT_FAMILY);
  const [r, g, b] = hexToRgb(element.color ?? DEFAULT_TEXT_COLOR);

  doc.setTextColor(r, g, b);
  doc.setFont(fontFamily, "bold");
  doc.setFontSize(Math.max(7, (element.fontSize ?? DEFAULT_FONT_SIZE) * 0.7));
  doc.text(element.label, x, top, { align: "left" });

  const rows = parseTableRows(value, columns.length);
  const colWidth = widthMm / columns.length;
  const rowHeight = 6;
  let cursorY = top + 4;

  doc.setFontSize(8);
  doc.setDrawColor(180);

  const drawRow = (cells: string[], bold: boolean) => {
    doc.setFont(fontFamily, bold ? "bold" : "normal");
    cells.forEach((cell, index) => {
      const cellX = x + index * colWidth;
      doc.rect(cellX, cursorY, colWidth, rowHeight);
      const lines = doc.splitTextToSize(cell || "", colWidth - 2);
      doc.text(lines[0] ?? "", cellX + 1, cursorY + 4);
    });
    cursorY += rowHeight;
  };

  drawRow(columns, true);
  rows.forEach((row) => {
    if (cursorY > PAGE_HEIGHT_MM - MARGIN_MM) return;
    drawRow(columns.map((_, index) => row[index] ?? ""), false);
  });
}

function drawFileElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, value: string | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(20, pxToMm(element.width, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  const fontFamily = fontPdfFamily(element.fontFamily ?? DEFAULT_FONT_FAMILY);
  const [r, g, b] = hexToRgb(element.color ?? DEFAULT_TEXT_COLOR);

  doc.setTextColor(r, g, b);
  doc.setFont(fontFamily, "bold");
  doc.setFontSize(Math.max(7, (element.fontSize ?? DEFAULT_FONT_SIZE) * 0.7));
  doc.text(element.label, x, top, { align: "left" });

  const attachments = parseAttachments(value);
  doc.setFont(fontFamily, "normal");
  doc.setFontSize(9);
  let cursorY = top + 5;
  if (attachments.length === 0) {
    doc.text("Sin archivos adjuntos.", x, cursorY);
    return;
  }
  attachments.forEach((attachment) => {
    if (cursorY > PAGE_HEIGHT_MM - MARGIN_MM) return;
    doc.text(`• ${attachment.originalFilename}`, x, cursorY);
    cursorY += 5;
  });
}

function drawOdontogramElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, value: string | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(60, pxToMm(element.width, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  const fontFamily = fontPdfFamily(element.fontFamily ?? DEFAULT_FONT_FAMILY);
  const [r, g, b] = hexToRgb(element.color ?? DEFAULT_TEXT_COLOR);

  doc.setTextColor(r, g, b);
  doc.setFont(fontFamily, "bold");
  doc.setFontSize(Math.max(7, (element.fontSize ?? DEFAULT_FONT_SIZE) * 0.7));
  doc.text(element.label, x, top, { align: "left" });

  const data = parseOdontogram(value);
  const toothSize = Math.min(9, widthMm / 16);
  let cursorY = top + 4;

  const drawArch = (arch: number[]) => {
    if (cursorY + toothSize + 3 > PAGE_HEIGHT_MM - MARGIN_MM) return;

    arch.forEach((tooth, index) => {
      const tx = x + index * toothSize;
      const record = data.teeth[String(tooth)] ?? emptyTooth();
      const half = toothSize / 2;
      const cx = tx + half;
      const cy = cursorY + half;
      const corners: Record<"tl" | "tr" | "bl" | "br", [number, number]> = {
        tl: [tx, cursorY],
        tr: [tx + toothSize, cursorY],
        bl: [tx, cursorY + toothSize],
        br: [tx + toothSize, cursorY + toothSize]
      };

      doc.setDrawColor(200);
      const fillTriangle = (p1: [number, number], p2: [number, number], surface: "V" | "D" | "M" | "L") => {
        const code = record.surfaces[surface] ?? "sano";
        const colorHex = SURFACE_CONDITION_COLORS[code];
        if (colorHex === "transparent") {
          doc.triangle(p1[0], p1[1], p2[0], p2[1], cx, cy, "S");
          return;
        }
        const [fr, fg, fb] = hexToRgb(colorHex);
        doc.setFillColor(fr, fg, fb);
        doc.triangle(p1[0], p1[1], p2[0], p2[1], cx, cy, "FD");
      };

      fillTriangle(corners.tl, corners.tr, "V");
      fillTriangle(corners.tr, corners.br, "D");
      fillTriangle(corners.br, corners.bl, "L");
      fillTriangle(corners.bl, corners.tl, "M");

      const oclusalCode = record.surfaces.O ?? "sano";
      const oSize = toothSize * 0.32;
      if (oclusalCode !== "sano") {
        const [orr, og, ob] = hexToRgb(SURFACE_CONDITION_COLORS[oclusalCode]);
        doc.setFillColor(orr, og, ob);
        doc.rect(cx - oSize / 2, cy - oSize / 2, oSize, oSize, "FD");
      } else {
        doc.rect(cx - oSize / 2, cy - oSize / 2, oSize, oSize, "S");
      }

      doc.rect(tx, cursorY, toothSize, toothSize, "S");

      if (record.whole === "ausente") {
        doc.setDrawColor(107, 114, 128);
        doc.line(tx + 0.5, cursorY + 0.5, tx + toothSize - 0.5, cursorY + toothSize - 0.5);
        doc.line(tx + toothSize - 0.5, cursorY + 0.5, tx + 0.5, cursorY + toothSize - 0.5);
      } else if (record.whole === "extraccion_indicada") {
        doc.setDrawColor(214, 69, 69);
        doc.line(tx + 0.5, cursorY + 0.5, tx + toothSize - 0.5, cursorY + toothSize - 0.5);
      } else if (record.whole === "corona") {
        doc.setDrawColor(201, 151, 31);
        doc.rect(tx + 0.4, cursorY + 0.4, toothSize - 0.8, toothSize - 0.8, "S");
      } else if (record.whole === "fracturado") {
        doc.setDrawColor(224, 138, 44);
        doc.lines(
          [
            [toothSize * 0.25, -toothSize * 0.25],
            [toothSize * 0.25, toothSize * 0.4],
            [toothSize * 0.25, -toothSize * 0.4],
            [toothSize * 0.25, toothSize * 0.25]
          ],
          tx + 0.5,
          cursorY + toothSize * 0.5,
          [1, 1],
          "S"
        );
      }

      const badge = WHOLE_CONDITION_BADGES[record.whole];
      if (badge) {
        const badgeR = toothSize * 0.18;
        const badgeCx = tx + toothSize - badgeR;
        const badgeCy = cursorY + badgeR;
        const [bgR, bgG, bgB] = hexToRgb(badge.color);
        doc.setFillColor(bgR, bgG, bgB);
        doc.circle(badgeCx, badgeCy, badgeR, "F");
        doc.setTextColor(255, 255, 255);
        doc.setFont(fontFamily, "bold");
        doc.setFontSize(Math.max(3, badgeR * 1.3));
        doc.text(badge.text, badgeCx, badgeCy + badgeR * 0.35, { align: "center" });
      }

      doc.setTextColor(r, g, b);
      doc.setFont(fontFamily, "normal");
      doc.setFontSize(5);
      doc.text(String(tooth), cx, cursorY + toothSize + 2.5, { align: "center" });
    });

    cursorY += toothSize + 5;
  };

  drawArch(UPPER_ARCH);
  drawArch(LOWER_ARCH);
}

function drawClinicHeaderElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, info: ResolvedClinicHeaderInfo | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(30, pxToMm(element.width, canvasWidthPx));
  const heightMm = Math.max(15, pxToMm(element.height, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  if (!info) {
    doc.setTextColor(120, 120, 120);
    doc.setFont("helvetica", "italic");
    doc.setFontSize(8);
    doc.text(element.label, x, top + 4);
    return;
  }

  const logoSize = Math.min(heightMm, 18);
  let textX = x;
  if (info.logoDataUrl) {
    try {
      doc.addImage(info.logoDataUrl, "PNG", x, top, logoSize, logoSize);
      textX = x + logoSize + 3;
    } catch {
      // logotipo no disponible (formato no soportado o error de carga); se omite sin interrumpir el PDF
    }
  }

  const textWidth = Math.max(20, widthMm - (textX - x));
  const lines = [info.legalName || info.name, info.addressLine, info.phone ? `Tel. ${info.phone}` : undefined, info.email]
    .filter((line): line is string => Boolean(line));

  doc.setTextColor(26, 26, 26);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(9);
  doc.text(doc.splitTextToSize(lines[0] ?? "", textWidth), textX, top + 4);

  doc.setFont("helvetica", "normal");
  doc.setFontSize(7.5);
  let cursorY = top + 8.5;
  lines.slice(1).forEach((line) => {
    const wrapped = doc.splitTextToSize(line, textWidth);
    doc.text(wrapped, textX, cursorY);
    cursorY += 3.6 * wrapped.length;
  });
}

function drawSignatureElement(doc: jsPDF, element: TemplateElement, canvasWidthPx: number, value: string | undefined) {
  const x = MARGIN_MM + pxToMm(element.x, canvasWidthPx);
  const top = CONTENT_TOP_MM + pxToMm(element.y, canvasWidthPx);
  const widthMm = Math.max(30, pxToMm(element.width, canvasWidthPx));
  const heightMm = Math.max(20, pxToMm(element.height, canvasWidthPx));
  if (top > PAGE_HEIGHT_MM - MARGIN_MM) return;

  const lineY = top + heightMm - 8;
  if (value) {
    try {
      doc.addImage(value, "PNG", x, top, widthMm, heightMm - 12);
    } catch {
      // firma no disponible (dato inválido); se deja el recuadro en blanco para firma en tinta
    }
  }

  doc.setDrawColor(120, 120, 120);
  doc.line(x, lineY, x + widthMm, lineY);
  doc.setTextColor(80, 80, 80);
  doc.setFont("helvetica", "normal");
  doc.setFontSize(8);
  doc.text(element.label, x + widthMm / 2, lineY + 4, { align: "center" });
}

function drawPage(doc: jsPDF, title: string, pageNumber: number, totalPages: number, page: TemplatePage, canvasWidthPx: number, answers?: Record<string, string>, clinicInfo?: ResolvedClinicHeaderInfo) {
  doc.setTextColor(26, 26, 26);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(11);
  doc.text(title, MARGIN_MM, 10);
  doc.setFont("helvetica", "normal");
  doc.setFontSize(9);
  doc.text(`Página ${pageNumber} de ${totalPages}`, PAGE_WIDTH_MM - MARGIN_MM, 10, { align: "right" });
  doc.setDrawColor(200);
  doc.line(MARGIN_MM, 13, PAGE_WIDTH_MM - MARGIN_MM, 13);

  [...page.elements]
    .sort((a, b) => a.y - b.y || a.x - b.x)
    .forEach((element) => {
      const value = answers ? answers[element.id] : undefined;
      if (element.type === "table") {
        drawTableElement(doc, element, canvasWidthPx, value);
      } else if (element.type === "file") {
        drawFileElement(doc, element, canvasWidthPx, value);
      } else if (element.type === "odontogram") {
        drawOdontogramElement(doc, element, canvasWidthPx, value);
      } else if (element.type === "clinic_header") {
        drawClinicHeaderElement(doc, element, canvasWidthPx, clinicInfo);
      } else if (element.type === "signature_patient" || element.type === "signature_doctor") {
        drawSignatureElement(doc, element, canvasWidthPx, value);
      } else {
        drawElement(doc, element, canvasWidthPx, value);
      }
    });
}

export interface PdfExportRange {
  from?: number;
  to?: number;
}

export async function exportPagesToPdf(options: {
  title: string;
  pages: TemplatePage[];
  canvasWidthPx: number;
  answers?: Record<string, string>;
  range?: PdfExportRange;
  clinicInfo?: ClinicHeaderInfo;
}) {
  const { title, pages, canvasWidthPx, answers, range, clinicInfo } = options;
  const startIdx = Math.max(0, (range?.from ?? 1) - 1);
  const endIdx = Math.min(pages.length - 1, (range?.to ?? pages.length) - 1);
  const selected = pages.slice(startIdx, endIdx + 1);
  if (selected.length === 0) return;

  const resolvedClinicInfo: ResolvedClinicHeaderInfo | undefined = clinicInfo
    ? { ...clinicInfo, logoDataUrl: clinicInfo.logoUrl ? (await loadImageAsDataUrl(clinicInfo.logoUrl)) ?? undefined : undefined }
    : undefined;

  const doc = new jsPDF({ unit: "mm", format: "letter" });
  selected.forEach((page, index) => {
    if (index > 0) doc.addPage();
    drawPage(doc, title, startIdx + index + 1, pages.length, page, canvasWidthPx, answers, resolvedClinicInfo);
  });

  const safeName = title.trim().replace(/[^a-z0-9áéíóúñü_\- ]/gi, "").replace(/\s+/g, "_") || "documento";
  doc.save(`${safeName}.pdf`);
}
