<template>
  <div class="ops-mode-mgmt-page">
    <div class="toolbar">
      <a-input v-model:value="filterEqGroup" placeholder="eqGroupId" style="width: 200px" />
      <a-button v-if="can('show')" type="primary" @click="loadRows">조회</a-button>
      <a-button v-if="can('create')" @click="openEditor(null)">신규 등록</a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="rows"
      :pagination="false"
      size="small"
      bordered
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a v-if="can('update')" @click="openEditor(record)">편집</a>
        </template>
        <template v-else-if="column.key === 'isPutbackEnabled'">
          <span :class="flagCls(record.isPutbackEnabled)">{{ flagLabel(record.isPutbackEnabled) }}</span>
        </template>
        <template v-else-if="column.key === 'isDispatchLockEnabled'">
          <span :class="flagCls(record.isDispatchLockEnabled)">{{ flagLabel(record.isDispatchLockEnabled) }}</span>
        </template>
        <template v-else-if="column.key === 'isOperationModeEnabled'">
          <span :class="flagCls(record.isOperationModeEnabled)">{{ flagLabel(record.isOperationModeEnabled) }}</span>
        </template>
        <template v-else-if="column.key === 'isInspectionEnabled'">
          <span :class="flagCls(record.isInspectionEnabled)">{{ flagLabel(record.isInspectionEnabled) }}</span>
        </template>
      </template> </a-table>

    <a-modal
      v-model:open="editorOpen"
      :title="editing?.id ? `편집: ${editing.id}` : '신규 센터 등록'"
      @ok="saveEditor"
      :confirm-loading="saving"
    >
      <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 14 }">
        <a-form-item label="eqGroupId">
          <a-input
            v-model:value="editForm.eqGroupId"
            :disabled="!!editing?.id"
            placeholder="'GLOBAL' 또는 센터 ID"
          />
        </a-form-item>
        <a-form-item label="운영 모드">
          <a-select v-model:value="editForm.operationMode" :options="MODE_OPTIONS" />
        </a-form-item>
        <a-form-item v-for="f in FLAG_KEYS" :key="f.key" :label="f.label">
          <a-radio-group v-model:value="editForm[f.key]">
            <a-radio :value="true">ON</a-radio>
            <a-radio :value="false">OFF</a-radio>
            <a-radio :value="null">상속</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="사유" required>
          <a-input v-model:value="editForm.reason" placeholder="변경 사유 (감사 로그)" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { usePermissionLocal } from '/@/views/tspg_4way/common/usePermissionLocal';

const MENU = 'OpsModeManagement';
const { can } = usePermissionLocal(MENU);

const { notification } = useMessage();

const MODE_OPTIONS = [
  { label: 'NORMAL', value: 'NORMAL' },
  { label: '입고 우선', value: 'INBOUND_PRIORITY' },
  { label: '출고 우선', value: 'OUTBOUND_PRIORITY' },
  { label: '재배치', value: 'RELOCATION' },
  { label: '점검', value: 'MAINTENANCE' },
];

type FlagKey =
  | 'isOperationModeEnabled'
  | 'isPutbackEnabled'
  | 'isDispatchLockEnabled'
  | 'isInspectionEnabled';

const FLAG_KEYS: Array<{ key: FlagKey; label: string }> = [
  { key: 'isOperationModeEnabled', label: '운영 모드 게이팅' },
  { key: 'isPutbackEnabled',       label: 'PUTBACK 자동' },
  { key: 'isDispatchLockEnabled',  label: '배차 락' },
  { key: 'isInspectionEnabled',    label: '입고 검수' },
];

const columns = [
  { title: 'id', dataIndex: 'id', key: 'id' },
  { title: 'eqGroupId', dataIndex: 'eqGroupId', key: 'eqGroupId' },
  { title: '운영 모드', dataIndex: 'operationMode', key: 'operationMode' },
  { title: '모드 적용', dataIndex: 'isOperationModeEnabled', key: 'isOperationModeEnabled' },
  { title: 'PUTBACK', dataIndex: 'isPutbackEnabled', key: 'isPutbackEnabled' },
  { title: '배차 락', dataIndex: 'isDispatchLockEnabled', key: 'isDispatchLockEnabled' },
  { title: '검수', dataIndex: 'isInspectionEnabled', key: 'isInspectionEnabled' },
  { title: '변경자', dataIndex: 'changedBy', key: 'changedBy' },
  { title: '변경시각', dataIndex: 'changedAt', key: 'changedAt' },
  { title: '사유', dataIndex: 'reason', key: 'reason' },
  { title: '액션', key: 'action' },
];

