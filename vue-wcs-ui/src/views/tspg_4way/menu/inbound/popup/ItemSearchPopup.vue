<template>
  <BasicModal
    @register="registerModal"
    title="품목 검색"
    :width="900"
    :footer="null"
    :destroyOnClose="true"
    @cancel="handleClose"
  >
    <!-- 검색 영역 -->
    <div class="search-bar">
      <Input
        v-model:value="searchOwner"
        placeholder="화주"
        style="width: 120px"
        allowClear
        @press-enter="doSearch"
      />
      <Select
        v-model:value="searchCategory"
        placeholder="자재 분류"
        style="width: 130px"
        allowClear
        :options="categoryOptions"
        @change="doSearch"
      />
      <Input
        v-model:value="searchCode"
        placeholder="품목코드"
        style="width: 150px"
        allowClear
        @press-enter="doSearch"
      />
      <Input
        v-model:value="searchName"
        placeholder="품목명"
        style="width: 200px"
        allowClear
        @press-enter="doSearch"
      />
      <Button type="primary" :loading="loading" @click="doSearch">조회</Button>
      <Button @click="handleClose">닫기</Button>
    </div>

    <Table
      :dataSource="items"
      :columns="columns"
      :pagination="{ pageSize: 10, size: 'small', showSizeChanger: false }"
      :loading="loading"
      size="small"
      :scroll="{ y: 380 }"
      :rowKey="(record: any) => record.id || (record.item_owner + '-' + record.item_code)"
      :customRow="(record: any) => ({ onDblclick: () => handleSelect(record) })"
      @resizeColumn="handleResizeColumn"
    />
    <div class="hint">행을 더블클릭하면 선택됩니다.</div>
  </BasicModal>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, watch } from 'vue';
import { Input, Button, Table, Select } from 'ant-design-vue';
import { BasicModal, useModal } from '/src/components/Modal';
import { getSearchList, getCommonCodeByName } from '/@/api/common/api';
import { useMessage } from '/src/hooks/web/useMessage';

const props = defineProps({
  resourceUrl: { type: String, default: '/tbinventoryitemmaster' },
  autoSearch: { type: Boolean, default: true },
  // 자재 분류 공통코드 그룹명
  categoryCode: { type: String, default: 'ITEM_CATEGORY' },
});

const emit = defineEmits<{
  (e: 'ready', api: { openModal: () => void; closeModal: () => void }): void;
  (e: 'select', record: any): void;
  (e: 'close'): void;
}>();

const { notification } = useMessage();

// =====================================================
// 공통코드 — 다른 화면(gridMeta)과 동일하게 getCommonCodeByName 사용
//   응답: [{ text: 명칭, value: 코드 }, ...] (rank 순)
// =====================================================
// Select 옵션 형태 [{ label, value }]
const categoryOptions = ref<{ label: string; value: string }[]>([]);
// 코드 → 명칭 dictionary (테이블 표시 전용)
const categoryDict = ref<Record<string, string>>({});

async function loadCategoryCode() {
  try {
    const list = await getCommonCodeByName(props.categoryCode); // [{ text, value }]
    categoryOptions.value = list.map((o: any) => ({ label: o.text, value: o.value }));
    categoryDict.value = list.reduce((acc: Record<string, string>, o: any) => {
      acc[o.value] = o.text;
      return acc;
    }, {});
  } catch (e) {
    console.warn('[ItemSearchPopup] 공통코드 로드 실패:', props.categoryCode, e);
    categoryOptions.value = [];
    categoryDict.value = {};
  }
}

// 코드 → 명칭 (없으면 코드 그대로)
function categoryLabel(code: any): string {
  if (!code) return '';
  return categoryDict.value[code] ?? code;
}

const loading = ref(false);
const searchCode = ref('');
const searchName = ref('');
const searchOwner = ref(''); // 화주 검색 필드
const searchCategory = ref<string | undefined>(undefined); // 자재 분류 검색 필드
const items = ref<any[]>([]);

// 문자열 정렬 (null/undefined 안전)
const strSorter = (key: string) => (a: any, b: any) =>
  String(a?.[key] ?? '').localeCompare(String(b?.[key] ?? ''));

