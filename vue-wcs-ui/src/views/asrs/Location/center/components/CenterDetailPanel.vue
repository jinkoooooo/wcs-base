<template>
  <section class="center-detail-panel">
    <div class="center-detail-panel__header">
      <div>
        <p class="center-detail-panel__eyebrow">DETAIL</p>
        <h3 class="center-detail-panel__title">
          {{ editMode === 'create' ? '센터 신규 등록' : '센터 상세 / 수정' }}
        </h3>
      </div>
    </div>

    <div class="center-detail-panel__body">
      <AsrsFormField
        v-model="form.centerCode"
        label="Center Code"
        label-mode="top"
        :compact="true"
        placeholder="예: ICN_ASRS"
      />

      <AsrsFormField
        v-model="form.centerName"
        label="Center Name"
        label-mode="top"
        :compact="true"
        placeholder="센터명"
      />

      <AsrsFormField
        v-model="form.centerType"
        label="Center Type"
        type="select"
        label-mode="top"
        :compact="true"
        :options="centerTypeOptions"
      />

      <AsrsFormField
        v-model="form.timezone"
        label="Timezone"
        type="select"
        label-mode="top"
        :compact="true"
        :options="timezoneOptions"
      />

      <AsrsFormField
        v-model="form.activeYn"
        label="Active"
        type="select"
        label-mode="top"
        :compact="true"
        :options="activeOptions"
      />

      <AsrsFormField
        v-model="form.description"
        label="Description"
        type="textarea"
        label-mode="top"
        :compact="true"
        placeholder="설명"
      />

      <div class="center-detail-panel__meta">
        <span>참조 아레아 수</span>
        <strong>{{ linkedAreaCount }}</strong>
      </div>

      <div class="center-detail-panel__actions">
        <AsrsActionButton
          variant="primary"
          :loading="loadingSave"
          loading-text="저장 중..."
          @click="$emit('save')"
        >
          저장
        </AsrsActionButton>

        <AsrsActionButton
          variant="ghost"
          @click="$emit('reset-form')"
        >
          폼 초기화
        </AsrsActionButton>

        <AsrsActionButton
          variant="danger"
          :disabled="editMode === 'create' || linkedAreaCount > 0"
          :loading="loadingDelete"
          loading-text="삭제 중..."
          @click="$emit('delete')"
        >
          삭제
        </AsrsActionButton>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import type { CenterForm, SelectOption } from '../types';

defineProps<{
  form: CenterForm;
  centerTypeOptions: SelectOption[];
  timezoneOptions: SelectOption[];
  activeOptions: SelectOption[];
  editMode: 'create' | 'update';
  linkedAreaCount: number;
  loadingSave?: boolean;
  loadingDelete?: boolean;
}>();

defineEmits<{
  (e: 'save'): void;
  (e: 'reset-form'): void;
  (e: 'delete'): void;
}>();
</script>
