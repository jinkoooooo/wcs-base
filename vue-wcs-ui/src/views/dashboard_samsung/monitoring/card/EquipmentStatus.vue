<!-- EquipmentStatus.vue -->
<template>
  <!-- 셀 높이 꽉 채우도록 래퍼 유지 -->
  <div class="card-wrapper">
    <div class="custom-ribbon"></div>

    <Card
      :loading="loading"
      ref="cardRef"
      :title="'설비 상태'"
      :headStyle="headStyleRef"
      :bodyStyle="bodyStyleRef"
      :style="styleRef"
    >
      <div v-if="dataRef && dataRef.length" class="card-body">
        <!-- 상단 범례 -->
        <div class="status-legend">
          <span class="legend-item"> <span class="dot dot-ok"></span> 정상 </span>
          <span class="legend-item"> <span class="dot dot-error"></span> 장애 </span>
        </div>

        <!-- 테이블 영역 -->
        <div class="table-wrapper">
          <Table
            :columns="columns"
            :dataSource="dataRef"
            :pagination="false"
            :rowKey="(record) => record.unit_type"
            :style="tableStyle"
            class="status-table"
          />
        </div>
      </div>

      <!-- 데이터 없을 때 -->
      <div v-else class="empty-box"> 통신 정보가 없습니다. </div>
    </Card>
  </div>
</template>

