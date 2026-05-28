/**
 * useCellClassify
 *
 * 선택된 셀들의 분류·제약 4필드 (item_type / item_group / max_weight / max_height)
 * 일괄 편집 상태를 관리한다.
 *
 *  - mergedCurrent: 선택 셀들의 현재 값 머지 ("uniform" | "mixed" | "empty")
 *  - editForm:      라디오(mode=skip|set|clear) + value 입력 폼
 *  - apply():       /update-classification POST + 옵션 재로드
 *  - options:       /classify-options 의 distinct 값들 (콤보 옵션)
 *
 * mode 가 'skip' 이면 해당 필드는 SET 절에서 빠진다 (변경 안 함).
 */

import { computed, reactive, ref, watch } from 'vue';
import type { CellStateInfo } from '../../dashboard_2d/api/types';
import { getSearchList, updateList } from '/@/api/common/api';

export type FieldMode = 'skip' | 'set' | 'clear';

export interface FieldEdit<T> {
  mode: FieldMode;
  value: T;
}

export interface ClassifyForm {
  itemType:  FieldEdit<string>;
  itemGroup: FieldEdit<string>;
  maxWeight: FieldEdit<number | null>;
  maxHeight: FieldEdit<number | null>;
}

/** 선택 셀들의 현재 값 머지 결과 */
export interface MergedCurrent<T> {
  /** 모두 같은 값이면 'uniform', 셀이 없으면 'empty', 그 외는 'mixed' */
  kind: 'uniform' | 'mixed' | 'empty';
  /** uniform 일 때의 값. mixed/empty 면 null */
  value: T | null;
  /** mixed 일 때 나타난 distinct 값들 (uniform/empty 는 빈 배열) */
  distinctValues: T[];
}

export interface ClassifyOptions {
  item_types: string[];
  item_groups: string[];
}

const EMPTY_OPTIONS: ClassifyOptions = { item_types: [], item_groups: [] };

const BASE_URL = '/wcs/inventory/cell-state';

function freshForm(): ClassifyForm {
  return {
    itemType:  { mode: 'skip', value: '' },
    itemGroup: { mode: 'skip', value: '' },
    maxWeight: { mode: 'skip', value: null },
    maxHeight: { mode: 'skip', value: null },
  };
}

/**
 * 입력값 기반 자동 mode 전환 헬퍼 — 사용자가 입력칸에 타이핑하면 자동 set,
 * 모두 지우면 자동 skip 으로 복귀. clear 는 명시적 버튼이 호출.
 *
 *  ※ 호출 측에서 사용자가 [✕ 비움] 토글로 clear 모드를 켰을 때는 이 함수를
 *    호출하지 않는다 (텍스트 입력으로 인한 자동 전환은 set/skip 사이만).
 */
export function inferStringMode(value: string): FieldMode {
  return value && value.length > 0 ? 'set' : 'skip';
}

/** 숫자 필드용 자동 mode 추론 — null/빈문자 = skip, 그 외 = set */
export function inferNumberMode(value: number | null | string): FieldMode {
  if (value === null || value === undefined || value === '') return 'skip';
  return 'set';
}

function mergeStringField(cells: CellStateInfo[], key: 'item_type' | 'item_group'): MergedCurrent<string> {
  if (cells.length === 0) return { kind: 'empty', value: null, distinctValues: [] };
  const set = new Set<string>();
  for (const c of cells) set.add(String(c[key] ?? ''));
  const arr = Array.from(set);
  if (arr.length === 1) return { kind: 'uniform', value: arr[0], distinctValues: [] };
  return { kind: 'mixed', value: null, distinctValues: arr };
}

function mergeNumberField(cells: CellStateInfo[], key: 'max_weight' | 'max_height'): MergedCurrent<number> {
  if (cells.length === 0) return { kind: 'empty', value: null, distinctValues: [] };
  const set = new Set<number>();
  for (const c of cells) set.add(Number(c[key] ?? 0));
  const arr = Array.from(set);
  if (arr.length === 1) return { kind: 'uniform', value: arr[0], distinctValues: [] };
  return { kind: 'mixed', value: null, distinctValues: arr };
}

