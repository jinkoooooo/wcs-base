import { computed, onMounted, reactive, ref } from 'vue';
import {
  createOperationProfile,
  deleteOperationProfile,
  fetchOperationProfileDetail,
  fetchOperationProfiles,
  updateOperationProfile,
} from '@/api/asrs/location';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import type {
  OperationProfileFilters,
  OperationProfileForm,
  OperationProfileLoadingState,
  OperationProfileRow,
} from '../types';

function createInitialFilters(): OperationProfileFilters {
  return {
    profileCode: '',
    profileName: '',
    activeYn: 'Y',
  };
}

function createInitialForm(): OperationProfileForm {
  return {
    profileCode: '',
    profileName: '',
    industryType: 'FOOD',
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
function resolveRows(payload: any): OperationProfileRow[] {
  const rows = Array.isArray(payload)
    ? payload
    : Array.isArray(payload?.items)
      ? payload.items
      : Array.isArray(payload?.content)
        ? payload.content
        : Array.isArray(payload?.data)
          ? payload.data
          : [];

  return rows.map(normalizeRow);
}

/**
 * 오퍼레이션 프로필 row normalize
 *
 * 규칙:
 * - snake_case / camelCase 모두 흡수
 */
function normalizeRow(row: any): OperationProfileRow {
  return {
    id: row?.id ?? '',
    profileCode: row?.profileCode ?? row?.profile_code ?? '',
    profileName: row?.profileName ?? row?.profile_name ?? '',
    industryType: row?.industryType ?? row?.industry_type ?? '',
    description: row?.description ?? '',
    activeYn: row?.activeYn ?? row?.active_yn ?? 'Y',
    linkedAreaCount: Number(row?.linkedAreaCount ?? row?.linked_area_count ?? 0),
    createdAt: row?.createdAt ?? row?.created_at ?? '',
    updatedAt: row?.updatedAt ?? row?.updated_at ?? '',
  };
}

export function useOperationProfileManager() {
  const filters = reactive<OperationProfileFilters>(createInitialFilters());
  const form = reactive<OperationProfileForm>(createInitialForm());

  const rows = ref<OperationProfileRow[]>([]);
  const selectedRow = ref<OperationProfileRow | null>(null);
  const editMode = ref<'create' | 'update'>('create');

  const { flags: loading } = useAsyncFlags<OperationProfileLoadingState>({
    search: false,
    detail: false,
    save: false,
    delete: false,
  });

  const { feedback, setFeedback, clearFeedback } = useFeedback();

  const totalCount = computed(() => rows.value.length);
  const activeCount = computed(() => rows.value.filter((row) => row.activeYn === 'Y').length);

  /**
   * 목록 조회
   */
  async function search() {
    loading.search = true;

    try {
      const payload = await fetchOperationProfiles({
        profileCode: filters.profileCode,
        profileName: filters.profileName,
        activeYn: filters.activeYn,
      });

      rows.value = resolveRows(payload);

      if (rows.value.length === 0) {
        setFeedback('warning', '조회 결과가 없습니다.');
        selectedRow.value = null;
        resetFormOnly();
        editMode.value = 'create';
        return;
      }

      setFeedback('success', `${rows.value.length}건 조회되었습니다.`);

      if (!selectedRow.value && rows.value.length > 0 && rows.value[0]?.profileCode) {
        await selectRow(rows.value[0]);
      }
    } catch (error) {
      console.error(error);
      rows.value = [];
      selectedRow.value = null;
      setFeedback('error', '오퍼레이션 프로필 조회 중 오류가 발생했습니다.');
    } finally {
      loading.search = false;
    }
  }

  /**
   * 행 선택 후 상세 조회
   */
  async function selectRow(row: OperationProfileRow) {
    if (!row || !row.profileCode || !row.profileCode.trim()) {
      return;
    }

    selectedRow.value = row;
    editMode.value = 'update';
    loading.detail = true;

    try {
      const payload = await fetchOperationProfileDetail(row.profileCode.trim());
      const detail = normalizeRow(payload);

      form.profileCode = detail.profileCode;
      form.profileName = detail.profileName;
      form.industryType = detail.industryType;
      form.description = detail.description;
      form.activeYn = detail.activeYn;
      clearFeedback();
    } catch (error) {
      console.error(error);
      setFeedback('error', '오퍼레이션 프로필 상세 조회 중 오류가 발생했습니다.');
    } finally {
      loading.detail = false;
    }
  }

  /**
   * 신규 입력 모드 전환
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
    if (!form.profileCode.trim()) {
      setFeedback('warning', '프로필 코드는 필수입니다.');
      return;
    }
    if (!form.profileName.trim()) {
      setFeedback('warning', '프로필명은 필수입니다.');
      return;
    }
    if (!form.industryType.trim()) {
      setFeedback('warning', '산업군은 필수입니다.');
      return;
    }

    loading.save = true;

    try {
      const payload = {
        profileCode: form.profileCode.trim(),
        profileName: form.profileName.trim(),
        industryType: form.industryType,
        description: form.description.trim(),
        activeYn: form.activeYn,
      };

      if (editMode.value === 'create') {
        await createOperationProfile(payload);
        setFeedback('success', '오퍼레이션 프로필이 생성되었습니다.');
      } else {
        const originalProfileCode = selectedRow.value?.profileCode || form.profileCode.trim();
        await updateOperationProfile(originalProfileCode, payload);
        setFeedback('success', '오퍼레이션 프로필이 수정되었습니다.');
      }

      await search();

      const targetCode = (form.profileCode || '').trim();
      if (targetCode) {
        const found = rows.value.find((row) => row.profileCode === targetCode);
        if (found) {
          await selectRow(found);
        }
      }
    } catch (error) {
      console.error(error);
      setFeedback('error', '오퍼레이션 프로필 저장 중 오류가 발생했습니다.');
    } finally {
      loading.save = false;
    }
  }

  /**
   * 삭제
   */
  async function remove() {
    if (!selectedRow.value) {
      setFeedback('warning', '삭제할 오퍼레이션 프로필을 먼저 선택해주세요.');
      return;
    }

    if (selectedRow.value.linkedAreaCount > 0) {
      setFeedback('warning', `참조 중인 아레아가 ${selectedRow.value.linkedAreaCount}건 있어 삭제할 수 없습니다.`);
      return;
    }

    const confirmed = window.confirm(
      `오퍼레이션 프로필 [${selectedRow.value.profileCode}]를 삭제하시겠습니까?`,
    );

    if (!confirmed) {
      return;
    }

    loading.delete = true;

    try {
      await deleteOperationProfile(selectedRow.value.profileCode);
      setFeedback('success', '오퍼레이션 프로필이 삭제되었습니다.');
      startCreate();
      await search();
    } catch (error) {
      console.error(error);
      setFeedback('error', '오퍼레이션 프로필 삭제 중 오류가 발생했습니다.');
    } finally {
      loading.delete = false;
    }
  }

  onMounted(async () => {
    await search();
  });

  return {
    filters,
    form,
    rows,
    selectedRow,
    editMode,
    loading,
    feedback,
    totalCount,
    activeCount,
    search,
    selectRow,
    startCreate,
    resetFilters,
    resetFormOnly,
    save,
    remove,
  };
}
