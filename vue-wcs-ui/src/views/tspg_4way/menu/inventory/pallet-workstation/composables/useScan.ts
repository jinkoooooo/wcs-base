// 박스 스캔 + 진행률 + 부분 출고 안내 + 자동 release/highlight.

import { computed, nextTick, ref, watch, type Ref } from 'vue';
import { palletApi } from '../api';
import { parseApiError, pickedOf, remainingOf } from '../shared';
import { BoxStatus, WcsOrderStatus } from '/@/views/tspg_4way/constants/wcsConsts';

export interface UseScanDeps {
  palletBarcode: Ref<string>;
  box: Ref<string>;
  boxRef: Ref<any>;
  boxTableWrapRef: Ref<HTMLElement | null>;
  act: Ref<any>;
  boxes: Ref<any[]>;
  inboundProgress: Ref<any>;
  autoRelease: Ref<boolean>;
  refreshBoxesOnly: () => Promise<void>;
  releaseInbound: () => Promise<void>;
  printBoxLabel: (
    record: any,
    partialOverride?: { remainingQty: number; pickedQty: number },
  ) => void;
  setMsg: (text: string, kind?: 'ok' | 'err') => void;
  notification: any;
}

export function useScan(deps: UseScanDeps) {
  const {
    palletBarcode,
    box,
    boxRef,
    boxTableWrapRef,
    act,
    boxes,
    inboundProgress,
    autoRelease,
    refreshBoxesOnly,
    releaseInbound,
    printBoxLabel,
    setMsg,
    notification,
  } = deps;

  // 부분 출고 결과 — 셔틀 키가 바뀌면 stale → null.
  const lastPartial = ref<{
    itemCode: string;
    lotNo: string;
    uom: string;
    totalQty: number;
    remainingQty: number;
    outboundQty: number;
  } | null>(null);
  watch(
    () => act.value?.orderKey ?? null,
    (curr, prev) => {
      if (curr !== prev) lastPartial.value = null;
    },
  );

  // 스캔된 박스 ID — 다음 스캔/reset 전까지 유지 (영구 파란색).
  const lastScannedBoxId = ref<string | null>(null);

  function highlightScannedBox(boxId: string) {
    lastScannedBoxId.value = boxId;
    nextTick(() => {
      const wrap = boxTableWrapRef.value;
      if (!wrap) return;
      const row = wrap.querySelector(`tr[data-box-id="${CSS.escape(String(boxId))}"]`);
      if (row && (row as HTMLElement).scrollIntoView) {
        (row as HTMLElement).scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    });
  }

  // ─── 진행률 / 모드 플래그 ─────
  const isOutboundArrived = computed(
    () => act.value?.mode === 'OUTBOUND' && act.value.orderStatus === WcsOrderStatus.ARRIVED,
  );
  const showOutboundScan = computed(() => {
    if (act.value?.mode !== 'OUTBOUND') return false;
    if (act.value.autoFinalize) return false;
    return isOutboundArrived.value;
  });
  const showOutboundWaiting = computed(() => {
    if (act.value?.mode !== 'OUTBOUND') return false;
    if (act.value.autoFinalize) return false;
    return !isOutboundArrived.value;
  });
  const showInboundScan = computed(() => {
    if (act.value?.mode === 'OUTBOUND') return false;
    if (act.value?.mode === 'PENDING_SAMPLE') return false;
    if (act.value?.mode === 'POST_OUTBOUND') return false;
    if (!act.value) return true;
    return act.value.orderStatus === WcsOrderStatus.CREATED;
  });
  const showInboundProgress = computed(
    () => act.value?.mode === 'INBOUND' && act.value.orderStatus >= WcsOrderStatus.SENT,
  );
  const canPartialOutbound = computed(() => showOutboundScan.value);

  function isPartialOutboundBox(b: any): boolean {
    if (b?.box_status !== BoxStatus.SCANNED.code) return false;
    const picked = pickedOf(b);
    return picked > 0 && picked < remainingOf(b);
  }
  const hasPartialPicked = computed(() => boxes.value.some(isPartialOutboundBox));
  const partialPickedCount = computed(() => boxes.value.filter(isPartialOutboundBox).length);

  // 시험 채취 박스 존재 여부 — picked > 0.
  const anyTaken = computed(() =>
    boxes.value.some((b: any) => {
      if (Number(b.box_status) !== BoxStatus.SCANNED.code) return false;
      return pickedOf(b) > 0;
    }),
  );

  const pi = computed(() => {
    if (act.value?.mode === 'OUTBOUND') {
      const p = act.value.progress || {};
      return {
        expected: p.expectedQty || 0,
        picked: p.pickedQty || 0,
        message: p.userMessage || '박스 바코드를 스캔하세요.',
      };
    }
    if (act.value?.mode === 'INBOUND') {
      const p = act.value.progress || {};
      const e = p.totalQty || 0;
      const s = p.scannedQty || 0;
      return {
        expected: e,
        picked: s,
        message: p.completed ? '입고 스캔 완료.' : `박스를 스캔하세요. (잔여 ${e - s})`,
      };
    }
    const p = inboundProgress.value || {};
    const e = p.totalQty || 0;
    const s = p.scannedQty || 0;
    if (e === 0)
      return { expected: 0, picked: 0, message: '박스 라벨을 인쇄한 후 스캔을 시작하세요.' };
    return {
      expected: e,
      picked: s,
      message: p.completed
        ? '입고 스캔 완료. 입고 가능 처리하세요.'
        : `박스를 스캔하세요. (잔여 ${e - s})`,
    };
  });

  const percent = computed(() => {
    const e = pi.value.expected;
    const p = pi.value.picked;
    return e > 0 ? Math.min(100, Math.round((p / e) * 100)) : 0;
  });

  const scanComplete = computed(() => percent.value === 100);
  const outboundComplete = computed(() => percent.value === 100);

  async function scan() {
    const code = box.value.trim();
    if (!code) return;
    try {
      const isOutbound = act.value?.mode === 'OUTBOUND';
      const palletBc = palletBarcode.value.trim();
      const result: any = isOutbound
        ? await palletApi.scanOut(palletBc, code, act.value.orderKey)
        : await palletApi.scanIn(palletBc, code);

      if (act.value) act.value.progress = result;
      else inboundProgress.value = result;

      box.value = '';

      const scannedId =
        result?.scannedBoxId ?? result?.boxId ?? result?.box?.id ?? result?.id ?? null;

      if (isOutbound && result?.partialOutbound && result?.partialInfo) {
        lastPartial.value = {
          itemCode: result.partialInfo.itemCode,
          lotNo: result.partialInfo.lotNo,
          uom: result.partialInfo.uom || 'EA',
          totalQty: result.partialInfo.totalQty,
          remainingQty: result.partialInfo.remainingQty,
          outboundQty: result.partialInfo.outboundQty,
        };
        notification.warning({
          message: '박스 부분 출고',
          description: result.userMessage,
          duration: 5,
        });
      } else if (result?.userMessage) {
        setMsg(result.userMessage);
      }

      await refreshBoxesOnly();

      let targetId = scannedId;
      if (!targetId && code) {
        const matched = boxes.value.find((b: any) => b.box_barcode === code);
        if (matched) targetId = matched.id;
      }
      if (targetId) highlightScannedBox(String(targetId));

      // 부분 출고 박스는 잔량이 변경 → 라벨 stale → 재발행 모달 강제.
      // backend remaining_qty 는 finalize 까지 차감 안 되므로
      // 라벨 잔량 = remainingQty(DB) - pickedQtyAfter.
      const isPartialScan = isOutbound && result?.partialOutbound && result?.partialInfo;
      if (isPartialScan) {
        const partialBox = boxes.value.find(
          (b: any) => String(b.id) === String(result.partialInfo.boxId),
        );
        if (partialBox) {
          const remainingDb = Number(result.partialInfo.remainingQty) || 0;
          const pickedAfter = Number(result.partialInfo.pickedQtyAfter) || 0;
          const outQty = Number(result.partialInfo.outboundQty) || 0;
          printBoxLabel(partialBox, {
            remainingQty: Math.max(0, remainingDb - pickedAfter),
            pickedQty: outQty,
          });
        } else {
          nextTick(() => boxRef.value?.focus?.());
        }
      } else if (autoRelease.value && result?.completed && !act.value) {
        // 입고만 자동 release — 셔틀 송신만 트리거, 재고 영향 없음.
        await releaseInbound();
      } else {
        // 출고 확정은 자동 처리 안 함 — 비가역 작업, 사용자 명시 확인 필요.
        nextTick(() => boxRef.value?.focus?.());
      }
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
      nextTick(() => boxRef.value?.focus?.());
    }
  }

  return {
    lastPartial,
    lastScannedBoxId,
    highlightScannedBox,
    scan,
    pi,
    percent,
    scanComplete,
    outboundComplete,
    anyTaken,
    hasPartialPicked,
    partialPickedCount,
    isPartialOutboundBox,
    isOutboundArrived,
    showInboundScan,
    showInboundProgress,
    showOutboundScan,
    showOutboundWaiting,
    canPartialOutbound,
  };
}
