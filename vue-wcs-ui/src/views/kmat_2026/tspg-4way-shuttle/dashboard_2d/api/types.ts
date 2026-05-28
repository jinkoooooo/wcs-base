/**
 * types.ts
 * 4-Way Shuttle WCS 타입 정의 (FRONTEND / camelCase)
 *
 * ============================================
 * 파일 개요
 * ============================================
 * 4방향 셔틀 WCS(Warehouse Control System) 시스템에서
 * 프론트엔드가 사용하는 모든 타입을 정의합니다.
 *
 * ============================================
 * 타입 분류
 * ============================================
 *
 * 1. 공통 타입 (ElidomStamp)
 *    - 모든 엔티티의 기본 감사(Audit) 필드
 *
 * 2. 2D 에디터/레이아웃 타입
 *    - TbEcs2dPage: 2D 에디터 페이지 (층별 캔버스)
 *    - TbEcs2dItemType: 설비 유형 정의
 *    - TbEcs2dItem: 2D 에디터에 배치된 객체
 *    - DashboardEquipmentData: 대시보드 렌더링용 확장 타입
 *
 * 3. WebSocket 이벤트 타입
 *    - 설비 위치/이동 이벤트
 *    - 작업(Job) 이벤트
 *    - 화물(Cargo) 이벤트
 *    - 알람/에러 이벤트
 *
 * 4. 실운영 테이블 타입 (tspg4way.entity.* 1:1 매칭)
 *    - TbEqCarMst: 셔틀카(운반차) 마스터
 *    - TbEqCvMst: 컨베이어 마스터
 *    - TbEqRackMst: 랙(선반) 마스터
 *    - TbWcsShuttleOrder: WCS 셔틀 작업 오더
 *
 * ============================================
 * 네이밍 규칙
 * ============================================
 * - 프론트엔드: camelCase (posX, equipmentId)
 * - 백엔드 JSON: snake_case (pos_x, equipment_id)
 * - 변환: keysToCamel() 유틸리티 함수 사용
 *
 * ============================================
 * 기준 문서
 * ============================================
 * - tspg4way.entity.* (Java Entity 클래스)
 * - ElidomStampHook + Column 스펙
 */

// =============================================
// 1. 공통 타입 (ElidomStampHook-like)
// =============================================

/**
 * ElidomStamp - 공통 감사(Audit) 필드 인터페이스
 *
 * 모든 엔티티가 상속하는 기본 필드입니다.
 * 데이터 생성/수정 이력을 추적하는 데 사용됩니다.
 *
 * @remarks
 * - 실제 API 응답에서 이 필드들이 없을 수 있으므로 모두 optional
 * - Java의 ElidomStampHook 인터페이스와 매칭
 */
export interface ElidomStamp {
  domainId?: number;
  creatorId?: string;
  updaterId?: string;
  createdAt?: string;
  updatedAt?: string;
}

// =============================================
// 2. 2D 에디터 / 레이아웃 타입
// =============================================

/**
 * Layout/UI equipment type codes
 * - TbEcs2dItem.equipmentTypeCode 에서 사용하는 문자열 타입
 * - tb_eq_mst.type(EqType) 숫자 계열과는 다른 개념
 */
/**
 * 레이아웃(2D 에디터) 설비 타입 — 코드 / 한글 라벨 / 아이콘을 한 항목 안에 통합.
 * tb_eq_mst.type(EqType) 숫자 enum 과는 별개의 문자열 분류.
 */
export const LayoutEquipmentType = {
  CONVEYOR: { code: 'CONVEYOR', label: '컨베이어',     icon: '➡️' },
  LIFTER:   { code: 'LIFTER',   label: '리프터',       icon: '⬆️' },
  SHUTTLE:  { code: 'SHUTTLE',  label: '셔틀',         icon: '🚗' },
  RACK:     { code: 'RACK',     label: '랙',           icon: '📦' },
  BCR:      { code: 'BCR',      label: '바코드 스캐너', icon: '▥' },
  PILLAR:   { code: 'PILLAR',   label: '기둥',         icon: '⏹' },
  STV:      { code: 'STV',      label: 'STV',         icon: 'STV' },
  CRANE:    { code: 'CRANE',    label: '크레인',       icon: 'CRN' },
} as const;

export type LayoutEquipmentTypeCode =
  (typeof LayoutEquipmentType)[keyof typeof LayoutEquipmentType]['code'];

/** 알려진 모든 코드 set — 정규화에 사용 */
const LAYOUT_EQUIPMENT_TYPE_CODES = new Set<string>(
  Object.values(LayoutEquipmentType).map((e) => e.code),
);

/** 문자열을 안전하게 layout equipment type 으로 정규화 */
export function normalizeLayoutEquipmentType(value?: string | null): LayoutEquipmentTypeCode | '' {
  const v = String(value ?? '')
    .trim()
    .toUpperCase();
  return LAYOUT_EQUIPMENT_TYPE_CODES.has(v) ? (v as LayoutEquipmentTypeCode) : '';
}

export function isConveyorType(value?: string | null): boolean {
  return normalizeLayoutEquipmentType(value) === LayoutEquipmentType.CONVEYOR.code;
}

export function isLifterType(value?: string | null): boolean {
  return normalizeLayoutEquipmentType(value) === LayoutEquipmentType.LIFTER.code;
}

export function isShuttleType(value?: string | null): boolean {
  return normalizeLayoutEquipmentType(value) === LayoutEquipmentType.SHUTTLE.code;
}

export function isRackType(value?: string | null): boolean {
  return normalizeLayoutEquipmentType(value) === LayoutEquipmentType.RACK.code;
}

export function isError(errorCode?: string | number | null): boolean {
  if (errorCode == null) return false;
  const v = String(errorCode).trim();
  // '', '0' 둘 다 정상 상태로 간주 (백엔드에서 errorCode/errorId가 비어있을 수 있음)
  return v !== '' && v !== '0';
}

