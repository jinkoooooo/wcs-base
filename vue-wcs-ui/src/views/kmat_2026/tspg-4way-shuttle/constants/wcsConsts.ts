/**
 * wcsConsts.ts
 * WCS 도메인 상수 — 백엔드 WcsDomainEnums.java 미러
 *
 * 모든 enum 은 `enumHelpers.ts` 의 EnumEntry 패턴을 따른다:
 *   { code, label, ...meta }
 * 별도 FooLabels / FooClassMap 같은 동행 객체는 만들지 않는다.
 *
 * NOTE: StockStatus / WcsLocType / WcsOrderType 은 EquipmentDetailPopup 작업
 *       완료 시점까지 분리 정의 형태를 한시적으로 유지한다.
 */

import { toSelectOptions } from './EcsDBConsts';
import { enumToSelectOptions } from './enumHelpers';

// ============================================
// WCS 셔틀 오더 상태 (tb_wcs_shuttle_order.order_status)
// 0~89: 정상, 90~99: 종료, 100+: 에러
// 백엔드 SSOT: operato.logis.wcs.consts.ShuttleOrderStatus
// ============================================
export const WcsOrderStatus = {
  CREATED: { code: 0, label: '생성됨', badgeVariant: 'waiting', phase: 'normal' },
  SENT: { code: 10, label: '전송됨', badgeVariant: 'assigned', phase: 'normal' },
  ACCEPTED: { code: 20, label: '수락됨', badgeVariant: 'assigned', phase: 'normal' },
  WAITING: { code: 25, label: '대기', badgeVariant: 'waiting', phase: 'normal' },
  RUNNING: { code: 30, label: '실행중', badgeVariant: 'in-progress', phase: 'normal' },
  ARRIVED: { code: 40, label: '도착', badgeVariant: 'in-progress', phase: 'normal' },
  COMPLETED: { code: 90, label: '완료', badgeVariant: 'completed', phase: 'final' },
  CANCELLED: { code: 91, label: '취소', badgeVariant: 'cancelled', phase: 'final' },
  ABORTED: { code: 95, label: '중단', badgeVariant: 'error', phase: 'final' },
  ERROR_GENERAL: { code: 100, label: '일반 에러', badgeVariant: 'error', phase: 'error' },
  ERROR_SEND_FAIL: { code: 110, label: '전송 실패', badgeVariant: 'error', phase: 'error' },
  ERROR_TIMEOUT: { code: 120, label: '응답 지연', badgeVariant: 'error', phase: 'error' },
  ERROR_HARDWARE: { code: 130, label: '설비 에러', badgeVariant: 'error', phase: 'error' },
  ERROR_LOCATION: { code: 140, label: '로케이션 에러', badgeVariant: 'error', phase: 'error' },
  ERROR_INVENTORY: { code: 150, label: '재고 에러', badgeVariant: 'error', phase: 'error' },
  ERROR_SYSTEM_RESTART: {
    code: 190,
    label: '시스템 재시작 에러',
    badgeVariant: 'error',
    phase: 'error',
  },
} as const;

export type WcsOrderStatusCode = (typeof WcsOrderStatus)[keyof typeof WcsOrderStatus]['code'];

/** 종료(90+) / 에러(100+) 임계값 — 숫자 비교용 */
export const WCS_ORDER_FINAL = 90;
export const WCS_ORDER_ERROR = 100;

// ============================================
// HOST 오더 상태
// 백엔드 SSOT: operato.logis.wcs.consts.HostOrderStatus
// ============================================
export const HostOrderStatus = {
  RECEIVED: { code: 0, label: '수신됨', phase: 'normal' },
  WAITING_SCHEDULE: { code: 5, label: '예정일 대기', phase: 'normal' },
  VALIDATED: { code: 10, label: '검증완료', phase: 'normal' },
  READY_FOR_ALLOC: { code: 12, label: '산출 준비 완료', phase: 'normal' },
  WAITING_EXEC: { code: 30, label: 'ECS 실행 대기', phase: 'normal' },
  EXECUTING: { code: 40, label: '설비 실행 중', phase: 'normal' },
  PUTBACK_WAIT: { code: 60, label: '재입고 shuttle 미완 대기', phase: 'normal' },
  INBOUND_TEST_WAIT: { code: 75, label: '입고 후 시험 미종결', phase: 'normal' },
  COMPLETED: { code: 80, label: '완료', phase: 'final' },
  CANCELLED: { code: 85, label: '취소', phase: 'final' },
  REJECTED: { code: 88, label: '거절', phase: 'final' },
  TEST_FAILED: { code: 90, label: '시험 부적합 - 폐기 대기', phase: 'final' },
  ERROR: { code: 100, label: '오류', phase: 'error' },
} as const;

