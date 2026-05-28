<template>
  <CommonPage
    ref="commPageRef"
    :limit="limit"
    :fetchHandler="fetchHandler"
    @grid-fetched="handleGridCreated"
  >
    <ButtonGroup :buttonlist="buttonList" @btn-handler="btnHandler" />
  </CommonPage>
</template>
<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { ref, computed, onMounted } from 'vue';
  import CommonPage from '/src/views/common/CommonPage.vue';
  import { updateList, getSearchList } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getQueryFilters, hasKeyWithFormat } from '/src/views/common/utils';
  import ButtonGroup from '/src/views/common/ButtonGroup.vue';
  import { decodeRmk } from '@/utils/metas/gridRmkMeta';
  import { useFetchStore } from '/@/store/modules/fetchStore';
  import { callApi } from './common/api/callApi.js';

  /**
   * global use ref
   */
  const { createConfirm, notification } = useMessage();
  const fetchStore = useFetchStore();

  /**
   * local ref
   */
  const commPageRef = ref(null as any);
  let limit = 100;
  let gridRef = computed(() => commPageRef.value?.grid);
  let getFormFields = computed(() => commPageRef.value?.getFormFields);
  let validate = computed(() => commPageRef.value?.formValidate);
  let resourceUrl = computed(() => commPageRef.value?.resourceUrl);
  let gridSaveUrl = computed(() => commPageRef.value?.gridSaveUrl);
  let buttonList = computed(() => commPageRef.value?.buttons);
  let columnMetaData = computed(() => commPageRef.value.columns);
  /**life cycle */
  onMounted(() => {});

  // Grid Rmk : Code -> Description
  const handleGridCreated = (record) => {
    fetchStore.isUpdatingRows = true;
    gridRef.value.getData().forEach((row, rowIndex) => {
      decodeRmk(row, columnMetaData.value);
      gridRef.value.setRow(rowIndex, row);
    });
    fetchStore.isUpdatingRows = false;
  };

  /**
   * 꼭 필요한 경우에만 생성한다. 필요하지 않은 경우에는 삭제하고 common page의 Default기능을 사용한다.
   * @param page page번호
   * @param limit page limit
   * @param sorters sortfield
   * @param searchProps search properties
   */
  async function fetchHandler(
    page: any,
    limit: any,
    sorters: any, // = [{ field: 'created_at', ascending: false }],
    searchProps: any,
  ) {
    await validate.value();
    const fields = getFormFields.value();

    hasKeyWithFormat(fields, 'Custom String', searchProps);

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

    for (const [key, value] of Object.entries(response.items)) {
      if (typeof value === 'object' && value !== null) {
        for (const [innerKey, innerValue] of Object.entries(value)) {
          if (typeof innerValue === 'number') {
            value[innerKey] = String(innerValue);
          }
        }
      }
    }
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

  /**
   * 행추가
   */
  const addRow = () => {
    gridRef.value?.addRow();
  };

  /**
   * 수정
   */
  const saveRows = async () => {
    let patches = gridRef.value.getCURows();
    if (patches.length === 0)
      return notification.error({
        message: '에러',
        description: `수정 또는 추가된 데이터가 없습니다`,
        duration: 1,
      });
    console.log(patches)
    let response = await updateList(gridSaveUrl.value, patches);
    if (response) {
      gridRef.value?.fetch();
    }
  };

  /**
   * 삭제
   */
  const deleteRows = () => {
    let selectedRows = gridRef.value.getCheckedRows();
    if (selectedRows.length === 0)
      return notification.error({
        message: '에러',
        description: `선택한 데이터가 없습니다`,
        duration: 1,
      });
    createConfirm({
      iconType: 'warning',
      title: () => '삭제',
      content: () => '선택한 데이터를 삭제하시겠습니까?',
      onOk: async () => {
        let selectedActualRows = gridRef.value.getSelectedRowsToDelete();
        deleteConfirm(selectedActualRows);
      },
    });
  };

  const deleteConfirm = async (selecteRows: any) => {
    let response = await updateList(gridSaveUrl.value, selecteRows);
    if (response) {
      gridRef.value.fetch();
    }

    return true;
  };

  const onAggregateClick = async () => {
    const url = `http://${window.location.hostname}:9500/rest/tb_mw_bcr_item_dimension_avg/dimension-avg/aggregate`;

    const result = await callApi('POST', url);

    const periodDays = result?.period_days ?? 0;
    const targetCnt = result?.target_cnt ?? 0;
    const upsertCnt = result?.upsert_cnt ?? 0;
    const outlierCnt = result?.outlier_cnt ?? 0;

    notification.info({
      message: '집계 성공',
      description: `기간 ${periodDays}일 | 대상 ${targetCnt}건 | 반영 ${upsertCnt}건 | 이상치 ${outlierCnt}건`,
      duration: 2,
    });

    // 재조회
    gridRef.value?.fetch();
  };

  async function btnHandler(param: any) {
    switch (param) {
      case 'addBtnHandler':
        addRow();
        break;
      case 'saveBtnHandler':
        saveRows();
        break;
      case 'deleteBtnHandler':
        deleteRows();
        break;
      case 'excelExportBtnHandler':
        excelExport();
        break;
      case 'AggregateBtnHandler':
        onAggregateClick();
        break;
      default:
        break;
    }
  }

  async function excelExport() {
    await commPageRef.value.downExcel();
  }
</script>
