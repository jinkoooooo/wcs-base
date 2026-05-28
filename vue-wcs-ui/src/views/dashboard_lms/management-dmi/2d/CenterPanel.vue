<template>
  <div class="center-panel-container">

    <div class="toolbar">
      <div class="toolbar-item">
        <label>BG Color:</label>
        <input type="color" v-model="localPage.backgroundColor" @change="emitPageUpdate" />
        <input type="text" v-model="localPage.backgroundColor" class="text-input" @change="emitPageUpdate" />
      </div>
      <div class="toolbar-item">
        <label>Width:</label>
        <input type="number" v-model.number="localPage.lengthX" class="number-input" @change="emitPageUpdate" /> px
      </div>
      <div class="toolbar-item">
        <label>Height:</label>
        <input type="number" v-model.number="localPage.lengthY" class="number-input" @change="emitPageUpdate" /> px
      </div>
    </div>

    <div class="stage-outer-wrapper" ref="stageContainerRef">
      <div
        class="stage"
        ref="stageRef"
        :style="stageStyle"
        @mousedown.self="onStageMouseDown"
        @dragover.prevent
        @drop="handleDropOnStage"
      >
        <div
          v-for="obj in sortedInternalObjects"
          :key="obj.id"
          class="dmi-object"
          :class="{ selected: selectedObjectIds.has(obj.id) }"
          :style="getObjectStyle(obj)"
          @mousedown.stop="onObjectMouseDown($event, obj)"
        >
          <img
            v-if="imageUrlsCache.get(obj.model_type)"
            :src="imageUrlsCache.get(obj.model_type)"
            :alt="`${obj.group_code} 이미지`"
            class="object-image"
            draggable="false"
          />
          <div v-else class="object-placeholder">
            {{ obj.group_code }}
          </div>
        </div>

        <div v-if="isSelecting" class="selection-box" :style="selectionBoxStyle"></div>
      </div>
    </div>

    <div class="footer">
      <button @click="handleSave" class="save-button">저장</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { buildUUID } from '@/utils/uuid';
import { apiClient } from '/@/api/common/api';

/**
 * 상태 변수
 */
const props = defineProps({
  page: { type: Object, required: true },
  lcId: { type: String, required: true },
});

const emit = defineEmits(['update:page', 'update:objects', 'save', 'object-selected']);
const imageUrlsCache = ref(new Map());
const stageContainerRef = ref(null);
const stageRef = ref(null);
const canvasScale = ref(1);
const internalObjects = ref([]);
const originalDmiMap = ref(new Map());
const localPage = computed(() => props.page);

const sortedInternalObjects = computed(() => {
  return [...internalObjects.value].sort((a, b) => (a.render_order || 0) - (b.render_order || 0));
});

const selectedObjectIds = ref(new Set());
let dragStartPositions = new Map();
let isDraggingObjects = false;
let dragStartMouse = { x: 0, y: 0 };
const isSelecting = ref(false);
const selectionStart = ref({ x: 0, y: 0 }); // 캔버스 기준 좌표
const selectionCurrent = ref({ x: 0, y: 0 });


/**
 * 이미지 로딩 로직
 */
const fetchAndCacheImage = async (modelType) => {
  const cacheKey = modelType;
  if (imageUrlsCache.value.has(cacheKey)) return imageUrlsCache.value.get(cacheKey);
  if (!props.lcId || !modelType) {
    imageUrlsCache.value.set(cacheKey, null);
    return null;
  }
  const url = `/status_board_dmt/download/image?lcId=${props.lcId}&modelType=${modelType}&dimension=2D`;
  try {
    const response = await apiClient.get(url, { responseType: 'blob' });
    if (!response.data || response.data.size === 0 || !response.headers['content-type']?.startsWith('image')) {
      imageUrlsCache.value.set(cacheKey, null);
      return null;
    }
    if (imageUrlsCache.value.has(cacheKey) && imageUrlsCache.value.get(cacheKey)) {
      URL.revokeObjectURL(imageUrlsCache.value.get(cacheKey));
    }
    const blob = new Blob([response.data], { type: response.headers['content-type'] });
    const urlObject = URL.createObjectURL(blob);
    imageUrlsCache.value.set(cacheKey, urlObject);
    return urlObject;
  } catch (error) {
    imageUrlsCache.value.set(cacheKey, null);
    return null;
  }
};

