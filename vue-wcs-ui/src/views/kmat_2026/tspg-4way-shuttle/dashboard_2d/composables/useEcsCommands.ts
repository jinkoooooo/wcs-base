/**
 * useEcsCommands.ts
 * ECS(Equipment Control System) 제어 명령 컴포저블
 *
 * ============================================
 * 기능 설명
 * ============================================
 * - ECS 서버와의 연결 상태 확인
 * - 셔틀 제어 명령 (정지, 재시작, 이동)
 * - 작업(Job) 제어 명령 (취소, 재개)
 * - 모든 명령은 토스트 메시지로 결과 피드백
 *
 * ============================================
 * ECS 명령 종류
 * ============================================
 * 1. 셔틀 제어
 *    - stopShuttle: 셔틀 긴급 정지
 *    - restartShuttle: 셔틀 재시작
 *    - moveShuttle: 셔틀 특정 위치로 이동
 *
 * 2. 작업 제어
 *    - cancelJob: 진행 중인 작업 취소
 *    - resumeJob: 일시정지된 작업 재개
 *
 * ============================================
 * 사용 방법
 * ============================================
 * ```ts
 * const { showToast } = useToast();
 * const {
 *   ecsReachable,
 *   checkEcsConnection,
 *   handleStopShuttle,
 *   handleCancelJob
 * } = useEcsCommands(lcId, showToast);
 *
 * // 셔틀 정지
 * await handleStopShuttle('SHUTTLE-001', () => closePopup());
 *
 * // 작업 취소 (확인 다이얼로그 포함)
 * await handleCancelJob('JOB-123');
 * ```
 */

import { ref, Ref, computed } from 'vue';
import { ecsApi } from '../api/ecsApi';
import type { ToastType } from './useToast';

/** 토스트 표시 함수 타입 */
type ShowToastFn = (message: string, type?: ToastType, duration?: number) => void;

/**
 * ECS 제어 명령 컴포저블
 *
 * @param lcIdRef - 현재 센터 ID (반응형 또는 계산된 속성)
 * @param showToast - 토스트 메시지 표시 함수
 * @returns ECS 연결 상태와 제어 함수들
 */
