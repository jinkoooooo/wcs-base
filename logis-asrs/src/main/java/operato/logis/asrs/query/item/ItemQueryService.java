package operato.logis.asrs.query.item;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.entity.TbAcItemCategory;
import operato.logis.asrs.entity.TbAcItemMaster;
import operato.logis.asrs.entity.TbAcLot;
import operato.logis.asrs.entity.TbAcOperationProfile;
import operato.logis.asrs.query.item.model.ResolvedAttrValue;
import xyz.elidom.base.util.OrmUtil;
import xyz.elidom.dbist.dml.Query;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 상품/상품군/운영프로파일/LOT 조회 전용 서비스.
 *
 * <p>
 * 신규 업무 로직은 code 기반 조회를 사용한다.
 * </p>
 *
 * <ul>
 *   <li>품목 조회: item_code</li>
 *   <li>상품군 조회: category_code</li>
 *   <li>운영프로파일 조회: profile_code</li>
 *   <li>LOT 조회: item_code + lot_no</li>
 * </ul>
 *
 * <p>
 * id 기반 메서드는 하위호환 목적으로만 유지한다.
 * </p>
 */
@Service
public class ItemQueryService extends AbstractQueryService {

    /* =========================================================
     * 신규 표준: code 기반 조회
     * ========================================================= */

    /**
     * 품목코드로 활성 품목을 조회한다.
     *
     * @param itemCode 품목코드
     * @return TbAcItemMaster
     */
    public TbAcItemMaster findItemByCode(String itemCode) {
        if (ValueUtil.isEmpty(itemCode)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "itemCode is empty."
            );
        }

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("item_code", itemCode);
        condition.addFilter("active_yn", "Y");

