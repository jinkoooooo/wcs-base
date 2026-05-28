<template>
  <section class="stock-overview-summary">
    <AsrsStatCard
      label="총 재고 건수"
      :value="summary.totalCount"
      description="현재 조회 결과 기준"
    />

    <AsrsStatCard
      label="활성 재고"
      :value="summary.activeCount"
      description="activeYn = Y 기준"
    />

    <AsrsStatCard
      label="총 수량"
      :value="summary.totalQty"
      description="전체 qty 합계"
    />

    <AsrsStatCard
      label="예약 수량"
      :value="summary.totalReservedQty"
      description="reservedQty 합계"
      accent
    />
  </section>
</template>

<script setup lang="ts">
import AsrsStatCard from '@/views/asrs/shared/components/ui/AsrsStatCard.vue';
import type { StockOverviewSummary } from '../types';

defineProps<{
  summary: StockOverviewSummary;
}>();
</script>
