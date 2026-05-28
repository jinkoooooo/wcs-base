<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="titleRef"
    @ok="handleSubmit"
    :useWrapper="false"
    :scroll="false"
    :bodyStyle="{ height: '350px', overflowY: 'auto' }"
  >
    <div class="task-process-container">
      <div class="input-group">
        <label>
          <span class="label-text">작업 ID :</span>
          <a-input v-model:value="taskId" placeholder="작업 ID를 입력하세요" class="task-id-input" :readonly="true" />
        </label>
      </div>

      <h3 style="margin-top: 20px; margin-bottom: 15px;">해당 작업을 다음과 같이 처리합니다.</h3>
      <a-radio-group v-model:value="processType" class="process-options">
        <a-radio :value="PROCESS_TYPE.COMPLETE" class="option">
          완료 (재고가 작업 목적지에 있습니다.)
        </a-radio>

        <a-radio :value="PROCESS_TYPE.RESTORE" class="option">
          취소 (재고가 작업 출발지에 있습니다.)
        </a-radio>

        <a-radio :value="PROCESS_TYPE.TRANSFER" class="option">
          이송 (재고를 아래 위치에 이송시켰습니다.)
        </a-radio>

<!--        <a-radio :value="PROCESS_TYPE.REMOVE" class="option">-->
<!--          제거 (재고를 하이랙 밖으로 제거했습니다.)-->
<!--        </a-radio>-->
      </a-radio-group>

      <div v-if="processType === PROCESS_TYPE.TRANSFER" class="location-input-group">
        <label>열: <input type="number" v-model.number="transferPoint.col" min="1" /></label>
        <label>행: <input type="number" v-model.number="transferPoint.row" min="1" /></label>
        <label>단: <input type="number" v-model.number="transferPoint.level" min="1" /></label>
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { BasicModal, useModalInner } from '/@/components/Modal';
import { useI18n } from '/@/hooks/web/useI18n';
import { useMessage } from '/@/hooks/web/useMessage';
import { getCommonPostApi } from '/@/api/common/api';
import { Input as AInput, Radio as ARadio, RadioGroup as ARadioGroup } from 'ant-design-vue';


const { t } = useI18n();
const { notification } = useMessage();
const titleRef = ref( t("작업 강제 처리"));

// 처리 타입 상수 정의
const PROCESS_TYPE = {
  COMPLETE: 'B1',
  TRANSFER: 'A1',
  REMOVE: 'A2',
  RESTORE: 'A3',
};

// 데이터 정의
const taskId = ref<string>('');
const processType = ref<string>(PROCESS_TYPE.COMPLETE); // 기본값 설정
const transferPoint = ref({ col: 0, row: 0, level: 0 });
const taskRef = ref();
let doubleClickPrevent = true;


const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
  setModalProps({ confirmLoading: false });

  // 모달 열릴 때 데이터 초기화
  taskId.value = data.task_id;
  processType.value = PROCESS_TYPE.COMPLETE;
  transferPoint.value = { col: 0, row: 0, level: 0 };
  taskRef.value = data;

  doubleClickPrevent = true;
});

/**
 * 이송 위치에 대한 유효성 검사
 * @returns {boolean | {bool: false, missingValue: string}} - 유효하면 true, 아니면 오류 정보 객체를 반환
 */
const validation = () => {
  // '이송' 옵션이 아닐 경우 위치 검사를 할 필요가 없습니다.
  if (processType.value !== PROCESS_TYPE.TRANSFER) {
    return { bool: true };
  }

  const point = transferPoint.value;

  const checks = [
    { name: '이송 열', value: point.col, max: 21, min: 1 },
    { name: '이송 행', value: point.row, max: 18, min: 1 },
    { name: '이송 단', value: point.level, max: 3, min: 1 },
  ];

  for (const check of checks) {
    // 2. col, row, level은 0 초과 (1 이상)
    if (check.value <= 0) {
      return { bool: false, missingValue: `${check.name}은 1 이상이어야 합니다.` };
    }
    // 3, 4, 5. col(21 이하), row(18 이하), level(3 이하) 제한
    if (check.value > check.max) {
      return { bool: false, missingValue: `${check.name}은(는) ${check.max} 이하로 입력해야 합니다.` };
    }
  }

  return { bool: true };
};


// -----------------------------------------------------------
// 팝업창 확인 버튼 클릭
// -----------------------------------------------------------
const handleSubmit = async () => {
  // 이송 선택 시 위치 유효성 검사
  const validationResult = validation();
  if (!validationResult.bool) {
    return notification.error({
      message: '에러',
      description: validationResult.missingValue,
      duration: 5,
    });
  }

  if (doubleClickPrevent) {
    doubleClickPrevent = false;
  }
  else {
    return;
  }

  // 파라미터 구성
  const job = {
    wms_ord_no: taskRef.value.task_id,
    order_id: taskRef.value.task_no,
    result_type: processType.value
  };

  const formattedCol = String(transferPoint.value.col).padStart(2, '0');
  const formattedRow = String(transferPoint.value.row).padStart(2, '0');
  const formattedLevel = String(transferPoint.value.level).padStart(2, '0');

  const param = {
    result: job,
    // 이송일 경우에만 위치 정보 포함
    ...(processType.value === PROCESS_TYPE.TRANSFER && {
      end_point_cd: `P${formattedCol}-${formattedRow}-${formattedLevel}`,
    }),
  };

  try {
    let url = '/wcs_task/force_completion';
    const result = await getCommonPostApi(url, param);

    if (result === "success") {
      return notification.success({
        message: '성공',
        description: `작업 ID ${taskId.value} 처리에 성공하였습니다.`,
        duration: 3,
      });
    }
    else {
      return notification.error({
        message: '에러',
        description: `작업 처리 실패: ${result}`,
        duration: 3,
      });
    }
  }
  catch(Exception) {
    notification.error({
      message: t('title.error'),
      description: t('TITLE_DATA_BASE_ERROR'),
      duration: 3,
    });
  }
  finally {
    closeModal();
  }
};
</script>

<style lang="less" scoped>
.task-process-container {
  padding: 10px 20px;
}
.input-group {
  // 이전 요청처럼 왼쪽 마진 20px 적용
  margin-left: 20px;
  .label-text {
    font-weight: bold;
    margin-right: 10px;
  }
  .task-id-input {
    width: 200px;
  }
}
.process-options {
  display: flex;
  flex-direction: column;
  margin-left: 20px;
}
.option {
  margin: 5px 0;
}
.location-input-group {
  margin-top: 5px;
  margin-bottom: 15px;
  margin-left: 30px; /* 라디오 버튼보다 더 들여쓰기 */
  label {
    margin-right: 15px;
    input {
      width: 60px;
      text-align: right;
      margin-left: 5px;
    }
  }
}
</style>
