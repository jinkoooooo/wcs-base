<template>
      <div>
    <div class="custom-ribbon"></div>
  <!-- 입하 & 품질검사 현황 -->
  <Card
    :loading="loading"
    ref="cardRef"
    :title="titleRef"
    :headStyle="headStyleRef"
    :bodyStyle="bodyStyleRef"
    :style="styleRef"
  >
    <div v-if="$props.data" style="margin-left:20px;">
      <div v-for="(d ,i) in props.data" :key="i">
        <div>{{ d.date }}</div>
        <div>{{ d.content}}</div>
      </div>
    </div>
    <div v-else style="height:8rem;"></div>

  </Card>
</div>

</template>

<script lang="ts" setup>
  import { Card } from 'ant-design-vue';
  import { ref, type CSSProperties } from 'vue';
  const styleRef = ref({background:'black',border: '1px solid #00BFFF',borderRadius:'0px',height:'9.5rem',overflow:'auto'})
  const loading = ref(false);
  const titleRef = '실시간 현황 로그';
  const headStyleRef: CSSProperties = {
    textAlign:'center',lineHeight:'0vw',minHeight:'0vw',fontSize:'0.9rem',backgroundColor:'#00386C',color:'white',borderRadius:'0px',padding:'0px'
  };
  
  const bodyStyleRef: CSSProperties = {
    backgroundColor: '#00386C',
    padding: '0px',
    overflow: 'auto',
    color:'white',
    whiteSpace: "nowrap",
    overflowX: "auto",
    fontSize:'0.7rem'
  };


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

</script>

<style scoped>
.circle-container{
  display:flex;
  justify-content:space-between;
  margin-top:5px;
  overflow:hidden;
}
.circle {
  margin-top:2px;
      width: 7vh;
      height: 7vh;
      border: 3px solid white;  /* 선의 두께와 색 설정 */
      border-radius: 50%;  /* 원형으로 만들기 */
      display: flex;
      justify-content: center;
      align-items: center;
    }
    hr {
      border: 0; /* 기존 테두리 없애기 */
      border-top: 2px solid white; /* 선의 두께와 색 지정 */
      height: 1px; /* 높이 설정 */
      width: 200%; /* 너비를 100%로 지정 */
    }

.custom-ribbon {
  position: absolute;
  background:  #00BFFF;;
  color: white;
  padding: 18px 20px;
  font-size: 14px;
  font-weight: bold;
  z-index: 10;
  clip-path: polygon(0% 0%, 100% 0%, 0% 100%, 0% 100%)
}
</style>



