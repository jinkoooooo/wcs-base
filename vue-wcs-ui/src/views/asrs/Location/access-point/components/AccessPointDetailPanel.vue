<template>
  <section class="access-point-detail-panel">
    <div class="access-point-detail-panel__header">
      <div>
        <p class="access-point-detail-panel__eyebrow">DETAIL</p>
        <h3 class="access-point-detail-panel__title">
          {{ editMode === 'create' ? 'Access Point 신규 등록' : 'Access Point 상세 / 수정' }}
        </h3>
      </div>
    </div>

    <div class="access-point-detail-panel__body">
      <div class="access-point-detail-panel__grid access-point-detail-panel__grid--two">
        <AsrsFormField
          v-model="form.areaCode"
          label="Area"
          type="select"
          label-mode="top"
          :compact="true"
          :options="areaOptions"
        />

        <AsrsFormField
          v-model="form.pointCode"
          label="Point Code"
          label-mode="top"
          :compact="true"
          placeholder="예: AP-L1-LEFT-MAIN"
        />
      </div>

      <div class="access-point-detail-panel__grid access-point-detail-panel__grid--two">
        <AsrsFormField
          v-model="form.pointName"
          label="Point Name"
          label-mode="top"
          :compact="true"
          placeholder="예: 1층 좌측 메인포트"
        />

        <AsrsFormField
          v-model="form.pointType"
          label="Point Type"
          type="select"
          label-mode="top"
          :compact="true"
          :options="pointTypeOptions"
        />
      </div>

      <div class="access-point-detail-panel__section">
        <div class="access-point-detail-panel__section-title">좌표 정보</div>

        <div class="access-point-detail-panel__grid access-point-detail-panel__grid--five">
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

      <div class="access-point-detail-panel__grid access-point-detail-panel__grid--two">
        <AsrsFormField
          v-model="form.useForSortYn"
          label="Use For Sort"
          type="select"
          label-mode="top"
          :compact="true"
          :options="ynOptions"
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

      <div class="access-point-detail-panel__grid access-point-detail-panel__grid--one">
        <AsrsFormField
          v-model="form.description"
          label="Description"
          label-mode="top"
          :compact="true"
          placeholder="설명"
        />
      </div>

      <div class="access-point-detail-panel__section access-point-detail-panel__purpose-section">
        <div class="access-point-detail-panel__section-title">목적 / 우선순위</div>

        <div class="access-point-detail-panel__grid access-point-detail-panel__grid--four">
          <AsrsFormField
            v-model="form.inboundYn"
            label="Inbound"
            type="select"
            label-mode="top"
            :compact="true"
            :options="ynOptions"
          />

          <AsrsFormField
            v-model="form.inboundPriorityNo"
            label="Inbound Priority"
            type="number"
            label-mode="top"
            :compact="true"
          />

          <AsrsFormField
            v-model="form.outboundYn"
            label="Outbound"
            type="select"
            label-mode="top"
            :compact="true"
            :options="ynOptions"
          />

          <AsrsFormField
            v-model="form.outboundPriorityNo"
            label="Outbound Priority"
            type="number"
            label-mode="top"
            :compact="true"
          />
        </div>

        <div class="access-point-detail-panel__grid access-point-detail-panel__grid--four">
          <AsrsFormField
            v-model="form.pickYn"
            label="Pick"
            type="select"
            label-mode="top"
            :compact="true"
            :options="ynOptions"
          />

          <AsrsFormField
            v-model="form.pickPriorityNo"
            label="Pick Priority"
            type="number"
            label-mode="top"
            :compact="true"
          />

          <AsrsFormField
            v-model="form.relocationYn"
            label="Relocation"
            type="select"
            label-mode="top"
            :compact="true"
            :options="ynOptions"
          />

          <AsrsFormField
            v-model="form.relocationPriorityNo"
            label="Relocation Priority"
            type="number"
            label-mode="top"
            :compact="true"
          />
        </div>
      </div>

      <div class="access-point-detail-panel__actions">
        <AsrsActionButton
          variant="primary"
          :loading="loadingSave"
          loading-text="저장 중..."
          @click="$emit('save')"
        >
          저장
        </AsrsActionButton>

        <AsrsActionButton variant="ghost" @click="$emit('reset-form')">
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
  import type { AccessPointForm, SelectOption } from '../types';

  defineProps<{
    form: AccessPointForm;
    areaOptions: SelectOption[];
    pointTypeOptions: SelectOption[];
    sideCodeOptions: SelectOption[];
    ynOptions: SelectOption[];
    editMode: 'create' | 'update';
    loadingSave?: boolean;
    loadingDelete?: boolean;
  }>();

  defineEmits<{
    (e: 'save'): void;
    (e: 'reset-form'): void;
    (e: 'delete'): void;
  }>();
</script>
