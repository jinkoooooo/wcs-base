<template>
  <div class="card-wrapper">
    <div class="custom-ribbon"></div>
    <Card
      :loading="loading"
      ref="cardRef"
      :title="title"
      :headStyle="headStyleRef"
      :bodyStyle="bodyStyleRef"
      :style="styleRef"
    >
      <div class="list-header">
        <div class="col-group-left">
          <div class="th-cell cell-chute">작업위치</div>
          <div class="th-cell cell-code">SKU</div>
          <div class="th-cell cell-name center-align">제품명</div>
        </div>
        <div class="col-group-right">
          <div class="th-cell cell-pallet center-text">Pallet 현황</div>

          <div class="th-cell cell-progress-wrapper">
            <div class="cell-progress-text">적재 현황</div>
            <div class="cell-progress-spacer"></div>
          </div>
        </div>
      </div>

      <div class="list-container">
        <div v-for="(item, index) in chuteList" :key="index" class="list-row">
          <div class="col-group-left row-content">
            <div
              class="td-cell cell-chute highlight-text clickable-chute"
              @click="openDetailModal(item)"
            >
              {{ item.endPointCd }}
            </div>
            <div class="td-cell cell-code">{{ item.itemCode }}</div>
            <div class="td-cell cell-name">{{ item.itemName }}</div>
          </div>

          <div class="col-group-right row-progress">
            <div class="td-cell cell-pallet center-text">
              {{ item.currentPallets }} / {{ item.totalPallets }}
            </div>

            <div class="progress-container">
              <div class="progress-track">
                <div class="progress-fill" :style="{ width: item.percent + '%' }"></div>
                <div class="progress-text-overlay">
                  {{ item.boxQty }} / {{ item.palletCapacity }}
                </div>
              </div>
              <div class="progress-percent">{{ item.percent }}%</div>
            </div>
          </div>
        </div>
      </div>
    </Card>

    <Modal
      v-model:visible="isModalVisible"
      :title="modalTitle"
      :footer="null"
      width="600px"
      centered
      wrap-class-name="dark-modal-wrapper"
      destroyOnClose
    >
      <div v-if="isModalVisible && selectedChute" class="modal-content-body">
        <Pollet3DViewer
          :count="selectedChute.boxQty"
          :boxWidth="selectedChute.boxWidth"
          :boxLength="selectedChute.boxLength"
          :boxHeight="selectedChute.boxHeight"
        />
      </div>
    </Modal>
  </div>
</template>

<script lang="ts" setup>
  import { Card, Modal } from 'ant-design-vue';
  import { ref, type CSSProperties, watch, computed } from 'vue';
  import Pollet3DViewer from '@/views/dashboard_samsung/monitoring/card/Pallet3DViewer.vue';

  const props = defineProps({
    loading: Boolean,
    data: { type: Object, required: false, default: () => [] },
    date: { type: Object, required: true },
  });

  const title = 'PALLETIZER 작업 현황';

  const styleRef = ref({
    border: '1px solid var(--dashboard-primary-color)',
    borderRadius: '0px',
    background: 'var(--dashboard-bg-color)',
    height: '100%',
    display: 'flex',
    flexDirection: 'column',
  });

  const headStyleRef = computed(
    (): CSSProperties => ({
      textAlign: 'center',
      lineHeight: '0.5vw',
      minHeight: '0vw',
      fontSize: '1.1rem',
      backgroundColor: 'var(--dashboard-bg-color)',
      color: 'white',
      borderRadius: '0px',
      padding: '0px',
    }),
  );

  const bodyStyleRef: CSSProperties = {
    backgroundColor: 'var(--dashboard-bg-color)',
    padding: '5px 10px 10px 10px',
    color: 'white',
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
    boxSizing: 'border-box',
  };

  interface ChuteUIModel {
    endPointCd: string;
    itemCode: string;
    itemName: string;
    boxQty: number;
    palletCapacity: number;
    currentPallets: number;
    totalPallets: number;
    percent: number;
    boxWidth: number;
    boxLength: number;
    boxHeight: number;
  }

  const chuteList = ref<ChuteUIModel[]>([]);
  const isModalVisible = ref(false);
  const selectedChute = ref<ChuteUIModel | null>(null);
  const modalTitle = computed(() =>
    selectedChute.value
      ? `${selectedChute.value.endPointCd} 실시간 적재 시뮬레이션`
      : '적재 시뮬레이션',
  );

  function openDetailModal(item: ChuteUIModel) {
    selectedChute.value = item;
    isModalVisible.value = true;
  }

  function setData(newData: any[]) {
    if (!newData) return;
    chuteList.value = newData.map((item: any) => {
      const expected = item.expected_quantity || 0;
      const completed = item.completed_quantity || 0;
      const boxQty = item.pallet_qty || 0;
      const palletCapacity = item.pallet_capacity || 0;
      const totalPallets = palletCapacity > 0 ? Math.ceil(expected / palletCapacity) : 0;
      const currentPallets = palletCapacity > 0 ? Math.ceil(completed / palletCapacity) : 0;
      let percentVal = 0;
      if (palletCapacity > 0) percentVal = Math.round((boxQty / palletCapacity) * 100);

      return {
        endPointCd: item.end_point_cd || '-',
        itemCode: item.item_code || '',
        itemName: item.item_name || '',
        boxQty,
        palletCapacity,
        currentPallets,
        totalPallets,
        percent: percentVal,
        boxWidth: item.item_width || 300,
        boxLength: item.item_length || 400,
        boxHeight: item.item_height || 250,
      };
    });
  }

  watch(
    [() => props.loading, () => props.data],
    async ([newLoading, newData]) => {
      if (newLoading) return;
      setData(Array.isArray(newData) ? newData : []);
    },
    { immediate: true },
  );
