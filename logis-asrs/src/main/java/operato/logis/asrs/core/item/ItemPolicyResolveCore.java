package operato.logis.asrs.core.item;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import operato.logis.asrs.core.common.AisleCoreErrorCode;
import operato.logis.asrs.core.common.AisleCoreException;
import operato.logis.asrs.core.item.model.ResolvedItemPolicy;
import operato.logis.asrs.entity.TbAcItemCategory;
import operato.logis.asrs.entity.TbAcItemMaster;
import operato.logis.asrs.entity.TbAcStorageArea;
import operato.logis.asrs.enums.AcAllocationRule;
import operato.logis.asrs.enums.AcYn;
import operato.logis.asrs.query.item.ItemQueryService;
import operato.logis.asrs.query.item.model.ResolvedAttrValue;
import operato.logis.asrs.query.location.LocationQueryService;
import xyz.elidom.util.ValueUtil;

/**
 * 상품 최종 운영정책 해석 코어.
 *
 * <p>
 * 아래 우선순위로 정책을 병합하여 최종 운영속성을 도출한다.
 * </p>
 *
 * <ol>
 *   <li>영역 기본 운영 프로파일</li>
 *   <li>상품군 기본 운영 프로파일</li>
 *   <li>품목 개별 운영 프로파일</li>
 *   <li>품목 개별 override 속성</li>
 *   <li>최종적으로 item master 핵심 컬럼을 우선 적용</li>
 * </ol>
 *
 * <p>
 * 즉, 프로파일 기반 표준화는 기본 정책을 제공하고,
 * 품목 마스터의 핵심 컬럼은 실제 운영값으로 최종 확정한다.
 * </p>
 */
@Component
public class ItemPolicyResolveCore {

    /** 프로파일 속성 코드: 할당 규칙 */
    private static final String ATTR_ALLOCATION_RULE_CODE = "ALLOCATION_RULE_CODE";

    /** 프로파일 속성 코드: 산업 유형 */
    private static final String ATTR_INDUSTRY_TYPE = "INDUSTRY_TYPE";

    /** 프로파일 속성 코드: 보관 온도 유형 */
    private static final String ATTR_STORAGE_TEMP_TYPE = "STORAGE_TEMP_TYPE";

    /** 프로파일 속성 코드: 회전 특성 코드 */
    private static final String ATTR_ROTATION_PROFILE_CODE = "ROTATION_PROFILE_CODE";

    /** 프로파일 속성 코드: LOT 관리 여부 */
    private static final String ATTR_LOT_CONTROL_REQUIRED = "LOT_CONTROL_REQUIRED";

    /** 프로파일 속성 코드: 유통기한 관리 여부 */
    private static final String ATTR_EXPIRY_CONTROL_REQUIRED = "EXPIRY_CONTROL_REQUIRED";

    /** 프로파일 속성 코드: 시리얼 관리 여부 */
    private static final String ATTR_SERIAL_CONTROL_REQUIRED = "SERIAL_CONTROL_REQUIRED";

    /** 프로파일 속성 코드: 부분출고 허용 여부 */
    private static final String ATTR_PARTIAL_PICK_ALLOWED = "PARTIAL_PICK_ALLOWED";

    /** 프로파일 속성 코드: 혼적 허용 여부 */
    private static final String ATTR_MIXED_LOAD_ALLOWED = "MIXED_LOAD_ALLOWED";

    /** 프로파일 속성 코드: 격리 필요 여부 */
    private static final String ATTR_QUARANTINE_REQUIRED = "QUARANTINE_REQUIRED";

    private final ItemQueryService itemQueryService;
    private final LocationQueryService locationQueryService;

    public ItemPolicyResolveCore(ItemQueryService itemQueryService,
                                 LocationQueryService locationQueryService) {
        this.itemQueryService = itemQueryService;
        this.locationQueryService = locationQueryService;
    }

