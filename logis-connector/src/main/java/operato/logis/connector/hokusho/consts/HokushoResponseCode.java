package operato.logis.connector.hokusho.consts;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HOKUSHO 응답 코드 규격
 */
@Getter
@AllArgsConstructor
public enum HokushoResponseCode {

    SUCCESS("0000", "success"),
    INVALID_FORMAT("1001", "invalid format / JSON schema error"),
    MISSING_PARAM("1002", "missing required parameter"),
    UNSUPPORTED_TYPE("2001", "unsupported commandType"),
    INVALID_LINE_OR_EQUIP("2002", "invalid lineId/equipId"),
    EQUIP_TIMEOUT("3001", "equipment timeout"),
    EQUIP_INTERNAL("3002", "equipment internal error"),
    SERVER_ERROR("9000", "unexpected server error");

    private final String code;
    private final String message;
}

