<template>
  <div v-if="descriptor" class="row-action" :class="`tone-${descriptor.tone ?? 'muted'}`">
    <button
      v-if="descriptor.actionId"
      type="button"
      class="row-action-btn"
      :class="`variant-${descriptor.variant}`"
      @click.stop="onClick"
    >
      {{ descriptor.label }}
    </button>
    <span v-else class="row-action-label" :class="`variant-${descriptor.variant}`">
      {{ descriptor.label }}
    </span>
  </div>
</template>

<script lang="ts" setup>
import type { RowActionDescriptor, ActionId } from '../../scenarios';

const props = defineProps<{
  descriptor: RowActionDescriptor | null;
  box: any;
}>();

const emit = defineEmits<{
  (e: 'run-row-action', payload: { actionId: ActionId; box: any }): void;
}>();

function onClick() {
  if (!props.descriptor?.actionId) return;
  emit('run-row-action', { actionId: props.descriptor.actionId, box: props.box });
}
</script>

<style scoped>
.row-action {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 72px;
}
.row-action-btn,
.row-action-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid transparent;
  white-space: nowrap;
  line-height: 1.3;
}
.row-action-btn {
  cursor: pointer;
  transition: all 0.15s;
}

/* tone-muted — 회색 (대기) */
.tone-muted .row-action-label,
.tone-muted .row-action-btn {
  background: #f1f5f9;
  color: var(--c-muted, #94a3b8);
  border-color: #e2e8f0;
}
.tone-muted .row-action-btn:hover {
  background: #e2e8f0;
}

/* tone-success — 녹색 (스캔됨/채취 완료) */
.tone-success .row-action-label,
.tone-success .row-action-btn {
  background: #dcfce7;
  color: #15803d;
  border-color: #bbf7d0;
}
.tone-success .row-action-btn:hover {
  background: #bbf7d0;
}

/* tone-warn — 노란 (부분/주의) */
.tone-warn .row-action-label,
.tone-warn .row-action-btn {
  background: #fef3c7;
  color: #92400e;
  border-color: #fde68a;
}
.tone-warn .row-action-btn:hover {
  background: #fde68a;
}

/* tone-primary — 파랑 (액션 가능) */
.tone-primary .row-action-label,
.tone-primary .row-action-btn {
  background: #dbeafe;
  color: #1e40af;
  border-color: #93c5fd;
}
.tone-primary .row-action-btn:hover {
  background: #bfdbfe;
}

/* variant overrides */
.variant-sample-input {
  font-size: 12px;
  padding: 5px 14px;
}
.variant-edit.row-action-label {
  font-style: normal;
}
</style>
