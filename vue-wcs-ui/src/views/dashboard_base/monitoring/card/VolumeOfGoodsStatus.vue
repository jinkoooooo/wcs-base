<template>
  <div>
   <div class="custom-ribbon"></div>
  <Card
    :loading="loading"
    ref="cardRef"
    :title="t('label.volume_of_goods_status')"
    :headStyle="headStyleRef"
    :bodyStyle="bodyStyleRef"
    :style = "styleRef"
  >    
<span class="select-container">
  <!-- 우측 상단 SELECT-BOX AREA -->
    <Select v-model:value="optionsValueRef" 
      :size="sizeRef" 
      :options="selectOptionsRef" 
      @change="valueChanged" 
      class="selectStyle" :style="localeStyleRef"/>
</span>
  <!-- 좌측 CHART AREA -->
<div 
  class="chart-container"
  ref="chartRef" 
  :style="chartStyleRef">  
</div>    
  </Card>
</div>

</template>

<script lang="ts" setup>
  import { Ref, ref, watch,computed } from 'vue';
  import { Card } from 'ant-design-vue';
  import type { SelectProps } from "ant-design-vue";
  import { Select } from "ant-design-vue";
  import { useECharts } from '/@/hooks/web/useECharts';
  import { onBeforeMount } from 'vue';
  import { getCommonGetApi } from '/@/api/common/api';
  import {type CSSProperties} from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';

  const props = defineProps({
    loading: Boolean,
    data:{
      type:Object,
      required:true
    },
    date:{
      type:String as PropType<string|null>,
      required:true
    }
  });
  let emit = defineEmits();

/************* 스타일 *************/
//카드컴포넌트 관련 스타일
const styleRef = ref({border: '1px solid #00BFFF',borderRadius:'0',background:'#00386C',height:'16.5rem'});

//카드컴포넌트 - 타이틀 관련 스타일
const headStyleRef=ref<CSSProperties>({
  textAlign:'center',lineHeight:'0vw',minHeight:'0vw',fontSize:'0.9rem',backgroundColor:'#00386C',color:'white',borderRadius:'0px',padding:'0px'
  })

//카드컴포넌트 - 바디 관련 스타일
const bodyStyleRef: CSSProperties = {
  backgroundColor: '#00386C',
  padding: '1vh 5vh 0px 0vh', // top right bottom left 순서
  color:'white'
};

//select box size
const sizeRef = ref<SelectProps["size"]>("small");

const localeStyleRef =computed(()=>{
return {
  width:isKorean.value?'4rem':'5rem',padding:'0px',fontSize:'0.6rem' }
})

const chartStyleRef = ref({ 
height:'12rem',
zIndex: 1,
overflow: 'hidden' /* 오버플로우 처리 */
});

/************* SELECT BOX*************/
const optionsValueRef = ref<number>();
//select box 에 들어갈 데이터
const optionKeys = ['label.daily', 'label.weekly', 'label.monthly','label.hourly'];

/************* CHART *************/
const dataRef = ref();
const chartRef = ref<HTMLDivElement | null>(null);
const { setOptions, echarts } = useECharts(chartRef as Ref<HTMLDivElement>);

/************* 그 외 *************/
//언어 추출 -> 언어에 따라 css 상이
const { t,locale } = useI18n();
const koLocaleRef = ref('ko-KR');
const isKorean = computed(() => locale.value === koLocaleRef.value);

/**
 * 일별 :  1
 * 주별 : 2
 * 월별 : 3
 * 시간별 : 4 
*/
const selectOptionsRef = computed(() =>
optionKeys.map((key, index) => ({
    value: index + 1,
    label: t(key),
  }))
);


onBeforeMount(()=>{
  optionsValueRef.value = selectOptionsRef.value[0]?.value;
})

function valueChanged(e:any){
  optionsValueRef.value = e;
  //값이 변하면 상위컴포넌트에 변경 된 값 전달
  emit("lastDays",e);
}


function getLineData(data){
  let chartDatas = data;
  let category:string[] = chartDatas.map(({ date }) => date);  // x축 카테고리 목록
  let barData:string[] = chartDatas.map(({ inbound_qty }) => inbound_qty);  // 입고 수량 목록
  let barData2:string[] = chartDatas.map(({ input_qty }) => input_qty);  // 투입 수량 목록
  let barData3:string[] = chartDatas.map(({ complete_product_qty }) => complete_product_qty);  // 완제품 수량 목록
  let lineData:string[] = chartDatas.map(({ inbound_qty }) => inbound_qty);  // 입고 목록
  let lineData2:string[] = chartDatas.map(({ input_qty }) => input_qty);  // 출고 목록
  let lineData3:string[] = chartDatas.map(({ complete_product_qty }) => complete_product_qty);  // 출고 목록

  return {category,barData,barData2,barData3,lineData,lineData2,lineData3}
};

