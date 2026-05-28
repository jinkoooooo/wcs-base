돼<template>
  <div class="inbound-regist-page">
    <CommonPage
      ref="commPageRef"
      :limit="20"
      :showPagination="true"
      :showSummary="true"
      :summaryColumns="['item_qty']"
      :fetchHandler="fetchHandler"
      @resource-popup-click="onResourcePopupClick"
    >
      <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btn-handler="btnHandler" />
      <span style="margin-left: 8px; font-size: 12px; color: #555">카테고리</span>
      <a-select
        v-model:value="selectedCategory"
        :options="categoryOptions"
        size="small"
        style="width: 120px"
      />
      <a-button v-if="can('create') && !planRequired" type="primary" @click="addRow">추가</a-button>
      <a-button
        v-if="can('create')"
        type="primary"
        style="background: #52c41a; border-color: #52c41a"
        @click="doRegister"
      >
        등록
      </a-button>
      <a-button v-if="can('delete')" danger @click="doDelete">삭제</a-button>
<!--      <a-button v-if="can('update')" @click="doRetryInspection">재검수 요청</a-button>-->
      <a-button v-if="can('delete')" @click="doCancelOrders">주문 취소</a-button>
      <a-button v-if="can('update')" @click="doPrintPallet">파렛트 라벨</a-button>
      <a-upload
        v-if="can('create') && !planRequired"
        :before-upload="onExcelPick"
        :show-upload-list="false"
        accept=".xlsx,.xls"
      >
        <a-button>엑셀 업로드</a-button>
      </a-upload>
<!--      <a-button v-if="can('create')" @click="openQcLookup">QC 의뢰 검색</a-button>-->
      <a-button v-if="can('create')" type="primary" @click="openPlanPopup">
        입고 예정 조회
      </a-button>
    </CommonPage>

    <!-- 품목 검색 팝업 -->
    <ItemSearchPopup v-model:open="itemPopupOpen" @select="onItemSelected" />

    <!-- 시험 의뢰 검색 팝업 -->
    <QcTestRequestPopup v-model:open="qcPopupOpen" @select="onQcRequestSelected" />

    <!-- 입고 예정 검색 팝업 — @ready 로 받은 openModal API 로 연다(BasicModal 은 visible 제어, v-model:open 무효) -->
    <InboundPlanPopup @ready="onPlanPopupReady" @select="onPlanSelected" />
  </div>
</template>

