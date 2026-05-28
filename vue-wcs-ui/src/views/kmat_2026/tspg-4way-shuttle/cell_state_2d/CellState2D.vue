<!--
  CellState2D.vue
  2D 레이아웃 기반 셀 상태 관리 화면

  ============================================
  핵심 원칙
  ============================================
  - Dashboard2D 의 랙 내부 DOM 구조(.object-content / rack-drive-overlay /
    rack-type-badge / rack-inventory-indicator)를 100% 그대로 유지한다.
  - 여기에 "재고 축 반투명 틴트 + 금지 축 배지/오버레이"를 덧입힌다.
  - 공통 스타일은 ./styles/cell-state.scss 에서 단일 소스로 관리되며,
    이 파일과 components/CellStateLegend.vue 모두 같은 스타일을 공유한다.

  [재고 축]  state_code → .rack-state-tint  (반투명 색)
  [금지 축]  locked / inbound_forbidden / outbound_forbidden
             → .rack-locked-overlay / .rack-forbid-badge (⊘ IN / ⊘ OUT)
-->

<template>
  <div class="dashboard-container cell-state-2d" @click="handleBackgroundClick">
    <CenterSelectModal
      v-if="showCenterModal"
      @submit="handleCenterSelect"
      @close="handleCenterModalClose"
    />

    <div v-else class="dashboard-shell">
      <DashboardHeader
        :eq-groups="store.eqGroups"
        :selected-eq-group-id="store.selectedEqGroupId"
        :pages="sortedPages"
        :active-page-id="activePageId"
        :is-connected="true"
        :last-error="null"
        :ecs-reachable="true"
        :lc-id="lcId"
        page-selector-mode="pager"
        :page-selector-disabled="loading.switching"
        @select-eq-group="handleSelectEqGroup"
        @select-page="selectPage"
        @open-center-modal="openCenterModal"
      />

      <!-- 액션 바 -->
      <div class="action-bar" @click.stop>
        <span class="action-hint">💡 셀 우클릭 → 상태 변경 메뉴 · 드래그로 다중 선택</span>

        <div v-if="can('update')" class="zone-action-group classify-action-group">
          <button
            class="zone-btn zone-btn--classify"
            :disabled="selectedIds.size === 0 || loading.update"
            :title="
              selectedIds.size === 0
                ? '먼저 분류를 변경할 셀을 선택하세요'
                : `선택 ${selectedIds.size}개 셀의 분류·제약 일괄 변경`
            "
            @click="openClassifyPanel"
            >🏷️ 셀 분류 ({{ selectedIds.size }})</button
          >
        </div>

        <div v-if="can('update')" class="zone-action-group">
          <button
            class="zone-btn zone-btn--in"
            :disabled="!canApplyZoneAction || loading.update"
            :title="
              canApplyZoneAction
                ? '현재 ZONE 과 적재단의 모든 셀을 입고 전체 금지 처리합니다'
                : 'ZONE 과 적재단을 먼저 선택하세요'
            "
            @click="applyZoneLevelAction('FORBID_IN_ALL', '입고 전체 금지')"
            >입고 전체 금지</button
          >
          <button
            class="zone-btn zone-btn--out"
            :disabled="!canApplyZoneAction || loading.update"
            :title="
              canApplyZoneAction
                ? '현재 ZONE 과 적재단의 모든 셀을 출고 전체 금지 처리합니다'
                : 'ZONE 과 적재단을 먼저 선택하세요'
            "
            @click="applyZoneLevelAction('FORBID_OUT_ALL', '출고 전체 금지')"
            >출고 전체 금지</button
          >
          <button
            class="zone-btn zone-btn--unlock"
            :disabled="!canApplyZoneAction || loading.update"
            :title="
              canApplyZoneAction
                ? '현재 ZONE 과 적재단의 모든 셀을 금지 해제합니다'
                : 'ZONE 과 적재단을 먼저 선택하세요'
            "
            @click="applyZoneLevelAction('UNLOCK', '전체 금지 해제')"
            >전체 해제</button
          >
        </div>
      </div>

      <div class="dashboard-content">
        <main
          ref="stageContainerRef"
          class="dashboard-body"
          @wheel.prevent="handleWheel"
          @mousedown="onStageMouseDown"
          @contextmenu.prevent
        >
          <div v-if="isLoading" class="loading-overlay">
            <div class="spinner"></div>
            <p>데이터 로딩 중...</p>
          </div>

          <div v-else-if="activePage" class="stage-wrapper">
            <ViewportControls
              @zoom-in="viewport.zoomIn()"
              @zoom-out="viewport.zoomOut()"
              @reset-zoom="viewport.resetZoom100()"
              @fit-to-page="viewport.fitToPage()"
            />

            <CellStateLegend :used-item-groups="usedItemGroups" />

            <div
              class="stage"
              :key="activePageId"
              :style="stageStyle"
              :class="{ 'stage-switching': loading.switching }"
            >
              <!-- 랙 레이어 -->
              <div class="static-equipment-layer">
                <div
                  v-for="obj in rackEquipments"
                  :key="obj.id"
                  class="equipment-object"
                  :class="[
                    getEquipmentClass(obj),
                    {
                      'rack-drive-only': obj.realRackDriveOnlyYn === true,
                      'cell-state-selected': selectedIds.has(obj.id) && !dragging,
                      'cell-state-drag-preview': dragging && dragRectIds.has(obj.id),
                      'cell-state-unselectable': !isRackSelectable(obj),
                      'has-inventory': hasRackInventory(obj),
                      'cs-hide-classify': hideClassifyAxis,
                    },
                  ]"
                  :style="getStaticEquipmentStyle(obj)"
                  :title="getRackTooltip(obj)"
                  @click.stop="handleRackClick(obj, $event)"
                  @contextmenu.prevent.stop="onRackCtxMenu($event, obj)"
                >
                  <!-- DRIVE 전용 오버레이 (Dashboard2D 원본 유지) -->
                  <div v-if="obj.realRackDriveOnlyYn === true" class="rack-drive-overlay"></div>

                  <!-- 재고 축 틴트 (공통 .rack-state-tint) -->
                  <div
                    v-if="getRackTintStyle(obj)"
                    class="rack-state-tint"
                    :style="getRackTintStyle(obj) || {}"
                  ></div>

                  <!-- 랙 본체 = Dashboard2D 와 동일 -->
                  <div class="object-content">
                    <span
                      v-if="getObjectSvg(obj)"
                      class="object-svg"
                      v-html="getObjectSvg(obj)"
                    ></span>
                    <img
                      v-else-if="getObjectIconUrl(obj)"
                      :src="getObjectIconUrl(obj)"
                      :alt="obj.equipmentCode"
                      class="object-image"
                      draggable="false"
                    />
                    <div v-else class="object-placeholder">
                      {{ obj.equipmentTypeCode?.charAt(0) || '?' }}
                    </div>
                  </div>

                  <!-- 포트 배지 -->
                  <div
                    v-if="getRackTypeBadgeText(obj)"
                    class="rack-type-badge"
                    :class="getRackTypeBadgeClass(obj)"
                  >
                    {{ getRackTypeBadgeText(obj) }}
                  </div>

                  <!-- 재고 있음 아이콘 — WCS state_code 기준 (hasRackInventory). -->
                  <!-- ECS realRackCargoYn 는 WCS 와 비동기라 EMPTY 인데도 카고 아이콘이 -->
                  <!-- 잔상으로 남는 케이스가 있어 OR 조건에서 제외. -->
                  <div
                    v-if="hasRackInventory(obj)"
                    class="rack-inventory-indicator"
                    :title="getRackInventoryTooltip(obj)"
                  >
                    <span class="rack-inventory-icon" v-html="cargoIconSvg"></span>
                    <span v-if="(obj.realRackSkuQty ?? 0) > 1" class="rack-inventory-qty">
                      {{ obj.realRackSkuQty }}
                    </span>
                  </div>

                  <!-- 분류 축: item_group 우측 세로 색띠 -->
                  <div
                    v-if="getItemGroupStripeStyle(obj)"
                    class="rack-item-group-stripe"
                    :style="getItemGroupStripeStyle(obj) || {}"
                    :title="getClassifyTooltip(obj)"
                  ></div>

                  <!-- 분류 축: item_type 우상단 mono 배지 -->
                  <div
                    v-if="getItemTypeBadgeText(obj)"
                    class="rack-item-type-badge"
                    :title="getClassifyTooltip(obj)"
                  >
                    {{ getItemTypeBadgeText(obj) }}
                  </div>

                  <!-- 사용 금지 (LOCK) 오버레이 — 빗금 (공통 .rack-locked-overlay) -->
                  <div v-if="isLocked(obj)" class="rack-locked-overlay" title="사용 금지"></div>

                  <!-- 입고 금지 배지 ⊘ IN (좌상단) -->
                  <div
                    v-if="isInboundForbidden(obj)"
                    class="rack-forbid-badge rack-forbid-in"
                    title="입고 금지"
                  >
                    <span class="forbid-slash">⊘</span>
                    <span class="forbid-text">IN</span>
                  </div>

                  <!-- 출고 금지 배지 ⊘ OUT (좌하단) -->
                  <div
                    v-if="isOutboundForbidden(obj)"
                    class="rack-forbid-badge rack-forbid-out"
                    title="출고 금지"
                  >
                    <span class="forbid-slash">⊘</span>
                    <span class="forbid-text">OUT</span>
                  </div>
                </div>
              </div>

              <!-- BAY 라벨 레이어 — 맨 위 랙 꼭대기 위에 숫자 표시 (stage transform 적용) -->
              <div v-if="showCoordinateRulers" class="bay-labels-layer">
                <div
                  v-for="m in bayMarkers"
                  :key="`bay-${m.bay}`"
                  class="bay-label"
                  :class="{ 'label-major': m.bay % 5 === 0 }"
                  :style="{
                    left: `${m.worldX}px`,
                    bottom: `${rackBoundingBox.maxY + labelOffset}px`,
                    width: `${labelSize}px`,
                    height: `${labelSize}px`,
                    transform: 'translateX(-50%)',
                    fontSize: `${labelFontSize}px`,
                  }"
                >
                  {{ m.bay }}
                </div>
              </div>

              <!-- ROW 라벨 레이어 — 맨 왼쪽 랙 왼쪽 바깥에 숫자 표시 -->
              <div v-if="showCoordinateRulers" class="row-labels-layer">
                <div
                  v-for="m in rowMarkers"
                  :key="`row-${m.row}`"
                  class="row-label"
                  :class="{ 'label-major': m.row % 5 === 0 }"
                  :style="{
                    left: `${rackBoundingBox.minX - labelOffset}px`,
                    bottom: `${m.worldY}px`,
                    width: `${labelSize}px`,
                    height: `${labelSize}px`,
                    transform: 'translate(-100%, 50%)',
                    fontSize: `${labelFontSize}px`,
                  }"
                >
                  {{ m.row }}
                </div>
              </div>

              <!-- 드래그 프리뷰 사각형 -->
              <div
                v-if="dragging && dragMoved"
                class="drag-preview-rect"
                :style="dragRectStyle"
              ></div>
            </div>

            <MiniMap
              :page-width="pageWidth"
              :page-height="pageHeight"
              :viewport-rect="viewportWorldRect"
              :zoom="viewport.zoom.value"
              @center="({ worldX, worldY }) => viewport.centerOnWorld(worldX, worldY)"
              @click.stop
            />
          </div>

          <div v-else class="no-data">
            <p>표시할 페이지가 없습니다.</p>
            <p>먼저 맵 에디터에서 레이아웃을 설정해주세요.</p>
            <button v-if="can('update')" class="center-change-btn" @click.stop="openCenterModal">센터 변경</button>
          </div>
        </main>

        <div
          v-show="!taskGrid.isCollapsed.value"
          class="task-resizer"
          :class="{ dragging: taskGrid.isResizing.value }"
          title="드래그해서 상세 그리드 높이 조절"
          @mousedown.stop.prevent="taskGrid.startResize"
        ></div>

        <CellStockDetailGrid
          :cells="selectedCells"
          :eq-group-id="store.selectedEqGroupId"
          :height="taskGrid.height.value"
          v-model:collapsed="taskGrid.isCollapsed.value"
        />
      </div>
    </div>

    <!-- 우클릭 컨텍스트 메뉴 -->
    <ul
      v-show="ctxMenu.visible"
      :style="{ top: ctxMenu.y + 'px', left: ctxMenu.x + 'px' }"
      class="ctx-menu"
      @click.stop
    >
      <template v-for="(item, idx) in ctxMenuItems" :key="idx">
        <li v-if="item.kind === 'section'" class="ctx-section">{{ item.label }}</li>
        <li v-else-if="item.kind === 'divider'" class="ctx-divider"></li>
        <li
          v-else-if="item.visible && can('update')"
          :class="{ 'ctx-disabled': item.disabled }"
          :title="item.title || ''"
          @click="item.disabled ? null : applyCtxItem(item)"
          >{{ item.label }}</li
        >
      </template>
    </ul>

    <!-- 우측 분류·제약 편집 패널 (선택 셀이 있을 때 액션바/우클릭에서 토글) -->
    <CellClassifyPanel
      :visible="showClassifyPanel"
      :selected-cells="selectedCells"
      :eq-group-id="store.selectedEqGroupId"
      @close="showClassifyPanel = false"
      @applied="onClassifyApplied"
    />

    <DashboardToast
      :message="toast.toastMessage.value"
      :type="toast.toastType.value"
      @close="toast.hideToast"
    />
  </div>
