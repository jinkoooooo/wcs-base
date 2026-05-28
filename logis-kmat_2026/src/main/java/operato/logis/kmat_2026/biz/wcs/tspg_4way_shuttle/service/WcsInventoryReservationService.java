package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommandItem;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsInventory;
import operato.logis.kmat_2026.service.impl.TbWcsInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.List;

/**
 * ====================================================================
 * Inventory Reservation Service
 * ====================================================================
 *
 * [역할]
 * - 재고 예약(allocQty 증가)
 * - 예약 해제(allocQty 감소)
 * - 출고 확정(qty/allocQty 감소)
 * - 입고 확정(재고 생성 또는 증가)
 * - 이동 확정(출발지 감소 + 목적지 증가)
 *
 * [설계 포인트 (리팩토링 완료)]
 * - 수동 롤백 코드 제거: 예외 발생 시 Spring @Transactional 에 의해 자동 롤백됩니다.
 * - 실패 시 ElidomRuntimeException 을 던져 호출자(Manager)의 트랜잭션까지 안전하게 취소시킵니다.
 */
@Service
public class WcsInventoryReservationService {

    private static final Logger logger = LoggerFactory.getLogger(WcsInventoryReservationService.class);

    @Autowired
    protected TbWcsInventoryService inventoryService;

