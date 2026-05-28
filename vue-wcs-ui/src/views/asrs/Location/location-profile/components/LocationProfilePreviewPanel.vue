<template>
  <section class="location-profile-preview-panel">
    <div class="location-profile-preview-panel__header">
      <div>
        <p class="location-profile-preview-panel__eyebrow">PREVIEW / RESULT</p>
        <h3 class="location-profile-preview-panel__title">생성 미리보기 / 실행 결과</h3>
      </div>
    </div>

    <div class="location-profile-preview-panel__body">
      <div class="location-profile-preview-panel__summary-grid">
        <div class="location-profile-preview-panel__summary-card">
          <span>Preview Total</span>
          <strong>{{ previewState.totalTargetCount }}</strong>
        </div>

        <div class="location-profile-preview-panel__summary-card">
          <span>Preview Existing</span>
          <strong>{{ previewState.existingCount }}</strong>
        </div>

        <div class="location-profile-preview-panel__summary-card">
          <span>Preview Creatable</span>
          <strong>{{ previewState.creatableCount }}</strong>
        </div>

        <div class="location-profile-preview-panel__summary-card">
          <span>Generate Created</span>
          <strong>{{ generateState.createdCount }}</strong>
        </div>

        <div class="location-profile-preview-panel__summary-card">
          <span>Generate Skipped</span>
          <strong>{{ generateState.skippedCount }}</strong>
        </div>
      </div>

      <div class="location-profile-preview-panel__split">
        <div class="location-profile-preview-panel__list-box">
          <div class="location-profile-preview-panel__list-title">
            Preview Location Codes
          </div>

          <ul class="location-profile-preview-panel__code-list">
            <li
              v-for="code in previewState.previewLocationCodes"
              :key="`preview-${code}`"
            >
              {{ code }}
            </li>

            <li v-if="!previewState.previewLocationCodes.length" class="location-profile-preview-panel__empty">
              Preview 데이터가 없습니다.
            </li>
          </ul>
        </div>

        <div class="location-profile-preview-panel__list-box">
          <div class="location-profile-preview-panel__list-title">
            Created Location Codes
          </div>

          <ul class="location-profile-preview-panel__code-list">
            <li
              v-for="code in generateState.createdLocationCodes"
              :key="`created-${code}`"
            >
              {{ code }}
            </li>

            <li v-if="!generateState.createdLocationCodes.length" class="location-profile-preview-panel__empty">
              생성 결과 데이터가 없습니다.
            </li>
          </ul>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type {
  LocationGenerateState,
  LocationProfilePreviewState,
} from '../types';

defineProps<{
  previewState: LocationProfilePreviewState;
  generateState: LocationGenerateState;
}>();
</script>
