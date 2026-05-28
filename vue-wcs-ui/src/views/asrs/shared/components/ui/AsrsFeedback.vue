<template>
  <!--
    공통 feedback 메시지.
    info / success / warning / error 타입 공통 처리
  -->
  <transition name="fade">
    <div
      v-if="message"
      class="asrs-ui-feedback"
      :class="`asrs-ui-feedback--${type}`"
    >
      {{ message }}
    </div>
  </transition>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    type?: 'info' | 'success' | 'warning' | 'error';
    message?: string;
  }>(),
  {
    type: 'info',
    message: '',
  },
);
</script>
