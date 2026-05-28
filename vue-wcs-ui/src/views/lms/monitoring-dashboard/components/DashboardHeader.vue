<template>
  <div class="header shrink-0 border border-[#1e3a5f] rounded-xl px-5 py-4 flex flex-col gap-3">

    <!-- 센터명, 센터 상태 라벨 -->
    <div class="flex items-start gap-2">
      <span class="status-dot rounded-full shrink-0" :class="`dot-${status}`"></span>
      <h1 class="center-name m-0">{{ lcNm }}</h1>
    </div>
    <span class="status-badge px-[10px] py-[2px] rounded-full self-start"
          :class="`badge-${status}`">{{ statusLabel }}
    </span>

    <!-- 센터코드, 라인코드 -->
    <div class="flex gap-2 flex-wrap">
      <span class="meta-chip"><EnvironmentOutlined />{{ lcId }}</span>
      <span class="meta-chip"><ApartmentOutlined />{{ lineCnt }} {{ t("label.lines") }}</span>
    </div>

    <!-- 현재날짜 및 시각 -->
    <div class="border-t border-[#1e3a5f] pt-3">
      <div class="clock-date mb-1">{{ currentDate }}</div>
      <div class="clock-time">{{ currentTime }}</div>
    </div>

  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, onUnmounted, ref } from "vue"
  import { useLocaleStore } from "@/store/modules/locale";
  import { useI18n } from "@/hooks/web/useI18n";
  import { CenterStatusType } from "@/views/lms/monitoring-dashboard/types";
  import { EnvironmentOutlined, ApartmentOutlined } from "@ant-design/icons-vue"


  const { getLocale } = useLocaleStore()
  const { t } = useI18n();

  const props = defineProps<{
    lcId: string
    lcNm: string
    lineCnt: number
    status: CenterStatusType
  }>()

  // 센터 상태별 라벨
  const statusLabelMap: Record<CenterStatusType, string> = {
    running: t("label.normal_operation"),
    warning: t("label.attention_required"),
    stopped: t("label.operation_stopped"),
  }
  const statusLabel = computed(() => (statusLabelMap[props.status]))

  /* 데이터 */
  const currentDate = ref("")
  const currentTime = ref("")
  let timer: number // 매 초 시간 업데이트 타이머

  /* 라이프사이클 */
  onMounted(() => {
    const update = () => {
      const now = new Date()
      const locale = getLocale.replace("_", '-')

      currentDate.value = now.toLocaleDateString(locale, {
        year: "numeric", month: "long", day: "numeric", weekday: "short",
      })

      currentTime.value = now.toLocaleTimeString(locale, {
        hour: "2-digit", minute: "2-digit", second: "2-digit",
      })
    }

    update()
    timer = window.setInterval(update, 1000)
  })

  onUnmounted(() => clearInterval(timer))

</script>

<style scoped>

  .header {
    background: linear-gradient(135deg, #0d2137 0%, #132f4c 100%);
  }

  .center-name {
    font-size: var(--fs-lg);
    font-weight: 700;
    letter-spacing: -0.02em;
    color: #e2e8f0;
    line-height: 1.4;
  }

  /* dot을 center-name 첫 줄 세로 중앙에 맞춤 */
  .status-dot {
    width: 9px;
    height: 9px;
    margin-top: calc((var(--fs-lg, 1.125rem) * 1.4 - 9px) / 2);
  }

  /* 센터 상태별 status-dot 색상 변경 */
  .dot-running {
    background: #22c55e;
    box-shadow: 0 0 8px #22c55e80;
    animation: pulse 2s infinite;
  }

  .dot-warning {
    background: #f59e0b;
    box-shadow: 0 0 8px #f59e0b80;
    animation: pulse 1.5s infinite;
  }

  .dot-stopped {
    background: #ef4444;
  }

  /* "pulse" 애니메이션 정의: 깜박이는 효과 */
  @keyframes pulse {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.35;
    }
  }

  /* 센터 상태별 status-badge 색상 변경 */
  .status-badge {
    font-size: var(--fs-xs);
    font-weight: 600;
    letter-spacing: 0.02em;
  }

  .badge-running {
    background: rgba(34, 197, 94, 0.12);
    color: #22c55e;
    border: 1px solid rgba(34, 197, 94, 0.3);
  }

  .badge-warning {
    background: rgba(245, 158, 11, 0.12);
    color: #f59e0b;
    border: 1px solid rgba(245, 158, 11, 0.3);
  }

  .badge-stopped {
    background: rgba(239, 68, 68, 0.12);
    color: #ef4444;
    border: 1px solid rgba(239, 68, 68, 0.3);
  }

  .meta-chip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: var(--fs-sm);
    color: #94a3b8;
    background: rgba(255, 255, 255, 0.04);
    padding: 3px 10px;
    border-radius: 999px;
    border: 1px solid rgba(255, 255, 255, 0.08);
  }

  .clock-date {
    font-size: var(--fs-sm);
    color: #8899b4;
  }

  .clock-time {
    font-size: var(--fs-xl);
    font-weight: 700;
    font-variant-numeric: tabular-nums;
    color: #38bdf8;
    letter-spacing: 0.04em;
  }
</style>
