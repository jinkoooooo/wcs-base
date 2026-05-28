<template>
  <div class="outbound-result-page">
    <VerticalDualGrid
      ref="dualRef"
      :masterFlex="6"
      :detailFlex="4"
      masterKeyField="history_no"
      :masterLimit="20"
      :masterShowPagination="true"
      :masterShowSummary="true"
      :masterSummaryColumns="['item_qty']"
      :masterShowSearchForm="showMasterSearchForm"
      :masterShowButtons="showMasterButtons"
      :masterFetchHandler="masterFetchHandler"
      :detailMetaUrl="detailMetaUrl"
      :detailMenuMetaProp="0"
      :detailLimit="50"
      :detailShowPagination="true"
      :detailShowSearchForm="showDetailSearchForm"
      :detailShowButtons="showDetailButtons"
      :detailRowHeaders="['rowNum']"
      :detailFetchFn="detailFetchFn"
      @resource-popup-click="onResourcePopupClick"
      @master-clicked="onMasterClicked"
    >
      <!-- ★ 상단(Master) 버튼 슬롯 — 메뉴 메타 기반 ButtonGroup -->
      <template #masterButtons>
        <ButtonGroup
          v-if="showMasterButtons && masterButtonlist"
          :buttonlist="masterButtonlist"
          @btn-handler="masterBtnHandler"
        />
      </template>

      <!--
        ★ 하단(Detail) 버튼 슬롯 — 서버 메타와 무관하게 "내보내기" 버튼 직접 렌더
          - 메뉴 메타에 버튼이 등록되지 않아도 항상 표시된다.
          - 필요시 추가 버튼(예: 새로고침)을 여기에 덧붙일 수 있음.
      -->
      <template #detailButtons>
        <a-button v-if="can('show')" type="primary" @click="onDetailExportClick">
          내보내기
        </a-button>
      </template>
    </VerticalDualGrid>

    <!-- 품목/업체 검색 팝업 -->
    <ItemSearchPopup v-model:open="itemPopupOpen" @select="onItemSelected" />
    <ItemSearchPopup
      v-model:open="ownerPopupOpen"
      resourceUrl="/tb_wcs_item_owner"
      @select="onOwnerSelected"
    />
  </div>
</template>

<script lang="ts" setup>
import 'tui-grid/dist/tui-grid.css';
import { ref, computed } from 'vue';
import { useRoute } from 'vue-router';
import { Button as AButton } from 'ant-design-vue';
import { DownloadOutlined } from '@ant-design/icons-vue';
import VerticalDualGrid from '../../common/VirticalDualGrid.vue';
import ItemSearchPopup from '../inbound/popup/ItemSearchPopup.vue';
import { getSearchList } from '/@/api/common/api';
import { useMessage } from '/@/hooks/web/useMessage';
import ButtonGroup from '/src/views/common/ButtonGroup.vue';
import { getFormattedFilters } from '../../common/utils';
import { usePermissionLocal } from '../../common/usePermissionLocal';

const MENU = 'OutboundResult';
const { can } = usePermissionLocal(MENU);

const { notification } = useMessage();
const route = useRoute();

const dualRef = ref<any>(null);

// ═══════════════════════════════════════════════════════════════════
// ★ 상/하단 표시 토글
// ═══════════════════════════════════════════════════════════════════
const showMasterSearchForm = ref(true);  // 상단 검색 폼
const showMasterButtons    = ref(true);  // 상단 버튼 (메뉴 메타 기반 ButtonGroup)
const showDetailSearchForm = ref(false); // 하단 검색 폼
const showDetailButtons    = ref(true);  // ★ true 로 변경 — 하단 내보내기 버튼 영역 활성화

// 상/하단 버튼 메타 (VerticalDualGrid 내부 CommonPage 에서 parseButtons 된 결과)
const masterButtonlist = computed(() => dualRef.value?.masterButtons);
const detailButtonlist = computed(() => dualRef.value?.detailButtons);

