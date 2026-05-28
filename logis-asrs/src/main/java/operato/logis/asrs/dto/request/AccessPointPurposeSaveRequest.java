package operato.logis.asrs.dto.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Access Point 목적 저장 요청 DTO.
 */
@Getter
@Setter
@ToString
public class AccessPointPurposeSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 목적 코드: INBOUND / OUTBOUND / PICK / RELOCATION */
    @JsonAlias({"purposeCode", "purpose_code"})
    private String purposeCode;

    /** 우선순위 */
    @JsonAlias({"priorityNo", "priority_no"})
    private Integer priorityNo;

    /** 활성 여부 */
    @JsonAlias({"activeYn", "active_yn"})
    private String activeYn;

    /** 설명 */
    private String description;
}