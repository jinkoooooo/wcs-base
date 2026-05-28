package operato.logis.wcs.service.impl.order.intake;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.OrderType;
import operato.logis.wcs.consts.WcsError;
import operato.logis.wcs.dto.HostOrderApi;
import operato.logis.wcs.dto.WcsOrderCommand;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.handler.WcsOrderHandler;
import operato.logis.wcs.service.impl.system.SystemModeService;
import operato.logis.wcs.service.repository.ShuttleOrderRepository;
import operato.logis.wcs.simulator.SimulatorStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.elidom.exception.server.ElidomRuntimeException;
import xyz.elidom.util.ValueUtil;

import java.util.List;
import java.util.Set;

/**
 * 주문 수신 진입점 — 게이팅, 입력 검증, handler 분기, 응답 빌딩.
 *
 * 본 클래스는 트랜잭션 시작 지점이 아니다. 실제 등록 트랜잭션은 ShuttleOrderRegistrar 에서
 * 시작된다 — 게이팅·검증·예외 매핑은 트랜잭션 밖에서 이루어져야 하므로 빈을 분리한다.
 */
@Service
@RequiredArgsConstructor
public class OrderIntakeService {

    private static final Logger logger = LoggerFactory.getLogger(OrderIntakeService.class);

    private static final Set<String> BARCODE_REQUIRED_TYPES =
            Set.of(OrderType.INBOUND.codeAsString());

    private static final Set<String> ITEMS_OPTIONAL_TYPES =
            Set.of(OrderType.MOVE.codeAsString());

    private final List<WcsOrderHandler> orderHandlers;
    private final ShuttleOrderRegistrar registrar;
    private final SystemModeService systemModeService;
    private final SimulatorStateService simulatorStateService;
    private final ShuttleOrderRepository shuttleOrderRepository;

    /**
     * 주문 수신 + 등록 전체 진입점 — 게이팅, 검증, handler 분기, 등록.
     */
    public HostOrderApi.Response execute(WcsOrderCommand command) {
        // 시스템 모드 게이팅
        SystemModeService.GatingResult gate = systemModeService.check(
                command.getEqGroupId(), OrderType.from(command.getOrderType()));
        if (!gate.allowed()) {
            logger.warn("[ Order ][ Shuttle ] allocation gated - eqGroupId={}, orderType={}, reason={}",
                    command.getEqGroupId(), command.getOrderType(), gate.reason());
            return HostOrderApi.Response.fail(command.getHostOrderKey(),
                    gate.reason().codeAsString(), gate.reason().desc());
        }

        // 입력 검증
        String vError = validate(command);
        if (ValueUtil.isNotEmpty(vError)) {
            logger.warn("[ Order ][ Shuttle ] validation failed - {}", vError);
            return HostOrderApi.Response.fail(command.getHostOrderKey(),
                    WcsError.INVALID_REQUEST.codeAsString(), vError);
        }

        // handler 선택
        WcsOrderHandler handler = findHandler(command.getOrderType());
        if (ValueUtil.isEmpty(handler)) {
            logger.warn("[ Order ][ Shuttle ] no handler - orderType={}", command.getOrderType());
            return HostOrderApi.Response.fail(command.getHostOrderKey(),
                    WcsError.INVALID_ORDER_TYPE.codeAsString(), "No handler found");
        }

        // 등록 + 시뮬레이터 override
        OrderContext context;
        try {
            context = registrar.registerOrder(handler, command);
            applySimulatorOverride(command, context);
        } catch (ElidomRuntimeException e) {
            logger.error("[ Order ][ Shuttle ] location locked - hostOrderKey={}",
                    command.getHostOrderKey(), e);
            return HostOrderApi.Response.fail(command.getHostOrderKey(),
                    WcsError.LOCATION_LOCKED.codeAsString(), e.getMessage());
        } catch (Exception e) {
            logger.error("[ Order ][ Shuttle ] allocation failed - hostOrderKey={}",
                    command.getHostOrderKey(), e);
            return HostOrderApi.Response.fail(command.getHostOrderKey(),
                    WcsError.ALLOCATION_FAILED.codeAsString(), e.getMessage());
        }

        return HostOrderApi.Response.success(context.firstOrderKey(), command.getHostOrderKey());
    }

