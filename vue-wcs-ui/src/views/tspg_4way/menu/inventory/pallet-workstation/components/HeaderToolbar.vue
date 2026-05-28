<template>
  <div class="header-toolbar">
    <div class="tb-actions">
      <button
        v-if="canUpdate"
        type="button"
        class="tb-btn"
        :class="{ 'tb-btn-loading': printing.pallet }"
        :disabled="!info || printing.pallet"
        @click="$emit('run-action', 'printPallet')"
      >
        <span v-if="printing.pallet" class="tb-spinner" aria-hidden="true"></span>
        {{ printing.pallet ? '인쇄 중' : '파렛트' }}
      </button>
      <button
        v-if="canUpdate"
        type="button"
        class="tb-btn"
        :class="{ 'tb-btn-loading': printing.boxes }"
        :disabled="!info || printing.boxes"
        @click="$emit('run-action', 'printAllBoxes')"
      >
        <span v-if="printing.boxes" class="tb-spinner" aria-hidden="true"></span>
        {{ printing.boxes ? '인쇄 중' : '박스 전체' }}
      </button>
      <button
        v-if="canUpdate && selectedCount > 0"
        type="button"
        class="tb-btn tb-btn-selected"
        :class="{ 'tb-btn-loading': printing.selected }"
        :disabled="printing.selected"
        @click="$emit('run-action', 'printSelectedBoxes')"
      >
        <span v-if="printing.selected" class="tb-spinner" aria-hidden="true"></span>
        {{ printing.selected ? '인쇄 중' : `선택 ${selectedCount}` }}
      </button>
      <!-- 출력 완료 버튼 제거 — `박스 전체 라벨 인쇄` 후 인쇄창 닫으면 자동으로 markPalletPrinted 호출됨.
           실패 케이스는 박스 행의 단건 [라벨] 또는 `박스 전체` 재클릭으로 복구.
           박스 추가/선택 삭제는 박스 목록 상단 액션 바로 이동. -->
    </div>

    <button
      type="button"
      class="tb-kebab"
      :class="{ open: menuOpen }"
      :disabled="!info"
      @click="toggleMenu"
    >
      <span class="kebab-dot"></span>
      <span class="kebab-dot"></span>
      <span class="kebab-dot"></span>
    </button>

    <div v-if="menuOpen" class="tb-menu-mask" @click="menuOpen = false">
      <div class="tb-menu" @click.stop>
        <button class="tb-menu-item" @click="runAndClose('autoReleaseToggle')">
          <span>자동 release</span>
          <span class="tb-switch" :class="{ on: autoRelease }">
            <span class="tb-switch-knob"></span>
          </span>
        </button>
        <button class="tb-menu-item" @click="runAndClose('reset')">초기화</button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import type { ActionId } from '../scenarios';

defineProps<{
  info: any;
  canUpdate: boolean;
  selectedCount: number;
  autoRelease: boolean;
  printing: { pallet: boolean; boxes: boolean; mark: boolean; selected: boolean };
}>();

const emit = defineEmits<{
  (e: 'run-action', id: ActionId): void;
}>();

const menuOpen = ref(false);

function toggleMenu() {
  menuOpen.value = !menuOpen.value;
}
function runAndClose(id: ActionId) {
  emit('run-action', id);
  menuOpen.value = false;
}
</script>

<style scoped>
.header-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}
.tb-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.tb-btn {
  background: var(--c-card, #ffffff);
  border: 1px solid var(--c-border, #e8eaed);
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 11px;
  font-weight: 600;
  color: var(--c-text-2, #475569);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.tb-btn:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: var(--c-text, #0f172a);
}
.tb-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.tb-btn-loading {
  opacity: 0.85;
  cursor: progress;
  color: var(--c-primary, #3182f6);
  border-color: #93c5fd;
  background: #eff6ff;
}
.tb-spinner {
  display: inline-block;
  width: 11px;
  height: 11px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: tb-spin 0.6s linear infinite;
  margin-right: 5px;
  vertical-align: -1px;
}
@keyframes tb-spin {
  to {
    transform: rotate(360deg);
  }
}
.tb-btn-selected {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #1e40af;
}
.tb-btn-selected:hover:not(:disabled) {
  background: #bfdbfe;
  border-color: #60a5fa;
}

/* Kebab */
.tb-kebab {
  background: transparent;
  border: 1px solid var(--c-border, #e8eaed);
  border-radius: 6px;
  width: 28px;
  height: 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  cursor: pointer;
  transition: all 0.15s;
}
.tb-kebab:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #cbd5e1;
}
.tb-kebab:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.tb-kebab.open {
  background: #f1f5f9;
  border-color: var(--c-primary, #3182f6);
}
.kebab-dot {
  width: 3px;
  height: 3px;
  background: var(--c-text-2, #475569);
  border-radius: 50%;
}

/* Menu */
.tb-menu-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
}
.tb-menu {
  position: absolute;
  top: 36px;
  right: 0;
  min-width: 200px;
  background: var(--c-card, #ffffff);
  border: 1px solid var(--c-border, #e8eaed);
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.12);
  padding: 4px;
  z-index: 1001;
}
.tb-menu-item {
  width: 100%;
  background: transparent;
  border: 0;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text, #0f172a);
  text-align: left;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.tb-menu-item:hover {
  background: #f1f5f9;
}

/* autoRelease switch (작은 버전) */
.tb-switch {
  position: relative;
  width: 26px;
  height: 14px;
  background: #cbd5e1;
  border-radius: 999px;
  transition: background 0.2s;
  display: inline-block;
  flex-shrink: 0;
}
.tb-switch.on {
  background: var(--c-primary, #3182f6);
}
.tb-switch-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 10px;
  height: 10px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s;
}
.tb-switch.on .tb-switch-knob {
  transform: translateX(12px);
}
</style>
