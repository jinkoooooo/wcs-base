<template>
  <!--
    부모 영역을 100% 높이로 채우고 flex 비율로 상/하단을 분할.
    .vben-page-wrapper-content { height:100% } 강제 주입으로 PageWrapper
    내부 높이 체인을 복원하여 TUI Grid fitToParent 가 정상 동작하도록 한다.

    ★ 이 컴포넌트 하나로 레이아웃이 완결된다 — 부모에서 추가 CSS 불필요.
      height: calc(100vh - offset) 으로 viewport 기준 높이를 직접 확보.
  -->
  <div class="vertical-dual-grid" :style="rootStyle">
    <div class="vdg-pane" :style="{ flex: props.masterFlex }">
      <CommonPage
        ref="masterRef"
        v-bind="masterBindProps"
        :fetchHandler="masterFetchHandler"
        @gridClicked="onMasterClicked"
        @gridChecked="(e: any) => emit('masterChecked', e)"
        @gridUnChecked="(e: any) => emit('masterUnChecked', e)"
        @gridDbClicked="(e: any) => emit('masterDbClicked', e)"
        @gridFetched="onMasterFetched"
        @resourcePopupClick="(e: any) => emit('resourcePopupClick', e)"
      >
        <slot name="masterButtons"></slot>
      </CommonPage>
    </div>
    <div class="vdg-pane" :style="{ flex: props.detailFlex }">
      <CommonPage
        ref="detailRef"
        v-bind="detailBindProps"
        :fetchHandler="detailFetchProxy"
        @gridClicked="(e: any) => emit('detailClicked', e)"
        @gridChecked="(e: any) => emit('detailChecked', e)"
        @gridUnChecked="(e: any) => emit('detailUnChecked', e)"
        @gridDbClicked="(e: any) => emit('detailDbClicked', e)"
        @gridFetched="(e: any) => emit('detailFetched', e)"
        @resourcePopupClick="(e: any) => emit('detailResourcePopupClick', e)"
      >
        <slot name="detailButtons"></slot>
      </CommonPage>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import CommonPage from './CommonPage.vue';

const route = useRoute();

const props = defineProps({
  // 레이아웃 — 상단/하단 flex 비율 (예: 6:4 → 상단 60%, 하단 40%)
  masterFlex: { type: Number, default: 6 },
  detailFlex: { type: Number, default: 4 },
  masterKeyField: { type: String, default: 'id' },
  detailFetchFn: { type: Function },

  /**
   * ★ 상단 고정영역 offset (Vben 헤더 + 탭바 등)
   *  - height: calc(100vh - {topOffset}px) 로 반영
   *  - 기본값 88 (헤더 48 + 탭바 40)
   *  - 탭바 없는 환경: 48 / 탭바+브레드크럼 별도: 110 등으로 조정
   */
  topOffset: { type: Number, default: 88 },

  // Master
  masterLimit: { type: Number, default: 20 },
  masterShowPagination: { type: Boolean, default: true },
  masterShowSummary: { type: Boolean, default: false },
  masterSummaryColumns: { type: Array as () => string[], default: () => [] },
  masterFetchHandler: { type: Function },
  masterShowSearchForm: { type: Boolean, default: true },
  masterShowButtons: { type: Boolean, default: true },
  masterRowHeaders: { type: Array, default: () => ['rowNum', 'checkbox'] },
  masterMenuName: { type: String },
  masterMetaUrl: { type: String },
  masterMetas: { type: Object },
  masterActionColumns: { type: Array, default: () => [] },

  // Detail
  detailLimit: { type: Number, default: 100 },
  detailShowPagination: { type: Boolean, default: true },
  detailShowSummary: { type: Boolean, default: false },
  detailSummaryColumns: { type: Array as () => string[], default: () => [] },
  detailShowSearchForm: { type: Boolean, default: false },
  detailShowButtons: { type: Boolean, default: true },
  detailRowHeaders: { type: Array, default: () => ['rowNum', 'checkbox'] },
  detailMenuName: { type: String },
  detailMetaUrl: { type: String },
  detailMenuMetaProp: { type: [String, Number] },
  detailMetas: { type: Object },
  detailActionColumns: { type: Array, default: () => [] },
});

