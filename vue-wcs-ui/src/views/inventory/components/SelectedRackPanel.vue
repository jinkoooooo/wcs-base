<template>
  <div
    style="position: absolute; top: 20px; right: 250px; width: 250px; background: rgba(0,0,0,0.8); color: white; padding: 15px; border-radius: 8px; z-index: 10;"
    @pointerdown.stop
    @pointermove.stop
    @pointerup.stop
  >
    <h3 style="margin-top: 0; color: #42b883;">선택된 랙 ({{ selectedCodes.length }}개)</h3>
    <div style="max-height: 200px; overflow-y: auto; font-size: 14px;">
      <div v-if="selectedCodes.length === 0" style="color: #888;">선택된 객체가 없습니다.</div>
      <div v-for="code in selectedCodes" :key="code" style="padding: 4px 0; border-bottom: 1px solid #444;">
        📍 {{ code }}
      </div>
    </div>

    <button
      v-if="selectedCodes.length > 0"
      @click="$emit('clear')"
      style="margin-top: 10px; width: 100%; padding: 5px; background: #d9534f; color: white; border: none; cursor: pointer;"
    >
      선택 해제
    </button>
  </div>
</template>

<script setup>
defineProps({
  selectedCodes: {
    type: Array,
    required: true,
    default: () => []
  }
})

defineEmits(['clear'])
</script>
