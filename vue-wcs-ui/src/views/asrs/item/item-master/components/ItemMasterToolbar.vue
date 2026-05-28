<template>
  <section class="item-master-toolbar asrs-ui-toolbar">
    <!-- 검색 조건 영역 -->
    <div class="item-master-toolbar__search">
      <AsrsFormField
        v-model="searchForm.itemCode"
        label="Item"
        type="text"
        label-mode="left"
        :compact="true"
        placeholder="예: AC-ITEM-001"
        :disabled="loading.search || loading.save"
        @enter="$emit('search')"
      />

      <AsrsFormField
        v-model="searchForm.itemName"
        label="Name"
        type="text"
        label-mode="left"
        :compact="true"
        placeholder="예: DEMO 테스트 품목"
        :disabled="loading.search || loading.save"
        @enter="$emit('search')"
      />

      <AsrsFormField
        v-model="searchForm.categoryCode"
        label="Category"
        type="select"
        label-mode="left"
        :compact="true"
        :options="categorySelectOptions"
        placeholder="전체"
        :disabled="loading.categories || loading.search || loading.save"
      />

      <AsrsFormField
        v-model="searchForm.storageTempType"
        label="Temp"
        type="select"
        label-mode="left"
        :compact="true"
        :options="storageTempOptions"
        placeholder="전체"
        :disabled="loading.search || loading.save"
      />

      <AsrsFormField
        v-model="searchForm.activeYn"
        label="Active"
        type="select"
        label-mode="left"
        :compact="true"
        :options="activeOptions"
        placeholder="전체"
        :disabled="loading.search || loading.save"
      />
    </div>

    <!-- 액션 영역 -->
    <div class="item-master-toolbar__actions-row">
      <div class="item-master-toolbar__feedback-wrap">
        <AsrsFeedback
          class="item-master-toolbar__feedback"
          :type="feedbackType"
          :message="feedbackMessage"
        />
      </div>

      <div class="item-master-toolbar__actions">
        <AsrsActionButton
          variant="secondary"
          :loading="loading.search"
          loading-text="조회 중..."
          @click="$emit('search')"
        >
          조회
        </AsrsActionButton>

        <AsrsActionButton
          variant="ghost"
          :disabled="loading.save || loading.delete"
          @click="$emit('create')"
        >
          신규
        </AsrsActionButton>

        <AsrsActionButton
          variant="ghost"
          :disabled="loading.save || loading.delete"
          @click="$emit('open-bulk')"
        >
          엑셀 붙여넣기
        </AsrsActionButton>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * 상품마스터 상단 툴바.
 *
 * 역할:
 * - 검색조건 입력
 * - 상단 액션 버튼 렌더링
 * - feedback 표시
 *
 * 주의:
 * - 상세 저장/삭제/사용여부는 이제 모달 내부에서 수행
 */
import { computed } from 'vue';
import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import AsrsFeedback from '@/views/asrs/shared/components/ui/AsrsFeedback.vue';
import type { ItemCategoryOption, ItemMasterSearchForm } from '../types';

const props = defineProps<{
  searchForm: ItemMasterSearchForm;
  categoryOptions: ItemCategoryOption[];
  loading: {
    search: boolean;
    detail: boolean;
    save: boolean;
    delete: boolean;
    categories: boolean;
    bulkSave: boolean;
  };
  feedbackType?: 'info' | 'success' | 'warning' | 'error';
  feedbackMessage?: string;
}>();

defineEmits<{
  (e: 'search'): void;
  (e: 'create'): void;
  (e: 'open-bulk'): void;
}>();

const categorySelectOptions = computed(() =>
  props.categoryOptions.map((option) => ({
    value: option.categoryCode,
    label: option.categoryName
      ? `${option.categoryCode} - ${option.categoryName}`
      : option.categoryCode,
  })),
);

const storageTempOptions = [
  { value: 'AMBIENT', label: 'AMBIENT' },
  { value: 'CHILLED', label: 'CHILLED' },
  { value: 'FROZEN', label: 'FROZEN' },
];

const activeOptions = [
  { value: 'Y', label: 'Y' },
  { value: 'N', label: 'N' },
];
</script>
