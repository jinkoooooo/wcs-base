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
        >
          <!-- ★ resource-popup 필드 — DOM 조작 없이 BasicForm 네이티브 slot 으로 렌더 -->
          <template v-for="p in popupFields" :key="p.field" #[p.slotName]="{ model, field }">
            <a-input
              :value="model[field]"
              allow-clear
              :placeholder="p.label"
              @update:value="(v: any) => (model[field] = v)"
              @press-enter="() => formRef?.submitForm?.()"
            >
              <template #suffix>
                <SearchOutlined
                  :style="{ cursor: 'pointer', color: '#1890ff', fontSize: '16px' }"
                  @click="onPopupIconClick(p)"
                />
              </template>
            </a-input>
          </template>
        </BasicForm>
      </div>

      <div class="flex flex-col flex-1">
        <Grid
          ref="gridRef"
          @click="onGridClicked"
          @check="onGridChecked"
          @uncheck="onGridUnchecked"
          @dblclick="onGridDbClicked"
          @grid-fetched="gridFetched"
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
          :showSummary="showSummary"
          :summaryColumns="summaryColumns"
          :summaryLabelColumn="summaryLabelColumn"
          :summaryCountColumn="summaryCountColumn"
          :summaryHeight="summaryHeight"
          :summaryFormatter="summaryFormatter"
          :summaryColumnOverrides="summaryColumnOverrides"
        >
          <div
            class="ant-form-item-control-input form-color flex space-x-2 justify-end"
            v-if="showButtons"
          >
            <slot></slot>
          </div>
        </Grid>
      </div>
    </div>

    <!-- ★ 메타 기반 동적 팝업 — ref_name/popup_component 에 해당하는 *Popup.vue 를 자동 렌더 -->
    <component
      v-if="popupComponent"
      :is="popupComponent"
      :key="popupKey"
      @ready="onPopupReady"
      @select="onPopupSelect"
    />
  </PageWrapper>
</template>

