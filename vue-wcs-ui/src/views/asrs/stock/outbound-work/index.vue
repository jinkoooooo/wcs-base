<template>
  <AsrsPageShell headerMode="toolbarOnly" title="">
    <template #toolbar>
      <OutboundWorkToolbar
        :active-tab="activeTab"
        :form="form"
        :area-options="areaOptions"
        :loading="loading"
        :feedback-type="feedback.type"
        :feedback-message="feedback.message"
        @change-tab="changeTab"
        @search="runSearch"
        @auto-search="runAutoSearch"
        @reset="initialize"
      />
    </template>

    <section class="outbound-work-page">
      <div class="outbound-work-left">
        <OutboundAutoCandidateTable
          v-if="activeTab === 'auto'"
          :rows="autoCandidateRows"
          :loading="loading.autoSearch"
          :selected-stock-unit-no="selectedStock?.stockUnitNo"
          @select="selectAutoCandidate"
        />

        <OutboundStockTable
          v-else
          :rows="stockRows"
          :loading="loading.search"
          :selected-stock-unit-no="selectedStock?.stockUnitNo"
          @select="selectStock"
        />
      </div>

      <div class="outbound-work-right">
        <OutboundSidePanel
          :form="form"
          :selected-stock="selectedStock"
          :allocation-rows="allocationRows"
          :selected-allocation="selectedAllocation"
          :history-rows="historyRows"
          :loading="loading"
          @allocate="allocateSelectedStock"
          @partial-out="partialOutSelectedStock"
          @full-out="fullOutSelectedStock"
          @release="releaseSelectedAllocation"
          @select-allocation="selectAllocation"
        />
      </div>
    </section>
  </AsrsPageShell>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';

import '@/views/asrs/shared/styles/asrs-ui-shared.css';
import '@/views/asrs/shared/styles/asrs-ui-form.css';
import '@/views/asrs/shared/styles/asrs-ui-table.css';
import '@/views/asrs/shared/styles/asrs-ui-panel.css';
import './styles/outbound-work.css';

import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
import { useOutboundWork } from './composables/useOutboundWork';
import OutboundWorkToolbar from './components/OutboundWorkToolbar.vue';
import OutboundStockTable from './components/OutboundStockTable.vue';
import OutboundAutoCandidateTable from './components/OutboundAutoCandidateTable.vue';
import OutboundSidePanel from './components/OutboundSidePanel.vue';

const {
  activeTab,
  form,
  stockRows,
  autoCandidateRows,
  selectedStock,
  allocationRows,
  selectedAllocation,
  historyRows,
  loading,
  feedback,
  areaOptions,
  initialize,
  changeTab,
  runSearch,
  runAutoSearch,
  selectStock,
  selectAutoCandidate,
  selectAllocation,
  allocateSelectedStock,
  partialOutSelectedStock,
  fullOutSelectedStock,
  releaseSelectedAllocation,
} = useOutboundWork();

onMounted(() => {
  initialize();
});
</script>
