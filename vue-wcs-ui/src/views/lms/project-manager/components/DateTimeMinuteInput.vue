<template>
  <input
    class="grid-input"
    type="datetime-local"
    step="60"
    :value="asLocalValue"
    @input="onInput"
  />
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  const props = defineProps<{
    modelValue: string | null | undefined;
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', v: string | null): void;
  }>();

  // 화면: YYYY-MM-DDTHH:mm
  const asLocalValue = computed(() => {
    const v = String(props.modelValue ?? '').trim();
    if (!v) return '';

    // "YYYY-MM-DD HH:mm(:ss)" -> "YYYY-MM-DDTHH:mm"
    if (v.includes(' ')) {
      const [d, t] = v.split(' ');
      const hhmm = (t ?? '').slice(0, 5);
      if (/^\d{4}-\d{2}-\d{2}$/.test(d) && /^\d{2}:\d{2}$/.test(hhmm)) return `${d}T${hhmm}`;
    }

    // "YYYY-MM-DDTHH:mm" 그대로
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(v)) return v;

    return '';
  });

  function onInput(e: Event) {
    const raw = (e.target as HTMLInputElement).value; // YYYY-MM-DDTHH:mm
    if (!raw) {
      emit('update:modelValue', null);
      return;
    }
    // 저장: "YYYY-MM-DD HH:mm"
    emit('update:modelValue', raw.replace('T', ' '));
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
    font-size: 12px;
    background: #fff;
  }
</style>
