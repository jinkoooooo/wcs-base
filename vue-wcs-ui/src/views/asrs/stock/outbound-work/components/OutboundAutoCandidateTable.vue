<template>
  <section class="outbound-auto-panel">
    <div class="outbound-auto-panel__header">
      <div>
        <p class="outbound-auto-panel__eyebrow">AUTO CANDIDATES</p>
        <h3 class="outbound-auto-panel__title">자동할당 후보 목록</h3>
      </div>

      <span class="outbound-auto-panel__count">{{ rows.length }}건</span>
    </div>

    <div class="outbound-auto-panel__body">
      <div class="outbound-auto-panel__table-wrap">
        <table class="asrs-ui-table">
          <thead>
          <tr>
            <th>Rank</th>
            <th>Stock Unit</th>
            <th>Location</th>
            <th>Lot</th>
            <th>Qty</th>
            <th>Reserved</th>
            <th>Available</th>
            <th>Status</th>
            <th>선택가능</th>
          </tr>
          </thead>

          <tbody>
          <tr v-if="loading">
            <td colspan="9" class="outbound-auto-panel__empty">조회 중입니다...</td>
          </tr>

          <tr v-else-if="!rows.length">
            <td colspan="9" class="outbound-auto-panel__empty">후보가 없습니다.</td>
          </tr>

          <tr
            v-for="row in rows"
            :key="row.stockUnitNo"
            :class="{ 'asrs-ui-table__row--selected': selectedStockUnitNo === row.stockUnitNo }"
            @click="row.selectable && $emit('select', row)"
          >
            <td>{{ row.candidateRank }}</td>
            <td class="asrs-ui-table__key">{{ row.stockUnitNo }}</td>
            <td>{{ row.locationCode || '-' }}</td>
            <td>{{ row.lotNo || '-' }}</td>
            <td>{{ row.qty }}</td>
            <td>{{ row.reservedQty }}</td>
            <td>{{ row.availableQty }}</td>
            <td>{{ row.stockStatusCode || '-' }}</td>
            <td>{{ row.selectable ? 'Y' : 'N' }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { OutboundAutoCandidateRow } from '../types';

defineProps<{
  rows: OutboundAutoCandidateRow[];
  loading?: boolean;
  selectedStockUnitNo?: string;
}>();

defineEmits<{
  (e: 'select', row: OutboundAutoCandidateRow): void;
}>();
</script>
