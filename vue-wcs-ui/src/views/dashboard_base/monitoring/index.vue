<template>
  <PageWrapper class="overflow-hidden max-full page-background">
    <div class="inline-flex flex-col flex-1 page-background">
      <!-- 상단 영역 -->
      <div class="form-container">
        <div class="absolute top-0 left-0 right-0 flex justify-between items-center">
          <!-- 왼쪽 섹션 -->
          <span class="ml-5 flex items-center mt-2">
            <img src="./BorgWarner_Logo_White.png" class="w-45 pr-5" />
            <span style="font-size: 1rem; padding-left: 2vh" class="text-white">
            {{ currentTime }}
            </span>
          </span>

          <!-- BUTTON(POPUP) + BUTTON(POPUP) + SELECT-BOX AREA-->
          <div class="flex items-center space-x-2 mt-1.5 mr-4">
            <Button
              class="clear-btn red-button"
              @click="shipmentStatusPopup"
              :style="buttonStyle"
            >
              <Icon icon="ion:add-outline" :style="iconStyleRef" />{{shipmentStatusLabel }}
            </Button>
            <Button
              class="clear-btn red-button"
              @click="alarmDetailPopup"
              :style="buttonStyle"
            >
              <Icon icon="ion:alarm-outline" :style="iconStyleRef" />{{alarmDetailLabel }}
            </Button>
            <Select
              v-model:value="chooseDateRef"
              :size="sizeRef"
              :options="selectOptionsRef"
              @change="changeValue"
              :style="{ width: '7.5rem', fontSize: '0.75rem', padding: '0px' }"
            />
          </div>
        </div>
      </div>

      <!-- 메인 콘텐츠 (flex 컨테이너) -->
      <div class="flex flex-row space-x-0.7 mt-2">
        <!-- 좌측 (40%) -->
        <div class="flex-1 min-w-[50%] max-w-[50%] ">
          <div class="box-container">
            <div class="flex w-full h-full space-x-0.8">

              <!-- 입하 종합 & 품질검사 현황 -->
              <div class="w-2.3/4">
              </div>

              <!-- 자재입고 -->
              <div class="w-1.7/4">
              </div>
            </div>
          </div>
          <div>

            <div class="flex-1 space-y-0.8">
              <div class="flex-1">
              </div>
              <div class="flex flex-row gap-0.8">
                <!-- 창고가동률 -->
                <div class="w-2.3/4">
                </div>
                 <!-- 당일 검사 -->
                <div class="w-1.7/4">
                </div>
              </div>
              <div class="flex flex-row gap-0.8">
                <div class="w-1/3">
                 <!-- SW 모터 -->
                </div>
                <div class="w-1/3">
                <!-- SW 스테이터 -->
              </div>

              <div class="w-1/3">
                <!-- SW 인버터 -->
              </div>
              </div>

            </div>
          </div>
        </div>

        <div class="flex-1 min-w-[49.7%] max-w-[49.7%] flex flex-col">
          <!-- 상단 영역: 대물투입 + I/F 현황, 소물분류 현황 + 설비현황 -->
          <div class="flex flex-row flex-1 space-x-0.8">
            <!-- 대물투입 + I/F 현황 (50%) -->
            <div class="min-w-[42%] max-w-[42%] flex flex-col space-y-0.8">

              <div class="flex-5" :class="isKorean ? 'mt--0.1' : 'mt--0.5'">
                <!-- 대물투입 -->
              </div>
              <div class="flex-4">
                <!-- I/F현황 -->
              </div>
            </div>

            <!-- 소물분류 현황 + 설비현황 (50%) -->
            <div class="flex-1 flex flex-col space-y-0.8">
              <div class="flex-3">
                <!-- 소물분류 현황 -->
              </div>
              <div class="flex flex-row gap-0.8">
                <div class="w-2/2">
                  <!-- 설비 현황 -->
                </div>
              </div>
            </div>
          </div>

          <!-- 하단 영역: 물동량 현황 (50%) -->
          <div class="flex-1 box-container w-full mt-0.8">
            <!-- 물동량 현황 -->
          </div>
        </div>
      </div>
    </div>
  </PageWrapper>
