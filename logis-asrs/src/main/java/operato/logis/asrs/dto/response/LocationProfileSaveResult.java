package operato.logis.asrs.dto.response;

import lombok.Data;

/**
 * 로케이션 프로필 저장/삭제 결과 DTO.
 */
@Data
public class LocationProfileSaveResult {

    private String id;
    private String areaCode;
    private String profileCode;
    private String action;
    private String message;
}