<template>
  <AsrsPageShell headerMode="toolbarOnly" title="">
    <template #toolbar>
      <CenterToolbar
        :filters="filters"
        :active-options="activeFilterOptions"
        :feedback-type="feedback.type"
        :feedback-message="feedback.message"
        @search="search"
        @reset-filters="handleResetFilters"
        @create="startCreate"
      />
    </template>

    <section class="center-page">
      <section class="center-page__content">
        <div class="center-page__left">
          <CenterTable
            :rows="rows"
            :selected-center-code="selectedRow?.centerCode"
            :loading="loading.search"
            @select="selectRow"
          />
        </div>

        <div class="center-page__right">
          <CenterDetailPanel
            :form="form"
            :center-type-options="centerTypeOptions"
            :timezone-options="timezoneOptions"
            :active-options="formActiveOptions"
            :edit-mode="editMode"
            :linked-area-count="selectedRow?.linkedAreaCount ?? 0"
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
import './styles/center.css';

import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
import CenterToolbar from './components/CenterToolbar.vue';
import CenterTable from './components/CenterTable.vue';
import CenterDetailPanel from './components/CenterDetailPanel.vue';
import { useCenterManager } from './composables/useCenterManager';
import {
  ACTIVE_YN_OPTIONS,
  CENTER_TYPE_OPTIONS,
  FORM_ACTIVE_YN_OPTIONS,
  TIMEZONE_OPTIONS,
} from './types';

const {
  filters,
  form,
  rows,
  selectedRow,
  editMode,
  loading,
  feedback,
  totalCount,
  activeCount,
  search,
  selectRow,
  startCreate,
  resetFilters,
  resetFormOnly,
  save,
  remove,
} = useCenterManager();

const activeFilterOptions = ACTIVE_YN_OPTIONS;
const formActiveOptions = FORM_ACTIVE_YN_OPTIONS;
const centerTypeOptions = CENTER_TYPE_OPTIONS;
const timezoneOptions = TIMEZONE_OPTIONS;

function handleResetFilters() {
  resetFilters();
  search();
}

function handleResetForm() {
  if (editMode.value === 'create') {
    resetFormOnly();
    return;
  }

  if (selectedRow.value?.centerCode) {
    selectRow(selectedRow.value);
  }
}
</script>
