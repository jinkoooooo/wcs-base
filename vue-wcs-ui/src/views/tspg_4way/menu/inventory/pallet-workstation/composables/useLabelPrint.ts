// QR/Code128 라벨 렌더 + 인쇄창 + 단건/일괄/팔레트 라벨 발행.

import { reactive, type Ref } from 'vue';
import * as QRCode from 'qrcode';
import { palletApi } from '../api';
import { parseApiError, type LabelReissueTarget } from '../shared';
import { BoxStatus } from '/@/views/tspg_4way/constants/wcsConsts';

export interface UseLabelPrintDeps {
  palletBarcode: Ref<string>;
  info: Ref<any>;
  boxes: Ref<any[]>;
  selectedBoxIds: Ref<Set<string>>;
  printing: { pallet: boolean; boxes: boolean; mark: boolean; selected: boolean };
  setMsg: (text: string, kind?: 'ok' | 'err') => void;
  notification: any;
  refreshBoxesOnly: () => Promise<void>;
  reloadAll: () => Promise<void>;
  clearSelection: () => void;
}

async function qrDataUrl(text: string): Promise<string> {
  try {
    return await QRCode.toDataURL(text, { margin: 2, width: 240, errorCorrectionLevel: 'Q' });
  } catch {
    return '';
  }
}

async function code128DataUrl(text: string): Promise<string> {
  try {
    const m = await import('jsbarcode');
    const JsBarcode = (m as any).default || m;
    const canvas = document.createElement('canvas');
    JsBarcode(canvas, text, {
      format: 'CODE128',
      width: 2,
      height: 60,
      displayValue: false,
      margin: 0,
    });
    return canvas.toDataURL('image/png');
  } catch {
    return '';
  }
}

function esc(v: any): string {
  if (v == null) return '';
  return String(v).replace(/[&<>"']/g, (c) => {
    const map: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
    };
    return map[c];
  });
}

function renderPalletHtml(label: any, qrUrl: string, code128Url: string): string {
  return `
  <div class="label">
    <div class="header"><span>PALLET</span><span>${esc(label.palletBarcode)}</span></div>
    <div class="qr-top"><img src="${qrUrl}" alt="QR" /><div class="qr-text">${esc(
    label.palletBarcode,
  )}</div></div>
    <div class="info">
      <div class="row"><b>Host</b>${esc(label.hostOrderKey)}</div>
      <div class="row"><b>Group</b>${esc(label.eqGroupId)}</div>
      <div class="row"><b>Owner</b>${esc(label.ownerCode)}</div>
      <div class="row"><b>입고일자</b>${esc(label.inboundDate)}</div>
      <div class="row"><b>박스 수</b>${esc(label.boxCount)}</div>
      <div class="row"><b>시험</b>${label.testRequired ? 'YES' : 'NO'}</div>
    </div>
    ${
      code128Url
        ? `<div class="code128"><img src="${code128Url}" alt="Code128" /><div class="code-text">${esc(
            label.palletBarcode,
          )}</div></div>`
        : ''
    }
  </div>`;
}

function renderBoxHtml(l: any, qrUrl: string, code128Url: string): string {
  const total = l.totalQty ?? l.qty ?? 0;
  const picked = l.pickedQty ?? 0;
  const remaining = l.remainingQty ?? total;
  const showPicked = picked > 0;
  return `
  <div class="label">
    <div class="header"><span>BOX #${esc(l.boxSeq)}</span><span>${esc(l.boxBarcode)}</span></div>
    <div class="qr-top"><img src="${qrUrl}" alt="QR" /><div class="qr-text">${esc(
    l.boxBarcode,
  )}</div></div>
    <div class="info">
      <div class="row"><b>코드</b>${esc(l.itemCode)}</div>
      <div class="row"><b>품명</b>${esc(l.itemName)}</div>
      <div class="row"><b>Lot</b>${esc(l.lotNo)}</div>
      <div class="row"><b>수량</b></div>
      <div class="qty-big">${esc(remaining)} / ${esc(total)} ${esc(l.uom)}</div>
      ${showPicked ? `<div class="qty-small">출고 ${esc(picked)} ${esc(l.uom)}</div>` : ''}
    </div>
    <div class="details">
      <div><b>파렛트</b>${esc(l.palletBarcode)}</div>
      <div><b>제조일</b>${esc(
        l.produceDate,
      )} <b style="min-width:50px;margin-left:8px">유효</b>${esc(l.expiryDate)}</div>
      <div><b>입고일자</b>${esc(l.inboundDate)}</div>
      ${l.testRequestNo ? `<div><b>시험의뢰</b>${esc(l.testRequestNo)}</div>` : ''}
    </div>
    ${
      code128Url
        ? `<div class="code128"><img src="${code128Url}" alt="Code128" /><div class="code-text">${esc(
            l.boxBarcode,
          )}</div></div>`
        : ''
    }
  </div>`;
}

