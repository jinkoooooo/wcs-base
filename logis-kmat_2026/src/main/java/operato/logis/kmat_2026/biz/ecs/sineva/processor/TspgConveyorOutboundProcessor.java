package operato.logis.kmat_2026.biz.ecs.sineva.processor;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.CommandType;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.LocationStatus;
import operato.logis.kmat_2026.biz.ecs.sineva.service.LocationLockService;
import operato.logis.kmat_2026.biz.ecs.sineva.service.LocationStateService;
import operato.logis.kmat_2026.biz.ecs.sineva.service.OrderCommandService;
import operato.logis.kmat_2026.biz.ecs.sineva.service.ProcessorSupportService;
import operato.logis.kmat_2026.biz.ecs.sineva.service.RouteSelectService;
import operato.logis.kmat_2026.biz.ecs.sineva.support.EcsErrorCode;
import operato.logis.kmat_2026.biz.ecs.sineva.support.EcsException;
import operato.logis.kmat_2026.entity.TbEcsLocMst;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.service.impl.TbWcsOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

/**
 * ============================================================================
 * TSPG Conveyor Outbound Processor
 * ============================================================================
 *
 * [역할]
 * - 출고단에서 시작하는 Conveyor 이동 처리
 *
 * [정책]
 * - 출발지는 출고단 group 이어야 한다.
 * - 입고단이 비어 있으면 입고단으로 이동한다.
 * - 입고단이 비어 있지 않으면 버퍼단으로 이동한다.
 * - 목적지에 따라 commandType 이 달라진다.
 */
@Component
public class TspgConveyorOutboundProcessor implements OrderProcessor<TbWcsOrder> {

    private static final Logger logger = LoggerFactory.getLogger(TspgConveyorOutboundProcessor.class);

    private static final String GROUP_OUT = "TSPG_CONV_OUT";
    private static final String GROUP_IN = "TSPG_CONV_IN";
    private static final String GROUP_BUF = "TSPG_CONV_BUF";

    private static final String LOCK_FROM = "FROM";
    private static final String LOCK_TO = "TO";

    @Autowired
    protected TbWcsOrderService tbWcsOrderService;

    @Autowired
    protected RouteSelectService routeSelectService;

    @Autowired
    protected LocationLockService locationLockService;

    @Autowired
    protected OrderCommandService orderCommandService;

    @Autowired
    protected LocationStateService locationStateService;

    @Autowired
    protected ProcessorSupportService processorSupportService;

    @Override
    @Transactional
    public TbWcsOrder execute(String fromLocationCd) {
        logger.info("[TspgConveyorOutboundProcessor][execute] start - fromLocationCd={}", fromLocationCd);

        TbEcsLocMst fromLoc = null;
        RouteDecision decision = null;
        String orderId = null;
        String taskId = null;

        try {
            fromLoc = processorSupportService.getRequiredLocation(fromLocationCd, "출발");
            validateFromLocation(fromLocationCd, fromLoc);

            orderId = tbWcsOrderService.createOrderId();
            taskId = orderId;

            decision = selectRouteDecision(orderId, taskId);
            if (ValueUtil.isEmpty(decision) || ValueUtil.isEmpty(decision.toLoc)) {
                throw new EcsException(
                        EcsErrorCode.LOCATION_NOT_FOUND,
                        "출고 대상 위치를 찾을 수 없습니다. fromLocationCd=" + fromLocationCd + ", orderId=" + orderId
                );
            }

            boolean fromLocked = locationLockService.tryLockLocation(
                    fromLoc.getLocationCd(),
                    orderId,
                    taskId,
                    LOCK_FROM
            );

            if (!fromLocked) {
                throw new EcsException(
                        EcsErrorCode.LOCK_CONFLICT,
                        "출발지 lock 획득 실패. locationCd=" + fromLoc.getLocationCd() + ", orderId=" + orderId
                );
            }

            TbWcsOrder order = processorSupportService.buildFreightMoveOrder(
                    orderId,
                    taskId,
                    fromLoc,
                    decision.toLoc,
                    decision.commandType
            );

            TbWcsOrder created = orderCommandService.createAndSendInitialWcsOrder(order);

            logger.info(
                    "[TspgConveyorOutboundProcessor][execute] success - routeName={}, commandType={}, orderId={}, from={}, to={}",
                    decision.routeName,
                    decision.commandType.getCode(),
                    created.getOrderId(),
                    created.getFromSide(),
                    created.getToSide()
            );

            return created;

        } catch (EcsException e) {
            logger.warn(
                    "[TspgConveyorOutboundProcessor][execute] business skip - errorCode={}, orderId={}, fromLocationCd={}, reason={}",
                    e.getErrorCode(),
                    orderId,
                    fromLocationCd,
                    e.getMessage()
            );

            if (ValueUtil.isNotEmpty(orderId)) {
                processorSupportService.rollbackBothLocks(
                        orderId,
                        fromLoc,
                        decision != null ? decision.toLoc : null
                );
            }

            return null;

        } catch (Exception e) {
            logger.error(
                    "[TspgConveyorOutboundProcessor][execute] fail - orderId={}, fromLocationCd={}, reason={}",
                    orderId,
                    fromLocationCd,
                    e.getMessage(),
                    e
            );

            if (ValueUtil.isNotEmpty(orderId)) {
                processorSupportService.rollbackBothLocks(
                        orderId,
                        fromLoc,
                        decision != null ? decision.toLoc : null
                );
            }

            throw e;
        }
    }

