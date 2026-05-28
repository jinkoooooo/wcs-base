<template>
  <div class="panel">

    <div class="panel-header">
      <h3 class="panel-title">{{ t("label.maintenance_status") }}</h3>
      <span class="total-badge">{{ t("label.total") }} {{ total }}{{ t("label.orders") }}</span>
    </div>

    <div class="stats">
      <div v-for="item in stats" :key="item.label" class="stat-item">
        <div class="stat-header">
          <span class="stat-label">{{ item.label }}</span>
          <span class="stat-value" :style="{ color: item.color }">{{
              item.value
            }}{{ t("label.orders") }}</span>
        </div>
        <div class="stat-bar-bg">
          <div
            class="stat-bar"
            :style="{ width: item.ratio + '%', background: item.color }"
          ></div>
        </div>
      </div>
    </div>

    <div class="ring-wrapper">
      <div ref="chartRef" class="w-full min-h-0 h-full" />
    </div>

  </div>
</template>

<script setup lang="ts">
  /**
   * NOTE
   * - 데이터 변경 시 차트 재 렌더링
   */
  import { computed, nextTick, onMounted, Ref, ref, watch } from "vue"
  import { useI18n } from "@/hooks/web/useI18n";
  import { Maintenance } from "@/views/lms/monitoring-dashboard/types";
  import { useECharts } from "@/hooks/web/useECharts";

  const props = defineProps<{
    maintenance: Maintenance
  }>()

  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions, resize } = useECharts(chartRef as Ref<HTMLDivElement>);

  const { t } = useI18n()

  // 총 유지보수 건수
  const total = computed(() => props.maintenance.inProgress + props.maintenance.completed + props.maintenance.deleted)
  const activeTotal = computed(() => props.maintenance.inProgress + props.maintenance.completed)
  // 유지보수 완료율
  const completionRate = computed(() => Math.round((props.maintenance.completed / (activeTotal.value || 1)) * 100))

  const stats = computed(() => {
    const max = Math.max(
      props.maintenance.inProgress,
      props.maintenance.completed,
      props.maintenance.deleted,
    ) || 1
    return [
      {
        label: t("label.progress"),
        value: props.maintenance.inProgress,
        ratio: (props.maintenance.inProgress / max) * 100,
        color: "#f59e0b" // orange
      },
      {
        label: t("label.completed"),
        value: props.maintenance.completed,
        ratio: (props.maintenance.completed / max) * 100,
        color: "#22c55e" // green
      },
      {
        label: t("label.delete"),
        value: props.maintenance.deleted,
        ratio: (props.maintenance.deleted / max) * 100,
        color: "#ef4444" // red
      },
    ]
  })

  /**
   * echart 적용 메타데이터
   */
  function setOptionsData(newData: any) {
    setOptions({
        graphic: [{
          type: "group",
          left: "center",
          top: "center",
          children: [
            {
              type: "text",
              left: "center",
              top: -14,
              style: {
                text: t("label.completion_rate"),
                fill: "#94a3b8", // gray
                fontSize: 13,
                textAlign: "center"
              },
            },
            {
              type: "text",
              left: "center",
              top: 6,
              style: {
                text: `${ completionRate.value }%`,
                fill: "#22c55e", // green
                fontSize: 26,
                fontWeight: "bold",
                textAlign: "center",
              },
            },
          ],
        }],
        series: [{
          type: "pie",
          radius: ["68%", "85%"],
          startAngle: 90,
          label: { show: false },
          emphasis: { scale: false },
          itemStyle: { borderWidth: 0 },
          data: [
            { value: completionRate.value, itemStyle: { color: "#22c55e" } }, // green
            { value: 100 - completionRate.value, itemStyle: { color: "rgba(255,255,255,0.05)" } }, // white
          ],
        }],
      }
    )
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

  watch(() => props.maintenance, () => {
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
    gap: 1rem;
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
  }

  .panel-title {
    margin: 0;
    font-size: var(--fs-base);
    font-weight: 600;
    color: #e2e8f0;
  }

  .total-badge {
    font-size: var(--fs-sm);
    color: #94a3b8;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid #1e3a5f;
    border-radius: 999px;
    padding: 2px 10px;
  }

  .stats {
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 0.8rem;
  }

  .stat-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 5px;
  }

  .stat-label {
    font-size: var(--fs-sm);
    color: #94a3b8;
  }

  .stat-value {
    font-size: var(--fs-base);
    font-weight: 700;
  }

  .stat-bar-bg {
    height: 6px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 999px;
    overflow: hidden;
  }

  .stat-bar {
    height: 100%;
    border-radius: 999px;
    transition: width 0.8s ease;
  }

  .ring-wrapper {
    flex: 1;
    min-height: 0;
  }


</style>
