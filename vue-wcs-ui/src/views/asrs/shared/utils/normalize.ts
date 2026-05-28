import { pick } from './pick';

/**
 * 공통 리스트 normalize
 */
export function normalizeList<T = any>(payload: any): T[] {
  if (Array.isArray(payload)) return payload as T[];

  const rows =
    pick<any[]>(payload, ['items', 'list', 'rows', 'data', 'contents', 'records'], []) || [];

  return Array.isArray(rows) ? (rows as T[]) : [];
}

/**
 * 공통 단건 normalize
 */
export function normalizeSingle<T = any>(payload: any): T | null {
  if (payload == null) return null;

  const single = pick<T>(payload, ['data', 'result', 'row', 'item'], undefined);

  if (single !== undefined && single !== null) {
    return single;
  }

  return payload as T;
}

/**
 * 공통 메시지 normalize
 */
export function normalizeMessage(payload: any, fallback = ''): string {
  return String(
    pick<any>(payload, ['msg', 'message', 'errorMessage', 'error_message'], fallback) ?? fallback,
  );
}

/**
 * 공통 코드 normalize
 */
export function normalizeCode(payload: any, fallback = ''): string {
  return String(
    pick<any>(payload, ['code', 'errorCode', 'error_code'], fallback) ?? fallback,
  );
}
