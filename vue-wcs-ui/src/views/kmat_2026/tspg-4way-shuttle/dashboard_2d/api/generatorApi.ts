/**
 * generatorApi.ts
 * 설비 생성 API (역매핑)
 *
 * ⚠️ 모든 하위 설비 관련 API 는 (eqGroupId + eqId) 쌍으로 식별
 *    tb_eq_mst 는 (eqGroupId, id) 복합 unique 이므로 eqId 단독으로는 유일하지 않음
 *
 * @author WCS Development Team
 * @since 2026-03-27
 */

import { defHttp } from '/@/utils/http/axios';
import type {
  EqGroupTypeValue,
  EqTypeValue,
  RackTypeValue,
  ConveyorTypeValue,
} from '../../constants/EcsDBConsts';

const BASE_URL = '/equipment-generator';

// ============================================
// 요청/응답 타입
// ============================================

export interface EqGroupCreateRequest {
  id: string;
  name: string;
  type?: EqGroupTypeValue;
}

export interface EqMstCreateRequest {
  id: string;
  eqGroupId: string;
  name: string;
  type: EqTypeValue;

  plcId?: string;
  plcName?: string;
  plcIp?: string;
  plcPort?: number;
  plcIfType?: string;
  plcEqType?: number;
  connectYn?: boolean;
  useYn?: boolean;
}

export interface RackCellCreateRequest {
  id: string;
  eqId: string;
  type?: RackTypeValue;
  row: number;
  bay: number;
  level: number;
  driveOnlyYn?: boolean;
  useYn?: boolean;
}

export interface SpecialCellConfig {
  row: number;
  bay: number;
  type: RackTypeValue;
  driveOnlyYn?: boolean;
}

/**
 * ⚠️ eqGroupId 필수 (로케이션 동기화 및 안전한 식별에 필요)
 *
 * ⭐ startLevel ~ endLevel 범위의 각 층마다 별도의 2D 페이지가 생성됨
 *    (한 층 = 한 페이지)
 */
export interface RackBulkCreateRequest {
  lcId?: string;
  eqGroupId: string;
  create2dItems?: boolean;

  eqId: string;

  /** ⭐ 시작 층 */
  startLevel: number;
  /** ⭐ 끝 층 (startLevel == endLevel 이면 단일 층) */
  endLevel: number;

  rackType?: RackTypeValue;
  createMode?: 'GRID';

  startRow: number;
  endRow: number;
  startBay: number;
  endBay: number;

  driveOnlyRows?: number[];
  driveOnlyBays?: number[];

  specialCells?: SpecialCellConfig[];

  useYn?: boolean;
}

export interface RackBulkCreateResult {
  createdCount: number;
  createdIds: string[];
}

export type CellOpKind = 'DISABLE' | 'ENABLE' | 'CREATE';

export interface CellOp {
  cellId: string;
  level: number;
  row: number;
  bay: number;
  kind: CellOpKind;
  rackType?: number;
}

export interface RackCellSyncRequest {
  eqGroupId: string;
  rackEqId: string;
  ops: CellOp[];
}

export interface RackCellSyncResult {
  disabled: number;
  enabled: number;
  created: number;
  rejectedCellIds: string[];
  rejectReasons: string[];
}

export interface CvMstCreateRequest {
  id: string;
  eqId: string;
  type: ConveyorTypeValue;
  level: number;
  useYn?: boolean;
  autoYn?: boolean;
}

export interface CarMstCreateRequest {
  id: string;
  eqId: string;
  type: string;
  row: number;
  bay: number;
  level: number;
  rackId?: string;
  rackEqId?: string;
  minRow: number;
  maxRow: number;
  autoYn?: boolean;
  useYn?: boolean;
  cargoYn?: boolean;
  completeYn?: boolean;
  plcCmdId?: number;
  plcCompCmdId?: number;
  batteryStatus?: number;
}

export interface EqMstDetailResponse {
  id: string;
  eqGroupId: string;
  name: string;
  type: EqTypeValue;
  plcId?: string;
  plcName?: string;
  plcIp?: string;
  plcPort?: number;
  plcIfType?: string;
  plcEqType?: number;
  connectYn?: boolean;
  useYn?: boolean;
}

// ============================================
// API
// ============================================

