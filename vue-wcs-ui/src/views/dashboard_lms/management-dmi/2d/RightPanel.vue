<template>
  <div class="right-panel-container">
    <div v-if="!hasSelection" class="placeholder">
      객체를 선택하면 여기에 속성이 표시됩니다.
    </div>
    <div v-else class="properties-editor">
      <h3>객체 속성 편집 ({{ selectedCount }}개 선택됨)</h3>

      <div class="property-group" v-if="selectedCount === 1">
        <div class="property-item read-only">
          <label class="label">ID</label>
          <input type="text" :value="firstObject.id" readonly class="text-input" />
        </div>
      </div>

      <div class="property-group">
        <div class="property-item read-only">
          <label class="label">Model Type</label>
          <input type="text" :value="commonValues.model_type" readonly class="text-input" placeholder="(Multiple)" />
        </div>
        <div class="property-item read-only">
          <label class="label">Group Type</label>
          <input type="text" :value="commonValues.group_type" readonly class="text-input" placeholder="(Multiple)" />
        </div>
        <div class="property-item read-only">
          <label class="label">Group Code</label>
          <input type="text" :value="commonValues.group_code" readonly class="text-input" placeholder="(Multiple)" />
        </div>

        <div class="property-item">
          <label class="label">Model Code</label>
          <input type="text" :value="commonValues.model_code" @input="updateField('model_code', $event.target.value)" class="text-input" placeholder="" />
        </div>
        <div class="property-item checkbox-item">
          <label class="label" for="is_use">사용 여부</label>
          <input type="checkbox" id="is_use" :checked="commonValues.is_use === true" @change="updateField('is_use', $event.target.checked)" />
        </div>
      </div>

      <h4>2D 위치 및 변형</h4>

      <!-- Position -->
      <div class="property-group grid-group">
        <div class="property-item">
          <label class="label">Pos X (px)</label>
          <input type="number" :value="commonValues.position_x_2d" @change="updateField('position_x_2d', $event.target.value)" class="number-input" placeholder="" />
        </div>
        <div class="property-item">
          <label class="label">Pos Y (px)</label>
          <input type="number" :value="commonValues.position_y_2d" @change="updateField('position_y_2d', $event.target.value)" class="number-input" placeholder="" />
        </div>
      </div>

      <!-- Scale -->
      <div class="property-group grid-group">
        <div class="property-item">
          <label class="label">Scale X</label>
          <input type="number" step="0.1" :value="commonValues.scale_x_2d" @change="updateField('scale_x_2d', $event.target.value)" class="number-input" placeholder="" />
        </div>
        <div class="property-item">
          <label class="label">Scale Y</label>
          <input type="number" step="0.1" :value="commonValues.scale_y_2d" @change="updateField('scale_y_2d', $event.target.value)" class="number-input" placeholder="" />
        </div>
      </div>

      <!-- Rotation -->
      <div class="property-group">
        <div class="property-item">
          <label class="label">Rotation</label>
          <input type="number" :value="commonValues.rotation_2d" @change="updateField('rotation_2d', $event.target.value)" class="number-input full-width" placeholder="" />
        </div>
      </div>

      <!-- Flip -->
      <div class="property-group grid-group">
        <div class="property-item checkbox-item">
          <label class="label" for="flip_h">수평 반전</label>
          <input type="checkbox" id="flip_h" :checked="commonValues.flip_horizontal_2d === true" @change="updateField('flip_horizontal_2d', $event.target.checked)" />
        </div>
        <div class="property-item checkbox-item">
          <label class="label" for="flip_v">수직 반전</label>
          <input type="checkbox" id="flip_v" :checked="commonValues.flip_vertical_2d === true" @change="updateField('flip_vertical_2d', $event.target.checked)" />
        </div>
      </div>

      <!-- Render Order -->
      <div class="property-group">
        <div class="property-item">
          <label class="label">Render Order</label>
          <input type="number" :value="commonValues.render_order" @change="updateField('render_order', $event.target.value)" class="number-input full-width" placeholder="" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';

const props = defineProps({
  selectedObject: {
    type: [Array, Object], // 호환성을 위해 둘 다 허용하되 로직에선 배열로 처리
    default: () => []
  }
});

