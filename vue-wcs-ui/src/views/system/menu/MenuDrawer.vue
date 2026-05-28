<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    :showFooter="false"
    :title="getTitle"
    width="100%"
    height="100%"
  >
    <div :class="`${prefixCls}`">
      <Tabs class="h-full" :tabBarGutter="2" type="card">
        <TabPane :tab="t('title.main_menu')" class="h-full" key="{{resourceId}}-master">
          <Tabs class="h-full" :onChange="onTabChange" :tabBarGutter="2" type="card">
            <TabPane :tab="t('label.menu')" class="h-full">
              <BasicForm class="h-full gap-3" @register="registerForm">
                <template #formFooter>
                  <div
                    class="ant-col ant-col-24 gap-2"
                    style="display: flex !important; flex: 1; justify-content: end"
                  >
                    <Button type="primary" @click="onSync()">{{ t('button.sync') }}</Button>
                    <Button type="primary" @click="onClearCache()">{{
                      t('button.clear_cache')
                    }}</Button>
                    <Button type="primary" @click="handleSubmit">{{ t('button.save') }}</Button>
                  </div>
                </template>
              </BasicForm>
            </TabPane>
            <template v-for="item in menuMetas" :key="item.key">
              <TabPane :tab="item.name" class="h-full">
                <CommonPage
                  :ref="
                    (el) => {
                      if (!currPage || Object.keys(currPage).length === 0) {
                        nextTick(function () {
                          currPage = el;
                        });
                      }
                      pageRefs[item.key.slice(-1)] = el;
                    }
                  "
                  class="flex-1"
                  :is="item.component"
                  :menuName="item.menuName"
                  :resourceId="item.resourceId"
                  :showSearchForm="false"
                  :showPagination="false"
                  :fetchHandler="fetchHandler"
                  :limit="100"
                >
                  <Button type="primary" @click="addRow()">{{ t('button.new') }}</Button>
                  <Button type="primary" @click="saveRows()">{{ t('button.save') }}</Button>
                  <Button danger @click="deleteRows()">{{ t('button.delete') }}</Button>
                </CommonPage>
              </TabPane>
            </template>
          </Tabs>
        </TabPane>
        <TabPane :tab="t('title.detail')" class="h-full" key="{{resourceId}}-detail">
          <MenuDetailSetup :menuId="resourceId" :key="resourceId"></MenuDetailSetup>
        </TabPane>
      </Tabs>
    </div>
  </BasicDrawer>
