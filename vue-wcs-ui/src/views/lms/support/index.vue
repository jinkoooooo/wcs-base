<template>
  <div class="h-full">
    <!-- 1. 요청/답변 상세 모달 -->
    <RequestModal @register="registerRequestModal" :schemas="schemas" :isAdmin="isAdmin"
                  :isAdminPage="isAdminPage"
                  @success="handleSuccess"></RequestModal>

    <!-- 2. 그리드 & 버튼 & 상단 설명 -->
    <CommonPage ref="pageRef" :limit="limit" :fetchHandler="masterFetchHandler"
                @gridFetched="handleGridFetched"
                @gridClicked="handleGridClick" @gridDbClicked="handleGridDbClick">
      <!-- 그리드 버튼 -->
      <ButtonGroup v-if="buttonList && buttonList.length > 0"
                   :buttonlist="buttonList"
                   @btnHandler="btnHandler" />
      <!-- 상단 설명 -->
      <template #customDesc>
        <div class="relative rounded-xl border border-slate-200 bg-white shadow-sm mb-2">
          <div
            class="absolute inset-y-0 left-0 w-1 bg-gradient-to-b from-sky-400 to-indigo-500"
          ></div>
          <div class="px-4 py-3">
            <div class="mb-2 text-sm font-semibold text-slate-800">{{ t("text.maintenance_desc_title")}}</div>

            <div class="flex items-center gap-3 text-slate-700">
                  <span
                    class="inline-flex h-6 w-6 items-center justify-center rounded-full border border-slate-300"
                  >1</span
                  >
              <span>{{t("text.request_submitted")}}</span>
              <span class="h-px flex-1 bg-slate-200"></span>

              <span
                class="inline-flex h-6 w-6 items-center justify-center rounded-full border border-slate-300"
              >2</span
              >
              <span>{{t("text.assignee_assigned")}}</span>
              <span class="h-px flex-1 bg-slate-200"></span>

              <span
                class="inline-flex h-6 w-6 items-center justify-center rounded-full border border-slate-300"
              >3</span
              >
              <span>{{t("text.in_progress")}}</span>
              <span class="h-px flex-1 bg-slate-200"></span>

              <span
                class="inline-flex h-6 w-6 items-center justify-center rounded-full border border-slate-300"
              >4</span
              >
              <span>{{t("text.completed")}}</span>
            </div>

            <ul class="mt-3 list-disc pl-5 leading-relaxed text-slate-600">
              <li>{{t("text.maintenance_desc_content_1")}}</li>
              <li>{{t("text.maintenance_desc_content_2")}}</li>
              <!--        - “등록” 상태인 경우에는 작성자 본인만 취소할 수 있습니다.-->
              <!--        - “담당자 지정” 이후 상태에서는 작성자 본인이더라도 취소할 수 없으며,-->
              <!--        완료 요청을 통해 취소/완료 사유를 입력하면 담당자가 확인 후 “완료”로 변경 가능합니다.-->
            </ul>
          </div>
        </div>
      </template>
    </CommonPage>
  </div>

</template>

