/**
 * 4-Way Shuttle WCS API
 *
 * - 레이아웃 페이지/설비 타입/설비 배치 관리 (2D 에디터용)
 * - 실운영 설비 연동 (TbEqGroupMst, TbEqMst, TbEqCarMst, TbEqRackMst)
 * - 실시간 상태 조회 및 작업 제어 (TbWcsShuttleOrder)
 */
import { defHttp } from '/@/utils/http/axios';
import { getCommonPostApi } from '/@/api/common/api';

import type {
  TbEcs2dPage,
  TbEcs2dItemType,
  TbEcs2dItem,
  // 실운영 설비 타입
  TbEqGroupMst,
  TbEqMst,
  TbEqRackMst,
  TbWcsShuttleOrder,
  LayoutWithRealStatus,
  OrderControlResponse,
} from './types';

import { keysToCamel, keysToSnake } from '../utils/case';

const BASE_URL = '';

// API 헬퍼 함수 (body로 전송)
const httpGet = <T>(url: string, params?: any): Promise<T> => {
  return defHttp.get({ url, params }, { isTransformResponse: false });
};

const httpPost = <T>(url: string, data?: any): Promise<T> => {
  return getCommonPostApi(url, data);
};

const httpPut = <T>(url: string, data?: any): Promise<T> => {
  return defHttp.put({ url, data }, { isTransformResponse: false });
};

// backend(snake_case) ↔ frontend(camelCase) 변환 래퍼
const httpGetC = async <T>(url: string, params?: any): Promise<T> => {
  const res = await httpGet<any>(url, params ? keysToSnake(params) : undefined);
  return keysToCamel<T>(res);
};

const httpPostC = async <T>(url: string, data?: any): Promise<T> => {
  const res = await httpPost<any>(url, data ? keysToSnake(data) : undefined);
  return keysToCamel<T>(res);
};

const httpPutC = async <T>(url: string, data?: any): Promise<T> => {
  const res = await httpPut<any>(url, data ? keysToSnake(data) : undefined);
  return keysToCamel<T>(res);
};

// ============================================
// 레이아웃 페이지 API (2D 에디터용)
// ============================================
export const layoutPageApi = {
  // 센터별 페이지 목록 조회
  getPages(lcId: string): Promise<TbEcs2dPage[]> {
    return httpGetC(`${BASE_URL}/tb_ecs_2d_page/${lcId}`);
  },

  // 페이지 단건 조회
  getPage(id: string): Promise<TbEcs2dPage> {
    return httpGetC(`${BASE_URL}/tb_ecs_2d_page/detail/${id}`);
  },

  // 페이지 생성
  createPage(page: Partial<TbEcs2dPage>): Promise<TbEcs2dPage> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_page/create`, page);
  },

  // 페이지 수정
  updatePage(page: TbEcs2dPage): Promise<TbEcs2dPage> {
    return httpPutC(`${BASE_URL}/tb_ecs_2d_page/update`, page);
  },

  // 페이지 이름 수정
  updatePageName(id: string, pageName: string): Promise<TbEcs2dPage> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_page/update_name`, { id, pageName });
  },

  // 페이지 캔버스 설정 수정
  updatePageCanvas(
    id: string,
    canvasWidth: number,
    canvasHeight: number,
    backgroundColor: string,
  ): Promise<TbEcs2dPage> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_page/update_canvas`, {
      id,
      canvasWidth,
      canvasHeight,
      backgroundColor,
    });
  },

  // 페이지 삭제
  deletePage(id: string): Promise<boolean> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_page/delete`, { id });
  },

  // 페이지 순서 변경
  updatePageIndex(id: string, pageIndex: number): Promise<boolean> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_page/update_index`, { id, pageIndex });
  },

  // 페이지 복사 (층 복사용)
  copyPage(
    sourcePageId: string,
    newPageName: string,
    newFloorLevel?: number,
  ): Promise<TbEcs2dPage> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_page/copy`, {
      sourcePageId,
      newPageName,
      newFloorLevel,
    });
  },

  // 설비그룹별 페이지 목록 조회
  getPagesByEqGroup(lcId: string, eqGroupId: string): Promise<TbEcs2dPage[]> {
    return httpGetC(`${BASE_URL}/tb_ecs_2d_page/${lcId}/eq_group/${eqGroupId}`);
  },

  // 설비그룹+층별 페이지 목록 조회
  getPagesByEqGroupAndFloor(
    lcId: string,
    eqGroupId: string,
    floor: number,
  ): Promise<TbEcs2dPage[]> {
    return httpGetC(`${BASE_URL}/tb_ecs_2d_page/${lcId}/eq_group/${eqGroupId}/floor/${floor}`);
  },

  // 설비그룹별 층 목록 조회
  getFloorsByEqGroup(lcId: string, eqGroupId: string): Promise<number[]> {
    return httpGetC(`${BASE_URL}/tb_ecs_2d_page/${lcId}/eq_group/${eqGroupId}/floors`);
  },

  // 페이지 설비그룹 매핑 업데이트
  updatePageEqGroup(id: string, eqGroupId: string): Promise<TbEcs2dPage> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_page/update_eq_group`, { id, eqGroupId });
  },

  // 페이지 설비그룹 매핑 해제
  clearPageEqGroup(id: string): Promise<TbEcs2dPage> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_page/clear_eq_group`, { id });
  },

  // 매핑되지 않은 페이지 목록 조회
  getUnmappedPages(lcId: string): Promise<TbEcs2dPage[]> {
    return httpGetC(`${BASE_URL}/tb_ecs_2d_page/${lcId}/unmapped`);
  },
};

