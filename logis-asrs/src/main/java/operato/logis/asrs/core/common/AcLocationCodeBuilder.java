package operato.logis.asrs.core.common;

import org.springframework.stereotype.Component;

import operato.logis.asrs.enums.AcLocationSide;

/**
 * AisleCore 표준 로케이션 코드 생성기.
 *
 * <p>
 * 공용 위치키는 Aisle / Side / Bay / Level / Depth 기준으로 관리하며,
 * 화면 표시용 location_code 도 동일 좌표 기준으로 생성한다.
 * </p>
 *
 * <p>
 * 예시:
 * ASRS1-A01-L-B001-L01-D01
 * </p>
 */
@Component
public class AcLocationCodeBuilder {

    /**
     * 표준 로케이션 코드를 생성한다.
     *
     * @param areaCode 영역 코드
     * @param aisleNo Aisle 번호
     * @param sideCode Side 코드 (L/R)
     * @param bayNo Bay 번호
     * @param levelNo Level 번호
     * @param depthNo Depth 번호
     * @return 조합된 로케이션 코드
     */
    public String build(String areaCode,
                        Integer aisleNo,
                        String sideCode,
                        Integer bayNo,
                        Integer levelNo,
                        Integer depthNo) {

        // 좌표값과 Side 코드의 기본 유효성 검증
        validate(areaCode, aisleNo, sideCode, bayNo, levelNo, depthNo);

        String normalizedAreaCode = areaCode.trim().toUpperCase();
        String normalizedSideCode = AcLocationSide.from(sideCode).name();

        return normalizedAreaCode
                + "-A" + pad(aisleNo, 2)
                + "-" + normalizedSideCode
                + "-B" + pad(bayNo, 3)
                + "-L" + pad(levelNo, 2)
                + "-D" + pad(depthNo, 2);
    }

    /**
     * 숫자를 지정 자릿수만큼 0-padding 한다.
     *
     * @param value 원본 숫자
     * @param length 자릿수
     * @return 패딩된 문자열
     */
    public String pad(Integer value, int length) {
        if (value == null) {
            throw new IllegalArgumentException("Pad target value is null.");
        }

        return String.format("%0" + length + "d", value);
    }

    /**
     * 로케이션 코드 생성 전 기본 유효성 검증.
     *
     * <p>
     * 1차 기준으로 음수 좌표는 허용하지 않는다.
     * </p>
     */
    private void validate(String areaCode,
                          Integer aisleNo,
                          String sideCode,
                          Integer bayNo,
                          Integer levelNo,
                          Integer depthNo) {

        if (areaCode == null || areaCode.isBlank()) {
            throw new IllegalArgumentException("Area code is empty.");
        }
        if (aisleNo == null || aisleNo < 0) {
            throw new IllegalArgumentException("Invalid aisleNo: " + aisleNo);
        }
        if (bayNo == null || bayNo < 0) {
            throw new IllegalArgumentException("Invalid bayNo: " + bayNo);
        }
        if (levelNo == null || levelNo < 0) {
            throw new IllegalArgumentException("Invalid levelNo: " + levelNo);
        }
        if (depthNo == null || depthNo < 0) {
            throw new IllegalArgumentException("Invalid depthNo: " + depthNo);
        }

        // L/R 외 값은 Enum 변환 단계에서 차단
        AcLocationSide.from(sideCode);
    }
}