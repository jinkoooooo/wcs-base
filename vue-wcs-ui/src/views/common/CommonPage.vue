<template>
  <PageWrapper class="overflow-hidden max-full h-full">
    <div class="inline-flex flex-col flex-1">

      <slot name="customDesc"></slot>

      <div class="form-container">
        <BasicForm
            ref="formRef"
            @register="formRegister"
            autoSubmitOnEnter
            @toggle-advanced="handleAdvanced"
            :hidden="!showSearchForm"
        />
      </div>
      <div class="flex flex-col flex-1">
        <BasicGrid
            ref="gridRef"
            @click="onGridClicked"
            @check="onGridChecked"
            @uncheck="onGridUnchecked"
            @dblclick="onGridDbClicked"
            @gridFetched="gridFetched"
            :data="gridProps.data"
            :columns="gridProps.columns"
            :options="gridProps.options"
            :theme="gridProps.myTheme"
            :rowHeaders="gridProps.rowHeaders"
            :columnOptions="gridProps.columnOptions"
            :limit="limit"
            :pageSizeOptions="[10, 20, 50, 100, 500]"
            :fetchHandler="fetch"
            :showButtons="showButtons"
            :showPagination="showPagination"
            :frozenCount="frozen"
        >
          <div
              class="ant-form-item-control-input form-color flex space-x-2 justify-end"
              v-if="showButtons"
          >
            <!-- 메뉴 상세 버튼 -->
            <slot></slot>
          </div>
        </BasicGrid>
      </div>
    </div>
  </PageWrapper>
