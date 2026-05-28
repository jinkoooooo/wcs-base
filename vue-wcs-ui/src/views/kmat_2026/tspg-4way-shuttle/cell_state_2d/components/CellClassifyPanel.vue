<!--
  CellClassifyPanel.vue
  셀 분류·제약 일괄 편집 패널 (우측 슬라이드) — v4

  ============================================
  v4 변경점 — scoped 적용 보장
  ============================================
  v3 까지 defineComponent + h() 로 만든 인라인 컴포넌트(FieldEditor)는
  부모의 scoped data-attribute([data-v-xxx]) 를 상속받지 못해
  scoped CSS 가 input 에 아예 매칭되지 않는 문제가 있었음.
  → FieldEditor 인라인 컴포넌트를 제거하고 템플릿 안에서 직접 v-for 로 렌더링.
  → 이제 scoped 가 정상 작동하므로 !important 도 최소화 가능.
-->

<template>
  <transition name="slide-fade">
    <aside
      v-if="visible"
      class="classify-panel"
      @click.stop
      @wheel.stop
      @mousedown.stop
      @contextmenu.stop
    >
      <!-- 헤더 -->
      <div class="panel-header">
        <div class="header-left">
          <h3>셀 분류 / 제약</h3>
          <span class="selection-badge">{{ selectedCells.length }}개 선택</span>
        </div>
        <button class="close-btn" @click="$emit('close')" aria-label="닫기">&times;</button>
      </div>

      <!-- 본문 -->
      <div class="panel-content">
        <div v-if="selectedCells.length === 0" class="empty-hint"> 셀을 1개 이상 선택하세요. </div>

        <template v-else>
          <div class="usage-hint">
            <span class="hint-icon">💡</span>
            <span>값 입력 = 변경 · 빈칸 = 유지 · <b>⊘</b> = 비움</span>
          </div>

          <!-- 4 필드 — 템플릿 내 직접 렌더링 (scoped 적용 보장) -->
          <div
            v-for="f in fieldDefs"
            :key="f.key"
            :class="['field-row', `mode-${editForm[f.key].mode}`]"
          >
            <!-- 1행: 라벨 + 상태배지 -->
            <div class="field-row-head">
              <span class="field-label">{{ f.label }}</span>
              <span v-if="optionsLoading && f.inputType === 'combo'" class="field-loading"
                >...</span
              >

              <span v-if="editForm[f.key].mode === 'set'" class="status-badge status-set">
                →
                {{
                  f.inputType === 'number'
                    ? formatNumber(editForm[f.key].value) + (f.unit ? ' ' + f.unit : '')
                    : editForm[f.key].value
                }}
              </span>
              <span v-else-if="editForm[f.key].mode === 'clear'" class="status-badge status-clear">
                → ⊘ {{ f.clearHint }}
              </span>
            </div>

            <!-- 2행: [현재값] → [입력] [⊘] -->
            <div class="field-row-body">
              <!-- 현재값 칩 -->
              <span
                v-if="!mergedCurrent[f.key] || mergedCurrent[f.key].kind === 'empty'"
                class="cur-box cur-empty"
                >—</span
              >

              <span
                v-else-if="mergedCurrent[f.key].kind === 'mixed'"
                class="cur-box cur-mixed"
                :title="mixedTooltip(f.key)"
              >
                <span class="mix-dot"></span>
                <span>혼합 {{ mergedCurrent[f.key].distinctValues.length }}</span>
              </span>

              <span v-else-if="isCurrentEmpty(f.key)" class="cur-box cur-empty">{{
                f.clearHint
              }}</span>

              <span v-else-if="f.key === 'itemGroup'" class="cur-box cur-uniform">
                <span
                  class="group-dot"
                  :style="{ background: groupColor(mergedCurrent[f.key].value) }"
                ></span>
                <span class="cur-text">{{ mergedCurrent[f.key].value }}</span>
              </span>

              <span v-else class="cur-box cur-uniform">
                <span class="cur-text">{{ formatCurrent(f.key) }}</span>
              </span>

              <!-- 화살표 -->
              <span class="arrow">→</span>

              <!-- 입력 영역 -->
              <div class="input-wrap">
                <!-- combo (datalist) -->
                <input
                  v-if="f.inputType === 'combo'"
                  type="text"
                  :list="`${f.key}-options`"
                  :placeholder="placeholderOf(f)"
                  :value="inputValueOf(f.key)"
                  :disabled="editForm[f.key].mode === 'clear'"
                  :class="[
                    'value-input',
                    {
                      'is-clear': editForm[f.key].mode === 'clear',
                      'is-set': editForm[f.key].mode === 'set',
                    },
                  ]"
                  @input="onInput(f, $event)"
                />
                <datalist v-if="f.inputType === 'combo'" :id="`${f.key}-options`">
                  <option v-for="o in optionsOf(f.key)" :key="o" :value="o"></option>
                </datalist>

                <!-- number -->
                <input
                  v-else
                  type="number"
                  min="0"
                  step="1"
                  :placeholder="placeholderOf(f)"
                  :value="inputValueOf(f.key)"
                  :disabled="editForm[f.key].mode === 'clear'"
                  :class="[
                    'value-input',
                    'num',
                    {
                      'is-clear': editForm[f.key].mode === 'clear',
                      'is-set': editForm[f.key].mode === 'set',
                    },
                  ]"
                  @input="onInput(f, $event)"
                />

                <span v-if="f.inputType === 'number' && f.unit" class="unit-label">{{
                  f.unit
                }}</span>
              </div>

              <!-- ⊘ / ↺ 토글 -->
              <button
                type="button"
                :class="['clear-btn', { active: editForm[f.key].mode === 'clear' }]"
                :title="
                  editForm[f.key].mode === 'clear' ? '비움 취소' : `${f.clearHint} 으로 비우기`
                "
                @click="toggleClear(f)"
              >
                {{ editForm[f.key].mode === 'clear' ? '↺' : '⊘' }}
              </button>
            </div>
          </div>
        </template>
      </div>

      <!-- 푸터 -->
      <div class="panel-footer">
        <div class="summary">
          <span
            >대상 <b>{{ selectedCells.length }}</b></span
          >
          <span class="dot">·</span>
          <span :class="['change-count', { 'has-change': changedFieldCount > 0 }]">
            변경 <b>{{ changedFieldCount }}</b
            >축
          </span>
        </div>
        <div class="actions">
          <button class="btn btn-secondary" :disabled="submitting" @click="handleReset">
            초기화
          </button>
          <button
            class="btn btn-primary"
            :disabled="!canApply"
            :title="!canApply ? '변경할 필드를 1개 이상 선택하세요' : ''"
            @click="handleApply"
          >
            {{ submitting ? '적용 중...' : `적용 (${selectedCells.length})` }}
          </button>
        </div>
      </div>
    </aside>
  </transition>
