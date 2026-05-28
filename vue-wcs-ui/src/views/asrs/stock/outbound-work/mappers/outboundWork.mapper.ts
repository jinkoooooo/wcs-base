/**
 * outbound-work 전용 mapper.
 *
 * 목적:
 * - snake_case / camelCase 혼재 응답 흡수
 * - 화면 표시용 row normalize
 */

import { normalizeList } from '@/views/asrs/shared/utils/normalize';
import { pickNumber, pickString } from '@/views/asrs/shared/utils/pick';
import type {
  OutboundAllocationRow,
  OutboundAutoCandidateRow,
  OutboundHistoryRow,
  OutboundStockRow,
} from '../types';

/** 현재고 row normalize */
export function normalizeOutboundStockRow(row: any): OutboundStockRow {
  const qty = pickNumber(row, ['qty'], 0);
  const reservedQty = pickNumber(row, ['reservedQty', 'reserved_qty'], 0);
  const availableQty = pickNumber(
    row,
    ['availableQty', 'available_qty'],
    Math.max(0, qty - reservedQty),
  );

  return {
    stockUnitNo: pickString(row, ['stockUnitNo', 'stock_unit_no'], ''),
    itemCode: pickString(row, ['itemCode', 'item_code'], ''),
    itemName: pickString(row, ['itemName', 'item_name'], ''),
    areaCode: pickString(row, ['areaCode', 'area_code'], ''),
    locationCode: pickString(row, ['locationCode', 'location_code'], ''),
    qty,
    reservedQty,
    availableQty,
    lotNo: pickString(row, ['lotNo', 'lot_no'], ''),
    stockStatusCode: pickString(row, ['stockStatusCode', 'stock_status_code'], ''),
    activeYn: pickString(row, ['activeYn', 'active_yn'], 'Y'),
    lastTxnAt: pickString(row, ['lastTxnAt', 'last_txn_at'], ''),
  };
}

/** 현재고 목록 normalize */
export function normalizeOutboundStockRows(payload: any): OutboundStockRow[] {
  return normalizeList<any>(payload)
    .map(normalizeOutboundStockRow)
    .filter((row) => !!row.stockUnitNo);
}

/** 자동후보 목록 normalize */
export function normalizeOutboundAutoCandidateRows(payload: any): OutboundAutoCandidateRow[] {
  return normalizeOutboundStockRows(payload).map((row, index) => ({
    ...row,
    candidateRank: index + 1,
    candidateReason: '',
    selectable: row.availableQty > 0 && row.activeYn === 'Y',
  }));
}

/** 활성 할당 row normalize */
export function normalizeOutboundAllocationRow(row: any): OutboundAllocationRow {
  return {
    stockUnitNo: pickString(row, ['stockUnitNo', 'stock_unit_no'], ''),
    itemCode: pickString(row, ['itemCode', 'item_code'], ''),
    allocatedQty: pickNumber(row, ['allocatedQty', 'allocated_qty'], 0),
    allocStatusCode: pickString(row, ['allocStatusCode', 'alloc_status_code'], ''),
    refDocType: pickString(row, ['refDocType', 'ref_doc_type'], ''),
    refDocNo: pickString(row, ['refDocNo', 'ref_doc_no'], ''),
    refLineNo: pickString(row, ['refLineNo', 'ref_line_no'], ''),
    dueDate: pickString(row, ['dueDate', 'due_date'], ''),
    allocatedAt: pickString(row, ['allocatedAt', 'allocated_at'], ''),
    remark: pickString(row, ['remark'], ''),
  };
}

/** 활성 할당 목록 normalize */
export function normalizeOutboundAllocationRows(payload: any): OutboundAllocationRow[] {
  return normalizeList<any>(payload).map(normalizeOutboundAllocationRow);
}

/** 이력 row normalize */
export function normalizeOutboundHistoryRow(row: any): OutboundHistoryRow {
  return {
    txnType: pickString(row, ['txnType', 'txn_type'], ''),
    txnAt: pickString(row, ['txnAt', 'txn_at'], ''),
    fromLocationCode: pickString(row, ['fromLocationCode', 'from_location_code'], ''),
    toLocationCode: pickString(row, ['toLocationCode', 'to_location_code'], ''),
    qty: pickNumber(row, ['qty'], 0),
    refDocType: pickString(row, ['refDocType', 'ref_doc_type'], ''),
    refDocNo: pickString(row, ['refDocNo', 'ref_doc_no'], ''),
    refLineNo: pickString(row, ['refLineNo', 'ref_line_no'], ''),
    reasonCode: pickString(row, ['reasonCode', 'reason_code'], ''),
    remark: pickString(row, ['remark'], ''),
  };
}

/** 이력 목록 normalize */
export function normalizeOutboundHistoryRows(payload: any): OutboundHistoryRow[] {
  return normalizeList<any>(payload).map(normalizeOutboundHistoryRow);
}
