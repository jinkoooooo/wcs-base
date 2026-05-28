// 시나리오별 step 청사진 + 라벨 매핑.

import type { ActionId, ResolveCtx, Scenario, ScenarioBadgeTone, StepBlueprint } from './types';
import {
  rowAction_doneOnly,
  rowAction_inboundFinalize,
  rowAction_inboundPrint,
  rowAction_sampleScanAndInput,
  rowAction_scan,
} from './rowActions';

export function computeShuttleStage(orderStatus: number | null | undefined): number {
  const st = orderStatus ?? 0;
  if (st >= 40) return 3;
  if (st >= 30) return 2;
  if (st >= 10) return 1;
  return 0;
}

const auxBaseDraft = (ctx: ResolveCtx): ActionId[] => {
  const list: ActionId[] = [];
  if (ctx.hasUnsavedChanges) list.push('saveDraft', 'cancelDraft');
  return list;
};

export const SCENARIO_LABELS: Record<Scenario, { label: string; tone: ScenarioBadgeTone }> = {
  IDLE: { label: '', tone: 'idle' },
  INBOUND_NORMAL: { label: '입고', tone: 'in' },
  OUTBOUND_NORMAL: { label: '전체 출고', tone: 'out' },
  OUTBOUND_PARTIAL_OUT: { label: '부분 출고', tone: 'out' },
  SAMPLE_CYCLE: { label: '시험 사이클', tone: 'sample' },
  INBOUND_REINBOUND: { label: '재입고', tone: 'reinbound' },
  POST_OUTBOUND: { label: '작업 완료', tone: 'success' },
};

