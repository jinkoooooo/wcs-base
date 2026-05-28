import { onMounted, reactive, ref } from 'vue';
import {
  createAccessPointMaster,
  deleteAccessPointMaster,
  fetchAccessPointMasterDetail,
  fetchAccessPointMasters,
  fetchStorageAreas,
  updateAccessPointMaster,
} from '@/api/asrs/location';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import type {
  AccessPointFilters,
  AccessPointForm,
  AccessPointLoadingState,
  AccessPointRow,
  SelectOption,
} from '../types';

/**
 * Access Point 관리 composable.
 *
 * 규칙:
 * - API 응답은 snake_case / camelCase 모두 흡수
 * - Area / Point Code를 업무 key로 사용
 * - 목적 정보는 INBOUND / OUTBOUND / PICK / RELOCATION 4개 항목으로 화면에서 관리
 */
export function useAccessPointManager() {
  const filters = reactive<AccessPointFilters>(createInitialFilters());
  const form = reactive<AccessPointForm>(createInitialForm());

  const rows = ref<AccessPointRow[]>([]);
  const selectedRow = ref<AccessPointRow | null>(null);
  const editMode = ref<'create' | 'update'>('create');

  const areaOptions = ref<SelectOption[]>([]);

  const { flags: loading } = useAsyncFlags<AccessPointLoadingState>({
    options: false,
    search: false,
    detail: false,
    save: false,
    delete: false,
  });

  const { feedback, setFeedback, clearFeedback } = useFeedback();

  function createInitialFilters(): AccessPointFilters {
    return {
      areaCode: '',
      pointCode: '',
      pointName: '',
      pointType: '',
      purposeCode: '',
      useForSortYn: '',
      activeYn: 'Y',
    };
  }

  function createInitialForm(): AccessPointForm {
    return {
      areaCode: '',
      pointCode: '',
      pointName: '',
      pointType: 'PORT',
      aisleNo: 1,
      sideCode: 'L',
      bayNo: 1,
      levelNo: 1,
      depthNo: 0,
      useForSortYn: 'Y',
      activeYn: 'Y',
      description: '',

      inboundYn: 'Y',
      inboundPriorityNo: 1,

      outboundYn: 'Y',
      outboundPriorityNo: 1,

      pickYn: 'N',
      pickPriorityNo: 2,

      relocationYn: 'N',
      relocationPriorityNo: 3,
    };
  }

  function resolveList(payload: any): any[] {
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.items)) return payload.items;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.data)) return payload.data;
    return [];
  }

  function normalizeAreaOption(row: any): SelectOption {
    const areaCode = row?.areaCode ?? row?.area_code ?? '';
    const areaName = row?.areaName ?? row?.area_name ?? '';

    return {
      label: areaName ? `${areaCode} - ${areaName}` : areaCode,
      value: areaCode,
    };
  }

  function normalizeRow(row: any): AccessPointRow {
    return {
      id: row?.id ?? '',
      areaId: row?.areaId ?? row?.area_id ?? '',
      areaCode: row?.areaCode ?? row?.area_code ?? '',
      areaName: row?.areaName ?? row?.area_name ?? '',
      pointCode: row?.pointCode ?? row?.point_code ?? '',
      pointName: row?.pointName ?? row?.point_name ?? '',
      pointType: row?.pointType ?? row?.point_type ?? '',
      aisleNo: Number(row?.aisleNo ?? row?.aisle_no ?? 0),
      sideCode: row?.sideCode ?? row?.side_code ?? '',
      bayNo: Number(row?.bayNo ?? row?.bay_no ?? 0),
      levelNo: Number(row?.levelNo ?? row?.level_no ?? 0),
      depthNo: Number(row?.depthNo ?? row?.depth_no ?? 0),
      useForSortYn: row?.useForSortYn ?? row?.use_for_sort_yn ?? 'Y',
      activeYn: row?.activeYn ?? row?.active_yn ?? 'Y',
      description: row?.description ?? '',
      purposeCodes: row?.purposeCodes ?? row?.purpose_codes ?? '',
      createdAt: row?.createdAt ?? row?.created_at ?? '',
      updatedAt: row?.updatedAt ?? row?.updated_at ?? '',
    };
  }

  function applyDetailToForm(payload: any) {
    const detail = normalizeRow(payload);

    form.areaCode = detail.areaCode;
    form.pointCode = detail.pointCode;
    form.pointName = detail.pointName;
    form.pointType = detail.pointType || 'PORT';
    form.aisleNo = detail.aisleNo;
    form.sideCode = detail.sideCode || 'L';
    form.bayNo = detail.bayNo;
    form.levelNo = detail.levelNo;
    form.depthNo = detail.depthNo;
    form.useForSortYn = detail.useForSortYn || 'Y';
    form.activeYn = detail.activeYn || 'Y';
    form.description = detail.description || '';

    resetPurposeFormOnly();

    const purposes = resolveList(payload?.purposes ?? []);

    purposes.forEach((purpose: any) => {
      const purposeCode = purpose?.purposeCode ?? purpose?.purpose_code ?? '';
      const activeYn = purpose?.activeYn ?? purpose?.active_yn ?? 'N';
      const priorityNo = Number(purpose?.priorityNo ?? purpose?.priority_no ?? 1);

      if (purposeCode === 'INBOUND') {
        form.inboundYn = activeYn;
        form.inboundPriorityNo = priorityNo;
      }

      if (purposeCode === 'OUTBOUND') {
        form.outboundYn = activeYn;
        form.outboundPriorityNo = priorityNo;
      }

      if (purposeCode === 'PICK') {
        form.pickYn = activeYn;
        form.pickPriorityNo = priorityNo;
      }

      if (purposeCode === 'RELOCATION') {
        form.relocationYn = activeYn;
        form.relocationPriorityNo = priorityNo;
      }
    });
  }

  function resetPurposeFormOnly() {
    form.inboundYn = 'N';
    form.inboundPriorityNo = 1;
    form.outboundYn = 'N';
    form.outboundPriorityNo = 1;
    form.pickYn = 'N';
    form.pickPriorityNo = 2;
    form.relocationYn = 'N';
    form.relocationPriorityNo = 3;
  }

  async function loadOptions() {
    loading.options = true;

    try {
      const payload = await fetchStorageAreas({
        centerCode: '',
        areaCode: '',
        areaName: '',
        activeYn: 'Y',
      });

      areaOptions.value = resolveList(payload).map(normalizeAreaOption);
    } catch (error) {
      console.error(error);
      setFeedback('error', 'Access Point 옵션 조회 중 오류가 발생했습니다.');
    } finally {
      loading.options = false;
    }
  }

  async function search() {
    loading.search = true;

    try {
      const payload = await fetchAccessPointMasters({
        areaCode: filters.areaCode,
        pointCode: filters.pointCode,
        pointName: filters.pointName,
        pointType: filters.pointType,
        purposeCode: filters.purposeCode,
        useForSortYn: filters.useForSortYn,
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

      if (!selectedRow.value && rows.value[0]?.areaCode && rows.value[0]?.pointCode) {
        await selectRow(rows.value[0]);
      }
    } catch (error) {
      console.error(error);
      rows.value = [];
      selectedRow.value = null;
      setFeedback('error', 'Access Point 조회 중 오류가 발생했습니다.');
    } finally {
      loading.search = false;
    }
  }

  async function selectRow(row: AccessPointRow) {
    if (!row || !row.areaCode || !row.pointCode) {
      return;
    }

    selectedRow.value = row;
    editMode.value = 'update';
    loading.detail = true;

    try {
      const payload = await fetchAccessPointMasterDetail(row.areaCode.trim(), row.pointCode.trim());

      applyDetailToForm(payload);
      clearFeedback();
    } catch (error) {
      console.error(error);
      setFeedback('error', 'Access Point 상세 조회 중 오류가 발생했습니다.');
    } finally {
      loading.detail = false;
    }
  }

  function startCreate() {
    selectedRow.value = null;
    editMode.value = 'create';
    resetFormOnly();
    clearFeedback();

    if (areaOptions.value.length) {
      form.areaCode = areaOptions.value[0].value;
    }
  }

  function resetFilters() {
    Object.assign(filters, createInitialFilters());
  }

  function resetFormOnly() {
    Object.assign(form, createInitialForm());
  }

  function validateForm(): boolean {
    if (!form.areaCode.trim()) {
      setFeedback('warning', 'Area는 필수입니다.');
      return false;
    }

    if (!form.pointCode.trim()) {
      setFeedback('warning', 'Point Code는 필수입니다.');
      return false;
    }

    if (!form.pointName.trim()) {
      setFeedback('warning', 'Point Name은 필수입니다.');
      return false;
    }

    if (!form.pointType.trim()) {
      setFeedback('warning', 'Point Type은 필수입니다.');
      return false;
    }

    if (!form.sideCode.trim()) {
      setFeedback('warning', 'Side는 필수입니다.');
      return false;
    }

    const purposes = buildPurposes();
    if (!purposes.some((purpose) => purpose.activeYn === 'Y')) {
      setFeedback('warning', '목적은 최소 1개 이상 Y로 설정해야 합니다.');
      return false;
    }

    return true;
  }

  function buildPurposes() {
    return [
      {
        purposeCode: 'INBOUND',
        priorityNo: Number(form.inboundPriorityNo || 1),
        activeYn: form.inboundYn,
      },
      {
        purposeCode: 'OUTBOUND',
        priorityNo: Number(form.outboundPriorityNo || 1),
        activeYn: form.outboundYn,
      },
      {
        purposeCode: 'PICK',
        priorityNo: Number(form.pickPriorityNo || 1),
        activeYn: form.pickYn,
      },
      {
        purposeCode: 'RELOCATION',
        priorityNo: Number(form.relocationPriorityNo || 1),
        activeYn: form.relocationYn,
      },
    ];
  }

  function buildSavePayload() {
    return {
      areaCode: form.areaCode.trim(),
      pointCode: form.pointCode.trim(),
      pointName: form.pointName.trim(),
      pointType: form.pointType,
      aisleNo: Number(form.aisleNo),
      sideCode: form.sideCode,
      bayNo: Number(form.bayNo),
      levelNo: Number(form.levelNo),
      depthNo: Number(form.depthNo),
      useForSortYn: form.useForSortYn,
      activeYn: form.activeYn,
      description: form.description.trim(),
      purposes: buildPurposes(),
    };
  }

  async function save() {
    if (!validateForm()) {
      return;
    }

    loading.save = true;

    try {
      const payload = buildSavePayload();

      if (editMode.value === 'create') {
        await createAccessPointMaster(payload);
        setFeedback('success', 'Access Point가 생성되었습니다.');
      } else {
        const originalAreaCode = selectedRow.value?.areaCode || form.areaCode.trim();
        const originalPointCode = selectedRow.value?.pointCode || form.pointCode.trim();

        await updateAccessPointMaster(originalAreaCode, originalPointCode, payload);
        setFeedback('success', 'Access Point가 수정되었습니다.');
      }

      await search();

      const found = rows.value.find(
        (row) => row.areaCode === form.areaCode.trim() && row.pointCode === form.pointCode.trim(),
      );

      if (found) {
        await selectRow(found);
      }
    } catch (error) {
      console.error(error);
      setFeedback('error', 'Access Point 저장 중 오류가 발생했습니다.');
    } finally {
      loading.save = false;
    }
  }

  async function remove() {
    if (!selectedRow.value?.areaCode || !selectedRow.value?.pointCode) {
      setFeedback('warning', '삭제할 Access Point를 먼저 선택해주세요.');
      return;
    }

    const confirmed = window.confirm(
      `Access Point를 비활성 처리하시겠습니까?\n\n` +
        `- Area: ${selectedRow.value.areaCode}\n` +
        `- Point: ${selectedRow.value.pointCode}\n\n` +
        `목적 정보도 함께 비활성 처리됩니다.`,
    );

    if (!confirmed) {
      return;
    }

    loading.delete = true;

    try {
      await deleteAccessPointMaster(selectedRow.value.areaCode, selectedRow.value.pointCode);

      setFeedback('success', 'Access Point가 비활성 처리되었습니다.');
      startCreate();
      await search();
    } catch (error) {
      console.error(error);
      setFeedback('error', 'Access Point 삭제 중 오류가 발생했습니다.');
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
    areaOptions,
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
