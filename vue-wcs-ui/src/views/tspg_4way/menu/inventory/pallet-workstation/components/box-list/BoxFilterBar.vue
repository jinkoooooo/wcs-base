<template>
  <div class="box-filter-tabs">
    <button
      type="button"
      class="filter-tab"
      :class="{ active: boxFilter === 'all' }"
      @click="$emit('update:boxFilter', 'all')"
    >
      전체 <span class="filter-count">{{ filterCounts.all }}</span>
    </button>
    <button
      type="button"
      class="filter-tab"
      :class="{ active: boxFilter === 'printed' }"
      @click="$emit('update:boxFilter', 'printed')"
    >
      <span class="filter-dot dot-printed"></span>인쇄됨
      <span class="filter-count">{{ filterCounts.printed }}</span>
    </button>
    <button
      type="button"
      class="filter-tab"
      :class="{ active: boxFilter === 'scanned' }"
      @click="$emit('update:boxFilter', 'scanned')"
    >
      <span class="filter-dot dot-scanned"></span>스캔됨
      <span class="filter-count">{{ filterCounts.scanned }}</span>
    </button>
    <button
      type="button"
      class="filter-tab"
      :class="{ active: boxFilter === 'partial' }"
      @click="$emit('update:boxFilter', 'partial')"
    >
      <span class="filter-dot dot-partial"></span>부분 출고
      <span class="filter-count">{{ filterCounts.partial }}</span>
    </button>
    <button
      type="button"
      class="filter-tab"
      :class="{ active: boxFilter === 'depleted' }"
      @click="$emit('update:boxFilter', 'depleted')"
    >
      <span class="filter-dot dot-depleted"></span>소진됨
      <span class="filter-count">{{ filterCounts.depleted }}</span>
    </button>
    <button
      type="button"
      class="filter-tab"
      :class="{ active: boxFilter === 'pending' }"
      @click="$emit('update:boxFilter', 'pending')"
    >
      <span class="filter-dot dot-pending"></span>대기
      <span class="filter-count">{{ filterCounts.pending }}</span>
    </button>
  </div>
</template>

<script lang="ts" setup>
  import type { BoxFilter } from '../../shared';

  defineProps<{
    boxFilter: BoxFilter;
    filterCounts: {
      all: number;
      printed: number;
      scanned: number;
      partial: number;
      depleted: number;
      pending: number;
    };
  }>();
  defineEmits<{ (e: 'update:boxFilter', v: BoxFilter): void }>();
</script>

<style scoped>
  .box-filter-tabs {
    flex: 0 0 auto;
    display: flex;
    gap: 4px;
    margin-bottom: 10px;
    border-bottom: 1px solid var(--c-border);
    padding-bottom: 0;
  }
  .filter-tab {
    border: none;
    background: transparent;
    padding: 8px 14px;
    font-size: 13px;
    font-weight: 600;
    color: var(--c-text-2);
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    border-bottom: 2px solid transparent;
    transition: all 0.15s;
    margin-bottom: -1px;
  }
  .filter-tab:hover:not(.active) {
    color: var(--c-text);
    background: #f8fafc;
  }
  .filter-tab.active {
    color: var(--c-text);
    border-bottom-color: var(--c-text);
  }
  .filter-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    display: inline-block;
  }
  .dot-printed {
    background: #3182f6;
  }
  .dot-scanned {
    background: var(--c-success);
  }
  .dot-partial {
    background: var(--c-warning);
  }
  .dot-depleted {
    background: var(--c-muted);
  }
  .dot-pending {
    background: #cbd5e1;
  }
  .filter-count {
    font-size: 11px;
    font-weight: 700;
    color: var(--c-muted);
    background: #f1f5f9;
    padding: 1px 6px;
    border-radius: 8px;
    min-width: 18px;
    text-align: center;
  }
  .filter-tab.active .filter-count {
    color: #fff;
    background: var(--c-text);
  }
</style>