const loadImagesForDmiObjects = async (objects) => {
  if (!objects || objects.length === 0) return;
  const uniqueModelTypes = [...new Set(objects.map(obj => obj.model_type))];
  await Promise.all(
    uniqueModelTypes.map(modelType => fetchAndCacheImage(modelType))
  );
};

watch(internalObjects, (newObjects) => {
  if (newObjects && newObjects.length > 0) {
    loadImagesForDmiObjects(newObjects);
  }
}, { immediate: true });


/**
 * 캔버스 크기 관리 로직
 */
const stageStyle = computed(() => {
  const width = props.page.lengthX || 1920;
  const height = props.page.lengthY || 1080;

  return {
    position: 'absolute', // 부모의 Flex 영향을 받지 않도록 절대 위치 사용
    width: `${width}px`,
    height: `${height}px`,
    backgroundColor: props.page.backgroundColor,

    // 중앙 정렬: top/left 50% 이동 후 translate로 본인 크기 반만큼 되돌림
    top: '50%',
    left: '50%',

    // Scale 적용 (translate와 순서 중요)
    transform: `translate(-50%, -50%) scale(${canvasScale.value})`,
    transformOrigin: 'center center', // 중심점 기준 스케일
    boxSizing: 'border-box' // 테두리가 사이즈에 포함되도록 설정
  };
});

const updateCanvasScale = async () => {
  await nextTick();
  if (!stageContainerRef.value || !props.page.lengthX || !props.page.lengthY) return;

  // 컨테이너 크기 (여백 40px 제외)
  const containerWidth = stageContainerRef.value.clientWidth;
  const containerHeight = stageContainerRef.value.clientHeight;
  const padding = 40;
  const availableWidth = Math.max(0, containerWidth - padding);
  const availableHeight = Math.max(0, containerHeight - padding);

  const pageWidth = props.page.lengthX;
  const pageHeight = props.page.lengthY;

  if (pageWidth <= 0 || pageHeight <= 0) {
    canvasScale.value = 1;
    return;
  }

  // 가로 비율과 세로 비율 중 더 작은 쪽을 선택 (Contain 방식)
  const scaleX = availableWidth / pageWidth;
  const scaleY = availableHeight / pageHeight;

  canvasScale.value = Math.min(scaleX, scaleY);
};

watch(() => [props.page.lengthX, props.page.lengthY, props.page.backgroundColor], updateCanvasScale, { immediate: true });

let resizeObserver;
onMounted(() => {
  if (stageContainerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      // requestAnimationFrame을 사용하여 ResizeObserver 루프 오류 방지 및 부드러운 업데이트
      window.requestAnimationFrame(updateCanvasScale);
    });
    resizeObserver.observe(stageContainerRef.value);
  }
  window.addEventListener('resize', updateCanvasScale);
  window.addEventListener('keydown', handleKeyDown);
  updateCanvasScale(); // 초기 실행
});

onUnmounted(() => {
  if (resizeObserver) resizeObserver.disconnect();
  window.removeEventListener('resize', updateCanvasScale);
  window.removeEventListener('mousemove', onWindowMouseMove);
  window.removeEventListener('mouseup', onWindowMouseUp);
  imageUrlsCache.value.forEach(url => { if (url) URL.revokeObjectURL(url); });
});

const emitPageUpdate = () => {
  updateCanvasScale();
};


/**
 * 객체 랜더링 스타일
 */
