export interface OutboundLocation2DFilter {
  areaCode: string;
  aisleNo: string;
  sideCode: string;
}

export interface OutboundLocation2DAisleOption {
  aisleNo: number;
}

export interface OutboundLocation2DSideOption {
  sideCode: string;
}

export interface OutboundLocation2DDepth {
  depthNo: number;
  locationId: string;
  locationCode: string;
  occupied: boolean;
  stockUnitId?: string;
  stockUnitNo?: string;
  itemId?: string;
  itemCode?: string;
  itemName?: string;
  qty: number;
  reservedQty: number;
  lotNo?: string;
  stockStatusCode?: string;
  activeYn?: string;
}

export interface OutboundLocation2DCell {
  bayNo: number;
  levelNo: number;
  depths: OutboundLocation2DDepth[];
}

export interface OutboundLocation2DMap {
  areaCode: string;
  aisleNo: number;
  sideCode: string;
  maxBayNo: number;
  maxLevelNo: number;
  cells: OutboundLocation2DCell[];
}

/**
 * 지정출고 실행 폼
 */
export interface OutboundLocation2DExecuteForm {
  refDocType: string;
  refDocNo: string;
  refLineNo: string;
  reasonCode: string;
  remark: string;
}

export type OutboundLocation2DLoadingState = Record<string, boolean> & {
  areas: boolean;
  aisles: boolean;
  sides: boolean;
  map: boolean;
  execute: boolean;
};

export interface OutboundLocation2DViewState {
  zoom: number;
  offsetX: number;
  offsetY: number;
}
