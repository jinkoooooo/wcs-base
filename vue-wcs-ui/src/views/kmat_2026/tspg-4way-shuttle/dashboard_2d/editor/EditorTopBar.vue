<template>
  <div class="editor-topbar" ref="topbarRef">
    <div class="topbar-left">
      <!-- ✅ 페이지 eqGroupId 기준으로 그룹 탭 표시 -->
      <div class="row scroll-x eqgroup-tabs">
        <button
          v-for="group in pageGroups"
          :key="group.eqGroupId || '__none__'"
          class="chip"
          :class="{
            active: group.eqGroupId === activeEqGroupId || (!group.eqGroupId && !activeEqGroupId),
          }"
          @click="$emit('select-eq-group', group.eqGroupId || '')"
          type="button"
        >
          {{ group.displayName }}
          <span class="group-count">({{ group.pages.length }})</span>
          <!-- 실제 eqGroupId가 있는 그룹에만 삭제 버튼 표시 (이름 없음 그룹은 제외) -->
          <span
            v-if="group.eqGroupId"
            class="group-x"
            title="이 그룹과 모든 페이지/설비 삭제"
            @click.stop="$emit('delete-eq-group', group.eqGroupId)"
            >×</span
          >
        </button>
      </div>

      <div class="row page-row">
        <div ref="pageTabsRef" class="scroll-x page-tabs">
          <button
            v-for="page in sortedPages"
            :key="page.id"
            class="tab"
            :class="{ active: page.id === activePageId }"
            @click="selectPage(page.id)"
            type="button"
          >
            <span class="tab-label" v-if="!isEditing(page.id)" @dblclick.stop="startEdit(page.id)">
              {{ floorLabel(page) }}
            </span>

            <input
              v-else
              ref="editInputRef"
              v-model="editingName"
              type="text"
              class="edit-input"
              @click.stop
              @blur="finishEdit(page.id)"
              @keyup.enter="finishEdit(page.id)"
              @keyup.escape="cancelEdit"
            />

            <span
              v-if="sortedPages.length > 1 && page.id === activePageId"
              class="remove-x"
              @click.stop="removePage(page.id)"
              title="층 삭제"
            >
              ×
            </span>
          </button>

          <!-- 페이지 추가 버튼 -->
          <div class="tab-plus-wrap">
            <button class="add-page-btn" @click.stop="onAddFloor" title="새 층 추가" type="button">
              + 층 추가
            </button>
            <button
              ref="plusBtnRef"
              class="menu-toggle-btn"
              @click.stop="toggleMenu"
              title="추가 옵션"
              type="button"
            >
              <span class="caret">▾</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="toolbar-actions">
      <!-- 현재 모드 배지 -->
      <div class="mode-badge" :class="isDefaultMode ? 'badge-master' : 'badge-center'">
        <span v-if="isDefaultMode">시스템 마스터</span>
        <span v-else>센터: {{ lcId }}</span>
      </div>

      <!-- 센터 변경 버튼 -->
      <div class="center-selector-wrap" ref="centerSelectorRef">
        <button
          class="center-btn"
          @click.stop="toggleCenterSelector"
          title="센터 선택/변경"
          type="button"
        >
          {{ isDefaultMode ? '센터 선택' : '센터 변경' }}
        </button>

        <teleport to="body">
          <div
            v-if="isCenterSelectorOpen"
            class="menu-popover center-popover"
            :style="centerPopoverStyle"
            @click.stop
          >
            <div class="field">
              <label>센터 ID</label>
              <input
                ref="centerInputRef"
                v-model="centerInputValue"
                type="text"
                class="panel-input"
                placeholder="예: LC_13 또는 DEFAULT"
                @keyup.enter="confirmCenterChange"
                @keyup.escape="closeCenterSelector"
              />
              <div class="input-hint">DEFAULT 입력 시 시스템 마스터 모드로 전환</div>
            </div>
            <div class="panel-actions">
              <button class="panel-btn ghost" @click="confirmCenterChange('DEFAULT')" type="button"
                >마스터로</button
              >
              <button class="panel-btn ghost" @click="closeCenterSelector" type="button"
                >취소</button
              >
              <button class="panel-btn primary" @click="confirmCenterChange()" type="button"
                >적용</button
              >
            </div>
          </div>
        </teleport>
      </div>

      <div class="toolbar-divider"></div>

      <button
        class="icon-btn"
        :disabled="!canUndo"
        @click="$emit('undo')"
        title="Undo"
        type="button"
      >
        ↶
      </button>
      <button
        class="icon-btn"
        :disabled="!canRedo"
        @click="$emit('redo')"
        title="Redo"
        type="button"
      >
        ↷
      </button>

      <div class="toolbar-divider"></div>

      <button
        class="generator-btn"
        @click="$emit('open-generator')"
        title="설비 생성 (역매핑)"
        type="button"
      >
        설비 생성
      </button>

      <button
        type="button"
        class="rack-cell-edit-btn"
        :class="{ active: props.rackCellEditMode }"
        @click="$emit('toggle-rack-cell-edit')"
        title="랙 셀 ON/OFF 편집 모드"
      >
        랙 셀 편집{{ props.rackCellEditMode ? ' ON' : '' }}
      </button>

      <div class="toolbar-divider"></div>

      <button
        class="save-btn"
        :disabled="!props.canSave"
        @click="$emit('save')"
        :title="props.canSave ? 'Save' : '저장 권한 없음 (super 전용)'"
        type="button"
      >
        저장
      </button>
    </div>

    <teleport to="body">
      <div v-if="isMenuOpen" class="menu-popover" :style="menuStyle" @click.stop>
        <button class="menu-item" @click="onAddFloor" type="button">새 층 추가</button>

        <button class="menu-item" :disabled="!activePageId" @click="openCopyPanel" type="button">
          현재 층 복사
        </button>

        <div class="menu-divider"></div>

        <div v-if="isCopyPanelOpen" class="copy-panel">
          <div class="field">
            <label>새 층 번호</label>
            <input
              ref="copyFloorInputRef"
              v-model.number="copyFloorLevel"
              type="number"
              class="panel-input"
              placeholder="예: 2"
              @keyup.enter="confirmCopy"
              @keyup.escape="closeCopyPanel"
            />
          </div>

          <div class="panel-actions">
            <button class="panel-btn ghost" @click="closeCopyPanel" type="button">취소</button>
            <button class="panel-btn primary" @click="confirmCopy" type="button">복사</button>
          </div>
        </div>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
  import { ref, nextTick, computed, onMounted, onBeforeUnmount } from 'vue';
  import type { TbEcs2dPage, TbEqGroupMst } from '../api/types';
  import { useShuttleStore } from '../store/shuttleStore';

  const store = useShuttleStore();
  const canUndo = computed(() => store.canUndo);
  const canRedo = computed(() => store.canRedo);

  const props = withDefaults(
    defineProps<{
      eqGroups: TbEqGroupMst[];
      activeEqGroupId: string;
      pages: TbEcs2dPage[];
      activePageId: string;
      lcId: string;
      isDefaultMode: boolean;
      canSave?: boolean;
      canManagePages?: boolean;
      rackCellEditMode?: boolean;
    }>(),
    {
      canSave: true,
      canManagePages: true,
      rackCellEditMode: false,
    },
  );

  const emit = defineEmits<{
    (e: 'select-eq-group', eqGroupId: string): void;
    (e: 'select-page', pageId: string): void;
    (e: 'add-floor'): void;
    (e: 'remove-page', pageId: string): void;
    (e: 'update-page-name', payload: { id: string; pageName: string }): void;
    (e: 'copy-floor', payload: { sourcePageId: string; newFloorLevel: number }): void;
    (e: 'save'): void;
    (e: 'undo'): void;
    (e: 'redo'): void;
    (e: 'open-generator'): void;
    (e: 'change-center', lcId: string): void;
    (e: 'delete-eq-group', eqGroupId: string): void;
    (e: 'toggle-rack-cell-edit'): void;
  }>();

  // ============================================
  // 센터 변경 팝오버
  // ============================================
  const centerSelectorRef = ref<HTMLElement | null>(null);
  const centerInputRef = ref<HTMLInputElement | null>(null);
  const isCenterSelectorOpen = ref(false);
  const centerInputValue = ref('');
  const centerPopoverPos = ref({ top: 0, right: 0 });

  const centerPopoverStyle = computed(() => ({
    position: 'fixed',
    top: `${centerPopoverPos.value.top}px`,
    right: `${centerPopoverPos.value.right}px`,
    width: '280px',
    zIndex: 9999,
  }));

  const updateCenterPopoverPos = () => {
    const btn = centerSelectorRef.value;
    if (!btn) return;
    const rect = btn.getBoundingClientRect();
    centerPopoverPos.value = {
      top: rect.bottom + 8,
      right: window.innerWidth - rect.right,
    };
  };

  const toggleCenterSelector = async () => {
    isCenterSelectorOpen.value = !isCenterSelectorOpen.value;
    if (isCenterSelectorOpen.value) {
      centerInputValue.value = props.isDefaultMode ? '' : props.lcId;
      await nextTick();
      updateCenterPopoverPos();
      centerInputRef.value?.focus();
    }
  };

  const closeCenterSelector = () => {
    isCenterSelectorOpen.value = false;
    centerInputValue.value = '';
  };

  const confirmCenterChange = (forceLcId?: string) => {
    const target = forceLcId ?? centerInputValue.value.trim();
    if (!target) {
      centerInputRef.value?.focus();
      return;
    }
    emit('change-center', target);
    closeCenterSelector();
  };

  const onGlobalClickForCenter = (e: MouseEvent) => {
    if (!isCenterSelectorOpen.value) return;
    const target = e.target as Node;
    if (centerSelectorRef.value?.contains(target)) return;
    closeCenterSelector();
  };

  // ✅ 페이지를 eqGroupId 기준으로 그룹핑
  interface PageGroup {
    eqGroupId: string | null;
    displayName: string;
    pages: TbEcs2dPage[];
  }

  const pageGroups = computed<PageGroup[]>(() => {
    const allPages = props.pages || [];
    const groupMap = new Map<string, TbEcs2dPage[]>();

    // 페이지를 eqGroupId별로 그룹핑
    allPages.forEach((page) => {
      const key = page.eqGroupId || '';
      if (!groupMap.has(key)) {
        groupMap.set(key, []);
      }
      groupMap.get(key)!.push(page);
    });

    const groups: PageGroup[] = [];

    // eqGroupId가 없는 그룹 (이름 없음)
    if (groupMap.has('')) {
      groups.push({
        eqGroupId: null,
        displayName: '이름 없음',
        pages: groupMap.get('')!,
      });
    }

    // eqGroupId가 있는 그룹들
    groupMap.forEach((pages, key) => {
      if (key === '') return;
      // tb_eq_group_mst에서 이름 찾기
      const eqGroup = props.eqGroups?.find((g) => g.id === key);
      groups.push({
        eqGroupId: key,
        displayName: eqGroup?.name || key,
        pages,
      });
    });

    return groups;
  });

  // ✅ 현재 선택된 그룹의 페이지만 표시
  const sortedPages = computed(() => {
    const currentGroupId = props.activeEqGroupId || '';
    const filtered = (props.pages || []).filter((p) => (p.eqGroupId || '') === currentGroupId);
    return [...filtered].sort((a, b) => {
      const af = a.floorLevel ?? 0;
      const bf = b.floorLevel ?? 0;
      if (af !== bf) return af - bf;
      return (a.pageIndex ?? 0) - (b.pageIndex ?? 0);
    });
  });

  const floorLabel = (p: TbEcs2dPage) => {
    if (typeof p.floorLevel === 'number') return `${p.floorLevel}층`;
    return p.pageName;
  };

  const selectPage = (pageId: string) => emit('select-page', pageId);

  const removePage = (pageId: string) => {
    if (confirm('이 층을 삭제하시겠습니까? 배치된 설비 정보도 함께 삭제됩니다.')) {
      emit('remove-page', pageId);
    }
  };

  const editInputRef = ref<HTMLInputElement[]>([]);
  const editingPageId = ref<string | null>(null);
  const editingName = ref('');

  const isEditing = (pageId: string) => editingPageId.value === pageId;

  const startEdit = (pageId: string) => {
    const page = props.pages.find((p) => p.id === pageId);
    if (!page) return;
    editingPageId.value = pageId;
    editingName.value = page.pageName;

    nextTick(() => {
      const input = editInputRef.value?.[0];
      input?.focus();
      input?.select();
    });
  };

  const finishEdit = (pageId: string) => {
    if (editingName.value.trim() && editingPageId.value === pageId) {
      emit('update-page-name', { id: pageId, pageName: editingName.value.trim() });
    }
    editingPageId.value = null;
    editingName.value = '';
  };

  const cancelEdit = () => {
    editingPageId.value = null;
    editingName.value = '';
  };

  const topbarRef = ref<HTMLElement | null>(null);
  const plusBtnRef = ref<HTMLElement | null>(null);

  const isMenuOpen = ref(false);
  const isCopyPanelOpen = ref(false);

  const copyFloorLevel = ref<number | null>(null);
  const copyFloorInputRef = ref<HTMLInputElement | null>(null);

  const menuPos = ref({ top: 0, left: 0, width: 220 });
  const menuStyle = computed(() => ({
    position: 'fixed',
    top: `${menuPos.value.top}px`,
    left: `${menuPos.value.left}px`,
    width: `${menuPos.value.width}px`,
    zIndex: 9999,
  }));

  const updateMenuPosition = () => {
    const btn = plusBtnRef.value;
    if (!btn) return;

    const rect = btn.getBoundingClientRect();
    menuPos.value = { top: rect.bottom + 8, left: rect.left, width: 220 };
  };

  const toggleMenu = async () => {
    isMenuOpen.value = !isMenuOpen.value;
    if (!isMenuOpen.value) {
      closeCopyPanel();
      return;
    }
    await nextTick();
    updateMenuPosition();
  };

  const closeMenu = () => {
    isMenuOpen.value = false;
    closeCopyPanel();
  };

  const onAddFloor = () => {
    emit('add-floor');
    closeMenu();
  };

  const openCopyPanel = async () => {
    if (!props.activePageId) return;
    copyFloorLevel.value = null;
    isCopyPanelOpen.value = true;

    await nextTick();
    copyFloorInputRef.value?.focus();
  };

  const closeCopyPanel = () => {
    isCopyPanelOpen.value = false;
    copyFloorLevel.value = null;
  };

  const confirmCopy = () => {
    if (!props.activePageId) return;
    if (typeof copyFloorLevel.value !== 'number' || !copyFloorLevel.value) {
      alert('새 층 번호를 입력해주세요.');
      copyFloorInputRef.value?.focus();
      return;
    }

    emit('copy-floor', { sourcePageId: props.activePageId, newFloorLevel: copyFloorLevel.value });
    closeMenu();
  };

  const onGlobalClick = (e: MouseEvent) => {
    if (!isMenuOpen.value) return;
    const target = e.target as Node;
    const top = topbarRef.value;
    if (top && top.contains(target)) return;
    closeMenu();
  };

  const onResize = () => {
    if (!isMenuOpen.value) return;
    updateMenuPosition();
  };

  const onScroll = () => {
    if (!isMenuOpen.value) return;
    updateMenuPosition();
  };

  onMounted(() => {
    window.addEventListener('click', onGlobalClick);
    window.addEventListener('click', onGlobalClickForCenter);
    window.addEventListener('resize', onResize);
    window.addEventListener('scroll', onScroll, true);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('click', onGlobalClick);
    window.removeEventListener('click', onGlobalClickForCenter);
    window.removeEventListener('resize', onResize);
    window.removeEventListener('scroll', onScroll, true);
  });
