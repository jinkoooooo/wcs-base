export interface StorageAreaFilters {
  centerCode: string;
  areaCode: string;
  areaName: string;
  activeYn: string;
}

export interface StorageAreaRow {
  id: string;
  centerCode: string;
  centerName: string;
  areaCode: string;
  areaName: string;
  areaType: string;
  operationProfileCode: string;
  operationProfileName: string;
  description: string;
  activeYn: string;
  linkedLocationProfileCount: number;
  linkedLocationCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface StorageAreaForm {
  centerCode: string;
  areaCode: string;
  areaName: string;
  areaType: string;
  operationProfileCode: string;
  description: string;
  activeYn: string;
}

export interface SelectOption {
  label: string;
  value: string;
}

export type StorageAreaLoadingState = Record<string, boolean> & {
  options: boolean;
  search: boolean;
  detail: boolean;
  save: boolean;
  delete: boolean;
};

export const ACTIVE_YN_OPTIONS: SelectOption[] = [
  { label: '전체', value: '' },
  { label: 'Y', value: 'Y' },
  { label: 'N', value: 'N' },
];

export const FORM_ACTIVE_YN_OPTIONS: SelectOption[] = [
  { label: 'Y', value: 'Y' },
  { label: 'N', value: 'N' },
];

export const AREA_TYPE_OPTIONS: SelectOption[] = [
  { label: '자동창고 메인 보관영역', value: 'ASRS' },
  { label: '버퍼영역', value: 'BUFFER' },
  { label: '입출고 대기영역', value: 'STAGING' },
  { label: '입고 전용영역', value: 'INBOUND' },
  { label: '출고 전용영역', value: 'OUTBOUND' },
  { label: '피킹영역', value: 'PICKING' },
  { label: '반품영역', value: 'RETURN' },
  { label: '검수영역', value: 'QC' },
  { label: '보류영역', value: 'HOLD' },
  { label: '파손/불량영역', value: 'DAMAGE' },
  { label: '폐기영역', value: 'DISPOSAL' },
  { label: '수동 보관/작업영역', value: 'MANUAL' },
];
