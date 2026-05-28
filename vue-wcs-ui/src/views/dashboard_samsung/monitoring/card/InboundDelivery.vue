<template>
  <div class="inbound-card-wrapper">
    <div class="custom-ribbon"></div>
    <Card
      :loading="loading"
      ref="cardRef"
      :title="title"
      :headStyle="headStyleRef"
      :bodyStyle="bodyStyleRef"
      :style="styleRef"
    >
      <div v-if="containerOptions.length" class="container-select-wrapper">
        <span class="info-label">컨테이너</span>
        <Select
          v-model:value="selectedIndex"
          size="small"
          class="container-select-box"
          :options="containerOptions"
          :bordered="false"
        />
      </div>

      <div class="card-content">
        <div class="top-section">
          <div class="top-line">
            <div class="info-group">
              <div class="info-item"
                ><span class="info-label">B/L No</span
                ><span class="info-value">{{ deliveryData || '-' }}</span></div
              >
              <div class="info-item"
                ><span class="info-label">CNTR No</span
                ><span class="info-value">{{ cntrData || '-' }}</span></div
              >
            </div>
            <div class="info-group info-group--right">
              <div class="info-item"
                ><span class="info-label">자동화 SKU</span
                ><span class="info-value">{{ automationSummary }}개</span></div
              >
              <div class="info-item"
                ><span class="info-label">수동 SKU</span
                ><span class="info-value">{{ manualSummary }}개</span></div
              >
            </div>
          </div>
          <div class="divider"></div>
        </div>

        <div class="bottom-section">
          <div class="sku-table">
            <div class="sku-row sku-row--header">
              <div class="sku-col sku-col--sku align-left">SKU</div>
              <div class="sku-col sku-col--type align-left">그룹</div>
              <div class="sku-col sku-col--desc align-left">품목</div>
              <div class="sku-col sku-col--status align-center">상태</div>
              <div class="sku-col sku-col--progress-combined align-center">진행현황</div>
            </div>

            <div
              v-for="row in skuSummary"
              :key="row.sku"
              :class="['sku-row', { 'sku-row--active': isInProgress(row) }]"
            >
              <div class="sku-col sku-col--sku align-left">{{ row.sku }}</div>
              <div class="sku-col sku-col--type align-left">{{ row.item_type || '-' }}</div>
              <div class="sku-col sku-col--desc align-left" :title="row.item_desc">{{
                row.item_desc || '-'
              }}</div>
              <div class="sku-col sku-col--status align-center">
                <span class="status-pill">
                  <span :class="['status-dot', statusClass(row)]"></span>
                  <span class="status-text">{{ statusText(row) }}</span>
                </span>
              </div>
              <div class="sku-col sku-col--progress">
                <div class="progress-track">
                  <div class="progress-fill" :style="{ width: calculatePercent(row) + '%' }"></div>
                  <div class="progress-text-overlay"
                    >{{ formatNumber(row.inbound_qty) }} / {{ formatNumber(row.total_qty) }}</div
                  >
                </div>
              </div>
              <div class="sku-col sku-col--percent align-right">{{ calculatePercent(row) }}%</div>
            </div>
            <div v-if="!skuSummary.length" class="sku-empty">진행 중인 SKU 정보가 없습니다.</div>
          </div>
        </div>
      </div>
    </Card>
  </div>
</template>

