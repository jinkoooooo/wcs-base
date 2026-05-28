/**
 * item_group 문자열 → 일관된 색상 매핑 유틸.
 *
 * 별도 마스터 테이블 없이 문자열 hash 로 HSL 회전. 같은 group 이면 항상 같은
 * 색이 나오므로 셀 색띠 / 범례 / 패널 미리보기에서 동일한 컬러가 유지된다.
 *
 * 색조(hue) 만 회전하고 채도/명도는 어두운 배경(.equipment-object) 위에서
 * 잘 보이는 값으로 고정.
 */

export function groupColor(group: string | null | undefined): string {
  if (!group) return 'transparent';
  const hue = hashHue(group);
  return `hsl(${hue}, 70%, 55%)`;
}

/** 작은 컬러 칩 / Legend 용 — 살짝 더 진한 톤. */
export function groupColorChip(group: string | null | undefined): string {
  if (!group) return '#3a3f4a';
  const hue = hashHue(group);
  return `hsl(${hue}, 65%, 48%)`;
}

function hashHue(s: string): number {
  // 단순 FNV-1a 32bit → 0~359 hue.
  let h = 2166136261;
  for (let i = 0; i < s.length; i += 1) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  // unsigned 변환 후 360 으로 모듈로
  return Math.abs(h) % 360;
}

/**
 * item_type 라벨 약어 — 셀 우상단 배지는 좁기 때문에 4자 이내로 축약.
 *  - 이미 4자 이하면 그대로
 *  - 영문 + 숫자만: 앞 4자
 *  - 한글 등: 앞 3자
 */
export function abbreviateItemType(value: string | null | undefined, max = 4): string {
  if (!value) return '';
  const v = String(value).trim();
  if (v.length <= max) return v;
  // 한글이 1자라도 있으면 더 짧게 (시각 폭 차이 보정)
  const hasHangul = /[ㄱ-힝]/.test(v);
  return v.slice(0, hasHangul ? Math.max(2, max - 1) : max);
}
