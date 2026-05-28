<template>
  <div
    ref="panelRef"
    class="legend-panel"
    :class="{ collapsed: isCollapsed, dragging: isDragging }"
    :style="panelStyle"
    @click.stop
    @wheel.stop
  >
    <div class="legend-header" @mousedown.left="startDrag" @click="handleHeaderClick">
      <div class="header-left">
        <span class="drag-handle" title="드래그하여 이동">⋮⋮</span>
        <span class="header-title">범례</span>
        <span class="header-desc" v-if="!isCollapsed">실시간 상태 기준</span>
      </div>
      <span class="toggle-icon">{{ isCollapsed ? '◀' : '▼' }}</span>
    </div>

    <div v-show="!isCollapsed" class="legend-body">
      <div
        v-for="section in sections"
        :key="section.section"
        class="legend-section"
      >
        <div class="legend-subtitle">{{ section.title }}</div>
        <div class="legend-grid">
          <div
            v-for="item in section.items"
            :key="`${section.section}-${item.code}`"
            class="legend-item"
          >
            <span
              class="legend-color"
              :class="chipClass(item)"
              :style="chipStyle(item)"
            >
              <span v-if="item.pattern === 'forbid-in'" class="forbid-glyph">⊘</span>
              <span v-else-if="item.pattern === 'forbid-out'" class="forbid-glyph">⊘</span>
            </span>
            <span class="legend-text">{{ item.label }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { LEGEND_SECTIONS, type LegendItem } from '@/views/kmat_2026/tspg-4way-shuttle/constants/legend/legend-spec';

/**
 * DashboardLegend — legend-spec.ts 를 단일 소스로 사용.
 * 섹션/아이템 정의 변경은 legend-spec.ts 에서만. 이 컴포넌트는 렌더링만 담당.
 */

const sections = LEGEND_SECTIONS;

function chipClass(item: LegendItem): string[] {
  const classes: string[] = [];
  if (item.pattern) classes.push(`pattern-${item.pattern}`);
  if (item.cssClass) classes.push(`chip-${item.cssClass}`);
  return classes;
}

function chipStyle(item: LegendItem): Record<string, string> {
  const style: Record<string, string> = {};
  if (item.color) {
    // hatched 패턴은 색을 stripe 변수로 전달, 그 외는 배경으로 직접.
    if (item.pattern === 'hatched') {
      style['--chip-base'] = item.color;
    } else {
      style.backgroundColor = item.color;
    }
  }
  return style;
}

const panelRef = ref<HTMLElement | null>(null);
const isCollapsed = ref(true);

const isDragging = ref(false);
const dragStartX = ref(0);
const dragStartY = ref(0);
const panelX = ref<number | null>(null);
const panelY = ref<number | null>(null);
const wasDragged = ref(false);

const panelStyle = computed(() => {
  if (panelX.value === null || panelY.value === null) return {};
  return {
    position: 'absolute' as const,
    top: `${panelY.value}px`,
    left: `${panelX.value}px`,
    right: 'auto',
    bottom: 'auto',
  };
});

const handleHeaderClick = () => {
  if (wasDragged.value) {
    wasDragged.value = false;
    return;
  }
  isCollapsed.value = !isCollapsed.value;

  nextTick(() => {
    clampPanelToContainer();
  });
};

const handleResize = () => {
  nextTick(() => {
    clampPanelToContainer();
  });
};

watch(isCollapsed, async () => {
  await nextTick();
  clampPanelToContainer();
});

const getContainerEl = (): HTMLElement | null => {
  const panel = panelRef.value;
  if (!panel) return null;
  return panel.offsetParent as HTMLElement | null;
};

const startDrag = (e: MouseEvent) => {
  if (e.button !== 0) return;

  const target = e.target as HTMLElement;
  if (target.closest('button, a, input, textarea, select, .no-drag')) return;

  const panel = panelRef.value;
  const container = getContainerEl();
  if (!panel || !container) return;

  const panelRect = panel.getBoundingClientRect();
  const containerRect = container.getBoundingClientRect();

  isDragging.value = true;
  wasDragged.value = false;

  panelX.value = panelRect.left - containerRect.left;
  panelY.value = panelRect.top - containerRect.top;

  dragStartX.value = e.clientX - panelRect.left;
  dragStartY.value = e.clientY - panelRect.top;

  window.addEventListener('mousemove', onDrag);
  window.addEventListener('mouseup', stopDrag);

  e.preventDefault();
};

const onDrag = (e: MouseEvent) => {
  if (!isDragging.value) return;

  const panel = panelRef.value;
  const container = getContainerEl();
  if (!panel || !container) return;

  const containerRect = container.getBoundingClientRect();
  const panelWidth = panel.offsetWidth;
  const panelHeight = panel.offsetHeight;

  let newX = e.clientX - containerRect.left - dragStartX.value;
  let newY = e.clientY - containerRect.top - dragStartY.value;

  if (Math.abs(newX - (panelX.value ?? 0)) > 3 || Math.abs(newY - (panelY.value ?? 0)) > 3) {
    wasDragged.value = true;
  }

  const maxX = Math.max(0, container.clientWidth - panelWidth - 8);
  const maxY = Math.max(0, container.clientHeight - panelHeight - 8);

  newX = Math.max(8, Math.min(newX, maxX));
  newY = Math.max(8, Math.min(newY, maxY));

  panelX.value = newX;
  panelY.value = newY;
};

const stopDrag = () => {
  isDragging.value = false;
  window.removeEventListener('mousemove', onDrag);
  window.removeEventListener('mouseup', stopDrag);
};

const clampPanelToContainer = () => {
  const panel = panelRef.value;
  const container = getContainerEl();
  if (!panel || !container) return;
  if (panelX.value === null || panelY.value === null) return;

  const panelWidth = panel.offsetWidth;
  const panelHeight = panel.offsetHeight;

  const maxX = Math.max(0, container.clientWidth - panelWidth - 8);
  const maxY = Math.max(0, container.clientHeight - panelHeight - 8);

  panelX.value = Math.max(8, Math.min(panelX.value, maxX));
  panelY.value = Math.max(8, Math.min(panelY.value, maxY));
};

onMounted(() => {
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('mousemove', onDrag);
  window.removeEventListener('mouseup', stopDrag);
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
.legend-panel {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 600;
  width: 240px;
  height: 520px;
  background: rgba(20, 22, 28, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  box-shadow: 0 6px 28px rgba(0, 0, 0, 0.35);
  user-select: none;
  display: flex;
  flex-direction: column;
  transition: border-color 0.3s, box-shadow 0.2s;
  overflow: hidden;
  box-sizing: border-box;
  /* 캔버스(dashboard-body) 안에서만 표시되도록 부모 기준 max-height */
  max-width: calc(100% - 24px);
  max-height: calc(100% - 24px);
}

.legend-panel.dragging {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  cursor: grabbing;
}

.legend-panel.collapsed {
  height: auto;
  width: auto;
  min-width: 120px;
}

.legend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  cursor: grab;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
  user-select: none;
}

.legend-panel.dragging .legend-header {
  cursor: grabbing;
}

.legend-panel.collapsed .legend-header {
  border-bottom: none;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.drag-handle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  color: #606266;
  font-size: 10px;
  letter-spacing: -2px;
  cursor: grab;
  flex-shrink: 0;
}

.legend-panel.dragging .drag-handle {
  cursor: grabbing;
}

.header-title {
  font-size: 13px;
  font-weight: 700;
  color: #e5eaf3;
  line-height: 1.2;
}

.header-desc {
  font-size: 10px;
  color: #909399;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toggle-icon {
  font-size: 10px;
  color: #909399;
  flex-shrink: 0;
}

.legend-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 12px 12px 10px;
  min-height: 0;
}

.legend-body::-webkit-scrollbar {
  width: 8px;
}

.legend-body::-webkit-scrollbar-track {
  background: transparent;
}

.legend-body::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999px;
}

.legend-body::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.28);
}

