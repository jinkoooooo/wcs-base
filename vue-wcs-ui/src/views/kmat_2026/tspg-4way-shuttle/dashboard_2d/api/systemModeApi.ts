/**
 * [wcs-ops Step 14/20] System Mode / Ops Status API.
 * 백엔드:
 *   GET /rest/wcs/system-mode?eqGroupId=...
 *   GET /rest/wcs/ops-status?eqGroupId=...   (Step 20 에서 구현)
 *   GET /rest/wcs/admin/port-locks?eqGroupId=...
 */

import { defHttp } from '/@/utils/http/axios';
import { keysToCamel } from '../utils/case';

export interface SystemModeFlags {
  isOperationModeEnabled: boolean;
  isPutbackEnabled: boolean;
  isDispatchLockEnabled: boolean;
  isInspectionEnabled: boolean;
}

export interface SystemModeInfo {
  eqGroupId: string | null;
  operationMode: string;
  flags: SystemModeFlags;
}

export interface OpsStatusCounts {
  waitingSchedule: number;
  inspectionWait: number;
  inspectionFailed: number;
  readyForAlloc: number;
  executing: number;
  putbackActive: number;
  portLocked: number;
}

export interface OpsStatus {
  mode: string;
  flags: Record<string, string>;
  counts: OpsStatusCounts;
}

export async function fetchSystemMode(eqGroupId: string | null): Promise<SystemModeInfo> {
  const url = eqGroupId
    ? `/wcs/system-mode?eqGroupId=${encodeURIComponent(eqGroupId)}`
    : '/wcs/system-mode';
  const res = await defHttp.get({ url }, { isTransformResponse: false });
  return keysToCamel<SystemModeInfo>(res);
}

export async function fetchOpsStatus(eqGroupId: string | null): Promise<OpsStatus> {
  const url = eqGroupId
    ? `/wcs/ops-status?eqGroupId=${encodeURIComponent(eqGroupId)}`
    : '/wcs/ops-status';
  const res = await defHttp.get({ url }, { isTransformResponse: false });
  return keysToCamel<OpsStatus>(res);
}

export async function fetchPortLocks(eqGroupId: string | null): Promise<any[]> {
  const url = eqGroupId
    ? `/wcs/admin/port-locks?eqGroupId=${encodeURIComponent(eqGroupId)}`
    : '/wcs/admin/port-locks';
  const res = await defHttp.get({ url }, { isTransformResponse: false });
  return keysToCamel<any[]>(res);
}
