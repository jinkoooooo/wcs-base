<template>
  <div>
    <BasicTable @register="registerTable" @fetch-success="onFetchSuccess">
      <template #toolbar>
        <a-button type="primary" @click="openDrawerForCreate"> 메뉴추가 </a-button>
        <a-button type="primary" @click="onClearCache"> {{ t('button.clear_cache') }} </a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <TableAction
            :actions="[
              {
                label: t('title.setup_columns'),
                icon: 'clarity:note-edit-line',
                onClick: handleUpdateMaster.bind(null, record),
              },
              {
                label: t('label.delete'),
                icon: 'ant-design:delete-outlined',
                color: 'error',
                popConfirm: {
                  title: '삭제하시겠습니까?',
                  placement: 'left',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </template>
    </BasicTable>
    <MenuDrawer @register="registerMasterDrawer" @success="handleSuccess" />
    <!-- <MenuDetailDrawer @register="registerDetailDrawer" @success="handleSuccess" /> -->
  </div>
</template>
<script lang="ts" setup>
  import { useMessage } from '/@/hooks/web/useMessage';
  import { nextTick } from 'vue';
  import { Recordable } from '@vben/types';
  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import { getMenuList, getMenuList3 } from '/@/api/sys/menu';
  import { getCommonDeleteApi } from '/@/api/common/api';
  import { useDrawer } from '/@/components/Drawer';
  import MenuDrawer from './MenuDrawer.vue';
  // import MenuDetailDrawer from './MenuDetailDrawer.vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { columns, searchFormSchema } from './menu.data';
  import { getCommonPutApi } from '/@/api/common/api';

  const { t } = useI18n();
  const { createConfirm } = useMessage();
  const [registerMasterDrawer, { openDrawer: openMasterDrawer }] = useDrawer();
  // const [registerDetailDrawer, { openDrawer: openDetailDrawer }] = useDrawer();

  const [registerTable, { reload, expandAll }] = useTable({
    title: '메뉴관리',
    api: getMenuList3,
    columns,
    formConfig: {
      labelWidth: 120,
      schemas: searchFormSchema,
    },
    isTreeTable: true,
    pagination: false,
    striped: false,
    useSearchForm: true,
    showTableSetting: true,
    bordered: true,
    showIndexColumn: false,
    canResize: false,
    actionColumn: {
      width: 240,
      title: t('label.job'),
      dataIndex: 'action',
      // slots: { customRender: 'action' },
      fixed: undefined,
    },
  });

  function openDrawerForCreate() {
    openMasterDrawer(true, {
      isUpdate: false,
    });
  }
  /**
   * edit 클릭하는 경우 drawer 열기처리
   * @param record Recordable는 Record<String,T>형태로 정의되어 있으며 T에 유형을 제공하여야 한다. Map의 사용법과 비슷하다.
   */
  function handleUpdateMaster(record: Recordable<Object>) {
    openMasterDrawer(true, {
      record,
      isUpdate: true,
    });
  }

  async function handleDelete(record: Recordable<Object>) {
    let response = await getCommonDeleteApi(`/menus/${record.id}`, '');
    if (response) {
      reload();
    }
  }
  /**
   * menu cache clear
   */
  const onClearCache = async () => {
    createConfirm({
      iconType: 'warning',
      title: () => '동기화',
      content: () =>
        '케시 삭제하면 본 도메인에서 기존에 등록된 모든 메뉴의 케시를 재 등록하게 됩니다. 그래도 진행하시겠습니까?',
      onOk: async () => {
        let response = await getCommonPutApi(`/menus/clear_cache`, null);
        if (response) {
          reload();
        }
      },
    });
  };
  function handleSuccess() {
    reload();
  }

  function onFetchSuccess() {
    // nextTick(expandAll);
  }
</script>
