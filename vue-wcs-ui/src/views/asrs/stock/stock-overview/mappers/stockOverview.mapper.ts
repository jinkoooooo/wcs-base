/**
 * stock-overview mapper
 *
 * 목적:
 * - API 응답 구조를 화면 전용 row 타입으로 normalize
 * - snake_case / camelCase 혼재 대응
 */

import { normalizeList, normalizeSingle } from '@/views/asrs/shared/utils/normalize';
import { pickNumber, pickString } from '@/views/asrs/shared/utils/pick';
import type {
  StockOverviewHistoryRow,
  StockOverviewRow,
} from '../types';

export function normalizeStockOverviewRow(row: any): StockOverviewRow {
  return {
    stockUnitNo: pickString(row, ['stockUnitNo', 'stock_unit_no'], '-'),
    itemCode: pickString(row, ['itemCode', 'item_code'], '-'),
    itemName: pickString(row, ['itemName', 'item_name'], ''),
    areaCode: pickString(row, ['areaCode', 'area_code'], ''),
    locationCode: pickString(
      row,
      ['locationCode', 'location_code', 'currentLocationCode', 'current_location_code'],
      '-',
    ),
    qty: pickNumber(row, ['qty', 'quantity'], 0),
    reservedQty: pickNumber(row, ['reservedQty', 'reserved_qty'], 0),
    lotNo: pickString(row, ['lotNo', 'lot_no'], ''),
    stockStatusCode: pickString(row, ['stockStatusCode', 'stock_status_code'], '-'),
    activeYn: pickString(row, ['activeYn', 'active_yn'], 'Y'),
  };
}

export function normalizeStockOverviewRows(payload: any): StockOverviewRow[] {
  return normalizeList<any>(payload)
    .map(normalizeStockOverviewRow)
    .filter((row) => !!row.stockUnitNo && row.stockUnitNo !== '-');
}

export function normalizeStockOverviewSingle(payload: any): StockOverviewRow | null {
  const single = normalizeSingle<any>(payload);
  if (!single) return null;

  const normalized = normalizeStockOverviewRow(single);

  if (!normalized.stockUnitNo || normalized.stockUnitNo === '-') {
    return null;
  }

  return normalized;
}

export function normalizeStockOverviewHistoryRow(row: any): StockOverviewHistoryRow {
  return {
    txnType: pickString(row, ['txnType', 'txn_type'], '-'),
    txnAt: pickString(row, ['txnAt', 'txn_at'], '-'),
    fromLocationCode: pickString(row, ['fromLocationCode', 'from_location_code'], '-'),
    toLocationCode: pickString(row, ['toLocationCode', 'to_location_code'], '-'),
    qty: pickNumber(row, ['qty'], 0),
  };
}

export function normalizeStockOverviewHistoryRows(payload: any): StockOverviewHistoryRow[] {
  return normalizeList<any>(payload).map(normalizeStockOverviewHistoryRow);
}
