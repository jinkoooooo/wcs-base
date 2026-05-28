<template>
  <PageWrapper>
    <Layout class="gap-1">
      <Sider width="60%">
        <CommonPage
          ref="commPageRef"
          @gridClicked="onGridClick"
          @gridChecked="onGridChecked"
          :baseColProps="{ xxl: 8, lg: 24, md: 24, sm: 24 }"
        >

          <Button @click="addRow" type="primary">{{ t('button.add') }}</Button>
          <Button
            v-for="button in buttons"
            :key="button.text"
            @click="button.listener"
            :type="button.auth === 'delete' ? 'dashed' : 'primary'"
            :danger="button.auth === 'delete'"
            :hidden="button.hidden == 1"
            :value="button.text"
          >
            <template #icon v-if="button.auth === 'export'">
              <DownloadOutlined />
            </template>

            <template #icon v-if="button.auth === 'import'">
              <UploadOutlined />
            </template>
            {{ t(button.title) }}</Button>
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
          <Button @click="saveUserDomain" type="primary">{{ t('button.save') }}</Button>
        </CommonPage>
      </Content>
    </Layout>
    <PasswordChangeModal @register="registerModal" />
  </PageWrapper>
</template>
<script lang="ts" setup>
  import { onMounted, ref, unref, computed } from 'vue';
  import { useRoute } from 'vue-router';
  import { Layout } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button } from 'ant-design-vue';
  import { getSearchList, updateList, getCommonPutApi } from '/@/api/common/api';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useModal } from '/@/components/Modal';

  import { PageWrapper } from '/@/components/Page';
  import CommonPage from '@/views/common/CommonPage.vue';
  import PasswordChangeModal from './PasswordChangeModal.vue';
  import { DownloadOutlined, UploadOutlined } from '@ant-design/icons-vue';

  const { Sider, Content } = Layout;
  const commPageRef = ref(null as any);
  let gridRef = computed(() => commPageRef.value?.grid);
  let gridSaveUrl = computed(() => commPageRef.value?.gridSaveUrl);

  const detailRef = ref(null as any);
  let detailGridRef = computed(() => detailRef.value?.grid);
  let detailGridSaveUrl = computed(() => detailRef.value?.gridSaveUrl);

  const selectedRows = ref();

  const { createMessage, createConfirm, notification } = useMessage();
  const { t } = useI18n();

  const selected = ref();
  const route = useRoute();
  const buttons = ref([] as any[]);

  const [registerModal, { openModal }] = useModal();

  const metaUrl = computed(() => {
    return `/menu_details/${String(route.name)}/named_meta`;
  });

  //LifeCycle
  onMounted(() => {
    buttons.value = [
      {
        name: 'activateAccount',
        title: 'button.activate_account',
        auth: 'update',
        listener: activateAccount,
      },
      {
        name: 'lockAccount',
        title: 'button.lock_account',
        auth: 'update',
        listener: lockAccount,
      },
      {
        name: 'passwordChange',
        title: 'button.password_change',
        auth: 'update',
        listener: passwordChange,
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
  /* custmizing point */
  const onGridClick = (selectedRow) => {
    selected.value = selectedRow;
    let gridRef = detailRef.value.grid;
    gridRef.fetch({ page: 1, limit: 100, sorters: [] });
  };

  const onGridChecked = (checkeds) => {
    if (checkeds && checkeds.length > 1) {
      selectedRows.value = [];
      createMessage.info(t('error.MORE_THAN_ONE_ERROR'));
      return;
    }
    selectedRows.value = checkeds;
  };

  const addRow = () => {
    gridRef.value.addRow();
  };
  const activateAccount = () => {
    doTransaction('users/:id/release_lock');
  };
  const lockAccount = () => {
    doTransaction('users/:id/lock');
  };
  const passwordChange = () => {
    let selectedList = selectedRows.value;
    if (!selectedList || selectedList.length == 0) {
      createMessage.info(t('text.NOTHING_SELECTED'));
    } else {
      if (selectedList.length > 1) {
        createMessage.info(t('error.MORE_THAN_ONE_ERROR'));
      }
      openModal(true, {
        record: selectedList[0],
        isUpdate: true,
      });
    }
  };
  const doTransaction = async (url: String) => {
    let selectedList = selectedRows.value;
    if (!selectedList || selectedList.length == 0) {
      notification.info({
        message: t('title.info'),
        description: t('text.NOTHING_SELECTED'),
        duration: 3,
      });
    } else {
      let record = selectedList[0];
      if (!record.active_flag) {
        createConfirm({
          iconType: 'info',
          title: () => t('title.confirm'),
          content: () => t('text.Sure to process'),
          onOk: async () => {
            url = url.replace(':id', record.id);
            let response = await getCommonPutApi(url, null);
            if (response) {
              notification.info({
                message: '성공',
                description: t('text.Success to Process'),
                duration: 3,
              });
              gridRef.value.fetch();
            }
          },
        });
      }
    }
  };
  const saveRows = async () => {
    let patches = gridRef.value.getCURows();
    if (patches.length === 0)
      return notification.error({
        message: '에러',
        description: `수정 또는 추가된 데이터가 없습니다`,
        duration: 1,
      });
    //  if (validateLogins(patches)) {
    let response = await updateList(gridSaveUrl.value, patches);
    if (response) {
      gridRef.value.fetch();
      //      }
    }
  };

  const validateLogins = (records) => {
    let validAll = true;

    records.forEach((record) => {
      if (!validateLogin(record.login)) {
        validAll = false;
      }
    });

    return validAll;
  };

  const validateLogin = (login) => {
    let regExp = /^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}$/i;
    let valid = login != '' && login != 'undefined' && regExp.test(login);
    if (!valid) {
      createMessage.info(t('text.email_error'));
    }

    return valid;
  };

  const deleteRows = () => {
    if (selectedRows.value.length === 0)
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
  const saveUserDomain = async () => {
    let cudRecords = detailGridRef.value.getCURows();

    // 변경된 내용이 없음
    if (!cudRecords || cudRecords.length == 0) {
      createMessage.info(t('text.NOTHING_CHANGED'));
      return;
    }

    // 변경된 내용 추출
    let patches = cudRecords.map((record) => {
      record.user_id = selected.value.login;
      record.cud_flag_ = record.has_permission == true ? 'c' : 'd';
      return record;
    });

    // 변경된 내용 업데이트 처리
    let response = await updateList(detailGridSaveUrl.value, patches);
    if (response) {
      detailGridRef.value.fetch();
    }
  };
  async function detailFetchHandler({
    page,
    limit,
    sorters = [{ field: 'created_at', ascending: false }],
  }) {
    try {
      if (!selected.value) {
        return {
          total: 0,
          records: [],
        };
      }
      const filterCols: { name: string; operator: string; value: string; relation: boolean }[] = [];
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
      let detailView = unref(detailRef);
      let requestUrl = detailView.resourceUrl.replace(':id', selected.value.login);
      const response = await getSearchList(requestUrl, requestParams);
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
    } catch (error) {}
  }
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
