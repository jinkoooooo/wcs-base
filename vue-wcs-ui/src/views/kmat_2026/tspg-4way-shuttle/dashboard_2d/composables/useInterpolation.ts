/**
 * useInterpolation.ts
 * 위치 보간(Interpolation) 애니메이션 컴포저블
 *
 * ============================================
 * 개요 (비개발자용 설명)
 * ============================================
 *
 * 이 파일은 셔틀과 화물이 화면에서 부드럽게 이동하는 것처럼 보이게 하는
 * "보간(Interpolation)" 기능을 담당합니다.
 *
 * [왜 필요한가?]
 * - 서버에서 셔틀/화물 위치 정보는 약 500ms(0.5초)마다 전송됩니다.
 * - 만약 보간 없이 그대로 표시하면, 셔틀이 0.5초마다 "순간이동"하는 것처럼 보입니다.
 * - 보간을 사용하면 이전 위치에서 새 위치로 부드럽게 이동하는 애니메이션 효과를 줍니다.
 *
 * [작동 원리]
 * 1. 서버에서 새 위치 데이터 수신 (예: 셔틀이 (100, 100)에서 (200, 200)으로 이동)
 * 2. 현재 위치를 "시작점"으로, 새 위치를 "목표점"으로 설정
 * 3. 500ms 동안 시작점에서 목표점까지 점진적으로 이동 (매 프레임마다 위치 계산)
 * 4. 이동 속도는 "이징(Easing)" 함수로 자연스럽게 조절 (처음 빠르게, 끝에서 느리게)
 *
 * [용어 설명]
 * - 보간(Interpolation): 두 점 사이의 중간 값을 계산하는 것
 * - 이징(Easing): 애니메이션의 속도 변화를 조절하는 함수
 * - requestAnimationFrame: 브라우저의 화면 갱신 주기(보통 60fps)에 맞춰 애니메이션을 실행하는 API
 *
 * ============================================
 * 사용 방법
 * ============================================
 *
 * ```typescript
 * // 셔틀용 보간
 * const {
 *   interpolatedShuttles,    // 현재 보간된 셔틀 위치 목록
 *   updateShuttlePositions,  // 새 셔틀 위치 업데이트
 *   clearShuttles,           // 모든 셔틀 데이터 초기화
 * } = useShuttleInterpolation();
 *
 * // 화물용 보간
 * const {
 *   interpolatedCargos,      // 현재 보간된 화물 위치 목록
 *   updateCargoPositions,    // 새 화물 위치 업데이트
 *   clearCargos,             // 모든 화물 데이터 초기화
 * } = useCargoInterpolation();
 *
 * // WebSocket에서 데이터 수신 시
 * watch(store.shuttlePositions, (newPositions) => {
 *   updateShuttlePositions(newPositions);
 * });
 *
 * // 템플릿에서 보간된 위치 사용
 * <div
 *   v-for="shuttle in interpolatedShuttles"
 *   :key="shuttle.id"
 *   :style="{ left: shuttle.currentX + 'px', top: shuttle.currentY + 'px' }"
 * >
 *   {{ shuttle.data.equipmentCode }}
 * </div>
 * ```
 *
 * ============================================
 * 내보내는 함수들
 * ============================================
 *
 * 1. useInterpolation(duration?)
 *    - 범용 보간 컴포저블
 *    - duration: 보간 지속 시간 (기본값: 500ms)
 *
 * 2. useShuttleInterpolation()
 *    - 셔틀 전용 보간 컴포저블
 *    - equipmentId를 ID로 사용
 *    - posX, posY를 좌표로 사용
 *
 * 3. useCargoInterpolation()
 *    - 화물 전용 보간 컴포저블
 *    - cargoId를 ID로 사용
 *    - posX, posY를 좌표로 사용
 */

import { shallowRef, onMounted, onUnmounted } from 'vue';

// ============================================
// 인터페이스 정의
// ============================================

