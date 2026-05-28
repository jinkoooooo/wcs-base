package operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.service.LocationAllocator;

import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.ErrorEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.consts.LocTypeEnumCode;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.dto.*;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.util.WcsLocationUtil;
import operato.logis.kmat_2026.biz.wcs.tspg_4way_shuttle.entity.TbWcsLocMst;
import operato.logis.kmat_2026.service.impl.TbWcsLocMstService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * [입고 로케이션 할당 Allocator - 최종 완성형]
 * 1. 구역 전체 지형 분석 (Sandwich 판별)
 * 2. 가용 및 접근 가능 후보군(Tier 1) 추출
 * 3. 외부 엔진 최적화 호출
 * 4. DB SKIP LOCKED를 통한 물리적 선점(Lock) 확정
 */
@Service
public class WcsInboundLocationAllocator {
    private static final Logger logger = LoggerFactory.getLogger(WcsInboundLocationAllocator.class);

    @Autowired protected TbWcsLocMstService locMstService;

    public AllocationResult allocate(WcsOrderCommand command) {
        logger.info("========== [Strategic Inbound Allocation Start] ==========");
        String eqGroupId = command.getEqGroupId();

        // 1. [지형 데이터 로드] 해당 구역(층)의 모든 RACK 정보를 가져옵니다. (샌드위치 판별용)
        // findMultipleWithPosition은 내부적으로 JOIN을 통해 Row/Bay/Level/DriveOnly 정보를 포함합니다.
        List<LocWithPosition> allRacks = locMstService.findMultipleWithPosition(eqGroupId, null, LocTypeEnumCode.RACK);

        if (allRacks.isEmpty()) {
            return AllocationResult.fail("ERR_NO_DATA", "해당 구역에 로케이션 마스터 정보가 없습니다: " + eqGroupId);
        }

        // 2. [접근성 분석] 전체 지형을 분석하여 샌드위치(ㅁ) 맵 생성
        Map<String, Boolean> sandwichMap = WcsLocationUtil.buildSandwichMap(allRacks);

        // 3. [후보군 필터링]
        // - 조건 A: 현재 비어있고(Status=0) 락이 걸리지 않은(LockYn=0) 곳
        // - 조건 B: 샌드위치 상태가 아닌 곳 (Tier 1: 가장자리 또는 주행로 인접)
        List<LocWithPosition> validCandidates = allRacks.stream()
                .filter(lwp -> (lwp.getLoc().getStatus() != null && lwp.getLoc().getStatus() == 0)
                        && lwp.getLoc().getLockYn() == 0
                        && !Boolean.TRUE.equals(sandwichMap.get(lwp.getLoc().getLocCode())))
                .collect(Collectors.toList());

        // [예외 처리] 만약 모든 빈자리가 샌드위치 상태라면? (풀 캐파에 가까운 상황)
        // 운영 정책에 따라 에러를 내거나, 억지로라도 샌드위치 자리를 줘야 합니다. 여기서는 에러로 처리합니다.
        if (validCandidates.isEmpty()) {
            logger.warn("[Step 3] 가용 자리는 있으나 모두 샌드위치(접근 불가) 상태입니다. EqGroupId: {}", eqGroupId);
            return AllocationResult.fail(ErrorEnumCode.NO_AVAILABLE_LOCATION.codeAsString(), "Reachable Rack Full");
        }

        // 4. [외부 최적화 엔진 호출]
        // 샌드위치를 제외한 '진짜 갈 수 있는' 후보들만 엔진에 넘깁니다.
        List<String> optimizedCodes = callExternalOptimizationEngine(command, validCandidates);

        if (optimizedCodes == null || optimizedCodes.isEmpty()) {
            return AllocationResult.fail("ERR_ENGINE", "최적화 엔진으로부터 추천 결과를 받지 못했습니다.");
        }

        // 5. [물리적 선점 및 확정]
        // 엔진이 추천한 '최적의 리스트'를 들고 DB에 가서 SKIP LOCKED로 하나를 낚아챕니다.
        // 이 과정에서 selectAndLockBestCandidate 내부적으로 findAndLockBestOne을 호출합니다.
        TbWcsLocMst lockedLoc = selectAndLockBestCandidate(eqGroupId, optimizedCodes);

        if (lockedLoc == null) {
            // 엔진 추천 시점과 락 시점 사이(수 ms)에 다른 주문이 채간 경우
            logger.error("[Step 5] 동시성 충돌: 엔진 추천 로케이션이 이미 점유되었습니다.");
            return AllocationResult.fail(ErrorEnumCode.NO_AVAILABLE_LOCATION.codeAsString(), "Concurrent Lock Conflict");
        }

        logger.info("[Final Success] 최종 할당 완료: {}", lockedLoc.getLocCode());
        return AllocationResult.success("INBOUND_PORT", lockedLoc.getLocCode(), eqGroupId);
    }

    /**
     * 외부 최적화 엔진 호출 (예시)
     */
    private List<String> callExternalOptimizationEngine(WcsOrderCommand command, List<LocWithPosition> candidates) {
        // 엔진은 이미 샌드위치가 제거된 '클린한 후보'들만 받기 때문에
        // 동선 최적화나 LocSeq 정렬에만 집중하면 됩니다.
        return candidates.stream()
                .sorted(Comparator.comparingInt(c -> c.getLoc().getLocSeq() != null ? c.getLoc().getLocSeq() : 999))
                .limit(5) // 상위 5개를 후보로 넘겨서 락 충돌 시 차선책을 가질 수 있게 함
                .map(l -> l.getLoc().getLocCode())
                .collect(Collectors.toList());
    }

    /**
     * DB SKIP LOCKED를 통한 물리적 락 시도
     */
    protected TbWcsLocMst selectAndLockBestCandidate(String eqGroupId, List<String> orderedCodes) {
        // 이 메서드는 반드시 트랜잭션 내부(Manager)에서 호출되어야 하며,
        // SELECT ... FOR UPDATE SKIP LOCKED LIMIT 1 쿼리를 실행합니다.
        return locMstService.findAndLockBestOne(eqGroupId, orderedCodes);
    }
}