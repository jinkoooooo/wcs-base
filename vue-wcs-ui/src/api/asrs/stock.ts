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

function resolveStockApiBaseUrl(): string {
  const directBaseUrl = String(env.VITE_STOCK_API_BASE_URL ?? '').trim();
  if (directBaseUrl) {
    return directBaseUrl.replace(/\/+$/, '');
  }

  const protocol =
    typeof window !== 'undefined' && window.location?.protocol
      ? window.location.protocol
      : 'http:';

  const hostname =
    typeof window !== 'undefined' && window.location?.hostname
      ? window.location.hostname
      : 'localhost';

  const port = String(env.VITE_STOCK_API_PORT ?? '9500').trim() || '9500';

  return `${protocol}//${hostname}:${port}`;
}

const STOCK_API_BASE_URL = resolveStockApiBaseUrl();

const STOCK_REQUEST_OPTIONS: RequestOptionOverrides = {
  joinPrefix: false,
  apiUrl: '',
  urlPrefix: '',
  errorMessageMode: 'message',
  joinTime: true,
};

const STOCK_COMMAND_OPTIONS: RequestOptionOverrides = {
  joinPrefix: false,
  apiUrl: '',
  urlPrefix: '',
  errorMessageMode: 'message',
  successMessageMode: 'message',
  joinTime: true,
};

function absoluteUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${STOCK_API_BASE_URL}${normalizedPath}`;
}

function stockGet<T = any>(path: string, params?: Record<string, any>) {
  return defHttp.get<T>(
    {
      url: absoluteUrl(path),
      params,
    },
    STOCK_REQUEST_OPTIONS,
  );
}

function stockPost<T = any>(path: string, data?: Record<string, any>) {
  return defHttp.post<T>(
    {
      url: absoluteUrl(path),
      params: data,
    },
    STOCK_COMMAND_OPTIONS,
  );
}

export interface StockUnitViewModel {
  stockUnitNo: string;
  itemCode?: string;
  itemName?: string;
  areaCode?: string;
  locationCode?: string;
  qty?: number;
  reservedQty?: number;
  availableQty?: number;
  lotNo?: string;
  stockStatusCode?: string;
  activeYn?: string;
  lastTxnAt?: string;
}

export interface StockHistoryRow {
  txnType?: string;
  txnAt?: string;
  fromLocationCode?: string;
  toLocationCode?: string;
  qty?: number;
  refDocType?: string;
  refDocNo?: string;
  refLineNo?: string;
  reasonCode?: string;
  remark?: string;
}

export interface StockAllocationRow {
  stockUnitNo?: string;
  itemCode?: string;
  allocatedQty?: number;
  allocStatusCode?: string;
  refDocType?: string;
  refDocNo?: string;
  refLineNo?: string;
  dueDate?: string;
  allocatedAt?: string;
  remark?: string;
}

export interface InboundStockRequest {
  areaCode: string;
  locationCode: string;
  stockUnitNo?: string;
  itemCode: string;
  lotNo?: string;
  qty: number;
  stockUnitType?: string;
  refDocType?: string;
  refDocNo?: string;
  refLineNo?: string;
  reasonCode?: string;
  remark?: string;
}

export interface StorageAreaOption {
  id?: string;
  areaCode: string;
  areaName?: string;
  areaType?: string;
  activeYn?: string;
}

export interface InboundLocationSelectRequest {
  areaCode: string;
  itemCode: string;
  qty: number;
  lotNo?: string;
}

export interface PutawayStockRequest {
  areaCode: string;
  stockUnitNo: string;
  toLocationCode: string;
  refDocType?: string;
  refDocNo?: string;
  refLineNo?: string;
  reasonCode?: string;
  remark?: string;
}

export interface MoveStockRequest {
  areaCode: string;
  stockUnitNo: string;
  toLocationCode: string;
  refDocType?: string;
  refDocNo?: string;
  refLineNo?: string;
  reasonCode?: string;
  remark?: string;
}

export interface AllocateStockRequest {
  stockUnitNo: string;
  itemCode?: string;
  allocatedQty: number;
  refDocType?: string;
  refDocNo?: string;
  refLineNo?: string;
  dueDate?: string;
}

export interface ReleaseAllocationRequest {
  stockUnitNo: string;
  refDocNo: string;
  refLineNo?: string;
  remark?: string;
}

export interface PartialOutRequest {
  stockUnitNo: string;
  outQty: number;
  refDocType?: string;
  refDocNo?: string;
  refLineNo?: string;
  reasonCode?: string;
  remark?: string;
}

export interface FullOutRequest {
  stockUnitNo: string;
  refDocType?: string;
  refDocNo?: string;
  refLineNo?: string;
  reasonCode?: string;
  remark?: string;
}

export interface ReturnInRequest {
  areaCode: string;
  toLocationCode: string;
  originalStockUnitNo: string;
  newStockUnitNo: string;
  itemCode: string;
  qty: number;
  refDocType?: string;
  refDocNo?: string;
  refLineNo?: string;
  reasonCode?: string;
  remark?: string;
}

export interface OutboundLocation2DExecuteForm {
  refDocType: string;
  refDocNo: string;
  refLineNo: string;
  reasonCode: string;
  remark: string;
}

enum Api {
  AllocationByStockUnitActive = '/rest/aislecore/stocks/allocations/active',

  Inbound = '/rest/aislecore/stocks/inbound',
  Putaway = '/rest/aislecore/stocks/putaway',
  Move = '/rest/aislecore/stocks/move',
  Allocate = '/rest/aislecore/stocks/allocations/allocate',
  ReleaseAllocation = '/rest/aislecore/stocks/allocations/release-allocation',
  PartialOut = '/rest/aislecore/stocks/partial-out',
  FullOut = '/rest/aislecore/stocks/full-out',
  ReturnIn = '/rest/aislecore/stocks/return-in',

  StockByUnit = '/rest/aislecore/stocks/current',
  StockByItem = '/rest/aislecore/stocks/current/by-item',
  StockByLocation = '/rest/aislecore/stocks/current/by-location',
  StockHistoryByUnit = '/rest/aislecore/stocks/history',
}

export function getStockByUnit(stockUnitNo: string) {
  return stockGet<any>(Api.StockByUnit, { stockUnitNo });
}

export function getStocksByItem(areaCode: string, itemCode: string) {
  return stockGet<any>(Api.StockByItem, { areaCode, itemCode });
}

export function getStocksByLocation(areaCode: string, locationCode: string) {
  return stockGet<any>(Api.StockByLocation, { areaCode, locationCode });
}

export function getStockHistoryByUnit(stockUnitNo: string) {
  return stockGet<any>(Api.StockHistoryByUnit, { stockUnitNo });
}

export function getActiveAllocationsByStockUnit(stockUnitNo: string) {
  return stockGet<any>(Api.AllocationByStockUnitActive, { stockUnitNo });
}

export function postInboundStock(data: InboundStockRequest) {
  return stockPost<any>(Api.Inbound, data);
}

export function postPutawayStock(data: PutawayStockRequest) {
  return stockPost<any>(Api.Putaway, data);
}

export function postMoveStock(data: MoveStockRequest) {
  return stockPost<any>(Api.Move, data);
}

export function postAllocateStock(data: AllocateStockRequest) {
  return stockPost<any>(Api.Allocate, data);
}

export function postReleaseAllocation(data: ReleaseAllocationRequest) {
  return stockPost<any>(Api.ReleaseAllocation, data);
}

export function postPartialOut(data: PartialOutRequest) {
  return stockPost<any>(Api.PartialOut, data);
}

export function postFullOut(data: FullOutRequest) {
  return stockPost<any>(Api.FullOut, data);
}

export function postReturnIn(data: ReturnInRequest) {
  return stockPost<any>(Api.ReturnIn, data);
}

function stockGetSilent<T = any>(path: string, params?: Record<string, any>) {
  return defHttp.get<T>(
    {
      url: absoluteUrl(path),
      params,
    },
    {
      joinPrefix: false,
      apiUrl: '',
      urlPrefix: '',
      errorMessageMode: 'none',
      joinTime: true,
    },
  );
}

function stockPostSilent<T = any>(path: string, data?: Record<string, any>) {
  return defHttp.post<T>(
    {
      url: absoluteUrl(path),
      params: data,
    },
    {
      joinPrefix: false,
      apiUrl: '',
      urlPrefix: '',
      errorMessageMode: 'none',
      successMessageMode: 'none',
      joinTime: true,
    },
  );
}

/** 활성 Area 목록 조회 */
export function getActiveStorageAreas() {
  return stockGetSilent<any>('/rest/tb_ac_storage_area/active-list');
}

/** 입고 추천 로케이션 조회 */
export function selectInboundLocation(data: InboundLocationSelectRequest) {
  return stockPostSilent<any>('/rest/aislecore/strategy/select-inbound-location', data);
}

/** 입고 상품 정책 조회 */
export function getInboundItemPolicy(areaCode: string, itemCode: string) {
  return stockGetSilent<any>('/rest/aislecore/items/inbound-policy', {
    areaCode,
    itemCode,
  });
}

/**
 * 출고 대상 현재고 전체 조회
 *
 * 규칙:
 * - areaCode 없으면 전체 영역
 * - 기본적으로 activeYn=Y, OUT 제외
 */
export function getAllCurrentStocks(areaCode?: string) {
  const params: Record<string, any> = {
    activeYn: 'Y',
    excludeOutYn: 'Y',
  };

  if (areaCode) {
    params.areaCode = areaCode;
  }

  return stockGet<any>('/rest/aislecore/stocks/current/all', params);
}


/**
 * 2D 로케이션 지정출고 화면용 API
 *
 * 주의:
 * - 별도 api.ts 를 만들지 않고 기존 stock.ts 공통 wrapper 재사용
 * - 이렇게 해야 기존 재고/출고/입고 화면과 동일한 base path / 옵션을 탄다.
 */

/** Area 목록 조회 (입고 화면과 동일) */
export function getOutboundLocation2DAreas() {
  return getActiveStorageAreas();
}

/** area 기준 aisle 목록 조회 */
export function getOutboundLocation2DAisles(areaCode: string) {
  return stockGet<any[]>(
    `/rest/aislecore/stocks/outbound-location-2d/aisles`,
    { areaCode },
  );
}

/** area + aisle 기준 side 목록 조회 */
export function getOutboundLocation2DSides(areaCode: string, aisleNo: string | number) {
  return stockGet<any[]>(
    `/rest/aislecore/stocks/outbound-location-2d/sides`,
    { areaCode, aisleNo },
  );
}

/** 2D 맵 조회 */
export function getOutboundLocation2DMap(
  areaCode: string,
  aisleNo: string | number,
  sideCode: string,
) {
  return stockGet<any>(
    `/rest/aislecore/stocks/outbound-location-2d/map`,
    { areaCode, aisleNo, sideCode },
  );
}

/** 2D 지정출고 실행 */
export function postOutboundLocation2DExecute(data: {
  locationId: string;
  stockUnitNo: string;
  refDocType?: string;
  refDocNo?: string;
  refLineNo?: string;
  reasonCode?: string;
  remark?: string;
}) {
  return stockPost<void>(
    `/rest/aislecore/stocks/outbound-location-2d/execute`,
    data,
  );
}