    /**
     * 시뮬레이션/단순 insert 흐름 — 방해물·예약·락 없이 shuttle insert 만.
     */
    public HostOrderApi.Response executeInsertOnly(WcsOrderCommand command) {
        WcsOrderHandler handler = findHandler(command.getOrderType());
        if (ValueUtil.isEmpty(handler)) {
            return HostOrderApi.Response.fail(command.getHostOrderKey(),
                    WcsError.INVALID_ORDER_TYPE.codeAsString(),
                    "No handler for orderType: " + command.getOrderType());
        }

        try {
            OrderContext context = registrar.executeInsertOnly(handler, command);
            return HostOrderApi.Response.success(context.firstOrderKey(), command.getHostOrderKey());
        } catch (Exception e) {
            logger.error("[ Order ][ Shuttle ] executeInsertOnly failed - hostOrderKey={}",
                    command.getHostOrderKey(), e);
            return HostOrderApi.Response.fail(command.getHostOrderKey(),
                    WcsError.INTERNAL_ERROR.codeAsString(), e.getMessage());
        }
    }

    /**
     * Simulator 모드면 등록된 모든 shuttle 의 orderType 을 MOVE 로 강제.
     */
    private void applySimulatorOverride(WcsOrderCommand command, OrderContext context) {
        if (!simulatorStateService.isSimulatorRunByEqGroupId(command.getEqGroupId())) return;
        for (TbWcsShuttleOrder order : context.getAllOrders()) {
            order.setOrderType(OrderType.MOVE.codeAsString());
            shuttleOrderRepository.update(order, "orderType");
            logger.info("[ Order ][ Shuttle ] simulator override - orderKey={}, newType=MOVE",
                    order.getOrderKey());
        }
    }

    /**
     * orderType 으로 핸들러 1건 선택.
     */
    private WcsOrderHandler findHandler(String orderType) {
        if (!StringUtils.hasText(orderType)) return null;
        return orderHandlers.stream().filter(h -> h.supports(orderType)).findFirst().orElse(null);
    }

    /**
     * 입력 검증 — null = 통과, 메시지 = 실패.
     */
    private String validate(WcsOrderCommand command) {
        if (ValueUtil.isEmpty(command)) return "command is required";
        if (!StringUtils.hasText(command.getOrderType())) return "orderType is required";
        if (ValueUtil.isEmpty(command.getEqGroupId())) return "eqGroupId is required";

        // items 비어있을 때 — MOVE 는 허용, 그 외는 거부
        if (ValueUtil.isEmpty(command.getItems())) {
            if (ITEMS_OPTIONAL_TYPES.contains(command.getOrderType())) return null;
            return "items is required and must not be empty";
        }

        // INBOUND 는 barCode 필수
        if (ValueUtil.isEmpty(command.getBarCode())
                && BARCODE_REQUIRED_TYPES.contains(command.getOrderType())) {
            return "barCode is required";
        }

        // MOVE 는 fromLocId 필수
        if (OrderType.MOVE.matches(command.getOrderType())
                && ValueUtil.isEmpty(command.getFromLocId())) {
            return "fromLocId is required for MOVE";
        }

        // 아이템 1건씩 검증
        for (int i = 0; i < command.getItems().size(); i++) {
            WcsOrderCommand.Item item = command.getItems().get(i);
            if (ValueUtil.isEmpty(item)) return "items[" + i + "] is null";
            if (!StringUtils.hasText(item.getItemCode())) return "items[" + i + "].itemCode is required";

            if (command.getOrderType().equals(OrderType.INBOUND.code())
                    && ValueUtil.isEmpty(item.getLotNo())) {
                return "items[" + i + "].lotNo is required";
            }
            if (ValueUtil.isEmpty(item.getQty()) || item.getQty() <= 0) {
                return "items[" + i + "].qty is required and must be positive";
            }
        }
        return null;
    }
}
