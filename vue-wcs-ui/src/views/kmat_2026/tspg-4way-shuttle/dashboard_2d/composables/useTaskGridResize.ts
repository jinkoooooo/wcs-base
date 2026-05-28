/**
 * useTaskGridResize.ts
 * 작업 그리드(TaskGrid) 리사이즈 컴포저블
 *
 * ============================================
 * 기능 설명
 * ============================================
 * - 하단 작업 패널의 높이를 드래그로 조절
 * - 최소/최대 높이 제한으로 사용성 보장
 * - 접기/펼치기 토글 지원
 * - 리사이즈 시 뷰포트 자동 재계산
 *
 * ============================================
 * UI 동작
 * ============================================
 * 1. 드래그 리사이저 바를 위/아래로 드래그
 * 2. 위로 드래그 → 패널 높이 증가
 * 3. 아래로 드래그 → 패널 높이 감소
 * 4. 최소 160px ~ 최대 화면 70% 범위 제한
 *
 * ============================================
 * 사용 방법
 * ============================================
 * ```vue
 * <template>
 *   <div
 *     class="task-resizer"
 *     :class="{ dragging: isResizing }"
 *     @mousedown.stop.prevent="startResize"
 *   />
 *   <TaskGrid :height="height" v-model:collapsed="isCollapsed" />
 * </template>
 *
 * <script setup>
 * const {
 *   height,
 *   isCollapsed,
 *   isResizing,
 *   startResize,
 *   cleanup
 * } = useTaskGridResize({
 *   onResize: () => viewport.recalculateFit({ keepWorldCenter: true })
 * });
 *
 * onUnmounted(() => cleanup());
 * </script>
 * ```
 */

import { ref } from 'vue';

/** 리사이즈 옵션 인터페이스 */
export interface TaskGridResizeOptions {
  /** 초기 높이 (픽셀, 기본값: 320) */
  initialHeight?: number;

  /** 최소 높이 (픽셀, 기본값: 160) */
  minHeight?: number;

  /** 최대 높이 비율 (화면 높이 대비, 기본값: 0.7 = 70%) */
  maxHeightRatio?: number;

  /** 리사이즈 중 호출되는 콜백 (뷰포트 재계산 등) */
  onResize?: () => void;
}

/**
 * 작업 그리드 리사이즈 컴포저블
 *
 * @param options - 리사이즈 설정 옵션
 * @returns 리사이즈 상태와 제어 함수들
 */
export function useTaskGridResize(options: TaskGridResizeOptions = {}) {
  // ============================================
  // 설정값 (기본값 적용)
  // ============================================

  /** 초기 높이 (픽셀) */
  const initialHeight = options.initialHeight ?? 320;

  /** 최소 높이 (픽셀) - 이 이하로는 줄어들지 않음 */
  const MIN_HEIGHT = options.minHeight ?? 160;

  /** 최대 높이 비율 (화면 높이 대비) */
  const MAX_HEIGHT_RATIO = options.maxHeightRatio ?? 0.7;

  /** 리사이즈 중 콜백 */
  const onResizeCallback = options.onResize;

  // ============================================
  // 상태 (State)
  // ============================================

  /** 현재 작업 그리드 높이 (픽셀) */
  const height = ref(initialHeight);

  /** 그리드 접힘 상태 (true: 접힘, false: 펼침) */
  const isCollapsed = ref(false);

  /** 현재 리사이즈 진행 중 여부 */
  const isResizing = ref(false);

  /** 드래그 시작 시 마우스 Y 좌표 */
  const resizeStartY = ref(0);

  /** 드래그 시작 시 그리드 높이 */
  const resizeStartHeight = ref(0);

  // ============================================
  // 내부 함수 (Private)
  // ============================================

  /**
   * 높이 값을 유효 범위 내로 제한합니다.
   *
   * @param h - 제한할 높이 값
   * @returns 유효 범위 내의 높이 값
   */
  function clampHeight(h: number): number {
    const min = MIN_HEIGHT;
    // 최대 높이: 화면 높이의 70% 또는 최소 240px
    const max = Math.max(240, Math.floor(window.innerHeight * MAX_HEIGHT_RATIO));
    return Math.min(Math.max(h, min), max);
  }

  /**
   * 마우스 이동 이벤트 핸들러 (드래그 중)
   *
   * @param e - 마우스 이벤트
   */
  function onResizeMove(e: MouseEvent): void {
    if (!isResizing.value) return;

    // 위로 드래그하면 높이 증가 (Y 감소 → dy 양수)
    const dy = resizeStartY.value - e.clientY;
    height.value = clampHeight(resizeStartHeight.value + dy);

    // 뷰포트 재계산 콜백 호출
    onResizeCallback?.();
  }

  /**
   * 리사이즈 종료 처리
   * (마우스 업 또는 마우스가 화면을 벗어났을 때)
   */
  function onResizeEnd(): void {
    if (!isResizing.value) return;

    isResizing.value = false;

    // 전역 이벤트 리스너 제거
    window.removeEventListener('mousemove', onResizeMove);
    window.removeEventListener('mouseup', onResizeEnd);
    window.removeEventListener('mouseleave', onResizeEnd);
  }

  // ============================================
  // 외부 함수 (Public)
  // ============================================

  /**
   * 리사이즈 시작 (마우스 다운 이벤트 핸들러)
   *
   * 리사이저 바에서 마우스 다운 시 호출됩니다.
   *
   * @param e - 마우스 이벤트
   *
   * @example
   * <div @mousedown.stop.prevent="startResize">리사이저</div>
   */
  function startResize(e: MouseEvent): void {
    isResizing.value = true;
    resizeStartY.value = e.clientY;
    resizeStartHeight.value = height.value;

    // 전역 이벤트 리스너 등록 (드래그 추적)
    window.addEventListener('mousemove', onResizeMove);
    window.addEventListener('mouseup', onResizeEnd);
    window.addEventListener('mouseleave', onResizeEnd);
  }

  /**
   * 이벤트 리스너 정리 (컴포넌트 언마운트 시 호출)
   *
   * @example
   * onUnmounted(() => cleanup());
   */
  function cleanup(): void {
    onResizeEnd();
  }

  /**
   * 작업 그리드를 접거나 펼칩니다.
   *
   * @param collapsed - true: 접기, false: 펼치기
   */
  function setCollapsed(collapsed: boolean): void {
    isCollapsed.value = collapsed;
  }

  /**
   * 접기/펼치기 토글
   */
  function toggleCollapsed(): void {
    isCollapsed.value = !isCollapsed.value;
  }

  /**
   * 높이를 특정 값으로 설정합니다.
   *
   * @param newHeight - 새 높이 값 (유효 범위로 자동 제한)
   */
  function setHeight(newHeight: number): void {
    height.value = clampHeight(newHeight);
  }

  /**
   * 높이를 초기값으로 리셋합니다.
   */
  function resetHeight(): void {
    height.value = initialHeight;
  }

  // ============================================
  // 반환 (Return)
  // ============================================

  return {
    /** 현재 그리드 높이 (픽셀) */
    height,

    /** 그리드 접힘 상태 */
    isCollapsed,

    /** 리사이즈 진행 중 여부 */
    isResizing,

    /** 리사이즈 시작 (마우스 다운 핸들러) */
    startResize,

    /** 이벤트 리스너 정리 */
    cleanup,

    /** 접힘 상태 설정 */
    setCollapsed,

    /** 접기/펼치기 토글 */
    toggleCollapsed,

    /** 높이 설정 */
    setHeight,

    /** 높이 초기화 */
    resetHeight,
  };
}
