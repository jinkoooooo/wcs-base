<!--
  ActionCommentModal — Dashboard2D 재고 액션 공용 모달.
  운영자가 반품/폐기/국검 승인·미승인/국검 대기/수동 출고를 실행할 때,
  사유(comment)를 필수로 입력받아 부모로 emit('confirm', payload).
  NIA_PENDING 전환 시에는 showTestRequestNo=true 로 의뢰번호 input도 노출.
-->
<template>
  <teleport to="body">
    <transition name="acm-modal">
      <div v-if="visible" class="acm-mask" @click.self="onCancel">
        <div class="acm-dialog" role="dialog" aria-modal="true">
          <div class="acm-header">
            <span class="acm-title">{{ title }}</span>
            <button type="button" class="acm-close" @click="onCancel">×</button>
          </div>

          <div class="acm-body">
            <div v-if="stockInfo?.stockId" class="acm-info-row">
              <span class="acm-label">재고</span>
              <span class="acm-value mono">{{ stockInfo.stockId }}</span>
            </div>
            <div v-if="stockInfo?.sku || stockInfo?.lot" class="acm-info-row">
              <span class="acm-label">SKU / Lot</span>
              <span class="acm-value">{{ stockInfo.sku || '-' }} / {{ stockInfo.lot || '-' }}</span>
            </div>
            <div v-if="currentType" class="acm-info-row">
              <span class="acm-label">현재 카테고리</span>
              <span class="acm-value">{{ currentType }}</span>
            </div>
            <div v-if="nextType" class="acm-info-row">
              <span class="acm-label">변경 후</span>
              <span class="acm-value" :class="`next-${confirmVariant}`">{{ nextType }}</span>
            </div>

            <div v-if="extraNotice" class="acm-notice">{{ extraNotice }}</div>

            <!-- 출고 포트 (수동 출고일 때) -->
            <div v-if="showPortCode" class="acm-input-section">
              <label class="acm-input-label">출고 포트 (선택)</label>
              <input
                v-model="portCodeLocal"
                type="text"
                class="acm-text-input"
                placeholder="비워두면 자동 배정"
              />
            </div>

            <!-- 시험 의뢰 번호 (NIA_PENDING 전환일 때) -->
            <div v-if="showTestRequestNo" class="acm-input-section">
              <label class="acm-input-label">시험 의뢰 번호 (선택)</label>
              <input
                v-model="testRequestNoLocal"
                type="text"
                class="acm-text-input mono"
                placeholder="예: QC-20260521-001"
              />
              <div class="acm-hint">비워두면 audit 에 (미입력) 으로 기록</div>
            </div>

            <!-- 보정 수량 (수량 보정 액션일 때) -->
            <div v-if="showQty" class="acm-input-section">
              <label class="acm-input-label">보정 수량 (EA) <span class="req">*</span></label>
              <input
                v-model.number="qtyLocal"
                type="number"
                min="0"
                class="acm-text-input"
                placeholder="실제 수량 (0 = 비활성)"
              />
              <div class="acm-hint">0 입력 시 해당 재고가 비활성 처리됩니다</div>
            </div>

            <!-- 사유 (필수) -->
            <div class="acm-input-section">
              <label class="acm-input-label">사유 (comment) <span class="req">*</span></label>
              <textarea
                v-model="commentLocal"
                class="acm-textarea"
                :placeholder="`최소 ${MIN}자, 최대 ${MAX}자`"
                rows="3"
                :maxlength="MAX"
              ></textarea>
              <div class="acm-hint" :class="{ 'acm-hint-error': commentError }">
                {{ commentHint }}
              </div>
            </div>
          </div>

          <div class="acm-footer">
            <button type="button" class="acm-btn acm-btn-cancel" :disabled="busy" @click="onCancel">
              취소
            </button>
            <button
              type="button"
              class="acm-btn"
              :class="`acm-btn-${confirmVariant}`"
              :disabled="busy || !canConfirm"
              @click="onConfirm"
            >
              <span v-if="busy">처리 중...</span>
              <span v-else>{{ confirmLabel }}</span>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';

  const MIN = 2;
  const MAX = 500;

  type Variant = 'primary' | 'warn' | 'danger' | 'success';

  interface StockInfo {
    stockId?: string;
    sku?: string;
    lot?: string;
  }

  const props = withDefaults(
    defineProps<{
      visible: boolean;
      title: string;
      stockInfo?: StockInfo;
      currentType?: string;
      nextType?: string;
      confirmLabel: string;
      confirmVariant?: Variant;
      busy?: boolean;
      showTestRequestNo?: boolean;
      showPortCode?: boolean;
      showQty?: boolean;
      qtyDefault?: number;
      extraNotice?: string;
    }>(),
    {
      confirmVariant: 'primary',
      busy: false,
      showTestRequestNo: false,
      showPortCode: false,
      showQty: false,
      qtyDefault: 0,
    },
  );

  const emit = defineEmits<{
    (e: 'update:visible', v: boolean): void;
    (e: 'cancel'): void;
    (
      e: 'confirm',
      payload: { comment: string; testRequestNo?: string; portCode?: string; qty?: number },
    ): void;
  }>();

  const commentLocal = ref('');
  const testRequestNoLocal = ref('');
  const portCodeLocal = ref('');
  const qtyLocal = ref<number>(0);

  // 열릴 때 input 초기화
  watch(
    () => props.visible,
    (v) => {
      if (v) {
        commentLocal.value = '';
        testRequestNoLocal.value = '';
        portCodeLocal.value = '';
        qtyLocal.value = props.qtyDefault ?? 0;
      }
    },
  );

  const commentError = computed(() => {
    const t = commentLocal.value.trim();
    if (t.length === 0) return false; // 빈 상태는 hint만, 에러 아님
    return t.length < MIN || t.length > MAX;
  });

  const commentHint = computed(() => {
    const t = commentLocal.value.trim();
    if (t.length === 0) return `최소 ${MIN}자 이상 입력하세요`;
    if (t.length < MIN) return `최소 ${MIN}자 이상 입력하세요 (현재 ${t.length}자)`;
    if (t.length > MAX) return `${MAX}자 이하로 입력하세요`;
    return `${t.length}자 / ${MAX}자`;
  });

  const canConfirm = computed(() => {
    const t = commentLocal.value.trim();
    const commentOk = t.length >= MIN && t.length <= MAX;
    // 수량 보정 모드면 0 이상 정수 수량도 필수
    const qtyOk = !props.showQty || (qtyLocal.value != null && qtyLocal.value >= 0);
    return commentOk && qtyOk;
  });

  function onCancel() {
    if (props.busy) return;
    emit('cancel');
    emit('update:visible', false);
  }

  function onConfirm() {
    if (!canConfirm.value || props.busy) return;
    const payload: { comment: string; testRequestNo?: string; portCode?: string; qty?: number } = {
      comment: commentLocal.value.trim(),
    };
    if (props.showTestRequestNo) payload.testRequestNo = testRequestNoLocal.value.trim();
    if (props.showPortCode) payload.portCode = portCodeLocal.value.trim();
    if (props.showQty) payload.qty = Number(qtyLocal.value);
    emit('confirm', payload);
  }
