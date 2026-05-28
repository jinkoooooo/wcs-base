package operato.logis.connector.sineva.facade;

import com.fasterxml.jackson.databind.JsonNode;
import operato.logis.connector.sineva.client.SinevaHttpClient;
import operato.logis.connector.sineva.constants.EcsConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sineva 연동 공식 진입점
 *
 * [설계 목적]
 * - 다른 모듈에서는 이 Facade의 public static 메서드만 호출한다.
 * - endpoint 경로 / body key / 호출 방식 / 로깅은 내부에 감춘다.
 * - static 유틸처럼 사용하되, 실제 HTTP 호출은 Spring Bean인 SinevaHttpClient에 위임한다.
 *
 * [주의]
 * - static 메서드에서 Spring Bean을 사용하려면 일반 @Autowired 인스턴스 필드를 직접 사용할 수 없다.
 * - 따라서 ApplicationContextAware로 Bean을 받아 static 필드에 보관한다.
 */
@Component
public class SinevaFacade implements ApplicationContextAware {

    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(SinevaFacade.class);

    /**
     * static 메서드에서 사용할 HTTP 클라이언트
     */
    private static SinevaHttpClient httpClient;

    /**
     * Spring Context 초기화 시점에 Bean 주입
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        httpClient = applicationContext.getBean(SinevaHttpClient.class);
    }

    /**
     * AGV 작업 생성
     *
     * @param url      Sineva base URL
     * @param taskId   작업 ID
     * @param priority 우선순위
     * @param agvGroup AGV 그룹
     * @param agvId    AGV ID
     * @param fromSide 출발 위치
     * @param toSide   도착 위치
     * @param type     작업 타입
     * @return 응답 JSON
     */
    public static JsonNode createTask(
            String url,
            String taskId,
            int priority,
            String agvGroup,
            String agvId,
            String fromSide,
            String toSide,
            String type
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("taskid", taskId);
        body.put("priority", priority);
        body.put("agvGroup", agvGroup);
        body.put("agvID", agvId);
        body.put("fromSide", fromSide);
        body.put("toSide", toSide);
        body.put("type", type);

        JsonNode result = httpClient.call(HttpMethod.POST, url, EcsConstants.CREATE_TASK, body);

        logger.info(
                "\n\n==================== [SinevaFacade.createTask] ====================\n\n" +
                        "Request  → url={}, taskId={}, priority={}, agvGroup={}, agvId={}, fromSide={}, toSide={}, type={}\n" +
                        "Response → {}\n" +
                        "\n===================================================================\n",
                url, taskId, priority, agvGroup, agvId, fromSide, toSide, type, result
        );

        return result;
    }

    /**
     * AGV 작업 취소
     *
     * @param url    Sineva base URL
     * @param taskId 작업 ID
     * @return 응답 JSON
     */
    public static JsonNode cancelTask(String url, String taskId) {
        Map<String, Object> body = new HashMap<>();
        body.put("taskid", taskId);

        JsonNode result = httpClient.call(HttpMethod.POST, url, EcsConstants.CANCEL_TASK, body);

        logger.info(
                "\n\n==================== [SinevaFacade.cancelTask] ====================\n\n" +
                        "Request  → url={}, taskId={}\n" +
                        "Response → {}\n" +
                        "\n===================================================================\n",
                url, taskId, result
        );

        return result;
    }

    /**
     * AGV 작업 우선순위 변경
     *
     * @param url      Sineva base URL
     * @param taskId   작업 ID
     * @param priority 변경 우선순위
     * @return 응답 JSON
     */
    public static JsonNode setTaskPriority(String url, String taskId, int priority) {
        Map<String, Object> body = new HashMap<>();
        body.put("taskid", taskId);
        body.put("priority", priority);

        JsonNode result = httpClient.call(HttpMethod.POST, url, EcsConstants.SET_PRIORITY, body);

        logger.info(
                "\n\n==================== [SinevaFacade.setTaskPriority] ====================\n\n" +
                        "Request  → url={}, taskId={}, priority={}\n" +
                        "Response → {}\n" +
                        "\n========================================================================\n",
                url, taskId, priority, result
        );

        return result;
    }

    /**
     * AGV 작업 release 처리
     *
     * @param url         Sineva base URL
     * @param taskId      작업 ID
     * @param releaseCode release 코드
     * @return 응답 JSON
     */
    public static JsonNode releaseTask(String url, String taskId, String releaseCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("taskid", taskId);
        body.put("releaseCode", releaseCode);

        // 3초 대기 후 Release 전송 (시네바 요청사항)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.error("Delay interrupted: {}", e.getMessage());
        }
        Thread.currentThread().interrupt();

        JsonNode result = httpClient.call(HttpMethod.POST, url, EcsConstants.RELEASE_TASK, body);

        logger.info(
                "\n\n==================== [SinevaFacade.releaseTask] ====================\n\n" +
                        "Request  → url={}, taskId={}, releaseCode={}\n" +
                        "Response → {}\n" +
                        "\n====================================================================\n",
                url, taskId, releaseCode, result
        );

        return result;
    }

    /**
     * 전체 AGV 상태 조회
     *
     * @param url Sineva base URL
     * @return 응답 JSON
     */
    public static JsonNode getAgvStatus(String url) {
        JsonNode result = httpClient.call(HttpMethod.GET, url, EcsConstants.GET_AGV_STATUS, Map.of());

        logger.info(
                "\n\n==================== [SinevaFacade.getAgvStatus] ====================\n\n" +
                        "Request  → url={}\n" +
                        "Response → {}\n" +
                        "\n=====================================================================\n",
                url, result
        );

        return result;
    }

    /**
     * 로봇을 dispatchable 상태로 전환
     *
     * @param url      Sineva base URL
     * @param vehicles 대상 차량 목록
     * @return 응답 JSON
     */
    public static JsonNode setRobotRunningType(String url, List<String> vehicles) {
        Map<String, Object> body = new HashMap<>();
        body.put("vehicles", vehicles);
        body.put("type", "dispatchable");

        JsonNode result = httpClient.call(HttpMethod.POST, url, EcsConstants.SET_ROBOT_RUNNING_TYPE, body);

        logger.info(
                "\n\n==================== [SinevaFacade.setRobotRunningType] ====================\n\n" +
                        "Request  → url={}, vehicles={}\n" +
                        "Response → {}\n" +
                        "\n============================================================================\n",
                url, String.join(", ", vehicles), result
        );

        return result;
    }

    /**
     * 현재 포인트 skip 처리
     *
     * @param url     Sineva base URL
     * @param equipId AGV/설비 ID
     * @return 응답 JSON
     */
    public static JsonNode skipPoint(String url, String equipId) {
        Map<String, Object> body = new HashMap<>();
        body.put("agvid", equipId);

        JsonNode result = httpClient.call(HttpMethod.POST, url, EcsConstants.SKIP_POINT, body);

        logger.info(
                "\n\n==================== [SinevaFacade.skipPoint] ====================\n\n" +
                        "Request  → url={}, equipId={}\n" +
                        "Response → {}\n" +
                        "\n==================================================================\n",
                url, equipId, result
        );

        return result;
    }
}