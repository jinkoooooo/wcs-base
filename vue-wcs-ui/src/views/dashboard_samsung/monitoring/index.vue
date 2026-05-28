<template>
  <PageWrapper class="overflow-hidden max-full page-background">
    <div class="inline-flex flex-col flex-1 page-background">
      <div class="form-container">
        <div class="absolute top-0 left-0 right-0 flex justify-between items-center h-10">
          <span class="ml-5 flex items-center">
            <img src="./systems_logo_white.png" class="h-10 w-auto pr-5" />
            <span style="font-size: 1rem; padding-left: 2vh" class="text-white">
              {{ currentTime }}
            </span>
          </span>

          <div class="flex items-center space-x-3 mt-1.5 mr-4">
            <div class="relative cursor-pointer" @click="openAlarmPopup">
              <Badge :count="displayErrorCount" :overflowCount="99" title="에러 로그 확인">
                <Button
                  shape="circle"
                  type="primary"
                  ghost
                  class="flex items-center justify-center border-none bg-transparent hover:bg-white/10"
                >
                  <BellOutlined style="font-size: 1.2rem; color: white" />
                </Button>
              </Badge>
            </div>

            <DatePicker
              v-model:value="chooseDateRef"
              :size="sizeRef"
              :bordered="false"
              :allowClear="false"
              placeholder="날짜 선택"
              format="YYYY-MM-DD"
              @change="changeValue"
              class="custom-date-picker"
              :style="{ width: '8.5rem' }"
            />
          </div>
        </div>
      </div>

      <div class="dashboard-main mt-2">
        <div class="dashboard-grid">
          <div class="grid-cell cell-inbound-delivery">
            <InboundDelivery
              :loading="loading"
              :data="inboundDeliveryListRef"
              :date="chooseDateRef"
              class="w-full h-full"
            />
          </div>
          <div class="grid-cell cell-inbound-status">
            <InboundStatus
              :loading="loading"
              :data="inboundStatus"
              :date="chooseDateRef"
              class="w-full h-full"
            />
          </div>
          <div class="grid-cell cell-chute">
            <PalletizerJobStatus
              :loading="loading"
              :data="chuteRef"
              :date="chooseDateRef"
              class="w-full h-full"
            />
          </div>
          <div class="grid-cell cell-unit-status">
            <EquipmentStatus
              :loading="loading"
              :data="equipmentStatus"
              :date="chooseDateRef"
              class="w-full h-full"
            />
          </div>
          <div class="grid-cell cell-volume2">
            <AlarmHistoryView :loading="loading" :data="uphChartDataRef" :date="chooseDateRef" />
          </div>
          <div class="grid-cell cell-volume">
            <VolumeOfGoods
              :loading="loading"
              :data="volumeOfGoodsRef"
              :date="chooseDateRef"
              @lastDays="lastDaysChanged"
            />
          </div>
        </div>
      </div>

      <AlarmDetailPopup @register="registerAlarmModal" @ok="handleAlarmConfirm" />
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { ref, onMounted, watch, computed } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { DatePicker, Button, Badge } from 'ant-design-vue';
  import { BellOutlined } from '@ant-design/icons-vue';
  import { getCommonGetApi } from '/@/api/common/api';
  import dayjs, { Dayjs } from 'dayjs';

  import { useModal } from '/@/components/Modal';
  import AlarmDetailPopup from './card/AlarmDetailPopup.vue';

  import InboundDelivery from './card/InboundDelivery.vue';
  import EquipmentStatus from './card/EquipmentStatus.vue';
  import VolumeOfGoods from './card/VolumeOfGoods.vue';
  import InboundStatus from './card/InboundStatus.vue';
  import PalletizerJobStatus from './card/PalletizerJobStatus.vue';
  import AlarmHistoryView from './card/AlarmHistoryView.vue';

  import type { SelectProps } from 'ant-design-vue';

  const chooseDateRef = ref<Dayjs>(dayjs());
  const sizeRef = ref<SelectProps['size']>('small');
  const currentTime = ref();
  const loading = ref(true);
  const lastDaysRef = ref();

  // 데이터 Refs
  const inboundDeliveryListRef = ref([]);
  const inboundStatus = ref(null);
  const chuteRef = ref(null);
  const equipmentStatus = ref(null);
  const volumeOfGoodsRef = ref(null);
  const uphChartDataRef = ref([]);

  // ✅ [수정] 알람 카운트 로직 단순화
  const errorLogDataRef = ref([]);
  const lastConfirmedCount = ref(0); // 사용자가 마지막으로 확인 버튼 눌렀을 때의 에러 개수

  // 표시할 뱃지 개수 = (현재 총 에러 개수) - (확인했던 개수)
  const displayErrorCount = computed(() => {
    const total = errorLogDataRef.value?.length || 0;
    const count = total - lastConfirmedCount.value;
    return count > 0 ? count : 0; // 음수 방지
  });

  const [registerAlarmModal, { openModal }] = useModal();

  setTimeout(() => {
    loading.value = false;
  }, 1500);

  onMounted(async () => {
    await initData();
    setInterval(updateTime, 1000);
    setInterval(mainDataBinding, 5000);
    setInterval(dateDataBinding, 5000);
  });

  async function initData() {
    loading.value = true;
    mainDataBinding();
    dateDataBinding();
  }

  function updateTime() {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    const h = String(now.getHours()).padStart(2, '0');
    const min = String(now.getMinutes()).padStart(2, '0');
    const s = String(now.getSeconds()).padStart(2, '0');
    currentTime.value = `${y}-${m}-${d} ${h}:${min}:${s}`;
  }

  async function changeValue() {
    if (chooseDateRef.value) {
      await mainDataBinding();
      await dateDataBinding();
    }
  }

  async function lastDaysChanged(e: number) {
    lastDaysRef.value = e;
    await dateDataBinding();
  }

  function openAlarmPopup() {
    openModal(true, {
      items: errorLogDataRef.value,
      date: chooseDateRef.value.format('YYYY-MM-DD'),
    });
  }

  // ✅ [핵심] 팝업에서 확인 버튼 누르면 실행
  function handleAlarmConfirm() {
    // 현재 에러 개수를 '확인된 개수'로 저장 -> 뱃지 0됨
    const currentTotal = errorLogDataRef.value?.length || 0;
    lastConfirmedCount.value = currentTotal;
  }

  async function mainDataBinding() {
    try {
      const chooseDateString = chooseDateRef.value.format('YYYY-MM-DD');
      let param = { chooseDate: chooseDateString };
      let mainResponse = await getCommonGetApi('/dashboard/main', param);
      dataBindingResponseMainDataToRef(mainResponse);
    } catch (Error) {
      console.log(Error);
    }
  }

  async function dateDataBinding() {
    const chooseDateString = chooseDateRef.value.format('YYYY-MM-DD');
    let param = { chooseDate: chooseDateString, inputData: lastDaysRef.value };
    try {
      let dateResponse = await getCommonGetApi('/dashboard/date', param);
      dataBindingResponseDateDataToRef(dateResponse);
    } catch (Error) {
      console.log(Error);
    }
  }

  function dataBindingResponseMainDataToRef(response: any) {
    inboundDeliveryListRef.value = response.dashboard_inbound_delivery_list || [];
    inboundStatus.value = response.dashboard_inbound_status;
    chuteRef.value = response.dashboard_chute;
    equipmentStatus.value = response.dashboard_equipment_status;
    uphChartDataRef.value = response.dashboard_uph_list || response.dashboardUphList || [];
    errorLogDataRef.value = response.dashboard_error_log_list || [];
  }

  function dataBindingResponseDateDataToRef(response: any) {
    volumeOfGoodsRef.value = response.dashboard_volume_of_goods;
  }

  watch(
    [chooseDateRef],
    () => {
      mainDataBinding();
      dateDataBinding();
    },
    { immediate: true },
  );
