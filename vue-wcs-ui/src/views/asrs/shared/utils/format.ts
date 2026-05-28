/**
 * 숫자 포맷 공통 유틸
 */
export function formatNumber(value: number | string | null | undefined): string {
  const num = Number(value ?? 0);
  if (Number.isNaN(num)) return '0';
  return new Intl.NumberFormat('ko-KR').format(num);
}

/**
 * 날짜/시간 포맷 유틸
 */
export function formatDateTime(value: string | Date | null | undefined): string {
  if (!value) return '-';

  if (typeof value === 'string') {
    return value;
  }

  const yyyy = value.getFullYear();
  const mm = String(value.getMonth() + 1).padStart(2, '0');
  const dd = String(value.getDate()).padStart(2, '0');
  const hh = String(value.getHours()).padStart(2, '0');
  const mi = String(value.getMinutes()).padStart(2, '0');

  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
}

/**
 * 현재 시각을 문자열로 생성
 */
export function createLocalDateTimeText(date = new Date()): string {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const mi = String(date.getMinutes()).padStart(2, '0');

  return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
}
