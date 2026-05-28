<!--
  DashboardToast.vue
  대시보드 토스트 메시지 컴포넌트

  ============================================
  기능 설명
  ============================================
  화면 하단 중앙에 일시적으로 표시되는 알림 메시지입니다.

  타입별 색상:
  - success (성공): 녹색 배경
  - error (오류): 빨간색 배경
  - info (정보): 파란색 배경

  애니메이션:
  - 아래에서 위로 슬라이드하며 나타남
  - 지정된 시간 후 자동으로 사라짐
-->

<template>
  <Transition name="toast">
    <div v-if="message" class="toast-message" :class="type" @click="$emit('close')">
      {{ message }}
    </div>
  </Transition>
</template>

<script setup lang="ts">
  /**
   * DashboardToast - 토스트 메시지 컴포넌트
   *
   * Props:
   * - message: 표시할 메시지 (null이면 숨김)
   * - type: 메시지 타입 (success/error/info)
   *
   * Events:
   * - close: 토스트 클릭 시 발생
   */

  defineProps<{
    /** 표시할 메시지 (null이면 토스트 숨김) */
    message: string | null;
    /** 메시지 타입 */
    type: 'success' | 'error' | 'info';
  }>();

  defineEmits<{
    (e: 'close'): void;
  }>();
</script>

<style scoped>
  .toast-message {
    position: fixed;
    bottom: 100px;
    left: 50%;
    transform: translateX(-50%);
    padding: 12px 24px;
    border-radius: 8px;
    font-size: 14px;
    z-index: 9999;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  }

  /* 타입별 색상 */
  .toast-message.success {
    background-color: rgba(103, 194, 58, 0.95);
    color: white;
  }

  .toast-message.error {
    background-color: rgba(245, 108, 108, 0.95);
    color: white;
  }

  .toast-message.info {
    background-color: rgba(64, 158, 255, 0.95);
    color: white;
  }

  /* 트랜지션 애니메이션 */
  .toast-enter-active {
    animation: toast-in 0.3s ease-out;
  }

  .toast-leave-active {
    animation: toast-out 0.2s ease-in;
  }

  @keyframes toast-in {
    from {
      opacity: 0;
      transform: translateX(-50%) translateY(20px);
    }
    to {
      opacity: 1;
      transform: translateX(-50%) translateY(0);
    }
  }

  @keyframes toast-out {
    from {
      opacity: 1;
      transform: translateX(-50%) translateY(0);
    }
    to {
      opacity: 0;
      transform: translateX(-50%) translateY(-10px);
    }
  }
</style>
