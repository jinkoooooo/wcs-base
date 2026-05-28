<template>
  <Teleport to="body">
    <transition name="sim-modal-fade">
      <div v-if="open" class="sim-modal-root" role="dialog" aria-modal="true"
           @mousedown.self="closeOnBackdrop">
        <div class="sim-modal-card" @mousedown.stop>
          <header class="sim-modal-head">
            <div class="head-title">
              <span class="logo">🤖</span>
              <span>WCS 시뮬레이터</span>
              <span v-if="eqGroupId" class="group-tag">{{ eqGroupId }}</span>
              <span v-if="simRunning" class="badge-on">
                <span class="dot" /> 가동 중
              </span>
              <span v-else class="badge-off">정지됨</span>
            </div>
            <button class="btn-close" aria-label="닫기"
                    @click="$emit('update:open', false)">✕</button>
          </header>

          <div class="sim-modal-body">
            <div v-if="!eqGroupId" class="no-group-warn">
              ⚠ 설비 그룹이 선택되지 않았습니다. 통합 대시보드에서 그룹을 먼저 선택하세요.
            </div>

            <!-- 일괄 제어 -->
            <section class="bulk-row">
              <div class="info">
                <p class="lbl">전체 일괄 제어</p>
                <p class="hint">
                  HOST + PLC 를 한 번에 시작/정지합니다
                  <span v-if="eqGroupId"> (대상: <strong>{{ eqGroupId }}</strong>)</span>
                </p>
              </div>
              <div class="actions">
                <button
                  class="btn btn-stop"
                  :disabled="!simRunning || !eqGroupId"
                  @click="onStopAll"
                >
                  ■ 모두 정지
                </button>
                <button
                  class="btn btn-start"
                  :disabled="simRunning || !eqGroupId"
                  @click="onStartAll"
                >
                  ▶ 모두 시작
                </button>
              </div>
            </section>

            <!-- 개별 토글: HOST -->
            <section class="toggle-row" :class="{ active: hostRunning }">
              <div class="toggle-info">
                <div class="toggle-title">
                  <span class="role-tag host">HOST</span>
                  <span class="role-name">호스트(WMS) 시뮬</span>
                  <span v-if="hostRunning" class="state-dot on" />
                  <span v-else class="state-dot off" />
                  <span v-if="hostRunning && phaseTarget > 0" class="phase-tag">
                    {{ currentPhase }} {{ phaseSubmitted }}/{{ phaseTarget }}
                  </span>
                </div>
                <p class="toggle-hint">
                  HOST 주문을 자동 생성합니다 (host_system_code = 'WMS-SIM')
                </p>
              </div>
              <button
                class="btn-toggle"
                :class="hostRunning ? 'on' : 'off'"
                :disabled="!hostRunning && !eqGroupId"
                @click="onToggleHost"
              >
                {{ hostRunning ? '■ 정지' : '▶ 시작' }}
              </button>
            </section>

            <!-- 개별 토글: PLC -->
            <section class="toggle-row" :class="{ active: plcRunning }">
              <div class="toggle-info">
                <div class="toggle-title">
                  <span class="role-tag plc">PLC</span>
                  <span class="role-name">ECS/PLC 콜백 시뮬</span>
                  <span v-if="plcRunning" class="state-dot on" />
                  <span v-else class="state-dot off" />
                  <span v-if="plcInFlight > 0" class="inflight">
                    처리중 {{ plcInFlight }}건
                  </span>
                </div>
                <p class="toggle-hint">
                  실제 PLC 대신 셔틀/컨베이어 콜백을 자동 발사합니다.
                  <strong v-if="hostRunning && !plcRunning" class="warn">
                    ⚠ 외부 시뮬레이터(3D 등)에서 콜백을 직접 보낼 때 끄세요
                  </strong>
                </p>
              </div>
              <button
                class="btn-toggle"
                :class="plcRunning ? 'on' : 'off'"
                :disabled="!plcRunning && !eqGroupId"
                @click="onTogglePlc"
              >
                {{ plcRunning ? '■ 정지' : '▶ 시작' }}
              </button>
            </section>

            <SimControlPanel
              :status="status"
              :eq-group-id="eqGroupId"
              @changed="refresh"
            />

          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue';
import SimControlPanel from './SimControlPanel.vue';
import {
  getStatus,
  startAll,
  stopAll,
  startHost,
  stopHost,
  startPlc,
  stopPlc,
  type SimulatorStatus,
  type SimulatorGroupStatus,
} from '../api';

