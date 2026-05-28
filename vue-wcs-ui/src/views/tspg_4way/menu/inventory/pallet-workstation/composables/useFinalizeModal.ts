// 확정 미리보기 모달 — 시험 / 관리자 입고·출고·시험 우회 공통.
// kind 맵 dispatch — buildGroups + submit 만 kind 별 정의.

import { reactive, type ComputedRef, type Ref } from 'vue';
import { palletApi } from '../api';
import {
  parseApiError,
  pickedOf,
  printedBoxes,
  remainingOf,
  scannedBoxes,
  totalOf,
  unscannedAlive,
} from '../shared';
import type { FinalizeGroup } from '../scenarios';

export type FinalizeKind = 'sample' | 'adminInbound' | 'adminOutbound' | 'adminSample';

interface FinalizeCtx {
  boxes: any[];
  expected: number;
  picked: number;
}

interface FinalizeBuildResult {
  groups: FinalizeGroup[];
  depleteBoxIds: string[];
}

interface FinalizeHandler {
  title: string;
  confirmLabel: string;
  buildGroups: (ctx: FinalizeCtx) => FinalizeBuildResult;
  submit: (barcode: string, depleteBoxIds: string[]) => Promise<any>;
  successMessage: string;
}

export interface UseFinalizeModalDeps {
  palletBarcode: Ref<string>;
  boxes: Ref<any[]>;
  pi: ComputedRef<{ expected: number; picked: number; message: string }>;
  setMsg: (text: string, kind?: 'ok' | 'err') => void;
  reloadAll: () => Promise<void>;
}

const FINALIZE_HANDLERS: Record<FinalizeKind, FinalizeHandler> = {
  sample: {
    title: '시험 채취 결과 확정',
    confirmLabel: '확정 및 재입고 진행',
    successMessage: '시험 채취 확정 및 재입고 진행됨.',
    buildGroups: ({ boxes }) => {
      const unscanned = unscannedAlive(boxes);
      const scanned = scannedBoxes(boxes);
      const fullyTaken = scanned.filter((b: any) => {
        const total = totalOf(b);
        return total > 0 && pickedOf(b) >= total;
      });
      const partial = scanned.filter((b: any) => {
        const total = totalOf(b);
        const picked = pickedOf(b);
        return picked > 0 && picked < total;
      });
      const noPick = scanned.filter((b: any) => pickedOf(b) === 0);

      return {
        depleteBoxIds: [
          ...unscanned.map((b: any) => String(b.id)),
          ...fullyTaken.map((b: any) => String(b.id)),
        ],
        groups: [
          {
            key: 'depleted',
            title: '소진 처리 (미스캔 + 전량 채취)',
            tone: 'warn',
            items: [
              ...unscanned.map((b: any) => ({ label: b.box_barcode, sub: '미스캔' })),
              ...fullyTaken.map((b: any) => ({
                label: b.box_barcode,
                sub: `전량 채취 ${pickedOf(b)}`,
              })),
            ],
          },
          {
            key: 'partial',
            title: '부분 채취 (잔량 재입고)',
            tone: 'success',
            items: partial.map((b: any) => ({
              label: b.box_barcode,
              sub: `채취 ${pickedOf(b)} / 잔량 ${remainingOf(b) - pickedOf(b)} ${b.uom || ''}`.trim(),
            })),
          },
          {
            key: 'noPick',
            title: '채취 없음 (재입고)',
            tone: 'primary',
            items: noPick.map((b: any) => ({
              label: b.box_barcode,
              sub: `잔량 ${remainingOf(b)} ${b.uom || ''}`.trim(),
            })),
          },
        ],
      };
    },
    submit: (barcode, depleteBoxIds) => palletApi.sampleFinalizeReinbound(barcode, depleteBoxIds),
  },

  adminInbound: {
    title: '관리자 우회 — 박스 스캔 자동 완료',
    confirmLabel: '확정 및 입고 가능 처리',
    successMessage: '입고 가능 처리 완료 (관리자 우회).',
    buildGroups: ({ boxes }) => {
      const unscanned = unscannedAlive(boxes);
      const scanned = scannedBoxes(boxes);
      return {
        depleteBoxIds: [],
        groups: [
          {
            key: 'autoScan',
            title: '자동 스캔 처리될 박스 (미스캔)',
            tone: 'warn',
            items: unscanned.map((b: any) => ({ label: b.box_barcode, sub: '미스캔 → SCANNED' })),
          },
          {
            key: 'scanned',
            title: '이미 스캔된 박스',
            tone: 'success',
            items: scanned.map((b: any) => ({ label: b.box_barcode })),
          },
        ],
      };
    },
    submit: (barcode) => palletApi.releaseInbound(barcode, true),
  },

  adminOutbound: {
    title: '관리자 우회 — 박스 스캔 자동 완료',
    confirmLabel: '박스 스캔 자동 완료',
    successMessage: '박스 스캔 자동 완료. [출고 확정] 으로 진행하세요.',
    buildGroups: ({ boxes, expected, picked }) => {
      const scanned = scannedBoxes(boxes);
      const pickedBoxes = scanned.filter((b: any) => pickedOf(b) > 0);
      const unpicked = scanned.filter((b: any) => pickedOf(b) === 0);
      const shortage = Math.max(0, expected - picked);
      return {
        depleteBoxIds: [],
        groups: [
          {
            key: 'picked',
            title: '이미 스캔되어 출고 처리될 박스',
            tone: 'success',
            items: pickedBoxes.map((b: any) => ({
              label: b.box_barcode,
              sub: `출고 ${pickedOf(b)} ${b.uom || ''}`.trim(),
            })),
          },
          {
            key: 'auto',
            title: '자동 추가 처리될 박스 (부족분 자동 충당)',
            tone: 'warn',
            summary: `요청 ${expected} / 스캔 ${picked} = 부족 ${shortage} 만큼 자동 충당`,
            items: unpicked.map((b: any) => ({
              label: b.box_barcode,
              sub: `잔량 ${remainingOf(b)} ${b.uom || ''}`.trim(),
            })),
          },
        ],
      };
    },
    submit: (barcode) => palletApi.autoScanOutboundBoxes(barcode),
  },

  adminSample: {
    title: '관리자 우회 — 박스 전수 자동 스캔',
    confirmLabel: '박스 전수 자동 스캔',
    successMessage: '박스 전수 자동 스캔 완료. 채취 수량을 입력하세요.',
    buildGroups: ({ boxes }) => {
      const unscanned = printedBoxes(boxes);
      const scanned = scannedBoxes(boxes);
      return {
        depleteBoxIds: [],
        groups: [
          {
            key: 'autoScan',
            title: '자동 스캔 처리될 박스 (미스캔)',
            tone: 'warn',
            items: unscanned.map((b: any) => ({ label: b.box_barcode, sub: '미스캔 → SCANNED' })),
          },
          {
            key: 'scanned',
            title: '이미 스캔된 박스',
            tone: 'success',
            items: scanned.map((b: any) => ({ label: b.box_barcode })),
          },
        ],
      };
    },
    submit: (barcode) => palletApi.autoScanSampleBoxes(barcode),
  },
};

