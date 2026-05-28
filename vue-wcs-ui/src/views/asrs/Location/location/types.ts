export interface SelectOption {
  label: string;
  value: string;
}

export interface LocationFilters {
  areaCode: string;
  locationCode: string;
  locationType: string;
  activeYn: string;
}

export interface LocationRow {
  id: string;
  areaCode: string;
  areaName: string;
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
  dedicatedItemCategoryCode: string;
  dedicatedItemCategoryName: string;
  maxWeightG: number | null;
  maxVolumeMm3: number | null;
  sortSeq: number | null;
  activeYn: string;
  locationGrade: string;
  accessScore: number | null;
  primaryAccessPointCode: string;
  primaryAccessPointName: string;
  createdAt: string;
  updatedAt: string;
}

export interface LocationForm {
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
  dedicatedItemCategoryCode: string;
  maxWeightG: number | null;
  maxVolumeMm3: number | null;
  sortSeq: number | null;
  activeYn: string;
  locationGrade: string;
  accessScore: number | null;
  primaryAccessPointCode: string;
}

export type LocationLoadingState = Record<string, boolean> & {
  options: boolean;
  search: boolean;
  detail: boolean;
  save: boolean;
  delete: boolean;
  accessPointOptions: boolean;
};

export const ACTIVE_YN_OPTIONS: SelectOption[] = [
  { label: '전체', value: '' },
  { label: 'Y', value: 'Y' },
  { label: 'N', value: 'N' },
];

export const FORM_YN_OPTIONS: SelectOption[] = [
  { label: 'Y', value: 'Y' },
  { label: 'N', value: 'N' },
];

export const SIDE_CODE_OPTIONS: SelectOption[] = [
  { label: 'L', value: 'L' },
  { label: 'R', value: 'R' },
];

export const LOCATION_TYPE_OPTIONS: SelectOption[] = [
  { label: '일반 보관', value: 'NORMAL' },
  { label: '버퍼', value: 'BUFFER' },
  { label: '입고 전용', value: 'INBOUND' },
  { label: '출고 전용', value: 'OUTBOUND' },
  { label: '피킹 전용', value: 'PICK' },
  { label: '반품 전용', value: 'RETURN' },
  { label: '검수 전용', value: 'QC' },
  { label: '보류 전용', value: 'HOLD' },
  { label: '파손/불량', value: 'DAMAGE' },
  { label: '폐기', value: 'DISPOSAL' },
  { label: '가상', value: 'VIRTUAL' },
  { label: '임시', value: 'TEMP' },
];

export const LOCATION_USAGE_STATUS_OPTIONS: SelectOption[] = [
  { label: '사용 가능', value: 'ENABLED' },
  { label: '사용 중지', value: 'DISABLED' },
  { label: '운영 차단', value: 'BLOCKED' },
  { label: '점검중', value: 'MAINTENANCE' },
  { label: '운영 보류', value: 'HOLD' },
];

export const LOCATION_GRADE_OPTIONS: SelectOption[] = [
  { label: 'A', value: 'A' },
  { label: 'B', value: 'B' },
  { label: 'C', value: 'C' },
  { label: 'D', value: 'D' },
];
