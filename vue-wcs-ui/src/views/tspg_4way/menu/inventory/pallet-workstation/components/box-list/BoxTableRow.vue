<template>
  <tr
    :data-box-id="b.id"
    :class="[
      boxRowClass(b),
      b._draftDeleted ? 'row-draft-deleted' : '',
      b._draftEditTotal != null ? 'row-draft-edited' : '',
      b._isAddition ? 'row-draft-addition' : '',
      lastScannedBoxId === b.id ? 'row-just-scanned' : '',
      selectedBoxIds.has(b.id) ? 'row-selected' : '',
    ]"
  >
    <td class="td-select">
      <label v-if="!b._isAddition" class="row-checkbox-label" @click.stop>
        <input
          type="checkbox"
          class="row-checkbox"
          :checked="selectedBoxIds.has(b.id)"
          @change="$emit('toggle-row', b.id)"
        />
      </label>
    </td>
    <td class="td-seq">{{ b.box_seq ?? '-' }}</td>
    <td :title="b.box_barcode || ''">{{ b.box_barcode || '-' }}</td>
    <td>{{ b.item_code }}</td>
    <td>{{ b.lot_no }}</td>
    <td class="num">
      <span :class="{ 'num-picked': pickedOf(b) > 0 }">{{ pickedOf(b) }}</span>
    </td>
    <td class="td-progress">
      <div class="cell-progress-wrap">
        <span class="cell-remain">
          {{ b._draftEditTotal != null ? b._draftEditTotal : remainingOf(b) }}
        </span>
        <div class="cell-bar">
          <div
            class="cell-bar-fill"
            :class="cellBarColor(b)"
            :style="{ width: cellBarPercent(b) + '%' }"
          ></div>
        </div>
        <span class="cell-total"
          >/ {{ b._draftEditTotal != null ? b._draftEditTotal : totalOf(b) }}</span
        >
      </div>
    </td>
    <td>
      <span v-if="b._isAddition" class="status-pill sp-draft-add">
        <span class="sp-dot"></span>추가 예정
      </span>
      <span v-else-if="b._draftDeleted" class="status-pill sp-draft-del">
        <span class="sp-dot"></span>삭제 예정
      </span>
      <span v-else-if="b._draftEditTotal != null" class="status-pill sp-draft-edit">
        <span class="sp-dot"></span>수정 예정
      </span>
      <span v-else :class="['status-pill', boxStatusClass(b)]">
        <span class="sp-dot"></span>{{ boxStatusLabel(b) }}
      </span>
    </td>
    <td class="actions">
      <BoxRowActionCell
        v-if="step.rowAction(b)"
        :descriptor="step.rowAction(b)"
        :box="b"
        @run-row-action="$emit('run-row-action', $event)"
      />
      <button
        v-if="!b._isAddition && can('update') && b.box_seq != null"
        type="button"
        class="row-print-btn"
        :disabled="hasUnsavedChanges"
        :title="hasUnsavedChanges ? '미저장 변경사항이 있어 비활성화됨' : '라벨 인쇄'"
        @click.stop="$emit('print-box-label', b)"
      >
        라벨
      </button>
      <button
        v-if="can('update') && hasMoreActions(b) && step.boxMenuActions.length > 0"
        type="button"
        class="more-btn-link"
        :data-box-id="b.id"
        title="추가 작업"
        @click.stop="$emit('toggle-action-menu', { event: $event, box: b })"
      >
        ⋮
      </button>
    </td>
  </tr>
</template>

<script lang="ts" setup>
  import { pickedOf, remainingOf, totalOf, cellBarPercent, cellBarColor } from '../../shared';
  import type { StepDescriptor, ActionId } from '../../scenarios';
  import BoxRowActionCell from './BoxRowActionCell.vue';

  defineProps<{
    b: any;
    step: StepDescriptor;
    selectedBoxIds: Set<string>;
    lastScannedBoxId: string | null;
    hasUnsavedChanges: boolean;
    can: (action: string) => boolean;
    boxStatusLabel: (b: any) => string;
    boxStatusClass: (b: any) => string;
    boxRowClass: (b: any) => string;
    hasMoreActions: (b: any) => boolean;
  }>();

  defineEmits<{
    (e: 'toggle-row', id: string): void;
    (e: 'print-box-label', b: any): void;
    (e: 'toggle-action-menu', payload: { event: MouseEvent; box: any }): void;
    (e: 'run-row-action', payload: { actionId: ActionId; box: any }): void;
  }>();