export function hasShuttleError(shuttle?: Partial<DashboardShuttleData> | null): boolean {
  if (!shuttle) return false;
  return isError(shuttle.errorCode);
}

/**
 * TbEcs2dPage - 2D 에디터 페이지 (층별 캔버스)
 *
 * 물류센터의 각 층을 나타내는 2D 캔버스 정보입니다.
 * 한 페이지에 해당 층의 모든 설비(랙, 컨베이어, 셔틀 등)가 배치됩니다.
 *
 * @example
 * ```ts
 * const page: TbEcs2dPage = {
 *   id: 'page-001',
 *   lcId: 'LC001',
 *   eqGroupId: 'GROUP-A',
 *   pageName: '1층 자동창고',
 *   floorLevel: 1,
 *   canvasWidth: 1920,
 *   canvasHeight: 1080,
 *   backgroundColor: '#2a2d36'
 * };
 * ```
 */
export interface TbEcs2dPage extends ElidomStamp {
  id: string;
  lcId: string;

  // 페이지-설비그룹 매핑(프론트에서만 쓰더라도 컬럼/DTO에 있다고 가정)
  eqGroupId?: string;

  pageIndex: number;
  pageName: string;
  floorLevel?: number;
  zoneCode?: string;

  canvasWidth: number;
  canvasHeight: number;
  backgroundColor: string;
  backgroundImage?: string;

  showGrid?: boolean;
  gridSize?: number;
  snapToGrid?: boolean;

  isActive?: boolean;
  description?: string;
}

export interface TbEcs2dItemType extends ElidomStamp {
  id: string;
  lcId: string;

  typeCode: string;
  typeName: string;
  /** 표시용 이름 (typeName의 alias - 레거시 호환) */
  typeName2d?: string;
  category?: string;
  description?: string;

  iconUrl2d?: string;
  iconData2d?: string;
  iconFileName?: string;

  defaultWidth?: number;
  defaultHeight?: number;

  rotatable?: boolean;
  resizable?: boolean;
  showStatus?: boolean;
  showPopup?: boolean;
  sortOrder?: number;
  isActive?: boolean;

  /**
   * 레이어 타입: 'static' | 'dynamic' | 'overlay'
   * - static: 정적 설비 (MapEditor 캔버스 고정)
   * - dynamic: 동적 설비 (Dashboard 실시간 이동)
   * - overlay: 오버레이 (기둥, 레일 등 구조물)
   */
  layerType?: string;

  /**
   * 실운영 EqType 숫자 코드 (tb_eq_mst.type)
   * - RACK=11, CONVEYOR/LIFTER=21, SHUTTLE_CAR=22
   * - null 이면 실운영 설비 매핑 불가
   */
  realEqTypeNum?: number | null;

  /** 화물 데이터 연동 여부 */
  hasCargo?: boolean;
  /** 재고 데이터 연동 여부 */
  hasInventory?: boolean;
}

export interface TbEcs2dItem extends ElidomStamp {
  id: string;
  lcId: string;
  pageId: string;

  equipmentId?: string;
  equipmentCode: string;
  equipmentTypeCode?: string;

  customLabel?: string;

  /**
   * 실운영 설비 매핑
   * - realEqId: tb_eq_*_mst.id 또는 tb_eq_mst.id 등(프로젝트 정책에 맞게)
   * - realEqType: 'CAR' | 'RACK' | 'CV' | 'EQ' 등 UI에서 구분용
   */
  realEqId?: string;
  realEqType?: string;

  posX: number;
  posY: number;
  width: number;
  height: number;

  rotation: number;
  scaleX: number;
  scaleY: number;
  flipH: boolean;
  flipV: boolean;

  zIndex: number;
  opacity: number;

  showLabel: boolean;
  isVisible: boolean;
  isLocked: boolean;
}

/**
 * DashboardEquipmentData - 대시보드 렌더링용 설비 데이터
 *
 * TbEcs2dItem(레이아웃 정보)에 실시간 상태와 아이콘을 추가한 확장 타입입니다.
 * 대시보드에서 설비를 시각적으로 표시할 때 사용합니다.
 *
 * ============================================
 * 데이터 구조
 * ============================================
 *
 * 1. 레이아웃 정보 (TbEcs2dItem 상속)
 *    - 위치: posX, posY
 *    - 크기: width, height
 *    - 변환: rotation, scaleX, scaleY, flipH, flipV
 *
 * 2. 아이콘/기본 크기
 *    - iconData2d: SVG 또는 이미지 URL
 *    - defaultWidth, defaultHeight: 기본 크기
 *
 * 3. 실시간 CAR(셔틀카) 상태
 *    - realCarStatus: 셔틀 상태 코드
 *    - realCarBatteryStatus: 배터리 상태
 *    - realCarCargoYn: 화물 적재 여부
 *
 * 4. 실시간 RACK(랙) 상태 — CellStateService 기반
 *    - realRackStateCode: 재고 축 상태 (PRODUCT/EMPTY_BOX/INBOUND/OUTBOUND/...)
 *    - realRackLocked / realRackInboundForbidden / realRackOutboundForbidden: 금지 축
 *    - realRackItemCode / realRackLotNo / realRackStockId / realRackTaskId: 재고 상세
 *
 * 5. UI 파생 필드
 *    - currentStatus: 현재 상태 (CAR 전용)
 *    - hasCargo: 화물 보유 여부
 *    - batteryLevel: 배터리 레벨
 */
export interface DashboardEquipmentData extends TbEcs2dItem {
  // 아이콘/기본 크기(프론트 렌더링용)
  iconData2d?: string;
  defaultWidth?: number;
  defaultHeight?: number;

  // ====== realtime CAR columns (flat) ======
  realCarId?: string | null;
  realCarEqId?: string | null;
  realCarType?: string | null;
  realCarRow?: number | null;
  realCarBay?: number | null;
  realCarLevel?: number | null;
  realCarStatus?: number | null;
  realCarBatteryStatus?: number | null;
  realCarCargoYn?: boolean | null;
  realCarErrorId?: string | null;
  realCarErrorDesc?: string | null;

