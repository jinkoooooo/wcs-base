<template>
  <div
    class="outbound-rack-grid"
    :style="{
      gridTemplateColumns: `70px repeat(${maxBayNo}, minmax(84px, 84px))`,
      gridTemplateRows: `32px repeat(${maxLevelNo}, minmax(56px, 56px))`,
    }"
  >
    <div class="outbound-rack-grid__corner"></div>

    <div
      v-for="bay in maxBayNo"
      :key="`bay-${bay}`"
      class="outbound-rack-grid__header"
    >
      {{ bay }}
    </div>

    <template v-for="level in displayLevels" :key="`level-row-${level}`">
      <div class="outbound-rack-grid__side-label">{{ level }}</div>

      <template v-for="bay in maxBayNo" :key="`${bay}-${level}`">
        <OutboundRackCell
          v-if="findCell(bay, level)"
          :cell="findCell(bay, level)!"
          :selected="selectedCellKey === `${bay}-${level}`"
        />

        <div v-else class="outbound-rack-grid__empty-cell"></div>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import OutboundRackCell from './OutboundRackCell.vue';
import type { OutboundLocation2DCell } from '../types';

const props = defineProps<{
  cells: OutboundLocation2DCell[];
  maxBayNo: number;
  maxLevelNo: number;
  selectedCellKey?: string;
}>();

const displayLevels = computed(() => {
  const arr: number[] = [];
  for (let level = props.maxLevelNo; level >= 1; level -= 1) {
    arr.push(level);
  }
  return arr;
});

function findCell(bayNo: number, levelNo: number) {
  return props.cells.find((cell) => cell.bayNo === bayNo && cell.levelNo === levelNo);
}
</script>
