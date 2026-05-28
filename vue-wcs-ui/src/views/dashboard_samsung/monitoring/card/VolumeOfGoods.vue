<template>
  <div class="card-wrapper">
    <div class="custom-ribbon"></div>
    <Card
      :loading="loading"
      ref="cardRef"
      :title="title"
      :headStyle="headStyleRef"
      :bodyStyle="bodyStyleRef"
      :style="styleRef"
      class="card-full"
    >
      <div class="card-inner">
        <div class="select-container">
          <Select
            v-model:value="optionsValueRef"
            :size="sizeRef"
            :options="selectOptionsRef"
            @change="valueChanged"
            class="selectStyle"
            :style="localeStyleRef"
          />
        </div>
        <div class="chart-container" ref="chartRef"></div>
      </div>
    </Card>
  </div>
</template>

<script lang="ts" setup>
  import { Ref, ref, watch, computed, onBeforeMount, type CSSProperties } from 'vue';
  import { Card, Select } from 'ant-design-vue';
  import type { SelectProps } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';

  const props = defineProps({
    loading: Boolean,
    data: { type: Object, required: true },
    date: { type: Object, required: true },
  });
  const emit = defineEmits();

  const title = '기간별 물동량 추이';

  const styleRef = computed(() => ({
    border: '1px solid var(--dashboard-primary-color)',
    borderRadius: '0',
    background: 'var(--dashboard-bg-color)',
    minHeight: '10.5rem',
    height: '100%',
    display: 'flex',
    flexDirection: 'column',
  }));

  const headStyleRef = ref<CSSProperties>({
    textAlign: 'center',
    lineHeight: '0.5vw',
    minHeight: '0vw',
    fontSize: '1.1rem',
    backgroundColor: 'var(--dashboard-bg-color)',
    color: 'white',
    borderRadius: '0px',
    padding: '0px',
  });

  const bodyStyleRef: CSSProperties = {
    backgroundColor: 'var(--dashboard-bg-color)',
    padding: '0.2rem 0.7rem 0.4rem 0.3rem',
    color: 'white',
    boxSizing: 'border-box',
    display: 'flex',
    flexDirection: 'column',
    flex: '1 1 auto',
  };

  const sizeRef = ref<SelectProps['size']>('small');
  const localeStyleRef = computed(() => ({ width: '4rem', padding: '0px', fontSize: '0.8rem' }));

  const optionsValueRef = ref<number>();
  const optionKeys = ['일별', '주별', '월별', '시간별'];
  const selectOptionsRef = computed(() =>
    optionKeys.map((key, index) => ({ value: index + 1, label: key })),
  );

  onBeforeMount(() => {
    optionsValueRef.value = selectOptionsRef.value[0]?.value;
  });
  function valueChanged(e: any) {
    optionsValueRef.value = e;
    emit('lastDays', e);
  }

  const dataRef = ref<any[]>([]);
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions, echarts } = useECharts(chartRef as Ref<HTMLDivElement>);

  function getLineData(data: any[]) {
    const chartDatas = data || [];
    const category: string[] = chartDatas.map(({ inbound_date }) => inbound_date);
    const barData: number[] = chartDatas.map(({ plan_qty }) => plan_qty);
    const barData2: number[] = chartDatas.map(({ pass_qty }) => pass_qty);
    const barData3: number[] = chartDatas.map(({ cntr_qty }) => cntr_qty);
    return { category, barData, barData2, barData3 };
  }

  async function fetchData(data: any) {
    dataRef.value = Array.isArray(data) ? data : [];
  }

  function setOptionData() {
    if (!dataRef.value.length) return;
    const chartData = getLineData(dataRef.value);

    setOptions({
      backgroundColor: 'transparent',
      grid: { left: '5%', right: '15%', top: '8%', bottom: '10%', containLabel: true },
      tooltip: { axisPointer: { type: 'line', label: { show: true, backgroundColor: '#333' } } },
      legend: {
        data: ['주문량', '처리량', '컨테이너'],
        textStyle: { color: 'white', fontSize: 12 },
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
          fontSize: dataRef.value.length > 15 ? 11 : 12,
          color: 'white',
          fontWeight: 'bold',
        },
      },
      yAxis: [
        {
          type: 'value',
          axisLine: { lineStyle: { color: 'white' } },
          splitLine: { show: false },
          axisLabel: { fontSize: 12, fontWeight: 'bold' },
        },
        {
          type: 'value',
          axisLine: { lineStyle: { color: 'white' } },
          splitLine: { show: false },
          position: 'right',
          axisLabel: { fontSize: 12, fontWeight: 'bold' },
        },
      ],
      series: [
        {
          name: '주문량',
          type: 'bar',
          barWidth: dataRef.value.length === 7 ? 20 : 10,
          itemStyle: {
            borderRadius: 0,
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 1, color: '#90ee90' },
            ]),
          },
          data: chartData.barData,
        },
        {
          name: '처리량',
          type: 'bar',
          barWidth: dataRef.value.length === 7 ? 20 : 10,
          itemStyle: {
            borderRadius: 0,
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#49feff' },
            ]),
          },
          data: chartData.barData2,
        },
        {
          name: '컨테이너',
          type: 'line',
          yAxisIndex: 1,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#FBB6C1' },
            ]),
          },
          lineStyle: { width: 2 },
          smooth: true,
          symbol: 'circle',
          data: chartData.barData3,
        },
      ],
    });
  }

  watch(
    [() => props.loading, () => optionsValueRef.value, () => props.date, () => props.data],
    async ([newLoading, _changedOptions, _newDate, newData]) => {
      if (newLoading) return;
      await fetchData(newData);
      setOptionData();
    },
    { immediate: true },
  );
</script>

<style scoped>
  .card-wrapper {
    height: 100%;
    display: flex;
    flex-direction: column;
  }

  .card-full {
    flex: 1 1 auto;
    display: flex;
    flex-direction: column;
  }

  .card-inner {
    flex: 1 1 auto;
    display: flex;
    flex-direction: column;
    position: relative; /* 자식 absolute 위치 기준 */
  }

  .chart-container {
    flex: 1 1 auto;
    width: 100%;
    height: 100%;
    position: relative;
    z-index: 1;
    overflow: hidden;
  }

  .custom-ribbon {
    position: absolute;
    background: var(--dashboard-primary-color);
    color: white;
    padding: 18px 20px;
    font-size: 14px;
    font-weight: bold;
    z-index: 1000;
    clip-path: polygon(0% 0%, 100% 0%, 0% 100%, 0% 100%);
  }

  .select-container {
    position: absolute;
    /* [수정] 헤더 라인과 겹치지 않도록 top을 10px로 내려서 여백 확보 */
    right: 10px;
    top: 10px;
    z-index: 10;
  }

  .selectStyle {
    z-index: 10;
  }
</style>
