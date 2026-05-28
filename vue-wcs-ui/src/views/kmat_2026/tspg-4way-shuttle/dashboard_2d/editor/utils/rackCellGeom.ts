// 랙 셀 격자 좌표 매핑 유틸
// cellId 규칙: {level}{row:02d}{bay:02d} (예: 10601)

export interface RackGridBox {
  x: number;
  y: number;
  width: number;
  height: number;
  rows: number;
  bays: number;
}

export interface CellRect {
  cellId: string;
  row: number;
  bay: number;
  x: number;
  y: number;
  w: number;
  h: number;
}

export function makeCellId(level: number, row: number, bay: number): string {
  return `${level}${String(row).padStart(2, '0')}${String(bay).padStart(2, '0')}`;
}

export function computeCellRects(box: RackGridBox, level: number): CellRect[] {
  const cellW = box.width / box.bays;
  const cellH = box.height / box.rows;
  const rects: CellRect[] = [];
  for (let r = 1; r <= box.rows; r++) {
    for (let b = 1; b <= box.bays; b++) {
      rects.push({
        cellId: makeCellId(level, r, b),
        row: r,
        bay: b,
        x: box.x + (b - 1) * cellW,
        y: box.y + (r - 1) * cellH,
        w: cellW,
        h: cellH,
      });
    }
  }
  return rects;
}

export function pointToCell(
  box: RackGridBox,
  level: number,
  px: number,
  py: number,
): { row: number; bay: number; cellId: string } | null {
  if (px < box.x || px >= box.x + box.width || py < box.y || py >= box.y + box.height) {
    return null;
  }
  const bay = Math.floor((px - box.x) / (box.width / box.bays)) + 1;
  const row = Math.floor((py - box.y) / (box.height / box.rows)) + 1;
  return { row, bay, cellId: makeCellId(level, row, bay) };
}

export function rectToCells(
  box: RackGridBox,
  level: number,
  dragRect: { x: number; y: number; w: number; h: number },
): string[] {
  const cellW = box.width / box.bays;
  const cellH = box.height / box.rows;
  const x1 = dragRect.x;
  const y1 = dragRect.y;
  const x2 = dragRect.x + dragRect.w;
  const y2 = dragRect.y + dragRect.h;
  const bayStart = Math.max(1, Math.floor((Math.min(x1, x2) - box.x) / cellW) + 1);
  const bayEnd = Math.min(box.bays, Math.floor((Math.max(x1, x2) - box.x) / cellW) + 1);
  const rowStart = Math.max(1, Math.floor((Math.min(y1, y2) - box.y) / cellH) + 1);
  const rowEnd = Math.min(box.rows, Math.floor((Math.max(y1, y2) - box.y) / cellH) + 1);
  const ids: string[] = [];
  for (let r = rowStart; r <= rowEnd; r++) {
    for (let b = bayStart; b <= bayEnd; b++) {
      ids.push(makeCellId(level, r, b));
    }
  }
  return ids;
}
