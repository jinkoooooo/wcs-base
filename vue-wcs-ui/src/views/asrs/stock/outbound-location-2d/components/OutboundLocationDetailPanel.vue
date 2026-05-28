<template>
  <section class="outbound-location-2d-detail">
    <div class="outbound-location-2d-detail__header">
      <div>
        <p class="outbound-location-2d-detail__eyebrow">DETAIL</p>
        <h3 class="outbound-location-2d-detail__title">선택 로케이션 상세</h3>
      </div>
    </div>

    <div v-if="selectedCell" class="outbound-location-2d-detail__body">
      <div class="outbound-location-2d-detail__meta">
        <div class="outbound-location-2d-detail__meta-item">
          <span>Bay</span>
          <strong>{{ selectedCell.bayNo }}</strong>
        </div>
        <div class="outbound-location-2d-detail__meta-item">
          <span>Level</span>
          <strong>{{ selectedCell.levelNo }}</strong>
        </div>
      </div>

      <OutboundDepthTabs
        :model-value="selectedDepthNo"
        :depths="selectedCell.depths"
        @update:model-value="$emit('update:selected-depth-no', $event)"
      />

      <div v-if="selectedDepth" class="outbound-location-2d-detail__info-grid">
        <div class="outbound-location-2d-detail__info-item">
          <span>Location Code</span>
          <strong>{{ selectedDepth.locationCode || '-' }}</strong>
        </div>

        <div class="outbound-location-2d-detail__info-item">
          <span>Occupied</span>
          <strong>{{ selectedDepth.occupied ? 'Y' : 'N' }}</strong>
        </div>

        <div class="outbound-location-2d-detail__info-item">
          <span>Stock Unit No</span>
          <strong>{{ selectedDepth.stockUnitNo || '-' }}</strong>
        </div>

        <div class="outbound-location-2d-detail__info-item">
          <span>Item Code</span>
          <strong>{{ selectedDepth.itemCode || '-' }}</strong>
        </div>

        <div class="outbound-location-2d-detail__info-item">
          <span>Item Name</span>
          <strong>{{ selectedDepth.itemName || '-' }}</strong>
        </div>

        <div class="outbound-location-2d-detail__info-item">
          <span>Qty</span>
          <strong>{{ selectedDepth.qty }}</strong>
        </div>

        <div class="outbound-location-2d-detail__info-item">
          <span>Reserved Qty</span>
          <strong>{{ selectedDepth.reservedQty }}</strong>
        </div>

        <div class="outbound-location-2d-detail__info-item">
          <span>Lot No</span>
          <strong>{{ selectedDepth.lotNo || '-' }}</strong>
        </div>

        <div class="outbound-location-2d-detail__info-item">
          <span>Status</span>
          <strong>{{ selectedDepth.stockStatusCode || '-' }}</strong>
        </div>
      </div>

      <div class="outbound-location-2d-detail__execute">
        <AsrsFormField
          v-model="executeForm.refDocType"
          label="Ref Type"
          type="text"
          label-mode="left"
          :compact="true"
          placeholder="ORDER"
        />

        <AsrsFormField
          v-model="executeForm.refDocNo"
          label="Ref No"
          type="text"
          label-mode="left"
          :compact="true"
          placeholder="문서번호"
        />

        <AsrsFormField
          v-model="executeForm.refLineNo"
          label="Ref Line"
          type="text"
          label-mode="left"
          :compact="true"
          placeholder="라인번호"
        />

        <AsrsFormField
          v-model="executeForm.reasonCode"
          label="Reason"
          type="text"
          label-mode="left"
          :compact="true"
          placeholder="사유코드"
        />

        <AsrsFormField
          v-model="executeForm.remark"
          label="Remark"
          type="text"
          label-mode="left"
          :compact="true"
          placeholder="비고"
        />

        <div class="outbound-location-2d-detail__execute-actions">
          <AsrsActionButton
            variant="primary"
            :disabled="!canExecute"
            :loading="loadingExecute"
            loading-text="출고 중..."
            @click="$emit('execute')"
          >
            지정출고 실행
          </AsrsActionButton>
        </div>
      </div>
    </div>

    <div v-else class="outbound-location-2d-detail__empty">
      랙 셀을 선택하면 상세 정보가 표시됩니다.
    </div>
  </section>
</template>

<script setup lang="ts">
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
import OutboundDepthTabs from './OutboundDepthTabs.vue';
import type {
  OutboundLocation2DCell,
  OutboundLocation2DDepth,
  OutboundLocation2DExecuteForm,
} from '../types';

defineProps<{
  selectedCell: OutboundLocation2DCell | null;
  selectedDepth: OutboundLocation2DDepth | null;
  selectedDepthNo: number;
  executeForm: OutboundLocation2DExecuteForm;
  canExecute: boolean;
  loadingExecute?: boolean;
}>();

defineEmits<{
  (e: 'update:selected-depth-no', value: number): void;
  (e: 'execute'): void;
}>();
</script>