    @Override
    @Transactional
    public TbWcsOrder callback(TbWcsOrder order) {
        if (ValueUtil.isEmpty(order)) {
            return null;
        }

        logger.info(
                "[TspgConveyorOutboundProcessor][callback] orderId={}, taskId={}, cbkStatus={}",
                order.getOrderId(),
                order.getTaskId(),
                order.getCbkStatus()
        );

        locationStateService.updateLocationsOnAgvTaskComplete(order, LocationStatus.FULL);
        logger.info("[TspgConveyorOutboundProcessor][callback] 목적지 로케이션 상태 FULL 업데이트 완료");

        orderCommandService.completeTask(order);
        logger.info("[TspgConveyorOutboundProcessor][callback] task 완료 처리");

        return order;
    }

    @Override
    public String getProcessorType() {
        return getClass().getSimpleName();
    }

    private void validateFromLocation(String fromLocationCd, TbEcsLocMst fromLoc) {
        processorSupportService.validateUsableLocation(fromLocationCd, fromLoc, "출발");
        processorSupportService.validateGroupCd(fromLoc, GROUP_OUT, "출발");
    }

    private RouteDecision selectRouteDecision(String orderId, String taskId) {
        TbEcsLocMst inbound = routeSelectService.selectOneWithLockSkip(
                GROUP_IN,
                LocationStatus.EMPTY.getCode(),
                true,
                orderId,
                taskId,
                LOCK_TO
        );

        if (ValueUtil.isNotEmpty(inbound)) {
            return new RouteDecision(
                    "OUTBOUND_TO_INBOUND",
                    inbound,
                    CommandType.K_MAT_TSPG_CONVEYOR_INBOUND
            );
        }

        TbEcsLocMst buffer = routeSelectService.selectOneWithLockSkip(
                GROUP_BUF,
                LocationStatus.EMPTY.getCode(),
                true,
                orderId,
                taskId,
                LOCK_TO
        );

        if (ValueUtil.isNotEmpty(buffer)) {
            return new RouteDecision(
                    "OUTBOUND_TO_BUFFER",
                    buffer,
                    CommandType.K_MAT_TSPG_CONVEYOR_BUFFER_OUTBOUND
            );
        }

        return null;
    }

    private static class RouteDecision {
        private final String routeName;
        private final TbEcsLocMst toLoc;
        private final CommandType commandType;

        private RouteDecision(String routeName, TbEcsLocMst toLoc, CommandType commandType) {
            this.routeName = routeName;
            this.toLoc = toLoc;
            this.commandType = commandType;
        }
    }
}