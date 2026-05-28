<template>
  <section class="access-point-table-panel">
    <div class="access-point-table-panel__header">
      <div>
        <p class="access-point-table-panel__eyebrow">ACCESS POINT LIST</p>
        <h3 class="access-point-table-panel__title">Access Point 목록</h3>
      </div>

      <span class="access-point-table-panel__count">{{ rows.length }}건</span>
    </div>

    <div class="access-point-table-panel__body">
      <table class="asrs-ui-table">
        <thead>
          <tr>
            <th>Area</th>
            <th>Point Code</th>
            <th>Point Name</th>
            <th>Type</th>
            <th>Coordinate</th>
            <th>Purpose</th>
            <th>Sort</th>
            <th>Active</th>
          </tr>
        </thead>

        <tbody>
          <tr v-if="loading">
            <td colspan="8" class="access-point-table-panel__empty"> 조회 중입니다... </td>
          </tr>

          <tr v-else-if="!rows.length">
            <td colspan="8" class="access-point-table-panel__empty"> 데이터가 없습니다. </td>
          </tr>

          <tr
            v-for="row in rows"
            :key="`${row.areaCode}-${row.pointCode}`"
            :class="{
              'asrs-ui-table__row--selected': selectedKey === `${row.areaCode}-${row.pointCode}`,
            }"
            @click="$emit('select', row)"
          >
            <td>{{ row.areaCode }}</td>
            <td class="asrs-ui-table__key">{{ row.pointCode }}</td>
            <td>{{ row.pointName }}</td>
            <td>{{ row.pointType }}</td>
            <td>{{ formatCoordinate(row) }}</td>
            <td>{{ row.purposeCodes || '-' }}</td>
            <td>{{ row.useForSortYn }}</td>
            <td>{{ row.activeYn }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
  import type { AccessPointRow } from '../types';

  defineProps<{
    rows: AccessPointRow[];
    selectedKey?: string;
    loading?: boolean;
  }>();

  defineEmits<{
    (e: 'select', row: AccessPointRow): void;
  }>();

  function formatCoordinate(row: AccessPointRow) {
    return `A:${row.aisleNo} / ${row.sideCode} / B:${row.bayNo} / L:${row.levelNo} / D:${row.depthNo}`;
  }
</script>
