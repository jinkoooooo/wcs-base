/**
 * rackTint.ts
 * 랙 재고 축(state_code) → 반투명 틴트 스타일 계산 공용 유틸
 *
 * ============================================
 * SSOT (단일 소스)
 * ============================================
 * Dashboard2D 와 CellState2D 가 동일한 랙 셀을 동일한 톤으로 보여주기
 * 위해 틴트 계산 로직을 이 파일에서 유일하게 정의한다.
 *
 * 두 화면 모두:
 *   1) getRackStateTintStyle(equipment) 를 호출해 스타일 객체를 얻고
 *   2) 그것을 .rack-state-tint 엘리먼트의 :style 로 주입
 *
 * 색상값은 cell-state.scss 의 CSS 변수 (--cs-*-color) 와 반드시 동기화.
 *
 * ============================================
 * 설계 원칙
 * ============================================
 * - state_code 가 EMPTY / NONE / DRIVE / null → null 반환 (틴트 없음)
 * - drive_only_yn = true → null 반환 (.rack-drive-overlay 가 담당)
 * - 나머지 상태 → hex → rgba 변환 후 background + inset boxShadow 반환
 * - 테두리 알파는 배경 알파 + 0.2 (최대 0.85) 로 살짝 진하게
 */

import { rackStateSpec } from '@/views/kmat_2026/tspg-4way-shuttle/constants/legend/legend-spec';

// --------------------------------------------
// 색상 변환 (hex / CSS var 둘 다 지원)
// --------------------------------------------

/** "#RRGGBB" → "r,g,b" 문자열. 잘못된 형식이면 null. */
function hexToRgbTriplet(hex: string): string | null {
  const h = hex.replace('#', '').trim();
  if (h.length !== 6) return null;
  const r = parseInt(h.substring(0, 2), 16);
  const g = parseInt(h.substring(2, 4), 16);
  const b = parseInt(h.substring(4, 6), 16);
  if (Number.isNaN(r) || Number.isNaN(g) || Number.isNaN(b)) return null;
  return `${r},${g},${b}`;
}

/**
 * legend-spec 의 color 값을 알파가 있는 색 문자열로 변환.
 *   - "#RRGGBB" → "rgba(r,g,b,a)"
 *   - "var(--cs-foo-color)" → "rgb(from var(--cs-foo-color) r g b / a)"
 *     (모던 브라우저의 relative color syntax. Chromium 119+/Safari 16.4+/Firefox 128+)
 *
 * 두 경로 모두 결과는 동일한 "투명도 적용된 같은 색".
 */
function withAlpha(color: string, alpha: number): string {
  const triplet = hexToRgbTriplet(color);
  if (triplet) return `rgba(${triplet},${alpha})`;
  // CSS var() 등 비-hex 값은 relative color syntax 로 알파 주입.
  return `rgb(from ${color} r g b / ${alpha})`;
}



// --------------------------------------------
// 1. 메인 — 틴트 스타일 계산
// --------------------------------------------
//
// 색상/알파 값의 단일 출처(SSOT):
//   1) cell-state.scss 의 --cs-{code}-color / --cs-{code}-border CSS 변수
//   2) legend-spec.ts 의 RACK_STATES[].color / tintAlpha (var(--cs-*-color) 참조)
//
// 본 파일은 rackStateSpec(stateCode) 으로 legend 항목을 읽고, withAlpha 로
// 알파를 주입해 .rack-state-tint 의 inline-style 객체를 만든다.
// hex 직접 정의나 색상 테이블 중복 보관 금지 — 새 상태 추가 시 legend-spec + scss 만 갱신.

/**
 * @param args.stateCode    셀의 재고 축 상태 코드 (없으면 null)
 * @param args.driveOnlyYn  주행 전용 셀이면 true → 틴트 없음
 * @returns                 { background, boxShadow } 또는 null
 */
export function getRackStateTintStyle(args: {
  stateCode: string | null | undefined;
  driveOnlyYn: boolean | null | undefined;
}): Record<string, string> | null {
  const { stateCode, driveOnlyYn } = args;

  if (driveOnlyYn === true) return null;
  if (!stateCode) return null;

  const spec = rackStateSpec(stateCode);
  if (!spec || !spec.color) return null;

  const alpha = spec.tintAlpha ?? 0;
  if (alpha <= 0) return null;

  const bg = withAlpha(spec.color, alpha);
  const borderAlpha = Math.min(alpha + 0.2, 0.85);
  const borderColor = withAlpha(spec.color, borderAlpha);

  return {
    background: bg,
    boxShadow: `inset 0 0 0 1.5px ${borderColor}`,
  };
}