</template>

<script lang="ts" setup>
  import { ref,onUnmounted,onMounted,computed } from 'vue';
  import { PageWrapper } from '/@/components/Page';
  import { Select } from 'ant-design-vue';
  import { getCommonGetApi } from '/@/api/common/api';
  import { useModal } from '/@/components/Modal';
  import InboundQualityStatus from './card/InboundQualityStatus.vue';
  import StatusOfFacility from './card/StatusOfFacility.vue';
  import SameDayInspection from './card/SameDayInspection.vue';
  import VolumeOfGoodsStatus from './card/VolumeOfGoodsStatus.vue';
  import AlarmDetailPopup from './card/AlarmDetailPopup.vue';
  import ShipmentStatusPopup from './card/ShipmentStatusPopup.vue';
  import SWStator from './card/SWStator.vue';
  import SWInverter from './card/SWInverter.vue';
  import SWMotor from './card/SWMotor.vue';
  import { useLocaleStoreWithOut } from '/@/store/modules/locale';
  import WarehouseInbound from './card/WarehouseInbound.vue';
  import LargeItemInbound from './card/LargeItemInbound.vue';
  import WarehouseOperateRate from './card/WarehouseOperateRate.vue';
  import StatusOfSmallItems from './card/StatusOfSmallItems.vue';
  import { Button } from 'ant-design-vue';
  import Icon from '/@/components/Icon/Icon.vue';
  import type { SelectProps } from 'ant-design-vue';
  import InterfaceStatus from './card/InterfaceStatus.vue';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useLocale } from '/@/locales/useLocale';
  import { useRoute } from 'vue-router'
  const route = useRoute();
  const { changeLocale } = useLocale();
  const { t, locale } = useI18n();

  const previousLocale = ref();
  const shipmentStatusLabel = computed(() => t('label.shipment_status'));
  const alarmDetailLabel = computed(() => t('label.alarm_detail'));
  const iconStyleRef =ref({ width: '0.75rem' });

  const koLocaleRef = ref('ko-KR');
  const isKorean = computed(() => locale.value === koLocaleRef.value);


  const [registerShipmentStatusPopup, { openModal: openShipmentStatusPopup }] = useModal();
  const [registerAlarmDetailPopup, { openModal: openAlarmDetailPopup }] = useModal();


  let selectOptionsRef = ref<SelectProps['options']>([]);

  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const chooseDateRef: any = ref(`${year}-${month}-${day}`);
  const sizeRef = ref<SelectProps['size']>('small');

  const currentTime = ref();
  const loading = ref(true);
  const lastDaysRef = ref();
  const inboundQualtyStatusRef: Record<string, any> = ref(null);
  const warehouseInboundRef: Record<string, any> = ref(null);
  const sameDayInspectionRef:Record<string,any>=ref(null);
  const warehouseOperateRateRef: Record<string, any> = ref(null);
  const largeItemInboundRef: Record<string, any> = ref(null);
  const interfaceStatusRef: Record<string, any> = ref(null);
  const statusOfSmallItemsRef: Record<string, any> = ref(null);
  
  const statusOfFacilityRef: Record<string, any> = ref(null);

  const volumeOfGoodsStatusRef: Record<string, any> = ref(null);
  const shipmentStatusPopupRef: Record<string, any> = ref(null);
  const alarmDetailPopupRef: Record<string, any> = ref(null);

  setTimeout(() => {
    loading.value = false;
  }, 1500);

  onMounted(async () => {
    //메뉴에 있는 대시보드로 진입 시 다국어 -> 영어로 셋팅
    //path:/dx-viewer/dashboard_en 이면
    if(route.path=='/dx-viewer/dashboard_en'){
    const localeStore = useLocaleStoreWithOut();
    const getLocale = localeStore.getLocale;
    previousLocale.value = getLocale;
    if(previousLocale.value!=='en-US'){
      await changeLocale('en-US');
    }
    }

    await initData();
    setInterval(updateTime, 1000);
  });

  onUnmounted(() => {
    if (previousLocale.value && previousLocale.value !== 'en-US') {
    changeLocale(previousLocale.value)
  }
});


  async function initData() {
    loading.value = true;
  }

  const buttonStyle =computed(()=>{
  return {width: locale.value==koLocaleRef.value?'5rem':'8rem', height: '1.5rem', fontSize: '0.8rem', padding: '0px' 
}
})


  /**
   * 창녕센터 종합 현황판
   *
   */
  function updateTime() {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0'); // 월은 0부터 시작하므로 +1
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    // YYYY-MM-DD HH:MM:SS 형식으로 포맷
    currentTime.value = `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  }

  function changeValue() {
    // await dataBinding();
    setInterval(updateTime, 1000);
  }

  /**
   * 물동량 현황 -> 최근 10일 쪽 select-box 값이 변할 시
   * @param e select-box로 받아온 value 값
   */
  async function lastDaysChanged(e: number) {
    lastDaysRef.value = e;
  }

  //데이터바인딩
  async function dataBinding() {
    let param = {
      chooseDate: chooseDateRef.value,
      inputData: lastDaysRef.value,
    };
    try {

      let response = await getCommonGetApi('/dashboard/7/main', param);

      //select-box 셋팅 (대시보드 우측 상단 일자 데이터)
      settingSelectBox(response);

      //response -> ref변수에 값 바인딩
      dataBindingResponseDataToRef(response);
    } catch (Error) {
      console.log(Error);
    }
  }

  /**
   * response 객체 각 할당된 ref 변수에 바인딩
   * @param response 백엔드 응답 객체
   */
  function dataBindingResponseDataToRef(response: any) {
    inboundQualtyStatusRef.value = response.inbound_quality_status;
    warehouseInboundRef.value = response.warehouse_inbound;
    sameDayInspectionRef.value = response.same_day_inspection;
    warehouseOperateRateRef.value = response.warehouse_operate_rate;
    volumeOfGoodsStatusRef.value = response.volume_of_goods_status;
    statusOfSmallItemsRef.value = response.status_of_small_items;
    statusOfFacilityRef.value = response.status_of_facilities;
    interfaceStatusRef.value =
    response.interface_status && response.interface_status.length > 0
    ? response.interface_status
    : null;
    largeItemInboundRef.value = response.large_item_inbound;
  }

  /**
   * select box 셋팅
   * @param response 
   */
  function settingSelectBox(response: any) {
    let array: any = [];
    response.from_today_to7_days_ago.forEach((data) => {
      array.push({
        value: data,
        label: data,
      });
      selectOptionsRef.value = array;
    });
  }

  /**
   * 알람 상세 팝업 
   */
  const alarmDetailPopup = async () => {
    let param = {
      chooseDate: chooseDateRef.value,
    };

    let response = await getCommonGetApi('/dashboard/7/popup/alarmDetailPopup', param);

    if (!response) {
      openAlarmDetailPopup(true, []);
    }
    openAlarmDetailPopup(true, response);
  };

  /**
   * 입하 현황 팝업 
   */
  const shipmentStatusPopup = async () => {
    let param = {
      chooseDate: chooseDateRef.value,
    };

    let response = await getCommonGetApi('/dashboard/7/popup/shipmentStatusPopup', param);
    if (!response) {
      openShipmentStatusPopup(true, []);
    }
    openShipmentStatusPopup(true, response);
  };
</script>

<style scoped>
  .box {
    /* flex: 1;  */
    border: 2px solid black;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 1.5rem;
    background-color: #e0e0e0;
  }

  .page-background {
    /* background:  var(--sider-dark-bg-color); */
    background: #00386C;
    margin-top: inherit;
  }
</style>
