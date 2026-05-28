<!--
  DashboardSearchBar.vue
  대시보드 검색 오버레이 컴포넌트

  ============================================
  기능 설명
  ============================================
  - 설비 코드, 셔틀 코드, 바코드, 위치 검색
  - 실시간 검색 결과 표시
  - 결과 선택 시 해당 위치로 맵 이동
  - 키보드 네비게이션 지원 (↑↓ Enter Esc)

  ============================================
  Props
  ============================================
  - equipments: 설비 데이터 배열
  - shuttles: 셔틀 데이터 배열
  - cargos: 화물 데이터 배열

  ============================================
  Events
  ============================================
  - @select: 검색 결과 선택 시
  - @close: 검색 닫기
-->

<template>
  <div class="search-overlay" @click.self="$emit('close')" @keydown="handleKeyDown">
    <div class="search-container">
      <!-- 검색 입력 -->
      <div class="search-input-wrapper">
        <span class="search-icon">🔍</span>
        <input
          ref="inputRef"
          v-model="query"
          type="text"
          class="search-input"
          placeholder="설비코드, 셔틀코드, 바코드, 위치 검색..."
          autofocus
          @keydown.esc="$emit('close')"
          @keydown.enter="selectHighlighted"
          @keydown.up.prevent="moveHighlight(-1)"
          @keydown.down.prevent="moveHighlight(1)"
        />
        <button v-if="query" class="clear-btn" @click="clearQuery">✕</button>
        <span class="shortcut-hint">ESC로 닫기</span>
      </div>

      <!-- 검색 결과 -->
      <div v-if="hasQuery" class="search-results">
        <!-- 결과 있음 -->
        <template v-if="results.length > 0">
          <div class="results-header">
            <span class="results-count">{{ results.length }}건 검색됨</span>
            <span class="results-hint">↑↓로 이동, Enter로 선택</span>
          </div>
          <div class="results-list" ref="resultsListRef">
            <div
              v-for="(item, index) in results"
              :key="`${item.type}-${item.id}`"
              class="result-item"
              :class="{
                highlighted: index === highlightedIndex,
                [item.type]: true,
              }"
              @click="selectItem(item)"
              @mouseenter="highlightedIndex = index"
            >
              <div class="result-icon" :class="item.type">
                {{ getTypeIcon(item.type) }}
              </div>
              <div class="result-content">
                <div class="result-code">{{ item.code }}</div>
                <div class="result-subtext">{{ item.subText }}</div>
              </div>
              <div class="result-type-badge" :class="item.type">
                {{ getTypeLabel(item.type) }}
              </div>
            </div>
          </div>
        </template>

        <!-- 결과 없음 -->
        <div v-else class="no-results">
          <span class="no-results-icon">📭</span>
          <span class="no-results-text">"{{ query }}"에 대한 검색 결과가 없습니다</span>
        </div>
      </div>

      <!-- 검색어 없을 때 안내 -->
      <div v-else class="search-guide">
        <div class="guide-title">검색 도움말</div>
        <div class="guide-items">
          <div class="guide-item">
            <span class="guide-icon">🚗</span>
            <span class="guide-text">셔틀 코드 (예: S-001, CAR-A01)</span>
          </div>
          <div class="guide-item">
            <span class="guide-icon">📦</span>
            <span class="guide-text">화물 바코드 (예: BC12345678)</span>
          </div>
          <div class="guide-item">
            <span class="guide-icon">⚙️</span>
            <span class="guide-text">설비 코드 (예: CV-001, LF-A1)</span>
          </div>
          <div class="guide-item">
            <span class="guide-icon">📍</span>
            <span class="guide-text">랙 위치 (예: R01-B02-L03)</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue';
import type { DashboardEquipmentData, DashboardShuttleData, DashboardCargoData } from '../../api/types';
import { useDashboardSearch, type SearchResultItem } from '../../composables/useDashboardSearch';

// ============================================
// Props 정의
// ============================================

const props = defineProps<{
  equipments: DashboardEquipmentData[];
  shuttles: DashboardShuttleData[];
  cargos: DashboardCargoData[];
}>();

// ============================================
// Emits 정의
// ============================================

const emit = defineEmits<{
  (e: 'select', item: SearchResultItem): void;
  (e: 'close'): void;
}>();

// ============================================
// Refs
// ============================================

const inputRef = ref<HTMLInputElement | null>(null);
const resultsListRef = ref<HTMLElement | null>(null);
const highlightedIndex = ref(0);

// ============================================
// 검색 Composable
// ============================================

const equipmentsRef = computed(() => props.equipments || []);
const shuttlesRef = computed(() => props.shuttles || []);
const cargosRef = computed(() => props.cargos || []);

const search = useDashboardSearch(
  equipmentsRef as any,
  shuttlesRef as any,
  cargosRef as any,
);

const query = ref('');
const results = computed(() => {
  search.setQuery(query.value);
  return search.results.value;
});
const hasQuery = computed(() => query.value.trim().length > 0);

// ============================================
// 함수
// ============================================

function getTypeIcon(type: string): string {
  switch (type) {
    case 'shuttle':
      return '🚗';
    case 'cargo':
      return '📦';
    case 'equipment':
      return '⚙️';
    case 'rack':
      return '📍';
    default:
      return '📋';
  }
}