  // ====== realtime RACK columns (flat) ======
  realRackId?: string | null;
  realRackEqId?: string | null;
  /** 랙 타입: 11=셀, 21=입고포트, 22=출고포트, 23=입출고포트, 31=충전포트, 32=충전진입포트 */
  realRackType?: number | null;
  realRackRow?: number | null;
  realRackBay?: number | null;
  realRackLevel?: number | null;
  /** 주행 전용 여부 (true: 적재 불가, 셔틀 주행만 가능) */
  realRackDriveOnlyYn?: boolean | null;

  // ====== CellStateService 기반 랙 상태 (Rack 범례 SSOT) ======
  /**
   * 재고 축 상태 코드 (CellStateService.state_code).
   * EMPTY / PRODUCT / EMPTY_BOX / INBOUND / OUTBOUND /
   * DOUBLE_IN / EMPTY_OUT / CONFIRM / DRIVE.
   */
  realRackStateCode?: string | null;
  /** 재고 카테고리 (stock_type) — NORMAL/QC_PENDING/QC_FAIL/NIA_PENDING/RETURN/DISPOSAL */
  realRackStockType?: string | null;
  /** 유통기한 (expired_datetime ISO 문자열) — 만료 시 회색 점선 오버레이 */
  realRackExpiredDatetime?: string | null;
  /** 사용금지 (locked). is_enabled=false */
  realRackLocked?: boolean | null;
  /** 입고금지 (inbound_forbidden). is_inbound_enabled=false */
  realRackInboundForbidden?: boolean | null;
  /** 출고금지 (outbound_forbidden). is_outbound_enabled=false */
  realRackOutboundForbidden?: boolean | null;
  /** tb_inventory_stock.item_code (재고 아이템 코드) */
  realRackItemCode?: string | null;
  /** tb_inventory_stock.lot_no */
  realRackLotNo?: string | null;
  /** tb_inventory_location.stock_id */
  realRackStockId?: string | null;
  /** tb_inventory_location.task_id */
  realRackTaskId?: string | null;
  /**
   * 활성 작업의 실제 order_type (INBOUND / OUTBOUND / MOVE / PUTBACK).
   * task_id 가 가리키는 tb_wcs_shuttle_order.order_type 그대로.
   * task 가 없으면 null.
   * realRackStateCode 는 셀 색상/legend 용 추상 상태이고,
   * 이 필드는 상세 팝업에서 운영자에게 실제 작업 종류를 보여주기 위함.
   */
  realRackActiveOrderType?: string | null;
  /** 정규화 저장 위치 (ZONE-RR-BB-LL) */
  realRackStorLoc?: string | null;

  // ====== WCS 로케이션 상태 (RACK 타입 전용) ======
  /**
   * WCS 로케이션 상태 코드 (tb_wcs_loc_mst.status)
   * - 0  : EMPTY       — 비어있음
   * - 10 : OCCUPIED    — 재고있음
   * - 20 : LOCKED      — 작업중(오더 점유)
   * - 30 : EMPTY_PICK  — 공출고(시스템 재고O, 실물X) → 캔버스 빨간 테두리
   * - 40 : DOUBLE_ENTRY — 이중입고(실물O, 시스템 재고X) → 캔버스 주황 테두리
   * - 90 : DISABLED    — 사용불가
   */
  wcsLocStatus?: number | null;
  /** WCS 로케이션 사용 여부 (0=비가동, 1=사용) */
  wcsLocUseYn?: number | null;
  /** WCS 로케이션 수동 잠금 여부 (0=해제, 1=잠금) */
  wcsLocLockYn?: number | null;

  // ====== UI에서 바로 쓰고 싶은 “파생 필드” (프론트에서 만들기) ======
  currentStatus?: number; // CAR 의 realCarStatus. RACK 상태는 realRackStateCode 사용.
  hasCargo?: boolean; // 예: CAR면 realCarCargoYn
  batteryLevel?: number; // 현재는 battery_status 뿐이라면 그대로 쓰거나 변환 규칙 필요
}

// =============================================
// 3. WebSocket 이벤트 타입
// =============================================

/**
 * EquipmentPositionEvent - 설비 위치 변경 이벤트
 *
 * 셔틀 등 동적 설비의 위치가 변경될 때 WebSocket으로 수신하는 이벤트입니다.
 * 실시간으로 설비 위치를 추적하고 화면에 반영하는 데 사용합니다.
 *
 * @property equipmentId - 설비 고유 ID
 * @property pointCode - 현재 위치 포인트 코드 (예: 'A1-01-01')
 * @property posX - X 좌표 (픽셀)
 * @property posY - Y 좌표 (픽셀)
 * @property movementStatus - 이동 상태 (1: 이동중, 2: 정지, 3: 작업중)
 * @property timestamp - 이벤트 발생 시간 (밀리초)
 */
export interface EquipmentPositionEvent {
  equipmentId: string;
  pointCode: string;
  posX?: number;
  posY?: number;
  movementStatus: number;
  timestamp: number;
}

export interface EquipmentMoveEvent {
  equipmentId: string;
  fromPointCode?: string;
  toPointCode: string;
  movementStatus: number;
  timestamp: number;
}

export interface BcrScanEvent {
  equipmentId: string;
  equipmentCode: string;
  barcode: string;
  timestamp: number;
}

export interface AlarmData {
  id?: string;
  lcId: string;
  level?: string;
  message: string;
  timestamp: string;
  equipmentId?: string;
  equipmentCode?: string;
  raw?: Record<string, any>;
}

export interface WebSocketError {
  message: string;
  code?: string | number;
  errorMessage?: string;
  raw?: any;
  timestamp?: number;
}

