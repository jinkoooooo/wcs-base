<template>
  <div class="outbound-instruction-page">
    <CommonPage
      ref="commPageRef"
      :limit="20"
      :showPagination="true"
      :showSummary="true"
      :summaryColumns="['order_qty', 'picked_qty', 'complete_qty']"
      :fetchHandler="fetchHandler"
      @resource-popup-click="onResourcePopupClick"
    >
      <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btn-handler="btnHandler" />

      <!-- 출고대 선택 (선택사항 — 비워두면 서버 자동 할당) -->
      <span class="toolbar-label">출고대</span>
      <Select
        v-model:value="selectedPort"
        style="width: 180px"
        size="small"
        :options="portOptions"
        :loading="portsLoading"
        placeholder="자동 할당"
        allowClear
      />

      <!-- 출고 예정일 (null/오늘=즉시 산출, 미래=WAITING_SCHEDULE) -->
      <span class="toolbar-label">출고 예정일</span>
      <a-date-picker
        v-model:value="scheduledDate"
        size="small"
        style="width: 140px"
        placeholder="오늘"
        valueFormat="YYYY-MM-DD"
        format="YYYY-MM-DD"
        allowClear
      />

      <a-button v-if="can('create')" type="primary" @click="addRow">추가</a-button>
      <a-button
        v-if="can('create')"
        type="primary"
        style="background: #52c41a; border-color: #52c41a"
        @click="doRegister"
      >
        등록
      </a-button>
      <a-button v-if="can('delete')" danger @click="doDelete">삭제</a-button>
      <a-button v-if="can('delete')" @click="doCancelOrders">주문 취소</a-button>
    </CommonPage>

    <!-- 출고는 재고 기준으로 선택 — SKU/LOT/창고/위치/수량을 한 번에 -->
    <StockSearchPopup v-model:open="itemPopupOpen" @select="onItemSelected" />
  </div>
</template>