        TbAcItemMaster item = this.queryManager.select(TbAcItemMaster.class, condition);
        if (item == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Item not found. itemCode=" + itemCode
            );
        }
        return item;
    }

    /**
     * 상품군 코드로 활성 상품군을 조회한다.
     *
     * @param categoryCode 상품군 코드
     * @return TbAcItemCategory
     */
    public TbAcItemCategory findCategoryByCode(String categoryCode) {
        if (ValueUtil.isEmpty(categoryCode)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "categoryCode is empty."
            );
        }

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("category_code", categoryCode);
        condition.addFilter("active_yn", "Y");

        TbAcItemCategory category = this.queryManager.select(TbAcItemCategory.class, condition);
        if (category == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Item category not found. categoryCode=" + categoryCode
            );
        }
        return category;
    }

    /**
     * 운영 프로파일 코드로 활성 운영 프로파일을 조회한다.
     *
     * @param profileCode 운영 프로파일 코드
     * @return TbAcOperationProfile
     */
    public TbAcOperationProfile findOperationProfileByCode(String profileCode) {
        if (ValueUtil.isEmpty(profileCode)) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.INVALID_REQUEST,
                    "profileCode is empty."
            );
        }

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("profile_code", profileCode);
        condition.addFilter("active_yn", "Y");

        TbAcOperationProfile profile = this.queryManager.select(TbAcOperationProfile.class, condition);
        if (profile == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Operation profile not found. profileCode=" + profileCode
            );
        }
        return profile;
    }

    /**
     * 품목코드 + LOT번호로 활성 LOT를 조회한다.
     *
     * @param itemCode 품목코드
     * @param lotNo LOT번호
     * @return TbAcLot 또는 null
     */
    public TbAcLot findLotByItemCodeAndLotNoOrNull(String itemCode, String lotNo) {
        if (ValueUtil.isEmpty(itemCode) || ValueUtil.isEmpty(lotNo)) {
            return null;
        }

        TbAcItemMaster item = findItemByCode(itemCode);

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("item_id", item.getId());
        condition.addFilter("lot_no", lotNo);
        condition.addFilter("active_yn", "Y");

        return this.queryManager.select(TbAcLot.class, condition);
    }

    /**
     * 품목코드 + LOT번호로 활성 LOT를 조회한다. 없으면 예외.
     *
     * @param itemCode 품목코드
     * @param lotNo LOT번호
     * @return TbAcLot
     */
    public TbAcLot findLotByItemCodeAndLotNo(String itemCode, String lotNo) {
        TbAcLot lot = findLotByItemCodeAndLotNoOrNull(itemCode, lotNo);
        if (lot == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Lot not found. itemCode=" + itemCode + ", lotNo=" + lotNo
            );
        }
        return lot;
    }

    /**
     * 운영 프로파일에 연결된 속성 목록을 조인 조회한다.
     *
     * @param profileId 운영 프로파일 ID
     * @return 속성 목록
     */
    public List<ResolvedAttrValue> findProfileAttrs(String profileId) {
        if (ValueUtil.isEmpty(profileId)) {
            return Collections.emptyList();
        }

        String sql =
                "select " +
                        "    pv.attr_def_id, " +
                        "    d.attr_code, " +
                        "    d.attr_name, " +
                        "    pv.attr_value " +
                        "  from logis_asrs.tb_ac_profile_attr_value pv " +
                        "  join logis_asrs.tb_ac_profile_attr_def d " +
                        "    on pv.attr_def_id = d.id " +
                        " where pv.domain_id = :domainId " +
                        "   and d.domain_id = :domainId " +
                        "   and pv.operation_profile_id = :profileId " +
                        "   and pv.active_yn = 'Y' " +
                        "   and d.active_yn = 'Y' " +
                        " order by d.attr_group asc, d.attr_code asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,profileId",
                Domain.currentDomainId(), profileId
        );

        return this.queryManager.selectListBySql(sql, param, ResolvedAttrValue.class, 0, 0);
    }

    /**
     * 품목별 override 속성 목록을 조인 조회한다.
     *
     * @param itemId 품목 ID
     * @return override 속성 목록
     */
    public List<ResolvedAttrValue> findItemOverrides(String itemId) {
        if (ValueUtil.isEmpty(itemId)) {
            return Collections.emptyList();
        }

        String sql =
                "select " +
                        "    o.attr_def_id, " +
                        "    d.attr_code, " +
                        "    d.attr_name, " +
                        "    o.attr_value " +
                        "  from logis_asrs.tb_ac_item_policy_override o " +
                        "  join logis_asrs.tb_ac_profile_attr_def d " +
                        "    on o.attr_def_id = d.id " +
                        " where o.domain_id = :domainId " +
                        "   and d.domain_id = :domainId " +
                        "   and o.item_id = :itemId " +
                        "   and o.active_yn = 'Y' " +
                        "   and d.active_yn = 'Y' " +
                        " order by d.attr_group asc, d.attr_code asc ";

        Map<String, Object> param = ValueUtil.newMap(
                "domainId,itemId",
                Domain.currentDomainId(), itemId
        );

        return this.queryManager.selectListBySql(sql, param, ResolvedAttrValue.class, 0, 0);
    }

    /* =========================================================
     * 하위호환: id 기반 조회 (신규 사용 금지)
     * ========================================================= */

    /**
     * @deprecated 신규 업무 로직에서는 사용하지 말고 findItemByCode 를 사용할 것.
     */
    @Deprecated
    public TbAcItemMaster findItem(String itemId) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("id", itemId);
        condition.addFilter("active_yn", "Y");

        TbAcItemMaster item = this.queryManager.select(TbAcItemMaster.class, condition);
        if (item == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Item not found. itemId=" + itemId
            );
        }
        return item;
    }

    /**
     * @deprecated 신규 업무 로직에서는 사용하지 말고 findCategoryByCode 를 사용할 것.
     */
    @Deprecated
    public TbAcItemCategory findCategory(String categoryId) {
        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("id", categoryId);
        condition.addFilter("active_yn", "Y");

        TbAcItemCategory category = this.queryManager.select(TbAcItemCategory.class, condition);
        if (category == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Item category not found. categoryId=" + categoryId
            );
        }
        return category;
    }

    /**
     * @deprecated 신규 업무 로직에서는 사용하지 말고 findOperationProfileByCode 를 사용할 것.
     */
    @Deprecated
    public TbAcOperationProfile findOperationProfile(String profileId) {
        TbAcOperationProfile profile = findOperationProfileOrNull(profileId);
        if (profile == null) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ENTITY_NOT_FOUND,
                    "Operation profile not found. profileId=" + profileId
            );
        }
        return profile;
    }

    /**
     * @deprecated 신규 업무 로직에서는 사용하지 말고 findOperationProfileByCode 를 사용할 것.
     */
    @Deprecated
    public TbAcOperationProfile findOperationProfileOrNull(String profileId) {
        if (ValueUtil.isEmpty(profileId)) {
            return null;
        }

        Query condition = OrmUtil.newConditionForExecution(Domain.currentDomainId());
        condition.addFilter("id", profileId);
        condition.addFilter("active_yn", "Y");

        return this.queryManager.select(TbAcOperationProfile.class, condition);
    }
}