/**
 * Position (위치 인터페이스)
 *
 * 보간할 객체의 기본 위치 정보입니다.
 * 서버에서 수신하는 원시 위치 데이터 형식입니다.
 *
 * @property id - 객체 고유 식별자 (예: 셔틀 ID, 화물 ID)
 * @property x - X 좌표 (픽셀 단위)
 * @property y - Y 좌표 (픽셀 단위)
 * @property [key: string] - 추가 속성 (예: equipmentCode, barcode 등)
 */
export interface Position {
  /** 객체 고유 식별자 */
  id: string;

  /** X 좌표 (캔버스 기준, 픽셀 단위) */
  x: number;

  /** Y 좌표 (캔버스 기준, 픽셀 단위) */
  y: number;

  /** 추가 데이터 (설비코드, 바코드, 상태 등) */
  [key: string]: unknown;
}

/**
 * InterpolatedPosition (보간된 위치 인터페이스)
 *
 * 보간 계산에 필요한 모든 정보를 포함합니다.
 * 이전 위치, 현재 보간 위치, 목표 위치를 모두 저장합니다.
 *
 * [시각적 설명]
 *
 *   prevX,prevY ────────────────────> targetX,targetY
 *   (시작점)                           (목표점)
 *                ↑
 *           currentX,currentY
 *           (현재 보간 위치)
 *
 * 시간이 지남에 따라 currentX,currentY가 target 방향으로 이동합니다.
 */
export interface InterpolatedPosition extends Position {
  /**
   * 현재 보간된 X 좌표
   * - 화면에 실제로 표시되는 X 위치
   * - 매 프레임마다 갱신됨
   */
  currentX: number;

  /**
   * 현재 보간된 Y 좌표
   * - 화면에 실제로 표시되는 Y 위치
   * - 매 프레임마다 갱신됨
   */
  currentY: number;

  /**
   * 목표 X 좌표
   * - 서버에서 수신한 최신 X 위치
   * - 보간이 끝나면 currentX가 이 값이 됨
   */
  targetX: number;

  /**
   * 목표 Y 좌표
   * - 서버에서 수신한 최신 Y 위치
   * - 보간이 끝나면 currentY가 이 값이 됨
   */
  targetY: number;

  /**
   * 이전 X 좌표 (보간 시작점)
   * - 새 데이터 수신 시점의 currentX 값
   * - 보간 계산의 시작점으로 사용
   */
  prevX: number;

  /**
   * 이전 Y 좌표 (보간 시작점)
   * - 새 데이터 수신 시점의 currentY 값
   * - 보간 계산의 시작점으로 사용
   */
  prevY: number;

  /**
   * 보간 시작 시간 (밀리초)
   * - performance.now() 값
   * - 보간 진행률 계산에 사용
   */
  startTime: number;

  /**
   * 추가 데이터 저장소
   * - id, x, y를 제외한 모든 추가 정보
   * - 예: equipmentCode, movementStatus, hasCargo 등
   */
  data: Record<string, unknown>;

  /**
   * 항목 표시 상태
   * - 'visible': 정상 표시
   * - 'entering': 새로 추가됨 (fade-in)
   * - 'leaving': 사라지는 중 (fade-out)
   */
  displayState: 'visible' | 'entering' | 'leaving';

  /**
   * 투명도 (0.0 ~ 1.0)
   * - entering/leaving 애니메이션에 사용
   */
  opacity: number;

  /**
   * 마지막 업데이트 시간
   * - 오래된 항목 감지에 사용
   */
  lastUpdateTime: number;
}

// ============================================
// 상수 정의
// ============================================

/**
 * 보간 지속 시간 (밀리초)
 *
 * - ECS DB 업데이트 주기 250ms와 동일하게 설정
 * - 선형(linear) 이징 사용 시: 250ms 동안 등속으로 이동 → 매끄러움
 * - easeOutCubic 사용 시 끝에서 감속하여 뚝뚝 끊기므로 선형 이징 필수
 */
const INTERPOLATION_DURATION = 250;

/**
 * 페이드 인 지속 시간 (밀리초)
 * - 새 항목이 나타날 때 투명도 애니메이션
 */
