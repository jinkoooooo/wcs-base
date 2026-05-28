<template>
  <div>
    <div class="custom-ribbon"></div>
    <!-- 입하 & 품질검사 현황 -->
  <Card
    :loading="loading"
    ref="cardRef"
    :title="t('label.inbound_summary_quality_inspection_status')"
    :headStyle="headStyleRef"
    :bodyStyle="bodyStyleRef"
    :style="styleRef"
  >
    <div class="card-content">
      <div class="top-info">
        <div class="info-item">{{ t('label.inbound_summary') }} ( {{proceedData}} / {{ countData }} - {{ t('label.batch_number') }} )</div>
        <div class="info-item">Inb No : {{ inbNo }}</div>
        <div class="info-item"> {{ t('label.mapping_complete_pallet_qty') }} : {{ mappingPltQty }}</div>
      </div>

      <div>
        <div class="circle-container">
          <div
              v-for="(item, index) in items"
              :key="`${index}`"
              class="circle-inner-container"
            >
            <div :style=localeStyleRef >{{ item.title }}</div>
            <hr class="hr-style">
            <div class="circle" :style="circleStyleRef(item)">{{ item.data }}</div>
          </div>
        </div>
      </div>
    </div>
  </Card>
  </div>
</template>

<script lang="ts" setup>
  import { Card } from 'ant-design-vue';
  import { ref, type CSSProperties,watch,computed } from 'vue';
  import { useI18n } from '/@/hooks/web/useI18n';

  const { t ,locale} = useI18n();
  const props = defineProps({
    loading: Boolean,
    data: {
      type: Object,
      required: false
    },
    date: {
      type: String as PropType<string | null>,
      required: true
    }
  });
  
  //언어 추출 -> 언어에 따라 css 상이
  const koLocaleRef = ref('ko-KR');
  const isKorean = computed(() => locale.value === koLocaleRef.value);
  const loading = ref(false);


  const localeStyleRef = computed(() => ({
  textAlign: 'center',
  fontWeight: 'bold',
  marginTop: '0.3rem',
  fontSize: isKorean.value ? '0.6rem' : '0.4rem'
  }));
  const styleRef = ref({border: '1px solid #00BFFF',borderRadius:'0px',background:'#00386C',height:'9.95rem'})


  const headStyleRef = computed((): CSSProperties => ({
    textAlign: 'center',
    lineHeight: '0vw',
    minHeight: '0vw',
    fontSize: isKorean.value ? '0.9rem' : '0.8rem',
    backgroundColor: '#00386C',
    color: 'white',
    borderRadius: '0px',
    padding: '0px'
  }));
    
  const bodyStyleRef: CSSProperties = {
    backgroundColor: '#00386C',
    paddingTop: '0vh',
    paddingBottom:'0px',
    color:'white'
  };
  
  /**
   * 원 부분에 적용되는 스타일
   * @param item 원 안에 들어가는 데이터 객체 
   */
  const circleStyleRef=(item) =>{
    return {height:'2.5rem',fontSize:item.data>=1000?'0.5rem':'0.6rem',width:item.data>=1000 ? '3rem':'2.5rem'}
  }; 

  const proceedData = ref<number>(0);
  const countData = ref<number>(0);
  const mappingPltQty = ref<number>(0);
  const inbNo = ref<string>();

  const totalCnt = ref(0);
  const passedInspection = ref(0);
  const uninspected = ref(0);
  const mappingComplete = ref(0);

  const items = computed(() => {
  locale.value;
  return [
    {
      title: t('label.total_qty'),
      data: totalCnt.value
    },
    {
      title: t('label.mapping_complete'),
      data: mappingComplete.value
    },
    {
      title: t('label.passed_inspection'),
      data: passedInspection.value
    },
    {
      title: t('label.uninspected'),
      data: uninspected.value
    }
  ];
  });

  function setData(newData:any){
    proceedData.value =  newData.proceed_data;
    countData.value = newData.count_data;
    inbNo.value = newData.inb_no;
    mappingPltQty.value = newData.mapping_plt_qty;
    totalCnt.value = newData.total_cnt;
    passedInspection.value = newData.passed_inspection;
    inbNo.value = newData.inb_no;
    mappingComplete.value = newData.mapping_complete;
    uninspected.value = newData.uninspected;
  }

  watch([() => props.loading,()=>props.date,()=>props.data], async ([newLoading,newDate,newData]) => {
  if (props.loading) {
    return;
  }
  await setData(newData);

});



</script>

<style scoped>
.card-content {
  display: flex;
  flex-direction: column;
  padding-bottom:0.2rem;
}

.top-info {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.info-item {
  font-size: 0.73rem;
  margin-left: 3%;
}

.circle-container {
  display: flex;
  justify-content: space-between;
  /* gap: 0.5rem; 요소 간 간격 */
  overflow: hidden;
}

.circle-container > div {
  flex: 1;
  text-align: center;
  
}
.circle {
  width: 2.5rem;
  height: 2.5rem;
  border: 2px solid white;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 0.6rem;
  margin-top: 0.1rem; /* 제목과의 간격 */
}
.circle-inner-container{
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  height: 100%; /* 높이 균일화 */
}
hr {
  border: 0;
  border-top: 2px solid white;
  height: 1px;
  width: 200%;
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
.hr-style{
  padding: 0; 
  margin: 0.1rem 0px;
}
</style>
