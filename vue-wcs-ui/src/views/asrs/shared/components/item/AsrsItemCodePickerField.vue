<template>
  <div
    class="asrs-item-code-picker-field asrs-form-field"
    :class="fieldClass"
  >
    <!-- 라벨 -->
    <label
      v-if="labelMode !== 'inside'"
      class="asrs-ui-label"
    >
      {{ label }}
    </label>

    <!-- 입력영역 + 검색버튼 -->
    <div class="asrs-item-code-picker-field__control">
      <input
        :value="modelValue"
        type="text"
        class="asrs-ui-input asrs-item-code-picker-field__input"
        :placeholder="placeholder"
        :disabled="disabled"
        @input="handleInput"
        @keydown.enter="$emit('enter')"
      />

      <button
        type="button"
        class="asrs-item-code-picker-field__icon-button"
        :disabled="disabled"
        @click="openModal"
      >
        <!-- 장난감 느낌 이모지 대신 검정 단색 SVG -->
        <svg
          viewBox="0 0 24 24"
          class="asrs-item-code-picker-field__icon"
          aria-hidden="true"
        >
          <circle cx="11" cy="11" r="6.5" fill="none" stroke="currentColor" stroke-width="2" />
          <path d="M16 16L21 21" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
        </svg>
      </button>
    </div>

    <!-- 공통 상품 조회 모달 -->
    <AsrsItemPickerModal
      :open="modalOpen"
      :keyword="keyword"
      :rows="filteredRows"
      :loading="loading"
      :selected-row="selectedRow"
      @close="closeModal"
      @search="handleSearch"
      @update:selectedRow="selectedRow = $event"
      @apply="applySelection"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import AsrsItemPickerModal, {
  type AsrsItemPickerRow,
} from './AsrsItemPickerModal.vue';

/**
 * 실제 프로젝트 item API import
 * - 이 함수는 기존 item.ts 에 이미 있다고 했던 상품 목록 조회 함수 사용
 */
import { fetchItemMasters } from '@/api/asrs/item';

/**
 * props
 */
const props = withDefaults(
  defineProps<{
    modelValue: string;
    label?: string;
    placeholder?: string;
    disabled?: boolean;
    compact?: boolean;
    labelMode?: 'left' | 'top' | 'inside';
  }>(),
  {
    label: 'Item',
    placeholder: '예: AC-ITEM-100',
    disabled: false,
    compact: true,
    labelMode: 'left',
  },
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'select-item', row: AsrsItemPickerRow): void;
  (e: 'enter'): void;
}>();

/** 모달 열림 여부 */
const modalOpen = ref(false);

/** 모달 내부 검색 키워드 */
const keyword = ref('');

/** 전체 상품 row */
const allRows = ref<AsrsItemPickerRow[]>([]);

/** 조회 중 여부 */
const loading = ref(false);

/** 현재 선택 row */
const selectedRow = ref<AsrsItemPickerRow | null>(null);

/** 최초 전체 조회 여부 */
const loaded = ref(false);

/**
 * 공통 field class
 * - 기존 공통 field 와 최대한 같은 구조 유지
 */
const fieldClass = computed(() => ({
  'asrs-form-field--left': props.labelMode === 'left',
  'asrs-form-field--top': props.labelMode === 'top',
  'asrs-form-field--compact': props.compact,
}));

/**
 * 프론트 내부 like 필터
 * - 상품코드 / 상품명 기준
 */
const filteredRows = computed(() => {
  const q = (keyword.value || '').trim().toLowerCase();

  if (!q) {
    return allRows.value;
  }

  return allRows.value.filter((row) => {
    const itemCode = String(row.itemCode || '').toLowerCase();
    const itemName = String(row.itemName || '').toLowerCase();
    return itemCode.includes(q) || itemName.includes(q);
  });
});

/**
 * API 응답에서 실제 배열을 꺼내는 함수
 * - 프로젝트마다 응답 shape 이 다를 수 있어서 방어적으로 처리
 */
