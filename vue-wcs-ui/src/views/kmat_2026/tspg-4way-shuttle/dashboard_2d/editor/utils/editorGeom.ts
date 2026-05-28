import type { TbEcs2dItem } from '../../api/types';

export const clamp = (v: number, min: number, max: number) => Math.min(max, Math.max(min, v));

/**
 * 객체의 화면상 AABB (회전 적용 후 차지하는 사각형 영역) 계산.
 * 회전이 0이면 원래 박스와 동일.
 */
const getScreenAABB = (
  posX: number,
  posY: number,
  width: number,
  height: number,
  rotation: number,
) => {
  if (!rotation) {
    return {
      left: posX,
      right: posX + width,
      bottom: posY,
      top: posY + height,
    };
  }

  const rad = (rotation * Math.PI) / 180;
  const cos = Math.cos(rad);
  const sin = Math.sin(rad);

  const cx = posX + width / 2;
  const cy = posY + height / 2;
  const halfW = width / 2;
  const halfH = height / 2;

  const corners = [
    { x: -halfW, y: -halfH },
    { x:  halfW, y: -halfH },
    { x:  halfW, y:  halfH },
    { x: -halfW, y:  halfH },
  ].map(p => ({
    x: cx + (p.x * cos + p.y * sin),
    y: cy + (-p.x * sin + p.y * cos),
  }));

  return {
    left:   Math.min(...corners.map(p => p.x)),
    right:  Math.max(...corners.map(p => p.x)),
    bottom: Math.min(...corners.map(p => p.y)),
    top:    Math.max(...corners.map(p => p.y)),
  };
};

/**
 * 객체와 사각형 영역(마퀴 등)의 충돌 검사.
 * 회전된 객체의 화면상 AABB 기준으로 판정 (사용자가 보는 영역과 일치).
 */
export const intersectsRect = (
  o: TbEcs2dItem,
  r: { left: number; right: number; bottom: number; top: number },
) => {
  const box = getScreenAABB(
    o.posX,
    o.posY,
    o.width || 0,
    o.height || 0,
    o.rotation || 0,
  );

  return !(box.right < r.left || box.left > r.right || box.top < r.bottom || box.bottom > r.top);
};

/**
 * "실제 겹침" 만 true (닿기 0px 은 겹침으로 보지 않음) → 옆에 딱 붙이기 가능.
 * 회전된 객체도 화면상 AABB 기준으로 판정.
 */
export const overlapsAny = (
  x: number,
  y: number,
  me: { id: string; width: number; height: number; rotation?: number },
  objects: TbEcs2dItem[],
) => {
  const meBox = getScreenAABB(x, y, me.width || 0, me.height || 0, me.rotation || 0);

  for (const o of objects) {
    if (!o.id || o.id === me.id) continue;
    if (o.isLocked) continue;

    const oBox = getScreenAABB(
      o.posX,
      o.posY,
      o.width || 0,
      o.height || 0,
      o.rotation || 0,
    );

    const ox = Math.min(meBox.right, oBox.right) - Math.max(meBox.left, oBox.left);
    const oy = Math.min(meBox.top,   oBox.top)   - Math.max(meBox.bottom, oBox.bottom);

    if (ox > 0.01 && oy > 0.01) return true;
  }
  return false;
};

/**
 * 두 객체가 화면상 겹치는지 검사 (회전 적용).
 * PropertyPanel 의 stackedObjects 등에서 사용.
 */
export const objectsIntersect = (a: TbEcs2dItem, b: TbEcs2dItem): boolean => {
  const aBox = getScreenAABB(
    a.posX,
    a.posY,
    a.width || 0,
    a.height || 0,
    a.rotation || 0,
  );
  const bBox = getScreenAABB(
    b.posX,
    b.posY,
    b.width || 0,
    b.height || 0,
    b.rotation || 0,
  );

  return !(
    aBox.right  <= bBox.left   ||
    aBox.left   >= bBox.right  ||
    aBox.top    <= bBox.bottom ||
    aBox.bottom >= bBox.top
  );
};
