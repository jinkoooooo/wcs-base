<template>
  <div class="year-range">
    <div class="year-field">
      <select class="year-select" :value="fromValue" @change="onFromChange">
        <option v-for="y in fromYearOptions" :key="'f-' + y" :value="String(y)">
          {{ y }}
        </option>
      </select>
    </div>

    <span class="year-sep">~</span>

    <div class="year-field">
      <select class="year-select" :value="toValue" @change="onToChange">
        <option v-for="y in toYearOptions" :key="'t-' + y" :value="String(y)">
          {{ y }}
        </option>
      </select>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted } from 'vue';

  type YearRange = { from: string; to: string };

  const props = defineProps<{ modelValue: YearRange }>();
  const emit = defineEmits<{ (e: 'update:modelValue', v: YearRange): void }>();

  const nowYearNum = new Date().getFullYear();
  const nowYear = String(nowYearNum);

  // ✅ 여기 추가: from 기본값 = 1년 전
  const defaultFrom = String(nowYearNum - 1);
  const defaultTo = String(nowYearNum + 1);

  onMounted(() => {
    // ✅ 여기 수정: from 기본값을 defaultFrom으로
    const from = props.modelValue?.from || defaultFrom;
    const to = props.modelValue?.to || defaultTo;
    emit('update:modelValue', normalizeRange({ from, to }));
  });

  // ✅ 여기 수정: computed 기본값도 defaultFrom/defaultTo로
  const fromValue = computed(() => props.modelValue?.from || defaultFrom);
  const toValue = computed(() => props.modelValue?.to || defaultTo);

  const yearOptions = computed(() => {
    const start = nowYearNum - 20;
    const end = nowYearNum + 5;
    const arr: number[] = [];
    for (let y = end; y >= start; y--) arr.push(y);
    return arr;
  });

  const fromYearNum = computed(() => Number(fromValue.value || defaultFrom));

  const fromYearOptions = computed(() => yearOptions.value);
  const toYearOptions = computed(() => yearOptions.value.filter((y) => y >= fromYearNum.value));

  function clampYearStr(v: string) {
    const n = Number(v);
    if (!Number.isFinite(n)) return nowYear;
    return String(Math.min(2100, Math.max(1900, n)));
  }

  function normalizeRange(r: YearRange): YearRange {
    const f = clampYearStr(r.from);
    const t = clampYearStr(r.to);
    if (Number(f) > Number(t)) return { from: t, to: f };
    return { from: f, to: t };
  }

  function emitRange(next: Partial<YearRange>) {
    emit(
      'update:modelValue',
      normalizeRange({
        // ✅ 여기 수정: emitRange도 기본값을 defaultFrom/defaultTo로
        from: props.modelValue?.from || defaultFrom,
        to: props.modelValue?.to || defaultTo,
        ...next,
      }),
    );
  }

  function onFromChange(e: Event) {
    emitRange({ from: (e.target as HTMLSelectElement).value });
  }
  function onToChange(e: Event) {
    emitRange({ to: (e.target as HTMLSelectElement).value });
  }
</script>

<style scoped>
  .year-range {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
  }
  .year-field {
    flex: 1;
    display: flex;
    align-items: center;
  }
  .year-sep {
    color: #64748b;
    font-size: 0.875rem;
  }

  .year-select {
    width: 100%;
    padding: 8px 12px;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
    background-color: #ffffff;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.02);
    font-size: 0.875rem;
  }
</style>
