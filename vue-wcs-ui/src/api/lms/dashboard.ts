import { getCommonGetApi, getSearchList } from "@/api/common/api";

//TODO: 유효한 엔드포인트로 변경
enum Api {
  // note: 유효하지 않은 엔드포인트
  Kpi = '/',
  Throughput = '/',
  InventoryFlow = '/',
  InboundFulfillment = '/',
  OutboundFulfillment = '/',
  InventoryFulfillment = '/',
  RecentOrders = '/',
  TargetVsActual = '/',

  // 유효한 엔드포인트
  CenterInfo = '/centers_users/current_user/detail',
  Alarms = '/lms_alarm_status_dev',
  SupportRequest = '/support-request',
  Equipments = '/lms_equip_status_dev',
}

/** 현재 사용자의 소속 센터 목록 조회 */
export const fetchCenterInfo = () => getSearchList(Api.CenterInfo)

/** 소속 센터의 KPI 조회 (유효하지 않은 엔드포인트) */
export const fetchKpi = () => getCommonGetApi(Api.Kpi, null)
/** 알람 목록 조회 */
export const fetchAlarms = () => getCommonGetApi(Api.Alarms, null)
/** 설비 상태 목록 조회 */
export const fetchEquipments = () => getCommonGetApi(Api.Equipments, null)
/** 소속 센터의 기간별 처리량 조회 (유효하지 않은 엔드포인트) */
export const fetchThroughput = () => getCommonGetApi(Api.Throughput, null)
/** 소속 센터의 입출고 흐름 조회 (유효하지 않은 엔드포인트) */
export const fetchInventoryFlow = () => getCommonGetApi(Api.InventoryFlow, null) // return: InventoryFlow
/** 소속 센터의 입고/출고/재정리 현황 조회 (유효하지 않은 엔드포인트) */
export const fetchInboundFulfillment = () => getCommonGetApi(Api.InboundFulfillment, null)
export const fetchOutboundFulfillment = () => getCommonGetApi(Api.OutboundFulfillment, null)
export const fetchInventoryFulfillment = () => getCommonGetApi(Api.InventoryFulfillment, null)
/** 소속 센터의 작업이력 조회 (유효하지 않은 엔드포인트) */
export const fetchRecentOrders = () => getCommonGetApi(Api.RecentOrders, null)
/** 소속 센터의 목표 대비 실적 조회 (유효하지 않은 엔드포인트) */
export const fetchTargetVsActual = () => getCommonGetApi(Api.TargetVsActual, null)
/**
 * 유지보수 요청 전체 조회
 * @param lcIds 사용자 소속 센터 ID 목록
 */
export const fetchSupportRequests = (lcIds: string[]) => {
  const query = JSON.stringify([
    { name: 'lc_id', operator: 'in', value: lcIds.join(','), relation: false },
  ])
  return getSearchList(Api.SupportRequest, {
    query,
    sort: JSON.stringify([]),
    page: 1,
    limit: 9999,
  })
}
