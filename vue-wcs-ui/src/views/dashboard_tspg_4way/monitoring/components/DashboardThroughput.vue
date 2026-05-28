<!--
  최근 30분 분단위 처리량 — 타입별 라인.
  실 운영 데이터 (tb_wcs_shuttle_order 의 완료 건) 기반.
-->
<template>
  <div class="throughput-card">
    <div class="head">
      <span class="title">📈 처리량 (최근 {{ minutes }}분 · 분단위 완료)</span>
      <span class="legend">
        <span class="lg lg-inbound">입고</span>
        <span class="lg lg-outbound">출고</span>
        <span class="lg lg-move">이동</span>
        <span class="lg lg-putback">재입고</span>
      </span>
    </div>
    <div ref="chartRef" class="chart" />
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch, onMounted, nextTick } from 'vue';
  import { useECharts } from '/@/hooks/web/useECharts';
  import type { ThroughputPoint } from '../api';

  const props = defineProps<{ data: ThroughputPoint[]; minutes?: number }>();

  const chartRef = ref<HTMLDivElement>(null as any);
  const { setOptions } = useECharts(chartRef as any);

  const minutes = props.minutes ?? 30;

  function formatTime(ts: string) {
    if (!ts) return '';
    const d = new Date(ts);
    return [d.getHours(), d.getMinutes()]
      .map((n) => String(n).padStart(2, '0'))
      .join(':');
  }

  function buildOption() {
    const points = props.data ?? [];
    const xData = points.map((p) => formatTime(p.ts));
    return {
      // ▼ 폴링마다 깜빡이지 않게 애니메이션 OFF
      animation: false,
      animationDuration: 0,
      animationDurationUpdate: 0,
      grid: { top: 16, right: 16, bottom: 28, left: 36 },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
      },
      xAxis: {
        type: 'category',
        data: xData,
        axisLine: { lineStyle: { color: 'rgba(255,255,255,0.16)' } },
        axisLabel: { color: '#94a3b8', fontSize: 11 },
        splitLine: { show: false },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { color: '#94a3b8', fontSize: 11 },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
      },
      series: [
        {
          name: '입고', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
          itemStyle: { color: '#3b82f6' },
          areaStyle: { color: 'rgba(59,130,246,0.12)' },
          data: points.map((p) => p.INBOUND),
        },
        {
          name: '출고', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
          itemStyle: { color: '#f59e0b' },
          areaStyle: { color: 'rgba(245,158,11,0.10)' },
          data: points.map((p) => p.OUTBOUND),
        },
        {
          name: '이동', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
          itemStyle: { color: '#8b5cf6' },
          data: points.map((p) => p.MOVE),
        },
        {
          name: '재입고', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
          itemStyle: { color: '#14b8a6' },
          data: points.map((p) => p.PUTBACK),
        },
      ],
    };
  }

  // 데이터가 정말 바뀌었을 때만 redraw — 같은 값으로 polling 되면 setOptions 호출 안 함
  const dataSig = computed(() =>
    (props.data ?? [])
      .map((p) => `${p.ts}|${p.INBOUND}|${p.OUTBOUND}|${p.MOVE}|${p.PUTBACK}`)
      .join(','),
  );

  onMounted(async () => {
    await nextTick();
    setOptions(buildOption() as any);
  });

  watch(dataSig, () => setOptions(buildOption() as any));
</script>

<style lang="scss" scoped>
  @use '../styles/sim-tokens' as *;

  .throughput-card {
    @include sim-card;
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

    .title {
      color: $sim-text-primary;
      font-weight: 600;
      font-size: 15px;
    }
    .legend {
      display: inline-flex;
      gap: $sim-space-md;
      font-size: 12px;
      .lg {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        color: $sim-text-muted;
        &::before {
          content: '';
          width: 8px; height: 8px;
          border-radius: 50%;
          background: currentColor;
        }
      }
      .lg-inbound  { color: $sim-type-inbound; }
      .lg-outbound { color: $sim-type-outbound; }
      .lg-move     { color: $sim-type-move; }
      .lg-putback  { color: $sim-type-putback; }
    }
  }

  .chart {
    width: 100%;
    height: 260px;
  }
</style>