const FADE_IN_DURATION = 300;

/**
 * 페이드 아웃 지속 시간 (밀리초)
 * - 사라지는 항목의 투명도 애니메이션
 */
const FADE_OUT_DURATION = 400;

/**
 * 항목 타임아웃 (밀리초)
 * - 이 시간 동안 업데이트가 없으면 leaving 상태로 전환
 * - WebSocket 주기(500ms)의 6배로 설정하여 네트워크 지연에 대응
 * - 기존 1500ms에서 3000ms로 증가하여 깜빡임 방지
 */
const ITEM_TIMEOUT = 3000;

// ============================================
// 메인 컴포저블: useInterpolation
// ============================================

/**
 * 위치 보간 컴포저블 (범용)
 *
 * 이 함수는 위치 데이터를 부드럽게 보간하는 모든 로직을 제공합니다.
 * requestAnimationFrame을 사용하여 브라우저의 화면 갱신 주기(60fps)에 맞춰
 * 매 프레임마다 보간된 위치를 계산합니다.
 *
 * [동작 흐름]
 * 1. 컴포넌트 마운트 → 애니메이션 루프 시작
 * 2. 서버 데이터 수신 → updateTargetPositions() 호출
 * 3. 매 프레임 → 보간된 위치 계산 → Vue 반응성 시스템이 UI 갱신
 * 4. 컴포넌트 언마운트 → 애니메이션 루프 중지
 *
 * @param duration - 보간 지속 시간 (기본값: 500ms)
 * @returns 보간 상태와 제어 함수들
 */