<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { nextTick, reactive, ref, shallowRef, defineAsyncComponent, onMounted, onActivated } from 'vue';
  import { SearchOutlined } from '@ant-design/icons-vue';
  import Grid from './Grid.vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { PageWrapper } from '/@/components/Page';
  import { useRoute } from 'vue-router';
  import { getCommonGetListApi, getSearchList } from '/@/api/common/api';
  import { getFormattedFilters } from './utils';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { parseSearchFormFields, parseSortFields } from './searchFormMeta';
  import { parseGridColumns } from './gridMeta';
  import { parseButtons } from '/@/utils/metas/buttonMeta';

  const props = defineProps({
    limit: { type: Number, default: 50 },
    menuName: { type: String },
    metaUrl: { type: String },
    menuMetaProp: { type: [String, Number] },
    metas: { type: Object },
    rowHeaders: { type: Array, default: () => ['rowNum', 'checkbox'] },
    optionCheck: { type: Array, default: () => ['checkbox'] },
    resourceId: { type: String },
    actionColumns: { type: Array, default: () => [] },
    fetchHandler: { type: Function },
    showSearchForm: { type: Boolean, default: true },
    baseColProps: { type: Object, default: () => ({ xxl: 7, lg: 7, md: 7, sm: 28 }) },
    showPagination: { type: Boolean, default: true },
    showButtons: { type: Boolean, default: true },

    // Summary — Grid wrapper 로 전달
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
    gridBodyHeight: { type: [String, Number], default: undefined },
  });

  const emit = defineEmits([
    'gridClicked',
    'gridChecked',
    'gridDbClicked',
    'gridUnChecked',
    'gridFetched',
    'resourcePopupClick',
  ]);

  const { t } = useI18n();
  const formRef = ref();
  const gridRef = ref();
  const gridColumnRef = ref();
  const resourceUrl = ref();
  const resourceName = ref();
  const gridSaveUrl = ref();
  const metaRouting = ref();
  const route = useRoute();
  const buttons = ref();

  // ★ reactive 필수 — columns 변경이 Grid wrapper의 computed summary 에 전파되어야 함
  const gridProps = reactive({
    rowHeaders: props.rowHeaders,
    columnOptions: { resizable: true },
    columns: [] as any[],
    data: [] as any[],
    myTheme: 'default',
    options: { rowHeaders: props.optionCheck } as Record<string, any>,
  });

  // ★ resource-popup 필드 목록 — BasicForm slot 으로 렌더
  type PopupField = {
    field: string;
    slotName: string;
    target: string;
    component: string;
    label: string;
  };
  const popupFields = ref([] as PopupField[]);

  // ★ 메타 기반 동적 팝업 ──────────────────────────────
  //   - /views/**/*Popup.vue 파일을 자동 수집 → 메타의 popup_component/ref_name 로 매칭 렌더
  //   - 매칭 실패 시 기존처럼 resourcePopupClick 이벤트를 emit → 부모가 직접 처리 (하위 호환)
  const popupModules = import.meta.glob('/src/views/**/*Popup.vue');
  const popupComponent = shallowRef<any>(null);
  const popupOpen = ref(false);
  const activePopupField = ref<string>('');
  const popupComponentCache = new Map<string, any>();
  const popupKey = ref(0);
  const popupApi = shallowRef<{ openModal: () => void; closeModal: () => void } | null>(null);

  // 메타 입력값 → /src/views/**/*Popup.vue 매칭
  //   - 경로 입력 (권장) : '/tspg_4way/common/ItemSearchPopup'
  //       → '/src/views/tspg_4way/common/ItemSearchPopup.vue' 와 정확 매칭
  //   - 확장자(.vue) 포함 : '/tspg_4way/common/ItemSearchPopup.vue' 도 허용
  //   - 파일명만 입력     : 'ItemSearchPopup' — 이름으로 탐색 (같은 이름이 여러 개면 첫 매치)
  function resolvePopupComponent(nameOrPath: string): any | null {
    if (!nameOrPath) return null;
    if (popupComponentCache.has(nameOrPath)) return popupComponentCache.get(nameOrPath);

    const keys = Object.keys(popupModules);
    let matched: string | undefined;

    if (nameOrPath.includes('/')) {
      // 경로 입력 — /src/views 를 prefix 로 붙여서 정확 매칭
      const normalized = nameOrPath.startsWith('/') ? nameOrPath : `/${nameOrPath}`;
      const withExt = normalized.endsWith('.vue') ? normalized : `${normalized}.vue`;
      const fullPath = `/src/views${withExt}`;
      matched = keys.find((k) => k === fullPath);
      console.log(`fullPath: ${fullPath}, matched: ${matched}`);
    } else {
      // 파일명만 — 관례 유지 (하위 호환)
      matched = keys.find((k) => k.endsWith(`/${nameOrPath}.vue`));
    }

    if (!matched) return null;
    const asyncComp = defineAsyncComponent(popupModules[matched] as any);
    console.log(`Popup component resolved: ${nameOrPath} -> ${matched}`);
    popupComponentCache.set(nameOrPath, asyncComp);
    return asyncComp;
  }

  async function onPopupIconClick(p: PopupField) {
    activePopupField.value = p.field;
    const resolved = resolvePopupComponent(p.component || p.target);
    if (!resolved) {
      emit('resourcePopupClick', { field: p.field, target: p.target });
      return;
    }

    // 같은 팝업 재클릭 → 이미 mount, API 로 바로 열기
    if (popupComponent.value === resolved && popupApi.value) {
      popupApi.value.openModal();
      return;
    }

    // 최초 or 다른 팝업 전환 → re-mount
    //   mount 완료 시 자식이 @ready 로 API 를 보내주면 onPopupReady 에서 open
    popupApi.value = null;
    popupKey.value++;
    popupComponent.value = resolved;
  }

  // 자식이 mount 완료 시점에 제어 API 를 전달해줌 → 저장 + 즉시 open
  async function onPopupReady(api: { openModal: () => void; closeModal: () => void }) {
    console.log('api 객체:', JSON.stringify(api,null,2));
    console.log('api의 메서드들:', JSON.stringify(Object.keys(api || {}),null,2));
    console.log('openModal 타입:', typeof api?.openModal);

    popupApi.value = api;
    api.openModal();
  }

  function onPopupSelect(payload: any) {
    let value: any = '';
    if (payload != null) {
      if (typeof payload === 'object') {
        value = payload[activePopupField.value] ?? payload.code ?? payload.value ?? '';
      } else {
        value = payload;
      }
    }
    setFieldsValue({ [activePopupField.value]: value });
    popupApi.value?.closeModal?.();
  }

  let searchProps = [] as any;
  let defaultSorters = [] as any;
  let frozen = 2;

  function onGridDbClicked(e: any) {
    if (!e || !e.instance) return;
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
  function onGridClicked(e: any) {
    if (!e || !e.instance) return;
    if (e.targetType != 'columnHeader') {
      const clickedRow = e.instance.getRow(e.rowKey);
      let clicked = { ...clickedRow };
      if (clicked) {
        clicked.event = e;
        clicked.columnName = e.columnName ?? '';
      }
      emit('gridClicked', clicked);
    }
  }
  function onGridChecked(e: any) {
    if (e.targetType != 'columnHeader') emit('gridChecked', e.instance.getCheckedRows());
  }
  function onGridUnchecked(e: any) {
    if (e.targetType != 'columnHeader') emit('gridUnChecked', e.instance.getCheckedRows());
  }
  async function gridFetched(e: any) {
    await nextTick();
    setTimeout(() => gridRef.value?.refreshLayout?.(), 100);
    emit('gridFetched', e);
  }

  const { createMessage } = useMessage();
  const [formRegister, { validate, setProps, getFieldsValue, resetSchema, setFieldsValue }] = useForm({
    labelWidth: 100,
    labelAlign: 'right',
    baseColProps: props.baseColProps,
    actionColOptions: { span: 24 },
    alwaysShowLines: 1,
    compact: true,
    showAdvancedButton: true,
    submitButtonOptions: { text: t('button.show') },
    submitFunc: async () => {
      gridRef.value.current = 1;
      getMenuMetas(true);
    },
  });

  onMounted(async () => {
    await getMenuMetas();
  });

  onActivated(async () => {
    if (!gridRef.value) return;

    // 1. TUI Grid 특유의 화면 깨짐(백지 현상) 방지를 위해 레이아웃 강제 갱신
    gridRef.value.refreshLayout();

    // 2. 다른 화면을 다녀오면 무조건 최신 데이터를 서버에서 다시 불러오게 하고 싶다면 실행
    // (만약 데이터 재조회는 필요 없고 화면만 안 깨지면 된다면 아래 두 줄은 지우셔도 됩니다)
    gridRef.value.current = 1; // 1페이지부터 다시 볼 경우
    await getMenuMetas(true); // true를 넣어 그리드 인스턴스 재생성은 막고 데이터만 갱신
  });

  async function getMenuMetas(isClicked = false) {
    let metas: any = props.metas;
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

    metas =
      props.menuMetaProp !== undefined && props.menuMetaProp !== null
        ? metas[props.menuMetaProp]
        : metas;

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

    // ★ resource-popup 필드 수집 — searchFormMeta 에서 slot 이름을 미리 지정했음
    popupFields.value = searchProps
      .filter((p: any) => p && p.resourcePopup && p.slot)
      .map((p: any) => ({
        field: p.field,
        slotName: p.slot,
        target: p.resourcePopupTarget || '',
        component: p.resourcePopupComponent || p.resourcePopupTarget || '',
        label: p.label || '',
      }));

    let columns = [...props.actionColumns, ...(await parseGridColumns(metas.columns))];
    columns.forEach((column) => {
      column.width = column.minWidth;
    });
    gridProps.columns = columns;

    if (props.gridBodyHeight !== undefined) {
      gridProps.options.bodyHeight = props.gridBodyHeight;
    }

    buttons.value = await parseButtons(metas.buttons);

    nextTick(async () => {
      if (!isClicked) gridRef.value?.createInstance();
      await gridRef.value?.setColumns(gridProps.columns);
      gridColumnRef.value = gridProps.columns;
      gridRef.value?.setFrozenColumn(frozen);
    });
  }

  function debounce(fnc: any) {
    let timer: any;
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

  async function fetch({ page, limit, sorters = [] }: any) {
    try {
      setProps({ submitButtonOptions: { loading: true } });
      let result;
      if (sorters.length == 0) sorters = defaultSorters;
      if (props.fetchHandler && typeof props.fetchHandler == 'function') {
        result = await props.fetchHandler.call(null, page, limit, sorters, searchProps);
      } else {
        result = await fetcher(page, limit, sorters, searchProps);
      }
      result.records = result.records.map((row: any) => ({
        ...row,
        _attributes: { className: { row: row.deleted === true ? ['row-deleted'] : [] } },
      }));
      return result;
    } finally {
      setProps({ submitButtonOptions: { loading: false } });
    }
  }

  async function fetcher(page: number, limit: number, sorters: any, searchProps: any) {
    await validate();
    const fields = getFieldsValue();
    const queryFilters = await getFormattedFilters(fields, searchProps);
    const requestParams: Record<string, any> = {
      query: JSON.stringify(queryFilters),
      sort: JSON.stringify(sorters),
      page,
      limit,
    };
    if (!resourceUrl.value) return { total: 0, records: [] };
    const response: any = await getSearchList(resourceUrl.value, requestParams);
    if (Array.isArray(response)) return { total: response.length, records: response };
    return { total: response.total, records: response.items };
  }

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
    metaRouting,
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

  :deep(.tui-grid-summary-area) {
    border-top: 1px solid #ccc;
    .tui-grid-cell {
      border-right: 1px solid #e0e0e0 !important;
      border-bottom: 1px solid #e0e0e0 !important;
      text-align: center !important;
      vertical-align: middle !important;
      font-weight: bold;
      background-color: #f8f8f8;
    }
  }
</style>
