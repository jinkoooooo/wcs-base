<template>
  <div class="select-outbound-page">
    <CommonPage
      ref="commPageRef"
      :limit="20"
      :showPagination="true"
      :showSummary="true"
      :summaryColumns="['item_qty']"
      :fetchHandler="fetchHandler"
      @resource-popup-click="onResourcePopupClick"
    >
      <!-- 카테고리 + 출고대 드롭다운 + 출고지시 버튼 (그리드 우상단 툴바 영역) -->
      <div class="toolbar-right">
        <span class="label">카테고리</span>
        <Select
          v-model:value="selectedCategory"
          style="width: 120px"
          size="small"
          :options="categoryOptions"
          @change="onCategoryChange"
        />
        <span class="label">출고대</span>
        <Select
          v-model:value="selectedPort"
          style="width: 200px"
          size="small"
          :options="portOptions"
          :loading="portsLoading"
          placeholder="출고대 선택"
          allowClear
        />
        <a-button v-if="can('update')" type="primary" :loading="issuing" @click="doIssueOutbound">
          출고지시
        </a-button>
        <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btn-handler="btnHandler" />
      </div>
    </CommonPage>

    <!-- 품목 검색 팝업 -->
    <ItemSearchPopup v-model:open="itemPopupOpen" @select="onItemSelected" />
  </div>
</template>

