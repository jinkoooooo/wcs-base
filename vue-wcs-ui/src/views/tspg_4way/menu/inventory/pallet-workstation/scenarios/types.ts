// 파렛트 작업대 시나리오 타입 모음.

import type { LifecycleEntry } from '../shared';

export type Scenario =
  | 'IDLE'
  | 'INBOUND_NORMAL'
  | 'OUTBOUND_NORMAL'
  | 'OUTBOUND_PARTIAL_OUT'
  | 'SAMPLE_CYCLE'
  | 'INBOUND_REINBOUND'
  | 'POST_OUTBOUND';

export type ActionId =
  | 'finalizeBoxes'
  | 'releaseInbound'
  | 'finalizeOutbound'
  | 'doReinbound'
  | 'doRemainderReinbound'
  | 'cancelReinbound'
  | 'printPallet'
  | 'printAllBoxes'
  | 'printSelectedBoxes'
  | 'printBoxLabel'
  | 'markPalletPrinted'
  | 'saveDraft'
  | 'cancelDraft'
  | 'editTotal'
  | 'cancelEdit'
  | 'void'
  | 'sample'
  | 'partial'
  | 'reset'
  | 'autoReleaseToggle'
  | 'openHistory'
  | 'confirmSampleScan'
  | 'adminBypassRelease'
  | 'adminBypassFinalize'
  | 'adminBypassSample'
  | 'adjust';

export type ScenarioBadgeTone = 'idle' | 'in' | 'out' | 'sample' | 'reinbound' | 'success';

export type RowActionVariant =
  | 'wait'
  | 'scanned'
  | 'partial'
  | 'edit'
  | 'print'
  | 'reprint'
  | 'sample-input'
  | 'sample-edit';

export interface PrimaryAction {
  label: string;
  actionId: ActionId | null;
  disabled: boolean;
  loading?: boolean;
  tooltip?: string;
  variant?: 'primary' | 'ghost' | 'success';
}

export interface RowActionDescriptor {
  variant: RowActionVariant;
  actionId?: ActionId | null;
  label?: string;
  tone?: 'muted' | 'success' | 'warn' | 'primary';
}

export interface ShuttleProgress {
  currentStage: number;
  stages: readonly string[];
}

export interface StepDescriptor {
  scenario: Scenario;
  badgeLabel: string;
  badgeTone: ScenarioBadgeTone;

  stepIndex: number;
  totalSteps: number;
  stage?: string;

  title: string;
  hint?: string;

  primaryInput: 'box' | 'pallet' | null;
  showProgressBar: boolean;
  progressValue?: number;
  progressMax?: number;
  progressColor?: 'in' | 'out';

  shuttleProgress?: ShuttleProgress;

  primaryAction: PrimaryAction | null;
  /** 관리자 전용 우회 액션 — admin 인 경우에만 채워짐. 카드 내 별도 영역 노출. */
  adminActions: Array<{ label: string; actionId: ActionId }>;
  auxActions: ActionId[];
  boxMenuActions: ActionId[];
  rowAction: (box: any) => RowActionDescriptor | null;
}

export interface ResolveCtx {
  info: any;
  act: any;
  boxes: any[];
  lifecycle: LifecycleEntry[];

  hasUnsavedChanges: boolean;
  hasDraft: boolean;
  hasUnprintedActive: boolean;
  scanComplete: boolean;
  outboundComplete: boolean;
  hasPartialPicked: boolean;
  anyTaken: boolean;
  isOutboundArrived: boolean;

  unfinalizedCount: number;
  pickedQty: number;
  expectedQty: number;

  canUpdate: boolean;
  isAdmin: boolean;
  busy: boolean;
  finalizingBoxes: boolean;
  autoRelease: boolean;
  printing: { pallet: boolean; boxes: boolean; mark: boolean; selected: boolean };
}

export interface StepBlueprint {
  title: string;
  hint?: string;
  primaryInput: 'box' | 'pallet' | null;
  showProgressBar: boolean;
  progressColor?: 'in' | 'out';
  shuttleStages?: readonly string[];
  stage?: string;
  isHere: (ctx: ResolveCtx) => boolean;
  primaryAction: (ctx: ResolveCtx) => PrimaryAction | null;
  auxActions: (ctx: ResolveCtx) => ActionId[];
  boxMenuActions: ActionId[];
  rowAction: (box: any) => RowActionDescriptor | null;
}

// 확정 미리보기 모달 — 시험/관리자 우회 공통.
export interface FinalizeGroup {
  key: string;
  title: string;
  tone: 'warn' | 'success' | 'muted' | 'primary';
  items: Array<{ label: string; sub?: string }>;
  summary?: string;
}
