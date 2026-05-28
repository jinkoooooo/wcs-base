<template>
  <PageWrapper :fixedHeight="true" :contentFullHeight="true" :contentClass="'m-0 bg-[#0a1929]'"
               :contentStyle="{ margin: 0}">
    <div class="dashboard flex gap-4 p-4 h-full box-border ">

      <div class="sidebar flex flex-col gap-4">

        <!-- 1. 센터 선택 -->
        <div v-if="userCenters.length > 1" ref="lcSelectorRef" class="center-selector shrink-0">
          <div class="selector-label">{{ t("label.select_lc") }}</div>
          <button class="selector-trigger" @click="lcSelectorOpen = !lcSelectorOpen">
            <span class="selector-value">{{ selectedLcNm }}</span>
            <DownOutlined class="selector-chevron" :class="{ open: lcSelectorOpen }" />
          </button>
          <div v-show="lcSelectorOpen" class="selector-dropdown">
            <button
              v-for="c in userCenters"
              :key="c.lcId"
              class="selector-option"
              :class="{ active: c.lcId === selectedLcId }"
              @click="selectCenter(c.lcId)"
            >
              <span class="option-nm">{{ c.lcNm }}</span>
              <span class="option-id">{{ c.lcId }}</span>
            </button>
          </div>
        </div>

        <!-- 2. 센터 정보: 센터명, 센터코드, 라인코드, 현재시각 -->
        <DashboardHeader
          :lcId="selectedLcId"
          :lcNm="selectedLcNm"
          :lineCnt="selectedLcLineCnt"
          :status="centerStatus"
        />

        <!-- 3. 탭 바: 운영현황, 작업관리, 설비/품질 -->
        <div class="tab-bar flex-1">
          <button
            v-for="tab in TABS"
            :key="tab.key"
            class="tab-btn"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <component :is="tab.icon" class="tab-icon" />
            {{ tab.label }}
            <span v-if="tab.badge" class="tab-badge">{{ tab.badge }}</span>
          </button>
        </div>

      </div>

      <div class="flex-1 min-w-0 flex flex-col gap-4">

        <!-- 4. KPI -->
        <DashboardOrderSummary class="shrink-0" :kpiData="kpiData" />

        <!-- 5-1. 운영 현황 탭 -->
        <div v-if="visitedTabs.has('operations')" v-show="activeTab === 'operations'"
             class="tab-grid operations-grid">

          <OperationsAlarmList
            class="g-alarms"
            :alarms="alarms"
          />

          <OperationsEquipStatusChart
            ref="operEquipChartRef"
            class="g-equipment"
            :equipmentStatus="equipmentStatus"
          />

          <OperationsThroughputChart
            ref="operThroughputChartRef"
            class="g-throughput"
            :throughput="throughput"
          />

          <OperationsInventoryFlowChart
            ref="operInventoryChartRef"
            class="g-inventory"
            :flow="inventoryFlow"
          />

        </div>

        <!-- 5-2. 작업 관리 탭 -->
        <div v-if="visitedTabs.has('work')" v-show="activeTab === 'work'"
             class="tab-grid work-grid">

          <WorkOrderStatusPanel
            class="g-inbound"
            :title="t('label.inbound') + ' ' + t('label.overview')"
            color="#38bdf8"
            :fulfillment="inboundFulfillment"
          />

          <WorkOrderStatusPanel
            class="g-outbound"
            :title="t('label.outbound') + ' ' + t('label.overview')"
            color="#34d399"
            :fulfillment="outboundFulfillment"
          />

          <WorkOrderStatusPanel
            class="g-inventory"
            :title="t('label.relocation') + ' ' + t('label.overview')"
            color="#a78bfa"
            :fulfillment="inventoryFulfillment"
          />

          <WorkRecentOrderPanel
            class="g-recentwo"
            :workOrders="recentOrders"
          />

          <WorkPerformanceChart
            ref="workPerformanceChartRef"
            class="g-target"
            :targetVsActual="targetVsActual"
          />

        </div>

        <!-- 5-3. 설비/품질 탭 -->
        <div v-if="visitedTabs.has('equipment')" v-show="activeTab === 'equipment'"
             class="tab-grid equipment-grid">

          <EquipStatusPanel
            class="g-equiplist"
            :list="equips"
          />

          <EquipMaintenancePanel
            ref="equipMaintRef"
            class="g-maintenance"
            :maintenance="maintenance"
          />

          <EquipErrorTrendChart
            ref="equipErrorChart"
            class="g-errorrate"
            :trend="errorRateTrend"
          />

        </div>
      </div>
    </div>
  </PageWrapper>
