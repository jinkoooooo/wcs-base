<template>
  <AsrsPageShell headerMode="toolbarOnly" title="">
    <template #toolbar>
      <AccessPointToolbar
        :filters="filters"
        :area-options="areaOptions"
        :point-type-options="pointTypeOptions"
        :purpose-options="purposeOptions"
        :active-options="activeFilterOptions"
        :feedback-type="feedback.type"
        :feedback-message="feedback.message"
        @search="search"
        @reset-filters="handleResetFilters"
        @create="startCreate"
      />
    </template>

    <section class="access-point-page">
      <section class="access-point-page__content">
        <div class="access-point-page__left">
          <AccessPointTable
            :rows="rows"
            :selected-key="selectedRow ? `${selectedRow.areaCode}-${selectedRow.pointCode}` : ''"
            :loading="loading.search"
            @select="selectRow"
          />
        </div>

        <div class="access-point-page__right">
          <AccessPointDetailPanel
            :form="form"
            :area-options="areaOptions"
            :point-type-options="pointTypeOptions"
            :side-code-options="sideCodeOptions"
            :yn-options="formYnOptions"
            :edit-mode="editMode"
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
  import './styles/access-point.css';

  import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
  import AccessPointToolbar from './components/AccessPointToolbar.vue';
  import AccessPointTable from './components/AccessPointTable.vue';
  import AccessPointDetailPanel from './components/AccessPointDetailPanel.vue';
  import { useAccessPointManager } from './composables/useAccessPointManager';
  import {
    ACCESS_POINT_TYPE_OPTIONS,
    ACTIVE_YN_OPTIONS,
    FORM_YN_OPTIONS,
    PURPOSE_CODE_OPTIONS,
    SIDE_CODE_OPTIONS,
  } from './types';

  const {
    filters,
    form,
    rows,
    selectedRow,
    editMode,
    areaOptions,
    loading,
    feedback,
    search,
    selectRow,
    startCreate,
    resetFilters,
    resetFormOnly,
    save,
    remove,
  } = useAccessPointManager();

  const activeFilterOptions = ACTIVE_YN_OPTIONS;
  const formYnOptions = FORM_YN_OPTIONS;
  const sideCodeOptions = SIDE_CODE_OPTIONS;
  const pointTypeOptions = ACCESS_POINT_TYPE_OPTIONS;
  const purposeOptions = PURPOSE_CODE_OPTIONS;

  function handleResetFilters() {
    resetFilters();
    search();
  }

  function handleResetForm() {
    if (editMode.value === 'create') {
      resetFormOnly();
      return;
    }

    if (selectedRow.value?.areaCode && selectedRow.value?.pointCode) {
      selectRow(selectedRow.value);
    }
  }
</script>
