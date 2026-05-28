/**
 * 재입고 대기 파렛트 알람 composable.
 *
 * STOMP 브로드캐스트만 사용 — 폴링 없음. ReinboundAlarmBroadcaster 가 5초마다
 * /topic/wcs/GLOBAL/reinbound-alarm 으로 현재 목록(+경과 elapsed_min)을 push. 프론트는 받기만.
 * WS 일시 단절은 SockJS 자동 재연결 + onConnect 구독 복원으로 자가 복구.
 * 초기 1회 REST 로 첫 화면을 채운다(첫 브로드캐스트 전 즉시 표시용).
 *
 * 경과(elapsed_min)·intervalMin 은 백엔드 페이로드 값을 그대로 사용.
 * due/소리/ack 은 데이터 수신 시점에 평가 — 백엔드 push 주기가 곧 재알람 주기.
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { playSound } from '@/utils/sound';
import { palletApi } from '../api';
import { getWcsRealtimeClient } from '/@/views/tspg_4way/common/wcsRealtimeClient';

/** 재입고 알람 브로드캐스트 토픽 (GLOBAL). */
const REINBOUND_TOPIC = '/topic/wcs/GLOBAL/reinbound-alarm';

/** 설정값 미존재·이상치 fallback (분). 백엔드 기본값과 일치. */
const DEFAULT_INTERVAL_MIN = 20;

export interface FollowUpAlarmRow {
  pallet_barcode: string;
  host_order_key?: string;
  eq_group_id?: string;
  sub_order_type?: string;
  follow_up_since?: string;
  elapsed_min?: number;
}

export function useReinboundAlarm() {
  const intervalMin = ref(DEFAULT_INTERVAL_MIN);
  const pendingPallets = ref<FollowUpAlarmRow[]>([]);
  const ringing = ref(false);

  // 파렛트별 ack 한 경계 인덱스 — dismiss 시점 기록.
  const ackedBoundary = new Map<string, number>();

  // 백엔드가 내려준 경과(분). 없으면 0.
  function elapsedMinOf(row: FollowUpAlarmRow): number {
    return Math.max(0, Number(row.elapsed_min ?? 0));
  }

  function boundaryOf(row: FollowUpAlarmRow): number {
    const n = intervalMin.value > 0 ? intervalMin.value : DEFAULT_INTERVAL_MIN;
    return Math.floor(elapsedMinOf(row) / n);
  }

  // 배너/모달 표시용 가공 목록 — 경과(분) + due 플래그.
  const rows = computed(() =>
    pendingPallets.value.map((r) => ({
      ...r,
      elapsedMin: Math.floor(elapsedMinOf(r)),
      due: boundaryOf(r) >= 1,
    })),
  );

  // 배너 카운트 — due(경과 ≥ intervalMin) 파렛트 수.
  const dueCount = computed(() => rows.value.filter((r) => r.due).length);

  // 울려야 하는 파렛트 — due 이면서 현재 경계가 ack 경계보다 큰 것.
  function ringingPallets(): FollowUpAlarmRow[] {
    return pendingPallets.value.filter((r) => {
      const b = boundaryOf(r);
      return b >= 1 && b > (ackedBoundary.get(r.pallet_barcode) ?? 0);
    });
  }

  // 수신 데이터 반영 — 간격 + 대기 목록 + 사라진 파렛트 ack 정리 + 소리 평가.
  // 응답 키: REST=interval_min(snake), WS=intervalMin(camel). 둘 다 허용.
  function applyData(intervalRaw: any, palletsRaw: any) {
    const n = Number(intervalRaw);
    intervalMin.value = n > 0 ? n : DEFAULT_INTERVAL_MIN;
    pendingPallets.value = Array.isArray(palletsRaw) ? palletsRaw : [];

    const alive = new Set(pendingPallets.value.map((r) => r.pallet_barcode));
    for (const key of [...ackedBoundary.keys()]) {
      if (!alive.has(key)) ackedBoundary.delete(key);
    }

    // due + 미ack 파렛트가 있으면 알림음 (push 주기마다 반복).
    ringing.value = ringingPallets().length > 0;
    if (ringing.value) playSound('error');
  }

  // 초기 1회 REST 조회 — 첫 화면용.
  async function poll() {
    try {
      const res: any = await palletApi.followUpAlarms();
      applyData(res?.interval_min, res?.pallets);
    } catch {
      /* 조회 실패 — WS push 로 갱신 대기 */
    }
  }

  // 알람 해제 — 현재 울리는 파렛트의 경계를 ack. 다음 경계에서 다시 울림.
  function dismiss() {
    for (const r of ringingPallets()) {
      ackedBoundary.set(r.pallet_barcode, boundaryOf(r));
    }
    ringing.value = false;
  }

  onMounted(() => {
    poll();
    // 백엔드 주기 push 구독 — 목록·경과 갱신. WS 단절은 자동 재연결로 자가 복구.
    getWcsRealtimeClient().subscribe(REINBOUND_TOPIC, (msg: any) => {
      applyData(msg?.intervalMin ?? msg?.interval_min, msg?.pallets);
    });
  });

  onBeforeUnmount(() => {
    getWcsRealtimeClient().unsubscribe(REINBOUND_TOPIC);
  });

  return { intervalMin, rows, dueCount, ringing, dismiss, refresh: poll };
}
