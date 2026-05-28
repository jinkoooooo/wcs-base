/**
 * item-master 화면 전용 타입 정의.
 *
 * 목적:
 * - index / composable / mapper / components 간 타입 통일
 * - 상품마스터 관리 화면의 상태 구조를 명확히 분리
 */

import type { AsrsFeedbackType } from '@/views/asrs/shared/composables/useFeedback';

/**
 * 화면 모드
 *
 * create:
 * - 신규 등록 모드
 *
 * edit:
 * - 기존 상품 수정 모드
 */
export type ItemMasterMode = 'create' | 'edit';

/**
 * 상세 패널 탭
 *
 * 현재는 기본정보 / 부가정보 정도로도 확장할 수 있지만,
 * 우선은 단일 form 구조로 유지하므로 modal 용 상태만 남겨둠
 */
export type ItemMasterRightTab = 'detail';

/**
 * 상품 검색 폼
 */
export interface ItemMasterSearchForm {
  itemCode: string;
  itemName: string;
  categoryCode: string;
  storageTempType: string;
  activeYn: string;
}

/**
 * 상품 목록 row
 */
export interface ItemMasterRow {
  id: string;
  itemCode: string;
  itemName: string;
  itemCategoryId: string;
  categoryCode: string;
  categoryName: string;
  operationProfileId: string;
  industryType: string;
  baseUom: string;
  handlingUnitType: string;
  outboundUnitType: string;
  storageTempType: string;
  lotControlYn: string;
  expiryControlYn: string;
  serialControlYn: string;
  activeYn: string;
  updatedAt: string;
}

/**
 * 상품 상세 편집 form
 *
 * 주의:
 * - 백엔드 저장 DTO와 최대한 유사하게 맞춤
 * - volumeMm3 는 읽기전용 표시값으로 사용
 */
export interface ItemMasterDetailForm {
  id: string;
  itemCode: string;
  itemName: string;
  categoryCode: string;
  categoryName: string;
  operationProfileId: string;
  industryType: string;
  baseUom: string;
  handlingUnitType: string;
  outboundUnitType: string;
  lengthMm: number;
  widthMm: number;
  heightMm: number;
  weightG: number;
  volumeMm3: number;
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
  extAttr: string;
  activeYn: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 카테고리 옵션
 */
export interface ItemCategoryOption {
  id: string;
  categoryCode: string;
  categoryName: string;
  defaultOperationProfileId: string;
  description: string;
  activeYn: string;
}

/**
 * 엑셀 붙여넣기 row
 *
 * 실제 저장 전 preview grid 에 표시할 구조
 */
export interface ItemBulkPasteRow {
  rowNo: number;
  itemCode: string;
  itemName: string;
  categoryCode: string;
  operationProfileId: string;
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
  activeYn: string;
  extAttr: string;
}

/**
 * bulk 저장 오류 row
 */
export interface ItemBulkPasteErrorRow {
  rowNo: number;
  itemCode: string;
  message: string;
}

/**
 * 로딩 상태
 */
export type ItemMasterLoadingState = {
  search: boolean;
  detail: boolean;
  save: boolean;
  delete: boolean;
  categories: boolean;
  bulkSave: boolean;
};

/**
 * 피드백 타입 alias
 */
export type ItemMasterFeedbackType = AsrsFeedbackType;