// ============================================
// 설비 타입 API (2D 에디터용 아이콘/타입 정의)
// ============================================
export const equipmentTypeApi = {
  // 센터별 설비 타입 목록 조회
  getTypes(lcId: string): Promise<TbEcs2dItemType[]> {
    return httpGetC(`${BASE_URL}/shuttle_equipment_types/${lcId}`);
  },

  // 카테고리별 설비 타입 목록 조회
  getTypesByCategory(lcId: string, category: string): Promise<TbEcs2dItemType[]> {
    return httpGetC(`${BASE_URL}/shuttle_equipment_types/${lcId}/category/${category}`);
  },

  // 설비 타입 단건 조회
  getType(id: string): Promise<TbEcs2dItemType> {
    return httpGetC(`${BASE_URL}/shuttle_equipment_types/detail/${id}`);
  },

  // 설비 타입 생성
  createType(type: Partial<TbEcs2dItemType>): Promise<TbEcs2dItemType> {
    return httpPostC(`${BASE_URL}/shuttle_equipment_types/create`, type);
  },

  // 설비 타입 수정
  updateType(type: TbEcs2dItemType): Promise<TbEcs2dItemType> {
    return httpPutC(`${BASE_URL}/shuttle_equipment_types/update`, type);
  },

  // 설비 타입 아이콘 업데이트
  updateTypeIcon(id: string, iconData2d: string): Promise<TbEcs2dItemType> {
    return httpPostC(`${BASE_URL}/shuttle_equipment_types/update_icon`, { id, iconData2d });
  },

  // 설비 타입 삭제
  deleteType(id: string): Promise<boolean> {
    return httpPostC(`${BASE_URL}/shuttle_equipment_types/delete`, { id });
  },

  // 기본 설비 타입 초기화
  initializeDefaultTypes(lcId: string): Promise<boolean> {
    return httpPostC(`${BASE_URL}/shuttle_equipment_types/initialize`, { lcId });
  },

  // DEFAULT 마스터 → targetLcId 복제
  cloneFromDefault(targetLcId: string): Promise<{ clonedCount: number; targetLcId: string }> {
    return httpPostC(`${BASE_URL}/shuttle_equipment_types/clone`, { targetLcId });
  },

  // 일괄 저장 (Upsert) - 로컬 SVG + Config 한번에 등록
  saveBatch(lcId: string, types: Partial<TbEcs2dItemType>[]): Promise<TbEcs2dItemType[]> {
    return httpPostC(`${BASE_URL}/shuttle_equipment_types/save_batch`, { lcId, types });
  },

  // 설비 타입 수정 (전체 필드)
  updateTypeFull(type: TbEcs2dItemType): Promise<TbEcs2dItemType> {
    return httpPutC(`${BASE_URL}/shuttle_equipment_types/update`, type);
  },
};

