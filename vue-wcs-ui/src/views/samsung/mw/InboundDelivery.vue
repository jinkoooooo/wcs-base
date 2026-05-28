<template>
  <CommonPage
    ref="commPageRef"
    :limit="limit"
    :fetchHandler="fetchHandler"
    @grid-fetched="handleGridCreated"
  >
    <ButtonGroup :buttonlist="buttonList" @btn-handler="btnHandler" />

    <ExcelUploadPopup
      @register="registerExcelUploadModal"
      :result="resultExcelRef"
      width="fit-content"
    />

    <div v-if="showDeliveryModal" class="custom-modal-overlay">
      <div class="custom-modal-content">
        <div class="modal-header">
          <h3>배송 시작 (Start Delivery)</h3>
        </div>

        <div class="modal-body">
          <div class="input-group">
            <label>B/L No</label>
            <input
              ref="blInputRef"
              v-model="deliveryForm.blNo"
              @keydown.enter="handleBlScan"
              placeholder="B/L No 스캔"
              autocomplete="off"
            />
          </div>

          <div class="input-group">
            <label>Container No</label>
            <input
              ref="cntrInputRef"
              v-model="deliveryForm.cntrNo"
              @keydown.enter="submitDelivery"
              placeholder="Container No 스캔"
              autocomplete="off"
            />
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-confirm" @click="submitDelivery" :disabled="isSubmitting">
            {{ isSubmitting ? '전송중...' : '확인' }}
          </button>
          <button class="btn-cancel" @click="closeDeliveryModal">취소</button>
        </div>
      </div>
    </div>
  </CommonPage>
</template>

