<template>
  <!-- 위쪽 그리드 -->
  <PageWrapper class="overflow-hidden h-full p-2">
    <div class="inline-flex flex-col flex-1">
      <slot name="customDesc"></slot>

      <section
        class="form-container mb-2 rounded-xl border border-slate-200 bg-white/90 p-3"
        role="search"
      >
        <BasicForm
          ref="searchFormRef"
          @register="formRegister"
          autoSubmitOnEnter
          @toggle-advanced="onHandleAdvanced"
          :hidden="!props.showSearchForm"
        />
      </section>

      <div style="min-height: 480px; height: 50vh">
        <BasicGrid
          ref="gridRef"
          @click="onGridClicked"
          @check="onGridChecked"
          @uncheck="onGridUnchecked"
          @dblclick="onGridDbClicked"
          @gridFetched="onGridFetched"
          :data="gridProps.data"
          :columns="gridProps.columns"
          :options="gridProps.options"
          :theme="gridProps.myTheme"
          :rowHeaders="gridProps.rowHeaders"
          :columnOptions="gridProps.columnOptions"
          :limit="props.masterLimit"
          :pageSizeOptions="[10, 20, 50, 100, 500]"
          :fetchHandler="fetch"
          :showButtons="props.showButtons"
          :showPagination="props.showPagination"
          :frozenCount="frozen"
          :oneLineStyle="true"
          class="h-full"
        >
          <div
            v-if="props.showButtons"
            class="w-full ant-form-item-control-input form-color flex space-x-2 justify-end"
          >
            <slot name="masterButtons"></slot>
          </div>
        </BasicGrid>
      </div>

      <!-- 아래쪽 폼 -->
      <div :class="[{ 'grid grid-cols-2 gap-2': usingRightForm }]" class="mb-4">
        <div class="pt-6 pb-4 pl-2 pr-6 bg-[#fff] rounded-2xl border border-slate-200/70 shadow-sm">
          <BasicForm
            ref="detailRef"
            @register="register"
            :schemas="schemas"
            :submitButtonOptions="props.submitButtonOptions"
            :resetButtonOptions="props.resetButtonOptions"
            :actionColOptions="props.actionColOptions"
            @submit="handleSubmit"
            @reset="handleReset"
          >
            <slot name="customDesc"></slot>

            <template #customSlot="slotProps">
              <slot name="customSlot" v-bind="slotProps"></slot>
            </template>
            <template #submitBefore>
              <slot name="submitBefore" :form="methods"></slot>
            </template>
            <template #resetBefore>
              <slot name="resetBefore" :form="methods"></slot>
            </template>
            <template #formFooter>
              <slot name="formFooter" :form="methods"></slot>
            </template>
          </BasicForm>
          <slot name="detailButtons"></slot>
        </div>

        <div
          v-if="usingRightForm"
          class="right-form-container p-2 rounded-2xl border border-slate-300/70 shadow-sm"
        >
          <slot name="afterDetailForm" :detail="detailForm"></slot>
        </div>
      </div>
    </div>
  </PageWrapper>

  <!--    <div class="flex flex-col">-->
  <!--      <div class="mb-2">-->
  <!--  <CommonPage-->
  <!--    v-if="masterMetas"-->
  <!--    ref="gridRef"-->
  <!--    :metaUrl="masterMetaUrl"-->
  <!--    :metas="masterMetas"-->
  <!--    :limit="masterLimit"-->
  <!--    :fetchHandler="masterFetchHandler"-->
  <!--    @gridClicked="handleGridClicked"-->
  <!--    @gridFetched="handleGridFetched"-->
  <!--    class="min-h-[40vh]"-->
  <!--  >-->

  <!--  </CommonPage>-->
  <!--      </div>-->

  <!--    </div>-->
</template>

