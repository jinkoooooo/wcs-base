<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="getTitle"
    :showCancelBtn="false"
    @ok="handleSubmit"
  >
    <PDFViewer
      :source="pdfFile"
      :controls="controlsRef"
      style="height: 500px; width: 100vh"
      @download="handleDownload"
    />
  </BasicModal>
</template>
<script lang="ts" setup>
  import { useI18n } from '/@/hooks/web/useI18n';

  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getCommonFileApi } from '/@/api/common/api';
  import PDFViewer from 'pdf-viewer-vue';

  const emit = defineEmits(['success', 'register']);
  const isUpdate = ref(true);
  const { notification } = useMessage();
  const { t } = useI18n();

  const getTitle = ref(); //computed(() => t('button.change_rack'));
  const recordId = ref();
  const pdfFile = ref();
  const controlsRef = ref(['print', 'zoom', 'switchPage']);

  const [registerModal, { setModalProps, redoModalHeight, closeModal }] = useModalInner(
    async (data) => {
      getTitle.value = data.clickedRow.TotalPicking ? '토탈피킹 지시서' : '분류작업 지시서';
      recordId.value = data.clickedRow.TotalPicking
        ? '2c66814b-b0dc-44b8-9e6e-7bc3b694c68d'
        : 'b55bda08-76f7-4e18-afc3-639d12a9e397';
      setModalProps({ confirmLoading: false });
      fetchPdf(data.clickedRow);
    },
  );

  const fetchPdf = async (pdfData) => {
    let filterCols = [];
    filterCols.push({ name: 'batch_id', value: pdfData.id });
    let params = {};
    filterCols.forEach((c) => {
      delete c['operator'];
      delete c['relation'];
      params[c.name] = c.value;
    });

    let url = `/printouts/show_pdf/by_template/${recordId.value}?query=${encodeURI(
      JSON.stringify(filterCols),
    )}`;
    let res = await getCommonFileApi(url, params);

    var fileObj = await new Blob([res], { type: 'application/pdf' });

    pdfFile.value = URL.createObjectURL(fileObj);
  };

  const handleSubmit = () => {
    pdfFile.value = null;
    closeModal();
  };

  defineExpose({
    registerModal,
    getTitle,
    redoModalHeight,
    t,
  });
</script>
<style lang="less" scoped></style>
