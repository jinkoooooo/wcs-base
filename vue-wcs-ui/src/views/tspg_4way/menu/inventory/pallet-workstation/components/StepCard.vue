<template>
  <div class="step-card" :class="cardClass">
    <div class="sc-header">
      <div v-if="step.badgeLabel" class="sc-scenario-row">
        <span class="sc-scenario-label" :class="`sc-scenario-${step.badgeTone}`">
          {{ step.badgeLabel }} 시나리오
        </span>
        <span class="sc-step-progress">
          {{ step.stepIndex + 1 }} / {{ step.totalSteps }} 단계
        </span>
      </div>
      <h2 class="sc-title">{{ step.title }}</h2>
      <p v-if="step.hint" class="sc-hint">{{ step.hint }}</p>
    </div>

    <!-- 셔틀 진행 미니바 (송신/운송/도착 같은 ECS 자동 구간 시각화) -->
    <div v-if="step.shuttleProgress" class="sc-shuttle">
      <template v-for="(stage, i) in step.shuttleProgress.stages" :key="i">
        <div class="sc-shuttle-stage" :class="shuttleStageClass(i)">
          <span class="sc-shuttle-dot"></span>
          <span class="sc-shuttle-label">{{ stage }}</span>
        </div>
        <span
          v-if="i < step.shuttleProgress.stages.length - 1"
          class="sc-shuttle-bar"
          :class="i < step.shuttleProgress.currentStage ? 'sc-shuttle-bar-done' : ''"
        ></span>
      </template>
    </div>

    <!-- 박스 입력 칸 -->
    <div v-if="step.primaryInput === 'box'" class="sc-input-row">
      <input
        :ref="boxInputRef"
        :value="box"
        placeholder="박스 바코드 스캔 (B-...)"
        class="sc-box-input"
        :disabled="!canScan"
        @keydown.enter="$emit('scan')"
        @input="$emit('update:box', ($event.target as HTMLInputElement).value)"
      />
      <button
        type="button"
        class="sc-confirm-btn"
        :disabled="!canScan || !box.trim()"
        @click="$emit('scan')"
      >
        확인
      </button>
    </div>

    <!-- 진행률 바 -->
    <div v-if="step.showProgressBar && (step.progressMax ?? 0) > 0" class="sc-progress">
      <div class="sc-progress-info">
        <span class="sc-progress-stats">{{ step.progressValue }} / {{ step.progressMax }}</span>
        <span class="sc-progress-percent">{{ percent }}%</span>
      </div>
      <div class="sc-progress-bar">
        <div
          class="sc-progress-fill"
          :class="[
            `sc-progress-${step.progressColor ?? 'in'}`,
            percent === 100 ? 'sc-progress-done' : '',
          ]"
          :style="{ width: percent + '%' }"
        ></div>
      </div>
    </div>

    <!-- 주 CTA 버튼 -->
    <div v-if="step.primaryAction" class="sc-cta">
      <button
        type="button"
        class="sc-cta-btn"
        :class="[
          `sc-cta-${step.primaryAction.variant ?? 'primary'}`,
          { 'sc-cta-loading': step.primaryAction.loading },
        ]"
        :disabled="step.primaryAction.disabled || step.primaryAction.loading"
        :title="step.primaryAction.tooltip ?? ''"
        @click="onCtaClick"
      >
        <span v-if="step.primaryAction.loading" class="sc-cta-spinner" aria-hidden="true"></span>
        <span class="sc-cta-label">
          {{ step.primaryAction.loading ? '처리 중...' : step.primaryAction.label }}
        </span>
      </button>
    </div>

    <!-- 관리자 우회 — admin 인 경우에만 노출. 비가역 작업이므로 색상 톤 분리. -->
    <div v-if="step.adminActions.length > 0" class="sc-admin">
      <span class="sc-admin-label">관리자 도구</span>
      <button
        v-for="aa in step.adminActions"
        :key="aa.actionId"
        type="button"
        class="sc-admin-btn"
        @click="$emit('run-action', aa.actionId)"
      >
        {{ aa.label }}
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { StepDescriptor, ActionId } from '../scenarios';

const props = defineProps<{
  step: StepDescriptor;
  box: string;
  canScan: boolean;
  boxInputRef: any;
}>();

