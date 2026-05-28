<template>
  <div>
    <div class="custom-ribbon"></div>

    <Card
      :loading="loading"
      :title="t('label.warehouse_operate_rate')"
      :headStyle="headStyleRef"
      :bodyStyle="bodyStyleRef"
      :style="styleRef"
    >
      <div class="card-grid">
        
        <!-- CHART AREA -->
        <div class="a">
          <div
            ref="chartRef"
            :style="{ height: chartHeight, marginTop: '1rem' }"
          ></div>

        <!-- CHART SUMMARY DATA -->
          <div v-if="props.data">
            <div :style="chartSummaryStyleRef">
              {{ t('label.loading_rate') }}(Total) : {{ loadingCnt }} / {{ totalCnt }}
            </div>
            <div :style="chartSummaryStyleRef">
              ({{ t('label.good_stock') }} : {{ normalInventory }} /
              {{ t('label.on_hold_stock') }} : {{ pendingInventory }})
            </div>
          </div>
        </div>

      <!-- PROGRESS BAR AREA -->
        <div class="b">
          <div v-if="props.data" style="text-align: left;">
            <div
              v-for="(chart, i) in chartData"
              :key="i"
              class="text-style"
            >
              <div style="font-size: 0.5rem;">
                {{ chart.plt_name }}
              </div>
              <Progress
                :percent="chart.percent"
                :showInfo="false"
                :strokeWidth="30"
                :style="progressStyle"
                strokeColor="#03ffff"
                trailColor="#c1ffff"
                :format="(percent) => `${percent}%  ${chart.data}`"
              />
              <div style="font-size: 0.5rem; width: 40px;">
                {{ chart.percent }}% ({{ chart.data }})
              </div>
            </div>
          </div>
        </div>
      </div>
    </Card>
  </div>
</template>



<script lang="ts" setup>
import { Card, Progress } from 'ant-design-vue';
import { PropType, ref, Ref, computed } from 'vue';
import { useECharts } from '/@/hooks/web/useECharts';
import { watch } from 'vue';
import { type CSSProperties } from 'vue';
import { useI18n } from '/@/hooks/web/useI18n';

const { t, locale } = useI18n();

const props = defineProps({
  loading: Boolean,
  width: {
    type: String as PropType<string>,
    default: '200px',
  },
  height: {
    type: String as PropType<string>,
  },
  data: {
    type: Object,
    require: false,
  },
  date: {
    type: String as PropType<string | null>,
    required: true,
  },
});

/************* 바인딩 변수 *************/
const normalInventory = ref<number>(0);
const loadingCnt = ref<number>(0);
const totalCnt = ref<number>(0);
const pendingInventory = ref<number>(0);
const gaugeDataValue = ref();


/************* 스타일 *************/

//카드컴포넌트 관련 스타일
const styleRef = ref({
  border: '1px solid #00BFFF',
  borderRadius: '0px',
  backgroundColor: '#00386C',
  height: '15.20rem',
});

//카드 헤더 스타일 적용
const headStyleRef = ref<CSSProperties>({
  textAlign: 'center',
  lineHeight: '0vw',
  minHeight: '0vw',
  fontSize: '0.9rem',
  backgroundColor: '#00386C',
  color: 'white',
  borderRadius: '0px',
  padding: '0px',
});

//카드 바디 스타일 적용
const bodyStyleRef: CSSProperties = {
  backgroundColor: '#00386C',
  paddingTop: '0vh',
  paddingBottom: '2px',
  padding: '0px',
  height: '10rem',
};

//프로그래스바 스타일
const progressStyle = ref({
  height: '2rem',
  width: '130px',
  color: '#ffffff',
  borderRadius: '0px',
});

//차트 스타일 : 높이만(너비는 자동 조정됨)
const chartHeight = ref('8rem');

//언어 추출 -> 언어에 따라 css 상이
const koLocaleRef = ref('ko-KR');
const isKorean = computed(() => locale.value === koLocaleRef.value);

//차트 아래 스타일 적용
const chartSummaryStyleRef = computed(() => ({
  textAlign: 'center',
  color: '#ffffff',
  fontSize: isKorean.value ? '0.65rem' : '0.4rem',
  marginTop: '0.2rem',
}));

/************* 그 외 *************/
const chartData = ref();
const chartRef = ref<HTMLDivElement | null>(null);

const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);

/**
 * echart 적용 메타데이터
 */
function setOptionsData(newData: any) {
  if (!newData) {
    setOptions({});
    return;
  }

  const gaugeData = [
    {
      value: gaugeDataValue.value,
      detail: {
        valueAnimation: true,
        offsetCenter: ['0%', '0%'],
      },
    },
  ];

  setOptions({
    grid: {
      top: 0,
      bottom: 0,
      left: 0,
      right: 0,
      containLabel: false,
    },
    series: [
      {
        type: 'gauge',
        startAngle: 90,
        endAngle: -270,
        pointer: {
          show: false,
        },
        progress: {
          show: true,
          overlap: false,
          roundCap: true,
          clip: false,
          itemStyle: {
            color: '#49feff',
            borderWidth: 0,
          },
        },
        axisLine: {
          lineStyle: {
            width: 10,
            color: [[1, '#555']],
          },
        },
        splitLine: {
          show: false,
        },
        axisTick: {
          show: false,
        },
        axisLabel: {
          show: false,
        },
        data: gaugeData,
        title: {
          show: false,
        },
        detail: {
          fontSize: 15,
          fontWeight: 'bold',
          color: '#49feff',
          formatter: '{value}%',
          offsetCenter: ['0%', '0%'],
        },
        radius: '100%',
      },
    ],
  });
}

/**
 * 새로운 데이터 바인딩
 * @param newData 부모컴포넌트를 통해 전달 받은 데이터
 */
function setData(data: any) {
  if (!data) {
    return;
  }

  chartData.value = data.chart_datas;

  const loadingData = data.loading_data;
  loadingCnt.value = loadingData.loading_cnt;
  totalCnt.value = loadingData.total_cnt;
  normalInventory.value = loadingData.normal_inventory;
  pendingInventory.value = loadingData.pending_inventory;
  gaugeDataValue.value = loadingData.percent;
}

watch([() => props.loading, () => props.data], async ([newLoading, newData]) => {
  if (newLoading) {
    return;
  }

  await setData(newData);
  setOptionsData(newData);
});
</script>

<style scoped>
.card-grid {
  display: flex;
  flex-direction: row; /* 가로로 배치 */
  gap:0.1rem;
}

.a {
  flex: 1; 
  display: grid;
  justify-content: center;
  height:100%;
}

.b {
  margin-top:1rem;
  height:100%;
  margin-right:0.1rem;
  flex: 1.2; 
  font-size: 0.6vw;
  border: none;
  display:flex;
}

.ant-progress-outer {
  border-radius: 0 !important;
}
.ant-progress-text {
  position: absolute;
  right: 0;
  top: 50%;
  font-size: 0.1rem; /* 크기를 조정 */
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
.ant-progress-text {
  color: white !important;
  font-size: 1vh;
}

.text-style{
  padding-bottom:0.9rem;display: flex; align-items: center; gap: 0.2rem;width:99%; color:#ffffff
}

</style>