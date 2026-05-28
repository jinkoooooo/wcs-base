// 시나리오/스텝 결정 + StepDescriptor 빌더.

import { SubOrderType, WCS_ORDER_FINAL } from '/@/views/tspg_4way/constants/wcsConsts';
import { computeAdminActions } from './admin';
import {
  SCENARIO_BLUEPRINTS,
  SCENARIO_LABELS,
  computeShuttleStage,
} from './blueprints';
import type { ResolveCtx, Scenario, ShuttleProgress, StepDescriptor } from './types';

function isSampleFlow(act: any): boolean {
  if (!act) return false;
  if (act.mode === 'PENDING_SAMPLE') return true;
  // backend act.sampleFlow = SubOrderType.isSampleFlow() — SAMPLE_OUT / SAMPLE_DISCARD 만 true.
  // (DISPOSAL_OUT / RETURN_OUT 은 autoFinalize 지만 시험 흐름 아님)
  if (act.mode === 'OUTBOUND' && act.sampleFlow) return true;
  if (act.mode === 'INBOUND' && act.inSampleCycle) return true;
  return false;
}

export function resolveScenario(ctx: ResolveCtx): Scenario {
  if (!ctx.info) return 'IDLE';
  if (isSampleFlow(ctx.act)) return 'SAMPLE_CYCLE';
  if (ctx.act?.mode === 'INBOUND' && ctx.act?.isReinbound) return 'INBOUND_REINBOUND';
  if (ctx.act?.mode === 'POST_OUTBOUND') {
    if (ctx.act.fullyShipped || ctx.act.reinboundCompleted) return 'POST_OUTBOUND';
    return 'OUTBOUND_PARTIAL_OUT';
  }
  if (ctx.act?.mode === 'OUTBOUND' && !ctx.act?.autoFinalize) {
    // backend sub_order_type 이 1차 판정. PARTIAL_OUT 명시되면 부분 출고.
    // hasPartialPicked / reinboundOrderKey 는 legacy fallback.
    if (ctx.act?.subOrderType === SubOrderType.PARTIAL_OUT.code) return 'OUTBOUND_PARTIAL_OUT';
    if (ctx.hasPartialPicked || ctx.act?.reinboundOrderKey) return 'OUTBOUND_PARTIAL_OUT';
    return 'OUTBOUND_NORMAL';
  }
  return 'INBOUND_NORMAL';
}

export function resolveStepIndex(scenario: Scenario, ctx: ResolveCtx): number {
  const blueprints = SCENARIO_BLUEPRINTS[scenario];
  if (blueprints.length === 0) return 0;
  for (let i = blueprints.length - 1; i >= 0; i--) {
    if (blueprints[i].isHere(ctx)) return i;
  }
  return 0;
}

export function buildStepDescriptor(
  scenario: Scenario,
  idx: number,
  ctx: ResolveCtx,
): StepDescriptor {
  const blueprints = SCENARIO_BLUEPRINTS[scenario];
  const labelInfo = SCENARIO_LABELS[scenario];
  const blueprint = blueprints[idx];

  if (!blueprint) {
    return {
      scenario,
      badgeLabel: labelInfo.label,
      badgeTone: labelInfo.tone,
      stepIndex: 0,
      totalSteps: blueprints.length,
      title: '',
      primaryInput: null,
      showProgressBar: false,
      primaryAction: null,
      adminActions: [],
      auxActions: [],
      boxMenuActions: [],
      rowAction: () => null,
    };
  }

  // shuttle 미니바는 act 가 있을 때만 표시.
  const shuttleProgress: ShuttleProgress | undefined =
    blueprint.shuttleStages && ctx.act != null
      ? {
          stages: blueprint.shuttleStages,
          currentStage: computeShuttleStage(ctx.act.orderStatus),
        }
      : undefined;

  const descriptor: StepDescriptor = {
    scenario,
    badgeLabel: labelInfo.label,
    badgeTone: labelInfo.tone,
    stepIndex: idx,
    totalSteps: blueprints.length,
    stage: blueprint.stage,
    title: blueprint.title,
    hint: blueprint.hint,
    primaryInput: blueprint.primaryInput,
    showProgressBar: blueprint.showProgressBar,
    progressValue: blueprint.showProgressBar ? ctx.pickedQty : undefined,
    progressMax: blueprint.showProgressBar ? ctx.expectedQty : undefined,
    progressColor: blueprint.progressColor,
    shuttleProgress,
    primaryAction: blueprint.primaryAction(ctx),
    adminActions: computeAdminActions(scenario, idx, ctx),
    auxActions: blueprint.auxActions(ctx),
    boxMenuActions: blueprint.boxMenuActions,
    rowAction: blueprint.rowAction,
  };

  // 입고 진행 step 에서 act 없고 lifecycle 의 최근이 입고 COMPLETED 면 "입고 완료" override.
  if (scenario === 'INBOUND_NORMAL' && idx === 3 && ctx.act == null) {
    const head = ctx.lifecycle[0];
    const isInbound = head && (head.orderType || '').toUpperCase() === 'INBOUND';
    const isReinbound = !!head?.parentOrderKey;
    if (head && head.orderStatus >= WCS_ORDER_FINAL && isInbound && !isReinbound) {
      return {
        ...descriptor,
        title: '입고 완료',
        hint: '이 파렛트는 입고가 완료되었습니다. 다른 파렛트를 조회하거나 초기화 하세요.',
        primaryAction: {
          label: '다음 작업',
          actionId: 'reset',
          disabled: !ctx.canUpdate,
          variant: 'success',
        },
      };
    }
  }

  // POST_OUTBOUND — 마지막 작업 종류에 따라 title/hint 동적.
  if (scenario === 'POST_OUTBOUND') {
    if (ctx.act?.reinboundCompleted) {
      return {
        ...descriptor,
        title: '재입고 완료',
        hint: '잔량이 rack 에 재입고되었습니다.',
      };
    }
    if (ctx.act?.fullyShipped) {
      return {
        ...descriptor,
        title: '출고 완료',
        hint: '이 파렛트는 완전 출고되었습니다.',
      };
    }
  }

  return descriptor;
}

// 시나리오 step 라벨 (MiniStepper 호버 툴팁용)
export function getStepTooltips(scenario: Scenario): string[] {
  return SCENARIO_BLUEPRINTS[scenario].map((b) => b.title);
}