.legend-section {
  margin-bottom: 12px;
}

.legend-section:last-child {
  margin-bottom: 0;
}

.legend-subtitle {
  font-size: 10px;
  font-weight: 700;
  color: #8fa2c7;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.6px;
}

.legend-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 18px;
}

.legend-color {
  position: relative;
  width: 16px;
  height: 16px;
  border-radius: 4px;
  flex: 0 0 16px;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.legend-text {
  min-width: 0;
  font-size: 11px;
  color: #c0c4cc;
  line-height: 1.3;
  word-break: keep-all;
}

/* DISABLED — 빗금 */
.legend-color.pattern-hatched {
  background:
    var(--chip-base, #4B5563)
    repeating-linear-gradient(
      -45deg,
      rgba(255, 255, 255, 0.25) 0,
      rgba(255, 255, 255, 0.25) 3px,
      transparent 3px,
      transparent 6px
    );
  border-color: rgba(255, 255, 255, 0.3);
}

/* Rack LOCK 프리뷰 — cell-state.scss 의 CSS 변수 재사용 */
.legend-color.pattern-locked {
  background:
    linear-gradient(var(--cs-locked-tint), var(--cs-locked-tint)),
    repeating-linear-gradient(
        45deg,
        transparent 0,
        transparent 3px,
        var(--cs-locked-stripe) 3px,
        var(--cs-locked-stripe) 5px
    );
  box-shadow: inset 0 0 0 1px var(--cs-locked-border);
}

/* Rack FORBID_IN / FORBID_OUT 배지 프리뷰 */
.legend-color.pattern-forbid-in {
  background: var(--cs-forbid-in-bg);
  color: var(--cs-forbid-in-text);
  font-size: 10px;
  font-weight: 800;
}
.legend-color.pattern-forbid-out {
  background: var(--cs-forbid-out-bg);
  color: var(--cs-forbid-out-text);
  font-size: 10px;
  font-weight: 800;
}
.forbid-glyph {
  line-height: 1;
}

/* Rack Type 주행전용 — 회색 빗금 + 점선 테두리 */
.legend-color.pattern-striped-gray {
  background:
    repeating-linear-gradient(
      -45deg,
      rgba(170, 176, 186, 0.85),
      rgba(170, 176, 186, 0.85) 3px,
      rgba(210, 216, 224, 0.75) 3px,
      rgba(210, 216, 224, 0.75) 6px
    );
  border: 1px dashed rgba(220, 226, 234, 0.9);
}

/* Rack Type 입출고포트 — 대각 2색 + 파랑 테두리 */
.legend-color.pattern-gradient-inout {
  background: linear-gradient(135deg, #67c23a 50%, #e6a23c 50%);
  border: 2px solid #409eff;
  box-shadow: 0 0 6px rgba(64, 158, 255, 0.35);
}

/* Rack Type 기존 badge-* 프리뷰 */
.legend-color.chip-badge-inbound {
  background-color: #67c23a;
  border: 2px solid rgba(103, 194, 58, 0.95);
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.35);
}
.legend-color.chip-badge-outbound {
  background-color: #e6a23c;
  border: 2px solid rgba(230, 162, 60, 0.95);
  box-shadow: 0 0 6px rgba(230, 162, 60, 0.35);
}
.legend-color.chip-badge-charge {
  background-color: #9b59b6;
  border: 2px solid rgba(155, 89, 182, 0.95);
  box-shadow: 0 0 6px rgba(155, 89, 182, 0.35);
}
.legend-color.chip-badge-inout {
  background: linear-gradient(135deg, #67c23a 50%, #e6a23c 50%);
  border: 2px solid #409eff;
  box-shadow: 0 0 6px rgba(64, 158, 255, 0.35);
}
.legend-color.chip-badge-charge-enter {
  background-color: #9b59b6;
  border: 2px dashed rgba(155, 89, 182, 0.9);
  box-shadow: 0 0 6px rgba(155, 89, 182, 0.35);
}

/* 반응형 */
@media (max-height: 820px) {
  .legend-panel {
    top: 12px;
    width: 224px;
    height: 460px;
  }
  .header-title {
    font-size: 12px;
  }
  .legend-subtitle {
    margin-bottom: 6px;
  }
  .legend-grid {
    gap: 5px;
  }
  .legend-text {
    font-size: 10px;
  }
}
</style>
