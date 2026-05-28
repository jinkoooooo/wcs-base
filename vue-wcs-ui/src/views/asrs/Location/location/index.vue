<template>
  <AsrsPageShell headerMode="toolbarOnly" title="">
    <template #toolbar>
      <LocationToolbar
        :filters="filters"
        :area-options="areaOptions"
        :location-type-options="locationTypeOptions"
        :active-options="activeFilterOptions"
        :feedback-type="feedback.type"
        :feedback-message="feedback.message"
        @search="search"
        @reset-filters="handleResetFilters"
        @bulk-delete="bulkRemove"
        @create="startCreate"
      />
    </template>

    <section class="location-page">
      <section class="location-page__content">
        <div class="location-page__left">
          <LocationTable
            :rows="rows"
            :selected-key="selectedRow ? `${selectedRow.areaCode}-${selectedRow.locationCode}` : ''"
            :loading="loading.search"
            @select="selectRow"
          />
        </div>

        <div class="location-page__right">
          <LocationDetailPanel
            :form="form"
            :area-options="areaOptions"
            :side-code-options="sideCodeOptions"
            :location-type-options="locationTypeOptions"
            :usage-status-options="usageStatusOptions"
            :yn-options="formYnOptions"
            :item-category-options="itemCategoryOptions"
            :access-point-options="accessPointOptions"
            :location-grade-options="locationGradeOptions"
            :edit-mode="editMode"
            :loading-save="loading.save"
            :loading-delete="loading.delete"
            @change-area-code="changeAreaCode"
            @save="save"
            @reset-form="handleResetForm"
            @delete="remove"
          />
        </div>
      </section>
    </section>
  </AsrsPageShell>
</template>

<script setup lang="ts">
import '@/views/asrs/shared/styles/asrs-ui-shared.css';
import '@/views/asrs/shared/styles/asrs-ui-form.css';
import '@/views/asrs/shared/styles/asrs-ui-table.css';
import '@/views/asrs/shared/styles/asrs-ui-panel.css';
import './styles/location.css';

import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
import LocationToolbar from './components/LocationToolbar.vue';
import LocationTable from './components/LocationTable.vue';
import LocationDetailPanel from './components/LocationDetailPanel.vue';
import { useLocationManager } from './composables/useLocationManager';
import {
  ACTIVE_YN_OPTIONS,
  FORM_YN_OPTIONS,
  LOCATION_GRADE_OPTIONS,
  LOCATION_TYPE_OPTIONS,
  LOCATION_USAGE_STATUS_OPTIONS,
  SIDE_CODE_OPTIONS,
} from './types';

const {
  filters,
  form,
  rows,
  selectedRow,
  editMode,
  areaOptions,
  itemCategoryOptions,
  accessPointOptions,
  loading,
  feedback,
  changeAreaCode,
  search,
  selectRow,
  startCreate,
  resetFilters,
  resetFormOnly,
  save,
  remove,
  bulkRemove,
} = useLocationManager();

const activeFilterOptions = ACTIVE_YN_OPTIONS;
const formYnOptions = FORM_YN_OPTIONS;
const sideCodeOptions = SIDE_CODE_OPTIONS;
const locationTypeOptions = LOCATION_TYPE_OPTIONS;
const usageStatusOptions = LOCATION_USAGE_STATUS_OPTIONS;
const locationGradeOptions = LOCATION_GRADE_OPTIONS;

function handleResetFilters() {
  resetFilters();
  search();
}

function handleResetForm() {
  if (editMode.value === 'create') {
    resetFormOnly();
    return;
  }

  if (selectedRow.value?.areaCode && selectedRow.value?.locationCode) {
    selectRow(selectedRow.value);
  }
}
</script>
