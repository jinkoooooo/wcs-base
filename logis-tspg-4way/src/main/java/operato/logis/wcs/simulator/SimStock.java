package operato.logis.wcs.simulator;

// 시뮬 재고 1건 — buildOutbound / createOutboundOrder 공용.
public record SimStock(String stockId, String itemCode, String lotNo, int itemQty, String locId) {}
