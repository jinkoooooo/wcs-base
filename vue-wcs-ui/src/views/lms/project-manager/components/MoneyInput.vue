<template>
  <input
    class="grid-input"
    :class="{ empty: isEmpty }"
    inputmode="numeric"
    type="text"
    :value="display"
    @input="onInput"
    @blur="onBlur"
  />
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';

  const props = defineProps<{
    modelValue: string | number | null | undefined; // 내부는 숫자/문자 모두 허용
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', v: string | null): void; // ✅ 콤마 없는 숫자 문자열로 유지
  }>();

  const focused = ref(false);

  const raw = computed(() => {
    const v = props.modelValue;
    if (v == null) return '';
    return String(v).replace(/[^\d]/g, ''); // 콤마/기타 제거
  });

  const isEmpty = computed(() => raw.value.length === 0);

  const display = computed(() => {
    // 포커스 중엔 입력 편의상 "콤마 없는 값" 그대로 보여주고,
    // 포커스 아닐 땐 콤마 포맷으로 보여줌
    if (focused.value) return raw.value;

    if (!raw.value) return '';
    const n = Number(raw.value);
    if (!Number.isFinite(n)) return '';
    return new Intl.NumberFormat('ko-KR').format(n);
  });

  function onInput(e: Event) {
    focused.value = true;

    const v = (e.target as HTMLInputElement).value ?? '';
    const digits = v.replace(/[^\d]/g, '');

    // 빈 값이면 null
    emit('update:modelValue', digits ? digits : null);
  }

  function onBlur() {
    focused.value = false;
  }
</script>

<style scoped>
  .grid-input {
    width: 100%;
    height: 30px;
    padding: 4px 8px;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
    outline: none;
    background: #fff;
    font-size: 12px;
    box-sizing: border-box;
    text-align: right; /* ✅ 돈은 우측정렬 기본 */
  }

  .grid-input:focus {
    border-color: rgba(91, 97, 246, 0.6);
    box-shadow: 0 0 0 2px rgba(91, 97, 246, 0.12);
  }
</style>
