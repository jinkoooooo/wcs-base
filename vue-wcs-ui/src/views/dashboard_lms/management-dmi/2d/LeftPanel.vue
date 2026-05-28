<template>
  <div class="left-panel-container">
    <div v-if="isLoading" class="loading-indicator"> Loading... </div>
    <div v-else class="item-list">
      <div
        v-for="item in dmgItems"
        :key="item.id"
        class="item-card"
        draggable="true"
        @click="handleItemClick(item)"
        @dragstart="handleDragStart($event, item)"
      >
        <div class="image-container">
          <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.group_code" />
          <div v-else class="image-placeholder">No Image</div>
        </div>
        <div class="item-info">
          {{ item.group_type }} : {{ item.group_code }}
        </div>

        <button class="batch-btn" @click.stop="openBatchModal(item)">
          일괄
        </button>
      </div>
    </div>

    <div v-if="showBatchModal" class="modal-overlay">
      <div class="modal-content">
        <h3>일괄 생성 ({{ selectedBatchItem?.group_code }})</h3>

        <div class="form-grid">
          <div class="form-group">
            <label>Row (행)</label>
            <input type="number" v-model.number="batchForm.rows" min="1" />
          </div>
          <div class="form-group">
            <label>Column (열)</label>
            <input type="number" v-model.number="batchForm.cols" min="1" />
          </div>

          <div class="form-group">
            <label>Start Pos X</label>
            <input type="number" v-model.number="batchForm.posX" />
          </div>
          <div class="form-group">
            <label>Start Pos Y</label>
            <input type="number" v-model.number="batchForm.posY" />
          </div>
          <div class="form-group">
            <label>Interval X</label>
            <input type="number" v-model.number="batchForm.intervalX" />
          </div>
          <div class="form-group">
            <label>Interval Y</label>
            <input type="number" v-model.number="batchForm.intervalY" />
          </div>

          <div class="form-group full-width">
            <label>Tail Model Code (접미사)</label>
            <input type="text" v-model="batchForm.tailCode" placeholder="ex) _01" />
          </div>

          <div class="form-group">
            <label>Scale X</label>
            <input type="number" v-model.number="batchForm.scaleX" step="0.1" />
          </div>
          <div class="form-group">
            <label>Scale Y</label>
            <input type="number" v-model.number="batchForm.scaleY" step="0.1" />
          </div>
          <div class="form-group">
            <label>Rotation</label>
            <input type="number" v-model.number="batchForm.rotation" />
          </div>
          <div class="form-group">
            <label>Render Order</label>
            <input type="number" v-model.number="batchForm.renderOrder" />
          </div>

          <div class="form-group checkbox-group">
            <label>
              <input type="checkbox" v-model="batchForm.flipH" /> 수평 반전
            </label>
            <label>
              <input type="checkbox" v-model="batchForm.flipV" /> 수직 반전
            </label>
          </div>
        </div>

        <div class="modal-actions">
          <button @click="closeBatchModal">취소</button>
          <button class="primary" @click="handleBatchSubmit">생성</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { onUnmounted, ref, watch, reactive } from 'vue';
  import { apiClient, getCommonGetApi } from '/@/api/common/api';

  const props = defineProps({
    lcId: {
      type: String,
      required: true,
    },
  });

  const emit = defineEmits(['add-object', 'batch-add-objects']);
  const dmgItems = ref([]);
  const isLoading = ref(false);

  const showBatchModal = ref(false);
  const selectedBatchItem = ref(null);
  const batchForm = reactive({
    rows: 1,
    cols: 1,
    posX: 0,
    posY: 0,
    intervalX: 100,
    intervalY: 100,
    scaleX: 1,
    scaleY: 1,
    rotation: 0,
    renderOrder: 1,
    flipH: false,
    flipV: false,
    tailCode: ''
  });

  // 이미지 Blob URL을 가져오는 함수
  // createObjectURL은 메모리 누수를 방지하기 위해 컴포넌트가 파괴될 때 해제해야 합니다.
  const fetchImageAsBlobUrl = async (modelType) => {
    try {
      const url = `/status_board_dmt/download/image?lcId=${props.lcId}&modelType=${modelType}&dimension=2D`;
      const response = await apiClient.get(url, { responseType: 'blob' });

      if (response.data.size === 0) {
        return null; // 이미지가 없는 경우
      }

      const blob = new Blob([response.data], { type: response.headers['content-type'] });
      return URL.createObjectURL(blob);
    } catch (error) {
      console.error(`이미지 로딩 실패 (modelType: ${modelType}):`, error);
      return null;
    }
  };

  // lcId가 변경될 때마다 객체 목록과 이미지를 다시 불러옵니다.
  watch(
    () => props.lcId,
    async (newLcId) => {
      if (!newLcId) return;

      isLoading.value = true;

      // 1. 객체 목록(dmg) 가져오기
      const url = `/status_board_dmg/select/${newLcId}`;
      const response = await getCommonGetApi(url);

      // 2. 각 객체의 이미지를 병렬로 가져오기
      dmgItems.value = await Promise.all(
        response.map(async (item) => {
          const imageUrl = await fetchImageAsBlobUrl(item.model_type);
          return {
            ...item,
            imageUrl,
          };
        }),
      );
      isLoading.value = false;
    },
    { immediate: true },
  ); // 컴포넌트가 마운트될 때 즉시 실행

  // 클릭 이벤트 핸들러
  const handleItemClick = (item) => {
    // 부모에게 객체 정보를 담아 이벤트 발생
    emit('add-object', { ...item });
  };

  // 드래그 시작 이벤트 핸들러
  const handleDragStart = (event, item) => {
    // 드래그 데이터에 객체 정보를 JSON 문자열로 저장
    event.dataTransfer.setData('application/json', JSON.stringify(item));
    event.dataTransfer.effectAllowed = 'copy';
  };

  // 일괄 생성 모달 열기
  const openBatchModal = (item) => {
    selectedBatchItem.value = item;

    // 초기값 설정
    batchForm.rows = 1;
    batchForm.cols = 1;
    batchForm.posX = 0;
    batchForm.posY = 0;
    batchForm.intervalX = 100;
    batchForm.intervalY = 100;
    batchForm.scaleX = item.scaleX2d || 1;
    batchForm.scaleY = item.scaleY2d || 1;
    batchForm.rotation = item.rotation2d || 0;
    batchForm.renderOrder = item.renderOrder || 1;
    batchForm.flipH = item.flipHorizontal2d || false;
    batchForm.flipV = item.flipVertical2d || false;
    batchForm.tailCode = '';

    showBatchModal.value = true;
  };

  // 모달 닫기
  const closeBatchModal = () => {
    showBatchModal.value = false;
    selectedBatchItem.value = null;
  };

  // 일괄 생성 확인 및 이벤트 전송
  const handleBatchSubmit = () => {
    if (!selectedBatchItem.value) return;

    const newObjects = [];
    const groupCode = selectedBatchItem.value.group_code;
    const {
      rows, cols, posX, posY, intervalX, intervalY,
      scaleX, scaleY, rotation, renderOrder, flipH, flipV, tailCode
    } = batchForm;

    // Row, Column 반복
    for (let r = 1; r <= rows; r++) {
      for (let c = 1; c <= cols; c++) {
        // 좌표 계산: 시작점 + (인덱스 * 간격)
        // c는 1부터 시작하므로 0-based index를 위해 -1
        const currentX = posX + (c - 1) * intervalX;
        const currentY = posY + (r - 1) * intervalY;

        // Model Code 생성: group_code + Col(2자리) + "-" + Row(2자리) + Tail
        const colStr = String(c).padStart(2, '0');
        const rowStr = String(r).padStart(2, '0');
        const generatedModelCode = `${groupCode}${colStr}-${rowStr}${tailCode}`;

        // 생성될 객체 정보 구성
        newObjects.push({
          // 원본 DMG 아이템 정보
          ...selectedBatchItem.value,

          // 일괄 생성 오버라이드 속성
          model_code: generatedModelCode,
          positionX2d: currentX,
          positionY2d: currentY,
          scaleX2d: scaleX,
          scaleY2d: scaleY,
          rotation2d: rotation,
          flipHorizontal2d: flipH,
          flipVertical2d: flipV,
          renderOrder: renderOrder
        });
      }
    }

    // 부모에게 리스트 전달
    emit('batch-add-objects', newObjects);
    closeBatchModal();
  };

  // 컴포넌트가 언마운트될 때 생성된 Blob URL들을 메모리에서 해제합니다.
  onUnmounted(() => {
    dmgItems.value.forEach((item) => {
      if (item.imageUrl) {
        URL.revokeObjectURL(item.imageUrl);
      }
    });
  });
