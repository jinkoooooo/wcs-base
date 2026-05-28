import { computed, onMounted, reactive, ref } from 'vue';
import {
  createCenter,
  deleteCenter,
  fetchCenterDetail,
  fetchCenters,
  updateCenter,
} from '@/api/asrs/location';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import type {
  CenterFilters,
  CenterForm,
  CenterLoadingState,
  CenterRow,
} from '../types';

function createInitialFilters(): CenterFilters {
  return {
    centerCode: '',
    centerName: '',
    activeYn: 'Y',
  };
}

function createInitialForm(): CenterForm {
  return {
    centerCode: '',
    centerName: '',
    centerType: 'ASRS',
    timezone: 'Asia/Seoul',
    description: '',
    activeYn: 'Y',
  };
}

function resolveRows(payload: any): CenterRow[] {
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
 * snake_case / camelCase 모두 흡수
 */
function normalizeRow(row: any): CenterRow {
  return {
    id: row?.id ?? '',
    centerCode: row?.centerCode ?? row?.center_code ?? '',
    centerName: row?.centerName ?? row?.center_name ?? '',
    centerType: row?.centerType ?? row?.center_type ?? '',
    timezone: row?.timezone ?? '',
    description: row?.description ?? '',
    activeYn: row?.activeYn ?? row?.active_yn ?? 'Y',
    linkedAreaCount: Number(row?.linkedAreaCount ?? row?.linked_area_count ?? 0),
    createdAt: row?.createdAt ?? row?.created_at ?? '',
    updatedAt: row?.updatedAt ?? row?.updated_at ?? '',
  };
}

export function useCenterManager() {
  const filters = reactive<CenterFilters>(createInitialFilters());
  const form = reactive<CenterForm>(createInitialForm());

  const rows = ref<CenterRow[]>([]);
  const selectedRow = ref<CenterRow | null>(null);
  const editMode = ref<'create' | 'update'>('create');

  const { flags: loading } = useAsyncFlags<CenterLoadingState>({
    search: false,
    detail: false,
    save: false,
    delete: false,
  });

  const { feedback, setFeedback, clearFeedback } = useFeedback();

  const totalCount = computed(() => rows.value.length);
  const activeCount = computed(() => rows.value.filter((row) => row.activeYn === 'Y').length);

  async function search() {
    loading.search = true;

    try {
      const payload = await fetchCenters({
        centerCode: filters.centerCode,
        centerName: filters.centerName,
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

      if (!selectedRow.value && rows.value.length > 0 && rows.value[0]?.centerCode) {
        await selectRow(rows.value[0]);
      }
    } catch (error) {
      console.error(error);
      rows.value = [];
      selectedRow.value = null;
      setFeedback('error', '센터 조회 중 오류가 발생했습니다.');
    } finally {
      loading.search = false;
    }
  }

  async function selectRow(row: CenterRow) {
    if (!row || !row.centerCode || !row.centerCode.trim()) {
      return;
    }

    selectedRow.value = row;
    editMode.value = 'update';
    loading.detail = true;

    try {
      const payload = await fetchCenterDetail(row.centerCode.trim());
      const detail = normalizeRow(payload);

      form.centerCode = detail.centerCode;
      form.centerName = detail.centerName;
      form.centerType = detail.centerType;
      form.timezone = detail.timezone;
      form.description = detail.description;
      form.activeYn = detail.activeYn;
      clearFeedback();
    } catch (error) {
      console.error(error);
      setFeedback('error', '센터 상세 조회 중 오류가 발생했습니다.');
    } finally {
      loading.detail = false;
    }
  }

  function startCreate() {
    selectedRow.value = null;
    editMode.value = 'create';
    resetFormOnly();
    clearFeedback();
  }

  function resetFilters() {
    Object.assign(filters, createInitialFilters());
  }

  function resetFormOnly() {
    Object.assign(form, createInitialForm());
  }

  async function save() {
    if (!form.centerCode.trim()) {
      setFeedback('warning', '센터 코드는 필수입니다.');
      return;
    }
    if (!form.centerName.trim()) {
      setFeedback('warning', '센터명은 필수입니다.');
      return;
    }
    if (!form.centerType.trim()) {
      setFeedback('warning', '센터 타입은 필수입니다.');
      return;
    }
    if (!form.timezone.trim()) {
      setFeedback('warning', 'Timezone 은 필수입니다.');
      return;
    }

    loading.save = true;

    try {
      const payload = {
        centerCode: form.centerCode.trim(),
        centerName: form.centerName.trim(),
        centerType: form.centerType,
        timezone: form.timezone,
        description: form.description.trim(),
        activeYn: form.activeYn,
      };

      if (editMode.value === 'create') {
        await createCenter(payload);
        setFeedback('success', '센터가 생성되었습니다.');
      } else {
        const originalCenterCode = selectedRow.value?.centerCode || form.centerCode.trim();
        await updateCenter(originalCenterCode, payload);
        setFeedback('success', '센터가 수정되었습니다.');
      }

      await search();

      const targetCode = (form.centerCode || '').trim();

      if (targetCode) {
        const found = rows.value.find((row) => row.centerCode === targetCode);
        if (found) {
          await selectRow(found);
        }
      }
    } catch (error) {
      console.error(error);
      setFeedback('error', '센터 저장 중 오류가 발생했습니다.');
    } finally {
      loading.save = false;
    }
  }

  async function remove() {
    if (!selectedRow.value || !selectedRow.value.centerCode) {
      setFeedback('warning', '삭제할 센터를 먼저 선택해주세요.');
      return;
    }

    if (selectedRow.value.linkedAreaCount > 0) {
      setFeedback('warning', `참조 중인 아레아가 ${selectedRow.value.linkedAreaCount}건 있어 삭제할 수 없습니다.`);
      return;
    }

    const confirmed = window.confirm(
      `센터 [${selectedRow.value.centerCode}]를 삭제하시겠습니까?`,
    );

    if (!confirmed) {
      return;
    }

    loading.delete = true;

    try {
      await deleteCenter(selectedRow.value.centerCode);
      setFeedback('success', '센터가 삭제되었습니다.');
      startCreate();
      await search();
    } catch (error) {
      console.error(error);
      setFeedback('error', '센터 삭제 중 오류가 발생했습니다.');
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
