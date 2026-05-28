<template>
  <div class="panel">

    <div class="panel-header">
      <h3 class="panel-title">{{ t("label.actual_vs_target") }}</h3>
    </div>

    <div ref="chartRef" class="flex-1 min-h-0 w-full" />

    <div class="bottom-info">
      <div class="info-item">
        <span class="info-label">{{ t("label.target") }}</span>
        <span class="info-value">{{ target.toLocaleString() }}<span
          class="info-unit">{{ t("label.box") }}</span></span>
      </div>
      <div class="divider"></div>
      <div class="info-item">
        <span class="info-label">{{ t("label.actual") }}</span>
        <span class="info-value actual">{{ actual.toLocaleString() }}<span
          class="info-unit">{{ t("label.box") }}</span></span>
      </div>
      <div class="divider"></div>
      <div class="info-item">
        <span class="info-label">{{ t("label.remaining") }}</span>
        <span class="info-value remain">{{ (target - actual).toLocaleString() }}<span
          class="info-unit">{{ t("label.box") }}</span></span>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
  /**
   * NOTE
   * - 데이터 변경 시 차트 재 렌더링
   */
  import { computed, nextTick, onMounted, ref, Ref, watch } from "vue"
  import { useI18n } from "@/hooks/web/useI18n";
  import { useECharts } from '/@/hooks/web/useECharts';
  import { TargetVsActual } from "@/views/lms/monitoring-dashboard/types";

  const props = defineProps<{
    targetVsActual: TargetVsActual
  }>()

  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions, resize } = useECharts(chartRef as Ref<HTMLDivElement>);

  const { t } = useI18n()

  const target = computed(() => props.targetVsActual?.target ?? 0)
  const actual = computed(() => props.targetVsActual?.actual ?? 0)

  const achievementRate = computed(() => {
    if (target.value == null || target.value === 0) return 0
    return Math.min(Math.round((actual.value / target.value) * 100), 100)
  })

  // 80%이상: green / 50%이상: orange / 50%미만: red
  const gaugeColor = computed(() => {
    if (achievementRate.value >= 80) return "#22c55e"
    if (achievementRate.value >= 50) return "#f59e0b"
    return "#ef4444"
  })

  /**
   * echart 적용 메타데이터
   */
  function setOptionsData(newData: any) {
    setOptions({
      series: [{
        type: "gauge",
        startAngle: 210,
        endAngle: -30,
        min: 0,
        max: 100,
        splitNumber: 5,
        radius: "88%",
        center: ["50%", "58%"],
        progress: {
          show: true,
          width: 14,
          itemStyle: { color: gaugeColor.value },
        },
        pointer: { show: false },
        axisLine: {
          lineStyle: { width: 14, color: [[1, "rgba(255,255,255,0.06)"]] }, /* white */
        },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        anchor: { show: false },
        title: { show: false },
        detail: {
          valueAnimation: true,
          offsetCenter: [0, "-10%"],
          fontSize: 26,
          fontWeight: "bold",
          color: "#e2e8f0", /* light gray */
          formatter: "{value}%",
        },
        data: [{ value: achievementRate.value }],
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

  watch(() => props.targetVsActual, () => {
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
    flex-shrink: 0;
    margin-bottom: 0.25rem;
  }

  .panel-title {
    margin: 0;
    font-size: var(--fs-base);
    font-weight: 600;
    color: #e2e8f0;
  }

  .bottom-info {
    display: flex;
    justify-content: space-around;
    align-items: center;
    flex-shrink: 0;
    padding-top: 0.5rem;
    border-top: 1px solid #1e3a5f; /* navy */
  }

  .info-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
  }

  .info-label {
    font-size: var(--fs-xs);
    color: #8899b4; /* gray */
  }

  .info-value {
    font-size: var(--fs-base);
    font-weight: 700;
    color: #e2e8f0; /* light gray */
    font-variant-numeric: tabular-nums;
  }

  /* green */
  .info-value.actual {
    color: #22c55e;
  }

  /* gray */
  .info-value.remain {
    color: #94a3b8;
  }

  .info-unit {
    font-size: var(--fs-xs);
    color: #8899b4; /* gray */
    font-weight: 400;
    margin-left: 2px;
  }

  .divider {
    width: 1px;
    height: 28px;
    background: #1e3a5f; /* navy */
  }

</style>
