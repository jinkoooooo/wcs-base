<!--
  index.vue
  TSPG 4-way WCS 통합 대시보드 — 한눈에 운영 상태 파악

  v2 변경:
  - 헤더에 eqGroup 드롭다운 추가
  - 선택된 eqGroupId 가 모든 API + 시뮬레이터 모달과 공유됨
  - localStorage 'TSPG_WCS_SELECTED_EQ_GROUP_ID' 로 새로고침 시 유지

  레이아웃:
  ┌─ 헤더 (제목 + [그룹 선택] + 시뮬 가동 뱃지 + [시뮬레이터])
  ├─ OperationStatusBar
  ├─ KpiCards
  ├─ OrderPipeline
  ├─ grid-2: DashboardThroughput | OrderTypeDonut
  ├─ grid-2: PortModeStatus      | HostOrderRecent
  └─ PendingOrdersList
-->

<template>
  <div class="wcs-dashboard">
    <!-- 헤더 -->
    <header class="wcs-header">
      <div class="title-block">
        <h2 class="title">📊 TSPG WCS 통합 대시보드</h2>
        <p class="subtitle">실시간 운영 현황 — 입고 · 출고 · 재고이동 · 재입고</p>
      </div>

      <div class="header-actions">
        <!-- eqGroup 선택 -->
        <div class="group-select">
          <label class="g-lbl">설비 그룹</label>
          <select
            v-model="selectedEqGroupId"
            class="g-sel"
            :disabled="eqGroups.length === 0"
            @change="onEqGroupChange"
          >
            <option v-if="eqGroups.length === 0" value="" disabled>그룹 없음</option>
            <option v-else value="" disabled>그룹 선택</option>
            <option v-for="g in eqGroups" :key="g.id" :value="g.id">
              {{ g.name || g.id }}
            </option>
          </select>
        </div>

        <span v-if="simRunning" class="sim-badge">
          <span class="dot" /> {{ simRunningGroup || selectedEqGroupId }} 시뮬 가동중
        </span>
        <button
          class="btn-sim"
          :disabled="!selectedEqGroupId"
          @click="simModalOpen = true"
        >
          시뮬레이터
        </button>
      </div>
    </header>

    <!-- 그룹 미선택 가이드 -->
    <div v-if="!selectedEqGroupId" class="no-group">
      먼저 상단에서 설비 그룹을 선택해주세요.
    </div>

    <template v-else>
      <!-- 운영 모드 + 플래그 -->
      <OperationStatusBar :op="summary?.operation ?? null" />

      <!-- KPI 6 카드 -->
      <KpiCards :summary="summary" :ports="ports" />

      <!-- 호스트 주문 파이프라인 -->
      <OrderPipeline :pipeline="summary?.hostPipeline ?? null" />

      <!-- 처리량 + 타입 분포 -->
      <div class="grid-2">
        <DashboardThroughput :data="throughput" :minutes="30" />
        <OrderTypeDonut :data="summary?.todayByType ?? null" />
      </div>

      <!-- 포트 + 최근 호스트 주문 -->
      <div class="grid-2">
        <PortModeStatus :ports="ports" />
        <HostOrderRecent :orders="hostRecent" />
      </div>

      <!-- 진행 중 셔틀 주문 -->
      <PendingOrdersList :orders="inProgress" />
    </template>

    <!-- 시뮬레이터 팝업 (그룹 선택 안되어 있으면 비활성) -->
    <SimulatorModal
      v-model:open="simModalOpen"
      :eq-group-id="selectedEqGroupId"
      @changed="refreshSimStatus"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, onDeactivated, onActivated, ref, watch } from 'vue';

