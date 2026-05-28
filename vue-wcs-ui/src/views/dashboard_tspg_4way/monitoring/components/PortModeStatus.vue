<template>
  <div class="port-mode-status">
    <div class="head">
      <span class="title">🚪 입출고 포트 현황</span>
      <span class="count">{{ ports.length }}개 포트</span>
    </div>

    <div v-if="ports.length === 0" class="empty">등록된 포트가 없어요.</div>

    <ul v-else class="port-list">
      <li
        v-for="p in ports"
        :key="p.loc_id"
        class="port-row"
        :title="tooltip(p)"
      >
        <span class="loc">포트 {{ p.loc_id }}</span>
        <span class="mode-pill" :class="modeClass(p.port_mode)">
          {{ modeLabel(p.port_mode) }}
        </span>
        <span class="status">
          <template v-if="p.task_id || (p.active_task_count ?? 0) > 0">
            <span class="dot dot-busy" />사용중
          </template>
          <template v-else-if="p.stock_id">
            <span class="dot dot-idle" />적재중
          </template>
          <template v-else>
            <span class="dot dot-empty" />대기
          </template>
        </span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
  import type { PortRow } from '../api';

  defineProps<{ ports: PortRow[] }>();

  function modeLabel(mode: string | null) {
    if (mode === 'INBOUND') return '입고';
    if (mode === 'OUTBOUND') return '출고';
    if (mode === 'OUTBOUND_PRIORITY') return '출고 우선';
    if (mode === 'IDLE') return '공용';
    return mode || '-';
  }

  function modeClass(mode: string | null) {
    if (mode === 'INBOUND') return 'mode-inbound';
    if (mode === 'OUTBOUND' || mode === 'OUTBOUND_PRIORITY') return 'mode-outbound';
    if (mode === 'IDLE') return 'mode-idle';
    return 'mode-unknown';
  }

  function tooltip(p: PortRow) {
    const parts = [
      `포트 ${p.loc_id}`,
      `모드: ${modeLabel(p.port_mode)}`,
      p.task_id ? `task=${p.task_id}` : null,
      p.stock_id ? `stock=${p.stock_id}` : null,
      p.active_task_count != null ? `active=${p.active_task_count}` : null,
    ].filter(Boolean);
    return parts.join(' · ');
  }
</script>

<style lang="scss" scoped>
  @use '../styles/sim-tokens' as *;

  .port-mode-status {
    @include sim-card;
    display: flex;
    flex-direction: column;
    gap: $sim-space-md;
  }

  .head {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    border-bottom: 1px solid $sim-border-default;
    padding-bottom: $sim-space-sm;

    .title {
      color: $sim-text-primary;
      font-weight: 600;
      font-size: 15px;
    }
    .count {
      color: $sim-text-muted;
      font-size: 13px;
      font-variant-numeric: tabular-nums;
    }
  }

  .empty {
    color: $sim-text-muted;
    font-size: 13px;
    text-align: center;
    padding: $sim-space-xl 0;
  }

  .port-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: $sim-space-sm;
    max-height: 320px;
    overflow-y: auto;
  }

  .port-row {
    display: grid;
    grid-template-columns: 1fr auto 80px;
    align-items: center;
    gap: $sim-space-md;
    padding: $sim-space-sm $sim-space-md;
    background: $sim-bg-elevated;
    border: 1px solid $sim-border-default;
    border-radius: $sim-radius-md;

    .loc {
      color: $sim-text-primary;
      font-weight: 600;
      font-size: 14px;
      font-variant-numeric: tabular-nums;
    }
  }

  .mode-pill {
    padding: 3px 12px;
    border-radius: $sim-radius-pill;
    font-size: 12px;
    font-weight: 600;
    border: 1px solid transparent;

    &.mode-inbound {
      background: rgba($sim-type-inbound, 0.18);
      color: $sim-type-inbound;
      border-color: rgba($sim-type-inbound, 0.4);
    }
    &.mode-outbound {
      background: rgba($sim-type-outbound, 0.18);
      color: $sim-type-outbound;
      border-color: rgba($sim-type-outbound, 0.4);
    }
    &.mode-idle {
      background: rgba($sim-state-idle, 0.18);
      color: $sim-text-muted;
      border-color: rgba($sim-state-idle, 0.4);
    }
    &.mode-unknown {
      background: $sim-bg-card;
      color: $sim-text-muted;
      border-color: $sim-border-default;
    }
  }

  .status {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: $sim-text-secondary;

    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      display: inline-block;
    }
    .dot-busy { background: $sim-state-running; box-shadow: 0 0 6px rgba($sim-state-running, 0.6); }
    .dot-idle { background: $sim-color-portmode; }
    .dot-empty { background: $sim-state-idle; }
  }
</style>
