/**
 * useDashboardSearch.ts
 * 대시보드 검색 기능 Composable
 *
 * ============================================
 * 기능 설명
 * ============================================
 * 1. 설비 코드 검색 (컨베이어, 리프터, 랙 등)
 * 2. 셔틀 코드 검색
 * 3. 화물 바코드 검색
 * 4. 랙 위치(셀) 검색
 * 5. 검색 결과 하이라이트 및 맵 이동 지원
 *
 * ============================================
 * 사용 예시
 * ============================================
 * ```ts
 * const search = useDashboardSearch(store);
 *
 * // 검색 실행
 * search.setQuery('S-001');
 *
 * // 결과 선택
 * search.selectResult(search.results.value[0]);
 * ```
 */

import { ref, computed, watch } from 'vue';
import type { Ref, ComputedRef } from 'vue';
import type { DashboardEquipmentData, DashboardShuttleData, DashboardCargoData } from '../api/types';

// ============================================
// 타입 정의
// ============================================

/**
 * 검색 결과 타입
 */
export type SearchResultType = 'equipment' | 'shuttle' | 'cargo' | 'rack';

/**
 * 검색 결과 아이템 인터페이스
 */
export interface SearchResultItem {
  /** 결과 타입 */
  type: SearchResultType;
  /** 고유 ID */
  id: string;
  /** 표시용 코드/이름 */
  code: string;
  /** 부가 정보 (타입명, 위치 등) */
  subText: string;
  /** 검색 점수 (높을수록 관련성 높음) */
  score: number;
  /** X 좌표 (맵 이동용) */
  posX: number;
  /** Y 좌표 (맵 이동용) */
  posY: number;
  /** 원본 데이터 참조 */
  raw: any;
}

/**
 * 검색 Composable 반환 타입
 */
export interface DashboardSearchReturn {
  /** 검색어 */
  query: Ref<string>;
  /** 검색 결과 목록 */
  results: ComputedRef<SearchResultItem[]>;
  /** 검색 로딩 상태 */
  isSearching: Ref<boolean>;
  /** 검색어 설정 */
  setQuery: (q: string) => void;
  /** 검색 초기화 */
  clearSearch: () => void;
  /** 결과 선택 콜백 설정 */
  onSelectResult: (callback: (item: SearchResultItem) => void) => void;
  /** 결과 선택 실행 */
  selectResult: (item: SearchResultItem) => void;
  /** 검색 결과 있는지 여부 */
  hasResults: ComputedRef<boolean>;
  /** 검색어 있는지 여부 */
  hasQuery: ComputedRef<boolean>;
}

// ============================================
// 상수
// ============================================

/** 최대 검색 결과 개수 */
const MAX_RESULTS = 50;

/** 최소 검색어 길이 */
const MIN_QUERY_LENGTH = 1;

/** 설비 타입 한글 매핑 */
const EQUIPMENT_TYPE_LABELS: Record<string, string> = {
  CONVEYOR: '컨베이어',
  LIFTER: '리프터',
  RACK: '랙',
  SHUTTLE: '셔틀',
  BCR: 'BCR',
  PILLAR: '기둥',
  STV: 'STV',
  CRANE: '크레인',
};

// ============================================
// Composable 함수
// ============================================

/**
 * 대시보드 검색 Composable
 *
 * @param equipments - 설비 데이터 ref
 * @param shuttles - 셔틀 데이터 ref
 * @param cargos - 화물 데이터 ref
 * @returns 검색 상태 및 제어 함수들
 */
