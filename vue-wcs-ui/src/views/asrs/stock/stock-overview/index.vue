<template>
  <AsrsPageShell headerMode="toolbarOnly" title="">
    <template #toolbar>
      <StockOverviewToolbar
        :search-modes="searchModes"
        :search-mode="searchMode"
        :filters="filters"
        :visible-field-count="visibleFieldCount"
        :area-options="areaOptions"
        :feedback-type="feedback.type"
        :feedback-message="feedback.message"
        @change-search-mode="changeSearchMode"
        @run-search="runSearch"
      />
    </template>

    <template #actions>
      <AsrsActionButton variant="ghost" @click="resetPage">
        초기화
      </AsrsActionButton>

      <AsrsActionButton
        variant="primary"
        :loading="loading.search"
        loading-text="조회 중..."
        @click="runSearch"
      >
        조회
      </AsrsActionButton>
    </template>

    <section class="stock-overview-page">
      <StockOverviewSummaryBar :summary="summary" />

      <section class="stock-overview-content-grid">
        <div class="stock-overview-content-grid__left">
          <StockOverviewTable
            :rows="rows"
            :loading="loading.search"
            :selected-stock-unit-no="selectedStock?.stockUnitNo"
            @select="selectRow"
          />
        </div>

        <div class="stock-overview-content-grid__right">
          <StockOverviewDetailPanel
            :stock="selectedStock"
            :history-rows="historyRows"
            :loading-history="loading.history"
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
import './styles/stock-overview.css';

import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import { useStockOverview } from './composables/useStockOverview';
import StockOverviewToolbar from './components/StockOverviewToolbar.vue';
import StockOverviewSummaryBar from './components/StockOverviewSummaryBar.vue';
import StockOverviewTable from './components/StockOverviewTable.vue';
import StockOverviewDetailPanel from './components/StockOverviewDetailPanel.vue';

const {
  searchModes,
  searchMode,
  filters,
  loading,
  feedback,
  rows,
  selectedStock,
  historyRows,
  summary,
  visibleFieldCount,
  areaOptions, // 이거 까지는 했는데, use클래스에서 뭐해야할지 모르겠어. 전체코드로줘
  changeSearchMode,
  selectRow,
  runSearch,
  resetPage,
} = useStockOverview();
</script>