<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { ref, computed, onMounted, reactive, nextTick } from 'vue';
  import CommonPage from '/src/views/common/CommonPage.vue';
  import { updateList, getSearchList, getCommonPostApi } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import { getQueryFilters, hasKeyWithFormat } from '/src/views/common/utils';
  import ButtonGroup from '/src/views/common/ButtonGroup.vue';
  import { decodeRmk } from '@/utils/metas/gridRmkMeta';
  import { useFetchStore } from '/@/store/modules/fetchStore';
  import ExcelUploadPopup from '/src/views/common/ExcelUploadPopup.vue';

  /**
   * global use ref
   */
  const { createConfirm, notification } = useMessage();
  const fetchStore = useFetchStore();

  /**
   * local ref
   */
  const commPageRef = ref(null as any);
  let limit = 100;
  let gridRef = computed(() => commPageRef.value?.grid);
  let getFormFields = computed(() => commPageRef.value?.getFormFields);
  let validate = computed(() => commPageRef.value?.formValidate);
  let resourceUrl = computed(() => commPageRef.value?.resourceUrl);
  let gridSaveUrl = computed(() => commPageRef.value?.gridSaveUrl);
  let buttonList = computed(() => commPageRef.value?.buttons);
  let columnMetaData = computed(() => commPageRef.value.columns);
  const resultExcelRef = ref();
  const [registerExcelUploadModal, { openModal: openExcelUploadPopup }] = useModal();

  // --- [Start Delivery 관련 상태] ---
  const showDeliveryModal = ref(false); // 팝업 표시 여부
  const isSubmitting = ref(false); // 전송 중 상태

  // DOM Refs (포커스 제어용)
  const blInputRef = ref<HTMLInputElement | null>(null);
  const cntrInputRef = ref<HTMLInputElement | null>(null);

  // 입력 데이터
  const deliveryForm = reactive({
    blNo: '',
    cntrNo: '',
  });

  /**life cycle */
  onMounted(() => {});

  // Grid Rmk : Code -> Description
  const handleGridCreated = (record) => {
    fetchStore.isUpdatingRows = true;
    gridRef.value.getData().forEach((row, rowIndex) => {
      decodeRmk(row, columnMetaData.value);
      gridRef.value.setRow(rowIndex, row);
    });
    fetchStore.isUpdatingRows = false;
  };

  const uploadExcel = () => {
    openExcelUploadPopup(true, {
      fileType: 'excels',
      sampleFile: 'TbMwInboundDelivery_Sample.xlsx',
      uploadUrl: '/tb_mw_inbound_delivery/excel_upload',
      columnSchema: [
        { field: 'lc_nm', index: 1, type: 'string' },
        { field: 'item_desc', index: 2, type: 'string' },
        { field: 'item_cbm', index: 3, type: 'number' },
        { field: 'total_cbm', index: 4, type: 'number' },
        { field: 'item_priority', index: 7, type: 'string' },
        { field: 'inbound_date', index: 8, type: 'date' },
        { field: 'lc_id', index: 9, type: 'string' },
        { field: 'bl_no', index: 10, type: 'string' },
        { field: 'cntr_no', index: 11, type: 'string' },
        { field: 'invoice', index: 12, type: 'string' },
        { field: 'item_type', index: 13, type: 'string' },
        { field: 'item_code', index: 14, type: 'string' },
        { field: 'item_qty', index: 15, type: 'number' },
      ],
    });
  };

  /**
   * Fetch Handler
   */
  async function fetchHandler(page: any, limit: any, sorters: any, searchProps: any) {
    await validate.value();
    const fields = getFormFields.value();

    hasKeyWithFormat(fields, 'Custom String', searchProps);

    const queryFilters = await getQueryFilters(fields, searchProps);
    let params = [
      { name: 'query', value: JSON.stringify(queryFilters) },
      { name: 'sort', value: JSON.stringify(sorters) },
      { name: 'page', value: page },
      { name: 'limit', value: limit },
    ];
    let requestParams = {};
    params.forEach((item) => {
      requestParams[item['name']] = item['value'];
    });
    const response = await getSearchList(resourceUrl.value, requestParams);

    for (const [key, value] of Object.entries(response.items)) {
      if (typeof value === 'object' && value !== null) {
        for (const [innerKey, innerValue] of Object.entries(value)) {
          if (typeof innerValue === 'number') {
            value[innerKey] = String(innerValue);
          }
        }
      }
    }
    if (Array.isArray(response)) {
      return { total: response.length, records: response };
    } else {
      return { total: response.total, records: response.items };
    }
  }

  const addRow = () => {
    gridRef.value?.addRow();
  };

  const saveRows = async () => {
    let patches = gridRef.value.getCURows();
    if (patches.length === 0)
      return notification.error({
        message: '에러',
        description: `수정 또는 추가된 데이터가 없습니다`,
        duration: 1,
      });
    let response = await updateList(gridSaveUrl.value, patches);
    if (response) {
      gridRef.value?.fetch();
    }
  };

  const deleteRows = () => {
    let selectedRows = gridRef.value.getCheckedRows();
    if (selectedRows.length === 0)
      return notification.error({
        message: '에러',
        description: `선택한 데이터가 없습니다`,
        duration: 1,
      });
    createConfirm({
      iconType: 'warning',
      title: () => '삭제',
      content: () => '선택한 데이터를 삭제하시겠습니까?',
      onOk: async () => {
        let selectedActualRows = gridRef.value.getSelectedRowsToDelete();
        deleteConfirm(selectedActualRows);
      },
    });
  };

  const deleteConfirm = async (selecteRows: any) => {
    let response = await updateList(gridSaveUrl.value, selecteRows);
    if (response) {
      gridRef.value.fetch();
    }
    return true;
  };

  // --- [Start Delivery 로직] ---

  // 1. 팝업 열기 (초기화 및 포커스)
  const openDeliveryModal = () => {
    deliveryForm.blNo = '';
    deliveryForm.cntrNo = '';
    showDeliveryModal.value = true;

    // DOM 렌더링 후 B/L 입력창 포커스
    nextTick(() => {
      blInputRef.value?.focus();
    });
  };

  // 2. 팝업 닫기
  const closeDeliveryModal = () => {
    showDeliveryModal.value = false;
  };

  // 3. B/L 입력 후 엔터 -> 컨테이너 포커스
  const handleBlScan = () => {
    if (deliveryForm.blNo) {
      cntrInputRef.value?.focus();
    }
  };

  // 4. 확인 버튼 / 컨테이너 엔터 -> 서버 전송
  const submitDelivery = async () => {
    // 입력값 검증
    if (!deliveryForm.blNo) {
      notification.error({ message: '입력 오류', description: 'B/L No를 입력해주세요.' });
      blInputRef.value?.focus();
      return;
    }
    if (!deliveryForm.cntrNo) {
      notification.error({ message: '입력 오류', description: 'Container No를 입력해주세요.' });
      cntrInputRef.value?.focus();
      return;
    }

    try {
      isSubmitting.value = true;

      // API 호출 URL (필요에 따라 수정)
      const url = '/tb_mw_inbound_delivery/start_delivery';
      const param = {
        bl_no: deliveryForm.blNo,
        cntr_no: deliveryForm.cntrNo,
      };

      // API 전송
      const result = await getCommonPostApi(url, param);
      if (result.code === 0) {
        notification.success({ message: '성공', description: result.message });

        // 그리드 재조회
        gridRef.value?.fetch();
      } else {
        notification.error({ message: '실패', description: result.message });
      }

      closeDeliveryModal();
    } catch (error) {
      console.error(error);
      notification.error({ message: '실패', description: '서버 전송 중 오류가 발생했습니다.' });
    } finally {
      isSubmitting.value = false;
    }
  };

  // 버튼 핸들러 연결
  async function btnHandler(param: any) {
    switch (param) {
      case 'addBtnHandler':
        addRow();
        break;
      case 'saveBtnHandler':
        saveRows();
        break;
      case 'deleteBtnHandler':
        deleteRows();
        break;
      case 'excelExportBtnHandler':
        excelExport();
        break;
      case 'excelImportBtnHandler':
        uploadExcel();
        break;
      case 'startDeliveryBtnHandler':
        openDeliveryModal();
        break;
      default:
        break;
    }
  }

  async function excelExport() {
    await commPageRef.value.downExcel();
  }
