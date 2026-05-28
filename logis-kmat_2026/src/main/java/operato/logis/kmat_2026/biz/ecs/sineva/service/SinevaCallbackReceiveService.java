package operato.logis.kmat_2026.biz.ecs.sineva.service;

import operato.logis.connector.sineva.event.SinevaEvent;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.CbkStatus;
import operato.logis.kmat_2026.biz.ecs.sineva.event.CallbackEvent;
import operato.logis.kmat_2026.entity.TbEcsTaskProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.sys.entity.Domain;
import xyz.elidom.sys.system.service.AbstractQueryService;
import xyz.elidom.util.ValueUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * Sineva Callback Receive Service
 * ============================================================================
 *
 * [역할]
 * - 외부에서 수신된 SinevaEvent(callback)를 실제 내부 callback 처리 흐름으로 연결한다.
 *
 * [처리 순서]
 * 1. SinevaEvent 수신
 * 2. payload 파싱
 * 3. TbEcsTaskProcess 저장
 * 4. 내부 CallbackEvent 발행
 *
 * [주의]
 * - 여기서는 실제 WCS order 상태 반영까지 하지 않는다.
 * - "callback 원본 수신 및 내부 전달"까지만 책임진다.
 * - 이후 후속 처리(직렬 처리, order update, equip event 발행)는 별도 listener가 담당한다.
 */
@Service
public class SinevaCallbackReceiveService extends AbstractQueryService {

    private static final Logger logger = LoggerFactory.getLogger(SinevaCallbackReceiveService.class);

    private final ApplicationEventPublisher eventPublisher;

    public SinevaCallbackReceiveService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * robot callback 수신 처리
     *
     * [기존 정책 유지]
     * - domainId / interfaceName 조건으로 필터링
     * - callback payload를 TbEcsTaskProcess로 적재
     * - 내부 CallbackEvent로 후속 비동기/직렬 처리 넘김
     */
    @Transactional
    @EventListener(classes = SinevaEvent.class, condition = "#event.domainId == 7 and #event.interfaceName == 'callback'")
    public void callBackProcess(SinevaEvent event) {
        Map<String, Object> params = event.getIfData();

        String taskId = ValueUtil.isEmpty(params.get("taskId")) ? null : params.get("taskId").toString();
        String status = ValueUtil.isEmpty(params.get("status")) ? null : params.get("status").toString();
        String agvId = ValueUtil.isEmpty(params.get("robotCode")) ? null : params.get("robotCode").toString();
        String errorCode = ValueUtil.isEmpty(params.get("errorCode")) ? null : params.get("errorCode").toString();
        String currentPositionCode = ValueUtil.isEmpty(params.get("currentPositionCode")) ? null : params.get("currentPositionCode").toString();

        logger.info("[CALLBACK 수신] taskId={}, status={}, agvId={}, thread={}",
                taskId, status, agvId, Thread.currentThread().getName());

        CbkStatus cbk = CbkStatus.fromCode(status);

        TbEcsTaskProcess callBackTask = new TbEcsTaskProcess();
        callBackTask.setOrderId(taskId);
        callBackTask.setCbkStatus(status);
        callBackTask.setCbkStatusDesc(cbk.getDesc());
        callBackTask.setEquipStatus(status);
        callBackTask.setEquipId(agvId);
        callBackTask.setErrorCode(errorCode);
        callBackTask.setCurrentPositionCod(currentPositionCode);
        callBackTask.setReqType("RES");

        // 콜백 원본 이력 저장
        this.queryManager.insert(callBackTask);

        logger.info("[CALLBACK DB INSERT] taskId={}, status={}, agvId={}, thread={}",
                callBackTask.getOrderId(),
                callBackTask.getCbkStatus(),
                callBackTask.getEquipId(),
                Thread.currentThread().getName());

        // 내부 후속 이벤트 발행
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("callbackTask", callBackTask);

        CallbackEvent callbackEvent = new CallbackEvent(
                eventData,
                Domain.currentDomain()
        );

        eventPublisher.publishEvent(callbackEvent);

        logger.info("[CALLBACK EVENT PUBLISH] taskId={}, status={}, agvId={}, thread={}",
                callBackTask.getOrderId(),
                callBackTask.getCbkStatus(),
                callBackTask.getEquipId(),
                Thread.currentThread().getName());
    }
}