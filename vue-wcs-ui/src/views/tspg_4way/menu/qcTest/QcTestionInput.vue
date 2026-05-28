<template>
  <div class="inspection-input-page">
    <div class="toolbar">
      <a-button v-if="can('show')" type="primary" :loading="loading" @click="loadList">새로고침</a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="rows"
      :pagination="{ pageSize: 50 }"
      :loading="loading"
      rowKey="host_order_key"
      size="small"
      :expanded-row-keys="expandedKeys"
      @expand="onExpand"
    >
      <template #expandedRowRender="{ record }">
        <div class="cycle-list">
          <h4>시험 사이클 (shuttle 시간순)</h4>
          <a-table
            :columns="shuttleColumns"
            :data-source="record.shuttles || []"
            :pagination="false"
            size="small"
            rowKey="orderKey"
          />
        </div>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actions'">
          <a-space>
            <a-button v-if="can('update')" size="small" type="primary" @click="applyResult(record, 'PASSED')"
              >적합</a-button
            >
            <a-button v-if="can('delete')" size="small" danger @click="applyResult(record, 'FAILED')">부적합</a-button>
            <a-button v-if="can('update')" size="small" @click="triggerReinbound(record)">재입고 지시</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted } from 'vue';
  import { getCommonGetListApi, getCommonPostApi } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';

  const MENU = 'QcTestionInput';
  const { can } = usePermissionLocal(MENU);

  const { notification, createConfirm } = useMessage();

  const loading = ref(false);
  const rows = ref<any[]>([]);
  const expandedKeys = ref<string[]>([]);

  const columns = [
    { title: 'Host Order', dataIndex: 'host_order_key', key: 'host_order_key' },
    { title: '구역', dataIndex: 'eq_group_id', key: 'eq_group_id' },
    { title: 'Barcode', dataIndex: 'barcode', key: 'barcode' },
    { title: '시험 상태', dataIndex: 'test_status', key: 'test_status' },
    { title: '의뢰 시각', dataIndex: 'test_requested_at', key: 'test_requested_at' },
    { title: '결과 시각', dataIndex: 'test_resulted_at', key: 'test_resulted_at' },
    { title: '액션', key: 'actions' },
  ];

  const shuttleColumns = [
    { title: 'Shuttle Order', dataIndex: 'orderKey', key: 'orderKey' },
    { title: 'Type', dataIndex: 'orderType', key: 'orderType' },
    { title: 'Status', dataIndex: 'orderStatus', key: 'orderStatus' },
    { title: 'Parent', dataIndex: 'parentOrderKey', key: 'parentOrderKey' },
    { title: 'From', dataIndex: 'fromLocCode', key: 'fromLocCode' },
    { title: 'To', dataIndex: 'toLocCode', key: 'toLocCode' },
    { title: 'Created', dataIndex: 'createdAt', key: 'createdAt' },
  ];

  async function loadList() {
    loading.value = true;
    try {
      const resp: any = await getCommonGetListApi('/wcs/qc-test/target', null);
      console.log(`resp: ${JSON.stringify(resp, null, 2)}`)
      const list: any[] = Array.isArray(resp) ? resp : resp?.items ?? [];
      rows.value = list;
    } catch (e: any) {
      notification.error({ message: '오류', description: e?.message || '조회 실패' });
    } finally {
      loading.value = false;
    }
  }

  function onExpand(expanded: boolean, record: any) {
    if (expanded) {
      expandedKeys.value = [...expandedKeys.value, record.host_order_key];
    } else {
      expandedKeys.value = expandedKeys.value.filter((k) => k !== record.host_order_key);
    }
  }

  async function applyResult(record: any, result: 'PASSED' | 'FAILED') {
    createConfirm({
      iconType: 'warning',
      title: () => '시험 결과 입력',
      content: () => `${record.host_order_key} 시험 결과를 [${result}] 로 등록하시겠습니까?`,
      onOk: async () => {
        try {
          await getCommonPostApi(`/wcs/qc-test/target/${record.host_order_key}/result`, { result });
          notification.success({ message: '결과 입력 완료', description: result });
          loadList();
        } catch (e: any) {
          notification.error({ message: '오류', description: e?.message || '결과 입력 실패' });
        }
      },
    });
  }

  async function triggerReinbound(record: any) {
    createConfirm({
      iconType: 'warning',
      title: () => '재입고 지시',
      content: () =>
        `${record.host_order_key} 사전 발행 INBOUND shuttle 을 SENDING 으로 전환하시겠습니까?`,
      onOk: async () => {
        try {
          const resp: any = await getCommonPostApi(
            `/wcs/qc-test/target/${record.host_order_key}/reinbound`,
            {},
          );
          if (resp?.success) {
            notification.success({ message: '재입고 지시 완료', description: resp.orderKey });
          } else {
            notification.warn({ message: '재입고 지시 실패', description: resp?.message });
          }
          loadList();
        } catch (e: any) {
          notification.error({ message: '오류', description: e?.message || '재입고 지시 실패' });
        }
      },
    });
  }

  onMounted(() => {
    loadList();
  });
</script>

<style scoped>
  .inspection-input-page {
    padding: 12px;
  }
  .toolbar {
    margin-bottom: 12px;
  }
  .cycle-list h4 {
    margin: 8px 0;
  }
</style>
