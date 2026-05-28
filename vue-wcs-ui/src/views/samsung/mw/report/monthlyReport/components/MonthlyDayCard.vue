<template>
  <div class="day-card" :class="{ 'out-month': !inMonth }" @dblclick="$emit('open-day', date)">
    <div class="day-card-head">
      <div class="day-title" :class="{ 'has-data': hasData }">
        {{ dayText }}
        <span class="weekday">{{ weekdayK }}</span>
        <span v-if="hasData" class="dot"></span>
      </div>

      <div class="day-total">총 {{ totalBoxQty }}건</div>
    </div>

    <div class="summary-box">
      <div class="summary-row">
        <span class="label">정상</span>
        <span class="value ok">{{ okBoxQty }}건</span>
      </div>
      <div class="summary-row">
        <span class="label">NG</span>
        <span class="value ng">{{ ngBoxQty }}건</span>
      </div>
      <div class="summary-row">
        <span class="label">미완료</span>
        <span class="value">{{ pendingBoxQty }}건</span>
      </div>
    </div>

    <div class="metric-box">
      <div class="metric-line">
        <span>전체운영 UPH</span>
        <b>{{ totalTimeUphText }}</b>
      </div>
      <div class="metric-line">
        <span>파렛타이저 UPH</span>
        <b>{{ palletTimeUphText }}</b>
      </div>
      <div class="metric-line">
        <span>운영시간</span>
        <b>{{ totalOperatingTimeText }}</b>
      </div>
      <div class="metric-line">
        <span>파렛타이저 운영</span>
        <b>{{ palletOperatingTimeText }}</b>
      </div>
    </div>

    <div class="time-range">
      <span>{{ firstReceivedAtText }}</span>
      <span>{{ lastPalletizedAtText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';

  type MonthlySummaryRow = {
    reportDate?: string;
    report_date?: string;
    totalBoxQty?: number;
    total_box_qty?: number;
    okBoxQty?: number;
    ok_box_qty?: number;
    ngBoxQty?: number;
    ng_box_qty?: number;
    pendingBoxQty?: number;
    pending_box_qty?: number;
    totalTimeUph?: number | null;
    total_time_uph?: number | null;
    palletTimeUph?: number | null;
    pallet_time_uph?: number | null;
    totalOperatingTime?: string | null;
    total_operating_time?: string | null;
    palletOperatingTime?: string | null;
    pallet_operating_time?: string | null;
    firstReceivedAt?: string | null;
    first_received_at?: string | null;
    lastPalletizedAt?: string | null;
    last_palletized_at?: string | null;
  };

  const props = defineProps<{
    date: string;
    inMonth: boolean;
    summary?: MonthlySummaryRow;
  }>();

  defineEmits<{
    (e: 'open-day', date: string): void;
  }>();

  function pick<T = any>(obj: Record<string, any> | undefined, ...keys: string[]): T | undefined {
    if (!obj) return undefined;
    for (const key of keys) {
      if (obj[key] !== undefined) return obj[key] as T;
    }
    return undefined;
  }

  const dayText = computed(() => props.date.slice(8, 10));

  const weekdayK = computed(() => {
    const d = new Date(props.date + 'T00:00:00');
    return ['일', '월', '화', '수', '목', '금', '토'][d.getDay()];
  });

  const totalBoxQty = computed(
    () => pick<number>(props.summary as any, 'totalBoxQty', 'total_box_qty') ?? 0,
  );
  const okBoxQty = computed(
    () => pick<number>(props.summary as any, 'okBoxQty', 'ok_box_qty') ?? 0,
  );
  const ngBoxQty = computed(
    () => pick<number>(props.summary as any, 'ngBoxQty', 'ng_box_qty') ?? 0,
  );
  const pendingBoxQty = computed(
    () => pick<number>(props.summary as any, 'pendingBoxQty', 'pending_box_qty') ?? 0,
  );

  const totalTimeUph = computed(() =>
    pick<number | null>(props.summary as any, 'totalTimeUph', 'total_time_uph'),
  );
  const palletTimeUph = computed(() =>
    pick<number | null>(props.summary as any, 'palletTimeUph', 'pallet_time_uph'),
  );
  const totalOperatingTime = computed(() =>
    pick<string | null>(props.summary as any, 'totalOperatingTime', 'total_operating_time'),
  );
  const palletOperatingTime = computed(() =>
    pick<string | null>(props.summary as any, 'palletOperatingTime', 'pallet_operating_time'),
  );
  const firstReceivedAt = computed(() =>
    pick<string | null>(props.summary as any, 'firstReceivedAt', 'first_received_at'),
  );
  const lastPalletizedAt = computed(() =>
    pick<string | null>(props.summary as any, 'lastPalletizedAt', 'last_palletized_at'),
  );

  const totalTimeUphText = computed(() =>
    totalTimeUph.value == null ? '-' : Number(totalTimeUph.value).toFixed(1),
  );
  const palletTimeUphText = computed(() =>
    palletTimeUph.value == null ? '-' : Number(palletTimeUph.value).toFixed(1),
  );
  const totalOperatingTimeText = computed(() => totalOperatingTime.value || '-');
  const palletOperatingTimeText = computed(() => palletOperatingTime.value || '-');
  const firstReceivedAtText = computed(() =>
    firstReceivedAt.value ? firstReceivedAt.value.slice(11, 16) : '-',
  );
  const lastPalletizedAtText = computed(() =>
    lastPalletizedAt.value ? lastPalletizedAt.value.slice(11, 16) : '-',
  );

  const hasData = computed(() => totalBoxQty.value > 0);
</script>

<style scoped>
  .day-card {
    min-height: 210px;
    border-radius: 18px;
    border: 1px solid #e2e8f0;
    background: rgba(255, 255, 255, 0.92);
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
    padding: 14px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    transition: all 0.2s ease;
    cursor: pointer;
  }

  .day-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  }

  .day-card.out-month {
    opacity: 0.45;
  }

  .day-card-head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 8px;
  }

  .day-title {
    font-size: 18px;
    font-weight: 700;
    color: #334155;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .day-title.has-data {
    color: #65a30d;
  }

  .weekday {
    font-size: 11px;
    font-weight: 600;
    color: #64748b;
  }

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 999px;
    background: #84cc16;
    display: inline-block;
  }

  .day-total {
    font-size: 12px;
    color: #64748b;
    font-weight: 600;
  }

  .summary-box {
    border: 1px solid #e2e8f0;
    background: linear-gradient(to bottom right, #f8fafc, rgba(241, 245, 249, 0.7));
    border-radius: 14px;
    padding: 10px;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .summary-row,
  .metric-line,
  .time-range {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    align-items: center;
  }

  .label {
    font-size: 12px;
    color: #64748b;
  }

  .value {
    font-size: 12px;
    font-weight: 700;
    color: #334155;
  }

  .value.ok {
    color: #2563eb;
  }

  .value.ng {
    color: #ef4444;
  }

  .metric-box {
    border-radius: 14px;
    padding: 10px;
    background: #fff;
    border: 1px solid #f1f5f9;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .metric-line span,
  .time-range span {
    font-size: 11px;
    color: #64748b;
  }

  .metric-line b {
    font-size: 12px;
    color: #0f172a;
  }

  .time-range {
    margin-top: auto;
    font-size: 11px;
    color: #94a3b8;
    border-top: 1px dashed #e2e8f0;
    padding-top: 8px;
  }
</style>
