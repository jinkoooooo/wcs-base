<template>
  <section class="location-profile-detail-panel">
    <div class="location-profile-detail-panel__header">
      <div>
        <p class="location-profile-detail-panel__eyebrow">DETAIL</p>
        <h3 class="location-profile-detail-panel__title">
          {{ editMode === 'create' ? '로케이션 프로필 신규 등록' : '로케이션 프로필 상세 / 수정' }}
        </h3>
      </div>
    </div>

    <div class="location-profile-detail-panel__body">
      <div class="location-profile-detail-panel__grid location-profile-detail-panel__grid--two">
        <AsrsFormField
          v-model="form.areaCode"
          label="Area"
          type="select"
          label-mode="top"
          :compact="true"
          :options="areaOptions"
        />

        <AsrsFormField
          v-model="form.profileCode"
          label="Profile Code"
          label-mode="top"
          :compact="true"
          placeholder="예: STD_2AISLE"
        />

        <AsrsFormField
          v-model="form.profileName"
          label="Profile Name"
          label-mode="top"
          :compact="true"
          placeholder="프로필명"
        />

        <AsrsFormField
          v-model="form.locationType"
          label="Location Type"
          type="select"
          label-mode="top"
          :compact="true"
          :options="locationTypeOptions"
        />
      </div>

      <div class="location-profile-detail-panel__section">
        <div class="location-profile-detail-panel__section-title">좌표 범위</div>

        <div class="location-profile-detail-panel__grid location-profile-detail-panel__grid--range">
          <AsrsFormField
            v-model="form.aisleStart"
            label="Aisle Start"
            type="number"
            label-mode="top"
            :compact="true"
          />
          <AsrsFormField
            v-model="form.aisleEnd"
            label="Aisle End"
            type="number"
            label-mode="top"
            :compact="true"
          />

          <AsrsFormField
            v-model="form.bayStart"
            label="Bay Start"
            type="number"
            label-mode="top"
            :compact="true"
          />
          <AsrsFormField
            v-model="form.bayEnd"
            label="Bay End"
            type="number"
            label-mode="top"
            :compact="true"
          />

          <AsrsFormField
            v-model="form.levelStart"
            label="Level Start"
            type="number"
            label-mode="top"
            :compact="true"
          />
          <AsrsFormField
            v-model="form.levelEnd"
            label="Level End"
            type="number"
            label-mode="top"
            :compact="true"
          />

          <AsrsFormField
            v-model="form.depthStart"
            label="Depth Start"
            type="number"
            label-mode="top"
            :compact="true"
          />
          <AsrsFormField
            v-model="form.depthEnd"
            label="Depth End"
            type="number"
            label-mode="top"
            :compact="true"
          />
        </div>
      </div>

      <div class="location-profile-detail-panel__grid location-profile-detail-panel__grid--two">
        <AsrsFormField
          v-model="form.sideCodes"
          label="Side Codes"
          label-mode="top"
          :compact="true"
          placeholder="예: L,R"
        />

        <AsrsFormField
          v-model="form.activeYn"
          label="Active"
          type="select"
          label-mode="top"
          :compact="true"
          :options="ynOptions"
        />
      </div>

      <div class="location-profile-detail-panel__grid location-profile-detail-panel__grid--three">
        <AsrsFormField
          v-model="form.mixedLoadYn"
          label="Mixed Load"
          type="select"
          label-mode="top"
          :compact="true"
          :options="ynOptions"
        />

        <AsrsFormField
          v-model="form.inboundAllowedYn"
          label="Inbound Allowed"
          type="select"
          label-mode="top"
          :compact="true"
          :options="ynOptions"
        />

        <AsrsFormField
          v-model="form.outboundAllowedYn"
          label="Outbound Allowed"
          type="select"
          label-mode="top"
          :compact="true"
          :options="ynOptions"
        />
      </div>

      <div class="location-profile-detail-panel__section">
        <div class="location-profile-detail-panel__section-title">코드 패턴</div>

        <div class="location-profile-detail-panel__grid location-profile-detail-panel__grid--one">
          <AsrsFormField
            v-model="form.codePatternPreset"
            label="Pattern Preset"
            type="select"
            label-mode="top"
            :compact="true"
            :options="codePatternPresetOptions"
          />

          <AsrsFormField
            v-if="form.codePatternPreset === '__CUSTOM__'"
            v-model="form.codePatternCustom"
            label="Custom Pattern"
            label-mode="top"
            :compact="true"
            placeholder="예: {AREA}-A{AISLE}-S{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}"
          />

          <div class="location-profile-detail-panel__pattern-preview">
            <span>적용 패턴</span>
            <strong>{{ effectiveCodePattern || '-' }}</strong>
          </div>
        </div>
      </div>

      <div class="location-profile-detail-panel__meta">
        <div class="location-profile-detail-panel__meta-item">
          <span>참조 로케이션 수</span>
          <strong>{{ linkedLocationCount }}</strong>
        </div>
      </div>

      <div class="location-profile-detail-panel__actions">
        <AsrsActionButton
          variant="primary"
          :loading="loadingSave"
          loading-text="저장 중..."
          @click="$emit('save')"
        >
          저장
        </AsrsActionButton>

        <AsrsActionButton
          variant="secondary"
          :loading="loadingPreview"
          loading-text="Preview..."
          @click="$emit('preview')"
        >
          Preview
        </AsrsActionButton>

        <AsrsActionButton
          variant="secondary"
          :loading="loadingGenerate"
          loading-text="Generate..."
          @click="$emit('generate')"
        >
          Generate
        </AsrsActionButton>

        <AsrsActionButton
          variant="ghost"
          @click="$emit('reset-form')"
        >
          폼 초기화
        </AsrsActionButton>

        <AsrsActionButton
          variant="danger"
          :disabled="editMode === 'create' || linkedLocationCount > 0"
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
import type {
  LocationProfileForm,
  SelectOption,
} from '../types';

defineProps<{
  form: LocationProfileForm;
  areaOptions: SelectOption[];
  locationTypeOptions: SelectOption[];
  ynOptions: SelectOption[];
  codePatternPresetOptions: SelectOption[];
  effectiveCodePattern: string;
  editMode: 'create' | 'update';
  linkedLocationCount: number;
  loadingSave?: boolean;
  loadingDelete?: boolean;
  loadingPreview?: boolean;
  loadingGenerate?: boolean;
}>();

defineEmits<{
  (e: 'save'): void;
  (e: 'preview'): void;
  (e: 'generate'): void;
  (e: 'reset-form'): void;
  (e: 'delete'): void;
}>();
</script>
