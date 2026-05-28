<template>
  <div class="grid-wrap" :style="{ height }">
    <table class="grid-table">
      <colgroup>
        <col
          v-for="c in columns"
          :key="c.key"
          :style="{ width: c.width ? c.width + 'px' : 'auto' }"
        />
      </colgroup>

      <thead>
      <tr>
        <th v-for="c in columns" :key="c.key" :class="thClass(c)">
          {{ c.label }}
        </th>
      </tr>
      </thead>

      <tbody v-if="rows && rows.length">
      <tr
        v-for="(r, rowIndex) in rows"
        :key="r[rowKey]"
        :class="rowClass(r, rowIndex)"
        @click="selectRow(r)"
        @contextmenu.prevent="onContextMenu($event, r)"
        @dragover.prevent="onDragOver(rowIndex)"
        @drop.prevent="onDrop(rowIndex)"
      >
        <td v-for="c in columns" :key="c.key" :class="tdClass(c)">
          <template v-if="c.type === 'drag'">
              <span
                class="drag-handle"
                :class="{ disabled: !reorderable }"
                :draggable="reorderable"
                title="드래그로 순서 변경"
                @click.stop
                @dragstart="onDragStart($event, rowIndex)"
                @dragend="onDragEnd"
              >
                ≡
              </span>
          </template>

          <template v-else-if="c.type === 'run'">
            <div class="run-cell" @click.stop>
              <input
                v-if="showCheck"
                type="checkbox"
                class="run-check"
                :checked="isChecked(r)"
                @change="toggleChecked(r)"
              />

              <button class="run-btn" :disabled="isRunDisabled(r)" @click="emitRun(r)" title="실행">
                <svg v-if="r.status === 'RUNNING'" class="spin" viewBox="0 0 24 24" width="16" height="16">
                  <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="2" opacity="0.25"></circle>
                  <path d="M21 12a9 9 0 0 0-9-9" fill="none" stroke="currentColor" stroke-width="2"></path>
                </svg>

                <svg v-else-if="r.status === 'READY' || r.status === 'PAUSED'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M8 5v14l11-7z" />
                </svg>

                <svg v-else-if="r.status === 'COMPLETE' || r.status === 'ABORTED'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 6 9 17l-5-5" />
                </svg>
              </button>
            </div>
          </template>

          <template v-else-if="c.type === 'status'">
              <span class="pill" :class="pillClass(r.status)">
                {{ statusText(r.status) }}
              </span>
          </template>

          <template v-else-if="c.type === 'checkbox'">
            <div class="checkbox-cell">
              <input
                type="checkbox"
                class="grid-checkbox"
                v-model="r[c.key]"
                @click.stop
              />
            </div>
          </template>

          <template v-else-if="c.type === 'number-edit'">
            <input
              type="number"
              class="cell-input"
              v-model.number="r[c.key]"
              min="1"
              @click.stop
            />
          </template>

          <template v-else>
            {{ safeText(r[c.key]) }}
          </template>
        </td>
      </tr>
      </tbody>

      <tbody v-else>
      <tr>
        <td class="empty" :colspan="columns.length">데이터 없음</td>
      </tr>
      </tbody>
    </table>

    <div v-if="activeModalRow" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>작업 제어 확인</h3>
          <button class="close-x" @click="closeModal">×</button>
        </div>

        <div class="modal-body">
          <div class="info-box">
            <div class="info-item">
              <span class="label">컨테이너:</span>
              <span class="value">{{ activeModalRow.cntr_no }}</span>
            </div>
            <div class="info-item">
              <span class="label">B/L No:</span>
              <span class="value">{{ activeModalRow.bl_no }}</span>
            </div>
          </div>

          <p class="modal-msg">{{ modalMessage }}</p>
        </div>

        <div class="modal-footer">
          <template v-if="activeModalRow.status === 'RUNNING'">
            <button class="btn btn-abort" @click="handleAction('ABORT')">강제종료</button>
            <button class="btn btn-pause" @click="handleAction('PAUSE')">일시정지</button>
          </template>

          <template v-else>
            <button class="btn btn-primary" @click="handleAction('START')">실행 시작</button>
          </template>

          <button class="btn btn-ghost" @click="closeModal">취소</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

type Column = {
  key: string
  label: string
  width?: number
  align?: 'left' | 'center' | 'right'
  type?: 'drag' | 'run' | 'status' | 'number-edit'
}

