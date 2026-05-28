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

    <CodeEditorModal
      @register="registerGroovy"
      @success="handleSuccess"
      :mode="MODE.GROOVY"
      codeField="log"
      width="fit-content"
    />
  </CommonPage>
</template>
<script lang="ts" setup>
  import { useMessage } from '/@/hooks/web/useMessage';
  import { ref, computed, onMounted } from 'vue';
  import { Button } from 'ant-design-vue';
  import { updateList, getCommonGetApi } from '/@/api/common/api';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useModal } from '/@/components/Modal';

  import CommonPage from '/src/views/common/CommonPage.vue';
  import CodeEditorModal from '/src/views/common/CodeEditorModal.vue';
  import { MODE } from '/@/components/CodeEditor';
  import { useGlobSetting } from '/@/hooks/setting';
  import { DownloadOutlined, UploadOutlined } from '@ant-design/icons-vue';

  const { t } = useI18n();

  const { createConfirm, notification } = useMessage();
  const [registerGroovy, { openModal: openGroovyModal }] = useModal();
  /**
   * local ref
   */
  const commPageRef = ref(null as any);
  let limit = 100;
  let gridRef = computed(() => commPageRef.value?.grid);
  let gridSaveUrl = computed(() => commPageRef.value?.gridSaveUrl);
  let selectedRows = ref();
  let editorUrl = ref();
  let buttons = ref([] as any[]);

  const actionColumns = [
    {
      name: 'logic',
      header: t('label.logic'),
      align: 'center',
      width: 1,
      minWidth: 40,
      renderer: {
        type: 'icon',
        options: {
          icon: (record) => {
            return record && record.id && record.status != 'RUNNING'
              ? 'ant-design:edit-twotone'
              : '';
          },
        },
      },
    },
    {
      name: 'download',
      header: t('button.download'),
      align: 'center',
      width: 1,
      minWidth: 60,
      renderer: {
        type: 'icon',
        options: {
          icon: (record) => {
            return record && record.id && record.status != 'RUNNING'
              ? 'ant-design:down-outlined'
              : '';
          },
        },
      },
    },
  ];

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
        name: 'deleteRows',
        title: 'button.delete',
        auth: 'delete',
        hidden: 0,
        listener: deleteRows,
      },
    ];
  });
  const onGridDbClicked = (clickedRow) => {
    let event = clickedRow.event;
    if (event.columnName === 'logic') {
      editSource(clickedRow, openGroovyModal, event.columnName);
    } else if (event.columnName === 'download') {
      downloadLogfile(clickedRow);
    }
  };

  const editSource = async (record, openModal, field) => {
    editorUrl.value = `/diy_services/${record.id}`;
    let url = `/log_management/read/${record.name}?lines=1000`;
    let response = await getCommonGetApi(url, null);
    openModal(true, {
      record: response,
      isUpdate: true,
    });
  };

  const downloadLogfile = async (record) => {
    createConfirm({
      iconType: 'info',
      title: () => t('button.confirm'),
      content: () => t('text.Want to download file'),
      onOk: async () => {
        const globSetting = useGlobSetting();
        const urlPrefix = globSetting.nearRequestUrl;
        //TODO: get base url for download
        let url = `/rest/log_management/download/${record.name}`;
        window.location = url;
      },
    });
  };
  const handleSuccess = () => {
    gridRef.value.fetch();
  };

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
