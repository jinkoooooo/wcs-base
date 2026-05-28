<template>
  <div ref="wrapRef" class="grid-wrap" :style="{ height }" @wheel="onWheel">
    <table class="grid-table" :class="{ resizing: isResizing }">
      <colgroup>
        <col v-for="c in columns" :key="c.key" :style="{ width: getColWidth(c.key) + 'px' }" />
      </colgroup>

      <thead>
        <tr>
          <th v-for="c in columns" :key="c.key" :style="{ textAlign: c.align ?? 'left' }">
            <!-- ✅ header slot: header-컬럼키 -->
            <template v-if="$slots[`header-${c.key}`]">
              <slot :name="`header-${c.key}`" :column="c"></slot>
            </template>
            <template v-else>
              {{ c.label }}
            </template>

            <!-- resize handle (컨트롤 컬럼은 제외 권장) -->
            <span
              v-if="c.key !== '__ctrl'"
              class="col-resizer"
              @mousedown="onResizeStart($event, c.key)"
              title="드래그해서 컬럼 너비 조절"
            ></span>
          </th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="r in rows"
          :key="String(r[rowKey] ?? '')"
          :class="{ selected: String(r[rowKey] ?? '') === String(selectedKey ?? '') }"
          @click="selectRow(r)"
          @dblclick="onRowDblClick($event, r)"
        >
          <td v-for="c in columns" :key="c.key" :style="{ textAlign: c.align ?? 'left' }">
            <!-- ✅ cell slot: cell-컬럼키 -->
            <template v-if="$slots[`cell-${c.key}`]">
              <slot :name="`cell-${c.key}`" :row="r" :column="c" :value="r[c.key]"></slot>
            </template>

            <!-- 기본 렌더 -->
            <template v-else>
              <!-- readonly -->
              <template v-if="c.editor === 'readonly'">
                <span class="cell-text">{{ safeText(r[c.key]) }}</span>
              </template>

              <!-- select -->
              <template v-else-if="c.editor === 'select'">
                <select class="grid-input" v-model="r[c.key]">
                  <option
                    v-for="opt in c.editorOptions?.items ?? []"
                    :key="String(opt.value)"
                    :value="opt.value"
                  >
                    {{ opt.label }}
                  </option>
                </select>
              </template>

              <!-- datetime minute -->
              <template v-else-if="c.editor === 'datetime-minute'">
                <DateTimeMinuteInput v-model="r[c.key]" />
              </template>

              <!-- date ymd -->
              <template v-else-if="c.editor === 'date-ymd'">
                <DateYmdInput v-model="r[c.key]" />
              </template>

              <!-- money -->
              <template v-else-if="c.editor === 'money'">
                <MoneyInput v-model="r[c.key]" />
              </template>

              <!-- default text -->
              <template v-else>
                <input class="grid-input" v-model="r[c.key]" />
              </template>
            </template>
          </td>
        </tr>

        <tr v-if="!rows || rows.length === 0">
          <td :colspan="columns.length" class="empty">데이터가 없습니다.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, ref, watch } from 'vue';
  import DateTimeMinuteInput from './DateTimeMinuteInput.vue';
  import DateYmdInput from './DateYmdInput.vue';
  import MoneyInput from './MoneyInput.vue';

  type SelectItem = { value: any; label: string };
  type Column = {
    key: string;
    label: string;
    width?: number;
    align?: 'left' | 'center' | 'right';
    editor?: 'readonly' | 'text' | 'select' | 'datetime-minute' | 'date-ymd' | 'money';
    editorOptions?: { items?: SelectItem[] };
  };

  const props = defineProps<{
    columns: Column[];
    rows: any[];
    rowKey: string;
    height: string;
    selectedKey?: string | null;
  }>();

  const emit = defineEmits<{
    (e: 'update:selectedKey', v: string | null): void;
    (e: 'row-dblclick', row: any): void;
  }>();

  const selectedKey = computed(() => props.selectedKey ?? null);

  function selectRow(row: any) {
    const v = String(row?.[props.rowKey] ?? '');
    emit('update:selectedKey', v || null);
  }

  function onRowDblClick(e: MouseEvent, row: any) {
    const tag = (e.target as HTMLElement)?.tagName?.toUpperCase?.() ?? '';
    if (['INPUT', 'SELECT', 'TEXTAREA', 'BUTTON', 'LABEL'].includes(tag)) return;
    emit('row-dblclick', row);
  }

  function safeText(v: any) {
    if (v == null) return '';
    return String(v);
  }

  /* Ctrl + Wheel => Horizontal Scroll */
  const wrapRef = ref<HTMLElement | null>(null);

  function onWheel(e: WheelEvent) {
    if (!e.ctrlKey) return;
    const el = wrapRef.value;
    if (!el) return;
    e.preventDefault();
    const delta = Math.abs(e.deltaX) > Math.abs(e.deltaY) ? e.deltaX : e.deltaY;
    el.scrollLeft += delta;
  }

  /* Column Width State */
  const DEFAULT_COL_WIDTH = 120;
  const MIN_COL_WIDTH = 60;

  const colWidths = ref<Record<string, number>>({});
  const touched = ref(new Set<string>());

  function initWidthsFromProps() {
    for (const c of props.columns) {
      const key = c.key;
      if (touched.value.has(key)) continue;

      if (typeof c.width === 'number' && c.width > 0) {
        colWidths.value[key] = c.width;
        continue;
      }
      if (colWidths.value[key] == null) colWidths.value[key] = DEFAULT_COL_WIDTH;
    }
  }

  function getColWidth(key: string) {
    const w = colWidths.value[key];
    return typeof w === 'number' && w > 0 ? w : DEFAULT_COL_WIDTH;
  }

  watch(
    () => props.columns,
    () => initWidthsFromProps(),
    { deep: true, immediate: true },
  );

  /* Resize Drag Logic */
  const isResizing = ref(false);
  const resizeState = ref<{ key: string; startX: number; startW: number } | null>(null);

  function onResizeStart(e: MouseEvent, key: string) {
    e.preventDefault();
    e.stopPropagation();

    touched.value.add(key);
    isResizing.value = true;

    resizeState.value = { key, startX: e.clientX, startW: getColWidth(key) };

    window.addEventListener('mousemove', onResizing);
    window.addEventListener('mouseup', onResizeEnd);
  }

  function onResizing(e: MouseEvent) {
    const st = resizeState.value;
    if (!st) return;
    const dx = e.clientX - st.startX;
    colWidths.value[st.key] = Math.max(MIN_COL_WIDTH, st.startW + dx);
  }

  function onResizeEnd() {
    isResizing.value = false;
    resizeState.value = null;
    window.removeEventListener('mousemove', onResizing);
    window.removeEventListener('mouseup', onResizeEnd);
  }

  onBeforeUnmount(() => {
    window.removeEventListener('mousemove', onResizing);
    window.removeEventListener('mouseup', onResizeEnd);
  });
