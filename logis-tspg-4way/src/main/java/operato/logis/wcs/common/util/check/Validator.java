package operato.logis.wcs.common.util.check;

import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

// 공통 입력값 검증 유틸 - 단순 검증 실패 시에만 사용 (비즈니스 예외는 호출부에서 직접 throw)
public class Validator {

    private Validator() {}

    // 조회 결과 null/empty 검증
    public static void requireFound(Object obj, String code, String message) {
        if (ValueUtil.isEmpty(obj)) {
            throw new ElidomRuntimeException(code, message);
        }
    }

    // 문자열 not empty 검증
    public static void requireNotEmpty(String value, String code, String message) {
        if (ValueUtil.isEmpty(value)) {
            throw new ElidomRuntimeException(code, message);
        }
    }

    // 양수 검증 (1 이상)
    public static void requirePositive(int value, String message) {
        if (value <= 0) {
            throw new ElidomRuntimeException("INVALID_QTY", message);
        }
    }

    // 0 이상 검증
    public static void requireNonNegative(int value, String message) {
        if (value < 0) {
            throw new ElidomRuntimeException("INVALID_QTY", message);
        }
    }

    // 로케이션 키(eqGroupId + locId) 모두 채워졌는지 확인 - location 작업 공통 가드
    public static boolean hasLocationKey(String eqGroupId, String locId) {
        return ValueUtil.isNotEmpty(eqGroupId) && ValueUtil.isNotEmpty(locId);
    }
}
