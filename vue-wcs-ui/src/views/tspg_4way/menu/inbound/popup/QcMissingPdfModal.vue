<template>
  <a-modal
    v-model:open="visible"
    title="신규 시험 의뢰 PDF 첨부"
    :width="780"
    :confirm-loading="submitting"
    :ok-text="`발행 (${rows.length}건)`"
    :ok-button-props="{ disabled: !canSubmit }"
    @ok="onConfirm"
    @cancel="onCancel"
  >
    <p class="hint">
      아래 항목은 (입고일자, SKU, LOT) 기준 시험 의뢰 마스터가 존재하지 않습니다.
      <b>각 항목마다 PDF 를 첨부</b>해야 입고 등록이 진행됩니다.
    </p>

    <a-table
      :columns="columns"
      :data-source="rows"
      :pagination="false"
      size="small"
      rowKey="key"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'test_request_no'">
          <a-input
            v-model:value="record.test_request_no"
            placeholder="(공란이면 자동 채번)"
            size="small"
          />
        </template>
        <template v-else-if="column.key === 'pdf'">
          <a-upload
            :before-upload="(file: File) => onPick(record, file)"
            :show-upload-list="false"
            :max-count="1"
            accept=".pdf"
          >
            <a-button size="small">{{ record.pdfFile ? '교체' : '파일 선택' }}</a-button>
          </a-upload>
          <span v-if="record.pdfFile" class="filename">{{ record.pdfFile.name }}</span>
          <span v-else class="missing">미첨부</span>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { apiClient } from '/@/api/common/api';

  type MissingRow = {
    key: string;          // sku|lot 합성 키
    inbound_date: string; // YYYY-MM-DD
    sku: string;
    lot_no: string;
    owner_code: string;
    test_request_no: string;
    pdfFile: File | null;
  };

  const emit = defineEmits<{
    (e: 'done', publishedKeys: string[]): void; // 모두 발행 성공 → 부모가 register 호출
    (e: 'cancel'): void;
  }>();

  const { notification } = useMessage();

  const visible = ref(false);
  const submitting = ref(false);
  const rows = ref<MissingRow[]>([]);

  const columns = [
    { title: '입고일자', dataIndex: 'inbound_date', width: 110 },
    { title: 'SKU',      dataIndex: 'sku',          width: 140 },
    { title: 'LOT No.',  dataIndex: 'lot_no',       width: 140 },
    { title: '화주',     dataIndex: 'owner_code',   width: 100 },
    { title: '시험의뢰번호 (선택)', key: 'test_request_no', width: 180 },
    { title: 'PDF',      key: 'pdf',                width: 240 },
  ];

  const canSubmit = computed(
    () => rows.value.length > 0 && rows.value.every((r) => r.pdfFile != null),
  );

  function open(missing: MissingRow[]) {
    rows.value = missing.map((m) => ({ ...m, pdfFile: null }));
    visible.value = true;
  }

  function onPick(record: MissingRow, file: File): boolean {
    record.pdfFile = file;
    return false; // a-upload 자동 업로드 차단
  }

  async function onConfirm() {
    if (!canSubmit.value) {
      notification.warning({
        message: '안내',
        description: '모든 항목에 PDF 를 첨부해야 합니다.',
      });
      return;
    }
    submitting.value = true;
    const publishedKeys: string[] = [];
    try {
      for (const r of rows.value) {
        const form = new FormData();
        form.append('inbound_date', r.inbound_date);
        form.append('sku', r.sku);
        if (r.lot_no) form.append('lot_no', r.lot_no);
        if (r.owner_code) form.append('owner_code', r.owner_code);
        if (r.test_request_no?.trim()) form.append('test_request_no', r.test_request_no.trim());
        form.append('file', r.pdfFile as Blob);
        await apiClient.post('/wcs/qc-test/request/with-pdf', form, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        publishedKeys.push(r.key);
      }
      notification.success({
        message: '시험 의뢰 발행 완료',
        description: `${publishedKeys.length}건 발행됨. 입고 등록을 진행합니다.`,
      });
      visible.value = false;
      emit('done', publishedKeys);
    } catch (e: any) {
      notification.error({
        message: '시험 의뢰 발행 실패',
        description: e?.message || '발행 중 오류가 발생했습니다.',
      });
    } finally {
      submitting.value = false;
    }
  }

  function onCancel() {
    visible.value = false;
    emit('cancel');
  }

  defineExpose({ open });
</script>

<style scoped>
  .hint { margin-bottom: 12px; color: #555; }
  .filename { margin-left: 8px; color: #1677ff; font-size: 12px; }
  .missing  { margin-left: 8px; color: #ff4d4f; font-size: 12px; }
</style>