    /**
     * 영역 + 품목 기준 최종 운영정책을 해석한다.
     *
     * @param areaId 영역 ID
     * @param itemId 품목 ID
     * @return 최종 운영정책
     */
    public ResolvedItemPolicy resolve(String areaId, String itemId) {
        try {
            TbAcStorageArea area = locationQueryService.findArea(areaId);
            TbAcItemMaster item = itemQueryService.findItem(itemId);
            TbAcItemCategory category = itemQueryService.findCategory(item.getItemCategoryId());

            ResolvedItemPolicy policy = new ResolvedItemPolicy();
            policy.setAreaId(areaId);
            policy.setItemId(itemId);
            policy.setAreaOperationProfileId(area.getOperationProfileId());
            policy.setCategoryOperationProfileId(category.getDefaultOperationProfileId());
            policy.setItemOperationProfileId(item.getOperationProfileId());

            // 1) 최종 프로파일 ID는 item > category > area 우선순위로 선택
            String finalOperationProfileId = firstNotBlank(
                    item.getOperationProfileId(),
                    category.getDefaultOperationProfileId(),
                    area.getOperationProfileId()
            );
            policy.setFinalOperationProfileId(finalOperationProfileId);

            // 2) 프로파일 기반 속성 누적
            //    area -> category -> item 순으로 덮어쓴다.
            Map<String, String> resolvedAttrMap = new LinkedHashMap<>();
            applyProfileAttributes(resolvedAttrMap, area.getOperationProfileId());
            applyProfileAttributes(resolvedAttrMap, category.getDefaultOperationProfileId());
            applyProfileAttributes(resolvedAttrMap, item.getOperationProfileId());

            // 3) SKU override는 가장 마지막에 덮어쓴다.
            applyItemOverrides(resolvedAttrMap, itemId);

            // 4) 디버깅/운영확인용으로 최종 맵 전체를 결과에 적재
            for (Map.Entry<String, String> entry : resolvedAttrMap.entrySet()) {
                policy.putResolvedAttribute(entry.getKey(), entry.getValue());
            }

            // 5) 핵심 item master 컬럼을 최종 우선값으로 사용
            policy.setAllocationRuleCode(
                    normalizeAllocationRule(
                            coalesce(
                                    item.getAllocationRuleCode(),
                                    resolvedAttrMap.get(ATTR_ALLOCATION_RULE_CODE),
                                    AcAllocationRule.FIFO.name()
                            )
                    )
            );
            policy.setIndustryType(
                    coalesce(
                            item.getIndustryType(),
                            resolvedAttrMap.get(ATTR_INDUSTRY_TYPE)
                    )
            );
            policy.setStorageTempType(
                    coalesce(
                            item.getStorageTempType(),
                            resolvedAttrMap.get(ATTR_STORAGE_TEMP_TYPE)
                    )
            );
            policy.setRotationProfileCode(
                    coalesce(
                            item.getRotationProfileCode(),
                            resolvedAttrMap.get(ATTR_ROTATION_PROFILE_CODE)
                    )
            );

            policy.setLotControlRequired(
                    resolveBoolean(item.getLotControlYn(), resolvedAttrMap.get(ATTR_LOT_CONTROL_REQUIRED))
            );
            policy.setExpiryControlRequired(
                    resolveBoolean(item.getExpiryControlYn(), resolvedAttrMap.get(ATTR_EXPIRY_CONTROL_REQUIRED))
            );
            policy.setSerialControlRequired(
                    resolveBoolean(item.getSerialControlYn(), resolvedAttrMap.get(ATTR_SERIAL_CONTROL_REQUIRED))
            );
            policy.setPartialPickAllowed(
                    resolveBoolean(item.getPartialPickYn(), resolvedAttrMap.get(ATTR_PARTIAL_PICK_ALLOWED))
            );
            policy.setMixedLoadAllowed(
                    resolveBoolean(item.getMixedLoadYn(), resolvedAttrMap.get(ATTR_MIXED_LOAD_ALLOWED))
            );
            policy.setQuarantineRequired(
                    resolveBoolean(item.getQuarantineRequiredYn(), resolvedAttrMap.get(ATTR_QUARANTINE_REQUIRED))
            );

            return policy;

        } catch (AisleCoreException e) {
            throw e;
        } catch (Exception e) {
            throw new AisleCoreException(
                    AisleCoreErrorCode.ITEM_POLICY_RESOLVE_FAILED,
                    "Failed to resolve item policy. areaId=" + areaId + ", itemId=" + itemId,
                    e
            );
        }
    }