</template>

<script setup lang="ts">
  import {
    ref,
    reactive,
    computed,
    watch,
    onMounted,
    onUnmounted,
    nextTick,
    onActivated,
  } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import { useShuttleStore } from '../dashboard_2d/store/shuttleStore';
  import {
    useToast,
    useTaskGridResize,
    useStageViewport,
    useDashboardStyles,
  } from '../dashboard_2d/composables';

  import CenterSelectModal from '../dashboard_2d/editor/CenterSelectModal.vue';
  import DashboardHeader from '../dashboard_2d/dashboard/components/DashboardHeader.vue';
  import ViewportControls from '../dashboard_2d/dashboard/components/ViewportControls.vue';
  import DashboardToast from '../dashboard_2d/dashboard/components/DashboardToast.vue';
  import MiniMap from '../dashboard_2d/dashboard/components/MiniMap.vue';
  import CellStockDetailGrid from './CellStockDetailGrid.vue';
  import CellStateLegend from './components/CellStateLegend.vue';
  import CellClassifyPanel from './components/CellClassifyPanel.vue';
  import { groupColor, abbreviateItemType } from './utils/groupColor';

  import {
    asInlineSvg,
    resolveTypeIconSvg,
    resolveTypeIconUrl,
  } from '../dashboard_2d/assets/iconUrl';
  import cargoIconRaw from '../dashboard_2d/assets/icons/cargo.svg?raw';

  import { getSearchList, updateList } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';

  const MENU = 'CellState2D';
  const { can } = usePermissionLocal(MENU);
  import type { CellStateInfo, DashboardEquipmentData } from '../dashboard_2d/api/types';
  import { LayoutEquipmentType } from '../dashboard_2d/api/types';
  import { RackType } from '../constants/EcsDBConsts';
  import { getRackStateTintStyle } from '../dashboard_2d/utils/rackTint';
  import {
    rackStateLabel,
    hasInventoryByStateCode,
  } from '@/views/kmat_2026/tspg-4way-shuttle/constants/legend/legend-spec';

  // ============================================
  // Store / Router / Message
  // ============================================

  const store = useShuttleStore();
  const route = useRoute();
  const router = useRouter();
  const { notification, createConfirm } = useMessage();

  // 기본 센터 ID — URL 파라미터나 저장된 값이 없을 때 사용.
  // Dashboard2D 와 동일한 값을 사용하여 두 화면이 같은 센터로 기본 진입한다.
  const DEFAULT_LC_ID = '1';

  const toast = useToast();
  const taskGrid = useTaskGridResize({
    onResize: () => viewport.recalculateFit({ keepWorldCenter: true }),
  });

  // ============================================
  // Refs
  // ============================================

  const stageContainerRef = ref<HTMLElement | null>(null);
  const showCenterModal = ref(true);
  const lcIdRef = ref<string>('');
  const spaceDown = ref(false);

  const loading = reactive({ fetch: false, update: false, switching: false });

  const cellStateMap = ref<Map<string, any>>(new Map());
  const selectedIds = ref<Set<string>>(new Set());

  const selectedCellIds = computed<string[]>(() => {
    const out: string[] = [];
    for (const rackLayoutId of selectedIds.value) {
      const cell = cellStateMap.value.get(rackLayoutId);
      if (cell?.rack_id != null && cell.rack_id !== '') {
        out.push(String(cell.rack_id));
      }
    }
    return out;
  });

  /** 그리드용 선택된 셀의 전체 정보 (CellStateService.getCellsByGroup 응답 그대로) */
  const selectedCells = computed<CellStateInfo[]>(() => {
    const out: CellStateInfo[] = [];
    for (const rackLayoutId of selectedIds.value) {
      const cell = cellStateMap.value.get(rackLayoutId);
      if (cell?.rack_id != null && cell.rack_id !== '') {
        out.push(cell as CellStateInfo);
      }
    }
    return out;
  });

  const ctxMenu = reactive({ visible: false, x: 0, y: 0 });

  // 분류 패널 표시 여부 — 액션바 [셀 분류] 또는 우클릭 메뉴에서 토글
  const showClassifyPanel = ref(false);

  const dragging = ref(false);
  const dragMoved = ref(false);
  const dragStartWorld = reactive({ x: 0, y: 0 });
  const dragEndWorld = reactive({ x: 0, y: 0 });
  const dragStartCtrl = ref(false);
  const dragClickGuardUntil = ref(0);

  // ============================================
  // Computed (store 연동)
  // ============================================

  const isLoading = computed(() => store.isLoading || loading.fetch || loading.switching);
  const pages = computed(() => store.pages);
  const activePageId = computed(() => store.activePageId);
  const activePage = computed(() => store.activePage);
  const lcId = computed(() => lcIdRef.value);

  const sortedPages = computed(() => {
    const currentGroupId = store.selectedEqGroupId || '';
    const filtered = (pages.value || []).filter((p) => (p.eqGroupId || '') === currentGroupId);
    return [...filtered].sort((a, b) => {
      const af = a.floorLevel ?? 0;
      const bf = b.floorLevel ?? 0;
      if (af !== bf) return af - bf;
      return (a.pageIndex ?? 0) - (b.pageIndex ?? 0);
    });
  });

  const sortedEquipments = computed(() => store.sortedDashboardData);

  const rackEquipments = computed(() =>
    sortedEquipments.value.filter((e) => e.equipmentTypeCode === LayoutEquipmentType.RACK.code),
  );

  const pageWidth = computed(() => Number(activePage.value?.canvasWidth || 1920));
  const pageHeight = computed(() => Number(activePage.value?.canvasHeight || 1080));

  // ============================================
  // Viewport
  // ============================================

  const viewport = useStageViewport({
    containerRef: stageContainerRef,
    pageWidth,
    pageHeight,
    fitPadding: 0.95,
    minZoom: 0.2,
    maxZoom: 6,
    clampMarginPx: 140,
  });

  const viewportWorldRect = computed(() => viewport.getViewportWorldRect());

  const stageStyle = computed(() => {
    if (!activePage.value) return {};
    return {
      position: 'absolute' as const,
      width: `${pageWidth.value}px`,
      height: `${pageHeight.value}px`,
      backgroundColor: 'transparent',
      left: '0px',
      top: '0px',
      transform: `translate(${viewport.panX.value}px, ${viewport.panY.value}px) scale(${viewport.scale.value})`,
      transformOrigin: '0 0',
    };
  });

  // ============================================
  // 좌표 눈금 (ROW / BAY)
  // ============================================
  // stage transform 안에서 렌더되므로 줌/팬 시 랙과 함께 자동 이동·스케일.

  /** 좌표 눈금 표시 여부 (추후 토글 UI 연결용) */
  const showCoordinateRulers = ref(true);

  /** 평균 랙 셀 크기 (라벨 크기 비례화용) */
  const averageRackCellSize = computed(() => {
    const racks = rackEquipments.value;
    if (racks.length === 0) return { width: 50, height: 50 };
    let totalW = 0;
    let totalH = 0;
    racks.forEach((r) => {
      totalW += r.width || r.defaultWidth || 50;
      totalH += r.height || r.defaultHeight || 50;
    });
    return {
      width: Math.round(totalW / racks.length),
      height: Math.round(totalH / racks.length),
    };
  });

  /** 유니크 BAY → 같은 BAY 랙들의 중앙 X 평균 */
  const bayMarkers = computed(() => {
    const acc = new Map<number, { sum: number; count: number }>();
    for (const rack of rackEquipments.value) {
      const bay = rack.realRackBay;
      if (bay == null) continue;
      const w = Number(rack.width || rack.defaultWidth || 50);
      const cx = Number(rack.posX ?? 0) + w / 2;
      const cur = acc.get(bay);
      if (cur) {
        cur.sum += cx;
        cur.count += 1;
      } else {
        acc.set(bay, { sum: cx, count: 1 });
      }
    }
    return Array.from(acc.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([bay, { sum, count }]) => ({ bay, worldX: sum / count }));
  });

  /** 유니크 ROW → 같은 ROW 랙들의 중앙 Y 평균 */
  const rowMarkers = computed(() => {
    const acc = new Map<number, { sum: number; count: number }>();
    for (const rack of rackEquipments.value) {
      const row = rack.realRackRow;
      if (row == null) continue;
      const h = Number(rack.height || rack.defaultHeight || 50);
      const cy = Number(rack.posY ?? 0) + h / 2;
      const cur = acc.get(row);
      if (cur) {
        cur.sum += cy;
        cur.count += 1;
      } else {
        acc.set(row, { sum: cy, count: 1 });
      }
    }
    return Array.from(acc.entries())
      .sort((a, b) => a[0] - b[0])
      .map(([row, { sum, count }]) => ({ row, worldY: sum / count }));
  });

  /** 라벨 박스 — 랙 셀의 35% (숫자 1~3자리만 담길 정도). */
  const labelSize = computed(() => {
    const cell = averageRackCellSize.value;
    return Math.max(Math.min(cell.width, cell.height) * 0.85, 30);
  });
  /** 폰트 크기 — 라벨 박스의 60% (박스 대비 글자 선명도 UP). */
  const labelFontSize = computed(() => Math.max(labelSize.value * 0.6, 11));
  /** 랙과 라벨 사이 여백 — 셀 폭의 4% (바짝 붙음). */
  const labelOffset = computed(() => Math.max(averageRackCellSize.value.width * 0.04, 2));

  /** 랙들의 실제 렌더링 영역(Bounding Box) 계산 */
  const rackBoundingBox = computed(() => {
    let minX = Infinity;
    let minY = Infinity;
    let maxX = -Infinity;
    let maxY = -Infinity;

    for (const rack of rackEquipments.value) {
      const x = Number(rack.posX ?? 0);
      const y = Number(rack.posY ?? 0);
      const w = Number(rack.width || rack.defaultWidth || 50);
      const h = Number(rack.height || rack.defaultHeight || 50);

      if (x < minX) minX = x;
      if (y < minY) minY = y;
      if (x + w > maxX) maxX = x + w;
      if (y + h > maxY) maxY = y + h;
    }

    // 랙이 하나도 없을 경우 기본값 방어
    if (minX === Infinity) return { minX: 0, minY: 0, maxX: 0, maxY: 0 };
    return { minX, minY, maxX, maxY };
  });

  // ============================================
  // Dashboard2D 스타일 Composable
  // ============================================

  const shuttleSize = ref(50);
  const cargoSize = ref(35);
  const conveyorCargoItems = computed(() => [] as any[]);

  const styles = useDashboardStyles(store as any, shuttleSize, cargoSize, conveyorCargoItems);

  function getEquipmentClass(obj: DashboardEquipmentData) {
    // Dashboard2D 의 'selected' 클래스(파란 glow)를 쓰지 않기 위해 빈 배열 전달.
    return styles.getEquipmentClass(obj, []);
  }

  function getStaticEquipmentStyle(obj: DashboardEquipmentData) {
    return styles.getStaticEquipmentStyle(obj);
  }

  // ============================================
  // 아이콘
  // ============================================

  const cargoIconSvg = cargoIconRaw;
  const _svgCache = new Map<string, string | null>();

  function getObjectSvg(data: any): string | null {
    const key = `${data?.id ?? ''}|${data?.equipmentTypeCode ?? ''}|${data?.iconData2d ?? ''}`;
    if (_svgCache.has(key)) return _svgCache.get(key)!;
    const fromData = asInlineSvg(data?.iconData2d);
    const svg = fromData ?? resolveTypeIconSvg(data?.equipmentTypeCode);
    _svgCache.set(key, svg);
    return svg;
  }

  function getObjectIconUrl(data: any) {
    if (getObjectSvg(data)) return null;
    if (data?.iconData2d) return data.iconData2d;
    return resolveTypeIconUrl(data?.equipmentTypeCode, null);
  }

  // ============================================
  // 유틸
  // ============================================

  function normalizeRow(row: any): any {
    if (!row || typeof row !== 'object') return row;
    const out: Record<string, any> = {};
    for (const k of Object.keys(row)) {
      const v = (row as any)[k];
      out[k] = v === null || v === undefined ? '' : v;
    }
    return out;
  }

  function normalizeList(list: any[]): any[] {
    if (!Array.isArray(list)) return [];
    return list.map(normalizeRow);
  }

  function findCellState(rack: DashboardEquipmentData): any | null {
    return cellStateMap.value.get(rack.id) || null;
  }

  function resolveState(rack: DashboardEquipmentData): string {
    const cell = findCellState(rack);
    if (!cell) return 'NONE';
    return cell.state_code || 'EMPTY';
  }

  function isRackSelectable(rack: DashboardEquipmentData): boolean {
    if (rack.realRackDriveOnlyYn === true) return false;
    return true;
  }

  /**
   * 랙에 물리 재고가 있는지 판단 — 카고 인디케이터(.rack-inventory-indicator,
   * 노란 카고 아이콘) 표시 여부의 SSOT.
   *
   * 반드시 WCS state_code 기준이어야 한다.
   *   - PRODUCT / EMPTY_BOX / OUTBOUND  → 실제 물리 화물 있음
   *   - INBOUND / INBOUND_READY         → 작업 중 (아직 화물 없음)
   *   - EMPTY_OUT / DOUBLE_IN           → 이상 상태 (별도 처리)
   *   - EMPTY / DRIVE / NONE / LOCK     → 화물 없음
   *
   * 과거 구현은 ECS 측 tb_eq_rack_mst.cargo_yn (=obj.realRackCargoYn) 를 직접
   * 봤는데, ECS 와 WCS 가 비동기 갱신되어 cargo_yn=true 인데 stock_id 는 NULL
   * 인 시점 윈도우가 존재한다. 이 경우 셀은 출고/입고 완료 직후의 EMPTY 임에도
   * 노란 카고 아이콘만 남아 "출고중처럼 보이는" 시각 버그가 발생한다.
   * Dashboard2D 가 같은 이유로 state_code 기준을 쓰므로 동일 정책으로 통일.
   */
  function hasRackInventory(obj: DashboardEquipmentData): boolean {
    return hasInventoryByStateCode(resolveState(obj));
  }

  /**
   * 랙 재고 축 state_code → 반투명 틴트 스타일.
   * 공용 유틸 getRackStateTintStyle 를 호출. state_code 는 cellStateMap 에서
   * resolveState(obj) 로 꺼내 Dashboard2D(realRackStateCode) 와 입력 경로만 다름.
   */
  function getRackTintStyle(obj: DashboardEquipmentData): Record<string, string> | null {
    return getRackStateTintStyle({
      stateCode: resolveState(obj),
      driveOnlyYn: obj.realRackDriveOnlyYn,
    });
  }

  // ============================================
  // 금지 축 플래그
  // ============================================

  function toBool(v: any): boolean {
    if (v === true || v === 'true' || v === 'TRUE' || v === 1 || v === '1' || v === 't')
      return true;
    return false;
  }

  function isLocked(rack: DashboardEquipmentData): boolean {
    if (rack.realRackDriveOnlyYn === true) return false;
    const cell = findCellState(rack);
    return toBool(cell?.locked);
  }

  function isInboundForbidden(rack: DashboardEquipmentData): boolean {
    if (rack.realRackDriveOnlyYn === true) return false;
    const cell = findCellState(rack);
    return toBool(cell?.inbound_forbidden);
  }

  function isOutboundForbidden(rack: DashboardEquipmentData): boolean {
    if (rack.realRackDriveOnlyYn === true) return false;
    const cell = findCellState(rack);
    return toBool(cell?.outbound_forbidden);
  }

  function getRackTooltip(rack: DashboardEquipmentData): string {
    const state = resolveState(rack);
    const label = rackStateLabel(state);              // ← legendMap 대신
    const loc = `R${rack.realRackRow ?? '-'}-B${rack.realRackBay ?? '-'}-L${
      rack.realRackLevel ?? '-'
    }`;
    const code = rack.equipmentCode || rack.realRackId || rack.id;
    const drive = rack.realRackDriveOnlyYn === true ? '\n🚗 주행 전용 (적재 불가)' : '';

    const forbids: string[] = [];
    if (isLocked(rack)) forbids.push('🔒 사용 금지');
    if (isInboundForbidden(rack)) forbids.push('⊘ 입고 금지');
    if (isOutboundForbidden(rack)) forbids.push('⊘ 출고 금지');
    const forbidStr = forbids.length > 0 ? '\n' + forbids.join(' · ') : '';

    return `${code}\n상태: ${label}\n위치: ${loc}${forbidStr}${drive}`;
  }

  function getRackInventoryTooltip(obj: DashboardEquipmentData): string {
    const skuId = obj.realRackSkuId || 'N/A';
    const qty = obj.realRackSkuQty ?? 0;
    const location = `R${obj.realRackRow ?? '-'}-B${obj.realRackBay ?? '-'}-L${
      obj.realRackLevel ?? '-'
    }`;
    return `재고: ${skuId}\n수량: ${qty}\n위치: ${location}`;
  }

  // ============================================
  // 분류·제약 축 (item_type / item_group / max_weight / max_height)
  // ============================================

  /** 줌이 일정 이하면 분류 표시 숨김 — 가독성 보호 */
  const hideClassifyAxis = computed(() => viewport.scale.value < 0.6);

  /** 현재 페이지 cellStateMap 에 실제로 등장한 item_group 들 (distinct, 정렬). Legend 용. */
  const usedItemGroups = computed<string[]>(() => {
    const set = new Set<string>();
    for (const cell of cellStateMap.value.values()) {
      const g = cell?.item_group;
      if (g != null && g !== '') set.add(String(g));
    }
    return Array.from(set).sort();
  });

  function getRackItemType(rack: DashboardEquipmentData): string {
    const cell = findCellState(rack);
    const v = cell?.item_type;
    return ValueUtilSafe(v) ? String(v) : '';
  }

  function getRackItemGroup(rack: DashboardEquipmentData): string {
    const cell = findCellState(rack);
    const v = cell?.item_group;
    return ValueUtilSafe(v) ? String(v) : '';
  }

  function getRackMaxWeight(rack: DashboardEquipmentData): number {
    const cell = findCellState(rack);
    const v = Number(cell?.max_weight ?? 0);
    return Number.isFinite(v) ? v : 0;
  }

  function getRackMaxHeight(rack: DashboardEquipmentData): number {
    const cell = findCellState(rack);
    const v = Number(cell?.max_height ?? 0);
    return Number.isFinite(v) ? v : 0;
  }

  function getItemTypeBadgeText(rack: DashboardEquipmentData): string {
    return abbreviateItemType(getRackItemType(rack));
  }

  function getItemGroupStripeStyle(rack: DashboardEquipmentData): Record<string, string> | null {
    const g = getRackItemGroup(rack);
    if (!g) return null;
    return { backgroundColor: groupColor(g) };
  }

  function getClassifyTooltip(rack: DashboardEquipmentData): string {
    const t = getRackItemType(rack);
    const g = getRackItemGroup(rack);
    const w = getRackMaxWeight(rack);
    const h = getRackMaxHeight(rack);
    const lines: string[] = [];
    if (t) lines.push(`Type: ${t}`);
    if (g) lines.push(`Group: ${g}`);
    if (w > 0) lines.push(`Max W: ${w}`);
    if (h > 0) lines.push(`Max H: ${h}`);
    return lines.join('\n');
  }

  /** null/undefined/'' 가 아닌지 — ValueUtil import 없이 가벼운 가드 */
  function ValueUtilSafe(v: any): boolean {
    return v !== null && v !== undefined && v !== '';
  }

  // ============================================
  // 분류 패널 진입점
  // ============================================

  function openClassifyPanel() {
    if (selectedIds.value.size === 0) {
      notification.warning({
        message: '선택 없음',
        description: '먼저 분류를 변경할 셀을 선택하세요.',
        duration: 2,
      });
      return;
    }
    closeCtxMenu();
    showClassifyPanel.value = true;
  }

  async function onClassifyApplied(affected: number) {
    notification.success({
      message: '분류 변경 완료',
      description: `${affected}개 셀이 갱신되었습니다.`,
      duration: 1,
    });
    await loadCellStates();
  }

  function getRackTypeBadgeText(obj: DashboardEquipmentData): string {
    const rackType = obj.realRackType;
    switch (rackType) {
      case RackType.INBOUND_PORT.code:
        return 'IN';
      case RackType.OUTBOUND_PORT.code:
        return 'OUT';
      case RackType.IN_OUTBOUND_PORT.code:
        return 'I/O';
      case RackType.CHARGE_PORT.code:
        return 'CHG';
      case RackType.CHARGE_ENTER_PORT.code:
        return 'ENT';
      default:
        return '';
    }
  }

  function getRackTypeBadgeClass(obj: DashboardEquipmentData): string {
    const rackType = obj.realRackType;
    switch (rackType) {
      case RackType.INBOUND_PORT.code:
        return 'badge-inbound';
      case RackType.OUTBOUND_PORT.code:
        return 'badge-outbound';
      case RackType.IN_OUTBOUND_PORT.code:
        return 'badge-inout';
      case RackType.CHARGE_PORT.code:
        return 'badge-charge';
      case RackType.CHARGE_ENTER_PORT.code:
        return 'badge-charge-enter';
      default:
        return '';
    }
  }

  // ============================================
  // 셀 상태 로드
  // ============================================

  async function loadCellStates(): Promise<void> {
    const eqGroupId = store.selectedEqGroupId;
    const level = activePage.value?.floorLevel;
    if (!eqGroupId || level == null) {
      cellStateMap.value = new Map();
      return;
    }
    loading.fetch = true;
    try {
      const resp = await getSearchList('/wcs/inventory/cell-state/cells', {
        eq_group_id: eqGroupId,
        level,
      });
      const rows = normalizeList(Array.isArray(resp) ? resp : []);

      const idMap = new Map<string, any>();
      const keyMap = new Map<string, any>();

      for (const c of rows) {
        if (c.id != null && c.id !== '') idMap.set(String(c.id), c);
        const k = `${c.row}:${c.bay}:${c.level}`;
        if (!keyMap.has(k)) keyMap.set(k, c);
      }

      const next = new Map<string, any>();
      for (const rack of rackEquipments.value) {
        let cell: any = null;

        if (rack.realEqId != null && rack.realEqId !== '') {
          cell = idMap.get(String(rack.realEqId));
        }
        if (!cell && (rack as any).realRackId != null && (rack as any).realRackId !== '') {
          cell = idMap.get(String((rack as any).realRackId));
        }
        if (
          !cell &&
          rack.realRackRow != null &&
          rack.realRackBay != null &&
          rack.realRackLevel != null
        ) {
          const k = `${rack.realRackRow}:${rack.realRackBay}:${rack.realRackLevel}`;
          cell = keyMap.get(k);
        }

        if (cell) next.set(rack.id, cell);
      }
      cellStateMap.value = next;
    } catch (e: any) {
      notification.error({
        message: '조회 오류',
        description: e?.message || '셀 상태 조회 실패',
        duration: 2,
      });
    } finally {
      loading.fetch = false;
    }
  }

  // ============================================
  // 센터 / 그룹 / 페이지 초기화
  // ============================================

  function resolveInitialLcId(): string {
    return DEFAULT_LC_ID;
  }

  function openCenterModal() {
    showCenterModal.value = true;
  }
  function handleCenterModalClose() {}

  async function handleCenterSelect(nextLcId: string) {
    showCenterModal.value = false;
    await applyCenter(nextLcId);
  }

  function clearCenterScopedLocalStorageExceptLcId() {
    localStorage.removeItem('TSPG_CELL_STATE_EQ_GROUP_ID');
    const keysToRemove: string[] = [];
    for (let i = 0; i < localStorage.length; i += 1) {
      const key = localStorage.key(i);
      if (!key) continue;
      if (key.startsWith('TSPG_CELL_STATE_PAGE_')) keysToRemove.push(key);
    }
    keysToRemove.forEach((k) => localStorage.removeItem(k));
  }

  async function applyCenter(nextLcId: string) {
    const v = String(nextLcId ?? '').trim();
    if (!v) return;

    const prevLcId = String(localStorage.getItem('TSPG_WORK_LC_ID') ?? '').trim();
    const isCenterChanged = !!prevLcId && prevLcId !== v;

    if (isCenterChanged) clearCenterScopedLocalStorageExceptLcId();

    localStorage.setItem('TSPG_WORK_LC_ID', v);

    try {
      if (route.params.lcId !== v) {
        await router.replace({
          name: route.name as string,
          params: { ...route.params, lcId: v },
          query: { ...route.query },
        });
      }
    } catch {
      /* empty */
    }

    lcIdRef.value = v;
    store.reset();
    selectedIds.value = new Set();
    cellStateMap.value = new Map();
    closeCtxMenu();

    const savedEqGroupId = localStorage.getItem('TSPG_CELL_STATE_EQ_GROUP_ID') || '';

    if (savedEqGroupId) {
      await store.initializeWithEqGroup(v, savedEqGroupId);
      if (!store.selectedEqGroupId && store.eqGroups.length > 0) {
        await store.selectEqGroup(store.eqGroups[0].id);
      }
    } else {
      await store.initializeLcOnly(v);
      if (!store.selectedEqGroupId && store.eqGroups.length > 0) {
        await store.selectEqGroup(store.eqGroups[0].id);
      }
    }

    if (store.selectedEqGroupId) {
      const savedPageId = localStorage.getItem(`TSPG_CELL_STATE_PAGE_${store.selectedEqGroupId}`);
      const groupId = store.selectedEqGroupId || '';
      const candidatePages = (store.pages || []).filter((p) => (p.eqGroupId || '') === groupId);

      if (savedPageId && candidatePages.some((p) => p.id === savedPageId)) {
        store.selectPage(savedPageId);
      } else if (candidatePages.length > 0) {
        const first = [...candidatePages].sort((a, b) => {
          const af = a.floorLevel ?? 0;
          const bf = b.floorLevel ?? 0;
          if (af !== bf) return af - bf;
          return (a.pageIndex ?? 0) - (b.pageIndex ?? 0);
        })[0];
        store.selectPage(first.id);
      }
    }

    if (store.activePageId) {
      loading.switching = true;
      try {
        await Promise.all([store.loadLayouts(), store.loadDashboardData()]);
        await loadCellStates();
      } finally {
        loading.switching = false;
      }
    }

    await nextTick();
    viewport.fitToPage();
  }

  async function handleSelectEqGroup(eqGroupId: string) {
    if (!eqGroupId || eqGroupId === store.selectedEqGroupId) return;
    localStorage.setItem('TSPG_CELL_STATE_EQ_GROUP_ID', eqGroupId);

    loading.switching = true;
    try {
      await store.selectEqGroup(eqGroupId);

      const savedPageId = localStorage.getItem(`TSPG_CELL_STATE_PAGE_${eqGroupId}`);
      if (savedPageId && store.pages.some((p) => p.id === savedPageId)) {
        store.selectPage(savedPageId);
      } else if (sortedPages.value.length > 0) {
        store.selectPage(sortedPages.value[0].id);
      }

      selectedIds.value = new Set();
      cellStateMap.value = new Map();
      closeCtxMenu();

      await Promise.all([store.loadLayouts(), store.loadDashboardData()]);
      await loadCellStates();
    } finally {
      loading.switching = false;
    }

    await nextTick();
    viewport.fitToPage();
  }

  async function selectPage(pageId: string) {
    if (pageId === activePageId.value) return;
    closeCtxMenu();
    selectedIds.value = new Set();

    if (store.selectedEqGroupId) {
      localStorage.setItem(`TSPG_CELL_STATE_PAGE_${store.selectedEqGroupId}`, pageId);
    }

    // 깜빡임 방지:
    //   1) loading.switching=true 로 스피너가 끊기지 않게 유지
    //   2) watch 들이 중간에 fitToPage 를 호출하지 못하게 차단
    //   3) 레이아웃/대시보드/셀상태 로드를 병렬(Promise.all)로 묶어
    //      리렌더 빈도 최소화
    loading.switching = true;
    try {
      store.selectPage(pageId);
      await Promise.all([store.loadLayouts(), store.loadDashboardData()]);
      await loadCellStates();
    } finally {
      // switching 을 먼저 내려야 nextTick 이후 watch 가 정상 동작 가능
      loading.switching = false;
    }

    await nextTick();
    viewport.fitToPage();
  }

  // ============================================
  // 좌표 변환 / 드래그
  // ============================================

  function screenToWorld(clientX: number, clientY: number): { x: number; y: number } {
    const rect = stageContainerRef.value?.getBoundingClientRect();
    if (!rect) return { x: 0, y: 0 };
    const s = viewport.scale.value || 1;
    const localX = (clientX - rect.left - viewport.panX.value) / s;
    const localY = (clientY - rect.top - viewport.panY.value) / s;
    return { x: localX, y: pageHeight.value - localY };
  }

  const dragRectIds = computed<Set<string>>(() => {
    if (!dragging.value || !dragMoved.value) return new Set();
    const minX = Math.min(dragStartWorld.x, dragEndWorld.x);
    const maxX = Math.max(dragStartWorld.x, dragEndWorld.x);
    const minY = Math.min(dragStartWorld.y, dragEndWorld.y);
    const maxY = Math.max(dragStartWorld.y, dragEndWorld.y);
    const ids = new Set<string>();
    for (const r of rackEquipments.value) {
      if (!isRackSelectable(r)) continue;
      const rx = Number(r.posX ?? 0);
      const ry = Number(r.posY ?? 0);
      const rw = Number(r.width || r.defaultWidth || 50);
      const rh = Number(r.height || r.defaultHeight || 50);
      if (rx < maxX && rx + rw > minX && ry < maxY && ry + rh > minY) {
        ids.add(r.id);
      }
    }
    return ids;
  });

  const dragRectStyle = computed(() => {
    const minX = Math.min(dragStartWorld.x, dragEndWorld.x);
    const maxX = Math.max(dragStartWorld.x, dragEndWorld.x);
    const minY = Math.min(dragStartWorld.y, dragEndWorld.y);
    const maxY = Math.max(dragStartWorld.y, dragEndWorld.y);
    return {
      position: 'absolute' as const,
      left: `${minX}px`,
      bottom: `${minY}px`,
      width: `${Math.max(0, maxX - minX)}px`,
      height: `${Math.max(0, maxY - minY)}px`,
      // 셀 프리뷰(.cell-state-drag-preview) 와 색상 통일 — 네온 주황
      border: '3px dashed #FFAA00',
      background: 'rgba(255, 170, 0, 0.12)',
      boxShadow: '0 0 12px rgba(255, 170, 0, 0.5)',
      pointerEvents: 'none' as const,
      zIndex: 200,
    };
  });

  function dragShouldIgnoreClick() {
    return Date.now() < dragClickGuardUntil.value;
  }

  function handleWheel(e: WheelEvent) {
    viewport.onWheel(e);
  }

  function onStageMouseDown(e: MouseEvent) {
    const wantsPan = (spaceDown.value && e.button === 0) || e.button === 1;
    if (wantsPan) {
      e.preventDefault();
      e.stopPropagation();
      viewport.startPan(e.clientX, e.clientY);
      window.addEventListener('mousemove', onPanMove, { passive: false });
      window.addEventListener('mouseup', onPanEnd, { passive: false });
      return;
    }
    if (e.button !== 0) return;

    closeCtxMenu();
    dragging.value = true;
    dragMoved.value = false;
    dragStartCtrl.value = e.ctrlKey || e.metaKey;
    const w = screenToWorld(e.clientX, e.clientY);
    dragStartWorld.x = w.x;
    dragStartWorld.y = w.y;
    dragEndWorld.x = w.x;
    dragEndWorld.y = w.y;

    window.addEventListener('mousemove', onDragMove, { passive: false });
    window.addEventListener('mouseup', onDragEnd, { passive: false });
  }

  function onDragMove(e: MouseEvent) {
    if (!dragging.value) return;
    const w = screenToWorld(e.clientX, e.clientY);
    const s = viewport.scale.value || 1;
    const dxScreen = Math.abs(w.x - dragStartWorld.x) * s;
    const dyScreen = Math.abs(w.y - dragStartWorld.y) * s;
    if (dxScreen > 3 || dyScreen > 3) dragMoved.value = true;
    dragEndWorld.x = w.x;
    dragEndWorld.y = w.y;
  }

  function onDragEnd() {
    if (!dragging.value) return;
    const wasMoved = dragMoved.value;
    const ids = wasMoved ? new Set(dragRectIds.value) : new Set<string>();

    dragging.value = false;
    window.removeEventListener('mousemove', onDragMove);
    window.removeEventListener('mouseup', onDragEnd);

    if (wasMoved) {
      if (dragStartCtrl.value) {
        const next = new Set(selectedIds.value);
        ids.forEach((id) => next.add(id));
        selectedIds.value = next;
      } else {
        selectedIds.value = ids;
      }
      dragClickGuardUntil.value = Date.now() + 180;
    } else {
      // 드래그 없이 빈 공간 클릭 → 선택 해제 (Ctrl+클릭은 "추가 선택" 이니 유지)
      if (!dragStartCtrl.value && selectedIds.value.size > 0) {
        selectedIds.value = new Set();
      }
    }
    dragMoved.value = false;
  }

  function onPanMove(e: MouseEvent) {
    if (!viewport.isPanning.value) return;
    e.preventDefault();
    viewport.movePan(e.clientX, e.clientY);
  }

  function onPanEnd() {
    if (!viewport.isPanning.value) return;
    viewport.endPan();
    window.removeEventListener('mousemove', onPanMove);
    window.removeEventListener('mouseup', onPanEnd);
  }

  function handleRackClick(rack: DashboardEquipmentData, e: MouseEvent) {
    if (viewport.shouldIgnoreClick() || dragShouldIgnoreClick()) return;
    if (!isRackSelectable(rack)) return;

    closeCtxMenu();

    if (e.ctrlKey || e.metaKey) {
      const next = new Set(selectedIds.value);
      if (next.has(rack.id)) next.delete(rack.id);
      else next.add(rack.id);
      selectedIds.value = next;
    } else {
      selectedIds.value = new Set([rack.id]);
    }
  }

  function onRackCtxMenu(ev: MouseEvent, rack: DashboardEquipmentData) {
    if (!isRackSelectable(rack)) return;
    if (!selectedIds.value.has(rack.id)) {
      selectedIds.value = new Set([rack.id]);
    }
    ctxMenu.x = ev.clientX;
    ctxMenu.y = ev.clientY;
    ctxMenu.visible = true;
  }

  function closeCtxMenu() {
    ctxMenu.visible = false;
  }

  const ctxMenuItems = computed<any[]>(() => {
    let anyLocked = false;
    for (const id of selectedIds.value) {
      const rack = rackEquipments.value.find((r) => r.id === id);
      if (rack && isLocked(rack)) {
        anyLocked = true;
        break;
      }
    }

    return [
      { kind: 'section', label: '입고' },
      { kind: 'action', label: '입고 허가', action: 'ALLOW_IN', visible: true, disabled: false },
      { kind: 'action', label: '입고 금지', action: 'FORBID_IN', visible: true, disabled: false },
      { kind: 'divider' },
      { kind: 'section', label: '출고' },
      { kind: 'action', label: '출고 허가', action: 'ALLOW_OUT', visible: true, disabled: false },
      { kind: 'action', label: '출고 금지', action: 'FORBID_OUT', visible: true, disabled: false },
      { kind: 'divider' },
      { kind: 'section', label: '잠금' },
      {
        kind: 'action',
        label: '사용 금지 (LOCK)',
        action: 'LOCK',
        visible: !anyLocked,
        disabled: false,
      },
      {
        kind: 'action',
        label: '전체 해제 (UNLOCK)',
        action: 'UNLOCK',
        visible: anyLocked,
        disabled: false,
      },
      { kind: 'divider' },
      { kind: 'section', label: '분류 / 제약' },
      {
        kind: 'action',
        label: '🏷️ 분류·제약 편집...',
        action: 'OPEN_CLASSIFY_PANEL',
        visible: true,
        disabled: false,
        title: 'item_type / item_group / max_weight / max_height 일괄 편집',
      },
    ];
  });

  function applyCtxItem(item: any) {
    if (!item || !item.action) return;
    if (item.action === 'OPEN_CLASSIFY_PANEL') {
      openClassifyPanel();
      return;
    }
    applyAction(item.action, item.label);
  }

  async function applyAction(action: string, label: string) {
    closeCtxMenu();
    if (selectedIds.value.size === 0) {
      notification.error({
        message: '선택 없음',
        description: '대상 셀을 먼저 선택하세요.',
        duration: 2,
      });
      return;
    }
    const cellIds = selectedCellIds.value;
    if (cellIds.length === 0) {
      notification.error({
        message: '매핑 없음',
        description: '선택한 랙의 셀 상태가 조회되지 않았습니다.',
        duration: 2,
      });
      return;
    }
    createConfirm({
      iconType: 'warning',
      title: () => '상태 변경',
      content: () => `선택 ${cellIds.length}개 셀에 [${label}]을(를) 적용합니다.`,
      onOk: async () => {
        loading.update = true;
        try {
          await updateList('/wcs/inventory/cell-state/update-status', {
            eq_group_id: store.selectedEqGroupId,   // ⭐ 추가
            cell_ids: cellIds,
            action,
          });
          notification.success({
            message: '완료',
            description: `${label} 처리되었습니다.`,
            duration: 1,
          });
          await loadCellStates();
        } catch (e: any) {
          notification.error({
            message: '오류',
            description: e?.message || '상태 변경 실패',
            duration: 2,
          });
        } finally {
          loading.update = false;
        }
      },
    });
  }

  const canApplyZoneAction = computed<boolean>(() => {
    return !!store.selectedEqGroupId && activePage.value?.floorLevel != null;
  });

  async function applyZoneLevelAction(action: string, label: string) {
    const zone = store.selectedEqGroupId;
    const lv = activePage.value?.floorLevel;
    if (!zone || lv == null) {
      notification.error({
        message: '선택 필요',
        description: 'ZONE 과 적재단을 먼저 선택하세요.',
        duration: 2,
      });
      return;
    }
    createConfirm({
      iconType: 'warning',
      title: () => '층 전체 상태 변경',
      content: () =>
        `ZONE [${zone}] 적재단 [${lv}] 의 모든 셀에 [${label}]을(를) 적용합니다. 계속하시겠습니까?`,
      onOk: async () => {
        loading.update = true;
        try {
          await updateList('/wcs/inventory/cell-state/update-status', {
            cell_ids: [],
            action,
            eq_group_id: zone,
            level: lv,
          });
          notification.success({
            message: '완료',
            description: `${label} 처리되었습니다.`,
            duration: 1,
          });
          await loadCellStates();
        } catch (e: any) {
          notification.error({
            message: '오류',
            description: e?.message || '상태 변경 실패',
            duration: 2,
          });
        } finally {
          loading.update = false;
        }
      },
    });
  }

  function handleBackgroundClick() {
    if (viewport.shouldIgnoreClick() || dragShouldIgnoreClick()) return;
    closeCtxMenu();
  }

  function onKeyDownGlobal(e: KeyboardEvent) {
    if (e.code === 'Space') {
      spaceDown.value = true;
      e.preventDefault();
    }
    if (e.key === 'Escape') closeCtxMenu();
  }

  function onKeyUpGlobal(e: KeyboardEvent) {
    if (e.code === 'Space') spaceDown.value = false;
  }

  function onGlobalClick() {
    closeCtxMenu();
  }

  onMounted(async () => {
    const initLc = resolveInitialLcId();
    if (initLc) {
      showCenterModal.value = false;
      await applyCenter(initLc);
    } else {
      showCenterModal.value = true;
    }

    window.addEventListener('resize', () => viewport.recalculateFit({ keepWorldCenter: true }));
    window.addEventListener('keydown', onKeyDownGlobal, { passive: false });
    window.addEventListener('keyup', onKeyUpGlobal);
    document.addEventListener('click', onGlobalClick);
  });

  onActivated(async () => {
    // 다른 화면에서 store.reset()을 호출하고 갔을 수 있으므로 재진입 시 복구
    // 현재 lcId 가 있으면 데이터만 다시 로드, 없으면 센터 초기화
    if (lcIdRef.value) {
      // store 가 비어있는지 체크
      if (!store.selectedEqGroupId || !store.activePageId || store.dashboardData.length === 0) {
        await applyCenter(lcIdRef.value);
      } else {
        // store 는 살아있지만 셀 상태만 새로고침
        await loadCellStates();
      }
    }
  });

  onUnmounted(() => {
    taskGrid.cleanup();
    store.reset();
    window.removeEventListener('keydown', onKeyDownGlobal as any);
    window.removeEventListener('keyup', onKeyUpGlobal as any);
    document.removeEventListener('click', onGlobalClick);
  });

  watch(
    () => activePageId.value,
    async () => {
      // 페이지 전환 중에는 selectPage()/applyCenter() 가 마지막에 직접 호출.
      if (loading.switching) return;
      await nextTick();
      viewport.fitToPage();
    },
  );

  watch(
    () => rackEquipments.value.length,
    async (n) => {
      // 페이지 전환 중에는 selectPage()/applyCenter() 가 마지막에 fitToPage 를
      // 직접 호출하므로, 여기서 또 호출하면 transform 이 연속으로 2~3번 바뀌어
      // 화면이 깜빡인다. 전환 중에는 건드리지 않는다.
      if (loading.switching) return;
      if (n > 0) {
        await nextTick();
        viewport.fitToPage();
      }
    },
  );

  watch(taskGrid.isCollapsed, async () => {
    await nextTick();
    viewport.recalculateFit({ keepWorldCenter: true });
  });
