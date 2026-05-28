<template>
  <div class="inbound-page" @click="closeContextMenu">
    <div class="inbound-header">
      <h1 class="inbound-title">컨테이너 실행/입고순서 관리</h1>

      <div class="inbound-header-actions">
        <button class="btn btn-ghost" @click="onDefectExcelDownload" :disabled="loading">
          <svg
            class="h-4 w-4"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          불량보고서 양식 다운로드
        </button>

        <button class="btn btn-ghost" @click="onExcelDownload" :disabled="loading">
          <svg
            class="h-4 w-4"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M12 3v12" />
            <path d="m7 10 5 5 5-5" />
            <path d="M5 21h14" />
          </svg>
          업로드 양식 다운로드
        </button>

        <button class="btn btn-ghost" @click="onExcelUploadClick" :disabled="loading">
          <svg
            class="h-4 w-4"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M12 21V9" />
            <path d="m17 14-5-5-5 5" />
            <path d="M5 3h14" />
          </svg>
          입고 리스트 업로드(excel)
        </button>

        <button class="btn btn-ghost" @click="openImportPopup" :disabled="loading">
          <svg
            class="h-4 w-4"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M12 21V9" />
            <path d="m17 14-5-5-5 5" />
            <path d="M5 3h14" />
          </svg>
          입고 리스트 업로드(manual)
        </button>

        <input
          ref="excelInputRef"
          type="file"
          accept=".xlsx,.xls"
          style="display: none"
          @change="onExcelSelected"
        />

        <button class="btn btn-ghost" @click="onSearch" :disabled="loading">
          <svg
            class="h-4 w-4"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.3-4.3" />
          </svg>
          조회
        </button>

        <button class="btn btn-ghost" @click="onReset">초기화</button>
      </div>
    </div>

    <div class="card card-search">
      <div class="search-grid search-grid-3">
        <div class="search-row">
          <span class="search-label">기간</span>
          <DatePicker v-model="cond.startDate" />
          <span style="margin: 0 8px; color: #64748b">~</span>
          <DatePicker v-model="cond.endDate" />
        </div>

        <div class="search-row">
          <span class="search-label">주문번호</span>
          <input v-model.trim="cond.bl_no" class="form-input" placeholder="bl_no" />
        </div>

        <div class="search-row">
          <span class="search-label">컨테이너</span>
          <input v-model.trim="cond.cntr_no" class="form-input" placeholder="cntr_no" />
        </div>
      </div>
    </div>

    <div class="split-wrap">
      <div class="card card-grid split-half">
        <div class="grid-title">
          <span class="section-title">상단: 컨테이너 목록</span>
          <span class="hint">※ 실행은 1건씩만 가능 (우클릭하여 컨테이너 삭제 가능)</span>
        </div>

        <div class="grid-body">
          <SimpleGrid
            :columns="topColumns"
            :rows="topRows"
            row-key="cntr_key"
            height="100%"
            v-model:selectedKey="focusedCntrKey"
            :runningKey="runningCntrKey"
            @row-selected="onTopRowSelected"
            @row-context-menu="onRowContextMenu"
            @run="onRun"
          />
        </div>
      </div>

      <div class="card card-grid split-half">
        <div class="grid-title">
          <span class="section-title">
            하단: 입고 상세 (선택 컨테이너:
            <b>{{ focusedCntrNoDisplay || '-' }}</b
            >)
          </span>
          <span class="hint">※ 대기 상태만 드래그로 순서 변경 가능</span>
        </div>

        <div class="grid-body">
          <SimpleGrid
            :columns="bottomColumns"
            :rows="detailDraftRows"
            row-key="id"
            height="100%"
            :reorderable="true"
            @reorder="onDetailReorder"
          />
        </div>

        <div class="grid-footer">
          <div class="footer-left">
            <span class="footer-note">* 나머지 컬럼은 추후 추가 예정</span>
          </div>

          <div class="footer-actions">
            <button class="btn btn-ghost" @click="onDetailReset" :disabled="!focusedCntrKey">
              초기화
            </button>
            <button class="btn btn-primary" @click="onDetailSave" :disabled="!focusedCntrKey">
              저장
            </button>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="contextMenu.isShow"
      class="context-menu"
      :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
    >
      <div class="context-menu-item text-danger" @click.stop="onDeleteContainer">
        <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="3 6 5 6 21 6" />
          <path
            d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
          />
          <line x1="10" y1="11" x2="10" y2="17" />
          <line x1="14" y1="11" x2="14" y2="17" />
        </svg>
        컨테이너 삭제
      </div>
    </div>
    <InboundImportPasteModal
      v-if="importPopupVisible"
      v-model:visible="importPopupVisible"
      @saved="onImportSaved"
    />
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, onUnmounted, onDeactivated, reactive, ref } from 'vue';
  import DatePicker from './components/DatePicker.vue';
  import SimpleGrid from './components/SimpleGrid.vue';
  import { callApi } from '../common/api/callApi.js';
  import { downloadTemplate, parseInboundImportXlsx } from './components/inboundImportExcel';
  import ExcelJS from 'exceljs';
  import InboundImportPasteModal from './components/InboundImportPaste.vue';
  import { onBeforeRouteLeave } from 'vue-router';

  const importPopupVisible = ref(false);

  function closeImportPopup() {
    importPopupVisible.value = false;
  }

  function openImportPopup() {
    importPopupVisible.value = true;
  }

  async function onImportSaved() {
    importPopupVisible.value = false;
    await onSearch();
  }

  type Status = 'READY' | 'RUNNING' | 'COMPLETE' | 'PAUSED' | 'ABORTED';

  type InboundJobApiRow = {
    id?: string;
    job_no?: string;
    jobNo?: string;
    bl_no?: string;
    blNo?: string;
    cntr_no?: string;
    cntrNo?: string;
    inbound_date?: string;
    inboundDate?: string;
    job_status?: number | string | null;
    jobStatus?: number | string | null;
    job_status_desc?: string | null;
    jobStatusDesc?: string | null;
    job_start_dt?: string | null;
    jobStartDt?: string | null;
    job_end_dt?: string | null;
    jobEndDt?: string | null;
    sku_qty?: number | string | null;
    skuQty?: number | string | null;
    total_item_qty?: number | string | null;
    totalItemQty?: number | string | null;
    completed_item_qty?: number | string | null;
    completedItemQty?: number | string | null;
    ng_item_qty?: number | string | null;
    ngItemQty?: number | string | null;
    created_at?: string | null;
    createdAt?: string | null;
    updated_at?: string | null;
    updatedAt?: string | null;
  };

  type InboundDeliveryApiRow = {
    id: string;
    cntr_no: string;
    bl_no: string;
    inbound_date: string;
    created_at?: string;
    start_datetime?: string | null;
    complete_datetime?: string | null;
    inbound_seq?: number | string | null;
    item_code?: string | null;
    inner_item_code?: string | null;
    innerItemCode?: string | null;
    item_desc?: string | null;
    item_name?: string | null;
    item_qty?: number | null;
    reg_time?: string | null;
    inbound_status?: number | string | null;
    inboundStatus?: number | string | null;
    manual_flag: boolean;
  };

  type ContainerRow = {
    cntr_key: string;
    cntr_no: string;
    bl_no: string;
    received_at: string;
    status: Status;
    inbound_date?: string;
    job_start_dt?: string;
    job_end_dt?: string;
    sku_qty?: number;
    total_item_qty?: number;
    completed_item_qty?: number;
    ng_item_qty?: number;
    job_no?: string;
    job_status?: number;
    job_status_desc?: string;
  };

  type InboundRow = {
    id: string;
    inbound_seq: number;
    inbound_status: number;
    status: Status;
    can_reorder: boolean;
    inner_item_code: string;
    item_code: string;
    item_name: string;
    qty: number;
    inbound_time: string;
    manual_flag: boolean;
  };

  const loading = ref(false);

  const today = new Date().toISOString().slice(0, 10);
  const cond = reactive({ startDate: today, endDate: today, bl_no: '', cntr_no: '' });

  const allContainers = ref<ContainerRow[]>([]);
  const detailStore = ref<Record<string, InboundRow[]>>({});
  const focusedCntrKey = ref<string | null>(null);
  const runningCntrKey = ref<string | null>(null);
  const detailDraftRows = ref<InboundRow[]>([]);
  const detailSnapshot = ref<InboundRow[]>([]);

  const focusedCntrNoDisplay = computed(() => {
    if (!focusedCntrKey.value) return '';
    const row = allContainers.value.find((x) => x.cntr_key === focusedCntrKey.value);
    return row?.cntr_no || '';
  });

  const topColumns = [
    { key: '_run', label: '실행', width: 40, type: 'run' },
    { key: 'status', label: '상태', width: 70, type: 'status' },
    { key: 'cntr_no', label: '컨테이너번호', width: 100 },
    { key: 'bl_no', label: '주문번호(bl_no)', width: 100 },
    { key: 'inbound_date', label: '입고예정일자', width: 110, align: 'center' },
    { key: 'received_at', label: '생성시간', width: 150 },
    { key: 'job_start_dt', label: '시작시간', width: 150 },
    { key: 'job_end_dt', label: '종료시간', width: 150 },
    { key: 'sku_qty', label: 'SKU수', width: 50, align: 'right' },
    { key: 'total_item_qty', label: '총수량', width: 50, align: 'right' },
    { key: 'completed_item_qty', label: '완료수량', width: 50, align: 'right' },
    { key: 'ng_item_qty', label: 'NG수량', width: 50, align: 'right' },
  ] as any[];

  const bottomColumns = [
    { key: '_drag', label: '', width: 44, type: 'drag' },
    { key: 'status', label: '상태', width: 50, type: 'status', align: 'center' },
    { key: 'inbound_seq', label: '입고 순서', width: 70, align: 'center' },
    { key: 'inner_item_code', label: '내부상품코드', width: 180, align: 'center' },
    { key: 'item_code', label: '상품코드', width: 170, align: 'center' },
    { key: 'item_name', label: '상품명', width: 150, align: 'center' },
    { key: 'qty', label: '수량', width: 80, align: 'center' },
    { key: 'inbound_time', label: '입고시간', width: 150, align: 'center' },
    { key: 'manual_flag', label: '매뉴얼', width: 100, type: 'checkbox', align: 'center' },
  ] as any[];

  const topRows = computed(() => {
    const sd = cond.startDate;
    const ed = cond.endDate;
    const bl = cond.bl_no.trim().toLowerCase();
    const cn = cond.cntr_no.trim().toLowerCase();

    const filtered = allContainers.value.filter((r) => {
      const rDate = (r.inbound_date || r.received_at || '').slice(0, 10);
      const okDate = (!sd || rDate >= sd) && (!ed || rDate <= ed);
      const okBl = !bl || (r.bl_no || '').toLowerCase().includes(bl);
      const okCn = !cn || (r.cntr_no || '').toLowerCase().includes(cn);
      return okDate && okBl && okCn;
    });

    const prio: Record<string | number, number> = {
      READY: 0,
      RUNNING: 1,
      COMPLETE: 2,
      PAUSED: 8,
      ABORTED: 9,
    };
    return filtered.slice().sort((a, b) => {
      const pa = prio[a.status] ?? 9;
      const pb = prio[b.status] ?? 9;
      if (pa !== pb) return pa - pb;
      return String(b.received_at || '').localeCompare(String(a.received_at || ''));
    });
  });

  function onTopRowSelected(row: ContainerRow) {
    focusedCntrKey.value = row.cntr_key;
    loadDetailDraft(row.cntr_key);
  }

  /** =========================================================
   * ✅ 우클릭 삭제 (Context Menu) 로직
   * ========================================================= */
  const contextMenu = reactive({
    isShow: false,
    x: 0,
    y: 0,
    targetRow: null as ContainerRow | null,
  });

  function onRowContextMenu(row: ContainerRow, event: MouseEvent) {
    contextMenu.targetRow = row;
    contextMenu.x = event.clientX;
    contextMenu.y = event.clientY;
    contextMenu.isShow = true;
  }

  function closeContextMenu() {
    if (contextMenu.isShow) {
      contextMenu.isShow = false;
      contextMenu.targetRow = null;
    }
  }

  async function onDeleteContainer() {
    const row = contextMenu.targetRow;
    if (!row) return;

    if (row.status !== 'READY') {
      alert('대기 상태인 컨테이너만 삭제할 수 있습니다.');
      closeContextMenu();
      return;
    }

    if (!confirm(`[${row.cntr_no}] 컨테이너를 정말 삭제하시겠습니까?`)) {
      closeContextMenu();
      return;
    }

    try {
      const payload = { cntr_no: row.cntr_no, bl_no: row.bl_no, inbound_date: row.inbound_date };
      await callApi(
        'POST',
        `http://${window.location.hostname}:9500/rest/tb_mw_inbound_job/delete_delivery_job`,
        payload,
      );

      alert('삭제되었습니다.');
      await onSearch();
    } catch (error) {
      console.error('삭제 중 오류:', error);
      alert('삭제 중 오류가 발생했습니다.');
    } finally {
      closeContextMenu();
    }
  }

  /** =========================================================
   * ✅ 그리드 상태 및 실행 (Run) 로직
   * ========================================================= */
  /*async function onRun({ row, action }: { row: ContainerRow; action: string }) {
    try {
      let result;
      if (action === 'START') {
        result = await callApi(
          'POST',
          `http://${window.location.hostname}:9500/rest/tb_mw_inbound_job/start_delivery_job`,
          row,
        );
      } else if (action === 'PAUSE') {
        result = await callApi(
          'POST',
          `http://${window.location.hostname}:9500/rest/tb_mw_inbound_job/pause_delivery_job`,
          row,
        );
      } else if (action === 'ABORT') {
        result = await callApi(
          'POST',
          `http://${window.location.hostname}:9500/rest/tb_mw_inbound_job/abort_delivery_job`,
          row,
        );
      }
      if (result) {
        alert(result.message || '요청이 처리되었습니다.');
        await onSearch();
      }
    } catch (error) {
      console.error('실행 중 오류 발생: ', error);
      alert('작업 처리 중 오류가 발생했습니다.');
    }
  }*/

  async function onRun({ row, action }: { row: ContainerRow; action: string }) {
    try {
      let result;

      if (action === 'START') {
        // 1. 기존 WCS 시작 처리
        result = await callApi(
          'POST',
          `http://${window.location.hostname}:9500/rest/tb_mw_inbound_job/start_delivery_job`,
          row,
        );

        // // 2. XYZ 하차(Devanning) 지시
        const xyzPayload = { cntr_no: row.cntr_no, bl_no: row.bl_no };
        await callApi(
          'POST',
          `http://${window.location.hostname}:9500/rest/xyz/devanning/devanning_order`,
          xyzPayload,
        );
      } else if (action === 'PAUSE') {
        // 1. 기존 WCS 일시정지 처리
        result = await callApi(
          'POST',
          `http://${window.location.hostname}:9500/rest/tb_mw_inbound_job/pause_delivery_job`,
          row,
        );

        // 디베이닝 쪽 딜리트
        await callApi(
          'DELETE',
          `http://${window.location.hostname}:9500/rest/xyz/devanning/devanning_delete`,
          {},
        );
      } else if (action === 'ABORT') {
        // 🌟 [추가] 강제종료는 실행 중(RUNNING)에만 버튼이 보이므로 여기서 클릭됨.
        if (
          !confirm(
            `[${row.cntr_no}] 강제종료를 진행하시겠습니까?\n(안전한 종료를 위해 자동으로 일시정지 후 종료됩니다.)`,
          )
        ) {
          return; // 사용자가 취소하면 중단
        }

        // 🌟 Step 1: 시스템 요구조건에 맞춰 PAUSE(일시정지) API 선행 호출
        await callApi(
          'POST',
          `http://${window.location.hostname}:9500/rest/tb_mw_inbound_job/pause_delivery_job`,
          row,
        );
        await callApi(
          'POST',
          `http://${window.location.hostname}:9500/rest/xyz/devanning/devanning_cancel`,
          {},
        );

        await new Promise((resolve) => setTimeout(resolve, 300));

        result = await callApi(
          'POST',
          `http://${window.location.hostname}:9500/rest/tb_mw_inbound_job/abort_delivery_job`,
          row,
        );
        await callApi(
          'DELETE',
          `http://${window.location.hostname}:9500/rest/xyz/devanning/devanning_delete`,
          {},
        );
      }

      // 결과 처리 및 화면 갱신
      if (result) {
        alert(result.message || '요청이 처리되었습니다.');
        await onSearch();
      }
    } catch (error) {
      console.error('실행 중 오류 발생: ', error);
      alert('작업 처리 중 오류가 발생했습니다.');
    }
  }

  function deepClone<T>(v: T): T {
    return JSON.parse(JSON.stringify(v));
  }
  function normalizeNumber(v: any, def = 0): number {
    const n = Number(v);
    return Number.isFinite(n) ? n : def;
  }

  function normalizeInboundStatus(v: any): number {
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
  }

  function itemProcStatus(st: any): Status {
    const n = normalizeInboundStatus(st);
    if (n === 0) return 'READY';
    if (n === 1) return 'RUNNING';
    if (n === 2 || n === 3) return 'COMPLETE';
    if (n === 8) return 'PAUSED';
    if (n === 9) return 'ABORTED';
    return 'READY';
  }

  function jobProcStatus(st: any): Status {
    const n = normalizeNumber(st, 0);
    if (n === 0) return 'READY';
    if (n === 1) return 'RUNNING';
    if (n === 2) return 'COMPLETE';
    if (n === 8) return 'PAUSED';
    if (n === 9) return 'ABORTED';
    return 'READY';
  }

  function getVal(obj: any, ...keys: string[]) {
    for (const k of keys) {
      const v = obj?.[k];
      if (v !== undefined && v !== null && v !== '') return v;
    }
    return null;
  }
  function makeCntrKey(blNo: string, cntrNo: string) {
    return `${blNo || ''}||${cntrNo || ''}`;
  }

  function onDetailReorder(payload: { from: number; to: number }) {
    const { from, to } = payload;
    if (from === to) return;
    const arr = detailDraftRows.value;
    const fromRow = arr[from];
    const toRow = arr[to];
    if (!fromRow || !toRow || !fromRow.can_reorder || !toRow.can_reorder) return;

    const waitIdx = arr.map((r, i) => (r.can_reorder ? i : -1)).filter((i) => i >= 0);
    const fromPos = waitIdx.indexOf(from);
    const toPos = waitIdx.indexOf(to);
    if (fromPos < 0 || toPos < 0) return;

    const waitRows = waitIdx.map((i) => arr[i]);
    const moved = waitRows.splice(fromPos, 1)[0];
    waitRows.splice(toPos, 0, moved);

    let wi = 0;
    detailDraftRows.value = arr.map((r) => (r.can_reorder ? waitRows[wi++] : r));
    renumberInboundSeqKeepingFixed(detailDraftRows.value);
  }

  function renumberAll(arr: InboundRow[]) {
    arr.forEach((r, i) => (r.inbound_seq = i + 1));
  }
  function renumberInboundSeqKeepingFixed(arr: InboundRow[]) {
    const n = arr.length;
    const waitCount = arr.filter((r) => r.can_reorder).length;
    if (n === 0 || waitCount === 0) return;

    const hasAnySeq = arr.some(
      (r) => Number.isFinite(Number(r.inbound_seq)) && Number(r.inbound_seq) > 0,
    );
    if (!hasAnySeq) {
      renumberAll(arr);
      return;
    }

    const fixedSeq = new Set<number>();
    arr.forEach((r) => {
      if (!r.can_reorder) {
        const s = Number(r.inbound_seq);
        if (Number.isFinite(s) && s >= 1 && s <= n) fixedSeq.add(s);
      }
    });

    const available: number[] = [];
    for (let i = 1; i <= n; i++) if (!fixedSeq.has(i)) available.push(i);

    if (available.length < waitCount) {
      renumberAll(arr);
      return;
    }

    let ai = 0;
    arr.forEach((r) => {
      if (r.can_reorder) r.inbound_seq = available[ai++];
    });
  }

  async function onDetailSave() {
    if (!focusedCntrKey.value) return;
    const saving = deepClone(detailDraftRows.value);
    renumberInboundSeqKeepingFixed(saving);

    const snapshotMap = new Map(detailSnapshot.value.map((r) => [r.id, r]));
    const targetContainer = allContainers.value.find((x) => x.cntr_key === focusedCntrKey.value);
    const targetInboundDate = targetContainer?.inbound_date || cond.startDate;

    const changedEntities = saving
      .filter((r) => {
        const original = snapshotMap.get(r.id);
        if (!original) return false;
        return original.inbound_seq !== r.inbound_seq || original.manual_flag !== r.manual_flag;
      })
      .map((r) => ({
        id: r.id,
        inbound_seq: String(r.inbound_seq),
        inbound_status: r.inbound_status,
        item_code: r.item_code,
        cntr_no: focusedCntrNoDisplay.value,
        bl_no: targetContainer?.bl_no || '',
        inbound_date: targetInboundDate,
        manual_flag: r.manual_flag,
      }));

    if (changedEntities.length === 0) {
      alert('변경사항이 없습니다.');
      return;
    }

    try {
      await callApi(
        'POST',
        `http://${window.location.hostname}:9500/rest/tb_mw_inbound_delivery/save_inbound_seq`,
        changedEntities,
      );
      detailStore.value[focusedCntrKey.value] = deepClone(saving);
      detailSnapshot.value = deepClone(saving);
      detailDraftRows.value = deepClone(saving);
      alert('저장되었습니다.');
    } catch (e) {
      console.error(e);
      alert('저장 중 오류가 발생했습니다.');
    }
  }

  function onDetailReset() {
    detailDraftRows.value = deepClone(detailSnapshot.value);
  }
  function loadDetailDraft(cntrKey: string) {
    const base = deepClone(detailStore.value[cntrKey] || []).sort(
      (a, b) => (a.inbound_seq ?? 0) - (b.inbound_seq ?? 0),
    );
    detailSnapshot.value = deepClone(base);
    detailDraftRows.value = deepClone(base);
  }

  async function onSearch() {
    await fetchAllFromApi();
  }
  function onReset() {
    const resetToday = new Date().toISOString().slice(0, 10);
    cond.startDate = resetToday;
    cond.endDate = resetToday;
    cond.bl_no = '';
    cond.cntr_no = '';
    onSearch();
  }

  onMounted(() => {
    onSearch();
    window.addEventListener('click', closeContextMenu); // 우클릭 메뉴 닫기용
  });

  onBeforeRouteLeave(() => {
    closeImportPopup();
    closeContextMenu();
  });

  onDeactivated(() => {
    closeImportPopup();
    closeContextMenu();
  });

  onUnmounted(() => {
    closeImportPopup();
    closeContextMenu();
    window.removeEventListener('click', closeContextMenu);
  });

  async function fetchAllFromApi() {
    loading.value = true;
    try {
      await fetchInboundJobFromApi();
      if (!allContainers.value || allContainers.value.length === 0) {
        detailStore.value = {};
        focusedCntrKey.value = null;
        runningCntrKey.value = null;
        detailDraftRows.value = [];
        detailSnapshot.value = [];
        return;
      }
      await fetchInboundDeliveryFromApi();
      syncFocusAfterFetch();
    } finally {
      loading.value = false;
    }
  }

  function syncFocusAfterFetch() {
    const currentKey = focusedCntrKey.value;
    const exists = currentKey && allContainers.value.some((r) => r.cntr_key === currentKey);
    const nextKey = exists ? currentKey : allContainers.value[0]?.cntr_key ?? null;
    focusedCntrKey.value = nextKey;
    if (nextKey) loadDetailDraft(nextKey);
    else {
      detailDraftRows.value = [];
      detailSnapshot.value = [];
    }
  }

  async function fetchInboundJobFromApi() {
    const params = {
      startDate: cond.startDate,
      endDate: cond.endDate,
      bl_no: cond.bl_no,
      cntr_no: cond.cntr_no,
    };
    const apiList = await callApi(
      'POST',
      `http://${window.location.hostname}:9500/rest/tb_mw_inbound_job/inbound_job_info_date`,
      params,
    );
    if (!apiList || !Array.isArray(apiList) || apiList.length === 0) {
      allContainers.value = [];
      runningCntrKey.value = null;
      return;
    }

    console.log(apiList);

    const list = apiList as InboundJobApiRow[];
    const mapped: ContainerRow[] = list
      .map((r) => {
        const blNo = String(getVal(r, 'bl_no', 'blNo') || '');
        const cntrNo = String(getVal(r, 'cntr_no', 'cntrNo') || '');
        if (!cntrNo) return null;
        const key = makeCntrKey(blNo, cntrNo);
        const jobStatusRaw = getVal(r, 'job_status', 'jobStatus');
        const jobStatus = normalizeNumber(jobStatusRaw, 2);
        const uiStatus = jobProcStatus(jobStatus);
        const createdAt = String(
          getVal(r, 'created_at', 'createdAt') || getVal(r, 'inbound_date', 'inboundDate') || '-',
        );
        const jobStart = String(getVal(r, 'job_start_dt', 'jobStartDt') || '-');
        const jobEnd = String(getVal(r, 'job_end_dt', 'jobEndDt') || '-');
        const skuQty = normalizeNumber(getVal(r, 'sku_qty', 'skuQty'), 0);
        const totalItemQty = normalizeNumber(getVal(r, 'total_item_qty', 'totalItemQty'), 0);
        const completedQty = normalizeNumber(
          getVal(r, 'completed_item_qty', 'completedItemQty'),
          0,
        );
        const ngQty = normalizeNumber(getVal(r, 'ng_item_qty', 'ngItemQty'), 0);

        return {
          cntr_key: key,
          cntr_no: cntrNo,
          bl_no: blNo,
          received_at: createdAt,
          status: uiStatus,
          inbound_date: String(getVal(r, 'inbound_date', 'inboundDate') || ''),
          job_start_dt: jobStart,
          job_end_dt: jobEnd,
          sku_qty: skuQty,
          total_item_qty: totalItemQty,
          completed_item_qty: completedQty,
          ng_item_qty: ngQty,
          job_no: String(getVal(r, 'job_no', 'jobNo') || ''),
          job_status: jobStatus,
          job_status_desc: String(getVal(r, 'job_status_desc', 'jobStatusDesc') || ''),
        } as ContainerRow;
      })
      .filter(Boolean) as ContainerRow[];
    allContainers.value = mapped;
    runningCntrKey.value = mapped.find((c) => c.status === 'RUNNING')?.cntr_key || null;
  }

  async function fetchInboundDeliveryFromApi() {
    const params = {
      startDate: cond.startDate,
      endDate: cond.endDate,
      bl_no: cond.bl_no,
      cntr_no: cond.cntr_no,
    };
    const apiList = await callApi(
      'POST',
      `http://${window.location.hostname}:9500/rest/tb_mw_inbound_delivery/inbound_delivey_info_date`,
      params,
    );
    if (!apiList || !Array.isArray(apiList) || apiList.length === 0) {
      detailStore.value = {};
      return;
    }

    const list = apiList as InboundDeliveryApiRow[];
    const byCntr: Record<string, InboundRow[]> = {};
    for (const r of list) {
      if (!r.cntr_no) continue;
      const key = makeCntrKey(
        (r as any).bl_no || (r as any).blNo || '',
        (r as any).cntr_no || (r as any).cntrNo || '',
      );
      if (!byCntr[key]) byCntr[key] = [];

      const rawSt = normalizeInboundStatus((r as any).inbound_status ?? (r as any).inboundStatus);
      const uiSt = itemProcStatus(rawSt);
      const seqNum = r.inbound_seq == null ? 0 : parseInt(String(r.inbound_seq), 10) || 0;
      const inner = String((r as any).inner_item_code ?? (r as any).innerItemCode ?? '-');
      const itemCode = String((r as any).item_code ?? '-');
      const itemName = String((r as any).item_name ?? (r as any).item_desc ?? '-');
      const rawTime =
        (r as any).reg_time || (r as any).created_at || (r as any).inbound_date || '-';

      byCntr[key].push({
        id: (r as any).id,
        inbound_seq: seqNum,
        inbound_status: rawSt,
        status: uiSt,
        can_reorder: rawSt === 0, // 0(READY) 상태만 드래그 가능
        inner_item_code: inner,
        item_code: itemCode,
        item_name: itemName,
        qty: ((r as any).item_qty ?? 0) as number,
        inbound_time: formatTimestamp(rawTime) as string,
        manual_flag: r.manual_flag,
      });
    }

    Object.keys(byCntr).forEach((key) => {
      const rows = byCntr[key];
      const hasValidSeq = rows.some((x) => (x.inbound_seq ?? 0) > 0);
      if (hasValidSeq) rows.sort((a, b) => (a.inbound_seq ?? 0) - (b.inbound_seq ?? 0));
      else {
        rows.sort(
          (a, b) =>
            String(a.inbound_time).localeCompare(String(b.inbound_time)) ||
            String(a.item_code).localeCompare(String(b.item_code)),
        );
        renumberAll(rows);
      }
      renumberInboundSeqKeepingFixed(rows);
      byCntr[key] = rows;
    });
    detailStore.value = byCntr;
  }

  /** =========================================================
   * ✅ 엑셀 다운로드/업로드 로직 통합
   * ========================================================= */
  const excelInputRef = ref<HTMLInputElement | null>(null);
  const TEMPLATE_URL = '/excel/inbound_import_template.xlsx';
  const DEFECT_TEMPLATE_URL = '/excel/inbound_defect_report_template.xlsx';

  async function onExcelDownload() {
    try {
      await downloadTemplate(TEMPLATE_URL, 'inbound_import_template.xlsx');
    } catch (e) {
      console.error(e);
      alert('업로드 템플릿 다운로드 중 오류가 발생했습니다.');
    }
  }

  // ✅ 다중 컨테이너 불량보고서 다운로드 로직 (24행 간격 블록 채우기)
  async function onDefectExcelDownload() {
    // 1. 현재 화면에 조회된 컨테이너 중 불량수량(ng_item_qty)이 1 이상인 것만 필터링
    const ngContainers = topRows.value.filter((c) => (c.ng_item_qty || 0) > 0);

    if (ngContainers.length === 0) {
      alert('현재 조회된 목록에 불량(NG)이 포함된 컨테이너가 없습니다.');
      return;
    }

    try {
      const response = await fetch(DEFECT_TEMPLATE_URL);
      const arrayBuffer = await response.arrayBuffer();

      const workbook = new ExcelJS.Workbook();
      await workbook.xlsx.load(arrayBuffer);

      const worksheet = workbook.getWorksheet(1);
      if (!worksheet) throw new Error('시트를 찾을 수 없습니다.');

      // 2. 불량 컨테이너 갯수만큼 반복하며 엑셀의 각 블록을 채움
      for (let i = 0; i < ngContainers.length; i++) {
        const container = ngContainers[i];
        const offset = i * 24; // 두 번째 컨테이너부터는 24줄 아래로 이동해서 작성

        // API 호출하여 해당 컨테이너의 불량 상세내역 가져오기
        const params = {
          cntrNo: container.cntr_no,
          inboundDate: container.inbound_date,
        };

        const defectList = (await callApi(
          'POST',
          `http://${window.location.hostname}:9500/rest/tb_mw_inbound_delivery/defect_report`,
          params,
        )) as any[];

        console.log(`[컨테이너 ${container.cntr_no}] 불량 리스트:`, defectList);

        // 3. 헤더 영역 매핑 (offset 적용)
        worksheet.getCell(`K${4 + offset}`).value = container.inbound_date;
        worksheet.getCell(`G${6 + offset}`).value = container.bl_no;
        worksheet.getCell(`K${5 + offset}`).value = container.cntr_no;
        worksheet.getCell(`K${6 + offset}`).value = '1F';
        worksheet.getCell(`K${7 + offset}`).value = container.total_item_qty || 0;
        worksheet.getCell(`G${7 + offset}`).value = container.ng_item_qty || 0;

        // 4. 불량 리스트 매핑
        if (!defectList || defectList.length === 0) {
          worksheet.getCell(`K${11 + offset}`).value = '상세 불량 내역 없음';
        } else {
          // S/N 합쳐서 23행 E열에 입력 (E23)
          const combinedBoxIds = defectList
            .map((item) => item.boxid)
            .filter(Boolean)
            .join(', ');
          worksheet.getCell(`E${23 + offset}`).value = combinedBoxIds || '-';

          // 템플릿의 No.1, No.2, No.3 칸(최대 3개)에 맞춰서 바인딩
          defectList.slice(0, 3).forEach((item, index) => {
            const rowNum = 11 + offset + index * 3;

            worksheet.getCell(`A${rowNum}`).value = item.itemname || '-';
            worksheet.getCell(`B${rowNum}`).value = item.itemcode || '-';
            worksheet.getCell(`K${rowNum}`).value = item.finalremark || '-';
          });
        }
      }

      // 5. 파일 다운로드 (파일명에 기간 표시)
      const buffer = await workbook.xlsx.writeBuffer();
      const blob = new Blob([buffer], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });

      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);

      // 파일명 지정: 시작일과 종료일이 같으면 하나만, 다르면 기간으로 표시
      const dateStr =
        cond.startDate === cond.endDate ? cond.startDate : `${cond.startDate}_${cond.endDate}`;
      link.download = `입고불량보고서_${dateStr}.xlsx`;

      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (e) {
      console.error('불량보고서 생성 오류:', e);
      alert('불량보고서 엑셀 파일을 생성하는 중 오류가 발생했습니다.');
    }
  }

  function onExcelUploadClick() {
    if (loading.value) return;
    excelInputRef.value?.click();
  }

  async function onExcelSelected(e: Event) {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    try {
      const rows = await parseInboundImportXlsx(file);

      if (!rows || rows.length === 0) {
        alert('업로드할 데이터가 없습니다.');
        return;
      }

      const importEntities = rows.map((r) => ({
        bl_no: r.bl_no,
        cntr_no: r.cntr_no,
        inbound_status: 0,
        inbound_date: r.inbound_date,
        item_code: r.item_code,
        item_desc: r.item_desc,
        item_qty: r.item_qty,
        item_type: r.item_type,
      }));

      if (importEntities.length === 0) {
        alert('저장 될 데이터가 없습니다.');
        return;
      }

      let apiResult: any;
      try {
        apiResult = await callApi(
          'POST',
          `http://${window.location.hostname}:9500/rest/tb_mw_inbound_delivery/import_inbound_delivery`,
          importEntities,
        );
      } catch (e) {
        console.error(e);
        alert('저장 중 오류가 발생했습니다.');
      }

      const missingCount = Number(apiResult?.missingMaterialCount || 0);
      const missingMaterials: string[] = apiResult?.missingMaterials || [];
      const insertedDeliveryCount = Number(apiResult?.insertedDeliveryCount || 0);
      const insertedJobCount = Number(apiResult?.insertedJobCount || 0);
      const skippedGroupCount = Number(apiResult?.skippedGroupCount || 0);
      const skippedGroups: Array<{ bl_no: string; cntr_no: string }> =
        apiResult?.skippedGroups || [];

      if (missingCount > 0) {
        const lines = (missingMaterials || [])
          .slice(0, 50)
          .map((x) => `- ${x}`)
          .join('\n');
        alert(
          `업로드 중단: 상품마스터에 없는 상품코드(item_code) ${missingCount}건\n\n누락 코드 목록:\n${lines}${
            missingMaterials.length > 50 ? `\n... 외 ${missingMaterials.length - 50}건` : ''
          }`,
        );
        return;
      }

      if (insertedDeliveryCount === 0 && skippedGroupCount > 0) {
        const list = skippedGroups
          .slice(0, 10)
          .map((g) => `${g.bl_no}/${g.cntr_no}`)
          .join('\n');
        alert(
          `업로드 결과: 저장된 데이터가 없습니다.\n이미 등록된 주문/컨테이너 그룹 ${skippedGroupCount}건이 제외되었습니다.\n\n${list}${
            skippedGroups.length > 10 ? `\n... 외 ${skippedGroups.length - 10}건` : ''
          }`,
        );
        return;
      }

      if (insertedDeliveryCount > 0 && skippedGroupCount > 0) {
        const list = skippedGroups
          .slice(0, 10)
          .map((g) => `${g.bl_no}/${g.cntr_no}`)
          .join('\n');
        alert(
          `업로드 완료\n\n- Job 생성: ${insertedJobCount}건\n- Delivery 저장: ${insertedDeliveryCount}건\n- 제외(이미 존재): ${skippedGroupCount}건\n\n제외 목록(일부):\n${list}${
            skippedGroups.length > 10 ? `\n... 외 ${skippedGroups.length - 10}건` : ''
          }`,
        );
      } else {
        alert(
          `업로드 완료\n\n- Job 생성: ${insertedJobCount}건\n- Delivery 저장: ${insertedDeliveryCount}건`,
        );
      }

      await onSearch();
    } catch (err: any) {
      console.error(err);
      alert(err?.message || '엑셀 업로드 중 오류가 발생했습니다.');
    } finally {
      input.value = '';
    }
  }

  function formatTimestamp(ts: any): string {
    if (!ts) return '-';

    if (typeof ts === 'string' && ts.includes('-')) return ts;

    const num = Number(ts);
    if (Number.isNaN(num)) return String(ts);

    const date = new Date(num);
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    const hh = String(date.getHours()).padStart(2, '0');
    const min = String(date.getMinutes()).padStart(2, '0');
    const ss = String(date.getSeconds()).padStart(2, '0');

    return `${yyyy}-${mm}-${dd} ${hh}:${min}:${ss}`;
  }
