package operato.logis.asrs.dto.response;

import lombok.Data;

/**
 * 센터 저장/삭제 결과 DTO.
 */
@Data
public class CenterSaveResult {

    private String id;
    private String centerCode;
    private String action;
    private String message;
}