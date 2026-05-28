<template>
  <section class="stock-overview-toolbar asrs-ui-toolbar">
    <div class="stock-overview-toolbar__top">
      <div class="stock-overview-toolbar__mode">
        <button
          v-for="mode in searchModes"
          :key="mode.value"
          type="button"
          class="stock-overview-toolbar__mode-button"
          :class="{ 'stock-overview-toolbar__mode-button--active': searchMode === mode.value }"
          @click="$emit('change-search-mode', mode.value)"
        >
          {{ mode.label }}
        </button>
      </div>

      <div class="stock-overview-toolbar__feedback-slot">
        <AsrsFeedback
          class="stock-overview-toolbar__feedback"
          :type="feedbackType"
          :message="feedbackMessage"
        />
      </div>
    </div>

    <div
      class="stock-overview-toolbar__fields"
      :class="{
        'stock-overview-toolbar__fields--single': visibleFieldCount === 1,
        'stock-overview-toolbar__fields--multi': visibleFieldCount >= 2,
      }"
    >
      <AsrsAreaCodeField
        v-if="searchMode !== 'stockUnit'"
        v-model="filters.areaCode"
        :options="areaOptions"
        placeholder="전체"
      />

      <AsrsFormField
        v-if="searchMode === 'stockUnit'"
        v-model="filters.stockUnitNo"
        label="Stock Unit"
        type="text"
        label-mode="left"
        :compact="true"
        placeholder="예: LPN-0001"
        @enter="$emit('run-search')"
      />

      <AsrsItemCodePickerField
        v-if="searchMode === 'item'"
        v-model="filters.itemCode"
        label="Item"
        label-mode="left"
        :compact="true"
        placeholder="예: AC-ITEM-100"
        @enter="$emit('run-search')"
      />

      <AsrsFormField
        v-if="searchMode === 'location'"
        v-model="filters.locationCode"
        label="Location"
        type="text"
        label-mode="left"
        :compact="true"
        placeholder="예: ASRS1-A01-L-B001-L01-D01"
        @enter="$emit('run-search')"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
import AsrsFeedback from '@/views/asrs/shared/components/ui/AsrsFeedback.vue';
import type {
  StockOverviewFilters,
  StockOverviewSearchMode,
  StockOverviewSearchModeOption,
} from '../types';
import AsrsAreaCodeField, {
  AreaCodeOption
} from "@/views/asrs/shared/components/form/AsrsAreaCodeField.vue";
import AsrsItemCodePickerField
  from "@/views/asrs/shared/components/item/AsrsItemCodePickerField.vue";

defineProps<{
  searchModes: StockOverviewSearchModeOption[];
  searchMode: StockOverviewSearchMode;
  filters: StockOverviewFilters;
  visibleFieldCount: number;
  areaOptions: AreaCodeOption[];
  feedbackType?: 'info' | 'success' | 'warning' | 'error';
  feedbackMessage?: string;
}>();

defineEmits<{
  (e: 'change-search-mode', mode: StockOverviewSearchMode): void;
  (e: 'run-search'): void;
}>();
</script>