// 작업/화물 이벤트(네가 websocket.ts에서 import 중이라면 최소 타입만 유지)
// - 실제 스펙이 따로 있으면 추후 교체
export interface JobStatusChangeEvent {
  jobKey: string;
  status: number | string;
  timestamp: number;
  [k: string]: any;
}
export interface JobCreatedEvent {
  jobKey: string;
  timestamp: number;
  [k: string]: any;
}
export interface JobAssignedEvent {
  jobKey: string;
  shuttleId?: string;
  timestamp: number;
  [k: string]: any;
}

export interface CargoStatusChangeEvent {
  cargoId: string;
  status: number | string;
  timestamp: number;
  [k: string]: any;
}
export interface CargoPositionChangeEvent {
  cargoId: string;
  posX?: number;
  posY?: number;
  timestamp: number;
  [k: string]: any;
}
export interface CargoLoadedEvent {
  cargoId: string;
  shuttleId?: string;
  timestamp: number;
  [k: string]: any;
}
export interface CargoUnloadedEvent {
  cargoId: string;
  shuttleId?: string;
  timestamp: number;
  [k: string]: any;
}

export interface DashboardShuttleData {
  equipmentId: string;
  equipmentCode?: string;

  /** 백엔드 DTO */
  cellId?: string;

  posX: number;
  posY: number;

  /** 백엔드 DTO의 floor */
  floor?: number;

  /** 기존 화면 호환용 alias */
  posZ?: number;

  /**
   * 0: READY
   * 1: RESERVE
   * 2: RUN
   * 5: EMR_STOP
   * 8: ERROR
   * 9: COMPLETE
   */
  status?: number;

  /** 상태 설명 */
  statusDesc?: string;

  batteryLevel?: number;
  batteryStatus?: number;

  hasCargo?: boolean;

  errorCode?: string | number | null;
  errorMessage?: string | null;

  currentJobKey?: string;

  row?: number;
  bay?: number;

  /** 0: 정지, 1: 이동중 */
  movementStatus?: number;

  targetCellId?: string;

  hasActiveJob?: boolean;
  currentOrderKey?: string;
  currentOrderType?: number;
  currentOrderStatus?: number;
  currentBarcode?: string;
  currentFromLoc?: string;
  currentToLoc?: string;

  ts?: number;

  [k: string]: any;
}

export interface DashboardCargoData {
  cargoId: string;
  barcode?: string;
  posX: number;
  posY: number;
  cargoStatus?: number; // 0=대기, 1=이동중, 2=보관중, 3=피킹중, 9=에러
  carriedByShuttleId?: string;
  storedCellId?: string; // 보관된 랙 셀 ID
  floor?: number; // 층 정보
  [k: string]: any;
}

// =============================================
// Provider-based RealTime DTO (Backend Provider DTOs)
// =============================================

/**
 * ShuttlePositionDto - 셔틀 위치/상태 DTO
 * Backend: /topic/shuttle/car/{lcId} (500ms)
 */
export interface RtShuttlePosition {
  equipmentId: string;
  equipmentCode?: string;
  cellId?: string;
  posX: number;
  posY: number;
  floor?: number;
  /** 0: READY, 1: RESERVE, 2: RUN, 5: EMR_STOP, 8: ERROR, 9: COMPLETE */
  status?: number;
  statusDesc?: string;
  batteryLevel?: number;
  batteryStatus?: number;
  hasCargo?: boolean;
  /** 자동 모드 여부 (tb_eq_car_mst.auto_yn) */
  autoYn?: boolean;
  /** 사용 여부 (tb_eq_car_mst.use_yn) */
  useYn?: boolean;
  /** 정규화된 셔틀 상태 — 프런트는 이 필드로만 색/라벨 분기 */
  shuttleState?: 'DISABLED' | 'ERROR' | 'MANUAL' | 'CHARGING' | 'RUNNING' | 'IDLE' | null;
  errorCode?: string;
  errorMessage?: string;
  currentJobKey?: string;
  row?: number;
  bay?: number;
  /** 0: 정지, 1: 이동중 */
  movementStatus?: number;
  targetCellId?: string;
  // ===== 현재 작업 정보 (tb_ecs_rack_order) =====
  /** 현재 진행 중인 작업 여부 */
  hasActiveJob?: boolean;
  /** 현재 작업 키 */
  currentOrderKey?: string;
  /** 현재 작업 타입 (11:입고, 12:출고, 21:충전, 22:이동) */
  currentOrderType?: number;
  /** 현재 작업 상태 (0:대기, 1:전송, 2:작업중, 9:완료) */
  currentOrderStatus?: number;
  /** 현재 작업 바코드 */
  currentBarcode?: string;
  /** 현재 작업 출발지 */
  currentFromLoc?: string;
  /** 현재 작업 목적지 */
  currentToLoc?: string;
  ts?: number;
}

/**
 * ConveyorStatusDto - 컨베이어 상태 DTO
 * Backend: /topic/realtime/conveyor/{lcId}/{eqGroupId}/{pageId} (500ms)
 */
export interface RtConveyorStatus {
  equipmentId: string;
  eqId?: string;
  type?: string;
  posX: number;
  posY: number;
  /** EqConveyorStatus 원시값. 0: READY, 1: MOVE_RESERVE. 프런트 분기에는 conveyorState 사용 권장. */
  status?: number;
  hasCargo?: boolean;
  moving?: boolean;
  runYn?: boolean;
  /** 자동 모드 여부 (tb_eq_cv_mst.auto_yn) */
  autoYn?: boolean;
  /** 사용 여부 (tb_eq_cv_mst.use_yn) */
  useYn?: boolean;
  /** 정규화된 컨베이어 상태 — 프런트는 이 필드로만 색/라벨 분기 */
  conveyorState?: 'DISABLED' | 'ERROR' | 'MANUAL' | 'AUTO' | null;
  /** 정규화된 Pallet 3-way 상태 — 프런트는 이 필드로만 분기 */
  palletState?: 'DATA_AND_PHY' | 'PHY_ONLY' | 'DATA_ONLY' | null;
  /** PLC 명령 ID */
  plcCmdId?: number;
  errorCode?: string;
  errorMessage?: string;
  level?: number;
  // ===== 현재 작업 정보 (tb_ecs_route_order) =====
  /** 현재 진행 중인 작업 여부 */
  hasActiveJob?: boolean;
  /** 현재 작업 키 */
  currentOrderKey?: string;
  /** 현재 작업 타입 (11:입고, 12:출고, 21:충전, 22:이동) */
  currentOrderType?: number;
  /** 현재 작업 상태 (0:대기, 1:전송, 2:작업중, 9:완료) */
  currentOrderStatus?: number;
  /** 현재 작업 바코드 */
  currentBarcode?: string;
  /** 현재 작업 출발지 */
  currentFromLoc?: string;
  /** 현재 작업 목적지 */
  currentToLoc?: string;
  ts?: number;
}

