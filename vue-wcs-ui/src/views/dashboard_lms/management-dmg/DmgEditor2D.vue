<template>
  <BasicModal @register="registerModal">
    <div class="modal-backdrop">
      <div class="modal-container">

        <div class="modal-content">
          <div class="image-pane">
            <div class="image-wrapper">
              <img
                v-if="imageUrl"
                :src="imageUrl"
                alt="속성 변경 이미지"
                :style="imageStyle"
                @load="onImageLoad"
              />
              <div v-else>
                이미지 URL이 없습니다.
              </div>
            </div>
          </div>

          <div class="controls-pane">
            <div class="control-group">
              <label for="xPos">X좌표</label>
              <input id="xPos" type="number" v-model.number="attributes.xPos" />
            </div>
            <div class="control-group">
              <label for="yPos">Y좌표</label>
              <input id="yPos" type="number" v-model.number="attributes.yPos" />
            </div>

            <hr class="divider" />

            <div class="control-group">
              <label for="xScale">X길이 (배율)</label>
              <input id="xScale" type="number" step="0.1" v-model.number="attributes.xScale" />
            </div>
            <div class="control-group">
              <label for="yScale">Y길이 (배율)</label>
              <input id="yScale" type="number" step="0.1" v-model.number="attributes.yScale" />
            </div>

            <hr class="divider" />

            <div class="control-group">
              <label for="rotation">회전값 (시계방향 °)</label>
              <input id="rotation" type="number" v-model.number="attributes.rotation" />
            </div>

            <hr class="divider" />
            <div class="control-group">
              <label>반전</label>
              <div class="checkbox-group">
                <label for="flipHorizontal">
                  <input id="flipHorizontal" type="checkbox" v-model="attributes.flipHorizontal" />
                  좌우반전
                </label>
                <label for="flipVertical">
                  <input id="flipVertical" type="checkbox" v-model="attributes.flipVertical" />
                  상하반전
                </label>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button @click="handleClose">닫기</button>
          <button class="primary" @click="handleSave">저장</button>
        </div>

      </div>
    </div>
  </BasicModal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue';
import { BasicModal, useModalInner } from '@/components/Modal';
import { getCommonGetApi, getCommonPostApi } from '/@/api/common/api';

const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
  setModalProps({ confirmLoading: false });
});

// 부모 컴포넌트로부터 이미지 URL을 전달받기 위한 Props
const props = defineProps({
  data: {
    type: Object,
    required: true,
  },
});

// 부모로부터 받은 imageUrl 값을 동기화
watch(
  () => props.data,
  async (newVal) => {
    imageUrl.value = newVal.initial2DImageUrl;
    centerInfo.value = newVal.center2DInfo;

    const url = `/status_board_dmg/select/${centerInfo.value.lc_id}/${centerInfo.value.group_code}`;
    const response = await getCommonGetApi(url, null);

    attributes.xPos = response.positionX2d;
    attributes.yPos = response.positionY2d;
    attributes.xScale = response.scaleX2d;
    attributes.yScale = response.scaleY2d;
    attributes.rotation = response.rotation2d;
    attributes.flipHorizontal = response.flipHorizontal2d;
    attributes.flipVertical = response.flipVertical2d;
  },
  { deep: true },
);

// 이미지 URL 상태
const imageUrl = ref<string | null>();
const centerInfo = ref<any>();

// 이미지의 원본 크기를 저장할 ref
const naturalSize = reactive({
  width: 0,
  height: 0,
});

// 오른쪽 입력 값들을 관리하는 reactive 객체
const attributes = reactive({
  xPos: 0,
  yPos: 0,
  xScale: 1, // 기본 배율은 1
  yScale: 1, // 기본 배율은 1
  rotation: 0, // 기본 회전값은 0
  flipHorizontal: false, // 좌우반전 상태
  flipVertical: false,   // 상하반전 상태
});

/**
 * 이미지가 로드되었을 때 원본 픽셀 크기를 저장하는 함수
 * @param event 이미지 로드 이벤트
 */