const props = defineProps<{
  open: boolean;
  eqGroupId: string;
}>();

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void;
  (e: 'changed'): void;
}>();

const REFRESH_MS = 2000;
const status = ref<SimulatorStatus | null>(null);

/** 현재 선택된 그룹의 상태 한 행. 없으면 null. */
const myGroup = computed<SimulatorGroupStatus | null>(() => {
  if (!status.value || !props.eqGroupId) return null;
  return status.value.groups.find(g => g.eqGroupId === props.eqGroupId) ?? null;
});

const hostRunning    = computed(() => myGroup.value?.hostRunning ?? false);
const plcRunning     = computed(() => myGroup.value?.plcRunning ?? false);
const currentPhase   = computed(() => myGroup.value?.currentPhase ?? '');
const phaseSubmitted = computed(() => myGroup.value?.phaseSubmitted ?? 0);
const phaseTarget    = computed(() => myGroup.value?.phaseTarget ?? 0);
const plcInFlight    = computed(() => status.value?.plcInFlight ?? 0);

const simRunning = computed(() => hostRunning.value || plcRunning.value);

let timer: ReturnType<typeof setInterval> | null = null;

async function refresh() {
  try {
    status.value = await getStatus();
    emit('changed');
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error('[SIM-MODAL] status 조회 실패', e);
  }
}

async function onStartAll() {
  if (!props.eqGroupId) return;
  await startAll(props.eqGroupId);
  await refresh();
}

async function onStopAll() {
  if (!props.eqGroupId) return;
  await stopAll(props.eqGroupId);
  await refresh();
}

async function onToggleHost() {
  if (!props.eqGroupId) return;
  if (hostRunning.value) {
    await stopHost(props.eqGroupId);
  } else {
    await startHost(props.eqGroupId);
  }
  await refresh();
}

async function onTogglePlc() {
  if (!props.eqGroupId) return;
  if (plcRunning.value) {
    await stopPlc(props.eqGroupId);
  } else {
    await startPlc(props.eqGroupId);
  }
  await refresh();
}

function startPolling() {
  if (timer) return;
  refresh();
  timer = setInterval(refresh, REFRESH_MS);
}
function stopPolling() {
  if (timer) { clearInterval(timer); timer = null; }
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.open) emit('update:open', false);
}
function closeOnBackdrop() {
  emit('update:open', false);
}

watch(
  () => props.open,
  (now) => {
    if (now) {
      startPolling();
      document.body.style.overflow = 'hidden';
    } else {
      stopPolling();
      document.body.style.overflow = '';
    }
  },
  { immediate: true },
);

onMounted(() => {
  document.addEventListener('keydown', onKey);
});
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey);
  stopPolling();
  document.body.style.overflow = '';
});
</script>

<style lang="scss" scoped>
@use '../styles/sim-tokens' as *;

/* 기존 스타일 그대로, phase-tag 만 추가 */

.sim-modal-root {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 48px 24px;
  overflow-y: auto;
}

.sim-modal-card {
  width: 100%;
  max-width: 720px;
  background: $sim-bg-page;
  color: $sim-text-primary;
  border: 1px solid $sim-border-default;
  border-radius: 12px;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 96px);
}

.sim-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid $sim-border-default;
  background: $sim-bg-elevated;
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;

  .head-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 16px;
    font-weight: 700;
    color: $sim-text-primary;
    flex-wrap: wrap;
    .logo { font-size: 18px; }
  }

  .group-tag {
    padding: 2px 10px;
    border-radius: $sim-radius-pill;
    font-size: 12px;
    font-weight: 600;
    background: rgba($sim-color-host, 0.18);
    color: $sim-color-host;
    border: 1px solid rgba($sim-color-host, 0.4);
  }

  .badge-on {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 2px 12px;
    border-radius: $sim-radius-pill;
    font-size: 12px;
    font-weight: 600;
    background: rgba($sim-state-running, 0.15);
    color: $sim-state-running;
    border: 1px solid rgba($sim-state-running, 0.4);

    .dot {
      width: 8px; height: 8px; border-radius: 50%;
      background: $sim-state-running;
      box-shadow: 0 0 8px rgba($sim-state-running, 0.6);
    }
  }
  .badge-off {
    padding: 2px 12px;
    border-radius: $sim-radius-pill;
    font-size: 12px;
    font-weight: 600;
    background: rgba($sim-state-idle, 0.15);
    color: $sim-text-muted;
    border: 1px solid rgba($sim-state-idle, 0.4);
  }
}

