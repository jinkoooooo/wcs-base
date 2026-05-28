<template>
  <PageWrapper>
    <div class="h-99">
      <CommonPage @gridClicked="onClick" :baseColProps="{ xxl: 12, lg: 24, md: 24, sm: 24 }" />
    </div>
    <CommonPage
      ref="detailRef"
      :limit="100"
      :metaUrl="metaUrl"
      menuMetaProp="0"
      :showSearchForm="false"
      :fetchHandler="detailFetchHandler"
    />
  </PageWrapper>
</template>
<script lang="ts" setup>
  import { ref, unref, computed } from 'vue';
  import { useRoute } from 'vue-router';
  import { useMessage } from '/@/hooks/web/useMessage';

  import { getSearchList } from '/@/api/common/api';

  import { PageWrapper } from '/@/components/Page';
  import CommonPage from '/@/views/common/CommonPage.vue';
  const { createMessage } = useMessage();
  const detailRef = ref();
  const stationCd = ref();
  const route = useRoute();
  const metaUrl = computed(() => {
    return `/menu_details/${String(route.name)}/named_meta`;
  });
  const onClick = (selectedRow) => {
    stationCd.value = selectedRow.station_cd;
    let gridRef = detailRef.value.grid;
    gridRef.fetch({ page: 1, limit: 100, sorters: [] });
  };

  async function detailFetchHandler({
    page,
    limit,
    sorters = [{ field: 'created_at', ascending: false }],
  }) {
    try {
      if (!stationCd.value) {
        return {
          total: 0,
          records: [],
        };
      }
      const filterCols: { name: string; operator: string; value: string; relation: boolean }[] = [];
      filterCols.push({
        name: 'station_cd',
        operator: 'eq',
        value: stationCd.value,
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
      let detailView = unref(detailRef);
      const response = await getSearchList(detailView.resourceUrl, requestParams);
      createMessage.success('조회완료!');

      return {
        total: response.total,
        records: response.items,
      };
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
