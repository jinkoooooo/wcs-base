<template>
  <div class="bg-slate-50 min-h-screen px-8 py-8">
    <div class="mx-auto max-w-7xl">

      <!-- KPI -->
      <section class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-5">
        <div
          class="rounded-xl bg-white px-5 py-2 shadow-sm ring-1 ring-slate-200 h-20 flex flex-col justify-center">
          <p class="m-0 mt-1 text-base text-slate-600">
            {{ t("label.total_centers") }}
          </p>
          <p class="m-0 mt-1 text-2xl font-semibold leading-none text-slate-900 tabular-nums">
            {{ centerSummaryList.length }}
          </p>
        </div>

        <div
          class="rounded-xl bg-white px-5 py-2 shadow-sm ring-1 ring-slate-200 h-20 flex flex-col justify-center">
          <p class="m-0 mt-1 text-base text-slate-600">
            {{ t("label.total_abnormal_alarms") }}
          </p>
          <p class="m-0 mt-1 text-2xl font-semibold leading-none tabular-nums">
            {{ totalErrorAlarmCount }}
          </p>
        </div>

        <div
          class="rounded-xl bg-white px-5 py-2 shadow-sm ring-1 ring-slate-200 h-20 flex flex-col justify-center">
          <p class="m-0 mt-1 text-base text-slate-600">
            {{ t("label.total_operating_equip") }}
          </p>
          <p class="m-0 mt-1 text-2xl font-semibold leading-none tabular-nums">
            {{ totalRunningEquip }}
          </p>
        </div>

        <div
          class="rounded-xl bg-white px-5 py-2 shadow-sm ring-1 ring-slate-200 h-20 flex flex-col justify-center">
          <p class="m-0 mt-1 text-base text-slate-600">
            {{ t("label.avg_operation_rate") }} <span
            class="text-xs">{{ t("text.avg_operation_rate_desc") }}</span>
          </p>
          <p class="m-0 mt-1 text-2xl font-semibold leading-none text-slate-900 tabular-nums">
            {{ avgUtilizationRate }}%
          </p>
        </div>
      </section>

      <!-- 센터 별 카드 -->
      <div class="grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
        <article
          v-for="center in centerSummaryList"
          :key="center.id"
          class="group flex flex-col rounded-2xl p-6 shadow-sm ring-1 transition"
          :class="[
            center.alarmError > 0
              ? 'bg-red-50 ring-red-200'
              : 'bg-white ring-slate-200'
          ]"
        >
          <!-- 1. 센터명, 센터코드, 마지막업데이트 -->
          <header class="flex items-start justify-between mb-5">
            <div>
              <h2 class="text-lg font-semibold text-slate-900">
                {{ center.lcNm }}
              </h2>
              <p class="text-sm text-slate-400 m-0">
                {{ t("label.lc_id") }}: {{ center.lcId }}
              </p>
              <p class="text-sm text-slate-400 m-0">
                {{ t("label.last_updated") }}: {{ center.updatedAt }}
              </p>
            </div>
          </header>

          <!-- 2. 상태 요약 -->
          <section class="grid grid-cols-2 gap-6 text-base divide-x divide-slate-200"
                   :class="center.alarmError > 0 ? 'bg-red-50 rounded-lg' : ''"
          >

            <!-- 좌측: 설비 상태 -->
            <div class="space-y-4 px-3">
              <p class="text-sm font-semibold text-slate-800 uppercase tracking-wide">
                {{ t("label.facility_status") }}</p>

              <div class="flex justify-between px-3">
                <span class="text-slate-600">{{ t("label.equip_running") }}</span>
                <span class="font-semibold">{{ center.runningEquipCnt }}</span>
              </div>

              <div class="flex justify-between px-3">
                <span class="text-slate-600">{{ t("label.stopped") }}</span>
                <span class="font-semibold"
                      :class="center.stoppedEquipCnt > 0 ? 'text-amber-600' : 'text-slate-700'">
                    {{ center.stoppedEquipCnt }}
                </span>
              </div>

              <div class="flex justify-between px-3">
                <span class="text-slate-600">{{ t("label.fault") }}</span>
                <span class="font-semibold"
                      :class="center.errorEquipCnt > 0 ? 'text-red-600' : 'text-slate-700'">
                    {{ center.errorEquipCnt }}
                </span>
              </div>
            </div>

            <!-- 우측 : 알람 상세 -->
            <div class="space-y-4 px-3">
              <p class="text-sm font-semibold text-slate-800 uppercase tracking-wide">
                {{ t("label.alarm_status") }}
              </p>

              <div class="flex justify-between px-3">
                <span class="text-slate-600">INFO</span>
                <span class="font-semibold text-slate-700">{{ center.alarmInfo }}</span>
              </div>

              <div class="flex justify-between px-3">
                <span class="text-slate-600">WARN</span>
                <span class="font-semibold"
                      :class="center.alarmWarn > 0 ? 'text-amber-600' : 'text-slate-700'">
                  {{ center.alarmWarn }}
                </span>
              </div>

              <div class="flex justify-between px-3">
                <span class="text-slate-600">ERROR</span>
                <span
                  class="font-semibold"
                  :class="center.alarmError > 0 ? 'text-red-600' : 'text-slate-700'">
                  {{ center.alarmError }}
                </span>
              </div>
            </div>
          </section>

          <!-- 3. 버튼: 모니터링/2D/3D 화면 이동 -->
          <div class="mt-6 flex gap-2">
            <Button class="flex-1" type="primary" @click="goDashboard(center)">
              {{ t("button.monitoring") }}
            </Button>

            <Button v-if="isAdmin" class="btn-outline flex-1" @click="go2dLayout(center)">2D</Button>

            <Button v-if="isAdmin" class="btn-outline flex-1" disabled>3D</Button>
          </div>
        </article>

        <!-- 빈 화면: 등록된 센터 없을 때 -->
        <p
          v-if="!centerSummaryList.length"
          class="col-span-full text-center text-slate-500 mt-16">
          {{ t("text.empty_center_list") }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  /**
   * TODO List
   * - 운영 오더 조회
   * - 3D Layout버튼 사용 검토
   *
   * NOTE
   * - 연관 알람, 연관 설비, 센터코드 테이블의 lc_id 미일치로 lc_id 하드코딩으로 연결: lcIdMatchingMap
   */
  import { computed, nextTick, onMounted, onUnmounted } from 'vue';
  import { Button } from 'ant-design-vue';
  import { useRouter } from 'vue-router';
  import { useCenters } from "@/views/lms/monitoring-status/composables/useCenters";
  import { useRefCodes } from "@/views/lms/composables/useRefCodes";
  import { usePermission } from '/@/hooks/web/usePermission';
  import dayjs from 'dayjs'
  import customParseFormat from 'dayjs/plugin/customParseFormat'
  import { useI18n } from "@/hooks/web/useI18n";

  interface CenterSummary {
    id: string;
    lcId: string; // 라인ID
    lcNm: string; // 센터명
    operationOrder: string; // 운영 오더 정보
    alarmInfo: number; // INFO 유형 알람 수
    alarmWarn: number; // WARN 유형 알람 수
    alarmError: number; // ERROR 유형 알람 수
    runningEquipCnt: number; // 사용 중인 설비 수
    stoppedEquipCnt: number; // 미사용 중인 설비 수
    errorEquipCnt: number; // 알람상태 설비 수
    utilizationRate: number; // 가동률 (%)
    updatedAt: string; // 마지막 업데이트 일시
  }

  const { t } = useI18n();
  const router = useRouter();

  // 사용자 권한
  const { hasPermission } = usePermission();
  const isAdmin = computed(() => hasPermission('admin'));

  dayjs.extend(customParseFormat)

  const { clearCenter, loadCenter, centerData } = useCenters('/lms-centers');
  const { equipList, alarmList, centerOptions, loadEquips, loadAlarms, loadCenterOptions, clearOptions } = useRefCodes(false);

  // 속성
  const isTest = false; // 더미데이터 사용

  // TODO: lms_centers, lms_equipment_status_dev, lms_alarm_status_dev 테이블 간 LC_ID 통일
  const lcIdMatchingMap: Record<string, string> = {
    'JNM001': 'KR_KPP_YEOSU',
    'GYG001': 'KR_Daewha',
  }

  // 데이터
  const dummyCenterList: CenterSummary[] = [
    {
      id: 'SEO001',
      lcId: 'testId',
      lcNm: '서울 물류센터',
      operationOrder: '12',
      runningEquipCnt: 48,
      stoppedEquipCnt: 3,
      errorEquipCnt: 1,
      utilizationRate: 89,
      alarmInfo: 0,
      alarmWarn: 0,
      alarmError: 0,
      updatedAt: '-',
    },
    {
      id: 'BUS001',
      lcId: 'testId',
      lcNm: '부산 물류센터',
      operationOrder: '98',
      runningEquipCnt: 36,
      stoppedEquipCnt: 4,
      errorEquipCnt: 0,
      utilizationRate: 82,
      alarmInfo: 0,
      alarmWarn: 0,
      alarmError: 0,
      updatedAt: '-',
    },
    {
      id: 'DAE001',
      lcId: 'testId',
      lcNm: '대구 물류센터',
      operationOrder: '76',
      runningEquipCnt: 29,
      stoppedEquipCnt: 2,
      errorEquipCnt: 2,
      utilizationRate: 75,
      alarmInfo: 0,
      alarmWarn: 0,
      alarmError: 0,
      updatedAt: '-',
    },
  ]
  const centerSummaryList = computed<CenterSummary[]>(() => {
    if (isTest) {
      return dummyCenterList;
    }

    // 관리자가 아닌 경우 사용자 소속 센터만 표시
    const userCenterIds = (centerOptions.value ?? []).map((option) => option.value);
    const filteredCenters = isAdmin.value
      ? centerData.value
      : centerData.value.filter((center) => userCenterIds.includes(String(center.lc_id)));

    return filteredCenters.map((center) => {
      const { id, lc_id, lc_nm } = center;

      // 센터별 설비 집계
      const centerEquips = equipList.value.filter((equip) => equip.lc_id === lc_id || equip.lc_id === lcIdMatchingMap[lc_id]);
      const totalEquipCnt = centerEquips.length;
      const runningEquipCnt = centerEquips.filter((equip) => equip.current_status === 'RUN' || equip.current_status === 'WAITING').length;
      const stoppedEquipCnt = centerEquips.filter((equip) => equip.current_status === 'STOP').length;
      const errorEquipCnt = centerEquips.filter((equip) => (equip.err_cnt ?? 0) > 0).length;
      const utilizationRate = totalEquipCnt > 0
        ? Math.round((runningEquipCnt / totalEquipCnt) * 100)
        : 0;

      // 센터별 알람 집계
      const centerAlarms = alarmList.value.filter((alarm) => alarm.lc_id === lc_id || alarm.lc_id === lcIdMatchingMap[lc_id]);
      const alarmInfo = centerAlarms.filter((alarm) => alarm.alarm_type === 'INFO').length;
      const alarmWarn = centerAlarms.filter((alarm) => alarm.alarm_type === 'WARN').length;
      const alarmError = centerAlarms.filter((alarm) => alarm.alarm_type === 'ERROR').length;

      // 업데이트 시각
      const updatedAt = (() => {
        const times = [...centerEquips, ...centerAlarms]
          .map(item => item.updated_at)
          .filter(Boolean)

        if (!times.length) return '-'

        const maxTime = Math.max(...times.map(t => dayjs(t, 'YYYY-MM-DD HH:mm:ss').valueOf()))
        return dayjs(maxTime).format('YYYY-MM-DD HH:mm:ss')
      })()

      return {
        id,
        lcId: lc_id,
        lcNm: lc_nm,
        operationOrder: '-', // todo: 별도 데이터 연동 필요
        runningEquipCnt,
        stoppedEquipCnt,
        errorEquipCnt,
        utilizationRate,
        alarmInfo,
        alarmWarn,
        alarmError,
        updatedAt,
      } as CenterSummary;
    })
  })

  /**
   * 이벤트 핸들러
   */
  // Dashboard 페이지 이동
  function goDashboard(data: CenterSummary) {
    router.push({
      name: 'EquipmentStatus',
    });
  }

  // 2D Layout 페이지 이동
  function go2dLayout(data: CenterSummary) {
    router.push({
      name: 'LayoutViewer2D',
      params: { lcId: data.lcId }
    });
  }

  // 3D Layout 페이지 이동
  function go3dLayout(data: CenterSummary) {
    router.push({
      name: 'StatusBoardDmi2D', // TODO: 3D Layout 화면으로 변경
      // params: {lcId: data.lcId}
    });
  }

  /**
   * 스타일
   */
    // 총 이상 알람
  const totalErrorAlarmCount = computed(() =>
      centerSummaryList.value.reduce(
        (sum, center) => sum + center.alarmError,
        0
      )
    )

  // 가동 설비 합계
  const totalRunningEquip = computed(() =>
    centerSummaryList.value.reduce(
      (sum, center) => sum + center.runningEquipCnt,
      0
    )
  )

  // 평균 가동률
  const avgUtilizationRate = computed(() => {
    if (!centerSummaryList.value.length) return 0
    const total = centerSummaryList.value.reduce(
      (sum, center) => sum + center.utilizationRate,
      0
    )
    return Math.round(total / centerSummaryList.value.length)
  })


  /**
   * 라이프 사이클
   */
  onMounted(async () => {
    await nextTick();
    clearCenter();
    clearOptions();
    await Promise.allSettled([loadCenter(), loadCenterOptions(), loadEquips(), loadAlarms()]);
  })

  onUnmounted(() => {
    clearCenter();
    clearOptions();
  })
</script>

<style scoped>
</style>
