<template>
  <section class="inbound-summary">
    <AsrsStatCard
      label="대기 건수"
      :value="summary.pendingCount"
      description="실행 전 기준"
    />

    <AsrsStatCard
      label="총 수량"
      :value="summary.totalQty"
      description="대기 수량 합계"
    />

    <AsrsStatCard
      label="품목 수"
      :value="summary.itemCount"
      description="중복 제외 기준"
    />

    <AsrsStatCard
      label="최근 처리"
      :value="recentHistoryCount"
      description="현재 세션 기준"
      accent
    />
  </section>
</template>

<script setup lang="ts">
import AsrsStatCard from '@/views/asrs/shared/components/ui/AsrsStatCard.vue';
import type { InboundSummary } from '../types';

defineProps<{
  summary: InboundSummary;
  recentHistoryCount: number;
}>();
</script>