export function useFinalizeModal(deps: UseFinalizeModalDeps) {
  const { palletBarcode, boxes, pi, setMsg, reloadAll } = deps;

  const finalizeModal = reactive({
    open: false,
    busy: false,
    kind: null as FinalizeKind | null,
    title: '',
    confirmLabel: '',
    groups: [] as FinalizeGroup[],
    depleteBoxIds: [] as string[],
  });

  function openFinalize(kind: FinalizeKind) {
    const handler = FINALIZE_HANDLERS[kind];
    const built = handler.buildGroups({
      boxes: boxes.value,
      expected: pi.value.expected,
      picked: pi.value.picked,
    });
    finalizeModal.kind = kind;
    finalizeModal.title = handler.title;
    finalizeModal.confirmLabel = handler.confirmLabel;
    finalizeModal.depleteBoxIds = built.depleteBoxIds;
    finalizeModal.groups = built.groups;
    finalizeModal.busy = false;
    finalizeModal.open = true;
  }

  function closeFinalizeModal() {
    if (finalizeModal.busy) return;
    finalizeModal.open = false;
  }

  async function confirmFinalizeModal() {
    if (finalizeModal.busy || !finalizeModal.kind) return;
    const handler = FINALIZE_HANDLERS[finalizeModal.kind];
    finalizeModal.busy = true;
    try {
      const r: any = await handler.submit(
        palletBarcode.value.trim(),
        finalizeModal.depleteBoxIds,
      );
      setMsg(r?.userMessage || handler.successMessage);
      finalizeModal.open = false;
      await reloadAll();
    } catch (e: any) {
      setMsg(parseApiError(e), 'err');
    } finally {
      finalizeModal.busy = false;
    }
  }

  // 호환용 — 기존 호출자가 sample/adminInbound/adminOutbound/adminSample 별도 함수로 호출 중.
  const openFinalizeModalSample = () => openFinalize('sample');
  const openFinalizeModalAdminRelease = () => openFinalize('adminInbound');
  const openFinalizeModalAdminFinalize = () => openFinalize('adminOutbound');
  const openFinalizeModalAdminSample = () => openFinalize('adminSample');

  return {
    finalizeModal,
    openFinalize,
    openFinalizeModalSample,
    openFinalizeModalAdminRelease,
    openFinalizeModalAdminFinalize,
    openFinalizeModalAdminSample,
    closeFinalizeModal,
    confirmFinalizeModal,
  };
}
