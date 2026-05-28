package operato.logis.wcs.service.impl.allocation.port;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.SubOrderType;
import operato.logis.wcs.entity.TbWcsShuttleOrder;
import operato.logis.wcs.service.impl.system.SystemModeService;
import operato.logis.wcs.service.repository.ShuttleOrderReleaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xyz.elidom.util.ValueUtil;

/**
 * 포트 락 필요 여부 평가 - 락 판단의 단일 진실 소스.
 *
 * "포트 락을 걸어야 하는가"의 모든 조건이 여기 한 곳에 있다.
 * 신규 사유는 requiresPortLock 안에 if 한 줄만 추가.
 *
 * 실제 락 UPDATE 는 PortService.tryLockForDispatch 가 수행 - 그쪽은 판단 없는 순수 실행.
 */
@Component
@RequiredArgsConstructor
public class PortLockPolicy {

    private static final Logger logger = LoggerFactory.getLogger(PortLockPolicy.class);

    private final ShuttleOrderReleaseRepository releaseRepository;
    private final SystemModeService systemModeService;

    /**
     * 포트 락을 걸어야 하는가 - 모든 판단을 여기서 끝낸다.
     */
    public boolean requiresPortLock(TbWcsShuttleOrder parent) {

        // 기본 전제 - 대상 자체가 성립해야
        if (ValueUtil.isEmpty(parent)) return false;
        if (ValueUtil.isEmpty(parent.getToLocCode())) return false;
        if (ValueUtil.isEmpty(parent.getHostOrderKey())) return false;

        // 그룹 dispatch lock 모드가 꺼져 있으면 어떤 사유든 락 안 함
        if (!systemModeService.isDispatchLockEnabled(parent.getEqGroupId())) {
            logger.debug("[ Allocation ][ Port ] lock skip - dispatch lock disabled. eqGroupId={}, parentKey={}",
                    parent.getEqGroupId(), parent.getOrderKey());
            return false;
        }

        // 락 사유 1 - 짝지어진 재입고 존재
        if (hasPairedReinbound(parent)) {
            logger.debug("[ Allocation ][ Port ] lock reason=paired_reinbound, parentKey={}", parent.getOrderKey());
            return true;
        }

        // 락 사유 2 - 서브타입이 락 요구
        if (SubOrderType.fromOrNormal(parent.getSubOrderType()).requiresPortLock()) {
            logger.debug("[ Allocation ][ Port ] lock reason=sub_type, parentKey={}, sub={}",
                    parent.getOrderKey(), parent.getSubOrderType());
            return true;
        }
        return false;
    }

    // 짝지어진 재입고 셔틀이 존재하는지 카운트 조회
    private boolean hasPairedReinbound(TbWcsShuttleOrder parent) {
        return releaseRepository.countPairedReinboundShuttles(
                parent.getHostOrderKey(), parent.getOrderKey()) > 0;
    }
}
