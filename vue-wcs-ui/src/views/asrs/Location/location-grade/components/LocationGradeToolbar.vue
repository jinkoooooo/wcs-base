<template>
  <section class="location-grade-toolbar asrs-ui-toolbar">
    <div class="location-grade-toolbar__fields">
      <AsrsFormField
        v-model="filters.areaCode"
        label="Area"
        type="select"
        label-mode="left"
        :compact="true"
        :options="areaOptions"
        @enter="$emit('preview')"
      />

      <AsrsFormField
        v-model="filters.purposeCode"
        label="Purpose"
        type="select"
        label-mode="left"
        :compact="true"
        :options="purposeOptions"
        @enter="$emit('preview')"
      />

      <AsrsFormField
        v-model="filters.limit"
        label="Preview Limit"
        type="number"
        label-mode="left"
        :compact="true"
        @enter="$emit('preview')"
      />
    </div>

    <div class="location-grade-toolbar__bottom">
      <AsrsFeedback
        class="location-grade-toolbar__feedback"
        :type="feedbackType"
        :message="feedbackMessage"
      />

      <div class="location-grade-toolbar__actions">
        <AsrsActionButton
          variant="secondary"
          :loading="loadingPreview"
          loading-text="Preview..."
          @click="$emit('preview')"
        >
          Preview
        </AsrsActionButton>

        <AsrsActionButton
          variant="primary"
          :loading="loadingExecute"
          loading-text="재산출 중..."
          @click="$emit('execute')"
        >
          등급 재산출
        </AsrsActionButton>

        <AsrsActionButton variant="ghost" @click="$emit('reset-filters')">
          조건 초기화
        </AsrsActionButton>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import AsrsFeedback from '@/views/asrs/shared/components/ui/AsrsFeedback.vue';
import type {
  LocationGradeFilters,
  SelectOption,
} from '../types';

defineProps<{
  filters: LocationGradeFilters;
  areaOptions: SelectOption[];
  purposeOptions: SelectOption[];
  feedbackType?: 'info' | 'success' | 'warning' | 'error';
  feedbackMessage?: string;
  loadingPreview?: boolean;
  loadingExecute?: boolean;
}>();

defineEmits<{
  (e: 'preview'): void;
  (e: 'execute'): void;
  (e: 'reset-filters'): void;
}>();
</script>
