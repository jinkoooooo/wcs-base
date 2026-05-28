/**
 * statusHelpers.ts
 * 상태 판정 / 라벨 헬퍼 — 매직 넘버 제거용
 */

import {
  WcsOrderStatus,
  WcsOrderStatusLabels,
  WCS_ORDER_FINAL,
  WCS_ORDER_ERROR,
  WcsLocStatus,
  PortMode,
  type PortModeValue,
} from './wcsConsts';

// ============================================
// WCS 오더 상태
// ============================================

/** 종료된 오더(90+) */
export const isOrderFinished = (s?: number | null) => s != null && s >= WCS_ORDER_FINAL;

/** 에러 상태(100+) */
export const isOrderError = (s?: number | null) => s != null && s >= WCS_ORDER_ERROR;

/** 진행 중(<90) */
export const isOrderActive = (s?: number | null) => s != null && s < WCS_ORDER_FINAL;

/** 라벨 — 미정의 에러는 "에러 (xxx)" */
export function getOrderStatusText(s?: number | null): string {
  if (s == null) return '-';
  if (WcsOrderStatusLabels[s]) return WcsOrderStatusLabels[s];
  if (isOrderError(s)) return `에러 (${s})`;
  return `상태 ${s}`;
}

/** CSS 클래스 */
export function getOrderStatusClass(s?: number | null): string {
  if (s == null) return '';
  if (isOrderError(s)) return 'status-error';
  if (s === WcsOrderStatus.RUNNING) return 'status-working';
  return '';
}

// ============================================
// 로케이션 상태
// ============================================
export const isEmptyPick = (s?: number | null) => s === WcsLocStatus.EMPTY_PICK;
export const isDoubleEntry = (s?: number | null) => s === WcsLocStatus.DOUBLE_ENTRY;

// ============================================
// 포트 모드 (드레인 처리)
// ============================================
export const isPortDraining = (m?: string | null) =>
  m === PortMode.SWITCHING_TO_INBOUND.code || m === PortMode.SWITCHING_TO_OUTBOUND.code;

/** 드레인 목표 모드 */
export function getPortDrainTarget(m?: string | null): PortModeValue | null {
  if (m === PortMode.SWITCHING_TO_INBOUND.code) return PortMode.INBOUND.code;
  if (m === PortMode.SWITCHING_TO_OUTBOUND.code) return PortMode.OUTBOUND.code;
  return null;
}

/** 드레인 취소 시 복귀 모드 (반대 방향) */
export function getPortDrainOrigin(m?: string | null): PortModeValue | null {
  if (m === PortMode.SWITCHING_TO_INBOUND.code) return PortMode.OUTBOUND.code;
  if (m === PortMode.SWITCHING_TO_OUTBOUND.code) return PortMode.INBOUND.code;
  return null;
}
