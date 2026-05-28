<template>
  <CommonPage
    ref="commPageRef"
    :limit="limit"
    :fetchHandler="fetchHandler"
    :actionColumns="actionColumns"
    @gridFetched="handleGridCreated"
    @gridDbClicked="onGridDbClicked"
  >
    <ButtonGroup :buttonlist="buttonlist" @btnHandler="btnHandler" />
  </CommonPage>
</template>
<script lang="ts" setup>
import 'tui-grid/dist/tui-grid.css';
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import CommonPage from '/src/views/common/CommonPage.vue';
import { useI18n } from '/@/hooks/web/useI18n';
import { updateList, getSearchList, getCommonPostApi, getExcelDownload } from '/@/api/common/api';
import { useMessage } from '/@/hooks/web/useMessage';
import { getQueryFilters, hasKeyWithFormat } from '/src/views/common/utils';
import { useFetchStore } from '/@/store/modules/fetchStore';
import ButtonGroup from '/src/views/common/ButtonGroup.vue';

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
/**life cycle */
onMounted(() => {});

// Grid fetch 이후 로직
const handleGridCreated = (records) => {
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
 * 액션 컬럼 정의
 * is_active 값에 따라 실행(play) / 정지(pause) 아이콘 표시
 */
const actionColumns = [
  {
    name: 'activeToggle',
    header: ' ',
    align: 'center',
    width: 1,
    minWidth: 40,
    renderer: {
      type: 'icon',
      options: {
        icon: (record) => {
          if (record && record.id !== undefined) {
            return record.is_active
              ? 'ant-design:pause-circle-outlined'
              : 'ant-design:play-square-filled';
          }
        },
      },
    },
  },
];

/**
 * 그리드 더블 클릭 또는 아이콘 클릭 이벤트 핸들러
 */
const onGridDbClicked = async (clickedRow) => {
  const { event, id, is_active } = clickedRow;

  if (event.columnName === 'activeToggle') {
    const title = is_active ? '정지' : '실행';
    const msg = is_active ? '정지하시겠습니까?' : '실행하시겠습니까?';
    const param = {
      id: id,
      is_active: !is_active,
    }
    console.log(param);

    createConfirm({
      iconType: 'warning',
      title: () => title,
      content: () => msg,
      onOk: async () => {
        try {
          const url = `/job/control`;
          let response = await getCommonPostApi(url, param);
          if (response) {
            notification.success({
              message: '성공',
              description: `${title} 처리가 완료되었습니다.`,
              duration: 1,
            });
            gridRef.value.fetch(); // 그리드 새로고침
          }
        } catch (error) {
          notification.error({
            message: '오류',
            description: `${title} 처리 중 오류가 발생했습니다.`,
          });
        }
      },
    });
  }
};

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

  // patches 순회하며 기본값 대입
  patches.forEach((item) => {
    // 1. ok_count가 없거나 null이면 0 대입
    if (item.ok_count === undefined || item.ok_count === null) {
      item.ok_count = 0;
    }
    // 2. ng_count가 없거나 null이면 0 대입
    if (item.ng_count === undefined || item.ng_count === null) {
      item.ng_count = 0;
    }
    // 3. is_active가 설정되어 있지 않으면 false 대입
    if (item.is_active === undefined || item.is_active === null) {
      item.is_active = false;
    }
  });

  let response = await updateList(gridSaveUrl.value, patches);
  if (response.code === 0) {
    gridRef.value?.fetch();
  } else {
    return notification.error({
      message: '에러',
      description: `진행 중인 Job은 수정할 수 없습니다.`,
      duration: 1,
    });
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
  if (response.code === 0) {
    gridRef.value.fetch();
  } else {
    return notification.error({
      message: '에러',
      description: `진행 중인 Job은 삭제할 수 없습니다.`,
      duration: 1,
    });
  }

  return true;
};

/*
 * 엑셀 다운로드
 */
async function downExcel() {
  commPageRef.value.downExcel();
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
</script>
