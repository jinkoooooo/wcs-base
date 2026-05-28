package operato.logis.asrs.query.strategy;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.entity.TbAcItemMaster;
import operato.logis.asrs.entity.TbAcStrategyRule;
import operato.logis.asrs.entity.TbAcStrategySet;
import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.query.item.ItemQueryService;
import operato.logis.asrs.query.location.LocationQueryService;
import operato.logis.asrs.query.strategy.model.InboundLocationCandidateRow;
import operato.logis.asrs.query.strategy.model.OutboundStockCandidateRow;
import operato.logis.asrs.query.strategy.model.RelocationStockCandidateRow;
import operato.logis.asrs.query.strategy.model.RelocationSwapCandidateRow;
import operato.logis.asrs.query.strategy.model.RelocationTargetLocationRow;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 전략/재배치 관련 조회 서비스.
 *
 * <p>
 * 역할
 * 1. 전략세트/룰 조회
 * 2. 리로케이션 source 후보 조회
 * 3. 빈 target location 조회
 * 4. swap 가능 target location 조회
 * 5. 입고/출고 추천 후보 조회
 * </p>
 */
@Service
@RequiredArgsConstructor
public class StrategyQueryService extends AbstractQueryService {

    private final LocationQueryService locationQueryService;
    private final ItemQueryService itemQueryService;

