<!--
  EquipmentShuttleSection.vue
  EquipmentDetailPopup 의 SHUTTLE 전용 섹션.

  ============================================
  설명
  ============================================
  - 셔틀이 선택됐을 때만 EquipmentDetailPopup 안에서 렌더된다.
  - 이전 ShuttleControlPopup 의 기능(수동 이동, 정지/재시작) 을 이쪽으로 이전.
  - 작업 취소/재개는 EquipmentDetailPopup 의 "작업 제어" 섹션에서 이미 처리되므로 중복 노출하지 않는다.
  - 부모 popup 한 파일이 비대해지지 않도록 분리.
-->

<template>
  <section class="info-section shuttle-section">
    <div class="section-label">셔틀 제어</div>

    <!-- 셔틀 상태 요약 -->
    <div class="info-row" v-if="batteryLevel != null">
      <span class="label">배터리</span>
      <span class="value" :class="batteryClass">
        {{ batteryLevel }}%
      </span>
    </div>
    <div class="info-row" v-if="hasCargo != null">
      <span class="label">화물 적재</span>
      <span class="value" :class="hasCargo ? 'has-cargo' : ''">
        {{ hasCargo ? '있음' : '없음' }}
      </span>
    </div>
    <div class="info-row" v-if="currentCellId">
      <span class="label">현재 셀</span>
      <span class="value">{{ currentCellId }}</span>
    </div>
    <div class="info-row" v-if="movementStatusText">
      <span class="label">이동 상태</span>
      <span class="value" :class="movementStatusClass">
        {{ movementStatusText }}
      </span>
    </div>

    <!-- 수동 이동 -->
    <div class="ctl-block">
      <label class="ctl-label">수동 이동</label>
      <div class="ctl-row">
        <input
          v-model="targetPointCode"
          type="text"
          placeholder="목표 포인트 코드 (예: 1-01-01)"
          class="ctrl-input"
        />
        <button
          v-if="can('update')"
          class="control-btn btn-move"
          :disabled="!targetPointCode.trim() || loading"
          @click="onMove"
        >
          ▶ 이동
        </button>
      </div>
    </div>

    <!-- 정지 / 재시작 -->
    <div class="ctl-row ctl-buttons">
      <button v-if="can('update')" class="control-btn btn-danger" :disabled="loading" @click="onStop">
        ⏹ 정지
      </button>
      <button v-if="can('update')" class="control-btn btn-resume" :disabled="loading" @click="onRestart">
        ▶ 재시작
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
  import { computed, ref } from 'vue';
  import { useEcsCommands, useToast } from '../../composables';
  import {
    ShuttleMovementStatus,
    getShuttleBatteryLevel,
    enumLabel,
    enumClass,
  } from '../../../constants';
  import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';

  const MENU = 'Dashboard2D';
  const { can } = usePermissionLocal(MENU);

  const props = defineProps<{
    /** 셔틀 코드(=equipmentCode 또는 realEqId) */
    shuttleCode: string;
    /** 센터 ID (lcId) */
    lcId: string;
    /** 팝업이 노출하는 셔틀 의 merge 된 실시간 데이터 (배터리/cargo/cell 등) */
    shuttleData?: any;
  }>();

  // ============================================
  // 셔틀 상태 표시 — 라벨/CSS 클래스는 모두 constants 의 매핑 lookup
  // ============================================

  const batteryLevel = computed<number | null>(() => {
    const v = props.shuttleData?.batteryLevel;
    return typeof v === 'number' ? v : null;
  });

  const batteryClass = computed(() => {
    // getShuttleBatteryLevel 은 디스크립터 항목 자체를 반환 — className 직접 추출
    const stage = getShuttleBatteryLevel(batteryLevel.value);
    return stage?.className ?? '';
  });

  const hasCargo = computed<boolean | null>(() => {
    const v = props.shuttleData?.hasCargo ?? props.shuttleData?.cargoYn;
    return typeof v === 'boolean' ? v : null;
  });

  const currentCellId = computed<string>(() => {
    return String(
      props.shuttleData?.cellId ??
        props.shuttleData?.currentPointCode ??
        props.shuttleData?.currentFromLoc ??
        '',
    );
  });

  const movementStatusText = computed(() =>
    enumLabel(ShuttleMovementStatus, props.shuttleData?.movementStatus),
  );

  const movementStatusClass = computed(() =>
    enumClass(ShuttleMovementStatus, props.shuttleData?.movementStatus),
  );

  // ============================================
  // 제어 (ECS 명령)
  // ============================================

  const targetPointCode = ref('');
  const loading = ref(false);

  const lcIdRef = computed(() => props.lcId);
  const toast = useToast();
  const ecsCommands = useEcsCommands(lcIdRef, toast.showToast);

  async function onStop() {
    if (!props.shuttleCode) return;
    if (!confirm(`[${props.shuttleCode}] 셔틀을 정지하시겠습니까?`)) return;
    loading.value = true;
    try {
      await ecsCommands.handleStopShuttle(props.shuttleCode);
    } finally {
      loading.value = false;
    }
  }

  async function onRestart() {
    if (!props.shuttleCode) return;
    if (!confirm(`[${props.shuttleCode}] 셔틀을 재시작하시겠습니까?`)) return;
    loading.value = true;
    try {
      await ecsCommands.handleRestartShuttle(props.shuttleCode);
    } finally {
      loading.value = false;
    }
  }

  async function onMove() {
    if (!props.shuttleCode || !targetPointCode.value.trim()) return;
    const target = targetPointCode.value.trim();
    if (!confirm(`[${props.shuttleCode}] 셔틀을 [${target}] 위치로 이동하시겠습니까?`)) return;
    loading.value = true;
    try {
      await ecsCommands.handleMoveShuttle(props.shuttleCode, target, () => {
        targetPointCode.value = '';
      });
    } finally {
      loading.value = false;
    }
  }
