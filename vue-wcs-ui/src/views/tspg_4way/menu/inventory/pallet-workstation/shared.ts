// 파렛트 작업대 전용 공유 모듈 — 타입, 상수, 순수 유틸 함수
// 부수효과 없음. 다른 파일에서 import해서 사용.

import {
  WcsOrderStatus,
  WcsOrderStatusLabels,
  WCS_ORDER_FINAL,
  WCS_ORDER_ERROR,
  BoxStatus,
} from '@/views/tspg_4way/constants/wcsConsts';

// ─────────────────────────────────────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────────────────────────────────────

export type BoxFilter = 'all' | 'printed' | 'scanned' | 'partial' | 'depleted' | 'pending';

export type QtyModalKind = 'adjust' | 'sample' | 'partial' | 'edit-total';

export type LabelReissueTarget =
  | {
      kind: 'single';
      boxId: string;
      boxBarcode: string;
      isFirstIssue: boolean;
      // partial outbound 직후 라벨 재발행 — 백엔드는 finalize 전이라 remaining_qty 가 stale.
      // 호출자가 정확한 잔량을 알고 있으면 여기 전달 → confirmLabelReissue 에서 override.
      partialOverride?: { remainingQty: number; pickedQty: number };
    }
  | { kind: 'multi'; boxIds: string[]; allFirstIssue: boolean };

export type ColDef = { key: string; label: string; width: number; minWidth: number; align?: 'right' };

export type DraftAddition = {
  tempId: string;
  itemCode: string;
  lotNo: string;
  totalQty: number;
  produceDate?: string | null;
  expiryDate?: string | null;
  uom?: string;
};

