import type {
  OutboundLocation2DAisleOption,
  OutboundLocation2DCell,
  OutboundLocation2DDepth,
  OutboundLocation2DMap,
  OutboundLocation2DSideOption,
} from '../types';

export function normalizeAisleOptions(payload: any): OutboundLocation2DAisleOption[] {
  if (!Array.isArray(payload)) return [];

  return payload
    .map((item) => ({
      aisleNo: Number(item.aisleNo ?? item.aisle_no ?? 0),
    }))
    .filter((item) => item.aisleNo > 0);
}

export function normalizeSideOptions(payload: any): OutboundLocation2DSideOption[] {
  if (!Array.isArray(payload)) return [];

  return payload
    .map((item) => ({
      sideCode: String(item.sideCode ?? item.side_code ?? ''),
    }))
    .filter((item) => !!item.sideCode);
}

function normalizeDepth(item: any): OutboundLocation2DDepth {
  return {
    depthNo: Number(item.depthNo ?? item.depth_no ?? 0),
    locationId: String(item.locationId ?? item.location_id ?? ''),
    locationCode: String(item.locationCode ?? item.location_code ?? ''),
    occupied: Boolean(item.occupied),
    stockUnitId: item.stockUnitId ?? item.stock_unit_id ?? '',
    stockUnitNo: item.stockUnitNo ?? item.stock_unit_no ?? '',
    itemId: item.itemId ?? item.item_id ?? '',
    itemCode: item.itemCode ?? item.item_code ?? '',
    itemName: item.itemName ?? item.item_name ?? '',
    qty: Number(item.qty ?? 0),
    reservedQty: Number(item.reservedQty ?? item.reserved_qty ?? 0),
    lotNo: item.lotNo ?? item.lot_no ?? '',
    stockStatusCode: item.stockStatusCode ?? item.stock_status_code ?? '',
    activeYn: item.activeYn ?? item.active_yn ?? '',
  };
}

function normalizeCell(item: any): OutboundLocation2DCell {
  const depths = Array.isArray(item.depths) ? item.depths.map(normalizeDepth) : [];

  return {
    bayNo: Number(item.bayNo ?? item.bay_no ?? 0),
    levelNo: Number(item.levelNo ?? item.level_no ?? 0),
    depths,
  };
}

export function normalizeLocation2DMap(payload: any): OutboundLocation2DMap {
  return {
    areaCode: String(payload?.areaCode ?? payload?.area_code ?? ''),
    aisleNo: Number(payload?.aisleNo ?? payload?.aisle_no ?? 0),
    sideCode: String(payload?.sideCode ?? payload?.side_code ?? ''),
    maxBayNo: Number(payload?.maxBayNo ?? payload?.max_bay_no ?? 0),
    maxLevelNo: Number(payload?.maxLevelNo ?? payload?.max_level_no ?? 0),
    cells: Array.isArray(payload?.cells) ? payload.cells.map(normalizeCell) : [],
  };
}
