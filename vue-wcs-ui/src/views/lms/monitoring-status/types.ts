export type CenterDataType = {
  id: string;
  lc_id: string;
  lc_nm: string;
  status: string;
  address_plain?: string;
  db_connect_info?: string;
  db_type?: string;
  edge_module_version?: string;

  created_at: string;
  creator_id: string;
  creator: object;
  updated_at?: string;
  updater_id?: string;
  updater?: object;
  domain_id: number;
}

export type CenterType = {
  lc_id: string;
  lc_nm: string;
  lat?: number;
  lng?: number;
  address?: string;
};

export type CenterMetaType = CenterType & {
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
