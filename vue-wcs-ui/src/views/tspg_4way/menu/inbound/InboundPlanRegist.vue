<template>
  <div class="inbound-plan-page">
    <CommonPage
      ref="commPageRef"
      :limit="20"
      :showPagination="true"
      :showSummary="true"
      :summaryColumns="['planned_qty', 'ordered_qty', 'remaining_qty']"
      :fetchHandler="fetchHandler"
      @resource-popup-click="onResourcePopupClick"
    >
      <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btn-handler="btnHandler" />
      <a-button v-if="can('create')" type="primary" @click="() => addRow()">추가</a-button>
      <a-button
        v-if="can('create')"
        type="primary"
        style="background: #52c41a; border-color: #52c41a"
        @click="doSave"
      >
        저장
      </a-button>
      <a-button v-if="can('delete')" danger @click="doDelete">삭제</a-button>
    </CommonPage>

    <!-- 품목 검색 팝업 -->
    <ItemSearchPopup v-model:open="itemPopupOpen" @select="onItemSelected" />
  </div>
</template>

<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { ref, computed } from 'vue';
  import CommonPage from '../../common/CommonPage.vue';
  import ItemSearchPopup from './popup/ItemSearchPopup.vue';
  import { getSearchList, getCommonPostApi } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ButtonGroup from '/src/views/common/ButtonGroup.vue';
  import { getFormattedFilters } from '../../common/utils.ts';
  import { usePermissionLocal } from '../../common/usePermissionLocal';

  const MENU = 'InboundPlanRegist';
  const { can } = usePermissionLocal(MENU);

  const { notification, createConfirm } = useMessage();

  // CommonPage refs
  const commPageRef = ref(null as any);
  const gridRef = computed(() => commPageRef.value?.grid);
  const getFormFields = computed(() => commPageRef.value?.getFormFields);
  const validate = computed(() => commPageRef.value?.formValidate);
  const buttonlist = computed(() => commPageRef.value?.buttons);

  // 품목 팝업
  const itemPopupOpen = ref(false);
  const activePopupField = ref<string>('item_code');

  // 그리드 boolean 은 문자열("true"/"false") 로 들어올 수 있어 명시 변환
  function toBool(v: any): boolean {
    if (v === true || v === 1) return true;
    if (typeof v === 'string') return v.trim().toLowerCase() === 'true';
    return false;
  }

  function todayStr(): string {
    return new Date().toISOString().slice(0, 10);
  }

  // 날짜 정규화 — DatePicker / 문자열 / Date 모두 'YYYY-MM-DD' 로
  function normalizeDate(v: any): string | null {
    if (v == null || v === '') return null;
    if (typeof v === 'string') return v.length >= 10 ? v.substring(0, 10) : null;
    if (typeof v?.format === 'function') return v.format('YYYY-MM-DD');
    if (v instanceof Date) {
      const y = v.getFullYear();
      const m = String(v.getMonth() + 1).padStart(2, '0');
      const d = String(v.getDate()).padStart(2, '0');
      return `${y}-${m}-${d}`;
    }
    return null;
  }

  // 조회 — GET /rest/wcs/inbound/plan
  async function fetchHandler(page: number, limit: number, sorters: any[], searchProps: any[]) {
    try {
      try {
        await validate.value();
      } catch (_) {
        /* outOfDate 무시 */
      }

      const fields = getFormFields.value();
      const queryFilters = await getFormattedFilters(fields, searchProps);

      const resp = await getSearchList('/wcs/inbound/plan', {
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
      console.error('[InboundPlanRegist] fetchHandler ERROR:', e);
      notification.error({
        message: '조회 오류',
        description: e?.message || '조회 중 오류가 발생했습니다.',
        duration: 2,
      });
      return { total: 0, records: [] };
    }
  }

  // 행 추가 — 입고 예정일 기본 오늘, 단위 EA
  function addRow(preset?: Record<string, any>) {
    gridRef.value?.addRow({
      plan_date: todayStr(),
      uom: 'EA',
      planned_qty: 0,
      ordered_qty: 0,
      test_required: false,
      nia_required: false,
      ...(preset || {}),
    });
  }

  // 저장 — POST /rest/wcs/inbound/plan/save (신규 행만)
  async function doSave() {
    const allRows = (gridRef.value?.getData?.() ?? gridRef.value?.getCURows?.() ?? []) as any[];
    const newRows = allRows.filter((row: any) => !row.id);

    if (newRows.length === 0) {
      return notification.warning({
        message: '안내',
        description: '추가된 행이 없습니다. [추가] 버튼으로 행을 먼저 추가하세요.',
      });
    }

    // 필수값 검증 — 품목코드, 입고 예정 수량(>0)
    const invalid = newRows.find(
      (r: any) => !r.item_code || !(Number(r.planned_qty) > 0),
    );
    if (invalid) {
      return notification.warning({
        message: '입력 오류',
        description: '품목코드와 입고 예정 수량(0보다 큰 값)은 필수입니다.',
      });
    }

    const rows = newRows.map((r: any) => ({
      planDate: normalizeDate(r.plan_date) || todayStr(),
      itemCode: r.item_code,
      lotNo: r.lot_no || '',
      ownerCode: r.owner_code || r.item_owner || 'OWN001',
      plannedQty: Number(r.planned_qty) || 0,
      uom: r.uom || 'EA',
      produceDate: normalizeDate(r.produce_date),
      expiryDate: normalizeDate(r.expiry_date),
      testRequired: toBool(r.test_required),
      niaRequired: toBool(r.nia_required),
    }));

    try {
      const resp = await getCommonPostApi('/wcs/inbound/plan/save', { rows });
      if (resp?.success) {
        notification.success({
          message: '저장 완료',
          description: `${resp.createdCount ?? rows.length}건 입고 예정 등록 완료`,
        });
        gridRef.value?.fetch();
      } else {
        notification.error({
          message: '저장 실패',
          description: resp?.message || '저장에 실패했습니다.',
        });
      }
    } catch (e: any) {
      notification.error({
        message: '저장 오류',
        description: e?.message || '저장 중 오류가 발생했습니다.',
      });
    }
  }

  // 삭제 — POST /rest/wcs/inbound/plan/delete (체크 행)
  function doDelete() {
    const checkedRows = gridRef.value?.getCheckedRows?.() ?? [];
    const ids = [...new Set(checkedRows.map((r: any) => r.id).filter(Boolean))];

    if (ids.length === 0) {
      return notification.warning({
        message: '안내',
        description: '삭제할 항목을 선택하세요. (저장된 예정만 삭제 가능)',
      });
    }

    createConfirm({
      iconType: 'warning',
      title: () => '삭제 확인',
      content: () => `${ids.length}건의 입고 예정을 삭제하시겠습니까?`,
      onOk: async () => {
        try {
          const resp = await getCommonPostApi('/wcs/inbound/plan/delete', { ids });
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

  // 메타 버튼 핸들러
  async function btnHandler(listenerName: any) {
    const handlers: Record<string, () => void> = {
      addBtnHandler: () => addRow(),
      saveBtnHandler: doSave,
      deleteBtnHandler: doDelete,
      exportBtnHandler: () => commPageRef.value?.downExcel(),
      exceldownBtnHandler: () => commPageRef.value?.downExcel(),
    };
    const handler = handlers[listenerName];
    if (handler) handler();
  }

  // 돋보기 팝업 (resource-popup) — 품목 마스터
  function onResourcePopupClick({ field, target }: { field: string; target: string }) {
    activePopupField.value = field;
    if (target === 'tb_inventory_item_mst') {
      itemPopupOpen.value = true;
    }
  }

  // 품목 선택 → 포커스 행에 코드/품명/화주/국검 미러 (없으면 검색 폼 필드)
  function onItemSelected(record: any) {
    const grid: any = gridRef.value;
    const rowKey = grid?.getFocusedCell?.()?.rowKey;
    if (rowKey == null) {
      commPageRef.value?.form?.setFieldsValue?.({ [activePopupField.value]: record?.item_code ?? '' });
      return;
    }
    grid.setValue?.(rowKey, 'item_code', record?.item_code ?? '');
    grid.setValue?.(rowKey, 'item_name', record?.item_name ?? '');
    grid.setValue?.(rowKey, 'owner_code', record?.owner_code ?? record?.item_owner ?? '');
    grid.setValue?.(rowKey, 'nia_required', toBool(record?.nia_required));
  }
</script>

<style scoped>
  .inbound-plan-page {
    height: 100%;
  }
</style>
