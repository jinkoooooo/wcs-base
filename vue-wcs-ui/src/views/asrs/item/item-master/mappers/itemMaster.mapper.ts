/**
 * item-master 화면 전용 mapper.
 *
 * 목적:
 * - 상품 목록/상세/카테고리 응답 normalize
 * - snake_case / camelCase 혼재 흡수
 * - bulk 저장 응답 normalize
 */

import { normalizeList, normalizeSingle } from '@/views/asrs/shared/utils/normalize';
import { pickNumber, pickString } from '@/views/asrs/shared/utils/pick';
import type {
  ItemBulkPasteErrorRow,
  ItemBulkPasteRow,
  ItemCategoryOption,
  ItemMasterDetailForm,
  ItemMasterRow,
} from '../types';

/**
 * 상품 목록 row normalize
 */
export function normalizeItemMasterRow(row: any): ItemMasterRow {
  return {
    id: pickString(row, ['id'], ''),
    itemCode: pickString(row, ['itemCode', 'item_code'], ''),
    itemName: pickString(row, ['itemName', 'item_name'], ''),
    itemCategoryId: pickString(row, ['itemCategoryId', 'item_category_id'], ''),
    categoryCode: pickString(row, ['categoryCode', 'category_code'], ''),
    categoryName: pickString(row, ['categoryName', 'category_name'], ''),
    operationProfileId: pickString(row, ['operationProfileId', 'operation_profile_id'], ''),
    industryType: pickString(row, ['industryType', 'industry_type'], ''),
    baseUom: pickString(row, ['baseUom', 'base_uom'], ''),
    handlingUnitType: pickString(row, ['handlingUnitType', 'handling_unit_type'], ''),
    outboundUnitType: pickString(row, ['outboundUnitType', 'outbound_unit_type'], ''),
    storageTempType: pickString(row, ['storageTempType', 'storage_temp_type'], ''),
    lotControlYn: pickString(row, ['lotControlYn', 'lot_control_yn'], 'N'),
    expiryControlYn: pickString(row, ['expiryControlYn', 'expiry_control_yn'], 'N'),
    serialControlYn: pickString(row, ['serialControlYn', 'serial_control_yn'], 'N'),
    activeYn: pickString(row, ['activeYn', 'active_yn'], 'Y'),
    updatedAt: pickString(row, ['updatedAt', 'updated_at'], ''),
  };
}

/**
 * 상품 목록 normalize
 */
export function normalizeItemMasterRows(payload: any): ItemMasterRow[] {
  return normalizeList<any>(payload)
    .map(normalizeItemMasterRow)
    .filter((row) => !!row.itemCode);
}

/**
 * 빈 상세 form 생성
 *
 * 신규 등록 모드 진입 시 사용
 */
export function createEmptyItemMasterDetailForm(): ItemMasterDetailForm {
  return {
    id: '',
    itemCode: '',
    itemName: '',
    categoryCode: '',
    categoryName: '',
    operationProfileId: '',
    industryType: 'GENERAL',
    baseUom: 'EA',
    handlingUnitType: 'PALLET',
    outboundUnitType: 'FULL',
    lengthMm: 0,
    widthMm: 0,
    heightMm: 0,
    weightG: 0,
    volumeMm3: 0,
    storageTempType: 'AMBIENT',
    lotControlYn: 'N',
    expiryControlYn: 'N',
    serialControlYn: 'N',
    partialPickYn: 'N',
    mixedLoadYn: 'N',
    fragileYn: 'N',
    heavyYn: 'N',
    quarantineRequiredYn: 'N',
    allocationRuleCode: 'FIXED',
    rotationProfileCode: 'SLOW',
    storageGradeSeed: 'C',
    extAttr: '',
    activeYn: 'Y',
    createdAt: '',
    updatedAt: '',
  };
}

/**
 * 상품 상세 normalize
 */
export function normalizeItemMasterDetail(payload: any): ItemMasterDetailForm {
  const row = normalizeSingle<any>(payload);

  if (!row) {
    return createEmptyItemMasterDetailForm();
  }

  return {
    id: pickString(row, ['id'], ''),
    itemCode: pickString(row, ['itemCode', 'item_code'], ''),
    itemName: pickString(row, ['itemName', 'item_name'], ''),
    categoryCode: pickString(row, ['categoryCode', 'category_code'], ''),
    categoryName: pickString(row, ['categoryName', 'category_name'], ''),
    operationProfileId: pickString(row, ['operationProfileId', 'operation_profile_id'], ''),
    industryType: pickString(row, ['industryType', 'industry_type'], 'GENERAL'),
    baseUom: pickString(row, ['baseUom', 'base_uom'], 'EA'),
    handlingUnitType: pickString(row, ['handlingUnitType', 'handling_unit_type'], 'PALLET'),
    outboundUnitType: pickString(row, ['outboundUnitType', 'outbound_unit_type'], 'FULL'),
    lengthMm: pickNumber(row, ['lengthMm', 'length_mm'], 0),
    widthMm: pickNumber(row, ['widthMm', 'width_mm'], 0),
    heightMm: pickNumber(row, ['heightMm', 'height_mm'], 0),
    weightG: pickNumber(row, ['weightG', 'weight_g'], 0),
    volumeMm3: pickNumber(row, ['volumeMm3', 'volume_mm3'], 0),
    storageTempType: pickString(row, ['storageTempType', 'storage_temp_type'], 'AMBIENT'),
    lotControlYn: pickString(row, ['lotControlYn', 'lot_control_yn'], 'N'),
    expiryControlYn: pickString(row, ['expiryControlYn', 'expiry_control_yn'], 'N'),
    serialControlYn: pickString(row, ['serialControlYn', 'serial_control_yn'], 'N'),
    partialPickYn: pickString(row, ['partialPickYn', 'partial_pick_yn'], 'N'),
    mixedLoadYn: pickString(row, ['mixedLoadYn', 'mixed_load_yn'], 'N'),
    fragileYn: pickString(row, ['fragileYn', 'fragile_yn'], 'N'),
    heavyYn: pickString(row, ['heavyYn', 'heavy_yn'], 'N'),
    quarantineRequiredYn: pickString(row, ['quarantineRequiredYn', 'quarantine_required_yn'], 'N'),
    allocationRuleCode: pickString(row, ['allocationRuleCode', 'allocation_rule_code'], 'FIXED'),
    rotationProfileCode: pickString(row, ['rotationProfileCode', 'rotation_profile_code'], 'SLOW'),
    storageGradeSeed: pickString(row, ['storageGradeSeed', 'storage_grade_seed'], 'C'),
    extAttr: pickString(row, ['extAttr', 'ext_attr'], ''),
    activeYn: pickString(row, ['activeYn', 'active_yn'], 'Y'),
    createdAt: pickString(row, ['createdAt', 'created_at'], ''),
    updatedAt: pickString(row, ['updatedAt', 'updated_at'], ''),
  };
}

