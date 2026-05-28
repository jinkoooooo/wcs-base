import { computed, onMounted, reactive, ref } from 'vue';
import {
  createLocationProfile,
  deleteLocationProfile,
  fetchLocationProfileDetail,
  fetchLocationProfilePreview,
  fetchLocationProfiles,
  fetchStorageAreas,
  generateLocationsByProfile,
  updateLocationProfile,
} from '@/api/asrs/location';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import type {
  LocationGenerateState,
  LocationProfileFilters,
  LocationProfileForm,
  LocationProfileLoadingState,
  LocationProfilePreviewState,
  LocationProfileRow,
  SelectOption,
} from '../types';

/**
 * 로케이션 프로필 관리 composable
 *
 * 규칙:
 * - API 응답은 snake_case / camelCase 모두 흡수
 * - composable 내부에서 normalize 함수로 일관 처리
 * - 상세/수정/삭제/preview/generate 호출 전 key 빈값 방어
 */
export function useLocationProfileManager() {
  const filters = reactive<LocationProfileFilters>(createInitialFilters());
  const form = reactive<LocationProfileForm>(createInitialForm());

  const rows = ref<LocationProfileRow[]>([]);
  const selectedRow = ref<LocationProfileRow | null>(null);
  const editMode = ref<'create' | 'update'>('create');

  const areaOptions = ref<SelectOption[]>([]);
  const previewState = reactive<LocationProfilePreviewState>(createInitialPreviewState());
  const generateState = reactive<LocationGenerateState>(createInitialGenerateState());

  const { flags: loading } = useAsyncFlags<LocationProfileLoadingState>({
    options: false,
    search: false,
    detail: false,
    save: false,
    delete: false,
    preview: false,
    generate: false,
  });

  const { feedback, setFeedback, clearFeedback } = useFeedback();

  /**
   * 실제 저장/preview/generate 에 사용할 code_pattern 계산
   *
   * - preset 이 직접입력이면 custom 사용
   * - 아니면 preset 값 그대로 사용
   */
  const effectiveCodePattern = computed(() => {
    if (form.codePatternPreset === '__CUSTOM__') {
      return (form.codePatternCustom || '').trim();
    }
    return form.codePatternPreset;
  });

  /**
   * 초기 필터 생성
   */
  function createInitialFilters(): LocationProfileFilters {
    return {
      areaCode: '',
      profileCode: '',
      profileName: '',
      activeYn: 'Y',
    };
  }

  /**
   * 초기 폼 생성
   */
  function createInitialForm(): LocationProfileForm {
    return {
      areaCode: '',
      profileCode: '',
      profileName: '',
      aisleStart: 1,
      aisleEnd: 1,
      sideCodes: 'L,R',
      bayStart: 1,
      bayEnd: 1,
      levelStart: 1,
      levelEnd: 1,
      depthStart: 1,
      depthEnd: 1,
      locationType: 'NORMAL',
      codePatternPreset: '{AREA}-A{AISLE}-S{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}',
      codePatternCustom: '',
      mixedLoadYn: 'N',
      inboundAllowedYn: 'Y',
      outboundAllowedYn: 'Y',
      activeYn: 'Y',
    };
  }

  /**
   * 초기 preview 상태 생성
   */
  function createInitialPreviewState(): LocationProfilePreviewState {
    return {
      totalTargetCount: 0,
      existingCount: 0,
      creatableCount: 0,
      previewLocationCodes: [],
    };
  }

  /**
   * 초기 generate 상태 생성
   */
  function createInitialGenerateState(): LocationGenerateState {
    return {
      requestedCount: 0,
      createdCount: 0,
      skippedCount: 0,
      createdLocationCodes: [],
      skippedLocationCodes: [],
    };
  }

  /**
   * payload 에서 목록 배열 추출
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
   * 로케이션 프로필 row normalize
   *
   * 목적:
   * - 테이블/상세 공통 매핑
   * - snake_case / camelCase 동시 대응
   */
  function normalizeLocationProfileRow(row: any): LocationProfileRow {
    return {
      id: row?.id ?? '',
      areaCode: row?.areaCode ?? row?.area_code ?? '',
      areaName: row?.areaName ?? row?.area_name ?? '',
      profileCode: row?.profileCode ?? row?.profile_code ?? '',
      profileName: row?.profileName ?? row?.profile_name ?? '',
      aisleStart: Number(row?.aisleStart ?? row?.aisle_start ?? 0),
      aisleEnd: Number(row?.aisleEnd ?? row?.aisle_end ?? 0),
      sideCodes: row?.sideCodes ?? row?.side_codes ?? '',
      bayStart: Number(row?.bayStart ?? row?.bay_start ?? 0),
      bayEnd: Number(row?.bayEnd ?? row?.bay_end ?? 0),
      levelStart: Number(row?.levelStart ?? row?.level_start ?? 0),
      levelEnd: Number(row?.levelEnd ?? row?.level_end ?? 0),
      depthStart: Number(row?.depthStart ?? row?.depth_start ?? 0),
      depthEnd: Number(row?.depthEnd ?? row?.depth_end ?? 0),
      locationType: row?.locationType ?? row?.location_type ?? '',
      codePattern: row?.codePattern ?? row?.code_pattern ?? '',
      mixedLoadYn: row?.mixedLoadYn ?? row?.mixed_load_yn ?? 'N',
      inboundAllowedYn: row?.inboundAllowedYn ?? row?.inbound_allowed_yn ?? 'Y',
      outboundAllowedYn: row?.outboundAllowedYn ?? row?.outbound_allowed_yn ?? 'Y',
      activeYn: row?.activeYn ?? row?.active_yn ?? 'Y',
      linkedLocationCount: Number(row?.linkedLocationCount ?? row?.linked_location_count ?? 0),
      createdAt: row?.createdAt ?? row?.created_at ?? '',
      updatedAt: row?.updatedAt ?? row?.updated_at ?? '',
    };
  }

  /**
   * preview 결과 normalize
   *
   * 목적:
   * - payload 직접 접근 대신 일관된 변환 계층 유지
   * - snake_case / camelCase 동시 대응
   */
  function normalizeLocationProfilePreview(payload: any): LocationProfilePreviewState {
    return {
      totalTargetCount: Number(
        payload?.totalTargetCount ?? payload?.total_target_count ?? 0,
      ),
      existingCount: Number(
        payload?.existingCount ?? payload?.existing_count ?? 0,
      ),
      creatableCount: Number(
        payload?.creatableCount ?? payload?.creatable_count ?? 0,
      ),
      previewLocationCodes:
        payload?.previewLocationCodes
        ?? payload?.preview_location_codes
        ?? [],
    };
  }

  /**
   * generate 결과 normalize
   *
   * 목적:
   * - payload 직접 접근 대신 일관된 변환 계층 유지
   * - snake_case / camelCase 동시 대응
   */
  function normalizeLocationGenerateResult(payload: any): LocationGenerateState {
    return {
      requestedCount: Number(
        payload?.requestedCount ?? payload?.requested_count ?? 0,
      ),
      createdCount: Number(
        payload?.createdCount ?? payload?.created_count ?? 0,
      ),
      skippedCount: Number(
        payload?.skippedCount ?? payload?.skipped_count ?? 0,
      ),
      createdLocationCodes:
        payload?.createdLocationCodes
        ?? payload?.created_location_codes
        ?? [],
      skippedLocationCodes:
        payload?.skippedLocationCodes
        ?? payload?.skipped_location_codes
        ?? [],
    };
  }

  /**
   * area 옵션 normalize
   */
  function normalizeAreaOption(row: any): SelectOption {
    const areaCode = row?.areaCode ?? row?.area_code ?? '';
    const areaName = row?.areaName ?? row?.area_name ?? '';

    return {
      label: `${areaCode} - ${areaName}`,
      value: areaCode,
    };
  }

  /**
   * detail row 를 form 에 반영
   */
  function applyDetailToForm(detail: LocationProfileRow) {
    form.areaCode = detail.areaCode;
    form.profileCode = detail.profileCode;
    form.profileName = detail.profileName;
    form.aisleStart = detail.aisleStart;
    form.aisleEnd = detail.aisleEnd;
    form.sideCodes = detail.sideCodes;
    form.bayStart = detail.bayStart;
    form.bayEnd = detail.bayEnd;
    form.levelStart = detail.levelStart;
    form.levelEnd = detail.levelEnd;
    form.depthStart = detail.depthStart;
    form.depthEnd = detail.depthEnd;
    form.locationType = detail.locationType;
    form.mixedLoadYn = detail.mixedLoadYn;
    form.inboundAllowedYn = detail.inboundAllowedYn;
    form.outboundAllowedYn = detail.outboundAllowedYn;
    form.activeYn = detail.activeYn;

    /**
     * code pattern 이 preset 목록 중 하나면 preset 선택
     * 아니면 custom 입력으로 전환
     */
    if (
      detail.codePattern === '{AREA}-A{AISLE}-S{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}'
      || detail.codePattern === '{AREA}-A{AISLE}-{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}'
      || detail.codePattern === '{AREA}-A{AISLE}-{SIDE}{BAY}-{LEVEL}-{DEPTH}'
      || detail.codePattern === '{AREA}-R{AISLE}-S{SIDE}-B{BAY}-L{LEVEL}-D{DEPTH}'
      || detail.codePattern === '{AREA}-{AISLE}-{SIDE}-{BAY}-{LEVEL}-{DEPTH}'
    ) {
      form.codePatternPreset = detail.codePattern;
      form.codePatternCustom = '';
    } else {
      form.codePatternPreset = '__CUSTOM__';
      form.codePatternCustom = detail.codePattern;
    }
  }

  /**
   * preview 상태 초기화
   */
  function resetPreviewState() {
    Object.assign(previewState, createInitialPreviewState());
  }

  /**
   * generate 상태 초기화
   */
  function resetGenerateState() {
    Object.assign(generateState, createInitialGenerateState());
  }

  /**
   * area 옵션 로딩
   */
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
      setFeedback('error', '아레아 옵션 조회 중 오류가 발생했습니다.');
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
      const payload = await fetchLocationProfiles({
        areaCode: filters.areaCode,
        profileCode: filters.profileCode,
        profileName: filters.profileName,
        activeYn: filters.activeYn,
      });

      rows.value = resolveList(payload).map(normalizeLocationProfileRow);

      if (!rows.value.length) {
        setFeedback('warning', '조회 결과가 없습니다.');
        selectedRow.value = null;
        resetFormOnly();
        editMode.value = 'create';
        resetPreviewState();
        resetGenerateState();
        return;
      }

      setFeedback('success', `${rows.value.length}건 조회되었습니다.`);

      if (!selectedRow.value && rows.value[0]?.areaCode && rows.value[0]?.profileCode) {
        await selectRow(rows.value[0]);
      }
    } catch (error) {
      console.error(error);
      rows.value = [];
      selectedRow.value = null;
      setFeedback('error', '로케이션 프로필 조회 중 오류가 발생했습니다.');
    } finally {
      loading.search = false;
    }
  }

  /**
   * 행 선택 후 상세조회
   */
  async function selectRow(row: LocationProfileRow) {
    if (!row || !row.areaCode || !row.profileCode) {
      return;
    }

    selectedRow.value = row;
    editMode.value = 'update';
    loading.detail = true;

    try {
      const payload = await fetchLocationProfileDetail(
        row.areaCode.trim(),
        row.profileCode.trim(),
      );

      const detail = normalizeLocationProfileRow(payload);

      applyDetailToForm(detail);
      clearFeedback();
      resetPreviewState();
      resetGenerateState();
    } catch (error) {
      console.error(error);
      setFeedback('error', '로케이션 프로필 상세 조회 중 오류가 발생했습니다.');
    } finally {
      loading.detail = false;
    }
  }

  /**
   * 신규 모드
   */
  function startCreate() {
    selectedRow.value = null;
    editMode.value = 'create';
    resetFormOnly();
    resetPreviewState();
    resetGenerateState();
    clearFeedback();
  }

  /**
   * 조회조건 초기화
   */
  function resetFilters() {
    Object.assign(filters, createInitialFilters());
  }

  /**
   * 폼 초기화
   */
  function resetFormOnly() {
    Object.assign(form, createInitialForm());
  }

  /**
   * 저장
   */
  async function save() {
    if (!form.areaCode.trim()) {
      setFeedback('warning', '아레아는 필수입니다.');
      return;
    }
    if (!form.profileCode.trim()) {
      setFeedback('warning', '프로필 코드는 필수입니다.');
      return;
    }
    if (!form.profileName.trim()) {
      setFeedback('warning', '프로필명은 필수입니다.');
      return;
    }
    if (!effectiveCodePattern.value) {
      setFeedback('warning', '코드 패턴은 필수입니다.');
      return;
    }

    loading.save = true;

    try {
      const payload = {
        areaCode: form.areaCode.trim(),
        profileCode: form.profileCode.trim(),
        profileName: form.profileName.trim(),
        aisleStart: Number(form.aisleStart),
        aisleEnd: Number(form.aisleEnd),
        sideCodes: form.sideCodes.trim(),
        bayStart: Number(form.bayStart),
        bayEnd: Number(form.bayEnd),
        levelStart: Number(form.levelStart),
        levelEnd: Number(form.levelEnd),
        depthStart: Number(form.depthStart),
        depthEnd: Number(form.depthEnd),
        locationType: form.locationType,
        codePattern: effectiveCodePattern.value,
        mixedLoadYn: form.mixedLoadYn,
        inboundAllowedYn: form.inboundAllowedYn,
        outboundAllowedYn: form.outboundAllowedYn,
        activeYn: form.activeYn,
      };

      if (editMode.value === 'create') {
        await createLocationProfile(payload);
        setFeedback('success', '로케이션 프로필이 생성되었습니다.');
      } else {
        const originalAreaCode = selectedRow.value?.areaCode || form.areaCode.trim();
        const originalProfileCode = selectedRow.value?.profileCode || form.profileCode.trim();

        await updateLocationProfile(originalAreaCode, originalProfileCode, payload);
        setFeedback('success', '로케이션 프로필이 수정되었습니다.');
      }

      await search();

      const found = rows.value.find(
        (row) =>
          row.areaCode === form.areaCode.trim()
          && row.profileCode === form.profileCode.trim(),
      );

      if (found) {
        await selectRow(found);
      }
    } catch (error) {
      console.error(error);
      setFeedback('error', '로케이션 프로필 저장 중 오류가 발생했습니다.');
    } finally {
      loading.save = false;
    }
  }

  /**
   * 삭제
   */
  async function remove() {
    if (!selectedRow.value?.areaCode || !selectedRow.value?.profileCode) {
      setFeedback('warning', '삭제할 로케이션 프로필을 먼저 선택해주세요.');
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
      `로케이션 프로필 [${selectedRow.value.areaCode} / ${selectedRow.value.profileCode}]를 삭제하시겠습니까?`,
    );

    if (!confirmed) {
      return;
    }

    loading.delete = true;

    try {
      await deleteLocationProfile(
        selectedRow.value.areaCode,
        selectedRow.value.profileCode,
      );
      setFeedback('success', '로케이션 프로필이 삭제되었습니다.');
      startCreate();
      await search();
    } catch (error) {
      console.error(error);
      setFeedback('error', '로케이션 프로필 삭제 중 오류가 발생했습니다.');
    } finally {
      loading.delete = false;
    }
  }

  /**
   * preview 실행
   */
  async function preview() {
    if (!form.areaCode.trim() || !form.profileCode.trim()) {
      setFeedback('warning', 'Preview 는 areaCode / profileCode 가 필요합니다.');
      return;
    }

    loading.preview = true;

    try {
      const payload = await fetchLocationProfilePreview(
        form.areaCode.trim(),
        form.profileCode.trim(),
      );

      Object.assign(previewState, normalizeLocationProfilePreview(payload));
      setFeedback('success', 'Preview 조회가 완료되었습니다.');
    } catch (error) {
      console.error(error);
      setFeedback('error', 'Preview 조회 중 오류가 발생했습니다.');
    } finally {
      loading.preview = false;
    }
  }

  /**
   * generate 실행
   */
  async function generate() {
    if (!form.areaCode.trim() || !form.profileCode.trim()) {
      setFeedback('warning', 'Generate 는 areaCode / profileCode 가 필요합니다.');
      return;
    }

    const confirmed = window.confirm(
      `선택한 프로필 기준으로 로케이션을 생성하시겠습니까?\n[${form.areaCode} / ${form.profileCode}]`,
    );

    if (!confirmed) {
      return;
    }

    loading.generate = true;

    try {
      const payload = await generateLocationsByProfile(
        form.areaCode.trim(),
        form.profileCode.trim(),
      );

      Object.assign(generateState, normalizeLocationGenerateResult(payload));
      setFeedback('success', '로케이션 생성이 완료되었습니다.');

      await search();
    } catch (error) {
      console.error(error);
      setFeedback('error', '로케이션 생성 중 오류가 발생했습니다.');
    } finally {
      loading.generate = false;
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
    previewState,
    generateState,
    effectiveCodePattern,
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
    preview,
    generate,
  };
}