<script lang="ts" setup>
  import { Card, Table, Tooltip } from 'ant-design-vue';
  import { computed, ref, type CSSProperties, watch, h } from 'vue';
  import type { ColumnType } from 'ant-design-vue/es/table/interface';

  /* ---------------- props ---------------- */
  const props = defineProps({
    loading: Boolean,
    data: {
      type: Object,
      require: false,
    },
    date: {
      type: Object,
      required: true,
    },
  });

  /* ---------------- 스타일 ---------------- */
  const styleRef = computed(() => ({
    border: '1px solid var(--dashboard-primary-color)',
    borderRadius: '0',
    background: 'var(--dashboard-bg-color)',
    minHeight: '10.5rem',
    height: '100%', // 셀 전체 높이 채우기
    display: 'flex',
    flexDirection: 'column',
  }));

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
    padding: '0 0.4rem 0.4rem 0.4rem',
    color: 'white',
    height: '100%',
    boxSizing: 'border-box',
  };

  const tableStyle = ref<CSSProperties>({
    background: 'none',
    fontSize: '0.8rem',
  });

  /* ---------------- 타입 정의 ---------------- */
  interface RawRecord {
    unit_type: string;
    unit_code: string;
    status: string;
    msg: string;
  }

  interface SummaryRecord {
    unit_type: string; // 구분
    equip_count: number; // 설비 수
    ok_count: number; // 정상 개수
    error_count: number; // 장애 개수
    status: number; // 0: 정상, 9: 장애
    msg: string; // 상세 (리스트형 요약)
    tooltip: string; // 상태 동그라미에 띄울 툴팁
  }

  /* ---------------- 데이터 ---------------- */
  const dataList = ref<SummaryRecord[]>([]);
  const dataRef = computed(() => dataList.value);

  /* ---------------- 데이터 세팅 (group by unit_type) ---------------- */
  function setData(newData: any) {
    if (!Array.isArray(newData) || newData.length === 0) {
      dataList.value = [];
      return;
    }

    const rawList = newData as RawRecord[];

    const map: Record<
      string,
      {
        unit_type: string;
        equip_count: number;
        ok_count: number;
        error_count: number;
        okCodes: string[];
        errorDetails: { code: string; msg: string }[];
      }
    > = {};

    rawList.forEach((row) => {
      const type = row.unit_type || '-';

      if (!map[type]) {
        map[type] = {
          unit_type: type,
          equip_count: 0,
          ok_count: 0,
          error_count: 0,
          okCodes: [],
          errorDetails: [],
        };
      }

      const group = map[type];
      group.equip_count++;

      if (row.status === "0") {
        group.ok_count++;
        group.okCodes.push(row.unit_code);
      } else {
        group.error_count++;
        group.errorDetails.push({
          code: row.unit_code,
          msg: row.msg,
        });
      }
    });

    const summaryList: SummaryRecord[] = Object.values(map).map((g) => {
      const hasError = g.error_count > 0;

      let msg: string;
      let tooltip: string;

      if (!hasError) {
        msg = `정상 (${g.ok_count} / ${g.equip_count})`;
        const codes = g.okCodes.join(', ');
        tooltip = `정상 설비 (${g.ok_count} / ${g.equip_count})` + (codes ? `\n${codes}` : '');
      } else {
        const errorCodes = g.errorDetails.map((d) => d.code).join(', ');
        msg = `장애 ${g.error_count}건 (${errorCodes})`;

        const detailLines = g.errorDetails.map((d) => {
          const m = d.msg || '-';
          return `${d.code} : ${m}`;
        });

        tooltip = `장애 설비 목록\n${detailLines.join('\n')}`;
      }

      return {
        unit_type: g.unit_type,
        equip_count: g.equip_count,
        ok_count: g.ok_count,
        error_count: g.error_count,
        status: hasError ? 9 : 0,
        msg,
        tooltip,
      };
    });

    dataList.value = summaryList;
  }

  /* ---------------- 컬럼 정의 ---------------- */
  const columns = computed<ColumnType<SummaryRecord>[]>(() => {
    const getCustomCell = () => ({
      style: {
        padding: '0.2rem 0.35rem',
        backgroundColor: 'var(--dashboard-bg-color)',
        color: 'white',
        fontSize: '0.9rem',
      },
    });

    const getCustomHeaderCell = () => ({
      style: {
        padding: '0.25rem 0.35rem',
        backgroundColor: 'var(--dashboard-bg-color)',
        color: '#cfe8ff',
        fontSize: '0.9rem',
        height: '1.7rem',
        borderBottom: '1px solid rgba(255,255,255,0.15)',
      },
    });

    return [
      {
        title: '구분',
        dataIndex: 'unit_type',
        align: 'center',
        width: 90,
        customCell: getCustomCell,
        customHeaderCell: getCustomHeaderCell,
      },
      {
        title: '설비 수',
        dataIndex: 'equip_count',
        align: 'center',
        width: 80,
        customCell: getCustomCell,
        customHeaderCell: getCustomHeaderCell,
      },
      {
        title: '정상 / 장애',
        align: 'center',
        width: 100,
        customRender: ({ record }) => {
          const ok = record.ok_count;
          const err = record.error_count;
          return `${ok} / ${err}`;
        },
        customCell: getCustomCell,
        customHeaderCell: getCustomHeaderCell,
      },
      {
        title: '상태',
        dataIndex: 'status',
        align: 'center',
        width: 70,
        customRender: ({ value, record }) => {
          const color = value === 0 ? '#16a34a' : '#ef4444';

          // Tooltip 안에 동그라미 렌더링
          return h(
            Tooltip,
            {
              placement: 'bottom',
            },
            {
              default: () =>
                h(
                  'div',
                  {
                    style: {
                      display: 'flex',
                      justifyContent: 'center',
                      alignItems: 'center',
                      height: '100%',
                    },
                  },
                  [
                    h('span', {
                      style: {
                        display: 'inline-block',
                        width: '13px',
                        height: '13px',
                        borderRadius: '999px',
                        backgroundColor: color,
                        boxShadow: `0 0 6px ${color}`,
                      },
                    }),
                  ],
                ),
              title: () =>
                h(
                  'div',
                  {
                    style: {
                      whiteSpace: 'pre-line',
                      fontSize: '0.75rem',
                    },
                  },
                  record.tooltip,
                ),
            },
          );
        },
        customCell: getCustomCell,
        customHeaderCell: getCustomHeaderCell,
      },
      {
        title: '상세',
        dataIndex: 'msg',
        align: 'left',
        ellipsis: true,
        customCell: getCustomCell,
        customHeaderCell: getCustomHeaderCell,
      },
    ];
  });

  /* ---------------- watch ---------------- */
  watch(
    [() => props.loading, () => props.date, () => props.data],
    ([newLoading, _newDate, newData]) => {
      if (newLoading) return;
      setData(newData);
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

  .custom-ribbon {
    position: absolute;
    background: var(--dashboard-primary-color);
    color: white;
    padding: 10px 12px;
    font-size: 14px;
    font-weight: bold;
    z-index: 10;
    clip-path: polygon(0% 0%, 100% 0%, 0% 100%, 0% 100%);
  }

  /* Card 내부 레이아웃 */
  .card-body {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  /* 상단 범례 */
  .status-legend {
    display: flex;
    justify-content: flex-end;
    gap: 0.9rem;
    font-size: 0.7rem;
    margin: 0.15rem 0 0.35rem 0;
    color: #e5f3ff;
  }

  .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
  }

  .dot {
    width: 11px;
    height: 11px;
    border-radius: 999px;
    display: inline-block;
  }

  .dot-ok {
    background-color: #16a34a;
    box-shadow: 0 0 4px #16a34a;
  }

  .dot-error {
    background-color: #ef4444;
    box-shadow: 0 0 4px #ef4444;
  }

  /* 테이블 영역 */
  .table-wrapper {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
  }

  /* 테이블 스타일 */
  .status-table :deep(.ant-table) {
    background: transparent;
    color: white;
  }

  .status-table :deep(.ant-table-thead > tr > th) {
    background-color: rgba(20, 40, 160, 0.3) !important; /* 삼성 블루 톤의 반투명 배경 */
    color: #fff;
    font-weight: 700; /* 굵게 */
    border-bottom: 2px solid #1428a0; /* 하단 강조 선 */
  }

  .status-table :deep(.ant-table-tbody > tr > td) {
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }

  /* 줄무늬 & hover */
  .status-table :deep(.ant-table-tbody > tr:nth-child(odd) > td) {
    background-color: rgba(255, 255, 255, 0.02);
  }

  .status-table :deep(.ant-table-tbody > tr:hover > td) {
    background-color: rgba(0, 191, 255, 0.12);
  }

  /* 빈 데이터 */
  .empty-box {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    font-size: 0.8rem;
    color: #cfe8ff;
    opacity: 0.8;
  }
</style>
