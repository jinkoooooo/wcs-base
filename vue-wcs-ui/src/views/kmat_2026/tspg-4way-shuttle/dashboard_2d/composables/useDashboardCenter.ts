/**
 * useDashboardCenter.ts
 * 대시보드 센터(물류센터) 초기화 및 선택 관리 컴포저블
 *
 * ============================================
 * 기능 설명
 * ============================================
 * - 물류센터(LC) 선택 및 초기화 로직 관리
 * - 센터 선택 모달 표시/숨김 제어
 * - 라우트 파라미터, 로컬스토리지, 기본값 순으로 센터 ID 결정
 * - 센터 변경 시 스토어 리셋 및 재초기화 처리
 *
 * ============================================
 * 센터 ID 결정 우선순위
 * ============================================
 * 1. URL 라우트 파라미터 (route.params.lcId)
 * 2. 로컬스토리지 저장값 (TSPG_WORK_LC_ID)
 * 3. 기본값 (빈 문자열 → 센터 선택 모달 표시)
 *
 * ============================================
 * 사용 방법
 * ============================================
 * ```ts
 * const {
 *   showCenterModal,
 *   lcId,
 *   resolveInitialLcId,
 *   openCenterModal,
 *   handleCenterSelect,
 *   applyCenter
 * } = useDashboardCenter(store, router, route, callbacks);
 * ```
 */

import { ref, computed } from 'vue';
import { RouteLocationNormalizedLoaded, Router } from 'vue-router';

/** 센터 적용 완료 후 실행할 콜백 함수들 인터페이스 */
export interface CenterApplyCallbacks {
  /** 셔틀 보간 데이터 초기화 */
  clearShuttles: () => void;

  /** 화물 보간 데이터 초기화 */
  clearCargos: () => void;

  /** 뷰포트를 페이지에 맞춤 */
  fitToPage: () => void;

  /** 설비 상세 팝업 닫기 */
  closeDetailPopup: () => void;

  /** 셔틀 제어 팝업 닫기 */
  closeShuttlePopup: () => void;

  /** ECS 연결 상태 확인 */
  checkEcsConnection: () => Promise<void>;
}

/** Pinia Store 인터페이스 (필요한 메서드만 정의) */
export interface DashboardStore {
  selectedEqGroupId: string;
  pages: Array<{ id: string; eqGroupId?: string; pageIndex?: number }>;
  eqGroups: Array<{ id: string }>;
  activePageId: string;

  disconnectWebSocket: () => void;
  reset: () => void;
  initializeWithEqGroup: (lcId: string, eqGroupId: string) => Promise<void>;
  initializeLcOnly: (lcId: string) => Promise<void>;
  selectEqGroup: (eqGroupId: string) => Promise<void>;
  selectPage: (pageId: string) => void;
  loadLayouts: () => Promise<void>;
  loadDashboardData: () => Promise<void>;
  loadActiveJobs: () => Promise<void>;
  connectWebSocket: () => Promise<void>;
}

/**
 * 대시보드 센터 관리 컴포저블
 *
 * @param store - Pinia 셔틀 스토어 인스턴스
 * @param router - Vue Router 인스턴스
 * @param route - 현재 라우트 정보
 * @param callbacks - 센터 적용 후 실행할 콜백 함수들
 * @returns 센터 관련 상태와 제어 함수들
 */
