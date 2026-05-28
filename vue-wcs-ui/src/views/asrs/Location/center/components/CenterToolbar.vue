<template>
  <section class="center-toolbar asrs-ui-toolbar">
    <div class="center-toolbar__fields">
      <AsrsFormField
        v-model="filters.centerCode"
        label="Center Code"
        label-mode="left"
        :compact="true"
        placeholder="예: ICN_ASRS"
        @enter="$emit('search')"
      />

      <AsrsFormField
        v-model="filters.centerName"
        label="Center Name"
        label-mode="left"
        :compact="true"
        placeholder="센터명"
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

    <div class="center-toolbar__bottom">
      <AsrsFeedback
        class="center-toolbar__feedback"
        :type="feedbackType"
        :message="feedbackMessage"
      />

      <div class="center-toolbar__actions">
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
import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import AsrsFeedback from '@/views/asrs/shared/components/ui/AsrsFeedback.vue';
import type { CenterFilters, SelectOption } from '../types';

defineProps<{
  filters: CenterFilters;
  activeOptions: SelectOption[];
  feedbackType?: 'info' | 'success' | 'warning' | 'error';
  feedbackMessage?: string;
}>();

defineEmits<{
  (e: 'search'): void;
  (e: 'reset-filters'): void;
  (e: 'create'): void;
}>();
</script>
