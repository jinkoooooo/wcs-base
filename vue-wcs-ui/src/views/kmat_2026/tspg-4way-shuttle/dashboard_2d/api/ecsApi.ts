/**
 * ECS 프록시 API
 *
 * LMS 2D → ECS 방향의 제어 명령 API
 * 실제 명령 실행은 ECS가 수행하며, LMS 2D는 트리거 역할만 함.
 *
 * 명령 실행 원칙:
 * - 즉시 ACK(accepted/commandId/message)를 받음
 * - UI에서 성공/실패를 토스트로 안내
 * - 실제 상태 변화 반영은 ECS가 발행하는 이벤트로 자동 갱신
 */

import axios from 'axios';

const BASE_URL = '/rest/shuttle/ecs-proxy';

// 응답 타입
export interface EcsCommandResponse {
  accepted: boolean;
  commandId?: string;
  message: string;
  jobKey?: string;
  shuttleCode?: string;
  alarmId?: string;
  ecsResponse?: Record<string, unknown>;
  ecsStatus?: number;
}

export interface EcsHealthResponse {
  ecsReachable: boolean;
  ecsStatus?: number;
  ecsUrl: string;
  error?: string;
  timestamp: number;
}

// ============================================
// 작업 제어 API
// ============================================

/**
 * 작업 취소 요청
 */
export async function cancelJob(
  lcId: string,
  jobKey: string,
  reason?: string,
): Promise<EcsCommandResponse> {
  const response = await axios.post<EcsCommandResponse>(
    `${BASE_URL}/${lcId}/jobs/${jobKey}/cancel`,
    { reason },
  );
  return response.data;
}

/**
 * 작업 재개 요청
 */
export async function resumeJob(lcId: string, jobKey: string): Promise<EcsCommandResponse> {
  const response = await axios.post<EcsCommandResponse>(
    `${BASE_URL}/${lcId}/jobs/${jobKey}/resume`,
    {},
  );
  return response.data;
}

/**
 * 작업 우선순위 변경 요청
 */
export async function updateJobPriority(
  lcId: string,
  jobKey: string,
  priority: number,
): Promise<EcsCommandResponse> {
  const response = await axios.put<EcsCommandResponse>(
    `${BASE_URL}/${lcId}/jobs/${jobKey}/priority`,
    { priority },
  );
  return response.data;
}

// ============================================
// 셔틀 제어 API
// ============================================

/**
 * 셔틀 정지 요청
 */
export async function stopShuttle(
  lcId: string,
  shuttleCode: string,
  reason?: string,
): Promise<EcsCommandResponse> {
  const response = await axios.post<EcsCommandResponse>(
    `${BASE_URL}/${lcId}/shuttles/${shuttleCode}/stop`,
    { reason },
  );
  return response.data;
}

/**
 * 셔틀 재시작 요청
 */
export async function restartShuttle(
  lcId: string,
  shuttleCode: string,
): Promise<EcsCommandResponse> {
  const response = await axios.post<EcsCommandResponse>(
    `${BASE_URL}/${lcId}/shuttles/${shuttleCode}/restart`,
    {},
  );
  return response.data;
}

/**
 * 셔틀 수동 이동 요청
 */
export async function moveShuttle(
  lcId: string,
  shuttleCode: string,
  targetPointCode: string,
): Promise<EcsCommandResponse> {
  const response = await axios.post<EcsCommandResponse>(
    `${BASE_URL}/${lcId}/shuttles/${shuttleCode}/move`,
    { targetPointCode },
  );
  return response.data;
}

// ============================================
// 알람 제어 API
// ============================================

/**
 * 알람 확인(ACK) 요청
 */
export async function acknowledgeAlarm(lcId: string, alarmId: string): Promise<EcsCommandResponse> {
  const response = await axios.post<EcsCommandResponse>(
    `${BASE_URL}/${lcId}/alarms/${alarmId}/ack`,
    {},
  );
  return response.data;
}

/**
 * 알람 리셋 요청
 */
export async function resetAlarm(lcId: string, alarmId: string): Promise<EcsCommandResponse> {
  const response = await axios.post<EcsCommandResponse>(
    `${BASE_URL}/${lcId}/alarms/${alarmId}/reset`,
    {},
  );
  return response.data;
}

// ============================================
// ECS 상태 확인
// ============================================

/**
 * ECS 연결 상태 확인
 */
export async function checkEcsHealth(): Promise<EcsHealthResponse> {
  const response = await axios.get<EcsHealthResponse>(`${BASE_URL}/health`);
  return response.data;
}

// ============================================
// 통합 API 객체
// ============================================

export const ecsApi = {
  // 작업 제어
  cancelJob,
  resumeJob,
  updateJobPriority,

  // 셔틀 제어
  stopShuttle,
  restartShuttle,
  moveShuttle,

  // 알람 제어
  acknowledgeAlarm,
  resetAlarm,

  // 상태 확인
  checkEcsHealth,
};

export default ecsApi;
