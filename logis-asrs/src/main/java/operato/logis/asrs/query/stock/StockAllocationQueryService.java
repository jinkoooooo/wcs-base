package operato.logis.asrs.query.stock;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.entity.TbAcStockAllocation;
import operato.logis.asrs.entity.TbAcStockUnit;
import operato.logis.asrs.query.stock.model.StockAllocationView;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 재고 할당 조회 전용 서비스.
 *
 * <p>
 * 역할을 아래 2가지로 구성한다.
 * </p>
 *
 * <ol>
 *   <li>내부 코어용 allocation entity 조회</li>
 *   <li>외부 조회용 allocation read model 조회</li>
 * </ol>
 *
 * <p>
 * 외부 조회는 business key 기준으로 수행한다.
 * 내부 저장/FK 처리 시에만 row id 를 사용한다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class StockAllocationQueryService extends AbstractQueryService {

    /* =========================================================
     * 1. 내부 코어용 entity lookup
     * ========================================================= */

    /**
     * 재고단위 row id 기준 활성 할당 목록 조회.
     *
     * <p>
     * 활성 상태는 ALLOCATED 만 대상으로 본다.
     * </p>
     *
     * @param stockUnitId 재고 단위 row id
     * @return 활성 할당 목록
     */
    public List<TbAcStockAllocation> findActiveAllocationsByStockUnitId(String stockUnitId) {
        if (ValueUtil.isEmpty(stockUnitId)) {
            return Collections.emptyList();
        }

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("stock_unit_id", stockUnitId);
        condition.addFilter("alloc_status_code", "ALLOCATED");

        condition.addOrder("allocated_at", true);
        condition.addOrder("id", true);

        return this.queryManager.selectList(TbAcStockAllocation.class, condition);
    }

    /**
     * 재고단위 row id + 참조문서 기준 활성 할당 목록 조회.
     *
     * @param stockUnitId 재고 단위 row id
     * @param refDocType 참조 문서 유형
     * @param refDocNo 참조 문서 번호
     * @param refLineNo 참조 문서 라인 번호
     * @return 활성 할당 목록
     */
    public List<TbAcStockAllocation> findActiveAllocationsByRefDoc(String stockUnitId,
                                                                   String refDocType,
                                                                   String refDocNo,
                                                                   String refLineNo) {

        if (ValueUtil.isEmpty(stockUnitId) || ValueUtil.isEmpty(refDocNo)) {
            return Collections.emptyList();
        }

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("stock_unit_id", stockUnitId);
        condition.addFilter("alloc_status_code", "ALLOCATED");
        condition.addFilter("ref_doc_no", refDocNo);

        if (ValueUtil.isNotEmpty(refDocType)) {
            condition.addFilter("ref_doc_type", refDocType);
        }
        if (ValueUtil.isNotEmpty(refLineNo)) {
            condition.addFilter("ref_line_no", refLineNo);
        }

        condition.addOrder("allocated_at", true);
        condition.addOrder("id", true);

        return this.queryManager.selectList(TbAcStockAllocation.class, condition);
    }

    /**
     * 재고단위번호 기준 활성 할당 목록 조회.
     *
     * @param stockUnitNo 재고 단위 번호
     * @param stockQueryService 재고 조회 서비스
     * @return 활성 할당 목록
     */
    public List<TbAcStockAllocation> findActiveAllocationsByStockUnitNo(String stockUnitNo,
                                                                        StockQueryService stockQueryService) {
        if (ValueUtil.isEmpty(stockUnitNo)) {
            return Collections.emptyList();
        }

        TbAcStockUnit stockUnit = stockQueryService.findByStockUnitNo(stockUnitNo);
        return findActiveAllocationsByStockUnitId(stockUnit.getId());
    }

    /* =========================================================
     * 2. 외부 조회용 allocation read model
     * ========================================================= */

    /**
     * 재고단위번호 기준 전체 할당 이력 조회.
     *
     * @param stockUnitNo 재고 단위 번호
     * @param stockQueryService 재고 조회 서비스
     * @return 할당 목록
     */
    public List<StockAllocationView> findAllocationsByStockUnitNo(String stockUnitNo,
                                                                  StockQueryService stockQueryService) {
        if (ValueUtil.isEmpty(stockUnitNo)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "stockUnitNo is empty."
            );
        }

        TbAcStockUnit stockUnit = stockQueryService.findAnyByStockUnitNo(stockUnitNo);

        String sql =
                "select " +
                        "    a.id as allocation_id, " +
                        "    a.stock_unit_id, " +
                        "    su.stock_unit_no, " +
                        "    a.item_id, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    su.current_location_id, " +
                        "    ar.area_code, " +
                        "    l.location_code, " +
                        "    a.allocated_qty, " +
                        "    a.alloc_status_code, " +
                        "    a.ref_doc_type, " +
                        "    a.ref_doc_no, " +
                        "    a.ref_line_no, " +
                        "    a.due_date, " +
                        "    a.allocated_at " +
                        "  from logis_asrs.tb_ac_stock_allocation a " +
                        "  join logis_asrs.tb_ac_stock_unit su " +
                        "    on a.stock_unit_id = su.id " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on a.item_id = i.id " +
                        "  left join logis_asrs.tb_ac_location l " +
                        "    on su.current_location_id = l.id " +
                        "  left join logis_asrs.tb_ac_storage_area ar " +
                        "    on l.area_id = ar.id " +
                        " where a.domain_id = :domainId " +
                        "   and su.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and a.stock_unit_id = :stockUnitId " +
                        " order by a.allocated_at desc, a.created_at desc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,stockUnitId",
                Domain.currentDomainId(), stockUnit.getId()
        );

        return this.queryManager.selectListBySql(sql, param, StockAllocationView.class, 0, 0);
    }

    /**
     * 재고단위번호 기준 활성 할당 조회.
     *
     * @param stockUnitNo 재고 단위 번호
     * @param stockQueryService 재고 조회 서비스
     * @return 활성 할당 목록
     */
    public List<StockAllocationView> findActiveAllocationViewsByStockUnitNo(String stockUnitNo,
                                                                            StockQueryService stockQueryService) {
        if (ValueUtil.isEmpty(stockUnitNo)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "stockUnitNo is empty."
            );
        }

        TbAcStockUnit stockUnit = stockQueryService.findAnyByStockUnitNo(stockUnitNo);

        String sql =
                "select " +
                        "    a.id as allocation_id, " +
                        "    a.stock_unit_id, " +
                        "    su.stock_unit_no, " +
                        "    a.item_id, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    su.current_location_id, " +
                        "    ar.area_code, " +
                        "    l.location_code, " +
                        "    a.allocated_qty, " +
                        "    a.alloc_status_code, " +
                        "    a.ref_doc_type, " +
                        "    a.ref_doc_no, " +
                        "    a.ref_line_no, " +
                        "    a.due_date, " +
                        "    a.allocated_at " +
                        "  from logis_asrs.tb_ac_stock_allocation a " +
                        "  join logis_asrs.tb_ac_stock_unit su " +
                        "    on a.stock_unit_id = su.id " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on a.item_id = i.id " +
                        "  left join logis_asrs.tb_ac_location l " +
                        "    on su.current_location_id = l.id " +
                        "  left join logis_asrs.tb_ac_storage_area ar " +
                        "    on l.area_id = ar.id " +
                        " where a.domain_id = :domainId " +
                        "   and su.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and a.stock_unit_id = :stockUnitId " +
                        "   and a.alloc_status_code = 'ALLOCATED' " +
                        " order by a.allocated_at desc, a.created_at desc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,stockUnitId",
                Domain.currentDomainId(), stockUnit.getId()
        );

        return this.queryManager.selectListBySql(sql, param, StockAllocationView.class, 0, 0);
    }

    /**
     * 참조문서번호 기준 전체 할당 이력 조회.
     *
     * @param refDocNo 참조 문서 번호
     * @param refDocType 참조 문서 유형
     * @param refLineNo 참조 문서 라인 번호
     * @return 할당 목록
     */
    public List<StockAllocationView> findAllocationsByRefDocNo(String refDocNo,
                                                               String refDocType,
                                                               String refLineNo) {
        if (ValueUtil.isEmpty(refDocNo)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "refDocNo is empty."
            );
        }

        StringBuilder sql = new StringBuilder();
        sql.append("select ")
                .append("    a.id as allocation_id, ")
                .append("    a.stock_unit_id, ")
                .append("    su.stock_unit_no, ")
                .append("    a.item_id, ")
                .append("    i.item_code, ")
                .append("    i.item_name, ")
                .append("    su.current_location_id, ")
                .append("    ar.area_code, ")
                .append("    l.location_code, ")
                .append("    a.allocated_qty, ")
                .append("    a.alloc_status_code, ")
                .append("    a.ref_doc_type, ")
                .append("    a.ref_doc_no, ")
                .append("    a.ref_line_no, ")
                .append("    a.due_date, ")
                .append("    a.allocated_at ")
                .append("  from logis_asrs.tb_ac_stock_allocation a ")
                .append("  join logis_asrs.tb_ac_stock_unit su ")
                .append("    on a.stock_unit_id = su.id ")
                .append("  join logis_asrs.tb_ac_item_master i ")
                .append("    on a.item_id = i.id ")
                .append("  left join logis_asrs.tb_ac_location l ")
                .append("    on su.current_location_id = l.id ")
                .append("  left join logis_asrs.tb_ac_storage_area ar ")
                .append("    on l.area_id = ar.id ")
                .append(" where a.domain_id = :domainId ")
                .append("   and su.domain_id = :domainId ")
                .append("   and i.domain_id = :domainId ")
                .append("   and a.ref_doc_no = :refDocNo ");

        if (ValueUtil.isNotEmpty(refDocType)) {
            sql.append("   and a.ref_doc_type = :refDocType ");
        }
        if (ValueUtil.isNotEmpty(refLineNo)) {
            sql.append("   and a.ref_line_no = :refLineNo ");
        }

        sql.append(" order by a.allocated_at desc, a.created_at desc ");

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,refDocNo,refDocType,refLineNo",
                Domain.currentDomainId(), refDocNo, refDocType, refLineNo
        );

        return this.queryManager.selectListBySql(sql.toString(), param, StockAllocationView.class, 0, 0);
    }

    /**
     * 참조문서번호 기준 활성 할당 조회.
     *
     * @param refDocNo 참조 문서 번호
     * @param refDocType 참조 문서 유형
     * @param refLineNo 참조 문서 라인 번호
     * @return 활성 할당 목록
     */
    public List<StockAllocationView> findActiveAllocationsByRefDocNo(String refDocNo,
                                                                     String refDocType,
                                                                     String refLineNo) {
        if (ValueUtil.isEmpty(refDocNo)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "refDocNo is empty."
            );
        }

        StringBuilder sql = new StringBuilder();
        sql.append("select ")
                .append("    a.id as allocation_id, ")
                .append("    a.stock_unit_id, ")
                .append("    su.stock_unit_no, ")
                .append("    a.item_id, ")
                .append("    i.item_code, ")
                .append("    i.item_name, ")
                .append("    su.current_location_id, ")
                .append("    ar.area_code, ")
                .append("    l.location_code, ")
                .append("    a.allocated_qty, ")
                .append("    a.alloc_status_code, ")
                .append("    a.ref_doc_type, ")
                .append("    a.ref_doc_no, ")
                .append("    a.ref_line_no, ")
                .append("    a.due_date, ")
                .append("    a.allocated_at ")
                .append("  from logis_asrs.tb_ac_stock_allocation a ")
                .append("  join logis_asrs.tb_ac_stock_unit su ")
                .append("    on a.stock_unit_id = su.id ")
                .append("  join logis_asrs.tb_ac_item_master i ")
                .append("    on a.item_id = i.id ")
                .append("  left join logis_asrs.tb_ac_location l ")
                .append("    on su.current_location_id = l.id ")
                .append("  left join logis_asrs.tb_ac_storage_area ar ")
                .append("    on l.area_id = ar.id ")
                .append(" where a.domain_id = :domainId ")
                .append("   and su.domain_id = :domainId ")
                .append("   and i.domain_id = :domainId ")
                .append("   and a.ref_doc_no = :refDocNo ")
                .append("   and a.alloc_status_code = 'ALLOCATED' ");

        if (ValueUtil.isNotEmpty(refDocType)) {
            sql.append("   and a.ref_doc_type = :refDocType ");
        }
        if (ValueUtil.isNotEmpty(refLineNo)) {
            sql.append("   and a.ref_line_no = :refLineNo ");
        }

        sql.append(" order by a.allocated_at desc, a.created_at desc ");

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,refDocNo,refDocType,refLineNo",
                Domain.currentDomainId(), refDocNo, refDocType, refLineNo
        );

        return this.queryManager.selectListBySql(sql.toString(), param, StockAllocationView.class, 0, 0);
    }
}