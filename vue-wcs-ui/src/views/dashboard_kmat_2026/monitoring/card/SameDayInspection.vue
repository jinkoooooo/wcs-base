<template>
  <div>
  <div class="custom-ribbon"></div>
  <!-- 당일 검사 -->
  <Card
    :loading="loading"
    ref="cardRef"
    :title="t('label.same_day_inspection')"
    :headStyle="headStyleRef"
    :bodyStyle="bodyStyleRef"
    :style="styleRef"
  >
    
  <div class="content" style="display: flex;">
    <!-- CHART AREA -->
    <div class="chart-area">
      <div ref="chartRef" :style="{ height:chartHeight,}" ></div>
    </div>

    <!-- DETAIL AREA -->    
    <div class="text-area">
      <div  :style="chartDetailStyleRef">
        <div>{{ t('label.lab_transfer_count') }} : {{ qcTransferCountForTheDay }}</div>
        <div>{{ t('label.under_inspection') }} : {{ qcInspectionInProgress }}</div>
        <div>{{ t('label.inspection_pass') }} : {{ qcInspectionPass }}</div>
        <div>{{ t('label.inspection_ng') }} : {{ qcInspectionNg }}</div>
        <div>{{ t('label.inspection_completed') }} :  {{ passStockCompleted }}</div>
      </div>     
    </div>

  </div>
</Card>
</div>
</template>

<script lang="ts" setup>
import { Card } from 'ant-design-vue';
import { ref, type CSSProperties,watch,nextTick,onMounted,Ref,computed } from 'vue';
import * as echarts from 'echarts';
import { useECharts } from '/@/hooks/web/useECharts';
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

const loading = ref(false);

/************* 바인딩 변수 *************/
const qcTransferCountForTheDay = ref(0);
const qcInspectionInProgress= ref(0);
const qcInspectionPass= ref(0);
const passStockCompleted= ref(0); 
const qcInspectionRework = ref(0);
const qcInspectionNg= ref(0);


/************* 스타일 *************/
//카드컴포넌트 관련 스타일
const styleRef = ref({border: '1px solid #00BFFF',borderRadius:'0px',backgroundColor:'#00386C',height:'15.2rem'})

//카드 헤더 스타일 적용
const headStyleRef: CSSProperties = {
  textAlign:'center',lineHeight:'0vw',minHeight:'0vw',fontSize:'0.9rem',backgroundColor:'#00386C',color:'white',borderRadius:'0px',padding:'0px'
};

//카드 바디 스타일 적용
const bodyStyleRef: CSSProperties = {
  backgroundColor: '#00386C',
  color:'white',
  padding:'0',
};

//차트 스타일 : 높이만(너비는 자동 조정됨)
const chartDetailStyleRef =computed(()=>{
  return {fontSize:isKorean.value ? '0.6rem':'0.5rem'}
});
const chartHeight=ref('13rem');


/************* 그 외 *************/
//언어 추출 -> 언어에 따라 css 상이
const koLocaleRef = ref('ko-KR');
const isKorean = computed(() => locale.value === koLocaleRef.value);

//차트 표시
const chartRef = ref<HTMLDivElement | null>(null);

//locale 관련 다국어 처리, 현재 locale 추출
const { t,locale } = useI18n();
const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);



onMounted(async()=>{
  //마운트 시 데이터 셋팅
  nextTick(()=>{
      setData(props.data);
    })
})




/**
 * 새로운 데이터 바인딩
 * @param newData 부모컴포넌트를 통해 전달 받은 데이터
 */
function setData(newData:any){
  if (!newData) {
  // 데이터가 없으면 기본값으로 설정
  qcTransferCountForTheDay.value = 0;
  qcInspectionInProgress.value = 0;
  qcInspectionPass.value = 0;
  passStockCompleted.value = 0;
  qcInspectionNg.value = 0;
  qcInspectionRework.value = 0;
  return;
}

qcTransferCountForTheDay.value = newData.qc_transfer_count_for_the_day || 0;
qcInspectionInProgress.value = newData.qc_inspection_in_progress || 0;
qcInspectionPass.value = newData.qc_inspection_pass || 0;
passStockCompleted.value = newData.pass_stock_completed || 0;
qcInspectionNg.value = newData.qc_inspection_ng || 0;
qcInspectionRework.value = newData.qc_inspection_rework || 0;

}



/**
 * echart 적용 메타데이터
 */
function setOptionsData(newData:any){
setOptions({
  grid: {
  top: 0,
  bottom: 0,
  left: 0,
  right: 0,
  containLabel: false
},
tooltip: {
  trigger: 'item',
  padding: 0
},
legend: {
  bottom:'4%',
  left: 'center',
  orient: 'horizontal',
  textStyle: {
    fontSize: 7,
    color:'white'
  },
  itemGap:8,
  itemWidth: 6,
  itemHeight: 6
},
series: [
  {
    name: '',
    type: 'pie',
    radius: ['40%', '70%'],
    center: ['50%', '35%'], 
    avoidLabelOverlap: false,
    label: {
      show: false,
      position: 'center'
    },
    emphasis: {
      label: {
        show: false,
        fontSize: 40,
        fontWeight: 'bold'
      }
    },
    labelLine: {
      show: false
    },
    data: [
        {
          value: qcTransferCountForTheDay.value,
          name: t('label.transferred_to_inspection_room'),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#248fe0' },
            ]),
            borderWidth: 1
          }
        },
        {
          value: qcInspectionInProgress.value,
          name: t('label.inspection_in_progress'),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#6ab0e6' },
            ]),
            borderWidth: 1
          }
        },
        {
          value: qcInspectionPass.value,
          name: t('label.passed_inspection'),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#40E0D0' },
            ]),
            borderWidth: 1
          }
        },
        {
          value: qcInspectionNg.value ,
          name: t('label.failed_inspection'),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#2c4a61' },
            ]),
            borderWidth: 1
          }
        },
        {
          value: passStockCompleted.value,
          name: t('label.inspection_completed'),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#1c6ead' },
            ]),
            borderWidth: 1
          }
        }
      ]
  }
],

});
}


watch([() => props.loading,()=>props.date, ()=>props.data], async ([newLoading,newDate,newData]) => {

// 만약 loading이 true일 경우에는 아무 동작도 하지 않음
if (newLoading) {
  return;
}
await setData(newData);

// ECharts 옵션을 업데이트
setOptionsData(newData);
},
);


</script>

<style scoped>
.content {
  display: flex; /* Flexbox를 사용하여 나란히 배치 */
  justify-content: space-between; /* 두 div 사이에 여백을 자동으로 배치 */
}

.chart-area{
  min-width: 50%;
  flex:1;
}
.text-area{
  min-width: 50%;
padding:10px 0px 0px 0px;
min-width: 50%;
margin-top:8%;
}



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
.text-area > div > div {
  padding-bottom: 2px; /* 원하는 값으로 조절 */
}
</style>