/**
 * 카테고리 옵션 normalize
 */
export function normalizeItemCategoryOptions(payload: any): ItemCategoryOption[] {
  return normalizeList<any>(payload)
    .map((row: any) => ({
      id: pickString(row, ['id'], ''),
      categoryCode: pickString(row, ['categoryCode', 'category_code'], ''),
      categoryName: pickString(row, ['categoryName', 'category_name'], ''),
      defaultOperationProfileId: pickString(
        row,
        ['defaultOperationProfileId', 'default_operation_profile_id'],
        '',
      ),
      description: pickString(row, ['description'], ''),
      activeYn: pickString(row, ['activeYn', 'active_yn'], 'Y'),
    }))
    .filter((row) => !!row.categoryCode);
}

/**
 * volume 계산
 *
 * 화면에서 길이/폭/높이 입력 시 즉시 표시용
 * 최종 저장 값은 서버가 다시 계산하므로, 프론트는 보조 표시 역할만 수행
 */
export function calculateItemVolumeMm3(detail: ItemMasterDetailForm): number {
  const length = Number(detail.lengthMm || 0);
  const width = Number(detail.widthMm || 0);
  const height = Number(detail.heightMm || 0);

  if (length <= 0 || width <= 0 || height <= 0) {
    return 0;
  }

  return length * width * height;
}

/**
 * bulk paste row 텍스트 파싱.
 *
 * 입력 형식:
 * - 엑셀에서 복사한 탭(tab) 구분 텍스트
 * - 줄바꿈 단위 row
 *
 * 컬럼 순서:
 * itemCode, itemName, categoryCode, operationProfileId, industryType, baseUom,
 * handlingUnitType, outboundUnitType, lengthMm, widthMm, heightMm, weightG,
 * storageTempType, lotControlYn, expiryControlYn, serialControlYn, partialPickYn,
 * mixedLoadYn, fragileYn, heavyYn, quarantineRequiredYn, allocationRuleCode,
 * rotationProfileCode, storageGradeSeed, activeYn, extAttr
 */
export function parseBulkPasteText(text: string): ItemBulkPasteRow[] {
  const trimmed = (text || '').trim();
  if (!trimmed) return [];

  const lines = trimmed
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => !!line);

  return lines.map((line, index) => {
    const cols = line.split('\t');

    return {
      rowNo: index + 1,
      itemCode: String(cols[0] || '').trim(),
      itemName: String(cols[1] || '').trim(),
      categoryCode: String(cols[2] || '').trim(),
      operationProfileId: String(cols[3] || '').trim(),
      industryType: String(cols[4] || '').trim(),
      baseUom: String(cols[5] || '').trim(),
      handlingUnitType: String(cols[6] || '').trim(),
      outboundUnitType: String(cols[7] || '').trim(),
      lengthMm: Number(cols[8] || 0),
      widthMm: Number(cols[9] || 0),
      heightMm: Number(cols[10] || 0),
      weightG: Number(cols[11] || 0),
      storageTempType: String(cols[12] || '').trim(),
      lotControlYn: String(cols[13] || '').trim(),
      expiryControlYn: String(cols[14] || '').trim(),
      serialControlYn: String(cols[15] || '').trim(),
      partialPickYn: String(cols[16] || '').trim(),
      mixedLoadYn: String(cols[17] || '').trim(),
      fragileYn: String(cols[18] || '').trim(),
      heavyYn: String(cols[19] || '').trim(),
      quarantineRequiredYn: String(cols[20] || '').trim(),
      allocationRuleCode: String(cols[21] || '').trim(),
      rotationProfileCode: String(cols[22] || '').trim(),
      storageGradeSeed: String(cols[23] || '').trim(),
      activeYn: String(cols[24] || '').trim(),
      extAttr: String(cols[25] || '').trim(),
    };
  });
}

/**
 * bulk 오류 목록 normalize
 */
export function normalizeBulkErrorRows(payload: any): ItemBulkPasteErrorRow[] {
  const rows = normalizeList<any>(payload?.errors ?? payload);

  return rows.map((row: any) => ({
    rowNo: pickNumber(row, ['rowNo', 'row_no'], 0),
    itemCode: pickString(row, ['itemCode', 'item_code'], ''),
    message: pickString(row, ['message', 'msg'], ''),
  }));
}
