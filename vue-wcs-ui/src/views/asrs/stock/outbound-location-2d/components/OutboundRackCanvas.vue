<template>
  <section class="outbound-rack-canvas-panel">
    <div class="outbound-rack-canvas-panel__header">
      <div>
        <p class="outbound-rack-canvas-panel__eyebrow">RACK 2D VIEW</p>
        <h3 class="outbound-rack-canvas-panel__title">로케이션 2D 평면</h3>
      </div>

      <Outbound2DLegend />
    </div>

    <div class="outbound-rack-canvas-panel__body">
      <div
        ref="viewportRef"
        class="outbound-rack-canvas-panel__viewport"
        :class="{ 'outbound-rack-canvas-panel__viewport--dragging': dragging }"
        @wheel.prevent="handleWheel"
        @pointerdown="handlePointerDown"
      >
        <div
          class="outbound-rack-canvas-panel__transform"
          :style="transformStyle"
        >
          <OutboundRackGrid
            v-if="rackMap"
            :cells="rackMap.cells"
            :max-bay-no="rackMap.maxBayNo"
            :max-level-no="rackMap.maxLevelNo"
            :selected-cell-key="selectedCellKey"
          />

          <div v-else class="outbound-rack-canvas-panel__empty">
            Area / Aisle / Side를 선택하면 2D 맵이 표시됩니다.
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue';
import Outbound2DLegend from './Outbound2DLegend.vue';
import OutboundRackGrid from './OutboundRackGrid.vue';
import type {
  OutboundLocation2DCell,
  OutboundLocation2DMap,
  OutboundLocation2DViewState,
} from '../types';

const props = defineProps<{
  rackMap: OutboundLocation2DMap | null;
  selectedCell: OutboundLocation2DCell | null;
  viewState: OutboundLocation2DViewState;
}>();

const emit = defineEmits<{
  (e: 'select-cell', cell: OutboundLocation2DCell): void;
  (e: 'zoom-in'): void;
  (e: 'zoom-out'): void;
  (e: 'set-pan', payload: { offsetX: number; offsetY: number }): void;
}>();

const viewportRef = ref<HTMLElement | null>(null);
const dragging = ref(false);

let pointerStartX = 0;
let pointerStartY = 0;
let startOffsetX = 0;
let startOffsetY = 0;
let moved = false;

const DRAG_THRESHOLD = 6;

const selectedCellKey = computed(() => {
  if (!props.selectedCell) return '';
  return `${props.selectedCell.bayNo}-${props.selectedCell.levelNo}`;
});

const transformStyle = computed(() => ({
  transform: `translate(${props.viewState.offsetX}px, ${props.viewState.offsetY}px) scale(${props.viewState.zoom})`,
}));

function handleWheel(event: WheelEvent) {
  if (event.deltaY < 0) {
    emit('zoom-in');
  } else {
    emit('zoom-out');
  }
}

function handlePointerDown(event: PointerEvent) {
  if (event.button !== 0) return;

  event.preventDefault();

  dragging.value = true;
  moved = false;
  pointerStartX = event.clientX;
  pointerStartY = event.clientY;
  startOffsetX = props.viewState.offsetX;
  startOffsetY = props.viewState.offsetY;

  document.addEventListener('pointermove', handlePointerMove);
  document.addEventListener('pointerup', handlePointerUp);
}

function handlePointerMove(event: PointerEvent) {
  if (!dragging.value) return;

  const deltaX = event.clientX - pointerStartX;
  const deltaY = event.clientY - pointerStartY;

  if (Math.abs(deltaX) > DRAG_THRESHOLD || Math.abs(deltaY) > DRAG_THRESHOLD) {
    moved = true;
  }

  emit('set-pan', {
    offsetX: startOffsetX + deltaX,
    offsetY: startOffsetY + deltaY,
  });
}

function handlePointerUp(event: PointerEvent) {
  dragging.value = false;

  document.removeEventListener('pointermove', handlePointerMove);
  document.removeEventListener('pointerup', handlePointerUp);

  // drag가 아니면 클릭 처리
  if (!moved && props.rackMap) {
    const hitEl = document.elementFromPoint(event.clientX, event.clientY) as HTMLElement | null;
    const cellEl = hitEl?.closest('.outbound-rack-cell') as HTMLElement | null;

    if (!cellEl) return;

    const bayNo = Number(cellEl.dataset.bayNo || 0);
    const levelNo = Number(cellEl.dataset.levelNo || 0);

    if (!bayNo || !levelNo) return;

    const cell = props.rackMap.cells.find(
      (item) => item.bayNo === bayNo && item.levelNo === levelNo,
    );

    if (cell) {
      emit('select-cell', cell);
    }
  }
}

onBeforeUnmount(() => {
  document.removeEventListener('pointermove', handlePointerMove);
  document.removeEventListener('pointerup', handlePointerUp);
});
</script>
