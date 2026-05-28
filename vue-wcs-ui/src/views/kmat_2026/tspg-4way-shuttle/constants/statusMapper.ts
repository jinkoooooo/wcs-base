/**
 * statusMapper.ts
 *
 * 백엔드 도메인 status (WCS ShuttleOrderStatus / ECS OrderStatus) 와
 * 프론트 추상 status (JobUiStatus) 간 변환 책임 단일 출처.
 *
 * 기준
 * - WCS: tb_wcs_shuttle_order.order_status — `WcsOrderStatus` (백엔드 ShuttleOrderStatus 미러)
 * - ECS: tb_ecs_rack_order / tb_ecs_route_order — 내부 `ECS_ORDER_STATUS` 상수
 * - UI : PENDING / ASSIGNED / RUNNING / PAUSED / COMPLETED / CANCELLED / FAILED / UNKNOWN
 *
 * 백엔드 status 는 문자열 enum 이름('WAITING') 또는 숫자 코드(25) 어느 쪽이든 들어올 수 있다.
 */

import { JobUiStatus, type JobUiStatusCode } from './jobUiStatus';
import { WcsOrderStatus } from './wcsConsts';
import { enumLabel, enumBadgeVariant, findEnumEntry } from './enumHelpers';

/** RtJobStatus 의 부분 의존성만 차용 (constants → api 역의존 회피) */
type JobStatusInput = {
  jobLevel?: string;
  status?: string | number;
  cmdStatus?: number;
  errorCode?: string;
  errorMessage?: string;
};

// ============================================
// 원본 상태 코드
// ============================================

/** tb_ecs_rack_order / tb_ecs_route_order - EcsDbConsts.OrderStatus */
export const ECS_ORDER_STATUS = {
  READY: 0,
  EQ_SEND: 1,
  WORKING: 2,
  COMPLETE: 9,
  CANCELLED: 90,
  ERROR: 99,
} as const;

// ============================================
// UI Job 상태(문자열) -> WCS Order 상태(숫자)
// ============================================

/**
 * 기존 함수명 유지.
 * 주의: 이 함수는 WCS order 기준으로 숫자를 되돌린다.
 */
export function jobStatusToOrderStatus(jobStatus: string | number | undefined): number {
  if (typeof jobStatus === 'number') return jobStatus;

  switch (jobStatus) {
    case JobUiStatus.CREATED.code:
      return WcsOrderStatus.CREATED.code;
    case JobUiStatus.PENDING.code:
      return WcsOrderStatus.WAITING.code;
    case JobUiStatus.ASSIGNED.code:
      return WcsOrderStatus.SENT.code;
    case JobUiStatus.RUNNING.code:
    case JobUiStatus.PAUSED.code:
      return WcsOrderStatus.RUNNING.code;
    case JobUiStatus.AWAITING_SCAN.code:
      return WcsOrderStatus.ARRIVED.code;
    case JobUiStatus.COMPLETED.code:
      return WcsOrderStatus.COMPLETED.code;
    case JobUiStatus.CANCELLED.code:
      return WcsOrderStatus.CANCELLED.code;
    case JobUiStatus.FAILED.code:
      return WcsOrderStatus.ERROR_GENERAL.code;
    default:
      return WcsOrderStatus.CREATED.code;
  }
}

// ============================================
// WCS / ECS -> UI 상태 변환
// ============================================

/** 숫자 코드(WcsOrderStatus.code) 기준 매핑 — 내부 사용 */
function wcsCodeToJobStatus(code: number): JobUiStatusCode {
  switch (code) {
    case WcsOrderStatus.CREATED.code:
      return JobUiStatus.CREATED.code;
    case WcsOrderStatus.SENT.code:
    case WcsOrderStatus.ACCEPTED.code:
      return JobUiStatus.ASSIGNED.code;
    case WcsOrderStatus.WAITING.code:
      return JobUiStatus.PENDING.code;
    case WcsOrderStatus.RUNNING.code:
      return JobUiStatus.RUNNING.code;
    case WcsOrderStatus.ARRIVED.code:
      return JobUiStatus.AWAITING_SCAN.code;
    case WcsOrderStatus.COMPLETED.code:
      return JobUiStatus.COMPLETED.code;
    case WcsOrderStatus.CANCELLED.code:
      return JobUiStatus.CANCELLED.code;
    case WcsOrderStatus.ABORTED.code:
      return JobUiStatus.FAILED.code;
    default:
      if (code >= WcsOrderStatus.ERROR_GENERAL.code) return JobUiStatus.FAILED.code;
      return JobUiStatus.UNKNOWN.code;
  }
}

/**
 * tb_wcs_shuttle_order 전용 매퍼.
 *
 * 백엔드는 status 필드를 문자열 enum 이름('WAITING') 또는 숫자 코드(25) 중 하나로 보낼 수 있다.
 * 이미 JobUiStatus code('PENDING' 등) 가 들어오는 경우도 그대로 통과시킨다.
 */
