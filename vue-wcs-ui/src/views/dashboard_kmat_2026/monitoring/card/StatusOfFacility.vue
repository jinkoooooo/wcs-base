<template>
  <div>
<div class="custom-ribbon"></div>
<!-- 설비 현황 -->
<Card
  :loading="loading"
  ref="cardRef"
  :title="t('label.status_of_facility')"
  :headStyle="headStyleRef"
  :bodyStyle="bodyStyleRef"
  :style="styleRef"
>
<div v-if="$props.data"  class="flex p-1">
<div class="w-1/2">
  <!-- GRID AREA 1/2 -->
  <Table
    :ref="gridRef"
    :columns="columns"
    :dataSource="dataRef1"
    :pagination="false"
    :rowKey="record => record.id"
    :style="tableStyle" 
    class="no-border-table"
    :customRow="(record) => ({
      onClick: () => cellClick(record),
    })"
    >
    </Table>
</div>
<div class="w-1/2">
  <!-- GRID AREA 1/2 -->
  <Table
  :ref="gridRef"
  :columns="columns"
  :dataSource="dataRef2"
  :rowKey="record => record.id"
  :pagination="false"
  :style="tableStyle" 
  class="no-border-table"
  :customRow="(record) => ({
    onClick: () => cellClick(record),
  })"
  >
  </Table>
</div>
</div>


</Card>
</div>
</template>

<script lang="ts" setup>
import { Card } from 'ant-design-vue';
import { Table } from 'ant-design-vue';
import { computed,ref, type CSSProperties,watch,h } from 'vue';
import { onBeforeMount } from 'vue';
import { useMessage } from "/@/hooks/web/useMessage";
import { ColumnType } from 'ant-design-vue/es/table/interface';
import { getCommonGetApi } from '/@/api/common/api';
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
const styleRef =computed(()=>{
  return {
    border: '1px solid  #00BFFF',borderRadius:'0',overflowY: 'auto',background:'#00386C',
    height:isKorean.value ? '12.85rem':'11.94rem'
}});
//카드컴포넌트 - 타이틀 관련 스타일
const headStyleRef: CSSProperties = {
  textAlign:'center',lineHeight:'0vw',minHeight:'0vw',fontSize:'0.9rem',backgroundColor:'#00386C',color:'white',borderRadius:'0px',padding:'0px'
};

//카드컴포넌트 - 바디 관련 스타일
const bodyStyleRef: CSSProperties = {
  backgroundColor: '#00386C',
  padding:'0px 0rem 0px 0rem',
  color:'white',
};

//그리드 관련 스타일
const tableStyle = ref<CSSProperties>({
background: 'none',
fontSize:'0.7rem'
});

/************* 그리드 *************/
//그리드 객체
const gridRef = ref();

//그리드1 데이터
const dataList1 = ref();
//그리드2 데이터
const dataList2 = ref();

const dataRef1 = computed(()=>{
return dataList1.value;
});
const dataRef2 = computed(()=>{
return dataList2.value;
});

/************* 그 외 *************/
//언어 추출 -> 언어에 따라 css 상이
const { t, locale } = useI18n();
const koLocaleRef = ref('ko-KR');
const isKorean = computed(() => locale.value === koLocaleRef.value);

const { createMessage } = useMessage();
const loading = ref(false);


interface RecordType{
key: number; // 각 레코드를 구분하는 고유 키
equipType:string;
robotCode:string;
robotName:string;
facilityStatus:string;
communicationStatus:string;
jobQty:number;
alarmQty:number;
}



onBeforeMount(()=>{
gridRef.value?.createInstance();
gridRef.value?.setColumns(columns.value);
})

//부모로부터 받은 객체에 대해서 데이터 바인딩
function setData(newData:any){
dataList1.value = newData.status_of_facility_list1;
dataList2.value = newData.status_of_facility_list2;

}


