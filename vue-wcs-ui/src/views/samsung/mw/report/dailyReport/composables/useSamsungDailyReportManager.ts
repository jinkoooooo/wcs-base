import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import {
  exportSamsungDailyReportExcel,
  fetchSamsungDailyReportBcr,
  fetchSamsungDailyReportPalletized,
  fetchSamsungDailyReportSorter,
  fetchSamsungDailyReportSummary,
  fetchSamsungDailyReportTimeline,
  type DailyRawRowDto,
  type DailyReportSearchRequest,
  type DailyReportSummaryDto,
  type DailyTimelineRowDto,
} from '@/api/samsung/report';

export interface DailyReportFilters {
  todayDate: string;
  blNo: string;
  cntrNo: string;
}

export type DailyReportLoadingState = Record<string, boolean> & {
  search: boolean;
  export: boolean;
};

function createInitialFilters(): DailyReportFilters {
  return {
    todayDate: new Date().toISOString().slice(0, 10),
    blNo: '',
    cntrNo: '',
  };
}

function resolveList(payload: any): any[] {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.dataResult)) return payload.dataResult;
  return [];
}

function resolveObject(payload: any): any {
  if (!payload) return null;
  if (payload?.dataResult) return payload.dataResult;
  return payload;
}

function normalizeSummary(row: any): DailyReportSummaryDto {
  return row ?? {};
}

function normalizeTimelineRow(row: any): DailyTimelineRowDto {
  return row ?? {};
}

function normalizeRawRow(row: any): DailyRawRowDto {
  return row ?? {};
}

export function useSamsungDailyReportManager() {
  const route = useRoute();

  const filters = reactive<DailyReportFilters>(createInitialFilters());

  const summary = ref<DailyReportSummaryDto | null>(null);
  const timelineRows = ref<DailyTimelineRowDto[]>([]);
  const bcrRows = ref<DailyRawRowDto[]>([]);
  const sorterRows = ref<DailyRawRowDto[]>([]);
  const palletizedRows = ref<DailyRawRowDto[]>([]);

  const loading = reactive<DailyReportLoadingState>({
    search: false,
    export: false,
  });

  const requestPayload = computed<DailyReportSearchRequest>(() => ({
    todayDate: filters.todayDate,
    blNo: filters.blNo?.trim() || '',
    cntrNo: filters.cntrNo?.trim() || '',
  }));

  function applyRouteQueryToFilters() {
    const q = route.query;
    const date = typeof q.date === 'string' ? q.date : '';

    if (date) {
      filters.todayDate = date;
    }

    // 월별에서 BL / 컨테이너는 안 넘기므로 항상 비운 상태 유지
    filters.blNo = '';
    filters.cntrNo = '';
  }

  async function search() {
    loading.search = true;

    try {
      const [summaryPayload, timelinePayload, bcrPayload, sorterPayload, palletizedPayload] =
        await Promise.all([
          fetchSamsungDailyReportSummary(requestPayload.value),
          fetchSamsungDailyReportTimeline(requestPayload.value),
          fetchSamsungDailyReportBcr({
            ...requestPayload.value,
            processType: 'BCR',
          }),
          fetchSamsungDailyReportSorter({
            ...requestPayload.value,
            processType: 'SORTER',
          }),
          fetchSamsungDailyReportPalletized({
            ...requestPayload.value,
            processType: 'PALLETIZED',
          }),
        ]);

      summary.value = normalizeSummary(resolveObject(summaryPayload));
      timelineRows.value = resolveList(timelinePayload).map(normalizeTimelineRow);
      bcrRows.value = resolveList(bcrPayload).map(normalizeRawRow);
      sorterRows.value = resolveList(sorterPayload).map(normalizeRawRow);
      palletizedRows.value = resolveList(palletizedPayload).map(normalizeRawRow);
    } catch (error) {
      console.error(error);
      summary.value = null;
      timelineRows.value = [];
      bcrRows.value = [];
      sorterRows.value = [];
      palletizedRows.value = [];
      window.alert('일별 리포트 조회 중 오류가 발생했습니다.');
    } finally {
      loading.search = false;
    }
  }

  async function exportExcel() {
    loading.export = true;

    try {
      const blob = await exportSamsungDailyReportExcel(requestPayload.value);
      const url = window.URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `samsung_day_report_${filters.todayDate}.xlsx`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error(error);
      window.alert('엑셀 다운로드 중 오류가 발생했습니다.');
    } finally {
      loading.export = false;
    }
  }

  function resetFilters() {
    Object.assign(filters, createInitialFilters());
  }

  onMounted(async () => {
    applyRouteQueryToFilters();
    await search();
  });

  return {
    filters,
    summary,
    timelineRows,
    bcrRows,
    sorterRows,
    palletizedRows,
    loading,
    search,
    exportExcel,
    resetFilters,
  };
}
