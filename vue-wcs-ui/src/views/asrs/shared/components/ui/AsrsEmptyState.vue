<template>
  <!--
    공통 empty state.
    상세 패널 / 리스트 / 이력 탭 등에서 재사용
  -->
  <div class="asrs-ui-empty">
    <slot>{{ message }}</slot>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    message?: string;
  }>(),
  {
    message: '표시할 데이터가 없습니다.',
  },
);
</script>
