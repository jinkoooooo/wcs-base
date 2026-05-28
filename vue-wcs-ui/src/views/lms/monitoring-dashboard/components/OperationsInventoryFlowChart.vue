<template>
  <div class="panel">
    <div class="panel-header">
      <h3 class="panel-title">{{ t("label.in_outbound_flow") }}</h3>
      <div class="header-right">
        <div class="legend">
          <span class="legend-item">
            <span class="dot inbound-dot"></span>{{ t("label.inbound_qty") }}
          </span>
          <span class="legend-item">
            <span class="dot outbound-dot"></span>{{ t("label.outbound_qty") }}
          </span>
          <span class="summary">
            {{ t("label.net_change") }}:
            <strong :class="netDiff >= 0 ? 'pos' : 'neg'">
              {{ netDiff >= 0 ? "+" : "" }}{{ netDiff.toLocaleString() }}
            </strong>
          </span>
        </div>
        <div class="period-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="tab-btn"
            :class="{ active: selectedTab === tab.key }"
            @click="selectedTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>
    </div>

    <div ref="chartRef" class="w-full flex-1 min-h-0" />

  </div>
</template>

<script setup lang="ts">
  /**
   * NOTE
   * - 데이터 변경 시 차트 재 렌더링
   */
  import { computed, nextTick, onMounted, ref, Ref, watch } from "vue"
  import * as echarts from "echarts"
  import { useI18n } from "@/hooks/web/useI18n";
  import { useECharts } from '/@/hooks/web/useECharts';
  import { FlowPeriod } from "@/views/lms/monitoring-dashboard/types";

  type Period = "daily" | "weekly" | "monthly"

  const props = defineProps<{
    flow: {
      daily: FlowPeriod
      weekly: FlowPeriod
      monthly: FlowPeriod
    }
  }>()

  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions, resize } = useECharts(chartRef as Ref<HTMLDivElement>);

  const { t } = useI18n()

  const hasData = computed(() => {return props.flow != null})

  const tabs = [
    { key: "daily" as Period, label: t("label.daily") },
    { key: "weekly" as Period, label: t("label.weekly") },
    { key: "monthly" as Period, label: t("label.monthly") },
  ]

  const selectedTab = ref<Period>("daily")

  const currentData = computed((): FlowPeriod => {
    const data = props.flow?.[selectedTab.value]

    if (!data) {
      return { labels: [], inbound: [], outbound: [] }
    }

    return {
      labels: selectedTab.value === "daily" ? data.labels.map(l => `${ l }${ t("label.hour") }`) : data.labels,
      inbound: data.inbound,
      outbound: data.outbound,
    }
  })

  // 순증감
  const netDiff = computed(() => {
    const data = currentData.value
    return data.inbound.reduce((acc, curr) => acc + curr, 0) - data.outbound.reduce((acc, curr) => acc + curr, 0)
  })

  /**
   * echart 적용 메타데이터
   */
  function setOptionsData(newData: FlowPeriod) {
    setOptions({
      grid: { top: 16, right: 16, bottom: 36, left: 60 },
      tooltip: {
        trigger: "axis",
        backgroundColor: "#0d2137",
        borderColor: "#1e3a5f",
        textStyle: { color: "#e2e8f0", fontSize: 14 },
      },
      xAxis: {
        type: "category",
        data: newData.labels,
        boundaryGap: false,
        axisLine: { lineStyle: { color: "#1e3a5f" } },
        axisLabel: { color: "#94a3b8", fontSize: 13 },
        axisTick: { show: false },
      },
      yAxis: {
        type: "value",
        splitLine: { lineStyle: { color: "#1e3a5f", type: "dashed" } },
        axisLabel: { color: "#94a3b8", fontSize: 13 },
      },
      series: [
        {
          name: t("label.inbound_qty"),
          type: "line",
          data: newData.inbound,
          smooth: true,
          symbol: "circle",
          symbolSize: 5,
          lineStyle: { color: "#38bdf8", width: 2 },
          itemStyle: { color: "#38bdf8" },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: "rgba(56,189,248,0.28)" },
              { offset: 1, color: "rgba(56,189,248,0.02)" },
            ]),
          },
        },
        {
          name: t("label.outbound_qty"),
          type: "line",
          data: newData.outbound,
          smooth: true,
          symbol: "circle",
          symbolSize: 5,
          lineStyle: { color: "#34d399", width: 2 },
          itemStyle: { color: "#34d399" },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: "rgba(52,211,153,0.22)" },
              { offset: 1, color: "rgba(52,211,153,0.02)" },
            ]),
          },
        },
      ],
    })
  }

  function redrawChart(data?: any) {
    if (!chartRef) return;

    if (!data) {
      data = currentData.value;
    }

    resize()
    setOptionsData(data);
  }

  onMounted(async () => {
    await nextTick();
    redrawChart(currentData.value);
  })

  watch(currentData, (data) => { redrawChart(data); })

  watch(() => props.flow, () => {
    redrawChart(currentData.value);
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
    margin-bottom: 1rem;
    flex-shrink: 0;
    flex-wrap: wrap;
    gap: 0.75rem;
  }

  .panel-title {
    margin: 0;
    font-size: var(--fs-base);
    font-weight: 600;
    color: #e2e8f0;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 1rem;
    flex-wrap: wrap;
  }

  .legend {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .legend-item {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: var(--fs-sm);
    color: #94a3b8;
  }

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .inbound-dot {
    background: #38bdf8;
  }

  .outbound-dot {
    background: #34d399;
  }

  .summary {
    font-size: var(--fs-sm);
    color: #94a3b8;
  }

  .summary strong {
    font-weight: 700;
  }

  .pos {
    color: #22c55e;
  }

  .neg {
    color: #ef4444;
  }

  .period-tabs {
    display: flex;
    gap: 2px;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid #1e3a5f;
    border-radius: 8px;
    padding: 3px;
  }

  .tab-btn {
    font-size: var(--fs-xs);
    color: #94a3b8;
    background: transparent;
    border: none;
    border-radius: 6px;
    padding: 4px 14px;
    cursor: pointer;
    transition: background 0.15s, color 0.15s;
  }

  .tab-btn:hover:not(.active) {
    color: #cbd5e1;
    background: rgba(255, 255, 255, 0.05);
  }

  .tab-btn.active {
    background: #1e3a5f;
    color: #e2e8f0;
    font-weight: 600;
  }

</style>
