/**
 * websocket.ts
 * 4-Way Shuttle WebSocket STOMP 클라이언트
 *
 * ============================================
 * 파일 개요
 * ============================================
 * 백엔드 서버와의 실시간 통신을 담당하는 WebSocket 클라이언트입니다.
 * STOMP 프로토콜을 사용하여 메시지를 송수신합니다.
 *
 * ============================================
 * 주요 기능
 * ============================================
 *
 * 1. 연결 관리
 *    - SockJS를 통한 WebSocket 연결
 *    - 자동 재연결 (지수 백오프 알고리즘)
 *    - 연결 상태 모니터링 (Heartbeat)
 *
 * 2. 구독 관리
 *    - 토픽별 구독/구독해제
 *    - 재연결 시 구독 자동 복원
 *    - 구독 갱신 (타임아웃 방지)
 *
 * 3. 실시간 데이터 수신
 *    - 셔틀 위치 (500ms 주기)
 *    - 화물 위치 (500ms 주기)
 *    - 랙 재고 상태
 *    - 컨베이어 상태
 *    - WCS 작업 오더
 *
 * ============================================
 * 토픽 구조
 * ============================================
 *
 * 구독 토픽 (Subscribe):
 * - /topic/shuttle/positions/{lcId} : 셔틀 실시간 위치
 * - /topic/shuttle/cargos/{lcId} : 화물 실시간 위치
 * - /topic/shuttle/car/status/{eqGroupId} : 셔틀카 상태
 * - /topic/shuttle/rack/status/{eqGroupId}/{floor} : 랙 재고 상태
 * - /topic/shuttle/cv/status/{eqGroupId} : 컨베이어 상태
 * - /topic/shuttle/wcs/order/{lcId} : WCS 작업 오더
 * - /topic/shuttle/alarm/{lcId} : 알람
 *
 * 발행 토픽 (Publish):
 * - /app/shuttle/dashboard/subscribe/{lcId}/{pageId} : 구독 시작
 * - /app/shuttle/dashboard/unsubscribe/{lcId}/{pageId} : 구독 해제
 * - /app/shuttle/realtime/subscribe/{eqGroupId}/{floor} : 실시간 구독 시작
 *
 * ============================================
 * 재연결 알고리즘 (지수 백오프)
 * ============================================
 *
 * 재연결 시도 간격 = min(baseDelay * 2^attempts, maxDelay)
 *
 * - baseDelay: 1000ms (1초)
 * - maxDelay: 30000ms (30초)
 * - maxAttempts: 10회
 *
 * 예시:
 * - 1회: 1초 후 재시도
 * - 2회: 2초 후 재시도
 * - 3회: 4초 후 재시도
 * - ...
 * - 10회: 30초 후 재시도 (최대)
 *
 * ============================================
 * 사용 예시
 * ============================================
 * ```ts
 * import { getWebSocketClient } from './api/websocket';
 *
 * const ws = getWebSocketClient();
 *
 * // 연결
 * await ws.connect({
 *   onConnect: () => console.log('Connected'),
 *   onDisconnect: () => console.log('Disconnected'),
 *   onError: (err) => console.error('Error:', err)
 * });
 *
 * // 셔틀 위치 구독
 * ws.subscribeShuttlePositions('LC001', (positions) => {
 *   console.log('Shuttle positions:', positions);
 * });
 *
 * // 연결 해제
 * ws.disconnect();
 * ```
 */
import SockJS from 'sockjs-client/dist/sockjs';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import type {
  DashboardEquipmentData,
  AlarmData,
  WebSocketError,
  EquipmentPositionEvent,
  EquipmentMoveEvent,
  BcrScanEvent,
  DashboardShuttleData,
  DashboardCargoData,
  // 실시간 설비 타입
  // Provider-based RealTime DTOs
  RtShuttlePosition,
  RtConveyorStatus,
  RtLifterStatus,
  RtCargoPosition,
  RtJobStatus,
  RtAlarm,
} from './types';
import { keysToCamel } from '../utils/case';