<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { ref, computed, onMounted } from 'vue';
  import { Select } from 'ant-design-vue';
  import CommonPage from '../../common/CommonPage.vue';
  import ItemSearchPopup from '../inbound/popup/ItemSearchPopup.vue';
  import { getSearchList, getCommonPostApi, getCommonGetListApi } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ButtonGroup from '/src/views/common/ButtonGroup.vue';
  import { getFormattedFilters } from '/src/views/tspg_4way/common/utils';
  import { usePermissionLocal } from '/src/views/tspg_4way/common/usePermissionLocal';

  const MENU = 'SelectOutbound';
  const { can } = usePermissionLocal(MENU);

  const { notification, createConfirm } = useMessage();

  const commPageRef = ref(null as any);
  const gridRef = computed(() => commPageRef.value?.grid);
  const getFormFields = computed(() => commPageRef.value?.getFormFields);
  const validate = computed(() => commPageRef.value?.formValidate);
  const buttonlist = computed(() => commPageRef.value?.buttons);

  const itemPopupOpen = ref(false);
  const activePopupField = ref<string>('item_code');

  // 카테고리 — NORMAL / RETURN / DISPOSAL.
  // stockType 으로 조회를 필터링하고, host_order.order_type 으로 출고 카테고리 표현.
  const selectedCategory = ref<'NORMAL' | 'RETURN' | 'DISPOSAL'>('NORMAL');
  const categoryOptions = [
    { label: '일반', value: 'NORMAL' },
    { label: '반품', value: 'RETURN' },
    { label: '폐기', value: 'DISPOSAL' },
  ];

  // 카테고리 → host_order.order_type 매핑.
  function orderTypeForCategory(category: string): string {
    if (category === 'DISPOSAL') return 'DISPOSAL_OUT';
    return 'OUTBOUND';
  }

  // 출고대 드롭다운 상태
  const selectedPort = ref<string | undefined>(undefined);
  const portOptions = ref<Array<{ label: string; value: string }>>([]);
  const portsLoading = ref(false);
  const issuing = ref(false);

  function onCategoryChange() {
    gridRef.value?.fetch?.();
  }

  // =====================================================
  // 재고현황 조회 — GET /rest/wcs/outbound/select
  // =====================================================
  async function fetchHandler(page: number, limit: number, sorters: any[], searchProps: any[]) {
    try {
      try {
        await validate.value();
      } catch (_) {
        /* outOfDate 무시 */
      }
      const fields = getFormFields.value();
      const queryFilters = await getFormattedFilters(fields, searchProps);

      const resp = await getSearchList('/wcs/outbound/select', {
        query: JSON.stringify(queryFilters),
        sort: JSON.stringify(sorters),
        page,
        limit,
        stockType: selectedCategory.value,
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
      console.error('[SelectOutbound] fetchHandler ERROR:', e);
      notification.error({
        message: '오류',
        description: e?.message || '조회 중 오류가 발생했습니다.',
        duration: 2,
      });
      return { total: 0, records: [] };
    }
  }

  // =====================================================
  // 출고대 드롭다운 로드 — GET /rest/wcs/outbound/select/ports
  // =====================================================
  async function loadPorts() {
    portsLoading.value = true;
    try {
      const resp: any = await getCommonGetListApi('/wcs/outbound/select/ports', null);
      const list: any[] = Array.isArray(resp) ? resp : resp?.items ?? [];
      portOptions.value = list.map((r) => ({
        label: `${r.port_code ?? ''} ${r.port_name ?? ''}`.trim(),
        value: r.port_code,
      }));
      if (portOptions.value.length > 0 && !selectedPort.value) {
        selectedPort.value = portOptions.value[0].value;
      }
    } catch (e: any) {
      console.warn('[SelectOutbound] loadPorts failed:', e?.message);
    } finally {
      portsLoading.value = false;
    }
  }

  onMounted(() => {
    loadPorts();
  });

  // =====================================================
  // 출고 지시 — POST /rest/wcs/outbound/select/issue
  // =====================================================
  async function doIssueOutbound() {
    const checkedRows = gridRef.value?.getCheckedRows?.() ?? [];
    if (checkedRows.length === 0) {
      return notification.warning({
        message: '안내',
        description: '출고할 재고를 선택하세요.',
      });
    }
    if (!selectedPort.value) {
      return notification.warning({
        message: '안내',
        description: '출고대(포트)를 선택하세요.',
      });
    }

    const stocks = checkedRows.map((r: any) => ({
      stockId: r.stock_id,
      itemCode: r.item_code,
      itemOwner: r.item_owner,
      ownerCode: r.item_owner,
      lotNo: r.lot_no,
      qty: Number(r.item_qty) || 0,
      uom: r.item_unit || 'BOX',
      locId: r.loc_id,
      eq_group_id: r.eq_group_id,
    }));

    createConfirm({
      iconType: 'warning',
      title: () => '출고 지시 확인',
      content: () =>
        `선택한 ${stocks.length}건을 출고대 [${selectedPort.value}]로 출고지시하시겠습니까?`,
      onOk: async () => {
        issuing.value = true;
        try {
          const resp: any = await getCommonPostApi('/wcs/outbound/select/issue', {
            portCode: selectedPort.value,
            eqGroupId: stocks[0]?.eq_group_id || '',
            orderType: orderTypeForCategory(selectedCategory.value),
            stocks,
          });
          if (resp?.success) {
            notification.success({
              message: '출고지시 완료',
              description: resp.message || `${stocks.length}건 출고지시가 등록되었습니다.`,
            });
            gridRef.value?.fetch?.();
          } else {
            notification.error({
              message: '출고지시 실패 (일부)',
              description: resp?.message || '출고지시 중 오류가 발생했습니다.',
            });
            gridRef.value?.fetch?.();
          }
        } catch (e: any) {
          notification.error({
            message: '출고지시 오류',
            description: e?.message || '출고지시 중 오류가 발생했습니다.',
          });
        } finally {
          issuing.value = false;
        }
      },
    });
  }

  // =====================================================
  // 메타 버튼 핸들러
  // =====================================================
  async function btnHandler(listenerName: any) {
    const handlers: Record<string, () => void> = {
      issueOutboundBtnHandler: doIssueOutbound,
      exportBtnHandler: () => commPageRef.value?.downExcel(),
      exceldownBtnHandler: () => commPageRef.value?.downExcel(),
    };
    const handler = handlers[listenerName];
    if (handler) handler();
  }

  // =====================================================
  // 돋보기 팝업 (resource-popup)
  // =====================================================
  function onResourcePopupClick({ field, target }: { field: string; target: string }) {
    activePopupField.value = field;
    if (target === 'tb_inventory_item_mst') {
      itemPopupOpen.value = true;
    }
  }

  function onItemSelected(record: any) {
    const form: any = commPageRef.value?.form;
    const code = record?.item_code ?? '';
    form?.setFieldsValue?.({ [activePopupField.value]: code });
  }
</script>

<style scoped>
  .select-outbound-page {
    height: 100%;
  }
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
  .toolbar-right .label {
    font-size: 12px;
    font-weight: 500;
    color: #333;
  }
</style>