export function wcsOrderStatusToJobStatus(
  orderStatus: number | string | null | undefined,
): JobUiStatusCode {
  if (orderStatus == null || orderStatus === '') return JobUiStatus.UNKNOWN.code;

  if (typeof orderStatus === 'number') {
    return wcsCodeToJobStatus(orderStatus);
  }

  // 이미 JobUiStatus code 면 그대로 반환 (BE 가 추상 status 로 보낸 경우)
  if (findEnumEntry(JobUiStatus, orderStatus)) {
    return orderStatus as JobUiStatusCode;
  }

  // WcsOrderStatus enum 이름('WAITING' 등) 으로 들어온 경우 → code 조회 후 매핑
  const wcsEntry = (WcsOrderStatus as Record<string, { code: number }>)[orderStatus];
  if (wcsEntry) {
    return wcsCodeToJobStatus(wcsEntry.code);
  }

  // 숫자 문자열('25') 인 경우
  const n = Number(orderStatus);
  if (Number.isFinite(n)) {
    return wcsCodeToJobStatus(n);
  }

  return JobUiStatus.UNKNOWN.code;
}

/** tb_ecs_rack_order / tb_ecs_route_order 전용 */
export function ecsOrderStatusToJobStatus(
  orderStatus: number | undefined,
  cmdStatus?: number,
  errorCode?: string,
  errorMessage?: string,
): JobUiStatusCode {
  if (errorCode || errorMessage || Number(cmdStatus) === 99) {
    return JobUiStatus.FAILED.code;
  }

  switch (Number(orderStatus)) {
    case ECS_ORDER_STATUS.READY:
      return JobUiStatus.PENDING.code;
    case ECS_ORDER_STATUS.EQ_SEND:
      return JobUiStatus.ASSIGNED.code;
    case ECS_ORDER_STATUS.WORKING:
      return JobUiStatus.RUNNING.code;
    case ECS_ORDER_STATUS.COMPLETE:
      return JobUiStatus.COMPLETED.code;
    case ECS_ORDER_STATUS.CANCELLED:
      return JobUiStatus.CANCELLED.code;
    case ECS_ORDER_STATUS.ERROR:
      return JobUiStatus.FAILED.code;
    default:
      return JobUiStatus.UNKNOWN.code;
  }
}

/** 기존 함수명 유지 — fallback WCS 목록 변환용 */
export function orderStatusToJobStatus(
  orderStatus: number | string | undefined,
): JobUiStatusCode {
  return wcsOrderStatusToJobStatus(orderStatus);
}

/** 실시간 Job DTO -> UI Job 상태 */
export function rtJobStatusToJobStatus(job: JobStatusInput): JobUiStatusCode {
  const level = String(job.jobLevel ?? '').toUpperCase();

  if (level === 'WCS') {
    return wcsOrderStatusToJobStatus(job.status);
  }

  if (level === 'ECS_RACK' || level === 'ECS_ROUTE') {
    return ecsOrderStatusToJobStatus(
      Number(job.status),
      job.cmdStatus,
      job.errorCode,
      job.errorMessage,
    );
  }

  // jobLevel 비어 있어도 ECS 기본 규칙으로 처리
  return ecsOrderStatusToJobStatus(
    Number(job.status),
    job.cmdStatus,
    job.errorCode,
    job.errorMessage,
  );
}

// ============================================
// JobUiStatus 라벨/배지 variant 조회
// (WCS 원본 코드 기반 헬퍼는 `statusHelpers.ts` 의 getOrderStatusText / getOrderStatusClass 사용)
// ============================================

export function getJobStatusLabel(status: string | undefined): string {
  return enumLabel(JobUiStatus, status, JobUiStatus.UNKNOWN.label);
}

export function getJobStatusClass(status: string | undefined): string {
  return enumBadgeVariant(JobUiStatus, status);
}

// ============================================
// 상태 판별 헬퍼
// ============================================

export function isRunningOrPaused(status: string | number | undefined): boolean {
  const ui =
    typeof status === 'number' || (typeof status === 'string' && status !== '' && !findEnumEntry(JobUiStatus, status))
      ? wcsOrderStatusToJobStatus(status)
      : (status as JobUiStatusCode);
  return (
    ui === JobUiStatus.ASSIGNED.code ||
    ui === JobUiStatus.RUNNING.code ||
    ui === JobUiStatus.PAUSED.code
  );
}

export function isActiveStatus(status: string | number | undefined): boolean {
  const ui =
    typeof status === 'number' || (typeof status === 'string' && status !== '' && !findEnumEntry(JobUiStatus, status))
      ? wcsOrderStatusToJobStatus(status)
      : (status as JobUiStatusCode);
  return (
    ui === JobUiStatus.PENDING.code ||
    ui === JobUiStatus.ASSIGNED.code ||
    ui === JobUiStatus.RUNNING.code ||
    ui === JobUiStatus.PAUSED.code
  );
}

export function isErrorStatus(status: string | number | undefined): boolean {
  const ui =
    typeof status === 'number' || (typeof status === 'string' && status !== '' && !findEnumEntry(JobUiStatus, status))
      ? wcsOrderStatusToJobStatus(status)
      : (status as JobUiStatusCode);
  return ui === JobUiStatus.FAILED.code;
}

export function isCompletedStatus(status: string | number | undefined): boolean {
  const ui =
    typeof status === 'number' || (typeof status === 'string' && status !== '' && !findEnumEntry(JobUiStatus, status))
      ? wcsOrderStatusToJobStatus(status)
      : (status as JobUiStatusCode);
  return ui === JobUiStatus.COMPLETED.code;
}
