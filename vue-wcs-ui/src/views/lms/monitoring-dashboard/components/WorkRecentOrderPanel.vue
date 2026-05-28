<template>
  <div class="panel">

    <div class="panel-header">
      <h3 class="panel-title">{{ t("label.job_history") }}</h3>
      <span class="count-badge">{{ workOrders.length }}{{ t("label.orders") }}</span>
    </div>

    <div class="wo-list">
      <div v-if="!workOrders.length">
        <p class="empty-text">{{ t("text.No Data") }}</p>
      </div>

      <div v-else
        v-for="(wo, idx) in workOrders"
        :key="wo.orderNumber"
        class="wo-item"
      >
        <div class="wo-rank">{{ idx + 1 }}</div>
        <div class="wo-body">
          <div class="wo-number">{{ wo.orderNumber }}</div>
          <div class="wo-meta">
            <span class="wo-qty">{{ wo.quantity.toLocaleString() }}{{ t("label.box") }}</span>
            <span class="wo-sep">·</span>
            <span class="wo-time">{{ wo.completedAt }} {{ t("label.completed") }}</span>
            <span class="wo-sep">·</span>
            <span class="wo-duration">{{ wo.duration }}</span>
          </div>
        </div>
        <div class="wo-check">
          <CheckCircleFilled />
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
  import { CheckCircleFilled } from "@ant-design/icons-vue"
  import { useI18n } from "@/hooks/web/useI18n";
  import { RecentOrder } from "@/views/lms/monitoring-dashboard/types";

  defineProps<{
    workOrders: RecentOrder[]
  }>()

  const { t } = useI18n()

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
  }

  .panel-title {
    margin: 0;
    font-size: var(--fs-base);
    font-weight: 600;
    color: #e2e8f0;
  }

  .count-badge {
    font-size: var(--fs-xs);
    color: #94a3b8;
    background: rgba(255,255,255,0.04);
    border: 1px solid #1e3a5f;
    border-radius: 999px;
    padding: 2px 10px;
  }

  .wo-list {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .wo-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.6rem 0.75rem;
    background: rgba(255,255,255,0.03);
    border: 1px solid rgba(30,58,95,0.5);
    border-radius: 8px;
    transition: background 0.15s;
  }

  .wo-item:hover {
    background: rgba(255,255,255,0.06);
  }

  .wo-rank {
    font-size: var(--fs-xs);
    font-weight: 700;
    color: #8899b4;
    width: 18px;
    text-align: center;
    flex-shrink: 0;
  }

  .wo-body {
    flex: 1;
    min-width: 0;
  }

  .wo-number {
    font-size: var(--fs-sm);
    font-weight: 600;
    color: #cbd5e1;
    margin-bottom: 3px;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .wo-meta {
    display: flex;
    align-items: center;
    gap: 5px;
    flex-wrap: wrap;
  }

  .wo-qty      { font-size: var(--fs-xs); color: #38bdf8; font-weight: 600; }
  .wo-time     { font-size: var(--fs-xs); color: #94a3b8; font-variant-numeric: tabular-nums; }
  .wo-duration { font-size: var(--fs-xs); color: #8899b4; }
  .wo-sep      { font-size: var(--fs-xs); color: #475569; }

  .wo-check {
    color: #22c55e;
    font-size: var(--fs-base);
    flex-shrink: 0;
  }

  .empty-text {
    text-align: center;
    padding: 20px;
    color: #606266;
    font-size: 12px;
  }

</style>
