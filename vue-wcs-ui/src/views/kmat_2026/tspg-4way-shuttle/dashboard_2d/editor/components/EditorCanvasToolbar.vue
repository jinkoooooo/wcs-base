<template>
  <div class="canvas-toolbar">
    <div class="toolbar-group">
      <label>캔버스 크기:</label>
      <input
        type="number"
        class="size-input"
        min="100"
        :value="canvasWidth"
        @change="onWidthChange"
      />
      <span>x</span>
      <input
        type="number"
        class="size-input"
        min="100"
        :value="canvasHeight"
        @change="onHeightChange"
      />

      <button
        class="tool-btn"
        :disabled="!hasSelection"
        @click="$emit('centerSelection')"
        title="선택된 객체들을 형태 유지한 채 캔버스 중앙으로 이동"
      >
        선택 중앙
      </button>
    </div>

    <div class="toolbar-group">
      <label>배경색:</label>
      <input type="color" class="color-input" :value="backgroundColor" @change="onBgChange" />
    </div>

    <div class="toolbar-group">
      <label>
        <input type="checkbox" :checked="showGrid" @change="onShowGridChange" />
        그리드
      </label>
    </div>

    <div class="toolbar-group">
      <label>
        <input type="checkbox" :checked="snapEnabled" @change="onSnapChange" />
        스냅
      </label>
      <label class="inline">
        <input
          type="checkbox"
          :checked="smartGuides"
          :disabled="!snapEnabled"
          @change="onGuidesChange"
        />
        가이드
      </label>
    </div>

    <div class="toolbar-group">
      <button class="tool-btn" @click="$emit('fit')">Fit</button>
      <button class="tool-btn" @click="$emit('reset')">100%</button>
    </div>

    <div class="toolbar-group">
      <span class="scale-info">{{ Math.round(scale * 100) }}%</span>
    </div>
  </div>
</template>

<script setup lang="ts">
  const props = defineProps<{
    canvasWidth: number;
    canvasHeight: number;
    backgroundColor: string;
    showGrid: boolean;
    snapEnabled: boolean;
    smartGuides: boolean;
    scale: number;
    hasSelection: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'update:canvasWidth', v: number): void;
    (e: 'update:canvasHeight', v: number): void;
    (e: 'update:backgroundColor', v: string): void;
    (e: 'update:showGrid', v: boolean): void;
    (e: 'update:snapEnabled', v: boolean): void;
    (e: 'update:smartGuides', v: boolean): void;

    (e: 'applyCanvasSize'): void;
    (e: 'fit'): void;
    (e: 'reset'): void;
    (e: 'centerSelection'): void;
  }>();

  const onWidthChange = (e: Event) => {
    const v = Number((e.target as HTMLInputElement).value || 0);
    emit('update:canvasWidth', v);
    emit('applyCanvasSize');
  };

  const onHeightChange = (e: Event) => {
    const v = Number((e.target as HTMLInputElement).value || 0);
    emit('update:canvasHeight', v);
    emit('applyCanvasSize');
  };

  const onBgChange = (e: Event) => {
    const v = String((e.target as HTMLInputElement).value || '#FFFFFF');
    emit('update:backgroundColor', v);
    emit('applyCanvasSize');
  };

  const onShowGridChange = (e: Event) => {
    const v = (e.target as HTMLInputElement).checked;
    emit('update:showGrid', v);
  };

  const onSnapChange = (e: Event) => {
    const v = (e.target as HTMLInputElement).checked;
    emit('update:snapEnabled', v);
    // snap 토글은 저장대상이 아니니 applyCanvasSize는 호출 안 함
  };

  const onGuidesChange = (e: Event) => {
    const v = (e.target as HTMLInputElement).checked;
    emit('update:smartGuides', v);
  };
</script>

<style scoped>
  .canvas-toolbar {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 8px 16px;
    background-color: #ffffff;
    border-bottom: 1px solid #e4e7ed;
    flex-shrink: 0;
  }

  .toolbar-group {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: #606266;
  }

  .toolbar-group label {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    line-height: 1;
    white-space: nowrap;
  }

  .toolbar-group input[type='checkbox'] {
    margin: 0;
  }

  .toolbar-group label.inline {
    margin-left: 8px;
    opacity: 0.95;
  }

  .tool-btn {
    padding: 4px 10px;
    border: 1px solid #dcdfe6;
    background: #fff;
    border-radius: 6px;
    font-size: 12px;
    cursor: pointer;
  }
  .tool-btn:hover {
    border-color: #c0c4cc;
  }
  .tool-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .size-input {
    width: 70px;
    padding: 4px 8px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 13px;
  }

  .color-input {
    width: 32px;
    height: 24px;
    padding: 0;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
  }

  .scale-info {
    color: #909399;
    font-size: 12px;
  }
</style>
