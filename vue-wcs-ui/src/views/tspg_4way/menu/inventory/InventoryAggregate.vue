<template>
  <div class="inventory-aggregate-page">
    <CommonPage
      ref="commPageRef"
      :limit="20"
      :showPagination="true"
      :showSummary="true"
      :summaryColumns="['total_qty', 'pallet_qty']"
      :fetchHandler="fetchHandler"
      @grid-fetched="handleGridFetched"
    >
      <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btn-handler="btnHandler" />
    </CommonPage>
  </div>
</template>

<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { ref, computed } from 'vue';
  import CommonPage from '../../common/CommonPage.vue';
  import { getSearchList } from '/src/api/common/api';
  import { useMessage } from '/src/hooks/web/useMessage';
  import { useFetchStore } from '/src/store/modules/fetchStore';
  import ButtonGroup from '/src/views/common/ButtonGroup.vue';
  import { getFormattedFilters } from '/src/views/tspg_4way/common/utils';

  const { notification } = useMessage();
  const fetchStore = useFetchStore();

  const commPageRef = ref(null as any);
  const getFormFields = computed(() => commPageRef.value?.getFormFields);
  const validate = computed(() => commPageRef.value?.formValidate);
  const buttonlist = computed(() => commPageRef.value?.buttons);

  const handleGridFetched = () => {
    fetchStore.isUpdatingRows = false;
  };

  /**
   * 커스텀 fetchHandler
   * - InboundResult 와 동일한 query/sort/page/limit 규약 사용
   * - 백엔드(/wcs/inventory/aggregate/summary) 는 AbstractSubqueryPagedService 기반으로 {total, items} 반환
   */
  async function fetchHandler(page: number, limit: number, sorters: any[], searchProps: any[]) {
    try {
      try {
        await validate.value();
      } catch (_) {
        /* outOfDate 무시 */
      }
      const fields = getFormFields.value();

      const queryFilters = await getFormattedFilters(fields, searchProps);

      const requestParams: Record<string, any> = {
        query: JSON.stringify(queryFilters),
        sort: JSON.stringify(sorters),
        page,
        limit,
      };

      const resp = await getSearchList('/wcs/inventory/aggregate/summary', requestParams);

      let records: any[] = [];
      let total = 0;
      if (resp && resp.items) {
        records = resp.items;
        total = resp.total;
      } else if (Array.isArray(resp)) {
        records = resp;
        total = resp.length;
      }

      return { total, records };
    } catch (e: any) {
      console.error('[InventoryAggregate] ERROR:', e);
      notification.error({
        message: '오류',
        description: e?.message || '조회 중 오류가 발생했습니다.',
        duration: 2,
      });
      return { total: 0, records: [] };
    }
  }

  async function btnHandler(listenerName: any) {
    const handlers: Record<string, () => void> = {
      exportBtnHandler: () => commPageRef.value?.downExcel(),
      exceldownBtnHandler: () => commPageRef.value?.downExcel(),
    };
    const handler = handlers[listenerName];
    if (handler) handler();
  }
</script>

<style scoped>
  .inventory-aggregate-page {
    height: 100%;
  }
</style>
