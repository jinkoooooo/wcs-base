<template>
  <section class="location-grade-table-panel">
    <div class="location-grade-table-panel__header">
      <div>
        <p class="location-grade-table-panel__eyebrow">LOCATION GRADE PREVIEW</p>
        <h3 class="location-grade-table-panel__title">로케이션 등급 미리보기</h3>
      </div>

      <span class="location-grade-table-panel__count">
        {{ rows.length }} / {{ totalCount }}건
      </span>
    </div>

    <div class="location-grade-table-panel__body">
      <table class="asrs-ui-table">
        <thead>
          <tr>
            <th>Location</th>
            <th>Aisle</th>
            <th>Side</th>
            <th>Bay</th>
            <th>Level</th>
            <th>Depth</th>
            <th>Front</th>
            <th>Score</th>
            <th>Sort</th>
            <th>Grade</th>
            <th>Access Point</th>
          </tr>
        </thead>

        <tbody>
          <tr v-if="loading">
            <td colspan="11" class="location-grade-table-panel__empty"> 조회 중입니다... </td>
          </tr>

          <tr v-else-if="!rows.length">
            <td colspan="11" class="location-grade-table-panel__empty">
              미리보기 데이터가 없습니다.
            </td>
          </tr>

          <tr v-for="row in rows" :key="row.locationId || row.locationCode">
            <td class="asrs-ui-table__key">{{ row.locationCode }}</td>
            <td>{{ row.aisleNo }}</td>
            <td>{{ row.sideCode }}</td>
            <td>{{ row.bayNo }}</td>
            <td>{{ row.levelNo }}</td>
            <td>{{ row.depthNo }}</td>
            <td>{{ row.frontPriorityYn }}</td>
            <td>{{ row.accessScore ?? '-' }}</td>
            <td>{{ row.newSortSeq ?? '-' }}</td>
            <td>
              <span
                :class="[
                  'location-grade-table-panel__grade',
                  `location-grade-table-panel__grade--${row.newLocationGrade}`,
                ]"
              >
                {{ row.newLocationGrade || '-' }}
              </span>
            </td>
            <td>{{ row.primaryAccessPointCode || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
  import type { LocationGradePreviewRow } from '../types';

  defineProps<{
    rows: LocationGradePreviewRow[];
    totalCount: number;
    loading?: boolean;
  }>();
</script>
