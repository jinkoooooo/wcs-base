<template>
  <teleport to="body">
    <transition name="bm-fade">
      <div v-if="open" class="bm-mask" @click.self="onClose">
        <div
          class="bm-modal"
          :class="modalClass"
          :style="`max-width: ${width}px`"
          role="dialog"
          aria-modal="true"
          @click.stop
        >
          <slot name="header">
            <header class="bm-header" :class="headerClass">
              <div class="bm-header-left">
                <slot name="header-icon"></slot>
                <span class="bm-title">{{ title }}</span>
              </div>
              <button
                type="button"
                class="bm-close"
                :disabled="busy"
                aria-label="닫기"
                @click="onClose"
              >
                <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                  <path
                    d="M6 6 L18 18 M18 6 L6 18"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.2"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
            </header>
          </slot>
          <div class="bm-body"><slot /></div>
          <slot name="footer">
            <footer v-if="showFooter" class="bm-footer">
              <button
                type="button"
                class="bm-btn bm-btn-cancel"
                :disabled="busy"
                @click="onClose"
              >
                취소
              </button>
              <button
                type="button"
                class="bm-btn bm-btn-confirm"
                :class="confirmClass"
                :disabled="busy || confirmDisabled"
                @click="$emit('confirm')"
              >
                <span v-if="busy" class="bm-spinner" aria-hidden="true"></span>
                {{ busy ? '처리 중...' : confirmLabel }}
              </button>
            </footer>
          </slot>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script lang="ts" setup>
const props = withDefaults(
  defineProps<{
    open: boolean;
    busy?: boolean;
    title?: string;
    confirmLabel?: string;
    width?: number;
    headerClass?: string;
    confirmClass?: string;
    modalClass?: string;
    showFooter?: boolean;
    confirmDisabled?: boolean;
  }>(),
  {
    busy: false,
    title: '',
    confirmLabel: '확인',
    width: 480,
    headerClass: '',
    confirmClass: '',
    modalClass: '',
    showFooter: true,
    confirmDisabled: false,
  },
);

const emit = defineEmits<{
  (e: 'confirm'): void;
  (e: 'close'): void;
}>();

function onClose() {
  if (props.busy) return;
  emit('close');
}
</script>

<style scoped>
.bm-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1100;
  padding: 16px;
}
.bm-modal {
  width: 100%;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.35), 0 10px 20px -8px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 32px);
  font-family: 'Pretendard Variable', Pretendard, sans-serif;
}

.bm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  color: #fff;
  flex-shrink: 0;
}
.bm-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bm-title {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.2px;
}
.bm-close {
  border: none;
  background: transparent;
  color: inherit;
  width: 32px;
  height: 32px;
  flex: none;
  padding: 0;
  cursor: pointer;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.bm-close svg {
  display: block;
}
.bm-close:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.18);
}
.bm-close:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 헤더 테마 — teleport 로 body 로 옮겨진 헤더엔 부모의 :deep 가 안 닿으므로 BaseModal 이 직접 소유 */
.bm-header.header-add-box {
  background: linear-gradient(135deg, #16a34a 0%, #15803d 100%);
}
.bm-header.header-label-reissue {
  background: linear-gradient(135deg, #6366f1 0%, #4338ca 100%);
}
.bm-header.header-adjust {
  background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
}
.bm-header.header-sample {
  background: linear-gradient(135deg, #ec4899 0%, #be185d 100%);
}
.bm-header.header-partial {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}
.bm-header.header-edit-total {
  background: linear-gradient(135deg, #0ea5e9 0%, #0369a1 100%);
}
.bm-header.ra-header {
  background: linear-gradient(135deg, #ef4444 0%, #b91c1c 100%);
}

/* 흰 배경 헤더 — 글자·닫기 버튼을 어둡게 (history, finalize) */
.bm-header.phm-header,
.bm-header.pfm-header-plain {
  background: #fff;
  color: #0f172a;
  border-bottom: 1px solid #e8eaed;
}
.bm-header.phm-header .bm-title {
  font-size: 14px;
  font-weight: 600;
}
.bm-header.pfm-header-plain .bm-title {
  font-size: 16px;
  font-weight: 800;
}
.bm-header.phm-header .bm-close,
.bm-header.pfm-header-plain .bm-close {
  color: #94a3b8;
}
.bm-header.phm-header .bm-close:hover:not(:disabled),
.bm-header.pfm-header-plain .bm-close:hover:not(:disabled) {
  background: #f1f5f9;
  color: #0f172a;
}

.bm-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1 1 auto;
}

.bm-footer {
  display: flex;
  gap: 8px;
  padding: 14px 20px;
  border-top: 1px solid #e8eaed;
  flex-shrink: 0;
  background: #fafbfc;
}
.bm-btn {
  flex: 1;
  height: 44px;
  border: 0;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.bm-btn-cancel {
  background: #fff;
  border: 1px solid #e2e8f0;
  color: #475569;
}
.bm-btn-cancel:hover:not(:disabled) {
  background: #f1f5f9;
}
.bm-btn-confirm {
  background: #3182f6;
  color: #fff;
}
.bm-btn-confirm:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(49, 130, 246, 0.3);
}
.bm-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

/* 확인 버튼 테마 — 헤더와 동일 사유(teleport)로 BaseModal 이 소유. 헤더 색과 짝 */
.bm-btn.confirm-add-box {
  background: linear-gradient(135deg, #16a34a 0%, #15803d 100%);
}
.bm-btn.confirm-label-reissue {
  background: linear-gradient(135deg, #6366f1 0%, #4338ca 100%);
}
.bm-btn.confirm-adjust {
  background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
}
.bm-btn.confirm-sample {
  background: linear-gradient(135deg, #ec4899 0%, #be185d 100%);
}
.bm-btn.confirm-partial {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}
.bm-btn.confirm-edit-total {
  background: linear-gradient(135deg, #0ea5e9 0%, #0369a1 100%);
}
.bm-btn.pfm-confirm-danger {
  background: #ef4444;
}
.bm-btn.pfm-confirm-danger:hover:not(:disabled) {
  background: #dc2626;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.bm-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: bm-spin 0.6s linear infinite;
}
@keyframes bm-spin {
  to {
    transform: rotate(360deg);
  }
}

.bm-fade-enter-active,
.bm-fade-leave-active {
  transition: opacity 0.15s ease;
}
.bm-fade-enter-from,
.bm-fade-leave-to {
  opacity: 0;
}
</style>
