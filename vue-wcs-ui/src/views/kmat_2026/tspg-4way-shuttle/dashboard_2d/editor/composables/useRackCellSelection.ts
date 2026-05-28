import { reactive, ref } from 'vue';
import { rectToCells, pointToCell, type RackGridBox } from '../utils/rackCellGeom';

export function useRackCellSelection() {
  const isDragging = ref(false);
  const dragRect = reactive({ x: 0, y: 0, w: 0, h: 0 });
  let startX = 0;
  let startY = 0;

  function start(px: number, py: number) {
    isDragging.value = true;
    startX = px;
    startY = py;
    dragRect.x = px;
    dragRect.y = py;
    dragRect.w = 0;
    dragRect.h = 0;
  }

  function update(px: number, py: number) {
    if (!isDragging.value) return;
    dragRect.x = Math.min(px, startX);
    dragRect.y = Math.min(py, startY);
    dragRect.w = Math.abs(px - startX);
    dragRect.h = Math.abs(py - startY);
  }

  function end(box: RackGridBox, level: number): string[] {
    if (!isDragging.value) return [];
    isDragging.value = false;
    if (dragRect.w < 2 && dragRect.h < 2) {
      const hit = pointToCell(box, level, dragRect.x, dragRect.y);
      return hit ? [hit.cellId] : [];
    }
    return rectToCells(box, level, dragRect);
  }

  return { isDragging, dragRect, start, update, end };
}
