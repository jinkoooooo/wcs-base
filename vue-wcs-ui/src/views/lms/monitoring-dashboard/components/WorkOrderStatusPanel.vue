<template>
  <div class="panel">

    <div class="panel-header">
      <h3 class="panel-title">{{ title }}</h3>
      <span class="fulfillment-rate" :class="rateClass">{{
          fulfillmentRate
        }}% {{ t("label.fulfillment") }}</span>
    </div>

    <div class="total-bar-wrap">
      <div class="total-bar-bg">
        <div class="total-bar"
             :style="{ width: fulfillmentRate + '%', background: barGradient }"></div>
      </div>
      <span class="total-label">{{ t("label.all") }} {{
          fulfillment.total.toLocaleString()
        }}{{ t("label.orders") }}</span>
    </div>

    <div class="status-grid">

      <div class="status-item" v-for="item in statusItems" :key="item.key">
        <div class="status-header">
          <span class="status-dot" :style="{ background: item.color }"></span>
          <span class="status-label">{{ item.label }}</span>
          <span class="status-count" :style="{ color: item.color }">{{ item.value }}{{ t("label.orders") }}</span>
          <span class="status-pct">{{ Math.round((item.value / (fulfillment.total || 1)) * 100) }}%</span>
        </div>
        <div class="status-bar-bg">
          <div
            class="status-bar"
            :style="{ width: (item.value / (fulfillment.total || 1) * 100) + '%', background: item.color }"
          ></div>
        </div>
      </div>

    </div>

  </div>
</template>

<script setup lang="ts">
  import { computed } from "vue"
  import { useI18n } from "@/hooks/web/useI18n";
  import { FulfillmentData } from "@/views/lms/monitoring-dashboard/types";

  const props = defineProps<{
    title: string
    color?: string
    fulfillment: FulfillmentData
  }>()

  const { t } = useI18n()

  const barGradient = computed(() =>
    props.color
      ? `linear-gradient(90deg, ${ props.color }, #22c55e)`
      : "linear-gradient(90deg, #22c55e, #38bdf8)"
  )

  const fulfillmentRate = computed(() =>
    Math.round((props.fulfillment.completed / (props.fulfillment.total || 1)) * 100)
  )

  const rateClass = computed(() => {
    if (fulfillmentRate.value >= 80) return "rate-good"
    if (fulfillmentRate.value >= 50) return "rate-warn"
    return "rate-bad"
  })

  // completed: green / inProgress: blue / pedning: gray
  const statusItems = computed(() => [
    {
      key: "completed",
      label: t("label.completed"),
      value: props.fulfillment.completed,
      color: "#22c55e"
    },
    {
      key: "inProgress",
      label: t("label.in_progress"),
      value: props.fulfillment.inProgress,
      color: "#38bdf8"
    },
    {
      key: "pending",
      label: t("label.pending"),
      value: props.fulfillment.pending,
      color: "#94a3b8"
    },
  ])

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
    gap: 0.9rem;
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

  .fulfillment-rate {
    font-size: var(--fs-sm);
    font-weight: 700;
    padding: 3px 12px;
    border-radius: 999px;
  }

  /* green */
  .rate-good {
    background: rgba(34, 197, 94, 0.12);
    color: #22c55e;
    border: 1px solid rgba(34, 197, 94, 0.3);
  }

  /* orange */
  .rate-warn {
    background: rgba(245, 158, 11, 0.12);
    color: #f59e0b;
    border: 1px solid rgba(245, 158, 11, 0.3);
  }

  /* red */
  .rate-bad {
    background: rgba(239, 68, 68, 0.12);
    color: #ef4444;
    border: 1px solid rgba(239, 68, 68, 0.3);
  }

  .total-bar-wrap {
    flex-shrink: 0;
  }

  .total-bar-bg {
    height: 8px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 999px;
    overflow: hidden;
    margin-bottom: 5px;
  }

  .total-bar {
    height: 100%;
    background: linear-gradient(90deg, #22c55e, #38bdf8);
    border-radius: 999px;
    transition: width 0.8s ease;
  }

  .total-label {
    font-size: var(--fs-xs);
    color: #94a3b8;
  }

  .status-grid {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }

  .status-item {
  }

  .status-header {
    display: flex;
    align-items: center;
    gap: 7px;
    margin-bottom: 5px;
  }

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .status-label {
    font-size: var(--fs-sm);
    color: #94a3b8;
    flex: 1;
  }

  .status-count {
    font-size: var(--fs-sm);
    font-weight: 700;
  }

  .status-pct {
    font-size: var(--fs-xs);
    color: #8899b4;
    min-width: 36px;
    text-align: right;
  }

  .status-bar-bg {
    height: 6px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 999px;
    overflow: hidden;
  }

  .status-bar {
    height: 100%;
    border-radius: 999px;
    transition: width 0.8s ease;
  }

</style>
