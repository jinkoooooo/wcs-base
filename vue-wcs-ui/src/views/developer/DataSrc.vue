<template>
  <CommonPage
    ref="commPageRef"
    :actionColumns="actionColumns"
    @gridDbClicked="onGridDbClicked"
    @gridChecked="onGridChecked"
    :limit="limit"
  >
    <Button
      v-for="button in buttons"
      :key="button.text"
      @click="button.listener"
      :type="button.auth === 'delete' ? 'dashed' : 'primary'"
      :danger="button.auth === 'delete'"
      :hidden="button.hidden == 1"
    >
      <template #icon v-if="button.auth === 'export'">
        <DownloadOutlined />
      </template>

      <template #icon v-if="button.auth === 'import'">
        <UploadOutlined />
      </template>

      {{ t(button.title) }}</Button
    >
  </CommonPage>
</template>
<script lang="ts" setup>
  import { useMessage } from '/@/hooks/web/useMessage';
  import { ref, computed } from 'vue';
  import { Button } from 'ant-design-vue';
  import { updateList, getSearchList, getCommonPutApi } from '/@/api/common/api';
  import { useI18n } from '/@/hooks/web/useI18n';

  import CommonPage from '@/views/common/CommonPage.vue';
  import { onMounted } from 'vue';
  import { getCommonPostApi } from '/@/api/sys/menu';
  import { DownloadOutlined, UploadOutlined } from '@ant-design/icons-vue';

  const { t } = useI18n();

  const { createConfirm, notification } = useMessage();

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
  let selectedRows = ref();
  const actionColumns = [
    {
      name: 'startStop',
      header: ' ',
      align: 'center',
      width: 1,
      minWidth: 40,
      renderer: {
        type: 'icon',
        options: {
          icon: (record) => {
            if (record && record.id) {
              return record.status && record.status == 'CONNECTED'
                ? 'ant-design:pause-circle-outlined'
                : 'ant-design:play-square-filled';
            }
          },
        },
      },
    },
  ];

  const onGridDbClicked = (clickedRow) => {
    let event = clickedRow.event;
    if (event.columnName === 'startStop') {
      executeDatasource(clickedRow);
    }
  };

  const executeDatasource = async (record) => {
    if (
      !record.status ||
      record.status == '' ||
      record.status == 'ERROR' ||
      record.status == 'CLOSED'
    ) {
      await connectDatasource(record);
    } else if (record.status == 'CONNECTED') {
      await disconnectDatasource(record);
    }
  };

  const connectDatasource = async (record) => {
    await dsTransaction('init_pool', t('button.connect'), t('text.Sure to process'), record);
  };

  const disconnectDatasource = async (record) => {
    await dsTransaction('destroy_pool', t('button.disconnect'), t('text.Sure to process'), record);
  };

  const dsTransaction = (url, title, msg, record) => {
    createConfirm({
      iconType: 'warning',
      title: () => title,
      content: () => msg,
      onOk: async () => {
        url = `/data_srcs/${record.id}/${url}`;
        let response = await getCommonPostApi(url, null);
        if (response) {
          notification.info({
            message: 'title.info',
            description: t('text.processed'),
            duration: 1,
          });
          gridRef.value.fetch();
        }
      },
    });
  };
  let buttons = ref([] as any[]);

  const onGridChecked = (checkeds) => {
    if (checkeds && checkeds.length > 1) {
      notification.info({
        message: t('title.info'),
        description: '재 프린트 할 배치 1건만 선택해 주세요!',
        duration: 2,
      });
      selectedRows.value = [];
      return;
    }
    selectedRows.value = checkeds;
  };

  onMounted(() => {
    buttons.value = [
      {
        name: 'addRow',
        title: 'button.add',
        auth: 'create',
        hidden: 0,
        listener: addRow,
      },
      {
        name: 'saveRows',
        title: 'button.save',
        auth: 'update',
        hidden: 0,
        listener: saveRows,
      },
      {
        name: 'deleteRows',
        title: 'button.delete',
        auth: 'delete',
        hidden: 0,
        listener: deleteRows,
      },
    ];
  });
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
        let response = await updateList(gridSaveUrl.value, selectedActualRows);
        if (response) {
          gridRef.value.fetch();
        }
      },
    });
  };
</script>
