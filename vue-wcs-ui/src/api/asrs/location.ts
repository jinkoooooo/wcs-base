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
 * location API base url 계산.
 *
 * 중요:
 * - stock.ts 와 완전히 동일한 기준 사용
 * - VITE_LOCATION_API_* 는 보지 않음
 * - 오직 VITE_STOCK_API_BASE_URL / VITE_STOCK_API_PORT 기준
 */
function resolveLocationApiBaseUrl(): string {
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

const LOCATION_API_BASE_URL = resolveLocationApiBaseUrl();

const LOCATION_REQUEST_OPTIONS: RequestOptionOverrides = {
  joinPrefix: false,
  apiUrl: '',
  urlPrefix: '',
  errorMessageMode: 'message',
  joinTime: true,
};

const LOCATION_COMMAND_OPTIONS: RequestOptionOverrides = {
  joinPrefix: false,
  apiUrl: '',
  urlPrefix: '',
  errorMessageMode: 'message',
  successMessageMode: 'message',
  joinTime: true,
};

const LOCATION_SILENT_REQUEST_OPTIONS: RequestOptionOverrides = {
  joinPrefix: false,
  apiUrl: '',
  urlPrefix: '',
  errorMessageMode: 'none',
  joinTime: true,
};

function absoluteUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${LOCATION_API_BASE_URL}${normalizedPath}`;
}

function locationGet<T = any>(path: string, params?: Record<string, any>) {
  return defHttp.get<T>(
    {
      url: absoluteUrl(path),
      params,
    },
    LOCATION_REQUEST_OPTIONS,
  );
}

function locationPost<T = any>(path: string, data?: Record<string, any>) {
  return defHttp.post<T>(
    {
      url: absoluteUrl(path),
      params: data,
    },
    LOCATION_COMMAND_OPTIONS,
  );
}

function locationPut<T = any>(path: string, data?: Record<string, any>) {
  return defHttp.put<T>(
    {
      url: absoluteUrl(path),
      params: data,
    },
    LOCATION_COMMAND_OPTIONS,
  );
}

function locationDelete<T = any>(path: string) {
  return defHttp.delete<T>(
    {
      url: absoluteUrl(path),
    },
    LOCATION_COMMAND_OPTIONS,
  );
}

function locationGetSilent<T = any>(path: string, params?: Record<string, any>) {
  return defHttp.get<T>(
    {
      url: absoluteUrl(path),
      params,
    },
    LOCATION_SILENT_REQUEST_OPTIONS,
  );
}

/* -------------------------------------------------------------------------- */
/* Operation Profile                                                           */
/* -------------------------------------------------------------------------- */

export interface OperationProfileSearchParams {
  profileCode?: string;
  profileName?: string;
  activeYn?: string;
}

export interface OperationProfileRowDto {
  id?: string;
  profileCode?: string;
  profileName?: string;
  industryType?: string;
  description?: string;
  activeYn?: string;
  linkedAreaCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface OperationProfileUpsertRequest {
  profileCode: string;
  profileName: string;
  industryType: string;
  description?: string;
  activeYn: string;
}

export interface OperationProfileSaveResultDto {
  id?: string;
  profileCode?: string;
  action?: string;
  message?: string;
}

export function fetchOperationProfiles(params: OperationProfileSearchParams) {
  return locationGet<any>('/rest/aislecore/operation-profiles', params);
}

export function fetchOperationProfileDetail(profileCode: string) {
  const normalized = (profileCode || '').trim();

  if (!normalized) {
    return Promise.reject(new Error('profileCode is empty.'));
  }

  return locationGet<any>(`/rest/aislecore/operation-profiles/${encodeURIComponent(normalized)}`);
}

export function createOperationProfile(data: OperationProfileUpsertRequest) {
  return locationPost<OperationProfileSaveResultDto>('/rest/aislecore/operation-profiles', data);
}

export function updateOperationProfile(profileCode: string, data: OperationProfileUpsertRequest) {
  const normalized = (profileCode || '').trim();

  if (!normalized) {
    return Promise.reject(new Error('profileCode is empty.'));
  }

  return locationPut<OperationProfileSaveResultDto>(
    `/rest/aislecore/operation-profiles/${encodeURIComponent(normalized)}`,
    data,
  );
}

export function deleteOperationProfile(profileCode: string) {
  const normalized = (profileCode || '').trim();

  if (!normalized) {
    return Promise.reject(new Error('profileCode is empty.'));
  }

  return locationDelete<OperationProfileSaveResultDto>(
    `/rest/aislecore/operation-profiles/${encodeURIComponent(normalized)}`,
  );
}

/* -------------------------------------------------------------------------- */
/* Storage Area                                                                */
/* -------------------------------------------------------------------------- */

export interface CenterOptionDto {
  id?: string;
  centerCode?: string;
  centerName?: string;
  activeYn?: string;
}

export interface OperationProfileOptionDto {
  id?: string;
  profileCode?: string;
  profileName?: string;
  activeYn?: string;
}

export interface StorageAreaSearchParams {
  centerCode?: string;
  areaCode?: string;
  areaName?: string;
  activeYn?: string;
}

export interface StorageAreaRowDto {
  id?: string;
  centerId?: string;
  centerCode?: string;
  centerName?: string;
  areaCode?: string;
  areaName?: string;
  areaType?: string;
  operationProfileId?: string;
  operationProfileCode?: string;
  operationProfileName?: string;
  description?: string;
  activeYn?: string;
  linkedLocationProfileCount?: number;
  linkedLocationCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface StorageAreaUpsertRequest {
  centerCode: string;
  areaCode: string;
  areaName: string;
  areaType: string;
  operationProfileCode: string;
  description?: string;
  activeYn: string;
}

export interface StorageAreaSaveResultDto {
  id?: string;
  areaCode?: string;
  action?: string;
  message?: string;
}

export function fetchStorageAreas(params: StorageAreaSearchParams) {
  return locationGet<any>('/rest/aislecore/storage-areas/biz', params);
}

export function fetchStorageAreaDetail(centerCode: string, areaCode: string) {
  const normalizedCenterCode = (centerCode || '').trim();
  const normalizedAreaCode = (areaCode || '').trim();

  if (!normalizedCenterCode || !normalizedAreaCode) {
    return Promise.reject(new Error('centerCode or areaCode is empty.'));
  }

  return locationGet<any>('/rest/aislecore/storage-areas/biz/detail', {
    centerCode: normalizedCenterCode,
    areaCode: normalizedAreaCode,
  });
}

export function fetchCenterOptions() {
  return locationGetSilent<any>('/rest/aislecore/storage-areas/biz/options/centers');
}

export function fetchOperationProfileOptions() {
  return locationGetSilent<any>('/rest/aislecore/storage-areas/biz/options/operation-profiles');
}

export function createStorageArea(data: StorageAreaUpsertRequest) {
  return locationPost<StorageAreaSaveResultDto>('/rest/aislecore/storage-areas/biz', data);
}

export function updateStorageArea(
  originalCenterCode: string,
  originalAreaCode: string,
  data: StorageAreaUpsertRequest,
) {
  const normalizedCenterCode = (originalCenterCode || '').trim();
  const normalizedAreaCode = (originalAreaCode || '').trim();

  if (!normalizedCenterCode || !normalizedAreaCode) {
    return Promise.reject(new Error('centerCode or areaCode is empty.'));
  }

  return locationPut<StorageAreaSaveResultDto>(
    `/rest/aislecore/storage-areas/biz/${encodeURIComponent(
      normalizedCenterCode,
    )}/${encodeURIComponent(normalizedAreaCode)}`,
    data,
  );
}

export function deleteStorageArea(centerCode: string, areaCode: string) {
  const normalizedCenterCode = (centerCode || '').trim();
  const normalizedAreaCode = (areaCode || '').trim();

  if (!normalizedCenterCode || !normalizedAreaCode) {
    return Promise.reject(new Error('centerCode or areaCode is empty.'));
  }

  return locationDelete<StorageAreaSaveResultDto>(
    `/rest/aislecore/storage-areas/biz/${encodeURIComponent(
      normalizedCenterCode,
    )}/${encodeURIComponent(normalizedAreaCode)}`,
  );
}

/* -------------------------------------------------------------------------- */
/* Center                                                                      */
/* -------------------------------------------------------------------------- */

export interface CenterSearchParams {
  centerCode?: string;
  centerName?: string;
  activeYn?: string;
}

export interface CenterUpsertRequest {
  centerCode: string;
  centerName: string;
  centerType: string;
  timezone: string;
  description?: string;
  activeYn: string;
}

export interface CenterSaveResultDto {
  id?: string;
  centerCode?: string;
  action?: string;
  message?: string;
}

export function fetchCenters(params: CenterSearchParams) {
  return locationGet<any>('/rest/aislecore/centers', params);
}

export function fetchCenterDetail(centerCode: string) {
  const normalized = (centerCode || '').trim();

  if (!normalized) {
    return Promise.reject(new Error('centerCode is empty.'));
  }

  return locationGet<any>(`/rest/aislecore/centers/${encodeURIComponent(normalized)}`);
}

export function createCenter(data: CenterUpsertRequest) {
  return locationPost<CenterSaveResultDto>('/rest/aislecore/centers', data);
}

export function updateCenter(centerCode: string, data: CenterUpsertRequest) {
  const normalized = (centerCode || '').trim();

  if (!normalized) {
    return Promise.reject(new Error('centerCode is empty.'));
  }

  return locationPut<CenterSaveResultDto>(
    `/rest/aislecore/centers/${encodeURIComponent(normalized)}`,
    data,
  );
}

export function deleteCenter(centerCode: string) {
  const normalized = (centerCode || '').trim();

  if (!normalized) {
    return Promise.reject(new Error('centerCode is empty.'));
  }

  return locationDelete<CenterSaveResultDto>(
    `/rest/aislecore/centers/${encodeURIComponent(normalized)}`,
  );
}

/* -------------------------------------------------------------------------- */
/* Location Profile                                                            */
/* -------------------------------------------------------------------------- */

export interface LocationProfileSearchParams {
  areaCode?: string;
  profileCode?: string;
  profileName?: string;
  activeYn?: string;
}

export interface LocationProfileUpsertRequest {
  areaCode: string;
  profileCode: string;
  profileName: string;
  aisleStart: number;
  aisleEnd: number;
  sideCodes: string;
  bayStart: number;
  bayEnd: number;
  levelStart: number;
  levelEnd: number;
  depthStart: number;
  depthEnd: number;
  locationType: string;
  codePattern: string;
  mixedLoadYn: string;
  inboundAllowedYn: string;
  outboundAllowedYn: string;
  activeYn: string;
}

export interface LocationProfileSaveResultDto {
  id?: string;
  areaCode?: string;
  area_code?: string;
  profileCode?: string;
  profile_code?: string;
  action?: string;
  message?: string;
}

export interface LocationGeneratePreviewResultDto {
  locationProfileId?: string;
  location_profile_id?: string;
  areaId?: string;
  area_id?: string;
  totalTargetCount?: number;
  total_target_count?: number;
  existingCount?: number;
  existing_count?: number;
  creatableCount?: number;
  creatable_count?: number;
  previewLocationCodes?: string[];
  preview_location_codes?: string[];
}

export interface LocationGenerateResultDto {
  locationProfileId?: string;
  location_profile_id?: string;
  areaId?: string;
  area_id?: string;
  requestedCount?: number;
  requested_count?: number;
  createdCount?: number;
  created_count?: number;
  skippedCount?: number;
  skipped_count?: number;
  createdLocationCodes?: string[];
  created_location_codes?: string[];
  skippedLocationCodes?: string[];
  skipped_location_codes?: string[];
}

export function fetchLocationProfiles(params: LocationProfileSearchParams) {
  return locationGet<any>('/rest/aislecore/location-profiles', params);
}

export function fetchLocationProfileDetail(areaCode: string, profileCode: string) {
  const normalizedAreaCode = (areaCode || '').trim();
  const normalizedProfileCode = (profileCode || '').trim();

  if (!normalizedAreaCode || !normalizedProfileCode) {
    return Promise.reject(new Error('areaCode or profileCode is empty.'));
  }

  return locationGet<any>('/rest/aislecore/location-profiles/detail', {
    areaCode: normalizedAreaCode,
    profileCode: normalizedProfileCode,
  });
}

export function createLocationProfile(data: LocationProfileUpsertRequest) {
  return locationPost<LocationProfileSaveResultDto>('/rest/aislecore/location-profiles', data);
}

export function updateLocationProfile(
  originalAreaCode: string,
  originalProfileCode: string,
  data: LocationProfileUpsertRequest,
) {
  const normalizedAreaCode = (originalAreaCode || '').trim();
  const normalizedProfileCode = (originalProfileCode || '').trim();

  if (!normalizedAreaCode || !normalizedProfileCode) {
    return Promise.reject(new Error('areaCode or profileCode is empty.'));
  }

  return locationPut<LocationProfileSaveResultDto>(
    `/rest/aislecore/location-profiles/${encodeURIComponent(
      normalizedAreaCode,
    )}/${encodeURIComponent(normalizedProfileCode)}`,
    data,
  );
}

export function deleteLocationProfile(areaCode: string, profileCode: string) {
  const normalizedAreaCode = (areaCode || '').trim();
  const normalizedProfileCode = (profileCode || '').trim();

  if (!normalizedAreaCode || !normalizedProfileCode) {
    return Promise.reject(new Error('areaCode or profileCode is empty.'));
  }

  return locationDelete<LocationProfileSaveResultDto>(
    `/rest/aislecore/location-profiles/${encodeURIComponent(
      normalizedAreaCode,
    )}/${encodeURIComponent(normalizedProfileCode)}`,
  );
}

export function fetchLocationProfilePreview(areaCode: string, profileCode: string) {
  const normalizedAreaCode = (areaCode || '').trim();
  const normalizedProfileCode = (profileCode || '').trim();

  if (!normalizedAreaCode || !normalizedProfileCode) {
    return Promise.reject(new Error('areaCode or profileCode is empty.'));
  }

  return locationGet<LocationGeneratePreviewResultDto>(
    '/rest/aislecore/location-profiles/preview',
    {
      areaCode: normalizedAreaCode,
      profileCode: normalizedProfileCode,
    },
  );
}

/**
 * 백엔드가 @RequestParam("areaCode"), @RequestParam("profileCode") 로 받으므로
 * POST body 가 아니라 query string 으로 호출한다.
 */
export function generateLocationsByProfile(areaCode: string, profileCode: string) {
  const normalizedAreaCode = (areaCode || '').trim();
  const normalizedProfileCode = (profileCode || '').trim();

  if (!normalizedAreaCode || !normalizedProfileCode) {
    return Promise.reject(new Error('areaCode or profileCode is empty.'));
  }

  return locationPost<LocationGenerateResultDto>(
    `/rest/aislecore/location-profiles/generate?areaCode=${encodeURIComponent(
      normalizedAreaCode,
    )}&profileCode=${encodeURIComponent(normalizedProfileCode)}`,
    {},
  );
}

/* -------------------------------------------------------------------------- */
/* Location                                                                    */
/* -------------------------------------------------------------------------- */

export interface LocationSearchParams {
  areaCode?: string;
  locationCode?: string;
  locationType?: string;
  activeYn?: string;
}

export interface LocationUpsertRequest {
  areaCode: string;
  locationCode: string;
  aisleNo: number;
  sideCode: string;
  bayNo: number;
  levelNo: number;
  depthNo: number;
  locationType: string;
  usageStatusCode: string;
  inboundAllowedYn: string;
  outboundAllowedYn: string;
  mixedLoadYn: string;
  frontPriorityYn: string;
  dedicatedItemCategoryCode?: string;
  maxWeightG?: number | null;
  maxVolumeMm3?: number | null;
  sortSeq?: number | null;
  activeYn: string;
  locationGrade: string;
  accessScore?: number | null;
  primaryAccessPointCode?: string;
}

export interface LocationSaveResultDto {
  id?: string;
  areaCode?: string;
  area_code?: string;
  locationCode?: string;
  location_code?: string;
  action?: string;
  message?: string;
  deletedLocationCount?: number;
  deleted_location_count?: number;
  deletedStockCount?: number;
  deleted_stock_count?: number;
}

export interface LocationBulkDeleteRequest {
  areaCode?: string;
  locationCode?: string;
  locationType?: string;
  activeYn?: string;
}

export function fetchLocations(params: LocationSearchParams) {
  return locationGet<any>('/rest/aislecore/locations/biz', params);
}

export function fetchLocationDetail(areaCode: string, locationCode: string) {
  const normalizedAreaCode = (areaCode || '').trim();
  const normalizedLocationCode = (locationCode || '').trim();

  if (!normalizedAreaCode || !normalizedLocationCode) {
    return Promise.reject(new Error('areaCode or locationCode is empty.'));
  }

  return locationGet<any>('/rest/aislecore/locations/biz/detail', {
    areaCode: normalizedAreaCode,
    locationCode: normalizedLocationCode,
  });
}

export function fetchItemCategoryOptions() {
  return locationGetSilent<any>('/rest/aislecore/locations/biz/options/item-categories');
}

export function fetchAccessPointOptions(areaCode?: string) {
  return locationGetSilent<any>('/rest/aislecore/locations/biz/options/access-points', {
    areaCode: (areaCode || '').trim(),
  });
}

export function createLocation(data: LocationUpsertRequest) {
  return locationPost<LocationSaveResultDto>('/rest/aislecore/locations/biz', data);
}

export function updateLocation(
  originalAreaCode: string,
  originalLocationCode: string,
  data: LocationUpsertRequest,
) {
  const normalizedAreaCode = (originalAreaCode || '').trim();
  const normalizedLocationCode = (originalLocationCode || '').trim();

  if (!normalizedAreaCode || !normalizedLocationCode) {
    return Promise.reject(new Error('areaCode or locationCode is empty.'));
  }

  return locationPut<LocationSaveResultDto>(
    `/rest/aislecore/locations/biz/${encodeURIComponent(normalizedAreaCode)}/${encodeURIComponent(
      normalizedLocationCode,
    )}`,
    data,
  );
}

export function deleteLocation(areaCode: string, locationCode: string) {
  const normalizedAreaCode = (areaCode || '').trim();
  const normalizedLocationCode = (locationCode || '').trim();

  if (!normalizedAreaCode || !normalizedLocationCode) {
    return Promise.reject(new Error('areaCode or locationCode is empty.'));
  }

  return locationDelete<LocationSaveResultDto>(
    `/rest/aislecore/locations/biz/${encodeURIComponent(normalizedAreaCode)}/${encodeURIComponent(
      normalizedLocationCode,
    )}`,
  );
}

export function bulkDeleteLocations(data: LocationBulkDeleteRequest) {
  return locationPost<LocationSaveResultDto>('/rest/aislecore/locations/biz/bulk-delete', data);
}

/* -------------------------------------------------------------------------- */
/* Location Access / Grade Recalculate                                         */
/* -------------------------------------------------------------------------- */

/**
 * @RequestBody API 전용 POST helper.
 *
 * 기존 locationPost()는 params 로 전송하므로,
 * RequestBody 컨트롤러 호출 시 body 바인딩이 안 될 수 있다.
 */
function locationPostBody<T = any>(path: string, data?: Record<string, any>) {
  return defHttp.post<T>(
    {
      url: absoluteUrl(path),
      data,
    },
    LOCATION_COMMAND_OPTIONS,
  );
}

export interface LocationAccessPreviewRequest {
  areaCode: string;
  purposeCode: string;
  gradeARatio?: number | null;
  gradeBRatio?: number | null;
  gradeCRatio?: number | null;
  limit?: number | null;
}

export interface LocationAccessRecalculateRequest {
  areaCode: string;
  purposeCode: string;
  gradeARatio?: number | null;
  gradeBRatio?: number | null;
  gradeCRatio?: number | null;
}

export interface LocationAccessPreviewRowDto {
  locationId?: string;
  location_id?: string;
  locationCode?: string;
  location_code?: string;
  aisleNo?: number;
  aisle_no?: number;
  sideCode?: string;
  side_code?: string;
  bayNo?: number;
  bay_no?: number;
  levelNo?: number;
  level_no?: number;
  depthNo?: number;
  depth_no?: number;
  frontPriorityYn?: string;
  front_priority_yn?: string;
  accessScore?: number;
  access_score?: number;
  newSortSeq?: number;
  new_sort_seq?: number;
  newLocationGrade?: string;
  new_location_grade?: string;
  primaryAccessPointId?: string;
  primary_access_point_id?: string;
  primaryAccessPointCode?: string;
  primary_access_point_code?: string;
}

export interface LocationAccessPreviewResultDto {
  areaCode?: string;
  area_code?: string;
  purposeCode?: string;
  purpose_code?: string;
  totalCount?: number;
  total_count?: number;
  previewCount?: number;
  preview_count?: number;
  rows?: LocationAccessPreviewRowDto[];
}

export interface LocationAccessRecalculateResultDto {
  areaCode?: string;
  area_code?: string;
  purposeCode?: string;
  purpose_code?: string;
  targetLocationCount?: number;
  target_location_count?: number;
  updatedCount?: number;
  updated_count?: number;
  gradeACount?: number;
  grade_a_count?: number;
  gradeBCount?: number;
  grade_b_count?: number;
  gradeCCount?: number;
  grade_c_count?: number;
  gradeDCount?: number;
  grade_d_count?: number;
  message?: string;
}

/**
 * 로케이션 접근성 / 등급 재산출 미리보기.
 *
 * Backend:
 * POST /rest/aislecore/location-access/preview
 */
export function fetchLocationAccessPreview(data: LocationAccessPreviewRequest) {
  return locationPostBody<LocationAccessPreviewResultDto>(
    '/rest/aislecore/location-access/preview',
    data,
  );
}

/**
 * 로케이션 접근성 / 등급 재산출 실제 반영.
 *
 * Backend:
 * POST /rest/aislecore/location-access/execute
 */
export function executeLocationAccessRecalculate(data: LocationAccessRecalculateRequest) {
  return locationPostBody<LocationAccessRecalculateResultDto>(
    '/rest/aislecore/location-access/execute',
    data,
  );
}

/* -------------------------------------------------------------------------- */
/* Access Point Management                                                     */
/* -------------------------------------------------------------------------- */

/**
 * Access Point 목록 조회 조건.
 */
export interface AccessPointSearchParams {
  areaCode?: string;
  pointCode?: string;
  pointName?: string;
  pointType?: string;
  purposeCode?: string;
  useForSortYn?: string;
  activeYn?: string;
}

/**
 * Access Point 목적 저장 요청.
 */
export interface AccessPointPurposeSaveRequest {
  purposeCode: string;
  priorityNo: number;
  activeYn: string;
  description?: string;
}

/**
 * Access Point 저장/수정 요청.
 */
export interface AccessPointSaveRequest {
  areaCode: string;
  pointCode: string;
  pointName: string;
  pointType: string;
  aisleNo: number;
  sideCode: string;
  bayNo: number;
  levelNo: number;
  depthNo: number;
  useForSortYn: string;
  activeYn: string;
  description?: string;
  purposes: AccessPointPurposeSaveRequest[];
}

/**
 * Access Point 목적 응답 DTO.
 */
export interface AccessPointPurposeDto {
  id?: string;
  accessPointId?: string;
  access_point_id?: string;
  purposeCode?: string;
  purpose_code?: string;
  priorityNo?: number;
  priority_no?: number;
  activeYn?: string;
  active_yn?: string;
  description?: string;
}

/**
 * Access Point 응답 DTO.
 */
export interface AccessPointResponseDto {
  id?: string;

  areaId?: string;
  area_id?: string;
  areaCode?: string;
  area_code?: string;
  areaName?: string;
  area_name?: string;

  pointCode?: string;
  point_code?: string;
  pointName?: string;
  point_name?: string;
  pointType?: string;
  point_type?: string;

  aisleNo?: number;
  aisle_no?: number;
  sideCode?: string;
  side_code?: string;
  bayNo?: number;
  bay_no?: number;
  levelNo?: number;
  level_no?: number;
  depthNo?: number;
  depth_no?: number;

  useForSortYn?: string;
  use_for_sort_yn?: string;
  activeYn?: string;
  active_yn?: string;
  description?: string;

  purposeCodes?: string;
  purpose_codes?: string;
  purposes?: AccessPointPurposeDto[];

  action?: string;
  message?: string;

  createdAt?: string;
  created_at?: string;
  updatedAt?: string;
  updated_at?: string;
}

/**
 * Access Point 목록 조회.
 */
export function fetchAccessPointMasters(params: AccessPointSearchParams) {
  return locationGet<any>('/rest/aislecore/location-access/access-points', params);
}

/**
 * Access Point 상세 조회.
 */
export function fetchAccessPointMasterDetail(areaCode: string, pointCode: string) {
  const normalizedAreaCode = (areaCode || '').trim();
  const normalizedPointCode = (pointCode || '').trim();

  if (!normalizedAreaCode || !normalizedPointCode) {
    return Promise.reject(new Error('areaCode or pointCode is empty.'));
  }

  return locationGet<AccessPointResponseDto>(
    '/rest/aislecore/location-access/access-points/detail',
    {
      areaCode: normalizedAreaCode,
      pointCode: normalizedPointCode,
    },
  );
}

/**
 * Access Point 신규 생성.
 */
export function createAccessPointMaster(data: AccessPointSaveRequest) {
  return locationPostBody<AccessPointResponseDto>(
    '/rest/aislecore/location-access/access-points',
    data,
  );
}

/**
 * Access Point 수정.
 */
export function updateAccessPointMaster(
  originalAreaCode: string,
  originalPointCode: string,
  data: AccessPointSaveRequest,
) {
  const normalizedAreaCode = (originalAreaCode || '').trim();
  const normalizedPointCode = (originalPointCode || '').trim();

  if (!normalizedAreaCode || !normalizedPointCode) {
    return Promise.reject(new Error('areaCode or pointCode is empty.'));
  }

  return locationPut<AccessPointResponseDto>(
    `/rest/aislecore/location-access/access-points/${encodeURIComponent(
      normalizedAreaCode,
    )}/${encodeURIComponent(normalizedPointCode)}`,
    data,
  );
}

/**
 * Access Point 비활성 처리.
 */
export function deleteAccessPointMaster(areaCode: string, pointCode: string) {
  const normalizedAreaCode = (areaCode || '').trim();
  const normalizedPointCode = (pointCode || '').trim();

  if (!normalizedAreaCode || !normalizedPointCode) {
    return Promise.reject(new Error('areaCode or pointCode is empty.'));
  }

  return locationDelete<AccessPointResponseDto>(
    `/rest/aislecore/location-access/access-points/${encodeURIComponent(
      normalizedAreaCode,
    )}/${encodeURIComponent(normalizedPointCode)}`,
  );
}
