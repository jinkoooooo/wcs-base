<template>
  <div class="cell-usage-page">
    <CommonPage
      ref="commPageRef"
      :limit="20"
      :showPagination="true"
      :showSummary="true"
      :summaryColumns="['total_cells', 'used_cells', 'empty_cells', 'work_cells', 'forbid_cells', 'usage_rate']"
      :summaryColumnOverrides="summaryOverrides"
      :fetchHandler="fetchHandler"
      @gridFetched="handleGridFetched"
    >
      <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btnHandler="btnHandler" />
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

/** 마지막 조회 결과 — usage_rate 가중평균 계산용 */
const lastRecords = ref<any[]>([]);

const summaryOverrides = {
  usage_rate: () => {
    const total = lastRecords.value.reduce((s, r) => s + Number(r.total_cells || 0), 0);
    const used = lastRecords.value.reduce((s, r) => s + Number(r.used_cells || 0), 0);
    return total === 0 ? '0.00' : (used * 100 / total).toFixed(2);
  },
};

const handleGridFetched = () => {
  fetchStore.isUpdatingRows = false;
};

/**
 * 커스텀 fetchHandler
 * - InboundResult 와 동일한 query/sort/page/limit 규약 사용
 * - 백엔드(/wcs/inventory/cell-usage/summary) 는 AbstractSubqueryPagedService 기반으로 {total, items} 반환
 */
async function fetchHandler(
  page: number,
  limit: number,
  sorters: any[],
  searchProps: any[],
) {
  try {
    try { await validate.value(); } catch (_) { /* outOfDate 무시 */ }
    const fields = getFormFields.value();

    const queryFilters = await getFormattedFilters(fields, searchProps);

    const requestParams: Record<string, any> = {
      query: JSON.stringify(queryFilters),
      sort: JSON.stringify(sorters),
      page,
      limit,
    };

    const resp = await getSearchList('/wcs/inventory/cell-usage/summary', requestParams);

    let records: any[] = [];
    let total = 0;
    if (resp && resp.items) {
      records = resp.items;
      total = resp.total;
    } else if (Array.isArray(resp)) {
      records = resp;
      total = resp.length;
    }

    lastRecords.value = records;
    return { total, records };
  } catch (e: any) {
    console.error('[CellUsage] ERROR:', e);
    notification.error({
      message: '오류',
      description: e?.message || '조회 중 오류가 발생했습니다.',
      duration: 2,
    });
    lastRecords.value = [];
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
.cell-usage-page {
  height: 100%;
}
</style>