export function useDashboardSearch(
  equipments: Ref<DashboardEquipmentData[]>,
  shuttles: Ref<DashboardShuttleData[]>,
  cargos: Ref<DashboardCargoData[]>,
): DashboardSearchReturn {
  // ========================================
  // 상태
  // ========================================
  const query = ref('');
  const isSearching = ref(false);
  let selectCallback: ((item: SearchResultItem) => void) | null = null;

  // ========================================
  // 계산된 값
  // ========================================

  const hasQuery = computed(() => query.value.trim().length >= MIN_QUERY_LENGTH);

  /**
   * 검색 결과 계산
   */
  const results = computed<SearchResultItem[]>(() => {
    const q = query.value.trim().toLowerCase();

    if (q.length < MIN_QUERY_LENGTH) {
      return [];
    }

    const allResults: SearchResultItem[] = [];

    // 1) 설비 검색
    for (const eq of equipments.value || []) {
      const score = calculateEquipmentScore(eq, q);
      if (score > 0) {
        allResults.push({
          type: 'equipment',
          id: eq.id || '',
          code: eq.equipmentCode || eq.realEqId || '-',
          subText: getEquipmentSubText(eq),
          score,
          posX: eq.posX ?? 0,
          posY: eq.posY ?? 0,
          raw: eq,
        });
      }
    }

    // 2) 셔틀 검색
    for (const shuttle of shuttles.value || []) {
      const score = calculateShuttleScore(shuttle, q);
      if (score > 0) {
        allResults.push({
          type: 'shuttle',
          id: shuttle.equipmentId || '',
          code: shuttle.equipmentCode || shuttle.equipmentId || '-',
          subText: getShuttleSubText(shuttle),
          score,
          posX: shuttle.posX ?? 0,
          posY: shuttle.posY ?? 0,
          raw: shuttle,
        });
      }
    }

    // 3) 화물 검색
    for (const cargo of cargos.value || []) {
      const score = calculateCargoScore(cargo, q);
      if (score > 0) {
        allResults.push({
          type: 'cargo',
          id: cargo.cargoId || '',
          code: cargo.barcode || cargo.cargoId || '-',
          subText: getCargoSubText(cargo),
          score,
          posX: cargo.posX ?? 0,
          posY: cargo.posY ?? 0,
          raw: cargo,
        });
      }
    }

    // 점수 내림차순 정렬 후 상위 결과만 반환
    return allResults
      .sort((a, b) => b.score - a.score)
      .slice(0, MAX_RESULTS);
  });

  const hasResults = computed(() => results.value.length > 0);

  // ========================================
  // 점수 계산 함수들
  // ========================================

  /**
   * 설비 검색 점수 계산
   */
  function calculateEquipmentScore(eq: DashboardEquipmentData, q: string): number {
    let score = 0;

    // 설비 코드 매칭
    const code = (eq.equipmentCode || '').toLowerCase();
    if (code === q) {
      score += 100; // 완전 일치
    } else if (code.startsWith(q)) {
      score += 80; // 시작 일치
    } else if (code.includes(q)) {
      score += 50; // 부분 일치
    }

    // 실제 설비 ID 매칭
    const realEqId = (eq.realEqId || '').toLowerCase();
    if (realEqId === q) {
      score += 90;
    } else if (realEqId.startsWith(q)) {
      score += 70;
    } else if (realEqId.includes(q)) {
      score += 40;
    }

    // 랙 셀 코드 매칭 (위치 검색)
    const cellCode = (eq.cellCode || '').toLowerCase();
    if (cellCode && cellCode.includes(q)) {
      score += 60;
    }

    // 설명/이름 매칭
    const name = (eq.equipmentName || '').toLowerCase();
    if (name.includes(q)) {
      score += 20;
    }

    return score;
  }

  /**
   * 셔틀 검색 점수 계산
   */
  function calculateShuttleScore(shuttle: DashboardShuttleData, q: string): number {
    let score = 0;

    // 셔틀 코드 매칭
    const code = (shuttle.equipmentCode || '').toLowerCase();
    if (code === q) {
      score += 100;
    } else if (code.startsWith(q)) {
      score += 80;
    } else if (code.includes(q)) {
      score += 50;
    }

    // 셔틀 ID 매칭
    const id = (shuttle.equipmentId || '').toLowerCase();
    if (id === q) {
      score += 90;
    } else if (id.startsWith(q)) {
      score += 70;
    } else if (id.includes(q)) {
      score += 40;
    }

    // 현재 바코드 매칭
    const barcode = (shuttle.currentBarcode || '').toLowerCase();
    if (barcode && barcode.includes(q)) {
      score += 60;
    }

    // 작업 키 매칭
    const jobKey = (shuttle.currentJobKey || shuttle.currentOrderKey || '').toLowerCase();
    if (jobKey && jobKey.includes(q)) {
      score += 30;
    }

    return score;
  }

  /**
   * 화물 검색 점수 계산
   */
  function calculateCargoScore(cargo: DashboardCargoData, q: string): number {
    let score = 0;

    // 바코드 매칭
    const barcode = (cargo.barcode || '').toLowerCase();
    if (barcode === q) {
      score += 100;
    } else if (barcode.startsWith(q)) {
      score += 80;
    } else if (barcode.includes(q)) {
      score += 50;
    }

    // 화물 ID 매칭
    const cargoId = (cargo.cargoId || '').toLowerCase();
    if (cargoId.includes(q)) {
      score += 40;
    }

    // 저장 위치 매칭
    const cellId = (cargo.storedCellId || '').toLowerCase();
    if (cellId && cellId.includes(q)) {
      score += 30;
    }

    return score;
  }

  // ========================================
  // 부가 텍스트 생성 함수들
  // ========================================

  function getEquipmentSubText(eq: DashboardEquipmentData): string {
    const type = EQUIPMENT_TYPE_LABELS[eq.equipmentTypeCode?.toUpperCase() || ''] || eq.equipmentTypeCode || '';
    const location = eq.cellCode ? `위치: ${eq.cellCode}` : '';
    return [type, location].filter(Boolean).join(' · ');
  }

  function getShuttleSubText(shuttle: DashboardShuttleData): string {
    const parts: string[] = ['셔틀'];

    if (shuttle.hasActiveJob) {
      parts.push('작업중');
    } else if (shuttle.hasCargo) {
      parts.push('화물적재');
    }

    if (shuttle.batteryLevel != null) {
      parts.push(`배터리 ${shuttle.batteryLevel}%`);
    }

    return parts.join(' · ');
  }

  function getCargoSubText(cargo: DashboardCargoData): string {
    const parts: string[] = ['화물'];

    if (cargo.carriedByShuttleId) {
      parts.push(`셔틀: ${cargo.carriedByShuttleId}`);
    } else if (cargo.storedCellId) {
      parts.push(`위치: ${cargo.storedCellId}`);
    }

    const statusMap: Record<string, string> = {
      STORED: '보관중',
      MOVING: '이동중',
      PENDING: '대기',
      PICKING: '피킹중',
    };
    if (cargo.cargoStatus && statusMap[cargo.cargoStatus]) {
      parts.push(statusMap[cargo.cargoStatus]);
    }

    return parts.join(' · ');
  }

  // ========================================
  // 함수
  // ========================================

  function setQuery(q: string): void {
    query.value = q;
  }

  function clearSearch(): void {
    query.value = '';
  }

  function onSelectResult(callback: (item: SearchResultItem) => void): void {
    selectCallback = callback;
  }

  function selectResult(item: SearchResultItem): void {
    if (selectCallback) {
      selectCallback(item);
    }
  }

  // ========================================
  // 반환
  // ========================================
  return {
    query,
    results,
    isSearching,
    hasQuery,
    hasResults,
    setQuery,
    clearSearch,
    onSelectResult,
    selectResult,
  };
}