    /**
     * 최종 할당 규칙 코드만 반환한다.
     */
    public String resolveAllocationRule(String areaId, String itemId) {
        return resolve(areaId, itemId).getAllocationRuleCode();
    }

    /**
     * LOT 관리 필요 여부를 반환한다.
     */
    public boolean isLotControlRequired(String areaId, String itemId) {
        return resolve(areaId, itemId).isLotControlRequired();
    }

    /**
     * 유통기한 관리 필요 여부를 반환한다.
     */
    public boolean isExpiryControlRequired(String areaId, String itemId) {
        return resolve(areaId, itemId).isExpiryControlRequired();
    }

    /**
     * 부분출고 허용 여부를 반환한다.
     */
    public boolean isPartialPickAllowed(String areaId, String itemId) {
        return resolve(areaId, itemId).isPartialPickAllowed();
    }

    /**
     * 혼적 허용 여부를 반환한다.
     */
    public boolean isMixedLoadAllowed(String areaId, String itemId) {
        return resolve(areaId, itemId).isMixedLoadAllowed();
    }

    /**
     * 운영 프로파일 속성값을 대상 맵에 누적 적용한다.
     *
     * <p>
     * 동일 attr_code 는 나중 단계 값이 앞 단계를 덮어쓴다.
     * </p>
     *
     * @param target 최종 누적 대상 맵
     * @param profileId 운영 프로파일 ID
     */
    private void applyProfileAttributes(Map<String, String> target, String profileId) {
        List<ResolvedAttrValue> values = itemQueryService.findProfileAttrs(profileId);
        for (ResolvedAttrValue value : values) {
            if (!ValueUtil.isEmpty(value.getAttrCode())) {
                target.put(value.getAttrCode(), value.getAttrValue());
            }
        }
    }

    /**
     * 품목 개별 override 값을 대상 맵에 누적 적용한다.
     *
     * <p>
     * SKU 개별 override 는 최종 우선순위를 가진다.
     * </p>
     *
     * @param target 최종 누적 대상 맵
     * @param itemId 품목 ID
     */
    private void applyItemOverrides(Map<String, String> target, String itemId) {
        List<ResolvedAttrValue> overrides = itemQueryService.findItemOverrides(itemId);
        for (ResolvedAttrValue value : overrides) {
            if (!ValueUtil.isEmpty(value.getAttrCode())) {
                target.put(value.getAttrCode(), value.getAttrValue());
            }
        }
    }

    /**
     * 최종 boolean 성격 값을 해석한다.
     *
     * <p>
     * item master 값이 있으면 그것을 우선 사용하고,
     * 없으면 프로파일/override 속성값을 사용한다.
     * 둘 다 없으면 false 로 처리한다.
     * </p>
     *
     * @param itemYn item master 의 Y/N 값
     * @param resolvedAttrValue 프로파일/override 해석값
     * @return 최종 boolean
     */
    private boolean resolveBoolean(String itemYn, String resolvedAttrValue) {
        String raw = coalesce(itemYn, resolvedAttrValue, "N");
        return AcYn.from(raw).toBoolean();
    }

    /**
     * 첫 번째 non-blank 값을 반환한다.
     */
    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (!ValueUtil.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 첫 번째 non-blank 값을 반환한다.
     */
    private String coalesce(String... values) {
        return firstNotBlank(values);
    }

    /**
     * 할당 규칙 코드를 normalize 한다.
     *
     * <p>
     * 잘못된 코드가 들어오면 Enum 변환 단계에서 예외가 발생한다.
     * </p>
     *
     * @param ruleCode 원본 코드
     * @return normalize 된 코드
     */
    private String normalizeAllocationRule(String ruleCode) {
        return AcAllocationRule.from(ruleCode).name();
    }
}