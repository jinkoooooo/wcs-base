<template>
  <div class="sim-control">
    <div class="head">
      <span class="title">⚙️ 시뮬 제어</span>
      <span class="hint">설정은 즉시 반영됩니다</span>
    </div>

    <!-- HOST 주기 -->
    <div class="row">
      <span class="lbl">HOST 주기</span>
      <div class="seg">
        <button
          v-for="opt in HOST_INTERVALS"
          :key="opt.ms"
          class="seg-btn"
          :class="{ active: status?.config.hostIntervalMs === opt.ms }"
          @click="onHostInterval(opt.ms)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <!-- PLC 단계 지연 -->
    <div class="row">
      <span class="lbl">PLC 단계 지연</span>
      <div class="seg">
        <button
          v-for="opt in PLC_DELAYS"
          :key="opt.ms"
          class="seg-btn"
          :class="{ active: status?.config.plcStepDelayMs === opt.ms }"
          @click="onPlcDelay(opt.ms)"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>

    <!-- 비율 -->
    <div class="row">
      <span class="lbl"> 작업 수량 (입고 / 출고 / 이동)</span>
      <div class="ratio">
        <input v-model.number="ratioIn"  type="number" min="0" max="100" @input="ratioDirty = true" />
        <input v-model.number="ratioOut" type="number" min="0" max="100" @input="ratioDirty = true" />
<!--        <input v-model.number="ratioMv"  type="number" min="0" max="100" @input="ratioDirty = true" />-->
        <button class="btn btn-apply" @click="onApplyRatio">적용</button>
      </div>
    </div>

    <!-- 정리 -->
    <div class="row cleanup">
      <span class="lbl">시뮬 데이터 정리</span>
      <div class="cleanup-actions">
        <button class="btn btn-preview" @click="onPreview">👁 미리보기</button>
        <button class="btn btn-execute" :disabled="!preview" @click="onExecute">
          🗑 실행
        </button>
      </div>
    </div>

    <div v-if="preview" class="preview">
      <div class="preview-row" v-for="(v, k) in previewView" :key="k">
        <span class="pk">{{ labelOf(k) }}</span>
        <span class="pv">{{ v }}</span>
      </div>
    </div>

    <div v-if="lastResult" class="result">
      ✓ 정리 완료 — host 주문 {{ lastResult.hostOrders }}건, shuttle 주문 {{ lastResult.shuttleOrders }}건,
      재고 {{ lastResult.simStocks }}건 삭제
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue';
  import {
    setHostInterval,
    setPlcStepDelay,
    setRatio,
    cleanupPreview,
    cleanupExecute,
    type SimulatorStatus,
    type CleanupPreview,
    type CleanupResult,
  } from '../api';

  const props = defineProps<{
    status: SimulatorStatus | null,
    eqGroupId: string
  }>();

  const emit = defineEmits<{ (e: 'changed'): void }>();

  const HOST_INTERVALS = [
    { ms: 1000,  label: '1초' },
    { ms: 3000,  label: '3초' },
    { ms: 5000,  label: '5초' },
    { ms: 10000, label: '10초' },
  ];
  const PLC_DELAYS = [
    { ms: 100,   label: '100ms' },
    { ms: 500,   label: '500ms' },
    { ms: 1000,  label: '1초' },
    { ms: 3000,  label: '3초' },
  ];

  const ratioIn = ref(50);
  const ratioOut = ref(50);
  const ratioMv = ref(0);
  const ratioDirty = ref(false);  // 편집중 FLAG

  // SimControlPanel.vue 의 watch 부분 수정

  watch(
    () => [props.status, props.eqGroupId], // status나 선택된 그룹 ID가 바뀔 때 실행
    ([newStatus, newId]) => {
      if (!newStatus || !newId) return;
      if (ratioDirty.value) return; // 사용자가 입력 중일 때는 덮어쓰지 않음

      const statusObj = newStatus as SimulatorStatus;

      // 1. 글로벌 설정 (주기, 지연시간 등) 동기화
      const cfg = statusObj.config;
      if (cfg) {
        // 이 값들은 모든 그룹 공통 설정임
      }

      const myGroup = statusObj.groups.find((g: any) => g.eqGroupId === newId);

      if (myGroup) {
        // DB에 저장된 그룹별 target 값을 화면 입력창(ref)에 할당
        ratioIn.value = myGroup.targetInbound;
        ratioOut.value = myGroup.targetOutbound;
        // ratioMv는 현재 DB에 없으므로 0 혹은 필요시 추가
      }
    },
    { immediate: true, deep: true } // 객체 내부 변경 감지를 위해 deep 옵션 추가
  );

  const preview = ref<CleanupPreview | null>(null);
  const lastResult = ref<CleanupResult | null>(null);

  const previewView = ref<Record<string, number>>({});
  watch(preview, (p) => {
    previewView.value = p ? { ...(p as unknown as Record<string, number>) } : {};
  });

  function labelOf(k: string) {
    switch (k) {
      case 'hostOrders':        return 'HOST 주문';
      case 'hostOrderItems':    return 'HOST 주문 아이템';
      case 'shuttleOrders':     return '셔틀 주문';
      case 'shuttleOrderItems': return '셔틀 주문 아이템';
      case 'simStocks':         return '시뮬 재고';
      default:                  return k;
    }
  }

  async function onHostInterval(ms: number) {
    await setHostInterval(ms);
    emit('changed');
  }

  async function onPlcDelay(ms: number) {
    await setPlcStepDelay(ms);
    emit('changed');
  }

  async function onApplyRatio() {
    if (!props.eqGroupId) return;
    await setRatio(props.eqGroupId, ratioIn.value, ratioOut.value,0);
    ratioDirty.value = false;  // ← 적용 후 다시 동기화 허용
    emit('changed');
  }

  async function onPreview() {
    preview.value = await cleanupPreview();
    lastResult.value = null;
  }

  async function onExecute() {
    if (!preview.value) return;
    if (!window.confirm('시뮬 데이터를 모두 삭제합니다. 운영 데이터는 영향받지 않습니다. 진행할까요?')) return;
    lastResult.value = await cleanupExecute();
    preview.value = null;
    emit('changed');
  }
