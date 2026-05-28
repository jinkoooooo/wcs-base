<template>
  <nav class="top-tab-bar">
    <draggable
      v-model="draggablePages"
      class="tabs-container"
      item-key="id"
      animation="200"
    >
      <template #item="{ element: page }">
        <div
          class="tab-item"
          :class="{ 'active': page.id === activePageId }"
          @click="selectPage(page.id)"
          @dblclick="startEditing(page)"
        >
          <template v-if="editingPageId === page.id">
            <input
              ref="nameInput"
              type="text"
              v-model="editingPageName"
              @blur="finishEditing(page)"
              @keyup.enter="finishEditing(page)"
              @keyup.esc="cancelEditing"
            />
          </template>
          <template v-else>
            <span class="page-name">{{ page.pageName }}</span>
          </template>
          <button class="close-btn" @click.stop="removePage(page.id, page.pageName)">-</button>
        </div>
      </template>
    </draggable>
    <button class="add-btn" @click="addPage">+</button>
  </nav>
</template>

<script setup>
import { ref, defineProps, defineEmits, nextTick, computed } from 'vue';
import { useMessage } from '/@/hooks/web/useMessage';
import draggable from 'vuedraggable';
import { getCommonPostApi } from '@/api/common/api';

const { createConfirm } = useMessage();

// 부모 컴포넌트로부터 받을 props 정의
const props = defineProps({
  pages: {
    type: Array,
    required: true
  },
  activePageId: {
    type: String,
    required: true
  }
});

// 부모 컴포넌트로 보낼 emits 이벤트 정의
const emit = defineEmits([
  'update:pages',
  'add-page',
  'remove-page',
  'select-page',
  'update-page-name'
]);

// 내부 상태 관리
const draggablePages = computed({
  get() {
    return props.pages;
  },
  set(newPages) {
    emit('update:pages', newPages);
    newPages.forEach((page, index) => {
      page.page_index = index;
    });

    const url = `/status_board_page/update_index`;
    getCommonPostApi(url, newPages);
  },
});
const editingPageId = ref(null);
const editingPageName = ref('');
const nameInput = ref(null); // input 엘리먼트 참조

// 페이지 추가 이벤트 emit
const addPage = () => {
  emit('add-page');
};

// 페이지 삭제 이벤트 emit
const removePage = (pageId, pageName) => {
  // 이벤트 전파를 막아 click 이벤트가 실행되지 않도록 함
  createConfirm({
    iconType: 'warning',
    title: () => '삭제',
    content: () => pageName + ' 페이지를 삭제하시겠습니까? 삭제한 데이터는 복구할 수 없습니다.',
    onOk: async () => {
      emit('remove-page', pageId);
    },
  });
};

// 페이지 선택 이벤트 emit
const selectPage = (pageId) => {
  createConfirm({
    iconType: 'warning',
    title: () => '이동',
    content: () => '페이지 이동 시 저장되지 않은 데이터는 소실됩니다. 진행하시겠습니까?',
    onOk: async () => {
      emit('select-page', pageId);
    },
  });
};

// 페이지 이름 편집 시작
const startEditing = async (page) => {
  editingPageId.value = page.id;
  editingPageName.value = page.pageName;
  // DOM 업데이트를 기다린 후 input에 포커스
  await nextTick();
  if (nameInput.value && nameInput.value[0]) {
    nameInput.value[0].focus();
  }
};

// 페이지 이름 편집 완료
const finishEditing = (page) => {
  if (editingPageId.value) {
    emit('update-page-name', { id: page.id, pageName: editingPageName.value });
    editingPageId.value = null;
  }
};

// 페이지 이름 편집 취소
const cancelEditing = () => {
  editingPageId.value = null;
};
</script>

<style scoped>
.top-tab-bar {
  display: flex;
  align-items: center;
  background-color: #f0f2f5;
  padding: 5px 5px 0 5px;
  border-bottom: 1px solid #dcdfe6;
}
.tabs-container {
  display: flex;
  align-items: center;
  flex-grow: 1;
  overflow-x: auto;
}
.tab-item {
  display: flex;
  align-items: center;
  padding: 8px 15px;
  background-color: #e9ecef;
  border: 1px solid #dcdfe6;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  margin-right: 2px;
  cursor: move; /* 4. 드래그 가능한 요소임을 나타내는 커서 추가 */
  color: #606266;
  white-space: nowrap;
  position: relative;
}
.tab-item.active {
  background-color: #ffffff; /* 활성 탭은 흰색 */
  color: #409eff; /* 활성 탭 텍스트는 파란색 */
  font-weight: bold;
  border-top: 2px solid #409eff; /* 위쪽에 강조선 */
  padding-top: 6px; /* 위쪽 보더 두께만큼 패딩 조정 */
}
.tab-item:hover:not(.active) {
  background-color: #f2f2f2; /* 호버 시 약간 밝게 */
}
.page-name {
  margin-right: 10px;
}
.close-btn {
  background: none;
  border: none;
  color: #909399;
  cursor: pointer;
  padding: 0 5px;
  font-size: 16px;
  line-height: 1;
  border-radius: 50%;
}
.close-btn:hover {
  color: #303133;
  background-color: #e9ecef;
}
.add-btn {
  background-color: #f0f2f5;
  border: 1px dashed #c0c4cc; /* 점선 테두리 */
  color: #909399;
  cursor: pointer;
  padding: 4px 8px;
  font-size: 16px;
  border-radius: 4px;
  margin-left: 8px; /* 탭과의 간격 */
}
.add-btn:hover {
  border-color: #409eff;
  color: #409eff;
}
input[type="text"] {
  background-color: #fff;
  color: #303133;
  border: 1px solid #409eff;
  padding: 2px 4px;
  border-radius: 3px;
  outline: none;
}
</style>