const PRINT_STYLES = `
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; padding: 12px; color: #000; }
.label { border: 1px solid #000; padding: 12px; margin: 0 0 12px; width: 380px; box-sizing: border-box; position: relative; min-height: 540px; }
.label .header { font-size: 13px; font-weight: 700; margin-bottom: 8px; display: flex; justify-content: space-between; }
.label .qr-top { position: absolute; top: 36px; left: 12px; }
.label .qr-top img { width: 160px; height: 160px; display: block; }
.label .qr-text { font-size: 9px; margin-top: 2px; word-break: break-all; max-width: 160px; }
.label .info { margin-left: 175px; min-height: 170px; }
.label .info .row { font-size: 12px; line-height: 1.7; }
.label .info .row b { display: inline-block; min-width: 56px; color: #555; font-weight: 500; }
.label .info .qty-big { font-size: 22px; font-weight: 800; margin: 6px 0; }
.label .info .qty-small { font-size: 10px; color: #888; }
.label .details { clear: both; margin-top: 12px; padding-top: 12px; border-top: 1px dashed #999; font-size: 11px; line-height: 1.6; }
.label .details b { display: inline-block; min-width: 70px; color: #555; font-weight: 500; }
.label .code128 { position: absolute; bottom: 12px; left: 12px; right: 12px; padding-top: 8px; border-top: 1px solid #000; }
.label .code128 img { width: 100%; height: 60px; display: block; }
.label .code128 .code-text { font-size: 10px; text-align: center; margin-top: 2px; }
.page-break { page-break-after: always; }
@media print { body { padding: 0; } .no-print { display: none; } }
`;

