/**
 * wcsConsts.ts
 * WCS 도메인 상수 — 백엔드 WcsDomainEnums.java 미러
 */

import { toSelectOptions } from './EcsDBConsts';

// ============================================
// WCS 셔틀 오더 상태 (tb_wcs_shuttle_order.order_status)
// 0~89: 정상, 90~99: 종료, 100+: 에러
// ============================================
export const WcsOrderStatus = {
  CREATED: 0,
  SENT: 10,
  ACCEPTED: 20,
  WAITING: 25,
  RUNNING: 30,
  ARRIVED: 40,
  COMPLETED: 90,
  CANCELLED: 91,
  ABORTED: 95,
  ERROR_GENERAL: 100,
  ERROR_SEND_FAIL: 110,
  ERROR_TIMEOUT: 120,
  ERROR_HARDWARE: 130,
  ERROR_LOCATION: 140,
  ERROR_INVENTORY: 150,
  ERROR_SYSTEM_RESTART: 190,
} as const;

export const WcsOrderStatusLabels: Record<number, string> = {
  0: '생성됨',
  10: '전송됨',
  20: '수락됨',
  25: '대기',
  30: '실행중',
  40: '도착',
  90: '완료',
  91: '취소',
  95: '중단',
  100: '일반 에러',
  110: '전송 실패',
  120: '응답 지연',
  130: '설비 에러',
  140: '로케이션 에러',
  150: '재고 에러',
  190: '시스템 재시작 에러',
};

/** 종료(90+) / 에러(100+) 임계값 */
export const WCS_ORDER_FINAL = 90;
export const WCS_ORDER_ERROR = 100;

// ============================================
// HOST 오더 상태
// ============================================
export const HostOrderStatus = {
  RECEIVED: 0,
  WAITING_SCHEDULE: 5,
  VALIDATED: 10,
  READY_FOR_ALLOC: 12,
  WAITING_EXEC: 30,
  EXECUTING: 40,
  PUTBACK_WAIT: 60,
  INBOUND_TEST_WAIT: 75,
  COMPLETED: 80,
  CANCELLED: 85,
  REJECTED: 88,
  TEST_FAILED: 90,
  ERROR: 100,
} as const;

export const HostOrderStatusLabels: Record<number, string> = {
  0: '수신됨',
  5: '예정일 대기',
  10: '검증완료',
  12: '산출 준비 완료',
  30: 'ECS 실행 대기',
  40: '설비 실행 중',
  60: '재입고 shuttle 미완 대기',
  75: '입고 후 시험 미종결',
  80: '완료',
  85: '취소',
  88: '거절',
  90: '시험 부적합 - 폐기 대기',
  100: '오류',
};

// ============================================
// WCS ECS 인터페이스 상태 (tb_wcs_shuttle_order.ecs_if_status)
// ⚠️ ECS 도메인의 EcsIfStatus와 다름
// ============================================
export const WcsEcsIfStatus = {
  READY: 0,
  SENDING: 10,
  SENT: 20,
  ACK: 30,
  FAIL: 99,
} as const;

export const WcsEcsIfStatusLabels: Record<number, string> = {
  0: '대기중',
  10: '전송중',
  20: '전송됨',
  30: '응답수신',
  99: '실패',
};

// ============================================
// ECS 콜백 상태 (문자열)
// ============================================
export const EcsCallbackStatus = {
  ACCEPTED: 'ACCEPTED',
  STARTED: 'STARTED',
  IN_PROGRESS: 'IN_PROGRESS',
  FROM_LOADING_COMPLETE: 'FROM_LOADING_COMPLETE',
  TO_UNLOADING_COMPLETE: 'TO_UNLOADING_COMPLETE',
  RACK_CONVEYOR_ARRIVED: 'RACK_CONVEYOR_ARRIVED',
  COMPLETE: 'COMPLETE',
  ERROR: 'ERROR',
  CANCELLED: 'CANCELLED',
} as const;

export const EcsCallbackStatusLabels: Record<string, string> = {
  ACCEPTED: '작업 수락',
  STARTED: '작업 시작',
  IN_PROGRESS: '작업 진행 중',
  FROM_LOADING_COMPLETE: '출발지 로딩 완료',
  TO_UNLOADING_COMPLETE: '목적지 언로딩 완료',
  RACK_CONVEYOR_ARRIVED: '랙단 컨베이어 도착',
  COMPLETE: '작업 완료',
  ERROR: '작업 오류',
  CANCELLED: '작업 취소',
};

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
// 운영 모드 / 룰 타입
// ============================================
export const WcsOperationMode = {
  NORMAL: 'NORMAL',
  INBOUND_PRIORITY: 'INBOUND_PRIORITY',
  OUTBOUND_PRIORITY: 'OUTBOUND_PRIORITY',
  RELOCATION: 'RELOCATION',
  MAINTENANCE: 'MAINTENANCE',
} as const;

export const WcsOperationModeLabels: Record<string, string> = {
  NORMAL: '정상',
  INBOUND_PRIORITY: '입고 우선',
  OUTBOUND_PRIORITY: '출고 우선',
  RELOCATION: '재배치',
  MAINTENANCE: '점검',
};

export const PortRuleType = {
  INTERLEAVING: 'INTERLEAVING',
  PARALLEL: 'PARALLEL',
} as const;

export const PortRuleTypeLabels: Record<string, string> = {
  INTERLEAVING: '교차 제어',
  PARALLEL: '동시 처리',
};