    @Transactional(rollbackFor = Exception.class)
    public boolean reserve(String inventoryId, int qty) {
        if (!StringUtils.hasText(inventoryId) || qty <= 0) {
            logger.error("Reserve failed: invalid parameter. inventoryId={}, qty={}", inventoryId, qty);
            return false;
        }

        logger.info("Reserving inventory. inventoryId={}, qty={}", inventoryId, qty);

        TbWcsInventory inv = inventoryService.findById(inventoryId);
        if (inv == null) {
            logger.error("Reserve failed: inventory not found. inventoryId={}", inventoryId);
            return false;
        }

        int availableQty = safeInt(inv.getQty()) - safeInt(inv.getAllocQty());
        if (availableQty < qty) {
            throw new ElidomRuntimeException("단일 재고 예약 실패: 가용 수량 부족 (inventoryId: " + inventoryId + ")");
        }

        inventoryService.increaseAllocQty(inventoryId, qty);
        logger.info("Inventory reserved. inventoryId={}, qty={}", inventoryId, qty);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void release(String inventoryId, int qty) {
        if (!StringUtils.hasText(inventoryId) || qty <= 0) {
            return;
        }
        logger.info("Releasing inventory reservation. inventoryId={}, qty={}", inventoryId, qty);
        inventoryService.decreaseAllocQty(inventoryId, qty);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmOutbound(String inventoryId, int qty) {
        if (!StringUtils.hasText(inventoryId) || qty <= 0) {
            return;
        }
        logger.info("Confirming outbound. inventoryId={}, qty={}", inventoryId, qty);
        inventoryService.decreaseQtyAndAllocQty(inventoryId, qty);
    }

    @Transactional(rollbackFor = Exception.class)
    public TbWcsInventory confirmInbound(String eqGroupId, String locCode, String ownerCode, String skuCode, String lotNo, int qty) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(locCode) || !StringUtils.hasText(ownerCode) || !StringUtils.hasText(skuCode) || qty <= 0) {
            logger.error("Confirm inbound failed: invalid parameter");
            return null;
        }
        logger.info("Confirming inbound. eqGroupId={}, locCode={}, ownerCode={}, skuCode={}, lotNo={}, qty={}",
                eqGroupId, locCode, ownerCode, skuCode, lotNo, qty);
        return inventoryService.createOrIncreaseQty(eqGroupId, locCode, ownerCode, skuCode, lotNo, qty);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmMove(String fromInventoryId, int qty, String toEqGroupId, String toLocCode, String ownerCode, String skuCode, String lotNo) {
        if (!StringUtils.hasText(fromInventoryId) || !StringUtils.hasText(toEqGroupId) || !StringUtils.hasText(toLocCode) || !StringUtils.hasText(ownerCode) || !StringUtils.hasText(skuCode) || qty <= 0) {
            logger.error("Confirm move failed: invalid parameter");
            return;
        }
        logger.info("Confirming move. fromInventoryId={}, toLocCode={}, qty={}", fromInventoryId, toLocCode, qty);
        inventoryService.decreaseQtyAndAllocQty(fromInventoryId, qty);
        inventoryService.createOrIncreaseQty(toEqGroupId, toLocCode, ownerCode, skuCode, lotNo, qty);
    }

    /**
     * locCode 기준 아이템 전체 예약. (수동 롤백 제거 및 자동 롤백 적용)
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveForItems(String eqGroupId, String ownerCode, String fromLocCode, List<WcsOrderCommandItem> items) {
        logger.info("Reserve for items. eqGroupId={}, ownerCode={}, fromLocCode={}, itemCount={}",
                eqGroupId, ownerCode, fromLocCode, items == null ? 0 : items.size());

        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(fromLocCode) || ValueUtil.isEmpty(items)) {
            logger.error("Reserve for items failed: invalid parameter");
            return false;
        }

        // 품목 예약 중 하나라도 실패하면 ElidomRuntimeException 이 발생하여
        // 트랜잭션이 전체 롤백
        for (WcsOrderCommandItem item : items) {
            reserveSingleItemByLocCode(eqGroupId, ownerCode, fromLocCode, item);
        }

        logger.info("Reserve for items completed. eqGroupId={}, ownerCode={}, fromLocCode={}, itemCount={}",
                eqGroupId, ownerCode, fromLocCode, items.size());
        return true;
    }

    /**
     * 실패 시 수동 기록 대신 ElidomRuntimeException을 던져 트랜잭션을 롤백시킵니다.
     */
    private void reserveSingleItemByLocCode(String eqGroupId, String ownerCode, String fromLocCode, WcsOrderCommandItem item) {
        if (item == null) {
            throw new ElidomRuntimeException("재고 예약 실패: 품목 정보가 없습니다.");
        }

        String skuCode = item.getSkuCode();
        String lotNo = item.getLotNo();
        int requestQty = safeInt(item.getQty());

        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(skuCode) || requestQty <= 0) {
            throw new ElidomRuntimeException("재고 예약 실패: 유효하지 않은 품목. skuCode=" + skuCode + ", qty=" + requestQty);
        }

        List<TbWcsInventory> availableStock = getAvailableStockByLocCode(eqGroupId, fromLocCode, ownerCode, skuCode, lotNo);
        if (ValueUtil.isEmpty(availableStock)) {
            throw new ElidomRuntimeException("재고 예약 실패: 가용 재고 없음. fromLocCode=" + fromLocCode + ", skuCode=" + skuCode);
        }

        int remainingQty = requestQty;
        for (TbWcsInventory inv : availableStock) {
            if (remainingQty <= 0) {
                break;
            }

            int availableQty = safeInt(inv.getQty()) - safeInt(inv.getAllocQty());
            int reserveQty = Math.min(availableQty, remainingQty);

            if (reserveQty > 0) {
                inventoryService.increaseAllocQty(inv.getId(), reserveQty);
                remainingQty -= reserveQty;
            }
        }

        if (remainingQty > 0) {
            throw new ElidomRuntimeException("재고 예약 실패: 재고 수량 부족. skuCode=" + skuCode + ", 부족수량=" + remainingQty);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean reserveByLocation(String eqGroupId, String ownerCode, String locCode) {
        if (ValueUtil.isEmpty(eqGroupId) || ValueUtil.isEmpty(locCode)) {
            return false;
        }

        List<TbWcsInventory> inventories = inventoryService.findByEqGroupIdAndLocCode(eqGroupId, locCode);
        if (ValueUtil.isEmpty(inventories)) {
            return true;
        }

        for (TbWcsInventory inv : inventories) {
            if (ownerCode != null && !ownerCode.equals(inv.getOwnerCode())) {
                continue;
            }
            int reservableQty = inv.getQty() - inv.getAllocQty();
            if (reservableQty <= 0) {
                continue;
            }
            inv.setAllocQty(inv.getAllocQty() + reservableQty);
            inventoryService.update(inv);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmOutboundByLocCode(String eqGroupId, String fromLocCode, String ownerCode, String skuCode, String lotNo, int qty) {
        processInventoryByLocCode(eqGroupId, fromLocCode, ownerCode, skuCode, lotNo, qty, InventoryProcessType.CONFIRM_OUTBOUND);
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseForItems(String eqGroupId, String ownerCode, String fromLocCode, List<WcsOrderCommandItem> items) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(fromLocCode) || ValueUtil.isEmpty(items)) {
            return;
        }

        for (WcsOrderCommandItem item : items) {
            if (item == null) continue;
            int qty = item.getQty() == null ? 0 : item.getQty();
            if (qty <= 0) continue;
            releaseByLocCode(eqGroupId, fromLocCode, ownerCode, item.getSkuCode(), item.getLotNo(), qty);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseByLocCode(String eqGroupId, String fromLocCode, String ownerCode, String skuCode, String lotNo, int qty) {
        processInventoryByLocCode(eqGroupId, fromLocCode, ownerCode, skuCode, lotNo, qty, InventoryProcessType.RELEASE);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmMoveByLocCode(String eqGroupId, String fromLocCode, String toLocCode, String ownerCode, String skuCode, String lotNo, int qty) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(fromLocCode) || !StringUtils.hasText(toLocCode) || !StringUtils.hasText(ownerCode) || !StringUtils.hasText(skuCode) || qty <= 0) {
            logger.error("Confirm move by locCode failed: invalid parameter");
            return;
        }

        List<TbWcsInventory> availableStock = getAvailableStockByLocCode(eqGroupId, fromLocCode, ownerCode, skuCode, lotNo);
        int remainingQty = qty;

        for (TbWcsInventory inv : availableStock) {
            if (remainingQty <= 0) break;

            int movableQty = Math.min(Math.min(safeInt(inv.getQty()), safeInt(inv.getAllocQty())), remainingQty);
            if (movableQty > 0) {
                inventoryService.decreaseQtyAndAllocQty(inv.getId(), movableQty);
                inventoryService.createOrIncreaseQty(eqGroupId, toLocCode, ownerCode, skuCode, lotNo, movableQty);
                remainingQty -= movableQty;
            }
        }
    }

    private void processInventoryByLocCode(String eqGroupId, String fromLocCode, String ownerCode, String skuCode, String lotNo, int qty, InventoryProcessType processType) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(fromLocCode) || !StringUtils.hasText(ownerCode) || !StringUtils.hasText(skuCode) || qty <= 0) {
            return;
        }

        List<TbWcsInventory> availableStock = getAvailableStockByLocCode(eqGroupId, fromLocCode, ownerCode, skuCode, lotNo);
        int remainingQty = qty;

        for (TbWcsInventory inv : availableStock) {
            if (remainingQty <= 0) break;

            int processQty = switch (processType) {
                case RELEASE -> Math.min(safeInt(inv.getAllocQty()), remainingQty);
                case CONFIRM_OUTBOUND -> Math.min(Math.min(safeInt(inv.getQty()), safeInt(inv.getAllocQty())), remainingQty);
            };

            if (processQty <= 0) continue;

            switch (processType) {
                case RELEASE -> inventoryService.decreaseAllocQty(inv.getId(), processQty);
                case CONFIRM_OUTBOUND -> inventoryService.decreaseQtyAndAllocQty(inv.getId(), processQty);
            }
            remainingQty -= processQty;
        }
    }

    private List<TbWcsInventory> getAvailableStockByLocCode(String eqGroupId, String fromLocCode, String ownerCode, String skuCode, String lotNo) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(fromLocCode) || !StringUtils.hasText(ownerCode) || !StringUtils.hasText(skuCode)) {
            return Collections.emptyList();
        }

        if (StringUtils.hasText(lotNo)) {
            List<TbWcsInventory> list = inventoryService.findAvailableStockByLocCodeWithLot(eqGroupId, fromLocCode, ownerCode, skuCode, lotNo);
            return list == null ? Collections.emptyList() : list;
        }

        List<TbWcsInventory> list = inventoryService.findAvailableStockByLocCode(eqGroupId, fromLocCode, ownerCode, skuCode);
        return list == null ? Collections.emptyList() : list;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean reserveAllInventoryByLocation(String eqGroupId, String ownerCode, String locCode) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(locCode)) {
            return false;
        }