const emit = defineEmits([
  'masterClicked', 'masterChecked', 'masterUnChecked', 'masterDbClicked', 'masterFetched',
  'detailClicked', 'detailChecked', 'detailUnChecked', 'detailDbClicked', 'detailFetched',
  'resourcePopupClick', 'detailResourcePopupClick',
]);

const masterRef = ref<any>(null);
const detailRef = ref<any>(null);
const selectedMasterKey = ref<string | null>(null);

/**
 * 루트 엘리먼트 높이를 viewport 기반으로 직접 확정.
 * 부모에서 별도 height/flex 설정 없이 바로 사용 가능.
 */
const rootStyle = computed(() => ({
  height: `calc(100vh - ${props.topOffset}px)`,
}));

const masterBindProps = computed(() => ({
  limit: props.masterLimit,
  showPagination: props.masterShowPagination,
  showSummary: props.masterShowSummary,
  summaryColumns: props.masterSummaryColumns,
  showSearchForm: props.masterShowSearchForm,
  showButtons: props.masterShowButtons,
  rowHeaders: props.masterRowHeaders,
  menuName: props.masterMenuName,
  metaUrl: props.masterMetaUrl,
  metas: props.masterMetas,
  actionColumns: props.masterActionColumns,
}));

const detailBindProps = computed(() => {
  const metaUrl = props.detailMetaUrl || `/menu_details/${String(route.name)}/named_meta`;
  return {
    limit: props.detailLimit,
    showPagination: props.detailShowPagination,
    showSummary: props.detailShowSummary,
    summaryColumns: props.detailSummaryColumns,
    showSearchForm: props.detailShowSearchForm,
    showButtons: props.detailShowButtons,
    rowHeaders: props.detailRowHeaders,
    actionColumns: props.detailActionColumns,
    metas: props.detailMetas,
    menuName: props.detailMenuName,
    metaUrl: metaUrl,
    menuMetaProp: props.detailMenuMetaProp ?? 0,
  };
});

function onMasterClicked(row: any) {
  emit('masterClicked', row);
  if (!row) return;
  const key = row[props.masterKeyField];
  if (key && key !== selectedMasterKey.value) {
    selectedMasterKey.value = key;
    nextTick(() => { detailRef.value?.grid?.fetch?.(); });
  }
}

/**
 * ★ 상단(Master) 조회 완료 시 처리
 *  - 결과가 있으면: 첫 번째 행을 자동 선택 → 하단 조회
 *  - 결과가 없으면: selectedMasterKey 를 null 로 리셋 → 하단도 빈 상태
 *
 *  CommonPage.gridFetched 는 BasicGrid 가 fetch 완료 후 발생시키는 이벤트로,
 *  payload 에 TUI Grid instance 및 records 가 포함된다.
 *  방어적으로 records 우선, 없으면 grid instance 에서 getData() 로 조회.
 */
function onMasterFetched(e: any) {
  emit('masterFetched', e);

  // 1) 우선 이벤트 payload 에서 records 추출 시도
  let records: any[] =
    (Array.isArray(e?.records) && e.records) ||
    (Array.isArray(e?.data) && e.data) ||
    (Array.isArray(e) && e) ||
    [];

  // 2) payload 에 없으면 grid instance 에서 직접 가져옴 (fallback)
  if (records.length === 0) {
    try {
      const data = masterRef.value?.grid?.getData?.();
      if (Array.isArray(data)) records = data;
    } catch (_) { /* noop */ }
  }

  if (records.length > 0) {
    // ── 결과 있음: 첫 번째 행 자동 선택 → 하단 조회 ──────────────────
    const firstRow = records[0];
    const key = firstRow?.[props.masterKeyField];

    if (key !== undefined && key !== null && key !== '') {
      selectedMasterKey.value = String(key);
      nextTick(() => { detailRef.value?.grid?.fetch?.(); });
      return;
    }
  }

  // ── 결과 없음 (또는 key 추출 실패): 하단도 빈 상태로 ─────────────
  selectedMasterKey.value = null;
  nextTick(() => { detailRef.value?.grid?.fetch?.(); });
}

