<template>
  <section class="inbound-toolbar asrs-ui-toolbar">
    <div class="inbound-toolbar__top">
      <AsrsAreaCodeField
        v-model="form.areaCode"
        :options="areaOptions"
        :disabled="loading.areas || loading.submit"
        placeholder="선택"
      />

      <AsrsItemCodePickerField
        v-model="form.itemCode"
        label="Item"
        label-mode="left"
        :compact="true"
        placeholder="예: AC-ITEM-100"
        :disabled="loading.submit"
        @enter="$emit('run-recommend')"
      />

      <AsrsFormField
        v-model="form.qty"
        label="Qty"
        type="number"
        label-mode="left"
        :compact="true"
        placeholder="예: 10"
        :disabled="loading.submit"
        @enter="$emit('run-recommend')"
      />

      <AsrsFormField
        v-model="form.lotNo"
        :label="lotLabel"
        type="text"
        label-mode="left"
        :compact="true"
        placeholder="예: LOT-001"
        :disabled="!itemPolicy.lotEnabled || loading.submit"
        @enter="$emit('run-recommend')"
      />

      <div class="inbound-toolbar__actions">
        <AsrsActionButton
          variant="ghost"
          :disabled="loading.submit"
          @click="$emit('reset-page')"
        >
          전체 초기화
        </AsrsActionButton>

        <AsrsActionButton
          variant="secondary"
          :disabled="loading.submit"
          :loading="loading.recommend"
          loading-text="추천 중..."
          @click="$emit('run-recommend')"
        >
          로케이션 재추천
        </AsrsActionButton>

        <AsrsActionButton
          variant="secondary"
          :disabled="loading.submit"
          @click="$emit('add-draft')"
        >
          입고 대기 등록
        </AsrsActionButton>

        <AsrsActionButton
          variant="primary"
          :disabled="!hasDraftRows"
          :loading="loading.submit"
          loading-text="처리 중..."
          @click="$emit('submit-inbound')"
        >
          수동 입고 실행
        </AsrsActionButton>
      </div>
    </div>

    <div class="inbound-toolbar__bottom">
      <AsrsFormField
        v-model="recommend.selectedLocationCode"
        label="추천"
        type="select"
        label-mode="left"
        :compact="true"
        :options="recommendSelectOptions"
        :placeholder="loading.recommend ? '추천 중...' : '추천 결과 없음'"
        :disabled="loading.recommend || loading.submit || !recommend.options.length"
      />

      <div class="inbound-toolbar__meta">
        <div class="inbound-toolbar__meta-chip">
          <span class="inbound-toolbar__meta-label">후보 수</span>
          <strong class="inbound-toolbar__meta-value">
            {{ recommend.candidateCount }}
          </strong>
        </div>

        <div class="inbound-toolbar__meta-chip">
          <span class="inbound-toolbar__meta-label">선택 등급</span>
          <strong class="inbound-toolbar__meta-value">
            {{ selectedRecommend?.locationGrade || '-' }}
          </strong>
        </div>

        <AsrsFeedback
          class="inbound-toolbar__feedback"
          :type="feedbackType"
          :message="feedbackMessage"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { StorageAreaOption } from '@/api/asrs/stock';
import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import AsrsFeedback from '@/views/asrs/shared/components/ui/AsrsFeedback.vue';
import type {
  InboundFormState,
  InboundItemPolicyState,
  InboundLocationOption,
  InboundRecommendState,
} from '../types';
import AsrsAreaCodeField from "@/views/asrs/shared/components/form/AsrsAreaCodeField.vue";
import AsrsItemCodePickerField
  from "@/views/asrs/shared/components/item/AsrsItemCodePickerField.vue";

const props = defineProps<{
  form: InboundFormState;
  loading: {
    areas: boolean;
    recommend: boolean;
    submit: boolean;
  };
  areaOptions: StorageAreaOption[];
  itemPolicy: InboundItemPolicyState;
  recommend: InboundRecommendState;
  selectedRecommend: InboundLocationOption | null;
  feedbackType?: 'info' | 'success' | 'warning' | 'error';
  feedbackMessage?: string;
  hasDraftRows: boolean;
}>();

defineEmits<{
  (e: 'reset-page'): void;
  (e: 'run-recommend'): void;
  (e: 'add-draft'): void;
  (e: 'submit-inbound'): void;
}>();


/** 추천 로케이션 option normalize */
const recommendSelectOptions = computed(() =>
  props.recommend.options.map((option) => ({
    value: option.locationCode,
    label:
      `${option.locationCode}` +
      `${option.locationGrade ? ` / 등급 ${option.locationGrade}` : ''}` +
      `${option.frontPriorityYn === 'Y' ? ' / 우선' : ''}`,
  })),
);

/** Lot No 라벨 상태를 텍스트로 흡수 */
const lotLabel = computed(() => {
  if (props.itemPolicy.lotRequired) return 'Lot (필수)';
  if (props.itemPolicy.lotEnabled) return 'Lot (선택)';
  return 'Lot (사용안함)';
});
</script>
