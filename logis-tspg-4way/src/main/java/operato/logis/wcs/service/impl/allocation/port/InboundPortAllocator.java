package operato.logis.wcs.service.impl.allocation.port;

import lombok.RequiredArgsConstructor;
import operato.logis.wcs.consts.LocType;
import operato.logis.wcs.consts.PortMode;
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
 * 입고 포트 동적 할당기.
 *
 * 상태를 변경하지 않는다.
 * BCR 스캔 시 handleInboundEntry 에서 최종 재검증/확정된다.
 */
@Service
@RequiredArgsConstructor
public class InboundPortAllocator {

    private static final Logger logger = LoggerFactory.getLogger(InboundPortAllocator.class);

    // 입고 가능한 포트 타입 (전용 입고 포트 + 입출고 겸용 포트)
    private static final List<String> INBOUND_PORT_TYPES = List.of(
            LocType.INBOUND_PORT.code().toString(),
            LocType.IN_OUTBOUND_PORT.code().toString()
    );

    private final InventoryLocationRepository locationRepository;

    /**
     * 가용 입고 포트 중 최적 1개 선정 (활성 + 비드레인 + activeTaskCount 적은 순 → locId 사전순).
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public String allocateBestPort(String eqGroupId) {

        // 입력값 검증
        if (ValueUtil.isEmpty(eqGroupId)) {
            logger.warn("[ Allocation ][ Port ] inbound alloc - eqGroupId missing");
            return null;
        }

        logger.info("[ Allocation ][ Port ] inbound alloc start - eqGroupId={}", eqGroupId);

        // 입고 포트 타입 전체 조회
        List<ExtTbInventoryLocation> allCandidates =
                locationRepository.findByEqGroupIdAndLocTypeList(eqGroupId, INBOUND_PORT_TYPES);
        if (ValueUtil.isEmpty(allCandidates)) {
            logger.warn("[ Allocation ][ Port ] inbound alloc - no port master. eqGroupId={}", eqGroupId);
            return null;
        }

        // 가용 포트 필터링 (활성 + 모드 전환 중 아님)
        List<ExtTbInventoryLocation> filtered = allCandidates.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsEnabled()))
                .filter(this::isNotDraining)
                .toList();
        if (ValueUtil.isEmpty(filtered)) {
            logger.warn("[ Allocation ][ Port ] inbound alloc - no available port. total={}, eqGroupId={}",
                    allCandidates.size(), eqGroupId);
            return null;
        }

        // 최적 포트 선정
        ExtTbInventoryLocation best = filtered.stream()
                .min(bestPortComparator())
                .orElse(filtered.get(0));

        logger.info("[ Allocation ][ Port ] inbound alloc done - locId={}, activeTask={}, candidates={}/{}, type={}, mode={}",
                best.getLocId(), best.getActiveTaskCount(),
                filtered.size(), allCandidates.size(),
                best.getLocType(), best.getPortMode());

        return best.getLocId();
    }

    // 포트 정렬 기준 - 미점유 우선 → activeTaskCount 오름 → locId 사전순
    private Comparator<ExtTbInventoryLocation> bestPortComparator() {
        return Comparator
                .comparingInt((ExtTbInventoryLocation p) -> isNotLocked(p) ? 0 : 1)
                .thenComparingInt(p -> nz(p.getActiveTaskCount()))
                .thenComparing(p -> orDefault(p.getLocId(), "ZZZZ"));
    }

    // 포트 락 여부 - taskId 비어있으면 가용
    private boolean isNotLocked(ExtTbInventoryLocation port) {
        return ValueUtil.isEmpty(port.getTaskId());
    }

    // 모드 전환 중인지 - SWITCHING_TO_* 상태면 제외
    private boolean isNotDraining(ExtTbInventoryLocation port) {
        String mode = port.getPortMode();
        return !PortMode.SWITCHING_TO_INBOUND.code().toString().equalsIgnoreCase(mode)
                && !PortMode.SWITCHING_TO_OUTBOUND.code().toString().equalsIgnoreCase(mode);
    }
}
