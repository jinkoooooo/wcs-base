<template>
  <section class="location-grade-result-panel">
    <div class="location-grade-result-panel__header">
      <div>
        <p class="location-grade-result-panel__eyebrow">PREVIEW / RESULT</p>
        <h3 class="location-grade-result-panel__title">미리보기 / 실행 결과</h3>
      </div>
    </div>

    <div class="location-grade-result-panel__body">
      <div class="location-grade-result-panel__summary-grid">
        <div class="location-grade-result-panel__summary-card">
          <span>Preview Total</span>
          <strong>{{ previewState.totalCount }}</strong>
        </div>

        <div class="location-grade-result-panel__summary-card">
          <span>Preview Rows</span>
          <strong>{{ previewState.previewCount }}</strong>
        </div>

        <div class="location-grade-result-panel__summary-card">
          <span>Target Locations</span>
          <strong>{{ resultState.targetLocationCount }}</strong>
        </div>

        <div class="location-grade-result-panel__summary-card">
          <span>Updated</span>
          <strong>{{ resultState.updatedCount }}</strong>
        </div>
      </div>

      <div class="location-grade-result-panel__grade-grid">
        <div
          class="location-grade-result-panel__grade-card location-grade-result-panel__grade-card--A"
        >
          <span>A Grade</span>
          <strong>{{ resultState.gradeACount }}</strong>
        </div>

        <div
          class="location-grade-result-panel__grade-card location-grade-result-panel__grade-card--B"
        >
          <span>B Grade</span>
          <strong>{{ resultState.gradeBCount }}</strong>
        </div>

        <div
          class="location-grade-result-panel__grade-card location-grade-result-panel__grade-card--C"
        >
          <span>C Grade</span>
          <strong>{{ resultState.gradeCCount }}</strong>
        </div>

        <div
          class="location-grade-result-panel__grade-card location-grade-result-panel__grade-card--D"
        >
          <span>D Grade</span>
          <strong>{{ resultState.gradeDCount }}</strong>
        </div>
      </div>

      <div class="location-grade-result-panel__message">
        <span>Message</span>
        <strong>{{ resultState.message || '-' }}</strong>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
  import type { LocationGradePreviewState, LocationGradeResultState } from '../types';

  defineProps<{
    previewState: LocationGradePreviewState;
    resultState: LocationGradeResultState;
  }>();
</script>