const props = defineProps<{
  columns: Column[]
  rows: any[]
  rowKey: string
  height?: string

  selectedKey?: string | number | null
  checkedKey?: string | number | null
  runningKey?: string | number | null

  reorderable?: boolean
}>()

// ✅ rowContextMenu emit 추가
const emit = defineEmits<{
  (e: 'update:selectedKey', v: string | number | null): void
  (e: 'update:checkedKey', v: string | number | null): void
  (e: 'rowSelected', row: any): void
  (e: 'run', row: any): void
  (e: 'reorder', v: { from: number; to: number }): void
  (e: 'rowContextMenu', row: any, event: MouseEvent): void
}>()

const height = props.height ?? '360px'
const reorderable = computed(() => !!props.reorderable)
const showCheck = computed(() => props.checkedKey !== undefined)

const dragFrom = ref<number | null>(null)
const dragOver = ref<number | null>(null)

const activeModalRow = ref<any>(null);

const modalMessage = computed(() => {
  if (!activeModalRow.value) return '';
  const s = activeModalRow.value.status;
  if (s === 'RUNNING') return '해당 컨테이너의 작업을 어떻게 변경하시겠습니까?';
  return '해당 컨테이너의 작업을 시작하시겠습니까?';
});

function keyOf(row: any) {
  return row?.[props.rowKey]
}

function selectRow(row: any) {
  const k = keyOf(row)
  emit('update:selectedKey', k)
  emit('rowSelected', row)
}

// ✅ 우클릭 이벤트 발생 시 부모로 전달
function onContextMenu(e: MouseEvent, row: any) {
  emit('rowContextMenu', row, e)
}

function isSelected(row: any) {
  return props.selectedKey != null && String(props.selectedKey) === String(keyOf(row))
}

function isChecked(row: any) {
  if (!showCheck.value) return false
  return props.checkedKey != null && String(props.checkedKey) === String(keyOf(row))
}

function toggleChecked(row: any) {
  if (!showCheck.value) return
  const k = keyOf(row)
  const next = isChecked(row) ? null : k
  emit('update:checkedKey', next)
}

function emitRun(row: any) {
  activeModalRow.value = row;
}

function closeModal() {
  activeModalRow.value = null;
}

function handleAction(action: 'START' | 'ABORT' | 'PAUSE') {
  emit('run', { row: activeModalRow.value, action });
  closeModal();
}

function isRunDisabled(row: any) {
  const running = props.runningKey
  if (row.status === 'COMPLETE') return true
  if (row.status === 'ABORTED') return true
  if (running && String(running) !== String(keyOf(row))) return true
  return false
}

function statusText(s: string) {
  if (s === 'READY') return '대기'
  if (s === 'RUNNING') return '실행중'
  if (s === 'COMPLETE') return '완료'
  if (s === 'PAUSED') return '일시정지'
  if (s === 'ABORTED') return '강제종료'
  return '대기'
}

function pillClass(s: string) {
  if (s === 'READY') return 'pill-ready'
  if (s === 'RUNNING') return 'pill-running'
  if (s === 'COMPLETE') return 'pill-done'
  if (s === 'PAUSED') return 'pill-paused'
  if (s === 'ABORTED') return 'pill-aborted'
  return 'pill-idle'
}

function thClass(c: Column) {
  return ['th', c.align ? `a-${c.align}` : 'a-left']
}
function tdClass(c: Column) {
  return ['td', c.align ? `a-${c.align}` : 'a-left']
}
function rowClass(r: any, idx: number) {
  return ['tr', isSelected(r) ? 'is-selected' : '', reorderable.value && dragOver.value === idx ? 'is-drag-over' : '']
}

function safeText(v: any) {
  if (v === null || v === undefined || v === '') return '-'
  return String(v)
}

/** ✅ Drag & Drop reorder */
function onDragStart(e: DragEvent, fromIdx: number) {
  if (!reorderable.value) return
  dragFrom.value = fromIdx
  dragOver.value = fromIdx
  try {
    e.dataTransfer?.setData('text/plain', String(fromIdx))
    if (e.dataTransfer) e.dataTransfer.effectAllowed = 'move'
  } catch {}
}
function onDragOver(toIdx: number) {
  if (!reorderable.value) return
  dragOver.value = toIdx
}
function onDrop(toIdx: number) {
  if (!reorderable.value) return
  const fromIdx = dragFrom.value
  if (fromIdx === null || fromIdx === toIdx) {
    onDragEnd()
    return
  }
  emit('reorder', { from: fromIdx, to: toIdx })
  onDragEnd()
}
function onDragEnd() {
  dragFrom.value = null
  dragOver.value = null
}
</script>

