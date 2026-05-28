/**
 * inbound-work 화면 전용 타입 정의.
 *
 * 목적:
 * - index / composable / mapper / 하위 컴포넌트 간 타입 통일
 * - 현재 단일 index.vue 에 몰려 있는 상태/행 데이터 구조를 분리
 */

import type { StockFeedbackType } from '../shared/composables/useFeedback';

/**
 * 우측 상세 패널 탭 타입
 */
export type InboundTabType = 'detail' | 'history';

/**
 * 수동입고 입력 폼 상태
 */
export interface InboundFormState {
  areaCode: string;
  itemCode: string;
  qty: number;
  lotNo: string;
}

/**
 * 품목 정책 상태
 *
 * lotEnabled:
 * - Lot 입력칸 자체를 쓸 수 있는지 여부
 *
 * lotRequired:
 * - Lot 입력이 필수인지 여부
 */
export interface InboundItemPolicyState {
  lotEnabled: boolean;
  lotRequired: boolean;
  lotControlRequired: boolean;
  expiryControlRequired: boolean;
  serialControlRequired: boolean;
}

/**
 * 추천 로케이션 옵션
 *
 * 전략 API 응답을 select 박스에서 사용하기 위한 화면 전용 타입
 */
export interface InboundLocationOption {
  locationId?: string;
  locationCode: string;
  locationGrade?: string;
  frontPriorityYn?: string;
  sortSeq?: number;
}

/**
 * 추천 결과 상태
 *
 * options:
 * - 추천 후보 리스트
 *
 * selectedLocationCode:
 * - 작업자가 실제 선택한 로케이션
 *
 * candidateCount:
 * - 전체 추천 후보 수
 */
export interface InboundRecommendState {
  options: InboundLocationOption[];
  selectedLocationCode: string;
  candidateCount: number;
}

/**
 * 입고 대기 목록 row
 *
 * 실제 입고 실행 전, 화면에서 큐처럼 쌓아두는 데이터 구조
 */
export interface InboundDraftRow {
  rowId: string;
  areaCode: string;
  itemCode: string;
  qty: number;
  lotNo?: string;
  locationCode: string;
  locationGrade?: string;
  candidateCount: number;
  status: string;
}

/**
 * 입고 실행 후 최근 처리 결과 row
 */
export interface InboundHistoryRow {
  rowId: string;
  inboundAt: string;
  stockUnitNo: string;
  txnNo: string;
  itemCode: string;
  qty: number;
}

/**
 * 상단 요약 카드 집계 타입
 */
export interface InboundSummary {
  pendingCount: number;
  totalQty: number;
  itemCount: number;
}

/**
 * 화면 로딩 상태
 *
 * 중요:
 * - useAsyncFlags 제네릭과 함께 사용할 타입
 * - 각 필드는 반드시 boolean 으로 선언
 */
export type InboundLoadingState = {
  areas: boolean;
  recommend: boolean;
  submit: boolean;
};

/**
 * 피드백 타입 alias
 *
 * shared 계층 feedback 타입을 inbound 에서 그대로 사용
 */
export type InboundFeedbackType = StockFeedbackType;
