import { onMounted, reactive, ref } from 'vue';
import {
  createStorageArea,
  deleteStorageArea,
  fetchCenterOptions,
  fetchOperationProfileOptions,
  fetchStorageAreaDetail,
  fetchStorageAreas,
  updateStorageArea,
} from '@/api/asrs/location';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import type {
  SelectOption,
  StorageAreaFilters,
  StorageAreaForm,
  StorageAreaLoadingState,
  StorageAreaRow,
} from '../types';

function createInitialFilters(): StorageAreaFilters {
  return {
    centerCode: '',
    areaCode: '',
    areaName: '',
    activeYn: 'Y',
  };
}

function createInitialForm(): StorageAreaForm {
  return {
    centerCode: '',
    areaCode: '',
    areaName: '',
    areaType: 'ASRS',
    operationProfileCode: '',
    description: '',
    activeYn: 'Y',
  };
}

/**
 * 응답 payload 에서 목록 배열 추출
 *
 * 방어 대상:
 * - []
 * - { items: [] }
 * - { content: [] }
 * - { data: [] }
 */
function resolveList(payload: any): any[] {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  return [];
}

/**
 * 아레아 row normalize
 *
 * 규칙:
 * - snake_case / camelCase 모두 흡수
 */
function normalizeRow(row: any): StorageAreaRow {
  return {
    id: row?.id ?? '',
    centerCode: row?.centerCode ?? row?.center_code ?? '',
    centerName: row?.centerName ?? row?.center_name ?? '',
    areaCode: row?.areaCode ?? row?.area_code ?? '',
    areaName: row?.areaName ?? row?.area_name ?? '',
    areaType: row?.areaType ?? row?.area_type ?? '',
    operationProfileCode: row?.operationProfileCode ?? row?.operation_profile_code ?? '',
    operationProfileName: row?.operationProfileName ?? row?.operation_profile_name ?? '',
    description: row?.description ?? '',
    activeYn: row?.activeYn ?? row?.active_yn ?? 'Y',
    linkedLocationProfileCount: Number(
      row?.linkedLocationProfileCount ?? row?.linked_location_profile_count ?? 0,
    ),
    linkedLocationCount: Number(
      row?.linkedLocationCount ?? row?.linked_location_count ?? 0,
    ),
    createdAt: row?.createdAt ?? row?.created_at ?? '',
    updatedAt: row?.updatedAt ?? row?.updated_at ?? '',
  };
}

