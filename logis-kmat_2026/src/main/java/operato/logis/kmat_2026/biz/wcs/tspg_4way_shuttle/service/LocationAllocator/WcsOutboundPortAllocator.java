package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.LocationAllocator;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsLocMst;
import operato.logis.kmat_2026.service.impl.TbWcsLocMstService;
import operato.logis.kmat_2026.service.impl.TbWcsShuttleOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WcsOutboundPortAllocator {
    private static final Logger logger = LoggerFactory.getLogger(WcsOutboundPortAllocator.class);

    @Autowired private TbWcsLocMstService locMstService;
    @Autowired private TbWcsShuttleOrderService shuttleOrderService;

    /**
     * 출고 최적 포트 선정 전략:
     * 1. 전용 출고 포트(OUTBOUND_PORT)를 겸용 포트(IN_OUTBOUND_PORT)보다 우선한다.
     * 2. 현재 셔틀 오더가 가장 적게 쌓인(대기열이 짧은) 포트를 선택한다 (Load Balancing).
     * 3. 입고 작업이 활발한 겸용 포트는 페널티를 부여하여 회피한다.
     */
    public String allocateBestPort(String eqGroupId) {
        logger.info("[PortAlloc] >>> Starting Port Allocation for Group: {}", eqGroupId);

        // 1. 후보 포트 추출
        List<TbWcsLocMst> candidates = locMstService.findByEqGroupIdAndLocTypeList(eqGroupId,
                Arrays.asList(LocTypeEnumCode.OUTBOUND_PORT.code().toString(),
                        LocTypeEnumCode.IN_OUTBOUND_PORT.code().toString()));

        if (candidates.isEmpty()) {
            logger.warn("[PortAlloc] No outbound ports found in zone {}. Fallback to OUT-PORT-01", eqGroupId);
            return "OUT-PORT-01";
        }

        // 2. 실시간 데이터 확보
        Map<String, Long> portLoadMap = shuttleOrderService.getActiveOrderCountByPort(eqGroupId);
        Set<String> activeInboundPorts = shuttleOrderService.getActiveInboundPorts(eqGroupId);

        logger.info("[PortAlloc] Current Context -> ActiveInbound: {}, LoadMap: {}", activeInboundPorts, portLoadMap);

        // 3. 최적의 포트 선정 (상세 로그 포함)
        TbWcsLocMst bestPort = candidates.stream()
                .min((p1, p2) -> {
                    long s1 = calculateScoreWithLog(p1, portLoadMap.getOrDefault(p1.getLocCode(), 0L), activeInboundPorts);
                    long s2 = calculateScoreWithLog(p2, portLoadMap.getOrDefault(p2.getLocCode(), 0L), activeInboundPorts);

                    if (s1 != s2) return Long.compare(s1, s2);

                    // 점수 같을 경우 Seq 비교
                    int seq1 = p1.getLocSeq() != null ? p1.getLocSeq() : 999;
                    int seq2 = p2.getLocSeq() != null ? p2.getLocSeq() : 999;
                    return Integer.compare(seq1, seq2);
                })
                .orElse(candidates.get(0));

        logger.info("[PortAlloc] <<< Final Decision: {} (Candidates: {})", bestPort.getLocCode(), candidates.size());

        return bestPort.getLocCode();
    }

    /**
     * 포트 점수 계산 및 상세 로그 출력
     */
    private long calculateScoreWithLog(TbWcsLocMst port, long currentLoad, Set<String> activeInboundPorts) {
        String locCode = port.getLocCode();
        String locType = port.getLocType();

        // 1. 기본 부하 점수 (오더 1개당 10점)
        long loadScore = currentLoad * 10;

        // 2. 겸용 포트 페널티 (+5)
        long typePenalty = 0;
        if (LocTypeEnumCode.IN_OUTBOUND_PORT.code().toString().equalsIgnoreCase(locType)) {
            typePenalty = 5;
        }

        // 3. 입고 간섭 페널티 (+50)
        long inboundPenalty = 0;
        if (activeInboundPorts.contains(locCode)) {
            inboundPenalty = 50;
        }

        long totalScore = loadScore + typePenalty + inboundPenalty;

        // [핵심 로그] 각 후보의 성적표 출력
        logger.info("[PortAlloc-Score] Loc: {} | Total: {} [Load:({}*10), TypePen: {}, InboundPen: {}] | Type: {}",
                locCode, totalScore, currentLoad, typePenalty, inboundPenalty, locType);

        return totalScore;
    }
}