<!-- src/views/tspg-4way-shuttle/dashboard_2d/components/MiniMap.vue -->
<template>
  <div class="minimap" @mousedown.stop @click.stop>
    <div class="minimap-header">
      <span>MINI MAP</span>
      <span class="zoom">{{ Math.round(zoom * 100) }}%</span>
    </div>

    <svg
      class="minimap-svg"
      :width="miniW"
      :height="miniH"
      :viewBox="`0 0 ${miniW} ${miniH}`"
      @mousedown.prevent="onDown"
      @mousemove.prevent="onMove"
      @mouseup.prevent="onUp"
      @mouseleave.prevent="onUp"
      @click.prevent="onClick"
    >
      <!-- 전체 캔버스 -->
      <rect x="0" y="0" :width="miniW" :height="miniH" rx="8" class="mm-bg" />
      <rect x="1" y="1" :width="miniW - 2" :height="miniH - 2" rx="7" class="mm-border" />

      <!-- 현재 뷰포트 -->
      <rect
        v-if="vp"
        :x="vpRect.x"
        :y="vpRect.y"
        :width="vpRect.w"
        :height="vpRect.h"
        rx="4"
        class="mm-viewport"
      />
      <rect
        v-if="vp"
        :x="vpRect.x"
        :y="vpRect.y"
        :width="vpRect.w"
        :height="vpRect.h"
        rx="4"
        class="mm-viewport-outline"
      />
    </svg>

    <div class="minimap-hint">클릭/드래그로 이동</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { ViewportWorldRect } from '../../composables/useStageViewport';

const props = defineProps<{
  pageWidth: number;
  pageHeight: number;
  viewportRect: ViewportWorldRect | null;
  zoom: number;
  width?: number; // minimap width(px)
}>();

const emit = defineEmits<{
  (e: 'center', payload: { worldX: number; worldY: number }): void;
}>();

const miniW = computed(() => props.width ?? 210);

const miniH = computed(() => {
  const w = miniW.value;
  const pw = Math.max(1, props.pageWidth || 1);
  const ph = Math.max(1, props.pageHeight || 1);
  const h = Math.round((w * ph) / pw);
  // 너무 길면 보기 안 좋아서 상한
  return Math.min(Math.max(h, 110), 220);
});

const vp = computed(() => props.viewportRect);

const vpRect = computed(() => {
  const r = props.viewportRect;
  if (!r) return { x: 0, y: 0, w: 0, h: 0 };

  const pw = Math.max(1, props.pageWidth || 1);
  const ph = Math.max(1, props.pageHeight || 1);

  const x = (r.x / pw) * miniW.value;
  const y = (r.y / ph) * miniH.value;
  const w = (r.w / pw) * miniW.value;
  const h = (r.h / ph) * miniH.value;

  return { x, y, w, h };
});

const dragging = ref(false);

const svgPointToWorld = (evt: MouseEvent) => {
  const target = evt.currentTarget as SVGSVGElement;
  const rect = target.getBoundingClientRect();

  const px = evt.clientX - rect.left;
  const py = evt.clientY - rect.top;

  const rx = Math.min(Math.max(px / rect.width, 0), 1);
  const ry = Math.min(Math.max(py / rect.height, 0), 1);

  const worldX = rx * Math.max(1, props.pageWidth || 1);
  const worldY = ry * Math.max(1, props.pageHeight || 1);
  return { worldX, worldY };
};

const onClick = (e: MouseEvent) => {
  const { worldX, worldY } = svgPointToWorld(e);
  emit('center', { worldX, worldY });
};

const onDown = (e: MouseEvent) => {
  dragging.value = true;
  const { worldX, worldY } = svgPointToWorld(e);
  emit('center', { worldX, worldY });
};

const onMove = (e: MouseEvent) => {
  if (!dragging.value) return;
  const { worldX, worldY } = svgPointToWorld(e);
  emit('center', { worldX, worldY });
};

const onUp = () => {
  dragging.value = false;
};
</script>

<style scoped>
.minimap {
  position: absolute;
  right: 14px;
  bottom: 14px;
  z-index: 500;
  background: rgba(20, 22, 28, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 12px;
  padding: 10px 10px 8px;
  backdrop-filter: blur(6px);
  user-select: none;
}

.minimap-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: rgba(229, 234, 243, 0.85);
  margin-bottom: 8px;
  letter-spacing: 0.3px;
}

.zoom {
  color: rgba(64, 158, 255, 0.95);
  font-weight: 600;
}

.minimap-svg {
  display: block;
  cursor: crosshair;
}

.mm-bg {
  fill: rgba(255, 255, 255, 0.06);
}

.mm-border {
  fill: none;
  stroke: rgba(255, 255, 255, 0.12);
  stroke-width: 2;
}

.mm-viewport {
  fill: rgba(64, 158, 255, 0.18);
}

.mm-viewport-outline {
  fill: none;
  stroke: rgba(64, 158, 255, 0.7);
  stroke-width: 2;
  stroke-dasharray: 6 4;
}

.minimap-hint {
  margin-top: 6px;
  font-size: 10px;
  color: rgba(229, 234, 243, 0.55);
  text-align: center;
}
</style>
