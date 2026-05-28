<template>
  <div class="test-outbound-page">
    <CommonPage
      ref="commPageRef"
      :limit="20"
      :showPagination="true"
      :showSummary="true"
      :summaryColumns="['item_qty']"
      :fetchHandler="fetchHandler"
      @resourcePopupClick="onResourcePopupClick"
    >
      <div class="toolbar-right">
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
        <a-button v-if="can('create')" type="primary" :loading="issuing" @click="doIssueOutbound">시험 출고 지시</a-button>
        <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btnHandler="btnHandler" />
      </div>
    </CommonPage>
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

const MENU = 'QcTestOutbound';
const { can } = usePermissionLocal(MENU);

const { notification, createConfirm } = useMessage();
const commPageRef = ref(null as any);
const gridRef = computed(() => commPageRef.value?.grid);
const getFormFields = computed(() => commPageRef.value?.getFormFields);
const validate = computed(() => commPageRef.value?.formValidate);
const buttonlist = computed(() => commPageRef.value?.buttons);
const itemPopupOpen = ref(false);
const activePopupField = ref('item_code');
const selectedPort = ref<string | undefined>(undefined);
const portOptions = ref<Array<{ label: string; value: string }>>([]);
const portsLoading = ref(false);
const issuing = ref(false);

async function fetchHandler(page: number, limit: number, sorters: any[], searchProps: any[]) {
  try {
    try { await validate.value(); } catch (_) {}
    const queryFilters = await getFormattedFilters(getFormFields.value(), searchProps);
    const resp = await getSearchList('/wcs/outbound/qc-test', {
      query: JSON.stringify(queryFilters), sort: JSON.stringify(sorters), page, limit,
    });
    if (resp?.items) return { total: resp.total, records: resp.items };
    if (Array.isArray(resp)) return { total: resp.length, records: resp };
    return { total: 0, records: [] };
  } catch (e: any) {
    notification.error({ message: '오류', description: e?.message || '조회 실패', duration: 2 });
    return { total: 0, records: [] };
  }
}

async function loadPorts() {
  portsLoading.value = true;
  try {
    const resp: any = await getCommonGetListApi('/wcs/outbound/qc-test/ports', null);
    const list: any[] = Array.isArray(resp) ? resp : (resp?.items ?? []);
    portOptions.value = list.map((r) => ({
      label: `${r.port_code ?? ''} ${r.port_name ?? ''}`.trim(),
      value: r.port_code,
    }));
    if (portOptions.value.length > 0 && !selectedPort.value) selectedPort.value = portOptions.value[0].value;
  } finally { portsLoading.value = false; }
}

onMounted(loadPorts);

async function doIssueOutbound() {
  const checkedRows = gridRef.value?.getCheckedRows?.() ?? [];
  if (checkedRows.length === 0) return notification.warning({ message: '안내', description: '재고를 선택하세요.' });
  if (!selectedPort.value) return notification.warning({ message: '안내', description: '출고대를 선택하세요.' });
  const stocks = checkedRows.map((r: any) => ({
    stockId: r.stock_id, itemCode: r.item_code, itemOwner: r.item_owner, ownerCode: r.item_owner,
    lotNo: r.lot_no, qty: Number(r.item_qty) || 0, uom: r.item_unit || 'EA',
    locId: r.loc_id, eq_group_id: r.eq_group_id, origin_host_order_key: r.origin_host_order_key,
  }));
  createConfirm({
    iconType: 'warning',
    title: () => '시험 출고 지시',
    content: () => `${stocks.length}건을 [${selectedPort.value}]로 출고지시하시겠습니까? 박스 스캔/확정은 통합 작업 화면에서 진행됩니다.`,
    onOk: async () => {
      issuing.value = true;
      try {
        const resp: any = await getCommonPostApi('/wcs/outbound/qc-test/issue', {
          portCode: selectedPort.value,
          eqGroupId: stocks[0]?.eq_group_id || '',
          stocks,
        });
        if (resp?.success) {
          notification.success({ message: '출고지시 완료', description: `${stocks.length}건 등록. 통합 작업 화면에서 박스 스캔 후 확정하세요.` });
        } else {
          notification.error({ message: '실패', description: resp?.message || '오류' });
        }
        gridRef.value?.fetch?.();
      } catch (e: any) {
        notification.error({ message: '오류', description: e?.message || '출고지시 오류' });
      } finally { issuing.value = false; }
    },
  });
}

async function btnHandler(listenerName: any) {
  const handlers: Record<string, () => void> = {
    issueOutboundBtnHandler: doIssueOutbound,
    exportBtnHandler: () => commPageRef.value?.downExcel(),
    exceldownBtnHandler: () => commPageRef.value?.downExcel(),
  };
  handlers[listenerName]?.();
}

function onResourcePopupClick({ field, target }: { field: string; target: string }) {
  activePopupField.value = field;
  if (target === 'tb_inventory_item_mst') itemPopupOpen.value = true;
}

function onItemSelected(record: any) {
  commPageRef.value?.form?.setFieldsValue?.({ [activePopupField.value]: record?.item_code ?? '' });
}
</script>

<style scoped>
.test-outbound-page { height: 100%; }
.toolbar-right { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.toolbar-right .label { font-size: 12px; font-weight: 500; color: #333; }
</style>
