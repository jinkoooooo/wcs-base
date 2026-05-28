<template>
  <div ref="rootRef" class="relative inline-block w-full">
    <button v-if="!inline" type="button" @click="toggle()" class="date-trigger">
      <svg class="date-icon" viewBox="0 0 20 20" fill="currentColor">
        <path
          d="M6 2a1 1 0 100 2h8a1 1 0 100-2H6zM3 7a2 2 0 012-2h10a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V7zm2 3h10v6H5v-6z"
        />
      </svg>
      {{ displayValue }}
    </button>

    <div v-show="inline || open" class="calendar-panel" :class="!inline ? 'calendar-dropdown' : ''">
      <div class="calendar-header">
        <button class="nav-btn" @click="prevMonth()">‹</button>
        <div class="calendar-title"> {{ viewYear }}년 {{ monthNamesShort[viewMonth] }} </div>
        <button class="nav-btn" @click="nextMonth()">›</button>
      </div>

      <div class="week-grid">
        <div v-for="d in weekNames" :key="d" class="week-name">{{ d }}</div>
      </div>

      <div class="day-grid">
        <button
          v-for="cell in cells"
          :key="cell.key"
          :disabled="cell.disabled"
          class="day-btn"
          :class="[
            !cell.inMonth ? 'is-out' : '',
            cell.isToday ? 'is-today' : '',
            cell.isSelected ? 'is-selected' : '',
          ]"
          @click="select(cell.date)"
        >
          {{ cell.day }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

  const props = defineProps<{
    modelValue?: string | Date | null;
    inline?: boolean;
    min?: string | Date | null;
    max?: string | Date | null;
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', v: string): void;
    (e: 'change', v: string): void;
  }>();

  const open = ref(false);
  const rootRef = ref<HTMLElement | null>(null);

  function toDate(v?: string | Date | null): Date {
    if (!v) return new Date();
    if (v instanceof Date) return v;
    const [y, m, d] = v.split('-').map((n) => Number(n));
    return new Date(y, (m || 1) - 1, d || 1);
  }

  function toISO(d: Date) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  const selected = ref<Date>(toDate(props.modelValue ?? new Date()));
  const viewYear = ref(selected.value.getFullYear());
  const viewMonth = ref(selected.value.getMonth());

  watch(
    () => props.modelValue,
    (v) => {
      if (!v) return;
      const nd = toDate(v);
      selected.value = nd;
      viewYear.value = nd.getFullYear();
      viewMonth.value = nd.getMonth();
    },
  );

  const monthNamesShort = [
    '1월',
    '2월',
    '3월',
    '4월',
    '5월',
    '6월',
    '7월',
    '8월',
    '9월',
    '10월',
    '11월',
    '12월',
  ];
  const weekNames = ['일', '월', '화', '수', '목', '금', '토'];

  const cells = computed(() => {
    const y = viewYear.value;
    const m = viewMonth.value;
    const first = new Date(y, m, 1);
    const startDay = first.getDay();
    const daysInMonth = new Date(y, m + 1, 0).getDate();
    const daysPrevMonth = new Date(y, m, 0).getDate();
    const arr: any[] = [];

    for (let i = startDay - 1; i >= 0; i--) {
      const d = new Date(y, m - 1, daysPrevMonth - i);
      arr.push(buildCell(d, false));
    }

    for (let d = 1; d <= daysInMonth; d++) {
      arr.push(buildCell(new Date(y, m, d), true));
    }

    while (arr.length % 7 !== 0 || arr.length < 42) {
      const last = arr[arr.length - 1].date as Date;
      const next = new Date(last);
      next.setDate(last.getDate() + 1);
      arr.push(buildCell(next, false));
    }

    return arr;
  });

  function buildCell(d: Date, inMonth: boolean) {
    const today = new Date();
    const isToday =
      d.getFullYear() === today.getFullYear() &&
      d.getMonth() === today.getMonth() &&
      d.getDate() === today.getDate();

    const isSelected =
      d.getFullYear() === selected.value.getFullYear() &&
      d.getMonth() === selected.value.getMonth() &&
      d.getDate() === selected.value.getDate();

    return {
      key: toISO(d),
      date: d,
      inMonth,
      isToday,
      isSelected,
      disabled: isOutOfRange(d),
      day: d.getDate(),
    };
  }

  function isOutOfRange(d: Date) {
    if (props.min) {
      const mn = toDate(props.min);
      if (d < new Date(mn.getFullYear(), mn.getMonth(), mn.getDate())) return true;
    }
    if (props.max) {
      const mx = toDate(props.max);
      if (d > new Date(mx.getFullYear(), mx.getMonth(), mx.getDate())) return true;
    }
    return false;
  }

  function prevMonth() {
    if (viewMonth.value === 0) {
      viewMonth.value = 11;
      viewYear.value--;
    } else {
      viewMonth.value--;
    }
  }

  function nextMonth() {
    if (viewMonth.value === 11) {
      viewMonth.value = 0;
      viewYear.value++;
    } else {
      viewMonth.value++;
    }
  }

  function select(d: Date) {
    if (isOutOfRange(d)) return;
    selected.value = d;
    viewYear.value = d.getFullYear();
    viewMonth.value = d.getMonth();
    const iso = toISO(d);
    emit('update:modelValue', iso);
    emit('change', iso);
    if (!props.inline) open.value = false;
  }

  function toggle() {
    open.value = !open.value;
  }

  function onDocClick(e: MouseEvent) {
    if (props.inline) return;
    const el = rootRef.value;
    if (el && !el.contains(e.target as Node)) open.value = false;
  }

  onMounted(() => document.addEventListener('mousedown', onDocClick));
  onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick));

  const displayValue = computed(() => toISO(selected.value));
