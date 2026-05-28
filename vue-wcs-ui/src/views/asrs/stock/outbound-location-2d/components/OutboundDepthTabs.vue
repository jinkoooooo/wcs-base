<template>
  <div class="outbound-depth-tabs">
    <button
      v-for="depth in depths"
      :key="depth.depthNo"
      type="button"
      class="outbound-depth-tabs__button"
      :class="{ 'outbound-depth-tabs__button--active': modelValue === depth.depthNo }"
      @click="$emit('update:modelValue', depth.depthNo)"
    >
      D{{ depth.depthNo }}
    </button>
  </div>
</template>

<script setup lang="ts">
import type { OutboundLocation2DDepth } from '../types';

defineProps<{
  modelValue: number;
  depths: OutboundLocation2DDepth[];
}>();

defineEmits<{
  (e: 'update:modelValue', value: number): void;
}>();
</script>