.btn-close {
  width: 32px; height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: $sim-text-secondary;
  font-size: 16px;
  cursor: pointer;
  transition: background $sim-transition-fast, color $sim-transition-fast;
  &:hover { background: rgba(255,255,255,0.08); color: $sim-text-primary; }
}

.sim-modal-body {
  padding: 20px 24px 24px;
  display: flex;
  flex-direction: column;
  gap: $sim-space-md;
  overflow-y: auto;
}

.no-group-warn {
  padding: $sim-space-md $sim-space-lg;
  background: rgba($sim-state-warning, 0.12);
  border: 1px solid rgba($sim-state-warning, 0.4);
  border-radius: $sim-radius-md;
  color: $sim-state-warning;
  font-size: 13px;
  font-weight: 600;
}

.bulk-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $sim-space-lg;
  padding: $sim-space-md $sim-space-lg;
  background: $sim-bg-card;
  border: 1px solid $sim-border-default;
  border-radius: $sim-radius-lg;
  flex-wrap: wrap;

  .info {
    .lbl { margin: 0; font-size: 13px; font-weight: 600; color: $sim-text-primary; }
    .hint {
      margin: 4px 0 0 0;
      font-size: 12px;
      color: $sim-text-muted;
      strong { color: $sim-text-primary; font-weight: 700; }
    }
  }
  .actions { display: flex; gap: $sim-space-sm; }
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $sim-space-lg;
  padding: $sim-space-md $sim-space-lg;
  background: $sim-bg-card;
  border: 1px solid $sim-border-default;
  border-radius: $sim-radius-lg;
  transition: border-color $sim-transition-fast, background $sim-transition-fast;

  &.active {
    border-color: rgba($sim-state-running, 0.5);
    background: linear-gradient(180deg,
      rgba($sim-state-running, 0.06) 0%,
      $sim-bg-card 100%);
  }
}

.toggle-info { flex: 1; min-width: 0; }

.toggle-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;

  .role-tag {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.5px;
    color: #fff;
    &.host { background: $sim-color-host; }
    &.plc  { background: $sim-color-portmode; }
  }

  .role-name { font-size: 14px; font-weight: 600; color: $sim-text-primary; }

  .state-dot {
    width: 8px; height: 8px;
    border-radius: 50%;
    &.on  { background: $sim-state-running; box-shadow: 0 0 8px rgba($sim-state-running,0.6); }
    &.off { background: $sim-state-idle; }
  }

  .inflight {
    margin-left: 4px;
    padding: 2px 8px;
    border-radius: $sim-radius-pill;
    background: rgba($sim-color-portmode, 0.15);
    color: $sim-color-portmode;
    font-size: 11px;
    font-weight: 600;
  }

  /* ★ 추가 — 페이즈 진행 표시 */
  .phase-tag {
    margin-left: 4px;
    padding: 2px 8px;
    border-radius: $sim-radius-pill;
    background: rgba($sim-color-host, 0.15);
    color: $sim-color-host;
    font-size: 11px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }
}

.toggle-hint {
  margin: 6px 0 0 0;
  font-size: 12px;
  color: $sim-text-muted;
  line-height: 1.5;

  .warn {
    display: inline-block;
    margin-left: 4px;
    color: $sim-color-portmode;
    font-weight: 600;
  }
}

.btn-toggle {
  flex-shrink: 0;
  min-width: 96px;
  height: 38px;
  padding: 0 18px;
  border: none;
  border-radius: $sim-radius-md;
  color: #fff;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: filter $sim-transition-fast, transform $sim-transition-fast;
  &:hover:not(:disabled) { filter: brightness(1.1); }
  &:active:not(:disabled) { transform: translateY(1px); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
  &.on  { background: $sim-state-stopped; }
  &.off { background: $sim-state-running; }
}

.btn {
  height: 38px;
  padding: 0 20px;
  border: none;
  border-radius: $sim-radius-md;
  color: #fff;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: filter $sim-transition-fast;
  &:hover:not(:disabled) { filter: brightness(1.1); }
  &:disabled { opacity: 0.35; cursor: not-allowed; }
  &.btn-start { background: $sim-state-running; }
  &.btn-stop  { background: $sim-state-stopped; }
}

.sim-modal-fade-enter-active,
.sim-modal-fade-leave-active {
  transition: opacity 0.18s ease;
  .sim-modal-card { transition: transform 0.22s ease; }
}
.sim-modal-fade-enter-from,
.sim-modal-fade-leave-to {
  opacity: 0;
  .sim-modal-card { transform: translateY(-12px) scale(0.98); }
}
</style>
