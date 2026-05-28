import { computed, reactive, ref } from 'vue';
import {
  bulkUpsertItemMasters,
  changeItemActiveYn,
  createItemMaster,
  deleteItemMaster,
  fetchItemCategories,
  fetchItemMasterDetail,
  fetchItemMasters,
  updateItemMaster,
  type ItemMasterUpsertRequest,
} from '@/api/asrs/item';
import { useAsyncFlags } from '@/views/asrs/shared/composables/useAsyncFlags';
import { useFeedback } from '@/views/asrs/shared/composables/useFeedback';
import { normalizeCode, normalizeMessage } from '@/views/asrs/shared/utils/normalize';
import {
  calculateItemVolumeMm3,
  createEmptyItemMasterDetailForm,
  normalizeBulkErrorRows,
  normalizeItemCategoryOptions,
  normalizeItemMasterDetail,
  normalizeItemMasterRows,
  parseBulkPasteText,
} from '../mappers/itemMaster.mapper';
import type {
  ItemBulkPasteErrorRow,
  ItemBulkPasteRow,
  ItemCategoryOption,
  ItemMasterDetailForm,
  ItemMasterLoadingState,
  ItemMasterMode,
  ItemMasterRow,
  ItemMasterSearchForm,
} from '../types';

/**
 * 검색 폼 초기값 생성
 */
function createInitialSearchForm(): ItemMasterSearchForm {
  return {
    itemCode: '',
    itemName: '',
    categoryCode: '',
    storageTempType: '',
    activeYn: 'Y',
  };
}

/**
 * item-master 화면 전용 composable.
 *
 * 역할:
 * - 검색/상세/저장/삭제/사용여부 변경
 * - 신규/수정 모드 전환
 * - 상세 모달 상태 관리
 * - bulk paste 상태 관리
 */
