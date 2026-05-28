<template>
  <PageWrapper
    class="overflow-hidden max-full h-full"
    :contentFullHeight="true"
    :fixedHeight="true"
  >
    <div class="cell-state-page">
      <div class="master-area" :style="{ flex: masterRatio }">
        <div class="form-container">
          <BasicForm @register="formRegister" @submit="onSearchSubmit" />

          <div class="action-bar">
            <span class="action-hint">💡 셀 우클릭 → 상태 변경 메뉴</span>

            <!-- 층 전체 일괄 금지 버튼 (ZONE + 선택된 적재단 기준) (v7 추가) -->
            <div v-if="can('update')" class="zone-action-group">
              <button
                class="zone-btn zone-btn--in"
                :disabled="!canApplyZoneAction || loading.update"
                :title="
                  canApplyZoneAction
                    ? '현재 ZONE 과 적재단의 모든 셀을 입고 전체 금지(사용 금지) 처리합니다'
                    : 'ZONE 과 적재단을 먼저 선택하세요'
                "
                @click="applyZoneLevelAction('FORBID_IN_ALL', '입고 전체 금지')"
                >입고 전체 금지</button
              >
              <button
                class="zone-btn zone-btn--out"
                :disabled="!canApplyZoneAction || loading.update"
                :title="
                  canApplyZoneAction
                    ? '현재 ZONE 과 적재단의 모든 셀을 출고 전체 금지(사용 금지) 처리합니다'
                    : 'ZONE 과 적재단을 먼저 선택하세요'
                "
                @click="applyZoneLevelAction('FORBID_OUT_ALL', '출고 전체 금지')"
                >출고 전체 금지</button
              >
              <button
                class="zone-btn zone-btn--unlock"
                :disabled="!canApplyZoneAction || loading.update"
                :title="
                  canApplyZoneAction
                    ? '현재 ZONE 과 적재단의 모든 셀을 금지 해제합니다'
                    : 'ZONE 과 적재단을 먼저 선택하세요'
                "
                @click="applyZoneLevelAction('UNLOCK', '전체 금지 해제')"
                >전체 해제</button
              >
            </div>

            <div class="legend-group">
              <span
                v-for="item in legend"
                :key="item.code"
                class="legend-box"
                :style="{
                  background: item.color,
                  color: item.textColor,
                  borderColor: item.border || item.color,
                }"
                :title="item.label"
                >{{ item.label }}</span
              >
            </div>
          </div>
        </div>

        <div class="rack-grid-area" @mouseup="onDragEnd" @mouseleave="onDragEnd">
          <table
            v-if="bayList.length > 0 && rowList.length > 0"
            class="rack-table"
            cellspacing="0"
            cellpadding="0"
          >
            <thead>
              <tr>
                <th class="rack-th rack-corner"></th>
                <th v-for="bay in bayList" :key="'th-' + bay" class="rack-th">{{ bay }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rowList" :key="'tr-' + row">
                <td class="rack-row-hd">{{ row }}</td>
                <td
                  v-for="bay in bayList"
                  :key="row + '-' + bay"
                  class="rack-td"
                  :class="tdClass(row, bay)"
                  :style="tdStyle(getCellData(row, bay))"
                  @mousedown.prevent="onMouseDown($event, row, bay)"
                  @mouseenter="onMouseEnter(row, bay)"
                  @click="onCellClick(row, bay)"
                  @contextmenu.prevent="onCtxMenu($event, row, bay)"
                ></td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td class="rack-th rack-corner"></td>
                <td v-for="bay in bayList" :key="'tf-' + bay" class="rack-th">{{ bay }}</td>
              </tr>
            </tfoot>
          </table>
          <div v-else-if="!loading.fetch" class="empty-msg">ZONE을 선택 후 조회해주세요.</div>
          <div v-else class="empty-msg">조회 중…</div>
        </div>
      </div>

      <!-- 우클릭 컨텍스트 메뉴 — 선택 셀 상태에 따라 동적 렌더링
           (전체 입고/출고 금지는 상단 버튼으로 옮겨 여기서는 제거) -->
      <ul
        v-show="ctxMenu.visible"
        :style="{ top: ctxMenu.y + 'px', left: ctxMenu.x + 'px' }"
        class="ctx-menu"
      >
        <template v-for="(item, idx) in ctxMenuItems" :key="idx">
          <li v-if="item.kind === 'section'" class="ctx-section">{{ item.label }}</li>
          <li v-else-if="item.kind === 'divider'" class="ctx-divider"></li>
          <li
            v-else-if="item.visible && can('update')"
            :class="{ 'ctx-disabled': item.disabled }"
            :title="item.title || ''"
            @click="item.disabled ? null : applyCtxItem(item)"
            >{{ item.label }}</li
          >
        </template>
      </ul>

      <div class="detail-area" :style="{ flex: detailRatio }">
        <CommonPage
          ref="detailRef"
          :limit="500"
          :metas="detailMetas"
          :showSearchForm="false"
          :showPagination="false"
          :showButtons="false"
          :fetchHandler="detailFetchHandler"
          gridBodyHeight="fitToParent"
        />
      </div>
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';
  import { PageWrapper } from '/src/components/Page';
  import { BasicForm, useForm } from '/src/components/Form';
  import CommonPage from '../../common/CommonPage.vue';
  import { getSearchList, updateList } from '/src/api/common/api';
  import { useMessage } from '/src/hooks/web/useMessage';
  import { usePermissionLocal } from '../../common/usePermissionLocal';

  const MENU = 'CellStateManagement';
  const { can } = usePermissionLocal(MENU);

  /**
   * ════════════════════════════════════════════════════════════════════
   *  CellStateManagement 화면 정책 (v7)
   * ════════════════════════════════════════════════════════════════════
   *
   *  v7 변경점:
   *   1. 초기 로드 시 ZONE·적재단 모두 첫 옵션을 자동 선택 후 즉시 조회
   *   2. 서버 응답의 null/빈값은 프런트에서도 '' 로 안전하게 정규화
   *      (백엔드에서도 COALESCE 처리되나 2중 방어)
   *   3. runFetch 시작 시 cells 를 즉시 비우지 않고, 응답 수신 후에 "한 번에" 교체
   *      → 이전엔 cells=[] 로 먼저 비워서 "회색 셀" 이 순간 표시되는 현상 방지
   *   4. cellMap 의 level 중복 필터 제거 (백엔드가 이미 level 로 필터했으므로,
   *      프런트 필터가 폼값 타이밍 이슈로 잘못 작동하여 모든 셀이 NONE(회색)으로
   *      보이던 버그를 제거)
   *   5. 상단 액션바에 [입고 전체 금지], [출고 전체 금지], [전체 해제] 버튼 추가
   *      → ZONE + 현재 적재단 기준으로 해당 층 전체 셀에 적용 (서버가 일괄 처리)
   *      → 우클릭 메뉴에서는 해당 항목 제거
   *
   *  ─── 상태 변경 액션 정책 ────────────────────────────────────────
   *    ALLOW_IN       (입고허가)      : use_yn=true
   *    FORBID_IN      (입고금지)      : use_yn=true (마커 컬럼 도입 전)
   *    FORBID_IN_ALL  (입고전체금지)  : use_yn=false → LOCK 색 / 층 전체
   *    ALLOW_OUT      (출고허가)      : use_yn=true
   *    FORBID_OUT     (출고금지)      : use_yn=true
   *    FORBID_OUT_ALL (출고전체금지)  : use_yn=false → LOCK 색 / 층 전체
   *    LOCK           (사용금지)      : use_yn=false → LOCK 색
   *    UNLOCK         (금지해제)      : use_yn=true
   * ════════════════════════════════════════════════════════════════════
   */

  const { notification, createConfirm } = useMessage();

  const masterRatio = 7;
  const detailRatio = 3;

  const detailRef = ref();
  const loading = reactive({ fetch: false, update: false });
  const cells = ref<any[]>([]);
  const selectedIds = ref<Set<string>>(new Set());
  const ctxMenu = reactive({ visible: false, x: 0, y: 0 });
  const selectedCellId = ref<string | null>(null);

  const isDragging = ref(false);
  const dragStartRow = ref<number | null>(null);
  const dragStartBay = ref<number | null>(null);
  const dragEndRow = ref<number | null>(null);
  const dragEndBay = ref<number | null>(null);

  const zoneOptions = ref<{ label: string; value: string }[]>([]);
  const levelOptions = ref<{ label: string; value: number }[]>([]);

  const [formRegister, { validate, getFieldsValue, updateSchema, setFieldsValue }] = useForm({
    labelWidth: 70,
    labelAlign: 'right',
    baseColProps: { xxl: 6, lg: 8, md: 12, sm: 24 },
    actionColOptions: { span: 24 },
    alwaysShowLines: 1,
    compact: true,
    showAdvancedButton: false,
    submitButtonOptions: { text: '조회' },
    resetButtonOptions: { text: '리셋' },
    schemas: [
      {
        field: 'eq_group_id',
        label: 'ZONE',
        component: 'Select',
        componentProps: {
          options: [],
          placeholder: '선택',
          allowClear: false,
          style: { width: '140px' },
        },
        colProps: { span: 6 },
      },
      {
        field: 'level',
        label: '적재단',
        component: 'Select',
        componentProps: {
          options: [],
          placeholder: '전체',
          allowClear: true,
          style: { width: '120px' },
        },
        colProps: { span: 6 },
      },
    ],
    submitFunc: async () => {
      await runFetch();
    },
  });

  const legend = [
    { code: 'EMPTY', label: '빈셀', color: '#FFFFFF', textColor: '#333', border: '#999' },
    { code: 'NONE', label: '셀없음', color: '#999999', textColor: '#FFF', border: '#888' },
    { code: 'PRODUCT', label: '제품', color: '#0066FF', textColor: '#FFF', border: '#0055DD' },
    { code: 'EMPTY_BOX', label: '공BOX', color: '#55BBFF', textColor: '#FFF', border: '#44AAEE' },
    { code: 'INBOUND', label: '입고', color: '#22BB22', textColor: '#FFF', border: '#1EA01E' },
    { code: 'OUTBOUND', label: '출고', color: '#FFE000', textColor: '#333', border: '#EED000' },
    {
      code: 'DOUBLE_IN',
      label: '이중입고',
      color: '#FF69B4',
      textColor: '#FFF',
      border: '#EE5599',
    },
    { code: 'EMPTY_OUT', label: '공출고', color: '#FF2222', textColor: '#FFF', border: '#DD0000' },
    { code: 'CONFIRM', label: '적재확인', color: '#9933CC', textColor: '#FFF', border: '#882299' },
    { code: 'LOCK', label: '사용금지', color: '#222222', textColor: '#FFF', border: '#111' },
    { code: 'DRIVE', label: '주행라인', color: '#44BBAA', textColor: '#FFF', border: '#33AA99' },
  ];

  const detailColMeta = (
    name: string,
    term: string,
    grid_rank: number,
    grid_align: 'left' | 'center' | 'right',
    grid_width: number,
  ) => ({ name, term, grid_rank, grid_align, grid_width });

  const detailMetas = {
    menu: { resource_url: '', grid_save_url: '', routing: '', fixed_columns: 0 },
    columns: [
      detailColMeta('stor_loc', '보관위치', 1, 'center', 140),
      detailColMeta('item_code', '품목코드', 2, 'center', 110),
      detailColMeta('item_name', '품목명', 3, 'left', 150),
      detailColMeta('item_spec', '세부규격', 4, 'left', 120),
      detailColMeta('item_memo', '품목비고', 5, 'left', 120),
      detailColMeta('lot_no', 'Lot No.', 6, 'center', 120),
      detailColMeta('item_qty', '품목수량', 7, 'right', 90),
      detailColMeta('unit', '단위', 8, 'center', 70),
      detailColMeta('produce_date', '생산일자', 9, 'center', 110),
      detailColMeta('expire_date', '사용기한', 10, 'center', 110),
      detailColMeta('stock_status', '재고상태', 11, 'center', 90),
      detailColMeta('task_id', '작업ID', 12, 'center', 130),
      detailColMeta('inbound_date', '입고일', 13, 'center', 110),
    ],
    buttons: [],
  };

  const bayList = computed<number[]>(() => {
    if (!cells.value.length) return [];
    const s = new Set<number>();
    cells.value.forEach((c) => s.add(Number(c.bay)));
    return [...s].sort((a, b) => b - a);
  });
  const rowList = computed<number[]>(() => {
    if (!cells.value.length) return [];
    const s = new Set<number>();
    cells.value.forEach((c) => s.add(Number(c.row)));
    return [...s].sort((a, b) => b - a);
  });

  /**
   * row:bay 좌표 → 셀 매핑.
   * v7: 백엔드가 이미 level 로 필터하여 내려주므로 프런트에서의 중복 필터를 제거한다.
   *     (폼 값의 비동기 반영 타이밍 때문에 filter 가 오동작하여 cellMap 이 비어
   *      전체가 NONE(회색)으로 보이던 버그의 근본 원인.)
   *     같은 좌표에 여러 level 이 섞일 가능성은 서버에서 차단되므로,
   *     혹시라도 들어오면 가장 먼저 만난 항목을 유지한다 (덮어쓰기 방지).
   */
  const cellMap = computed<Map<string, any>>(() => {
    const m = new Map<string, any>();
    for (const c of cells.value) {
      const k = `${c.row}:${c.bay}`;
      if (!m.has(k)) m.set(k, c);
    }
    return m;
  });
  function getCellData(r: number, b: number) {
    return cellMap.value.get(`${r}:${b}`) || null;
  }

  const dragRectIds = computed<Set<string>>(() => {
    if (
      !isDragging.value ||
      dragStartRow.value == null ||
      dragEndRow.value == null ||
      dragStartBay.value == null ||
      dragEndBay.value == null
    )
      return new Set();
    const r1 = Math.min(dragStartRow.value, dragEndRow.value),
      r2 = Math.max(dragStartRow.value, dragEndRow.value);
    const b1 = Math.min(dragStartBay.value, dragEndBay.value),
      b2 = Math.max(dragStartBay.value, dragEndBay.value);
    const ids = new Set<string>();
    for (const row of rowList.value) {
      if (row < r1 || row > r2) continue;
      for (const bay of bayList.value) {
        if (bay < b1 || bay > b2) continue;
        const cell = getCellData(row, bay);
        if (cell && isSelectable(cell)) ids.add(cell.id);
      }
    }
    return ids;
  });

  /** 상단 전체 금지 버튼 활성화 조건 (v7) */
  const canApplyZoneAction = computed<boolean>(() => {
    const fields = (getFieldsValue() as any) || {};
    return !!fields.eq_group_id && fields.level != null && fields.level !== '';
  });

  /** 응답 레코드의 null/undefined 값을 '' 로 정규화.
   *  - 프런트 그리드·템플릿 어디에서도 "null" 문자열이 보이지 않도록 한다. (v7) */
  function normalizeRow(row: any): any {
    if (!row || typeof row !== 'object') return row;
    const out: Record<string, any> = {};
    for (const k of Object.keys(row)) {
      const v = (row as any)[k];
      out[k] = v === null || v === undefined ? '' : v;
    }
    return out;
  }
  function normalizeList(list: any[]): any[] {
    if (!Array.isArray(list)) return [];
    return list.map(normalizeRow);
  }

  async function loadZones() {
    try {
      const resp = await getSearchList('/wcs/inventory/cell-state/zones');
      const items = normalizeList(Array.isArray(resp) ? resp : []);
      zoneOptions.value = items
        .map((r: any) => r.eq_group_id)
        .filter((id: any) => !!id && id !== '')
        .map((id: string) => ({ label: id, value: id }));

      const defaultZone = zoneOptions.value.length > 0 ? zoneOptions.value[0].value : undefined;
      await updateSchema({
        field: 'eq_group_id',
        componentProps: {
          options: zoneOptions.value,
          placeholder: '선택',
          allowClear: false,
          style: { width: '140px' },
          onChange: async () => {
            await loadLevels();
            await runFetch();
          },
        },
        defaultValue: defaultZone,
      });
      if (defaultZone !== undefined) {
        setFieldsValue({ eq_group_id: defaultZone });
      }
    } catch {
      zoneOptions.value = [];
    }
  }

  async function loadLevels() {
    const fields = (getFieldsValue() as any) || {};
    if (!fields.eq_group_id) {
      levelOptions.value = [];
      await updateSchema({
        field: 'level',
        componentProps: {
          options: [],
          placeholder: '전체',
          allowClear: true,
          style: { width: '120px' },
          onChange: onLevelChanged,
        },
      });
      return;
    }
    try {
      const resp = await getSearchList('/wcs/inventory/cell-state/levels', {
        eq_group_id: fields.eq_group_id,
      });
      const items = normalizeList(Array.isArray(resp) ? resp : []);
      levelOptions.value = items
        .map((r: any) => Number(r.level))
        .filter((n: number) => !isNaN(n))
        .sort((a: number, b: number) => a - b)
        .map((n: number) => ({ label: String(n), value: n }));

      // 항상 첫번째 적재단을 기본 선택 (v7: allowClear=false 도 함께 바꿔 비어있는 상태 원천 차단)
      const defaultLevel = levelOptions.value.length > 0 ? levelOptions.value[0].value : undefined;

      await updateSchema({
        field: 'level',
        componentProps: {
          options: levelOptions.value,
          placeholder: '선택',
          allowClear: false,
          style: { width: '120px' },
          onChange: onLevelChanged,
        },
        defaultValue: defaultLevel,
      });
      if (defaultLevel !== undefined) {
        setFieldsValue({ level: defaultLevel });
      }
    } catch {
      levelOptions.value = [];
    }
  }

  /** 적재단 변경 시 자동 재조회 + detail 비움 */
  async function onLevelChanged() {
    selectedIds.value = new Set();
    selectedCellId.value = null;
    await runFetch();
  }

  async function runFetch() {
    await validate();
    const fields = (getFieldsValue() as any) || {};
    if (!fields.eq_group_id) {
      cells.value = [];
      return;
    }

    loading.fetch = true;
    try {
      const params: Record<string, any> = { eq_group_id: fields.eq_group_id };
      // level 폴백: 폼 값이 비어있으면 levelOptions 의 최솟값 사용
      if (fields.level != null && fields.level !== '') {
        params.level = fields.level;
      } else if (levelOptions.value.length > 0) {
        params.level = levelOptions.value[0].value;
        setFieldsValue({ level: params.level }); // 폼에도 반영해 UI 와 동기화
      }

      const resp = await getSearchList('/wcs/inventory/cell-state/cells', params);
      const next = normalizeList(Array.isArray(resp) ? resp : []);

      // v7: "cells=[] 먼저" 가 아니라 "응답 수신 후 한 번에 교체" → 회색 깜빡임 차단
      cells.value = next;
      selectedIds.value = new Set();
      selectedCellId.value = null;
      detailRef.value?.grid?.fetch?.({ page: 1, limit: 500, sorters: [] });
    } catch (e: any) {
      notification.error({
        message: '조회 오류',
        description: e?.message || '셀 조회 실패',
        duration: 2,
      });
      // 실패 시에도 기존 cells 를 유지 — 불필요한 화면 깜빡임 방지
    } finally {
      loading.fetch = false;
    }
  }
  function onSearchSubmit() {
    runFetch();
  }

  /** 백엔드가 state_code 를 항상 확정해 내려주므로 그대로 사용. 셀 데이터가 없으면 NONE */
  function resolveState(cell: any): string {
    if (!cell) return 'NONE';
    return cell.state_code || 'EMPTY';
  }
  function isSelectable(cell: any) {
    if (!cell) return false;
    const s = resolveState(cell);
    return s !== 'NONE' && s !== 'DRIVE';
  }
  function tdClass(row: number, bay: number) {
    const cell = getCellData(row, bay);
    const s = resolveState(cell);
    const isSelected = cell && selectedIds.value.has(cell.id);
    const isDragPreview = cell && isDragging.value && dragRectIds.value.has(cell.id);
    return {
      'rack-td--drive': s === 'DRIVE',
      'rack-td--none': s === 'NONE',
      'rack-td--selected': isSelected && !isDragging.value,
      'rack-td--drag-preview': isDragPreview,
      'rack-td--sel': s !== 'NONE' && s !== 'DRIVE',
    };
  }
  function tdStyle(cell: any) {
    const s = resolveState(cell);
    const f = legend.find((l) => l.code === s);
    return { backgroundColor: f?.color || '#fff' };
  }

  function onMouseDown(ev: MouseEvent, row: number, bay: number) {
    if (ev.button === 2) return;
    closeCtxMenu();
    if (!ev.ctrlKey && !ev.metaKey) selectedIds.value = new Set();
    isDragging.value = true;
    dragStartRow.value = row;
    dragStartBay.value = bay;
    dragEndRow.value = row;
    dragEndBay.value = bay;
  }
  function onMouseEnter(row: number, bay: number) {
    if (!isDragging.value) return;
    dragEndRow.value = row;
    dragEndBay.value = bay;
  }
  function onDragEnd() {
    const wasDragging = isDragging.value;
    if (wasDragging) {
      const newIds = new Set<string>(selectedIds.value);
      dragRectIds.value.forEach((id) => newIds.add(id));
      selectedIds.value = newIds;
    }
    isDragging.value = false;
    dragStartRow.value = null;
    dragStartBay.value = null;
    dragEndRow.value = null;
    dragEndBay.value = null;
    if (wasDragging && selectedIds.value.size > 0) {
      selectedCellId.value = null;
      detailRef.value?.grid?.fetch?.({ page: 1, limit: 500, sorters: [] });
    }
  }

  async function onCellClick(row: number, bay: number) {
    const cell = getCellData(row, bay);
    if (!cell) return;
    selectedCellId.value = cell.id;
    selectedIds.value = new Set([cell.id]);
    detailRef.value?.grid?.fetch?.({ page: 1, limit: 500, sorters: [] });
  }

  async function detailFetchHandler(
    _page: number,
    _limit: number,
    _sorters: any[],
    _searchProps: any[],
  ) {
    const ids = [...selectedIds.value];
    if (ids.length === 0) return { total: 0, records: [] };
    try {
      let records: any[] = [];
      if (ids.length === 1) {
        const resp = await getSearchList(
          `/wcs/inventory/cell-state/stock-detail/${encodeURIComponent(ids[0])}`,
        );
        records = Array.isArray(resp) ? resp : [];
      } else {
        const resp = await updateList('/wcs/inventory/cell-state/stock-detail-multi', { cell_ids: ids });
        records = Array.isArray(resp) ? resp : [];
      }
      records = normalizeList(records);
      records.sort((a, b) => String(a?.stor_loc ?? '').localeCompare(String(b?.stor_loc ?? '')));
      return { total: records.length, records };
    } catch {
      return { total: 0, records: [] };
    }
  }

  function onCtxMenu(ev: MouseEvent, row: number, bay: number) {
    const cell = getCellData(row, bay);
    if (!cell || !isSelectable(cell)) return;
    if (!selectedIds.value.has(cell.id)) selectedIds.value = new Set([cell.id]);
    ctxMenu.x = ev.clientX;
    ctxMenu.y = ev.clientY;
    ctxMenu.visible = true;
  }
  function closeCtxMenu() {
    ctxMenu.visible = false;
  }

  /**
   * 우클릭 컨텍스트 메뉴 항목 구성. (v7)
   * - 전체 입고/출고 금지 항목은 상단 액션바 버튼으로 이동 → 여기서는 제거.
   * - 남은 항목: 입고 허가/금지, 출고 허가/금지, 사용 금지(LOCK)/해제(UNLOCK)
   */
  const ctxMenuItems = computed<any[]>(() => {
    let primary: any = null;
    for (const id of selectedIds.value) {
      primary = cells.value.find((c: any) => c && c.id === id);
      if (primary) break;
    }
    const state = resolveState(primary);
    const isLocked = state === 'LOCK';

    return [
      { kind: 'section', label: '입고' },
      { kind: 'action', label: '입고 허가', action: 'ALLOW_IN', visible: true, disabled: false },
      { kind: 'action', label: '입고 금지', action: 'FORBID_IN', visible: true, disabled: false },
      { kind: 'divider' },
      { kind: 'section', label: '출고' },
      { kind: 'action', label: '출고 허가', action: 'ALLOW_OUT', visible: true, disabled: false },
      { kind: 'action', label: '출고 금지', action: 'FORBID_OUT', visible: true, disabled: false },
      { kind: 'divider' },
      { kind: 'section', label: '잠금' },
      {
        kind: 'action',
        label: '사용 금지 (LOCK)',
        action: 'LOCK',
        visible: !isLocked,
        disabled: false,
      },
      {
        kind: 'action',
        label: '금지 해제 (UNLOCK)',
        action: 'UNLOCK',
        visible: isLocked,
        disabled: false,
      },
    ];
  });

  function applyCtxItem(item: any) {
    if (!item || !item.action) return;
    applyAction(item.action, item.label);
  }

  /** 개별 셀(들) 선택 기반 액션 */
  async function applyAction(action: string, label: string) {
    closeCtxMenu();
    if (selectedIds.value.size === 0) {
      notification.error({
        message: '선택 없음',
        description: '대상 셀을 먼저 선택하세요.',
        duration: 2,
      });
      return;
    }
    createConfirm({
      iconType: 'warning',
      title: () => '상태 변경',
      content: () => `선택 ${selectedIds.value.size}개 셀에 [${label}]을(를) 적용합니다.`,
      onOk: async () => {
        loading.update = true;
        try {
          await updateList('/wcs/inventory/cell-state/update-status', {
            cell_ids: [...selectedIds.value],
            action,
          });
          notification.success({
            message: '완료',
            description: `${label} 처리되었습니다.`,
            duration: 1,
          });
          await runFetch();
        } catch (e: any) {
          notification.error({
            message: '오류',
            description: e?.message || '상태 변경 실패',
            duration: 2,
          });
        } finally {
          loading.update = false;
        }
      },
    });
  }

  /** ZONE + 적재단 기준 층 전체 일괄 액션 (v7 추가) */
  async function applyZoneLevelAction(action: string, label: string) {
    const fields = (getFieldsValue() as any) || {};
    const zone = fields.eq_group_id;
    const lv = fields.level;
    if (!zone || lv == null || lv === '') {
      notification.error({
        message: '선택 필요',
        description: 'ZONE 과 적재단을 먼저 선택하세요.',
        duration: 2,
      });
      return;
    }
    createConfirm({
      iconType: 'warning',
      title: () => '층 전체 상태 변경',
      content: () =>
        `ZONE [${zone}] 적재단 [${lv}] 의 모든 셀에 [${label}]을(를) 적용합니다. 계속하시겠습니까?`,
      onOk: async () => {
        loading.update = true;
        try {
          await updateList('/wcs/inventory/cell-state/update-status', {
            // cell_ids 는 비워 보냄 → 서버가 eq_group_id + level 기반으로 층 전체 처리
            cell_ids: [],
            action,
            eq_group_id: zone,
            level: lv,
          });
          notification.success({
            message: '완료',
            description: `${label} 처리되었습니다.`,
            duration: 1,
          });
          await runFetch();
        } catch (e: any) {
          notification.error({
            message: '오류',
            description: e?.message || '상태 변경 실패',
            duration: 2,
          });
        } finally {
          loading.update = false;
        }
      },
    });
  }

  function onGlobalClick() {
    closeCtxMenu();
  }

  onMounted(async () => {
    await loadZones();
    await loadLevels();
    await runFetch();
    document.addEventListener('click', onGlobalClick);
    document.addEventListener('mouseup', onDragEnd);
  });
  onUnmounted(() => {
    document.removeEventListener('click', onGlobalClick);
    document.removeEventListener('mouseup', onDragEnd);
  });
</script>

<style lang="less" scoped>
  @cell-w: 56px;
  @cell-h: 48px;
  @hd-w: 40px;

  .cell-state-page {
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow: hidden;
    user-select: none;
  }

  .master-area {
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
    border-bottom: 2px solid #ccc;
  }

  .detail-area {
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
  }

  .detail-area :deep(> *) {
    height: 100% !important;
  }
  .detail-area :deep(.tui-grid-container) {
    width: 100% !important;
  }
  .detail-area :deep(.overflow-hidden),
  .detail-area :deep(.overflow-hidden.max-full) {
    height: 100% !important;
    max-height: 100% !important;
  }
  .detail-area :deep(.inline-flex.flex-col.flex-1) {
    height: 100% !important;
  }
  .detail-area :deep(.flex.flex-col.flex-1) {
    flex: 1 1 0 !important;
    min-height: 0 !important;
    overflow: hidden !important;
  }
  .detail-area :deep(.form-container) {
    display: none !important;
  }
  .detail-area :deep(.vben-page-wrapper),
  .detail-area :deep(.vben-page-wrapper-content),
  .detail-area :deep(.ant-page-wrapper) {
    padding: 0 !important;
    margin: 0 !important;
  }

  .form-container {
    flex: 0 0 auto;
    background: @component-background;
    padding: 8px 10px 6px;
    :deep(.ant-form) {
      margin-bottom: 0;
    }
  }

  .action-bar {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 4px 10px;
    background: @component-background;
    border-top: 1px solid #eee;
    border-bottom: 1px solid #ddd;
    flex-wrap: wrap;
  }
  .action-hint {
    font-size: 12px;
    color: #666;
    font-weight: 500;
    white-space: nowrap;
  }

  /** ZONE+Level 일괄 액션 버튼 그룹 (v7) */
  .zone-action-group {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 2px 8px;
    border-left: 1px solid #ddd;
    border-right: 1px solid #ddd;
  }
  .zone-btn {
    height: 26px;
    padding: 0 12px;
    font-size: 12px;
    font-weight: 600;
    border-radius: 3px;
    border: 1px solid;
    cursor: pointer;
    transition: filter 0.15s;
    white-space: nowrap;
  }
  .zone-btn:hover:not(:disabled) {
    filter: brightness(1.08);
  }
  .zone-btn:active:not(:disabled) {
    filter: brightness(0.95);
  }
  .zone-btn:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
  .zone-btn--in {
    background: #ffecec;
    color: #c1272d;
    border-color: #f2a7aa;
  }
  .zone-btn--out {
    background: #fff4d9;
    color: #b58105;
    border-color: #f2d38a;
  }
  .zone-btn--unlock {
    background: #e6f4ea;
    color: #237a3a;
    border-color: #a9d9b9;
  }

  .legend-group {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 2px 2px;
    flex: 1 1 auto;
  }
  .legend-box {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 2px 8px;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
    height: 22px;
    line-height: 22px;
    border: 1px solid;
    min-width: 50px;
  }

  .rack-grid-area {
    flex: 1 1 0;
    min-height: 0;
    overflow: auto;
    background: #fff;
  }
  .rack-table {
    border-collapse: collapse;
  }
  .rack-th {
    background: linear-gradient(to bottom, #f9f9f9, #eee);
    border: 1px solid #ccc;
    text-align: center;
    font-size: 12px;
    font-weight: 600;
    color: #333;
    padding: 0;
    min-width: @cell-w;
    width: @cell-w;
    height: 28px;
    line-height: 28px;
  }
  thead .rack-th {
    position: sticky;
    top: 0;
    z-index: 3;
  }
  tfoot .rack-th {
    position: sticky;
    bottom: 0;
    z-index: 3;
  }
  .rack-corner {
    min-width: @hd-w;
    width: @hd-w;
    background: #e8e8e8;
    position: sticky;
    left: 0;
    z-index: 4;
  }
  .rack-row-hd {
    background: linear-gradient(to right, #f9f9f9, #eee);
    border: 1px solid #ccc;
    text-align: center;
    font-size: 12px;
    font-weight: 600;
    color: #333;
    min-width: @hd-w;
    width: @hd-w;
    height: @cell-h;
    padding: 0;
    position: sticky;
    left: 0;
    z-index: 1;
  }
  .rack-td {
    border: 1px solid #ccc;
    min-width: @cell-w;
    width: @cell-w;
    height: @cell-h;
    padding: 0;
    cursor: default;
  }
  .rack-td--sel {
    cursor: pointer;
  }
  .rack-td--sel:hover {
    opacity: 0.8;
  }
  .rack-td--drive {
    background-color: #44bbaa !important;
    cursor: not-allowed;
  }
  .rack-td--none {
    background-color: #999999 !important;
    cursor: not-allowed;
  }
  .rack-td--selected {
    outline: 2px solid #f00;
    outline-offset: -2px;
    z-index: 1;
    position: relative;
  }
  .rack-td--drag-preview {
    outline: 2px solid #1890ff;
    outline-offset: -2px;
    z-index: 1;
    position: relative;
  }

  .empty-msg {
    text-align: center;
    padding: 40px;
    color: #999;
  }

  .ctx-menu {
    position: fixed;
    z-index: 999;
    list-style: none;
    margin: 0;
    padding: 4px 0;
    background: #fff;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    min-width: 160px;
  }
  .ctx-menu li {
    padding: 6px 14px;
    cursor: pointer;
    font-size: 13px;
  }
  .ctx-menu li:hover:not(.ctx-section):not(.ctx-divider) {
    background: #f5f5f5;
  }
  .ctx-menu .ctx-section {
    font-size: 11px;
    font-weight: 700;
    color: #888;
    padding: 4px 14px 2px;
    cursor: default;
    background: #fafafa;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .ctx-menu .ctx-divider {
    height: 1px;
    background: #eee;
    margin: 4px 0;
    padding: 0;
    cursor: default;
  }
  .ctx-menu .ctx-disabled {
    color: #aaa;
    font-style: italic;
  }
  .ctx-menu .ctx-disabled:hover {
    background: #fff8e1 !important;
  }
</style>
