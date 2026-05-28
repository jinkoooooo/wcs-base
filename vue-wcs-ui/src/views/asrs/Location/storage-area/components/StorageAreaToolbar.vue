<template>
  <section class="storage-area-toolbar asrs-ui-toolbar">
    <div class="storage-area-toolbar__fields">
      <AsrsFormField
        v-model="filters.centerCode"
        label="Center"
        type="select"
        label-mode="left"
        :compact="true"
        :options="centerOptionsWithAll"
        @enter="$emit('search')"
      />

      <AsrsFormField
        v-model="filters.areaCode"
        label="Area Code"
        label-mode="left"
        :compact="true"
        placeholder="예: ASRS1"
        @enter="$emit('search')"
      />

      <AsrsFormField
        v-model="filters.areaName"
        label="Area Name"
        label-mode="left"
        :compact="true"
        placeholder="아레아명"
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

    <div class="storage-area-toolbar__bottom">
      <AsrsFeedback
        class="storage-area-toolbar__feedback"
        :type="feedbackType"
        :message="feedbackMessage"
      />

      <div class="storage-area-toolbar__actions">
        <AsrsActionButton variant="secondary" @click="$emit('search')">
          조회
        </AsrsActionButton>

        <AsrsActionButton variant="ghost" @click="$emit('reset-filters')">
          조건 초기화
        </AsrsActionButton>

        <AsrsActionButton variant="primary" @click="$emit('create')">
          신규
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
import type { SelectOption, StorageAreaFilters } from '../types';

const props = defineProps<{
  filters: StorageAreaFilters;
  centerOptions: SelectOption[];
  activeOptions: SelectOption[];
  feedbackType?: 'info' | 'success' | 'warning' | 'error';
  feedbackMessage?: string;
}>();

defineEmits<{
  (e: 'search'): void;
  (e: 'reset-filters'): void;
  (e: 'create'): void;
}>();

const centerOptionsWithAll = computed<SelectOption[]>(() => [
  { label: '전체', value: '' },
  ...props.centerOptions,
]);
</script>
