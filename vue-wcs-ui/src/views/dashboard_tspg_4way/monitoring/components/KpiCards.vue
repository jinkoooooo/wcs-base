<!--
  6 KPI 카드: 입고 / 출고 / 재고이동 / 재입고 + 포트 + 랙.
  주문 타입 4 카드는 같은 형태라서 v-for 로 처리.
-->
<template>
  <div class="kpi-grid">
    <!-- 주문 타입 4종 -->
    <div
      v-for="card in typeCards"
      :key="card.key"
      class="kpi-card"
      :class="card.className"
    >
      <div class="head">
        <span class="icon">{{ card.icon }}</span>
        <span class="label">{{ card.label }}</span>
      </div>
      <div class="value">
        <span class="big">{{ card.inProgress }}</span>
        <span class="unit">건</span>
      </div>
      <div class="sub">진행 중</div>
      <div class="divider" />
      <div class="row">
        <span class="lbl">오늘 누적</span>
        <span class="val">{{ card.today }}</span>
      </div>
    </div>

    <!-- 포트 -->
    <div class="kpi-card t-port">
      <div class="head">
        <span class="icon">🚪</span>
        <span class="label">포트</span>
      </div>
      <div class="value">
        <span class="big">{{ portInUse }}</span>
        <span class="slash"> / {{ ports.length }}</span>
      </div>
      <div class="sub">사용중</div>
      <div class="divider" />
      <div class="row">
        <span class="lbl">입고 모드</span>
        <span class="val">{{ portByMode.INBOUND }}</span>
      </div>
      <div class="row">
        <span class="lbl">출고 모드</span>
        <span class="val">{{ portByMode.OUTBOUND }}</span>
      </div>
    </div>

    <!-- 랙 -->
    <div class="kpi-card t-rack">
      <div class="head">
        <span class="icon">🗂</span>
        <span class="label">랙</span>
      </div>
      <div class="value">
        <span class="big">{{ summary?.rack.occupied ?? 0 }}</span>
        <span class="slash"> / {{ summary?.rack.total ?? 0 }}</span>
      </div>
      <div class="sub">점유 ({{ usagePct }}%)</div>
      <div class="usage-bar">
        <div class="usage-fill" :style="{ width: usagePct + '%' }" />
      </div>
      <div class="row">
        <span class="lbl">빈 자리</span>
        <span class="val ok">{{ summary?.rack.empty ?? 0 }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import type { DashboardSummary, PortRow } from '../api';

  const props = defineProps<{
    summary: DashboardSummary | null;
    ports: PortRow[];
  }>();

  const typeCards = computed(() => {
    const ip = props.summary?.inProgressByType;
    const td = props.summary?.todayByType;
    return [
      { key: 'INBOUND',  icon: '📥', label: '입고',     className: 't-inbound',  inProgress: ip?.INBOUND  ?? 0, today: td?.INBOUND  ?? 0 },
      { key: 'OUTBOUND', icon: '📤', label: '출고',     className: 't-outbound', inProgress: ip?.OUTBOUND ?? 0, today: td?.OUTBOUND ?? 0 },
      { key: 'MOVE',     icon: '🔄', label: '재고이동', className: 't-move',     inProgress: ip?.MOVE     ?? 0, today: td?.MOVE     ?? 0 },
      { key: 'PUTBACK',  icon: '↩️', label: '재입고',   className: 't-putback',  inProgress: ip?.PUTBACK  ?? 0, today: td?.PUTBACK  ?? 0 },
    ];
  });

  const portInUse = computed(
    () => props.ports.filter((p) => p.task_id || (p.active_task_count ?? 0) > 0).length,
  );

  const portByMode = computed(() => {
    const acc = { INBOUND: 0, OUTBOUND: 0 };
    for (const p of props.ports) {
      if (p.port_mode === 'INBOUND') acc.INBOUND++;
      else if (p.port_mode === 'OUTBOUND' || p.port_mode === 'OUTBOUND_PRIORITY') acc.OUTBOUND++;
    }
    return acc;
  });

  const usagePct = computed(() => {
    const total = props.summary?.rack.total ?? 0;
    const occ = props.summary?.rack.occupied ?? 0;
    if (total === 0) return 0;
    return Math.round((occ / total) * 100);
  });
</script>

<style lang="scss" scoped>
  @use '../styles/sim-tokens' as *;

  .kpi-grid {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: $sim-space-md;

    @media (max-width: 1280px) { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    @media (max-width: 720px)  { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  }

  .kpi-card {
    background: $sim-bg-card;
    border: 1px solid $sim-border-default;
    border-radius: $sim-radius-lg;
    padding: $sim-space-md $sim-space-lg;
    display: flex;
    flex-direction: column;
    gap: 6px;
    border-top: 3px solid transparent;
    transition: background $sim-transition-normal,
                transform $sim-transition-normal,
                border-color $sim-transition-normal;

    &:hover {
      background: $sim-bg-card-hover;
      border-color: $sim-border-strong;
      transform: translateY(-1px);
    }

    &.t-inbound  { border-top-color: $sim-type-inbound; }
    &.t-outbound { border-top-color: $sim-type-outbound; }
    &.t-move     { border-top-color: $sim-type-move; }
    &.t-putback  { border-top-color: $sim-type-putback; }
    &.t-port     { border-top-color: $sim-color-portmode; }
    &.t-rack     { border-top-color: $sim-color-rack; }
  }

  .head {
    display: flex;
    align-items: center;
    gap: 6px;

    .icon { font-size: 16px; line-height: 1; }
    .label { font-size: 13px; color: $sim-text-muted; font-weight: 600; }
  }

  .value {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-top: 4px;

    .big {
      font-size: 30px;
      font-weight: 700;
      color: $sim-text-primary;
      line-height: 1;
      font-variant-numeric: tabular-nums;
    }
    .unit, .slash { font-size: 13px; color: $sim-text-secondary; }
  }
  .sub {
    font-size: 12px;
    color: $sim-text-muted;
  }

  .divider {
    height: 1px;
    background: $sim-border-default;
    margin: 4px 0;
  }

  .row {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    font-variant-numeric: tabular-nums;

    .lbl { color: $sim-text-muted; }
    .val { color: $sim-text-primary; font-weight: 600; }
    .val.ok { color: $sim-state-running; }
  }

  .usage-bar {
    height: 4px;
    background: $sim-bg-elevated;
    border-radius: $sim-radius-pill;
    overflow: hidden;
    margin: 2px 0;
  }
  .usage-fill {
    height: 100%;
    background: linear-gradient(90deg, $sim-color-rack, lighten($sim-color-rack, 12%));
    transition: width $sim-transition-normal;
  }
</style>