// ============================================
// 설비 레이아웃 API (2D 에디터 배치 정보)
// ============================================
export const equipmentLayoutApi = {
  // 페이지별 레이아웃 목록 조회
  getLayouts(lcId: string, pageId: string): Promise<TbEcs2dItem[]> {
    return httpGetC(`${BASE_URL}/tb_ecs_2d_item/${lcId}/${pageId}`);
  },

  // 레이아웃 단건 조회
  getLayout(id: string): Promise<TbEcs2dItem> {
    return httpGetC(`${BASE_URL}/tb_ecs_2d_item/detail/${id}`);
  },

  // 레이아웃 생성
  createLayout(layout: Partial<TbEcs2dItem>): Promise<TbEcs2dItem> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_item/create`, layout);
  },

  // 레이아웃 수정
  updateLayout(layout: TbEcs2dItem): Promise<TbEcs2dItem> {
    return httpPutC(`${BASE_URL}/tb_ecs_2d_item/update`, layout);
  },

  // 레이아웃 위치 수정
  updateLayoutPosition(id: string, posX: number, posY: number): Promise<TbEcs2dItem> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_item/update_position`, {
      id,
      posX,
      posY,
    });
  },

  // 레이아웃 크기 수정
  updateLayoutSize(id: string, width: number, height: number): Promise<TbEcs2dItem> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_item/update_size`, {
      id,
      width,
      height,
    });
  },

  // 레이아웃 변형 수정
  updateLayoutTransform(
    id: string,
    rotation?: number,
    scaleX?: number,
    scaleY?: number,
    flipH?: boolean,
    flipV?: boolean,
  ): Promise<TbEcs2dItem> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_item/update_transform`, {
      id,
      rotation,
      scaleX,
      scaleY,
      flipH,
      flipV,
    });
  },

  // 레이아웃 삭제
  deleteLayout(id: string): Promise<boolean> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_item/delete`, { id });
  },

  // 페이지 전체 레이아웃 일괄 저장
  saveAllLayouts(lcId: string, pageId: string, layouts: TbEcs2dItem[]): Promise<boolean> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_item/save_all`, { lcId, pageId, layouts });
  },

  // 레이아웃 일괄 업데이트
  batchUpdateLayouts(layouts: TbEcs2dItem[]): Promise<boolean> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_item/batch_update`, layouts);
  },

  // 실운영 설비 ID 매핑
  updateRealEqMapping(id: string, realEqId: string, realEqType: string): Promise<TbEcs2dItem> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_item/update_real_eq_mapping`, {
      id,
      realEqId,
      realEqType,
    });
  },

  // 실운영 설비 ID 매핑 해제
  clearRealEqMapping(id: string): Promise<TbEcs2dItem> {
    return httpPostC(`${BASE_URL}/tb_ecs_2d_item/clear_real_eq_mapping`, { id });
  },

  // 대시보드용 레이아웃 조회 (실시간 상태 결합)
  getLayoutsWithRealStatus(lcId: string, pageId: string): Promise<LayoutWithRealStatus[]> {
    return httpGetC(`${BASE_URL}/tb_ecs_2d_item/${lcId}/${pageId}/with_real_status`);
  },

  // realEqId로 레이아웃 조회
  getLayoutByRealEqId(lcId: string, pageId: string, realEqId: string): Promise<TbEcs2dItem> {
    return httpGetC(`${BASE_URL}/tb_ecs_2d_item/${lcId}/${pageId}/by_real_eq/${realEqId}`);
  },

  // 실운영 설비가 매핑된 레이아웃 목록 조회
  getMappedLayouts(lcId: string, pageId: string): Promise<TbEcs2dItem[]> {
    return httpGetC(`${BASE_URL}/tb_ecs_2d_item/${lcId}/${pageId}/mapped`);
  },
};

