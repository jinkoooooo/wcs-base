import { onMounted, reactive, ref } from 'vue';
import {
  bulkDeleteLocations,
  createLocation,
  deleteLocation,
  fetchAccessPointOptions,
  fetchItemCategoryOptions,
  fetchLocationDetail,
  fetchLocations,
  fetchStorageAreas,
  updateLocation,
} from '@/api/asrs/location';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import type {
  LocationFilters,
  LocationForm,
  LocationLoadingState,
  LocationRow,
  SelectOption,
} from '../types';

function createInitialFilters(): LocationFilters {
  return {
    areaCode: '',
    locationCode: '',
    locationType: '',
    activeYn: 'Y',
  };
}

function createInitialForm(): LocationForm {
  return {
    areaCode: '',
    locationCode: '',
    aisleNo: 1,
    sideCode: 'L',
    bayNo: 1,
    levelNo: 1,
    depthNo: 1,
    locationType: 'NORMAL',
    usageStatusCode: 'ENABLED',
    inboundAllowedYn: 'Y',
    outboundAllowedYn: 'Y',
    mixedLoadYn: 'N',
    frontPriorityYn: 'Y',
    dedicatedItemCategoryCode: '',
    maxWeightG: null,
    maxVolumeMm3: null,
    sortSeq: null,
    activeYn: 'Y',
    locationGrade: 'D',
    accessScore: null,
    primaryAccessPointCode: '',
  };
}

function resolveList(payload: any): any[] {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  return [];
}

function normalizeRow(row: any): LocationRow {
  return {
    id: row?.id ?? '',
    areaCode: row?.areaCode ?? row?.area_code ?? '',
    areaName: row?.areaName ?? row?.area_name ?? '',
    locationCode: row?.locationCode ?? row?.location_code ?? '',
    aisleNo: Number(row?.aisleNo ?? row?.aisle_no ?? 0),
    sideCode: row?.sideCode ?? row?.side_code ?? '',
    bayNo: Number(row?.bayNo ?? row?.bay_no ?? 0),
    levelNo: Number(row?.levelNo ?? row?.level_no ?? 0),
    depthNo: Number(row?.depthNo ?? row?.depth_no ?? 0),
    locationType: row?.locationType ?? row?.location_type ?? '',
    usageStatusCode: row?.usageStatusCode ?? row?.usage_status_code ?? '',
    inboundAllowedYn: row?.inboundAllowedYn ?? row?.inbound_allowed_yn ?? 'Y',
    outboundAllowedYn: row?.outboundAllowedYn ?? row?.outbound_allowed_yn ?? 'Y',
    mixedLoadYn: row?.mixedLoadYn ?? row?.mixed_load_yn ?? 'N',
    frontPriorityYn: row?.frontPriorityYn ?? row?.front_priority_yn ?? 'Y',
    dedicatedItemCategoryCode: row?.dedicatedItemCategoryCode ?? row?.dedicated_item_category_code ?? '',
    dedicatedItemCategoryName: row?.dedicatedItemCategoryName ?? row?.dedicated_item_category_name ?? '',
    maxWeightG: row?.maxWeightG ?? row?.max_weight_g ?? null,
    maxVolumeMm3: row?.maxVolumeMm3 ?? row?.max_volume_mm3 ?? null,
    sortSeq: row?.sortSeq ?? row?.sort_seq ?? null,
    activeYn: row?.activeYn ?? row?.active_yn ?? 'Y',
    locationGrade: row?.locationGrade ?? row?.location_grade ?? 'D',
    accessScore: row?.accessScore ?? row?.access_score ?? null,
    primaryAccessPointCode: row?.primaryAccessPointCode ?? row?.primary_access_point_code ?? '',
    primaryAccessPointName: row?.primaryAccessPointName ?? row?.primary_access_point_name ?? '',
    createdAt: row?.createdAt ?? row?.created_at ?? '',
    updatedAt: row?.updatedAt ?? row?.updated_at ?? '',
  };
}

