<template>
  <section class="operation-profile-table-panel">
    <div class="operation-profile-table-panel__header">
      <div>
        <p class="operation-profile-table-panel__eyebrow">OPERATION PROFILE LIST</p>
        <h3 class="operation-profile-table-panel__title">오퍼레이션 프로필 목록</h3>
      </div>

      <span class="operation-profile-table-panel__count">{{ rows.length }}건</span>
    </div>

    <div class="operation-profile-table-panel__body">
      <table class="asrs-ui-table">
        <thead>
        <tr>
          <th>Profile Code</th>
          <th>Profile Name</th>
          <th>Industry Type</th>
          <th>Active</th>
          <th>Linked Areas</th>
          <th>Updated At</th>
        </tr>
        </thead>

        <tbody>
        <tr v-if="loading">
          <td colspan="6" class="operation-profile-table-panel__empty">조회 중입니다...</td>
        </tr>

        <tr v-else-if="!rows.length">
          <td colspan="6" class="operation-profile-table-panel__empty">데이터가 없습니다.</td>
        </tr>

        <tr
          v-for="row in rows"
          :key="row.profileCode"
          :class="{ 'asrs-ui-table__row--selected': selectedProfileCode === row.profileCode }"
          @click="$emit('select', row)"
        >
          <td class="asrs-ui-table__key">{{ row.profileCode }}</td>
          <td>{{ row.profileName }}</td>
          <td>{{ row.industryType }}</td>
          <td>{{ row.activeYn }}</td>
          <td>{{ row.linkedAreaCount }}</td>
          <td>{{ row.updatedAt || '-' }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { OperationProfileRow } from '../types';

defineProps<{
  rows: OperationProfileRow[];
  selectedProfileCode?: string;
  loading?: boolean;
}>();

defineEmits<{
  (e: 'select', row: OperationProfileRow): void;
}>();
</script>
