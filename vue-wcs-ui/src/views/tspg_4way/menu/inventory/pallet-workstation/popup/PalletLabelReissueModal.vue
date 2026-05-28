<template>
  <BaseModal
    :open="open"
    :busy="busy"
    :title="labelReissueTitle"
    :confirm-label="busy ? '인쇄 중...' : '인쇄'"
    :confirm-disabled="!!labelReissueValidationError"
    header-class="header-label-reissue"
    confirm-class="confirm-label-reissue"
    @confirm="$emit('confirm')"
    @close="$emit('close')"
  >
    <template #header-icon><span class="qty-icon">🏷</span></template>

    <!-- 첫 발행 안내 -->
    <div v-if="!labelReissueRequireComment" class="first-issue-notice">
      <span class="first-issue-icon">✓</span>
      <div>
        <div class="first-issue-title">첫 발행</div>
        <div class="first-issue-msg">
          <template v-if="target?.kind === 'multi'">
            선택한 {{ target.boxIds.length }}매 모두 첫 발행 대상입니다.
            사유 입력 없이 발행됩니다.
          </template>
          <template v-else>
            대기 상태(미인쇄) 박스입니다. 사유 입력 없이 발행됩니다.
          </template>
        </div>
      </div>
    </div>

    <!-- 단건 박스 정보 -->
    <div v-if="target?.kind === 'single'" class="box-info-card">
      <div class="box-info-row">
        <span class="box-info-label">박스 바코드</span>
        <span class="box-info-value">{{ target.boxBarcode }}</span>
      </div>
    </div>

    <!-- 사유 입력란 (재발행 케이스에만 노출) -->
    <template v-if="labelReissueRequireComment">
      <div class="qty-input-section">
        <label class="qty-input-label">
          재발행 사유 <span class="required-mark">*</span>
        </label>
        <textarea
          ref="labelReissueCommentRef"
          :value="comment"
          class="reissue-comment-input"
          placeholder="예시) 라벨 훼손으로 재출력 / 박스 분실로 재발행 / 검수 후 재인쇄 등"
          rows="3"
          maxlength="500"
          :disabled="busy"
          @input="$emit('update:comment', ($event.target as HTMLTextAreaElement).value)"
        ></textarea>
        <div class="reissue-char-count">{{ (comment ?? '').length }} / 500</div>
      </div>
      <div v-if="labelReissueValidationError" class="qty-error">
        ⚠ {{ labelReissueValidationError }}
      </div>
      <div class="qty-hint">
        감사 로그(LABEL_REPRINT)에 사유가 기록됩니다. (2~500자)
        <template v-if="target?.kind === 'multi'">
          <br />
          <strong>
            선택한 {{ target.boxIds.length }}개 박스에 동일 사유로 일괄 적용됩니다.
          </strong>
        </template>
      </div>
    </template>
  </BaseModal>
</template>

<script lang="ts" setup>
  import { computed, nextTick, ref, watch } from 'vue';
  import BaseModal from './BaseModal.vue';
  import type { LabelReissueTarget } from '../shared';

  const props = defineProps<{
    open: boolean;
    target: LabelReissueTarget | null;
    comment: string;
    busy: boolean;
  }>();

  defineEmits<{
    (e: 'update:comment', v: string): void;
    (e: 'confirm'): void;
    (e: 'close'): void;
  }>();

  const labelReissueCommentRef = ref<HTMLTextAreaElement | null>(null);

  watch(
    () => props.open,
    (v) => {
      if (v) nextTick(() => labelReissueCommentRef.value?.focus?.());
    },
  );

  const labelReissueRequireComment = computed(() => {
    const t = props.target;
    if (!t) return true;
    if (t.kind === 'single') return !t.isFirstIssue;
    return !t.allFirstIssue;
  });

  const labelReissueValidationError = computed(() => {
    if (!labelReissueRequireComment.value) return '';
    const c = (props.comment ?? '').trim();
    if (c.length < 2) return '재발행 사유는 2자 이상 입력해주세요.';
    if (c.length > 500) return '재발행 사유는 500자를 초과할 수 없습니다.';
    return '';
  });

  const labelReissueTitle = computed(() => {
    const t = props.target;
    if (!t) return '';
    const verb = labelReissueRequireComment.value ? '재발행' : '발행';
    if (t.kind === 'single') return `박스 라벨 ${verb} (${t.boxBarcode})`;
    return `박스 라벨 일괄 ${verb} (${t.boxIds.length}매)`;
  });
</script>

<!-- 폼 위젯 공통 스타일 -->
<style src="./palletModalShared.css"></style>

<style scoped>
  .qty-icon {
    font-size: 16px;
    width: 28px;
    height: 28px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.22);
    border-radius: 50%;
  }
  .required-mark {
    color: #fef3c7;
    font-weight: 700;
    margin-left: 2px;
  }
  .first-issue-notice {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 12px 14px;
    margin-bottom: 16px;
    background: #f0fdf4;
    border: 1px solid #bbf7d0;
    border-left: 3px solid #16a34a;
    border-radius: 8px;
  }
  .first-issue-icon {
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: #16a34a;
    color: #fff;
    border-radius: 50%;
    font-weight: 800;
    font-size: 13px;
  }
  .first-issue-title {
    font-size: 13px;
    font-weight: 700;
    color: #15803d;
    margin-bottom: 2px;
  }
  .first-issue-msg {
    font-size: 12px;
    color: #166534;
    line-height: 1.5;
  }

  .reissue-comment-input {
    width: 100%;
    min-height: 90px;
    padding: 10px 12px;
    border: 2px solid #e2e8f0;
    border-radius: 10px;
    font: inherit;
    font-size: 14px;
    color: #0f172a;
    background: #fff;
    outline: none;
    resize: vertical;
    transition: border-color 0.15s, box-shadow 0.15s;
    font-family: inherit;
    line-height: 1.5;
  }
  .reissue-comment-input::placeholder {
    color: #94a3b8;
  }
  .reissue-comment-input:focus {
    border-color: #6366f1;
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.18);
  }
  .reissue-comment-input:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    background: #f8fafc;
  }
  .reissue-char-count {
    text-align: right;
    font-size: 11px;
    color: #94a3b8;
    margin-top: 4px;
    font-variant-numeric: tabular-nums;
  }
</style>
