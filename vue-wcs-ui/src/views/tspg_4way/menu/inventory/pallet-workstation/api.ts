// 파렛트 작업대 backend 호출 집중.
// composable/components 는 palletApi.X(...) 만 호출.

import { getCommonGetApi, getCommonGetListApi, getCommonPostApi } from '/@/api/common/api';

const enc = (s: string) => encodeURIComponent(s);

export const palletApi = {
  // ─── 조회 ───
  label(barcode: string) {
    return getCommonGetListApi(`/wcs/labels/pallet/${enc(barcode)}`, null);
  },
  boxes(barcode: string) {
    return getCommonGetListApi(`/wcs/boxes/pallet/${enc(barcode)}`, null);
  },
  active(barcode: string) {
    return getCommonGetListApi(`/wcs/boxes/pallet/${enc(barcode)}/active`, null);
  },
  progress(barcode: string) {
    return getCommonGetListApi(`/wcs/boxes/pallet/${enc(barcode)}/progress`, null);
  },
  lifecycle(barcode: string) {
    return getCommonGetListApi(`/wcs/pallets/${enc(barcode)}/lifecycle`, null);
  },
  hostItems(barcode: string) {
    return getCommonGetListApi(`/wcs/boxes/pallet/${enc(barcode)}/host-items`, null);
  },
  // 재입고 대기 파렛트 알람 — { intervalMin, pallets[] }
  followUpAlarms() {
    return getCommonGetApi('/wcs/alarms/follow-up', {});
  },
  boxLabels(barcode: string) {
    return getCommonGetListApi(`/wcs/labels/pallet/${enc(barcode)}/boxes`, null);
  },

  // ─── 스캔 ───
  scanIn(palletBarcode: string, boxBarcode: string) {
    return getCommonPostApi('/wcs/boxes/scan', { palletBarcode, boxBarcode });
  },
  scanOut(palletBarcode: string, boxBarcode: string, outboundOrderKey: string) {
    return getCommonPostApi('/wcs/boxes/scan-out', {
      palletBarcode,
      boxBarcode,
      outboundOrderKey,
    });
  },

  // ─── 박스 편집 (draft / 단건) ───
  finalize(barcode: string) {
    return getCommonPostApi(`/wcs/boxes/pallet/${enc(barcode)}/finalize`, {});
  },
  editBatch(
    barcode: string,
    body: {
      edits: Array<{ boxId: string; totalQty: number }>;
      additions: Array<{
        itemCode: string;
        lotNo: string;
        totalQty: number;
        produceDate: string | null;
        expiryDate: string | null;
      }>;
      deletions: Array<{ boxId: string }>;
    },
  ) {
    return getCommonPostApi(`/wcs/boxes/pallet/${enc(barcode)}/edit-batch`, body);
  },
  adjustBox(boxId: string, newQty: number) {
    return getCommonPostApi(`/wcs/boxes/${enc(boxId)}/adjust`, { newQty });
  },
  partialOutboundBox(boxId: string, outboundQty: number) {
    return getCommonPostApi(`/wcs/pallets/box/${enc(boxId)}/partial-outbound`, { outboundQty });
  },

  // ─── 입출고 / 재입고 ───
  releaseInbound(barcode: string, adminBypass = false) {
    return getCommonPostApi(`/wcs/pallets/${enc(barcode)}/release`, { adminBypass });
  },
  finalizeOutbound(barcode: string, adminBypass = false) {
    return getCommonPostApi(`/wcs/pallets/${enc(barcode)}/finalize-outbound`, { adminBypass });
  },
  reinbound(barcode: string) {
    return getCommonPostApi(`/wcs/pallets/${enc(barcode)}/reinbound`, {});
  },
  remainderReinbound(barcode: string) {
    return getCommonPostApi(`/wcs/pallets/${enc(barcode)}/remainder-reinbound`, {});
  },
  cancelReinbound(reinboundOrderKey: string, reason = 'user cancel') {
    return getCommonPostApi(`/wcs/boxes/reinbound/${enc(reinboundOrderKey)}/cancel`, { reason });
  },

  // ─── 시험 사이클 ───
  sampleTaken(barcode: string, boxId: string, takenQty: number) {
    return getCommonPostApi(`/wcs/pallets/${enc(barcode)}/sample-taken`, { boxId, takenQty });
  },
  sampleFinalizeReinbound(barcode: string, depleteBoxIds: string[]) {
    return getCommonPostApi(`/wcs/pallets/${enc(barcode)}/sample-finalize-reinbound`, {
      depleteBoxIds,
    });
  },

  // ─── 관리자 우회 ───
  autoScanOutboundBoxes(barcode: string) {
    return getCommonPostApi(`/wcs/pallets/${enc(barcode)}/auto-scan-outbound-boxes`, {});
  },
  autoScanSampleBoxes(barcode: string) {
    return getCommonPostApi(`/wcs/pallets/${enc(barcode)}/auto-scan-sample-boxes`, {});
  },

  // ─── 라벨 ───
  reissueBoxLabel(boxId: string, comment?: string) {
    return getCommonPostApi(
      `/wcs/labels/box/${enc(boxId)}/reissue`,
      comment ? { comment } : {},
    );
  },
  reissueBoxLabelsBatch(boxIds: string[], comment?: string) {
    const body: any = { boxIds };
    if (comment) body.comment = comment;
    return getCommonPostApi('/wcs/labels/boxes/reissue-batch', body);
  },
  markPalletPrinted(barcode: string) {
    return getCommonPostApi(`/wcs/labels/pallet/${enc(barcode)}/mark-printed`, {});
  },
};
