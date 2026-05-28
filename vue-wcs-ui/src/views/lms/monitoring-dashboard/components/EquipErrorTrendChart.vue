<template>
  <div class="panel">

    <div class="panel-header">
      <h3 class="panel-title">{{ t("label.error_rate_trend") }}</h3>
      <div class="header-meta">
        <span class="meta-chip">{{ t("label.avg") }} <strong>{{ avgRate }}%</strong></span>
        <span class="meta-chip">{{ t("label.maximum") }} <strong class="max">{{ maxRate }}%</strong></span>
        <span class="threshold-chip">{{ t("label.std") }} {{ trend.threshold }}%</span>
      </div>
    </div>

    <div ref="chartRef" class="flex-1 min-h-0 h-full w-full" />

  </div>
</template>

<script setup lang="ts">
  /**
   * NOTE
   * - 데이터 변경 시 차트 재 렌더링
   */
  import { ref, computed, nextTick, onMounted, onUnmounted, Ref, watch } from "vue"
  import * as echarts from "echarts"
  import { useI18n } from "@/hooks/web/useI18n";
  import { useECharts } from '/@/hooks/web/useECharts';
  import { ErrorRateTrend } from "@/views/lms/monitoring-dashboard/types";

  const props = defineProps<{
    trend: ErrorRateTrend
  }>()

  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions, resize } = useECharts(chartRef as Ref<HTMLDivElement>);

  const { t } = useI18n()

  const avgRate = computed(() => {
    const avg = props.trend.rates.reduce((s, v) => s + v, 0) / (props.trend.rates.length || 1)
    return avg.toFixed(1)
  })

  const maxRate = computed(() => Math.max(...props.trend.rates).toFixed(1))

  /**
   * echart 적용 메타데이터
   */
  function setOptionsData(newData: any) {
    setOptions({
      grid: { top: 24, right: 16, bottom: 36, left: 52 },
      tooltip: {
        trigger: "axis",
        backgroundColor: "#0d2137",
        borderColor: "#1e3a5f",
        textStyle: { color: "#e2e8f0", fontSize: 14 },
        formatter: (params: any) =>
          `${params[0].axisValue}시<br/>에러율: <strong>${params[0].value}%</strong>`,
      },
      xAxis: {
        type: "category",
        data: props.trend.hours.map(h => `${h}시`),
        boundaryGap: false,
        axisLine:  { lineStyle: { color: "#1e3a5f" } },
        axisLabel: { color: "#94a3b8", fontSize: 13 },
        axisTick:  { show: false },
      },
      yAxis: {
        type: "value",
        splitLine: { lineStyle: { color: "#1e3a5f", type: "dashed" } },
        axisLabel: { color: "#94a3b8", fontSize: 13, formatter: "{value}%" },
      },
      series: [{
        type: "line",
        data: props.trend.rates,
        smooth: true,
        symbol: "circle",
        symbolSize: 5,
        lineStyle: { color: "#ef4444", width: 2 },
        itemStyle: {
          color: (params: any) =>
            params.value > props.trend.threshold ? "#ef4444" : "#f59e0b",
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(239,68,68,0.25)" },
            { offset: 1, color: "rgba(239,68,68,0.02)" },
          ]),
        },
        markLine: {
          silent: true,
          symbol: "none",
          lineStyle: { color: "#f59e0b", type: "dashed", width: 1.5 },
          label: {
            position: "end",
            color: "#f59e0b",
            fontSize: 11,
            formatter: `기준 ${props.trend.threshold}%`,
          },
          data: [{ yAxis: props.trend.threshold }],
        },
      }],
    })
  }

  function redrawChart() {
    if (!chartRef) return;

    resize();
    setOptionsData([])
  }

  onMounted(async () => {
    await nextTick();
    redrawChart();
  })

  watch(() => props.trend, () => {
    redrawChart();
  }, { deep: true })

  defineExpose({
    redrawChart
  })

</script>

<style scoped>

  .panel {
    display: flex;
    flex-direction: column;
    background: linear-gradient(135deg, #0d2137, #132f4c);
    border: 1px solid #1e3a5f;
    border-radius: 12px;
    padding: 1.25rem;
    height: 100%;
    box-sizing: border-box;
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.75rem;
    flex-shrink: 0;
    flex-wrap: wrap;
    gap: 0.5rem;
  }

  .panel-title {
    margin: 0;
    font-size: var(--fs-base);
    font-weight: 600;
    color: #e2e8f0;
  }

  .header-meta {
    display: flex;
    gap: 0.5rem;
    align-items: center;
  }

  .meta-chip {
    font-size: var(--fs-xs);
    color: #94a3b8;
    background: rgba(255,255,255,0.04);
    border: 1px solid #1e3a5f;
    border-radius: 999px;
    padding: 2px 10px;
  }

  .meta-chip strong      { color: #e2e8f0; font-weight: 600; }
  .meta-chip strong.max  { color: #ef4444; }

  .threshold-chip {
    font-size: var(--fs-xs);
    color: #f59e0b;
    background: rgba(245,158,11,0.1);
    border: 1px solid rgba(245,158,11,0.3);
    border-radius: 999px;
    padding: 2px 10px;
  }

</style>
