<template>
  <BasicModal
    @register="registerModal"
    title="재고 검색"
    :width="1100"
    :footer="null"
    :destroyOnClose="true"
    @cancel="handleClose"
  >
    <!-- 검색 영역 -->
    <div class="search-bar">
      <Input
        v-model:value="searchSku"
        placeholder="SKU (품목코드)"
        style="width: 180px"
        allowClear
        @press-enter="doSearch"
      />
      <Input
        v-model:value="searchLot"
        placeholder="LOT No."
        style="width: 180px"
        allowClear
        @press-enter="doSearch"
      />
      <Input
        v-model:value="searchOwner"
        placeholder="화주"
        style="width: 120px"
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
      :rowKey="(record: any) => record.stock_id || (record.stor_loc + '-' + record.item_code + '-' + record.lot_no)"
      :customRow="(record: any) => ({ onDblclick: () => handleSelect(record) })"
    />

    <!-- 합계 영역 — 조회된 재고의 총 수량 -->
    <div class="footer-bar">
      <span class="hint">행을 더블클릭하면 선택됩니다.</span>
      <span class="total">
        총 수량: <b>{{ totalQty.toLocaleString() }}</b>
        <span v-if="items.length" class="row-count">({{ items.length }}건)</span>
      </span>
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted, watch } from 'vue';
  import { Input, Button, Table } from 'ant-design-vue';
  import { BasicModal, useModal } from '/src/components/Modal';
  import { getSearchList } from '/src/api/common/api';
  import { useMessage } from '/src/hooks/web/useMessage';

  const props = defineProps({
    // 재고 조회 endpoint — 기본값: 개별 재고 현황 API
    resourceUrl: { type: String, default: '/wcs/inventory/stock-individual' },
    autoSearch: { type: Boolean, default: true },
  });

  const emit = defineEmits<{
    (e: 'ready', api: { openModal: () => void; closeModal: () => void }): void;
    (e: 'select', record: any): void;
    (e: 'close'): void;
  }>();

  const { notification } = useMessage();

  const loading = ref(false);
  const searchSku = ref('');
  const searchLot = ref('');
  const searchOwner = ref('');
  const items = ref<any[]>([]);

  const columns = [
    { title: '화주', dataIndex: 'item_owner', width: 90 },
    { title: 'SKU', dataIndex: 'item_code', width: 110 },
    { title: '품목명', dataIndex: 'item_name', width: 180 },
    { title: 'LOT No.', dataIndex: 'lot_no', width: 120 },
    { title: '단위', dataIndex: 'unit', width: 70 },
    { title: '수량', dataIndex: 'item_qty', width: 90, align: 'right' as const },
    { title: '보관 위치', dataIndex: 'stor_loc', width: 110 },
    { title: '입고일자', dataIndex: 'inbound_date', width: 110 },
    { title: '창고', dataIndex: 'eq_group_id', width: 100 },
  ];

  // 조회된 행들의 수량 합계 (number 변환 후 합산 — null/문자열 안전)
  const totalQty = computed(() => {
    return items.value.reduce((sum, r) => sum + (Number(r.item_qty) || 0), 0);
  });

  const [registerModal, { openModal, closeModal, getVisible }] = useModal();

  watch(getVisible, (visible, prev) => {
    if (visible) {
      searchSku.value = '';
      searchLot.value = '';
      searchOwner.value = '';
      if (props.autoSearch) doSearch();
    } else if (prev) {
      emit('close');
    }
  });

  onMounted(() => {
    emit('ready', { openModal, closeModal });
  });

  async function doSearch() {
    const filters: any[] = [];
    if (searchSku.value)
      filters.push({ name: 'item_code', operator: 'like', value: searchSku.value });
    if (searchLot.value)
      filters.push({ name: 'lot_no', operator: 'like', value: searchLot.value });
    if (searchOwner.value)
      filters.push({ name: 'item_owner', operator: 'like', value: searchOwner.value });

    loading.value = true;
    try {
      const resp: any = await getSearchList(props.resourceUrl, {
        query: JSON.stringify(filters),
        page: 1,
        limit: 200,
      });
      if (resp && Array.isArray(resp.items)) items.value = resp.items;
      else if (Array.isArray(resp)) items.value = resp;
      else items.value = [];
    } catch (e: any) {
      notification.error({
        message: '조회 오류',
        description: e?.message || '재고 조회 중 오류가 발생했습니다.',
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

  /** 선택된 재고 row → 외부로 내보낼 payload. */
  function buildSelectPayload(record: any): Record<string, any> {
    const EXCLUDE_KEYS = new Set([
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

    // item_owner → owner_code 별칭 (ItemSearchPopup 호환)
    if (payload.item_owner != null && payload.owner_code == null) {
      payload.owner_code = payload.item_owner;
    }
    // unit → uom 별칭
    if (payload.unit != null && payload.uom == null) {
      payload.uom = payload.unit;
    }
    // 재고에서 가져온 가용 수량 — 호출자가 참조용으로 쓸 수 있도록 별도 키로도 전달
    if (payload.item_qty != null && payload.available_qty == null) {
      payload.available_qty = payload.item_qty;
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
  .footer-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 8px;
    padding: 6px 4px 0;
    border-top: 1px solid #f0f0f0;
  }
  .hint {
    font-size: 12px;
    color: #888;
  }
  .total {
    font-size: 13px;
    color: #333;
  }
  .total b {
    color: #1677ff;
    font-size: 14px;
    margin: 0 2px;
  }
  .row-count {
    color: #888;
    font-size: 12px;
    margin-left: 4px;
  }
</style>
