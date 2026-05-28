/**
 * inbound-work 전용 composable
 */

import { computed, onMounted, reactive, ref, watch } from 'vue';
import {
  getActiveStorageAreas,
  getInboundItemPolicy,
  postInboundStock,
  type StorageAreaOption,
  selectInboundLocation,
} from '@/api/asrs/stock';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import { createLocalDateTimeText } from '@/views/asrs/shared/utils/format';
import { normalizeCode, normalizeMessage } from '@/views/asrs/shared/utils/normalize';
import {
  normalizeInboundAreaList,
  normalizeInboundCommandResult,
  normalizeInboundItemPolicy,
  normalizeInboundRecommendCandidates,
} from '../mappers/inboundWork.mapper';
import type {
  InboundDraftRow,
  InboundFormState,
  InboundHistoryRow,
  InboundItemPolicyState,
  InboundLoadingState,
  InboundRecommendState,
  InboundSummary,
  InboundTabType,
} from '../types';

function createInitialForm(): InboundFormState {
  return {
    areaCode: '',
    itemCode: '',
    qty: 0,
    lotNo: '',
  };
}

function createInitialItemPolicy(): InboundItemPolicyState {
  return {
    lotEnabled: false,
    lotRequired: false,
    lotControlRequired: false,
    expiryControlRequired: false,
    serialControlRequired: false,
  };
}

function createInitialRecommend(): InboundRecommendState {
  return {
    options: [],
    selectedLocationCode: '',
    candidateCount: 0,
  };
}