</script>

<style lang="scss" scoped>
  @use '../styles/sim-tokens' as *;

  .sim-control {
    @include sim-card;
    display: flex;
    flex-direction: column;
    gap: $sim-space-md;
  }

  .head {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    border-bottom: 1px solid $sim-border-default;
    padding-bottom: $sim-space-sm;

    .title { color: $sim-text-primary; font-weight: 600; font-size: 15px; }
    .hint  { color: $sim-text-muted; font-size: 12px; }
  }

  .row {
    display: grid;
    grid-template-columns: 140px 1fr;
    align-items: center;
    gap: $sim-space-md;

    .lbl {
      color: $sim-text-muted;
      font-size: 13px;
    }

    &.cleanup {
      .cleanup-actions {
        display: flex;
        gap: $sim-space-sm;
      }
    }
  }

  .seg {
    display: inline-flex;
    border: 1px solid $sim-border-default;
    border-radius: $sim-radius-md;
    overflow: hidden;
    background: $sim-bg-elevated;

    .seg-btn {
      background: transparent;
      border: none;
      padding: 6px 14px;
      color: $sim-text-secondary;
      cursor: pointer;
      font-size: 13px;
      transition: background $sim-transition-fast, color $sim-transition-fast;

      & + .seg-btn { border-left: 1px solid $sim-border-default; }
      &:hover { background: $sim-bg-card-hover; color: $sim-text-primary; }
      &.active {
        background: $sim-color-host;
        color: #fff;
      }
    }
  }

  .ratio {
    display: flex;
    gap: $sim-space-sm;
    align-items: center;

    input[type='number'] {
      width: 64px;
      height: 32px;
      padding: 0 8px;
      background: $sim-bg-elevated;
      color: $sim-text-primary;
      border: 1px solid $sim-border-default;
      border-radius: $sim-radius-sm;
      font-variant-numeric: tabular-nums;
      &:focus { outline: none; border-color: $sim-color-host; }
    }
  }

  .btn {
    height: 32px;
    padding: 0 14px;
    border: none;
    border-radius: $sim-radius-md;
    color: #fff;
    font-weight: 600;
    cursor: pointer;
    transition: filter $sim-transition-fast;
    &:hover:not(:disabled) { filter: brightness(1.1); }
    &:disabled { opacity: 0.4; cursor: not-allowed; }

    &.btn-apply   { background: $sim-color-host; }
    &.btn-preview { background: $sim-color-portmode; }
    &.btn-execute { background: $sim-state-stopped; }
  }

  .preview {
    margin-top: $sim-space-sm;
    padding: $sim-space-sm $sim-space-md;
    background: $sim-bg-elevated;
    border: 1px solid $sim-border-default;
    border-radius: $sim-radius-md;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .preview-row {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      .pk { color: $sim-text-muted; }
      .pv { color: $sim-text-primary; font-weight: 600; font-variant-numeric: tabular-nums; }
    }
  }

  .result {
    color: $sim-state-running;
    font-size: 13px;
    font-weight: 600;
    padding: $sim-space-sm 0;
  }
</style>
