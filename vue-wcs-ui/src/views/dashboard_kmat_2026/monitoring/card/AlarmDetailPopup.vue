<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title=" t('label.alarm_detail_list')"
    @ok="handleSubmit"
    :useWrapper="false"
  >
    <div class="modal-size">
      <BasicGrid
        ref="gridRef"
        :show-pagination="false"
        :rowHeaders="gridMeta.rowHeaders"
        :columns="gridMeta.columns"
        :columnOptions="gridMeta.columnOptions"
        :fetchHandler="fetchHandler"
        :style="{
          width:'100%'
        }"
        >
      </BasicGrid>
    </div>
  </BasicModal>
</template>
<script lang="ts" setup>
  import 'tui-grid/dist/tui-grid.css';
  import { ref } from 'vue';
  import { Grid as BasicGrid } from '/@/components/Grid/index';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useI18n } from '/@/hooks/web/useI18n';

  const gridRef = ref();
  const gridData = ref();

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false });  
    gridData.value=data;
    gridRef.value.fetch();

  });
  const { t } = useI18n();


  const gridMeta= {
    rowHeaders: undefined,
      columnOptions: {
        resizable: false,
        frozenCount:0
      },
      columns: [ 
      {
          header: t('label.begin_date'),  //발생 일시
          name: 'begin_date',
          sortable: true,
          width: 180,
          align:'center',
        },
        {
          header: t('label.classification'), //분류
          name: 'equip_type',
          sortable: true,
          width: 120,
          align:'center',

        },
        {    
          header: t('label.alarm_code'), //알람코드
          name: 'alarm_code',
          sortable: true,
          width: 120,
          align:'center',
        },
        {
          header: t('label.alarm_content'),  //알람내용
          name: 'warn_content',
          sortable: true,
          width: 350,
          align:'center',

        },
        {
          header: t('label.is_release'), //해제여부
          name: 'status',
          sortable: true,
          width: 120,
          align:'center',

        },
      ],
      data: [],
     
    };


async function fetchHandler({
    page,
    limit,
    sorters = [{ field: 'created_at', ascending: false }],
  }) {
    let returnData = {
      total:0,
      records:[]
    }
    try {
      if(!gridData.value || gridData.value.length==0){
        return returnData
      }
      returnData.records = gridData.value;
      returnData.total = gridData.value.length;
      return returnData;
    } catch (error) {}
  }


/**
 * 팝업창 확인 버튼 클릭
 */
const handleSubmit = async () => {
      // 모달창 닫기
      closeModal();
};

</script>
<style lang="less" scoped>
#container{
  // text-align: center;
  padding: 0.6rem;
  flex: 1;
  display: flex;
  flex-direction: column;
}
  .modal-size {
    min-height: 800px;
    display: flex;
    flex-direction: column;
    width: 60rem;
    background-color: @component-background;
  }
  .input-container input,

.input-container{
  display: flex;
  justify-content: space-between;
  margin-top:5px;

}
input {
  color: #000000;
  // border-radius: unset;
  // outline: none;
}

.ant-modal-footer{
  border-top:none !important;
}
</style>
