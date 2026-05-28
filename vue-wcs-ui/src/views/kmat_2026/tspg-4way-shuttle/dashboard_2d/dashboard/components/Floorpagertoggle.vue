<!--
  FloorPagerToggle.vue
  층(페이지) 선택을 위한 페이지네이션 토글 컨트롤

  ============================================
  UI 구조
  ============================================
  [ ◀  3F ▼  ▶   3/20 ]
    │    │    │    └─ 현재 위치 / 총 페이지 수
    │    │    └────── 다음 층
    │    └─────────── 현재 층 (클릭 → 드롭다운)
    └──────────────── 이전 층

  ============================================
  Props
  ============================================
  - pages: 정렬된 페이지 목록
  - activePageId: 현재 선택된 페이지 ID
  - disabled: 전체 비활성화 (전환 중 등)

  ============================================
  Events
  ============================================
  - @select: 페이지 선택 시 pageId 전달
-->

<template>
  <div class="floor-pager" :class="{ disabled }" @click.stop>
    <!-- 이전 층 -->
    <button
      class="fp-btn fp-nav"
      :disabled="disabled || !canGoPrev"
      title="이전 층"
      @click="goPrev"
    >
      ◀
    </button>

    <!-- 현재 층 (클릭 → 드롭다운) -->
    <div class="fp-current-wrapper" ref="dropdownWrapperRef">
      <button
        class="fp-btn fp-current"
        :disabled="disabled || pages.length === 0"
        :title="currentLabel || '층을 선택하세요'"
        @click="toggleDropdown"
      >
        <span class="fp-current-label">{{ currentLabel || '-' }}</span>
        <span class="fp-caret" :class="{ open: dropdownOpen }">▼</span>
      </button>

      <!-- 드롭다운 메뉴 -->
      <ul v-show="dropdownOpen" class="fp-dropdown">
        <li
          v-for="(page, idx) in pages"
          :key="page.id"
          class="fp-dropdown-item"
          :class="{ active: page.id === activePageId }"
          @click="handleSelect(page.id)"
        >
          <span class="fp-dropdown-label">{{ floorLabel(page) }}</span>
          <span class="fp-dropdown-index">{{ idx + 1 }}</span>
        </li>
      </ul>
    </div>

    <!-- 다음 층 -->
    <button
      class="fp-btn fp-nav"
      :disabled="disabled || !canGoNext"
      title="다음 층"
      @click="goNext"
    >
      ▶
    </button>

    <!-- 현재 위치 / 총 개수 -->
    <span class="fp-count" v-if="pages.length > 0">
      {{ currentIndex + 1 }}/{{ pages.length }}
    </span>
    <span class="fp-count fp-count-empty" v-else> 0/0 </span>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, onMounted, onUnmounted } from 'vue';

  interface Page {
    id: string;
    pageName: string;
    eqGroupId?: string;
    floorLevel?: number;
    pageIndex?: number;
  }

  const props = defineProps<{
    pages: Page[];
    activePageId: string;
    disabled?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'select', pageId: string): void;
  }>();

  const dropdownOpen = ref(false);
  const dropdownWrapperRef = ref<HTMLElement | null>(null);

  // 현재 선택된 페이지의 인덱스
  const currentIndex = computed(() => {
    const idx = props.pages.findIndex((p) => p.id === props.activePageId);
    return idx >= 0 ? idx : 0;
  });

  const canGoPrev = computed(() => currentIndex.value > 0);
  const canGoNext = computed(() => currentIndex.value < props.pages.length - 1);

  // 현재 페이지의 표시 라벨 (예: "1F")
  const currentLabel = computed(() => {
    const page = props.pages[currentIndex.value];
    return page ? floorLabel(page) : '';
  });

  function floorLabel(page: Page): string {
    if (typeof page.floorLevel === 'number') {
      return `${page.floorLevel}F`;
    }
    return page.pageName || '-';
  }

  function goPrev() {
    if (props.disabled || !canGoPrev.value) return;
    const next = props.pages[currentIndex.value - 1];
    if (next) emit('select', next.id);
  }

  function goNext() {
    if (props.disabled || !canGoNext.value) return;
    const next = props.pages[currentIndex.value + 1];
    if (next) emit('select', next.id);
  }

  function toggleDropdown() {
    if (props.disabled || props.pages.length === 0) return;
    dropdownOpen.value = !dropdownOpen.value;
  }

  function handleSelect(pageId: string) {
    dropdownOpen.value = false;
    if (pageId !== props.activePageId) {
      emit('select', pageId);
    }
  }

  // 외부 클릭 시 드롭다운 닫기
  function onDocumentClick(e: MouseEvent) {
    if (!dropdownOpen.value) return;
    const target = e.target as Node;
    if (dropdownWrapperRef.value && !dropdownWrapperRef.value.contains(target)) {
      dropdownOpen.value = false;
    }
  }

  function onEsc(e: KeyboardEvent) {
    if (e.key === 'Escape' && dropdownOpen.value) dropdownOpen.value = false;
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
   컨테이너 — 전체를 하나의 박스로 묶음
   ============================================ */
  .floor-pager {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 3px 10px 3px 6px;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.15);
    border-radius: 8px;
    user-select: none;
    transition: opacity 0.2s;
  }

  .floor-pager.disabled {
    opacity: 0.5;
    pointer-events: none;
  }

  /* ============================================
   공통 버튼
   ============================================ */
  .fp-btn {
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

  .fp-btn:disabled {
    color: #555;
    cursor: not-allowed;
  }

  .fp-btn:hover:not(:disabled) {
    color: #409eff;
  }

  /* ============================================
   좌우 네비게이션 ◀ ▶
   ============================================ */
  .fp-nav {
    width: 22px;
    height: 22px;
    font-size: 10px;
    border-radius: 4px;
  }

  .fp-nav:hover:not(:disabled) {
    background: rgba(64, 158, 255, 0.15);
  }

  /* ============================================
   현재 층 버튼 (드롭다운 트리거)
   ============================================ */
  .fp-current-wrapper {
    position: relative;
  }

  .fp-current {
    min-width: 54px;
    height: 24px;
    padding: 0 8px;
    font-size: 13px;
    font-weight: 600;
    color: #e5eaf3;
    border-radius: 4px;
    gap: 6px;
  }

  .fp-current:hover:not(:disabled) {
    background: rgba(64, 158, 255, 0.15);
    color: #409eff;
  }

  .fp-current-label {
    line-height: 1;
  }

  .fp-caret {
    font-size: 8px;
    color: #909399;
    transition: transform 0.2s;
  }

  .fp-caret.open {
    transform: rotate(180deg);
    color: #409eff;
  }

  /* ============================================
   드롭다운
   ============================================ */
  .fp-dropdown {
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

  .fp-dropdown::-webkit-scrollbar {
    width: 6px;
  }
  .fp-dropdown::-webkit-scrollbar-track {
    background: transparent;
  }
  .fp-dropdown::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.2);
    border-radius: 999px;
  }

  .fp-dropdown-item {
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

  .fp-dropdown-item:hover {
    background: rgba(64, 158, 255, 0.15);
    color: #e5eaf3;
  }

  .fp-dropdown-item.active {
    background: rgba(64, 158, 255, 0.25);
    color: #409eff;
    font-weight: 600;
  }

  .fp-dropdown-label {
    flex: 1;
  }

  .fp-dropdown-index {
    font-size: 10px;
    color: #606266;
    font-family: 'Roboto Mono', Consolas, monospace;
  }

  /* ============================================
   현재 위치 / 총 개수 표시
   ============================================ */
  .fp-count {
    font-size: 12px;
    color: #909399;
    font-family: 'Roboto Mono', Consolas, monospace;
    padding-left: 4px;
    border-left: 1px solid rgba(255, 255, 255, 0.1);
    margin-left: 2px;
    line-height: 1;
  }

  .fp-count-empty {
    color: #606266;
  }
</style>
