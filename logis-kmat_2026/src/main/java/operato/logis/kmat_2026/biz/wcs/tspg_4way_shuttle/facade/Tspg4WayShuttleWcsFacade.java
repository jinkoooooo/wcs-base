package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.facade;

import operato.logis.kmat_2026.biz.ecs.tspg4way.dashboard.service.impl.TbEqGroupMstService;
import operato.logis.kmat_2026.biz.ecs.tspg4way.entity.TbEqGroupMst;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ErrorEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ShuttleOrderStatusEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.*;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.handler.WcsOrderHandler;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.*;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsHostOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrder;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsShuttleOrderItem;
import operato.logis.kmat_2026.service.impl.TbWcsHostOrderService;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderItemService;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ====================================================================
 * WCS 주문 수신 / ECS 콜백 오케스트레이션 (Facade)
 * ====================================================================
 *
 * [역할]
 * - 이 클래스는 비즈니스 로직(트랜잭션, 락, 재고)을 직접 다루지 않습니다.
 * - 외부 요청(Host, ECS)을 받아 적절한 Service로 작업을 "위임(Delegate)"하고
 * 그 결과를 반환하는 흐름 제어(교통정리) 역할만 수행합니다.
 */
@Service
public class Tspg4WayShuttleWcsFacade {

    private static final Logger logger = LoggerFactory.getLogger(Tspg4WayShuttleWcsFacade.class);

    @Autowired private List<WcsOrderHandler> orderHandlers;
    @Autowired private HostOrderValidationService validationService;
    @Autowired private HostOrderPersistenceService hostOrderPersistenceService;
    @Autowired private WcsOrderCommandMapper commandMapper;
    @Autowired private EcsCallbackProcessor ecsCallbackProcessor;
    @Autowired private TbWcsHostOrderService hostOrderService;
    @Autowired private TbWcsShuttleOrderService shuttleOrderService;
    @Autowired private TbWcsShuttleOrderItemService shuttleOrderItemService;
    @Autowired private TbEqGroupMstService tbEqGroupMstService;
    @Autowired private WcsOrderService wcsOrderService;

