package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts;

import java.util.List;

/**
 * ECS 인터페이스 상태 코드
 * - WCS가 ECS와 통신하는 상태 관리
 */
public enum EcsIfStatusEnumCode implements EnumCode {

    READY(0, "대기중 - 아직 ECS로 전송하지 않은 상태"),
    SENDING(10, "전송중 - ECS로 명령 전송 진행중"),
    SENT(20, "전송됨 - ECS로 명령 전송 완료"),
    ACK(30, "응답수신 - ECS로부터 정상 응답 수신"),
    FAIL(99, "실패 - ECS 통신 오류");

    private final Integer code;
    private final String desc;

    EcsIfStatusEnumCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override public Object code() { return code; }
    @Override public String desc() { return desc; }

    public List<String> aliases() { return List.of(); }

    public static EcsIfStatusEnumCode from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(
                EcsIfStatusEnumCode.class,
                codeOrAlias,
                true,
                EcsIfStatusEnumCode::aliases
        );
    }
}