async function detailFetchProxy(page: number, limit: number, sorters: any[], searchProps: any[]) {
  if (!selectedMasterKey.value) return { total: 0, records: [] };
  if (props.detailFetchFn && typeof props.detailFetchFn === 'function') {
    return await props.detailFetchFn(selectedMasterKey.value, page, limit, sorters, searchProps);
  }
  return { total: 0, records: [] };
}

defineExpose({
  // Master
  master: masterRef,
  masterGrid: computed(() => masterRef.value?.grid),
  masterForm: computed(() => masterRef.value?.form),
  masterGetFormFields: computed(() => masterRef.value?.getFormFields),
  masterFormValidate: computed(() => masterRef.value?.formValidate),
  masterButtons: computed(() => masterRef.value?.buttons),
  masterGetMenuMetas: () => masterRef.value?.getMenuMetas?.(),
  masterDownExcel: () => masterRef.value?.downExcel?.(),

  // Detail
  detail: detailRef,
  detailGrid: computed(() => detailRef.value?.grid),
  detailForm: computed(() => detailRef.value?.form),
  detailGetFormFields: computed(() => detailRef.value?.getFormFields),
  detailFormValidate: computed(() => detailRef.value?.formValidate),
  detailButtons: computed(() => detailRef.value?.buttons),
  detailGetMenuMetas: () => detailRef.value?.getMenuMetas?.(),
  detailDownExcel: () => detailRef.value?.downExcel?.(),

  selectedMasterKey,
});
</script>

<style lang="less">
/*
 * ★ DualGrid 높이 체인 — 자기 완결형 (부모 CSS 불필요)
 *
 * 1. 루트 .vertical-dual-grid 는 viewport 기준 높이를 직접 확보 (inline style).
 *    부모 체인에 의존하지 않으므로 어떤 라우터/레이아웃 안에서도 동작.
 * 2. 각 .vdg-pane 은 flex 비율로만 분배, min-height:0 + overflow:hidden 필수.
 * 3. CommonPage 의 PageWrapper 체인:
 *    <PageWrapper h-full max-full>              ← max-height 제약 해제
 *      <vben-page-wrapper-content>               ← height:100% 강제
 *        <div inline-flex flex-col flex-1>       ← inline-flex → flex 강제 (★ 핵심)
 *          <form-container>
 *          <Grid wrapper flex-1>                  ← 남은 영역 전부 차지
 *
 * inline-flex 는 inline 레벨 박스라 height:100% 가 부모로부터 상속되지 않는다.
 * 이 때문에 내부 컨텐츠가 자연 높이로 펼쳐져 전체 페이지 스크롤이 발생한다.
 */
.vertical-dual-grid {
  /* height 는 :style 로 주입 (viewport 기반) */
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  > .vdg-pane {
    min-height: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  /* PageWrapper 내부 높이 체인 복원 */
  .vben-page-wrapper-content {
    height: 100% !important;
  }

  /* CommonPage.max-full 의 max-height:calc(100vh - 76px) 제약 해제
     pane 2개가 각각 이 값까지 늘어나면 전체 스크롤 발생 */
  .max-full {
    max-height: none !important;
  }

  /* PageWrapper 가 pane 내부에서 flex 로 동작하도록 */
  .vben-page-wrapper {
    height: 100% !important;
    max-height: 100% !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: column !important;
    flex: 1 !important;
    min-height: 0 !important;
  }

  /* ★ 핵심 수정: CommonPage 내부 inline-flex → flex 강제
     inline-flex 는 inline 레벨이라 height:100% 상속이 안 됨.
     이 때문에 내부 Grid 영역이 0 이 되거나 자연 높이로 터져나옴. */
  .inline-flex.flex-col {
    display: flex !important;
    width: 100% !important;
    height: 100% !important;
    min-height: 0 !important;
  }

  /* Grid 영역 (.flex.flex-col.flex-1) 이 남은 공간 모두 차지 */
  .flex.flex-col.flex-1 {
    min-height: 0 !important;
    overflow: hidden;
  }
}
</style>
