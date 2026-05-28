package operato.logis.wcs.common.service.audit;

/**
 * 감사 행위자 컨텍스트.
 * 진입 경계(HTTP 인터셉터·ECS 콜백·스케줄러)에서 set, 처리 후 clear.
 * 비동기 실행 시 TaskDecorator 가 capture/restore 로 스레드 간 전파한다.
 */
public final class ActorContext {

    /** 진입 채널 식별자. */
    public static final String CH_HTTP_UI = "HTTP_UI";
    public static final String CH_ECS_CALLBACK = "ECS_CALLBACK";
    public static final String CH_SCHEDULER = "SCHEDULER";

    /** 행위자 스냅샷. 진입 시 1회 결정해 스레드에 보관. */
    public record Actor(ActorType type, String id, String name, String channel) {}

    private static final ThreadLocal<Actor> HOLDER = new ThreadLocal<>();

    private ActorContext() {}

    /** 행위자 설정. */
    public static void set(Actor actor) {
        HOLDER.set(actor);
    }

    /** 현재 행위자 반환. 없으면 null. */
    public static Actor get() {
        return HOLDER.get();
    }

    /** 컨텍스트 제거. 진입 경계 finally 에서 호출. */
    public static void clear() {
        HOLDER.remove();
    }

    /** 운영자(사람) 행위자. */
    public static Actor user(String id, String name) {
        return new Actor(ActorType.USER, id, name, CH_HTTP_UI);
    }

    /** ECS 설비제어 콜백 행위자. */
    public static Actor ecs() {
        return new Actor(ActorType.ECS, "ECS", "ECS Callback", CH_ECS_CALLBACK);
    }

    /** 스케줄러 행위자. */
    public static Actor scheduler(String jobName) {
        return new Actor(ActorType.SCHEDULER, jobName, jobName, CH_SCHEDULER);
    }
}
