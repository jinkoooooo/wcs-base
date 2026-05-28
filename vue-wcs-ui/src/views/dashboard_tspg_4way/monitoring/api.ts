/**
 * Simulator REST API 호출 모음
 *
 * 백엔드: logis-tspg-4way/.../simulator/SimulatorController.java
 * 모든 호출은 defHttp 를 사용 (프로젝트 공통 axios 래퍼).
 *
 * v2 변경:
 * - eqGroupId 가 컨트롤 / 데이터 조회 모두에서 파라미터로 필요.
 * - GET /eq-groups 신규.
 */
import { defHttp } from '/@/utils/http/axios';

// ============== 공통 타입 ==============

export interface EqGroup {
  id: string;
  name?: string | null;
}

// ============== 시뮬레이터 데이터 타입 ==============

/** 그룹별 가동 상태 */
export interface SimulatorGroupStatus {
  eqGroupId: string;
  hostRunning: boolean;
  plcRunning: boolean;
  portModeRunning: boolean;
  currentPhase: string | null;
  phaseStartedAt: string | null;
  phaseSubmitted: number;
  phaseTarget: number;
}

export interface SimulatorStatus {
  enabled: boolean;
  /** 모든 그룹의 가동 상태 (DB 에 등록된 모든 그룹) */
  groups: SimulatorGroupStatus[];
  config: {
    ownerCode: string;
    hostSystemCode: string;
    hostIntervalMs: number;
    hostMaxPending: number;
    plcPollMs: number;
    plcStepDelayMs: number;
    ratioInbound: number;
    ratioOutbound: number;
    ratioMove: number;
  };
  plcInFlight: number;
  seedDone: boolean;
  seedSkus: { code: string; name: string }[];
}


export interface SimulatorEvent {
  ts: number;
  type: string;
  [k: string]: unknown;
}

export interface SimulatorMetrics {
  hostSubmitted: number;
  hostFailed: number;
  hostByType: Record<string, number>;
  plcStarted: number;
  plcCompleted: number;
  plcFailed: number;
  recentEvents: SimulatorEvent[];
}

export interface TimeSeriesPoint {
  ts: number;
  submitted: number;
  completed: number;
  failed: number;
}

export interface InProgressOrder {
  order_key: string;
  order_type: string;
  order_status: number;
  ecs_if_status: number;
  from_loc_code: string | null;
  to_loc_code: string | null;
  host_order_key: string | null;
  created_at: string | null;
}

export interface HostOrderRow {
  host_order_key: string;
  host_system_code?: string | null;
  order_type: string;
  order_status: number;
  inspection_required: boolean | null;
  inspection_status: string | null;
  wcs_order_key: string | null;
  error_code: string | null;
  created_at: string | null;
}

export interface PortRow {
  loc_id: string;
  port_mode: string | null;
  active_task_count: number | null;
  task_id: string | null;
  stock_id: string | null;
}

export interface RackSummary {
  empty?: number;
  occupied?: number;
  reserved?: number;
  total?: number;
  simStocks?: number;
}

export interface CleanupPreview {
  hostOrders: number;
  hostOrderItems: number;
  shuttleOrders: number;
  shuttleOrderItems: number;
  simStocks: number;
}

export interface CleanupResult {
  hostOrders: number;
  hostItems: number;
  shuttleOrders: number;
  shuttleItems: number;
  locationsUnlinked: number;
  simStocks: number;
}

// ============== 그룹 목록 (드롭다운용) ==============
export const getEqGroups = () =>
  defHttp.get<EqGroup[]>({
    url: '/simulator/eq-groups',
    params: { },
  });

// ============== 제어 ==============
// 모든 start 계열은 eqGroupId 필수 (백엔드에서 검증)

// ============== 제어 ==============
export const startAll = (eqGroupId: string) =>
  defHttp.post({
    url: `/simulator/all/start?eqGroupId=${encodeURIComponent(eqGroupId)}`,
  });

export const stopAll = (eqGroupId: string) =>
  defHttp.post({
    url: `/simulator/all/stop?eqGroupId=${encodeURIComponent(eqGroupId)}`,
  });

export const startHost = (eqGroupId: string) =>
  defHttp.post({
    url: `/simulator/host/start?eqGroupId=${encodeURIComponent(eqGroupId)}`,
  });

export const stopHost = (eqGroupId: string) =>
  defHttp.post({
    url: `/simulator/host/stop?eqGroupId=${encodeURIComponent(eqGroupId)}`,
  });

