<template>
  <AsrsPanel eyebrow="Inbound Queue" title="입고 대기 목록">
    <template #actions>
      <span class="inbound-queue-count-badge">{{ rows.length }}건</span>
    </template>

    <div class="asrs-ui-table-wrap">
      <table class="asrs-ui-table">
        <thead>
        <tr>
          <th>Area</th>
          <th>Item Code</th>
          <th>Qty</th>
          <th>Lot No</th>
          <th>선택 Location</th>
          <th>등급</th>
          <th>후보 수</th>
          <th>Status</th>
          <th>-</th>
        </tr>
        </thead>

        <tbody>
        <tr v-if="!rows.length">
          <td colspan="9" class="inbound-queue-table__empty-cell">
            대기 데이터가 없습니다.
          </td>
        </tr>

        <tr
          v-for="row in rows"
          :key="row.rowId"
          :class="{ 'asrs-ui-table__row--selected': selectedRowId === row.rowId }"
          @click="$emit('select', row)"
        >
          <td>{{ row.areaCode }}</td>
          <td class="asrs-ui-table__key">{{ row.itemCode }}</td>
          <td>{{ row.qty }}</td>
          <td>{{ row.lotNo || '-' }}</td>
          <td>{{ row.locationCode }}</td>
          <td>{{ row.locationGrade || '-' }}</td>
          <td>{{ row.candidateCount }}</td>
          <td>
              <span class="inbound-queue-status-badge inbound-queue-status-badge--ready">
                {{ row.status }}
              </span>
          </td>
          <td>
            <button
              class="inbound-queue-row-action"
              @click.stop="$emit('remove', row.rowId)"
            >
              삭제
            </button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </AsrsPanel>
</template>

<script setup lang="ts">
import AsrsPanel from '@/views/asrs/shared/components/ui/AsrsPanel.vue';
import type { InboundDraftRow } from '../types';

defineProps<{
  rows: InboundDraftRow[];
  selectedRowId?: string;
}>();

defineEmits<{
  (e: 'select', row: InboundDraftRow): void;
  (e: 'remove', rowId: string): void;
}>();
</script>
