package operato.logis.kmat_2026.biz.ecs.sineva.service;

import operato.logis.kmat_2026.biz.ecs.sineva.consts.LocationStatus;
import operato.logis.kmat_2026.entity.TbWcsOrder;
import operato.logis.kmat_2026.service.impl.TbEcsLocMstService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

/**
 * ============================================================================
 * Location State Service
 * ============================================================================
 *
 * [역할]
 * - 기존 LocationCommandManager 의 "위치 상태 변경" 책임만 담당
 * - lock/unlock 자체는 LocationLockService가 담당
 *
 * [중요]
 * - 기존 운영 정책의 위치 전이 메서드 이름/의미를 유지한다.
 */
@Service
public class LocationStateService {

    private static final Logger logger = LoggerFactory.getLogger(LocationStateService.class);

    @Autowired
    protected TbEcsLocMstService tbEcsLocMstService;

    @Autowired
    protected LocationLockService locationLockService;

    /**
     * 기존 updateLocationsOnRobotTaskStart 의미 유지
     *
     * 여기서는 이미 락은 잡혀 있다고 가정한다.
     */
    @Transactional
    public void updateLocationsOnRobotTaskStart(TbWcsOrder order) {
        logger.info("[LocationStateService][updateLocationsOnRobotTaskStart] orderId={}, from={}, to={}",
                order.getOrderId(), order.getFromPositionCod(), order.getToPositionCod());
    }

    /**
     * 기존 updateLocationsOnRobotLoadingFinishOnFromSide 의미 유지
     */
    @Transactional
    public void updateLocationsOnRobotLoadingFinishOnFromSide(TbWcsOrder order) {
        String fromPositionCd = order.getFromPositionCod();

        locationLockService.unlockLocationByOrder(fromPositionCd, order.getOrderId());
        tbEcsLocMstService.updatePodCdAndStatus(fromPositionCd, null, null, LocationStatus.EMPTY, null, null);

        logger.info("[LocationStateService][updateLocationsOnRobotLoadingFinishOnFromSide] orderId={}, from={}",
                order.getOrderId(), fromPositionCd);
    }

    /**
     * 기존 updateLocationsOnAgvTaskComplete 의미 유지
     */
    @Transactional
    public void updateLocationsOnAgvTaskComplete(TbWcsOrder order, LocationStatus toStatus) {
        String toPositionCd = order.getToPositionCod();

        locationLockService.unlockLocationByOrder(toPositionCd, order.getOrderId());
        tbEcsLocMstService.updatePodCdAndStatus(
                toPositionCd,
                order.getPodCd(),
                order.getEquipId(),
                toStatus,
                order.getTaskId(),
                order.getOrderId()
        );

        logger.info("[LocationStateService][updateLocationsOnAgvTaskComplete] orderId={}, to={}, status={}",
                order.getOrderId(), toPositionCd, toStatus.getCode());
    }

    /**
     * 기존 updateLocationCancelTaskMng 의미 유지
     */
    @Transactional
    public void updateLocationCancelTaskMng(TbWcsOrder cancelOrder, Integer cancelStatus) {
        if (ValueUtil.isEmpty(cancelOrder)) {
            return;
        }

        String fromPositionCd = cancelOrder.getFromPositionCod();
        String toPositionCd = cancelOrder.getToPositionCod();

        if (ValueUtil.isNotEmpty(fromPositionCd)) {
            locationLockService.unlockLocationByOrder(fromPositionCd, cancelOrder.getOrderId());
            tbEcsLocMstService.updatePodCdAndStatus(fromPositionCd, null, null, LocationStatus.EMPTY, null, null);
        }

        if (ValueUtil.isNotEmpty(toPositionCd)) {
            locationLockService.unlockLocationByOrder(toPositionCd, cancelOrder.getOrderId());
            tbEcsLocMstService.updatePodCdAndStatus(toPositionCd, null, null, LocationStatus.EMPTY, null, null);
        }

        logger.info("[LocationStateService][updateLocationCancelTaskMng] orderId={}, cancelStatus={}, from={}, to={}",
                cancelOrder.getOrderId(), cancelStatus, fromPositionCd, toPositionCd);
    }
}