export type LifecycleEntry = {
  orderKey: string;
  orderType: string;
  subOrderType: string | null;
  orderStatus: number;
  fromLocCode: string | null;
  toLocCode: string | null;
  parentOrderKey: string | null;
  hostOrderKey: string | null;
  carryingStockId: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type PalletStateView = { label: string; cls: string };

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────

export const COL_STORAGE_KEY = 'PalletWorkstation_ColWidths_v3';

export const DEFAULT_COLUMNS: ColDef[] = [
  { key: 'select', label: '', width: 40, minWidth: 40 },
  { key: 'seq', label: '#', width: 50, minWidth: 40 },
  { key: 'barcode', label: '박스 바코드', width: 280, minWidth: 140 },
  { key: 'item', label: '품목', width: 180, minWidth: 100 },
  { key: 'lot', label: 'LOT', width: 90, minWidth: 60 },
  { key: 'picked', label: '출고', width: 70, minWidth: 50, align: 'right' },
  { key: 'progress', label: '잔여 / 전체', width: 220, minWidth: 140 },
  { key: 'status', label: '상태', width: 110, minWidth: 80 },
  { key: 'actions', label: '액션', width: 130, minWidth: 100 },
];

// ─────────────────────────────────────────────────────────────────────────────
// Box status 필터 헬퍼
// ─────────────────────────────────────────────────────────────────────────────

export const byStatus = (boxes: any[], code: number) =>
  boxes.filter((b) => Number(b.box_status) === code);

export const aliveBoxes = (boxes: any[]) =>
  boxes.filter((b) => {
    const s = Number(b.box_status);
    return s !== BoxStatus.DEPLETED.code && s !== BoxStatus.VOID.code;
  });

export const scannedBoxes = (boxes: any[]) => byStatus(boxes, BoxStatus.SCANNED.code);
export const printedBoxes = (boxes: any[]) => byStatus(boxes, BoxStatus.PRINTED.code);
export const depletedBoxes = (boxes: any[]) => byStatus(boxes, BoxStatus.DEPLETED.code);

// 미스캔 살아있는 박스 (SCANNED/DEPLETED/VOID 제외)
export const unscannedAlive = (boxes: any[]) =>
  boxes.filter((b) => {
    const s = Number(b.box_status);
    return s !== BoxStatus.SCANNED.code && s !== BoxStatus.DEPLETED.code && s !== BoxStatus.VOID.code;
  });

// ─────────────────────────────────────────────────────────────────────────────
// Pure utils — 박스 수량 추출
// ─────────────────────────────────────────────────────────────────────────────

export function totalOf(b: any): number {
  if (!b) return 0;
  return Number(b.total_qty ?? b.totalQty ?? 0);
}
export function pickedOf(b: any): number {
  if (!b) return 0;
  return Number(b.picked_qty ?? b.pickedQty ?? 0);
}
export function remainingOf(b: any): number {
  if (!b) return 0;
  if (b.remaining_qty != null) return Number(b.remaining_qty);
  if (b.remainingQty != null) return Number(b.remainingQty);
  return totalOf(b) - pickedOf(b);
}

// ─────────────────────────────────────────────────────────────────────────────
// Pure utils — 행 progress bar 시각
// ─────────────────────────────────────────────────────────────────────────────

export function cellBarPercent(b: any): number {
  const total = b._draftEditTotal != null ? Number(b._draftEditTotal) : totalOf(b);
  const remaining = b._draftEditTotal != null ? Number(b._draftEditTotal) : remainingOf(b);
  if (total <= 0) return 0;
  return Math.min(100, Math.round((remaining / total) * 100));
}
export function cellBarColor(b: any): string {
  const s = b.box_status;
  if (s === BoxStatus.DEPLETED.code) return 'bar-depleted';
  if (s === BoxStatus.SCANNED.code && (pickedOf(b) > 0 || remainingOf(b) < totalOf(b)))
    return 'bar-partial';
  if (s === BoxStatus.PRINTED.code) return 'bar-printed';
  if (s === BoxStatus.PENDING.code || s === BoxStatus.DRAFT.code) return 'bar-pending';
  return 'bar-default';
}

// ─────────────────────────────────────────────────────────────────────────────
// Pure utils — 에러 메시지 추출
// ─────────────────────────────────────────────────────────────────────────────

export function parseApiError(e: any): string {
  if (!e) return '알 수 없는 오류가 발생했습니다.';
  if (typeof e === 'string') return e;
  if (e.response) {
    const data = e.response.data;
    if (data) {
      if (typeof data === 'string') return data;
      if (data.message) return String(data.message);
      if (data.detail) return String(data.detail);
      if (data.error) {
        return typeof data.error === 'string'
          ? data.error
          : data.error.message || JSON.stringify(data.error);
      }
      if (data.msg) return String(data.msg);
    }
    if (e.response.statusText) return `${e.response.status} ${e.response.statusText}`;
  }
  if (e.message) return String(e.message);
  try {
    return JSON.stringify(e);
  } catch {
    return '알 수 없는 오류가 발생했습니다.';
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// localStorage helpers — 컬럼 너비 영속화
// ─────────────────────────────────────────────────────────────────────────────

export function loadColumnWidths(): ColDef[] {
  try {
    const raw = localStorage.getItem(COL_STORAGE_KEY);
    if (!raw) return DEFAULT_COLUMNS.map((c) => ({ ...c }));
    const saved: Record<string, number> = JSON.parse(raw);
    return DEFAULT_COLUMNS.map((c) => ({
      ...c,
      width: Math.max(c.minWidth, Number(saved[c.key]) || c.width),
    }));
  } catch {
    return DEFAULT_COLUMNS.map((c) => ({ ...c }));
  }
}
export function persistColumnWidths(columns: ColDef[]) {
  try {
    const map: Record<string, number> = {};
    for (const c of columns) map[c.key] = c.width;
    localStorage.setItem(COL_STORAGE_KEY, JSON.stringify(map));
  } catch (_) {}
}

// ─────────────────────────────────────────────────────────────────────────────
// Lifecycle — pure helpers
// ─────────────────────────────────────────────────────────────────────────────

// 진행 중 셔틀(act non-null)이 있으면 그 라벨이 우선. 없으면 lifecycle[0] 기준.
export function derivePalletStateLabel(
  act: any | null,
  lifecycle: LifecycleEntry[],
  percent: number,
): PalletStateView {
  if (act) {
    if (act.mode === 'POST_OUTBOUND') {
      if (act.fullyShipped) return { label: '완전 출고 완료 — 반출 가능', cls: 'state-ready' };
      if (act.requiresReinbound) return { label: '재입고 필요', cls: 'state-warn' };
      if (act.reinboundCompleted) return { label: '재입고 완료 — 반출 가능', cls: 'state-ready' };
      return { label: '출고 완료', cls: 'state-ready' };
    }
    if (act.mode === 'PENDING_SAMPLE')
      return { label: '시험 출고 완료 — 재입고 대기', cls: 'state-sample' };

    const st = act.orderStatus;
    const mode = act.mode;
    if (st >= WCS_ORDER_ERROR)
      return { label: `에러 (${WcsOrderStatusLabels[st] || st})`, cls: 'state-err' };
    if (st >= WCS_ORDER_FINAL)
      return { label: WcsOrderStatusLabels[st] || '완료', cls: 'state-ready' };
    if (mode === 'OUTBOUND') {
      if (act.sampleFlow) return { label: '시험 출고 진행 중', cls: 'state-sample' };
      if (st === WcsOrderStatus.CREATED) return { label: '출고 작업 대기', cls: 'state-out' };
      if (st === WcsOrderStatus.ARRIVED)
        return { label: '출고 도착 - 확정 대기', cls: 'state-out' };
      if (st >= WcsOrderStatus.SENT) return { label: '출고 진행 중', cls: 'state-out' };
      return { label: '출고 대기', cls: 'state-out' };
    }
    if (st === WcsOrderStatus.CREATED) return { label: 'BCR 스캔 대기', cls: 'state-in' };
    if (st === WcsOrderStatus.SENT) return { label: 'ECS 송신 완료', cls: 'state-progress' };
    if (st === WcsOrderStatus.ACCEPTED) return { label: 'ECS 수락됨', cls: 'state-progress' };
    if (st === WcsOrderStatus.WAITING) return { label: '실행 대기', cls: 'state-progress' };
    if (st === WcsOrderStatus.RUNNING) return { label: '입고 실행 중', cls: 'state-progress' };
    if (st === WcsOrderStatus.ARRIVED) return { label: '적치 완료', cls: 'state-progress' };
    return { label: WcsOrderStatusLabels[st] || '입고 진행 중', cls: 'state-in' };
  }

  if (lifecycle && lifecycle.length > 0) {
    const head = lifecycle[0];
    const st = head.orderStatus;
    if (st >= WCS_ORDER_ERROR)
      return {
        label: `최근 작업 에러: ${WcsOrderStatusLabels[st] || st}`,
        cls: 'state-err',
      };
    if (st === 91) return { label: '최근 작업 취소됨', cls: 'state-warn' };
    if (st === 95) return { label: '최근 작업 중단됨', cls: 'state-warn' };
    if (st >= WCS_ORDER_FINAL) {
      const ty = (head.orderType || '').toUpperCase();
      const sub = (head.subOrderType || 'NORMAL').toUpperCase();
      if (ty === 'INBOUND') {
        if (head.parentOrderKey) {
          const parent = lifecycle.find((e) => e.orderKey === head.parentOrderKey);
          if (parent && (parent.orderType || '').toUpperCase() === 'OUTBOUND')
            return { label: '최근 완료: 재입고 완료', cls: 'state-ready' };
        }
        return { label: '최근 완료: 입고 완료', cls: 'state-ready' };
      }
      if (ty === 'OUTBOUND') {
        if (sub === 'SAMPLE_OUT')
          return { label: '최근 완료: 시험 출고 완료', cls: 'state-sample' };
        if (sub === 'SAMPLE_DISCARD')
          return { label: '최근 완료: 시험 폐기 완료', cls: 'state-warn' };
        return { label: '최근 완료: 출고 완료', cls: 'state-ready' };
      }
      if (ty === 'MOVE') return { label: '최근 완료: 이동 완료', cls: 'state-progress' };
      return { label: '최근 완료', cls: 'state-ready' };
    }
    return { label: WcsOrderStatusLabels[st] || '진행 중', cls: 'state-progress' };
  }

  return percent === 100
    ? { label: '입고 가능 처리 대기', cls: 'state-ready' }
    : { label: '등록됨 (산출 전)', cls: 'state-idle' };
}

export function lifecycleEntryShortLabel(
  entry: LifecycleEntry,
  all: LifecycleEntry[],
): string {
  const ty = (entry.orderType || '').toUpperCase();
  const sub = (entry.subOrderType || 'NORMAL').toUpperCase();
  if (ty === 'INBOUND') {
    if (entry.parentOrderKey) {
      const parent = all.find((e) => e.orderKey === entry.parentOrderKey);
      if (parent && (parent.orderType || '').toUpperCase() === 'OUTBOUND') return '재입고';
    }
    return '입고';
  }
  if (ty === 'OUTBOUND') {
    if (sub === 'SAMPLE_OUT') return '시험출고';
    if (sub === 'SAMPLE_DISCARD') return '시험폐기';
    return '출고';
  }
  if (ty === 'MOVE') return '이동';
  return ty || '?';
}

export function lifecycleEntryChipClass(
  entry: LifecycleEntry,
  all: LifecycleEntry[],
): string {
  const st = entry.orderStatus;
  if (st >= WCS_ORDER_ERROR) return 'chip-life chip-life-err';
  if (st === 91 || st === 95) return 'chip-life chip-life-cancel';
  const short = lifecycleEntryShortLabel(entry, all);
  switch (short) {
    case '입고':
      return 'chip-life chip-life-inbound';
    case '재입고':
      return 'chip-life chip-life-reinbound';
    case '출고':
      return 'chip-life chip-life-outbound';
    case '시험출고':
      return 'chip-life chip-life-sample';
    case '시험폐기':
      return 'chip-life chip-life-discard';
    case '이동':
      return 'chip-life chip-life-move';
    default:
      return 'chip-life';
  }
}

export function formatLifecycleTime(
  iso: string | null | undefined,
  withSec = false,
): string {
  if (!iso) return '-';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return String(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  const base = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  return withSec ? `${base}:${pad(d.getSeconds())}` : base;
}