</template>

<script setup lang="ts">
  import { toRef, watch } from 'vue';
  import type { CellStateInfo } from '../../dashboard_2d/api/types';
  import {
    useCellClassify,
    inferStringMode,
    inferNumberMode,
  } from '../composables/useCellClassify';
  import { groupColor } from '../utils/groupColor';

  // ============================================
  // Props / Emits
  // ============================================

  const props = defineProps<{
    visible: boolean;
    selectedCells: CellStateInfo[];
    eqGroupId: string | null | undefined;
  }>();

  const emit = defineEmits<{
    (e: 'close'): void;
    (e: 'applied', affected: number): void;
  }>();

  // ============================================
  // Composable
  // ============================================

  const {
    editForm,
    mergedCurrent,
    options,
    optionsLoading,
    submitting,
    changedFieldCount,
    canApply,
    loadOptions,
    resetForm,
    apply,
  } = useCellClassify({
    selectedCells: toRef(props, 'selectedCells'),
    eqGroupId: toRef(props, 'eqGroupId'),
  });

  watch(
    () => props.visible,
    (v) => {
      if (v) loadOptions();
    },
    { immediate: true },
  );

  // ============================================
  // 필드 정의 — 4축 메타데이터 한 곳에 모음
  // ============================================

  type FieldKey = 'itemType' | 'itemGroup' | 'maxWeight' | 'maxHeight';

  interface FieldDef {
    key: FieldKey;
    label: string;
    inputType: 'combo' | 'number';
    placeholder: string;
    unit: string;
    clearHint: string;
  }

  const fieldDefs: FieldDef[] = [
    {
      key: 'itemType',
      label: 'Item Type',
      inputType: 'combo',
      placeholder: '입력...',
      unit: '',
      clearHint: '없음',
    },
    {
      key: 'itemGroup',
      label: 'Item Group',
      inputType: 'combo',
      placeholder: '입력...',
      unit: '',
      clearHint: '없음',
    },
    {
      key: 'maxWeight',
      label: 'Max Weight',
      inputType: 'number',
      placeholder: '800',
      unit: 'kg',
      clearHint: '제한없음',
    },
    {
      key: 'maxHeight',
      label: 'Max Height',
      inputType: 'number',
      placeholder: '1500',
      unit: 'mm',
      clearHint: '제한없음',
    },
  ];

  // ============================================
  // Helpers
  // ============================================

  function optionsOf(key: FieldKey): string[] {
    if (key === 'itemType') return options.value.item_types ?? [];
    if (key === 'itemGroup') return options.value.item_groups ?? [];
    return [];
  }

  function formatNumber(v: any): string {
    const n = Number(v ?? 0);
    if (Number.isNaN(n)) return String(v ?? '');
    if (n === 0) return '0';
    return n.toLocaleString();
  }

  function formatCurrent(key: FieldKey): string {
    const m = mergedCurrent.value[key];
    if (!m || m.kind !== 'uniform') return '';
    const v = (m as any).value;
    if (key === 'maxWeight' || key === 'maxHeight') return formatNumber(v);
    return String(v ?? '');
  }

  function isCurrentEmpty(key: FieldKey): boolean {
    const m = mergedCurrent.value[key];
    if (!m || m.kind !== 'uniform') return false;
    const v = (m as any).value;
    return v == null || v === '' || v === 0;
  }

  function mixedTooltip(key: FieldKey): string {
    const m = mergedCurrent.value[key];
    if (!m || m.kind !== 'mixed') return '';
    const vals = (m as any).distinctValues as any[];
    return vals
      .slice(0, 10)
      .map((v) => {
        if (key === 'maxWeight' || key === 'maxHeight') return formatNumber(v);
        return String(v ?? '∅');
      })
      .join(', ');
  }

  function placeholderOf(f: FieldDef): string {
    if (editForm[f.key].mode === 'clear') return `⊘ ${f.clearHint}`;
    const m = mergedCurrent.value[f.key];
    if (m && m.kind === 'mixed') {
      return `혼합 ${(m as any).distinctValues.length}종 · 일괄변경`;
    }
    return f.placeholder || '입력...';
  }

  function inputValueOf(key: FieldKey): any {
    if (editForm[key].mode === 'clear') return '';
    return editForm[key].value ?? '';
  }

  // ============================================
  // Event handlers
  // ============================================

  function onInput(f: FieldDef, e: Event) {
    const target = e.target as HTMLInputElement;
    const ed = editForm[f.key];
    if (f.inputType === 'number') {
      const raw = target.value;
      ed.value = raw === '' ? null : Number(raw);
      ed.mode = inferNumberMode(raw);
    } else {
      ed.value = target.value;
      ed.mode = inferStringMode(target.value);
    }
  }

  function toggleClear(f: FieldDef) {
    const ed = editForm[f.key];
    if (ed.mode === 'clear') {
      ed.mode = 'skip';
      ed.value = f.inputType === 'number' ? null : '';
    } else {
      ed.mode = 'clear';
      ed.value = f.inputType === 'number' ? null : '';
    }
  }

  function handleReset() {
    resetForm();
  }

  async function handleApply() {
    try {
      const affected = await apply();
      emit('applied', affected);
      resetForm();
    } catch (e: any) {
      console.error('[CellClassifyPanel] apply 실패', e);
    }
  }