</script>

<style scoped>
  .grid-wrap {
    overflow: auto;
    border: 1px solid #eef2f7;
    border-radius: 12px;
    background: #fff;
  }

  .grid-wrap::-webkit-scrollbar {
    height: 14px;
    width: 12px;
  }
  .grid-wrap::-webkit-scrollbar-thumb {
    background: rgba(100, 116, 139, 0.45);
    border-radius: 999px;
    border: 3px solid rgba(255, 255, 255, 0.9);
  }
  .grid-wrap::-webkit-scrollbar-track {
    background: rgba(15, 23, 42, 0.05);
    border-radius: 999px;
  }

  .grid-table {
    table-layout: fixed;
    width: max-content;
    min-width: 100%;
    border-collapse: separate;
    border-spacing: 0;
    font-size: 12px;
  }

  thead th {
    position: sticky;
    top: 0;
    z-index: 1;
    background: #f8fafc;
    border-bottom: 1px solid #e5e7eb;
    padding: 8px 10px;
    white-space: nowrap;
    position: relative;
  }

  .col-resizer {
    position: absolute;
    top: 0;
    right: 0;
    width: 10px;
    height: 100%;
    cursor: col-resize;
    user-select: none;
  }

  .col-resizer::after {
    content: '';
    position: absolute;
    top: 6px;
    bottom: 6px;
    left: 50%;
    width: 2px;
    transform: translateX(-50%);
    background: rgba(0, 0, 0, 0.24);
    border-radius: 2px;
  }

  .grid-table.resizing,
  .grid-table.resizing * {
    user-select: none !important;
    cursor: col-resize;
  }

  tbody td {
    border-bottom: 1px solid #f1f5f9;
    padding: 6px 8px;
    vertical-align: middle;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  tbody tr.selected {
    background: rgba(91, 97, 246, 0.06);
  }

  .grid-input {
    width: 100%;
    height: 30px;
    padding: 4px 8px;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
    outline: none;
    background: #fff;
    font-size: 12px;
    box-sizing: border-box;
  }

  .cell-text {
    display: inline-block;
    min-height: 30px;
    line-height: 30px;
    padding: 0 4px;
    color: #0f172a;
  }

  .empty {
    text-align: center;
    padding: 14px;
    color: #64748b;
  }
</style>
