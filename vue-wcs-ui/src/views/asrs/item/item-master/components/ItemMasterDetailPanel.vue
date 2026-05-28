<template>
  <div v-if="open" class="item-master-detail-modal">
    <div class="item-master-detail-modal__backdrop" @click="$emit('close')" />

    <div class="item-master-detail-modal__dialog">
      <div class="item-master-detail-modal__header">
        <div>
          <p class="item-master-detail-modal__eyebrow">Item Detail</p>
          <h3 class="item-master-detail-modal__title">
            {{ mode === 'create' ? '상품 신규 등록' : '상품 상세 / 수정' }}
          </h3>
        </div>

        <div class="item-master-detail-modal__header-actions">
          <AsrsActionButton variant="ghost" @click="$emit('close')">
            닫기
          </AsrsActionButton>
        </div>
      </div>

      <div class="item-master-detail-modal__body">
        <div class="item-master-detail__scroll">
          <!-- 기본 정보 -->
          <section class="item-master-detail__section">
            <button
              type="button"
              class="item-master-detail__section-toggle"
              @click="toggleSection('basic')"
            >
              <span>기본 정보</span>
              <span>{{ openedSections.basic ? '−' : '+' }}</span>
            </button>

            <div v-if="openedSections.basic" class="item-master-detail__section-content">
              <div class="item-master-detail__grid">
                <AsrsFormField
                  v-model="form.itemCode"
                  label="Item Code"
                  type="text"
                  label-mode="top"
                  :required="true"
                  placeholder="예: AC-ITEM-100"
                  :disabled="mode === 'edit' || loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.itemName"
                  label="Item Name"
                  type="text"
                  label-mode="top"
                  :required="true"
                  placeholder="상품명"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.categoryCode"
                  label="Category"
                  type="select"
                  label-mode="top"
                  :required="true"
                  :options="categorySelectOptions"
                  placeholder="선택"
                  :disabled="loading.categories || loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.operationProfileId"
                  label="Operation Profile"
                  type="text"
                  label-mode="top"
                  placeholder="예: AC-OP-001"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.industryType"
                  label="Industry Type"
                  type="text"
                  label-mode="top"
                  :required="true"
                  placeholder="예: GENERAL"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.baseUom"
                  label="Base UOM"
                  type="text"
                  label-mode="top"
                  :required="true"
                  placeholder="예: EA"
                  :disabled="loading.detail || loading.save"
                />
              </div>
            </div>
          </section>

          <!-- 취급 / 보관 -->
          <section class="item-master-detail__section">
            <button
              type="button"
              class="item-master-detail__section-toggle"
              @click="toggleSection('handling')"
            >
              <span>취급 / 보관 정보</span>
              <span>{{ openedSections.handling ? '−' : '+' }}</span>
            </button>

            <div v-if="openedSections.handling" class="item-master-detail__section-content">
              <div class="item-master-detail__grid">
                <AsrsFormField
                  v-model="form.handlingUnitType"
                  label="Handling Unit"
                  type="text"
                  label-mode="top"
                  :required="true"
                  placeholder="예: PALLET"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.outboundUnitType"
                  label="Outbound Unit"
                  type="text"
                  label-mode="top"
                  :required="true"
                  placeholder="예: FULL"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.storageTempType"
                  label="Storage Temp"
                  type="select"
                  label-mode="top"
                  :required="true"
                  :options="storageTempOptions"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.allocationRuleCode"
                  label="Allocation Rule"
                  type="text"
                  label-mode="top"
                  :required="true"
                  placeholder="예: FIXED"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.rotationProfileCode"
                  label="Rotation Profile"
                  type="text"
                  label-mode="top"
                  :required="true"
                  placeholder="예: SLOW"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.storageGradeSeed"
                  label="Storage Grade Seed"
                  type="text"
                  label-mode="top"
                  :required="true"
                  placeholder="예: C"
                  :disabled="loading.detail || loading.save"
                />
              </div>
            </div>
          </section>

          <!-- 제어 속성 -->
          <section class="item-master-detail__section">
            <button
              type="button"
              class="item-master-detail__section-toggle"
              @click="toggleSection('flags')"
            >
              <span>제어 / 운영 속성</span>
              <span>{{ openedSections.flags ? '−' : '+' }}</span>
            </button>

            <div v-if="openedSections.flags" class="item-master-detail__section-content">
              <div class="item-master-detail__grid item-master-detail__grid--yn">
                <AsrsFormField
                  v-for="field in ynFields"
                  :key="field.key"
                  v-model="(form as any)[field.key]"
                  :label="field.label"
                  type="select"
                  label-mode="top"
                  :options="ynOptions"
                  :disabled="loading.detail || loading.save"
                />
              </div>
            </div>
          </section>

          <!-- 치수 / 중량 -->
          <section class="item-master-detail__section">
            <button
              type="button"
              class="item-master-detail__section-toggle"
              @click="toggleSection('dimensions')"
            >
              <span>치수 / 중량 정보</span>
              <span>{{ openedSections.dimensions ? '−' : '+' }}</span>
            </button>

            <div v-if="openedSections.dimensions" class="item-master-detail__section-content">
              <div class="item-master-detail__grid">
                <AsrsFormField
                  v-model="form.lengthMm"
                  label="Length (mm)"
                  type="number"
                  label-mode="top"
                  :required="true"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.widthMm"
                  label="Width (mm)"
                  type="number"
                  label-mode="top"
                  :required="true"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.heightMm"
                  label="Height (mm)"
                  type="number"
                  label-mode="top"
                  :required="true"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  v-model="form.weightG"
                  label="Weight (g)"
                  type="number"
                  label-mode="top"
                  :required="true"
                  :disabled="loading.detail || loading.save"
                />

                <AsrsFormField
                  :model-value="computedVolumeMm3"
                  label="Volume (mm³)"
                  type="readonly"
                  label-mode="top"
                />

                <AsrsFormField
                  v-model="form.activeYn"
                  label="Active"
                  type="select"
                  label-mode="top"
                  :options="ynOptions"
                  :disabled="loading.detail || loading.save"
                />
              </div>
            </div>
          </section>

          <!-- 확장 속성 -->
          <section class="item-master-detail__section">
            <button
              type="button"
              class="item-master-detail__section-toggle"
              @click="toggleSection('ext')"
            >
              <span>확장 속성</span>
              <span>{{ openedSections.ext ? '−' : '+' }}</span>
            </button>

            <div v-if="openedSections.ext" class="item-master-detail__section-content">
              <div class="item-master-detail__grid item-master-detail__grid--single">
                <AsrsFormField
                  v-model="form.extAttr"
                  label="Ext Attr"
                  type="textarea"
                  label-mode="top"
                  placeholder="JSON / 메모 / 기타 확장 속성"
                  :disabled="loading.detail || loading.save"
                />
              </div>
            </div>
          </section>

          <!-- 메타 -->
          <section v-if="mode === 'edit'" class="item-master-detail__section">
            <button
              type="button"
              class="item-master-detail__section-toggle"
              @click="toggleSection('meta')"
            >
              <span>이력 정보</span>
              <span>{{ openedSections.meta ? '−' : '+' }}</span>
            </button>

            <div v-if="openedSections.meta" class="item-master-detail__section-content">
              <div class="item-master-detail__meta-grid">
                <div class="item-master-detail__meta-item">
                  <span class="item-master-detail__meta-label">Created At</span>
                  <strong>{{ form.createdAt || '-' }}</strong>
                </div>

                <div class="item-master-detail__meta-item">
                  <span class="item-master-detail__meta-label">Updated At</span>
                  <strong>{{ form.updatedAt || '-' }}</strong>
                </div>
              </div>
            </div>
          </section>
        </div>
      </div>

      <div class="item-master-detail-modal__footer">
        <div class="item-master-detail-modal__footer-left">
          <AsrsActionButton variant="ghost" @click="$emit('close')">
            닫기
          </AsrsActionButton>
        </div>

        <div class="item-master-detail-modal__footer-right">
          <AsrsActionButton
            v-if="mode === 'edit'"
            variant="secondary"
            :disabled="loading.save || loading.delete"
            @click="$emit('toggle-active')"
          >
            {{ form.activeYn === 'Y' ? '미사용 전환' : '사용 전환' }}
          </AsrsActionButton>

          <AsrsActionButton
            v-if="mode === 'edit'"
            variant="ghost"
            :disabled="loading.save"
            :loading="loading.delete"
            loading-text="삭제 중..."
            @click="$emit('remove')"
          >
            삭제
          </AsrsActionButton>

          <AsrsActionButton
            variant="primary"
            :loading="loading.save"
            loading-text="저장 중..."
            @click="$emit('save')"
          >
            {{ mode === 'create' ? '신규 저장' : '수정 저장' }}
          </AsrsActionButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 상품 상세 편집 팝업.
 *
 * 정책:
 * - 기존 우측 고정 패널을 모달로 전환
 * - 신규 버튼 클릭 시 신규 모달
 * - 목록 행 클릭 시 수정 모달
 * - 그룹별 접기/펼치기 지원
 */
