<template>
  <section class="storage-area-detail-panel">
    <div class="storage-area-detail-panel__header">
      <div>
        <p class="storage-area-detail-panel__eyebrow">DETAIL</p>
        <h3 class="storage-area-detail-panel__title">
          {{ editMode === 'create' ? '아레아 신규 등록' : '아레아 상세 / 수정' }}
        </h3>
      </div>
    </div>

    <div class="storage-area-detail-panel__body">
      <AsrsFormField
        v-model="form.centerCode"
        label="Center"
        type="select"
        label-mode="top"
        :compact="true"
        :options="centerOptions"
      />

      <AsrsFormField
        v-model="form.areaCode"
        label="Area Code"
        label-mode="top"
        :compact="true"
        placeholder="예: ASRS1"
      />

      <AsrsFormField
        v-model="form.areaName"
        label="Area Name"
        label-mode="top"
        :compact="true"
        placeholder="아레아명"
      />

      <AsrsFormField
        v-model="form.areaType"
        label="Area Type"
        type="select"
        label-mode="top"
        :compact="true"
        :options="areaTypeOptions"
      />

      <AsrsFormField
        v-model="form.operationProfileCode"
        label="Operation Profile"
        type="select"
        label-mode="top"
        :compact="true"
        :options="operationProfileOptions"
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

      <div class="storage-area-detail-panel__meta">
        <div class="storage-area-detail-panel__meta-item">
          <span>로케이션 프로필 수</span>
          <strong>{{ linkedLocationProfileCount }}</strong>
        </div>

        <div class="storage-area-detail-panel__meta-item">
          <span>로케이션 수</span>
          <strong>{{ linkedLocationCount }}</strong>
        </div>
      </div>

      <div class="storage-area-detail-panel__actions">
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
          :disabled="editMode === 'create' || linkedLocationProfileCount > 0 || linkedLocationCount > 0"
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
import type { SelectOption, StorageAreaForm } from '../types';

defineProps<{
  form: StorageAreaForm;
  centerOptions: SelectOption[];
  operationProfileOptions: SelectOption[];
  areaTypeOptions: SelectOption[];
  activeOptions: SelectOption[];
  editMode: 'create' | 'update';
  linkedLocationProfileCount: number;
  linkedLocationCount: number;
  loadingSave?: boolean;
  loadingDelete?: boolean;
}>();

defineEmits<{
  (e: 'save'): void;
  (e: 'reset-form'): void;
  (e: 'delete'): void;
}>();
</script>
