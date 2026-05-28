<template>
  <div
    class="outbound-rack-cell"
    :class="{
      'outbound-rack-cell--selected': selected,
      'outbound-rack-cell--occupied': occupiedCount > 0,
    }"
    :data-bay-no="cell.bayNo"
    :data-level-no="cell.levelNo"
  >
    <div class="outbound-rack-cell__depth outbound-rack-cell__depth--front">
      <span class="outbound-rack-cell__depth-label">D1</span>
      <span class="outbound-rack-cell__depth-value">{{ frontDepthState }}</span>
    </div>

    <div class="outbound-rack-cell__depth outbound-rack-cell__depth--rear">
      <span class="outbound-rack-cell__depth-label">D2</span>
      <span class="outbound-rack-cell__depth-value">{{ rearDepthState }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { OutboundLocation2DCell } from '../types';

const props = defineProps<{
  cell: OutboundLocation2DCell;
  selected?: boolean;
}>();

const depth1 = computed(() => props.cell.depths.find((d) => d.depthNo === 1));
const depth2 = computed(() => props.cell.depths.find((d) => d.depthNo === 2));

const frontDepthState = computed(() => (depth1.value?.occupied ? 'FULL' : 'EMPTY'));
const rearDepthState = computed(() => (depth2.value?.occupied ? 'FULL' : 'EMPTY'));

const occupiedCount = computed(() => props.cell.depths.filter((d) => d.occupied).length);
</script>