</template>

<script setup lang="ts">
  /**
   * NOTE
   * - 소속된 센터가 여러곳일 경우 센터 선택 옵션 표시
   * - 탭 우측에 알림Badge 생성 조건
   *   - Error 알람 존재 -> 운영 현황에 Badge 생성
   *   - Error 설비 존재 -> 설비/품질에 Badge 생성
   * - 3가지 탭 중 선택
   *   - operations, work, equipment
   *   - 초기 화면: operations
   *   - 탭 선택 시 차트 리사이즈
   */
  import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue"
  import { DashboardOutlined, DownOutlined, ProfileOutlined, ToolOutlined } from "@ant-design/icons-vue"

  import { useDashboard } from "@/views/lms/monitoring-dashboard/composables/useDashboard"
  import { useI18n } from "@/hooks/web/useI18n";

  import DashboardHeader from "@/views/lms/monitoring-dashboard/components/DashboardHeader.vue"
  import DashboardOrderSummary from "@/views/lms/monitoring-dashboard/components/DashboardOrderSummary.vue"

  import OperationsAlarmList from "@/views/lms/monitoring-dashboard/components/OperationsAlarmList.vue"
  import OperationsEquipStatusChart from "@/views/lms/monitoring-dashboard/components/OperationsEquipStatusChart.vue"
  import OperationsThroughputChart from "@/views/lms/monitoring-dashboard/components/OperationsThroughputChart.vue"
  import OperationsInventoryFlowChart from "@/views/lms/monitoring-dashboard/components/OperationsInventoryFlowChart.vue"

  import WorkOrderStatusPanel from "@/views/lms/monitoring-dashboard/components/WorkOrderStatusPanel.vue"
  import WorkRecentOrderPanel from "@/views/lms/monitoring-dashboard/components/WorkRecentOrderPanel.vue"
  import WorkPerformanceChart from "@/views/lms/monitoring-dashboard/components/WorkPerformanceChart.vue"

  import EquipStatusPanel from "@/views/lms/monitoring-dashboard/components/EquipStatusPanel.vue"
  import EquipMaintenancePanel from "@/views/lms/monitoring-dashboard/components/EquipMaintenancePanel.vue"
  import EquipErrorTrendChart from "@/views/lms/monitoring-dashboard/components/EquipErrorTrendChart.vue"

  import { PageWrapper } from "@/components/Page";

  /* == 데이터 ========== */

  const {
    userCenters, selectedLcId, selectedLcNm, selectedLcLineCnt,
    centerStatus,
    kpiData, alarms, equipmentStatus, throughput, inventoryFlow,
    maintenance, inboundFulfillment, outboundFulfillment, inventoryFulfillment, targetVsActual,
    errorRateTrend, equips, recentOrders,
    loadData,
  } = useDashboard()
  const { t } = useI18n()

  onMounted(() => {
    loadData()
    document.addEventListener('mousedown', handleSelectorClickOutside)
  })

  onUnmounted(() => document.removeEventListener('mousedown', handleSelectorClickOutside))

  /* == 센터 선택 드롭다운 ========== */
  const lcSelectorRef = ref<HTMLElement | null>(null)
  const lcSelectorOpen = ref(false)

  function selectCenter(lcId: string) {
    selectedLcId.value = lcId
    lcSelectorOpen.value = false
  }

  function handleSelectorClickOutside(e: MouseEvent) {
    if (lcSelectorRef.value && !lcSelectorRef.value.contains(e.target as Node)) {
      lcSelectorOpen.value = false
    }
  }

  const operThroughputChartRef = ref();
  const operEquipChartRef = ref();
  const operInventoryChartRef = ref();
  const workPerformanceChartRef = ref();
  const equipMaintRef = ref();
  const equipErrorChart = ref();

  /* == 탭 ========== */
  const activeTab = ref("operations")
  // 처음 방문한 탭만 DOM에 마운트 (lazy mount) -> 비활성 탭에서 ECharts clientHeight=0 경고 발생 방지
  const visitedTabs = ref(new Set<string>(["operations"]))

  const errorAlarmCnt = computed(() =>
    alarms.value.filter(a => a.level === "error").length
  )

  const errorEquipCnt = computed(() =>
    equips.value.filter(e => e.status === "error" || e.status === "stop").length
  )

  const TABS = computed(() => [
    {
      key: "operations",
      label: t("label.operation_status"),
      icon: DashboardOutlined,
      badge: errorAlarmCnt.value || null
    },
    { key: "work", label: t("label.work_management"), icon: ProfileOutlined, badge: null },
    {
      key: "equipment",
      label: t("label.equip_and_quality"),
      icon: ToolOutlined,
      badge: errorEquipCnt.value || null
    },
  ])

  // 탭 전환 시 차트 재렌더링 및 첫 방문한 탭 마운트 등록
  watch(activeTab, async (tab) => {
    visitedTabs.value.add(tab)
    await nextTick()

    operEquipChartRef.value?.redrawChart();
    operThroughputChartRef.value?.redrawChart();
    operInventoryChartRef.value?.redrawChart();
    workPerformanceChartRef.value?.redrawChart();
    equipMaintRef.value?.redrawChart();
    equipErrorChart.value?.redrawChart();
  })
