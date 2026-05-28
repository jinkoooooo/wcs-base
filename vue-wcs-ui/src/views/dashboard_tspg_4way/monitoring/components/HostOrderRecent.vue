<template>
  <div class="host-recent">
    <div class="head">
      <span class="title">🔍 최근 HOST 주문</span>
      <span class="count">최근 {{ orders.length }}건</span>
    </div>

    <div v-if="orders.length === 0" class="empty">아직 주문이 없어요.</div>

    <ul v-else class="order-list">
      <li v-for="o in orders" :key="o.host_order_key" class="order">
        <div class="row1">
          <span class="key">{{ o.host_order_key }}</span>
          <span class="chip" :class="typeClass(o.order_type)">
            {{ typeLabel(o.order_type) }}
          </span>
        </div>
        <div class="row2">
          <span class="status">{{ statusLabel(o.order_status) }}</span>
          <span class="dot" />
          <span class="inspect" :class="inspectClass(o)">
            {{ inspectLabel(o) }}
          </span>
          <span class="ts">{{ formatRelative(o.created_at) }}</span>
        </div>
        <div v-if="o.error_code" class="row3 error">
          ⚠ {{ o.error_code }}
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
  import type { HostOrderRow } from '../api';
  import { formatRelative, orderTypeLabel as typeLabel, orderTypeChipClass as typeClass } from '../utils';

  defineProps<{ orders: HostOrderRow[] }>();

  // HostOrderStatus: 0 RECEIVED, 5 WAITING_SCHEDULE, 7 INSPECTION_WAIT, 8 INSPECTION_FAILED,
  // 10 VALIDATED, 12 READY_FOR_ALLOC, 20 ALLOCATED, 30 WAITING_EXEC, 40 EXECUTING,
  // 60 PUTBACK_WAIT, 80 COMPLETED, 85 CANCELLED, 88 REJECTED, 100 ERROR
  function statusLabel(s: number) {
    switch (s) {
      case 0:   return '접수됨';
      case 5:   return '예약 대기';
      case 7:   return '검수 대기';
      case 8:   return '검수 실패';
      case 10:  return '검증 완료';
      case 12:  return '할당 대기';
      case 20:  return '할당 완료';
      case 30:  return '실행 대기';
      case 40:  return '실행 중';
      case 60:  return '반납 대기';
      case 80:  return '완료';
      case 85:  return '취소';
      case 88:  return '거부';
      case 100: return '오류';
      default:  return `상태 ${s}`;
    }
  }

  function inspectLabel(o: HostOrderRow) {
    if (o.inspection_required === false || o.inspection_required === null) return '검수 불요';
    switch (o.inspection_status) {
      case 'REQUESTED': return '검수 대기';
      case 'PASSED':    return '검수 통과';
      case 'FAILED':    return '검수 실패';
      default:          return '검수 필요';
    }
  }
  function inspectClass(o: HostOrderRow) {
    switch (o.inspection_status) {
      case 'PASSED':    return 'in-pass';
      case 'FAILED':    return 'in-fail';
      case 'REQUESTED': return 'in-wait';
      default:          return 'in-skip';
    }
  }
</script>

<style lang="scss" scoped>
  @use '../styles/sim-tokens' as *;

  .host-recent {
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

    .title { color: $sim-text-primary; font-weight: 600; font-size: 15px; }
    .count { color: $sim-text-muted; font-size: 13px; }
  }

  .empty {
    color: $sim-text-muted;
    font-size: 13px;
    text-align: center;
    padding: $sim-space-xl 0;
  }

  .order-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: $sim-space-sm;
    max-height: 320px;
    overflow-y: auto;
  }

  .order {
    background: $sim-bg-elevated;
    border: 1px solid $sim-border-default;
    border-radius: $sim-radius-md;
    padding: $sim-space-sm $sim-space-md;
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 13px;

    .row1 {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: $sim-space-sm;

      .key {
        color: $sim-text-primary;
        font-weight: 600;
        font-variant-numeric: tabular-nums;
      }
    }

    .row2 {
      display: flex;
      align-items: center;
      gap: 8px;
      color: $sim-text-secondary;
      font-size: 12px;

      .status { color: $sim-text-secondary; }
      .dot {
        width: 3px; height: 3px; border-radius: 50%;
        background: $sim-text-muted;
      }
      .ts { margin-left: auto; color: $sim-text-muted; }
    }

    .row3.error {
      color: $sim-state-stopped;
      font-size: 12px;
    }
  }

  .chip {
    padding: 2px 10px;
    border-radius: $sim-radius-pill;
    font-size: 11px;
    font-weight: 600;
    border: 1px solid transparent;

    &.chip-inbound  { background: rgba($sim-type-inbound, 0.18);  color: $sim-type-inbound;  border-color: rgba($sim-type-inbound, 0.4); }
    &.chip-outbound { background: rgba($sim-type-outbound, 0.18); color: $sim-type-outbound; border-color: rgba($sim-type-outbound, 0.4); }
    &.chip-move     { background: rgba($sim-type-move, 0.18);     color: $sim-type-move;     border-color: rgba($sim-type-move, 0.4); }
    &.chip-other    { background: rgba($sim-state-idle, 0.18);    color: $sim-text-muted;    border-color: rgba($sim-state-idle, 0.4); }
  }

  .inspect {
    font-size: 11px;
    font-weight: 600;
    padding: 1px 8px;
    border-radius: $sim-radius-pill;
    border: 1px solid transparent;

    &.in-pass { background: rgba($sim-state-running, 0.15); color: $sim-state-running; border-color: rgba($sim-state-running, 0.4); }
    &.in-fail { background: rgba($sim-state-stopped, 0.15); color: $sim-state-stopped; border-color: rgba($sim-state-stopped, 0.4); }
    &.in-wait { background: rgba($sim-state-warning, 0.15); color: $sim-state-warning; border-color: rgba($sim-state-warning, 0.4); }
    &.in-skip { background: rgba($sim-state-idle, 0.15);    color: $sim-text-muted;    border-color: rgba($sim-state-idle, 0.4); }
  }
</style>
