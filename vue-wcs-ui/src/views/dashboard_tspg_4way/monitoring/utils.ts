/**
 * monitoring 그룹 내 공용 유틸.
 * - 시간 포맷: HostOrderRecent / PendingOrdersList 가 공유.
 * - 주문 타입 라벨/클래스: 여러 컴포넌트가 공유.
 *
 * 그룹 내부에서만 import 한다 (외부에서 참조하지 않음).
 */

/**
 * 'YYYY-MM-DD HH:mm:ss' 또는 ISO 문자열을 epoch ms 로 파싱. 실패 시 null.
 */
export function parseTs(s: string): number | null {
  const iso = Date.parse(s);
  if (!isNaN(iso)) return iso;
  const t = Date.parse(s.replace(' ', 'T'));
  return isNaN(t) ? null : t;
}

/**
 * created_at 같은 타임스탬프를 상대 시간(예: '3분 전')으로 변환.
 * null/파싱 실패 시 원본 또는 '-'.
 */
export function formatRelative(s: string | null): string {
  if (!s) return '-';
  const ts = parseTs(s);
  if (ts === null) return s;
  const sec = Math.floor(Math.max(0, Date.now() - ts) / 1000);
  if (sec < 5)    return '방금 전';
  if (sec < 60)   return `${sec}초 전`;
  const min = Math.floor(sec / 60);
  if (min < 60)   return `${min}분 전`;
  const hr = Math.floor(min / 60);
  if (hr < 24)    return `${hr}시간 전`;
  return `${Math.floor(hr / 24)}일 전`;
}

/** 주문 타입 한글 라벨. 알 수 없으면 원본. */
export function orderTypeLabel(t: string): string {
  switch (t) {
    case 'INBOUND':  return '입고';
    case 'OUTBOUND': return '출고';
    case 'MOVE':     return '이동';
    case 'PUTBACK':  return '재입고';
    default:         return t;
  }
}

/** 주문 타입 chip CSS 클래스. */
export function orderTypeChipClass(t: string): string {
  switch (t) {
    case 'INBOUND':  return 'chip-inbound';
    case 'OUTBOUND': return 'chip-outbound';
    case 'MOVE':     return 'chip-move';
    default:         return 'chip-other';
  }
}
