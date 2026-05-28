package operato.logis.kmat_2026.biz.ecs.sineva.service;

import com.fasterxml.jackson.databind.JsonNode;
import operato.logis.connector.sineva.facade.SinevaFacade;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.ResponseUtil;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.service.impl.TbEcsTaskProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * ECS Command Service
 * ============================================================================
 *
 * [역할]
 * - Sineva ECS 외부 인터페이스 전담
 * - 기존 EcsEquipCommandManager 역할 유지
 */
@Service("sinevaEcsCommandService")
public class EcsCommandService {

    private static final Logger logger = LoggerFactory.getLogger(EcsCommandService.class);

    @Autowired
    protected TbEcsTaskProcessService tbEcsTaskProcessService;

    public Map<String, Object> sendInitialTaskCommand(TbWcsOrder task) {
        try {
            JsonNode resp = SinevaFacade.createTask(
                    getSinevaUrl(),
                    task.getOrderId(),
                    task.getPriority(),
                    task.getEquipType(),
                    task.getEquipId(),
                    task.getFromSide(),
                    task.getToSide(),
                    task.getTaskType()
            );

            tbEcsTaskProcessService.saveReqCommandHistory(task, "taskCreate");
            return ResponseUtil.jsonNodeToMap(resp);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    public Map<String, Object> sendReleaseCommand(TbWcsOrder task) {
        try {
            JsonNode resp = SinevaFacade.releaseTask(getSinevaUrl(), task.getOrderId(), "0");
            tbEcsTaskProcessService.saveReqCommandHistory(task, "releaseTask");
            return ResponseUtil.jsonNodeToMap(resp);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    public Map<String, Object> cancelTask(TbWcsOrder task) {
        try {
            JsonNode resp = SinevaFacade.cancelTask(getSinevaUrl(), task.getOrderId());
            tbEcsTaskProcessService.saveReqCommandHistory(task, "cancelTask");
            return ResponseUtil.jsonNodeToMap(resp);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    public Map<String, Object> setTaskPriority(TbWcsOrder task, Integer priority) {
        try {
            JsonNode resp = SinevaFacade.setTaskPriority(getSinevaUrl(), task.getOrderId(), priority);
            tbEcsTaskProcessService.saveReqCommandHistory(task, "setTaskPriority");
            return ResponseUtil.jsonNodeToMap(resp);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    public Map<String, Object> getAgvStatus() {
        try {
            JsonNode resp = SinevaFacade.getAgvStatus(getSinevaUrl());
            return ResponseUtil.jsonNodeToMap(resp);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    public Map<String, Object> setRobotRunningType(List<String> vehicles) {
        try {
            JsonNode resp = SinevaFacade.setRobotRunningType(getSinevaUrl(), vehicles);
            return ResponseUtil.jsonNodeToMap(resp);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    public Map<String, Object> skipPoint(String equipId) {
        try {
            JsonNode resp = SinevaFacade.skipPoint(getSinevaUrl(), equipId);
            return ResponseUtil.jsonNodeToMap(resp);
        } catch (Exception e) {
            return buildErrorResponse(e);
        }
    }

    public boolean isSuccess(Map<String, Object> returnValue) {
        if (returnValue == null || !returnValue.containsKey("code")) {
            return false;
        }
        return "200".equals(String.valueOf(returnValue.get("code")));
    }

    public boolean isFail(Map<String, Object> returnValue) {
        return !isSuccess(returnValue);
    }

    private String getSinevaUrl() {
        // String sinevaUrl = "http://192.168.10.172:9000";
        // return "true".equals(SettingUtil.getValue("test")) ? "http://192.168.10.172:9000" : sinevaUrl;
        return "http://192.168.10.172:9000";
    }

    private Map<String, Object> buildErrorResponse(Exception e) {
        logger.error("[EcsCommandService] interface call fail: {}", e.getMessage(), e);
        return ResponseUtil.errorResponse(e.getMessage());
    }
}