function resolveList(payload: any): any[] {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.result)) return payload.result;
  if (Array.isArray(payload?.body?.content)) return payload.body.content;
  if (Array.isArray(payload?.body?.items)) return payload.body.items;
  return [];
}

/**
 * 상품 row normalize
 * - snake_case / camelCase 모두 대응
 */
function normalizeItemRow(row: any): AsrsItemPickerRow {
  return {
    id: row?.id ?? '',
    itemCode: row?.itemCode ?? row?.item_code ?? '',
    itemName: row?.itemName ?? row?.item_name ?? '',
    categoryCode: row?.categoryCode ?? row?.category_code ?? '',
    categoryName: row?.categoryName ?? row?.category_name ?? '',
    storageTempType: row?.storageTempType ?? row?.storage_temp_type ?? '',
    activeYn: row?.activeYn ?? row?.active_yn ?? '',
  };
}

/**
 * input 직접입력
 */
function handleInput(event: Event) {
  const target = event.target as HTMLInputElement;
  emit('update:modelValue', target.value);
}

/**
 * 전체 상품 조회
 * - 백엔드 추가 없이 기존 item.ts API 재사용
 */
async function loadAllItems() {
  if (loaded.value) return;

  loading.value = true;

  try {
    /**
     * 주의:
     * 프로젝트 item.ts 파라미터 스펙에 맞춰 최소조건으로 호출
     * - 전체조회 목적
     * - activeYn: 'Y' 를 기본 사용
     */
    const payload = await fetchItemMasters({
      itemCode: '',
      itemName: '',
      categoryCode: '',
      storageTempType: '',
      activeYn: 'Y',
    });

    const rows = resolveList(payload)
      .map(normalizeItemRow)
      .filter((row) => !!row.itemCode);

    allRows.value = rows;
    loaded.value = true;

  } catch (error) {
    console.error('[item-picker] loadAllItems error', error);
    allRows.value = [];
  } finally {
    loading.value = false;
  }
}

/**
 * 모달 열기
 */
async function openModal() {
  modalOpen.value = true;
  keyword.value = '';
  selectedRow.value = null;
  await loadAllItems();
}

/**
 * 모달 닫기
 */
function closeModal() {
  modalOpen.value = false;
}

/**
 * 모달 조회 버튼
 * - 실제론 전체조회 이후 프론트에서 like 처리
 */
function handleSearch(nextKeyword: string) {
  keyword.value = nextKeyword || '';
}

/**
 * 상품 선택 완료
 */
function applySelection(row: AsrsItemPickerRow) {
  emit('update:modelValue', row.itemCode || '');
  emit('select-item', row);
  modalOpen.value = false;
}
</script>

<style scoped>
.asrs-item-code-picker-field {
  min-width: 0;
  width: 100%;
}

/**
 * 기존 공통 form field 레이아웃에 맞춰 정렬
 * - label 왼쪽
 * - control 오른쪽
 */
.asrs-item-code-picker-field.asrs-form-field--left {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  column-gap: 12px;
}

.asrs-item-code-picker-field.asrs-form-field--top {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.asrs-item-code-picker-field__control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 52px;
  gap: 8px;
  width: 100%;
  min-width: 0;
  align-items: center;
}

.asrs-item-code-picker-field__input {
  width: 100%;
  height: 38px;
  min-width: 0;
  box-sizing: border-box;
}

/**
 * 돋보기 버튼
 * - 기존 input 높이와 맞춤
 * - 불필요한 장난감 느낌 제거
 */
.asrs-item-code-picker-field__icon-button {
  width: 52px;
  height: 38px;
  border-radius: 14px;
  border: 1px solid #cfd8e3;
  background: #ffffff;
  color: #111827;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease, color 0.16s ease;
}

.asrs-item-code-picker-field__icon-button:hover {
  border-color: #9fb2c8;
  background: #f8fbff;
}

.asrs-item-code-picker-field__icon-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.asrs-item-code-picker-field__icon {
  width: 19px;
  height: 19px;
  display: block;
}
</style>
