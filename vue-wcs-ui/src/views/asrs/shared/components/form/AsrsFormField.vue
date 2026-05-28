<template>
  <div
    class="asrs-form-field"
    :class="[
      `asrs-form-field--${labelMode}`,
      { 'asrs-form-field--compact': compact, 'asrs-form-field--disabled': disabled },
    ]"
    :style="labelMode === 'left' ? { '--asrs-label-width': labelWidth } : {}"
  >
    <!-- left / top 모드일 때만 별도 label 표시 -->
    <label
      v-if="labelMode !== 'inside'"
      class="asrs-form-field__label"
      :for="inputId"
    >
      {{ label }}
      <span v-if="required" class="asrs-form-field__required">*</span>
    </label>

    <div class="asrs-form-field__control">
      <!-- text / number / date -->
      <input
        v-if="isInputType"
        :id="inputId"
        class="asrs-form-field__input"
        :type="type"
        :value="displayValue"
        :placeholder="resolvedPlaceholder"
        :disabled="disabled"
        :readonly="readonly"
        @input="onInput"
        @keyup.enter="$emit('enter')"
      />

      <!-- select -->
      <select
        v-else-if="type === 'select'"
        :id="inputId"
        class="asrs-form-field__select"
        :value="displayValue"
        :disabled="disabled"
        @change="onSelectChange"
      >
        <option v-if="placeholder" value="">
          {{ placeholder }}
        </option>

        <option
          v-for="option in options"
          :key="String(option.value)"
          :value="String(option.value)"
        >
          {{ option.label }}
        </option>
      </select>

      <!-- textarea -->
      <textarea
        v-else-if="type === 'textarea'"
        :id="inputId"
        class="asrs-form-field__textarea"
        :value="displayValue"
        :placeholder="resolvedPlaceholder"
        :disabled="disabled"
        :readonly="readonly"
        rows="3"
        @input="onTextareaInput"
      />

      <!-- readonly -->
      <div v-else class="asrs-form-field__readonly">
        {{ displayValue || '-' }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 공용 입력 필드 컴포넌트
 *
 * 목적:
 * - 검색/입력 UI를 한 군데서 통일
 * - text / number / date / select / textarea 대응
 * - left / top / inside 라벨 모드 지원
 */

import { computed } from 'vue';

type AsrsFieldType = 'text' | 'number' | 'date' | 'select' | 'textarea' | 'readonly';

interface AsrsFieldOption {
  label: string;
  value: string | number;
}

const props = withDefaults(
  defineProps<{
    label: string;
    modelValue: string | number | null | undefined;
    type?: AsrsFieldType;
    placeholder?: string;
    options?: AsrsFieldOption[];
    disabled?: boolean;
    readonly?: boolean;
    required?: boolean;
    compact?: boolean;
    labelMode?: 'left' | 'top' | 'inside';
    labelWidth?: string;
    inputId?: string;
  }>(),
  {
    type: 'text',
    placeholder: '',
    options: () => [],
    disabled: false,
    readonly: false,
    required: false,
    compact: false,
    labelMode: 'left',
    labelWidth: '86px',
    inputId: '',
  },
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number | null): void;
  (e: 'enter'): void;
}>();

const isInputType = computed(() => {
  return props.type === 'text' || props.type === 'number' || props.type === 'date';
});

const displayValue = computed(() => {
  if (props.modelValue === null || props.modelValue === undefined) return '';
  return String(props.modelValue);
});

const resolvedPlaceholder = computed(() => {
  if (props.placeholder) return props.placeholder;
  if (props.labelMode === 'inside') {
    return props.required ? `${props.label} *` : props.label;
  }
  return '';
});

function onInput(event: Event) {
  const target = event.target as HTMLInputElement;
  const raw = target.value;

  if (props.type === 'number') {
    emit('update:modelValue', raw === '' ? null : Number(raw));
    return;
  }

  emit('update:modelValue', raw);
}

function onTextareaInput(event: Event) {
  const target = event.target as HTMLTextAreaElement;
  emit('update:modelValue', target.value);
}

function onSelectChange(event: Event) {
  const target = event.target as HTMLSelectElement;
  const raw = target.value;

  if (raw === '') {
    emit('update:modelValue', '');
    return;
  }

  // option.value 가 number 인 경우 원래 타입 유지
  const matched = props.options.find((option) => String(option.value) === raw);
  emit('update:modelValue', matched ? matched.value : raw);
}
</script>

<style scoped>
.asrs-form-field {
  min-width: 0;
}

.asrs-form-field--left {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  align-items: center;
  column-gap: 6px;
}

.asrs-form-field--top {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.asrs-form-field--inside {
  display: block;
}

.asrs-form-field__label {
  font-size: 12px;
  font-weight: 800;
  color: #475569;
  white-space: nowrap;
}

.asrs-form-field__required {
  margin-left: 4px;
  color: #dc2626;
}

.asrs-form-field__control {
  min-width: 0;
}

.asrs-form-field__input,
.asrs-form-field__select,
.asrs-form-field__textarea,
.asrs-form-field__readonly {
  width: 100%;
  min-height: 42px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid #cfd9e5;
  background: #fff;
  color: #122033;
  font-size: 14px;
  box-sizing: border-box;
}

.asrs-form-field__textarea {
  min-height: 96px;
  padding: 12px 14px;
  resize: vertical;
}

.asrs-form-field__readonly {
  display: flex;
  align-items: center;
  background: #f7f9fc;
  font-weight: 700;
}

.asrs-form-field--compact .asrs-form-field__input,
.asrs-form-field--compact .asrs-form-field__select,
.asrs-form-field--compact .asrs-form-field__readonly {
  min-height: 38px;
  border-radius: 12px;
  font-size: 13px;
}

.asrs-form-field__input:focus,
.asrs-form-field__select:focus,
.asrs-form-field__textarea:focus {
  outline: none;
  border-color: #6aa6ff;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.10);
}

.asrs-form-field--disabled {
  opacity: 0.65;
}
</style>
