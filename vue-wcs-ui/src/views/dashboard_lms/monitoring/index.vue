<template>
  <PageWrapper
    :contentFullHeight="true"
    :contentClass="'p-0'"
    class="overflow=hidden- page-background"
  >
    <div class="flex flex-col h-full">
      <!-- 상단 영역 : 좌측 라벨+시간 / 우측 버튼+날짜선택 -->
      <div class="flex items-center justify-between pb-2">
        <!-- 왼쪽 섹션 -->
        <div class="flex items-center">
          <span class="left-title">{{ t('label.IntegratedMonitoringSystem') }}</span>
          <span class="left-title pl-6">
            {{ currentTime }}
          </span>
        </div>

        <!-- 오른족 섹션 -->
        <div class="flex items-center space-x-2"></div>
      </div>

      <!-- 하단: 지도 + 센터목록 -->
      <div class="flex-1 pb-4">
        <div class="relative w-full h-full min-h-[80vh]" style="min-width: min(1600px, 95vw)">
          <!-- 지도 -->
          <div
            ref="mapEl"
            class="w-full h-full min-h-[80vh]"
            style="min-width: min(1600px, 95vw)"
          ></div>

          <!-- 에러 상태인 센터 리스트 -->
          <div
            class="err-container w-[20rem] max-h-[60vh] bg-white/80 border-red-100 absolute top-2 left-2 px-2 py-1 rounded-lg shadow-md overflow-y-auto border"
          >
            <div class="border-red-100 flex items-center justify-between px-3 py-2 border-b">
              <span class="err-container-title-l">에러 센터 {{ errLcList.length }}곳</span>
              <span class="err-container-title-r">실시간 기준</span>
            </div>

            <ul v-if="errLcList.length" class="p-2">
              <li
                v-for="it in errLcList"
                :key="it.lc_id"
                class="err-card leading-5 px-2 py-2 border-b border-slate-300 last:border-b-0"
                style="list-style-type: none"
              >
                <!-- 센터명 + 상태 -->
                <div class="flex items-center justify-between">
                  <span class="err-card-row1-l truncate" :title="it.lc_nm">{{ it.lc_nm }}</span>
                  <span
                    class="err-card-row1-r inline-flex items-center justify-center gap-1 rounded-full py-[1px] px-2"
                    :class="
                      it.status === LC_STATUS_CONST.ERROR
                        ? 'bg-red-100 text-red-700'
                        : 'bg-neutral-400 text-gray-50'
                    "
                  >
                    {{ it.status }}
                    <Badge :status="getBadgeData(it.status)" :offset="[3, 0]" dot></Badge>
                  </span>
                </div>

                <!-- 상세 정보 -->
                <div class="mt-1 grid grid-cols-2 gap-x-2 gap-y-[2px]">
                  <span class="err-card-row2-l mb-1">센터 ID</span>
                  <span class="err-card-row2-r font-semibold text-right text-gray-500">{{
                    it.lc_id
                  }}</span>

                  <span class="err-card-row2-l mb-1">엣지 서버</span>
                  <span
                    :class="[
                      'err-card-row2-r font-semibold text-right',
                      it.edgeStatus === 'ON' ? 'text-gray-500' : 'text-red-600',
                    ]"
                  >
                    {{ it.edgeStatus }}
                  </span>

                  <span class="err-card-row2-l mb-1">에러 알람</span>
                  <span
                    class="err-card-row2-r font-semibold text-right"
                    :class="it.alarm?.error ?? 0 ? 'text-red-600' : 'text-gray-500'"
                    >{{ it.alarm?.error ?? 0 }}</span
                  >
                </div>
              </li>
            </ul>
            <!-- 지도 내 미표기 센터 리스트 -->
            <div
              v-if="unAddressedLcList.length"
              class="border-slate-300 mt-2 border-t border-dashed pt-2"
            >
              <!-- 접기/펼치기 헤더 -->
              <button
                type="button"
                class="unaddr-btn flex w-full items-center justify-between bg-white/80 rounded-md"
                @click="toggleUnAddressedLcList"
              >
                <span>지도 내 미표기 센터 ({{ unAddressedLcList.length }}곳)</span>
                <span>
                  {{ showUnAddressedLcList ? '접기 ▲' : '펼치기 ▼' }}
                </span>
              </button>

              <!-- 목록 -->
              <transition name="fade">
                <ul
                  v-if="showUnAddressedLcList"
                  class="mt-1 max-h-[24vh] space-y-1 overflow-y-auto px-1"
                >
                  <li
                    v-for="it in unAddressedLcList"
                    :key="it.lc_id + '-addr'"
                    class="leading-5 px-2 py-1 border-b border-slate-200 last:border-b-0"
                    style="list-style-type: none"
                  >
                    <div class="flex items-center justify-between">
                      <span class="unaddr-card-row1-l truncate" :title="it.lc_nm">
                        {{ it.lc_nm }}
                      </span>
                      <span class="unaddr-card-row1-r">
                        {{ it.address ? '좌표 변환 실패' : '주소 미등록' }}
                      </span>
                    </div>

                    <div class="flex items-center justify-between">
                      <span class="unaddr-card-row1-l truncate" :title="it.lc_nm"> 주소 </span>
                      <span class="unaddr-card-row2-r">
                        {{ it.address || '등록된 주소 없음' }}
                      </span>
                    </div>

                    <!-- 주소 텍스트 -->
                    <div class="unaddr-card-row2-r mt-[2px] text-right"></div>
                  </li>
                </ul>
              </transition>
            </div>
          </div>
        </div>
      </div>
    </div>
  </PageWrapper>
