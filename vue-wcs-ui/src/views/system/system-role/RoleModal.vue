<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="getTitle"
    @ok="handleSubmit"
    :useWrapper="false"
  >
    <div class="modal-size">
      <Layout class="h-full flex">
        <Sider style="background: #f0f2f5; flex: 0 0 28%;" :width="280">
          <CommonPage
            ref="menuRef"
            :limit="100"
            :metaUrl="metaUrl"
            menuMetaProp="0"
            :onGridClicked="onGridClick"
            :showSearchForm="false"
            :showButtons="false"
            :fetchHandler="fetchMenuHandler"
            :optionCheck="[]"
          />
        </Sider>
        <Content style="background: #f0f2f5; flex:1;">
          <CommonPage
            ref="permRef"
            :limit="100"
            :metaUrl="metaUrl"
            menuMetaProp="1"
            :showSearchForm="false"
            :showButtons="false"
            :fetchHandler="fetchPermHandler"
            :optionCheck="[]"
          />
        </Content>
      </Layout>
    </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { useI18n } from '/@/hooks/web/useI18n';
  import { Layout } from 'ant-design-vue';

  import { ref, computed } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getCommonPostApi, getSearchList } from '/@/api/common/api';
  import { isEmpty } from '/@/utils/is';
  import CommonPage from '@/views/common/CommonPage.vue';
  import { useRoute } from 'vue-router';
  const { Sider, Content } = Layout;

  const isUpdate = ref(true);
  const { notification } = useMessage();
  const menuRef = ref();
  const menuGridRef = computed(() => menuRef.value?.grid);
  const permRef = ref();
  const gridPermRef = computed(() => permRef.value?.grid);
  // let getFormFields = computed(() => menuRef.value?.getFormFields);
  // let validate = computed(() => menuRef.value?.formValidate);
  // let resourceUrl = computed(() => menuRef.value?.resourceUrl);
  // let gridSaveUrl = computed(() => menuRef.value?.gridSaveUrl);
  const route = useRoute();
  const metaUrl = computed(() => {
    return `/menu_details/${String(route.name)}/named_meta`;
  });
  const userInfo = ref();
  const selectedMenu = ref();
  const { t } = useI18n();

  const getTitle = computed(() => t('title.menu_auth'));

  const [registerModal, { setModalProps }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });
    userInfo.value = data.selectedRow;
    menuGridRef.value.fetch();
    isUpdate.value = !!data?.isUpdate;
  });

  const emit = defineEmits(['success']);

  /* custmizing point */
  const onGridClick = (selectedRow) => {
    selectedMenu.value = selectedRow;
    gridPermRef.value.fetch({ page: 1, limit: 100, sorters: [] });
  };

  async function fetchMenuHandler({
    page,
    limit,
    sorters = [{ field: 'created_at', ascending: false }],
  }) {
    let columnList = 'id,rank,name,description,hidden_flag,category';
    let filters = [
      { name: 'menu_type', value: 'MENU' },
      { name: 'hidden_flag', operator: 'is_not_true' },
    ];

    //params
    let params = [
      {
        name: 'select',
        value: encodeURI(columnList),
      },
      {
        name: 'query',
        value: JSON.stringify(filters),
      },
      {
        name: 'page',
        value: 0,
      },
      {
        name: 'limit',
        value: 0,
      },
    ];
    let requestParams = {};
    params.forEach((item) => {
      requestParams[item['name']] = item['value'];
    });
    const response = await getSearchList('menus', requestParams);
    return {
      total: response.total,
      records: response.items,
    };
  }

  async function fetchPermHandler({
    page,
    limit,
    sorters = [{ field: 'created_at', ascending: false }],
  }) {
    const selectItem = selectedMenu.value;
    const record = userInfo.value;
    if (selectItem) {
      // 서브 메뉴 & 권한 조회
      let filters = [{ name: 'parent_id', value: selectItem.id }];
      let params = [
        {
          name: 'query',
          value: encodeURI(JSON.stringify(filters)),
        },
        {
          name: 'page',
          value: 0,
        },
        {
          name: 'limit',
          value: 0,
        },
      ];
      let requestParams = {};
      params.forEach((item) => {
        requestParams[item['name']] = item['value'];
      });
      let urlRoles = `/roles/${record.id}/permitted_resources/${selectItem.id}`;
      const response = await getSearchList(urlRoles, requestParams);

      let fetchResultSet = responseDataSet(response);
      return {
        total: fetchResultSet.total,
        records: fetchResultSet.records,
      };
    } else {
      return {
        total: 0,
        records: [],
      };
    }
  }

  /**
   * 데이터 변경
   ***********************************
   * @param {Object} response
   * @returns
   */
  const responseDataSet = (response) => {
    let mergeResult = { total: response.items.length, records: [] } as any;
    let idMergeObject = {} as any;
    if (response && response.items.length > 0) {
      response.items.forEach((record) => {
        if (isEmpty(idMergeObject[record.id])) {
          idMergeObject[record.id] = {
            menu_id: record.id,
            parent_id: record.parent_id,
            name: record.name,
            title: t(`menu.${record.name}`),
            show: false,
            update: false,
            create: false,
            delete: false,
          };
        }

        if (record.action_name) {
          idMergeObject[record.id][record.action_name] = true;
        }
      });

      Object.keys(idMergeObject).forEach((key) => {
        mergeResult.records.push(idMergeObject[key]);
      });
      console.log(mergeResult);
    } else {
      mergeResult.total = 0;
      mergeResult.records = [];
    }

    return mergeResult;
  };

  async function handleSubmit() {
    try {
      setModalProps({ confirmLoading: true });
      // TODO custom api
      const records = gridPermRef.value.getData();
      const roleInfo = userInfo.value;
      const selectItem = selectedMenu.value;

      // 변경된 내용이 없음
      if (!records || records.length == 0) {
        return notification.error({
          message: t('title.info'),
          description: t('text.NOTHING_CHANGED'),
          duration: 1,
        });
      } else {
        let patchDataList = records
          .filter((x) => x.show == true || x.create == true || x.update == true || x.delete == true)
          .map((data) => {
            let patchData = {
              menu_id: data.menu_id,
              parent_id: data.parent_id,
            } as any;

            if (data.show === true) patchData.show = true;
            if (data.create === true) patchData.create = true;
            if (data.update === true) patchData.update = true;
            if (data.delete === true) patchData.delete = true;

            return patchData;
          });
        // 여러 건의 레코드에 대해서 insert / update 동시 요청
        let url = `/roles/${roleInfo.id}/update_permissions?parent_menu_id=${selectItem.id}`;
        url += isEmpty(patchDataList) ? '&delete_all=true' : '';
        var response = await getCommonPostApi(url, patchDataList);
        if (response && response.success) {
          notification.info({
            message: t('title.info'),
            description: t('text.Success_to_Save'),
            duration: 1,
          });
          gridPermRef.value.fetch();
        }
      }
      emit('success');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
<style lang="less" scoped>
  .modal-size {
    height: 500px;
    width: 80vw;
    background-color: @component-background;
  }

  .tui-grid-cell-content {
    text-align: center;
  }
</style>
