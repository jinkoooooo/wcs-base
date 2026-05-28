// src/views/lms/tspg-4way-shuttle-ui/assets/iconUrl.ts
import { defaultTypeIconDataUrl } from './equipmentTypeIcons';

// ✅ v-html 인라인 렌더링용 raw svg 문자열
import cargoSvgRaw from './icons/cargo.svg?raw';
import shuttleSvgRaw from './icons/shuttle.svg?raw';

export function resolveTypeIconUrl(
  typeCode: string | null | undefined,
  iconFileName?: string | null,
) {
  // 1) 프론트 내장 DataURL이 있으면 그걸 우선 사용 (경로/배포 이슈 없음)
  if (typeCode && defaultTypeIconDataUrl[typeCode]) {
    return defaultTypeIconDataUrl[typeCode];
  }

  const file = iconFileName && iconFileName.trim() !== '' ? iconFileName : `${typeCode}.svg`;

  // ✅ 실제 파일 위치가 assets/icons 이므로 ./icons 로
  return new URL(`./icons/${file}`, import.meta.url).href;
}

// ✅ 특정 아이콘 직접 가져오기 (URL)
export function getCargoIconUrl() {
  return new URL('./icons/cargo.svg', import.meta.url).href;
}

export function getShuttleIconUrl() {
  return new URL('./icons/shuttle.svg', import.meta.url).href;
}

export function getRackIconUrl() {
  return new URL('./icons/rack.svg', import.meta.url).href;
}

// ✅ 특정 아이콘 직접 가져오기 (RAW SVG 문자열: v-html용)
export function getCargoIconSvg() {
  return normalizeSvgToCurrentColor(cargoSvgRaw);
}

export function getShuttleIconSvg() {
  return normalizeSvgToCurrentColor(shuttleSvgRaw);
}

// ✅ 폴더 안 svg들을 raw로 한번에 로드 (Vite)
const SVG_RAW = import.meta.glob('./icons/*.svg', { as: 'raw', eager: true }) as Record<
  string,
  string
>;

// path => 파일명(key) 매핑
const SVG_BY_NAME: Record<string, string> = Object.fromEntries(
  Object.entries(SVG_RAW).map(([path, raw]) => {
    const name = path.split('/').pop()?.replace('.svg', '') ?? path;
    return [name.toLowerCase(), raw];
  }),
);

// equipmentTypeCode -> 파일명 매핑 (네 아이콘 파일명에 맞춰 수정)
const TYPE_TO_ICON_NAME: Record<string, string> = {
  RACK: 'rack',
  CONVEYOR: 'conveyor',
  LIFTER: 'lifter',
  BCR: 'bcr',
  WORKSTATION: 'workstation',
  CRANE: 'crane',
  // 필요하면 계속 추가
};

/**
 * ✅ SVG를 currentColor 기반으로 “자동 변환”
 * - fill/stroke가 'none'이면 유지
 * - 흰색(#fff/white)은 하이라이트로 남기고 유지
 * - url(#...) 같은 그라데이션/패턴은 유지
 */
export function normalizeSvgToCurrentColor(svg: string): string {
  if (!svg) return svg;

  const keep = (v: string) => {
    const s = v.trim().toLowerCase();
    if (s === 'none' || s === 'currentcolor' || s === 'transparent') return true;
    if (s.startsWith('url(')) return true;
    if (s === '#fff' || s === '#ffffff' || s === 'white') return true;
    if (s.startsWith('rgb(255,255,255') || s.startsWith('rgba(255,255,255')) return true;
    return false;
  };

  // fill="..." / stroke="..." + 단일따옴표 대응
  const repl = (attr: 'fill' | 'stroke') => {
    const r1 = new RegExp(`${attr}="([^"]+)"`, 'gi');
    const r2 = new RegExp(`${attr}='([^']+)'`, 'gi');

    const apply = (src: string, re: RegExp) =>
      src.replace(re, (m, v) => (keep(v) ? m : `${attr}="currentColor"`));

    return (s: string) => apply(apply(s, r1), r2);
  };

  return repl('stroke')(repl('fill')(svg));
}

/** ✅ iconData2d가 "raw svg"로 들어오는 경우도 커버 */
export function asInlineSvg(iconData2d?: string | null): string | null {
  const t = String(iconData2d ?? '').trim();
  if (!t) return null;

  // ✅ "<svg"가 앞에 바로 안 와도 잡히게
  const idx = t.toLowerCase().indexOf('<svg');
  if (idx >= 0) {
    const svg = t.slice(idx);
    return normalizeSvgToCurrentColor(svg);
  }
  return null;
}

/** ✅ 설비 타입으로 svg raw 반환 (없으면 null) */
export function resolveTypeIconSvg(equipmentTypeCode?: string | null): string | null {
  const type = String(equipmentTypeCode ?? '').trim();
  if (!type) return null;

  const name = (TYPE_TO_ICON_NAME[type] ?? type).toLowerCase();
  const raw = SVG_BY_NAME[name];
  return raw ? normalizeSvgToCurrentColor(raw) : null;
}
