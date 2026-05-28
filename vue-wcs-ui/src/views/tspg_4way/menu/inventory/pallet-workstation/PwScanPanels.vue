<template>
  <!-- 헤더 -->
  <header class="ws-header">
    <div class="ws-header-left">
      <h1>파렛트 작업대</h1>
      <span v-if="info" class="ws-host-info">
        <span class="ws-host-key">{{ info.hostOrderKey }}</span>
        <span v-if="info.eqGroupId" class="ws-meta">· {{ info.eqGroupId }}</span>
        <span v-if="info.ownerCode" class="ws-meta">· {{ info.ownerCode }}</span>
        <span v-if="info.testRequired" class="chip chip-test">시험 대상</span>
      </span>
      <ScenarioBadge v-if="info && step.badgeLabel" :label="step.badgeLabel" :tone="step.badgeTone" />
    </div>
    <HeaderToolbar
      :info="info"
      :can-update="canUpdate"
      :selected-count="selectedBoxIds.size"
      :auto-release="autoRelease"
      :printing="printing"
      @run-action="$emit('run-action', $event)"
    />
  </header>

  <!-- 파렛트 입력 바 -->
  <div class="ws-pallet-bar">
    <div class="pallet-input-wrap">
      <a-input
        :ref="palletInputRef"
        :value="pallet"
        placeholder="파렛트 바코드"
        size="large"
        autofocus
        allowClear
        @press-enter="$emit('load')"
        @update:value="$emit('update:pallet', $event)"
        class="ws-pallet-input"
      />
      <span class="focus-hint"><kbd>/</kbd> 포커스</span>
    </div>
    <a-button
      v-if="canShow"
      type="primary"
      size="large"
      :loading="loading"
      @click="$emit('load')"
    >
      조회
    </a-button>
  </div>

  <!-- 좌측 작업 패널 -->
  <section v-if="info" class="ws-work-panel">
    <MiniStepper
      v-if="step.totalSteps > 0"
      :total="step.totalSteps"
      :current="step.stepIndex"
      :tooltips="stepTooltips"
    />

    <StepCard
      :step="step"
      :box="box"
      :can-scan="canScanInput"
      :box-input-ref="boxInputRef"
      @update:box="$emit('update:box', $event)"
      @scan="$emit('scan')"
      @run-action="$emit('run-action', $event)"
    />

    <AuxDrawer
      :open="auxDrawerOpen"
      :actions="step.auxActions"
      @toggle="$emit('toggle-aux')"
      @run-action="$emit('run-action', $event)"
    />

    <HistoryDrawer
      :open="historyDrawerOpen"
      :lifecycle="lifecycle"
      @toggle="$emit('toggle-history')"
      @open-history="(k?: string) => $emit('open-history', k)"
    />

    <div v-if="msg" :class="['msg-banner', msgClass]">{{ msg }}</div>
  </section>
</template>

<script lang="ts" setup>
import ScenarioBadge from './components/ScenarioBadge.vue';
import HeaderToolbar from './components/HeaderToolbar.vue';
import MiniStepper from './components/MiniStepper.vue';
import StepCard from './components/StepCard.vue';
import AuxDrawer from './components/AuxDrawer.vue';
import HistoryDrawer from './components/HistoryDrawer.vue';
import type { LifecycleEntry } from './shared';
import type { StepDescriptor, ActionId } from './scenarios';

defineProps<{
  // ─── 로드된 데이터 ───
  info: any;
  lifecycle: LifecycleEntry[];

  // ─── 입력 ───
  pallet: string;
  box: string;
  palletInputRef: any;
  boxInputRef: any;

  // ─── 시나리오 / 스텝 ───
  step: StepDescriptor;
  stepTooltips: string[];
  canScanInput: boolean;

  // ─── 권한 ───
  canShow: boolean;
  canUpdate: boolean;

  // ─── 박스 선택 / 라벨 인쇄 상태 ───
  selectedBoxIds: Set<string>;
  printing: { pallet: boolean; boxes: boolean; mark: boolean; selected: boolean };
  autoRelease: boolean;

  // ─── 드로어 상태 ───
  auxDrawerOpen: boolean;
  historyDrawerOpen: boolean;

  // ─── 로딩 / 메시지 ───
  loading: boolean;
  msg: string;
  msgClass: string;
}>();

defineEmits<{
  (e: 'update:pallet', v: string): void;
  (e: 'update:box', v: string): void;
  (e: 'load'): void;
  (e: 'scan'): void;
  (e: 'open-history', orderKey?: string): void;
  (e: 'toggle-aux'): void;
  (e: 'toggle-history'): void;
  (e: 'run-action', id: ActionId): void;
}>();
</script>

<style scoped>
/* 헤더 */
.ws-header {
  flex: 0 0 auto;
  height: 48px;
  background: var(--c-card, #ffffff);
  border-bottom: 1px solid var(--c-border, #e8eaed);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  gap: 12px;
}
.ws-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex: 1;
}
.ws-header-left h1 {
  font-size: 15px;
  font-weight: 700;
  margin: 0;
  letter-spacing: -0.2px;
  flex-shrink: 0;
}
.ws-host-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  min-width: 0;
}
.ws-host-key {
  color: var(--c-text-2, #475569);
  font-weight: 500;
  background: #f1f5f9;
  padding: 3px 8px;
  border-radius: 5px;
  font-size: 12px;
  border: 1px solid #e2e8f0;
}
.ws-meta {
  color: var(--c-muted, #94a3b8);
  font-size: 12px;
}
.chip {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 4px;
  white-space: nowrap;
}
.chip-test {
  background: #fef3c7;
  color: #92400e;
}

/* 파렛트 입력 바 */
.ws-pallet-bar {
  flex: 0 0 auto;
  background: var(--c-card, #ffffff);
  border-bottom: 1px solid var(--c-border, #e8eaed);
  padding: 10px 20px;
  display: flex;
  gap: 8px;
  align-items: center;
}
.pallet-input-wrap {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}
.focus-hint {
  position: absolute;
  right: 36px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--c-muted, #94a3b8);
  pointer-events: none;
  z-index: 1;
}
.focus-hint kbd {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 10px;
  color: var(--c-text-2, #475569);
}
.ws-pallet-input {
  flex: 1;
}
.ws-pallet-input :deep(.ant-input) {
  font-size: 14px;
  font-weight: 500;
  padding-right: 70px;
  height: 40px;
  border-radius: 8px;
}

/* 좌측 작업 패널 */
.ws-work-panel {
  background: var(--c-bg, #f6f7f9);
  border-right: 1px solid var(--c-border, #e8eaed);
  padding: 14px;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}
.ws-work-panel::-webkit-scrollbar {
  width: 6px;
}
.ws-work-panel::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

/* 메시지 배너 */
.msg-banner {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.msg-banner.ok {
  background: #f0fdf4;
  color: var(--c-success, #16a34a);
  border-left: 3px solid var(--c-success, #16a34a);
}
.msg-banner.err {
  background: #fef2f2;
  color: var(--c-danger, #ef4444);
  border-left: 3px solid var(--c-danger, #ef4444);
}
</style>
