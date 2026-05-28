<template>
  <thead>
    <tr>
      <th
        v-for="(col, idx) in columns"
        :key="col.key"
        :class="['th-' + col.key, col.align ? 'th-align-' + col.align : '']"
      >
        <template v-if="col.key === 'select'">
          <label class="row-checkbox-label">
            <input
              type="checkbox"
              class="row-checkbox"
              :checked="allSelected"
              :indeterminate.prop="someSelected && !allSelected"
              @change="$emit('toggle-select-all')"
            />
          </label>
        </template>
        <template v-else>
          <span class="th-label">{{ col.label }}</span>
        </template>
        <span
          v-if="idx < columns.length - 1"
          class="col-resizer"
          :class="{ 'is-resizing': resizingColKey === col.key }"
          title="드래그: 너비 조절 / 더블클릭: 자동 맞춤"
          @mousedown.prevent.stop="$emit('start-col-resize', { event: $event, colKey: col.key })"
          @dblclick.prevent.stop="$emit('auto-fit-column', col.key)"
        ></span>
      </th>
    </tr>
  </thead>
</template>

<script lang="ts" setup>
import type { ColDef } from '../../shared';

defineProps<{
  columns: ColDef[];
  resizingColKey: string | null;
  allSelected: boolean;
  someSelected: boolean;
}>();

defineEmits<{
  (e: 'toggle-select-all'): void;
  (e: 'start-col-resize', payload: { event: MouseEvent; colKey: string }): void;
  (e: 'auto-fit-column', colKey: string): void;
}>();
</script>

<style scoped>
th {
  position: relative;
  background: #f8fafc;
  font-weight: 600;
  font-size: 11px;
  color: var(--c-text-2);
  text-transform: uppercase;
  letter-spacing: 0.4px;
  text-align: left;
  padding: 10px 12px;
  border-bottom: 1px solid var(--c-border);
  white-space: nowrap;
  overflow: hidden;
  user-select: none;
}
th.th-align-right { text-align: right; }
th.th-select {
  padding: 10px;
  text-align: center;
}
.th-label {
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  max-width: calc(100% - 8px);
}
.row-checkbox-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  cursor: pointer;
}
.row-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--c-primary);
  margin: 0;
}

.col-resizer {
  position: absolute;
  top: 0;
  right: 0;
  width: 6px;
  height: 100%;
  cursor: col-resize;
  user-select: none;
  z-index: 3;
}
.col-resizer::before {
  content: '';
  position: absolute;
  top: 25%;
  bottom: 25%;
  right: 2px;
  width: 2px;
  background: transparent;
  border-radius: 1px;
  transition: background 0.15s;
}
.col-resizer:hover::before,
.col-resizer.is-resizing::before {
  background: var(--c-primary);
}
.col-resizer.is-resizing {
  background: rgba(49, 130, 246, 0.08);
}
</style>
