<template>
  <div class="panel">
    <div class="panel-header">
      <h3 class="panel-title">{{ t("label.hourly_throughput") }}</h3>

      <div class="flex flex-wrap gap-2">
        <span class="chip rounded-full px-[10px] py-[2px]">{{
            t("label.today")
          }} <strong>{{ daily.toLocaleString() }}</strong>{{ t("label.box") }}</span>
        <span class="chip rounded-full px-[10px] py-[2px]">{{
            t("label.this_week")
          }} <strong>{{ weekly.toLocaleString() }}</strong>{{ t("label.box") }}</span>
        <span class="chip rounded-full px-[10px] py-[2px]">{{
            t("label.this_month")
          }} <strong>{{ monthly.toLocaleString() }}</strong>{{ t("label.box") }}</span>
      </div>
    </div>

    <div class="relative flex-1 min-h-0">
      <div v-show="hasData" ref="chartRef" class="w-full h-full" />

      <div
        v-show="!hasData"
        class="no-data absolute inset-0 flex items-center justify-center"
      >
        {{ t("text.No Data") }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  /**
   * NOTE
   * - 데이터 변경 시 차트 재 렌더링
   */
  import { computed, nextTick, onMounted, Ref, ref, watch } from "vue"
  import * as echarts from "echarts"
  import { useI18n } from "@/hooks/web/useI18n";
  import { useECharts } from '/@/hooks/web/useECharts';
  import { Throughput } from "@/views/lms/monitoring-dashboard/types";

  const props = defineProps<{
    throughput: Throughput
  }>()

  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions, resize } = useECharts(chartRef as Ref<HTMLDivElement>);

  const { t } = useI18n()

  const hours = computed(() => props.throughput?.hours ?? [])
  const hourly = computed(() => props.throughput?.hourly ?? [])
  const daily = computed(() => props.throughput?.daily ?? 0)
  const weekly = computed(() => props.throughput?.weekly ?? 0)
  const monthly = computed(() => props.throughput?.monthly ?? 0)

  const hasData = computed(() => {return props.throughput?.hourly?.length > 0})

  /**
   * echart 적용 메타데이터
   */
  function setOptionsData(newData: any) {
    setOptions({
      grid: { top: 16, right: 16, bottom: 36, left: 52 },
      tooltip: {
        trigger: "axis",
        backgroundColor: "#0d2137", /* black 계열 */
        borderColor: "#1e3a5f", /* navy 계열 */
        textStyle: { color: "#e2e8f0", fontSize: 14 }, /* light gray 계열 */
        formatter: (params: any) =>
          `${ params[0].axisValue }시<br/>처리량: <strong>${ params[0].value.toLocaleString() }</strong> 박스`,
      },
      xAxis: {
        type: "category",
        data: hours.value.map(h => `${ h }시`),
        axisLine: { lineStyle: { color: "#1e3a5f" } }, /* navy 계열 */
        axisLabel: { color: "#94a3b8", fontSize: 13 }, /* gray 계열 */
        axisTick: { show: false },
      },
      yAxis: {
        type: "value",
        splitLine: { lineStyle: { color: "#1e3a5f", type: "dashed" } }, /* navy 계열 */
        axisLabel: { color: "#94a3b8", fontSize: 13 }, /* gray 계열 */
      },
      series: [{
        type: "bar",
        data: hourly.value,
        barMaxWidth: 36,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "#38bdf8" }, /* blue 계열 */
            { offset: 1, color: "#0369a1" }, /* dark blue 계열 */
          ]),
          borderRadius: [4, 4, 0, 0],
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: "#7dd3fc" }, /* light blue 계열 */
              { offset: 1, color: "#38bdf8" }, /* blue 계열 */
            ]),
          },
        },
        label: {
          show: false,
        },
      }],
    })
  }

  function redrawChart() {
    if (!chartRef) return;

    resize()
    setOptionsData([])
  }

  onMounted(async () => {
    await nextTick();
    redrawChart();
  })

  watch(() => props.throughput, () => {
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
    margin-bottom: 1rem;
    flex-shrink: 0;
    flex-wrap: wrap;
    gap: 0.5rem;
  }

  .panel-title {
    margin: 0;
    font-size: var(--fs-base);
    font-weight: 600;
    color: #e2e8f0;
    letter-spacing: -0.01em;
  }

  .chip {
    font-size: var(--fs-sm);
    color: #94a3b8; /* gray 계열 */
    background: rgba(255, 255, 255, 0.04); /* white 계열 */
    border: 1px solid #1e3a5f; /* navy 계열 */
  }

  .chip strong {
    color: #38bdf8; /* light blue 계열 */
    font-weight: 600;
  }

  .no-data {
    color: #8899b4; /* dark gray 계열 */
    font-size: var(--fs-base);
  }

</style>
