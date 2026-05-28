package operato.logis.connector.sineva.constants;

/**
 * Sineva ECS endpoint 상수 모음.
 *
 * - endpoint 문자열을 여기서만 관리한다.
 * - 다른 클래스에서는 하드코딩하지 않고 이 상수를 사용한다.
 */
public final class EcsConstants {

    private EcsConstants() {
        // 유틸성 상수 클래스이므로 인스턴스화 방지
    }

    /** 작업 생성 */
    public static final String CREATE_TASK = "/rest/ecs/createTask";

    /** 작업 취소 */
    public static final String CANCEL_TASK = "/rest/ecs/cancelTask";

    /** 작업 우선순위 변경 */
    public static final String SET_PRIORITY = "/rest/ecs/setTaskPriority";

    /** 작업 다음 목적지 릴리즈 */
    public static final String RELEASE_TASK = "/rest/ecs/releaseCode";

    /** AGV 상태 조회 */
    public static final String GET_AGV_STATUS = "/rest/ecs/robotsStatus";

    /** 로봇 dispatchable 상태 변경 */
    public static final String SET_ROBOT_RUNNING_TYPE = "/rest/ecs/dispatchable";

    /** 포인트 skip */
    public static final String SKIP_POINT = "/rest/ecs/skipPoint";
}