package operato.logis.kmat_2026.biz.ecs.sineva.processor;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.CommandType;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.EquipTaskType;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.EquipType;
import operato.logis.kmat_2026.biz.ecs.sineva.consts.LocationStatus;
import operato.logis.kmat_2026.biz.ecs.sineva.service.LocationLockService;
import operato.logis.kmat_2026.biz.ecs.sineva.service.LocationStateService;
import operato.logis.kmat_2026.biz.ecs.sineva.service.OrderCommandService;
import operato.logis.kmat_2026.biz.ecs.sineva.service.RouteSelectService;
import operato.logis.kmat_2026.biz.ecs.sineva.support.EcsErrorCode;
import operato.logis.kmat_2026.biz.ecs.sineva.support.EcsException;
import operato.logis.kmat_2026.entity.TbEcsLocMst;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.service.impl.TbEcsLocMstService;
import operato.logis.kmat_2026.service.impl.TbWcsOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

/**
 * ============================================================================
 * Shuttle Inbound Processor
 * ============================================================================
 *
 * [역할]
 * - 신규 inbound 작업 시작
 * - route 선택
 * - from/to lock 확보
 * - WCS order 생성
 * - 기존 createOrReleaseWcsOrder 정책으로 실제 지시 생성/전송
 *
 * [주의]
 * - release 정책은 OrderCommandService가 담당한다.
 * - 이 클래스는 "신규 시작" 흐름에 집중한다.
 */
@Component
public class TspgConveyorInboundProcessor implements OrderProcessor<TbWcsOrder> {

    private static final Logger logger = LoggerFactory.getLogger(TspgConveyorInboundProcessor.class);

    private static final String LOCK_FROM = "FROM";

    @Autowired
    protected TbWcsOrderService tbWcsOrderService;

    @Autowired
    protected TbEcsLocMstService tbEcsLocMstService;

    @Autowired
    protected RouteSelectService routeSelectService;

    @Autowired
    protected LocationLockService locationLockService;

    @Autowired
    protected OrderCommandService orderCommandService;

    @Autowired
    protected LocationStateService locationStateService;