    /**
     * areaCode + strategyCode 기준 활성 전략세트 조회
     *
     * @param areaCode 영역 코드
     * @param strategyCode 전략 코드
     * @return 전략세트
     */
    public TbAcStrategySet findStrategySet(String areaCode, String strategyCode) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(strategyCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "strategyCode is empty.");
        }

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("area_id", area.getId());
        condition.addFilter("strategy_code", strategyCode);
        condition.addFilter("active_yn", "Y");

        TbAcStrategySet strategySet = this.queryManager.select(TbAcStrategySet.class, condition);
        if (strategySet == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "StrategySet not found. areaCode=" + areaCode + ", strategyCode=" + strategyCode
            );
        }

        return strategySet;
    }

    /**
     * 전략세트 기준 활성/사용 룰 조회
     *
     * @param strategySetId 전략세트 ID
     * @return 룰 목록(priority_no ASC)
     */
    public List<TbAcStrategyRule> findEnabledRules(String strategySetId) {
        if (ValueUtil.isEmpty(strategySetId)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "strategySetId is empty.");
        }

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("strategy_set_id", strategySetId);
        condition.addFilter("enabled_yn", "Y");
        condition.addFilter("active_yn", "Y");
        condition.addOrder("priority_no", true);

        return this.queryManager.selectList(TbAcStrategyRule.class, condition);
    }

    /**
     * 재배치 source 후보 조회.
     *
     * <p>
     * 현재 재고 + 상품등급 + 현재 로케이션등급 + 명일수요를 한 번에 조회한다.
     * strategy core 에서 condition_json 기반으로 필터링할 수 있도록
     * 가능한 많은 판단 재료를 포함한다.
     * </p>
     *
     * @param areaCode 영역 코드
     * @return source 후보 목록
     */
    public List<RelocationStockCandidateRow> findRelocationSourceCandidates(String areaCode) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);

        String sql =
                "select " +
                        "    su.id as stock_unit_id, " +
                        "    su.stock_unit_no, " +
                        "    su.item_id, " +
                        "    im.item_code, " +
                        "    im.item_name, " +
                        "    im.item_category_id, " +
                        "    su.qty, " +
                        "    su.reserved_qty, " +
                        "    su.stock_status_code, " +
                        "    su.active_yn, " +
                        "    su.hold_yn, " +
                        "    l.id as current_location_id, " +
                        "    l.location_code as current_location_code, " +
                        "    l.location_grade as current_location_grade, " +
                        "    l.sort_seq as current_sort_seq, " +
                        "    l.front_priority_yn as current_front_priority_yn, " +
                        "    ig.current_grade as item_grade, " +
                        "    coalesce(dp_sum.demand_qty, 0) as demand_tomorrow_qty, " +
                        "    case ig.current_grade " +
                        "         when 'A' then 1 " +
                        "         when 'B' then 2 " +
                        "         when 'C' then 3 " +
                        "         when 'D' then 4 " +
                        "         else 999 end as item_grade_rank, " +
                        "    case l.location_grade " +
                        "         when 'A' then 1 " +
                        "         when 'B' then 2 " +
                        "         when 'C' then 3 " +
                        "         when 'D' then 4 " +
                        "         else 999 end as location_grade_rank " +
                        "  from logis_asrs.tb_ac_stock_unit su " +
                        "  join logis_asrs.tb_ac_item_master im " +
                        "    on su.item_id = im.id " +
                        "  join logis_asrs.tb_ac_location l " +
                        "    on su.current_location_id = l.id " +
                        "  join logis_asrs.tb_ac_item_grade ig " +
                        "    on ig.area_id = l.area_id " +
                        "   and ig.item_id = su.item_id " +
                        "  left join ( " +
                        "        select area_id, item_id, sum(demand_qty) as demand_qty " +
                        "          from logis_asrs.tb_ac_demand_plan " +
                        "         where active_yn = 'Y' " +
                        "         group by area_id, item_id " +
                        "   ) dp_sum " +
                        "    on dp_sum.area_id = l.area_id " +
                        "   and dp_sum.item_id = su.item_id " +
                        " where su.domain_id = :domainId " +
                        "   and im.domain_id = :domainId " +
                        "   and l.domain_id = :domainId " +
                        "   and ig.domain_id = :domainId " +
                        "   and l.area_id = :areaId " +
                        "   and su.active_yn = 'Y' " +
                        "   and su.hold_yn = 'N' " +
                        "   and su.stock_status_code in ('AVAILABLE', 'RESERVED') " +
                        " order by " +
                        "    case ig.current_grade " +
                        "         when 'A' then 1 " +
                        "         when 'B' then 2 " +
                        "         when 'C' then 3 " +
                        "         when 'D' then 4 " +
                        "         else 999 end asc, " +
                        "    case l.location_grade " +
                        "         when 'A' then 1 " +
                        "         when 'B' then 2 " +
                        "         when 'C' then 3 " +
                        "         when 'D' then 4 " +
                        "         else 999 end asc, " +
                        "    l.sort_seq asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId",
                Domain.currentDomainId(),
                area.getId()
        );

        return this.queryManager.selectListBySql(sql, param, RelocationStockCandidateRow.class, 0, 0);
    }

    /**
     * 특정 등급의 빈 target location 조회
     *
     * @param areaCode 영역 코드
     * @param locationGrade 목표 로케이션 등급
     * @return 빈 로케이션 목록
     */
    public List<RelocationTargetLocationRow> findEmptyTargetLocations(String areaCode, String locationGrade) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(locationGrade)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "locationGrade is empty.");
        }

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);

        String sql =
                "select " +
                        "    l.id as location_id, " +
                        "    l.location_code, " +
                        "    l.location_grade, " +
                        "    l.sort_seq, " +
                        "    l.front_priority_yn " +
                        "  from logis_asrs.tb_ac_location l " +
                        " where l.domain_id = :domainId " +
                        "   and l.area_id = :areaId " +
                        "   and l.active_yn = 'Y' " +
                        "   and l.location_grade = :locationGrade " +
                        "   and not exists ( " +
                        "       select 1 " +
                        "         from logis_asrs.tb_ac_stock_unit su " +
                        "        where su.current_location_id = l.id " +
                        "          and su.active_yn = 'Y' " +
                        "          and su.stock_status_code in ('AVAILABLE', 'RESERVED', 'HOLD') " +
                        "   ) " +
                        " order by l.sort_seq asc, l.location_code asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,locationGrade",
                Domain.currentDomainId(),
                area.getId(),
                locationGrade
        );

        return this.queryManager.selectListBySql(sql, param, RelocationTargetLocationRow.class, 0, 0);
    }

    /**
     * 특정 등급의 swap 가능 target location 조회
     *
     * <p>
     * 이미 재고가 점유 중인 location 중,
     * 현재 후보보다 더 낮은 우선순위 stock이 들어있다면 swap 후보로 사용할 수 있다.
     * </p>
     *
     * @param areaCode 영역 코드
     * @param locationGrade 목표 로케이션 등급
     * @return 점유 중 swap 후보 목록
     */
    public List<RelocationSwapCandidateRow> findSwapTargetLocations(String areaCode, String locationGrade) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(locationGrade)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "locationGrade is empty.");
        }

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);

        String sql =
                "select " +
                        "    l.id as location_id, " +
                        "    l.location_code, " +
                        "    l.location_grade, " +
                        "    l.sort_seq, " +
                        "    l.front_priority_yn, " +
                        "    su.id as stock_unit_id, " +
                        "    su.stock_unit_no, " +
                        "    su.item_id, " +
                        "    im.item_code, " +
                        "    im.item_name, " +
                        "    ig.current_grade as item_grade, " +
                        "    su.qty, " +
                        "    su.reserved_qty " +
                        "  from logis_asrs.tb_ac_location l " +
                        "  join logis_asrs.tb_ac_stock_unit su " +
                        "    on su.current_location_id = l.id " +
                        "  join logis_asrs.tb_ac_item_master im " +
                        "    on su.item_id = im.id " +
                        "  join logis_asrs.tb_ac_item_grade ig " +
                        "    on ig.area_id = l.area_id " +
                        "   and ig.item_id = su.item_id " +
                        " where l.domain_id = :domainId " +
                        "   and su.domain_id = :domainId " +
                        "   and im.domain_id = :domainId " +
                        "   and ig.domain_id = :domainId " +
                        "   and l.area_id = :areaId " +
                        "   and l.active_yn = 'Y' " +
                        "   and l.location_grade = :locationGrade " +
                        "   and su.active_yn = 'Y' " +
                        "   and su.hold_yn = 'N' " +
                        "   and su.stock_status_code in ('AVAILABLE', 'RESERVED') " +
                        " order by l.sort_seq asc, l.location_code asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,locationGrade",
                Domain.currentDomainId(),
                area.getId(),
                locationGrade
        );

        return this.queryManager.selectListBySql(sql, param, RelocationSwapCandidateRow.class, 0, 0);
    }

    /**
     * 입고 추천 location 조회
     *
     * <p>
     * 상품등급과 동일한 location_grade를 우선으로 비어 있는 로케이션을 추천한다.
     * </p>
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @return 입고 추천 위치 목록
     */
    public List<InboundLocationCandidateRow> findInboundLocations(String areaCode, String itemCode) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(itemCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "itemCode is empty.");
        }

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);
        TbAcItemMaster item = itemQueryService.findItemByCode(itemCode);

        String sql =
                "select " +
                        "    l.id as location_id, " +
                        "    l.location_code, " +
                        "    l.location_grade, " +
                        "    l.sort_seq, " +
                        "    l.front_priority_yn " +
                        "  from logis_asrs.tb_ac_location l " +
                        "  join logis_asrs.tb_ac_item_grade ig " +
                        "    on ig.area_id = l.area_id " +
                        "   and ig.item_id = :itemId " +
                        " where l.domain_id = :domainId " +
                        "   and ig.domain_id = :domainId " +
                        "   and l.area_id = :areaId " +
                        "   and l.active_yn = 'Y' " +
                        "   and l.inbound_allowed_yn = 'Y' " +
                        "   and l.location_grade = ig.current_grade " +
                        "   and not exists ( " +
                        "       select 1 " +
                        "         from logis_asrs.tb_ac_stock_unit su " +
                        "        where su.current_location_id = l.id " +
                        "          and su.active_yn = 'Y' " +
                        "          and su.stock_status_code in ('AVAILABLE', 'RESERVED', 'HOLD') " +
                        "   ) " +
                        " order by l.sort_seq asc, l.location_code asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,itemId",
                Domain.currentDomainId(),
                area.getId(),
                item.getId()
        );

        return this.queryManager.selectListBySql(sql, param, InboundLocationCandidateRow.class, 0, 0);
    }

    /**
     * 출고 추천 stock 조회
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @return 출고 후보 재고 목록
     */
    public List<OutboundStockCandidateRow> findOutboundStocks(String areaCode, String itemCode) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }
        if (ValueUtil.isEmpty(itemCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "itemCode is empty.");
        }

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);
        TbAcItemMaster item = itemQueryService.findItemByCode(itemCode);

        String sql =
                "select " +
                        "    su.id as stock_unit_id, " +
                        "    su.stock_unit_no, " +
                        "    su.item_id, " +
                        "    im.item_code, " +
                        "    im.item_name, " +
                        "    su.qty, " +
                        "    su.reserved_qty, " +
                        "    (su.qty - su.reserved_qty) as available_qty, " +
                        "    l.id as location_id, " +
                        "    l.location_code, " +
                        "    l.location_grade, " +
                        "    l.sort_seq, " +
                        "    l.front_priority_yn, " +
                        "    su.lot_id, " +
                        "    lot.lot_no " +
                        "  from logis_asrs.tb_ac_stock_unit su " +
                        "  join logis_asrs.tb_ac_item_master im " +
                        "    on su.item_id = im.id " +
                        "  join logis_asrs.tb_ac_location l " +
                        "    on su.current_location_id = l.id " +
                        "  left join logis_asrs.tb_ac_lot lot " +
                        "    on su.lot_id = lot.id " +
                        " where su.domain_id = :domainId " +
                        "   and im.domain_id = :domainId " +
                        "   and l.domain_id = :domainId " +
                        "   and su.item_id = :itemId " +
                        "   and l.area_id = :areaId " +
                        "   and su.active_yn = 'Y' " +
                        "   and su.hold_yn = 'N' " +
                        "   and su.stock_status_code in ('AVAILABLE', 'RESERVED') " +
                        "   and (su.qty - su.reserved_qty) > 0 " +
                        " order by " +
                        "    case l.location_grade " +
                        "         when 'A' then 1 " +
                        "         when 'B' then 2 " +
                        "         when 'C' then 3 " +
                        "         when 'D' then 4 " +
                        "         else 999 end asc, " +
                        "    l.sort_seq asc, " +
                        "    l.front_priority_yn desc, " +
                        "    su.inbound_at asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,itemId",
                Domain.currentDomainId(),
                area.getId(),
                item.getId()
        );

        return this.queryManager.selectListBySql(sql, param, OutboundStockCandidateRow.class, 0, 0);
    }
}