const emit = defineEmits<{
  (e: 'update:box', v: string): void;
  (e: 'scan'): void;
  (e: 'run-action', id: ActionId): void;
}>();

const cardClass = computed(() => {
  if (props.step.primaryAction?.variant === 'success') return 'sc-tone-success';
  if (props.step.scenario === 'SAMPLE_CYCLE') return 'sc-tone-sample';
  if (props.step.scenario === 'OUTBOUND_NORMAL' || props.step.scenario === 'OUTBOUND_PARTIAL_OUT') {
    return 'sc-tone-out';
  }
  if (props.step.scenario === 'INBOUND_REINBOUND') return 'sc-tone-reinbound';
  return 'sc-tone-in';
});

const percent = computed(() => {
  const v = props.step.progressValue ?? 0;
  const m = props.step.progressMax ?? 0;
  return m > 0 ? Math.min(100, Math.round((v / m) * 100)) : 0;
});

function shuttleStageClass(idx: number): string {
  const cur = props.step.shuttleProgress!.currentStage;
  if (idx < cur) return 'sc-shuttle-done';
  if (idx === cur) return 'sc-shuttle-current';
  return 'sc-shuttle-todo';
}

function onCtaClick() {
  const aid = props.step.primaryAction?.actionId;
  if (!aid) return;
  emit('run-action', aid);
}
</script>

