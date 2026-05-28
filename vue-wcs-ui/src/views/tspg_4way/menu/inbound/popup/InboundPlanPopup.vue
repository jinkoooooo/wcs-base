<template>
  <BasicModal
    @register="registerModal"
    title="입고 예정 검색"
    :width="1200"
    :footer="null"
    :destroyOnClose="true"
    @cancel="handleClose"
  >
    <div class="search-bar">
      <Input
        v-model:value="searchSku"
        placeholder="SKU"
        style="width: 160px"
        allowClear
        @press-enter="doSearch"
      />
      <Input
        v-model:value="searchLot"
        placeholder="LOT No."
        style="width: 160px"
        allowClear
        @press-enter="doSearch"
      />
      <Checkbox v-model:checked="onlyRemaining">잔여수량 있는 예정만</Checkbox>
      <Button type="primary" :loading="loading" @click="doSearch">조회</Button>
      <Button @click="handleClose">닫기</Button>
    </div>

    <Table
      :dataSource="filtered"
      :columns="columns"
      :pagination="{ pageSize: 10, size: 'small', showSizeChanger: false }"
      :loading="loading"
      size="small"
      :scroll="{ x: 1300, y: 380 }"
      :rowKey="(record: any) => record.id"
      :customRow="(record: any) => ({ onDblclick: () => handleSelect(record) })"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'test_required'">
          <a-tag :color="toBool(record.test_required) ? 'orange' : 'default'">
            {{ toBool(record.test_required) ? '시험' : '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'nia_required'">
          <a-tag :color="toBool(record.nia_required) ? 'blue' : 'default'">
            {{ toBool(record.nia_required) ? '국검' : '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag v-if="record.status" :color="getStatusColor(record.status)">
            {{ record.status }}
          </a-tag>
          <span v-else>-</span>
        </template>
      </template>
    </Table>
    <div class="hint">잔여수량이 있는 행을 더블클릭하면 선택됩니다.</div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed, watch, onMounted } from 'vue';
  import { Input, Button, Table, Checkbox } from 'ant-design-vue';
  import { BasicModal, useModal } from '/src/components/Modal';
  import { getSearchList } from '/@/api/common/api';
  import { useMessage } from '/src/hooks/web/useMessage';

  const emit = defineEmits<{
    (e: 'ready', api: { openModal: () => void; closeModal: () => void }): void;
    (e: 'select', record: any): void;
    (e: 'close'): void;
  }>();

  const { notification } = useMessage();

  const loading = ref(false);
  const searchSku = ref('');
  const searchLot = ref('');
  const onlyRemaining = ref(true);
  const items = ref<any[]>([]);

  // 그리드 boolean 은 문자열로 들어올 수 있어 명시 변환
  function toBool(v: any): boolean {
    if (v === true || v === 1) return true;
    if (typeof v === 'string') return v.trim().toLowerCase() === 'true';
    return false;
  }

  // QC 상태별 태그 색상 지정
  function getStatusColor(status: string): string {
    switch (status?.toUpperCase()) {
      case 'DRAFT':
        return 'default';
      case 'PENDING':
        return 'processing';
      case 'COMPLETED':
        return 'success';
      default:
        return 'default';
    }
  }

  const columns = [
    { title: '예정일', dataIndex: 'plan_date', width: 110 },
    { title: 'SKU', dataIndex: 'item_code', width: 130 },
    { title: '품목명', dataIndex: 'item_name', width: 170 },
    { title: 'LOT No.', dataIndex: 'lot_no', width: 120 },
    { title: '화주', dataIndex: 'item_owner', width: 90 },
    { title: '예정수량', dataIndex: 'planned_qty', width: 90, align: 'right' },
    { title: '주문수량', dataIndex: 'ordered_qty', width: 90, align: 'right' },
    { title: '잔여수량', dataIndex: 'remaining_qty', width: 90, align: 'right' },
    { title: '시험', dataIndex: 'test_required', width: 70, align: 'center' },
    { title: '국검', dataIndex: 'nia_required', width: 70, align: 'center' },
    // 새로 추가된 QC 관련 컬럼
    { title: 'QC상태', dataIndex: 'status', width: 100, align: 'center' },
    { title: 'QC의뢰번호', dataIndex: 'test_request_no', width: 140 },
    { title: 'QC시험번호', dataIndex: 'test_no', width: 140 },
  ];

  const filtered = computed(() => {
    const sku = searchSku.value?.trim().toLowerCase();
    const lot = searchLot.value?.trim().toLowerCase();
    return items.value.filter((r) => {
      if (
        sku &&
        !String(r.item_code || '')
          .toLowerCase()
          .includes(sku)
      )
        return false;
      if (
        lot &&
        !String(r.lot_no || '')
          .toLowerCase()
          .includes(lot)
      )
        return false;
      if (onlyRemaining.value && Number(r.remaining_qty) <= 0) return false;
      return true;
    });
  });

  const [registerModal, { openModal, closeModal, getVisible }] = useModal();

  watch(getVisible, (visible, prev) => {
    if (visible) {
      searchSku.value = '';
      searchLot.value = '';
      doSearch();
    } else if (prev) {
      emit('close');
    }
  });

  onMounted(() => {
    emit('ready', { openModal, closeModal });
  });

  async function doSearch() {
    loading.value = true;
    try {
      const resp: any = await getSearchList('/wcs/inbound/plan', {
        query: JSON.stringify([]),
        page: 1,
        limit: 200,
      });
      items.value = Array.isArray(resp) ? resp : resp?.items ?? [];
    } catch (e: any) {
      notification.error({
        message: '조회 오류',
        description: e?.message || '입고 예정 조회 중 오류가 발생했습니다.',
        duration: 2,
      });
      items.value = [];
    } finally {
      loading.value = false;
    }
  }

  function handleSelect(record: any) {
    // 잔여수량 없는 예정은 입고 주문 불가
    if (Number(record.remaining_qty) <= 0) {
      notification.warning({
        message: '선택 불가',
        description: '잔여 예정 수량이 없는 입고 예정입니다.',
      });
      return;
    }
    emit('select', record);
    closeModal();
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
