<!--
  HOST 주문 처리 흐름 (오늘 누적): 접수 → 검증 → 할당 → 실행 → 완료.
  오류/취소는 별도 칼럼.
-->
<template>
  <div class="pipeline-card">
    <div class="head">
      <span class="title">🔁 호스트 주문 처리 흐름</span>
      <span class="hint">오늘 누적 — 각 단계까지 도달한 건수 (funnel)</span>
    </div>

    <div class="pipeline">
      <template v-for="(s, i) in stages" :key="s.key">
        <div class="stage" :style="{ '--c': s.color }">
          <div class="stage-icon">{{ s.icon }}</div>
          <div class="stage-meta">
            <div class="stage-lbl">{{ s.label }}</div>
            <div class="stage-val">{{ s.count }}</div>
          </div>
        </div>
        <div v-if="i < stages.length - 1" class="arrow">›</div>
      </template>

      <div v-if="errorCount > 0" class="error-stage">
        <span class="icon">⚠</span>
        <div class="meta">
          <div class="lbl">오류/취소</div>
          <div class="val">{{ errorCount }}</div>
        </div>
      </div>
    </div>

    <div class="totals">
      <span class="t-row">총 접수 <strong>{{ pipeline?.total ?? 0 }}</strong></span>
      <span class="dot" />
      <span class="t-row">완료 <strong>{{ pipeline?.completed ?? 0 }}</strong></span>
      <span class="dot" />
      <span class="t-row">완료율 <strong>{{ completionRate }}%</strong></span>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import type { HostPipeline } from '../api';

  const props = defineProps<{ pipeline: HostPipeline | null }>();

  const stages = computed(() => [
    { key: 'received',  icon: '📨', label: '접수', count: props.pipeline?.received  ?? 0, color: '#64748b' },
    { key: 'validated', icon: '✅', label: '검증', count: props.pipeline?.validated ?? 0, color: '#06b6d4' },
    { key: 'allocated', icon: '🎯', label: '할당', count: props.pipeline?.allocated ?? 0, color: '#3b82f6' },
    { key: 'executing', icon: '⚙️', label: '실행', count: props.pipeline?.executing ?? 0, color: '#f59e0b' },
    { key: 'completed', icon: '🏁', label: '완료', count: props.pipeline?.completed ?? 0, color: '#22c55e' },
  ]);

  const errorCount = computed(() => props.pipeline?.error ?? 0);

  const completionRate = computed(() => {
    const total = props.pipeline?.total ?? 0;
    const done = props.pipeline?.completed ?? 0;
    if (total === 0) return 0;
    return Math.round((done / total) * 100);
  });
</script>

<style lang="scss" scoped>
  @use '../styles/sim-tokens' as *;

  .pipeline-card {
    background: $sim-bg-card;
    border: 1px solid $sim-border-default;
    border-radius: $sim-radius-lg;
    padding: $sim-space-lg $sim-space-xl;
    display: flex;
    flex-direction: column;
    gap: $sim-space-md;
  }

  .head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: $sim-space-md;
    flex-wrap: wrap;
    padding-bottom: $sim-space-sm;
    border-bottom: 1px solid $sim-border-default;

    .title { color: $sim-text-primary; font-weight: 600; font-size: 15px; }
    .hint  { color: $sim-text-muted;  font-size: 12px; }
  }

  .pipeline {
    display: flex;
    align-items: stretch;
    flex-wrap: wrap;
    gap: $sim-space-sm;
    padding: $sim-space-sm 0;
  }

  .stage {
    flex: 1 1 130px;
    min-width: 120px;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 14px;
    background: $sim-bg-elevated;
    border-radius: $sim-radius-md;
    border-left: 3px solid var(--c, #64748b);

    .stage-icon { font-size: 20px; line-height: 1; }
    .stage-meta {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .stage-lbl {
        font-size: 11px;
        color: $sim-text-muted;
        line-height: 1;
      }
      .stage-val {
        font-size: 20px;
        font-weight: 700;
        color: var(--c);
        font-variant-numeric: tabular-nums;
        line-height: 1;
      }
    }
  }

  .arrow {
    display: flex;
    align-items: center;
    font-size: 24px;
    color: $sim-text-subtle;
    line-height: 1;
    flex: 0 0 auto;
  }

  .error-stage {
    flex: 1 1 130px;
    min-width: 120px;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 14px;
    background: rgba($sim-state-stopped, 0.12);
    border-radius: $sim-radius-md;
    border-left: 3px solid $sim-state-stopped;
    margin-left: $sim-space-sm;

    .icon { font-size: 20px; color: $sim-state-stopped; line-height: 1; }
    .meta {
      display: flex; flex-direction: column; gap: 2px;
      .lbl { font-size: 11px; color: $sim-text-muted; line-height: 1; }
      .val {
        font-size: 20px; font-weight: 700;
        color: $sim-state-stopped;
        font-variant-numeric: tabular-nums;
        line-height: 1;
      }
    }
  }

  .totals {
    display: flex;
    align-items: center;
    gap: $sim-space-md;
    font-size: 13px;
    color: $sim-text-muted;
    padding-top: $sim-space-sm;
    border-top: 1px solid $sim-border-default;

    strong {
      color: $sim-text-primary;
      font-weight: 700;
      margin-left: 4px;
      font-variant-numeric: tabular-nums;
    }

    .dot {
      width: 3px; height: 3px; border-radius: 50%;
      background: $sim-text-subtle;
    }
  }
</style>
