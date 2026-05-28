/**
 * EcsDBConsts.ts
 * ECS 설비 관련 상수 정의
 *
 * 백엔드 EcsDBConsts.java와 동기화 유지 필요
 * @see logis-kmat_2026/.../domain/enums/EcsDBConsts.java
 */

// ============================================
// 설비 그룹 타입 (tb_eq_group_mst.type)
// ============================================
export const EqGroupType = {
  SHUTTLE_RACK_4WAY: 11,
} as const;

export const EqGroupTypeLabels: Record<number, string> = {
  [EqGroupType.SHUTTLE_RACK_4WAY]: '4way 셔틀 랙',
};

export type EqGroupTypeValue = (typeof EqGroupType)[keyof typeof EqGroupType];

// ============================================
// 설비 타입 (tb_eq_mst.type)
// ============================================
export const EqType = {
  RACK: 11,
  CONVEYOR: 21,
  SHUTTLE_CAR: 22,
} as const;

export const EqTypeLabels: Record<number, string> = {
  [EqType.RACK]: '보관설비',
  [EqType.CONVEYOR]: '이송설비',
  [EqType.SHUTTLE_CAR]: '셔틀 카',
};

export type EqTypeValue = (typeof EqType)[keyof typeof EqType];

// ============================================
// 랙 타입 (tb_eq_rack_mst.type) — 코드/라벨/배지 메타데이터 통합 정의
// ============================================
export const RackType = {
  CELL: { code: 11, label: '셀', badgeText: '', badgeVariant: '', icon: '📦' },
  INBOUND_PORT: {
    code: 21,
    label: '입고포트',
    badgeText: 'IN',
    badgeVariant: 'badge-inbound',
    icon: '⬇️',
  },
  OUTBOUND_PORT: {
    code: 22,
    label: '출고포트',
    badgeText: 'OUT',
    badgeVariant: 'badge-outbound',
    icon: '⬆️',
  },
  IN_OUTBOUND_PORT: {
    code: 23,
    label: '입출고포트',
    badgeText: 'I/O',
    badgeVariant: 'badge-inout',
    icon: '⇅',
  },
  CHARGE_PORT: {
    code: 31,
    label: '충전포트',
    badgeText: 'CHG',
    badgeVariant: 'badge-charge',
    icon: '⚡',
  },
  CHARGE_ENTER_PORT: {
    code: 32,
    label: '충전진입포트',
    badgeText: 'ENT',
    badgeVariant: 'badge-charge-enter',
    icon: '🔌',
  },
  BAN_CELL: { code: 41, label: '영구 금지 셀(기둥)', badgeText: '', badgeVariant: '', icon: '⏹' },
} as const;

export type RackTypeCode = (typeof RackType)[keyof typeof RackType]['code'];
/** @deprecated `RackTypeCode` 사용 — 외부 API DTO 호환용으로만 유지 */
export type RackTypeValue = RackTypeCode;

// ============================================
// 컨베이어 타입 (tb_eq_cv_mst.type)
// ============================================
export const ConveyorType = {
  GROUND: 1,
  INBOUND: 2,
  OUTBOUND: 3,
  IN_OUTBOUND: 4,
  LIFT: 11,
  RACK_IN: 12,
} as const;

export const ConveyorTypeLabels: Record<number, string> = {
  [ConveyorType.GROUND]: '지상컨베이어',
  [ConveyorType.INBOUND]: '입고대컨베이어',
  [ConveyorType.OUTBOUND]: '출고대컨베이어',
  [ConveyorType.IN_OUTBOUND]: '입출고대컨베이어',
  [ConveyorType.LIFT]: '리프트컨베이어',
  [ConveyorType.RACK_IN]: '랙단 컨베이어',
};

export type ConveyorTypeValue = (typeof ConveyorType)[keyof typeof ConveyorType];

