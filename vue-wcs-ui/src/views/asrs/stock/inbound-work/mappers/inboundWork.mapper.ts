/**
 * inbound-work 전용 mapper
 */

import { normalizeList } from '@/views/asrs/shared/utils/normalize';
import { pick, pickBoolean, pickNumber, pickString } from '@/views/asrs/shared/utils/pick';
import type {
  InboundItemPolicyState,
  InboundLocationOption,
} from '../types';
import type { StorageAreaOption } from '@/api/asrs/stock';

export function normalizeInboundAreaList(payload: any): StorageAreaOption[] {
  const rows = normalizeList<any>(payload);

  return rows
    .map((row: any) => ({
      id: pickString(row, ['id'], ''),
      areaCode: pickString(row, ['areaCode', 'area_code'], ''),
      areaName: pickString(row, ['areaName', 'area_name'], ''),
      areaType: pickString(row, ['areaType', 'area_type'], ''),
      activeYn: pickString(row, ['activeYn', 'active_yn'], ''),
    }))
    .filter((row) => !!row.areaCode);
}

export function normalizeInboundRecommendCandidates(payload: any): {
  candidateCount: number;
  options: InboundLocationOption[];
} {
  const root = pick<any>(payload, ['data'], payload) ?? {};

  const locations =
    pick<any[]>(root, ['locations', 'candidates', 'recommendedLocations', 'recommended_locations'], []) || [];

  const options: InboundLocationOption[] = locations
    .map((row: any) => ({
      locationId: pickString(row, ['locationId', 'location_id'], ''),
      locationCode: pickString(
        row,
        [
          'locationCode',
          'location_code',
          'recommendedLocationCode',
          'recommended_location_code',
          'targetLocationCode',
          'target_location_code',
        ],
        '',
      ),
      locationGrade: pickString(
        row,
        [
          'locationGrade',
          'location_grade',
          'recommendedLocationGrade',
          'recommended_location_grade',
        ],
        '',
      ),
      frontPriorityYn: pickString(row, ['frontPriorityYn', 'front_priority_yn'], ''),
      sortSeq: pickNumber(row, ['sortSeq', 'sort_seq'], 0),
    }))
    .filter((row) => !!row.locationCode);

  options.sort((a, b) => {
    const aFront = a.frontPriorityYn === 'Y' ? 0 : 1;
    const bFront = b.frontPriorityYn === 'Y' ? 0 : 1;
    if (aFront !== bFront) return aFront - bFront;
    return (a.sortSeq || 0) - (b.sortSeq || 0);
  });

  return {
    candidateCount: pickNumber(root, ['candidateCount', 'candidate_count'], options.length),
    options,
  };
}

export function normalizeInboundItemPolicy(payload: any): InboundItemPolicyState {
  const root = pick<any>(payload, ['data'], payload) ?? {};

  const lotControlRequired = pickBoolean(
    root,
    ['lotControlRequired', 'lot_control_required'],
    false,
  );

  const expiryControlRequired = pickBoolean(
    root,
    ['expiryControlRequired', 'expiry_control_required'],
    false,
  );

  const serialControlRequired = pickBoolean(
    root,
    ['serialControlRequired', 'serial_control_required'],
    false,
  );

  const computedLotEnabled = lotControlRequired || expiryControlRequired;

  return {
    lotEnabled: pickBoolean(root, ['lotEnabled', 'lot_enabled'], computedLotEnabled),
    lotRequired: pickBoolean(root, ['lotRequired', 'lot_required'], computedLotEnabled),
    lotControlRequired,
    expiryControlRequired,
    serialControlRequired,
  };
}

export function normalizeInboundCommandResult(payload: any): {
  stockUnitNo: string;
  txnNo: string;
} {
  const root = pick<any>(payload, ['data'], payload) ?? {};

  return {
    stockUnitNo: pickString(root, ['stockUnitNo', 'stock_unit_no'], '-'),
    txnNo: pickString(root, ['txnNo', 'txn_no'], '-'),
  };
}