const filterEqGroup = ref('');
const rows = ref<any[]>([]);

const editorOpen = ref(false);
const editing = ref<any | null>(null);
const saving = ref(false);

const editForm = reactive({
  eqGroupId: '',
  operationMode: 'NORMAL',
  isOperationModeEnabled: null as boolean | null,
  isPutbackEnabled:       null as boolean | null,
  isDispatchLockEnabled:  null as boolean | null,
  isInspectionEnabled:    null as boolean | null,
  reason: '',
});

function flagLabel(v: boolean | null | undefined) {
  if (v === true)  return 'ON';
  if (v === false) return 'OFF';
  return '— (상속)';
}
function flagCls(v: boolean | null | undefined) {
  if (v === true)  return 'flag-on';
  if (v === false) return 'flag-off';
  return 'flag-inherit';
}

async function loadRows() {
  try {
    const target = filterEqGroup.value || 'GLOBAL';
    const res: any = await defHttp.get(
      { url: `/wcs/system-mode?eqGroupId=${encodeURIComponent(target)}` },
      { isTransformResponse: false },
    );
    rows.value = [
      {
        id: target,
        eqGroupId: target === 'GLOBAL' ? null : target,
        operationMode: res?.operationMode,
        isOperationModeEnabled: res?.flags?.isOperationModeEnabled ?? null,
        isPutbackEnabled:       res?.flags?.isPutbackEnabled ?? null,
        isDispatchLockEnabled:  res?.flags?.isDispatchLockEnabled ?? null,
        isInspectionEnabled:    res?.flags?.isInspectionEnabled ?? null,
      },
    ];
  } catch (e: any) {
    notification.error({ message: '조회 오류', description: e?.message });
  }
}

function openEditor(row: any | null) {
  editing.value = row;
  editForm.eqGroupId = row?.eqGroupId ?? row?.id ?? '';
  editForm.operationMode = row?.operationMode ?? 'NORMAL';
  editForm.isOperationModeEnabled = row?.isOperationModeEnabled ?? null;
  editForm.isPutbackEnabled       = row?.isPutbackEnabled ?? null;
  editForm.isDispatchLockEnabled  = row?.isDispatchLockEnabled ?? null;
  editForm.isInspectionEnabled    = row?.isInspectionEnabled ?? null;
  editForm.reason = '';
  editorOpen.value = true;
}

async function saveEditor() {
  if (!editForm.reason.trim()) {
    notification.warning({ message: '사유는 필수입니다.' });
    return;
  }
  saving.value = true;
  try {
    // 운영 모드 저장
    await defHttp.put(
      {
        url: '/wcs/system-mode',
        data: {
          eqGroupId: editForm.eqGroupId,
          operationMode: editForm.operationMode,
          operator: 'UI',
          reason: editForm.reason,
        },
      },
      { isTransformResponse: false },
    );
    // 각 플래그 저장 (Boolean | null)
    for (const f of FLAG_KEYS) {
      const value = editForm[f.key];
      await defHttp.put(
        {
          url: '/wcs/system-mode/flag',
          data: {
            eqGroupId: editForm.eqGroupId,
            flagName: mapFlagKey(f.key),
            value,
            operator: 'UI',
            reason: editForm.reason,
          },
        },
        { isTransformResponse: false },
      );
    }
    notification.success({ message: '저장 완료' });
    editorOpen.value = false;
    await loadRows();
  } catch (e: any) {
    notification.error({ message: '저장 오류', description: e?.message });
  } finally {
    saving.value = false;
  }
}

function mapFlagKey(k: FlagKey): string {
  switch (k) {
    case 'isOperationModeEnabled': return 'operationMode';
    case 'isPutbackEnabled':       return 'putback';
    case 'isDispatchLockEnabled':  return 'dispatchLock';
    case 'isInspectionEnabled':    return 'inspection';
  }
}
</script>

<style scoped>
.ops-mode-mgmt-page {
  padding: 12px;
  height: 100%;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.flag-on {
  color: #52c41a;
  font-weight: 600;
}
.flag-off {
  color: #999;
}
.flag-inherit {
  color: #bbb;
  font-style: italic;
}
</style>