export const generatorApi = {
  // ============================================
  // 1. 설비 그룹
  // ============================================

  createEqGroup: (request: EqGroupCreateRequest) => {
    return defHttp.post({ url: `${BASE_URL}/eq-group`, data: request });
  },

  getEqGroups: () => {
    return defHttp.get<any[]>({ url: `${BASE_URL}/eq-groups` });
  },

  updateEqGroup: (id: string, request: Pick<EqGroupCreateRequest, 'name' | 'type'>) => {
    return defHttp.put({ url: `${BASE_URL}/eq-group/${id}`, data: request });
  },

  deleteEqGroup: (id: string) => {
    return defHttp.delete({ url: `${BASE_URL}/eq-group/${id}` });
  },

  // ============================================
  // 2. 기본 설비  ⭐ eqGroupId 포함
  // ============================================

  createEqMst: (request: EqMstCreateRequest) => {
    return defHttp.post({ url: `${BASE_URL}/eq-mst`, data: request });
  },

  getEqMstByGroup: (eqGroupId: string) => {
    return defHttp.get<any[]>({ url: `${BASE_URL}/eq-mst/group/${eqGroupId}` });
  },

  /** ⭐ (eqGroupId + id) 로 단건 상세 조회 */
  getEqMstDetail: (eqGroupId: string, id: string) => {
    return defHttp.get<EqMstDetailResponse>({
      url: `${BASE_URL}/eq-mst/group/${eqGroupId}/${id}`,
    });
  },

  /** ⭐ (eqGroupId + id) 로 수정 */
  updateEqMst: (
    eqGroupId: string,
    id: string,
    request: Omit<EqMstCreateRequest, 'id' | 'eqGroupId' | 'type'>,
  ) => {
    return defHttp.put({
      url: `${BASE_URL}/eq-mst/group/${eqGroupId}/${id}`,
      data: request,
    });
  },

  /** ⭐ (eqGroupId + id) 로 삭제 */
  deleteEqMst: (eqGroupId: string, id: string) => {
    return defHttp.delete({
      url: `${BASE_URL}/eq-mst/group/${eqGroupId}/${id}`,
    });
  },

  // ============================================
  // 3. 랙 셀  ⭐ eqGroupId 포함
  // ============================================

  createRackCell: (eqGroupId: string, request: RackCellCreateRequest) => {
    return defHttp.post({
      url: `${BASE_URL}/rack-cell/group/${eqGroupId}`,
      data: request,
    });
  },

  createRackCellsGrid: (request: RackBulkCreateRequest) => {
    return defHttp.post<RackBulkCreateResult>({
      url: `${BASE_URL}/rack-cells/grid`,
      data: request,
    });
  },

  /** MapEditor 셀 단위 ON/OFF/CREATE 일괄 동기화 */
  syncRackCells: (request: RackCellSyncRequest) => {
    return defHttp.put<RackCellSyncResult>({
      url: `${BASE_URL}/rack-cells/sync`,
      data: request,
    });
  },

  getRackCellsByEqId: (eqGroupId: string, eqId: string) => {
    return defHttp.get<any[]>({
      url: `${BASE_URL}/rack-cells/group/${eqGroupId}/eq/${eqId}`,
    });
  },

  getRackCellsByLevel: (eqGroupId: string, eqId: string, level: number) => {
    return defHttp.get<any[]>({
      url: `${BASE_URL}/rack-cells/group/${eqGroupId}/eq/${eqId}/level/${level}`,
    });
  },

  /** 단건 RACK 셀 조회 */
  getRackCell: (eqGroupId: string, eqId: string, id: string) => {
    return defHttp.get<any>({
      url: `${BASE_URL}/rack-cell/group/${eqGroupId}/eq/${eqId}/${id}`,
    });
  },

  /** 단건 RACK 셀 수정 (각 필드 optional, null 이면 변경 안 함) */
  updateRackCell: (
    eqGroupId: string,
    eqId: string,
    id: string,
    request: Partial<{
      type: number;
      row: number;
      bay: number;
      level: number;
      skuId: string;
      skuQty: number;
      useYn: boolean;
      cargoYn: boolean;
      bufferYn: boolean;
      driveOnlyYn: boolean;
    }>,
  ) => {
    return defHttp.put<any>({
      url: `${BASE_URL}/rack-cell/group/${eqGroupId}/eq/${eqId}/${id}`,
      data: request,
    });
  },

  deleteRackCell: (eqGroupId: string, eqId: string, id: string) => {
    return defHttp.delete({
      url: `${BASE_URL}/rack-cell/group/${eqGroupId}/eq/${eqId}/${id}`,
    });
  },

  deleteRackCellsByEqId: (eqGroupId: string, eqId: string) => {
    return defHttp.delete({
      url: `${BASE_URL}/rack-cells/group/${eqGroupId}/eq/${eqId}`,
    });
  },

  // ============================================
  // 4. 컨베이어/리프터  ⭐ eqGroupId 포함
  // ============================================

  createCvMst: (eqGroupId: string, request: CvMstCreateRequest) => {
    return defHttp.post({
      url: `${BASE_URL}/cv-mst/group/${eqGroupId}`,
      data: request,
    });
  },

  getCvMstByEqId: (eqGroupId: string, eqId: string) => {
    return defHttp.get<any[]>({
      url: `${BASE_URL}/cv-mst/group/${eqGroupId}/eq/${eqId}`,
    });
  },

  /** 단건 CONVEYOR 마스터 조회 */
  getCvMst: (eqGroupId: string, eqId: string, id: string) => {
    return defHttp.get<any>({
      url: `${BASE_URL}/cv-mst/group/${eqGroupId}/eq/${eqId}/${id}`,
    });
  },

  updateCvMst: (
    eqGroupId: string,
    eqId: string,
    id: string,
    request: Omit<CvMstCreateRequest, 'id' | 'eqId'>,
  ) => {
    return defHttp.put({
      url: `${BASE_URL}/cv-mst/group/${eqGroupId}/eq/${eqId}/${id}`,
      data: request,
    });
  },

  deleteCvMst: (eqGroupId: string, eqId: string, id: string) => {
    return defHttp.delete({
      url: `${BASE_URL}/cv-mst/group/${eqGroupId}/eq/${eqId}/${id}`,
    });
  },

  // ============================================
  // 5. 셔틀카  ⭐ eqGroupId 포함
  // ============================================

  createCarMst: (eqGroupId: string, request: CarMstCreateRequest) => {
    return defHttp.post({
      url: `${BASE_URL}/car-mst/group/${eqGroupId}`,
      data: request,
    });
  },

  getCarMstByEqId: (eqGroupId: string, eqId: string) => {
    return defHttp.get<any[]>({
      url: `${BASE_URL}/car-mst/group/${eqGroupId}/eq/${eqId}`,
    });
  },

  updateCarMst: (
    eqGroupId: string,
    eqId: string,
    id: string,
    request: Omit<CarMstCreateRequest, 'id' | 'eqId'>,
  ) => {
    return defHttp.put({
      url: `${BASE_URL}/car-mst/group/${eqGroupId}/eq/${eqId}/car/${id}`,
      data: request,
    });
  },

  deleteCarMst: (eqGroupId: string, eqId: string, id: string) => {
    return defHttp.delete({
      url: `${BASE_URL}/car-mst/group/${eqGroupId}/eq/${eqId}/car/${id}`,
    });
  },

  /** 기존 랙 데이터로 2D page/item + inventory_location 역생성 */
  generate2dFromExistingRacks: (lcId: string, eqGroupId: string, eqId: string) => {
    return defHttp.post<{ createdItems: number; createdLocations: number }>({
      url: `/equipment-generator/dashboard-2d/generate?lcId=${encodeURIComponent(lcId)}&eqGroupId=${encodeURIComponent(eqGroupId)}&eqId=${encodeURIComponent(eqId)}`,
    });
  },

  // ============================================
  // 6. tb_inventory_location 단건 GET/PUT
  //    3-tuple key: locGroup + rackEqId + locId
  //    locGroup = eqGroupId, rackEqId = tb_eq_mst.id, locId = tb_eq_rack_mst.rack_id
  // ============================================

  /** 단건 location 조회 (locGroup + rackEqId + locId) */
  getLocation: (locGroup: string, rackEqId: string, locId: string) => {
    return defHttp.get<any>({
      url: `${BASE_URL}/inventory-location/group/${locGroup}/eq/${rackEqId}/${locId}`,
    });
  },

  /** 단건 location 수정 (각 필드 optional, null 이면 변경 안 함) */
  updateLocation: (
    locGroup: string,
    rackEqId: string,
    locId: string,
    request: Partial<{
      itemType: string;
      itemGroup: string;
      itemGrade: number;
      maxHeight: number;
      maxWeight: number;
      locDeep: number;
      locSide: string;
      isEnabled: boolean;
      isInboundEnabled: boolean;
      isOutboundEnabled: boolean;
      isPath: boolean;
      equipType: string;
      equipCode: string;
      destNodeCode: string;
    }>,
  ) => {
    return defHttp.put<any>({
      url: `${BASE_URL}/inventory-location/group/${locGroup}/eq/${rackEqId}/${locId}`,
      data: request,
    });
  },
};

export default generatorApi;
