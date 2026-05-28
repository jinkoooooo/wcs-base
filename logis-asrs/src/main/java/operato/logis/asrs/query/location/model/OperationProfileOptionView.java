package operato.logis.asrs.query.location.model;

import lombok.Data;

/**
 * 오퍼레이션 프로필 선택 옵션 조회 모델.
 */
@Data
public class OperationProfileOptionView {

    private String id;
    private String profileCode;
    private String profileName;
    private String activeYn;
}