</script>

<style>
  /* 반응형 폰트 변수 - 대시보드 하위 컴포넌트 전체에 cascade */
  .dashboard {
    --fs-2xs: clamp(0.55rem, 0.65vw, 0.65rem);
    --fs-xs: clamp(0.65rem, 0.78vw, 0.78rem);
    --fs-sm: clamp(0.72rem, 0.88vw, 0.88rem);
    --fs-base: clamp(0.88rem, 1.05vw, 1.05rem);
    --fs-lg: clamp(1.1rem, 1.5vw, 1.6rem);
    --fs-xl: clamp(1.5rem, 2.0vw, 2.2rem);
  }
</style>

<style scoped>

  /* ==== 사이드바 ==== */
  .sidebar {
    width: 200px;
    flex-shrink: 0;
  }

  /* ==== 센터 선택 ==== */
  .center-selector {
    position: relative;
    background: linear-gradient(135deg, #0d2137, #132f4c);
    border: 1px solid #1e3a5f;
    border-radius: 12px;
    padding: 10px 14px;
  }

  .selector-label {
    font-size: var(--fs-xs);
    color: #8899b4;
    letter-spacing: 0.08em;
    margin-bottom: 6px;
  }

  .selector-trigger {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid #1e3a5f;
    border-radius: 8px;
    color: #e2e8f0;
    font-size: var(--fs-sm);
    padding: 7px 12px;
    cursor: pointer;
    transition: border-color 0.15s, background 0.15s;
    text-align: left;
  }

  .selector-trigger:hover {
    border-color: #2d5a8e;
    background: rgba(255, 255, 255, 0.08);
  }

  .selector-value {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .selector-chevron {
    font-size: var(--fs-xs);
    color: #8899b4;
    flex-shrink: 0;
    transition: transform 0.2s;
  }

  .selector-chevron.open {
    transform: rotate(180deg);
  }

  .selector-dropdown {
    position: absolute;
    left: 0;
    right: 0;
    top: calc(100% + 6px);
    background: #0d2137;
    border: 1px solid #1e3a5f;
    border-radius: 10px;
    overflow: hidden;
    z-index: 200;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
  }

  .selector-option {
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
    padding: 9px 14px;
    background: transparent;
    border: none;
    cursor: pointer;
    transition: background 0.12s;
  }

  .selector-option + .selector-option {
    border-top: 1px solid rgba(30, 58, 95, 0.5);
  }

  .selector-option:hover {
    background: rgba(255, 255, 255, 0.06);
  }

  .selector-option.active {
    background: #1e3a5f;
  }

  .option-nm {
    font-size: var(--fs-sm);
    font-weight: 600;
    color: #e2e8f0;
  }

  .option-id {
    font-size: var(--fs-xs);
    color: #8899b4;
  }

  .selector-option.active .option-nm {
    color: #38bdf8;
  }

  /* ==== 탭 바 ==== */
  .tab-bar {
    display: flex;
    flex-direction: column;
    gap: 4px;
    background: linear-gradient(135deg, #0d2137, #132f4c);
    border: 1px solid #1e3a5f;
    border-radius: 12px;
    padding: 6px;
  }

  .tab-btn {
    position: relative;
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
    font-size: var(--fs-sm);
    font-weight: 500;
    color: #94a3b8;
    background: transparent;
    border: none;
    border-radius: 8px;
    padding: 0.75rem 1rem;
    cursor: pointer;
    transition: background 0.15s, color 0.15s;
    text-align: left;
  }

  .tab-btn:hover:not(.active) {
    color: #cbd5e1;
    background: rgba(255, 255, 255, 0.05);
  }

  .tab-btn.active {
    background: #1e3a5f;
    color: #e2e8f0;
    font-weight: 600;
  }

  .tab-icon {
    font-size: var(--fs-base);
    flex-shrink: 0;
  }

  .tab-badge {
    margin-left: auto;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    background: #ef4444;
    color: #fff;
    font-size: var(--fs-2xs);
    font-weight: 700;
    border-radius: 999px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  /* ==== 탭 그리드 공통 ==== */
  .tab-grid {
    display: grid;
    gap: 1rem;
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .tab-grid > * {
    min-height: 0;
  }

  /* ==== Tab 1: 운영 현황 ==== */
  .operations-grid {
    grid-template-columns: 2fr 1fr;
    grid-template-rows: 9fr 11fr;
    grid-template-areas:
      "alarms     equipment"
      "throughput inventory";
  }

  .g-alarms {
    grid-area: alarms;
  }

  .g-equipment {
    grid-area: equipment;
  }

  .g-throughput {
    grid-area: throughput;
  }

  .g-inventory {
    grid-area: inventory;
  }

  /* ==== Tab 2: 작업 관리 ==== */
  .work-grid {
    grid-template-columns: 1fr 1fr 1fr;
    grid-template-rows: 9fr 10fr;
    grid-template-areas:
      "inbound   outbound  inventory"
      "recentwo  recentwo  target";
  }

  .g-inbound {
    grid-area: inbound;
  }

  .g-outbound {
    grid-area: outbound;
  }

  .g-inventory {
    grid-area: inventory;
  }

  .g-recentwo {
    grid-area: recentwo;
  }

  .g-target {
    grid-area: target;
  }

  /* ==== Tab 3: 설비/품질 ==== */
  .equipment-grid {
    grid-template-columns: 2fr 1fr;
    grid-template-rows: 11fr 9fr;
    grid-template-areas:
      "equiplist  maintenance"
      "errorrate  errorrate";
  }

  .g-equiplist {
    grid-area: equiplist;
  }

  .g-maintenance {
    grid-area: maintenance;
  }

  .g-errorrate {
    grid-area: errorrate;
  }

</style>
