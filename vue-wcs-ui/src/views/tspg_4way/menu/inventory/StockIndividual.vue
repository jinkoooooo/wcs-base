<template>
  <div class="stock-individual-page">
    <CommonPage
      ref="commPageRef"
      :limit="50"
      :showPagination="true"
      :showSummary="true"
      :summaryColumns="['item_qty']"
      :fetchHandler="fetchHandler"
    >
      <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btnHandler="btnHandler" />
    </CommonPage>
  </div>
</template>

<script lang="ts" setup>
import 'tui-grid/dist/tui-grid.css';
import { ref, computed } from 'vue';
import CommonPage from '../../common/CommonPage.vue';
import { getSearchList } from '/@/api/common/api';
import { useMessage } from '/@/hooks/web/useMessage';
import ButtonGroup from '/src/views/common/ButtonGroup.vue';
import { getFormattedFilters } from '/src/views/tspg_4way/common/utils';

const { notification } = useMessage();

// =====================================================
// CommonPage refs
// =====================================================
const commPageRef = ref(null as any);
const getFormFields = computed(() => commPageRef.value?.getFormFields);
const validate = computed(() => commPageRef.value?.formValidate);
const buttonlist = computed(() => commPageRef.value?.buttons);

// =====================================================
// 조회 — GET /rest/wcs/inventory/stock-individual
// =====================================================
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

    const resp = await getSearchList('/wcs/inventory/stock-individual', {
      query: JSON.stringify(queryFilters),
      sort: JSON.stringify(sorters),
      page,
      limit,
    });

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
    console.error('[StockIndividual] ERROR:', e);
    notification.error({
      message: '오류',
      description: e?.message || '조회 중 오류가 발생했습니다.',
      duration: 2,
    });
    return { total: 0, records: [] };
  }
}

// =====================================================
// 메타 버튼 핸들러 (엑셀다운로드, 조회 등)
// =====================================================
async function btnHandler(listenerName: any) {
  const handlers: Record<string, () => void> = {
    exportBtnHandler: () => commPageRef.value?.downExcel(),
    exceldownBtnHandler: () => commPageRef.value?.downExcel(),
    searchBtnHandler: () => commPageRef.value?.grid?.fetch(),
  };
  const handler = handlers[listenerName];
  if (handler) handler();
}
</script>

<style scoped>
.stock-individual-page {
  height: 100%;
}
</style>