export function useInterpolation(duration: number = INTERPOLATION_DURATION) {
  // ============================================
  // 내부 상태 (State)
  // ============================================

  /**
   * 보간 중인 모든 위치 데이터 (비-반응성 plain Map)
   *
   * [최적화 핵심]
   * - ref<Map<>>()으로 감싸면 Vue가 Map의 get/set/forEach를 모두 Proxy로 감시
   * - 60fps RAF 루프 안에서 Proxy가 개입하면 프레임당 오버헤드가 누적
   * - plain Map으로 선언 → Map 연산에 Vue 개입 없음
   * - 반응성은 오직 interpolatedList(shallowRef)만 담당
   */
  const positionsMap = new Map<string, InterpolatedPosition>();

  /**
   * 화면에 표시할 보간된 위치 목록 (유일한 반응성 게이트)
   * - shallowRef: 배열 참조 교체 시만 Vue 갱신 트리거
   * - 매 RAF 프레임마다 새 배열로 교체
   */
  const interpolatedList = shallowRef<InterpolatedPosition[]>([]);

  /**
   * requestAnimationFrame의 반환 ID
   * - 애니메이션 취소 시 사용
   * - null이면 애니메이션이 실행 중이지 않음
   */
  let animationFrameId: number | null = null;

  /**
   * 마지막 프레임 시간 (밀리초)
   * - 프레임 간격 계산에 사용 (디버깅 용도)
   */
  let lastFrameTime = 0;

  // ============================================
  // 핵심 함수들
  // ============================================

  /**
   * 목표 위치 업데이트
   *
   * WebSocket에서 새 위치 데이터를 수신했을 때 호출합니다.
   * 기존 데이터가 있으면 현재 보간 위치를 시작점으로,
   * 새 위치를 목표점으로 설정하여 다음 보간을 준비합니다.
   *
   * [처리 로직]
   * - 기존 데이터 있음: 현재 위치 → 시작점, 새 위치 → 목표점
   * - 새 데이터: fade-in 애니메이션과 함께 추가
   * - 사라진 데이터: fade-out 애니메이션 후 제거
   *
   * @param newPositions - 서버에서 수신한 새 위치 데이터 배열
   *
   * @example
   * // WebSocket 메시지 수신 시
   * watch(store.shuttlePositions, (positions) => {
   *   updateTargetPositions(positions.map(p => ({
   *     id: p.equipmentId,
   *     x: p.posX,
   *     y: p.posY,
   *     ...p
   *   })));
   * });
   */
  function updateTargetPositions(newPositions: Position[]) {
    const now = performance.now();
    const newIds = new Set(newPositions.map((p) => p.id));

    // 1. 새 위치 데이터 처리
    for (const pos of newPositions) {
      const existing = positionsMap.get(pos.id);

      // 보간 진행 상태 확인
      // - 보간이 끝나기 전 새 데이터가 도착하면(시뮬레이터 빠를 때):
      //   prev/startTime 을 리셋하지 않고 target 만 갱신해서 흐름을 이어간다.
      //   → 셔틀이 가던 방향/속도 그대로 유지되어 끊김 없이 부드럽게 이어짐.
      // - 보간이 이미 완료된 상태(셔틀이 멈춰 있다가 새 명령 받음):
      //   정상적으로 새 보간 시작.
      if (existing) {
        // 기존 데이터가 있으면 현재 보간 위치를 시작점으로 설정
        const elapsed = now - existing.startTime;
        const stillInterpolating = elapsed < duration;

        if (stillInterpolating) {
          // 흐름 유지: target 만 갱신, prev/startTime 보존
          existing.targetX = pos.x;
          existing.targetY = pos.y;
        } else {
          // 보간 완료 → 새 보간 시작
          existing.prevX = existing.currentX;
          existing.prevY = existing.currentY;
          existing.targetX = pos.x;
          existing.targetY = pos.y;
          existing.startTime = now;
        }
        existing.x = pos.x;
        existing.y = pos.y;
        existing.lastUpdateTime = now;

        // leaving 상태였다면 다시 visible로 복구
        if (existing.displayState === 'leaving') {
          existing.displayState = 'visible';
          existing.opacity = 1;
        }

        // 추가 데이터 업데이트 (id, x, y 제외)
        const { id, x, y, ...rest } = pos;
        existing.data = { ...existing.data, ...rest };
      } else {
        // 새 데이터면 fade-in 애니메이션과 함께 추가
        const { id, x, y, ...rest } = pos;
        positionsMap.set(pos.id, {
          id: pos.id,
          x: pos.x,
          y: pos.y,
          currentX: pos.x,
          currentY: pos.y,
          targetX: pos.x,
          targetY: pos.y,
          prevX: pos.x,
          prevY: pos.y,
          startTime: now,
          data: rest,
          displayState: 'entering',
          opacity: 0,
          lastUpdateTime: now,
        });
      }
    }

    // 2. 사라진 항목을 leaving 상태로 전환
    positionsMap.forEach((pos, id) => {
      if (!newIds.has(id) && pos.displayState !== 'leaving') {
        pos.displayState = 'leaving';
        pos.startTime = now; // fade-out 시작 시간
      }
    });

    // 3. 즉시 반응성 트리거 - 새 데이터가 바로 화면에 표시되도록
    interpolatedList.value = Array.from(positionsMap.values());
  }

  /**
   * 특정 항목 제거
   */
  function removePosition(id: string) {
    positionsMap.delete(id);
    // 즉시 반응성 트리거
    interpolatedList.value = Array.from(positionsMap.values());
  }

  /**
   * 모든 항목 초기화
   */
  function clearPositions() {
    positionsMap.clear();
    // 즉시 반응성 트리거
    interpolatedList.value = [];
  }

  /**
   * 이징 함수: ease-out cubic
   *
   * 애니메이션의 속도 변화를 조절하는 함수입니다.
   * "ease-out"은 처음에 빠르게 시작해서 끝에서 느려지는 효과입니다.
   *
   * [시각적 비유]
   * - 자동차가 브레이크를 밟으며 정차하는 느낌
   * - 처음에는 빠르게 이동하다가 목표 지점에 가까워질수록 천천히
   *
   * [수학적 설명]
   * - 입력 t: 0.0 ~ 1.0 (0% ~ 100% 진행)
   * - 출력: 이징이 적용된 진행률 (0.0 ~ 1.0)
   * - 공식: 1 - (1 - t)^3
   *
   * [진행률 변환 예시]
   * - t = 0.0 → 0.0 (시작)
   * - t = 0.2 → 0.49 (20% 시간에 49% 이동)
   * - t = 0.5 → 0.88 (50% 시간에 88% 이동)
   * - t = 0.8 → 0.99 (80% 시간에 99% 이동)
   * - t = 1.0 → 1.0 (완료)
   *
   * @param t - 선형 진행률 (0.0 ~ 1.0)
   * @returns 이징이 적용된 진행률 (0.0 ~ 1.0)
   */
  function easeOutCubic(t: number): number {
    return 1 - Math.pow(1 - t, 3);
  }

  /**
   * 선형 보간 (Linear Interpolation, LERP)
   *
   * 두 값 사이의 중간 값을 계산합니다.
   * 게임, 애니메이션에서 가장 기본적인 보간 방법입니다.
   *
   * [수학 공식]
   * result = start + (end - start) × t
   *
   * [예시]
   * - start = 100, end = 200, t = 0.0 → 100 (시작점)
   * - start = 100, end = 200, t = 0.5 → 150 (중간점)
   * - start = 100, end = 200, t = 1.0 → 200 (끝점)
   *
   * @param start - 시작 값
   * @param end - 끝 값
   * @param t - 보간 진행률 (0.0 ~ 1.0)
   * @returns 보간된 값
   */
  function lerp(start: number, end: number, t: number): number {
    return start + (end - start) * t;
  }

  /**
   * 애니메이션 프레임 업데이트 (핵심 루프)
   *
   * requestAnimationFrame에 의해 매 프레임(약 16.67ms, 60fps) 호출됩니다.
   * 모든 보간 대상의 현재 위치를 계산하고 갱신합니다.
   *
   * [처리 순서]
   * 1. 경과 시간 계산 (현재 시간 - 보간 시작 시간)
   * 2. 진행률 계산 (경과 시간 / 전체 시간, 최대 1.0)
   * 3. 이징 적용 (부드러운 감속 효과)
   * 4. 보간된 좌표 계산 (lerp 함수 사용)
   * 5. fade-in/out 애니메이션 처리
   * 6. 다음 프레임 예약
   *
   * @param currentTime - 현재 시간 (performance.now()에서 제공)
   */
  function updateFrame(currentTime: number) {
    // 프레임 간격 계산 (디버깅/성능 모니터링용)
    const _deltaTime = currentTime - lastFrameTime;
    lastFrameTime = currentTime;

    // 제거할 항목 ID 목록
    const toRemove: string[] = [];

    // 데이터가 있을 때만 업데이트
    if (positionsMap.size > 0) {
      // 모든 위치 데이터에 대해 보간 계산
      positionsMap.forEach((pos, id) => {
        // 1. displayState에 따른 처리
        if (pos.displayState === 'entering') {
          // Fade-in 애니메이션
          const fadeElapsed = currentTime - pos.startTime;
          const fadeProgress = Math.min(fadeElapsed / FADE_IN_DURATION, 1);
          pos.opacity = easeOutCubic(fadeProgress);

          if (fadeProgress >= 1) {
            pos.displayState = 'visible';
            pos.opacity = 1;
          }
        } else if (pos.displayState === 'leaving') {
          // Fade-out 애니메이션
          const fadeElapsed = currentTime - pos.startTime;
          const fadeProgress = Math.min(fadeElapsed / FADE_OUT_DURATION, 1);
          pos.opacity = 1 - easeOutCubic(fadeProgress);

          if (fadeProgress >= 1) {
            toRemove.push(id);
            return; // 이 항목은 더 이상 처리하지 않음
          }
        } else {
          // visible 상태 - 타임아웃 체크
          if (currentTime - pos.lastUpdateTime > ITEM_TIMEOUT) {
            pos.displayState = 'leaving';
            pos.startTime = currentTime;
          }
        }

        // 2. 위치 보간 (leaving이 아닌 경우에만)
        // 선형(linear) 이징: 수신 주기(250ms) = 보간 시간(250ms)일 때
        // easeOutCubic은 끝에서 감속하여 셔틀이 목적지에서 '정지' 후 재출발처럼 보임
        // linear는 등속 이동 → 다음 데이터 수신 시 자연스럽게 연결됨
        if (pos.displayState !== 'leaving') {
          const elapsed = currentTime - pos.startTime;

          // 진행률 (0.0 ~ 1.0)
          const progress = Math.min(elapsed / duration, 1);

          // ✨ 핵심 수정: progress를 그대로 lerp에 전달하여 등속(Linear)으로 이동시킴
          pos.currentX = lerp(pos.prevX, pos.targetX, progress);
          pos.currentY = lerp(pos.prevY, pos.targetY, progress);
        }
      });

      // 3. 제거할 항목 삭제
      toRemove.forEach((id) => positionsMap.delete(id));

      // 4. Vue 반응성 트리거 - 새 배열 참조로 교체하여 shallowRef 반응성 보장
      interpolatedList.value = Array.from(positionsMap.values());
    } else {
      // 데이터가 없으면 빈 배열로 설정
      if (interpolatedList.value.length > 0) {
        interpolatedList.value = [];
      }
    }

    // 5. 다음 프레임 예약 (계속 루프)
    animationFrameId = requestAnimationFrame(updateFrame);
  }

  /**
   * 애니메이션 시작
   */
  function startAnimation() {
    if (animationFrameId !== null) return;
    lastFrameTime = performance.now();
    animationFrameId = requestAnimationFrame(updateFrame);
  }

  /**
   * 애니메이션 중지
   */
  function stopAnimation() {
    if (animationFrameId !== null) {
      cancelAnimationFrame(animationFrameId);
      animationFrameId = null;
    }
  }

  // 특정 ID의 보간된 위치 조회
  function getInterpolatedPosition(id: string): InterpolatedPosition | undefined {
    return positionsMap.get(id);
  }

  // 컴포넌트 마운트 시 애니메이션 시작
  onMounted(() => {
    startAnimation();
  });

  // 컴포넌트 언마운트 시 애니메이션 중지
  onUnmounted(() => {
    stopAnimation();
  });

  return {
    // 상태
    interpolatedPositions: interpolatedList,
    // plain Map 반환 (reactive ref 아님 - 렌더링은 interpolatedPositions로만)
    interpolatedPositionMap: positionsMap,

    // 메서드
    updateTargetPositions,
    removePosition,
    clearPositions,
    getInterpolatedPosition,
    startAnimation,
    stopAnimation,
  };
}

