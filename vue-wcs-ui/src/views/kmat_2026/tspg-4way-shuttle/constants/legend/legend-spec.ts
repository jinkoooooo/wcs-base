/**
 * Dashboard2D Legend 단일 소스 (SSOT).
 *
 * 이 파일 하나에서만 legend 의 섹션/코드/라벨/색을 정의한다.
 * Dashboard2D 의 legend 렌더, shuttle/conveyor/pallet 색 결정 로직은
 * 전부 이 모듈을 import 해서 쓴다.
 *
 * 원칙
 * ────
 * 1. Rack 섹션 색은 반드시 `var(--cs-*)` 형태. hex 복제 금지.
 *    (cell-state.scss 가 유일한 색 정의 소스)
 * 2. Shuttle / Conveyor / Pallet 색은 이 파일에서 hex 로 한 번만 정의.
 *    다른 컴포넌트는 `stateColor()` / `shuttleStateColor()` 등 export 된 헬퍼로 참조.
 * 3. 코드값(ShuttleState 등) 은 백엔드 DTO 의 정규화 필드값과 **정확히 일치**.
 *    예: ShuttleState.name() = "RUNNING" ↔ SHUTTLE_STATES[code:"RUNNING"]
 */

// ---------------------------------------------
// 타입 정의
// ---------------------------------------------

export type LegendSection =
  | 'rack' // 재고 축 (state_code) + 금지 축 (locked/forbid-in/forbid-out)
  | 'rack-type' // 포트 종류 (입고/출고/입출고/충전/주행전용)
  | 'rack-alert' // 정합성 이상 감지 (공출고 / 이중입고)
  | 'shuttle'
  | 'conveyor'
  | 'pallet'
  | 'equipment'; // PILLAR / BCR / STV / CRANE 등 단순 표시 설비

export type RackStateSubSection = 'basic' | 'inout' | 'alert';

/**
 * DISABLED 같은 "빗금+진한그레이" 조합이나,
 * Rack Type "주행전용" 같은 특수 렌더가 필요한 경우 힌트로 사용.
 */
export type LegendPattern =
  | 'hatched'
  | 'striped-gray'
  | 'gradient-inout'
  | 'forbid-in'
  | 'forbid-out'
  | 'locked'
  | 'expired';

export interface LegendItem {
  section: LegendSection;
  /** 백엔드 정규화 필드와 일치하는 코드값 (Rack 은 state_code, Rack Type 은 RackType 이름/특수명) */
  code: string;
  /** 사용자에게 보일 한글 라벨 */
  label: string;
  /**
   * 색. Rack 섹션은 반드시 `var(--cs-*)` 형태.
   * Rack Type 중 일부(주행전용/공출고감지/이중입고감지) 는 pattern 으로 표현하므로 생략 가능.
   * Shuttle/Conveyor/Pallet 은 hex.
   */
  color?: string;
  /** 특수 렌더 힌트. 렌더러가 이 값을 보고 대응 CSS 클래스 선택. */
  pattern?: LegendPattern;
  /**
   * Rack Type 전용: dashboard.scss 에 정의된 기존 CSS 클래스를 직접 지정.
   * 예: `badge-inbound`, `badge-outbound`, `badge-inout`, `badge-charge`, `badge-charge-enter`.
   */
  cssClass?: string;
  /**
   * 캔버스 랙 셀 틴트 반투명도 (0~1).
   * Rack 섹션 전용. 0 또는 미설정이면 틴트 없음 (EMPTY/NONE 등).
   * rackTint.ts 가 이 값을 읽어 background/boxShadow 를 계산한다.
   */
  tintAlpha?: number;
  /**
   * Rack 섹션 전용 — 백엔드 state_code 가 이 코드일 때 "물리 재고가 있다"고
   * 판정 (카고 인디케이터 표시 여부의 SSOT).
   */
  hasInventory?: boolean;

  /**
   * Rack 섹션 전용 — CellStateLegend 가 재고 축을
   * "기본 / 입·출고 / 이상·확인" 으로 나눠 표시할 때 사용.
   * 미지정 시 'basic' 으로 간주.
   */
  subSection?: RackStateSubSection;
  /**
   * 칩 테두리 색. 미지정 시 color 와 동일하게 처리.
   * cell-state.scss 의 --cs-*-border 변수 그대로 var(--cs-*-border) 형태로 지정.
   */
  border?: string;
}

// ---------------------------------------------
// Rack — 재고 축 (CellStateService.state_code)
// ---------------------------------------------
//
// cell-state.scss 의 CSS 변수에서만 색을 참조. hex 복제 금지.

