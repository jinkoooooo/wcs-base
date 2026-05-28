/**
 * equipmentControlApi.ts
 * WCS 대시보드 팝업 수동 제어 API
 *
 * [역할 분리 원칙]
 * - ecsApi.ts: ECS(Equipment Control System) 향 제어 명령 (셔틀 정지/이동 등)
 * - equipmentControlApi.ts: WCS(Warehouse Control System) 향 제어 명령
 *   (로케이션 use/lock, 재고 정합성, 오더 흐름 제어)
 *
 * 이 파일은 Backend WcsDashboardControlController 와 1:1 대응한다.
 */

import { defHttp } from '/@/utils/http/axios';
import { keysToCamel } from '../utils/case';
import type {
  DashboardControlInfo,
  DashboardControlResponse,
  InventoryItem,
  OperatorPortMode,
  PortModeChangeResult,
  PortModeValueAll,
} from './types';

// 타입은 단일 출처(types.ts)에서 정의하고 여기서는 재export 만 한다.
// 백엔드 DTO 변경 시 types.ts 한 곳만 수정하면 된다.
export type {
  DashboardControlInfo,
  DashboardControlResponse,
  InventoryItem,
  OperatorPortMode,
  PortModeChangeResult,
};
export type PortMode = PortModeValueAll;

const BASE = '/wcs/tspg-4way/dashboard-control';

// ============================================================
// 내부 HTTP 헬퍼
// ============================================================

const httpGet = async <T>(url: string): Promise<T> => {
  const res = await defHttp.get({ url }, { isTransformResponse: false });
  return keysToCamel<T>(res);
};

const httpPost = async <T>(url: string, data?: any): Promise<T> => {
  const res = await defHttp.post({ url, data: data ?? {} }, { isTransformResponse: false });
  return keysToCamel<T>(res);
};

const httpDelete = async <T>(url: string): Promise<T> => {
  const res = await defHttp.delete({ url }, { isTransformResponse: false });
  return keysToCamel<T>(res);
};

const httpPut = async <T>(url: string, data?: any): Promise<T> => {
  const res = await defHttp.put({ url, data: data ?? {} }, { isTransformResponse: false });
  return keysToCamel<T>(res);
};

// ============================================================
// [1] 제어 정보 조회 API
// ============================================================

/**
 * 랙 셀 제어 정보 조회
 * 팝업이 열릴 때 1회 호출 → use/lock 상태 + 재고 목록 반환
 *
 * @param rackCellId TbEcs2dItem.realEqId (RACK 타입)
 */
export function getRackControlInfo(
  eqGroupId: string,
  rackCellId: string,
): Promise<DashboardControlInfo> {
  return httpGet(
    `${BASE}/rack/${encodeURIComponent(eqGroupId)}/${encodeURIComponent(rackCellId)}/info`,
  );
}

/**
 * 비랙 설비 제어 정보 조회 (CONVEYOR / LIFTER / SHUTTLE)
 *
 * @param eqType  설비 타입 코드
 * @param eqId    TbEcs2dItem.realEqId
 */
export function getEquipmentControlInfo(
  eqType: string,
  eqId: string,
): Promise<DashboardControlInfo> {
  return httpGet(
    `${BASE}/equipment/${encodeURIComponent(eqType)}/${encodeURIComponent(eqId)}/info`,
  );
}

// ============================================================
// [2] Use/Disable 제어 API
// ============================================================

/**
 * 랙 로케이션 사용 여부 토글 (가동 ↔ 비가동)
 */
export function toggleLocUse(
  eqGroupId: string,
  locCode: string,
): Promise<DashboardControlResponse> {
  return httpPost(
    `${BASE}/loc/${encodeURIComponent(eqGroupId)}/${encodeURIComponent(locCode)}/toggle-use`,
  );
}

/**
 * 비랙 설비 사용 여부 토글 (CONVEYOR / LIFTER / SHUTTLE)
 */
export function toggleEquipmentUse(
  eqType: string,
  eqId: string,
): Promise<DashboardControlResponse> {
  return httpPost(
    `${BASE}/equipment/${encodeURIComponent(eqType)}/${encodeURIComponent(eqId)}/toggle-use`,
  );
}

// ============================================================
// [3] 수동 Lock/Unlock API (RACK 전용)
// ============================================================

/**
 * 로케이션 수동 잠금 (MANUAL lock)
 * 현장 점검 / 실사 / 물리적 장애 시 신규 오더 배정 차단용
 */
export function manualLock(eqGroupId: string, locCode: string): Promise<DashboardControlResponse> {
  return httpPost(
    `${BASE}/loc/${encodeURIComponent(eqGroupId)}/${encodeURIComponent(locCode)}/lock`,
  );
}

/**
 * 로케이션 수동 잠금 해제
 */
export function manualUnlock(
  eqGroupId: string,
  locCode: string,
): Promise<DashboardControlResponse> {
  return httpPost(
    `${BASE}/loc/${encodeURIComponent(eqGroupId)}/${encodeURIComponent(locCode)}/unlock`,
  );
}

// ============================================================
// [4] 재고 정합성 제어 API
// ============================================================

/**
 * 수동 재고 삭제 — 공출고(Empty Pick) 복구
 * 시스템에 재고 있고 실물 없는 상태 → WCS 재고 레코드 삭제 + 로케이션 EMPTY 복원
 */
export function deleteInventory(
  eqGroupId: string,
  locCode: string,
): Promise<DashboardControlResponse> {
  return httpDelete(
    `${BASE}/inventory/${encodeURIComponent(eqGroupId)}/${encodeURIComponent(locCode)}`,
  );
}

