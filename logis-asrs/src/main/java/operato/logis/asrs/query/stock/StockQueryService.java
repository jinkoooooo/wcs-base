package operato.logis.asrs.query.stock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import operato.logis.asrs.entity.TbAcStorageArea;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.entity.TbAcItemMaster;
import operato.logis.asrs.entity.TbAcLocation;
import operato.logis.asrs.entity.TbAcStockUnit;
import operato.logis.asrs.query.item.ItemQueryService;
import operato.logis.asrs.query.location.LocationQueryService;
import operato.logis.asrs.query.stock.model.CurrentStockView;
import operato.logis.asrs.query.stock.model.StockTxnHistoryView;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 재고 조회 전용 통합 서비스.
 *
 * <p>
 * 역할을 아래 3가지로 통합한다.
 * </p>
 *
 * <ol>
 *   <li>내부 코어용 entity lookup</li>
 *   <li>외부 현재고 조회(Current Stock Read Model)</li>
 *   <li>외부 이력 조회(Transaction History Read Model)</li>
 * </ol>
 *
 * <p>
 * 외부 조회는 business key 기준으로 수행한다.
 * 내부 저장/FK 처리를 위해서만 row id 를 사용한다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class StockQueryService extends AbstractQueryService {

    private final ItemQueryService itemQueryService;
    private final LocationQueryService locationQueryService;

    /* =========================================================
     * 1. 내부 코어용 entity lookup
     * ========================================================= */

    /**
     * 재고 단위 row id 기준 활성 재고 조회.
     *
     * <p>
     * 신규 업무 로직에서는 stockUnitNo 사용이 원칙이지만,
     * 내부 FK 처리/legacy 호환을 위해 유지한다.
     * </p>
     *
     * @param stockUnitId 재고 단위 row id
     * @return TbAcStockUnit
     * @deprecated 신규 업무 로직에서는 findByStockUnitNo 사용 권장
     */
    @Deprecated
    public TbAcStockUnit findStockUnit(String stockUnitId) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("id", stockUnitId);
        condition.addFilter("active_yn", "Y");

        TbAcStockUnit stockUnit = this.queryManager.select(TbAcStockUnit.class, condition);
        if (stockUnit == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Stock unit not found. stockUnitId=" + stockUnitId
            );
        }
        return stockUnit;
    }

    /**
     * 재고 단위 번호 기준 활성 재고 조회.
     *
     * @param stockUnitNo 재고 단위 번호
     * @return TbAcStockUnit
     */
    public TbAcStockUnit findByStockUnitNo(String stockUnitNo) {
        TbAcStockUnit stockUnit = findByStockUnitNoOrNull(stockUnitNo);
        if (stockUnit == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Stock unit not found. stockUnitNo=" + stockUnitNo
            );
        }
        return stockUnit;
    }

    /**
     * 재고 단위 번호 기준 활성 재고 조회. 없으면 null 반환.
     *
     * @param stockUnitNo 재고 단위 번호
     * @return TbAcStockUnit 또는 null
     */
    public TbAcStockUnit findByStockUnitNoOrNull(String stockUnitNo) {
        if (ValueUtil.isEmpty(stockUnitNo)) {
            return null;
        }

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("stock_unit_no", stockUnitNo);
        condition.addFilter("active_yn", "Y");

        return this.queryManager.select(TbAcStockUnit.class, condition);
    }

    /**
     * 재고 단위 번호 기준 전체 재고 조회. active_yn 여부와 무관.
     *
     * <p>
     * stock_unit_no 중복 체크, 원 출고 재고 추적 등에 사용한다.
     * </p>
     *
     * @param stockUnitNo 재고 단위 번호
     * @return TbAcStockUnit 또는 null
     */
    public TbAcStockUnit findAnyByStockUnitNoOrNull(String stockUnitNo) {
        if (ValueUtil.isEmpty(stockUnitNo)) {
            return null;
        }

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("stock_unit_no", stockUnitNo);

        return this.queryManager.select(TbAcStockUnit.class, condition);
    }

    /**
     * 재고 단위 번호 기준 전체 재고 조회. active_yn 여부와 무관.
     *
     * @param stockUnitNo 재고 단위 번호
     * @return TbAcStockUnit
     */
    public TbAcStockUnit findAnyByStockUnitNo(String stockUnitNo) {
        TbAcStockUnit stockUnit = findAnyByStockUnitNoOrNull(stockUnitNo);
        if (stockUnit == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Stock unit not found. stockUnitNo=" + stockUnitNo
            );
        }
        return stockUnit;
    }

    /**
     * 특정 로케이션 row id 기준 활성 재고 목록 조회.
     *
     * <p>
     * 혼적 검증, 로케이션 적재 상태 점검에 사용한다.
     * </p>
     *
     * @param locationId 로케이션 row id
     * @return 활성 재고 목록
     */
    public List<TbAcStockUnit> findActiveStockByLocation(String locationId) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("current_location_id", locationId);
        condition.addFilter("active_yn", "Y");

        condition.addOrder("inbound_at", true);
        condition.addOrder("stock_unit_no", true);

        return this.queryManager.selectList(TbAcStockUnit.class, condition);
    }

    /* =========================================================
     * 2. 외부 현재고 조회 (business key 기준)
     * ========================================================= */

    /**
     * 재고단위번호 기준 현재고 단건 조회.
     *
     * @param stockUnitNo 재고 단위 번호
     * @return CurrentStockView
     */
    public CurrentStockView findCurrentStockByStockUnitNo(String stockUnitNo) {
        if (ValueUtil.isEmpty(stockUnitNo)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "stockUnitNo is empty."
            );
        }

        String sql =
                "select " +
                        "    su.id as stock_unit_id, " +
                        "    su.stock_unit_no, " +
                        "    su.item_id, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    su.lot_id, " +
                        "    l2.lot_no, " +
                        "    su.current_location_id, " +
                        "    a.area_code, " +
                        "    l.location_code, " +
                        "    su.stock_unit_type, " +
                        "    su.qty, " +
                        "    su.reserved_qty, " +
                        "    su.stock_status_code, " +
                        "    su.hold_yn, " +
                        "    su.inbound_at, " +
                        "    su.last_moved_at, " +
                        "    su.active_yn " +
                        "  from logis_asrs.tb_ac_stock_unit su " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on su.item_id = i.id " +
                        "  join logis_asrs.tb_ac_location l " +
                        "    on su.current_location_id = l.id " +
                        "  join logis_asrs.tb_ac_storage_area a " +
                        "    on l.area_id = a.id " +
                        "  left join logis_asrs.tb_ac_lot l2 " +
                        "    on su.lot_id = l2.id " +
                        " where su.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and l.domain_id = :domainId " +
                        "   and a.domain_id = :domainId " +
                        "   and su.stock_unit_no = :stockUnitNo " +
                        "   and su.active_yn = 'Y' ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,stockUnitNo",
                Domain.currentDomainId(), stockUnitNo
        );

        List<CurrentStockView> list = this.queryManager.selectListBySql(sql, param, CurrentStockView.class, 0, 0);

        if (list == null || list.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Current stock not found. stockUnitNo=" + stockUnitNo
            );
        }

        return list.get(0);
    }

    /**
     * 영역코드 + 품목코드 기준 현재고 목록 조회.
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @return 현재고 목록
     */
    public List<CurrentStockView> findCurrentStocksByItemCode(String areaCode, String itemCode) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(itemCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "itemCode is empty.");
        }

        TbAcItemMaster item = itemQueryService.findItemByCode(itemCode);

        String sql =
                "select " +
                        "    su.id as stock_unit_id, " +
                        "    su.stock_unit_no, " +
                        "    su.item_id, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    su.lot_id, " +
                        "    l2.lot_no, " +
                        "    su.current_location_id, " +
                        "    a.area_code, " +
                        "    l.location_code, " +
                        "    su.stock_unit_type, " +
                        "    su.qty, " +
                        "    su.reserved_qty, " +
                        "    su.stock_status_code, " +
                        "    su.hold_yn, " +
                        "    su.inbound_at, " +
                        "    su.last_moved_at, " +
                        "    su.active_yn " +
                        "  from logis_asrs.tb_ac_stock_unit su " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on su.item_id = i.id " +
                        "  join logis_asrs.tb_ac_location l " +
                        "    on su.current_location_id = l.id " +
                        "  join logis_asrs.tb_ac_storage_area a " +
                        "    on l.area_id = a.id " +
                        "  left join logis_asrs.tb_ac_lot l2 " +
                        "    on su.lot_id = l2.id " +
                        " where su.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and l.domain_id = :domainId " +
                        "   and a.domain_id = :domainId " +
                        "   and a.area_code = :areaCode " +
                        "   and su.item_id = :itemId " +
                        "   and su.active_yn = 'Y' " +
                        " order by su.inbound_at asc, su.stock_unit_no asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaCode,itemId",
                Domain.currentDomainId(), areaCode, item.getId()
        );

        return this.queryManager.selectListBySql(sql, param, CurrentStockView.class, 0, 0);
    }

    /**
     * 영역코드 + 로케이션코드 기준 현재고 목록 조회.
     *
     * @param areaCode 영역 코드
     * @param locationCode 로케이션 코드
     * @return 현재고 목록
     */
    public List<CurrentStockView> findCurrentStocksByLocationCode(String areaCode, String locationCode) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(locationCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "locationCode is empty.");
        }

        TbAcLocation location = locationQueryService.findLocationByCode(areaCode, locationCode);

        String sql =
                "select " +
                        "    su.id as stock_unit_id, " +
                        "    su.stock_unit_no, " +
                        "    su.item_id, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    su.lot_id, " +
                        "    l2.lot_no, " +
                        "    su.current_location_id, " +
                        "    a.area_code, " +
                        "    l.location_code, " +
                        "    su.stock_unit_type, " +
                        "    su.qty, " +
                        "    su.reserved_qty, " +
                        "    su.stock_status_code, " +
                        "    su.hold_yn, " +
                        "    su.inbound_at, " +
                        "    su.last_moved_at, " +
                        "    su.active_yn " +
                        "  from logis_asrs.tb_ac_stock_unit su " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on su.item_id = i.id " +
                        "  join logis_asrs.tb_ac_location l " +
                        "    on su.current_location_id = l.id " +
                        "  join logis_asrs.tb_ac_storage_area a " +
                        "    on l.area_id = a.id " +
                        "  left join logis_asrs.tb_ac_lot l2 " +
                        "    on su.lot_id = l2.id " +
                        " where su.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and l.domain_id = :domainId " +
                        "   and a.domain_id = :domainId " +
                        "   and su.current_location_id = :locationId " +
                        "   and su.active_yn = 'Y' " +
                        " order by su.inbound_at asc, su.stock_unit_no asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,locationId",
                Domain.currentDomainId(), location.getId()
        );

        return this.queryManager.selectListBySql(sql, param, CurrentStockView.class, 0, 0);
    }

    /* =========================================================
     * 3. 외부 이력 조회 (business key 기준)
     * ========================================================= */

    /**
     * 재고단위번호 기준 이력 조회.
     *
     * <p>
     * 이력 조회는 활성 여부와 무관하게 전체 stock_unit 를 대상으로 해야 한다.
     * 따라서 active_yn = Y 전용 조회가 아니라 findAnyByStockUnitNo 를 사용한다.
     * </p>
     *
     * @param stockUnitNo 재고 단위 번호
     * @return 이력 목록
     */
    public List<StockTxnHistoryView> findTxnHistoryByStockUnitNo(String stockUnitNo) {
        if (ValueUtil.isEmpty(stockUnitNo)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "stockUnitNo is empty."
            );
        }

        // 중요:
        // 이력 조회는 이미 FULL_OUT 되어 active_yn = N 인 재고도 조회 가능해야 한다.
        TbAcStockUnit stockUnit = findAnyByStockUnitNo(stockUnitNo);

        String sql =
                "select " +
                        "    t.id as stock_txn_id, " +
                        "    t.txn_no, " +
                        "    t.txn_type, " +
                        "    t.stock_unit_id, " +
                        "    su.stock_unit_no, " +
                        "    t.item_id, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    t.lot_id, " +
                        "    l2.lot_no, " +
                        "    t.from_location_id, " +
                        "    lf.location_code as from_location_code, " +
                        "    t.to_location_id, " +
                        "    lt.location_code as to_location_code, " +
                        "    t.qty, " +
                        "    t.ref_doc_type, " +
                        "    t.ref_doc_no, " +
                        "    t.ref_line_no, " +
                        "    t.reason_code, " +
                        "    t.remark, " +
                        "    t.txn_at " +
                        "  from logis_asrs.tb_ac_stock_txn t " +
                        "  join logis_asrs.tb_ac_stock_unit su " +
                        "    on t.stock_unit_id = su.id " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on t.item_id = i.id " +
                        "  left join logis_asrs.tb_ac_lot l2 " +
                        "    on t.lot_id = l2.id " +
                        "  left join logis_asrs.tb_ac_location lf " +
                        "    on t.from_location_id = lf.id " +
                        "  left join logis_asrs.tb_ac_location lt " +
                        "    on t.to_location_id = lt.id " +
                        " where t.domain_id = :domainId " +
                        "   and su.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and t.stock_unit_id = :stockUnitId " +
                        " order by t.txn_at asc, t.created_at asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,stockUnitId",
                Domain.currentDomainId(), stockUnit.getId()
        );

        return this.queryManager.selectListBySql(sql, param, StockTxnHistoryView.class, 0, 0);
    }

    /**
     * 참조문서번호 기준 이력 조회.
     *
     * @param refDocNo 참조 문서 번호
     * @return 이력 목록
     */
    public List<StockTxnHistoryView> findTxnHistoryByRefDocNo(String refDocNo) {
        if (ValueUtil.isEmpty(refDocNo)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "refDocNo is empty."
            );
        }

        String sql =
                "select " +
                        "    t.id as stock_txn_id, " +
                        "    t.txn_no, " +
                        "    t.txn_type, " +
                        "    t.stock_unit_id, " +
                        "    su.stock_unit_no, " +
                        "    t.item_id, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    t.lot_id, " +
                        "    l2.lot_no, " +
                        "    t.from_location_id, " +
                        "    lf.location_code as from_location_code, " +
                        "    t.to_location_id, " +
                        "    lt.location_code as to_location_code, " +
                        "    t.qty, " +
                        "    t.ref_doc_type, " +
                        "    t.ref_doc_no, " +
                        "    t.ref_line_no, " +
                        "    t.reason_code, " +
                        "    t.remark, " +
                        "    t.txn_at " +
                        "  from logis_asrs.tb_ac_stock_txn t " +
                        "  join logis_asrs.tb_ac_stock_unit su " +
                        "    on t.stock_unit_id = su.id " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on t.item_id = i.id " +
                        "  left join logis_asrs.tb_ac_lot l2 " +
                        "    on t.lot_id = l2.id " +
                        "  left join logis_asrs.tb_ac_location lf " +
                        "    on t.from_location_id = lf.id " +
                        "  left join logis_asrs.tb_ac_location lt " +
                        "    on t.to_location_id = lt.id " +
                        " where t.domain_id = :domainId " +
                        "   and su.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and t.ref_doc_no = :refDocNo " +
                        " order by t.txn_at asc, t.created_at asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,refDocNo",
                Domain.currentDomainId(), refDocNo
        );

        return this.queryManager.selectListBySql(sql, param, StockTxnHistoryView.class, 0, 0);
    }


    /**
     * 출고 대상 현재고 전체 조회.
     *
     * 목적:
     * - 출고 화면에서 조회조건 없이 전체 현재고를 조회할 수 있도록 지원
     *
     * 기본 정책:
     * - activeYn 가 없으면 Y 기본 적용
     * - excludeOutYn = Y 이면 OUT 상태 제외
     */
    public List<CurrentStockView> findCurrentStocks(String areaCode,
                                                    String activeYn,
                                                    String excludeOutYn) {
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append("    su.stock_unit_no as stock_unit_no, ");
        sql.append("    im.item_code as item_code, ");
        sql.append("    im.item_name as item_name, ");
        sql.append("    sa.area_code as area_code, ");
        sql.append("    loc.location_code as location_code, ");
        sql.append("    su.qty as qty, ");
        sql.append("    su.reserved_qty as reserved_qty, ");
        sql.append("    lot.lot_no as lot_no, ");
        sql.append("    su.stock_status_code as stock_status_code, ");
        sql.append("    su.active_yn as active_yn, ");
        sql.append("    to_char(su.last_moved_at, 'YYYY-MM-DD HH24:MI:SS') as last_txn_at ");
        sql.append("from tb_ac_stock_unit su ");
        sql.append("inner join tb_ac_item_master im on im.id = su.item_id ");
        sql.append("inner join tb_ac_location loc on loc.id = su.current_location_id ");
        sql.append("inner join tb_ac_storage_area sa on sa.id = loc.area_id ");
        sql.append("left join tb_ac_lot lot on lot.id = su.lot_id ");
        sql.append("where 1 = 1 ");

        Map<String, Object> params = new HashMap<String, Object>();

        if (areaCode != null && !areaCode.trim().isEmpty()) {
            sql.append("and sa.area_code = :areaCode ");
            params.put("areaCode", areaCode.trim());
        }

        if (activeYn != null && !activeYn.trim().isEmpty()) {
            sql.append("and su.active_yn = :activeYn ");
            params.put("activeYn", activeYn.trim());
        } else {
            sql.append("and su.active_yn = 'Y' ");
        }

        if (!"N".equalsIgnoreCase(excludeOutYn)) {
            sql.append("and coalesce(su.stock_status_code, '') <> 'OUT' ");
        }

        sql.append("and coalesce(su.qty, 0) > 0 ");
        sql.append("order by sa.area_code asc, loc.location_code asc, su.stock_unit_no asc ");

        return this.queryManager.selectListBySql(
                sql.toString(),
                params,
                CurrentStockView.class,
                0,
                0
        );
    }

}