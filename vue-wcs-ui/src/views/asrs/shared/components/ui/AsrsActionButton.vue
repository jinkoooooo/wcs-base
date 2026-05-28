<template>
  <!--
    공통 액션 버튼.
    variant / loading / disabled 규칙을 한 곳에서 통일
  -->
  <button
    :type="type"
    :class="buttonClass"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading && loadingText">{{ loadingText }}</span>
    <slot v-else />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue';

type AsrsActionButtonVariant = 'primary' | 'secondary' | 'ghost';

const props = withDefaults(
  defineProps<{
    variant?: AsrsActionButtonVariant;
    type?: 'button' | 'submit' | 'reset';
    disabled?: boolean;
    loading?: boolean;
    loadingText?: string;
  }>(),
  {
    variant: 'ghost',
    type: 'button',
    disabled: false,
    loading: false,
    loadingText: '',
  },
);

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void;
}>();

/**
 * variant 별 공통 클래스 매핑
 */
const buttonClass = computed(() => {
  return [
    'asrs-ui-btn',
    `asrs-ui-btn--${props.variant}`,
  ];
});

/**
 * disabled / loading 상태에서는 click emit 차단
 */
function handleClick(event: MouseEvent) {
  if (props.disabled || props.loading) return;
  emit('click', event);
}
</script>
