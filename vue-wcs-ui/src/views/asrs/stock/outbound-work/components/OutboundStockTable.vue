<template>
  <section class="outbound-stock-panel">
    <div class="outbound-stock-panel__header">
      <div>
        <p class="outbound-stock-panel__eyebrow">STOCK LIST</p>
        <h3 class="outbound-stock-panel__title">출고 대상 재고 목록</h3>
      </div>

      <span class="outbound-stock-panel__count">{{ rows.length }}건</span>
    </div>

    <!-- 중요: body 영역을 별도로 두고 여기서 내부 스크롤 -->
    <div class="outbound-stock-panel__body">
      <div class="outbound-stock-panel__table-wrap">
        <table class="asrs-ui-table">
          <thead>
          <tr>
            <th>Stock Unit No</th>
            <th>Item Code</th>
            <th>Location</th>
            <th>Qty</th>
            <th>Reserved</th>
            <th>Available</th>
            <th>Lot No</th>
            <th>Status</th>
            <th>Active</th>
            <th>Last Txn</th>
          </tr>
          </thead>

          <tbody>
          <tr v-if="loading">
            <td colspan="10" class="outbound-stock-panel__empty">
              조회 중입니다...
            </td>
          </tr>

          <tr v-else-if="!rows.length">
            <td colspan="10" class="outbound-stock-panel__empty">
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
            <td>{{ row.qty }}</td>
            <td>{{ row.reservedQty }}</td>
            <td>{{ row.availableQty }}</td>
            <td>{{ row.lotNo || '-' }}</td>
            <td>{{ row.stockStatusCode || '-' }}</td>
            <td>{{ row.activeYn || '-' }}</td>
            <td>{{ row.lastTxnAt || '-' }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * 출고 대상 재고 그리드.
 *
 * 중요:
 * - 패널 전체가 아니라 body/table-wrap 안에서만 스크롤하도록 구성
 */
import type { OutboundStockRow } from '../types';

defineProps<{
  rows: OutboundStockRow[];
  loading?: boolean;
  selectedStockUnitNo?: string;
}>();

defineEmits<{
  (e: 'select', row: OutboundStockRow): void;
}>();
</script>