// ============================================
// WCS 오더 타입 (문자열, ⚠️ ECS의 EcsOrderType과 다름)
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
// 셔틀 오더 sub type (tb_wcs_shuttle_order.sub_order_type)
// 백엔드 SSOT: operato.logis.wcs.consts.SubOrderType
// ============================================
export const SubOrderType = {
  NORMAL: { code: 'NORMAL', label: '일반' },
  PARTIAL_OUT: { code: 'PARTIAL_OUT', label: '부분 출고' },
  SAMPLE_OUT: { code: 'SAMPLE_OUT', label: '시험 출고' },
  SAMPLE_DISCARD: { code: 'SAMPLE_DISCARD', label: '시험 폐기' },
  RETURN_IN: { code: 'RETURN_IN', label: '반품 입고' },
  RETURN_OUT: { code: 'RETURN_OUT', label: '반품 출고' },
  DISPOSAL_OUT: { code: 'DISPOSAL_OUT', label: '폐기 출고' },
} as const;

export type SubOrderTypeCode = (typeof SubOrderType)[keyof typeof SubOrderType]['code'];

// ============================================
// 박스 상태 (tb_wcs_pallet_box.box_status)
// 백엔드 SSOT: operato.logis.wcs.consts.BoxStatus
// DRAFT(10) → PENDING(20) → PRINTED(30) → SCANNED(40) → DEPLETED(90), * → VOID(99)
// ============================================
export const BoxStatus = {
  DRAFT: { code: 10, label: '미확정' },
  PENDING: { code: 20, label: '대기' },
  PRINTED: { code: 30, label: '인쇄됨' },
  SCANNED: { code: 40, label: '스캔됨' },
  DEPLETED: { code: 90, label: '소진됨' },
  VOID: { code: 99, label: '폐기' },
} as const;

export type BoxStatusCode = (typeof BoxStatus)[keyof typeof BoxStatus]['code'];

/** 박스가 "살아있는" 상태인지 — DEPLETED/VOID 외 모두. 시험 채취·잔량 계산 등에서 사용. */
export function isBoxAlive(code: number | null | undefined): boolean {
  return code !== BoxStatus.DEPLETED.code && code !== BoxStatus.VOID.code;
}

// ============================================
// WCS 로케이션 타입
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
  EMPTY: 0,
  OCCUPIED: 10,
  LOCKED: 20,
  EMPTY_PICK: 30,
  DOUBLE_ENTRY: 40,
  DISABLED: 90,
} as const;

export const WcsLocStatusLabels: Record<number, string> = {
  0: '비어있음',
  10: '재고있음',
  20: '작업중',
  30: '공출고 감지',
  40: '이중입고 감지',
  90: '사용불가',
};

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
// 단위 / 시험 / 재고 상태
// ============================================
export const UomType = {
  EA: 'EA',
  BOX: 'BOX',
  PLT: 'PLT',
} as const;

export const UomTypeLabels: Record<string, string> = {
  EA: '낱개',
  BOX: '박스',
  PLT: '팔레트',
};

export const TestStatus = {
  REQUESTED: 'REQUESTED',
  PASSED: 'PASSED',
  FAILED: 'FAILED',
} as const;

export const TestStatusLabels: Record<string, string> = {
  REQUESTED: '시험 의뢰됨',
  PASSED: '시험 통과',
  FAILED: '시험 실패',
};

/** 재고 운영 상태 (operato.logis.inventory.consts.StockStatus) */
export const StockStatus = {
  IDLE: 0,
  INBOUND: 1,
  OUTBOUND: 2,
  RELOCATION: 3,
  WAITING_INBOUND: 4,
  HOLD: 7,
} as const;

export const StockStatusLabels: Record<number, string> = {
  0: '사용 가능',
  1: '입고 중',
  2: '출고 중',
  3: '정렬 중',
  4: '입고 대기',
  7: '입출고 불가',
};

/** WCS 재고 상태 별칭 (백엔드 WcsStockStatus 와 일치) */
export const WcsStockStatus = StockStatus;
export const WcsStockStatusLabels = StockStatusLabels;

/** 재고 카테고리 (operato.logis.wcs.tspg_4way_shuttle.consts.WcsDomainEnums.StockType) */
export const StockType = {
  NORMAL: 'NORMAL',
  QC_PENDING: 'QC_PENDING',
  QC_FAIL: 'QC_FAIL',
  NIA_PENDING: 'NIA_PENDING',
  RETURN: 'RETURN',
  DISPOSAL: 'DISPOSAL',
} as const;

export const StockTypeLabels: Record<string, string> = {
  NORMAL: '일반',
  QC_PENDING: '시험 대기',
  QC_FAIL: '시험 부적합',
  NIA_PENDING: '국가 검열 대기',
  RETURN: '반품',
  DISPOSAL: '폐기',
};

// ============================================
// 미리 생성된 옵션 목록
// ============================================
export const WcsOrderStatusOptions = toSelectOptions(WcsOrderStatus, WcsOrderStatusLabels);
export const HostOrderStatusOptions = toSelectOptions(HostOrderStatus, HostOrderStatusLabels);
export const WcsLocStatusOptions = toSelectOptions(WcsLocStatus, WcsLocStatusLabels);
export const StockStatusOptions = toSelectOptions(StockStatus, StockStatusLabels);
