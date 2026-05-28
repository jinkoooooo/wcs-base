/* src/views/tspg-4way-shuttle/dashboard_2d/composables/useCanvasViewport.ts */

import { computed, ref } from 'vue';

export function useCanvasViewport({
  wrapperRef,
  canvasWidth,
  canvasHeight,
}: {
  wrapperRef: any; // ref<HTMLElement|null>
  canvasWidth: any; // computed<number>
  canvasHeight: any; // computed<number>
}) {
  const scale = ref(1);
  const offsetX = ref(0);
  const offsetY = ref(0);

  // 추가: 현재 스케일 기준으로 캔버스를 wrapper 중앙에 배치
  const centerToView = (targetScale = scale.value) => {
    const el = wrapperRef.value as HTMLElement | null;
    if (!el) return;

    // rect보다 clientWidth/Height가 레이아웃 기준으로 더 예측 가능
    const w = el.clientWidth || el.getBoundingClientRect().width;
    const h = el.clientHeight || el.getBoundingClientRect().height;

    const cw = canvasWidth.value * targetScale;
    const ch = canvasHeight.value * targetScale;

    // 캔버스 좌상단이 wrapper 중앙정렬되도록 translate 값 설정
    offsetX.value = (w - cw) / 2;
    offsetY.value = (h - ch) / 2;
  };

  const canvasTransformStyle = computed(() => ({
    width: `${canvasWidth.value}px`,
    height: `${canvasHeight.value}px`,
    transform: `translate(${offsetX.value}px, ${offsetY.value}px) scale(${scale.value})`,
    transformOrigin: 'left top',
  }));

  const panBy = (dx: number, dy: number) => {
    offsetX.value += dx;
    offsetY.value += dy;
  };

  const zoomAt = (clientX: number, clientY: number, nextScale: number) => {
    const el = wrapperRef.value as HTMLElement | null;
    if (!el) return;

    const rect = el.getBoundingClientRect();

    // 현재 마우스 위치가 가리키는 캔버스 좌표(줌 전)
    const beforeX = (clientX - rect.left - offsetX.value) / scale.value;
    const beforeY = (clientY - rect.top - offsetY.value) / scale.value;

    // 스케일 변경
    const prevScale = scale.value;
    scale.value = Math.max(0.05, Math.min(6, nextScale));

    // 줌 후에도 같은 캔버스 점이 마우스 아래에 오도록 offset 보정
    offsetX.value = clientX - rect.left - beforeX * scale.value;
    offsetY.value = clientY - rect.top - beforeY * scale.value;

    // (선택) 너무 큰 드리프트 방지용 클램프가 있다면 여기서 처리
  };

  // 수정: 100%는 좌상단 고정이 아니라 “중앙 정렬된 100%”로
  const resetView = () => {
    scale.value = 1;
    centerToView(1);
  };

  // 수정: fit도 계산 후 중앙 정렬
  const fitToView = () => {
    const el = wrapperRef.value as HTMLElement | null;
    if (!el) return;

    const w = el.clientWidth || el.getBoundingClientRect().width;
    const h = el.clientHeight || el.getBoundingClientRect().height;

    // 약간의 여백(선택)
    const padding = 24;
    const sx = (w - padding * 2) / canvasWidth.value;
    const sy = (h - padding * 2) / canvasHeight.value;

    const next = Math.max(0.05, Math.min(6, Math.min(sx, sy)));
    scale.value = next;
    centerToView(next);
  };

  // 이미 쓰고 있는 함수면 그대로 두고, 없다면 아래처럼 유지
  const toCanvasXY = (clientX: number, clientY: number) => {
    const el = wrapperRef.value as HTMLElement | null;
    if (!el) return { x: 0, yBottom: 0 };
    const rect = el.getBoundingClientRect();

    const x = (clientX - rect.left - offsetX.value) / scale.value;

    // EditorCenterPanel이 bottom 좌표계를 쓰니 yBottom으로 반환
    const yFromTop = (clientY - rect.top - offsetY.value) / scale.value;
    const yBottom = canvasHeight.value - yFromTop;

    return { x, yBottom };
  };

  // 기존 bindNonPassiveWheel 등은 그대로 유지
  const bindNonPassiveWheel = (handler: (e: WheelEvent) => void) => {
    const el = wrapperRef.value as HTMLElement | null;
    if (!el) return;
    el.addEventListener('wheel', handler as any, { passive: false });
  };

  return {
    scale,
    canvasTransformStyle,
    panBy,
    zoomAt,
    resetView,
    fitToView,
    toCanvasXY,
    bindNonPassiveWheel,

    // 외부에서 “중앙 정렬만” 호출하고 싶을 때도 쓸 수 있게 export
    centerToView,
  };
}
