<template>
  <section class="panel">
    <!-- 탭 -->
    <div class="tab-wrap">
      <button
        v-for="tab in tabDefs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.title }}
        <span class="tab-count">{{ tab.rows.length }}</span>
      </button>
    </div>

    <!-- 그리드 -->
    <div class="grid-wrap">
      <div class="grid-head">
        <div>
          <div class="grid-title">{{ activeTabDef?.title }}</div>
          <div class="grid-sub">표시 건수: {{ activeRows.length }}</div>
        </div>
        <div class="grid-note">* 첫 Row 기준 컬럼 자동 생성</div>
      </div>

      <div class="table-scroll">
        <table class="data-table">
          <thead>
            <tr>
              <th>#</th>
              <th v-for="col in activeColumns" :key="col">{{ col }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in activeRows" :key="idx">
              <td>{{ idx + 1 }}</td>
              <td v-for="col in activeColumns" :key="col">
                {{ formatCell(row[col]) }}
              </td>
            </tr>

            <tr v-if="!activeRows.length">
              <td :colspan="activeColumns.length + 1" class="empty-row">
                {{ loading ? '불러오는 중...' : '데이터가 없습니다.' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import ExcelExportButton from './ExcelExportButton.vue';

  const props = defineProps<{
    bcrRows: Array<Record<string, any>>;
    sorterRows: Array<Record<string, any>>;
    palletizedRows: Array<Record<string, any>>;
    exportDate?: string;
    loading?: boolean;
  }>();

  type TabKey = 'bcrRows' | 'sorterRows' | 'palletizedRows';

  const activeTab = ref<TabKey>('bcrRows');

  const tabDefs = computed(() => [
    { key: 'bcrRows' as const, title: 'BCR Rows', sheet: 'BCR_Rows', rows: props.bcrRows || [] },
    {
      key: 'sorterRows' as const,
      title: 'SORTER Rows',
      sheet: 'SORTER_Rows',
      rows: props.sorterRows || [],
    },
    {
      key: 'palletizedRows' as const,
      title: 'PALLETIZED Rows',
      sheet: 'PALLETIZED_Rows',
      rows: props.palletizedRows || [],
    },
  ]);

  const activeTabDef = computed(() => {
    return tabDefs.value.find((t) => t.key === activeTab.value);
  });

  const activeRows = computed(() => activeTabDef.value?.rows || []);

  const activeColumns = computed(() => {
    const rows = activeRows.value;
    if (!rows.length) return [];
    return Object.keys(rows[0] || {});
  });

  const exportFileName = computed(() => {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    const hh = String(d.getHours()).padStart(2, '0');
    const mi = String(d.getMinutes()).padStart(2, '0');
    const ss = String(d.getSeconds()).padStart(2, '0');
    const date = props.exportDate || 'ALL';
    return `삼성_일별리포트_${date}_${yyyy}${mm}${dd}_${hh}${mi}${ss}.xlsx`;
  });

  function formatCell(v: any) {
    if (v == null || v === '') return '-';
    if (typeof v === 'string') return v;
    if (typeof v === 'number' || typeof v === 'boolean') return String(v);
    try {
      return JSON.stringify(v);
    } catch {
      return String(v);
    }
  }
</script>

<style scoped>
  .panel {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 20px;
    padding: 16px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .panel-title {
    font-size: 18px;
    font-weight: 700;
    color: #0f172a;
  }

  .panel-desc {
    margin-top: 4px;
    font-size: 12px;
    color: #64748b;
  }

  .tab-wrap {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    padding-bottom: 8px;
  }

  .tab-btn {
    flex-shrink: 0;
    border: 1px solid #e2e8f0;
    background: #fff;
    color: #334155;
    border-radius: 12px;
    padding: 8px 12px;
    font-size: 13px;
    cursor: pointer;
  }

  .tab-btn.active {
    background: #0f172a;
    color: #fff;
    border-color: #0f172a;
  }

  .tab-count {
    margin-left: 8px;
    font-size: 11px;
    opacity: 0.8;
  }

  .grid-wrap {
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    overflow: hidden;
  }

  .grid-head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    align-items: center;
    padding: 12px 14px;
    background: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
  }

  .grid-title {
    font-size: 14px;
    font-weight: 700;
    color: #0f172a;
  }

  .grid-sub {
    margin-top: 4px;
    font-size: 11px;
    color: #64748b;
  }

  .grid-note {
    font-size: 11px;
    color: #94a3b8;
  }

  .table-scroll {
    max-height: 420px;
    overflow: auto;
    background: #fff;
  }

  .data-table {
    min-width: 100%;
    border-collapse: collapse;
    font-size: 13px;
  }

  .data-table thead {
    position: sticky;
    top: 0;
    z-index: 1;
    background: #fff;
  }

  .data-table th,
  .data-table td {
    white-space: nowrap;
    border-bottom: 1px solid #f1f5f9;
    padding: 10px 12px;
    text-align: left;
    color: #334155;
  }

  .data-table th {
    font-size: 11px;
    font-weight: 700;
    color: #64748b;
  }

  .empty-row {
    text-align: center !important;
    color: #94a3b8 !important;
    padding: 28px 12px !important;
  }
</style>
