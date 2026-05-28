<template>
  <div class="pending-orders">
    <div class="head">
      <span class="title">🔄 진행 중인 작업</span>
      <span class="count">총 {{ orders.length }}건</span>
    </div>

    <div class="table-wrap">
      <table class="tbl">
        <thead>
          <tr>
            <th>주문 번호</th>
            <th>종류</th>
            <th>상태</th>
            <th>전송 상태</th>
            <th>출발</th>
            <th>도착</th>
            <th>호스트 번호</th>
            <th>생성 시각</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="orders.length === 0">
            <td colspan="8" class="empty">진행 중인 작업이 없어요.</td>
          </tr>
          <tr v-for="o in orders" :key="o.order_key">
            <td class="key">{{ o.order_key }}</td>
            <td>
              <span class="chip" :class="typeClass(o.order_type)">
                {{ typeLabel(o.order_type) }}
              </span>
            </td>
            <td>
              <span class="chip" :class="statusClass(o.order_status)">
                {{ statusLabel(o.order_status) }}
              </span>
            </td>
            <td>
              <span class="chip" :class="ecsClass(o.ecs_if_status)">
                {{ ecsLabel(o.ecs_if_status) }}
              </span>
            </td>
            <td class="loc">{{ o.from_loc_code || '-' }}</td>
            <td class="loc">{{ o.to_loc_code || '-' }}</td>
            <td class="host">{{ o.host_order_key || '-' }}</td>
            <td class="ts">{{ formatRelative(o.created_at) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
  import type { InProgressOrder } from '../api';
  import { formatRelative, orderTypeLabel as typeLabel, orderTypeChipClass as typeClass } from '../utils';

  defineProps<{ orders: InProgressOrder[] }>();

  // ShuttleOrderStatus: 0 CREATED, 10 SENT, 20 ACCEPTED, 25 WAITING, 30 RUNNING, 40 ARRIVED,
  // 90 COMPLETED, 91 CANCELLED, 95 ABORTED, 100+ ERRORs
  function statusLabel(s: number) {
    switch (s) {
      case 0:  return '생성됨';
      case 10: return '전송';
      case 20: return '수락';
      case 25: return '대기';
      case 30: return '실행중';
      case 40: return '도착';
      case 90: return '완료';
      case 91: return '취소';
      case 95: return '중단';
      default: return s >= 100 ? `오류(${s})` : `상태 ${s}`;
    }
  }
  function statusClass(s: number) {
    if (s === 0)            return 'chip-status-created';
    if (s >= 10 && s < 30)  return 'chip-status-sent';
    if (s >= 30 && s < 90)  return 'chip-status-running';
    if (s === 90)           return 'chip-status-done';
    if (s >= 100)           return 'chip-status-error';
    return 'chip-status';
  }

  // EcsIfStatus: 0 READY, 10 SENDING, 20 SENT, 30 ACK, 99 FAIL
  function ecsLabel(code: number) {
    switch (code) {
      case 0:  return '대기';
      case 10: return '전송중';
      case 20: return 'SENT';
      case 30: return 'ACK';
      case 99: return '실패';
      default: return String(code);
    }
  }
  function ecsClass(code: number) {
    switch (code) {
      case 10: return 'chip-ecs-sending';
      case 20: return 'chip-ecs-sent';
      case 30: return 'chip-ecs-ack';
      case 99: return 'chip-ecs-fail';
      default: return 'chip-ecs-ready';
    }
  }
</script>

<style lang="scss" scoped>
  @use '../styles/sim-tokens' as *;

  .pending-orders {
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

  .table-wrap {
    overflow-x: auto;
  }

  .tbl {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
    font-variant-numeric: tabular-nums;

    thead th {
      text-align: left;
      color: $sim-text-muted;
      font-weight: 600;
      font-size: 12px;
      letter-spacing: 0.02em;
      padding: $sim-space-sm $sim-space-md;
      border-bottom: 1px solid $sim-border-strong;
      background: $sim-bg-elevated;
      white-space: nowrap;
    }

    tbody td {
      padding: $sim-space-sm $sim-space-md;
      border-bottom: 1px solid $sim-border-default;
      color: $sim-text-primary;
      white-space: nowrap;
    }

    // zebra
    tbody tr:nth-child(even) td {
      background: rgba(255, 255, 255, 0.02);
    }
    tbody tr:hover td {
      background: $sim-bg-card-hover;
    }

    .key  { font-weight: 600; }
    .loc  { color: $sim-color-host; }
    .host { color: $sim-text-muted; }
    .ts   { color: $sim-text-muted; font-size: 12px; }

    .empty {
      text-align: center;
      color: $sim-text-muted;
      padding: $sim-space-xl 0;
      background: transparent !important;
    }
  }

  .chip {
    display: inline-flex;
    align-items: center;
    height: 20px;
    padding: 0 10px;
    border-radius: $sim-radius-pill;
    font-size: 11px;
    font-weight: 600;
    border: 1px solid transparent;

    &.chip-inbound {
      background: rgba($sim-type-inbound, 0.18);
      color: $sim-type-inbound;
      border-color: rgba($sim-type-inbound, 0.4);
    }
    &.chip-outbound {
      background: rgba($sim-type-outbound, 0.18);
      color: $sim-type-outbound;
      border-color: rgba($sim-type-outbound, 0.4);
    }
    &.chip-move {
      background: rgba($sim-type-move, 0.18);
      color: $sim-type-move;
      border-color: rgba($sim-type-move, 0.4);
    }
    &.chip-other,
    &.chip-status,
    &.chip-status-created {
      background: rgba($sim-state-idle, 0.18);
      color: $sim-text-secondary;
      border-color: rgba($sim-state-idle, 0.4);
    }
    &.chip-status-sent {
      background: rgba($sim-state-warning, 0.18);
      color: $sim-state-warning;
      border-color: rgba($sim-state-warning, 0.4);
    }
    &.chip-status-running {
      background: rgba($sim-color-host, 0.18);
      color: $sim-color-host;
      border-color: rgba($sim-color-host, 0.4);
    }
    &.chip-status-done {
      background: rgba($sim-state-running, 0.18);
      color: $sim-state-running;
      border-color: rgba($sim-state-running, 0.4);
    }
    &.chip-status-error {
      background: rgba($sim-state-stopped, 0.18);
      color: $sim-state-stopped;
      border-color: rgba($sim-state-stopped, 0.4);
    }

    &.chip-ecs-ready {
      background: rgba($sim-state-idle, 0.18);
      color: $sim-text-muted;
      border-color: rgba($sim-state-idle, 0.4);
    }
    &.chip-ecs-sending {
      background: rgba($sim-color-host, 0.18);
      color: $sim-color-host;
      border-color: rgba($sim-color-host, 0.4);
    }
    &.chip-ecs-sent {
      background: rgba($sim-state-warning, 0.18);
      color: $sim-state-warning;
      border-color: rgba($sim-state-warning, 0.4);
    }
    &.chip-ecs-ack {
      background: rgba($sim-state-running, 0.18);
      color: $sim-state-running;
      border-color: rgba($sim-state-running, 0.4);
    }
    &.chip-ecs-fail {
      background: rgba($sim-state-stopped, 0.18);
      color: $sim-state-stopped;
      border-color: rgba($sim-state-stopped, 0.4);
    }
  }
</style>
