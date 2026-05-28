<template>
  <section v-if="info" class="ws-box-panel">
    <div class="box-panel-header">
      <div class="box-panel-header-left">
        <h2
          >박스 목록 <span class="count">{{ displayBoxes.length }}</span></h2
        >
        <span v-if="selectedBoxIds.size > 0" class="selection-info">
          {{ selectedBoxIds.size }}개 선택됨
          <button type="button" class="selection-clear" @click="$emit('clear-selection')"
            >해제</button
          >
        </span>
      </div>
      <div class="box-panel-header-right">
        <div class="legend">
          <span class="legend-item"><span class="dot dot-printed"></span>인쇄됨</span>
          <span class="legend-item"><span class="dot dot-scanned"></span>스캔됨</span>
          <span class="legend-item"><span class="dot dot-partial"></span>부분 출고</span>
          <span class="legend-item"><span class="dot dot-depleted"></span>소진됨</span>
        </div>
        <a-button
          size="small"
          class="col-reset-btn"
          title="컬럼 너비 초기화"
          @click="$emit('reset-column-widths')"
          >↺ 컬럼</a-button
        >
      </div>
    </div>

    <!-- 미저장 draft -->
    <div v-if="hasUnsavedChanges" class="draft-banner">
      <div class="draft-info">
        <span class="draft-badge">변경사항 {{ totalDraftCount }}건</span>
        <span class="draft-detail">
          <template v-if="Object.keys(draftEdits).length"
            >수정 {{ Object.keys(draftEdits).length }}</template
          >
          <template v-if="draftAdditions.length">
            <template v-if="Object.keys(draftEdits).length"> · </template>추가
            {{ draftAdditions.length }}
          </template>
          <template v-if="Object.keys(draftDeletions).length">
            <template v-if="Object.keys(draftEdits).length || draftAdditions.length"> · </template
            >삭제 {{ Object.keys(draftDeletions).length }}
          </template>
        </span>
      </div>
      <div class="draft-actions">
        <a-button size="small" :disabled="savingDraft" @click="$emit('cancel-draft-edit')"
          >취소</a-button
        >
        <a-button type="primary" size="small" :loading="savingDraft" @click="$emit('save-draft')">
          저장 (일괄 반영)
        </a-button>
      </div>
    </div>

    <!-- 합계 -->
    <div v-if="canAddOrDeleteBox && itemLotSummary.length > 0" class="item-lot-summary">
      <div class="ils-title">박스 합계 VS 주문 (DRAFT 반영 미리보기)</div>
      <div
        v-for="row in itemLotSummary"
        :key="`${row.itemCode}|${row.lotNo}`"
        class="ils-row"
        :class="{
          'ils-ok': row.sum === expectedSumOf(row.itemCode, row.lotNo),
          'ils-bad': row.sum !== expectedSumOf(row.itemCode, row.lotNo),
        }"
      >
        <div class="ils-left">
          <b class="ils-item">{{ row.itemCode }}</b>
          <span class="ils-dot">·</span>
          <span class="ils-lot">{{ row.lotNo }}</span>
        </div>
        <div class="ils-bar-wrap">
          <div class="ils-bar">
            <div
              class="ils-bar-fill"
              :style="{
                width:
                  Math.min(
                    100,
                    Math.round(
                      (row.sum / Math.max(1, expectedSumOf(row.itemCode, row.lotNo))) * 100,
                    ),
                  ) + '%',
              }"
            ></div>
          </div>
        </div>
        <div class="ils-right">
          <b>{{ row.sum }}</b> / <b>{{ expectedSumOf(row.itemCode, row.lotNo) }}</b> EA
          <span v-if="row.sum === expectedSumOf(row.itemCode, row.lotNo)" class="ils-check">✓</span>
          <span v-else class="ils-diff">
            ({{ expectedSumOf(row.itemCode, row.lotNo) - row.sum > 0 ? '+' : ''
            }}{{ expectedSumOf(row.itemCode, row.lotNo) - row.sum }})
          </span>
        </div>
      </div>
    </div>

    <!-- 확정 전 박스 편집 액션 바 — 확정 후 canAddOrDeleteBox=false 로 자동 숨김 -->
    <div v-if="canAddOrDeleteBox" class="grid-action-bar">
      <a-button size="small" @click="$emit('open-add-box')">박스 추가</a-button>
      <a-button
        size="small"
        danger
        :disabled="deletableSelectedCount === 0"
        @click="$emit('bulk-delete-selected')"
      >
        선택 삭제<template v-if="deletableSelectedCount > 0"> ({{ deletableSelectedCount }})</template>
      </a-button>
    </div>

    <BoxFilterBar
      :box-filter="boxFilter"
      :filter-counts="filterCounts"
      @update:box-filter="$emit('update:boxFilter', $event)"
    />

    <BoxTable
      :columns="columns"
      :table-total-width="tableTotalWidth"
      :resizing-col-key="resizingColKey"
      :filtered-display-boxes="filteredDisplayBoxes"
      :step="step"
      :selected-box-ids="selectedBoxIds"
      :last-scanned-box-id="lastScannedBoxId"
      :has-unsaved-changes="hasUnsavedChanges"
      :all-selected="allSelected"
      :some-selected="someSelected"
      :can="can"
      :box-status-label="boxStatusLabel"
      :box-status-class="boxStatusClass"
      :box-row-class="boxRowClass"
      :has-more-actions="hasMoreActions"
      :box-table-wrap-ref="boxTableWrapRef"
      @toggle-row="$emit('toggle-row', $event)"
      @toggle-select-all="$emit('toggle-select-all')"
      @start-col-resize="$emit('start-col-resize', $event)"
      @auto-fit-column="$emit('auto-fit-column', $event)"
      @print-box-label="$emit('print-box-label', $event)"
      @toggle-action-menu="$emit('toggle-action-menu', $event)"
      @run-row-action="$emit('run-row-action', $event)"
    />
  </section>

  <!-- 초기 안내 -->
  <div v-if="!info" class="ws-empty">
    <div class="empty-icon">📦</div>
    <div class="empty-text">파렛트 바코드를 스캔하거나 입력하세요</div>
  </div>

  <BoxActionPopover
    :action-menu="actionMenu"
    :step="step"
    :can-edit-qty="canEditQty"
    :can-edit-total="canEditTotal"
    :can-add-or-delete-box="canAddOrDeleteBox"
    :has-unsaved-changes="hasUnsavedChanges"
    @close="$emit('close-action-menu')"
    @run-menu="$emit('run-menu', $event)"
  />