export const RACK_STATES: LegendItem[] = [
  // ── 기본 ─────────────────────────────────────────────────────────────
  {
    section: 'rack',
    code: 'EMPTY',
    label: '비어있음',
    subSection: 'basic',
    color: 'var(--cs-empty-color)',
    border: 'var(--cs-empty-border)',
    tintAlpha: 0,
  },
  {
    section: 'rack',
    code: 'NONE',
    label: '셀없음',
    subSection: 'basic',
    color: 'var(--cs-none-color)',
    border: 'var(--cs-none-border)',
    tintAlpha: 0,
  },

  // ── 입·출고 진행 ────────────────────────────────────────────────────
  {
    section: 'rack',
    code: 'PRODUCT',
    label: '제품',
    subSection: 'inout',
    color: 'var(--cs-product-color)',
    border: 'var(--cs-product-border)',
    tintAlpha: 0.3,
    hasInventory: true,
  },
  {
    section: 'rack',
    code: 'EMPTY_BOX',
    label: '공박스',
    subSection: 'inout',
    color: 'var(--cs-emptybox-color)',
    border: 'var(--cs-emptybox-border)',
    tintAlpha: 0.3,
    hasInventory: true,
  },
  {
    section: 'rack',
    code: 'INBOUND',
    label: '입고 중',
    subSection: 'inout',
    color: 'var(--cs-inbound-color)',
    border: 'var(--cs-inbound-border)',
    tintAlpha: 0.35,
  },
  {
    section: 'rack',
    code: 'INBOUND_READY',
    label: '입고 대기',
    subSection: 'inout',
    color: 'var(--cs-inboundready-color)',
    border: 'var(--cs-inboundready-border)',
    tintAlpha: 0.32,
  },
  {
    section: 'rack',
    code: 'HOST_PENDING',
    label: 'HOST 예약',
    subSection: 'inout',
    color: 'var(--cs-hostpending-color)',
    border: 'var(--cs-hostpending-border)',
    tintAlpha: 0.34,
    hasInventory: true,
  },
  {
    section: 'rack',
    code: 'OUTBOUND',
    label: '출고 중',
    subSection: 'inout',
    color: 'var(--cs-outbound-color)',
    border: 'var(--cs-outbound-border)',
    tintAlpha: 0.38,
    hasInventory: true,
  },
  {
    section: 'rack',
    code: 'RELOCATION',
    label: '정렬 중',
    subSection: 'inout',
    color: 'var(--cs-relocation-color)',
    border: 'var(--cs-relocation-border)',
    tintAlpha: 0.35,
    hasInventory: true,
  },

  // ── 이상 / 확인 ──────────────────────────────────────────────────────
  {
    section: 'rack',
    code: 'QC_PENDING',
    label: '시험 대기',
    subSection: 'alert',
    color: 'var(--cs-qcpending-color)',
    border: 'var(--cs-qcpending-border)',
    tintAlpha: 0.36,
    hasInventory: true,
  },
  // ABNORMAL — QC 부적합 + 사용기한 만료 통합. 상세 패널에서 사유 분리 표시.
  {
    section: 'rack',
    code: 'ABNORMAL',
    label: '부적합·사용기한 만료',
    subSection: 'alert',
    color: 'var(--cs-abnormal-color)',
    border: 'var(--cs-abnormal-border)',
    tintAlpha: 0.42,
    hasInventory: true,
  },
  {
    section: 'rack',
    code: 'NIA_PENDING',
    label: '국검 대기',
    subSection: 'alert',
    color: 'var(--cs-niapending-color)',
    border: 'var(--cs-niapending-border)',
    tintAlpha: 0.36,
    hasInventory: true,
  },
  {
    section: 'rack',
    code: 'RETURN',
    label: '반품',
    subSection: 'alert',
    color: 'var(--cs-return-color)',
    border: 'var(--cs-return-border)',
    tintAlpha: 0.36,
    hasInventory: true,
  },
  {
    section: 'rack',
    code: 'DISPOSAL',
    label: '폐기',
    subSection: 'alert',
    color: 'var(--cs-disposal-color)',
    border: 'var(--cs-disposal-border)',
    tintAlpha: 0.4,
    hasInventory: true,
  },
  {
    section: 'rack',
    code: 'DOUBLE_IN',
    label: '이중입고',
    subSection: 'alert',
    color: 'var(--cs-doublein-color)',
    border: 'var(--cs-doublein-border)',
    tintAlpha: 0.36,
  },
  {
    section: 'rack',
    code: 'EMPTY_OUT',
    label: '공출고',
    subSection: 'alert',
    color: 'var(--cs-emptyout-color)',
    border: 'var(--cs-emptyout-border)',
    tintAlpha: 0.4,
  },
  {
    section: 'rack',
    code: 'CONFIRM',
    label: '적재 확인',
    subSection: 'alert',
    color: 'var(--cs-confirm-color)',
    border: 'var(--cs-confirm-border)',
    tintAlpha: 0.34,
  },
];

