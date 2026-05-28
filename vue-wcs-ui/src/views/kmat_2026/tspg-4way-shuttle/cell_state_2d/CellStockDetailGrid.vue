<!--
  CellStockDetailGrid.vue
  2D 셀 상태 관리 화면의 하단 상세 그리드

  ============================================
  설명
  ============================================
  - 선택된 셀(cell) 의 상태와 재고를 같이 보여준다.
  - 셀 정보(셀 ID/위치/타입/주행라인/잠금/입출고 금지/상태)는 항상 노출.
  - 재고가 있는 셀은 재고 항목별로 1 row, 없는 셀은 cell 정보 + "재고 없음" 1 row.
  - 데이터 소스:
    · 셀 상태(타입/주행/잠금/state) → 부모(CellState2D)가 전달하는 cells prop
      (CellStateService.getCellsByGroup 응답)
    · 재고 → getRackControlInfo (EquipmentDetailPopup 와 동일한 API)
  - 백엔드 DTO 변경 시 단일 출처 dashboard_2d/api/types.ts 만 수정하면 된다.
-->

<template>
  <div
    class="stock-grid-container"
    :class="{ collapsed: collapsedModel }"
    :style="containerStyle"
    @wheel.stop
  >
    <div class="grid-header" @click="toggleCollapse">
      <h4>셀 / 재고 상세</h4>
      <div class="header-actions">
        <span class="task-count">
          {{ cells.length }}셀 선택 · {{ records.length }}건
        </span>
        <span class="toggle-icon">{{ collapsedModel ? '▲' : '▼' }}</span>
      </div>
    </div>

    <div v-show="!collapsedModel" class="grid-body">
      <div class="grid-toolbar">
        <div class="filter-group">
          <span v-if="cells.length === 0" class="info-badge">셀을 선택하세요</span>
        </div>
        <button v-if="can('show')" class="refresh-btn" :disabled="cells.length === 0" @click="refresh">
          새로고침
        </button>
      </div>

      <div class="grid-table-wrapper">
        <table class="grid-table">
          <thead>
            <tr>
              <th
                v-for="col in columns"
                :key="col.field"
                :style="{ textAlign: col.align, width: col.width + 'px' }"
              >
                {{ col.title }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(row, idx) in paginatedRecords"
              :key="`${row.cellId}-${row.invId ?? 'cell'}-${idx}`"
              :class="{
                'row-empty-cell': row.invEmpty,
                'row-drive-only': row.driveOnly,
                'row-locked': row.locked,
              }"
            >
              <td
                v-for="col in columns"
                :key="col.field"
                :style="{ textAlign: col.align }"
                :class="getCellClass(row, col.field)"
                v-html="formatCell(row, col.field)"
              />
            </tr>
            <tr v-if="paginatedRecords.length === 0">
              <td :colspan="columns.length" class="empty-row">
                좌측 2D 맵에서 셀을 선택하세요.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
          ◀
        </button>
        <template v-for="(page, index) in visiblePages" :key="index">
          <span v-if="page === -1" class="page-ellipsis">...</span>
          <button
            v-else
            class="page-btn"
            :class="{ active: page === currentPage }"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>
        </template>
        <button
          class="page-btn"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
        >
          ▶
        </button>
        <span class="page-info">
          {{ records.length }}건 중 {{ (currentPage - 1) * pageSize + 1 }}-{{
            Math.min(currentPage * pageSize, records.length)
          }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { getRackControlInfo } from '../dashboard_2d/api/equipmentControlApi';
  import type { CellStateInfo, InventoryItem } from '../dashboard_2d/api/types';
  import { RackType, StockStatusLabels, enumLabel } from '../constants';
  import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';

  const MENU = 'CellState2D';
  const { can } = usePermissionLocal(MENU);

  interface Column {
    field: string;
    title: string;
    align: 'left' | 'center' | 'right';
    width: number;
  }

  /** 그리드 한 행 — 셀 정보 + (있으면) 재고 1건 */
  interface GridRow {
    // 셀 정보 (항상)
    cellId: string;
    locCode: string;
    location: string;
    rackTypeLabel: string;
    cellStateLabel: string;
    cellFlags: string;
    driveOnly: boolean;
    locked: boolean;
    inboundForbidden: boolean;
    outboundForbidden: boolean;

    // 재고 정보 (재고 없으면 invEmpty=true 인 placeholder row 1건)
    invEmpty: boolean;
    invId?: string;
    skuCode?: string;
    itemCode?: string;
    palletId?: string;
    qty?: number;
    lotNo?: string;
    stockStatus?: number;
    inbDatetime?: string;
    expiredDate?: string;
    itemOwner?: string;
    itemPriority?: number;
  }

  const props = defineProps<{
    /** 부모(CellState2D)가 전달하는 선택된 셀의 전체 정보 */
    cells: CellStateInfo[];
    eqGroupId: string;
    height?: number;
    collapsed?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'update:collapsed', v: boolean): void;
  }>();

  // ============================================
  // State
  // ============================================

  const collapsedModel = computed({
    get: () => !!props.collapsed,
    set: (v: boolean) => emit('update:collapsed', v),
  });

  const inventoryByCell = ref<Map<string, InventoryItem[]>>(new Map());
  const currentPage = ref(1);
  const pageSize = ref(20);

  // ============================================
  // Columns
  // ============================================

  const columns: Column[] = [
    // ── 셀 정보 ──
    { field: 'locCode', title: '셀 ID', align: 'center', width: 150 },
    { field: 'location', title: '위치(R-B-L)', align: 'center', width: 110 },
    { field: 'rackTypeLabel', title: '셀 타입', align: 'center', width: 110 },
    { field: 'cellFlags', title: '셀 상태', align: 'center', width: 130 },
    { field: 'cellStateLabel', title: '재고 상태', align: 'center', width: 110 },
    // ── 재고 정보 ──
    { field: 'skuCode', title: 'SKU', align: 'center', width: 110 },
    { field: 'itemCode', title: '품목코드', align: 'center', width: 110 },
    { field: 'palletId', title: 'Pallet ID', align: 'center', width: 130 },
    { field: 'qty', title: '수량', align: 'right', width: 80 },
    { field: 'lotNo', title: 'Lot No.', align: 'center', width: 110 },
    { field: 'itemOwner', title: '화주', align: 'center', width: 100 },
    { field: 'stockStatus', title: '재고상태', align: 'center', width: 100 },
    { field: 'itemPriority', title: '우선순위', align: 'right', width: 80 },
    { field: 'inbDatetime', title: '입고일시', align: 'center', width: 150 },
    { field: 'expiredDate', title: '유통기한', align: 'center', width: 110 },
  ];

  // ============================================
  // Computed - records
  // ============================================

  const records = computed<GridRow[]>(() => {
    const sorted = [...props.cells].sort((a, b) =>
      String(a.stor_loc ?? a.rack_id ?? '').localeCompare(String(b.stor_loc ?? b.rack_id ?? '')),
    );

    const rows: GridRow[] = [];
    for (const cell of sorted) {
      const base = buildCellBase(cell);
      const invList = inventoryByCell.value.get(String(cell.rack_id)) ?? [];

      if (invList.length === 0) {
        rows.push({ ...base, invEmpty: true });
        continue;
      }
      for (const inv of invList) {
        rows.push({
          ...base,
          invEmpty: false,
          invId: inv.id,
          skuCode: inv.skuCode,
          itemCode: inv.itemCode,
          palletId: inv.palletId,
          qty: inv.qty,
          lotNo: inv.lotNo,
          stockStatus: inv.stockStatus,
          inbDatetime: inv.inbDatetime,
          expiredDate: inv.expiredDate,
          itemOwner: inv.itemOwner,
          itemPriority: inv.itemPriority,
        });
      }
    }
    return rows;
  });

  // ============================================
  // Layout
  // ============================================

  const containerStyle = computed(() => {
    if (collapsedModel.value) return { height: '40px' };
    const h = props.height ?? 320;
    return { height: `${h}px` };
  });

  const totalPages = computed(() => Math.max(1, Math.ceil(records.value.length / pageSize.value)));

  const paginatedRecords = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return records.value.slice(start, start + pageSize.value);
  });

  const visiblePages = computed(() => {
    const pages: number[] = [];
    const total = totalPages.value;
    const current = currentPage.value;
    pages.push(1);
    const start = Math.max(2, current - 1);
    const end = Math.min(total - 1, current + 1);
    if (start > 2) pages.push(-1);
    for (let i = start; i <= end; i++) pages.push(i);
    if (end < total - 1) pages.push(-1);
    if (total > 1) pages.push(total);
    return pages;
  });

  // ============================================
  // Methods
  // ============================================

  function toggleCollapse() {
    collapsedModel.value = !collapsedModel.value;
  }

  function goToPage(page: number) {
    if (page >= 1 && page <= totalPages.value) currentPage.value = page;
  }

  function buildCellBase(cell: CellStateInfo): Omit<GridRow, 'invEmpty'> {
    const driveOnly = cell.drive_only_yn === true;
    const locked = cell.locked === true;
    const inboundForbidden = cell.inbound_forbidden === true;
    const outboundForbidden = cell.outbound_forbidden === true;

    const flags: string[] = [];
    if (driveOnly) flags.push('주행라인');
    if (locked) flags.push('잠금');
    if (inboundForbidden) flags.push('입고금지');
    if (outboundForbidden) flags.push('출고금지');
    const cellFlags = flags.length > 0 ? flags.join(' · ') : '정상';

    return {
      cellId: String(cell.rack_id ?? ''),
      locCode: cell.stor_loc ?? String(cell.rack_id ?? ''),
      location: `R${cell.row ?? '-'}-B${cell.bay ?? '-'}-L${cell.level ?? '-'}`,
      rackTypeLabel: enumLabel(RackType, cell.type, `타입 ${cell.type}`),
      cellStateLabel: stateCodeLabel(cell.state_code, driveOnly),
      cellFlags,
      driveOnly,
      locked,
      inboundForbidden,
      outboundForbidden,
    };
  }

  function stateCodeLabel(code: string | null | undefined, driveOnly: boolean): string {
    if (driveOnly) return '주행 전용';
    switch (code) {
      case 'PRODUCT':
        return '적재';
      case 'EMPTY_BOX':
        return '빈박스';
      case 'OUTBOUND':
        return '출고중';
      case 'INBOUND':
        return '입고중';
      case 'INBOUND_READY':
        return '입고 대기';
      case 'EMPTY_OUT':
        return '공출고 감지';
      case 'DOUBLE_IN':
        return '이중입고 감지';
      case 'EMPTY':
        return '비어 있음';
      default:
        return code ? String(code) : '비어 있음';
    }
  }

  function getCellClass(row: GridRow, field: string): string {
    if (row.invEmpty && isInventoryField(field)) return 'cell-muted';
    if (field === 'cellFlags') {
      if (row.driveOnly) return 'flag-drive';
      if (row.locked) return 'flag-lock';
      if (row.inboundForbidden || row.outboundForbidden) return 'flag-forbid';
      return 'flag-ok';
    }
    if (field === 'cellStateLabel') {
      if (row.driveOnly) return 'flag-drive';
    }
    return '';
  }

  const INVENTORY_FIELDS = new Set([
    'skuCode',
    'itemCode',
    'palletId',
    'qty',
    'lotNo',
    'itemOwner',
    'stockStatus',
    'itemPriority',
    'inbDatetime',
    'expiredDate',
  ]);

  function isInventoryField(field: string): boolean {
    return INVENTORY_FIELDS.has(field);
  }

  function formatCell(row: GridRow, field: string): string {
    // 재고 컬럼인데 재고가 없는 cell → '-' 한 번만 (첫 컬럼인 skuCode 위치에 안내문구)
    if (row.invEmpty && isInventoryField(field)) {
      return field === 'skuCode' ? '재고 없음' : '';
    }

    const value = (row as any)[field];
    if (value === null || value === undefined || value === '') return '';

    if (field === 'stockStatus') {
      const num = typeof value === 'number' ? value : Number(value);
      if (Number.isNaN(num)) return String(value);
      return StockStatusLabels[num] ?? `상태 ${num}`;
    }
    if (field === 'expiredDate') {
      const s = String(value);
      return s.length >= 10 ? s.slice(0, 10) : s;
    }
    if (field === 'inbDatetime') {
      const s = String(value);
      return s.length >= 16 ? s.slice(0, 16) : s;
    }
    return escapeHtml(String(value));
  }

  function escapeHtml(s: string): string {
    return s
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // ============================================
  // Fetch — 셀 별 재고 조회
  // ============================================

  async function fetchInventory() {
    if (props.cells.length === 0 || !props.eqGroupId) {
      inventoryByCell.value = new Map();
      currentPage.value = 1;
      return;
    }
    const next = new Map<string, InventoryItem[]>();
    const responses = await Promise.all(
      props.cells.map((c) =>
        getRackControlInfo(props.eqGroupId, String(c.rack_id))
          .then((info) => ({ id: String(c.rack_id), inv: info?.inventory ?? [] }))
          .catch((e) => {
            console.warn('[CellStockDetailGrid] getRackControlInfo failed', c.rack_id, e);
            return { id: String(c.rack_id), inv: [] as InventoryItem[] };
          }),
      ),
    );
    for (const r of responses) next.set(r.id, r.inv);
    inventoryByCell.value = next;
    currentPage.value = 1;
  }

  function refresh() {
    fetchInventory();
  }

  // ============================================
  // Watch & Page size
  // ============================================

  watch(
    () => [props.cells, props.eqGroupId],
    () => fetchInventory(),
    { deep: true, immediate: true },
  );

  function recalcPageSize() {
    if (collapsedModel.value) return;
    const headerH = 40;
    const toolbarH = 41;
    const paginationH = totalPages.value > 1 ? 44 : 0;
    const theadH = 33;
    const rowH = 33;
    const bottomPadding = 8;

    const panelH = props.height ?? 320;
    const available = Math.max(
      panelH - headerH - toolbarH - paginationH - theadH - bottomPadding,
      rowH * 3,
    );
    const rows = Math.floor(available / rowH);
    pageSize.value = Math.min(Math.max(rows, 5), 50);

    if (currentPage.value > totalPages.value) {
      currentPage.value = Math.max(totalPages.value, 1);
    }
  }

  watch(
    () => [props.height, collapsedModel.value, records.value.length],
    () => recalcPageSize(),
    { immediate: true },
  );

  defineExpose({ refresh });
</script>

<style scoped>
  .stock-grid-container {
    width: 100%;
    background: rgba(30, 34, 45, 0.98);
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    z-index: 100;
    display: flex;
    flex-direction: column;
  }

  .stock-grid-container.collapsed {
    height: 40px !important;
  }

  .grid-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 16px;
    background: rgba(255, 255, 255, 0.05);
    cursor: pointer;
    user-select: none;
  }

  .grid-header h4 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: #e5eaf3;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .task-count {
    font-size: 12px;
    color: #909399;
    background: rgba(64, 158, 255, 0.2);
    padding: 2px 8px;
    border-radius: 10px;
  }

  .toggle-icon {
    font-size: 10px;
    color: #909399;
  }

  .grid-body {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .grid-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }

  .filter-group {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  .info-badge {
    padding: 4px 12px;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 4px;
    color: #909399;
    font-size: 12px;
  }

  .refresh-btn {
    padding: 4px 12px;
    background: rgba(103, 194, 58, 0.2);
    border: 1px solid rgba(103, 194, 58, 0.3);
    border-radius: 4px;
    color: #67c23a;
    font-size: 12px;
    cursor: pointer;
  }

  .refresh-btn:hover:not(:disabled) {
    background: rgba(103, 194, 58, 0.3);
  }

  .refresh-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  .grid-table-wrapper {
    flex: 1;
    min-height: 0;
    overflow: auto;
    overscroll-behavior: contain;
  }

  .grid-table {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
  }

  .grid-table th,
  .grid-table td {
    padding: 8px 12px;
    text-align: left;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .grid-table th {
    background: rgba(30, 34, 45, 0.98);
    color: #909399;
    font-weight: 500;
    position: sticky;
    top: 0;
    z-index: 3;
    background-clip: padding-box;
  }

  .grid-table td {
    color: #c0c4cc;
  }

  .grid-table tbody tr:hover td {
    background: rgba(64, 158, 255, 0.05);
  }

  .row-empty-cell td {
    color: #909399;
  }

  .cell-muted {
    color: #606266;
  }

  .flag-ok {
    color: #67c23a;
  }
  .flag-drive {
    color: #909399;
  }
  .flag-lock {
    color: #f56c6c;
    font-weight: 600;
  }
  .flag-forbid {
    color: #e6a23c;
    font-weight: 600;
  }

  .empty-row {
    text-align: center !important;
    color: #606266 !important;
    padding: 24px !important;
  }

  .pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    padding: 8px 16px;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    background: rgba(255, 255, 255, 0.02);
  }

  .page-btn {
    min-width: 28px;
    height: 28px;
    padding: 0 8px;
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 4px;
    color: #909399;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .page-btn:hover:not(:disabled) {
    background: rgba(64, 158, 255, 0.1);
    border-color: rgba(64, 158, 255, 0.3);
    color: #409eff;
  }

  .page-btn.active {
    background: rgba(64, 158, 255, 0.2);
    border-color: #409eff;
    color: #409eff;
  }

  .page-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  .page-ellipsis {
    color: #606266;
    padding: 0 4px;
  }

  .page-info {
    margin-left: 12px;
    font-size: 11px;
    color: #606266;
  }
</style>
