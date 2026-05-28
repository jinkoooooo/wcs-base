/* src/views/tspg-4way-shuttle/dashboard_2d/composables/useCanvasSnapping.vue */
import type { TbEcs2dItem } from '../api/types';

export type SnapOptions = {
  enabled: boolean;
  gridSize: number;
  // 스마트 가이드(다른 오브젝트 기준 스냅) 활성화 여부
  smartGuides: boolean;
  // 스냅 허용 오차(px) - 캔버스 좌표 기준
  tolerance: number;
};

/**
 * 위치 스냅(그리드 + 스마트 가이드) 계산.
 * - bottom 좌표계를 쓰는 모델(posY = bottom) 기준
 */
export function snapPosition(
  nextX: number,
  nextY: number,
  moving: { id: string; width: number; height: number },
  others: TbEcs2dItem[],
  opts: SnapOptions,
) {
  let x = nextX;
  let y = nextY;
  const guides: { x?: number; y?: number } = {};

  if (!opts.enabled) return { x, y, guides: null as any };

  // 1) grid snap
  if (opts.gridSize > 0) {
    x = Math.round(x / opts.gridSize) * opts.gridSize;
    y = Math.round(y / opts.gridSize) * opts.gridSize;
  }

  if (!opts.smartGuides) return { x, y, guides: null as any };

  // 2) smart guides: 다른 오브젝트의 left/center/right, bottom/center/top에 붙이기
  const tol = Math.max(1, opts.tolerance);
  const myLeft = x;
  const myRight = x + moving.width;
  const myCenterX = x + moving.width / 2;
  const myBottom = y;
  const myTop = y + moving.height;
  const myCenterY = y + moving.height / 2;

  const candidatesX: number[] = [];
  const candidatesY: number[] = [];

  for (const o of others) {
    if (!o?.id || o.id === moving.id) continue;
    const ox = o.posX;
    const oy = o.posY;
    const ow = o.width;
    const oh = o.height;

    const oLeft = ox;
    const oRight = ox + ow;
    const oCenterX = ox + ow / 2;
    const oBottom = oy;
    const oTop = oy + oh;
    const oCenterY = oy + oh / 2;

    candidatesX.push(oLeft, oCenterX, oRight);
    candidatesY.push(oBottom, oCenterY, oTop);
  }

  // x: 내 left/center/right가 후보에 tol 이내면 스냅
  for (const gx of candidatesX) {
    if (Math.abs(myLeft - gx) <= tol) {
      x = gx;
      guides.x = gx;
      break;
    }
    if (Math.abs(myCenterX - gx) <= tol) {
      x = gx - moving.width / 2;
      guides.x = gx;
      break;
    }
    if (Math.abs(myRight - gx) <= tol) {
      x = gx - moving.width;
      guides.x = gx;
      break;
    }
  }

  // y: 내 bottom/center/top이 후보에 tol 이내면 스냅
  for (const gy of candidatesY) {
    if (Math.abs(myBottom - gy) <= tol) {
      y = gy;
      guides.y = gy;
      break;
    }
    if (Math.abs(myCenterY - gy) <= tol) {
      y = gy - moving.height / 2;
      guides.y = gy;
      break;
    }
    if (Math.abs(myTop - gy) <= tol) {
      y = gy - moving.height;
      guides.y = gy;
      break;
    }
  }

  return { x, y, guides };
}
