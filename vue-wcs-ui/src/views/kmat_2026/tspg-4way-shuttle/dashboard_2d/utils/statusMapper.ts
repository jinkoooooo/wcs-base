/**
 * 상태 변환 유틸리티
 *
 * 기준
 * - WCS: tb_wcs_shuttle_order (0,10,20,30,90,98,99)
 * - ECS: tb_ecs_rack_order / tb_ecs_route_order (0,1,2,9,90,99)
 * - UI : PENDING / ASSIGNED / RUNNING / PAUSED / COMPLETED / CANCELLED / FAILED / UNKNOWN
 */

import type { RtJobStatus } from '../api/types';
import { JobUiStatus } from '../api/types';
import { enumLabel, enumBadgeVariant } from '../../constants';

export type UiJobStatus =
  | 'PENDING'
  | 'ASSIGNED'
  | 'RUNNING'
  | 'PAUSED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'FAILED'
  | 'UNKNOWN';

// ============================================
// 원본 상태 코드
// ============================================

/** tb_wcs_shuttle_order - ShuttleOrderStatusEnumCode */
export const WCS_ORDER_STATUS = {
  CREATED: 0,
  SENT: 10,
  ACCEPTED: 20,
  RUNNING: 30,
  COMPLETED: 90,
  CANCELLED: 98,
  ERROR: 99,
} as const;

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
// Job 상태(문자열) -> WCS Order 상태(숫자)
// ============================================

/**
 * 기존 함수명 유지
 * 주의: 이 함수는 WCS order 기준으로 숫자를 되돌린다.
 */
export function jobStatusToOrderStatus(jobStatus: string | number | undefined): number {
  if (typeof jobStatus === 'number') return jobStatus;

  switch (jobStatus) {
    case JobUiStatus.PENDING.code:
      return WCS_ORDER_STATUS.CREATED;
    case JobUiStatus.ASSIGNED.code:
      return WCS_ORDER_STATUS.SENT;
    case JobUiStatus.RUNNING.code:
      return WCS_ORDER_STATUS.RUNNING;
    case JobUiStatus.PAUSED.code:
      return WCS_ORDER_STATUS.RUNNING;
    case JobUiStatus.COMPLETED.code:
      return WCS_ORDER_STATUS.COMPLETED;
    case JobUiStatus.CANCELLED.code:
      return WCS_ORDER_STATUS.CANCELLED;
    case JobUiStatus.FAILED.code:
      return WCS_ORDER_STATUS.ERROR;
    default:
      return WCS_ORDER_STATUS.CREATED;
  }
}

// ============================================
// WCS / ECS -> UI 상태 변환
// ============================================

/** tb_wcs_shuttle_order 전용 */
export function wcsOrderStatusToJobStatus(orderStatus: number | undefined): UiJobStatus {
  switch (Number(orderStatus)) {
    case WCS_ORDER_STATUS.CREATED:
      return JobUiStatus.PENDING.code;
    case WCS_ORDER_STATUS.SENT:
    case WCS_ORDER_STATUS.ACCEPTED:
      return JobUiStatus.ASSIGNED.code;
    case WCS_ORDER_STATUS.RUNNING:
      return JobUiStatus.RUNNING.code;
    case WCS_ORDER_STATUS.COMPLETED:
      return JobUiStatus.COMPLETED.code;
    case WCS_ORDER_STATUS.CANCELLED:
      return JobUiStatus.CANCELLED.code;
    case WCS_ORDER_STATUS.ERROR:
      return JobUiStatus.FAILED.code;
    default:
      return JobUiStatus.UNKNOWN.code;
  }
}

/** tb_ecs_rack_order / tb_ecs_route_order 전용 */
export function ecsOrderStatusToJobStatus(
  orderStatus: number | undefined,
  cmdStatus?: number,
  errorCode?: string,
  errorMessage?: string,
): UiJobStatus {
  // 에러 정보가 있으면 우선 실패 처리
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

/**
 * 기존 함수명 유지
 * - fallback WCS 목록 변환용으로 사용
 */
export function orderStatusToJobStatus(orderStatus: number | undefined): UiJobStatus {
  return wcsOrderStatusToJobStatus(orderStatus);
}

/**
 * 실시간 Job DTO -> TaskGrid용 UI 상태
 */
export function rtJobStatusToJobStatus(
  job: Pick<RtJobStatus, 'jobLevel' | 'status' | 'cmdStatus' | 'errorCode' | 'errorMessage'>,
): UiJobStatus {
  const level = String(job.jobLevel ?? '').toUpperCase();

  if (level === 'WCS') {
    return wcsOrderStatusToJobStatus(Number(job.status));
  }

  if (level === 'ECS_RACK' || level === 'ECS_ROUTE') {
    return ecsOrderStatusToJobStatus(
      Number(job.status),
      job.cmdStatus,
      job.errorCode,
      job.errorMessage,
    );
  }

  // jobLevel이 비어 있어도 ECS 기본 규칙으로 처리
  return ecsOrderStatusToJobStatus(
    Number(job.status),
    job.cmdStatus,
    job.errorCode,
    job.errorMessage,
  );
}

// ============================================
// 상태 라벨/클래스 조회 — 모두 JobUiStatus 디스크립터에서 lookup
// ============================================

export function getOrderStatusLabel(status: number | undefined): string {
  return getJobStatusLabel(wcsOrderStatusToJobStatus(status));
}

export function getOrderStatusClass(status: number | undefined): string {
  return getJobStatusClass(wcsOrderStatusToJobStatus(status));
}

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
    typeof status === 'number' ? wcsOrderStatusToJobStatus(status) : (status as UiJobStatus);
  return (
    ui === JobUiStatus.ASSIGNED.code ||
    ui === JobUiStatus.RUNNING.code ||
    ui === JobUiStatus.PAUSED.code
  );
}

export function isActiveStatus(status: string | number | undefined): boolean {
  const ui =
    typeof status === 'number' ? wcsOrderStatusToJobStatus(status) : (status as UiJobStatus);

  return ui === 'PENDING' || ui === 'ASSIGNED' || ui === 'RUNNING' || ui === 'PAUSED';
}

export function isErrorStatus(status: string | number | undefined): boolean {
  const ui =
    typeof status === 'number' ? wcsOrderStatusToJobStatus(status) : (status as UiJobStatus);

  return ui === 'FAILED';
}

export function isCompletedStatus(status: string | number | undefined): boolean {
  const ui =
    typeof status === 'number' ? wcsOrderStatusToJobStatus(status) : (status as UiJobStatus);

  return ui === 'COMPLETED';
}
