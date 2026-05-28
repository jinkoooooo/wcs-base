<template>
  <PageWrapper>
    <Layout class="gap-1">
      <Sider width="50%">
        <CommonPage ref="commPageRef"
                    :limit="limit"
                    :fetchHandler="fetchHandler"
                    @gridFetched="handleGridCreated"
                    @gridClicked="onClick"
                    :baseColProps="{ xxl: 12, lg: 24, md: 24, sm: 24 }">
          <ButtonGroup :buttonlist="buttonList" @btnHandler="btnHandler" />
        </CommonPage>
      </Sider>
      <Content>
        <DmtDisplayer :data="{ selectedLcId, selectedModelType }"/>
      </Content>
    </Layout>
  </PageWrapper>
</template>
<script lang="ts" setup>

import { ref, computed } from 'vue';
import { Layout } from 'ant-design-vue';
import { useMessage } from '/@/hooks/web/useMessage';
import { PageWrapper } from '/@/components/Page';
import CommonPage from '/src/views/common/CommonPage.vue';
import { getQueryFilters, hasKeyWithFormat } from '/src/views/common/utils';
import { updateList, getSearchList } from '/@/api/common/api';
import ButtonGroup from '@/views/common/ButtonGroup.vue';
import DmtDisplayer from './DmtDisplayer.vue';

/**
 * global use ref
 */
const { createConfirm, notification } = useMessage();
const { Sider, Content } = Layout;

/**
 * local ref
 */
const commPageRef = ref(null as any);
let limit = 100;
let gridRef = computed(() => commPageRef.value?.grid);
let getFormFields = computed(() => commPageRef.value?.getFormFields);
let validate = computed(() => commPageRef.value?.formValidate);
let resourceUrl = computed(() => commPageRef.value?.resourceUrl);
// let resourceName = computed(() => commPageRef.value?.resourceName);
let gridSaveUrl = computed(() => commPageRef.value?.gridSaveUrl);
let buttonList = computed(() => commPageRef.value?.buttons);
const selectedLcId = ref<string | null>(null);
const selectedModelType = ref<string | null>(null);

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
  sorters: any,
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
  gridRef.value.getData().forEach((row, rowIndex) => {
    gridRef.value.setRow(rowIndex, row);
  });
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

const handleGridCreated = () => {

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
    default:
      break;
  }
}

const onClick = (selectedRow) => {
  if (selectedRow) {
    selectedLcId.value = selectedRow.lc_id;
    selectedModelType.value = selectedRow.model_type;
  }
};

</script>
<style lang="less" scoped>
.ant-layout-header {
  height: auto !important;
  border-radius: 10px;
  border: 1px solid #d9d9d9;
  background-color: @component-background;
}
.ant-form {
  padding: 10px;
}
.ant-layout {
  padding-bottom: 10px;
}
.ant-layout-sider {
  border-radius: 10px;
  border: 1px solid #d9d9d9;
  background: @component-background;
}

.ant-layout-content {
  border-radius: 10px;
  border: 1px solid #d9d9d9;
  background: @component-background;
}
.border-com {
  border-radius: 10px;
  border: 1px solid #d9d9d9;
}
.ant-layout-sider-dark {
  background: @component-background;
}
</style>
