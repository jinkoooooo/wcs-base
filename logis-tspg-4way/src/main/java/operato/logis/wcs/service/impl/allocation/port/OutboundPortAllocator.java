package operato.logis.wcs.service.impl.allocation.port;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.LocType;
import operato.logis.wcs.entity.ExtTbInventoryLocation;
import operato.logis.wcs.service.repository.InventoryLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.elidom.util.ValueUtil;

import java.util.Comparator;
import java.util.List;

import static operato.logis.wcs.common.util.lang.CommonUtils.nz;
import static operato.logis.wcs.common.util.lang.CommonUtils.orDefault;

/**
 * 출고 포트 동적 할당기.
 *
 * 가용 포트가 없으면 예외 대신 WAITING 큐에 적재하고 null 반환.
 * 호출 측은 null 체크 후 처리 중단 필요.
 */
@Service
@RequiredArgsConstructor
public class OutboundPortAllocator {

    private static final Logger logger = LoggerFactory.getLogger(OutboundPortAllocator.class);

    // 출고 가능한 포트 타입 (전용 출고 포트 + 입출고 겸용 포트)
    private static final List<String> OUTBOUND_PORT_TYPES = List.of(
            LocType.OUTBOUND_PORT.code().toString(),
            LocType.IN_OUTBOUND_PORT.code().toString()
    );

    private final InventoryLocationRepository locationRepository;
    private final PortTrafficService portTrafficController;

    /**
     * 가용 출고 포트 중 최적 1개 선정 (activeTaskCount 적은 순 → locId 사전순).
     * 가용 포트가 없으면 orderKey 를 WAITING 큐에 적재하고 null 반환.
     */
    @Transactional(rollbackFor = Exception.class)
    public String allocateBestPort(String eqGroupId, String orderKey) {

        logger.info("[ Allocation ][ Port ] outbound alloc start - eqGroupId={}, orderKey={}", eqGroupId, orderKey);

        // 출고 포트 타입 전체 조회
        List<ExtTbInventoryLocation> allCandidates =
                locationRepository.findByEqGroupIdAndLocTypeList(eqGroupId, OUTBOUND_PORT_TYPES);
        if (ValueUtil.isEmpty(allCandidates)) {
            logger.warn("[ Allocation ][ Port ] outbound alloc - no port master. eqGroupId={}", eqGroupId);
            enqueueWaiting(orderKey);
            return null;
        }

        // 활성 포트만 필터링
        List<ExtTbInventoryLocation> filtered = allCandidates.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsEnabled()))
                .toList();
        if (ValueUtil.isEmpty(filtered)) {
            logger.warn("[ Allocation ][ Port ] outbound alloc - no available port. orderKey={}", orderKey);
            enqueueWaiting(orderKey);
            return null;
        }

        // 최적 포트 선정
        ExtTbInventoryLocation best = filtered.stream()
                .min(bestPortComparator())
                .orElse(filtered.get(0));

        logger.info("[ Allocation ][ Port ] outbound alloc done - locId={}, activeTask={}, candidates={}/{}, type={}, mode={}",
                best.getLocId(), best.getActiveTaskCount(),
                filtered.size(), allCandidates.size(),
                best.getLocType(), best.getPortMode());

        return best.getLocId();
    }

    // 포트 정렬 기준 - activeTaskCount 오름차순 → locId 사전순
    private Comparator<ExtTbInventoryLocation> bestPortComparator() {
        return Comparator
                .comparingInt((ExtTbInventoryLocation p) -> nz(p.getActiveTaskCount()))
                .thenComparing(p -> orDefault(p.getLocId(), "ZZZZ"));
    }

    // 가용 포트 없을 때 출고 오더를 WAITING 큐에 적재
    private void enqueueWaiting(String orderKey) {
        if (ValueUtil.isNotEmpty(orderKey)) {
            portTrafficController.addToWaitingQueue(orderKey);
        }
    }
}