</script>

<style scoped>
  /* --- 커스텀 팝업 스타일 --- */

  /* 1. 배경 오버레이 (화면 전체 덮기) */
  .custom-modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5); /* 반투명 검정 */
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 9999; /* 다른 요소보다 위에 표시 */
  }

  /* 2. 팝업 본문 */
  .custom-modal-content {
    background-color: white;
    padding: 25px;
    border-radius: 8px;
    width: 450px;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  /* 헤더 */
  .modal-header h3 {
    margin: 0;
    font-size: 1.25rem;
    font-weight: bold;
    color: #333;
    border-bottom: 1px solid #eee;
    padding-bottom: 10px;
  }

  /* 입력 필드 영역 */
  .input-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .input-group label {
    font-weight: 600;
    color: #555;
  }

  .input-group input {
    padding: 10px;
    border: 1px solid #ccc;
    border-radius: 4px;
    font-size: 1rem;
    transition: border-color 0.2s;
  }

  .input-group input:focus {
    border-color: #007bff;
    outline: none;
    box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.2);
  }

  /* 푸터 (버튼 영역) */
  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 10px;
  }

  .modal-footer button {
    padding: 10px 20px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-weight: bold;
    font-size: 0.95rem;
  }

  .btn-confirm {
    background-color: #007bff;
    color: white;
  }

  .btn-confirm:hover:not(:disabled) {
    background-color: #0056b3;
  }

  .btn-confirm:disabled {
    background-color: #ccc;
    cursor: not-allowed;
  }

  .btn-cancel {
    background-color: #6c757d;
    color: white;
  }

  .btn-cancel:hover {
    background-color: #545b62;
  }
</style>
