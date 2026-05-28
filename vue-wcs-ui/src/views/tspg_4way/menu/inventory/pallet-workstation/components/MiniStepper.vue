<template>
  <div v-if="total > 0" class="mini-stepper">
    <template v-for="i in total" :key="i">
      <div class="step-item" :class="stepClass(i - 1)">
        <span class="step-num">{{ i }}</span>
        <span class="step-label">{{ tooltips[i - 1] ?? '' }}</span>
      </div>
      <span
        v-if="i < total"
        class="step-sep"
        :class="i - 1 < current ? 'step-sep-done' : ''"
      ></span>
    </template>
  </div>
</template>

<script lang="ts" setup>
const props = defineProps<{
  total: number;
  current: number;
  tooltips: string[];
}>();

function stepClass(idx: number): string {
  if (idx < props.current) return 'step-done';
  if (idx === props.current) return 'step-current';
  return 'step-todo';
}
</script>

<style scoped>
.mini-stepper {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  row-gap: 6px;
  padding: 8px 10px;
  background: var(--c-card, #ffffff);
  border: 1px solid var(--c-border, #e8eaed);
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  flex: 0 0 auto;
}
.step-item {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 3px 6px;
  border-radius: 6px;
  flex-shrink: 0;
  transition: all 0.2s;
}
.step-num {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 800;
  flex-shrink: 0;
  transition: all 0.2s;
}
.step-label {
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.step-todo .step-num {
  background: #e2e8f0;
  color: var(--c-muted, #94a3b8);
}
.step-todo .step-label {
  color: var(--c-muted, #94a3b8);
}
.step-current {
  background: #eff6ff;
  box-shadow: inset 0 0 0 1.5px var(--c-primary, #3182f6);
}
.step-current .step-num {
  background: var(--c-primary, #3182f6);
  color: #fff;
}
.step-current .step-label {
  color: var(--c-primary, #3182f6);
  font-weight: 700;
}
.step-done .step-num {
  background: var(--c-success, #16a34a);
  color: #fff;
}
.step-done .step-label {
  color: var(--c-success, #16a34a);
}
.step-sep {
  flex: 1;
  min-width: 6px;
  height: 1px;
  background: #e2e8f0;
  transition: background 0.25s;
}
.step-sep-done {
  background: var(--c-success, #16a34a);
}
</style>