export function useLabelPrint(deps: UseLabelPrintDeps) {
  const {
    palletBarcode,
    info,
    boxes,
    selectedBoxIds,
    printing,
    setMsg,
    notification,
    refreshBoxesOnly,
    reloadAll,
    clearSelection,
  } = deps;

  const labelReissueModal = reactive({
    open: false,
    busy: false,
    comment: '',
    target: null as LabelReissueTarget | null,
  });

  function isFirstIssueBox(b: any): boolean {
    if (!b) return false;
    const pc = Number(b.print_count ?? b.printCount ?? 0);
    return b.box_status === BoxStatus.PENDING.code && pc === 0;
  }
  function allFirstIssue(boxIds: string[]): boolean {
    const set = new Set(boxIds);
    return boxes.value.every((b: any) => !set.has(b.id) || isFirstIssueBox(b));
  }

  function _requireComment(): boolean {
    const t = labelReissueModal.target;
    if (!t) return true;
    if (t.kind === 'single') return !t.isFirstIssue;
    return !t.allFirstIssue;
  }
  function _hasValidationError(): boolean {
    if (!_requireComment()) return false;
    const c = (labelReissueModal.comment ?? '').trim();
    return c.length < 2 || c.length > 500;
  }

  function openPrintWindow(bodyHtml: string, onClosed?: () => void) {
    const w = window.open('', '_blank', 'width=720,height=900');
    if (!w) {
      notification.error({
        message: '인쇄창 차단됨',
        description:
          '브라우저가 새 창을 차단했습니다. 주소창 우측의 차단 아이콘을 눌러 이 사이트의 팝업을 허용한 뒤 다시 시도해 주세요.',
        duration: 8,
      });
      return;
    }
    // 인쇄창 닫힘 감지 — print 또는 cancel 후 창 닫으면 callback.
    if (onClosed) {
      const timer = window.setInterval(() => {
        if (w.closed) {
          window.clearInterval(timer);
          try {
            onClosed();
          } catch (_) {
            /* swallow */
          }
        }
      }, 500);
    }
    w.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>Label Print</title>
<style>${PRINT_STYLES}</style></head><body>${bodyHtml}
<div class="no-print" style="margin-top:12px;text-align:center"><button onclick="window.print()">인쇄</button></div>
</body></html>`);
    w.document.close();
    w.onload = () => {
      setTimeout(() => {
        try {
          w.focus();
          w.print();
        } catch (_) {
          /* empty */
        }
      }, 200);
    };
  }

  async function printPalletLabel() {
    if (!info.value) return;
    printing.pallet = true;
    try {
      const [qrUrl, c128Url] = await Promise.all([
        qrDataUrl(info.value.palletBarcode),
        code128DataUrl(info.value.palletBarcode),
      ]);
      openPrintWindow(renderPalletHtml(info.value, qrUrl, c128Url));
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
    } finally {
      printing.pallet = false;
    }
  }

  async function markPalletPrinted() {
    if (!info.value) return;
    printing.mark = true;
    try {
      const r: any = await palletApi.markPalletPrinted(palletBarcode.value.trim());
      setMsg(`출력 완료 처리됨. (${r?.markedCount ?? 0}개 박스 PRINTED)`);
      await reloadAll();
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
    } finally {
      printing.mark = false;
    }
  }

  async function printAllBoxLabels() {
    if (!info.value) return;
    printing.boxes = true;
    try {
      const data: any = await palletApi.boxLabels(palletBarcode.value.trim());
      const labels: any[] = data?.labels ?? [];
      if (labels.length === 0) {
        notification.warning({ message: '안내', description: '인쇄할 박스 라벨이 없습니다.' });
        return;
      }
      const htmls: string[] = [];
      for (const l of labels) {
        const [qrUrl, c128Url] = await Promise.all([
          qrDataUrl(l.boxBarcode),
          code128DataUrl(l.boxBarcode),
        ]);
        htmls.push(renderBoxHtml(l, qrUrl, c128Url));
      }
      // 인쇄창 닫히면 자동 mark-printed → step 자동 진행.
      openPrintWindow(htmls.join('<div class="page-break"></div>'), () => {
        markPalletPrinted();
      });
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
    } finally {
      printing.boxes = false;
    }
  }

  // 단건 라벨 — 모달 열기. partialOverride: partial outbound 직후, 정확한 잔량 전달용.
  function printBoxLabel(
    record: any,
    partialOverride?: { remainingQty: number; pickedQty: number },
  ) {
    labelReissueModal.target = {
      kind: 'single',
      boxId: record.id,
      boxBarcode: record.box_barcode,
      isFirstIssue: isFirstIssueBox(record),
      partialOverride,
    };
    labelReissueModal.comment = '';
    labelReissueModal.open = true;
  }

  function printSelectedBoxLabels() {
    if (selectedBoxIds.value.size === 0) return;
    const ids = Array.from(selectedBoxIds.value);
    const idSet = new Set(ids);
    const targets = boxes.value.filter((b: any) => idSet.has(b.id));
    const allFirst = targets.length > 0 && targets.every((b: any) => isFirstIssueBox(b));
    labelReissueModal.target = { kind: 'multi', boxIds: ids, allFirstIssue: allFirst };
    labelReissueModal.comment = '';
    labelReissueModal.open = true;
  }

  function closeLabelReissueModal() {
    if (labelReissueModal.busy) return;
    labelReissueModal.open = false;
  }

  async function confirmLabelReissue() {
    if (!labelReissueModal.target) return;
    if (_hasValidationError()) return;

    const target = labelReissueModal.target;
    const comment = (labelReissueModal.comment ?? '').trim();
    const requireComment = _requireComment();
    labelReissueModal.busy = true;

    try {
      if (target.kind === 'single') {
        const l: any = await palletApi.reissueBoxLabel(
          target.boxId,
          requireComment ? comment : undefined,
        );
        // partial outbound 직후 — 백엔드는 finalize 전이라 remaining_qty stale.
        // 호출자 전달 잔량 override.
        if (target.partialOverride) {
          l.remainingQty = target.partialOverride.remainingQty;
          l.pickedQty = target.partialOverride.pickedQty;
        }
        const [qrUrl, c128Url] = await Promise.all([
          qrDataUrl(l.boxBarcode),
          code128DataUrl(l.boxBarcode),
        ]);
        openPrintWindow(renderBoxHtml(l, qrUrl, c128Url));
        setMsg(`박스 라벨 ${requireComment ? '재발행' : '발행'}됨.`);
      } else {
        const r: any = await palletApi.reissueBoxLabelsBatch(
          target.boxIds,
          requireComment ? comment : undefined,
        );
        const labelList: any[] = r?.labels ?? [];
        if (labelList.length === 0) {
          notification.warning({ message: '안내', description: '인쇄 가능한 라벨이 없습니다.' });
          return;
        }
        const htmls = await Promise.all(
          labelList.map(async (l) => {
            const [qrUrl, c128Url] = await Promise.all([
              qrDataUrl(l.boxBarcode),
              code128DataUrl(l.boxBarcode),
            ]);
            return renderBoxHtml(l, qrUrl, c128Url);
          }),
        );
        openPrintWindow(htmls.join('<div class="page-break"></div>'));
        setMsg(
          `${labelList.length}개 박스 라벨 ${requireComment ? '일괄 재발행' : '일괄 발행'}됨.`,
        );
        clearSelection();
      }
      labelReissueModal.open = false;
      labelReissueModal.target = null;
      labelReissueModal.comment = '';
      await refreshBoxesOnly();
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
    } finally {
      labelReissueModal.busy = false;
    }
  }

  return {
    labelReissueModal,
    isFirstIssueBox,
    allFirstIssue,
    printPalletLabel,
    printAllBoxLabels,
    printBoxLabel,
    printSelectedBoxLabels,
    confirmLabelReissue,
    markPalletPrinted,
    closeLabelReissueModal,
  };
}
