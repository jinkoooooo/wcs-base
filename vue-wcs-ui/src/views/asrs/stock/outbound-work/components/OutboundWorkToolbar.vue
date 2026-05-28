<template>
  <section class="outbound-toolbar asrs-ui-toolbar">
    <div class="outbound-toolbar__tabs">
      <button
        type="button"
        class="outbound-toolbar__tab"
        :class="{ 'outbound-toolbar__tab--active': activeTab === 'auto' }"
        @click="$emit('change-tab', 'auto')"
      >
        자동할당 출고
      </button>

      <button
        type="button"
        class="outbound-toolbar__tab"
        :class="{ 'outbound-toolbar__tab--active': activeTab === 'location' }"
        @click="$emit('change-tab', 'location')"
      >
        로케이션 지정 출고
      </button>

      <button
        type="button"
        class="outbound-toolbar__tab"
        :class="{ 'outbound-toolbar__tab--active': activeTab === 'item' }"
        @click="$emit('change-tab', 'item')"
      >
        품목 기준 조회/출고
      </button>

      <button
        type="button"
        class="outbound-toolbar__tab"
        :class="{ 'outbound-toolbar__tab--active': activeTab === 'history' }"
        @click="$emit('change-tab', 'history')"
      >
        단건 이력 조회
      </button>
    </div>

    <div class="outbound-toolbar__form outbound-toolbar__form--compact">
      <AsrsAreaCodeField
        v-model="form.areaCode"
        :options="areaOptions"
        :disabled="loading.areas"
        placeholder="선택"
        @enter="emitSearch"
      />

      <AsrsFormField
        v-if="activeTab === 'location'"
        v-model="form.locationCode"
        label="Location"
        label-mode="left"
        :compact="true"
        placeholder="예: ASRS1-A01-L-B004-L02-D01"
        @enter="emitSearch"
      />

      <AsrsItemCodePickerField
        v-if="activeTab === 'item' || activeTab === 'auto'"
        v-model="form.itemCode"
        label="Item"
        label-mode="left"
        :compact="true"
        placeholder="예: AC-ITEM-001"
        @enter="emitSearch"
      />

      <AsrsFormField
        v-if="activeTab === 'history'"
        v-model="form.stockUnitNo"
        label="Stock Unit"
        label-mode="left"
        :compact="true"
        placeholder="예: AC-SU-001"
        @enter="emitSearch"
      />

      <AsrsFormField
        v-if="activeTab === 'auto'"
        v-model="form.outQty"
        label="Out Qty"
        type="number"
        label-mode="left"
        :compact="true"
        placeholder="예: 10"
        @enter="emitSearch"
      />

      <AsrsFormField
        v-model="form.refDocType"
        label="Ref Type"
        label-mode="left"
        :compact="true"
        placeholder="ORDER"
      />

      <AsrsFormField
        v-model="form.refDocNo"
        label="Ref No"
        label-mode="left"
        :compact="true"
        placeholder="문서번호"
      />

      <AsrsFormField
        v-model="form.refLineNo"
        label="Ref Line"
        label-mode="left"
        :compact="true"
        placeholder="라인번호"
      />

      <AsrsFormField
        v-model="form.reasonCode"
        label="Reason"
        label-mode="left"
        :compact="true"
        placeholder="사유코드"
      />

      <AsrsFormField
        v-model="form.remark"
        label="Remark"
        label-mode="left"
        :compact="true"
        placeholder="비고"
      />
    </div>

    <div class="outbound-toolbar__bottom">
      <div class="outbound-toolbar__feedback">
        <AsrsFeedback :type="feedbackType" :message="feedbackMessage" />
      </div>

      <div class="outbound-toolbar__actions">
        <AsrsActionButton
          variant="secondary"
          :loading="activeTab === 'auto' ? loading.autoSearch : loading.search"
          :loading-text="activeTab === 'auto' ? '후보 조회 중...' : '조회 중...'"
          @click="emitSearch"
        >
          {{ activeTab === 'auto' ? '후보 조회' : '조회' }}
        </AsrsActionButton>

        <AsrsActionButton variant="ghost" @click="$emit('reset')">
          초기화
        </AsrsActionButton>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import AsrsFeedback from '@/views/asrs/shared/components/ui/AsrsFeedback.vue';
import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
import AsrsAreaCodeField from '@/views/asrs/shared/components/form/AsrsAreaCodeField.vue';
import type { AreaCodeOption } from '@/views/asrs/shared/components/form/AsrsAreaCodeField.vue';
import type { OutboundWorkForm, OutboundWorkTab } from '../types';
import AsrsItemCodePickerField
  from "@/views/asrs/shared/components/item/AsrsItemCodePickerField.vue";

const props = defineProps<{
  activeTab: OutboundWorkTab;
  form: OutboundWorkForm;
  areaOptions: AreaCodeOption[];
  loading: {
    areas: boolean;
    search: boolean;
    detail: boolean;
    autoSearch: boolean;
    allocate: boolean;
    partialOut: boolean;
    fullOut: boolean;
    release: boolean;
  };
  feedbackType?: 'info' | 'success' | 'warning' | 'error';
  feedbackMessage?: string;
}>();

const emit = defineEmits<{
  (e: 'change-tab', tab: OutboundWorkTab): void;
  (e: 'search'): void;
  (e: 'auto-search'): void;
  (e: 'reset'): void;
}>();

function emitSearch() {
  if (props.activeTab === 'auto') {
    emit('auto-search');
    return;
  }

  emit('search');
}
</script>
