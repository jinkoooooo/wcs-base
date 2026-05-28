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
 * item API base url 계산.
 *
 * stock.ts 와 동일 정책:
 * - VITE_ITEM_API_BASE_URL 이 있으면 우선 사용
 * - 없으면 현재 브라우저 host 기준 + 9500 포트 사용
 *
 * 목적:
 * - 프론트(3100) 상대경로 호출이 아니라
 *   백엔드(9500) 절대경로 호출로 고정
 */
function resolveItemApiBaseUrl(): string {
  const directBaseUrl = String(
    env.VITE_ITEM_API_BASE_URL
    ?? env.VITE_STOCK_API_BASE_URL
    ?? '',
  ).trim();

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

  const port = String(
    env.VITE_ITEM_API_PORT
    ?? env.VITE_STOCK_API_PORT
    ?? '9500',
  ).trim() || '9500';

  return `${protocol}//${hostname}:${port}`;
}

const ITEM_API_BASE_URL = resolveItemApiBaseUrl();

/**
 * 일반 조회 옵션
 */
const ITEM_REQUEST_OPTIONS: RequestOptionOverrides = {
  joinPrefix: false,
  apiUrl: '',
  urlPrefix: '',
  errorMessageMode: 'message',
  joinTime: true,
};

/**
 * 명령(create/update/delete/bulk) 옵션
 */
const ITEM_COMMAND_OPTIONS: RequestOptionOverrides = {
  joinPrefix: false,
  apiUrl: '',
  urlPrefix: '',
  errorMessageMode: 'message',
  successMessageMode: 'message',
  joinTime: true,
};

/**
 * 조용한 조회 옵션
 *
 * 목적:
 * - option 목록처럼 실패 시 화면에서 직접 제어하고 싶은 API에 사용 가능
 */
const ITEM_SILENT_REQUEST_OPTIONS: RequestOptionOverrides = {
  joinPrefix: false,
  apiUrl: '',
  urlPrefix: '',
  errorMessageMode: 'none',
  joinTime: true,
};

/**
 * 조용한 명령 옵션
 */
const ITEM_SILENT_COMMAND_OPTIONS: RequestOptionOverrides = {
  joinPrefix: false,
  apiUrl: '',
  urlPrefix: '',
  errorMessageMode: 'none',
  successMessageMode: 'none',
  joinTime: true,
};

/**
 * 절대 URL 생성
 */
function absoluteUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${ITEM_API_BASE_URL}${normalizedPath}`;
}

/**
 * 공통 GET
 */
function itemGet<T = any>(path: string, params?: Record<string, any>) {
  return defHttp.get<T>(
    {
      url: absoluteUrl(path),
      params,
    },
    ITEM_REQUEST_OPTIONS,
  );
}

/**
 * 공통 POST
 */
function itemPost<T = any>(path: string, data?: Record<string, any>) {
  return defHttp.post<T>(
    {
      url: absoluteUrl(path),
      params: data,
    },
    ITEM_COMMAND_OPTIONS,
  );
}

/**
 * 공통 PUT
 */
function itemPut<T = any>(path: string, data?: Record<string, any>) {
  return defHttp.put<T>(
    {
      url: absoluteUrl(path),
      params: data,
    },
    ITEM_COMMAND_OPTIONS,
  );
}

/**
 * 공통 DELETE
 */
function itemDelete<T = any>(path: string) {
  return defHttp.delete<T>(
    {
      url: absoluteUrl(path),
    },
    ITEM_COMMAND_OPTIONS,
  );
}

/**
 * 조용한 GET
 */
function itemGetSilent<T = any>(path: string, params?: Record<string, any>) {
  return defHttp.get<T>(
    {
      url: absoluteUrl(path),
      params,
    },
    ITEM_SILENT_REQUEST_OPTIONS,
  );
}

/**
 * 조용한 POST
 */
function itemPostSilent<T = any>(path: string, data?: Record<string, any>) {
  return defHttp.post<T>(
    {
      url: absoluteUrl(path),
      params: data,
    },
    ITEM_SILENT_COMMAND_OPTIONS,
  );
}

export interface ItemMasterSearchParams {
  itemCode?: string;
  itemName?: string;
  categoryCode?: string;
  storageTempType?: string;
  activeYn?: string;
}

export interface ItemMasterListRow {
  id?: string;
  itemCode?: string;
  itemName?: string;
  itemCategoryId?: string;
  categoryCode?: string;
  categoryName?: string;
  operationProfileId?: string;
  industryType?: string;
  baseUom?: string;
  handlingUnitType?: string;
  outboundUnitType?: string;
  storageTempType?: string;
  lotControlYn?: string;
  expiryControlYn?: string;
  serialControlYn?: string;
  activeYn?: string;
  updatedAt?: string;
}

export interface ItemMasterDetailDto {
  id?: string;
  itemCode?: string;
  itemName?: string;
  itemCategoryId?: string;
  categoryCode?: string;
  categoryName?: string;
  operationProfileId?: string;
  industryType?: string;
  baseUom?: string;
  handlingUnitType?: string;
  outboundUnitType?: string;
  lengthMm?: number;
  widthMm?: number;
  heightMm?: number;
  weightG?: number;
  volumeMm3?: number;
  storageTempType?: string;
  lotControlYn?: string;
  expiryControlYn?: string;
  serialControlYn?: string;
  partialPickYn?: string;
  mixedLoadYn?: string;
  fragileYn?: string;
  heavyYn?: string;
  quarantineRequiredYn?: string;
  allocationRuleCode?: string;
  rotationProfileCode?: string;
  storageGradeSeed?: string;
  extAttr?: string;
  activeYn?: string;
  domainId?: number;
  creatorId?: string;
  updaterId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ItemCategoryOptionDto {
  id?: string;
  categoryCode?: string;
  categoryName?: string;
  defaultOperationProfileId?: string;
  description?: string;
  activeYn?: string;
}

export interface ItemMasterUpsertRequest {
  itemCode: string;
  itemName: string;
  categoryCode: string;
  operationProfileId?: string;
  industryType: string;
  baseUom: string;
  handlingUnitType: string;
  outboundUnitType: string;
  lengthMm: number;
  widthMm: number;
  heightMm: number;
  weightG: number;
  storageTempType: string;
  lotControlYn: string;
  expiryControlYn: string;
  serialControlYn: string;
  partialPickYn: string;
  mixedLoadYn: string;
  fragileYn: string;
  heavyYn: string;
  quarantineRequiredYn: string;
  allocationRuleCode: string;
  rotationProfileCode: string;
  storageGradeSeed: string;
  extAttr?: string;
  activeYn: string;
}

export interface ItemActiveToggleRequest {
  activeYn: string;
}

export interface ItemMasterBulkUpsertRequest {
  rows: ItemMasterUpsertRequest[];
}

export interface ItemMasterSaveResultDto {
  id?: string;
  itemCode?: string;
  action?: string;
  message?: string;
}

export interface ItemMasterBulkSaveResultDto {
  totalCount?: number;
  successCount?: number;
  failCount?: number;
  errors?: Array<{
    rowNo?: number;
    itemCode?: string;
    message?: string;
  }>;
}

enum Api {
  ItemMasters = '/rest/aislecore/items',
  ItemCategories = '/rest/aislecore/item-categories',
}

/**
 * 상품 목록 조회
 */
export function fetchItemMasters(params: ItemMasterSearchParams) {
  return itemGet<any>(Api.ItemMasters, params);
}

/**
 * 상품 상세 조회
 */
export function fetchItemMasterDetail(itemCode: string) {
  return itemGet<any>(`${Api.ItemMasters}/${encodeURIComponent(itemCode)}`);
}

/**
 * 활성 카테고리 목록 조회
 *
 * 옵션성 조회라 silent 모드 사용
 */
export function fetchItemCategories() {
  return itemGetSilent<any>(Api.ItemCategories);
}

/**
 * 상품 신규 등록
 */
export function createItemMaster(data: ItemMasterUpsertRequest) {
  return itemPost<ItemMasterSaveResultDto>(Api.ItemMasters, data);
}

/**
 * 상품 수정
 */
export function updateItemMaster(itemCode: string, data: ItemMasterUpsertRequest) {
  return itemPut<ItemMasterSaveResultDto>(
    `${Api.ItemMasters}/${encodeURIComponent(itemCode)}`,
    data,
  );
}

/**
 * 상품 삭제
 */
export function deleteItemMaster(itemCode: string) {
  return itemDelete<void>(`${Api.ItemMasters}/${encodeURIComponent(itemCode)}`);
}

/**
 * 상품 사용 여부 변경
 *
 * 주의:
 * - 현재 VAxios 래퍼에 patch() 가 없어 PUT 으로 통일
 */
export function changeItemActiveYn(itemCode: string, data: ItemActiveToggleRequest) {
  return itemPut<ItemMasterSaveResultDto>(
    `${Api.ItemMasters}/${encodeURIComponent(itemCode)}/active`,
    data,
  );
}

/**
 * 상품 일괄 저장
 */
export function bulkUpsertItemMasters(data: ItemMasterBulkUpsertRequest) {
  return itemPost<ItemMasterBulkSaveResultDto>(
    `${Api.ItemMasters}/bulk-upsert`,
    data,
  );
}

/**
 * 필요 시 조용한 bulk 호출이 필요하면 사용 가능
 */
export function bulkUpsertItemMastersSilent(data: ItemMasterBulkUpsertRequest) {
  return itemPostSilent<ItemMasterBulkSaveResultDto>(
    `${Api.ItemMasters}/bulk-upsert`,
    data,
  );
}
