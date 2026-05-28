/**
 * stock-overview 전용 composable
 *
 * 목적:
 * - index.vue 에서 상태/비즈니스 로직 분리
 * - 검색 / 이력 조회 / 선택 / 리셋 공통 관리
 * - Area 드롭다운 옵션 공통 로딩
 *
 * 변경 사항:
 * - 재고번호 조회는 기존 그대로 stockUnitNo 필수
 * - 품목 조회는 itemCode 없으면 전체조회
 * - 위치 조회는 locationCode 없으면 전체조회
 * - 전체조회는 stock.ts 의 getAllCurrentStocks() 재사용
 * - Area 목록은 inbound 화면과 동일하게 getActiveStorageAreas() 사용
 */

import { computed, onMounted, reactive, ref } from 'vue';
import {
  getActiveStorageAreas,
  getAllCurrentStocks,
  getStockByUnit,
  getStockHistoryByUnit,
  getStocksByItem,
  getStocksByLocation,
} from '@/api/asrs/stock';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import { normalizeCode, normalizeMessage } from '@/views/asrs/shared/utils/normalize';
import type { AreaCodeOption } from '@/views/asrs/shared/components/form/AsrsAreaCodeField.vue';
import {
  normalizeStockOverviewHistoryRows,
  normalizeStockOverviewRows,
  normalizeStockOverviewSingle,
} from '../mappers/stockOverview.mapper';
import type {
  StockOverviewFilters,
  StockOverviewHistoryRow,
  StockOverviewLoadingState,
  StockOverviewRow,
  StockOverviewSearchMode,
  StockOverviewSearchModeOption,
  StockOverviewSummary,
} from '../types';

const SEARCH_MODE_OPTIONS: StockOverviewSearchModeOption[] = [
  { label: '재고번호 조회', value: 'stockUnit' },
  { label: '품목 조회', value: 'item' },
  { label: '위치 조회', value: 'location' },
];

/**
 * 초기 검색 조건
 */
function createInitialFilters(): StockOverviewFilters {
  return {
    areaCode: '',
    stockUnitNo: '',
    itemCode: '',
    locationCode: '',
  };
}