// ============================================
// 설비 그룹 API (실운영 TbEqGroupMst)
// ============================================
export const eqGroupApi = {
  // 센터별 설비 그룹 목록 조회
  getGroupsByLcId(lcId: string): Promise<TbEqGroupMst[]> {
    return httpGetC(`${BASE_URL}/tb_eq_group_mst/${lcId}`);
  },

  getGroupsAll(): Promise<TbEqGroupMst[]> {
    return httpGetC(`${BASE_URL}/tb_eq_group_mst/all`);
  },

  // 설비 그룹 타입별 목록 조회
  getGroupsByType(lcId: string, eqGroupType: string): Promise<TbEqGroupMst[]> {
    return httpGetC(`${BASE_URL}/tb_eq_group_mst/${lcId}/type/${eqGroupType}`);
  },

  // 설비 그룹 단건 조회
  getGroup(lcId: string, eqGroupId: string): Promise<TbEqGroupMst> {
    return httpGetC(`${BASE_URL}/tb_eq_group_mst/${lcId}/group/${eqGroupId}`);
  },

  // 설비 그룹별 층 목록 조회
  getFloorsByEqGroup(lcId: string, eqGroupId: string): Promise<number[]> {
    return httpGetC(`${BASE_URL}/tb_eq_group_mst/${lcId}/group/${eqGroupId}/floors`);
  },
};

// ============================================
// 실운영 설비 마스터 API (TbEqMst)
// ============================================
export const realEqMstApi = {
  // 설비 그룹별 설비 목록 조회
  getEquipmentsByGroup(eqGroupId: string): Promise<TbEqMst[]> {
    return httpGetC(`${BASE_URL}/tb_eq_mst/group/${eqGroupId}`);
  },

  // 설비 그룹+타입별 설비 목록 조회
  getEquipmentsByGroupAndType(eqGroupId: string, eqType: string): Promise<TbEqMst[]> {
    return httpGetC(`${BASE_URL}/tb_eq_mst/group/${eqGroupId}/type/${eqType}`);
  },

  // 타입별 설비 목록 조회 (매핑 콤보박스용)
  getEquipmentsByEqGroupIdAndEqType(eqGroupId: string, eqType: number): Promise<TbEqMst[]> {
    console.log('getEquipmentsByEqGroupIdAndEqType');
    console.log(`${BASE_URL}/realtime/equipments/group/${eqGroupId}/type/${eqType}`);
    return httpGetC(`${BASE_URL}/realtime/equipments/group/${eqGroupId}/type/${eqType}`);
  },

  // 설비 단건 조회
  getEquipment(eqId: string): Promise<TbEqMst> {
    return httpGetC(`${BASE_URL}/tb_eq_mst/eq/${eqId}`);
  },

  // PLC별 설비 목록 조회
  getEquipmentsByPlc(plcId: string): Promise<TbEqMst[]> {
    return httpGetC(`${BASE_URL}/tb_eq_mst/plc/${plcId}`);
  },
};