export function useDashboardCenter(
  store: DashboardStore,
  router: Router,
  route: RouteLocationNormalizedLoaded,
  callbacks: CenterApplyCallbacks,
) {
  // ============================================
  // 상태 (State)
  // ============================================

  /** 센터 선택 모달 표시 여부 */
  const showCenterModal = ref(true);

  /** 현재 선택된 센터 ID (내부용) */
  const lcIdRef = ref<string>('');

  /** 기본 센터 ID (없으면 모달 표시) */
  const DEFAULT_LC_ID = '';

  // ============================================
  // 계산된 속성 (Computed)
  // ============================================

  /** 현재 센터 ID (읽기 전용) */
  const lcId = computed(() => lcIdRef.value);

  // ============================================
  // 함수 (Functions)
  // ============================================

  /**
   * 초기 센터 ID를 결정합니다.
   *
   * 우선순위:
   * 1. URL 라우트 파라미터 (route.params.lcId)
   * 2. 로컬스토리지 (TSPG_WORK_LC_ID)
   * 3. 기본값 (빈 문자열)
   *
   * @returns 결정된 센터 ID (빈 문자열이면 센터 선택 필요)
   */
  function resolveInitialLcId(): string {
    // 1순위: URL 파라미터
    const fromRoute = String(route.params.lcId ?? '').trim();
    if (fromRoute) return fromRoute;

    // 2순위: 로컬스토리지
    const fromLocal = String(localStorage.getItem('TSPG_WORK_LC_ID') ?? '').trim();
    if (fromLocal) return fromLocal;

    // 3순위: 기본값
    if (DEFAULT_LC_ID) return DEFAULT_LC_ID;

    return '';
  }

  /**
   * 센터 선택 모달을 엽니다.
   */
  function openCenterModal(): void {
    showCenterModal.value = true;
  }

  /**
   * 센터 선택 모달 닫기 처리
   * (현재는 반드시 센터를 선택해야 하므로 빈 함수)
   */
  function handleCenterModalClose(): void {
    // 센터 선택 필수 - 닫기 버튼 없음
  }

  /**
   * 센터 선택 완료 시 호출됩니다.
   *
   * @param nextLcId - 선택된 센터 ID
   */
  async function handleCenterSelect(nextLcId: string): Promise<void> {
    showCenterModal.value = false;
    await applyCenter(nextLcId);
  }

  /**
   * 선택된 센터를 적용합니다.
   *
   * 처리 순서:
   * 1. 센터 ID 유효성 검증
   * 2. 로컬스토리지에 저장
   * 3. URL 파라미터 업데이트
   * 4. 기존 WebSocket 연결 해제 및 스토어 초기화
   * 5. 새 센터로 스토어 초기화
   * 6. 페이지 데이터 로드
   * 7. WebSocket 재연결
   * 8. 뷰포트 맞춤
   *
   * @param nextLcId - 적용할 센터 ID
   */
  async function applyCenter(nextLcId: string): Promise<void> {
    // 센터 ID 정리 및 검증
    const v = String(nextLcId ?? '').trim();
    if (!v) return;

    // 로컬스토리지에 저장 (다음 접속 시 자동 선택)
    localStorage.setItem('TSPG_WORK_LC_ID', v);

    // URL 파라미터 업데이트 (뒤로가기 지원)
    try {
      if (route.params.lcId !== v) {
        await router.replace({
          name: route.name as string,
          params: { ...route.params, lcId: v },
          query: { ...route.query },
        });
      }
    } catch {
      // 라우트에 :lcId 파라미터가 없을 수도 있음 - 무시
    }

    // 내부 상태 업데이트
    lcIdRef.value = v;

    // 기존 연결 정리
    store.disconnectWebSocket();
    store.reset();
    callbacks.clearShuttles();
    callbacks.clearCargos();

    // 이전에 선택했던 설비그룹 확인
    const savedEqGroupId = localStorage.getItem('TSPG_WORK_EQ_GROUP_ID') || '';

    // 스토어 초기화 (설비그룹 유무에 따라 분기)
    if (savedEqGroupId) {
      await store.initializeWithEqGroup(v, savedEqGroupId);

      // 설비그룹이 없으면 첫 번째 그룹 선택
      if (!store.selectedEqGroupId && store.eqGroups.length > 0) {
        await store.selectEqGroup(store.eqGroups[0].id);
      }
    } else {
      await store.initializeLcOnly(v);
    }

    // 페이지 선택 (저장된 페이지 또는 첫 번째 페이지)
    if (store.selectedEqGroupId) {
      const savedPageId = localStorage.getItem(`TSPG_WORK_PAGE_${store.selectedEqGroupId}`);
      const groupId = store.selectedEqGroupId || '';
      const candidatePages = (store.pages || []).filter((p) => (p.eqGroupId || '') === groupId);

      if (savedPageId && store.pages.some((p) => p.id === savedPageId)) {
        store.selectPage(savedPageId);
      } else if (candidatePages.length > 0) {
        store.selectPage(
          candidatePages.sort((a, b) => (a.pageIndex ?? 0) - (b.pageIndex ?? 0))[0].id,
        );
      }
    }

    // 페이지 데이터 로드 및 WebSocket 연결
    if (store.activePageId) {
      await store.loadLayouts();
      await store.loadDashboardData();
      await store.loadActiveJobs();
      await store.connectWebSocket();
    }

    // ECS 연결 상태 확인
    await callbacks.checkEcsConnection();

    // 뷰포트를 페이지에 맞춤
    callbacks.fitToPage();

    // 팝업 닫기
    callbacks.closeDetailPopup();
    callbacks.closeShuttlePopup();
  }

  /**
   * 설비그룹 선택 처리
   *
   * @param eqGroupId - 선택할 설비그룹 ID
   */
  async function handleSelectEqGroup(eqGroupId: string): Promise<void> {
    if (!eqGroupId || eqGroupId === store.selectedEqGroupId) return;

    // 로컬스토리지에 저장
    localStorage.setItem('TSPG_WORK_EQ_GROUP_ID', eqGroupId);

    // 스토어에서 설비그룹 선택
    await store.selectEqGroup(eqGroupId);

    // 이전에 선택했던 페이지 확인
    const savedPageId = localStorage.getItem(`TSPG_WORK_PAGE_${eqGroupId}`);
    if (savedPageId && store.pages.some((p) => p.id === savedPageId)) {
      store.selectPage(savedPageId);
    } else if (store.pages.length > 0) {
      // 첫 번째 페이지 선택 (정렬된 상태)
      const groupPages = store.pages.filter((p) => (p.eqGroupId || '') === eqGroupId);
      if (groupPages.length > 0) {
        const sorted = [...groupPages].sort((a, b) => (a.pageIndex ?? 0) - (b.pageIndex ?? 0));
        store.selectPage(sorted[0].id);
      }
    }

    // 데이터 초기화 및 재로드
    callbacks.clearShuttles();
    callbacks.clearCargos();
    await store.loadLayouts();
    await store.loadDashboardData();
    await store.loadActiveJobs();

    // WebSocket 재연결 (설비그룹 변경 시 구독 갱신)
    store.disconnectWebSocket();
    await store.connectWebSocket();

    // 뷰포트 맞춤
    callbacks.fitToPage();
  }

  // ============================================
  // 반환 (Return)
  // ============================================

  return {
    /** 센터 선택 모달 표시 여부 */
    showCenterModal,

    /** 현재 센터 ID */
    lcId,

    /** 내부 센터 ID ref (직접 수정 필요 시) */
    lcIdRef,

    /** 초기 센터 ID 결정 함수 */
    resolveInitialLcId,

    /** 센터 선택 모달 열기 */
    openCenterModal,

    /** 센터 모달 닫기 처리 */
    handleCenterModalClose,

    /** 센터 선택 완료 처리 */
    handleCenterSelect,

    /** 센터 적용 */
    applyCenter,

    /** 설비그룹 선택 처리 */
    handleSelectEqGroup,
  };
}