/**
 * LifterStatusDto - 리프터 상태 DTO
 * Backend: /topic/realtime/lifter/{lcId}/{eqGroupId}/{pageId} (500ms)
 */
export interface RtLifterStatus {
  equipmentId: string;
  eqId: string;
  layoutId?: string;
  posX?: number;
  posY?: number;
  currentLevel?: number;
  targetLevel?: number | null;
  status?: number;
  hasCargo?: boolean;
  hasShuttle?: boolean;
  moving?: boolean;
  stopperOpen?: boolean;
  plcCmdId?: number;
  /** 에러 ID (tb_eq_cv_mst.error_id). null/""/"0" 은 정상. AlarmDataProvider 와 정합. */
  errorId?: string;
  errorMessage?: string;
  /** 사용 여부 (tb_eq_cv_mst.use_yn). false 인 리프터는 알람 대상 아님 → 에러 판정도 제외. */
  useYn?: boolean;
  hasActiveJob?: boolean;
  currentOrderKey?: string;
  currentOrderType?: number;
  currentOrderStatus?: number;
  currentBarcode?: string;
  currentFromLoc?: string;
  currentToLoc?: string;
  ts?: number;
}

/**
 * CargoPositionDto - 화물 위치 DTO
 * Backend: /topic/shuttle/cargo/{lcId} (500ms)
 */
export interface RtCargoPosition {
  cargoId: string;
  barcode?: string;
  posX: number;
  posY: number;
  /** 0: WAITING, 1: MOVING, 2: STORED, 3: ON_CONVEYOR */
  status?: number;
  carriedByShuttleId?: string;
  cellId?: string;
  conveyorId?: string;
  skuCode?: string;
  itemName?: string;
  qty?: number;
  floor?: number;
  ts?: number;
}

/** Cargo Status constants */
export const CargoStatus = {
  WAITING: 0,
  MOVING: 1,
  STORED: 2,
  ON_CONVEYOR: 3,
} as const;

// =============================================
// ECS 상태 상수
// 단일 출처 = ../../constants/EcsDBConsts.ts
// 기존 import 호환을 위해 여기서는 re-export 만 한다.
// =============================================
export {
  EqCarStatus,
  EqCarBatteryStatus,
  EqRackStatus,
  EqConveyorStatus,
  RackType,
  ConveyorType,
  EqType,
} from '../../constants/EcsDBConsts';

/**
 * JobStatusDto - ECS 작업 상태 DTO
 * Backend: /topic/realtime/job/{lcId}/{eqGroupId} (1000ms)
 *
 * [작업 계층 구조]
 * - WCS: tb_wcs_shuttle_order (상위 오더)
 * - ECS_RACK: tb_ecs_rack_order (셔틀 PICK/DROP 단위)
 * - ECS_ROUTE: tb_ecs_route_order (리프터/CV 이동)
 */
export interface RtJobStatus {
  jobKey: string;
  /** 상위 WCS 오더 키 (ECS 레벨인 경우) */
  parentJobKey?: string;
  /** 작업 레벨: WCS | ECS_RACK | ECS_ROUTE */
  jobLevel?: string;
  /** 11: INBOUND, 12: OUTBOUND, 21: CHARGE, 22: MOVE */
  jobType?: number;
  /** 0: READY, 1: EQ_SEND, 2: WORKING, 9: COMPLETE, 90: CANCEL, 99: ERROR */
  status?: string;
  fromLoc?: string;
  toLoc?: string;
  priority?: number;
  barcode?: string;
  /** 할당된 설비 ID (셔틀/리프터/CV) */
  assignedEquipmentId?: string;
  /** 할당된 설비 타입 (SHUTTLE/LIFTER/CONVEYOR) */
  assignedEquipmentType?: string;
  eqGroupId?: string;
  /** PLC 명령 ID */
  plcCmdId?: number;
  /** PLC 명령 상태 */
  cmdStatus?: number;
  createdAt?: number;
  startedAt?: number;
  completedAt?: number;
  errorCode?: string;
  errorMessage?: string;
  ts?: number;
}

/** Job Type constants (EcsDBConsts.OrderType 기준) */
export const JobType = {
  INBOUND: 11, // 입고
  OUTBOUND: 12, // 출고
  MOVE: 13, // 재고이동 (백엔드 기준)
  CHARGE: 21, // 충전
} as const;

/**
 * 프론트 작업 상태 — 정의는 `constants/jobUiStatus.ts` 로 이전.
 * 기존 import 경로 호환을 위해 여기서 re-export.
 */
export { JobUiStatus, ACTIVE_JOB_STATUSES, FINISHED_JOB_STATUSES } from '../../constants';
export type { JobUiStatusCode } from '../../constants';

/** Job Status constants */
export const JobStatusCode = {
  READY: 0,
  EQ_SEND: 1,
  WORKING: 2,
  COMPLETE: 9,
  CANCEL: 90,
  ERROR: 99,
} as const;

/** Job Level constants */
export const JobLevel = {
  WCS: 'WCS',
  ECS_RACK: 'ECS_RACK',
  ECS_ROUTE: 'ECS_ROUTE',
} as const;

