<template>
  <AsrsPanel eyebrow="Stock List" title="재고 목록">
    <template #actions>
      <span class="stock-overview-count-badge">{{ rows.length }}건</span>
    </template>

    <div class="asrs-ui-table-wrap stock-overview-table-wrap">
      <table class="asrs-ui-table">
        <thead>
        <tr>
          <th>Stock Unit No</th>
          <th>Item Code</th>
          <th>Location</th>
          <th>Qty</th>
          <th>Reserved</th>
          <th>Status</th>
          <th>Lot No</th>
          <th>Active</th>
        </tr>
        </thead>

        <tbody>
        <tr v-if="loading">
          <td colspan="8" class="stock-overview-table__empty-cell">
            조회 중입니다...
          </td>
        </tr>

        <tr v-else-if="!rows.length">
          <td colspan="8" class="stock-overview-table__empty-cell">
            조회 결과가 없습니다.
          </td>
        </tr>

        <tr
          v-for="row in rows"
          :key="row.stockUnitNo"
          :class="{ 'asrs-ui-table__row--selected': selectedStockUnitNo === row.stockUnitNo }"
          @click="$emit('select', row)"
        >
          <td class="asrs-ui-table__key">{{ row.stockUnitNo }}</td>
          <td>{{ row.itemCode || '-' }}</td>
          <td>{{ row.locationCode || '-' }}</td>
          <td>{{ row.qty ?? 0 }}</td>
          <td>{{ row.reservedQty ?? 0 }}</td>
          <td>{{ row.stockStatusCode || '-' }}</td>
          <td>{{ row.lotNo || '-' }}</td>
          <td>
            <span
              class="stock-overview-status-badge"
              :class="row.activeYn === 'Y' ? 'stock-overview-status-badge--on' : 'stock-overview-status-badge--off'"
            >
              {{ row.activeYn || '-' }}
            </span>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </AsrsPanel>
</template>

<script setup lang="ts">
import AsrsPanel from '@/views/asrs/shared/components/ui/AsrsPanel.vue';
import type { StockOverviewRow } from '../types';

defineProps<{
  rows: StockOverviewRow[];
  loading?: boolean;
  selectedStockUnitNo?: string;
}>();

defineEmits<{
  (e: 'select', row: StockOverviewRow): void;
}>();
</script>
