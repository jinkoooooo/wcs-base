<template>
  <CommonPage
    ref="commPageRef"
    :limit="limit"
    :fetchHandler="fetchHandler"
    @gridFetched="handleGridCreated"
    :actionColumns="actionColumns"
    @gridDbClicked="onGridDbClicked"
  >
    <ButtonGroup :buttonlist="buttonList" @btnHandler="btnHandler" />
    <DmgEditor2D @register="registerDmgEditor2D" :result="resultRef2D" :data="{ initial2DImageUrl, center2DInfo }" width="fit-content"/>
  </CommonPage>
</template>
<script lang="ts" setup>
import 'tui-grid/dist/tui-grid.css';
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import CommonPage from '/src/views/common/CommonPage.vue';
import { useI18n } from '/@/hooks/web/useI18n';
import { updateList, getSearchList, getCommonPutApi, getExcelDownload, apiClient } from '/@/api/common/api';
import { useMessage } from '/@/hooks/web/useMessage';
import { useModal } from '/@/components/Modal';
import { getQueryFilters, hasKeyWithFormat } from '/src/views/common/utils';
import ButtonGroup from '/src/views/common/ButtonGroup.vue';
import DmgEditor2D from '@/views/dashboard_lms/management-dmg/DmgEditor2D.vue';

/**
 * global use ref
 */
const { t } = useI18n();
const { createConfirm, notification } = useMessage();
const route = useRoute();

const exceldownName = t('menu.' + route.name);

/**
 * local ref
 */
const commPageRef = ref(null as any);
const resultRef2D = ref(null as any);
const [registerDmgEditor2D, { openModal: openDmgEditor2D }] = useModal();
const initial2DImageUrl = ref(null as any);
const center2DInfo = ref(null as any);
let limit = 100;
let gridRef = computed(() => commPageRef.value?.grid);
let getFormFields = computed(() => commPageRef.value?.getFormFields);
let validate = computed(() => commPageRef.value?.formValidate);
let resourceUrl = computed(() => commPageRef.value?.resourceUrl);
let resourceName = computed(() => commPageRef.value?.resourceName);
let gridSaveUrl = computed(() => commPageRef.value?.gridSaveUrl);
let buttonList = computed(() => commPageRef.value?.buttons);

const actionColumns = [
  {
    name: 'simulator2D',
    header: '2D',
    align: 'center',
    width: 1,
    minWidth: 40,
    renderer: {
      type: 'icon',
      options: {
        icon: (record) => {
          if (record && record.id) {
            return 'ant-design:play-square-filled';
          }
        },
      },
    },
  },
  {
    name: 'simulator3D',
    header: '3D',
    align: 'center',
    width: 1,
    minWidth: 40,
    renderer: {
      type: 'icon',
      options: {
        icon: (record) => {
          if (record && record.id) {
            return 'ant-design:play-square-filled';
          }
        },
      },
    },
  }
];

const openDmgEditor2DPopup = async (clickedRow) => {
  const image = await downloadImage(clickedRow, "2D");
  initial2DImageUrl.value = image;
  center2DInfo.value = clickedRow;

  openDmgEditor2D(true, {});
};

const onGridDbClicked = (clickedRow) => {
  let event = clickedRow.event;
  if (event.columnName === 'simulator2D') {
    openDmgEditor2DPopup(clickedRow);
  } else if (event.columnName === 'simulator3D') {

  }
};

/**life cycle */
onMounted(() => {});

const handleGridCreated = (records) => {

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

  // 데이터베이스 Not Null 컬럼 기본값 할당
  for (let i = 0; i < patches.length; i++) {
    if (patches[i].cud_flag_ === "c") {
      if (patches[i].is_use === null) {
        patches[i].is_use = false;
      }
      patches[i].positionX2d = 0;
      patches[i].positionY2d = 0;
      patches[i].scaleX2d = 1;
      patches[i].scaleY2d = 1;
      patches[i].rotation2d = 0;
      patches[i].flipHorizontal2d = false;
      patches[i].flipVertical2d = false;
      patches[i].positionX3d = 0;
      patches[i].positionY3d = 0;
      patches[i].positionZ3d = 0;
      patches[i].scaleX3d = 1;
      patches[i].scaleY3d = 1;
      patches[i].scaleZ3d = 1;
      patches[i].rotationX3d = 0;
      patches[i].rotationY3d = 0;
      patches[i].rotationZ3d = 0;
      patches[i].boxPositionX3d = 0;
      patches[i].boxPositionY3d = 0;
      patches[i].boxPositionZ3d = 0;
      patches[i].boxScaleX3d = 1;
      patches[i].boxScaleY3d = 1;
      patches[i].boxScaleZ3d = 1;
      patches[i].boxRotationX3d = 0;
      patches[i].boxRotationY3d = 0;
      patches[i].boxRotationZ3d = 0;
      patches[i].box_is_use = true;
    }
  }

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

const downloadImage = async (clickedRow, dimension) => {
  try {
    // 1. axios를 사용해 Spring Boot API에 GET 요청
    // ★★★ 중요: 응답 타입을 'blob'으로 지정해야 합니다. ★★★
    const url = `/status_board_dmt/download/image?lcId=${clickedRow.lc_id}&modelType=${clickedRow.model_type}&dimension=${dimension}`;
    const response = await apiClient.get(url, { responseType: 'blob' });

    // 2. 응답으로 받은 이미지 데이터(Blob)를 브라우저가 사용할 수 있는 임시 URL로 변환
    const blob = new Blob([response.data], { type: response.headers['content-type'] });

    // 2-1. 응답으로 받은 이미지가 없을 경우 이미지 초기화 후 종료
    if (response.data.size === 0) {
      return null;
    }

    // 3. 생성된 URL을 imageUrl에 할당하여 <img> 태그에 연결
    return URL.createObjectURL(blob);
  } catch (e) {
    return null;
  }
};
</script>
