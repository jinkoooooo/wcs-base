<template>
  <input
    type="date"
    class="date-input"
    :value="modelValue"
    @input="onInput"
  />
</template>

<script setup lang="ts">
const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLInputElement).value)
}
</script>

<style scoped>
.date-input {
  flex: 1;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  background-color: #ffffff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.02);
  font-size: 0.875rem;
}
</style>
