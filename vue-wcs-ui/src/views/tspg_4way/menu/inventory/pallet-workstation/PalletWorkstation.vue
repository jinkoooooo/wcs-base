<template>
  <div class="ws-shell">
    <!-- 재입고 대기 알람 배너 — 최상단 -->
    <ReinboundAlarmBar
      :due-count="dueCount"
      :ringing="ringing"
      @open="alarmModalOpen = true"
      @dismiss="dismiss"
    />

    <div class="ws-root">
    <!-- 좌측 패널: 헤더 + 파렛트 바 + StepCard + 드로어 -->
    <PwScanPanels
      :info="info"
      :lifecycle="lifecycle"
      v-model:pallet="pallet"
      v-model:box="box"
      :pallet-input-ref="palletRef"
      :box-input-ref="boxRef"
      :step="step"
      :step-tooltips="stepTooltips"
      :can-scan-input="canScanInput"
      :can-show="can('show')"
      :can-update="can('update')"
      :selected-box-ids="selectedBoxIds"
      :printing="printing"
      :auto-release="autoRelease"
      :aux-drawer-open="auxDrawerOpen"
      :history-drawer-open="historyDrawerOpen"
      :loading="loading"
      :msg="msg"
      :msg-class="msgClass"
      @load="load"
      @scan="scan"
      @open-history="openHistoryModal"
      @toggle-aux="auxDrawerOpen = !auxDrawerOpen"
      @toggle-history="historyDrawerOpen = !historyDrawerOpen"
      @run-action="runAction"
    />

    <!-- 우측: 박스 목록 + 액션 popover -->
    <BoxList
      :info="info"
      :step="step"
      :display-boxes="displayBoxes"
      :filtered-display-boxes="filteredDisplayBoxes"
      :boxes="boxes"
      :box-filter="boxFilter"
      :filter-counts="filterCounts"
      :selected-box-ids="selectedBoxIds"
      :selectable-ids="selectableIds"
      :all-selected="allSelected"
      :some-selected="someSelected"
      :columns="columns"
      :table-total-width="tableTotalWidth"
      :resizing-col-key="resizingColKey"
      :item-lot-summary="itemLotSummary"
      :total-draft-count="totalDraftCount"
      :has-unsaved-changes="hasUnsavedChanges"
      :can-add-or-delete-box="canAddOrDeleteBox"
      :deletable-selected-count="deletableSelectedCount"
      :can-partial-outbound="canPartialOutbound"
      :saving-draft="savingDraft"
      :finalizing-boxes="finalizingBoxes"
      :has-unfinalized-boxes="hasUnfinalizedBoxes"
      :draft-edits="draftEdits"
      :draft-additions="draftAdditions"
      :draft-deletions="draftDeletions"
      :last-scanned-box-id="lastScannedBoxId"
      :printing="printing"
      :busy="busy"
      :action-menu="actionMenu"
      :can="can"
      :box-status-label="boxStatusLabel"
      :box-status-class="boxStatusClass"
      :box-row-class="boxRowClass"
      :can-edit-qty="canEditQty"
      :can-edit-total="canEditTotal"
      :has-more-actions="hasMoreActions"
      :expected-sum-of="expectedSumOf"
      :box-table-wrap-ref="boxTableWrapRef"
      @update:box-filter="boxFilter = $event"
      @toggle-row="toggleSelectRow"
      @toggle-select-all="toggleSelectAll"
      @clear-selection="clearSelection"
      @start-col-resize="startColResize($event.event, $event.colKey)"
      @auto-fit-column="autoFitColumn"
      @reset-column-widths="resetColumnWidths"
      @toggle-action-menu="toggleActionMenu($event.event, $event.box)"
      @close-action-menu="closeActionMenu"
      @run-menu="runMenu"
      @cancel-draft-edit="confirmDiscardDraft"
      @open-add-box="openAddBox"
      @bulk-delete-selected="bulkDeleteSelected"
      @save-draft="saveDraft"
      @finalize-boxes="finalizePallet"
      @print-box-label="printBoxLabel"
      @run-row-action="onRowAction"
    />

    <!-- 수량 입력 모달 -->
    <PalletQtyModal
      :open="qtyModal.open"
      :kind="qtyModal.kind"
      :title="qtyModal.title"
      :target="qtyModal.target"
      v-model:value="qtyModal.value"
      :busy="qtyModal.busy"
      :host-item-expected="expectedSumOf"
      :item-lot-summary="itemLotSummary"
      :display-boxes="displayBoxes"
      @confirm="confirmQtyModal"
      @close="closeQtyModal"
    />

    <!-- 박스 추가 모달 -->
    <PalletAddBoxModal
      :open="addBoxModal.open"
      :busy="addBoxModal.busy"
      :host-items="addBoxModal.hostItems"
      v-model:item-key="addBoxModal.itemKey"
      v-model:total-qty="addBoxModal.totalQty"
      :item-lot-summary="itemLotSummary"
      @confirm="confirmAddBox"
      @close="closeAddBoxModal"
    />

    <!-- 라벨 재발행 사유 입력 모달 -->
    <PalletLabelReissueModal
      :open="labelReissueModal.open"
      :target="labelReissueModal.target"
      :comment="labelReissueModal.comment"
      :busy="labelReissueModal.busy"
      @update:comment="labelReissueModal.comment = $event"
      @confirm="confirmLabelReissue"
      @close="closeLabelReissueModal"
    />

    <!-- 확정 미리보기 모달 — 시험 사이클 / 관리자 입고·출고 우회 공통 -->
    <PalletFinalizeModal
      :open="finalizeModal.open"
      :title="finalizeModal.title"
      :groups="finalizeModal.groups"
      :confirm-label="finalizeModal.confirmLabel"
      :busy="finalizeModal.busy"
      @confirm="confirmFinalizeModal"
      @close="closeFinalizeModal"
    />

    <!-- 파렛트 이력 모달 -->
    <PalletHistoryModal
      :open="historyModal.open"
      :entries="lifecycle"
      :highlight-order-key="historyModal.highlightOrderKey"
      @close="closeHistoryModal"
    />
    </div>

    <!-- 재입고 대기 파렛트 목록 모달 -->
    <ReinboundAlarmModal
      :open="alarmModalOpen"
      :rows="rows"
      :interval-min="intervalMin"
      @close="alarmModalOpen = false"
      @load-pallet="onLoadAlarmPallet"
    />
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { usePalletWorkstation } from './composables/usePalletWorkstation';
  import { useReinboundAlarm } from './composables/useReinboundAlarm';
  import PwScanPanels from './PwScanPanels.vue';
  import BoxList from './components/box-list/BoxList.vue';
  import ReinboundAlarmBar from './components/ReinboundAlarmBar.vue';
  import PalletQtyModal from './popup/PalletQtyModal.vue';
  import PalletAddBoxModal from './popup/PalletAddBoxModal.vue';
  import PalletLabelReissueModal from './popup/PalletLabelReissueModal.vue';
  import PalletHistoryModal from './popup/PalletHistoryModal.vue';
  import PalletFinalizeModal from './popup/PalletFinalizeModal.vue';
  import ReinboundAlarmModal from './popup/ReinboundAlarmModal.vue';
  import type { ActionId } from './scenarios';

  const {
    // ─── Vue refs ───
    palletRef,
    boxRef,
    boxTableWrapRef,

    // ─── 입력 / 데이터 ───
    pallet,
    box,
    info,
    boxes,
    lifecycle,

    // ─── 이력 모달 ───
    historyModal,
    openHistoryModal,
    closeHistoryModal,

    // ─── UI 플래그 ───
    autoRelease,
    loading,
    busy,
    printing,
    msg,
    msgClass,
    boxFilter,

    // ─── 라벨 재발행 모달 ───
    labelReissueModal,
    closeLabelReissueModal,
    confirmLabelReissue,

    // ─── 행 선택 ───
    selectedBoxIds,
    selectableIds,
    allSelected,
    someSelected,
    toggleSelectRow,
    clearSelection,
    toggleSelectAll,

    // ─── 컬럼 ───
    columns,
    tableTotalWidth,
    resizingColKey,
    startColResize,
    autoFitColumn,
    resetColumnWidths,

    // ─── 스캔 하이라이트 ───
    lastScannedBoxId,

    // ─── 액션 popover ───
    actionMenu,
    toggleActionMenu,
    closeActionMenu,
    runMenu,

    // ─── 모달 / draft ───
    qtyModal,
    addBoxModal,
    draftEdits,
    draftDeletions,
    draftAdditions,
    savingDraft,
    finalizingBoxes,
    closeQtyModal,
    confirmQtyModal,
    openAddBox,
    closeAddBoxModal,
    confirmAddBox,
    confirmDiscardDraft,
    saveDraft,

    // ─── 박스 액션 (단건) ───
    openSampleTaken,
    openPartialOutbound,
    openEditTotal,
    confirmVoidPending,

    // ─── 표시 데이터 ───
    displayBoxes,
    filteredDisplayBoxes,
    filterCounts,
    hasUnsavedChanges,
    hasUnfinalizedBoxes,
    totalDraftCount,
    itemLotSummary,
    canAddOrDeleteBox,
    deletableSelectedCount,
    bulkDeleteSelected,
    canPartialOutbound,
    percent,

    // ─── 권한 ───
    can,

    // ─── 액션 함수 ───
    load,
    scan,
    finalizePallet,
    printBoxLabel,

    // ─── 박스 상태 표시 ───
    boxStatusLabel,
    boxStatusClass,
    boxRowClass,
    canEditQty,
    canEditTotal,
    hasMoreActions,
    expectedSumOf,

    // ─── 시나리오 / 스텝 ───
    step,
    stepTooltips,
    auxDrawerOpen,
    historyDrawerOpen,
    runAction,

    // ─── 확정 미리보기 모달 ───
    finalizeModal,
    confirmFinalizeModal,
    closeFinalizeModal,
  } = usePalletWorkstation();

  // 재입고 대기 알람 — followUpSince 기반 폴링 + 소리
  const { intervalMin, rows, dueCount, ringing, dismiss } = useReinboundAlarm();
  const alarmModalOpen = ref(false);

  // 알람 목록에서 파렛트 선택 → 작업대 로드
  function onLoadAlarmPallet(barcode: string) {
    alarmModalOpen.value = false;
    pallet.value = barcode;
    load();
  }

  // 박스 입력 가능 여부 — 기존 동작과 일치 (전수 스캔 후 / 미저장 변경 시 막음)
  const canScanInput = computed(() => !hasUnsavedChanges.value && percent.value < 100);

  // 박스 행 인라인 액션 → composable 의 박스 단건 함수로 라우팅
  function onRowAction(payload: { actionId: ActionId; box: any }) {
    const { actionId, box: b } = payload;
    if (!b) return;
    if (actionId === 'printBoxLabel') printBoxLabel(b);
    else if (actionId === 'sample') openSampleTaken(b);
    else if (actionId === 'partial') openPartialOutbound(b);
    else if (actionId === 'editTotal') openEditTotal(b);
    else if (actionId === 'void') confirmVoidPending(b);
  }
