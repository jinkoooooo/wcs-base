<template>
  <section class="location-table-panel">
    <div class="location-table-panel__header">
      <div>
        <p class="location-table-panel__eyebrow">LOCATION LIST</p>
        <h3 class="location-table-panel__title">로케이션 목록</h3>
      </div>

      <span class="location-table-panel__count">{{ rows.length }}건</span>
    </div>

    <div class="location-table-panel__body">
      <table class="asrs-ui-table">
        <thead>
        <tr>
          <th>Area</th>
          <th>Location Code</th>
          <th>Type</th>
          <th>Usage</th>
          <th>Coordinate</th>
          <th>Grade</th>
          <th>Sort Seq</th>
          <th>Active</th>
        </tr>
        </thead>

        <tbody>
        <tr v-if="loading">
          <td colspan="8" class="location-table-panel__empty">
            조회 중입니다...
          </td>
        </tr>

        <tr v-else-if="!rows.length">
          <td colspan="8" class="location-table-panel__empty">
            데이터가 없습니다.
          </td>
        </tr>

        <tr
          v-for="row in rows"
          :key="`${row.areaCode}-${row.locationCode}`"
          :class="{
              'asrs-ui-table__row--selected':
                selectedKey === `${row.areaCode}-${row.locationCode}`,
            }"
          @click="$emit('select', row)"
        >
          <td>{{ row.areaCode }}</td>
          <td class="asrs-ui-table__key">{{ row.locationCode }}</td>
          <td>{{ row.locationType }}</td>
          <td>{{ row.usageStatusCode }}</td>
          <td>{{ formatCoordinate(row) }}</td>
          <td>{{ row.locationGrade }}</td>
          <td>{{ row.sortSeq ?? '-' }}</td>
          <td>{{ row.activeYn }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { LocationRow } from '../types';

defineProps<{
  rows: LocationRow[];
  selectedKey?: string;
  loading?: boolean;
}>();

defineEmits<{
  (e: 'select', row: LocationRow): void;
}>();

function formatCoordinate(row: LocationRow) {
  return `A:${row.aisleNo} / ${row.sideCode} / B:${row.bayNo} / L:${row.levelNo} / D:${row.depthNo}`;
}
</script>
