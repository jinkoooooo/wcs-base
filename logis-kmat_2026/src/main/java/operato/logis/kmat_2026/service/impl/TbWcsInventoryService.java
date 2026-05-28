package operato.logis.kmat_2026.service.impl;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Map;

/**
 * TbWcsInventory Entity Service (DB 전용)
 */
@Service
public class TbWcsInventoryService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TbWcsInventoryService.class);

    /**
     * ID로 조회
     */
    public TbWcsInventory findById(String id) {
        return this.queryManager.select(TbWcsInventory.class, id);
    }

    /**
     * eqGroupId + locCode + ownerCode + skuCode + lotNo 로 조회
     */
    public TbWcsInventory findByKey(String eqGroupId, String locCode, String ownerCode, String skuCode, String lotNo) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("loc_code", locCode);
        condition.addFilter("owner_code", ownerCode);
        condition.addFilter("sku_code", skuCode);
        condition.addFilter("lot_no", lotNo);
        return this.queryManager.selectByCondition(TbWcsInventory.class, condition);
    }

    /**
     * eqGroupId + locCode 로 조회
     */
    public List<TbWcsInventory> findByEqGroupIdAndLocCode(String eqGroupId, String locCode) {
        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("loc_code", locCode);
        return this.queryManager.selectList(TbWcsInventory.class, condition);
    }

    /**
     * eqGroupId + skuCode + ownerCode 로 가용 재고 조회 (qty - allocQty > 0)
     */
    public List<TbWcsInventory> findAvailableStock(String eqGroupId, String ownerCode, String skuCode) {
        String sql = "SELECT * FROM tb_wcs_inventory " +
                "WHERE eq_group_id = :eqGroupId " +
                "AND owner_code = :ownerCode " +
                "AND sku_code = :skuCode " +
                "AND (qty - alloc_qty) > 0 " +
                "AND stock_status = 0 " +
                "ORDER BY created_at ASC";

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,ownerCode,skuCode",
                eqGroupId, ownerCode, skuCode
        );

        return this.queryManager.selectListBySql(sql, params, TbWcsInventory.class, 0, 0);
    }

    /**
     * eqGroupId + skuCode + ownerCode + lotNo 로 가용 재고 조회
     */
    public List<TbWcsInventory> findAvailableStockWithLot(String eqGroupId, String ownerCode, String skuCode, String lotNo) {
        String sql = "SELECT * FROM tb_wcs_inventory " +
                "WHERE eq_group_id = :eqGroupId " +
                "AND owner_code = :ownerCode " +
                "AND sku_code = :skuCode " +
                "AND lot_no = :lotNo " +
                "AND (qty - alloc_qty) > 0 " +
                "AND stock_status = 0 " +
                "ORDER BY created_at ASC";

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,ownerCode,skuCode,lotNo",
                eqGroupId, ownerCode, skuCode, lotNo
        );

        return this.queryManager.selectListBySql(sql, params, TbWcsInventory.class, 0, 0);
    }

    /**
     * 신규 저장
     */
    public TbWcsInventory insert(TbWcsInventory entity) {
        logger.debug("Inserting TbWcsInventory: eqGroupId={}, locCode={}, skuCode={}, qty={}",
                entity.getEqGroupId(), entity.getLocCode(), entity.getSkuCode(), entity.getQty());
        this.queryManager.insert(entity);
        return entity;
    }

    /**
     * 수정
     */
    public TbWcsInventory update(TbWcsInventory entity) {
        logger.debug("Updating TbWcsInventory: id={}, qty={}, allocQty={}",
                entity.getId(), entity.getQty(), entity.getAllocQty());
        this.queryManager.update(entity);
        return entity;
    }

    /**
     * allocQty 증가 (예약)
     */
    public void increaseAllocQty(String id, int qty) {
        String sql = "UPDATE tb_wcs_inventory " +
                "SET alloc_qty = alloc_qty + :qty " +
                "WHERE id = :id " +
                "AND (qty - alloc_qty) >= :qty"; // [중요] 가용 재고가 있을 때만 업데이트

        Map<String, Object> params = ValueUtil.newMap("id,qty", id, qty);
        int updatedRows = this.queryManager.executeBySql(sql, params);

        if (updatedRows == 0) {
            throw new ElidomRuntimeException("재고 예약 실패: 가용 수량이 부족하거나 재고가 존재하지 않습니다. ID: " + id);
        }
    }

    /**
     * allocQty 감소 (예약 해제)
     */
    public void decreaseAllocQty(String id, int qty) {
        String sql = "UPDATE tb_wcs_inventory " +
                "SET alloc_qty = alloc_qty - :qty " +
                "WHERE id = :id " +
                "AND alloc_qty >= :qty"; // 예약된 수량보다 더 많이 풀 수 없음

        Map<String, Object> params = ValueUtil.newMap("id,qty", id, qty);
        int updatedRows = this.queryManager.executeBySql(sql, params);

        if (updatedRows == 0) {
            // 예약 해제 실패는 보통 데이터 정합성 오류이므로 경고 로그나 예외 처리
            throw new ElidomRuntimeException("재고 예약 해제 실패: 예약 수량이 부족하거나 재고가 없습니다. ID: " + id);
        }
    }

    /**
     * qty 증가 (입고 확정)
     */
    public void increaseQty(String id, int qty) {
        String sql = "UPDATE tb_wcs_inventory " +
                "SET qty = qty + :qty " +
                "WHERE id = :id";

        Map<String, Object> params = ValueUtil.newMap("id,qty", id, qty);
        int updatedRows = this.queryManager.executeBySql(sql, params);

        if (updatedRows == 0) {
            throw new ElidomRuntimeException("재고 증가 실패: 대상 재고가 존재하지 않습니다. ID: " + id);
        }
    }

    /**
     * qty 감소 + allocQty 감소 (출고/이동 확정)
     */
    public void decreaseQtyAndAllocQty(String id, int qty) {
        String sql = "UPDATE tb_wcs_inventory " +
                "SET qty = qty - :qty, " +
                "    alloc_qty = alloc_qty - :qty " +
                "WHERE id = :id " +
                "AND qty >= :qty AND alloc_qty >= :qty"; // [중요] 마이너스 재고 방지

        Map<String, Object> params = ValueUtil.newMap("id,qty", id, qty);
        int updatedRows = this.queryManager.executeBySql(sql, params);

        if (updatedRows == 0) {
            throw new ElidomRuntimeException("재고 차감 실패: 현재고 또는 예약고가 부족합니다. ID: " + id);
        }
    }

    /**
     * 신규 재고 생성 또는 기존 재고 qty 증가
     */
    public TbWcsInventory createOrIncreaseQty(String eqGroupId,
                                              String locCode,
                                              String ownerCode,
                                              String skuCode,
                                              String lotNo,
                                              int qty) {
        TbWcsInventory existing = this.findByKey(eqGroupId, locCode, ownerCode, skuCode, lotNo);
        if (existing != null) {
            this.increaseQty(existing.getId(), qty);
            return this.findById(existing.getId());
        }

        TbWcsInventory newInv = new TbWcsInventory();
        newInv.setEqGroupId(eqGroupId);
        newInv.setLocCode(locCode);
        newInv.setOwnerCode(ownerCode);
        newInv.setSkuCode(skuCode);
        newInv.setLotNo(lotNo);
        newInv.setQty(qty);
        newInv.setAllocQty(0);
        newInv.setStockStatus(0);
        newInv.setLockYn(0);
        return this.insert(newInv);
    }

    /**
     * eqGroupId + locCode + ownerCode + skuCode 로 가용 재고 조회
     */
    public List<TbWcsInventory> findAvailableStockByLocCode(String eqGroupId,
                                                            String locCode,
                                                            String ownerCode,
                                                            String skuCode) {
        String sql = "SELECT * FROM tb_wcs_inventory " +
                "WHERE eq_group_id = :eqGroupId " +
                "AND loc_code = :locCode " +
                "AND owner_code = :ownerCode " +
                "AND sku_code = :skuCode " +
                "AND (qty - alloc_qty) > 0 " +
                "AND stock_status = 0 " +
                "ORDER BY created_at ASC";

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,locCode,ownerCode,skuCode",
                eqGroupId, locCode, ownerCode, skuCode
        );

        return this.queryManager.selectListBySql(sql, params, TbWcsInventory.class, 0, 0);
    }

    /**
     * eqGroupId + locCode + ownerCode + skuCode + lotNo 로 가용 재고 조회
     */
    public List<TbWcsInventory> findAvailableStockByLocCodeWithLot(String eqGroupId,
                                                                   String locCode,
                                                                   String ownerCode,
                                                                   String skuCode,
                                                                   String lotNo) {
        String sql = "SELECT * FROM tb_wcs_inventory " +
                "WHERE eq_group_id = :eqGroupId " +
                "AND loc_code = :locCode " +
                "AND owner_code = :ownerCode " +
                "AND sku_code = :skuCode " +
                "AND lot_no = :lotNo " +
                "AND (qty - alloc_qty) > 0 " +
                "AND stock_status = 0 " +
                "ORDER BY created_at ASC";

        Map<String, Object> params = ValueUtil.newMap(
                "eqGroupId,locCode,ownerCode,skuCode,lotNo",
                eqGroupId, locCode, ownerCode, skuCode, lotNo
        );

        return this.queryManager.selectListBySql(sql, params, TbWcsInventory.class, 0, 0);
    }

    /**
     * locCode + eqGroupId 가 유효한지 간단 조회할 때 사용 가능
     */
    public boolean existsByEqGroupIdAndLocCode(String eqGroupId, String locCode) {
        if (!StringUtils.hasText(eqGroupId) || !StringUtils.hasText(locCode)) {
            return false;
        }

        Query condition = OrmUtil.newConditionForExecution();
        condition.addFilter("eq_group_id", eqGroupId);
        condition.addFilter("loc_code", locCode);

        TbWcsInventory found = this.queryManager.selectByCondition(TbWcsInventory.class, condition);
        return found != null;
    }
}