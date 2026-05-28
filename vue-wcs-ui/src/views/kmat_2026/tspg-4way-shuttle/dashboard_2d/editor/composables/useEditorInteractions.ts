/* vue-wcs-ui/src/views/kmat_2026/tspg-4way-shuttle/dashboard_2d/editor/composables/useEditorInteractions.ts */

import { computed, ref, type ComputedRef, type Ref } from 'vue';
import type { TbEcs2dItem } from '../../api/types';
import { clamp, intersectsRect } from '../utils/editorGeom';
import { snapToObjectEdges } from '../utils/editorSnap';

export function useEditorInteractions(args: {
  wrapperRef: Ref<HTMLElement | null>;
  viewport: {
    scale: Ref<number>;
    panBy: (dx: number, dy: number) => void;
    toCanvasXY: (clientX: number, clientY: number) => { x: number; yBottom: number };
  };
  objects: ComputedRef<TbEcs2dItem[]>;
  canvasWidth: ComputedRef<number>;
  canvasHeight: ComputedRef<number>;
  showGrid: Ref<boolean>;
  gridSize: Ref<number>;
  snapEnabled: Ref<boolean>;
  smartGuides: Ref<boolean>;
  canvasScale: Ref<number>;
  selectedIds: Ref<string[]>;
  primaryId: Ref<string | null>;
  isSelected: (id: string) => boolean;
  clear: () => void;
  selectSingle: (id: string) => void;
  toggle: (id: string) => void;
  setMany: (ids: string[], opt: { keepExisting: boolean }) => void;
  emitUpdate: (id: string, updates: Partial<TbEcs2dItem>) => void;
  emitSelect: (id: string | null) => void;
}) {

  // ==========================================
  // 0. 회전 영역 계산 도우미 (Bounding Box)
  // ==========================================
  const getRotatedOffsets = (w: number, h: number, rot: number) => {
    const rad = (rot || 0) * (Math.PI / 180);
    const cos = Math.cos(rad);
    const sin = Math.sin(rad);

    const cx = w / 2;
    const cy = h / 2;

    const pts = [
      { x: 0, y: 0 },
      { x: w, y: 0 },
      { x: w, y: h },
      { x: 0, y: h },
    ].map(p => {
      const tx = p.x - cx;
      const ty = p.y - cy;
      const rx = tx * cos + ty * sin;
      const ry = -tx * sin + ty * cos;
      return { x: rx + cx, y: ry + cy };
    });

    return {
      minX: Math.min(...pts.map(p => p.x)),
      maxX: Math.max(...pts.map(p => p.x)),
      minY: Math.min(...pts.map(p => p.y)),
      maxY: Math.max(...pts.map(p => p.y)),
    };
  };

  // ==========================================
  // 1. 상태 관리
  // ==========================================
  type ActionType = 'none' | 'drag' | 'resize' | 'rotate' | 'marquee' | 'pan';
  const action = ref<ActionType>('none');
  const hasDragged = ref(false);
  const suppressCanvasClick = ref(false);

  let cachedSnapTargets: TbEcs2dItem[] = [];

  const marquee = ref({ active: false, additive: false, left: 0, bottom: 0, width: 0, height: 0 });
  const marqueeStart = { x: 0, y: 0 };
  const marqueeStyle = computed(() => ({
    left: marquee.value.left + 'px',
    bottom: marquee.value.bottom + 'px',
    width: marquee.value.width + 'px',
    height: marquee.value.height + 'px',
  }));

  const guideLineX = ref<number | null>(null);
  const guideLineY = ref<number | null>(null);

  // dragInfo.dir: 화면상 핸들 방향 (직각 회전 시 화면 좌표계 기반 리사이즈에 사용)
  // dragInfo.localDir: 객체 로컬 핸들 방향 (자유 각도 회전 시 사용)
  const dragInfo = { mouseX: 0, mouseY: 0, dir: '', localDir: '' };

  const startPrimary = {
    x: 0, y: 0, w: 0, h: 0, rot: 0,
    anchorCx: 0, anchorCy: 0,
  };
  const startById = new Map<string, { x: number; y: number }>();

  const panStart = { x: 0, y: 0 };

  // ==========================================
  // 2. 공통 도우미
  // ==========================================

  // 90도 단위 회전 여부
  const isOrthogonalRotation = (rot: number): boolean => {
    const norm = ((rot % 360) + 360) % 360;
    const nearest = Math.round(norm / 90) * 90;
    return Math.abs(norm - nearest) < 0.5;
  };

  // ✅ 로컬 방향 → 화면 방향 매핑 (시계 90도)
  // Canvas.vue 의 핸들은 객체 div 내부에 절대 위치로 박혀 있어 객체와 함께 회전됨.
  // 즉 startResize 에 들어오는 direction 은 "객체 로컬 방향" (DOM 클래스명).
  // CSS rotate(+deg) 는 시계방향 → 로컬 N 위치가 화면 E 위치로 이동.
  // (getHandleCursor 의 cw90 매핑과 동일)
  const localDirToScreenDir = (localDir: string, rotation: number): string => {
    const norm = ((Math.round(rotation / 90) * 90) % 360 + 360) % 360;
    const cw90: Record<string, string> = {
      n: 'e', e: 's', s: 'w', w: 'n',
      ne: 'se', se: 'sw', sw: 'nw', nw: 'ne',
    };
    let screenDir = localDir;
    const steps = norm / 90;
    for (let i = 0; i < steps; i++) {
      screenDir = cw90[screenDir] || screenDir;
    }
    return screenDir;
  };

  const initInteraction = (
    e: MouseEvent,
    obj: TbEcs2dItem,
    type: ActionType,
    localDir = '',
  ) => {
    action.value = type;
    hasDragged.value = false;
    dragInfo.mouseX = e.clientX;
    dragInfo.mouseY = e.clientY;
    dragInfo.dir = '';
    dragInfo.localDir = localDir;

    startPrimary.x = obj.posX;
    startPrimary.y = obj.posY;
    startPrimary.w = obj.width || 0;
    startPrimary.h = obj.height || 0;
    startPrimary.rot = obj.rotation || 0;

    if (type === 'resize' && localDir) {
      const rot = startPrimary.rot;
      const isOrtho = isOrthogonalRotation(rot);

      if (isOrtho) {
        // ✅ 직각 회전: 로컬 → 화면 방향 변환 후 화면 좌표계 기반 리사이즈
        const screenDir = localDirToScreenDir(localDir, rot);
        dragInfo.dir = screenDir;

        // 화면 AABB 의 반대편을 앵커로
        const offsets = getRotatedOffsets(startPrimary.w, startPrimary.h, rot);
        const aabbLeft   = obj.posX + offsets.minX;
        const aabbRight  = obj.posX + offsets.maxX;
        const aabbBottom = obj.posY + offsets.minY;
        const aabbTop    = obj.posY + offsets.maxY;

        let aCx: number;
        let aCy: number;
        if (screenDir.includes('e')) aCx = aabbLeft;
        else if (screenDir.includes('w')) aCx = aabbRight;
        else aCx = (aabbLeft + aabbRight) / 2;

        if (screenDir.includes('n')) aCy = aabbBottom;
        else if (screenDir.includes('s')) aCy = aabbTop;
        else aCy = (aabbBottom + aabbTop) / 2;

        startPrimary.anchorCx = aCx;
        startPrimary.anchorCy = aCy;

        // 그리드 정렬
        if (args.snapEnabled.value && args.showGrid.value && args.gridSize.value > 0) {
          const g = args.gridSize.value;
          startPrimary.anchorCx = Math.round(startPrimary.anchorCx / g) * g;
          startPrimary.anchorCy = Math.round(startPrimary.anchorCy / g) * g;
        }
      } else {
        // 자유 각도: 로컬 방향 그대로 사용, 로컬 좌표계 앵커
        const sx = localDir.includes('e') ? -1 : (localDir.includes('w') ? 1 : 0);
        const sy = localDir.includes('n') ? -1 : (localDir.includes('s') ? 1 : 0);

        const cx0 = obj.posX + (obj.width || 0) / 2;
        const cy0 = obj.posY + (obj.height || 0) / 2;

        const localAX = sx * startPrimary.w / 2;
        const localAY = sy * startPrimary.h / 2;

        const rad = (rot * Math.PI) / 180;
        const cos = Math.cos(rad);
        const sin = Math.sin(rad);

        startPrimary.anchorCx = cx0 + (localAX * cos + localAY * sin);
        startPrimary.anchorCy = cy0 + (-localAX * sin + localAY * cos);
      }
    } else {
      startPrimary.anchorCx = 0;
      startPrimary.anchorCy = 0;
    }

    // startById, cachedSnapTargets
    startById.clear();
    const idsToTrack = new Set(args.selectedIds.value);
    idsToTrack.add(obj.id!);

    for (const id of idsToTrack) {
      const o = args.objects.value.find((x) => x.id === id);
      if (o) startById.set(id, { x: o.posX, y: o.posY });
    }

    cachedSnapTargets = args.objects.value.filter((o) => {
      if (idsToTrack.has(o.id!) || o.isLocked) return false;
      const dist = Math.abs(o.posX - obj.posX) + Math.abs(o.posY - obj.posY);
      return dist < 3000;
    });
  };

  // ==========================================
  // 3. 마우스 다운
  // ==========================================
  const handleObjectMouseDown = (e: MouseEvent, obj: TbEcs2dItem) => {
    if (obj.isLocked || e.button !== 0) return;

    if (e.shiftKey) {
      args.toggle(obj.id!);
      args.emitSelect(args.primaryId.value);
    } else if (!args.isSelected(obj.id!)) {
      args.selectSingle(obj.id!);
      args.emitSelect(obj.id!);
    }

    initInteraction(e, obj, 'drag');
  };

  const handleCanvasMouseDown = (e: MouseEvent) => {
    if (e.button !== 0 || action.value !== 'none') return;
    if ((e.target as HTMLElement).closest('.canvas-object')) return;

    action.value = 'marquee';
    suppressCanvasClick.value = true;

    const { x, yBottom } = args.viewport.toCanvasXY(e.clientX, e.clientY);
    marqueeStart.x = x;
    marqueeStart.y = yBottom;

    marquee.value.active = true;
    marquee.value.additive = e.shiftKey;
    marquee.value.left = x;
    marquee.value.bottom = yBottom;
    marquee.value.width = 0;
    marquee.value.height = 0;
  };

  const handlePanStart = (e: MouseEvent) => {
    action.value = 'pan';
    panStart.x = e.clientX;
    panStart.y = e.clientY;
    suppressCanvasClick.value = true;
  };

  const handleObjectClick = (e: MouseEvent, obj: TbEcs2dItem) => {
    if (hasDragged.value) {
      hasDragged.value = false;
      return;
    }
    if (action.value !== 'none') return;

    if (e.shiftKey) {
      args.toggle(obj.id!);
      args.emitSelect(args.primaryId.value);
      return;
    }
    args.selectSingle(obj.id!);
    args.emitSelect(obj.id!);
  };

  // ==========================================
  // 4. 마우스 이동
  // ==========================================
  const processMarquee = (e: MouseEvent) => {
    const { x, yBottom } = args.viewport.toCanvasXY(e.clientX, e.clientY);
    const left = Math.min(marqueeStart.x, x);
    const right = Math.max(marqueeStart.x, x);
    const bottom = Math.min(marqueeStart.y, yBottom);
    const top = Math.max(marqueeStart.y, yBottom);

    marquee.value.left = left;
    marquee.value.bottom = bottom;
    marquee.value.width = Math.max(0, right - left);
    marquee.value.height = Math.max(0, top - bottom);

    const ids = args.objects.value
      .filter((o) => intersectsRect(o, { left, right, bottom, top }))
      .filter((o) => !o.isLocked)
      .map((o) => o.id!);

    args.setMany(ids, { keepExisting: marquee.value.additive });
    args.emitSelect(args.primaryId.value);
  };

  const processRotate = (e: MouseEvent) => {
    hasDragged.value = true;
    const obj = args.objects.value.find((o) => o.id === args.primaryId.value);
    if (!obj) return;

    const centerX = obj.posX + (obj.width || 0) / 2;
    const centerY = obj.posY + (obj.height || 0) / 2;
    const { x, yBottom } = args.viewport.toCanvasXY(e.clientX, e.clientY);

    let deg = Math.round((Math.atan2(yBottom - centerY, x - centerX) * 180) / Math.PI);
    if (e.shiftKey) deg = Math.round(deg / 15) * 15;

    args.emitUpdate(obj.id!, { rotation: deg });
  };

  // ✅ 직각 회전: 화면 좌표계 기반 리사이즈
  const processResizeOrtho = (e: MouseEvent) => {
    const obj = args.objects.value.find((o) => o.id === args.primaryId.value);
    if (!obj) return;

    const screenDx = (e.clientX - dragInfo.mouseX) / args.canvasScale.value;
    const screenDy = (e.clientY - dragInfo.mouseY) / args.canvasScale.value;
    const canvasDx = screenDx;
    const canvasDy = -screenDy; // 캔버스 y 는 위로 +

    const startOffsets = getRotatedOffsets(startPrimary.w, startPrimary.h, startPrimary.rot);
    const startAabbW = startOffsets.maxX - startOffsets.minX;
    const startAabbH = startOffsets.maxY - startOffsets.minY;

    let newAabbW = startAabbW;
    let newAabbH = startAabbH;

    if (dragInfo.dir.includes('e')) newAabbW = Math.max(20, startAabbW + canvasDx);
    if (dragInfo.dir.includes('w')) newAabbW = Math.max(20, startAabbW - canvasDx);
    if (dragInfo.dir.includes('n')) newAabbH = Math.max(20, startAabbH + canvasDy);
    if (dragInfo.dir.includes('s')) newAabbH = Math.max(20, startAabbH - canvasDy);

    if (args.snapEnabled.value && args.showGrid.value && args.gridSize.value > 0) {
      const g = args.gridSize.value;
      if (dragInfo.dir.includes('e') || dragInfo.dir.includes('w')) {
        newAabbW = Math.max(20, Math.round(newAabbW / g) * g);
      }
      if (dragInfo.dir.includes('n') || dragInfo.dir.includes('s')) {
        newAabbH = Math.max(20, Math.round(newAabbH / g) * g);
      }
    }

    // AABB → 객체 로컬 width/height (90/270도는 swap)
    const norm = ((Math.round(startPrimary.rot / 90) * 90) % 360 + 360) % 360;
    const swapped = (norm === 90 || norm === 270);
    const newW = swapped ? newAabbH : newAabbW;
    const newH = swapped ? newAabbW : newAabbH;

    let newAabbLeft: number;
    let newAabbBottom: number;

    if (dragInfo.dir.includes('e')) {
      newAabbLeft = startPrimary.anchorCx;
    } else if (dragInfo.dir.includes('w')) {
      newAabbLeft = startPrimary.anchorCx - newAabbW;
    } else {
      newAabbLeft = startPrimary.anchorCx - newAabbW / 2;
    }

    if (dragInfo.dir.includes('n')) {
      newAabbBottom = startPrimary.anchorCy;
    } else if (dragInfo.dir.includes('s')) {
      newAabbBottom = startPrimary.anchorCy - newAabbH;
    } else {
      newAabbBottom = startPrimary.anchorCy - newAabbH / 2;
    }

    const newOffsets = getRotatedOffsets(newW, newH, startPrimary.rot);
    const newX = newAabbLeft - newOffsets.minX;
    const newY = newAabbBottom - newOffsets.minY;

    // ✅ 부동소수점 오차 제거 (24.999999... 같은 값 → 25)
    args.emitUpdate(obj.id!, {
      posX: Math.round(clamp(newX, -newOffsets.minX, args.canvasWidth.value - newOffsets.maxX)),
      posY: Math.round(clamp(newY, -newOffsets.minY, args.canvasHeight.value - newOffsets.maxY)),
      width: Math.round(newW),
      height: Math.round(newH),
    });
  };

  // ✅ 자유 각도 회전: 로컬 좌표계 기반 리사이즈
  const processResizeFree = (e: MouseEvent) => {
    const obj = args.objects.value.find((o) => o.id === args.primaryId.value);
    if (!obj) return;

    const localDir = dragInfo.localDir;

    const screenDx = (e.clientX - dragInfo.mouseX) / args.canvasScale.value;
    const screenDy = (e.clientY - dragInfo.mouseY) / args.canvasScale.value;
    const canvasDx = screenDx;
    const canvasDy = -screenDy;

    const rad = (startPrimary.rot * Math.PI) / 180;
    const cos = Math.cos(rad);
    const sin = Math.sin(rad);

    const localDx =  canvasDx * cos + canvasDy * sin;
    const localDy = -canvasDx * sin + canvasDy * cos;

    let newW = startPrimary.w;
    let newH = startPrimary.h;

    if (localDir.includes('e')) newW = Math.max(20, startPrimary.w + localDx);
    if (localDir.includes('w')) newW = Math.max(20, startPrimary.w - localDx);
    if (localDir.includes('n')) newH = Math.max(20, startPrimary.h + localDy);
    if (localDir.includes('s')) newH = Math.max(20, startPrimary.h - localDy);

    const sx = localDir.includes('e') ? -1 : (localDir.includes('w') ? 1 : 0);
    const sy = localDir.includes('n') ? -1 : (localDir.includes('s') ? 1 : 0);

    const localAX = sx * newW / 2;
    const localAY = sy * newH / 2;

    const offsetCanvasX = localAX * cos - localAY * sin;
    const offsetCanvasY = localAX * sin + localAY * cos;

    const newCx = startPrimary.anchorCx - offsetCanvasX;
    const newCy = startPrimary.anchorCy - offsetCanvasY;

    const newX = newCx - newW / 2;
    const newY = newCy - newH / 2;

    const offsets = getRotatedOffsets(newW, newH, obj.rotation || 0);

    args.emitUpdate(obj.id!, {
      posX: Math.round(clamp(newX, -offsets.minX, args.canvasWidth.value - offsets.maxX)),
      posY: Math.round(clamp(newY, -offsets.minY, args.canvasHeight.value - offsets.maxY)),
      width: Math.round(newW),
      height: Math.round(newH),
    });
  };

  const processResize = (e: MouseEvent) => {
    hasDragged.value = true;
    if (isOrthogonalRotation(startPrimary.rot)) {
      processResizeOrtho(e);
    } else {
      processResizeFree(e);
    }
  };

  const processDrag = (e: MouseEvent) => {
    if (!args.primaryId.value || args.selectedIds.value.length === 0) return;
    hasDragged.value = true;

    guideLineX.value = null;
    guideLineY.value = null;

    const currentMouse = args.viewport.toCanvasXY(e.clientX, e.clientY);
    const startMouse = args.viewport.toCanvasXY(dragInfo.mouseX, dragInfo.mouseY);

    const rawDx = currentMouse.x - startMouse.x;
    const rawDy = currentMouse.yBottom - startMouse.yBottom;

    let targetX = startPrimary.x + rawDx;
    let targetY = startPrimary.y + rawDy;

    const enableSnap = args.snapEnabled.value && !e.altKey;

    if (enableSnap) {
      const snapTargets = cachedSnapTargets;
      const MAGNETIC_STRENGTH = 15;

      if (args.smartGuides.value) {
        const edgeSnap = snapToObjectEdges(
          targetX, targetY,
          {
            id: args.primaryId.value,
            width: startPrimary.w,
            height: startPrimary.h,
            rotation: startPrimary.rot,   // ✅ 회전 정보 전달 (AABB 기반 정렬에 필수)
          },
          snapTargets,
          MAGNETIC_STRENGTH,
          0
        );

        let isSnappedToObject = false;

        if (edgeSnap.x !== targetX || edgeSnap.y !== targetY) {
          targetX = edgeSnap.x;
          targetY = edgeSnap.y;
          isSnappedToObject = true;

          if (edgeSnap.guides?.x !== undefined && edgeSnap.guides?.x !== null) {
            guideLineX.value = edgeSnap.guides.x;
          }
          if (edgeSnap.guides?.y !== undefined && edgeSnap.guides?.y !== null) {
            guideLineY.value = edgeSnap.guides.y;
          }
        }

        if (!isSnappedToObject && args.showGrid.value && args.gridSize.value > 0) {
          targetX = Math.round(targetX / args.gridSize.value) * args.gridSize.value;
          targetY = Math.round(targetY / args.gridSize.value) * args.gridSize.value;
        }
      } else if (args.showGrid.value && args.gridSize.value > 0) {
        targetX = Math.round(targetX / args.gridSize.value) * args.gridSize.value;
        targetY = Math.round(targetY / args.gridSize.value) * args.gridSize.value;
      }
    }

    const finalDx = targetX - startPrimary.x;
    const finalDy = targetY - startPrimary.y;

    for (const id of args.selectedIds.value) {
      const start = startById.get(id);
      if (!start) continue;

      const newX = start.x + finalDx;
      const newY = start.y + finalDy;

      args.emitUpdate(id, { posX: newX, posY: newY });
    }
  };

  const handleCanvasMouseMove = (e: MouseEvent) => {
    if (action.value === 'marquee') processMarquee(e);
    else if (action.value === 'drag') processDrag(e);
    else if (action.value === 'resize') processResize(e);
    else if (action.value === 'rotate') processRotate(e);
    else if (action.value === 'pan') {
      args.viewport.panBy(e.clientX - panStart.x, e.clientY - panStart.y);
      panStart.x = e.clientX;
      panStart.y = e.clientY;
    }
  };

  // ==========================================
  // 5. 마우스 업
  // ==========================================
  const handleCanvasMouseUp = () => {
    // ✅ drag/resize 종료 시 좌표/크기를 정수로 라운딩
    //    부동소수점 오차가 누적되면 다음 인터랙션에서 점프 현상 발생
    if ((action.value === 'drag' || action.value === 'resize') && hasDragged.value) {
      const ids = action.value === 'resize' && args.primaryId.value
        ? [args.primaryId.value]
        : args.selectedIds.value;

      for (const id of ids) {
        const obj = args.objects.value.find(o => o.id === id);
        if (obj) {
          args.emitUpdate(id, {
            posX: Math.round(obj.posX),
            posY: Math.round(obj.posY),
            width: Math.round(obj.width || 0),
            height: Math.round(obj.height || 0),
          });
        }
      }
    }

    const wasMarquee = action.value === 'marquee';

    action.value = 'none';
    guideLineX.value = null;
    guideLineY.value = null;
    cachedSnapTargets = [];

    if (wasMarquee && marquee.value.active) {
      if (marquee.value.width < 3 && marquee.value.height < 3 && !marquee.value.additive) {
        args.clear();
        args.emitSelect(null);
      }
      marquee.value.active = false;
    }

    setTimeout(() => { suppressCanvasClick.value = false; }, 50);
  };

  // ==========================================
  // 6. 특수 핸들러
  // ==========================================

  // ✅ direction 은 객체 로컬 핸들 방향 (DOM 클래스명 = 객체와 함께 회전됨)
  //    initInteraction 에서 직각/자유 각도에 따라 화면 방향으로 변환
  const startResize = (
    e: MouseEvent,
    obj: TbEcs2dItem,
    direction: 'nw' | 'ne' | 'sw' | 'se' | 'n' | 's' | 'e' | 'w',
  ) => {
    if (obj.isLocked) return;
    if (!args.isSelected(obj.id!)) {
      args.selectSingle(obj.id!);
      args.emitSelect(obj.id!);
    }

    initInteraction(e, obj, 'resize', direction);
  };

  const startRotate = (e: MouseEvent, obj: TbEcs2dItem) => {
    if (obj.isLocked) return;
    if (!args.isSelected(obj.id!)) {
      args.selectSingle(obj.id!);
      args.emitSelect(obj.id!);
    }
    initInteraction(e, obj, 'rotate');
  };

  const centerSelectionToCanvas = () => {
    const selected = [...args.selectedIds.value]
      .map((id) => args.objects.value.find((o) => o.id === id))
      .filter((o): o is TbEcs2dItem => !!o && !o.isLocked);

    if (selected.length === 0) return;

    let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;

    selected.forEach((o) => {
      const offsets = getRotatedOffsets(o.width || 0, o.height || 0, o.rotation || 0);
      minX = Math.min(minX, o.posX + offsets.minX);
      maxX = Math.max(maxX, o.posX + offsets.maxX);
      minY = Math.min(minY, o.posY + offsets.minY);
      maxY = Math.max(maxY, o.posY + offsets.maxY);
    });

    const groupCx = (minX + maxX) / 2;
    const groupCy = (minY + maxY) / 2;
    const desiredDx = args.canvasWidth.value / 2 - groupCx;
    const desiredDy = args.canvasHeight.value / 2 - groupCy;

    const dx = clamp(desiredDx, -minX, args.canvasWidth.value - maxX);
    const dy = clamp(desiredDy, -minY, args.canvasHeight.value - maxY);

    for (const o of selected) {
      args.emitUpdate(o.id!, { posX: o.posX + dx, posY: o.posY + dy });
    }
    args.emitSelect(args.primaryId.value || selected[0]?.id || null);
  };

  return {
    marquee,
    marqueeStyle,
    guideLineX,
    guideLineY,
    suppressCanvasClick,

    handleObjectClick,
    handleObjectMouseDown,
    handleCanvasMouseDown,
    handleCanvasMouseMove,
    handleCanvasMouseUp,
    handlePanStart,

    startResize,
    startRotate,
    centerSelectionToCanvas,
  };
}