export type HostOrderStatusCode = (typeof HostOrderStatus)[keyof typeof HostOrderStatus]['code'];

// ============================================
// WCS ECS 인터페이스 상태 (tb_wcs_shuttle_order.ecs_if_status)
// ⚠️ ECS 도메인의 EcsIfStatus와 다름
// ============================================
export const WcsEcsIfStatus = {
  READY: { code: 0, label: '대기중' },
  SENDING: { code: 10, label: '전송중' },
  SENT: { code: 20, label: '전송됨' },
  ACK: { code: 30, label: '응답수신' },
  FAIL: { code: 99, label: '실패' },
} as const;

export type WcsEcsIfStatusCode = (typeof WcsEcsIfStatus)[keyof typeof WcsEcsIfStatus]['code'];

// ============================================
// ECS 콜백 상태 (문자열)
// ============================================
export const EcsCallbackStatus = {
  ACCEPTED: { code: 'ACCEPTED', label: '작업 수락' },
  STARTED: { code: 'STARTED', label: '작업 시작' },
  IN_PROGRESS: { code: 'IN_PROGRESS', label: '작업 진행 중' },
  FROM_LOADING_COMPLETE: { code: 'FROM_LOADING_COMPLETE', label: '출발지 로딩 완료' },
  TO_UNLOADING_COMPLETE: { code: 'TO_UNLOADING_COMPLETE', label: '목적지 언로딩 완료' },
  RACK_CONVEYOR_ARRIVED: { code: 'RACK_CONVEYOR_ARRIVED', label: '랙단 컨베이어 도착' },
  COMPLETE: { code: 'COMPLETE', label: '작업 완료' },
  ERROR: { code: 'ERROR', label: '작업 오류' },
  CANCELLED: { code: 'CANCELLED', label: '작업 취소' },
} as const;

export type EcsCallbackStatusCode =
  (typeof EcsCallbackStatus)[keyof typeof EcsCallbackStatus]['code'];

// ============================================
// 포트 모드 — 코드 / 한글 라벨 / 짧은 라벨 / 아이콘 / 배지 variant 모두 한 항목에 통합.
// 색상 정의는 styles/tokens.scss 의 `--tspg-port-{variant}-*` 변수와 매핑.
// ============================================
export const PortMode = {
  IDLE: {
    code: 'IDLE',
    label: '유휴',
    shortLabel: '유휴',
    icon: '◯',
    badgeVariant: 'idle',
  },
  INBOUND: {
    code: 'INBOUND',
    label: '입고',
    shortLabel: '입고',
    icon: '⬇',
    badgeVariant: 'in',
  },
  OUTBOUND: {
    code: 'OUTBOUND',
    label: '출고',
    shortLabel: '출고',
    icon: '⬆',
    badgeVariant: 'out',
  },
  OUTBOUND_PRIORITY: {
    code: 'OUTBOUND_PRIORITY',
    label: '출고 우선',
    shortLabel: '출고우선',
    icon: '⬆',
    badgeVariant: 'out',
  },
  SWITCHING_TO_INBOUND: {
    code: 'SWITCHING_TO_INBOUND',
    label: '입고 전환 대기',
    shortLabel: '입고전환중',
    icon: '⬇',
    badgeVariant: 'switching',
  },
  SWITCHING_TO_OUTBOUND: {
    code: 'SWITCHING_TO_OUTBOUND',
    label: '출고 전환 대기',
    shortLabel: '출고전환중',
    icon: '⬆',
    badgeVariant: 'switching',
  },
} as const;

export type PortModeCode = (typeof PortMode)[keyof typeof PortMode]['code'];
/** @deprecated `PortModeCode` 사용 — 외부 API DTO 호환용으로만 유지 */
export type PortModeValue = PortModeCode;