<style scoped>
.step-card {
  background: var(--c-card, #ffffff);
  border-radius: 12px;
  border: 1px solid var(--c-border, #e8eaed);
  padding: 18px 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.05);
  flex: 0 0 auto;
}

/* 톤별 좌측 보더 (시나리오 분위기) */
.step-card.sc-tone-in {
  border-left: 3px solid var(--c-primary, #3182f6);
}
.step-card.sc-tone-out {
  border-left: 3px solid var(--c-warning, #f59e0b);
}
.step-card.sc-tone-sample {
  border-left: 3px solid var(--c-sample, #ec4899);
}
.step-card.sc-tone-reinbound {
  border-left: 3px solid #8b5cf6;
}
.step-card.sc-tone-success {
  background: linear-gradient(135deg, #ecfdf5 0%, #ffffff 100%);
  border-color: var(--c-success, #16a34a);
  border-left: 3px solid var(--c-success, #16a34a);
}

.sc-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.sc-scenario-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.sc-scenario-label {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
  letter-spacing: -0.2px;
}
.sc-scenario-idle {
  background: #f1f5f9;
  color: var(--c-muted, #94a3b8);
}
.sc-scenario-in {
  background: #dbeafe;
  color: #1e40af;
}
.sc-scenario-out {
  background: #fef3c7;
  color: #92400e;
}
.sc-scenario-sample {
  background: #fce7f3;
  color: #be185d;
}
.sc-scenario-reinbound {
  background: #ede9fe;
  color: #5b21b6;
}
.sc-scenario-success {
  background: #dcfce7;
  color: #15803d;
}
.sc-step-progress {
  font-size: 10px;
  font-weight: 700;
  color: var(--c-muted, #94a3b8);
  letter-spacing: 0.3px;
}
.sc-title {
  font-size: 19px;
  font-weight: 800;
  color: var(--c-text, #0f172a);
  margin: 0;
  letter-spacing: -0.4px;
  line-height: 1.3;
}
.sc-hint {
  font-size: 13px;
  color: var(--c-text-2, #475569);
  margin: 0;
  line-height: 1.5;
}

/* 셔틀 미니바 */
.sc-shuttle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid var(--c-border, #e8eaed);
  border-radius: 8px;
}
.sc-shuttle-stage {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
}
.sc-shuttle-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  transition: all 0.25s;
}
.sc-shuttle-label {
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.sc-shuttle-todo .sc-shuttle-dot {
  background: #e2e8f0;
}
.sc-shuttle-todo .sc-shuttle-label {
  color: var(--c-muted, #94a3b8);
}
.sc-shuttle-current .sc-shuttle-dot {
  background: var(--c-primary, #3182f6);
  box-shadow: 0 0 0 3px rgba(49, 130, 246, 0.2);
  animation: shuttle-pulse 1.6s ease-in-out infinite;
}
.sc-shuttle-current .sc-shuttle-label {
  color: var(--c-primary, #3182f6);
  font-weight: 700;
}
.sc-shuttle-done .sc-shuttle-dot {
  background: var(--c-success, #16a34a);
}
.sc-shuttle-done .sc-shuttle-label {
  color: var(--c-success, #16a34a);
}
.sc-shuttle-bar {
  flex: 1;
  min-width: 12px;
  height: 1px;
  background: #e2e8f0;
  transition: background 0.25s;
}
.sc-shuttle-bar-done {
  background: var(--c-success, #16a34a);
}

@keyframes shuttle-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.55; }
}

/* 박스 입력 */
.sc-input-row {
  display: flex;
  gap: 8px;
  align-items: stretch;
}
.sc-box-input {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  height: 44px;
  padding: 0 14px;
  background: #fff;
  border: 1.5px solid var(--c-border, #e8eaed);
  border-radius: 8px;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  color: var(--c-text, #0f172a);
}
.sc-box-input:focus {
  border-color: var(--c-primary, #3182f6);
  box-shadow: 0 0 0 3px rgba(49, 130, 246, 0.15);
}
.sc-box-input:disabled {
  background: #f1f5f9;
  color: var(--c-muted, #94a3b8);
  cursor: not-allowed;
}
.sc-confirm-btn {
  flex: 0 0 auto;
  height: 44px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 800;
  background: var(--c-primary, #3182f6);
  color: #fff;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.sc-confirm-btn:hover:not(:disabled) {
  background: #2667d4;
}
.sc-confirm-btn:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

/* 진행률 바 */
.sc-progress {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.sc-progress-info {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 12px;
}
.sc-progress-stats {
  font-weight: 700;
  color: var(--c-text, #0f172a);
  font-size: 14px;
  font-variant-numeric: tabular-nums;
}
.sc-progress-percent {
  color: var(--c-muted, #94a3b8);
  font-size: 11px;
}
.sc-progress-bar {
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}
.sc-progress-fill {
  height: 100%;
  transition: width 0.3s ease;
  border-radius: 4px;
}
.sc-progress-in {
  background: var(--c-primary, #3182f6);
}
.sc-progress-out {
  background: var(--c-warning, #f59e0b);
}
.sc-progress-done {
  background: var(--c-success, #16a34a) !important;
}

/* CTA */
.sc-cta {
  display: flex;
}
.sc-cta-btn {
  width: 100%;
  height: 52px;
  font-size: 15px;
  font-weight: 800;
  border: 0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  letter-spacing: -0.2px;
}
.sc-cta-primary {
  background: var(--c-primary, #3182f6);
  color: #fff;
}
.sc-cta-primary:hover:not(:disabled) {
  background: #2667d4;
  box-shadow: 0 4px 12px rgba(49, 130, 246, 0.3);
}
.sc-cta-success {
  background: var(--c-success, #16a34a);
  color: #fff;
}
.sc-cta-success:hover:not(:disabled) {
  background: #128039;
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3);
}
.sc-cta-ghost {
  background: var(--c-card, #fff);
  color: var(--c-text, #0f172a);
  border: 1.5px solid var(--c-border, #e8eaed);
}
.sc-cta-ghost:hover:not(:disabled) {
  background: #f1f5f9;
}
.sc-cta-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}
.sc-cta-btn.sc-cta-loading {
  opacity: 0.85;
  cursor: progress;
}
.sc-cta-label {
  display: inline-flex;
  align-items: center;
}
.sc-cta-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: sc-spin 0.6s linear infinite;
  margin-right: 10px;
  vertical-align: -3px;
}
@keyframes sc-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 관리자 우회 영역 */
.sc-admin {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #fef2f2;
  border: 1px dashed #fca5a5;
  border-radius: 8px;
}
.sc-admin-label {
  font-size: 10px;
  font-weight: 800;
  color: #991b1b;
  letter-spacing: 0.4px;
  white-space: nowrap;
  flex-shrink: 0;
}
.sc-admin-btn {
  background: var(--c-card, #fff);
  border: 1px solid var(--c-danger, #ef4444);
  color: var(--c-danger, #ef4444);
  padding: 5px 12px;
  font-size: 12px;
  font-weight: 700;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.sc-admin-btn:hover {
  background: var(--c-danger, #ef4444);
  color: #fff;
}
</style>
