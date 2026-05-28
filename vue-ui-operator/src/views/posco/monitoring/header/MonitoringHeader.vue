<template>
  <div class="monitoring-header">
    <button class="back-btn" @click="goToRoot" title="메인 화면으로 이동">
      <span class="icon">◀</span>
    </button>

    <img
      src="/images/posco/posco-logo.png"
      alt="POSCO Logo"
      class="logo-img"
      draggable="false"
    />

    <span class="time-text">{{ currentTime }}</span>

    <button class="alarm-btn">
      <span class="icon">🚨</span> 알람 현황
    </button>

    <select
      v-model="selectedCamera"
      @change="onCameraChange"
      class="camera-select"
    >
      <option value="전체">전체</option>
      <option value="검사실">검사실</option>
      <option value="생산라인">생산라인</option>
    </select>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const emit = defineEmits(['cameraSelected']);

const currentTime = ref('');
const selectedCamera = ref('전체'); // 기본값 설정
let timer = null;

// 현재 시간 포맷팅 함수 (YYYY-MM-DD HH:mm:ss)
const updateTime = () => {
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  const hh = String(now.getHours()).padStart(2, '0');
  const min = String(now.getMinutes()).padStart(2, '0');
  const ss = String(now.getSeconds()).padStart(2, '0');

  currentTime.value = `${yyyy}-${mm}-${dd} ${hh}:${min}:${ss}`;
};

onMounted(() => {
  updateTime();
  timer = setInterval(updateTime, 1000); // 1초마다 시간 갱신
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});

// 콤보박스 변경 이벤트 발생
const onCameraChange = () => {
  emit('cameraSelected', selectedCamera.value);
};

const goToRoot = () => {
  window.location.href = '/';
};
</script>

<style scoped>
.monitoring-header {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 100; /* 도화지보다 항상 위에 있도록 설정 */
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 8px 15px;
  background-color: rgba(30, 30, 30, 0.85); /* 캡처 이미지와 유사한 반투명 어두운 배경 */
  border-radius: 6px;
  color: #ffffff;
  pointer-events: auto; /* 터치/마우스 이벤트 활성화 */
}

.logo-img {
  height: 24px;
  object-fit: contain;
}

.time-text {
  font-size: 16px;
  font-family: monospace;
  letter-spacing: 0.5px;
}

.alarm-btn {
  background-color: #555;
  color: #fff;
  border: 1px solid #777;
  padding: 4px 10px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
}
.alarm-btn:hover {
  background-color: #666;
}

.camera-select {
  background-color: #fff;
  color: #333;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  outline: none;
}

.back-btn {
  background-color: transparent;
  color: #fff;
  border: none;
  border-right: 1px solid rgba(255, 255, 255, 0.3); /* 로고와의 구분선 */
  padding: 0 12px 0 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: color 0.2s;
}

.back-btn:hover {
  color: #4facfe;
}
</style>