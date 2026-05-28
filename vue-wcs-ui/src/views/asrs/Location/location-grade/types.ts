export interface SelectOption {
  label: string;
  value: string;
}

export interface LocationGradeFilters {
  areaCode: string;
  purposeCode: string;
  gradeARatio: number;
  gradeBRatio: number;
  gradeCRatio: number;
  limit: number;
}

export interface LocationGradePreviewRow {
  locationId: string;
  locationCode: string;
  aisleNo: number;
  sideCode: string;
  bayNo: number;
  levelNo: number;
  depthNo: number;
  frontPriorityYn: string;
  accessScore: number | null;
  newSortSeq: number | null;
  newLocationGrade: string;
  primaryAccessPointId: string;
  primaryAccessPointCode: string;
}

export interface LocationGradePreviewState {
  areaCode: string;
  purposeCode: string;
  totalCount: number;
  previewCount: number;
  rows: LocationGradePreviewRow[];
}

export interface LocationGradeResultState {
  areaCode: string;
  purposeCode: string;
  targetLocationCount: number;
  updatedCount: number;
  gradeACount: number;
  gradeBCount: number;
  gradeCCount: number;
  gradeDCount: number;
  message: string;
}

export type LocationGradeLoadingState = Record<string, boolean> & {
  options: boolean;
  preview: boolean;
  execute: boolean;
};

export const PURPOSE_CODE_OPTIONS: SelectOption[] = [
  { label: '입고 기준', value: 'INBOUND' },
  { label: '출고 기준', value: 'OUTBOUND' },
  { label: '피킹 기준', value: 'PICK' },
  { label: '반품 기준', value: 'RETURN' },
];

export const DEFAULT_GRADE_RATIO = {
  gradeARatio: 0.15,
  gradeBRatio: 0.5,
  gradeCRatio: 0.8,
  limit: 100,
};
