<template>
  <section class="access-point-toolbar asrs-ui-toolbar">
    <div class="access-point-toolbar__fields">
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
        v-model="filters.pointCode"
        label="Point Code"
        label-mode="left"
        :compact="true"
        placeholder="예: AP-L1-LEFT-MAIN"
        @enter="$emit('search')"
      />

      <AsrsFormField
        v-model="filters.pointName"
        label="Point Name"
        label-mode="left"
        :compact="true"
        placeholder="포인트명"
        @enter="$emit('search')"
      />

      <AsrsFormField
        v-model="filters.pointType"
        label="Point Type"
        type="select"
        label-mode="left"
        :compact="true"
        :options="pointTypeOptionsWithAll"
        @enter="$emit('search')"
      />

      <AsrsFormField
        v-model="filters.purposeCode"
        label="Purpose"
        type="select"
        label-mode="left"
        :compact="true"
        :options="purposeOptions"
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

    <div class="access-point-toolbar__bottom">
      <AsrsFeedback
        class="access-point-toolbar__feedback"
        :type="feedbackType"
        :message="feedbackMessage"
      />

      <div class="access-point-toolbar__actions">
        <AsrsActionButton variant="secondary" @click="$emit('search')"> 조회 </AsrsActionButton>

        <AsrsActionButton variant="ghost" @click="$emit('reset-filters')">
          조건 초기화
        </AsrsActionButton>

        <AsrsActionButton variant="primary" @click="$emit('create')"> 신규 </AsrsActionButton>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
  import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
  import AsrsFeedback from '@/views/asrs/shared/components/ui/AsrsFeedback.vue';
  import type { AccessPointFilters, SelectOption } from '../types';

  const props = defineProps<{
    filters: AccessPointFilters;
    areaOptions: SelectOption[];
    pointTypeOptions: SelectOption[];
    purposeOptions: SelectOption[];
    activeOptions: SelectOption[];
    feedbackType?: 'info' | 'success' | 'warning' | 'error';
    feedbackMessage?: string;
  }>();

  defineEmits<{
    (e: 'search'): void;
    (e: 'reset-filters'): void;
    (e: 'create'): void;
  }>();

  const areaOptionsWithAll = computed<SelectOption[]>(() => [
    { label: '전체', value: '' },
    ...props.areaOptions,
  ]);

  const pointTypeOptionsWithAll = computed<SelectOption[]>(() => [
    { label: '전체', value: '' },
    ...props.pointTypeOptions,
  ]);
</script>