// ============================================
// 실시간 상태 API (Dashboard2D/Editor용)
// - WCS 작업 상태 조회/제어
// - 랙 셀 조회 (에디터 매핑용)
// - 대시보드 초기 데이터
// ============================================
export const realTimeApi = {
  // ---- WCS 작업 상태 (TbWcsShuttleOrder) ----
  // 활성 WCS 작업 목록 조회 (TaskGrid용)
  getActiveOrders(): Promise<TbWcsShuttleOrder[]> {
    return httpGetC(`${BASE_URL}/realtime/orders/active`);
  },

  // 에러 상태 작업 목록 조회
  getErrorOrders(): Promise<TbWcsShuttleOrder[]> {
    return httpGetC(`${BASE_URL}/realtime/orders/errors`);
  },

  // ---- 랙 셀 조회 (에디터 매핑용) ----
  // 설비그룹 + 층별 랙 셀 목록 조회 (매핑 드롭다운용)
  getRackCellsByGroupAndFloor(eqGroupId: string, floor: number): Promise<TbEqRackMst[]> {
    return httpGetC(`${BASE_URL}/realtime/racks/group/${eqGroupId}/floor/${floor}`);
  },

  // ---- 대시보드 초기 데이터 ----
  // 대시보드 초기 데이터 통합 조회 (셔틀 + 화물 + 작업)
  getDashboardInitialData(
    lcId: string,
    pageId: string,
  ): Promise<{
    shuttles: any[];
    cargos: any[];
    orders: TbWcsShuttleOrder[];
    shuttleCount: number;
    cargoCount: number;
    orderCount: number;
  }> {
    return httpGetC(`${BASE_URL}/realtime/dashboard/initial/${lcId}/${pageId}`);
  },

  // ---- 작업 제어 ----
  // 작업 취소 요청
  cancelOrder(orderKey: string, reason?: string): Promise<OrderControlResponse> {
    return httpPostC(`${BASE_URL}/realtime/orders/${orderKey}/cancel`, { reason });
  },

  // 작업 재개 요청
  resumeOrder(orderKey: string): Promise<OrderControlResponse> {
    return httpPostC(`${BASE_URL}/realtime/orders/${orderKey}/resume`, {});
  },
};

// ============================================
// 가상 시뮬레이션 API
// ============================================
export const simulationApi = {
  // 가상 설비 데이터 생성
  generateVirtualEquipment(
    lcId: string,
    eqGroupId: string,
    floors = 3,
    shuttlesPerFloor = 2,
    rackRows = 20,
    rackCols = 20,
  ): Promise<{ success: boolean; carCount: number; rackCount: number; cvCount: number }> {
    return httpPostC(`${BASE_URL}/simulation/seed`, {
      lcId,
      eqGroupId,
      floors,
      shuttlesPerFloor,
      rackRows,
      rackCols,
    });
  },

  // Layout과 가상 설비 자동 매핑
  autoMapLayouts(lcId: string, pageId: string): Promise<{ success: boolean; mappedCount: number }> {
    return httpPostC(`${BASE_URL}/simulation/auto-map`, { lcId, pageId });
  },

  // 가상 데이터 삭제
  clearVirtualData(eqGroupId: string): Promise<{ success: boolean; message: string }> {
    return defHttp.delete(
      { url: `${BASE_URL}/simulation/clear`, params: { eqGroupId } },
      { isTransformResponse: false },
    );
  },

  // 시뮬레이션 시작
  startSimulation(lcId: string, eqGroupId: string): Promise<{ success: boolean; message: string }> {
    return httpPostC(`${BASE_URL}/simulation/start`, { lcId, eqGroupId });
  },

  // 시뮬레이션 중지
  stopSimulation(): Promise<{ success: boolean; message: string }> {
    return httpPostC(`${BASE_URL}/simulation/stop`, {});
  },

  // 시뮬레이션 상태 조회
  getSimulationStatus(): Promise<{
    isSimulating: boolean;
    shuttleCount: number;
    activeOrderCount: number;
  }> {
    return httpGetC(`${BASE_URL}/simulation/status`);
  },

  // 전체 초기화 및 시뮬레이션 시작 (원클릭)
  initAndStart(
    lcId: string,
    eqGroupId: string,
    pageId: string,
    floors = 3,
    shuttlesPerFloor = 2,
    rackRows = 20,
    rackCols = 20,
  ): Promise<{
    success: boolean;
    seedResult: any;
    mapResult: any;
    message: string;
  }> {
    return httpPostC(`${BASE_URL}/simulation/init-and-start`, {
      lcId,
      eqGroupId,
      pageId,
      floors,
      shuttlesPerFloor,
      rackRows,
      rackCols,
    });
  },
};
