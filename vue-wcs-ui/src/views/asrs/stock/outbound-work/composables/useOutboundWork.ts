import { reactive, ref, onMounted } from 'vue';
import {
  getActiveAllocationsByStockUnit,
  getActiveStorageAreas,
  getAllCurrentStocks,
  getStockHistoryByUnit,
  getStockByUnit,
  getStocksByItem,
  getStocksByLocation,
  postAllocateStock,
  postFullOut,
  postPartialOut,
  postReleaseAllocation,
} from '@/api/asrs/stock';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import { normalizeMessage } from '@/views/asrs/shared/utils/normalize';
import type { AreaCodeOption } from '@/views/asrs/shared/components/form/AsrsAreaCodeField.vue';
import {
  normalizeOutboundAllocationRows,
  normalizeOutboundAutoCandidateRows,
  normalizeOutboundHistoryRows,
  normalizeOutboundStockRow,
  normalizeOutboundStockRows,
} from '../mappers/outboundWork.mapper';
import type {
  OutboundAllocationRow,
  OutboundAutoCandidateRow,
  OutboundHistoryRow,
  OutboundStockRow,
  OutboundWorkForm,
  OutboundWorkLoadingState,
  OutboundWorkTab,
} from '../types';

function createInitialForm(): OutboundWorkForm {
  return {
    areaCode: '',
    itemCode: '',
    locationCode: '',
    stockUnitNo: '',
    outQty: null,
    allocatedQty: null,
    refDocType: 'ORDER',
    refDocNo: '',
    refLineNo: '',
    dueDate: '',
    reasonCode: '',
    remark: '',
  };
}

/**
 * 후보 정렬 규칙
 *
 * 현재 2차 1단계는 프론트 정렬 기준:
 * - 사용 가능 재고 우선
 * - availableQty 큰 순
 * - lot 있는 건 앞쪽
 * - locationCode 오름차순
 */
function sortAutoCandidates(rows: OutboundAutoCandidateRow[]): OutboundAutoCandidateRow[] {
  return [...rows]
    .sort((a, b) => {
      const selectableDiff = Number(b.selectable) - Number(a.selectable);
      if (selectableDiff !== 0) return selectableDiff;

      const availableDiff = b.availableQty - a.availableQty;
      if (availableDiff !== 0) return availableDiff;

      const lotDiff = Number(!!b.lotNo) - Number(!!a.lotNo);
      if (lotDiff !== 0) return lotDiff;

      return String(a.locationCode).localeCompare(String(b.locationCode));
    })
    .map((row, index) => ({
      ...row,
      candidateRank: index + 1,
      candidateReason: row.availableQty > 0 ? '가용재고 우선' : '가용재고 부족',
      selectable: row.availableQty > 0 && row.activeYn === 'Y',
    }));
}