</script>

<style scoped>
  .card-wrapper {
    height: 100%;
    display: flex;
    flex-direction: column;
  }
  .custom-ribbon {
    position: absolute;
    background: var(--dashboard-primary-color, #00bfff);
    color: white;
    padding: 18px 20px;
    font-size: 14px;
    font-weight: bold;
    z-index: 10;
    clip-path: polygon(0% 0%, 100% 0%, 0% 100%, 0% 100%);
  }

  .col-group-left {
    flex: 4;
    display: flex;
    align-items: center;
    overflow: hidden;
  }

  /* [수정] gap 추가하여 헤더와 본문의 수직 정렬 통일 */
  .col-group-right {
    flex: 6;
    display: flex;
    justify-content: flex-start;
    align-items: center;
    padding-left: 5px;
    gap: 8px;
  }

  .cell-chute {
    flex: 0 0 15%;
    text-align: center;
  }
  .clickable-chute {
    cursor: pointer;
    text-decoration: underline;
    text-underline-offset: 3px;
    transition: all 0.3s ease;
  }
  .clickable-chute:hover {
    color: #00ffff !important;
    text-shadow: 0 0 8px rgba(0, 255, 255, 0.6);
  }
  .cell-code {
    flex: 0 0 50%;
    text-align: center;
    padding-left: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: white;
    font-weight: bold;
  }
  .cell-name {
    flex: 1;
    text-align: center;
    padding-left: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: white;
    font-weight: bold;
  }
  .cell-pallet {
    flex: 0 0 25%;
    font-weight: bold;
  }

  /* [수정] 헤더 정렬용 클래스 추가 */
  .cell-progress-wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px; /* 본문의 gap과 통일 */
  }
  .cell-progress-text {
    flex: 1;
    text-align: center; /* Bar 영역 내에서 중앙 정렬 */
  }
  .cell-progress-spacer {
    width: 40px; /* 퍼센트 텍스트(40px) 공간만큼 빈칸 확보 */
  }

  .list-header {
    display: flex;
    font-size: 0.9rem;
    font-weight: bold;
    color: white;
    padding-bottom: 5px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    margin-top: 10px;
    margin-bottom: 0.1rem;
  }
  .center-text {
    width: 100%;
    text-align: center;
  }
  .list-container {
    flex-grow: 1;
    padding-right: 0px;
  }
  .list-row {
    display: flex;
    align-items: center;
    padding: 7px 0;
    border-bottom: 1px dashed rgba(255, 255, 255, 0.1);
    font-size: 0.85rem;
  }
  .highlight-text {
    font-weight: bold;
    color: #ffb6c1;
  }
  .td-cell {
    color: #ddd;
  }

  /* [수정] 본문 gap 제거 (상위 col-group-right에서 상속) */
  .row-progress {
    display: flex;
    width: 100%;
  }

  .progress-container {
    display: flex;
    flex: 1;
    align-items: center;
    gap: 8px;
  }
  .progress-track {
    flex-grow: 1;
    height: 1.4rem;
    background-color: rgba(120, 240, 211, 0.15);
    border: 1px solid rgba(255, 255, 255, 0.3);
    position: relative;
    display: flex;
    align-items: center;
  }
  .progress-fill {
    height: 100%;
    background-color: #32cd32;
    opacity: 0.7;
    transition: width 0.5s ease;
  }
  .progress-text-overlay {
    position: absolute;
    width: 100%;
    text-align: center;
    font-size: 0.8rem;
    font-weight: bold;
    color: white;
    text-shadow: 1px 1px 1px rgba(0, 0, 0, 0.5);
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    z-index: 2;
  }
  .progress-percent {
    width: 40px;
    text-align: right;
    font-size: 0.9rem;
    font-weight: bold;
    color: #ffffff;
  }
  .center-align {
    text-align: center !important;
    padding-left: 0;
  }
  .modal-content-body {
    background-color: #021a30;
    padding: 10px;
    border-radius: 4px;
  }
</style>
