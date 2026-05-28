import { computed, ref, watch } from 'vue'
import {
  Alarm,
  CenterStatusType,
  Equipment,
  EquipmentStatus,
  ErrorRateTrend,
  FulfillmentData,
  InventoryFlow,
  KpiData,
  KpiMetric,
  Maintenance,
  RecentOrder,
  TargetVsActual,
  Throughput,
} from "@/views/lms/monitoring-dashboard/types"
import {
  fetchAlarms,
  fetchCenterInfo,
  fetchEquipments,
  fetchSupportRequests,
} from '@/api/lms/dashboard'
import { DASHBOARD_DUMMY } from "@/views/lms/monitoring-dashboard/dashboardDummy";
import { useI18n } from "@/hooks/web/useI18n";

const { t } = useI18n()

/**
 * 유지보수 완료로 간주할 SUPPORT_STATUS 공통코드 값 / TODO: 공통코드 로드 검토
 */
const COMPLETED_STATUS = new Set(['5'])
const DRAFT_STATUS = new Set(['1'])

/**
 * todo: lms_equip_status_dev 테이블과 centers 테이블의 lc_id 불일치 수정
 * 센터코드 매핑
 * - monitoring-status와 동일한 맵 사용
 */
const LC_ID_MATCHING_MAP: Record<string, string> = {
  'JNM001': 'KR_KPP_YEOSU',
  'GYG001': 'KR_Daewha',
}

/* 더미 데이터 사용 여부 (개발용) */
const USE_TEST_DATA = false

