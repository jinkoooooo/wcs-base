package operato.logis.asrs.query.item.model;

import java.io.Serializable;

/**
 * 운영 프로파일 속성 / 품목 override 속성 조회용 DTO.
 *
 * <p>
 * 자동생성 엔티티에는 attr_code 가 직접 없고 attr_def_id 만 있는 구조이므로,
 * tb_ac_profile_attr_def 와 조인 조회한 결과를 담기 위한 전용 DTO 이다.
 * </p>
 */
public class ResolvedAttrValue implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 속성 정의 ID */
    private String attrDefId;

    /** 속성 코드 */
    private String attrCode;

    /** 속성명 */
    private String attrName;

    /** 속성값 */
    private String attrValue;

    public String getAttrDefId() {
        return attrDefId;
    }

    public void setAttrDefId(String attrDefId) {
        this.attrDefId = attrDefId;
    }

    public String getAttrCode() {
        return attrCode;
    }

    public void setAttrCode(String attrCode) {
        this.attrCode = attrCode;
    }

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    public String getAttrValue() {
        return attrValue;
    }

    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue;
    }
}