<script lang="ts" setup>
  /**
   * Note
   * -. 검색 필터링 : 센터목록 select 방식으로 변경 검토
   * -. 검색 필터링 : 나의 등록 건만 보기 추가 검토
   * -. 스케쥴러로 소프트삭제 데이터 정리 로직 추가 검토
   * -. 복제 기능 추가 검토
   */
  import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';

  import { useModal } from "@/components/Modal";
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useUserStore } from "@/store/modules/user";
  import { useMessage } from '/@/hooks/web/useMessage';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useFetchStore } from '/@/store/modules/fetchStore';
  import { useRefCodes } from "@/views/lms/composables/useRefCodes";
  import { getFormSchemas } from "@/views/lms/support/data";
  import { getQueryFilters, hasKeyWithFormat } from '@/views/common/utils';
  import { getSearchList, updateList, } from '/@/api/common/api';

  import RequestModal from "@/views/lms/support/components/RequestModal.vue";
  import CommonPage from "@/views/common/CommonPage.vue";
  import ButtonGroup from "@/views/common/ButtonGroup.vue";

  const { t } = useI18n();
  const { createConfirm, notification } = useMessage();
  const [registerRequestModal, { openModal }] = useModal();
  const fetchStore = useFetchStore();
  const userStore = useUserStore();

  // 사용자 권한
  const { hasPermission } = usePermission();
  const isAdmin = computed(() => hasPermission('admin'));
  const currentUser = computed(() => userStore.getUserInfo?.userId || '')
  // 라우팅 기반 관리자 페이지 구분
  const isAdminPage = computed(() => {
    const routingVal = pageRef.value?.metaRouting;
    return routingVal?.endsWith('admin') ?? false;
  })

  // 그리드 옵션 및 제어
  const record = ref<any>(null);
  const pageRef = ref();
  const limit = 100;
  const gridRef = computed(() => pageRef.value?.grid);
  const gridSaveUrl = computed(() => pageRef.value?.gridSaveUrl);
  const buttonList = computed(() => pageRef.value?.buttons);
  const resourceUrl = computed(() => pageRef.value?.resourceUrl);
  const searchFormValidate = computed(() => pageRef.value?.formValidate);
  const getSearchFormFields = computed(() => pageRef.value?.getFormFields)

  // 선택 목록 및 요청 폼 메타데이터
  const {
    categoryOptions, statusOptions, centerOptions, assigneeOptions, alarmOptions, equipOptions,
    loadCommonCodes, loadCenterOptions, loadAssignerOptions, loadAlarms, loadEquips, clearOptions
  } = useRefCodes(!isAdminPage.value);
  const schemas = computed(() => getFormSchemas(t, {
    center: centerOptions.value || [],
    assignee: assigneeOptions.value || [],
    category: categoryOptions.value || [],
    status: statusOptions.value || [],
    alarm: alarmOptions.value || [],
    equip: equipOptions.value || [],
  }, isAdminPage.value, String(currentUser.value)))

  /**
   * 데이터 API
   */
  /**
   * master 그리드 조회
   * 1. 검색 폼 유효성 검증 및 쿼리 생성
   * 2. 사용자 소속 센터 조건 추가
   */
  async function masterFetchHandler(
    page,
    limit,
    sorters = [{ field: 'lc_id', ascending: true }, { field: 'is_deleted', ascending: true }, { field: 'created_at', ascending: false }],
    searchProps,
  ) {
    await searchFormValidate.value();
    if (!centerOptions.value?.length) return { total: 0, records: [] };

    const fields = getSearchFormFields.value();
    hasKeyWithFormat(fields, 'Custom string', searchProps);

    let queryFilters = [];
    if (searchProps) {
      queryFilters = await getQueryFilters(fields, searchProps);
    }

    // 사용자에게 할당된 센터 목록 필터링
    const userCenterIds = centerOptions.value.map((option) => option.value);
    let filterCols = [];
    if (!isAdminPage.value) {
      filterCols.push({ name: 'is_deleted', operator: 'is_not_true' });
      filterCols.push({ name: 'lc_id', operator: 'in',value: userCenterIds.join(','), relation: false,})
    }

    const mergedQueryFilter = [...filterCols, ...queryFilters];

    const requestParams = {
      // query: JSON.stringify(filterCols),
      query: JSON.stringify(mergedQueryFilter),
      sort: JSON.stringify(sorters),
      page,
      limit,
    };

    const url = pageRef.value?.resourceUrl;

    try {
      const response = await getSearchList(url, requestParams);
      if (Array.isArray(response)) {
        return {
          total: response.length,
          records: response,
        };
      } else {
        return {
          total: response.total,
          records: response.items,
        };
      }
    } catch (e) {
      // console.warn('[masterFetchHandler] e =', e);
    }
  }

  // 그리드 조회 이후 로직
  function handleGridFetched() {
    fetchStore.isUpdatingRows = false;
  }

  /**
   * master 그리드 저장
   * 1. 카테고리 유효성 검증
   * 2. cud_flag_ 추가
   * 3. 상태 기본값 추가 (저장:'2')
   */
  async function handleUpdateRows() {
    const patches = pageRef.value.grid?.getCURows() || [];

    const INVALID_CATEGORIES = new Set(['', '-', '0']); // 선택 불가 카테고리
    const invalidCategoryRecords: any[] = [];
    const payload: any[] = [];

    for (const record of patches) {
      const category = String(record.category ?? '').trim();
      if (INVALID_CATEGORIES.has(category)) {
        invalidCategoryRecords.push(record.support_id);
        continue; // 제외
      }

      record.cud_flag_ = record.support_id ? 'u' : 'c';

      if (record.status == null || record.status === '') record.status = '2';

      payload.push(record);
    }

    // 검증 경고
    if (invalidCategoryRecords.length) {
      notification.error({
        message: '카테고리 오류',
        description: `유효하지 않은 카테고리 데이터는 제외되었습니다. \n대상: ${ invalidCategoryRecords.join(
          ', ',
        ) }`,
        duration: 2,
      });
    }

    if (!payload.length) {
      return notification.warning({
        message: 'INFO',
        description: '수정 또는 추가된 데이터가 없습니다',
        duration: 2,
      });
    }

    try {
      const response = await updateList(pageRef.value?.gridSaveUrl, patches);
      if (response) {
        // grid 전체 새로고침
        await pageRef.value.grid?.fetch();
        await pageRef.value?.detailRef?.resetFields?.();
      }
    } catch (e) {
      // console.warn('[handleUpdateRows] e =', e);
      notification.error({
        message: 'ERROR',
        description: e?.response?.data?.code ?? '',
      });
    }
  }

  // master 그리드 삭제 - 소프트삭제 (is_deleted = true)
  async function handleSoftDeleteRows() {
    const patches = pageRef.value.grid?.getCheckedRows();

    if (!patches.length) {
      return notification.error({
        message: 'INFO',
        description: '선택된 항목이 없습니다',
        duration: 2,
      });
    }

    createConfirm({
      iconType: 'warning',
      title: () => '삭제',
      content: () => '선택된 항목을 삭제하시겠습니까?',
      onOk: async () => {
        try {
          patches.forEach((el) => {
            el.cud_flag_ = 'u';
            el.deleted = true;
            el.is_deleted = true;
          });
          const response = await updateList(pageRef.value?.gridSaveUrl, patches);
          if (response) {
            notification.success({
              message: '삭제 완료',
              description: `${ patches.length }건이 삭제 처리되었습니다.`
            })
            await pageRef.value.grid?.fetch();
            await pageRef.value.detailRef?.resetFields();
            return Promise.resolve();
          }
        } catch (e) {
          // console.error('[handleSoftDeleteRows] e =', e);
          notification.error({ message: '삭제 실패', description: '데이터 처리 중 오류가 발생했습니다.' })
          return Promise.reject();
        }
      },
    });
  }

  // master 그리드 삭제 - 하드 삭제
  async function handleHardDeleteRows() {
    const patches = pageRef.value.grid?.getCheckedRows();

    if (!patches.length) {
      return notification.error({
        message: 'INFO',
        description: '선택된 항목이 없습니다',
        duration: 2,
      });
    }

    createConfirm({
      iconType: 'warning',
      title: () => '영구 삭제 (복구 불가)',
      content: () => `선택된 ${ patches.length }건을 DB에서 완전히 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.`,
      onOk: async () => {
        try {
          patches.forEach((el) => {
            el.cud_flag_ = 'd';
          });
          const response = await updateList(pageRef.value?.gridSaveUrl, patches);
          if (response) {
            notification.success({
              message: '영구 삭제 완료',
              description: `${ patches.length }건이 삭제 처리되었습니다.`
            })
            await pageRef.value.grid?.fetch();
            await pageRef.value.detailRef?.resetFields();
            return Promise.resolve();
          }
        } catch (e) {
          // console.warn('[handleHardDeleteRows] e =', e);
          notification.error({ message: '영구 삭제 실패', description: '데이터 처리 중 오류가 발생했습니다.' })
          return Promise.reject();
        }
      },
    });
  }

  /**
   * 버튼 및 이벤트 핸들러
   * - 추가: 모달 오픈
   * - 저장: 선택한 그리드 목록 저장
   * - 삭제: 소프트삭제
   * - 영구삭제: 하드삭제
   */

  // 버튼 핸들러 매핑
  async function btnHandler(listenerName: any) {

    const handlers = {
      'addBtnHandler': handleOpenModal,
      'saveBtnHandler': handleUpdateRows,
      'deleteBtnHandler': handleSoftDeleteRows,
      'deletePermanentlyBtnHandler': handleHardDeleteRows,
    }

    const targetHandler = handlers[listenerName];
    if (targetHandler) {
      targetHandler()
    }
  }

  // 모달 작업 성공 후 레이아웃 갱신
  function handleSuccess() {
    reloadGridLayout();
  }


  // 그리드 클릭 시 모달 오픈 이벤트 핸들러
  // - 사용자 페이지 : 모달 오플
  // - 관리자 페이지 : 그리드 '상태', '카테고리' 필드 제외한 영역 클릭 시 모달 오픈
  function handleGridClick(record: any) {
    if (record.columnName == "") return;
    if (isAdminPage.value && ['status', 'category', '_checked'].includes(record.columnName)) return;
    openModal(true, {
      record, isUpdate: true, saveUrl: gridSaveUrl.value
    })
  }

  // 그리드 '상태', '카테고리' 필드 제외한 영역 더블클릭 시 모달 오픈
  function handleGridDbClick(record: any) {
    handleGridClick(record)
  }

  // '추가' 버튼 클릭 시 모달 오픈
  function handleOpenModal() {
    openModal(true, {
      record: {},
      isUpdate: false,
      saveUrl: gridSaveUrl,
    })
  }

  /**
   * 스타일 변경
   */
  // 요청 폼 임시저장/저장 이후 master layout 재계산
  async function reloadGridLayout() {
    if (!pageRef.value) return;
    await pageRef.value?.grid?.fetch();

    await nextTick();
    setTimeout(() => {
      pageRef.value?.grid?.refreshLayout?.();
    }, 150);
  }

  /**
   * 라이프사이클
   */
  // Select 옵션, 그리드 조회
  onMounted(async () => {
    await nextTick();

    clearOptions();
    await Promise.allSettled([loadCenterOptions(), loadCommonCodes(), loadAssignerOptions(), loadAlarms(), loadEquips()]);

    if (pageRef.value?.grid) {
      await pageRef.value?.grid?.fetch();
    }
  });

  // Select 옵션 초기화
  onUnmounted(() => {
    clearOptions();
  })

</script>
<style scoped>
  :deep(#form_item_created_at) {
    text-align: right;
    margin-right: 4px;
  }

  :deep(.ant-input-suffix) {
    color: rgba(55, 57, 55, 0.54);
    font-size: clamp(12px, 0.8vw, 14px);
  }

  /* 섹션 Divider 라벨 크기 업 (Vben Divider의 텍스트) */
  :deep(.ant-divider-inner-text) {
    font-size: clamp(13px, 1vw, 16px);
    font-weight: 600;
    color: #334155; /* slate-700 */
  }

  /* 추가: 반응형 타이포 (폰트만) */
  .page-title {
    font-size: clamp(18px, 1.6vw, 22px);
  }

  .page-subtitle {
    font-size: clamp(12px, 1vw, 14px);
  }

  button {
    cursor: pointer;
  }

  /* 삭제된 필드 스타일 */
  :deep(.row-deleted) {
    background-color: rgba(225, 225, 225, 0.59);
    color: #999;
    text-decoration: line-through; /* 취소선 */
  }
</style>
