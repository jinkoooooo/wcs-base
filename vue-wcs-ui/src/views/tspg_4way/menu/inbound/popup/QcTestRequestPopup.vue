<template>
  <BasicModal
    @register="registerModal"
    title="시험 의뢰 검색 (금일)"
    :width="1000"
    :footer="null"
    :destroyOnClose="true"
    @cancel="handleClose"
  >
    <!-- 필터 -->
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
      <Select
        v-model:value="searchStatus"
        placeholder="상태"
        allowClear
        style="width: 140px"
        :options="statusOptions"
      />
      <Button type="primary" :loading="loading" @click="doSearch">조회</Button>
      <Button @click="handleClose">닫기</Button>
    </div>

    <Table
      :dataSource="filtered"
      :columns="columns"
      :pagination="{ pageSize: 10, size: 'small', showSizeChanger: false }"
      :loading="loading"
      size="small"
      :scroll="{ y: 380 }"
      :rowKey="(record: any) => record.id || record.test_request_no"
      :customRow="(record: any) => ({ onDblclick: () => handleSelect(record) })"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="record.status === 'COMPLETED' ? 'green' : 'orange'">
            {{ record.status === 'COMPLETED' ? '완료' : '미완료' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'fetched'">
          <a-tag :color="record.fetched ? 'blue' : 'default'">
            {{ record.fetched ? '인수' : '미인수' }}
          </a-tag>
        </template>
      </template>
    </Table>
    <div class="hint">행을 더블클릭하면 선택됩니다.</div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed, watch, onMounted } from 'vue';
  import { Input, Select, Button, Table } from 'ant-design-vue';
  import { BasicModal, useModal } from '/src/components/Modal';
  import { getCommonGetListApi } from '/src/api/common/api';
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
  const searchStatus = ref<string | undefined>(undefined);
  const items = ref<any[]>([]);

  const statusOptions = [
    { label: '미완료', value: 'PENDING' },
    { label: '완료', value: 'COMPLETED' },
  ];

  const columns = [
    { title: '입고일자', dataIndex: 'inbound_date', width: 110 },
    { title: 'SKU', dataIndex: 'item_code', width: 140 },
    { title: 'LOT No.', dataIndex: 'lot_no', width: 140 },
    { title: '시험의뢰번호', dataIndex: 'test_request_no', width: 200 },
    { title: '시험번호', dataIndex: 'test_no', width: 140 },
    { title: '상태', dataIndex: 'status', width: 90 },
    { title: '상위인수', dataIndex: 'fetched', width: 90 },
  ];

  const filtered = computed(() => {
    const item_code = searchSku.value?.trim().toLowerCase();
    const lot = searchLot.value?.trim().toLowerCase();
    const st = searchStatus.value;
    return items.value.filter((r) => {
      if (
        item_code &&
        !String(r.item_code || '')
          .toLowerCase()
          .includes(item_code)
      )
        return false;
      if (
        lot &&
        !String(r.lot_no || '')
          .toLowerCase()
          .includes(lot)
      )
        return false;
      if (st && r.status !== st) return false;
      return true;
    });
  });

  const [registerModal, { openModal, closeModal, getVisible }] = useModal();

  watch(getVisible, (visible, prev) => {
    if (visible) {
      searchSku.value = '';
      searchLot.value = '';
      searchStatus.value = undefined;
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
      const resp: any = await getCommonGetListApi('/wcs/qc-test/request/today', null);
      console.log(`resp : ${JSON.stringify(resp,null,2)}`);
      items.value = Array.isArray(resp) ? resp : resp?.items ?? [];
    } catch (e: any) {
      notification.error({
        message: '조회 오류',
        description: e?.message || '시험 의뢰 조회 중 오류가 발생했습니다.',
        duration: 2,
      });
      items.value = [];
    } finally {
      loading.value = false;
    }
  }

  function handleSelect(record: any) {
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
