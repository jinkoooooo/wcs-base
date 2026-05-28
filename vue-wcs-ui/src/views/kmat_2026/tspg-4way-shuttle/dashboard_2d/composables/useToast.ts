/**
 * useToast.ts
 * 토스트 메시지 관리 컴포저블
 *
 * ============================================
 * 기능 설명
 * ============================================
 * - 화면 하단에 일시적으로 표시되는 알림 메시지(토스트) 관리
 * - 성공(success), 오류(error), 정보(info) 3가지 타입 지원
 * - 지정된 시간(기본 3초) 후 자동으로 사라짐
 * - 새 토스트가 표시되면 이전 토스트는 즉시 사라짐 (단일 토스트 정책)
 *
 * ============================================
 * 사용 방법
 * ============================================
 * ```ts
 * const { toastMessage, toastType, showToast } = useToast();
 *
 * // 성공 메시지 표시
 * showToast('작업이 완료되었습니다.', 'success');
 *
 * // 오류 메시지 표시 (5초간 표시)
 * showToast('오류가 발생했습니다.', 'error', 5000);
 *
 * // 정보 메시지 표시 (기본 3초)
 * showToast('데이터를 로딩 중입니다.', 'info');
 * ```
 *
 * ============================================
 * 반환 값
 * ============================================
 * - toastMessage: 현재 표시 중인 토스트 메시지 (null이면 표시 안 함)
 * - toastType: 현재 토스트 타입 ('success' | 'error' | 'info')
 * - showToast: 토스트를 표시하는 함수
 * - hideToast: 토스트를 즉시 숨기는 함수
 */

import { ref } from 'vue';

/** 토스트 메시지 타입 정의 */
export type ToastType = 'success' | 'error' | 'info';

/**
 * 토스트 메시지 관리 컴포저블
 *
 * @returns 토스트 상태와 제어 함수들
 */
export function useToast() {
  // ============================================
  // 상태 (State)
  // ============================================

  /** 현재 표시 중인 토스트 메시지 (null이면 토스트 숨김) */
  const toastMessage = ref<string | null>(null);

  /** 현재 토스트 타입 (성공/오류/정보) */
  const toastType = ref<ToastType>('info');

  /** 자동 숨김 타이머 ID (중복 방지용) */
  let toastTimeout: ReturnType<typeof setTimeout> | null = null;

  // ============================================
  // 함수 (Functions)
  // ============================================

  /**
   * 토스트 메시지를 화면에 표시합니다.
   *
   * @param message - 표시할 메시지 텍스트
   * @param type - 토스트 타입 (기본값: 'info')
   *   - 'success': 녹색 배경, 성공 알림에 사용
   *   - 'error': 빨간색 배경, 오류 알림에 사용
   *   - 'info': 파란색 배경, 일반 정보 알림에 사용
   * @param duration - 표시 지속 시간 (밀리초, 기본값: 3000ms = 3초)
   *
   * @example
   * // 기본 정보 메시지 (3초간 표시)
   * showToast('데이터가 저장되었습니다.');
   *
   * // 성공 메시지
   * showToast('작업 완료!', 'success');
   *
   * // 오류 메시지 (5초간 표시)
   * showToast('네트워크 오류가 발생했습니다.', 'error', 5000);
   */
  function showToast(message: string, type: ToastType = 'info', duration = 3000): void {
    // 기존 타이머가 있으면 제거 (이전 토스트 취소)
    if (toastTimeout) {
      clearTimeout(toastTimeout);
      toastTimeout = null;
    }

    // 새 토스트 상태 설정
    toastMessage.value = message;
    toastType.value = type;

    // 지정된 시간 후 자동으로 토스트 숨김
    toastTimeout = setTimeout(() => {
      toastMessage.value = null;
      toastTimeout = null;
    }, duration);
  }

  /**
   * 현재 표시 중인 토스트를 즉시 숨깁니다.
   *
   * @example
   * // 사용자가 토스트를 클릭했을 때 즉시 닫기
   * hideToast();
   */
  function hideToast(): void {
    if (toastTimeout) {
      clearTimeout(toastTimeout);
      toastTimeout = null;
    }
    toastMessage.value = null;
  }

  // ============================================
  // 반환 (Return)
  // ============================================

  return {
    /** 현재 토스트 메시지 (null이면 표시 안 함) */
    toastMessage,

    /** 현재 토스트 타입 */
    toastType,

    /** 토스트 표시 함수 */
    showToast,

    /** 토스트 즉시 숨김 함수 */
    hideToast,
  };
}