<script lang="ts" setup>
  import { Card, Select } from 'ant-design-vue';
  import { ref, computed, watch, type CSSProperties } from 'vue';

  const title = '작업 컨테이너 정보';
  interface SkuSummaryRow {
    sku: string;
    item_type: string;
    item_desc: string;
    total_qty: number;
    inbound_qty: number;
    pallet_qty: number;
    status?: string | null;
    manual_flag?: boolean | null;
    inbound_status?: number | null;
    process_status?: number | null;
  }
  interface DashboardInboundDeliveryData {
    delivery_no: string;
    cntr_no: string;
    automation_item_code: string[];
    manual_item_code: string[];
    sku_summary: SkuSummaryRow[];
  }
  const props = defineProps<{
    loading: boolean;
    data?: DashboardInboundDeliveryData | DashboardInboundDeliveryData[] | null;
    date: any;
  }>();

  function asList(data: any): DashboardInboundDeliveryData[] {
    return !data ? [] : Array.isArray(data) ? data : [data];
  }
  const styleRef = ref({
    border: '1px solid var(--dashboard-primary-color)',
    borderRadius: '0px',
    background: 'var(--dashboard-bg-color)',
    height: '100%',
  });
  const headStyleRef = computed(
    (): CSSProperties => ({
      textAlign: 'center',
      lineHeight: '0',
      minHeight: '0',
      fontSize: '1.1rem',
      backgroundColor: 'var(--dashboard-bg-color)',
      color: 'white',
      borderRadius: '0px',
      padding: '0.4rem 0.7rem 0 0.9rem',
    }),
  );
  const bodyStyleRef: CSSProperties = {
    backgroundColor: 'var(--dashboard-bg-color)',
    padding: '0.2rem 0.8rem 0.5rem 0.8rem',
    color: 'white',
  };

  const deliveryData = ref<string>('');
  const cntrData = ref<string>('');
  const automationItemCodes = ref<string[]>([]);
  const manualItemCodes = ref<string[]>([]);
  const skuSummary = ref<SkuSummaryRow[]>([]);
  const selectedIndex = ref<number | null>(null);

  const containerOptions = computed(() =>
    asList(props.data).map((d, idx) => ({ label: d.cntr_no || `컨테이너 ${idx + 1}`, value: idx })),
  );
  const automationSummary = computed(() => automationItemCodes.value?.length ?? 0);
  const manualSummary = computed(() => manualItemCodes.value?.length ?? 0);

  function setData(newData: DashboardInboundDeliveryData | null) {
    if (!newData) {
      deliveryData.value = '';
      cntrData.value = '';
      automationItemCodes.value = [];
      manualItemCodes.value = [];
      skuSummary.value = [];
      return;
    }
    deliveryData.value = newData.delivery_no ?? '';
    cntrData.value = newData.cntr_no ?? '';
    automationItemCodes.value = newData.automation_item_code ?? [];
    manualItemCodes.value = newData.manual_item_code ?? [];
    skuSummary.value = (newData.sku_summary ?? []).slice().sort(compareSkuStatus);
  }

  function calculatePercent(row: SkuSummaryRow): number {
    return !row.total_qty || row.total_qty <= 0
      ? 0
      : Math.floor((row.inbound_qty / row.total_qty) * 100);
  }
  function isInProgress(row: SkuSummaryRow): boolean {
    return statusKey(row) === 'IN_PROGRESS';
  }

  watch(
    [() => props.loading, () => props.date, () => props.data],
    () => {
      if (props.loading) return;
      const list = asList(props.data);
      if (!list.length) {
        selectedIndex.value = null;
        return;
      }
      let idx = selectedIndex.value ?? -1;
      if (idx < 0 || idx >= list.length) {
        idx = list.findIndex((d) =>
          (d.sku_summary ?? []).some((r) => statusKey(r) === 'IN_PROGRESS'),
        );
        if (idx < 0) idx = 0;
      }
      selectedIndex.value = idx;
      setData(list[idx]);
    },
    { immediate: true },
  );

  watch(selectedIndex, (idx) => {
    if (!props.loading)
      setData(
        idx == null || idx < 0 || idx >= asList(props.data).length ? null : asList(props.data)[idx],
      );
  });

  function normalizeStatus(v: any): string {
    return String(v ?? '')
      .trim()
      .toUpperCase();
  }
  function inferStatus(row: SkuSummaryRow): string {
    if (row.manual_flag === true) return 'MANUAL';
    if (row.process_status === 39) return 'ERROR';
    if (row.inbound_status === 2) return 'COMPLETE';
    if (row.inbound_status === 8) return 'WAIT';
    if (row.inbound_status === 9) return 'ERROR';

    if (Number(row.total_qty) > 0 && Number(row.pallet_qty) >= Number(row.total_qty))
      return 'COMPLETE';
    if (Number(row.inbound_qty) > 0 || Number(row.pallet_qty) > 0) return 'IN_PROGRESS';

    return 'WAIT';
  }

  function statusKey(row: SkuSummaryRow): string {
    const inferred = inferStatus(row);

    if (inferred === 'COMPLETE' || inferred === 'ERROR' || inferred === 'MANUAL') {
      return inferred;
    }
    return normalizeStatus(row.status) || inferred;
  }

  // [수정] 용어 통일: 메뉴얼 -> 수동
  function statusText(row: SkuSummaryRow): string {
    const map: any = {
      COMPLETE: '완료',
      IN_PROGRESS: '진행중',
      WAIT: '대기중',
      MANUAL: '수동 전환',
      MANUAL_SWITCH: '수동 전환',
      ERROR: '중지',
    };
    return map[statusKey(row)] || '대기중';
  }
  function statusClass(row: SkuSummaryRow): string {
    return `status-${statusKey(row).toLowerCase().replace('_', '-')}`;
  }
  const STATUS_ORDER: Record<string, number> = {
    ERROR: 1,
    IN_PROGRESS: 2,
    MANUAL: 3,
    WAIT: 4,
    COMPLETE: 5,
  };
  function compareSkuStatus(a: SkuSummaryRow, b: SkuSummaryRow): number {
    const aRank = STATUS_ORDER[statusKey(a)] ?? 99;
    const bRank = STATUS_ORDER[statusKey(b)] ?? 99;
    return aRank !== bRank ? aRank - bRank : (a.sku || '').localeCompare(b.sku || '');
  }
  function formatNumber(v: number | null | undefined): string {
    return Number(v ?? 0).toLocaleString();
  }
