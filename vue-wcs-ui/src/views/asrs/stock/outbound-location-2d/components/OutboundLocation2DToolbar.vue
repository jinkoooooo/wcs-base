<template>
  <section class="outbound-location-2d-toolbar asrs-ui-toolbar">
    <div class="outbound-location-2d-toolbar__fields">
      <AsrsAreaCodeField
        v-model="filter.areaCode"
        :options="areaOptions"
        :disabled="loading.areas"
        placeholder="선택"
      />

      <AsrsFormField
        v-model="filter.aisleNo"
        label="Aisle"
        type="select"
        label-mode="left"
        :compact="true"
        :options="aisleSelectOptions"
        :disabled="loading.aisles || !filter.areaCode"
        placeholder="선택"
      />

      <AsrsFormField
        v-model="filter.sideCode"
        label="Side"
        type="select"
        label-mode="left"
        :compact="true"
        :options="sideSelectOptions"
        :disabled="loading.sides || !filter.aisleNo"
        placeholder="선택"
      />
    </div>

    <div class="outbound-location-2d-toolbar__actions">
      <AsrsActionButton variant="ghost" @click="$emit('zoom-out')">축소</AsrsActionButton>
      <AsrsActionButton variant="ghost" @click="$emit('zoom-in')">확대</AsrsActionButton>
      <AsrsActionButton variant="ghost" @click="$emit('reset-zoom')">배율 초기화</AsrsActionButton>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import AsrsAreaCodeField from '@/views/asrs/shared/components/form/AsrsAreaCodeField.vue';
import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import type { AreaCodeOption } from '@/views/asrs/shared/components/form/AsrsAreaCodeField.vue';
import type { OutboundLocation2DFilter } from '../types';

const props = defineProps<{
  filter: OutboundLocation2DFilter;
  areaOptions: AreaCodeOption[];
  aisleOptions: number[];
  sideOptions: string[];
  loading: {
    areas: boolean;
    aisles: boolean;
    sides: boolean;
    map: boolean;
  };
}>();

defineEmits<{
  (e: 'zoom-in'): void;
  (e: 'zoom-out'): void;
  (e: 'reset-zoom'): void;
}>();

const aisleSelectOptions = computed(() =>
  props.aisleOptions.map((no) => ({
    label: `${no}호기`,
    value: String(no),
  })),
);

const sideSelectOptions = computed(() =>
  props.sideOptions.map((side) => ({
    label: side,
    value: side,
  })),
);
</script>
