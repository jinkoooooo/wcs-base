package operato.logis.asrs.query.grade;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.entity.TbAcGradePolicy;
import operato.logis.asrs.entity.TbAcItemGrade;
import operato.logis.asrs.entity.TbAcItemMaster;
import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.query.grade.model.ItemGradeHistoryView;
import operato.logis.asrs.query.grade.model.ItemGradeView;
import operato.logis.asrs.query.item.ItemQueryService;
import operato.logis.asrs.query.location.LocationQueryService;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 상품 등급 조회 전용 서비스.
 *
 * <p>
 * 내부:
 * - 적용 정책 조회
 * - 현재 등급 row 조회
 * </p>
 *
 * <p>
 * 외부:
 * - 현재 등급 조회
 * - 등급 변경 이력 조회
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ItemGradeQueryService extends AbstractQueryService {

    private final LocationQueryService locationQueryService;
    private final ItemQueryService itemQueryService;

    /* =========================================================
     * 내부 코어용 조회
     * ========================================================= */

    /**
     * 기준일자에 적용 가능한 정책 조회.
     *
     * <p>
     * policyCode 가 있으면 해당 정책을 사용하고,
     * 없으면 active 정책 중 effective_from 이 가장 최신인 정책을 사용한다.
     * </p>
     *
     * @param policyCode 정책 코드
     * @param targetDate 기준일자
     * @return 적용 정책
     */
    public TbAcGradePolicy findApplicablePolicy(String policyCode, LocalDate targetDate) {
        if (targetDate == null) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "targetDate is null.");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("select * ")
                .append("  from logis_asrs.tb_ac_grade_policy p ")
                .append(" where p.domain_id = :domainId ")
                .append("   and p.active_yn = 'Y' ")
                .append("   and (p.effective_from is null or p.effective_from <= :targetDate) ")
                .append("   and (p.effective_to is null or p.effective_to >= :targetDate) ");

        if (ValueUtil.isNotEmpty(policyCode)) {
            sql.append("   and p.policy_code = :policyCode ");
        }

        sql.append(" order by p.effective_from desc nulls last, p.created_at desc ");

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,targetDate,policyCode",
                Domain.currentDomainId(), Date.valueOf(targetDate), policyCode
        );

        List<TbAcGradePolicy> list = this.queryManager.selectListBySql(sql.toString(), param, TbAcGradePolicy.class, 0, 0);
        if (list == null || list.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Applicable grade policy not found. policyCode=" + policyCode + ", targetDate=" + targetDate
            );
        }

        return list.get(0);
    }

    /**
     * 영역 row id + 품목 row id 기준 현재 등급 row 조회.
     *
     * @param areaId 영역 row id
     * @param itemId 품목 row id
     * @return 현재 등급 row 또는 null
     */
    public TbAcItemGrade findCurrentItemGradeOrNull(String areaId, String itemId) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("area_id", areaId);
        condition.addFilter("item_id", itemId);

        return this.queryManager.select(TbAcItemGrade.class, condition);
    }

    /* =========================================================
     * 외부 조회
     * ========================================================= */

    /**
     * 영역 기준 현재 상품 등급 목록 조회.
     *
     * @param areaCode 영역 코드
     * @return 현재 등급 목록
     */
    public List<ItemGradeView> findCurrentGrades(String areaCode) {
        if (ValueUtil.isEmpty(areaCode)) {
            throw new AisleCoreException(AisleCoreErrorCode.INVALID_REQUEST, "areaCode is empty.");
        }

        TbAcStorageArea area = locationQueryService.findAreaByCode(areaCode);

        String sql =
                "select " +
                        "    g.id as item_grade_id, " +
                        "    g.area_id, " +
                        "    a.area_code, " +
                        "    g.item_id, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    g.grade_policy_id, " +
                        "    p.policy_code, " +
                        "    p.policy_name, " +
                        "    g.manual_seed_grade, " +
                        "    g.manual_seed_score, " +
                        "    g.learned_score, " +
                        "    g.final_score, " +
                        "    g.current_grade, " +
                        "    g.last_calculated_at " +
                        "  from logis_asrs.tb_ac_item_grade g " +
                        "  join logis_asrs.tb_ac_storage_area a " +
                        "    on g.area_id = a.id " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on g.item_id = i.id " +
                        "  left join logis_asrs.tb_ac_grade_policy p " +
                        "    on g.grade_policy_id = p.id " +
                        " where g.domain_id = :domainId " +
                        "   and a.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and g.area_id = :areaId " +
                        " order by g.final_score desc nulls last, i.item_code asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId",
                Domain.currentDomainId(), area.getId()
        );

        return this.queryManager.selectListBySql(sql, param, ItemGradeView.class, 0, 0);
    }

    /**
     * 영역 + 품목 기준 현재 상품 등급 단건 조회.
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @return 현재 등급 단건
     */
    public ItemGradeView findCurrentGrade(String areaCode, String itemCode) {
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
                        "    g.id as item_grade_id, " +
                        "    g.area_id, " +
                        "    a.area_code, " +
                        "    g.item_id, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    g.grade_policy_id, " +
                        "    p.policy_code, " +
                        "    p.policy_name, " +
                        "    g.manual_seed_grade, " +
                        "    g.manual_seed_score, " +
                        "    g.learned_score, " +
                        "    g.final_score, " +
                        "    g.current_grade, " +
                        "    g.last_calculated_at " +
                        "  from logis_asrs.tb_ac_item_grade g " +
                        "  join logis_asrs.tb_ac_storage_area a " +
                        "    on g.area_id = a.id " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on g.item_id = i.id " +
                        "  left join logis_asrs.tb_ac_grade_policy p " +
                        "    on g.grade_policy_id = p.id " +
                        " where g.domain_id = :domainId " +
                        "   and a.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and g.area_id = :areaId " +
                        "   and g.item_id = :itemId ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,itemId",
                Domain.currentDomainId(), area.getId(), item.getId()
        );

        List<ItemGradeView> list = this.queryManager.selectListBySql(sql, param, ItemGradeView.class, 0, 0);
        if (list == null || list.isEmpty()) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Item grade not found. areaCode=" + areaCode + ", itemCode=" + itemCode
            );
        }

        return list.get(0);
    }

    /**
     * 영역 + 품목 기준 등급 변경 이력 조회.
     *
     * @param areaCode 영역 코드
     * @param itemCode 품목 코드
     * @return 등급 이력 목록
     */
    public List<ItemGradeHistoryView> findGradeHistories(String areaCode, String itemCode) {
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
                        "    h.id as item_grade_hist_id, " +
                        "    h.item_grade_id, " +
                        "    a.area_code, " +
                        "    i.item_code, " +
                        "    i.item_name, " +
                        "    h.grade_policy_id, " +
                        "    p.policy_code, " +
                        "    p.policy_name, " +
                        "    h.previous_grade, " +
                        "    h.new_grade, " +
                        "    h.previous_score, " +
                        "    h.new_score, " +
                        "    h.reason_json, " +
                        "    h.calculated_at " +
                        "  from logis_asrs.tb_ac_item_grade_hist h " +
                        "  join logis_asrs.tb_ac_item_grade g " +
                        "    on h.item_grade_id = g.id " +
                        "  join logis_asrs.tb_ac_storage_area a " +
                        "    on g.area_id = a.id " +
                        "  join logis_asrs.tb_ac_item_master i " +
                        "    on g.item_id = i.id " +
                        "  left join logis_asrs.tb_ac_grade_policy p " +
                        "    on h.grade_policy_id = p.id " +
                        " where h.domain_id = :domainId " +
                        "   and g.domain_id = :domainId " +
                        "   and a.domain_id = :domainId " +
                        "   and i.domain_id = :domainId " +
                        "   and g.area_id = :areaId " +
                        "   and g.item_id = :itemId " +
                        " order by h.calculated_at desc, h.created_at desc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,areaId,itemId",
                Domain.currentDomainId(), area.getId(), item.getId()
        );

        return this.queryManager.selectListBySql(sql, param, ItemGradeHistoryView.class, 0, 0);
    }
}