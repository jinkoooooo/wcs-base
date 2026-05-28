<template>
  <AsrsPageShell headerMode="toolbarOnly" title="">
    <template #toolbar>
      <StorageAreaToolbar
        :filters="filters"
        :center-options="centerOptions"
        :active-options="activeFilterOptions"
        :feedback-type="feedback.type"
        :feedback-message="feedback.message"
        @search="search"
        @reset-filters="handleResetFilters"
        @create="startCreate"
      />
    </template>

    <section class="storage-area-page">
      <section class="storage-area-page__content">
        <div class="storage-area-page__left">
          <StorageAreaTable
            :rows="rows"
            :selected-key="selectedRow ? `${selectedRow.centerCode}-${selectedRow.areaCode}` : ''"
            :loading="loading.search"
            @select="selectRow"
          />
        </div>

        <div class="storage-area-page__right">
          <StorageAreaDetailPanel
            :form="form"
            :center-options="centerOptions"
            :operation-profile-options="operationProfileOptions"
            :area-type-options="areaTypeOptions"
            :active-options="formActiveOptions"
            :edit-mode="editMode"
            :linked-location-profile-count="selectedRow?.linkedLocationProfileCount ?? 0"
            :linked-location-count="selectedRow?.linkedLocationCount ?? 0"
            :loading-save="loading.save"
            :loading-delete="loading.delete"
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
import './styles/storage-area.css';

import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
import StorageAreaToolbar from './components/StorageAreaToolbar.vue';
import StorageAreaTable from './components/StorageAreaTable.vue';
import StorageAreaDetailPanel from './components/StorageAreaDetailPanel.vue';
import { useStorageAreaManager } from './composables/useStorageAreaManager';
import {
  ACTIVE_YN_OPTIONS,
  AREA_TYPE_OPTIONS,
  FORM_ACTIVE_YN_OPTIONS,
} from './types';

const {
  filters,
  form,
  rows,
  selectedRow,
  editMode,
  centerOptions,
  operationProfileOptions,
  loading,
  feedback,
  search,
  selectRow,
  startCreate,
  resetFilters,
  resetFormOnly,
  save,
  remove,
} = useStorageAreaManager();

const activeFilterOptions = ACTIVE_YN_OPTIONS;
const formActiveOptions = FORM_ACTIVE_YN_OPTIONS;
const areaTypeOptions = AREA_TYPE_OPTIONS;

function handleResetFilters() {
  resetFilters();
  search();
}

function handleResetForm() {
  if (editMode.value === 'create') {
    resetFormOnly();
    return;
  }

  if (selectedRow.value?.centerCode && selectedRow.value?.areaCode) {
    selectRow(selectedRow.value);
  }
}
</script>