/**
 * AlarmDto - 알람/에러 DTO
 * Backend: /topic/realtime/alarm/{lcId}/{eqGroupId} (1000ms)
 *
 * [알람 타입]
 * - EQUIPMENT: 설비 에러 (셔틀, 컨베이어, 리프터)
 * - JOB_ERROR: 작업 에러 (ECS 작업 실패)
 */
export interface RtAlarm {
  alarmId: string;
  /** 알람 타입: EQUIPMENT | JOB_ERROR */
  alarmType?: string;
  /** SHUTTLE, CONVEYOR, LIFTER, RACK */
  equipmentType?: string;
  equipmentId?: string;
  equipmentCode?: string;
  // ===== 작업 관련 필드 (JOB_ERROR인 경우) =====
  /** 작업 키 (JOB_ERROR인 경우) */
  orderKey?: string;
  /** 작업 타입 (11:입고, 12:출고, 21:충전, 22:이동) */
  orderType?: number;
  /** 작업 바코드 */
  barcode?: string;
  // ===== 에러 정보 =====
  errorCode?: string;
  errorMessage?: string;
  /** 0: INFO, 1: WARNING, 2: ERROR, 3: CRITICAL */
  severity?: number;
  occurredAt?: number;
  acknowledged?: boolean;
  acknowledgedAt?: number;
  acknowledgedBy?: string;
  ts?: number;
}

/** Alarm Type constants */
export const AlarmType = {
  EQUIPMENT: 'EQUIPMENT',
  JOB_ERROR: 'JOB_ERROR',
} as const;

/** Alarm Severity constants */
export const AlarmSeverity = {
  INFO: 0,
  WARNING: 1,
  ERROR: 2,
  CRITICAL: 3,
} as const;

// =============================================
// Editor Constants
// =============================================
export const STATIC_EQUIPMENT_TYPES = [
  LayoutEquipmentType.CONVEYOR.code,
  LayoutEquipmentType.LIFTER.code,
  LayoutEquipmentType.BCR.code,
  LayoutEquipmentType.RACK.code,
  LayoutEquipmentType.PILLAR.code,
  LayoutEquipmentType.STV.code,
  LayoutEquipmentType.CRANE.code,
] as const;

export type StaticEquipmentType = (typeof STATIC_EQUIPMENT_TYPES)[number];

export const DYNAMIC_EQUIPMENT_TYPES = [LayoutEquipmentType.SHUTTLE.code] as const;

export type DynamicEquipmentType = (typeof DYNAMIC_EQUIPMENT_TYPES)[number];

// 에디터에서 쓰는 객체
export type EditorObject = TbEcs2dItem;

// Undo/Redo
export type HistoryType = 'add' | 'remove' | 'update' | 'batch';
export interface HistoryItem {
  id: string;
  timestamp: number;
  type: HistoryType;
  description?: string;
  before?: any;
  after?: any;
}

// =============================================
// ✅ Real Operation Tables (tspg4way.entity.* 1:1)
// =============================================

/** tb_cell */
export interface TbCell extends ElidomStamp {
  id: string;
  cellId: string;
  locX: number;
  locY: number;
  hasCargo: boolean;
}

/** tb_ecs_rack_order */
export interface TbEcsRackOrder extends ElidomStamp {
  id: number; // Java long
  orderKey: string;
  orderType: number;
  orderStatus: number;
  priority: number;
  barcode: string;
  eqId: string;
  eqType: string;
  eqCarId: string;
  plcCmdId: number;
  cmdStatus: number;
  fromLocCode: string;
  fromRow: number;
  fromBay: number;
  toLocCode: string;
  toRow: number;
  toBay: number;
  errorId: string;
  errorDesc: string;
  startedAt: string;
  finishedAt: string;
}

/** tb_ecs_route_order */
export interface TbEcsRouteOrder extends ElidomStamp {
  id: number; // Java long
  orderKey: string;
  orderType: number;
  orderStatus: number;
  priority: number;
  barcode: string;
  eqId: string;
  eqType: string;
  plcCmdId: string;
  cmdStatus: number;
  fromLocCode: string;
  toLocCode: string;
  errorId: string;
  errorDesc: string;
  startedAt: string;
  finishedAt: string;
}

/** tb_eq_group_mst */
export interface TbEqGroupMst extends ElidomStamp {
  id: string;
  name: string;
  type: string;
}

/** tb_eq_mst */
export interface TbEqMst extends ElidomStamp {
  id: string;
  eqGroupId: string;
  name: string;
  type: string;
  plcId: string;
}

/** tb_eq_plc_mst */
export interface TbEqPlcMst extends ElidomStamp {
  id: string;
  name: string;
  ip: string;
  port: number;
  plcType: string;
  plcEqType: number;
  connectYn: boolean;
  useYn: boolean;
}

/**
 * TbEqCarMst - 셔틀카(운반차) 마스터 테이블
 *
 * 4방향 셔틀 시스템의 핵심 설비인 셔틀카 정보입니다.
 * 셔틀카는 랙 사이를 이동하며 화물을 운반합니다.
 *
 * ============================================
 * 주요 필드 설명
 * ============================================
 *
 * 위치 정보:
 * - row: 행 위치 (가로축)
 * - bay: 열 위치 (세로축)
 * - level: 층 위치 (높이)
 * - rackId: 현재 위치한 랙 ID
 *
 * 상태 정보:
 * - mode: 운영 모드 (자동/수동)
 * - status: 설비 상태 (0: 정상, 9: 에러 등)
 * - batteryStatus: 배터리 상태 (0~100%)
 * - cargoYn: 화물 적재 여부
 *
 * 운영 범위:
 * - minRow, maxRow: 이동 가능한 행 범위
 *
 * 에러 정보:
 * - errorId: 에러 코드
 * - errorDesc: 에러 설명
 *
 * @table tb_eq_car_mst
 */
export interface TbEqCarMst extends ElidomStamp {
  id: string;
  eqId: string;
  type: string;

  row: number;
  bay: number;
  level: number;

  rackId: string;

  mode: number;
  plcCmdId: number;

  status: number;
  batteryStatus: number;

