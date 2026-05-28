<template>
  <section class="center-table-panel">
    <div class="center-table-panel__header">
      <div>
        <p class="center-table-panel__eyebrow">CENTER LIST</p>
        <h3 class="center-table-panel__title">센터 목록</h3>
      </div>

      <span class="center-table-panel__count">{{ rows.length }}건</span>
    </div>

    <div class="center-table-panel__body">
      <table class="asrs-ui-table">
        <thead>
        <tr>
          <th>Center Code</th>
          <th>Center Name</th>
          <th>Center Type</th>
          <th>Timezone</th>
          <th>Linked Areas</th>
          <th>Active</th>
          <th>Updated At</th>
        </tr>
        </thead>

        <tbody>
        <tr v-if="loading">
          <td colspan="7" class="center-table-panel__empty">조회 중입니다...</td>
        </tr>

        <tr v-else-if="!rows.length">
          <td colspan="7" class="center-table-panel__empty">데이터가 없습니다.</td>
        </tr>

        <tr
          v-for="row in rows"
          :key="row.centerCode"
          :class="{ 'asrs-ui-table__row--selected': selectedCenterCode === row.centerCode }"
          @click="$emit('select', row)"
        >
          <td class="asrs-ui-table__key">{{ row.centerCode }}</td>
          <td>{{ row.centerName }}</td>
          <td>{{ row.centerType }}</td>
          <td>{{ row.timezone }}</td>
          <td>{{ row.linkedAreaCount }}</td>
          <td>{{ row.activeYn }}</td>
          <td>{{ row.updatedAt || '-' }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { CenterRow } from '../types';

defineProps<{
  rows: CenterRow[];
  selectedCenterCode?: string;
  loading?: boolean;
}>();

defineEmits<{
  (e: 'select', row: CenterRow): void;
}>();
</script>
