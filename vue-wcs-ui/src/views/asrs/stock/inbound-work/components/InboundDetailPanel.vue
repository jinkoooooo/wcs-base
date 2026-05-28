<template>
  <AsrsPanel eyebrow="Inbound Detail" title="작업 상세 정보">
    <template #actions>
      <div class="inbound-detail-tab-group">
        <button
          type="button"
          class="inbound-detail-tab-button"
          :class="{ 'inbound-detail-tab-button--active': activeTab === 'detail' }"
          @click="$emit('update:activeTab', 'detail')"
        >
          상세
        </button>
        <button
          type="button"
          class="inbound-detail-tab-button"
          :class="{ 'inbound-detail-tab-button--active': activeTab === 'history' }"
          @click="$emit('update:activeTab', 'history')"
        >
          처리 결과
        </button>
      </div>
    </template>

    <template v-if="activeTab === 'detail'">
      <div v-if="!selectedRow" class="inbound-detail__scroll">
        <AsrsEmptyState message="대기 목록에서 행을 선택하면 상세 정보가 표시됩니다." />
      </div>

      <div v-else class="inbound-detail__scroll">
        <div class="inbound-detail-info-grid">
          <div class="inbound-detail-info-item">
            <span class="inbound-detail-info-item__label">Area Code</span>
            <strong>{{ selectedRow.areaCode }}</strong>
          </div>

          <div class="inbound-detail-info-item">
            <span class="inbound-detail-info-item__label">Item Code</span>
            <strong>{{ selectedRow.itemCode }}</strong>
          </div>

          <div class="inbound-detail-info-item">
            <span class="inbound-detail-info-item__label">Qty</span>
            <strong>{{ selectedRow.qty }}</strong>
          </div>

          <div class="inbound-detail-info-item">
            <span class="inbound-detail-info-item__label">Lot No</span>
            <strong>{{ selectedRow.lotNo || '-' }}</strong>
          </div>

          <div class="inbound-detail-info-item">
            <span class="inbound-detail-info-item__label">선택 Location</span>
            <strong>{{ selectedRow.locationCode }}</strong>
          </div>

          <div class="inbound-detail-info-item">
            <span class="inbound-detail-info-item__label">Location Grade</span>
            <strong>{{ selectedRow.locationGrade || '-' }}</strong>
          </div>

          <div class="inbound-detail-info-item">
            <span class="inbound-detail-info-item__label">추천 후보 수</span>
            <strong>{{ selectedRow.candidateCount }}</strong>
          </div>

          <div class="inbound-detail-info-item">
            <span class="inbound-detail-info-item__label">상태</span>
            <strong>{{ selectedRow.status }}</strong>
          </div>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="asrs-ui-table-wrap">
        <table class="asrs-ui-table">
          <thead>
          <tr>
            <th>At</th>
            <th>Stock Unit No</th>
            <th>Txn No</th>
            <th>Item Code</th>
            <th>Qty</th>
          </tr>
          </thead>

          <tbody>
          <tr v-if="!historyRows.length">
            <td colspan="5" class="inbound-detail-table__empty-cell">
              최근 처리 결과가 없습니다.
            </td>
          </tr>

          <tr v-for="row in historyRows" :key="row.rowId">
            <td>{{ row.inboundAt }}</td>
            <td class="asrs-ui-table__key">{{ row.stockUnitNo }}</td>
            <td>{{ row.txnNo }}</td>
            <td>{{ row.itemCode }}</td>
            <td>{{ row.qty }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </template>
  </AsrsPanel>
</template>

<script setup lang="ts">
import AsrsPanel from '@/views/asrs/shared/components/ui/AsrsPanel.vue';
import AsrsEmptyState from '@/views/asrs/shared/components/ui/AsrsEmptyState.vue';
import type {
  InboundDraftRow,
  InboundHistoryRow,
  InboundTabType,
} from '../types';

defineProps<{
  activeTab: InboundTabType;
  selectedRow: InboundDraftRow | null;
  historyRows: InboundHistoryRow[];
}>();

defineEmits<{
  (e: 'update:activeTab', tab: InboundTabType): void;
}>();
</script>