</template>

<script lang="ts" setup>
  import BoxFilterBar from './BoxFilterBar.vue';
  import BoxTable from './BoxTable.vue';
  import BoxActionPopover from './BoxActionPopover.vue';
  import type { BoxFilter, ColDef } from '../../shared';
  import type { StepDescriptor, ActionId } from '../../scenarios';

  defineProps<{
    step: StepDescriptor;
    info: any;
    displayBoxes: any[];
    filteredDisplayBoxes: any[];
    boxes: any[];
    boxFilter: BoxFilter;
    filterCounts: {
      all: number;
      printed: number;
      scanned: number;
      partial: number;
      depleted: number;
      pending: number;
    };
    selectedBoxIds: Set<string>;
    selectableIds: string[];
    allSelected: boolean;
    someSelected: boolean;
    columns: ColDef[];
    tableTotalWidth: number;
    resizingColKey: string | null;
    itemLotSummary: any[];
    totalDraftCount: number;
    hasUnsavedChanges: boolean;
    canAddOrDeleteBox: boolean;
    deletableSelectedCount: number;
    canPartialOutbound: boolean;
    savingDraft: boolean;
    hasUnfinalizedBoxes: boolean;
    finalizingBoxes: boolean;
    draftEdits: Record<string, any>;
    draftAdditions: any[];
    draftDeletions: Record<string, any>;
    lastScannedBoxId: string | null;
    printing: { pallet: boolean; boxes: boolean; mark: boolean; selected: boolean };
    busy: boolean;
    actionMenu: { open: boolean; target: any; top: number; left: number };
    can: (action: string) => boolean;
    boxStatusLabel: (b: any) => string;
    boxStatusClass: (b: any) => string;
    boxRowClass: (b: any) => string;
    canEditQty: (b: any) => boolean;
    canEditTotal: (b: any) => boolean;
    hasMoreActions: (b: any) => boolean;
    expectedSumOf: (itemCode: string, lotNo: string) => number;
    boxTableWrapRef?: any;
  }>();

  defineEmits<{
    (e: 'update:boxFilter', v: BoxFilter): void;
    (e: 'toggle-row', id: string): void;
    (e: 'toggle-select-all'): void;
    (e: 'clear-selection'): void;
    (e: 'start-col-resize', payload: { event: MouseEvent; colKey: string }): void;
    (e: 'auto-fit-column', colKey: string): void;
    (e: 'reset-column-widths'): void;
    (e: 'toggle-action-menu', payload: { event: MouseEvent; box: any }): void;
    (e: 'close-action-menu'): void;
    (e: 'run-menu', action: string): void;
    (e: 'cancel-draft-edit'): void;
    (e: 'open-add-box'): void;
    (e: 'bulk-delete-selected'): void;
    (e: 'save-draft'): void;
    (e: 'finalize-boxes'): void;
    (e: 'print-box-label', b: any): void;
    (e: 'run-row-action', payload: { actionId: ActionId; box: any }): void;
  }>();
</script>

