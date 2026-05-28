package operato.logis.wcs.common.validation;

import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

/**
 * 운영자 COMMENT 길이/blank 검증.
 *
 * 셀 상태 전환·라벨 재발행 양쪽 컨트롤러가 공통 사용한다.
 * 상한 500자는 {@code TbWcsAuditLog.reason} 컬럼 길이에 맞춘다.
 */
public final class CommentValidator {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 500;

    private CommentValidator() {}

    /**
     * COMMENT 가 비어 있지 않고 길이 제약을 충족하는지 검증.
     * 실패 시 {@link ElidomRuntimeException} 을 던진다.
     *
     * @param fieldLabel 사용자에게 노출되는 필드 이름 (예: "comment").
     * @param value      검증 대상 값.
     */
    public static void requireValid(String fieldLabel, String value) {
        if (ValueUtil.isEmpty(value) || value.trim().length() < MIN_LENGTH) {
            throw new ElidomRuntimeException(
                    fieldLabel + " 는 필수입니다 (" + MIN_LENGTH + "자 이상)");
        }
        if (value.length() > MAX_LENGTH) {
            throw new ElidomRuntimeException(
                    fieldLabel + " 는 " + MAX_LENGTH + "자 이하여야 합니다");
        }
    }
}
