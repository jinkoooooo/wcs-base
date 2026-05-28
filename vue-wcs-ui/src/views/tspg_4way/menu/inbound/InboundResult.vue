<template>
  <div class="inbound-result-page">
    <CommonPage
      ref="commPageRef"
      :limit="20"
      :showPagination="true"
      :showSummary="true"
      :summaryColumns="['item_qty']"
      :fetchHandler="fetchHandler"
      @resourcePopupClick="onResourcePopupClick"
    >
      <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btnHandler="btnHandler" />
    </CommonPage>

    <!-- 품목 검색 팝업 (item_code 필드 🔍 클릭 시 오픈) -->
    <ItemSearchPopup v-model:open="itemPopupOpen" @select="onItemSelected" />
  </div>
</template>

<script lang="ts" setup>
import 'tui-grid/dist/tui-grid.css';
import { ref, computed } from 'vue';
import CommonPage from '../../common/CommonPage.vue';
import ItemSearchPopup from './popup/ItemSearchPopup.vue';
import { getSearchList } from '/@/api/common/api';
import { useMessage } from '/@/hooks/web/useMessage';
import ButtonGroup from '/src/views/common/ButtonGroup.vue';
import { getFormattedFilters } from '/src/views/tspg_4way/common/utils';

const { notification } = useMessage();

const commPageRef = ref(null as any);
const getFormFields = computed(() => commPageRef.value?.getFormFields);
const validate = computed(() => commPageRef.value?.formValidate);
const buttonlist = computed(() => commPageRef.value?.buttons);

const lastRecordCount = ref(0);
const itemPopupOpen = ref(false);
// 팝업에서 선택한 값이 반영될 대상 필드 (resourcePopupClick 시 갱신)
const activePopupField = ref<string>('item_code');

/**
 * 커스텀 fetchHandler
 * - CommonPage의 기본 fetcher와 동일한 query/sort/page/limit 파라미터 구조 사용
 * - getFormattedFilters로 DatePicker/RangePicker/in 등을 자동 변환
 * - 서버 커스텀 API(/wcs/inbound/result)로 전달
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

    const requestParams: Record<string, any> = {
      query: JSON.stringify(queryFilters),
      sort: JSON.stringify(sorters),
      page,
      limit,
    };

    const resp = await getSearchList('/wcs/inbound/result', requestParams);

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
    console.error('[InboundResult] ERROR:', e);
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

// ─────────────────────────────────────────────────────────────
//  돋보기 아이콘 클릭 → target별 팝업 오픈
//  (CommonPage가 🔍 아이콘 주입/이벤트 발행까지 공통 처리)
// ─────────────────────────────────────────────────────────────
function onResourcePopupClick({ field, target }: { field: string; target: string }) {
  activePopupField.value = field;
  if (target === 'tb_inventory_item_mst') {
    itemPopupOpen.value = true;
  }
  // 다른 마스터 팝업은 여기서 분기 추가
}

/** 팝업에서 품목 선택 → 검색폼 해당 필드에 반영 */
function onItemSelected(record: any) {
  const form: any = commPageRef.value?.form;
  const code = record?.item_code ?? '';
  form?.setFieldsValue?.({ [activePopupField.value]: code });
}
</script>

<style scoped>
/* CommonPage 의 h-full 레이아웃이 죽지 않도록 wrapper 에도 전체 높이를 준다 */
.inbound-result-page {
  height: 100%;
}
</style>
