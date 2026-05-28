<template>
  <section class="location-grade-control-panel">
    <div class="location-grade-control-panel__header">
      <div>
        <p class="location-grade-control-panel__eyebrow">GRADE RULE</p>
        <h3 class="location-grade-control-panel__title">등급 산정 기준</h3>
      </div>
    </div>

    <div class="location-grade-control-panel__body">
      <div class="location-grade-control-panel__section">
        <div class="location-grade-control-panel__section-title"> 누적 비율 설정 </div>

        <div class="location-grade-control-panel__grid">
          <AsrsFormField
            v-model="filters.gradeARatio"
            label="A Grade Ratio"
            type="number"
            label-mode="top"
            :compact="true"
          />

          <AsrsFormField
            v-model="filters.gradeBRatio"
            label="B Grade Ratio"
            type="number"
            label-mode="top"
            :compact="true"
          />

          <AsrsFormField
            v-model="filters.gradeCRatio"
            label="C Grade Ratio"
            type="number"
            label-mode="top"
            :compact="true"
          />
        </div>

        <div class="location-grade-control-panel__guide">
          <div>
            <span>A</span>
            <strong>상위 {{ formatPercent(filters.gradeARatio) }}</strong>
          </div>

          <div>
            <span>B</span>
            <strong>
              {{ formatPercent(filters.gradeARatio) }} 초과 ~
              {{ formatPercent(filters.gradeBRatio) }}
            </strong>
          </div>

          <div>
            <span>C</span>
            <strong>
              {{ formatPercent(filters.gradeBRatio) }} 초과 ~
              {{ formatPercent(filters.gradeCRatio) }}
            </strong>
          </div>

          <div>
            <span>D</span>
            <strong>{{ formatPercent(filters.gradeCRatio) }} 초과</strong>
          </div>
        </div>
      </div>

      <div class="location-grade-control-panel__section">
        <div class="location-grade-control-panel__section-title"> 실행 기준 </div>

        <div class="location-grade-control-panel__info-list">
          <div>
            <span>Area</span>
            <strong>{{ filters.areaCode || '-' }}</strong>
          </div>

          <div>
            <span>Purpose</span>
            <strong>{{ filters.purposeCode || '-' }}</strong>
          </div>

          <div>
            <span>Preview Limit</span>
            <strong>{{ filters.limit }}</strong>
          </div>
        </div>
      </div>

      <div class="location-grade-control-panel__actions">
        <AsrsActionButton
          variant="secondary"
          :loading="loadingPreview"
          loading-text="Preview..."
          @click="$emit('preview')"
        >
          Preview
        </AsrsActionButton>

        <AsrsActionButton
          variant="primary"
          :loading="loadingExecute"
          loading-text="재산출 중..."
          @click="$emit('execute')"
        >
          등급 재산출
        </AsrsActionButton>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
  import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
  import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
  import type { LocationGradeFilters } from '../types';

  defineProps<{
    filters: LocationGradeFilters;
    loadingPreview?: boolean;
    loadingExecute?: boolean;
  }>();

  defineEmits<{
    (e: 'preview'): void;
    (e: 'execute'): void;
  }>();

  function formatPercent(value: number) {
    const num = Number(value || 0);
    return `${Math.round(num * 100)}%`;
  }
</script>