export function useCellClassify(opts: {
  selectedCells: { value: CellStateInfo[] };
  eqGroupId:     { value: string | null | undefined };
}) {
  const editForm = reactive<ClassifyForm>(freshForm());
  const options  = ref<ClassifyOptions>({ ...EMPTY_OPTIONS });
  const submitting = ref(false);
  const optionsLoading = ref(false);

  const mergedCurrent = computed(() => ({
    itemType:  mergeStringField(opts.selectedCells.value, 'item_type'),
    itemGroup: mergeStringField(opts.selectedCells.value, 'item_group'),
    maxWeight: mergeNumberField(opts.selectedCells.value, 'max_weight'),
    maxHeight: mergeNumberField(opts.selectedCells.value, 'max_height'),
  }));

  /** 변경 예정 필드 수 (skip 이 아닌 것) */
  const changedFieldCount = computed(() => {
    let n = 0;
    if (editForm.itemType.mode  !== 'skip') n += 1;
    if (editForm.itemGroup.mode !== 'skip') n += 1;
    if (editForm.maxWeight.mode !== 'skip') n += 1;
    if (editForm.maxHeight.mode !== 'skip') n += 1;
    return n;
  });

  const canApply = computed(() =>
    !submitting.value &&
    opts.selectedCells.value.length > 0 &&
    changedFieldCount.value > 0 &&
    formIsValid.value,
  );

  /** set 모드일 때 값이 유효한지 (숫자 음수 금지 등) */
  const formIsValid = computed(() => {
    if (editForm.maxWeight.mode === 'set') {
      const v = editForm.maxWeight.value;
      if (v == null || Number.isNaN(Number(v)) || Number(v) < 0) return false;
    }
    if (editForm.maxHeight.mode === 'set') {
      const v = editForm.maxHeight.value;
      if (v == null || Number.isNaN(Number(v)) || Number(v) < 0) return false;
    }
    return true;
  });

  function resetForm() {
    const f = freshForm();
    editForm.itemType  = f.itemType;
    editForm.itemGroup = f.itemGroup;
    editForm.maxWeight = f.maxWeight;
    editForm.maxHeight = f.maxHeight;
  }

  async function loadOptions() {
    const eqGroupId = opts.eqGroupId.value;
    if (!eqGroupId) {
      options.value = { ...EMPTY_OPTIONS };
      return;
    }
    optionsLoading.value = true;
    try {
      const resp: any = await getSearchList(`${BASE_URL}/classify-options`, {
        eq_group_id: eqGroupId,
      });
      options.value = {
        item_types:  Array.isArray(resp?.item_types)  ? resp.item_types  : [],
        item_groups: Array.isArray(resp?.item_groups) ? resp.item_groups : [],
      };
    } catch {
      options.value = { ...EMPTY_OPTIONS };
    } finally {
      optionsLoading.value = false;
    }
  }

  /**
   * 변경 적용. 성공 시 affected 행 수 반환.
   * 호출 측에서 셀 목록 재로드 + 토스트 처리.
   */
  async function apply(): Promise<number> {
    const eqGroupId = opts.eqGroupId.value;
    if (!eqGroupId) throw new Error('eq_group_id 누락');
    if (changedFieldCount.value === 0) throw new Error('변경 필드 없음');
    if (!formIsValid.value) throw new Error('입력값이 올바르지 않습니다');

    const cellIds = opts.selectedCells.value
      .map((c) => String(c.rack_id ?? ''))
      .filter((id) => id !== '' && id !== 'null');

    if (cellIds.length === 0) throw new Error('대상 셀 없음');

    const payload: any = {
      eq_group_id: eqGroupId,
      cell_ids:    cellIds,
    };
    if (editForm.itemType.mode !== 'skip') {
      payload.item_type = {
        mode: editForm.itemType.mode,
        value: editForm.itemType.mode === 'set' ? (editForm.itemType.value ?? '').trim() : '',
      };
    }
    if (editForm.itemGroup.mode !== 'skip') {
      payload.item_group = {
        mode: editForm.itemGroup.mode,
        value: editForm.itemGroup.mode === 'set' ? (editForm.itemGroup.value ?? '').trim() : '',
      };
    }
    if (editForm.maxWeight.mode !== 'skip') {
      payload.max_weight = {
        mode: editForm.maxWeight.mode,
        value: editForm.maxWeight.mode === 'set' ? Number(editForm.maxWeight.value ?? 0) : 0,
      };
    }
    if (editForm.maxHeight.mode !== 'skip') {
      payload.max_height = {
        mode: editForm.maxHeight.mode,
        value: editForm.maxHeight.mode === 'set' ? Number(editForm.maxHeight.value ?? 0) : 0,
      };
    }

    submitting.value = true;
    try {
      const resp: any = await updateList(`${BASE_URL}/update-classification`, payload);
      return Number(resp?.affected ?? 0);
    } finally {
      submitting.value = false;
    }
  }

  // 선택 셀이 바뀌면 폼은 초기화하지 않지만, 옵션은 ZONE 바뀔 때 재로드.
  watch(() => opts.eqGroupId.value, () => { loadOptions(); });

  return {
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
  };
}
