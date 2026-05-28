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
  import { getCommonPostApi } from '/@/api/common/api';
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

    const requestParams = {
      page: page,
      limit: limit,
      process_status: fields.process_status,
      item_code: fields.item_code,
      accept_datetime: fields.accept_datetime,
    };

    const response = await callApi(
      'POST',
      `http://${window.location.hostname}:9500/rest/${resourceUrl.value}/reject_info`,
      requestParams,
    );

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
   * 엑셀 다운로드
   */
  async function excelExport() {
    await commPageRef.value.downExcel();
  }

  /**
   * 작업 강제 취소
   */
  const powerCancel = async () => {
    let selectedRows = gridRef.value.getCheckedRows();
    if (selectedRows.length === 0)
      return notification.error({
        message: '에러',
        description: `선택된 작업이 없습니다.`,
        duration: 1,
      });
    else if (selectedRows.length > 1)
      return notification.error({
        message: '에러',
        description: `한 번에 하나의 작업만 취소 가능합니다.`,
        duration: 1,
      });
    try {
      createConfirm({
        iconType: 'warning',
        title: () => '강제 취소',
        content: () => '선택한 작업을 강제 취소하시겠습니까?',
        onOk: async () => {
          const url = '/tb_mw_xyz_order/cancel_order';
          const param = {
            order_id: selectedRows[0].order_id,
          };
          const result = await getCommonPostApi(url, param);
          if (result.code === 0) {
            return notification.info({
              message: '성공',
              description: result.message,
              duration: 3,
            });
          } else {
            return notification.error({
              message: '실패',
              description: result.message,
              duration: 3,
            });
          }
        },
      });
    } catch (error) {
      return notification.error({
        message: '에러',
        description: error.message,
        duration: 3,
      });
    }
  };

  async function btnHandler(param: any) {
    switch (param) {
      case 'excelExportBtnHandler':
        excelExport();
        break;
      case 'powerCancelBtnHandler':
        powerCancel();
        break;
      default:
        break;
    }
  }
</script>
