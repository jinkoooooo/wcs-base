<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="modalTitle"
    @ok="handleSubmit"
    :useWrapper="false"
    width="950px"
  >
    <div class="modal-content-wrapper">
      <BasicGrid
        ref="gridRef"
        :show-pagination="false"
        :rowHeaders="gridMeta.rowHeaders"
        :columns="gridMeta.columns"
        :columnOptions="gridMeta.columnOptions"
        :fetchHandler="fetchHandler"
        :style="{ width: '100%', height: '100%' }"
        class="custom-alarm-grid"
      />
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
import 'tui-grid/dist/tui-grid.css';
import { ref, computed, nextTick } from 'vue';
import { Grid as BasicGrid } from '/@/components/Grid/index';
import { BasicModal, useModalInner } from '/@/components/Modal';
import { useI18n } from '/@/hooks/web/useI18n';

const emit = defineEmits(['register', 'ok']);
const { t } = useI18n();
const gridRef = ref();
const gridData = ref([]);
const dateStr = ref('');

const modalTitle = computed(() => {
  const label = t('label.alarm_detail_list', '알람 상세 내역');
  return dateStr.value ? `${label} (${dateStr.value})` : label;
});

const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
  setModalProps({ confirmLoading: false });

  if (data) {
    gridData.value = data.items || [];
    dateStr.value = data.date || '';
  } else {
    gridData.value = [];
    dateStr.value = '';
  }

  if (gridRef.value) {
    await nextTick();
    gridRef.value.fetch();

    // ✅ 수정 1: 모달 애니메이션이 완전히 끝난 후 레이아웃 재계산 (200ms -> 400ms 넉넉하게)
    setTimeout(() => {
      if (gridRef.value) {
        gridRef.value.refreshLayout();
      }
    }, 400);
  }
});

const gridMeta = {
  // ✅ 수정 2: 왼쪽 영역이 하얗게 깨지는 것을 방지하기 위해 rowNum 주석 처리 또는 삭제
  // rowHeaders: ['rowNum'],

  // ✅ 수정 3: 모달 안에서 그리드 데이터 영역이 0으로 쪼그라들지 않도록 고정 높이 부여
  bodyHeight: 400,

  columnOptions: {
    resizable: true,
    frozenCount: 0,
    minWidth: 50,
  },
  columns: [
    {
      header: t('label.begin_date', '발생일시'),
      name: 'created_at',
      sortable: true,
      width: 170,
      align: 'center',
      valign: 'middle',
    },
    {
      header: t('label.classification', '설비명'),
      name: 'unit_code',
      sortable: true,
      width: 90,
      align: 'center',
      valign: 'middle',
    },
    {
      header: t('label.alarm_code', '에러코드'),
      name: 'error_code',
      sortable: true,
      width: 90,
      align: 'center',
      valign: 'middle',
    },
    {
      header: t('label.alarm_content', '에러내용'),
      name: 'error_msg',
      sortable: true,
      align: 'left',
      valign: 'middle',
      ellipsis: true,
      // ✅ 수정 4: 존재하지 않는 renderer 삭제 및 formatter로 툴팁 구현
      formatter: ({ value }) => {
        return value ? `<span title="${value}">${value}</span>` : '';
      },
      escapeHTML: false, // HTML 태그(span)가 그대로 렌더링되도록 허용
    },
  ],
  data: [],
};

async function fetchHandler() {
  return {
    records: gridData.value,
    total: gridData.value.length,
  };
}

const handleSubmit = async () => {
  emit('ok');
  closeModal();
};
</script>

<style lang="less" scoped>
  .modal-content-wrapper {
    height: 500px;
    display: flex;
    flex-direction: column;
    padding: 10px 15px;
    background-color: @component-background;
  }

  :deep(.ant-modal-footer) {
    border-top: none !important;
  }

  /* ✅ [Grid Design Polish] 행 높이 고정 및 줄바꿈 강제 금지 */
  :deep(.tui-grid-cell) {
    vertical-align: middle !important;
  }

  :deep(.tui-grid-cell-content) {
    line-height: 40px !important; /* 행 높이와 텍스트 높이 일치 */
    white-space: nowrap !important; /* 줄바꿈 절대 금지 */
    overflow: hidden !important;
    text-overflow: ellipsis !important;
  }

  /* 그리드 행 높이 고정 */
  :deep(.tui-grid-body-area .tui-grid-row) {
    height: 40px !important;
  }
</style>