export function useStorageAreaManager() {
  const filters = reactive<StorageAreaFilters>(createInitialFilters());
  const form = reactive<StorageAreaForm>(createInitialForm());

  const rows = ref<StorageAreaRow[]>([]);
  const selectedRow = ref<StorageAreaRow | null>(null);
  const editMode = ref<'create' | 'update'>('create');

  const centerOptions = ref<SelectOption[]>([]);
  const operationProfileOptions = ref<SelectOption[]>([]);

  const { flags: loading } = useAsyncFlags<StorageAreaLoadingState>({
    options: false,
    search: false,
    detail: false,
    save: false,
    delete: false,
  });

  const { feedback, setFeedback, clearFeedback } = useFeedback();

  /**
   * 센터 / 오퍼레이션 프로필 옵션 로딩
   */
  async function loadOptions() {
    loading.options = true;

    try {
      const [centerPayload, profilePayload] = await Promise.all([
        fetchCenterOptions(),
        fetchOperationProfileOptions(),
      ]);

      centerOptions.value = resolveList(centerPayload).map((row: any) => ({
        label: `${row?.centerCode ?? row?.center_code ?? ''} - ${row?.centerName ?? row?.center_name ?? ''}`,
        value: row?.centerCode ?? row?.center_code ?? '',
      }));

      operationProfileOptions.value = resolveList(profilePayload).map((row: any) => ({
        label: `${row?.profileCode ?? row?.profile_code ?? ''} - ${row?.profileName ?? row?.profile_name ?? ''}`,
        value: row?.profileCode ?? row?.profile_code ?? '',
      }));
    } catch (error) {
      console.error(error);
      setFeedback('error', '옵션 조회 중 오류가 발생했습니다.');
    } finally {
      loading.options = false;
    }
  }

  /**
   * 목록 조회
   */
  async function search() {
    loading.search = true;

    try {
      const payload = await fetchStorageAreas({
        centerCode: filters.centerCode,
        areaCode: filters.areaCode,
        areaName: filters.areaName,
        activeYn: filters.activeYn,
      });

      rows.value = resolveList(payload).map(normalizeRow);

      if (!rows.value.length) {
        setFeedback('warning', '조회 결과가 없습니다.');
        selectedRow.value = null;
        resetFormOnly();
        editMode.value = 'create';
        return;
      }

      setFeedback('success', `${rows.value.length}건 조회되었습니다.`);

      if (!selectedRow.value && rows.value[0]?.centerCode && rows.value[0]?.areaCode) {
        await selectRow(rows.value[0]);
      }
    } catch (error) {
      console.error(error);
      rows.value = [];
      selectedRow.value = null;
      setFeedback('error', '아레아 조회 중 오류가 발생했습니다.');
    } finally {
      loading.search = false;
    }
  }

  /**
   * 상세 조회
   */
  async function selectRow(row: StorageAreaRow) {
    if (!row || !row.centerCode || !row.areaCode) {
      return;
    }

    selectedRow.value = row;
    editMode.value = 'update';
    loading.detail = true;

    try {
      const payload = await fetchStorageAreaDetail(row.centerCode.trim(), row.areaCode.trim());
      const detail = normalizeRow(payload);

      form.centerCode = detail.centerCode;
      form.areaCode = detail.areaCode;
      form.areaName = detail.areaName;
      form.areaType = detail.areaType;
      form.operationProfileCode = detail.operationProfileCode;
      form.description = detail.description;
      form.activeYn = detail.activeYn;
      clearFeedback();
    } catch (error) {
      console.error(error);
      setFeedback('error', '아레아 상세 조회 중 오류가 발생했습니다.');
    } finally {
      loading.detail = false;
    }
  }

  /**
   * 신규 등록 모드
   */
  function startCreate() {
    selectedRow.value = null;
    editMode.value = 'create';
    resetFormOnly();
    clearFeedback();
  }

  /**
   * 조회조건 초기화
   */
  function resetFilters() {
    Object.assign(filters, createInitialFilters());
  }

  /**
   * 폼만 초기화
   */
  function resetFormOnly() {
    Object.assign(form, createInitialForm());
  }

  /**
   * 저장
   */
  async function save() {
    if (!form.centerCode.trim()) {
      setFeedback('warning', '센터는 필수입니다.');
      return;
    }
    if (!form.areaCode.trim()) {
      setFeedback('warning', '아레아 코드는 필수입니다.');
      return;
    }
    if (!form.areaName.trim()) {
      setFeedback('warning', '아레아명은 필수입니다.');
      return;
    }
    if (!form.areaType.trim()) {
      setFeedback('warning', '아레아 타입은 필수입니다.');
      return;
    }
    if (!form.operationProfileCode.trim()) {
      setFeedback('warning', '오퍼레이션 프로필은 필수입니다.');
      return;
    }

    loading.save = true;

    try {
      const payload = {
        centerCode: form.centerCode.trim(),
        areaCode: form.areaCode.trim(),
        areaName: form.areaName.trim(),
        areaType: form.areaType,
        operationProfileCode: form.operationProfileCode,
        description: form.description.trim(),
        activeYn: form.activeYn,
      };

      if (editMode.value === 'create') {
        await createStorageArea(payload);
        setFeedback('success', '아레아가 생성되었습니다.');
      } else {
        const originalCenterCode = selectedRow.value?.centerCode || form.centerCode.trim();
        const originalAreaCode = selectedRow.value?.areaCode || form.areaCode.trim();

        await updateStorageArea(originalCenterCode, originalAreaCode, payload);
        setFeedback('success', '아레아가 수정되었습니다.');
      }

      await search();

      const found = rows.value.find(
        (row) => row.centerCode === form.centerCode.trim() && row.areaCode === form.areaCode.trim(),
      );
      if (found) {
        await selectRow(found);
      }
    } catch (error) {
      console.error(error);
      setFeedback('error', '아레아 저장 중 오류가 발생했습니다.');
    } finally {
      loading.save = false;
    }
  }

  /**
   * 삭제
   */
  async function remove() {
    if (!selectedRow.value) {
      setFeedback('warning', '삭제할 아레아를 먼저 선택해주세요.');
      return;
    }

    if (selectedRow.value.linkedLocationProfileCount > 0) {
      setFeedback(
        'warning',
        `참조 중인 로케이션 프로필이 ${selectedRow.value.linkedLocationProfileCount}건 있어 삭제할 수 없습니다.`,
      );
      return;
    }

    if (selectedRow.value.linkedLocationCount > 0) {
      setFeedback(
        'warning',
        `참조 중인 로케이션이 ${selectedRow.value.linkedLocationCount}건 있어 삭제할 수 없습니다.`,
      );
      return;
    }

    const confirmed = window.confirm(
      `아레아 [${selectedRow.value.centerCode} / ${selectedRow.value.areaCode}]를 삭제하시겠습니까?`,
    );

    if (!confirmed) {
      return;
    }

    loading.delete = true;

    try {
      await deleteStorageArea(selectedRow.value.centerCode, selectedRow.value.areaCode);
      setFeedback('success', '아레아가 삭제되었습니다.');
      startCreate();
      await search();
    } catch (error) {
      console.error(error);
      setFeedback('error', '아레아 삭제 중 오류가 발생했습니다.');
    } finally {
      loading.delete = false;
    }
  }

  onMounted(async () => {
    await loadOptions();
    await search();
  });

  return {
    filters,
    form,
    rows,
    selectedRow,
    editMode,
    centerOptions,
    operationProfileOptions,
    loading,
    feedback,
    loadOptions,
    search,
    selectRow,
    startCreate,
    resetFilters,
    resetFormOnly,
    save,
    remove,
  };
}
