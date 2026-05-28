<template>
  <div class="dashboard-viewer" @click="closeInfoPanel">
    <header v-if="pages.length > 1" class="viewer-header">
      <div
        v-for="page in pages"
        :key="page.id"
        class="page-tab"
        :class="{ active: activePageId === page.id }"
        @click.stop="selectPage(page.id)"
      >
        {{ page.pageName }}
      </div>
    </header>

    <main class="viewer-body" ref="stageContainerRef">
      <div v-if="activePage" class="stage-wrapper">
        <div class="stage" :style="stageStyle">
          <div
            v-for="obj in sortedObjects"
            :key="obj.id"
            class="dmi-object"
            :class="{
              'status-normal': obj.instance_status === 1,
              'status-working': obj.instance_status === 2,
              'status-error': obj.instance_status === 9,
              'is-selected': selectedTargetId === obj.id
            }"
            :style="getObjectStyle(obj)"
            @click.stop="handleObjectClick(obj)"
          >
            <img
              v-if="imageUrlsCache.get(obj.model_type)"
              :src="imageUrlsCache.get(obj.model_type)"
              :alt="obj.group_code"
              class="object-image main-image"
              draggable="false"
            />
            <div v-else class="object-placeholder">
              {{ obj.group_code }}
            </div>

            <img
              v-if="obj.box_is_use && imageUrlsCache.get('BOX')"
              :src="imageUrlsCache.get('BOX')"
              alt="BOX"
              class="object-image box-overlay"
              draggable="false"
            />
          </div>
        </div>
      </div>

      <transition name="slide-fade">
        <aside v-if="isInfoVisible" class="info-panel" @click.stop>
          <div class="info-header">
            <h3><i class="icon-detail"></i> 상세 정보 [{{ selectedObject.model_code }}]</h3>
            <button class="close-btn" @click="isInfoVisible = false">×</button>
          </div>

          <div v-if="infoLoading" class="info-loader">
            <div class="spinner"></div>
            <p>데이터를 불러오는 중...</p>
          </div>

          <div v-else-if="detailInfo" class="info-content">
            <section
              v-if="selectedObject && selectedObject.error_code && selectedObject.error_code !== 0"
              class="info-section error-section"
            >
              <div class="error-header">
                <i class="icon-warning">⚠</i> Error Detected
              </div>
              <div class="info-row">
                <span class="label">에러 코드</span>
                <span class="value error-code">{{ selectedObject.error_code }}</span>
              </div>
              <div class="error-message-box">
                {{ selectedObject.error_message || '알 수 없는 오류가 발생했습니다.' }}
              </div>
              <hr class="divider error-divider" />
            </section>

            <section class="info-section task-info">
              <label>Task Information</label>
              <div class="info-row">
                <span class="label">작업 번호</span>
                <span class="value highlight">
                  <small>[{{ detailInfo.command_type || '-' }}]</small> {{ detailInfo.task_id || 'N/A' }}
                </span>
              </div>
              <div class="info-row">
                <span class="label">출발지</span>
                <span class="value">{{ detailInfo.start_point_cd || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="label">도착지</span>
                <span class="value">{{ detailInfo.end_point_cd || '-' }}</span>
              </div>
            </section>

            <hr class="divider" />

            <section class="info-section stock-info">
              <label>Stock Information (ID: {{ detailInfo.stock_id || 'N/A' }})</label>
              <div class="stock-list-container">
                <div
                  v-for="(stock, idx) in detailInfo.stock_info_list"
                  :key="idx"
                  class="stock-item"
                >
                  <div class="stock-main">
                    <span class="item-name">{{ stock.item_name }}</span>
                    <span class="item-qty">{{ stock.item_qty }} EA</span>
                  </div>
                  <div class="stock-sub">
                    <span class="item-code">{{ stock.item_code }}</span>
                  </div>
                </div>
                <div v-if="!detailInfo.stock_info_list?.length" class="empty-stock">
                  재고 정보가 없습니다.
                </div>
              </div>
            </section>
          </div>
        </aside>
      </transition>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick, shallowRef } from 'vue';
import { useRoute } from 'vue-router';
import { getCommonGetApi, apiClient } from '/src/api/common/api';

const route = useRoute();

/**
 * =========================================================
 * 상태 변수 (State)
 * =========================================================
 */
const lcId = ref(route.params.lcId || ''); // URL 파라미터에서 lcId 획득
const pages = ref([]);                     // 페이지 목록
const activePageId = ref('');              // 현재 선택된 페이지 ID
const rawObjects = shallowRef([]);         // API로 불러온 DMI(인스턴스) 원본 데이터
const groupsMap = ref(new Map());          // DMG(그룹) 정보 맵 (Key: group_code, Value: DmgObject)
const imageUrlsCache = ref(new Map());     // 이미지 Blob URL 캐시

// 캔버스 스케일링 관련
const stageContainerRef = ref(null);
const canvasScale = ref(1);

// 상세 정보 관련
const isInfoVisible = ref(false);
const infoLoading = ref(false);
const detailInfo = ref(null);
const selectedObject = ref(null);
const selectedTargetId = ref(null); // 현재 클릭된 객체 하이라이트용

// 타이머 및 옵저버
let refreshTimer = null;
let resizeObserver = null;

/**
 * =========================================================
 * Computed (계산된 속성)
 * =========================================================
 */
// 현재 활성화된 페이지 상세 정보
const activePage = computed(() => pages.value.find((p) => p.id === activePageId.value));

// 객체 클릭 핸들러
const handleObjectClick = async (obj) => {
  selectedTargetId.value = obj.id;
  selectedObject.value = obj;
  isInfoVisible.value = true;
  infoLoading.value = true;
  console.log(obj);

  try {
    // task_id와 stock_id가 null일 경우를 대비해 문자열 처리
    const taskId = obj.task_id || 'null';
    const stockId = obj.stock_id || 'null';

    const url = `/status_board_wcs/getTaskAndStockInfo/${lcId.value}/${taskId}/${stockId}`;
    detailInfo.value = await getCommonGetApi(url);
  } catch (e) {
    console.error("상세 정보 조회 실패:", e);
    detailInfo.value = null;
  } finally {
    infoLoading.value = false;
  }
};

// 정보창 닫기 (배경 클릭 시)
const closeInfoPanel = () => {
  isInfoVisible.value = false;
  selectedTargetId.value = null;
  selectedObject.value = null;
};

/**
 * [핵심 로직] 렌더링 객체 리스트 계산
 * 1. DMI(Instance)와 DMG(Group) 정보를 병합
 * 2. 미사용(isUse=false) 필터링
 * 3. 위치, 회전, 스케일 계산 (Group + Instance)
 * 4. 렌더링 순서(RenderOrder) 정렬
 */
const sortedObjects = computed(() => {
  if (!rawObjects.value.length) return [];

  const renderList = [];

  // Helper: 안전하게 속성 가져오기
  const getVal = (obj, camel, snake, defaultVal) => {
    return obj[camel] ?? obj[snake] ?? defaultVal;
  };

  for (const dmi of rawObjects.value) {
    // 1. DMI 사용여부
    const dmiIsUse = getVal(dmi, 'isUse', 'is_use', true);
    if (dmiIsUse === false) continue;

    // 2. 그룹 매칭
    const dmiGroupCode = dmi.group_code || dmi.groupCode;
    const group = groupsMap.value.get(dmiGroupCode);
    if (!group) continue;

    // 3. 그룹 사용여부
    const groupIsUse = getVal(group, 'isUse', 'is_use', true);
    if (groupIsUse === false) continue;

    // 4. 속성 계산
    const finalX = getVal(group, 'positionX2d', 'position_x_2d', 0) + getVal(dmi, 'positionX2d', 'position_x_2d', 0);
    const finalY = getVal(group, 'positionY2d', 'position_y_2d', 0) + getVal(dmi, 'positionY2d', 'position_y_2d', 0);
    const finalRotation = getVal(group, 'rotation2d', 'rotation_2d', 0) + getVal(dmi, 'rotation2d', 'rotation_2d', 0);
    const finalScaleX = getVal(group, 'scaleX2d', 'scale_x_2d', 1.0) * getVal(dmi, 'scaleX2d', 'scale_x_2d', 1.0);
    const finalScaleY = getVal(group, 'scaleY2d', 'scale_y_2d', 1.0) * getVal(dmi, 'scaleY2d', 'scale_y_2d', 1.0);

    const gFH = getVal(group, 'flipHorizontal2d', 'flip_horizontal_2d', false);
    const dFH = getVal(dmi, 'flipHorizontal2d', 'flip_horizontal_2d', false);
    const finalFlipH = Boolean(gFH) !== Boolean(dFH);

    const gFV = getVal(group, 'flipVertical2d', 'flip_vertical_2d', false);
    const dFV = getVal(dmi, 'flipVertical2d', 'flip_vertical_2d', false);
    const finalFlipV = Boolean(gFV) !== Boolean(dFV);

    const renderOrder = getVal(group, 'renderOrder', 'render_order', 0);

    const boxIsUse = getVal(dmi, 'boxIsUse', 'box_is_use', false);

    renderList.push({
      ...dmi,
      _finalX: finalX,
      _finalY: finalY,
      _finalRotation: finalRotation,
      _finalScaleX: finalScaleX,
      _finalScaleY: finalScaleY,
      _finalFlipH: finalFlipH,
      _finalFlipV: finalFlipV,
      _renderOrder: renderOrder,
      box_is_use: boxIsUse
    });
  }

  // 5. 정렬 (RenderOrder 오름차순)
  return renderList.sort((a, b) => a._renderOrder - b._renderOrder);
});

// 캔버스 스타일 (중앙 정렬 및 Scale Fit)
const stageStyle = computed(() => {
  if (!activePage.value) return {};

  const width = activePage.value.lengthX || 1920;
  const height = activePage.value.lengthY || 1080;

  return {
    position: 'absolute',
    width: `${width}px`,
    height: `${height}px`,
    backgroundColor: activePage.value.backgroundColor || '#FFFFFF',
    top: '50%',
    left: '50%',
    // 화면 크기에 맞춰 scale 조정
    transform: `translate(-50%, -50%) scale(${canvasScale.value})`,
    transformOrigin: 'center center',
  };
});

/**
 * =========================================================
 * Methods (기능 구현)
 * =========================================================
 */

// 초기화
const initialize = async () => {
  if (!lcId.value) {
    alert("센터 ID가 없습니다.");
    return;
  }

  // 1. 그룹(DMG) 정보 먼저 로드 (변하지 않는 정보라 가정하고 최초 1회 로드)
  await fetchGroups();

  // 2. 페이지 목록 조회
  await fetchPages();

  // 3. 첫 페이지 선택
  if (pages.value.length > 0) {
    activePageId.value = pages.value[0].id;
  }

  // 4. 자동 갱신 시작 (DMI 데이터 polling)
  startAutoRefresh();
};

// [API] 그룹(DMG) 정보 조회 및 Map핑
const fetchGroups = async () => {
  try {
    const url = `/status_board_dmg/select/${lcId.value}`;
    const response = await getCommonGetApi(url);

    // 빠른 조회를 위해 List -> Map 변환
    const map = new Map();
    if (Array.isArray(response)) {
      response.forEach(g => {
        const code = g.groupCode || g.group_code;
        if (code) map.set(code, g);
      });
    }
    groupsMap.value = map;
  } catch (e) {
    console.error("그룹 정보(DMG) 로드 실패:", e);
  }
};

// [API] 페이지 목록 조회
const fetchPages = async () => {
  try {
    const url = `/status_board_page/${lcId.value}`;
    const response = await getCommonGetApi(url);
    pages.value = response.map((item) => ({
      id: item.id,
      pageName: item.page_name,
      backgroundColor: item.background_color,
      lengthX: item.length_x,
      lengthY: item.length_y,
    }));
  } catch (e) {
    console.error("페이지 목록 로드 실패:", e);
  }
};

// [API] DMI(인스턴스) 객체 로드
const loadDmiObjects = async (pageId) => {
  if (!pageId) return;

  try {
    const url = `/status_board_dmi/${lcId.value}/${pageId}`;
    const result = await getCommonGetApi(url);

    // 원본 데이터 갱신 -> computed(sortedObjects)가 자동으로 재계산됨
    rawObjects.value = result;

    // 새 객체가 들어오면 이미지 캐싱 확인 및 로드 (최초 1회만 수행됨)
    await loadImagesForDmiObjects(result);
  } catch (e) {
    console.error("객체 데이터 로드 실패:", e);
  }
};

// 이미지 캐싱 로직 (중복 호출 방지)
const loadImagesForDmiObjects = async (objList) => {
  if (!objList || objList.length === 0) return;

  // 1. DMI 객체의 model_type 수집
  const modelTypesToLoad = new Set();

  // 2. BOX 이미지 필요 여부 체크
  let needBox = false;

  objList.forEach(obj => {
    // 일반 모델 타입
    if (obj.model_type && !imageUrlsCache.value.has(obj.model_type)) {
      modelTypesToLoad.add(obj.model_type);
    }

    // 박스 사용 여부 확인
    const isBox = obj.box_is_use || obj.boxIsUse;
    if (isBox) {
      needBox = true;
    }
  });

  // 3. BOX 이미지가 필요하고 캐시에 없다면 추가
  if (needBox && !imageUrlsCache.value.has('BOX')) {
    modelTypesToLoad.add('BOX');
  }

  if (modelTypesToLoad.size === 0) return;

  await Promise.all([...modelTypesToLoad].map(fetchAndCacheImage));
};

// [API] 이미지 Blob 다운로드
const fetchAndCacheImage = async (modelType) => {
  if (!modelType) return;

  // 중복 요청 방지를 위해 일단 null로 세팅
  imageUrlsCache.value.set(modelType, null);

  const url = `/status_board_dmt/download/image?lcId=${lcId.value}&modelType=${modelType}&dimension=2D`;
  try {
    const response = await apiClient.get(url, { responseType: 'blob' });
    if (response.data && response.data.size > 0 && response.headers['content-type']?.startsWith('image')) {
      const blob = new Blob([response.data], { type: response.headers['content-type'] });
      const urlObject = URL.createObjectURL(blob);
      imageUrlsCache.value.set(modelType, urlObject);
    }
  } catch (error) {
    console.warn(`이미지 로드 실패 [${modelType}]`);
  }
};

// 자동 갱신 (Polling) 시작
const startAutoRefresh = () => {
  // 최초 실행
  if (activePageId.value) loadDmiObjects(activePageId.value);

  // 5초 주기 실행
  refreshTimer = setInterval(() => {
    if (activePageId.value) {
      loadDmiObjects(activePageId.value);
    }
  }, 1000);
};

// 페이지 탭 선택 시
const selectPage = (pageId) => {
  activePageId.value = pageId;
  // 페이지 바뀌면 즉시 로드
  loadDmiObjects(pageId);
  updateCanvasScale();
};

// 화면 Fit (Scale 계산) 로직
const updateCanvasScale = async () => {
  await nextTick();
  if (!stageContainerRef.value || !activePage.value) return;

  const containerWidth = stageContainerRef.value.clientWidth;
  const containerHeight = stageContainerRef.value.clientHeight;
  const padding = 20; // 여백

  const availableWidth = Math.max(0, containerWidth - padding);
  const availableHeight = Math.max(0, containerHeight - padding);

  const pageWidth = activePage.value.lengthX || 1920;
  const pageHeight = activePage.value.lengthY || 1080;

  // 가로 비율과 세로 비율 중 더 작은 쪽을 선택하여 전체가 보이도록 함
  const scaleX = availableWidth / pageWidth;
  const scaleY = availableHeight / pageHeight;

  canvasScale.value = Math.min(scaleX, scaleY);
};

// 개별 객체 스타일 반환 (computed에서 계산된 _final 값 사용)
const getObjectStyle = (obj) => {
  const dims = { width: 100, height: 100 };

  // Flip 처리 (scale에 -1 곱하기)
  const scaleX = obj._finalScaleX * (obj._finalFlipH ? -1 : 1);
  const scaleY = obj._finalScaleY * (obj._finalFlipV ? -1 : 1);

  return {
    position: 'absolute',
    left: `${obj._finalX}px`,
    bottom: `${obj._finalY}px`,
    width: `${dims.width}px`,
    height: `${dims.height}px`,
    zIndex: obj._renderOrder, // Group의 renderOrder 적용
    transformOrigin: 'left bottom',
    // 2D 회전은 반시계/시계 방향 고려 (보통 웹은 시계방향이 +)
    transform: `rotate(${-obj._finalRotation}deg) scale(${scaleX}, ${scaleY})`,
    cursor: 'pointer', // 클릭 가능 표시
    pointerEvents: 'auto'
  };
};

/**
 * =========================================================
 * Lifecycle Hooks
 * =========================================================
 */
onMounted(() => {
  initialize();

  // 컨테이너 크기 변경 감지 (반응형 Scale)
  if (stageContainerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      window.requestAnimationFrame(updateCanvasScale);
    });
    resizeObserver.observe(stageContainerRef.value);
  }
  window.addEventListener('resize', updateCanvasScale);
});

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer);
  if (resizeObserver) resizeObserver.disconnect();
  window.removeEventListener('resize', updateCanvasScale);

  // 이미지 URL 해제 (메모리 누수 방지)
  imageUrlsCache.value.forEach((url) => {
    if (url) URL.revokeObjectURL(url);
  });
});

