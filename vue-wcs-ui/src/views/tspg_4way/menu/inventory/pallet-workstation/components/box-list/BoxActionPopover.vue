<template>
  <teleport to="body">
    <div
      v-if="actionMenu.open"
      class="action-popover-mask"
      @click.self="$emit('close')"
      @contextmenu.prevent="$emit('close')"
    >
      <div
        class="action-popover"
        :style="{ top: actionMenu.top + 'px', left: actionMenu.left + 'px' }"
        @click.stop
      >
        <button
          v-if="
            actionMenu.target &&
            step.boxMenuActions.includes('editTotal') &&
            canEditTotal(actionMenu.target) &&
            !actionMenu.target._draftDeleted
          "
          type="button"
          class="action-popover-item"
          @click="$emit('run-menu', 'editTotal')"
        >
          <span class="ap-icon">✏</span>
          {{ actionMenu.target._draftEditTotal != null ? '수정 변경' : '수량 수정' }}
        </button>
        <button
          v-if="
            actionMenu.target &&
            step.boxMenuActions.includes('cancelEdit') &&
            !actionMenu.target._isAddition &&
            canEditTotal(actionMenu.target) &&
            actionMenu.target._draftEditTotal != null
          "
          type="button"
          class="action-popover-item"
          @click="$emit('run-menu', 'cancelEdit')"
        >
          <span class="ap-icon">↺</span>
          수정 취소
        </button>
        <button
          v-if="
            actionMenu.target &&
            step.boxMenuActions.includes('void') &&
            canEditTotal(actionMenu.target) &&
            canAddOrDeleteBox
          "
          type="button"
          class="action-popover-item"
          :class="{ danger: !actionMenu.target._draftDeleted && !actionMenu.target._isAddition }"
          @click="$emit('run-menu', 'void')"
        >
          <span class="ap-icon">🗑</span>
          {{
            actionMenu.target._isAddition
              ? '추가 취소'
              : actionMenu.target._draftDeleted
              ? '삭제 취소'
              : '삭제'
          }}
        </button>
        <button
          v-if="
            actionMenu.target &&
            step.boxMenuActions.includes('sample') &&
            !actionMenu.target._isAddition &&
            canEditQty(actionMenu.target)
          "
          type="button"
          class="action-popover-item"
          :disabled="hasUnsavedChanges"
          :title="hasUnsavedChanges ? '미저장 변경사항이 있어 비활성화됨' : ''"
          @click="$emit('run-menu', 'sample')"
        >
          <span class="ap-icon">⚗</span>
          채취
        </button>
        <button
          v-if="
            actionMenu.target &&
            step.boxMenuActions.includes('partial') &&
            !actionMenu.target._isAddition &&
            canEditQty(actionMenu.target)
          "
          type="button"
          class="action-popover-item"
          :disabled="hasUnsavedChanges"
          :title="hasUnsavedChanges ? '미저장 변경사항이 있어 비활성화됨' : ''"
          @click="$emit('run-menu', 'partial')"
        >
          <span class="ap-icon">⇣</span>
          부분 출고
        </button>
        <button
          v-if="
            actionMenu.target &&
            step.boxMenuActions.includes('printBoxLabel') &&
            !actionMenu.target._isAddition
          "
          type="button"
          class="action-popover-item"
          :disabled="hasUnsavedChanges || actionMenu.target.box_seq == null"
          :title="
            hasUnsavedChanges
              ? '미저장 변경사항이 있어 비활성화됨'
              : actionMenu.target.box_seq == null
              ? '박스 확정 후 인쇄 가능'
              : ''
          "
          @click="$emit('run-menu', 'printBoxLabel')"
        >
          <span class="ap-icon">🏷</span>
          라벨 인쇄
        </button>
      </div>
    </div>
  </teleport>
</template>

<script lang="ts" setup>
  import type { StepDescriptor } from '../../scenarios';

  defineProps<{
    actionMenu: { open: boolean; target: any; top: number; left: number };
    step: StepDescriptor;
    canEditQty: (b: any) => boolean;
    canEditTotal: (b: any) => boolean;
    canAddOrDeleteBox: boolean;
    hasUnsavedChanges: boolean;
  }>();

  defineEmits<{
    (e: 'close'): void;
    (e: 'run-menu', action: string): void;
  }>();
</script>

<style>
  /* 전역 — teleport */
  .action-popover-mask {
    position: fixed;
    inset: 0;
    z-index: 1050;
    background: transparent;
  }
  .action-popover {
    position: fixed;
    min-width: 160px;
    max-width: 220px;
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 4px;
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.18), 0 2px 6px rgba(15, 23, 42, 0.08);
    z-index: 1051;
    animation: ap-pop-in 0.12s ease-out;
  }
  @keyframes ap-pop-in {
    from {
      opacity: 0;
      transform: translateY(-4px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  .action-popover-item {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 8px 12px;
    background: transparent;
    border: none;
    border-radius: 6px;
    font: inherit;
    font-size: 13px;
    color: #0f172a;
    cursor: pointer;
    text-align: left;
    transition: background 0.1s;
  }
  .action-popover-item:hover:not(:disabled) {
    background: #f1f5f9;
  }
  .action-popover-item:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
  .action-popover-item.danger {
    color: #dc2626;
  }
  .action-popover-item.danger:hover:not(:disabled) {
    background: #fef2f2;
  }
  .ap-icon {
    width: 18px;
    text-align: center;
    color: #64748b;
    font-size: 13px;
  }
  .action-popover-item.danger .ap-icon {
    color: #dc2626;
  }
</style>