<style scoped>
  .ws-box-panel {
    grid-column: 2;
    grid-row: 3;
    background: var(--c-card);
    padding: 14px 20px;
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
  }
  .box-panel-header {
    flex: 0 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    gap: 12px;
    flex-wrap: wrap;
  }
  .box-panel-header-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .box-panel-header-right {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
  }
  .box-panel-header h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .box-panel-header .count {
    font-size: 13px;
    font-weight: 500;
    color: var(--c-muted);
    background: var(--c-bg);
    padding: 2px 8px;
    border-radius: 10px;
  }
  .selection-info {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    font-weight: 600;
    color: var(--c-primary);
    background: #eff6ff;
    padding: 3px 8px 3px 10px;
    border-radius: 999px;
    border: 1px solid #dbeafe;
  }
  .selection-clear {
    border: none;
    background: transparent;
    color: var(--c-primary);
    font-size: 11px;
    font-weight: 700;
    cursor: pointer;
    padding: 0 2px;
    text-decoration: underline;
  }
  .selection-clear:hover {
    color: #1d4ed8;
  }

  .legend {
    display: flex;
    gap: 10px;
    font-size: 11px;
    color: var(--c-text-2);
  }
  .legend-item {
    display: flex;
    align-items: center;
    gap: 4px;
  }
  .legend .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    display: inline-block;
  }
  .dot-printed {
    background: #3182f6;
  }
  .dot-scanned {
    background: var(--c-success);
  }
  .dot-partial {
    background: var(--c-warning);
  }
  .dot-depleted {
    background: var(--c-muted);
  }
  .col-reset-btn {
    font-size: 11px;
  }

  /* 확정 전 박스 편집 액션 바 */
  .grid-action-bar {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
  }

  .item-lot-summary {
    flex: 0 0 auto;
    margin-bottom: 10px;
    padding: 10px 14px;
    background: linear-gradient(135deg, #eff6ff 0%, #f0fdf4 100%);
    border: 1px solid #bfdbfe;
    border-radius: 10px;
    font-size: 12px;
  }
  .ils-title {
    font-weight: 700;
    color: #1e40af;
    margin-bottom: 8px;
    font-size: 11px;
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
  .ils-row {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: 14px;
    align-items: center;
    padding: 4px 0;
  }
  .ils-row.ils-ok {
    color: #15803d;
  }
  .ils-row.ils-bad {
    color: #b91c1c;
  }
  .ils-left {
    display: flex;
    align-items: center;
    gap: 6px;
    white-space: nowrap;
  }
  .ils-item {
    color: var(--c-primary);
    font-size: 13px;
  }
  .ils-dot {
    color: var(--c-muted);
  }
  .ils-lot {
    color: var(--c-text-2);
  }
  .ils-bar-wrap {
    width: 100%;
  }
  .ils-bar {
    width: 100%;
    height: 6px;
    background: #e2e8f0;
    border-radius: 3px;
    overflow: hidden;
  }
  .ils-bar-fill {
    height: 100%;
    background: #94a3b8;
    transition: width 0.3s ease;
    border-radius: 3px;
  }
  .ils-row.ils-ok .ils-bar-fill {
    background: var(--c-success);
  }
  .ils-row.ils-bad .ils-bar-fill {
    background: var(--c-warning);
  }
  .ils-right {
    white-space: nowrap;
    font-size: 13px;
    font-variant-numeric: tabular-nums;
  }
  .ils-right b {
    color: var(--c-text);
  }
  .ils-row.ils-ok .ils-right b {
    color: #15803d;
  }
  .ils-row.ils-bad .ils-right b {
    color: #b91c1c;
  }
  .ils-check {
    color: var(--c-success);
    font-weight: 700;
    margin-left: 4px;
  }
  .ils-diff {
    margin-left: 6px;
    font-weight: 700;
  }

  .draft-banner {
    flex: 0 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    padding: 10px 14px;
    background: linear-gradient(135deg, #fef3c7 0%, #fff7ed 100%);
    border: 1.5px solid #f59e0b;
    border-radius: 10px;
    box-shadow: 0 2px 8px rgba(245, 158, 11, 0.18);
  }
  .draft-info {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .draft-badge {
    display: inline-block;
    padding: 3px 9px;
    background: #f59e0b;
    color: #fff;
    font-size: 12px;
    font-weight: 700;
    border-radius: 6px;
  }
  .draft-detail {
    font-size: 12px;
    color: #92400e;
    font-weight: 500;
  }
  .draft-actions {
    display: flex;
    gap: 6px;
  }

  .ws-empty {
    grid-column: 1 / -1;
    grid-row: 3;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 16px;
    color: var(--c-muted);
    min-height: 0;
  }
  .empty-icon {
    font-size: 64px;
    opacity: 0.4;
  }
  .empty-text {
    font-size: 16px;
    font-weight: 500;
  }
</style>
