<template>
  <section class="panel">
    <div class="panel-header">
      <div>
        <h3 class="panel-title">주요 KPI</h3>
        <p class="panel-desc">일별 처리량 및 운영 지표</p>
      </div>
    </div>

    <div v-if="loading" class="empty-box"> 불러오는 중... </div>

    <div v-else-if="!summary" class="empty-box"> 조회된 데이터가 없습니다. </div>

    <div v-else class="card-grid">
      <div v-for="card in cards" :key="card.label" class="kpi-card">
        <div class="kpi-label">{{ card.label }}</div>
        <div class="kpi-value">{{ card.value }}</div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  const props = defineProps<{
    summary: Record<string, any> | null;
    loading?: boolean;
  }>();

  function pick<T = any>(
    obj: Record<string, any> | null | undefined,
    ...keys: string[]
  ): T | undefined {
    if (!obj) return undefined;
    for (const key of keys) {
      if (obj[key] !== undefined) return obj[key] as T;
    }
    return undefined;
  }

  const cards = computed(() => {
    const s = props.summary;
    if (!s) return [];

    return [
      { label: '총 박스수', value: pick(s, 'totalBoxQty', 'total_box_qty') ?? '-' },
      { label: '정상완료', value: pick(s, 'okBoxQty', 'ok_box_qty') ?? '-' },
      { label: '최종 NG', value: pick(s, 'ngBoxQty', 'ng_box_qty') ?? '-' },
      { label: '실처리 SKU 수', value: pick(s, 'actualSkuQty', 'actual_sku_qty') ?? '-' },
      {
        label: '전체 운영시간',
        value: pick(s, 'totalOperatingTime', 'total_operating_time') ?? '-',
      },
      { label: '전체운영 UPH', value: pick(s, 'totalTimeUph', 'total_time_uph') ?? '-' },
      { label: '파렛타이저 유휴시간', value: pick(s, 'idleTime', 'idle_time') ?? '-' },
      {
        label: '파렛타이저 운영시간',
        value: pick(s, 'palletOperatingTime', 'pallet_operating_time') ?? '-',
      },
      { label: '파렛타이저 UPH', value: pick(s, 'palletTimeUph', 'pallet_time_uph') ?? '-' },
      { label: '평균 처리시간', value: pick(s, 'avgAllTime', 'avg_all_time') ?? '-' },
      { label: '중앙값', value: pick(s, 'medianTime', 'median_time') ?? '-' },
      { label: 'P95', value: pick(s, 'p95Time', 'p95_time') ?? '-' },
    ];
  });
</script>

<style scoped>
  .panel {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 20px;
    padding: 16px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  }

  .panel-header {
    margin-bottom: 14px;
  }

  .panel-title {
    font-size: 18px;
    font-weight: 700;
    color: #0f172a;
  }

  .panel-desc {
    margin-top: 4px;
    font-size: 12px;
    color: #64748b;
  }

  .empty-box {
    padding: 32px 16px;
    border-radius: 16px;
    text-align: center;
    font-size: 13px;
    color: #94a3b8;
    background: #f8fafc;
  }

  .card-grid {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 12px;
  }

  .kpi-card {
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 18px;
    padding: 14px;
  }

  .kpi-label {
    font-size: 11px;
    color: #64748b;
  }

  .kpi-value {
    margin-top: 8px;
    font-size: 22px;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.2;
    word-break: break-word;
  }

  @media (max-width: 1200px) {
    .card-grid {
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .card-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
</style>
