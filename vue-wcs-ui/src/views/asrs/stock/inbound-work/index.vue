<template>
  <AsrsPageShell headerMode="toolbarOnly" title="">
    <template #toolbar>
      <InboundToolbar
        :form="form"
        :loading="loading"
        :area-options="areaOptions"
        :item-policy="itemPolicy"
        :recommend="recommend"
        :selected-recommend="selectedRecommend"
        :feedback-type="feedback.type"
        :feedback-message="feedback.message"
        :has-draft-rows="draftRows.length > 0"
        @reset-page="resetPage"
        @run-recommend="runRecommend"
        @add-draft="addDraft"
        @submit-inbound="submitInbound"
      />
    </template>

    <section class="inbound-page">
      <InboundSummaryBar
        :summary="summary"
        :recent-history-count="recentHistory.length"
      />

      <section class="inbound-content-grid">
        <div class="inbound-content-grid__left">
          <InboundQueueTable
            :rows="draftRows"
            :selected-row-id="selectedRow?.rowId"
            @select="selectRow"
            @remove="removeDraft"
          />
        </div>

        <div class="inbound-content-grid__right">
          <InboundDetailPanel
            :active-tab="activeTab"
            :selected-row="selectedRow"
            :history-rows="recentHistory"
            @update:active-tab="activeTab = $event"
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
import './styles/inbound-work.css';

import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
import { useInboundWork } from './composables/useInboundWork';
import InboundToolbar from './components/InboundToolbar.vue';
import InboundSummaryBar from './components/InboundSummaryBar.vue';
import InboundQueueTable from './components/InboundQueueTable.vue';
import InboundDetailPanel from './components/InboundDetailPanel.vue';

const {
  form,
  loading,
  feedback,
  itemPolicy,
  activeTab,
  areaOptions,
  draftRows,
  recentHistory,
  selectedRow,
  recommend,
  selectedRecommend,
  summary,
  runRecommend,
  resetPage,
  addDraft,
  selectRow,
  removeDraft,
  submitInbound,
} = useInboundWork();
</script>