    /**
     * HOST 주문 수신 처리 (비동기 처리 지원)
     */
    public HostOrderReceiveResponse receiveHostOrder(HostOrderReceiveRequest request) {
        logger.info("Receiving host order. hostOrderKey={}", request == null ? null : request.getHostOrderKey());

        // 1. 유효성 검사 (입력값 체크)
        String validationError = validationService.validateRequest(request);
        if (validationError != null) {
            logger.error("Validation failed: {}", validationError);
            return HostOrderReceiveResponse.fail(request == null ? null : request.getHostOrderKey(),
                    ErrorEnumCode.INVALID_REQUEST.codeAsString(), validationError);
        }

        // 2. 멱등성 체크 (중복 수신 방지)
        TbWcsHostOrder existing = hostOrderService.findByHostOrderKey(request.getHostSystemCode(), request.getHostOrderKey());
        if (existing != null) {
            logger.info("Duplicate order received. hostOrderKey={}, wcsOrderKey={}",
                    existing.getHostOrderKey(), existing.getWcsOrderKey());
            return HostOrderReceiveResponse.duplicate(existing.getWcsOrderKey(), existing.getHostOrderKey());
        }

        // 3. 설비 그룹(eqGroupId) 선정
        if (!StringUtils.hasText(request.getEqGroupId())) {
            TbEqGroupMst group = tbEqGroupMstService.getGroup(request.getEqGroupId());
            if (ValueUtil.isEmpty(group) || ValueUtil.isEmpty(group.getId())) {
                logger.error("eqGroupId selection failed: {}", request.getEqGroupId());
                return HostOrderReceiveResponse.fail(request.getHostOrderKey(),
                        ErrorEnumCode.ALLOCATION_FAILED.codeAsString(), "eqGroupId selection failed");
            }
            request.setEqGroupId(group.getId());
        }

        // 4. Host Order 기록 보존 (WCS 내부 DB 저장 완료 -> Host에게는 '성공' 처리됨)
        TbWcsHostOrder hostOrder = hostOrderPersistenceService.saveHostOrderWithItems(request);

        // 5. WCS 핵심 비즈니스 로직(할당 및 전송)을 위한 Command 객체 생성
        WcsOrderCommand command = commandMapper.fromHostRequest(request);
        command.setPersistHostOrder(false); // 위에서 이미 수동으로 저장했으므로 매니저에서의 저장은 생략함

        // [핵심 변경] 6. WCS 내부 처리는 비동기로 던지고, Host에게는 즉시 성공(수신 완료) 응답을 반환
        CompletableFuture.runAsync(() -> {
            try {
                HostOrderReceiveResponse internalResponse = wcsOrderService.execute(command);

                // 내부 처리 결과에 따른 Host Order 상태 업데이트
                if (internalResponse.isSuccess()) {
                    hostOrderPersistenceService.markAllocated(hostOrder, internalResponse.getWcsOrderKey());
                } else {
                    logger.warn("Async Host order processing failed (will be retried by scheduler): {}", internalResponse.getErrorDesc());
                    hostOrderPersistenceService.markError(hostOrder, internalResponse.getErrorCode(), internalResponse.getErrorDesc());
                }
            } catch (Exception e) {
                logger.error("Unexpected error during async WCS order execution", e);
                hostOrderPersistenceService.markError(hostOrder, ErrorEnumCode.INTERNAL_ERROR.codeAsString(), e.getMessage());
            }
        });

        // 7. Host에는 수신(저장) 완료에 대한 성공 응답 즉각 반환 (응답 대기 시간 최소화)
        logger.info("Host order successfully received and saved. Internal processing started async. hostOrderKey={}", request.getHostOrderKey());
        return HostOrderReceiveResponse.success(null, request.getHostOrderKey());
        // 참고: 아직 비동기 할당 전이라 wcsOrderKey가 없을 수 있으므로 null을 넘기거나, 필요시 UUID 등으로 미리 채번해서 넘겨줄 수 있습니다.
    }

    /**
     * ECS 콜백 처리
     */
    public EcsCallbackResponse processEcsCallback(EcsCallbackRequest request) {
        logger.info("Processing ECS callback. orderKey={}, status={}, errorCode={}",
                request == null ? null : request.getOrderKey(),
                request == null ? null : request.getStatus(),
                request == null ? null : request.getErrorCode());

        if (request == null || !StringUtils.hasText(request.getOrderKey())) {
            return EcsCallbackResponse.fail(ErrorEnumCode.INVALID_REQUEST.codeAsString(), "orderKey is required");
        }

        TbWcsShuttleOrder shuttleOrder = shuttleOrderService.findByOrderKey(request.getOrderKey());
        if (shuttleOrder == null) {
            return EcsCallbackResponse.fail(ErrorEnumCode.ORDER_NOT_FOUND.codeAsString(), "Order not found");
        }

        if (ShuttleOrderStatusEnumCode.isFinalStatus(shuttleOrder.getOrderStatus())) {
            return EcsCallbackResponse.success("Order already processed (idempotent)");
        }

        List<TbWcsShuttleOrderItem> items = shuttleOrderItemService.findByOrderKey(shuttleOrder.getOrderKey());

        WcsOrderHandler handler = findHandler(shuttleOrder.getOrderType());
        if (handler == null) {
            return EcsCallbackResponse.fail(ErrorEnumCode.INVALID_ORDER_TYPE.codeAsString(), "No handler for order type: " + shuttleOrder.getOrderType());
        }

        // 콜백 트랜잭션 처리는 EcsCallbackProcessor 로 위임
        return ecsCallbackProcessor.process(request, shuttleOrder, items, handler);
    }

    // -----------------------------------------------------------------------
    // 내부 헬퍼 메서드
    // -----------------------------------------------------------------------

    private WcsOrderHandler findHandler(String orderType) {
        if (!StringUtils.hasText(orderType)) return null;
        return orderHandlers.stream().filter(handler -> handler.supports(orderType)).findFirst().orElse(null);
    }
}