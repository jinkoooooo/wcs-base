<template>
  <AsrsPanel eyebrow="Stock Detail" title="상세 정보">
    <template #actions>
      <div class="stock-overview-tab-group">
        <button
          type="button"
          class="stock-overview-tab-button"
          :class="{ 'stock-overview-tab-button--active': activeTab === 'detail' }"
          @click="activeTab = 'detail'"
        >
          상세
        </button>
        <button
          type="button"
          class="stock-overview-tab-button"
          :class="{ 'stock-overview-tab-button--active': activeTab === 'history' }"
          @click="activeTab = 'history'"
        >
          이력
        </button>
      </div>
    </template>

    <template v-if="activeTab === 'detail'">
      <div v-if="!stock" class="stock-overview-detail__scroll stock-overview-detail__scroll--fixed">
        <AsrsEmptyState message="재고를 선택하면 상세 정보가 표시됩니다." />
      </div>

      <div v-else class="stock-overview-detail__scroll stock-overview-detail__scroll--fixed">
        <div class="stock-overview-info-grid">
          <div class="stock-overview-info-item">
            <span class="stock-overview-info-item__label">Stock Unit No</span>
            <strong>{{ stock.stockUnitNo }}</strong>
          </div>

          <div class="stock-overview-info-item">
            <span class="stock-overview-info-item__label">Item Code</span>
            <strong>{{ stock.itemCode || '-' }}</strong>
          </div>

          <div class="stock-overview-info-item">
            <span class="stock-overview-info-item__label">Area Code</span>
            <strong>{{ stock.areaCode || '-' }}</strong>
          </div>

          <div class="stock-overview-info-item">
            <span class="stock-overview-info-item__label">Location Code</span>
            <strong>{{ stock.locationCode || '-' }}</strong>
          </div>

          <div class="stock-overview-info-item">
            <span class="stock-overview-info-item__label">Qty</span>
            <strong>{{ stock.qty ?? 0 }}</strong>
          </div>

          <div class="stock-overview-info-item">
            <span class="stock-overview-info-item__label">Reserved Qty</span>
            <strong>{{ stock.reservedQty ?? 0 }}</strong>
          </div>

          <div class="stock-overview-info-item">
            <span class="stock-overview-info-item__label">Lot No</span>
            <strong>{{ stock.lotNo || '-' }}</strong>
          </div>

          <div class="stock-overview-info-item">
            <span class="stock-overview-info-item__label">Status</span>
            <strong>{{ stock.stockStatusCode || '-' }}</strong>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="asrs-ui-table-wrap stock-overview-history-wrap">
        <table class="asrs-ui-table">
          <thead>
          <tr>
            <th>Txn Type</th>
            <th>Txn At</th>
            <th>From</th>
            <th>To</th>
            <th>Qty</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="loadingHistory">
            <td colspan="5" class="stock-overview-table__empty-cell">
              이력 조회 중입니다...
            </td>
          </tr>

          <tr v-else-if="!historyRows.length">
            <td colspan="5" class="stock-overview-table__empty-cell">
              이력이 없습니다.
            </td>
          </tr>

          <tr v-for="row in historyRows" :key="`${row.txnType}-${row.txnAt}-${row.qty}`">
            <td>{{ row.txnType }}</td>
            <td>{{ row.txnAt }}</td>
            <td>{{ row.fromLocationCode }}</td>
            <td>{{ row.toLocationCode }}</td>
            <td>{{ row.qty }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </template>
  </AsrsPanel>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import AsrsPanel from '@/views/asrs/shared/components/ui/AsrsPanel.vue';
import AsrsEmptyState from '@/views/asrs/shared/components/ui/AsrsEmptyState.vue';
import type {
  StockOverviewHistoryRow,
  StockOverviewRow,
} from '../types';

const activeTab = ref<'detail' | 'history'>('detail');

defineProps<{
  stock: StockOverviewRow | null;
  historyRows: StockOverviewHistoryRow[];
  loadingHistory?: boolean;
}>();
</script>