// ============================================
// 랙 상태 (tb_eq_rack_mst.status)
// ============================================
export const EqRackStatus = {
  READY: 0,
  MOVE_RESERVE: 1,
  CARGO_RESERVE: 2,
  CHARGE_RESERVE: 3,
  CARGO: 5,
  CAR: 6,
} as const;

export const EqRackStatusLabels: Record<number, string> = {
  [EqRackStatus.READY]: '대기',
  [EqRackStatus.MOVE_RESERVE]: '주행 예약',
  [EqRackStatus.CARGO_RESERVE]: '화물 예약',
  [EqRackStatus.CHARGE_RESERVE]: '충전 예약',
  [EqRackStatus.CARGO]: '화물 적재',
  [EqRackStatus.CAR]: '카 위치',
};

// ============================================
// 컨베이어 상태 (tb_eq_cv_mst.status)
// ============================================
export const EqConveyorStatus = {
  READY: 0,
  MOVE_RESERVE: 1,
} as const;

export const EqConveyorStatusLabels: Record<number, string> = {
  [EqConveyorStatus.READY]: '대기',
  [EqConveyorStatus.MOVE_RESERVE]: '주행 예약',
};

// ============================================
// 셔틀 카 상태 (tb_eq_car_mst.status)
// ============================================
export const EqCarStatus = {
  READY: 0,
  RESERVE: 1,
  RUN: 2,
  EMR_STOP: 5,
  ERROR: 8,
  COMPLETE: 9,
} as const;

export type EqCarStatusValue = (typeof EqCarStatus)[keyof typeof EqCarStatus];

export const EqCarStatusLabels: Record<number, string> = {
  [EqCarStatus.READY]: '대기',
  [EqCarStatus.RESERVE]: '설비 예약',
  [EqCarStatus.RUN]: '작업중',
  [EqCarStatus.EMR_STOP]: '비상정지',
  [EqCarStatus.ERROR]: '에러',
  [EqCarStatus.COMPLETE]: '완료',
};

export const EqCarStatusOptions = toSelectOptions(EqCarStatus, EqCarStatusLabels);

export const EqCarBatteryStatus = {
  CAN_MOVE: 0,
  NEED_CHARGE: 1,
  CHARGING: 2,
  COMPLETE_CHARGE: 9,
} as const;

export const EqCarBatteryStatusLabels: Record<number, string> = {
  [EqCarBatteryStatus.CAN_MOVE]: '작업가능',
  [EqCarBatteryStatus.NEED_CHARGE]: '충전필요',
  [EqCarBatteryStatus.CHARGING]: '충전중',
  [EqCarBatteryStatus.COMPLETE_CHARGE]: '충전완료',
};

export type EqCarBatteryStatusValue = (typeof EqCarBatteryStatus)[keyof typeof EqCarBatteryStatus];

export const EqCarBatteryStatusOptions = toSelectOptions(
  EqCarBatteryStatus,
  EqCarBatteryStatusLabels,
);

// ============================================
// 셔틀 움직임 상태 (movementStatus — 프론트 string enum)
// 코드 / 한글 라벨 / CSS 클래스를 한 항목 안에 묶어 단일 출처로 정의.
// ============================================
export const ShuttleMovementStatus = {
  IDLE: { code: 'IDLE', label: '대기', className: 'tspg-status-idle' },
  MOVING: { code: 'MOVING', label: '이동중', className: 'tspg-status-moving' },
  LOADING: { code: 'LOADING', label: '적재중', className: 'tspg-status-working' },
  UNLOADING: { code: 'UNLOADING', label: '하역중', className: 'tspg-status-working' },
  CHARGING: { code: 'CHARGING', label: '충전중', className: 'tspg-status-charging' },
  ERROR: { code: 'ERROR', label: '에러', className: 'tspg-status-error' },
  STOPPED: { code: 'STOPPED', label: '정지됨', className: 'tspg-status-stopped' },
} as const;

export type ShuttleMovementStatusCode =
  (typeof ShuttleMovementStatus)[keyof typeof ShuttleMovementStatus]['code'];

