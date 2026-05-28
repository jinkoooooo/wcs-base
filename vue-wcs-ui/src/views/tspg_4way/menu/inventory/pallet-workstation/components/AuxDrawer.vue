<template>
  <div class="aux-drawer">
    <button type="button" class="aux-toggle" :class="{ open }" @click="$emit('toggle')">
      <span class="aux-icon">{{ open ? '−' : '+' }}</span>
      <span class="aux-label">더보기</span>
      <span v-if="actions.length > 0" class="aux-count">{{ actions.length }}</span>
    </button>
    <div v-if="open && actions.length > 0" class="aux-body">
      <button
        v-for="aid in actions"
        :key="aid"
        type="button"
        class="aux-item"
        @click="$emit('run-action', aid)"
      >
        {{ labelOf(aid) }}
      </button>
    </div>
    <div v-else-if="open" class="aux-empty">
      현재 단계에서 가능한 보조 액션이 없습니다.
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { ActionId } from '../scenarios';

defineProps<{
  open: boolean;
  actions: ActionId[];
}>();

defineEmits<{
  (e: 'toggle'): void;
  (e: 'run-action', id: ActionId): void;
}>();

const LABELS: Partial<Record<ActionId, string>> = {
  saveDraft: '미저장 변경 저장',
  cancelDraft: '미저장 변경 취소',
  cancelReinbound: '재입고 예약 취소',
  printSelectedBoxes: '선택 박스 라벨 인쇄',
};

function labelOf(id: ActionId): string {
  return LABELS[id] ?? id;
}
</script>

<style scoped>
.aux-drawer {
  background: var(--c-card, #ffffff);
  border: 1px solid var(--c-border, #e8eaed);
  border-radius: 10px;
  overflow: hidden;
  flex: 0 0 auto;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}
.aux-toggle {
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
.aux-toggle:hover {
  background: #f8fafc;
}
.aux-toggle.open {
  border-bottom: 1px solid var(--c-border, #e8eaed);
}
.aux-icon {
  font-size: 16px;
  font-weight: 800;
  color: var(--c-muted, #94a3b8);
  width: 14px;
  text-align: center;
  line-height: 1;
}
.aux-label {
  flex: 1;
  text-align: left;
}
.aux-count {
  background: var(--c-primary, #3182f6);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 999px;
  min-width: 18px;
  text-align: center;
}
.aux-body {
  display: flex;
  flex-direction: column;
  padding: 6px;
}
.aux-item {
  background: transparent;
  border: 0;
  padding: 9px 10px;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text, #0f172a);
  text-align: left;
  cursor: pointer;
  border-radius: 6px;
}
.aux-item:hover {
  background: #f1f5f9;
}
.aux-empty {
  padding: 10px 14px;
  font-size: 12px;
  color: var(--c-muted, #94a3b8);
  border-top: 1px solid var(--c-border, #e8eaed);
}
</style>