// ============================================
// 셔틀 전용 컴포저블: useShuttleInterpolation
// ============================================

/**
 * 셔틀 전용 보간 컴포저블
 *
 * useInterpolation을 셔틀에 맞게 래핑한 컴포저블입니다.
 * 셔틀 데이터 구조(equipmentId, posX, posY)를 보간 시스템이 요구하는
 * Position 형식(id, x, y)으로 자동 변환합니다.
 *
 * [사용 시나리오]
 * 1. Dashboard2D.vue에서 셔틀 위치를 부드럽게 표시할 때
 * 2. WebSocket으로 500ms마다 셔틀 위치가 갱신될 때
 *
 * [반환 값 설명]
 * - interpolatedShuttles: 화면에 표시할 셔틀 목록 (currentX, currentY 사용)
 * - updatePositions: 서버에서 새 위치 수신 시 호출
 * - clear: 페이지/센터 변경 시 모든 셔틀 데이터 초기화
 *
 * @example
 * ```typescript
 * const {
 *   interpolatedItems: interpolatedShuttles,  // 보간된 셔틀 목록
 *   updatePositions: updateShuttlePositions,  // 위치 업데이트 함수
 *   clear: clearShuttles,                     // 초기화 함수
 * } = useShuttleInterpolation();
 *
 * // WebSocket 데이터 수신 시
 * watch(store.shuttlePositions, (newPositions) => {
 *   updateShuttlePositions(newPositions);
 * });
 *
 * // 템플릿에서 사용
 * <div v-for="shuttle in interpolatedShuttles" :key="shuttle.id"
 *      :style="{ left: shuttle.currentX + 'px', top: shuttle.currentY + 'px' }">
 *   {{ shuttle.data.equipmentCode }}
 * </div>
 * ```
 */
