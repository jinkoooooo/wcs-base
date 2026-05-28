<template>
  <AsrsFormField
    :model-value="modelValue"
    label="Area"
    type="select"
    :label-mode="labelMode"
    :compact="compact"
    :options="normalizedOptions"
    :placeholder="placeholder"
    :disabled="disabled"
    @update:model-value="$emit('update:modelValue', $event as string)"
    @enter="$emit('enter')"
  />
</template>

<script setup lang="ts">
/**
 * Area Code 전용 공통 필드.
 *
 * 목적:
 * - 모든 화면에서 Area 입력을 드롭다운으로 통일
 * - option label 을 "areaCode - areaName" 형식으로 공통 처리
 * - AsrsFormField 공통 컴포넌트 위에 얇은 래퍼로 구현
 */

import { computed } from 'vue';
import AsrsFormField from './AsrsFormField.vue';

export interface AreaCodeOption {
  id?: string;
  areaCode: string;
  areaName?: string;
}

const props = withDefaults(
  defineProps<{
    modelValue: string;
    options: AreaCodeOption[];
    disabled?: boolean;
    compact?: boolean;
    labelMode?: 'left' | 'top' | 'inside';
    placeholder?: string;
  }>(),
  {
    disabled: false,
    compact: true,
    labelMode: 'left',
    placeholder: '선택',
  },
);

defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'enter'): void;
}>();

/**
 * 공통 option normalize
 *
 * label 규칙:
 * - areaName 있으면 "ASRS1 - 실온창고"
 * - 없으면 "ASRS1"
 */
const normalizedOptions = computed(() =>
  props.options.map((area) => ({
    value: area.areaCode,
    label: area.areaName ? `${area.areaCode} - ${area.areaName}` : area.areaCode,
  })),
);
</script>
