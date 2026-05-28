<!--
  DashboardHeader.vue
  대시보드 상단 헤더 컴포넌트

  ============================================
  기능 설명
  ============================================
  - 설비그룹 탭: 물류센터 내 설비그룹 선택
  - 페이지(층) 선택기: 설비그룹의 층 선택 UI (2가지 모드)
      mode='tabs'  : 탭 버튼 나열 (Dashboard2D 기본)
      mode='pager' : [◀  3F ▼  ▶  3/20] 페이지네이션 토글 (CellState2D)
  - 연결 상태: WebSocket 실시간 연결 상태 표시
  - ECS 상태: 설비 제어 시스템 연결 상태 표시
  - 센터 뱃지: 현재 센터 ID 표시 및 변경 버튼

  ============================================
  Props 설명
  ============================================
  - eqGroups:           설비그룹 목록
  - selectedEqGroupId:  현재 선택된 설비그룹 ID
  - pages:              페이지(층) 목록
  - activePageId:       현재 선택된 페이지 ID
  - isConnected:        WebSocket 연결 상태
  - lastError:          마지막 오류 메시지
  - ecsReachable:       ECS 연결 상태
  - lcId:               현재 센터 ID
  - pageSelectorMode:   'tabs' (기본) | 'pager'
  - pageSelectorDisabled: 페이지 선택기 비활성화 (전환 중 등)

  ============================================
  Events 설명
  ============================================
  - @select-eq-group:   설비그룹 선택 시
  - @select-page:       페이지 선택 시
  - @open-center-modal: 센터 변경 버튼 클릭 시
-->

<template>
  <header class="dashboard-header">
    <!-- ========================================
         설비그룹 탭 영역
         - 가로 스크롤 가능한 칩 형태 버튼
         - 선택된 그룹은 파란색 배경
    ======================================== -->
    <div v-if="eqGroups.length > 0" class="eqgroup-tabs">
      <button
        v-for="group in eqGroups"
        :key="group.id"
        class="eqgroup-chip"
        :class="{ active: group.id === selectedEqGroupId }"
        :title="`설비그룹: ${group.name}`"
        @click.stop="$emit('select-eq-group', group.id)"
      >
        {{ group.name }}
      </button>
    </div>

    <!-- ========================================
         페이지(층) 선택기 - 모드별 분기
    ======================================== -->

    <!-- [mode='tabs'] 기본 탭 버튼 (Dashboard2D) -->
    <div
      v-if="pageSelectorMode === 'tabs' && sortedPages.length > 0"
      class="page-tabs"
    >
      <button
        v-for="page in sortedPages"
        :key="page.id"
        class="page-tab"
        :class="{ active: activePageId === page.id }"
        :title="`페이지: ${page.pageName}`"
        @click.stop="$emit('select-page', page.id)"
      >
        {{ floorLabel(page) }}
      </button>
    </div>

    <!-- [mode='pager'] 페이지네이션 토글 (CellState2D) -->
    <div
      v-else-if="pageSelectorMode === 'pager' && sortedPages.length > 0"
      class="page-pager-wrapper"
      :class="{ disabled: pageSelectorDisabled }"
      @click.stop
    >
      <!-- 이전 층 -->
      <button
        class="pager-btn pager-nav"
        :disabled="pageSelectorDisabled || !canGoPrev"
        title="이전 층"
        @click="goPrev"
      >
        ◀
      </button>

      <!-- 현재 층 (클릭 → 드롭다운) -->
      <div class="pager-current-wrapper" ref="pagerDropdownRef">
        <button
          class="pager-btn pager-current"
          :disabled="pageSelectorDisabled"
          :title="currentLabel || '층을 선택하세요'"
          @click="togglePagerDropdown"
        >
          <span class="pager-current-label">{{ currentLabel || '-' }}</span>
          <span class="pager-caret" :class="{ open: pagerDropdownOpen }">▼</span>
        </button>

        <!-- 드롭다운 메뉴 -->
        <ul v-show="pagerDropdownOpen" class="pager-dropdown">
          <li
            v-for="(page, idx) in sortedPages"
            :key="page.id"
            class="pager-dropdown-item"
            :class="{ active: page.id === activePageId }"
            @click="handlePagerSelect(page.id)"
          >
            <span class="pager-dropdown-label">{{ floorLabel(page) }}</span>
            <span class="pager-dropdown-index">{{ idx + 1 }}</span>
          </li>
        </ul>
      </div>

      <!-- 다음 층 -->
      <button
        class="pager-btn pager-nav"
        :disabled="pageSelectorDisabled || !canGoNext"
        title="다음 층"
        @click="goNext"
      >
        ▶
      </button>

      <!-- 현재 위치 / 총 개수 -->
      <span class="pager-count">
        {{ currentIndex + 1 }}/{{ sortedPages.length }}
      </span>
    </div>

    <!-- 중앙 여백 (spacer) -->
    <div class="header-spacer"></div>

    <!-- ========================================
         WebSocket 연결 상태 표시
    ======================================== -->
    <div
      class="connection-status"
      :class="{
        connected: isConnected,
        error: !!lastError,
      }"
    >
      <span v-if="lastError" :title="lastError">{{ lastError }}</span>
      <span v-else>{{ isConnected ? '실시간 연결됨' : '연결 중...' }}</span>
    </div>

    <!-- ========================================
         ECS 연결 상태 표시
    ======================================== -->
    <div class="ecs-status" :class="{ reachable: ecsReachable }">
      ECS: {{ ecsReachable ? '연결됨' : '연결 안됨' }}
    </div>

    <!-- [wcs-ops Step 14] 운영 모드 배지 + 기능 플래그 도트 -->
    <OperationModeBadge
      :eq-group-id="selectedEqGroupId || null"
      @click="can('update') ? $emit('open-ops-console') : null"
      :style="can('update') ? '' : 'cursor: default;'"
    />

    <!-- ========================================
         센터 ID 뱃지 및 변경 버튼
    ======================================== -->
    <div
      class="lc-badge"
      :title="can('update') ? '클릭하여 센터 변경' : ''"
      @click.stop="can('update') ? $emit('open-center-modal') : null"
    >
      {{ lcId }}
      <span v-if="can('update')" class="lc-badge-edit">센터 변경</span>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue';
