// 박스 단건 액션 모달 (수량 조정/시험 채취/부분 출고/전체 수량 수정)
// + 박스 추가 모달 + 행 popover 메뉴.

import { computed, reactive, type ComputedRef, type Ref } from 'vue';
import { palletApi } from '../api';
import {
  parseApiError,
  pickedOf,
  remainingOf,
  totalOf,
  type DraftAddition,
  type QtyModalKind,
} from '../shared';

export interface UseBoxActionsDeps {
  palletBarcode: Ref<string>;
  draftEdits: Record<string, number>;
  draftAdditions: DraftAddition[];
  draftDeletions: Record<string, boolean>;
  addDraftAddition: (a: Omit<DraftAddition, 'tempId'>) => void;
  applyDraftEditTotal: (boxId: string, newTotalQty: number) => void;
  toggleDraftDeletion: (b: any) => void;
  canAddOrDeleteBox: ComputedRef<boolean>;
  setMsg: (text: string, kind?: 'ok' | 'err') => void;
  reloadAll: () => Promise<void>;
}

export function useBoxActions(deps: UseBoxActionsDeps) {
  const {
    palletBarcode,
    draftEdits,
    draftAdditions,
    draftDeletions,
    addDraftAddition,
    applyDraftEditTotal,
    toggleDraftDeletion,
    canAddOrDeleteBox,
    setMsg,
    reloadAll,
  } = deps;

  // ─── 수량 모달 ─────
  const qtyModal = reactive({
    open: false,
    busy: false,
    kind: 'adjust' as QtyModalKind,
    title: '',
    target: null as any,
    value: 0,
  });

  function closeQtyModal() {
    if (qtyModal.busy) return;
    qtyModal.open = false;
  }

  function openAdjust(record: any) {
    qtyModal.kind = 'adjust';
    qtyModal.title = '박스 수량 조정';
    qtyModal.target = record;
    qtyModal.value = remainingOf(record);
    qtyModal.open = true;
  }
  function openSampleTaken(record: any) {
    qtyModal.kind = 'sample';
    qtyModal.title = '시험 채취 수량';
    qtyModal.target = record;
    qtyModal.value = 1;
    qtyModal.open = true;
  }
  function openPartialOutbound(record: any) {
    qtyModal.kind = 'partial';
    qtyModal.title = '부분 출고 수량';
    qtyModal.target = record;
    qtyModal.value = 1;
    qtyModal.open = true;
  }
  function openEditTotal(record: any) {
    qtyModal.kind = 'edit-total';
    if (record?._isAddition) {
      qtyModal.title = '추가 예정 박스 수량 수정';
      qtyModal.target = record;
      qtyModal.value = totalOf(record);
    } else {
      qtyModal.title = '박스 전체 수량 수정';
      qtyModal.target = record;
      qtyModal.value = draftEdits[record.id] ?? totalOf(record);
    }
    qtyModal.open = true;
  }
  function confirmVoidPending(b: any) {
    if (b._isAddition) {
      toggleDraftDeletion(b);
      setMsg(`🗑 추가 예정 박스 제거됨 (저장 안 함).`);
      return;
    }
    if (draftDeletions[b.id]) {
      toggleDraftDeletion(b);
      setMsg(`↺ 박스 ${b.box_barcode} 삭제 예정 해제.`);
      return;
    }
    toggleDraftDeletion(b);
    setMsg(`🗑 박스 ${b.box_barcode} 삭제 예정 (저장 대기).`);
  }

  function _qtyHasValidationError(): boolean {
    const v = Number(qtyModal.value);
    if (qtyModal.value === null || qtyModal.value === undefined || Number.isNaN(v)) return true;
    if (qtyModal.kind === 'adjust') {
      if (v < 0) return true;
      if (v > totalOf(qtyModal.target)) return true;
    } else if (qtyModal.kind === 'edit-total') {
      if (v < 1) return true;
    } else {
      if (v <= 0) return true;
      const qMax = !qtyModal.target ? 0 : remainingOf(qtyModal.target);
      if (v > qMax) return true;
    }
    return false;
  }

  async function confirmQtyModal() {
    if (!qtyModal.target) return;
    if (_qtyHasValidationError()) return;
    const id = qtyModal.target.id;
    const v = Number(qtyModal.value) || 0;
    qtyModal.busy = true;
    try {
      if (qtyModal.kind === 'adjust') {
        await palletApi.adjustBox(id, v);
        setMsg(`박스 ${qtyModal.target.box_barcode} 잔여 수량 → ${v}`);
        qtyModal.open = false;
        await reloadAll();
      } else if (qtyModal.kind === 'sample') {
        const r: any = await palletApi.sampleTaken(palletBarcode.value.trim(), id, v);
        setMsg(r?.userMessage || `채취 수량 반영됨.`);
        qtyModal.open = false;
        await reloadAll();
      } else if (qtyModal.kind === 'edit-total') {
        if (qtyModal.target._isAddition) {
          const idx = qtyModal.target._additionIndex;
          if (typeof idx === 'number' && draftAdditions[idx]) draftAdditions[idx].totalQty = v;
          setMsg(`✏ 추가 예정 박스 수량 → ${v} (저장 대기)`);
        } else {
          applyDraftEditTotal(id, v);
          setMsg(`✏ 박스 ${qtyModal.target.box_barcode} 전체 수량 → ${v} (저장 대기)`);
        }
        qtyModal.open = false;
      } else {
        const r: any = await palletApi.partialOutboundBox(id, v);
        setMsg(r?.userMessage || `부분 출고 처리됨.`);
        qtyModal.open = false;
        await reloadAll();
      }
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
    } finally {
      qtyModal.busy = false;
    }
  }

  // ─── 박스 추가 모달 ─────
  const addBoxModal = reactive({
    open: false,
    busy: false,
    itemKey: '',
    totalQty: 1,
    hostItems: [] as Array<{
      itemCode: string;
      lotNo: string;
      qty: number;
      uom?: string;
      produceDate?: string;
      expiryDate?: string;
    }>,
  });

  function _getAddBoxSelected() {
    if (!addBoxModal.itemKey) return null;
    return (
      addBoxModal.hostItems.find(
        (it) => `${it.itemCode ?? ''}|${it.lotNo ?? ''}` === addBoxModal.itemKey,
      ) ?? null
    );
  }
  function _getAddBoxError() {
    const sel = _getAddBoxSelected();
    if (!sel) return '품목/Lot 을 선택하세요.';
    const v = Number(addBoxModal.totalQty);
    if (Number.isNaN(v) || v < 1) return '박스 수량은 1 이상이어야 합니다.';
    return '';
  }

  async function openAddBox() {
    if (!canAddOrDeleteBox.value) return;
    addBoxModal.itemKey = '';
    addBoxModal.totalQty = 1;
    addBoxModal.hostItems = [];
    try {
      const r: any = await palletApi.hostItems(palletBarcode.value.trim());
      addBoxModal.hostItems = Array.isArray(r) ? r : r?.data ?? [];
      if (addBoxModal.hostItems.length === 1) {
        const it = addBoxModal.hostItems[0];
        addBoxModal.itemKey = `${it.itemCode ?? ''}|${it.lotNo ?? ''}`;
      }
      addBoxModal.open = true;
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
    }
  }
  function closeAddBoxModal() {
    if (addBoxModal.busy) return;
    addBoxModal.open = false;
  }
  function confirmAddBox() {
    if (_getAddBoxError()) return;
    const sel = _getAddBoxSelected()!;
    addDraftAddition({
      itemCode: sel.itemCode,
      lotNo: sel.lotNo,
      totalQty: Number(addBoxModal.totalQty) || 0,
      produceDate: sel.produceDate ?? null,
      expiryDate: sel.expiryDate ?? null,
      uom: sel.uom ?? 'EA',
    });
    setMsg(
      `➕ 박스 추가 예정: ${sel.itemCode} / ${sel.lotNo} ${addBoxModal.totalQty} EA (저장 대기)`,
    );
    addBoxModal.open = false;
  }

  // ─── 행 popover 메뉴 ─────
  const actionMenu = reactive({
    open: false,
    top: 0,
    left: 0,
    target: null as any,
  });

  function toggleActionMenu(e: MouseEvent, b: any) {
    if (actionMenu.open && actionMenu.target?.id === b.id) {
      closeActionMenu();
      return;
    }
    const btn = e.currentTarget as HTMLElement;
    const rect = btn.getBoundingClientRect();
    const menuWidth = 180;
    let left = rect.right - menuWidth;
    if (left < 8) left = 8;
    actionMenu.top = rect.bottom + 4;
    actionMenu.left = left;
    actionMenu.target = b;
    actionMenu.open = true;
  }
  function closeActionMenu() {
    actionMenu.open = false;
    actionMenu.target = null;
  }

  return {
    qtyModal,
    addBoxModal,
    actionMenu,
    openAdjust,
    openSampleTaken,
    openPartialOutbound,
    openEditTotal,
    confirmVoidPending,
    confirmQtyModal,
    closeQtyModal,
    openAddBox,
    closeAddBoxModal,
    confirmAddBox,
    toggleActionMenu,
    closeActionMenu,
  };
}
