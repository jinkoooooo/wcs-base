package operato.logis.wcs.service.repository;

import operato.logis.inventory.consts.StockStatus;
import operato.logis.wcs.common.service.audit.AuditReason;
import operato.logis.wcs.consts.StockType;
import operato.logis.wcs.entity.ExtTbInventoryStock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ExtTbInventoryStock 영속성 전담 DAO.
 *
 * 한 재고 aggregate 의 조회·생성·수량 증감·상태/타입 전이·논리 삭제를 캡슐화한다.
 * 상태 전이는 CAS(expected -> target) 방식으로 동시성 race 를 방지한다.
 */
@Repository
public class InventoryStockRepository extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryStockRepository.class);

    /** PK 로 단건 조회. */
    public ExtTbInventoryStock findById(String id) {
        return this.queryManager.select(ExtTbInventoryStock.class, id);
    }

    /** (eqGroup, stockId, owner, sku, lot) 키로 단건 조회. lot 은 지정된 경우에만 필터. */
    public ExtTbInventoryStock findByKey(String eqGroupId, String stockId, String itemOwner, String sku, String lotNo) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("stock_id", stockId);
        condition.addFilter("item_owner", itemOwner);
        condition.addFilter("sku", sku);
        // lot 은 값이 있을 때만 필터에 추가
        if (ValueUtil.isNotEmpty(lotNo)) {
            condition.addFilter("lot_no", lotNo);
        }
        return this.queryManager.selectByCondition(ExtTbInventoryStock.class, condition);
    }

    /** 특정 로케이션(loc_id) 에 적재된 stock 목록 조회. */
    public List<ExtTbInventoryStock> findByEqGroupIdAndLocId(String eqGroupId, String locId) {
        String sql = """
            SELECT s.* FROM tb_inventory_stock s
              JOIN tb_inventory_location l ON l.stock_id = s.stock_id
             WHERE l.loc_group = :locGroup
               AND l.loc_id = :locId
               AND s.eq_group_id = :eqGroupId
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "locGroup,locId,eqGroupId",
                eqGroupId, locId, eqGroupId
        );
        return this.queryManager.selectListBySql(sql, params, ExtTbInventoryStock.class, 0, 0);
    }

    /** 가용(qty>0, status=0) NORMAL stock 조회. */
    public List<ExtTbInventoryStock> findAvailableStock(String eqGroupId, String itemOwner, String sku) {
        return findAvailableStock(eqGroupId, itemOwner, sku, StockType.NORMAL.code());
    }

    /** 가용 stock 을 stockType 지정으로 조회. created_at 오름차순(FIFO). */
    public List<ExtTbInventoryStock> findAvailableStock(String eqGroupId, String itemOwner, String sku, String stockType) {
        String sql = """
            SELECT * FROM tb_inventory_stock
             WHERE eq_group_id = :eqGroupId
               AND item_owner = :itemOwner
               AND sku = :sku
               AND item_qty > 0
               AND stock_status = 0
               AND stock_type   = :stockType
             ORDER BY created_at ASC
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,itemOwner,sku,stockType",
                eqGroupId, itemOwner, sku,
                ValueUtil.isEmpty(stockType) ? StockType.NORMAL.code() : stockType
        );
        return this.queryManager.selectListBySql(sql, params, ExtTbInventoryStock.class, 0, 0);
    }

    /** lot 단위 가용 NORMAL stock 조회. */
    public List<ExtTbInventoryStock> findAvailableStockWithLot(String eqGroupId, String itemOwner, String sku, String lotNo) {
        return findAvailableStockWithLot(eqGroupId, itemOwner, sku, lotNo, StockType.NORMAL.code());
    }

    /** lot + stockType 지정 가용 stock 조회. created_at 오름차순(FIFO). */
    public List<ExtTbInventoryStock> findAvailableStockWithLot(String eqGroupId, String itemOwner, String sku, String lotNo, String stockType) {
        String sql = """
            SELECT * FROM tb_inventory_stock
             WHERE eq_group_id = :eqGroupId
               AND item_owner = :itemOwner
               AND sku = :sku
               AND lot_no = :lotNo
               AND item_qty > 0
               AND stock_status = 0
               AND stock_type   = :stockType
             ORDER BY created_at ASC
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,itemOwner,sku,lotNo,stockType",
                eqGroupId, itemOwner, sku, lotNo,
                ValueUtil.isEmpty(stockType) ? StockType.NORMAL.code() : stockType
        );
        return this.queryManager.selectListBySql(sql, params, ExtTbInventoryStock.class, 0, 0);
    }

    /** 신규 stock insert. */
    public ExtTbInventoryStock insert(ExtTbInventoryStock entity) {
        this.queryManager.insert(entity);
        return entity;
    }

    /** stock 전체 컬럼 update. */
    public ExtTbInventoryStock update(ExtTbInventoryStock entity) {
        this.queryManager.update(entity);
        return entity;
    }

    /** 동일 키 stock 있으면 수량 증가, 없으면 신규 생성. 기본 인자 위임. */
    public ExtTbInventoryStock createOrIncreaseQty(String eqGroupId,
                                                   String stockId,
                                                   String itemOwner,
                                                   String sku,
                                                   String lotNo,
                                                   int qty) {
        return createOrIncreaseQty(eqGroupId, stockId, itemOwner, sku, lotNo, qty, null, null);
    }

    /** produce/expiry date 포함 변종. 초기 상태 IDLE 로 위임. */
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

    /** upsert 본체. 존재 시 서버측 산술로 수량 증가, 없으면 신규 insert. */
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
        if (ValueUtil.isNotEmpty(existing)) {
            // 서버측 산술(item_qty + qty) — race 방지 위해 raw SQL 유지
            String sql = """
                UPDATE tb_inventory_stock
                   SET item_qty = item_qty + :qty
                 WHERE id = :id
                """;
            Map<String, Object> params = ValueUtil.newMap("qty,id", qty, existing.getId());
            AuditReason.run("createOrIncreaseQty", () -> this.queryManager.executeBySql(sql, params));
            return this.findById(existing.getId());
        }

        // 신규 stock 행 구성 후 insert
        ExtTbInventoryStock newInv = new ExtTbInventoryStock();
        newInv.setEqGroupId(eqGroupId);
        newInv.setStockId(stockId);
        newInv.setItemOwner(itemOwner);
        newInv.setSku(sku);
        newInv.setItemCode(sku);
        newInv.setLotNo(lotNo);
        newInv.setItemQty(qty);
        newInv.setStockStatus(
                (ValueUtil.isEmpty(initialStatus) ? StockStatus.IDLE : initialStatus).value()
        );
        newInv.setStockType(StockType.NORMAL.code());
        newInv.setIsEnabled(true);
        newInv.setInbDatetime(new Date());
        newInv.setStockHeight(null);
        newInv.setProduceDate(produceDate);
        newInv.setExpiredDatetime(expiryDate);

        return this.insert(newInv);
    }

    /** 행 PK(id) 기준 수량 절대값 보정(audit). 보정 가드는 호출자(서비스) 책임. */
    public int updateItemQtyById(String id, int newQty, String reason) {
        if (ValueUtil.isEmpty(id)) return 0;
        String sql = "UPDATE tb_inventory_stock SET item_qty = :qty WHERE id = :id";
        Map<String, Object> params = ValueUtil.newMap("qty,id", newQty, id);
        return AuditReason.call(reason, () -> this.queryManager.executeBySql(sql, params));
    }

    /** 행 PK(id) 기준 비활성(audit). 물리삭제 대신 is_enabled=false. */
    public int disableById(String id, String reason) {
        if (ValueUtil.isEmpty(id)) return 0;
        String sql = "UPDATE tb_inventory_stock SET is_enabled = false WHERE id = :id";
        Map<String, Object> params = Map.of("id", id);
        return AuditReason.call(reason, () -> this.queryManager.executeBySql(sql, params));
    }

    /** 특정 로케이션의 가용 NORMAL stock 조회. */
    public List<ExtTbInventoryStock> findAvailableStockByLocId(String eqGroupId,
                                                               String locId,
                                                               String itemOwner,
                                                               String sku) {
        return findAvailableStockByLocId(eqGroupId, locId, itemOwner, sku, StockType.NORMAL.code());
    }

    /** 특정 로케이션의 가용 stock 을 stockType 지정 조회. created_at 오름차순(FIFO). */
    public List<ExtTbInventoryStock> findAvailableStockByLocId(String eqGroupId,
                                                               String locId,
                                                               String itemOwner,
                                                               String sku,
                                                               String stockType) {
        String sql = """
            SELECT s.* FROM tb_inventory_stock s
              JOIN tb_inventory_location l ON l.stock_id = s.stock_id
             WHERE l.loc_group = :locGroup
               AND l.loc_id = :locId
               AND s.eq_group_id = :eqGroupId
               AND s.item_owner = :itemOwner
               AND s.sku = :sku
               AND s.item_qty > 0
               AND s.stock_status = 0
               AND s.stock_type   = :stockType
             ORDER BY s.created_at ASC
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "locGroup,locId,eqGroupId,itemOwner,sku,stockType",
                eqGroupId, locId, eqGroupId, itemOwner, sku,
                ValueUtil.isEmpty(stockType) ? StockType.NORMAL.code() : stockType
        );
        return this.queryManager.selectListBySql(sql, params, ExtTbInventoryStock.class, 0, 0);
    }

    /** lot 단위 로케이션 가용 NORMAL stock 조회. */
    public List<ExtTbInventoryStock> findAvailableStockByLocIdWithLot(String eqGroupId,
                                                                      String locId,
                                                                      String itemOwner,
                                                                      String sku,
                                                                      String lotNo) {
        return findAvailableStockByLocIdWithLot(eqGroupId, locId, itemOwner, sku, lotNo, StockType.NORMAL.code());
    }

    /** lot + stockType 지정 로케이션 가용 stock 조회. created_at 오름차순(FIFO). */
    public List<ExtTbInventoryStock> findAvailableStockByLocIdWithLot(String eqGroupId,
                                                                      String locId,
                                                                      String itemOwner,
                                                                      String sku,
                                                                      String lotNo,
                                                                      String stockType) {
        String sql = """
            SELECT s.* FROM tb_inventory_stock s
              JOIN tb_inventory_location l ON l.stock_id = s.stock_id
             WHERE l.loc_group = :locGroup
               AND l.loc_id = :locId
               AND s.eq_group_id = :eqGroupId
               AND s.item_owner = :itemOwner
               AND s.sku = :sku
               AND s.lot_no = :lotNo
               AND s.item_qty > 0
               AND s.stock_status = 0
               AND s.stock_type   = :stockType
             ORDER BY s.created_at ASC
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "locGroup,locId,eqGroupId,itemOwner,sku,lotNo,stockType",
                eqGroupId, locId, eqGroupId, itemOwner, sku, lotNo,
                ValueUtil.isEmpty(stockType) ? StockType.NORMAL.code() : stockType
        );
        return this.queryManager.selectListBySql(sql, params, ExtTbInventoryStock.class, 0, 0);
    }

    /** 로케이션에 stock 적재 여부. */
    public boolean existsByEqGroupIdAndLocId(String eqGroupId, String locId) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(locId)) {
            return false;
        }
        String sql = """
            SELECT COUNT(*) AS cnt FROM tb_inventory_stock s
              JOIN tb_inventory_location l ON l.stock_id = s.stock_id
             WHERE l.loc_group = :locGroup
               AND l.loc_id = :locId
               AND s.eq_group_id = :eqGroupId
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "locGroup,locId,eqGroupId",
                eqGroupId, locId, eqGroupId
        );
        // count 결과 1행만 조회해 0 초과 여부 판정
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isNotEmpty(rows)) {
            Object cnt = rows.get(0).get("cnt");
            return cnt instanceof Number n && n.intValue() > 0;
        }
        return false;
    }

    /** (eqGroup, stockId) 로 stock 행 목록 조회. */
    public List<ExtTbInventoryStock> findByEqGroupIdAndStockId(String eqGroupId, String stockId) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId)) {
            return Collections.emptyList();
        }
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("stock_id", stockId);
        return this.queryManager.selectList(ExtTbInventoryStock.class, condition);
    }

    /** 상태 전이 (reason 없는 변종). */
    @Transactional(rollbackFor = Exception.class)
    public int updateStockStatus(String eqGroupId, String stockId,
                                 StockStatus nextStatus, StockStatus expectedCurrent) {
        return updateStockStatus(eqGroupId, stockId, nextStatus, expectedCurrent, null);
    }

    /** 상태 전이. reason 동반 시 audit log 에 운영자 사유/시스템 동작 기록. */
    @Transactional(rollbackFor = Exception.class)
    public int updateStockStatus(String eqGroupId, String stockId,
                                 StockStatus nextStatus, StockStatus expectedCurrent,
                                 String reason) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId) || ValueUtil.isEmpty(nextStatus)) {
            return 0;
        }

        List<ExtTbInventoryStock> targets = this.findByEqGroupIdAndStockId(eqGroupId, stockId);
        if (ValueUtil.isEmpty(targets)) {
            logger.warn("[ Inventory ][ Stock ] update status skipped, no stock - eqGroupId={}, stockId={}", eqGroupId, stockId);
            return 0;
        }

        // 대상 행마다 상태 세팅 후 audit update
        int updated = 0;
        for (ExtTbInventoryStock stock : targets) {
            stock.setStockStatus(nextStatus.value());
            if (StringUtils.hasText(reason)) {
                AuditReason.run(reason, () -> this.queryManager.update(stock, "stockStatus"));
            } else {
                this.queryManager.update(stock, "stockStatus");
            }
            updated++;
        }

        return updated;
    }

    /** 상태+타입 동시 전이 (reason 없는 변종). */
    @Transactional(rollbackFor = Exception.class)
    public int updateStockStatusAndType(String eqGroupId, String stockId,
                                        StockStatus nextStatus, StockType nextType) {
        return updateStockStatusAndType(eqGroupId, stockId, nextStatus, nextType, null);
    }

    /** 상태/타입 부분 전이. 지정된 컬럼만 갱신, reason 동반 시 사유 기록. */
    @Transactional(rollbackFor = Exception.class)
    public int updateStockStatusAndType(String eqGroupId, String stockId,
                                        StockStatus nextStatus, StockType nextType,
                                        String reason) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId)) return 0;
        if (ValueUtil.isEmpty(nextStatus) && ValueUtil.isEmpty(nextType)) return 0;

        List<ExtTbInventoryStock> targets = this.findByEqGroupIdAndStockId(eqGroupId, stockId);
        if (ValueUtil.isEmpty(targets)) {
            return 0;
        }

        int updated = 0;
        for (ExtTbInventoryStock stock : targets) {
            if (ValueUtil.isNotEmpty(nextStatus)) stock.setStockStatus(nextStatus.value());
            if (ValueUtil.isNotEmpty(nextType)) stock.setStockType(nextType.code());

            // 갱신할 컬럼만 선별 (status/type 중 지정된 것)
            String[] columns;
            if (ValueUtil.isNotEmpty(nextStatus) && ValueUtil.isNotEmpty(nextType)) {
                columns = new String[]{"stockStatus", "stockType"};
            } else if (ValueUtil.isNotEmpty(nextStatus)) {
                columns = new String[]{"stockStatus"};
            } else {
                columns = new String[]{"stockType"};
            }

            if (StringUtils.hasText(reason)) {
                AuditReason.run(reason, () -> this.queryManager.update(stock, columns));
            } else {
                this.queryManager.update(stock, columns);
            }
            updated++;
        }

        return updated;
    }

    /** stockType 만 전이. */
    @Transactional(rollbackFor = Exception.class)
    public int updateStockType(String eqGroupId, String stockId, StockType nextType) {
        return updateStockStatusAndType(eqGroupId, stockId, null, nextType);
    }

    /** (eqGroup, stockId) stock 물리 삭제. */
    @Transactional(rollbackFor = Exception.class)
    public int deleteByStockId(String eqGroupId, String stockId) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId)) return 0;

        List<ExtTbInventoryStock> targets = this.findByEqGroupIdAndStockId(eqGroupId, stockId);
        if (ValueUtil.isEmpty(targets)) {
            return 0;
        }

        int deleted = 0;
        for (ExtTbInventoryStock stock : targets) {
            this.queryManager.delete(stock);
            deleted++;
        }

        return deleted;
    }

    /** stockId 존재 여부. */
    public boolean existsByStockId(String eqGroupId, String stockId) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId)) return false;
        List<ExtTbInventoryStock> list = findByEqGroupIdAndStockId(eqGroupId, stockId);
        return !ValueUtil.isEmpty(list);
    }

    /** stockId 의 첫 행 반환, 없으면 null. */
    public ExtTbInventoryStock findAnyByStockId(String eqGroupId, String stockId) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId)) return null;
        List<ExtTbInventoryStock> list = findByEqGroupIdAndStockId(eqGroupId, stockId);
        return ValueUtil.isEmpty(list) ? null : list.get(0);
    }

    /** origin_host_order_key 갱신. */
    @Transactional(rollbackFor = Exception.class)
    public int updateSourceHostOrderKey(String eqGroupId, String stockId, String sourceHostOrderKey) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId)
                || !StringUtils.hasText(sourceHostOrderKey)) return 0;
        List<ExtTbInventoryStock> targets = this.findByEqGroupIdAndStockId(eqGroupId, stockId);
        if (ValueUtil.isEmpty(targets)) return 0;
        int updated = 0;
        for (ExtTbInventoryStock s : targets) {
            s.setOriginHostOrderKey(sourceHostOrderKey);
            this.queryManager.update(s, "originHostOrderKey");
            updated++;
        }
        return updated;
    }

    /**
     * origin_host_order_key 기준으로 특정 stock_type 인 stock 의 (eq_group_id, stock_id) 키만 조회.
     * QC 결과 전파에서 QC_PENDING 행 식별에 사용.
     */
    public List<Map> findStockIdsByOriginHostOrderKeyAndType(String hostOrderKey, String stockTypeCode) {
        if (!StringUtils.hasText(hostOrderKey) || !StringUtils.hasText(stockTypeCode)) {
            return Collections.emptyList();
        }
        String sql = """
            SELECT s.eq_group_id, s.stock_id
              FROM tb_inventory_stock s
             WHERE s.origin_host_order_key = :hostOrderKey
               AND s.stock_type = :qcPendingType
            """;
        Map<String, Object> params = ValueUtil.newMap("hostOrderKey,qcPendingType",
                hostOrderKey, stockTypeCode);
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    /**
     * 사용기한 만료된 NORMAL stock 조회.
     *
     * expired_datetime < NOW() AND stock_type = NORMAL AND stock_status = IDLE.
     * MaintenanceJobs 의 ExpiryCheck 잡이 호출하여 HOLD + QC_FAIL 로 전이.
     */
    public List<Map> findExpiredNormalStocks() {
        String sql = """
            SELECT eq_group_id, stock_id
              FROM tb_inventory_stock
             WHERE is_enabled       = TRUE
               AND stock_type       = :normal
               AND stock_status     = 0
               AND expired_datetime IS NOT NULL
               AND expired_datetime < NOW()
            """;
        Map<String, Object> params = ValueUtil.newMap("normal", StockType.NORMAL.code());
        return this.queryManager.selectListBySql(sql, params, Map.class, 0, 0);
    }

    /**
     * SKU + lot 단위 NORMAL 재고 집계 (QR 태블릿용).
     * 응답 Map 키: total_qty, row_count.
     */
    public Map<String, Object> aggregateBySkuAndLot(String sku, String lotNo) {
        if (!StringUtils.hasText(sku)) {
            return Map.of("total_qty", 0, "row_count", 0);
        }
        String sql = """
            SELECT COALESCE(SUM(s.item_qty), 0) AS total_qty,
                   COUNT(*)                     AS row_count
              FROM tb_inventory_stock s
             WHERE s.is_enabled   = TRUE
               AND s.sku          = :sku
               AND s.lot_no       = :lotNo
               AND s.stock_type   = :stockType
               AND s.stock_status = 0
            """;
        Map<String, Object> params = ValueUtil.newMap("sku,lotNo,stockType",
                sku, ValueUtil.isEmpty(lotNo) ? "" : lotNo, StockType.NORMAL.code());
        List<Map> rows = this.queryManager.selectListBySql(sql, params, Map.class, 0, 1);
        if (ValueUtil.isEmpty(rows)) {
            return Map.of("total_qty", 0, "row_count", 0);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) rows.get(0);
        return result;
    }

    /**
     * 폐기 출고 완료 — 물리 삭제 대신 논리 삭제.
     * item_qty=0 + is_enabled=false 마크. 감사 추적용으로 행 자체는 남긴다.
     */
    @Transactional(rollbackFor = Exception.class)
    public int markDisposed(String eqGroupId, String stockId) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(stockId)) return 0;

        String sql = """
            UPDATE tb_inventory_stock
               SET item_qty   = 0,
                   is_enabled = false
             WHERE eq_group_id = :eqGroupId
               AND stock_id    = :stockId
            """;
        Map<String, Object> params = ValueUtil.newMap("eqGroupId,stockId", eqGroupId, stockId);
        this.queryManager.executeBySql(sql, params);
        return 1;
    }

    /**
     * fromLocId 의 stock 행이 expected 상태일 때만 target 상태로 CAS 전이.
     * 반환값은 전이된 행 수(0 이면 expected 불일치 또는 stock 없음).
     */
    public int transitionStatusByLoc(String eqGroupId, String locId,
                                     StockStatus expected, StockStatus target) {
        String sql = """
            UPDATE tb_inventory_stock
               SET stock_status = :targetStatus,
                   updated_at = NOW()
             WHERE eq_group_id = :eqGroupId
               AND stock_id IN (
                     SELECT stock_id FROM tb_inventory_location
                      WHERE eq_group_id = :eqGroupId
                        AND loc_id = :locId
                        AND stock_id IS NOT NULL
                   )
               AND stock_status = :expectedStatus
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,locId,expectedStatus,targetStatus", eqGroupId,
                locId,
                expected.value(),
                target.value()
        );
        return this.queryManager.executeBySql(sql, params);
    }

    /** stockId 단위 CAS 전이. expected 일치 시에만 target 으로, 성공 여부 반환. */
    public boolean transitionStatusByStockId(String eqGroupId, String stockId,
                                             StockStatus expected, StockStatus target) {
        String sql = """
            UPDATE tb_inventory_stock
               SET stock_status = :targetStatus,
                   updated_at = NOW()
             WHERE eq_group_id = :eqGroupId
               AND stock_id = :stockId
               AND stock_status = :expectedStatus
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,stockId,expectedStatus,targetStatus", eqGroupId,
                stockId,
                expected.value(),
                target.value()
        );
        return this.queryManager.executeBySql(sql, params) > 0;
    }

    /**
     * 여러 expected 상태 중 하나라도 매치되면 target 으로 전이.
     * 셔틀 reserve 가 IDLE/HOST_PENDING 둘 다 시작점으로 인정할 때 사용.
     */
    public int transitionStatusByLocFromAny(String eqGroupId, String locId,
                                            List<StockStatus> expectedAny, StockStatus target) {
        List<Integer> expectedValues = expectedAny.stream()
                .map(StockStatus::value)
                .toList();

        String sql = """
            UPDATE tb_inventory_stock
               SET stock_status = :targetStatus,
                   updated_at = NOW()
             WHERE eq_group_id = :eqGroupId
               AND stock_id IN (
                     SELECT stock_id FROM tb_inventory_location
                      WHERE eq_group_id = :eqGroupId
                        AND loc_id = :locId
                        AND stock_id IS NOT NULL
                   )
               AND stock_status IN (:expectedStatuses)
            """;
        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,locId,expectedStatuses,targetStatus",
                eqGroupId,
                locId,
                expectedValues,
                target.value()
        );
        return this.queryManager.executeBySql(sql, params);
    }

    /**
     * stock_type 필터 포함 CAS 전이.
     * matchType=true 면 stock_type = filterType 인 행만, false 면 stock_type <> filterType 인 행만 전이.
     * HOST_PENDING 롤백 시 stock_type 으로 원래 상태 추론에 사용. 반환값은 전이된 행 수.
     */
    @Transactional(rollbackFor = Exception.class)
    public int transitionStatusByLocWithTypeFilter(String eqGroupId, String locId,
                                                   StockStatus expected, StockStatus target,
                                                   boolean matchType, StockType filterType) {
        // matchType 에 따라 stock_type 일치/불일치 조건 결정
        String typeCondition = matchType
                ? " AND stock_type = :filterType"
                : " AND stock_type <> :filterType";

        String sql = """
            UPDATE tb_inventory_stock
               SET stock_status = :targetStatus,
                   updated_at = NOW()
             WHERE eq_group_id = :eqGroupId
               AND stock_id IN (
                     SELECT stock_id FROM tb_inventory_location
                      WHERE eq_group_id = :eqGroupId
                        AND loc_id = :locId
                        AND stock_id IS NOT NULL
                   )
               AND stock_status = :expectedStatus""" + typeCondition;

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,locId,expectedStatus,targetStatus,filterType", eqGroupId, locId, expected.value(), target.value(), filterType.code()
        );
        return this.queryManager.executeBySql(sql, params);
    }
}
