import { FormSchema } from "@/components/Form";

// 수정 가능한 상태
export const EDITABLE_STATUSES = new Set(['0', '1', '', undefined]); // 알수없음/임시저장

// 상태/권한별 입력 제한
// - 기존 작성자와 현재 유저가 다를 경우 수정 제한
// - 관리자는 예외
const isDisabled = (values: any, isAdminPage: boolean, currentUser: string) => {
  if (isAdminPage) return false;

  const isInitialStatus = EDITABLE_STATUSES.has(values?.status);

  const isNotAuthor = values?.request_id && values.request_id !== currentUser;

  return !isInitialStatus || isNotAuthor;
}

// 상태/권한별 입력 제한 (신규 생성만 가능)
const isDisabledLcId = (values: any, isAdminPage: boolean, currentUser: string) => {

  const isInitialStatus = EDITABLE_STATUSES.has(values?.status);

  const isNotAuthor = values?.request_id && values.request_id !== currentUser;

  const hasSupportId = !!values?.support_id;

  return !isInitialStatus || isNotAuthor || hasSupportId;
}

/**
 * 유지보수 요청 폼 스키마
 * @param t 다국어 변환 함수
 * @param options 선택목록 객체
 * @param isAdminPage 관리자 여부
 * @param isDisabled 상태별 수정권한 부여
 */
export const getFormSchemas = (
  t: any,
  options: { center: any[], assignee: any[], category: any[], status: any[], alarm: any[], equip: any[] },
  isAdminPage: boolean,
  currentUser: string,
): FormSchema[] => [
  // 1행
  {
    field: 'support_id',
    component: 'Input',
    label: t('label.support_id'),
    slot: 'etcSlot',
    colProps: { span: 12 },
    itemProps: {
      labelCol: { span: 6 },
      wrapperCol: { span: 18 },
    },
    componentProps: {
      bordered: false,
    },
  },
  {
    field: 'created_at',
    component: 'Input',
    label: t('label.created_at'),
    slot: 'createdAtSlot',
    colProps: { span: 12 },
    itemProps: {
      labelCol: { span: 0 },
      wrapperCol: { span: 24 },
    },
  },
  {
    field: 'completed_at',
    component: 'Input',
    label: t('label.completed_at'),
    slot: 'etcSlot',
    colProps: { span: 6 },
    ifShow: false,
  },
  // 2행
  {
    field: 'requester_id',
    component: 'Input',
    label: t('label.requester_id'),
    slot: 'etcSlot',
    colProps: { span: 12 },
    itemProps: {
      labelCol: { span: 6 },
      wrapperCol: { span: 18 },
    },
  },
  {
    field: 'status',
    component: isAdminPage ? 'Select' : 'Input',
    label: t('label.status'),
    slot: 'statusSlot',
    colProps: { span: 12 },
    itemProps: {
      labelCol: { span: 7 },
      wrapperCol: { span: 17 },
    },
    componentProps: {
      options: options.status,
      allowClear: false,
      placeholder: '신규',
    },
    dynamicDisabled: !isAdminPage,
  },
  // 3행 : 섹션 구분선
  {
    field: 'section_basic',
    component: 'Divider',
    label: '요청 기본정보',
    colProps: { span: 24 },
  },
  // 4행
  {
    field: 'assignee_id',
    component: 'Input',
    label: t('label.assignee_id'),
    rules: [{ required: true, message: '담당자는 필수필드입니다.', trigger: 'change' }],
    colProps: { span: 24 },
    itemProps: {
      labelCol: { span: 3 },
      wrapperCol: { span: 21 },
      extra: '예: 홍길동 프로(로지스올시스템즈)',
    },
    slot: 'assigneeSlot',
  },
  // 5행
  {
    field: 'receiver_id',
    component: 'Select',
    label: t('label.receiver_id'),
    colProps: { span: 24 },
    itemProps: {
      labelCol: { span: 3 },
      wrapperCol: { span: 21 },
    },
    componentProps: {
      options: options.assignee,
      placeholder: isAdminPage ? '접수자를 선택해주세요' : '자동 지정',
      allowClear: true,
    },
    dynamicDisabled: () => !isAdminPage,
  },
  // 6행 : 섹션 구분선
  {
    field: 'section_meta',
    component: 'Divider',
    label: '분류/센터/연관설비',
    colProps: { span: 24 },
  },
  // 7행
  {
    field: 'category',
    component: 'Select',
    label: t('label.category'),
    rules: [{ required: true }],
    colProps: { span: 12 },
    itemProps: {
      labelCol: { span: 6 },
      wrapperCol: { span: 18 },
    },
    componentProps: {
      placeholder: '요청유형을 선택해주세요',
      options: options.category?.filter((el) => el.label !== '-'),
      allowClear: false,
    },
    defaultValue: '7',
    dynamicDisabled: ({ values }) => isDisabled(values, isAdminPage, currentUser),
  },
  {
    field: 'lc_id',
    component: 'Select',
    label: t('label.lc_id'),
    rules: [{ required: true, message: '센터는 필수필드입니다', trigger: 'change' }],
    colProps: { span: 12 },
    itemProps: {
      labelCol: { span: 7 },
      wrapperCol: { span: 17 },
    },
    slot: 'lcIdSlot',
  },
  // 8행
  {
    field: 'equip_id',
    component: 'Select',
    label: t('label.equip_id'),
    colProps: { span: 12 },
    itemProps: {
      labelCol: { span: 6 },
      wrapperCol: { span: 18 },
    },
    slot: 'equipSlot',
  },
  {
    field: 'alarm_id',
    component: 'Select',
    label: t('label.alarm_id'),
    colProps: { span: 12 },
    itemProps: {
      labelCol: { span: 7 },
      wrapperCol: { span: 17 },
    },
    slot: 'alarmSlot',
  },
  // 9행
  {
    field: 'title',
    component: 'Input',
    label: t('label.title'),
    rules: [{ required: true, message: '제목은 필수 필드입니다', trigger: 'change' }],
    colProps: { span: 24 },
    itemProps: {
      labelCol: { span: 3 },
      wrapperCol: { span: 21 },
      extra: '예: 시스템 사용자 교육 요청',
    },
    slot: 'titleSlot',
  },
  // 10행
  {
    field: 'content',
    component: 'InputTextArea',
    label: t('label.content'),
    rules: [{ required: true, message: '내용은 필수 필드입니다', trigger: 'change' }],
    colProps: { span: 24 },
    itemProps: {
      labelCol: { span: 3 },
      wrapperCol: { span: 21 },
    },
    slot: 'contentSlot',
  },
  // 11행
  {
    field: 'attachment',
    component: 'Input',
    label: t('label.attachment'),
    slot: 'attachmentSlot',
    colProps: { span: 24 },
    itemProps: {
      labelCol: { span: 3 },
      wrapperCol: { span: 21 },
    },
    dynamicDisabled: ({ values }) => isDisabled(values, isAdminPage, currentUser),
  },
]
