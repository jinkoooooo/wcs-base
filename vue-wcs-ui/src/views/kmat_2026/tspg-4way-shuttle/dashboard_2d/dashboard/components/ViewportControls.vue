<!--
  ViewportControls.vue
  대시보드 뷰포트 줌/팬 컨트롤 컴포넌트

  ============================================
  기능 설명
  ============================================
  화면 우측 상단에 표시되는 줌/팬 컨트롤 패널입니다.

  버튼:
  - (+) 확대: 줌 레벨 증가
  - (-) 축소: 줌 레벨 감소
  - 100%: 줌 레벨 100%로 리셋
  - FIT: 화면에 맞게 자동 조절

  안내:
  - 휠: 마우스 휠로 줌
  - Space+드래그: 캔버스 이동
  - 휠클릭 드래그: 캔버스 이동
-->

<template>
  <div class="viewport-controls" @click.stop>
    <!-- 확대 버튼 -->
    <button class="vc-btn" title="확대 (Ctrl+마우스휠 위)" @click="$emit('zoom-in')"> + </button>

    <!-- 축소 버튼 -->
    <button class="vc-btn" title="축소 (Ctrl+마우스휠 아래)" @click="$emit('zoom-out')"> - </button>

    <!-- 100% 리셋 버튼 -->
    <button class="vc-btn" title="100% 크기로 보기" @click="$emit('reset-zoom')"> 100% </button>

    <!-- 화면 맞춤 버튼 -->
    <button class="vc-btn primary" title="화면에 맞추기" @click="$emit('fit-to-page')">
      FIT
    </button>

    <!-- 조작 안내 -->
    <div class="vc-hint">
      <span>휠: 줌</span>
      <span>Space+드래그: 팬</span>
      <span>휠클릭 드래그: 팬</span>
    </div>
  </div>
</template>

<script setup lang="ts">
  /**
   * ViewportControls - 줌/팬 컨트롤 컴포넌트
   *
   * Events:
   * - zoom-in: 확대 버튼 클릭
   * - zoom-out: 축소 버튼 클릭
   * - reset-zoom: 100% 버튼 클릭
   * - fit-to-page: FIT 버튼 클릭
   */

  defineEmits<{
    (e: 'zoom-in'): void;
    (e: 'zoom-out'): void;
    (e: 'reset-zoom'): void;
    (e: 'fit-to-page'): void;
  }>();
</script>

<style scoped>
  .viewport-controls {
    position: absolute;
    top: 12px;
    right: 12px;
    z-index: 600;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px;
    border-radius: 12px;
    background: rgba(20, 22, 28, 0.7);
    border: 1px solid rgba(255, 255, 255, 0.14);
    backdrop-filter: blur(6px);
    user-select: none;
  }

  .vc-btn {
    height: 30px;
    padding: 0 10px;
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.14);
    background: rgba(255, 255, 255, 0.06);
    color: rgba(229, 234, 243, 0.9);
    cursor: pointer;
    font-size: 12px;
    font-weight: 600;
    transition: background 0.2s;
  }

  .vc-btn:hover {
    background: rgba(255, 255, 255, 0.12);
  }

  .vc-btn.primary {
    border-color: rgba(64, 158, 255, 0.35);
    background: rgba(64, 158, 255, 0.18);
    color: #cfe6ff;
  }

  .vc-btn.primary:hover {
    background: rgba(64, 158, 255, 0.28);
  }

  .vc-hint {
    display: flex;
    flex-direction: column;
    gap: 2px;
    margin-left: 6px;
    font-size: 10px;
    color: rgba(229, 234, 243, 0.55);
    line-height: 1.2;
  }
</style>
