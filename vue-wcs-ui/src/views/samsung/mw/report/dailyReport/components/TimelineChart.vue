<template>
  <div class="timeline-chart-wrap">
    <div class="rounded-3xl bg-white shadow-sm ring-1 ring-slate-200 p-4">
      <div class="flex items-start justify-between gap-3 mb-3">
        <div>
          <h3 class="font-semibold tracking-tight">컨테이너별 공정 타임라인</h3>
          <p class="text-[11px] md:text-xs text-slate-500 mt-1">가로: {{ rangeLabel }}</p>
        </div>

        <div class="text-[11px] md:text-xs text-slate-500 whitespace-nowrap">30분 단위</div>
      </div>

      <!-- 공정 색상 가이드 -->
      <div
        class="mb-3 flex flex-wrap items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2"
      >
        <div
          v-for="item in legendItems"
          :key="item.key"
          class="inline-flex items-center gap-2 text-[12px] text-slate-700"
        >
          <span
            class="inline-block h-3 w-3 rounded-sm"
            :style="{ backgroundColor: item.color }"
          ></span>
          <span class="font-medium">{{ item.label }}</span>
        </div>
      </div>

      <div class="relative overflow-x-auto">
        <div
          ref="chartEl"
          :style="{
            minWidth: `${chartMinWidth}px`,
            height: `${chartHeight}px`,
          }"
        ></div>

        <div v-if="loading" class="chart-overlay">불러오는 중...</div>
        <div v-else-if="!normalizedRows.length" class="chart-overlay">데이터가 없습니다.</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import * as echarts from 'echarts';

  type TimelineRow = Record<string, any>;

  const props = defineProps<{
    rows: TimelineRow[];
    loading?: boolean;
  }>();

  /* -------------------------------------------------------------------------- */
  /* Utils                                                                       */
  /* -------------------------------------------------------------------------- */

  function pick<T = any>(obj: Record<string, any>, ...keys: string[]): T | undefined {
    for (const key of keys) {
      if (obj && obj[key] !== undefined) return obj[key] as T;
    }
    return undefined;
  }

  function norm(v: any) {
    return v == null ? '' : String(v).trim();
  }

  function toDate(value?: string | null): Date | null {
    if (!value) return null;
    const normalized = String(value)
      .replace(' ', 'T')
      .replace(/(\.\d{3})\d+$/, '$1');
    const d = new Date(normalized);
    return Number.isNaN(d.getTime()) ? null : d;
  }

  function pad2(n: number) {
    return String(n).padStart(2, '0');
  }

  function formatDateTime(d?: Date | null) {
    if (!d) return '-';
    return [
      d.getFullYear(),
      '-',
      pad2(d.getMonth() + 1),
      '-',
      pad2(d.getDate()),
      ' ',
      pad2(d.getHours()),
      ':',
      pad2(d.getMinutes()),
      ':',
      pad2(d.getSeconds()),
    ].join('');
  }

  function formatRangeDateTime(d?: Date | null) {
    if (!d) return '-';
    return `${formatDateTime(d)}`;
  }

  function formatHms(ms: number) {
    const totalSec = Math.max(0, Math.floor(ms / 1000));
    const hh = Math.floor(totalSec / 3600);
    const mm = Math.floor((totalSec % 3600) / 60);
    const ss = totalSec % 60;
    return `${pad2(hh)}:${pad2(mm)}:${pad2(ss)}`;
  }

  function hourFloor(date: Date) {
    const d = new Date(date);
    d.setMinutes(0, 0, 0);
    return d;
  }

  function addHours(date: Date, hours: number) {
    return new Date(date.getTime() + hours * 60 * 60 * 1000);
  }

  /* -------------------------------------------------------------------------- */
  /* Normalize                                                                   */
  /* -------------------------------------------------------------------------- */

  type ProcessKey = 'BCR' | 'SORTER' | 'PALLETIZED';

  const PROCESS_ORDER: ProcessKey[] = ['BCR', 'SORTER', 'PALLETIZED'];

  const PROCESS_COLOR: Record<ProcessKey, string> = {
    BCR: '#60a5fa',
    SORTER: '#22c55e',
    PALLETIZED: '#f97316',
  };

  const legendItems = [
    { key: 'BCR', label: '박스 입고시점', color: PROCESS_COLOR.BCR },
    { key: 'SORTER', label: '박스 분기시점', color: PROCESS_COLOR.SORTER },
    { key: 'PALLETIZED', label: '박스 적재완료', color: PROCESS_COLOR.PALLETIZED },
  ];

  type BarRow = {
    containerNo: string;
    processType: ProcessKey;
    boxId: string;
    parcelId: string;
    plcSeqNo: string;
    itemCode: string;
    blNo: string;
    startAt: Date;
    endAt: Date;
    durationMs: number;
    tooltipTitle: string;
    tooltipSub1: string;
    tooltipSub2: string;
  };

  const normalizedRows = computed<BarRow[]>(() => {
    return (props.rows ?? [])
      .map((r) => {
        const processType = norm(pick<string>(r, 'processType', 'process_type')).toUpperCase();
        const rowType = norm(pick<string>(r, 'rowType', 'row_type')).toUpperCase();

        if (rowType === 'CONTAINER') return null;
        if (!PROCESS_ORDER.includes(processType as ProcessKey)) return null;

        const start = toDate(pick<string>(r, 'startAt', 'start_at'));
        const endRaw = toDate(pick<string>(r, 'endAt', 'end_at'));

        if (!start) return null;

        let end = endRaw ?? new Date(start.getTime() + 1000);
        if (end.getTime() <= start.getTime()) {
          end = new Date(start.getTime() + 1000);
        }

        return {
          containerNo:
            norm(pick<string>(r, 'cntrNo', 'cntr_no')) ||
            norm(pick<string>(r, 'rowGroup', 'row_group')) ||
            '-',
          processType: processType as ProcessKey,
          boxId: norm(pick<string>(r, 'boxId', 'box_id')),
          parcelId: norm(pick<string>(r, 'parcelId', 'parcel_id')),
          plcSeqNo: norm(pick<string>(r, 'plcSeqNo', 'plc_seq_no')),
          itemCode: norm(pick<string>(r, 'itemCode', 'item_code')),
          blNo: norm(pick<string>(r, 'blNo', 'bl_no')),
          startAt: start,
          endAt: end,
          durationMs: end.getTime() - start.getTime(),
          tooltipTitle: norm(pick<string>(r, 'tooltipTitle', 'tooltip_title')) || processType,
          tooltipSub1: norm(pick<string>(r, 'tooltipSub1', 'tooltip_sub1')),
          tooltipSub2: norm(pick<string>(r, 'tooltipSub2', 'tooltip_sub2')),
        };
      })
      .filter(Boolean) as BarRow[];
  });

  /* -------------------------------------------------------------------------- */
  /* Axis / Size                                                                  */
  /* -------------------------------------------------------------------------- */

  const containerCategories = computed<string[]>(() => {
    const set = new Set<string>();
    normalizedRows.value.forEach((r) => set.add(r.containerNo));
    return Array.from(set);
  });

  const containerCount = computed(() => containerCategories.value.length);

  const minStart = computed<Date | null>(() => {
    if (!normalizedRows.value.length) return null;
    return normalizedRows.value.reduce(
      (min, row) => (row.startAt.getTime() < min.getTime() ? row.startAt : min),
      normalizedRows.value[0].startAt,
    );
  });

  const maxEnd = computed<Date | null>(() => {
    if (!normalizedRows.value.length) return null;
    return normalizedRows.value.reduce(
      (max, row) => (row.endAt.getTime() > max.getTime() ? row.endAt : max),
      normalizedRows.value[0].endAt,
    );
  });

  const chartStart = computed<number>(() => {
    if (!minStart.value) return Date.now();
    return addHours(hourFloor(minStart.value), -1).getTime();
  });

  const chartEnd = computed<number>(() => {
    if (!maxEnd.value) return Date.now();
    return addHours(hourFloor(maxEnd.value), 1).getTime();
  });

  const rangeLabel = computed(() => {
    return `${formatRangeDateTime(new Date(chartStart.value))} ~ ${formatRangeDateTime(
      new Date(chartEnd.value),
    )}`;
  });

  const HALF_HOUR_MS = 30 * 60 * 1000;

  const halfHourSlotCount = computed(() => {
    const diff = Math.max(chartEnd.value - chartStart.value, HALF_HOUR_MS);
    return Math.ceil(diff / HALF_HOUR_MS);
  });

  /**
   * 차트 최소 폭
   * - 기존보다 과도하게 넓지 않도록 축소
   */
  const chartMinWidth = computed(() => {
    return Math.max(1100, halfHourSlotCount.value * 90);
  });

  /**
   * 차트 높이
   * - 컨테이너 1개일 때는 낮게
   * - 컨테이너 수 많아지면 자동 증가
   */
  const ROW_HEIGHT = 64;
  const CHART_BASE_HEIGHT = 120;
  const CHART_MIN_HEIGHT = 260;

  const chartHeight = computed(() => {
    return Math.max(CHART_MIN_HEIGHT, CHART_BASE_HEIGHT + containerCount.value * ROW_HEIGHT);
  });

  /* -------------------------------------------------------------------------- */
  /* ECharts data                                                                */
  /* -------------------------------------------------------------------------- */

  const occData = computed(() => {
    return normalizedRows.value.map((row) => {
      const laneIdx = PROCESS_ORDER.indexOf(row.processType);
      const laneTotal = PROCESS_ORDER.length;

      return {
        value: [
          row.containerNo, // 0 y
          row.startAt.getTime(), // 1 x1
          row.endAt.getTime(), // 2 x2
          PROCESS_COLOR[row.processType], // 3 color
          row.processType, // 4 process
          row.boxId, // 5 boxId
          row.itemCode, // 6 itemCode
          row.blNo, // 7 blNo
          row.parcelId, // 8 parcelId
          row.plcSeqNo, // 9 plcSeqNo
          row.tooltipTitle, // 10 title
          row.tooltipSub1, // 11 sub1
          row.tooltipSub2, // 12 sub2
          laneIdx, // 13 lane idx
          laneTotal, // 14 lane total
          row.durationMs, // 15 duration
        ],
      };
    });
  });

  const occSeries = computed<echarts.SeriesOption>(() => {
    const renderItem = (params: any, api: any) => {
      const y = api.value(0);
      const x1 = api.value(1);
      const x2 = api.value(2);
      const fill = api.value(3);
      const laneIdx = api.value(13) ?? 0;
      const laneCnt = api.value(14) ?? 1;

      const p1 = api.coord([x1, y]);
      const p2 = api.coord([x2, y]);
      const band = api.size([0, 1])[1];

      // 기존보다 줄 높이를 더 타이트하게
      const usable = band * 0.54;
      const gap = 2;
      const laneH = Math.max(4, Math.min(10, (usable - gap * (laneCnt - 1)) / laneCnt));
      const totalH = laneH * laneCnt + gap * (laneCnt - 1);
      const top = p1[1] - totalH / 2;
      const yTopPx = top + laneIdx * (laneH + gap);

      const rect = {
        x: p1[0],
        y: yTopPx,
        width: Math.max(2, p2[0] - p1[0]),
        height: laneH,
        r: Math.min(3, laneH / 2),
      };

      const cs = params.coordSys;
      const clipped = cs
        ? (echarts.graphic as any).clipRectByRect(rect, {
            x: cs.x,
            y: cs.y,
            width: cs.width,
            height: cs.height,
          })
        : rect;

      return {
        type: 'rect',
        shape: clipped || rect,
        style: { fill, opacity: 0.95 },
      };
    };

    return {
      name: 'Daily Timeline',
      type: 'custom',
      xAxisIndex: 0,
      yAxisIndex: 0,
      z: 30,
      renderItem,
      data: occData.value,
      encode: { x: [1, 2], y: 0 },
      tooltip: {
        formatter: (it: any) => {
          const v = it?.value ?? [];
          const start = new Date(Number(v[1]));
          const end = new Date(Number(v[2]));

          return [
            `<b>${v[4] || '-'}</b>`,
            `컨테이너: <b>${v[0] || '-'}</b>`,
            `BL No: <b>${v[7] || '-'}</b>`,
            `BOX ID: <b>${v[5] || '-'}</b>`,
            `PARCEL ID: <b>${v[8] || '-'}</b>`,
            `PLC SEQ: <b>${v[9] || '-'}</b>`,
            `상품코드: <b>${v[6] || '-'}</b>`,
            `시작: ${formatDateTime(start)}`,
            `종료: ${formatDateTime(end)}`,
            `소요시간: ${formatHms(Number(v[15] || 0))}`,
          ].join('<br/>');
        },
      },
    } as echarts.SeriesOption;
  });

  const option = computed<echarts.EChartsOption>(() => ({
    animation: false,
    grid: {
      top: 20,
      left: 120,
      right: 50,
      bottom: 60,
    },
    tooltip: {
      trigger: 'item',
      confine: true,
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#22c55e',
      borderWidth: 1,
      textStyle: {
        color: '#334155',
        fontSize: 12,
      },
      extraCssText: 'box-shadow: 0 8px 20px rgba(15,23,42,0.12);',
    },
    xAxis: {
      type: 'time',
      min: chartStart.value,
      max: chartEnd.value,
      interval: HALF_HOUR_MS,
      name: '시각',
      nameLocation: 'end',
      axisLabel: {
        formatter: (value: number) => {
          const d = new Date(value);
          return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
        },
      },
      splitLine: {
        show: true,
        lineStyle: { color: '#e5e7eb' },
      },
      axisPointer: {
        show: false,
      },
    },
    yAxis: {
      type: 'category',
      data: containerCategories.value,
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: {
        margin: 12,
      },
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed',
          color: '#e5e7eb',
        },
      },
    },
    dataZoom: [
      {
        type: 'slider',
        xAxisIndex: 0,
        height: 14,
        bottom: 12,
        start: 0,
        end: 100,
      },
      {
        type: 'inside',
        xAxisIndex: 0,
      },
    ],
    series: [occSeries.value],
  }));

  /* -------------------------------------------------------------------------- */
  /* Crosshair overlay                                                           */
  /* -------------------------------------------------------------------------- */

  const formatMsToHHmmss = (ms: number) => {
    const d = new Date(ms);
    return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;
  };

  const chartEl = ref<HTMLDivElement | null>(null);
  let chart: echarts.ECharts | null = null;
  let ro: ResizeObserver | null = null;

  let zr: any = null;
  let crossGroup: any = null;
  let crossV: any = null;
  let crossH: any = null;
  let crossLabel: any = null;
  let onZrMove: any = null;
  let onZrOut: any = null;

  const initCrosshairOverlay = () => {
    if (!chart) return;

    const g: any = (echarts as any).graphic;
    zr = chart.getZr();

    crossGroup = new g.Group({ silent: true });

    crossV = new g.Line({
      shape: { x1: 0, y1: 0, x2: 0, y2: 0 },
      style: { stroke: 'rgba(100,116,139,0.85)', lineWidth: 1 },
      silent: true,
    });

    crossH = new g.Line({
      shape: { x1: 0, y1: 0, x2: 0, y2: 0 },
      style: { stroke: 'rgba(100,116,139,0.65)', lineWidth: 1 },
      silent: true,
    });

    crossLabel = new g.Text({
      style: {
        text: '',
        fill: '#ffffff',
        backgroundColor: 'rgba(15,23,42,0.92)',
        padding: [4, 8],
        borderRadius: 4,
        fontSize: 11,
        align: 'left',
        verticalAlign: 'top',
      },
      silent: true,
    });

    crossGroup.add(crossV);
    crossGroup.add(crossH);
    crossGroup.add(crossLabel);
    crossGroup.hide();
    zr.add(crossGroup);

    onZrMove = (e: any) => {
      if (!chart) return;

      const x = (e?.zrX ?? e?.offsetX) as number;
      const y = (e?.zrY ?? e?.offsetY) as number;

      const grid = (chart as any).getModel().getComponent('grid', 0);
      if (!grid || !grid.coordinateSystem) {
        crossGroup?.hide();
        zr?.refresh();
        return;
      }

      const rect = grid.coordinateSystem.getRect();
      const inGrid =
        x >= rect.x && x <= rect.x + rect.width && y >= rect.y && y <= rect.y + rect.height;

      if (!inGrid) {
        crossGroup?.hide();
        zr?.refresh();
        return;
      }

      const pt = chart.convertFromPixel({ gridIndex: 0 }, [x, y]);
      const xVal = Array.isArray(pt) ? pt[0] : pt;
      const timeText = formatMsToHHmmss(Number(xVal));

      crossGroup.show();

      crossV.setShape({
        x1: x,
        y1: rect.y,
        x2: x,
        y2: rect.y + rect.height,
      });

      crossH.setShape({
        x1: rect.x,
        y1: y,
        x2: rect.x + rect.width,
        y2: y,
      });

      crossLabel.attr({ style: { ...crossLabel.style, text: timeText } });
      const br = crossLabel.getBoundingRect();

      let tx = x + 8;
      let ty = rect.y + 6;

      if (tx + br.width > rect.x + rect.width) tx = x - br.width - 8;
      if (tx < rect.x) tx = rect.x;

      crossLabel.attr({ x: tx, y: ty });

      zr.refresh();
    };

    onZrOut = () => {
      crossGroup?.hide();
      zr?.refresh();
    };

    zr.on('mousemove', onZrMove);
    zr.on('mouseout', onZrOut);
  };

  const destroyCrosshairOverlay = () => {
    if (!zr) return;
    if (onZrMove) zr.off('mousemove', onZrMove);
    if (onZrOut) zr.off('mouseout', onZrOut);
    if (crossGroup) zr.remove(crossGroup);

    zr = null;
    crossGroup = null;
    crossV = null;
    crossH = null;
    crossLabel = null;
    onZrMove = null;
    onZrOut = null;
  };

  /* -------------------------------------------------------------------------- */
  /* Render                                                                      */
  /* -------------------------------------------------------------------------- */

  const handleResize = () => {
    chart?.resize();
  };

  const render = async () => {
    await nextTick();
    if (!chartEl.value) return;

    // 현재 DOM과 chart 인스턴스가 다르면 재생성
    if (!chart || chart.getDom() !== chartEl.value) {
      chart?.dispose();
      chart = echarts.init(chartEl.value);
    }

    if (props.loading || !normalizedRows.value.length) {
      chart.clear();
      return;
    }

    chart.setOption(option.value, true);
    chart.resize();

    destroyCrosshairOverlay();
    initCrosshairOverlay();
  };

  watch(
    [() => props.loading, () => props.rows, option],
    () => {
      render();
    },
    { deep: true, immediate: true },
  );

  onMounted(() => {
    if (chartEl.value && !chart) {
      chart = echarts.init(chartEl.value);
    }

    ro = new ResizeObserver(handleResize);
    if (chartEl.value) ro.observe(chartEl.value);
    window.addEventListener('resize', handleResize);

    render();
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize);
    ro?.disconnect();
    ro = null;

    destroyCrosshairOverlay();

    chart?.dispose();
    chart = null;
  });
</script>

<style scoped>
  .timeline-chart-wrap {
    width: 100%;
  }

  .chart-empty {
    padding: 36px 12px;
    border-radius: 16px;
    text-align: center;
    background: #f8fafc;
    color: #94a3b8;
    font-size: 13px;
  }

  .chart-overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 16px;
    background: rgba(248, 250, 252, 0.9);
    color: #94a3b8;
    font-size: 13px;
  }
</style>
