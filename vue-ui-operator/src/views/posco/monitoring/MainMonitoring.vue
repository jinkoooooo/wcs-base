<template>
  <!-- Tablet 스크롤 방지를 위해 touch-action: none 적용 -->
  <div
    ref="containerRef"
    class="monitoring-container"
    @mousedown="startDrag"
    @mousemove="onDrag"
    @mouseup="endDrag"
    @mouseleave="endDrag"
    @wheel.prevent="onWheel"
    @touchstart.prevent="startTouch"
    @touchmove.prevent="onTouch"
    @touchend="endTouch"
  >
    <MonitoringHeader @cameraSelected="handleCameraMove" />

    <div
      class="canvas-board"
      :style="{
        transform: `translate(${translateX}px, ${translateY}px) scale(${scale})`
      }"
    >
      <template v-for="equip in equipList" :key="equip.equip_id">
        <AGV
          v-if="equip.equip_type === 'AGV'"
          :equip="equip"
          @equipClicked="onEquipClicked"
        />
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getCommonGetApi } from "@/api/CommonApi";
import MonitoringHeader from './header/MonitoringHeader.vue';
import AGV from './object/AGV.vue';

const CANVAS_WIDTH = 3000;
const CANVAS_HEIGHT = 1500;

const containerRef = ref(null);
const equipList = ref([]);

// Transform 상태
const scale = ref(1);
const translateX = ref(0);
const translateY = ref(0);

// Scale 제한
const minScale = ref(0.1);
const maxScale = ref(3.0); // 최대 3배 확대

// 드래그 상태
let isDragging = false;
let startX = 0;
let startY = 0;

// 터치 줌 상태
let initialDistance = 0;
let initialScale = 1;

// 타이머 변수 선언
let pollingInterval = null;

// 현재 카메라 모드 상태 저장
const currentCameraMode = ref('전체');

onMounted(async () => {
  await fetchEquipList();
  initCanvas();
  // 윈도우 리사이즈 시 비율 재계산 (화면 회전 등 대비)
  window.addEventListener('resize', initCanvas);

  pollingInterval = setInterval(async () => {
    await fetchEquipList();
  }, 1000);
});

onUnmounted(() => {
  if (pollingInterval) {
    clearInterval(pollingInterval);
  }
  window.removeEventListener('resize', initCanvas);
});

const fetchEquipList = async () => {
  try {
    const response = await getCommonGetApi('/tbMcsEquip/getEquipList');
    equipList.value = response.data || response;
  } catch (error) {
    console.error("설비 목록을 불러오는데 실패했습니다:", error);
  }
};

const initCanvas = () => {
  if (!containerRef.value) return;
  const { clientWidth, clientHeight } = containerRef.value;

  // 화면 안에 도화지가 다 들어가도록 최소 스케일 계산 (Contain 방식)
  minScale.value = Math.min(clientWidth / CANVAS_WIDTH, clientHeight / CANVAS_HEIGHT);
  scale.value = minScale.value;

  // 정중앙 배치
  translateX.value = (clientWidth - CANVAS_WIDTH * scale.value) / 2;
  translateY.value = (clientHeight - CANVAS_HEIGHT * scale.value) / 2;
};

// 화면 이탈(빈 공간) 방지 로직
const applyBounds = () => {
  if (!containerRef.value) return;
  const { clientWidth, clientHeight } = containerRef.value;

  const scaledWidth = CANVAS_WIDTH * scale.value;
  const scaledHeight = CANVAS_HEIGHT * scale.value;

  // 가로 경계 제한: 캔버스가 화면보다 작으면 중앙, 크면 화면 밖으로 벗어나지 못하게 제한
  if (scaledWidth <= clientWidth) {
    translateX.value = (clientWidth - scaledWidth) / 2;
  } else {
    translateX.value = Math.min(0, Math.max(translateX.value, clientWidth - scaledWidth));
  }

  // 세로 경계 제한
  if (scaledHeight <= clientHeight) {
    translateY.value = (clientHeight - scaledHeight) / 2;
  } else {
    translateY.value = Math.min(0, Math.max(translateY.value, clientHeight - scaledHeight));
  }
};

// -----------------------------------------
// 이벤트 핸들러 (Mouse / Wheel)
// -----------------------------------------
const startDrag = (e) => {
  if (currentCameraMode.value !== '전체') return;

  isDragging = true;
  startX = e.clientX;
  startY = e.clientY;
};

const onDrag = (e) => {
  if (!isDragging) return;
  translateX.value += e.clientX - startX;
  translateY.value += e.clientY - startY;
  startX = e.clientX;
  startY = e.clientY;
  applyBounds();
};

const endDrag = () => {
  isDragging = false;
};