</template>
<script lang="ts" setup>
  import { Tabs, TabPane } from 'ant-design-vue';
  import { ref, computed, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { getCommonPostApi, getCommonPutApi, getSearchList } from '/@/api/common/api';
  import CommonPage from '@/views/common/CommonPage.vue';
  import MenuDetailSetup from './MenuDetailSetup.vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { updateList } from '/@/api/common/api';
  import { formSchema } from './menu.drawer.data';
  import { Button } from 'ant-design-vue';
  import { nextTick } from 'vue';

  const { t } = useI18n();
  const { createConfirm, notification } = useMessage();
  const emit = defineEmits(['success', 'register']);
  // setup(_, { emit }) {
  const isUpdate = ref(true);
  let menuMetas = ref();
  let currPage = {} as any;
  let pageRefs = [] as any[];
  const prefixCls = 'menu-drawer';
  const resourceId = ref('');
  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 150,
    schemas: formSchema,
    showActionButtonGroup: false,
    baseColProps: { xxl: 4, lg: 6, md: 12, sm: 24 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    resetFields();
    setDrawerProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;

    if (unref(isUpdate)) {
      setFieldsValue({
        ...data.record,
      });
      resourceId.value = data.record.id;
      let metas = [
        {
          key: data.record.id + '0',
          name: t('title.setup_columns'),
          menuName: 'MenuColumns',
          resourceId: data.record.id,
        },
        {
          key: data.record.id + '1',
          name: t('title.setup_searching'),
          menuName: 'MenuSearchForm',
          resourceId: data.record.id,
        },
        {
          key: data.record.id + '2',
          name: t('title.setup_grid'),
          menuName: 'MenuGrid',
          resourceId: data.record.id,
        },
        {
          key: data.record.id + '3',
          name: t('title.setup_button'),
          menuName: 'MenuButtons',
          resourceId: data.record.id,
        },
      ];
      menuMetas.value = metas;
    }
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '메뉴추가' : '메뉴변경'));

  const onTabChange = (key) => {
    nextTick(function () {
      currPage = pageRefs[key.slice(-1)];
    });
  };

  /**
   * 꼭 필요한 경우에만 생성한다. 필요하지 않은 경우에는 삭제하고 common page의 Default기능을 사용한다.
   * @param page page번호
   * @param limit page limit
   * @param sorters sortfield
   * @param searchProps search properties
   */
  async function fetchHandler(
    page,
    limit,
    sorters, // = [{ field: 'created_at', ascending: false }],
    searchProps,
  ) {
    let queryFilters: {
      name: string;
      operator: string;
      value?: string | boolean;
      relation?: boolean;
    }[] = [];
    const searchFormFilter = [
      {
        name: 'search_rank',
        operator: 'gt',
        value: '0',
      },
      {
        name: 'search_rank',
        operator: 'is_not_null',
      },
    ];

    const gridFilter = [
      {
        name: 'grid_rank',
        operator: 'gt',
        value: '0',
      },
      {
        name: 'grid_rank',
        operator: 'is_not_null',
      },
    ];

    if (currPage.menuName == 'MenuSearchForm') {
      queryFilters = searchFormFilter;
    } else if (currPage.menuName == 'MenuGrid') {
      queryFilters = gridFilter;
    }
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

    // 문자열을 '/' 기준으로 분할
    let parts = currPage.resourceUrl.split('/');
    // 분할된 배열의 중간 값(인덱스 1) 변경
    parts[1] = resourceId.value;
    // 배열을 다시 '/' 기준으로 합치기
    let replacedUrl = parts.join('/');
    const response = await getSearchList(replacedUrl, requestParams);
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
   * 칼럼동기화
   */
  const onSync = async () => {
    createConfirm({
      iconType: 'warning',
      title: () => '동기화',
      content: () =>
        '동기화 시 화면에 설정되어 있는 칼럼과 검색정보가 초기화 될 수 있는데 그래도 진행하시겠습니까?',
      onOk: async () => {
        let response = await getCommonPostApi(`/menus/${resourceId.value}/sync_menu_columns`, null);
        if (response) {
          closeDrawer();
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
        '동기화 시 화면에 설정되어 있는 칼럼과 검색정보가 초기화 될 수 있는데 그래도 진행하시겠습니까?',
      onOk: async () => {
        let response = await getCommonPutApi(`/menus/clear_cache`, null);
        if (response) {
          closeDrawer();
        }
      },
    });
  };
  /**
   * 행추가
   */
  const addRow = () => {
    currPage.grid.addRow();
  };

  /**
   * 수정
   */
  const saveRows = async () => {
    let patches = currPage.grid.getCURows();
    if (patches.length === 0)
      return notification.error({
        message: '에러',
        description: `수정 또는 추가된 데이터가 없습니다`,
        duration: 1,
      });
    patches.forEach((patch) => {
      patch.menu_id = resourceId.value;
    });
    let response = await updateList(currPage.gridSaveUrl, patches);
    if (response) {
      currPage.grid.fetch();
    }
  };

  /**
   * 삭제
   */
  const deleteRows = () => {
    let selectedRows = currPage.grid.getCheckedRows();
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
        let selectedActualRows = currPage.grid.getSelectedRowsToDelete();
        let response = await updateList(currPage.gridSaveUrl, selectedActualRows);
        if (response) {
          currPage.grid.fetch();
        }
      },
    });
  };
  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });
      if (!unref(isUpdate)) {
        values.hidden_flag = values.hidden_flag == '0' ? false : true;
        await getCommonPostApi('/menus', values);
      } else {
        values.hidden_flag = values.hidden_flag == '0' ? false : true;
        await getCommonPutApi(`/menus/ + ${values.id}`, values);
      }
      closeDrawer();
      emit('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }

  defineExpose({
    prefixCls,
    menuMetas,
    registerDrawer,
    registerForm,
    getTitle,
    handleSubmit,
    addRow,
    deleteRows,
    saveRows,
    t,
  });
  // },
  // });
</script>
<style lang="less">
  .ant-tabs-content {
    display: flex;
    width: 100%;
    height: 100%;
  }
</style>
<style lang="less" scoped>
  @prefix-cls: ~'menu-drawer';
  // .ant-tabs-tab-active {
  //   color: #fff;
  //   background-color: #0960bd;
  //   background: #0960bd;
  // }
  .@{prefix-cls} {
    display: flex;
    flex: 1;
    flex-direction: column;
    &-col:not(:last-child) {
      padding: 0 10px;

      &:not(:last-child) {
        border-right: 1px dashed rgb(206 206 206 / 50%);
      }
    }
    &-top {
      margin: 16px 16px 12px;
      padding: 10px;
      border-radius: 3px;
      background-color: @component-background;
    }

    &-bottom {
      display: flex;
      flex-direction: column;
      flex: 1;
      margin: 0 16px 16px;
      padding: 10px;
      border-radius: 3px;
      background-color: @component-background;
    }
  }
</style>
