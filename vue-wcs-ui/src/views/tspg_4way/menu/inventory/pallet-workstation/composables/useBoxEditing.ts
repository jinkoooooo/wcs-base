// 박스 draft 편집(수정/추가/삭제) + saveDraft + finalize(box_seq 부여).

import { computed, reactive, ref, type Ref, type ComputedRef } from 'vue';
import { palletApi } from '../api';
import { parseApiError, type DraftAddition } from '../shared';
import { BoxStatus } from '/@/views/tspg_4way/constants/wcsConsts';

export interface UseBoxEditingDeps {
  palletBarcode: Ref<string>;
  boxes: Ref<any[]>;
  setMsg: (text: string, kind?: 'ok' | 'err') => void;
  reloadAll: () => Promise<void>;
  createConfirm: (opts: any) => void;
}

export function useBoxEditing(deps: UseBoxEditingDeps) {
  const { palletBarcode, boxes, setMsg, reloadAll, createConfirm } = deps;

  const draftEdits = reactive<Record<string, number>>({});
  const draftDeletions = reactive<Record<string, boolean>>({});
  const draftAdditions = reactive<DraftAddition[]>([]);
  const savingDraft = ref(false);
  const finalizingBoxes = ref(false);

  const totalDraftCount = computed(
    () =>
      Object.keys(draftEdits).length +
      draftAdditions.length +
      Object.keys(draftDeletions).length,
  );
  const hasUnsavedChanges = computed(() => totalDraftCount.value > 0);

  const hasUnfinalizedBoxes: ComputedRef<boolean> = computed(() =>
    boxes.value.some((b: any) => Number(b.box_status) === BoxStatus.DRAFT.code),
  );
  const unfinalizedCount = computed(
    () => boxes.value.filter((b: any) => Number(b.box_status) === BoxStatus.DRAFT.code).length,
  );

  function applyDraftEditTotal(boxId: string, newTotalQty: number) {
    draftEdits[boxId] = newTotalQty;
  }
  function cancelDraftEdit(boxId: string) {
    delete draftEdits[boxId];
  }
  function toggleDraftDeletion(b: any) {
    if (b._isAddition) {
      const idx = b._additionIndex;
      if (typeof idx === 'number') draftAdditions.splice(idx, 1);
      return;
    }
    if (draftDeletions[b.id]) delete draftDeletions[b.id];
    else {
      draftDeletions[b.id] = true;
      delete draftEdits[b.id];
    }
  }
  function addDraftAddition(a: Omit<DraftAddition, 'tempId'>) {
    draftAdditions.push({
      tempId: `add-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      ...a,
    });
  }
  function discardDraft() {
    for (const k of Object.keys(draftEdits)) delete draftEdits[k];
    for (const k of Object.keys(draftDeletions)) delete draftDeletions[k];
    draftAdditions.splice(0, draftAdditions.length);
  }
  function confirmDiscardDraft() {
    if (!hasUnsavedChanges.value) return;
    createConfirm({
      iconType: 'warning',
      title: () => '변경사항 취소',
      content: () => `미저장 변경 ${totalDraftCount.value} 건을 모두 취소합니다.`,
      onOk: () => discardDraft(),
    });
  }

  async function finalizePallet() {
    const code = palletBarcode.value.trim();
    if (!code || finalizingBoxes.value) return;
    if (hasUnsavedChanges.value) {
      setMsg('미저장 변경사항이 있습니다. 먼저 저장 후 확정하세요.', 'err');
      return;
    }
    if (!hasUnfinalizedBoxes.value) {
      setMsg('확정할 박스가 없습니다.', 'err');
      return;
    }
    finalizingBoxes.value = true;
    try {
      const r: any = await palletApi.finalize(code);
      setMsg(`박스 확정 완료 — ${r?.finalizedCount ?? 0}개`);
      await reloadAll();
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
    } finally {
      finalizingBoxes.value = false;
    }
  }

  async function saveDraft() {
    if (!hasUnsavedChanges.value || savingDraft.value) return;
    savingDraft.value = true;
    try {
      const body = {
        edits: Object.entries(draftEdits).map(([boxId, totalQty]) => ({ boxId, totalQty })),
        additions: draftAdditions.map((a) => ({
          itemCode: a.itemCode,
          lotNo: a.lotNo,
          totalQty: a.totalQty,
          produceDate: a.produceDate ?? null,
          expiryDate: a.expiryDate ?? null,
        })),
        deletions: Object.keys(draftDeletions).map((boxId) => ({ boxId })),
      };
      const r: any = await palletApi.editBatch(palletBarcode.value.trim(), body);
      setMsg(
        `일괄 저장 완료 — 수정 ${r?.editedCount ?? 0} / 추가 ${r?.addedCount ?? 0} / 삭제 ${
          r?.deletedCount ?? 0
        }`,
      );
      discardDraft();
      await reloadAll();
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
    } finally {
      savingDraft.value = false;
    }
  }

  return {
    draftEdits,
    draftDeletions,
    draftAdditions,
    savingDraft,
    finalizingBoxes,
    totalDraftCount,
    hasUnsavedChanges,
    hasUnfinalizedBoxes,
    unfinalizedCount,
    applyDraftEditTotal,
    cancelDraftEdit,
    toggleDraftDeletion,
    addDraftAddition,
    discardDraft,
    confirmDiscardDraft,
    finalizePallet,
    saveDraft,
  };
}