const onWheel = (e) => {
  if (currentCameraMode.value !== '전체') return;

  const zoomFactor = 1.1;
  const direction = e.deltaY > 0 ? -1 : 1;
  let newScale = direction > 0 ? scale.value * zoomFactor : scale.value / zoomFactor;

  newScale = Math.max(minScale.value, Math.min(newScale, maxScale.value));

  // 마우스 포인터 위치를 기준으로 줌 인/아웃
  const rect = containerRef.value.getBoundingClientRect();
  const mouseX = e.clientX - rect.left;
  const mouseY = e.clientY - rect.top;

  const originX = (mouseX - translateX.value) / scale.value;
  const originY = (mouseY - translateY.value) / scale.value;

  scale.value = newScale;
  translateX.value = mouseX - originX * scale.value;
  translateY.value = mouseY - originY * scale.value;

  applyBounds();
};

// -----------------------------------------
// 이벤트 핸들러 (Touch - Tablet 지원)
// -----------------------------------------
const getTouchDistance = (touches) => {
  return Math.hypot(
      touches[0].clientX - touches[1].clientX,
      touches[0].clientY - touches[1].clientY
  );
};

const startTouch = (e) => {
  if (currentCameraMode.value !== '전체') return;

  if (e.touches.length === 2) {
    // 핀치 줌 시작
    initialDistance = getTouchDistance(e.touches);
    initialScale = scale.value;
  } else if (e.touches.length === 1) {
    // 드래그 시작
    isDragging = true;
    startX = e.touches[0].clientX;
    startY = e.touches[0].clientY;
  }
};

const onTouch = (e) => {
  if (currentCameraMode.value !== '전체') return;

  if (e.touches.length === 2) {
    // 핀치 줌 처리
    const currentDistance = getTouchDistance(e.touches);
    const scaleFactor = currentDistance / initialDistance;
    let newScale = initialScale * scaleFactor;
    newScale = Math.max(minScale.value, Math.min(newScale, maxScale.value));

    const rect = containerRef.value.getBoundingClientRect();
    const centerX = ((e.touches[0].clientX + e.touches[1].clientX) / 2) - rect.left;
    const centerY = ((e.touches[0].clientY + e.touches[1].clientY) / 2) - rect.top;

    const originX = (centerX - translateX.value) / scale.value;
    const originY = (centerY - translateY.value) / scale.value;

    scale.value = newScale;
    translateX.value = centerX - originX * scale.value;
    translateY.value = centerY - originY * scale.value;
    applyBounds();

  } else if (e.touches.length === 1 && isDragging) {
    // 드래그 처리
    translateX.value += e.touches[0].clientX - startX;
    translateY.value += e.touches[0].clientY - startY;
    startX = e.touches[0].clientX;
    startY = e.touches[0].clientY;
    applyBounds();
  }
};

const endTouch = () => {
  isDragging = false;
};

const handleCameraMove = (selectedZone) => {
  currentCameraMode.value = selectedZone; // 현재 상태 업데이트

  if (!containerRef.value) return;
  const { clientWidth, clientHeight } = containerRef.value;

  if (selectedZone === '전체') {
    // 1배율(화면 맞춤) 및 중앙 정렬 초기화
    initCanvas();

  } else if (selectedZone === '검사실') {
    scale.value = 3.0; // 3배율 확대

    // 포커스할 도화지 상의 좌표
    const focusX = 600;
    const focusY = 900; // ※ 참고: 좌표계가 top 기준일 경우 (x, y), bottom 기준일 경우 (x, CANVAS_HEIGHT - y)

    // 타겟 좌표가 화면 정중앙(clientWidth/2)에 오도록 이동값 계산
    translateX.value = (clientWidth / 2) - (focusX * scale.value);
    translateY.value = (clientHeight / 2) - (focusY * scale.value);

    // 빈 공간이 보이지 않도록 바운더리 보정
    applyBounds();
  }
};

const onEquipClicked = (equipInfo) => {
  console.log("선택된 설비 타입:", equipInfo.equip_type);
  console.log("선택된 설비 코드:", equipInfo.equip_code);
};
</script>

<style scoped>
.monitoring-container {
  width: 100%;
  height: 100vh; /* 부모 레이아웃에 맞게 조절 가능 */
  overflow: hidden;
  position: relative;
  touch-action: none; /* 브라우저 기본 스크롤(스와이프) 차단 */
  background-color: #2c3e50; /* 배경색 (빈 공간 여백 확인용) */
}

.canvas-board {
  width: 3000px;
  height: 1500px;
  position: absolute;
  transform-origin: top left; /* Transform 기준점을 좌상단으로 고정 */
  background-image: url('도면_배경_이미지가_있다면_추가.png'); /* 센터 레이아웃 도면 등 */
  background-color: #f5f6fa;
  box-shadow: 0 0 20px rgba(0,0,0,0.5);
  will-change: transform; /* 렌더링 성능 최적화 */
}
</style>