import OperationStatusBar  from './components/OperationStatusBar.vue';
import KpiCards            from './components/KpiCards.vue';
import OrderPipeline       from './components/OrderPipeline.vue';
import DashboardThroughput from './components/DashboardThroughput.vue';
import OrderTypeDonut      from './components/OrderTypeDonut.vue';
import PortModeStatus      from './components/PortModeStatus.vue';
import HostOrderRecent     from './components/HostOrderRecent.vue';
import PendingOrdersList   from './components/PendingOrdersList.vue';
import SimulatorModal      from './components/SimulatorModal.vue';
import {
  getEqGroups,
  getDashboardSummary,
  getDashboardThroughput,
  getStatus,
  getInProgress,
  getHostRecent,
  getPorts,
  type EqGroup,
  type DashboardSummary,
  type ThroughputPoint,
  type SimulatorStatus,
  type InProgressOrder,
  type HostOrderRow,
  type PortRow,
} from './api';

// 상수
const STORAGE_KEY = 'TSPG_WCS_SELECTED_EQ_GROUP_ID';
const REFRESH_MS = 3000;

// ── 그룹 선택 상태 ──
const eqGroups = ref<EqGroup[]>([]);
const selectedEqGroupId = ref<string>('');

// ── 데이터 ──
const summary    = ref<DashboardSummary | null>(null);
const throughput = ref<ThroughputPoint[]>([]);
const ports      = ref<PortRow[]>([]);
const hostRecent = ref<HostOrderRow[]>([]);
const inProgress = ref<InProgressOrder[]>([]);
const simStatus  = ref<SimulatorStatus | null>(null);

// ── 모달 ──
const simModalOpen = ref(false);

// ── 폴링 제어 ──
const isPollingActive = ref(false);
let timer: ReturnType<typeof setTimeout> | null = null;

const simRunning = computed(
  () => !!(simStatus.value?.host?.running || simStatus.value?.plc?.running),
);
/** 시뮬이 어느 그룹을 돌리고 있는지 (있으면 뱃지에 표시) */
const simRunningGroup = computed(() => simStatus.value?.config?.currentEqGroupId ?? '');

// ── 그룹 목록 로드 + 선택 복원 ──
async function loadEqGroups() {
  try {
    const list = await getEqGroups();
    eqGroups.value = list ?? [];

    const saved = localStorage.getItem(STORAGE_KEY) || '';
    if (saved && eqGroups.value.some((g) => g.id === saved)) {
      selectedEqGroupId.value = saved;
    } else if (eqGroups.value.length > 0) {
      selectedEqGroupId.value = eqGroups.value[0].id;
      localStorage.setItem(STORAGE_KEY, selectedEqGroupId.value);
    } else {
      selectedEqGroupId.value = '';
    }
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error('[WCS-DASH] eqGroups 로드 실패', e);
    eqGroups.value = [];
    selectedEqGroupId.value = '';
  }
}

function onEqGroupChange() {
  if (selectedEqGroupId.value) {
    localStorage.setItem(STORAGE_KEY, selectedEqGroupId.value);
  }
  // watch 가 startPolling 을 호출하므로 여기서는 별도 호출 불필요
}

// ── 데이터 새로고침 ──
async function refresh() {
  // 화면을 나갔거나 폴링이 비활성화 상태면 즉시 중단
  if (!isPollingActive.value) return;

  if (!selectedEqGroupId.value) {
    try { simStatus.value = await getStatus(); } catch { /* ignore */ }
    // 그룹이 없어도 다음 폴링은 예약
    if (isPollingActive.value) {
      timer = setTimeout(refresh, REFRESH_MS);
    }
    return;
  }

  try {
    const groupId = selectedEqGroupId.value;
    const [sum, tp, p, hr, ip, st] = await Promise.all([
      getDashboardSummary(groupId),
      getDashboardThroughput(groupId, 30),
      getPorts(groupId),
      getHostRecent('all', groupId),
      getInProgress(groupId),
      getStatus(),
    ]);

    // API 응답이 왔을 때 화면을 이미 나갔을 수도 있으므로 한 번 더 체크
    if (!isPollingActive.value) return;

    summary.value    = sum;
    throughput.value = tp ?? [];
    ports.value      = p ?? [];
    hostRecent.value = hr ?? [];
    inProgress.value = ip ?? [];
    simStatus.value  = st;
  } catch (e) {
    console.error('[WCS-DASH] 새로고침 실패', e);
  } finally {
    // ▣ 핵심: 작업이 끝나면 다음 폴링을 예약 (재귀적 호출)
    if (isPollingActive.value) {
      timer = setTimeout(refresh, REFRESH_MS);
    }
  }
}

