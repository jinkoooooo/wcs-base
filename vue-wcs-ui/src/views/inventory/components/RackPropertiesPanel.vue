<template>
  <div
    v-if="selectedRacks.length > 0"
    style="position: absolute; bottom: 100px; right: 250px; width: 320px; max-height: 50vh; display: flex; flex-direction: column; background: rgba(30, 30, 30, 0.95); border: 1px solid #444; color: white; border-radius: 8px; z-index: 10; box-shadow: 0 4px 15px rgba(0,0,0,0.5);"
    @pointerdown.stop
    @pointermove.stop
    @pointerup.stop
  >
    <div style="padding: 12px 15px; border-bottom: 1px solid #555; display: flex; justify-content: space-between; align-items: center;">
      <h3 style="margin: 0; color: #42b883; font-size: 16px;">
        속성 편집 ({{ selectedRacks.length }}개)
      </h3>
    </div>

    <div style="flex: 1; overflow-y: auto; padding: 15px; font-size: 13px;">
      <div v-for="field in fieldDefinitions" :key="field.key" style="margin-bottom: 12px; display: flex; align-items: center;">
        <label :style="{ width: '100px', color: '#bbb' }">{{ field.label }}</label>

        <select
          v-if="field.type === 'boolean'"
          v-model="formData[field.key]"
          @change="onBulkFieldChange(field.key, formData[field.key], field.type)"
          :disabled="field.readonly"
          :style="{
            flex: 1, padding: '4px', background: field.readonly ? '#222' : '#333',
            color: field.readonly ? '#888' : 'white', border: '1px solid #555',
            borderRadius: '4px', cursor: field.readonly ? 'not-allowed' : 'auto'
          }"
        >
          <option :value="true">True</option>
          <option :value="false">False</option>
        </select>

        <input
          v-else-if="field.type === 'number'"
          type="number"
          v-model.number="formData[field.key]"
          @change="onBulkFieldChange(field.key, formData[field.key], field.type)"
          :placeholder="formData[field.key] === '■' ? '■' : '값 입력'"
          :readonly="field.readonly"
          :style="{
            flex: 1, padding: '4px 8px', background: field.readonly ? '#222' : '#333',
            color: field.readonly ? '#888' : 'white', border: '1px solid #555',
            borderRadius: '4px', cursor: field.readonly ? 'not-allowed' : 'auto'
          }"
        />

        <input
          v-else
          type="text"
          v-model="formData[field.key]"
          @change="onBulkFieldChange(field.key, formData[field.key], field.type)"
          :placeholder="formData[field.key] === '■' ? '■' : '값 입력'"
          :readonly="field.readonly"
          :style="{
            flex: 1, padding: '4px 8px', background: field.readonly ? '#222' : '#333',
            color: field.readonly ? '#888' : 'white', border: '1px solid #555',
            borderRadius: '4px', cursor: field.readonly ? 'not-allowed' : 'auto'
          }"
        />
      </div>
    </div>

    <div style="padding: 12px 15px; border-top: 1px solid #555;">
      <button
        @click="handleSave"
        style="width: 100%; padding: 8px; background: #42b883; color: #1e1e1e; font-weight: bold; border: none; border-radius: 4px; cursor: pointer;"
      >
        수정사항 저장
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, inject, computed } from 'vue';

const props = defineProps({
  selectedRacks: {
    type: Array,
    required: true,
    default: () => []
  }
});

const emit = defineEmits(['save']);

// 부모에게서 필드 정의 주입받음
const fieldDefinitions = inject('rackFieldDefinitions', []);
const handleGlobalSave = inject('handleGlobalSave');

// 화면에 바인딩 될 폼 데이터
const formData = ref({});

// 선택된 객체가 바뀔 때마다 폼 데이터 재계산
watch(() => props.selectedRacks, (newRacks) => {
  if (newRacks.length === 0) {
    formData.value = {};
    return;
  }

  const newForm = {};

  fieldDefinitions.forEach(field => {
    // 선택된 모든 랙의 해당 필드 값을 수집
    const uniqueValues = new Set(newRacks.map(rack => rack[field.key]));

    if (uniqueValues.size === 1) {
      // 모든 객체의 값이 똑같다면 그 값을 표시
      newForm[field.key] = [...uniqueValues][0];
    } else {
      // 값이 하나라도 다르면 '■' 표시
      newForm[field.key] = '■';
    }
  });

  formData.value = newForm;
}, { deep: true, immediate: true });

// 필드 수정 시 데이터 타입을 보정하고 부모 데이터(props)를 직접 수정
const onBulkFieldChange = (key, val, type) => {

  // '■' 상태인 필드는 사용자가 건드리지 않은 다중 값 필드이므로 무시
  if (val === '■') return;

  // 1. 화면 업데이트
  formData.value[key] = val;

  // 2. 데이터 타입 보정
  let parsedValue = val;
  if (type === 'number') {
    parsedValue = val === '' || val === null ? null : Number(val);
  } else if (type === 'string') {
    parsedValue = val === '' ? null : String(val);
  } else if (type === 'boolean') {
    parsedValue = (val === 'true' || val === true);
  }

  // 선택된 부모 객체들의 속성을 즉시 직접 수정
  // Props는 참조형이므로 여기서 바꾸면 부모 rackList도 함께 바뀝니다.
  props.selectedRacks.forEach(rack => {
    rack[key] = parsedValue;
  });
};

// 부모의 '일괄 저장' 함수 호출
const handleSave = () => {
  handleGlobalSave(); // 부모의 저장 로직 호출
};
</script>
