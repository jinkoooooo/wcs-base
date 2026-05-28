<!--
  Dashboard2D.vue
  4방향 셔틀 WCS 2D 대시보드 메인 컴포넌트

  ============================================
  기능 개요
  ============================================
  - 물류센터 2D 실시간 모니터링 화면
  - 셔틀, 화물, 랙, 컨베이어 등 설비 시각화
  - 실시간 WebSocket 데이터 연동
  - 설비/셔틀/화물 상세 정보 팝업
  - 하단 작업(Job) 목록 패널

  ============================================
  컴포넌트 구조
  ============================================
  Dashboard2D.vue
  ├── CenterSelectModal (센터 선택 모달)
  ├── DashboardHeader (상단 헤더)
  ├── ViewportControls (줌/팬 컨트롤)
  ├── DashboardLegend (범례 패널)
  ├── MiniMap (미니맵)
  ├── EquipmentDetailPopup (설비 상세 — 셔틀·화물·랙·컨베이어 모두 탭으로 노출)
  ├── AlarmPanel (알람 패널)
  ├── TaskGrid (작업 목록)
  └── DashboardToast (토스트 메시지)

  ============================================
  레이어 구조 (z-index)
  ============================================
  - 정적 설비 (RACK, CONVEYOR 등): z-index 10
  - 컨베이어 화물: z-index 75
  - 랙 재고: z-index 80
  - 이동 화물: z-index 90
  - 셔틀: z-index 100
  - UI 컨트롤: z-index 600
-->

