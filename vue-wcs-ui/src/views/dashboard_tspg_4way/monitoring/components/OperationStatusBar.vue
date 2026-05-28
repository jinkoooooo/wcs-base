<!--
  운영 모드 + 핵심 플래그 표시 바.
  현재 WCS 가 어떤 모드로 운영 중인지 한눈에.
-->
<template>
  <div class="op-bar">
    <div class="left">
      <span class="label">운영모드</span>
      <span class="mode-pill" :class="modeClass">{{ modeLabel }}</span>
    </div>

    <div class="flags">
      <span class="flag" :class="flagClass(op?.isInspectionEnabled)">
        검수 <strong>{{ flagLabel(op?.isInspectionEnabled) }}</strong>
      </span>
      <span class="flag" :class="flagClass(op?.isPutbackEnabled)">
        재입고 <strong>{{ flagLabel(op?.isPutbackEnabled) }}</strong>
      </span>
      <span class="flag" :class="flagClass(op?.isDispatchLockEnabled)">
        디스패치락 <strong>{{ flagLabel(op?.isDispatchLockEnabled) }}</strong>
      </span>
      <span class="flag" :class="flagClass(op?.isOperationModeEnabled)">
        운영모드 적용 <strong>{{ flagLabel(op?.isOperationModeEnabled) }}</strong>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import type { OperationModeInfo } from '../api';

  const props = defineProps<{ op: OperationModeInfo | null }>();

  const modeLabel = computed(() => {
    switch (props.op?.mode) {
      case 'NORMAL':            return '정상 운영';
      case 'INBOUND_PRIORITY':  return '입고 우선';
      case 'OUTBOUND_PRIORITY': return '출고 우선';
      case 'RELOCATION':        return '재배치 모드';
      case 'MAINTENANCE':       return '점검 중';
      default:                  return props.op?.mode ?? '-';
    }
  });

  const modeClass = computed(() => {
    switch (props.op?.mode) {
      case 'NORMAL':            return 'mode-normal';
      case 'INBOUND_PRIORITY':  return 'mode-inbound';
      case 'OUTBOUND_PRIORITY': return 'mode-outbound';
      case 'RELOCATION':        return 'mode-relocation';
      case 'MAINTENANCE':       return 'mode-maintenance';
      default:                  return 'mode-normal';
    }
  });

  function flagLabel(v: boolean | undefined) {
    return v === true ? 'ON' : 'OFF';
  }
  function flagClass(v: boolean | undefined) {
    return v === true ? 'on' : 'off';
  }
</script>

<style lang="scss" scoped>
  @use '../styles/sim-tokens' as *;

  .op-bar {
    @include sim-card;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: $sim-space-lg;
    padding: $sim-space-md $sim-space-xl;
  }

  .left {
    display: flex;
    align-items: center;
    gap: $sim-space-md;
    .label {
      color: $sim-text-muted;
      font-size: 13px;
    }
  }

  .mode-pill {
    padding: 4px 16px;
    border-radius: $sim-radius-pill;
    font-size: 14px;
    font-weight: 700;
    border: 1px solid transparent;

    &.mode-normal      { background: rgba($sim-state-running, 0.18); color: $sim-state-running; border-color: rgba($sim-state-running, 0.4); }
    &.mode-inbound     { background: rgba($sim-type-inbound,  0.18); color: $sim-type-inbound;  border-color: rgba($sim-type-inbound,  0.4); }
    &.mode-outbound    { background: rgba($sim-type-outbound, 0.18); color: $sim-type-outbound; border-color: rgba($sim-type-outbound, 0.4); }
    &.mode-relocation  { background: rgba($sim-type-move,     0.18); color: $sim-type-move;     border-color: rgba($sim-type-move,     0.4); }
    &.mode-maintenance { background: rgba($sim-state-warning, 0.18); color: $sim-state-warning; border-color: rgba($sim-state-warning, 0.4); }
  }

  .flags {
    display: flex;
    gap: $sim-space-sm;
    flex-wrap: wrap;
  }

  .flag {
    font-size: 12px;
    padding: 4px 12px;
    border-radius: $sim-radius-pill;
    border: 1px solid transparent;
    color: $sim-text-secondary;

    strong {
      margin-left: 4px;
      font-weight: 700;
    }

    &.on  { background: rgba($sim-state-running, 0.12); color: $sim-state-running; border-color: rgba($sim-state-running, 0.35); }
    &.off { background: rgba($sim-state-idle,    0.12); color: $sim-text-muted;    border-color: rgba($sim-state-idle,    0.35); }
  }
</style>