export const SCENARIO_BLUEPRINTS: Record<Scenario, StepBlueprint[]> = {
  IDLE: [],

  // ─── 입고 4 step ─────────────────────────────
  INBOUND_NORMAL: [
    {
      title: '박스 확정',
      hint: '박스 일련번호를 부여합니다.',
      primaryInput: null,
      showProgressBar: false,
      isHere: () => true, // cumulative base
      primaryAction: (ctx) => ({
        label: `박스 ${ctx.unfinalizedCount}개 확정`,
        actionId: 'finalizeBoxes',
        disabled:
          !ctx.canUpdate ||
          ctx.hasUnsavedChanges ||
          ctx.finalizingBoxes ||
          ctx.unfinalizedCount === 0,
        loading: ctx.finalizingBoxes,
        variant: 'primary',
      }),
      auxActions: (ctx) => [...auxBaseDraft(ctx)],
      boxMenuActions: ['editTotal', 'cancelEdit', 'void'],
      rowAction: rowAction_inboundFinalize,
    },
    {
      title: '라벨 인쇄',
      hint: '박스 라벨을 인쇄하세요.',
      primaryInput: null,
      showProgressBar: false,
      isHere: (ctx) => !ctx.hasDraft,
      primaryAction: (ctx) => ({
        label: '박스 라벨 인쇄',
        actionId: 'printAllBoxes',
        disabled: !ctx.canUpdate || ctx.hasUnsavedChanges || ctx.printing.boxes,
        loading: ctx.printing.boxes,
        variant: 'primary',
      }),
      auxActions: (ctx) => [...auxBaseDraft(ctx)],
      boxMenuActions: ['printBoxLabel'],
      rowAction: rowAction_inboundPrint,
    },
    {
      title: '박스 스캔',
      hint: '박스 바코드를 차례로 스캔하세요.',
      primaryInput: 'box',
      showProgressBar: true,
      progressColor: 'in',
      isHere: (ctx) => !ctx.hasDraft && !ctx.hasUnprintedActive,
      primaryAction: () => null,
      auxActions: (ctx) => [...auxBaseDraft(ctx)],
      boxMenuActions: ['printBoxLabel'],
      rowAction: rowAction_scan,
    },
    {
      title: '입고 진행',
      hint: '파렛트를 입고 위치로 보내세요. ECS 셔틀이 자동으로 진행합니다.',
      primaryInput: null,
      showProgressBar: false,
      shuttleStages: ['송신', '실행', '적치'] as const,
      isHere: (ctx) => ctx.scanComplete || ctx.act != null,
      primaryAction: (ctx) => {
        if (ctx.act != null) return null;
        return {
          label: '입고 가능 처리',
          actionId: 'releaseInbound',
          disabled: !ctx.canUpdate || ctx.busy,
          loading: ctx.busy,
          variant: 'primary',
        };
      },
      auxActions: () => [],
      boxMenuActions: [],
      rowAction: rowAction_doneOnly,
    },
  ],

  // ─── 전체 출고 3 step ─────────────────────────────
  OUTBOUND_NORMAL: [
    {
      title: '출고 운송 중',
      hint: 'ARRIVED 도착 시 박스 스캔이 가능합니다.',
      primaryInput: null,
      showProgressBar: false,
      shuttleStages: ['송신', '운송', '도착'] as const,
      isHere: () => true,
      primaryAction: () => null,
      auxActions: () => [],
      boxMenuActions: [],
      rowAction: () => null,
    },
    {
      title: '박스 스캔',
      hint: '박스 바코드를 차례로 스캔하세요.',
      primaryInput: 'box',
      showProgressBar: true,
      progressColor: 'out',
      isHere: (ctx) => ctx.isOutboundArrived,
      primaryAction: () => null,
      auxActions: () => [],
      boxMenuActions: ['printBoxLabel'],
      rowAction: rowAction_scan,
    },
    {
      title: '출고 확정',
      hint: '전수 스캔 완료. 확정하시겠습니까?',
      primaryInput: null,
      showProgressBar: false,
      isHere: (ctx) => ctx.isOutboundArrived && ctx.outboundComplete,
      primaryAction: (ctx) => ({
        label: '출고 확정',
        actionId: 'finalizeOutbound',
        disabled: !ctx.canUpdate || ctx.busy,
        loading: ctx.busy,
        variant: 'primary',
      }),
      auxActions: () => [],
      boxMenuActions: ['printBoxLabel'],
      rowAction: rowAction_doneOnly,
    },
  ],

  // ─── 부분 출고 4 step ─────────────────────────────
  OUTBOUND_PARTIAL_OUT: [
    {
      title: '출고 운송 중',
      hint: 'ARRIVED 도착 시 박스 스캔이 가능합니다.',
      primaryInput: null,
      showProgressBar: false,
      shuttleStages: ['송신', '운송', '도착'] as const,
      isHere: () => true,
      primaryAction: () => null,
      auxActions: (ctx) => (ctx.act?.reinboundOrderKey ? ['cancelReinbound' as ActionId] : []),
      boxMenuActions: [],
      rowAction: () => null,
    },
    {
      title: '박스 스캔 또는 부분 출고',
      hint: '박스 바코드를 스캔하거나 부분 출고를 선택하세요.',
      primaryInput: 'box',
      showProgressBar: true,
      progressColor: 'out',
      isHere: (ctx) => ctx.isOutboundArrived,
      primaryAction: () => null,
      auxActions: () => [],
      boxMenuActions: ['printBoxLabel', 'partial'],
      rowAction: rowAction_scan,
    },
    {
      title: '출고 확정',
      hint: '부분 출고된 박스의 잔량은 자동 재입고로 처리됩니다.',
      primaryInput: null,
      showProgressBar: false,
      isHere: (ctx) => ctx.isOutboundArrived && ctx.outboundComplete,
      primaryAction: (ctx) => ({
        label: '출고 확정',
        actionId: 'finalizeOutbound',
        disabled: !ctx.canUpdate || ctx.busy,
        loading: ctx.busy,
        variant: 'primary',
      }),
      auxActions: () => [],
      boxMenuActions: ['printBoxLabel'],
      rowAction: rowAction_scan,
    },
    {
      title: '잔량 재입고 생성',
      hint: '남은 잔량을 재입고 셔틀로 발행합니다.',
      primaryInput: null,
      showProgressBar: false,
      isHere: (ctx) => ctx.act?.mode === 'POST_OUTBOUND' && !!ctx.act?.requiresReinbound,
      primaryAction: (ctx) => ({
        label: '재입고 생성',
        actionId: 'doRemainderReinbound',
        disabled: !ctx.canUpdate || ctx.busy,
        loading: ctx.busy,
        variant: 'primary',
      }),
      auxActions: () => [],
      boxMenuActions: [],
      rowAction: () => null,
    },
  ],

  // ─── 시험 사이클 ─────────────────────────────
  //   1) 시험 출고 진행 (ECS 자동)
  //   2) 박스 전량 스캔 — 살아있는 박스 확인. 미스캔 = 전량 소진.
  //   3) 채취 수량 입력 — 스캔된 박스에 picked_qty 입력.
  //   4) 시험 재입고 진행 (ECS 자동, 박스 스캔 없음)
  SAMPLE_CYCLE: [
    {
      title: '시험 출고 진행 중',
      hint: 'ECS 운반 완료 시 자동 완료됩니다.',
      primaryInput: null,
      showProgressBar: false,
      shuttleStages: ['송신', '운송', '도착·자동 완료'] as const,
      stage: 'sample_out',
      isHere: () => true,
      primaryAction: () => null,
      auxActions: () => [],
      boxMenuActions: [],
      rowAction: () => null,
    },
    {
      title: '박스 스캔 및 채취 입력',
      hint: '돌아온 박스를 스캔하고 행에서 채취 수량을 입력하세요. [확정] 시 미스캔/전량 채취 박스 처리 내역 미리보기를 띄웁니다.',
      primaryInput: 'box',
      showProgressBar: false,
      stage: 'pending_sample',
      isHere: (ctx) => ctx.act?.mode === 'PENDING_SAMPLE',
      primaryAction: (ctx) => ({
        label: '확정 미리보기',
        actionId: 'confirmSampleScan',
        disabled: !ctx.canUpdate,
        variant: 'primary',
      }),
      auxActions: () => [],
      boxMenuActions: ['sample', 'printBoxLabel'],
      rowAction: rowAction_sampleScanAndInput,
    },
    {
      title: '시험 재입고 진행 중',
      hint: 'ECS 셔틀이 자동으로 재입고를 진행합니다.',
      primaryInput: null,
      showProgressBar: false,
      shuttleStages: ['송신', '운송', '도착'] as const,
      stage: 'sample_reinbound',
      isHere: (ctx) => ctx.act?.mode === 'INBOUND' && !!ctx.act?.inSampleCycle,
      primaryAction: () => null,
      auxActions: () => [],
      boxMenuActions: [],
      rowAction: () => null,
    },
  ],

  // ─── 잔량 재입고 셔틀 1 step (스캔 없음, ECS 자동) ─────────────────────────────
  INBOUND_REINBOUND: [
    {
      title: '잔량 재입고 진행',
      hint: 'ECS 셔틀이 자동으로 재입고를 진행합니다.',
      primaryInput: null,
      showProgressBar: false,
      shuttleStages: ['송신', '운송', '도착'] as const,
      isHere: () => true,
      primaryAction: () => null,
      auxActions: () => [],
      boxMenuActions: [],
      rowAction: () => null,
    },
  ],

  // ─── 작업 완료 1 step (출고/재입고 종류는 buildStepDescriptor 에서 title/hint override) ──
  POST_OUTBOUND: [
    {
      title: '작업 완료',
      hint: '파렛트 작업이 완료되었습니다.',
      primaryInput: null,
      showProgressBar: false,
      isHere: () => true,
      primaryAction: (ctx) => ({
        label: '다음 작업',
        actionId: 'reset',
        disabled: !ctx.canUpdate,
        variant: 'success',
      }),
      auxActions: () => [],
      boxMenuActions: [],
      rowAction: () => null,
    },
  ],
};