export function useShuttleInterpolation() {
  // 250ms: ECS 수신 주기와 동일, linear 이징으로 등속 이동
  const {
    interpolatedPositions,
    interpolatedPositionMap,
    updateTargetPositions,
    removePosition,
    clearPositions,
    getInterpolatedPosition,
    startAnimation,
    stopAnimation,
  } = useInterpolation(250);

  /**
   * 셔틀 위치 데이터를 Position 형식으로 변환하여 업데이트
   *
   * 서버에서 수신한 셔틀 데이터는 equipmentId, posX, posY 형식입니다.
   * 이를 보간 시스템이 요구하는 id, x, y 형식으로 변환합니다.
   *
   * @param shuttles - 서버에서 수신한 셔틀 위치 배열
   *
   * @example
   * // 서버 데이터 형식
   * const serverData = [
   *   { equipmentId: 'SH001', posX: 100, posY: 200, movementStatus: 'MOVING' },
   *   { equipmentId: 'SH002', posX: 300, posY: 400, movementStatus: 'IDLE' },
   * ];
   * updateShuttlePositions(serverData);
   */
  function updateShuttlePositions(
    shuttles: Array<{
      equipmentId: string;
      posX: number;
      posY: number;
      [key: string]: unknown;
    }>,
  ) {
    const positions: Position[] = shuttles.map((s) => ({
      id: s.equipmentId, // equipmentId → id
      x: s.posX ?? 0, // posX → x
      y: s.posY ?? 0, // posY → y
      ...s, // 나머지 데이터 유지
    }));
    updateTargetPositions(positions);
  }

  /**
   * 특정 셔틀의 현재 보간된 좌표 조회
   *
   * @param equipmentId - 조회할 셔틀의 설비 ID
   * @returns 현재 보간된 좌표 또는 null (해당 셔틀이 없는 경우)
   */
  function getShuttlePosition(equipmentId: string): { x: number; y: number } | null {
    const pos = getInterpolatedPosition(equipmentId);
    if (pos) {
      return { x: pos.currentX, y: pos.currentY };
    }
    return null;
  }

  return {
    /** 보간된 셔틀 위치 목록 (화면 표시용) */
    interpolatedItems: interpolatedPositions,

    /** 보간된 셔틀 위치 Map (빠른 조회용) */
    interpolatedShuttleMap: interpolatedPositionMap,

    /** 셔틀 위치 업데이트 (서버 데이터 수신 시 호출) */
    updatePositions: updateShuttlePositions,

    /** 특정 셔틀의 보간된 위치 조회 */
    getShuttlePosition,

    /** 특정 셔틀 제거 */
    removeShuttle: removePosition,

    /** 모든 셔틀 데이터 초기화 */
    clear: clearPositions,

    /** 애니메이션 수동 시작 (보통 자동으로 시작됨) */
    startAnimation,

    /** 애니메이션 수동 중지 */
    stopAnimation,
  };
}

