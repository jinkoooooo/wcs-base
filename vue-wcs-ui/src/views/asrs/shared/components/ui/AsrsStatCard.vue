<template>
  <!--
    상단 KPI/통계 카드 공통 컴포넌트
  -->
  <article class="asrs-ui-stat-card" :class="{ 'asrs-ui-stat-card--accent': accent }">
    <span class="asrs-ui-stat-card__label">{{ label }}</span>
    <strong class="asrs-ui-stat-card__value">{{ value }}</strong>
    <span class="asrs-ui-stat-card__desc">{{ description }}</span>
  </article>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string;
    value: string | number;
    description?: string;
    accent?: boolean;
  }>(),
  {
    description: '',
    accent: false,
  },
);
</script>
