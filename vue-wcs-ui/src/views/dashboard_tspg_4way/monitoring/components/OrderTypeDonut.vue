<!--
  오늘 주문 타입 분포 도넛 — 입고/출고/이동/재입고 비중.
-->
<template>
  <div class="donut-card">
    <div class="head">
      <span class="title">📊 오늘 주문 타입 분포</span>
      <span class="hint">총 {{ total }}건</span>
    </div>

    <div class="content">
      <div ref="chartRef" class="chart" />

      <ul class="legend">
        <li v-for="row in rows" :key="row.label" class="lg-row">
          <span class="lg-dot" :style="{ background: row.color }" />
          <span class="lg-lbl">{{ row.label }}</span>
          <span class="lg-val">{{ row.value }}건</span>
          <span class="lg-pct">{{ row.pct }}%</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch, onMounted, nextTick } from 'vue';
  import { useECharts } from '/@/hooks/web/useECharts';
  import type { ByTypeCounts } from '../api';

  const props = defineProps<{ data: ByTypeCounts | null }>();

  const chartRef = ref<HTMLDivElement>(null as any);
  const { setOptions } = useECharts(chartRef as any);

  const total = computed(() => {
    const d = props.data;
    if (!d) return 0;
    return d.INBOUND + d.OUTBOUND + d.MOVE + d.PUTBACK;
  });

  const rows = computed(() => {
    const d = props.data ?? { INBOUND: 0, OUTBOUND: 0, MOVE: 0, PUTBACK: 0 };
    const t = total.value || 1;
    return [
      { label: '입고',     value: d.INBOUND,  color: '#3b82f6', pct: Math.round((d.INBOUND  / t) * 100) },
      { label: '출고',     value: d.OUTBOUND, color: '#f59e0b', pct: Math.round((d.OUTBOUND / t) * 100) },
      { label: '재고이동', value: d.MOVE,     color: '#8b5cf6', pct: Math.round((d.MOVE     / t) * 100) },
      { label: '재입고',   value: d.PUTBACK,  color: '#14b8a6', pct: Math.round((d.PUTBACK  / t) * 100) },
    ];
  });

  function buildOption() {
    const filled = rows.value.filter((r) => r.value > 0);
    return {
      // 폴링마다 깜빡임 방지
      animation: false,
      animationDuration: 0,
      animationDurationUpdate: 0,
      tooltip: { trigger: 'item', formatter: '{b}: {c}건 ({d}%)' },
      series: [
        {
          type: 'pie',
          radius: ['58%', '82%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: true,
          itemStyle: {
            borderRadius: 6,
            borderColor: '#1a1f2e',
            borderWidth: 2,
          },
          label: {
            show: filled.length > 0,
            color: '#f1f5f9',
            formatter: '{c}',
            fontSize: 13,
            fontWeight: 'bold',
          },
          data: filled.length > 0
            ? filled.map((r) => ({ name: r.label, value: r.value, itemStyle: { color: r.color } }))
            : [{ name: '데이터 없음', value: 1, itemStyle: { color: '#374151' }, label: { show: false } }],
        },
      ],
    };
  }

  // 같은 값일 땐 redraw 안 함
  const dataSig = computed(() => {
    const d = props.data;
    if (!d) return 'null';
    return `${d.INBOUND}|${d.OUTBOUND}|${d.MOVE}|${d.PUTBACK}`;
  });

  onMounted(async () => {
    await nextTick();
    setOptions(buildOption() as any);
  });
  watch(dataSig, () => setOptions(buildOption() as any));
</script>

<style lang="scss" scoped>
  @use '../styles/sim-tokens' as *;

  .donut-card {
    background: $sim-bg-card;
    border: 1px solid $sim-border-default;
    border-radius: $sim-radius-lg;
    padding: $sim-space-lg $sim-space-xl;
    display: flex;
    flex-direction: column;
    gap: $sim-space-md;
  }

  .head {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    border-bottom: 1px solid $sim-border-default;
    padding-bottom: $sim-space-sm;

    .title { color: $sim-text-primary; font-weight: 600; font-size: 15px; }
    .hint  { color: $sim-text-muted;  font-size: 13px; }
  }

  .content {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: $sim-space-lg;
    align-items: center;

    @media (max-width: 720px) { grid-template-columns: 1fr; }
  }

  .chart {
    width: 100%;
    height: 220px;
  }

  .legend {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: $sim-space-sm;
  }

  .lg-row {
    display: grid;
    grid-template-columns: 12px 1fr auto auto;
    gap: $sim-space-sm;
    align-items: center;
    font-size: 13px;
    padding: 8px 12px;
    border-radius: $sim-radius-sm;
    background: $sim-bg-elevated;

    .lg-dot { width: 10px; height: 10px; border-radius: 50%; }
    .lg-lbl { color: $sim-text-secondary; }
    .lg-val { color: $sim-text-primary; font-weight: 600; font-variant-numeric: tabular-nums; }
    .lg-pct { color: $sim-text-muted; font-variant-numeric: tabular-nums; }
  }
</style>
