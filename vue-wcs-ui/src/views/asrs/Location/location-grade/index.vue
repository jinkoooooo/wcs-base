<template>
  <AsrsPageShell headerMode="toolbarOnly" title="">
    <template #toolbar>
      <LocationGradeToolbar
        :filters="filters"
        :area-options="areaOptions"
        :purpose-options="purposeOptions"
        :feedback-type="feedback.type"
        :feedback-message="feedback.message"
        :loading-preview="loading.preview"
        :loading-execute="loading.execute"
        @preview="preview"
        @execute="execute"
        @reset-filters="handleResetFilters"
      />
    </template>

    <section class="location-grade-page">
      <section class="location-grade-page__content">
        <div class="location-grade-page__left">
          <LocationGradePreviewTable
            :rows="previewState.rows"
            :total-count="previewState.totalCount"
            :loading="loading.preview"
          />
        </div>

        <div class="location-grade-page__right">
          <LocationGradeControlPanel
            :filters="filters"
            :loading-preview="loading.preview"
            :loading-execute="loading.execute"
            @preview="preview"
            @execute="execute"
          />
        </div>
      </section>

      <section class="location-grade-page__bottom">
        <LocationGradeResultPanel :preview-state="previewState" :result-state="resultState" />
      </section>
    </section>
  </AsrsPageShell>
</template>

<script setup lang="ts">
  import '@/views/asrs/shared/styles/asrs-ui-shared.css';
  import '@/views/asrs/shared/styles/asrs-ui-form.css';
  import '@/views/asrs/shared/styles/asrs-ui-table.css';
  import '@/views/asrs/shared/styles/asrs-ui-panel.css';
  import './styles/location-grade.css';

  import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
  import LocationGradeToolbar from './components/LocationGradeToolbar.vue';
  import LocationGradePreviewTable from './components/LocationGradePreviewTable.vue';
  import LocationGradeControlPanel from './components/LocationGradeControlPanel.vue';
  import LocationGradeResultPanel from './components/LocationGradeResultPanel.vue';
  import { useLocationGradeManager } from './composables/useLocationGradeManager';
  import { PURPOSE_CODE_OPTIONS } from './types';

  const {
    filters,
    areaOptions,
    previewState,
    resultState,
    loading,
    feedback,
    preview,
    execute,
    resetFilters,
  } = useLocationGradeManager();

  const purposeOptions = PURPOSE_CODE_OPTIONS;

  function handleResetFilters() {
    resetFilters();
  }
</script>