</script>

<style scoped lang="less">
  .inbound-page {
    --top-grid-card-h: clamp(300px, 40vh, 380px);
    --bottom-grid-card-h: clamp(300px, 40vh, 380px);
    padding: 24px;
    height: 100%;
    min-height: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    gap: 16px;
    background-color: fade(#5b61f6, 10%);
  }
  .inbound-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .inbound-title {
    font-size: 1.25rem;
    font-weight: 700;
  }
  .inbound-header-actions {
    display: flex;
    column-gap: 8px;
  }

  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    column-gap: 8px;
    padding: 8px 12px;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05), 0 1px 3px rgba(15, 23, 42, 0.1);
    transition: transform 0.1s ease-in-out, box-shadow 0.1s ease-in-out;
    cursor: pointer;
  }
  .btn:active {
    transform: scale(0.98);
  }
  .btn:disabled {
    cursor: not-allowed;
  }
  .btn-ghost {
    background-color: #fff;
    border: 1px solid #e5e7eb;
  }
  .btn-ghost:hover {
    background-color: #f9fafb;
  }
  .btn-primary {
    background-color: #5b61f6;
    color: #fff;
  }
  .btn-primary:hover {
    opacity: 0.9;
  }

  .card {
    border-radius: 16px;
    background-color: #fff;
    border: 1px solid #e5e7eb;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
    padding: 16px;
  }
  .card-search {
    background-image: linear-gradient(135deg, #ffffff, rgba(148, 163, 184, 0.1));
  }
  .card-grid {
    overflow: hidden;
    padding: 12px 12px 8px 12px;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  .section-title {
    font-size: 0.875rem;
    font-weight: 600;
    color: #334155;
  }
  .hint {
    font-size: 12px;
    color: #64748b;
  }

  .search-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 12px;
  }
  @media (min-width: 768px) {
    .search-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }
  @media (min-width: 1024px) {
    .search-grid-3 {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }

  .search-row {
    display: flex;
    align-items: center;
    column-gap: 8px;
  }
  .search-label {
    width: 5rem;
    font-size: 0.875rem;
    color: #475569;
  }
  .form-input {
    flex: 1;
    padding: 8px 12px;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
    background-color: #fff;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.02);
    font-size: 0.875rem;
  }

  .split-wrap {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .split-half {
    flex: 0 0 auto;
    min-height: 0;
  }
  .split-wrap > .split-half:first-child {
    height: var(--top-grid-card-h);
  }
  .split-wrap > .split-half:last-child {
    height: var(--bottom-grid-card-h);
  }

  .grid-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 2px 10px 2px;
  }
  .grid-body {
    flex: 1;
    min-height: 0;
  }

  .grid-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding-top: 10px;
    border-top: 1px solid #eef2f7;
  }
  .footer-note {
    font-size: 12px;
    color: #64748b;
  }
  .footer-actions {
    display: flex;
    gap: 8px;
  }

  /* ✅ 우클릭 커스텀 메뉴 스타일 */
  .context-menu {
    position: fixed;
    z-index: 9999;
    background-color: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
    min-width: 150px;
    padding: 4px 0;
    animation: fade-in 0.15s ease-out;
  }
  .context-menu-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: background-color 0.1s;
  }
  .context-menu-item:hover {
    background-color: #f1f5f9;
  }
  .context-menu-item.text-danger {
    color: #ef4444;
  }
  .context-menu-item.text-danger:hover {
    background-color: #fef2f2;
  }
  .context-menu-item .icon {
    width: 16px;
    height: 16px;
  }
  @keyframes fade-in {
    from {
      opacity: 0;
      transform: translateY(-4px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
</style>