</script>

<style scoped>
  .shuttle-section {
    margin-bottom: 12px;
  }

  .section-label {
    font-size: 0.7rem;
    color: #858585;
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-bottom: 10px;
  }

  .info-row {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 8px;
    font-size: 0.85rem;
  }

  .info-row .label {
    color: #909399;
  }

  .info-row .value {
    font-weight: 500;
    text-align: right;
    color: #e5eaf3;
  }

  .ctl-block {
    margin-top: 8px;
  }

  .ctl-label {
    display: block;
    font-size: 11px;
    color: #909399;
    margin-bottom: 6px;
  }

  .ctl-row {
    display: flex;
    gap: 6px;
    align-items: center;
  }

  .ctl-buttons {
    margin-top: 10px;
  }

  .ctrl-input {
    flex: 1;
    min-width: 0;
    padding: 6px 10px;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 4px;
    color: #e5eaf3;
    font-size: 12px;
    box-sizing: border-box;
  }

  .ctrl-input:focus {
    outline: none;
    border-color: #409eff;
  }

  .control-btn {
    padding: 6px 12px;
    border: 1px solid;
    border-radius: 4px;
    cursor: pointer;
    font-size: 12px;
    font-weight: 600;
    flex: 1;
  }

  .control-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  .btn-move {
    background: rgba(64, 158, 255, 0.15);
    border-color: rgba(64, 158, 255, 0.4);
    color: #409eff;
    flex: 0 0 auto;
  }

  .btn-move:hover:not(:disabled) {
    background: rgba(64, 158, 255, 0.25);
  }

  .btn-danger {
    background: rgba(245, 108, 108, 0.15);
    border-color: rgba(245, 108, 108, 0.4);
    color: #f56c6c;
  }

  .btn-danger:hover:not(:disabled) {
    background: rgba(245, 108, 108, 0.25);
  }

  .btn-resume {
    background: rgba(103, 194, 58, 0.15);
    border-color: rgba(103, 194, 58, 0.4);
    color: #67c23a;
  }

  .btn-resume:hover:not(:disabled) {
    background: rgba(103, 194, 58, 0.25);
  }

  /*
   * 상태 / 배터리 색상 클래스는 tspg-4way-shuttle/styles/tokens.scss 의
   * 전역 utility (tspg-status-*, tspg-battery-*) 를 그대로 사용한다.
   * 이 파일에서는 절대 색상을 다시 정의하지 않는다.
   */
  .has-cargo {
    color: var(--tspg-color-warning);
  }
</style>
