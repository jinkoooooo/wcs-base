import type { TbEcs2dItem } from '../../api/types';

/**
 * 회전된 객체의 화면상 AABB (Axis-Aligned Bounding Box) 계산.
 * 모든 회전 각도(0, 45, 70, 89, 90, 180 등) 지원.
 *
 * @param posX, posY  객체 좌하단 좌표 (canvas 기준, y 위로 +)
 * @param width, height  객체 로컬 크기
 * @param rotation  회전 각도 (degree)
 *
 * @returns left/right/bottom/top 화면 좌표 + 중심점
 */
const getAABB = (
  posX: number,
  posY: number,
  width: number,
  height: number,
  rotation: number,
) => {
  const rad = ((rotation || 0) * Math.PI) / 180;
  const cos = Math.cos(rad);
  const sin = Math.sin(rad);

  const cx = posX + width / 2;
  const cy = posY + height / 2;

  const halfW = width / 2;
  const halfH = height / 2;

  // 4 모서리를 회전 적용해서 절대 좌표로 환산
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
    left:    Math.min(...corners.map(p => p.x)),
    right:   Math.max(...corners.map(p => p.x)),
    bottom:  Math.min(...corners.map(p => p.y)),
    top:     Math.max(...corners.map(p => p.y)),
    centerX: cx,
    centerY: cy,
  };
};

/**
 * 드래그 중 객체의 가장자리 / 중심선이 다른 객체와 정렬되는지 검사.
 * PPT / Figma 스타일 정렬 가이드.
 *
 * 회전(자유 각도) 지원: AABB 기반으로 비교하므로 모든 각도에서 자연스럽게 동작.
 *
 * 검사하는 정렬 패턴:
 *  - 가장자리 정렬: left↔left, right↔right, top↔top, bottom↔bottom
 *  - 가장자리 붙이기: left↔right, right↔left, top↔bottom, bottom↔top (gap 만큼 띄움)
 *  - 중심선 정렬: 가운데 X ↔ 가운데 X, 가운데 Y ↔ 가운데 Y
 */
export const snapToObjectEdges = (
  x: number,
  y: number,
  me: { id: string; width: number; height: number; rotation?: number },
  objects: TbEcs2dItem[],
  tol: number,
  gap = 0,
) => {
  let bestX = x;
  let bestY = y;
  let bestDx = tol + 1;
  let bestDy = tol + 1;

  let guideX: number | null = null;
  let guideY: number | null = null;

  // 내 객체 AABB
  const meBox = getAABB(x, y, me.width, me.height, me.rotation || 0);
  const meScreenW = meBox.right - meBox.left;
  const meScreenH = meBox.top - meBox.bottom;

  // (x, y) = posX/posY (좌하단) ↔ AABB 좌표 보정 오프셋
  // 회전 시 AABB 가 객체보다 클 수 있으므로 보정 필요
  const offsetX = meBox.left - x;          // posX 가 변하면 meBox.left 도 같이 변함 (회전이 같으므로)
  const offsetY = meBox.bottom - y;

  /**
   * X 축 정렬 후보를 검사하고 더 가까운 것이면 갱신.
   * @param candMeLeft 새 meBox.left 값 (이걸 만족시키도록 x 조정)
   * @param guideAt 가이드라인 표시 위치 (보통 상대 객체의 가장자리)
   */
  const tryX = (candMeLeft: number, guideAt: number) => {
    const d = Math.abs(meBox.left - candMeLeft);
    if (d < bestDx && d <= tol) {
      bestDx = d;
      bestX = candMeLeft - offsetX;
      guideX = guideAt;
    }
  };

  /** Y 축 정렬 후보 검사 */
  const tryY = (candMeBottom: number, guideAt: number) => {
    const d = Math.abs(meBox.bottom - candMeBottom);
    if (d < bestDy && d <= tol) {
      bestDy = d;
      bestY = candMeBottom - offsetY;
      guideY = guideAt;
    }
  };

  for (const o of objects) {
    if (!o.id || o.id === me.id) continue;
    if (o.isLocked) continue;

    const oBox = getAABB(o.posX, o.posY, o.width || 0, o.height || 0, o.rotation || 0);

    const yOverlap = Math.min(meBox.top, oBox.top) - Math.max(meBox.bottom, oBox.bottom);
    const xOverlap = Math.min(meBox.right, oBox.right) - Math.max(meBox.left, oBox.left);

    // ─────────────────────────────────────
    // X 방향 정렬 / 붙이기
    // ─────────────────────────────────────

    // (1) 가장자리 정렬: left ↔ left, right ↔ right
    tryX(oBox.left,                   oBox.left);                 // 내 left = 상대 left
    tryX(oBox.right - meScreenW,      oBox.right);                // 내 right = 상대 right
    // (2) 중심선 정렬: 가운데 X
    // tryX(oBox.centerX - meScreenW / 2, oBox.centerX);

    // (3) 가장자리 붙이기 — 세로로 겹쳐있을 때만 (시각적 의미)
    if (yOverlap > 0) {
      tryX(oBox.right + gap,                   oBox.right);  // 내 left ↔ 상대 right
      tryX(oBox.left - meScreenW - gap,        oBox.left);   // 내 right ↔ 상대 left
    }

    // ─────────────────────────────────────
    // Y 방향 정렬 / 붙이기
    // ─────────────────────────────────────

    // (1) 가장자리 정렬: bottom ↔ bottom, top ↔ top
    tryY(oBox.bottom,                  oBox.bottom);
    tryY(oBox.top - meScreenH,         oBox.top);
    // (2) 중심선 정렬: 가운데 Y
    // tryY(oBox.centerY - meScreenH / 2, oBox.centerY);

    // (3) 가장자리 붙이기 — 가로로 겹쳐있을 때만
    if (xOverlap > 0) {
      tryY(oBox.top + gap,                    oBox.top);     // 내 bottom ↔ 상대 top
      tryY(oBox.bottom - meScreenH - gap,     oBox.bottom);  // 내 top ↔ 상대 bottom
    }
  }

  return {
    x: bestX,
    y: bestY,
    guides: { x: guideX, y: guideY },
  };
};