const onImageLoad = (event: Event) => {
  const img = event.target as HTMLImageElement;
  naturalSize.width = img.naturalWidth;
  naturalSize.height = img.naturalHeight;
};

/**
 * attributes 값에 따라 동적으로 이미지 스타일을 계산하는 computed 속성
 */
const imageStyle = computed(() => {
  // 원본 크기를 아직 모를 경우 스타일을 적용하지 않음
  if (naturalSize.width === 0 || naturalSize.height === 0) {
    return {};
  }

  // transform 속성을 배열로 관리하여 동적으로 조합
  const transforms: string[] = [];
  transforms.push(`rotate(${attributes.rotation}deg)`);

  // --- 추가된 부분 ---
  // 좌우반전이 true이면 scaleX(-1) 추가
  if (attributes.flipHorizontal) {
    transforms.push('scaleX(-1)');
  }
  // 상하반전이 true이면 scaleY(-1) 추가
  if (attributes.flipVertical) {
    transforms.push('scaleY(-1)');
  }

  return {
    // X, Y 길이는 원본 크기에 배율을 곱한 값으로 설정
    width: `${naturalSize.width * attributes.xScale}px`,
    height: `${naturalSize.height * attributes.yScale}px`,
    // 회전값 적용
    transform: transforms.join(' '),
  };
});


/**
 * 닫기 버튼 클릭 시 'close' 이벤트를 발생시키는 함수
 */
const handleClose = () => {
  closeModal();
};

/**
 * 저장 버튼 클릭 시 현재 attribute 값들을 payload에 담아 'save' 이벤트를 발생시키는 함수
 */
const handleSave = async () => {
  try {
    const url = '/status_board_dmg/update/attributes2D';
    const param = {
      lc_id: centerInfo.value.lc_id,
      group_code: centerInfo.value.group_code,
      positionX2d: attributes.xPos,
      positionY2d: attributes.yPos,
      scaleX2d: attributes.xScale,
      scaleY2d: attributes.yScale,
      rotation2d: attributes.rotation,
      flipHorizontal2d: attributes.flipHorizontal,
      flipVertical2d: attributes.flipVertical
    }

    console.log(param);
    const response = await getCommonPostApi(url, param);
    if (response) {
      alert("저장 완료");
    }
  }
  catch (error) {
    alert(error.message);
  }
  finally {
    closeModal();
  }
};

</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  width: 80vw; /* 화면 너비의 80% */
  height: 80vh; /* 화면 높이의 80% */
  max-width: 1200px; /* 최대 너비 제한 */
  max-height: 800px; /* 최대 높이 제한 */
  /* ------------------ */
  display: flex;
  flex-direction: column;
  padding: 24px;
}
.modal-content {
  display: flex;
  flex-grow: 1;
  gap: 24px;
  /* 내용이 컨테이너를 넘어가지 않도록 높이 제한 */
  height: calc(100% - 73px); /* footer 높이만큼 빼기 */
  /* ------------------ */
}
.image-pane {
  flex: 1;
  background-color: #f0f2f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  display: flex;
  justify-content: center;
  align-items: center;
  /* 이미지가 영역보다 커지면 스크롤 자동 생성 */
  overflow: auto;
  /* ------------------ */
}
.image-wrapper {
  transform-origin: center center;
}
.controls-pane {
  flex: 0 0 300px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.control-group {
  display: flex;
  flex-direction: column;
}
.control-group label {
  margin-bottom: 6px;
  font-weight: 500;
  color: #262626;
}
.control-group input {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
}
.divider {
  border: none;
  border-top: 1px solid #f0f0f0;
  margin: 8px 0;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  margin-top: auto; /* footer를 항상 하단에 위치시킴 */
}
button {
  padding: 8px 16px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
  background-color: white;
  cursor: pointer;
  font-size: 14px;
}
button.primary {
  background-color: #1677ff;
  color: white;
  border-color: #1677ff;
}

.checkbox-group {
  display: flex;
  gap: 24px;
  align-items: center;
  padding: 8px 0;
}

.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: normal; /* 그룹 타이틀(label)과 폰트 굵기 다르게 설정 */
  cursor: pointer;
}
</style>