const getObjectStyle = (obj) => {
  const dims = { width: 100, height: 100 };

  const scaleX = obj.scale_x_2d * (obj.flip_horizontal_2d ? -1 : 1);
  const scaleY = obj.scale_y_2d * (obj.flip_vertical_2d ? -1 : 1);

  return {
    position: 'absolute',
    left: `${obj.position_x_2d}px`,
    bottom: `${obj.position_y_2d}px`,
    width: `${dims.width}px`,
    height: `${dims.height}px`,
    zIndex: obj.render_order || 1,
    transformOrigin: 'left bottom',
    transform: `rotate(${-obj.rotation_2d}deg) scale(${scaleX}, ${scaleY})`,
  };
};


/**
 * 선택 박스 스타일 계산
 * selectionStart/Current는 Canvas 좌표계(Scale이 적용되기 전 원본 크기 좌표) 기준
 * 하지만 Y축이 bottom 기준이므로 top 기준으로 변환해서 그려야 함
 */
const selectionBoxStyle = computed(() => {
  if (!isSelecting.value) return {};

  // Y축 변환 (Bottom-Up -> Top-Down)
  // selectionStart.y, selectionCurrent.y는 Top-Left 기준 픽셀값(screen -> canvas 변환된 값)

  const x = Math.min(selectionStart.value.x, selectionCurrent.value.x);
  const y = Math.min(selectionStart.value.y, selectionCurrent.value.y);
  const width = Math.abs(selectionCurrent.value.x - selectionStart.value.x);
  const height = Math.abs(selectionCurrent.value.y - selectionStart.value.y);

  return {
    position: 'absolute',
    left: `${x}px`,
    top: `${y}px`,
    width: `${width}px`,
    height: `${height}px`,
    border: '1px dashed #409eff',
    backgroundColor: 'rgba(64, 158, 255, 0.2)',
    pointerEvents: 'none', // 마우스 이벤트 투과
    zIndex: 9999
  };
});


/**
 * 좌표 변환 유틸리티
 * Screen 좌표(MouseEvent) -> Canvas 좌표(Stage 내부)
 */
const getCanvasCoordinates = (clientX, clientY) => {
  const stageRect = stageRef.value.getBoundingClientRect();
  const mouseX = clientX - stageRect.left;
  const mouseY = clientY - stageRect.top;

  // Scale 보정
  return {
    x: mouseX / canvasScale.value,
    y: mouseY / canvasScale.value
  };
};


/**
 * Event: 영역 선택 (Background MouseDown)
 */
const onStageMouseDown = (event) => {
  // 이미 선택된게 있고, Shift 키가 없으면 선택 해제
  if (!event.shiftKey) {
    deselectAll();
  }

  isSelecting.value = true;
  const coords = getCanvasCoordinates(event.clientX, event.clientY);
  selectionStart.value = coords;
  selectionCurrent.value = coords;

  window.addEventListener('mousemove', onWindowMouseMove);
  window.addEventListener('mouseup', onWindowMouseUp);
};


/**
 * Event: 객체 드래그 시작 (Object MouseDown)
 */
const onObjectMouseDown = (event, obj) => {
  // 현재 클릭한 객체가 이미 선택된 그룹에 없다면,
  // (Shift 키가 안 눌린 경우) 기존 선택 해제하고 얘만 선택
  if (!selectedObjectIds.value.has(obj.id)) {
    if (!event.shiftKey) {
      selectedObjectIds.value.clear();
    }
    selectedObjectIds.value.add(obj.id);
  } else {
    // 이미 선택된 그룹에 포함된 애를 클릭했는데 Shift를 누르면 제거
    if (event.shiftKey) {
      selectedObjectIds.value.delete(obj.id);
      emitSelectedObjects();
      return; // 드래그 시작 안 함
    }
  }

  // 선택된 상태 업데이트
  emitSelectedObjects();

  // 드래그 준비
  isDraggingObjects = true;
  dragStartMouse = { x: event.clientX, y: event.clientY };

  // 선택된 모든 객체의 초기 위치 저장
  dragStartPositions.clear();
  internalObjects.value.forEach(o => {
    if (selectedObjectIds.value.has(o.id)) {
      dragStartPositions.set(o.id, { x: o.position_x_2d, y: o.position_y_2d });
    }
  });

  window.addEventListener('mousemove', onWindowMouseMove);
  window.addEventListener('mouseup', onWindowMouseUp);
};


