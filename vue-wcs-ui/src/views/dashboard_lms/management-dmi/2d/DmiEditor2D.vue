<template>
  <div>
    <CenterCodeModal v-if="showModal" @submit="handleCenterCodeSubmit" />

    <!-- lcId가 설정된 후에만 에디터 표시 -->
    <div class="dashboard-editor" v-else-if="lcId">
      <div class="top-section">
        <TopTabBar
          v-model:pages="pages"
          :active-page-id="activePageId"
          @add-page="addPage"
          @remove-page="removePage"
          @select-page="selectPage"
          @update-page-name="updatePageName"
        />
      </div>
      <div class="main-section">
        <div class="left-panel">
          <LeftPanel
            :lcId="lcId"
            @add-object="handleAddNewObject"
            @batch-add-objects="handleBatchAddObjects"
          />
        </div>

        <div class="center-panel">
          <CenterPanel
            v-if="activePage"
            ref="centerPanelRef"
            :lcId="lcId"
            v-model:page="activePage"
            @save="handleSaveDashboard"
            @object-selected="handleObjectSelected"
          />
          <div v-else class="no-page-selected">
            페이지를 선택하거나 새 페이지를 추가하세요.
          </div>
        </div>

        <RightPanel
          :selectedObject="selectedObjects"
          @update:selectedObject="handleRightPanelUpdate"
        />
      </div>
    </div>

    <div v-if="isSaving" class="loading-overlay">
      <div class="spinner"></div>
      <p class="loading-text">저장 중입니다...</p>
    </div>

  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import TopTabBar from './TopTabBar.vue';
import CenterCodeModal from '../CenterCodeModal.vue';
import LeftPanel from './LeftPanel.vue';
import CenterPanel from './CenterPanel.vue';
import RightPanel from './RightPanel.vue';
import { buildUUID } from '@/utils/uuid';
import { getCommonGetApi, getCommonPostApi } from '/@/api/common/api';



/**
 * 상태 변수
 */
const pages = ref([]);
const activePageId = ref('');
const lcId = ref(null);
const showModal = ref(true);
const isSaving = ref(false);

const centerPanelRef = ref(null); // CenterPanel 참조
const selectedObjects = ref([]); // 현재 선택된 단일 객체 (RightPanel용)



/**
 * 페이지 변경 감지 및 객체 로드
 */
const activePage = computed(() => pages.value.find((p) => p.id === activePageId.value));

watch(activePageId, (newPageId) => {
  if (newPageId) {
    loadDmiObjectsForPage(newPageId);
    selectedObjects.value = [];
  } else {
    selectedObjects.value = [];
  }
});

// 페이지에 해당하는 DMI 객체 목록을 불러오는 함수
const loadDmiObjectsForPage = async (pageId) => {
  if (centerPanelRef.value) {
    centerPanelRef.value.clearAllObjects();
  }

  const url = `/status_board_dmi/${lcId.value}/${pageId}`;
  const result = await getCommonGetApi(url);

  for (const objectData of result) {
    centerPanelRef.value.addExistingObject(objectData, objectData.positionX2d, objectData.positionY2d);
  }
};



/**
 * 모달 및 페이지 초기화
 */
const initializeDashboard = async () => {
  if (!lcId.value) return;
  pages.value = await fetchPages();
  if (pages.value.length > 0) {
    activePageId.value = pages.value[0].id;
  }
};

const handleCenterCodeSubmit = (submittedLcId) => {
  lcId.value = submittedLcId;
  showModal.value = false;
  initializeDashboard();
};

// 센터 코드에 해당하는 페이지 목록 불러오기
const fetchPages = async () => {
  const url = `/status_board_page/${lcId.value}`;
  const response = await getCommonGetApi(url);
  return response.map((item) => {
    const { page_name, background_color, length_x, length_y, ...rest } = item;
    return {
      pageName: page_name,
      backgroundColor: background_color || '#FFFFFF',
      lengthX: length_x || 1920,
      lengthY: length_y || 1080,
      ...rest
    };
  });
};



/**
 * LeftPanel Event
 */
// CenterPanel Object 생성 요청
const handleAddNewObject = (objectData) => {
  if (centerPanelRef.value && activePage.value) {
    centerPanelRef.value.addNewObject(objectData, 0, 0);
  }
};

const handleBatchAddObjects = (objectList) => {
  if (centerPanelRef.value && activePage.value) {
    objectList.forEach(obj => {
      centerPanelRef.value.addNewObject(obj, obj.positionX2d, obj.positionY2d);
    });
  }
};

/**
 * CenterPanel Event
 */
