// 시나리오 / 스텝 / 관리자 우회 가능 여부 computed 묶음.

import { computed, type ComputedRef, type Ref } from 'vue';
import {
  buildStepDescriptor,
  getStepTooltips,
  resolveScenario,
  resolveStepIndex,
  type ResolveCtx,
  type Scenario,
  type StepDescriptor,
} from '../scenarios';

export interface UseScenarioDeps {
  info: Ref<any>;
  act: Ref<any>;
  boxes: Ref<any[]>;
  lifecycle: Ref<any[]>;
  hasUnsavedChanges: ComputedRef<boolean>;
  hasDraft: ComputedRef<boolean>;
  hasUnprintedActive: ComputedRef<boolean>;
  scanComplete: ComputedRef<boolean>;
  outboundComplete: ComputedRef<boolean>;
  hasPartialPicked: ComputedRef<boolean>;
  anyTaken: ComputedRef<boolean>;
  isOutboundArrived: ComputedRef<boolean>;
  unfinalizedCount: ComputedRef<number>;
  pickedQty: ComputedRef<number>;
  expectedQty: ComputedRef<number>;
  canUpdate: ComputedRef<boolean>;
  isAdmin: ComputedRef<boolean>;
  busy: Ref<boolean>;
  finalizingBoxes: Ref<boolean>;
  autoRelease: Ref<boolean>;
  printing: { pallet: boolean; boxes: boolean; mark: boolean; selected: boolean };
}

export function useScenario(deps: UseScenarioDeps) {
  const resolveCtx = computed<ResolveCtx>(() => ({
    info: deps.info.value,
    act: deps.act.value,
    boxes: deps.boxes.value,
    lifecycle: deps.lifecycle.value,
    hasUnsavedChanges: deps.hasUnsavedChanges.value,
    hasDraft: deps.hasDraft.value,
    hasUnprintedActive: deps.hasUnprintedActive.value,
    scanComplete: deps.scanComplete.value,
    outboundComplete: deps.outboundComplete.value,
    hasPartialPicked: deps.hasPartialPicked.value,
    anyTaken: deps.anyTaken.value,
    isOutboundArrived: deps.isOutboundArrived.value,
    unfinalizedCount: deps.unfinalizedCount.value,
    pickedQty: deps.pickedQty.value,
    expectedQty: deps.expectedQty.value,
    canUpdate: deps.canUpdate.value,
    isAdmin: deps.isAdmin.value,
    busy: deps.busy.value,
    finalizingBoxes: deps.finalizingBoxes.value,
    autoRelease: deps.autoRelease.value,
    printing: { ...deps.printing },
  }));

  const scenario = computed<Scenario>(() => resolveScenario(resolveCtx.value));
  const stepIndex = computed<number>(() => resolveStepIndex(scenario.value, resolveCtx.value));
  const step = computed<StepDescriptor>(() =>
    buildStepDescriptor(scenario.value, stepIndex.value, resolveCtx.value),
  );
  const stepTooltips = computed<string[]>(() => getStepTooltips(scenario.value));

  return {
    resolveCtx,
    scenario,
    stepIndex,
    step,
    stepTooltips,
  };
}