// Rack — 금지 축 (locked / inbound_forbidden / outbound_forbidden)
export const RACK_FLAGS: LegendItem[] = [
  { section: 'rack', code: 'LOCK', label: '사용금지 (LOCK)', pattern: 'locked' },
  { section: 'rack', code: 'FORBID_IN', label: '입고 금지', pattern: 'forbid-in' },
  { section: 'rack', code: 'FORBID_OUT', label: '출고 금지', pattern: 'forbid-out' },
  { section: 'rack', code: 'EXPIRED', label: '유통기한 만료', pattern: 'expired' },
];

// ---------------------------------------------
// Rack Type — 포트/특수 랙 (기존 유지)
// ---------------------------------------------
//
// RackType enum 값(INBOUND_PORT=21 등) 또는 별도 플래그/state_code 기반.
// 렌더는 dashboard.scss 의 기존 .rack-type-badge.badge-* 클래스를 그대로 사용.

export const RACK_TYPES: LegendItem[] = [
  { section: 'rack-type', code: 'INBOUND_PORT', label: '입고포트', cssClass: 'badge-inbound' },
  { section: 'rack-type', code: 'OUTBOUND_PORT', label: '출고포트', cssClass: 'badge-outbound' },
  {
    section: 'rack-type',
    code: 'IN_OUTBOUND_PORT',
    label: '입출고포트',
    cssClass: 'badge-inout',
    pattern: 'gradient-inout',
  },
  { section: 'rack-type', code: 'CHARGE_PORT', label: '충전포트', cssClass: 'badge-charge' },
  { section: 'rack-type', code: 'DRIVE_ONLY', label: '주행전용', pattern: 'striped-gray' },
];

/**
 * Rack 정합성 이상 감지 — tb_wcs_loc_mst.status 기반.
 * RackType enum 과 무관하므로 별도 섹션으로 분리 (운영자 혼동 방지).
 *   EMPTY_PICK_DETECT   : wcsLocStatus === 30  — 시스템 재고 O, 실물 X
 *   DOUBLE_ENTRY_DETECT : wcsLocStatus === 40  — 실물 O, 시스템 재고 X
 */
export const RACK_ALERTS: LegendItem[] = [
  {
    section: 'rack-alert',
    code: 'EMPTY_PICK_DETECT',
    label: '공출고 감지 (재고 O / 실물 X)',
    color: '#F56C6C',
  },
  {
    section: 'rack-alert',
    code: 'DOUBLE_ENTRY_DETECT',
    label: '이중입고 감지 (실물 O / 재고 X)',
    color: '#E6A23C',
  },
];

// ---------------------------------------------
// Shuttle (Q1 확정)
// ---------------------------------------------
// 우선순위: DISABLED > ERROR > MANUAL > CHARGING > RUNNING > IDLE

export const SHUTTLE_STATES: LegendItem[] = [
  { section: 'shuttle', code: 'DISABLED', label: '사용안함', color: '#4B5563', pattern: 'hatched' },
  { section: 'shuttle', code: 'ERROR', label: '이상', color: '#EF4444' },
  { section: 'shuttle', code: 'MANUAL', label: '수동', color: '#6B7280' },
  { section: 'shuttle', code: 'CHARGING', label: '충전중', color: '#A855F7' },
  { section: 'shuttle', code: 'RUNNING', label: '작동중', color: '#22C55E' },
  { section: 'shuttle', code: 'IDLE', label: '대기', color: '#3B82F6' },
];

// ---------------------------------------------
// Conveyor (Q3 확정)
// ---------------------------------------------
// 우선순위: DISABLED > ERROR > MANUAL > AUTO

export const CONVEYOR_STATES: LegendItem[] = [
  {
    section: 'conveyor',
    code: 'DISABLED',
    label: '사용안함',
    color: '#4B5563',
    pattern: 'hatched',
  },
  { section: 'conveyor', code: 'ERROR', label: '이상', color: '#EF4444' },
  { section: 'conveyor', code: 'AUTO', label: '자동', color: '#22C55E' },
  { section: 'conveyor', code: 'MANUAL', label: '수동', color: '#6B7280' },
];

// ---------------------------------------------
// Pallet (Conveyor 위 화물, Q5 확정 — Q2: 셔틀 화물은 legend 미표시)
// ---------------------------------------------

export const PALLET_STATES: LegendItem[] = [
  { section: 'pallet', code: 'DATA_AND_PHY', label: '정상 (데이터+화물)', color: '#22C55E' },
  { section: 'pallet', code: 'PHY_ONLY', label: '화물만 (데이터 없음)', color: '#F59E0B' },
  { section: 'pallet', code: 'DATA_ONLY', label: '데이터만 (화물 없음)', color: '#3B82F6' },
];