export function useLocationManager() {
  const filters = reactive<LocationFilters>(createInitialFilters());
  const form = reactive<LocationForm>(createInitialForm());

  const rows = ref<LocationRow[]>([]);
  const selectedRow = ref<LocationRow | null>(null);
  const editMode = ref<'create' | 'update'>('create');

  const areaOptions = ref<SelectOption[]>([]);
  const itemCategoryOptions = ref<SelectOption[]>([]);
  const accessPointOptions = ref<SelectOption[]>([]);

  const { flags: loading } = useAsyncFlags<LocationLoadingState>({
    options: false,
    search: false,
    detail: false,
    save: false,
    delete: false,
    accessPointOptions: false,
  });

  const { feedback, setFeedback, clearFeedback } = useFeedback();

  async function loadOptions() {
    loading.options = true;

    try {
      const [areaPayload, itemCategoryPayload] = await Promise.all([
        fetchStorageAreas({
          centerCode: '',
          areaCode: '',
          areaName: '',
          activeYn: 'Y',
        }),
        fetchItemCategoryOptions(),
      ]);

      areaOptions.value = resolveList(areaPayload).map((row: any) => ({
        label: `${row?.areaCode ?? row?.area_code ?? ''} - ${row?.areaName ?? row?.area_name ?? ''}`,
        value: row?.areaCode ?? row?.area_code ?? '',
      }));

      itemCategoryOptions.value = resolveList(itemCategoryPayload).map((row: any) => ({
        label: `${row?.categoryCode ?? row?.category_code ?? ''} - ${row?.categoryName ?? row?.category_name ?? ''}`,
        value: row?.categoryCode ?? row?.category_code ?? '',
      }));
    } catch (error) {
      console.error(error);
      setFeedback('error', '로케이션 옵션 조회 중 오류가 발생했습니다.');
    } finally {
      loading.options = false;
    }
  }

  async function loadAccessPointOptions(areaCode: string) {
    if (!areaCode || !areaCode.trim()) {
      accessPointOptions.value = [];
      return;
    }

    loading.accessPointOptions = true;

    try {
      const payload = await fetchAccessPointOptions(areaCode.trim());

      accessPointOptions.value = resolveList(payload).map((row: any) => ({
        label: `${row?.pointCode ?? row?.point_code ?? ''} - ${row?.pointName ?? row?.point_name ?? ''}`,
        value: row?.pointCode ?? row?.point_code ?? '',
      }));
    } catch (error) {
      console.error(error);
      accessPointOptions.value = [];
      setFeedback('error', 'Access Point 옵션 조회 중 오류가 발생했습니다.');
    } finally {
      loading.accessPointOptions = false;
    }
  }

  async function search() {
    loading.search = true;

    try {
      const payload = await fetchLocations({
        areaCode: filters.areaCode,
        locationCode: filters.locationCode,
        locationType: filters.locationType,
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

      if (!selectedRow.value && rows.value[0]?.areaCode && rows.value[0]?.locationCode) {
        await selectRow(rows.value[0]);
      }
    } catch (error) {
      console.error(error);
      rows.value = [];
      selectedRow.value = null;
      setFeedback('error', '로케이션 조회 중 오류가 발생했습니다.');
    } finally {
      loading.search = false;
    }
  }

  async function selectRow(row: LocationRow) {
    if (!row || !row.areaCode || !row.locationCode) {
      return;
    }

    selectedRow.value = row;
    editMode.value = 'update';
    loading.detail = true;

    try {
      const payload = await fetchLocationDetail(row.areaCode.trim(), row.locationCode.trim());
      const detail = normalizeRow(payload);

      form.areaCode = detail.areaCode;
      form.locationCode = detail.locationCode;
      form.aisleNo = detail.aisleNo;
      form.sideCode = detail.sideCode;
      form.bayNo = detail.bayNo;
      form.levelNo = detail.levelNo;
      form.depthNo = detail.depthNo;
      form.locationType = detail.locationType;
      form.usageStatusCode = detail.usageStatusCode;
      form.inboundAllowedYn = detail.inboundAllowedYn;
      form.outboundAllowedYn = detail.outboundAllowedYn;
      form.mixedLoadYn = detail.mixedLoadYn;
      form.frontPriorityYn = detail.frontPriorityYn;
      form.dedicatedItemCategoryCode = detail.dedicatedItemCategoryCode;
      form.maxWeightG = detail.maxWeightG;
      form.maxVolumeMm3 = detail.maxVolumeMm3;
      form.sortSeq = detail.sortSeq;
      form.activeYn = detail.activeYn;
      form.locationGrade = detail.locationGrade;
      form.accessScore = detail.accessScore;
      form.primaryAccessPointCode = detail.primaryAccessPointCode;

      await loadAccessPointOptions(detail.areaCode);
      clearFeedback();
    } catch (error) {
      console.error(error);
      setFeedback('error', '로케이션 상세 조회 중 오류가 발생했습니다.');
    } finally {
      loading.detail = false;
    }
  }

  async function changeAreaCode(nextAreaCode: string) {
    form.areaCode = nextAreaCode;
    form.primaryAccessPointCode = '';
    await loadAccessPointOptions(nextAreaCode);
  }

  function startCreate() {
    selectedRow.value = null;
    editMode.value = 'create';
    resetFormOnly();
    accessPointOptions.value = [];
    clearFeedback();
  }

  function resetFilters() {
    Object.assign(filters, createInitialFilters());
  }

  function resetFormOnly() {
    Object.assign(form, createInitialForm());
  }

  async function save() {
    if (!form.areaCode.trim()) {
      setFeedback('warning', '아레아는 필수입니다.');
      return;
    }
    if (!form.locationCode.trim()) {
      setFeedback('warning', '로케이션 코드는 필수입니다.');
      return;
    }

    loading.save = true;

    try {
      const payload = {
        areaCode: form.areaCode.trim(),
        locationCode: form.locationCode.trim(),
        aisleNo: Number(form.aisleNo),
        sideCode: form.sideCode,
        bayNo: Number(form.bayNo),
        levelNo: Number(form.levelNo),
        depthNo: Number(form.depthNo),
        locationType: form.locationType,
        usageStatusCode: form.usageStatusCode,
        inboundAllowedYn: form.inboundAllowedYn,
        outboundAllowedYn: form.outboundAllowedYn,
        mixedLoadYn: form.mixedLoadYn,
        frontPriorityYn: form.frontPriorityYn,
        dedicatedItemCategoryCode: form.dedicatedItemCategoryCode || '',
        maxWeightG: form.maxWeightG,
        maxVolumeMm3: form.maxVolumeMm3,
        sortSeq: form.sortSeq,
        activeYn: form.activeYn,
        locationGrade: form.locationGrade,
        accessScore: form.accessScore,
        primaryAccessPointCode: form.primaryAccessPointCode || '',
      };

      if (editMode.value === 'create') {
        await createLocation(payload);
        setFeedback('success', '로케이션이 생성되었습니다.');
      } else {
        const originalAreaCode = selectedRow.value?.areaCode || form.areaCode.trim();
        const originalLocationCode = selectedRow.value?.locationCode || form.locationCode.trim();
        await updateLocation(originalAreaCode, originalLocationCode, payload);
        setFeedback('success', '로케이션이 수정되었습니다.');
      }

      await search();

      const found = rows.value.find(
        (row) => row.areaCode === form.areaCode.trim() && row.locationCode === form.locationCode.trim(),
      );
      if (found) {
        await selectRow(found);
      }
    } catch (error) {
      console.error(error);
      setFeedback('error', '로케이션 저장 중 오류가 발생했습니다.');
    } finally {
      loading.save = false;
    }
  }

  async function remove() {
    if (!selectedRow.value?.areaCode || !selectedRow.value?.locationCode) {
      setFeedback('warning', '삭제할 로케이션을 먼저 선택해주세요.');
      return;
    }

    const confirmed = window.confirm(
      `선택한 로케이션을 삭제하시겠습니까?\n\n` +
      `- Area: ${selectedRow.value.areaCode}\n` +
      `- Location: ${selectedRow.value.locationCode}\n\n` +
      `해당 로케이션에 적치된 현재고도 함께 삭제됩니다.`,
    );

    if (!confirmed) {
      return;
    }

    loading.delete = true;

    try {
      const result = await deleteLocation(
        selectedRow.value.areaCode,
        selectedRow.value.locationCode,
      );

      setFeedback(
        'success',
        `로케이션이 삭제되었습니다. 함께 삭제된 재고 ${result?.deletedStockCount ?? 0}건`,
      );

      startCreate();
      await search();
    } catch (error) {
      console.error(error);
      setFeedback('error', '로케이션 삭제 중 오류가 발생했습니다.');
    } finally {
      loading.delete = false;
    }
  }

  async function bulkRemove() {
    if (!rows.value.length) {
      setFeedback('warning', '일괄삭제할 조회 결과가 없습니다.');
      return;
    }

    const confirmed = window.confirm(
      `현재 조회된 로케이션 ${rows.value.length}건을 일괄 삭제하시겠습니까?\n\n` +
      `연결된 현재고도 함께 삭제됩니다.\n` +
      `이 작업은 되돌릴 수 없습니다.`,
    );

    if (!confirmed) {
      return;
    }

    loading.delete = true;

    try {
      const result = await bulkDeleteLocations({
        areaCode: filters.areaCode,
        locationCode: filters.locationCode,
        locationType: filters.locationType,
        activeYn: filters.activeYn,
      });

      setFeedback(
        'success',
        `일괄 삭제 완료. 로케이션 ${result?.deletedLocationCount ?? 0}건, 재고 ${result?.deletedStockCount ?? 0}건 삭제`,
      );

      startCreate();
      await search();
    } catch (error) {
      console.error(error);
      setFeedback('error', '조회결과 일괄삭제 중 오류가 발생했습니다.');
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
    itemCategoryOptions,
    accessPointOptions,
    loading,
    feedback,
    loadOptions,
    loadAccessPointOptions,
    changeAreaCode,
    search,
    selectRow,
    startCreate,
    resetFilters,
    resetFormOnly,
    save,
    remove,
    bulkRemove,
  };
}