export interface WebSocketOptions {
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: WebSocketError) => void;
  onReconnecting?: (attempt: number, maxAttempts: number) => void;
}

// 구독 정보 저장용 인터페이스
interface SubscriptionInfo {
  topic: string;
  callback: (message: IMessage) => void;
  subscription: StompSubscription | null;
}

export class ShuttleWebSocketClient {
  private client: Client | null = null;
  private subscriptions: Map<string, SubscriptionInfo> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private baseReconnectDelay = 1000;
  private maxReconnectDelay = 30000;
  private options: WebSocketOptions = {};
  private isManualDisconnect = false;
  private connectionPromise: Promise<void> | null = null;
  private heartbeatInterval: ReturnType<typeof setInterval> | null = null;
  private subscriptionRenewalInterval: ReturnType<typeof setInterval> | null = null;

  private dashboardRenewTargets: Set<string> = new Set(); // key = `${lcId}::${pageId}`

  private makeDashboardKey(lcId: string, pageId: string): string {
    return `${lcId}::${pageId}`;
  }

  private parseDashboardKey(key: string): { lcId: string; pageId: string } {
    const [lcId, pageId] = key.split('::');
    return { lcId, pageId };
  }

  constructor(private baseUrl: string = 'http://localhost:9500/ws/shuttle') {}
  /**
   * 재연결 딜레이 계산 (지수 백오프)
   */
  private getReconnectDelay(): number {
    const delay = this.baseReconnectDelay * Math.pow(2, this.reconnectAttempts);
    return Math.min(delay, this.maxReconnectDelay);
  }

  /**
   * WebSocket 연결
   */
  connect(options: WebSocketOptions = {}): Promise<void> {
    // 이미 연결 중이면 기존 Promise 반환
    if (this.connectionPromise) {
      return this.connectionPromise;
    }

    // 이미 연결되어 있으면 즉시 resolve
    if (this.isConnected()) {
      return Promise.resolve();
    }

    this.options = { ...this.options, ...options };
    this.isManualDisconnect = false;

    this.connectionPromise = new Promise((resolve, reject) => {
      try {
        // 기존 클라이언트 정리
        if (this.client) {
          this.client.deactivate();
        }


        this.client = new Client({
          webSocketFactory: () => {
            return new SockJS(this.baseUrl);
          },
          reconnectDelay: 0, // 자체 재연결 로직 사용
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          onConnect: () => {
            this.reconnectAttempts = 0;
            this.connectionPromise = null;
            console.log('[Shuttle WS] Connected');

            // 기존 구독 복원
            this.restoreSubscriptions();

            // heartbeat 시작
            this.startHeartbeat();

            this.options.onConnect?.();
            resolve();
          },
          onDisconnect: () => {
            console.log('[Shuttle WS] Disconnected');
            this.stopHeartbeat();
            this.connectionPromise = null;
            this.options.onDisconnect?.();

            // 수동 연결 해제가 아니면 재연결 시도
            if (!this.isManualDisconnect) {
              this.handleReconnect();
            }
          },
          onStompError: (frame) => {
            console.error('[Shuttle WS] STOMP Error:', frame);
            this.connectionPromise = null;
            const error: WebSocketError = {
              code: 'STOMP_ERROR',
              message: frame.headers?.message || 'STOMP error occurred',
              timestamp: Date.now(),
            };
            this.options.onError?.(error);
            reject(error);
          },
          onWebSocketError: (event) => {
            console.error('[Shuttle WS] WebSocket Error:', event);
            this.connectionPromise = null;
            if (!this.isManualDisconnect) {
              this.handleReconnect();
            }
          },
          debug: (str) => {
            // STOMP 디버그 로그 (연결 문제 디버깅용)
            if (
              str.includes('Opening') ||
              str.includes('connected') ||
              str.includes('CONNECT') ||
              str.includes('error') ||
              str.includes('Error')
            ) {
              console.log('[Shuttle WS Debug]', str);
            }
          },
        });

        this.client.activate();
      } catch (error) {
        this.connectionPromise = null;
        reject(error);
      }
    });

    return this.connectionPromise;
  }

