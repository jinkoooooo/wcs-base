<template>
  <div ref="rootRef" class="relative inline-block">
    <!-- 트리거: 드롭다운 모드일 때만 표시 -->
    <button
      v-if="!inline"
      type="button"
      @click="toggle()"
      class="h-9 min-w-[160px] px-3 pl-9 rounded-lg border bg-white text-sm shadow-sm hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-emerald-400/50 relative text-left"
    >
      <svg
        class="absolute left-2.5 top-2.5 h-4 w-4 opacity-70"
        viewBox="0 0 20 20"
        fill="currentColor"
      >
        <path
          d="M6 2a1 1 0 100 2h8a1 1 0 100-2H6zM3 7a2 2 0 012-2h10a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V7zm2 3h10v6H5v-6z"
        />
      </svg>
      {{ displayValue }}
    </button>

    <!-- 캘린더 패널 -->
    <div
      v-show="inline || open"
      class="z-50 mt-2 w-[220px] select-none rounded-xl bg-white p-2 shadow ring-1 ring-slate-200 text-slate-800"
      :class="!inline ? 'absolute right-0' : ''"
    >
      <!-- 헤더 -->
      <div class="flex items-center justify-between px-1 py-1.5">
        <button class="p-1 rounded hover:bg-slate-100" @click="prevMonth()">
          <span class="sr-only">이전달</span> ‹
        </button>
        <div class="text-sm font-semibold"> {{ viewYear }}년 {{ monthNamesShort[viewMonth] }} </div>
        <button class="p-1 rounded hover:bg-slate-100" @click="nextMonth()">
          <span class="sr-only">다음달</span> ›
        </button>
      </div>

      <!-- 요일 -->
      <div class="grid grid-cols-7 gap-1 px-1 pb-1 text-[11px] tracking-tight text-slate-500">
        <div v-for="d in weekNames" :key="d" class="text-center">{{ d }}</div>
      </div>

      <!-- 날짜 -->
      <div class="grid grid-cols-7 gap-1 px-1 pb-1">
        <button
          v-for="cell in cells"
          :key="cell.key"
          :disabled="cell.disabled"
          @click="select(cell.date)"
          class="h-8 rounded-md text-sm transition hover:bg-emerald-50 focus:outline-none focus:ring-2 focus:ring-emerald-400/40"
          :class="[
            !cell.inMonth ? 'text-slate-400' : 'text-slate-800',
            cell.isToday ? 'ring-1 ring-emerald-400/60' : '',
            cell.isSelected ? 'bg-emerald-600 text-white hover:bg-emerald-600' : '',
          ]"
        >
          {{ cell.day }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';

  /** Props & Emits */
  const props = defineProps<{
    modelValue?: string | Date | null;
    inline?: boolean; // true면 항상 펼쳐진 인라인 캘린더
    min?: string | Date | null; // (선택) 최소 날짜
    max?: string | Date | null; // (선택) 최대 날짜
  }>();
  const emit = defineEmits<{
    (e: 'update:modelValue', v: string): void;
    (e: 'change', v: string): void;
  }>();

  /** 상태 */
  const open = ref(false);
  const rootRef = ref<HTMLElement | null>(null);

  function toDate(v?: string | Date | null): Date {
    if (!v) return new Date();
    if (v instanceof Date) return v;
    // YYYY-MM-DD
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

  /** 헤더 표시 */
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

  /** 캘린더 셀 생성 */
  const cells = computed(() => {
    const y = viewYear.value;
    const m = viewMonth.value;
    const first = new Date(y, m, 1);
    const startDay = first.getDay(); // 일요일 시작
    const daysInMonth = new Date(y, m + 1, 0).getDate();
    const daysPrevMonth = new Date(y, m, 0).getDate();
    const arr: any[] = [];

    // 앞쪽 이전달
    for (let i = startDay - 1; i >= 0; i--) {
      const d = new Date(y, m - 1, daysPrevMonth - i);
      arr.push(buildCell(d, false));
    }
    // 이번달
    for (let d = 1; d <= daysInMonth; d++) {
      arr.push(buildCell(new Date(y, m, d), true));
    }
    // 뒤쪽 다음달
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
    const disabled = isOutOfRange(d);

    return {
      key: toISO(d),
      date: d,
      inMonth,
      isToday,
      isSelected,
      disabled,
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

  /** 동작 */
  function prevMonth() {
    if (viewMonth.value === 0) {
      viewMonth.value = 11;
      viewYear.value--;
    } else viewMonth.value--;
  }
  function nextMonth() {
    if (viewMonth.value === 11) {
      viewMonth.value = 0;
      viewYear.value++;
    } else viewMonth.value++;
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

  /** 바깥 클릭 닫기 */
  function onDocClick(e: MouseEvent) {
    if (props.inline) return;
    const el = rootRef.value;
    if (el && !el.contains(e.target as Node)) open.value = false;
  }
  onMounted(() => document.addEventListener('mousedown', onDocClick));
  onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick));

  /** 입력 표시값 */
  const displayValue = computed(() => toISO(selected.value));
</script>