async function refreshSimStatus() {
  try { simStatus.value = await getStatus(); } catch { /* ignore */ }
}

// 폴링 시작
function startPolling() {
  stopPolling(); // 중복 방지
  isPollingActive.value = true;
  refresh();
}

// 폴링 중지
function stopPolling() {
  isPollingActive.value = false;
  if (timer) {
    clearTimeout(timer);
    timer = null;
  }
}

// ── 생명주기 관리 ──

// 일반적인 언마운트 (페이지 파괴)
onUnmounted(() => {
  stopPolling();
});

// KeepAlive를 위해 비활성화될 때
onDeactivated(() => {
  stopPolling();
});

// 다시 화면으로 돌아올 때 (KeepAlive용 포함)
onActivated(() => {
  startPolling();
});

onMounted(async () => {
  await loadEqGroups();
  startPolling();
});

// 그룹이 바뀌면 타이머를 리셋하고 즉시 갱신
watch(selectedEqGroupId, (v) => {
  if (v) startPolling();
});
</script>

<style lang="scss" scoped>
@use './styles/sim-tokens' as *;

.wcs-dashboard {
  display: flex;
  flex-direction: column;
  gap: $sim-space-lg;
  padding: $sim-space-xl;
  min-height: 100%;
  color: $sim-text-primary;
  background: $sim-bg-page;
}

.wcs-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: $sim-space-lg;
  flex-wrap: wrap;

  .title-block {
    .title {
      margin: 0;
      font-size: 24px;
      font-weight: 700;
      color: $sim-text-primary;
      line-height: 1.2;
    }
    .subtitle {
      margin: 4px 0 0 0;
      font-size: 13px;
      color: $sim-text-muted;
    }
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: $sim-space-md;
  flex-wrap: wrap;
}

// ── 그룹 선택 ──
.group-select {
  display: inline-flex;
  align-items: center;
  gap: $sim-space-sm;

  .g-lbl {
    font-size: 12px;
    color: $sim-text-muted;
    font-weight: 600;
  }

  .g-sel {
    height: 36px;
    min-width: 160px;
    padding: 0 12px;
    background: $sim-bg-elevated;
    color: $sim-text-primary;
    border: 1px solid $sim-border-default;
    border-radius: $sim-radius-md;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: border-color $sim-transition-fast;

    &:hover:not(:disabled) { border-color: $sim-border-strong; }
    &:focus { outline: none; border-color: $sim-color-host; }
    &:disabled { opacity: 0.5; cursor: not-allowed; }
  }
}

// ── 그룹 미선택 안내 ──
.no-group {
  @include sim-card;
  text-align: center;
  padding: $sim-space-xl * 2 0;
  color: $sim-text-muted;
  font-size: 14px;
}

.sim-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: $sim-radius-pill;
  font-size: 12px;
  font-weight: 600;
  background: rgba($sim-state-running, 0.15);
  color: $sim-state-running;
  border: 1px solid rgba($sim-state-running, 0.4);

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: $sim-state-running;
    box-shadow: 0 0 8px rgba($sim-state-running, 0.6);
    animation: pulse 1.5s ease-in-out infinite;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.4; }
}

.btn-sim {
  height: 36px;
  padding: 0 18px;
  border: none;
  border-radius: $sim-radius-md;
  background: $sim-color-host;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: filter $sim-transition-fast, transform $sim-transition-fast;

  &:hover:not(:disabled)  { filter: brightness(1.1); }
  &:active:not(:disabled) { transform: translateY(1px); }
  &:disabled              { opacity: 0.4; cursor: not-allowed; }
}

.grid-2 {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: $sim-space-lg;

  @media (max-width: 1100px) { grid-template-columns: minmax(0, 1fr); }
}
</style>
