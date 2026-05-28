<template>
  <div v-if="open" class="asrs-item-picker-modal">
    <!-- 모달 백드롭 -->
    <div class="asrs-item-picker-modal__backdrop" @click="$emit('close')" />

    <!-- 모달 본문 -->
    <section class="asrs-item-picker-modal__dialog">
      <!-- 헤더 -->
      <header class="asrs-item-picker-modal__header">
        <div>
          <p class="asrs-item-picker-modal__eyebrow">ITEM PICKER</p>
          <h3 class="asrs-item-picker-modal__title">상품 조회</h3>
        </div>

        <div class="asrs-item-picker-modal__header-actions">
          <button
            type="button"
            class="asrs-item-picker-modal__header-btn"
            @click="$emit('close')"
          >
            닫기
          </button>

          <button
            type="button"
            class="asrs-item-picker-modal__header-btn asrs-item-picker-modal__header-btn--primary"
            :disabled="!selectedRowLocal"
            @click="handleApply"
          >
            입력
          </button>
        </div>
      </header>

      <!-- 검색영역 -->
      <div class="asrs-item-picker-modal__search">
        <input
          v-model.trim="localKeyword"
          type="text"
          class="asrs-item-picker-modal__search-input"
          placeholder="상품코드 / 상품명 검색"
          @keydown.enter="emitSearch"
        />

        <button
          type="button"
          class="asrs-item-picker-modal__search-btn"
          @click="emitSearch"
        >
          조회
        </button>
      </div>

      <!-- 테이블 영역 -->
      <div class="asrs-item-picker-modal__table-wrap">
        <table class="asrs-item-picker-modal__table">
          <thead>
          <tr>
            <th>Item Code</th>
            <th>Item Name</th>
            <th>Category</th>
            <th>Temp</th>
            <th>Active</th>
          </tr>
          </thead>

          <tbody>
          <tr v-if="loading">
            <td colspan="5" class="asrs-item-picker-modal__empty">
              조회 중입니다...
            </td>
          </tr>

          <tr v-else-if="!rows.length">
            <td colspan="5" class="asrs-item-picker-modal__empty">
              조회 결과가 없습니다.
            </td>
          </tr>

          <tr
            v-for="row in rows"
            :key="row.itemCode"
            :class="{
                'asrs-item-picker-modal__selected':
                  selectedRowLocal?.itemCode === row.itemCode,
              }"
            @click="selectedRowLocal = row"
            @dblclick="handleDoubleClick(row)"
          >
            <td class="asrs-item-picker-modal__key">{{ row.itemCode || '-' }}</td>
            <td>{{ row.itemName || '-' }}</td>
            <td>{{ row.categoryCode || row.categoryName || '-' }}</td>
            <td>{{ row.storageTempType || '-' }}</td>
            <td>{{ row.activeYn || '-' }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

/**
 * 모달 내부에서 사용하는 상품 row 타입
 */
export interface AsrsItemPickerRow {
  id?: string;
  itemCode?: string;
  itemName?: string;
  categoryCode?: string;
  categoryName?: string;
  storageTempType?: string;
  activeYn?: string;
}

const props = defineProps<{
  open: boolean;
  keyword?: string;
  rows: AsrsItemPickerRow[];
  loading?: boolean;
  selectedRow?: AsrsItemPickerRow | null;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'search', keyword: string): void;
  (e: 'update:selectedRow', row: AsrsItemPickerRow | null): void;
  (e: 'apply', row: AsrsItemPickerRow): void;
}>();

/** 모달 내부 검색어 상태 */
const localKeyword = ref(props.keyword || '');

/** 모달 내부 선택 row 상태 */
const selectedRowLocal = ref<AsrsItemPickerRow | null>(props.selectedRow || null);

/** 부모에서 keyword가 바뀌면 동기화 */
watch(
  () => props.keyword,
  (value) => {
    localKeyword.value = value || '';
  },
);

/** 부모에서 선택 row가 바뀌면 동기화 */
watch(
  () => props.selectedRow,
  (value) => {
    selectedRowLocal.value = value || null;
  },
);

/** 내부 선택 row 변경 시 부모로 전달 */
watch(selectedRowLocal, (value) => {
  emit('update:selectedRow', value);
});

/** 조회 버튼/엔터 시 검색 이벤트 발생 */
function emitSearch() {
  emit('search', localKeyword.value || '');
}

/** 입력 버튼 클릭 */
function handleApply() {
  if (!selectedRowLocal.value) return;
  emit('apply', selectedRowLocal.value);
}

/** 더블클릭 시 바로 적용 */
function handleDoubleClick(row: AsrsItemPickerRow) {
  emit('apply', row);
}
</script>

<style scoped>
.asrs-item-picker-modal {
  position: fixed;
  inset: 0;
  z-index: 3000;
}

.asrs-item-picker-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.34);
}

.asrs-item-picker-modal__dialog {
  position: relative;
  z-index: 1;
  width: min(1080px, calc(100vw - 48px));
  height: min(760px, calc(100vh - 48px));
  margin: 24px auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  box-sizing: border-box;
  border-radius: 24px;
  background: #ffffff;
  border: 1px solid #dbe4ee;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.18);
}

.asrs-item-picker-modal__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.asrs-item-picker-modal__eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 900;
  color: #2563eb;
  letter-spacing: 0.04em;
}

.asrs-item-picker-modal__title {
  margin: 0;
  font-size: 20px;
  font-weight: 900;
  color: #132033;
}

.asrs-item-picker-modal__header-actions {
  display: flex;
  gap: 10px;
}

.asrs-item-picker-modal__header-btn,
.asrs-item-picker-modal__search-btn {
  min-width: 54px;
  height: 40px;
  padding: 0 14px;
  border-radius: 12px;
  border: 1px solid #cfd8e3;
  background: #ffffff;
  color: #30455f;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.asrs-item-picker-modal__header-btn--primary {
  background: #1f4f87;
  border-color: #1f4f87;
  color: #ffffff;
}

.asrs-item-picker-modal__search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.asrs-item-picker-modal__search-input {
  width: 100%;
  height: 40px;
  padding: 0 14px;
  box-sizing: border-box;
  border-radius: 12px;
  border: 1px solid #cfd8e3;
  outline: none;
  font-size: 14px;
  color: #132033;
  background: #ffffff;
}

.asrs-item-picker-modal__table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid #dbe4ee;
  border-radius: 18px;
}

.asrs-item-picker-modal__table {
  width: 100%;
  border-collapse: collapse;
  min-width: 760px;
}

.asrs-item-picker-modal__table th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #eef3f8;
  color: #486078;
  font-size: 13px;
  font-weight: 800;
  text-align: left;
  padding: 14px 16px;
  border-bottom: 1px solid #d7e0ea;
}

.asrs-item-picker-modal__table td {
  padding: 14px 16px;
  border-bottom: 1px solid #e7edf4;
  font-size: 13px;
  color: #24384f;
  white-space: nowrap;
}

.asrs-item-picker-modal__table tbody tr {
  cursor: pointer;
  transition: background 0.14s ease;
}

.asrs-item-picker-modal__table tbody tr:hover {
  background: #f8fbff;
}

.asrs-item-picker-modal__selected {
  background: #eaf2fc !important;
}

.asrs-item-picker-modal__key {
  font-weight: 900;
  color: #10233a;
}

.asrs-item-picker-modal__empty {
  text-align: center;
  color: #7d8b99;
  padding: 28px 16px !important;
}
</style>
