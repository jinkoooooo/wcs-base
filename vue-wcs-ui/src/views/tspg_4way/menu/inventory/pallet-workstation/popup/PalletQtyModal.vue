<template>
  <BaseModal
    :open="open"
    :busy="busy"
    :title="title"
    :confirm-label="busy ? '처리 중...' : confirmButtonLabel"
    :confirm-disabled="!!qtyValidationError"
    :header-class="`header-${kind}`"
    :confirm-class="`confirm-${kind}`"
    @confirm="$emit('confirm')"
    @close="$emit('close')"
  >
    <template #header-icon>
      <span class="qty-icon">{{ qtyModalIcon }}</span>
    </template>

    <div v-if="target" class="box-info-card">
      <div class="box-info-row">
        <span class="box-info-label">박스 바코드</span>
        <span class="box-info-value">{{ target.box_barcode }}</span>
      </div>
      <div class="box-info-row">
        <span class="box-info-label">품목 / Lot</span>
        <span class="box-info-value">{{ target.item_code }} / {{ target.lot_no }}</span>
      </div>
      <div class="box-info-row">
        <span class="box-info-label">전체 / 잔여</span>
        <span class="box-info-value">
          <b>{{ totalOf(target) }}</b> /
          <b class="current-qty">{{ remainingOf(target) }}</b>
          {{ target.uom }}
        </span>
      </div>
    </div>
    <div class="qty-input-section">
      <label class="qty-input-label">{{ qtyModalLabel }}</label>
      <div class="qty-input-wrap">
        <button
          type="button"
          class="qty-step-btn"
          :class="`step-${kind}`"
          :disabled="!canDecrease"
          @click="step(-1)"
        >−</button>
        <input
          ref="qtyInputRef"
          type="number"
          class="qty-input"
          :class="`input-${kind}`"
          v-model.number="localValue"
          :min="qtyMin"
          :max="qtyMax"
          @keyup.enter="$emit('confirm')"
          @focus="onQtyInputFocus"
        />
        <button
          type="button"
          class="qty-step-btn"
          :class="`step-${kind}`"
          :disabled="!canIncrease"
          @click="step(1)"
        >+</button>
      </div>
      <div
        v-if="kind !== 'adjust' && kind !== 'edit-total' && qtyMax > 1"
        class="quick-picks"
      >
        <button
          type="button"
          class="quick-pick-btn"
          :class="{ active: localValue === 1 }"
          @click="emit('update:value', 1)"
        >1</button>
        <button
          v-if="qtyMax >= 5"
          type="button"
          class="quick-pick-btn"
          :class="{ active: localValue === Math.min(5, qtyMax) }"
          @click="emit('update:value', Math.min(5, qtyMax))"
        >
          {{ Math.min(5, qtyMax) }}
        </button>
        <button
          v-if="qtyMax >= 4 && Math.floor(qtyMax / 2) > 1 && Math.floor(qtyMax / 2) !== 5"
          type="button"
          class="quick-pick-btn"
          :class="{ active: localValue === Math.floor(qtyMax / 2) }"
          @click="emit('update:value', Math.floor(qtyMax / 2))"
        >
          절반 ({{ Math.floor(qtyMax / 2) }})
        </button>
        <button
          type="button"
          class="quick-pick-btn"
          :class="{ active: localValue === qtyMax }"
          @click="emit('update:value', qtyMax)"
        >
          전체 ({{ qtyMax }})
        </button>
      </div>
    </div>
    <div class="qty-preview" :class="`preview-${kind}`">
      <div class="preview-row">
        <span class="preview-label">현재 잔여</span>
        <span class="preview-value preview-before"
          >{{ remainingOf(target) }} {{ target?.uom ?? '' }}</span
        >
      </div>
      <div class="preview-arrow">↓</div>
      <div class="preview-row">
        <span class="preview-label">{{ previewLabel2 }}</span>
        <span class="preview-value preview-after" :class="previewAfterClass">
          {{ previewAfter }} {{ target?.uom ?? '' }}
        </span>
      </div>
      <div v-if="previewExtra" class="preview-extra">{{ previewExtra }}</div>
    </div>
    <div v-if="qtyValidationError" class="qty-error"> ⚠ {{ qtyValidationError }} </div>
    <div class="qty-hint">{{ qtyModalHint }}</div>
  </BaseModal>
