<!--
  재입고 대기 파렛트 목록 모달.
  행 클릭 시 해당 파렛트를 작업대에 로드 (load-pallet emit) 후 닫힘. due 행 강조.
-->
<template>
  <BaseModal
    :open="open"
    :title="`재입고 대기 파렛트 (${rows.length}건 · 알람 간격 ${intervalMin}분)`"
    :width="760"
    :show-footer="false"
    header-class="ra-header"
    @close="$emit('close')"
  >
    <div class="ra-body">
      <table class="ra-table">
        <thead>
          <tr>
            <th>파렛트 바코드</th>
            <th>호스트오더</th>
            <th>구분</th>
            <th>대기 시작</th>
            <th class="num">경과</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="r in rows"
            :key="r.pallet_barcode"
            :class="{ due: r.due }"
            @click="$emit('load-pallet', r.pallet_barcode)"
          >
            <td class="mono">{{ r.pallet_barcode }}</td>
            <td class="mono">{{ r.host_order_key || '-' }}</td>
            <td>{{ subLabel(r.sub_order_type) }}</td>
            <td>{{ startTime(r.follow_up_since) }}</td>
            <td class="num">{{ elapsedLabel(r.elapsedMin) }}</td>
          </tr>
          <tr v-if="rows.length === 0">
            <td class="empty" colspan="5">재입고 대기 파렛트가 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </BaseModal>
</template>

<script lang="ts" setup>
  import BaseModal from './BaseModal.vue';

  interface AlarmRow {
    pallet_barcode: string;
    host_order_key?: string;
    sub_order_type?: string;
    follow_up_since?: string;
    elapsedMin: number;
    due: boolean;
  }

  defineProps<{ open: boolean; rows: AlarmRow[]; intervalMin: number }>();
  defineEmits<{ (e: 'close'): void; (e: 'load-pallet', barcode: string): void }>();

  // 출고 구분 라벨.
  function subLabel(sub?: string): string {
    if (sub === 'SAMPLE_OUT') return '시험출고';
    if (sub === 'PARTIAL_OUT') return '부분출고';
    return '-';
  }

  // 대기 시작 시각 — MM-DD HH:mm.
  function startTime(iso?: string): string {
    if (!iso) return '-';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return String(iso);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  // 경과 분 → "N분" / "H시간 M분".
  function elapsedLabel(min: number): string {
    if (min < 60) return `${min}분`;
    return `${Math.floor(min / 60)}시간 ${min % 60}분`;
  }
</script>

<style scoped>
  .ra-body {
    padding: 0;
  }
  .ra-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 12px;
  }
  .ra-table thead th {
    text-align: left;
    padding: 6px 8px;
    background: #f6f7f9;
    color: #475569;
    font-weight: 600;
    border-bottom: 1px solid #e8eaed;
  }
  .ra-table thead th.num {
    text-align: right;
  }
  .ra-table tbody td {
    padding: 7px 8px;
    border-bottom: 1px solid #f1f5f9;
    color: #0f172a;
  }
  .ra-table tbody tr {
    cursor: pointer;
  }
  .ra-table tbody tr:hover {
    background: #f1f5f9;
  }
  .ra-table tbody tr.due {
    background: #fef2f2;
  }
  .ra-table tbody tr.due:hover {
    background: #fee2e2;
  }
  .mono {
    font-family: ui-monospace, SFMono-Regular, monospace;
    font-size: 11.5px;
  }
  .num {
    text-align: right;
    font-variant-numeric: tabular-nums;
  }
  .empty {
    text-align: center;
    color: #94a3b8;
    padding: 18px 8px;
  }
</style>
