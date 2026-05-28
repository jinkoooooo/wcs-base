<template>
  <div class="date-wrap">
    <!-- 텍스트 입력: 다양한 포맷 허용 -->
    <input
      ref="textEl"
      class="grid-input date-text"
      :class="{ invalid: showInvalid }"
      type="text"
      inputmode="numeric"
      :placeholder="placeholder"
      :value="draft"
      @input="onTextInput"
      @keydown.enter.prevent="commitDraft"
      @blur="onBlur"
    />

    <!-- 달력 아이콘(시각용) + 오버레이 date input(클릭용) -->
    <div class="cal-wrap" title="달력 열기">
      <button class="cal-btn" type="button" tabindex="-1" aria-hidden="true">
        <svg class="cal-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
          <path d="M16 2v4M8 2v4M3 10h18" />
        </svg>
      </button>

      <!-- ✅ 핵심: 버튼 위를 실제 date input이 투명하게 덮어서 "유저 클릭"으로 인식 -->
      <input class="date-picker-overlay" type="date" :value="val" @input="onPickerInput" />
    </div>

    <!-- ✅ 에러 아이콘 + 툴팁 -->
    <div
      v-if="showInvalid"
      class="err-wrap"
      @mouseenter="tipOpen = true"
      @mouseleave="tipOpen = false"
    >
      <svg class="err-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M10.3 3.6 2.6 17a2 2 0 0 0 1.7 3h15.4a2 2 0 0 0 1.7-3L13.7 3.6a2 2 0 0 0-3.4 0Z" />
        <path d="M12 9v4" />
        <path d="M12 17h.01" />
      </svg>

      <div v-if="tipOpen" class="err-tooltip" role="tooltip">
        {{ errorMessage }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';

  const props = defineProps<{
    modelValue: string | null | undefined; // YYYY-MM-DD
    placeholder?: string;
  }>();

  const emit = defineEmits<{
    (e: 'update:modelValue', v: string | null): void;
  }>();

  const textEl = ref<HTMLInputElement | null>(null);

  const placeholder = computed(() => props.placeholder ?? 'YYYY-MM-DD / 20260910 / 260910');

  // 현재 v-model 값을 date input에 맞게
  const val = computed(() => {
    const v = String(props.modelValue ?? '').trim();
    return /^\d{4}-\d{2}-\d{2}$/.test(v) ? v : '';
  });

  // 텍스트 입력 draft (사용자 입력 유지)
  const draft = ref<string>('');

  // 에러 표시 상태
  const invalid = ref(false);
  const tipOpen = ref(false);

  const errorMessage = computed(() => {
    return '유효하지 않은 날짜입니다. 예) 2026-09-18 / 20260918 / 260918';
  });

  // 입력 중 invalid 표시 기준:
  // - 빈 값은 invalid 아님
  // - 뭔가 입력했는데 parse가 안 되면 invalid
  const showInvalid = computed(() => {
    const s = String(draft.value ?? '').trim();
    if (!s) return false;
    return invalid.value;
  });

  // v-model 변경 시 draft 동기화 + invalid 초기화
  watch(
    () => props.modelValue,
    () => {
      draft.value = val.value || '';
      invalid.value = false;
      tipOpen.value = false;
    },
    { immediate: true },
  );

  function onPickerInput(e: Event) {
    const raw = (e.target as HTMLInputElement).value; // YYYY-MM-DD
    emit('update:modelValue', raw ? raw : null);

    // draft 동기화 + invalid 해제
    draft.value = raw ? raw : '';
    invalid.value = false;
    tipOpen.value = false;

    // UX: 달력 선택 후 커서 텍스트로 복귀(선택사항)
    textEl.value?.focus?.();
  }

  function onTextInput(e: Event) {
    draft.value = (e.target as HTMLInputElement).value;

    const s = String(draft.value ?? '').trim();
    if (!s) {
      invalid.value = false;
      return;
    }

    const parsed = parseFlexibleDate(s);
    invalid.value = parsed == null;
  }

  function onBlur() {
    // blur 시 입력값 커밋 시도, 실패하면 원복 + invalid 해제
    const ok = commitDraft();
    if (!ok) {
      draft.value = val.value || '';
      invalid.value = false;
      tipOpen.value = false;
    }
  }

  function commitDraft(): boolean {
    const s = String(draft.value ?? '').trim();

    // 빈 값이면 null 처리
    if (!s) {
      emit('update:modelValue', null);
      draft.value = '';
      invalid.value = false;
      tipOpen.value = false;
      return true;
    }

    const parsed = parseFlexibleDate(s);
    if (parsed) {
      emit('update:modelValue', parsed);
      draft.value = parsed;
      invalid.value = false;
      tipOpen.value = false;
      return true;
    }

    invalid.value = true;
    return false;
  }

  /**
   * 허용 입력:
   * - 260910 (YYMMDD)  -> 20YY-MM-DD (00~79 => 2000~2079, 80~99 => 1980~1999)
   * - 20260910 (YYYYMMDD)
   * - 2026-09-18 (YYYY-MM-DD)
   * - 2026/09/18, 2026.09.18, 2026 09 18 등
   *
   * 반환: YYYY-MM-DD (유효한 달/일만)
   */
  function parseFlexibleDate(input: string): string | null {
    const s0 = String(input ?? '').trim();
    if (!s0) return null;

    // 1) YYYY-MM-DD (또는 구분자 변형)
    const sepMatch = s0.match(/^(\d{4})\D+(\d{1,2})\D+(\d{1,2})$/);
    if (sepMatch) {
      const y = Number(sepMatch[1]);
      const m = Number(sepMatch[2]);
      const d = Number(sepMatch[3]);
      return normalizeYmd(y, m, d);
    }

    // 2) 숫자만
    const digits = s0.replace(/\D/g, '');
    if (digits.length === 8) {
      // YYYYMMDD
      const y = Number(digits.slice(0, 4));
      const m = Number(digits.slice(4, 6));
      const d = Number(digits.slice(6, 8));
      return normalizeYmd(y, m, d);
    }

    if (digits.length === 6) {
      // YYMMDD
      const yy = Number(digits.slice(0, 2));
      const m = Number(digits.slice(2, 4));
      const d = Number(digits.slice(4, 6));
      const y = yy <= 79 ? 2000 + yy : 1900 + yy;
      return normalizeYmd(y, m, d);
    }

    return null;
  }

  function pad2(n: number) {
    return String(n).padStart(2, '0');
  }

  function isValidDate(y: number, m: number, d: number): boolean {
    if (!Number.isFinite(y) || !Number.isFinite(m) || !Number.isFinite(d)) return false;
    if (y < 1900 || y > 2099) return false;
    if (m < 1 || m > 12) return false;
    if (d < 1 || d > 31) return false;

    const dt = new Date(y, m - 1, d);
    return (
      dt.getFullYear() === y &&
      dt.getMonth() === m - 1 &&
      dt.getDate() === d &&
      !Number.isNaN(dt.getTime())
    );
  }

  function normalizeYmd(y: number, m: number, d: number): string | null {
    if (!isValidDate(y, m, d)) return null;
    return `${y}-${pad2(m)}-${pad2(d)}`;
  }
</script>

<style scoped>
  .date-wrap {
    position: relative;
    width: 100%;
  }

  .grid-input {
    width: 100%;
    height: 30px;
    padding: 4px 66px 4px 8px; /* ✅ 오른쪽: 달력영역(32) + 여유 */
    border-radius: 8px;
    border: 1px solid #e5e7eb;
    outline: none;
    font-size: 12px;
    background: #fff;
    box-sizing: border-box;
  }

  .grid-input.invalid {
    padding-right: 90px; /* 달력 + 에러 아이콘까지 */
    border-color: rgba(239, 68, 68, 0.85);
    box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.12);
  }

  .grid-input:focus {
    border-color: rgba(91, 97, 246, 0.6);
    box-shadow: 0 0 0 2px rgba(91, 97, 246, 0.12);
  }
  .grid-input.invalid:focus {
    border-color: rgba(239, 68, 68, 0.9);
    box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.14);
  }

  /* 달력 영역 */
  .cal-wrap {
    position: absolute;
    top: 50%;
    right: 6px;
    transform: translateY(-50%);
    width: 28px;
    height: 28px;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
    background: #fff;
    overflow: hidden;
  }

  .cal-btn {
    width: 100%;
    height: 100%;
    border: 0;
    background: transparent;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    pointer-events: none; /* ✅ 클릭은 오버레이 input이 받게 */
  }

  .cal-ico {
    width: 16px;
    height: 16px;
    color: #475569;
  }

  /* ✅ 핵심: 달력 버튼 위를 덮는 실제 date input */
  .date-picker-overlay {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    opacity: 0; /* 투명하지만 클릭 가능 */
    cursor: pointer;
    border: 0;
    padding: 0;
    margin: 0;
  }

  /* 에러 아이콘 */
  .err-wrap {
    position: absolute;
    top: 50%;
    right: 38px; /* 달력 영역 왼쪽 */
    transform: translateY(-50%);
    width: 22px;
    height: 22px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .err-ico {
    width: 16px;
    height: 16px;
    color: rgba(239, 68, 68, 0.95);
  }

  /* 툴팁 */
  .err-tooltip {
    position: absolute;
    top: -8px;
    right: 0;
    transform: translateY(-100%);
    min-width: 260px;
    max-width: 360px;
    padding: 8px 10px;
    border-radius: 10px;
    background: rgba(15, 23, 42, 0.92);
    color: #fff;
    font-size: 12px;
    line-height: 1.3;
    box-shadow: 0 6px 18px rgba(15, 23, 42, 0.25);
    z-index: 10;
    white-space: normal;
  }

  .err-tooltip::after {
    content: '';
    position: absolute;
    right: 10px;
    bottom: -6px;
    width: 0;
    height: 0;
    border-left: 6px solid transparent;
    border-right: 6px solid transparent;
    border-top: 6px solid rgba(15, 23, 42, 0.92);
  }
</style>