  /**
   * Heartbeat 시작
   */
  private startHeartbeat(): void {
    this.stopHeartbeat();
    // 10초마다 연결 상태 확인
    this.heartbeatInterval = setInterval(() => {
      if (!this.isConnected() && !this.isManualDisconnect) {
        console.warn('[Shuttle WS] Heartbeat detected disconnection');
        this.handleReconnect();
      }
    }, 10000);

    // 구독 갱신 시작 (15초마다 - 백엔드 타임아웃 30초 전에 갱신)
    this.startSubscriptionRenewal();
  }

  /**
   * Heartbeat 중지
   */
  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
    this.stopSubscriptionRenewal();
  }

  /**
   * 구독 갱신 시작 (백엔드 구독 타임아웃 방지)
   */
  private startSubscriptionRenewal(): void {
    this.stopSubscriptionRenewal();

    this.subscriptionRenewalInterval = setInterval(() => {
      if (!this.isConnected()) return;
      if (this.dashboardRenewTargets.size === 0) return;

      for (const key of this.dashboardRenewTargets) {
        const { lcId, pageId } = this.parseDashboardKey(key);
        this.client?.publish({
          destination: `/app/shuttle/dashboard/subscribe/${lcId}/${pageId}`,
          body: '{}',
        });
      }
    }, 15000);
  }

  /**
   * 구독 갱신 중지
   */
  private stopSubscriptionRenewal(): void {
    if (this.subscriptionRenewalInterval) {
      clearInterval(this.subscriptionRenewalInterval);
      this.subscriptionRenewalInterval = null;
    }
  }

  /**
   * 기존 구독 복원
   */
  private restoreSubscriptions(): void {
    if (!this.client?.connected) return;

    this.subscriptions.forEach((info, topic) => {
      if (this.client?.connected) {
        info.subscription = this.client.subscribe(topic, info.callback);
      }
    });
  }

  /**
   * WebSocket 연결 해제
   */
  disconnect(): void {
    this.isManualDisconnect = true;
    this.stopHeartbeat();

    // 모든 구독 해제
    this.subscriptions.forEach((info) => {
      info.subscription?.unsubscribe();
    });
    this.subscriptions.clear();

    // 클라이언트 비활성화
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }

    this.connectionPromise = null;
    this.reconnectAttempts = 0;
  }

  /**
   * 연결 상태 확인
   */
  isConnected(): boolean {
    return this.client?.connected ?? false;
  }

  /**
   * 재연결 처리
   */
  private handleReconnect(): void {
    if (this.isManualDisconnect) {
      return;
    }

    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.getReconnectDelay();

      this.options.onReconnecting?.(this.reconnectAttempts, this.maxReconnectAttempts);

      setTimeout(() => {
        if (!this.isManualDisconnect) {
          this.connect(this.options).catch((error) => {
            console.error('[Shuttle WS] Reconnection failed:', error);
          });
        }
      }, delay);
    } else {
      console.error('[Shuttle WS] Max reconnect attempts reached');
      const error: WebSocketError = {
        code: 'MAX_RECONNECT_REACHED',
        message: 'Maximum reconnection attempts reached. Please refresh the page.',
        timestamp: Date.now(),
      };
      this.options.onError?.(error);
    }
  }

  /**
   * 대시보드 데이터 구독
   */
  subscribeDashboard(
    lcId: string,
    pageId: string,
    callback: (data: DashboardEquipmentData[]) => void,
  ): void {
    const topic = `/topic/shuttle/dashboard/${lcId}/${pageId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(data);
      } catch (e) {
        console.error('[Shuttle WS] Parse error:', e);
      }
    });

    // 초기 데이터 요청
    this.requestDashboardRefresh(lcId, pageId);
  }

  /**
   * 대시보드 구독 해제
   */
  unsubscribeDashboard(lcId: string, pageId: string): void {
    const topic = `/topic/shuttle/dashboard/${lcId}/${pageId}`;
    this.unsubscribe(topic);
  }

  /**
   * 대시보드 데이터 새로고침 요청
   */
  requestDashboardRefresh(lcId: string, pageId: string): void {
    if (this.client?.connected) {
      this.client.publish({
        destination: `/app/shuttle/dashboard/refresh/${lcId}/${pageId}`,
        body: '{}',
      });
    }
  }

  /**
   * 설비 상태 변경 구독
   */
  subscribeEquipmentChanges(lcId: string, callback: (data: any) => void): void {
    const topic = `/topic/shuttle/equipment/change/${lcId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(data);
      } catch (e) {
        console.error('[Shuttle WS] Parse error:', e);
      }
    });
  }

  /**
   * 알람 구독
   */
  subscribeAlarms(lcId: string, callback: (data: AlarmData) => void): void {
    const topic = `/topic/shuttle/alarm/${lcId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(data);
      } catch (e) {
        console.error('[Shuttle WS] Parse error:', e);
      }
    });
  }

  /**
   * 에러 구독
   */
  subscribeErrors(lcId: string, callback: (data: WebSocketError) => void): void {
    const topic = `/topic/shuttle/error/${lcId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(data);
      } catch (e) {
        console.error('[Shuttle WS] Parse error:', e);
      }
    });
  }

  /**
   * 설비 위치 변경 구독 (Shuttle 이동 추적)
   */
  subscribeEquipmentPosition(lcId: string, callback: (data: EquipmentPositionEvent) => void): void {
    const topic = `/topic/shuttle/equipment/position/${lcId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(data);
      } catch (e) {
        console.error('[Shuttle WS] Parse error:', e);
      }
    });
  }

  /**
   * 설비 이동 시작 구독
   */
  subscribeEquipmentMove(lcId: string, callback: (data: EquipmentMoveEvent) => void): void {
    const topic = `/topic/shuttle/equipment/move/${lcId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(data);
      } catch (e) {
        console.error('[Shuttle WS] Parse error:', e);
      }
    });
  }

  /**
   * BCR 스캔 이벤트 구독
   */
  subscribeBcrScan(lcId: string, callback: (data: BcrScanEvent) => void): void {
    const topic = `/topic/shuttle/bcr/scan/${lcId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(data);
      } catch (e) {
        console.error('[Shuttle WS] Parse error:', e);
      }
    });
  }

  // ============================================
  // 셔틀/화물 실시간 위치 브로드캐스트 구독
  // ============================================

  /**
   * 셔틀 실시간 위치 구독 (500ms 주기)
   */
  subscribeShuttlePositions(lcId: string, callback: (data: DashboardShuttleData[]) => void): void {
    const topic = `/topic/shuttle/positions/${lcId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(data);
      } catch (e) {
        console.error('[Shuttle WS] Parse error:', e);
      }
    });
  }

  /**
   * 화물 실시간 위치 구독 (500ms 주기)
   */
  subscribeCargoPositions(lcId: string, callback: (data: DashboardCargoData[]) => void): void {
    const topic = `/topic/shuttle/cargos/${lcId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(data);
      } catch (e) {
        console.error('[Shuttle WS] Parse error:', e);
      }
    });
  }

  // ============================================
  // 실시간 설비 상태 구독 (TbEqCarMst, TbEqRackMst, TbWcsShuttleOrder)
  // ============================================

  /**
   * 컨베이어(TbEqCvMst) 상태 구독 (실시간)
   * 설비그룹 단위로 구독하여 컨베이어 상태(cargo_yn 포함) 수신
   */
  subscribeCvStatus(eqGroupId: string, callback: (data: TbEqCvMst[]) => void): void {
    const topic = `/topic/shuttle/cv/status/${eqGroupId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(data);
      } catch (e) {
        console.error('[Shuttle WS] Parse error:', e);
      }
    });
  }

  /**
   * 대시보드 구독 시작 메시지 전송
   */
  sendDashboardSubscribe(lcId: string, pageId: string): void {
    this.dashboardRenewTargets.add(this.makeDashboardKey(lcId, pageId));

    if (this.client?.connected) {
      this.client.publish({
        destination: `/app/shuttle/dashboard/subscribe/${lcId}/${pageId}`,
        body: '{}',
      });
    }
  }

  sendDashboardUnsubscribe(lcId: string, pageId: string): void {
    this.dashboardRenewTargets.delete(this.makeDashboardKey(lcId, pageId));

    // 더 이상 갱신할 대상이 없으면 갱신 루프를 꺼도 됨(선택)
    if (this.dashboardRenewTargets.size === 0) {
      this.stopSubscriptionRenewal();
    }

    if (this.client?.connected) {
      this.client.publish({
        destination: `/app/shuttle/dashboard/unsubscribe/${lcId}/${pageId}`,
        body: '{}',
      });
    }
  }

  /**
   * 범용 구독
   */
  private subscribe(topic: string, callback: (message: IMessage) => void): void {
    // 기존 구독이 있으면 해제
    if (this.subscriptions.has(topic)) {
      const existingInfo = this.subscriptions.get(topic);
      existingInfo?.subscription?.unsubscribe();
    }

    // 구독 정보 저장 (재연결 시 복원용)
    const subscriptionInfo: SubscriptionInfo = {
      topic,
      callback,
      subscription: null,
    };

    if (this.client?.connected) {
      subscriptionInfo.subscription = this.client.subscribe(topic, callback);
    } else {
    }

    this.subscriptions.set(topic, subscriptionInfo);
  }

  /**
   * 범용 구독 해제
   */
  private unsubscribe(topic: string): void {
    if (this.subscriptions.has(topic)) {
      const info = this.subscriptions.get(topic);
      info?.subscription?.unsubscribe();
      this.subscriptions.delete(topic);
    }
  }

  /**
   * 구독 개수 조회
   */
  getSubscriptionCount(): number {
    return this.subscriptions.size;
  }

  // ============================================
  // Provider-based RealTime 구독 (백엔드 토픽과 일치)
  // Topic Pattern: /topic/realtime/{type}/{lcId}/{eqGroupId}/{pageId}
  // ============================================

  /**
   * 셔틀 실시간 위치 구독 (Provider 기반, 500ms)
   * Topic: /topic/realtime/shuttle/{lcId}/{eqGroupId}/{pageId}
   */
  subscribeShuttleRt(
    lcId: string,
    eqGroupId: string,
    pageId: string,
    callback: (data: RtShuttlePosition[]) => void,
  ): void {
    const topic = `/topic/realtime/shuttle/${lcId}/${eqGroupId}/${pageId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(Array.isArray(data) ? data : [data]);
      } catch (e) {
        console.error('[Shuttle WS] Parse error on shuttle RT:', e);
      }
    });
  }

  /**
   * 셔틀 실시간 구독 해제
   */
  unsubscribeShuttleRt(lcId: string, eqGroupId: string, pageId: string): void {
    const topic = `/topic/realtime/shuttle/${lcId}/${eqGroupId}/${pageId}`;
    this.unsubscribe(topic);
  }

  /**
   * 컨베이어 실시간 상태 구독 (Provider 기반, 500ms)
   * Topic: /topic/realtime/conveyor/{lcId}/{eqGroupId}/{pageId}
   */
  subscribeConveyorRt(
    lcId: string,
    eqGroupId: string,
    pageId: string,
    callback: (data: RtConveyorStatus[]) => void,
  ): void {
    const topic = `/topic/realtime/conveyor/${lcId}/${eqGroupId}/${pageId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(Array.isArray(data) ? data : [data]);
      } catch (e) {
        console.error('[Shuttle WS] Parse error on conveyor RT:', e);
      }
    });
  }

  /**
   * 컨베이어 실시간 구독 해제
   */
  unsubscribeConveyorRt(lcId: string, eqGroupId: string, pageId: string): void {
    const topic = `/topic/realtime/conveyor/${lcId}/${eqGroupId}/${pageId}`;
    this.unsubscribe(topic);
  }

  /**
   * 리프터 실시간 상태 구독 (Provider 기반, 500ms)
   * Topic: /topic/realtime/lifter/{lcId}/{eqGroupId}/{pageId}
   */
  subscribeLifterRt(
    lcId: string,
    eqGroupId: string,
    pageId: string,
    callback: (data: RtLifterStatus[]) => void,
  ): void {
    const topic = `/topic/realtime/lifter/${lcId}/${eqGroupId}/${pageId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(Array.isArray(data) ? data : [data]);
      } catch (e) {
        console.error('[Shuttle WS] Parse error on lifter RT:', e);
      }
    });
  }

  /**
   * 리프터 실시간 구독 해제
   */
  unsubscribeLifterRt(lcId: string, eqGroupId: string, pageId: string): void {
    const topic = `/topic/realtime/lifter/${lcId}/${eqGroupId}/${pageId}`;
    this.unsubscribe(topic);
  }

  /**
   * 화물 실시간 위치 구독 (Provider 기반, 500ms)
   * Topic: /topic/realtime/cargo/{lcId}/{eqGroupId}/{pageId}
   */
  subscribeCargoRt(
    lcId: string,
    eqGroupId: string,
    pageId: string,
    callback: (data: RtCargoPosition[]) => void,
  ): void {
    const topic = `/topic/realtime/cargo/${lcId}/${eqGroupId}/${pageId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(Array.isArray(data) ? data : [data]);
      } catch (e) {
        console.error('[Shuttle WS] Parse error on cargo RT:', e);
      }
    });
  }

  /**
   * 화물 실시간 구독 해제
   */
  unsubscribeCargoRt(lcId: string, eqGroupId: string, pageId: string): void {
    const topic = `/topic/realtime/cargo/${lcId}/${eqGroupId}/${pageId}`;
    this.unsubscribe(topic);
  }

  /**
   * 작업 실시간 상태 구독 (Provider 기반, 1000ms)
   * Topic: /topic/realtime/job/{lcId}/{eqGroupId}
   * Note: Job은 pageId 필터링 없음 (전체 작업 현황)
   */
  subscribeJobRt(lcId: string, eqGroupId: string, callback: (data: RtJobStatus[]) => void): void {
    const topic = `/topic/realtime/job/${lcId}/${eqGroupId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(Array.isArray(data) ? data : [data]);
      } catch (e) {
        console.error('[Shuttle WS] Parse error on job RT:', e);
      }
    });
  }

  /**
   * 작업 실시간 구독 해제
   */
  unsubscribeJobRt(lcId: string, eqGroupId: string): void {
    const topic = `/topic/realtime/job/${lcId}/${eqGroupId}`;
    this.unsubscribe(topic);
  }

  /**
   * 알람 실시간 구독 (Provider 기반, 1000ms)
   * Topic: /topic/realtime/alarm/{lcId}/{eqGroupId}
   * Note: Alarm은 pageId 필터링 없음 (설비 레벨 이벤트)
   */
  subscribeAlarmRt(lcId: string, eqGroupId: string, callback: (data: RtAlarm[]) => void): void {
    const topic = `/topic/realtime/alarm/${lcId}/${eqGroupId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(Array.isArray(data) ? data : [data]);
      } catch (e) {
        console.error('[Shuttle WS] Parse error on alarm RT:', e);
      }
    });
  }

  /**
   * 알람 실시간 구독 해제
   */
  unsubscribeAlarmRt(lcId: string, eqGroupId: string): void {
    const topic = `/topic/realtime/alarm/${lcId}/${eqGroupId}`;
    this.unsubscribe(topic);
  }

  /**
   * 랙 재고 실시간 구독 (Provider 기반, 1000ms)
   * Topic: /topic/realtime/rack-inventory/{lcId}/{eqGroupId}/{pageId}
   */
  subscribeRackInventoryRt(
    lcId: string,
    eqGroupId: string,
    pageId: string,
    callback: (data: any[]) => void,
  ): void {
    const topic = `/topic/realtime/rack-inventory/${lcId}/${eqGroupId}/${pageId}`;
    this.subscribe(topic, (message) => {
      try {
        const data = keysToCamel(JSON.parse(message.body));
        callback(Array.isArray(data) ? data : [data]);
      } catch (e) {
        console.error('[Shuttle WS] Parse error on rack-inventory RT:', e);
      }
    });
  }

  /**
   * 랙 재고 실시간 구독 해제
   */
  unsubscribeRackInventoryRt(lcId: string, eqGroupId: string, pageId: string): void {
    const topic = `/topic/realtime/rack-inventory/${lcId}/${eqGroupId}/${pageId}`;
    this.unsubscribe(topic);
  }

  /**
   * 모든 Provider 기반 실시간 토픽 구독 해제
   */
  unsubscribeAllRt(lcId: string, eqGroupId: string, pageId: string): void {
    this.unsubscribeShuttleRt(lcId, eqGroupId, pageId);
    this.unsubscribeConveyorRt(lcId, eqGroupId, pageId);
    this.unsubscribeLifterRt(lcId, eqGroupId, pageId);
    this.unsubscribeCargoRt(lcId, eqGroupId, pageId);
    this.unsubscribeJobRt(lcId, eqGroupId);
    this.unsubscribeAlarmRt(lcId, eqGroupId);
    this.unsubscribeRackInventoryRt(lcId, eqGroupId, pageId);
  }

  // ============================================
  // Provider 브로드캐스트 시작/종료 요청
  // ============================================

  /**
   * Provider 기반 실시간 브로드캐스트 시작 요청
   * 백엔드 RealTimeBroadcastScheduler를 시작시킴
   *
   * @param eqGroupId 설비 그룹 ID
   * @param lcId 센터 ID
   * @param pageId 페이지 ID (좌표 로드용)
   */
  requestRtBroadcastStart(eqGroupId: string, lcId: string, pageId: string): void {
    if (!this.client || !this.client.connected) {
      console.warn('[Shuttle WS] Cannot request broadcast start: not connected');
      return;
    }

    const destination = `/app/realtime/subscribe/${eqGroupId}/${lcId}/${pageId}`;

    this.client.publish({
      destination,
      body: JSON.stringify({ timestamp: Date.now() }),
    });
  }

  /**
   * Provider 기반 실시간 브로드캐스트 종료 요청
   *
   * @param eqGroupId 설비 그룹 ID
   * @param lcId 센터 ID
   * @param pageId 페이지 ID
   */
  requestRtBroadcastStop(eqGroupId: string, lcId: string, pageId: string): void {
    if (!this.client || !this.client.connected) {
      console.warn('[Shuttle WS] Cannot request broadcast stop: not connected');
      return;
    }

    const destination = `/app/realtime/unsubscribe/${eqGroupId}/${lcId}/${pageId}`;

    this.client.publish({
      destination,
      body: JSON.stringify({ timestamp: Date.now() }),
    });
  }

  /**
   * Provider 기반 전체 구독 해제 (종료 요청 + 토픽 구독 해제)
   *
   * @param lcId 센터 ID
   * @param eqGroupId 설비 그룹 ID
   * @param pageId 페이지 ID
   */
  teardownRtSubscriptions(lcId: string, eqGroupId: string, pageId: string): void {
    // 1. 토픽 구독 해제
    this.unsubscribeAllRt(lcId, eqGroupId, pageId);

    // 2. 백엔드에 브로드캐스트 종료 요청
    this.requestRtBroadcastStop(eqGroupId, lcId, pageId);
  }
}

// 싱글톤 인스턴스
let wsClient: ShuttleWebSocketClient | null = null;

export function getWebSocketClient(): ShuttleWebSocketClient {
  if (!wsClient) {
    wsClient = new ShuttleWebSocketClient();
  }
  return wsClient;
}

export function destroyWebSocketClient(): void {
  if (wsClient) {
    wsClient.disconnect();
    wsClient = null;
  }
}