export const startPlc = (eqGroupId: string) =>
  defHttp.post({
    url: `/simulator/plc/start?eqGroupId=${encodeURIComponent(eqGroupId)}`,
  });

export const stopPlc = (eqGroupId: string) =>
  defHttp.post({
    url: `/simulator/plc/stop?eqGroupId=${encodeURIComponent(eqGroupId)}`,
  });
// ============== 설정 ==============
export const setHostInterval = (ms: number) =>
  defHttp.post({ url: `/simulator/config/host-interval?ms=${ms}` });

export const setPlcStepDelay = (ms: number) =>
  defHttp.post({ url: `/simulator/config/plc-step-delay?ms=${ms}` });

export const setRatio = (eqGroupId: string, inbound: number, outbound: number, move: number) =>
  defHttp.post({
    url: `/simulator/config/phase-count?eqGroupId=${eqGroupId}&inbound=${inbound}&outbound=${outbound}`,
  });

// ============== 데이터 조회 ==============
export const getStatus = () =>
  defHttp.get<SimulatorStatus>({ url: '/simulator/status' });

export const getMetrics = () =>
  defHttp.get<SimulatorMetrics>({ url: '/simulator/metrics' });

export const getTimeseries = () =>
  defHttp.get<TimeSeriesPoint[]>({ url: '/simulator/timeseries' });

export const getInProgress = (eqGroupId: string) =>
  defHttp.get<InProgressOrder[]>({
    url: '/simulator/orders/in-progress',
    params: { eqGroupId },
  });

/**
 * 최근 HOST 주문.
 * - source 'all' (기본): 전체 호스트 주문 (메인 대시보드용)
 * - source 'sim'      : 시뮬 생성건만 (시뮬 모달용)
 * - eqGroupId 가 주어지면 해당 그룹만 필터링
 */
export const getHostRecent = (
  source: 'all' | 'sim' = 'all',
  eqGroupId?: string,
) =>
  defHttp.get<HostOrderRow[]>({
    url: '/simulator/orders/host-recent',
    params: { source, ...(eqGroupId ? { eqGroupId } : {}) },
  });

export const getPorts = (eqGroupId: string) =>
  defHttp.get<PortRow[]>({
    url: '/simulator/ports',
    params: { eqGroupId },
  });

export const getRackSummary = (eqGroupId: string) =>
  defHttp.get<RackSummary>({
    url: '/simulator/racks/summary',
    params: { eqGroupId },
  });

// ============== 정리 ==============
// cleanup 은 host_system_code='WMS-SIM' / lot_no LIKE 'LOT-SIM-%' 기준이라 그룹 무관
export const cleanupPreview = () =>
  defHttp.post<CleanupPreview>({ url: '/simulator/cleanup/preview' });

export const cleanupExecute = () =>
  defHttp.post<CleanupResult>({ url: '/simulator/cleanup/execute' });

// ====================================================================
// TSPG WCS 통합 대시보드 API (실 운영 데이터)
// ====================================================================

export interface OperationModeInfo {
  mode: string;
  isOperationModeEnabled: boolean;
  isPutbackEnabled: boolean;
  isDispatchLockEnabled: boolean;
  isInspectionEnabled: boolean;
}

export interface ByTypeCounts {
  INBOUND: number;
  OUTBOUND: number;
  MOVE: number;
  PUTBACK: number;
}

export interface HostPipeline {
  received: number;
  validated: number;
  allocated: number;
  executing: number;
  completed: number;
  error: number;
  total: number;
}

export interface DashboardSummary {
  eqGroupId: string;
  operation: OperationModeInfo;
  inProgressByType: ByTypeCounts;
  todayByType: ByTypeCounts;
  hostPipeline: HostPipeline;
  rack: RackSummary;
}

export interface ThroughputPoint {
  ts: string;
  INBOUND: number;
  OUTBOUND: number;
  MOVE: number;
  PUTBACK: number;
}

export const getDashboardSummary = (eqGroupId: string) =>
  defHttp.get<DashboardSummary>({
    url: '/wcs/dashboard/summary',
    params: { eqGroupId },
  });

export const getDashboardThroughput = (eqGroupId: string, minutes = 30) =>
  defHttp.get<ThroughputPoint[]>({
    url: '/wcs/dashboard/throughput',
    params: { eqGroupId, minutes },
  });
