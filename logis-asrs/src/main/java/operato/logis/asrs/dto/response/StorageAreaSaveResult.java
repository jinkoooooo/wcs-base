package operato.logis.asrs.dto.response;

import lombok.Data;

/**
 * 아레아 저장/삭제 결과 DTO.
 */
@Data
public class StorageAreaSaveResult {

    private String id;
    private String areaCode;
    private String action;
    private String message;
}