<template>
  <div class="dashboard-container" @click="handleBackgroundClick">
    <!-- ========================================
         센터 선택 모달
         - 첫 접속 또는 센터 변경 시 표시
    ======================================== -->
    <CenterSelectModal
      v-if="showCenterModal"
      @submit="handleCenterSelect"
      @close="handleCenterModalClose"
    />

    <!-- ========================================
         메인 대시보드 (센터 선택 후 표시)
    ======================================== -->
    <div v-else class="dashboard-shell">
      <!-- 상단 헤더 (설비그룹, 페이지, 연결 상태) -->
      <DashboardHeader
        :eq-groups="store.eqGroups"
        :selected-eq-group-id="store.selectedEqGroupId"
        :pages="sortedPages"
        :active-page-id="activePageId"
        :is-connected="isConnected"
        :last-error="lastError"
        page-selector-mode="pager"
        :ecs-reachable="ecsReachable"
        :lc-id="lcId"
        @select-eq-group="handleSelectEqGroup"
        @select-page="selectPage"
        @open-center-modal="openCenterModal"
      />

      <!-- 2D 캔버스 + 작업 패널 영역 -->
      <div class="dashboard-content">
        <!-- ========================================
             메인 캔버스 영역
        ======================================== -->
        <main
          ref="stageContainerRef"
          class="dashboard-body"
          @wheel.prevent="handleWheel"
          @mousedown="onStageMouseDown"
          @contextmenu.prevent
        >
          <!-- 로딩 오버레이 -->
          <div v-if="isLoading" class="loading-overlay">
            <div class="spinner"></div>
            <p>데이터 로딩 중...</p>
          </div>

          <!-- 캔버스 콘텐츠 -->
          <div v-else-if="activePage" class="stage-wrapper">
            <!-- 줌/팬 컨트롤 -->
            <ViewportControls
              @zoom-in="viewport.zoomIn()"
              @zoom-out="viewport.zoomOut()"
              @reset-zoom="viewport.resetZoom100()"
              @fit-to-page="viewport.fitToPage()"
            />

            <!-- 2D 스테이지 (줌/팬 변환 적용) -->
            <div class="stage" :key="activePageId" :style="stageStyle">
              <!-- 정적 설비 레이어 -->
              <div class="static-equipment-layer">
                <div
                  v-for="obj in staticEquipments"
                  :key="obj.id"
                  class="equipment-object"
                  :class="[
                    getEquipmentClass(obj),
                    {
                      'rack-drive-only':
                        obj.equipmentTypeCode === LayoutEquipmentType.RACK.code && obj.realRackDriveOnlyYn === true,
                      'popup-active': isDetailPopupVisible && selectedEquipmentIds.includes(obj.id),
                      'is-dimmed': filters.isEquipmentDimmed(obj),
                      'has-inventory': hasRackInventory(obj), // 랙 재고 클래스 바인딩
                      // 유통기한 만료 — legend-spec RACK_FLAGS 'EXPIRED' / pattern:'expired'.
                      // 색은 cell-state.scss 의 --cs-expired-* 변수가 SSOT (hex 인라인 금지).
                      'expired': obj.equipmentTypeCode === LayoutEquipmentType.RACK.code && isExpired(obj.realRackExpiredDatetime),
                    },
                  ]"
                  :data-type="obj.equipmentTypeCode || ''"
                  :style="getStaticEquipmentStyle(obj)"
                  :title="getEquipmentTooltip(obj)"
                  @click.stop="handleEquipmentClick(obj)"
                >
                  <div
                    v-if="obj.equipmentTypeCode === LayoutEquipmentType.RACK.code && obj.realRackDriveOnlyYn === true"
                    class="rack-drive-overlay"
                  ></div>

                  <!-- 랙 재고 축 틴트 (CellStateService.state_code) — 공용 유틸 rackTint 사용 -->
                  <div
                    v-if="obj.equipmentTypeCode === LayoutEquipmentType.RACK.code && getRackTintStyle(obj)"
                    class="rack-state-tint"
                    :style="getRackTintStyle(obj) || {}"
                  ></div>

                  <!-- 랙 금지 축: 사용금지 (LOCK) 빗금 오버레이 -->
                  <div
                    v-if="obj.equipmentTypeCode === LayoutEquipmentType.RACK.code && obj.realRackLocked === true"
                    class="rack-locked-overlay"
                  ></div>

                  <!-- 랙 금지 축: 입고 금지 (⊘ IN) -->
                  <div
                    v-if="obj.equipmentTypeCode === LayoutEquipmentType.RACK.code && obj.realRackInboundForbidden === true"
                    class="rack-forbid-badge rack-forbid-in"
                  >
                    <span class="forbid-slash">⊘</span>
                    <span class="forbid-text">IN</span>
                  </div>

                  <!-- 랙 금지 축: 출고 금지 (⊘ OUT) -->
                  <div
                    v-if="
                      obj.equipmentTypeCode === LayoutEquipmentType.RACK.code && obj.realRackOutboundForbidden === true
                    "
                    class="rack-forbid-badge rack-forbid-out"
                  >
                    <span class="forbid-slash">⊘</span>
                    <span class="forbid-text">OUT</span>
                  </div>

                  <div class="object-content">
                    <img
                      v-if="getObjectImageSrc(obj)"
                      :src="getObjectImageSrc(obj)"
                      :alt="obj.equipmentCode"
                      class="object-image"
                      draggable="false"
                    />
                    <div v-else-if="obj.equipmentTypeCode === LayoutEquipmentType.PILLAR.code" class="pillar-placeholder">
                      <span class="pillar-label">기둥</span>
                    </div>
                    <div v-else-if="obj.equipmentTypeCode === LayoutEquipmentType.BCR.code" class="bcr-placeholder">
                      <span class="bcr-icon">▥</span>
                      <span class="bcr-label">BCR</span>
                    </div>
                    <div v-else-if="obj.equipmentTypeCode === LayoutEquipmentType.STV.code" class="stv-placeholder">
                      <span class="stv-label">STV</span>
                    </div>
                    <div v-else-if="obj.equipmentTypeCode === LayoutEquipmentType.CRANE.code" class="crane-placeholder">
                      <span class="crane-label">CRN</span>
                    </div>
                    <div v-else class="object-placeholder">
                      {{ obj.equipmentTypeCode?.charAt(0) || '?' }}
                    </div>
                  </div>

                  <div
                    v-if="obj.equipmentTypeCode === LayoutEquipmentType.RACK.code && getRackTypeBadgeText(obj)"
                    class="rack-type-badge"
                    :class="getRackTypeBadgeClass(obj)"
                  >
                    {{ getRackTypeBadgeText(obj) }}
                  </div>

                  <!-- 포트 운영 모드 배지 (입고/출고/유휴) — 사용자가 한눈에 파악할 수 있도록 큼직하게 노출 -->
                  <div
                    v-if="isPortRack(obj) && getPortModeBadgeText(obj)"
                    class="port-mode-badge-overlay"
                    :class="getPortModeBadgeClass(obj)"
                    :title="getPortModeTooltip(obj)"
                  >
                    <span class="port-mode-icon">{{ getPortModeIcon(obj) }}</span>
                    <span class="port-mode-text">{{ getPortModeBadgeText(obj) }}</span>
                  </div>

                  <div
                    v-if="obj.equipmentTypeCode === LayoutEquipmentType.RACK.code && hasRackInventory(obj)"
                    class="rack-inventory-indicator"
                    :title="getRackInventoryTooltip(obj)"
                  >
                    <span class="rack-inventory-icon" v-html="cargoIconSvg"></span>
                  </div>

                  <div v-if="hasEquipmentError(obj)" class="equipment-error-badge">
                    <span>⚠️ ERROR</span>
                  </div>

                  <div v-if="obj.showLabel" class="object-label">
                    {{ obj.customLabel || obj.equipmentCode }}
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

              <!-- 컨베이어 화물 레이어 (Conveyor Pallet, legend-spec 의 PalletState 기준) -->
              <div class="conveyor-cargo-layer">
                <div
                  v-for="item in conveyorCargoItems"
                  :key="item.layout.id"
                  class="conveyor-cargo"
                  :class="`pallet-state-${item.palletState}`"
                  :style="styles.getConveyorCargoStyle(item)"
                  :title="styles.getConveyorCargoTooltip(item)"
                  @click.stop="handleConveyorCargoClick(item)"
                >
                  <span class="conveyor-cargo-icon" v-html="cargoIconSvg"></span>
                </div>
              </div>

              <!-- 화물 레이어 -->
              <div class="cargo-layer">
                <div
                  v-for="cargo in displayCargos"
                  :key="cargo.id"
                  class="cargo-object"
                  :class="[
                    ...styles.getCargoClass(cargo),
                    { 'is-entering': cargo.displayState === 'entering' },
                    { 'is-leaving': cargo.displayState === 'leaving' },
                  ]"
                  :style="{
                    ...styles.getCargoStyle(cargo),
                    opacity: cargo.opacity ?? 1,
                  }"
                  :title="styles.getCargoTooltip(cargo)"
                  @click.stop="handleCargoClick(cargo)"
                >
                  <span class="cargo-icon" v-html="cargoIconSvg"></span>
                  <div class="cargo-label">
                    {{ cargo.data?.barcode?.toString().slice(-4) || '?' }}
                  </div>
                </div>
              </div>

              <!-- 셔틀 레이어 -->
              <div class="shuttle-layer">
                <div
                  v-for="shuttle in interpolatedShuttles"
                  :key="shuttle.id"
                  class="shuttle-object"
                  :class="[
                    ...styles.getShuttleClass(shuttle, selectedShuttleEntry?.id),
                    { 'is-entering': shuttle.displayState === 'entering' },
                    { 'is-leaving': shuttle.displayState === 'leaving' },
                    {
                      'popup-active':
                        isDetailPopupVisible && selectedShuttleEntry?.id === shuttle.id,
                    },
                    { 'has-error': hasEquipmentError(shuttle) },
                    { 'is-dimmed': filters.isShuttleDimmed(shuttle.data) },
                  ]"
                  :style="{
                    ...styles.getShuttleStyle(shuttle),
                    opacity: shuttle.opacity ?? 1,
                  }"
                  :title="getShuttleTooltip(shuttle)"
                  @click.stop="handleShuttleClick(shuttle)"
                >
                  <span class="shuttle-icon" v-html="shuttleIconSvg"></span>
                  <span
                    v-if="shuttle.data?.hasCargo"
                    class="shuttle-carried-cargo"
                    v-html="cargoIconSvg"
                  ></span>
                  <div class="shuttle-label">
                    {{ shuttle.data?.equipmentCode || shuttle.id }}
                  </div>
                  <!-- 배터리 인디케이터 -->
                  <div
                    v-if="shuttle.data?.batteryLevel != null"
                    class="shuttle-battery"
                    :class="getBatteryClass(shuttle.data.batteryLevel)"
                    :title="`배터리: ${shuttle.data.batteryLevel}%`"
                  >
                    <div
                      class="battery-fill"
                      :style="{ width: `${shuttle.data.batteryLevel}%` }"
                    ></div>
                  </div>
                  <!-- 에러 배지 -->
                  <div v-if="hasEquipmentError(shuttle)" class="equipment-error-badge">
                    <span>⚠️ ERROR</span>
                  </div>
                </div>
              </div>

              <!-- 리프터 상태 오버레이 레이어 -->
              <div class="lifter-overlay-layer">
                <div
                  v-for="lifter in lifterOverlays"
                  :key="lifter.layoutId"
                  class="lifter-overlay"
                  :class="{
                    'is-running': lifter.isMoving,
                    'is-going-up': lifter.isGoingUp,
                    'is-going-down': lifter.isGoingDown,
                    'has-shuttle': lifter.hasShuttle,
                    'has-cargo': lifter.hasCargo,
                    'has-error': lifter.hasError,
                    'has-active-job': lifter.hasActiveJob,
                    'stopper-open': lifter.stopperOpen,
                    'popup-active':
                      isDetailPopupVisible && selectedEquipmentIds.includes(lifter.layout.id),
                    'is-dimmed': filters.isLifterDimmed(lifter),
                  }"
                  :style="getLifterOverlayStyle(lifter)"
                  :title="getLifterTooltip(lifter)"
                  @click.stop="handleLifterClick(lifter)"
                >
                  <!-- 리프터 상태 카드 -->
                  <div class="lifter-status-card">
                    <!-- 층 표시 -->
                    <div class="lifter-floor-display">
                      <span class="floor-current">{{ lifter.currentLevel }}F</span>
                      <template v-if="lifter.isMoving && lifter.targetLevel">
                        <span
                          class="floor-arrow"
                          :class="{
                            'going-up': lifter.isGoingUp,
                            'going-down': lifter.isGoingDown,
                          }"
                        >
                          {{ lifter.isGoingUp ? '▲' : '▼' }}
                        </span>
                        <span class="floor-target">{{ lifter.targetLevel }}F</span>
                      </template>
                    </div>

                    <!-- 상태 아이콘 영역 -->
                    <div class="lifter-status-icons">
                      <!-- 동작 상태 (run_yn) -->
                      <div class="status-indicator run-status" :class="{ active: lifter.isMoving }">
                        <span class="indicator-icon">⚡</span>
                      </div>
                      <!-- 화물 상태 (cargo_yn) -->
                      <div
                        class="status-indicator cargo-status"
                        :class="{ active: lifter.hasCargo }"
                      >
                        <span class="indicator-icon">📦</span>
                      </div>
                      <!-- 스토퍼 상태 -->
                      <div
                        class="status-indicator stopper-status"
                        :class="{ open: lifter.stopperOpen }"
                      >
                        <span class="indicator-icon">🚧</span>
                      </div>
                    </div>
                  </div>

                  <!-- 셔틀 적재 배지 -->
                  <div v-if="lifter.hasShuttle" class="lifter-shuttle-indicator">
                    <span class="shuttle-icon-mini">🚗</span>
                    <span class="shuttle-label-mini">셔틀</span>
                  </div>

                  <!-- 화물 적재 배지 (큰 아이콘) -->
                  <div v-if="lifter.hasCargo" class="lifter-cargo-indicator">
                    <span class="lifter-cargo-icon" v-html="cargoIconSvg"></span>
                  </div>

                  <!-- 에러 표시 -->
                  <div v-if="lifter.hasError" class="lifter-error-badge">
                    <span>⚠️ ERROR</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 미니맵 -->
            <MiniMap
              :page-width="pageWidth"
              :page-height="pageHeight"
              :viewport-rect="viewportWorldRect"
              :zoom="viewport.zoom.value"
              @center="({ worldX, worldY }) => viewport.centerOnWorld(worldX, worldY)"
              @click.stop
            />
          </div>

          <!-- 페이지 없음 -->
          <div v-else class="no-data">
            <p>표시할 페이지가 없습니다.</p>
            <p>먼저 맵 에디터에서 레이아웃을 설정해주세요.</p>
            <button v-if="can('update')" class="center-change-btn" @click.stop="openCenterModal">센터 변경</button>
          </div>

          <!-- 설비 상세 팝업 — 정적 설비 + 셔틀까지 모두 동일 팝업의 탭으로 노출 -->
          <EquipmentDetailPopup
            :visible="isDetailPopupVisible"
            :equipments="selectedEquipments"
            :lc-id="lcId"
            @close="closeDetailPopup"
          />

          <!-- 범례/알람 — dashboard-body(캔버스) 안에서만 floating. 헤더/TaskGrid 와는 겹치지 않음.
               AlarmPanel 이 설비 알람 + 재입고 필요 파렛트 알람(§12) 통합 표시. -->
          <DashboardLegend />
          <AlarmPanel />
        </main>

        <!-- 작업 패널 리사이저 -->
        <div
          v-show="!taskGrid.isCollapsed.value"
          class="task-resizer"
          :class="{ dragging: taskGrid.isResizing.value }"
          title="드래그해서 작업 목록 높이 조절"
          @mousedown.stop.prevent="taskGrid.startResize"
        ></div>

        <!-- 하단 작업 그리드 -->
        <TaskGrid
          :jobs="activeJobs"
          :height="taskGrid.height.value"
          v-model:collapsed="taskGrid.isCollapsed.value"
          @cancel-job="ecsCommands.handleCancelJob"
          @resume-job="ecsCommands.handleResumeJob"
          @refresh="refreshJobs"
        />
      </div>
    </div>

    <!-- 토스트 메시지 -->
    <DashboardToast
      :message="toast.toastMessage.value"
      :type="toast.toastType.value"
      @close="toast.hideToast"
    />

    <!-- 검색 오버레이 -->
    <DashboardSearchBar
      v-if="showSearchOverlay"
      :equipments="searchEquipments"
      :shuttles="searchShuttles"
      :cargos="searchCargos"
      @select="handleSearchSelect"
      @close="showSearchOverlay = false"
    />
  </div>
