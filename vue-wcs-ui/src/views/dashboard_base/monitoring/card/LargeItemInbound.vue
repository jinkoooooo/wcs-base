<template>
    <div>
    <div class="custom-ribbon"></div>
  <!-- 자재 입고 -->
<Card
  :loading="loading"
  ref="cardRef"
  :title=" t('label.large_item_inbound')"
  :headStyle="headStyleRef"
  :bodyStyle="bodyStyleRef"
  :style = "styleRef"
>
<div class="content" :style =localeContentStyleRef> 
    <!-- CHART AREA -->
    <div class="box" :style=localeBoxStyleRef  >
      <div ref="chartRef" :style="{ height:chartHeight}" ></div>
    </div>
    <!-- CONTENT AREA -->
    <div class="box box-in-content-style"  >
      <div>
        <div :style="titleLocaleStyleRef">{{ t('label.summary') }}</div>
        <div :style="localeStyleRef">{{t('label.input_instruction')  }} : {{inputInstruction  }}</div>
        <div :style="localeStyleRef">{{t('label.input_completed')  }} : {{ inputCompleted }}</div>
      </div>

      <div class="box-in-content-style">
        <div :style="titleLocaleStyleRef">{{ t('label.detail_information') }}</div>
        <div  :style="localeStyleRef">Motor Line : {{ motorLineQty }}</div>
        <div  :style="localeStyleRef">Stator Line : {{ statorLineQty }}</div>
        <div  :style="localeStyleRef">Manual {{ t('label.transfer') }} : {{ manualTransferQty }}</div>
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
const inputInstruction = ref(0);
const inputCompleted = ref(0);
const motorLineQty = ref(0);
const statorLineQty = ref(0);
const manualTransferQty = ref(0);
const percentRef = ref(0);


/************* 스타일 *************/
const styleRef = computed(() => ({
  marginTop:isKorean.value ? null:'0.1rem',border: '1px solid #00BFFF',background:'#00386C',borderRadius:'0',height:'9.94rem'}));
  
const headStyleRef: CSSProperties = {
  textAlign:'center',lineHeight:'0vw',minHeight:'0vw',fontSize:'0.9rem',backgroundColor:'#00386C',color:'white',borderRadius:'0px',padding:'0px'};
   
const bodyStyleRef: CSSProperties = {
   backgroundColor: '#00386C',
   color:'white',
   paddingTop:'0px',
   paddingBottom:'0px',  };

const localeStyleRef = computed(() => ({
    fontSize:isKorean.value ? '0.6rem':'0.5rem',marginTop:isKorean.value ? '':'0.1rem' 
}));

const localeContentStyleRef = computed(() => ({
  gap: isKorean.value ? '10px' : '15px'
}));

const chartHeight=ref('7rem');

const titleLocaleStyleRef = computed(() => ({
  fontWeight: 'bold', fontSize: isKorean.value ? '0.7rem':'0.6rem'}));

const localeBoxStyleRef = computed(() => ({
  marginTop : isKorean.value ? '0rem':'0.4rem'
}));



/************* 그 외 *************/
//언어 추출 -> 언어에 따라 css 상이
const koLocaleRef = ref('ko-KR');
const isKorean = computed(() => locale.value === koLocaleRef.value);
const chartRef = ref<HTMLDivElement | null>(null);
const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);
const loading = ref(false);  



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

onMounted(async()=>{
  setOptionsData(null);
})

/**
 * 새로운 데이터 바인딩
 * @param newData 부모컴포넌트를 통해 받는 데이터
 */
function setData(newData:any){
  inputInstruction.value = newData.input_instruction;
  inputCompleted.value= newData.input_completed;
  motorLineQty.value= newData.motor_line_qty;
  statorLineQty.value= newData.stator_line_qty;
  manualTransferQty.value=newData.manual_transfer_qty;
  percentRef.value = newData.percent;
  }



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
          color: '#90ee90',
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
        color: '#90ee90',
        formatter: '{value}%',
      },
      radius: '100%'
    }
  ]
});
  }


watch([() => props.loading,()=>props.data], async ([newLoading,newData]) => {
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
.box{
  min-width: 50%;
}

.box-in-content-style{
  margin-top:0.2rem;
}

</style>