</script>

<style scoped>
  /* 스타일 기존 동일 */
  .page-background {
    --dashboard-bg-color: #002b55;
    --dashboard-primary-color: #00bfff;
    background: var(--dashboard-bg-color);
    margin-top: inherit;
  }
  :deep(.custom-date-picker) {
    background-color: #ffffff !important;
    border-radius: 6px !important;
    border: none !important;
    padding: 0 8px !important;
    height: 1.5rem !important;
    box-shadow: 0 2px 0 rgba(0, 0, 0, 0.015);
  }
  :deep(.custom-date-picker .ant-picker-input > input) {
    color: #333333 !important;
    font-weight: 500 !important;
    font-size: 0.8rem !important;
    text-align: center;
  }
  :deep(.custom-date-picker .ant-picker-suffix) {
    color: #333333 !important;
  }
  :deep(.custom-date-picker input::placeholder) {
    color: #bfbfbf !important;
  }
  :deep(.custom-date-picker:hover) {
    cursor: pointer;
    opacity: 0.9;
  }

  .dashboard-main {
    flex: 1 1 auto;
    display: flex;
    padding: 0 0 0.8rem;
    box-sizing: border-box;
  }
  .dashboard-grid {
    display: grid;
    width: 100%;
    grid-template-columns: 1fr 1fr 1fr 1fr 1fr 1fr;
    grid-template-rows: auto auto auto;
    column-gap: 0.8rem;
    row-gap: 0.8rem;
  }
  .cell-inbound-delivery {
    grid-column: 1 / 5;
    grid-row: 1 / 2;
  }
  .cell-inbound-status {
    grid-column: 5 / 7;
    grid-row: 1 / 2;
  }
  .cell-chute {
    grid-column: 1 / 5;
    grid-row: 2 / 3;
  }
  .cell-unit-status {
    grid-column: 5 / 7;
    grid-row: 2 / 3;
  }
  .cell-volume2 {
    grid-column: 1 / 4;
    grid-row: 3 / 4;
  }
  .cell-volume {
    grid-column: 4 / 7;
    grid-row: 3 / 4;
  }
  .grid-cell {
    width: 100%;
    height: 100%;
  }
  .grid-cell > * {
    height: 100%;
  }
</style>
