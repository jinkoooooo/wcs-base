<template>
  <div class="inbound-status-root">
    <div class="custom-ribbon"></div>
    <Card
      class="inbound-status-card"
      :loading="loading"
      ref="cardRef"
      :title="title"
      :headStyle="headStyleRef"
      :bodyStyle="bodyStyleRef"
      :style="styleRef"
    >
      <div class="content" :style="localeContentStyleRef">
        <div class="box box-chart" :style="localeBoxStyleRef">
          <div ref="chartRef" class="chart" :style="{ height: chartHeight }"></div>
          <div class="chart-footer"> {{ completedBoxQty }} / {{ inboundPlanBoxQty }} </div>
        </div>

        <div class="box box-in-content-style">
          <div>
            <div :style="titleLocaleStyleRef">컨테이너</div>
            <div :style="localeStyleRef"> 입고 예정 : {{ inboundPlanCntrQty }} </div>
            <div :style="localeStyleRef"> 입고 완료 : {{ completedCntrQty }} </div>
          </div>

          <div class="box-in-content-style" :style="{ marginTop: '1.1rem' }">
            <div :style="titleLocaleStyleRef">박스</div>
            <div :style="localeStyleRef"> 입고 예정 : {{ inboundPlanBoxQty }} </div>
            <div :style="localeStyleRef"> 설비 완료 : {{ autoCompletedQty }} </div>
            <div :style="localeStyleRef"> 수동 완료 : {{ manualCompletedQty }} </div>
            <div :style="localeStyleRef"> NG 박스 : {{ ngBoxQty }} </div>
          </div>
        </div>
      </div>
    </Card>
  </div>
</template>

<script lang="ts" setup>
  import { Card } from 'ant-design-vue';
  import { ref, type CSSProperties, Ref, watch, computed, onMounted } from 'vue';
  import { useECharts } from '/@/hooks/web/useECharts';

  /* props 및 로직 기존과 동일 */
  const props = defineProps({
    loading: Boolean,
    data: { type: Object, required: false },
    date: { type: Object, required: true },
  });

  const inboundPlanCntrQty = ref(0);
  const completedCntrQty = ref(0);
  const inboundPlanBoxQty = ref(0);
  const autoCompletedQty = ref(0);
  const manualCompletedQty = ref(0);
  const ngBoxQty = ref(0);
  const completedBoxQty = ref(0);
  const percentRef = ref(0.0);

  const title = '당일 입고 총 현황';

  /* 스타일은 그대로 유지 */
  const titleLocaleStyleRef = computed(() => ({
    fontWeight: 'bold',
    fontSize: '1.2rem',
    marginBottom: '0.1rem',
  }));
  const chartHeight = ref('11rem');
  const localeStyleRef = computed(() => ({
    fontSize: '0.9rem',
    marginTop: '0.2rem',
  }));
  const localeContentStyleRef = computed(() => ({
    gap: '40px',
    alignItems: 'center',
  }));
  const localeBoxStyleRef = computed(() => ({
    marginTop: '0.3rem',
  }));
  const styleRef = ref({
    border: '1px solid var(--dashboard-primary-color)',
    borderRadius: '0px',
    backgroundColor: 'var(--dashboard-bg-color)',
    minHeight: '12rem',
    height: '100%',
  });

  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);
  const headStyleRef: CSSProperties = {
    textAlign: 'center',
    lineHeight: '0.5vw',
    minHeight: '0vw',
    fontSize: '1.1rem',
    backgroundColor: 'var(--dashboard-bg-color)',
    color: 'white',
    borderRadius: '0px',
    padding: '0px',
  };
  const bodyStyleRef: CSSProperties = {
    backgroundColor: 'var(--dashboard-bg-color)',
    color: 'white',
    paddingTop: '0px',
    paddingBottom: '0.4rem',
  };

  function resetData() {
    inboundPlanCntrQty.value = 0;
    completedCntrQty.value = 0;
    inboundPlanBoxQty.value = 0;
    autoCompletedQty.value = 0;
    manualCompletedQty.value = 0;
    ngBoxQty.value = 0;
    completedBoxQty.value = 0;
    percentRef.value = 0;
  }

  function setData(newData: any) {
    if (!newData) {
      resetData();
      return;
    }
    inboundPlanCntrQty.value = newData.inbound_plan_cntr_qty ?? 0;
    completedCntrQty.value = newData.completed_cntr_qty ?? 0;
    inboundPlanBoxQty.value = newData.inbound_plan_box_qty ?? 0;
    autoCompletedQty.value = newData.auto_completed_qty ?? 0;
    manualCompletedQty.value = newData.manual_completed_qty ?? 0;
    ngBoxQty.value = newData.ng_box_qty ?? 0;
    const totalCompletedBox = autoCompletedQty.value + manualCompletedQty.value;
    completedBoxQty.value = totalCompletedBox;
    const planBox = inboundPlanBoxQty.value;
    if (planBox > 0) {
      const calculatedPercent = (totalCompletedBox / planBox) * 100;
      percentRef.value = Math.round(calculatedPercent * 10) / 10;
    } else {
      percentRef.value = 0;
    }
  }

  const gaugeData = computed(() => [
    {
      value: percentRef.value,
      detail: { valueAnimation: true, offsetCenter: ['0%', '0%'] },
    },
  ]);

  function setOptionsData() {
    setOptions({
      tooltip: { show: false },
      series: [
        {
          type: 'gauge',
          startAngle: 90,
          endAngle: -270,
          pointer: { show: false },
          progress: {
            show: true,
            overlap: false,
            roundCap: true,
            clip: false,
            itemStyle: { color: '#FFB6C1', borderWidth: 0 },
          },
          axisLine: { lineStyle: { width: 18, color: [[1, '#555']] } },
          splitLine: { show: false },
          axisTick: { show: false },
          axisLabel: { show: false },
          data: gaugeData.value,
          title: { show: false },
          detail: {
            fontSize: 32,
            fontWeight: 'bold',
            color: '#FFB6C1',
            formatter: '{value}%',
          },
          radius: '100%',
        },
      ],
    });
  }

  onMounted(() => {
    setOptionsData();
  });
  watch(
    [() => props.loading, () => props.date, () => props.data],
    async ([newLoading, _newDate, newData]) => {
      if (newLoading) return;
      await setData(newData);
      setOptionsData();
    },
  );
</script>

<style scoped>
  /* CSS 기존과 동일 */
  .inbound-status-root {
    height: 100%;
    display: flex;
    flex-direction: column;
  }
  .inbound-status-card {
    flex: 1 1 auto;
    display: flex;
    flex-direction: column;
  }
  .content {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    flex: 1 1 auto;
    align-items: center;
  }
  .box {
    min-width: 50%;
  }
  .box-chart {
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  .chart {
    width: 100%;
  }
  .chart-footer {
    margin-top: 0.8rem;
    margin-bottom: 1.2rem;
    font-size: 1.2rem;
    font-weight: bold;
    color: #ffffff;
    text-align: center;
  }
  .custom-ribbon {
    position: absolute;
    background: var(--dashboard-primary-color);
    color: white;
    padding: 18px 20px;
    font-size: 14px;
    font-weight: bold;
    z-index: 10;
    clip-path: polygon(0% 0%, 100% 0%, 0% 100%, 0% 100%);
  }
  .box-in-content-style {
    margin-top: 0.2rem;
  }
</style>
