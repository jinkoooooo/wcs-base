<!--
  재입고 대기 알람 배너 — 작업대 최상단 풀폭.
  due 0건이면 숨김. 울리는 중이면 빨강 펄스, 해제(silenced)면 앰버. 클릭 시 목록 모달.
-->
<template>
  <div v-if="dueCount > 0" class="rb-bar" :class="{ ringing }" @click="$emit('open')">
    <span class="rb-dot" />
    <span class="rb-text">재입고 대기 {{ dueCount }}건 — 재입고가 필요한 파렛트가 있습니다</span>
    <button class="rb-dismiss" type="button" @click.stop="$emit('dismiss')">알람 해제</button>
    <span class="rb-hint">클릭하여 목록 보기</span>
  </div>
</template>

<script lang="ts" setup>
  defineProps<{ dueCount: number; ringing: boolean }>();
  defineEmits<{ (e: 'open'): void; (e: 'dismiss'): void }>();
</script>

<style scoped>
  /* 기본(해제됨) — 앰버. 울리는 중 ringing 클래스로 빨강 + 펄스. */
  .rb-bar {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 14px;
    background: #fffbeb;
    border-bottom: 1px solid #fde68a;
    color: #92400e;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    user-select: none;
  }
  .rb-bar.ringing {
    background: #fef2f2;
    border-bottom-color: #fecaca;
    color: #991b1b;
    animation: rb-pulse 1.1s ease-in-out infinite;
  }
  .rb-dot {
    width: 9px;
    height: 9px;
    border-radius: 9999px;
    background: #f59e0b;
    flex: none;
  }
  .rb-bar.ringing .rb-dot {
    background: #ef4444;
  }
  .rb-text {
    flex: 1;
    min-width: 0;
  }
  .rb-dismiss {
    flex: none;
    padding: 4px 12px;
    border: 1px solid currentColor;
    border-radius: 6px;
    background: transparent;
    color: inherit;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
  }
  .rb-dismiss:hover {
    background: rgba(0, 0, 0, 0.05);
  }
  .rb-hint {
    flex: none;
    font-size: 11px;
    font-weight: 500;
    opacity: 0.7;
  }
  @keyframes rb-pulse {
    0%,
    100% {
      background: #fef2f2;
    }
    50% {
      background: #fee2e2;
    }
  }
</style>