</template>

<script lang="ts" setup>
import { computed, nextTick, ref, watch } from 'vue';
import BaseModal from './BaseModal.vue';
import { QtyModalKind, totalOf, remainingOf } from '../shared';

const props = defineProps<{
  open: boolean;
  kind: QtyModalKind;
  title: string;
  target: any;
  value: number;
  busy: boolean;
  hostItemExpected: (itemCode: string, lotNo: string) => number;
  itemLotSummary: any[];
  displayBoxes: any[];
}>();

const emit = defineEmits<{
  (e: 'update:value', v: number): void;
  (e: 'confirm'): void;
  (e: 'close'): void;
}>();

const qtyInputRef = ref<HTMLInputElement | null>(null);

watch(
  () => props.open,
  (v) => {
    if (v) {
      nextTick(() => {
        qtyInputRef.value?.focus?.();
        qtyInputRef.value?.select?.();
      });
    }
  },
);

const localValue = computed({
  get: () => props.value,
  set: (v: number) => emit('update:value', v),
});

function onQtyInputFocus(e: FocusEvent) {
  (e.target as HTMLInputElement).select();
}

function step(delta: number) {
  const cur = Number(props.value) || 0;
  const next = Math.max(qtyMin.value, Math.min(qtyMax.value, cur + delta));
  emit('update:value', next);
}

const qtyModalIcon = computed(() => {
  switch (props.kind) {
    case 'adjust': return '✎';
    case 'sample': return '⚗';
    case 'partial': return '⇣';
    case 'edit-total': return '✏';
  }
  return '';
});
const qtyModalLabel = computed(() => {
  switch (props.kind) {
    case 'adjust': return '조정 후 잔여 수량';
    case 'sample': return '채취 수량';
    case 'partial': return '부분 출고 수량';
    case 'edit-total': return '박스 전체 수량 (EA)';
  }
  return '수량';
});
const qtyModalHint = computed(() => {
  switch (props.kind) {
    case 'adjust':
      return '박스 잔여 수량을 직접 변경합니다. 박스 바코드는 변경되지 않으므로 라벨 재발행은 필요 없습니다.';
    case 'sample':
      return '시험 채취한 만큼 박스에서 차감됩니다. 박스 바코드 불변 — 라벨 재발행 불필요.';
    case 'partial':
      return '박스 내 일부만 출고합니다. 잔여 분량은 [재동기화] 버튼으로 재입고 동기화됩니다.';
    case 'edit-total':
      return '입고 스캔 시작 전(대기 + 라벨 미발행)일 때만 가능합니다. 변경 후 (품목/Lot) 합계가 주문 EA 수량과 정확히 일치해야 저장됩니다.';
  }
  return '';
});
const qtyMin = computed(() => (props.kind === 'adjust' ? 0 : 1));
const qtyMax = computed(() => {
  if (!props.target) return 0;
  if (props.kind === 'adjust') return totalOf(props.target);
  if (props.kind === 'edit-total') return 999999;
  return remainingOf(props.target);
});
const canDecrease = computed(() => Number(props.value) > qtyMin.value);
const canIncrease = computed(() => Number(props.value) < qtyMax.value);
const previewLabel2 = computed(() => {
  switch (props.kind) {
    case 'adjust': return '조정 후';
    case 'sample': return '채취 후 잔여';
    case 'partial': return '출고 후 잔여';
    case 'edit-total': return '변경 후 전체';
  }
  return '결과';
});
const previewAfter = computed(() => {
  const v = Number(props.value) || 0;
  const cur = remainingOf(props.target);
  switch (props.kind) {
    case 'adjust': return v;
    case 'sample': return Math.max(0, cur - v);
    case 'partial': return Math.max(0, cur - v);
    case 'edit-total': return v;
  }
  return v;
});
const previewExtra = computed(() => {
  const v = Number(props.value) || 0;
  if (props.kind === 'sample') return v > 0 ? `채취량: ${v} ${props.target?.uom ?? ''}` : '';
  if (props.kind === 'partial') return v > 0 ? `출고량: ${v} ${props.target?.uom ?? ''}` : '';
  if (props.kind === 'edit-total') {
    const before = totalOf(props.target);
    const diff = v - before;
    if (diff === 0) return '';
    const sign = diff > 0 ? '+' : '';
    return `이전 전체: ${before} → 변동 ${sign}${diff} ${props.target?.uom ?? ''}`;
  }
  return '';
});
const previewAfterClass = computed(() => {
  const after = Number(previewAfter.value);
  if (after === 0 && props.kind !== 'adjust' && props.kind !== 'edit-total') return 'zero-warn';
  return '';
});
const qtyValidationError = computed(() => {
  const v = Number(props.value);
  if (props.value === null || props.value === undefined || Number.isNaN(v))
    return '수량을 입력하세요.';
  if (props.kind === 'adjust') {
    if (v < 0) return '수량은 0 이상이어야 합니다.';
    if (v > totalOf(props.target))
      return `전체 수량(${totalOf(props.target)})을 초과할 수 없습니다.`;
  } else if (props.kind === 'edit-total') {
    if (v < 1) return '박스 전체 수량은 1 이상이어야 합니다.';
  } else {
    if (v <= 0) return '수량은 1 이상이어야 합니다.';
    if (v > qtyMax.value) return `박스 잔여 수량(${qtyMax.value})을 초과할 수 없습니다.`;
  }
  return '';
});
const confirmButtonLabel = computed(() => {
  switch (props.kind) {
    case 'adjust': return '조정 확정';
    case 'sample': return '채취 확정';
    case 'partial': return '출고 확정';
    case 'edit-total': return '전체 수량 변경';
  }
  return '확인';
});
</script>

