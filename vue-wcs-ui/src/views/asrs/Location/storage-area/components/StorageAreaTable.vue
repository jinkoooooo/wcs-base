<template>
  <section class="storage-area-table-panel">
    <div class="storage-area-table-panel__header">
      <div>
        <p class="storage-area-table-panel__eyebrow">STORAGE AREA LIST</p>
        <h3 class="storage-area-table-panel__title">아레아 목록</h3>
      </div>

      <span class="storage-area-table-panel__count">{{ rows.length }}건</span>
    </div>

    <div class="storage-area-table-panel__body">
      <table class="asrs-ui-table">
        <thead>
        <tr>
          <th>Center</th>
          <th>Area Code</th>
          <th>Area Name</th>
          <th>Area Type</th>
          <th>Operation Profile</th>
          <th>Location Profiles</th>
          <th>Locations</th>
          <th>Active</th>
        </tr>
        </thead>

        <tbody>
        <tr v-if="loading">
          <td colspan="8" class="storage-area-table-panel__empty">조회 중입니다...</td>
        </tr>

        <tr v-else-if="!rows.length">
          <td colspan="8" class="storage-area-table-panel__empty">데이터가 없습니다.</td>
        </tr>

        <tr
          v-for="row in rows"
          :key="`${row.centerCode}-${row.areaCode}`"
          :class="{
              'asrs-ui-table__row--selected':
                selectedKey === `${row.centerCode}-${row.areaCode}`,
            }"
          @click="$emit('select', row)"
        >
          <td>{{ row.centerCode }}</td>
          <td class="asrs-ui-table__key">{{ row.areaCode }}</td>
          <td>{{ row.areaName }}</td>
          <td>{{ row.areaType }}</td>
          <td>{{ row.operationProfileCode }}</td>
          <td>{{ row.linkedLocationProfileCount }}</td>
          <td>{{ row.linkedLocationCount }}</td>
          <td>{{ row.activeYn }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { StorageAreaRow } from '../types';

defineProps<{
  rows: StorageAreaRow[];
  selectedKey?: string;
  loading?: boolean;
}>();

defineEmits<{
  (e: 'select', row: StorageAreaRow): void;
}>();
</script>
