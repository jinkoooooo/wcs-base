<template>
  <div class="order-result-page">
    <VerticalDualGrid
      ref="dualRef"
      :masterFlex="6"
      :detailFlex="4"
      masterKeyField="history_no"
      :masterLimit="20"
      :masterShowPagination="true"
      :masterShowSummary="true"
      :masterSummaryColumns="['item_qty']"
      :masterShowSearchForm="true"
      :masterShowButtons="true"
      :masterFetchHandler="masterFetchHandler"
      :detailMetaUrl="detailMetaUrl"
      :detailMenuMetaProp="0"
      :detailLimit="50"
      :detailShowPagination="true"
      :detailShowButtons="true"
      :detailRowHeaders="['rowNum']"
      :detailFetchFn="detailFetchFn"
      @resource-popup-click="onResourcePopupClick"
    >
      <template #masterButtons>
        <ButtonGroup
          v-if="masterButtonlist"
          :buttonlist="masterButtonlist"
          @btn-handler="masterBtnHandler"
        />
      </template>

      <template #detailButtons>
        <a-button v-if="can('show')" type="primary" @click="onDetailExportClick">
          내보내기
        </a-button>
      </template>
    </VerticalDualGrid>

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
  import VerticalDualGrid from '../../common/VirticalDualGrid.vue';
  import ItemSearchPopup from '../inbound/popup/ItemSearchPopup.vue';
  import { getSearchList } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ButtonGroup from '/src/views/common/ButtonGroup.vue';
  import { getFormattedFilters } from '../../common/utils';
  import { usePermissionLocal } from '../../common/usePermissionLocal';

  // 라우트 meta.orderType으로 입고/출고 구분 (없으면 전체)
  const route = useRoute();
  const orderType = (route.meta?.orderType as string) || '';

  const MENU = (route.name as string) || 'OrderResult';
  const { can } = usePermissionLocal(MENU);
  const { notification } = useMessage();

  const dualRef = ref<any>(null);
  const itemPopupOpen = ref(false);
  const ownerPopupOpen = ref(false);
  const activePopupField = ref<string>('item_code');

  const masterButtonlist = computed(() => dualRef.value?.masterButtons);

  const detailMetaUrl = computed(() => `/menu_details/${String(route.name)}/named_meta`);

  // 라우트로 받은 order_type을 고정 필터로 추가
  function withOrderTypeFilter(filters: any[]): any[] {
    if (!orderType) return filters;
    const fixed = { name: 'order_type', operator: 'eq', value: orderType };
    return [...(filters || []), fixed];
  }

  // Master fetch
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
      const queryFilters = withOrderTypeFilter(await getFormattedFilters(fields, searchProps));

      const resp = await getSearchList('/wcs/order-result', {
        query: JSON.stringify(queryFilters),
        sort: JSON.stringify(sorters),
        page,
        limit,
      });

      return normalizeResp(resp);
    } catch (e: any) {
      notification.error({
        message: '오류',
        description: e?.message || '조회 중 오류가 발생했습니다.',
        duration: 2,
      });
      return { total: 0, records: [] };
    }
  }

  // Detail fetch
  async function detailFetchFn(masterKey: string, page: number, limit: number, sorters: any[]) {
    try {
      const resp = await getSearchList('/wcs/order-result/shuttle-orders', {
        host_order_key: masterKey,
        page,
        limit,
        sort: JSON.stringify(sorters),
      });
      return normalizeResp(resp);
    } catch (e: any) {
      notification.error({
        message: '오류',
        description: e?.message || '상세 조회 중 오류가 발생했습니다.',
        duration: 2,
      });
      return { total: 0, records: [] };
    }
  }

  // 응답 정규화
  function normalizeResp(resp: any) {
    if (resp && resp.items) return { total: resp.total, records: resp.items };
    if (Array.isArray(resp)) return { total: resp.length, records: resp };
    return { total: 0, records: [] };
  }

  // 버튼 핸들러
  async function masterBtnHandler(listenerName: any) {
    const handlers: Record<string, () => void> = {
      exportBtnHandler: () => dualRef.value?.masterDownExcel?.(),
      exceldownBtnHandler: () => dualRef.value?.masterDownExcel?.(),
    };
    handlers[listenerName]?.();
  }

  function onDetailExportClick() {
    try {
      dualRef.value?.detailDownExcel?.();
    } catch (e: any) {
      notification.error({
        message: '오류',
        description: e?.message || '내보내기 중 오류가 발생했습니다.',
        duration: 2,
      });
    }
  }

  // 팝업
  function onResourcePopupClick({ field, target }: { field: string; target: string }) {
    activePopupField.value = field;
    if (target === 'tb_inventory_item_mst') itemPopupOpen.value = true;
    else if (target === 'tb_wcs_item_owner') ownerPopupOpen.value = true;
  }

  function onItemSelected(record: any) {
    dualRef.value?.masterForm?.setFieldsValue?.({
      [activePopupField.value]: record?.item_code ?? '',
    });
  }

  function onOwnerSelected(record: any) {
    dualRef.value?.masterForm?.setFieldsValue?.({
      [activePopupField.value]: record?.owner_code ?? '',
      owner_name: record?.owner_name ?? '',
    });
  }
</script>
