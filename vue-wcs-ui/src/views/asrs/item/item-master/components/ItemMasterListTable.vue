<template>
  <AsrsPanel class="item-master-list-panel" eyebrow="Item List" title="상품 목록">
    <template #actions>
      <span class="item-master-list__count-badge">{{ rows.length }}건</span>
    </template>

    <div class="asrs-ui-table-wrap item-master-list-panel__table-wrap">
      <table class="asrs-ui-table">
        <thead>
        <tr>
          <th>Item Code</th>
          <th>Item Name</th>
          <th>Category</th>
          <th>Temp</th>
          <th>Handling</th>
          <th>Outbound</th>
          <th>Lot</th>
          <th>Active</th>
          <th>Updated At</th>
        </tr>
        </thead>

        <tbody>
        <tr v-if="loading">
          <td colspan="9" class="item-master-list__empty-cell">
            조회 중입니다...
          </td>
        </tr>

        <tr v-else-if="!rows.length">
          <td colspan="9" class="item-master-list__empty-cell">
            조회 결과가 없습니다.
          </td>
        </tr>

        <tr
          v-for="row in rows"
          :key="row.itemCode"
          :class="{ 'asrs-ui-table__row--selected': selectedItemCode === row.itemCode }"
          @click="$emit('select', row)"
        >
          <td class="asrs-ui-table__key">{{ row.itemCode }}</td>
          <td>{{ row.itemName || '-' }}</td>
          <td>{{ row.categoryCode || '-' }}</td>
          <td>{{ row.storageTempType || '-' }}</td>
          <td>{{ row.handlingUnitType || '-' }}</td>
          <td>{{ row.outboundUnitType || '-' }}</td>
          <td>{{ row.lotControlYn || '-' }}</td>
          <td>
            <span
              class="item-master-list__active-badge"
              :class="row.activeYn === 'Y'
                ? 'item-master-list__active-badge--on'
                : 'item-master-list__active-badge--off'"
            >
              {{ row.activeYn || '-' }}
            </span>
          </td>
          <td>{{ row.updatedAt || '-' }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </AsrsPanel>
</template>

<script setup lang="ts">
import AsrsPanel from '@/views/asrs/shared/components/ui/AsrsPanel.vue';
import type { ItemMasterRow } from '../types';

defineProps<{
  rows: ItemMasterRow[];
  loading?: boolean;
  selectedItemCode?: string;
}>();

defineEmits<{
  (e: 'select', row: ItemMasterRow): void;
}>();
</script>
