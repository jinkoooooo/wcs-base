export type CenterType = {
  lc_id: string;
  lc_nm: string;
  lat?: number;
  lng?: number;
  address?: string;
};

export type CenterMetaType = CenterType & {
  // TODO: 쿼리 생성 후, 컬럼명 수정 필요
  edgeStatus?: 'ON' | 'OFF';
  alarm?: { info: number; warning: number; error: number };
  lastHeartbeat?: string;
  status?: LcStatusType;
};

// 센터 상태 타입
export type LcStatusType = 'RUN' | 'WARNING' | 'ERROR' | 'DISCONNECT' | 'UNKNOWN' | 'WRONG_ADDRESS';

// 센터 상태 상수
export const LC_STATUS_CONST = {
  RUN: 'RUN' as LcStatusType,
  WARNING: 'WARNING' as LcStatusType,
  ERROR: 'ERROR' as LcStatusType,
  DISCONNECT: 'DISCONNECT' as LcStatusType,
  UNKNOWN: 'UNKNOWN' as LcStatusType,
  WRONG_ADDRESS: 'WRONG_ADDRESS' as LcStatusType
}