        List<TbWcsInventory> inventories = inventoryService.findByEqGroupIdAndLocCode(eqGroupId, locCode);
        if (inventories == null || inventories.isEmpty()) {
            return true;
        }

        int reservedCount = 0;
        for (TbWcsInventory inv : inventories) {
            if (inv == null) continue;
            if (StringUtils.hasText(ownerCode) && !ownerCode.equals(inv.getOwnerCode())) continue;

            int reservableQty = safeInt(inv.getQty()) - safeInt(inv.getAllocQty());
            if (reservableQty <= 0) continue;

            inventoryService.increaseAllocQty(inv.getId(), reservableQty);
            reservedCount++;
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void releaseAllInventoryByLocation(String eqGroupId, String ownerCode, String locCode) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(locCode)) return;

        List<TbWcsInventory> inventories = inventoryService.findByEqGroupIdAndLocCode(eqGroupId, locCode);
        if (inventories == null || inventories.isEmpty()) return;

        for (TbWcsInventory inv : inventories) {
            if (inv == null) continue;
            if (StringUtils.hasText(ownerCode) && !ownerCode.equals(inv.getOwnerCode())) continue;

            int allocQty = safeInt(inv.getAllocQty());
            if (allocQty <= 0) continue;

            inventoryService.decreaseAllocQty(inv.getId(), allocQty);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void transferAllReservedInventoryByLocation(String eqGroupId, String ownerCode, String fromLocCode, String toLocCode) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(fromLocCode) || !StringUtils.hasText(toLocCode)) return;

        List<TbWcsInventory> fromInventories = inventoryService.findByEqGroupIdAndLocCode(eqGroupId, fromLocCode);
        if (fromInventories == null || fromInventories.isEmpty()) return;

        for (TbWcsInventory inv : fromInventories) {
            if (inv == null) continue;
            if (StringUtils.hasText(ownerCode) && !ownerCode.equals(inv.getOwnerCode())) continue;

            int transferableQty = Math.min(safeInt(inv.getQty()), safeInt(inv.getAllocQty()));
            if (transferableQty <= 0) continue;

            inventoryService.decreaseQtyAndAllocQty(inv.getId(), transferableQty);
            inventoryService.createOrIncreaseQty(eqGroupId, toLocCode, inv.getOwnerCode(), inv.getSkuCode(), inv.getLotNo(), transferableQty);
        }
    }

    private enum InventoryProcessType {
        RELEASE,
        CONFIRM_OUTBOUND
    }
}