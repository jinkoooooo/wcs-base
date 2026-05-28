/**
 * shuttleStore.ts
 * 4-Way Shuttle Dashboard/Editor Pinia Store (From Scratch)
 *
 * 목표:
 * - 1) 코드가 읽히고 2) 변경 범위가 예측 가능하고 3) 버그가 재발하지 않게
 *
 * 설계:
 * - store는 상태와 흐름(orchestration)만 담당한다.
 * - REST 초기 로드 + WebSocket 실시간 구독을 "단일 진입점(connect)"로 통합한다.
 * - 구독 키(lcId/eqGroupId/pageId)가 바뀌면 teardown → setup 순서로 재구독한다.
 * - 화면이 믿는 상태는 dashboardXXX 로 수렴한다(레거시/Provider 어디서 오든 최종은 동일).
 */

import { defineStore } from 'pinia';
import { ref, computed, shallowRef, markRaw } from 'vue';

import type {
  // Layout
  TbEcs2dPage,
  TbEcs2dItemType,
  TbEcs2dItem,

  // Dashboard view models
  DashboardEquipmentData,
  DashboardShuttleData,
  DashboardCargoData,
  LayoutWithRealStatus,

  // Alarm / History
  AlarmData,
  HistoryItem,

  // WebSocket events (legacy topics)
  WebSocketError,
  EquipmentPositionEvent,
  EquipmentMoveEvent,
  BcrScanEvent,

  // Real 운영 테이블 타입
  TbEqGroupMst,
  TbEqMst,
  TbWcsShuttleOrder,

  // Provider 기반 RealTime DTO
  RtShuttlePosition,
  RtConveyorStatus,
  RtLifterStatus,
  RtCargoPosition,
  RtJobStatus,
  RtAlarm,
} from '../api/types';

import { isConveyorType, isShuttleType, isLifterType, LayoutEquipmentType } from '../api/types';

import {
  // Layout/Editor APIs
  layoutPageApi,
  equipmentTypeApi,
  equipmentLayoutApi,

  // Real 운영 APIs
  eqGroupApi,
  realEqMstApi,
  realTimeApi,
} from '../api/shuttle';

import { getWebSocketClient, destroyWebSocketClient } from '../api/websocket';
import { defaultTypeIconDataUrl } from '../assets/equipmentTypeIcons';

// =========================================================
// Constants
// =========================================================
const MAX_HISTORY_SIZE = 50;
const WCS_ORDERS_POLLING_INTERVAL = 3000;

// =========================================================
// Utils (store 내부에서만 쓰는 작은 유틸)
// =========================================================

/** 안전한 ISO string 변환 */
function toIso(ts?: any) {
  try {
    return ts ? new Date(ts).toISOString() : new Date().toISOString();
  } catch {
    return new Date().toISOString();
  }
}

/**
 * 깜빡임 방지 in-place merge
 * - 배열을 교체하지 않고 splice/assign로만 갱신
 * - Vue가 동일 배열 참조를 유지하므로 UI 깜빡임이 줄어듦
 */
function mergeArrayInPlace<T>(
  target: T[],
  incoming: T[],
  keyFn: (x: T) => string,
  shouldUpdate?: (oldItem: T, newItem: T) => boolean,
) {
  const incomingMap = new Map<string, T>();
  for (const x of incoming) incomingMap.set(keyFn(x), x);

  // remove (뒤에서부터)
  for (let i = target.length - 1; i >= 0; i--) {
    const k = keyFn(target[i]);
    if (!incomingMap.has(k)) target.splice(i, 1);
  }

  // add/update
  for (const x of incoming) {
    const k = keyFn(x);
    const idx = target.findIndex((t) => keyFn(t) === k);
    if (idx === -1) {
      target.push(x);
    } else {
      const oldItem = target[idx];
      const ok = shouldUpdate ? shouldUpdate(oldItem, x) : true;
      if (ok) Object.assign(oldItem as any, x as any);
    }
  }
}

/** 구독 키(중복 구독 방지) */
type SubKey = { lcId: string; eqGroupId: string; pageId: string };
function makeSubKey(lcId: string, eqGroupId: string, pageId: string): SubKey {
  return { lcId, eqGroupId: eqGroupId || '', pageId };
}
function sameSubKey(a: SubKey | null, b: SubKey | null) {
  if (!a || !b) return false;
  return a.lcId === b.lcId && a.eqGroupId === b.eqGroupId && a.pageId === b.pageId;
}