// 수정사항 저장 요청
const handleSaveDashboard = async (deltaPayload, page) => {
  isSaving.value = true;
  let result = false;

  // 1. Page 저장
  try {
    const pageUrl = `/status_board_page/update_value`;
    const pageParam = {
      id: page.id,
      background_color: page.backgroundColor,
      length_x: page.lengthX,
      length_y: page.lengthY
    }
    await getCommonPostApi(pageUrl, pageParam);

    // 2. DMI 저장
    const url = `/status_board_dmi/update_on_page`;
    const param = {
      added_list: deltaPayload.addedList,
      updated_list: deltaPayload.updatedList,
      deleted_ids: deltaPayload.deletedIds,
      lc_id: lcId.value,
      page_id: page.id,
      dimension: '2D'
    }

    result = await getCommonPostApi(url, param);
    if (result) {
      alert("저장되었습니다!");
      await loadDmiObjectsForPage(page.id); // 데이터 재조회
    } else {
      alert("저장에 실패했습니다. 중복되는 객체가 없는지 확인해 주세요.");
    }
  } catch (e) {
    console.error(e.message);
    alert("서버 통신 중 오류가 발생했습니다.");
  } finally {
    isSaving.value = false;
  }
};

// CenterPanel에서 선택된 객체 정보를 받아 RightPanel에 전달하기 위해 상태 업데이트
const handleObjectSelected = (objects) => {
  selectedObjects.value = objects || [];
};



/**
 * RightPanel Event
 */
// Object 변경 내역을 CenterPanel에 전달
const handleRightPanelUpdate = (updatedObjects) => {
  if (centerPanelRef.value) {
    centerPanelRef.value.updateObjectValue(updatedObjects);
  }
};



/**
 * TopTabBar Event
 */
// TopTabBar 페이지 추가
const addPage = () => {
  if (centerPanelRef.value) {
    centerPanelRef.value.clearAllObjects();
  }

  const newPage = {
    id: buildUUID(),
    pageName: 'New Page',
    backgroundColor: '#FFFFFF',
    lengthX: 1920,
    lengthY: 1080
  };
  pages.value.push(newPage);
  activePageId.value = newPage.id;

  const url = `/status_board_page/create`;
  const param = {
    id: newPage.id,
    lc_id: lcId.value,
    page_index: pages.value.length - 1,
    page_name: newPage.pageName,
    background_color: newPage.backgroundColor,
    length_x: newPage.lengthX,
    length_y: newPage.lengthY
  };
  getCommonPostApi(url, param);
};

// TopTabBar 페이지 삭제
const removePage = (pageIdToRemove) => {
  const pageIndex = pages.value.findIndex((p) => p.id === pageIdToRemove);
  if (pageIndex === -1) return;
  const isRemovingActivePage = activePageId.value === pageIdToRemove;
  pages.value.splice(pageIndex, 1);
  if (isRemovingActivePage) {
    if (pages.value.length > 0) {
      const newActiveIndex = Math.min(pageIndex, pages.value.length - 1);
      activePageId.value = pages.value[newActiveIndex].id;
    } else {
      activePageId.value = '';
    }
  }
  const url = `/status_board_page/delete`;
  const param = { id: pageIdToRemove };
  getCommonPostApi(url, param);
};

// TopTabBar 페이지 선택
const selectPage = (pageId) => {
  centerPanelRef.value.clearAllObjects();
  activePageId.value = pageId;
};

// TopTabBar 이름 수정
const updatePageName = ({ id, pageName }) => {
  const page = pages.value.find((p) => p.id === id);
  if (page) {
    page.pageName = pageName;
    const url = `/status_board_page/update_name`;
    const param = { id: id, page_name: pageName };
    getCommonPostApi(url, param);
  }
};
</script>

<style scoped>
.dashboard-editor {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f4f7f6;
  color: #303133;
}

.top-section { flex-shrink: 0; }

.main-section {
  display: flex;
  flex-grow: 1;
  overflow: hidden;
  border-top: 1px solid #dcdfe6;
}

.left-panel {
  flex-basis: 20%;
  flex-grow: 1;
  background-color: #ffffff;
  padding: 10px;
  border-right: 1px solid #dcdfe6;
}

.center-panel {
  flex-basis: 60%; flex-grow: 4;
  background-color: #fdfdfd;
  overflow: hidden;
  min-width: 0;
  min-height: 0;
}

.no-page-selected {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  font-size: 1.2em;
  color: #909399;
}

/* 로딩 오버레이 스타일 */
.loading-overlay {
  position: fixed;       /* 화면 스크롤 상관없이 고정 */
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5); /* 검은색 반투명 배경 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 9999;         /* 모든 요소보다 위에 표시 */
  backdrop-filter: blur(2px); /* 배경 흐림 효과 */
}

/* 원형 스피너 */
.spinner {
  width: 50px;
  height: 50px;
  border: 5px solid rgba(255, 255, 255, 0.3); /* 흐릿한 흰색 테두리 */
  border-radius: 50%;
  border-top-color: #409eff; /* 회전하는 부분 색상 */
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 15px;
}

/* 로딩 텍스트 */
.loading-text {
  color: #ffffff;
  font-size: 1.2rem;
  font-weight: 500;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

/* 회전 애니메이션 키프레임 */
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
