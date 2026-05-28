<template>
  <section class="asrs-ui-panel">
    <div class="asrs-ui-panel__header">
      <div>
        <p v-if="eyebrow" class="asrs-ui-panel__eyebrow">{{ eyebrow }}</p>
        <h3 v-if="title" class="asrs-ui-panel__title">{{ title }}</h3>
      </div>

      <div v-if="$slots.actions" class="asrs-ui-panel__actions">
        <slot name="actions" />
      </div>
    </div>

    <!-- 핵심: body가 flex:1 / min-height:0 이어야 내부스크롤 가능 -->
    <div class="asrs-ui-panel__body">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * 공용 ASRS 패널
 *
 * 중요:
 * - 자식 화면에서 내부 스크롤을 쓰려면
 *   panel body 가 반드시 flex:1 / min-height:0 구조여야 한다.
 */
defineProps<{
  eyebrow?: string;
  title?: string;
}>();
</script>

<style scoped>
.asrs-ui-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px;
  border-radius: 24px;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);

  /* 내부 스크롤을 위해 반드시 필요 */
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.asrs-ui-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.asrs-ui-panel__eyebrow {
  margin: 0 0 6px 0;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #2563eb;
}

.asrs-ui-panel__title {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: #122033;
}

.asrs-ui-panel__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

/* 핵심 */
.asrs-ui-panel__body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
