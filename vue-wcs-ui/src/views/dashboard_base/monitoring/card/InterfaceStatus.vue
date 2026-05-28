<template>
  <div>
   <div class="custom-ribbon"></div>
 <!-- 입하 & 품질검사 현황 -->
 <Card
   :loading="loading"
   ref="cardRef"
   :title=" t('label.interface_status')"
   :headStyle="headStyleRef"
   :bodyStyle="bodyStyleRef"
   :style="styleRef"
 >
 <!-- GRID AREA -->
   <div v-if="$props.data">
     <Table
     :ref="gridRef"
     :columns="columns"
     :dataSource="dataRef"
     :pagination="false"
     :style="tableStyle" >
     </Table>
   </div>
   <div v-else style="height:6.35rem;"></div>
 </Card>
 </div>
</template>

<script lang="ts" setup>
 import { Card } from 'ant-design-vue';
 import { computed,ref, type CSSProperties,onMounted,h } from 'vue';
 import { Table } from 'ant-design-vue';
 import { useI18n } from '/@/hooks/web/useI18n';
 import { ColumnType } from 'ant-design-vue/es/table/interface';


 const props = defineProps({
   loading: Boolean,
     data:{
       type:Object,
       default:[],
       require:true
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
   border: '1px solid  #00BFFF',borderRadius:'0',paddingBottom:'1px',backgroundColor:'#00386C',height:isKorean.value?'8.56rem':'8.55rem',
   padding:isKorean.value?'0.1rem' : '0.05rem',
   }
 })

//카드컴포넌트 - 타이틀 관련 스타일
 const headStyleRef: CSSProperties = {
   textAlign:'center',lineHeight:'0vw',minHeight:'0vw',fontSize:'0.9rem',backgroundColor:'#00386C',color:'white',borderRadius:'0px',padding:'0px'

 };

//카드컴포넌트 - 바디 관련 스타일
 const bodyStyleRef: CSSProperties = {
   backgroundColor: '#00386C',
   padding:'0px',
   color:'white',
 };

//그리드 관련 스타일
 const tableStyle = ref<CSSProperties>(
   {background:'none',fontSize:'0.7rem'}
 );

/************* 그리드 *************/
//그리드 객체
const gridRef = ref();

/************* 그 외 *************/
//언어 추출 -> 언어에 따라 css 상이
const { t, locale } = useI18n();
const koLocaleRef = ref('ko-KR');
const isKorean = computed(() => locale.value === koLocaleRef.value);
const loading = ref(false);


//그리드에 데이터 매핑
const dataRef:any=computed(()=>{
 return props.data;
});

/**
* 그리드 메타데이터 셋팅
*/
const columns = computed<ColumnType<RecordType>[]>(() => {


onMounted(()=>{
 gridRef.value?.createInstance();
 gridRef.value?.setColumns(columns.value);
});

/**
  * 공통 셀 스타일을 적용하는 함수
 */
 const getCustomCell = () => ({
 style: {
   padding: '0.1rem',  // 모든 셀에 공통으로 적용할 padding
   backgroundColor:'#00386C',
   color:'white',
   fontSize: isKorean.value  ? '0.6rem':'0.5rem' 
 },
});

/**
  * 공통 헤더 스타일을 적용하는 함수
 */
const getCustomHeaderCell = (param) => {
return { style: {
padding: '0.5px',  // 모든 셀에 공통으로 적용할 padding
backgroundColor:'#00386C',
color:'white',
fontSize: isKorean.value  ? '0.7rem':'0.55rem' ,
height:'1rem'
},
}
};

  return [
   {
     title: t('label.system'),
     dataIndex: 'system',
     align:'center',
     width:70,
     customRender:(record)=>{
       if(record.value.includes('label.')){
         return t(record.value);
       }
       return record.value;
     },
    customCell: getCustomCell,
    customHeaderCell: getCustomHeaderCell,
   },
   {
     title: t('label.status'),
     dataIndex: 'status',
     align:'center',
     width:70,
     customRender:(record)=>{
       return t(record.value);
     },
    customCell: getCustomCell,
    customHeaderCell: getCustomHeaderCell,
   },
   {
   title:  t('label.communication_status'),
   dataIndex: 'communication_status',
   align: 'center',
   width:80,    
   customRender: ({ value }: { value: boolean }) => {
   const color = value ? 'green' : 'red';
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
   }]
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



