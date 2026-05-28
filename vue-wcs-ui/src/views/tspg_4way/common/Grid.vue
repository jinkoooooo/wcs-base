<template>
  <BasicGrid
    ref="innerRef"
    :data="data"
    :columns="columns"
    :options="innerOptions"
    :theme="theme"
    :rowHeaders="rowHeaders"
    :columnOptions="columnOptions"
    :limit="limit"
    :pageSizeOptions="pageSizeOptions"
    :fetchHandler="fetchHandler"
    :showButtons="showButtons"
    :showPagination="showPagination"
    :frozenCount="frozenCount"
    @click="(e: any) => emit('click', e)"
    @dblclick="(e: any) => emit('dblclick', e)"
    @check="(e: any) => emit('check', e)"
    @uncheck="(e: any) => emit('uncheck', e)"
    @gridFetched="(e: any) => emit('gridFetched', e)"
  >
    <slot></slot>
  </BasicGrid>
</template>

<script lang="ts" setup>
import { Grid as BasicGrid } from '/@/components/Grid/index';
import { computed, ref } from 'vue';

const props = defineProps({
  data: { type: [Array, Object], default: () => [] },
  columns: { type: Array, default: () => [] },
  options: { type: Object, default: () => ({}) },
  theme: { type: [String, Object], default: 'default' },
  rowHeaders: { type: Array, default: () => ['rowNum', 'checkbox'] },
  columnOptions: { type: Object, default: () => ({ resizable: true }) },
  limit: { type: Number, default: 50 },
  pageSizeOptions: { type: Array, default: () => [10, 20, 50, 100, 500] },
  fetchHandler: { type: Function },
  showButtons: { type: Boolean, default: true },
  showPagination: { type: Boolean, default: true },
  frozenCount: { type: Number, default: 2 },

  // ★ Summary — TUI Grid 네이티브 summary 옵션으로만 구성
  showSummary: { type: Boolean, default: false },
  summaryColumns: { type: Array as () => string[], default: () => [] },
  summaryLabelColumn: { type: String, default: '' },
  summaryCountColumn: { type: String, default: '' },
  summaryHeight: { type: Number, default: 32 },
  summaryFormatter: { type: Function as any, default: null },
  summaryColumnOverrides: {
    type: Object as () => Record<string, (v: any) => string>,
    default: () => ({}),
  },
});

const emit = defineEmits(['click', 'dblclick', 'check', 'uncheck', 'gridFetched']);
const innerRef = ref<any>();

function defaultFormatter(v: any): string {
  if (v == null || isNaN(v)) return '0';
  return Number(v).toLocaleString();
}

/**
 * ★ 핵심: summary 를 TUI Grid 네이티브 columnContent.template 으로만 구성
 *   - 라벨("합계"): template 문자열 반환
 *   - 건수: template 에 전달되는 { cnt } 사용
 *   - 합계: template 에 전달되는 { sum } 사용
 *   DOM 조작 일체 없음 → keep-alive 에 완전 무영향
 */
const innerOptions = computed(() => {
  const opts: Record<string, any> = { ...(props.options || {}) };
  const cols: any[] = (props.columns as any[]) || [];
  if (!props.showSummary || cols.length === 0) return opts;

  const formatter = props.summaryFormatter || defaultFormatter;
  const labelCol = props.summaryLabelColumn || cols[0]?.name;
  const countCol = props.summaryCountColumn || cols[1]?.name;

  const sumTargets: string[] = props.summaryColumns.length
    ? [...props.summaryColumns]
    : cols
      .filter((c) => c.align === 'right' || c.align === 'center-right')
      .map((c) => c.name);

  const columnContent: Record<string, any> = {};

  if (labelCol) {
    // columnContent[labelCol] = {
    //   template: () => '<span style="font-weight:bold;">합계</span>',
    // };
  }
  if (countCol && countCol !== labelCol) {
    columnContent[countCol] = {
      template: ({ cnt }: any) =>
        `<span style="font-weight:bold;">${cnt ?? 0}건</span>`,
    };
  }
  sumTargets.forEach((name) => {
    if (name === labelCol || name === countCol) return;
    if (props.summaryColumnOverrides[name]) {
      columnContent[name] = { template: props.summaryColumnOverrides[name] };
    } else {
      columnContent[name] = { template: ({ sum }: any) => formatter(sum ?? 0) };
    }
  });

  opts.summary = {
    height: props.summaryHeight,
    position: 'bottom',
    columnContent,
  };
  return opts;
});

// BasicGrid 메서드 전체 노출
defineExpose({
  createInstance: () => innerRef.value?.createInstance(),
  destroy: () => innerRef.value?.destroy(),
  setColumns: (c: any) => innerRef.value?.setColumns(c),
  setFrozenColumn: (n: number) => innerRef.value?.setFrozenColumn(n),
  refreshLayout: () => innerRef.value?.refreshLayout(),
  fetch: () => innerRef.value?.fetch(),
  addRow: (row?: any) => innerRef.value?.addRow(row),
  getCURows: () => innerRef.value?.getCURows(),
  getCheckedRows: () => innerRef.value?.getCheckedRows(),
  getSelectedRowsToDelete: () => innerRef.value?.getSelectedRowsToDelete(),
  getData: () => innerRef.value?.getData(),
  getRow: (k: any) => innerRef.value?.getRow(k),
  getValue: (r: any, c: any) => innerRef.value?.getValue(r, c),
  setValue: (r: any, c: any, v: any) => innerRef.value?.setValue(r, c, v),
  setRow: (r: any, v: any) => innerRef.value?.setRow(r, v),
  exportExcel: (n: string) => innerRef.value?.exportExcel(n),
  uncheckAll: () => innerRef.value?.uncheckAll(),
  finishEditing: () => innerRef.value?.finishEditing(),
  getModifiedRows: () => innerRef.value?.getModifiedRows(),
  disable: () => innerRef.value?.disable(),
  disableCell: (r: any, c: any) => innerRef.value?.disableCell(r, c),
  disableColumn: (c: any) => innerRef.value?.disableColumn(c),
  disableRow: (r: any, w: any) => innerRef.value?.disableRow(r, w),
  get $el() { return innerRef.value?.$el; },
});
</script>
