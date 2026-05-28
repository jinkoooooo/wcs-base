<template>
  <section v-if="lifecycle && lifecycle.length > 0" class="ph-panel">
    <div class="ph-header">
      <span class="ph-title">📜 파렛트 이력</span>
      <button
        v-if="lifecycle.length > MAX_INLINE"
        type="button"
        class="ph-more-btn"
        @click="$emit('open-history')"
      >
        +{{ lifecycle.length - MAX_INLINE }}건 더보기
      </button>
    </div>
    <ul class="ph-list">
      <li
        v-for="entry in inlineEntries"
        :key="entry.orderKey"
        class="ph-row"
        @click="$emit('open-history', entry.orderKey)"
      >
        <span :class="chipClassOf(entry)">{{ shortLabelOf(entry) }}</span>
        <span class="ph-loc">
          {{ entry.fromLocCode || '-' }} → {{ entry.toLocCode || '-' }}
        </span>
        <span class="ph-time">{{ timeOf(entry.createdAt) }}</span>
        <span class="ph-status">{{ statusLabel(entry.orderStatus) }}</span>
      </li>
    </ul>
  </section>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import {
    lifecycleEntryShortLabel,
    lifecycleEntryChipClass,
    formatLifecycleTime,
    type LifecycleEntry,
  } from './shared';
  import { WcsOrderStatusLabels } from '/@/views/tspg_4way/constants/wcsConsts';

  const props = defineProps<{ lifecycle: LifecycleEntry[] }>();
  defineEmits<{ (e: 'open-history', orderKey?: string): void }>();

  const MAX_INLINE = 5;

  const inlineEntries = computed(() => props.lifecycle.slice(0, MAX_INLINE));

  function shortLabelOf(e: LifecycleEntry) {
    return lifecycleEntryShortLabel(e, props.lifecycle);
  }
  function chipClassOf(e: LifecycleEntry) {
    return lifecycleEntryChipClass(e, props.lifecycle);
  }
  function timeOf(iso: string | null | undefined) {
    return formatLifecycleTime(iso, false);
  }
  function statusLabel(st: number) {
    return WcsOrderStatusLabels[st] ?? `상태 ${st}`;
  }
</script>

<style scoped>
  .ph-panel {
    flex-shrink: 0;
    margin-top: 8px;
    padding: 8px 10px 10px;
    background: #fff;
    border: 1px solid #e8eaed;
    border-radius: 8px;
    overflow: hidden;
  }
  .ph-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;
  }
  .ph-title {
    font-size: 12px;
    font-weight: 600;
    color: #475569;
  }
  .ph-more-btn {
    font-size: 11px;
    color: #3182f6;
    background: transparent;
    border: none;
    cursor: pointer;
    padding: 2px 4px;
  }
  .ph-more-btn:hover {
    text-decoration: underline;
  }
  .ph-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 3px;
  }
  .ph-row {
    display: grid;
    grid-template-columns: 64px 1fr 44px 60px;
    align-items: center;
    gap: 6px;
    padding: 4px 6px;
    border-radius: 4px;
    font-size: 11.5px;
    color: #0f172a;
    cursor: pointer;
    background: #fafbfc;
  }
  .ph-row:hover {
    background: #eef2f7;
  }
  .ph-loc {
    color: #475569;
    font-variant-numeric: tabular-nums;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .ph-time {
    color: #94a3b8;
    text-align: right;
    font-variant-numeric: tabular-nums;
  }
  .ph-status {
    color: #475569;
    text-align: right;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.chip-life) {
    display: inline-block;
    padding: 1px 6px;
    border-radius: 9999px;
    font-size: 10.5px;
    font-weight: 600;
    text-align: center;
    line-height: 1.4;
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
