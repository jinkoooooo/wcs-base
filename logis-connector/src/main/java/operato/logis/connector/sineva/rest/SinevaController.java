package operato.logis.connector.sineva.rest;

import com.fasterxml.jackson.databind.JsonNode;
import operato.logis.connector.sineva.facade.SinevaFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xyz.elidom.orm.system.annotation.service.ApiDesc;
import xyz.elidom.orm.system.annotation.service.ServiceDesc;

import java.util.List;
import java.util.Map;

/**
 * Sineva 연동 REST API 컨트롤러.
 *
 * 역할:
 * - 외부에서 HTTP로 들어온 요청을 받아
 * - SinevaFacade의 공식 인터페이스를 호출한다.
 *
 * 주의:
 * - 컨트롤러는 비즈니스/HTTP 세부 구현을 갖지 않는다.
 * - 파라미터 추출만 하고 facade에 위임한다.
 */
@RestController
@Transactional
@ResponseStatus(HttpStatus.OK)
@RequestMapping("/rest/test/ecs")
@ServiceDesc(description = "Sineva AGV Integration API")
public class SinevaController {

    @Autowired
    protected SinevaFacade sinevaFacade;

    /**
     * AGV 작업 생성
     *
     * Request Example:
     * {
     *   "url": "http://localhost:8080",
     *   "taskId": "TASK-001",
     *   "priority": 10,
     *   "agvGroup": "G1",
     *   "agvId": "AGV-01",
     *   "fromSide": "LOC-A",
     *   "toSide": "LOC-B",
     *   "type": "MOVE"
     * }
     */
    @PostMapping(value = "/createTask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Create AGV task via Sineva API")
    public JsonNode createTask(@RequestBody Map<String, Object> data) {
        return sinevaFacade.createTask(
                (String) data.get("url"),
                (String) data.get("taskId"),
                Integer.parseInt(String.valueOf(data.get("priority"))),
                (String) data.get("agvGroup"),
                (String) data.get("agvId"),
                (String) data.get("fromSide"),
                (String) data.get("toSide"),
                (String) data.get("type")
        );
    }

    /**
     * AGV 작업 취소
     *
     * Request Example:
     * {
     *   "url": "http://localhost:8080",
     *   "taskId": "TASK-001"
     * }
     */
    @PostMapping(value = "/cancelTask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Cancel AGV task via Sineva API")
    public JsonNode cancelTask(@RequestBody Map<String, Object> data) {
        return sinevaFacade.cancelTask(
                (String) data.get("url"),
                (String) data.get("taskId")
        );
    }

    /**
     * AGV 작업 우선순위 변경
     *
     * Request Example:
     * {
     *   "url": "http://localhost:8080",
     *   "taskId": "TASK-001",
     *   "priority": 5
     * }
     */
    @PutMapping(value = "/setTaskPriority", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Set AGV task priority via Sineva API")
    public JsonNode setTaskPriority(@RequestBody Map<String, Object> data) {
        return sinevaFacade.setTaskPriority(
                (String) data.get("url"),
                (String) data.get("taskId"),
                Integer.parseInt(String.valueOf(data.get("priority")))
        );
    }

    /**
     * AGV 작업 release
     *
     * Request Example:
     * {
     *   "url": "http://localhost:8080",
     *   "taskId": "TASK-001",
     *   "releaseCode": "REL-01"
     * }
     */
    @PostMapping(value = "/releaseCode", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Release AGV task via Sineva API")
    public JsonNode releaseTask(@RequestBody Map<String, Object> data) {
        return sinevaFacade.releaseTask(
                (String) data.get("url"),
                (String) data.get("taskId"),
                (String) data.get("releaseCode")
        );
    }

    /**
     * AGV 상태 조회
     *
     * Request Example:
     * {
     *   "url": "http://localhost:8080"
     * }
     */
    @PostMapping(value = "/robotsStatus", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Get AGV status via Sineva API")
    public JsonNode getAgvStatus(@RequestBody Map<String, Object> data) {
        return sinevaFacade.getAgvStatus((String) data.get("url"));
    }

    /**
     * 로봇 dispatchable 상태 변경
     *
     * Request Example:
     * {
     *   "url": "http://localhost:8080",
     *   "vehicles": ["AMR-01", "AMR-02"]
     * }
     */
    @SuppressWarnings("unchecked")
    @PostMapping(value = "/dispatchable", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Set robot running type via Sineva API")
    public JsonNode setRobotRunningType(@RequestBody Map<String, Object> data) {
        return sinevaFacade.setRobotRunningType(
                (String) data.get("url"),
                (List<String>) data.get("vehicles")
        );
    }

    /**
     * 현재 포인트 skip
     *
     * Request Example:
     * {
     *   "url": "http://localhost:8080",
     *   "equipId": "AMR-01"
     * }
     */
    @PostMapping(value = "/skipPoint", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiDesc(description = "Skip point via Sineva API")
    public JsonNode skipPoint(@RequestBody Map<String, Object> data) {
        return sinevaFacade.skipPoint(
                (String) data.get("url"),
                (String) data.get("equipId")
        );
    }
}