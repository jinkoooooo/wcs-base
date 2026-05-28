export type DayResult = {
  batch_order_qty?: string;
  batch_pcs?: string;
  batch_sku_qty?: string;
  job_date: string;
  result_order_qty?: string;
  result_pcs?: string;
  result_sku_qty?: string;
  orderPercent: number;
  skuPercent: number;
  pcsPercent: number;
};

export type EquipWorkResult = {
  batchPcs?: Number;
  orderQty?: Number;
  rackCd?: String;
  rackNm?: String;
  resultPcs?: Number;
  resultQty?: Number;
  resultSku?: Number;
  skuQty?: Number;
  orderPercent?: number;
  pcsPercent?: number;
  skuPercent?: number;
  accrueQty?: number;
};