    /**
     * 신규 inbound 실행
     *
     * @param fromLocationCd 출발지 location code
     * @return 생성된 WCS order
     */
    @Override
    @Transactional
    public TbWcsOrder execute(String fromLocationCd) {
        logger.info("[TspgConveyorInboundProcessor][execute] start - fromLocationCd={}", fromLocationCd);

        TbEcsLocMst fromLoc = null;
        TbEcsLocMst toLoc = null;
        String orderId = null;
        String taskId = null;

        try {
            fromLoc = tbEcsLocMstService.findLocationByCode(fromLocationCd);
            validateFromLocation(fromLocationCd, fromLoc);

            orderId = tbWcsOrderService.createOrderId();
            taskId = orderId;

            // routeSelectService.selectInboundTargetWithLock(...) 에서 TO lock 이 이미 잡힌 상태라고 가정
            toLoc = routeSelectService.selectInboundTargetWithLock(orderId, taskId);
            if (ValueUtil.isEmpty(toLoc)) {
                throw new EcsException(
                        EcsErrorCode.LOCATION_NOT_FOUND,
                        "입고 대상 위치를 찾을 수 없습니다. fromLocationCd=" + fromLocationCd + ", orderId=" + orderId
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

            TbWcsOrder order = buildInboundOrder(fromLoc, toLoc, orderId, taskId);

            TbWcsOrder created = orderCommandService.createAndSendInitialWcsOrder(order);

            logger.info(
                    "[TspgConveyorInboundProcessor][execute] success - orderId={}, taskId={}, from={}, to={}",
                    created.getOrderId(),
                    created.getTaskId(),
                    created.getFromSide(),
                    created.getToSide()
            );

            return created;

        } catch (EcsException e) {
            logger.warn(
                    "[TspgConveyorInboundProcessor][execute] business skip - errorCode={}, orderId={}, fromLocationCd={}, reason={}",
                    e.getErrorCode(),
                    orderId,
                    fromLocationCd,
                    e.getMessage()
            );

            if (ValueUtil.isNotEmpty(orderId)) {
                rollbackBothLocks(orderId, fromLoc, toLoc);
            }

            return null;

        } catch (Exception e) {
            logger.error(
                    "[TspgConveyorInboundProcessor][execute] fail - orderId={}, fromLocationCd={}, reason={}",
                    orderId,
                    fromLocationCd,
                    e.getMessage(),
                    e
            );

            if (ValueUtil.isNotEmpty(orderId)) {
                rollbackBothLocks(orderId, fromLoc, toLoc);
            }

            throw e;
        }
    }

    /**
     * callback 후속 처리
     *
     * 현재 실제 callback 정책은 EquipCallbackRoutingService 에서 처리하므로
     * processor callback 은 최소한의 trace 용도로만 유지한다.
     */
    @Override
    public TbWcsOrder callback(TbWcsOrder order) {
        if (ValueUtil.isEmpty(order)) {
            return null;
        }

        logger.info(
                "[TspgConveyorInboundProcessor][callback] orderId={}, taskId={}, cbkStatus={}",
                order.getOrderId(),
                order.getTaskId(),
                order.getCbkStatus()
        );

        locationStateService.updateLocationsOnAgvTaskComplete(order, LocationStatus.FULL);
        logger.info("[TspgConveyorInboundProcessor][callback] 로케이션 상태 FULL 업데이트 완료");

        orderCommandService.completeTask(order);
        logger.info("[TspgConveyorInboundProcessor][callback] task 완료 처리");

        return order;
    }

    @Override
    public String getProcessorType() {
        return getClass().getSimpleName();
    }

    /**
     * 출발지 유효성 검증
     */
    private void validateFromLocation(String fromLocationCd, TbEcsLocMst fromLoc) {
        if (ValueUtil.isEmpty(fromLoc)) {
            throw new EcsException(
                    EcsErrorCode.LOCATION_NOT_FOUND,
                    "출발 위치를 찾을 수 없습니다. fromLocationCd=" + fromLocationCd
            );
        }

        if (ValueUtil.isEmpty(fromLoc.getLocationCd())) {
            throw new EcsException(
                    EcsErrorCode.LOCATION_NOT_FOUND,
                    "출발 위치 코드가 비어 있습니다. fromLocationCd=" + fromLocationCd
            );
        }

        if (fromLoc.getLocationUseYn() == null || fromLoc.getLocationUseYn() != 1) {
            throw new EcsException(
                    EcsErrorCode.LOCATION_NOT_FOUND,
                    "출발 위치가 사용 불가 상태입니다. fromLocationCd=" + fromLocationCd
            );
        }
    }

    /**
     * Inbound 오더 생성
     *
     * [정책]
     * - 실제 저장/전송은 createOrReleaseWcsOrder 에서 수행
     * - 여기서는 생성용 order 객체만 만든다.
     */
    private TbWcsOrder buildInboundOrder(
            TbEcsLocMst fromLoc,
            TbEcsLocMst toLoc,
            String orderId,
            String taskId
    ) {
        TbWcsOrder order = tbWcsOrderService.createOrder(
                taskId,
                fromLoc.getLocationCd(),
                toLoc.getLocationCd(),
                fromLoc.getPodCd(),
                CommandType.K_MAT_TSPG_CONVEYOR_INBOUND,
                EquipType.AGF,
                null,
                1
        );

        order.setOrderId(orderId);
        order.setTaskType(EquipTaskType.FREIGHT_MOVE.getCode());

        return order;
    }

    /**
     * to lock만 되돌리기
     */
    private void rollbackToLock(String orderId, TbEcsLocMst toLoc) {
        if (ValueUtil.isNotEmpty(toLoc) && ValueUtil.isNotEmpty(toLoc.getLocationCd())) {
            locationLockService.unlockLocationByOrder(toLoc.getLocationCd(), orderId);
        }
    }

    /**
     * from/to lock 모두 되돌리기
     */
    private void rollbackBothLocks(String orderId, TbEcsLocMst fromLoc, TbEcsLocMst toLoc) {
        if (ValueUtil.isNotEmpty(fromLoc) && ValueUtil.isNotEmpty(fromLoc.getLocationCd())) {
            locationLockService.unlockLocationByOrder(fromLoc.getLocationCd(), orderId);
        }

        if (ValueUtil.isNotEmpty(toLoc) && ValueUtil.isNotEmpty(toLoc.getLocationCd())) {
            locationLockService.unlockLocationByOrder(toLoc.getLocationCd(), orderId);
        }
    }
}