/**
 * enumHelpers.ts
 *
 * tspg-4way-shuttle 의 모든 enum 은 다음 패턴을 따른다:
 *
 *   export const FooStatus = {
 *     READY:    { code: 0,        label: '대기',   className: 'tspg-status-idle'  },
 *     RUNNING:  { code: 'RUN',    label: '실행중', className: 'tspg-status-moving' },
 *     ...
 *   } as const;
 *
 * 즉 한 enum 정의 안에 코드/한글 라벨/CSS 클래스/아이콘 등 모든 메타데이터를 같이 담는다.
 * 별도의 `FooStatusLabels`, `FooStatusClass` 등 동행 객체를 만들지 않는다.
 *
 * 사용처:
 *   FooStatus.READY.code      // 비교용 코드 값
 *   FooStatus.READY.label     // 라벨
 *   findEnumEntry(FooStatus, value)?.label   // 동적 lookup
 *   enumLabel(FooStatus, value)              // shortcut
 */

/** enum 한 항목의 표준 메타데이터 — 컴포넌트가 자주 쓰는 attribute 들 */
export interface EnumEntry<TCode = string | number> {
  /** 비교용 코드 값 (string 또는 number) */
  code: TCode;
  /** 화면 표시용 한글 라벨 */
  label: string;
  /** tokens.scss 의 utility 클래스명 (선택) */
  className?: string;
  /** 짧은 라벨 — 캔버스 등 좁은 공간 (선택) */
  shortLabel?: string;
  /** 이모지/유니코드 아이콘 (선택) */
  icon?: string;
  /** 배지 텍스트 — IN/OUT/CHG 등 짧은 영문 (선택) */
  badgeText?: string;
  /** 배지 색상 variant — port-mode--{variant} 등에 매핑 (선택) */
  badgeVariant?: string;
}

/**
 * code 값으로 enum 항목 조회.
 * @example findEnumEntry(JobUiStatus, 'PENDING') → JobUiStatus.PENDING
 */
export function findEnumEntry<TCode, T extends EnumEntry<TCode>>(
  enumDef: Record<string, T>,
  code: TCode | null | undefined,
): T | null {
  if (code == null) return null;
  for (const entry of Object.values(enumDef)) {
    if (entry.code === code) return entry;
  }
  return null;
}

/** 라벨 lookup — 못 찾으면 fallback 또는 빈 문자열 */
export function enumLabel<TCode>(
  enumDef: Record<string, EnumEntry<TCode>>,
  code: TCode | null | undefined,
  fallback = '',
): string {
  return findEnumEntry(enumDef, code)?.label ?? fallback;
}

/** 짧은 라벨 lookup (shortLabel 없으면 label fallback) */
export function enumShortLabel<TCode>(
  enumDef: Record<string, EnumEntry<TCode>>,
  code: TCode | null | undefined,
  fallback = '',
): string {
  const entry = findEnumEntry(enumDef, code);
  return entry?.shortLabel ?? entry?.label ?? fallback;
}

/** CSS 클래스 lookup */
export function enumClass<TCode>(
  enumDef: Record<string, EnumEntry<TCode>>,
  code: TCode | null | undefined,
  fallback = '',
): string {
  return findEnumEntry(enumDef, code)?.className ?? fallback;
}

/** 아이콘 lookup */
export function enumIcon<TCode>(
  enumDef: Record<string, EnumEntry<TCode>>,
  code: TCode | null | undefined,
  fallback = '',
): string {
  return findEnumEntry(enumDef, code)?.icon ?? fallback;
}

/** badgeText lookup (영문 배지) */
export function enumBadgeText<TCode>(
  enumDef: Record<string, EnumEntry<TCode>>,
  code: TCode | null | undefined,
  fallback = '',
): string {
  return findEnumEntry(enumDef, code)?.badgeText ?? fallback;
}

/** badgeVariant lookup (CSS variant 클래스 suffix) */
export function enumBadgeVariant<TCode>(
  enumDef: Record<string, EnumEntry<TCode>>,
  code: TCode | null | undefined,
  fallback = '',
): string {
  return findEnumEntry(enumDef, code)?.badgeVariant ?? fallback;
}

/** Object.values 로 모든 항목 배열 반환 */
export function enumEntries<T>(enumDef: Record<string, T>): T[] {
  return Object.values(enumDef);
}
