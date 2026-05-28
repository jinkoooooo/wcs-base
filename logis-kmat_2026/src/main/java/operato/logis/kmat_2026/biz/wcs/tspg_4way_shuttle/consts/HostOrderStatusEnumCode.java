package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts;

import java.util.List;

public enum HostOrderStatusEnumCode implements EnumCode {

    RECEIVED(0, "수신됨"),

    VALIDATED(10, "검증완료"),

    ALLOCATED(20, "로케이션 할당 및 재고 예약 완료"),

    WAITING_EXEC(30, "ECS 실행 대기"),

    EXECUTING(40, "설비 실행 중"),

    COMPLETED(80, "작업 완료"),

    CANCELLED(85, "취소"),

    REJECTED(88, "검증 실패 거절"),

    ERROR(100, "처리 중 오류");


    private final Integer code;
    private final String desc;

    HostOrderStatusEnumCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Object code() { return code; }

    @Override
    public String desc() { return desc; }

    /**
     * 레거시 문자열/이름 호환
     */
    public List<String> aliases() {
        return switch (this) {
            case WAITING_EXEC -> List.of("PROCESSING");
            case VALIDATED -> List.of("VALIDATE");
            default -> List.of();
        };
    }

    public static HostOrderStatusEnumCode from(Object codeOrAlias) {
        return EnumCodeUtil.fromCodeOrNull(
                HostOrderStatusEnumCode.class,
                codeOrAlias,
                true,
                HostOrderStatusEnumCode::aliases
        );
    }
}