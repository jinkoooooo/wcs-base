<template>
  <BaseModal
    :open="open"
    :busy="busy"
    :title="title"
    :confirm-label="confirmLabel"
    :width="560"
    modal-class="pfm-modal-shell"
    header-class="pfm-header-plain"
    confirm-class="pfm-confirm-danger"
    @confirm="$emit('confirm')"
    @close="$emit('close')"
  >
    <div
      v-for="g in groups"
      :key="g.key"
      class="pfm-group"
      :class="`pfm-tone-${g.tone}`"
    >
      <div class="pfm-group-head">
        <span class="pfm-group-title">{{ g.title }}</span>
        <span class="pfm-group-count">{{ g.items.length }}개</span>
      </div>
      <div v-if="g.summary" class="pfm-group-summary">{{ g.summary }}</div>
      <ul v-if="g.items.length > 0" class="pfm-list">
        <li
          v-for="(it, i) in g.items.slice(0, MAX_INLINE)"
          :key="`${g.key}-${i}`"
          class="pfm-item"
        >
          <span class="pfm-item-label">{{ it.label }}</span>
          <span v-if="it.sub" class="pfm-item-sub">{{ it.sub }}</span>
        </li>
        <li v-if="g.items.length > MAX_INLINE" class="pfm-item pfm-item-more">
          +{{ g.items.length - MAX_INLINE }} 개 더
        </li>
      </ul>
      <div v-else class="pfm-group-empty">대상 박스 없음</div>
    </div>
  </BaseModal>
</template>

<script lang="ts" setup>
import BaseModal from './BaseModal.vue';

export interface FinalizeGroup {
  key: string;
  title: string;
  tone: 'warn' | 'success' | 'muted' | 'primary';
  items: Array<{ label: string; sub?: string }>;
  summary?: string;
}

defineProps<{
  open: boolean;
  title: string;
  groups: FinalizeGroup[];
  confirmLabel: string;
  busy: boolean;
}>();

defineEmits<{
  (e: 'confirm'): void;
  (e: 'close'): void;
}>();

const MAX_INLINE = 10;
</script>

<style scoped>
.pfm-group {
  border: 1px solid #e8eaed;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
}
.pfm-group:last-child {
  margin-bottom: 0;
}
.pfm-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 700;
}
.pfm-group-title {
  letter-spacing: -0.2px;
}
.pfm-group-count {
  font-variant-numeric: tabular-nums;
}
.pfm-group-summary {
  padding: 4px 12px 8px;
  font-size: 11.5px;
  color: #475569;
}

.pfm-tone-warn {
  background: #fef2f2;
  border-color: #fca5a5;
}
.pfm-tone-warn .pfm-group-head {
  background: #fef2f2;
  color: #991b1b;
}
.pfm-tone-success {
  background: #f0fdf4;
  border-color: #86efac;
}
.pfm-tone-success .pfm-group-head {
  background: #f0fdf4;
  color: #166534;
}
.pfm-tone-primary {
  background: #eff6ff;
  border-color: #93c5fd;
}
.pfm-tone-primary .pfm-group-head {
  background: #eff6ff;
  color: #1e40af;
}
.pfm-tone-muted {
  background: #f8fafc;
}
.pfm-tone-muted .pfm-group-head {
  background: #f1f5f9;
  color: #475569;
}

.pfm-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 220px;
  overflow-y: auto;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}
.pfm-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  font-size: 11.5px;
  border-top: 1px solid #f1f5f9;
}
.pfm-item:first-child {
  border-top: 0;
}
.pfm-item-label {
  color: #0f172a;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}
.pfm-item-sub {
  color: #64748b;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}
.pfm-item-more {
  justify-content: center;
  color: #94a3b8;
  font-style: italic;
  font-size: 11px;
}
.pfm-group-empty {
  padding: 10px 12px;
  font-size: 11.5px;
  color: #94a3b8;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}
</style>