// ---------------------------------------------
// Equipment — PILLAR / BCR / STV / CRANE (Phase D)
// ---------------------------------------------
// 단순 박스+라벨로 표시되는 정적 설비. 색은 Dashboard2D.vue 의 *-placeholder
// SCSS 와 1:1 일치해야 함 (변경 시 양쪽 동시 수정).

export const EQUIPMENT_TYPES: LegendItem[] = [
  { section: 'equipment', code: 'PILLAR', label: '기둥 (PILLAR)', color: '#555555' },
  { section: 'equipment', code: 'BCR', label: 'BCR (바코드 리더)', color: '#2563EB' },
  { section: 'equipment', code: 'STV', label: 'STV (스태커)', color: '#D97706' },
  { section: 'equipment', code: 'CRANE', label: 'CRANE (크레인)', color: '#7C3AED' },
];

// ---------------------------------------------
// 섹션 구조 — DashboardLegend.vue 가 이 순서 그대로 렌더
// ---------------------------------------------

export interface LegendSectionSpec {
  section: LegendSection;
  title: string;
  items: LegendItem[];
}

export const LEGEND_SECTIONS: LegendSectionSpec[] = [
  { section: 'rack', title: 'Rack (셀 상태)', items: [...RACK_STATES, ...RACK_FLAGS] },
  { section: 'rack-type', title: 'Rack Type', items: RACK_TYPES },
  { section: 'rack-alert', title: 'Rack 이상 감지', items: RACK_ALERTS },
  { section: 'shuttle', title: 'Shuttle (셔틀)', items: SHUTTLE_STATES },
  { section: 'conveyor', title: 'Conveyor (컨베이어)', items: CONVEYOR_STATES },
  { section: 'pallet', title: 'Conveyor Pallet', items: PALLET_STATES },
  { section: 'equipment', title: 'Equipment (기타 설비)', items: EQUIPMENT_TYPES },
];

// ---------------------------------------------
// 색 조회 헬퍼 — 렌더 컴포넌트에서 공용으로 사용
// ---------------------------------------------
//
// 백엔드 DTO 의 shuttleState / conveyorState / palletState 문자열을 받아
// 해당 색(hex) 을 돌려준다. 알 수 없는 코드면 null.

function lookup(items: LegendItem[], code: string | null | undefined): LegendItem | null {
  if (!code) return null;
  return items.find((x) => x.code === code) ?? null;
}

export function shuttleStateSpec(code: string | null | undefined): LegendItem | null {
  return lookup(SHUTTLE_STATES, code);
}

export function conveyorStateSpec(code: string | null | undefined): LegendItem | null {
  return lookup(CONVEYOR_STATES, code);
}

export function palletStateSpec(code: string | null | undefined): LegendItem | null {
  return lookup(PALLET_STATES, code);
}

export function shuttleStateColor(code: string | null | undefined): string | null {
  return shuttleStateSpec(code)?.color ?? null;
}

export function conveyorStateColor(code: string | null | undefined): string | null {
  return conveyorStateSpec(code)?.color ?? null;
}

export function palletStateColor(code: string | null | undefined): string | null {
  return palletStateSpec(code)?.color ?? null;
}

// ---------------------------------------------
// 색 조회 헬퍼들 (기존) 옆에 추가
// ---------------------------------------------

/** Rack state_code 의 legend 항목 조회 */
export function rackStateSpec(code: string | null | undefined): LegendItem | null {
  return lookup(RACK_STATES, code);
}

/** Rack state_code 의 라벨. 못 찾으면 코드 자체를 반환. */
export function rackStateLabel(code: string | null | undefined): string {
  return rackStateSpec(code)?.label ?? code ?? '';
}

/** 카고 인디케이터 표시 여부 — SSOT */
export function hasInventoryByStateCode(code: string | null | undefined): boolean {
  return rackStateSpec(code)?.hasInventory === true;
}

// ---------------------------------------------
// SubSection 헬퍼 — CellStateLegend 가 사용
// ---------------------------------------------

/** subSection 별 라벨 (그룹 헤더 표시용) */
export const RACK_SUB_SECTION_TITLES: Record<RackStateSubSection, string> = {
  basic: '기본',
  inout: '입·출고 진행',
  alert: '이상 / 확인',
};

/** subSection 별로 그룹핑된 RACK_STATES 반환 */
export function rackStatesBySubSection(): Array<{
  subSection: RackStateSubSection;
  title: string;
  items: LegendItem[];
}> {
  const order: RackStateSubSection[] = ['basic', 'inout', 'alert'];
  return order.map((sub) => ({
    subSection: sub,
    title: RACK_SUB_SECTION_TITLES[sub],
    items: RACK_STATES.filter((it) => (it.subSection ?? 'basic') === sub),
  }));
}

/**
 * DISABLED 등 hatched 패턴이 필요한지 여부.
 */
export function isHatched(spec: LegendItem | null): boolean {
  return !!spec && spec.pattern === 'hatched';
}
