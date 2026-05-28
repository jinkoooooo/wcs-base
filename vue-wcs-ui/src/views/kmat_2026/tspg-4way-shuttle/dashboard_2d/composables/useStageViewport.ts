/**
 * useStageViewport.ts
 * 2D 캔버스 뷰포트 관리 컴포저블
 *
 * ============================================
 * 개요 (비개발자용 설명)
 * ============================================
 *
 * 이 파일은 2D 대시보드의 "카메라" 역할을 담당합니다.
 * 마치 구글 맵스에서 지도를 확대/축소하고 이동하는 것처럼,
 * 물류센터 레이아웃을 확대/축소(줌)하고 이동(팬)할 수 있게 해줍니다.
 *
 * [주요 기능]
 * 1. 줌 인/아웃: 마우스 휠 또는 +/- 버튼으로 확대/축소
 * 2. 팬(이동): 마우스 드래그 또는 스페이스바+드래그로 화면 이동
 * 3. 화면 맞춤: 전체 레이아웃이 화면에 딱 맞게 자동 조정
 * 4. 미니맵 연동: 현재 보고 있는 영역 정보 제공
 *
 * [용어 설명]
 * - 줌(Zoom): 확대/축소 배율 (1.0 = 100%, 2.0 = 200%)
 * - 팬(Pan): 화면을 상하좌우로 이동하는 것
 * - 뷰포트(Viewport): 화면에 보이는 영역
 * - 월드 좌표(World Coords): 캔버스 원본 좌표 (줌 적용 전)
 * - 스크린 좌표(Screen Coords): 실제 화면상의 픽셀 좌표
 *
 * ============================================
 * 좌표계 설명
 * ============================================
 *
 * [월드 좌표 → 스크린 좌표 변환]
 * screenX = worldX × scale + panX
 * screenY = worldY × scale + panY
 *
 * [스크린 좌표 → 월드 좌표 변환]
 * worldX = (screenX - panX) / scale
 * worldY = (screenY - panY) / scale
 *
 * ============================================
 * 사용 방법
 * ============================================
 *
 * ```typescript
 * const viewport = useStageViewport({
 *   containerRef,        // 캔버스 컨테이너 DOM 참조
 *   pageWidth,           // 페이지 너비 (월드 좌표)
 *   pageHeight,          // 페이지 높이 (월드 좌표)
 *   fitPadding: 0.95,    // 화면 맞춤 시 여백 비율
 *   minZoom: 0.2,        // 최소 줌 (20%)
 *   maxZoom: 6,          // 최대 줌 (600%)
 *   clampMarginPx: 140,  // 화면 밖으로 나갈 수 있는 여백 (픽셀)
 * });
 *
 * // 템플릿에서 사용
 * <div
 *   @wheel.prevent="viewport.onWheel"
 *   @mousedown="startDrag"
 * >
 *   <div :style="{
 *     transform: `translate(${viewport.panX.value}px, ${viewport.panY.value}px) scale(${viewport.scale.value})`
 *   }">
 *     <!-- 캔버스 내용 -->
 *   </div>
 * </div>
 * ```
 */

import { computed, ref, type Ref } from 'vue';

// ============================================
// 타입 정의
// ============================================

/**
 * 뷰포트의 월드 좌표 영역
 *
 * 현재 화면에 보이는 영역을 월드 좌표로 표현합니다.
 * 미니맵에서 현재 보고 있는 위치를 표시할 때 사용합니다.
 *
 * @property x - 뷰포트 왼쪽 상단 X 좌표 (월드 좌표)
 * @property y - 뷰포트 왼쪽 상단 Y 좌표 (월드 좌표)
 * @property w - 뷰포트 너비 (월드 좌표)
 * @property h - 뷰포트 높이 (월드 좌표)
 */
export type ViewportWorldRect = {
  /** 뷰포트 왼쪽 상단 X 좌표 (월드 좌표, 원점: 좌상단) */
  x: number;
  /** 뷰포트 왼쪽 상단 Y 좌표 (월드 좌표, 원점: 좌상단) */
  y: number;
  /** 뷰포트 너비 (월드 좌표) */
  w: number;
  /** 뷰포트 높이 (월드 좌표) */
  h: number;
};

/**
 * 뷰포트 재계산 옵션
 */