</template>
<script lang="ts" setup>
import 'tui-grid/dist/tui-grid.css';
import { Grid as BasicGrid } from '/@/components/Grid/index';
import { BasicForm, useForm } from '/@/components/Form/index';
import { useMessage } from '/@/hooks/web/useMessage';
import { PageWrapper } from '/@/components/Page';
import { nextTick, ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { getCommonGetListApi, getSearchList } from '/@/api/common/api';
import { getQueryFilters } from '/src/views/common/utils';
import { useI18n } from '/@/hooks/web/useI18n';
import { parseSearchFormFields, parseSortFields } from '/@/utils/metas/searchFormMeta';
import { parseGridColumns } from '/@/utils/metas/gridMeta';
import { parseButtons } from '/@/utils/metas/buttonMeta';
import { gridThemeCust } from '@/design/gridTheme';
/**
 * define area
 */
let props = defineProps({
  limit: { type: Number, default: 50 },
  menuName: { type: String },
  metaUrl: { type: String },
  menuMetaProp: { type: [String, Number] },
  metas: {
    type: Object,
    // default: function () {
    //   return { buttons: [], columns: [], menu: {}, menu_params: [] };
    // },
  },
  rowHeaders: {
    type: Array,
    default: () => {
      return ['rowNum', 'checkbox'];
    },
  },
  optionCheck: {
    type: Array,
    default: () => {
      return ['checkbox'];
    },
  },
  resourceId: { type: String },
  actionColumns: {
    type: Array,
    default: () => {
      return [];
    },
  },
  fetchHandler: {
    type: Function,
  },
  showSearchForm: {
    type: Boolean,
    default() {
      return true;
    },
  },
  baseColProps: {
    type: Object,
    default: () => {
      // return { xxl: 4, lg: 6, md: 12, sm: 24 };
      return { xxl: 7, lg: 7, md: 7, sm: 28 };
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
});
const emit = defineEmits(['gridClicked', 'gridChecked', 'gridDbClicked', 'gridUnChecked', 'gridFetched']);
function onGridDbClicked(e) {
  // 그리드 이벤트가 아니면 무시. DOM 더블클릭 시 이벤트 객체가 없음
  if (!e || !e.instance)
    return;

  if (e.targetType != 'columnHeader') {
    const clickedRow = e.instance.getRow(e.rowKey);
    let clicked = clickedRow;
    if (clicked) {
      clicked.event = e;
      clicked.columnName = e.columnName ?? '';
    }
    emit('gridDbClicked', clicked);
  }
}
function onGridClicked(e) {
  // 그리드 이벤트가 아니면 무시. DOM 클릭 시 이벤트 객체가 없음
  if (!e || !e.instance)
    return;

  if (e.targetType != 'columnHeader') {
    const clickedRow = e.instance.getRow(e.rowKey);
    let clicked = {...clickedRow};
    if (clicked) {
      clicked.event = e;
      clicked.columnName = e.columnName ?? '';
    }
    emit('gridClicked', clicked);
  }
}

function onGridChecked(e) {
  if (e.targetType != 'columnHeader') {
    const checkedRows = e.instance.getCheckedRows();
    let checkeds = checkedRows;
    emit('gridChecked', checkeds);
  }
}

function onGridUnchecked(e) {
  if (e.targetType != 'columnHeader') {
    const checkedRows = e.instance.getCheckedRows();
    let checkeds = checkedRows;
    emit('gridUnChecked', checkeds);
  }
}

async function gridFetched(e) {
  await nextTick();
  setTimeout(() => {
    gridRef.value?.refreshLayout?.();
  }, 100);

  emit('gridFetched', e);
}

/**
 * main logic
 */
const { t } = useI18n();
const formRef = ref();
const gridRef = ref();
const gridColumnRef = ref()
const resourceUrl = ref();
const resourceName = ref();
const gridSaveUrl = ref();
const metaRouting = ref();
const route = useRoute();
const buttons = ref();
let searchProps = [] as any;
let defaultSorters = [] as any;
let frozen = 2;
const gridProps = {
  rowHeaders: props.rowHeaders,
  columnOptions: {
    resizable: true,
  },
  columns: [] as any[],
  data: [] as any[],
  myTheme: 'default', // gridThemeCust,
  options: {
    rowHeaders: props.optionCheck,
  },
};

const { createMessage } = useMessage();
const [formRegister, { validate, setProps, getFieldsValue, resetSchema }] = useForm({
  labelWidth: 100,
  labelAlign: 'right',
  baseColProps: props.baseColProps,
  actionColOptions: {
    span: 24,
  },
  alwaysShowLines: 1,
  compact: true,
  showAdvancedButton: true,
  submitButtonOptions: {
    text: t('button.show'),
  },
  submitFunc: async () => {
    gridRef.value.current = 1;
    // 조회버튼을 클릭했을 때 컬럼 정보도 함께 갱신
    // 공통코드가 수정되었을 수도 있기 때문
    // gridRef.value.fetch();, getMenuMetas() 호출하면서 fetch()도 함께 됨
    getMenuMetas(true)
  },
});

onMounted(async () => {
  await getMenuMetas();
});

async function getMenuMetas(isClicked = false) {
  let metas: any;
  metas = props.metas;
  if (!metas) {
    if (props.metaUrl) {
      metas = await getCommonGetListApi(props.metaUrl, null);
    } else {
      metas = await getCommonGetListApi(
          `/menus/${String(props.menuName ? props.menuName : route.name)}/named_meta`,
          null,
      );
    }
  }
  metas = props.menuMetaProp ? metas[props.menuMetaProp] : metas;

  searchProps = parseSearchFormFields(metas.columns);
  defaultSorters = parseSortFields(metas.columns);
  frozen = metas.menu.fixed_columns ? metas.menu.fixed_columns : 0;

  let searchUrl = metas.menu.resource_url ? metas.menu.resource_url : metas.menu.search_url;
  resourceUrl.value = props.resourceId ? searchUrl.replace(':id', props.resourceId) : searchUrl;
  resourceName.value = metas.menu.resource_name;
  metaRouting.value = metas.menu.routing;

  let saveUrl = metas.menu.grid_save_url
      ? '/' + metas.menu.grid_save_url
      : '/' + metas.menu.save_url;
  gridSaveUrl.value = props.resourceId ? saveUrl.replace(':id', props.resourceId) : saveUrl;

  searchProps.splice(3, 0, { field: '', component: 'Divider', label: '' });
  resetSchema(searchProps);
  formRef.value.handleToggleAdvanced = handleAdvanced;

  let columns = [...props.actionColumns, ...(await parseGridColumns(metas.columns))];

  columns.forEach((column) => {
    column.width = column.minWidth;
  });
  gridProps.columns = columns;

  //버튼 메타 정보
  buttons.value = await parseButtons(metas.buttons);

  nextTick(async () => {
    if(!isClicked){
      gridRef.value?.createInstance();
    }

    await gridRef.value?.setColumns(gridProps.columns);
    gridColumnRef.value = gridProps.columns;
    gridRef.value?.setFrozenColumn(frozen);
  });
}
function debounce(fnc) {
  var timer;
  return function () {
    if (timer) clearTimeout(timer);
    timer = setTimeout(fnc, 100);
  };
}
async function gridRedraw() {
  gridRef.value?.destroy();
  await nextTick();
  gridRef.value?.createInstance();
  await gridRef.value?.setColumns(gridProps.columns);
  gridRef.value?.setFrozenColumn(frozen);
}

async function handleAdvanced() {
  formRef.value.advanceState.isAdvanced = !formRef.value?.advanceState.isAdvanced;
  await nextTick(debounce(gridRedraw));
}

async function fetch({ page, limit, sorters = [] }) {
  try {
    setProps({
      submitButtonOptions: {
        loading: true,
      },
    });
    let result;
    if (sorters.length == 0) sorters = defaultSorters;
    if (props.fetchHandler && typeof props.fetchHandler == 'function') {
      result = await props.fetchHandler.call(null, page, limit, sorters, searchProps);
    } else {
      result = await fetcher(page, limit, sorters, searchProps);
    }

    const processedData = result.records.map((row) => ({
      ...row,
      _attributes: {
        className: {
          row: row.deleted === true ? ['row-deleted'] : []
        }
      }
    }));
    result.records = processedData;

    // createMessage.success('조회완료!');
    return result;
  } finally {
    setProps({
      submitButtonOptions: {
        loading: false,
      },
    });
  }
}

async function fetcher(page, limit, sorters, searchProps) {
  await validate();
  const fields = getFieldsValue();
  const queryFilters = await getQueryFilters(fields, searchProps);
  let params = [
    {
      name: 'query',
      value: JSON.stringify(queryFilters),
    },
    {
      name: 'sort',
      value: JSON.stringify(sorters),
    },
    {
      name: 'page',
      value: page,
    },
    {
      name: 'limit',
      value: limit,
    },
  ];

  let requestParams = {};
  params.forEach((item) => {
    requestParams[item['name']] = item['value'];
  });

  if (!resourceUrl.value){
    return {total: 0, records: []};
  }

  const response = await getSearchList(resourceUrl.value, requestParams);
  if (Array.isArray(response)) {
    return {
      total: response.length,
      records: response,
    };
  } else {
    return {
      total: response.total,
      records: response.items,
    };
  }
}

/*
 * 엑셀 다운로드
 */
async function downExcel() {
  gridRef.value.exportExcel(route.name);
}

async function getSearchProps() {
  return searchProps;
}

defineExpose({
  grid: gridRef,
  columns: gridColumnRef,
  form: formRef,
  getFormFields: getFieldsValue,
  formValidate: validate,
  getMenuMetas,
  searchProps,
  resourceUrl,
  gridSaveUrl,
  ...props,
  downExcel,
  buttons,
  getSearchProps,
  resourceName,
  metaRouting
});
</script>
<style lang="less" scoped>
.flex-1 {
  flex-shrink: 1;
  flex-basis: auto;
}
.form-container {
  .ant-form {
    width: 100%;
    margin-bottom: 16px;
    padding: 12px 10px 6px;
    border-radius: 4px;
    background-color: @component-background;
  }
}
.max-full {
  max-height: calc(100vh - 76px);
}
</style>