// 숫자 정렬 (null/undefined 안전)
const numSorter = (key: string) => (a: any, b: any) =>
  (Number(a?.[key]) || 0) - (Number(b?.[key]) || 0);

// ref 로 선언해야 리사이즈한 너비가 반영됨
const columns = ref<any[]>([
  {
    title: '화주',
    dataIndex: 'item_owner',
    width: 120,
    align: 'center',
    resizable: true,
    sorter: strSorter('item_owner'),
  },
  {
    title: '자재 분류',
    dataIndex: 'item_category',
    width: 120,
    align: 'center',
    resizable: true,
    sorter: strSorter('item_category'),
    // ★ 공통코드 명칭으로 표시 (원본 코드값은 보존)
    customRender: ({ text }: { text: any }) => categoryLabel(text),
  },
  {
    title: '품목코드',
    dataIndex: 'item_code',
    width: 140,
    align: 'center',
    resizable: true,
    sorter: strSorter('item_code'),
  },
  {
    title: '품목명',
    dataIndex: 'item_name',
    width: 220,
    align: 'center',
    resizable: true,
    sorter: strSorter('item_name'),
  },
  {
    title: '박스 내품 수량',
    dataIndex: 'box_qty',
    width: 130,
    align: 'center',
    resizable: true,
    sorter: numSorter('box_qty'),
  },
]);

// 컬럼 너비 드래그 리사이즈 핸들러
function handleResizeColumn(w: number, col: any) {
  col.width = w;
}

const [registerModal, { openModal, closeModal, getVisible }] = useModal();

watch(getVisible, (visible, prev) => {
  if (visible) {
    searchCode.value = '';
    searchName.value = '';
    searchOwner.value = '';
    searchCategory.value = undefined;
    if (props.autoSearch) doSearch();
  } else if (prev) {
    emit('close');
  }
});

onMounted(() => {
  loadCategoryCode(); // 공통코드 1회 로드
  emit('ready', { openModal, closeModal });
});

async function doSearch() {
  const filters: any[] = [];
  if (searchOwner.value)
    filters.push({ name: 'item_owner', operator: 'like', value: searchOwner.value });
  if (searchCategory.value)
    filters.push({ name: 'item_category', operator: 'eq', value: searchCategory.value });
  if (searchCode.value)
    filters.push({ name: 'item_code', operator: 'like', value: searchCode.value });
  if (searchName.value)
    filters.push({ name: 'item_name', operator: 'like', value: searchName.value });

  loading.value = true;
  try {
    const resp: any = await getSearchList(props.resourceUrl, {
      query: JSON.stringify(filters),
      page: 1,
      limit: 100,
    });
    console.log('[ItemSearchPopup] resp:', JSON.stringify(resp, null, 2));
    if (resp && Array.isArray(resp.items)) items.value = resp.items;
    else if (Array.isArray(resp)) items.value = resp;
    else items.value = [];
  } catch (e: any) {
    notification.error({
      message: '조회 오류',
      description: e?.message || '품목 조회 중 오류가 발생했습니다.',
      duration: 2,
    });
    items.value = [];
  } finally {
    loading.value = false;
  }
}

function handleSelect(record: any) {
  const payload = buildSelectPayload(record);
  emit('select', payload);
  closeModal();
}

/** 팝업 결과 record를 외부로 내보낼 payload 형태로 정제 */
function buildSelectPayload(record: any): Record<string, any> {
  // 제외할 메타/감사 필드
  const EXCLUDE_KEYS = new Set([
    'id',
    'cud_flag_',
    'extprops_',
    'domain_id',
    'creator_id',
    'updater_id',
    'creator',
    'updater',
    'created_at',
    'updated_at',
  ]);

  const payload: Record<string, any> = {};
  Object.keys(record || {}).forEach((key) => {
    if (EXCLUDE_KEYS.has(key)) return;
    payload[key] = record[key];
  });

  // ★ item_category 는 원본 코드값 그대로 유지 (명칭으로 치환하지 않음)

  // item_owner → owner_code 로도 복제
  if (payload.item_owner != null && payload.owner_code == null) {
    payload.owner_code = payload.item_owner;
  }

  return payload;
}

function handleClose() {
  closeModal();
}
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
  align-items: center;
}
.hint {
  margin-top: 8px;
  font-size: 12px;
  color: #888;
  text-align: right;
}
</style>
