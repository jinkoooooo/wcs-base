<template>
  <div
    class="center-panel"
    ref="containerRef"
    @drop="handleDrop"
    @dragover.prevent
    @dragenter.prevent
  >
    <EditorCanvasToolbar
      v-model:canvasWidth="localCanvasWidth"
      v-model:canvasHeight="localCanvasHeight"
      v-model:backgroundColor="localBackgroundColor"
      v-model:showGrid="showGrid"
      v-model:snapEnabled="snapEnabled"
      v-model:smartGuides="smartGuides"
      :scale="canvasScale"
      :hasSelection="selectedIds.length > 0"
      @apply-canvas-size="updateCanvasSize"
      @fit="fitToView"
      @reset="resetView"
      @center-selection="centerSelectionToCanvas"
    />

    <div
      class="canvas-wrapper"
      ref="wrapperRef"
      tabindex="0"
      @wheel.prevent="onWheel"
      @mousedown="rackCellEditMode ? onRackCellMouseDown($event) : handleCanvasMouseDown($event)"
      @mousemove="rackCellEditMode ? onRackCellMouseMove($event) : handleCanvasMouseMove($event)"
      @mouseup="rackCellEditMode ? onRackCellMouseUp() : handleCanvasMouseUp($event)"
      @contextmenu.prevent
      @keydown="handleKeyDown"
    >
      <div class="canvas" :class="{ 'rack-cell-edit': rackCellEditMode }" :style="canvasStyle" @click="rackCellEditMode ? null : handleCanvasClick()">
        <svg v-if="showGrid" class="grid-overlay" :width="canvasWidth" :height="canvasHeight">
          <defs>
            <pattern id="grid" :width="gridSize" :height="gridSize" patternUnits="userSpaceOnUse">
              <path
                :d="`M ${gridSize} 0 L 0 0 0 ${gridSize}`"
                fill="none"
                stroke="#e0e0e0"
                stroke-width="1"
              />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#grid)" />
        </svg>

        <div
          v-for="obj in objects"
          :key="obj.id"
          class="canvas-object"
          :class="{ selected: isSelected(obj.id!), locked: obj.isLocked }"
          :data-type="obj.equipmentTypeCode || ''"
          :style="getObjectStyle(obj)"
          @mousedown.stop="handleObjectMouseDown($event, obj)"
          @click.stop="handleObjectClick($event, obj)"
        >
          <div class="object-content">
            <img
              v-if="getObjectIcon(obj)"
              :src="getObjectIcon(obj)"
              :alt="obj.equipmentCode"
              class="object-image"
              draggable="false"
            />
            <div v-else class="object-placeholder">
              {{ obj.equipmentTypeCode?.charAt(0) || '?' }}
            </div>
          </div>

          <div v-if="obj.showLabel" class="object-label">
            {{ obj.customLabel || obj.equipmentCode }}
          </div>

          <template v-if="isSelected(obj.id!) && !obj.isLocked">
            <div class="resize-handle nw"
                 :style="{ cursor: getHandleCursor('nw', obj.rotation || 0) }"
                 @mousedown.stop="startResize($event, obj, 'nw')"></div>
            <div class="resize-handle ne"
                 :style="{ cursor: getHandleCursor('ne', obj.rotation || 0) }"
                 @mousedown.stop="startResize($event, obj, 'ne')"></div>
            <div class="resize-handle sw"
                 :style="{ cursor: getHandleCursor('sw', obj.rotation || 0) }"
                 @mousedown.stop="startResize($event, obj, 'sw')"></div>
            <div class="resize-handle se"
                 :style="{ cursor: getHandleCursor('se', obj.rotation || 0) }"
                 @mousedown.stop="startResize($event, obj, 'se')"></div>

            <div class="resize-handle n"
                 :style="{ cursor: getHandleCursor('n', obj.rotation || 0) }"
                 @mousedown.stop="startResize($event, obj, 'n')"></div>
            <div class="resize-handle s"
                 :style="{ cursor: getHandleCursor('s', obj.rotation || 0) }"
                 @mousedown.stop="startResize($event, obj, 's')"></div>
            <div class="resize-handle w"
                 :style="{ cursor: getHandleCursor('w', obj.rotation || 0) }"
                 @mousedown.stop="startResize($event, obj, 'w')"></div>
            <div class="resize-handle e"
                 :style="{ cursor: getHandleCursor('e', obj.rotation || 0) }"
                 @mousedown.stop="startResize($event, obj, 'e')"></div>

            <div class="rotate-handle" @mousedown.stop="startRotate($event, obj)"></div>
          </template>
        </div>

        <div v-if="marquee.active" class="marquee" :style="marqueeStyle"></div>

        <!-- 랙 셀 편집 모드 인라인 레이어 -->
        <template v-if="rackCellEditMode">
          <div
            v-for="obj in rackCellObjects"
            :key="`rce-${obj.id}`"
            :style="cellOverlayStyle(obj)"
            :title="cellStateTitle(realEqIdOf(obj))"
            class="rack-cell-overlay-cell"
          ></div>

          <div
            v-for="m in bayMarkers"
            :key="`bay-${m.bay}`"
            class="rack-edit-bay-label"
            :style="{
              left: `${m.worldX}px`,
              bottom: `${rackBoundingBox.maxY + labelOffset}px`,
              width: `${labelSize}px`,
              height: `${labelSize}px`,
              transform: 'translateX(-50%)',
              fontSize: `${labelFontSize}px`,
            }"
          >{{ m.bay }}</div>

          <div
            v-for="m in rowMarkers"
            :key="`row-${m.row}`"
            class="rack-edit-row-label"
            :style="{
              left: `${rackBoundingBox.minX - labelOffset}px`,
              bottom: `${m.worldY}px`,
              width: `${labelSize}px`,
              height: `${labelSize}px`,
              transform: 'translate(-100%, 50%)',
              fontSize: `${labelFontSize}px`,
            }"
          >{{ m.row }}</div>

          <div v-if="cellDragRectStyle" :style="cellDragRectStyle"></div>
        </template>

        <div
          v-if="snapEnabled && smartGuides && guideLineX != null"
          class="guide-line-x"
          :style="{ left: guideLineX + 'px' }"
        ></div>
        <div
          v-if="snapEnabled && smartGuides && guideLineY != null"
          class="guide-line-y"
          :style="{ bottom: guideLineY + 'px' }"
        ></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
  import type { TbEcs2dItem, TbEcs2dItemType, TbEcs2dPage } from '../api/types';
  import { resolveTypeIconUrl } from '../assets/iconUrl';
  import { buildUUID } from '/@/utils/uuid';

  import EditorCanvasToolbar from './components/EditorCanvasToolbar.vue';

  import { useCanvasViewport } from '../composables/useCanvasViewport';
  import { useCanvasSelection } from '../composables/useCanvasSelection';

  import { useEditorClipboard } from './composables/useEditorClipboard';
  import { useEditorInteractions } from './composables/useEditorInteractions';
  import { useEditorShortcuts } from './composables/useEditorShortcuts';
  import type { CellDraft } from './composables/useRackCellDraft';
  import { useRackCellSelection } from './composables/useRackCellSelection';

  const props = defineProps<{
    page: TbEcs2dPage | undefined;
    objects: TbEcs2dItem[];
    selectedObjectId: string | null;
    equipmentTypeMap: Map<string, TbEcs2dItemType>;
    rackCellEditMode?: boolean;
    rackCellsDraft?: Record<string, CellDraft>;
    rackCellRejectedIds?: string[];
    rackCellRejectedReasons?: Record<string, string>;
  }>();

  // 셀 편집 드래그 선택 (이 컴포넌트 소유 — prop으로 넘기면 ref auto-unwrap 문제)
  const cellSelection = useRackCellSelection();

  const emit = defineEmits<{
    (e: 'add-object', obj: TbEcs2dItem): void;
    (e: 'update-object', id: string, updates: Partial<TbEcs2dItem>): void;
    (e: 'select-object', id: string | null): void;
    (e: 'selection-changed', payload: { ids: string[]; primaryId: string | null }): void;
    (e: 'update-canvas', width: number, height: number, backgroundColor: string): void;
    (e: 'delete-objects', ids: string[]): void;
    (e: 'save'): void;
    (e: 'undo'): void;
    (e: 'redo'): void;
    (e: 'rack-cell-drag-end', cellIds: string[]): void;
  }>();

  const containerRef = ref<HTMLElement | null>(null);
  const wrapperRef = ref<HTMLElement | null>(null);

  // =========================
  // Canvas Config (local state)
  // =========================
  const localCanvasWidth = ref(1920);
  const localCanvasHeight = ref(1080);
  const localBackgroundColor = ref('#FFFFFF');
  const showGrid = ref(true);
  const gridSize = ref(50);

  const snapEnabled = ref(true);
  const smartGuides = ref(true);

  const canvasWidth = computed(() => localCanvasWidth.value);
  const canvasHeight = computed(() => localCanvasHeight.value);

  // Viewport
  const viewport = useCanvasViewport({ wrapperRef, canvasWidth, canvasHeight });
  const canvasScale = viewport.scale;

  // =========================
  // ✅ 초기 진입 Fit 버그 해결 (1층 처음 진입 시 레이아웃 안 뜨는 문제)
  // - wrapperRef가 null/0px인 타이밍에도 재시도
  // - "fit 성공" 후에만 fittedPageId 세팅
  // - 페이지가 바뀌는 중엔 이전 페이지로 fitted 처리하지 않도록 방어
  // =========================
  const fittedPageId = ref<string | null>(null);

  // (선택) LC 바뀌면 다시 Fit 허용
  watch(
    () => props.page?.lcId,
    () => {
      fittedPageId.value = null;
    },
    { immediate: true },
  );

  const fitToViewOncePerPage = async () => {
    const pid = props.page?.id;
    if (!pid) return;

    if (fittedPageId.value === pid) return;

    await nextTick();

    const tryFit = (tries = 30) => {
      // 페이지가 이미 바뀌었으면 중단
      if (props.page?.id !== pid) return;

      const el = wrapperRef.value;

      // ref가 아직 안 잡힌 경우도 재시도
      if (!el) {
        if (tries <= 0) {
          viewport.fitToView();
          if (props.page?.id === pid) fittedPageId.value = pid;
          return;
        }
        requestAnimationFrame(() => tryFit(tries - 1));
        return;
      }

      const rect = el.getBoundingClientRect();

      if (rect.width > 0 && rect.height > 0) {
        viewport.fitToView();
        if (props.page?.id === pid) fittedPageId.value = pid; // ✅ 성공 후에만 기록
        return;
      }

      if (tries <= 0) {
        viewport.fitToView();
        if (props.page?.id === pid) fittedPageId.value = pid;
        return;
      }

      requestAnimationFrame(() => tryFit(tries - 1));
    };

    // 레이아웃이 한 프레임 더 늦게 잡히는 케이스 방지 (2프레임)
    requestAnimationFrame(() => requestAnimationFrame(() => tryFit()));
  };

  watch(
    () => props.page?.id,
    () => {
      fitToViewOncePerPage();
    },
    { immediate: true },
  );

  watch(
    () => wrapperRef.value,
    (el) => {
      if (el) fitToViewOncePerPage();
    },
    { immediate: true },
  );

  onMounted(() => {
    fitToViewOncePerPage();
  });

  const canvasStyle = computed(() => ({
    ...viewport.canvasTransformStyle.value,
    width: `${canvasWidth.value}px`,
    height: `${canvasHeight.value}px`,
    backgroundColor: localBackgroundColor.value,
  }));

  // =========================
  // Selection
  // =========================
  const { selectedIds, primaryId, isSelected, clear, selectSingle, toggle, setMany } =
    useCanvasSelection();

  watch(
    [selectedIds, primaryId],
    () => {
      emit('selection-changed', {
        ids: [...selectedIds.value],
        primaryId: primaryId.value ?? null,
      });
    },
    { immediate: true },
  );

  // store의 단일 선택 동기화
  watch(
    () => props.selectedObjectId,
    (id) => {
      if (!id) {
        clear();
        return;
      }
      if (primaryId.value !== id) selectSingle(id);
    },
    { immediate: true },
  );

  // 페이지 설정 동기화
  watch(
    () => props.page,
    (page) => {
      if (!page) return;
      localCanvasWidth.value = page.canvasWidth || 1920;
      localCanvasHeight.value = page.canvasHeight || 1080;
      localBackgroundColor.value = page.backgroundColor || '#FFFFFF';
      showGrid.value = page.showGrid ?? true;
      gridSize.value = page.gridSize || 50;

      // 캔버스 크기/배경 바뀐 직후에도 한 번 Fit (원치 않으면 제거 가능)
      // fittedPageId.value = null;
      // fitToViewOncePerPage();
    },
    { immediate: true },
  );

  // =========================
  // UI helpers
  // =========================
  const getObjectStyle = (obj: TbEcs2dItem) => {
    const scaleX = (obj.scaleX || 1) * (obj.flipH ? -1 : 1);
    const scaleY = (obj.scaleY || 1) * (obj.flipV ? -1 : 1);

    return {
      position: 'absolute' as const,
      left: `${obj.posX}px`,
      bottom: `${obj.posY}px`,
      width: `${obj.width}px`,
      height: `${obj.height}px`,
      transform: `rotate(${obj.rotation || 0}deg) scale(${scaleX}, ${scaleY})`,
      transformOrigin: 'center center', // 'left bottom'에서 변경!
      zIndex: obj.zIndex || 0,
      opacity: obj.opacity ?? 1,
    };
  };

  // =========================
  // Rack Cell Edit Mode — 인라인 레이어
  // =========================

  function realEqIdOf(obj: TbEcs2dItem): string {
    return String((obj as any).realEqId || '');
  }

  function parseCellId(cellId: string | undefined | null): { level: number; row: number; bay: number } | null {
    if (!cellId || !/^\d{5,6}$/.test(cellId)) return null;
    const lenLevel = cellId.length === 5 ? 1 : 2;
    const level = parseInt(cellId.slice(0, lenLevel), 10);
    const row = parseInt(cellId.slice(lenLevel, lenLevel + 2), 10);
    const bay = parseInt(cellId.slice(lenLevel + 2), 10);
    return { level, row, bay };
  }

  /** 현재 페이지에서 RACK 셀 단위 2D 아이템들 (realEqId = cellId) */
  const rackCellObjects = computed(() => {
    if (!props.rackCellEditMode) return [] as TbEcs2dItem[];
    return props.objects.filter(
      (o) =>
        (o.equipmentTypeCode === 'RACK' || (o as any).realEqType === 'RACK') &&
        !!(o as any).realEqId &&
        parseCellId((o as any).realEqId) != null,
    );
  });

  /** 셀 ID → 2D 아이템 매핑 (한 번만 빌드) */
  const cellItemByCellId = computed(() => {
    const m = new Map<string, TbEcs2dItem>();
    for (const o of rackCellObjects.value) {
      m.set(String((o as any).realEqId), o);
    }
    return m;
  });

  /** 셀 상태 색상 */
  function cellStateColor(cellId: string): string {
    if (props.rackCellRejectedIds?.includes(cellId)) return 'rgba(180,0,0,0.5)';
    const c = props.rackCellsDraft?.[cellId];
    if (!c) return 'transparent';
    switch (c.state) {
      case 'active': return 'rgba(40,180,80,0.15)';
      case 'disabled': return 'rgba(120,120,120,0.55)';
      case 'pending-disable': return 'rgba(220,40,40,0.55)';
      case 'pending-enable': return 'rgba(40,180,80,0.55)';
      case 'pending-create': return 'rgba(40,120,220,0.55)';
    }
    return 'transparent';
  }

  function cellStateTitle(cellId: string): string {
    const reason = props.rackCellRejectedReasons?.[cellId];
    if (reason) return `${cellId} — 거절: ${reason}`;
    const c = props.rackCellsDraft?.[cellId];
    if (!c) return cellId;
    return `${cellId} (${c.row}, ${c.bay}) — ${c.state}`;
  }

  /** 랙 바운딩 박스 (라벨 위치 기준) */
  const rackBoundingBox = computed(() => {
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    for (const r of rackCellObjects.value) {
      const x = Number(r.posX || 0), y = Number(r.posY || 0);
      const w = Number(r.width || 50), h = Number(r.height || 50);
      if (x < minX) minX = x;
      if (y < minY) minY = y;
      if (x + w > maxX) maxX = x + w;
      if (y + h > maxY) maxY = y + h;
    }
    if (minX === Infinity) return { minX: 0, minY: 0, maxX: 0, maxY: 0 };
    return { minX, minY, maxX, maxY };
  });

  /** BAY → 같은 BAY 셀들의 중심 X 평균 */
  const bayMarkers = computed(() => {
    const acc = new Map<number, { sum: number; count: number }>();
    for (const r of rackCellObjects.value) {
      const parsed = parseCellId((r as any).realEqId);
      if (!parsed) continue;
      const w = Number(r.width || 50);
      const cx = Number(r.posX || 0) + w / 2;
      const cur = acc.get(parsed.bay);
      if (cur) { cur.sum += cx; cur.count += 1; }
      else acc.set(parsed.bay, { sum: cx, count: 1 });
    }
    return Array.from(acc.entries()).sort((a, b) => a[0] - b[0])
      .map(([bay, { sum, count }]) => ({ bay, worldX: sum / count }));
  });

  /** ROW → 같은 ROW 셀들의 중심 Y 평균 (bottom 좌표계) */
  const rowMarkers = computed(() => {
    const acc = new Map<number, { sum: number; count: number }>();
    for (const r of rackCellObjects.value) {
      const parsed = parseCellId((r as any).realEqId);
      if (!parsed) continue;
      const h = Number(r.height || 50);
      const cy = Number(r.posY || 0) + h / 2;
      const cur = acc.get(parsed.row);
      if (cur) { cur.sum += cy; cur.count += 1; }
      else acc.set(parsed.row, { sum: cy, count: 1 });
    }
    return Array.from(acc.entries()).sort((a, b) => a[0] - b[0])
      .map(([row, { sum, count }]) => ({ row, worldY: sum / count }));
  });

  /** 라벨 크기 — 평균 셀 크기 기준 */
  const rackAvgCell = computed(() => {
    const cells = rackCellObjects.value;
    if (cells.length === 0) return { width: 50, height: 50 };
    let tw = 0, th = 0;
    for (const c of cells) { tw += Number(c.width || 50); th += Number(c.height || 50); }
    return { width: tw / cells.length, height: th / cells.length };
  });
  const labelSize = computed(() => Math.max(Math.min(rackAvgCell.value.width, rackAvgCell.value.height) * 0.85, 30));
  const labelFontSize = computed(() => Math.max(labelSize.value * 0.6, 11));
  const labelOffset = computed(() => Math.max(rackAvgCell.value.width * 0.04, 0.4));

  /** 셀 사각형 오버레이 스타일 — bottom 좌표계 */
  function cellOverlayStyle(obj: TbEcs2dItem) {
    return {
      position: 'absolute' as const,
      left: `${obj.posX}px`,
      bottom: `${obj.posY}px`,
      width: `${obj.width}px`,
      height: `${obj.height}px`,
      background: cellStateColor(realEqIdOf(obj)),
      border: '1px solid rgba(0,0,0,0.35)',
      zIndex: 9000,
      pointerEvents: 'none' as const,
      boxSizing: 'border-box' as const,
    };
  }

  /** 드래그 사각형 화면 표시 (bottom 좌표계로 변환) */
  const cellDragRectStyle = computed(() => {
    if (!cellSelection.isDragging.value) return null;
    const r = cellSelection.dragRect;
    const bottom = canvasHeight.value - (r.y + r.h);
    return {
      position: 'absolute' as const,
      left: `${r.x}px`,
      bottom: `${bottom}px`,
      width: `${r.w}px`,
      height: `${r.h}px`,
      background: 'rgba(0,120,255,0.15)',
      border: '1px dashed #0078ff',
      zIndex: 9100,
      pointerEvents: 'none' as const,
    };
  });

  // 드래그 좌표는 top-기준 world 좌표계 사용 (사각형 충돌 판정용)
  function getWorldXYTopFromEvent(e: MouseEvent): { x: number; yTop: number } | null {
    const r = viewport.toCanvasXY(e.clientX, e.clientY);
    return { x: r.x, yTop: canvasHeight.value - r.yBottom };
  }

  function onRackCellMouseDown(e: MouseEvent) {
    if (!props.rackCellEditMode) return;
    if (e.button !== 0) return;
    const p = getWorldXYTopFromEvent(e);
    if (!p) return;
    cellSelection.start(p.x, p.yTop);
    window.addEventListener('mousemove', onRackCellWindowMouseMove);
    window.addEventListener('mouseup', onRackCellWindowMouseUp);
    e.preventDefault();
    e.stopPropagation();
  }

  function onRackCellWindowMouseMove(e: MouseEvent) {
    onRackCellMouseMove(e);
  }
  function onRackCellWindowMouseUp() {
    onRackCellMouseUp();
    window.removeEventListener('mousemove', onRackCellWindowMouseMove);
    window.removeEventListener('mouseup', onRackCellWindowMouseUp);
  }

  function onRackCellMouseMove(e: MouseEvent) {
    if (!props.rackCellEditMode) return;
    if (!cellSelection.isDragging.value) return;
    const p = getWorldXYTopFromEvent(e);
    if (!p) return;
    cellSelection.update(p.x, p.yTop);
  }

  function onRackCellMouseUp() {
    if (!props.rackCellEditMode) return;
    if (!cellSelection.isDragging.value) return;
    cellSelection.isDragging.value = false;
    const r = cellSelection.dragRect;
    // 작은 드래그면 클릭으로 간주 — 포인트 교차 판정 (한 셀)
    const isClick = r.w < 3 && r.h < 3;
    const x1 = r.x, x2 = isClick ? r.x + 1 : r.x + r.w;
    const yT1 = r.y, yT2 = isClick ? r.y + 1 : r.y + r.h;
    const hits: string[] = [];
    for (const obj of rackCellObjects.value) {
      const cellId = realEqIdOf(obj);
      const ox = Number(obj.posX || 0);
      const oy = Number(obj.posY || 0); // bottom-y
      const ow = Number(obj.width || 50);
      const oh = Number(obj.height || 50);
      const oyTop = canvasHeight.value - (oy + oh);
      const oyTopEnd = canvasHeight.value - oy;
      const intersects = ox < x2 && ox + ow > x1 && oyTop < yT2 && oyTopEnd > yT1;
      if (intersects) hits.push(cellId);
    }
    if (hits.length > 0) emit('rack-cell-drag-end', hits);
  }

  // 회전 각도를 고려한 핸들 커서 계산
  // 핸들의 시각적 위치(화면상 위치)에 맞는 커서를 반환
  const getHandleCursor = (
    dir: 'nw' | 'ne' | 'sw' | 'se' | 'n' | 's' | 'e' | 'w',
    rotation: number,
  ): string => {
    // 90도 단위로 정규화
    const norm = ((Math.round(rotation / 90) * 90) % 360 + 360) % 360;

    // 시계 방향 90도 회전 시: 핸들의 시각적 방향 매핑
    // 객체 로컬 N → 화면상 E 위치로 이동 → e-resize 커서
    const cw90: Record<string, string> = {
      n: 'e', e: 's', s: 'w', w: 'n',
      ne: 'se', se: 'sw', sw: 'nw', nw: 'ne',
    };

    let mapped = dir as string;
    const steps = norm / 90;
    for (let i = 0; i < steps; i++) {
      mapped = cw90[mapped] || mapped;
    }
    return `${mapped}-resize`;
  };

  const getObjectIcon = (obj: TbEcs2dItem) => {
    const typeCode = obj.equipmentTypeCode || '';
    const type = props.equipmentTypeMap.get(typeCode);
    if (!type) return '';
    if (type.iconData2d && type.iconData2d.trim() !== '') return type.iconData2d;
    return resolveTypeIconUrl(type.typeCode, type.iconFileName);
  };

  // ============================================================
  // 아이콘 비율로 기본 width/height를 계산
  // ============================================================
  const iconRawMap = import.meta.glob('./icons/*.svg', { as: 'raw', eager: true }) as Record<
    string,
    string
  >;

  const getTypeIconRawSvg = (typeCode: string, iconFileName?: string) => {
    const file = iconFileName && iconFileName.trim() !== '' ? iconFileName : `${typeCode}.svg`;
    return iconRawMap[`./icons/${file}`] || '';
  };

  const extractSvgViewBoxRatio = (svgText: string): number | null => {
    if (!svgText) return null;

    const vb = svgText.match(/viewBox\s*=\s*"([\d.\-]+)\s+([\d.\-]+)\s+([\d.\-]+)\s+([\d.\-]+)"/i);
    if (vb) {
      const w = parseFloat(vb[3]);
      const h = parseFloat(vb[4]);
      if (Number.isFinite(w) && Number.isFinite(h) && w > 0 && h > 0) return w / h;
    }

    const wMatch = svgText.match(/\bwidth\s*=\s*"([\d.]+)"/i);
    const hMatch = svgText.match(/\bheight\s*=\s*"([\d.]+)"/i);
    if (wMatch && hMatch) {
      const w = parseFloat(wMatch[1]);
      const h = parseFloat(hMatch[1]);
      if (Number.isFinite(w) && Number.isFinite(h) && w > 0 && h > 0) return w / h;
    }

    return null;
  };

  const tryGetSvgTextFromIconData2d = (iconData2d: string): string | null => {
    const s = (iconData2d || '').trim();
    if (!s) return null;

    if (s.startsWith('<svg')) return s;

    if (s.startsWith('data:image/svg+xml')) {
      const comma = s.indexOf(',');
      if (comma < 0) return null;
      const meta = s.slice(0, comma);
      const data = s.slice(comma + 1);

      if (/;base64/i.test(meta)) {
        try {
          const decoded = atob(data);
          return decoded;
        } catch {
          return null;
        }
      }

      try {
        return decodeURIComponent(data);
      } catch {
        return data;
      }
    }

    return null;
  };

  const loadImageNaturalSize = (src: string): Promise<{ w: number; h: number } | null> => {
    return new Promise((resolve) => {
      const img = new Image();
      img.onload = () => {
        const w = img.naturalWidth;
        const h = img.naturalHeight;
        if (w > 0 && h > 0) resolve({ w, h });
        else resolve(null);
      };
      img.onerror = () => resolve(null);
      img.src = src;
    });
  };

  const computeDefaultSizeByType = async (type: TbEcs2dItemType) => {
    if ((type.defaultWidth ?? 0) > 0 && (type.defaultHeight ?? 0) > 0) {
      return { width: type.defaultWidth!, height: type.defaultHeight! };
    }

    const u = gridSize.value || 50;

    // ✅ 타입별 “그리드 기준 고정 크기” 우선 적용
    switch (type.typeCode) {
      case 'RACK':
        return { width: u, height: u };
      case 'LIFTER':
        return { width: u, height: u };
      case 'BCR':
        return { width: Math.round(u * 0.8), height: Math.round(u * 0.8) };
      case 'CONVEYOR':
        return { width: u, height: u }; // rack과 동일한 1:1 기본 크기
    }

    // 그 외 타입은 기존처럼 ratio 기반
    const baseW = u;

    const iconData2d = (type.iconData2d || '').trim();
    if (iconData2d) {
      const svgText = tryGetSvgTextFromIconData2d(iconData2d);
      if (svgText) {
        const ratio = extractSvgViewBoxRatio(svgText);
        if (ratio) return { width: baseW, height: Math.max(10, Math.round(baseW / ratio)) };
      }
      const natural = await loadImageNaturalSize(iconData2d);
      if (natural) {
        const ratio = natural.w / natural.h;
        return { width: baseW, height: Math.max(10, Math.round(baseW / ratio)) };
      }
    }

    const rawSvg = getTypeIconRawSvg(type.typeCode, type.iconFileName);
    const ratioFromFile = extractSvgViewBoxRatio(rawSvg);
    if (ratioFromFile)
      return { width: baseW, height: Math.max(10, Math.round(baseW / ratioFromFile)) };

    return { width: baseW, height: baseW };
  };

  const safeParseDragData = (dt: DataTransfer) => {
    const candidates = [
      dt.getData('application/json'),
      dt.getData('text/plain'),
      dt.getData('text'),
      dt.getData('application/x-editor-dnd'),
    ].filter(Boolean);

    for (const raw of candidates) {
      try {
        const parsed = JSON.parse(raw);
        if (parsed && typeof parsed === 'object') return parsed;
      } catch {
        // ignore
      }
    }
    return null;
  };

  const snapXY = (x: number, y: number) => {
    if (!snapEnabled.value) return { x, y };
    const u = gridSize.value || 50;
    return {
      x: Math.round(x / u) * u,
      y: Math.round(y / u) * u,
    };
  };

  const handleDrop = async (event: DragEvent) => {
    event.preventDefault();
    const dt = event.dataTransfer;
    if (!dt) return;

    const dragData = safeParseDragData(dt);
    if (!dragData) {
      console.warn('[DND] dragData parse failed', {
        types: Array.from(dt.types),
        json: dt.getData('application/json'),
        plain: dt.getData('text/plain'),
      });
      return;
    }

    const raw = viewport.toCanvasXY(event.clientX, event.clientY);
    const snapped = snapXY(raw.x, raw.yBottom);

    const x = snapped.x;
    const y = snapped.y;

    if (dragData.dragType === 'equipmentType') {
      const type = dragData.data as TbEcs2dItemType;
      const size = await computeDefaultSizeByType(type);

      const newObject: TbEcs2dItem = {
        id: buildUUID(),
        lcId: props.page?.lcId || '',
        pageId: props.page?.id || '',
        equipmentCode: `${type.typeCode}_${Date.now()}`,
        equipmentTypeCode: type.typeCode,
        posX: x,
        posY: y,
        width: size.width,
        height: size.height,
        rotation: 0,
        scaleX: 1,
        scaleY: 1,
        flipH: false,
        flipV: false,
        zIndex: props.objects.length,
        opacity: 1,
        showLabel: true,
        isVisible: true,
        isLocked: false,
      };

      emit('add-object', newObject);
      selectSingle(newObject.id!);
      emit('select-object', newObject.id!);
    }

    // equipmentMaster drop도 동일하게 확장 가능
  };

  // =========================
  // Clipboard (copy/paste)
  // =========================
  const { copySelection, pasteSelection } = useEditorClipboard({
    page: computed(() => props.page),
    objects: computed(() => props.objects),
    selectedIds,
    clear,
    setMany,
    emitAdd: (obj) => emit('add-object', obj),
    emitSelect: (id) => emit('select-object', id),
    canvasWidth,
    canvasHeight,
  });

  // =========================
  // Interactions
  // =========================
  const {
    marquee,
    marqueeStyle,
    guideLineX,
    guideLineY,
    suppressCanvasClick,
    handleObjectClick,
    handleObjectMouseDown,
    handleCanvasMouseDown: startMarquee,
    handleCanvasMouseMove,
    handleCanvasMouseUp,
    startResize,
    startRotate,
    centerSelectionToCanvas,
    handlePanStart, // ✅ 추가됨
  } = useEditorInteractions({
    wrapperRef,
    viewport,
    objects: computed(() => props.objects),
    canvasWidth,
    canvasHeight,
    showGrid,
    gridSize,
    snapEnabled,
    smartGuides,
    canvasScale,
    selectedIds,
    primaryId,
    isSelected,
    clear,
    selectSingle,
    toggle,
    setMany,
    emitUpdate: (id, updates) => emit('update-object', id, updates),
    emitSelect: (id) => emit('select-object', id),
  });

  // 캔버스 클릭(선택 해제)
  const handleCanvasClick = () => {
    if (marquee.value.active) return;
    if (suppressCanvasClick.value) return;
    clear();
    emit('select-object', null);
  };

  // =========================
  // Toolbar actions
  // =========================
  const updateCanvasSize = () => {
    emit(
      'update-canvas',
      localCanvasWidth.value,
      localCanvasHeight.value,
      localBackgroundColor.value,
    );
  };

  const fitToView = () => viewport.fitToView();
  const resetView = () => viewport.resetView();

  // =========================
  // Wheel zoom
  // =========================
  const onWheel = (e: WheelEvent) => {
    // 1. 공통적으로 브라우저 기본 스크롤 방지 (중요!)
    e.preventDefault();

    if (e.ctrlKey || e.metaKey) {
      // 2. Ctrl 또는 Cmd를 누른 상태에서는 확대/축소
      // 휠을 올리면(deltaY < 0) 확대, 내리면 축소
      const factor = e.deltaY < 0 ? 1.1 : 0.9;
      const newScale = viewport.scale.value * factor;

      // 마우스 커서 위치를 기준으로 확대/축소 실행
      viewport.zoomAt(e.clientX, e.clientY, newScale);
    }
  };

  const handleNudge = (dx: number, dy: number) => {
    if (selectedIds.value.length === 0) return;

    for (const id of selectedIds.value) {
      const o = props.objects.find((x) => x.id === id);
      if (!o || o.isLocked) continue;

      const rad = (o.rotation || 0) * (Math.PI / 180);
      const cos = Math.cos(rad);
      const sin = Math.sin(rad);

      const cx = (o.width || 0) / 2;
      const cy = (o.height || 0) / 2;

      const pts = [
        { x: 0, y: 0 },
        { x: o.width || 0, y: 0 },
        { x: o.width || 0, y: o.height || 0 },
        { x: 0, y: o.height || 0 },
      ].map((p) => {
        const tx = p.x - cx;
        const ty = p.y - cy;
        return { x: tx * cos + ty * sin + cx, y: -tx * sin + ty * cos + cy };
      });

      const minX = Math.min(...pts.map((p) => p.x));
      const maxX = Math.max(...pts.map((p) => p.x));
      const minY = Math.min(...pts.map((p) => p.y));
      const maxY = Math.max(...pts.map((p) => p.y));

      emit('update-object', id, {
        posX: Math.min(canvasWidth.value - maxX, Math.max(-minX, o.posX + dx)),
        posY: Math.min(canvasHeight.value - maxY, Math.max(-minY, o.posY + dy)),
      });
    }
  };

  // (유지) Space key 추적
  // ✅ [적용] 중앙화된 단축키 관리자 연결
  const { spaceDown } = useEditorShortcuts({
    onSave: () => emit('save'),
    onUndo: () => emit('undo'),
    onRedo: () => emit('redo'),
    onCopy: () => copySelection(),
    onPaste: () => pasteSelection(),
    onNudge: (dx, dy) => handleNudge(dx, dy),
    onDelete: () => {
      if (selectedIds.value.length === 0) return;
      const deletableIds = selectedIds.value.filter((id) => {
        const o = props.objects.find((x) => x.id === id);
        return o && !o.isLocked;
      });
      if (deletableIds.length > 0) emit('delete-objects', deletableIds);
      clear();
      emit('select-object', null);
    },
  });

  const handleCanvasMouseDown = (e: MouseEvent) => {
    // ✅ 1. 우클릭(2) 또는 휠 클릭(1) 또는 Space를 누른 상태면 화면 이동 모드 시작
    if (e.button === 2 || e.button === 1 || spaceDown.value) {
      handlePanStart(e);
      return;
    }

    // ✅ 2. 그 외의 경우(일반 좌클릭) 기존 마퀴 드래그 시작
    startMarquee(e);
  };

  onMounted(() => {
    window.addEventListener('mouseup', handleCanvasMouseUp);
    window.addEventListener('mousemove', handleCanvasMouseMove);
  });

  onUnmounted(() => {
    window.removeEventListener('mouseup', handleCanvasMouseUp);
    window.removeEventListener('mousemove', handleCanvasMouseMove);
  });

  defineExpose({ centerSelectionToCanvas });