</script>

<style scoped>
  .left-panel-container {
    height: 100%;
    display: flex;
    flex-direction: column;
  }
  .loading-indicator {
    margin: auto;
    font-size: 1.2em;
    color: #909399;
  }
  .item-list {
    overflow-y: auto; /* 목록이 길어지면 스크롤바 생성 */
    padding: 5px;
  }
  .item-card {
    display: flex;
    align-items: center;
    padding: 10px;
    border: 1px solid #e9ecef;
    border-radius: 4px;
    margin-bottom: 8px;
    cursor: grab;
    background-color: #fff;
    transition: box-shadow 0.2s, background-color 0.2s;
    position: relative;
  }
  .item-card:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    background-color: #fcfcfc;
  }
  .item-card:active {
    cursor: grabbing;
  }
  .image-container {
    width: 60px;
    height: 60px;
    margin-right: 15px;
    border: 1px solid #f0f2f5;
    display: flex;
    justify-content: center;
    align-items: center;
    background-color: #f8f9fa;
  }
  img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
  }
  .image-placeholder {
    font-size: 12px;
    color: #909399;
  }
  .item-info {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    flex-grow: 1; /* 남은 공간 차지 */
  }

  /* 일괄 생성 버튼 스타일 */
  .batch-btn {
    padding: 4px 8px;
    font-size: 12px;
    background-color: #409eff;
    color: white;
    border: none;
    border-radius: 3px;
    cursor: pointer;
    margin-left: 8px;
  }
  .batch-btn:hover {
    background-color: #66b1ff;
  }

  /* 일괄 생성 모달 스타일 */
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 2000;
  }
  .modal-content {
    background: white;
    padding: 20px;
    border-radius: 8px;
    width: 500px;
    max-width: 90%;
    max-height: 90vh;
    overflow-y: auto;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  }
  .modal-content h3 {
    margin-top: 0;
    margin-bottom: 20px;
    border-bottom: 1px solid #eee;
    padding-bottom: 10px;
  }
  .form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 15px;
  }
  .form-group {
    display: flex;
    flex-direction: column;
  }
  .form-group.full-width {
    grid-column: span 2;
  }
  .form-group label {
    font-size: 12px;
    color: #606266;
    margin-bottom: 4px;
  }
  .form-group input[type="number"],
  .form-group input[type="text"] {
    padding: 6px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
  }
  .checkbox-group {
    flex-direction: row;
    align-items: center;
    gap: 15px;
  }
  .checkbox-group label {
    display: flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    font-size: 13px;
    margin-bottom: 0;
  }
  .modal-actions {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
  .modal-actions button {
    padding: 8px 16px;
    border: 1px solid #dcdfe6;
    background: white;
    border-radius: 4px;
    cursor: pointer;
  }
  .modal-actions button.primary {
    background-color: #409eff;
    color: white;
    border-color: #409eff;
  }
  .modal-actions button:hover {
    opacity: 0.8;
  }
</style>