</template>
<script lang="ts" setup>
  /**
   * Note
   *
   * 로직
   * 1. 센터정보 조회 (fetchCenterInfo())
   * 2. edge서버 상태, 알람건수 기반 센터 상태 결정 (deriveStatus())
   * 3. ERROR/DISCONNECTED 상태 센터 목록 업데이트 (errLcList)
   * 4. 센터별 위도/경도 변환
   * 5. 위/경도 변환 실패한 센터 목록 업데이트 (unAddressedLcList)
   * 6. 지도에 센터별 위치 표시
   * 7. 지도에 센터별 정보창 표시
   *
   * 설정
   * - 에러상태 설비목록 테스트데이터 추가 여부 (isTest)
   *
   */
  import { computed, nextTick, onBeforeUnmount, onMounted, onUnmounted, ref } from 'vue';
  import { Badge, Button, notification, type SelectProps } from 'ant-design-vue';
  import { PageWrapper } from '/@/components/Page';
  import { getCommonGetApi } from '/@/api/common/api';
  import { useLocaleStoreWithOut } from '/@/store/modules/locale';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useLocale } from '/@/locales/useLocale';
  import { useRoute, useRouter } from 'vue-router';
  import {
    createCustomOverlay,
    createMarker,
    getGeocodeList,
    loadMap,
    renderInfoContent,
    setLatLngList,
  } from '@/views/dashboard_lms/monitoring/utils/maps';
  import {
    CenterMetaType,
    LC_STATUS_CONST,
    LcStatusType,
  } from '@/views/dashboard_lms/monitoring/types';
  import dayjs from 'dayjs';

  const route = useRoute();
  const router = useRouter();
  const { changeLocale } = useLocale();
  const { t, locale } = useI18n();

  const previousLocale = ref();
  const koLocaleRef = ref('ko-KR');
  const isKorean = computed(() => locale.value === koLocaleRef.value);

  const isTest = ref<boolean>(false); // 테스트 데이터 추가여부

  let selectOptionsRef = ref<SelectProps['options']>([]);
  const errLcList = ref<CenterMetaType[]>([]); // ERROR상태인 센터 목록
  const unAddressedLcList = ref<CenterMetaType[]>([]); // 주소 미표시된 센터 목록
  const showUnAddressedLcList = ref<boolean>(false); // 주소 미표기 센터 접기/펼치기 상태 관리

  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const chooseDateRef: any = ref(`${year}-${month}-${day}`);
  const sizeRef = ref<SelectProps['size']>('small');

  const currentTime = ref();
  const loading = ref(true);
  const lastDaysRef = ref();

  // 지도
  const mapEl = ref<HTMLDivElement | null>(null);
  const OVERLAY_VISIBLE_ZOOM = 9; // 센터별 정보창이 보이는 확대 정도
  const MAX_INIT_ZOOM = 8; // 처음 로딩 시 최대 확대 정도
  let mapRef: any = null; // map instance
  let infoWindows: any[] = [];
  let markers: any[] = [];

  // '상세보기' 버튼 클릭 시 알람목록 페이지로 이동
  function goToAlarmPg(centerId: string) {
    router.push({
      name: 'AlarmStatus',
    });
  }

  onMounted(async () => {
    //메뉴에 있는 대시보드로 진입 시 다국어 -> 영어로 셋팅
    //path:/dx-viewer/dashboard_en 이면
    if (route.path == '/dx-viewer/dashboard_en') {
      const localeStore = useLocaleStoreWithOut();
      const getLocale = localeStore.getLocale;
      previousLocale.value = getLocale;
      if (previousLocale.value !== 'en-US') {
        await changeLocale('en-US');
      }
    }
    setInterval(updateTime, 1000);

    await initData();
  });

  onUnmounted(() => {
    if (previousLocale.value && previousLocale.value !== 'en-US') {
      changeLocale(previousLocale.value);
    }

    // 정보창 정리
    infoWindows.forEach((iw) => iw.close());
    infoWindows = [];
  });

  onBeforeUnmount(() => {
    for (const marker of markers) {
      marker.__overlay?.setMap(null);
      marker.setMap(null);
    }
    markers.length = 0;
  });

  async function initData() {
    loading.value = true;

    // 지도 로드
    await loadMap();
    const { naver } = window as any;

    if (!mapEl.value) return;

    // 1. 지도 생성
    const initLatLngPos = new naver.maps.LatLng(36.215, 127.432); // 초기 중심
    mapRef = new naver.maps.Map(mapEl.value, {
      center: initLatLngPos,
      zoom: MAX_INIT_ZOOM,
    });

    // 지도 이벤트 등록
    mapEl.value.addEventListener('click', (event: MouseEvent) => {
      const target = event.target as HTMLElement;

      if (target.classList.contains('goto-detail-btn')) {
        // '상세 보기' 버튼 액션 : 알람목록 페이지 이동
        const centerId = target.dataset.centerId; // 버튼 속성 data-center-id 값 추출
        if (centerId) {
          goToAlarmPg(centerId);
        }
      }
    });

    // 2. 센터 조회 및 메타정보 가공
    const centers = await fetchCenterInfo();
    if (!centers.length) {
      loading.value = false;
      return;
    }

    // 3. 초기 지도영역 지정
    const bounds = new naver.maps.LatLngBounds();
    // new naver.maps.LatLng(33.0, 126.0), // SW: 남서쪽. 제주 아래쪽
    // new naver.maps.LatLng(37.8, 127.5), // NE: 남서쪽. 서울 위쪽

    const centerMetaList = setLatLngList(centers);

    // 4. 센터 표기 및 지도영역 재설정
    markers = centerMetaList.map(({ raw, posLatLng }, index) => {
      const newMarker = createMarker(mapRef, posLatLng, raw); // 센터별 마커 생성
      const side = index % 2 === 0 ? 'left' : 'right'; // 마커기준 overlay 정보창 위치
      const overlay = createCustomOverlay(mapRef, posLatLng, renderInfoContent(raw), {
        side,
        lineColor: '#00386c',
      }); // 센터별 정보창 생성
      (newMarker as any).__overlay = overlay;
      bounds.extend(posLatLng); // 센터위치를 기반으로 지도 경계 확장
      // NOTE: 필요 시 이벤트 추가
      return newMarker;
    });

    // 5. 변경된 지도영역 반영 - 좌표경계(bounds)를 포함하는 위치로 지도 이동
    if (centers.length > 1) {
      mapRef.fitBounds(bounds);

      // 최대 줌 제한
      if (mapRef.getZoom() > MAX_INIT_ZOOM) {
        mapRef.setZoom(MAX_INIT_ZOOM);
      }
    } else if (centers.length === 1) {
      mapRef.setCenter(new naver.maps.LatLng(centers[0].lat, centers[0].lng)); // 또는 mapRef.setCenter({lat: centers[0].lat, lng: centers[0].lng});
    }

    // 6. 현재 줌 기준으로 초기 정보창 표시/숨김
    updateOverlayVisibility();

    // 7. 지도 이벤트 추가
    naver.maps.Event.addListener(mapRef, 'zoom_changed', updateOverlayVisibility);

    // 8. 레이아웃 재계산
    await nextTick();
    naver.maps.Event.trigger(mapRef, 'resize');

    loading.value = false;
  }

  /**
   * 센터 정보 조회 및 Geocdoe 변환
   */
  async function fetchCenterInfo(): Promise<CenterMetaType[]> {
    try {
      // 1. 센터목록 조회
      // const response = await getCommonGetApi('/lms-centers', {});
      // const centerListRaw: any[] = Array.isArray(response)
      //   ? response.flatMap((it) => it?.items ?? [])
      //   : response?.items ?? [];
      const response = await getCommonGetApi('/lms-centers', {});
      const centerListRaw: any[] = response.items;
      // console.log('[fetchCenterInfo] 센터목록 조회 centerListRaw = ', centerListRaw);

      // 2. 센터목록 데이터 가공
      let centerMetaList = centerListRaw.map((record) => {
        const infoCnt = Number(record.alarm_info_cnt ?? 0);
        const warnCnt = Number(record.alarm_warning_cnt ?? 0);
        const errorCnt = Number(record.alarm_error_cnt ?? 0);

        const base: CenterMetaType = {
          lc_id: String(record.lc_id ?? ''),
          lc_nm: String(record.lc_nm),
          address: String(record.address_plain).trim() ?? '',
          // TODO: 쿼리 생성 후, 컬럼명 수정 필요
          edgeStatus: record.edge_server ?? 'OFF', // as 'ON' | 'OFF' | undefined,
          alarm: {
            info: infoCnt,
            warning: warnCnt,
            error: errorCnt,
          },
          lastHeartbeat: record.last_hearbeat ?? '',
        };

        base.status = deriveStatus({ edgeStatus: base.edgeStatus, alarm: base.alarm });
        return base;
      });

      if (isTest.value) {
        centerMetaList = addMockData(centerMetaList);
      }

      // 3. 에러/연결끊김 상태 센터 목록 업데이트
      errLcList.value = centerMetaList.filter((it) =>
        [LC_STATUS_CONST.ERROR, LC_STATUS_CONST.DISCONNECT].includes(it.status),
      );
      // console.log('[fetchCenterInfo] errLcList = ', errLcList.value);

      // 4. 센터주소 경도, 위도 변환 및 예외처리
      const geocodeTargetList = centerMetaList
        .filter((it) => it.address && it.address.trim().length > 0)
        .map((it) => ({
          lc_id: it.lc_id,
          lc_nm: it.lc_nm,
          address: it.address ?? '',
        }));

      if (!geocodeTargetList.length) {
        // 모든 센터가 주소가 없는 경우, 주소 미표기 센터 목록 추가
        unAddressedLcList.value = centerMetaList;
        // console.log('[fetchCenterInfo] unAddressedLcList = ', unAddressedLcList.value);

        notification.warning({
          message: '센터 주소 없음',
          description: '조회 가능한 센터가 없습니다.',
          duration: 2,
        });
        return [];
      }
      // console.log('[fetchCenterInfo] geocodeTargetList = ', geocodeTargetList);

      const geocodeList = await getGeocodeList(geocodeTargetList);

      // 5. 연결끊김, 에러, 주소변환 실패한 센터목록
      const geoSuccessIdSet = new Set(geocodeList.map((it) => it.lc_id)); // 주소변환 성공한 센터코드

      unAddressedLcList.value = centerMetaList.filter((meta) => {
        const hasGeo = geoSuccessIdSet.has(meta.lc_id);
        return !hasGeo;
      }); // 변환된 주소가 없는 센터

      // 6. 경도, 위도가 있는 센터에만 메타정보 추가
      return geocodeList
        .map((geo) => {
          const meta = centerMetaList.find((it) => it.lc_id === geo.lc_id);
          return {
            lc_id: geo.lc_id,
            lc_nm: geo.lc_nm,
            address: geo.address,
            lat: geo.lat,
            lng: geo.lng,
            edgeStatus: meta?.edgeStatus ?? 'OFF', // 메타정보 없을 경우 기본값 부여
            alarm: meta?.alarm ?? { info: 0, warning: 0, error: 0 },
            lastHeartbeat: meta?.lastHeartbeat ?? '',
            status: meta?.status ?? LC_STATUS_CONST.ERROR,
          };
        })
        .sort((a, b) => a.lc_id.localeCompare(b.lc_id));
    } catch (e) {
      // getCommonGetApi 실패시 발생. 지오코드 변환 에러는 maps.ts에서 처리
      notification.error({
        message: '센터 조회 실패',
        description: '센터 데이터를 가져오지 못했습니다.',
        duration: 2,
      });
      // console.log(e);
      return [];
    }
  }

  // 센터 상태 판별
  function deriveStatus(meta: {
    edgeStatus: 'ON' | 'OFF' | undefined;
    alarm: { info: number; warning: number; error: number } | undefined;
  }): LcStatusType {
    const edge = meta.edgeStatus ?? 'OFF';
    const alarm = {
      info: meta.alarm?.info ?? 0,
      warning: meta.alarm?.warning ?? 0,
      error: meta.alarm?.error ?? 0,
    };

    if (edge === 'OFF') return LC_STATUS_CONST.DISCONNECT;
    if ((alarm.error ?? 0) > 0) return LC_STATUS_CONST.ERROR;
    if ((alarm.warning ?? 0) > 0) return LC_STATUS_CONST.WARNING;
    if ((alarm.info ?? 0) > 0 || edge === 'ON') return LC_STATUS_CONST.RUN;
    return LC_STATUS_CONST.UNKNOWN;
  }

  // 줌 레벨에 따른 센터정보창 숨김/표시 처리
  function updateOverlayVisibility() {
    if (!mapRef || !markers) return;

    const zoom = mapRef.getZoom();
    const visible = zoom >= OVERLAY_VISIBLE_ZOOM;

    markers.forEach((marker: any) => {
      const overlay = marker.__overlay;
      if (!overlay || typeof overlay.setMap !== 'function') return;

      if (visible) {
        overlay.setMap(mapRef);
      } else {
        overlay.setMap(null);
      }
    });
  }

  // UI 내 시간 갱신
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
    // inboundQualtyStatusRef.value = response.inbound_quality_status;
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
   * 스타일 세팅
   */
  // badge 색상 변경 - 미사용 색상 processing (blue)
  const getBadgeData = (
    status: LcStatusType | undefined,
  ): 'success' | 'error' | 'default' | 'processing' | 'warning' => {
    if (!status) return 'error';
    if (status === LC_STATUS_CONST.RUN) return 'success'; // green
    if (status === LC_STATUS_CONST.WARNING) return 'warning'; // orange
    if (status === LC_STATUS_CONST.ERROR) return 'error'; // red
    return 'default'; // gray / DISCONNECT, UNKNOWN 상태
  };

  // 주소 미표기 센터목록 접기/펼치기
  function toggleUnAddressedLcList() {
    showUnAddressedLcList.value = !showUnAddressedLcList.value;
  }

  /**
   * 테스트 데이터 추가
   */
  function addMockData(list: CenterMetaType[]): CenterMetaType[] {
    const now = dayjs().toISOString();

    const mocks: CenterMetaType[] = [
      // RUN 상태 (edge ON, info > 0)
      {
        lc_id: 'MOCK_RUN_01',
        lc_nm: '모의센터_RUN_정보1',
        address: '부산광역시 해운대구 센텀중앙로 90',
        edgeStatus: 'ON',
        alarm: { info: 1, warning: 0, error: 0 },
        lastHeartbeat: now,
        status: LC_STATUS_CONST.RUN,
      },
      // WARNING 상태 (warning > 0)
      {
        lc_id: 'MOCK_WARN_01',
        lc_nm: '모의센터_WARNING',
        address: '광주광역시 서구 상무중앙로 75',
        edgeStatus: 'ON',
        alarm: { info: 0, warning: 2, error: 0 },
        lastHeartbeat: now,
        status: LC_STATUS_CONST.WARNING,
      },
      // ERROR 상태 (error > 0)
      {
        lc_id: 'MOCK_ERR_01',
        lc_nm: '모의센터_ERROR',
        address: '대전광역시 유성구 대덕대로 593',
        edgeStatus: 'ON',
        alarm: { info: 0, warning: 0, error: 3 },
        lastHeartbeat: now,
        status: LC_STATUS_CONST.ERROR,
      },
      // DISCONNECT 상태 (edge OFF, 알람 0)
      {
        lc_id: 'MOCK_DISC_01',
        lc_nm: '모의센터_DISCONNECT',
        address: '세종특별자치시 도움3로 20',
        edgeStatus: 'OFF',
        alarm: { info: 0, warning: 0, error: 0 },
        lastHeartbeat: now,
        status: LC_STATUS_CONST.DISCONNECT,
      },
      // UNKNOWN 상태 (edge undefined, 알람 0)
      {
        lc_id: 'MOCK_UNKNOWN_01',
        lc_nm: '모의센터_UNKNOWN',
        address: '강원특별자치도 춘천시 중앙로 55',
        edgeStatus: undefined,
        alarm: { info: 0, warning: 0, error: 0 },
        lastHeartbeat: now,
        status: LC_STATUS_CONST.UNKNOWN,
      },
      // 주소 없는 센터 (geocode 대상에서 제외 → unAddressedLcList 테스트용)
      {
        lc_id: 'MOCK_NOADDR_01',
        lc_nm: '주소없는센터_1',
        address: '', // 전라남도 순천시 팔마로 200
        edgeStatus: 'ON',
        alarm: { info: 0, warning: 0, error: 1 }, // ERROR + 주소 없음
        lastHeartbeat: now,
        status: LC_STATUS_CONST.ERROR,
      },
      // 공백 주소 (trim 후 빈 문자열 → geocode 대상에서 제외)
      {
        lc_id: 'MOCK_NOADDR_02',
        lc_nm: '주소없는센터_2',
        address: '   ', // 경상북도 포항시 남구 동해대로 740
        edgeStatus: 'OFF',
        alarm: { info: 0, warning: 1, error: 0 }, // WARNING + DISCONNECT 조합
        lastHeartbeat: now,
        status: LC_STATUS_CONST.DISCONNECT,
      },
    ];

    // mocks.forEach((it) => {
    //   it.status = deriveStatus({
    //     edgeStatus: it.edgeStatus as 'ON' | 'OFF' | undefined,
    //     alarm: it.alarm,
    //   });
    // });

    // 기존 데이터 + mock 데이터 합쳐서 반환
    return [...list, ...mocks];
  }