export function useEcsCommands(lcIdRef: Ref<string> | { value: string }, showToast: ShowToastFn) {
  // ============================================
  // 상태 (State)
  // ============================================

  /** ECS 서버 연결 가능 여부 */
  const ecsReachable = ref(true);

  // ============================================
  // 계산된 속성 (Computed)
  // ============================================

  /** 현재 센터 ID 값 */
  const lcId = computed(() => lcIdRef.value);

  // ============================================
  // ECS 연결 확인 함수
  // ============================================

  /**
   * ECS 서버 연결 상태를 확인합니다.
   *
   * - 30초마다 주기적으로 호출하여 연결 상태 모니터링
   * - 연결 실패 시 화면에 연결 안됨 표시
   */
  async function checkEcsConnection(): Promise<void> {
    try {
      const health = await ecsApi.checkEcsHealth();
      ecsReachable.value = health.ecsReachable;
    } catch {
      ecsReachable.value = false;
    }
  }

  // ============================================
  // 셔틀 제어 함수
  // ============================================

  /**
   * 셔틀을 긴급 정지시킵니다.
   *
   * 사용 상황:
   * - 안전 문제 발생 시
   * - 충돌 위험 감지 시
   * - 수동 개입이 필요한 경우
   *
   * @param shuttleCode - 정지할 셔틀 코드 (예: 'SHUTTLE-001')
   * @param onSuccess - 성공 시 실행할 콜백 (예: 팝업 닫기)
   *
   * @example
   * await handleStopShuttle('SHUTTLE-001', () => {
   *   closeShuttlePopup();
   * });
   */
  async function handleStopShuttle(shuttleCode: string, onSuccess?: () => void): Promise<void> {
    try {
      const response = await ecsApi.stopShuttle(lcId.value, shuttleCode, '운영자 정지');

      if (response.accepted) {
        showToast('셔틀 정지 요청이 접수되었습니다.', 'success');
        onSuccess?.();
      } else {
        showToast(`셔틀 정지 실패: ${response.message}`, 'error');
      }
    } catch (error: any) {
      showToast(`셔틀 정지 오류: ${error.message}`, 'error');
    }
  }

  /**
   * 셔틀을 재시작합니다.
   *
   * 사용 상황:
   * - 정지 상태의 셔틀을 다시 가동할 때
   * - 에러 복구 후 재시작할 때
   *
   * @param shuttleCode - 재시작할 셔틀 코드
   * @param onSuccess - 성공 시 실행할 콜백
   *
   * @example
   * await handleRestartShuttle('SHUTTLE-001');
   */
  async function handleRestartShuttle(shuttleCode: string, onSuccess?: () => void): Promise<void> {
    try {
      const response = await ecsApi.restartShuttle(lcId.value, shuttleCode);

      if (response.accepted) {
        showToast('셔틀 재시작 요청이 접수되었습니다.', 'success');
        onSuccess?.();
      } else {
        showToast(`셔틀 재시작 실패: ${response.message}`, 'error');
      }
    } catch (error: any) {
      showToast(`셔틀 재시작 오류: ${error.message}`, 'error');
    }
  }

  /**
   * 셔틀을 특정 위치로 이동시킵니다.
   *
   * 사용 상황:
   * - 수동으로 셔틀 위치 조정이 필요할 때
   * - 충전 스테이션으로 이동시킬 때
   * - 테스트/디버깅 목적
   *
   * @param shuttleCode - 이동할 셔틀 코드
   * @param targetPointCode - 목표 위치 코드 (예: 'POINT-A1-01')
   * @param onSuccess - 성공 시 실행할 콜백
   *
   * @example
   * await handleMoveShuttle('SHUTTLE-001', 'CHARGING-01', () => {
   *   closeShuttlePopup();
   * });
   */
  async function handleMoveShuttle(
    shuttleCode: string,
    targetPointCode: string,
    onSuccess?: () => void,
  ): Promise<void> {
    try {
      const response = await ecsApi.moveShuttle(lcId.value, shuttleCode, targetPointCode);

      if (response.accepted) {
        showToast(`셔틀 이동 요청이 접수되었습니다. 목표: ${targetPointCode}`, 'success');
        onSuccess?.();
      } else {
        showToast(`셔틀 이동 실패: ${response.message}`, 'error');
      }
    } catch (error: any) {
      showToast(`셔틀 이동 오류: ${error.message}`, 'error');
    }
  }

  // ============================================
  // 작업 제어 함수
  // ============================================

  /**
   * 진행 중인 작업을 취소합니다.
   *
   * 주의사항:
   * - 취소 전 확인 다이얼로그 표시
   * - 이미 완료된 작업은 취소 불가
   * - 취소된 작업은 복구 불가
   *
   * @param jobKey - 취소할 작업 키 (고유 식별자)
   *
   * @example
   * await handleCancelJob('JOB-2024-001');
   */
  async function handleCancelJob(jobKey: string): Promise<void> {
    // 사용자 확인
    if (!confirm(`작업 ${jobKey}을(를) 취소하시겠습니까?`)) {
      return;
    }

    try {
      const response = await ecsApi.cancelJob(lcId.value, jobKey, '운영자 취소');

      if (response.accepted) {
        showToast(`작업 취소 요청이 접수되었습니다. (${response.commandId})`, 'success');
      } else {
        showToast(`작업 취소 실패: ${response.message}`, 'error');
      }
    } catch (error: any) {
      showToast(`작업 취소 오류: ${error.message}`, 'error');
    }
  }

  /**
   * 일시정지된 작업을 재개합니다.
   *
   * 사용 상황:
   * - 에러로 일시정지된 작업 재시작
   * - 수동 개입 후 작업 계속 진행
   *
   * @param jobKey - 재개할 작업 키
   *
   * @example
   * await handleResumeJob('JOB-2024-001');
   */
  async function handleResumeJob(jobKey: string): Promise<void> {
    try {
      const response = await ecsApi.resumeJob(lcId.value, jobKey);

      if (response.accepted) {
        showToast(`작업 재개 요청이 접수되었습니다. (${response.commandId})`, 'success');
      } else {
        showToast(`작업 재개 실패: ${response.message}`, 'error');
      }
    } catch (error: any) {
      showToast(`작업 재개 오류: ${error.message}`, 'error');
    }
  }

  // ============================================
  // 반환 (Return)
  // ============================================

  return {
    /** ECS 서버 연결 가능 여부 */
    ecsReachable,

    /** ECS 연결 상태 확인 */
    checkEcsConnection,

    /** 셔틀 정지 */
    handleStopShuttle,

    /** 셔틀 재시작 */
    handleRestartShuttle,

    /** 셔틀 이동 */
    handleMoveShuttle,

    /** 작업 취소 */
    handleCancelJob,

    /** 작업 재개 */
    handleResumeJob,
  };
}