</script>

<style scoped>
  .center-panel {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    height: 100%;
    background-color: #f0f2f5;
    overflow: hidden;
  }

  .canvas-wrapper {
    flex: 1;
    overflow: hidden;
    position: relative;
    user-select: none;
    background-color: #f0f2f5;
  }

  .canvas {
    position: absolute;
    top: 0;
    left: 0;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    border: 1px solid #dcdfe6;
  }

  /* 랙 셀 편집 모드 — 객체 클릭/드래그 비활성화하고 wrapper 단의 셀 드래그가 받게 함 */
  .canvas.rack-cell-edit .canvas-object {
    pointer-events: none;
  }
  .canvas.rack-cell-edit {
    cursor: crosshair;
  }
  .rack-edit-bay-label,
  .rack-edit-row-label {
    position: absolute;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #333;
    font-weight: 600;
    pointer-events: none;
    z-index: 9200;
  }
  .rack-cell-overlay-cell {
    /* 인라인 style 로 색상/위치 지정. 추가 규칙 없음 */
  }

  .grid-overlay {
    position: absolute;
    top: 0;
    left: 0;
    pointer-events: none;
  }

  .canvas-object {
    position: absolute;
    cursor: move;
    border: 2px solid transparent;
    box-sizing: border-box;
    transition: border-color 0.2s;
  }

  .canvas-object:hover {
    border-color: rgba(64, 158, 255, 0.5);
  }

  .canvas-object.selected {
    border-color: #409eff;
    box-shadow: 0 0 8px rgba(64, 158, 255, 0.4);
  }

  .canvas-object.locked {
    cursor: not-allowed;
    opacity: 0.7;
  }

  .object-content {
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
  }

  .object-image {
    width: 100%;
    height: 100%;
    object-fit: fill;
    display: block;
    pointer-events: none;
  }

  .object-placeholder {
    font-size: 24px;
    font-weight: bold;
    color: #909399;
    background-color: rgba(144, 147, 153, 0.1);
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .object-label {
    position: absolute;
    bottom: -20px;
    left: 50%;
    transform: translateX(-50%);
    font-size: 11px;
    color: #303133;
    background-color: rgba(255, 255, 255, 0.9);
    padding: 2px 6px;
    border-radius: 2px;
    white-space: nowrap;
    pointer-events: none;
  }

  .resize-handle {
    position: absolute;
    width: 10px;
    height: 10px;
    background-color: #409eff;
    border: 1px solid #ffffff;
    border-radius: 2px;
    pointer-events: auto;
    z-index: 10;
  }

  .resize-handle.nw {
    top: -5px;
    left: -5px;
    cursor: nw-resize;
  }
  .resize-handle.ne {
    top: -5px;
    right: -5px;
    cursor: ne-resize;
  }
  .resize-handle.sw {
    bottom: -5px;
    left: -5px;
    cursor: sw-resize;
  }
  .resize-handle.se {
    bottom: -5px;
    right: -5px;
    cursor: se-resize;
  }
  .resize-handle.n {
    top: -5px;
    left: calc(50% - 5px);
    cursor: n-resize;
  }
  .resize-handle.s {
    bottom: -5px;
    left: calc(50% - 5px);
    cursor: s-resize;
  }
  .resize-handle.w {
    top: calc(50% - 5px);
    left: -5px;
    cursor: w-resize;
  }
  .resize-handle.e {
    top: calc(50% - 5px);
    right: -5px;
    cursor: e-resize;
  }

  .rotate-handle {
    position: absolute;
    top: -22px;
    left: 50%;
    transform: translateX(-50%);
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background-color: #67c23a;
    border: 2px solid #ffffff;
    cursor: grab;
  }

  .marquee {
    position: absolute;
    border: 1px dashed #409eff;
    background-color: rgba(64, 158, 255, 0.12);
    pointer-events: none;
  }

  .guide-line-x {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 1px;
    background-color: transparent;
    border-left: 1.5px dashed #ff5722; /* 쨍한 주황색 점선 */
    z-index: 9999; /* 캔버스 상의 모든 객체 위로 올라오도록 설정 */
    pointer-events: none;
  }

  .guide-line-y {
    position: absolute;
    left: 0;
    right: 0;
    height: 1px;
    background-color: transparent;
    border-bottom: 1.5px dashed #ff5722;
    z-index: 9999;
    pointer-events: none;
  }
</style>
