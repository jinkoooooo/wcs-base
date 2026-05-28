<template>
  <div class="qc-request-mgmt-page">
    <CommonPage ref="commPageRef" :limit="20" :showPagination="true" :fetchHandler="fetchHandler">
      <ButtonGroup v-if="buttonlist" :buttonlist="buttonlist" @btn-handler="btnHandler" />
      <a-button v-if="can('create')" type="primary" @click="addRow">추가</a-button>
      <a-button
        v-if="can('create')"
        type="primary"
        style="background: #52c41a; border-color: #52c41a"
        :loading="saving"
        @click="doSave"
      >
        저장
      </a-button>
      <a-button v-if="can('update')" @click="openOutbound">출고</a-button>
    </CommonPage>
    <QcOutboundRecommendPopup
        @ready="onOutboundPopupReady"
        @issued="onOutboundIssued"
    />
  </div>
</template>

<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { ref, computed } from 'vue';
  import CommonPage from '../../common/CommonPage.vue';
  import ButtonGroup from '/src/views/common/ButtonGroup.vue';
  import QcOutboundRecommendPopup from './popup/QcOutboundRecommendPopup.vue';
  import { getSearchList, getCommonPostApi } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getFormattedFilters } from '/src/views/tspg_4way/common/utils';
  import { usePermissionLocal } from '/src/views/tspg_4way/common/usePermissionLocal';

  const MENU = 'QcTestRequestManagement';
  const { can } = usePermissionLocal(MENU);
  const { notification, createConfirm } = useMessage();

  // 메뉴 메타의 file-upload 컬럼명과 일치. 셀 값 자체가 file_id 다 (FileUploadCellRenderer 약속).
  const PDF_ID_FIELD = 'report_pdf_id';

  /**
   * 백엔드(특히 ElidomRuntimeException) 가 내려준 메시지를 최대한 추출.
   * axios error 객체부터 string 까지 다 처리.
   */
  function parseApiError(e: any): string {
    if (!e) return '알 수 없는 오류가 발생했습니다.';
    if (typeof e === 'string') return e;
    if (e.response) {
      const data = e.response.data;
      if (data) {
        if (typeof data === 'string') return data;
        if (data.message) return String(data.message);
        if (data.detail) return String(data.detail);
        if (data.error) {
          return typeof data.error === 'string'
            ? data.error
            : data.error.message || JSON.stringify(data.error);
        }
        if (data.msg) return String(data.msg);
      }
      if (e.response.statusText) return `${e.response.status} ${e.response.statusText}`;
    }
    if (e.message) return String(e.message);
    try {
      return JSON.stringify(e);
    } catch {
      return '알 수 없는 오류가 발생했습니다.';
    }
  }

  const commPageRef = ref(null as any);
  const gridRef = computed(() => commPageRef.value?.grid);
  const getFormFields = computed(() => commPageRef.value?.getFormFields);
  const validate = computed(() => commPageRef.value?.formValidate);
  const buttonlist = computed(() => commPageRef.value?.buttons);

  const saving = ref(false);

  // 추천 출고 팝업 API (BasicModal — openModal 호출로 제어)
  const outboundPopupApi = ref<{ openModal: (data?: any) => void; closeModal: () => void } | null>(null);
  function onOutboundPopupReady(api: { openModal: (data?: any) => void; closeModal: () => void }) {
    outboundPopupApi.value = api;
  }

  function todayStr(): string {
    // toISOString 은 UTC 기준이라 KST(UTC+9) 새벽 시간대에 하루 밀린 값을 돌려준다.
    // 로컬 타임존 기준 YYYY-MM-DD 로 직접 포맷.
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

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

  async function fetchHandler(page: number, limit: number, sorters: any[], searchProps: any[]) {
    try {
      try {
        await validate.value();
      } catch (_) {
        /* outOfDate 무시 */
      }
      const fields = getFormFields.value();
      const queryFilters = await getFormattedFilters(fields, searchProps);

      console.log(
        `[QcTestRequestMgmt] fetchHandler queryFilters: ${JSON.stringify(queryFilters, null, 2)}`,
      );

      const resp = await getSearchList('/wcs/qc-test/request/search', {
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
      console.error('[QcTestRequestMgmt] fetchHandler ERROR:', e);
      notification.error({
        message: '조회 오류',
        description: parseApiError(e),
        duration: 6,
      });
      return { total: 0, records: [] };
    }
  }

  function addRow() {
    gridRef.value?.addRow({
      inbound_date: todayStr(),
      status: 'DRAFT',
      fetched: false,
    });
  }

  // ─────────────────────────────────────────
  // 저장 — 신규/수정 구분 없이 변경분 전체를 한 번에 전송.
  //   서버가 (입고일자, SKU, lot_no) 키로 조회해 upsert 처리.
  // ─────────────────────────────────────────
  async function doSave() {
    const grid: any = gridRef.value;
    grid?.finishEditing?.(); // 편집 중 셀 값 강제 커밋

    const mod = grid?.getModifiedRows?.() ?? {};
    const createdRows = (mod.createdRows ?? []) as any[];
    const updatedRows = (mod.updatedRows ?? []) as any[];

    if (createdRows.length === 0 && updatedRows.length === 0) {
      return notification.warning({ message: '안내', description: '저장할 변경 사항이 없습니다.' });
    }

    // 필수값 검증 (신규/수정 공통) — 입고일자, 품목코드
    const allRows = [...createdRows, ...updatedRows];
    for (const r of allRows) {
      if (!r.inbound_date) {
        return notification.warning({
          message: '입력 오류',
          description: '입고일자는 필수입니다.',
        });
      }
      if (!r.item_code) {
        return notification.warning({
          message: '입력 오류',
          description: '품목코드는 필수입니다.',
        });
      }
    }

    // 그리드 행 → 서버 entry (id 안 보냄, 키는 inbound_date/item_code/lot_no)
    const toEntry = (r: any) => ({
      inbound_date: normalizeDate(r.inbound_date) || todayStr(),
      item_code: r.item_code,
      lot_no: r.lot_no || '',
      file_id: r[PDF_ID_FIELD] || null,
      test_wf_type: r.test_wf_type || null,
      test_req_desc: r.test_req_desc || null,
      manufactured_date: normalizeDate(r.manufactured_date),
      expiry_date: normalizeDate(r.expiry_date),
      manufactured_qty: r.manufactured_qty ?? null,
      incoming_qty: r.incoming_qty ?? null,
      req_dept: r.req_dept || null,
      submitter_order: r.submitter_order || null,
    });

    const entries = allRows.map(toEntry);

    console.info('[QcTestRequestMgmt] POST /wcs/qc-test/request/batch-save', {
      created: createdRows.length,
      updated: updatedRows.length,
      entries,
    });

    saving.value = true;
    try {
      const resp = await getCommonPostApi('/wcs/qc-test/request/batch-save', { entries });
      notification.success({
        message: '저장 완료',
        description: `신규 ${resp?.createdCount ?? 0}건 / 수정 ${
          resp?.updatedCount ?? 0
        }건 처리되었습니다.`,
      });
      grid?.fetch?.();
    } catch (e: any) {
      console.error('[QcTestRequestMgmt] batch-save ERROR:', e);
      notification.error({ message: '저장 오류', description: parseApiError(e), duration: 6 });
    } finally {
      saving.value = false;
    }
  }

  // ─────────────────────────────────────────
  // 2. 팝업 여는 함수 (이 부분 교체)
  // ─────────────────────────────────────────
  function openOutbound() {
    const checked = gridRef.value?.getCheckedRows?.() ?? [];

    if (checked.length !== 1) {
      return notification.warning({
        message: '안내',
        description: '출고할 의뢰 1건을 선택하세요.',
      });
    }

    // 1. 여기서 실제로 선택된 행의 데이터 구조를 확인합니다!
    console.log('선택된 행 데이터 전체:', checked[0]);

    const reqNo = checked[0]?.test_request_no;

    // 2. reqNo에 값이 제대로 들어왔는지 확인합니다.
    console.log('추출된 의뢰번호:', reqNo);

    if (!reqNo) {
      return notification.warning({
        message: '안내',
        description: '의뢰번호가 없는 행입니다.',
      });
    }

    // 의뢰번호를 넘기며 팝업 오픈
    if (!outboundPopupApi.value) {
      return notification.warning({
        message: '안내',
        description: '팝업 초기화 중입니다. 잠시 후 다시 시도하세요.',
      });
    }

    outboundPopupApi.value.openModal({ testRequestNo: reqNo });
  }

  // 출고 지시 완료 후 그리드 새로고침
  function onOutboundIssued() {
    gridRef.value?.fetch?.();
  }

  async function btnHandler(listenerName: any) {
    const handlers: Record<string, () => void> = {
      addBtnHandler: addRow,
      saveBtnHandler: doSave,
      outboundBtnHandler: openOutbound,
      exportBtnHandler: () => commPageRef.value?.downExcel(),
      exceldownBtnHandler: () => commPageRef.value?.downExcel(),
    };
    const handler = handlers[listenerName];
    if (handler) handler();
  }
</script>

<style scoped>
  .qc-request-mgmt-page {
    height: 100%;
  }
</style>
