<template>
  <CommonPage
    ref="commPageRef"
    :actionColumns="actionColumns"
    @gridDbClicked="onGridDbClicked"
    @gridChecked="onGridChecked"
    :limit="limit"
    :fetchHandler="fetchHandler"
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
      codeField="service_logic"
      width="fit-content"
      :resourceUrl="editorUrl"
    />
  </CommonPage>
</template>
<script lang="ts" setup>
  import { useMessage } from '/@/hooks/web/useMessage';
  import { ref, computed, onMounted } from 'vue';
  import { Button } from 'ant-design-vue';
  import { updateList } from '/@/api/common/api';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useModal } from '/@/components/Modal';
  import { getQueryFilters } from '/src/views/common/utils';
  import {
    getSearchList,
  } from '/@/api/common/api';

  import CommonPage from '/src/views/common/CommonPage.vue';
  import CodeEditorModal from '/src/views/common/CodeEditorModal.vue';
  import { MODE } from '/@/components/CodeEditor';
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
  let getFormFields = computed(() => commPageRef.value?.getFormFields);
  let validate = computed(() => commPageRef.value?.formValidate);
  let resourceUrl = computed(() => commPageRef.value?.resourceUrl);
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
  const onGridDbClicked = (clickedRow) => {
    let event = clickedRow.event;
    selectedRows.value = clickedRow;
    if (event.columnName === 'logic') {
      editSource(selectedRows.value, openGroovyModal, event.columnName);
    }
  };

  const editSource = async (record, openModal, field) => {
    record.url = '/diy_services';
    editorUrl.value = record.url + '/' + record.id;
    openModal(true, {
      record,
      isUpdate: true,
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
   * 조회
   */
  async function fetchHandler(page, limit, sorters, searchProps) {
    await validate.value();
    const filterCols = getFormFields.value();
    const queryFilters = await getQueryFilters(filterCols, searchProps);

    // 'job_type'이 'DAS'인 고정된 검색 필터를 추가
    sorters = [{ field: 'name', ascending: false }];
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
    return {
      total: response.total,
      records: response.items,
    };
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
        let response = await updateList(gridSaveUrl.value, selectedActualRows);
        if (response) {
          gridRef.value.fetch();
        }
      },
    });
  };
</script>
