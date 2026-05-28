<template>
  <div class="panel flex flex-col h-full rounded-xl p-5 box-border">

    <div class="flex justify-between items-center mb-4 flex-wrap gap-2 shrink-0">
      <h3 class="panel-title m-0">{{ t("label.real_time_alarm") }}</h3>
      <div class="flex gap-2 flex-wrap">
        <span v-if="clearedErrorCnt" class="count-badge cleared">
          CLEARED ERROR {{ clearedErrorCnt }}
        </span>
        <span v-if="errorCnt" class="count-badge error">
          ERROR {{ errorCnt }}
        </span>
        <span v-if="warnCnt" class="count-badge warn">
          WARNING {{ warnCnt }}
        </span>
        <span v-if="infoCount" class="count-badge info">
          INFO {{ infoCount }}
        </span>
        <span v-if="!alarms.length" class="count-badge ok">{{ t("label.normal") }}</span>
      </div>
    </div>

    <div class="flex-1 min-h-0 overflow-y-auto overflow-x-auto">
      <table class="alarm-table w-full border-collapse">
        <thead>
        <tr>
          <th>{{ t("label.level") }}</th>
          <th>{{ t("label.equipment") }}</th>
          <th>{{ t("label.message") }}</th>
          <th>{{ t("label.occur_time") }}</th>
        </tr>
        </thead>
        <tbody>
        <tr v-if="!alarms.length">
          <td colspan="4" class="no-alarms">{{ t("text.No Data") }}</td>
        </tr>
        <tr
          v-for="alarm in alarms"
          :key="alarm.occurredAt + alarm.equipId"
          :class="`row-${alarm.level.toLowerCase()}`"
        >
          <td>
              <span class="level-badge px-2 py-[2px] rounded inline-block"
                    :class="`badge-${alarm.level.toLowerCase()}`">
                {{ alarm.level === "clearedError" ? "cleared error" : alarm.level }}
              </span>
          </td>
          <td class="td-equipment ">{{ alarm.equipId }}</td>
          <td class="td-message">{{ alarm.alarmMsg }}</td>
          <td class="td-time">{{ alarm.occurredAt }}</td>
        </tr>
        </tbody>
      </table>
    </div>

  </div>
</template>

<script setup lang="ts">
  /**
   * NOTE
   * - 알람이 없을 때, badge: '정상', 테이블body: '알람 없음'
   * -
   */
  import { computed } from "vue"
  import { useI18n } from "@/hooks/web/useI18n";
  import { Alarm } from "@/views/lms/monitoring-dashboard/types"

  const props = defineProps<{
    alarms: Alarm[]
  }>()

  const { t } = useI18n()

  const errorCnt = computed(() => props.alarms.filter(a => a.level === "error").length)
  const clearedErrorCnt = computed(() => props.alarms.filter(a => a.level === "clearedError").length)
  const warnCnt = computed(() => props.alarms.filter(a => a.level === "warn").length)
  const infoCount = computed(() => props.alarms.filter(a => a.level === "info").length)
</script>

<style scoped>

  .panel {
    background: linear-gradient(135deg, #0d2137, #132f4c);
    border: 1px solid #1e3a5f;
  }

  .panel-title {
    font-size: var(--fs-base);
    font-weight: 600;
    color: #e2e8f0;
  }

  .count-badge {
    font-size: var(--fs-xs);
    font-weight: 600;
    padding: 2px 10px;
    border-radius: 999px;
  }

  /* gray 계열 */
  .count-badge.cleared {
    background: rgba(148, 163, 184, 0.12);
    color: #94a3b8;
    border: 1px solid rgba(148, 163, 184, 0.3);
  }

  /* red 계열 */
  .count-badge.error {
    background: rgba(239, 68, 68, 0.12);
    color: #ef4444;
    border: 1px solid rgba(239, 68, 68, 0.3);
  }

  /* orange 계열 */
  .count-badge.warn {
    background: rgba(245, 158, 11, 0.12);
    color: #f59e0b;
    border: 1px solid rgba(245, 158, 11, 0.3);
  }

  /* blue 계열 */
  .count-badge.info {
    background: rgba(56, 189, 248, 0.12);
    color: #38bdf8;
    border: 1px solid rgba(56, 189, 248, 0.3);
  }

  /* green 계열 */
  .count-badge.ok {
    background: rgba(34, 197, 94, 0.12);
    color: #22c55e;
    border: 1px solid rgba(34, 197, 94, 0.3);
  }

  .alarm-table th {
    font-size: var(--fs-xs);
    color: #8899b4;
    text-align: left;
    padding: 0.6rem 0.75rem 0.6rem;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    border-bottom: 1px solid #1e3a5f;
    position: sticky;
    top: 0;
    background: #0d2137;
  }

  .alarm-table td {
    padding: 0.65rem 0.75rem;
    font-size: var(--fs-base);
    color: #94a3b8;
    border-bottom: 1px solid rgba(30, 58, 95, 0.4);
  }

  .alarm-table tbody tr:last-child td {
    border-bottom: none;
  }

  .alarm-table tbody tr {
    transition: background 0.15s;
  }

  .alarm-table tbody tr:hover td {
    background: rgba(255, 255, 255, 0.03);
  }

  .row-error td {
    background: rgba(239, 68, 68, 0.04);
  }

  .row-warn td {
    background: rgba(245, 158, 11, 0.03);
  }

  .level-badge {
    font-size: var(--fs-xs);
    font-weight: 700;
    letter-spacing: 0.04em;
    white-space: nowrap;
  }

  /* 알람 레벨 별 badge 스타일 */

  /* gray 계열 */
  .badge-clearederror {
    background: rgba(148, 163, 184, 0.18);
    color: #94a3b8;
    border: 1px solid rgba(148, 163, 184, 0.4);
  }

  /* red 계열 */
  .badge-error {
    background: rgba(239, 68, 68, 0.18);
    color: #ef4444;
    border: 1px solid rgba(239, 68, 68, 0.4);
  }

  /* orange 계열 */
  .badge-warn {
    background: rgba(245, 158, 11, 0.18);
    color: #f59e0b;
    border: 1px solid rgba(245, 158, 11, 0.4);
  }

  /* blue 계열 */
  .badge-info {
    background: rgba(56, 189, 248, 0.18);
    color: #38bdf8;
    border: 1px solid rgba(56, 189, 248, 0.4);
  }

  .td-equipment {
    font-weight: 600;
    color: #cbd5e1; /* gray 계열 */
  }

  .td-message {
    color: #94a3b8; /* dark gray 계열 */
  }

  .td-time {
    color: #8899b4; /* dark gray 계열 */
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }

  .no-alarms {
    text-align: center;
    padding: 2rem !important;
    color: #8899b4; /* dark gray 계열 */
    font-size: var(--fs-base);
  }

</style>
