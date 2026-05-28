<template>
  <div class="daily-report-page">
    <section class="panel filter-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">일별 리포트</h2>
          <p class="panel-desc">일자 / BL / 컨테이너 기준 일별 생산 리포트 조회</p>
        </div>

        <div class="filter-actions">
          <button class="btn btn-primary" :disabled="loading.search" @click="search">
            {{ loading.search ? '조회중...' : '조회' }}
          </button>

          <button class="btn btn-default" :disabled="loading.search" @click="handleResetFilters">
            초기화
          </button>

          <button class="btn btn-success" :disabled="loading.export" @click="exportExcel">
            {{ loading.export ? '추출중...' : '엑셀다운로드' }}
          </button>
        </div>
      </div>

      <div class="filter-grid">
        <div class="filter-item">
          <label class="filter-label">조회일자</label>
          <DatePicker v-model="filters.todayDate" />
        </div>

        <div class="filter-item">
          <label class="filter-label">BL No</label>
          <input
            v-model="filters.blNo"
            type="text"
            class="text-input"
            placeholder="예: CNB0311392"
            @keyup.enter="search"
          />
        </div>

        <div class="filter-item">
          <label class="filter-label">컨테이너 번호</label>
          <input
            v-model="filters.cntrNo"
            type="text"
            class="text-input"
            placeholder="예: CMAU8586628"
            @keyup.enter="search"
          />
        </div>
      </div>
    </section>

    <SummaryCards :summary="summary" :loading="loading.search" />

    <section v-if="summaryText" class="panel">
      <div class="panel-header panel-header-clickable" @click="toggleSummaryFold">
        <div>
          <h3 class="panel-title">종합 결과</h3>
          <p class="panel-desc">백엔드 집계 결과 요약</p>
        </div>

        <button type="button" class="fold-btn" @click.stop="toggleSummaryFold">
          <svg
            class="fold-icon"
            :class="{ folded: isSummaryFolded }"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fill-rule="evenodd"
              d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z"
              clip-rule="evenodd"
            />
          </svg>
          {{ isSummaryFolded ? '펼치기' : '접기' }}
        </button>
      </div>

      <transition name="fold">
        <pre v-show="!isSummaryFolded" class="summary-text">{{ summaryText }}</pre>
      </transition>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">컨테이너 / 공정 / 박스 단위 처리시간</h3>
          <p class="panel-desc">컨테이너 · BCR · SORTER · PALLETIZED 기준 타임라인</p>
        </div>

        <div class="meta-text">
          총 <span class="num">{{ timelineRows.length }}</span
          >건
        </div>
      </div>

      <TimelineChart :rows="timelineRows" :loading="loading.search" />
    </section>
<!--
    <RawDataTabs
      :bcr-rows="bcrRows"
      :sorter-rows="sorterRows"
      :palletized-rows="palletizedRows"
      :export-date="filters.todayDate"
      :loading="loading.search"
    />-->
  </div>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import DatePicker from './components/DatePicker.vue';
  import SummaryCards from './components/SummaryCards.vue';
  import TimelineChart from './components/TimelineChart.vue';
  import RawDataTabs from './components/RawDataTabs.vue';
  import { useSamsungDailyReportManager } from './composables/useSamsungDailyReportManager';

  const {
    filters,
    summary,
    timelineRows,
    bcrRows,
    sorterRows,
    palletizedRows,
    loading,
    search,
    exportExcel,
    resetFilters,
  } = useSamsungDailyReportManager();

  const isSummaryFolded = ref(true);

  function toggleSummaryFold() {
    isSummaryFolded.value = !isSummaryFolded.value;
  }

  function pick<T = any>(
    obj: Record<string, any> | null | undefined,
    ...keys: string[]
  ): T | undefined {
    if (!obj) return undefined;
    for (const key of keys) {
      if (obj[key] !== undefined) return obj[key] as T;
    }
    return undefined;
  }

  const summaryText = computed(() => {
    return pick<string>(summary.value as any, 'summaryText', 'summary_text') || '';
  });

  function handleResetFilters() {
    resetFilters();
    search();
  }
</script>

<style scoped>
  .daily-report-page {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 16px;
    min-height: 100vh;
    background: rgba(91, 97, 246, 0.08);
  }

  .panel {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 20px;
    padding: 16px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  }

  .filter-panel {
    padding-bottom: 18px;
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    align-items: flex-start;
    margin-bottom: 14px;
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

  .filter-actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }

  .filter-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
  }

  .filter-item {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .filter-label {
    font-size: 12px;
    font-weight: 600;
    color: #475569;
  }

  .text-input {
    width: 100%;
    height: 38px;
    border: 1px solid #cbd5e1;
    border-radius: 12px;
    padding: 0 12px;
    font-size: 14px;
    background: #fff;
    color: #0f172a;
  }

  .text-input:focus {
    outline: none;
    border-color: #10b981;
    box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
  }

  .btn {
    height: 38px;
    padding: 0 14px;
    border-radius: 12px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }

  .btn:disabled {
    opacity: 0.55;
    cursor: default;
  }

  .btn-primary {
    background: #0f172a;
    color: #fff;
    border: none;
  }

  .btn-default {
    background: #fff;
    color: #334155;
    border: 1px solid #cbd5e1;
  }

  .btn-success {
    background: #16a34a;
    color: #fff;
    border: none;
  }

  .summary-text {
    margin: 0;
    white-space: pre-wrap;
    background: #f8fafc;
    border-radius: 16px;
    padding: 16px;
    font-size: 13px;
    line-height: 1.7;
    color: #334155;
    overflow: auto;
  }

  .meta-text {
    font-size: 12px;
    color: #64748b;
  }

  .num {
    font-weight: 700;
    color: #0f172a;
  }

  @media (max-width: 1200px) {
    .filter-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .daily-report-page {
      padding: 12px;
    }

    .panel-header {
      flex-direction: column;
    }

    .filter-grid {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }

    .filter-actions {
      width: 100%;
      flex-wrap: wrap;
    }

    .filter-actions .btn {
      flex: 1;
    }
  }

  .panel-header-clickable {
    cursor: pointer;
    user-select: none;
  }

  .fold-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 34px;
    padding: 0 12px;
    border-radius: 10px;
    border: 1px solid #cbd5e1;
    background: #fff;
    color: #334155;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
  }

  .fold-icon {
    width: 16px;
    height: 16px;
    transition: transform 0.2s ease;
  }

  .fold-icon.folded {
    transform: rotate(-90deg);
  }

  .fold-enter-active,
  .fold-leave-active {
    transition: all 0.2s ease;
    overflow: hidden;
  }

  .fold-enter-from,
  .fold-leave-to {
    opacity: 0;
    max-height: 0;
    margin-top: 0;
    padding-top: 0;
    padding-bottom: 0;
  }

  .fold-enter-to,
  .fold-leave-from {
    opacity: 1;
    max-height: 500px;
  }
</style>
