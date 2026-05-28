package operato.logis.wcs.service.impl.ecs;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.EcsCallbackStatus;
import operato.logis.wcs.dto.EcsCallbackApi;
import operato.logis.wcs.facade.Tspg4WayShuttleWcsFacade;
import org.springframework.stereotype.Service;

/**
 * 내부(같은 JVM/REST 컨트롤러) 호출용 ECS 콜백 트리거.
 *
 * 트랜잭션은 Facade(processEcsCallback) 가 시작하므로 본 클래스는 트랜잭션 없음.
 */
@Service
@RequiredArgsConstructor
public class InternalEcsCallbackHandler {

    private final Tspg4WayShuttleWcsFacade wcsFacade;

    /**
     * 셔틀 작업 시작 콜백.
     */
    public EcsCallbackApi.Response started(String orderKey) {
        return dispatch(orderKey, EcsCallbackStatus.STARTED);
    }

    /**
     * 셔틀 진행 중 콜백.
     */
    public EcsCallbackApi.Response inProgress(String orderKey) {
        return dispatch(orderKey, EcsCallbackStatus.IN_PROGRESS);
    }

    /**
     * 출발지 적재 완료 콜백.
     */
    public EcsCallbackApi.Response fromLoadingComplete(String orderKey) {
        return dispatch(orderKey, EcsCallbackStatus.FROM_LOADING_COMPLETE);
    }

    /**
     * 목적지 하역 완료 콜백.
     */
    public EcsCallbackApi.Response toUnloadingComplete(String orderKey) {
        return dispatch(orderKey, EcsCallbackStatus.TO_UNLOADING_COMPLETE);
    }

    /**
     * 셔틀 작업 종료 콜백.
     */
    public EcsCallbackApi.Response complete(String orderKey) {
        return dispatch(orderKey, EcsCallbackStatus.COMPLETE);
    }

    /**
     * 셔틀 작업 취소 콜백.
     */
    public EcsCallbackApi.Response cancelled(String orderKey) {
        return dispatch(orderKey, EcsCallbackStatus.CANCELLED);
    }

    /**
     * 랙 컨베이어 도착 콜백.
     */
    public EcsCallbackApi.Response conveyorArrived(String orderKey) {
        return dispatch(orderKey, EcsCallbackStatus.RACK_CONVEYOR_ARRIVED);
    }

    /**
     * ECS 에러 콜백. errorCode/message 포함.
     */
    public EcsCallbackApi.Response error(String orderKey, String errorCode, String message) {
        return wcsFacade.processEcsCallback(
                EcsCallbackApi.Request.builder()
                        .orderKey(orderKey)
                        .status(EcsCallbackStatus.ERROR.codeAsString())
                        .errorCode(errorCode)
                        .message(message)
                        .build());
    }

    /**
     * 단순 status 만 실어 Facade 로 위임.
     */
    private EcsCallbackApi.Response dispatch(String orderKey, EcsCallbackStatus status) {
        return wcsFacade.processEcsCallback(
                EcsCallbackApi.Request.builder()
                        .orderKey(orderKey)
                        .status(status.codeAsString())
                        .build());
    }
}
