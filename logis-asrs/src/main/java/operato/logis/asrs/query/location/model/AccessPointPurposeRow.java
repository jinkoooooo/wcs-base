package operato.logis.asrs.query.location.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Access Point 목적 조회 모델.
 */
@Getter
@Setter
@ToString
public class AccessPointPurposeRow {

    private String id;
    private String accessPointId;
    private String purposeCode;
    private Integer priorityNo;
    private String activeYn;
    private String description;
}