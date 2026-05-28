package operato.logis.asrs.dto.response;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Access Point 목적 응답 DTO.
 */
@Getter
@Setter
@ToString
public class AccessPointPurposeResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String accessPointId;
    private String purposeCode;
    private Integer priorityNo;
    private String activeYn;
    private String description;
}