<template>
  <AsrsPageShell headerMode="toolbarOnly" title="">
    <template #toolbar>
      <OperationProfileToolbar
        :filters="filters"
        :active-options="activeFilterOptions"
        :feedback-type="feedback.type"
        :feedback-message="feedback.message"
        @search="search"
        @reset-filters="handleResetFilters"
        @create="startCreate"
      />
    </template>

    <section class="operation-profile-page">
      <section class="operation-profile-page__content">
        <div class="operation-profile-page__left">
          <OperationProfileTable
            :rows="rows"
            :selected-profile-code="selectedRow?.profileCode"
            :loading="loading.search"
            @select="selectRow"
          />
        </div>

        <div class="operation-profile-page__right">
          <OperationProfileDetailPanel
            :form="form"
            :industry-options="industryOptions"
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
import './styles/operation-profile.css';

import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
import OperationProfileToolbar from './components/OperationProfileToolbar.vue';
import OperationProfileTable from './components/OperationProfileTable.vue';
import OperationProfileDetailPanel from './components/OperationProfileDetailPanel.vue';
import { useOperationProfileManager } from './composables/useOperationProfileManager';
import {
  ACTIVE_YN_OPTIONS,
  FORM_ACTIVE_YN_OPTIONS,
  INDUSTRY_TYPE_OPTIONS,
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
} = useOperationProfileManager();

const activeFilterOptions = ACTIVE_YN_OPTIONS;
const formActiveOptions = FORM_ACTIVE_YN_OPTIONS;
const industryOptions = INDUSTRY_TYPE_OPTIONS;

function handleResetFilters() {
  resetFilters();
  search();
}

function handleResetForm() {
  if (editMode.value === 'create') {
    resetFormOnly();
    return;
  }

  if (selectedRow.value?.profileCode) {
    selectRow(selectedRow.value);
  }
}
</script>
