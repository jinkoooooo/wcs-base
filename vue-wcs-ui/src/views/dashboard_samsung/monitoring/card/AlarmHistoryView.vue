<template>
  <div class="card-wrapper">
    <div class="custom-ribbon"></div>

    <Card
      :loading="loading"
      :title="cardTitle"
      class="card-full"
      :headStyle="headStyleRef"
      :bodyStyle="bodyStyleRef"
      :style="styleRef"
    >
      <div class="card-inner">
        <div class="select-container">
          <Select
            v-model:value="selectedTimeUnit"
            :size="sizeRef"
            :options="timeOptions"
            @change="handleUnitChange"
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
  import { Ref, ref, watch, computed, type CSSProperties } from 'vue';
  import { Card, Select } from 'ant-design-vue';
  import type { SelectProps } from 'ant-design-vue';
  import { useECharts } from '/@/hooks/web/useECharts';

  const props = defineProps({
    loading: Boolean,
    data: {
      type: Array,
      default: () => [],
    },
    date: Object,
  });

  /* --- 상태 관리 --- */
  const selectedTimeUnit = ref<string>('30');
  const sizeRef = ref<SelectProps['size']>('small');

  const timeOptions = [
    { value: '30', label: '30분' },
    { value: '60', label: '1시간' },
    { value: '180', label: '3시간' },
  ];

  const cardTitle = computed(() => `시간당 처리량 (${selectedTimeUnit.value}분 단위)`);

  /* --- 스타일 --- */
  const styleRef = computed(() => ({
    border: '1px solid var(--dashboard-primary-color)',
    borderRadius: '0',
    background: 'var(--dashboard-bg-color)',
    height: '100%',
    display: 'flex',
    flexDirection: 'column',
  }));

  const headStyleRef = computed(
    (): CSSProperties => ({
      textAlign: 'center',
      lineHeight: '0.5vw',
      minHeight: '0vw',
      fontSize: '1.1rem',
      backgroundColor: 'var(--dashboard-bg-color)',
      color: 'white',
      borderRadius: '0px',
      padding: '0.4rem 0.7rem 0 0.9rem',
    }),
  );

  const bodyStyleRef: CSSProperties = {
    backgroundColor: 'var(--dashboard-bg-color)',
    padding: '0.2rem 0.5rem 0.5rem 0.5rem',
    color: 'white',
    boxSizing: 'border-box',
    display: 'flex',
    flexDirection: 'column',
    flex: '1 1 auto',
  };

  const localeStyleRef = computed(() => ({
    width: '5rem',
    padding: '0px',
    fontSize: '0.8rem',
    textAlign: 'center',
  }));

  /* --- 차트 설정 --- */
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions, echarts } = useECharts(chartRef as Ref<HTMLDivElement>);

  /**
   * ✅ [최종 로직] 09:00 고정 시작 + 데이터 정밀 매핑
   */
  function getChartData(rawData: any[], unit: string) {
    const dataMap: Record<string, number> = {};

    // 1. 데이터 파싱
    const cleanData = JSON.parse(JSON.stringify(rawData || []));

    if (Array.isArray(cleanData)) {
      cleanData.forEach((item: any) => {
        // 키 값 찾기 (time_slot 우선)
        let tKey = item.time_slot || item.timeSlot || item.TIME_SLOT || '';
        const qty = Number(item.qty || 0);

        if (tKey && typeof tKey === 'string') {
          const parts = tKey.trim().split(':');
          if (parts.length >= 2) {
            // "09:05" -> "09:05" (숫자 변환 후 패딩으로 포맷 통일)
            const h = Number(parts[0]);
            const m = Number(parts[1]);
            // 분 단위를 30분 단위로 내림 처리 (안전장치)
            const mBucket = m < 30 ? 0 : 30;

            const normalizedKey = `${String(h).padStart(2, '0')}:${String(mBucket).padStart(
              2,
              '0',
            )}`;

            dataMap[normalizedKey] = (dataMap[normalizedKey] || 0) + qty;
          }
        }
      });
    }

    const category: string[] = [];
    const values: number[] = [];

    // 🚨 [고정] 무조건 09:00 부터 시작 (깔끔한 UI 유지)
    let currentH = 9;
    let currentM = 0;

    // 2. 시간 루프 (23:30까지)
    while (true) {
      if (currentH > 23 || (currentH === 23 && currentM > 30)) break;

      const timeLabel = `${String(currentH).padStart(2, '0')}:${String(currentM).padStart(2, '0')}`;

      let sumQty = 0;
      const steps = parseInt(unit) / 30;

      for (let i = 0; i < steps; i++) {
        let tempM = currentM + i * 30;
        let tempH = currentH + Math.floor(tempM / 60);
        tempM = tempM % 60;

        // 검색 키 생성 ("11:30")
        const checkKey = `${String(tempH).padStart(2, '0')}:${String(tempM).padStart(2, '0')}`;

        sumQty += dataMap[checkKey] || 0;
      }

      category.push(timeLabel);
      values.push(sumQty);

      currentM += parseInt(unit);
      currentH += Math.floor(currentM / 60);
      currentM = currentM % 60;
    }

    return { category, values };
  }

  function setOptionData() {
    const { category, values } = getChartData(props.data, selectedTimeUnit.value);

    setOptions({
      backgroundColor: 'transparent',

      // ✅ [디자인 개선] 툴팁 스타일 업그레이드
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: 'rgba(0, 0, 0, 0.7)', // 반투명 검정 배경
        borderColor: '#00bfff', // 테두리 색상
        textStyle: { color: '#fff' }, // 글자 색상
        // 커스텀 포맷터: 동그라미 마커 + 텍스트
        formatter: (params: any) => {
          const item = params[0];
          if (!item) return '';
          // 마커(색상 점) 생성
          const marker = `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:10px;height:10px;background-color:${
            item.color.colorStops ? item.color.colorStops[0].color : item.color
          };"></span>`;
          return `${item.axisValueLabel}<br/>${marker} 처리량: <strong>${item.value}</strong> Box`;
        },
      },

      grid: { left: '3%', right: '4%', top: '15%', bottom: '5%', containLabel: true },
      xAxis: {
        type: 'category',
        data: category,
        axisLine: { lineStyle: { color: 'white' } },
        axisLabel: {
          color: 'white',
          fontSize: 11,
          fontWeight: 'bold',
          hideOverlap: true,
        },
        axisTick: { alignWithLabel: true },
      },
      yAxis: {
        type: 'value',
        name: '(Box)',
        minInterval: 1,
        nameTextStyle: { color: '#aaa', padding: [0, 0, 0, -20], fontSize: 12 },
        axisLine: { lineStyle: { color: 'white' } },
        splitLine: { show: true, lineStyle: { color: 'rgba(255, 255, 255, 0.1)', type: 'dashed' } },
        axisLabel: { fontSize: 12, fontWeight: 'bold' },
      },
      series: [
        {
          name: 'UPH',
          type: 'bar',
          barWidth: '40%',
          data: values,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#00bfff' },
              { offset: 1, color: '#002B55' },
            ]),
            borderRadius: [5, 5, 0, 0],
          },
          label: {
            show: true,
            position: 'top',
            color: '#49feff',
            fontSize: 12,
            fontWeight: 'bold',
            formatter: (params: any) => {
              return params.value > 0 ? params.value : '';
            },
          },
        },
        // ... (Trend 라인 등 기존 코드 유지)
        {
          name: 'Trend',
          type: 'line',
          data: values,
          smooth: true,
          symbol: 'none',
          lineStyle: { color: '#90ee90', width: 2, type: 'dotted' },
        },
      ],
    });
  }

  function handleUnitChange() {
    setOptionData();
  }

  watch(
    [() => props.loading, () => props.data],
    async ([newLoading]) => {
      if (newLoading) return;
      setOptionData();
    },
    { immediate: true, deep: true },
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
    height: 100%;
    position: relative;
  }
  .chart-container {
    flex: 1 1 auto;
    width: 100%;
    height: 100%;
    min-height: 150px;
  }
  .custom-ribbon {
    position: absolute;
    background: var(--dashboard-primary-color);
    z-index: 10;
    width: 35px;
    height: 35px;
    clip-path: polygon(0% 0%, 100% 0%, 0% 100%);
  }
  .select-container {
    position: absolute;
    right: 0.5rem;
    top: 0.2rem;
    z-index: 100;
  }
  .selectStyle {
    z-index: 100;
  }
</style>