</script>

<style scoped>
  /* ============================================
   루트
   ============================================ */
  .classify-panel {
    position: absolute;
    top: 20px;
    right: 20px;
    width: 400px;
    max-height: calc(100% - 40px);
    background: rgba(30, 34, 45, 0.96);
    backdrop-filter: blur(10px);
    border-radius: 12px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
    z-index: 1000;
    display: flex;
    flex-direction: column;
    color: #e5eaf3;
    overflow: hidden;
    user-select: none;
    box-sizing: border-box;
  }
  .classify-panel *,
  .classify-panel *::before,
  .classify-panel *::after {
    box-sizing: border-box;
  }

  /* ============================================
   헤더
   ============================================ */
  .panel-header {
    padding: 12px 16px;
    background: rgba(255, 255, 255, 0.05);
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    flex-shrink: 0;
  }
  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .panel-header h3 {
    margin: 0;
    font-size: 0.95rem;
    font-weight: 600;
    color: #409eff;
  }
  .selection-badge {
    font-size: 11px;
    color: #cfe7ff;
    background: rgba(64, 158, 255, 0.16);
    border: 1px solid rgba(64, 158, 255, 0.35);
    border-radius: 999px;
    padding: 2px 9px;
    font-weight: 600;
  }
  .close-btn {
    background: none;
    border: none;
    color: #909399;
    font-size: 22px;
    cursor: pointer;
    line-height: 1;
    padding: 0 4px;
  }
  .close-btn:hover {
    color: #e5eaf3;
  }

  /* ============================================
   본문
   ============================================ */
  .panel-content {
    padding: 12px 14px;
    overflow-y: auto;
    flex: 1;
  }
  .empty-hint {
    text-align: center;
    color: #909399;
    padding: 24px 8px;
    font-size: 12px;
  }

  /* ============================================
   안내 배너
   ============================================ */
  .usage-hint {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 11px;
    color: #a8aeba;
    background: rgba(64, 158, 255, 0.06);
    border-left: 2px solid rgba(64, 158, 255, 0.5);
    padding: 6px 10px;
    margin-bottom: 10px;
    border-radius: 2px;
    line-height: 1.4;
  }
  .usage-hint .hint-icon {
    flex-shrink: 0;
  }
  .usage-hint b {
    color: #ffd9a0;
    font-weight: 700;
    margin: 0 1px;
  }

  /* ============================================
   필드 행
   ============================================ */
  .field-row {
    padding: 10px 12px;
    margin-bottom: 8px;
    background: rgba(255, 255, 255, 0.025);
    border: 1px solid rgba(255, 255, 255, 0.06);
    border-radius: 6px;
    transition: border-color 0.12s, background 0.12s;
  }
  .field-row:last-child {
    margin-bottom: 0;
  }
  .field-row.mode-set {
    border-color: rgba(64, 158, 255, 0.55);
    background: rgba(64, 158, 255, 0.07);
  }
  .field-row.mode-clear {
    border-color: rgba(255, 170, 0, 0.55);
    background: rgba(255, 170, 0, 0.06);
  }

  /* 1행 */
  .field-row-head {
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 6px;
    margin-bottom: 6px;
    min-height: 16px;
    flex-wrap: nowrap;
  }
  .field-label {
    font-size: 11px;
    font-weight: 700;
    color: #c0c4cc;
    text-transform: uppercase;
    letter-spacing: 0.4px;
    flex-shrink: 0;
  }
  .field-loading {
    font-size: 10px;
    color: #909399;
    font-style: italic;
    margin-left: 6px;
    flex: 1;
  }
  .status-badge {
    font-size: 10px;
    font-weight: 700;
    padding: 1px 6px;
    border-radius: 3px;
    white-space: nowrap;
    max-width: 65%;
    overflow: hidden;
    text-overflow: ellipsis;
    flex-shrink: 1;
    min-width: 0;
  }
  .status-set {
    color: #cfe7ff;
    background: rgba(64, 158, 255, 0.22);
    border: 1px solid rgba(64, 158, 255, 0.5);
  }
  .status-clear {
    color: #ffd9a0;
    background: rgba(255, 170, 0, 0.18);
    border: 1px solid rgba(255, 170, 0, 0.5);
  }

  /* 2행 — 가로 정렬 */
  .field-row-body {
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 6px;
    flex-wrap: nowrap;
    width: 100%;
  }

  /* 현재값 칩 */
  .cur-box {
    flex: 0 0 auto;
    min-width: 64px;
    max-width: 120px;
    height: 28px;
    padding: 0 8px;
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 12px;
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.08);
    white-space: nowrap;
    overflow: hidden;
  }
  .cur-box .cur-text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
  }
  .cur-uniform {
    color: #e5eaf3;
    font-weight: 600;
  }
  .cur-empty {
    color: #707380;
    font-style: italic;
    justify-content: center;
  }
  .cur-mixed {
    color: #ffaa00;
    background: rgba(255, 170, 0, 0.08);
    border-color: rgba(255, 170, 0, 0.4);
    font-weight: 600;
    cursor: help;
  }
  .mix-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #ffaa00;
    display: inline-block;
    flex-shrink: 0;
  }
  .group-dot {
    width: 10px;
    height: 10px;
    border-radius: 2px;
    display: inline-block;
    flex-shrink: 0;
    border: 1px solid rgba(0, 0, 0, 0.25);
  }

  /* 화살표 */
  .arrow {
    flex-shrink: 0;
    color: #606470;
    font-size: 13px;
    font-weight: 700;
    padding: 0 1px;
  }
  .field-row.mode-set .arrow {
    color: #409eff;
  }
  .field-row.mode-clear .arrow {
    color: #ffaa00;
  }

  /* 입력 영역 */
  .input-wrap {
    flex: 1 1 auto;
    min-width: 0;
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 4px;
    flex-wrap: nowrap;
  }

  /* ============================================
   input — scoped 가 정상 적용되므로 specificity 충분
   외부 글로벌이 워낙 강할 경우만 !important 유지
   ============================================ */
  .value-input {
    flex: 1 1 auto;
    width: auto !important; /* element-plus / antd 의 width:100% 방어 */
    min-width: 0;
    display: block;
    background: rgba(0, 0, 0, 0.4) !important;
    background-image: none !important;
    border: 1px solid rgba(255, 255, 255, 0.15) !important;
    border-radius: 3px;
    padding: 0 8px;
    margin: 0;
    font-size: 12px;
    font-family: inherit;
    color: #e5eaf3 !important;
    outline: none;
    height: 28px;
    line-height: 28px;
    box-sizing: border-box;
    box-shadow: none !important;
    -webkit-appearance: none;
    -moz-appearance: none;
    appearance: none;
    transition: border-color 0.12s, background 0.12s;
  }
  .value-input:focus {
    border-color: #409eff !important;
    background: rgba(0, 0, 0, 0.5) !important;
  }
  .value-input:hover:not(:disabled):not(:focus) {
    border-color: rgba(255, 255, 255, 0.25) !important;
  }
  .value-input.is-set {
    border-color: #409eff !important;
    background: rgba(64, 158, 255, 0.12) !important;
    color: #ffffff !important;
  }
  .value-input.is-clear {
    color: #ffd9a0 !important;
    background: rgba(255, 170, 0, 0.08) !important;
    border-color: rgba(255, 170, 0, 0.4) !important;
    font-style: italic;
    cursor: not-allowed;
  }
  .value-input::placeholder {
    color: #707380 !important;
    font-style: italic;
    opacity: 1;
  }
  .value-input.is-clear::placeholder {
    color: #ffd9a0 !important;
    opacity: 0.85;
  }
  /* autofill 방어 */
  .value-input:-webkit-autofill,
  .value-input:-webkit-autofill:hover,
  .value-input:-webkit-autofill:focus {
    -webkit-text-fill-color: #e5eaf3 !important;
    -webkit-box-shadow: 0 0 0 1000px rgba(0, 0, 0, 0.4) inset !important;
    caret-color: #e5eaf3 !important;
    transition: background-color 5000s ease-in-out 0s;
  }
  .value-input.num::-webkit-outer-spin-button,
  .value-input.num::-webkit-inner-spin-button {
    -webkit-appearance: none;
    margin: 0;
  }
  .value-input.num {
    -moz-appearance: textfield;
  }

  /* datalist 드롭다운 화살표 제거 */
  .value-input::-webkit-calendar-picker-indicator {
    display: none !important;
    opacity: 0;
    -webkit-appearance: none;
    appearance: none;
  }
  .value-input::-webkit-list-button {
    display: none !important;
    opacity: 0;
    -webkit-appearance: none;
    appearance: none;
  }
  .unit-label {
    font-size: 10px;
    color: #909399;
    flex-shrink: 0;
    padding: 0 2px;
    font-weight: 600;
  }

  /* ⊘ / ↺ */
  .clear-btn {
    flex: 0 0 28px;
    height: 28px;
    width: 28px;
    min-width: 28px;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 3px;
    color: #909399;
    font-size: 14px;
    font-weight: 700;
    font-family: inherit;
    cursor: pointer;
    transition: all 0.12s;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    margin: 0;
    box-shadow: none;
    line-height: 1;
    -webkit-appearance: none;
    appearance: none;
  }
  .clear-btn:hover {
    background: rgba(255, 170, 0, 0.14);
    border-color: rgba(255, 170, 0, 0.45);
    color: #ffd9a0;
  }
  .clear-btn.active {
    background: rgba(255, 170, 0, 0.22);
    border-color: rgba(255, 170, 0, 0.65);
    color: #ffd9a0;
  }

  /* ============================================
   푸터
   ============================================ */
  .panel-footer {
    padding: 10px 14px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(255, 255, 255, 0.03);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    flex-shrink: 0;
  }
  .summary {
    font-size: 11px;
    color: #909399;
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
  .summary b {
    color: #e5eaf3;
    font-weight: 700;
  }
  .summary .dot {
    opacity: 0.5;
  }
  .change-count.has-change {
    color: #cfe7ff;
  }
  .change-count.has-change b {
    color: #409eff;
  }

  .actions {
    display: inline-flex;
    gap: 6px;
  }
  .btn {
    height: 30px;
    padding: 0 14px;
    font-size: 12px;
    font-weight: 600;
    font-family: inherit;
    border-radius: 4px;
    border: 1px solid transparent;
    cursor: pointer;
    transition: filter 0.15s, background 0.15s;
    margin: 0;
    line-height: 1;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
  .btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
  .btn-primary {
    background: #409eff;
    color: #fff;
    border-color: #409eff;
  }
  .btn-primary:hover:not(:disabled) {
    filter: brightness(1.1);
  }
  .btn-secondary {
    background: transparent;
    color: #c0c4cc;
    border-color: rgba(255, 255, 255, 0.18);
  }
  .btn-secondary:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.06);
  }

  /* 슬라이드 인 */
  .slide-fade-enter-active {
    transition: all 0.3s ease-out;
  }
  .slide-fade-leave-active {
    transition: all 0.2s cubic-bezier(1, 0.5, 0.8, 1);
  }
  .slide-fade-enter-from,
  .slide-fade-leave-to {
    transform: translateX(20px);
    opacity: 0;
  }
</style>