export function useDashboard() {

  // 사용자 소속 센터 목록
  const userCenters = ref<Array<{ lcId: string; lcNm: string; lineNm?: string }>>([])
  // 선택된 센터 코드
  const selectedLcId = ref<string>('')
  // 선택된 센터명
  const selectedLcNm = computed(() => {
    if (USE_TEST_DATA) {
      return DASHBOARD_DUMMY.center.lcNm
    }

    return (userCenters.value.find(center => center.lcId === selectedLcId.value)?.lcNm ?? '')
  })
  // 선택된 센터의 라인 수
  const selectedLcLineCnt = computed(() => {
    return userCenters.value.filter(
      center => center.lcId === selectedLcId.value
    ).length
  })

  const lineNm = ref(DASHBOARD_DUMMY.center.lineNm) // TODO: 사용 검토
  const centerStatus = ref<CenterStatusType>(DASHBOARD_DUMMY.center.centerStatus)

  const kpiData = ref<KpiData>(DASHBOARD_DUMMY.kpi)
  const alarms = ref<Alarm[]>(DASHBOARD_DUMMY.alarms)
  const equipmentStatus = ref<EquipmentStatus>(DASHBOARD_DUMMY.equipmentStatus)
  const throughput = ref<Throughput>(DASHBOARD_DUMMY.throughput)
  const inventoryFlow = ref<InventoryFlow>(DASHBOARD_DUMMY.inventoryFlow)
  const maintenance = ref<Maintenance>(DASHBOARD_DUMMY.maintenance)
  const inboundFulfillment = ref<FulfillmentData>(DASHBOARD_DUMMY.inboundFulfillment)
  const outboundFulfillment = ref<FulfillmentData>(DASHBOARD_DUMMY.outboundFulfillment)
  const inventoryFulfillment = ref<FulfillmentData>(DASHBOARD_DUMMY.inventoryFulfillment)
  const targetVsActual = ref<TargetVsActual>(DASHBOARD_DUMMY.targetVsActual)
  const errorRateTrend = ref<ErrorRateTrend>(DASHBOARD_DUMMY.errorRateTrend)
  const equips = ref<Equipment[]>(DASHBOARD_DUMMY.equips)
  const recentOrders = ref<RecentOrder[]>(DASHBOARD_DUMMY.recentOrders)

  /** 사용자 소속 센터 목록 조회 및 초기 선택 */
  async function loadUserCenters() {
    const response = await fetchCenterInfo()
    const list: any[] = Array.isArray(response?.items)
      ? response.items
      : Array.isArray(response) ? response : []

    userCenters.value = list
      .map((center: any) => ({
        lcId: String(center?.lc_id ?? '').trim(),
        lcNm: center?.lc_nm ?? '',
        lineNm: center?.line_nm ?? '',
      }))
      .filter(center => center.lcId)

    if (!selectedLcId.value && userCenters.value.length) {
      selectedLcId.value = userCenters.value[0].lcId
    }
  }

  /** 사용자 소속 센터의 KPI 조회 */
  async function loadKpi() {
    if (!selectedLcId.value) return

    try {
      // todo: endpoint 확정 후 주석해제
      // const response = await fetchKpi()
      // const items: any[] = extractItems(response)

      kpiData.value = {
        orderNumber: '-',
        throughput: { value: 0, unit: `${ t("label.box").toLowerCase() }/h`, change: 0, up: false },
        capacityUtilitzation: { value: 0, unit: '%', change: 0, up: false },
        fullFillmentRate: {
          value: 0,
          unit: `${ t("label.orders").toLowerCase() }/h`,
          change: 0,
          up: false
        },
        avgProcessTime: { value: 0, unit: t("label.min").toLowerCase(), change: 0, up: false },
      }
    } catch (e) {
    }
  }

  /**
   * 선택된 센터의 알람 목록 조회 및 에러율 추이 계산
   * - lc_id 불일치 매핑(LC_ID_MATCHING_MAP) 적용
   * - 조회 실패 시 기존 데이터 유지
   * - 발생일시 기반 오름차순 정렬
   * - 당일 발생 건에 한해 에러율 추이 계산
   */
  async function loadAlarms() {
    if (!selectedLcId.value) return

    try {
      const response = await fetchAlarms();
      const items: any[] = extractItems(response);
      console.log('loadAlamrs items = ', items);

      const lcId = selectedLcId.value
      const matchedId = LC_ID_MATCHING_MAP[lcId]

      const filtered = items.filter(item =>
        item.lc_id === lcId || (matchedId && item.lc_id === matchedId)
      )

      alarms.value = filtered.map(item => ({
        id: item.id ?? '',
        equipId: item.equip_id ?? '',
        alarmMsg: item.alarm_msg ?? '',
        level: mapAlarmLevelFromRaw(item),
        occurredAt: item.occurred_at ?? '',
      }))
        .sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime())

      buildErrorTrend()

    } catch (e) {
    }
  }

  /**
   * 선택된 센터의 설비 상태 목록 조회
   * - lc_id 불일치 매핑(LC_ID_MATCHING_MAP) 적용
   * - 조회 실패 시 기존 데이터 유지
   * - equip_id기반 오름차순 정렬
   */
  async function loadEquips() {
    if (!selectedLcId.value) return

    try {
      const response = await fetchEquipments()
      const items: any[] = extractItems(response)

      const lcId = selectedLcId.value
      const matchedId = LC_ID_MATCHING_MAP[lcId]

      const filtered = items.filter(item =>
        item.lc_id === lcId || (matchedId && item.lc_id === matchedId)
      )

      equips.value = filtered.map(item => ({
        id: String(item.equip_id ?? ''),
        name: String(item.equip_id ?? ''),
        status: mapEquipStatusFromRaw(item),
        uptime: item.updated_at ?? '-',
        lastError: item.err_msg ?? '-', // todo: lastError대신 err_msg로 변경
      }))
        .sort((a, b) => a.id.localeCompare(b.id))

      equipmentStatus.value = equips.value.reduce<EquipmentStatus>((acc, equip) => {
        acc[equip.status]++
        return acc
      }, { run: 0, idle: 0, stop: 0, error: 0, })
    } catch (e) {
    }
  }

  async function loadThroughput() {
    if (!selectedLcId.value) return

    try {
      // todo: endpoint 확정 후 주석해제
      // const response = await fetchThroughput()
      // const items: any[] = extractItems(response)

      throughput.value = {
        hours: ['06', '07', '08', '09', '10', '11', '12', '13', '14', '15', '16', '17'],
        hourly: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        daily: 0,
        weekly: 0,
        monthly: 0,
      }
    } catch (e) {
    }
  }

  async function loadInventoryFlow() {
    if (!selectedLcId.value) return

    try {
      // todo: endpoint 확정 후 주석해제
      // const response = await fetchInventoryFlow()
      // const items: any[] = extractItems(response)

      inventoryFlow.value = {
        daily: {
          labels: ['06', '07', '08', '09', '10', '11', '12', '13', '14', '15', '16', '17'],
          inbound: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
          outbound: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
        },
        weekly: {
          labels: ['월', '화', '수', '목', '금', '토', '일'],
          inbound: [0, 0, 0, 0, 0, 0, 0],
          outbound: [0, 0, 0, 0, 0, 0, 0],
        },
        monthly: {
          labels: ['1월', '2월', '3월', '4월', '5월', '6월'],
          inbound: [0, 0, 0, 0, 0, 0],
          outbound: [0, 0, 0, 0, 0, 0],
        },
      }
    } catch (e) {
    }
  }

  async function loadFulfillment() {
    if (!selectedLcId.value) return
    try {
      // todo: endpoint 확정 후 주석해제
      // let response = await fetchInboundFulfillment()
      // let items: any[] = extractItems(response)
      inboundFulfillment.value = { total: 0, completed: 0, inProgress: 0, pending: 0 }
      // let response = await fetchOutboundFulfillment()
      // let items: any[] = extractItems(response)
      outboundFulfillment.value = { total: 0, completed: 0, inProgress: 0, pending: 0 }
      // let response = await fetchInventoryFulfillment()
      // let items: any[] = extractItems(response)
      inventoryFulfillment.value = { total: 0, completed: 0, inProgress: 0, pending: 0 }
    } catch (e) {
    }
  }

  async function loadRecentOrders() {
    if (!selectedLcId.value) return

    try {
      // todo: endpoint 확정 후 주석해제
      // let response = await fetchRecentOrders()
      // let items: any[] = extractItems(response)
      recentOrders.value = []
    } catch (e) {
    }
  }

  async function loadTargetVsActual() {
    if (!selectedLcId.value) return

    try {
      // todo: endpoint 확정 후 주석해제
      // let response = await fetchTargetVsActual()
      // let items: any[] = extractItems(response)
      targetVsActual.value = { target: 0, actual: 0 }
    } catch (e) {
    }
  }

  /**
   * 선택된 센터의 유지보수 현황
   * - 임시저장 항목 제외
   * - 조회 실패 시 기존 데이터 유지
   */
  async function loadMaintenanceLive() {
    if (!selectedLcId.value) return

    try {
      const response = await fetchSupportRequests([selectedLcId.value])
      const items: any[] = Array.isArray(response?.items)
        ? response.items
        : Array.isArray(response) ? response : []

      let inProgress = 0, completed = 0, deleted = 0
      for (const item of items) {
        const currentStatus = String(item.status ?? '')

        if (item.deleted) {
          deleted++
        } else if (COMPLETED_STATUS.has(currentStatus) && !DRAFT_STATUS.has(currentStatus)) {
          completed++
        } else if (DRAFT_STATUS.has(currentStatus)) {
          inProgress++
        }
      }

      maintenance.value = { inProgress, completed, deleted }
    } catch (e) {
    }
  }

  function loadDummyData() {
    lineNm.value = DASHBOARD_DUMMY.center.lineNm
    centerStatus.value = DASHBOARD_DUMMY.center.centerStatus
    kpiData.value = DASHBOARD_DUMMY.kpi
    alarms.value = DASHBOARD_DUMMY.alarms
    equipmentStatus.value = DASHBOARD_DUMMY.equipmentStatus
    throughput.value = DASHBOARD_DUMMY.throughput
    inventoryFlow.value = DASHBOARD_DUMMY.inventoryFlow
    maintenance.value = DASHBOARD_DUMMY.maintenance
    inboundFulfillment.value = DASHBOARD_DUMMY.inboundFulfillment
    outboundFulfillment.value = DASHBOARD_DUMMY.outboundFulfillment
    inventoryFulfillment.value = DASHBOARD_DUMMY.inventoryFulfillment
    targetVsActual.value = DASHBOARD_DUMMY.targetVsActual
    errorRateTrend.value = DASHBOARD_DUMMY.errorRateTrend
    equips.value = DASHBOARD_DUMMY.equips
    recentOrders.value = DASHBOARD_DUMMY.recentOrders
  }

  // 설비 상태에 따라 상태라벨 부여
  function mapEquipStatusFromRaw(item: any): Equipment['status'] {
    // TODO: 추가되는 상태에 따라 옵션 관리 필요 / 2026-03-11 기준 'WAITING' 상태인 데이터만 존재
    if ((item.err_cnt ?? 0) > 0) return 'error'
    const upperStatus = String(item.current_status ?? '').toUpperCase()
    if (upperStatus === 'RUN' || upperStatus === 'WAITING') return 'run'
    if (upperStatus === 'STOP') return 'stop'
    if (upperStatus === 'ERROR') return 'error'
    return 'idle'
  }

  // 알람 상태에 따라 상태라벨 부여
  function mapAlarmLevelFromRaw(item: any): Alarm['level'] {
    // TODO: 추가되는 상태에 따라 옵션 관리 필요 / 2026-03-11 기준 'ERROR' 상태인 데이터만 존재
    if ((item.err_cnt ?? 0) > 0) return 'error'
    if (item.cleared_at) return 'clearedError'
    const upperStatus = String(item.alarm_type ?? '').toUpperCase()
    if (upperStatus === 'RUN' || upperStatus === 'WAITING') return 'info'
    if (upperStatus === 'WARNING') return 'warn'
    if (upperStatus === 'ERROR') return 'error'
    return 'info'
  }

  // 에러율 추이 계산
  async function buildErrorTrend() {
    const hours = ['08', '09', '10', '11', '12', '13', '14', '15', '16', '17', '18']
    const hourMap = Object.fromEntries(hours.map(h => [h, 0]))
    const today = new Date()
    console.log('today = ', today)

    alarms.value.filter(item => item.level === 'error')
      .forEach(item => {
        if (!item.occurredAt) return

        const date = new Date(item.occurredAt)
        if (date.getFullYear() !== today.getFullYear() || date.getMonth() !== today.getMonth() || date.getDate() !== today.getDate()) {
          console.log('date.getFullYear = ', date.getFullYear, '\ndate.getMonth = ', date.getMonth, '\n date.getDate() = ', date.getDate())
          return
        }

        const hour = String(date.getHours()).padStart(2, '0')

        if (hourMap[hour] !== undefined) {
          hourMap[hour] += 1
        }
        console.log('hour = ', hour, '\nhourMap[hour] =', hourMap[hour])
      })

    errorRateTrend.value = {
      hours,
      rates: hours.map(h => hourMap[h]),
      threshold: 0,
    }
  }

  // 선택 센터 변경 시 데이터 재조회 / todo: 나머지 데이터 조회 로직 추가
  watch(selectedLcId, async (newId) => {
    if (newId && !USE_TEST_DATA && selectedLcNm.value !== '테스트센터') {
      await loadKpi()
      await loadAlarms()
      await loadEquips()
      await loadThroughput()
      await loadInventoryFlow()
      await loadFulfillment()
      await loadRecentOrders()
      await loadTargetVsActual()
      await loadMaintenanceLive()
    } else {
      await loadDummyData()
    }
  })

  /**
   * 대시보드 데이터 조회
   * 1. 사용자 소속 센터 목록 조회 후, 센터코드 선택
   * 2. 선택한 센터코드 기반 유지보수 현황 조회
   */
  async function loadData() {
    if (USE_TEST_DATA || selectedLcNm.value === '테스트센터') {
      await loadDummyData()
      return
    }

    await Promise.allSettled([
      loadUserCenters(),
      loadKpi(), loadAlarms(), loadEquips(), loadThroughput(), loadInventoryFlow(),
      loadMaintenanceLive()
    ])
  }

  function mapKpiMetric(data: any): KpiMetric {
    return {
      value: data.value,
      unit: data.unit,
      change: data.change,
      up: data.up
    }
  }

  function mapKpiData(data: any): KpiData {
    return {
      orderNumber: data.order_number,
      throughput: mapKpiMetric(data.throughput),
      capacityUtilitzation: mapKpiMetric(data.operation_rate),
      fullFillmentRate: mapKpiMetric(data.order_rate),
      avgProcessTime: mapKpiMetric(data.avg_process_time)
    }
  }

  function mapFulfillmentData(data: any): FulfillmentData {
    return {
      total: data.total,
      completed: data.completed,
      inProgress: data.in_progress,
      pending: data.pending
    }
  }

  function mapRecentOrder(data: any): RecentOrder {
    return {
      orderNumber: data.order_number,
      quantity: data.quantity,
      completedAt: data.completed_at,
      duration: data.duration
    }
  }

  /** Util - 응답 추출 */
  function extractItems(response: any): any[] {
    return Array.isArray(response?.items)
      ? response.items
      : Array.isArray(response) ? response : []
  }

  return {
    userCenters, selectedLcId, selectedLcNm, selectedLcLineCnt,
    lineNm, centerStatus, kpiData,
    alarms, equipmentStatus, throughput, inventoryFlow,
    inboundFulfillment, outboundFulfillment, inventoryFulfillment, recentOrders, targetVsActual,
    equips, maintenance, errorRateTrend,
    loadData,
  }
}
