<template>
  <div class="panel">

    <div class="panel-header">
      <h3 class="panel-title">{{ t("label.live_equip_status") }}</h3>
      <div class="header-badges">
        <span v-if="errorCount" class="hbadge error">{{ t("label.error") }} {{ errorCount }}</span>
        <span v-if="stopCount" class="hbadge stop">{{ t("label.stopped") }} {{ stopCount }}</span>
        <span v-if="idleCount" class="hbadge idle">{{ t("label.idle") }} {{ idleCount }}</span>
        <span class="hbadge run">{{ t("label.equip_running") }} {{ runCount }}</span>
      </div>
    </div>

    <div class="flex gap-[0.3rem] flex-wrap mb-[0.75rem] shrink-0">
      <button
        v-for="group in GROUPS"
        :key="group.key"
        class="group-btn"
        :class="{ active: selectedGroup === group.key }"
        @click="selectedGroup = group.key"
      >
        {{ group.label }}
      </button>
    </div>

    <div class="table-wrap">
      <table class="equip-table">
        <thead>
        <tr>
          <th>{{ t("label.equip_nm") }}</th>
          <th>{{ t("label.status") }}</th>
          <th>{{ t("label.run_time") }}</th>
          <th>{{ t("label.last_error") }}</th>
        </tr>
        </thead>
        <tbody>
        <tr v-if="!sortedList.length">
          <td colspan="4" class="empty-text">{{ t("text.No Data") }}</td>
        </tr>
        <tr v-else
          v-for="equip in sortedList"
          :key="equip.id"
          :class="`row-${equip.status}`"
        >
          <td class="td-name">{{ equip.name }}</td>
          <td>
              <span class="status-badge" :class="`badge-${equip.status}`">
                {{ STATUS_LABEL[equip.status] }}
              </span>
          </td>
          <td class="td-uptime">{{ equip.uptime }}</td>
          <td class="td-error">{{ equip.lastError }}</td>
        </tr>
        </tbody>
      </table>
    </div>

  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from "vue"
  import { useI18n } from "@/hooks/web/useI18n";

  type EquipStatus = "run" | "idle" | "stop" | "error"
  type GroupKey = "all" | "agv" | "crane" | "sensor" | "etc"

  interface Equipment {
    id: string
    name: string
    status: EquipStatus
    uptime: string
    lastError: string
  }

  interface Group {
    key: GroupKey
    label: string
    prefixes: string[]
  }

  const props = defineProps<{ list: Equipment[] }>()

  const { t } = useI18n()

  const STATUS_ORDER: Record<EquipStatus, number> = { error: 0, stop: 1, idle: 2, run: 3 }
  const STATUS_LABEL: Record<EquipStatus, string> = {
    error: "ERROR",
    stop: "STOP",
    idle: "IDLE",
    run: "RUN"
  }

  // equip_id prefix 기반 그룹 정의
  // - ETC: AGV/CRANE/SENSOR에 속하지 않는 모든 설비
  const GROUPS: Group[] = [
    { key: "all",    label: "전체",              prefixes: [] },
    { key: "agv",    label: "AMR · AGF",  prefixes: ["AGV", "AGF", "RGV", "AMR"] },
    { key: "crane",  label: "CRANE · LIFT",       prefixes: ["SCC"] },
    { key: "sensor", label: "SENSOR",            prefixes: ["SENSOR"] },
    { key: "etc",    label: "ETC",               prefixes: [] },
  ]

  // 사용자가 선택한 설비그룹
  const selectedGroup = ref<GroupKey>("all")

  // 소속 설비그룹 조회
  function getGroupKey(equipId: string): GroupKey {
    const id = equipId.toUpperCase()
    for (const group of GROUPS) {
      if (group.key === "all" || group.key === "etc") continue
      if (group.prefixes.some(prefix => id.startsWith(prefix))) return group.key
    }
    return "etc"
  }

  // 선택 그룹별 설비 목록 필터링
  const filteredList = computed(() => {
    if (selectedGroup.value === "all") return props.list
    return props.list.filter(e => getGroupKey(e.id) === selectedGroup.value)
  })

  // 정렬 및 필터링 된 설비목록
  const sortedList = computed(() =>
    [...filteredList.value].sort((a, b) => {
      const idCmp = a.id.localeCompare(b.id)
      if (idCmp !== 0) return idCmp
      return STATUS_ORDER[a.status] - STATUS_ORDER[b.status]
    })
  )

  const errorCount = computed(() => props.list.filter(e => e.status === "error").length)
  const stopCount  = computed(() => props.list.filter(e => e.status === "stop").length)
  const idleCount  = computed(() => props.list.filter(e => e.status === "idle").length)
  const runCount   = computed(() => props.list.filter(e => e.status === "run").length)

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
  }

  .header-badges {
    display: flex;
    gap: 0.4rem;
  }

  .hbadge {
    font-size: var(--fs-xs);
    font-weight: 600;
    padding: 2px 9px;
    border-radius: 999px;
  }

  /* red */
  .hbadge.error {
    background: rgba(239, 68, 68, 0.12);
    color: #ef4444;
    border: 1px solid rgba(239, 68, 68, 0.3);
  }

  /* orange */
  .hbadge.stop {
    background: rgba(245, 158, 11, 0.12);
    color: #f59e0b;
    border: 1px solid rgba(245, 158, 11, 0.3);
  }

  /* gray */
  .hbadge.idle {
    background: rgba(100, 116, 139, 0.12);
    color: #94a3b8;
    border: 1px solid rgba(100, 116, 139, 0.3);
  }

  /* green */
  .hbadge.run {
    background: rgba(34, 197, 94, 0.12);
    color: #22c55e;
    border: 1px solid rgba(34, 197, 94, 0.3);
  }

  .group-btn {
    font-size: var(--fs-xs);
    font-weight: 600;
    padding: 3px 10px;
    border-radius: 999px;
    border: 1px solid #1e3a5f;
    background: transparent;
    color: #8899b4;
    cursor: pointer;
    transition: background 0.15s, color 0.15s, border-color 0.15s;
    white-space: nowrap;
  }

  .group-btn:hover {
    background: rgba(255, 255, 255, 0.05);
    color: #cbd5e1;
  }

  .group-btn.active {
    background: rgba(56, 139, 253, 0.15);
    color: #60a5fa;
    border-color: rgba(56, 139, 253, 0.4);
  }

  .table-wrap {
    flex: 1;
    overflow-y: auto;
    overflow-x: auto;
  }

  .equip-table {
    width: 100%;
    border-collapse: collapse;
  }

  .equip-table th {
    font-size: var(--fs-xs);
    color: #8899b4; /* gray */
    text-align: left;
    padding: 0.6rem 0.75rem 0.6rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    border-bottom: 1px solid #1e3a5f; /* navy */
    position: sticky;
    top: 0;
    background: #0d2137; /* black */
  }

  .equip-table td {
    padding: 0.55rem 0.75rem;
    font-size: var(--fs-sm);
    color: #94a3b8; /* gray */
    border-bottom: 1px solid rgba(30, 58, 95, 0.4); /* gray */
  }

  .equip-table tbody tr:last-child td {
    border-bottom: none;
  }

  .equip-table tbody tr:hover td {
    background: rgba(255, 255, 255, 0.03); /* white */
  }

  .row-error td {
    background: rgba(239, 68, 68, 0.04); /* ivory */
  }

  .row-stop td {
    background: rgba(245, 158, 11, 0.03); /* white */
  }

  .status-badge {
    font-size: var(--fs-xs);
    font-weight: 700;
    padding: 2px 8px;
    border-radius: 4px;
    letter-spacing: 0.04em;
    display: inline-block;
    white-space: nowrap;
  }

  /* green */
  .badge-run {
    background: rgba(34, 197, 94, 0.15);
    color: #22c55e;
    border: 1px solid rgba(34, 197, 94, 0.35);
  }

  /* gray */
  .badge-idle {
    background: rgba(100, 116, 139, 0.15);
    color: #94a3b8;
    border: 1px solid rgba(100, 116, 139, 0.35);
  }

  /* orange */
  .badge-stop {
    background: rgba(245, 158, 11, 0.15);
    color: #f59e0b;
    border: 1px solid rgba(245, 158, 11, 0.35);
  }

  /* red */
  .badge-error {
    background: rgba(239, 68, 68, 0.15);
    color: #ef4444;
    border: 1px solid rgba(239, 68, 68, 0.35);
  }

  .td-name {
    font-weight: 600;
    color: #cbd5e1; /* light gray */
  }

  .td-uptime {
    font-variant-numeric: tabular-nums;
    color: #94a3b8; /* gray */
  }

  .td-error {
    font-size: var(--fs-xs);
    color: #8899b4; /* dark gray */
  }

  .empty-text {
    text-align: center;
    padding: 2rem !important;
    color: #8899b4; /* dark gray 계열 */
    font-size: var(--fs-base);
  }
</style>