<script lang="ts" setup>
  import {
    computed,
    markRaw,
    nextTick,
    onActivated,
    onMounted,
    PropType,
    ref,
    toRaw,
    unref,
  } from 'vue';
  import { useRoute } from 'vue-router';
  import { getCommonGetListApi, getSearchList } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  // import { BasicForm, type FormSchema, useForm } from '/@/components/Form';
  import { type FormSchema } from '/@/components/Form';
  import dayjs from 'dayjs';
  import 'tui-grid/dist/tui-grid.css';
  import { Grid as BasicGrid } from '/@/components/Grid/index';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { PageWrapper } from '/@/components/Page';
  import { getQueryFilters, hasKeyWithFormat } from '/src/views/common/utils';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { parseSearchFormFields, parseSortFields } from '/@/utils/metas/searchFormMeta';
  import { parseGridColumns } from '/@/utils/metas/gridMeta';
  import { parseButtons } from '/@/utils/metas/buttonMeta';

  const props = defineProps({
    /* 공통스타일 */
    menuName: { type: String },
    menuMetaProp: { type: [String, Number] },
    metas: { type: Object },

    usingRightForm: { type: Boolean, default: false }, // 우측 하단 폼 사용여부
    rightFormId: { type: String, default: 'id' },
    showSearchForm: { type: Boolean, default: true },
    showPagination: { type: Boolean, default: true },
    showButtons: { type: Boolean, default: true },

    /* master 그리드 설정 */
    actionColumns: { type: Array, default: () => [] },
    rowHeaders: { type: Array, default: () => ['rowNum', 'checkbox'] },
    optionCheck: { type: Array, default: () => ['checkbox'] },
    masterMetaUrl: { type: String },
    masterFetchHandler: { type: Function },
    masterLimit: { type: Number, default: 30 }, // 페이지 별 레코드 최대 수
    masterRatio: { type: Number, default: 6 },
    // master에서 노출할 컬럼만 지정 // NOTE: 폼에서도 동일한 컬럼만 조회
    masterVisibleFields: {
      type: Array as () => string[],
      default: () => [],
    },
    onGridClicked: { type: Function },

    /* detail 폼 설정 */
    detailRatio: { type: Number, default: 4 },
    layout: {
      type: String as PropType<'horizontal' | 'vertical' | 'inline'>,
      default: 'horizontal',
    },
    baseColProps: {
      type: Object,
      default: () => {
        24;
      },
    },
    labelCol: {
      type: Object,
      default: () => {
        4;
      },
    },
    wrapperCol: {
      type: Object,
      default: () => {
        20;
      },
    },
    actionColOptions: {
      type: Object,
      default: {
        span: 24,
        style: {
          margin: 'auto',
          textAlign: 'right',
          justifyContent: 'flex-end',
          gap: '8px',
          flexWrap: 'wrap',
        },
      },
    },
    baseRowStyle: {
      type: Object,
      default: {
        columnGap: '16px',
        rowGap: '12px',
      },
    },
    resetButtonOptions: { type: Object },
    submitButtonOptions: {
      type: Object,
      default: {
        text: '저장',
      },
    },
    onSubmit: { type: Function },
    // 우선순위 : 스키마 직접 전달 > 빌더
    schemas: { type: Array as PropType<FormSchema[]> },
    buildSchemas: { type: Function },
  });

  const gridProps = {
    rowHeaders: props.rowHeaders,
    columnOptions: { resizable: true },
    columns: [] as any[],
    data: [] as any[],
    myTheme: 'default', // gridThemeCust,
    options: {
      rowHeaders: props.optionCheck,
    },
  };

  const emit = defineEmits([
    'reset',
    'gridClicked',
    'gridDbClicked',
    'gridChecked',
    'gridUnChecked',
    'gridFetched',

    // 그리드
    // 'click',
    // 'check',
    // 'uncheck',
    // 'dblclick',

    // 폼
    'advanced-change',
    'reset',
    'submit',
    'register',
    'field-value-change',
  ]);

  // Grid.applyTheme('clean', gridThemeCust);
  // Grid.applyTheme('default');

  const route = useRoute();

  const searchFormRef = ref();
  const gridRef = ref();
  const gridColumnRef = ref();
  const detailRef = ref();
  const detailForm = ref(); // 폼 데이터

  // 그리드 메타정보
  const resourceUrl = ref();
  const resourceName = ref();
  const masterMetas = ref<any>(null);
  const gridSaveUrl = ref();
  let frozen = 2; // 고정행
  // 버튼 메타정보
  const buttons = ref();
  // 검색 메타정보
  let searchProps = [] as any;
  let defaultSorters = [] as any;

  const { t } = useI18n();
  const { createMessage, notification } = useMessage();

  // 하단 detail 폼
  const [register, methods] = useForm({
    layout: props.layout,
    baseColProps: props.baseColProps,
    labelCol: props.labelCol,
    wrapperCol: props.wrapperCol,
    actionColOptions: props.actionColOptions,
    baseRowStyle: props.baseRowStyle,
  });

  // 검색 폼
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
      await getMenuMetas(true);
    },
  });

  /**
   * 메타 데이터 설정
   */
  // master 그리드 메타 데이터 조회
  async function getMenuMetas(isClicked = false) {
    try {
      masterMetas.value = props.metas ? props.metas : masterMetas.value;
      if (!masterMetas.value) {
        if (props.masterMetaUrl) {
          masterMetas.value = await getCommonGetListApi(props.masterMetaUrl, null);
        } else {
          masterMetas.value = await getCommonGetListApi(
            `/menus/${String(props.menuName ? props.menuName : route.name)}/named_meta`,
            null,
          );
        }
      }
      masterMetas.value = props.menuMetaProp
        ? filterMetasColumns(masterMetas.value[props.menuMetaProp], props.masterVisibleFields)
        : filterMetasColumns(masterMetas.value, props.masterVisibleFields);

      searchProps = parseSearchFormFields(masterMetas.value.columns);
      defaultSorters = parseSortFields(masterMetas.value.columns);
      frozen = masterMetas.value.menu.fixed_columns ? masterMetas.value.menu.fixed_columns : 0;

      let searchUrl = masterMetas.value.menu.resource_url
        ? masterMetas.value.menu.resource_url
        : masterMetas.value.menu.search_url;
      resourceUrl.value = searchUrl;
      resourceName.value = masterMetas.value.menu.resource_name;

      let saveUrl = masterMetas.value.menu.grid_save_url
        ? '/' + masterMetas.value.menu.grid_save_url
        : '/' + masterMetas.value.menu.save_url;
      gridSaveUrl.value = saveUrl;

      await nextTick(); // DOM 생성 보장
      searchProps.splice(3, 0, { field: '', component: 'Divider', label: '' });
      resetSchema(searchProps);

      await nextTick();
      // searchFormRef.value.handleToggleAdvanced = onHandleAdvanced;

      let columns = [
        ...props.actionColumns,
        ...(await parseGridColumns(masterMetas.value.columns)),
      ];

      columns.forEach((column) => {
        column.width = column.minWidth;

        // 공통코드의 ' '값 제외 (선택)
        const editor = column?.editor;
        const listItems = editor?.options?.listItems;
        if (editor?.type === 'select' && Array.isArray(listItems)) {
          const filtered = listItems.filter((it: any) => String(it?.text ?? '').trim() != '');
          editor.options.listItems = filtered;
        }
      });

      gridProps.columns = columns;

      //버튼 메타 정보
      buttons.value = await parseButtons(masterMetas.value.buttons);

      nextTick(async () => {
        if (!isClicked) {
          gridRef.value?.createInstance();
        }

        await gridRef.value?.setColumns(gridProps.columns);
        gridColumnRef.value = gridProps.columns;
        gridRef.value?.setFrozenColumn(frozen);
      });
    } catch (e) {
      createMessage.error('메타 정보 조회에 실패했습니다.');
    }
  }

  //
  async function fetch({ page, limit, sorters = [] }) {
    try {
      setProps({
        submitButtonOptions: {
          loading: true,
        },
      });
      let result;
      if (sorters.length == 0) sorters = defaultSorters;
      if (props.masterFetchHandler && typeof props.masterFetchHandler == 'function') {
        result = await props.masterFetchHandler.call(null, page, limit, sorters, searchProps);
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

      return result;
    } finally {
      setProps({
        submitButtonOptions: {
          loading: false,
        },
      });
    }
  }

  //
  async function fetcher(page, limit, sorters, searchProps) {
    await validate();
    let fields = getFieldsValue();
    hasKeyWithFormat(fields, 'Custom string', searchProps);
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

  // master 그리드에 사용할 컬럼만 필터링
  function filterMetasColumns(metas: any, whitelist: string[]) {
    if (!metas || !whitelist) return metas;
    // Proxy 제거(원본 추출)
    const raw = toRaw(unref(metas));

    // 깊은 복제
    const clone =
      typeof structuredClone === 'function'
        ? structuredClone(raw)
        : JSON.parse(JSON.stringify(raw));

    if (!Array.isArray(whitelist) || whitelist.length === 0) {
      return markRaw(clone);
    }

    // 원하는 컬럼만 필터링
    clone.columns = (raw.columns || []).filter((col: any) => {
      const key = col.name;
      return whitelist.includes(key);
    });

    // 반응성 제외
    return markRaw(clone);
  }

  // detail 폼 스키마 설정
  const schemas = computed<FormSchema[]>(() => {
    const cols = masterMetas.value?.columns || [];

    // 우선순위 : schema > builder > defaultBuilder
    if (props.schemas) return props.schemas;

    const builder = props.buildSchemas ?? defaultBuildSchemas;
    return builder(cols);
  });

  // detail 폼 스키마 생성 - masterGrid 컬럼 메타로부터 매핑
  function defaultBuildSchemas(columns: any[]): FormSchema[] {
    // Input 컴포넌트 타입 지정
    const mapType = (col: any): { component: FormSchema['component']; extra?: any } => {
      const columnType = col.col_type.toString().toLowerCase();
      if (columnType.includes('timestamp') || columnType.includes('date'))
        return { component: 'DatePicker' };
      if (columnType.includes('bool')) return { component: 'Switch' };
      if (
        columnType.includes('int') ||
        columnType.includes('numeric') ||
        columnType.includes('decimal')
      )
        return { component: 'InputNumber' };
      if (columnType.includes('text')) return { component: 'InputTextArea' };
      return { component: 'Input' };
    };

    // 폼 메타 세팅
    return (columns || [])
      .filter((col: any) => col?.name)
      .map((col: any) => {
        const columnType = mapType(col);
        const schema: FormSchema = {
          field: col.name,
          label: col?.label || col?.title || col.name,
          component: columnType.component,
          colProps: { span: 24 },
        };
        // NOTE: Select 컴포넌트는 커스텀 필요
        return schema;
      });
  }

  /**
   * 이벤트 래퍼
   */
  // 고급 검색 선택 이벤트
  async function onHandleAdvanced() {
    searchFormRef.value.advanceState.isAdvanced = !searchFormRef.value?.advanceState.isAdvanced;
    await nextTick(debounce(gridRedraw));
  }

  // master grid 레이아웃 재계산 이벤트
  async function onGridFetched(e) {
    // master grid 레이아웃 재계산
    await nextTick(); // DOM 업데이트
    setTimeout(() => {
      gridRef.value?.refreshLayout?.();
    }, 100);

    emit('gridFetched', e);
  }

  // master 그리드 더블 클릭 이벤트
  function onGridDbClicked(e) {
    if (!e || !e.instance || e.rowKey == null) return;

    if (e.targetType != 'columnHeader') {
      const clickedRow = e.instance.getRow(e.rowKey);
      let clicked = clickedRow;
      if (clicked) {
        clicked.event = e;
      }
      emit('gridDbClicked', clicked);
    }
  }

  // master 그리드 클릭 이벤트
  function onGridClicked(e) {
    if (!e || !e.instance || e.rowKey == null) return;

    if (e.targetType != 'columnHeader') {
      const clickedRow = e.instance.getRow(e.rowKey);
      let clicked = clickedRow;
      if (clicked) {
        clicked.event = e;
      }

      const api = {
        getSchemas: () => schemas.value,
        setFieldsValue: methods.setFieldsValue,
        getFieldsValue: methods.getFieldsValue,
        gridRef,
        detailRef,
      };

      if (props.onGridClicked) props.onGridClicked(clickedRow, api);
      else defaultOnGridClicked(clickedRow, api);

      emit('gridClicked', clicked);
    }
  }

  // master 그리드 체크 이벤트
  function onGridChecked(e) {
    if (e.targetType != 'columnHeader') {
      const checkedRows = e.instance.getCheckedRows();
      let checkeds = checkedRows;
      emit('gridChecked', checkeds);
    }
  }

  // master 그리드 체크 해제 이벤트
  function onGridUnchecked(e) {
    if (e.targetType != 'columnHeader') {
      const checkedRows = e.instance.getCheckedRows();
      let checkeds = checkedRows;
      emit('gridUnChecked', checkeds);
    }
  }

  // detail 폼 제출 이벤트 래퍼
  async function handleSubmit(formData: any) {
    const api = {
      getSchemas: () => schemas.value,
      setFieldsValue: methods.setFieldsValue,
      getFieldsValue: methods.getFieldsValue,
      gridRef,
      detailRef,
    };
    if (props.onSubmit) return props.onSubmit(formData, api);
    return defaultOnSubmit(formData, api);
  }

  // detail 폼 리셋 이벤트 래퍼
  async function handleReset(formData: any) {
    detailForm.value = null;
    emit('reset');
  }

  //
  function debounce(fnc) {
    var timer;
    return function () {
      if (timer) clearTimeout(timer);
      timer = setTimeout(fnc, 100);
    };
  }

  /**
   * 기본 이벤트
   */
  // master 그리드 클릭 이벤트 - 클릭 시, Form 조회
  async function defaultOnGridClicked(clickedRow: any, api: any) {
    const schemas: FormSchema[] = api.getSchemas() ?? [];
    const fieldValues: Record<string, any> = {};

    if (!schemas.length || !clickedRow) return;

    schemas.forEach((schema) => {
      const field = schema.field;

      // schemas에 정의된 필드만 데이터 할당 -> 전체 컬럼에 정의된 필드를 데이터로 할당
      // if (!Object.hasOwn(clickedRow, field)) return;

      let data = clickedRow[field];

      // 날짜 변환
      if (schema.component === 'DatePicker' && data) {
        data = dayjs(data);
      }

      fieldValues[field] = data;
    });

    await api.setFieldsValue(fieldValues);

    // 우측 하단 폼 id 전달
    detailForm.value = clickedRow ?? null;

    // handleGridFetched();
    onGridFetched(null);
    emit('field-value-change')
  }

  // detail 폼 제출 이벤트 - 클릭 시, 생성/수정
  async function defaultOnSubmit(formData: any, api: any) {
    // TODO: MASTER URL로 저장 로직 구현
  }

  // master 그리드 재 생성 함수
  async function gridRedraw() {
    gridRef.value?.destroy();
    await nextTick();
    gridRef.value?.createInstance();
    await gridRef.value?.setColumns(gridProps.columns);
    gridRef.value?.setFrozenColumn(frozen);
  }

  /*
   * 하위 노출 함수
   */
  // 엑셀 다운로드
  async function downExcel() {
    gridRef.value.exportExcel(route.name);
  }

  // 검색 메타정보 조회
  async function getSearchProps() {
    return searchProps;
  }

  /**
   * 라이프사이클
   */
  onMounted(async () => {
    // master 그리드 메타 데이터 조회
    await getMenuMetas();

    // master grid 레이아웃 재계산
    await nextTick();
    setTimeout(() => onGridFetched(null), 200);

    await nextTick();
    setTimeout(() => {
      gridRef.value?.refreshLayout?.();
    }, 200);
    if (gridRef.value) {
      gridRef.value?.refreshLayout?.();
    }
  });

  onActivated(async () => {
    await nextTick();
    setTimeout(() => {
      gridRef.value?.refreshLayout?.();
    }, 100);
  });

  defineExpose({
    searchFormRef, // searchForm: searchFormRef,
    grid: gridRef, // grid: gridRef,
    columns: gridColumnRef,
    detailRef,
    ...props,

    // 검색
    searchProps, // 검색정보
    formValidate: validate, // 검색검증
    getFormFields: getFieldsValue, // 검색조건

    // 메타정보
    resourceName,
    resourceUrl,
    gridSaveUrl,
    getSearchProps, // 검색메타정보
    buttons, // 버튼메타정보

    // 함수
    getMenuMetas, // 마스터그리드 메타정보 조회
    downExcel, // 엑셀 다운로드
  });
</script>

<style scoped>
  .right-form-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    min-height: 0;
    padding-bottom: 16px;
  }

  .right-form-container > * {
    flex: 1 1 auto;
    min-height: 0;
    height: 100%;
  }
</style>
