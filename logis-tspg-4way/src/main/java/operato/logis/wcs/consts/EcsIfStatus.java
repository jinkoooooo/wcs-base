package operato.logis.wcs.consts;

/**
 * ECS 인터페이스 송신 상태. 명령의 대기·전송·응답·실패 단계를 표현한다.
 */
public enum EcsIfStatus implements EnumCode {

    READY(0,    "대기중 - 아직 ECS로 전송하지 않은 상태"),
    SENDING(10, "전송중(릴리즈) - 오케스트레이터가 ECS 전달을 허가한 상태"),
    SENT(20,    "전송됨 - ECS로 명령 전송 완료"),
    ACK(30,     "응답수신 - ECS로부터 정상 응답 수신"),
    FAIL(99,    "실패 - ECS 통신 오류");

    private final Integer code;
    private final String  desc;

    EcsIfStatus(Integer code, String desc) {
        this.code = code; this.desc = desc;
    }

    @Override public Integer code() { return code; }
    @Override public String desc() { return desc; }

    /** 코드/별칭으로 enum 해석. 미일치 시 null. */
    public static EcsIfStatus from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(EcsIfStatus.class, codeOrAlias);
    }
}
