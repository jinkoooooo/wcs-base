<template>
     <div>
    <div class="custom-ribbon"></div>
  <!-- 입하 & 품질검사 현황 -->
  <Card
    :loading="loading"
    ref="cardRef"
    :headStyle="headStyleRef"
    :bodyStyle="bodyStyleRef"
    :style="styleRef"
  >
        <div class="round-rectangle-container">
          <div v-html="title2Ref" style="text-align:center; font-size:0.7rem;text-align:center;width:3.5rem;margin-right:-5px;"></div>
          <div style="width:2px;background-color: white;"></div>
          <div v-for="(item, index) in items" :key="index" style="padding-left:0.2rem;" >
              <div class="roundRectangle">
                  <div  v-html="item.title" style="font-size:0.75rem; text-align:center;"> </div>  
                  <div v-html="item.data" style="text-align:center; font-size:0.8rem"  
                  :style="{fontSize:item.data.length>=11?'0.7rem':'0.6rem'}"></div>  
              </div>
          </div>
        </div>
  </Card>
  </div>
</template>

<script lang="ts" setup>
  import { Card } from 'ant-design-vue';
  import { ref, type CSSProperties,onMounted,watch,nextTick,computed } from 'vue';

  const styleRef = ref({border: '1px solid  #00BFFF',borderRadius:'0px'})
  const title2Ref = ref("입&nbsp;&nbsp;하<br>품&nbsp;&nbsp;질<br>검&nbsp;&nbsp;사<br>현&nbsp;&nbsp;황");
  const props = defineProps({
    loading: Boolean,
      data:{
        type:Object,
        require:true,
        default: () => ({}) 
      },
      date:{
        type:String as PropType<string|null>,
        required:true
      }
});

  const loading = ref(false);
  const headStyleRef: CSSProperties = {
    textAlign:'center',fontSize:'2vh',borderRadius:'0px'
  };
  const bodyStyleRef: CSSProperties = {
    backgroundColor: '#00386c',
    paddingTop: '2px',
    paddingBottom: '2px',
    paddingLeft:'1rem',
    paddingRight:'0.3rem',
    color:'white'
  };

  onMounted(async ()=>{
    nextTick(()=>{
      setData(props.data);
    })
  })
  const fontSizeRef = computed((fontSize)=>{
    return fontSize>=100 ? '0.5rem' : '0.6rem';
  })

  const qcTransferCountForTheDay = ref(0);
  const qcInspectionInProgress= ref(0);
  const qcInspectionPass= ref(0);
  const passStockCompleted= ref(0); 
  const qcInspectionRework = ref(0);
  const qcInspectionNg= ref(0);

  function setData(newData: any) {
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
  

  
const items = computed(() => {
  // props.data가 null 또는 undefined일 경우를 대비하여 기본값을 설정
  const data = props.data || {
    qc_transfer_count_for_the_day: 0,
    qc_inspection_in_progress: 0,
    qc_inspection_pass: 0,
    pass_stock_completed: 0,
    qc_inspection_ng: 0,
    qc_inspection_rework: 0
  };

  return [
    {
      title: '당일 QC<br> 이송 수',
      data: `${data.qc_transfer_count_for_the_day} <span style="font-size:0.65rem;">Pallet</span>`
    },
    {
      title: 'QC 검사<br> 진행 중',
      data: `${data.qc_inspection_in_progress} <span style="font-size:0.65rem;">Pallet</span>`
    },
    {
      title: 'QC 검사<br> PASS',
      data: `${data.qc_inspection_pass} <span style="font-size:0.65rem;">Pallet</span>`
    },
    {
      title: 'PASS<br>입고 완료',
      data: `${data.pass_stock_completed} <span style="font-size:0.65rem;">Pallet</span>`
    },
    {
      title: 'QC 검사<br>NG',
      data: `${data.qc_inspection_ng} <span style="font-size:0.65rem;">Pallet</span>`
    },
    {
      title: 'QC 검사<br>재입고',
      data: `${data.qc_inspection_rework} <span style="font-size:0.65rem;">Pallet</span>`
    }
  ];
});



  watch([() => props.loading,()=>props.date, ()=>props.data], async ([newLoading,newDate,newData]) => {
  // 만약 loading이 true일 경우에는 아무 동작도 하지 않음
  if (newLoading) {
    return;
  }
  await setData(newData);

},
);

  // 추가 내용 (예시로 내용 추가)
</script>

<style scoped>
.round-rectangle-container{
  display:flex;
  justify-content:space-between;
  overflow:hidden;
}
.roundRectangle {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width:5.4rem;
  height: 100%;
  border-radius: 12px;
  border: 2px solid white;
  }
  
.custom-ribbon {
  position: absolute;
  background:  #00BFFF;;
  color: white;
  padding: 18px 20px;
  font-weight: bold;
  z-index: 10;
  clip-path: polygon(0% 0%, 100% 0%, 0% 100%, 0% 100%)
}


</style>



