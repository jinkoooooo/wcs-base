<template>
  <section class="location-profile-table-panel">
    <div class="location-profile-table-panel__header">
      <div>
        <p class="location-profile-table-panel__eyebrow">LOCATION PROFILE LIST</p>
        <h3 class="location-profile-table-panel__title">로케이션 프로필 목록</h3>
      </div>

      <span class="location-profile-table-panel__count">{{ rows.length }}건</span>
    </div>

    <div class="location-profile-table-panel__body">
      <table class="asrs-ui-table">
        <thead>
        <tr>
          <th>Area</th>
          <th>Profile Code</th>
          <th>Profile Name</th>
          <th>Location Type</th>
          <th>Range</th>
          <th>Side</th>
          <th>Linked Locations</th>
          <th>Active</th>
        </tr>
        </thead>

        <tbody>
        <tr v-if="loading">
          <td colspan="8" class="location-profile-table-panel__empty">
            조회 중입니다...
          </td>
        </tr>

        <tr v-else-if="!rows.length">
          <td colspan="8" class="location-profile-table-panel__empty">
            데이터가 없습니다.
          </td>
        </tr>

        <tr
          v-for="row in rows"
          :key="`${row.areaCode}-${row.profileCode}`"
          :class="{
              'asrs-ui-table__row--selected':
                selectedKey === `${row.areaCode}-${row.profileCode}`,
            }"
          @click="$emit('select', row)"
        >
          <td>{{ row.areaCode }}</td>
          <td class="asrs-ui-table__key">{{ row.profileCode }}</td>
          <td>{{ row.profileName }}</td>
          <td>{{ row.locationType }}</td>
          <td>{{ formatRange(row) }}</td>
          <td>{{ row.sideCodes }}</td>
          <td>{{ row.linkedLocationCount }}</td>
          <td>{{ row.activeYn }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { LocationProfileRow } from '../types';

defineProps<{
  rows: LocationProfileRow[];
  selectedKey?: string;
  loading?: boolean;
}>();

defineEmits<{
  (e: 'select', row: LocationProfileRow): void;
}>();

function formatRange(row: LocationProfileRow) {
  return `A:${row.aisleStart}-${row.aisleEnd} / B:${row.bayStart}-${row.bayEnd} / L:${row.levelStart}-${row.levelEnd} / D:${row.depthStart}-${row.depthEnd}`;
}
</script>
