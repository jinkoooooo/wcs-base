<template>
    <div>
    <div class="custom-ribbon"></div>
    <!-- 자재 입고 -->
  <Card
    :loading="loading"
    ref="cardRef"
    :title="t('label.warehouse_inbound')"
    :headStyle="headStyleRef"
    :bodyStyle="bodyStyleRef"
    :style="styleRef"
  >
  <div class="content" :style =localeContentStyleRef> 
    
    <!-- CHART AREA -->
    <div class="box" :style=localeBoxStyleRef  >      
        <div ref="chartRef" :style="{  height:chartHeight}" ></div>
      </div>

      <!-- CONTENT AREA -->
      <div class="box box-in-content-style"  >
        <div>
          <div :style="titleLocaleStyleRef">{{ t('label.summary') }}</div>
          <div  :style="localeStyleRef">{{ t('label.scheduled_inbound') }} : {{ scheduledInbound }}</div>
          <div :style="localeStyleRef">{{ t('label.finish_inbound') }} : {{ finishInbound }}</div>
        </div>

        <div class="box-in-content-style">
          <div :style="titleLocaleStyleRef">{{ t('label.detail_information') }}</div>
          <div :style="localeStyleRef">{{ t('label.normal_inbound') }} : {{ normalInbound }}</div>
          <div :style="localeStyleRef">{{ t('label.manual_inbound') }} : {{ manualInbound }}</div>
          <div :style="localeStyleRef">{{ t('label.cancel_inbound') }} :  {{ canceledInbound }}</div>
        </div>
      </div>
      </div>
  </Card>
  </div>
</template>

<script lang="ts" setup>
import { Card } from 'ant-design-vue';
import { ref, type CSSProperties,Ref,watch,computed } from 'vue';
import { useECharts } from '/@/hooks/web/useECharts';
import { onMounted } from 'vue';
import { useI18n } from '/@/hooks/web/useI18n';

const { t,locale } = useI18n();
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

/************* 바인딩 변수 *************/
const scheduledInbound = ref(0);
const finishInbound= ref(0);
const normalInbound= ref(0);
const manualInbound= ref(0);
const canceledInbound= ref(0);
const percentRef= ref(0.0);

//언어 추출 -> 언어에 따라 css 상이
const koLocaleRef = ref('ko-KR');
const isKorean = computed(() => locale.value === koLocaleRef.value);


/************* 스타일 *************/
const titleLocaleStyleRef = computed(() => ({
  fontWeight: 'bold',
  fontSize: locale.value === isKorean ? '0.7rem' : '0.6rem'
}));
const chartHeight=ref('7rem');

const localeStyleRef = computed(() => ({
  fontSize: isKorean.value ? '0.6rem' : '0.5rem',
  marginTop: isKorean.value? '' : '0.1rem'
}));

const localeContentStyleRef = computed(() => ({
  gap: isKorean.value ? '10px' : '15px'
}));

const localeBoxStyleRef = computed(() => ({
  marginTop: isKorean.value ? '0rem' : '0.4rem'
}));

const styleRef = ref({border: '1px solid #00BFFF',borderRadius:'0px',backgroundColor:'#00386C',height:'9.95rem'})
  
const chartRef = ref<HTMLDivElement | null>(null);

const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);

const loading = ref(false);


const headStyleRef: CSSProperties = {
  textAlign:'center',lineHeight:'0vw',minHeight:'0vw',fontSize:'0.9rem',backgroundColor:'#00386C',color:'white',borderRadius:'0px',padding:'0px'
};
const bodyStyleRef: CSSProperties = {
  backgroundColor: '#00386C',
  color:'white',
  paddingTop:'0px',
  paddingBottom:'0px',
};

onMounted(async()=>{
  setOptionsData(null);
})

/**
 * 새로운 데이터 바인딩
 * @param newData 부모컴포넌트를 통해 받는 데이터
 */
function setData(newData:any){
  scheduledInbound.value =  newData.scheduled_inbound;
  finishInbound.value = newData.finish_inbound;
  normalInbound.value = newData.normal_inbound;
  manualInbound.value = newData.manual_inbound;
  canceledInbound.value = newData.canceled_inbound;
  percentRef.value = newData.percent;
}
  
/**
 * echart에 바인딩 될 데이터  
 */
const gaugeData = computed(() => [
  {
    value: percentRef.value,
    detail: {
      valueAnimation: true,
      offsetCenter: ['0%', '0%']
    }
  }
]);


/**
 * echart 적용 메타데이터
 */
function setOptionsData(newData:any){
setOptions({
series: [
  {
    type: 'gauge',
    startAngle: 90,
    endAngle: -270,
    pointer: {
      show: false
    },
    progress: {
      show: true,
      overlap: false,
      roundCap: true,
      clip: false,
      itemStyle: {
        color: '#FFB6C1', // 핑크 계열 색상
        borderWidth: 0 // 테두리 제거
      }
    },
    axisLine: {
      lineStyle: {
        width: 10,
        color: [[1, '#555']] // 배경 색상을 어두운 회색으로 설정
      }
    },
    splitLine: {
      show: false
    },
    axisTick: {
      show: false
    },
    axisLabel: {
      show: false
    },
    data: gaugeData,
    title: {
      show: false
    },
    detail: {
      fontSize: 18,
      fontWeight: 'bold',
      color: '#FFB6C1',
      formatter: '{value}%',
    },
    radius: '100%'
  }
]
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
  gap:5px;
}
.box{
  min-width: 50%;
  
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

.box-in-content-style{
  margin-top:0.2rem;
}
</style>
