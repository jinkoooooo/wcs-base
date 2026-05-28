<template>
  <!-- 의뢰번호 단위 추천 파렛트 확인 후 출고 지시 -->
  <BasicModal
    @register="registerModal"
    title="추천 파렛트 출고"
    :width="640"
    :confirmLoading="issuing"
    okText="출고 지시"
    cancelText="닫기"
    :okButtonProps="{ disabled: !canIssue }"
    @ok="doIssue"
    @cancel="handleClose"
  >
    <a-spin :spinning="loading">
      <!-- 추천 없음 -->
      <div v-if="!loading && !found" class="rec-empty">
        <a-alert type="warning" :message="emptyMsg" show-icon />
      </div>

      <!-- 추천 결과 -->
      <template v-else-if="found">
        <div class="rec-summary">
          <div class="rec-row">
            <span class="rec-label">의뢰번호</span>
            <span class="rec-value">{{ testRequestNo }}</span>
          </div>
          <div class="rec-row">
            <span class="rec-label">추천 파렛트</span>
            <span class="rec-value strong">{{ rec.pallet_barcode }}</span>
          </div>
          <div class="rec-row">
            <span class="rec-label">로케이션</span>
            <span class="rec-value">{{ rec.loc_id || '-' }}</span>
          </div>
          <div class="rec-row">
            <span class="rec-label">최소 박스번호</span>
            <span class="rec-value strong">{{ rec.min_box_seq }}</span>
          </div>
        </div>

        <!-- 추천 파렛트의 박스 목록 (최소 박스번호 행 강조) -->
        <a-table
          :dataSource="boxes"
          :columns="boxColumns"
          :pagination="false"
          size="small"
          :rowKey="(r) => r.box_barcode || String(r.box_seq)"
          :scroll="{ y: 220 }"
          :rowClassName="
            (r) => (Number(r.box_seq) === Number(rec.min_box_seq) ? 'min-box-row' : '')
          "
        />

        <!-- 출고대 선택 -->
        <div class="port-row">
          <span class="rec-label">출고대</span>
          <a-select
            v-model:value="selectedPort"
            :options="portOptions"
            :loading="portsLoading"
            style="width: 260px"
            size="small"
            placeholder="출고대 선택"
            allowClear
          />
        </div>
      </template>
    </a-spin>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed } from 'vue';
  import { BasicModal, useModal } from '/src/components/Modal';
  import { getCommonGetListApi, getCommonPostApi } from '/@/api/common/api';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emit = defineEmits<{
    (e: 'ready', api: { openModal: (data?: any) => void; closeModal: () => void }): void;
    (e: 'issued'): void;
  }>();

  const { notification } = useMessage();

  // 부모가 openModal({ testRequestNo }) 로 넘겨줌
  const testRequestNo = ref('');

  const loading = ref(false);
  const found = ref(false);
  const emptyMsg = ref('추천 가능한 파렛트가 없습니다.');
  const rec = ref<any>({});
  const boxes = ref<any[]>([]);

  const portOptions = ref<Array<{ label: string; value: string }>>([]);
  const portsLoading = ref(false);
  const selectedPort = ref<string | undefined>(undefined);
  const issuing = ref(false);

  // 출고 가능 = 추천 있음 + 출고대 선택됨
  const canIssue = computed(() => found.value && !!selectedPort.value && !issuing.value);

  const boxColumns = [
    { title: '박스번호', dataIndex: 'box_seq', width: 90, align: 'center' },
    { title: '박스 바코드', dataIndex: 'box_barcode', align: 'center' },
    { title: '잔여수량', dataIndex: 'remaining_qty', width: 100, align: 'center' },
  ];

  // BasicModal 등록 — InboundPlanPopup 과 동일 패턴(visible prop 무효, openModal API 로 제어)
  const [registerModal, { openModal, closeModal }] = useModal();

  // 부모 진입점: 모달 열기 + 추천 조회
  function open(data: { testRequestNo: string }) {
    testRequestNo.value = data?.testRequestNo || '';
    selectedPort.value = undefined;
    openModal();
    if (testRequestNo.value) {
      loadRecommend();
    } else {
      found.value = false;
      emptyMsg.value = '의뢰번호가 없습니다.';
    }
  }

  function handleClose() {
    closeModal();
  }

  // 백엔드(Map) 응답 정규화 — 일부 transform 이 data 로 감쌀 수 있어 방어
  function unwrap(raw: any): any {
    if (raw && raw.found !== undefined) return raw;
    if (raw && raw.data && raw.data.found !== undefined) return raw.data;
    return raw ?? {};
  }

  // 의뢰번호 추천 파렛트 미리보기
  async function loadRecommend() {
    loading.value = true;
    found.value = false;
    rec.value = {};
    boxes.value = [];
    try {
      const url =
        '/wcs/outbound/qc-test/recommend?testRequestNo=' + encodeURIComponent(testRequestNo.value);
      const data = unwrap(await getCommonGetListApi(url, null));
      if (data.found) {
        found.value = true;
        rec.value = data;
        boxes.value = Array.isArray(data.boxes) ? data.boxes : [];
        await loadPorts(data.eq_group_id);
      } else {
        found.value = false;
        emptyMsg.value = data.message || '추천 가능한 파렛트가 없습니다.';
      }
    } catch (e: any) {
      found.value = false;
      emptyMsg.value = e?.message || '추천 조회 중 오류가 발생했습니다.';
    } finally {
      loading.value = false;
    }
  }

  // 출고대 목록 (추천 파렛트의 eqGroupId 기준)
  async function loadPorts(eqGroupId?: string) {
    portsLoading.value = true;
    try {
      const url = eqGroupId
        ? '/wcs/outbound/qc-test/ports?eqGroupId=' + encodeURIComponent(eqGroupId)
        : '/wcs/outbound/qc-test/ports';
      const resp: any = await getCommonGetListApi(url, null);
      const list: any[] = Array.isArray(resp) ? resp : resp?.items ?? [];
      portOptions.value = list.map((r) => ({
        label: `${r.port_code ?? ''} ${r.port_name ?? ''}`.trim(),
        value: r.port_code,
      }));
      if (portOptions.value.length > 0 && !selectedPort.value) {
        selectedPort.value = portOptions.value[0].value;
      }
    } finally {
      portsLoading.value = false;
    }
  }

  // 추천 파렛트 출고 지시 — 서버에서 추천 재계산 후 발행
  async function doIssue() {
    if (!canIssue.value) return;
    issuing.value = true;
    try {
      const resp: any = await getCommonPostApi('/wcs/outbound/qc-test/issue-recommended', {
        testRequestNo: testRequestNo.value,
        portCode: selectedPort.value,
      });
      if (resp?.success) {
        notification.success({
          message: '출고지시 완료',
          description: resp.message || '추천 파렛트 출고 지시가 등록되었습니다.',
        });
        emit('issued');
        closeModal();
      } else {
        notification.error({ message: '실패', description: resp?.message || '출고 지시 오류' });
      }
    } catch (e: any) {
      notification.error({ message: '오류', description: e?.message || '출고 지시 오류' });
    } finally {
      issuing.value = false;
    }
  }

  // 부모에 openModal API 전달 (InboundPlanPopup 과 동일 패턴)
  emit('ready', { openModal: open, closeModal });
</script>

<style scoped>
  .rec-summary {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 12px 16px;
    margin-bottom: 12px;
  }
  .rec-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 3px 0;
  }
  .rec-label {
    font-size: 12px;
    color: #64748b;
    font-weight: 500;
  }
  .rec-value {
    font-size: 14px;
    color: #0f172a;
  }
  .rec-value.strong {
    font-weight: 700;
  }
  .port-row {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 14px;
  }
  .rec-empty {
    padding: 12px 0;
  }
  :deep(.min-box-row) {
    background: #fff7e6;
    font-weight: 600;
  }
</style>