// ============================================
// 화물 전용 컴포저블: useCargoInterpolation
// ============================================

/**
 * 화물 전용 보간 컴포저블
 *
 * useInterpolation을 화물에 맞게 래핑한 컴포저블입니다.
 * 화물 데이터 구조(cargoId, posX, posY)를 보간 시스템이 요구하는
 * Position 형식(id, x, y)으로 자동 변환합니다.
 *
 * [화물 위치 종류]
 * 1. 랙에 보관된 화물: 셀(Cell) 위치에 고정
 * 2. 셔틀이 운반 중인 화물: 셔틀과 함께 이동 (별도 처리)
 * 3. 컨베이어 위의 화물: 컨베이어 위치에 표시
 *
 * [주의사항]
 * - 셔틀이 운반 중인 화물(carriedByShuttleId가 있는 경우)은
 *   이 보간 대상에서 제외해야 합니다.
 * - 셔틀 아이콘 위에 화물 아이콘을 별도로 표시합니다.
 *
 * @example
 * ```typescript
 * const {
 *   interpolatedItems: displayCargos,      // 보간된 화물 목록
 *   updatePositions: updateCargoPositions, // 위치 업데이트 함수
 *   clear: clearCargos,                    // 초기화 함수
 * } = useCargoInterpolation();
 *
 * // WebSocket 데이터 수신 시 (셔틀이 운반 중인 화물 제외)
 * watch(store.dashboardCargos, (newCargos) => {
 *   const visibleCargos = newCargos.filter(c => !c.carriedByShuttleId);
 *   updateCargoPositions(visibleCargos);
 * });
 * ```
 */