/**
 * Event: Global Mouse Move (영역 선택 or 객체 이동)
 */
const onWindowMouseMove = (event) => {
  // 1. 영역 선택 중
  if (isSelecting.value) {
    selectionCurrent.value = getCanvasCoordinates(event.clientX, event.clientY);
    return;
  }

  // 2. 객체 이동 중
  if (isDraggingObjects) {
    const deltaX = event.clientX - dragStartMouse.x;
    const deltaY = event.clientY - dragStartMouse.y;

    const canvasDeltaX = deltaX / canvasScale.value;
    const canvasDeltaY = deltaY / canvasScale.value;

    // 선택된 모든 객체 이동
    internalObjects.value.forEach(obj => {
      if (selectedObjectIds.value.has(obj.id)) {
        const startPos = dragStartPositions.get(obj.id);
        if (startPos) {
          obj.position_x_2d = startPos.x + canvasDeltaX;
          // Y축은 화면상 위쪽이 -(Minus)지만, 데이터 상 좌표계(Bottom-Left)에서는
          // 마우스를 위로 올리면(deltaY < 0) Y값은 증가해야 함.
          // 따라서 빼준다. (화면 Y 증가 -> 데이터 Y 감소)
          obj.position_y_2d = startPos.y - canvasDeltaY;
        }
      }
    });
  }
};

/**
 * 객체의 변형(회전, 스케일)을 고려한 Bounding Box(AABB) 계산 함수
 * - 객체의 4개 꼭짓점에 변환 행렬을 적용하여 실제 차지하는 최소/최대 X, Y를 구합니다.
 */
const getTransformedBoundingBox = (obj) => {
  const width = 100; // 객체 너비 (실제 값에 맞게 수정 필요)
  const height = 100; // 객체 높이

  // 값이 없을 경우 0으로 처리 (NaN 방지)
  const posX = Number(obj.position_x_2d) || 0;
  const posY = Number(obj.position_y_2d) || 0;
  const rot = Number(obj.rotation_2d) || 0;
  const scaleX = Number(obj.scale_x_2d) || 1;
  const scaleY = Number(obj.scale_y_2d) || 1;

  const rad = rot * (Math.PI / 180);
  const cos = Math.cos(rad);
  const sin = Math.sin(rad);

  // Flip 처리
  const sx = scaleX * (obj.flip_horizontal_2d ? -1 : 1);
  const sy = scaleY * (obj.flip_vertical_2d ? -1 : 1);

  // 로컬 좌표 (Pivot: Left-Bottom)
  const corners = [
    { x: 0, y: 0 },
    { x: width, y: 0 },
    { x: width, y: height },
    { x: 0, y: height }
  ];

  // 변환
  const txCorners = corners.map(p => {
    const tx = p.x * sx;
    const ty = p.y * sy;
    const rx = tx * cos - ty * sin;
    const ry = tx * sin + ty * cos;
    return { x: posX + rx, y: posY + ry };
  });

  const xs = txCorners.map(p => p.x);
  const ys = txCorners.map(p => p.y);

  return {
    minX: Math.min(...xs),
    maxX: Math.max(...xs),
    minY: Math.min(...ys),
    maxY: Math.max(...ys)
  };
};


/**
 * Event: Global Mouse Up
 */
