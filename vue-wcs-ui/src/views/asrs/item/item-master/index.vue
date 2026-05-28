<template>
  <div class="item-master-page-root">
    <AsrsPageShell headerMode="toolbarOnly" title="">
      <template #toolbar>
        <ItemMasterToolbar
          :search-form="searchForm"
          :category-options="categoryOptions"
          :loading="loading"
          :feedback-type="feedback.type"
          :feedback-message="feedback.message"
          @search="runSearch"
          @create="openCreateModal"
          @open-bulk="openBulkModal"
        />
      </template>

      <section class="item-master-page">
        <section class="item-master-content-grid">
          <div class="item-master-content-grid__left">
            <ItemMasterListTable
              :rows="rows"
              :loading="loading.search"
              :selected-item-code="selectedRow?.itemCode"
              @select="selectRow"
            />
          </div>
        </section>
      </section>

      <!-- 상세 등록/수정 모달 -->
      <ItemMasterDetailPanel
        :open="detailModalOpen"
        :form="detailForm"
        :mode="mode"
        :category-options="categoryOptions"
        :loading="loading"
        :computed-volume-mm3="computedVolumeMm3"
        @close="closeDetailModal"
        @save="saveItemMaster"
        @remove="removeItemMaster"
        @toggle-active="toggleActiveYn"
      />

      <!-- bulk 붙여넣기 모달 -->
      <ItemBulkPasteModal
        v-if="bulkModalOpen"
        :open="bulkModalOpen"
        :paste-text="bulkPasteText"
        :preview-rows="bulkPreviewRows"
        :errors="bulkErrors"
        :loading-bulk-save="loading.bulkSave"
        @update:pasteText="bulkPasteText = $event"
        @parse="parseBulkText"
        @save="saveBulkRows"
        @close="closeBulkModal"
      />
    </AsrsPageShell>
  </div>
</template>

<script setup lang="ts">
/**
 * item-master 페이지 entry.
 *
 * 변경 사항:
 * - 공용 AsrsPageShell 은 수정하지 않음
 * - 이 화면 전용 wrapper(item-master-page-root)에서만 overflow 제어
 * - 상품 목록은 카드 내부 스크롤 사용
 */
import { onMounted } from 'vue';

import '@/views/asrs/shared/styles/asrs-ui-shared.css';
import '@/views/asrs/shared/styles/asrs-ui-form.css';
import '@/views/asrs/shared/styles/asrs-ui-table.css';
import '@/views/asrs/shared/styles/asrs-ui-panel.css';
import './styles/item-master.css';

import AsrsPageShell from '@/views/asrs/shared/components/layout/AsrsPageShell.vue';
import { useItemMaster } from './composables/useItemMaster';
import ItemMasterToolbar from './components/ItemMasterToolbar.vue';
import ItemMasterListTable from './components/ItemMasterListTable.vue';
import ItemMasterDetailPanel from './components/ItemMasterDetailPanel.vue';
import ItemBulkPasteModal from './components/ItemBulkPasteModal.vue';

const {
  searchForm,
  rows,
  selectedRow,
  detailForm,
  mode,
  categoryOptions,
  detailModalOpen,
  bulkModalOpen,
  bulkPasteText,
  bulkPreviewRows,
  bulkErrors,
  loading,
  feedback,
  computedVolumeMm3,
  openCreateModal,
  closeDetailModal,
  runSearch,
  selectRow,
  saveItemMaster,
  removeItemMaster,
  toggleActiveYn,
  openBulkModal,
  closeBulkModal,
  parseBulkText,
  saveBulkRows,
  initialize,
} = useItemMaster();

onMounted(() => {
  initialize();
});
</script>
