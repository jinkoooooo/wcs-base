<template>
  <span v-if="label" class="scenario-badge" :class="`tone-${tone}`">
    {{ label }}
  </span>
</template>

<script lang="ts" setup>
import type { ScenarioBadgeTone } from '../scenarios';

defineProps<{
  label: string;
  tone: ScenarioBadgeTone;
}>();
</script>

<style scoped>
.scenario-badge {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 9px;
  border-radius: 999px;
  letter-spacing: -0.2px;
  white-space: nowrap;
  flex-shrink: 0;
}
.scenario-badge.tone-idle {
  background: #f1f5f9;
  color: var(--c-muted, #94a3b8);
  border: 1px solid #e2e8f0;
}
.scenario-badge.tone-in {
  background: #dbeafe;
  color: #1e40af;
}
.scenario-badge.tone-out {
  background: #fef3c7;
  color: #92400e;
}
.scenario-badge.tone-sample {
  background: #fce7f3;
  color: #be185d;
}
.scenario-badge.tone-reinbound {
  background: #ede9fe;
  color: #5b21b6;
}
.scenario-badge.tone-success {
  background: #dcfce7;
  color: #15803d;
}
</style>
