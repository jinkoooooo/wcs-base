<!--
  [wcs-ops Step 14] 운영 모드 배지 + 4개 기능 플래그 도트.
  상단 탭바 우측(ECS 상태 옆)에 배치.
   - operationMode:        NORMAL/INBOUND_PRIORITY/OUTBOUND_PRIORITY/RELOCATION/MAINTENANCE
   - operationModeEnabled: 플래그 ON 이면 모드가 효력 있음 → 배지 진하게
   - putbackEnabled / dispatchLockEnabled / inspectionEnabled: ON 초록, OFF 회색
  클릭 시 운영 콘솔 라우팅(§18) — 경로는 상위에서 결정.
-->
<template>
  <div class="op-mode-badge" :class="modeClass" @click.stop="$emit('click')">
    <span class="op-mode-text">{{ modeLabel }}</span>
    <div class="op-flags" v-if="flags">
      <span
        class="op-flag-dot"
        :class="{ on: flags.operationModeEnabled }"
        title="운영 모드 게이팅 적용 중"
        >M</span
      >
      <span class="op-flag-dot" :class="{ on: flags.putbackEnabled }" title="PUTBACK 자동 생성 활성"
        >P</span
      >
      <span class="op-flag-dot" :class="{ on: flags.dispatchLockEnabled }" title="포트 배차 락 활성"
        >L</span
      >
      <span
        class="op-flag-dot"
        :class="{ on: flags.inspectionEnabled }"
        title="입고 검수 파이프라인 활성"
        >I</span
      >
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
  import {
    fetchSystemMode,
    type SystemModeFlags,
    type SystemModeInfo,
  } from '../../api/systemModeApi';

  const props = defineProps<{
    /** selectedEqGroupId — null 이면 GLOBAL 모드 표시 */
    eqGroupId: string | null;
    /** 폴링 간격 (ms). Step 19 WebSocket 도입 시 null 로 꺼도 됨. 기본 10초. */
    pollInterval?: number;
  }>();

  defineEmits<{ (e: 'click'): void }>();

  const operationMode = ref<string | null>(null);
  const flags = ref<SystemModeFlags | null>(null);

  async function refresh() {
    try {
      const info: SystemModeInfo = await fetchSystemMode(props.eqGroupId);
      operationMode.value = info?.operationMode ?? null;
      flags.value = info?.flags ?? null;
    } catch (e) {
      // 조용히 무시 (네트워크 단절 등) — 배지는 마지막 상태 유지
    }
  }

  let timer: any = null;
  function startPolling() {
    stopPolling();
    const itv = props.pollInterval ?? 10_000;
    if (itv > 0) timer = setInterval(refresh, itv);
  }
  function stopPolling() {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  }

  onMounted(() => {
    refresh();
    startPolling();
  });
  onUnmounted(() => stopPolling());
  watch(
    () => props.eqGroupId,
    () => {
      refresh();
    },
  );

  const modeLabel = computed(() => {
    switch (
      operationMode.value // <-- 지역 ref 값 참조
    ) {
      case 'NORMAL':
        return 'NORMAL';
      case 'INBOUND_PRIORITY':
        return '입고 우선';
      case 'OUTBOUND_PRIORITY':
        return '출고 우선';
      case 'RELOCATION':
        return '재배치';
      case 'MAINTENANCE':
        return '점검';
      default:
        return operationMode.value ?? '-';
    }
  });

  const modeClass = computed(() => {
    const on = flags.value?.operationModeEnabled ?? false; // <-- flags.value 로 수정
    const base = 'mode-' + (operationMode.value?.toLowerCase() ?? 'unknown'); // <-- operationMode.value 로 수정
    return { [base]: true, 'mode-inactive': !on };
  });
</script>

<style scoped>
  .op-mode-badge {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 4px 10px;
    font-size: 12px;
    font-weight: 700;
    border-radius: 14px;
    cursor: pointer;
    border: 1px solid transparent;
    transition: background 0.2s;
  }
  .op-mode-badge:hover {
    filter: brightness(1.15);
  }

  .op-mode-text {
    letter-spacing: 0.3px;
  }

  .mode-inactive {
    opacity: 0.55;
  }

  .mode-normal {
    background: rgba(144, 147, 153, 0.18);
    color: #c0c4cc;
    border-color: rgba(144, 147, 153, 0.35);
  }
  .mode-inbound_priority {
    background: rgba(103, 194, 58, 0.18);
    color: #67c23a;
    border-color: rgba(103, 194, 58, 0.4);
  }
  .mode-outbound_priority {
    background: rgba(230, 162, 60, 0.18);
    color: #e6a23c;
    border-color: rgba(230, 162, 60, 0.4);
  }
  .mode-relocation {
    background: rgba(64, 158, 255, 0.18);
    color: #409eff;
    border-color: rgba(64, 158, 255, 0.4);
  }
  .mode-maintenance {
    background: rgba(245, 108, 108, 0.25);
    color: #f56c6c;
    border-color: rgba(245, 108, 108, 0.5);
    animation: op-mode-blink 1.5s ease-in-out infinite;
  }
  @keyframes op-mode-blink {
    0%,
    100% {
      box-shadow: 0 0 0 0 rgba(245, 108, 108, 0);
    }
    50% {
      box-shadow: 0 0 8px 2px rgba(245, 108, 108, 0.55);
    }
  }

  .op-flags {
    display: inline-flex;
    gap: 3px;
    margin-left: 2px;
  }
  .op-flag-dot {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    font-size: 9px;
    font-weight: 700;
    border-radius: 50%;
    background: rgba(144, 147, 153, 0.25);
    color: #8a8d93;
  }
  .op-flag-dot.on {
    background: rgba(103, 194, 58, 0.35);
    color: #a6de6e;
    box-shadow: 0 0 4px rgba(103, 194, 58, 0.35);
  }
</style>
