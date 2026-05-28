package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ErrorEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.HostOrderReceiveResponse;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.WcsOrderCommand;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.handler.WcsOrderHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WcsOrderService {

    @Autowired private List<WcsOrderHandler> orderHandlers;
    @Autowired private WcsOrderValidationService validationService;
    @Autowired private EcsCommandService ecsCommandService;
    @Autowired private WcsOrderManager orderManager;

    private static final Logger logger = LoggerFactory.getLogger(WcsOrderService.class);

    /**
     * 메인 주문 실행 로직 (할당부터 전송까지)
     */
    public HostOrderReceiveResponse execute(WcsOrderCommand command) {
        logger.info("Executing WCS order: {}", command);

        // 1. 기본 검증
        String vError = validationService.validate(command);
        if (vError != null) {
            logger.error("Validation failed: {}", vError);
            return HostOrderReceiveResponse.fail(command.getSourceOrderKey(), ErrorEnumCode.INVALID_REQUEST.codeAsString(), vError);
        }

        // 2. 핸들러 조회
        WcsOrderHandler handler = findHandler(command.getOrderType());
        if (handler == null) {
            logger.error("No handler found for order type: {}", command.getOrderType());
            return HostOrderReceiveResponse.fail(command.getSourceOrderKey(), ErrorEnumCode.INVALID_ORDER_TYPE.codeAsString(), "No handler found");
        }

        // 3. [통합 트랜잭션 실행] 할당 + 락 + 예약 + 오더 생성
        // 이제 registerOrder 내부에서 원자적으로 allocateLocation이 호출됩니다.
        WcsOrderManager.OrderContext context;
        try {
            context = orderManager.registerOrder(handler, command);
        } catch (Exception e) {
            logger.error("Order registration failed (Transaction Rolled back): {}", e.getMessage());
            return HostOrderReceiveResponse.fail(command.getSourceOrderKey(), ErrorEnumCode.ALLOCATION_FAILED.codeAsString(), e.getMessage());
        }

        // 4. ECS 명령 전송 (트랜잭션 외부 호출 - 커넥션 점유 방지)
        boolean isEcsSent = ecsCommandService.sendCommand(context.order, context.items);

        // 5. 전송 실패 시 처리 (보상 트랜잭션: 상태만 전송실패로 변경)
        if (!isEcsSent) {
            logger.info("ECS command failed for order: {}. Marking for retry.", context.order.getOrderKey());
            orderManager.handleSendFailure(context.order.getOrderKey(), "Network Timeout or Connection Refused");
        }

        return HostOrderReceiveResponse.success(context.order.getOrderKey(), command.getSourceOrderKey());
    }

    /**
     * 시나리오용 단순 생성 실행 (Insert Only - 락/재고예약 제외)
     */
    public HostOrderReceiveResponse executeInsertOnly(WcsOrderCommand command) {
        return processOrder(command, true);
    }

    /**
     * 내부 공통 처리 로직
     */
    private HostOrderReceiveResponse processOrder(WcsOrderCommand command, boolean isInsertOnly) {
        WcsOrderHandler handler = findHandler(command.getOrderType());
        if (handler == null) {
            return HostOrderReceiveResponse.fail(command.getSourceOrderKey(), "ERR", "No handler");
        }

        WcsOrderManager.OrderContext context;
        try {
            // [수정] 밖에서 할당을 하지 않고 Manager 내부로 위임합니다.
            if (isInsertOnly) {
                context = orderManager.executeInsertOnly(handler, command);
            } else {
                context = orderManager.registerOrder(handler, command);
            }
            return HostOrderReceiveResponse.success(context.order.getOrderKey(), command.getSourceOrderKey());
        } catch (Exception e) {
            logger.error("Order process failed", e);
            return HostOrderReceiveResponse.fail(command.getSourceOrderKey(), "ERR", e.getMessage());
        }
    }

    private WcsOrderHandler findHandler(String orderType) {
        if (!StringUtils.hasText(orderType)) return null;
        return orderHandlers.stream().filter(h -> h.supports(orderType)).findFirst().orElse(null);
    }
}