package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service;

import operato.logis.kmat_2026.biz.wcs.kmat_2026.service.KMat2026TspgCallbackHandler;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.EcsCallbackStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.EcsCallbackRequest;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.EcsCallbackResponse;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.facade.Tspg4WayShuttleWcsFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * ====================================================================
 * Internal ECS Callback Service
 * ====================================================================
 *
 * [목적]
 * - 같은 시스템 내부에서 ECS 작업 상태를 WCS로 전달하기 위한 서비스
 * - REST API 호출 없이 메서드 호출로 콜백 처리
 *
 * [사용 예]
 *
 * ecsCallbackService.complete(orderKey);
 * ecsCallbackService.started(orderKey);
 * ecsCallbackService.error(orderKey, "ECS_ERR", "equipment failure");
 * ecsCallbackService.cancelled(orderKey);
 *
 * ====================================================================
 */
@Service
public class InternalEcsCallbackService {

    @Autowired
    private Tspg4WayShuttleWcsFacade wcsFacade;

    /**
     * 작업 시작
     */
    public EcsCallbackResponse started(String orderKey) {
        return wcsFacade.processEcsCallback(
                EcsCallbackRequest.builder()
                        .orderKey(orderKey)
                        .status(EcsCallbackStatusEnumCode.STARTED.codeAsString())
                        .build()
        );
    }

    /**
     * 작업 진행중
     */
    public EcsCallbackResponse inProgress(String orderKey) {
        return wcsFacade.processEcsCallback(
                EcsCallbackRequest.builder()
                        .orderKey(orderKey)
                        .status(EcsCallbackStatusEnumCode.IN_PROGRESS.codeAsString())
                        .build()
        );
    }

    /**
     * 작업 완료
     */
    public EcsCallbackResponse complete(String orderKey) {
        return wcsFacade.processEcsCallback(
                EcsCallbackRequest.builder()
                        .orderKey(orderKey)
                        .status(EcsCallbackStatusEnumCode.COMPLETE.codeAsString())
                        .build()
        );
    }

    /**
     * 작업 실패
     */
    public EcsCallbackResponse error(String orderKey, String errorCode, String message) {
        return wcsFacade.processEcsCallback(
                EcsCallbackRequest.builder()
                        .orderKey(orderKey)
                        .status(EcsCallbackStatusEnumCode.ERROR.codeAsString())
                        .errorCode(errorCode)
                        .message(message)
                        .build()
        );
    }

    /**
     * 작업 취소
     */
    public EcsCallbackResponse cancelled(String orderKey) {
        return wcsFacade.processEcsCallback(
                EcsCallbackRequest.builder()
                        .orderKey(orderKey)
                        .status(EcsCallbackStatusEnumCode.CANCELLED.codeAsString())
                        .build()
        );
    }

    /**
     * 렉단 컨베이어 도착 완료
     * - ECS에서 렉단 컨베이어에 파렛트 도착 완료 시 호출
     * - 입고 명령 전송 + AGF 입고 리필 트리거
     */
    public EcsCallbackResponse conveyorArrived(String orderKey) {
        return wcsFacade.processEcsCallback(
                EcsCallbackRequest.builder()
                        .orderKey(orderKey)
                        .status(EcsCallbackStatusEnumCode.RACK_CONVEYOR_ARRIVED.codeAsString())
                        .build()
        );
    }
}