// ============================================
// 운영 모드
// ============================================
export const WcsOperationMode = {
  NORMAL: { code: 'NORMAL', label: '정상' },
  INBOUND_PRIORITY: { code: 'INBOUND_PRIORITY', label: '입고 우선' },
  OUTBOUND_PRIORITY: { code: 'OUTBOUND_PRIORITY', label: '출고 우선' },
  RELOCATION: { code: 'RELOCATION', label: '재배치' },
  MAINTENANCE: { code: 'MAINTENANCE', label: '점검' },
} as const;

export type WcsOperationModeCode = (typeof WcsOperationMode)[keyof typeof WcsOperationMode]['code'];

// ============================================
// 포트 룰 타입
// ============================================
export const PortRuleType = {
  INTERLEAVING: { code: 'INTERLEAVING', label: '교차 제어' },
  PARALLEL: { code: 'PARALLEL', label: '동시 처리' },
} as const;

export type PortRuleTypeCode = (typeof PortRuleType)[keyof typeof PortRuleType]['code'];

// ============================================
// WCS 오더 타입 (문자열, ⚠️ ECS의 EcsOrderType과 다름)
// NOTE: EquipmentDetailPopup 가 WcsOrderType.X (코드값) 와 WcsOrderTypeWorkingLabels 를
//       기존 분리 패턴으로 사용 중이다. 해당 컴포넌트 작업 완료 후 EnumEntry 패턴으로 통합 예정.
// ============================================
export const WcsOrderType = {
  INBOUND: 'INBOUND',
  OUTBOUND: 'OUTBOUND',
  MOVE: 'MOVE',
} as const;

export const WcsOrderTypeLabels: Record<string, string> = {
  INBOUND: '입고',
  OUTBOUND: '출고',
  MOVE: '이동',
};

/** 작업 진행 중 강조 라벨 */
export const WcsOrderTypeWorkingLabels: Record<string, string> = {
  INBOUND: '입고 작업 중',
  OUTBOUND: '출고 작업 중',
  MOVE: '이동 작업 중',
};

// ============================================
// WCS 로케이션 타입 (문자열)
// NOTE: EquipmentDetailPopup 가 WcsLocType.INBOUND_PORT 등 코드값으로 직접 비교 중이다.
//       해당 컴포넌트 작업 완료 후 EnumEntry 패턴으로 통합 예정.
// ============================================
export const WcsLocType = {
  RACK: 'RACK',
  INBOUND_PORT: 'INBOUND_PORT',
  OUTBOUND_PORT: 'OUTBOUND_PORT',
  IN_OUTBOUND_PORT: 'IN_OUTBOUND_PORT',
  CHARGE_PORT: 'CHARGE_PORT',
  VIRTUAL: 'VIRTUAL',
} as const;

export const WcsLocTypeLabels: Record<string, string> = {
  RACK: '물리 랙 셀',
  INBOUND_PORT: '입고 포트',
  OUTBOUND_PORT: '출고 포트',
  IN_OUTBOUND_PORT: '입출고 포트',
  CHARGE_PORT: '충전 포트',
  VIRTUAL: '가상 위치',
};

// ============================================
// WCS 로케이션 상태
// ============================================
export const WcsLocStatus = {
  EMPTY: { code: 0, label: '비어있음' },
  OCCUPIED: { code: 10, label: '재고있음' },
  LOCKED: { code: 20, label: '작업중' },
  EMPTY_PICK: { code: 30, label: '공출고 감지' },
  DOUBLE_ENTRY: { code: 40, label: '이중입고 감지' },
  DISABLED: { code: 90, label: '사용불가' },
} as const;

export type WcsLocStatusCode = (typeof WcsLocStatus)[keyof typeof WcsLocStatus]['code'];