const onWindowMouseUp = () => {
  // 1. 영역 선택 로직
  if (isSelecting.value) {
    // 안전장치: 좌표가 없으면 중단
    if (!selectionStart.value || !selectionCurrent.value) {
      isSelecting.value = false;
      return;
    }

    // 화면 좌표 (Screen Coords)
    const startX = selectionStart.value.x;
    const startY = selectionStart.value.y;
    const currX = selectionCurrent.value.x;
    const currY = selectionCurrent.value.y;

    // Y축 변환 (화면 Top-Down -> 데이터 Bottom-Up)
    // page.lengthY가 없으면 0으로 처리되어 NaN 방지
    const pageHeight = props.page?.lengthY || 2000;

    // 선택 박스 영역 계산 (Min/Max 정리)
    const boxX1 = Math.min(startX, currX);
    const boxX2 = Math.max(startX, currX);

    // Y좌표 변환: (PageHeight - 화면Y)
    const dataY1 = pageHeight - startY;
    const dataY2 = pageHeight - currY;

    const boxY1 = Math.min(dataY1, dataY2); // 아래쪽
    const boxY2 = Math.max(dataY1, dataY2); // 위쪽

    internalObjects.value.forEach(obj => {
      const { minX, maxX, minY, maxY } = getTransformedBoundingBox(obj);

      // 충돌 검사 (하나라도 겹치지 않으면 false)
      // NaN이 하나라도 있으면 이 비교문은 엉뚱하게 동작하므로 위에서 Number() 처리함
      const isNotOverlapping =
        maxX < boxX1 ||
        minX > boxX2 ||
        maxY < boxY1 ||
        minY > boxY2;

      if (!isNotOverlapping) {
        selectedObjectIds.value.add(obj.id);
      }
    });

    emitSelectedObjects();
    isSelecting.value = false;
  }

  // 2. 객체 드래그 종료 로직
  if (isDraggingObjects) {
    emit('update:objects', internalObjects.value);
    emitSelectedObjects();
    isDraggingObjects = false;
  }

  window.removeEventListener('mousemove', onWindowMouseMove);
  window.removeEventListener('mouseup', onWindowMouseUp);
};


/**
 * LeftPanel 객체 추가 로직
 */
// Drag&Drop 방식
const handleDropOnStage = (event) => {
  event.preventDefault();
  const itemData = event.dataTransfer.getData('application/json');
  if (!itemData) return;
  const item = JSON.parse(itemData);

  // getBoundingClientRect는 transform이 적용된 후의 실제 화면 좌표를 반환함
  const stageRect = stageRef.value.getBoundingClientRect();

  // Stage 좌측 상단으로부터의 마우스 거리 계산
  const mouseX = event.clientX - stageRect.left;
  const mouseY = event.clientY - stageRect.top;

  // Scale 역보정하여 실제 좌표 계산
  const canvasX = mouseX / canvasScale.value;
  const canvasY_top = mouseY / canvasScale.value;

  // Y축 반전 (좌측 하단 기준 좌표계로 변환)
  const canvasY = props.page.lengthY - canvasY_top;

  addNewObject(item, canvasX, canvasY);
};

// Click 방식
const addNewObject = (item, x, y) => {
  const newDmiObject = {
    id: buildUUID(),
    lc_id: props.lcId,
    model_type: item.model_type,
    model_code: item.model_code || item.group_code,
    group_type: item.group_type,
    group_code: item.group_code,
    is_use: item.is_use,
    render_page_id: props.page.id,
    position_x_2d: x,
    position_y_2d: y,
    scale_x_2d: item.scaleX2d,
    scale_y_2d: item.scaleY2d,
    rotation_2d: item.rotation2d,
    flip_horizontal_2d: item.flipHorizontal2d,
    flip_vertical_2d: item.flipVertical2d,
    render_order: item.render_order || 1,
  };

  internalObjects.value.push(newDmiObject);
  fetchAndCacheImage(newDmiObject.model_type);

  // 신규 추가 시 기존 선택 해제 후 얘만 선택
  selectedObjectIds.value.clear();
  selectedObjectIds.value.add(newDmiObject.id);
  emitSelectedObjects();
};


/**
 * 상위 객체 Event 송신
 */
const deselectAll = () => {
  selectedObjectIds.value.clear();
  emitSelectedObjects();
};

// RightPanel로 보낼 때는 Array 형태로 변환해서 보냄
const emitSelectedObjects = () => {
  const selectedObjs = internalObjects.value.filter(o => selectedObjectIds.value.has(o.id));
  emit('object-selected', JSON.parse(JSON.stringify(selectedObjs)));
};


