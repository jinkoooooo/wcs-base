// WCS 실시간 STOMP 클라이언트 (tspg_4way 공용).
// 백엔드 /ws/shuttle 에 연결해 /topic/wcs/** 토픽을 구독한다.
// @stomp/stompjs 내장 재연결 사용 — onConnect 에서 구독 복원.

import SockJS from 'sockjs-client/dist/sockjs';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

// 백엔드 WS 엔드포인트 (WAS 9500). 대시보드 클라이언트와 동일.
const WS_URL = 'http://localhost:9500/ws/shuttle';

type Handler = (body: any) => void;

interface SubInfo {
  topic: string;
  handler: Handler;
  sub: StompSubscription | null;
}

// 단일 연결을 공유하는 경량 클라이언트.
class WcsRealtimeClient {
  private client: Client | null = null;
  private subs = new Map<string, SubInfo>();

  // 최초 구독 시 연결 생성. 이미 있으면 재사용.
  private ensureClient(): void {
    if (this.client) return;
    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      // 재연결 포함 — 연결 시 등록된 모든 토픽 재구독
      onConnect: () => {
        this.subs.forEach((info) => {
          info.sub = this.client!.subscribe(info.topic, (m: IMessage) => this.dispatch(info, m));
        });
      },
    });
    this.client.activate();
  }

  // 수신 메시지 파싱 후 핸들러 전달. 파싱 실패는 무시.
  private dispatch(info: SubInfo, message: IMessage): void {
    try {
      info.handler(JSON.parse(message.body));
    } catch {
      /* 파싱 실패 — 다음 메시지 대기 */
    }
  }

  // 토픽 구독. 연결 전이면 onConnect 에서 복원.
  subscribe(topic: string, handler: Handler): void {
    this.ensureClient();
    const info: SubInfo = { topic, handler, sub: null };
    if (this.client?.connected) {
      info.sub = this.client.subscribe(topic, (m) => this.dispatch(info, m));
    }
    this.subs.set(topic, info);
  }

  // 구독 해제. 마지막 구독이 사라지면 연결 종료.
  unsubscribe(topic: string): void {
    const info = this.subs.get(topic);
    info?.sub?.unsubscribe();
    this.subs.delete(topic);
    if (this.subs.size === 0 && this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }
}

let instance: WcsRealtimeClient | null = null;

// 공용 싱글톤 반환.
export function getWcsRealtimeClient(): WcsRealtimeClient {
  if (!instance) instance = new WcsRealtimeClient();
  return instance;
}