</script>

<style scoped>
  /* 기존 스타일 유지 */
  .inbound-card-wrapper {
    position: relative;
    height: 100%;
    display: flex;
    flex-direction: column;
  }
  .card-content {
    display: flex;
    flex-direction: column;
    flex: 1 1 auto;
  }
  .top-section {
    flex: 0 0 auto;
    font-size: 0.95rem;
  }
  .top-line {
    display: flex;
    justify-content: space-between;
    gap: 1.5rem;
  }
  .info-group {
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
  }
  .info-group--right {
    min-width: 40%;
  }
  .info-item {
    display: flex;
    align-items: center;
  }
  .info-label {
    width: 5.5rem;
    color: #93c5fd;
    font-weight: 500;
  }
  .info-group--right .info-label {
    width: 6.5rem;
  }
  .info-value {
    flex: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    font-weight: 600;
  }
  .divider {
    border-top: 1px solid rgba(255, 255, 255, 0.25);
    margin-top: 0.6rem;
  }
  .bottom-section {
    flex: 1 1 auto;
  }
  .sku-table {
    width: 100%;
  }
  .sku-row {
    display: grid;
    grid-template-columns: 1.1fr 0.8fr 0.6fr 0.8fr 1.8fr 0.3fr;
    font-size: 0.9rem;
    padding: 0.4rem 0;
    align-items: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }
  .sku-row--header {
    font-weight: 700;
    border-bottom: 2px solid rgba(255, 255, 255, 0.3);
    padding-bottom: 0.5rem;
    margin-bottom: 0.1rem;
    font-size: 0.95rem;
  }
  .align-left {
    text-align: left;
    justify-content: flex-start;
  }
  .align-center {
    text-align: center;
    justify-content: center;
  }
  .align-right {
    text-align: right;
    justify-content: flex-end;
  }
  .sku-col {
    padding-right: 0.5rem;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: flex;
    align-items: center;
  }
  .sku-col--progress-combined {
    grid-column: span 2;
  }
  .sku-col--sku {
    font-weight: 600;
  }
  .sku-col--percent {
    font-weight: bold;
    color: white;
    padding-right: 0;
  }
  .progress-track {
    width: 100%;
    height: 1.4rem;
    background-color: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    position: relative;
    display: flex;
    align-items: center;
    border-radius: 4px;
  }
  .progress-fill {
    height: 100%;
    background-color: #32cd32;
    opacity: 0.85;
    transition: width 0.4s ease;
  }
  .progress-text-overlay {
    position: absolute;
    width: 100%;
    text-align: center;
    font-size: 0.85rem;
    font-weight: bold;
    color: white;
    text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.9);
    pointer-events: none;
  }
  .status-pill {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
  }
  .status-dot {
    width: 0.7rem;
    height: 0.7rem;
    border-radius: 50%;
    border: 1px solid rgba(255, 255, 255, 0.85);
  }
  .status-text {
    font-size: 0.85rem;
  }
  .status-complete {
    background-color: #095cea;
  }
  .status-in-progress {
    background-color: #22c55e;
  }
  .status-wait {
    background-color: #ffffff;
  }
  .status-manual {
    background-color: #eab308;
  }
  .status-error {
    background-color: #ef4444;
  }
  .sku-empty {
    padding: 1.5rem 0;
    text-align: center;
    font-size: 1rem;
    color: rgba(255, 255, 255, 0.5);
  }
  .container-select-wrapper {
    position: absolute;
    top: 0.4rem;
    right: 0.8rem;
    z-index: 20;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  .container-select-box {
    min-width: 11rem;
  }
  :deep(.container-select-box .ant-select-selector) {
    background-color: rgba(15, 23, 42, 0.65) !important;
    border-radius: 6px !important;
    border: 1px solid #38bdf8 !important;
    height: auto !important;
    min-height: 1.8rem !important;
    padding: 2px 8px !important;
    display: flex;
    align-items: center;
  }
  :deep(.container-select-box .ant-select-selection-item) {
    color: #e5e7eb !important;
    font-size: 0.9rem !important;
    line-height: 1.8rem !important;
  }
  .custom-ribbon {
    position: absolute;
    top: 0;
    left: 0;
    width: 48px;
    height: 48px;
    background: var(--dashboard-primary-color);
    z-index: 10;
    clip-path: polygon(0% 0%, 100% 0%, 0% 100%);
  }
  .sku-row--active {
    background-color: rgba(34, 197, 94, 0.12);
  }
</style>
