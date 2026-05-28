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
 * TSPG Conveyor Inbound Refill Processor
 * ============================================================================
 *
 * [역할]
 * - 입고단 작업 완료 후, 다음 이송 대상을 입고단으로 당겨오는 처리
 *
 * [정책]
 * - to 는 항상 입고단이다.
 * - from 선택 우선순위:
 *   1) 버퍼단(FULL)
 *   2) 출고단(FULL, location_seq ASC)
 *
 * [CommandType]
 * - 버퍼 -> 입고단  : K_MAT_TSPG_BUFFER_TO_CONVEYOR_INBOUND
 * - 출고 -> 입고단  : K_MAT_TSPG_CONVEYOR_INBOUND
 *
 * [주의]
 * - selectOneWithLockSkip(...) 호출 시 from lock 은 이미 획득된 상태로 반환된다.
 * - execute 본문에서 from lock 을 다시 획득하지 않는다.
 */
@Component
public class TspgConveyorInboundRefillProcessor implements OrderProcessor<TbWcsOrder> {

    private static final Logger logger = LoggerFactory.getLogger(TspgConveyorInboundRefillProcessor.class);

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

    /**
     * 입고단 기준 실행
     *
     * @param inboundLocationCd 목적지 입고단 locationCd
     */
    @Override
    @Transactional
    public TbWcsOrder execute(String inboundLocationCd) {
        logger.info("[TspgConveyorInboundRefillProcessor][execute] start - inboundLocationCd={}", inboundLocationCd);

        TbEcsLocMst toLoc = null;
        RouteDecision decision = null;
        String orderId = null;
        String taskId = null;

        try {
            // to 는 항상 입고단
            toLoc = processorSupportService.getRequiredLocation(inboundLocationCd, "입고");
            validateToInboundLocation(inboundLocationCd, toLoc);

            orderId = tbWcsOrderService.createOrderId();
            taskId = orderId;

            // 1순위 버퍼 FULL, 2순위 출고 FULL(location_seq ASC)
            // 주의: 여기서 이미 FROM lock 이 잡힌 상태
            decision = selectSourceDecision(orderId, taskId);

            if (ValueUtil.isEmpty(decision) || ValueUtil.isEmpty(decision.fromLoc)) {
                throw new EcsException(
                        EcsErrorCode.LOCATION_NOT_FOUND,
                        "입고단으로 이동할 출발 위치를 찾을 수 없습니다. inboundLocationCd=" + inboundLocationCd + ", orderId=" + orderId
                );
            }

            // to(입고단) lock
            boolean toLocked = locationLockService.tryLockLocation(
                    toLoc.getLocationCd(),
                    orderId,
                    taskId,
                    LOCK_TO
            );

            if (!toLocked) {
                throw new EcsException(
                        EcsErrorCode.LOCK_CONFLICT,
                        "입고단 lock 획득 실패. locationCd=" + toLoc.getLocationCd() + ", orderId=" + orderId
                );
            }

            TbWcsOrder order = processorSupportService.buildFreightMoveOrder(
                    orderId,
                    taskId,
                    decision.fromLoc,
                    toLoc,
                    decision.commandType
            );

            TbWcsOrder created = orderCommandService.createAndSendInitialWcsOrder(order);

            logger.info(
                    "[TspgConveyorInboundRefillProcessor][execute] success - routeName={}, commandType={}, orderId={}, from={}, to={}",
                    decision.routeName,
                    decision.commandType.getCode(),
                    created.getOrderId(),
                    created.getFromSide(),
                    created.getToSide()
            );

            return created;

        } catch (EcsException e) {
            logger.warn(
                    "[TspgConveyorInboundRefillProcessor][execute] business skip - errorCode={}, orderId={}, inboundLocationCd={}, reason={}",
                    e.getErrorCode(),
                    orderId,
                    inboundLocationCd,
                    e.getMessage()
            );

            if (ValueUtil.isNotEmpty(orderId)) {
                processorSupportService.rollbackBothLocks(
                        orderId,
                        decision != null ? decision.fromLoc : null,
                        toLoc
                );
            }

            return null;

        } catch (Exception e) {
            logger.error(
                    "[TspgConveyorInboundRefillProcessor][execute] fail - orderId={}, inboundLocationCd={}, reason={}",
                    orderId,
                    inboundLocationCd,
                    e.getMessage(),
                    e
            );

            if (ValueUtil.isNotEmpty(orderId)) {
                processorSupportService.rollbackBothLocks(
                        orderId,
                        decision != null ? decision.fromLoc : null,
                        toLoc
                );
            }

            throw e;
        }
    }