export function useOutboundWork() {
  const activeTab = ref<OutboundWorkTab>('auto');

  const form = reactive<OutboundWorkForm>(createInitialForm());

  const stockRows = ref<OutboundStockRow[]>([]);
  const autoCandidateRows = ref<OutboundAutoCandidateRow[]>([]);

  const selectedStock = ref<OutboundStockRow | null>(null);
  const allocationRows = ref<OutboundAllocationRow[]>([]);
  const selectedAllocation = ref<OutboundAllocationRow | null>(null);
  const historyRows = ref<OutboundHistoryRow[]>([]);

  /** Area dropdown 옵션 */
  const areaOptions = ref<AreaCodeOption[]>([]);

  const { flags: loading } = useAsyncFlags<OutboundWorkLoadingState>({
    areas: false,
    search: false,
    detail: false,
    autoSearch: false,
    allocate: false,
    partialOut: false,
    fullOut: false,
    release: false,
  });

  const { feedback, setFeedback, clearFeedback } = useFeedback();

  /**
   * Area 목록 조회
   *
   * 입고/재고현황과 동일하게 active storage areas 사용
   */
  async function loadAreas() {
    loading.areas = true;

    try {
      const payload = await getActiveStorageAreas();

      areaOptions.value = Array.isArray(payload)
        ? payload
          .map((area: any) => {
            const areaCode = area.areaCode || area.area_code || '';
            const areaName = area.areaName || area.area_name || '';

            if (!areaCode) return null;

            return {
              areaCode,
              areaName,
            };
          })
          .filter(Boolean) as AreaCodeOption[]
        : [];
    } catch (error) {
      console.error(error);
      areaOptions.value = [];
      setFeedback('error', 'Area 목록을 불러오지 못했습니다.');
    } finally {
      loading.areas = false;
    }
  }

  function resetForm() {
    const initial = createInitialForm();
    Object.keys(initial).forEach((key) => {
      (form as any)[key] = (initial as any)[key];
    });
  }

  function initialize() {
    resetForm();
    stockRows.value = [];
    autoCandidateRows.value = [];
    selectedStock.value = null;
    allocationRows.value = [];
    selectedAllocation.value = null;
    historyRows.value = [];
    clearFeedback();
  }

  function changeTab(tab: OutboundWorkTab) {
    activeTab.value = tab;
    stockRows.value = [];
    autoCandidateRows.value = [];
    selectedStock.value = null;
    allocationRows.value = [];
    selectedAllocation.value = null;
    historyRows.value = [];
    clearFeedback();
  }

  async function runSearch() {
    loading.search = true;

    try {
      if (activeTab.value === 'location') {
        if (form.locationCode) {
          if (!form.areaCode) {
            setFeedback('warning', 'Location 조회 시 Area를 선택해주세요.');
            return;
          }

          const payload = await getStocksByLocation(form.areaCode, form.locationCode);
          stockRows.value = normalizeOutboundStockRows(payload);
        } else {
          const payload = await getAllCurrentStocks(form.areaCode || undefined);
          stockRows.value = normalizeOutboundStockRows(payload);
        }
      } else if (activeTab.value === 'item') {
        if (form.itemCode) {
          if (!form.areaCode) {
            setFeedback('warning', 'Item 조회 시 Area를 선택해주세요.');
            return;
          }

          const payload = await getStocksByItem(form.areaCode, form.itemCode);
          stockRows.value = normalizeOutboundStockRows(payload);
        } else {
          const payload = await getAllCurrentStocks(form.areaCode || undefined);
          stockRows.value = normalizeOutboundStockRows(payload);
        }
      } else {
        if (!form.stockUnitNo) {
          setFeedback('warning', 'Stock Unit No를 입력해주세요.');
          return;
        }

        const payload = await getStockByUnit(form.stockUnitNo);
        const single = normalizeOutboundStockRow(payload);
        stockRows.value = single.stockUnitNo ? [single] : [];
      }

      autoCandidateRows.value = [];
      selectedStock.value = null;
      allocationRows.value = [];
      selectedAllocation.value = null;
      historyRows.value = [];

      if (!stockRows.value.length) {
        setFeedback('warning', '조회 결과가 없습니다.');
        return;
      }

      setFeedback('success', `${stockRows.value.length}건 조회되었습니다.`);
    } catch (error: any) {
      console.error(error);
      stockRows.value = [];
      const message = normalizeMessage(error?.response?.data);
      setFeedback('error', message || '재고 조회 중 오류가 발생했습니다.');
    } finally {
      loading.search = false;
    }
  }

  /**
   * 자동할당 후보 조회
   *
   * 현재는 기존 item 조회 API 재사용 + 프론트 정렬
   */
  async function runAutoSearch() {
    if (!form.areaCode) {
      setFeedback('warning', 'Area를 선택해주세요.');
      return;
    }

    if (!form.itemCode) {
      setFeedback('warning', 'Item Code를 입력해주세요.');
      return;
    }

    if (!form.outQty || form.outQty <= 0) {
      setFeedback('warning', 'Out Qty를 입력해주세요.');
      return;
    }

    loading.autoSearch = true;

    try {
      const payload = await getStocksByItem(form.areaCode, form.itemCode);
      const normalized = normalizeOutboundAutoCandidateRows(payload);

      autoCandidateRows.value = sortAutoCandidates(normalized);
      stockRows.value = [];
      selectedStock.value = null;
      allocationRows.value = [];
      selectedAllocation.value = null;
      historyRows.value = [];

      if (!autoCandidateRows.value.length) {
        setFeedback('warning', '자동할당 후보가 없습니다.');
        return;
      }

      setFeedback('success', `자동할당 후보 ${autoCandidateRows.value.length}건 조회되었습니다.`);
    } catch (error: any) {
      console.error(error);
      autoCandidateRows.value = [];
      const message = normalizeMessage(error?.response?.data);
      setFeedback('error', message || '자동할당 후보 조회 중 오류가 발생했습니다.');
    } finally {
      loading.autoSearch = false;
    }
  }

  async function selectStock(row: OutboundStockRow) {
    selectedStock.value = row;
    selectedAllocation.value = null;

    if (!form.itemCode && row.itemCode) {
      form.itemCode = row.itemCode;
    }

    if (!form.stockUnitNo && row.stockUnitNo) {
      form.stockUnitNo = row.stockUnitNo;
    }

    await loadSelectedStockDetails(row.stockUnitNo);
  }

  async function selectAutoCandidate(row: OutboundAutoCandidateRow) {
    if (!row.selectable) return;

    selectedStock.value = row;
    selectedAllocation.value = null;
    form.stockUnitNo = row.stockUnitNo;

    if (!form.allocatedQty || form.allocatedQty <= 0) {
      form.allocatedQty = form.outQty;
    }

    await loadSelectedStockDetails(row.stockUnitNo);
  }

  async function loadSelectedStockDetails(stockUnitNo: string) {
    if (!stockUnitNo) return;

    loading.detail = true;

    try {
      const [allocationResult, historyResult] = await Promise.allSettled([
        getActiveAllocationsByStockUnit(stockUnitNo),
        getStockHistoryByUnit(stockUnitNo),
      ]);

      if (allocationResult.status === 'fulfilled') {
        allocationRows.value = normalizeOutboundAllocationRows(allocationResult.value);
      } else {
        console.error(allocationResult.reason);
        allocationRows.value = [];
      }

      if (historyResult.status === 'fulfilled') {
        historyRows.value = normalizeOutboundHistoryRows(historyResult.value);
      } else {
        console.error(historyResult.reason);
        historyRows.value = [];
      }

      if (
        allocationResult.status === 'rejected' &&
        historyResult.status === 'rejected'
      ) {
        const message = normalizeMessage(
          (historyResult as PromiseRejectedResult).reason?.response?.data,
        );
        setFeedback('error', message || '선택 재고 상세 조회 중 오류가 발생했습니다.');
        return;
      }
    } finally {
      loading.detail = false;
    }
  }

  function selectAllocation(row: OutboundAllocationRow) {
    selectedAllocation.value = row;
  }

  async function allocateSelectedStock() {
    if (!selectedStock.value) {
      setFeedback('warning', '할당할 재고를 먼저 선택해주세요.');
      return;
    }

    if (!form.allocatedQty || form.allocatedQty <= 0) {
      setFeedback('warning', '할당 수량을 입력해주세요.');
      return;
    }

    loading.allocate = true;

    try {
      await postAllocateStock({
        stockUnitNo: selectedStock.value.stockUnitNo,
        itemCode: selectedStock.value.itemCode || undefined,
        allocatedQty: Number(form.allocatedQty),
        refDocType: form.refDocType || undefined,
        refDocNo: form.refDocNo || undefined,
        refLineNo: form.refLineNo || undefined,
        dueDate: form.dueDate || undefined,
      });

      setFeedback('success', '재고 할당이 완료되었습니다.');
      await loadSelectedStockDetails(selectedStock.value.stockUnitNo);

      if (activeTab.value === 'auto') {
        await runAutoSearch();
      } else {
        await runSearch();
      }
    } catch (error: any) {
      console.error(error);
      const message = normalizeMessage(error?.response?.data);
      setFeedback('error', message || '재고 할당 중 오류가 발생했습니다.');
    } finally {
      loading.allocate = false;
    }
  }

  async function partialOutSelectedStock() {
    if (!selectedStock.value) {
      setFeedback('warning', '부분출고할 재고를 먼저 선택해주세요.');
      return;
    }

    if (!form.outQty || form.outQty <= 0) {
      setFeedback('warning', '부분출고 수량을 입력해주세요.');
      return;
    }

    loading.partialOut = true;

    try {
      await postPartialOut({
        stockUnitNo: selectedStock.value.stockUnitNo,
        outQty: Number(form.outQty),
        refDocType: form.refDocType || undefined,
        refDocNo: form.refDocNo || undefined,
        refLineNo: form.refLineNo || undefined,
        reasonCode: form.reasonCode || undefined,
        remark: form.remark || undefined,
      });

      setFeedback('success', '부분출고가 완료되었습니다.');
      await loadSelectedStockDetails(selectedStock.value.stockUnitNo);

      if (activeTab.value === 'auto') {
        await runAutoSearch();
      } else {
        await runSearch();
      }
    } catch (error: any) {
      console.error(error);
      const message = normalizeMessage(error?.response?.data);
      setFeedback('error', message || '부분출고 중 오류가 발생했습니다.');
    } finally {
      loading.partialOut = false;
    }
  }

  async function fullOutSelectedStock() {
    if (!selectedStock.value) {
      setFeedback('warning', '전체출고할 재고를 먼저 선택해주세요.');
      return;
    }

    loading.fullOut = true;

    try {
      await postFullOut({
        stockUnitNo: selectedStock.value.stockUnitNo,
        refDocType: form.refDocType || undefined,
        refDocNo: form.refDocNo || undefined,
        refLineNo: form.refLineNo || undefined,
        reasonCode: form.reasonCode || undefined,
        remark: form.remark || undefined,
      });

      setFeedback('success', '전체출고가 완료되었습니다.');
      await loadSelectedStockDetails(selectedStock.value.stockUnitNo);

      if (activeTab.value === 'auto') {
        await runAutoSearch();
      } else {
        await runSearch();
      }
    } catch (error: any) {
      console.error(error);
      const message = normalizeMessage(error?.response?.data);
      setFeedback('error', message || '전체출고 중 오류가 발생했습니다.');
    } finally {
      loading.fullOut = false;
    }
  }

  async function releaseSelectedAllocation() {
    if (!selectedStock.value) {
      setFeedback('warning', '재고를 먼저 선택해주세요.');
      return;
    }

    if (!selectedAllocation.value?.refDocNo) {
      setFeedback('warning', '해제할 할당 건을 먼저 선택해주세요.');
      return;
    }

    loading.release = true;

    try {
      await postReleaseAllocation({
        stockUnitNo: selectedStock.value.stockUnitNo,
        refDocNo: selectedAllocation.value.refDocNo,
        refLineNo: selectedAllocation.value.refLineNo || undefined,
        remark: form.remark || undefined,
      });

      setFeedback('success', '할당 해제가 완료되었습니다.');
      await loadSelectedStockDetails(selectedStock.value.stockUnitNo);

      if (activeTab.value === 'auto') {
        await runAutoSearch();
      } else {
        await runSearch();
      }
    } catch (error: any) {
      console.error(error);
      const message = normalizeMessage(error?.response?.data);
      setFeedback('error', message || '할당 해제 중 오류가 발생했습니다.');
    } finally {
      loading.release = false;
    }
  }

  onMounted(() => {
    loadAreas();
  });

  return {
    activeTab,
    form,
    areaOptions,
    stockRows,
    autoCandidateRows,
    selectedStock,
    allocationRows,
    selectedAllocation,
    historyRows,
    loading,
    feedback,
    initialize,
    resetForm,
    changeTab,
    loadAreas,
    runSearch,
    runAutoSearch,
    selectStock,
    selectAutoCandidate,
    selectAllocation,
    allocateSelectedStock,
    partialOutSelectedStock,
    fullOutSelectedStock,
    releaseSelectedAllocation,
  };
}
