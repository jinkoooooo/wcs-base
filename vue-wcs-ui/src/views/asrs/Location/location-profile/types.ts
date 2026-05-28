export interface SelectOption {
  label: string;
  value: string;
}

export interface LocationProfileFilters {
  areaCode: string;
  profileCode: string;
  profileName: string;
  activeYn: string;
}

export interface LocationProfileRow {
  id: string;
  areaCode: string;
  areaName: string;
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
  linkedLocationCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface LocationProfileForm {
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
  codePatternPreset: string;
  codePatternCustom: string;
  mixedLoadYn: string;
  inboundAllowedYn: string;
  outboundAllowedYn: string;
  activeYn: string;
}

export interface LocationProfilePreviewState {
  totalTargetCount: number;
  existingCount: number;
  creatableCount: number;
  previewLocationCodes: string[];
}

export interface LocationGenerateState {
  requestedCount: number;
  createdCount: number;
  skippedCount: number;
  createdLocationCodes: string[];
  skippedLocationCodes: string[];
}

export type LocationProfileLoadingState = Record<string, boolean> & {
  options: boolean;
  search: boolean;
  detail: boolean;
  save: boolean;
  delete: boolean;
  preview: boolean;
  generate: boolean;
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

export const CODE_PATTERN_PRESET_OPTIONS: SelectOption[] = [
  { label: '{AREA}-A{AISLE}-S{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}', value: '{AREA}-A{AISLE}-S{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}' },
  { label: '{AREA}-A{AISLE}-{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}', value: '{AREA}-A{AISLE}-{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}' },
  { label: '{AREA}-A{AISLE}-{SIDE}{BAY}-{LEVEL}-{DEPTH}', value: '{AREA}-A{AISLE}-{SIDE}{BAY}-{LEVEL}-{DEPTH}' },
  { label: '{AREA}-R{AISLE}-S{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}', value: '{AREA}-R{AISLE}-S{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}' },
  { label: '{AREA}-{AISLE}-{SIDE}-{BAY}-{LEVEL}-{DEPTH}', value: '{AREA}-{AISLE}-{SIDE}-{BAY}-{LEVEL}-{DEPTH}' },
  { label: '직접 입력', value: '__CUSTOM__' },
];
