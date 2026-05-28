<template>
  <section class="outbound-side-panel">
    <div class="outbound-side-panel__header">
      <div>
        <p class="outbound-side-panel__eyebrow">DETAIL</p>
        <h3 class="outbound-side-panel__title">선택 재고 작업</h3>
      </div>
    </div>

    <div class="outbound-side-panel__body">
      <template v-if="selectedStock">
        <!-- 상단 요약: 카드 대신 칩형 정보 -->
        <div class="outbound-side-panel__info-strip">
          <div class="outbound-side-panel__info-chip outbound-side-panel__info-chip--wide">
            <span class="outbound-side-panel__info-label">Stock Unit No</span>
            <strong>{{ selectedStock.stockUnitNo }}</strong>
          </div>

          <div class="outbound-side-panel__info-chip">
            <span class="outbound-side-panel__info-label">Item Code</span>
            <strong>{{ selectedStock.itemCode || '-' }}</strong>
          </div>

          <div class="outbound-side-panel__info-chip outbound-side-panel__info-chip--wide">
            <span class="outbound-side-panel__info-label">Location</span>
            <strong>{{ selectedStock.locationCode || '-' }}</strong>
          </div>

          <div class="outbound-side-panel__info-chip">
            <span class="outbound-side-panel__info-label">Available</span>
            <strong>{{ selectedStock.availableQty }}</strong>
          </div>
        </div>

        <!-- 입력 + 버튼 압축 -->
        <div class="outbound-side-panel__action-box">
          <div class="outbound-side-panel__action-fields">
            <div class="asrs-ui-field outbound-side-panel__mini-field">
              <label class="asrs-ui-label">할당 수량</label>
              <input
                v-model.number="form.allocatedQty"
                class="asrs-ui-input"
                type="number"
                min="0"
              />
            </div>

            <div class="asrs-ui-field outbound-side-panel__mini-field">
              <label class="asrs-ui-label">부분출고 수량</label>
              <input
                v-model.number="form.outQty"
                class="asrs-ui-input"
                type="number"
                min="0"
              />
            </div>

            <div class="asrs-ui-field outbound-side-panel__mini-field">
              <label class="asrs-ui-label">Due Date</label>
              <input
                v-model="form.dueDate"
                class="asrs-ui-input"
                type="date"
              />
            </div>
          </div>

          <div class="outbound-side-panel__action-buttons">
            <AsrsActionButton
              variant="secondary"
              :loading="loading.allocate"
              loading-text="할당 중..."
              @click="$emit('allocate')"
            >
              할당
            </AsrsActionButton>

            <AsrsActionButton
              variant="secondary"
              :loading="loading.partialOut"
              loading-text="부분출고 중..."
              @click="$emit('partial-out')"
            >
              부분출고
            </AsrsActionButton>

            <AsrsActionButton
              variant="primary"
              :loading="loading.fullOut"
              loading-text="전체출고 중..."
              @click="$emit('full-out')"
            >
              전체출고
            </AsrsActionButton>
          </div>
        </div>

        <!-- 하단: 탭 전환 -->
        <div class="outbound-side-panel__content">
          <div class="outbound-side-panel__content-header">
            <div class="outbound-side-panel__tabs">
              <button
                type="button"
                class="outbound-side-panel__tab"
                :class="{ 'outbound-side-panel__tab--active': activeContentTab === 'allocations' }"
                @click="activeContentTab = 'allocations'"
              >
                활성 할당 목록
              </button>

              <button
                type="button"
                class="outbound-side-panel__tab"
                :class="{ 'outbound-side-panel__tab--active': activeContentTab === 'history' }"
                @click="activeContentTab = 'history'"
              >
                재고 트랜잭션 이력
              </button>
            </div>

            <div class="outbound-side-panel__content-actions">
              <AsrsActionButton
                v-if="activeContentTab === 'allocations'"
                variant="ghost"
                :disabled="!selectedAllocation"
                :loading="loading.release"
                loading-text="해제 중..."
                @click="$emit('release')"
              >
                할당 해제
              </AsrsActionButton>
            </div>
          </div>

          <div class="outbound-side-panel__content-body">
            <!-- 활성 할당 목록 -->
            <div
              v-show="activeContentTab === 'allocations'"
              class="outbound-side-panel__table-wrap outbound-side-panel__table-wrap--content"
            >
              <table class="asrs-ui-table">
                <thead>
                <tr>
                  <th>Ref Doc No</th>
                  <th>Qty</th>
                  <th>Status</th>
                  <th>Allocated At</th>
                </tr>
                </thead>
                <tbody>
                <tr v-if="loading.detail">
                  <td colspan="4" class="outbound-side-panel__empty">조회 중입니다...</td>
                </tr>

                <tr v-else-if="!allocationRows.length">
                  <td colspan="4" class="outbound-side-panel__empty">활성 할당이 없습니다.</td>
                </tr>

                <tr
                  v-for="row in allocationRows"
                  :key="`${row.refDocNo}-${row.refLineNo}-${row.allocatedAt}`"
                  :class="{ 'asrs-ui-table__row--selected': selectedAllocationKey === allocationKey(row) }"
                  @click="$emit('select-allocation', row)"
                >
                  <td>{{ row.refDocNo || '-' }}</td>
                  <td>{{ row.allocatedQty }}</td>
                  <td>{{ row.allocStatusCode || '-' }}</td>
                  <td>{{ row.allocatedAt || '-' }}</td>
                </tr>
                </tbody>
              </table>
            </div>

            <!-- 재고 트랜잭션 이력 -->
            <div
              v-show="activeContentTab === 'history'"
              class="outbound-side-panel__table-wrap outbound-side-panel__table-wrap--content"
            >
              <table class="asrs-ui-table">
                <thead>
                <tr>
                  <th>Txn Type</th>
                  <th>Qty</th>
                  <th>Ref Doc No</th>
                  <th>Txn At</th>
                </tr>
                </thead>
                <tbody>
                <tr v-if="loading.detail">
                  <td colspan="4" class="outbound-side-panel__empty">조회 중입니다...</td>
                </tr>

                <tr v-else-if="!historyRows.length">
                  <td colspan="4" class="outbound-side-panel__empty">이력이 없습니다.</td>
                </tr>

                <tr v-for="row in historyRows" :key="`${row.txnType}-${row.txnAt}-${row.refDocNo}`">
                  <td>{{ row.txnType || '-' }}</td>
                  <td>{{ row.qty }}</td>
                  <td>{{ row.refDocNo || '-' }}</td>
                  <td>{{ row.txnAt || '-' }}</td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="outbound-side-panel__empty-state">
        재고를 선택하면 할당/출고 작업과 이력이 표시됩니다.
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * 출고 작업 상세 패널.
 *
 * 구조 개선 포인트:
 * - 상단 큰 카드 -> 작은 정보 칩
 * - 입력/버튼 -> 한 줄 액션 박스
 * - 하단 테이블 2개 -> 탭 방식으로 1개씩 크게 표시
 */
import { ref } from 'vue';
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import type {
  OutboundAllocationRow,
  OutboundHistoryRow,
  OutboundStockRow,
  OutboundWorkForm,
} from '../types';

const props = defineProps<{
  form: OutboundWorkForm;
  selectedStock: OutboundStockRow | null;
  allocationRows: OutboundAllocationRow[];
  selectedAllocation: OutboundAllocationRow | null;
  historyRows: OutboundHistoryRow[];
  loading: {
    search: boolean;
    detail: boolean;
    allocate: boolean;
    partialOut: boolean;
    fullOut: boolean;
    release: boolean;
  };
}>();

defineEmits<{
  (e: 'allocate'): void;
  (e: 'partial-out'): void;
  (e: 'full-out'): void;
  (e: 'release'): void;
  (e: 'select-allocation', row: OutboundAllocationRow): void;
}>();

/** 우측 하단 컨텐츠 탭 */
const activeContentTab = ref<'allocations' | 'history'>('history');

function allocationKey(row: OutboundAllocationRow) {
  return `${row.refDocNo || ''}|${row.refLineNo || ''}|${row.allocatedAt || ''}`;
}

const selectedAllocationKey = props.selectedAllocation
  ? allocationKey(props.selectedAllocation)
  : '';
</script>