// ============================================
// 셔틀 배터리 단계 (% → 단계) — 표시용 임계치 + CSS 클래스 묶음
// ============================================
export const ShuttleBatteryLevel = {
  CRITICAL: { code: 'CRITICAL', label: '위험', className: 'tspg-battery-critical' },
  LOW: { code: 'LOW', label: '낮음', className: 'tspg-battery-low' },
  MEDIUM: { code: 'MEDIUM', label: '보통', className: 'tspg-battery-medium' },
  HIGH: { code: 'HIGH', label: '충분', className: 'tspg-battery-high' },
} as const;

export type ShuttleBatteryLevelCode =
  (typeof ShuttleBatteryLevel)[keyof typeof ShuttleBatteryLevel]['code'];

/** 배터리 % → 단계 항목 변환 (단일 출처: 표시용 임계치) */
export function getShuttleBatteryLevel(
  percent: number | null | undefined,
): (typeof ShuttleBatteryLevel)[keyof typeof ShuttleBatteryLevel] | null {
  if (typeof percent !== 'number') return null;
  if (percent <= 10) return ShuttleBatteryLevel.CRITICAL;
  if (percent <= 20) return ShuttleBatteryLevel.LOW;
  if (percent <= 50) return ShuttleBatteryLevel.MEDIUM;
  return ShuttleBatteryLevel.HIGH;
}

// ============================================
// 유틸리티: 옵션 목록 생성
// ============================================
export interface SelectOption {
  value: number;
  label: string;
}

/**
 * 상수 객체를 select 옵션 배열로 변환
 */
export function toSelectOptions(
  constObj: Record<string, number>,
  labels: Record<number, string>,
): SelectOption[] {
  return Object.values(constObj).map((value) => ({
    value,
    label: `${labels[value]} (${value})`,
  }));
}

// 미리 생성된 옵션 목록
export const EqGroupTypeOptions = toSelectOptions(EqGroupType, EqGroupTypeLabels);
export const EqTypeOptions = toSelectOptions(EqType, EqTypeLabels);
export const ConveyorTypeOptions = toSelectOptions(ConveyorType, ConveyorTypeLabels);

/** 디스크립터 기반 enum → SelectOption[] 변환 (라벨/코드 모두 한 정의에서 가져옴) */
export const RackTypeOptions: SelectOption[] = Object.values(RackType).map((entry) => ({
  value: entry.code,
  label: `${entry.label} (${entry.code})`,
}));

// ============================================
// ECS 인터페이스 상태 (tb_ecs_if 등)
// ============================================
export const EcsIfStatus = {
  READY: 20,
  RECEIVE: 30,
  RACK_IN_MOVE_COMPLETE: 40,
  COMPLETE: 50,
} as const;

export const EcsIfStatusLabels: Record<number, string> = {
  [EcsIfStatus.READY]: '지시 수신 대상',
  [EcsIfStatus.RECEIVE]: '지시 수신 완료',
  [EcsIfStatus.RACK_IN_MOVE_COMPLETE]: '이송지시 랙단 이동 완료',
  [EcsIfStatus.COMPLETE]: '완료보고',
};

// ============================================
// ECS 오더 상태 (⚠️ WCS와 다름 — wcsConsts.WcsOrderStatus 참고)
// ============================================
export const EcsOrderStatus = {
  READY: 0,
  EQ_SEND: 1,
  WORKING: 2,
  COMPLETE: 9,
} as const;

export const EcsOrderStatusLabels: Record<number, string> = {
  [EcsOrderStatus.READY]: '대기',
  [EcsOrderStatus.EQ_SEND]: '설비 지시 전송',
  [EcsOrderStatus.WORKING]: '작업중',
  [EcsOrderStatus.COMPLETE]: '완료',
};

