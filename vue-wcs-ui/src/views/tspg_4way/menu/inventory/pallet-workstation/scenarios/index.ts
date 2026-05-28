// 시나리오 모듈 — 기존 소비자가 './scenarios' 하나에서 모두 가져갈 수 있도록 통합 재export.

export * from './types';
export * from './rowActions';
export * from './admin';
export {
  SCENARIO_LABELS,
  SCENARIO_BLUEPRINTS,
  computeShuttleStage,
} from './blueprints';
export {
  resolveScenario,
  resolveStepIndex,
  buildStepDescriptor,
  getStepTooltips,
} from './resolve';
