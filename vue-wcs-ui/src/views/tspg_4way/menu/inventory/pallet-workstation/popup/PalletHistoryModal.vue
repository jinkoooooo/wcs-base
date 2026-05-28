<template>
  <BaseModal
    :open="open"
    :title="`파렛트 이력 (전체 ${entries.length}건)`"
    :width="880"
    :show-footer="false"
    header-class="phm-header"
    @close="$emit('close')"
  >
    <template #header-icon>
      <span class="phm-icon">📜</span>
    </template>
    <div class="phm-body">
      <table class="history-table">
        <thead>
          <tr>
            <th>시각</th>
            <th>타입</th>
            <th>From → To</th>
            <th>상태</th>
            <th>orderKey</th>
            <th>parent</th>
            <th>host</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="e in entries"
            :key="e.orderKey"
            :class="{ 'row-highlight': e.orderKey === highlightOrderKey }"
          >
            <td class="cell-time">{{ formatLifecycleTime(e.createdAt, true) }}</td>
            <td>
              <span :class="chipClassOf(e)">{{ shortLabelOf(e) }}</span>
              <span v-if="e.subOrderType && e.subOrderType !== 'NORMAL'" class="sub-text">
                / {{ e.subOrderType }}
              </span>
            </td>
            <td class="cell-loc">{{ e.fromLocCode || '-' }} → {{ e.toLocCode || '-' }}</td>
            <td>{{ statusLabel(e.orderStatus) }}</td>
            <td class="cell-key">{{ e.orderKey }}</td>
            <td class="cell-key">{{ e.parentOrderKey || '-' }}</td>
            <td class="cell-key">{{ e.hostOrderKey || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </BaseModal>
</template>

<script lang="ts" setup>
  import BaseModal from './BaseModal.vue';
  import {
    formatLifecycleTime,
    lifecycleEntryShortLabel,
    lifecycleEntryChipClass,
    type LifecycleEntry,
  } from '../shared';
  import { WcsOrderStatusLabels } from '/@/views/tspg_4way/constants/wcsConsts';

  const props = defineProps<{
    open: boolean;
    entries: LifecycleEntry[];
    highlightOrderKey?: string;
  }>();
  defineEmits<{ (e: 'close'): void }>();

  function shortLabelOf(e: LifecycleEntry) {
    return lifecycleEntryShortLabel(e, props.entries);
  }
  function chipClassOf(e: LifecycleEntry) {
    return lifecycleEntryChipClass(e, props.entries);
  }
  function statusLabel(st: number) {
    return WcsOrderStatusLabels[st] ?? `상태 ${st}`;
  }
</script>

<style scoped>
  .phm-icon {
    font-size: 18px;
  }
  .phm-body {
    padding: 0;
  }

  .history-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 12px;
  }
  .history-table thead th {
    text-align: left;
    padding: 6px 8px;
    background: #f6f7f9;
    color: #475569;
    font-weight: 600;
    border-bottom: 1px solid #e8eaed;
    position: sticky;
    top: 0;
  }
  .history-table tbody td {
    padding: 6px 8px;
    border-bottom: 1px solid #f1f5f9;
    color: #0f172a;
    vertical-align: middle;
  }
  .cell-time,
  .cell-loc,
  .cell-key {
    font-variant-numeric: tabular-nums;
  }
  .cell-key {
    font-family: ui-monospace, SFMono-Regular, monospace;
    font-size: 11.5px;
    color: #475569;
  }
  .sub-text {
    margin-left: 4px;
    color: #94a3b8;
    font-size: 11px;
  }
  .row-highlight {
    background: #fef3c7;
  }

  :deep(.chip-life) {
    display: inline-block;
    padding: 1px 6px;
    border-radius: 9999px;
    font-size: 10.5px;
    font-weight: 600;
  }
  :deep(.chip-life-inbound) {
    background: #dcfce7;
    color: #166534;
  }
  :deep(.chip-life-reinbound) {
    background: #bbf7d0;
    color: #14532d;
  }
  :deep(.chip-life-outbound) {
    background: #fed7aa;
    color: #9a3412;
  }
  :deep(.chip-life-sample) {
    background: #fce7f3;
    color: #9d174d;
  }
  :deep(.chip-life-discard) {
    background: #e5e7eb;
    color: #374151;
  }
  :deep(.chip-life-move) {
    background: #dbeafe;
    color: #1e40af;
  }
  :deep(.chip-life-cancel) {
    background: #fee2e2;
    color: #991b1b;
    border: 1px dashed #fca5a5;
  }
  :deep(.chip-life-err) {
    background: #ef4444;
    color: #fff;
  }
</style>