</script>

<style scoped>
  .td-select {
    padding: 8px 10px;
    text-align: center;
  }
  .row-checkbox-label {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    cursor: pointer;
  }
  .row-checkbox {
    width: 16px;
    height: 16px;
    cursor: pointer;
    accent-color: var(--c-primary);
    margin: 0;
  }
  .td-seq {
    font-weight: 700;
    color: var(--c-text-2);
  }
  .num {
    text-align: right;
    font-variant-numeric: tabular-nums;
  }
  .num-picked {
    color: var(--c-warning);
    font-weight: 700;
  }

  .td-progress {
    padding-right: 12px;
  }
  .cell-progress-wrap {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: 8px;
    align-items: center;
  }
  .cell-remain {
    font-weight: 700;
    color: var(--c-text);
    font-variant-numeric: tabular-nums;
    min-width: 22px;
    text-align: right;
  }
  .cell-bar {
    height: 4px;
    background: #e2e8f0;
    border-radius: 2px;
    overflow: hidden;
    min-width: 40px;
  }
  .cell-bar-fill {
    height: 100%;
    background: #94a3b8;
    border-radius: 2px;
    transition: width 0.3s ease;
  }
  .cell-bar-fill.bar-printed {
    background: var(--c-primary);
  }
  .cell-bar-fill.bar-pending {
    background: #cbd5e1;
  }
  .cell-bar-fill.bar-partial {
    background: var(--c-warning);
  }
  .cell-bar-fill.bar-depleted {
    background: var(--c-muted);
  }
  .cell-bar-fill.bar-default {
    background: var(--c-success);
  }
  .cell-total {
    color: var(--c-muted);
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    min-width: 30px;
  }

  .actions {
    white-space: nowrap;
    display: flex;
    align-items: center;
    gap: 4px;
    overflow: visible;
  }
  .actions > * {
    /* ← 추가: 버튼들이 찌그러지지 않게 */
    flex: 0 0 auto;
  }
  .more-btn-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 26px;
    width: 26px;
    height: 26px;
    border: 1px solid var(--c-border);
    background: #fff;
    border-radius: 6px;
    color: var(--c-text-2);
    cursor: pointer;
    font-size: 18px;
    font-weight: 700;
    line-height: 1;
    user-select: none;
    padding: 0;
    transition: all 0.15s;
  }
  .more-btn-link:hover {
    background: #f1f5f9;
    border-color: #cbd5e1;
    color: var(--c-text);
  }
  .more-btn-link:active {
    background: #e2e8f0;
  }
  .row-print-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 26px;
    padding: 0 10px;
    background: #fff;
    border: 1px solid var(--c-border);
    border-radius: 6px;
    color: var(--c-text-2);
    cursor: pointer;
    font-size: 11px;
    font-weight: 700;
    line-height: 1;
    user-select: none;
    transition: all 0.15s;
    white-space: nowrap;
  }
  .row-print-btn:hover:not(:disabled) {
    background: #eff6ff;
    border-color: #93c5fd;
    color: var(--c-primary);
  }
  .row-print-btn:active:not(:disabled) {
    background: #dbeafe;
  }
  .row-print-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }

  .status-pill {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 2px 9px;
    font-size: 11px;
    font-weight: 600;
    border-radius: 10px;
    letter-spacing: -0.1px;
  }
  .sp-dot {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: currentColor;
    display: inline-block;
  }
  .sp-pending {
    background: #f1f5f9;
    color: var(--c-muted);
  }
  .sp-printed {
    background: #dbeafe;
    color: #1e40af;
  }
  .sp-scanned {
    background: #dcfce7;
    color: #15803d;
  }
  .sp-partial {
    background: #fef3c7;
    color: #92400e;
  }
  .sp-depleted {
    background: #f1f5f9;
    color: var(--c-muted);
    text-decoration: line-through;
  }
  .sp-void {
    background: #fee2e2;
    color: #991b1b;
  }
  .sp-draft-add {
    background: #dcfce7;
    color: #15803d;
  }
  .sp-draft-edit {
    background: #e0f2fe;
    color: #0369a1;
  }
  .sp-draft-del {
    background: #fee2e2;
    color: #991b1b;
  }
</style>