/**
 * 키보드 이벤트 (Delete, 방향키)
 */
const handleKeyDown = (event) => {
  // Input 입력 중이면 무시
  if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement) return;

  // 1. 일괄 삭제
  if ((event.key === 'Delete' || event.key === 'Backspace') && selectedObjectIds.value.size > 0) {
    event.preventDefault();
    deleteSelectedObjects();
  }

  // 2. 방향키 이동 (일괄 +1/-1)
  if (selectedObjectIds.value.size > 0 && ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(event.key)) {
    event.preventDefault();
    moveSelectedObjectsByKeyboard(event.key);
  }
};

const deleteSelectedObjects = () => {
  if (selectedObjectIds.value.size === 0) return;

  internalObjects.value = internalObjects.value.filter(
    obj => !selectedObjectIds.value.has(obj.id)
  );
  deselectAll();
  emit('update:objects', internalObjects.value);
};

const moveSelectedObjectsByKeyboard = (key) => {
  let dx = 0;
  let dy = 0;
  const step = 1; // 1px 씩 이동

  switch (key) {
    case 'ArrowUp': dy = step; break;    // 위로 -> Y 증가
    case 'ArrowDown': dy = -step; break; // 아래로 -> Y 감소
    case 'ArrowLeft': dx = -step; break;
    case 'ArrowRight': dx = step; break;
  }

  let isChanged = false;
  internalObjects.value.forEach(obj => {
    if (selectedObjectIds.value.has(obj.id)) {
      obj.position_x_2d += dx;
      obj.position_y_2d += dy;
      isChanged = true;
    }
  });

  if (isChanged) {
    emitSelectedObjects(); // 좌표 바뀌었으니 RightPanel 업데이트
  }
};

const handleSave = () => {
  const addedList = [];
  const updatedList = [];
  const deletedIds = [];

  // 현재 캔버스에 있는 객체들을 맵으로 변환 (빠른 조회를 위해)
  const currentMap = new Map(internalObjects.value.map(obj => [obj.id, obj]));

  // 1. 추가(Added) 및 수정(Updated) 식별
  for (const obj of internalObjects.value) {
    const orig = originalDmiMap.value.get(obj.id);

    if (!orig) {
      // 원본에 없으면 새로 추가된 객체
      addedList.push(obj);
    } else {
      // 원본이 있으면 속성 비교
      const isChanged =
        obj.position_x_2d !== orig.position_x_2d ||
        obj.position_y_2d !== orig.position_y_2d ||
        obj.scale_x_2d !== orig.scale_x_2d ||
        obj.scale_y_2d !== orig.scale_y_2d ||
        obj.rotation_2d !== orig.rotation_2d ||
        obj.flip_horizontal_2d !== orig.flip_horizontal_2d ||
        obj.flip_vertical_2d !== orig.flip_vertical_2d ||
        obj.render_order !== orig.render_order ||
        obj.is_use !== orig.is_use ||
        obj.model_code !== orig.model_code;

      if (isChanged) {
        updatedList.push(obj);
      }
    }
  }

  // 2. 삭제(Deleted) 식별
  // 원본에는 있는데 현재 캔버스(currentMap)에는 없는 ID 추출
  for (const origId of originalDmiMap.value.keys()) {
    if (!currentMap.has(origId)) {
      deletedIds.push(origId);
    }
  }

  // 3. 변경사항이 하나도 없으면 저장 로직 스킵
  if (addedList.length === 0 && updatedList.length === 0 && deletedIds.length === 0) {
    return alert("변경사항이 없습니다.");
  }

  // 변경된 데이터 페이로드 전송
  const deltaPayload = { addedList, updatedList, deletedIds };
  emit('save', deltaPayload, localPage.value);
};


/**
 * 외부(RightPanel)에서 값이 수정되었을 때
 * updatedObjects: 수정된 객체 리스트 (RightPanel에서 처리해서 줌)
 */