<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { ref, computed, onMounted } from 'vue';
  import { Select } from 'ant-design-vue';
  import CommonPage from '../../common/CommonPage.vue';
  import StockSearchPopup from './popup/StockSearchPopup.vue';
  import { getSearchList, getCommonPostApi, getCommonGetListApi } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ButtonGroup from '/src/views/common/ButtonGroup.vue';
  import { getFormattedFilters } from '/src/views/tspg_4way/common/utils';
  import { usePermissionLocal } from '/src/views/tspg_4way/common/usePermissionLocal';

  const MENU = 'OutboundInstruction';
  const { can } = usePermissionLocal(MENU);

  const { notification, createConfirm } = useMessage();

  // =====================================================
  // CommonPage refs
  // =====================================================
  const commPageRef = ref(null as any);
  const gridRef = computed(() => commPageRef.value?.grid);
  const getFormFields = computed(() => commPageRef.value?.getFormFields);
  const validate = computed(() => commPageRef.value?.formValidate);
  const buttonlist = computed(() => commPageRef.value?.buttons);

  // 재고 팝업 — 출고는 재고(stock_id 단위)에서 선택
  const itemPopupOpen = ref(false);
  const activePopupField = ref<string>('item_code');

  // 출고대 (선택사항)
  const selectedPort = ref<string | undefined>(undefined);
  const portOptions = ref<Array<{ label: string; value: string }>>([]);
  const portsLoading = ref(false);

  // 출고 예정일
  const scheduledDate = ref<string | undefined>(undefined);

  async function loadPorts() {
    portsLoading.value = true;
    try {
      const resp: any = await getCommonGetListApi('/wcs/outbound/select/ports', null);
      const list: any[] = Array.isArray(resp) ? resp : resp?.items ?? [];
      portOptions.value = list.map((r) => ({
        label: `${r.port_code ?? ''} ${r.port_name ?? ''}`.trim(),
        value: r.port_code,
      }));
    } catch (e: any) {
      console.warn('[OutboundInstruction] loadPorts failed:', e?.message);
    } finally {
      portsLoading.value = false;
    }
  }

  onMounted(() => {
    loadPorts();
  });

  // =====================================================
  // 조회
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

      const resp = await getSearchList('/wcs/outbound/instruction', {
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
      console.error('[OutboundInstruction] ERROR:', e);
      notification.error({
        message: '오류',
        description: e?.message || '조회 중 오류가 발생했습니다.',
        duration: 2,
      });
      return { total: 0, records: [] };
    }
  }

  // =====================================================
  // 행 추가 — uom 기본값 BOX
  // =====================================================
  function addRow(preset?: Record<string, any>) {
    gridRef.value?.addRow({ uom: 'EA', ...(preset || {}) });
  }

  // =====================================================
  // 등록 — 각 행이 하나의 출고 주문(host_order). 입고와 동일한 흐름.
  //
  // [정책]
  //  - 모든 행을 백엔드에 한 번에 전송
  //  - 백엔드가 행 단위로 host_order 생성 (행별 독립 트랜잭션)
  //  - 부분 성공 허용
  // =====================================================
  async function doRegister() {
    // 프로그래매틱하게 추가한 행도 포함하려면 getData() 사용. order_key 없는 행 = 신규.
    const allRows = (gridRef.value?.getData?.() ?? gridRef.value?.getCURows?.() ?? []) as any[];
    const newRows = allRows.filter((row: any) => !row.order_key);

    if (newRows.length === 0) {
      return notification.warning({
        message: '안내',
        description: '추가된 행이 없습니다. [추가] 버튼으로 행을 먼저 추가하세요.',
      });
    }

    // 필수값 검증 — 입고와 동일하게 창고/화주/품목/수량/단위
    const invalid = newRows.find(
      (r: any) => !r.eq_group_id || !r.owner_code || !r.item_code || !r.order_qty || !r.uom,
    );
    if (invalid) {
      return notification.warning({
        message: '입력 오류',
        description: '창고, 화주, 품목코드, 출고수량, 단위(UOM)는 필수입니다.',
      });
    }

    // 품목명 비어있는 행 경고 — 마스터 미등록
    const noName = newRows.filter((r: any) => !r.item_name);
    if (noName.length > 0) {
      const codes = noName.map((r: any) => r.item_code).join(', ');
      return notification.warning({
        message: '미등록 품목',
        description:
          `품목 마스터에 없는 품목입니다: ${codes}\n` +
          `품목관리 화면에서 먼저 등록 후 다시 시도해 주세요.`,
        duration: 5,
      });
    }

    // 행마다 하나의 출고 주문 — orders 배열로 전송
    const orders = newRows.map((row: any, idx: number) => ({
      eqGroupId: row.eq_group_id,
      ownerCode: row.owner_code,
      itemCode: row.item_code,
      lotNo: row.lot_no || '',
      qty: Number(row.order_qty) || 0,
      uom: row.uom,
      clientRowSeq: idx + 1,
    }));

    const payload: any = { orders };

    if (selectedPort.value) {
      payload.portCode = selectedPort.value;
    }
    if (scheduledDate.value) {
      payload.scheduledDate = scheduledDate.value;
    }

    try {
      const resp = await getCommonPostApi('/wcs/outbound/instruction/register', payload);

      // 응답: { success, totalCount, successCount, failCount, results: [...] }
      const total = resp?.totalCount ?? orders.length;
      const ok = resp?.successCount ?? 0;
      const fail = resp?.failCount ?? 0;
      const results: any[] = resp?.results ?? [];

      if (resp?.success) {
        // 전체 성공
        notification.success({
          message: '등록 완료',
          description: `${total}건의 출고 지시가 등록되었습니다.`,
        });
        gridRef.value?.fetch();
      } else if (ok > 0) {
        // 부분 성공
        const failedDesc = results
          .filter((r) => !r.success)
          .map((r) => `• [${r.itemCode ?? '-'}] ${r.message ?? ''}`)
          .join('\n');
        notification.warning({
          message: '일부 등록 실패',
          description: `성공 ${ok} / 실패 ${fail} 건\n\n실패 목록:\n${failedDesc}`,
          duration: 8,
        });
        gridRef.value?.fetch();
      } else {
        // 전체 실패
        const desc = results.length
          ? results.map((r) => `• [${r.itemCode ?? '-'}] ${r.message ?? ''}`).join('\n')
          : resp?.message || '등록에 실패했습니다.';
        notification.error({
          message: '등록 실패',
          description: desc,
          duration: 8,
        });
      }
    } catch (e: any) {
      notification.error({
        message: '등록 오류',
        description: e?.message || '등록 중 오류가 발생했습니다.',
      });
    }
  }

  // =====================================================
  // 삭제
  // =====================================================
  function doDelete() {
    const checkedRows = gridRef.value?.getCheckedRows?.() ?? [];
    if (checkedRows.length === 0) {
      return notification.warning({ message: '안내', description: '삭제할 항목을 선택하세요.' });
    }

    const hostOrderKeys = [...new Set(checkedRows.map((r: any) => r.order_key).filter(Boolean))];

    if (hostOrderKeys.length === 0) {
      return notification.warning({
        message: '안내',
        description: '저장되지 않은 행은 삭제할 수 없습니다.',
      });
    }

    createConfirm({
      iconType: 'warning',
      title: () => '삭제 확인',
      content: () => `${hostOrderKeys.length}건의 출고지시를 삭제하시겠습니까?`,
      onOk: async () => {
        try {
          const resp = await getCommonPostApi('/wcs/outbound/instruction/delete', { hostOrderKeys });
          if (resp?.success) {
            notification.success({
              message: '삭제 완료',
              description: resp.message || '삭제되었습니다.',
            });
            gridRef.value?.fetch();
          } else {
            notification.error({
              message: '삭제 실패',
              description: resp?.message || '삭제에 실패했습니다.',
            });
          }
        } catch (e: any) {
          notification.error({
            message: '삭제 오류',
            description: e?.message || '삭제 중 오류가 발생했습니다.',
          });
        }
      },
    });
  }

  // =====================================================
  // 체크 행 주문 취소
  // =====================================================
  async function doCancelOrders() {
    const rows = gridRef.value?.getCheckedRows?.() ?? [];
    if (rows.length === 0) {
      return notification.warning({ message: '안내', description: '취소할 행을 선택하세요.' });
    }
    const targets = rows.filter((r: any) => r.host_order_key || r.order_key);
    if (targets.length === 0) {
      return notification.warning({
        message: '안내',
        description: '저장된 주문만 취소 가능합니다.',
      });
    }
    const reason = prompt(`${targets.length}건을 취소합니다. 사유를 입력하세요.`, '');
    if (reason == null) return;
    if (!reason.trim()) {
      return notification.warning({ message: '안내', description: '사유는 필수입니다.' });
    }
    for (const r of targets) {
      const key = r.host_order_key || r.order_key;
      try {
        await getCommonPostApi(
          `/rest/wcs/host-order/${encodeURIComponent(
            r.host_system_code || 'WMS',
          )}/${encodeURIComponent(key)}/cancel`,
          { operator: 'UI', reason: reason.trim() },
        );
      } catch (e: any) {
        notification.error({ message: '취소 실패', description: `${key}: ${e?.message || ''}` });
      }
    }
    notification.success({ message: '취소 처리 완료', description: `${targets.length}건 처리됨.` });
    gridRef.value?.fetch();
  }

  async function btnHandler(listenerName: any) {
    const handlers: Record<string, () => void> = {
      addBtnHandler: addRow,
      saveBtnHandler: doRegister,
      deleteBtnHandler: doDelete,
      exportBtnHandler: () => commPageRef.value?.downExcel(),
      exceldownBtnHandler: () => commPageRef.value?.downExcel(),
    };
    const handler = handlers[listenerName];
    if (handler) handler();
  }

  // =====================================================
  // 돋보기 팝업
  // =====================================================
  function onResourcePopupClick({ field, target }: { field: string; target: string }) {
    activePopupField.value = field;
    if (target === 'tb_inventory_item_mst') {
      itemPopupOpen.value = true;
    }
  }

  // 재고 선택 시 — 출고는 stock_id 단위로 결정되므로 LOT/창고까지 함께 채운다.
  // order_qty 는 선택한 재고 수량으로 자동 채우지 않고 사용자가 직접 입력하도록 유지.
  function onItemSelected(record: any) {
    const form: any = commPageRef.value?.form;
    const code = record?.item_code ?? '';
    form?.setFieldsValue?.({ [activePopupField.value]: code });

    try {
      const grid: any = gridRef.value;
      const rowKey = grid?.getFocusedCell?.()?.rowKey;
      if (rowKey != null) {
        if (record?.item_code) grid?.setValue?.(rowKey, 'item_code', record.item_code);
        if (record?.item_name) grid?.setValue?.(rowKey, 'item_name', record.item_name);
        if (record?.item_owner) grid?.setValue?.(rowKey, 'owner_code', record.item_owner);
        if (record?.lot_no) grid?.setValue?.(rowKey, 'lot_no', record.lot_no);
        if (record?.eq_group_id) grid?.setValue?.(rowKey, 'eq_group_id', record.eq_group_id);
        if (record?.uom) grid?.setValue?.(rowKey, 'uom', record.uom);
      }
    } catch (_) {
      /* grid 미준비 — 무시 */
    }
  }
</script>

<style scoped>
  .outbound-instruction-page {
    height: 100%;
  }
  .toolbar-label {
    font-size: 12px;
    font-weight: 500;
    color: #333;
    margin-left: 8px;
  }
</style>
