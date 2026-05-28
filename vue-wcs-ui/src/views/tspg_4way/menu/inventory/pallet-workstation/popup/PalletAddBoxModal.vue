<template>
  <BaseModal
    :open="open"
    :busy="busy"
    title="박스 추가"
    :confirm-label="busy ? '처리 중...' : '박스 추가'"
    :confirm-disabled="!!addBoxError"
    header-class="header-add-box"
    confirm-class="confirm-add-box"
    @confirm="$emit('confirm')"
    @close="$emit('close')"
  >
    <template #header-icon><span class="qty-icon">+</span></template>

    <div class="qty-input-section">
      <label class="qty-input-label">품목 / Lot</label>
      <select
        :value="itemKey"
        class="add-box-select"
        :disabled="busy"
        @change="$emit('update:itemKey', ($event.target as HTMLSelectElement).value)"
      >
        <option value="">선택하세요</option>
        <option
          v-for="it in hostItems"
          :key="`${it.itemCode}|${it.lotNo}`"
          :value="`${it.itemCode}|${it.lotNo}`"
        >
          {{ it.itemCode }} / {{ it.lotNo }} (주문 {{ it.qty }} EA)
        </option>
      </select>
    </div>
    <div v-if="addBoxSelected" class="box-info-card">
      <div class="box-info-row">
        <span class="box-info-label">주문 수량</span>
        <span class="box-info-value"><b>{{ addBoxSelected.qty }}</b> EA</span>
      </div>
      <div class="box-info-row">
        <span class="box-info-label">현재 박스 합계</span>
        <span class="box-info-value"><b>{{ addBoxCurrentSum }}</b> EA</span>
      </div>
      <div class="box-info-row">
        <span class="box-info-label">남은(부족) 수량</span>
        <span
          class="box-info-value"
          :style="{
            color: addBoxSelected.qty - addBoxCurrentSum > 0 ? '#dc2626' : '#0f172a',
          }"
        >
          <b>{{ addBoxSelected.qty - addBoxCurrentSum }}</b> EA
        </span>
      </div>
    </div>
    <div class="qty-input-section">
      <label class="qty-input-label">박스 수량 (EA)</label>
      <div class="qty-input-wrap">
        <button
          type="button"
          class="qty-step-btn"
          :disabled="Number(totalQty) <= 1"
          @click="$emit('update:totalQty', Math.max(1, Number(totalQty) - 1))"
        >−</button>
        <input
          type="number"
          class="qty-input"
          :value="totalQty"
          min="1"
          @input="$emit('update:totalQty', Number(($event.target as HTMLInputElement).value))"
          @keyup.enter="$emit('confirm')"
        />
        <button
          type="button"
          class="qty-step-btn"
          @click="$emit('update:totalQty', Number(totalQty) + 1)"
        >+</button>
      </div>
      <div
        v-if="addBoxSelected && addBoxSelected.qty - addBoxCurrentSum > 0"
        class="quick-picks"
      >
        <button
          type="button"
          class="quick-pick-btn"
          @click="$emit('update:totalQty', addBoxSelected.qty - addBoxCurrentSum)"
        >
          부족분 전체 ({{ addBoxSelected.qty - addBoxCurrentSum }})
        </button>
      </div>
    </div>
    <div v-if="addBoxError" class="qty-error"> ⚠ {{ addBoxError }} </div>
    <div class="qty-hint">
      새 박스의 box_seq 는 현재 파렛트의 max(box_seq)+1 로 부여됩니다. (item/lot) 그룹별
      합계가 주문 EA 와 정확히 일치해야 저장됩니다 — 불일치 시 자동 롤백.
    </div>
  </BaseModal>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import BaseModal from './BaseModal.vue';

const props = defineProps<{
  open: boolean;
  busy: boolean;
  hostItems: Array<{
    itemCode: string;
    lotNo: string;
    qty: number;
    uom?: string;
    produceDate?: string;
    expiryDate?: string;
  }>;
  itemKey: string;
  totalQty: number;
  itemLotSummary: Array<{ itemCode: string; lotNo: string; sum: number }>;
}>();

defineEmits<{
  (e: 'update:itemKey', v: string): void;
  (e: 'update:totalQty', v: number): void;
  (e: 'confirm'): void;
  (e: 'close'): void;
}>();

const addBoxSelected = computed(() => {
  if (!props.itemKey) return null;
  return (
    props.hostItems.find(
      (it) => `${it.itemCode ?? ''}|${it.lotNo ?? ''}` === props.itemKey,
    ) ?? null
  );
});

const addBoxCurrentSum = computed(() => {
  const sel = addBoxSelected.value;
  if (!sel) return 0;
  const key = `${sel.itemCode ?? ''}|${sel.lotNo ?? ''}`;
  return props.itemLotSummary.find((r) => `${r.itemCode}|${r.lotNo}` === key)?.sum ?? 0;
});

const addBoxError = computed(() => {
  if (!addBoxSelected.value) return '품목/Lot 을 선택하세요.';
  const v = Number(props.totalQty);
  if (Number.isNaN(v) || v < 1) return '박스 수량은 1 이상이어야 합니다.';
  return '';
});
</script>

<!-- 폼 위젯 공통 스타일 -->
<style src="./palletModalShared.css"></style>

<style scoped>
.qty-icon {
  font-size: 18px;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.22);
  border-radius: 50%;
}
.add-box-select {
  width: 100%;
  height: 44px;
  padding: 0 12px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  font-size: 14px;
  color: #0f172a;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.add-box-select:focus {
  border-color: #16a34a;
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.18);
}
.add-box-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