async function fetchData(e,date){
  let param = 
      {
      selectOption :e,
      chooseDate : date
      }
  try{
      let response = await getCommonGetApi("/dashboard/7/volumeOfGoodsStatus",param);
      dataRef.value = response; 
      }
  catch(Error){
      console.log(Error);
      }
}

/**
 * e-chart 적용 함수
 */
function setOptionData(){
  const chartData = getLineData(dataRef.value);

  setOptions({
  backgroundColor:'#00386C',
  grid: {
    left: '5%',   // 좌측 여백
    right: '15%',  // 우측 여백
    top: '3%',    // 상단 여백
    bottom: '0%',  //
    containLabel: true  // 레이블이 겹치지 않도록 보정
  },
  tooltip: {
    // trigger: 'axis',
    axisPointer: {
      type: 'line',
      label: { show: true, backgroundColor: '#333' },
    },
  },
  legend: {
    data: [t('label.inbound'), t('label.input'), t('label.final_product')],
    textStyle: { color: 'white',fontSize:'0.6rem' },
    right: '0%',  
    top: 'center',
    orient: 'vertical', 
  },
 
  xAxis: {
    type: 'category',
    data: chartData.category, 
    boundaryGap: true, 
    axisLine: { lineStyle: { color: 'white' } },
    axisLabel: {
    fontSize: dataRef.value.length>8 ? 9 : 12,
    color: 'white', 
  },
  },
  yAxis: {
    axisLine: { lineStyle: { color: 'white' } },
  },
  series: [
    {
      name: t('label.inbound'),
      type: 'bar',
      barWidth: dataRef.value.length==7 ? 20 : 10,
      itemStyle: {
        borderRadius: 0,
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 1, color: '#90ee90' },
        ]),
      },
      data: chartData.barData,
    },
    {
      name: t('label.input'),
      type: 'bar',
      barWidth: dataRef.value.length==7 ? 20 : 10,
      itemStyle: {
        borderRadius: 0,
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#49feff' },
        ]),
      },
      data: chartData.barData2,
    },
    {
      name: t('label.final_product'),
      type: 'bar',
      barWidth: dataRef.value.length==7 ? 20 : 10,
      itemStyle: {
        borderRadius: 0,
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#FBB6C1' },
        ]),
      },
      data: chartData.barData3,
    },
    {
      name: t('label.inbound'),
      type: 'line',
      data: chartData.barData, // 바 차트 데이터와 동일한 위치
      itemStyle: { color: 'white' },
      lineStyle: { width: 1, type: 'solid' },
      smooth: true,
      symbol: 'circle', // 데이터 포인트 표시
      symbolSize: 0, // 크기 조정
    },
    {
      name: t('label.inbound'),
      type: 'line',
      data: chartData.barData2,
      itemStyle: { color: 'white' },
      lineStyle: { width: 1, type: 'dotted' },
      smooth: true,
      symbol: 'circle',
      symbolSize: 0,
    },
    {
      name: t('label.inbound'),
      type: 'line',
      data: chartData.barData3,
      itemStyle: { color: 'white' },
      lineStyle: { width: 1, type: 'dotted' },
      smooth: true,
      symbol: 'circle',
      symbolSize: 0,
    },
  ],
});
}

watch([()=>props.loading, ()=>optionsValueRef.value,()=>props.date], async ([newLoading, changedOptions,newDate])=>{
  // 만약 loading이 true일 경우에는 아무 동작도 하지 않음
if (newLoading) {
  return;
}
  await fetchData(changedOptions,newDate);

setOptionData();
});


</script>
<style scoped>

.custom-ribbon {
  position: absolute;
  background:  #00BFFF;;
  color: white;
  padding: 18px 20px;
  font-size: 14px;
  font-weight: bold;
  z-index: 1000; /* 가장 앞쪽에 배치 */
  clip-path: polygon(0% 0%, 100% 0%, 0% 100%, 0% 100%);
}
.chart-container {
  position: relative;
  z-index: 1; /* 차트를 가장 밑의 레이어로 */
  overflow: hidden;
  margin-top:-1.3rem;
}

.selectStyle{
  left:90%;
  top:0px;
  position: relative;
  z-index: 10;
  margin-top:-1rem;
}

</style>