</script>

<style scoped>
  /*
    색상, 폰트 위주 스타일
    - "border-[color]-[number]" 형식의 border 색상은 inline-css로 부여
  */

  /* 페이지 전체 */
  .page-background {
    /* background:  var(--sider-dark-bg-color); */
    background: #00386c; /* 남색 */
  }

  /* 상단 제목 */
  .left-title {
    font-size: 1.4rem;
    font-weight: bold;
    color: #fff;
  }

  .right-title {
    font-size: 0.75rem;
  }

  /* 에러 센터 목록 */
  .err-container {
    /* tailwind로 색상 부여 - 배경색 */
  }

  .err-container-title-l {
    font-size: 1rem;
    font-weight: 500;
    color: oklch(57.7% 0.245 27.325); /* text-red-600 */
  }

  .err-container-title-r {
    font-size: 1rem;
    font-weight: 500;
    color: oklch(70.7% 0.022 261.325); /* text-gray-400 */
  }

  /* 에러 센터 */
  .err-card {
    /* tailwind로 색상 부여 - 테두리 색 */
    font-size: 1rem;
  }

  .err-card:hover {
    background-color: #fff;
    opacity: 100%;
  }

  .err-card-row1-l {
    /* tailwind로 색상 부여 - 글자색 */
    font-size: 1rem;
    font-weight: 600;
  }

  .err-card-row1-r {
    /* tailwind로 색상 부여 - 글자색, 배경색 */
    font-size: 0.8rem;
    font-weight: 600;
  }

  .err-card-row2-l {
    font-size: 0.9rem;
    color: oklch(55.1% 0.027 264.364); /* text-gray-500 */
  }

  .err-card-row2-r {
    /* tailwind로 색상 부여 - 글자색 */
    font-size: 1rem;
  }

  /* 지도 내 미표기 센터 */
  .unaddr-btn {
    font-size: 0.9rem;
    font-weight: 500;
  }

  .unaddr-card-row1-l {
    font-size: 0.9rem;
    color: oklch(55.1% 0.027 264.364); /* text-gray-500 */
  }

  .unaddr-card-row1-r {
    font-size: 0.8rem;
    font-weight: 500;
    color: oklch(57.7% 0.245 27.325); /* text-red-600 */
  }

  .unaddr-card-row2-r {
    font-size: 0.8rem;
    color: oklch(57.7% 0.245 27.325); /* text-red-600 */
  }
</style>
