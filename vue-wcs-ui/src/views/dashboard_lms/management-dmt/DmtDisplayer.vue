<template>
  <div class="right-panel">
    <div class="top-controls">
      <a-radio-group v-model:value="viewMode" button-style="solid">
        <a-radio-button value="2D">2D</a-radio-button>
        <a-radio-button value="3D">3D</a-radio-button>
      </a-radio-group>
    </div>

    <div
      class="image-drop-container"
      :class="{ 'drag-over': isDragging }"
      @dragover.prevent="handleDragOver"
      @dragleave.prevent="handleDragLeave"
      @drop.prevent="handleDrop"
    >
      <template v-if="imageUrl">
        <img :src="imageUrl" alt="Displayed Image" class="displayed-image" />
      </template>
      <template v-else>
        <div class="placeholder">
          <p>이미지 파일을 이곳으로 드래그 앤 드롭하세요.</p>
        </div>
      </template>
      <div v-if="isDragging" class="drop-overlay">
        <span>이미지를 여기에 드롭하세요</span>
      </div>
    </div>

    <div class="bottom-controls">
      <a-button type="primary" @click="handleUpload">업로드</a-button>
      <a-button danger @click="handleDelete">삭제</a-button>
      <a-button type="primary" @click="handleSave">저장</a-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, watch } from 'vue';
  import { Button, Radio } from 'ant-design-vue';
  import { apiClient, getCommonPostApi } from '/@/api/common/api';

  const ARadioGroup = Radio.Group;
  const ARadioButton = Radio.Button;
  const AButton = Button;

  // 부모 컴포넌트로부터 이미지 URL을 받기 위한 props 정의
  const emit = defineEmits(['update-data']);
  const props = defineProps({
    data: {
      type: Object,
      required: true,
    },
  });

  // 1. 2D/3D 모드 상태 관리
  const viewMode = ref<'2D' | '3D'>('2D');

  // 3. 표시될 이미지 URL 상태 관리
  const imageUrl = ref<string | null>();

  // 4. 드래그 상태 관리
  const isDragging = ref(false);

  // 5. 센터 코드, 모델 타입 관리
  const lcId = ref<string | null>();
  const modelType = ref<string | null>();

  // 선택한 센터 코드, 모델 유형이 변경될 때마다 내부 imageUrl 값을 동기화
  watch(
    () => props.data,
    async (newVal) => {
      lcId.value = newVal.selectedLcId;
      modelType.value = newVal.selectedModelType;

      await downloadImage();
    },
    { deep: true },
  );

  watch(
    () => viewMode.value,
    async () => {
      await downloadImage();
    },
    { deep: true },
  );

  // 서버에서 이미지 파일 불러오기
  const downloadImage = async () => {
    // 이전에 생성된 Object URL이 있다면 메모리 누수 방지를 위해 해제
    if (imageUrl.value && imageUrl.value.startsWith('blob:')) {
      URL.revokeObjectURL(imageUrl.value);
    }

    // 초기화
    imageUrl.value = null;

    try {
      if (lcId.value && modelType.value) {
        // 1. axios를 사용해 Spring Boot API에 GET 요청
        // ★★★ 중요: 응답 타입을 'blob'으로 지정해야 합니다. ★★★
        const url = `/status_board_dmt/download/image?lcId=${lcId.value}&modelType=${modelType.value}&dimension=${viewMode.value}`;
        const response = await apiClient.get(url, { responseType: 'blob' });

        // 2. 응답으로 받은 이미지 데이터(Blob)를 브라우저가 사용할 수 있는 임시 URL로 변환
        const blob = new Blob([response.data], { type: response.headers['content-type'] });

        // 2-1. 응답으로 받은 이미지가 없을 경우 이미지 초기화 후 종료
        if (response.data.size === 0) {
          imageUrl.value = null;
          return;
        }

        // 3. 생성된 URL을 imageUrl에 할당하여 <img> 태그에 연결
        imageUrl.value = URL.createObjectURL(blob);
      }
    } catch (e) {
      console.error('이미지 로딩 실패:', e);
      // 이미지를 찾지 못했거나(404) 다른 에러 발생 시 imageUrl을 null로 유지
      imageUrl.value = null;
    }
  };

  // --- 버튼 핸들러 함수 ---
  const handleUpload = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = (event: Event) => {
      const target = event.target as HTMLInputElement;
      if (target.files && target.files[0]) {
        const file = target.files[0];
        const reader = new FileReader();
        reader.onload = (e) => {
          imageUrl.value = e.target?.result as string;
        };
        reader.readAsDataURL(file);
      }
    };
    input.click();
  };

  const handleDelete = () => {
    imageUrl.value = null;
  };

  const handleSave = async () => {
    if (!lcId.value || !modelType.value) {
      alert('센터가 선택되지 않았습니다.');
      return;
    }

    const url = '/status_board_dmt/upload/image';
    const param = {
      image_data: imageUrl.value,
      lc_id: lcId.value,
      model_type: modelType.value,
      dimension: viewMode.value,
    };
    const result = await getCommonPostApi(url, param);
    alert(result);
  };

  // --- 드래그 앤 드롭 핸들러 함수 ---
  const handleDragOver = () => {
    isDragging.value = true;
  };
  const handleDragLeave = () => {
    isDragging.value = false;
  };
  const handleDrop = (event: DragEvent) => {
    isDragging.value = false;
    const files = event.dataTransfer?.files;
    if (files && files[0] && files[0].type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        imageUrl.value = e.target?.result as string;
      };
      reader.readAsDataURL(files[0]);
    } else {
      alert('이미지 파일만 드롭할 수 있습니다.');
    }
  };
</script>

<style scoped>
  /* 이전 답변과 동일한 스타일 코드 */
  .right-panel {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    padding: 16px;
    box-sizing: border-box;
    background-color: #fff;
  }
  .top-controls {
    position: absolute;
    top: 16px;
    right: 16px;
    z-index: 10;
  }
  .bottom-controls {
    position: absolute;
    bottom: 24px;
    right: 24px;
    display: flex;
    gap: 8px;
    z-index: 10;
  }
  .image-drop-container {
    flex-grow: 1;
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    background-color: #fafafa;
    border: 2px dashed #d9d9d9;
    border-radius: 4px;
    transition: border-color 0.3s, background-color 0.3s;
    position: relative;
    overflow: hidden;
  }
  .image-drop-container.drag-over {
    border-color: #1890ff;
    background-color: #e6f7ff;
  }
  .displayed-image {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
  }
  .placeholder {
    text-align: center;
    color: #8c8c8c;
  }
  .drop-overlay {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 109, 255, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 1.5rem;
    color: white;
    font-weight: bold;
    pointer-events: none;
  }
</style>