</script>

<style scoped>
  /* 최상단 알람 배너 + 작업대를 세로로 쌓는 shell */
  .ws-shell {
    height: 100%;
    width: 100%;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }

  /* 폰트 / 풀스크린 락 */
  .ws-root {
    font-family: 'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont,
      'Apple SD Gothic Neo', 'Malgun Gothic', 'Segoe UI', Roboto, sans-serif;

    --c-bg: #f6f7f9;
    --c-card: #ffffff;
    --c-border: #e8eaed;
    --c-text: #0f172a;
    --c-text-2: #475569;
    --c-muted: #94a3b8;
    --c-primary: #3182f6;
    --c-success: #16a34a;
    --c-warning: #f59e0b;
    --c-danger: #ef4444;
    --c-sample: #ec4899;

    flex: 1 1 0;
    width: 100%;
    min-height: 0;
    background: var(--c-bg);
    color: var(--c-text);
    display: grid;
    grid-template-rows: auto auto 1fr;
    grid-template-columns: 360px 1fr;
    overflow: hidden;
    font-feature-settings: 'tnum';
  }

  /* PwScanPanels fragment 자식들: header, pallet-bar, work-panel 배치 */
  .ws-root > :deep(.ws-header) {
    grid-column: 1 / -1;
    grid-row: 1;
  }
  .ws-root > :deep(.ws-pallet-bar) {
    grid-column: 1 / -1;
    grid-row: 2;
  }
  .ws-root > :deep(.ws-work-panel) {
    grid-column: 1;
    grid-row: 3;
    min-height: 0;
  }
</style>
