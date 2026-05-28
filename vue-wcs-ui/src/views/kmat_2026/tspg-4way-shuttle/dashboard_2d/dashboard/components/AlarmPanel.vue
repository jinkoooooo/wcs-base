<template>
  <div
    ref="panelRef"
    class="alarm-panel"
    :class="{ collapsed: isCollapsed, 'has-alarms': hasActiveAlarms, dragging: isDragging }"
    :style="panelStyle"
    @click.stop
    @wheel.stop
  >
    <div class="alarm-header" @mousedown.left="startDrag" @click="handleHeaderClick">
      <div class="header-left">
        <span class="drag-handle" title="드래그하여 이동">⋮⋮</span>
        <span class="alarm-icon" :class="{ 'has-error': hasErrorAlarms }">
          {{ hasErrorAlarms ? '🔴' : hasActiveAlarms ? '🟡' : '🟢' }}
        </span>
        <span class="header-title">활성 알람</span>
        <span v-if="activeAlarmCount > 0" class="alarm-count">{{ activeAlarmCount }}</span>
      </div>
      <span class="toggle-icon">{{ isCollapsed ? '◀' : '▼' }}</span>
    </div>

    <div v-show="!isCollapsed" class="alarm-body">
      <div class="alarm-toolbar">
        <div class="alarm-filters">
          <button class="filter-btn" :class="{ active: filter === 'all' }" @click="filter = 'all'">
            전체
          </button>
          <button
            class="filter-btn"
            :class="{ active: filter === 'equipment' }"
            @click="filter = 'equipment'"
          >
            설비
          </button>
          <button class="filter-btn" :class="{ active: filter === 'job' }" @click="filter = 'job'">
            작업
          </button>
          <button
            class="filter-btn"
            :class="{ active: filter === 'reinbound' }"
            @click="filter = 'reinbound'"
          >
            재입고
          </button>
        </div>

        <button
          v-if="activeAlarmCount > 0 && can('update')"
          class="ack-all-btn"
          @click="acknowledgeAll"
          title="현재 보이는 알람 모두 확인"
        >
          모두 확인
        </button>
      </div>

      <div class="alarm-list" ref="alarmListRef">
        <div
          v-for="alarm in filteredAlarms"
          :key="alarm.alarmId"
          class="alarm-item"
          :class="[
            getSeverityClass(alarm.severity),
            { acknowledged: isAcknowledged(alarm.alarmId) },
          ]"
        >
          <div class="alarm-type-icon" :class="getAlarmTypeClass(alarm.alarmType)">
            {{ getAlarmTypeIcon(alarm.alarmType) }}
          </div>

          <div class="alarm-content">
            <div class="alarm-header-row">
              <span class="severity-dot" :class="getSeverityClass(alarm.severity)"></span>
              <span class="equipment-type">{{ getEquipmentTypeLabel(alarm.equipmentType) }}</span>
              <span class="equipment-code">
                {{ alarm.equipmentCode || alarm.equipmentId || '-' }}
              </span>
            </div>

            <div v-if="alarm.errorCode" class="alarm-error-code">[{{ alarm.errorCode }}]</div>

            <div class="alarm-message" :title="alarm.errorMessage || '알 수 없는 에러'">
              {{ alarm.errorMessage || alarm.errorCode || '에러 발생' }}
            </div>

            <div class="alarm-meta">
              <span v-if="alarm.orderKey" class="order-info">작업: {{ alarm.orderKey }}</span>
              <span v-if="alarm.barcode" class="barcode-info">바코드: {{ alarm.barcode }}</span>
            </div>

            <div class="alarm-time">{{ formatTime(alarm.occurredAt) }}</div>
          </div>
        </div>

        <div v-if="filteredAlarms.length === 0" class="no-alarms">
          {{ filter === 'all' ? '현재 활성 알람이 없습니다' : '선택한 조건의 알람이 없습니다' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
  import { useShuttleStore } from '../../store/shuttleStore';
  import type { RtAlarm } from '../../api/types';
  import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';

  const MENU = 'Dashboard2D';
  const { can } = usePermissionLocal(MENU);
  const store = useShuttleStore();

  const isCollapsed = ref(true);
  const filter = ref<'all' | 'equipment' | 'job' | 'reinbound'>('all');
  const alarmListRef = ref<HTMLElement | null>(null);
  const panelRef = ref<HTMLElement | null>(null);

  const acknowledgedAlarmIds = ref<Set<string>>(new Set());

  const isDragging = ref(false);
  const dragStartX = ref(0);
  const dragStartY = ref(0);
  const panelX = ref<number | null>(null);
  const panelY = ref<number | null>(null);
  const wasDragged = ref(false);

  const panelStyle = computed(() => {
    if (panelX.value === null || panelY.value === null) return {};
    return {
      position: 'absolute' as const,
      top: `${panelY.value}px`,
      left: `${panelX.value}px`,
      right: 'auto',
    };
  });

  // 재입고 알람 포함 모든 실시간 알람 — AlarmDataProvider 가 REINBOUND 를 동일 토픽으로 송신
  const alarms = computed<RtAlarm[]>(() => [...((store as any).rtAlarms || [])]);

  const activeAlarmCount = computed(() => alarms.value.length);
  const hasActiveAlarms = computed(() => activeAlarmCount.value > 0);

  const hasErrorAlarms = computed(() =>
    alarms.value.some((a) => !isAcknowledged(a.alarmId) && (a.severity ?? 0) >= 2),
  );

  const filteredAlarms = computed(() => {
    let result = [...alarms.value];

    result.sort((a, b) => {
      const aAcked = isAcknowledged(a.alarmId);
      const bAcked = isAcknowledged(b.alarmId);
      if (aAcked !== bAcked) return aAcked ? 1 : -1;
      return (b.occurredAt || 0) - (a.occurredAt || 0);
    });

    if (filter.value === 'equipment') {
      return result.filter((a) => a.alarmType === 'EQUIPMENT');
    }
    if (filter.value === 'job') {
      return result.filter((a) => a.alarmType === 'JOB_ERROR');
    }
    if (filter.value === 'reinbound') {
      return result.filter((a) => a.alarmType === 'REINBOUND');
    }
    return result;
  });

  const isAcknowledged = (alarmId: string): boolean => {
    return acknowledgedAlarmIds.value.has(alarmId);
  };

  const handleHeaderClick = () => {
    if (wasDragged.value) {
      wasDragged.value = false;
      return;
    }

    isCollapsed.value = !isCollapsed.value;

    nextTick(() => {
      clampPanelToContainer();
    });
  };

  const handleResize = () => {
    nextTick(() => {
      clampPanelToContainer();
    });
  };

  watch(isCollapsed, async () => {
    await nextTick();
    clampPanelToContainer();
  });

  const getContainerEl = (): HTMLElement | null => {
    const panel = panelRef.value;
    if (!panel) return null;
    return panel.offsetParent as HTMLElement | null;
  };

  const startDrag = (e: MouseEvent) => {
    if (e.button !== 0) return;

    const target = e.target as HTMLElement;
    if (target.closest('button, a, input, textarea, select, .no-drag')) return;

    const panel = panelRef.value;
    const container = getContainerEl();
    if (!panel || !container) return;

    const panelRect = panel.getBoundingClientRect();
    const containerRect = container.getBoundingClientRect();

    isDragging.value = true;
    wasDragged.value = false;

    panelX.value = panelRect.left - containerRect.left;
    panelY.value = panelRect.top - containerRect.top;

    dragStartX.value = e.clientX - panelRect.left;
    dragStartY.value = e.clientY - panelRect.top;

    window.addEventListener('mousemove', onDrag);
    window.addEventListener('mouseup', stopDrag);

    e.preventDefault();
  };

  const onDrag = (e: MouseEvent) => {
    if (!isDragging.value) return;

    const panel = panelRef.value;
    const container = getContainerEl();
    if (!panel || !container) return;

    const containerRect = container.getBoundingClientRect();
    const panelWidth = panel.offsetWidth;
    const panelHeight = panel.offsetHeight;

    let newX = e.clientX - containerRect.left - dragStartX.value;
    let newY = e.clientY - containerRect.top - dragStartY.value;

    if (Math.abs(newX - (panelX.value ?? 0)) > 3 || Math.abs(newY - (panelY.value ?? 0)) > 3) {
      wasDragged.value = true;
    }

    const maxX = Math.max(0, container.clientWidth - panelWidth - 8);
    const maxY = Math.max(0, container.clientHeight - panelHeight - 8);

    newX = Math.max(8, Math.min(newX, maxX));
    newY = Math.max(8, Math.min(newY, maxY));

    panelX.value = newX;
    panelY.value = newY;
  };

  const stopDrag = () => {
    isDragging.value = false;
    window.removeEventListener('mousemove', onDrag);
    window.removeEventListener('mouseup', stopDrag);
  };

  const clampPanelToContainer = () => {
    const panel = panelRef.value;
    const container = getContainerEl();
    if (!panel || !container) return;
    if (panelX.value === null || panelY.value === null) return;

    const panelWidth = panel.offsetWidth;
    const panelHeight = panel.offsetHeight;

    const maxX = Math.max(0, container.clientWidth - panelWidth - 8);
    const maxY = Math.max(0, container.clientHeight - panelHeight - 8);

    panelX.value = Math.max(8, Math.min(panelX.value, maxX));
    panelY.value = Math.max(8, Math.min(panelY.value, maxY));
  };

  const getSeverityClass = (severity?: number) => {
    switch (severity) {
      case 0:
        return 'severity-info';
      case 1:
        return 'severity-warning';
      case 2:
        return 'severity-error';
      case 3:
        return 'severity-critical';
      default:
        return 'severity-error';
    }
  };

  const getAlarmTypeClass = (type?: string) => {
    if (type === 'EQUIPMENT') return 'type-equipment';
    if (type === 'REINBOUND') return 'type-reinbound';
    return 'type-job-error';
  };

  const getAlarmTypeIcon = (type?: string) => {
    if (type === 'EQUIPMENT') return '⚙️';
    if (type === 'REINBOUND') return '🔁';
    return '📋';
  };

  const getEquipmentTypeLabel = (type?: string) => {
    if (!type) return '설비';
    const labels: Record<string, string> = {
      SHUTTLE: '셔틀',
      CONVEYOR: '컨베이어',
      LIFTER: '리프터',
      RACK: '랙',
      PALLET: '파렛트',
    };
    return labels[type.toUpperCase()] || type;
  };

  const formatTime = (timestamp?: number) => {
    if (!timestamp) return '-';
    try {
      const date = new Date(timestamp);
      return date.toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
    } catch {
      return '-';
    }
  };

  const acknowledgeAll = () => {
    alarms.value.forEach((alarm) => {
      acknowledgedAlarmIds.value.add(alarm.alarmId);
    });
    saveAcknowledgedAlarms();
  };

  const saveAcknowledgedAlarms = () => {
    try {
      const arr = Array.from(acknowledgedAlarmIds.value);
      const recent = arr.slice(-100);
      localStorage.setItem('TSPG_ACKNOWLEDGED_ALARMS', JSON.stringify(recent));
    } catch (e) {
      console.warn('Failed to save acknowledged alarms:', e);
    }
  };

  const loadAcknowledgedAlarms = () => {
    try {
      const saved = localStorage.getItem('TSPG_ACKNOWLEDGED_ALARMS');
      if (saved) {
        const arr = JSON.parse(saved) as string[];
        acknowledgedAlarmIds.value = new Set(arr);
      }
    } catch (e) {
      console.warn('Failed to load acknowledged alarms:', e);
    }
  };

  onMounted(() => {
    loadAcknowledgedAlarms();
    window.addEventListener('resize', handleResize);
  });

  onUnmounted(() => {
    window.removeEventListener('mousemove', onDrag);
    window.removeEventListener('mouseup', stopDrag);
    window.removeEventListener('resize', handleResize);
  });
</script>

<style scoped>
  .alarm-panel {
    position: absolute;
    top: 12px;
    right: 12px;
    z-index: 600;
    background: rgba(20, 22, 28, 0.95);
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 10px;
    width: 320px;
    height: 380px;
    backdrop-filter: blur(8px);
    user-select: none;
    display: flex;
    flex-direction: column;
    transition: border-color 0.3s, box-shadow 0.2s;
    overflow: hidden;
    box-sizing: border-box;
    /* 캔버스(dashboard-body) 안에서만 표시되도록 부모 기준 max-height */
    max-width: calc(100% - 24px);
    max-height: calc(100% - 24px);
  }

  .alarm-panel.dragging {
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
    cursor: grabbing;
  }

  .alarm-panel.has-alarms {
    border-color: rgba(230, 162, 60, 0.4);
  }

  .alarm-panel.collapsed {
    height: auto;
    width: auto;
    min-width: 150px;
  }

  .alarm-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 14px;
    cursor: grab;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    flex-shrink: 0;
    user-select: none;
  }

  .alarm-panel.dragging .alarm-header {
    cursor: grabbing;
  }

  .alarm-panel.collapsed .alarm-header {
    border-bottom: none;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .drag-handle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    color: #606266;
    font-size: 10px;
    letter-spacing: -2px;
    cursor: grab;
  }

  .alarm-panel.dragging .drag-handle {
    cursor: grabbing;
  }

  .alarm-icon {
    font-size: 12px;
  }

  .alarm-icon.has-error {
    animation: blink 1s infinite;
  }

  @keyframes blink {
    0%,
    50% {
      opacity: 1;
    }
    51%,
    100% {
      opacity: 0.3;
    }
  }

  .header-title {
    font-size: 12px;
    font-weight: 600;
    color: #e5eaf3;
  }

  .alarm-count {
    background: rgba(245, 108, 108, 0.3);
    color: #f56c6c;
    font-size: 10px;
    padding: 2px 6px;
    border-radius: 10px;
    font-weight: 600;
  }

  .toggle-icon {
    font-size: 10px;
    color: #909399;
  }

  .alarm-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow: hidden;
  }

  .alarm-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    flex-shrink: 0;
  }

  .alarm-filters {
    display: flex;
    gap: 4px;
  }

  .filter-btn {
    padding: 3px 10px;
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 4px;
    color: #909399;
    font-size: 11px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .filter-btn:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  .filter-btn.active {
    background: rgba(64, 158, 255, 0.2);
    border-color: #409eff;
    color: #409eff;
  }

  .ack-all-btn {
    padding: 3px 8px;
    background: rgba(103, 194, 58, 0.2);
    border: 1px solid rgba(103, 194, 58, 0.3);
    border-radius: 4px;
    color: #67c23a;
    font-size: 10px;
    cursor: pointer;
    transition: all 0.2s;
  }

  .ack-all-btn:hover {
    background: rgba(103, 194, 58, 0.3);
  }

  .alarm-list {
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 8px;
  }

  .alarm-item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 10px;
    margin-bottom: 6px;
    background: rgba(255, 255, 255, 0.03);
    border-radius: 6px;
    border-left: 3px solid transparent;
    transition: all 0.2s;
  }

  .alarm-item:last-child {
    margin-bottom: 0;
  }

  .alarm-item.acknowledged {
    opacity: 0.4;
  }

  .alarm-item.severity-info {
    border-left-color: #909399;
  }

  .alarm-item.severity-warning {
    border-left-color: #e6a23c;
    background: rgba(230, 162, 60, 0.08);
  }

  .alarm-item.severity-error {
    border-left-color: #f56c6c;
    background: rgba(245, 108, 108, 0.08);
  }

  .alarm-item.severity-critical {
    border-left-color: #ff4757;
    background: rgba(255, 71, 87, 0.12);
    animation: critical-pulse 2s infinite;
  }

  @keyframes critical-pulse {
    0%,
    100% {
      background: rgba(255, 71, 87, 0.12);
    }
    50% {
      background: rgba(255, 71, 87, 0.2);
    }
  }

  .alarm-type-icon {
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 6px;
    font-size: 14px;
    flex-shrink: 0;
  }

  .alarm-type-icon.type-equipment {
    background: rgba(64, 158, 255, 0.2);
  }

  .alarm-type-icon.type-job-error {
    background: rgba(230, 162, 60, 0.2);
  }

  .alarm-content {
    flex: 1;
    min-width: 0;
  }

  .alarm-header-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 4px;
  }

  .severity-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .severity-dot.severity-info {
    background: #909399;
  }

  .severity-dot.severity-warning {
    background: #e6a23c;
  }

  .severity-dot.severity-error {
    background: #f56c6c;
  }

  .severity-dot.severity-critical {
    background: #ff4757;
    animation: blink 0.5s infinite;
  }

  .equipment-type {
    font-size: 10px;
    color: #909399;
    background: rgba(255, 255, 255, 0.1);
    padding: 1px 5px;
    border-radius: 3px;
  }

  .equipment-code {
    font-size: 12px;
    font-weight: 600;
    color: #e5eaf3;
  }

  .alarm-error-code {
    font-size: 11px;
    color: #f56c6c;
    font-weight: 600;
    margin-bottom: 2px;
  }

  .alarm-message {
    font-size: 11px;
    color: #c0c4cc;
    margin-bottom: 4px;
    line-height: 1.4;
    word-break: break-word;
  }

  .alarm-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 4px;
  }

  .order-info,
  .barcode-info {
    font-size: 10px;
    color: #909399;
    background: rgba(255, 255, 255, 0.05);
    padding: 1px 4px;
    border-radius: 2px;
  }

  .alarm-time {
    font-size: 10px;
    color: #606266;
  }

  .no-alarms {
    text-align: center;
    padding: 30px 20px;
    color: #606266;
    font-size: 12px;
  }
</style>