import OperationModeBadge from './OperationModeBadge.vue';
import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';

const MENU = 'Dashboard2D';
const { can } = usePermissionLocal(MENU);

// ============================================
// 타입 정의
// ============================================

interface EqGroup {
  id: string;
  name: string;
}

interface Page {
  id: string;
  pageName: string;
  eqGroupId?: string;
  floorLevel?: number;
  pageIndex?: number;
}

type PageSelectorMode = 'tabs' | 'pager';

// ============================================
// Props 정의
// ============================================

const props = withDefaults(
  defineProps<{
    eqGroups: EqGroup[];
    selectedEqGroupId: string;
    pages: Page[];
    activePageId: string;
    isConnected: boolean;
    lastError: string | null;
    ecsReachable: boolean;
    lcId: string;
    /** 페이지 선택기 UI 모드 (기본: 'tabs') */
    pageSelectorMode?: PageSelectorMode;
    /** 페이지 선택기 비활성화 (전환 중 등) */
    pageSelectorDisabled?: boolean;
  }>(),
  {
    pageSelectorMode: 'tabs',
    pageSelectorDisabled: false,
  },
);

// ============================================
// Events 정의
// ============================================

const emit = defineEmits<{
  (e: 'select-eq-group', groupId: string): void;
  (e: 'select-page', pageId: string): void;
  (e: 'open-center-modal'): void;
  /** [wcs-ops Step 14] 운영 모드 배지 클릭 → 운영 콘솔 라우팅 (Step 18) */
  (e: 'open-ops-console'): void;
}>();

// ============================================
// 계산된 속성 (Computed)
// ============================================

/**
 * 선택된 설비그룹에 속한 페이지만 필터링하고 정렬합니다.
 *   1. floorLevel (층 번호) 오름차순
 *   2. pageIndex (페이지 순서) 오름차순
 */
const sortedPages = computed(() => {
  const currentGroupId = props.selectedEqGroupId || '';
  const filtered = (props.pages || []).filter((p) => (p.eqGroupId || '') === currentGroupId);
  return [...filtered].sort((a, b) => {
    const af = a.floorLevel ?? 0;
    const bf = b.floorLevel ?? 0;
    if (af !== bf) return af - bf;
    return (a.pageIndex ?? 0) - (b.pageIndex ?? 0);
  });
});