  cargoYn: boolean;

  minRow: number;
  maxRow: number;

  errorId: string;
  errorDesc: string;

  useYn: boolean;
}

/**
 * 리프터는 TbEqCvMst에서 type='LIFT'로 관리됩니다.
 * 별도의 TbEqLifterMst 테이블은 존재하지 않습니다.
 * 리프터 실시간 데이터는 RtLifterStatus DTO를 사용합니다.
 */

/**
 * TbEqRackMst - 랙(선반) 마스터 테이블
 *
 * 화물을 보관하는 랙(선반)의 각 셀 정보입니다.
 * 랙은 row(행), bay(열), level(층)으로 구분되는 3차원 그리드 구조입니다.
 *
 * ============================================
 * 주요 필드 설명
 * ============================================
 *
 * 위치 정보:
 * - row: 행 위치 (가로축)
 * - bay: 열 위치 (세로축)
 * - level: 층 위치 (높이)
 *
 * 재고 정보:
 * - skuId: 보관된 SKU(상품) ID
 * - skuQty: 보관 수량
 *
 * 상태 정보:
 * - status: 셀 상태 (0: 비어있음, 1: 사용중 등)
 * - useYn: 사용 가능 여부
 *
 * @table tb_eq_rack_mst
 */
export interface TbEqRackMst extends ElidomStamp {
  id: string;
  eqId: string;
  /** 랙 타입: 11=셀, 21=입고포트, 22=출고포트, 23=입출고포트, 31=충전포트, 32=충전진입포트 */
  type: number;

  row: number;
  bay: number;
  level: number;

  skuId: string;
  skuQty: number;

  status: number;
  errorId: string;
  errorDesc: string;

  useYn: boolean;
  cargoYn: boolean;
  /** 주행 전용 여부 (true: 적재 불가, 셔틀 주행만 가능) */
  driveOnlyYn: boolean;
}

/** tb_wcs_shuttle_order */
export interface TbWcsShuttleOrder extends ElidomStamp {
  id: string;
  orderKey: string;
  orderType: string;
  status: string;
  orderStatus: number;
  priority: number;
  fromLocCode: string;
  toLocCode: string;
  ecsIfStatus: number;
  barcode: string;
}

// =============================================
// Layout + Real Status (Dashboard 응답 결합용)
// - getLayoutsWithRealStatus 응답에 맞춰 optional로 확장
// =============================================
export interface LayoutWithRealStatus extends TbEcs2dItem {
  // CAR
  realCarId?: string;
  realPosX?: number;
  realPosY?: number;
  realPosZ?: number;
  realStatus?: number;
  realBatteryStatus?: number;
  realCargoYn?: boolean;

  // RACK
  realSkuId?: string;
  realSkuQty?: number;
}

export interface OrderControlResponse {
  success: boolean;
  message: string;
  orderKey?: string;
  newPriority?: number;
}

// =============================================
// Dashboard UI ViewModel Types
// =============================================

/**
 * LifterOverlay
 * Dashboard2D에서 리프터 설비 위에 렌더링하는 UI 전용 ViewModel
 *
 * - layout: 2D 레이아웃 설비 정보
 * - currentLevel/targetLevel: 현재층/목표층
 * - isMoving/isGoingUp/isGoingDown: 애니메이션/상태 표시용
 * - hasShuttle/hasCargo/hasError/hasActiveJob/stopperOpen: 뱃지/스타일 표시용
 */
export interface LifterOverlay {
  layoutId: string;
  layout: DashboardEquipmentData;

  currentLevel: number;
  targetLevel: number | null;

  isMoving: boolean;
  isGoingUp: boolean;
  isGoingDown: boolean;

  hasShuttle: boolean;
  hasCargo: boolean;
  hasError: boolean;
  hasActiveJob: boolean;
  stopperOpen: boolean;
}

/**
 * 배터리 표시용 클래스 타입
 */
export type BatteryLevelClass =
  | 'battery-critical'
  | 'battery-low'
  | 'battery-medium'
  | 'battery-high';

// =============================================
// 대시보드 제어 API DTO (WcsDashboardControlController 1:1 대응)
// equipmentControlApi.ts 에서 import 하여 재export 한다.
// 백엔드 DTO 변경 시 여기 한 곳만 수정.
// =============================================

/**
 * 재고 1건 (tb_inventory_location + tb_inventory_stock + tb_inventory_item_mst)
 */
export interface InventoryItem {
  // ===== 기본 =====
  id: string;
  skuCode: string;
  /** LPN */
  palletId: string;
  qty: number;
  allocQty: number;
  /** 1=정상, 2=보류, 9=불량, 99=Mismatch */
  stockStatus: number;
  /** 재고 카테고리 (stock_type) */
  stockType?: string;
  /** tb_inventory_stock.stock_id — 셀 액션 메뉴에서 키로 사용 */
  stockId?: string;

  // ===== Stock 상세 (tb_inventory_stock) =====
  /** 상품 코드 */
  itemCode?: string;
  /** Lot 번호 */
  lotNo?: string;
  /** 화주 */
  itemOwner?: string;
  /** 입고 일시 "YYYY-MM-DD HH:MI:SS" */
  inbDatetime?: string;
  /** 유통기한 "YYYY-MM-DD" */
  expiredDate?: string;
  /** 제조일 "YYYY-MM-DD" */
  produceDate?: string;
  /** 우선순위 */
  itemPriority?: number;
  /** 재고 높이 */
  stockHeight?: number;
  /** attribute_a */
  attributeA?: string;

  // ===== Location 메타 =====
  /** 로케이션 타입 (RACK / INBOUND_PORT 등) */
  locType?: string;
  /** 포트 운영 모드 */
  portMode?: string;
  /** BCR 스캔 바코드 */
  scannedBarcode?: string;
}

/**
 * 대시보드 팝업 오픈 시 1회 조회되는 제어 정보
 */
