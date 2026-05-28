// 박스 행 액션 빌더 — 단계별.

import { pickedOf, remainingOf, totalOf } from '../shared';
import { BoxStatus } from '/@/views/tspg_4way/constants/wcsConsts';
import type { RowActionDescriptor } from './types';

export function rowAction_inboundFinalize(b: any): RowActionDescriptor | null {
  if (b._isAddition) return { variant: 'edit', label: '추가 예정', tone: 'primary' };
  if (Number(b.box_status) === BoxStatus.DRAFT.code)
    return { variant: 'edit', label: '미확정', tone: 'muted' };
  return null;
}

// 라벨 인쇄 상태 표시(클릭 불가) — 실제 인쇄는 행의 별도 [라벨] 버튼.
export function rowAction_inboundPrint(b: any): RowActionDescriptor | null {
  if (b._isAddition) return null;
  const printed = Number(b.print_count ?? b.printCount ?? 0);
  return {
    variant: printed > 0 ? 'reprint' : 'print',
    label: printed > 0 ? '인쇄됨' : '인쇄 대기',
    tone: printed > 0 ? 'success' : 'muted',
  };
}

export function rowAction_scan(b: any): RowActionDescriptor | null {
  if (b._isAddition) return null;
  if (Number(b.box_status) === BoxStatus.SCANNED.code) {
    const picked = pickedOf(b);
    const total = totalOf(b);
    const remaining = remainingOf(b);
    if (picked > 0 || remaining < total) {
      return { variant: 'partial', label: '부분', tone: 'warn' };
    }
    return { variant: 'scanned', label: '스캔됨', tone: 'success' };
  }
  return { variant: 'wait', label: '대기', tone: 'muted' };
}

export function rowAction_doneOnly(b: any): RowActionDescriptor | null {
  if (b._isAddition) return null;
  if (Number(b.box_status) === BoxStatus.SCANNED.code) {
    return { variant: 'scanned', label: '스캔됨', tone: 'success' };
  }
  return null;
}

export function rowAction_sampleInput(b: any): RowActionDescriptor | null {
  if (b._isAddition) return null;
  if (Number(b.box_status) !== BoxStatus.SCANNED.code) return null;
  const picked = pickedOf(b);
  const total = totalOf(b);
  if (picked === 0) {
    return { variant: 'sample-input', label: '채취 입력', tone: 'primary', actionId: 'sample' };
  }
  if (picked >= total && total > 0) {
    return {
      variant: 'sample-edit',
      label: `전량 채취 ${picked}`,
      tone: 'warn',
      actionId: 'sample',
    };
  }
  return { variant: 'sample-edit', label: `채취 ${picked}`, tone: 'success', actionId: 'sample' };
}

// 전량 스캔 + 채취 입력 통합 — 미스캔은 '대기', SCANNED 박스는 채취 입력 가능.
export function rowAction_sampleScanAndInput(b: any): RowActionDescriptor | null {
  if (b._isAddition) return null;
  const s = Number(b.box_status);
  if (s === BoxStatus.DEPLETED.code || s === BoxStatus.VOID.code) return null;
  if (s === BoxStatus.SCANNED.code) {
    return rowAction_sampleInput(b);
  }
  return { variant: 'wait', label: '대기', tone: 'muted' };
}