const itemPopupOpen = ref(false);
const ownerPopupOpen = ref(false);
const activePopupField = ref<string>('item_code');

/**
 * 하단 Detail Grid 메타 URL
 * — 기존 패턴: /menu_details/{route.name}/named_meta
 * — 메뉴변경 화면 > 상세 탭에서 등록한 컬럼 설정을 가져옴
 */
const detailMetaUrl = computed(() => {
  return `/menu_details/${String(route.name)}/named_meta`;
});

// ═══════════════════════════════════════════════════════════════════
// 상단 Master fetchHandler
// ═══════════════════════════════════════════════════════════════════

async function masterFetchHandler(
  page: number,
  limit: number,
  sorters: any[],
  searchProps: any[],
) {
  try {
    try {
      await dualRef.value?.masterFormValidate?.();
    } catch (_) {}
    const fields = dualRef.value?.masterGetFormFields?.() || {};
    const queryFilters = await getFormattedFilters(fields, searchProps);

    const resp = await getSearchList('/wcs/outbound/result', {
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
    console.error('[OutboundResult] ERROR:', e);
    notification.error({
      message: '오류',
      description: e?.message || '조회 중 오류가 발생했습니다.',
      duration: 2,
    });
    return { total: 0, records: [] };
  }
}

// ═══════════════════════════════════════════════════════════════════
// 하단 Detail fetchHandler
// ═══════════════════════════════════════════════════════════════════

async function detailFetchFn(
  masterKey: string,
  page: number,
  limit: number,
  sorters: any[],
  searchProps: any[],
) {
  try {
    const resp = await getSearchList('/wcs/outbound/result/shuttle-orders', {
      host_order_key: masterKey,
      page,
      limit,
      sort: JSON.stringify(sorters),
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
    console.error('[OutboundResult Detail] ERROR:', e);
    notification.error({
      message: '오류',
      description: e?.message || '상세 조회 중 오류가 발생했습니다.',
      duration: 2,
    });
    return { total: 0, records: [] };
  }
}

// ═══════════════════════════════════════════════════════════════════
// 이벤트 핸들러
// ═══════════════════════════════════════════════════════════════════

function onMasterClicked(row: any) {}

// 상단 버튼 핸들러 — 메타 기반 listenerName 으로 분기
async function masterBtnHandler(listenerName: any) {
  const handlers: Record<string, () => void> = {
    exportBtnHandler: () => dualRef.value?.masterDownExcel?.(),
    exceldownBtnHandler: () => dualRef.value?.masterDownExcel?.(),
  };
  const handler = handlers[listenerName];
  if (handler) handler();
}

/**
 * ★ 하단 내보내기 버튼 클릭
 *  - VerticalDualGrid.detailDownExcel() → CommonPage.downExcel()
 *  - → Grid.exportExcel(route.name) → BasicGrid.exportExcel()
 */
function onDetailExportClick() {
  try {
    dualRef.value?.detailDownExcel?.();
  } catch (e: any) {
    console.error('[OutboundResult Detail Export] ERROR:', e);
    notification.error({
      message: '오류',
      description: e?.message || '내보내기 중 오류가 발생했습니다.',
      duration: 2,
    });
  }
}

function onResourcePopupClick({ field, target }: { field: string; target: string }) {
  activePopupField.value = field;
  if (target === 'tb_inventory_item_mst') itemPopupOpen.value = true;
  else if (target === 'tb_wcs_item_owner') ownerPopupOpen.value = true;
}

function onItemSelected(record: any) {
  const form = dualRef.value?.masterForm;
  form?.setFieldsValue?.({ [activePopupField.value]: record?.item_code ?? '' });
}

function onOwnerSelected(record: any) {
  const form = dualRef.value?.masterForm;
  form?.setFieldsValue?.({
    [activePopupField.value]: record?.owner_code ?? '',
    owner_name: record?.owner_name ?? '',
  });
}
</script>

<!-- ★ style 블록 완전 제거 — VerticalDualGrid 가 자기 완결형 레이아웃 -->
