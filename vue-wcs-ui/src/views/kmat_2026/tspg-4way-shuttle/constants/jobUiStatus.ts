/**
 * jobUiStatus.ts
 * 프론트 표시용 작업 상태 — 코드/한글 라벨/utility 클래스/badge variant 한 항목에 통합.
 * 백엔드 JobStatusDto.status 와 1:1 매칭되는 추상화 단계.
 *
 * 백엔드 ShuttleOrderStatus (WAITING/ARRIVED/ABORTED/ERROR_* 등 세부 상태) 와의 변환은
 * `statusMapper.ts` 의 `wcsOrderStatusToJobStatus` / `rtJobStatusToJobStatus` 가 담당.
 */

export const JobUiStatus = {
  CREATED:        { code: 'CREATED',        label: '생성됨',     className: 'tspg-job-created',        badgeVariant: 'waiting' },
  PENDING:        { code: 'PENDING',        label: '대기',       className: 'tspg-job-pending',        badgeVariant: 'waiting' },
  ASSIGNED:       { code: 'ASSIGNED',       label: '할당됨',     className: 'tspg-job-assigned',       badgeVariant: 'assigned' },
  RUNNING:        { code: 'RUNNING',        label: '진행중',     className: 'tspg-job-running',        badgeVariant: 'in-progress' },
  AWAITING_SCAN:  { code: 'AWAITING_SCAN',  label: '스캔 대기',  className: 'tspg-job-awaiting-scan',  badgeVariant: 'waiting' },
  PAUSED:         { code: 'PAUSED',         label: '일시정지',   className: 'tspg-job-paused',         badgeVariant: 'paused' },
  COMPLETED:      { code: 'COMPLETED',      label: '완료',       className: 'tspg-job-completed',      badgeVariant: 'completed' },
  CANCELLED:      { code: 'CANCELLED',      label: '취소됨',     className: 'tspg-job-cancelled',      badgeVariant: 'cancelled' },
  FAILED:         { code: 'FAILED',         label: '실패',       className: 'tspg-job-failed',         badgeVariant: 'error' },
  UNKNOWN:        { code: 'UNKNOWN',        label: '알 수 없음', className: '',                        badgeVariant: '' },
} as const;

export type JobUiStatusCode = (typeof JobUiStatus)[keyof typeof JobUiStatus]['code'];

/** 진행 중으로 간주할 작업 상태 코드 목록 (AWAITING_SCAN 포함 — 작업자 액션 대기) */
export const ACTIVE_JOB_STATUSES: readonly JobUiStatusCode[] = [
  JobUiStatus.ASSIGNED.code,
  JobUiStatus.RUNNING.code,
  JobUiStatus.AWAITING_SCAN.code,
  JobUiStatus.PAUSED.code,
] as const;

/** 종료된 작업 상태 코드 목록 */
export const FINISHED_JOB_STATUSES: readonly JobUiStatusCode[] = [
  JobUiStatus.COMPLETED.code,
  JobUiStatus.CANCELLED.code,
  JobUiStatus.FAILED.code,
] as const;
