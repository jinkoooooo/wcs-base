<template>
  <AsrsPageShell headerMode="toolbarOnly" title="">
    <template #toolbar>
      <LocationProfileToolbar
        :filters="filters"
        :area-options="areaOptions"
        :active-options="activeFilterOptions"
        :feedback-type="feedback.type"
        :feedback-message="feedback.message"
        @search="search"
        @reset-filters="handleResetFilters"
        @create="startCreate"
      />
    </template>

    <section class="location-profile-page">
      <section class="location-profile-page__content">
        <div class="location-profile-page__left">
          <LocationProfileTable
            :rows="rows"
            :selected-key="selectedRow ? `${selectedRow.areaCode}-${selectedRow.profileCode}` : ''"
            :loading="loading.search"
            @select="selectRow"
          />
        </div>

        <div class="location-profile-page__right">
          <LocationProfileDetailPanel
            :form="form"
            :area-options="areaOptions"
            :location-type-options="locationTypeOptions"
            :yn-options="formYnOptions"
            :code-pattern-preset-options="codePatternPresetOptions"
            :effective-code-pattern="effectiveCodePattern"
            :edit-mode="editMode"
            :linked-location-count="selectedRow?.linkedLocationCount ?? 0"
            :loading-save="loading.save"
            :loading-delete="loading.delete"
            :loading-preview="loading.preview"
            :loading-generate="loading.generate"
            @save="save"
            @preview="preview"
            @generate="generate"
            @reset-form="handleResetForm"
            @delete="remove"
          />
        </div>
      </section>

      <section class="location-profile-page__bottom">
        <LocationProfilePreviewPanel
          :preview-state="previewState"
          :generate-state="generateState"
        />
      </section>
    </section>
  </AsrsPageShell>
</template>

<script setup lang="ts">
  import '@/views/asrs/shared/styles/asrs-ui-shared.css';
  import '@/views/asrs/shared/styles/asrs-ui-form.css';
  import '@/views/asrs/shared/styles/asrs-ui-table.css';
  import '@/views/asrs/shared/styles/asrs-ui-panel.css';
  import './styles/location-profile.css';

  import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
  import LocationProfileToolbar from './components/LocationProfileToolbar.vue';
  import LocationProfileTable from './components/LocationProfileTable.vue';
  import LocationProfileDetailPanel from './components/LocationProfileDetailPanel.vue';
  import LocationProfilePreviewPanel from './components/LocationProfilePreviewPanel.vue';
  import { useLocationProfileManager } from './composables/useLocationProfileManager';
  import {
    ACTIVE_YN_OPTIONS,
    CODE_PATTERN_PRESET_OPTIONS,
    FORM_YN_OPTIONS,
    LOCATION_TYPE_OPTIONS,
  } from './types';

  const {
    filters,
    form,
    rows,
    selectedRow,
    editMode,
    areaOptions,
    previewState,
    generateState,
    effectiveCodePattern,
    loading,
    feedback,
    search,
    selectRow,
    startCreate,
    resetFilters,
    resetFormOnly,
    save,
    remove,
    preview,
    generate,
  } = useLocationProfileManager();

  const activeFilterOptions = ACTIVE_YN_OPTIONS;
  const formYnOptions = FORM_YN_OPTIONS;
  const locationTypeOptions = LOCATION_TYPE_OPTIONS;
  const codePatternPresetOptions = CODE_PATTERN_PRESET_OPTIONS;

  function handleResetFilters() {
    resetFilters();
    search();
  }

  function handleResetForm() {
    if (editMode.value === 'create') {
      resetFormOnly();
      return;
    }

    if (selectedRow.value?.areaCode && selectedRow.value?.profileCode) {
      selectRow(selectedRow.value);
    }
  }
</script>
