<!--
  CellStateLegend.vue
  셀 상태 2D 화면 전용 범례 패널

  ============================================
  설계 원칙
  ============================================
  데이터 소스: legend-spec.ts (SSOT)
    - rackStatesBySubSection() 으로 재고 축을 3그룹 (기본/입출고/이상) 으로
    - RACK_FLAGS 로 금지 축 (LOCK/FORBID_IN/FORBID_OUT)

  두 축은 동시 성립 가능 (예: PRODUCT + 출고 금지).

  ============================================
  UX
  ============================================
  - 헤더 클릭: 접기/펼치기
  - 헤더 드래그: 컨테이너 내부 자유 이동 (clamp)
  - 내부 스크롤 지원
-->

<template>
  <div
    ref="panelRef"
    class="legend-panel"
    :class="{ collapsed: isCollapsed, dragging: isDragging }"
    :style="panelStyle"
    @click.stop
    @wheel.stop
    @mousedown.stop
    @contextmenu.stop
  >
    <div class="legend-header" @mousedown.left="startDrag" @click="handleHeaderClick">
      <div class="header-left">
        <span class="drag-handle" title="드래그하여 이동">⋮⋮</span>
        <span class="header-title">셀 상태</span>
        <span class="header-desc" v-if="!isCollapsed">우클릭으로 상태 변경</span>
      </div>
      <span class="toggle-icon">{{ isCollapsed ? '◀' : '▼' }}</span>
    </div>

    <div v-show="!isCollapsed" class="legend-body">
      <!-- ============================================ -->
      <!-- A. 재고 상태 — legend-spec.ts SSOT          -->
      <!-- ============================================ -->

      <div class="legend-axis-title">재고 상태</div>

      <div
        v-for="group in rackGroups"
        :key="group.subSection"
        class="legend-section"
      >
        <div class="legend-subtitle">{{ group.title }}</div>
        <div class="legend-grid">
          <div
            v-for="item in group.items"
            :key="item.code"
            class="legend-item"
          >
            <span
              class="cs-legend-color-chip"
              :style="{
                background: item.color,
                borderColor: item.border || item.color,
              }"
            ></span>
            <span class="legend-text">{{ item.label }}</span>
          </div>
        </div>
      </div>

      <!-- ============================================ -->
      <!-- B. 금지 상태 — RACK_FLAGS SSOT              -->
      <!-- ============================================ -->

      <div class="legend-divider"></div>
      <div class="legend-axis-title">금지 상태</div>
      <div class="legend-axis-hint">재고 상태와 동시에 적용 가능</div>

      <div class="legend-section">
        <div class="legend-grid">
          <div
            v-for="item in RACK_FLAGS"
            :key="item.code"
            class="legend-item legend-item-forbid"
          >
            <!-- 실제 셀과 동일한 스타일의 미니 프리뷰 -->
            <span
              class="cs-legend-forbid-preview"
              :class="previewClassFor(item)"
            >
              <span
                v-if="badgeClassFor(item)"
                class="cs-preview-forbid-badge"
                :class="badgeClassFor(item) || ''"
              >
                <span class="forbid-slash">⊘</span>
                <span class="forbid-text">{{ badgeTextFor(item) }}</span>
              </span>
            </span>
            <span class="legend-text">{{ item.label }}</span>
          </div>
        </div>
      </div>

      <!-- ============================================ -->
      <!-- C. 분류 / 제약 축 — 동적 (현재 페이지 사용중) -->
      <!-- ============================================ -->

      <div class="legend-divider"></div>
      <div class="legend-axis-title">분류 / 제약</div>
      <div class="legend-axis-hint">우상단 ★Type / 우측 색띠 Group</div>

      <div class="legend-section">
        <div v-if="usedItemGroups.length === 0" class="legend-empty-hint">
          이 페이지에 설정된 그룹 없음
        </div>
        <div v-else class="legend-grid">
          <div
            v-for="g in usedItemGroups"
            :key="g"
            class="legend-item"
          >
            <span
              class="cs-group-chip"
              :style="{ background: groupColor(g) }"
            ></span>
            <span class="legend-text">{{ g }}</span>
          </div>
        </div>
      </div>

      <div class="legend-section legend-section-tight">
        <div class="legend-mini-row">
          <span class="legend-mini-badge">★ABC</span>
          <span class="legend-text">item_type (우상단 배지)</span>
        </div>
        <div class="legend-mini-row">
          <span class="legend-mini-tip">🖱</span>
          <span class="legend-text">max_weight / max_height → 호버 툴팁</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import {
  RACK_FLAGS,
  rackStatesBySubSection,
  type LegendItem,
} from '@/views/kmat_2026/tspg-4way-shuttle/constants/legend/legend-spec';
import { groupColor } from '../utils/groupColor';

