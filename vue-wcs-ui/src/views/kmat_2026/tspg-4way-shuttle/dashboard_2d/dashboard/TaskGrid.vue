<template>
  <div class="task-grid-container" :class="{ collapsed: collapsedModel }" :style="containerStyle" @wheel.stop>
    <div class="grid-header" @click="toggleCollapse">
      <h4>작업 목록</h4>
      <div class="header-actions">
        <span class="task-count">{{ jobs.length }}건</span>
        <span class="toggle-icon">{{ collapsedModel ? '▲' : '▼' }}</span>
      </div>
    </div>

    <div v-show="!collapsedModel" class="grid-body">
      <div class="grid-toolbar">
        <div class="filter-group">
          <button :class="{ active: filter === 'all' }" @click="filter = 'all'"> 전체 </button>
          <button :class="{ active: filter === 'active' }" @click="filter = 'active'">
            진행중
          </button>
          <button :class="{ active: filter === 'error' }" @click="filter = 'error'"> 에러 </button>
        </div>
        <button v-if="can('show')" class="refresh-btn" @click="$emit('refresh')"> 새로고침 </button>
      </div>

      <div class="grid-table-wrapper">
        <table class="grid-table">
          <thead>
            <tr>
              <th>작업 ID</th>
              <th>파렛트</th>
              <th>레벨</th>
              <th>타입</th>
              <th>출발지</th>
              <th>도착지</th>
              <th>상태</th>
              <th>할당 설비</th>
              <th>생성시간</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="job in paginatedJobs" :key="job.jobKey" :class="getRowClass(job)">
              <td class="task-id">
                {{ job.jobKey }}
                <span
                  v-if="job.parentJobKey"
                  class="parent-job-link"
                  :title="`상위작업: ${job.parentJobKey}`"
                >
                  ↳
                </span>
              </td>
              <td class="barcode-cell" :title="job.barcode || ''">
                <span v-if="job.barcode" class="barcode-value">{{ job.barcode }}</span>
                <span v-else class="barcode-empty">-</span>
              </td>
              <td>
                <span class="job-level-badge" :class="getJobLevelClass(job.jobLevel)">
                  {{ getJobLevelText(job.jobLevel) }}
                </span>
              </td>
              <td>{{ getJobTypeText(job.jobType) }}</td>
              <td>{{ job.fromLoc || '-' }}</td>
              <td>{{ job.toLoc || '-' }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(job)">
                  {{ getStatusText(job) }}
                </span>
              </td>
              <td>
                <span v-if="getAssignedEquipmentText(job) !== '-'" class="equipment-info">
                  <span class="equipment-type">
                    {{ getEquipmentTypeIcon(job.assignedEquipmentType) }}
                  </span>
                  {{ getAssignedEquipmentText(job) }}
                </span>
                <span v-else>-</span>
              </td>

              <td>{{ formatTime(job.createdAt ?? job.ts) }}</td>

              <td class="action-cell">
                <div class="action-buttons">
                  <button
                    v-if="isRunningOrPaused(job) && can('delete')"
                    class="action-btn cancel"
                    @click="$emit('cancel-job', job.jobKey)"
                  >
                    취소
                  </button>
                  <button
                    v-if="isPaused(job) && can('update')"
                    class="action-btn resume"
                    @click="$emit('resume-job', job.jobKey)"
                  >
                    재개
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="paginatedJobs.length === 0">
              <td colspan="10" class="empty-row"> 표시할 작업이 없습니다. </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
          ◀
        </button>
        <template v-for="(page, index) in visiblePages" :key="index">
          <span v-if="page === -1" class="page-ellipsis">...</span>
          <button
            v-else
            class="page-btn"
            :class="{ active: page === currentPage }"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>
        </template>
        <button
          class="page-btn"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
        >
          ▶
        </button>
        <span class="page-info">
          {{ filteredJobs.length }}건 중 {{ (currentPage - 1) * pageSize + 1 }}-{{
            Math.min(currentPage * pageSize, filteredJobs.length)
          }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, watch } from 'vue';
  import type { RtJobStatus } from '../api/types';
  import { LayoutEquipmentType } from '../api/types';
  import {
    EcsOrderTypeLabels,
    JobUiStatus,
    enumLabel,
    enumIcon,
    enumBadgeVariant,
    rtJobStatusToJobStatus,
  } from '../../constants';
  import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';

  const MENU = 'Dashboard2D';
  const { can } = usePermissionLocal(MENU);

  // 단일 출처 — 백엔드 JobStatusDto 와 매칭되는 RtJobStatus 를 그대로 사용
  const props = defineProps<{
    jobs: RtJobStatus[];
    height?: number;
    collapsed?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'cancel-job', jobKey: string): void;
    (e: 'resume-job', jobKey: string): void;
    (e: 'refresh'): void;
    (e: 'update:collapsed', v: boolean): void;
  }>();

  const collapsedModel = computed({
    get: () => !!props.collapsed,
    set: (v: boolean) => emit('update:collapsed', v),
  });

  const filter = ref<'all' | 'active' | 'error'>('all');

  const currentPage = ref(1);
  const pageSize = ref(20);

  const toggleCollapse = () => {
    collapsedModel.value = !collapsedModel.value;
  };

  watch(filter, () => {
    currentPage.value = 1;
  });

  const filteredJobs = computed(() => {
    switch (filter.value) {
      case 'active':
        return props.jobs.filter((j) => {
          const ui = rtJobStatusToJobStatus(j);
          return (
            ui === JobUiStatus.PENDING.code ||
            ui === JobUiStatus.ASSIGNED.code ||
            ui === JobUiStatus.RUNNING.code ||
            ui === JobUiStatus.PAUSED.code
          );
        });
      case 'error':
        return props.jobs.filter((j) => rtJobStatusToJobStatus(j) === JobUiStatus.FAILED.code);
      default:
        return props.jobs;
    }
  });

  const totalPages = computed(() => Math.ceil(filteredJobs.value.length / pageSize.value));

  const containerStyle = computed(() => {
    if (collapsedModel.value) return { height: '40px' };
    const h = props.height ?? 320;
    return { height: `${h}px` };
  });

  const recalcPageSize = () => {
    if (collapsedModel.value) return;

    const headerH = 40;
    const toolbarH = 41;
    const paginationH = totalPages.value > 1 ? 44 : 0;
    const theadH = 33;
    const rowH = 33;
    const bottomPadding = 8;

    const panelH = props.height ?? 320;
    const available = Math.max(
      panelH - headerH - toolbarH - paginationH - theadH - bottomPadding,
      rowH * 3,
    );
    const rows = Math.floor(available / rowH);

    pageSize.value = Math.min(Math.max(rows, 5), 50);

    if (currentPage.value > totalPages.value) {
      currentPage.value = Math.max(totalPages.value, 1);
    }
  };

  watch(
    () => [props.height, collapsedModel.value, filter.value, props.jobs.length],
    () => recalcPageSize(),
    { immediate: true },
  );

  const paginatedJobs = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    const end = start + pageSize.value;
    return filteredJobs.value.slice(start, end);
  });

  const goToPage = (page: number) => {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page;
    }
  };

  const visiblePages = computed(() => {
    const pages: number[] = [];
    const total = totalPages.value;
    const current = currentPage.value;

    pages.push(1);

    const start = Math.max(2, current - 1);
    const end = Math.min(total - 1, current + 1);

    if (start > 2) pages.push(-1);
    for (let i = start; i <= end; i++) pages.push(i);
    if (end < total - 1) pages.push(-1);

    if (total > 1) pages.push(total);

    return pages;
  });

  const getAssignedEquipmentText = (job: RtJobStatus) => {
    if (job.assignedEquipmentId && String(job.assignedEquipmentId).trim() !== '') {
      return job.assignedEquipmentId;
    }
    return '-';
  };

  // 라벨/클래스/타입 모두 constants 의 단일 출처에서 가져온다.
  const getJobTypeText = (jobType?: string | number) => {
    const num = Number(jobType);
    if (Number.isFinite(num) && EcsOrderTypeLabels[num]) return EcsOrderTypeLabels[num];
    return jobType != null && jobType !== '' ? String(jobType) : '-';
  };

  const getStatusText = (job: RtJobStatus) =>
    enumLabel(JobUiStatus, rtJobStatusToJobStatus(job), JobUiStatus.UNKNOWN.label);

  const getStatusClass = (job: RtJobStatus) =>
    enumBadgeVariant(JobUiStatus, rtJobStatusToJobStatus(job));

  const getRowClass = (job: RtJobStatus) => {
    const ui = rtJobStatusToJobStatus(job);
    if (ui === JobUiStatus.FAILED.code) return 'error-row';
    if (ui === JobUiStatus.RUNNING.code || ui === JobUiStatus.PAUSED.code) return 'active-row';
    return '';
  };

  const isRunningOrPaused = (job: RtJobStatus) => {
    const ui = rtJobStatusToJobStatus(job);
    return (
      ui === JobUiStatus.ASSIGNED.code ||
      ui === JobUiStatus.RUNNING.code ||
      ui === JobUiStatus.PAUSED.code
    );
  };

  const isPaused = (job: RtJobStatus) =>
    rtJobStatusToJobStatus(job) === JobUiStatus.PAUSED.code;

  const formatTime = (value?: number | string | null) => {
    if (value == null || value === '') return '-';

    try {
      let date: Date;

      if (typeof value === 'number') {
        date = new Date(value);
      } else if (/^\d+$/.test(value)) {
        date = new Date(Number(value));
      } else {
        date = new Date(value);
      }

      if (isNaN(date.getTime())) return '-';

      return date.toLocaleString('ko-KR', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return '-';
    }
  };

  /**
   * 작업 레벨 텍스트 반환
   * WCS: 최상위 셔틀 작업
   * ECS_RACK: 셔틀 PICK/DROP 작업
   * ECS_ROUTE: 구간 이동 작업 (리프터/컨베이어)
   */
  const getJobLevelText = (level?: string) => {
    switch (level) {
      case 'WCS':
        return 'WCS';
      case 'ECS_RACK':
        return 'RACK';
      case 'ECS_ROUTE':
        return 'ROUTE';
      default:
        return level || 'WCS';
    }
  };

  /**
   * 작업 레벨 CSS 클래스 반환
   */
  const getJobLevelClass = (level?: string) => {
    switch (level) {
      case 'WCS':
        return 'level-wcs';
      case 'ECS_RACK':
        return 'level-ecs-rack';
      case 'ECS_ROUTE':
        return 'level-ecs-route';
      default:
        return 'level-wcs';
    }
  };

  /**
   * 설비 타입 아이콘 반환
   */
  const getEquipmentTypeIcon = (type?: string) => {
    const code = type?.toUpperCase();
    return enumIcon(LayoutEquipmentType, code, LayoutEquipmentType.RACK.icon);
  };
</script>

<style scoped>
  .task-grid-container {
    width: 100%;
    background: rgba(30, 34, 45, 0.98);
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    z-index: 100;
    display: flex;
    flex-direction: column;
  }

  .task-grid-container.collapsed {
    height: 40px !important;
  }

  .grid-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 16px;
    background: rgba(255, 255, 255, 0.05);
    cursor: pointer;
    user-select: none;
  }

  .grid-header h4 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    color: #e5eaf3;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .task-count {
    font-size: 12px;
    color: #909399;
    background: rgba(64, 158, 255, 0.2);
    padding: 2px 8px;
    border-radius: 10px;
  }

  .toggle-icon {
    font-size: 10px;
    color: #909399;
  }

  .grid-body {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .grid-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 16px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }

  .filter-group {
    display: flex;
    gap: 4px;
  }

  .filter-group button {
    padding: 4px 12px;
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 4px;
    color: #909399;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .filter-group button:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  .filter-group button.active {
    background: rgba(64, 158, 255, 0.2);
    border-color: #409eff;
    color: #409eff;
  }

  .refresh-btn {
    padding: 4px 12px;
    background: rgba(103, 194, 58, 0.2);
    border: 1px solid rgba(103, 194, 58, 0.3);
    border-radius: 4px;
    color: #67c23a;
    font-size: 12px;
    cursor: pointer;
  }

  .refresh-btn:hover {
    background: rgba(103, 194, 58, 0.3);
  }

  .grid-table-wrapper {
    flex: 1;
    min-height: 0;
    overflow: auto;
    overscroll-behavior: contain;
  }

  .grid-table {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
  }

  .grid-table th,
  .grid-table td {
    padding: 8px 12px;
    text-align: left;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  }

  .grid-table th {
    background: rgba(30, 34, 45, 0.98);
    color: #909399;
    font-weight: 500;
    position: sticky;
    top: 0;
    z-index: 3;
    background-clip: padding-box;
  }

  .grid-table td {
    color: #c0c4cc;
  }

  .task-id {
    font-weight: 500;
    color: #e5eaf3;
  }

  .barcode-cell {
    max-width: 160px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .barcode-value {
    font-weight: 500;
    color: #e5eaf3;
  }

  .barcode-empty {
    color: #606266;
  }

  .error-row {
    background: rgba(245, 108, 108, 0.1);
  }

  .active-row {
    background: rgba(230, 162, 60, 0.1);
  }

  .status-badge {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 10px;
    font-size: 11px;
    font-weight: 500;
  }

  .status-badge.waiting {
    background: rgba(144, 147, 153, 0.2);
    color: #909399;
  }

  .status-badge.assigned {
    background: rgba(64, 158, 255, 0.2);
    color: #409eff;
  }

  .status-badge.in-progress {
    background: rgba(230, 162, 60, 0.2);
    color: #e6a23c;
  }

  .status-badge.paused {
    background: rgba(230, 162, 60, 0.15);
    color: #e6a23c;
    border: 1px dashed rgba(230, 162, 60, 0.5);
  }

  .status-badge.completed {
    background: rgba(103, 194, 58, 0.2);
    color: #67c23a;
  }

  .status-badge.cancelled {
    background: rgba(144, 147, 153, 0.15);
    color: #909399;
    text-decoration: line-through;
  }

  .status-badge.error {
    background: rgba(245, 108, 108, 0.2);
    color: #f56c6c;
  }

  .action-cell {
    text-align: left;
    vertical-align: middle;
  }

  .action-buttons {
    display: flex;
    align-items: center;
    gap: 4px;
    min-height: 24px;
  }

  .action-btn {
    padding: 3px 8px;
    border: none;
    border-radius: 3px;
    font-size: 11px;
    cursor: pointer;
    transition: background 0.2s;
  }

  .action-btn.cancel {
    background: rgba(245, 108, 108, 0.2);
    color: #f56c6c;
  }

  .action-btn.cancel:hover {
    background: rgba(245, 108, 108, 0.3);
  }

  .action-btn.resume {
    background: rgba(64, 158, 255, 0.2);
    color: #409eff;
  }

  .action-btn.resume:hover {
    background: rgba(64, 158, 255, 0.3);
  }

  .empty-row {
    text-align: center !important;
    color: #606266 !important;
    padding: 24px !important;
  }

  .pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    padding: 8px 16px;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    background: rgba(255, 255, 255, 0.02);
  }

  .page-btn {
    min-width: 28px;
    height: 28px;
    padding: 0 8px;
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 4px;
    color: #909399;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .page-btn:hover:not(:disabled) {
    background: rgba(64, 158, 255, 0.1);
    border-color: rgba(64, 158, 255, 0.3);
    color: #409eff;
  }

  .page-btn.active {
    background: rgba(64, 158, 255, 0.2);
    border-color: #409eff;
    color: #409eff;
  }

  .page-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  .page-ellipsis {
    color: #606266;
    padding: 0 4px;
  }

  .page-info {
    margin-left: 12px;
    font-size: 11px;
    color: #606266;
  }

  /* ============================================
     작업 레벨 뱃지
     ============================================ */
  .job-level-badge {
    display: inline-block;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 10px;
    font-weight: 600;
    letter-spacing: 0.5px;
  }

  .job-level-badge.level-wcs {
    background: rgba(103, 194, 58, 0.2);
    color: #67c23a;
    border: 1px solid rgba(103, 194, 58, 0.3);
  }

  .job-level-badge.level-ecs-rack {
    background: rgba(64, 158, 255, 0.2);
    color: #409eff;
    border: 1px solid rgba(64, 158, 255, 0.3);
  }

  .job-level-badge.level-ecs-route {
    background: rgba(230, 162, 60, 0.2);
    color: #e6a23c;
    border: 1px solid rgba(230, 162, 60, 0.3);
  }

  /* 상위 작업 링크 표시 */
  .parent-job-link {
    color: #909399;
    font-size: 10px;
    margin-left: 4px;
    cursor: help;
  }

  /* 설비 정보 표시 */
  .equipment-info {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .equipment-type {
    font-size: 12px;
  }
</style>