const updateObjectValue = (updatedObjects) => {
  // 1. 들어온 데이터가 배열이 아니면 배열로 변환
  const updates = Array.isArray(updatedObjects) ? updatedObjects : [updatedObjects];

  // 2. 배열을 순회하며 업데이트
  updates.forEach(updated => {
    const index = internalObjects.value.findIndex(obj => obj.id === updated.id);
    if (index !== -1) {
      // 기존 객체 교체 (Vue Reactivity 반영)
      internalObjects.value[index] = updated;
    }
  });

  // 드래그 중이 아닐 때만 수행
  if (!isDragging) {
    emitSelectedObjects();
  }
};

const addExistingObject = (item, x, y) => {
  const newDmiObject = {
    id: item.id,
    lc_id: props.lcId,
    model_type: item.model_type,
    model_code: item.model_code || item.group_code, // model_code가 있으면 사용, 없으면 group_code 사용
    group_type: item.group_type,
    group_code: item.group_code,
    is_use: item.is_use,
    render_page_id: props.page.id,
    position_x_2d: x,
    position_y_2d: y,
    scale_x_2d: item.scaleX2d,
    scale_y_2d: item.scaleY2d,
    rotation_2d: item.rotation2d,
    flip_horizontal_2d: item.flipHorizontal2d,
    flip_vertical_2d: item.flipVertical2d,
    render_order: item.render_order,
  };

  internalObjects.value.push(newDmiObject);

  originalDmiMap.value.set(newDmiObject.id, JSON.parse(JSON.stringify(newDmiObject)));

  fetchAndCacheImage(newDmiObject.model_type);
}

const clearAllObjects = () => {
  internalObjects.value = [];
  originalDmiMap.value.clear();
  deselectAll();
};

defineExpose({ addNewObject, addExistingObject, clearAllObjects, updateObjectValue });
</script>

<style scoped>
.center-panel-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  padding: 8px 10px;
  border-bottom: 1px solid #dcdfe6;
  background-color: #f5f7fa;
  flex-shrink: 0;
}
.toolbar-item { display: flex; align-items: center; margin-right: 20px; font-size: 13px; }
.toolbar-item label { margin-right: 5px; color: #606266; font-weight: 500; }
.toolbar-item input { padding: 4px 8px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 13px; }
.toolbar-item .text-input { width: 70px; margin-left: 5px; }
.toolbar-item .number-input { width: 60px; }

/*
  Flex 정렬(justify-content) 대신 position: relative를 사용하여
  내부의 absolute 요소가 컨테이너 기준으로 위치하도록 함.
*/
.stage-outer-wrapper {
  flex-grow: 1;
  min-height: 0;
  background-color: #f0f2f5;
  position: relative; /* 중요 */
  overflow: hidden;
  padding: 0; /* padding은 JS에서 계산하므로 제거 */
  box-sizing: border-box;
}

/* stage는 JS style 속성에서 absolute로 제어됨 */
.stage {
  border: 1px dashed #909399;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  touch-action: none;
}

.dmi-object { position: absolute; border: 1px solid transparent; cursor: grab; pointer-events: auto; }
.dmi-object:active { cursor: grabbing; }
.dmi-object.selected { border: 2px solid #409eff; outline: 2px solid rgba(64, 158, 255, 0.3); }
.object-image, .object-placeholder { width: 100%; height: 100%; display: block; user-select: none; }
.object-placeholder { background-color: rgba(100, 180, 255, 0.3); border: 1px dashed rgba(64, 158, 255, 0.7); display: flex; justify-content: center; align-items: center; text-align: center; font-size: 12px; color: #333; }

.footer { padding: 8px 10px; border-top: 1px solid #dcdfe6; background-color: #f5f7fa; flex-shrink: 0; text-align: right; }
.save-button { background-color: #409eff; color: white; border: none; padding: 8px 15px; border-radius: 4px; cursor: pointer; font-weight: 500; }
.save-button:hover { background-color: #66b1ff; }

.selection-box {
  pointer-events: none;
}
</style>
