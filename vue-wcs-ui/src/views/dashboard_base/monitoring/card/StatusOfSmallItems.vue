<template>
  <div>
  <div class="custom-ribbon"></div>
<!-- 입하 & 품질검사 현황 -->
<Card
  :loading="loading"
  ref="cardRef"
  :title="t('label.status_of_small_items')"
  :headStyle="headStyleRef"
  :bodyStyle="bodyStyleRef"
  :style="styleRef"
>
  <div>
    <!-- GRID AREA -->
    <Table
    :ref="gridRef"
    :columns="columns"
    :dataSource="dataRef"
    :pagination="false"
    :style="tableStyle" 
    class="no-border-table"
    >
    </Table>
  </div>
</Card>
</div>
</template>

<script lang="ts" setup>
import { Card } from 'ant-design-vue';
import { computed,ref, type CSSProperties,onMounted } from 'vue';
import { Table } from 'ant-design-vue';
import { ColumnType } from 'ant-design-vue/es/table/interface';
import { useI18n } from '/@/hooks/web/useI18n';

const props = defineProps({
  loading: Boolean,
    data:{
      type:Object,
      require:false
    },
    date:{
      type:String as PropType<string|null>,
      required:true
    }
});



/************* 스타일 *************/
//카드컴포넌트 관련 스타일
const styleRef = ref({border: '1px solid  #00BFFF',borderRadius:'0',paddingBottom:'3px',backgroundColor:'#00386C'})

//카드컴포넌트 - 타이틀 관련 스타일
const headStyleRef: CSSProperties = {
  textAlign:'center',lineHeight:'0vw',minHeight:'0vw',fontSize:'0.9rem',backgroundColor:'#00386C',color:'white',borderRadius:'0px',padding:'0px'
};

//카드컴포넌트 - 바디 관련 스타일
const bodyStyleRef: CSSProperties = {
  backgroundColor: '#00386C',
  padding: '0px'
};

//카드컴포넌트 - 테이블 관련 스타일
const tableStyle = ref<CSSProperties>(
  {padding: '0px',background:'none',fontSize:'0.7rem'}
)

/************* 그리드 *************/
interface RecordType{
  key: number; // 각 레코드를 구분하는 고유 키
  shift: string; // SHIFT
  status: string; // 상태
  sr: string; // S/R
  assortProcessData: number; // 피킹지시
  inventorySupplyData:string;//재고보충
}
//그리드를 표현해주는 객체
const gridRef = ref();

//그리드에 데이터 매핑
const dataRef= computed(()=>{
  return props.data
  }
)
/************* 그 외 *************/
const loading = ref(false);
const { t,locale } = useI18n();


onMounted(()=>{
    gridRef.value?.createInstance();
    gridRef.value?.setColumns(columns.value);
})

/**
* 그리드 1 메타데이터 셋팅
*/
const columns = computed<ColumnType<RecordType>[]>(() => {

/**
 * 공통 셀 스타일을 적용하는 함수
*/
const getCustomCell = (param) => ({
  style: {
    padding: '1px',
    backgroundColor: '#00386C',
    color: 'white',
    fontSize: param.length > 5 ? '0.35rem' : '0.6rem'
  }
});

  return [
    {
      title: 'SHIFT',
      dataIndex: 'shift',
      align: 'center',
      width: 100,
      customRender: (record) => {
        if (record.index === 0) {
          return t('label.first_shift') + " " + record.value;
        } else if (record.index === 1) {
          return t('label.second_shift') + " " + record.value;
        }
        return record.value;
      },
      customCell: getCustomCell,
      customHeaderCell: getCustomCell,
    },
    {
      title: t('label.status'),
      dataIndex: 'status',
      align: 'center',
      width: 90,
      customRender: (record) => t(record.value),
      customCell: getCustomCell,
      customHeaderCell: getCustomCell,
    },
    {
      title: 'S/R',
      dataIndex: 'sr',
      align: 'center',
      width: 70,
      customCell: getCustomCell,
      customHeaderCell: getCustomCell,
    },
    {
      title: t('label.picking_instuction'),
      dataIndex: 'assort_process_data',
      align: 'center',
      width: 80,
      customCell: getCustomCell,
      customHeaderCell: getCustomCell,
    },
    {
      title: t('label.inventory_supply'),
      dataIndex: 'inventory_supply_data',
      align: 'center',
      width: 70,
      customCell: getCustomCell,
      customHeaderCell: getCustomCell,
    },
  ];
});



</script>

<style scoped>

.custom-ribbon {
position: absolute;
background: #00BFFF;
color: white;
padding: 18px 20px;
font-size: 14px;
font-weight: bold;
z-index: 10;
clip-path: polygon(0% 0%, 100% 0%, 0% 100%, 0% 100%)
}


</style>