</template>

<script setup lang="ts">
  /**
   * Dashboard2D 스크립트
   *
   * ============================================
   * Import 구조
   * ============================================
   * 1. Vue 핵심 (ref, computed, watch, 라이프사이클)
   * 2. Vue Router
   * 3. Pinia Store
   * 4. Composables (재사용 로직)
   * 5. 하위 컴포넌트
   * 6. 타입/상수/유틸
   */

  import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  // Store
  import { useShuttleStore } from '../store/shuttleStore';

  // 권한 (메뉴 단위)
  import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';

  // Composables
  import {
    useToast,
    useEcsCommands,
    useTaskGridResize,
    useDashboardStyles,
    useShuttleInterpolation,
    useCargoInterpolation,
    useStageViewport,
    useDashboardFilters,
  } from '../composables';
  import type { ConveyorCargoItem, SearchResultItem } from '../composables';

  // Components
  import CenterSelectModal from '../editor/CenterSelectModal.vue';
  import DashboardHeader from './components/DashboardHeader.vue';
  import ViewportControls from './components/ViewportControls.vue';
  import DashboardLegend from './components/DashboardLegend.vue';
  import DashboardToast from './components/DashboardToast.vue';
  import MiniMap from './components/MiniMap.vue';
  import TaskGrid from './TaskGrid.vue';
  import EquipmentDetailPopup from './components/EquipmentDetailPopup.vue';
  import AlarmPanel from './components/AlarmPanel.vue';
  import DashboardSearchBar from './components/DashboardSearchBar.vue';

  // Types & Utils
  import type { DashboardEquipmentData, LifterOverlay } from '../api/types';
  import { STATIC_EQUIPMENT_TYPES, LayoutEquipmentType, isError } from '../api/types';
  import {
    RackType,
    PortMode,
    enumLabel,
    enumShortLabel,
    enumIcon,
    enumBadgeText,
    enumBadgeVariant,
  } from '../../constants';
  import { shuttleStateSpec, hasInventoryByStateCode} from '@/views/kmat_2026/tspg-4way-shuttle/constants/legend/legend-spec';
  import { getRackStateTintStyle } from '../utils/rackTint';
  import { asInlineSvg, resolveTypeIconSvg, resolveTypeIconUrl } from '../assets/iconUrl';
  import shuttleIconRaw from '../assets/icons/shuttle.svg?raw';
  import cargoIconRaw from '../assets/icons/cargo.svg?raw';

  // ============================================
  // 권한 (메뉴 단위)
  // ============================================
  const MENU = 'Dashboard2D';
  const { can } = usePermissionLocal(MENU);

  // ============================================
  // Store & Router
  // ============================================

  const store = useShuttleStore();
  const route = useRoute();
  const router = useRouter();

  // ============================================
  // Composables 초기화
  // ============================================

  const DEFAULT_LC_ID = '1'; // ← 기본 센터 ID

  /** 토스트 메시지 관리 */
  const toast = useToast();

  /** 작업 패널 리사이즈 관리 */
  const taskGrid = useTaskGridResize({
    onResize: () => viewport.recalculateFit({ keepWorldCenter: true }),
  });

  /** 필터 및 하이라이트 모드 관리 (Conveyor 판정에 store.rtConveyors 필요) */
  const filters = useDashboardFilters(store as any);

  /** 검색용 설비/셔틀/화물 데이터 refs */
  const searchEquipments = computed(() => store.dashboardData || []);
  const searchShuttles = computed(() => store.shuttleRtData || []);
  const searchCargos = computed(() => store.dashboardCargos || []);

  // ============================================
  // Refs
  // ============================================

  /** 스테이지 컨테이너 참조 */
  const stageContainerRef = ref<HTMLElement | null>(null);

  /** 센터 선택 모달 표시 여부 */
  const showCenterModal = ref(true);

  /** 현재 센터 ID */
  const lcIdRef = ref<string>('');

  /** ECS 연결 상태 */
  const ecsReachable = ref(true);

  /** 스페이스바 누름 상태 (팬 모드) */
  const spaceDown = ref(false);

  // 팝업 상태
  const isDetailPopupVisible = ref(false);
  const selectedEquipmentIds = ref<string[]>([]);
  /** 셔틀 클릭 시 합성한 DashboardEquipmentData 엔트리 (SHUTTLE 도 같은 팝업 탭으로 노출) */
  const selectedShuttleEntry = ref<DashboardEquipmentData | null>(null);

  // 검색 오버레이 상태
  const showSearchOverlay = ref(false);

  /** 설비 타입 우선순위 — 같은 위치에 겹친 설비를 정렬할 때 사용 (SHUTTLE → RACK → CONVEYOR → 그외) */
  const EQUIPMENT_SORT_PRIORITY: Record<string, number> = {
    [LayoutEquipmentType.SHUTTLE.code]: 0,
    [LayoutEquipmentType.RACK.code]: 1,
    [LayoutEquipmentType.CONVEYOR.code]: 2,
    [LayoutEquipmentType.LIFTER.code]: 3,
  };

  function equipmentSortRank(eq: DashboardEquipmentData): number {
    const t = String(eq?.equipmentTypeCode || '').toUpperCase();
    return EQUIPMENT_SORT_PRIORITY[t] ?? 9;
  }

  const selectedEquipments = computed<DashboardEquipmentData[]>(() => {
    const statics = store.dashboardData.filter((x) => selectedEquipmentIds.value.includes(x.id));
    const all = selectedShuttleEntry.value
      ? [selectedShuttleEntry.value, ...statics]
      : [...statics];
    return all.sort((a, b) => equipmentSortRank(a) - equipmentSortRank(b));
  });


  // ============================================
  // Computed - Store 상태
  // ============================================

  const isLoading = computed(() => store.isLoading);
  const isConnected = computed(() => store.isConnected);
  const lastError = computed(() => store.lastError);
  const pages = computed(() => store.pages);
  const activePageId = computed(() => store.activePageId);
  const activePage = computed(() => store.activePage);
  const activeJobs = computed(() => store.activeJobs);
  const lcId = computed(() => lcIdRef.value);

  /** 정렬된 페이지 (현재 설비그룹 기준) */
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

  /** 정적 설비 (RACK, CONVEYOR 등) */
  const staticEquipments = computed(() =>
    sortedEquipments.value.filter((e) =>
      STATIC_EQUIPMENT_TYPES.includes(e.equipmentTypeCode as any),
    ),
  );

  /** 페이지 크기 */
  const pageWidth = computed(() => Number(activePage.value?.canvasWidth || 1920));
  const pageHeight = computed(() => Number(activePage.value?.canvasHeight || 1080));

  // ============================================
  // Viewport (줌/팬)
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

  /** 스테이지 CSS 스타일 (transform 적용) */
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
  // 월드좌표 기반이라 수동 screen 변환 불필요. 뷰포트 클리핑도 transform 이 처리.

  /** 좌표 눈금 표시 여부 (추후 토글 UI 연결용) */
  const showCoordinateRulers = ref(true);

  /** 랙만 필터링한 설비 (눈금 계산용) */
  const racksForRuler = computed(() =>
    staticEquipments.value.filter((e) => e.equipmentTypeCode === LayoutEquipmentType.RACK.code),
  );

  /** 유니크 BAY → 같은 BAY 랙들의 중앙 X 평균 */
  const bayMarkers = computed(() => {
    const acc = new Map<number, { sum: number; count: number }>();
    for (const rack of racksForRuler.value) {
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
    for (const rack of racksForRuler.value) {
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

  /**
   * 라벨 평균 셀 크기 (비례 계산용). averageRackCellSize 와 같은 랙 소스를 공유.
   * 여기서 직접 평균을 내 뒤쪽 computed 순서 의존을 피한다.
   */
  const rulerAverageCell = computed(() => {
    const racks = racksForRuler.value;
    if (racks.length === 0) return { width: 50, height: 50 };
    let tw = 0,
      th = 0;
    for (const r of racks) {
      tw += r.width || r.defaultWidth || 50;
      th += r.height || r.defaultHeight || 50;
    }
    return { width: tw / racks.length, height: th / racks.length };
  });

  /** 라벨 박스 — 랙 셀의 35% (숫자 1~3자리만 담길 정도). */
  const labelSize = computed(() => {
    const cell = rulerAverageCell.value;
    return Math.max(Math.min(cell.width, cell.height) * 0.85, 30);
  });
  /** 폰트 크기 — 라벨 박스의 60% (박스 대비 글자 선명도 UP). */
  const labelFontSize = computed(() => Math.max(labelSize.value * 0.6, 11));
  /** 랙과 라벨 사이 여백 — 셀 폭의 4% (바짝 붙음). */
  const labelOffset = computed(() => Math.max(rulerAverageCell.value.width * 0.04, 0.4));

  /** 랙들의 실제 렌더링 영역(Bounding Box) 계산 */
  const rackBoundingBox = computed(() => {
    let minX = Infinity;
    let minY = Infinity;
    let maxX = -Infinity;
    let maxY = -Infinity;

    for (const rack of racksForRuler.value) {
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
  // 셔틀/화물 보간 애니메이션
  // ============================================

  const {
    interpolatedItems: interpolatedShuttles,
    updatePositions: updateShuttlePositions,
    clear: clearShuttles,
  } = useShuttleInterpolation();

  const {
    interpolatedItems: displayCargos,
    updatePositions: updateCargoPositions,
    clear: clearCargos,
  } = useCargoInterpolation();

  // ============================================
  // 셔틀/화물 크기 계산
  // ============================================

  /** 평균 랙 셀 크기 (셔틀 크기 기준) */
  const averageRackCellSize = computed(() => {
    const racks = staticEquipments.value.filter((e) => e.equipmentTypeCode === LayoutEquipmentType.RACK.code);
    if (racks.length === 0) return { width: 50, height: 50 };

    let totalWidth = 0;
    let totalHeight = 0;
    racks.forEach((r) => {
      totalWidth += r.width || r.defaultWidth || 50;
      totalHeight += r.height || r.defaultHeight || 50;
    });

    return {
      width: Math.round(totalWidth / racks.length),
      height: Math.round(totalHeight / racks.length),
    };
  });

  /** 셔틀 크기 (랙 셀의 90%) */
  const shuttleSize = computed(() => {
    const cellSize = averageRackCellSize.value;
    const size = Math.min(cellSize.width, cellSize.height) * 0.9;
    return Math.max(size, 30);
  });

  /** 화물 크기 (셔틀의 70%) */
  const cargoSize = computed(() => {
    return Math.max(shuttleSize.value * 0.7, 25);
  });

  // ============================================
  // 컨베이어 화물 아이템
  // ============================================

  const conveyorCargoItems = computed<ConveyorCargoItem[]>(() => {
    // palletState 판정은 전적으로 백엔드(EquipmentStateClassifier.classifyPalletState) 에서 결정됨.
    // 프런트는 cv.palletState 값을 그대로 읽기만 하고 위치 판별/조합 로직을 재실행하지 않는다.
    const items: ConveyorCargoItem[] = [];

    staticEquipments.value.forEach((layout) => {
      if (layout.equipmentTypeCode !== LayoutEquipmentType.CONVEYOR.code || !layout.realEqId) return;

      const cv = store.rtConveyors.find(
        (x) => x.eqId === layout.realEqId || x.equipmentId === layout.realEqId,
      );
      if (!cv || !cv.palletState) return;

      items.push({
        layout,
        cv,
        palletState: cv.palletState,
      });
    });

    return items;
  });

  const lifterOverlays = computed<LifterOverlay[]>(() => {
    const overlays: LifterOverlay[] = [];
    const lifters = Array.isArray(store.rtLifters) ? store.rtLifters : [];

    sortedEquipments.value.forEach((layout) => {
      if (layout.equipmentTypeCode !== LayoutEquipmentType.LIFTER.code) return;

      const layoutId = String(layout.id ?? '');
      const realEqId = String(layout.realEqId ?? '');

      const rtLift = lifters.find((lf: any) => {
        const lfLayoutId = String(lf?.layoutId ?? '');
        const lfEqId = String(lf?.eqId ?? '');
        const lfEquipmentId = String(lf?.equipmentId ?? '');
        return lfLayoutId === layoutId || lfEqId === realEqId || lfEquipmentId === realEqId;
      });

      if (!rtLift) return;

      const currentLevel = Number(rtLift.currentLevel ?? 1);
      const targetLevel =
        rtLift.targetLevel == null || Number(rtLift.targetLevel) === 0
          ? null
          : Number(rtLift.targetLevel);

      const isMoving = rtLift.moving === true;
      // AlarmDataProvider 와 정합: use_yn=false 리프터는 에러 알람 대상 아님 → 여기도 제외.
      // 에러 판정은 errorId 기준 (null/""/"0" 제외).
      const disabled = rtLift.useYn === false;
      const hasError = !disabled && isError(rtLift.errorId) === true;
      const hasActiveJob = rtLift.hasActiveJob === true;

      overlays.push({
        layoutId,
        layout,
        currentLevel,
        targetLevel,
        isMoving,
        isGoingUp: isMoving && targetLevel != null && targetLevel > currentLevel,
        isGoingDown: isMoving && targetLevel != null && targetLevel < currentLevel,
        hasShuttle: rtLift.hasShuttle === true,
        hasCargo: rtLift.hasCargo === true,
        hasError,
        hasActiveJob,
        stopperOpen: rtLift.stopperOpen === true,
      });
    });

    return overlays;
  });

  /**
   * 리프터 오버레이 스타일
   * 리프터 설비 위에 크게 표시되도록 설정
   */
  function getLifterOverlayStyle(lifter: LifterOverlay): Record<string, any> {
    const layout = lifter.layout;
    const baseWidth = layout.width || layout.defaultWidth || 100;
    const baseHeight = layout.height || layout.defaultHeight || 100;

    // 오버레이를 설비보다 크게 (1.5배)
    const overlayWidth = Math.max(baseWidth * 1.5, 120);
    const overlayHeight = Math.max(baseHeight * 1.5, 120);

    // 중앙 정렬을 위한 오프셋
    const offsetX = (overlayWidth - baseWidth) / 2;
    const offsetY = (overlayHeight - baseHeight) / 2;

    return {
      position: 'absolute',
      left: `${layout.posX - offsetX}px`,
      bottom: `${layout.posY - offsetY}px`,
      width: `${overlayWidth}px`,
      height: `${overlayHeight}px`,
      zIndex: 95, // 셔틀(100)보다 아래, 화물(90)보다 위
      pointerEvents: 'auto',
    };
  }

  function getLifterTooltip(lifter: LifterOverlay): string {
    const lines: string[] = [];

    lines.push(`현재층: ${lifter.currentLevel}F`);

    if (lifter.targetLevel != null) {
      lines.push(`목표층: ${lifter.targetLevel}F`);
    }

    lines.push(`이동중: ${lifter.isMoving ? 'Y' : 'N'}`);
    lines.push(`셔틀 적재: ${lifter.hasShuttle ? 'Y' : 'N'}`);
    lines.push(`화물 적재: ${lifter.hasCargo ? 'Y' : 'N'}`);
    lines.push(`작업 존재: ${lifter.hasActiveJob ? 'Y' : 'N'}`);
    lines.push(`스토퍼: ${lifter.stopperOpen ? 'OPEN' : 'CLOSE'}`);
    lines.push(`에러: ${lifter.hasError ? 'Y' : 'N'}`);

    return lines.join('\n');
  }

  // ============================================
  // 스타일 Composable
  // ============================================

  const styles = useDashboardStyles(store as any, shuttleSize, cargoSize, conveyorCargoItems);

  // ============================================
  // ECS 명령 Composable
  // ============================================

  const ecsCommands = useEcsCommands(lcIdRef, toast.showToast);

  // ============================================
  // 아이콘 SVG
  // ============================================

  const shuttleIconSvg = shuttleIconRaw;
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

  // SVG는 data URL로, URL은 그대로 — 모두 <img> 로 처리
  // <img> 는 부모 CSS의 currentColor를 상속받지 못해 SVG 내부 fill="currentColor"가
  // 검정으로 떨어진다. 루트 <svg> 에 color/fill/stroke 를 직접 박아 인라인 SVG 시절의
  // `--dashboard-text-secondary` (#c0c4cc) 밝은 회색을 그대로 재현한다.
  const _imgSrcCache = new Map<string, string>();
  const OBJECT_SVG_TINT = '#c0c4cc'; // --dashboard-text-secondary

  function tintObjectSvg(svgText: string): string {
    // 첫 번째 <svg ...> 여는 태그에 color/fill/stroke 속성 주입.
    // 이미 있으면 덮어쓰지 않고, 없으면 추가한다.
    return svgText.replace(/<svg\b([^>]*)>/i, (_m, attrs: string) => {
      let next = attrs;
      if (!/\bcolor\s*=/.test(next)) next += ` color="${OBJECT_SVG_TINT}"`;
      if (!/\bfill\s*=/.test(next)) next += ` fill="${OBJECT_SVG_TINT}"`;
      if (!/\bstroke\s*=/.test(next)) next += ` stroke="${OBJECT_SVG_TINT}"`;
      return `<svg${next}>`;
    });
  }

  function getObjectImageSrc(data: any): string | null {
    const key = `${data?.id ?? ''}|${data?.equipmentTypeCode ?? ''}|${data?.iconData2d ?? ''}`;
    const cached = _imgSrcCache.get(key);
    if (cached) return cached;

    // 1) inline SVG 텍스트가 있으면 data URL 로 변환
    const svgText = getObjectSvg(data);
    if (svgText) {
      const dataUrl = `data:image/svg+xml;utf8,${encodeURIComponent(tintObjectSvg(svgText))}`;
      _imgSrcCache.set(key, dataUrl);
      return dataUrl;
    }

    // 2) 일반 이미지 URL
    const url = getObjectIconUrl(data);
    if (url) {
      _imgSrcCache.set(key, url);
      return url;
    }

    return null;
  }

  function getObjectIconUrl(data: any): string | null {
    if (getObjectSvg(data)) return null;
    if (data?.iconData2d) return data.iconData2d;
    return resolveTypeIconUrl(data?.equipmentTypeCode, null);
  }
  // ============================================
  // 스타일 함수 (정적 설비용)
  // ============================================

  function getStaticEquipmentStyle(obj: DashboardEquipmentData) {
    return styles.getStaticEquipmentStyle(obj);
  }

  function getEquipmentClass(obj: DashboardEquipmentData) {
    return styles.getEquipmentClass(obj, selectedEquipmentIds.value);
  }

  /**
   * 설비 타입별 툴팁 생성
   * LIFTER: ECS 작업 정보 표시
   * CONVEYOR: ECS 작업 정보 표시
   * RACK: 랙 타입, 위치, 재고, 주행전용 정보 표시
   * 기타: 기본 설비 코드 표시
   */
  function getEquipmentTooltip(obj: DashboardEquipmentData): string {
    if (obj.equipmentTypeCode === LayoutEquipmentType.LIFTER.code) {
      return styles.getLifterTooltip(obj);
    }
    if (obj.equipmentTypeCode === LayoutEquipmentType.CONVEYOR.code) {
      return styles.getConveyorTooltip(obj);
    }
    if (obj.equipmentTypeCode === LayoutEquipmentType.RACK.code) {
      return getRackTooltip(obj);
    }
    // 기본 툴팁
    return obj.equipmentCode || obj.id || '';
  }

  /**
   * 랙 설비 툴팁 생성
   * - 랙 타입 (셀, 입고포트, 출고포트, 입출고포트, 충전포트, 충전진입포트)
   * - 위치 정보 (Row-Bay-Level)
   * - 재고 정보
   * - 주행 전용 여부
   */
  function getRackTooltip(obj: DashboardEquipmentData): string {
    const lines: string[] = [];

    // 설비 코드
    lines.push(`랙: ${obj.equipmentCode || obj.realRackId || obj.id}`);

    // 랙 타입
    const rackType = obj.realRackType;
    if (rackType != null) {
      const typeLabel = enumLabel(RackType, rackType, `타입 ${rackType}`);
      lines.push(`타입: ${typeLabel}`);
    }

    // 위치 정보
    if (obj.realRackRow != null || obj.realRackBay != null || obj.realRackLevel != null) {
      const location = `R${obj.realRackRow ?? '-'}-B${obj.realRackBay ?? '-'}-L${
        obj.realRackLevel ?? '-'
      }`;
      lines.push(`위치: ${location}`);
    }

    // 주행 전용 여부
    if (obj.realRackDriveOnlyYn === true) {
      lines.push('🚗 주행 전용 (적재 불가)');
    }

    // 재고 정보 (CellStateService 기반)
    if (obj.realRackStateCode) {
      lines.push(`\n📦 상태: ${obj.realRackStateCode}`);
      if (obj.realRackItemCode) lines.push(`품목: ${obj.realRackItemCode}`);
      if (obj.realRackLotNo) lines.push(`Lot: ${obj.realRackLotNo}`);
      if (obj.realRackStorLoc) lines.push(`위치코드: ${obj.realRackStorLoc}`);
    }

    // 금지 축
    if (obj.realRackLocked) lines.push('🔒 사용금지 (LOCK)');
    if (obj.realRackInboundForbidden) lines.push('⊘ 입고 금지');
    if (obj.realRackOutboundForbidden) lines.push('⊘ 출고 금지');

    return lines.join('\n');
  }

  function getEquipmentBounds(obj: DashboardEquipmentData) {
    const left = Number(obj.posX ?? 0);
    const bottom = Number(obj.posY ?? 0);
    const width = Number(obj.width || obj.defaultWidth || 100);
    const height = Number(obj.height || obj.defaultHeight || 100);

    return {
      left,
      right: left + width,
      bottom,
      top: bottom + height,
    };
  }

  function isBoundsOverlap(
    a: ReturnType<typeof getEquipmentBounds>,
    b: ReturnType<typeof getEquipmentBounds>,
  ) {
    return a.left < b.right && a.right > b.left && a.bottom < b.top && a.top > b.bottom;
  }

  function findOverlappingEquipments(target: DashboardEquipmentData) {
    const targetBounds = getEquipmentBounds(target);

    return staticEquipments.value.filter((eq) => {
      const eqBounds = getEquipmentBounds(eq);
      return isBoundsOverlap(targetBounds, eqBounds);
    });
  }

  /**
   * 주어진 월드 좌표(x, y) 를 포함하는 모든 정적 설비를 반환.
   * 셔틀 클릭 시 셔틀이 가린 랙/컨베이어 등을 함께 띄울 때 사용한다.
   */
  function findEquipmentsAtPoint(x: number, y: number) {
    return staticEquipments.value.filter((eq) => {
      const b = getEquipmentBounds(eq);
      return x >= b.left && x <= b.right && y >= b.bottom && y <= b.top;
    });
  }

  // ============================================
  // 랙 재고 표시 함수 (TB_EQ_RACK_MST 기반)
  // ============================================

  /**
   * 랙 재고 축 state_code → 반투명 틴트 스타일.
   * 공용 유틸 getRackStateTintStyle 를 호출하고 입력 필드만 매핑.
   * CellState2D 와 동일한 파스텔 톤을 내기 위한 SSOT 경로.
   * 주행전용 셀(realRackDriveOnlyYn=true)은 유틸에서 null 반환 → 회색 빗금만 표현.
   */
  function getRackTintStyle(obj: DashboardEquipmentData): Record<string, string> | null {
    return getRackStateTintStyle({
      stateCode: obj.realRackStateCode,
      driveOnlyYn: obj.realRackDriveOnlyYn,
    });
  }

  /**
   * 랙에 물리 재고가 있는지 (인디케이터 표시 여부 판단용).
   * CellStateService 의 state_code 기반 — PRODUCT / EMPTY_BOX / OUTBOUND 는
   * 실제 화물이 있는 상태. INBOUND 는 작업중(화물 없음), EMPTY_OUT/DOUBLE_IN 은 이상 감지.
   */
  function hasRackInventory(obj: DashboardEquipmentData): boolean {
    return hasInventoryByStateCode(obj.realRackStateCode);
  }

  /** 유통기한 만료 여부 (expired_datetime < now). */
  function isExpired(expiredDatetime: string | null | undefined): boolean {
    if (!expiredDatetime) return false;
    const t = new Date(expiredDatetime).getTime();
    if (Number.isNaN(t)) return false;
    return t < Date.now();
  }

  /**
   * 랙 재고 툴팁 생성
   * @param obj 설비 데이터
   * @returns 툴팁 문자열
   */
  function getRackInventoryTooltip(obj: DashboardEquipmentData): string {
    const stateCode = obj.realRackStateCode || 'N/A';
    const itemCode = obj.realRackItemCode || '-';
    const lotNo = obj.realRackLotNo || '-';
    const location =
      obj.realRackStorLoc ||
      `R${obj.realRackRow ?? '-'}-B${obj.realRackBay ?? '-'}-L${obj.realRackLevel ?? '-'}`;
    return `상태: ${stateCode}\n품목: ${itemCode}\nLot: ${lotNo}\n위치: ${location}`;
  }

  // ============================================
  // 셔틀 유틸리티 함수
  // ============================================

  /**
   * 셔틀 툴팁 생성
   *
   * 셔틀 상태는 legend-spec 의 ShuttleState (DISABLED/ERROR/MANUAL/CHARGING/RUNNING/IDLE) 로만 표기.
   */
  function getShuttleTooltip(shuttle: any): string {
    const data = shuttle.data || {};
    const label = shuttleStateSpec(data.shuttleState)?.label ?? '알수없음';

    let tooltip = `셔틀: ${data.equipmentCode || shuttle.id}`;
    tooltip += `\n상태: ${label}`;

    if (data.batteryLevel != null) {
      tooltip += `\n배터리: ${data.batteryLevel}%`;
    }

    if (data.hasCargo) {
      tooltip += `\n📦 화물 적재중`;
    }

    if (data.currentJobKey) {
      tooltip += `\n작업: ${data.currentJobKey}`;
    }

    if (data.row != null && data.bay != null) {
      tooltip += `\n위치: R${data.row}-B${data.bay}`;
    }

    if (data.errorCode) {
      tooltip += `\n⚠️ 에러: ${data.errorCode}`;
      if (data.errorMessage) {
        tooltip += `\n${data.errorMessage}`;
      }
    }

    return tooltip;
  }

  /**
   * 배터리 레벨에 따른 CSS 클래스
   */
  function getBatteryClass(level: number): string {
    if (level <= 10) return 'battery-critical';
    if (level <= 20) return 'battery-low';
    if (level <= 50) return 'battery-medium';
    return 'battery-high';
  }

  // ============================================
  // 공통 에러 표시 유틸
  // ============================================

  /**
   * 설비 에러 여부 — 백엔드 정규화 상태(shuttleState/conveyorState === 'ERROR') 로 판정.
   * Lifter 는 정규화 범위 밖이라 errorCode 원시값 기반 유지.
   */
  function hasEquipmentError(equipment: any): boolean {
    if (!equipment) return false;
    const data = equipment?.data || equipment;

    // Shuttle: legend-spec 의 ShuttleState === 'ERROR'
    if (
      data?.equipmentTypeCode === LayoutEquipmentType.SHUTTLE.code ||
      equipment?.id === data?.equipmentId ||
      data?.cellId != null
    ) {
      return data?.shuttleState === 'ERROR';
    }

    // Conveyor: legend-spec 의 ConveyorState === 'ERROR' (rtConveyors 에서 정규화 필드 조회)
    if (data?.equipmentTypeCode === LayoutEquipmentType.CONVEYOR.code && data?.realEqId) {
      const cv = store.rtConveyors.find(
        (x) => x.eqId === data.realEqId || x.equipmentId === data.realEqId,
      );
      return cv?.conveyorState === 'ERROR';
    }

    // Lifter / 기타: errorCode 원시값 기반 (정규화 범위 밖)
    return isError(data?.errorCode) || isError(data?.errorId);
  }

  // ============================================
  // 센터 초기화 로직
  // ============================================

  function resolveInitialLcId(): string {

    return DEFAULT_LC_ID;
  }

  function openCenterModal() {
    showCenterModal.value = true;
  }

  function handleCenterModalClose() {
    // 센터 선택 필수
  }

  async function handleCenterSelect(nextLcId: string) {
    showCenterModal.value = false;
    await applyCenter(nextLcId);
  }

  function clearCenterScopedLocalStorageExceptLcId() {
    // eqGroup 제거
    localStorage.removeItem('TSPG_WORK_EQ_GROUP_ID');

    // page 관련 전부 제거
    const keysToRemove: string[] = [];

    for (let i = 0; i < localStorage.length; i += 1) {
      const key = localStorage.key(i);
      if (!key) continue;

      if (key.startsWith('TSPG_WORK_PAGE_')) {
        keysToRemove.push(key);
      }
    }

    keysToRemove.forEach((key) => localStorage.removeItem(key));
  }

  async function applyCenter(nextLcId: string) {
    const v = String(nextLcId ?? '').trim();
    if (!v) return;

    const prevLcId = String(localStorage.getItem('TSPG_WORK_LC_ID') ?? '').trim();
    const isCenterChanged = !!prevLcId && prevLcId !== v;

    // 센터가 실제로 바뀐 경우에만 eqGroup/page 관련 로컬값 제거
    if (isCenterChanged) {
      clearCenterScopedLocalStorageExceptLcId();
    }

    // lcId는 유지/갱신
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
      // 라우트에 :lcId가 없을 수 있음
    }

    lcIdRef.value = v;

    store.disconnectWebSocket();
    store.reset();
    clearShuttles();
    clearCargos();

    const savedEqGroupId = localStorage.getItem('TSPG_WORK_EQ_GROUP_ID') || '';

    if (savedEqGroupId) {
      await store.initializeWithEqGroup(v, savedEqGroupId);

      // 저장된 eqGroupId가 현재 센터에서 유효하지 않으면 첫 번째 그룹 선택
      if (!store.selectedEqGroupId && store.eqGroups.length > 0) {
        await store.selectEqGroup(store.eqGroups[0].id);
      }
    } else {
      await store.initializeLcOnly(v);

      // initializeLcOnly 이후에도 선택된 그룹이 없으면 첫 번째 그룹 선택
      if (!store.selectedEqGroupId && store.eqGroups.length > 0) {
        await store.selectEqGroup(store.eqGroups[0].id);
      }
    }

    if (store.selectedEqGroupId) {
      const savedPageId = localStorage.getItem(`TSPG_WORK_PAGE_${store.selectedEqGroupId}`);
      const groupId = store.selectedEqGroupId || '';
      const candidatePages = (store.pages || []).filter((p) => (p.eqGroupId || '') === groupId);

      if (savedPageId && candidatePages.some((p) => p.id === savedPageId)) {
        store.selectPage(savedPageId);
      } else if (candidatePages.length > 0) {
        store.selectPage(
          [...candidatePages].sort((a, b) => {
            const af = a.floorLevel ?? 0;
            const bf = b.floorLevel ?? 0;
            if (af !== bf) return af - bf;
            return (a.pageIndex ?? 0) - (b.pageIndex ?? 0);
          })[0].id,
        );
      }
    }

    if (store.activePageId) {
      await store.loadLayouts();
      await store.loadDashboardData();
      await store.connectWebSocket();
    }

    await checkEcsConnection();

    await nextTick();
    viewport.fitToPage();

    closeDetailPopup();
  }

  async function handleSelectEqGroup(eqGroupId: string) {
    if (!eqGroupId || eqGroupId === store.selectedEqGroupId) return;

    localStorage.setItem('TSPG_WORK_EQ_GROUP_ID', eqGroupId);

    await store.selectEqGroup(eqGroupId);

    const savedPageId = localStorage.getItem(`TSPG_WORK_PAGE_${eqGroupId}`);
    if (savedPageId && store.pages.some((p) => p.id === savedPageId)) {
      store.selectPage(savedPageId);
    } else if (store.pages.length > 0) {
      store.selectPage(sortedPages.value[0].id);
    }

    clearShuttles();
    clearCargos();
    await store.loadLayouts();
    await store.loadDashboardData();

    store.disconnectWebSocket();
    await store.connectWebSocket();

    await nextTick();
    viewport.fitToPage();
  }

  async function checkEcsConnection() {
    ecsReachable.value = true;
  }

  // ============================================
  // 페이지 선택
  // ============================================

  async function selectPage(pageId: string) {
    if (pageId === activePageId.value) return;

    closeDetailPopup();

    if (store.selectedEqGroupId) {
      localStorage.setItem(`TSPG_WORK_PAGE_${store.selectedEqGroupId}`, pageId);
    }

    clearShuttles();
    clearCargos();

    store.selectPage(pageId);
    await store.loadLayouts();
    await store.loadDashboardData();
    await store.loadDashboardInitialData();

    store.disconnectWebSocket();
    await store.connectWebSocket();

    await nextTick();
    viewport.fitToPage();
  }

  // ============================================
  // WebSocket 데이터 Watch
  // ============================================

  watch(
    () => store.shuttlePositions,
    (newPositions) => {
      if (newPositions && newPositions.length > 0) {
        const positions = newPositions
          .filter((p: any) => p && p.equipmentId && p.posX != null && p.posY != null)
          .map((p: any) => ({
            ...p,
            equipmentTypeCode: LayoutEquipmentType.SHUTTLE.code,
            currentPointCode: p.cellId,
          }));

        updateShuttlePositions(positions);
      } else {
        updateShuttlePositions([]);
      }
    },
    { deep: true, immediate: true },
  );

  watch(
    () => store.dashboardCargos,
    (newCargos) => {
      if (newCargos && newCargos.length > 0) {
        // 셔틀에 적재되지 않은 화물만 표시 (적재된 화물은 셔틀 아이콘에 표시)
        const visibleCargos = newCargos
          .filter(
            (c: any) => !c.carriedBy && !c.carriedByShuttleId && c.posX != null && c.posY != null,
          )
          .map((c: any) => ({
            cargoId: c.cargoId || c.id,
            posX: c.posX ?? 0,
            posY: c.posY ?? 0,
            barcode: c.barcode,
            cargoStatus: c.cargoStatus,
          }));
        updateCargoPositions(visibleCargos);
      } else {
        updateCargoPositions([]);
      }
    },
    { deep: true, immediate: true },
  );

  // ============================================
  // 키보드/마우스 이벤트
  // ============================================

  function onKeyDownGlobal(e: KeyboardEvent) {
    if (e.code === 'Space') {
      spaceDown.value = true;
      e.preventDefault();
    }
    if (e.key === 'Escape') {
      showSearchOverlay.value = false;
      closeDetailPopup();
    }
    // Ctrl+F: 검색 열기
    if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
      e.preventDefault();
      showSearchOverlay.value = true;
    }
  }

  function onKeyUpGlobal(e: KeyboardEvent) {
    if (e.code === 'Space') spaceDown.value = false;
  }

  function handleWheel(e: WheelEvent) {
    viewport.onWheel(e);
  }

  function onStageMouseDown(e: MouseEvent) {
    const allow = (spaceDown.value && e.button === 0) || e.button === 1;
    if (!allow) return;

    e.preventDefault();
    e.stopPropagation();

    viewport.startPan(e.clientX, e.clientY);

    window.addEventListener('mousemove', onStageMouseMove, { passive: false });
    window.addEventListener('mouseup', onStageMouseUp, { passive: false });
  }

  function onStageMouseMove(e: MouseEvent) {
    if (!viewport.isPanning.value) return;
    e.preventDefault();
    viewport.movePan(e.clientX, e.clientY);
  }

  function onStageMouseUp() {
    if (!viewport.isPanning.value) return;
    viewport.endPan();

    window.removeEventListener('mousemove', onStageMouseMove as any);
    window.removeEventListener('mouseup', onStageMouseUp as any);
  }

  // ============================================
  // 클릭 핸들러
  // ============================================

  function handleBackgroundClick() {
    if (viewport.shouldIgnoreClick()) return;
    closeDetailPopup();
  }

  function handleEquipmentClick(obj: DashboardEquipmentData) {
    if (viewport.shouldIgnoreClick()) return;
    selectOverlappedEquipments(obj);
  }

  /**
   * 리프터 오버레이 클릭 핸들러
   * 해당 리프터 설비의 상세 팝업을 열기
   */
  function handleLifterClick(lifter: LifterOverlay) {
    if (viewport.shouldIgnoreClick()) return;
    selectOverlappedEquipments(lifter.layout);
  }

  /**
   * interpolated shuttle → DashboardEquipmentData 형태로 합성.
   * EquipmentDetailPopup 의 "겹친 설비" 탭에 SHUTTLE 도 동일한 형식으로 노출하기 위함.
   */
  function synthesizeShuttleEntry(shuttle: any): DashboardEquipmentData | null {
    if (!shuttle) return null;
    const data = shuttle?.data || {};
    const id = String(shuttle?.id ?? data.equipmentId ?? '');
    if (!id) return null;
    const realEqId = String(data.equipmentId ?? id);
    const equipmentCode = String(data.equipmentCode ?? realEqId ?? id);

    return {
      id,
      realEqId,
      equipmentCode,
      equipmentTypeCode: LayoutEquipmentType.SHUTTLE.code,
      realEqType: 'CAR',
      posX: Number(shuttle?.posX ?? 0),
      posY: Number(shuttle?.posY ?? 0),
      width: shuttleSize.value,
      height: shuttleSize.value,
    } as unknown as DashboardEquipmentData;
  }

  function handleShuttleClick(shuttle: any) {
    if (viewport.shouldIgnoreClick()) return;

    // 컨베이어·랙 겹침 클릭과 동일한 동작 — SHUTTLE 도 동일한 EquipmentDetailPopup 의
    // 탭으로 묶어서 노출. (별도 ShuttleControlPopup 은 띄우지 않는다.)
    const px = Number(shuttle?.posX ?? shuttle?.data?.posX ?? NaN);
    const py = Number(shuttle?.posY ?? shuttle?.data?.posY ?? NaN);

    let overlapped: DashboardEquipmentData[] =
      Number.isFinite(px) && Number.isFinite(py) ? findEquipmentsAtPoint(px, py) : [];

    // 위치 기반 매칭 실패 시 cellId fallback
    if (overlapped.length === 0) {
      const shuttleData = shuttle?.data || {};
      const cellId = shuttleData.cellId || shuttleData.currentPointCode;
      if (cellId) {
        const base = store.dashboardData.find(
          (eq) => eq.realEqId === cellId || eq.id === cellId,
        );
        if (base) overlapped = findOverlappingEquipments(base);
      }
    }

    selectedShuttleEntry.value = synthesizeShuttleEntry(shuttle);
    selectedEquipmentIds.value = overlapped.map((x) => x.id);
    isDetailPopupVisible.value = true;
  }

  /**
   * 화물 클릭 → 설비 상세 팝업으로 통합
   * 화물이 위치한 설비(랙 등)의 상세 정보를 표시
   */
  function handleCargoClick(cargo: any) {
    if (viewport.shouldIgnoreClick()) return;

    const cargoData = cargo.data || cargo;
    const storedCellId = cargoData.storedCellId;

    if (storedCellId) {
      const rackEquipment = store.dashboardData.find(
        (eq) => eq.realEqId === storedCellId || eq.id === storedCellId,
      );

      if (rackEquipment) {
        selectOverlappedEquipments(rackEquipment);
        return;
      }
    }

    console.log('[handleCargoClick] 화물 위치 설비를 찾을 수 없음:', cargoData);
  }

  /**
   * 컨베이어 화물 클릭 → 해당 컨베이어 설비 상세로 표시
   */
  function handleConveyorCargoClick(item: ConveyorCargoItem) {
    if (viewport.shouldIgnoreClick()) return;
    selectOverlappedEquipments(item.layout);
  }

  function selectOverlappedEquipments(base: DashboardEquipmentData) {
    const overlapped = findOverlappingEquipments(base);
    selectedEquipmentIds.value = overlapped.length > 0 ? overlapped.map((x) => x.id) : [base.id];

    selectedShuttleEntry.value = null;
    isDetailPopupVisible.value = true;
  }

  function closeDetailPopup() {
    isDetailPopupVisible.value = false;
    selectedEquipmentIds.value = [];
    selectedShuttleEntry.value = null;
  }

  /**
   * 검색 결과 선택 핸들러
   * 해당 위치로 뷰포트 이동 및 해당 설비/셔틀 선택
   */
  function handleSearchSelect(item: SearchResultItem) {
    showSearchOverlay.value = false;

    // 뷰포트 이동
    if (item.posX != null && item.posY != null) {
      viewport.centerOnWorld(item.posX, item.posY);
    }

    // 타입별 선택 처리
    if (item.type === 'shuttle') {
      const shuttle = interpolatedShuttles.value.find(
        (s: any) => s.id === item.id || s.data?.equipmentId === item.id,
      );
      if (shuttle) {
        handleShuttleClick(shuttle);
      }
    } else if (item.type === 'equipment' || item.type === 'rack') {
      const equipment = store.dashboardData.find((eq) => eq.id === item.id);
      if (equipment) {
        selectOverlappedEquipments(equipment);
      }
    } else if (item.type === 'cargo') {
      const cargo = store.dashboardCargos.find((c: any) => c.cargoId === item.id);
      if (cargo && cargo.storedCellId) {
        const rack = store.dashboardData.find(
          (eq) => eq.realEqId === cargo.storedCellId || eq.id === cargo.storedCellId,
        );
        if (rack) {
          selectOverlappedEquipments(rack);
        }
      }
    }
  }

  // ============================================
  // 작업 관련
  // ============================================

  async function refreshJobs() {
    await store.loadActiveJobs();
  }

  // ============================================
  // 라이프사이클
  // ============================================

  let ecsInterval: ReturnType<typeof setInterval> | null = null;

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

    ecsInterval = setInterval(checkEcsConnection, 30000);
  });

  onUnmounted(() => {
    taskGrid.cleanup();
    store.disconnectWebSocket();
    store.reset();
    clearShuttles();
    clearCargos();

    window.removeEventListener('keydown', onKeyDownGlobal as any);
    window.removeEventListener('keyup', onKeyUpGlobal as any);

    if (ecsInterval) clearInterval(ecsInterval);
  });

  // activePage 변경 시 FIT
  watch(
    () => activePageId.value,
    async () => {
      await nextTick();
      viewport.fitToPage();
    },
  );

  // 작업패널 접힘 시 뷰포트 재계산
  watch(taskGrid.isCollapsed, async () => {
    await nextTick();
    viewport.recalculateFit({ keepWorldCenter: true });
  });

  // 배지 텍스트/클래스는 RackType 디스크립터(badgeText/badgeVariant)에서 lookup —
  // 별도의 switch 매핑 불필요.
  function getRackTypeBadgeText(obj: DashboardEquipmentData): string {
    return enumBadgeText(RackType, obj.realRackType);
  }

  function getRackTypeBadgeClass(obj: DashboardEquipmentData): string {
    return enumBadgeVariant(RackType, obj.realRackType);
  }

  // ============================================
  // 포트 운영 모드 (입고/출고/유휴) — 사용자가 자주 바꾸는 값이라 캔버스에 prominently 노출
  // 라벨/아이콘/색상은 모두 constants 의 PortMode* 매핑에서 가져온다 (단일 출처).
  // ============================================

  /** 입고/출고/입출고 포트 여부 */
  function isPortRack(obj: DashboardEquipmentData): boolean {
    const t = obj.realRackType;
    return (
      t === RackType.INBOUND_PORT.code ||
      t === RackType.OUTBOUND_PORT.code ||
      t === RackType.IN_OUTBOUND_PORT.code
    );
  }

  /** 모드 미설정 시 RackType 으로 추정 (단방향 포트는 타입대로) */
  function getPortMode(obj: DashboardEquipmentData): string | null {
    const direct = ((obj as any).wcsPortMode ?? (obj as any).portMode ?? null) as string | null;
    if (direct) return direct;
    if (obj.realRackType === RackType.INBOUND_PORT.code) return PortMode.INBOUND.code;
    if (obj.realRackType === RackType.OUTBOUND_PORT.code) return PortMode.OUTBOUND.code;
    if (obj.realRackType === RackType.IN_OUTBOUND_PORT.code) return PortMode.IDLE.code;
    return null;
  }

  // 모든 표시 속성은 PortMode 디스크립터에서 lookup — 매핑 객체 별도 필요 없음.
  function getPortModeBadgeText(obj: DashboardEquipmentData): string {
    return enumShortLabel(PortMode, getPortMode(obj));
  }

  function getPortModeIcon(obj: DashboardEquipmentData): string {
    return enumIcon(PortMode, getPortMode(obj));
  }

  function getPortModeBadgeClass(obj: DashboardEquipmentData): string {
    const variant = enumBadgeVariant(PortMode, getPortMode(obj));
    return variant ? `port-mode--${variant}` : '';
  }

  function getPortModeTooltip(obj: DashboardEquipmentData): string {
    const label = enumLabel(PortMode, getPortMode(obj), '-');
    if (obj.realRackType === RackType.IN_OUTBOUND_PORT.code) {
      return `입출고 겸용 포트 — 현재 모드: ${label}\n클릭해서 모드 변경`;
    }
    return `${label} 포트`;
  }

</script>

<style lang="scss">
  @import '../styles/dashboard.scss';
</style>
