package operato.logis.asrs.core.item.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 상품 최종 운영정책 해석 결과 모델.
 *
 * <p>
 * 아래 3단계 정책을 병합한 최종 결과를 담는다.
 * </p>
 *
 * <ul>
 *   <li>센터/영역 기본 운영 프로파일</li>
 *   <li>상품군 기본 운영 프로파일</li>
 *   <li>SKU 개별 override</li>
 * </ul>
 *
 * <p>
 * 이후 재고 입고, 적치, 할당, 출고, 선배치 판단 시
 * 이 객체를 기준으로 운영 제약을 적용한다.
 * </p>
 */
public class ResolvedItemPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 대상 영역 ID */
    private String areaId;

    /** 대상 품목 ID */
    private String itemId;

    /** 영역 기본 운영 프로파일 ID */
    private String areaOperationProfileId;

    /** 상품군 기본 운영 프로파일 ID */
    private String categoryOperationProfileId;

    /** SKU 자체 지정 운영 프로파일 ID */
    private String itemOperationProfileId;

    /** 최종 적용된 운영 프로파일 ID */
    private String finalOperationProfileId;

    /** 최종 할당 규칙 코드 */
    private String allocationRuleCode;

    /** 산업 유형 */
    private String industryType;

    /** 보관 온도 유형 */
    private String storageTempType;

    /** 회전 특성 코드 */
    private String rotationProfileCode;

    /** LOT 관리 필요 여부 */
    private boolean lotControlRequired;

    /** 유통기한 관리 필요 여부 */
    private boolean expiryControlRequired;

    /** 시리얼 관리 필요 여부 */
    private boolean serialControlRequired;

    /** 부분출고 허용 여부 */
    private boolean partialPickAllowed;

    /** 혼적 허용 여부 */
    private boolean mixedLoadAllowed;

    /** 격리 필요 여부 */
    private boolean quarantineRequired;

    /**
     * 해석된 모든 속성 맵.
     *
     * <p>
     * attr_code -> 최종 attr_value 형태로 저장한다.
     * 디버깅/정책 확인/화면 표시 시 유용하다.
     * </p>
     */
    private final Map<String, String> resolvedAttributes = new LinkedHashMap<>();

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getAreaOperationProfileId() {
        return areaOperationProfileId;
    }

    public void setAreaOperationProfileId(String areaOperationProfileId) {
        this.areaOperationProfileId = areaOperationProfileId;
    }

    public String getCategoryOperationProfileId() {
        return categoryOperationProfileId;
    }

    public void setCategoryOperationProfileId(String categoryOperationProfileId) {
        this.categoryOperationProfileId = categoryOperationProfileId;
    }

    public String getItemOperationProfileId() {
        return itemOperationProfileId;
    }

    public void setItemOperationProfileId(String itemOperationProfileId) {
        this.itemOperationProfileId = itemOperationProfileId;
    }

    public String getFinalOperationProfileId() {
        return finalOperationProfileId;
    }

    public void setFinalOperationProfileId(String finalOperationProfileId) {
        this.finalOperationProfileId = finalOperationProfileId;
    }

    public String getAllocationRuleCode() {
        return allocationRuleCode;
    }

    public void setAllocationRuleCode(String allocationRuleCode) {
        this.allocationRuleCode = allocationRuleCode;
    }

    public String getIndustryType() {
        return industryType;
    }

    public void setIndustryType(String industryType) {
        this.industryType = industryType;
    }

    public String getStorageTempType() {
        return storageTempType;
    }

    public void setStorageTempType(String storageTempType) {
        this.storageTempType = storageTempType;
    }

    public String getRotationProfileCode() {
        return rotationProfileCode;
    }

    public void setRotationProfileCode(String rotationProfileCode) {
        this.rotationProfileCode = rotationProfileCode;
    }

    public boolean isLotControlRequired() {
        return lotControlRequired;
    }

    public void setLotControlRequired(boolean lotControlRequired) {
        this.lotControlRequired = lotControlRequired;
    }

    public boolean isExpiryControlRequired() {
        return expiryControlRequired;
    }

    public void setExpiryControlRequired(boolean expiryControlRequired) {
        this.expiryControlRequired = expiryControlRequired;
    }

    public boolean isSerialControlRequired() {
        return serialControlRequired;
    }

    public void setSerialControlRequired(boolean serialControlRequired) {
        this.serialControlRequired = serialControlRequired;
    }

    public boolean isPartialPickAllowed() {
        return partialPickAllowed;
    }

    public void setPartialPickAllowed(boolean partialPickAllowed) {
        this.partialPickAllowed = partialPickAllowed;
    }

    public boolean isMixedLoadAllowed() {
        return mixedLoadAllowed;
    }

    public void setMixedLoadAllowed(boolean mixedLoadAllowed) {
        this.mixedLoadAllowed = mixedLoadAllowed;
    }

    public boolean isQuarantineRequired() {
        return quarantineRequired;
    }

    public void setQuarantineRequired(boolean quarantineRequired) {
        this.quarantineRequired = quarantineRequired;
    }

    public Map<String, String> getResolvedAttributes() {
        return resolvedAttributes;
    }

    /**
     * 해석된 속성값을 누적한다.
     *
     * @param key 속성 코드
     * @param value 최종 속성값
     */
    public void putResolvedAttribute(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }
        this.resolvedAttributes.put(key, value);
    }

    /**
     * 특정 속성의 최종 해석값 조회.
     *
     * @param key 속성 코드
     * @return 최종 값
     */
    public String getResolvedAttribute(String key) {
        return this.resolvedAttributes.get(key);
    }
}