</script>

<style scoped>
  .editor-topbar {
    display: flex;
    align-items: center;
    justify-content: space-between;

    padding: 8px 12px;
    border-bottom: 1px solid #e4e7ed;
    background: #fff;

    flex: 0 0 auto;
    min-height: 0;

    /* ✅ topbar는 절대 내용 때문에 커지면 안 됨 */
    overflow: hidden;
  }

  .topbar-left {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;
    flex: 1;
  }

  .row {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }

  /* ✅ “가로 스크롤바”가 레이아웃 높이를 먹지 않게: overlay 방식 */
  .scroll-x {
    min-width: 0;
    width: 100%;
    overflow-x: auto;
    overflow-y: hidden;
    white-space: nowrap;

    /* ✅ 스크롤바가 생겨도 높이 밀림 방지 (지원 브라우저에서 효과) */
    scrollbar-gutter: stable;

    /* ✅ 공통: 컨테이너 높이 고정 */
    height: 36px;
    display: flex;
    align-items: center;
  }

  /* ✅ 크롬/엣지에서 “높이”를 먹는 스크롤바를 얇게 + 오버레이에 가깝게 */
  .scroll-x::-webkit-scrollbar {
    height: 8px;
  }
  .scroll-x::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.18);
    border-radius: 999px;
  }
  .scroll-x::-webkit-scrollbar-track {
    background: transparent;
  }

  .eqgroup-tabs {
    height: 36px;
  }

  .chip {
    height: 28px;
    padding: 0 12px;
    border: 1px solid #dcdfe6;
    background: #f5f7fa;
    border-radius: 999px;
    cursor: pointer;
    font-size: 13px;
    color: #606266;
    flex: 0 0 auto;
  }
  .chip.active {
    background: #409eff;
    border-color: #409eff;
    color: #fff;
  }
  .group-count {
    font-size: 11px;
    opacity: 0.8;
    margin-left: 2px;
  }
  .group-x {
    margin-left: 6px;
    padding: 0 4px;
    font-size: 14px;
    font-weight: 400;
    line-height: 1;
    cursor: pointer;
    opacity: 0.5;
    color: inherit;
    transition: opacity 0.15s, color 0.15s;
  }
  .group-x:hover {
    opacity: 1;
    color: #dc2626;
  }

  .page-row {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .page-tabs {
    flex: 1;
    min-width: 0;
    height: 40px;
    display: flex;
    align-items: center;
  }

  .tab {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    margin-right: 6px;

    padding: 6px 10px;
    background: #f5f7fa;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    cursor: pointer;
    font-size: 13px;
    color: #606266;
    flex: 0 0 auto;
  }
  .tab.active {
    background: #ffffff;
    border-color: #409eff;
    color: #409eff;
    font-weight: 600;
  }

  .tab-label {
    max-width: 140px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .remove-x {
    font-size: 16px;
    color: #909399;
  }
  .remove-x:hover {
    color: #f56c6c;
  }

  .edit-input {
    width: 120px;
    padding: 2px 6px;
    border: 1px solid #409eff;
    border-radius: 6px;
    font-size: 13px;
  }

  .tab-plus-wrap {
    display: inline-flex;
    flex: 0 0 auto;
    gap: 2px;
  }

  /* 층 추가 버튼 - 눈에 띄게 */
  .add-page-btn {
    height: 32px;
    padding: 0 12px;
    background: #409eff;
    border: none;
    border-radius: 8px 0 0 8px;
    cursor: pointer;
    font-size: 13px;
    font-weight: 500;
    color: #fff;
    white-space: nowrap;
  }
  .add-page-btn:hover {
    background: #66b1ff;
  }

  /* 드롭다운 토글 버튼 */
  .menu-toggle-btn {
    height: 32px;
    padding: 0 8px;
    background: #409eff;
    border: none;
    border-left: 1px solid rgba(255, 255, 255, 0.3);
    border-radius: 0 8px 8px 0;
    cursor: pointer;
    color: #fff;
    display: inline-flex;
    align-items: center;
  }
  .menu-toggle-btn:hover {
    background: #66b1ff;
  }

  .caret {
    font-size: 12px;
  }

  .menu-popover {
    background: #fff;
    border: 1px solid #e4e7ed;
    border-radius: 10px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    padding: 8px;
  }

  .menu-item {
    width: 100%;
    text-align: left;
    background: transparent;
    border: none;
    padding: 10px 10px;
    border-radius: 8px;
    cursor: pointer;
    font-size: 14px;
    color: #303133;
  }
  .menu-item:hover:not(:disabled) {
    background: #f5f7fa;
  }
  .menu-item:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .menu-divider {
    height: 1px;
    background: #ebeef5;
    margin: 8px 0;
  }

  .copy-panel {
    padding: 6px 2px 2px;
  }

  .field {
    display: flex;
    flex-direction: column;
    gap: 6px;
    margin-bottom: 10px;
  }
  .field label {
    font-size: 12px;
    color: #909399;
  }

  .panel-input {
    padding: 8px 10px;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    font-size: 14px;
    outline: none;
  }
  .panel-input:focus {
    border-color: #409eff;
  }

  .panel-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 6px;
  }

  .panel-btn {
    padding: 8px 12px;
    border-radius: 8px;
    border: 1px solid #dcdfe6;
    cursor: pointer;
    font-size: 13px;
  }
  .panel-btn.ghost {
    background: #fff;
    color: #606266;
  }
  .panel-btn.ghost:hover {
    background: #f5f7fa;
  }
  .panel-btn.primary {
    background: #409eff;
    border-color: #409eff;
    color: #fff;
  }
  .panel-btn.primary:hover {
    opacity: 0.9;
  }

  .toolbar-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 0 0 auto;
    margin-left: 10px;
  }

  .icon-btn {
    height: 32px;
    width: 36px;
    border-radius: 8px;
    border: 1px solid #dcdfe6;
    background: #f5f7fa;
    cursor: pointer;
    font-size: 16px;
    color: #606266;
  }
  .icon-btn:hover:not(:disabled) {
    background: #ecf5ff;
    border-color: #409eff;
    color: #409eff;
  }
  .icon-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .toolbar-divider {
    width: 1px;
    height: 22px;
    background: #dcdfe6;
    margin: 0 2px;
  }

  .save-btn {
    height: 32px;
    padding: 0 14px;
    border: none;
    border-radius: 8px;
    background: #67c23a;
    color: #fff;
    cursor: pointer;
    font-size: 14px;
  }
  .save-btn:hover {
    background: #85ce61;
  }

  .generator-btn {
    height: 32px;
    padding: 0 14px;
    border: none;
    border-radius: 8px;
    background: #e6a23c;
    color: #fff;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
  }
  .generator-btn:hover {
    background: #ebb563;
  }

  .rack-cell-edit-btn {
    height: 32px;
    margin-left: 8px;
    padding: 0 12px;
    border: 1px solid #ccc;
    border-radius: 8px;
    background: #fff;
    color: #333;
    cursor: pointer;
    font-size: 13px;
    font-weight: 500;
  }
  .rack-cell-edit-btn.active {
    background: #0078ff;
    color: #fff;
    border-color: #0066d6;
  }

  /* 모드 배지 */
  .mode-badge {
    display: inline-flex;
    align-items: center;
    height: 28px;
    padding: 0 10px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
  }
  .badge-master {
    background: #f0f9eb;
    color: #67c23a;
    border: 1px solid #b3e19d;
  }
  .badge-center {
    background: #ecf5ff;
    color: #409eff;
    border: 1px solid #a0cfff;
  }

  /* 센터 변경 버튼 */
  .center-selector-wrap {
    position: relative;
  }
  .center-btn {
    height: 32px;
    padding: 0 12px;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    background: #fff;
    cursor: pointer;
    font-size: 13px;
    color: #606266;
    white-space: nowrap;
  }
  .center-btn:hover {
    border-color: #409eff;
    color: #409eff;
  }

  /* 센터 팝오버 */
  .center-popover {
    background: #fff;
    border: 1px solid #e4e7ed;
    border-radius: 10px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    padding: 12px;
  }

  .input-hint {
    font-size: 11px;
    color: #909399;
    margin-top: 4px;
  }
</style>