    /**
     * callback 은 프로젝트 정책에 따라 달라질 수 있으니
     * 현재는 기본적인 완료 처리만 둠
     */
    @Override
    @Transactional
    public TbWcsOrder callback(TbWcsOrder order) {
        if (ValueUtil.isEmpty(order)) {
            return null;
        }

        logger.info(
                "[TspgConveyorInboundRefillProcessor][callback] orderId={}, taskId={}, cbkStatus={}",
                order.getOrderId(),
                order.getTaskId(),
                order.getCbkStatus()
        );

        // 목적지(입고단)는 FULL
        locationStateService.updateLocationsOnAgvTaskComplete(order, LocationStatus.FULL);
        logger.info("[TspgConveyorInboundRefillProcessor][callback] 목적지 로케이션 상태 FULL 업데이트 완료");

        orderCommandService.completeTask(order);
        logger.info("[TspgConveyorInboundRefillProcessor][callback] task 완료 처리");

        return order;
    }

    @Override
    public String getProcessorType() {
        return getClass().getSimpleName();
    }

    /**
     * to 는 입고단이어야 하고, 다음 물건을 받을 수 있는 상태여야 함
     *
     * 현재는 EMPTY 기준으로 검증
     */
    private void validateToInboundLocation(String inboundLocationCd, TbEcsLocMst toLoc) {
        processorSupportService.validateUsableLocation(inboundLocationCd, toLoc, "입고");
        processorSupportService.validateGroupCd(toLoc, GROUP_IN, "입고");

        if (!ValueUtil.isEqual(toLoc.getLocationStatus(), LocationStatus.EMPTY.getCode())) {
            throw new EcsException(
                    EcsErrorCode.INVALID_PARAMETER,
                    "입고단이 EMPTY 상태가 아닙니다. inboundLocationCd=" + inboundLocationCd + ", locationStatus=" + toLoc.getLocationStatus()
            );
        }
    }

    /**
     * from 선택
     *
     * 우선순위
     * 1. 버퍼단 FULL
     * 2. 출고단 FULL (location_seq ASC)
     *
     * 주의:
     * - selectOneWithLockSkip(...) 는 반환 시점에 이미 FROM lock 이 잡혀 있다.
     */
    private RouteDecision selectSourceDecision(String orderId, String taskId) {
        TbEcsLocMst buffer = routeSelectService.selectOneWithLockSkip(
                GROUP_BUF,
                LocationStatus.FULL.getCode(),
                true,
                orderId,
                taskId,
                LOCK_FROM
        );

        if (ValueUtil.isNotEmpty(buffer)) {
            return new RouteDecision(
                    "BUFFER_TO_INBOUND",
                    buffer,
                    CommandType.K_MAT_TSPG_BUFFER_TO_CONVEYOR_INBOUND
            );
        }

        TbEcsLocMst outbound = routeSelectService.selectOneWithLockSkip(
                GROUP_OUT,
                LocationStatus.FULL.getCode(),
                true,
                orderId,
                taskId,
                LOCK_FROM
        );

        if (ValueUtil.isNotEmpty(outbound)) {
            return new RouteDecision(
                    "OUTBOUND_TO_INBOUND",
                    outbound,
                    CommandType.K_MAT_TSPG_CONVEYOR_INBOUND
            );
        }

        return null;
    }

    private static class RouteDecision {
        private final String routeName;
        private final TbEcsLocMst fromLoc;
        private final CommandType commandType;

        private RouteDecision(String routeName, TbEcsLocMst fromLoc, CommandType commandType) {
            this.routeName = routeName;
            this.fromLoc = fromLoc;
            this.commandType = commandType;
        }
    }
}