<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="t('label.shipment_status_detail_list')"
    :headStyle="headStyleRef"
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
  import { ref,type CSSProperties ,computed} from 'vue';
  import { Grid as BasicGrid } from '/@/components/Grid/index';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { useI18n } from '/@/hooks/web/useI18n';

  const gridRef = ref();
  const gridData = ref();
  const headStyleRef: CSSProperties = {
  textAlign:'center',lineHeight:'0vw',minHeight:'0vw',fontSize:'0.8rem',backgroundColor:'black',color:'white',borderRadius:'0px',padding:'0px'};
  
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
          header: t('label.start_date'),  //시작 일시
          name: 'start_date',
          sortable: true,
          width: 150,
          align:'center',
        },
        {
          header: t('label.inbound_no'), //Inbound No
          name: 'inbound_no',
          sortable: true,
          width: 150,
          align:'center',

        },
        {    
          header: t('label.item_owner2'), //거래처 명
          name: 'item_owner',
          sortable: true,
          width: 100,
          align:'center',
        },
        {
          header: t('label.shipment_total_qty'),  //입하 총 수량
          name: 'shipment_total_qty',
          sortable: true,
          width: 100,
          align:'center',

        },
        {
          header: t('label.inspection_free_qty'), //무검사 수량
          name: 'inspection_free_qty',
          sortable: true,
          width: 100,
          align:'center',

        },
        {
          header: t('label.inspection_qty'),  //유검사 수량
          name: 'inspection_qty',
          sortable: true,
          width: 100,
          align:'center',

        },
        {
          header: t('label.large_item_rack_qty'), //대물렉 수량
          name: 'large_item_rack_qty',
          sortable: true,
          width: 100,
          align:'center',

        },
        {
          header: t('label.small_item_pallet_mapping_qty'), //소물 팔레트 매핑 수량
          name: 'small_item_pallet_mapping_qty',
          sortable: true,
          width: 130,
          align:'center',
        }
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
