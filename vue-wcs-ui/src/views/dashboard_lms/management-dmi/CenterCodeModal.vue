<template>
  <div class="modal-overlay">
    <div class="modal-content">
      <h2 class="text-xl font-semibold text-gray-800">센터 선택</h2>
      <p class="text-gray-600 mb-4">데이터를 조회할 센터를 목록에서 선택해주세요.</p>

      <!-- 로딩 및 에러 상태 표시 -->
      <div v-if="isLoading" class="loading-state">
        <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-blue-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        센터 목록을 불러오는 중...
      </div>
      <div v-else-if="fetchError" class="error-state text-red-600 p-2 border border-red-300 rounded-md bg-red-50">
        센터 목록을 불러오지 못했습니다: {{ fetchError }}
      </div>
      <div v-else>
        <!-- 센터 선택 콤보 박스 -->
        <select
          v-model="selectedLcId"
          class="center-select"
          :disabled="centers.length === 0"
          @keyup.enter="submit"
        >
          <option value="" disabled>-- 센터를 선택하세요 --</option>
          <option
            v-for="center in centers"
            :key="center.lc_id"
            :value="center.lc_id"
          >
            {{ center.lc_nm }} ({{ center.lc_id }})
          </option>
        </select>

        <!-- 선택 값 유효성 검사 에러 메시지 -->
        <p v-if="validationError" class="text-red-500 text-sm mt-2 mb-4">{{ validationError }}</p>

        <!-- 확인 버튼 -->
        <button @click="submit" :disabled="isLoading || centers.length === 0" class="confirm-button">확인</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
// '/@/api/common/api' 경로는 환경에 따라 다를 수 있으나, 기존 코드를 따라 그대로 사용합니다.
import { getCommonGetApi } from '/@/api/common/api';

const emit = defineEmits(['submit']);

const centers = ref([]);
const selectedLcId = ref('');
const isLoading = ref(true);
const fetchError = ref(null);
const validationError = ref(null);

/**
 * 센터 목록을 API에서 불러옵니다.
 */
const fetchCenters = async () => {
  isLoading.value = true;
  fetchError.value = null;

  try {
    const url = '/lms-centers';
    // API 응답 구조에 따라 centers.value에 할당합니다.
    // 여기서는 응답이 직접 배열이라고 가정합니다.
    const response = await getCommonGetApi(url);

    // 응답이 유효한 배열인지 확인
    if (Array.isArray(response.items)) {
      centers.value = response.items;
      // 목록이 있다면 첫 번째 항목을 기본 선택합니다.
      if (centers.value.length > 0) {
        selectedLcId.value = centers.value[0].lc_id;
      }
    } else {
      // API가 배열이 아닌 다른 형태를 반환했을 경우 처리
      centers.value = [];
      fetchError.value = 'API 응답 형식이 올바르지 않습니다.';
    }

  } catch (err) {
    console.error("센터 목록 로딩 오류:", err);
    fetchError.value = '서버에서 데이터를 불러오는 데 실패했습니다.';
  } finally {
    isLoading.value = false;
  }
};

/**
 * 선택된 센터 코드를 상위 컴포넌트로 전달합니다.
 */
const submit = () => {
  validationError.value = null; // 초기화

  if (selectedLcId.value) {
    emit('submit', selectedLcId.value);
  } else {
    validationError.value = '센터를 선택해야 합니다.';
  }
};

// 컴포넌트가 마운트될 때 센터 목록을 불러옵니다.
onMounted(() => {
  fetchCenters();
});
</script>

<style scoped>
/* Tailwind CSS 스타일링이 기본이라고 가정하고, 기존 CSS를 약간 개선했습니다. */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  background-color: #ffffff;
  padding: 30px 40px;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
  text-align: center;
  width: 400px;
  max-width: 90%;
  transition: transform 0.3s ease-out;
}
h2 {
  margin-top: 0;
  margin-bottom: 5px;
  color: #303133;
  font-size: 1.5rem;
}
p {
  color: #606266;
  margin-bottom: 25px;
}
.center-select {
  width: 100%;
  padding: 10px;
  margin-bottom: 20px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  box-sizing: border-box;
  font-size: 1rem;
  appearance: none; /* 기본 OS 스타일 제거 */
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 20 20' fill='%23606266'%3E%3Cpath fill-rule='evenodd' d='M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z' clip-rule='evenodd' /%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-size: 1.5em 1.5em;
  cursor: pointer;
}
.center-select:disabled {
  background-color: #f5f7fa;
  cursor: not-allowed;
}

.confirm-button {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 6px;
  background-color: #409eff;
  color: white;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s ease;
}
.confirm-button:hover:not(:disabled) {
  background-color: #66b1ff;
}
.confirm-button:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.loading-state, .error-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  min-height: 80px;
  font-size: 1rem;
  color: #409eff;
}
</style>
