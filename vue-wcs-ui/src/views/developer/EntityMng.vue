<template>
  <PageWrapper>
    <div class="h-99">
      <!--:baseColProps="{ xxl: 12, lg: 24, md: 24, sm: 24 }"-->
      <CommonPage ref="masterRef" @gridClicked="onClick">
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

          {{ t(button.text) }}</Button
        >
      </CommonPage>
    </div>
    <CommonPage
      ref="detailRef"
      :limit="100"
      :metaUrl="metaUrl"
      menuMetaProp="0"
      :showSearchForm="false"
      :fetchHandler="detailFetchHandler"
      :baseColProps="{ xxl: 12, lg: 15, md: 15, sm: 15 }"
    >
      <Button type="primary" @click="addDetailRow">{{ t('button.new') }}</Button>
      <Button type="primary" @click="saveDetailRows">{{ t('button.save') }}</Button>
      <Button danger @click="deleteDetailRows">{{ t('button.delete') }}</Button>
    </CommonPage>
  </PageWrapper>
</template>
<script lang="ts" setup>
  import { ref, computed, onMounted } from 'vue';
  import { Button } from 'ant-design-vue';
  import { useRoute } from 'vue-router';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';

  import {
    updateList,
    updateCommonDetailList,
    getSearchList,
    getCommonPutApi,
    getCommonPostApi,
    getCommonDeleteApi,
  } from '/@/api/common/api';

  import { PageWrapper } from '/@/components/Page';
  import CommonPage from '/src/views/common/CommonPage.vue';
  import { DownloadOutlined, UploadOutlined } from '@ant-design/icons-vue';

  /**
   * global use ref
   */
  const { t } = useI18n();
  const { createConfirm, notification } = useMessage();

  /**
   * local ref
   */
  const masterRef = ref();
  let masterGridRef = computed(() => masterRef.value?.grid);
  let masterGridSaveUrl = computed(() => masterRef.value?.gridSaveUrl);
  let buttons = ref([] as any[]);

  const detailRef = ref();
  let detailGridRef = computed(() => detailRef.value?.grid);
  let detailGridSaveUrl = computed(() => {
    return detailRef.value.gridSaveUrl.replace(':id', record.value);
  });

  const { createMessage } = useMessage();
  const record = ref();
  const recordBunble = ref();
  const route = useRoute();
  const metaUrl = computed(() => {
    return `/menu_details/${String(route.name)}/named_meta`;
  });

  const resourceUrl = computed(() => {
    return detailRef.value.resourceUrl.replace(':id', record.value);
  });
  const onClick = (selectedRow) => {
    record.value = selectedRow.id;
    recordBunble.value = selectedRow.bundle;
    detailGridRef.value.fetch({ page: 1, limit: 100, sorters: [] });
  };

  async function detailFetchHandler({
    page,
    limit,
    sorters = [{ field: 'created_at', ascending: false }],
  }) {
    if (!record.value) {
      return {
        total: 0,
        records: [],
      };
    }
    const filterCols: { name: string; operator: string; value: string; relation: boolean }[] = [];
    filterCols.push({
      name: 'id',
      operator: 'eq',
      value: record.value,
      relation: false,
    });
    let params = [
      {
        name: 'query',
        value: JSON.stringify(filterCols),
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
    // createMessage.success('조회완료!');

    return {
      total: response.total,
      records: response.items,
    };
  }

  /**
   * 행추가
   */
  const addMasterRow = () => {
    masterGridRef.value?.addRow();
  };
  const addDetailRow = () => {
    detailGridRef.value?.addRow();
  };

  /**
   * 수정
   */
  const saveMasterRows = async () => {
    let patches = masterGridRef.value.getCURows();
    if (patches.length === 0)
      return notification.error({
        message: '에러',
        description: `수정 또는 추가된 데이터가 없습니다`,
        duration: 1,
      });
    let response = await updateList(masterGridSaveUrl.value, patches);
    if (response) {
      masterGridRef.value?.fetch();
    }
  };

  const saveDetailRows = async () => {
    if (!record.value) return;
    let patches = detailGridRef.value.getCURows();
    patches = patches.map((patch) => {
      patch.entity_id = record.value;
      return patch;
    });
    if (patches.length === 0)
      return notification.error({
        message: '에러',
        description: `수정 또는 추가된 데이터가 없습니다`,
        duration: 1,
      });
    let response = await updateList(detailGridSaveUrl.value, patches);
    if (response) {
      detailGridRef.value.fetch();
    }
  };

  /**
   * 삭제
   */
  const deleteMasterRows = () => {
    let selectedRows = masterGridRef.value.getCheckedRows();
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
        let selectedActualRows = masterGridRef.value.getSelectedRowsToDelete();
        let response = await updateList(masterGridSaveUrl.value, selectedActualRows);
        if (response) {
          masterGridRef.value.fetch();
        }
      },
    });
  };
  const deleteDetailRows = () => {
    if (!record.value) return;
    let selectedRows = detailGridRef.value.getCheckedRows();
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
        let selectedActualRows = detailGridRef.value.getSelectedRowsToDelete();
        let response = await updateList(detailGridSaveUrl.value, selectedActualRows);
        if (response) {
          detailGridRef.value.fetch();
        }
      },
    });
  };

  /**
   * entity 생성
   */
  const onGenerateEntity = async () => {
    let selectedRows = masterGridRef.value.getCheckedRows();

    if (selectedRows.length > 1) {
      masterGridRef.value.uncheckAll();
      return notification.error({
        message: '에러',
        description: `한개만 선택하여 주세요`,
        duration: 1,
      });
    }

    let url = '/code_generator/code/entity/' + selectedRows[0].id;
    createConfirm({
      iconType: 'warning',
      title: () => 'Entity 생성',
      content: () => 'Entity 생성 하시겠습니까?',
      onOk: async () => {
        let response = await getCommonPostApi(url, null);
        if (response) {
          masterGridRef.value.fetch();
        }
      },
    });
  };

  /**
   * entity 삭제
   */
  const onDeleteEntity = async () => {
    let selectedRows = masterGridRef.value.getCheckedRows();
    if (selectedRows.length > 1) {
      masterGridRef.value.uncheckAll();
      return notification.error({
        message: '에러',
        description: `한개만 선택하여 주세요`,
        duration: 1,
      });
    }

    let url = '/code_generator/code/entity/' + selectedRows[0].id;
    createConfirm({
      iconType: 'warning',
      title: () => 'Entity 삭제',
      content: () => 'Entity 삭제 하시겠습니까?',
      onOk: async () => {
        let response = await getCommonDeleteApi(url, null);
        if (response) {
          masterGridRef.value.fetch();
        }
      },
    });
  };

  /**
   * 서비스 생성
   */
  const onGenerateService = async () => {
    let selectedRows = masterGridRef.value.getCheckedRows();

    if (selectedRows.length > 1) {
      masterGridRef.value.uncheckAll();
      return notification.error({
        message: '에러',
        description: `한개만 선택하여 주세요`,
        duration: 1,
      });
    }

    let url = '/code_generator/code/controller/' + selectedRows[0].id;
    createConfirm({
      iconType: 'warning',
      title: () => 'Controller 생성',
      content: () => 'Controller 생성 하시겠습니까?',
      onOk: async () => {
        let response = await getCommonPostApi(url, null);
        if (response) {
          masterGridRef.value.fetch();
        }
      },
    });
  };

  /**
   * 서비스 삭제
   */
  const onDeleteService = async () => {
    let selectedRows = masterGridRef.value.getCheckedRows();

    if (selectedRows.length > 1) {
      masterGridRef.value.uncheckAll();
      return notification.error({
        message: '에러',
        description: `한개만 선택하여 주세요`,
        duration: 1,
      });
    }

    let url = '/code_generator/code/controller/' + selectedRows[0].id;
    createConfirm({
      iconType: 'warning',
      title: () => 'Controller 삭제',
      content: () => 'Controller 삭제 하시겠습니까?',
      onOk: async () => {
        let response = await getCommonDeleteApi(url, null);
        if (response) {
          masterGridRef.value.fetch();
        }
      },
    });
  };

  /**
   * 테이블 생성
   */
  const onGenerateTable = async () => {
    let selectedRows = masterGridRef.value.getCheckedRows();

    if (selectedRows.length > 1) {
      masterGridRef.value.uncheckAll();
      return notification.error({
        message: '에러',
        description: `한개만 선택하여 주세요`,
        duration: 1,
      });
    }

    let url = '/code_generator/code/table/' + selectedRows[0].id;
    createConfirm({
      iconType: 'warning',
      title: () => 'Table 생성',
      content: () => 'Table 생성 하시겠습니까?',
      onOk: async () => {
        let response = await getCommonPostApi(url, null);
        if (response) {
          masterGridRef.value.fetch();
        }
      },
    });
  };

  /**
   * 테이블 삭제
   */
  const onDropTable = async () => {
    let selectedRows = masterGridRef.value.getCheckedRows();

    if (selectedRows.length > 1) {
      masterGridRef.value.uncheckAll();
      return notification.error({
        message: '에러',
        description: `한개만 선택하여 주세요`,
        duration: 1,
      });
    }

    let url = '/code_generator/code/table/' + selectedRows[0].id;
    createConfirm({
      iconType: 'warning',
      title: () => 'Table 삭제',
      content: () => 'Table 삭제 하시겠습니까?',
      onOk: async () => {
        let response = await getCommonDeleteApi(url, null);
        if (response) {
          masterGridRef.value.fetch();
        }
      },
    });
  };

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
        let response = await getCommonPutApi(`/entities/clear_cache`, null);
        if (response) {
          masterGridRef.value.fetch();
        }
      },
    });
  };

  onMounted(() => {
    console.log('mounted');
    buttons.value = [
      {
        name: 'generate_entity',
        text: 'button.generate_entity',
        auth: 'create',
        hidden: 0,
        listener: onGenerateEntity,
      },
      {
        name: 'delete_entity',
        text: 'button.delete_entity',
        auth: 'delete',
        hidden: 0,
        listener: onDeleteEntity,
      },
      {
        name: 'generate_service',
        text: 'button.generate_service',
        auth: 'create',
        hidden: 0,
        listener: onGenerateService,
      },
      {
        name: 'delete_service',
        text: 'button.delete_service',
        auth: 'delete',
        hidden: 0,
        listener: onDeleteService,
      },
      // {
      //   name: 'generate_table',
      //   text: 'button.generate_table',
      //   auth: 'create',
      //   hidden: 0,
      //   listener: onGenerateTable,
      // },
      // {
      //   name: 'drop_table',
      //   text: 'button.delete_table',
      //   auth: 'delete',
      //   hidden: 0,
      //   listener: onDropTable,
      // },
      {
        name: 'clear_cache',
        text: 'button.clear_cache',
        auth: 'update',
        hidden: 0,
        listener: onClearCache,
      },
      {
        name: 'addMasterRow',
        text: 'button.new',
        auth: 'create',
        hidden: 0,
        listener: addMasterRow,
      },
      {
        name: 'saveMasterRows',
        text: 'button.save',
        auth: 'create',
        hidden: 0,
        listener: saveMasterRows,
      },
      {
        name: 'deleteMasterRows',
        text: 'button.delete',
        auth: 'delete',
        hidden: 0,
        listener: deleteMasterRows,
      },
    ];
  });
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
