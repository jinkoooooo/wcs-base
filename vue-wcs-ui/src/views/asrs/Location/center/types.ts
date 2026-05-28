export interface CenterFilters {
  centerCode: string;
  centerName: string;
  activeYn: string;
}

export interface CenterRow {
  id: string;
  centerCode: string;
  centerName: string;
  centerType: string;
  timezone: string;
  description: string;
  activeYn: string;
  linkedAreaCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CenterForm {
  centerCode: string;
  centerName: string;
  centerType: string;
  timezone: string;
  description: string;
  activeYn: string;
}

export type CenterLoadingState = Record<string, boolean> & {
  search: boolean;
  detail: boolean;
  save: boolean;
  delete: boolean;
};

export interface SelectOption {
  label: string;
  value: string;
}

export const ACTIVE_YN_OPTIONS: SelectOption[] = [
  { label: '전체', value: '' },
  { label: 'Y', value: 'Y' },
  { label: 'N', value: 'N' },
];

export const FORM_ACTIVE_YN_OPTIONS: SelectOption[] = [
  { label: 'Y', value: 'Y' },
  { label: 'N', value: 'N' },
];

export const CENTER_TYPE_OPTIONS: SelectOption[] = [
  { label: '자동창고 센터', value: 'ASRS' },
  { label: '일반 창고', value: 'WAREHOUSE' },
  { label: '물류센터', value: 'DISTRIBUTION' },
  { label: '공장 창고', value: 'FACTORY_WAREHOUSE' },
  { label: '반품 센터', value: 'RETURN_CENTER' },
  { label: '수동 운영 센터', value: 'MANUAL' },
];

export const TIMEZONE_OPTIONS: SelectOption[] = [
  { label: 'Asia/Seoul', value: 'Asia/Seoul' },
  { label: 'UTC', value: 'UTC' },
  { label: 'Asia/Tokyo', value: 'Asia/Tokyo' },
  { label: 'Asia/Shanghai', value: 'Asia/Shanghai' },
];