<style scoped lang="less">
.grid-wrap {
  width: 100%;
  overflow: auto;
  border-radius: 12px;
  border: 1px solid #eef2f7;
  background: #fff;
}

.grid-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
}

thead th {
  position: sticky;
  top: 0;
  z-index: 2;
  background: #f8fafc;
  color: #334155;
  font-weight: 600;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
  padding: 10px 10px;
  white-space: nowrap;
}

tbody td {
  border-bottom: 1px solid #f1f5f9;
  padding: 8px 10px;
  color: #0f172a;
  white-space: nowrap;
}

.tr:hover td {
  background: #f9fafb;
}

.is-selected td {
  background: fade(#5b61f6, 10%);
}

.is-drag-over td {
  outline: 2px dashed fade(#5b61f6, 35%);
  outline-offset: -2px;
}

.empty {
  text-align: center;
  color: #64748b;
  padding: 20px;
}

/* align */
.a-left { text-align: left; }
.a-center { text-align: center; }
.a-right { text-align: right; }

/* run cell */
.run-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.run-check {
  width: 16px;
  height: 16px;
}

.run-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: pointer;
}
.run-btn:hover { background: #f9fafb; }
.run-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* status pill */
.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}
.pill-idle, .pill-ready {
  color: #475569;
  background: #f1f5f9;
  border-color: #e2e8f0;
}
.pill-running {
  color: #0369a1;
  background: #e0f2fe;
  border-color: #bae6fd;
}
.pill-done {
  color: #047857;
  background: #dcfce7;
  border-color: #bbf7d0;
}
.pill-paused {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}
.pill-aborted {
  color: #be123c;
  background: #fff1f2;
  border-color: #fecdd3;
}

/* editable cell */
.cell-input {
  width: 90px;
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  font-size: 13px;
}

/* drag handle */
.drag-handle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  cursor: grab;
  user-select: none;
}
.drag-handle:active { cursor: grabbing; }
.drag-handle.disabled { opacity: .4; cursor: not-allowed; }

.modal-overlay {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(15, 23, 42, 0.5);
  display: flex; align-items: center; justify-content: center; z-index: 9999;
}

.modal-content {
  background: #fff; width: 400px; border-radius: 16px;
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04);
  overflow: hidden; animation: modal-up 0.2s ease-out;
}

@keyframes modal-up {
  from { transform: translateY(10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-header {
  padding: 16px 20px; border-bottom: 1px solid #f1f5f9;
  display: flex; justify-content: space-between; align-items: center;
  h3 { font-size: 16px; font-weight: 700; color: #1e293b; margin: 0; }
  .close-x { border: none; background: none; font-size: 24px; color: #94a3b8; cursor: pointer; }
}

.modal-body {
  padding: 20px;
  .info-box {
    background: #f8fafc; border-radius: 12px; padding: 12px; margin-bottom: 16px;
    .info-item {
      display: flex; gap: 8px; font-size: 14px; margin-bottom: 4px;
      .label { color: #64748b; width: 70px; }
      .value { color: #0f172a; font-weight: 600; }
    }
  }
  .modal-msg { font-size: 15px; color: #334155; line-height: 1.5; margin: 0; }
}

.modal-footer {
  padding: 16px 20px; background: #f8fafc; display: flex; gap: 8px; justify-content: flex-end;
  .btn {
    padding: 8px 16px; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; border: 1px solid transparent;
  }
  .btn-primary { background: #5b61f6; color: #fff; }
  .btn-abort { background: #fff1f2; color: #be123c; border-color: #fecdd3; }
  .btn-pause { background: #fffbeb; color: #b45309; border-color: #fde68a; }
  .btn-ghost { background: #fff; color: #64748b; border-color: #e2e8f0; }
}

.td.a-center { text-align: center; }
.td.a-right { text-align: right; }

.checkbox-cell {
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
}

.td.a-center .checkbox-cell { justify-content: center; }
.td.a-right .checkbox-cell { justify-content: flex-end; }
.td.a-left .checkbox-cell { justify-content: flex-start; }

.grid-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #5b61f6;
}
</style>
