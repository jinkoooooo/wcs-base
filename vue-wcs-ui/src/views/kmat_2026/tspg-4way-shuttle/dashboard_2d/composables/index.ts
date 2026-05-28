/**
 * composables/index.ts
 * 대시보드 2D 컴포저블 모듈 통합 내보내기
 *
 * ============================================
 * 컴포저블 목록
 * ============================================
 *
 * 1. useToast
 *    - 토스트 메시지 관리
 *    - 성공/오류/정보 메시지 표시
 *
 * 2. useDashboardCenter
 *    - 물류센터(LC) 선택 및 초기화
 *    - 센터 변경 시 스토어 리셋 처리
 *
 * 3. useEcsCommands
 *    - ECS 서버 연결 상태 확인
 *    - 셔틀/작업 제어 명령
 *
 * 4. useTaskGridResize
 *    - 하단 작업 패널 높이 조절
 *    - 드래그 리사이즈 지원
 *
 * 5. useDashboardStyles
 *    - 설비/셔틀/화물 스타일 계산
 *    - CSS 클래스 결정
 *
 * 6. useInterpolation
 *    - 셔틀/화물 위치 보간 애니메이션
 *    - 부드러운 이동 효과
 *
 * 7. useStageViewport
 *    - 2D 캔버스 줌/팬 제어
 *    - 뷰포트 상태 관리
 *
 * 8. useCanvasSelection (에디터용)
 *    - 캔버스 객체 선택 관리
 *
 * 9. useCanvasSnapping (에디터용)
 *    - 객체 스냅 기능
 *
 * 10. useCanvasViewport (에디터용)
 *     - 에디터 전용 뷰포트
 *
 * 11. useKeyboardShortcuts (에디터용)
 *     - 키보드 단축키 처리
 */

// ============================================
// 대시보드 공통 컴포저블
// ============================================

/**
 * 토스트 메시지 관리
 * @see useToast.ts
 */
export { useToast } from './useToast';
export type { ToastType } from './useToast';

/**
 * 센터 초기화 및 선택 관리
 * @see useDashboardCenter.ts
 */
export { useDashboardCenter } from './useDashboardCenter';
export type { CenterApplyCallbacks, DashboardStore } from './useDashboardCenter';

/**
 * ECS 제어 명령
 * @see useEcsCommands.ts
 */
export { useEcsCommands } from './useEcsCommands';

/**
 * 작업 그리드 리사이즈
 * @see useTaskGridResize.ts
 */
export { useTaskGridResize } from './useTaskGridResize';
export type { TaskGridResizeOptions } from './useTaskGridResize';

/**
 * 대시보드 스타일 계산
 * @see useDashboardStyles.ts
 */
export { useDashboardStyles } from './useDashboardStyles';
export type {
  PalletStateCode,
  ConveyorCargoItem,
  RackInventoryItem,
  StyleStore,
} from './useDashboardStyles';

// ============================================
// 애니메이션 관련 컴포저블
// ============================================

/**
 * 셔틀/화물 위치 보간 애니메이션
 * @see useInterpolation.ts
 */
export {
  useShuttleInterpolation,
  useCargoInterpolation,
} from './useInterpolation';

/**
 * 2D 스테이지 뷰포트 (줌/팬)
 * @see useStageViewport.ts
 */
export { useStageViewport } from './useStageViewport';

// ============================================
// 에디터 전용 컴포저블
// ============================================

/**
 * 캔버스 객체 선택 관리
 * @see useCanvasSelection.ts
 */
export { useCanvasSelection } from './useCanvasSelection';

/**
 * 객체 스냅 기능
 * @see useCanvasSnapping.ts
 */
export { snapPosition } from './useCanvasSnapping';
export type { SnapOptions } from './useCanvasSnapping';

/**
 * 에디터 뷰포트
 * @see useCanvasViewport.ts
 */
export { useCanvasViewport } from './useCanvasViewport';

/**
 * 키보드 단축키
 * @see useKeyboardShortcuts.ts
 */
export { useKeyboardShortcuts } from './useKeyboardShortcuts';

// ============================================
// 대시보드 고도화 컴포저블
// ============================================

/**
 * 필터 및 하이라이트 모드 관리
 * @see useDashboardFilters.ts
 */
export { useDashboardFilters } from './useDashboardFilters';
export type {
  EquipmentTypeFilter,
  StatusFilter,
  FilterState,
  DashboardFiltersReturn,
} from './useDashboardFilters';

/**
 * 검색 기능
 * @see useDashboardSearch.ts
 */
export { useDashboardSearch } from './useDashboardSearch';
export type {
  SearchResultType,
  SearchResultItem,
  DashboardSearchReturn,
} from './useDashboardSearch';
