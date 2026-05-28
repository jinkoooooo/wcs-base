package operato.logis.ecs.base.wcs.service.impl;

import operato.logis.inventory.consts.StockStatus;
import operato.logis.ecs.base.wcs.entity.ExtTbInventoryStock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * [ExtTbInventoryStock Entity Service]
 * - 테이블: tb_inventory_stock
 * - locId 직접 조회 제거 (tb_inventory_location JOIN 방식)
 * - allocQty 관련 메서드 없음 (taskId 기반 잠금)
 */
@Service
public class ExtTbInventoryStockService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(ExtTbInventoryStockService.class);

    @Autowired
    protected ExtTbInventoryItemMasterService itemMasterService;

    /** ID로 조회 */
    public ExtTbInventoryStock findById(String id) {
        return this.queryManager.select(ExtTbInventoryStock.class, id);
    }

    /** eqGroupId + stockId + itemOwner + sku + lotNo 로 조회 */
    public ExtTbInventoryStock findByKey(String eqGroupId, String stockId, String itemOwner, String sku, String lotNo) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("stock_id", stockId);
        condition.addFilter("item_owner", itemOwner);
        condition.addFilter("sku", sku);
        if (ValueUtil.isNotEmpty(lotNo)) {
            condition.addFilter("lot_no", lotNo);
        }

        return this.queryManager.selectByCondition(ExtTbInventoryStock.class, condition);
    }

    /** eqGroupId + locId 로 조회 (tb_inventory_location JOIN) */
    public List<ExtTbInventoryStock> findByEqGroupIdAndLocId(String eqGroupId, String locId) {
        String sql = "SELECT s.* FROM tb_inventory_stock s " +
                "JOIN tb_inventory_location l ON l.stock_id = s.stock_id " +
                "WHERE l.loc_group = :locGroup " +
                "AND l.loc_id = :locId " +
                "AND s.eq_group_id = :eqGroupId";

        Map<String, Object> params = ValueUtil.newMap(
                "locGroup,locId,eqGroupId",
                eqGroupId, locId, eqGroupId
        );

        return this.queryManager.selectListBySql(sql, params, ExtTbInventoryStock.class, 0, 0);
    }

    /** eqGroupId + sku + itemOwner 로 가용 재고 조회 (item_qty > 0) */
    public List<ExtTbInventoryStock> findAvailableStock(String eqGroupId, String itemOwner, String sku) {
        String sql = "SELECT * FROM tb_inventory_stock " +
                "WHERE eq_group_id = :eqGroupId " +
                "AND item_owner = :itemOwner " +
                "AND sku = :sku " +
                "AND item_qty > 0 " +
                "AND stock_status = 0 " +
                "ORDER BY created_at ASC";

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,itemOwner,sku",
                eqGroupId, itemOwner, sku
        );

        return this.queryManager.selectListBySql(sql, params, ExtTbInventoryStock.class, 0, 0);
    }

    /** eqGroupId + sku + itemOwner + lotNo 로 가용 재고 조회 */
    public List<ExtTbInventoryStock> findAvailableStockWithLot(String eqGroupId, String itemOwner, String sku, String lotNo) {
        String sql = "SELECT * FROM tb_inventory_stock " +
                "WHERE eq_group_id = :eqGroupId " +
                "AND item_owner = :itemOwner " +
                "AND sku = :sku " +
                "AND lot_no = :lotNo " +
                "AND item_qty > 0 " +
                "AND stock_status = 0 " +
                "ORDER BY created_at ASC";

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,itemOwner,sku,lotNo",
                eqGroupId, itemOwner, sku, lotNo
        );

        return this.queryManager.selectListBySql(sql, params, ExtTbInventoryStock.class, 0, 0);
    }

    /** 신규 저장 */
    public ExtTbInventoryStock insert(ExtTbInventoryStock entity) {
        logger.info("Inserting ExtTbInventoryStock: eqGroupId={}, sku={}, itemQty={}",
                entity.getEqGroupId(), entity.getSku(), entity.getItemQty());
        this.queryManager.insert(entity);
        return entity;
    }

    /** 수정 */
    public ExtTbInventoryStock update(ExtTbInventoryStock entity) {
        logger.info("Updating ExtTbInventoryStock: id={}, itemQty={}",
                entity.getId(), entity.getItemQty());
        this.queryManager.update(entity);
        return entity;
    }

    public ExtTbInventoryStock createOrIncreaseQty(String eqGroupId,
                                                   String stockId,
                                                   String itemOwner,
                                                   String sku,
                                                   String lotNo,
                                                   int qty) {
        return createOrIncreaseQty(eqGroupId, stockId, itemOwner, sku, lotNo, qty, null, null);
    }

    // ExtTbInventoryStockService 안에 추가 / 교체

    /** 기존 호환용: 상태 미지정 → IDLE 로 생성 */
    public ExtTbInventoryStock createOrIncreaseQty(String eqGroupId,
                                                   String stockId,
                                                   String itemOwner,
                                                   String sku,
                                                   String lotNo,
                                                   int qty,
                                                   Date produceDate,
                                                   Date expiryDate) {
        return createOrIncreaseQty(eqGroupId, stockId, itemOwner, sku, lotNo,
                qty, produceDate, expiryDate, StockStatus.IDLE);
    }

    /**
     * 신규 재고 생성 또는 기존 재고 qty 증가 (초기 상태 지정 가능)
     *
     * - 산출 시점:    initialStatus = INBOUND
     * - 입고 확정만:  initialStatus = IDLE  (기존 흐름)
     *
     * @param initialStatus 신규 생성 시 적용할 상태 (기존 행 update 시에는 무시)
     */
    public ExtTbInventoryStock createOrIncreaseQty(String eqGroupId,
                                                   String stockId,
                                                   String itemOwner,
                                                   String sku,
                                                   String lotNo,
                                                   int qty,
                                                   Date produceDate,
                                                   Date expiryDate,
                                                   StockStatus initialStatus) {
        ExtTbInventoryStock existing = this.findByKey(eqGroupId, stockId, itemOwner, sku, lotNo);
        if (existing != null) {
            String sql = "UPDATE tb_inventory_stock " +
                    "SET item_qty = item_qty + :qty " +
                    "WHERE id = :id";
            Map<String, Object> params = ValueUtil.newMap("qty,id", qty, existing.getId());
            this.queryManager.executeBySql(sql, params);
            return this.findById(existing.getId());
        }

        ExtTbInventoryStock newInv = new ExtTbInventoryStock();
        newInv.setEqGroupId(eqGroupId);
        newInv.setStockId(stockId);
        newInv.setItemOwner(itemOwner);
        newInv.setSku(sku);
        newInv.setItemCode(sku);
        newInv.setLotNo(lotNo);
        newInv.setItemQty(qty);
        newInv.setStockStatus(
                (initialStatus == null ? StockStatus.IDLE : initialStatus).value()
        );
        newInv.setIsEnabled(true);
        newInv.setInbDatetime(new Date());
        newInv.setStockHeight(null);
        newInv.setProduceDate(produceDate);
        newInv.setExpiredDatetime(expiryDate);

        logger.info("Creating new stock. eqGroupId={}, stockId={}, sku={}, qty={}, status={}",
                eqGroupId, stockId, sku, qty, newInv.getStockStatus());

        return this.insert(newInv);
    }

    /** eqGroupId + locId + itemOwner + sku 로 가용 재고 조회 (JOIN) */
    public List<ExtTbInventoryStock> findAvailableStockByLocId(String eqGroupId,
                                                               String locId,
                                                               String itemOwner,
                                                               String sku) {
        String sql = "SELECT s.* FROM tb_inventory_stock s " +
                "JOIN tb_inventory_location l ON l.stock_id = s.stock_id " +
                "WHERE l.loc_group = :locGroup " +
                "AND l.loc_id = :locId " +
                "AND s.eq_group_id = :eqGroupId " +
                "AND s.item_owner = :itemOwner " +
                "AND s.sku = :sku " +
                "AND s.item_qty > 0 " +
                "AND s.stock_status = 0 " +
                "ORDER BY s.created_at ASC";

        Map<String, Object> params = ValueUtil.newMap(
                "locGroup,locId,eqGroupId,itemOwner,sku",
                eqGroupId, locId, eqGroupId, itemOwner, sku
        );

        return this.queryManager.selectListBySql(sql, params, ExtTbInventoryStock.class, 0, 0);
    }

    /** eqGroupId + locId + itemOwner + sku + lotNo 로 가용 재고 조회 (JOIN) */
    public List<ExtTbInventoryStock> findAvailableStockByLocIdWithLot(String eqGroupId,
                                                                      String locId,
                                                                      String itemOwner,
                                                                      String sku,
                                                                      String lotNo) {
        String sql = "SELECT s.* FROM tb_inventory_stock s " +
                "JOIN tb_inventory_location l ON l.stock_id = s.stock_id " +
                "WHERE l.loc_group = :locGroup " +
                "AND l.loc_id = :locId " +
                "AND s.eq_group_id = :eqGroupId " +
                "AND s.item_owner = :itemOwner " +
                "AND s.sku = :sku " +
                "AND s.lot_no = :lotNo " +
                "AND s.item_qty > 0 " +
                "AND s.stock_status = 0 " +
                "ORDER BY s.created_at ASC";

        Map<String, Object> params = ValueUtil.newMap(
                "locGroup,locId,eqGroupId,itemOwner,sku,lotNo",
                eqGroupId, locId, eqGroupId, itemOwner, sku, lotNo
        );

        return this.queryManager.selectListBySql(sql, params, ExtTbInventoryStock.class, 0, 0);
    }

    /** locId + eqGroupId 에 재고 존재 여부 확인 (JOIN) */
    public boolean existsByEqGroupIdAndLocId(String eqGroupId, String locId) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(locId)) {
            return false;
        }

        String sql = "SELECT COUNT(*) AS cnt FROM tb_inventory_stock s " +
                "JOIN tb_inventory_location l ON l.stock_id = s.stock_id " +
                "WHERE l.loc_group = :locGroup " +
                "AND l.loc_id = :locId " +
                "AND s.eq_group_id = :eqGroupId";

        Map<String, Object> params = ValueUtil.newMap(
                "locGroup,locId,eqGroupId",
                eqGroupId, locId, eqGroupId
        );

        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isNotEmpty(rows)) {
            Object cnt = rows.get(0).get("cnt");
            return cnt instanceof Number && ((Number) cnt).intValue() > 0;
        }
        return false;
    }

    /** eqGroupId + stockId 에 해당하는 모든 재고 행 조회 (파렛트 단위 처리용) */
    public List<ExtTbInventoryStock> findByEqGroupIdAndStockId(String eqGroupId, String stockId) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId)) {
            return java.util.Collections.emptyList();
        }

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("stock_id", stockId);
        return this.queryManager.selectList(ExtTbInventoryStock.class, condition);
    }

    // ========================================================================
    // 7. Stock Status Transitions (상태 전이)
    // ========================================================================

    /**
     * 해당 stockId의 모든 재고를 특정 상태로 전환.
     * 현재 상태가 expectedCurrent 일 때만 변경 (동시성 보호).
     * queryManager.update()를 사용하여 updatedAt 자동 갱신 보장.
     *
     * @param eqGroupId       설비 그룹 ID
     * @param stockId         대상 stockId
     * @param nextStatus      전환할 상태
     * @param expectedCurrent 예상 현재 상태 (null 이면 조건 없이 변경)
     * @return 변경된 row 수
     */
    @Transactional
    public int updateStockStatus(String eqGroupId, String stockId,
                                 StockStatus nextStatus, StockStatus expectedCurrent) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId) || nextStatus == null) {
            return 0;
        }

        List<ExtTbInventoryStock> targets = this.findByEqGroupIdAndStockId(eqGroupId, stockId);
        if (ValueUtil.isEmpty(targets)) {
            logger.warn("updateStockStatus: no stock found. eqGroupId={}, stockId={}", eqGroupId, stockId);
            return 0;
        }

        int updated = 0;
        for (ExtTbInventoryStock stock : targets) {
//            // expectedCurrent 조건 체크
//            if (expectedCurrent != null
//                    && !expectedCurrent.value().equals(stock.getStockStatus())) {
//                logger.warn("updateStockStatus: status mismatch. stockId={}, expected={}, actual={}",
//                        stockId, expectedCurrent.value(), stock.getStockStatus());
//                continue;
//            }

            stock.setStockStatus(nextStatus.value());
            this.queryManager.update(stock, "stockStatus");  // updatedAt 자동 갱신
            updated++;
        }

        logger.info("Updated stock_status. eqGroupId={}, stockId={}, next={}, expected={}, updatedRows={}",
                eqGroupId, stockId, nextStatus, expectedCurrent, updated);
        return updated;
    }

    /**
     * 해당 stockId의 모든 재고를 물리적으로 삭제.
     * queryManager.delete()를 사용 (ORM 이벤트 훅 정상 동작).
     */
    @Transactional
    public int deleteByStockId(String eqGroupId, String stockId) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId)) return 0;

        List<ExtTbInventoryStock> targets = this.findByEqGroupIdAndStockId(eqGroupId, stockId);
        if (ValueUtil.isEmpty(targets)) {
            logger.info("deleteByStockId: no stock to delete. eqGroupId={}, stockId={}", eqGroupId, stockId);
            return 0;
        }

        int deleted = 0;
        for (ExtTbInventoryStock stock : targets) {
            this.queryManager.delete(stock);
            deleted++;
        }

        logger.info("Deleted stock by stockId. eqGroupId={}, stockId={}, deletedRows={}",
                eqGroupId, stockId, deleted);
        return deleted;
    }

    /** stockId 로 재고 존재 여부 확인 (어떤 상태든) */
    public boolean existsByStockId(String eqGroupId, String stockId) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId)) return false;
        List<ExtTbInventoryStock> list = findByEqGroupIdAndStockId(eqGroupId, stockId);
        return !ValueUtil.isEmpty(list);
    }

    /** stockId 로 재고 1건 조회 (상태 판단용) */
    public ExtTbInventoryStock findAnyByStockId(String eqGroupId, String stockId) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId)) return null;
        List<ExtTbInventoryStock> list = findByEqGroupIdAndStockId(eqGroupId, stockId);
        return ValueUtil.isEmpty(list) ? null : list.get(0);
    }
}
