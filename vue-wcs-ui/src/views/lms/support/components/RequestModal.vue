<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="isUpdate ? '유지보수 요청 상세' : '신규 유지보수 요청'"
    width="800px"
    :minHeight="400"
    :showCancelBtn="false"
    :showOkBtn="false"
    :loading="isLoading"
  >

    <div class="p-4">
      <!-- 요청 폼 -->
      <BasicForm ref="reqFormRef" @register="registerForm"
                 :submitButtonOptions="submitButtonOptions"
                 @submit="handleSubmit"
      >

        <!-- 유지보수 요청 폼 : support_id, completed_at, requester_id 필드 -->
        <template #etcSlot="{model, field}">
          <div
            class="inline-flex max-w-full items-center rounded-md border border-slate-200 bg-slate-50 px-2 py-1 text-slate-700 text-[clamp(12px,0.95vw,14px)]"
            :title="model[field]"
          >
            <span class="truncate">{{ model[field] ? model[field] : DEFAULT_VALUE_STR }}</span>
          </div>
        </template>

        <!-- 유지보수 요청 폼 : 생성일시 필드 -->
        <template #createdAtSlot="{ model, field }">
          <Input
            v-show="model[field]"
            :value="formatCreatedCompleted(model)"
            readonly
            :bordered="false"
            class="w-full text-right"
            style="color: gray"
          />
        </template>

        <!-- 유지보수 요청 폼 : 상태 필드 -->
        <template #statusSlot="{model, field}">
          <Select
            v-if="isAdminPage && model[field]"
            v-model:value="model[field]"
            :options="statusOptions?.filter((el) => el.label !== '-')"
            :placeholder="DEFAULT_VALUE_STR"
            :fieldNames="{label: 'label', value: 'value'}"
            :getPopupContainer="trigger => trigger.parentElement"
          />
          <div
            v-else
            class="inline-flex items-center rounded-md border border-slate-200 bg-slate-50 px-2 py-1 text-slate-700 text-[clamp(12px,0.95vw,14px)]"
          >
            <span>{{ getOptionValueToLabel(statusOptions, model[field]) }}</span>
          </div>
        </template>

        <!-- 유지보수 요청 폼 : 담당자 필드 (native input으로 커서 위치 유지, 뒤로 이동하는 것 방지) -->
        <template #assigneeSlot="{model, field, values}">
          <input
            class="ant-input"
            :value="model[field] || ''"
            @input="(e) => model[field] = (e.target as HTMLInputElement).value"
            placeholder="담당자를 작성해 주세요 (100자 이내)"
            maxlength="100"
            :disabled="getSlotDisabled(values)"
            style="width: 100%;"
          />
        </template>

        <!-- 유지보수 요청 폼 : 제목 필드 (native input으로 커서 위치 유지, 뒤로 이동하는 것 방지) -->
        <template #titleSlot="{model, field, values}">
          <input
            class="ant-input"
            :value="model[field] || ''"
            @input="(e) => model[field] = (e.target as HTMLInputElement).value"
            placeholder="요청에 대한 주요내용을 작성해 주세요 (200자 이내)"
            maxlength="200"
            :disabled="getSlotDisabled(values)"
            style="width: 100%;"
          />
        </template>

        <!-- 유지보수 요청 폼 : 내용 필드 (native textarea로 커서 위치 유지, 뒤로 이동하는 것 방지) -->
        <template #contentSlot="{model, field, values}">
          <textarea
            class="ant-input"
            :value="model[field] || ''"
            @input="(e) => model[field] = (e.target as HTMLTextAreaElement).value"
            placeholder="요청에 대한 상세내용을 작성해주세요 (2000자 이내)"
            rows="10"
            maxlength="2000"
            :disabled="getSlotDisabled(values)"
            style="width: 100%; resize: vertical;"
          ></textarea>
        </template>

        <!-- 유지보수 요청 폼 : 첨부파일 필드 -->
        <template #attachmentSlot="{model, field}">
          <div class="rounded-lg border border-slate-200 bg-slate-50/50 p-4">
            <div class="mb-3 ml-1 flex items-center justify-between">
              <span class="text-sm text-slate-400">허용 확장자: jpg, jpeg, png, gif, pdf, txt, doc, docx, xls, xlsx, ppt, pptx</span>
              <span class="text-sm text-slate-400">최대 파일 크기: 50MB</span>
            </div>
            <div class="flex flex-col gap-3">
              <label for="file-input"
                     @dragover.prevent="isDragging = true"
                     @dragleave.prevent="isDragging = false"
                     @drop.prevent="handleFileDrop"
                     class="group flex cursor-pointer items-center justify-center gap-2 rounded-md border border-dashed bg-white py-4 transition-all"
                     :class="isDragging ? 'border-sky-500 bg-sky-50 text-sky-600' : 'border-slate-300 bg-white text-slate-600 hover:border-sky-400 hover:bg-sky-50 hover:text-sky-500'"
              >
                <UploadOutlined />
                <span class="text-sm">파일을 선택하거나 여기로 드래그하세요</span>
                <input
                  type="file"
                  id="file-input"
                  multiple
                  hidden
                  @change="handleFileChange"
                  accept=".jpg,.jpeg,.png,.gif,.txt,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx"
                />
              </label>

              <ul v-if="selectedFiles.length > 0" class="m-0 p-0 space-y-2 list-none">

                <li
                  v-for="(file, idx) in selectedFiles"
                  :key="idx"
                  class="flex items-center justify-between rounded-lg bg-white px-4 py-2.5 shadow-sm border border-slate-200 hover:border-sky-200 transition-colors"
                >
                  <div class="flex items-center gap-3 overflow-hidden flex-1 min-w-0">
                    <PaperClipOutlined class="text-sky-500 flex-shrink-0" />

                    <div class="flex flex-col flex-1 min-w-0 whitespace-nowrap">
                      <div class="flex items-center space-x-2">
                        <template v-if="file.isServerFile">
                          <!-- 로컬 개발용 -->
                          <!-- <a :href="`/rest/support-attachment/download/${file.id}`">{{ file.name }}</a> -->
                          <!-- <a :href="`http://localhost/rest/support-attachment/download/${file.id}`">{{ file.name }}</a>-->

                          <!-- 배포 용 -->
                          <a :href="`/rest/support-attachment/download/${file.id}`"
                             class="truncate text-sm font-semibold text-slate-700 hover:text-sky-600 hover:underline decoration-2 underline-offset-4 transition-all">
                            {{ file.name }}
                          </a>
                          <!-- <a :href="`/rest/support-attachment/download/${file.id}`" download>{{ file.name }}</a> -->
                        </template>

                        <span v-else
                              class="truncate text-sm font-semibold text-slate-700 italic">{{
                            file.name
                          }}</span>
                      </div>

                      <span class="text-xs text-slate-400 flex-shrink-0">
                       {{ formatSize(file.size) }}
                      </span>
                    </div>
                  </div>

                  <Button
                    v-if="showFileDeleteBtn"
                    type="link" size="small"
                    class="flex items-center justify-center hover:bg-red-50"
                    @click.stop="removeFile(idx)" danger>
                    <template #icon>
                      <DeleteOutlined />
                    </template>
                  </Button>

                </li>
              </ul>
            </div>
          </div>
        </template>

        <!-- 유지보수 요청 폼 : 센터 필드 -->
        <template #lcIdSlot="{model, field}">
          <Select
            v-model:value="model[field]"
            :options="centerOptions"
            placeholder="센터를 선택해주세요"
            :disabled="isLcIdSlotDisabled"
            showSearch
            optionFilterProp="label"
            :allowClear="false"
            style="width: 100%"
            :getPopupContainer="trigger => trigger.parentElement"
            @change="() => { model.equip_id = null; model.alarm_id = null; }"
          />
        </template>

        <!-- 유지보수 요청 폼 : 설비 필드 (lc_id 선택 시 해당 센터의 설비만 표시) -->
        <template #equipSlot="{model, field}">
          <Select
            v-model:value="model[field]"
            :options="getFilteredEquipOptions(model.lc_id)"
            :placeholder="model.lc_id ? '연관 설비를 선택해주세요' : '센터를 먼저 선택해주세요'"
            :disabled="isEquipSlotDisabled"
            showSearch
            optionFilterProp="label"
            allowClear
            style="width: 100%"
            :getPopupContainer="trigger => trigger.parentElement"
            @change="() => { model.alarm_id = null; }"
          />
        </template>

        <!-- 유지보수 요청 폼 : 알람코드 필드 (lc_id·equip_id 선택 시 해당 설비의 알람만 표시) -->
        <template #alarmSlot="{model, field}">
          <Select
            v-model:value="model[field]"
            :options="getFilteredAlarmOptions(model.equip_id, model.lc_id)"
            :placeholder="model.lc_id ? '연관 알람을 선택해주세요' : '센터를 먼저 선택해주세요'"
            :disabled="isAlarmSlotDisabled"
            showSearch
            optionFilterProp="label"
            allowClear
            style="width: 100%"
            :fieldNames="{label: 'label', value: 'value'}"
            :getPopupContainer="trigger => trigger.parentElement"
          >
            <template #option="item">
              <span class="block truncate" :title="item.label">{{ item.label }}</span>
            </template>
          </Select>
        </template>

        <!-- 유지보수 요청 임시저장 버튼 -->
        <template #submitBefore>
          <Button v-if="showTempSubmitBtn" @click="handleTempSubmit" :loading="isSubmitting"
                  class="mr-2">임시저장
          </Button>
        </template>
      </BasicForm>

      <!-- 답변 폼 -->
      <template v-if="isUpdate">

        <Divider orientation="left" class="!mb-4">답변 관리</Divider>

        <div
          style="background-color: #f8fafc; padding:20px; border-radius:8px; border: 1px solid #e2e8f0; margin-top: 10px;">
          <ResponsePanel v-if="isUpdate" ref="resFormRef" :data="requestFormData"
                         :permissions="{canWrite: isAdminPage, canDelete: isAdminPage}"
                         :saveUrl=RESPONSE_SAVE_URL
                         :readUrl="RESPONSE_READ_URL"
                         @submitted="handleRespAction"
                         @completed="handleCompleted"
          ></ResponsePanel>
        </div>
      </template>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  /**
   * Note
   * - 기존에 작성된 요청에 한해서 관리자만 상태 변경 가능하되 임시저장, 알 수 없음 상태는 지정 불가
   * - 개발/배포 환경별 파일 다운로드 링크 상이 (line 99)
   * - 센터 선택 후에 연관설비, 연관 알림 선택 필터링 조회
   * - 연관 알람, 연관 설비, 센터코드 테이블의 lc_id 미일치로 lc_id 하드코딩으로 연결: lcIdMatchingMap
   */
  import { computed, ref } from 'vue';
  import { Button, Divider, Input, Select } from "ant-design-vue";
  import { DeleteOutlined, PaperClipOutlined, UploadOutlined } from '@ant-design/icons-vue';

  import { BasicModal, useModalInner } from "@/components/Modal";
  import { BasicForm, FormSchema, useForm } from "@/components/Form/index";
  import ResponsePanel from "@/views/lms/support/components/ResponsePanel.vue";
  import dayjs from "dayjs";

  import { useI18n } from "@/hooks/web/useI18n";
  import { useMessage } from "@/hooks/web/useMessage";
  import { useRefCodes } from "@/views/lms/composables/useRefCodes";
  import { useSupportFile } from "@/views/lms/support/composables/useSupportFile";
  import { useSupportFormActions } from "@/views/lms/support/composables/useSupportFormActions";
  import { getCommonGetApi } from '/@/api/common/api';
  import { OptionType } from "@/views/lms/support/types";
  import { useUserStore } from "@/store/modules/user";
  import { EDITABLE_STATUSES } from "@/views/lms/support/data";

  const props = defineProps({
    schemas: { type: Array as PropType<FormSchema[]>, default: () => [] },
    isAdmin: { type: Boolean, default: false },
    isAdminPage: { type: Boolean, default: false },
    saveUrl: { type: String, default: null }, // 요청 폼 저장 URL
  })

  const emit = defineEmits(['register', 'success']);

  // 상수
  const BASE_URL = '/support-response'
  const RESPONSE_READ_URL = BASE_URL // 응답 폼 조회 RUL
  const RESPONSE_SAVE_URL = BASE_URL + '/update_one' // 응답 폼 저장URL
  const DEFAULT_VALUE_STR = '신규';

  // TODO: lms_centers, lms_equipment_status_dev, lms_alarm_status_dev 테이블 간 LC_ID 통일
  const lcIdMatchingMap: Record<string, string> = {
    'JNM001': 'KR_KPP_YEOSU',
    'GYG001': 'KR_Daewha'
  }

  const { t } = useI18n();
  const { createConfirm, notification } = useMessage();
  const userStore = useUserStore();
  const currentUserId = computed(() => userStore.getUserInfo?.userId || '');

  // titleSlot, contentSlot의 비활성화 여부 계산
  // data.ts-isDisabled와 동일 로직
  function getSlotDisabled(values: any): boolean {
    if (props.isAdminPage) return false;
    const isInitialStatus = EDITABLE_STATUSES.has(values?.status);
    const isNotAuthor = values?.request_id && values.request_id !== currentUserId.value;
    return !isInitialStatus || isNotAuthor;
  }

  // 참조 및 상태
  const reqFormRef = ref();
  const resFormRef = ref();
  const requestFormData = ref<any>(null); // 유지보수 요청 폼 데이터
  const isUpdate = ref(false); // 수정모드 여부 (신규: false)
  const isSubmitting = ref<boolean>(false); // DB 작업 수행중 여부
  const isLoading = ref(false);
  const showTempSubmitBtn = ref(true) // 요청 폼 임시저장버튼 숨김/표시
  const showFileDeleteBtn = ref(true) // 요청 폼 첨부파일 개별 삭제버튼 숨김/표시

  // 선택 목록
  const {
    statusOptions,
    centerOptions,
    alarmOptions,
    equipOptions,
    loadCommonCodes
  } = useRefCodes(!props.isAdminPage);

  // 설비 옵션 필터링 : 선택한 센터의 설비만 표시
  function getFilteredEquipOptions(lcId: string | undefined | null) {
    if (!lcId) return [];

    return equipOptions.value.filter((e: any) => {
      const currentLcId = String(e.lc_id)
      const selectedLcId = String(lcId)

      if (lcIdMatchingMap[selectedLcId]) {
        return currentLcId === lcIdMatchingMap[selectedLcId]
      }
      return currentLcId === selectedLcId;
    });
  }

  // 알람 옵션 필터링 : 선택한 센터 및 설비의 알람만 표시
  function getFilteredAlarmOptions(equipId: string | undefined | null, lcId: string | undefined | null) {
    if (!lcId) return [];

    let filteredOptions = alarmOptions.value.filter((e: any) => {
      const currentLcId = String(e.lc_id)
      const selectedLcId = String(lcId)

      if (lcIdMatchingMap[selectedLcId]) {
        return currentLcId === lcIdMatchingMap[selectedLcId]
      }
      return currentLcId === selectedLcId;
    });
    if (equipId) filteredOptions = filteredOptions.filter((a: any) => String(a.equip_id) === String(equipId));
    return filteredOptions;
  }

  // 센터 필드 비활성화 여부 : 저장된 건은 센터 변경 불가
  const isLcIdSlotDisabled = computed(() => {
    if (!isUpdate.value) return false;
    if (requestFormData.value?.support_id) return true;
    return !showTempSubmitBtn.value;
  });

  // 설비 필드 비활성화 여부 : 저장된 건은 관리자페이지에서만 변경 가능
  const isEquipSlotDisabled = computed(() => {
    if (!isUpdate.value) return false;
    if (props.isAdminPage) return false;
    return !showTempSubmitBtn.value;
  });

  // 알람 필드 비활성화 여부 : 저장된 건은 관리자페이지에서만 변경 가능
  const isAlarmSlotDisabled = computed(() => {
    if (!isUpdate.value) return false;
    if (props.isAdminPage) return false;
    return !showTempSubmitBtn.value;
  });

  // 파일 관리
  const {
    FILE_BASE_URL, selectedFiles, deletedFileIds, isDragging,
    convertToBase64, clearFiles, addFiles, removeFile, handleFileDrop
  } = useSupportFile();

  // 폼 관리
  const {
    onSubmit, onTempSave, onSubmitCompletedStatus
  } = useSupportFormActions(reqFormRef, selectedFiles, deletedFileIds, convertToBase64, clearFiles, computed(() => props.isAdminPage));

  // 폼 및 모달 설정
  const submitButtonOptions = { text: t('button.save'), type: 'primary' as const }

  const [registerForm, {
    setFieldsValue, resetFields, validate, getFieldsValue, setProps
  }] = useForm({
    baseColProps: { span: 24 },
    labelWidth: 120,
    schemas: computed(() => props.schemas), // 요청 폼 스키마 전달
    showActionButtonGroup: true, // 버튼 표시/숨김
    showSubmitButton: true, // submit 버튼 사용
    showResetButton: false, // reset 버튼 미사용
    actionColOptions: { span: 24, style: { textAlign: 'center', marginTop: '10px' } } // 버튼 한줄로 표시
  })

  const [registerModal, { closeModal }] = useModalInner(async (data) => { await setReqForm(data) })

  /**
   * 모달 오픈 시 초기화
   * 1. 공통코드 조회
   * 2. 폼 스키마 초기화
   * 2. 생성/수정 상태관리
   * 3. 파일 조회 및 폼 데이터 바인딩
   * 4. 제출한 상태이면 버튼 표시/숨김 처리
   * @param data
   */
  async function setReqForm(data: any) {
    try {
      isLoading.value = true;

      loadCommonCodes();
      resetFields();
      selectedFiles.value = [];
      requestFormData.value = data?.record || {};

      isUpdate.value = !!data?.isUpdate;

      if (isUpdate.value) {
        await setFieldsValue(data.record)

        if (requestFormData.value?.support_id) {
          await loadServerFiles(requestFormData.value.support_id)
        }

        if (data?.record?.status != '1') { // 임시저장('1')
          showTempSubmitBtn.value = false;

          if (!props.isAdminPage) {
            showFileDeleteBtn.value = false;
            setProps({
              showSubmitButton: false
            })
          }
        }
      }
    } finally {
      isLoading.value = false;
    }
  }

  /**
   * 데이터 API 및 이벤트 핸들러
   */
  // 요청 폼 첨부파일 조회 - support_id기반
  async function loadServerFiles(supportId: string) {
    try {
      const response = await getCommonGetApi(FILE_BASE_URL + `/${ encodeURIComponent(supportId) }`, null)
      if (Array.isArray(response)) {
        selectedFiles.value = response.map(file => ({
          id: file.id,
          name: file.origin_file_name,
          size: file.size || 0,
          isServerFile: true,
        }))
      }
    } catch (e) {
      // console.warn("[loadServerFiles] 파일 로드 오류 e =", e);
    }
  }

  // 요청 폼 저장 핸들러
  // - validFormData: 유효성 검증 거친 요청 폼 데이터
  async function handleSubmit(validFormData) {
    try {
      const formMethods = { validate, getFieldsValue };

      const result = await onSubmit(validFormData, formMethods);
      if (result) {
        closeModal();
        emit('success')
      }
    } catch (e) {
      notification.error({
        message: '저장 실패',
        description: `필수 항목 또는 작성자 본인이 작성한 요청 건인지 확인해주세요`,
        duration: 2,
      });
    }
  }

  // 요청 폼 임시저장 핸들러
  async function handleTempSubmit() {
    try {
      const formMethods = { validate, getFieldsValue, setFieldsValue }

      const result = await onTempSave(formMethods);
      if (result) {
        closeModal();
        emit('success')
      }
    } catch (e) {
      // console.error("[handleTempSubmit] 임시저장 실패 e =", e);
      notification.error({
        message: '임시저장 실패',
        description: `필수 항목 또는 작성자 본인이 작성한 요청 건인지 확인해주세요`,
        duration: 2,
      });
    }
  }

  // 답변 완료 핸들러 - 답변 작성 시 detail 폼 상태 업데이트
  async function handleCompleted(isCompleted: boolean) {
    const currentFields = await getFieldsValue();

    try {
      const result = await onSubmitCompletedStatus(currentFields, isCompleted);
      if (result) {
        requestFormData.value = { ...requestFormData.value, ...result };
        await setFieldsValue({
          status: result.status, receiver_id: result.receiver_id, completed_at: result.completed_at
        })
        emit('success')
      }
    } catch (e) {
      // console.warn('[handleCompleted] e =', e)
    }
  }

  // 답변 완료 핸들러 - 상위 컴포넌트에 알림
  function handleRespAction() {
    emit('success')
  }

  // 파일 업로드 이벤트
  const handleFileChange = async (event: Event) => {
    const target = event.target as HTMLInputElement;
    if (target.files) {
      addFiles(Array.from(target.files));
      target.value = '';
    }
  };

  /**
   * 스타일
   */

    // 파일 사이즈 포맷팅
  const formatSize = (size: number) => {
      if (size === 0) return '0 Bytes';
      const k = 1024;
      const sizes = ['Bytes', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(size) / Math.log(k));
      const unitIndex = Math.min(i, sizes.length - 1); // 배열 내로 범위 제한
      return parseFloat((size / Math.pow(k, unitIndex)).toFixed(2)) + ' ' + sizes[unitIndex];
    };

  // 날짜 포맷팅 - 문자열 변환
  function formatDateToString(value: any) {
    if (value === null || value === undefined || value === '') return '-';
    const valueDate = dayjs(value);
    return valueDate.isValid() ? valueDate.format('YYYY-MM-DD HH:mm') : String(value);
  }

  // 날짜 포맷팅 - 생성/완료일시 통합
  function formatCreatedCompleted(model: any) {
    const t1 = formatDateToString(model?.created_at);
    const t2 = formatDateToString(model?.completed_at);
    return `생성 ${ t1 } / 완료 ${ t2 }`;
  }

  // 옵션 포맷팅 - Select 옵션의 Value를 Label로 변환
  function getOptionValueToLabel(options: OptionType[], optionValue: string): string {
    if (optionValue === null || optionValue === undefined || optionValue === '')
      return DEFAULT_VALUE_STR;

    const optionLabel = options.find((el: any) => el.value === optionValue);
    return optionLabel ? optionLabel.label : optionValue;
  }
</script>
<style scoped>
</style>
