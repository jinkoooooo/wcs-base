package operato.logis.asrs.dto.response;

import lombok.Data;

/**
 * 오퍼레이션 프로필 저장/삭제 결과 DTO.
 */
@Data
public class OperationProfileSaveResult {

    private String id;
    private String profileCode;
    private String action;
    private String message;
}