const emit = defineEmits(['update:selectedObject']);

// 내부 편집용 객체 리스트 (Deep Copy)
const localObjects = ref([]);

// Props -> Local 동기화
watch(() => props.selectedObject, (newVal) => {
  if (!newVal) {
    localObjects.value = [];
  } else if (Array.isArray(newVal)) {
    localObjects.value = JSON.parse(JSON.stringify(newVal));
  } else {
    // 혹시 모를 단일 객체 fallback
    localObjects.value = [JSON.parse(JSON.stringify(newVal))];
  }
}, { deep: true, immediate: true });

const hasSelection = computed(() => localObjects.value.length > 0);
const selectedCount = computed(() => localObjects.value.length);
const firstObject = computed(() => localObjects.value[0] || {});

// [핵심] 모든 필드에 대해 공통 값을 계산하여 반환하는 Computed Object
const commonValues = computed(() => {
  if (!hasSelection.value) return {};

  const keys = [
    'model_type', 'group_type', 'group_code', 'model_code', 'is_use',
    'position_x_2d', 'position_y_2d', 'scale_x_2d', 'scale_y_2d',
    'rotation_2d', 'flip_horizontal_2d', 'flip_vertical_2d', 'render_order'
  ];

  const result = {};

  keys.forEach(key => {
    // 첫 번째 객체의 값
    const firstVal = localObjects.value[0][key];

    // 모든 객체가 첫 번째 값과 같은지 확인
    const allSame = localObjects.value.every(obj => obj[key] === firstVal);

    // 같으면 값 반환, 다르면 '' (빈 문자열)
    result[key] = allSame ? firstVal : '';
  });

  return result;
});

// 값 수정 시 일괄 적용
const updateField = (key, value) => {
  if (localObjects.value.length === 0) return;

  // 숫자형 필드 변환
  let parsedValue = value;
  const numberFields = ['position_x_2d', 'position_y_2d', 'scale_x_2d', 'scale_y_2d', 'rotation_2d', 'render_order'];

  if (numberFields.includes(key)) {
    // 빈 문자열 입력 시 무시하거나 0 처리? 여기서는 입력값이 있으면 변환
    if (value === '' || value === null) parsedValue = '';
    else parsedValue = Number(value);
  }

  // 로컬 객체 일괄 업데이트
  localObjects.value.forEach(obj => {
    obj[key] = parsedValue;
  });

  // 변경된 전체 리스트를 부모에게 전송
  emit('update:selectedObject', JSON.parse(JSON.stringify(localObjects.value)));
};
</script>

<style scoped>
.right-panel-container {
  padding: 15px;
  background-color: #ffffff;
  border-left: 1px solid #e4e7ed;
  overflow-y: auto;
  min-width: 250px;
  max-width: 300px;
  flex-shrink: 0;
  height: 100%;
}

.placeholder {
  text-align: center;
  color: #909399;
  padding: 50px 0;
  font-size: 14px;
}

h3, h4 {
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 5px;
  margin-top: 15px;
  margin-bottom: 10px;
  font-weight: 600;
}
h3 { font-size: 16px; }
h4 { font-size: 14px; }

.property-group {
  margin-bottom: 15px;
  padding: 8px 0;
}

.property-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
}

.label {
  font-weight: 500;
  color: #606266;
  flex-shrink: 0;
  width: 100px; /* 라벨 너비 고정 */
}

input[type="text"], input[type="number"] {
  width: calc(100% - 110px); /* 라벨 너비 + 마진 제외 */
  padding: 6px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 13px;
  box-sizing: border-box;
}

input.full-width {
  width: 100%;
}

.text-input[readonly] {
  background-color: #f5f7fa;
  color: #909399;
  cursor: default;
}

.property-item.read-only .label {
  color: #909399;
}

.checkbox-item {
  justify-content: flex-start;
  gap: 10px;
}

.checkbox-item input[type="checkbox"] {
  width: auto;
  margin-left: auto;
  cursor: pointer;
}

/* 그리드 레이아웃 */
.grid-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}
.grid-group .property-item {
  display: block;
  margin-bottom: 0;
}
.grid-group .label {
  width: 100%;
  margin-bottom: 3px;
  display: block;
}
.grid-group input {
  width: 100%;
}
</style>
