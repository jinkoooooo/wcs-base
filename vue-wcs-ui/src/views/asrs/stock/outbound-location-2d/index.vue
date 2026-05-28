<template>
  <AsrsPageShell headerMode="toolbarOnly" title="">
    <template #toolbar>
      <OutboundLocation2DToolbar
        :filter="filter"
        :area-options="areaOptions"
        :aisle-options="aisleOptions"
        :side-options="sideOptions"
        :loading="loading"
        @zoom-in="zoomIn"
        @zoom-out="zoomOut"
        @reset-zoom="resetZoom"
      />
    </template>

    <section class="outbound-location-2d-page">
      <div class="outbound-location-2d-page__left">
        <OutboundRackCanvas
          :rack-map="rackMap"
          :selected-cell="selectedCell"
          :view-state="viewState"
          @select-cell="selectCell"
          @zoom-in="zoomIn"
          @zoom-out="zoomOut"
          @set-pan="setPan"
        />
      </div>

      <div class="outbound-location-2d-page__right">
        <OutboundLocationDetailPanel
          :selected-cell="selectedCell"
          :selected-depth="selectedDepth"
          :selected-depth-no="selectedDepthNo"
          :execute-form="executeForm"
          :can-execute="canExecute"
          :loading-execute="loading.execute"
          @update:selected-depth-no="selectDepth"
          @execute="executeOutbound"
        />
      </div>
    </section>
  </AsrsPageShell>
</template>

<script setup lang="ts">
import '@/views/asrs/shared/styles/asrs-ui-shared.css';
import '@/views/asrs/shared/styles/asrs-ui-form.css';
import '@/views/asrs/shared/styles/asrs-ui-table.css';
import '@/views/asrs/shared/styles/asrs-ui-panel.css';
import './styles/outbound-location-2d.css';

import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
import { useOutboundLocation2D } from './composables/useOutboundLocation2D';
import OutboundLocation2DToolbar from './components/OutboundLocation2DToolbar.vue';
import OutboundRackCanvas from './components/OutboundRackCanvas.vue';
import OutboundLocationDetailPanel from './components/OutboundLocationDetailPanel.vue';

const {
  filter,
  executeForm,
  loading,
  areaOptions,
  aisleOptions,
  sideOptions,
  rackMap,
  selectedCell,
  selectedDepthNo,
  selectedDepth,
  canExecute,
  selectCell,
  selectDepth,
  viewState,
  zoomIn,
  zoomOut,
  resetZoom,
  setPan,
  executeOutbound,
} = useOutboundLocation2D();
</script>
