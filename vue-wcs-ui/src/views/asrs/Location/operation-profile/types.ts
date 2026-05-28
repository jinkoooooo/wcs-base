export interface OperationProfileFilters {
  profileCode: string;
  profileName: string;
  activeYn: string;
}

export interface OperationProfileRow {
  id: string;
  profileCode: string;
  profileName: string;
  industryType: string;
  description: string;
  activeYn: string;
  linkedAreaCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface OperationProfileForm {
  profileCode: string;
  profileName: string;
  industryType: string;
  description: string;
  activeYn: string;
}

export type OperationProfileLoadingState = Record<string, boolean> & {
  search: boolean;
  detail: boolean;
  save: boolean;
  delete: boolean;
};

export interface SelectOption {
  label: string;
  value: string;
}

/**
 * 산업군 옵션.
 *
 * 1차 화면에서는 full set 제공.
 * 하림푸드 프로젝트에서는 FOOD / FROZEN_FOOD / CHILLED_FOOD / AMBIENT_FOOD 중심 사용 권장.
 */
export const INDUSTRY_TYPE_OPTIONS: SelectOption[] = [
  { label: '식품 일반', value: 'FOOD' },
  { label: '냉동식품', value: 'FROZEN_FOOD' },
  { label: '냉장식품', value: 'CHILLED_FOOD' },
  { label: '상온식품', value: 'AMBIENT_FOOD' },
  { label: '범용 / 기타', value: 'GENERAL' },
  { label: '음료', value: 'BEVERAGE' },
  { label: '신선식품', value: 'FRESH' },
  { label: '축산 / 육가공', value: 'MEAT' },
  { label: '유제품', value: 'DAIRY' },
  { label: '의약 / 의약외품', value: 'PHARMACEUTICAL' },
  { label: '화장품', value: 'COSMETIC' },
  { label: '의류', value: 'APPAREL' },
  { label: '이커머스 범용', value: 'E_COMMERCE' },
];

export const ACTIVE_YN_OPTIONS: SelectOption[] = [
  { label: '전체', value: '' },
  { label: 'Y', value: 'Y' },
  { label: 'N', value: 'N' },
];

export const FORM_ACTIVE_YN_OPTIONS: SelectOption[] = [
  { label: 'Y', value: 'Y' },
  { label: 'N', value: 'N' },
];