/**
* 그리드 1 메타데이터 셋팅
* 
 {
        "equip_type": "AGV",
        "robot_code": "13443",
        "robot_name": "AGV 11호기",
        "facility_status": "R" | "E" | "W",
        "communication_status": true | false,
        "job_qty": 0,
        "alarm_qty": 0
    },
*/
const columns = computed<ColumnType<RecordType>[]>(() => {

/**
* 공통 셀 스타일을 적용하는 함수
*/
const getCustomCell = () => {
    return { style: {
    padding:isKorean.value?'0.1rem' : '0.05rem',
    backgroundColor:'#00386C',
    color:'white',
    fontSize: isKorean.value? '0.6rem' : '0.58rem'
    },
    }
};

const getCustomHeaderCell = (param) => {
  return { style: {
  padding: '0px',  // 모든 셀에 공통으로 적용할 padding
  backgroundColor:'#00386C',
  color:'white',
  fontSize: isKorean.value? '0.6rem' : '0.45rem',
  height:'1.5rem'
  },
  }
  };

return [
{
  title: t('label.robot_name'),
  dataIndex: 'robot_name',
  align:'center',
  width:110,
  customRender:(record)=>{
    return record.value+''+t('label.robot');
  },
  customCell: getCustomCell,
  customHeaderCell: getCustomHeaderCell,
},
{
  title: t('label.facility_status'),
  dataIndex: 'facility_status',
  align:'center',
  width:90,
  customRender: (facility_status: string) => {
    let color = '';
    switch (facility_status.value) {
      case 'W':
        color = 'green';
        break;
      case 'R':
        color = 'orange';
        break;
      case 'E':
        color = 'red';
        break;
      default:
        color = 'red';
    }
    return h('div', {
    style: {
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',   
      height: '100%',         
    }
  }, [
    h('span', {
      style: {
        display: 'inline-block',
        width: '12px',
        height: '12px',
        borderRadius: '50%',
        backgroundColor: color,
      }
    })
  ]);
    },
  customCell: getCustomCell,
  customHeaderCell: getCustomHeaderCell,
},
{
title: t('label.communication_status'),
dataIndex: 'communication_status',
align: 'center',
width:90,
customRender:(record)=>{
  if(record.value){
    return t('label.normal');
  }
    return t('label.severity_error');
  },
  customCell: (record) => 
  ({
  style: {
    backgroundColor: record.communication_status === false ? '#f71702' : '#00386C',
    color: 'white',
    padding: '0px',  // 통신상태 셀에만 적용할 특별한 스타일
    fontSize:'0.6rem'
  },
}),
  customHeaderCell: getCustomHeaderCell,
}
];
});


const cellClick = async (record: RecordType) => {
try {
let response = await getCommonGetApi(
  `/task_process/7/search/taskInfo/${record.robot_code}`,
  null
);
if (response.length < 1) {
  createMessage.info(t('label.no_tasks_in_progress'));
} else {
  if (response.status == "W") {
    createMessage.info(t('label.no_tasks_in_progress'));
  } else {
    createMessage.info({
      content: h('span', [
        `* ${t('label.job_detail')} `,
        h('br'),
        `${t('label.start')}: `, response.start_point_cd,
        h('br'),
        `${t('label.arrival')} :`, response.end_point_cd,
      ])
    });
  }

}
} catch (Exception) {
createMessage.info(t('label.not_load_the_data'));
}
}

watch([() => props.loading,()=>props.date, ()=>props.data], async ([newLoading,newDate,newData]) => {

// 만약 loading이 true일 경우에는 아무 동작도 하지 않음
if (newLoading) {
return;
}
await setData(newData);

},
);



</script>

<style scoped>
.custom-ribbon {
position: absolute;
background: #00BFFF;
color: white;
padding: 10px 12px;
font-size: 14px;
font-weight: bold;
z-index: 10;
clip-path: polygon(0% 0%, 100% 0%, 0% 100%, 0% 100%)
}


</style>



