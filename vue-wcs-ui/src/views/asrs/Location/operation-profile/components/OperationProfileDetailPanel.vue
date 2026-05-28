<template>
  <section class="operation-profile-detail-panel">
    <div class="operation-profile-detail-panel__header">
      <div>
        <p class="operation-profile-detail-panel__eyebrow">DETAIL</p>
        <h3 class="operation-profile-detail-panel__title">
          {{ editMode === 'create' ? '오퍼레이션 프로필 신규 등록' : '오퍼레이션 프로필 상세 / 수정' }}
        </h3>
      </div>
    </div>

    <div class="operation-profile-detail-panel__body">
      <AsrsFormField
        v-model="form.profileCode"
        label="Profile Code"
        label-mode="top"
        :compact="true"
        placeholder="예: BULK_STD"
      />

      <AsrsFormField
        v-model="form.profileName"
        label="Profile Name"
        label-mode="top"
        :compact="true"
        placeholder="프로필명"
      />

      <AsrsFormField
        v-model="form.industryType"
        label="Industry Type"
        type="select"
        label-mode="top"
        :compact="true"
        :options="industryOptions"
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

      <div v-if="linkedAreaCount >= 0" class="operation-profile-detail-panel__meta">
        <span>참조 아레아 수</span>
        <strong>{{ linkedAreaCount }}</strong>
      </div>

      <div class="operation-profile-detail-panel__actions">
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
import type { OperationProfileForm, SelectOption } from '../types';

defineProps<{
  form: OperationProfileForm;
  industryOptions: SelectOption[];
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
