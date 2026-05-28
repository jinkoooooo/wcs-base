import { onMounted, reactive, ref } from 'vue';
import {
  executeLocationAccessRecalculate,
  fetchLocationAccessPreview,
  fetchStorageAreas,
} from '@/api/asrs/location';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import type {
  LocationGradeFilters,
  LocationGradeLoadingState,
  LocationGradePreviewRow,
  LocationGradePreviewState,
  LocationGradeResultState,
  SelectOption,
} from '../types';
import { DEFAULT_GRADE_RATIO } from '../types';

/**
 * 로케이션 등급 관리 composable.
 *
 * 규칙:
 * - API 응답은 snake_case / camelCase 모두 흡수
 * - 미리보기와 실행 요청은 동일한 기준값 사용
 * - 실행 전 사용자 확인 confirm 처리
 */
export function useLocationGradeManager() {
  const filters = reactive<LocationGradeFilters>(createInitialFilters());
  const areaOptions = ref<SelectOption[]>([]);

  const previewState = reactive<LocationGradePreviewState>(createInitialPreviewState());
  const resultState = reactive<LocationGradeResultState>(createInitialResultState());

  const { flags: loading } = useAsyncFlags<LocationGradeLoadingState>({
    options: false,
    preview: false,
    execute: false,
  });

  const { feedback, setFeedback, clearFeedback } = useFeedback();

  function createInitialFilters(): LocationGradeFilters {
    return {
      areaCode: '',
      purposeCode: 'INBOUND',
      gradeARatio: DEFAULT_GRADE_RATIO.gradeARatio,
      gradeBRatio: DEFAULT_GRADE_RATIO.gradeBRatio,
      gradeCRatio: DEFAULT_GRADE_RATIO.gradeCRatio,
      limit: DEFAULT_GRADE_RATIO.limit,
    };
  }

  function createInitialPreviewState(): LocationGradePreviewState {
    return {
      areaCode: '',
      purposeCode: '',
      totalCount: 0,
      previewCount: 0,
      rows: [],
    };
  }

  function createInitialResultState(): LocationGradeResultState {
    return {
      areaCode: '',
      purposeCode: '',
      targetLocationCount: 0,
      updatedCount: 0,
      gradeACount: 0,
      gradeBCount: 0,
      gradeCCount: 0,
      gradeDCount: 0,
      message: '',
    };
  }

  /**
   * payload 에서 목록 배열 추출.
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
   * Area 옵션 normalize.
   *
   * 목적:
   * - snake_case / camelCase 모두 대응
   * - select label 통일
   */
  function normalizeAreaOption(row: any): SelectOption {
    const areaCode = row?.areaCode ?? row?.area_code ?? '';
    const areaName = row?.areaName ?? row?.area_name ?? '';

    return {
      label: areaName ? `${areaCode} - ${areaName}` : areaCode,
      value: areaCode,
    };
  }

  /**
   * 미리보기 row normalize.
   *
   * 목적:
   * - 백엔드 DTO 필드가 camelCase / snake_case 어느 쪽으로 내려와도 화면에서 동일 처리
   */
  function normalizePreviewRow(row: any): LocationGradePreviewRow {
    return {
      locationId: row?.locationId ?? row?.location_id ?? '',
      locationCode: row?.locationCode ?? row?.location_code ?? '',
      aisleNo: Number(row?.aisleNo ?? row?.aisle_no ?? 0),
      sideCode: row?.sideCode ?? row?.side_code ?? '',
      bayNo: Number(row?.bayNo ?? row?.bay_no ?? 0),
      levelNo: Number(row?.levelNo ?? row?.level_no ?? 0),
      depthNo: Number(row?.depthNo ?? row?.depth_no ?? 0),
      frontPriorityYn: row?.frontPriorityYn ?? row?.front_priority_yn ?? '',
      accessScore: row?.accessScore ?? row?.access_score ?? null,
      newSortSeq: row?.newSortSeq ?? row?.new_sort_seq ?? null,
      newLocationGrade: row?.newLocationGrade ?? row?.new_location_grade ?? '',
      primaryAccessPointId: row?.primaryAccessPointId ?? row?.primary_access_point_id ?? '',
      primaryAccessPointCode: row?.primaryAccessPointCode ?? row?.primary_access_point_code ?? '',
    };
  }

  /**
   * 미리보기 결과 normalize.
   */
  function normalizePreviewResult(payload: any): LocationGradePreviewState {
    return {
      areaCode: payload?.areaCode ?? payload?.area_code ?? '',
      purposeCode: payload?.purposeCode ?? payload?.purpose_code ?? '',
      totalCount: Number(payload?.totalCount ?? payload?.total_count ?? 0),
      previewCount: Number(payload?.previewCount ?? payload?.preview_count ?? 0),
      rows: resolveList(payload?.rows ?? []).map(normalizePreviewRow),
    };
  }

  /**
   * 실행 결과 normalize.
   */
  function normalizeExecuteResult(payload: any): LocationGradeResultState {
    return {
      areaCode: payload?.areaCode ?? payload?.area_code ?? '',
      purposeCode: payload?.purposeCode ?? payload?.purpose_code ?? '',
      targetLocationCount: Number(
        payload?.targetLocationCount ?? payload?.target_location_count ?? 0,
      ),
      updatedCount: Number(payload?.updatedCount ?? payload?.updated_count ?? 0),
      gradeACount: Number(payload?.gradeACount ?? payload?.grade_a_count ?? 0),
      gradeBCount: Number(payload?.gradeBCount ?? payload?.grade_b_count ?? 0),
      gradeCCount: Number(payload?.gradeCCount ?? payload?.grade_c_count ?? 0),
      gradeDCount: Number(payload?.gradeDCount ?? payload?.grade_d_count ?? 0),
      message: payload?.message ?? '',
    };
  }

  /**
   * Area 옵션 로딩.
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

      if (!filters.areaCode && areaOptions.value.length) {
        filters.areaCode = areaOptions.value[0].value;
      }
    } catch (error) {
      console.error(error);
      setFeedback('error', '아레아 옵션 조회 중 오류가 발생했습니다.');
    } finally {
      loading.options = false;
    }
  }

  /**
   * 요청값 검증.
   */
  function validateRequest(): boolean {
    if (!filters.areaCode.trim()) {
      setFeedback('warning', '아레아는 필수입니다.');
      return false;
    }

    if (!filters.purposeCode.trim()) {
      setFeedback('warning', '목적 코드는 필수입니다.');
      return false;
    }

    if (
      Number(filters.gradeARatio) <= 0 ||
      Number(filters.gradeBRatio) <= 0 ||
      Number(filters.gradeCRatio) <= 0
    ) {
      setFeedback('warning', '등급 비율은 0보다 커야 합니다.');
      return false;
    }

    if (
      Number(filters.gradeARatio) >= Number(filters.gradeBRatio) ||
      Number(filters.gradeBRatio) >= Number(filters.gradeCRatio) ||
      Number(filters.gradeCRatio) > 1
    ) {
      setFeedback('warning', '등급 비율은 A < B < C <= 1 기준이어야 합니다.');
      return false;
    }

    if (Number(filters.limit) <= 0) {
      setFeedback('warning', 'Preview Limit은 1 이상이어야 합니다.');
      return false;
    }

    return true;
  }

  /**
   * 미리보기 요청 payload 생성.
   */
  function buildPreviewRequestPayload() {
    return {
      areaCode: filters.areaCode.trim(),
      purposeCode: filters.purposeCode.trim(),
      gradeARatio: Number(filters.gradeARatio),
      gradeBRatio: Number(filters.gradeBRatio),
      gradeCRatio: Number(filters.gradeCRatio),
      limit: Number(filters.limit || DEFAULT_GRADE_RATIO.limit),
    };
  }

  /**
   * 실행 요청 payload 생성.
   *
   * execute DTO에는 limit이 필요 없으므로 제외한다.
   */
  function buildExecuteRequestPayload() {
    return {
      areaCode: filters.areaCode.trim(),
      purposeCode: filters.purposeCode.trim(),
      gradeARatio: Number(filters.gradeARatio),
      gradeBRatio: Number(filters.gradeBRatio),
      gradeCRatio: Number(filters.gradeCRatio),
    };
  }

  /**
   * 미리보기 실행.
   */
  async function preview() {
    if (!validateRequest()) {
      return;
    }

    loading.preview = true;

    try {
      const payload = await fetchLocationAccessPreview(buildPreviewRequestPayload());

      Object.assign(previewState, normalizePreviewResult(payload));
      clearResult();

      setFeedback('success', `미리보기 ${previewState.previewCount}건 조회되었습니다.`);
    } catch (error) {
      console.error(error);
      clearPreview();
      setFeedback('error', '로케이션 등급 미리보기 중 오류가 발생했습니다.');
    } finally {
      loading.preview = false;
    }
  }

  /**
   * 재산출 실행.
   */
  async function execute() {
    if (!validateRequest()) {
      return;
    }

    const confirmed = window.confirm(
      `선택한 기준으로 로케이션 접근성/등급을 재산출하시겠습니까?\n\nArea: ${filters.areaCode}\nPurpose: ${filters.purposeCode}`,
    );

    if (!confirmed) {
      return;
    }

    loading.execute = true;

    try {
      const payload = await executeLocationAccessRecalculate(buildExecuteRequestPayload());

      Object.assign(resultState, normalizeExecuteResult(payload));
      setFeedback('success', `재산출 완료: ${resultState.updatedCount}건 업데이트`);

      /*
       * 실행 후 현재 기준으로 다시 미리보기 조회.
       * DB 반영 결과를 화면에서 바로 확인하기 위한 처리.
       */
      await preview();
    } catch (error) {
      console.error(error);
      setFeedback('error', '로케이션 등급 재산출 중 오류가 발생했습니다.');
    } finally {
      loading.execute = false;
    }
  }

  /**
   * 조건 초기화.
   */
  function resetFilters() {
    Object.assign(filters, createInitialFilters());

    if (areaOptions.value.length) {
      filters.areaCode = areaOptions.value[0].value;
    }

    clearPreview();
    clearResult();
    clearFeedback();
  }

  function clearPreview() {
    Object.assign(previewState, createInitialPreviewState());
  }

  function clearResult() {
    Object.assign(resultState, createInitialResultState());
  }

  onMounted(async () => {
    await loadOptions();

    if (filters.areaCode) {
      await preview();
    }
  });

  return {
    filters,
    areaOptions,
    previewState,
    resultState,
    loading,
    feedback,
    loadOptions,
    preview,
    execute,
    resetFilters,
    clearPreview,
    clearResult,
  };
}
