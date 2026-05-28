import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  fetchSamsungMonthlyReportSummary,
  type MonthlyReportSearchRequest,
  type MonthlyReportSummaryRowDto,
} from '@/api/samsung/report';

export type CalendarCell = {
  key: string;
  date: string;
  inMonth: boolean;
};

function ymdLocal(date: Date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function startOfMonth(yyyyMM: string) {
  const [y, m] = yyyyMM.split('-').map(Number);
  return new Date(y, m - 1, 1);
}

function endOfMonth(yyyyMM: string) {
  const [y, m] = yyyyMM.split('-').map(Number);
  return new Date(y, m, 0);
}

function pick<T = any>(obj: Record<string, any>, ...keys: string[]): T | undefined {
  for (const key of keys) {
    if (obj[key] !== undefined) return obj[key] as T;
  }
  return undefined;
}

function resolveList(payload: any): any[] {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.dataResult)) return payload.dataResult;
  return [];
}

export function useSamsungMonthlyReportManager() {
  const router = useRouter();

  const filters = reactive({
    month: new Date().toISOString().slice(0, 7),
  });

  const loading = ref(false);
  const monthlyRows = ref<MonthlyReportSummaryRowDto[]>([]);

  const requestPayload = computed<MonthlyReportSearchRequest>(() => ({
    month: filters.month,
  }));

  const currentMonthStart = computed(() => startOfMonth(filters.month));
  const currentMonthEnd = computed(() => endOfMonth(filters.month));

  const calendarCells = computed<CalendarCell[]>(() => {
    const start = currentMonthStart.value;
    const end = currentMonthEnd.value;
    const offset = start.getDay();
    const daysInMonth = end.getDate();

    const cells: CalendarCell[] = [];

    for (let i = 0; i < offset; i++) {
      const d = new Date(start);
      d.setDate(d.getDate() - (offset - i));
      cells.push({
        key: `prev-${i}`,
        date: ymdLocal(d),
        inMonth: false,
      });
    }

    for (let day = 1; day <= daysInMonth; day++) {
      const d = new Date(start);
      d.setDate(day);
      cells.push({
        key: `cur-${day}`,
        date: ymdLocal(d),
        inMonth: true,
      });
    }

    const remain = (7 - (cells.length % 7)) % 7;
    for (let i = 1; i <= remain; i++) {
      const d = new Date(end);
      d.setDate(end.getDate() + i);
      cells.push({
        key: `next-${i}`,
        date: ymdLocal(d),
        inMonth: false,
      });
    }

    return cells;
  });

  const daySummaryMap = computed<Record<string, MonthlyReportSummaryRowDto>>(() => {
    const map: Record<string, MonthlyReportSummaryRowDto> = {};

    monthlyRows.value.forEach((row) => {
      const reportDate = pick<string>(row as any, 'reportDate', 'report_date') || '';
      if (!reportDate) return;
      map[reportDate] = row;
    });

    return map;
  });

  async function fetchMonthlySummary() {
    loading.value = true;
    try {
      const payload = await fetchSamsungMonthlyReportSummary(requestPayload.value);
      monthlyRows.value = resolveList(payload) as MonthlyReportSummaryRowDto[];
    } catch (e) {
      console.error('fetchMonthlySummary error:', e);
      monthlyRows.value = [];
    } finally {
      loading.value = false;
    }
  }

  function openDailyReport(date: string) {
    router.push({
      path: '/report/dailyReport',
      query: { date },
    });
  }

  function resetFilters() {
    filters.month = new Date().toISOString().slice(0, 7);
  }

  onMounted(fetchMonthlySummary);

  return {
    filters,
    loading,
    monthlyRows,
    calendarCells,
    daySummaryMap,
    fetchMonthlySummary,
    openDailyReport,
    resetFilters,
  };
}
