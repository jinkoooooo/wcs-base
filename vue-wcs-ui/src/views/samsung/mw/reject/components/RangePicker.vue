<template>
  <div class="flex items-center gap-2">
    <DatePicker
      v-model="start"
      @change="handleStartChange"
    />

    <span class="text-center">-</span>

    <DatePicker
      v-model="end"
      @change="handleEndChange"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import dayjs from 'dayjs'
import DatePicker from '@/views/samsung/mw/reject/components/DatePicker.vue'

const props = defineProps({
  modelValue: {
    type: Array, // ['YYYY-MM-DD', 'YYYY-MM-DD']
    default: () => []
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

/** 상태 */
const start = ref(props.modelValue?.[0] || '')
const end = ref(props.modelValue?.[1] || '')

watch(
  () => props.modelValue,
  (v) => {
    start.value = v?.[0] || ''
    end.value = v?.[1] || ''
  }
)

/**
 * 시작일 변경
 * - v: YYYY-MM-DD 형식
 * - 종료일이 시작일보다 빠르지 않도록 변경
 */
const handleStartChange = (v) => {
  start.value = v;

  if (end.value && dayjs(v).isAfter(dayjs(end.value))) {
    end.value = v
  }

  emitChange()
}

/**
 * 종료일 변경
 * - v: YYYY-MM-DD 형식
 * - 시작일이 종료일보다 느리지 않도록 변경
 */
const handleEndChange = (v) => {
  end.value = v
  if (start.value && dayjs(v).isBefore(dayjs(start.value))) {
    start.value = v
  }

  emitChange()
}

/**
 * emit
 */
const emitChange = () => {
  const value = [start.value, end.value]
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style scoped>

</style>
