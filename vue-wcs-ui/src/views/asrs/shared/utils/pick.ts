/**
 * 공통 payload 접근 유틸
 *
 * 목적:
 * - snake_case / camelCase 혼재 응답 대응
 */
export function pick<T = any>(
  source: any,
  keys: string[],
  defaultValue?: T,
): T | undefined {
  if (source == null) return defaultValue;

  for (const key of keys) {
    const value = source?.[key];
    if (value !== undefined && value !== null) {
      return value as T;
    }
  }

  return defaultValue;
}

export function pickString(
  source: any,
  keys: string[],
  defaultValue = '',
): string {
  const value = pick<any>(source, keys, defaultValue);
  if (value === undefined || value === null) return defaultValue;
  return String(value);
}

export function pickNumber(
  source: any,
  keys: string[],
  defaultValue = 0,
): number {
  const value = pick<any>(source, keys, defaultValue);
  const num = Number(value);
  return Number.isNaN(num) ? defaultValue : num;
}

export function pickBoolean(
  source: any,
  keys: string[],
  defaultValue = false,
): boolean {
  const value = pick<any>(source, keys, defaultValue);

  if (typeof value === 'boolean') return value;
  if (value === undefined || value === null || value === '') return defaultValue;

  const normalized = String(value).trim().toUpperCase();

  if (['Y', 'YES', 'TRUE', '1'].includes(normalized)) return true;
  if (['N', 'NO', 'FALSE', '0'].includes(normalized)) return false;

  return defaultValue;
}