</script>

<style scoped>
  .date-trigger {
    width: 100%;
    height: 38px;
    padding: 0 12px 0 36px;
    border-radius: 12px;
    border: 1px solid #cbd5e1;
    background: #fff;
    font-size: 14px;
    color: #0f172a;
    text-align: left;
    position: relative;
    cursor: pointer;
  }

  .date-icon {
    position: absolute;
    left: 12px;
    top: 10px;
    width: 16px;
    height: 16px;
    opacity: 0.7;
  }

  .calendar-panel {
    z-index: 50;
    width: 220px;
    user-select: none;
    border-radius: 16px;
    background: #fff;
    padding: 8px;
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
    border: 1px solid #e2e8f0;
  }

  .calendar-dropdown {
    position: absolute;
    right: 0;
    top: calc(100% + 8px);
  }

  .calendar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 4px;
  }

  .calendar-title {
    font-size: 14px;
    font-weight: 700;
    color: #0f172a;
  }

  .nav-btn {
    border: none;
    background: transparent;
    width: 28px;
    height: 28px;
    border-radius: 8px;
    cursor: pointer;
  }

  .nav-btn:hover {
    background: #f1f5f9;
  }

  .week-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
    padding: 0 4px 4px;
  }

  .week-name {
    text-align: center;
    font-size: 11px;
    color: #64748b;
  }

  .day-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
    padding: 0 4px 4px;
  }

  .day-btn {
    height: 32px;
    border: none;
    border-radius: 8px;
    background: transparent;
    font-size: 14px;
    color: #0f172a;
    cursor: pointer;
  }

  .day-btn:hover {
    background: #ecfdf5;
  }

  .day-btn.is-out {
    color: #94a3b8;
  }

  .day-btn.is-today {
    outline: 1px solid rgba(16, 185, 129, 0.55);
  }

  .day-btn.is-selected {
    background: #059669;
    color: #fff;
  }

  .day-btn:disabled {
    cursor: default;
    opacity: 0.4;
  }
</style>
