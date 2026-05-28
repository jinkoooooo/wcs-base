<template>
  <div class="panel flex flex-col rounded-xl p-5 h-full box-border">

    <div class="flex justify-between items-center mb-2 shrink-0">
      <h3 class="panel-title m-0">{{ t("label.equipment_status") }}</h3>
      <span class="total-badge rounded-full px-[10px] py-[2px]">총 {{ total }}대</span>
    </div>

    <div ref="chartRef" class="w-full min-h-0 h-full" />

    <div class="legend grid grid-cols-2 gap-x-3 gap-y-[0.4rem] shrink-0 pt-2 ">
      <div v-for="item in legendItems" :key="item.name"
           class="legend-item flex items-center gap-[6px] ">
        <span class="w-2 h-2 rounded-full shrink-0" :style="{ background: item.color }"></span>
        <span class="legend-label flex-1">{{ item.name }}</span>
        <span class="legend-value">{{ item.value }}{{ t("label.equip_unit") }}</span>
        <span class="legend-pct">{{ total ? Math.round((item.value / total) * 100) : 0 }}%</span>
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
  import { EquipmentStatus } from "@/views/lms/monitoring-dashboard/types";

  const props = defineProps<{
    equipmentStatus: EquipmentStatus
  }>()

  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions, resize } = useECharts(chartRef as Ref<HTMLDivElement>);

  const { t } = useI18n()

  // run: green / idle: gray / stop: orange / error: red
  const COLORS = { run: "#22c55e", idle: "#94a3b8", stop: "#f59e0b", error: "#ef4444" }

  const total = computed(() => props.equipmentStatus.run + props.equipmentStatus.stop + props.equipmentStatus.error + props.equipmentStatus.idle)

  // 범례: 가동, 유휴, 정지, 에러
  const legendItems = computed(() => [
    { name: t("label.equip_running"), value: props.equipmentStatus.run, color: COLORS.run },
    { name: t("label.idle"), value: props.equipmentStatus.idle, color: COLORS.idle },
    { name: t("label.stopped"), value: props.equipmentStatus.stop, color: COLORS.stop },
    { name: t("label.equip_error"), value: props.equipmentStatus.error, color: COLORS.error },
  ])

  /**
   * echart 적용 메타데이터
   */
  function setOptionsData(newData: any) {
    setOptions({
      tooltip: {
        trigger: "item",
        backgroundColor: "#0d2137", // black 계열
        borderColor: "#1e3a5f", // navy 계열
        textStyle: { color: "#e2e8f0", fontSize: 14 }, // light gray 계열
        formatter: `{b}: {c}${ t("label.equip_unit") } ({d}%)`,
      },
      graphic: [{
        type: "group",
        left: "center",
        top: "center",
        children: [
          {
            type: "text",
            left: "center",
            top: -14,
            style: { text: t("label.total"), fill: "#94a3b8", fontSize: 14, align: "center" }, // gray 계열
          },
          {
            type: "text",
            left: "center",
            top: 6,
            style: {
              text: `${ total.value }${ t("label.equip_unit") }`,
              fill: "#e2e8f0", // light gray 계열
              fontSize: 24,
              fontWeight: "bold",
              align: "center",
            },
          },
        ],
      }],
      series: [{
        type: "pie",
        radius: ["52%", "76%"],
        center: ["50%", "48%"],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: {
          scale: true,
          scaleSize: 5,
          label: { show: false },
        },
        itemStyle: { borderRadius: 4, borderWidth: 2, borderColor: "#0a1929" }, // black 계열
        data: [
          {
            value: props.equipmentStatus.run,
            name: t("label.equip_running"),
            itemStyle: { color: COLORS.run }
          },
          { value: props.equipmentStatus.idle, name: "유휴", itemStyle: { color: COLORS.idle } },
          { value: props.equipmentStatus.stop, name: "정지", itemStyle: { color: COLORS.stop } },
          { value: props.equipmentStatus.error, name: "에러", itemStyle: { color: COLORS.error } },
        ],
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

  watch(() => props.equipmentStatus, () => {
    redrawChart();
  }, { deep: true })

  defineExpose({
    redrawChart
  })
</script>

<style scoped>

  .panel {
    background: linear-gradient(135deg, #0d2137, #132f4c); /* black 계열 */
    border: 1px solid #1e3a5f; /* navy 계열 */
  }

  .panel-title {
    font-size: var(--fs-base);
    font-weight: 600;
    color: #e2e8f0; /* ligth gray 계열 */
  }

  .total-badge {
    font-size: var(--fs-sm);
    color: #94a3b8; /* blue gray 계열 */
    background: rgba(255, 255, 255, 0.04); /* white 계열 */
    border: 1px solid #1e3a5f; /* navy 계열 */
  }

  .legend {
    border-top: 1px solid #1e3a5f; /* navy 계열 */
  }

  .legend-item {
    font-size: var(--fs-sm);
  }

  .legend-label {
    color: #94a3b8; /* gray 계열 */
  }

  .legend-value {
    color: #e2e8f0; /* light gray 계열 */
    font-weight: 600;
  }

  .legend-pct {
    color: #8899b4; /* gray 계열 */
  }

</style>