// ============================================
// ECS 오더 타입
// ============================================
export const EcsOrderType = {
  INBOUND: 11,
  OUTBOUND: 12,
  MOVE: 13,
  MOVE_HOME: 14,
  MOVE_CAR_FLOOR: 15,
  CHARGE: 21,
} as const;

export const EcsOrderTypeLabels: Record<number, string> = {
  [EcsOrderType.INBOUND]: '입고',
  [EcsOrderType.OUTBOUND]: '출고',
  [EcsOrderType.MOVE]: '재고이동',
  [EcsOrderType.MOVE_HOME]: '홈 이동',
  [EcsOrderType.MOVE_CAR_FLOOR]: '셔틀카 층간 이송',
  [EcsOrderType.CHARGE]: '충전',
};

// ============================================
// 오더 우선순위
// ============================================
export const OrderPriority = {
  MOVE_HOME: 3,
  MOVE_CAR_FLOOR: 4,
  NORMAL: 5,
} as const;

// ============================================
// 랙/라우트 오더 명령 상태
// ============================================
export const EcsRackOrderCmdStatus = {
  READY: 0,
  COMPLETE: 9,
  MOVE: 11,
  LOAD_MOVE: 12,
  UNLOAD_MOVE: 13,
  LOAD: 14,
  UNLOAD: 15,
  CHARGE_MOVE: 21,
  CHARGE: 22,
  MOVE_HOME: 31,
  MOVE_CAR_FROM_RACK_CV: 41,
  MOVE_CAR_LIFT_CV: 42,
  MOVE_CAR_LIFT_MOVE: 43,
  MOVE_CAR_TO_RACK_CV: 44,
} as const;

export const EcsRackOrderCmdStatusLabels: Record<number, string> = {
  [EcsRackOrderCmdStatus.READY]: '대기',
  [EcsRackOrderCmdStatus.COMPLETE]: '완료',
  [EcsRackOrderCmdStatus.MOVE]: '일반이송',
  [EcsRackOrderCmdStatus.LOAD_MOVE]: '로드이송',
  [EcsRackOrderCmdStatus.UNLOAD_MOVE]: '언로드이송',
  [EcsRackOrderCmdStatus.LOAD]: '로드',
  [EcsRackOrderCmdStatus.UNLOAD]: '언로드',
  [EcsRackOrderCmdStatus.CHARGE_MOVE]: '충전이송',
  [EcsRackOrderCmdStatus.CHARGE]: '충전',
  [EcsRackOrderCmdStatus.MOVE_HOME]: '홈 이동',
  [EcsRackOrderCmdStatus.MOVE_CAR_FROM_RACK_CV]: '셔틀카 출발지 랙단 이송',
  [EcsRackOrderCmdStatus.MOVE_CAR_LIFT_CV]: '셔틀카 목적지 랙단 이송',
  [EcsRackOrderCmdStatus.MOVE_CAR_LIFT_MOVE]: '셔틀카 목적지 랙단 이송',
  [EcsRackOrderCmdStatus.MOVE_CAR_TO_RACK_CV]: '셔틀카 목적지 랙단 이송',
};

export const EcsRouteOrderCmdStatus = {
  READY: 0,
  COMPLETE: 9,
  INBOUND_READY: 11,
  LIFT_MOVE: 12,
  RACK_CV_READY: 13,
} as const;

export const EcsRouteOrderCmdStatusLabels: Record<number, string> = {
  [EcsRouteOrderCmdStatus.READY]: '대기',
  [EcsRouteOrderCmdStatus.COMPLETE]: '완료',
  [EcsRouteOrderCmdStatus.INBOUND_READY]: '입고 대기',
  [EcsRouteOrderCmdStatus.LIFT_MOVE]: '리프트 이송',
  [EcsRouteOrderCmdStatus.RACK_CV_READY]: '랙단컨베이어 대기',
};

// ============================================
// PLC 설비 타입
// ============================================
export const PlcEqType = {
  SHUTTLE_CAR: 1,
  CONVEYOR_AND_LIFT: 2,
} as const;
