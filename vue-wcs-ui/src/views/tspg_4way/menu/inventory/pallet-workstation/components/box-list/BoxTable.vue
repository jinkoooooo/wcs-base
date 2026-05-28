<template>
  <div class="box-table-wrap" :ref="boxTableWrapRef">
    <table class="box-table" :style="{ width: tableTotalWidth + 'px' }">
      <colgroup>
        <col v-for="col in columns" :key="col.key" :style="{ width: col.width + 'px' }" />
      </colgroup>
      <BoxTableHeader
        :columns="columns"
        :resizing-col-key="resizingColKey"
        :all-selected="allSelected"
        :some-selected="someSelected"
        @toggle-select-all="$emit('toggle-select-all')"
        @start-col-resize="$emit('start-col-resize', $event)"
        @auto-fit-column="$emit('auto-fit-column', $event)"
      />
      <tbody>
        <BoxTableRow
          v-for="b in filteredDisplayBoxes"
          :key="b.id"
          :b="b"
          :step="step"
          :selected-box-ids="selectedBoxIds"
          :last-scanned-box-id="lastScannedBoxId"
          :has-unsaved-changes="hasUnsavedChanges"
          :can="can"
          :box-status-label="boxStatusLabel"
          :box-status-class="boxStatusClass"
          :box-row-class="boxRowClass"
          :has-more-actions="hasMoreActions"
          @toggle-row="$emit('toggle-row', $event)"
          @print-box-label="$emit('print-box-label', $event)"
          @toggle-action-menu="$emit('toggle-action-menu', $event)"
          @run-row-action="$emit('run-row-action', $event)"
        />
        <tr v-if="filteredDisplayBoxes.length === 0">
          <td :colspan="columns.length" class="empty">박스가 없습니다.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script lang="ts" setup>
import BoxTableHeader from './BoxTableHeader.vue';
import BoxTableRow from './BoxTableRow.vue';
import type { ColDef } from '../../shared';
import type { StepDescriptor, ActionId } from '../../scenarios';

defineProps<{
  columns: ColDef[];
  tableTotalWidth: number;
  resizingColKey: string | null;
  filteredDisplayBoxes: any[];
  step: StepDescriptor;
  selectedBoxIds: Set<string>;
  lastScannedBoxId: string | null;
  hasUnsavedChanges: boolean;
  allSelected: boolean;
  someSelected: boolean;
  can: (action: string) => boolean;
  boxStatusLabel: (b: any) => string;
  boxStatusClass: (b: any) => string;
  boxRowClass: (b: any) => string;
  hasMoreActions: (b: any) => boolean;
  // 부모 facade 의 boxTableWrapRef 를 그대로 받아 :ref 로 바인딩 → scrollIntoView 가능.
  boxTableWrapRef?: any;
}>();

defineEmits<{
  (e: 'toggle-row', id: string): void;
  (e: 'toggle-select-all'): void;
  (e: 'start-col-resize', payload: { event: MouseEvent; colKey: string }): void;
  (e: 'auto-fit-column', colKey: string): void;
  (e: 'print-box-label', b: any): void;
  (e: 'toggle-action-menu', payload: { event: MouseEvent; box: any }): void;
  (e: 'run-row-action', payload: { actionId: ActionId; box: any }): void;
}>();
</script>

<style scoped>
.box-table-wrap {
  flex: 1 1 0;
  overflow-y: auto;
  overflow-x: auto;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  min-height: 0;
  background: #fff;
}
.box-table-wrap::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
.box-table-wrap::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

.box-table {
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
  table-layout: fixed;
}
.box-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.box-table :deep(td) {
  padding: 8px 12px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.box-table :deep(td.actions) {
  overflow: visible;
}
.box-table :deep(tbody tr) {
  transition: background 0.15s;
}
.box-table :deep(tbody tr:hover) {
  background: #f8fafc;
}
.box-table :deep(tbody tr.row-printed) {
  background: transparent;
}
.box-table :deep(tbody tr.row-printed:hover) {
  background: #f0f9ff;
}
.box-table :deep(tbody tr.row-partial) {
  background: #fffbeb;
}
.box-table :deep(tbody tr.row-partial:hover) {
  background: #fef3c7;
}
.box-table :deep(tbody tr.row-depleted) {
  background: #f8fafc;
  opacity: 0.55;
}
.box-table :deep(tbody tr.row-depleted:hover) {
  opacity: 0.75;
}
.box-table :deep(tbody tr.row-void) {
  background: #fef2f2;
  opacity: 0.55;
}
.box-table :deep(tbody tr.row-void td:nth-child(3)) {
  text-decoration: line-through;
  color: var(--c-danger);
}

/* draft 박스 행 */
.box-table :deep(tr.row-draft-addition) {
  background: rgba(22, 163, 74, 0.07);
  box-shadow: inset 3px 0 0 #16a34a;
}
.box-table :deep(tr.row-draft-edited) {
  background: rgba(14, 165, 233, 0.07);
  box-shadow: inset 3px 0 0 #0ea5e9;
}
.box-table :deep(tr.row-draft-deleted) {
  background: rgba(239, 68, 68, 0.06);
  box-shadow: inset 3px 0 0 #ef4444;
  text-decoration: line-through;
  opacity: 0.75;
}
.box-table :deep(tr.row-draft-deleted .actions) {
  text-decoration: none;
}

/* 스캔 후 파란 하이라이트 — 영구 유지 */
.box-table :deep(tr.row-just-scanned) {
  background-color: rgba(49, 130, 246, 0.15) !important;
  box-shadow: inset 4px 0 0 #3182f6 !important;
}
.box-table :deep(tr.row-just-scanned:hover) {
  background-color: rgba(49, 130, 246, 0.22) !important;
}

/* 선택된 행 */
.box-table :deep(tr.row-selected) {
  background-color: rgba(99, 102, 241, 0.08);
}
.box-table :deep(tr.row-selected:hover) {
  background-color: rgba(99, 102, 241, 0.15);
}
.box-table :deep(tr.row-just-scanned.row-selected) {
  background-color: rgba(49, 130, 246, 0.18) !important;
}

.empty {
  text-align: center;
  color: var(--c-muted);
  padding: 40px;
  font-size: 13px;
}
</style>