// 활성 페이지가 바뀌면 스케일 재계산
watch(activePage, updateCanvasScale);
</script>

<style scoped>
/* 전체 컨테이너 */
.dashboard-viewer {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f4f7f6;
  overflow: hidden;
}

/* 1. 상단 탭 스타일 */
.viewer-header {
  flex-shrink: 0;
  display: flex;
  background-color: #ffffff;
  border-bottom: 1px solid #dcdfe6;
  padding: 0 10px;
  height: 50px;
  align-items: center;
}

.page-tab {
  padding: 8px 16px;
  margin-right: 4px;
  cursor: pointer;
  border-radius: 4px 4px 0 0;
  font-weight: 500;
  color: #606266;
  transition: all 0.3s;
}

.page-tab:hover {
  background-color: #f0f2f5;
}

.page-tab.active {
  color: #409eff;
  border-bottom: 2px solid #409eff;
  background-color: #ecf5ff;
}

/* 2. 메인 바디 스타일 */
.viewer-body {
  flex-grow: 1;
  position: relative; /* 중요: 내부 absolute 요소 기준점 */
  background-color: #eef0f4;
  overflow: hidden;
  width: 100%;
  height: 100%;
}

.stage-wrapper {
  width: 100%;
  height: 100%;
}

/* 3. 캔버스 (Stage) 스타일 */
.stage {
  /* JS에서 inline style로 width, height, transform 제어 */
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

/* 4. 객체 스타일 */
.dmi-object {
  transition: filter 0.2s ease;
}
.dmi-object:hover {
  filter: brightness(1.2) drop-shadow(0 0 5px rgba(64, 158, 255, 0.8));
}
.dmi-object.is-selected {
  filter: brightness(1.3) drop-shadow(0 0 8px #409eff);
  outline: 2px solid #409eff;
  outline-offset: 4px;
  border-radius: 2px;
}

.object-image {
  width: 100%;
  height: 100%;
  display: block;
}
.main-image {
  position: relative;
  z-index: 0;
}
.box-overlay {
  position: absolute;
  top: 15%; /* 중앙 정렬 계산: (100% - width or height) / 2 */
  left: 15%;
  width: 70%;
  height: 70%;
  z-index: 1;
  object-fit: contain; /* 이미지 비율이 찌그러지지 않게 하려면 추가 */
}

.object-placeholder {
  width: 100%;
  height: 100%;
  background-color: rgba(100, 180, 255, 0.2);
  border: 1px dashed rgba(64, 158, 255, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 12px;
}

/* 5. 상태값 표시 스타일 */
/* [정상 - 1: 녹색] */
.status-normal .object-image {
  filter: drop-shadow(0 0 5px rgba(103, 194, 58, 0.8));
}
.status-normal .object-placeholder {
  background-color: rgba(0, 255, 0, 0.3);
  border-color: green;
  color: darkgreen;
}

/* [작업 중 - 2: 주황색] */
.status-working .object-image {
  filter: sepia(0.5) saturate(200%) hue-rotate(10deg) drop-shadow(0 0 5px rgba(230, 162, 60, 0.8));
}
.status-working .object-placeholder {
  background-color: rgba(230, 162, 60, 0.3);
  border-color: orange;
  color: darkorange;
}

/* [에러 - 9: 빨간색] */
.status-error .object-image {
  filter: sepia(1) saturate(500%) hue-rotate(-50deg) drop-shadow(0 0 5px rgba(255, 0, 0, 0.8));
}
.status-error .object-placeholder {
  background-color: rgba(255, 0, 0, 0.3);
  border-color: red;
  color: darkred;
}

/* 6. 상세 정보 패널 스타일 (Glassmorphism & Modern Dark) */
.info-panel {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 320px;
  max-height: calc(100% - 40px);
  background: rgba(30, 34, 45, 0.95); /* 어두운 다크 테마 */
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  color: #e5eaf3;
  overflow: hidden;
}

.info-header {
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.info-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #409eff;
}

.close-btn {
  background: none;
  border: none;
  color: #909399;
  font-size: 24px;
  cursor: pointer;
  line-height: 1;
}

.info-content {
  padding: 20px;
  overflow-y: auto;
}

.info-section label {
  display: block;
  font-size: 0.75rem;
  color: #858585;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 0.9rem;
}

.info-row .label {
  color: #909399;
}

.info-row .value {
  font-weight: 500;
}

.info-row .value.highlight {
  color: #67c23a; /* 작업번호 강조색 */
}

.divider {
  border: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  margin: 15px 0;
}

/* 재고 리스트 스타일 */
.stock-list-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stock-item {
  background: rgba(255, 255, 255, 0.03);
  padding: 10px;
  border-radius: 8px;
  border-left: 3px solid #409eff;
}

.stock-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.item-name {
  font-weight: 600;
  font-size: 0.95rem;
}

.item-qty {
  color: #e6a23c;
  font-weight: bold;
}

.item-code {
  font-size: 0.8rem;
  color: #606266;
}

/* 로딩 애니메이션 */
.info-loader {
  padding: 40px;
  text-align: center;
}
.spinner {
  width: 30px;
  height: 30px;
  border: 3px solid rgba(64, 158, 255, 0.2);
  border-top-color: #409eff;
  border-radius: 50%;
  animation: spin 1s infinite linear;
  margin: 0 auto 10px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-section {
  background: rgba(245, 108, 108, 0.1); /* 붉은 배경 */
  border: 1px solid rgba(245, 108, 108, 0.3);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 20px;
}

.error-header {
  color: #f56c6c; /* Element UI Danger Color */
  font-weight: bold;
  font-size: 1rem;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.error-code {
  color: #f56c6c;
  font-weight: 800;
  font-size: 1.1rem;
}

.error-message-box {
  background: rgba(0, 0, 0, 0.2);
  padding: 8px;
  border-radius: 4px;
  color: #ffcccc;
  font-size: 0.9rem;
  margin-top: 8px;
  line-height: 1.4;
  white-space: pre-wrap; /* 줄바꿈 유지 */
}

.error-divider {
  border-top-color: rgba(245, 108, 108, 0.2);
  margin-top: 15px;
  margin-bottom: 5px;
}
</style>