/**
 * 수동 재고 생성 — 이중입고(Double Entry) 복구
 * 실물은 있는데 시스템에 없는 상태 → SKU/LPN 입력 후 WCS 재고 레코드 생성
 */
export function createInventory(
  eqGroupId: string,
  locCode: string,
  skuCode: string,
  palletId: string,
  qty: number,
  ownerCode?: string,
): Promise<DashboardControlResponse> {
  return httpPost(
    `${BASE}/inventory/${encodeURIComponent(eqGroupId)}/${encodeURIComponent(locCode)}`,
    { skuCode, palletId, qty, ownerCode: ownerCode ?? '' },
  );
}

/**
 * 라인 단위 수량 보정 — 채취/박스출고 오기입 보정
 * newQty=0 → 비활성(EMPTY 복원). 사유(comment) 필수.
 */
export function adjustInventoryQty(
  eqGroupId: string,
  locCode: string,
  stockRowId: string,
  newQty: number,
  comment: string,
): Promise<DashboardControlResponse> {
  return httpPost(
    `${BASE}/inventory/${encodeURIComponent(eqGroupId)}/${encodeURIComponent(
      locCode,
    )}/stock/${encodeURIComponent(stockRowId)}/adjust-qty`,
    { newQty, comment },
  );
}

// ============================================================
// [5] 작업 흐름 제어 API
// ============================================================

/**
 * 작업 강제 완료 (Force Complete)
 * 실물은 이동 완료됐으나 WCS 상태가 멈춘 경우 사용
 */
export function forceCompleteOrder(orderKey: string): Promise<DashboardControlResponse> {
  return httpPost(`${BASE}/orders/${encodeURIComponent(orderKey)}/force-complete`);
}

/**
 * 작업 취소 (WCS 레벨 Cancel)
 * 로케이션 락 해제 + 오더 상태 CANCELLED(91)로 변경
 */
export function cancelOrderWcs(
  orderKey: string,
  reason?: string,
): Promise<DashboardControlResponse> {
  return httpPost(`${BASE}/orders/${encodeURIComponent(orderKey)}/cancel`, {
    reason: reason ?? '',
  });
}

/**
 * 작업 재개 (Resume — 에러 상태 오더 재전송 트리거)
 * 에러(100+) 상태 오더를 SENT(10)으로 복원 → 스케줄러가 ECS 재전송
 */
export function resumeOrderWcs(orderKey: string): Promise<DashboardControlResponse> {
  return httpPost(`${BASE}/orders/${encodeURIComponent(orderKey)}/resume`);
}

// ============================================================
// [wcs-ops Step 13/15] 포트 모드 전환 API
// 백엔드: PUT /rest/wcs/port/{portCode}/mode (PortAdminController)
// ============================================================

export async function changePortMode(
  eqGroupId: string,
  portCode: string,
  portMode: OperatorPortMode,  // ← SWITCHING_* 는 직접 못 보냄
  reason: string,
  operator?: string,
): Promise<{ success: boolean; message: string; data?: PortModeChangeResult; isDraining?: boolean }> {
  try {
    const data = await httpPut<PortModeChangeResult>(
      `/wcs/port/${encodeURIComponent(portCode)}/mode`,
      {
        eqGroupId,
        portMode,
        operator: operator ?? 'UNKNOWN',
        reason,
      },
    );
    if (data?.errorCode) {
      return { success: false, message: data.errorDesc ?? data.errorCode, data };
    }
    // 드레인 진입 여부 판정 — 응답의 currentMode 가 SWITCHING_* 면 즉시 완료가 아님
    const isDraining = !!data?.currentMode?.startsWith('SWITCHING_');
    const message = isDraining
      ? '진행중 작업 완료 후 자동 전환됩니다 (드레인 시작)'
      : '전환 완료';
    return { success: true, message, data, isDraining };
  } catch (e: any) {
    return { success: false, message: e?.message ?? '전환 실패' };
  }
}

// ============================================================
// 포트 강제 락 해제
// 백엔드: POST /rest/wcs/admin/port/{portCode}/unlock (PortAdminController.forceUnlock)
// task_id 를 무조건 NULL 로 만든다 — 진행중 작업 정합성은 호출 측이 확인.
// ============================================================

export async function forceUnlockPort(
  eqGroupId: string,
  portCode: string,
  reason: string,
  operator?: string,
): Promise<{ success: boolean; message: string; data?: Record<string, any> }> {
  try {
    const data = await httpPost<Record<string, any>>(
      `/wcs/admin/port/${encodeURIComponent(portCode)}/unlock`,
      {
        eqGroupId,
        operator: operator ?? 'UNKNOWN',
        reason,
      },
    );
    return { success: true, message: '포트 락 해제 완료', data };
  } catch (e: any) {
    return { success: false, message: e?.message ?? '포트 락 해제 실패' };
  }
}

// ============================================================
// 통합 API 객체
// ============================================================

export const equipmentControlApi = {
  // 조회
  getRackControlInfo,
  getEquipmentControlInfo,

  // Use/Disable
  toggleLocUse,
  toggleEquipmentUse,

  // Lock/Unlock
  manualLock,
  manualUnlock,

  // 재고 정합성
  deleteInventory,
  createInventory,
  adjustInventoryQty,

  // 오더 제어
  forceCompleteOrder,
  cancelOrderWcs,
  resumeOrderWcs,

  // 포트 모드
  changePortMode,
  forceUnlockPort,
};

export default equipmentControlApi;