export function useInboundWork() {
  const form = reactive<InboundFormState>(createInitialForm());

  const { flags: loading } = useAsyncFlags<InboundLoadingState>({
    areas: false,
    recommend: false,
    submit: false,
  });

  const { feedback, setFeedback, clearFeedback } = useFeedback();

  const itemPolicy = reactive<InboundItemPolicyState>(createInitialItemPolicy());
  const activeTab = ref<InboundTabType>('detail');
  const areaOptions = ref<StorageAreaOption[]>([]);
  const draftRows = ref<InboundDraftRow[]>([]);
  const recentHistory = ref<InboundHistoryRow[]>([]);
  const selectedRow = ref<InboundDraftRow | null>(null);
  const recommend = reactive<InboundRecommendState>(createInitialRecommend());

  let recommendTimer: ReturnType<typeof setTimeout> | null = null;

  const selectedRecommend = computed(() => {
    return recommend.options.find((item) => item.locationCode === recommend.selectedLocationCode) || null;
  });

  const summary = computed<InboundSummary>(() => {
    const pendingCount = draftRows.value.length;
    const totalQty = draftRows.value.reduce((sum, row) => sum + Number(row.qty || 0), 0);
    const itemCount = new Set(draftRows.value.map((row) => row.itemCode)).size;

    return {
      pendingCount,
      totalQty,
      itemCount,
    };
  });

  function newRowId() {
    return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }

  function canRecommend() {
    return !!form.areaCode && !!form.itemCode && Number(form.qty) > 0;
  }

  async function loadAreas() {
    loading.areas = true;

    try {
      const payload = await getActiveStorageAreas();
      areaOptions.value = normalizeInboundAreaList(payload);
    } catch (error) {
      console.error(error);
      areaOptions.value = [];
      setFeedback('error', 'Area 목록을 불러오지 못했습니다.');
    } finally {
      loading.areas = false;
    }
  }

  async function loadItemPolicy() {
    if (!form.areaCode || !form.itemCode) {
      Object.assign(itemPolicy, createInitialItemPolicy());
      form.lotNo = '';
      return;
    }

    try {
      const payload = await getInboundItemPolicy(form.areaCode, form.itemCode);
      const normalized = normalizeInboundItemPolicy(payload);

      itemPolicy.lotEnabled = normalized.lotEnabled;
      itemPolicy.lotRequired = normalized.lotRequired;
      itemPolicy.lotControlRequired = normalized.lotControlRequired;
      itemPolicy.expiryControlRequired = normalized.expiryControlRequired;
      itemPolicy.serialControlRequired = normalized.serialControlRequired;

      if (!itemPolicy.lotEnabled) {
        form.lotNo = '';
      }
    } catch (error) {
      console.error(error);
      Object.assign(itemPolicy, createInitialItemPolicy());
      form.lotNo = '';
    }
  }

  async function runRecommend() {
    if (!canRecommend()) {
      Object.assign(recommend, createInitialRecommend());
      return;
    }

    loading.recommend = true;

    try {
      const payload = await selectInboundLocation({
        areaCode: form.areaCode,
        itemCode: form.itemCode,
        qty: Number(form.qty),
        lotNo: form.lotNo || undefined,
      });

      const resolved = normalizeInboundRecommendCandidates(payload);

      recommend.options = resolved.options;
      recommend.candidateCount = resolved.candidateCount;
      recommend.selectedLocationCode =
        resolved.options.length > 0 ? resolved.options[0].locationCode : '';

      if (!recommend.selectedLocationCode) {
        setFeedback('warning', '추천 가능한 로케이션을 찾지 못했습니다.');
      } else {
        clearFeedback();
      }
    } catch (error: any) {
      console.error(error);

      Object.assign(recommend, createInitialRecommend());

      const errorCode = normalizeCode(error?.response?.data);
      const errorMsg = normalizeMessage(error?.response?.data);

      if (errorCode === 'ENTITY_NOT_FOUND') {
        setFeedback('warning', '추천 가능한 로케이션이 없습니다.');
        return;
      }

      setFeedback('error', errorMsg || '로케이션 추천 중 오류가 발생했습니다.');
    } finally {
      loading.recommend = false;
    }
  }

  function validateForm() {
    if (!form.areaCode) {
      setFeedback('warning', 'Area Code를 선택해주세요.');
      return false;
    }

    if (!form.itemCode) {
      setFeedback('warning', 'Item Code를 입력해주세요.');
      return false;
    }

    if (!form.qty || Number(form.qty) <= 0) {
      setFeedback('warning', 'Qty는 1 이상이어야 합니다.');
      return false;
    }

    if (!recommend.selectedLocationCode) {
      setFeedback('warning', '추천 로케이션을 선택해주세요.');
      return false;
    }

    if (itemPolicy.lotRequired && !form.lotNo) {
      setFeedback('warning', '해당 품목은 Lot No가 필수입니다.');
      return false;
    }

    return true;
  }

  function resetForm() {
    Object.assign(form, createInitialForm());
    Object.assign(itemPolicy, createInitialItemPolicy());
    Object.assign(recommend, createInitialRecommend());
    clearFeedback();
  }

  function resetPage() {
    resetForm();
    draftRows.value = [];
    recentHistory.value = [];
    selectedRow.value = null;
    activeTab.value = 'detail';
  }

  function addDraft() {
    if (!validateForm()) return;

    const row: InboundDraftRow = {
      rowId: newRowId(),
      areaCode: form.areaCode,
      itemCode: form.itemCode,
      qty: Number(form.qty),
      lotNo: form.lotNo || undefined,
      locationCode: recommend.selectedLocationCode,
      locationGrade: selectedRecommend.value?.locationGrade || undefined,
      candidateCount: recommend.candidateCount,
      status: 'READY',
    };

    draftRows.value.unshift(row);
    selectedRow.value = row;
    activeTab.value = 'detail';

    setFeedback('success', '입고 대기 목록에 등록되었습니다.');
  }

  function selectRow(row: InboundDraftRow) {
    selectedRow.value = row;
    activeTab.value = 'detail';
  }

  function removeDraft(rowId: string) {
    draftRows.value = draftRows.value.filter((row) => row.rowId !== rowId);

    if (selectedRow.value?.rowId === rowId) {
      selectedRow.value = draftRows.value[0] || null;
    }

    setFeedback('info', '선택한 대기 항목이 삭제되었습니다.');
  }

  async function submitInbound() {
    if (!draftRows.value.length) {
      setFeedback('warning', '먼저 입고 대기 목록을 등록해주세요.');
      return;
    }

    loading.submit = true;

    try {
      const completedRows: InboundHistoryRow[] = [];

      for (const row of draftRows.value) {
        const payload = await postInboundStock({
          areaCode: row.areaCode,
          locationCode: row.locationCode,
          itemCode: row.itemCode,
          qty: row.qty,
          lotNo: row.lotNo,
        });

        const result = normalizeInboundCommandResult(payload);

        completedRows.unshift({
          rowId: newRowId(),
          inboundAt: createLocalDateTimeText(),
          stockUnitNo: result.stockUnitNo,
          txnNo: result.txnNo,
          itemCode: row.itemCode,
          qty: row.qty,
        });
      }

      recentHistory.value = [...completedRows, ...recentHistory.value].slice(0, 30);
      draftRows.value = [];
      selectedRow.value = null;
      activeTab.value = 'history';

      setFeedback('success', `${completedRows.length}건 입고 처리되었습니다.`);
      resetForm();
    } catch (error: any) {
      console.error(error);

      const errorMsg = normalizeMessage(error?.response?.data);
      setFeedback('error', errorMsg || '입고 처리 중 오류가 발생했습니다.');
    } finally {
      loading.submit = false;
    }
  }

  watch(
    () => [form.areaCode, form.itemCode, form.qty, form.lotNo],
    () => {
      if (recommendTimer) clearTimeout(recommendTimer);

      recommendTimer = setTimeout(() => {
        runRecommend();
      }, 350);
    },
  );

  watch(
    () => [form.areaCode, form.itemCode],
    () => {
      loadItemPolicy();
    },
  );

  onMounted(() => {
    loadAreas();
  });

  return {
    form,
    loading,
    feedback,
    itemPolicy,
    activeTab,
    areaOptions,
    draftRows,
    recentHistory,
    selectedRow,
    recommend,
    selectedRecommend,
    summary,
    loadAreas,
    loadItemPolicy,
    runRecommend,
    resetForm,
    resetPage,
    addDraft,
    selectRow,
    removeDraft,
    submitInbound,
  };
}