// ============================================
// Props — 부모로부터 분류 축 동적 데이터 받기
// ============================================
const props = withDefaults(
  defineProps<{
    /** 현재 페이지에서 실제 사용 중인 item_group 값들 (distinct, 정렬됨) */
    usedItemGroups?: string[];
  }>(),
  { usedItemGroups: () => [] },
);

const usedItemGroups = computed(() => props.usedItemGroups);

// ============================================
// A. 재고 축 — legend-spec.ts 가 단일 출처
// ============================================
const rackGroups = computed(() => rackStatesBySubSection());

// ============================================
// B. 금지 축 헬퍼 — RACK_FLAGS 의 pattern 으로 분기
// ============================================
function previewClassFor(item: LegendItem): string {
  if (item.pattern === 'locked') return 'cs-preview-locked';
  return '';
}

function badgeClassFor(item: LegendItem): 'badge-in' | 'badge-out' | null {
  if (item.pattern === 'forbid-in') return 'badge-in';
  if (item.pattern === 'forbid-out') return 'badge-out';
  return null;
}

function badgeTextFor(item: LegendItem): string {
  if (item.pattern === 'forbid-in') return 'IN';
  if (item.pattern === 'forbid-out') return 'OUT';
  return '';
}

// ============================================
// 드래그 이동 / 접기 (기존 로직 그대로)
// ============================================

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
  nextTick(() => clampPanelToContainer());
};

const handleResize = () => {
  nextTick(() => clampPanelToContainer());
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

<!--
  공통 CSS import — 색상/배지/LOCK 패턴을 메인 화면과 공유
  (unscoped 블록이라야 cs-legend-* / cs-preview-* 클래스가 그대로 매치됨)
-->
<style lang="scss">
@import '../styles/cell-state.scss';
</style>

<style scoped>
.legend-panel {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 600;
  width: 210px;
  height: 660px;       /* +100px — 분류 축 추가 (cap 마커는 툴팁이라 행 1줄 절약) */
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
  min-width: 140px;
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
  padding: 10px 12px 12px;
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

.legend-axis-title {
  font-size: 11px;
  font-weight: 700;
  color: #ffffff;
  margin: 2px 0 6px;
  padding: 4px 8px;
  background: rgba(64, 158, 255, 0.18);
  border-left: 2px solid #409eff;
  border-radius: 2px;
}

.legend-axis-hint {
  font-size: 9px;
  color: #8a8f99;
  margin: -4px 0 8px 10px;
  font-style: italic;
}

.legend-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  margin: 14px 0 10px;
}

.legend-section {
  margin-bottom: 10px;
}
.legend-section:last-child {
  margin-bottom: 0;
}

.legend-subtitle {
  font-size: 10px;
  font-weight: 600;
  color: #8fa2c7;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding-left: 4px;
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

.legend-item-forbid {
  min-height: 22px;
}

.legend-empty-hint {
  font-size: 10px;
  color: #707380;
  font-style: italic;
  padding: 2px 6px;
}

.legend-section-tight {
  margin-top: 4px;
}

.legend-mini-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 18px;
  margin-bottom: 4px;
}

.legend-mini-badge {
  font-family: 'Roboto Mono', Consolas, monospace;
  font-size: 9px;
  font-weight: 800;
  color: #ffffff;
  background: rgba(20, 22, 28, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.35);
  padding: 2px 5px;
  border-radius: 3px;
  flex: 0 0 auto;
}

.legend-mini-tip {
  font-size: 11px;
  color: #909399;
  width: 18px;
  text-align: center;
  flex: 0 0 18px;
}

.legend-text {
  min-width: 0;
  font-size: 11px;
  color: #c0c4cc;
  line-height: 1.3;
  word-break: keep-all;
}

/* ============================================ */
/* 반응형                                        */
/* ============================================ */

@media (max-height: 820px) {
  .legend-panel {
    top: 12px;
    width: 200px;
    height: 600px;     /* +100px — 분류/제약 축 추가 */
  }
  .header-title {
    font-size: 12px;
  }
  .legend-subtitle {
    margin-bottom: 5px;
  }
  .legend-grid {
    gap: 5px;
  }
  .legend-text {
    font-size: 10px;
  }
}
</style>
