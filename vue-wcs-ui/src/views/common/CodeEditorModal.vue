<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="titleRef" @ok="handleSubmit">
  <div class="modal-size">
    <CodeEditor
      v-model:value="codeValueNew"
      ref="myEditor"
      :mode="props.mode"
    />
  </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { useI18n } from '/@/hooks/web/useI18n';
  import { ref,onMounted } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getCommonPutApi,getCommonGetApi } from '/@/api/common/api';
  import { CodeEditor, MODE } from '/@/components/CodeEditor';

  const emit = defineEmits(['success', 'register']);
  const isUpdate = ref(true);
  const { notification } = useMessage();
  const { t } = useI18n();
  const titleRef = ref();
  const codeValueOrg = ref('');
  const codeValueNew = ref('');

  const props = defineProps({
    title: { type: String },
    resourceUrl: { type: String },
    searchParams: { type: Object },
    codeField: { type: String, default: 'logic' },
    codeValueNew: { type: String },
    mode: {
      type: String as PropType<MODE>,
      default: MODE.GROOVY,
      validator(value: any) {
        return Object.values(MODE).includes(value);
      },
    },
  });


  let recordId: any = '';
  let record: any = {};

  const [registerModal, { setModalProps, redoModalHeight, closeModal }] = useModalInner(
    async (data) => {
      setModalProps({ confirmLoading: false });
      record = data.record;
      //팝업 조회시 이전 title이 떠서 해당 부분 수정함
      titleRef.value = record.name;
      recordId = record.id;
      if (props.codeField) {

        //로그 뷰어의 경우 로그 파일만을 조회함. 백엔드 호출 X
        if(props.codeField==='log'){
          codeValueOrg.value = record[props.codeField];
          codeValueNew.value = codeValueOrg.value;}

        //커스텀 서비스, 커스텀 템플릿의 경우
          else{
          fetch(record);}
      }
    },
    );

  //해당 팝업창 열기 전 백엔드 추가 호출
  const fetch = async (data) => {
    let url = `${data.url}/${data.id}`;
    let rec = await getCommonGetApi(url, null);
    codeValueOrg.value = rec[props.codeField];
    codeValueNew.value = codeValueOrg.value;  
  };

  async function handleSubmit() {
    // 조회 값과 현재 에디터의 값이 같으면 저장 안 함.
    if (codeValueOrg.value == codeValueNew.value) {
      notification.info({
        message: t('title.info'),
        description: t('text.NOTHING_CHANGED'),
        duration: 2,
      });
      // 저장 처리
    } else {
      let saveObject = { id: recordId };
      saveObject[props.codeField] = codeValueNew.value;
      let response = await getCommonPutApi(props.resourceUrl, saveObject);
      if (response) {
        closeModal();
        emit('success');
      }
    }
  }
</script>
<style lang="less" scoped>
  .ant-modal-header {
    padding: 24px;
  }
  .modal-size {
    height: 900px;
    width: 800px;
  }
</style>
