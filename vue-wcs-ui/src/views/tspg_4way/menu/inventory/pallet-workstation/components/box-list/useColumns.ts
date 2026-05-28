// 박스 테이블 컬럼 너비/리사이즈/persistence.

import { computed, ref } from 'vue';
import {
  COL_STORAGE_KEY,
  DEFAULT_COLUMNS,
  loadColumnWidths,
  persistColumnWidths,
  type ColDef,
} from '../../shared';

export function useColumns() {
  const columns = ref<ColDef[]>(loadColumnWidths());
  const tableTotalWidth = computed(() => columns.value.reduce((sum, c) => sum + c.width, 0));

  const resizingColKey = ref<string | null>(null);
  let resizeStartX = 0;
  let resizeStartWidth = 0;

  function startColResize(e: MouseEvent, colKey: string) {
    const col = columns.value.find((c) => c.key === colKey);
    if (!col) return;
    resizingColKey.value = colKey;
    resizeStartX = e.clientX;
    resizeStartWidth = col.width;
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
    window.addEventListener('mousemove', onColResizeMove);
    window.addEventListener('mouseup', onColResizeUp);
  }
  function onColResizeMove(e: MouseEvent) {
    if (!resizingColKey.value) return;
    const col = columns.value.find((c) => c.key === resizingColKey.value);
    if (!col) return;
    const dx = e.clientX - resizeStartX;
    col.width = Math.max(col.minWidth, resizeStartWidth + dx);
  }
  function onColResizeUp() {
    if (!resizingColKey.value) return;
    resizingColKey.value = null;
    document.body.style.cursor = '';
    document.body.style.userSelect = '';
    window.removeEventListener('mousemove', onColResizeMove);
    window.removeEventListener('mouseup', onColResizeUp);
    persistColumnWidths(columns.value);
  }
  function autoFitColumn(colKey: string) {
    const def = DEFAULT_COLUMNS.find((c) => c.key === colKey);
    const cur = columns.value.find((c) => c.key === colKey);
    if (def && cur) {
      cur.width = def.width;
      persistColumnWidths(columns.value);
    }
  }
  function resetColumnWidths() {
    columns.value = DEFAULT_COLUMNS.map((c) => ({ ...c }));
    try {
      localStorage.removeItem(COL_STORAGE_KEY);
    } catch (_) {
      /* empty */
    }
  }

  return {
    columns,
    tableTotalWidth,
    resizingColKey,
    startColResize,
    onColResizeMove,
    onColResizeUp,
    autoFitColumn,
    resetColumnWidths,
  };
}