<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { ref, computed, onMounted } from 'vue';
  import * as XLSX from 'xlsx';
  import * as QRCode from 'qrcode';
  import CommonPage from '../../common/CommonPage.vue';
  import ItemSearchPopup from './popup/ItemSearchPopup.vue';
  import QcTestRequestPopup from './popup/QcTestRequestPopup.vue';
  import InboundPlanPopup from './popup/InboundPlanPopup.vue';
  import { getSearchList, getCommonPostApi, getCommonGetListApi } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ButtonGroup from '/src/views/common/ButtonGroup.vue';
  import { getFormattedFilters } from '../../common/utils.ts';
  import { usePermissionLocal } from '../../common/usePermissionLocal';

  const MENU = 'InboundRegist';
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

  // 품목 팝업
  const itemPopupOpen = ref(false);
  const activePopupField = ref<string>('item_code');

  // 시험 의뢰 팝업
  const qcPopupOpen = ref(false);

  // 입고 예정 팝업 — BasicModal 은 visible 로 제어되므로 @ready 의 openModal API 로 연다(v-model:open 무효)
  const planPopupApi = ref<{ openModal: () => void; closeModal: () => void } | null>(null);
  function onPlanPopupReady(api: { openModal: () => void; closeModal: () => void }) {
    planPopupApi.value = api;
  }
  function openPlanPopup() {
    if (!planPopupApi.value) {
      return notification.warning({
        message: '안내',
        description: '입고 예정 팝업 초기화 중입니다. 잠시 후 다시 시도하세요.',
      });
    }
    planPopupApi.value.openModal();
  }

  // 입고 예정 필수 설정 — true 면 수기 "추가"/"엑셀 업로드" 숨김(예정 조회로만 등록)
  const planRequired = ref(false);
  onMounted(async () => {
    try {
      const resp: any = await getCommonGetListApi('/wcs/inbound/plan/required', null);
      planRequired.value = !!resp?.required;
    } catch (e) {
      console.warn('[InboundRegist] plan-required 설정 조회 실패', e);
    }
  });

  // 카테고리 셀렉터 — 일반 입고 / 반품 입고. host_order.order_type 으로 전달.
  const selectedCategory = ref<string>('INBOUND');
  const categoryOptions = [
    { label: '일반', value: 'INBOUND' },
    { label: '반품', value: 'RETURN_IN' },
  ];

  // 그리드 boolean 컬럼은 문자열("true"/"false") 로 들어올 수 있어 명시 변환
  function toBool(v: any): boolean {
    if (v === true || v === 1) return true;
    if (typeof v === 'string') return v.trim().toLowerCase() === 'true';
    return false;
  }

  function todayStr(): string {
    return new Date().toISOString().slice(0, 10);
  }

  // =====================================================
  // 날짜 정규화 — DatePicker / 문자열 / Date 객체 모두 'YYYY-MM-DD' 로
  // =====================================================
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

  // =====================================================
  // 조회 — GET /rest/wcs/inbound/regist
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

      const resp = await getSearchList('/wcs/inbound/regist', {
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
      console.error('[InboundRegist] fetchHandler ERROR:', e);
      notification.error({
        message: '조회 오류',
        description: e?.message || '조회 중 오류가 발생했습니다.',
        duration: 2,
      });
      return { total: 0, records: [] };
    }
  }

  // =====================================================
  // 행 추가 — barcode 칸에 자동 구분자(P1, P2, ...)를 채워서 그룹핑 키로 활용.
  // 같은 파렛트에 묶고 싶은 행은 같은 구분자로 바꿔주면 된다.
  // 등록 시점에 구분자별로 실제 파렛트 바코드를 발번해서 치환한다.
  // =====================================================
  let _palletSep = 0;
  function nextSeparator() {
    _palletSep += 1;
    return `P${_palletSep}`;
  }
  function addRow(preset?: Record<string, any>) {
    gridRef.value?.addRow({ barcode: nextSeparator(), uom: 'EA', ...(preset || {}) });
  }

  // 구분자별로 실제 파렛트 바코드 1개씩 발번 — 같은 구분자는 같은 바코드를 공유.
  async function resolveBarcodesBySeparator(rows: any[]): Promise<Map<string, string>> {
    const seps = [...new Set(rows.map((r) => String(r.barcode || '').trim()).filter(Boolean))];
    const map = new Map<string, string>();
    for (const sep of seps) {
      // 이미 발번 규칙에 맞으면(YYYYMMDD_NNNNNN) 그대로 사용
      if (/^\d{8}_\d{6}$/.test(sep)) {
        map.set(sep, sep);
        continue;
      }
      const resp: any = await getCommonGetListApi('/wcs/inbound/regist/pallet-barcode', null);
      const real = resp?.palletBarcode || resp?.pallet_barcode;
      if (!real) throw new Error('파렛트 바코드 발번 실패');
      map.set(sep, real);
    }
    return map;
  }

  // =====================================================
  // 등록 — POST /rest/wcs/inbound/regist/register
  //
  // [정책]
  //  - 모든 행을 백엔드에 한 번에 전송
  //  - 백엔드가 (eqGroupId + barcode) 로 그룹핑하여 파렛트별 host_order 생성
  //  - 파렛트 단위 트랜잭션, 부분 성공 허용
  //  - QC 시험의뢰는 [QC 시험의뢰 관리] 화면에서 사전 발행 필수.
  //    여기서는 lookup 만 수행하여 마스터 없으면 에러로 차단.
  // =====================================================
  async function doRegister() {
    const allRows = (gridRef.value?.getData?.() ?? gridRef.value?.getCURows?.() ?? []) as any[];
    const newRows = allRows.filter((row: any) => !row.id);

    if (newRows.length === 0) {
      return notification.warning({
        message: '안내',
        description: '추가된 행이 없습니다. [추가] 버튼으로 행을 먼저 추가하세요.',
      });
    }

    // 필수값 검증
    const invalid = newRows.find(
      (r: any) => !r.item_code || !r.item_qty || !r.eq_group_id || !r.barcode || !r.uom,
    );
    if (invalid) {
      return notification.warning({
        message: '입력 오류',
        description: '그룹 코드, 품목코드, 입고수량, 파렛트 바코드, 단위(UOM)는 필수입니다.',
      });
    }

    // 품목명 비어있는 행 경고
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

    // 파렛트 바코드 발번
    let sepToBarcode: Map<string, string>;
    try {
      sepToBarcode = await resolveBarcodesBySeparator(newRows);
    } catch (e: any) {
      return notification.error({
        message: '파렛트 바코드 발번 실패',
        description: e?.message || '',
      });
    }

    const items = newRows.map((row: any) => ({
      eqGroupId: row.eq_group_id,
      barcode: sepToBarcode.get(String(row.barcode || '').trim()) || row.barcode,
      ownerCode: row.owner_code || 'OWN001',
      testRequired: toBool(row.test_required),
      niaRequired: toBool(row.nia_required),
      itemCode: row.item_code,
      lotNo: row.lot_no || '',
      qty: Number(row.item_qty) || 0,
      uom: row.uom,
      produceDate: normalizeDate(row.produce_date),
      expiryDate: normalizeDate(row.expiry_date),
      testRequestNo: row.test_request_no || null,
      testNo: row.test_no || null,
      planId: row.plan_id || null,
    }));

    // QC 사전체크 — test_required=true 인데 마스터 없으면 안내 후 중단
    const missingMsgs = await checkQcPrerequisites(items);
    if (missingMsgs.length > 0) {
      return notification.error({
        message: 'QC 시험의뢰 마스터 미발행',
        description:
          '다음 항목은 [QC 시험의뢰 관리] 화면에서 의뢰를 먼저 발행하세요:\n\n' +
          missingMsgs.join('\n'),
        duration: 10,
      });
    }

    await doActualRegister({ items, orderType: selectedCategory.value });
  }

  async function checkQcPrerequisites(items: any[]): Promise<string[]> {
    const date = todayStr();
    const seen = new Set<string>();
    const missing: string[] = [];
    for (const it of items) {
      if (!it.testRequired) continue;
      const key = `${it.itemCode}|${it.lotNo || ''}`;
      if (seen.has(key)) continue;
      seen.add(key);
      try {
        const resp: any = await getCommonGetListApi('/wcs/qc-test/request/lookup', {
          date,
          item_code: it.itemCode,    // ★ sku → item_code
          lot_no: it.lotNo || '',
        });
        if (!resp?.exists) {
          missing.push(`• ${it.itemCode} / LOT: ${it.lotNo || '-'}`);
        }
      } catch (e) {
        console.warn('[InboundRegist] lookup failed; treat as missing', e, it);
        missing.push(`• ${it.itemCode} / LOT: ${it.lotNo || '-'} (조회 실패)`);
      }
    }
    return missing;
  }

  async function doActualRegister(payload: any) {
    const palletKeys = new Set(payload.items.map((it: any) => `${it.eqGroupId}|${it.barcode}`));
    const palletCount = palletKeys.size;

    try {
      const resp = await getCommonPostApi('/wcs/inbound/regist/register', payload);
      const total = resp?.totalCount ?? palletCount;
      const ok = resp?.successCount ?? 0;
      const fail = resp?.failCount ?? 0;
      const results: any[] = resp?.results ?? [];

      if (resp?.success) {
        notification.success({
          message: '등록 완료',
          description: `${total} 파렛트 등록 완료 (총 ${payload.items.length}건)`,
        });
        gridRef.value?.fetch();
      } else if (ok > 0) {
        const failedDesc = results
          .filter((r) => !r.success)
          .map((r) => `• [${r.barcode}] ${r.message}`)
          .join('\n');
        notification.warning({
          message: '일부 등록 실패',
          description: `성공 ${ok} / 실패 ${fail} 파렛트\n\n실패 목록:\n${failedDesc}`,
          duration: 8,
        });
        gridRef.value?.fetch();
      } else {
        const desc = results.length
          ? results.map((r) => `• [${r.barcode || '-'}] ${r.message}`).join('\n')
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
      return notification.warning({
        message: '안내',
        description: '삭제할 항목을 선택하세요.',
      });
    }

    const hostOrderKeys = [
      ...new Set(checkedRows.map((r: any) => r.host_order_key).filter(Boolean)),
    ];

    if (hostOrderKeys.length === 0) {
      return notification.warning({
        message: '안내',
        description: '저장되지 않은 행은 삭제할 수 없습니다.',
      });
    }

    createConfirm({
      iconType: 'warning',
      title: () => '삭제 확인',
      content: () => `${hostOrderKeys.length}건의 입고등록을 삭제하시겠습니까?`,
      onOk: async () => {
        try {
          const resp = await getCommonPostApi('/wcs/inbound/regist/delete', { hostOrderKeys });
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
  // [wcs-ops Step 16] 체크 행 재검수 요청
  // =====================================================
  async function doRetryInspection() {
    const rows = gridRef.value?.getCheckedRows?.() ?? [];
    if (rows.length === 0) {
      return notification.warning({ message: '안내', description: '재검수할 행을 선택하세요.' });
    }
    const targets = rows.filter((r: any) => r.host_order_key);
    if (targets.length === 0) {
      return notification.warning({
        message: '안내',
        description: '저장된 주문만 재검수 가능합니다.',
      });
    }
    createConfirm({
      iconType: 'warning',
      title: () => '재검수 요청',
      content: () => `${targets.length}건에 대해 재검수를 요청합니다. (INSPECTION_FAILED 만 허용)`,
      onOk: async () => {
        for (const r of targets) {
          try {
            await getCommonPostApi(
              `/rest/wcs/host-order/${encodeURIComponent(
                r.host_system_code || 'WMS',
              )}/${encodeURIComponent(r.host_order_key)}/inspection-retry`,
              { operator: 'UI', reason: '재검수 요청' },
            );
          } catch (e: any) {
            notification.error({
              message: '재검수 실패',
              description: `${r.host_order_key}: ${e?.message || ''}`,
            });
          }
        }
        notification.success({
          message: '재검수 요청 완료',
          description: `${targets.length}건 처리됨.`,
        });
        gridRef.value?.fetch();
      },
    });
  }

  // =====================================================
  // [wcs-ops Step 16] 체크 행 주문 취소
  // =====================================================
  async function doCancelOrders() {
    const rows = gridRef.value?.getCheckedRows?.() ?? [];
    if (rows.length === 0) {
      return notification.warning({ message: '안내', description: '취소할 행을 선택하세요.' });
    }
    const targets = rows.filter((r: any) => r.host_order_key);
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
      try {
        await getCommonPostApi(
          `/rest/wcs/host-order/${encodeURIComponent(
            r.host_system_code || 'WMS',
          )}/${encodeURIComponent(r.host_order_key)}/cancel`,
          { operator: 'UI', reason: reason.trim() },
        );
      } catch (e: any) {
        notification.error({
          message: '취소 실패',
          description: `${r.host_order_key}: ${e?.message || ''}`,
        });
      }
    }
    notification.success({ message: '취소 처리 완료', description: `${targets.length}건 처리됨.` });
    gridRef.value?.fetch();
  }

  // =====================================================
  // 메타 버튼 핸들러
  // =====================================================
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

    // SKU 선택 시 nia_required 기본값을 grid 현재 행에 미러
    const niaDefault = toBool(record?.nia_required);
    try {
      const grid: any = gridRef.value;
      const rowKey = grid?.getFocusedCell?.()?.rowKey;
      if (rowKey != null) {
        grid?.setValue?.(rowKey, 'nia_required', niaDefault);
        const lotNo = String(grid?.getValue?.(rowKey, 'lot_no') ?? '');
        tryAutoFillQc(rowKey, code, lotNo);
      }
    } catch (_) {
      /* grid 미준비 — 무시 */
    }
  }

  // =====================================================
  // QC 시험 의뢰 자동 채움 / 팝업 통합
  //
  // 자동 채움 트리거: SKU 선택 직후 + "QC 의뢰 검색" 버튼 클릭 시.
  // (입고일자=오늘, sku, lot_no) 단위로 백엔드에 단건 조회한다.
  //  - 결과 존재 → test_request_no / test_no 자동 채움 + test_required=true
  //  - 결과 없음 → 사용자 수기 입력 (기존 동작 유지)
  // =====================================================
  async function tryAutoFillQc(rowKey: any, itemCode: string, lotNo: string): Promise<boolean> {
    if (!itemCode) return false;
    try {
      const resp: any = await getCommonGetListApi('/wcs/qc-test/request/lookup', {
        date: todayStr(),
        item_code: itemCode,   // ★ sku → item_code
        lot_no: lotNo || '',
      });
      if (!resp?.exists || !resp.record) return false;
      applyQcRecordToRow(rowKey, resp.record);
      notification.info({
        message: 'QC 시험 의뢰 자동 채움',
        description: `의뢰번호 ${resp.record.test_request_no}`,
        duration: 2,
      });
      return true;
    } catch (e: any) {
      console.warn('[InboundRegist] QC lookup failed', e);
      return false;
    }
  }

  function applyQcRecordToRow(rowKey: any, record: any) {
    const grid: any = gridRef.value;
    if (!grid || rowKey == null) return;
    grid.setValue?.(rowKey, 'test_required', true);
    grid.setValue?.(rowKey, 'test_request_no', record.test_request_no ?? '');
    grid.setValue?.(rowKey, 'test_no', record.test_no ?? '');
  }

  function openQcLookup() {
    const grid: any = gridRef.value;
    const rowKey = grid?.getFocusedCell?.()?.rowKey;
    if (rowKey == null) {
      notification.warning({
        message: '안내',
        description: '먼저 적용할 행을 클릭하세요.',
      });
      return;
    }
    const sku = String(grid?.getValue?.(rowKey, 'item_code') ?? '');
    const lot = String(grid?.getValue?.(rowKey, 'lot_no') ?? '');
    if (sku) {
      tryAutoFillQc(rowKey, sku, lot).then((hit) => {
        if (!hit) qcPopupOpen.value = true;
      });
    } else {
      qcPopupOpen.value = true;
    }
  }

  function onQcRequestSelected(record: any) {
    const grid: any = gridRef.value;
    const rowKey = grid?.getFocusedCell?.()?.rowKey;
    if (rowKey == null) {
      notification.warning({
        message: '안내',
        description: '적용할 행을 먼저 클릭하세요.',
      });
      return;
    }
    applyQcRecordToRow(rowKey, record);
    if (record.sku && !grid.getValue?.(rowKey, 'item_code')) {
      grid.setValue?.(rowKey, 'item_code', record.sku);
    }
    if (record.lot_no && !grid.getValue?.(rowKey, 'lot_no')) {
      grid.setValue?.(rowKey, 'lot_no', record.lot_no);
    }
  }

  // =====================================================
  // 입고 예정 선택 — 예정 정보로 새 행을 채우고 수량만 잔여수량으로 기본 세팅.
  // plan_id 를 행에 보관해 등록 시 백엔드 수량검증/연계에 사용.
  // =====================================================
  function onPlanSelected(record: any) {
    const remaining = Number(record.remaining_qty);
    addRow({
      item_code: record.item_code,
      item_name: record.item_name,
      lot_no: record.lot_no || '',
      owner_code: record.item_owner || 'OWN001',
      // item_qty: remaining > 0 ? remaining : record.planned_qty,
      uom: record.uom || 'EA',
      produce_date: record.produce_date,
      expiry_date: record.expiry_date,
      test_required: toBool(record.test_required),
      nia_required: toBool(record.nia_required),
      test_request_no: record.test_request_no,
      plan_id: record.id,
    });
    notification.info({
      message: '입고 예정 선택',
      description: `${record.item_code} / LOT ${record.lot_no || '-'} (잔여 ${remaining})`,
      duration: 2,
    });
  }

  // =====================================================
  // 엑셀 업로드 — 헤더 1행, 데이터 N행. pallet_no(구분자) 기준으로 파렛트 묶음.
  // =====================================================
  async function onExcelPick(file: File) {
    try {
      const buf = await file.arrayBuffer();
      const wb = XLSX.read(buf, { type: 'array', cellDates: true });
      const ws = wb.Sheets[wb.SheetNames[0]];
      if (!ws) throw new Error('빈 시트입니다.');
      const rows = XLSX.utils.sheet_to_json<any>(ws, { defval: '', blankrows: false });
      if (!rows.length) throw new Error('데이터가 없습니다.');

      for (const r of rows) {
        const sep = String(r.pallet_no ?? r.PALLET_NO ?? '').trim();
        if (!sep) continue;
        addRow({
          barcode: sep,
          eq_group_id: r.eq_group_id ?? r.EQ_GROUP_ID ?? '',
          owner_code: r.owner_code ?? r.OWNER_CODE ?? 'OWN001',
          item_code: r.item_code ?? r.ITEM_CODE ?? '',
          lot_no: r.lot_no ?? r.LOT_NO ?? '',
          item_qty: Number(r.item_qty ?? r.ITEM_QTY ?? 0) || 0,
          uom: r.uom ?? r.UOM ?? 'EA',
          test_required: toBool(r.test_required ?? r.TEST_REQUIRED ?? false),
          nia_required: toBool(r.nia_required ?? r.NIA_REQUIRED ?? false),
          test_request_no: r.test_request_no ?? r.TEST_REQUEST_NO ?? '',
          produce_date: normalizeDate(r.produce_date ?? r.PRODUCE_DATE),
          expiry_date: normalizeDate(r.expiry_date ?? r.EXPIRY_DATE),
        });
      }
      notification.success({
        message: '엑셀 로드 완료',
        description: `${rows.length}건 행 추가됨. 검토 후 [등록]을 누르세요.`,
      });
    } catch (e: any) {
      notification.error({ message: '엑셀 로드 실패', description: e?.message || '' });
    }
    return false; // a-upload 자동 업로드 차단
  }

  // =====================================================
  // 파렛트 라벨 인쇄
  // =====================================================
  async function qrDataUrl(text: string): Promise<string> {
    try {
      return await QRCode.toDataURL(text, { margin: 1, width: 200 });
    } catch {
      return '';
    }
  }

  function esc(v: any): string {
    if (v == null) return '';
    return String(v).replace(/[&<>"']/g, (c) => {
      const m: Record<string, string> = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
      };
      return m[c];
    });
  }

  function renderPalletHtml(label: any, qrUrl: string): string {
    const items = (label.items || [])
      .map(
        (it: any) =>
          `<tr><td>${esc(it.itemCode)}</td><td>${esc(it.itemName)}</td><td>${esc(
            it.lotNo,
          )}</td><td>${esc(it.qty)}</td><td>${esc(it.uom)}</td><td>${esc(it.expiryDate)}</td></tr>`,
      )
      .join('');
    return `
      <div class="label">
        <div class="header"><span>PALLET</span><span>${esc(label.palletBarcode)}</span></div>
        <div class="qr-wrap">
          <img src="${qrUrl}" alt="QR" />
          <div class="code-text">${esc(label.barcodes?.code128 || label.palletBarcode)}</div>
        </div>
        <div class="meta">
          <div><b>Host</b>${esc(label.hostOrderKey)}</div>
          <div><b>Group</b>${esc(label.eqGroupId)} <b style="min-width:60px">Owner</b>${esc(
      label.ownerCode,
    )}</div>
          <div><b>입고일자</b>${esc(label.inboundDate)}</div>
          <div><b>시험여부</b>${
            label.testRequired ? 'YES' : 'NO'
          } <b style="min-width:60px">박스수</b>${esc(label.boxCount)}</div>
        </div>
        <table>
          <thead><tr><th>코드</th><th>품명</th><th>Lot</th><th>수량</th><th>UOM</th><th>유효기간</th></tr></thead>
          <tbody>${items}</tbody>
        </table>
      </div>`;
  }

  function openPrintWindow(bodyHtml: string) {
    const w = window.open('', '_blank', 'width=720,height=900');
    if (!w) {
      notification.error({ message: '팝업 차단', description: '인쇄용 팝업을 허용해주세요.' });
      return;
    }
    w.document.write(`<!doctype html><html><head><meta charset="utf-8"><title>Label Print</title>
<style>
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; padding: 12px; color: #000; }
  .label { border: 1px solid #000; padding: 12px; margin: 0 0 12px; width: 380px; box-sizing: border-box; }
  .label .header { font-size: 13px; font-weight: 700; margin-bottom: 8px; display: flex; justify-content: space-between; }
  .label .qr-wrap { display: flex; align-items: center; gap: 12px; margin: 6px 0; }
  .label .qr-wrap img { width: 130px; height: 130px; }
  .label .code-text { font-family: 'Courier New', monospace; font-size: 13px; letter-spacing: 1px; word-break: break-all; }
  .label .meta { font-size: 11px; line-height: 1.6; }
  .label .meta b { display: inline-block; min-width: 70px; }
  .label table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 10px; }
  .label th, .label td { border: 1px solid #999; padding: 2px 4px; text-align: left; }
  .page-break { page-break-after: always; }
  @media print { body { padding: 0; } .no-print { display: none; } }
</style></head><body>${bodyHtml}
<div class="no-print" style="margin-top:12px;text-align:center"><button onclick="window.print()">인쇄</button></div>
</body></html>`);
    w.document.close();
    w.onload = () => {
      setTimeout(() => {
        try {
          w.focus();
          w.print();
        } catch (_) {}
      }, 200);
    };
  }

  async function doPrintPallet() {
    const rows = gridRef.value?.getCheckedRows?.() ?? [];
    const targets = rows.filter((r: any) => r.host_order_key && r.barcode);
    if (targets.length === 0) {
      return notification.warning({ message: '안내', description: '저장된 행을 선택하세요.' });
    }
    const barcodes = [...new Set(targets.map((r: any) => r.barcode))];
    try {
      const htmls: string[] = [];
      for (const bc of barcodes) {
        const label: any = await getCommonGetListApi(
          `/wcs/labels/pallet/${encodeURIComponent(bc)}`,
          null,
        );
        const qrUrl = await qrDataUrl(label?.barcodes?.qr || label?.palletBarcode || bc);
        htmls.push(renderPalletHtml(label, qrUrl));
      }
      openPrintWindow(htmls.join('<div class="page-break"></div>'));
    } catch (e: any) {
      notification.error({ message: '라벨 인쇄 실패', description: e?.message || '' });
    }
  }
</script>

<style scoped>
  .inbound-regist-page {
    height: 100%;
  }
  .toolbar-label {
    font-size: 12px;
    font-weight: 500;
    color: #333;
    margin-left: 8px;
  }
</style>