</script>

<!--
  ============================================
  Dashboard2D 원본 스타일 + 공통 셀 상태 스타일
  - 둘 다 전역(unscoped) 로 주입되어야 Vue 의 [data-v-*] 속성이 안 붙는
    :deep(.equipment-object) 자식 엘리먼트에도 적용된다.
  - cell-state.scss 는 CellStateLegend.vue 에서도 같은 경로로 import 하여
    범례 프리뷰와 실제 셀이 같은 CSS 변수로 동기화된다.
  ============================================
-->
<style lang="scss">
  @import '../dashboard_2d/styles/dashboard.scss';
  @import './styles/cell-state.scss';
</style>

<style lang="scss" scoped>
  // ============================================
  // 액션 바 (CellState2D 고유)
  // ============================================

  .action-bar {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 4px 10px;
    background: rgba(30, 34, 45, 0.95);
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    flex-wrap: wrap;
    color: #c0c4cc;
  }

  .action-hint {
    font-size: 12px;
    color: #a0a4ad;
    font-weight: 500;
    white-space: nowrap;
  }

  .zone-action-group {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 2px 8px;
    border-left: 1px solid rgba(255, 255, 255, 0.1);
    border-right: 1px solid rgba(255, 255, 255, 0.1);
  }

  .zone-btn {
    height: 26px;
    padding: 0 12px;
    font-size: 12px;
    font-weight: 600;
    border-radius: 3px;
    border: 1px solid;
    cursor: pointer;
    transition: filter 0.15s;
    white-space: nowrap;
  }
  .zone-btn:hover:not(:disabled) {
    filter: brightness(1.08);
  }
  .zone-btn:active:not(:disabled) {
    filter: brightness(0.95);
  }
  .zone-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
  .zone-btn--in {
    background: #ffecec;
    color: #c1272d;
    border-color: #f2a7aa;
  }
  .zone-btn--out {
    background: #fff4d9;
    color: #b58105;
    border-color: #f2d38a;
  }
  .zone-btn--unlock {
    background: #e6f4ea;
    color: #237a3a;
    border-color: #a9d9b9;
  }
  .zone-btn--classify {
    background: #e3eaff;
    color: #1d3fa8;
    border-color: #abbcef;
  }
  .classify-action-group {
    border-left: none;
  }

  .drag-preview-rect {
    border-radius: 2px;
  }

  // ============================================
  // 페이지 전환 중 — 중간 프레임 노출 차단
  // ============================================
  // :key="activePageId" 로 stage 가 재마운트 될 때 빈 프레임이 노출되는
  // 것을 막는다. opacity 트랜지션으로 깔끔하게 교체.

  .stage {
    transition: opacity 0.12s ease;
  }
  .stage.stage-switching {
    opacity: 0;
    pointer-events: none;
  }

  // ============================================
  // 우클릭 컨텍스트 메뉴
  // ============================================

  .ctx-menu {
    position: fixed;
    z-index: 9999;
    list-style: none;
    margin: 0;
    padding: 4px 0;
    background: #fff;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    min-width: 160px;
  }
  .ctx-menu li {
    padding: 6px 14px;
    cursor: pointer;
    font-size: 13px;
    color: #333;
  }
  .ctx-menu li:hover:not(.ctx-section):not(.ctx-divider) {
    background: #f5f5f5;
  }
  .ctx-menu .ctx-section {
    font-size: 11px;
    font-weight: 700;
    color: #888;
    padding: 4px 14px 2px;
    cursor: default;
    background: #fafafa;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .ctx-menu .ctx-divider {
    height: 1px;
    background: #eee;
    margin: 4px 0;
    padding: 0;
    cursor: default;
  }
  .ctx-menu .ctx-disabled {
    color: #aaa;
    font-style: italic;
  }
  .ctx-menu .ctx-disabled:hover {
    background: #fff8e1 !important;
  }
</style>