export function useItemMaster() {
  /** 검색 폼 */
  const searchForm = reactive<ItemMasterSearchForm>(createInitialSearchForm());

  /** 좌측 목록 */
  const rows = ref<ItemMasterRow[]>([]);

  /** 현재 선택 row */
  const selectedRow = ref<ItemMasterRow | null>(null);

  /** 상세 편집 form */
  const detailForm = reactive<ItemMasterDetailForm>(createEmptyItemMasterDetailForm());

  /** 현재 화면 모드 */
  const mode = ref<ItemMasterMode>('create');

  /** 카테고리 옵션 */
  const categoryOptions = ref<ItemCategoryOption[]>([]);

  /** 상세 모달 표시 여부 */
  const detailModalOpen = ref(false);

  /** bulk modal 표시 여부 */
  const bulkModalOpen = ref(false);

  /** bulk 붙여넣기 원문 */
  const bulkPasteText = ref('');

  /** bulk preview rows */
  const bulkPreviewRows = ref<ItemBulkPasteRow[]>([]);

  /** bulk 저장 오류 rows */
  const bulkErrors = ref<ItemBulkPasteErrorRow[]>([]);

  /** loading 상태 */
  const { flags: loading } = useAsyncFlags<ItemMasterLoadingState>({
    search: false,
    detail: false,
    save: false,
    delete: false,
    categories: false,
    bulkSave: false,
  });

  /** feedback 상태 */
  const { feedback, setFeedback, clearFeedback } = useFeedback();

  /**
   * 우측(현재는 모달) 상세에서 표시할 volume 계산값
   *
   * 주의:
   * - 프론트 보조 표시용
   * - 최종 저장 값은 서버가 다시 계산
   */
  const computedVolumeMm3 = computed(() => calculateItemVolumeMm3(detailForm));

  /**
   * bulk preview 건수
   */
  const bulkPreviewCount = computed(() => bulkPreviewRows.value.length);

  /**
   * 상세 form 리셋
   */
  function resetDetailForm() {
    const empty = createEmptyItemMasterDetailForm();
    Object.keys(empty).forEach((key) => {
      (detailForm as any)[key] = (empty as any)[key];
    });
  }

  /**
   * 신규 생성 모달 열기
   */
  function openCreateModal() {
    mode.value = 'create';
    selectedRow.value = null;
    resetDetailForm();
    detailModalOpen.value = true;
    clearFeedback();
  }

  /**
   * 상세 모달 닫기
   */
  function closeDetailModal() {
    detailModalOpen.value = false;
  }

  /**
   * 상세 form을 저장 요청 DTO 형태로 변환
   */
  function buildUpsertRequest(): ItemMasterUpsertRequest {
    return {
      itemCode: detailForm.itemCode,
      itemName: detailForm.itemName,
      categoryCode: detailForm.categoryCode,
      operationProfileId: detailForm.operationProfileId || undefined,
      industryType: detailForm.industryType,
      baseUom: detailForm.baseUom,
      handlingUnitType: detailForm.handlingUnitType,
      outboundUnitType: detailForm.outboundUnitType,
      lengthMm: Number(detailForm.lengthMm || 0),
      widthMm: Number(detailForm.widthMm || 0),
      heightMm: Number(detailForm.heightMm || 0),
      weightG: Number(detailForm.weightG || 0),
      storageTempType: detailForm.storageTempType,
      lotControlYn: detailForm.lotControlYn,
      expiryControlYn: detailForm.expiryControlYn,
      serialControlYn: detailForm.serialControlYn,
      partialPickYn: detailForm.partialPickYn,
      mixedLoadYn: detailForm.mixedLoadYn,
      fragileYn: detailForm.fragileYn,
      heavyYn: detailForm.heavyYn,
      quarantineRequiredYn: detailForm.quarantineRequiredYn,
      allocationRuleCode: detailForm.allocationRuleCode,
      rotationProfileCode: detailForm.rotationProfileCode,
      storageGradeSeed: detailForm.storageGradeSeed,
      extAttr: detailForm.extAttr || undefined,
      activeYn: detailForm.activeYn,
    };
  }

  /**
   * 카테고리 옵션 조회
   */
  async function loadCategories() {
    loading.categories = true;

    try {
      const payload = await fetchItemCategories();
      categoryOptions.value = normalizeItemCategoryOptions(payload);
    } catch (error) {
      console.error(error);
      categoryOptions.value = [];
      setFeedback('error', '카테고리 목록을 불러오지 못했습니다.');
    } finally {
      loading.categories = false;
    }
  }

  /**
   * 상품 목록 조회
   */
  async function runSearch() {
    loading.search = true;

    try {
      const payload = await fetchItemMasters({
        itemCode: searchForm.itemCode || undefined,
        itemName: searchForm.itemName || undefined,
        categoryCode: searchForm.categoryCode || undefined,
        storageTempType: searchForm.storageTempType || undefined,
        activeYn: searchForm.activeYn || undefined,
      });

      rows.value = normalizeItemMasterRows(payload);

      if (!rows.value.length) {
        selectedRow.value = null;
        setFeedback('warning', '조회 결과가 없습니다.');
        return;
      }

      setFeedback('success', `${rows.value.length}건 조회되었습니다.`);
    } catch (error: any) {
      console.error(error);
      rows.value = [];

      const errorMsg = normalizeMessage(error?.response?.data);
      setFeedback('error', errorMsg || '상품 목록 조회 중 오류가 발생했습니다.');
    } finally {
      loading.search = false;
    }
  }

  /**
   * 상품 상세 조회
   */
  async function loadDetail(itemCode: string) {
    if (!itemCode) return;

    loading.detail = true;

    try {
      const payload = await fetchItemMasterDetail(itemCode);
      const normalized = normalizeItemMasterDetail(payload);

      Object.keys(normalized).forEach((key) => {
        (detailForm as any)[key] = (normalized as any)[key];
      });

      mode.value = 'edit';
      detailModalOpen.value = true;
      clearFeedback();
    } catch (error: any) {
      console.error(error);

      const errorMsg = normalizeMessage(error?.response?.data);
      setFeedback('error', errorMsg || '상품 상세 조회 중 오류가 발생했습니다.');
    } finally {
      loading.detail = false;
    }
  }

  /**
   * 목록 row 선택 -> 수정 모달 열기
   */
  async function selectRow(row: ItemMasterRow) {
    selectedRow.value = row;
    await loadDetail(row.itemCode);
  }

  /**
   * 저장 전 최소 검증
   *
   * 주의:
   * - 강한 검증은 서버에서도 반드시 수행
   * - 프론트는 사용자 편의용 1차 검증만 수행
   */
  function validateBeforeSave() {
    if (!detailForm.itemCode) {
      setFeedback('warning', 'Item Code를 입력해주세요.');
      return false;
    }

    if (!detailForm.itemName) {
      setFeedback('warning', 'Item Name을 입력해주세요.');
      return false;
    }

    if (!detailForm.categoryCode) {
      setFeedback('warning', 'Category를 선택해주세요.');
      return false;
    }

    if (!detailForm.industryType) {
      setFeedback('warning', 'Industry Type을 입력해주세요.');
      return false;
    }

    if (
      Number(detailForm.lengthMm || 0) <= 0 ||
      Number(detailForm.widthMm || 0) <= 0 ||
      Number(detailForm.heightMm || 0) <= 0 ||
      Number(detailForm.weightG || 0) <= 0
    ) {
      setFeedback('warning', '치수/중량은 1 이상이어야 합니다.');
      return false;
    }

    return true;
  }

  /**
   * 신규/수정 저장
   */
  async function saveItemMaster() {
    if (!validateBeforeSave()) return;

    loading.save = true;

    try {
      const request = buildUpsertRequest();

      if (mode.value === 'create') {
        await createItemMaster(request);
        setFeedback('success', '상품이 등록되었습니다.');
      } else {
        await updateItemMaster(detailForm.itemCode, request);
        setFeedback('success', '상품이 수정되었습니다.');
      }

      await runSearch();

      if (detailForm.itemCode) {
        await loadDetail(detailForm.itemCode);
      }
    } catch (error: any) {
      console.error(error);

      const errorCode = normalizeCode(error?.response?.data);
      const errorMsg = normalizeMessage(error?.response?.data);

      if (errorCode === 'DUPLICATE_DATA') {
        setFeedback('warning', errorMsg || '중복된 상품코드입니다.');
        return;
      }

      setFeedback('error', errorMsg || '상품 저장 중 오류가 발생했습니다.');
    } finally {
      loading.save = false;
    }
  }

  /**
   * 상품 삭제
   */
  async function removeItemMaster() {
    if (!detailForm.itemCode || mode.value !== 'edit') {
      setFeedback('warning', '삭제할 상품을 먼저 선택해주세요.');
      return;
    }

    loading.delete = true;

    try {
      await deleteItemMaster(detailForm.itemCode);
      setFeedback('success', '상품이 삭제되었습니다.');

      await runSearch();
      closeDetailModal();
      resetDetailForm();
      selectedRow.value = null;
      mode.value = 'create';
    } catch (error: any) {
      console.error(error);

      const errorMsg = normalizeMessage(error?.response?.data);
      setFeedback('error', errorMsg || '상품 삭제 중 오류가 발생했습니다.');
    } finally {
      loading.delete = false;
    }
  }

  /**
   * 사용 여부 토글
   */
  async function toggleActiveYn() {
    if (!detailForm.itemCode || mode.value !== 'edit') {
      setFeedback('warning', '변경할 상품을 먼저 선택해주세요.');
      return;
    }

    loading.save = true;

    try {
      const nextActiveYn = detailForm.activeYn === 'Y' ? 'N' : 'Y';

      await changeItemActiveYn(detailForm.itemCode, {
        activeYn: nextActiveYn,
      });

      detailForm.activeYn = nextActiveYn;
      setFeedback('success', '사용 여부가 변경되었습니다.');
      await runSearch();
    } catch (error: any) {
      console.error(error);

      const errorMsg = normalizeMessage(error?.response?.data);
      setFeedback('error', errorMsg || '사용 여부 변경 중 오류가 발생했습니다.');
    } finally {
      loading.save = false;
    }
  }

  /**
   * bulk modal 열기
   */
  function openBulkModal() {
    bulkModalOpen.value = true;
    bulkPasteText.value = '';
    bulkPreviewRows.value = [];
    bulkErrors.value = [];
  }

  /**
   * bulk modal 닫기
   */
  function closeBulkModal() {
    bulkModalOpen.value = false;
  }

  /**
   * 붙여넣기 텍스트를 preview rows 로 파싱
   */
  function parseBulkText() {
    bulkPreviewRows.value = parseBulkPasteText(bulkPasteText.value);
    bulkErrors.value = [];
  }

  /**
   * bulk 저장
   */
  async function saveBulkRows() {
    if (!bulkPreviewRows.value.length) {
      setFeedback('warning', '붙여넣기 데이터가 없습니다.');
      return;
    }

    loading.bulkSave = true;

    try {
      const payload = await bulkUpsertItemMasters({
        rows: bulkPreviewRows.value.map((row) => ({
          itemCode: row.itemCode,
          itemName: row.itemName,
          categoryCode: row.categoryCode,
          operationProfileId: row.operationProfileId || undefined,
          industryType: row.industryType,
          baseUom: row.baseUom,
          handlingUnitType: row.handlingUnitType,
          outboundUnitType: row.outboundUnitType,
          lengthMm: Number(row.lengthMm || 0),
          widthMm: Number(row.widthMm || 0),
          heightMm: Number(row.heightMm || 0),
          weightG: Number(row.weightG || 0),
          storageTempType: row.storageTempType,
          lotControlYn: row.lotControlYn,
          expiryControlYn: row.expiryControlYn,
          serialControlYn: row.serialControlYn,
          partialPickYn: row.partialPickYn,
          mixedLoadYn: row.mixedLoadYn,
          fragileYn: row.fragileYn,
          heavyYn: row.heavyYn,
          quarantineRequiredYn: row.quarantineRequiredYn,
          allocationRuleCode: row.allocationRuleCode,
          rotationProfileCode: row.rotationProfileCode,
          storageGradeSeed: row.storageGradeSeed,
          activeYn: row.activeYn,
          extAttr: row.extAttr || undefined,
        })),
      });

      bulkErrors.value = normalizeBulkErrorRows(payload);

      if (!bulkErrors.value.length) {
        setFeedback('success', '일괄 저장이 완료되었습니다.');
        closeBulkModal();
        await runSearch();
        return;
      }

      setFeedback('warning', '일부 행 저장에 실패했습니다. 오류 목록을 확인해주세요.');
      await runSearch();
    } catch (error: any) {
      console.error(error);

      const errorMsg = normalizeMessage(error?.response?.data);
      setFeedback('error', errorMsg || '일괄 저장 중 오류가 발생했습니다.');
    } finally {
      loading.bulkSave = false;
    }
  }

  /**
   * 초기 진입 처리
   */
  async function initialize() {
    await loadCategories();
    resetDetailForm();
    await runSearch();
  }

  return {
    searchForm,
    rows,
    selectedRow,
    detailForm,
    mode,
    categoryOptions,
    detailModalOpen,
    bulkModalOpen,
    bulkPasteText,
    bulkPreviewRows,
    bulkErrors,
    loading,
    feedback,
    computedVolumeMm3,
    bulkPreviewCount,
    openCreateModal,
    closeDetailModal,
    loadCategories,
    runSearch,
    loadDetail,
    selectRow,
    saveItemMaster,
    removeItemMaster,
    toggleActiveYn,
    openBulkModal,
    closeBulkModal,
    parseBulkText,
    saveBulkRows,
    initialize,
  };
}