// ============================================
// WCS 에러 코드
// ============================================
export const WcsError = {
  MISSING_REQUIRED_FIELD: 'ERR_REQUIRED',
  INVALID_ORDER_TYPE: 'ERR_ORDER_TYPE',
  DUPLICATE_LINE: 'ERR_DUP_LINE',
  INSUFFICIENT_STOCK: 'ERR_STOCK',
  NO_AVAILABLE_LOCATION: 'ERR_LOC',
  ALLOCATION_FAILED: 'ERR_ALLOC',
  ECS_SEND_FAILED: 'ERR_ECS',
  NO_AVAILABLE_STOCK: 'ERR_NO_AVAILABLE_STOCK',
  INVALID_REQUEST: 'ERR_BAD_REQUEST',
  INVALID_ORDER_ITEM: 'ERR_BAD_ITEM',
  LOCATION_LOCKED: 'ERR_LOC_LOCKED',
  ORDER_NOT_FOUND: 'ERR_ORDER_NOT_FOUND',
  STOCK_RESERVATION_FAILED: 'ERR_RESERVE',
  INVALID_PARAMETER: 'ERR_INVALID_PARAM',
  INTERNAL_ERROR: 'ERR_INTERNAL',
  ALLOCATION_GATED: 'ERR_ALLOC_GATED',
  OPERATION_MODE_BLOCKED: 'ERR_OP_MODE_BLOCKED',
  PORT_DISPATCH_LOCKED: 'ERR_PORT_LOCKED',
  INVALID_PORT_MODE_CHANGE: 'ERR_PORT_MODE_CHANGE',
  TEST_NOT_PASSED: 'ERR_TEST_NOT_PASSED',
  PORT_MODE_NOT_READY: 'ERR_PORT_MODE_NOT_READY',
} as const;

// ============================================
// 단위 / 시험
// ============================================
export const UomType = {
  EA: { code: 'EA', label: '낱개' },
  BOX: { code: 'BOX', label: '박스' },
  PLT: { code: 'PLT', label: '팔레트' },
} as const;

export type UomTypeCode = (typeof UomType)[keyof typeof UomType]['code'];

export const TestStatus = {
  REQUESTED: { code: 'REQUESTED', label: '시험 의뢰됨' },
  PASSED: { code: 'PASSED', label: '시험 통과' },
  FAILED: { code: 'FAILED', label: '시험 실패' },
} as const;

export type TestStatusCode = (typeof TestStatus)[keyof typeof TestStatus]['code'];

// ============================================
// 재고 운영 상태 (operato.logis.inventory.consts.StockStatus)
// NOTE: EquipmentDetailPopup 가 StockStatus.X (코드값) 와 StockStatusLabels 를
//       기존 분리 패턴으로 사용 중이다. 해당 컴포넌트 작업 완료 후 EnumEntry 패턴으로 통합 예정.
//       CellStockDetailGrid 도 동일 의존성 보유.
// ============================================
export const StockStatus = {
  IDLE: 0,
  INBOUND: 1,
  OUTBOUND: 2,
  RELOCATION: 3,
  INBOUND_READY: 4,
  HOST_PENDING: 5,
  HOLD: 7,
} as const;

export const StockStatusLabels: Record<number, string> = {
  0: '사용 가능',
  1: '입고 중',
  2: '출고 중',
  3: '정렬 중',
  4: '입고 대기',
  5: 'HOST 예약',
  7: '입출고 불가',
};

/** WCS 재고 상태 별칭 (백엔드 WcsStockStatus 와 일치) */
export const WcsStockStatus = StockStatus;
export const WcsStockStatusLabels = StockStatusLabels;

/** 인라인 상태별 색상 클래스 — Vue 측 매핑 */
export const StockStatusClassMap: Record<number, string> = {
  [StockStatus.IDLE]: 'stock-status-normal',
  [StockStatus.HOST_PENDING]: 'stock-status-reserved',
  [StockStatus.INBOUND]: 'stock-status-hold',
  [StockStatus.OUTBOUND]: 'stock-status-hold',
  [StockStatus.RELOCATION]: 'stock-status-hold',
  [StockStatus.INBOUND_READY]: 'stock-status-hold',
  [StockStatus.HOLD]: 'stock-status-hold',
};

/** HOST_PENDING 도 사용 중 상태로 간주 (다른 액션 차단) */
export function isStockBusy(status: number | null | undefined): boolean {
  if (status == null) return false;
  return status !== StockStatus.IDLE;
}

// ============================================
// 재고 카테고리 (operato.logis.wcs.tspg_4way_shuttle.consts.WcsDomainEnums.StockType)
// ============================================
export const StockType = {
  NORMAL: { code: 'NORMAL', label: '일반' },
  QC_PENDING: { code: 'QC_PENDING', label: '시험 대기' },
  QC_FAIL: { code: 'QC_FAIL', label: '시험 부적합' },
  NIA_PENDING: { code: 'NIA_PENDING', label: '국가 검열 대기' },
  RETURN: { code: 'RETURN', label: '반품' },
  DISPOSAL: { code: 'DISPOSAL', label: '폐기' },
} as const;

