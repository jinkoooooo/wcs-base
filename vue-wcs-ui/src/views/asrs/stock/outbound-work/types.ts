/**
 * outbound-work 화면 전용 타입 정의.
 *
 * 2차 범위 추가:
 * - 자동할당(auto) 탭
 * - item + qty 기준 후보 조회/정렬
 */

import type { AsrsFeedbackType } from '@/views/asrs/shared/composables/useFeedback';

/** 작업 탭 */
export type OutboundWorkTab = 'location' | 'item' | 'history' | 'auto';

/** 상단 검색/작업 폼 */
export interface OutboundWorkForm {
  areaCode: string;
  itemCode: string;
  locationCode: string;
  stockUnitNo: string;
  outQty: number | null;
  allocatedQty: number | null;
  refDocType: string;
  refDocNo: string;
  refLineNo: string;
  dueDate: string;
  reasonCode: string;
  remark: string;
}

/** 현재고 row */
export interface OutboundStockRow {
  stockUnitNo: string;
  itemCode: string;
  itemName: string;
  areaCode: string;
  locationCode: string;
  qty: number;
  reservedQty: number;
  availableQty: number;
  lotNo: string;
  stockStatusCode: string;
  activeYn: string;
  lastTxnAt: string;
}

/** 자동할당 후보 row */
export interface OutboundAutoCandidateRow extends OutboundStockRow {
  candidateRank: number;
  candidateReason: string;
  selectable: boolean;
}

/** 활성 할당 row */
export interface OutboundAllocationRow {
  stockUnitNo: string;
  itemCode: string;
  allocatedQty: number;
  allocStatusCode: string;
  refDocType: string;
  refDocNo: string;
  refLineNo: string;
  dueDate: string;
  allocatedAt: string;
  remark: string;
}

/** 트랜잭션 이력 row */
export interface OutboundHistoryRow {
  txnType: string;
  txnAt: string;
  fromLocationCode: string;
  toLocationCode: string;
  qty: number;
  refDocType: string;
  refDocNo: string;
  refLineNo: string;
  reasonCode: string;
  remark: string;
}

/** 로딩 상태 */
export type OutboundWorkLoadingState = {
  areas: boolean;
  search: boolean;
  detail: boolean;
  autoSearch: boolean;
  allocate: boolean;
  partialOut: boolean;
  fullOut: boolean;
  release: boolean;
};

/** 피드백 타입 alias */
export type OutboundWorkFeedbackType = AsrsFeedbackType;
