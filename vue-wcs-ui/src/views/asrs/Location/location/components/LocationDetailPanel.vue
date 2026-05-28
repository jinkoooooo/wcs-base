<template>
  <section class="location-detail-panel">
    <div class="location-detail-panel__header">
      <div>
        <p class="location-detail-panel__eyebrow">DETAIL</p>
        <h3 class="location-detail-panel__title">
          {{ editMode === 'create' ? '로케이션 신규 등록' : '로케이션 상세 / 수정' }}
        </h3>
      </div>
    </div>

    <div class="location-detail-panel__body">
      <div class="location-detail-panel__grid location-detail-panel__grid--two">
        <AsrsFormField
          :model-value="form.areaCode"
          label="Area"
          type="select"
          label-mode="top"
          :compact="true"
          :options="areaOptions"
          @update:model-value="$emit('change-area-code', String($event || ''))"
        />

        <AsrsFormField
          v-model="form.locationCode"
          label="Location Code"
          label-mode="top"
          :compact="true"
          placeholder="예: ASRS1-A01-L-B001-L01-D01"
        />
      </div>

      <div class="location-detail-panel__section">
        <div class="location-detail-panel__section-title">좌표 정보</div>

        <div class="location-detail-panel__grid location-detail-panel__grid--five">
          <AsrsFormField
            v-model="form.aisleNo"
            label="Aisle"
            type="number"
            label-mode="top"
            :compact="true"
          />

          <AsrsFormField
            v-model="form.sideCode"
            label="Side"
            type="select"
            label-mode="top"
            :compact="true"
            :options="sideCodeOptions"
          />

          <AsrsFormField
            v-model="form.bayNo"
            label="Bay"
            type="number"
            label-mode="top"
            :compact="true"
          />

          <AsrsFormField
            v-model="form.levelNo"
            label="Level"
            type="number"
            label-mode="top"
            :compact="true"
          />

          <AsrsFormField
            v-model="form.depthNo"
            label="Depth"
            type="number"
            label-mode="top"
            :compact="true"
          />
        </div>
      </div>

      <div class="location-detail-panel__grid location-detail-panel__grid--two">
        <AsrsFormField
          v-model="form.locationType"
          label="Location Type"
          type="select"
          label-mode="top"
          :compact="true"
          :options="locationTypeOptions"
        />

        <AsrsFormField
          v-model="form.usageStatusCode"
          label="Usage Status"
          type="select"
          label-mode="top"
          :compact="true"
          :options="usageStatusOptions"
        />
      </div>

      <div class="location-detail-panel__grid location-detail-panel__grid--four">
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

        <AsrsFormField
          v-model="form.mixedLoadYn"
          label="Mixed Load"
          type="select"
          label-mode="top"
          :compact="true"
          :options="ynOptions"
        />

        <AsrsFormField
          v-model="form.frontPriorityYn"
          label="Front Priority"
          type="select"
          label-mode="top"
          :compact="true"
          :options="ynOptions"
        />
      </div>

      <div class="location-detail-panel__grid location-detail-panel__grid--two">
        <AsrsFormField
          v-model="form.dedicatedItemCategoryCode"
          label="Dedicated Item Category"
          type="select"
          label-mode="top"
          :compact="true"
          :options="itemCategoryOptions"
        />

        <AsrsFormField
          v-model="form.primaryAccessPointCode"
          label="Primary Access Point"
          type="select"
          label-mode="top"
          :compact="true"
          :options="accessPointOptions"
        />
      </div>

      <div class="location-detail-panel__grid location-detail-panel__grid--three">
        <AsrsFormField
          v-model="form.maxWeightG"
          label="Max Weight (g)"
          type="number"
          label-mode="top"
          :compact="true"
        />

        <AsrsFormField
          v-model="form.maxVolumeMm3"
          label="Max Volume (mm³)"
          type="number"
          label-mode="top"
          :compact="true"
        />

        <AsrsFormField
          v-model="form.sortSeq"
          label="Sort Seq"
          type="number"
          label-mode="top"
          :compact="true"
        />
      </div>

      <div class="location-detail-panel__grid location-detail-panel__grid--three">
        <AsrsFormField
          v-model="form.locationGrade"
          label="Location Grade"
          type="select"
          label-mode="top"
          :compact="true"
          :options="locationGradeOptions"
        />

        <AsrsFormField
          v-model="form.accessScore"
          label="Access Score"
          type="number"
          label-mode="top"
          :compact="true"
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

      <div class="location-detail-panel__actions">
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
          :disabled="editMode === 'create'"
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
  LocationForm,
  SelectOption,
} from '../types';

defineProps<{
  form: LocationForm;
  areaOptions: SelectOption[];
  sideCodeOptions: SelectOption[];
  locationTypeOptions: SelectOption[];
  usageStatusOptions: SelectOption[];
  ynOptions: SelectOption[];
  itemCategoryOptions: SelectOption[];
  accessPointOptions: SelectOption[];
  locationGradeOptions: SelectOption[];
  editMode: 'create' | 'update';
  loadingSave?: boolean;
  loadingDelete?: boolean;
}>();

defineEmits<{
  (e: 'change-area-code', areaCode: string): void;
  (e: 'save'): void;
  (e: 'reset-form'): void;
  (e: 'delete'): void;
}>();
</script>
