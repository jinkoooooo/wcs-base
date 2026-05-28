<template>
  <PageWrapper>
    <Layout class="gap-1">
      <Sider width="50%">
        <CommonPage
          ref="masterRef"
          @gridClicked="onClick"
          :baseColProps="{ xxl: 12, lg: 24, md: 24, sm: 24 }"
        >
          <Button type="primary" @click="onClearCache"> {{ t('button.clear_cache') }}</Button>
          <Button type="primary" @click="addMasterRow">{{ t('button.new') }}</Button>
          <Button type="primary" @click="saveMasterRows">{{ t('button.save') }}</Button>
          <Button danger @click="deleteMasterRows">{{ t('button.delete') }}</Button>
        </CommonPage>
      </Sider>
      <Content>
        <CommonPage
          ref="detailRef"
          :limit="100"
          :metaUrl="metaUrl"
          menuMetaProp="0"
          :showSearchForm="false"
          :fetchHandler="detailFetchHandler"
        >
          <Button type="primary" @click="addDetailRow">{{ t('button.new') }}</Button>
          <Button type="primary" @click="saveDetailRows">{{ t('button.save') }}</Button>
          <Button danger @click="deleteDetailRows">{{ t('button.delete') }}</Button>
        </CommonPage>
      </Content>
    </Layout>
  </PageWrapper>
</template>
<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { Button, Layout } from 'ant-design-vue';
  import { useRoute } from 'vue-router';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';

  import { updateList, getSearchList, getCommonPutApi } from '/@/api/common/api';

  import { PageWrapper } from '/@/components/Page';
  import CommonPage from '@/views/common/CommonPage.vue';

  /**
   * global use ref
   */
  const { t } = useI18n();
  const { createConfirm, notification } = useMessage();

  /**
   * local ref
   */
  const { Sider, Content } = Layout;
  const masterRef = ref();
  let masterGridRef = computed(() => masterRef.value?.grid);
  let masterGridSaveUrl = computed(() => masterRef.value?.gridSaveUrl);

  const detailRef = ref();
  let detailGridRef = computed(() => detailRef.value?.grid);
  let detailGridSaveUrl = computed(() => {
    return detailRef.value.gridSaveUrl.replace(':id', record.value);
  });

  const record = ref();
  const route = useRoute();
  const metaUrl = computed(() => {
    return `/menu_details/${String(route.name)}/named_meta`;
  });

  const resourceUrl = computed(() => {
    return detailRef.value.resourceUrl.replace(':id', record.value);
  });
  const onClick = (selectedRow) => {
    record.value = selectedRow.id;
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
      patch.parent_id = record.value;
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
   * menu cache clear
   */
  const onClearCache = async () => {
    createConfirm({
      iconType: 'warning',
      title: () => '동기화',
      content: () =>
        '케시 삭제하면 본 도메인에서 기존에 등록된 모든 메뉴의 케시를 재 등록하게 됩니다. 그래도 진행하시겠습니까?',
      onOk: async () => {
        let response = await getCommonPutApi(`/common_codes/clear_cache`, null);
        if (response) {
          masterGridRef.value.fetch();
        }
      },
    });
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