// =========================================================
// Store
// =========================================================
export const useShuttleStore = defineStore('shuttle', () => {
  // =========================================================
  // 1) Core State (센터/그룹/페이지/타입)
  // =========================================================
  // DEFAULT: 시스템 마스터 모드 (특정 센터 미선택)
  const lcId = ref<string>('DEFAULT');
  const eqGroups = ref<TbEqGroupMst[]>([]);
  const selectedEqGroupId = ref<string>(''); // ''이면 그룹 없음
  const pages = ref<TbEcs2dPage[]>([]);
  const activePageId = ref<string>('');
  const equipmentTypes = ref<TbEcs2dItemType[]>([]);

  // =========================================================
  // 2) Editor State (레이아웃 + 선택 + Undo/Redo)
  // =========================================================
  const layouts = ref<TbEcs2dItem[]>([]);
  const selectedObjectId = ref<string | null>(null);
  const selectedObjectIds = ref<string[]>([]);

  const undoStack = ref<HistoryItem[]>([]);
  const redoStack = ref<HistoryItem[]>([]);
  const isRecordingHistory = ref(true);

  // =========================================================
  // 3) Dashboard State (화면이 최종적으로 믿는 데이터)
  // =========================================================
  const dashboardData = ref<DashboardEquipmentData[]>([]);
  // shallowRef: 250ms 주기로 전체 배열 교체가 일어나므로 deep Proxy 오버헤드 불필요
  const dashboardCargos = shallowRef<DashboardCargoData[]>([]);
  const shuttleRtData = shallowRef<DashboardShuttleData[]>([]);
  const alarms = ref<AlarmData[]>([]);
  const activeWcsOrders = ref<TbWcsShuttleOrder[]>([]);
  const wcsOrders = ref<TbWcsShuttleOrder[]>([]); // error orders 등

  // =========================================================
  // 4) Real/Provider Raw State (원본 + Map)
  // =========================================================
  const realEquipments = ref<TbEqMst[]>([]);

  // Provider 기반 - shallowRef: 매 주기 전체 배열 교체, Vue가 내부 객체를 감시할 필요 없음
  const rtShuttles = shallowRef<RtShuttlePosition[]>([]);
  const rtConveyors = shallowRef<RtConveyorStatus[]>([]);
  const rtLifters = shallowRef<RtLifterStatus[]>([]);
  const rtCargos = shallowRef<RtCargoPosition[]>([]);
  const rtJobs = shallowRef<RtJobStatus[]>([]);
  const rtAlarms = shallowRef<RtAlarm[]>([]);

  // 셔틀 위치 비-반응성 버퍼 (markRaw Map → Vue Proxy 완전 차단)
  // WebSocket 수신 → 버퍼에 누적 → shuttleRtData로 한 번에 flush
  const _shuttleBuffer = markRaw(new Map<string, DashboardShuttleData>());

  // =========================================================
  // UI/Connection State
  // =========================================================
  const isLoading = ref(false);
  const isConnected = ref(false);
  const lastError = ref<string | null>(null);

  // connect 중복 호출 방지
  const connecting = ref(false);

  // 현재 구독중인 키(중복 구독/해제 꼬임 방지)
  const subscribedKey = ref<SubKey | null>(null);

  // polling handle
  let wcsOrdersPollingInterval: ReturnType<typeof setInterval> | null = null;

  // =========================================================
  // Computed
  // =========================================================
  const activePage = computed(() => pages.value.find((p) => p.id === activePageId.value));
  const selectedObject = computed(() => layouts.value.find((l) => l.id === selectedObjectId.value));
  const sortedLayouts = computed(() => [...layouts.value].sort((a, b) => a.zIndex - b.zIndex));
  const sortedDashboardData = computed(() =>
    [...dashboardData.value].sort((a, b) => a.zIndex - b.zIndex),
  );

  const equipmentTypeMap = computed(() => {
    const m = new Map<string, TbEcs2dItemType>();
    for (const t of equipmentTypes.value) m.set(t.typeCode, t);
    return m;
  });

  const canUndo = computed(() => undoStack.value.length > 0);
  const canRedo = computed(() => redoStack.value.length > 0);

  /** 시스템 마스터(DEFAULT) 모드 여부 */
  const isDefaultMode = computed(() => lcId.value === 'DEFAULT' || lcId.value === '');

  /**
   * shuttle provider 표준 dto 변환
   * @param s
   */
  function normalizeShuttleDto(s: Partial<RtShuttlePosition>): DashboardShuttleData {
    return {
      equipmentId: s.equipmentId ?? '',
      equipmentCode: s.equipmentCode,
      cellId: s.cellId,

      posX: s.posX ?? 0,
      posY: s.posY ?? 0,

      floor: s.floor,
      posZ: s.floor, // 기존 화면 호환용

      status: s.status,
      statusDesc: s.statusDesc,

      shuttleState: s.shuttleState,

      batteryLevel: s.batteryLevel,
      batteryStatus: s.batteryStatus,

      hasCargo: !!s.hasCargo,

      errorCode: s.errorCode ?? null,
      errorMessage: s.errorMessage ?? null,

      currentJobKey: s.currentJobKey,

      row: s.row,
      bay: s.bay,

      movementStatus: s.movementStatus,
      targetCellId: s.targetCellId,

      hasActiveJob: !!s.hasActiveJob,
      currentOrderKey: s.currentOrderKey,
      currentOrderType: s.currentOrderType,
      currentOrderStatus: s.currentOrderStatus,
      currentBarcode: s.currentBarcode,
      currentFromLoc: s.currentFromLoc,
      currentToLoc: s.currentToLoc,

      autoYn: s.autoYn,
      useYn: s.useYn,

      ts: s.ts,
    };
  }

  /** Dashboard2D가 쓰는 셔틀 위치 모델 */
  const shuttlePositions = computed(() => shuttleRtData.value);

  /**
   * TaskGrid 호환용 - rtJobs(실시간 ECS 데이터) 우선 사용
   * rtJobs가 없으면 activeWcsOrders fallback
   */
  const activeJobs = computed(() => rtJobs.value || []);

  // =========================================================
  // Core Actions
  // =========================================================

  /** 센터만 세팅(모달 등에서 사용) */
  function setLcId(centerId: string) {
    lcId.value = centerId;
  }

  /**
   * 전체 초기화(센터만 알고 있을 때)
   * - pages/types/groups 로드 후
   * - page 선택
   */
  async function initializeLcOnly(centerId: string) {
    lcId.value = centerId;
    isLoading.value = true;
    try {
      await Promise.all([loadEqGroups(), loadEquipmentTypes(), loadPagesByLcId()]);

      // 타입이 없다면 기본 타입 생성 + 아이콘 세팅
      if (equipmentTypes.value.length === 0) {
        await initializeDefaultTypes();
      }

      // 페이지 선택 로직(정렬 후 첫 페이지)
      sortPagesInPlace();
      if (pages.value.length > 0) {
        activePageId.value = pages.value[0].id;
        selectedEqGroupId.value = pages.value[0].eqGroupId || '';
      } else {
        const page = await createPageWithoutEqGroup();
        activePageId.value = page.id;
        selectedEqGroupId.value = '';
      }

      // 에디터 초기화
      layouts.value = [];
      clearSelection();
      clearHistory();
    } finally {
      isLoading.value = false;
    }
  }

  /**
   * 센터 + 설비그룹 기반 초기화
   * - 전체 페이지를 다 로드하고(필터는 UI에서)
   * - 선택 그룹의 첫 페이지로 activePageId를 세팅
   */
  async function initializeWithEqGroup(centerId: string, eqGroupId: string) {
    lcId.value = centerId;
    selectedEqGroupId.value = eqGroupId;
    isLoading.value = true;

    try {
      await Promise.all([loadEqGroups(), loadEquipmentTypes(), loadPagesByLcId()]);

      if (equipmentTypes.value.length === 0) {
        await initializeDefaultTypes();
      }

      sortPagesInPlace();

      // 선택 그룹의 첫 페이지를 선택
      const groupPages = pages.value.filter((p) => (p.eqGroupId || '') === (eqGroupId || ''));
      if (groupPages.length > 0) {
        activePageId.value = groupPages[0].id;
      } else if (pages.value.length > 0) {
        activePageId.value = pages.value[0].id;
      } else {
        const page = await createPageWithoutEqGroup();
        activePageId.value = page.id;
      }

      layouts.value = [];
      clearSelection();
      clearHistory();
    } finally {
      isLoading.value = false;
    }
  }

  /** eqGroup 목록 (센터별) */
  async function loadEqGroups() {
    if (!lcId.value) return;
    try {
      eqGroups.value = await eqGroupApi.getGroupsByLcId(lcId.value);
    } catch (e) {
      console.error('[ShuttleStore] loadEqGroups failed', e);
      eqGroups.value = [];
    }
  }

  /** eqGroup 전체 목록 (센터 무관) */
  async function loadAllEqGroups() {
    try {
      eqGroups.value = await eqGroupApi.getGroupsAll(); // tb_eq_group_mst 전체 조회
    } catch (e) {
      console.error('[ShuttleStore] loadAllEqGroups failed', e);
      eqGroups.value = [];
    }
  }

  /** lcId로 전체 pages 로드 */
  async function loadPagesByLcId() {
    if (!lcId.value) return;
    try {
      pages.value = await layoutPageApi.getPages(lcId.value);
      sortPagesInPlace();
    } catch (e) {
      console.error('[ShuttleStore] loadPagesByLcId failed', e);
      pages.value = [];
    }
  }

  /** 페이지 정렬: floorLevel → pageIndex */
  function sortPagesInPlace() {
    pages.value = [...pages.value].sort((a, b) => {
      const fa = a.floorLevel ?? 0;
      const fb = b.floorLevel ?? 0;
      if (fa !== fb) return fa - fb;
      return (a.pageIndex ?? 0) - (b.pageIndex ?? 0);
    });
  }

  /** 타입 로드 */
  async function loadEquipmentTypes() {
    if (!lcId.value) return;
    equipmentTypes.value = await equipmentTypeApi.getTypes(lcId.value);
  }

  /**
   * 기본 타입 생성 + 아이콘 누락분 보정
   * - backend initializeDefaultTypes 호출 후
   * - iconData2d 비어있으면 프론트 내장 svg로 update
   */
  async function initializeDefaultTypes() {
    if (!lcId.value) return;
    await equipmentTypeApi.initializeDefaultTypes(lcId.value);
    await loadEquipmentTypes();

    const needsUpdate = equipmentTypes.value.filter(
      (t) => (!t.iconData2d || t.iconData2d.trim() === '') && !!defaultTypeIconDataUrl[t.typeCode],
    );

    if (needsUpdate.length > 0) {
      await Promise.all(
        needsUpdate.map((t) =>
          equipmentTypeApi.updateTypeIcon(t.id, defaultTypeIconDataUrl[t.typeCode]),
        ),
      );
      await loadEquipmentTypes();
    }
  }

  /**
   * 로컬 SVG 파일을 읽어 DEFAULT 마스터로 일괄 등록 (제로베이스 초기화)
   * - import.meta.glob으로 assets/icons/*.svg 를 raw 텍스트로 읽음
   * - 파일명에서 typeCode 추출 (rack.svg → RACK, rail_lane.svg → RAIL_LANE)
   * - 기본 Config + SVG 데이터를 묶어 POST /save_batch
   */
  async function initMasterFromLocalAssets() {
    const DEFAULT_CONFIGS: Record<
      string,
      {
        typeName: string;
        category: string;
        layerType: string;
        realEqTypeNum: number | null;
        hasCargo: boolean;
        hasInventory: boolean;
      }
    > = {
      CONVEYOR: {
        typeName: '컨베이어',
        category: '이동설비',
        layerType: 'static',
        realEqTypeNum: 21,
        hasCargo: true,
        hasInventory: false,
      },
      LIFTER: {
        typeName: '리프터',
        category: '이동설비',
        layerType: 'static',
        realEqTypeNum: 21,
        hasCargo: true,
        hasInventory: false,
      },
      BCR: {
        typeName: '바코드리더',
        category: '검증설비',
        layerType: 'static',
        realEqTypeNum: null,
        hasCargo: false,
        hasInventory: false,
      },
      RACK: {
        typeName: '랙',
        category: '보관설비',
        layerType: 'static',
        realEqTypeNum: 11,
        hasCargo: false,
        hasInventory: true,
      },
      SHUTTLE: {
        typeName: '셔틀',
        category: '이동설비',
        layerType: 'dynamic',
        realEqTypeNum: 22,
        hasCargo: true,
        hasInventory: false,
      },
      STV: {
        typeName: 'STV',
        category: '이동설비',
        layerType: 'dynamic',
        realEqTypeNum: 22,
        hasCargo: true,
        hasInventory: false,
      },
      CRANE: {
        typeName: '크레인',
        category: '이동설비',
        layerType: 'dynamic',
        realEqTypeNum: null,
        hasCargo: false,
        hasInventory: false,
      },
      BUFFER: {
        typeName: '버퍼',
        category: '보관설비',
        layerType: 'static',
        realEqTypeNum: null,
        hasCargo: false,
        hasInventory: false,
      },
      WORKSTATION: {
        typeName: '작업대',
        category: '작업설비',
        layerType: 'static',
        realEqTypeNum: null,
        hasCargo: false,
        hasInventory: false,
      },
      GATE: {
        typeName: '게이트',
        category: '기타',
        layerType: 'static',
        realEqTypeNum: null,
        hasCargo: false,
        hasInventory: false,
      },
      PILLAR: {
        typeName: '기둥',
        category: '구조물',
        layerType: 'overlay',
        realEqTypeNum: null,
        hasCargo: false,
        hasInventory: false,
      },
      RAIL_LANE: {
        typeName: '레일',
        category: '구조물',
        layerType: 'overlay',
        realEqTypeNum: null,
        hasCargo: false,
        hasInventory: false,
      },
      CARGO: {
        typeName: '화물',
        category: '화물',
        layerType: 'overlay',
        realEqTypeNum: null,
        hasCargo: true,
        hasInventory: false,
      },
    };

    // Vite import.meta.glob으로 로컬 SVG 파일 raw 텍스트 읽기
    const svgModules = import.meta.glob('../assets/icons/*.svg', {
      as: 'raw',
      eager: true,
    }) as Record<string, string>;

    const typesToSave = Object.entries(svgModules).map(([path, svgContent], index) => {
      // 파일명에서 typeCode 추출: '../assets/icons/rack.svg' → 'RACK'
      const fileName = path.split('/').pop() ?? '';
      const typeCode = fileName.replace('.svg', '').toUpperCase();
      const config = DEFAULT_CONFIGS[typeCode] ?? {
        typeName: typeCode,
        category: '기타',
        layerType: 'static',
        realEqTypeNum: null,
        hasCargo: false,
        hasInventory: false,
      };

      return {
        typeCode,
        typeName: config.typeName,
        category: config.category,
        layerType: config.layerType,
        realEqTypeNum: config.realEqTypeNum,
        hasCargo: config.hasCargo,
        hasInventory: config.hasInventory,
        iconData2d: svgContent,
        iconFileName: fileName,
        defaultWidth: 100,
        defaultHeight: 100,
        rotatable: true,
        resizable: true,
        showStatus: false,
        showPopup: true,
        sortOrder: index,
        isActive: true,
      };
    });

    isLoading.value = true;
    try {
      await equipmentTypeApi.saveBatch('DEFAULT', typesToSave);
      await loadEquipmentTypes();
    } finally {
      isLoading.value = false;
    }
  }

  /**
   * DEFAULT 마스터 설비 타입을 현재 센터로 복제(동기화)
   */
  async function syncMasterToCenter(targetLcId?: string) {
    const target = targetLcId || lcId.value;
    if (!target || target === 'DEFAULT') throw new Error('복제 대상 센터 ID가 필요합니다.');

    isLoading.value = true;
    try {
      await equipmentTypeApi.cloneFromDefault(target);
      if (target === lcId.value) {
        await loadEquipmentTypes();
      }
    } finally {
      isLoading.value = false;
    }
  }

  function normalizeBool(v: any): boolean | null {
    if (v == null) return null;
    if (typeof v === 'boolean') return v;
    if (typeof v === 'number') return v !== 0;
    const s = String(v).toLowerCase();
    if (s === 'true' || s === 'y' || s === 'yes' || s === '1') return true;
    if (s === 'false' || s === 'n' || s === 'no' || s === '0') return false;
    return null;
  }

  function normalizeNumber(v: any): number | null {
    if (v == null || v === '') return null;
    const n = Number(v);
    return Number.isNaN(n) ? null : n;
  }

  function normalizeEquipmentRealtime(base: any, rt: any) {
    if (!base && !rt) return null;

    return {
      ...(base || {}),
      ...(rt || {}),

      // 공통 표시 필드 보정
      status: rt?.status ?? base?.status ?? base?.currentStatus ?? null,
      runYn: normalizeBool(rt?.runYn ?? rt?.moving),
      autoYn: normalizeBool(rt?.autoYn),
      cargoYn: normalizeBool(rt?.cargoYn),
      hasCargo: normalizeBool(rt?.hasCargo ?? rt?.cargoYn),
      hasShuttle: normalizeBool(rt?.hasShuttle),
      stopperOpenYn: normalizeBool(rt?.stopperOpenYn ?? rt?.stopperOpen),
      stopperOpen: normalizeBool(rt?.stopperOpen ?? rt?.stopperOpenYn),

      currentLevel: normalizeNumber(rt?.currentLevel),
      targetLevel: normalizeNumber(rt?.targetLevel),

      hasActiveJob: normalizeBool(rt?.hasActiveJob),
      currentOrderKey: rt?.currentOrderKey ?? null,
      currentOrderType: rt?.currentOrderType ?? null,
      currentOrderStatus: rt?.currentOrderStatus ?? null,
      currentBarcode: rt?.currentBarcode ?? null,
      currentFromLoc: rt?.currentFromLoc ?? null,
      currentToLoc: rt?.currentToLoc ?? null,

      errorCode: rt?.errorCode ?? rt?.errorId ?? null,
      errorMessage: rt?.errorMessage ?? rt?.errorDesc ?? null,
    };
  }

  function getMergedEquipmentState(equipment: any) {
    if (!equipment) return null;

    const base = equipment;
    const layoutId = String(base.id ?? '');
    const realEqId = String(base.realEqId ?? '');
    const eqType = String(base.equipmentTypeCode ?? base.realEqType ?? '').toUpperCase();

    // 1) LIFTER
    if (isLifterType(eqType)) {
      // ✨ 수정: this.rtLifters -> rtLifters.value
      const rt = (rtLifters.value || []).find((lf: any) => {
        const lfLayoutId = String(lf?.layoutId ?? '');
        const lfEqId = String(lf?.eqId ?? '');
        const lfEquipmentId = String(lf?.equipmentId ?? '');

        return lfLayoutId === layoutId || lfEqId === realEqId || lfEquipmentId === realEqId;
      });

      return normalizeEquipmentRealtime(base, rt);
    }

    // 2) CONVEYOR
    if (isConveyorType(eqType)) {
      // ✨ 수정: this.rtConveyors -> rtConveyors.value
      const rt = (rtConveyors.value || []).find((cv: any) => {
        const cvEqId = String(cv?.eqId ?? '');
        const cvEquipmentId = String(cv?.equipmentId ?? '');
        return cvEqId === realEqId || cvEquipmentId === realEqId;
      });

      return normalizeEquipmentRealtime(base, rt ?? null);
    }

    // 3) SHUTTLE
    if (isShuttleType(eqType)) {
      // ✨ 수정: this.shuttlePositions -> shuttlePositions.value
      const rt = (shuttlePositions.value || []).find((sh: any) => {
        const shEqId = String(sh?.eqId ?? '');
        const shEquipmentId = String(sh?.equipmentId ?? '');
        return shEqId === realEqId || shEquipmentId === realEqId;
      });

      return normalizeEquipmentRealtime(base, rt);
    }

    // 4) RACK / 그 외
    return normalizeEquipmentRealtime(base, null);
  }

  // =========================================================
  // Page Actions
  // =========================================================

  async function createPage(page: Partial<TbEcs2dPage>) {
    if (!lcId.value) throw new Error('lcId is empty');
    page.lcId = lcId.value;
    const created = await layoutPageApi.createPage(page);
    pages.value.push(created);
    sortPagesInPlace();
    return created;
  }

  /** eqGroup 없이 새 floor 자동 생성 */
  async function createPageWithoutEqGroup() {
    const maxFloor = pages.value.reduce((max, p) => Math.max(max, p.floorLevel ?? 0), 0);
    const nextFloor = maxFloor > 0 ? maxFloor + 1 : 1;

    return createPage({
      floorLevel: nextFloor,
      pageName: `${nextFloor}층`,
      canvasWidth: 1920,
      canvasHeight: 1080,
      backgroundColor: '#FFFFFF',
      isActive: true,
    });
  }

  async function copyPage(sourcePageId: string, newPageName: string, newFloorLevel?: number) {
    const created = await layoutPageApi.copyPage(sourcePageId, newPageName, newFloorLevel);
    pages.value.push(created);
    sortPagesInPlace();
    return created;
  }

  async function updatePageName(id: string, pageName: string) {
    const updated = await layoutPageApi.updatePageName(id, pageName);
    const idx = pages.value.findIndex((p) => p.id === id);
    if (idx !== -1) pages.value[idx] = updated;
    sortPagesInPlace();
  }

  async function updatePageCanvas(
    id: string,
    canvasWidth: number,
    canvasHeight: number,
    backgroundColor: string,
  ) {
    const updated = await layoutPageApi.updatePageCanvas(
      id,
      canvasWidth,
      canvasHeight,
      backgroundColor,
    );
    const idx = pages.value.findIndex((p) => p.id === id);
    if (idx !== -1) pages.value[idx] = updated;
  }

  async function updatePageEqGroup(pageId: string, eqGroupId: string) {
    const updated = await layoutPageApi.updatePageEqGroup(pageId, eqGroupId);
    const idx = pages.value.findIndex((p) => p.id === pageId);
    if (idx !== -1) pages.value[idx] = updated;
  }

  async function clearPageEqGroup(pageId: string) {
    const updated = await layoutPageApi.clearPageEqGroup(pageId);
    const idx = pages.value.findIndex((p) => p.id === pageId);
    if (idx !== -1) pages.value[idx] = updated;
  }

  async function deletePage(id: string) {
    await layoutPageApi.deletePage(id);
    pages.value = pages.value.filter((p) => p.id !== id);

    if (activePageId.value === id) {
      sortPagesInPlace();
      activePageId.value = pages.value[0]?.id || '';
    }
  }

  function selectPage(pageId: string) {
    activePageId.value = pageId;

    // 페이지가 바뀌면 에디터 레이아웃은 다시 로드하는 흐름이 일반적
    layouts.value = [];
    clearSelection();
  }

  /** 그룹 선택(페이지 로드는 이미 되어있고, UI 필터만) */
  function selectEqGroup(eqGroupIdOrEmpty: string) {
    selectedEqGroupId.value = eqGroupIdOrEmpty;

    // 해당 그룹의 첫 페이지 자동 선택
    const groupPages = pages.value.filter((p) => (p.eqGroupId || '') === (eqGroupIdOrEmpty || ''));
    if (groupPages.length > 0) activePageId.value = groupPages[0].id;

    layouts.value = [];
    clearSelection();
  }

  // shuttleStore.ts 내부 Page Actions 섹션에 추가

  /** 현재 선택된 그룹 정보를 포함하여 새 층을 자동 생성 */
  async function createFloorAuto() {
    if (!lcId.value) throw new Error('lcId가 설정되지 않았습니다.');

    // 현재 페이지들 중 가장 높은 층수 찾기
    const maxFloor = pages.value.reduce((max, p) => Math.max(max, p.floorLevel ?? 0), 0);
    const nextFloor = maxFloor + 1;

    // 새 페이지 객체 생성
    const newPage: Partial<TbEcs2dPage> = {
      lcId: lcId.value,
      eqGroupId: selectedEqGroupId.value || null, // 현재 선택된 그룹 유지
      floorLevel: nextFloor,
      pageName: `${nextFloor}층`,
      canvasWidth: 1920,
      canvasHeight: 1080,
      backgroundColor: '#FFFFFF',
      isActive: true,
    };

    try {
      const created = await layoutPageApi.createPage(newPage);
      pages.value.push(created);
      sortPagesInPlace();
      return created;
    } catch (e) {
      console.error('[ShuttleStore] createFloorAuto failed', e);
      throw e;
    }
  }

  // =========================================================
  // Editor Actions (Layouts + History)
  // =========================================================

  async function loadLayouts() {
    if (!lcId.value || !activePageId.value) return;
    isLoading.value = true;
    try {
      layouts.value = await equipmentLayoutApi.getLayouts(lcId.value, activePageId.value);
      clearHistory();
      lastError.value = null;
    } catch (e: any) {
      console.error('[ShuttleStore] loadLayouts failed', e);
      lastError.value = e?.message || 'Failed to load layouts';
      throw e;
    } finally {
      isLoading.value = false;
    }
  }

  async function saveAllLayouts() {
    if (!lcId.value || !activePageId.value) return;
    isLoading.value = true;
    try {
      await equipmentLayoutApi.saveAllLayouts(lcId.value, activePageId.value, layouts.value);
      lastError.value = null;
    } catch (e: any) {
      console.error('[ShuttleStore] saveAllLayouts failed', e);
      lastError.value = e?.message || 'Failed to save layouts';
      throw e;
    } finally {
      isLoading.value = false;
    }
  }

  function setSelection(ids: string[], primary: string | null) {
    selectedObjectIds.value = ids;
    selectedObjectId.value = primary;
  }

  function clearSelection() {
    selectedObjectIds.value = [];
    selectedObjectId.value = null;
  }

  function selectObject(id: string | null) {
    if (!id) {
      clearSelection();
      return;
    }
    setSelection([id], id);
  }

  function recordHistory(item: Omit<HistoryItem, 'id' | 'timestamp'>) {
    if (!isRecordingHistory.value) return;

    const historyItem: HistoryItem = {
      ...item,
      id: `history_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
      timestamp: Date.now(),
    };

    undoStack.value.push(historyItem);

    if (undoStack.value.length > MAX_HISTORY_SIZE) undoStack.value.shift();
    redoStack.value = [];
  }

  function clearHistory() {
    undoStack.value = [];
    redoStack.value = [];
  }

  function pauseHistoryRecording() {
    isRecordingHistory.value = false;
  }

  function resumeHistoryRecording() {
    isRecordingHistory.value = true;
  }

  function addLayout(layout: TbEcs2dItem) {
    recordHistory({
      type: 'add',
      after: { ...layout },
      description: `Add ${layout.equipmentCode || 'object'}`,
    });
    layouts.value.push(layout);
  }

  function updateLayout(id: string, updates: Partial<TbEcs2dItem>) {
    const idx = layouts.value.findIndex((l) => l.id === id);
    if (idx === -1) return;

    const before = { ...layouts.value[idx] };
    const after = { ...before, ...updates };

    recordHistory({
      type: 'update',
      before,
      after,
      description: `Update ${before.equipmentCode || 'object'}`,
    });

    layouts.value[idx] = after;
  }

  /** history 없이 조용히 업데이트(실시간 반영 등) */
  function updateLayoutSilent(id: string, updates: Partial<TbEcs2dItem>) {
    const idx = layouts.value.findIndex((l) => l.id === id);
    if (idx === -1) return;
    layouts.value[idx] = { ...layouts.value[idx], ...updates };
  }

  function removeLayouts(ids: string[]) {
    const uniqueIds = Array.from(new Set(ids)).filter(Boolean);
    if (uniqueIds.length === 0) return;

    const before = [...layouts.value];
    const after = layouts.value.filter((l) => !uniqueIds.includes(l.id!));
    if (before.length === after.length) return;

    recordHistory({
      type: 'batch',
      before,
      after,
      description: `Remove ${before.length - after.length} object(s)`,
    });

    layouts.value = after;

    if (selectedObjectId.value && uniqueIds.includes(selectedObjectId.value)) {
      selectedObjectId.value = null;
    }
    selectedObjectIds.value = selectedObjectIds.value.filter((x) => !uniqueIds.includes(x));
  }

  function removeLayout(id: string) {
    removeLayouts([id]);
  }

  function clearLayouts() {
    if (layouts.value.length > 0) {
      recordHistory({
        type: 'batch',
        before: [...layouts.value],
        after: [],
        description: 'Clear all objects',
      });
    }
    layouts.value = [];
    clearSelection();
  }

  function undo() {
    if (!canUndo.value) return;

    const item = undoStack.value.pop()!;
    pauseHistoryRecording();
    try {
      switch (item.type) {
        case 'add': {
          const layout = item.after as TbEcs2dItem | undefined;
          if (layout?.id) layouts.value = layouts.value.filter((l) => l.id !== layout.id);
          break;
        }
        case 'remove': {
          const layout = item.before as TbEcs2dItem | undefined;
          if (layout) layouts.value.push(layout);
          break;
        }
        case 'update': {
          const layout = item.before as TbEcs2dItem | undefined;
          if (layout?.id) {
            const idx = layouts.value.findIndex((l) => l.id === layout.id);
            if (idx !== -1) layouts.value[idx] = { ...layout };
          }
          break;
        }
        case 'batch': {
          if (Array.isArray(item.before)) layouts.value = [...(item.before as TbEcs2dItem[])];
          break;
        }
      }
      redoStack.value.push(item);
    } finally {
      resumeHistoryRecording();
    }
  }

  function redo() {
    if (!canRedo.value) return;

    const item = redoStack.value.pop()!;
    pauseHistoryRecording();
    try {
      switch (item.type) {
        case 'add': {
          const layout = item.after as TbEcs2dItem | undefined;
          if (layout) layouts.value.push(layout);
          break;
        }
        case 'remove': {
          const layout = item.before as TbEcs2dItem | undefined;
          if (layout?.id) layouts.value = layouts.value.filter((l) => l.id !== layout.id);
          break;
        }
        case 'update': {
          const layout = item.after as TbEcs2dItem | undefined;
          if (layout?.id) {
            const idx = layouts.value.findIndex((l) => l.id === layout.id);
            if (idx !== -1) layouts.value[idx] = { ...layout };
          }
          break;
        }
        case 'batch': {
          if (Array.isArray(item.after)) layouts.value = [...(item.after as TbEcs2dItem[])];
          break;
        }
      }
      undoStack.value.push(item);
    } finally {
      resumeHistoryRecording();
    }
  }

  // =========================================================
  // Mapping (Real equipment mapping)
  // =========================================================

  async function loadRealEquipmentsByEqGroupIdAndEqType(eqGroupId: string, eqType: number) {
    try {
      realEquipments.value = await realEqMstApi.getEquipmentsByEqGroupIdAndEqType(
        eqGroupId,
        eqType,
      );
    } catch (e) {
      console.error('[ShuttleStore] loadRealEquipmentsByType failed', e);
      realEquipments.value = [];
    }
  }

  async function loadRealEquipmentsByGroup(eqGroupId: string) {
    try {
      realEquipments.value = await realEqMstApi.getEquipmentsByGroup(eqGroupId);
    } catch (e) {
      console.error('[ShuttleStore] loadRealEquipmentsByGroup failed', e);
      realEquipments.value = [];
    }
  }

  async function updateRealEqMapping(layoutId: string, realEqId: string, realEqType: string) {
    const updated = await equipmentLayoutApi.updateRealEqMapping(layoutId, realEqId, realEqType);
    const idx = layouts.value.findIndex((l) => l.id === layoutId);
    if (idx !== -1) layouts.value[idx] = { ...layouts.value[idx], realEqId, realEqType };
    return updated;
  }

  async function clearRealEqMapping(layoutId: string) {
    const updated = await equipmentLayoutApi.clearRealEqMapping(layoutId);
    const idx = layouts.value.findIndex((l) => l.id === layoutId);
    if (idx !== -1)
      layouts.value[idx] = { ...layouts.value[idx], realEqId: undefined, realEqType: undefined };
    return updated;
  }

  // =========================================================
  // Dashboard (REST 초기 로드)
  // =========================================================

  async function loadDashboardData() {
    if (!lcId.value || !activePageId.value) return;
    try {
      const data = await equipmentLayoutApi.getLayoutsWithRealStatus(
        lcId.value,
        activePageId.value,
      );
      dashboardData.value = data as unknown as DashboardEquipmentData[];
    } catch (e) {
      console.error('[ShuttleStore] loadDashboardData failed', e);
      dashboardData.value = [];
    }
  }

  /**
   * WebSocket 연결 전/후에 화면을 "바로" 채우기 위한 초기 로드
   * - 서버가 제공하는 initialData를 신뢰한다.
   */
  async function loadDashboardInitialData() {
    if (!lcId.value || !activePageId.value) return;

    try {
      const result = await realTimeApi.getDashboardInitialData(lcId.value, activePageId.value);

      // shuttles
      if (Array.isArray(result?.shuttles)) {
        shuttleRtData.value = result.shuttles.map((s: any) => normalizeShuttleDto(s));
      }

      // cargos
      if (Array.isArray(result?.cargos)) {
        dashboardCargos.value = result.cargos.map((c: any) => ({
          cargoId: c.cargoId,
          barcode: c.barcode,
          posX: c.posX,
          posY: c.posY,
          cargoStatus: c.cargoStatus,
          carriedByShuttleId: c.carriedByShuttleId,
          storedCellId: c.storedCellId,
          floor: c.floor,
        }));
      }

      // orders
      if (Array.isArray(result?.orders)) {
        activeWcsOrders.value = result.orders;
      }
    } catch (e) {
      console.error('[ShuttleStore] loadDashboardInitialData failed', e);
    }
  }

  async function loadLayoutsWithRealStatus(): Promise<LayoutWithRealStatus[]> {
    if (!lcId.value || !activePageId.value) return [];
    try {
      return await equipmentLayoutApi.getLayoutsWithRealStatus(lcId.value, activePageId.value);
    } catch (e) {
      console.error('[ShuttleStore] loadLayoutsWithRealStatus failed', e);
      return [];
    }
  }

  // =========================================================
  // WCS Orders (Polling)
  // =========================================================

  async function loadActiveJobs() {
    try {
      // ECS 실시간 Job은 WebSocket 구독 데이터(rtJobs)를 사용하므로
      // REST로 강제 새로고침 가능한 건 현재 WCS active orders 쪽이다.
      await loadActiveWcsOrdersForce();
    } catch (e) {
      console.error('[ShuttleStore] loadActiveJobs failed', e);
    }
  }

  async function loadActiveWcsOrdersForce() {
    try {
      const orders = await realTimeApi.getActiveOrders();
      if (Array.isArray(orders)) {
        // 화면 깜빡임 줄이기: in-place merge
        mergeArrayInPlace(activeWcsOrders.value, orders, (o) => o.orderKey);
      }
    } catch (e) {
      console.error('[ShuttleStore] loadActiveWcsOrdersForce failed', e);
    }
  }

  async function loadActiveWcsOrders() {
    return loadActiveWcsOrdersForce();
  }

  function startWcsOrdersPolling() {
    stopWcsOrdersPolling();

    wcsOrdersPollingInterval = setInterval(async () => {
      try {
        const orders = await realTimeApi.getActiveOrders();
        if (Array.isArray(orders)) {
          mergeArrayInPlace(activeWcsOrders.value, orders, (o) => o.orderKey);
        }
      } catch (e) {
        // 실패해도 기존 데이터 유지
        console.error('[ShuttleStore] WCS orders polling failed', e);
      }
    }, WCS_ORDERS_POLLING_INTERVAL);
  }

  function stopWcsOrdersPolling() {
    if (wcsOrdersPollingInterval) {
      clearInterval(wcsOrdersPollingInterval);
      wcsOrdersPollingInterval = null;
    }
  }

  async function loadErrorWcsOrders() {
    try {
      wcsOrders.value = await realTimeApi.getErrorOrders();
    } catch (e) {
      console.error('[ShuttleStore] loadErrorWcsOrders failed', e);
      wcsOrders.value = [];
    }
  }

  async function cancelWcsOrder(orderKey: string, reason?: string) {
    const result = await realTimeApi.cancelOrder(orderKey, reason);
    if (result?.success) await loadActiveWcsOrdersForce();
    return result;
  }

  async function resumeWcsOrder(orderKey: string) {
    const result = await realTimeApi.resumeOrder(orderKey);
    if (result?.success) {
      await loadActiveWcsOrdersForce();
      await loadErrorWcsOrders();
    }
    return result;
  }

  // =========================================================
  // Realtime (WebSocket)
  // =========================================================

  /**
   * connect():
   * - 1) REST initial load로 화면을 채운다
   * - 2) active orders를 로드하고 polling을 시작한다
   * - 3) websocket을 연결하고 구독을 설정한다
   *
   * 멀티 유저/멀티 탭:
   * - 각 브라우저 탭은 store 인스턴스가 독립이다.
   * - 서버에 "브로드캐스트 시작" 요청이 있다면 서버가 멱등 처리해야 안전하다.
   */
  async function connectWebSocket() {
    if (connecting.value) return; // 중복 호출 방지
    if (!lcId.value || !activePageId.value) return;

    connecting.value = true;
    try {
      // 1) 화면 즉시 채우기
      await loadDashboardInitialData();

      // 2) 오더 로드 + 폴링
      await loadActiveWcsOrdersForce();
      startWcsOrdersPolling();

      // 3) WS 연결
      const client = getWebSocketClient();
      if (client.isConnected()) {
        isConnected.value = true;
        setupSubscriptions();
        return;
      }

      await client.connect({
        onConnect: () => {
          isConnected.value = true;
          lastError.value = null;
          setupSubscriptions();
        },
        onDisconnect: () => {
          isConnected.value = false;
        },
        onError: (err: WebSocketError) => {
          isConnected.value = false;
          lastError.value = err?.message || 'WebSocket error';
          console.error('[ShuttleStore] ws error', err);
        },
        onReconnecting: (attempt: number, maxAttempts: number) => {
          lastError.value = `Reconnecting... (${attempt}/${maxAttempts})`;
        },
      });

      isConnected.value = true;
    } catch (e: any) {
      console.error('[ShuttleStore] connectWebSocket failed', e);
      lastError.value = e?.message || 'WebSocket connection failed';
      isConnected.value = false;
    } finally {
      connecting.value = false;
    }
  }

  /**
   * 구독 설정은 "현재 키" 기준으로 딱 한 번만
   * - 키가 변경되면 teardown 후 재구독
   */
  function setupSubscriptions() {
    const client = getWebSocketClient();
    if (!client.isConnected()) return;
    if (!lcId.value || !activePageId.value) return;

    const key = makeSubKey(lcId.value, selectedEqGroupId.value, activePageId.value);
    if (sameSubKey(subscribedKey.value, key)) return;

    // 기존 구독 해제(있다면)
    teardownSubscriptions();

    // ---- Legacy dashboard 구독 (기존 토픽) ----
    client.subscribeDashboard(key.lcId, key.pageId, (data: DashboardEquipmentData[]) => {
      dashboardData.value = data;
    });
    client.sendDashboardSubscribe(key.lcId, key.pageId);

    client.subscribeEquipmentChanges(key.lcId, (data: any) => {
      const idx = dashboardData.value.findIndex((e) => e.id === data.equipmentId);
      if (idx !== -1) {
        dashboardData.value[idx] = {
          ...dashboardData.value[idx],
          currentStatus: data.status,
          currentTaskId: data.taskId,
          lastStatusTime: data.timestamp,
        };
      }
    });

    client.subscribeEquipmentPosition(key.lcId, (data: EquipmentPositionEvent) => {
      const idx = dashboardData.value.findIndex((e) => e.equipmentId === data.equipmentId);
      if (idx !== -1) {
        dashboardData.value[idx] = {
          ...dashboardData.value[idx],
          currentPointCode: data.pointCode,
          currentPosX: data.posX,
          currentPosY: data.posY,
          movementStatus: data.movementStatus,
        };
      }
    });

    client.subscribeEquipmentMove(key.lcId, (data: EquipmentMoveEvent) => {
      const idx = dashboardData.value.findIndex((e) => e.equipmentId === data.equipmentId);
      if (idx !== -1) {
        dashboardData.value[idx] = {
          ...dashboardData.value[idx],
          targetPointCode: data.toPointCode,
          movementStatus: data.movementStatus,
        };
      }
    });

    client.subscribeBcrScan(key.lcId, (data: BcrScanEvent) => {
      const idx = dashboardData.value.findIndex((e) => e.equipmentId === data.equipmentId);
      if (idx !== -1) {
        dashboardData.value[idx] = {
          ...dashboardData.value[idx],
          cargoBarcode: data.barcode,
        };
      }
    });

    client.subscribeAlarms(key.lcId, (data: AlarmData) => {
      alarms.value = [data, ...alarms.value].slice(0, 100);
    });

    client.subscribeErrors(key.lcId, (err: any) => {
      lastError.value = err?.message || err?.errorMessage || 'Server error';
      console.error('[ShuttleStore] server error', err);
    });

    client.subscribeShuttlePositions(key.lcId, (data: any[]) => {
      if (!Array.isArray(data)) return;
      shuttleRtData.value = data.map((s: any) => normalizeShuttleDto(s));
    });

    client.subscribeCargoPositions(key.lcId, (data: any[]) => {
      if (!Array.isArray(data)) return;
      dashboardCargos.value = data.map((c: any) => ({
        cargoId: c.cargoId,
        barcode: c.barcode,
        posX: c.posX,
        posY: c.posY,
        cargoStatus: c.cargoStatus,
        carriedByShuttleId: c.carriedByShuttleId,
        storedCellId: c.storedCellId,
        floor: c.floor,
      }));
    });

    // ---- Provider 기반 RT 구독(새 토픽) ----
    // Topic Pattern: /topic/realtime/{type}/{lcId}/{eqGroupId}/{pageId}
    // 서버에 브로드캐스트 시작 요청이 "세션당 1회"만 되도록 서버가 멱등 처리하는 게 중요.
    const eqGrp = key.eqGroupId || 'default';
    client.requestRtBroadcastStart(eqGrp, key.lcId, key.pageId || 'default');

    client.subscribeShuttleRt(key.lcId, eqGrp, key.pageId, (data: RtShuttlePosition[]) => {
      if (!Array.isArray(data)) return;
      // shallowRef: 참조만 교체 → Vue deep Proxy 없음
      rtShuttles.value = data;

      // 비-반응성 버퍼에 in-place 갱신 후 shallowRef로 한 번에 flush
      // → normalizeShuttleDto 결과 객체 재사용으로 GC 압력 감소
      for (const s of data) {
        _shuttleBuffer.set(s.equipmentId, normalizeShuttleDto(s));
      }
      // 사라진 셔틀 제거
      if (_shuttleBuffer.size !== data.length) {
        const incoming = new Set(data.map((s) => s.equipmentId));
        for (const id of _shuttleBuffer.keys()) {
          if (!incoming.has(id)) _shuttleBuffer.delete(id);
        }
      }
      shuttleRtData.value = Array.from(_shuttleBuffer.values());
    });

    client.subscribeConveyorRt(key.lcId, eqGrp, key.pageId, (data: RtConveyorStatus[]) => {
      if (!Array.isArray(data)) return;
      rtConveyors.value = data;
    });

    client.subscribeLifterRt(key.lcId, eqGrp, key.pageId, (data: RtLifterStatus[]) => {
      if (!Array.isArray(data)) return;
      rtLifters.value = data;
    });

    client.subscribeCargoRt(key.lcId, eqGrp, key.pageId, (data: RtCargoPosition[]) => {
      if (!Array.isArray(data)) return;
      rtCargos.value = data;

      // 최종 화면 상태로 수렴 (pageId별 필터링 위해 conveyorId 포함 가능)
      dashboardCargos.value = data.map((c) => ({
        cargoId: c.cargoId,
        barcode: c.barcode,
        posX: c.posX,
        posY: c.posY,
        cargoStatus: c.status,
        carriedByShuttleId: c.carriedByShuttleId,
        storedCellId: c.cellId,
        conveyorId: c.conveyorId,
        floor: c.floor,
      }));
    });

    // Job과 Alarm은 pageId 필터링 없음 (전체 현황)
    client.subscribeJobRt(key.lcId, eqGrp, (data: RtJobStatus[]) => {
      if (data === null || data === undefined) {
        rtJobs.value = [];
        return;
      }
      console.log(`subscribeJobRt: ${ JSON.stringify(data.length,null,2)} jobs`);
      rtJobs.value = Array.isArray(data) ? data : [];
    });

    client.subscribeAlarmRt(key.lcId, eqGrp, (data: RtAlarm[]) => {
      if (!Array.isArray(data)) return;
      rtAlarms.value = data;

      // 최종 화면 알람 상태로 수렴
      alarms.value = data.map((a) => ({
        id: a.alarmId,
        lcId: key.lcId,
        level: a.severity?.toString(),
        message: a.errorMessage || a.errorCode || '',
        timestamp: a.occurredAt ? toIso(a.occurredAt) : toIso(),
        equipmentId: a.equipmentId,
        equipmentCode: a.equipmentCode,
      }));
    });

    // 랙 재고/상태 실시간 구독 (250ms)
    //   - payload 는 CellStateService 기반 (stateCode / locked / inbound_forbidden / outbound_forbidden ...)
    client.subscribeRackInventoryRt(key.lcId, eqGrp, key.pageId, (data: any[]) => {
      if (!Array.isArray(data)) return;

      const inventoryMap = new Map<string, any>();
      for (const inv of data) {
        if (inv.layoutId) inventoryMap.set(inv.layoutId, inv);
      }

      dashboardData.value = dashboardData.value.map((item) => {
        const inv = inventoryMap.get(item.id);
        if (inv && item.equipmentTypeCode === LayoutEquipmentType.RACK.code) {
          return {
            ...item,
            realRackStateCode: inv.stateCode ?? null,
            realRackStockType: inv.stockType ?? null,
            realRackExpiredDatetime: inv.expiredDatetime ?? null,
            realRackLocked: !!inv.locked,
            realRackInboundForbidden: !!inv.inboundForbidden,
            realRackOutboundForbidden: !!inv.outboundForbidden,
            realRackDriveOnlyYn: inv.driveOnlyYn ?? item.realRackDriveOnlyYn ?? false,
            realRackItemCode: inv.itemCode ?? null,
            realRackLotNo: inv.lotNo ?? null,
            realRackStockId: inv.stockId ?? null,
            realRackTaskId: inv.taskId ?? null,
            realRackActiveOrderType: inv.activeOrderType ?? null,
            realRackStorLoc: inv.storLoc ?? null,
          };
        }
        return item;
      });
    });

    subscribedKey.value = key;
  }

  /** 구독 해제(한 곳에서만) */
  function teardownSubscriptions() {
    const client = getWebSocketClient();
    const key = subscribedKey.value;
    if (!key) return;

    try {
      if (client.isConnected()) {
        // legacy dashboard unsubscribe
        client.unsubscribeDashboard(key.lcId, key.pageId);
        client.sendDashboardUnsubscribe(key.lcId, key.pageId);

        // provider teardown (서버 브로드캐스트 stop + client unsubscribe)
        // 파라미터 순서: lcId, eqGroupId, pageId
        client.teardownRtSubscriptions(key.lcId, key.eqGroupId || 'default', key.pageId);

        // 랙 재고 구독 해제
        client.unsubscribeRackInventoryRt(key.lcId, key.eqGroupId || 'default', key.pageId);
      }
    } catch (e) {
      console.warn('[ShuttleStore] teardownSubscriptions failed (ignored)', e);
    } finally {
      subscribedKey.value = null;
    }
  }

  /** 연결 해제 */
  function disconnectWebSocket() {
    stopWcsOrdersPolling();
    teardownSubscriptions();
    destroyWebSocketClient();
    isConnected.value = false;
  }

  /**
   * 페이지 전환:
   * - activePageId 바꾸고
   * - REST initial 다시 로드해서 화면을 채우고
   * - WS는 setupSubscriptions가 알아서 재구독 처리
   */
  async function switchDashboardPage(pageId: string) {
    activePageId.value = pageId;

    // 화면 초기화(원하면 유지할 수도 있는데, 깔끔한게 안전)
    dashboardData.value = [];
    dashboardCargos.value = [];

    await loadDashboardInitialData();
    await loadActiveWcsOrdersForce();

    const client = getWebSocketClient();
    if (client.isConnected()) setupSubscriptions();
  }

  // =========================================================
  // Misc
  // =========================================================

  /**
   * store reset (로그아웃/센터 변경 등)
   * - 모든 연결/폴링을 끊고 상태를 초기화한다.
   */
  function reset() {
    disconnectWebSocket();

    lcId.value = '';
    selectedEqGroupId.value = '';

    eqGroups.value = [];
    pages.value = [];
    activePageId.value = '';
    equipmentTypes.value = [];

    layouts.value = [];
    clearSelection();
    clearHistory();

    dashboardData.value = [];
    dashboardCargos.value = [];
    shuttleRtData.value = [];
    alarms.value = [];
    activeWcsOrders.value = [];
    wcsOrders.value = [];

    realEquipments.value = [];

    rtShuttles.value = [];
    rtConveyors.value = [];
    rtLifters.value = [];
    rtCargos.value = [];
    rtJobs.value = [];
    rtAlarms.value = [];
    _shuttleBuffer.clear();

    isLoading.value = false;
    isConnected.value = false;
    lastError.value = null;
  }

  // =========================================================
  // Return API (컴포넌트에서 쓰는 것만 노출)
  // =========================================================
  return {
    // Core state
    lcId,
    eqGroups,
    selectedEqGroupId,
    pages,
    activePageId,
    equipmentTypes,

    // Editor state
    layouts,
    selectedObjectId,
    selectedObjectIds,
    canUndo,
    canRedo,

    // Dashboard state
    dashboardData,
    dashboardCargos,
    shuttleRtData,
    alarms,
    activeWcsOrders,
    wcsOrders,

    // Real/provider state (필요 시만 사용)
    realEquipments,

    rtShuttles,
    rtConveyors,
    rtLifters,
    rtCargos,
    rtJobs,
    rtAlarms,

    // UI state
    isLoading,
    isConnected,
    lastError,

    // Computed
    activePage,
    selectedObject,
    sortedLayouts,
    sortedDashboardData,
    equipmentTypeMap,
    shuttlePositions,
    activeJobs,
    isDefaultMode,

    getMergedEquipmentState,

    // Core actions
    setLcId,
    initializeLcOnly,
    initializeWithEqGroup,
    loadEqGroups,
    loadAllEqGroups,
    loadPagesByLcId,
    loadEquipmentTypes,
    initializeDefaultTypes,
    initMasterFromLocalAssets,
    syncMasterToCenter,

    // Page actions
    createPage,
    createPageWithoutEqGroup,
    copyPage,
    updatePageName,
    updatePageCanvas,
    updatePageEqGroup,
    clearPageEqGroup,
    deletePage,
    selectPage,
    selectEqGroup,
    createFloorAuto,

    // Editor actions
    loadLayouts,
    saveAllLayouts,
    setSelection,
    clearSelection,
    selectObject,

    addLayout,
    updateLayout,
    updateLayoutSilent,
    removeLayout,
    removeLayouts,
    clearLayouts,
    undo,
    redo,
    clearHistory,

    // Mapping actions
    loadRealEquipmentsByEqGroupIdAndEqType,
    loadRealEquipmentsByGroup,
    updateRealEqMapping,
    clearRealEqMapping,

    // Dashboard actions
    loadDashboardData,
    loadDashboardInitialData,
    loadLayoutsWithRealStatus,

    // Realtime actions
    connectWebSocket,
    disconnectWebSocket,
    switchDashboardPage,

    // Orders actions
    loadActiveJobs,
    loadActiveWcsOrders,
    loadActiveWcsOrdersForce,
    startWcsOrdersPolling,
    stopWcsOrdersPolling,
    loadErrorWcsOrders,
    cancelWcsOrder,
    resumeWcsOrder,

    // Alarm actions
    // clearAlarms,
    // removeAlarm,

    // Misc
    reset,
  };
});