export interface DashboardControlInfo {
  /** WCS 로케이션 코드 (inventory_location 내 key 값) (RACK 타입에서만 유효) */
  locCode: string | null;
  /** WCS 로케이션 Id (10501) (RACK 타입에서만 유효) */
  locId: string | null;
  /** 설비 그룹 ID */
  eqGroupId: string | null;
  /** 로케이션 사용 여부: 0=비가동, 1=사용 (RACK 전용) */
  locUseYn: number | null;
  /** 수동 잠금 여부: 0=해제, 1=잠금 (RACK 전용) */
  locLockYn: number | null;
  /** 잠금 주체 (MANUAL / 오더번호) */
  locLockBy: string | null;
  /**
   * 로케이션 상태: 0=EMPTY, 10=OCCUPIED, 20=LOCKED,
   * 30=EMPTY_PICK(공출고), 40=DOUBLE_ENTRY(이중입고), 90=DISABLED
   */
  locStatus: number | null;
  /** 설비 사용 여부 (CONVEYOR/SHUTTLE 전용) */
  eqUseYn: boolean | null;
  /** 현재 이 로케이션을 점유 중인 WCS 오더 키 (null이면 활성 오더 없음) */
  activeOrderKey: string | null;
  /** 활성 오더 상태 (10=SENT, 20=ACCEPTED, 30=RUNNING, 100+=ERROR) */
  activeOrderStatus: number | null;
  /** 활성 오더 타입 (11=입고, 12=출고, 13=이동 등) */
  activeOrderType: number | null;
  /**
   * 포트 락 holder — tb_inventory_location.task_id raw.
   * "DISPATCH_LOCK" sentinel 또는 셔틀 오더 키. null 이면 락 없음.
   * 강제 해제 호출 시 audit 용 (화면 미노출).
   */
  portLockTaskId?: string | null;
  /** 락 holder 셔틀 오더의 barcode. DISPATCH_LOCK 이면 null. */
  portLockBarcode?: string | null;
  /** 락 holder 셔틀 오더의 order_status. DISPATCH_LOCK 이면 null. */
  portLockOrderStatus?: number | null;
  /** 현재 재고 목록 (RACK 전용) */
  inventory: InventoryItem[];
}

/**
 * 대시보드 제어 API 공통 응답
 */
export interface DashboardControlResponse {
  success: boolean;
  message: string;
  data?: Record<string, any>;
}

/**
 * 포트 모드 — 운영자가 직접 보낼 수 있는 값
 */
export type OperatorPortMode = 'IDLE' | 'INBOUND' | 'OUTBOUND';

/**
 * 포트 모드 (백엔드 응답 포함 — SWITCHING_* 는 드레인 진행 상태)
 */
export type PortModeValueAll =
  | 'IDLE'
  | 'INBOUND'
  | 'OUTBOUND'
  | 'OUTBOUND_PRIORITY'
  | 'SWITCHING_TO_INBOUND'
  | 'SWITCHING_TO_OUTBOUND';

export interface PortModeChangeResult {
  previousMode?: string;
  /** 즉시 전환 성공 시 → 목표 모드, 드레인 진입 시 → SWITCHING_TO_* */
  currentMode?: string;
  errorCode?: string;
  errorDesc?: string;
}

// =============================================
// 셀 상태 (CellStateService.getCellsByGroup 응답)
// GET /rest/wcs/inventory/cell-state/cells?eq_group_id=...&level=...
// =============================================

/** 재고 축 상태 코드 (state_code) */
export type CellStateCode =
  | 'DRIVE'         // 주행 전용 (drive_only_yn=true)
  | 'PRODUCT'       // 정상 적재
  | 'EMPTY_BOX'     // 빈 박스 적재
  | 'OUTBOUND'      // 출고 작업중
  | 'INBOUND'       // 입고 작업중
  | 'INBOUND_READY' // 입고 대기
  | 'QC_PENDING'    // 시험 대기 — stock_type=QC_PENDING
  | 'QC_FAIL'       // 시험 부적합 — stock_type=QC_FAIL
  | 'NIA_PENDING'   // 국검 대기 — stock_type=NIA_PENDING
  | 'RETURN'        // 반품 — stock_type=RETURN
  | 'DISPOSAL'      // 폐기 — stock_type=DISPOSAL
  | 'EMPTY_OUT'     // 공출고 감지
  | 'DOUBLE_IN'     // 이중입고 감지
  | 'EMPTY';        // 비어 있음어

export interface CellStateInfo {
  /** tb_eq_rack_mst.rack_id (= cell ID) */
  rack_id: string;
  /** tb_eq_rack_mst.eq_id (소속 설비 ID) */
  eq_id: string;
  /** tb_eq_rack_mst.type — EcsDBConsts.RackType 와 매칭 */
  type: number;
  row: number;
  bay: number;
  level: number;
  /** 주행 전용 셀 여부 */
  drive_only_yn: boolean;
  /** ZONE (loc_group) */
  eq_group_id: string;
  /** "{eqGroup}-RR-BB-LL" 형식의 위치 코드 */
  stor_loc: string;
  /** 점유 중인 작업 ID */
  task_id: string | null;
  /** 점유 중인 재고 ID */
  stock_id: string | null;
  /** 재고 축 상태 */
  state_code: CellStateCode;
  /** 사용 금지 (LOCK) */
  locked: boolean;
  /** 입고 금지 */
  inbound_forbidden: boolean;
  /** 출고 금지 */
  outbound_forbidden: boolean;

  // ─ 분류·제약 축 (tb_inventory_location) ─────────────
  // NOT NULL 컬럼이므로 빈 문자열 / 0 이 "미설정" 의미
  /** 아이템 타입 (예: COLD, FROZEN, DRY ...) */
  item_type: string;
  /** 아이템 그룹 (예: ZONE_A, RETURN_ZONE ...) */
  item_group: string;
  /** 최대 적재 무게 (0 = 미설정/무제한) */
  max_weight: number;
  /** 최대 적재 높이 (0 = 미설정/무제한) */
  max_height: number;
}