export type StockTypeCode = (typeof StockType)[keyof typeof StockType]['code'];

/** stock_type code → 한글 라벨 (StockStatusLabels 동일 패턴) */
export const StockTypeLabels: Record<string, string> = Object.fromEntries(
  Object.values(StockType).map((t) => [t.code, t.label]),
);

// ============================================
// 랙 셀 상태 코드 (시각화용)
// ============================================
export const RackStateCode = {
  EMPTY: {
    code: 'EMPTY',
    label: '비어있음',
    shortLabel: '빈셀',
    color: '#FFFFFF',
    tintAlpha: 0,
  },
  NONE: {
    code: 'NONE',
    label: '셀없음',
    shortLabel: '셀없음',
    color: '#999999',
    tintAlpha: 0,
  },
  PRODUCT: {
    code: 'PRODUCT',
    label: '제품',
    shortLabel: '제품',
    color: '#0066FF',
    tintAlpha: 0.3,
  },
  EMPTY_BOX: {
    code: 'EMPTY_BOX',
    label: '공박스',
    shortLabel: '공BOX',
    color: '#55BBFF',
    tintAlpha: 0.3,
  },
  INBOUND: {
    code: 'INBOUND',
    label: '입고 중',
    shortLabel: '입고',
    color: '#22BB22',
    tintAlpha: 0.35,
  },
  INBOUND_READY: {
    code: 'INBOUND_READY',
    label: '입고 대기',
    shortLabel: '입고대기',
    color: '#26C6A8',
    tintAlpha: 0.32,
  },
  QC_PENDING: {
    code: 'QC_PENDING',
    label: '시험 대기',
    shortLabel: '시험대기',
    color: '#FFC107',
    tintAlpha: 0.36,
  },
  QC_FAIL: {
    code: 'QC_FAIL',
    label: '시험 부적합',
    shortLabel: '시험불합',
    color: '#FF6B35',
    tintAlpha: 0.4,
  },
  OUTBOUND: {
    code: 'OUTBOUND',
    label: '출고 중',
    shortLabel: '출고',
    color: '#FFE000',
    tintAlpha: 0.38,
  },
  DOUBLE_IN: {
    code: 'DOUBLE_IN',
    label: '이중입고',
    shortLabel: '이중입고',
    color: '#FF69B4',
    tintAlpha: 0.36,
  },
  EMPTY_OUT: {
    code: 'EMPTY_OUT',
    label: '공출고',
    shortLabel: '공출고',
    color: '#FF2222',
    tintAlpha: 0.4,
  },
  CONFIRM: {
    code: 'CONFIRM',
    label: '확정',
    shortLabel: '적재확인',
    color: '#9933CC',
    tintAlpha: 0.34,
  },
  HOST_PENDING: {
    code: 'HOST_PENDING',
    label: 'HOST 예약',
    shortLabel: 'HOST예약',
    color: '#3B82F6',
    tintAlpha: 0.34,
  },
} as const;

export type RackStateCodeValue = (typeof RackStateCode)[keyof typeof RackStateCode]['code'];

/** "재고가 실제 적재된" 상태 — 카고 아이콘 표시 여부 SSOT */
export const RACK_STATE_WITH_INVENTORY: ReadonlyArray<RackStateCodeValue> = [
  RackStateCode.PRODUCT.code,
  RackStateCode.EMPTY_BOX.code,
  RackStateCode.OUTBOUND.code,
  RackStateCode.HOST_PENDING.code,
];

export function hasInventoryByStateCode(code: string | null | undefined): boolean {
  if (!code) return false;
  return (RACK_STATE_WITH_INVENTORY as readonly string[]).includes(code);
}

// ============================================
// 미리 생성된 옵션 목록 (통합 enum 은 enumToSelectOptions, 분리 패턴 enum 은 toSelectOptions 사용)
// ============================================
export const WcsOrderStatusOptions = enumToSelectOptions(WcsOrderStatus);
export const HostOrderStatusOptions = enumToSelectOptions(HostOrderStatus);
export const WcsLocStatusOptions = enumToSelectOptions(WcsLocStatus);
export const StockStatusOptions = toSelectOptions(StockStatus, StockStatusLabels);
