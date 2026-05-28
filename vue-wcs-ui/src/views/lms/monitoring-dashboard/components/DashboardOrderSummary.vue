<template>
  <div class="grid grid-cols-[350px_1fr] gap-4">

    <!-- 작업 번호 -->
    <div class="wo-badge flex flex-col justify-center rounded-xl px-6 py-4">
      <div class="wo-label mb-[5px]">{{ t("label.order_id") }}</div>
      <div class="wo-number">{{ kpiData.orderNumber }}</div>
    </div>

    <!-- 일 입고량, 일 출고량, 시간당 처리율, 설비 가동률, 출고지시 처리율, 평균 처리 시간 -->
    <div class="kpi-box flex">
      <div
        v-for="card in cards"
        :key="card.key"
        class="kpi-item flex flex-1 items-center gap-4 px-5 py-4"
      >
        <div class="kpi-icon w-11 h-11 rounded-[10px] flex items-center justify-center shrink-0"
             :style="{ color: card.color, background: card.bg }">
          <component :is="card.icon" />
        </div>
        <div class="flex-1 min-w-0">
          <div class="kpi-value mb-[4px]">
            {{ card.value }}
            <span class="kpi-unit ml-[3px]">{{ card.unit }}</span>
          </div>
          <div class="kpi-label mb-[3px]">{{ card.label }}</div>
          <div class="kpi-change" :class="card.up ? 'up' : 'down'">
            {{ card.up ? "▲" : "▼" }} {{ card.change }}%
            <span class="kpi-vs ml-[3px]">{{ t("label.day_over_day") }}</span>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">

  import { computed } from "vue"
  import { useI18n } from "@/hooks/web/useI18n";
  import { KpiData } from "@/views/lms/monitoring-dashboard/types"
  import {
    ThunderboltOutlined,
    SettingOutlined,
    FileTextOutlined,
    FieldTimeOutlined,
  } from "@ant-design/icons-vue"

  const props = defineProps<{ kpiData: KpiData }>()
  const { t } = useI18n()

  const cards = computed(() => [
    {
      key: "throughput",
      label: t("label.throughput"),
      icon: ThunderboltOutlined,
      color: "#a78bfa", // purple 계열
      bg: "rgba(167,139,250,0.1)", // white 계열
      ...props.kpiData.throughput,
    },
    {
      key: "capacityUtilitzation",
      label: t("label.capacity_utilization"),
      icon: SettingOutlined,
      color: "#fb923c", // orange 계열
      bg: "rgba(251,146,60,0.1)", // white 계열
      ...props.kpiData.capacityUtilitzation,
    },
    {
      key: "fullFillmentRate",
      label: t("label.outbound_fulfillment_rate"),
      icon: FileTextOutlined,
      color: "#2dd4bf", // mint 계열
      bg: "rgba(45,212,191,0.1)", // white 계열
      ...props.kpiData.fullFillmentRate,
    },
    {
      key: "avgProcessTime",
      label: t("label.avg_turnaround_time"),
      icon: FieldTimeOutlined,
      color: "#e879f9", // pink 계열
      bg: "rgba(232,121,249,0.1)", // white 계열
      ...props.kpiData.avgProcessTime,
    },
  ])

</script>

<style scoped>

  .wo-badge {
    background: linear-gradient(135deg, #0d2137, #132f4c);
    border: 1px solid #1e3a5f;
  }

  .wo-label {
    font-size: var(--fs-xs);
    color: #8899b4;
    text-transform: uppercase;
    letter-spacing: 0.1em;
  }

  .wo-number {
    font-size: var(--fs-base);
    font-weight: 700;
    color: #38bdf8;
    font-variant-numeric: tabular-nums;
    word-break: break-all;
  }

  .kpi-box {
    background: linear-gradient(135deg, #0d2137, #132f4c);
    border: 1px solid #1e3a5f;
    border-radius: 12px;
    overflow: hidden;
  }

  .kpi-item + .kpi-item {
    border-left: 1px solid #1e3a5f;
  }

  .kpi-icon {
    font-size: var(--fs-lg);
  }

  .kpi-value {
    font-size: var(--fs-lg);
    font-weight: 700;
    color: #e2e8f0;
    line-height: 1;
  }

  .kpi-unit {
    font-size: var(--fs-sm);
    color: #8899b4;
    font-weight: 400;
  }

  .kpi-label {
    font-size: var(--fs-sm);
    color: #94a3b8;
  }

  .kpi-change {
    font-size: var(--fs-xs);
    font-weight: 600;
  }

  .kpi-change.up {
    color: #22c55e;
  }

  .kpi-change.down {
    color: #ef4444;
  }

  .kpi-vs {
    color: #8899b4;
    font-weight: 400;
  }

</style>