export function useCargoInterpolation() {
  // 250ms: ECS 수신 주기와 동일, linear 이징으로 등속 이동
  const {
    interpolatedPositions,
    interpolatedPositionMap,
    updateTargetPositions,
    removePosition,
    clearPositions,
    getInterpolatedPosition,
    startAnimation,
    stopAnimation,
  } = useInterpolation(250);

  /**
   * 화물 위치 데이터를 Position 형식으로 변환하여 업데이트
   *
   * 서버에서 수신한 화물 데이터는 cargoId, posX, posY 형식입니다.
   * 이를 보간 시스템이 요구하는 id, x, y 형식으로 변환합니다.
   *
   * @param cargos - 서버에서 수신한 화물 위치 배열
   *
   * @example
   * // 서버 데이터 형식
   * const serverData = [
   *   { cargoId: 'CGO001', posX: 100, posY: 200, barcode: '1234567890' },
   *   { cargoId: 'CGO002', posX: 300, posY: 400, barcode: '0987654321' },
   * ];
   * updateCargoPositions(serverData);
   */
  function updateCargoPositions(
    cargos: Array<{
      cargoId: string;
      posX: number;
      posY: number;
      [key: string]: unknown;
    }>,
  ) {
    const positions: Position[] = cargos.map((c) => ({
      id: c.cargoId, // cargoId → id
      x: c.posX ?? 0, // posX → x
      y: c.posY ?? 0, // posY → y
      ...c, // 나머지 데이터 유지 (barcode, cargoStatus 등)
    }));
    updateTargetPositions(positions);
  }

  /**
   * 특정 화물의 현재 보간된 좌표 조회
   *
   * @param cargoId - 조회할 화물의 ID
   * @returns 현재 보간된 좌표 또는 null (해당 화물이 없는 경우)
   */
  function getCargoPosition(cargoId: string): { x: number; y: number } | null {
    const pos = getInterpolatedPosition(cargoId);
    if (pos) {
      return { x: pos.currentX, y: pos.currentY };
    }
    return null;
  }

  return {
    /** 보간된 화물 위치 목록 (화면 표시용) */
    interpolatedItems: interpolatedPositions,

    /** 보간된 화물 위치 Map (빠른 조회용) */
    interpolatedCargoMap: interpolatedPositionMap,

    /** 화물 위치 업데이트 (서버 데이터 수신 시 호출) */
    updatePositions: updateCargoPositions,

    /** 특정 화물의 보간된 위치 조회 */
    getCargoPosition,

    /** 특정 화물 제거 */
    removeCargo: removePosition,

    /** 모든 화물 데이터 초기화 */
    clear: clearPositions,

    /** 애니메이션 수동 시작 (보통 자동으로 시작됨) */
    startAnimation,

    /** 애니메이션 수동 중지 */
    stopAnimation,
  };
}

// ============================================
// 기본 내보내기
// ============================================

/**
 * 기본 내보내기: 범용 보간 컴포저블
 *
 * 셔틀/화물 외의 다른 객체에 보간을 적용하고 싶을 때 사용합니다.
 */
export default useInterpolation;
