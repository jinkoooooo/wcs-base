<template>
  <div
    style="position: absolute; top: 20px; left: 20px; width: 300px; max-height: 42vh; display: flex; flex-direction: column; background: rgba(30, 30, 30, 0.95); border: 1px solid #444; color: white; border-radius: 8px; z-index: 10; box-shadow: 0 4px 15px rgba(0,0,0,0.5);"
    @pointerdown.stop
    @pointermove.stop
    @pointerup.stop
  >
    <div style="padding: 12px 15px; border-bottom: 1px solid #555; display: flex; justify-content: space-between; align-items: center;">
      <h3 style="margin: 0; color: #4a90e2; font-size: 16px;">랙 조건 조회</h3>

      <button
        @click="onReset"
        style="padding: 4px 10px; background: #555; color: #fff; font-size: 12px; border: none; border-radius: 4px; cursor: pointer;"
      >
        초기화
      </button>
    </div>

    <div style="flex: 1; overflow-y: auto; padding: 15px; font-size: 13px;">
      <div v-for="field in fieldDefinitions" :key="field.key" style="margin-bottom: 12px; display: flex; align-items: center;">
        <label :style="{ width: '100px', color: '#bbb' }">{{ field.label }}</label>

        <select
          v-if="field.type === 'boolean'"
          v-model="searchConditions[field.key]"
          style="flex: 1; padding: 4px; background: #333; color: white; border: 1px solid #555; border-radius: 4px;"
        >
          <option :value="null">전체</option>
          <option :value="true">True</option>
          <option :value="false">False</option>
        </select>

        <input
          v-else
          :type="field.type === 'number' ? 'number' : 'text'"
          v-model="searchConditions[field.key]"
          placeholder="전체"
          style="flex: 1; padding: 4px 8px; background: #333; color: white; border: 1px solid #555; border-radius: 4px;"
          @keyup.enter="onSearch"
        />
      </div>
    </div>

    <div style="padding: 12px 15px; border-top: 1px solid #555;">
      <button
        @click="onSearch"
        style="width: 100%; padding: 8px; background: #4a90e2; color: white; font-weight: bold; border: none; border-radius: 4px; cursor: pointer;"
      >
        조회
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, inject } from 'vue';

// 부모(메인 화면)로부터 필드 정의를 주입받음
const fieldDefinitions = inject('rackFieldDefinitions', []);

const emit = defineEmits(['search']);

// 사용자가 입력한 검색 조건을 담을 객체
const searchConditions = ref({});

// 단순히 객체를 빈 상태로 만들어 화면을 리셋
const onReset = () => {
  searchConditions.value = {};
};

const onSearch = () => {
  const payload = {};

  // 값이 비어있지 않은(null, undefined, 빈 문자열이 아닌) 조건만 payload에 담아서 부모로 전달
  for (const key in searchConditions.value) {
    const val = searchConditions.value[key];
    if (val !== null && val !== '' && val !== undefined) {
      // 숫자 타입은 확실하게 Number로 캐스팅
      const fieldDef = fieldDefinitions.find(f => f.key === key);
      payload[key] = fieldDef && fieldDef.type === 'number' ? Number(val) : val;
    }
  }

  emit('search', payload);
};
</script>
