<template>
  <section class="location-toolbar asrs-ui-toolbar">
    <div class="location-toolbar__fields">
      <AsrsFormField
        v-model="filters.areaCode"
        label="Area"
        type="select"
        label-mode="left"
        :compact="true"
        :options="areaOptionsWithAll"
        @enter="$emit('search')"
      />

      <AsrsFormField
        v-model="filters.locationCode"
        label="Location Code"
        label-mode="left"
        :compact="true"
        placeholder="예: ASRS1-A01-L-B001-L01-D01"
        @enter="$emit('search')"
      />

      <AsrsFormField
        v-model="filters.locationType"
        label="Location Type"
        type="select"
        label-mode="left"
        :compact="true"
        :options="locationTypeOptionsWithAll"
        @enter="$emit('search')"
      />

      <AsrsFormField
        v-model="filters.activeYn"
        label="Active"
        type="select"
        label-mode="left"
        :compact="true"
        :options="activeOptions"
        @enter="$emit('search')"
      />
    </div>

    <div class="location-toolbar__bottom">
      <AsrsFeedback
        class="location-toolbar__feedback"
        :type="feedbackType"
        :message="feedbackMessage"
      />

      <div class="location-toolbar__actions">
        <AsrsActionButton variant="secondary" @click="$emit('search')">
          조회
        </AsrsActionButton>

        <AsrsActionButton variant="ghost" @click="$emit('reset-filters')">
          조건 초기화
        </AsrsActionButton>

        <AsrsActionButton variant="primary" @click="$emit('create')">
          신규
        </AsrsActionButton>

        <AsrsActionButton variant="danger" @click="$emit('bulk-delete')">
          조회결과 일괄삭제
        </AsrsActionButton>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import AsrsFeedback from '@/views/asrs/shared/components/ui/AsrsFeedback.vue';
import type {
  LocationFilters,
  SelectOption,
} from '../types';

const props = defineProps<{
  filters: LocationFilters;
  areaOptions: SelectOption[];
  locationTypeOptions: SelectOption[];
  activeOptions: SelectOption[];
  feedbackType?: 'info' | 'success' | 'warning' | 'error';
  feedbackMessage?: string;
}>();

defineEmits<{
  (e: 'search'): void;
  (e: 'reset-filters'): void;
  (e: 'bulk-delete'): void;
  (e: 'create'): void;
}>();

const areaOptionsWithAll = computed<SelectOption[]>(() => [
  { label: '전체', value: '' },
  ...props.areaOptions,
]);

const locationTypeOptionsWithAll = computed<SelectOption[]>(() => [
  { label: '전체', value: '' },
  ...props.locationTypeOptions,
]);
</script>
