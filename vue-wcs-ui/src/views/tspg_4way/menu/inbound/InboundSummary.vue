<template>
  <div class="inbound-summary-page">
    <CommonPage
      ref="commPageRef"
      :limit="20"
      :showPagination="true"
      :showSummary="true"
      :summaryColumns="['inbound_qty']"
      :fetchHandler="fetchHandler"
    >
      <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btnHandler="btnHandler" />
    </CommonPage>
  </div>
</template>

<script lang="ts" setup>
import 'tui-grid/dist/tui-grid.css';
import { ref, computed, nextTick } from 'vue';
import CommonPage from '../../common/CommonPage.vue';
import { getSearchList } from '/@/api/common/api';
import { useMessage } from '/@/hooks/web/useMessage';
import { useFetchStore } from '/@/store/modules/fetchStore';
import ButtonGroup from '/src/views/common/ButtonGroup.vue';
import { getFormattedFilters } from '/src/views/tspg_4way/common/utils';

const { notification } = useMessage();
const fetchStore = useFetchStore();

const commPageRef = ref(null as any);
const gridRef = computed(() => commPageRef.value?.grid);
const getFormFields = computed(() => commPageRef.value?.getFormFields);
const validate = computed(() => commPageRef.value?.formValidate);
const buttonlist = computed(() => commPageRef.value?.buttons);

const lastRecordCount = ref(0);

// const handleGridFetched = () => {
//   fetchStore.isUpdatingRows = false;
//   nextTick(() => {
//     setTimeout(() => {
//       const el = commPageRef.value?.grid?.$el;
//       if (!el) return;
//       const summaryArea = el.querySelectorAll('.tui-grid-summary-area td');
//       if (summaryArea.length === 0) return;
//       const firstCell = summaryArea[0];
//       if (firstCell) {
//         firstCell.innerHTML =
//           `<span style="display:block;text-align:center;font-weight:bold;line-height:32px;font-size:12px;">${lastRecordCount.value}건</span>`;
//       }
//     }, 300);
//   });
// };

/**
 * 커스텀 fetchHandler
 * - CommonPage의 기본 fetcher와 동일한 query/sort/page/limit 파라미터 구조 사용
 * - getFormattedFilters로 DatePicker/RangePicker/in 등을 자동 변환
 * - 서버 커스텀 API(/wcs/inbound/summary)로 전달
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

    // ★ DatePicker → between, RangePicker → between, in → 콤마 등 자동 변환
    const queryFilters = await getFormattedFilters(fields, searchProps);

    // ★ 기본 fetcher와 동일한 파라미터 구조: query, sort, page, limit
    const requestParams: Record<string, any> = {
      query: JSON.stringify(queryFilters),
      sort: JSON.stringify(sorters),
      page,
      limit,
    };

    const resp = await getSearchList('/wcs/inbound/summary', requestParams);

    let records: any[] = [];
    let total = 0;
    if (resp && resp.items) {
      records = resp.items;
      total = resp.total;
    } else if (Array.isArray(resp)) {
      records = resp;
      total = resp.length;
    }

    lastRecordCount.value = total;
    return { total, records };
  } catch (e: any) {
    console.error('[InboundSummary] ERROR:', e);
    notification.error({
      message: '오류',
      description: e?.message || '조회 중 오류가 발생했습니다.',
      duration: 2,
    });
    lastRecordCount.value = 0;
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
.inbound-summary-page {
  height: 100%;
}
</style>
