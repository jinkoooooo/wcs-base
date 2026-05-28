<template>
  <CommonPage
    ref="commPageRef"
    :limit="limit"
    :fetchHandler="fetchHandler"
    @gridFetched="handleGridCreated"
  >
    <ButtonGroup :buttonlist="buttonlist" @btnHandler="btnHandler" />
  </CommonPage>
</template>
<script lang="ts" setup>
import 'tui-grid/dist/tui-grid.css';
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import CommonPage from '../common/CommonPage.vue';
import { useI18n } from '/@/hooks/web/useI18n';
import {
  updateList,
  getSearchList,
  getCommonPutApi,
  getExcelDownload,
  getCommonPostApi
} from '/@/api/common/api';
import { useMessage } from '/@/hooks/web/useMessage';
import { getQueryFilters, hasKeyWithFormat } from '/@/views/common/utils';
import ButtonGroup from '/@/views/common/ButtonGroup.vue';
import { decodeRmk } from "@/utils/metas/gridRmkMeta";
import { useFetchStore } from '/@/store/modules/fetchStore';

/**
 * global use ref
 */
const { t } = useI18n();
const { createConfirm, notification } = useMessage();
const route = useRoute();
const fetchStore = useFetchStore();

const exceldownName = t('menu.' + route.name);

/**
 * local ref
 */
const commPageRef = ref(null as any);
let limit = 100;
let gridRef = computed(() => commPageRef.value?.grid);
let getFormFields = computed(() => commPageRef.value?.getFormFields);
let validate = computed(() => commPageRef.value?.formValidate);
let resourceUrl = computed(() => commPageRef.value?.resourceUrl);
let resourceName = computed(() => commPageRef.value?.resourceName);
let gridSaveUrl = computed(() => commPageRef.value?.gridSaveUrl);
let buttonlist = computed(() => commPageRef.value?.buttons);
let columnMetaData = computed(() => commPageRef.value.columns);

const reset = async () => {
  let selectedRows = gridRef.value.getCheckedRows();
  if (selectedRows.length === 0)
    return notification.error({
      message: '에러',
      description: `선택한 데이터가 없습니다`,
      duration: 1,
    });

  const nonErrorRows = selectedRows.filter(row => row.snd_yn !== 'E');
  if (nonErrorRows.length > 0) {
    // 'E'가 아닌 데이터가 하나라도 있으면 오류 메시지 출력
    return notification.error({
      message: '에러',
      description: '에러인 주문만 초기화 가능합니다.',
      duration: 3,
    });
  }

  try {
    const url = '/c_wms_odr/reset_error';
    const result = await getCommonPostApi(url, selectedRows);

    if (result === "success") {
      return notification.success({
        message: '성공',
        description: `에러가 초기화 되었습니다.`,
        duration: 3,
      });
    }
  }
  catch (e) {
    return notification.error({
      message: '에러',
      description: e.message,
      duration: 3,
    });
  }
};

/**life cycle */
onMounted(() => {});

// Grid Rmk : Code -> Description
const handleGridCreated = (records) => {
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

/*
 * 엑셀 다운로드
 */
async function downExcel() {
  commPageRef.value.downExcel();
}

/*
 * 엑셀 다운로드
 */
async function clearCash() {
  let typeName = commPageRef.value.resourceUrl;
  let url = '';
  switch (typeName) {
    case 'terminologies':
      url = '/terminologies/clear_cache.json';
      break;
    case 'domains':
      url = '/domains/clear_cache.json';
      break;
    case 'menus':
      url = '/menus/clear_cache.json';
      break;
    case 'entities':
      url = '/menus/clear_cache.json';
      break;
    case 'common_codes':
      url = '/common_codes/clear_cache.json';
      break;
    case 'messages':
      url = '/messages/clear_cache.json';
      break;
    case 'settings':
      url = '/settings/clear_cache.json';
      break;
  }

  createConfirm({
    iconType: 'warning',
    title: () => '동기화',
    content: () =>
      '캐시 삭제하면 본 도메인에 등록된 정보 캐시를 재 등록하게 됩니다. 그래도 진행하시겠습니까?',
    onOk: async () => {
      let response = await getCommonPutApi(url, null);
      if (response) {
        gridRef.value.fetch();
      }
    },
  });
}

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
    case 'exportBtnHandler':
      downExcel();
      break;
    case 'clear-cacheBtnHandler':
      clearCash();
      break;
    case 'exceldownBtnHandler':
      exceldown();
      break;
    case 'resetBtnHandler':
      reset();
      break;
    default:
      break;
  }
}

async function exceldown() {
  const fields = getFormFields.value();
  const searchProps = await commPageRef.value.getSearchProps();
  hasKeyWithFormat(fields, 'date', searchProps);

  const queryFilters = await getQueryFilters(fields, searchProps);
  let params = [
    {
      name: 'query',
      value: JSON.stringify(queryFilters),
    },
  ];
  const match = resourceUrl.value.match(/\/?diy_services\/(.*?)\/read_by_pagination/);
  const extractedValue = match ? match[1] : 'empty';

  if (extractedValue === 'empty') {
    params.push({ name: 'entityName', value: resourceName.value });
    params.push({ name: 'routeName', value: route.name });
  }

  let requestParams = {};
  params.forEach((item) => {
    requestParams[item['name']] = item['value'];
  });

  let downUrl = '/diy_services/' + extractedValue + '/excelDown';
  let res = await getExcelDownload(downUrl, requestParams);

  var fileObj = await new Blob([res], { type: 'application/octet-stream' });
  var fileName = exceldownName;
  const filename = await downloadFile(fileObj, fileName + '.xlsx');
}

const downloadFile = async (blob, fileName) => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
  return fileName;
};
</script>
