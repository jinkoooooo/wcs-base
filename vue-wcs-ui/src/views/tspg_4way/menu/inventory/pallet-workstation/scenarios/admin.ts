// 시나리오·스텝별 관리자 우회 액션 계산.

import type { ActionId, ResolveCtx, Scenario } from './types';

/**
 * `isAdmin` 일 때만 표시. 부적합 조건(부분 출고 박스 등)은 백엔드가 거절 메시지로 응답.
 */
export function computeAdminActions(
  scenario: Scenario,
  stepIndex: number,
  ctx: ResolveCtx,
): Array<{ label: string; actionId: ActionId }> {
  if (!ctx.isAdmin) return [];
  const out: Array<{ label: string; actionId: ActionId }> = [];
  // 박스 스캔 단계(2)에서만 — 자동 완료로 입고 진행(3) 넘어가면 자연히 사라짐.
  if (scenario === 'INBOUND_NORMAL' && stepIndex === 2) {
    out.push({ label: '박스 스캔 자동 완료', actionId: 'adminBypassRelease' });
  }
  // 박스 스캔 단계(1)에서만 — 입고와 동일하게 스캔 완료(출고 확정 2)로 넘어가면 사라짐.
  if (
    (scenario === 'OUTBOUND_NORMAL' || scenario === 'OUTBOUND_PARTIAL_OUT') &&
    stepIndex === 1
  ) {
    out.push({ label: '박스 스캔 자동 완료', actionId: 'adminBypassFinalize' });
  }
  // SAMPLE_CYCLE 의 PENDING_SAMPLE 단계 — 박스 전수 자동 스캔 (채취/확정과 분리)
  if (scenario === 'SAMPLE_CYCLE' && ctx.act?.mode === 'PENDING_SAMPLE') {
    out.push({ label: '박스 전수 자동 스캔', actionId: 'adminBypassSample' });
  }
  return out;
}