// ============================================
// 페이지 라벨 함수
// ============================================

/**
 * 페이지의 표시 라벨.
 *   - tabs 모드: "N층"
 *   - pager 모드: "NF" (짧은 영문 표기가 공간 절약)
 */
function floorLabel(page: Page): string {
  if (typeof page.floorLevel === 'number') {
    return props.pageSelectorMode === 'pager'
      ? `${page.floorLevel}F`
      : `${page.floorLevel}층`;
  }
  return page.pageName;
}

// ============================================
// Pager 모드 전용 로직
// ============================================

const pagerDropdownOpen = ref(false);
const pagerDropdownRef = ref<HTMLElement | null>(null);

const currentIndex = computed(() => {
  const idx = sortedPages.value.findIndex((p) => p.id === props.activePageId);
  return idx >= 0 ? idx : 0;
});

const canGoPrev = computed(() => currentIndex.value > 0);
const canGoNext = computed(() => currentIndex.value < sortedPages.value.length - 1);

const currentLabel = computed(() => {
  const page = sortedPages.value[currentIndex.value];
  return page ? floorLabel(page) : '';
});

function goPrev() {
  if (props.pageSelectorDisabled || !canGoPrev.value) return;
  const target = sortedPages.value[currentIndex.value - 1];
  if (target) emit('select-page', target.id);
}

function goNext() {
  if (props.pageSelectorDisabled || !canGoNext.value) return;
  const target = sortedPages.value[currentIndex.value + 1];
  if (target) emit('select-page', target.id);
}

function togglePagerDropdown() {
  if (props.pageSelectorDisabled || sortedPages.value.length === 0) return;
  pagerDropdownOpen.value = !pagerDropdownOpen.value;
}

function handlePagerSelect(pageId: string) {
  pagerDropdownOpen.value = false;
  if (pageId !== props.activePageId) {
    emit('select-page', pageId);
  }
}

// 외부 클릭 / ESC 시 드롭다운 닫기
function onDocumentClick(e: MouseEvent) {
  if (!pagerDropdownOpen.value) return;
  const target = e.target as Node;
  if (pagerDropdownRef.value && !pagerDropdownRef.value.contains(target)) {
    pagerDropdownOpen.value = false;
  }
}

function onEsc(e: KeyboardEvent) {
  if (e.key === 'Escape' && pagerDropdownOpen.value) {
    pagerDropdownOpen.value = false;
  }
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick);
  document.addEventListener('keydown', onEsc);
});

onUnmounted(() => {
  document.removeEventListener('click', onDocumentClick);
  document.removeEventListener('keydown', onEsc);
});
</script>

<style scoped>
/* ============================================
 헤더 컨테이너
 ============================================ */
.dashboard-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: rgba(30, 34, 45, 0.95);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding: 0 16px;
  height: 54px;
}

/* ============================================
 설비그룹 탭
 ============================================ */

.eqgroup-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 35%;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  padding: 4px 0;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.3) transparent;
}

.eqgroup-tabs::-webkit-scrollbar { height: 4px; }
.eqgroup-tabs::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.3); border-radius: 2px; }
.eqgroup-tabs::-webkit-scrollbar-track { background: transparent; }

.eqgroup-chip {
  flex: 0 0 auto;
  height: 28px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.06);
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
  color: #909399;
  transition: all 0.2s;
}
.eqgroup-chip:hover { background: rgba(255, 255, 255, 0.12); color: #c0c4cc; }
.eqgroup-chip.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

/* ============================================
 페이지 탭 (mode='tabs')
 ============================================ */

.page-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  max-width: 40%;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  padding: 4px 0 4px 12px;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.3) transparent;
}

.page-tabs::-webkit-scrollbar { height: 4px; }
.page-tabs::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.3); border-radius: 2px; }
.page-tabs::-webkit-scrollbar-track { background: transparent; }

