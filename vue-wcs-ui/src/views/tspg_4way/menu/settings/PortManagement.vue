<!--
  [wcs-ops Step 18.B] 포트 관리 페이지.
  - /rest/wcs/port 로 포트 목록 조회
  - 모드 전환 / 락 강제 해제 액션
  - 관리자 권한 가정 (상세 권한 가드는 기존 세션/스토어 패턴 도입 후 적용)
-->
<template>
  <div class="port-mgmt-page">
    <div class="toolbar">
      <a-input v-model:value="filter.eqGroupId" placeholder="eqGroupId" style="width: 160px" />
      <a-select
        v-model:value="filter.locType"
        placeholder="locType"
        :options="locTypeOptions"
        style="width: 180px"
        allowClear
      />
      <a-select
        v-model:value="filter.portMode"
        placeholder="portMode"
        :options="portModeOptions"
        style="width: 180px"
        allowClear
      />
      <a-select
        v-model:value="filter.locked"
        placeholder="락 상태"
        :options="lockedOptions"
        style="width: 140px"
        allowClear
      />
      <a-input v-model:value="filter.keyword" placeholder="loc_id LIKE" style="width: 160px" />
      <a-button v-if="can('show')" type="primary" @click="load">조회</a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="rows"
      :pagination="false"
      size="small"
      bordered
      row-key="loc_id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button
              v-if="can('update')"
              size="small"
              :disabled="record.loc_type !== 'IN_OUTBOUND_PORT'"
              @click="handleChangeMode(record)"
            >
              모드 전환
            </a-button>
            <a-button
              v-if="can('delete')"
              size="small"
              danger
              :disabled="!record.task_id"
              @click="handleForceUnlock(record)"
            >
              락 해제
            </a-button>
          </a-space>
        </template>
        <template v-else-if="column.key === 'task_id'">
          <span v-if="record.task_id" class="lock-badge">🔒 {{ record.task_id }}</span>
          <span v-else class="lock-none">—</span>
        </template>
        <template v-else-if="column.key === 'is_enabled'">
          <span v-if="record.is_enabled" style="color: #52c41a">사용</span>
          <span v-else style="color: #999">비활성</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';

  const MENU = 'PortManagement';
  const { can } = usePermissionLocal(MENU);

  const { notification, createConfirm } = useMessage();

  const columns = [
    { title: 'locGroup', dataIndex: 'loc_group', key: 'loc_group' },
    { title: 'locId', dataIndex: 'loc_id', key: 'loc_id' },
    { title: 'locType', dataIndex: 'loc_type', key: 'loc_type' },
    { title: 'portMode', dataIndex: 'port_mode', key: 'port_mode' },
    { title: '락(task_id)', dataIndex: 'task_id', key: 'task_id' },
    { title: 'active', dataIndex: 'active_task_count', key: 'active_task_count' },
    { title: '사용여부', dataIndex: 'is_enabled', key: 'is_enabled' },
    { title: '액션', key: 'action', width: 220 },
  ];

  const locTypeOptions = [
    { label: 'INBOUND_PORT', value: 'INBOUND_PORT' },
    { label: 'OUTBOUND_PORT', value: 'OUTBOUND_PORT' },
    { label: 'IN_OUTBOUND_PORT', value: 'IN_OUTBOUND_PORT' },
  ];
  const portModeOptions = [
    { label: 'IDLE', value: 'IDLE' },
    { label: 'INBOUND', value: 'INBOUND' },
    { label: 'OUTBOUND', value: 'OUTBOUND' },
    { label: 'OUTBOUND_PRIORITY', value: 'OUTBOUND_PRIORITY' },
  ];
  const lockedOptions = [
    { label: '전체', value: null },
    { label: '락만', value: true },
    { label: '락 없음', value: false },
  ];

  const filter = reactive({
    eqGroupId: '',
    locType: undefined as string | undefined,
    portMode: undefined as string | undefined,
    locked: null as boolean | null,
    keyword: '',
  });

  const rows = ref<any[]>([]);

  async function load() {
    try {
      const params = new URLSearchParams();
      if (filter.eqGroupId) params.set('eqGroupId', filter.eqGroupId);
      if (filter.locType) params.set('locType', filter.locType);
      if (filter.portMode) params.set('portMode', filter.portMode);
      if (filter.locked !== null && filter.locked !== undefined)
        params.set('locked', String(filter.locked));
      if (filter.keyword) params.set('keyword', filter.keyword);

      const res: any = await defHttp.get(
        { url: `/rest/wcs/port?${params.toString()}` },
        { isTransformResponse: false },
      );
      rows.value = Array.isArray(res) ? res : [];
    } catch (e: any) {
      notification.error({ message: '조회 오류', description: e?.message });
    }
  }

  async function handleChangeMode(record: any) {
    const mode = prompt(
      `[${record.loc_id}] 현재 ${
        record.port_mode ?? '-'
      } → 전환할 모드 입력 (IDLE/INBOUND/OUTBOUND/OUTBOUND_PRIORITY)`,
      record.port_mode ?? 'IDLE',
    );
    if (!mode) return;
    const reason = prompt('전환 사유를 입력하세요 (감사 로그)', '');
    if (reason == null) return;
    if (!reason.trim()) {
      notification.warning({ message: '사유는 필수입니다.' });
      return;
    }
    try {
      const resp: any = await defHttp.put(
        {
          url: `/wcs/port/${encodeURIComponent(record.loc_id)}/mode`,
          data: {
            eqGroupId: record.loc_group,
            portMode: mode.trim().toUpperCase(),
            operator: 'UI',
            reason: reason.trim(),
          },
        },
        { isTransformResponse: false },
      );
      if (resp?.errorCode) {
        notification.error({ message: '전환 거부', description: resp.errorDesc || resp.errorCode });
      } else {
        notification.success({
          message: `전환 완료: ${resp?.previousMode} → ${resp?.currentMode}`,
        });
        await load();
      }
    } catch (e: any) {
      notification.error({ message: '전환 오류', description: e?.message });
    }
  }

  async function handleForceUnlock(record: any) {
    const reason = prompt(
      `[${record.loc_id}] 락 강제 해제.\n\n⚠ 진행중 작업이 있으면 데이터 정합성 이슈 가능.\n사유를 입력하세요.`,
      '',
    );
    if (reason == null) return;
    if (!reason.trim()) {
      notification.warning({ message: '사유는 필수입니다.' });
      return;
    }
    createConfirm({
      iconType: 'warning',
      title: () => '락 강제 해제',
      content: () => `정말로 [${record.loc_id}] 락을 해제하시겠습니까?`,
      onOk: async () => {
        try {
          await defHttp.post(
            {
              url: `/rest/wcs/admin/port/${encodeURIComponent(record.loc_id)}/unlock`,
              data: {
                eqGroupId: record.loc_group,
                operator: 'UI',
                reason: reason.trim(),
              },
            },
            { isTransformResponse: false },
          );
          notification.success({ message: '락 해제 완료' });
          await load();
        } catch (e: any) {
          notification.error({ message: '해제 오류', description: e?.message });
        }
      },
    });
  }
</script>

<style scoped>
  .port-mgmt-page {
    padding: 12px;
    height: 100%;
  }
  .toolbar {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
    flex-wrap: wrap;
  }
  .lock-badge {
    color: #f5222d;
    font-weight: 600;
  }
  .lock-none {
    color: #999;
  }
</style>