</script>

<style scoped>
  .acm-mask {
    position: fixed;
    inset: 0;
    z-index: 2000;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(2px);
  }

  .acm-dialog {
    width: 420px;
    max-width: calc(100vw - 32px);
    background: rgba(30, 34, 45, 0.98);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 12px;
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5);
    color: #e5eaf3;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .acm-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 16px;
    background: rgba(255, 255, 255, 0.05);
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }
  .acm-title {
    font-size: 14px;
    font-weight: 600;
    color: #f2f5fb;
  }
  .acm-close {
    background: none;
    border: none;
    color: #909399;
    font-size: 20px;
    cursor: pointer;
    line-height: 1;
    padding: 0;
  }
  .acm-close:hover {
    color: #c0c4cc;
  }

  .acm-body {
    padding: 14px 16px;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .acm-info-row {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    line-height: 1.5;
  }
  .acm-label {
    color: #909399;
  }
  .acm-value {
    color: #e5eaf3;
    font-weight: 500;
    text-align: right;
    word-break: break-word;
  }
  .acm-value.mono {
    font-family: 'Roboto Mono', 'Consolas', monospace;
    font-size: 11px;
  }
  .acm-value.next-primary {
    color: #409eff;
  }
  .acm-value.next-warn {
    color: #e6a23c;
  }
  .acm-value.next-danger {
    color: #f56c6c;
  }
  .acm-value.next-success {
    color: #67c23a;
  }

  .acm-notice {
    background: rgba(64, 158, 255, 0.08);
    border: 1px solid rgba(64, 158, 255, 0.2);
    border-radius: 6px;
    padding: 8px 10px;
    font-size: 11px;
    color: #cfd8e6;
    line-height: 1.5;
  }

  .acm-input-section {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-top: 4px;
  }
  .acm-input-label {
    font-size: 11px;
    color: #909399;
  }
  .acm-input-label .req {
    color: #f56c6c;
  }
  .acm-text-input,
  .acm-textarea {
    width: 100%;
    padding: 8px 10px;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 6px;
    color: #e5eaf3;
    font-size: 12px;
    box-sizing: border-box;
    resize: vertical;
    font-family: inherit;
  }
  .acm-text-input.mono {
    font-family: 'Roboto Mono', 'Consolas', monospace;
    letter-spacing: 0.3px;
  }
  .acm-text-input::placeholder,
  .acm-textarea::placeholder {
    color: #606266;
  }
  .acm-text-input:focus,
  .acm-textarea:focus {
    outline: none;
    border-color: rgba(64, 158, 255, 0.5);
  }
  .acm-hint {
    font-size: 10px;
    color: #606266;
  }
  .acm-hint-error {
    color: #f56c6c;
  }

  .acm-footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding: 12px 16px;
    background: rgba(0, 0, 0, 0.15);
    border-top: 1px solid rgba(255, 255, 255, 0.06);
  }

  .acm-btn {
    padding: 6px 14px;
    border: none;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    transition: filter 0.15s;
  }
  .acm-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .acm-btn:not(:disabled):hover {
    filter: brightness(1.12);
  }

  .acm-btn-cancel {
    background: rgba(255, 255, 255, 0.08);
    color: #cfd8e6;
  }
  .acm-btn-primary {
    background: #3182f6;
    color: #fff;
  }
  .acm-btn-warn {
    background: rgba(245, 158, 11, 0.7);
    color: #fff;
  }
  .acm-btn-danger {
    background: rgba(239, 68, 68, 0.7);
    color: #fff;
  }
  .acm-btn-success {
    background: #16a34a;
    color: #fff;
  }

  .acm-modal-enter-active,
  .acm-modal-leave-active {
    transition: opacity 0.18s ease;
  }
  .acm-modal-enter-from,
  .acm-modal-leave-to {
    opacity: 0;
  }
</style>
