import { defHttp } from '/@/utils/http/axios';

type RequestOptionOverrides = {
  joinPrefix: boolean;
  apiUrl: string;
  urlPrefix: string;
  errorMessageMode: 'none' | 'modal' | 'message';
  successMessageMode?: 'none' | 'modal' | 'message';
  joinTime?: boolean;
};

const env = (import.meta as any).env ?? {};

/**
 * Samsung report API base url 계산
 */
function resolveSamsungReportApiBaseUrl(): string {
  const directBaseUrl = String(env.VITE_STOCK_API_BASE_URL ?? '').trim();

  if (directBaseUrl) {
    return directBaseUrl.replace(/\/+$/, '');
  }

  const protocol =
    typeof window !== 'undefined' && window.location?.protocol ? window.location.protocol : 'http:';

  const hostname =
    typeof window !== 'undefined' && window.location?.hostname
      ? window.location.hostname
      : 'localhost';

  const port = String(env.VITE_STOCK_API_PORT ?? '9500').trim() || '9500';

  return `${protocol}//${hostname}:${port}`;
}

const SAMSUNG_REPORT_API_BASE_URL = resolveSamsungReportApiBaseUrl();

const REPORT_REQUEST_OPTIONS: RequestOptionOverrides = {
  joinPrefix: false,
  apiUrl: '',
  urlPrefix: '',
  errorMessageMode: 'message',
  joinTime: true,
};

const REPORT_COMMAND_OPTIONS: RequestOptionOverrides = {
  joinPrefix: false,
  apiUrl: '',
  urlPrefix: '',
  errorMessageMode: 'message',
  successMessageMode: 'none',
  joinTime: true,
};

function absoluteUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${SAMSUNG_REPORT_API_BASE_URL}${normalizedPath}`;
}

/**
 * RequestBody POST 전용 helper
 */
function reportPostBody<T = any>(path: string, data?: Record<string, any>) {
  return defHttp.post<T>(
    {
      url: absoluteUrl(path),
      data,
    },
    REPORT_COMMAND_OPTIONS,
  );
}

/**
 * 바이너리 다운로드용 helper
 */
function reportPostBlob(path: string, data?: Record<string, any>) {
  return defHttp.post<any>(
    {
      url: absoluteUrl(path),
      data,
      responseType: 'blob',
    },
    {
      ...REPORT_COMMAND_OPTIONS,
      errorMessageMode: 'message',
    },
  );
}

/* -------------------------------------------------------------------------- */
/* Daily Report                                                                */
/* -------------------------------------------------------------------------- */

export interface DailyReportSearchRequest {
  todayDate: string;
  blNo?: string;
  cntrNo?: string;
  excludeManualYn?: string;
}

export interface DailyReportRawRequest extends DailyReportSearchRequest {
  processType?: string;
}

export interface DailyReportSummaryDto {
  todayDate?: string;
  today_date?: string;
  blNo?: string;
  bl_no?: string;
  cntrNo?: string;
  cntr_no?: string;

  totalBoxQty?: number;
  total_box_qty?: number;
  doneBoxQty?: number;
  done_box_qty?: number;
  okBoxQty?: number;
  ok_box_qty?: number;
  ngBoxQty?: number;
  ng_box_qty?: number;
  pendingBoxQty?: number;
  pending_box_qty?: number;

  jobSkuQty?: number;
  job_sku_qty?: number;
  actualSkuQty?: number;
  actual_sku_qty?: number;

  firstReceivedAt?: string;
  first_received_at?: string;
  lastPalletizedAt?: string;
  last_palletized_at?: string;

  totalOperatingTime?: string;
  total_operating_time?: string;
  idleTime?: string;
  idle_time?: string;
  palletOperatingTime?: string;
  pallet_operating_time?: string;

  totalTimeUph?: number;
  total_time_uph?: number;
  palletTimeUph?: number;
  pallet_time_uph?: number;
  inboundNetTimeUph?: number;
  inbound_net_time_uph?: number;
  ngRatePct?: number;
  ng_rate_pct?: number;

  avgAllTime?: string;
  avg_all_time?: string;
  medianTime?: string;
  median_time?: string;
  p95Time?: string;
  p95_time?: string;
  avgExclP95Time?: string;
  avg_excl_p95_time?: string;
  minTime?: string;
  min_time?: string;
  maxTime?: string;
  max_time?: string;

  summaryText?: string;
  summary_text?: string;
}

export interface DailyTimelineRowDto {
  rowType?: string;
  row_type?: string;
  rowGroup?: string;
  row_group?: string;
  processType?: string;
  process_type?: string;

  blNo?: string;
  bl_no?: string;
  cntrNo?: string;
  cntr_no?: string;

  boxId?: string;
  box_id?: string;
  parcelId?: string;
  parcel_id?: string;
  plcSeqNo?: string;
  plc_seq_no?: string;
  itemCode?: string;
  item_code?: string;

  startAt?: string;
  start_at?: string;
  endAt?: string;
  end_at?: string;
  durationSec?: number;
  duration_sec?: number;

  tooltipTitle?: string;
  tooltip_title?: string;
  tooltipSub1?: string;
  tooltip_sub1?: string;
  tooltipSub2?: string;
  tooltip_sub2?: string;
}

export interface DailyRawRowDto {
  [key: string]: any;
}

/**
 * 일별 리포트 요약
 */
export function fetchSamsungDailyReportSummary(data: DailyReportSearchRequest) {
  return reportPostBody<DailyReportSummaryDto>('/rest/report/daily/summary', data);
}

/**
 * 일별 리포트 타임라인
 */
export function fetchSamsungDailyReportTimeline(data: DailyReportSearchRequest) {
  return reportPostBody<DailyTimelineRowDto[]>('/rest/report/daily/timeline', data);
}

/**
 * BCR 원본
 */
export function fetchSamsungDailyReportBcr(data: DailyReportRawRequest) {
  return reportPostBody<DailyRawRowDto[]>('/rest/report/daily/bcr', data);
}

/**
 * SORTER 원본
 */
export function fetchSamsungDailyReportSorter(data: DailyReportRawRequest) {
  return reportPostBody<DailyRawRowDto[]>('/rest/report/daily/sorter', data);
}

/**
 * PALLETIZED 원본
 */
export function fetchSamsungDailyReportPalletized(data: DailyReportRawRequest) {
  return reportPostBody<DailyRawRowDto[]>('/rest/report/daily/palletized', data);
}

/**
 * 통합 엑셀 다운로드
 */
export function exportSamsungDailyReportExcel(data: DailyReportSearchRequest) {
  return reportPostBlob('/rest/report/daily/export', data);
}

/* -------------------------------------------------------------------------- */
/* Monthly Report                                                              */
/* -------------------------------------------------------------------------- */

export interface MonthlyReportSearchRequest {
  month: string;
}

export interface MonthlyReportSummaryRowDto {
  reportDate?: string;
  report_date?: string;

  totalBoxQty?: number;
  total_box_qty?: number;

  okBoxQty?: number;
  ok_box_qty?: number;

  ngBoxQty?: number;
  ng_box_qty?: number;

  pendingBoxQty?: number;
  pending_box_qty?: number;

  totalTimeUph?: number | null;
  total_time_uph?: number | null;

  palletTimeUph?: number | null;
  pallet_time_uph?: number | null;

  totalOperatingTime?: string | null;
  total_operating_time?: string | null;

  palletOperatingTime?: string | null;
  pallet_operating_time?: string | null;

  firstReceivedAt?: string | null;
  first_received_at?: string | null;

  lastPalletizedAt?: string | null;
  last_palletized_at?: string | null;
}

/**
 * GET query helper
 */
function reportGetQuery<T = any>(path: string, params?: Record<string, any>) {
  return defHttp.get<T>(
    {
      url: absoluteUrl(path),
      params,
    },
    REPORT_REQUEST_OPTIONS,
  );
}

/**
 * 월별 리포트 일자 요약
 */
export function fetchSamsungMonthlyReportSummary(params: MonthlyReportSearchRequest) {
  return reportGetQuery<MonthlyReportSummaryRowDto[]>(
    '/rest/report/monthly/summary',
    params,
  );
}