<!-- 폼 위젯 공통 스타일 -->
<style src="./palletModalShared.css"></style>

<style scoped>
  .qty-icon {
    font-size: 16px;
    width: 28px;
    height: 28px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.22);
    border-radius: 50%;
  }
  .current-qty {
    font-size: 16px;
    color: #f59e0b;
  }

  .step-adjust:hover:not(:disabled) {
    border-color: #8b5cf6;
    color: #6d28d9;
  }
  .step-sample:hover:not(:disabled) {
    border-color: #ec4899;
    color: #be185d;
  }
  .step-partial:hover:not(:disabled) {
    border-color: #f59e0b;
    color: #d97706;
  }
  .step-edit-total:hover:not(:disabled) {
    border-color: #0ea5e9;
    color: #0369a1;
  }

  .input-adjust:focus {
    border-color: #8b5cf6;
    box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.18);
  }
  .input-sample:focus {
    border-color: #ec4899;
    box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.18);
  }
  .input-partial:focus {
    border-color: #f59e0b;
    box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.18);
  }
  .input-edit-total:focus {
    border-color: #0ea5e9;
    box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.18);
  }

  .qty-preview {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 12px 16px;
    margin-bottom: 14px;
  }
  .preview-adjust {
    border-left: 4px solid #8b5cf6;
  }
  .preview-sample {
    border-left: 4px solid #ec4899;
  }
  .preview-partial {
    border-left: 4px solid #f59e0b;
  }
  .preview-edit-total {
    border-left: 4px solid #0ea5e9;
  }
  .preview-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 2px 0;
  }
  .preview-label {
    font-size: 12px;
    color: #64748b;
    font-weight: 500;
  }
  .preview-value {
    font-size: 16px;
    font-weight: 700;
  }
  .preview-before {
    color: #64748b;
  }
  .preview-after {
    color: #0f172a;
    font-size: 20px;
  }
  .preview-after.zero-warn {
    color: #dc2626;
  }
  .preview-arrow {
    text-align: center;
    font-size: 16px;
    color: #94a3b8;
    margin: 4px 0;
    font-weight: 700;
  }
  .preview-extra {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px dashed #e2e8f0;
    font-size: 12px;
    color: #64748b;
    font-weight: 500;
    text-align: center;
  }
</style>
