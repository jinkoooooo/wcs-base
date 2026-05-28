<template>
  <div class="monthly-report-page">
    <section class="panel filter-panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">월별 리포트</h2>
          <p class="panel-desc">월 기준 일자별 요약 리포트 조회</p>
        </div>

        <div class="filter-actions">
          <button class="btn btn-primary" :disabled="loading" @click="fetchMonthlySummary">
            {{ loading ? '조회중...' : '조회' }}
          </button>

          <button class="btn btn-default" :disabled="loading" @click="handleResetFilters">
            초기화
          </button>
        </div>
      </div>

      <div class="filter-grid">
        <div class="filter-item">
          <label class="filter-label">조회월</label>
          <input v-model="filters.month" type="month" class="text-input" />
        </div>
      </div>
    </section>

    <section v-if="loading" class="panel empty-panel">불러오는 중...</section>

    <section v-else class="panel">
      <div class="calendar-header-wrap">
        <div class="calendar-head-row week-header">
          <div class="week-cell">일</div>
          <div class="week-cell">월</div>
          <div class="week-cell">화</div>
          <div class="week-cell">수</div>
          <div class="week-cell">목</div>
          <div class="week-cell">금</div>
          <div class="week-cell">토</div>
        </div>
      </div>

      <div class="calendar-grid">
        <MonthDayCard
          v-for="cell in calendarCells"
          :key="cell.key"
          :date="cell.date"
          :in-month="cell.inMonth"
          :summary="daySummaryMap[cell.date]"
          @open-day="openDailyReport"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
  import MonthDayCard from './components/MonthlyDayCard.vue';
  import { useSamsungMonthlyReportManager } from './composables/useSamsungMonthlyReportManager';

  const {
    filters,
    loading,
    calendarCells,
    daySummaryMap,
    fetchMonthlySummary,
    openDailyReport,
    resetFilters,
  } = useSamsungMonthlyReportManager();

  function handleResetFilters() {
    resetFilters();
    fetchMonthlySummary();
  }
</script>

<style scoped>
  .monthly-report-page {
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
    grid-template-columns: repeat(1, minmax(0, 320px));
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

  .empty-panel {
    text-align: center;
    color: #64748b;
    padding: 36px 12px;
  }

  .calendar-header-wrap {
    margin-bottom: 10px;
  }

  .calendar-head-row {
    display: grid;
    grid-template-columns: repeat(7, minmax(0, 1fr));
    gap: 12px;
  }

  .week-cell {
    text-align: center;
    font-size: 12px;
    font-weight: 600;
    color: #64748b;
    padding-bottom: 4px;
  }

  .calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, minmax(0, 1fr));
    gap: 12px;
    align-items: start;
  }

  @media (max-width: 1600px) {
    .calendar-grid,
    .calendar-head-row {
      grid-template-columns: repeat(6, minmax(0, 1fr));
    }
  }

  @media (max-width: 1400px) {
    .calendar-grid,
    .calendar-head-row {
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }
  }

  @media (max-width: 1024px) {
    .filter-grid {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }

    .calendar-grid,
    .calendar-head-row {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 768px) {
    .monthly-report-page {
      padding: 12px;
    }

    .panel-header {
      flex-direction: column;
    }

    .filter-actions {
      width: 100%;
      flex-wrap: wrap;
    }

    .filter-actions .btn {
      flex: 1;
    }

    .calendar-grid,
    .calendar-head-row {
      grid-template-columns: repeat(1, minmax(0, 1fr));
    }
  }
</style>
