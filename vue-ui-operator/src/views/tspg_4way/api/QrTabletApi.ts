/**
 * QR 태블릿 조회 API.
 *
 * 백엔드: GET /rest/wcs/qr/scan?code=...
 *  - 박스 바코드("B-" prefix) → BOX 응답 (박스 메타 + 사용기한 색상 + SKU+lot 집계)
 *  - 그 외 → PALLET 응답 (파렛트 메타 + 박스 리스트)
 *
 * 사용처: BoxQrView.vue
 */

import { defHttp } from "@/utils/http/axios";

export interface AggregateBySkuLot {
  total_qty?: number;
  row_count?: number;
}

export interface QrBoxScanResponse {
  type: "BOX";
  boxId: string;
  boxBarcode: string;
  palletBarcode: string;
  boxSeq?: number;
  itemCode?: string;
  itemName?: string;
  lotNo?: string;
  totalQty?: number;
  pickedQty?: number;
  remainingQty?: number;
  qty?: number;
  uom?: string;
  produceDate?: string | null;
  expiryDate?: string | null;
  testRequestNo?: string | null;
  testNo?: string | null;
  inboundDate?: string | null;
  boxStatus?: number;
  decomposition?: {
    box?: number;
    ea?: number;
    perBox?: number | null;
    text?: string;
  };
  barcodes?: { code128?: string; qr?: string };
  expiryStatus?: "NORMAL" | "WARN" | "ALERT" | "EXPIRED" | "UNKNOWN";
  daysToExpiry?: number | null;
  aggregateBySkuLot?: AggregateBySkuLot;
}

export interface QrPalletBoxSummary {
  boxId: string;
  boxBarcode: string;
  boxSeq?: number;
  itemCode?: string;
  lotNo?: string;
  totalQty?: number;
  pickedQty?: number;
  remainingQty?: number;
  boxStatus?: number;
  testNo?: string | null;
  expiryStatus?: string;
  daysToExpiry?: number | null;
  expiryDate?: string | null;
}

export interface QrPalletScanResponse {
  type: "PALLET";
  palletBarcode: string;
  hostOrderKey?: string;
  eqGroupId?: string;
  ownerCode?: string;
  testRequired?: boolean;
  inboundDate?: string | null;
  boxCount?: number;
  totalRemaining?: number;
  items?: Array<Record<string, unknown>>;
  boxes?: QrPalletBoxSummary[];
  barcodes?: { code128?: string; qr?: string };
}

export type QrScanResponse = QrBoxScanResponse | QrPalletScanResponse;

/** QR/바코드 스캔 결과 조회. */
export function scanQr(code: string): Promise<QrScanResponse> {
  return defHttp.get(
    { url: "/rest/wcs/qr/scan", params: { code } },
    { isTransformResponse: false }
  );
}
