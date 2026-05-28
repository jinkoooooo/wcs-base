<template>
  <div v-if="lifecycle.length > 0" class="hist-drawer">
    <button type="button" class="hist-toggle" :class="{ open }" @click="$emit('toggle')">
      <span class="hist-icon">{{ open ? '−' : '+' }}</span>
      <span class="hist-label">이력</span>
      <span class="hist-count">{{ lifecycle.length }}</span>
    </button>
    <div v-if="open" class="hist-body">
      <ul class="hist-list">
        <li
          v-for="entry in inlineEntries"
          :key="entry.orderKey"
          class="hist-row"
          @click="$emit('open-history', entry.orderKey)"
        >
          <span :class="chipClassOf(entry)">{{ shortLabelOf(entry) }}</span>
          <span class="hist-loc">
            {{ entry.fromLocCode || '-' }} → {{ entry.toLocCode || '-' }}
          </span>
          <span class="hist-time">{{ timeOf(entry.createdAt) }}</span>
          <span class="hist-status">{{ statusLabel(entry.orderStatus) }}</span>
        </li>
      </ul>
      <button
        v-if="lifecycle.length > MAX_INLINE"
        type="button"
        class="hist-more-btn"
        @click="$emit('open-history')"
      >
        +{{ lifecycle.length - MAX_INLINE }}건 더보기
      </button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import {
  lifecycleEntryShortLabel,
  lifecycleEntryChipClass,
  formatLifecycleTime,
  type LifecycleEntry,
} from '../shared';
import { WcsOrderStatusLabels } from '/@/views/tspg_4way/constants/wcsConsts';

const props = defineProps<{
  open: boolean;
  lifecycle: LifecycleEntry[];
}>();

defineEmits<{
  (e: 'toggle'): void;
  (e: 'open-history', orderKey?: string): void;
}>();

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
.hist-drawer {
  background: var(--c-card, #ffffff);
  border: 1px solid var(--c-border, #e8eaed);
  border-radius: 10px;
  overflow: hidden;
  flex: 0 0 auto;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}
.hist-toggle {
  width: 100%;
  background: transparent;
  border: 0;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-2, #475569);
  cursor: pointer;
  transition: background 0.15s;
}
.hist-toggle:hover {
  background: #f8fafc;
}
.hist-toggle.open {
  border-bottom: 1px solid var(--c-border, #e8eaed);
}
.hist-icon {
  font-size: 16px;
  font-weight: 800;
  color: var(--c-muted, #94a3b8);
  width: 14px;
  text-align: center;
  line-height: 1;
}
.hist-label {
  flex: 1;
  text-align: left;
}
.hist-count {
  background: #f1f5f9;
  color: var(--c-text-2, #475569);
  font-size: 10px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 999px;
  min-width: 18px;
  text-align: center;
  border: 1px solid #e2e8f0;
}
.hist-body {
  padding: 8px 10px 10px;
}
.hist-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.hist-row {
  display: grid;
  grid-template-columns: 64px 1fr 44px 60px;
  align-items: center;
  gap: 6px;
  padding: 4px 6px;
  border-radius: 4px;
  font-size: 11.5px;
  color: var(--c-text, #0f172a);
  cursor: pointer;
  background: #fafbfc;
}
.hist-row:hover {
  background: #eef2f7;
}
.hist-loc {
  color: var(--c-text-2, #475569);
  font-variant-numeric: tabular-nums;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hist-time {
  color: var(--c-muted, #94a3b8);
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.hist-status {
  color: var(--c-text-2, #475569);
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hist-more-btn {
  margin-top: 6px;
  width: 100%;
  background: transparent;
  border: 0;
  font-size: 11px;
  color: var(--c-primary, #3182f6);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}
.hist-more-btn:hover {
  background: #eff6ff;
}

/* Inherit chip styles from PalletHistoryPanel — reuse class names */
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