.page-tab {
  flex: 0 0 auto;
  padding: 6px 14px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #909399;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  transition: all 0.2s;
}
.page-tab:hover {
  color: #c0c4cc;
  background-color: rgba(255, 255, 255, 0.06);
}
.page-tab.active {
  color: #409eff;
  background: rgba(64, 158, 255, 0.15);
  border-color: rgba(64, 158, 255, 0.4);
}

/* ============================================
 페이지 토글 (mode='pager')
 ============================================ */

.page-pager-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px 3px 6px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  margin-left: 4px;
  transition: opacity 0.2s;
  user-select: none;
}

.page-pager-wrapper.disabled {
  opacity: 0.5;
  pointer-events: none;
}

.pager-btn {
  background: transparent;
  border: none;
  color: #c0c4cc;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s, background 0.15s;
}

.pager-btn:disabled {
  color: #555;
  cursor: not-allowed;
}

.pager-btn:hover:not(:disabled) { color: #409eff; }

/* ◀ ▶ 네비게이션 */
.pager-nav {
  width: 22px;
  height: 22px;
  font-size: 10px;
  border-radius: 4px;
}
.pager-nav:hover:not(:disabled) { background: rgba(64, 158, 255, 0.15); }

/* 현재 층 (드롭다운 트리거) */
.pager-current-wrapper {
  position: relative;
}

.pager-current {
  min-width: 54px;
  height: 24px;
  padding: 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #e5eaf3;
  border-radius: 4px;
  gap: 6px;
}
.pager-current:hover:not(:disabled) {
  background: rgba(64, 158, 255, 0.15);
  color: #409eff;
}

.pager-current-label { line-height: 1; }

.pager-caret {
  font-size: 8px;
  color: #909399;
  transition: transform 0.2s;
}
.pager-caret.open {
  transform: rotate(180deg);
  color: #409eff;
}

/* 드롭다운 */
.pager-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  list-style: none;
  margin: 0;
  padding: 4px;
  min-width: 100px;
  max-height: 280px;
  overflow-y: auto;
  background: rgba(30, 34, 45, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
}

.pager-dropdown::-webkit-scrollbar { width: 6px; }
.pager-dropdown::-webkit-scrollbar-track { background: transparent; }
.pager-dropdown::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 999px;
}

.pager-dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  font-size: 12px;
  color: #c0c4cc;
  cursor: pointer;
  border-radius: 4px;
  gap: 10px;
  transition: background 0.15s, color 0.15s;
}
.pager-dropdown-item:hover {
  background: rgba(64, 158, 255, 0.15);
  color: #e5eaf3;
}
.pager-dropdown-item.active {
  background: rgba(64, 158, 255, 0.25);
  color: #409eff;
  font-weight: 600;
}

.pager-dropdown-label { flex: 1; }

.pager-dropdown-index {
  font-size: 10px;
  color: #606266;
  font-family: 'Roboto Mono', Consolas, monospace;
}

/* 현재 위치 / 총 개수 */
.pager-count {
  font-size: 12px;
  color: #909399;
  font-family: 'Roboto Mono', Consolas, monospace;
  padding-left: 6px;
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  margin-left: 2px;
  line-height: 1;
}

/* ============================================
 중앙 여백
 ============================================ */
.header-spacer {
  flex: 1;
  min-width: 20px;
}

/* ============================================
 연결 상태 표시
 ============================================ */
.connection-status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  background-color: rgba(245, 108, 108, 0.2);
  color: #f56c6c;
}
.connection-status.connected {
  background-color: rgba(103, 194, 58, 0.2);
  color: #67c23a;
}
.connection-status.error {
  background-color: rgba(230, 162, 60, 0.2);
  color: #e6a23c;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ecs-status {
  padding: 4px 10px;
  border-radius: 10px;
  font-size: 11px;
  background-color: rgba(245, 108, 108, 0.2);
  color: #f56c6c;
}
.ecs-status.reachable {
  background-color: rgba(103, 194, 58, 0.2);
  color: #67c23a;
}

/* ============================================
 센터 뱃지
 ============================================ */
.lc-badge {
  padding: 6px 12px;
  border-radius: 10px;
  font-size: 12px;
  color: #e5eaf3;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  user-select: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.lc-badge-edit {
  font-size: 11px;
  color: #409eff;
  opacity: 0.9;
}
.lc-badge:hover { background: rgba(255, 255, 255, 0.1); }
</style>
