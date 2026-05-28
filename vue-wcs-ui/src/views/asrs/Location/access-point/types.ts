export interface SelectOption {
  label: string;
  value: string;
}

export interface AccessPointFilters {
  areaCode: string;
  pointCode: string;
  pointName: string;
  pointType: string;
  purposeCode: string;
  useForSortYn: string;
  activeYn: string;
}

export interface AccessPointRow {
  id: string;
  areaId: string;
  areaCode: string;
  areaName: string;
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
  description: string;
  purposeCodes: string;
  createdAt: string;
  updatedAt: string;
}

export interface AccessPointForm {
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
  description: string;

  inboundYn: string;
  inboundPriorityNo: number;

  outboundYn: string;
  outboundPriorityNo: number;

  pickYn: string;
  pickPriorityNo: number;

  relocationYn: string;
  relocationPriorityNo: number;
}

export type AccessPointLoadingState = Record<string, boolean> & {
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

export const FORM_YN_OPTIONS: SelectOption[] = [
  { label: 'Y', value: 'Y' },
  { label: 'N', value: 'N' },
];

export const SIDE_CODE_OPTIONS: SelectOption[] = [
  { label: 'L', value: 'L' },
  { label: 'R', value: 'R' },
];

export const ACCESS_POINT_TYPE_OPTIONS: SelectOption[] = [
  { label: 'PORT', value: 'PORT' },
  { label: 'LIFT', value: 'LIFT' },
  { label: 'PICK FACE', value: 'PICK_FACE' },
  { label: 'BUFFER POINT', value: 'BUFFER_POINT' },
  { label: 'WORK POINT', value: 'WORK_POINT' },
  { label: 'CRANE HOME', value: 'CRANE_HOME' },
];

export const PURPOSE_CODE_OPTIONS: SelectOption[] = [
  { label: '전체', value: '' },
  { label: '입고', value: 'INBOUND' },
  { label: '출고', value: 'OUTBOUND' },
  { label: '피킹', value: 'PICK' },
  { label: '재배치', value: 'RELOCATION' },
];