function getTypeLabel(type: string): string {
  switch (type) {
    case 'shuttle':
      return '셔틀';
    case 'cargo':
      return '화물';
    case 'equipment':
      return '설비';
    case 'rack':
      return '랙';
    default:
      return type;
  }
}

function clearQuery(): void {
  query.value = '';
  highlightedIndex.value = 0;
  inputRef.value?.focus();
}

function selectItem(item: SearchResultItem): void {
  emit('select', item);
}

function selectHighlighted(): void {
  if (results.value.length > 0 && highlightedIndex.value < results.value.length) {
    selectItem(results.value[highlightedIndex.value]);
  }
}

function moveHighlight(direction: number): void {
  const len = results.value.length;
  if (len === 0) return;

  highlightedIndex.value = (highlightedIndex.value + direction + len) % len;

  // 스크롤 위치 조정
  nextTick(() => {
    const list = resultsListRef.value;
    const items = list?.querySelectorAll('.result-item');
    if (list && items && items[highlightedIndex.value]) {
      const item = items[highlightedIndex.value] as HTMLElement;
      const listRect = list.getBoundingClientRect();
      const itemRect = item.getBoundingClientRect();

      if (itemRect.bottom > listRect.bottom) {
        item.scrollIntoView({ block: 'end', behavior: 'smooth' });
      } else if (itemRect.top < listRect.top) {
        item.scrollIntoView({ block: 'start', behavior: 'smooth' });
      }
    }
  });
}

function handleKeyDown(e: KeyboardEvent): void {
  // 글로벌 키 핸들링 (필요 시)
}

// ============================================
// Watchers
// ============================================

// 검색어 변경 시 하이라이트 초기화
watch(query, () => {
  highlightedIndex.value = 0;
});

// ============================================
// Lifecycle
// ============================================

onMounted(() => {
  nextTick(() => {
    inputRef.value?.focus();
  });
});
</script>

<style scoped>
/* ============================================
   오버레이
   ============================================ */
.search-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  padding-top: 120px;
  animation: fade-in 0.15s ease;
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* ============================================
   검색 컨테이너
   ============================================ */
.search-container {
  width: 100%;
  max-width: 560px;
  background: rgba(26, 28, 35, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  overflow: hidden;
  animation: slide-down 0.2s ease;
  max-height: calc(100vh - 180px);
  display: flex;
  flex-direction: column;
}

@keyframes slide-down {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ============================================
   검색 입력
   ============================================ */
.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.search-icon {
  font-size: 18px;
  opacity: 0.6;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 16px;
  color: #e5eaf3;
}

.search-input::placeholder {
  color: #606266;
}

.clear-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 50%;
  color: #909399;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.clear-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #c0c4cc;
}

.shortcut-hint {
  font-size: 11px;
  color: #606266;
  white-space: nowrap;
}

/* ============================================
   검색 결과
   ============================================ */
.search-results {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.results-count {
  font-size: 12px;
  color: #909399;
}

.results-hint {
  font-size: 11px;
  color: #606266;
}

.results-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

/* ============================================
   결과 아이템
   ============================================ */
.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.result-item:hover,
.result-item.highlighted {
  background: rgba(64, 158, 255, 0.12);
}

.result-item.highlighted {
  box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.3);
}

.result-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 16px;
  flex-shrink: 0;
}

.result-icon.shuttle {
  background: rgba(64, 158, 255, 0.15);
}

.result-icon.cargo {
  background: rgba(240, 193, 75, 0.15);
}

.result-icon.equipment {
  background: rgba(103, 194, 58, 0.15);
}

.result-icon.rack {
  background: rgba(155, 89, 182, 0.15);
}

.result-content {
  flex: 1;
  min-width: 0;
}

.result-code {
  font-size: 14px;
  font-weight: 600;
  color: #e5eaf3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-subtext {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-type-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 4px;
  flex-shrink: 0;
}

.result-type-badge.shuttle {
  background: rgba(64, 158, 255, 0.2);
  color: #409eff;
}

.result-type-badge.cargo {
  background: rgba(240, 193, 75, 0.2);
  color: #f0c14b;
}

.result-type-badge.equipment {
  background: rgba(103, 194, 58, 0.2);
  color: #67c23a;
}

.result-type-badge.rack {
  background: rgba(155, 89, 182, 0.2);
  color: #9b59b6;
}

/* ============================================
   결과 없음
   ============================================ */
.no-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  gap: 12px;
}

.no-results-icon {
  font-size: 36px;
  opacity: 0.5;
}

.no-results-text {
  font-size: 14px;
  color: #909399;
  text-align: center;
}

/* ============================================
   검색 가이드
   ============================================ */
.search-guide {
  padding: 20px;
}

.guide-title {
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 16px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.guide-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.guide-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.guide-icon {
  font-size: 16px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
}

.guide-text {
  font-size: 13px;
  color: #c0c4cc;
}

/* ============================================
   스크롤바
   ============================================ */
.results-list::-webkit-scrollbar {
  width: 6px;
}

.results-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 3px;
}

.results-list::-webkit-scrollbar-track {
  background: transparent;
}
</style>