import { computed, reactive } from 'vue';
import AsrsFormField from '@/views/asrs/shared/components/form/AsrsFormField.vue';
import AsrsActionButton from '@/views/asrs/shared/components/ui/AsrsActionButton.vue';
import type {
  ItemCategoryOption,
  ItemMasterDetailForm,
  ItemMasterMode,
} from '../types';

const props = defineProps<{
  open: boolean;
  form: ItemMasterDetailForm;
  mode: ItemMasterMode;
  categoryOptions: ItemCategoryOption[];
  loading: {
    search: boolean;
    detail: boolean;
    save: boolean;
    delete: boolean;
    categories: boolean;
    bulkSave: boolean;
  };
  computedVolumeMm3: number;
}>();

defineEmits<{
  (e: 'close'): void;
  (e: 'save'): void;
  (e: 'remove'): void;
  (e: 'toggle-active'): void;
}>();

/** 접기/펼치기 상태 */
const openedSections = reactive({
  basic: true,
  handling: true,
  flags: false,
  dimensions: true,
  ext: false,
  meta: false,
});

/** 섹션 토글 */
function toggleSection(key: keyof typeof openedSections) {
  openedSections[key] = !openedSections[key];
}

/** Y/N 제어 필드 목록 */
const ynFields = [
  { key: 'lotControlYn', label: 'Lot Control' },
  { key: 'expiryControlYn', label: 'Expiry Control' },
  { key: 'serialControlYn', label: 'Serial Control' },
  { key: 'partialPickYn', label: 'Partial Pick' },
  { key: 'mixedLoadYn', label: 'Mixed Load' },
  { key: 'fragileYn', label: 'Fragile' },
  { key: 'heavyYn', label: 'Heavy' },
  { key: 'quarantineRequiredYn', label: 'Quarantine Required' },
];

const ynOptions = [
  { value: 'Y', label: 'Y' },
  { value: 'N', label: 'N' },
];

const storageTempOptions = [
  { value: 'AMBIENT', label: 'AMBIENT' },
  { value: 'CHILLED', label: 'CHILLED' },
  { value: 'FROZEN', label: 'FROZEN' },
];

const categorySelectOptions = computed(() =>
  props.categoryOptions.map((option) => ({
    value: option.categoryCode,
    label: option.categoryName
      ? `${option.categoryCode} - ${option.categoryName}`
      : option.categoryCode,
  })),
);
</script>
