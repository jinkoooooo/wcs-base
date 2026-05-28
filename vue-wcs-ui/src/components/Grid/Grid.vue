<template>
<div class="tui-grid-root flex flex-col h-full w-full min-h-0 overflow-hidden">
  <div ref="gridLayout" class="flex flex-1 min-h-0">
    <div ref="tuiGrid" class="flex-1 "></div>
  </div>
  <div class="flex py-2 items-right justify-end paginator" v-if="showPagination">
    <Pagination
      v-model:current="current"
      v-model:page-size="pageSize"
      :pageSizeOptions="pageSizeOptions"
      :total="total"
      :show-total="(total, range) => `${range[0]}-${range[1]} of ${total} items`"
      @change="pageChanged"
    />
  </div>
  <div class="flex py-2 items-right justify-end" v-if="showButtons">
    <slot></slot>
  </div>
</div>
</template>
<script lang="ts">
  import Grid, { GridEventName } from 'tui-grid';
  import type { Nullable } from '@vben/types';
  import { defineComponent, ref, onMounted, onUnmounted, nextTick, PropType } from 'vue';
  import './css/tui-pagination.css';
  import { isFunction } from '/@/utils/is';
  import { Pagination } from 'ant-design-vue';
  import './css/grid.css';
  import { cloneDeep } from 'lodash-es';
  import registerRenderer from './renderer/registry';
  import { useFetchStore } from "/@/store/modules/fetchStore";

  const presetTheme = ['default', 'striped', 'clean'];

  const presetLanguage = ['en', 'ko'];

  const fetchStore = useFetchStore();

  export default defineComponent({
    name: 'TuiGrid',
    components: { Pagination },
    props: {
      data: {
        type: [Array, Object],
        required: true,
        default() {
          return [];
        },
      },
      rowHeaders: { type: Array },
      columnOptions: { type: Object },
      columns: {
        type: Array,
        required: true,
        default() {
          return [];
        },
      },
      options: {
        type: Object,
        default() {
          return {};
        },
      },
      // @deprecated. You should use it via importing tui-grid directly.
      theme: {
        type: [String, Object],
        validator(value) {
          let result = false;
          if (typeof value === 'string') {
            result = presetTheme.indexOf(value) > -1;
          } else {
            //client could send a JSON value like {"hasOwnProperty": 1} and cause the server to crash.
            result =
              Object.prototype.hasOwnProperty.call(value, 'name') &&
              Object.prototype.hasOwnProperty.call(value, 'value');
          }

          return result;
        },
      },
      // @deprecated. You should use it via importing tui-grid directly.
      language: {
        type: [String, Object],
        validator(value) {
          let result = false;
          if (typeof value === 'string') {
            result = presetLanguage.indexOf(value) > -1;
          } else {
            //client could send a JSON value like {"hasOwnProperty": 1} and cause the server to crash.
            result =
              Object.prototype.hasOwnProperty.call(value, 'name') &&
              Object.prototype.hasOwnProperty.call(value, 'value');
          }

          return result;
        },
      },
      limit: {
        type: Number,
      },
      fetchHandler: {
        type: Function,
      },
      pageSizeOptions: {
        type: Array as PropType<(string | number)[]>,
        default() {
          return [10, 20, 30, 50];
        },
      },
      showPagination: {
        type: Boolean,
        default() {
          return true;
        },
      },
      showButtons: {
        type: Boolean,
        default() {
          return true;
        },
      },
      frozenCount: {
        type: Number,
        default() {
          return 2;
        },
      },
    },
    // emits: [
    //   'click',
    //   'dblclick',
    //   'mousedown',
    //   'mouseover',
    //   'mouseout',
    //   'focusChange',
    //   'columnResize',
    //   'check',
    //   'uncheck',
    //   'checkAll',
    //   'uncheckAll',
    //   'selection',
    //   'editingStart',
    //   'editingFinish',
    //   'sort',
    //   'filter',
    //   'scrollEnd',
    //   'beforeRequest',
    //   'response',
    //   'successResponse',
    //   'failResponse',
    //   'errorResponse',
    //   'expand',
    //   'collapse',
    //   'beforeSort',
    //   'afterSort',
    //   'beforeUnsort',
    //   'afterUnsort',
    //   'beforeFilter',
    //   'afterFilter',
    //   'beforeUnfilter',
    //   'afterUnfilter',
    //   'beforePageMove',
    //   'afterPageMove',
    //   'beforeChange',
    //   'afterChange',
    //   'dragStart',
    //   'drag',
    //   'drop',
    //   'keydown',
    //   'beforeExport',
    //   'afterExport',
    // ],
    setup(props, { emit, attrs }) {
      const tuiGrid = ref(null);
      const gridInstance = ref<Nullable<Grid>>(null);
      const current = ref(1);
      const pageSize = ref(props.limit);
      const total = ref(0);
      const sorters = ref([]);
      const sortState = ref(null);
      const changes = ref(new Map());
      const originals = ref(new Map());

      onMounted(async () => {
        await nextTick();
        if (props.columns && props.columns.length > 0) createInstance();
      });

      onUnmounted(() => {
        removeEventListeners();
        gridInstance.value?.destroy();
        gridInstance.value = null;
      });

      function addEventListeners() {
        for (let eventName of Object.keys(attrs)) {
          if (isFunction(attrs[eventName])) {
            eventName = eventName.toLowerCase().slice(2, eventName.length);
            gridInstance.value?.on(eventName as GridEventName, (...args) => {
              emit(eventName, ...args);
            });
          }
        }
        gridInstance.value?.on('afterSort', async (e) => {
          if (fetchStore.isUpdatingRows) return;
          sorters.value = e.sortState.columns.map((column) => {
            return {
              field: column.columnName,
              ascending: column.ascending,
            };
          });
          sortState.value = {
            columnName: e.columnName,
            ascending: e.sortState.columns.find((column) => column.columnName === e.columnName)
              .ascending,
            multiple: true,
          };
          await fetch();
        });
        gridInstance.value?.on('afterUnsort', async (e) => {
          if (
            !e.sortState.columns
              .filter((column) => column.columnName !== 'sortKey')
              .find((column) => column.columnName === e.columnName)
          ) {
            sorters.value = [];
            sortState.value = null;
          } else {
            sorters.value = e.sortState.columns.map((column) => {
              return {
                field: column.columnName,
                ascending: column.ascending,
              };
            });
            sortState.value = {
              columnName: e.columnName,
              ascending: e.sortState.columns.find((column) => column.columnName === e.columnName)
                .ascending,
              multiple: true,
            };
          }
          await fetch();
        });
        gridInstance.value?.on('beforeChange', (e) => {});
        gridInstance.value?.on('afterChange', (e) => {
          for (let i = 0; i < e.changes.length; i++) {
            const rowKey = e.changes[i].rowKey;
            const columnName = e.changes[i].columnName;
            const value = e.changes[i].value;
            const valueType = typeof value;
            const originValue = (() => {
              if (valueType === 'string') {
                return (
                  originals.value.has(rowKey) ? originals.value.get(rowKey)[columnName] ?? '' : ''
                ).toString();
              }
              if (valueType === 'boolean') {
                return Boolean(
                  originals.value.has(rowKey)
                    ? originals.value.get(rowKey)[columnName] ?? false
                    : false,
                );
              }
            })();
            if (value !== originValue) {
              gridInstance.value.addCellClassName(rowKey, columnName, 'updated');
              if (changes.value.has(rowKey)) {
                changes.value.set(rowKey, {
                  ...changes.value.get(rowKey),
                  [columnName]: value,
                });
              } else {
                let obj = gridInstance.value.store.data.viewData.find(
                  (data) => data.rowKey === rowKey,
                );
                // id가 없는 레코드는 생성으로 간주한다
                if (obj?.valueMap?.id?.value) {
                  changes.value.set(rowKey, {
                    cud_flag_: 'u',
                    id: obj.valueMap.id.value,
                    [columnName]: value,
                  });
                } else {
                  let rowData = e.instance.getRow(rowKey);

                  delete rowData.rowKey;
                  delete rowData.rowSpanMap;
                  delete rowData.sortKey;
                  delete rowData.uniqueKey;
                  delete rowData._attributes;
                  delete rowData._disabledPriority;
                  delete rowData._relationListItemMap;

                  changes.value.set(rowKey, {
                    cud_flag_: 'c',
                    ...rowData,
                    [columnName]: value,
                  });
                }
              }
            } else {
              let obj = changes.value.get(rowKey);
              if (obj) {
                delete obj[columnName];
                gridInstance.value.removeCellClassName(rowKey, columnName, 'updated');
                let keyCount = 0;
                Object.keys(obj).forEach(() => keyCount++);
                if (
                  (changes.value.get(rowKey).cud_flag_ === 'u' && keyCount === 2) ||
                  (changes.value.get(rowKey).cud_flag_ === 'c' && keyCount === 1)
                ) {
                  changes.value.delete(rowKey);
                }
              }
            }
          }
        });
      }

      function removeEventListeners() {
        for (let eventName of Object.keys(attrs)) {
          if (isFunction(attrs[eventName])) {
            eventName = eventName.toLowerCase().slice(2, eventName.length);
            gridInstance.value?.off(eventName, (...args) => emit(eventName, ...args));
          }
        }
        gridInstance.value?.off('afterSort', (e) => {});
        gridInstance.value?.off('afterUnsort', (e) => {});
        gridInstance.value?.off('beforeChange', (e) => {});
        gridInstance.value?.off('afterChange', (e) => {});
      }

      // @deprecated. You should use it via importing tui-grid directly.
      function applyTheme() {
        if (props.theme) {
          if (typeof props.theme === 'string') {
            Grid.applyTheme(props.theme);
          } else {
            Grid.applyTheme(props.theme.name, props.theme.value);
          }
        }
      }
      // @deprecated. You should use it via importing tui-grid directly.
      function setLanguage() {
        if (props.language) {
          if (typeof props.language === 'string') {
            Grid.setLanguage(props.language);
          } else {
            Grid.setLanguage(props.language.name, props.language.value);
          }
        }
      }

      function getRootElement() {
        return tuiGrid.value;
      }

      function invoke(methodName, ...args) {
        return typeof gridInstance.value[methodName] === 'function'
          ? gridInstance.value[methodName](...args)
          : null;
      }

      async function pageChanged() {
        await fetch();
      }

      async function fetch() {
        const { records, total: tot } = await props.fetchHandler.call(null, {
          page: current.value,
          limit: pageSize.value,
          ...(sorters.value.length === 0 ? {} : { sorters: sorters.value }),
        });
        total.value = tot ? tot : records.length;
        gridInstance.value?.resetData(cloneDeep(records), {
          ...(sortState.value
            ? {
                sortState: sortState.value,
              }
            : {}),
        });
        originals.value.clear();
        for (let i = 0; i < cloneDeep(records).length; i++) {
          originals.value.set(i, cloneDeep(records)[i]);
        }
        emit("gridFetched", records);
        reset();
      }

      function reset() {
        changes.value = new Map();
      }

      function getColumns() {
        gridInstance.value.getColumns();
      }

      function setColumns(columns) {
        registerRenderer(columns);
        invoke('setColumns', columns);
      }

      function addRow(row?: Record<string, any>) {
        gridInstance.value.appendRow(row || {}, { at: 0 });
      }

      function getModifiedRows() {
        return gridInstance.value.getModifiedRows();
      }

      function removeCheckedRows() {
        return gridInstance.value.removeCheckedRows(false);
      }

      function uncheckAll() {
        return gridInstance.value.uncheckAll();
      }

      function getCheckedRows() {
        return gridInstance.value.getCheckedRows();
      }

      function exportExcel(downName) {
        let parame = {
          fileName: downName,
          useFormattedValue: true,
        };
        return gridInstance.value.export('xlsx', parame);
      }

      function getCURows() {
        // 편집 상태인 셀에서 바로 저장을 누를 경우 해당 셀은 반영이 안되므로 강제로 finish한다
        gridInstance.value.finishEditing();
        return [...changes.value.values()].map((item) => ({ ...item }));
      }

      function getData() {
        gridInstance.value.finishEditing();
        return gridInstance.value.getData();
      }

      function getRow(rowKey) {
        return gridInstance.value.getRow(rowKey);
      }

      function getSelectedRowsToDelete() {
        const checkedRows = gridInstance.value.getCheckedRows();
        return checkedRows.filter((row) => row.id).map((row) => ({ id: row.id, cud_flag_: 'd' }));
      }

      function createInstance() {
        props.columns.forEach((column) => (column.comparator = () => false));
        registerRenderer(props.columns);
        const options = Object.assign(props.options || {}, attrs, {
          el: tuiGrid.value,
          data: props.data,
          columns: props.columns,
          bodyHeight: 'fitToParent',
          useClientSort: true,
          scrollX: true,
          scrollY: true,
          columnOptions: {
            resizable: true,
            frozenCount: props.frozenCount ? 2 : props.frozenCount,
          },
        });
        gridInstance.value = new Grid(options);
        addEventListeners();
        applyTheme();
        setLanguage();
        return gridInstance;
      }

      function destroy() {
        gridInstance.value?.destroy();
      }

      function refreshLayout() {
        gridInstance.value?.refreshLayout();
      }

      function getValue(rowKey, columnName) {
        gridInstance.value?.getValue(rowKey, columnName);
      }

      function setValue(rowKey, columnName, value) {
        gridInstance.value?.setValue(rowKey, columnName, value);
      }

      function setRow(rowKey, row) {
        gridInstance.value?.setRow(rowKey, row);
      }

      function setFrozenColumn(count) {
        gridInstance.value?.setFrozenColumnCount(count);
      }

      function disable() {
        gridInstance.value?.disable();
      }

      function disableCell(rowKey, columnName) {
        gridInstance.value?.disableCell(rowKey, columnName);
      }

      function disableColumn(columnName) {
        gridInstance.value?.disableColumn(columnName);
      }

      function disableRow(rowKey, withCheckbox) {
        gridInstance.value?.disableRow(rowKey, withCheckbox);
      }

      function finishEditing() {
        gridInstance.value.finishEditing();
      }

      return {
        getRootElement,
        fetch,
        getColumns,
        setColumns,
        addRow,
        getModifiedRows,
        removeCheckedRows,
        getCheckedRows,
        getCURows,
        getSelectedRowsToDelete,
        pageChanged,
        current,
        pageSize,
        total,
        tuiGrid,
        createInstance,
        destroy,
        refreshLayout,
        exportExcel,
        uncheckAll,
        getData,
        getRow,
        getValue,
        setValue,
        setRow,
        setFrozenColumn,
        disable,
        disableCell,
        disableColumn,
        disableRow,
        finishEditing
      };
    },
  });
</script>
<style lang="less" scoped>
  @prefix-cls: ~'@{namespace}-tui-grid';
  .@{prefix-cls} {
    display: flex;
    flex-direction: column;
    // overflow: hidden;
    height: calc(100vh - 250px);

    &-grid {
      // overflow: hidden;
      flex: 1 1 0%;
    }
  }
</style>
