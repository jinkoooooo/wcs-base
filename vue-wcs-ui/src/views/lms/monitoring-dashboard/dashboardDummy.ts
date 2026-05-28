import {
  CenterInfo, KpiData, Alarm, EquipmentStatus, Throughput, InventoryFlow,
  FulfillmentData, RecentOrder, TargetVsActual,
  Equipment, Maintenance, ErrorRateTrend,
} from "@/views/lms/monitoring-dashboard/types"
import { useI18n } from "@/hooks/web/useI18n";

const { t } = useI18n()

/* 더미 데이터 */
export const DASHBOARD_DUMMY: {
  center: CenterInfo                      // 센터정보
  kpi: KpiData                            // KPI 정보
  // 운영 현황 탭
  alarms: Alarm[]                         // 실시간 알람
  equipmentStatus: EquipmentStatus        // 설비 상태
  throughput: Throughput                  // 시간별 처리량
  inventoryFlow: InventoryFlow            // 임출고 흐름
  // 작업 관리 탭
  inboundFulfillment: FulfillmentData     // 입고 현황
  outboundFulfillment: FulfillmentData    // 출고 현황
  inventoryFulfillment: FulfillmentData   // 재고정리 현황
  recentOrders: RecentOrder[]             // 최근 완료 작업
  targetVsActual: TargetVsActual          // 목표 대비 실적
  // 설비/품질 탭
  equips: Equipment[]                 // 설비별 상태 목록
  maintenance: Maintenance                // 유지보수 현황
  errorRateTrend: ErrorRateTrend          // 에러율 추이
} = {
  center: {
    lcId: 'TCT001',
    lcNm: '테스트 물류센터',
    lineNm: 'AS/RS Line',
    centerStatus: 'running',
  },

  kpi: {
    orderNumber: '20260306B2C0000003722-0000336932-20260306-10001',
    throughput: { value: 145, unit: `${ t("label.box").toLowerCase() }/h`, change: 3.1, up: false },
    capacityUtilitzation: { value: 89, unit: '%', change: 2.4, up: true },
    fullFillmentRate: { value: 38, unit: `${ t("label.orders").toLowerCase() }/h`, change: 5.2, up: true },
    avgProcessTime: { value: 12, unit: t("label.min").toLowerCase(), change: 8.1, up: false },
  },

  alarms: [
    { equipId: 'Conveyor-01', message: 'Motor Overheat', level: 'error', time: '10:25:30' },
    { equipId: 'Robot-02', message: 'Position Error', level: 'warn', time: '10:27:10' },
    { equipId: 'Scanner-03', message: 'Read Timeout', level: 'warn', time: '10:31:55' },
    { equipId: 'AGV-01', message: 'Battery Low', level: 'info', time: '10:38:22' },
    { equipId: 'Sorter-02', message: 'Belt Slip', level: 'error', time: '10:41:08' },
  ],

  equipmentStatus: { run: 35, stop: 3, error: 1, idle: 4 },

  throughput: {
    hours: ['06', '07', '08', '09', '10', '11', '12', '13', '14', '15', '16', '17'],
    hourly: [78, 95, 112, 134, 148, 162, 155, 143, 152, 168, 145, 130],
    daily: 1_420,
    weekly: 9_350,
    monthly: 38_200,
  },

  inventoryFlow: {
    daily: {
      labels: ['06', '07', '08', '09', '10', '11', '12', '13', '14', '15', '16', '17'],
      inbound: [80, 120, 250, 310, 280, 190, 140, 220, 290, 260, 180, 150],
      outbound: [60, 90, 200, 280, 300, 210, 160, 190, 250, 240, 200, 130],
    },
    weekly: {
      labels: ['월', '화', '수', '목', '금', '토', '일'],
      inbound: [1_200, 1_450, 1_380, 1_520, 1_690, 980, 430],
      outbound: [1_100, 1_320, 1_250, 1_480, 1_600, 850, 380],
    },
    monthly: {
      labels: ['1월', '2월', '3월', '4월', '5월', '6월'],
      inbound: [34_000, 31_200, 38_500, 36_100, 40_200, 38_200],
      outbound: [32_000, 29_800, 36_200, 34_500, 38_700, 36_900],
    },
  },

  inboundFulfillment: { total: 89, completed: 67, inProgress: 15, pending: 7 },
  outboundFulfillment: { total: 156, completed: 98, inProgress: 34, pending: 24 },
  inventoryFulfillment: { total: 42, completed: 31, inProgress: 8, pending: 3 },

  recentOrders: [
    { orderNumber: 'WO-202606-089', quantity: 45, completedAt: '10:38', duration: `18${ t("label.min") }` },
    { orderNumber: 'WO-202606-088', quantity: 62, completedAt: '10:21', duration: `24${ t("label.min") }` },
    { orderNumber: 'WO-202606-087', quantity: 38, completedAt: '10:05', duration: `15${ t("label.min") }` },
    { orderNumber: 'WO-202606-086', quantity: 51, completedAt: '09:48', duration: `21${ t("label.min") }` },
    { orderNumber: 'WO-202606-085', quantity: 29, completedAt: '09:30', duration: `12${ t("label.min") }` },
  ],

  targetVsActual: { target: 1_600, actual: 1_420 },

  equips: [
    {
      id: 'CNV-01',
      name: 'Conveyor-01',
      status: 'error',
      uptime: '2h 15m',
      lastError: 'Motor Overheat'
    },
    { id: 'RBT-01', name: 'Robot-01', status: 'stop', uptime: '-', lastError: 'Position Error' },
    { id: 'AGV-01', name: 'AGV-01', status: 'idle', uptime: '4h 30m', lastError: '-' },
    { id: 'CNV-02', name: 'Conveyor-02', status: 'run', uptime: '6h 10m', lastError: '-' },
    { id: 'SCN-01', name: 'Scanner-01', status: 'run', uptime: '6h 10m', lastError: '-' },
    { id: 'CNV-03', name: 'Conveyor-03', status: 'run', uptime: '5h 45m', lastError: '-' },
  ],

  maintenance: { inProgress: 5, completed: 21, deleted: 1 },

  errorRateTrend: {
    hours: ['06', '07', '08', '09', '10', '11', '12', '13', '14', '15', '16', '17'],
    rates: [0.5, 0.3, 1.2, 0.8, 0.4, 2.1, 0.6, 0.9, 1.5, 0.7, 0.3, 0.8],
    threshold: 1.5,
  },
}