type RecalcOptions = {
  /**
   * true: 현재 보고 있는 월드 좌표 중심점 유지
   * false: 페이지 중앙으로 리셋
   */
  keepWorldCenter?: boolean;
};

/**
 * 2D 스테이지 뷰포트 관리 컴포저블
 *
 * @param params.containerRef - 캔버스를 감싸는 컨테이너 DOM 참조
 * @param params.pageWidth - 페이지 너비 (월드 좌표)
 * @param params.pageHeight - 페이지 높이 (월드 좌표)
 * @param params.fitPadding - 화면 맞춤 시 여백 비율 (기본: 0.95 = 5% 여백)
 * @param params.minZoom - 최소 줌 배율 (기본: 0.2 = 20%)
 * @param params.maxZoom - 최대 줌 배율 (기본: 6 = 600%)
 * @param params.clampMarginPx - 화면 밖 스크롤 허용 여백 (기본: 120px)
 * @returns 뷰포트 상태와 제어 함수들
 */
export function useStageViewport(params: {
  /** 캔버스 컨테이너 DOM 참조 */
  containerRef: Ref<HTMLElement | null>;
  /** 페이지 너비 (월드 좌표) */
  pageWidth: Ref<number>;
  /** 페이지 높이 (월드 좌표) */
  pageHeight: Ref<number>;
  /** 화면 맞춤 시 여백 비율 (예: 0.95 = 5% 여백) */
  fitPadding?: number;
  /** 최소 줌 배율 (fitScale에 곱해짐) */
  minZoom?: number;
  /** 최대 줌 배율 */
  maxZoom?: number;
  /** 화면 밖 스크롤 허용 여백 (픽셀) */
  clampMarginPx?: number;
}) {
  // ============================================
  // 파라미터 기본값 설정
  // ============================================

  /** 화면 맞춤 시 여백 비율 (0.95 = 화면의 95%를 사용) */
  const fitPadding = params.fitPadding ?? 0.95;

  /** 최소 줌 배율 (0.2 = 20%) */
  const minZoom = params.minZoom ?? 0.2;

  /** 최대 줌 배율 (6 = 600%) */
  const maxZoom = params.maxZoom ?? 6;

  /** 화면 밖으로 스크롤 허용되는 여백 (픽셀) */
  const clampMargin = params.clampMarginPx ?? 120;

  // ============================================
  // 뷰포트 상태 (State)
  // ============================================

  /**
   * 사용자 줌 배율
   * - 1.0 = 100% (fitScale 기준)
   * - fitScale에 곱해져서 최종 scale 결정
   * - 예: fitScale=0.5, zoom=2.0 → scale=1.0
   */
  const zoom = ref(1);

  /**
   * 화면 맞춤 기준 스케일
   * - 페이지 전체가 화면에 맞게 들어오는 스케일
   * - 컨테이너 크기와 페이지 크기에 따라 자동 계산
   */
  const fitScale = ref(1);

  /**
   * 수평 이동(팬) 값 (스크린 픽셀 단위)
   * - 양수: 캔버스가 오른쪽으로 이동 (왼쪽 공간 생김)
   * - 음수: 캔버스가 왼쪽으로 이동 (오른쪽 공간 생김)
   */
  const panX = ref(0);

  /**
   * 수직 이동(팬) 값 (스크린 픽셀 단위)
   * - 양수: 캔버스가 아래로 이동 (위쪽 공간 생김)
   * - 음수: 캔버스가 위로 이동 (아래쪽 공간 생김)
   */
  const panY = ref(0);

  /**
   * 최종 스케일 (fitScale × zoom)
   * - CSS transform의 scale() 값으로 사용
   * - 1.0 = 원본 크기, 2.0 = 2배 확대
   */
  const scale = computed(() => fitScale.value * zoom.value);

  // ============================================
  // 팬(드래그) 상태
  // ============================================

  /** 현재 팬(드래그) 중인지 여부 */
  const isPanning = ref(false);

  /** 팬 시작 시점의 좌표 (드래그 거리 계산용) */
  const panStart = ref({ x: 0, y: 0, panX: 0, panY: 0 });

  /** 팬 중에 실제로 이동했는지 여부 (클릭과 드래그 구분용) */
  const panMoved = ref(false);

  // ============================================
  // 클릭 가드 (드래그 후 클릭 방지)
  // ============================================

  /**
   * 클릭을 무시해야 하는 시간 (타임스탬프)
   * - 드래그가 끝난 직후 발생하는 click 이벤트를 무시하기 위함
   * - 드래그 후 180ms 동안 클릭 이벤트 무시
   */
  const clickGuardUntil = ref(0);

  /**
   * 현재 클릭을 무시해야 하는지 확인
   * - 드래그 직후에는 true 반환
   * - 설비/셔틀 클릭 핸들러에서 먼저 호출하여 확인
   */
  const shouldIgnoreClick = () => Date.now() < clickGuardUntil.value;

  // ============================================
  // 유틸리티 함수
  // ============================================

  /**
   * 컨테이너의 현재 크기와 위치를 가져옵니다.
   *
   * @returns 컨테이너 정보 또는 null (컨테이너가 없는 경우)
   */
  const getContainerRect = () => {
    const el = params.containerRef.value;
    if (!el) return null;
    const r = el.getBoundingClientRect();
    return { w: r.width, h: r.height, left: r.left, top: r.top };
  };

  /**
   * 팬 값이 허용 범위를 벗어나지 않도록 제한합니다.
   *
   * [동작 방식]
   * - 콘텐츠가 컨테이너보다 작으면: 중앙에 고정
   * - 콘텐츠가 컨테이너보다 크면: clampMargin만큼 여백 허용
   *
   * @param nextPanX - 적용하려는 X 팬 값
   * @param nextPanY - 적용하려는 Y 팬 값
   * @returns 제한된 팬 값 { x, y }
   */
  const clampPan = (nextPanX: number, nextPanY: number) => {
    const rect = getContainerRect();
    if (!rect) return { x: nextPanX, y: nextPanY };

    const pageW = Math.max(1, Number(params.pageWidth.value || 1));
    const pageH = Math.max(1, Number(params.pageHeight.value || 1));
    const s = scale.value;

    const contentW = pageW * s;
    const contentH = pageH * s;

    // If content smaller than container -> force center
    const centerX = (rect.w - contentW) / 2;
    const centerY = (rect.h - contentH) / 2;

    let minX: number, maxX: number;
    let minY: number, maxY: number;

    if (contentW <= rect.w) {
      minX = maxX = centerX;
    } else {
      minX = rect.w - contentW - clampMargin;
      maxX = clampMargin;
    }

    if (contentH <= rect.h) {
      minY = maxY = centerY;
    } else {
      minY = rect.h - contentH - clampMargin;
      maxY = clampMargin;
    }

    const x = Math.min(Math.max(nextPanX, minX), maxX);
    const y = Math.min(Math.max(nextPanY, minY), maxY);
    return { x, y };
  };

  // ============================================
  // 줌/팬 제어 함수
  // ============================================

  /**
   * 화면 맞춤 스케일을 재계산합니다.
   *
   * 컨테이너 크기가 변경되었을 때 (윈도우 리사이즈 등) 호출합니다.
   * fitScale을 다시 계산하고 팬 위치를 조정합니다.
   *
   * @param opts.keepWorldCenter - true면 현재 보고 있는 월드 좌표 유지
   *
   * @example
   * // 윈도우 리사이즈 이벤트
   * window.addEventListener('resize', () => {
   *   viewport.recalculateFit({ keepWorldCenter: true });
   * });
   */
  const recalculateFit = (opts: RecalcOptions = {}) => {
    const rect = getContainerRect();
    if (!rect) return;

    const pageW = Number(params.pageWidth.value || 0);
    const pageH = Number(params.pageHeight.value || 0);
    if (!(pageW > 0 && pageH > 0 && rect.w > 0 && rect.h > 0)) {
      fitScale.value = 1;
      zoom.value = 1;
      panX.value = 0;
      panY.value = 0;
      return;
    }

    // keep current world center (optional)
    let worldCenterX = pageW / 2;
    let worldCenterY = pageH / 2;

    if (opts.keepWorldCenter) {
      const sOld = scale.value || 1;
      worldCenterX = (rect.w / 2 - panX.value) / sOld;
      worldCenterY = (rect.h / 2 - panY.value) / sOld;
    }

    const nextFit = Math.min(rect.w / pageW, rect.h / pageH) * fitPadding;
    fitScale.value = Number.isFinite(nextFit) && nextFit > 0 ? nextFit : 1;

    // adjust pan to keep world center
    const sNew = scale.value || 1;
    const nextPanX = rect.w / 2 - worldCenterX * sNew;
    const nextPanY = rect.h / 2 - worldCenterY * sNew;

    const clamped = clampPan(nextPanX, nextPanY);
    panX.value = clamped.x;
    panY.value = clamped.y;
  };

  /**
   * 페이지 전체가 화면에 맞게 줌과 팬을 조정합니다.
   *
   * - zoom을 1.0으로 리셋
   * - 페이지 전체가 화면 중앙에 표시되도록 팬 조정
   * - "FIT" 버튼 클릭 시 또는 페이지 변경 시 호출
   *
   * @example
   * // FIT 버튼 클릭
   * <button @click="viewport.fitToPage()">FIT</button>
   */
  const fitToPage = () => {
    zoom.value = 1;
    recalculateFit({ keepWorldCenter: false });

    const rect = getContainerRect();
    if (!rect) return;

    const pageW = Math.max(1, Number(params.pageWidth.value || 1));
    const pageH = Math.max(1, Number(params.pageHeight.value || 1));
    const s = scale.value;

    const nextPanX = (rect.w - pageW * s) / 2;
    const nextPanY = (rect.h - pageH * s) / 2;
    const clamped = clampPan(nextPanX, nextPanY);
    panX.value = clamped.x;
    panY.value = clamped.y;
  };

  /**
   * 지정된 줌 레벨로 줌을 변경합니다.
   *
   * 마우스 포인터 위치를 기준으로 줌이 적용되어,
   * 마우스 아래의 월드 좌표가 줌 후에도 같은 스크린 위치에 있게 됩니다.
   *
   * [동작 원리]
   * 1. 마우스 위치의 월드 좌표 계산
   * 2. 줌 적용
   * 3. 계산한 월드 좌표가 같은 스크린 위치에 오도록 팬 조정
   *
   * @param nextZoom - 적용할 줌 레벨
   * @param clientX - 줌 중심 X (마우스 스크린 X 좌표)
   * @param clientY - 줌 중심 Y (마우스 스크린 Y 좌표)
   */
  const zoomTo = (nextZoom: number, clientX: number, clientY: number) => {
    const rect = getContainerRect();
    if (!rect) return;

    const z = Math.min(Math.max(nextZoom, minZoom), maxZoom);
    const sOld = scale.value || 1;

    const px = clientX - rect.left;
    const py = clientY - rect.top;

    // world under pointer
    const wx = (px - panX.value) / sOld;
    const wy = (py - panY.value) / sOld;

    zoom.value = z;

    const sNew = scale.value || 1;

    const nextPanX = px - wx * sNew;
    const nextPanY = py - wy * sNew;

    const clamped = clampPan(nextPanX, nextPanY);
    panX.value = clamped.x;
    panY.value = clamped.y;
  };

  /**
   * 줌 인 (확대)
   *
   * 현재 줌의 115%로 확대합니다.
   * 마우스 위치가 지정되지 않으면 화면 중앙 기준으로 줌합니다.
   *
   * @param clientX - 줌 중심 X (생략 시 화면 중앙)
   * @param clientY - 줌 중심 Y (생략 시 화면 중앙)
   */
  const zoomIn = (clientX?: number, clientY?: number) => {
    const rect = getContainerRect();
    if (!rect) return;
    const cx = clientX ?? rect.left + rect.w / 2;
    const cy = clientY ?? rect.top + rect.h / 2;
    zoomTo(zoom.value * 1.15, cx, cy);
  };

  /**
   * 줌 아웃 (축소)
   *
   * 현재 줌의 약 87%로 축소합니다 (1/1.15).
   * 마우스 위치가 지정되지 않으면 화면 중앙 기준으로 줌합니다.
   *
   * @param clientX - 줌 중심 X (생략 시 화면 중앙)
   * @param clientY - 줌 중심 Y (생략 시 화면 중앙)
   */
  const zoomOut = (clientX?: number, clientY?: number) => {
    const rect = getContainerRect();
    if (!rect) return;
    const cx = clientX ?? rect.left + rect.w / 2;
    const cy = clientY ?? rect.top + rect.h / 2;
    zoomTo(zoom.value / 1.15, cx, cy);
  };

  /**
   * 줌을 100%로 리셋합니다.
   *
   * - zoom을 1.0으로 설정
   * - 현재 보고 있는 월드 좌표 중심점 유지
   * - "100%" 버튼 클릭 시 호출
   */
  const resetZoom100 = () => {
    const rect = getContainerRect();
    if (!rect) return;

    // keep current world center
    const sOld = scale.value || 1;
    const worldCenterX = (rect.w / 2 - panX.value) / sOld;
    const worldCenterY = (rect.h / 2 - panY.value) / sOld;

    zoom.value = 1;

    const sNew = scale.value || 1;
    const nextPanX = rect.w / 2 - worldCenterX * sNew;
    const nextPanY = rect.h / 2 - worldCenterY * sNew;

    const clamped = clampPan(nextPanX, nextPanY);
    panX.value = clamped.x;
    panY.value = clamped.y;
  };

  /**
   * 지정된 월드 좌표가 화면 중앙에 오도록 팬을 조정합니다.
   *
   * 미니맵에서 특정 위치를 클릭했을 때,
   * 해당 위치가 메인 뷰의 중앙에 오도록 이동합니다.
   *
   * @param worldX - 중앙에 표시할 월드 X 좌표
   * @param worldY - 중앙에 표시할 월드 Y 좌표
   *
   * @example
   * // 미니맵 클릭 이벤트
   * <MiniMap @center="({ worldX, worldY }) => viewport.centerOnWorld(worldX, worldY)" />
   */
  const centerOnWorld = (worldX: number, worldY: number) => {
    const rect = getContainerRect();
    if (!rect) return;

    const s = scale.value || 1;
    const nextPanX = rect.w / 2 - worldX * s;
    const nextPanY = rect.h / 2 - worldY * s;

    const clamped = clampPan(nextPanX, nextPanY);
    panX.value = clamped.x;
    panY.value = clamped.y;
  };

  /**
   * 현재 뷰포트의 월드 좌표 영역을 계산합니다.
   *
   * 미니맵에서 현재 보고 있는 영역을 표시할 때 사용합니다.
   *
   * @returns 뷰포트 영역 (월드 좌표) 또는 null
   */
  const getViewportWorldRect = (): ViewportWorldRect | null => {
    const rect = getContainerRect();
    if (!rect) return null;

    const pageW = Math.max(1, Number(params.pageWidth.value || 1));
    const pageH = Math.max(1, Number(params.pageHeight.value || 1));
    const s = scale.value || 1;

    const x = -panX.value / s;
    const y = -panY.value / s;
    const w = rect.w / s;
    const h = rect.h / s;

    // clamp to page
    const cx = Math.min(Math.max(x, 0), pageW);
    const cy = Math.min(Math.max(y, 0), pageH);

    const cw = Math.min(w, pageW - cx);
    const ch = Math.min(h, pageH - cy);

    return { x: cx, y: cy, w: cw, h: ch };
  };

  // ============================================
  // 이벤트 핸들러
  // ============================================

  /**
   * 마우스 휠 이벤트 핸들러 (줌)
   *
   * 마우스 휠을 굴리면 마우스 포인터 위치를 기준으로 줌이 적용됩니다.
   * 휠 위로 → 줌 인 (확대)
   * 휠 아래로 → 줌 아웃 (축소)
   *
   * @param e - 휠 이벤트
   *
   * @example
   * <div @wheel.prevent="viewport.onWheel">...</div>
   */
  const onWheel = (e: WheelEvent) => {
    e.preventDefault();

    // 휠 deltaY를 부드러운 줌 계수로 변환
    // 지수 함수를 사용하여 자연스러운 줌 느낌 제공
    const delta = e.deltaY;
    const factor = Math.exp(-delta * 0.0012);
    const next = zoom.value * factor;

    zoomTo(next, e.clientX, e.clientY);
  };

  /**
   * 팬(드래그) 시작
   *
   * 마우스 버튼을 눌렀을 때 호출합니다.
   * 시작 위치를 기록하여 이후 드래그 거리를 계산합니다.
   *
   * @param clientX - 마우스 스크린 X 좌표
   * @param clientY - 마우스 스크린 Y 좌표
   *
   * @example
   * function onMouseDown(e: MouseEvent) {
   *   if (e.button === 1 || (e.button === 0 && spaceDown)) {
   *     viewport.startPan(e.clientX, e.clientY);
   *   }
   * }
   */
  const startPan = (clientX: number, clientY: number) => {
    isPanning.value = true;
    panMoved.value = false;
    panStart.value = { x: clientX, y: clientY, panX: panX.value, panY: panY.value };
  };

  /**
   * 팬(드래그) 이동 중
   *
   * 마우스를 드래그하는 동안 호출합니다.
   * 시작 위치로부터의 이동 거리를 계산하여 팬 값을 업데이트합니다.
   *
   * @param clientX - 현재 마우스 스크린 X 좌표
   * @param clientY - 현재 마우스 스크린 Y 좌표
   *
   * @example
   * window.addEventListener('mousemove', (e) => viewport.movePan(e.clientX, e.clientY));
   */
  const movePan = (clientX: number, clientY: number) => {
    if (!isPanning.value) return;

    // 드래그 거리 계산
    const dx = clientX - panStart.value.x;
    const dy = clientY - panStart.value.y;

    // 2픽셀 이상 이동하면 실제 드래그로 인정
    if (Math.abs(dx) > 2 || Math.abs(dy) > 2) panMoved.value = true;

    // 새 팬 값 계산 및 적용
    const nextX = panStart.value.panX + dx;
    const nextY = panStart.value.panY + dy;

    const clamped = clampPan(nextX, nextY);
    panX.value = clamped.x;
    panY.value = clamped.y;
  };

  /**
   * 팬(드래그) 종료
   *
   * 마우스 버튼을 놓았을 때 호출합니다.
   * 드래그가 발생했으면 직후의 클릭 이벤트를 무시하도록 설정합니다.
   *
   * @example
   * window.addEventListener('mouseup', () => viewport.endPan());
   */
  const endPan = () => {
    if (!isPanning.value) return;
    isPanning.value = false;

    // 드래그가 있었으면 180ms 동안 클릭 이벤트 무시
    // (드래그 종료 시 발생하는 불필요한 click 이벤트 방지)
    if (panMoved.value) {
      clickGuardUntil.value = Date.now() + 180;
    }
  };

  // ============================================
  // 반환 (Return)
  // ============================================

  return {
    // ============================================
    // 상태 (반응성 ref들)
    // ============================================

    /** 사용자 줌 배율 (1.0 = 100%) */
    zoom,

    /** 화면 맞춤 기준 스케일 */
    fitScale,

    /** 수평 이동(팬) 값 (스크린 픽셀) */
    panX,

    /** 수직 이동(팬) 값 (스크린 픽셀) */
    panY,

    /** 최종 스케일 (fitScale × zoom) - CSS transform에 사용 */
    scale,

    /** 현재 팬(드래그) 중인지 여부 */
    isPanning,

    // ============================================
    // 가드 함수
    // ============================================

    /** 현재 클릭을 무시해야 하는지 확인 (드래그 직후 클릭 방지) */
    shouldIgnoreClick,

    // ============================================
    // 제어 함수
    // ============================================

    /** 화면 맞춤 스케일 재계산 (리사이즈 시 호출) */
    recalculateFit,

    /** 페이지 전체가 화면에 맞게 줌/팬 조정 */
    fitToPage,

    /** 지정된 줌 레벨로 변경 (마우스 위치 기준) */
    zoomTo,

    /** 줌 인 (15% 확대) */
    zoomIn,

    /** 줌 아웃 (15% 축소) */
    zoomOut,

    /** 줌을 100%로 리셋 */
    resetZoom100,

    /** 지정된 월드 좌표를 화면 중앙으로 이동 */
    centerOnWorld,

    // ============================================
    // 뷰포트 정보
    // ============================================

    /** 현재 뷰포트의 월드 좌표 영역 (미니맵용) */
    getViewportWorldRect,

    // ============================================
    // 이벤트 핸들러
    // ============================================

    /** 마우스 휠 핸들러 (줌) */
    onWheel,

    /** 팬(드래그) 시작 */
    startPan,

    /** 팬(드래그) 이동 */
    movePan,

    /** 팬(드래그) 종료 */
    endPan,
  };
}
