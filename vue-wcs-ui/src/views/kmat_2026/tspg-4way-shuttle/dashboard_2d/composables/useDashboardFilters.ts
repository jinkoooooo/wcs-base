/**
 * useDashboardFilters.ts
 * 대시보드 필터 및 하이라이트 모드 관리 Composable
 *
 * ============================================
 * 기능
 * ============================================
 * 1. 설비 타입 필터 (셔틀 / 컨베이어 / 리프터 / 랙)
 * 2. 상태 필터 (normal / working / error / charging)
 * 3. 에러 하이라이트 모드 (에러 설비만 강조, 나머지 dim)
 *
 * ============================================
 * 판정 원칙 — 정규화 필드만 사용
 * ============================================
 * - Shuttle  → data.shuttleState   (legend-spec ShuttleState)
 * - Conveyor → rtCv.conveyorState  (legend-spec ConveyorState)
 *              + 작업중 보조 신호로 moving / runYn 만 허용
 * - Lifter   → errorId / moving (Lifter 는 아직 정규화 범위 밖. useYn=false 리프터는 에러 제외)
 *
 * 구 status 숫자(0/1/2/5/8/9) 기반 분기는 전부 제거됨.
 */

import { ref, computed } from 'vue';
import type { Ref, ComputedRef } from 'vue';
import type { DashboardEquipmentData, RtConveyorStatus } from '../api/types';
import { isError } from '../api/types';

// ============================================
// 타입
// ============================================

export type EquipmentTypeFilter = 'SHUTTLE' | 'CONVEYOR' | 'LIFTER' | 'RACK' | null;
export type StatusFilter = 'normal' | 'working' | 'error' | 'charging' | null;

export interface FilterState {
  equipmentType: EquipmentTypeFilter;
  status: StatusFilter;
}

/**
 * Filter composable 이 의존하는 store 의 최소 면적.
 * Conveyor 판정에는 rtConveyors 로부터 conveyorState 를 조회해야 함.
 */
export interface FilterStore {
  rtConveyors: RtConveyorStatus[];
}

export interface DashboardFiltersReturn {
  highlightMode: Ref<boolean>;
  activeFilter: Ref<FilterState>;
  activeFilterKey: ComputedRef<string | null>;
  hasActiveFilter: ComputedRef<boolean>;
  toggleHighlightMode: () => void;
  setFilter: (type: EquipmentTypeFilter, status: StatusFilter) => void;
  clearFilters: () => void;
  isEquipmentDimmed: (eq: DashboardEquipmentData) => boolean;
  isShuttleDimmed: (shuttle: any) => boolean;
  isLifterDimmed: (lifter: any) => boolean;
}

// ============================================
// 구현
// ============================================

export function useDashboardFilters(store: FilterStore): DashboardFiltersReturn {
  const highlightMode = ref(false);
  const activeFilter = ref<FilterState>({ equipmentType: null, status: null });

  const activeFilterKey = computed<string | null>(() => {
    const { equipmentType, status } = activeFilter.value;
    if (!equipmentType && !status) return null;
    const typePart = equipmentType?.toLowerCase() || 'all';
    const statusPart = status || 'all';
    return `${typePart}-${statusPart}`;
  });

  const hasActiveFilter = computed(() => {
    return (
      highlightMode.value ||
      activeFilter.value.equipmentType !== null ||
      activeFilter.value.status !== null
    );
  });

  function toggleHighlightMode(): void {
    highlightMode.value = !highlightMode.value;
    activeFilter.value = highlightMode.value
      ? { equipmentType: null, status: 'error' }
      : { equipmentType: null, status: null };
  }

  function setFilter(type: EquipmentTypeFilter, status: StatusFilter): void {
    const currentType = activeFilter.value.equipmentType;
    const currentStatus = activeFilter.value.status;
    if (currentType === type && currentStatus === status) {
      clearFilters();
      return;
    }
    activeFilter.value = { equipmentType: type, status };
    highlightMode.value = status === 'error';
  }

  function clearFilters(): void {
    activeFilter.value = { equipmentType: null, status: null };
    highlightMode.value = false;
  }

  // -----------------------------------------
  // 판정 로직 — 정규화 필드 전용
  // -----------------------------------------

  function findRtConveyor(eq: DashboardEquipmentData): RtConveyorStatus | undefined {
    if (!eq?.realEqId) return undefined;
    return store.rtConveyors.find(
      (cv) => cv.eqId === eq.realEqId || cv.equipmentId === eq.realEqId,
    );
  }

  function isEquipmentDimmed(eq: DashboardEquipmentData): boolean {
    if (!hasActiveFilter.value) return false;
    const filter = activeFilter.value;
    const eqType = eq.equipmentTypeCode?.toUpperCase() || '';

    if (filter.equipmentType && eqType !== filter.equipmentType) {
      return true;
    }

    if (eqType === 'CONVEYOR') {
      const rtCv = findRtConveyor(eq);
      const state = rtCv?.conveyorState ?? null;
      switch (filter.status) {
        case 'error':
          return state !== 'ERROR';
        case 'working':
          // working = 정규화 범위 밖의 물리적 가동. moving / runYn 만 허용.
          return !(rtCv?.moving === true || rtCv?.runYn === true);
        case 'normal':
          return state !== 'AUTO';
        default:
          return false;
      }
    }

    // Shuttle / Lifter / Rack 은 이 함수의 주 대상이 아님. 통과.
    return false;
  }

  function isShuttleDimmed(shuttle: any): boolean {
    if (!hasActiveFilter.value) return false;
    const filter = activeFilter.value;

    if (filter.equipmentType && filter.equipmentType !== 'SHUTTLE') {
      return true;
    }

    const data = shuttle?.data || shuttle || {};
    const state: string = data.shuttleState ?? 'IDLE';

    switch (filter.status) {
      case 'error':
        return state !== 'ERROR';
      case 'working':
        return state !== 'RUNNING';
      case 'normal':
        return state !== 'IDLE';
      case 'charging':
        return state !== 'CHARGING';
      default:
        return false;
    }
  }

  function isLifterDimmed(lifter: any): boolean {
    // Lifter 는 정규화 범위 밖. TODO: Lifter 도 정규화 *State 필드 도입 시 여기 교체.
    if (!hasActiveFilter.value) return false;
    const filter = activeFilter.value;

    if (filter.equipmentType && filter.equipmentType !== 'LIFTER') {
      return true;
    }

    // use_yn=false 리프터는 알람 대상이 아니므로 에러 필터에서도 제외.
    const disabled = lifter?.useYn === false;
    const liftHasError = !disabled && isError(lifter?.errorId);

    switch (filter.status) {
      case 'error':
        return !liftHasError;
      case 'working':
        return !lifter?.moving;
      case 'normal':
        return lifter?.moving || liftHasError;
      default:
        return false;
    }
  }

  return {
    highlightMode,
    activeFilter,
    activeFilterKey,
    hasActiveFilter,
    toggleHighlightMode,
    setFilter,
    clearFilters,
    isEquipmentDimmed,
    isShuttleDimmed,
    isLifterDimmed,
  };
}
