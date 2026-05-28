<template>
  <!-- 바깥 래퍼: 높이만 관리 -->
  <div
    class="w-full rounded-2xl ring-1 ring-slate-200 bg-white shadow-sm overflow-hidden flex flex-col"
    :style="outerStyle"
  >
    <!-- ✅ 헤더 + 바디를 한 번에 스크롤 -->
    <div class="flex-1 overflow-auto">
      <!-- 내용이 grid/폭 때문에 넘치면 가로 스크롤 생김 -->
      <div class="min-w-max">
        <!-- 헤더 -->
        <div
          class="grid bg-slate-50 border-b border-slate-200 text-[12px] font-medium text-slate-600 sticky top-0 z-10"
          :style="gridStyle"
        >
          <div class="px-3 py-2">#</div>

          <div
            v-for="(c, colIndex) in columns"
            :key="c.field"
            class="relative flex items-center px-3 py-2 bg-slate-50"
          >
            <span class="truncate">
              {{ c.label }}
            </span>

            <!-- ✅ 리사이즈 핸들 -->
            <span
              class="absolute right-0 top-0 h-full w-1 cursor-col-resize select-none"
              @mousedown.prevent="startResize(colIndex, $event)"
            ></span>
          </div>
        </div>

        <!-- 바디 -->
        <template v-if="!loading && rows.length">
          <div
            v-for="(r, idx) in rows"
            :key="r[rowKeyName] ?? idx"
            class="grid items-center border-b border-slate-100 hover:bg-slate-50 cursor-pointer text-sm"
            :class="selectedKey === (r[rowKeyName] ?? idx) ? 'bg-indigo-50/60' : ''"
            :style="gridStyle"
            @click="onSelect(r)"
          >
            <!-- 단일 선택 (라디오 느낌) -->
            <div class="px-3 py-2">
              <span
                class="inline-block w-3 h-3 rounded-full ring-1 ring-slate-300 align-middle"
                :class="
                  selectedKey === (r[rowKeyName] ?? idx)
                    ? 'bg-indigo-500 ring-indigo-500'
                    : 'bg-white'
                "
              ></span>
            </div>

            <!-- 셀 -->
            <div
              v-for="c in columns"
              :key="c.field"
              :class="['px-3 py-2 truncate tabular-nums', getCellClass(c, r)]"
              :title="String(r[c.field] ?? '')"
            >
              {{ r[c.field] ?? '' }}
            </div>
          </div>
        </template>

        <div v-else-if="loading" class="p-8 text-center text-slate-500 text-sm"> 불러오는 중… </div>
        <div v-else class="p-8 text-center text-slate-500 text-sm"> 데이터가 없습니다. </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, ref, watch } from 'vue';

  /** 셀 클래스 함수 타입 */
  type CellClassFn = (value: any, row: Record<string, any>) => string | string[];

  type Col = {
    field: string;
    label: string;
    width?: number;
    /** 셀에 줄 클래스 (문자열 or 함수) */
    cellClass?: string | CellClassFn;
  };

  const props = defineProps<{
    columns: Col[];
    rows: Record<string, any>[];
    rowKey?: string;
    selectedKey?: string | number | null;
    loading?: boolean;
    /** 전체 그리드 높이 (예: '50vh', 420, '420px'). 기본값 50vh */
    height?: string | number;
  }>();

  const emit = defineEmits<{
    (e: 'update:selectedKey', v: string | number | null): void;
    (e: 'rowSelected', row: Record<string, any>): void;
  }>();

  const rowKeyName = computed(() => props.rowKey ?? 'id');

  /** ✅ 그리드 전체 높이 */
  const outerStyle = computed(() => {
    const h = props.height ?? '50vh';
    return { height: typeof h === 'number' ? `${h}px` : String(h) };
  });

  /** ✅ 컬럼 폭 상태 (리사이즈용) */
  const columnWidths = ref<number[]>([]);

  watch(
    () => props.columns,
    (cols) => {
      columnWidths.value = cols.map((c) => c.width ?? 150); // 기본 폭
    },
    { immediate: true, deep: true },
  );

  /** ✅ CSS Grid 템플릿 (라디오 48px + 각 컬럼 폭) */
  const gridStyle = computed(() => {
    const first = '48px';
    const cols = columnWidths.value.map((w) => `${w}px`);
    return { gridTemplateColumns: [first, ...cols].join(' ') };
  });

  /** ✅ 리사이즈 로직 */
  const resizing = ref(false);
  const resizeIndex = ref(-1);
  const startX = ref(0);
  const startWidth = ref(0);

  function startResize(index: number, e: MouseEvent) {
    resizing.value = true;
    resizeIndex.value = index;
    startX.value = e.clientX;
    startWidth.value = columnWidths.value[index];
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
  }

  function onMouseMove(e: MouseEvent) {
    if (!resizing.value) return;
    const dx = e.clientX - startX.value;
    const next = Math.max(80, startWidth.value + dx); // 최소 80px
    columnWidths.value.splice(resizeIndex.value, 1, next);
  }

  function onMouseUp() {
    if (!resizing.value) return;
    resizing.value = false;
    window.removeEventListener('mousemove', onMouseMove);
    window.removeEventListener('mouseup', onMouseUp);
  }

  onBeforeUnmount(() => {
    window.removeEventListener('mousemove', onMouseMove);
    window.removeEventListener('mouseup', onMouseUp);
  });

  /** ✅ 셀 클래스 계산 */
  function getCellClass(col: Col, row: Record<string, any>): string | string[] {
    if (!col.cellClass) return '';
    if (typeof col.cellClass === 'string') return col.cellClass;
    const value = row[col.field];
    return col.cellClass(value, row);
  }

  /** ✅ 행 선택 */
  function onSelect(row: Record<string, any>) {
    const key = row[rowKeyName.value];
    emit('update:selectedKey', key);
    emit('rowSelected', row);
  }
</script>

<style scoped>
  .tabular-nums {
    font-variant-numeric: tabular-nums;
  }

  ::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }
  ::-webkit-scrollbar-thumb {
    background-color: #cbd5e1;
    border-radius: 4px;
  }
  ::-webkit-scrollbar-thumb:hover {
    background-color: #94a3b8;
  }
</style>
