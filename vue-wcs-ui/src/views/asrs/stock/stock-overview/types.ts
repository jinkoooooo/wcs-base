/**
 * 재고 현황(stock-overview) 화면 전용 타입 정의.
 *
 * 목적:
 * - index.vue / composable / mapper / 하위 컴포넌트 간 타입 통일
 * - any 남발을 줄이고, 디버깅 시 데이터 구조를 빠르게 파악
 */

import type { StockFeedbackType } from '../shared/composables/useFeedback';

/**
 * 재고 현황 검색 모드
 * - stockUnit : 재고번호 단건 조회
 * - item      : 품목 기준 목록 조회
 * - location  : 위치 기준 목록 조회
 */
export type StockOverviewSearchMode = 'stockUnit' | 'item' | 'location';

/**
 * 검색 모드 버튼 렌더링용 타입
 */
export interface StockOverviewSearchModeOption {
  label: string;
  value: StockOverviewSearchMode;
}

/**
 * 화면 검색 조건 상태
 */
export interface StockOverviewFilters {
  areaCode: string;
  stockUnitNo: string;
  itemCode: string;
  locationCode: string;
}

/**
 * 재고 현황 테이블/상세 공용 row
 */
export interface StockOverviewRow {
  stockUnitNo: string;
  itemCode: string;
  itemName?: string;
  areaCode?: string;
  locationCode?: string;
  qty: number;
  reservedQty: number;
  lotNo?: string;
  stockStatusCode?: string;
  activeYn?: string;
}

/**
 * 재고 이력 row
 */
export interface StockOverviewHistoryRow {
  txnType: string;
  txnAt: string;
  fromLocationCode: string;
  toLocationCode: string;
  qty: number;
}

/**
 * 상단 요약 카드용 집계 타입
 */
export interface StockOverviewSummary {
  totalCount: number;
  activeCount: number;
  totalQty: number;
  totalReservedQty: number;
}

/**
 * stock-overview 비동기 로딩 상태 타입.
 *
 * Record<string, boolean> 기반으로 선언해서
 * useAsyncFlags 제네릭 제약조건과 정확히 맞춘다.
 */
export type StockOverviewLoadingState = Record<string, boolean> & {
  search: boolean;
  history: boolean;
  areas: boolean;
};

/**
 * 피드백 타입 alias
 *
 * 이유:
 * - shared feedback 타입을 overview에서도 그대로 사용
 * - types.ts에서 한 번 alias 해두면 다른 파일 import가 단순해짐
 */
export type StockOverviewFeedbackType = StockFeedbackType;
