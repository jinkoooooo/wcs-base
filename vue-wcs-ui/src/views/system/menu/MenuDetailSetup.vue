<template>
  <div class="menu-detail h-full">
    <div class="flex-1 menu-detail-top">
      <CommonPage
        menuName="MenuDetail"
        ref="detailListRef"
        :resourceId="props.menuId"
        :showSearchForm="false"
        :showPagination="false"
        :fetchHandler="detailListFetchHandler"
        @gridClicked="onGridClicked"
        :limit="100"
      >
        <Button type="primary" @click="addRow(unref(detailListRef))">{{ t('button.new') }}</Button>
        <Button type="primary" @click="saveRows(unref(detailListRef))">{{
          t('button.save')
        }}</Button>
        <Button danger @click="deleteRows(unref(detailListRef))">{{ t('button.delete') }}</Button>
        <Button danger @click="onSync()">{{ t('button.sync') }}</Button>
      </CommonPage>
    </div>
    <div class="menu-detail-bottom">
      <Tabs class="h-full menu-detail-bottom" :onChange="onTabChange" :tabBarGutter="2" type="card">
        <template v-for="item in menuDetailMetas" :key="item.key">
          <TabPane :tab="item.name" class="h-full">
            <CommonPage
              :ref="
                (el)=> {
                  if (!currPageProp || Object.keys(currPageProp).length === 0) {
                    nextTick(function () {
                      currPageProp = el;
                    });
                  }
                  pageRefs[item.key.slice(-1)] = el;
                }
              "
              class="flex-2"
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
    </div>
  </div>
</template>
<script lang="ts" setup>
  import { Tabs, TabPane } from 'ant-design-vue';
  import { ref, computed, unref, watch } from 'vue';
  import { getCommonPostApi, getCommonPutApi, getSearchList } from '/@/api/common/api';
  import CommonPage from '@/views/common/CommonPage.vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { updateList } from '/@/api/common/api';
  import { Button } from 'ant-design-vue';
  import { nextTick } from 'vue';

  const { t } = useI18n();
  const { createConfirm, notification } = useMessage();
  const emit = defineEmits(['success', 'register']);
  // setup(_, { emit }) {
  const isUpdate = ref(true);
  let currPageProp = {} as any;
  let pageRefs = [] as any[];
  const prefixCls = 'menu-detail';

  const props = defineProps({
    menuId: { type: String },
  });
  const menuDetailId = ref();
  const detailListRef = ref();
  let menuDetailMetas = computed(() => {
    return menuDetailId.value
      ? [
          {
            key: menuDetailId.value + '0',
            name: t('title.setup_columns'),
            menuName: 'MenuDetailColumn',
            resourceId: menuDetailId.value,
          },
          {
            key: menuDetailId.value + '1',
            name: t('title.setup_searching'),
            menuName: 'MenuDetailSearchForm',
            resourceId: menuDetailId.value,
          },
          {
            key: menuDetailId.value + '2',
            name: t('title.setup_grid'),
            menuName: 'MenuDetailGrid',
            resourceId: menuDetailId.value,
          },
          {
            key: menuDetailId.value + '3',
            name: t('title.setup_button'),
            menuName: 'MenuDetailButton',
            resourceId: menuDetailId.value,
          },
        ]
      : [];
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '메뉴추가' : '메뉴변경'));

  const onGridClicked = (menuDetail) => {
    if (menuDetail) {
      menuDetailId.value = menuDetail?.id;
    }
  };
  const onTabChange = (key) => {
    nextTick(function () {
      currPageProp = pageRefs[key.slice(-1)];
    });
  };
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
        let response = await getCommonPostApi(
          `/menu_details/${menuDetailId.value}/menu_detail_columns/sync_with_entity_columns`,
          null,
        );
        if (response) {
          onClearCache();
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
        await getCommonPutApi(`/menus/clear_cache`, null);
      },
    });
  };
  /**
   * 꼭 필요한 경우에만 생성한다. 필요하지 않은 경우에는 삭제하고 common page의 Default기능을 사용한다.
   * @param page page번호
   * @param limit page limit
   * @param sorters sortfield
   * @param searchProps search properties
   */
  async function detailListFetchHandler(
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
    const response = await getSearchList(detailListRef.value.resourceUrl, requestParams);
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
    if (currPageProp.menuName == 'MenuDetailSearchForm') {
      queryFilters = searchFormFilter;
    } else if (currPageProp.menuName == 'MenuDetailGrid') {
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
    const response = await getSearchList(currPageProp.resourceUrl, requestParams);
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
  const addRow = (currPage?) => {
    currPage = currPage ? currPage : currPageProp;
    currPage.grid.addRow();
  };

  /**
   * 수정
   */
  const saveRows = async (currPage?) => {
    currPage = currPage ? currPage : currPageProp;
    let patches = currPage.grid.getCURows();
    if (patches.length === 0)
      return notification.error({
        message: '에러',
        description: `수정 또는 추가된 데이터가 없습니다`,
        duration: 1,
      });
    for (let patch of patches) {
      patch.menu_id = props.menuId;
      patch.menu_detail_id = menuDetailId.value;
    }
    let response = await updateList(currPage.gridSaveUrl, patches);
    if (response) {
      currPage.grid.fetch();
    }
  };

  /**
   * 삭제
   */
  const deleteRows = (currPage?) => {
    currPage = currPage ? currPage : currPageProp;
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

  defineExpose({
    prefixCls,
    getTitle,
    addRow,
    deleteRows,
    saveRows,
    t,
  });
</script>
<style lang="less">
  .ant-tabs-content {
    display: flex;
    width: 100%;
    height: 100%;
  }
</style>
<style lang="less" scoped>
  @prefix-cls: ~'menu-detail';
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
      flex: 3;
      margin: 0 16px 16px;
      padding: 10px;
      border-radius: 3px;
      background-color: @component-background;
    }
  }
</style>