export function useStockOverview() {
  const searchMode = ref<StockOverviewSearchMode>('stockUnit');
  const filters = reactive<StockOverviewFilters>(createInitialFilters());

  /**
   * 주의:
   * - 기존 search / history 외에 areas 로딩 플래그 추가
   */
  const { flags: loading } = useAsyncFlags<StockOverviewLoadingState>({
    search: false,
    history: false,
    areas: false,
  } as StockOverviewLoadingState);

  const { feedback, setFeedback, clearFeedback } = useFeedback();

  const rows = ref<StockOverviewRow[]>([]);
  const selectedStock = ref<StockOverviewRow | null>(null);
  const historyRows = ref<StockOverviewHistoryRow[]>([]);

  /**
   * Area dropdown 옵션
   *
   * AsrsAreaCodeField 가 기대하는 형식:
   * - areaCode
   * - areaName
   */
  const areaOptions = ref<AreaCodeOption[]>([]);

  const summary = computed<StockOverviewSummary>(() => {
    const totalCount = rows.value.length;
    const activeCount = rows.value.filter((row) => row.activeYn === 'Y').length;
    const totalQty = rows.value.reduce((sum, row) => sum + Number(row.qty ?? 0), 0);
    const totalReservedQty = rows.value.reduce(
      (sum, row) => sum + Number(row.reservedQty ?? 0),
      0,
    );

    return {
      totalCount,
      activeCount,
      totalQty,
      totalReservedQty,
    };
  });

  const visibleFieldCount = computed(() => {
    if (searchMode.value === 'stockUnit') return 1;
    if (searchMode.value === 'item') return 2;
    if (searchMode.value === 'location') return 2;
    return 1;
  });

  /**
   * Area 목록 조회
   *
   * inbound 화면과 동일한 API 사용
   */
  async function loadAreas() {
    loading.areas = true;

    try {
      const payload = await getActiveStorageAreas();

      /**
       * payload 가 배열이라고 가정하고 areaCode / areaName 을 공통 형식으로 normalize
       */
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

  /**
   * 검색 모드 변경
   *
   * 규칙:
   * - 모드 전환 시 기존 결과/이력/피드백 초기화
   * - 검색 조건도 초기화
   */
  function changeSearchMode(mode: StockOverviewSearchMode) {
    searchMode.value = mode;
    Object.assign(filters, createInitialFilters());
    rows.value = [];
    selectedStock.value = null;
    historyRows.value = [];
    clearFeedback();
  }

  /**
   * 선택 재고 이력 조회
   */
  async function loadHistory(stockUnitNo: string) {
    if (!stockUnitNo) {
      historyRows.value = [];
      return;
    }

    loading.history = true;

    try {
      const payload = await getStockHistoryByUnit(stockUnitNo);
      historyRows.value = normalizeStockOverviewHistoryRows(payload);
    } catch (error: any) {
      console.error(error);

      const errorCode = normalizeCode(error?.response?.data);

      if (errorCode === 'ENTITY_NOT_FOUND') {
        historyRows.value = [];
        return;
      }

      historyRows.value = [];
    } finally {
      loading.history = false;
    }
  }

  /**
   * row 선택
   */
  async function selectRow(row: StockOverviewRow) {
    selectedStock.value = row;
    await loadHistory(row.stockUnitNo);
  }

  /**
   * 조회조건 검증
   *
   * 규칙:
   * - stockUnit 만 stockUnitNo 필수
   * - item/location 은 개별 검색값 없으면 전체조회 허용
   * - item/location 에서 개별 검색값이 있으면 areaCode 선택 필요
   */
  function validateSearch() {
    if (searchMode.value === 'stockUnit' && !filters.stockUnitNo.trim()) {
      setFeedback('warning', 'Stock Unit No를 입력해주세요.');
      return false;
    }

    if (searchMode.value === 'item' && filters.itemCode.trim() && !filters.areaCode.trim()) {
      setFeedback('warning', '품목 조회 시 Area를 선택해주세요.');
      return false;
    }

    if (
      searchMode.value === 'location' &&
      filters.locationCode.trim() &&
      !filters.areaCode.trim()
    ) {
      setFeedback('warning', '위치 조회 시 Area를 선택해주세요.');
      return false;
    }

    return true;
  }

  /**
   * 재고 조회 실행
   *
   * 규칙:
   * - stockUnit : 단건 조회
   * - item      : itemCode 있으면 품목조회 / 없으면 전체조회
   * - location  : locationCode 있으면 위치조회 / 없으면 전체조회
   */
  async function runSearch() {
    if (!validateSearch()) return;

    loading.search = true;
    selectedStock.value = null;
    historyRows.value = [];
    clearFeedback();

    try {
      if (searchMode.value === 'stockUnit') {
        const payload = await getStockByUnit(filters.stockUnitNo.trim());
        const single = normalizeStockOverviewSingle(payload);
        rows.value = single ? [single] : [];
      } else if (searchMode.value === 'item') {
        if (filters.itemCode.trim()) {
          const payload = await getStocksByItem(
            filters.areaCode.trim(),
            filters.itemCode.trim(),
          );
          rows.value = normalizeStockOverviewRows(payload);
        } else {
          /**
           * itemCode 비우면 전체조회
           * - areaCode 있으면 해당 area 전체
           * - 없으면 전체 area 전체
           */
          const payload = await getAllCurrentStocks(filters.areaCode.trim() || undefined);
          rows.value = normalizeStockOverviewRows(payload);
        }
      } else if (searchMode.value === 'location') {
        if (filters.locationCode.trim()) {
          const payload = await getStocksByLocation(
            filters.areaCode.trim(),
            filters.locationCode.trim(),
          );
          rows.value = normalizeStockOverviewRows(payload);
        } else {
          /**
           * locationCode 비우면 전체조회
           */
          const payload = await getAllCurrentStocks(filters.areaCode.trim() || undefined);
          rows.value = normalizeStockOverviewRows(payload);
        }
      }

      if (!rows.value.length) {
        setFeedback('warning', '조회 결과가 없습니다.');
        return;
      }

      /**
       * 첫 row 자동 선택 후 이력 조회
       */
      selectedStock.value = rows.value[0];
      await loadHistory(rows.value[0].stockUnitNo);

      const isAllSearch =
        (searchMode.value === 'item' && !filters.itemCode.trim()) ||
        (searchMode.value === 'location' && !filters.locationCode.trim());

      if (isAllSearch) {
        setFeedback('success', `전체 현재고 ${rows.value.length}건 조회되었습니다.`);
      } else {
        setFeedback('success', `총 ${rows.value.length}건 조회되었습니다.`);
      }
    } catch (error: any) {
      console.error(error);

      const errorCode = normalizeCode(error?.response?.data);
      const errorMsg = normalizeMessage(error?.response?.data);

      rows.value = [];
      selectedStock.value = null;
      historyRows.value = [];

      if (errorCode === 'ENTITY_NOT_FOUND') {
        setFeedback('warning', '조회 결과가 없습니다.');
        return;
      }

      if (errorCode === 'INVALID_REQUEST') {
        setFeedback('warning', errorMsg || '조회 조건이 올바르지 않습니다.');
        return;
      }

      setFeedback('error', errorMsg || '조회 중 오류가 발생했습니다.');
    } finally {
      loading.search = false;
    }
  }

  /**
   * 화면 초기화
   */
  function resetPage() {
    Object.assign(filters, createInitialFilters());
    rows.value = [];
    selectedStock.value = null;
    historyRows.value = [];
    clearFeedback();
    searchMode.value = 'stockUnit';
  }

  onMounted(() => {
    loadAreas();
  });

  return {
    searchModes: SEARCH_MODE_OPTIONS,
    searchMode,
    filters,
    loading,
    feedback,
    rows,
    selectedStock,
    historyRows,
    summary,
    visibleFieldCount,
    areaOptions,
    loadAreas,
    changeSearchMode,
    loadHistory,
    selectRow,
    runSearch,
    resetPage,
  };
}
